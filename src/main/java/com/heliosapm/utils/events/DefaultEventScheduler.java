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
package com.heliosapm.utils.events;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.jmx.JMXManagedScheduler;
import com.heliosapm.utils.ssh.terminal.SSHService;

/**
 * <p>Title: DefaultEventScheduler</p>
 * <p>Description: The default event scheduler</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.DefaultEventScheduler</code></p>
 */

public class DefaultEventScheduler implements EventScheduler {
	/** The singleton instance */
	private static volatile DefaultEventScheduler instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The event task scheduler */
	protected final JMXManagedScheduler scheduler;
	
	/**
	 * Acquires and returns the DefaultEventScheduler singleton
	 * @return the DefaultEventScheduler singleton
	 */
	public static DefaultEventScheduler getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new DefaultEventScheduler();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new DefaultEventScheduler
	 */
	private DefaultEventScheduler() {
		scheduler = new JMXManagedScheduler(JMXHelper.objectName(getClass()), "EventScheduler", ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors(), true);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.EventScheduler#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public ScheduledFuture<?> schedule(final Runnable task, final long delay, final TimeUnit unit) {
		return scheduler.schedule(task, delay, unit);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.EventScheduler#schedule(java.lang.Runnable, long)
	 */
	@Override
	public ScheduledFuture<?> schedule(Runnable task, long delaySecs) {
		return scheduler.schedule(task, delaySecs, TimeUnit.SECONDS);
	}

}
