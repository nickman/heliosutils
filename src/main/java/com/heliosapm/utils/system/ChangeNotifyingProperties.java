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
package com.heliosapm.utils.system;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.AttributeChangeNotification;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.jmx.SharedNotificationExecutor;

/**
 * <p>Title: ChangeNotifyingProperties</p>
 * <p>Description: Extension of {@link Properties} that notifies registered listeners about new, removed and changed properties</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.system.ChangeNotifyingProperties</code></p>
 */

public class ChangeNotifyingProperties extends Properties implements NotificationBroadcaster, ChangeNotifyingPropertiesMBean, MBeanRegistration {  // DynamicMBean
	/**  */
	private static final long serialVersionUID = -381405317030346569L;
	/** Flag indicating if notifications are enabled */
	protected final AtomicBoolean notificationsEnabled = new AtomicBoolean(false);
	/** The registered listeners */
	protected final Set<PropertyChangeListener> listeners = new CopyOnWriteArraySet<PropertyChangeListener>();
	/** The notification thread pool to async notify */
	protected final ExecutorService threadpool = SharedNotificationExecutor.getInstance();
	
	/** Embedded jmx notification broadcaster */
	protected final NotificationBroadcasterSupport notifier = new NotificationBroadcasterSupport(threadpool, mbeanNotifs);
	/** A counter to keep track of the number of listeners so we can suppress notifications if there are no listeners */
	protected final AtomicInteger listenerCounter = new AtomicInteger(0);
	/** Notification seq counter */
	protected final AtomicLong notifSeq = new AtomicLong(0L);
	
	/** The designated ObjectName assigned at registration */
	protected ObjectName objectName = null;
	
	/** The JMX ObjectName of the installed system properties ChangeNotifyingProperties */
	public static final ObjectName SYSPROPS_OBJECT_NAME = JMXHelper.objectName("com.heliosapm.system:service=SystemProperties");
	
	/** The property inserted notification type */
	public static final String NOTIF_INSERT_EVENT = "com.heliosapm.system.property.inserted";
	/** The property removed notification type */
	public static final String NOTIF_REMOVE_EVENT = "com.heliosapm.system.property.removed";
	/** The property changed notification type */
	public static final String NOTIF_CHANGE_EVENT = "com.heliosapm.system.property.changed";
	
	
	private static final MBeanNotificationInfo[] mbeanNotifs = new MBeanNotificationInfo[] {
		new MBeanNotificationInfo(new String[]{NOTIF_INSERT_EVENT}, Notification.class.getName(), "Event broadcast when a new property is set"),
		new MBeanNotificationInfo(new String[]{NOTIF_REMOVE_EVENT}, Notification.class.getName(), "Event broadcast when a property is removed"),
		new MBeanNotificationInfo(new String[]{NOTIF_CHANGE_EVENT}, AttributeChangeNotification.class.getName(), "Event broadcast when a property is changed")
	};
	
	private static final String CHANGE_MSG = "{\"change\":\"%s\",\"old\":\"%s\",\"new\":\"%s\"}";
	private static final String INSERT_MSG = "{\"insert\":\"%s\",\"new\":\"%s\"}";
	private static final String REMOVE_MSG = "{\"remove\":\"%s\",\"new\":\"%s\"}";
	
	/**
	 * Creates a new ChangeNotifyingProperties
	 */
	public ChangeNotifyingProperties() {
		notificationsEnabled.set(true);
	}
	
	/**
	 * Creates a new ChangeNotifyingProperties
	 * @param defaults The initial defaults
	 */
	public ChangeNotifyingProperties(final Properties defaults) {
		super(defaults);
		notificationsEnabled.set(true);
	}
	
