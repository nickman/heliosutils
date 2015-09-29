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
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.json.JSONObject;

import com.heliosapm.utils.enums.BitMasked;
import com.heliosapm.utils.jmx.notifcations.DelegateNotificationBroadcaster;

/**
 * <p>Title: EventSink</p>
 * <p>Description: Defines an event sink which sits at the end of a trigger pipeline</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.EventSink</code></p>
 * @param <E> Th event type
 */

public class EventSink<E extends Enum<E> & BitMasked> implements Sink<E>, DelegateNotificationBroadcaster {
	/** The event type tracked by this sink */
	protected final Class<E> eventType;
	/** The current state of this sink */
	protected final AtomicReference<E> state = new AtomicReference<E>(null);
	/** The prior state of this sink */
	protected final AtomicReference<E> priorState = new AtomicReference<E>(null);
	
	/** The effective timestamp of the current state */
	protected final AtomicLong timestamp = new AtomicLong(0L);
	/** The initial state when created and when resuming from Off */
	protected final E initialState;
	/** The state set to when sink is turned off */
	protected final E offState;
	/** The started indicator */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	/** The pipeline context of the context that this trigger is installed into */
	protected PipelineContext context = null;
	/** The events that are allowed to repeat, meaning they will send a broadcast every time a matching event is received */
	protected final EnumMap<E, int[]> repeatingEvents;
	/** The parent notification broadcaster, injected by the pipeline */
	protected NotificationBroadcasterSupport parentBroadcaster = null;
	/** The sink's MBean notification descriptors */
	protected final MBeanNotificationInfo[] infos;
	/** The ObjectName of the parent we're delegating to */
	protected ObjectName objectName = null;
	/** The notification sequence factory of the parent we're delegating to */
	protected AtomicLong notifSerial = null;
	
	/** The pipeline assigned trigger id */
	protected int pipelineId = -1;
	/** The JMX notification type prefix */
	public static final String NOTIF_PREFIX = "event.sink.statuschange";
	/** The JMX repeating notification type prefix */
	public static final String NOTIF_REPEATING_PREFIX = "event.sink.repeating";
	
	
	/**
	 * Creates a new EventSink from the passed JSON definition
	 * @param jsonConfig The json trigger definition
	 * @return the EventSink
	 */	
	@SuppressWarnings("unchecked")
	public static <E extends Enum<E> & BitMasked> EventSink<E> fromJSON(final JSONObject jsonConfig) {
		if(jsonConfig==null) throw new IllegalArgumentException("The passed JSON was null");
		final Class<E> eventType;
		final E initialState;
		final E offState;
		final String eventTypeName = jsonConfig.optString("eventType");
		if(eventTypeName==null) throw new IllegalArgumentException("The passed JSON did not contain an eventType");
		final String initialStateName = jsonConfig.optString("initialState");
		final String offStateName = jsonConfig.optString("offState");
		if(initialStateName==null) throw new IllegalArgumentException("The passed JSON did not contain an initial state");
		if(offStateName==null) throw new IllegalArgumentException("The passed JSON did not contain an off state");
		try {
			eventType = (Class<E>)Class.forName(eventTypeName.trim(), true, Thread.currentThread().getContextClassLoader());
			initialState = Enum.valueOf(eventType, initialStateName.trim().toUpperCase());
			offState = Enum.valueOf(eventType, offStateName.trim().toUpperCase());
			return new EventSink<E>(eventType, initialState, offState);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create EventSink", ex);
		}
	}
	
	/**
	 * Creates a new EventSink
	 * @param eventType The event type
	 * @param initialState The sink's initial state
	 * @param offState The sink's off state
	 * @param repeatingEvents An array of event types that support repeating broadcasts on a matching in
	 */
	public EventSink(final Class<E> eventType, final E initialState, final E offState, final E...repeatingEvents) {		
		this.eventType = eventType;
		this.initialState = initialState;
		this.offState = offState;
		this.repeatingEvents = new EnumMap<E, int[]>(eventType); 
		for(E rep: repeatingEvents) {
			this.repeatingEvents.put(rep, new int[1]);
		}
		infos = buildInfos(this.eventType, this.repeatingEvents.keySet());
		state.set(initialState);
		timestamp.set(System.currentTimeMillis());
	}
	
	private static <E extends Enum<E> & BitMasked> MBeanNotificationInfo[] buildInfos(final Class<E> eventType, final Set<E> repeating) {
		final E[] eventTypes = eventType.getEnumConstants();
		final MBeanNotificationInfo[] infos = new MBeanNotificationInfo[eventTypes.length + 1 + repeating.size()];
		infos[0] = new MBeanNotificationInfo(new String[]{NOTIF_PREFIX}, Notification.class.getName(), "Broadcasts any state change for event types of " + eventType.getName());
		int i = 1;
		for(; i < eventTypes.length; i++) {
			final E eType = eventTypes[i-1];
			infos[i] = new MBeanNotificationInfo(new String[]{NOTIF_PREFIX + "." + eType.name()}, Notification.class.getName(), "Broadcasts state changes to " + eType.name());
		}		
		i++;
		for(E rep : repeating) {			
			infos[i] = new MBeanNotificationInfo(new String[]{NOTIF_REPEATING_PREFIX + "." + rep.name()}, Notification.class.getName(), "Repeating status notification " + rep.name());
			i++;
		}
		return infos;
	}
	
