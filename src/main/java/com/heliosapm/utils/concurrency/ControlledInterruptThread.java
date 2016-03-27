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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Title: ControlledInterruptThread</p>
 * <p>Description: A thread that can switch into un-interruptible state</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.concurrency.ControlledInterruptThread</code></p>
 */

public class ControlledInterruptThread extends Thread {
	/** The interruptible state */
	protected final AtomicBoolean interruptible = new AtomicBoolean(true);
	/** Indicates if there was an attempt to interrupt this thread since it went into un-interruptible state */
	protected final AtomicBoolean interrupted = new AtomicBoolean(true);

	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread#toString()
	 */
	@Override
	public String toString() {
		return super.toString() + "[interruptible:" +  interruptible.get() + "]";
	}
	
	/**
	 * Sets the interruptible state
	 * @param inter true for interruptible, false otherwise 
	 * @return this thread
	 */
	public ControlledInterruptThread setInterruptible(final boolean inter) {
		if(!inter && interruptible.compareAndSet(true, false)) {
			interrupted.set(false);
		}
		return this;
	}
	
	/**
	 * Indicates if any attempts to interrupt this thread were made while it was interruptible,
	 * and resets the interrupted state
	 * @return true if interrupted, false otherwise
	 */
	public boolean wasInterrupted() {
		return interrupted.getAndSet(false);
	}
	
	/**
	 * Indicates if this thread is interruptible
	 * @return true if this thread is interruptible, false otherwise
	 */
	public boolean isInterruptible() {
		return interruptible.get();
	}
	
	@Override
	public void interrupt() {
		if(interruptible.get()) {
			super.interrupt();
		} else {
			interrupted.set(true);
		}
	}
	
	public ControlledInterruptThread runInterruptibly(final Runnable r) {
		try {
			setInterruptible(false);
			r.run();
		} finally {
			setInterruptible(true);
		}
		return this;
	}
	
	public <T> T callInterruptibly(final Callable<T> callable) throws Exception {
		try {
			setInterruptible(false);
			return callable.call();
		} finally {
			setInterruptible(true);
		}		
	}
	
	
	
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
	}

	/**
	 * Creates a new ControlledInterruptThread
	 * @param group The thread group this thread will belong to
	 * @param name The name for this thread
	 */
	public ControlledInterruptThread(final ThreadGroup group, final String name) {
		super(group, name);
	}

	/**
	 * Creates a new ControlledInterruptThread
	 * @param target The runnable this thread will run
	 * @param name The name for this thread
	 */
	public ControlledInterruptThread(final Runnable target, final String name) {
		super(target, name);
	}

	/**
	 * Creates a new ControlledInterruptThread
	 * @param group The thread group this thread will belong to
	 * @param target The runnable this thread will run
	 * @param name The name for this thread
	 */
	public ControlledInterruptThread(final ThreadGroup group, final Runnable target, final String name) {
		super(group, target, name);
	}

	/**
	 * Creates a new ControlledInterruptThread
	 * @param group The thread group this thread will belong to
	 * @param target The runnable this thread will run
	 * @param name The name for this thread
	 * @param stackSize The thread's stack size in bytes
	 */
	public ControlledInterruptThread(final ThreadGroup group, final Runnable target, final String name, final long stackSize) {
		super(group, target, name, stackSize);
	}

}
