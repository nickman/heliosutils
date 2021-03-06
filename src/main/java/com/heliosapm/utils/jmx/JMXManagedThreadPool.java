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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import com.heliosapm.utils.config.ConfigurationHelper;

/**
 * <p>Title: JMXManagedThreadPool</p>
 * <p>Description: A JMX managed worker pool</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jmx.concurrency.JMXManagedThreadPool</code></p>
 */

public class JMXManagedThreadPool extends ThreadPoolExecutor implements ThreadFactory, RejectedExecutionHandler, UncaughtExceptionHandler, JMXManagedThreadPoolMBean {
	/** The JMX ObjectName for this pool's MBean */
	protected final ObjectName objectName;
	/** The pool name */
	protected final String poolName;
	/** The task work queue */
	protected final BlockingQueue<Runnable> workQueue;
	/** The count of uncaught exceptions */
	protected final AtomicLong uncaughtExceptionCount = new AtomicLong(0L);
	/** The count of rejected executions where the task queue was full and a new task could not be accepted */
	protected final AtomicLong rejectedExecutionCount = new AtomicLong(0L);
	/** The thread group that threads created for this pool are created in */
	protected final ThreadGroup threadGroup;
	/** The thread factory thread serial number factory */
	protected final AtomicInteger threadSerial = new AtomicInteger(0);
	/** Threadlocal to hold the start time of a given task */
	protected final ThreadLocal<long[]> taskStartTime = new ThreadLocal<long[]>() {
		@Override
		protected long[] initialValue() {
			return new long[1];
		}
	};
	/** An externally added exception handler */
	protected UncaughtExceptionHandler exceptionHandler = null;

	
	/**
	 * Creates a new JMXManagedThreadPool, reading all the configuration values from Config and publishes the JMX interface
	 * @param objectName The JMX ObjectName for this pool's MBean 
	 * @param poolName The pool name
	 */
	public JMXManagedThreadPool(ObjectName objectName, String poolName) {
		this(objectName, poolName, true);
	}


