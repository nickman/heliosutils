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

import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;

/**
 * <p>Title: SubscriptionMBean</p>
 * <p>Description:JMX MBean interface for {@link Subscription} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.notifcations.SubscriptionMBean</code></p>
 */
public interface SubscriptionMBean {
	
	/** The initial subscribed set */
	public static String NOTIF_TYPE_INITIAL_SUBS = "subscription.initial";
	/** Dynamically detected additions to the subscribed set */
	public static String NOTIF_TYPE_NEW_SUBS = "subscription.new";
	/** Dynamically detected removals from the subscribed set */
	public static String NOTIF_TYPE_DROPPED_SUBS = "subscription.dropped";
	
	/**
	 * Queries one attribute from all subscribed mbeans
	 * @param type The expected type of the attribute
	 * @param attributeName The name of the attribute to query
	 * @return a map of the attribute values keyed by the ObjectName each one came from
	 */
	public <T> Map<ObjectName, T> getAttribute(final Class<T> type, final String attributeName);
	
	/**
	 * Queries one attribute from all subscribed mbeans
	 * @param attributeName The name of the attribute to query
	 * @return a map of the attribute values keyed by the ObjectName each one came from
	 */
	public Map<ObjectName, Object> getAttribute(final String attributeName);
	
	
	/**
	 * Queries the named attributes from all subscribed mbeans
	 * @param attributeNames The names of the attributes to query
	 * @return a map of the attribute values keyed by the attribute name within a map keyed by the ObjectName each one came from
	 */
	public Map<ObjectName, Map<String, Object>> getAttributes(final String... attributeNames);
	
	/**
	 * Starts the subscriptions, triggering the initial subs notification.
	 * This gives the registrar an opportunity to register notification listeners
	 */
	public void start();
	
	
	/**
	 * Stops the subscriptions, removes all registered listeners and unregisters this MBean
	 */
	public void stop();
	
	/**
	 * Indicates if this subscription is started
	 * @return true if this subscription is started, false otherwise
	 */
	public boolean isStarted();
	
	/**
	 * Returns the number of subscribed MBeans
	 * @return the number of subscribed MBeans
	 */
	public int getSubscribedCount();
	
	/**
	 * Returns a set of the subscribed object names
	 * @return a set of the subscribed object names
	 */
	public Set<ObjectName> exportSubscribed();
	
	/**
	 * Returns the subscription object name pattern
	 * @return the subscription object name pattern
	 */
	public ObjectName getObjectNameFilter();
	
	/**
	 * Returns this MBean's ObjectName
	 * @return this MBean's ObjectName
	 */
	public ObjectName getObjectName();
	
	/**
	 * Returns the filtering query
	 * @return the filtering query
	 */
	public QueryExp getQueryExp();
	
	/**
	 * Returns the proxy notification listener
	 * @return the proxy notification listener
	 */
	public NotificationListener getListener();
	
	/**
	 * Returns the proxy notification listener type name
	 * @return the proxy notification listener type name
	 */
	public String getListenerType();
	
	/**
	 * Returns the proxy notification filter
	 * @return the proxy notification filter
	 */
	public NotificationFilter getFilter();
	
	/**
	 * Returns the proxy notification filter type name
	 * @return the proxy notification filter type name
	 */
	public String getFilterType();
	
	/**
	 * Returns the proxy notification handback
	 * @return the proxy notification handback
	 */
	public Object getHandback();
	
	/**
	 * Returns the proxy notification handback type name
	 * @return the proxy notification handback type name
	 */
	public String getHandbackType();
	
	
	
}
