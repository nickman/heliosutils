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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;
import javax.management.StringValueExp;

import org.cliffc.high_scale_lib.NonBlockingHashSet;

import com.heliosapm.utils.jmx.SharedNotificationExecutor;

/**
 * <p>Title: Subscription</p>
 * <p>Description: Represents a unique multiplexed listener. When this listener is registered, it will be automatically
 * added as a notification listener to all MBeans responding to a {@link MBeanServer#queryMBeans(ObjectName, QueryExp)}.
 * In addition, if any new MBeans are registered that meet this criteria, this listener will be added as a listener on those MBeans as well (assuming the MBeans implement 
 * {@link NotificationBroadcaster}. By the same token, if a subscribed MBean is unregistered, this listener will be automatically removed.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.notifcations.Subscription</code></p>
 */

public class Subscription extends NotificationBroadcasterSupport implements SubscriptionMBean, NotificationListener, NotificationFilter, MBeanRegistration {
	/** The subscription's ObjectName criteria */
	protected final ObjectName objectName;
	/** The subscription's query criteria */
	protected final QueryExp query;
	/** The subscription's listener */
	protected final NotificationListener listener;
	/** The subscription's filter */
	protected final NotificationFilter filter;
	/** The subscription's handback */
	protected final Object handback;
	/** The MBeanServer we've been registered with */
	protected MBeanServer server = null;
	/** The ObjectName we've been registered under */
	protected ObjectName myObjectName = null;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass().getName());
	/** The started indicator */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	/** The notification serial */
	protected final AtomicLong notifSerial = new AtomicLong(0L);
	
	/** Query exp to filter out MBeans that are not notification broadcasters */
	protected static final QueryExp NOTIF_BCASTER_QUERYEXP = Query.isInstanceOf(new StringValueExp(NotificationEmitter.class.getName()));
	
	/** We've registered listeners against all these ObjectNames */
	protected final NonBlockingHashSet<ObjectName> subscribed = new NonBlockingHashSet<ObjectName>();
	

	/** This MBean's notification info */
	private static final MBeanNotificationInfo[] NOTIF_INFOS = {
		new MBeanNotificationInfo(new String[]{NOTIF_TYPE_INITIAL_SUBS}, Notification.class.getName(), "Notification issued when a subscription has started. Contains the set of subscribed ObjectNames in the user data."),
		new MBeanNotificationInfo(new String[]{NOTIF_TYPE_NEW_SUBS}, Notification.class.getName(), "Notification issued when a new ObjectName is added to the subscription set. Contains the new ObjectName in the user data."),
		new MBeanNotificationInfo(new String[]{NOTIF_TYPE_DROPPED_SUBS}, Notification.class.getName(), "Notification issued when an existing ObjectName is dropped from the subscription set. Contains the dropped ObjectName in the user data.")
	};
	
	
	/**
	 * Creates a new Subscription
	 * @param objectName The subscription's mandatory ObjectName criteria
	 * @param listener The subscription's mandatory listener
	 * @param query The subscription's optional query criteria 
	 * @param filter The subscription's optional filter
	 * @param handback The subscription's optional handback
	 */
	public Subscription(final ObjectName objectName, final NotificationListener listener, final QueryExp query, final NotificationFilter filter, final Object handback) {
		super(SharedNotificationExecutor.getInstance(), NOTIF_INFOS);
		if(objectName==null) throw new IllegalArgumentException("The passed ObjectName was null and is mandatory");
		if(listener==null) throw new IllegalArgumentException("The passed NotificationListener was null and is mandatory");
		this.objectName = objectName;
		this.query = append(query);
		this.listener = listener;
		this.filter = filter;
		this.handback = handback;
	}
	
	protected static QueryExp append(QueryExp exp) {
		if(exp==null) return NOTIF_BCASTER_QUERYEXP;
		else return Query.and(exp, NOTIF_BCASTER_QUERYEXP);
	}
	
	/**
	 * Creates a new Subscription
	 * @param objectName The subscription's mandatory ObjectName criteria
	 * @param listener The subscription's mandatory listener
	 */
	public Subscription(final ObjectName objectName, final NotificationListener listener) {
		this(objectName, listener, null, null, null);
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.notifcations.SubscriptionMBean#getAttribute(java.lang.Class, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> Map<ObjectName, T> getAttribute(final Class<T> type, final String attributeName) {			
		final HashMap<ObjectName, T> results = new HashMap<ObjectName, T>(subscribed.size());
		for(ObjectName on: subscribed) {
			try {
				results.put(on, (T)server.getAttribute(on, attributeName));
			} catch (Exception x) {/* No Op */}
		}
		return results;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.notifcations.SubscriptionMBean#getAttribute(java.lang.String)
	 */
	@Override
	public Map<ObjectName, Object> getAttribute(final String attributeName) {
		return getAttribute(Object.class, attributeName);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.notifcations.SubscriptionMBean#getAttributes(java.lang.String[])
	 */
	@Override
	public Map<ObjectName, Map<String, Object>> getAttributes(final String... attributeNames) {
		final Map<ObjectName, Map<String, Object>> results = new HashMap<ObjectName, Map<String, Object>>(subscribed.size());
		for(ObjectName on: subscribed) {
			try {
				final AttributeList attrs = server.getAttributes(on, attributeNames);
				final Map<String, Object> attrMap = new HashMap<String, Object>(attrs.size());
				for(Attribute attr: attrs.asList()) {
					attrMap.put(attr.getName(), attr.getValue());
				}
				results.put(on, attrMap);
			} catch (Exception x) {/* No Op */}
		}		
		return results;
	}

	
	/**
	 * <p>Filters out any notifications that are not {@link MBeanServerNotification} instances.</p>
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(final Notification notification) {
		return (
				notification!=null && 
				(notification instanceof MBeanServerNotification)				
		);
	}


	@Override
	public void handleNotification(Notification notification, Object handback) {
		final MBeanServerNotification mbsn = (MBeanServerNotification)notification;
		final ObjectName on = mbsn.getMBeanName();
		if(MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(mbsn.getType())) {			
			if(!subscribed.contains(on)) {
				if(query!=null) {
					try {
						if(query.apply(on)) {
							subscribeTo(on, true);
						}
					} catch (Exception ex) {
						log.log(Level.WARNING, "Error applying query [" + query + "] to ObjectName [" + on + "]", ex);
					}
				} else {
						subscribeTo(on, true);						
				}
			}
		} else if(MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(mbsn.getType())) {
			unsubscribeFrom(on, true);
		} else {
			// ?
		}
		
	}
	
	/**
	 * Adds the proxy listener to the passed ObjectName
	 * @param objectName
	 * @param sendNotif true to send notification, false otherwise
	 */
	protected void subscribeTo(final ObjectName objectName, final boolean sendNotif) {		
		if(!subscribed.contains(objectName)) {
			synchronized(subscribed) {
				if(!subscribed.contains(objectName)) {
					try {	
						server.addNotificationListener(objectName, listener, filter, handback);
						log.fine("Subscribed Proxy Listener [" + myObjectName + "] to [" + objectName + "]");
						subscribed.add(objectName);
						if(sendNotif) {
							final Notification n = new Notification(NOTIF_TYPE_NEW_SUBS, myObjectName, notifSerial.incrementAndGet(), System.currentTimeMillis(), "New MBean [" + objectName + "]");
							n.setUserData(objectName);
							sendNotification(n);
						}
					} catch (Exception ex) {
						log.log(Level.SEVERE, "Failed to subscribe Proxy Listener [" + myObjectName + "] to [" + objectName + "]", ex);
					}						
				}
			}
		}
	}
	
	/**
	 * Unsubscribes from the passed ObjectName
	 * @param objectName the ObjectName to unsub from
	 * @param sendNotif true to send a notification, false otherwise
	 */
	protected void unsubscribeFrom(final ObjectName objectName, final boolean sendNotif) {		
		if(subscribed.remove(objectName)) {
			try {
				server.removeNotificationListener(objectName, listener, filter, handback);
				final Notification n = new Notification(NOTIF_TYPE_DROPPED_SUBS, myObjectName, notifSerial.incrementAndGet(), System.currentTimeMillis(), "Dropped MBean [" + objectName + "]");				
				n.setUserData(objectName);
				sendNotification(n);
			} catch (Exception ex) {
				log.log(Level.SEVERE, "Failed to unsibscribe Proxy Listener [" + myObjectName + "] from [" + objectName + "]", ex);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.notifcations.SubscriptionMBean#stop()
	 */
	@Override
	public void stop() {
		if(started.compareAndSet(true, false)) {
			try {
				server.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this, this, null);			
			} catch (Exception ex) {
				log.log(Level.SEVERE, "Failed to unsubscribe Proxy Listener [" + myObjectName + "] from MBeanServerDelegate", ex);
			}
			final Set<ObjectName> ons = new HashSet<ObjectName>(subscribed);
			for(ObjectName on: ons) {
				unsubscribeFrom(on, false);
			}
			if(server.isRegistered(myObjectName)) {
				try { server.unregisterMBean(myObjectName); } catch (Exception x) {/* No Op */}			
			}			
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.notifcations.SubscriptionMBean#isStarted()
	 */
	@Override
	public boolean isStarted() {
		return started.get();
	}
	
	/**
	 * Starts the proxy subscription by starting the MBeanServerNotification listener, then issuing a query for all matching mbeans.
	 */
	public void start() {
		if(started.compareAndSet(false, true)) {
			try {
				server.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this, this, null);
			} catch (Exception ex) {
				log.log(Level.SEVERE, "Failed to subscribe Proxy Listener [" + myObjectName + "] to MBeanServerDelegate", ex);
			}
			try {
				final Set<ObjectName> matching = server.queryNames(objectName, query);
				matching.remove(myObjectName);
				for(ObjectName on: matching) {
					subscribeTo(on, false);
				}
			} catch (Exception ex) {
				log.log(Level.SEVERE, "Failed to start Proxy Listener [" + myObjectName + "]", ex);
			}

			final Notification n = new Notification(NOTIF_TYPE_INITIAL_SUBS, myObjectName, notifSerial.incrementAndGet(), System.currentTimeMillis(), "Subscription started with [" + subscribed.size() + "] MBeans");				
			n.setUserData(new HashSet<ObjectName>(subscribed));
			sendNotification(n);

		}
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.notifcations.SubscriptionMBean#getObjectNameFilter()
	 */
	@Override
	public ObjectName getObjectNameFilter() {
		return objectName;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.notifcations.SubscriptionMBean#getObjectName()
	 */
	@Override
	public ObjectName getObjectName() {
		return myObjectName;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.notifcations.SubscriptionMBean#getQueryExp()
	 */
	@Override
	public QueryExp getQueryExp() {
		return query;
	}

	/**
	 * Returns the subscription's query
	 * @return the query
	 */
	public QueryExp getQuery() {
		return query;
	}


	/**
	 * Returns the subscription's listener
	 * @return the listener
	 */
	public NotificationListener getListener() {
		return listener;
	}



	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result + ((handback == null) ? 0 : handback.hashCode());
		result = prime * result + ((listener == null) ? 0 : listener.hashCode());
		result = prime * result + ((objectName == null) ? 0 : objectName.hashCode());
		result = prime * result + ((query == null) ? 0 : query.hashCode());
		return result;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Subscription other = (Subscription) obj;
		if (filter == null) {
			if (other.filter != null) {
				return false;
			}
		} else if (!filter.equals(other.filter)) {
			return false;
		}
		if (handback == null) {
			if (other.handback != null) {
				return false;
			}
		} else if (!handback.equals(other.handback)) {
			return false;
		}
		if (listener == null) {
			if (other.listener != null) {
				return false;
			}
		} else if (!listener.equals(other.listener)) {
			return false;
		}
		if (objectName == null) {
			if (other.objectName != null) {
				return false;
			}
		} else if (!objectName.equals(other.objectName)) {
			return false;
		}
		if (query == null) {
			if (other.query != null) {
				return false;
			}
		} else if (!query.equals(other.query)) {
			return false;
		}
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.notifcations.SubscriptionMBean#getSubscribedCount()
	 */
	@Override
	public int getSubscribedCount() {
		return subscribed.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.notifcations.SubscriptionMBean#exportSubscribed()
	 */
	@Override
	public Set<ObjectName> exportSubscribed() {
		return new HashSet<ObjectName>(subscribed);
	}
	



	@Override
	public ObjectName preRegister(final MBeanServer server, final ObjectName name) throws Exception {
		myObjectName = name;
		this.server = server;
		return name;
	}

	@Override
	public void postRegister(final Boolean registrationDone) {
		if(registrationDone) {
			//start();
		}
		
	}

	@Override
	public void preDeregister() throws Exception {
		/* No Op */		
	}

	@Override
	public void postDeregister() {
		stop();		
	}

	
	/**
	 * Returns the proxy notification listener type name
	 * @return the proxy notification listener type name
	 */
	public String getListenerType() {
		return listener==null ? null : listener.getClass().getName();
	}
	
	/**
	 * Returns the proxy notification filter
	 * @return the proxy notification filter
	 */
	public NotificationFilter getFilter() {
		return filter;
	}
	
	/**
	 * Returns the proxy notification filter type name
	 * @return the proxy notification filter type name
	 */
	public String getFilterType() {
		return filter==null ? null : filter.getClass().getName();
	}
	
	/**
	 * Returns the proxy notification handback
	 * @return the proxy notification handback
	 */
	public Object getHandback() {
		return handback;
	}
	
	/**
	 * Returns the proxy notification handback type name
	 * @return the proxy notification handback type name
	 */
	public String getHandbackType() {
		return handback==null ? null : handback.getClass().getName();
	}

	
	

	
}
