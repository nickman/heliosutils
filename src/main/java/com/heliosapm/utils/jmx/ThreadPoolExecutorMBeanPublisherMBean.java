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

/**
 * <p>Title: ThreadPoolExecutorMBeanPublisherMBean</p>
 * <p>Description: JMX MBean interface for {@link ThreadPoolExecutorMBeanPublisher} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.ThreadPoolExecutorMBeanPublisherMBean</code></p>
 */

public interface ThreadPoolExecutorMBeanPublisherMBean {
	/**
	 * @param task
	 * @return
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable)
	 */
	public Future<?> submit(final Runnable task);

	/**
	 * @param task
	 * @param result
	 * @return
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable, java.lang.Object)
	 */
	public <T> Future<T> submit(final Runnable task, final T result);

	/**
	 * @param task
	 * @return
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.util.concurrent.Callable)
	 */
	public <T> Future<T> submit(final Callable<T> task);

	/**
	 * @param command
	 * @see java.util.concurrent.ThreadPoolExecutor#execute(java.lang.Runnable)
	 */
	public void execute(final Runnable command);

	/**
	 * 
	 * @see java.util.concurrent.ThreadPoolExecutor#shutdown()
	 */
	public void shutdown();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#shutdownNow()
	 */
	public List<Runnable> shutdownNow();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#isShutdown()
	 */
	public boolean isShutdown();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminating()
	 */
	public boolean isTerminating();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminated()
	 */
	public boolean isTerminated();


	/**
	 * @param corePoolSize
	 * @see java.util.concurrent.ThreadPoolExecutor#setCorePoolSize(int)
	 */
	public void setCorePoolSize(final int corePoolSize);

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getCorePoolSize()
	 */
	public int getCorePoolSize();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#prestartCoreThread()
	 */
	public boolean prestartCoreThread();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#prestartAllCoreThreads()
	 */
	public int prestartAllCoreThreads();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#allowsCoreThreadTimeOut()
	 */
	public boolean allowsCoreThreadTimeOut();

	/**
	 * @param value
	 * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	public void allowCoreThreadTimeOut(final boolean value);

	/**
	 * @param maximumPoolSize
	 * @see java.util.concurrent.ThreadPoolExecutor#setMaximumPoolSize(int)
	 */
	public void setMaximumPoolSize(final int maximumPoolSize);

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getMaximumPoolSize()
	 */
	public int getMaximumPoolSize();

	/**
	 * @param time
	 * @param unit
	 * @see java.util.concurrent.ThreadPoolExecutor#setKeepAliveTime(long, java.util.concurrent.TimeUnit)
	 */
	public void setKeepAliveTime(final long time, String unit);

	/**
	 * @param unit
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getKeepAliveTime(java.util.concurrent.TimeUnit)
	 */
	public long getKeepAliveTime(final String unit);
	
	/**
	 * @return
	 */
	public long getKeepAliveTimeMs();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getQueue()
	 */
	public String getQueueType();
	
	public int getQueueDepth();
	
	public int getQueueCapacity();
	

	/**
	 * @param task
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#remove(java.lang.Runnable)
	 */
	public boolean remove(Runnable task);

	/**
	 * 
	 * @see java.util.concurrent.ThreadPoolExecutor#purge()
	 */
	public void purge();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getLargestPoolSize()
	 */
	public int getLargestPoolSize();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getTaskCount()
	 */
	public long getTaskCount();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getCompletedTaskCount()
	 */
	public long getCompletedTaskCount();

}
