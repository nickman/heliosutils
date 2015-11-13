/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
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
package com.heliosapm.utils.jmx.protocol.sshjmxmp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.remote.JMXAddressable;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.generic.GenericConnector;
import javax.management.remote.generic.MessageConnection;
import javax.management.remote.message.Message;

import org.json.JSONException;
import org.json.JSONObject;

import com.heliosapm.utils.ssh.terminal.ConnectInfo;
import com.heliosapm.utils.ssh.terminal.SSHService;
import com.heliosapm.utils.ssh.terminal.WrappedStreamForwarder;
import com.sun.jmx.remote.generic.DefaultConfig;

/**
 * <p>Title: SSHTunnelMessageConnection</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jmx.remote.protocol.tunnel.SSHTunnelMessageConnection</code></p>
 */

public class SSHTunnelMessageConnection implements MessageConnection, JMXAddressable {
	/** The buffered input stream */
	protected BufferedInputStream bis = null;
	/** The buffered output stream */
	protected BufferedOutputStream bos = null;
	/** The buffered object input stream */
	private ObjectInputStream oin = null;
	/** The buffered object output strea, */
	private ObjectOutputStream oout;
	
	/** The original environment map */
	protected final Map<String, Object> originalEnv;
	/** Flag indicating the connected state */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/** The JMXServiceURL for this connection */
	protected final JMXServiceURL serviceURL;
	/** The extracted ConnectInfo */
	protected ConnectInfo connectInfo;
	
	
	/** The wrapped SSH tunnel stream forwarder */
	protected WrappedStreamForwarder wsf = null;
	/** The default class loader */
	protected ClassLoader defaultClassLoader;
	
	/** The state of this message connection */
	protected final AtomicReference<TunnelState> state = new AtomicReference<TunnelState>(TunnelState.UNCONNECTED); 
	/** The time to wait for a connected state in ms. */
	private long  waitConnectedState = 1000;
	
	/** Pattern to extract the relay host [and port] from the JMXServiceURL path */
	public static final Pattern RELAY_PATTERN = Pattern.compile("/(.*?)(?::(\\d+))??$");
	
	private final String defaultConnectionId = "Uninitialized connection id";
	
	/**
	 * Creates a new SSHTunnelMessageConnection
	 * @param serviceURL The JMXServiceURL to connect to
	 * @param env  The original environment
	 */
	public SSHTunnelMessageConnection(final JMXServiceURL serviceURL, final Map<String, Object> env) {
		
		if(serviceURL==null) throw new IllegalArgumentException("The passed JMXServiceURL was null");
		this.serviceURL = serviceURL;
		this.originalEnv = env==null ? new HashMap<String, Object>() : env;
		final String host = serviceURL.getHost();
		final int port = serviceURL.getPort();		
		
	}
	
	
	protected ConnectInfo extractConnectInfo() {
		final String uri = serviceURL.getURLPath();
		String relayHost = serviceURL.getHost();
		int relayPort = 22;
		ConnectInfo ci = null;
		if(uri!=null && !uri.trim().isEmpty()) {
			final Matcher m = RELAY_PATTERN.matcher(uri);
			if(m.matches()) {
				relayHost = m.group(1).trim();
				if(relayHost.isEmpty()) relayHost = serviceURL.getHost();
				String p = m.group(2);
				if(p!=null && !p.trim().isEmpty()) {					
					relayPort = Integer.parseInt(p.trim());
				}
			} else {
				try {
					final JSONObject json = new JSONObject(uri.substring(1));
					return ConnectInfo.fromJSON(json).setRelayHost(relayHost).setRelayPort(relayPort);
				} catch (JSONException jsex) {/* No Op */	}
			}
		}
		// Env must be in map
		return ConnectInfo.fromMap(originalEnv);
	}

	public void connect(final Map env) throws IOException {
		if(connected.compareAndSet(false, true)) {
			copyEnvs(env);
			connectInfo = extractConnectInfo();
			try {
				wsf = SSHService.getInstance()
					.connect(connectInfo.getRelayHost(serviceURL.getHost()), connectInfo.getRelayPort(), connectInfo)
					.dedicatedStreamTunnel(serviceURL.getHost(), serviceURL.getPort());
				
			} catch (Exception ex) {
				connected.set(false);
				throw new RuntimeException("Unexpected error creating stream forward to [" + connectInfo + "]", ex);
			}		
			waitConnectedState = DefaultConfig.getTimeoutForWaitConnectedState(env);
			
			state.set(TunnelState.CONNECTING);			
			if(env!=null) {
				defaultClassLoader = (ClassLoader)env.get(JMXConnectorFactory.DEFAULT_CLASS_LOADER);
			}
			bufferStreams();  // this will trigger the creation of the tunnel
			state.set(TunnelState.CONNECTED);
			checkState();			
		}
	}
	
