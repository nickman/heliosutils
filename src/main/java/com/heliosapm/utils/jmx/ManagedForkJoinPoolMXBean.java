/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.heliosapm.utils.jmx;

/**
 * <p>Title: ManagedForkJoinPoolMXBean</p>
 * <p>Description: JMX MXBean interface for {@link ManagedForkJoinPool} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.ManagedForkJoinPoolMXBean</code></p>
 */
public interface ManagedForkJoinPoolMXBean extends ForkJoinPoolManagement {
	
	/** The template for ManagedForkJoinPool JMX ObjectNames  */
	public static final String OBJECT_NAME_TEMPLATE = "com.heliosapm.tsdblite.concurrency:service=%s,type=ForkJoinPool";
	
	/**
	 * Returns the cummulative number of submitted callable tasks
	 * @return the number of submitted callable tasks
	 */
	public long getCallableTasks();
	
	/**
	 * Returns the cummulative number of submitted runnable tasks
	 * @return the number of submitted runnable tasks
	 */
	public long getRunnableTasks();
	
	/**
	 * Returns the cummulative number of submitted forkJoin tasks
	 * @return the number of submitted forkJoin tasks
	 */
	public long getForkJoinTasks();
	
	/**
	 * Resets the task counters
	 */
	public void resetCounters();

}