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
package com.heliosapm.utils.concurrency;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.jmx.SharedNotificationExecutor;

/**
 * <p>Title: ExtendedThreadManager</p>
 * <p>Description: A drop in replacement for the standard {@link ThreadMXBean} that adds additional functionality and notifications.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.concurrency.ExtendedThreadManager</code></p>
 */

public class ExtendedThreadManager extends NotificationBroadcasterSupport implements ExtendedThreadManagerMBean {
	private static final MBeanNotificationInfo[] notificationInfo = createMBeanInfo();
	/** The delegate ThreadMXBean */
	protected final ThreadMXBean delegate;
	/** Indicates if the delegate is installed */
	protected static final AtomicBoolean installed = new AtomicBoolean(false);
	/** The platform MBeanServer */
	protected static final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
	/** The ThreadMXBean object name */
	protected static final ObjectName THREAD_MX_NAME = JMXHelper.objectName(ManagementFactory.THREAD_MXBEAN_NAME);
	/** The JMX notification type emitted when Thread Contention Monitoring is enabled */
	public static final String NOTIF_TCM_ENABLED = "threadmxbean.tcm.enabled";
	/** The JMX notification type emitted when Thread Contention Monitoring is disabled */
	public static final String NOTIF_TCM_DISABLED = "threadmxbean.tcm.disabled";
	/** The JMX notification type emitted when Thread Timing is enabled */
	public static final String NOTIF_TCT_ENABLED = "threadmxbean.tct.enabled";
	/** The JMX notification type emitted when Thread Timing is disabled */
	public static final String NOTIF_TCT_DISABLED = "threadmxbean.tct.disabled";
	/** The extended thread manager instance */
	private static ExtendedThreadManager mxb = null;
	/** JMX notification serial number generator */
	private static final AtomicLong serial = new AtomicLong(0L);
	/** The original ThreadMXBean */
	public static final ThreadMXBean original = ManagementFactory.getThreadMXBean();
	/** Indicates if thread contention monitoring is supported */
	public static final boolean TCM_SUPPORTED = original.isThreadContentionMonitoringSupported();
	/** Indicates if thread cpu timing monitoring is supported */
	public static final boolean TCT_SUPPORTED = original.isThreadCpuTimeSupported();
	
	/** The default max depth to get thread Infos with */
	private int maxDepth = Integer.MAX_VALUE;
	
	// record initial tct and tcm states, store in statics
	
