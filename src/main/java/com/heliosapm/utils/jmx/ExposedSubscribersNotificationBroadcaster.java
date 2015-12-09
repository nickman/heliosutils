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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServerDelegate;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

/**
 * <p>Title: ExposedSubscribersNotificationBroadcaster</p>
 * <p>Description: A bit like a {@link javax.management.NotificationBroadcasterSupport} (and libreally borrowed from the same) except the
 * subscribers are exposed so we know how many there for cases where notifications are expensive to create.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.ExposedSubscribersNotificationBroadcaster</code></p>
 */

public class ExposedSubscribersNotificationBroadcaster implements NotificationEmitter {

	/** The total number of dispatched notifications */
	private final AtomicLong dispatches = new AtomicLong(0L);
	
	/** The registered notification listeners */
	private final CopyOnWriteArraySet<Listener> subs = new CopyOnWriteArraySet<Listener>();
	
	/** The executor used to asynch dispatch notifications */
	private final Executor executor; 
	/** The published MBean notification meta data */
	final MBeanNotificationInfo[] infos;
	
  private static final MBeanNotificationInfo[] NO_NOTIFICATION_INFO =
      new MBeanNotificationInfo[0];

	
	private static class Listener implements NotificationFilter, NotificationListener {
		final NotificationListener notifListener;
		final NotificationFilter notifFilter;
		final Object handback;
		final boolean wildcard;
		final int hashCode;
		final String pattern;
		final String toString;
		
		
		private Listener(final NotificationListener notifListener, final NotificationFilter notifFilter, final Object handback, final boolean wildcard) {
			if(notifListener==null) throw new IllegalArgumentException("The passed notification listener was null");
			this.notifListener = notifListener;
			this.notifFilter = notifFilter;
			this.handback = handback;
			this.wildcard = wildcard;
			hashCode = _hashCode();
			pattern = wildcard ? "X" : "X" + (notifFilter==null ? "0" : "X") + (handback==null ? "0" : "X"); 
			toString = _toString();			
		}
		
		
		/**
		 * Creates a new Listener
		 * @param notifListener The mandatory listener
		 * @param notifFilter The optional filter
		 * @param handback The optional handback
		 */
		public Listener(final NotificationListener notifListener, final NotificationFilter notifFilter, final Object handback) {
			this(notifListener, notifFilter, handback, false);
		}
		
		/**
		 * Creates a new Listener
		 * @param notifListener The mandatory listener
		 */
		public Listener(final NotificationListener notifListener) {
			this(notifListener, null, null, true);
		}
		
		/**
		 * @param notification
		 * @param handback
		 */
		@Override
		public void handleNotification(final Notification notification, final Object handback) {
			notifListener.handleNotification(notification, handback);
		}
		
		/**
		 * @param notification
		 * @return
		 */
		@Override
		public boolean isNotificationEnabled(final Notification notification) {			
			if(notification==null) return false;
			if(notifFilter==null) return true;
			return notifFilter.isNotificationEnabled(notification);
			
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return toString;
		}
		
		/**
		 * Returns the listener pattern
		 * @return the listener pattern
		 */
		public String pattern() {
			return pattern;
		}
		
		private String _toString() {
			final StringBuilder b = new StringBuilder("Listener [");
			b.append("hash:").append(hashCode()).append(",");
			b.append("pattern:").append(pattern).append(",");
			b.append("type:").append(notifListener.getClass().getName()).append(",");
			if(notifFilter!=null) b.append("filter:").append(notifFilter.getClass().getName()).append(",");
			if(handback!=null) b.append("handback:").append(handback.getClass().getName()).append(",");
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
//			if(!wildcard) {
//				result = prime * result + ((handback == null) ? 23 : handback.hashCode());
//				result = prime * result + ((notifFilter == null) ? 33 : notifFilter.hashCode());				
//			} 			
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
			// =============== Wildcard ===============
			if(other.wildcard && !wildcard && notifListener == other.notifListener) return true;
			// =============== Pattern ===============
			if(!pattern.equals(other.pattern)) return false;
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
			log("Adding XXX");
			nb.addNotificationListener(list2, filter1, MBeanServerDelegate.DELEGATE_NAME);
			log("Size: " + nb.subs.size() + " After XXX\n" + nb.printListeners());
			log("Adding XOO");
			nb.addNotificationListener(list2, null, null);
			log("Size: " + nb.subs.size() + " After XOO\n" + nb.printListeners());
			log("Adding XOX");
			nb.addNotificationListener(list2, null, MBeanServerDelegate.DELEGATE_NAME);
			log("Size: " + nb.subs.size() + " After XOX\n" + nb.printListeners());
			log("Adding XXO");
			nb.addNotificationListener(list2, filter1, null);
			log("Size: " + nb.subs.size() + " After XXO\n" + nb.printListeners());			
			log("================== Removing: XXX");
			nb.removeNotificationListener(list2, filter1, MBeanServerDelegate.DELEGATE_NAME);
			log(nb.printListeners());			
			log("Remaining: " + nb.subs.size());
			log("================== Removing: XXO");
			nb.removeNotificationListener(list2, filter1, null);
			log(nb.printListeners());
			log("Remaining: " + nb.subs.size());
			log(nb.printListeners());
			log("================== Removing: XOX");
			nb.removeNotificationListener(list2, null, MBeanServerDelegate.DELEGATE_NAME);
			log("Remaining: " + nb.subs.size());
			log("================== Removing: XOO");
			nb.removeNotificationListener(list2, null, null);
			log("Remaining: " + nb.subs.size());			

			
			
			
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
			log("================== Removing: X");
			nb.removeNotificationListener(list2);
			log("Remaining: " + nb.subs.size());			
			
			
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	/**
	 * Prints the registered listeners
	 * @return A string describing the listeners
	 */
	public String printListeners() {
		final StringBuilder b = new StringBuilder();
		for(Listener l : subs) {
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
	 * Dispatches the passed notification to all registered listeners
	 * @param n The notification to dispatch
	 * @return the number of actual receipients dispatched to after each subscriber filtered the notification
	 */
	public int sendNotification(final Notification n) {
		int cnt = 0;
		if(n!=null && !subs.isEmpty()) {
			for(final Listener x: subs) {
				if(x.isNotificationEnabled(n)) {
					executor.execute(new Runnable(){
						public void run() {
							x.handleNotification(n, x.handback);
						}
					});
					cnt++;
				}
			}
		}
		dispatches.addAndGet(cnt);
		return cnt;
	}
	
	/**
	 * Indicates if any subscribers are registered
	 * @return true if any subscribers are registered, false otherwise
	 */
	public boolean hasSubscribers() {
		return !subs.isEmpty();
	}

	/**
	 * Returns the number of registered subscribers
	 * @return the number of registered subscribers
	 */
	public int size() {
		return subs.size();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws IllegalArgumentException {
		final Listener x = new Listener(listener, filter, handback);
		subs.add(x);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {	
		final Listener key = new Listener(listener);
		if(!subs.removeAll(Collections.singleton(key))) {
			throw new ListenerNotFoundException();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws ListenerNotFoundException {		
		if(!subs.remove(new Listener(listener, filter, handback))) {
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
	
	/**
	 * Returns the total number of dispatched notifications
	 * @return the total number of dispatched notifications
	 */
	public long getDispatchCount() {
		return dispatches.get();
	}
	
	/**
	 * Returns info on the current subscribers
	 * @return info on the current subscribers
	 */
	public HashSet<String> getSubscriberInfo() {
		final HashSet<String> infos = new HashSet<String>();
		for(Listener x: subs) {
			infos.add(x.toString());
		}
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
