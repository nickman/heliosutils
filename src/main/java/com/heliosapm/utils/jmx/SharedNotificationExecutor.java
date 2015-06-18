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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.ObjectName;
import javax.management.QueryExp;

/**
 * <p>Title: SharedNotificationExecutor</p>
 * <p>Description: A centralized and shared JMX notification broadcaster executor</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.SharedNotificationExecutor</code></p>
 */

public class SharedNotificationExecutor implements ExecutorService {
	/** The singleton instance */
	private static volatile SharedNotificationExecutor instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The MBean ObjectName for the file watcher's JMX notification thread pool */
	public static final ObjectName NOTIF_THREAD_POOL_OBJECT_NAME = JMXHelper.objectName("com.heliosapm.notifications:service=NotificationThreadPool");

	/** The number of CPU cores available to the JVM */
	public static final int CORES = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	
	
	/** The shared thread pool */
	private final JMXManagedThreadPool threadPool;
	
	
	/**
	 * Acquires and returns the SharedNotificationExecutor singleton instance
	 * @return the SharedNotificationExecutor singleton instance
	 */
	public static SharedNotificationExecutor getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new SharedNotificationExecutor();
				}
			}
		}
		return instance;
	}
	
	
	private SharedNotificationExecutor() {		
		threadPool = new JMXManagedThreadPool(NOTIF_THREAD_POOL_OBJECT_NAME, "WatcherNotificationThreadPool", CORES, CORES, 1024, 60000, 100, 90);
		final Runnable r = new Runnable() {
			@Override
			public void run() {}
		};
		threadPool.execute(r);threadPool.execute(r);threadPool.execute(r);		
	}
	
	
	public void invokeOp(final ObjectName pattern, final QueryExp query, final String opName, final boolean sync) {
		invokeOp(pattern, query, opName, new Object[]{}, new String[]{}, sync);
	}
	
	public void invokeOp(final ObjectName pattern, final QueryExp query, final String opName, final Object[] args, final String[] signature, final boolean sync) {
		if(pattern==null) throw new IllegalArgumentException("The passed ObjectName was null");
		if(opName==null) throw new IllegalArgumentException("The passed opName was null");
		final ObjectName[] matches = JMXHelper.query(pattern, query);
		if(matches.length==0) return;
		List<Future<?>> futures = new ArrayList<Future<?>>(matches.length);
		for(final ObjectName on: matches) {
			futures.add(threadPool.submit(new Runnable(){
				@Override
				public void run() {
					JMXHelper.invoke(on, opName, args, signature);					
				}
			}));
		}
		if(sync) {
			for(Future<?> f: futures) {
				try {
					f.get();
				} catch (Exception ex) {
					throw new RuntimeException("Sync Op [" + opName + "] against [" + pattern + "] timed out", ex);
				}
			}
		}
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
	 */
	@Override
	public void execute(final Runnable command) {
		threadPool.execute(command);
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 */
	@Override
	public void shutdown() {
		final Thread t = new Thread("NotificationExecutorShutdownThread") {
			public void run() {
				try {
					awaitTermination(60, TimeUnit.SECONDS);					
				} catch (Exception x) { /* No Op */ }
				JMXHelper.unregisterMBean(NOTIF_THREAD_POOL_OBJECT_NAME);
			}
		};
		t.setDaemon(true);
		threadPool.shutdown();
		t.start();
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ExecutorService#shutdownNow()
	 */
	@Override
	public List<Runnable> shutdownNow() {
		try { JMXHelper.unregisterMBean(NOTIF_THREAD_POOL_OBJECT_NAME); } catch (Exception x) { /* No Op */ }
		return threadPool.shutdownNow();
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ExecutorService#isShutdown()
	 */
	@Override
	public boolean isShutdown() {
		return threadPool.isShutdown();
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ExecutorService#isTerminated()
	 */
	@Override
	public boolean isTerminated() {
		return threadPool.isTerminated();
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ExecutorService#awaitTermination(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {		
		return threadPool.awaitTermination(timeout, unit);
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ExecutorService#submit(java.util.concurrent.Callable)
	 */
	@Override
	public <T> Future<T> submit(final Callable<T> task) {
		return threadPool.submit(task);
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable, java.lang.Object)
	 */
	@Override
	public <T> Future<T> submit(final Runnable task, final T result) {
		return threadPool.submit(task, result);
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
	 */
	@Override
	public Future<?> submit(final Runnable task) {
		return threadPool.submit(task);
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection)
	 */
	@Override
	public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return threadPool.invokeAll(tasks);
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
		return threadPool.invokeAll(tasks, timeout, unit);
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection)
	 */
	@Override
	public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return threadPool.invokeAny(tasks);
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return threadPool.invokeAny(tasks, timeout, unit);
	}

}
