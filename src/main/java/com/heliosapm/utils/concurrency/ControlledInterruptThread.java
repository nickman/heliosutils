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

/**
 * <p>Title: ControlledInterruptThread</p>
 * <p>Description: A thread that can switch into un-interruptible state</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.concurrency.ControlledInterruptThread</code></p>
 */

public class ControlledInterruptThread extends Thread {

	/**
	 * Creates a new ControlledInterruptThread
	 */
	public ControlledInterruptThread() {

	}

	/**
	 * Creates a new ControlledInterruptThread
	 * @param target The runnable this thread will run
	 */
	public ControlledInterruptThread(final Runnable target) {
		super(target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new ControlledInterruptThread
	 * @param name The name for this thread
	 */
	public ControlledInterruptThread(final String name) {
		super(name);
	}

	/**
	 * Creates a new ControlledInterruptThread
	 * @param group	The thread group this thread will belong to
	 * @param target The runnable this thread will run
	 */
	public ControlledInterruptThread(final ThreadGroup group, final Runnable target) {
		super(group, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new ControlledInterruptThread
	 * @param group The thread group this thread will belong to
	 * @param name The name for this thread
	 */
	public ControlledInterruptThread(final ThreadGroup group, final String name) {
		super(group, name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new ControlledInterruptThread
	 * @param target The runnable this thread will run
	 * @param name The name for this thread
	 */
	public ControlledInterruptThread(final Runnable target, final String name) {
		super(target, name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new ControlledInterruptThread
	 * @param group
	 * @param target
	 * @param name
	 */
	public ControlledInterruptThread(ThreadGroup group, Runnable target, String name) {
		super(group, target, name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new ControlledInterruptThread
	 * @param group
	 * @param target
	 * @param name
	 * @param stackSize
	 */
	public ControlledInterruptThread(ThreadGroup group, Runnable target, String name, long stackSize) {
		super(group, target, name, stackSize);
		// TODO Auto-generated constructor stub
	}

}
