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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;

import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.jmx.SharedNotificationExecutor;

/**
 * <p>Title: ProxySubscriptionService</p>
 * <p>Description: Service to allow notification subscribers to listen on generalized subscription definitions
 * such as notifications from MBeans that have not been registered yet.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.notifcations.ProxySubscriptionService</code></p>
 */

public class ProxySubscriptionService extends NotificationBroadcasterSupport implements NotificationListener {

	private static final MBeanNotificationInfo[] MBEAN_INFOS = new MBeanNotificationInfo[] {
		
	};
	
	/** The default ObjectName for this service */
	public static final ObjectName DEFAULT_OBJECT_NAME = JMXHelper.objectName("com.heliosapm.notifications:service=ProxySubscriptionService");
	
	/** the MBeanServer where the service is registered */
	protected final MBeanServer server;
	/** the ObjectName of the service */
	protected final ObjectName objectName;
	
	/** A set of subscribed full proxy listeners */
	protected final Map<NotificationListener, ProxySubscription> proxyListeners = new ConcurrentHashMap<NotificationListener, ProxySubscription>();
	/** A map to associate an ObjetcName based listener with the listener dynamic invoker */
	protected final Map<ObjectName, NotificationListener> objectNameListeners = new ConcurrentHashMap<ObjectName, NotificationListener>();
	
	
	
	/**
	 * Creates a new ProxySubscriptionService
	 * @param server The MBeanServer where this service should be registered. If null,
	 * registers with the Helios MBeanServer. 
	 * @param objectName The ObjectName under which this subscripton service should be registered.
	 * If null, uses the {@link ProxySubscriptionService#DEFAULT_OBJECT_NAME}
	 */
	public ProxySubscriptionService(final MBeanServer server, final ObjectName objectName) {
		super(SharedNotificationExecutor.getInstance(), MBEAN_INFOS);
		this.server = server;
		this.objectName = objectName;
		JMXHelper.registerMBean(server, this, objectName);
		JMXHelper.addNotificationListener(server, MBeanServerDelegate.DELEGATE_NAME, objectName, null, null);
	}

	/**
	 * Creates a new ProxySubscriptionService registered with the Helios MBeanServer
	 * using the default ObjectName {@link ProxySubscriptionService#DEFAULT_OBJECT_NAME}
	 */
	public ProxySubscriptionService() {
		this(null, null);
	}
	
	/**
	 * Starts a new proxy subscription
	 * @param objectName The ObjectName to subscribe to events for (should be a pattern, but will not fail if it is not). Can be null. If query is null as well,
	 * will subscribe to all MBeans.
	 * @param query The query expression to be applied for selecting MBeans. If null no query expression will be applied for selecting MBeans.
	 * @param listener The listener to be notified on a matching notification 
	 * @param filter An optional notification filter
	 * @param handback The optional handback
	 */
	public void subscribe(final ObjectName objectName, final QueryExp query,  final NotificationListener listener, final NotificationFilter filter, final Object handback) {
		if(listener==null) throw new IllegalArgumentException("The passed listener was null");
		if(proxyListeners.containsKey(listener)) throw new RuntimeException("Listener already subscribed. ProxySubscriptionService only allows listeners to subscribe once");
		final boolean isPSL = listener instanceof ProxySubscriptionListener;
		if(query!=null) {
			query.setMBeanServer(server);
		}
		final ProxySubscription ps = new ProxySubscription(objectName, query, listener, filter, handback);		
		proxyListeners.put(listener, ps);		
		if(isPSL) {
			((ProxySubscriptionListener)listener).onSubscriptionInit(getMBeanInfoMap(objectName, query));
		}
	}
	
