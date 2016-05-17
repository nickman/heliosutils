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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;

/**
 * <p>Title: JMXConnectorConnectionStatus</p>
 * <p>Description: Tracks the connected status of a JMXConnector and notifies interested listeners</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.JMXConnectorConnectionStatus</code></p>
 */

public class JMXConnectorConnectionStatus implements NotificationListener, Closeable {
	/** The JMXConnector we're watching */
	protected final JMXConnector jmxConnector;
	/** Indicates if the collector has an active connection */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/** The current connection id */
	protected final AtomicReference<String> connectionId = new AtomicReference<String>(null);
	
	/**
	 * Creates a new JMXConnectorConnectionStatus
	 * @param jmxConnector The JMXConnector to watch
	 * @param autoReconnect true to auto [re-]connect when the connector is disconnected
	 * @param reconnectPeriod The reconnect period in seconds. Ignored if {@code autoReconnect} is false.
	 */
	public JMXConnectorConnectionStatus(final JMXConnector jmxConnector, final boolean autoReconnect, final long reconnectPeriod) {
		this.jmxConnector = jmxConnector;
		connected.set(isConnected());
		
	}
	
	/**
	 * Tests the JMXConnector to see if it is connected
	 * @return true if connected, false otherwise
	 */
	protected boolean isConnected() {
		try {
			jmxConnector.getConnectionId();
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
	public void handleNotification(final Notification notification, final Object handback) {
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		
	}

}
