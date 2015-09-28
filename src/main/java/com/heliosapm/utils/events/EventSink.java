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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;

import com.heliosapm.utils.enums.BitMasked;
import com.heliosapm.utils.jmx.SharedNotificationExecutor;

/**
 * <p>Title: EventSink</p>
 * <p>Description: Defines an event sink which sits at the end of a trigger pipeline</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.EventSink</code></p>
 */

public class EventSink<E extends Enum<E> & BitMasked> extends NotificationBroadcasterSupport implements Trigger<Void, E> {
	/** The event type tracked by this sink */
	protected final Class<E> eventType;
	/** The current state of this sink */
	protected final AtomicReference<E> state = new AtomicReference<E>(null);
	/** The effective timestamp of the current state */
	protected final AtomicLong timestamp = new AtomicLong();
	/** The started indicator */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	/** The pipeline context of the context that this trigger is installed into */
	protected PipelineContext context = null;

	/**
	 * Creates a new EventSink
	 */
	public EventSink(final Class<E> eventType, final E initialState) {
		super(SharedNotificationExecutor.getInstance(), buildInfos(eventType));
		this.eventType = eventType;
		state.set(initialState);
		timestamp.set(System.currentTimeMillis());
	}
	
	private static <E extends Enum<E> & BitMasked> MBeanNotificationInfo[] buildInfos(final Class<E> eventType) {
		final E[] eventTypes = eventType.getEnumConstants();
		final MBeanNotificationInfo[] infos = new MBeanNotificationInfo[eventTypes.length + 1];
		
		return infos;
	}

	@Override
	public Void in(final E event) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void out(Void result) {
		throw new UnsupportedOperationException("EventSinks do not support out events");
	}

	@Override
	public void reset() {
		
	}

	@Override
	public void windDown(final E event) {
		
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

	@Override
	public void setNextTrigger(final Trigger<?, Void> nextTrigger) {
		throw new UnsupportedOperationException("EventSinks do not support next triggers");		
	}

	@Override
	public Trigger<?, Void> nextTrigger() {
		return null;
	}

	@Override
	public Class<Void> getReturnType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<E> getInputType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPipelineContext(PipelineContext context) {
		// TODO Auto-generated method stub
		
	}

}