	/**
	 * Creates a new ChangeNotifyingProperties
	 * @param t The initial properties
	 */
	public ChangeNotifyingProperties(final Map<? extends Object, ? extends Object> t) {
		super();
		super.putAll(t);
		notificationsEnabled.set(true);
	}
	
	
	/**
	 * Enables or disables the triggersing of notifications
	 * @param enable true to enable, false to disable
	 */
	public void notificationsEnabled(final boolean enable) {
		notificationsEnabled.set(enable);
	}
	
	
	/**
	 * Installs a change notifying properties into System properties
	 */
	public static void systemInstall() {		
		final ChangeNotifyingProperties cnp = new ChangeNotifyingProperties(System.getProperties());
		System.setProperties(cnp);
		if(JMXHelper.isRegistered(SYSPROPS_OBJECT_NAME)) {
			JMXHelper.unregisterMBean(SYSPROPS_OBJECT_NAME);
		}
		JMXHelper.registerMBean(cnp, SYSPROPS_OBJECT_NAME);
	}
	
	/**
	 * Indicates if an instance of ChangeNotifyingProperties has been installed as System properties
	 * @return true if an instance of ChangeNotifyingProperties has been installed as System properties, false otherwise
	 */
	public static boolean isSystemInstalled() {
		return System.getProperties() instanceof ChangeNotifyingProperties;
	}
	
	/**
	 * Uninstalls the system ChangeNotifyingProperties and deregisters all the listeners
	 */
	public static void systemUninstall() {		
		if(isSystemInstalled()) {
			ChangeNotifyingProperties cnp = (ChangeNotifyingProperties)System.getProperties();
			cnp.listeners.clear();
			System.setProperties(new Properties(cnp));
			if(JMXHelper.isRegistered(SYSPROPS_OBJECT_NAME)) {
				JMXHelper.unregisterMBean(SYSPROPS_OBJECT_NAME);
			}
		}
	}
	
	/**
	 * Registers a properties listener
	 * @param listener the listener to register
	 */
	public void registerListener(final PropertyChangeListener listener) {
		if(listener!=null) {
			listeners.add(listener);
		}
	}
	
