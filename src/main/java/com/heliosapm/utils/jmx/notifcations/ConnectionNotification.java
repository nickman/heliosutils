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
package com.heliosapm.utils.jmx.notifcations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnectionNotification;

/**
 * <p>Title: ConnectionNotification</p>
 * <p>Description: Functional enumeration for JMXConnectionNotifications</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.notifcations.ConnectionNotification</code></p>
 */

public enum ConnectionNotification {
	CLOSED(JMXConnectionNotification.CLOSED),
	FAILED(JMXConnectionNotification.FAILED),
	NOTIFS_LOST(JMXConnectionNotification.NOTIFS_LOST),
	OPENED(JMXConnectionNotification.OPENED);
	
	public static final Map<String, ConnectionNotification> TYPE2ENUM;
	
	static {
		final ConnectionNotification[] values = ConnectionNotification.values();
		final Map<String, ConnectionNotification> tmp = new HashMap<String, ConnectionNotification>(values.length);
		for(ConnectionNotification cn: values) {
			tmp.put(cn.type, cn);
		}
		TYPE2ENUM = Collections.unmodifiableMap(tmp);
	}
	
	private ConnectionNotification(final String type) {
		this.type = type;
	}
	
	public final String type;
}
