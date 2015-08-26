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
import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionInfo;
import ch.ethz.ssh2.ConnectionMonitor;
import ch.ethz.ssh2.Session;

import com.heliosapm.utils.io.CloseListener;
import com.heliosapm.utils.jmx.SharedNotificationExecutor;

/**
 * <p>Title: WrappedConnection</p>
 * <p>Description: Wraps a connection, connection info and other bits and pieces of interest to a connection</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ssh.terminal.WrappedConnection</code></p>
 */

public class WrappedConnection implements ConnectionMonitor, Closeable, CloseListener<Closeable> {
	/** The wrapped connection */
	private final Connection connection;
	/** The connection info */
	private final ConnectionInfo connectionInfo;
	
	/** The resolved host name and key for this connection */
	private final String hostName;
	
	private final int port;

	/** The shared notification executor */
	private final SharedNotificationExecutor notifExecutor;
	
	private final AtomicBoolean connected = new AtomicBoolean(false);
	
	/** Externally registered monitors */
	private final Set<ConnectionMonitor> externalMonitors = new CopyOnWriteArraySet<ConnectionMonitor>();
	
	public static final WrappedConnection connect(final String hostName, final int port, final AuthInfo authInfo) {
		final Connection conn = new Connection(hostName, port);
		final WrappedConnection wconn = new WrappedConnection(conn, authInfo);
		
		return wconn;
	}
	
	
	/**
	 * Creates a new WrappedConnection
	 * @param connection The wrapped connection
	 * @param authInfo The authentication resources
	 */
	public WrappedConnection(final Connection connection, final AuthInfo authInfo) {
		if(connection==null) throw new IllegalArgumentException("The passed connection was null");
		this.connection = connection;
		String tmpName = null;
		try {
			tmpName = InetAddress.getByName(this.connection.getHostname()).getHostName();
		} catch (Exception ex) {
			tmpName = this.connection.getHostname();
		}
		hostName = tmpName;
		port = this.connection.getPort();
		try {
			connectionInfo = this.connection.getConnectionInfo();
		} catch (IOException e) {
			throw new RuntimeException("Failed to acquire ConnectionInfo for [" + hostName + "]", e);
		}
		notifExecutor = SharedNotificationExecutor.getInstance();
		this.connection.addConnectionMonitor(this);
	}
	
	/**
	 * Returns the resolved host name 
	 * @return the hostName
	 */
	public String getHostName() {
		return hostName;
	}
	
	
	public int getPort() {
		return port;
	}
	



	/**
	 * Add a ConnectionMonitor to this connection.
	 * @param cmon The connection monitor to add
	 * @see ch.ethz.ssh2.Connection#addConnectionMonitor(ch.ethz.ssh2.ConnectionMonitor)
	 */
	public void addConnectionMonitor(final ConnectionMonitor cmon) {
		if(cmon==null) throw new IllegalArgumentException("The passed connection monitor was null");
		externalMonitors.add(cmon);
	}

	/**
	 * Removes a registered connection monitor
	 * @param cmon The connection monitor to remove
	 * @return true if removed, false if did not exist
	 * @see ch.ethz.ssh2.Connection#removeConnectionMonitor(ch.ethz.ssh2.ConnectionMonitor)
	 */
	public boolean removeConnectionMonitor(final ConnectionMonitor cmon) {
		if(cmon==null) throw new IllegalArgumentException("The passed connection monitor was null");
		return externalMonitors.remove(cmon);
	}
	
	/**
	 * Delegates the connection list event to external monitors 
	 * @param reason the connection loss reason
	 */
	protected void fireConnectionMonitors(final Throwable reason) {
		if(!externalMonitors.isEmpty()) {
			for(final ConnectionMonitor cm: externalMonitors) {
				notifExecutor.execute(new Runnable(){
					public void run() {
						cm.connectionLost(reason);
					}
				});				
			}
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

	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.ConnectionMonitor#connectionLost(java.lang.Throwable)
	 */
	@Override
	public void connectionLost(final Throwable reason) {
//		log.info("Connection [{}] Closed:", hostName, reason);
		fireConnectionMonitors(reason);
	}
	
	public boolean isConnected() {
		return connected.get();
	}


	@Override
	public void onClosed(final Closeable closeable) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

}
