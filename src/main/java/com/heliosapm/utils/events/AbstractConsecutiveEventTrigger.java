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

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import com.heliosapm.utils.enums.BitMasked;
import com.heliosapm.utils.enums.Rollup;
import com.heliosapm.utils.unsafe.UnsafeAdapter;

/**
 * <p>Title: AbstractConsecutiveEventTrigger</p>
 * <p>Description: A trigger that fires on the receipt of a specified number of consecutive events</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.AbstractConsecutiveEventTrigger</code></p>
 * @param <E> The bit masked event type
 */

public abstract class AbstractConsecutiveEventTrigger<E extends Enum<E> & BitMasked> implements Trigger<E, E> {
	/** The event type class */
	final Class<E> eventType;
	/** The alarmable event counters for each triggering event */
	protected final EnumMap<E, int[]> counters;
	/** The accepted event types */
	protected final E[] accepted;
	/** The threshold number of consecutive events for each type that fires the trigger */
	protected final EnumMap<E, int[]> thresholds;
	/** The rollup type */
	protected final Rollup rollup;
	/** The bit mask of the event types that increment counters */
	protected final int alertMask;
	/** The bit mask of the event types that reset counters */
	protected final int resetMask;	
	/** The bit mask of accepted event types */
	protected final int acceptMask;	
	
	/** The pipeline provided id for this trigger */
	protected int pipelineId = -1;
	
	/** The started indicator */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	
	/** The cached rollup sets */
	protected final EnumMap<E, E[]> rollupSets;
	/** The next trigger */
	protected Trigger<?, E> nextTrigger = null;
	/** Spin lock around the counter */
	protected final UnsafeAdapter.SpinLock lock = UnsafeAdapter.allocateSpinLock();
	
	/** The pipeline context of the context that this trigger is installed into */
	protected PipelineContext context = null;
	/** The pipeline thread pool for async triggers */
	protected ExecutorService executor = null;
	
