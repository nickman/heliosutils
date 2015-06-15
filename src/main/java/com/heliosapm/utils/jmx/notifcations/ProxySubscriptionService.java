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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

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
	protected final Set<ProxySubscriptionListener> fullProxyListeners = new CopyOnWriteArraySet<ProxySubscriptionListener>();
	/** A set of subscribed notification listeners */
	protected final Set<NotificationListener> allListeners = new CopyOnWriteArraySet<NotificationListener>();
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
		if(allListeners.contains(listener)) throw new RuntimeException("Listener already subscribed. ProxySubscriptionService only allows listeners to subscribe once");
		final boolean isPSL = listener instanceof ProxySubscriptionListener;
		if(query!=null) {
			query.setMBeanServer(server);
		}
		allListeners.add(listener);
		if(isPSL) {
			final ProxySubscriptionListener psl = (ProxySubscriptionListener)listener;
			this.fullProxyListeners.add(psl);
			psl.onSubscriptionInit(server.queryNames(objectName, query).toArray(new ObjectName[0]));
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
		if(allListeners.contains(listener)) throw new RuntimeException("Listener already subscribed. ProxySubscriptionService only allows listeners to subscribe once");
		
		final boolean isPSL = JMXHelper.isInstanceOf(objectName, ProxySubscriptionListener.class.getName());
		final NotificationListener objectNameListener;
		if(isPSL) {
			objectNameListener = MBeanServerInvocationHandler.newProxyInstance(server, listener, ProxySubscriptionListener.class, JMXHelper.isInstanceOf(server, listener, NotificationBroadcaster.class.getName()));
		} else {
			objectNameListener = MBeanServerInvocationHandler.newProxyInstance(server, listener, NotificationListener.class, JMXHelper.isInstanceOf(server, listener, NotificationBroadcaster.class.getName()));	
		}
		objectNameListeners.put(listener, objectNameListener);
		subscribe(objectName, query, objectNameListener, filter, handback);		
	}
	
	/**
	 * cancels the subscription for the passed listener
	 * @param listener the listener to cancel the subscription for
	 */
	public void unsubscribe(final NotificationListener listener) {
		allListeners.remove(listener);
		fullProxyListeners.remove(listener);
	}
	

	/**
	 * <p>Handles notifications regarding new MBean registrations and unregistrations.
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(final Notification notification, final Object handback) {
		if(notification!=null) {
			if(notification instanceof MBeanServerNotification) {
				if(!fullProxyListeners.isEmpty()) {
					handleMBeanServerNotification((MBeanServerNotification)notification);
				}
			}
		}		
	}
	
	/**
	 * Handles MBeanServerNotifications
	 * @param mbsn The mbean server notification
	 */
	protected void handleMBeanServerNotification(final MBeanServerNotification mbsn) {
		final boolean reg = MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(mbsn.getType());
		final ObjectName objectName = mbsn.getMBeanName();
		final MBeanInfo info = reg ? JMXHelper.getMBeanInfo(server, objectName) : null;
		if(reg) {
			for(ProxySubscriptionListener psl: fullProxyListeners) {
				psl.onNewMBean(objectName, info);
			}
		} else {
			for(ProxySubscriptionListener psl: fullProxyListeners) {
				psl.onUnregisteredMBean(objectName);
			}			
		}
	}


}
