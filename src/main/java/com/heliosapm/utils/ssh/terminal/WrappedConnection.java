/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2015, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.utils.ssh.terminal;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionInfo;
import ch.ethz.ssh2.ConnectionMonitor;
import ch.ethz.ssh2.LocalPortForwarder;
import ch.ethz.ssh2.LocalStreamForwarder;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.SCPOutputStream;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import com.heliosapm.utils.io.BroadcastingCloseable;
import com.heliosapm.utils.io.BroadcastingCloseableImpl;
import com.heliosapm.utils.io.CloseListener;
import com.heliosapm.utils.io.NIOHelper;
import com.heliosapm.utils.jmx.SharedNotificationExecutor;

/**
 * <p>Title: WrappedConnection</p>
 * <p>Description: Wraps a connection, connection info and other bits and pieces of interest to a connection</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ssh.terminal.WrappedConnection</code></p>
 */

public class WrappedConnection implements WrappedConnectionMBean, ConnectionMonitor, BroadcastingCloseable<WrappedConnection>, CloseListener<Closeable> {
	/** The wrapped connection */
	private Connection connection;
	/** The connection info */
	private ConnectionInfo connectionInfo;
	/** The auth info */
	private ConnectInfo authInfo;
	
	/** The resolved host name and key for this connection */
	private final String hostName;	
	/** The remote port */
	private final int port;
	/** The connection key */
	private final String key;
	/** The reconnect schedule handle */
	private ScheduledFuture<?> reconnectHandle = null;

	/** The shared notification executor */
	private final SharedNotificationExecutor notifExecutor;
	
	/** The local port forwards created through this connection */
	private final Map<String, WrappedLocalPortForwarder> localPortForwards = new ConcurrentHashMap<String, WrappedLocalPortForwarder>();
	
	/** The connection cache */
	private static final Map<String, WrappedConnection> connectionCache = new ConcurrentHashMap<String, WrappedConnection>();
	
	/** The throwable message when the connection is closed by user request */
	public static final String USER_CLOSED_MSG = "Closed due to user request.";
	
	/** The close event broadcaster */
	private final BroadcastingCloseableImpl<WrappedConnection> closeBroadcaster = new BroadcastingCloseableImpl<WrappedConnection>(this) {

		@Override
		public void close() throws IOException {
			doClose(null);
		}
				
		public void close(final Throwable cause) throws IOException {
			if(cause!=null) {
				if(USER_CLOSED_MSG.equals(cause.getMessage())) {
					doClose(null);
					return;
				}
			}
			doClose(cause);
		}

		@Override
		public void reset() {
			
			try { connection.close(); } catch (Exception x) {}
			try {
				connection.connect(authInfo.getVerifier(), authInfo.getConnectTimeout(), authInfo.getKexTimeout());
			} catch (Exception ex) {
				throw new RuntimeException("Failed to reconnect", ex);
			}
			
			try {				
				if(!authInfo.authenticate(connection)) {
					throw new Exception("Failed to authenticate");
				}
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
				throw new RuntimeException("Failed to reauthenticate", ex);
			}
			doReset();
		}
	};
	
	/**
	 * Closes the connection and removes it from cache
	 */
	public void purge() {
		try { close(); } catch (Exception x) {/* No Op */}
		connectionCache.remove(key);
	}
	
	/**
	 * Registers a closed connection listener
	 * @param listener The listener to register
	 */
	@Override
	public void addListener(CloseListener<WrappedConnection> listener) {
		if(listener==null) throw new IllegalArgumentException("The passed listener was null");
		closeBroadcaster.addListener(listener);		
	}
	
	/**
	 * Registers a closed connection listener
	 * @param listener The listener to register
	 * @return this connection
	 */
	public WrappedConnection registerListener(final CloseListener<WrappedConnection> listener) {
		if(listener==null) throw new IllegalArgumentException("The passed listener was null");
		closeBroadcaster.addListener(listener);
		return this;
	}
	
	@Override
	public void removeListener(final CloseListener<WrappedConnection> listener) {
		closeBroadcaster.removeListener(listener);		
	}
	
