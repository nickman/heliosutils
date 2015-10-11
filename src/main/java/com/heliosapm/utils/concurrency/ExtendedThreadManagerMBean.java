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

import java.lang.management.ThreadMXBean;

import javax.management.MXBean;
import javax.management.openmbean.CompositeData;

import com.heliosapm.utils.jmx.JMXHelper;

/**
 * <p>Title: ExtendedThreadManagerMXBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.concurrency.ExtendedThreadManagerMXBean</code></p>
 */
@MXBean(true)
public interface ExtendedThreadManagerMBean extends ThreadMXBean { 
	/**
	 * Returns the max depth used for getting thread infos
	 * @return the max depth used for getting thread infos
	 */
	public int getMaxDepth();

	/**
	 * Sets the max depth used for getting thread infos
	 * @param maxDepth the max depth used for getting thread infos
	 */
	public void setMaxDepth(int maxDepth);
	
	/**
	 * Returns an array ExtendedThreadInfos for all threads in the VM
	 * @return an array ExtendedThreadInfos for all threads in the VM
	 */
	public CompositeData[] getThreadInfo();
	
	/**
	 * Returns the number of non-daemon threads
	 * @return the number of non-daemon threads
	 */
	public int getNonDaemonThreadCount();
	
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
	public long[] getSummedThreadStats(final String pattern);
	
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
	public long[] getAverageThreadStats(final String pattern);
	
	/**
	 * Returns the names of the non-daemon threads
	 * @return the names of the non-daemon threads
	 */
	public String[] getNonDaemonThreadNames();
	
	/**
	 * Generates a list of thread names ordered in cpu utilization descending
	 * @param sampleTime The time to sample for
	 * @return a list of thread names with the cpu time appended
	 */
	public String[] getBusyThreads(long sampleTime);
	
	public boolean isThreadAllocatedMemorySupported();
	public boolean isThreadAllocatedMemoryEnabled();
	public void setThreadAllocatedMemoryEnabled(final boolean enable);
	public long getThreadAllocatedBytes(final long id);
	
	public long getThreadAllocatedBytes(final long[] ids);

}