	@Override
	public void onUpstreamStateChange(final E state) {
		/* I'm a sink. No one is upstream from me */
		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#in(java.lang.Object)
	 */
	@Override
	public Void in(final E event) {
		if(event!=null) {
			final E prior = state.getAndSet(event);
			priorState.set(prior);
			if(prior!=event) {
				if(repeatingEvents.containsKey(prior)) {
					resetRepeatCounts();
				}
				timestamp.set(System.currentTimeMillis());
				context.onStateChange(this, event);
				sendStateChangeNotification(prior, event);
			} else if(repeatingEvents.containsKey(event)) {
				timestamp.set(System.currentTimeMillis());
				sendRepeatingStateNotification(event, getRepeatCount(event));				
			}
			
			context.eventSunk(pipelineId, event);
		}		
		return null;
	}
	
	/**
	 * Returns the repeat count for the passed event, not including the first.
	 * Resets all other event's repeat count to zero
	 * @param repeating The repeating event
	 * @return the number of repeats (1 if this is the first repeat)
	 */
	protected int getRepeatCount(final E repeating) {
		final int[] count = repeatingEvents.get(repeating);
		count[0]++;
		for(E nonrep : repeatingEvents.keySet()) {
			if(nonrep!=repeating) {
				repeatingEvents.get(nonrep)[0] = 0;
			}
		}
		return count[0];
	}
	
	/**
	 * Resets all repeating counts
	 */
	protected void resetRepeatCounts() {
		for(E nonrep : repeatingEvents.keySet()) {
			repeatingEvents.get(nonrep)[0] = 0;
		}
	}
	
	/**
	 * Sends state change notifications when the state of the sink changes
	 * @param from The prior state
	 * @param to the current state
	 */
	protected void sendStateChangeNotification(final E from, final E to) {
		final long t = timestamp.get();
		final String msg = "State change from [" + from + "] to [" + to + "]";
		final JSONObject eventChange = new JSONObject();
		final JSONObject body = new JSONObject();
		eventChange.put("to", to.name());
		eventChange.put("from", from.name());
		eventChange.put("time", t);
		body.put("statechange", eventChange);
		final String userMsg = body.toString();
		Notification n = new Notification(NOTIF_PREFIX, objectName, notifSerial.incrementAndGet(), t, msg);
		n.setUserData(userMsg);
		this.parentBroadcaster.sendNotification(n);
		n = new Notification(NOTIF_PREFIX + "." + to.name(), objectName, notifSerial.incrementAndGet(), t, msg);
		n.setUserData(userMsg);
		this.parentBroadcaster.sendNotification(n);
	}
	
	/**
	 * Sends a repeating event notification
	 * @param event The repeated event
	 * @param repeatCount The number of times the event has been repeated (not including the first)
	 */
	protected void sendRepeatingStateNotification(final E event, final int repeatCount) {
		final long t = System.currentTimeMillis();
		final String msg = "Repeating state change for [" + event + "]:" + repeatCount;
		final JSONObject repeat = new JSONObject();
		final JSONObject body = new JSONObject();
		repeat.put("event", event.name());
		repeat.put("time", System.currentTimeMillis());
		body.put("repeatstate", repeat);
		final String userMsg = body.toString();
		Notification n = new Notification(NOTIF_REPEATING_PREFIX + "." + event.name(), objectName, notifSerial.incrementAndGet(), t, msg);
		n.setUserData(userMsg);
		this.parentBroadcaster.sendNotification(n);		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#out(java.lang.Object)
	 */
	@Override
	public void out(Void result) {
		throw new UnsupportedOperationException("EventSinks do not support out events");
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#reset()
	 */
	@Override
	public void reset() {
		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#windDown(java.lang.Object)
	 */
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
			in(initialState);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#stop()
	 */
	@Override
	public void stop() {
		if(started.compareAndSet(true, false)) {
			in(offState);
		}				
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#setNextTrigger(com.heliosapm.utils.events.Trigger)
	 */
	@Override
	public void setNextTrigger(final Trigger<?, Void> nextTrigger) {
		throw new UnsupportedOperationException("EventSinks do not support next triggers");		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#nextTrigger()
	 */
	@Override
	public Trigger<?, Void> nextTrigger() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#getReturnType()
	 */
	@Override
	public Class<Void> getReturnType() {
		return Void.class;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#getInputType()
	 */
	@Override
	public Class<E> getInputType() {
		return eventType;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#setPipelineContext(com.heliosapm.utils.events.PipelineContext, int)
	 */
	@Override
	public void setPipelineContext(final PipelineContext context, final int pipelineId) {
		this.context = context;		
		this.pipelineId = pipelineId;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws IllegalArgumentException {
		this.parentBroadcaster.addNotificationListener(listener, filter, handback);		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
		this.parentBroadcaster.removeNotificationListener(listener);		
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		return infos;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.notifcations.DelegateNotificationBroadcaster#setNotificationBroadcaster(javax.management.NotificationBroadcaster, javax.management.ObjectName, java.util.concurrent.atomic.AtomicLong)
	 */
	@Override
	public void setNotificationBroadcaster(final NotificationBroadcasterSupport notificationBroadcaster, final ObjectName objectName, final AtomicLong notifSerial) {
		this.parentBroadcaster = notificationBroadcaster;		
		this.objectName = objectName;
		this.notifSerial = notifSerial;
		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Sink#getState()
	 */
	@Override
	public E getState() {
		return state.get();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Sink#getPriorState()
	 */
	@Override
	public E getPriorState() {
		return priorState.get();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Sink#getLastStatusChange()
	 */
	@Override
	public long getLastStatusChange() {
		return this.timestamp.get();
	}

}
