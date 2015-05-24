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

import java.lang.Thread.State;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;

/**
 * <p>Title: ExtendedThreadInfoMBean</p>
 * <p>Description: MXBean compatibility interface to expose {@link ExtendedThreadInfo}s as composite data instances.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.concurrency.ExtendedThreadInfoMBean</code></p>
 */

public interface ExtendedThreadInfoMBean {
	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public abstract int hashCode();

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public abstract boolean equals(Object obj);

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getThreadId()
	 */
	public abstract long getThreadId();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getThreadName()
	 */
	public abstract String getThreadName();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getThreadState()
	 */
	public abstract State getThreadState();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getBlockedTime()
	 */
	public abstract long getBlockedTime();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getBlockedCount()
	 */
	public abstract long getBlockedCount();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getWaitedTime()
	 */
	public abstract long getWaitedTime();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getWaitedCount()
	 */
	public abstract long getWaitedCount();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getLockInfo()
	 */
	public abstract LockInfo getLockInfo();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getLockName()
	 */
	public abstract String getLockName();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getLockOwnerId()
	 */
	public abstract long getLockOwnerId();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getLockOwnerName()
	 */
	public abstract String getLockOwnerName();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getStackTrace()
	 */
	public abstract StackTraceElement[] getStackTrace();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#isSuspended()
	 */
	public abstract boolean isSuspended();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#isInNative()
	 */
	public abstract boolean isInNative();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#toString()
	 */
	public abstract String toString();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getLockedMonitors()
	 */
	public abstract MonitorInfo[] getLockedMonitors();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getLockedSynchronizers()
	 */
	public abstract LockInfo[] getLockedSynchronizers();

}
