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

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import com.heliosapm.utils.jmx.SharedNotificationExecutor;

/**
 * <p>Title: ChangeNotifyingProperties</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.system.ChangeNotifyingProperties</code></p>
 */

public class ChangeNotifyingProperties extends Properties {
	/**  */
	private static final long serialVersionUID = -381405317030346569L;
	/** Flag indicating if notifications are enabled */
	protected final AtomicBoolean notificationsEnabled = new AtomicBoolean(false);
	/** The registered listeners */
	protected final Set<PropertyChangeListener> listeners = new CopyOnWriteArraySet<PropertyChangeListener>();
	/** The notification thread pool to async notify */
	protected final ExecutorService threadpool = SharedNotificationExecutor.getInstance();
	
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
		System.setProperties(new ChangeNotifyingProperties(System.getProperties()));
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
	
//	/**
//	 * {@inheritDoc}
//	 * @see java.util.Properties#load(java.io.InputStream)
//	 */
//	@Override
//	public synchronized void load(final InputStream inStream) throws IOException {
//		super.load(inStream);
//	}
//	
//	/**
//	 * {@inheritDoc}
//	 * @see java.util.Properties#load(java.io.Reader)
//	 */
//	@Override
//	public synchronized void load(final Reader reader) throws IOException {
//		super.load(reader);
//	}
//	
//	/**
//	 * {@inheritDoc}
//	 * @see java.util.Properties#loadFromXML(java.io.InputStream)
//	 */
//	@Override
//	public synchronized void loadFromXML(final InputStream in) throws IOException, InvalidPropertiesFormatException {
//		super.loadFromXML(in);
//	}
//	
//	/**
//	 * {@inheritDoc}
//	 * @see java.util.Hashtable#putAll(java.util.Map)
//	 */
//	@Override
//	public synchronized void putAll(final Map<? extends Object, ? extends Object> t) {
//		if(t!=null && !t.isEmpty()) {
//			super.putAll(t);
//		}
//	}
	
	
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
	}
	
	private static String toStr(final Object o) {
		return o==null ? null : o.toString();
	}
	
	
}
