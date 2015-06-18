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

import java.util.Map;

import javax.management.MBeanInfo;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * <p>Title: ProxySubscriptionListener</p>
 * <p>Description: Defines a subscription to the proxy subscription service</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.notifcations.ProxySubscriptionListener</code></p>
 */

public interface ProxySubscriptionListener extends NotificationListener {
	/**
	 * The ObjectNames of mbeans that matched the subscription criteria 
	 * on the subscription initialization
	 * @param initialMBeans a [possibly empty] map of the matching MBeanInfos on subscription init keyed by the ObjectName of the MBean
	 */
	public void onSubscriptionInit(final Map<ObjectName, MBeanInfo> initialMBeans);
	
	/**
	 * Callback when a new MBean is registered matching the criteria of the subscription
	 * @param objectName The ObjectName of the new MBean
	 * @param info The MBeanInfo of the registered MBean
	 */
	public void onNewMBean(final ObjectName objectName, final MBeanInfo info);
	
	/**
	 * Callback when an MBean that was in the subscription set is unregistered
	 * @param objectName The ObjectName of the unregistered MBean
	 */
	public void onUnregisteredMBean(final ObjectName objectName);
	
	
	
}