	public static void main(String[] args) {
		log("Installing Notifier");
		install();
		try { Thread.currentThread().join(); } catch (Exception ex) {};
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Installs and return the ExtendedThreadManager
	 * @return the ExtendedThreadManager instance
	 */
	public static ExtendedThreadManager install() {
		if(!installed.get()) {
			mxb = new ExtendedThreadManager(ManagementFactory.getThreadMXBean());
			try {
				server.unregisterMBean(THREAD_MX_NAME);				
				server.registerMBean(mxb, THREAD_MX_NAME);
				installed.set(true);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
				throw new RuntimeException("Failed to install ExtendedThreadManager", ex);
			}
		}
		return mxb;
	}
	
	/**
	 * Removes the extended thread manager and restores the native version.
	 * Could possibly fail, leaving the JVM with no Threading MBean. Use carefully.... 
	 */
	public static void remove() {
		if(installed.compareAndSet(true, false)) {
			// this may not always work ....
			try {
				server.unregisterMBean(THREAD_MX_NAME);
			} catch (Exception ex) {
				// ok, no harm done. bail out here.
				installed.set(true);
				return;
			}
			try {
				server.registerMBean(original, THREAD_MX_NAME);
			} catch (Exception ex) {
				// yikes, now we have no mbean...
			}
		}
	}
	
	public static boolean isInstalled() {
		return installed.get();
	}
	
	/**
	 * Creates a new ExtendedThreadManager
	 * @param delegate the ThreadMXBean delegate
	 */
	private ExtendedThreadManager(final ThreadMXBean delegate) {
		super(SharedNotificationExecutor.getInstance(), notificationInfo);
		this.delegate = delegate;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.PlatformManagedObject#getObjectName()
	 */
	@Override
	public ObjectName getObjectName() {
		return delegate.getObjectName();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getThreadCount()
	 */
	@Override
	public int getThreadCount() {
		return delegate.getThreadCount();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getPeakThreadCount()
	 */
	@Override
	public int getPeakThreadCount() {
		return delegate.getPeakThreadCount();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getTotalStartedThreadCount()
	 */
	@Override
	public long getTotalStartedThreadCount() {
		return delegate.getTotalStartedThreadCount();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getDaemonThreadCount()
	 */
	@Override
	public int getDaemonThreadCount() {
		return delegate.getDaemonThreadCount();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.concurrency.ExtendedThreadManagerMBean#getNonDaemonThreadCount()
	 */
	@Override
	public int getNonDaemonThreadCount() {
		return delegate.getThreadCount() - delegate.getDaemonThreadCount();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getAllThreadIds()
	 */
	@Override
	public long[] getAllThreadIds() {
		return delegate.getAllThreadIds();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getThreadInfo(long)
	 */
	@Override
	public ThreadInfo getThreadInfo(long id) {
		return delegate.getThreadInfo(id);
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getThreadInfo(long[])
	 */
	@Override
	public ThreadInfo[] getThreadInfo(long[] ids) {
		return delegate.getThreadInfo(ids);
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getThreadInfo(long, int)
	 */
	@Override
	public ThreadInfo getThreadInfo(long id, int maxDepth) {
		return delegate.getThreadInfo(id, maxDepth);
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getThreadInfo(long[], int)
	 */
	@Override
	public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) {
		return delegate.getThreadInfo(ids, maxDepth);
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#isThreadContentionMonitoringSupported()
	 */
	@Override
	public boolean isThreadContentionMonitoringSupported() {
		return delegate.isThreadContentionMonitoringSupported();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#isThreadContentionMonitoringEnabled()
	 */
	@Override
	public boolean isThreadContentionMonitoringEnabled() {
		return delegate.isThreadContentionMonitoringEnabled();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#setThreadContentionMonitoringEnabled(boolean)
	 */
	@Override
	public void setThreadContentionMonitoringEnabled(boolean enable) {
		delegate.setThreadContentionMonitoringEnabled(enable);
		if(enable) {
			sendNotification(new Notification(NOTIF_TCM_ENABLED, THREAD_MX_NAME, serial.incrementAndGet(), System.currentTimeMillis(), "Thread Contention Monitoring Enabled"));
		} else {
			sendNotification(new Notification(NOTIF_TCM_DISABLED, THREAD_MX_NAME, serial.incrementAndGet(), System.currentTimeMillis(), "Thread Contention Monitoring Disabled"));
		}
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getCurrentThreadCpuTime()
	 */
	@Override
	public long getCurrentThreadCpuTime() {
		return delegate.getCurrentThreadCpuTime();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getCurrentThreadUserTime()
	 */
	@Override
	public long getCurrentThreadUserTime() {
		return delegate.getCurrentThreadUserTime();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getThreadCpuTime(long)
	 */
	@Override
	public long getThreadCpuTime(long id) {
		return delegate.getThreadCpuTime(id);
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getThreadUserTime(long)
	 */
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getThreadUserTime(long)
	 */
	@Override
	public long getThreadUserTime(long id) {
		return delegate.getThreadUserTime(id);
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#isThreadCpuTimeSupported()
	 */
	@Override
	public boolean isThreadCpuTimeSupported() {
		return delegate.isThreadCpuTimeSupported();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#isCurrentThreadCpuTimeSupported()
	 */
	@Override
	public boolean isCurrentThreadCpuTimeSupported() {
		return delegate.isCurrentThreadCpuTimeSupported();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#isThreadCpuTimeEnabled()
	 */
	@Override
	public boolean isThreadCpuTimeEnabled() {
		return delegate.isThreadCpuTimeEnabled();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#setThreadCpuTimeEnabled(boolean)
	 */
	@Override
	public void setThreadCpuTimeEnabled(boolean enable) {
		delegate.setThreadCpuTimeEnabled(enable);
		if(enable) {
			sendNotification(new Notification(NOTIF_TCT_ENABLED, THREAD_MX_NAME, serial.incrementAndGet(), System.currentTimeMillis(), "Thread CPU Time Monitoring Enabled"));
		} else {
			sendNotification(new Notification(NOTIF_TCT_DISABLED, THREAD_MX_NAME, serial.incrementAndGet(), System.currentTimeMillis(), "Thread CPU Time Monitoring Disabled"));
		}		
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#findMonitorDeadlockedThreads()
	 */
	@Override
	public long[] findMonitorDeadlockedThreads() {
		return delegate.findMonitorDeadlockedThreads();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#resetPeakThreadCount()
	 */
	@Override
	public void resetPeakThreadCount() {
		delegate.resetPeakThreadCount();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#findDeadlockedThreads()
	 */
	@Override
	public long[] findDeadlockedThreads() {
		return delegate.findDeadlockedThreads();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#isObjectMonitorUsageSupported()
	 */
	@Override
	public boolean isObjectMonitorUsageSupported() {
		return delegate.isObjectMonitorUsageSupported();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#isSynchronizerUsageSupported()
	 */
	@Override
	public boolean isSynchronizerUsageSupported() {
		return delegate.isSynchronizerUsageSupported();
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#getThreadInfo(long[], boolean, boolean)
	 */
	@Override
	public ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors,
			boolean lockedSynchronizers) {
		return delegate.getThreadInfo(ids, lockedMonitors, lockedSynchronizers);
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#dumpAllThreads(boolean, boolean)
	 */
	@Override
	public ThreadInfo[] dumpAllThreads(boolean lockedMonitors,
			boolean lockedSynchronizers) {
		return delegate.dumpAllThreads(lockedMonitors, lockedSynchronizers);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.concurrency.ExtendedThreadManagerMBean#getNonDaemonThreadNames()
	 */
	@Override
	public String[] getNonDaemonThreadNames() {
		final CountDownLatch latch = new CountDownLatch(1);
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		ThreadGroup main = tg;
		while((tg=tg.getParent())!=null) {
			main = tg;
		}
		final ThreadGroup MAIN = main;
		final Set<String> threadNames=  new HashSet<String>(); 
		Thread t = new Thread(main, "NonDaemonFinder") {
			public void run() {
				try {			
					Thread[] allThreads = new Thread[getThreadCount()*10];
					MAIN.enumerate(allThreads, true);					
					for(Thread t: allThreads) {
						if(t==null) break;				
						if(!t.isDaemon()) {
							threadNames.add(t.toString());
						}
					}	
				} catch (Exception ex) {
					ex.printStackTrace(System.err);
					throw new RuntimeException("Failed to list non-daemon threads:" + ex);
				} finally {
					latch.countDown();
				}
			}
		};		
		t.setDaemon(true);
		t.start();
		try { latch.await(5000, TimeUnit.MILLISECONDS); } catch (Exception ex) {}
		return threadNames.toArray(new String[threadNames.size()]);
	}
	
/*
Thread[AWT-Shutdown,5,main]
Thread[AWT-EventQueue-0,6,main]
Thread[DestroyJavaVM,5,main]
Thread[Thread-10,6,main]
 */
	
	private static MBeanNotificationInfo[] createMBeanInfo() {
		return new MBeanNotificationInfo[]{
			new MBeanNotificationInfo(new String[]{NOTIF_TCM_ENABLED, NOTIF_TCM_DISABLED}, Notification.class.getName(), "Notification indicating if ThreadContentionMonitoring (tcm) enablement has changed"),
			new MBeanNotificationInfo(new String[]{NOTIF_TCT_ENABLED, NOTIF_TCT_DISABLED}, Notification.class.getName(), "Notification indicating if ThreadContentionMonitoring (tcm) enablement has changed"),
		};		
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.concurrency.ExtendedThreadManagerMBean#getBusyThreads(long)
	 */
	@Override
	public String[] getBusyThreads(long sampleTime) {
//		SimpleLogger.info("Starting BusyThreads");
		ThreadInfo[] infos = delegate.getThreadInfo(delegate.getAllThreadIds());
		Map<String, Long> init = new HashMap<String, Long>(infos.length);		
		for(ThreadInfo ti: infos) {
			init.put(ti.getThreadName() + ":" + ti.getThreadId(), delegate.getThreadCpuTime(ti.getThreadId()));
		}
//		SimpleLogger.info("BusyThreads Sampling Time [", sampleTime, "] ms.");
//		SystemClock.sleep(sampleTime);
//		SimpleLogger.info("Completed BusyThreads Sampling");
		infos = delegate.getThreadInfo(delegate.getAllThreadIds());
		Set<BusyThread> bthreads = new TreeSet<BusyThread>();
		for(ThreadInfo ti: infos) {
			String key = ti.getThreadName() + ":" + ti.getThreadId();
			if(!init.containsKey(key)) continue;
			long elapsedCpu = delegate.getThreadCpuTime(ti.getThreadId())-init.get(key);
			bthreads.add(new BusyThread(elapsedCpu, key));			
		}
		String[] out = new String[bthreads.size()];
		int cnt = 0;
		for(BusyThread bt: bthreads) {
			out[cnt] = bt.toString();
			cnt++;
		}
		return out;
	}
	
	private static final long[] NO_MATCH_STATS = {0, -1, -1, -1, -1, -1, -1};
	
	
	/**
	 * Returns summed up thread stats for all threads with names matching the passed regex.
	 * @param pattern The regex pattern to match against the threads
	 * @return a long array with the following stats: <ol>
	 *  <li>The total number of threads that matched</li>
	 * 	<li>Sys Cpu Time</li>		1
	 *  <li>User Cpu Time</li> 		2
	 *  <li>Wait Count</li>			3
	 *  <li>Wait Time</li>			4
	 *  <li>Block Count</li>		5
	 *  <li>Block Time</li>			6
	 * </ol>
	 * Any stat which is not enabled will be returned as a -1.
	 */
	public long[] getSummedThreadStats(final String pattern) {
		if(pattern==null || pattern.trim().isEmpty()) return NO_MATCH_STATS;
		final boolean cpu = isThreadCpuTimeEnabled();
		final boolean contention = isThreadContentionMonitoringEnabled();
		final Pattern p = Pattern.compile(pattern.trim());
		final long[] stats = new long[NO_MATCH_STATS.length];
		if(!cpu) {
			stats[1] = -1;
			stats[2] = -1;
		}
		if(!contention) {
			stats[4] = -1;
			stats[6] = -1;			
		}
		final ThreadInfo[] infos = delegate.getThreadInfo(delegate.getAllThreadIds(), 0);
		for(ThreadInfo t: infos) {
			if(p.matcher(t.getThreadName()).matches()) {
				final long tid = t.getThreadId();
				stats[0]++;
				stats[3] = t.getWaitedCount();
				stats[5] = t.getBlockedCount();				
				if(cpu) {
					stats[1] += original.getThreadCpuTime(tid);
					stats[2] += original.getThreadUserTime(tid);
				}
				if(contention) {
					stats[4] += t.getWaitedTime();
					stats[6] += t.getBlockedTime();
				}				
			}
		}		
		return stats;
	}
	
	/**
	 * Returns the arithmetic average of all thread stats for all threads with names matching the passed regex.
	 * @param pattern The regex pattern to match against the threads
	 * @return a long array with the following stats: <ol>
	 *  <li>The total number of threads that matched</li>
	 * 	<li>Sys Cpu Time</li>		1
	 *  <li>User Cpu Time</li> 		2
	 *  <li>Wait Count</li>			3
	 *  <li>Wait Time</li>			4
	 *  <li>Block Count</li>		5
	 *  <li>Block Time</li>			6
	 * </ol>
	 * Any stat which is not enabled will be returned as a -1.
	 */
	public long[] getAverageThreadStats(final String pattern) {
		final long[] stats =getSummedThreadStats(pattern);
		if(stats[0]>0) {
			for(int i = 1; i < stats.length; i++) {
				if(stats[i]==-1) continue;
				stats[i] = avg(stats[i], stats[0]);
			}
		}
		return stats;
	}
	
	private static long avg(final double total, final double count) {
		if(total < 1 || count==0) return 0L;
		final double d = total/count;
		return (long)d;
	}
	
	

	/**
	 * Returns the max depth used for getting thread infos
	 * @return the max depth used for getting thread infos
	 */
	@Override
	public int getMaxDepth() {
		return maxDepth;
	}

	/**
	 * Sets the max depth used for getting thread infos
	 * @param maxDepth the max depth used for getting thread infos
	 */
	@Override
	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.concurrency.ExtendedThreadManagerMBean#getThreadInfo()
	 */
	@Override
	public ExtendedThreadInfo[] getThreadInfo() {
		return ExtendedThreadInfo.wrapThreadInfos(delegate.getThreadInfo(delegate.getAllThreadIds(), maxDepth));
	}
}
