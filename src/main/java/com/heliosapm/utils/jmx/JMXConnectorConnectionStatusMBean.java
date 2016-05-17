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

import java.io.IOException;
import java.util.Date;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;

import com.heliosapm.utils.jmx.JMXConnectorConnectionStatus.JMXConnectorListener;

/**
 * <p>Title: JMXConnectorConnectionStatusMBean</p>
 * <p>Description: The mbean interface for {@link JMXConnectorConnectionStatus} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.JMXConnectorConnectionStatusMBean</code></p>
 */

public interface JMXConnectorConnectionStatusMBean {
	/**
	 * Returns an MBeanServerConnection from this managed connector
	 * @return an MBeanServerConnection from this managed connector
	 * @throws IOException thrown on an error acquiring the MBeanServerConnection or if we're disconnected.
	 */
	public MBeanServerConnection getServer() throws IOException;
	
	
	/**
	 * Closes this connector
	 * @throws IOException on any error closing
	 */
	public void close() throws IOException;

	/**
	 * Returns the JMXConnector
	 * @return the jmxConnector
	 */
	public JMXConnector getJmxConnector();

	/**
	 * Indicates if the JMXConnector is connected
	 * @return true if the JMXConnector is connected, false otherwise
	 */
	public boolean isConnected();

	/**
	 * Returns the date of the last connect, or null if a connect has never been observed
	 * @return the date of the last connect, or null if a connect has never been observed
	 */
	public Date getLastConnectTime();

	/**
	 * Returns the date of the last disconnect, or null if a disconnect has never been observed
	 * @return the date of the last disconnect, or null if a disconnect has never been observed
	 */
	public Date getLastDisconnectTime();
	
	/**
	 * Returns the date of the last connect attempt fail, or null if a connect attempt has never failed
	 * @return the date of the last connect attempt fail, or null if a connect attempt has never failed
	 */
	public Date getLastConnectFailTime();
	

	/**
	 * Returns the total cummulative count of connect events 
	 * @return the total cummulative count of connect events
	 */
	public long getConnectCount();

	/**
	 * Returns the total cummulative count of disconnect events 
	 * @return the total cummulative count of disconnect events
	 */
	public long getDisconnectCount();

	/**
	 * Indicates if a reconnect is running
	 * @return true if a reconnect is running, false othwerwise
	 */
	public boolean getReconnectRunning();
	
	/**
	 * Returns the reconnect period in seconds
	 * @return the reconnect period in seconds
	 */
	public long getReconnectPeriod();
	
	/**
	 * Indicates if auto reconnect is enabled
	 * @return true if auto reconnect is enabled, false otherwise
	 */
	public boolean isAutoReconnect();
	
	/**
	 * Indicates if a reconnect attempt is scheduled
	 * @return true if a reconnect attempt is scheduled, false otherwise
	 */
	public boolean isReconnectScheduled();
	
	/**
	 * Returns the cummulative number of reconnect failures. Reset on successful connect. 
	 * @return the cummulative number of reconnect failures
	 */
	public long getConnectionFailures();

	/**
	 * Returns the most recent reconnect error message
	 * @return the most recent reconnect error message
	 */
	public String getLastConnectErrorMessage();	
}