	/**
	 * Creates a new JMXManagedThreadPool, reading all the configuration values from Config
	 * @param objectName The JMX ObjectName for this pool's MBean 
	 * @param poolName The pool name
	 * @param publishJMX If true, publishes the JMX interface
	 */
	public JMXManagedThreadPool(ObjectName objectName, String poolName, boolean publishJMX) {
		this(
			objectName, 
			poolName,			
			ConfigurationHelper.getIntSystemThenEnvProperty(poolName.toLowerCase() + CONFIG_CORE_POOL_SIZE, DEFAULT_CORE_POOL_SIZE), 
			ConfigurationHelper.getIntSystemThenEnvProperty(poolName.toLowerCase() + CONFIG_MAX_POOL_SIZE, DEFAULT_MAX_POOL_SIZE), 
			ConfigurationHelper.getIntSystemThenEnvProperty(poolName.toLowerCase() + CONFIG_MAX_QUEUE_SIZE, DEFAULT_MAX_QUEUE_SIZE), 
			ConfigurationHelper.getLongSystemThenEnvProperty(poolName.toLowerCase() + CONFIG_KEEP_ALIVE, DEFAULT_KEEP_ALIVE), 
			ConfigurationHelper.getIntSystemThenEnvProperty(poolName.toLowerCase() + CONFIG_WINDOW_SIZE, DEFAULT_WINDOW_SIZE),
			ConfigurationHelper.getIntSystemThenEnvProperty(poolName.toLowerCase() + CONFIG_WINDOW_PERCENTILE, DEFAULT_WINDOW_PERCENTILE),
			publishJMX
		);
		int prestart = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_CORE_PRESTART, DEFAULT_CORE_PRESTART);
		for(int i = 0; i < prestart; i++) {
			prestartCoreThread();
		}
	}
	/**
	 * Creates a new JMXManagedThreadPool and publishes the JMX MBean management interface
	 * @param objectName The JMX ObjectName for this pool's MBean 
	 * @param poolName The pool name
	 * @param corePoolSize  the number of threads to keep in the pool, even if they are idle.
	 * @param maximumPoolSize the maximum number of threads to allow in the pool.
	 * @param queueSize The maximum number of pending tasks to queue
	 * @param keepAliveTimeMs when the number of threads is greater than the core, this is the maximum time in ms. that excess idle threads will wait for new tasks before terminating.
	 * @param metricWindowSize The maximum size of the metrics sliding window
	 * @param metricDefaultPercentile The default percentile reported in the metrics management  
	 */
	public JMXManagedThreadPool(ObjectName objectName, String poolName, int corePoolSize, int maximumPoolSize, int queueSize, long keepAliveTimeMs, int metricWindowSize, int metricDefaultPercentile) {
		this(objectName, poolName, corePoolSize, maximumPoolSize, queueSize, keepAliveTimeMs, metricWindowSize, metricDefaultPercentile, true);
		
	}
	/**
	 * Sets the thread pool's uncaught exception handler
	 * @param exceptionHandler the handler to set
	 */
	public void setUncaughtExceptionHandler(final UncaughtExceptionHandler exceptionHandler) {
		if(exceptionHandler!=null) {
			this.exceptionHandler = exceptionHandler;
		}
	}
	
	
	
	
	/**
	 * Creates a new JMXManagedThreadPool
	 * @param objectName The JMX ObjectName for this pool's MBean 
	 * @param poolName The pool name
	 * @param corePoolSize  the number of threads to keep in the pool, even if they are idle.
	 * @param maximumPoolSize the maximum number of threads to allow in the pool.
	 * @param queueSize The maximum number of pending tasks to queue
	 * @param keepAliveTimeMs when the number of threads is greater than the core, this is the maximum time in ms. that excess idle threads will wait for new tasks before terminating.
	 * @param metricWindowSize The maximum size of the metrics sliding window
	 * @param metricDefaultPercentile The default percentile reported in the metrics management  
	 * @param publishJMX If true, publishes the management interface
	 */
	public JMXManagedThreadPool(ObjectName objectName, String poolName, int corePoolSize, int maximumPoolSize, int queueSize, long keepAliveTimeMs, int metricWindowSize, int metricDefaultPercentile, boolean publishJMX) {
		this(null, objectName, poolName, corePoolSize, maximumPoolSize, queueSize, keepAliveTimeMs, metricWindowSize, metricDefaultPercentile, publishJMX);		
	}
	
	/**
	 * Creates a new JMXManagedThreadPool
	 * @param threadFactory An optional thread factory to create this pool's threads
	 * @param objectName The JMX ObjectName for this pool's MBean 
	 * @param poolName The pool name
	 * @param corePoolSize  the number of threads to keep in the pool, even if they are idle.
	 * @param maximumPoolSize the maximum number of threads to allow in the pool.
	 * @param queueSize The maximum number of pending tasks to queue
	 * @param keepAliveTimeMs when the number of threads is greater than the core, this is the maximum time in ms. that excess idle threads will wait for new tasks before terminating.
	 * @param metricWindowSize The maximum size of the metrics sliding window
	 * @param metricDefaultPercentile The default percentile reported in the metrics management  
	 * @param publishJMX If true, publishes the management interface
	 */
	public JMXManagedThreadPool(final ThreadFactory threadFactory, ObjectName objectName, String poolName, int corePoolSize, int maximumPoolSize, int queueSize, long keepAliveTimeMs, int metricWindowSize, int metricDefaultPercentile, boolean publishJMX) {
		super(corePoolSize, maximumPoolSize, keepAliveTimeMs, TimeUnit.MILLISECONDS, 
				queueSize==1 ? new SynchronousQueue<Runnable>() : new ArrayBlockingQueue<Runnable>(queueSize, false));
		this.threadGroup = new ThreadGroup(poolName + "ThreadGroup");
		setThreadFactory(threadFactory==null ? this : threadFactory);
		setRejectedExecutionHandler(this);		
		this.objectName = objectName;
		this.poolName = poolName;
		workQueue = (BlockingQueue<Runnable>)getQueue();
		if(publishJMX) {
			try {			
				JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
			} catch (Exception ex) {
				System.err.println("Failed to register JMX management interface. Will continue without:" + ex);
			}		
			System.err.println("Created JMX Managed Thread Pool [" + poolName + "]");
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#getInstance()
	 */
	public JMXManagedThreadPool getInstance() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadPoolExecutor#beforeExecute(java.lang.Thread, java.lang.Runnable)
	 */
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		taskStartTime.get()[0] = System.currentTimeMillis();
		super.beforeExecute(t, r);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadPoolExecutor#afterExecute(java.lang.Runnable, java.lang.Throwable)
	 */
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		if(t==null) {
			@SuppressWarnings("unused")  // TODO: tabulate task elapsed times
			long elapsed = System.currentTimeMillis() - taskStartTime.get()[0]; 
		}
		super.afterExecute(r, t);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		uncaughtExceptionCount.incrementAndGet();
		System.err.println("Thread pool [" + this.poolName + "] handled uncaught exception on thread [" + t + "]:" + e);
		if(exceptionHandler!=null) {
			exceptionHandler.uncaughtException(t, e);
		}
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		rejectedExecutionCount.incrementAndGet();
		System.err.println("Submitted execution task [" + r + "] was rejected by pool [" + this.poolName + "] due to a full task queue");		
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(threadGroup, r, poolName + "Thread#" + threadSerial.incrementAndGet());
		t.setDaemon(true);
		return t;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#getObjectName()
	 */
	@Override
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#getPoolName()
	 */
	@Override
	public String getPoolName() {
		return poolName;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#getQueueDepth()
	 */
	@Override
	public int getQueueDepth() {
		return workQueue.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#getQueueCapacity()
	 */
	@Override
	public int getQueueCapacity() {
		return workQueue.remainingCapacity();
	}
	

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#getUncaughtExceptionCount()
	 */
	@Override
	public long getUncaughtExceptionCount() {
		return uncaughtExceptionCount.get();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#getRejectedExecutionCount()
	 */
	@Override
	public long getRejectedExecutionCount() {
		return rejectedExecutionCount.get();
	}

//	/**
//	 * {@inheritDoc}
//	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#getMetrics()
//	 */
//	@Override
//	public Map<String, Long> getMetrics() {
//		Map<String, Long> map = new TreeMap<String, Long>();
//		for(Map.Entry<MetricType, Long> ex: metrics.getMetrics().entrySet()) {
//			map.put(ex.getKey().name(), ex.getValue());
//		}
//    	return map;
//	}
	
//	/**
//	 * {@inheritDoc}
//	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#getMetricsTable()
//	 */
//	@Override
//	public String getMetricsTable() {
//		StringBuilder b = new StringBuilder(METRIC_TABLE_HEADER);
//		b.append("<tr>");
//		b.append("<td>").append(poolName).append("</td>");
//		for(Map.Entry<String, Long> ex: getMetrics().entrySet()) {
//			b.append("<td>").append(ex.getValue()).append("</td>");
//		}
//		b.append("</tr>");    					
//		return b.append("</table>").toString();
//	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#getExecutingTaskCount()
	 */
	@Override
	public long getExecutingTaskCount() {
		return getTaskCount()-getCompletedTaskCount();
	}
	
	public void execute(Runnable r) {
		super.execute(r);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable)
	 */
	@Override
	public Future<?> submit(Runnable task) {
		return super.submit(task);
	}
	
	/**
	 * Executes a runnable task asynchronously, optionally executing the passed pre and post tasks before and after respectively.
	 * @param task The main task to execute
	 * @param preTask The optional pre-task to execute before the main task. Ignored if null.
	 * @param postTask The optional post-task to execute after the main task. Ignored if null.
	 * @param handler An optional uncaught exception handler, registered with the executing thread for this task. Ignored if null.
	 * @return a Future representing pending completion of the task
	 */
	public Future<?> submit(final Runnable task, final Runnable preTask, final Runnable postTask, final UncaughtExceptionHandler handler) {
		return submit(new Runnable(){
			public void run() {
				final UncaughtExceptionHandler currentHandler = Thread.currentThread().getUncaughtExceptionHandler();
				try {
					if(handler!=null) {
						Thread.currentThread().setUncaughtExceptionHandler(handler);
					}
					if(preTask!=null) preTask.run();
					task.run();
					if(postTask!=null) postTask.run();
				} finally {
					Thread.currentThread().setUncaughtExceptionHandler(currentHandler);
				}
			}
		});
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.JMXManagedThreadPoolMBean#stop()
	 */
	@Override
	public void stop() {
		shutdownNow();
		try { JMXHelper.unregisterMBean(objectName); } catch (Exception x) { /* No Op */}
	}
	
	@Override
	public void shutdown() {
		try {
			super.shutdown();
		} finally {
			try { JMXHelper.unregisterMBean(objectName); } catch (Exception x) {/* No Op */}
		}
	}
	
	@Override
	public List<Runnable> shutdownNow() {
		try {
			return super.shutdownNow();
		} finally {
			try { JMXHelper.unregisterMBean(objectName); } catch (Exception x) {/* No Op */}
		}
	}

	@Override
	public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
		try {
			return super.awaitTermination(timeout, unit);
		} finally {
			try { JMXHelper.unregisterMBean(objectName); } catch (Exception x) {/* No Op */}
		}		
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable, java.lang.Object)
	 */
	@Override
	public <T> Future<T> submit(Runnable task, T result) {		
		return super.submit(task, result);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.util.concurrent.Callable)
	 */
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return super.submit(task);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#getKeepAliveTime()
	 */
	@Override
	public long getKeepAliveTime() {
		return getKeepAliveTime(TimeUnit.MILLISECONDS);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#setKeepAliveTime(long)
	 */
	@Override
	public void setKeepAliveTime(long keepAliveTimeMs) {
		setKeepAliveTime(keepAliveTimeMs, TimeUnit.MILLISECONDS);
	}
	
//	/**
//	 * {@inheritDoc}
//	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#reset()
//	 */
//	@Override
//	public void reset() {
//		t
//	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jmx.concurrency.JMXManagedThreadPoolMBean#waitForCompletion(java.util.Collection, long)
	 */
	@Override
	public boolean waitForCompletion(Collection<Future<?>> futures, long timeout) {
		if(futures.isEmpty()) return true;
		final long expiryTime = System.currentTimeMillis() + timeout;
		final boolean[] bust = new boolean[]{false};
		while(System.currentTimeMillis() <= expiryTime) {
			if(bust[0]) return false;
			for(Iterator<Future<?>> fiter = futures.iterator(); fiter.hasNext();) {
				Future<?> f = fiter.next();
				if(f.isDone() || f.isCancelled()) {
					fiter.remove();
				} else {
					try {
						f.get(200, TimeUnit.MILLISECONDS);
						fiter.remove();
					} catch (CancellationException e) {
						System.err.println("Task Was Cancelled:" + e);
						fiter.remove();						
					} catch (InterruptedException e) {
						System.err.println("Thread interrupted while waiting for task check to complete:" + e);
						bust[0] = true;
					} catch (ExecutionException e) {
						System.err.println("Task Failed:" + e);
						fiter.remove();
					} catch (TimeoutException e) {
						/* No Op */
					} catch (Exception e) {
						System.err.println("Task Failure. Cancelling check:" + e);
						fiter.remove();
					}
				}
			}
			if(futures.isEmpty()) return true;
		}
		for(Future<?> f: futures) { f.cancel(true); }
		System.err.println("Task completion timed out with [" + futures.size() + "] tasks incomplete");
		futures.clear();
		return false;
	}
	
	public static JMXManagedThreadPoolBuilder builder() {
		return new JMXManagedThreadPoolBuilder();
	}

	public static class JMXManagedThreadPoolBuilder {
		public static final int CORES = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
		public static final String OBJECT_NAME_TEMPLATE = "com.heliosapm.threading:service=ThreadPool,name=%s";
		private static final AtomicInteger serial = new AtomicInteger(0);
		private ObjectName objectName = null;
		private String poolName = null;
		private int corePoolSize = CORES;
		private int maximumPoolSize = CORES * 2;
		private int queueSize = CORES * 100;
		private long keepAliveTimeMs = 60000;
		private int metricWindowSize = 1000;
		private int metricDefaultPercentile = 99;
		private boolean publishJMX = true;
		private int prestart = 0;
		private ThreadFactory threadFactory = null;
		private Thread.UncaughtExceptionHandler uncaughtHandler = null;
		private RejectedExecutionHandler rejectionHandler = null; 
		
		/**
		 * Builds and returns the configured JMXManagedThreadPool
		 * @return the configured JMXManagedThreadPool
		 */
		public JMXManagedThreadPool build() {
			if(!publishJMX) {
				objectName = null;
				if((poolName==null||poolName.trim().isEmpty())) {
					poolName = "Pool#" + serial.incrementAndGet();
				}
			} else {
				if(objectName==null && (poolName==null||poolName.trim().isEmpty())) {
					poolName = "Pool#" + serial.incrementAndGet();
					objectName = JMXHelper.objectName(String.format(OBJECT_NAME_TEMPLATE, poolName));
				} else {
					if(objectName==null) {
						objectName = JMXHelper.objectName(String.format(OBJECT_NAME_TEMPLATE, poolName));
					}
//					else {
//						poolName = "Pool#" + serial.incrementAndGet();
//					}
				}				
			}
			if(prestart > 0 && prestart > maximumPoolSize) {
				prestart = maximumPoolSize;
			}
			JMXManagedThreadPool pool = new JMXManagedThreadPool(objectName, poolName, corePoolSize, maximumPoolSize, queueSize, keepAliveTimeMs, metricWindowSize, metricDefaultPercentile, publishJMX);
			if(rejectionHandler!=null) {
				pool.setRejectedExecutionHandler(rejectionHandler);
			}
			if(uncaughtHandler!=null) {
				pool.setUncaughtExceptionHandler(uncaughtHandler);
			}
			if(prestart > 0) {
				for(int i = 0; i < prestart; i++) {
					pool.prestartCoreThread();
				}
			}
			return pool;
		}
		
		/**
		 * Sets 
		 * @param objectName the objectName to set
		 * @return this builder
		 */
		public JMXManagedThreadPoolBuilder objectName(final ObjectName objectName) {
			if(objectName==null) throw new IllegalArgumentException("The passed ObjectName was null");
			this.objectName = objectName;
			return this;
		}
		
		/**
		 * Sets the managed thread pool's thread factory
		 * @param threadFactory The thread factory the pool will use
		 * @return this builder
		 */
		public JMXManagedThreadPoolBuilder threadFactory(final ThreadFactory threadFactory) {
			if(threadFactory==null) throw new IllegalArgumentException("The passed ThreadFactory was null");
			this.threadFactory = threadFactory;
			return this;
		}
		
		/**
		 * Sets the pool name 
		 * @param poolName the poolName to set
		 * @return this builder
		 */
		public JMXManagedThreadPoolBuilder poolName(final String poolName) {
			if(poolName==null || poolName.trim().isEmpty()) throw new IllegalArgumentException("The passed Pool Name was null or empty");
			this.poolName = poolName;
			return this;
		}
		/**
		 * Sets 
		 * @param corePoolSize the corePoolSize to set
		 * @return this builder
		 */
		public JMXManagedThreadPoolBuilder corePoolSize(final int corePoolSize) {
			if(corePoolSize < 1) throw new IllegalArgumentException("Invalid core pool size [" + corePoolSize + "]");
			this.corePoolSize = corePoolSize;
			return this;
		}
		/**
		 * Sets 
		 * @param maximumPoolSize the maximumPoolSize to set
		 * @return this builder
		 */
		public JMXManagedThreadPoolBuilder maxPoolSize(final int maximumPoolSize) {
			if(maximumPoolSize < 1) throw new IllegalArgumentException("Invalid max pool size [" + maximumPoolSize + "]");
			this.maximumPoolSize = maximumPoolSize;
			return this;
		}
		/**
		 * Sets 
		 * @param queueSize the queueSize to set
		 * @return this builder
		 */
		public JMXManagedThreadPoolBuilder queueSize(final int queueSize) {
			if(queueSize < 1) throw new IllegalArgumentException("Invalid queue size [" + queueSize + "]");
			this.queueSize = queueSize;
			return this;
		}
		/**
		 * Sets 
		 * @param keepAliveTimeMs the keepAliveTimeMs to set
		 * @return this builder
		 */
		public JMXManagedThreadPoolBuilder keepAliveTimeMs(final long keepAliveTimeMs) {
			if(keepAliveTimeMs < 1) throw new IllegalArgumentException("Invalid keep alive time [" + keepAliveTimeMs + "] ms.");
			this.keepAliveTimeMs = keepAliveTimeMs;
			return this;
		}
		/**
		 * Sets 
		 * @param metricWindowSize the metricWindowSize to set
		 * @return this builder
		 */
		public JMXManagedThreadPoolBuilder metricWindowSize(final int metricWindowSize) {
			if(metricWindowSize < 1) throw new IllegalArgumentException("Invalid metric window size [" + metricWindowSize + "]");
			this.metricWindowSize = metricWindowSize;
			return this;
		}
		/**
		 * Sets 
		 * @param metricDefaultPercentile the metricDefaultPercentile to set
		 * @return this builder
		 */
		public JMXManagedThreadPoolBuilder metricDefaultPercentile(final int metricDefaultPercentile) {
			if(metricDefaultPercentile < 1 || metricDefaultPercentile > 99) throw new IllegalArgumentException("Invalid metric default percentile [" + metricDefaultPercentile + "]");
			this.metricDefaultPercentile = metricDefaultPercentile;
			return this;
		}
		/**
		 * Sets 
		 * @param publishJMX the publishJMX to set
		 * @return this builder
		 */
		public JMXManagedThreadPoolBuilder publishJMX(final boolean publishJMX) {
			this.publishJMX = publishJMX;
			return this;
		}


		/**
		 * Sets 
		 * @param prestart the prestart to set
		 * @return this builder
		 */
		public JMXManagedThreadPoolBuilder prestart(final int prestart) {
			if(prestart < 1) throw new IllegalArgumentException("Invalid prestart [" + prestart + "]");
			this.prestart = prestart;
			return this;
		}

		/**
		 * Sets 
		 * @param uncaughtHandler the uncaughtHandler to set
		 * @return this builder
		 */
		public JMXManagedThreadPoolBuilder uncaughtHandler(Thread.UncaughtExceptionHandler uncaughtHandler) {
			if(uncaughtHandler==null) throw new IllegalArgumentException("The passed UncaughtExceptionHandler was null");
			this.uncaughtHandler = uncaughtHandler;
			return this;
		}

		/**
		 * Sets 
		 * @param rejectionHandler the rejectionHandler to set
		 * @return this builder
		 */
		public JMXManagedThreadPoolBuilder rejectionHandler(RejectedExecutionHandler rejectionHandler) {
			if(rejectionHandler==null) throw new IllegalArgumentException("The passed RejectedExecutionHandler was null");
			this.rejectionHandler = rejectionHandler;
			return this;
		}
		
		
		
		
	}

}