	/**
	 * Starts a new proxy subscription
	 * @param objectName The ObjectName to subscribe to events for (should be a pattern, but will not fail if it is not). Can be null. If query is null as well,
	 * will subscribe to all MBeans.
	 * @param query The query expression to be applied for selecting MBeans. If null no query expression will be applied for selecting MBeans.
	 * @param listener The ObjectName of a registered listener to be notified on a matching notification 
	 * @param filter An optional notification filter
	 * @param handback The optional handback
	 */
	public void subscribe(final ObjectName objectName, final QueryExp query,  final ObjectName listener, final NotificationFilter filter, final Object handback) {
		if(listener==null) throw new IllegalArgumentException("The passed listener ObjectName was null");
		if(objectNameListeners.containsKey(objectName)) throw new RuntimeException("Listener already subscribed. ProxySubscriptionService only allows listeners to subscribe once");
		
		final boolean isPSL = JMXHelper.isInstanceOf(objectName, ProxySubscriptionListener.class.getName());
		if(query!=null) {
			query.setMBeanServer(server);
		}

		final NotificationListener objectNameListener;
		if(isPSL) {
			objectNameListener = MBeanServerInvocationHandler.newProxyInstance(server, listener, ProxySubscriptionListener.class, JMXHelper.isInstanceOf(server, listener, NotificationBroadcaster.class.getName()));			
		} else {
			objectNameListener = MBeanServerInvocationHandler.newProxyInstance(server, listener, NotificationListener.class, JMXHelper.isInstanceOf(server, listener, NotificationBroadcaster.class.getName()));	
		}
		final ProxySubscription ps = new ProxySubscription(objectName, query, objectNameListener, filter, handback, listener);
		objectNameListeners.put(listener, objectNameListener);
		proxyListeners.put(objectNameListener, ps);
		if(isPSL) {
			((ProxySubscriptionListener)objectNameListener).onSubscriptionInit(getMBeanInfoMap(objectName, query));
		}
	}
	
	/**
	 * Returns a map of MBeanInfos keyed by the ObjectName for MBeans matching the passed ObjectName and Query
	 * @param objectName The object name to query
	 * @param query The query to match against
	 * @return the [possibly empty] map of MBeanInfos.
	 */
	protected Map<ObjectName, MBeanInfo> getMBeanInfoMap(final ObjectName objectName, final QueryExp query) {
		final Set<ObjectName> matchingObjectNames = server.queryNames(objectName, query);
		if(matchingObjectNames.isEmpty()) return Collections.emptyMap();
		Map<ObjectName, MBeanInfo> map = new HashMap<ObjectName, MBeanInfo>(matchingObjectNames.size());
		for(ObjectName on: matchingObjectNames) {
			map.put(on, JMXHelper.getMBeanInfo(server, on));
		}
		return map;
	}
	
	/**
	 * cancels the subscription for the passed listener
	 * @param listener the listener to cancel the subscription for
	 */
	public void unsubscribe(final NotificationListener listener) {
		if(listener!=null) {
			final ProxySubscription ps = proxyListeners.remove(listener);
			if(ps!=null && ps.objectNameListener!=null) {
				objectNameListeners.remove(ps.objectNameListener);
			}
		}
	}
	

	/**
	 * <p>Handles notifications regarding new MBean registrations and unregistrations.
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(final Notification notification, final Object handback) {
		if(proxyListeners.isEmpty()) return;
		if(notification!=null) {
			if(notification instanceof MBeanServerNotification) {
				handleMBeanServerNotification((MBeanServerNotification)notification);
			}
			final Object source = notification.getSource();
			final ObjectName objectName = (source!=null && (source instanceof ObjectName)) ? ((ObjectName)source) : null;
			for(ProxySubscription ps: proxyListeners.values()) {
				ps.onNotification(notification, handback, objectName);
			}
		}		
	}
	
	/**
	 * Handles MBeanServerNotifications
	 * @param mbsn The mbean server notification
	 */
	protected void handleMBeanServerNotification(final MBeanServerNotification mbsn) {
		if(proxyListeners.isEmpty()) return;
		final boolean reg = MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(mbsn.getType());
		final ObjectName objectName = mbsn.getMBeanName();
		final MBeanInfo info = reg ? JMXHelper.getMBeanInfo(server, objectName) : null;
		if(reg) {
			for(ProxySubscription ps: proxyListeners.values()) {
				ps.onMBeanRegistration(objectName, info);
			}
		} else {
			NotificationListener onlis = objectNameListeners.remove(objectName);
			if(onlis!=null) {
				proxyListeners.remove(onlis);
			}
			for(ProxySubscription ps: proxyListeners.values()) {
				ps.onMBeanUnRegistration(objectName);
			}
		}
	}
	
	


}