	/**
	 * Registers a connection monitor
	 * @param listener The listener to register
	 * @return this connection
	 */
	public WrappedConnection addListener(final ConnectionMonitor listener) {
		if(listener==null) throw new IllegalArgumentException("The passed listener was null");
		closeBroadcaster.addListener(new CloseListener<WrappedConnection>(){
			@Override
			public void onClosed(final WrappedConnection closeable, final Throwable cause) {				
				listener.connectionLost(cause);
			}
			@Override
			public void onReset(WrappedConnection resetCloseable) {
				// No Op				
			}
		});	
		return this;
	}
	
	/**
	 * Creates an unwrapped local port forward
	 * @param hostToTunnel The host to tunnel to
	 * @param portToTunnel The port to tunnel to
	 * @return the local port forward reference
	 */
	LocalPortForwarder rawTunnel(final String hostToTunnel, final int portToTunnel) {
		if(hostToTunnel==null || hostToTunnel.trim().isEmpty()) throw new IllegalArgumentException("The passed hostToTunnel was null or empty");		
		if(!isOpen()) throw new RuntimeException("This connection to [" + hostName + ":" + port + "] is closed");
		try {
			return connection.createLocalPortForwarder(0, hostToTunnel, portToTunnel);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to tunnel to [" + key + "]", ex);
		}		
	}
	
	/**
	 * Creates an unwrapped local stream forward
	 * @param hostToTunnel The host to tunnel to
	 * @param portToTunnel The port to tunnel to
	 * @return the local stream forward reference
	 */
	LocalStreamForwarder rawStreamTunnel(final String hostToTunnel, final int portToTunnel) {
		if(hostToTunnel==null || hostToTunnel.trim().isEmpty()) throw new IllegalArgumentException("The passed hostToTunnel was null or empty");		
		if(!isOpen()) throw new RuntimeException("This connection to [" + hostName + ":" + port + "] is closed");
		try {
			return connection.createLocalStreamForwarder(hostToTunnel, portToTunnel);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to tunnel stream to [" + key + "]", ex);
		}		
	}
	
	
	/**
	 * Acquires a dedicated (unshared) local port forward
	 * @param hostToTunnel The host to tunnel to
	 * @param portToTunnel The port to tunnel to
	 * @return the local port forward reference
	 */
	public WrappedLocalPortForwarder dedicatedTunnel(final String hostToTunnel, final int portToTunnel) {
		if(hostToTunnel==null || hostToTunnel.trim().isEmpty()) throw new IllegalArgumentException("The passed hostToTunnel was null or empty");		
		if(!isOpen()) throw new RuntimeException("This connection to [" + hostName + ":" + port + "] is closed");
		LocalPortForwarder loc;
		final String key = hostToTunnel + ":" + portToTunnel;
		try {
			loc = connection.createLocalPortForwarder(0, hostToTunnel, portToTunnel);
			return new WrappedLocalPortForwarder(loc, key, this, true);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to tunnel to [" + key + "]", ex);
		}
		
	}
	
	/**
	 * Acquires a dedicated (unshared) local stream forward
	 * @param hostToTunnel The host to tunnel to
	 * @param portToTunnel The port to tunnel to
	 * @return the local stream forward reference
	 */
	public WrappedStreamForwarder dedicatedStreamTunnel(final String hostToTunnel, final int portToTunnel) {
		if(hostToTunnel==null || hostToTunnel.trim().isEmpty()) throw new IllegalArgumentException("The passed hostToTunnel was null or empty");		
		if(!isOpen()) throw new RuntimeException("This connection to [" + hostName + ":" + port + "] is closed");
		LocalStreamForwarder loc;
		final String key = hostToTunnel + ":" + portToTunnel;
		try {
			loc = rawStreamTunnel(hostToTunnel, portToTunnel);
			return new WrappedStreamForwarder(loc, key, this);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to tunnel stream to [" + key + "]", ex);
		}
		
	}
	
	
	
