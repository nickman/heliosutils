/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package com.heliosapm.utils.jmx;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.heliosapm.utils.io.CloseableService;
import com.heliosapm.utils.io.StdInCommandHandler;

import jsr166e.LongAdder;

/**
 * <p>Title: JMXConnectorConnectionStatus</p>
 * <p>Description: Tracks the connected status of a JMXConnector and notifies interested listeners</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.JMXConnectorConnectionStatus</code></p>
 */

public class JMXConnectorConnectionStatus extends NotificationBroadcasterSupport implements JMXConnectorConnectionStatusMBean, NotificationListener, Closeable, Runnable, MBeanRegistration {
	/** The JMXConnector we're watching */
	protected final JMXConnector jmxConnector;
	/** The optional environment to connect with */
	protected final Map<String,?> env;
	/** Indicates if the collector has an active connection */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/** The current connection id */
	protected final AtomicReference<String> connectionId = new AtomicReference<String>(null);
	/** Indicates if a reconnect task is scheduled */
	protected final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);	
	/** The last connect timestamp */
	protected final AtomicLong lastConnectTime = new AtomicLong(0L);
	/** The last disconnect timestamp */
	protected final AtomicLong lastDisconnectTime = new AtomicLong(0L);
	/** The last connect failure */
	protected final AtomicLong lastConnectFail = new AtomicLong(0L);
	/** The consecutive reconnect fail count */
	protected final LongAdder connectionFailures = new LongAdder();
	/** The most recent reconnect exception message */
	protected String connectErrorMessage = null;
	
	/** Indicates if this watcher should attempt to connect if started disconnected and attempt periodic reconnects on disconnect */
	protected final boolean autoReconnect;
	
	/** The connect event count */
	protected final LongAdder connectCount = new LongAdder();
	/** The disconnect event count */
	protected final LongAdder disconnectCount = new LongAdder();
	/** The registered listeners */
	protected final Set<JMXConnectorListener> listeners = new CopyOnWriteArraySet<JMXConnectorListener>();
	/** The reconnect schedule handle */
	protected ScheduledFuture<?> scheduleHandle = null;
	/** The reconnect period in seconds */
	protected final long reconnectPeriod;	
	/** Indicates if a connect attempt is running */
	protected final AtomicBoolean reconnectRunning = new AtomicBoolean(false);
	/** The ObjectName for the management interface */
	protected final ObjectName objectName;
	
	private static final MBeanNotificationInfo[] NOTIF_INFOS = new MBeanNotificationInfo[] {
		new MBeanNotificationInfo(new String[]{JMXConnectionNotification.OPENED}, JMXConnectionNotification.class.getName(), "A new client connection has been opened."),
		new MBeanNotificationInfo(new String[]{JMXConnectionNotification.CLOSED}, JMXConnectionNotification.class.getName(), "A client connection has been closed."),
		new MBeanNotificationInfo(new String[]{JMXConnectionNotification.FAILED}, JMXConnectionNotification.class.getName(), "A client connection has failed.")
	};
	
	public static void main(String[] args) {
		log("JMXConnectorConnectionStatus Test");
		final JMXConnectorConnectionStatus status = new JMXConnectorConnectionStatus(
				"service:jmx:attach:///[.*GroovyStarter.*]", null, true, 5, JMXHelper.objectName("com.heliosapm.jmx.connectors:target=Groovy"));
		JMXHelper.fireUpJMXMPServer(4545);
		StdInCommandHandler.getInstance().run();
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Creates a new JMXConnectorConnectionStatus
	 * @param jmxConnector The JMXConnector to watch
	 * @param env The optional environment to connect with 
	 * @param autoReconnect true to auto [re-]connect when the connector is disconnected
	 * @param reconnectPeriod The reconnect period in seconds. Ignored if {@code autoReconnect} is false.
	 * @param objectName The ObjectName to register the managtement interface with. If null, the mbean will not be registered.
	 */
	public JMXConnectorConnectionStatus(final JMXConnector jmxConnector, final Map<String,?> env, final boolean autoReconnect, final long reconnectPeriod, final ObjectName objectName) {
		super(SharedNotificationExecutor.getInstance(), NOTIF_INFOS);
		this.jmxConnector = jmxConnector;
		this.env = env;
		this.autoReconnect = autoReconnect;
		this.objectName = objectName;
		this.reconnectPeriod = reconnectPeriod;
		final boolean conn = _isConnected();		
		connected.set(conn);
		if(conn) {
			lastConnectTime.set(System.currentTimeMillis());
		} else {
			if(!autoReconnect) throw new IllegalStateException("The passed connector was not connected and autoReconnect was false");
			startReconnect();
		}
		this.jmxConnector.addConnectionNotificationListener(this, null, null);		
		CloseableService.getInstance().register(this);
		if(this.objectName!=null) {
			if(JMXHelper.isRegistered(objectName)) {
				try {
					JMXHelper.unregisterMBean(objectName);
				} catch (Exception ex) {/* No Op */}
			}
			try {
				JMXHelper.registerMBean(this, objectName);
			} catch (Exception ex) {
				System.err.println("Failed to register JMXConnectorConnectionStatusMBean with ObjectName [" + objectName + "]");
				ex.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 * Creates a new JMXConnector from the passed JMXServiceURL and then a JMXConnectorConnectionStatus to watch it.
	 * @param jmxUrl The JMXConnector endpoint to create the connector for and then watch
	 * @param env The optional environment to connect with 
	 * @param autoReconnect true to auto [re-]connect when the connector is disconnected
	 * @param reconnectPeriod The reconnect period in seconds. Ignored if {@code autoReconnect} is false.
	 * @param objectName The ObjectName to register the managtement interface with. If null, the mbean will not be registered.
	 */
	public JMXConnectorConnectionStatus(final JMXServiceURL jmxUrl, final Map<String,?> env, final boolean autoReconnect, final long reconnectPeriod, final ObjectName objectName) {
		this(connector(jmxUrl, env, autoReconnect), env, autoReconnect, reconnectPeriod, objectName);
	}
	
	/**
	 * Creates a new JMXConnector from the passed JMXServiceURL string  and then a JMXConnectorConnectionStatus to watch it.
	 * @param jmxUrl The JMXConnector endpoint to create the connector for and then watch
	 * @param env The optional environment to connect with 
	 * @param autoReconnect true to auto [re-]connect when the connector is disconnected
	 * @param reconnectPeriod The reconnect period in seconds. Ignored if {@code autoReconnect} is false.
	 * @param objectName The ObjectName to register the managtement interface with. If null, the mbean will not be registered.
	 */
	public JMXConnectorConnectionStatus(final String jmxUrl, final Map<String,?> env, final boolean autoReconnect, final long reconnectPeriod, final ObjectName objectName) {
		this(connector(jmxUrl, env, autoReconnect), env, autoReconnect, reconnectPeriod, objectName);
	}
	
	private static JMXConnector connector(final String jmxUrl, final Map<String,?> env, final boolean autoReconnect) {
		try {
			return connector(new JMXServiceURL(jmxUrl), env, autoReconnect);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Failed to create JMXServiceURL from [" + jmxUrl + "]", ex);
		}
	}
	private static JMXConnector connector(final JMXServiceURL jmxUrl, final Map<String,?> env, final boolean autoReconnect) {
		JMXConnector connector = null;
		try {
			connector = JMXConnectorFactory.newJMXConnector(jmxUrl, env);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create a JMXConnector from [" + jmxUrl + "]", ex);
		}
		if(!autoReconnect) {
			try {
				connector.connect(env);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to connect JMXConnector for [" + jmxUrl + "] and autoReconnect was false", ex);
			}			
		}
		return connector;
			
	}
	
	
	/**
	 * Starts the reconnect if not already started
	 */
	protected void startReconnect() {
		if(reconnectScheduled.compareAndSet(false, true)) {
			final Runnable r = this;
			scheduleHandle = SharedScheduler.getInstance().scheduleWithFixedDelay(new Runnable(){
				@Override
				public void run() {
					SharedExecutionExecutor.getInstance().execute(r);
				}
			}, reconnectPeriod, reconnectPeriod, TimeUnit.SECONDS);
		}
	}
	
	/**
	 * <p>The reconnect task</p>
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if(reconnectRunning.compareAndSet(false, true)) {
			try {
				jmxConnector.connect(env);
				connected.set(true);
				try { 
					scheduleHandle.cancel(false);
					reconnectScheduled.set(false);
				} catch (Exception x) {/* No Op */}
				
				connectCount.increment();
				lastConnectTime.set(System.currentTimeMillis());
				connectionFailures.reset();
			} catch (Exception ex) {
				connectErrorMessage = ex.getMessage();
				lastConnectFail.set(System.currentTimeMillis());
				connectionFailures.increment();
			} finally {
				reconnectRunning.set(false);
			}
		} 
	}
	
	
	/**
	 * Registers a listener to be notified of connect/disconnect events
	 * @param listener the listener to add
	 */
	public void addListener(final JMXConnectorListener listener) {
		if(listener!=null) listeners.add(listener);
	}
	
	/**
	 * Removes a registered listener
	 * @param listener the listener to remove
	 */
	public void removeListener(final JMXConnectorListener listener) {
		if(listener!=null) listeners.remove(listener);
	}
	
	
	
	/**
	 * Tests the JMXConnector to see if it is connected
	 * @return true if connected, false otherwise
	 */
	private boolean _isConnected() {
		try {
			connectionId.set(jmxConnector.getConnectionId());
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(final Notification n, final Object handback) {
		if(n instanceof JMXConnectionNotification) {
			final JMXConnectionNotification cn = (JMXConnectionNotification)n;
			sendNotification(cn);
			final String type = cn.getType();
			if(JMXConnectionNotification.OPENED.equals(type)) {
				connected.set(true);
				fireConnect(cn);				
			} else if(JMXConnectionNotification.CLOSED.equals(type)) {
				connected.set(false);
				lastDisconnectTime.set(System.currentTimeMillis());
				disconnectCount.increment();
				fireDisconnect(cn, false);		
				startReconnect();
			} else if(JMXConnectionNotification.FAILED.equals(type)) {
				connected.set(false);
				lastDisconnectTime.set(System.currentTimeMillis());
				disconnectCount.increment();
				fireDisconnect(cn, true);
				startReconnect();
			}
		}		
	}
	
	/**
	 * Returns an MBeanServerConnection from this managed connector
	 * @return an MBeanServerConnection from this managed connector
	 * @throws IOException thrown on an error acquiring the MBeanServerConnection or if we're disconnected.
	 */
	@Override
	public MBeanServerConnection getServer() throws IOException {
		return jmxConnector.getMBeanServerConnection();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		if(scheduleHandle!=null) {
			scheduleHandle.cancel(false);
		}
		try { jmxConnector.close(); } catch (Exception x) {/* No Op */}
		try { jmxConnector.removeConnectionNotificationListener(this); } catch (Exception x) {/* No Op */}
		if(objectName!=null) {
			if(JMXHelper.isRegistered(objectName)) {
				try { JMXHelper.unregisterMBean(objectName); } catch (Exception x) {/* No Op */}
			}
		}
	}
	
	/**
	 * <p>Title: JMXConnectorListener</p>
	 * <p>Description: Defines a listener to be notified of JMXConnector connect and disconnect events</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.jmx.JMXConnectorConnectionStatus.JMXConnectorListener</code></p>
	 */
	public static interface JMXConnectorListener {
		/**
		 * Callback when the connection is established
		 * @param seq The connection incident count for this connector 
		 * @param nseq The notification event sequence id
		 * @param timestamp The timestamp of the connect
		 * @param connectionId The connection id
		 * @param message The event message
		 * @param server The new MBeanServerConnection now that we're connected
		 */
		public void onConnect(final long seq, final long nseq, final long timestamp, final String connectionId, final String message, final MBeanServerConnection server);
		
		/**
		 * Callback when the connection is lost
		 * @param fail true if the disconnect was not requested, false if it was
		 * @param seq The disconnect incident count for this connector 
		 * @param nseq The notification event sequence id
		 * @param timestamp The timestamp of the disconnect
		 * @param connectionId The connection id
		 * @param message The event message
		 */
		public void onDisconnect(final boolean fail, final long seq, final long nseq, final long timestamp, final String connectionId, final String message);
	}
	
	/**
	 * Fires a connect event against all registered listeners
	 * @param notif The connect event
	 */
	protected void fireConnect(final JMXConnectionNotification notif) {
		if(listeners.isEmpty()) return; 
		for(final JMXConnectorListener listener: listeners) {
			SharedNotificationExecutor.getInstance().execute(new Runnable(){
				@Override
				public void run() {
					try {
						MBeanServerConnection server = jmxConnector.getMBeanServerConnection();
						listener.onConnect(connectCount.longValue(), notif.getSequenceNumber(), notif.getTimeStamp(), notif.getConnectionId(), notif.getMessage(), server);
					} catch (Exception ex) {
						// FIXME:  exception triggered disconnect
					}
				}					
			});
		}		
	}
	
	/**
	 * Fires a disconnect event against all registered listeners
	 * @param notif The disconnect event
	 * @param fail true if the disconnect was a failure, false if it was a sanctioned close
	 */
	protected void fireDisconnect(final JMXConnectionNotification notif, final boolean fail) {
		if(listeners.isEmpty()) return; 
		for(final JMXConnectorListener listener: listeners) {
			SharedNotificationExecutor.getInstance().execute(new Runnable(){
				@Override
				public void run() {
					listener.onDisconnect(fail, disconnectCount.longValue(), notif.getSequenceNumber(), notif.getTimeStamp(), notif.getConnectionId(), notif.getMessage());
				}					
			});
		}
	}

	/**
	 * Returns the JMXConnector
	 * @return the jmxConnector
	 */
	@Override
	public JMXConnector getJmxConnector() {
		return jmxConnector;
	}

	/**
	 * Indicates if the JMXConnector is connected
	 * @return true if the JMXConnector is connected, false otherwise
	 */
	@Override
	public boolean isConnected() {
		return connected.get();
	}

	/**
	 * Returns the date of the last connect, or null if a connect has never been observed
	 * @return the date of the last connect, or null if a connect has never been observed
	 */
	@Override
	public Date getLastConnectTime() {
		final long ts = lastConnectTime.get();
		return ts==-1L ? null : new Date(ts);		
	}

	/**
	 * Returns the date of the last disconnect, or null if a disconnect has never been observed
	 * @return the date of the last disconnect, or null if a disconnect has never been observed
	 */
	@Override
	public Date getLastDisconnectTime() {
		final long ts = lastDisconnectTime.get();
		return ts==-1L ? null : new Date(ts);		
	}
	
	/**
	 * Returns the date of the last connect attempt fail, or null if a connect attempt has never failed
	 * @return the date of the last connect attempt fail, or null if a connect attempt has never failed
	 */
	@Override
	public Date getLastConnectFailTime() {
		final long ts = lastConnectFail.get();
		return ts==-1L ? null : new Date(ts);		
	}
	

	/**
	 * Returns the total cummulative count of connect events 
	 * @return the total cummulative count of connect events
	 */
	@Override
	public long getConnectCount() {
		return connectCount.longValue();
	}

	/**
	 * Returns the total cummulative count of disconnect events 
	 * @return the total cummulative count of disconnect events
	 */
	@Override
	public long getDisconnectCount() {
		return disconnectCount.longValue();
	}

	/**
	 * Indicates if a reconnect is running
	 * @return true if a reconnect is running, false othwerwise
	 */
	@Override
	public boolean getReconnectRunning() {
		return reconnectRunning.get();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	@Override
	public ObjectName preRegister(final MBeanServer server, final ObjectName name) throws Exception {
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
	 */
	@Override
	public void postRegister(final Boolean registrationDone) {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	@Override
	public void preDeregister() throws Exception {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postDeregister()
	 */
	@Override
	public void postDeregister() {
		try { close(); } catch (Exception x) {/* No Op */}		
	}

	/**
	 * Returns the reconnect period in seconds
	 * @return the reconnect period in seconds
	 */
	@Override
	public long getReconnectPeriod() {
		return reconnectPeriod;
	}

	/**
	 * Indicates if auto reconnect is enabled
	 * @return true if auto reconnect is enabled, false otherwise
	 */
	@Override
	public boolean isAutoReconnect() {
		return autoReconnect;
	}

	/**
	 * Indicates if a reconnect attempt is scheduled
	 * @return true if a reconnect attempt is scheduled, false otherwise
	 */
	@Override
	public boolean isReconnectScheduled() {
		return reconnectScheduled.get();
	}

	/**
	 * Returns the cummulative number of reconnect failures. Reset on successful connect. 
	 * @return the cummulative number of reconnect failures
	 */
	@Override
	public long getConnectionFailures() {
		return connectionFailures.longValue();
	}

	/**
	 * Returns the most recent reconnect error message
	 * @return the most recent reconnect error message
	 */
	@Override
	public String getLastConnectErrorMessage() {
		return connectErrorMessage;
	}

}
