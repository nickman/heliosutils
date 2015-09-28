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

import java.util.concurrent.atomic.AtomicLong;

import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

/**
 * <p>Title: DelegateNotificationBroadcaster</p>
 * <p>Description: Defines a {@link NotificationBroadcaster} that contributes its MBean notification infos 
 * and delegates notification broadcast to an aggregating notification broadcaster.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.notifcations.DelegateNotificationBroadcaster</code></p>
 */

public interface DelegateNotificationBroadcaster extends NotificationBroadcaster {
	/**
	 * Accepts the "parent" NotificationBroadcaster and delegates its notification broadcasts to it
	 * @param notificationBroadcaster the "parent" NotificationBroadcaster 
	 * @param objectName The ObjectName of the parent
	 * @param notifSerial The notification serial number factory
	 */
	public void setNotificationBroadcaster(final NotificationBroadcasterSupport notificationBroadcaster, final ObjectName objectName, final AtomicLong notifSerial);
}