	/**
	 * Removes a properties listener
	 * @param listener the listener to remove
	 */
	public void removeListener(final PropertyChangeListener listener) {
		if(listener!=null) {
			listeners.remove(listener);
		}
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see java.util.Hashtable#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public synchronized Object put(final Object key, final Object value) {
		final Object prior = super.put(key, value);
		if(notificationsEnabled.get()) {
			if(prior==null) {
				fireInsert(toStr(key), value.toString());
			} else {
				if(!prior.equals(value)) {
					fireChange(toStr(key), value.toString(), prior.toString());
				}
			}
		}
		return prior;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.Hashtable#remove(java.lang.Object)
	 */
	@Override
	public synchronized Object remove(final Object key) {
		final Object prior = super.remove(key);
		if(notificationsEnabled.get()) {
			if(prior!=null) {
				fireRemove(toStr(key), prior.toString());
			}
		}
		return prior;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.Hashtable#clear()
	 */
	@Override
	public synchronized void clear() {
		if(!isEmpty()) { 
			final Properties p = new Properties(this);
			super.clear();
			if(notificationsEnabled.get()) {
				for(String key: p.stringPropertyNames()) {
					fireRemove(key, p.getProperty(key));
				}
			}
		}
	}
	
	
	/**
	 * Fires a change event, notifying all registered listeners of the watched property change
	 * @param key The property key
	 * @param newValue The new value
	 * @param oldValue The prior value
	 */
	protected void fireChange(final String key, final String newValue, final String oldValue) {
		if(!listeners.isEmpty()) {
			final Set<PropertyChangeListener> _listeners = new HashSet<PropertyChangeListener>(listeners);
			threadpool.execute(new Runnable(){
				@Override
				public void run() {
					for(PropertyChangeListener pcl: _listeners) {
						pcl.onChange(key, newValue, oldValue);
					}
				}
			});
		}
		if(listenerCounter.get()>0) {
			AttributeChangeNotification acn = new AttributeChangeNotification((objectName==null ? this : objectName), notifSeq.incrementAndGet(), System.currentTimeMillis(), String.format(CHANGE_MSG, key, oldValue, newValue), key, String.class.getName(), oldValue, newValue); 
			acn.setUserData(Collections.singletonMap(key, new String[]{oldValue, newValue}));
			notifier.sendNotification(acn);
		}		
	}
	
	/**
	 * Fires a new property event, notifying all registered listeners of the watched property insertion
	 * @param key The property key
	 * @param newValue The new value
	 */
	protected void fireInsert(final String key, final String newValue) {
		if(!listeners.isEmpty()) {
			final Set<PropertyChangeListener> _listeners = new HashSet<PropertyChangeListener>(listeners);
			threadpool.execute(new Runnable(){
				@Override
				public void run() {
					for(PropertyChangeListener pcl: _listeners) {
						pcl.onInsert(key, newValue);
					}
				}
			});
		}
		if(listenerCounter.get()>0) {
			Notification notif = new Notification(NOTIF_INSERT_EVENT, (objectName==null ? this : objectName), notifSeq.incrementAndGet(), System.currentTimeMillis(), String.format(INSERT_MSG, key, newValue));
			notif.setUserData(Collections.singletonMap(key, newValue));
			notifier.sendNotification(notif);
		}
	}

	/**
	 * Fires a removed property event, notifying all registered listeners of the watched property removal
	 * @param key The property key The property key
	 * @param oldValue The removed value
	 */
	protected void fireRemove(final String key, final String oldValue) {
		if(!listeners.isEmpty()) {
			final Set<PropertyChangeListener> _listeners = new HashSet<PropertyChangeListener>(listeners);
			threadpool.execute(new Runnable(){
				@Override
				public void run() {
					for(PropertyChangeListener pcl: _listeners) {
						pcl.onRemove(key, oldValue);
					}
				}
			});
		}
		if(listenerCounter.get()>0) {
			Notification notif = new Notification(NOTIF_REMOVE_EVENT, (objectName==null ? this : objectName), notifSeq.incrementAndGet(), System.currentTimeMillis(), String.format(REMOVE_MSG, key, oldValue));
			notif.setUserData(Collections.singletonMap(key, oldValue));
			notifier.sendNotification(notif);
		}
	}
	
	private static String toStr(final Object o) {
		return o==null ? null : o.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws IllegalArgumentException {
		notifier.addNotificationListener(listener, filter, handback);
		listenerCounter.incrementAndGet();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
		notifier.removeNotificationListener(listener);
		listenerCounter.decrementAndGet();
	}

	
	// ================================================================================================================================
	//			MBeanRegistration Impl.
	// ================================================================================================================================

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	@Override
	public ObjectName preRegister(final MBeanServer server, final ObjectName name) throws Exception {
		objectName = name;
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
	 */
	@Override
	public void postRegister(final Boolean registrationDone) {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	@Override
	public void preDeregister() throws Exception {
		/* No Op */		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postDeregister()
	 */
	@Override
	public void postDeregister() {
		objectName = null;
		listenerCounter.set(0);
	}
	
	// ================================================================================================================================
	//			DynamicMBean Impl.
	// ================================================================================================================================
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		return mbeanNotifs;
	}

//	/**
//	 * {@inheritDoc}
//	 * @see javax.management.DynamicMBean#getAttribute(java.lang.String)
//	 */
//	@Override
//	public Object getAttribute(final String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
//		return null;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
//	 */
//	@Override
//	public void setAttribute(final Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
//		
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * @see javax.management.DynamicMBean#getAttributes(java.lang.String[])
//	 */
//	@Override
//	public AttributeList getAttributes(final String[] attributes) {
//		return null;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * @see javax.management.DynamicMBean#setAttributes(javax.management.AttributeList)
//	 */
//	@Override
//	public AttributeList setAttributes(final AttributeList attributes) {
//		return null;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * @see javax.management.DynamicMBean#invoke(java.lang.String, java.lang.Object[], java.lang.String[])
//	 */
//	@Override
//	public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
//		return null;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * @see javax.management.DynamicMBean#getMBeanInfo()
//	 */
//	@Override
//	public MBeanInfo getMBeanInfo() {
//		return null;
//	}
//	
	
}
