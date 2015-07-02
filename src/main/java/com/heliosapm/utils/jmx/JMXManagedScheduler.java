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
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import com.heliosapm.utils.config.ConfigurationHelper;



/**
 * <p>Title: JMXManagedScheduler</p>
 * <p>Description: A simple JMX managed scheduler</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.JMXManagedScheduler</code></p>
 */

public class JMXManagedScheduler extends ScheduledThreadPoolExecutor implements ThreadFactory, RejectedExecutionHandler, UncaughtExceptionHandler, JMXManagedSchedulerMBean {
	/** The JMX ObjectName for this pool's MBean */
	protected final ObjectName objectName;
	/** The pool name */
	protected final String poolName;
	
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
	
	/**
	 * Creates a new JMXManagedScheduler and publishes the JMX interface
	 * @param objectName THe ObjectName of this scheduler's MBean
	 * @param poolName The pool name
	 * @param corePoolSize The core pool size
	 */
	public JMXManagedScheduler(ObjectName objectName, String poolName, int corePoolSize) {
		this(objectName, poolName, corePoolSize, true);
	}


	/**
	 * Creates a new JMXManagedScheduler
	 * @param objectName THe ObjectName of this scheduler's MBean
	 * @param poolName The pool name
	 * @param corePoolSize The core pool size
	 * @param publishJMX If true, publishes the JMX interface
	 */
	public JMXManagedScheduler(ObjectName objectName, String poolName, int corePoolSize, boolean publishJMX) {
		super(corePoolSize);
		this.objectName = objectName;
		this.poolName = poolName;
		this.threadGroup = new ThreadGroup(poolName + "ThreadGroup");
		setThreadFactory(this);
		setRejectedExecutionHandler(this);
		if(publishJMX) {
			try {			
				JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to register Scheduler MBean [" + objectName + "]", ex);
			}
		}
		
	}

	/**
	 * Creates a new JMXManagedScheduler and publishes the JMX interface
	 * @param objectName THe ObjectName of this scheduler's MBean
	 * @param poolName The pool name
	 */
	public JMXManagedScheduler(ObjectName objectName, String poolName) {
		this(objectName, poolName, true);
	}
	
	
	/**
	 * Creates a new JMXManagedScheduler
	 * @param objectName THe ObjectName of this scheduler's MBean
	 * @param poolName The pool name
	 * @param publishJMX If true, publishes the JMX interface
	 */
	public JMXManagedScheduler(ObjectName objectName, String poolName, boolean publishJMX) {
		this(
			objectName, 
			poolName,				
			ConfigurationHelper.getIntSystemThenEnvProperty(poolName.toLowerCase() + CONFIG_CORE_SCHEDULER_POOL_SIZE, DEFAULT_CORE_SCHEDULER_POOL_SIZE),
			publishJMX
		);
	}
	


	
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		uncaughtExceptionCount.incrementAndGet();
		e.printStackTrace(System.err);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		rejectedExecutionCount.incrementAndGet();
//		log.log(Level.SEVERE, "Submitted execution task [" + r + "] was rejected due to a full task queue", new Throwable());		
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
	 * @see com.heliosapm.utils.jmx.JMXManagedSchedulerMBean#getObjectName()
	 */
	@Override
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.JMXManagedSchedulerMBean#getPoolName()
	 */
	@Override
	public String getPoolName() {
		return poolName;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.JMXManagedSchedulerMBean#getUncaughtExceptionCount()
	 */
	@Override
	public long getUncaughtExceptionCount() {
		return uncaughtExceptionCount.get();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.JMXManagedSchedulerMBean#getRejectedExecutionCount()
	 */
	@Override
	public long getRejectedExecutionCount() {
		return rejectedExecutionCount.get();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.JMXManagedSchedulerMBean#getExecutingTaskCount()
	 */
	@Override
	public long getExecutingTaskCount() {
		return getTaskCount()-getCompletedTaskCount();
	}
	

}
