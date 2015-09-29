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

import org.json.JSONObject;

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
	protected final E decayedState;
	/** The initial state */
	protected final E initialState;
	/** The pipeline sink */
	protected Trigger<Void, E> sink = null;
	/** The current state */
	protected final AtomicReference<E> state = new AtomicReference<E>();
	/** The decay timer */
	protected volatile ScheduledFuture<?> handle = null;
	/** The started indicator */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	/** The next trigger */
	protected Trigger<?, E> nextTrigger = null;
	
	/** The type of the event to be forwarded */
	protected final Class<E> eventType;
	
	/** The pipeline assigned trigger id */
	protected int pipelineId = -1;
	
	/** Spin lock around the counter */
	protected final UnsafeAdapter.SpinLock lock = UnsafeAdapter.allocateSpinLock();
	/** The pipeline context */
	protected PipelineContext context = null;
	
	
	/**
	 * Creates a new TimedDecayEventTrigger from the passed JSON definition
	 * @param jsonDef The json trigger definition
	 * @return the TimedDecayEventTrigger
	 */	
	@SuppressWarnings("unchecked")
	public static <E extends Enum<E> & BitMasked> TimedDecayEventTrigger<E> fromJSON(final JSONObject jsonConfig) {
		if(jsonConfig==null) throw new IllegalArgumentException("The passed JSON was null");
		final Class<E> eventType;
		final E decayedState;
		final E initialState;
		final long period;
		final TimeUnit unit;
		
		final String eventTypeName = jsonConfig.optString("eventType");
		if(eventTypeName==null) throw new IllegalArgumentException("The passed JSON did not contain an eventType");
		final String decayStateName = jsonConfig.optString("decayState");
		if(decayStateName==null) throw new IllegalArgumentException("The passed JSON did not contain a decay state");
		final String initialStateName = jsonConfig.optString("decayState");
		
		
		final String stateToSetName = jsonConfig.optString("decayState");
		if(stateToSetName==null) throw new IllegalArgumentException("The passed JSON did not contain a decay state");
		
		period = jsonConfig.optLong("decayPeriod", 15);		
		String unitName = jsonConfig.optString("decayUnit");
		if(unitName==null) unitName = TimeUnit.SECONDS.name();
		try {
			
			eventType = (Class<E>)Class.forName(eventTypeName.trim(), true, Thread.currentThread().getContextClassLoader());
			decayedState = Enum.valueOf(eventType, stateToSetName.trim().toUpperCase());
			initialState = initialStateName==null ? null : Enum.valueOf(eventType, initialStateName.trim().toUpperCase());
			unit = TimeUnit.valueOf(unitName.trim().toUpperCase());
			return new TimedDecayEventTrigger<E>(eventType, period, unit, initialState, decayedState);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create TimedDecayEventTrigger", ex);
		}
	}
	
	
	/**
	 * Creates a new TimedDecayEventTrigger
	 * @param eventType The type of the event being decayed
	 * @param period The decay period, i.e. if no events are received within this period of time, the state will change to {@code stateToSet}
	 * @param unit The unit of the period
	 * @param initialState The initial state of the trigger
	 * @param decayedState The state to set on a decay event
	 */
	public TimedDecayEventTrigger(final Class<E> eventType, final long period, final TimeUnit unit,  final E initialState, final E decayedState) {
		this.eventType = eventType;
		scheduler = DefaultEventScheduler.getInstance();
		this.period = period;
		this.unit = unit;
		this.decayedState = decayedState;
		this.initialState = initialState;
		state.set(this.initialState);		
	}

	@Override
	public Class<E> getInputType() {		
		return eventType;
	}
	
	@Override
	public Class<E> getReturnType() {
		return eventType;
	}
	
	@Override
	public void setPipelineContext(final PipelineContext context, final int pipelineId) {
		this.context = context;		
		this.pipelineId = pipelineId;
		this.sink = context.getSink();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#onUpstreamStateChange(java.lang.Object)
	 */
	@Override
	public void onUpstreamStateChange(final E state) {
		/* No Op */
	}
	
	/**
	 * <p>ALWAYS forwards</p>
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#in(java.lang.Object)
	 */
	@Override
	public E in(final E event) {
		if(event==null || !started.get()) return null;
		if(event!=decayedState) {
			reset();
		}
		out(event);
		return event;
	}
	
	public void run() {
		final E prior = state.get();
		if(prior!=decayedState) {
			sink.in(decayedState);
		} else {
			reset();
		}
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
