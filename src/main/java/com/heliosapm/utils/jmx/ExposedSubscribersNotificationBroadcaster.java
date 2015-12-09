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
package com.heliosapm.utils.jmx;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServerDelegate;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

/**
 * <p>Title: ExposedSubscribersNotificationBroadcaster</p>
 * <p>Description: A bit like a {@link javax.management.NotificationBroadcasterSupport} (and libreally borrowed from the same) except the
 * subscribers are exposed so we know how many there for cases where notifications are expensive to create.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.ExposedSubscribersNotificationBroadcaster</code></p>
 */

public class ExposedSubscribersNotificationBroadcaster implements NotificationEmitter {

	protected final NonBlockingHashMap<Listener, String> subs = new NonBlockingHashMap<Listener, String>(8);
	
	protected final Executor executor; 
	final MBeanNotificationInfo[] infos;
	
  private static final MBeanNotificationInfo[] NO_NOTIFICATION_INFO =
      new MBeanNotificationInfo[0];

	
	private static class Listener {
		final NotificationListener notifListener;
		final NotificationFilter notifFilter;
		final Object handback;
		final boolean wildcard;
		final int hashCode;
		
		/**
		 * Creates a new Listener
		 * @param notifListener The mandatory listener
		 * @param notifFilter The optional filter
		 * @param handback The optional handback
		 */
		public Listener(final NotificationListener notifListener, final NotificationFilter notifFilter, final Object handback) {
			if(notifListener==null) throw new IllegalArgumentException("The passed notification listener was null");
			this.notifListener = notifListener;
			this.notifFilter = notifFilter;
			this.handback = handback;
			wildcard = notifFilter==null && handback==null;
			hashCode = _hashCode();
		}
		