	/**
	 * Creates a new AbstractConsecutiveEventTrigger
	 * @param eventType The event type class
	 * @param thresholds A map of the triggering consecutive thresholds for each triggering event type
	 * @param rollup Indicates if the event type rolls up, down or is absolute
	 * @param acceptedEvents The events accepted by this trigger. If length is zero, will assume all event types
	 */
	public AbstractConsecutiveEventTrigger(final Class<E> eventType, final EnumMap<E, Integer> thresholds, final Rollup rollup, final E...acceptedEvents) {
		if(eventType==null) throw new IllegalArgumentException("The passed event type was null");
		if(thresholds==null || thresholds.isEmpty()) throw new IllegalArgumentException("The passed threshold map was null or empty");
		if(rollup==null) throw new IllegalArgumentException("The passed rollup was null");		
		this.eventType = eventType;
		accepted = acceptedEvents.length==0 ? eventType.getEnumConstants() : acceptedEvents;
		acceptMask = BitMasked.StaticOps.maskFor(accepted);
		Arrays.sort(accepted);		
		this.thresholds = new EnumMap<E, int[]>(eventType);
		for(Map.Entry<E, Integer> entry: thresholds.entrySet()) {
			this.thresholds.put(entry.getKey(), new int[]{entry.getValue()});
		}
		counters = new EnumMap<E, int[]>(eventType);
		for(Map.Entry<E, Integer> entry: thresholds.entrySet()) {
			this.counters.put(entry.getKey(), new int[]{entry.getValue()});
		}
		this.rollup = rollup;
		this.alertMask = BitMasked.StaticOps.maskFor(this.thresholds.keySet());
		// pre-calculate and cache the rollup sets
		rollupSets = new EnumMap<E, E[]>(eventType);
		final E[] triggeringEvents = BitMasked.StaticOps.toArray(eventType, this.thresholds.keySet());
		for(E e: this.thresholds.keySet()) {
			rollupSets.put(e, BitMasked.StaticOps.roll(eventType, rollup, e, triggeringEvents));
		}
		// there must be at least one accepted event type that is *not* in the threshold, i.e. to reset the counters
		int x = 0;
		for(E e : accepted) {
			if(!e.isEnabled(alertMask)) {
				x= e.enableFor(x);
			}
		}
		if(x==0) throw new IllegalArgumentException("No resetting event types. thesholds:" + this.thresholds.keySet() + ", accepted:" + Arrays.toString(accepted));
		resetMask = x;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#setPipelineContext(com.heliosapm.utils.events.PipelineContext, int)
	 */
	@Override
	public void setPipelineContext(final PipelineContext context, final int pipelineId) {
		this.context = context;
		this.pipelineId = pipelineId;
		executor = this.context.getPipelineExecutor();
	}
	
	@Override
	public Class<E> getInputType() {		
		return eventType;
	}
	
	@Override
	public Class<E> getReturnType() {
		return eventType;
	}
	
	protected boolean isAccepted(final E event) {
		return event.isEnabled(acceptMask);
	}
	
	protected boolean isResetting(final E event) {
		return event.isEnabled(resetMask);
	}

	protected boolean isTriggering(final E event) {
		return event.isEnabled(alertMask);
	}
	
	@Override
	public void out(final E result) {
		if(nextTrigger != null) {
			nextTrigger.in(result);
		}		
	}
	
	@Override
	public Trigger<?, E> nextTrigger() {
		return nextTrigger;
	}
	
	@Override
	public void setNextTrigger(final Trigger<?, E> nextTrigger) {
		this.nextTrigger = nextTrigger;		
	}
	
	/**
	 * Returns the most severe (highest for rollup, lowest for rolldown) event type that has hit it's defined threshold
	 * @param justIncremented The array of event types incremented in the last event
	 * @return the most severe event type or null no event type triggered
	 */
	protected E mostSevereTriggered(final E[] justIncremented) {
		final TreeSet<E> triggered = new TreeSet<E>();
		for(E e: justIncremented) {
			if(counters.get(e)[0]==thresholds.get(e)[0]) {
				triggered.add(e);
			}
		}
		if(triggered.isEmpty()) return null;
		return rollup==Rollup.UP ? triggered.first() : triggered.last();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#in(java.lang.Enum)
	 */
	@Override
	public E in(final E state) {
		if(state==null || !started.get()) return null;
		if(!isAccepted(state)) return null;
		if(isResetting(state)) {
			try {
				lock.xlock(true);
				reset();
				return null;
			} finally {
				lock.xunlock();
				context.eventSunk(pipelineId);
			}
		} else if(!isTriggering(state)) {
			// ????  Should not happen
			return null;
		}
		try {
			final E[] toIncrement = rollupSets.get(state);
			if(toIncrement==null || toIncrement.length==0) throw new IllegalStateException("Rollup Set for [" + state + "] was null or empty");				
			lock.xlock();
			for(E e: toIncrement) {
				final int[] counter = counters.get(e);
				counter[0]++;					
			}
			final E mostSevere = mostSevereTriggered(toIncrement);			
			if(mostSevere!=null) {
				windDown(mostSevere);		
				out(mostSevere);
			}			
			return mostSevere;
		} finally {
			lock.xunlock();
			context.eventSunk(pipelineId);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#reset()
	 */
	@Override
	public void reset() {
		final boolean lockedByMe = lock.isLockedByMe();
		try {
			if(!lockedByMe) lock.xlock();
			for(int[] ctr: counters.values()) {
				ctr[0] = 0;
			}
		} finally {
			if(!lockedByMe) lock.xunlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#isStarted()
	 */
	@Override
	public boolean isStarted() {		
		return started.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#start()
	 */
	@Override
	public void start() {		
		if(started.compareAndSet(false, true)) {
			
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#stop()
	 */
	@Override
	public void stop() {
		if(started.compareAndSet(true, false)) {
			
		}				
	}
	

}
