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
package com.heliosapm.utils.jmx.protocol;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXAddressable;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

import org.cliffc.high_scale_lib.NonBlockingHashSet;

/**
 * <p>Title: UpdateableJMXConnector</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.protocol.UpdateableJMXConnector</code></p>
 */

public class UpdateableJMXConnector implements JMXConnector, JMXAddressable, NotificationListener, NotificationFilter {
	protected final AtomicReference<JMXConnector> connector = new AtomicReference<JMXConnector>(null);
	protected final JMXServiceURL serviceUrl;
	protected final NonBlockingHashSet<Notif> notifs = new NonBlockingHashSet<Notif>();
	protected final Map<String, Object> env;

	/**
	 * Creates a new UpdateableJMXConnector
	 * @param connector The initial connector
	 * @param serviceUrl the public (non-tunneled) service URL
	 */
	@SuppressWarnings("unchecked")
	public UpdateableJMXConnector(final JMXConnector connector, final JMXServiceURL serviceUrl, final Map<String, ?> env) {
		this.connector.set(connector);
		this.serviceUrl = serviceUrl;
		this.env = (Map<String, Object>) env;
		try {
			connector.getMBeanServerConnection();
		} catch (Exception x) {
			try {
				connector.connect(this.env);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to connect JMXConnector [" + this.serviceUrl + "]");
			}
		}
		connector.addConnectionNotificationListener(this, this, null);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXAddressable#getAddress()
	 */
	@Override
	public JMXServiceURL getAddress() {
		return serviceUrl;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#connect()
	 */
	@Override
	public void connect() throws IOException {		
		connector.get().connect();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#connect(java.util.Map)
	 */
	@Override
	public void connect(final Map<String, ?> env) throws IOException {
		final Map<String, Object> all = new HashMap<String, Object>();
		if(this.env!=null) {
			all.putAll(this.env);
		}
		if(env!=null) {
			all.putAll(env);
		}
		connector.get().connect(all);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getMBeanServerConnection()
	 */
	@Override
	public MBeanServerConnection getMBeanServerConnection() throws IOException {		
		return connector.get().getMBeanServerConnection();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getMBeanServerConnection(javax.security.auth.Subject)
	 */
	@Override
	public MBeanServerConnection getMBeanServerConnection(final Subject delegationSubject) throws IOException {
		return connector.get().getMBeanServerConnection(delegationSubject);		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#close()
	 */
	@Override
	public void close() throws IOException {
		try { connector.get().close(); } catch (Exception x) {/* No Op */}
		connector.set(null);
		notifs.clear();
		env.clear();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#addConnectionNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addConnectionNotificationListener(final NotificationListener listener, final NotificationFilter filter,	final Object handback) {
		final Notif n = new Notif(listener, filter, handback);
		connector.get().addConnectionNotificationListener(listener, filter, handback);
		notifs.add(n);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#removeConnectionNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		final Notif n = new Notif(listener);
		notifs.remove(n);
		connector.get().removeConnectionNotificationListener(listener);		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#removeConnectionNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void removeConnectionNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws ListenerNotFoundException {
		final Notif n = new Notif(listener, filter, handback);
		notifs.remove(n);
		connector.get().removeConnectionNotificationListener(listener, filter, handback);		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getConnectionId()
	 */
	@Override
	public String getConnectionId() throws IOException {
		return connector.get().getConnectionId();
	}
	
	/**
	 * Updates the closed connector with a connected one
	 * @param connector the new connector
	 */
	public void updateConnector(final JMXConnector connector) {
		final JMXConnector prior = this.connector.getAndSet(connector);
		try { prior.close(); } catch (Exception x) {/* No Op */}
		for(Notif n: notifs) {
			connector.addConnectionNotificationListener(n.listener, n.filter, n.handback);
		}
	}
	
	private static class Notif {
		final NotificationListener listener;
		final NotificationFilter filter; 
		final Object handback;
		/**
		 * Creates a new Notif
		 * @param listener
		 * @param filter
		 * @param handback
		 */
		public Notif(NotificationListener listener, NotificationFilter filter, Object handback) {
			this.listener = listener;
			this.filter = filter;
			this.handback = handback;
		}
		/**
		 * Creates a new Notif
		 * @param listener
		 */
		public Notif(NotificationListener listener) {
			this.listener = listener;
			this.filter = null;
			this.handback = null;			
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
			return result;
		}
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Notif other = (Notif) obj;
			if (filter == null) {
				if (other.filter != null)
					return false;
			} else if (!filter.equals(other.filter))
				return false;
			if (handback == null) {
				if (other.handback != null)
					return false;
			} else if (!handback.equals(other.handback))
				return false;
			if (listener == null) {
				if (other.listener != null)
					return false;
			} else if (!listener.equals(other.listener))
				return false;
			return true;
		}
		
		
		
		
		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(final Notification notification, final Object handback) {
		try { connector.get().close(); } catch (Exception x) {/* No Op */}
	}


	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(final Notification n) {
		if(n==null) return false;
		if(!JMXConnectionNotification.class.isInstance(n)) return false;
		final String type = n.getType();
		return (JMXConnectionNotification.FAILED.equals(type) || JMXConnectionNotification.CLOSED.equals(type));
	}


	/**
	 * Returns 
	 * @return the env
	 */
	public Map<String, Object> getEnv() {
		return env;
	}

}
