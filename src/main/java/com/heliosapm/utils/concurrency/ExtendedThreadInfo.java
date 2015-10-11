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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.Thread.State;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenType;

import com.sun.jmx.mbeanserver.MXBeanMapping;
import com.sun.jmx.mbeanserver.MXBeanMappingFactory;

/**
 * <p>Title: ExtendedThreadInfo</p>
 * <p>Description: Wrapper adding additional functionality for standard {@link ThreadInfo}s </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.concurrency.ExtendedThreadInfo</code></p>
 */

public class ExtendedThreadInfo implements ExtendedThreadInfoMBean, Serializable {
	/** The wrapped thread info */
	private final ThreadInfo delegate;
	
	/**
	 * Wraps an array of {@link ThreadInfo}s.
	 * @param infos The array of {@link ThreadInfo}s to wrap
	 * @return an array of ExtendedThreadInfos
	 */
	public static CompositeData[] wrapOpenTypeThreadInfos(ThreadInfo...infos) {
		try {
			CompositeData[] xinfos = new CompositeData[infos.length];
			for(int i = 0; i < infos.length; i++) {
				xinfos[i] = (CompositeData) mapping.toOpenValue(new ExtendedThreadInfo(infos[i]));
			}
			return xinfos;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static ExtendedThreadInfo[] wrapThreadInfos(ThreadInfo...infos) {
		final ExtendedThreadInfo[] xinfos = new ExtendedThreadInfo[infos.length];
		for(int i = 0; i < infos.length; i++) {
			xinfos[i] = new ExtendedThreadInfo(infos[i]);
		}			
		return xinfos;
	}
	
	
	private static final com.sun.jmx.mbeanserver.MXBeanMappingFactory factory = MXBeanMappingFactory.DEFAULT;
	private static final OpenType ot;
	private static final MXBeanMapping mapping;
	
	static {
		OpenType o = null;
		MXBeanMapping m = null;
		
		try {
			m = factory.mappingForType(ExtendedThreadInfo.class, factory);
			o = m.getOpenType();
		 } catch (Exception ex) {
			 ex.printStackTrace(System.err);
			 m = null;
			 o = null;
		 }
		ot = o;
		mapping = m;
	}
	
	Object writeReplace() throws ObjectStreamException {
		try {
			return mapping.toOpenValue(this);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
		
	}
	
	/**
	 * Creates a new ExtendedThreadInfo
	 * @param threadInfo the delegate thread info
	 */
	ExtendedThreadInfo(ThreadInfo threadInfo) {
		delegate = threadInfo;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#hashCode()
	 */
	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getThreadId()
	 */
	@Override
	public long getThreadId() {
		return delegate.getThreadId();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getThreadName()
	 */
	@Override
	public String getThreadName() {
		return delegate.getThreadName();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getThreadState()
	 */
	@Override
	public State getThreadState() {
		return delegate.getThreadState();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getBlockedTime()
	 */
	@Override
	public long getBlockedTime() {
		return delegate.getBlockedTime();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getBlockedCount()
	 */
	@Override
	public long getBlockedCount() {
		return delegate.getBlockedCount();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getWaitedTime()
	 */
	@Override
	public long getWaitedTime() {
		return delegate.getWaitedTime();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getWaitedCount()
	 */
	@Override
	public long getWaitedCount() {
		return delegate.getWaitedCount();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getLockInfo()
	 */
	@Override
	public LockInfo getLockInfo() {
		return delegate.getLockInfo();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getLockName()
	 */
	@Override
	public String getLockName() {
		return delegate.getLockName();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getLockOwnerId()
	 */
	@Override
	public long getLockOwnerId() {
		return delegate.getLockOwnerId();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getLockOwnerName()
	 */
	@Override
	public String getLockOwnerName() {
		return delegate.getLockOwnerName();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getStackTrace()
	 */
	@Override
	public StackTraceElement[] getStackTrace() {
		return delegate.getStackTrace();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#isSuspended()
	 */
	@Override
	public boolean isSuspended() {
		return delegate.isSuspended();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#isInNative()
	 */
	@Override
	public boolean isInNative() {
		return delegate.isInNative();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#toString()
	 */
	@Override
	public String toString() {
		return delegate.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getLockedMonitors()
	 */
	@Override
	public MonitorInfo[] getLockedMonitors() {
		return delegate.getLockedMonitors();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadInfoMBean#getLockedSynchronizers()
	 */
	@Override
	public LockInfo[] getLockedSynchronizers() {
		return delegate.getLockedSynchronizers();
	}
}

