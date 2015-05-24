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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: JMXManagedThreadFactory</p>
 * <p>Description: Creates a new JMX managed ThreadFactory (JMX part pending)</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.JMXManagedThreadFactory</code></p>
 */

public class JMXManagedThreadFactory implements ThreadFactory {
	/** A map of created factories keyed by the name */
	private static final Map<String, JMXManagedThreadFactory> factories = new ConcurrentHashMap<String, JMXManagedThreadFactory>();
	
	private final String name;
	private final boolean daemon;
	private final AtomicInteger serial  = new AtomicInteger(0);
	private final AtomicLong totalThreadsCreated  = new AtomicLong(0);
	private final AtomicInteger currentThreadCount  = new AtomicInteger(0);
	private final ThreadGroup threadGroup;
	private final Map<Long, Thread> threads = new ConcurrentHashMap<Long, Thread>();
	
	
	
	
	/**
	 * Acquires the named thread factory
	 * @param name The factory name
	 * @param daemon true for daemon threads, false otherwise
	 * @return the factory
	 */
	public static ThreadFactory newThreadFactory(final String name, final boolean daemon) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		final String _name = name.trim();
		JMXManagedThreadFactory factory = factories.get(_name);
		if(factory==null) {
			synchronized(factories) {
				factory = factories.get(_name);
				if(factory==null) {
					factory = new JMXManagedThreadFactory(_name, daemon);
					factories.put(_name, factory);
				}
			}
		}
		return factory;
	}
	
	private JMXManagedThreadFactory(final String name, boolean daemon) {
		this.name = name;
		this.daemon = daemon;
		this.threadGroup = new ThreadGroup(name + "ThreadGroup");
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(final Runnable r) {
		final Runnable wr = new Runnable() {
			@Override
			public void run() {
				try {
					r.run();
				} finally {
					currentThreadCount.decrementAndGet();
					threads.remove(Thread.currentThread().getId());
				}
			}
		};
		final Thread t = new Thread(threadGroup, wr, name + "Thread#" + serial.incrementAndGet());
		t.setDaemon(daemon);
		threads.put(t.getId(), t);
		currentThreadCount.incrementAndGet();
		totalThreadsCreated.incrementAndGet();
		return t;
	}

	/**
	 * Returns the thread factory name
	 * @return the thread factory name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Indicates if created threads are daemon threads
	 * @return true if created threads are daemon threads, false otherwise
	 */
	public boolean isDaemon() {
		return daemon;
	}

	/**
	 * Returns the cummulative number of threads that have been created
	 * @return the cummulative number of threads that have been created
	 */
	public long getTotalThreadsCreated() {
		return totalThreadsCreated.get();
	}

	/**
	 * Returns the current number of threads
	 * @return the current number of threads
	 */
	public int getCurrentThreadCount() {
		return currentThreadCount.get();
	}

	/**
	 * Returns the factory thread group
	 * @return the factory thread group
	 */
	public ThreadGroup getThreadGroup() {
		return threadGroup;
	}

}
