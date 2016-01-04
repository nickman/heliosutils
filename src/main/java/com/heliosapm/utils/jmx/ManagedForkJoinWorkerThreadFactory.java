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

import jsr166y.ForkJoinPool;
import jsr166y.ForkJoinPool.ForkJoinWorkerThreadFactory;
import jsr166y.ForkJoinTask;
import jsr166y.ForkJoinWorkerThread;

import java.util.concurrent.atomic.AtomicInteger;

import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

/**
 * <p>Title: ManagedForkJoinWorkerThreadFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.jmx.ManagedForkJoinWorkerThreadFactory</code></p>
 */

public class ManagedForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
	/** The thread name prefix */
	protected final String name;
	
	/** The priority of created threads */
	protected final int priority;
	/** The daemon attr of created threads */
	protected final boolean daemon;
	
	/** The live factory threads keyed by id */
	protected final NonBlockingHashMapLong<ManagedForkJoinWorkerThread> threads = new NonBlockingHashMapLong<ManagedForkJoinWorkerThread>(); 
	
	/** The serial number generator for created threads */
	protected AtomicInteger serial = new AtomicInteger();

	/**
	 * Creates a new ManagedForkJoinWorkerThreadFactory
	 * @param name The thread name prefix
	 */
	public ManagedForkJoinWorkerThreadFactory(final String name, final int priority, final boolean daemon) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		this.name = name.trim();
		this.priority = priority;
		this.daemon = daemon;
	}


	
	@Override
	public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
		return new ManagedForkJoinWorkerThread(pool, name, serial.incrementAndGet(), priority, daemon);		
	}
	
	public class ManagedForkJoinWorkerThread extends ForkJoinWorkerThread {
		protected final int id;
		protected ManagedForkJoinWorkerThread(final ForkJoinPool pool, final String name, final int id, final int priority, final boolean daemon) {
			super(pool);
			setName(name + "#" + id);
			setDaemon(true);
			setPriority(priority);
			this.id = id;
			
		}
		
		@Override
		public int getPoolIndex() {			
			return id;
		}
		
		@Override
		protected void onStart() {			
			threads.put(this.id, this);
			super.onStart();			
		}
		
		@Override
		protected void onTermination(final Throwable exception) {
			threads.remove(this.id);
			super.onTermination(exception);
		}
		
	}

}
