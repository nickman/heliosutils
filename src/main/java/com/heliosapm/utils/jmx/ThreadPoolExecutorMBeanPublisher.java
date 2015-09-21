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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import com.heliosapm.utils.ref.MBeanProxy;
import com.heliosapm.utils.ref.ReferenceService.ReferenceType;

/**
 * <p>Title: ThreadPoolExecutorMBeanPublisher</p>
 * <p>Description: Class to wrap JMX instrumentation around an existing {@link ThreadPoolExecutor}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.ThreadPoolExecutorMBeanPublisher</code></p>
 */

public class ThreadPoolExecutorMBeanPublisher implements ThreadPoolExecutorMBeanPublisherMBean {
	/** The instrumented thread pool executor */
	final ThreadPoolExecutor threadPoolExecutor;
	
	/**
	 * Creates and publishes a new ThreadPoolExecutorMBeanPublisher
	 * @param objectName The JMX ObjectName to publish under
	 * @param threadPoolExecutor The thread pool to instrument
	 */
	public static void publish(final ObjectName objectName, final ThreadPoolExecutor threadPoolExecutor) {
		if(JMXHelper.isRegistered(objectName)) throw new RuntimeException("The ObjectName [" + objectName + "] is already registered");
		final ThreadPoolExecutorMBeanPublisher tpm = new ThreadPoolExecutorMBeanPublisher(threadPoolExecutor);
		JMXHelper.registerMBean(tpm, objectName);
	}
	
	/**
	 * Creates and publishes a new ThreadPoolExecutorMBeanPublisher with a weak reference MBean that will not prevent the pool from being GCed
	 * @param objectName The JMX ObjectName to publish under
	 * @param threadPoolExecutor The thread pool to instrument
	 */
	public static void publishWeak(final ObjectName objectName, final ThreadPoolExecutor threadPoolExecutor) {
		if(JMXHelper.isRegistered(objectName)) throw new RuntimeException("The ObjectName [" + objectName + "] is already registered");
		final ThreadPoolExecutorMBeanPublisher tpm = new ThreadPoolExecutorMBeanPublisher(threadPoolExecutor);
		MBeanProxy.register(ReferenceType.WEAK, objectName, ThreadPoolExecutorMBeanPublisherMBean.class, tpm); 				
	}

	
	private ThreadPoolExecutorMBeanPublisher(final ThreadPoolExecutor threadPoolExecutor) {
		this.threadPoolExecutor = threadPoolExecutor;
	}
	
	/**
	 * @param task
	 * @return
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable)
	 */
	public Future<?> submit(final Runnable task) {
		return threadPoolExecutor.submit(task);
	}

	/**
	 * @param task
	 * @param result
	 * @return
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable, java.lang.Object)
	 */
	public <T> Future<T> submit(final Runnable task, final T result) {
		return threadPoolExecutor.submit(task, result);
	}

	/**
	 * @param task
	 * @return
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.util.concurrent.Callable)
	 */
	public <T> Future<T> submit(final Callable<T> task) {
		return threadPoolExecutor.submit(task);
	}

	/**
	 * @param command
	 * @see java.util.concurrent.ThreadPoolExecutor#execute(java.lang.Runnable)
	 */
	public void execute(final Runnable command) {
		threadPoolExecutor.execute(command);
	}

	/**
	 * 
	 * @see java.util.concurrent.ThreadPoolExecutor#shutdown()
	 */
	public void shutdown() {
		threadPoolExecutor.shutdown();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#shutdownNow()
	 */
	public List<Runnable> shutdownNow() {
		return threadPoolExecutor.shutdownNow();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#isShutdown()
	 */
	public boolean isShutdown() {
		return threadPoolExecutor.isShutdown();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminating()
	 */
	public boolean isTerminating() {
		return threadPoolExecutor.isTerminating();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminated()
	 */
	public boolean isTerminated() {
		return threadPoolExecutor.isTerminated();
	}


	/**
	 * @param corePoolSize
	 * @see java.util.concurrent.ThreadPoolExecutor#setCorePoolSize(int)
	 */
	public void setCorePoolSize(final int corePoolSize) {
		threadPoolExecutor.setCorePoolSize(corePoolSize);
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getCorePoolSize()
	 */
	public int getCorePoolSize() {
		return threadPoolExecutor.getCorePoolSize();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#prestartCoreThread()
	 */
	public boolean prestartCoreThread() {
		return threadPoolExecutor.prestartCoreThread();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#prestartAllCoreThreads()
	 */
	public int prestartAllCoreThreads() {
		return threadPoolExecutor.prestartAllCoreThreads();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#allowsCoreThreadTimeOut()
	 */
	public boolean allowsCoreThreadTimeOut() {
		return threadPoolExecutor.allowsCoreThreadTimeOut();
	}

	/**
	 * @param value
	 * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	public void allowCoreThreadTimeOut(final boolean value) {
		threadPoolExecutor.allowCoreThreadTimeOut(value);
	}

	/**
	 * @param maximumPoolSize
	 * @see java.util.concurrent.ThreadPoolExecutor#setMaximumPoolSize(int)
	 */
	public void setMaximumPoolSize(final int maximumPoolSize) {
		threadPoolExecutor.setMaximumPoolSize(maximumPoolSize);
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getMaximumPoolSize()
	 */
	public int getMaximumPoolSize() {
		return threadPoolExecutor.getMaximumPoolSize();
	}

	/**
	 * @param time
	 * @param unit
	 * @see java.util.concurrent.ThreadPoolExecutor#setKeepAliveTime(long, java.util.concurrent.TimeUnit)
	 */
	public void setKeepAliveTime(final long time, String unit) {
		threadPoolExecutor.setKeepAliveTime(time, TimeUnit.valueOf(unit.trim().toUpperCase()));
	}

	/**
	 * @param unit
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getKeepAliveTime(java.util.concurrent.TimeUnit)
	 */
	public long getKeepAliveTime(final String unit) {
		return threadPoolExecutor.getKeepAliveTime(TimeUnit.valueOf(unit.trim().toUpperCase()));
	}
	
	/**
	 * @return
	 */
	public long getKeepAliveTimeMs() {
		return threadPoolExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS);
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getQueue()
	 */
	public String getQueueType() {
		return threadPoolExecutor.getQueue().getClass().getName();
	}
	
	public int getQueueDepth() {
		return threadPoolExecutor.getQueue().size();
	}
	
	public int getQueueCapacity() {
		return threadPoolExecutor.getQueue().remainingCapacity();
	}
	

	/**
	 * @param task
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#remove(java.lang.Runnable)
	 */
	public boolean remove(Runnable task) {
		return threadPoolExecutor.remove(task);
	}

	/**
	 * 
	 * @see java.util.concurrent.ThreadPoolExecutor#purge()
	 */
	public void purge() {
		threadPoolExecutor.purge();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize() {
		return threadPoolExecutor.getPoolSize();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount() {
		return threadPoolExecutor.getActiveCount();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getLargestPoolSize()
	 */
	public int getLargestPoolSize() {
		return threadPoolExecutor.getLargestPoolSize();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getTaskCount()
	 */
	public long getTaskCount() {
		return threadPoolExecutor.getTaskCount();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getCompletedTaskCount()
	 */
	public long getCompletedTaskCount() {
		return threadPoolExecutor.getCompletedTaskCount();
	}

	
	

}
