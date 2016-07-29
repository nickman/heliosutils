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

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.ObjectName;

/**
 * <p>Title: SharedScheduler</p>
 * <p>Description: A JMX managed shared scheduler</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.SharedScheduler</code></p>
 */

public class SharedScheduler implements ScheduledExecutorService {
	/** The singleton instance */
	private static volatile SharedScheduler instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	
	
	/** The MBean ObjectName for the scheduler */
	public static final ObjectName SHARED_SCHEDULER_OBJECT_NAME = JMXHelper.objectName("com.heliosapm.scheduling:service=SharedScheduler");

	/** The number of CPU cores available to the JVM */
	public static final int CORES = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	
	
	/** The shared scheduler */
	private final JMXManagedScheduler scheduler;

	/**
	 * Acquires and returns the SharedScheduler singleton instance
	 * @return the SharedScheduler singleton instance
	 */
	public static SharedScheduler getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new SharedScheduler();
				}
			}
		}
		return instance;
	}
	
	
	private SharedScheduler() {		
		scheduler = new JMXManagedScheduler(SHARED_SCHEDULER_OBJECT_NAME, "SharedScheduler", CORES, true);
	}


	/**
	 * @return
	 * @see com.heliosapm.utils.jmx.JMXManagedScheduler#getObjectName()
	 */
	public ObjectName getObjectName() {
		return scheduler.getObjectName();
	}


	/**
	 * @return
	 * @see com.heliosapm.utils.jmx.JMXManagedScheduler#getPoolName()
	 */
	public String getPoolName() {
		return scheduler.getPoolName();
	}


	/**
	 * @return
	 * @see com.heliosapm.utils.jmx.JMXManagedScheduler#getUncaughtExceptionCount()
	 */
	public long getUncaughtExceptionCount() {
		return scheduler.getUncaughtExceptionCount();
	}


	/**
	 * @return
	 * @see com.heliosapm.utils.jmx.JMXManagedScheduler#getRejectedExecutionCount()
	 */
	public long getRejectedExecutionCount() {
		return scheduler.getRejectedExecutionCount();
	}


	/**
	 * @return
	 * @see com.heliosapm.utils.jmx.JMXManagedScheduler#getExecutingTaskCount()
	 */
	public long getExecutingTaskCount() {
		return scheduler.getExecutingTaskCount();
	}


	/**
	 * 
	 * @see com.heliosapm.utils.jmx.JMXManagedScheduler#stop()
	 */
	public void stop() {
		scheduler.stop();
	}


	/**
	 * @param <T>
	 * @param tasks
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @see java.util.concurrent.AbstractExecutorService#invokeAny(java.util.Collection)
	 */
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return scheduler.invokeAny(tasks);
	}


	/**
	 * @param <T>
	 * @param tasks
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 * @see java.util.concurrent.AbstractExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return scheduler.invokeAny(tasks, timeout, unit);
	}


	/**
	 * @param <T>
	 * @param tasks
	 * @return
	 * @throws InterruptedException
	 * @see java.util.concurrent.AbstractExecutorService#invokeAll(java.util.Collection)
	 */
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return scheduler.invokeAll(tasks);
	}


	/**
	 * @param <T>
	 * @param tasks
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 * @see java.util.concurrent.AbstractExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return scheduler.invokeAll(tasks, timeout, unit);
	}


	/**
	 * @param command
	 * @param delay
	 * @param unit
	 * @return
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
	 */
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return scheduler.schedule(command, delay, unit);
	}


	/**
	 * @param command
	 * @param initialDelay
	 * @param period
	 * @param unit
	 * @return
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return scheduler.scheduleAtFixedRate(command, initialDelay, period, unit);
	}
	
	/**
	 * Schedules the passed callable for repeating execution on a fixed delay
	 * @param command The callable to execute
	 * @param initialDelay The initial delay
	 * @param period The delay
	 * @param unit The delay unit
	 * @param exHandler An optional exception handler. If not supplied, any exception will throw a runtime exception
	 * @return the handle to the schedule
	 */
	public ScheduledFuture<?> scheduleWithFixedDelay(final Callable<?> command, final long initialDelay, final long period, final TimeUnit unit, final UncaughtExceptionHandler exHandler) {
		final UncaughtExceptionHandler ueh = new UncaughtExceptionHandler() {
			final WeakReference<UncaughtExceptionHandler> ref = new WeakReference<UncaughtExceptionHandler>(exHandler);
			@Override
			public void uncaughtException(final Thread t, final Throwable e) {
				UncaughtExceptionHandler handler = ref.get();
				if(handler!=null) {
					handler.uncaughtException(t, e);
				}
				handler = null;
			}
		};
		final Runnable r = new Runnable() {
			public void run() {
				try {
					command.call();
				} catch (Exception ex) {
					if(exHandler==null) {
						throw new RuntimeException(ex);
					} else {
						ueh.uncaughtException(Thread.currentThread(), ex);
					}
				}
			}
		};
		return scheduler.scheduleWithFixedDelay(r, initialDelay, period, unit);
	}
	


	/**
	 * @param command
	 * @param initialDelay
	 * @param delay
	 * @param unit
	 * @return
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return scheduler.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}


	/**
	 * @param command
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#execute(java.lang.Runnable)
	 */
	public void execute(Runnable command) {
		scheduler.execute(command);
	}


	/**
	 * @param task
	 * @return
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#submit(java.lang.Runnable)
	 */
	public Future<?> submit(Runnable task) {
		return scheduler.submit(task);
	}


	/**
	 * @param <T>
	 * @param task
	 * @param result
	 * @return
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#submit(java.lang.Runnable, java.lang.Object)
	 */
	public <T> Future<T> submit(Runnable task, T result) {
		return scheduler.submit(task, result);
	}


	/**
	 * @param <T>
	 * @param task
	 * @return
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#submit(java.util.concurrent.Callable)
	 */
	public <T> Future<T> submit(Callable<T> task) {
		return scheduler.submit(task);
	}


	/**
	 * 
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#shutdown()
	 */
	public void shutdown() {
		scheduler.shutdown();
	}


	/**
	 * @return
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#shutdownNow()
	 */
	public List<Runnable> shutdownNow() {
		return scheduler.shutdownNow();
	}


	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#isShutdown()
	 */
	public boolean isShutdown() {
		return scheduler.isShutdown();
	}


	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminating()
	 */
	public boolean isTerminating() {
		return scheduler.isTerminating();
	}


	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminated()
	 */
	public boolean isTerminated() {
		return scheduler.isTerminated();
	}


	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getRejectedExecutionHandler()
	 */
	public RejectedExecutionHandler getRejectedExecutionHandler() {
		return scheduler.getRejectedExecutionHandler();
	}


	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getCorePoolSize()
	 */
	public int getCorePoolSize() {
		return scheduler.getCorePoolSize();
	}


	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getMaximumPoolSize()
	 */
	public int getMaximumPoolSize() {
		return scheduler.getMaximumPoolSize();
	}


	/**
	 * @param task
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#remove(java.lang.Runnable)
	 */
	public boolean remove(Runnable task) {
		return scheduler.remove(task);
	}


	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize() {
		return scheduler.getPoolSize();
	}


	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount() {
		return scheduler.getActiveCount();
	}


	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getLargestPoolSize()
	 */
	public int getLargestPoolSize() {
		return scheduler.getLargestPoolSize();
	}


	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getTaskCount()
	 */
	public long getTaskCount() {
		return scheduler.getTaskCount();
	}


	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getCompletedTaskCount()
	 */
	public long getCompletedTaskCount() {
		return scheduler.getCompletedTaskCount();
	}


	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		// TODO Auto-generated method stub
		return null;
	}

}
