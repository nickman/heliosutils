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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.heliosapm.utils.enums.BitMasked;
import com.heliosapm.utils.unsafe.UnsafeAdapter;

/**
 * <p>Title: TimedDecayEventTrigger</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.TimedDecayEventTrigger</code></p>
 */

public class TimedDecayEventTrigger<E extends Enum<E> & BitMasked> implements Trigger<E, E>, Runnable {
	
	/** The shared event scheduler */
	protected final EventScheduler scheduler;
	/** The decay period */
	protected final long period;
	/** The decay period unit */
	protected final TimeUnit unit;
	/** The decayed state */
	protected final E stateToSet;
	/** The current state */
	protected final AtomicReference<E> state = new AtomicReference<E>();
	/** The decay timer */
	protected volatile ScheduledFuture<?> handle = null;
	/** The started indicator */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	/** The next trigger */
	protected Trigger<?, E> nextTrigger = null;
	
	/** Spin lock around the counter */
	protected final UnsafeAdapter.SpinLock lock = UnsafeAdapter.allocateSpinLock();
	/** The pipeline context */
	protected PipelineContext context = null;
	
	public TimedDecayEventTrigger(final long period, final TimeUnit unit,  final E stateToSet, final boolean autoStart) {
		scheduler =DefaultEventScheduler.getInstance();
		this.period = period;
		this.unit = unit;
		this.stateToSet = stateToSet;
		state.set(stateToSet);
		if(autoStart) start();
	}

	@Override
	public Class<E> getInputType() {		
		return stateToSet.getDeclaringClass();
	}
	
	@Override
	public Class<E> getReturnType() {
		return stateToSet.getDeclaringClass();
	}
	
	@Override
	public void setPipelineContext(final PipelineContext context) {
		this.context = context;		
	}
	

	@Override
	public E in(final E event) {
		if(event==null || !started.get()) return null;
		state.set(event);
		reset();
		return event;
	}
	
	public void run() {
		state.set(stateToSet);
		reset();
	}

	@Override
	public void reset() {
		if(handle!=null) {
			handle.cancel(false);
		}
		handle = scheduler.schedule(this, period, unit);		
	}

	@Override
	public void windDown(final E event) {
		/* No Op */
	}
	
	@Override
	public Trigger<?, E> nextTrigger() {
		return nextTrigger;
	}
	
	@Override
	public void setNextTrigger(final Trigger<?, E> nextTrigger) {
		this.nextTrigger = nextTrigger;	
	}
	
	@Override
	public void out(final E result) {
		if(nextTrigger!=null) nextTrigger.in(result);		
	}
	
	@Override
	public boolean isStarted() {
		return started.get();
	}
	
	public void start() {
		if(started.compareAndSet(false, true)) {
			reset();
		}
	}
	
	public void stop() {
		if(started.compareAndSet(true, false)) {
			if(handle!=null) {
				handle.cancel(false);
			}			
		}
	}

	
	

}