		/**
		 * Creates a new Listener
		 * @param notifListener The mandatory listener
		 */
		public Listener(final NotificationListener notifListener) {
			this(notifListener, null, null);
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			final StringBuilder b = new StringBuilder("Listener [");
			b.append("hash:").append(hashCode()).append(",");
			b.append("type:").append(notifListener.getClass().getSimpleName()).append(",");
			if(notifFilter!=null) b.append("filter:").append(notifFilter.getClass().getSimpleName()).append(",");
			if(handback!=null) b.append("handback:").append(handback.getClass().getSimpleName()).append(",");
			if(notifFilter==null && handback==null) b.append("wildcard:").append(wildcard).append(",");
			return b.deleteCharAt(b.length()-1).append("]").toString();
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return hashCode;
		}

		
		private int _hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + notifListener.hashCode();
			if(!wildcard) {
				result = prime * result + ((handback == null) ? 23 : handback.hashCode());
				result = prime * result + ((notifFilter == null) ? 33 : notifFilter.hashCode());				
			} 			
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			// =============== Object =============== 
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;			
			Listener other = (Listener) obj;
			// =============== Listener ONLY ===============
			if(wildcard && other.wildcard && notifListener == other.notifListener) return true;
			// =============== Handback ===============
			if (handback == null) {
				if (other.handback != null)
					return false;
			} else if (!handback.equals(other.handback))
				return false;
			// =============== Filter ===============
			if (notifFilter == null) {
				if (other.notifFilter != null)
					return false;
			} else if (!notifFilter.equals(other.notifFilter))
				return false;
			// =============== Listener ===============
			return notifListener == other.notifListener;
		}
	}
	
	public static void main(String[] args) {
		try {
			log("Test ExposedSubscribersNotificationBroadcaster");
			final ExposedSubscribersNotificationBroadcaster nb = new ExposedSubscribersNotificationBroadcaster();
			final TestNotificationListener list1 = new TestNotificationListener("One");
			final TestNotificationListener list2 = new TestNotificationListener("Two");
			final TestNotificationFilter filter1 = new TestNotificationFilter("XXX");
			log("================== Adding");
			nb.addNotificationListener(list2, filter1, MBeanServerDelegate.DELEGATE_NAME);
			log("Size: " + nb.subs.size());
			nb.addNotificationListener(list2, null, null);
			log("Size: " + nb.subs.size());
			nb.addNotificationListener(list2, null, MBeanServerDelegate.DELEGATE_NAME);
			log("Size: " + nb.subs.size());
			nb.addNotificationListener(list2, filter1, null);
			log("Size: " + nb.subs.size());
			log("Done Adding: " + nb.subs.size() + " :: \n" + nb.printListeners());
			log("================== Removing");
			nb.removeNotificationListener(list2, filter1, MBeanServerDelegate.DELEGATE_NAME);
			log(nb.printListeners());
			log("Remaining: " + nb.subs.size());
			nb.removeNotificationListener(list2, filter1, null);
			log(nb.printListeners());
			log("Remaining: " + nb.subs.size());
			log(nb.printListeners());
			nb.removeNotificationListener(list2, filter1, MBeanServerDelegate.DELEGATE_NAME);
			log("Remaining: " + nb.subs.size());
			nb.removeNotificationListener(list2, null, null);
			log("Remaining: " + nb.subs.size());			
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	public String printListeners() {
		final StringBuilder b = new StringBuilder();
		for(Listener l : subs.keySet()) {
			b.append(l.toString()).append("\n");
		}
		return b.toString();
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Creates a new ExposedSubscribersNotificationBroadcaster
	 */
	public ExposedSubscribersNotificationBroadcaster() {
		this(null, NO_NOTIFICATION_INFO);
	}
	
	/**
	 * Creates a new ExposedSubscribersNotificationBroadcaster
	 * @param infos The mbean notification metadata to publish
	 */
	public ExposedSubscribersNotificationBroadcaster(final MBeanNotificationInfo...infos) {
		this(null, infos);		
	}

	/**
	 * Creates a new ExposedSubscribersNotificationBroadcaster
	 * @param executor The executor to dispatch notifications on
	 */
	public ExposedSubscribersNotificationBroadcaster(final Executor executor) {
		this(executor,  NO_NOTIFICATION_INFO);		
	}

	/**
	 * Creates a new ExposedSubscribersNotificationBroadcaster
	 * @param executor The executor to dispatch notifications on
	 * @param infos The mbean notification metadata to publish
	 */
	public ExposedSubscribersNotificationBroadcaster(final Executor executor, final MBeanNotificationInfo...infos) {
		this.executor = executor==null ? SharedNotificationExecutor.getInstance() : executor;
		this.infos = infos==null ? NO_NOTIFICATION_INFO : infos.clone();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws IllegalArgumentException {
		subs.put(new Listener(listener, filter, handback), "");
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {		
		if(subs.remove(new Listener(listener))==null) {
			throw new ListenerNotFoundException();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws ListenerNotFoundException {
		if(subs.remove(new Listener(listener, filter, handback))==null) {
			throw new ListenerNotFoundException();
		}
	}
	

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		return infos;
	}
	
	

	public static class TestNotificationListener implements NotificationListener {
		public final String name;
		public final long id;
		
		private static final AtomicLong idfactory = new AtomicLong(0L);
		
		/**
		 * Creates a new TestNotificationListener
		 * @param name The name of this listener
		 */
		public TestNotificationListener(final String name) {
			this.name = name;
			this.id = idfactory.incrementAndGet();
		}
		
		public String toString() {
			return "Listener: " + name + "#" + id;
		}



		@Override
		public void handleNotification(final Notification notification, final Object handback) {
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (id ^ (id >>> 32));
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestNotificationListener other = (TestNotificationListener) obj;
			if (id != other.id)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}
	
	public static class TestNotificationFilter implements NotificationFilter {
		public final String name;
		public final long id;
		
		private static final AtomicLong idfactory = new AtomicLong(0L);
		
		/**
		 * Creates a new TestNotificationFilter
		 * @param name The name of this listener
		 */
		public TestNotificationFilter(final String name) {
			this.name = name;
			this.id = idfactory.incrementAndGet();
		}
		
		public String toString() {
			return "Filter: " + name + "#" + id;
		}

		@Override
		public boolean isNotificationEnabled(final Notification notification) {			
			return false;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (id ^ (id >>> 32));
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestNotificationFilter other = (TestNotificationFilter) obj;
			if (id != other.id)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}
	

}