	/**
	 * Acquires a local port forward
	 * @param hostToTunnel The host to tunnel to
	 * @param portToTunnel The port to tunnel to
	 * @return the local port forward reference
	 */
	public WrappedLocalPortForwarder tunnel(final String hostToTunnel, final int portToTunnel) {
		if(hostToTunnel==null || hostToTunnel.trim().isEmpty()) throw new IllegalArgumentException("The passed hostToTunnel was null or empty");		
		if(!isOpen()) throw new RuntimeException("This connection to [" + hostName + ":" + port + "] is closed");
		final String key = hostToTunnel + ":" + portToTunnel;
		WrappedLocalPortForwarder lpf = localPortForwards.get(key);
		if(lpf==null) {
			synchronized(localPortForwards) {
				lpf = localPortForwards.get(key);
				if(lpf==null) {
					try {
						final LocalPortForwarder loc = connection.createLocalPortForwarder(0, hostToTunnel, portToTunnel);
						lpf = new WrappedLocalPortForwarder(loc, key, this);
						localPortForwards.put(key, lpf);
					} catch (Exception ex) {
						throw new RuntimeException("Failed to tunnel to [" + key + "]", ex);
					}
				}
			}
		}
		if(!lpf.isOpen()) {
			localPortForwards.remove(lpf.getKey());
			return tunnel(hostToTunnel, portToTunnel);
		}
		lpf.incrementClaimCount();
		return lpf;
	}
	
	/**
	 * Returns a new SCPClient connected to the host this connection is connected to
	 * @return an SCPClient
	 */
	public SCPClient scpClient() {
		return new SCPClient(this.connection);
	}
	
