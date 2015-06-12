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
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanInfo;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;

/**
 * <p>Title: ProxySubscription</p>
 * <p>Description: Defines a proxy subscription managed by the {@link ProxySubscriptionService}
 * on behalf of a subscriber.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.notifcations.ProxySubscription</code></p>
 */

public class ProxySubscription {
	/** The subscription's ObjectName criteria */
	protected final ObjectName objectName;
	/** The subscription's query criteria */
	protected final QueryExp query;
	/** The subscription's listener */
	protected final NotificationListener listener;
	/** The upcast proxy subscription listener if applicable */
	protected final ProxySubscriptionListener psl;
	
	/** The subscription listener's notification filter */
	protected final NotificationFilter filter;
	/** The subscription's handback object */
	protected final Object handback;
	/** A map of currently active ObjectNames currently in the subscription's criteria window */
	protected final Map<ObjectName, MBeanInfo> currentlySubscribed = new ConcurrentHashMap<ObjectName, MBeanInfo>();
	
	/**
	 * Creates a new ProxySubscription
	 * @param objectName The ObjectName to subscribe to events for (should be a pattern, but will not fail if it is not). Can be null. If query is null as well,
	 * will subscribe to all MBeans.
	 * @param query The query expression to be applied for selecting MBeans. If null no query expression will be applied for selecting MBeans.
	 * @param listener The listener to be notified on a matching notification 
	 * @param filter An optional notification filter
	 * @param handback The optional handback
	 */
	public ProxySubscription(final ObjectName objectName, final QueryExp query, final NotificationListener listener, final NotificationFilter filter, final Object handback) {
		this.objectName = objectName;
		this.query = query;
		this.listener = listener;
		if(listener instanceof ProxySubscriptionListener) {
			psl = (ProxySubscriptionListener)listener;
		} else {
			psl = null;
		}
		this.filter = filter;
		this.handback = handback;
	}
	

}