	/**
	 * Copies the emap items from the passed map into the original environment
	 * without overwriting any existing items
	 * @param env The environment to copy from
	 */
	private void copyEnvs(final Map env) {
		if(env!=null) {
			final Map<String, Object> map = (Map<String, Object>)env; 
			final Map<String, Object> tomap = (Map<String, Object>)originalEnv;
			for(Map.Entry<String, ?> entry: map.entrySet()) {
				if(entry.getValue()!=null && !tomap.containsKey(entry.getKey())) {
					tomap.put(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	
	/**
	 * Acquires the stream forwarder raw streams, triggering a connect if they are not initialized,
	 * and buffers them
	 * @throws IOException thrown on any IO error
	 */
	protected void bufferStreams() throws IOException {
		bis = new BufferedInputStream(wsf.getInputStream());
		bos = new BufferedOutputStream(wsf.getOutputStream());
		oin = new ObjectInputStreamWithLoader(bis, defaultClassLoader);
		oout = new ObjectOutputStream(bos);
	}
	
	protected void checkState() {
	    if (state.get() == TunnelState.CONNECTED) {
	    	return;
	    } else if (state.get() == TunnelState.TERMINATED) {
	    	throw new IllegalStateException("The connection has been closed.");
	    }
	    final long waitingTime = waitConnectedState;	    
	    TunnelState.waitForState(state, waitingTime, TunnelState.CONNECTED, TunnelState.TERMINATED);
	    if (state.get() == TunnelState.CONNECTED) {
	    	return;
	    }
	    close();
		throw new IllegalStateException("The connection is not currently established.");
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.generic.MessageConnection#readMessage()
	 */
	@Override
	public Message readMessage() throws IOException, ClassNotFoundException {
		checkState();
		return (Message) oin.readObject();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.generic.MessageConnection#writeMessage(javax.management.remote.message.Message)
	 */
	@Override
	public void writeMessage(final Message msg) throws IOException {
		checkState();
		oout.writeObject(msg);
		oout.flush();
		oout.reset();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.generic.MessageConnection#close()
	 */
	@Override
	public void close() {
		if(connected.compareAndSet(true, false)) {
			state.set(TunnelState.TERMINATED);
			try { wsf.close(); } catch (Exception x) {/* No Op */}
			bis = null;
			bos = null;
			oin = null;
			oout = null;
		}
	}
	
	/**
	 * Returns the state of this message connection
	 * @return the state of this message connection
	 */
	public TunnelState getState() {
		return state.get();
	}

	
	private static final String NODEID = ManagementFactory.getRuntimeMXBean().getName();
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.generic.MessageConnection#getConnectionId()
	 */
	@Override
	public String getConnectionId() {
		if(state.get() != TunnelState.CONNECTED) {
			return defaultConnectionId;
		}
		
		StringBuilder buf = new StringBuilder("tunnel://");		
		buf.append(serviceURL.getHost()).append(":").append(serviceURL.getPort());
		buf.append("[").append(NODEID).append("]");
		buf.append(" ").append(System.identityHashCode(this));
		return buf.toString();
	}
	
	/**
	 * <p>Title: ObjectInputStreamWithLoader</p>
	 * <p>Description: Classloader enabled object inout stream</p>
	 * <p>Copied from: {@link com.sun.jmx.remote.socket.SocketConnection}</p>
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.jmx.remote.protocol.tunnel.SSHTunnelMessageConnection.ObjectInputStreamWithLoader</code></p>
	 */
	private static class ObjectInputStreamWithLoader extends ObjectInputStream {
		
		/**
		 * Creates a new ObjectInputStreamWithLoader
		 * @param in The underlying input stream
		 * @param cl The specified class loader
		 * @throws IOException thrown on any IO errors
		 */
		public ObjectInputStreamWithLoader(InputStream in, ClassLoader cl) throws IOException {
			super(in);
			this.cloader = cl;
		}

		protected Class<?> resolveClass(ObjectStreamClass aClass) throws IOException, ClassNotFoundException {
			return cloader == null ? super.resolveClass(aClass) : Class.forName(aClass.getName(), false, cloader);
		}

		private final ClassLoader cloader;
	}

	@Override
	public JMXServiceURL getAddress() {
		return serviceURL;
	}
	

}