	public void putFile(final File file, final String remoteFileName, final String remoteDirName, final String mode) {
		FileInputStream fis = null;
		FileChannel fc = null;
		MappedByteBuffer buff = null;
		try {
			final long fileSize = file.length();
			fis = new FileInputStream(file);
			fc = fis.getChannel();
			buff = fc.map(MapMode.READ_ONLY, 0, fileSize);
			buff.load();
			putFile(buff, remoteFileName, remoteDirName, mode);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to write file [" + remoteDirName + "/" + remoteFileName + "] on [" + hostName + ":" + port + "]", ex);
		} finally {
			if(buff!=null) try { NIOHelper.clean(buff); } catch (Exception x) {/* No Op */}
			if(fis!=null) try { fis.close(); } catch (Exception x) {/* No Op */}
			if(fc!=null) try { fc.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	public void putFile(final ByteBuffer buffer, final String remoteFileName, final String remoteDirName, final String mode) {
		SCPClient client = null;
		SCPOutputStream os = null;
		WritableByteChannel wbc = null;
		try {
			final long fileSize = buffer.limit()-buffer.position();
			client = scpClient();
			os = client.put(remoteFileName, fileSize, remoteDirName, mode);
			wbc = Channels.newChannel(os);
			wbc.write(buffer);
			wbc.close();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to write file [" + remoteDirName + "/" + remoteFileName + "] on [" + hostName + ":" + port + "]", ex);
		} finally {
			if(wbc!=null && wbc.isOpen()) try { wbc.close(); } catch (Exception x) {/* No Op */}
			if(os!=null) try { os.close(); } catch (Exception x) {/* No Op */}			
		}
	}
	
	
	/**
	 * Acquires a local port forward to the connected host
	 * @param portToTunnel The port to tunnel to
	 * @return the local port forward reference
	 */
	public WrappedLocalPortForwarder tunnel(final int portToTunnel) {
		return tunnel(hostName, portToTunnel);
	}
	
	
	/**
	 * Callback from issued WrappedLocalPortForwarder when the claim count drops to zero
	 * @param key The tunnel key
	 */
	void onLocalPortForwardClosed(final String key) {
		final WrappedLocalPortForwarder lpf = localPortForwards.remove(key);
		if(lpf!=null) {
			lpf.hardClose();
		}
	}
	
	public void hardReset(final ConnectInfo connectInfo) {
		try { connection.close(new Throwable("Hard reset"), true); } catch (Exception x) {/* No Op */}
		connection = null;
		try { reconnectHandle.cancel(true); } catch (Exception x) {/* No Op */}
		reconnectHandle = null;
		connection = connectRaw(hostName, port, connectInfo);
		try {
			connectionInfo = connection.getConnectionInfo();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.authInfo = authInfo==null ? new ConnectInfo() : authInfo;		
		this.connection.addConnectionMonitor(this);		
	}
	
	/**
	 * 
	 * @see com.heliosapm.utils.io.BroadcastingCloseable#reset()
	 */
	public void reset() {
		closeBroadcaster.reset();
		try {
			connectionInfo = this.connection.getConnectionInfo();
		} catch (IOException e) {
			this.close();
			throw new RuntimeException("Failed to acquire ConnectionInfo for [" + hostName + "]", e);
		}		
	}




	
	/**
	 * Creates and attempts to connect a new connection
	 * @param hostName The host name
	 * @param port The SSH listening port
	 * @param authInfo The authentication info
	 * @return a connected but not authenticated {@link WrappedConnection}
	 */
	public static final WrappedConnection connect(final String hostName, final int port, final ConnectInfo authInfo) {
		final WrappedConnection wconn = create(hostName, port, authInfo);
		try {
			wconn.connection.connect(authInfo.getVerifier(), authInfo.getConnectTimeout(), authInfo.getKexTimeout());			
		} catch (Exception ex) {
			// "is already in connected state"
			ex.printStackTrace(System.err);
			throw new RuntimeException("Failed to connect to [" + hostName + ":" + port + "]", ex);
		}
		return wconn;
	}
	
	/**
	 * Creates and attempts to connect a new connection
	 * @param hostName The host name
	 * @param port The SSH listening port
	 * @param authInfo The authentication info
	 * @return a connected but not authenticated {@link WrappedConnection}
	 */
	public static final Connection connectRaw(final String hostName, final int port, final ConnectInfo authInfo) {
		final Connection conn;
		try {
			conn = createRaw(hostName, port);
			conn.connect(authInfo.getVerifier(), authInfo.getConnectTimeout(), authInfo.getKexTimeout());			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException("Failed to connect to [" + hostName + ":" + port + "]", ex);
		}
		final boolean authed = authInfo.authenticate(conn);
		if(!authed) {
			throw new RuntimeException("Failed to authenticate");
		}		
		return conn;
	}
	
	
	/**
	 * Creates and attempts to connect a new connection on port 22
	 * @param hostName The host name
	 * @param authInfo The authentication info
	 * @return a connected but not authenticated {@link WrappedConnection}
	 */
	public static final WrappedConnection connect(final String hostName, final ConnectInfo authInfo) {
		return connect(hostName, 22, authInfo);
	}
	
	/**
	 * Resets the SSH connection to the passed host/port
	 * @param hostName The SSH server host name
	 * @param port The SSH listening port
	 * @param connectInfo The connect info to reset with
	 */
	public static void reset(final String hostName, final int port, final ConnectInfo connectInfo) {
		if(hostName==null || hostName.trim().isEmpty()) throw new IllegalArgumentException("The passed host name was null or empty");
		final String key = hostName + ":" + port;
		WrappedConnection wconn = connectionCache.get(key);
		if(wconn!=null) {
			wconn.hardReset(connectInfo);
		}
	}
	
	/**
	 * Creates but does not attempt to connect a new connection
	 * @param hostName The SSH server host name
	 * @param port The SSH listening port
	 * @param authInfo The authentication info
	 * @return a disconnected and not authenticated {@link WrappedConnection}
	 */
	public static final WrappedConnection create(final String hostName, final int port, final ConnectInfo authInfo) {
		if(hostName==null || hostName.trim().isEmpty()) throw new IllegalArgumentException("The passed host name was null or empty");
		final String key = hostName + ":" + port;
		WrappedConnection wconn = connectionCache.get(key);
		if(wconn==null) {
			synchronized(connectionCache) {
				wconn = connectionCache.get(key);
				if(wconn==null) {
					final Connection conn = new Connection(hostName, port);
//					try {
//						conn.connect(authInfo.getVerifier(), authInfo.getConnectTimeout(), authInfo.getKexTimeout());
//					} catch (Exception ex) {
//						throw new RuntimeException("Failed to connect to [" + key + "]", ex);
//					}
					wconn = new WrappedConnection(conn, authInfo);					
				}
			}
		}
		return wconn;
	}
	
	/**
	 * Creates but does not attempt to connect a new raw connection
	 * @param hostName The SSH server host name
	 * @param port The SSH listening port
	 * @return a disconnected and not authenticated {@link WrappedConnection}
	 */
	public static final Connection createRaw(final String hostName, final int port) {
		if(hostName==null || hostName.trim().isEmpty()) throw new IllegalArgumentException("The passed host name was null or empty");
		return new Connection(hostName, port);
	}
	
	/**
	 * Creates a new raw connection and attempts to authenticate
	 * @param hostName The SSH server host name
	 * @param port The SSH listening port
	 * @param authInfo The authentication credentials 
	 * @return a disconnected and not authenticated {@link WrappedConnection}
	 */
	public static final Connection createRaw(final String hostName, final int port, final ConnectInfo authInfo) {
		if(hostName==null || hostName.trim().isEmpty()) throw new IllegalArgumentException("The passed host name was null or empty");
		final Connection conn = new Connection(hostName, port);
		authInfo.authenticate(conn);
		return conn;
	}
	
	/**
	 * Creates a dedicated (non-shared) wrapped connection
	 * @param connection A raw connection
	 * @param authInfo The authentication credentials
	 * @return a dedicated wrapped connection
	 */
	public static WrappedConnection wrap(final Connection connection, final ConnectInfo authInfo) {
		return new WrappedConnection(connection, authInfo);
	}
	
	/**
	 * Creates but does not attempt to connect a new connection on port 22
	 * @param hostName The host name
	 * @param authInfo The authentication info
	 * @return a disconnected and not authenticated {@link WrappedConnection}
	 */
	public static final WrappedConnection create(final String hostName, final ConnectInfo authInfo) {
		return create(hostName, 22, authInfo);
	}
	
	/**
	 * Creates and attempts to connect and authenticate a new connection
	 * @param hostName The host name
	 * @param port The SSH listening port
	 * @param authInfo The authentication info
	 * @return a connected and authenticated {@link WrappedConnection}
	 */
	public static final WrappedConnection connectAndAuthenticate(final String hostName, final int port, final ConnectInfo authInfo) {
		final WrappedConnection wconn = connect(hostName, port, authInfo);
		final boolean authed = authInfo.authenticate(wconn.connection);
		if(!authed) {
			throw new RuntimeException("Failed to authenticate");
		}
		//wconn.reset();
		return wconn;
	}
	
	/**
	 * Creates and attempts to connect and authenticate a new connection on port 22
	 * @param hostName The host name
	 * @param authInfo The authentication info
	 * @return a connected and authenticated {@link WrappedConnection}
	 */
	public static final WrappedConnection connectAndAuthenticate(final String hostName, final ConnectInfo authInfo) {
		return connectAndAuthenticate(hostName, 22, authInfo);
	}
	
	
	
	/**
	 * Creates a new WrappedConnection
	 * @param connection The wrapped connection
	 * @param authInfo The authentication resources
	 */
	private WrappedConnection(final Connection connection, final ConnectInfo authInfo) {
		if(connection==null) throw new IllegalArgumentException("The passed connection was null");
		this.connection = connection;
		this.authInfo = authInfo==null ? new ConnectInfo() : authInfo;
		String tmpName = null;
		try {
			tmpName = InetAddress.getByName(this.connection.getHostname()).getHostName();
		} catch (Exception ex) {
			tmpName = this.connection.getHostname();
		}
		hostName = tmpName;
		port = this.connection.getPort();
		key = hostName + ":" + port;
		notifExecutor = SharedNotificationExecutor.getInstance();
		this.connection.addConnectionMonitor(this);
	}
	
	/**
	 * Returns the connection key
	 * @return the connection key
	 */
	public String getKey() {
		return key;
	}
	
	/**
	 * Returns the resolved host name 
	 * @return the hostName
	 */
	public String getHostName() {
		return hostName;
	}
	
	
	/**
	 * Returns the target port for this connection
	 * @return the target port for this connection
	 */
	public int getPort() {
		return port;
	}
	
	public WrappedSession getSession() {
		try {
			return new WrappedSession(connection.openSession(), this);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to open session", ex);
		}
	}
	

	/**
	 * Returns the ConnectionInfo for this connection
	 * @return the ConnectionInfo for this connection
	 * @see ch.ethz.ssh2.Connection#getConnectionInfo()
	 */
	public ConnectionInfo getConnectionInfo() {
		return connectionInfo;
	}

	/**
	 * Indicates if authentication has completed for this connection
	 * @return true if authentication has completed for this connection, false otherwise
	 * @see ch.ethz.ssh2.Connection#isAuthenticationComplete()
	 */
	public boolean isAuthenticationComplete() {
		return connection.isAuthenticationComplete();
	}
	
	

	/**
	 * Opens a new session using this connection
	 * @return a new session
	 * @throws IOException
	 * @see ch.ethz.ssh2.Connection#openSession()
	 */
	public Session openSession() throws IOException {
		return connection.openSession();
	}

	private final AtomicBoolean closing = new AtomicBoolean(false); 
	
	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.ConnectionMonitor#connectionLost(java.lang.Throwable)
	 */
	@Override
	public void connectionLost(final Throwable reason) {
		try {
			if(closing.compareAndSet(false, true)) {
				closeBroadcaster.doClose(reason);
				
				connection.close(null, false);
				
			}
		} finally {
			closing.set(false);
		}
	}
	
	public String execCommand(final String cmd) {
		return execCommand(cmd, "UTF8");
	}
	
	public String execCommand(final String cmd, final String charsetName) {
		Session session = null;
		try {
			session = connection.openSession();
			final StreamGobbler err = new StreamGobbler(session.getStderr());
			final StreamGobbler out = new StreamGobbler(session.getStdout());
			session.execCommand(cmd, charsetName);
			session.waitForCondition(ChannelCondition.EOF, authInfo.getConnectTimeout());
			final Charset charset = Charset.forName(charsetName);
			final byte[] osc = new byte[out.available()];
			out.read(osc);
			return new String(osc, charset);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to execute [" + cmd + "]", ex);
		} finally {
			if(session!=null) try { session.close(); } catch (Exception x) { /* No Op */ }
		}
	}
	

	@Override
	public void close() {
		try { connection.close(); } catch (Exception x) {/* No Op */}
		// Should call connectionLost(null)
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.io.CloseListener#onReset(java.io.Closeable)
	 */
	@Override
	public void onReset(Closeable resetCloseable) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isOpen() {		
		return connection.isConnected();
	}
	

	/**
	 * Callback when a sub-session (session or tunnel) for this connection is closed
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.io.CloseListener#onClosed(java.io.Closeable, Throwable)
	 */
	@Override
	public void onClosed(final Closeable closeable, final Throwable cause) {
		if(closeable!=null) {
			if(closeable instanceof WrappedSession) {
				
			} else if(closeable instanceof WrappedLocalPortForwarder) {
				
			}
		}		
	}

	/**
	 * Returns 
	 * @return the authInfo
	 */
	public ConnectInfo getAuthInfo() {
		return authInfo;
	}

	/**
	 * Returns the reconnect handle
	 * @return the reconnect handle
	 */
	ScheduledFuture<?> getReconnectHandle() {
		return reconnectHandle;
	}

	/**
	 * Sets the reconnect handle
	 * @param reconnectHandle the reconnect handle
	 */
	void setReconnectHandle(final ScheduledFuture<?> reconnectHandle) {
		if(reconnectHandle==null) throw new IllegalArgumentException("The passed reconnect handle was null");
		this.reconnectHandle = reconnectHandle;
	}
	
	/**
	 * Cancels the reconnect handle and sets it to null
	 */
	void cancelReconnectHandle() {
		if(reconnectHandle!=null) {
			reconnectHandle.cancel(false);
			reconnectHandle = null;
		}
	}


}
