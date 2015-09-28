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
 */

public class EventSink<E extends Enum<E> & BitMasked> implements Trigger<Void, E>, DelegateNotificationBroadcaster {
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
	
	
	/**
	 * Creates a new EventSink from the passed JSON definition
	 * @param jsonDef The json trigger definition
	 * @return the EventSink
	 */	
	@SuppressWarnings("unchecked")
	public static <E extends Enum<E> & BitMasked> EventSink<E> fromJSON(final JSONObject jsonConfig) {
		if(jsonConfig==null) throw new IllegalArgumentException("The passed JSON was null");
		final Class<E> eventType;
		final E initialState;
		final String eventTypeName = jsonConfig.optString("eventType");
		if(eventTypeName==null) throw new IllegalArgumentException("The passed JSON did not contain an eventType");
		final String initialStateName = jsonConfig.optString("initialState");
		if(initialStateName==null) throw new IllegalArgumentException("The passed JSON did not contain an initial state");
		try {
			eventType = (Class<E>)Class.forName(eventTypeName.trim(), true, Thread.currentThread().getContextClassLoader());
			initialState = Enum.valueOf(eventType, initialStateName.trim().toUpperCase());
			return new EventSink<E>(eventType, initialState);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create EventSink", ex);
		}
	}
	
	/**
	 * Creates a new EventSink
	 */
	public EventSink(final Class<E> eventType, final E initialState) {		
		this.eventType = eventType;
		infos = buildInfos(this.eventType);
		state.set(initialState);
		timestamp.set(System.currentTimeMillis());
	}
	
	private static <E extends Enum<E> & BitMasked> MBeanNotificationInfo[] buildInfos(final Class<E> eventType) {
		final E[] eventTypes = eventType.getEnumConstants();
		final MBeanNotificationInfo[] infos = new MBeanNotificationInfo[eventTypes.length + 1];
		infos[0] = new MBeanNotificationInfo(new String[]{NOTIF_PREFIX}, Notification.class.getName(), "Broadcasts any state change for event types of " + eventType.getName());
		for(int i = 1; i < infos.length; i++) {
			final E eType = eventTypes[i-1];
			infos[i] = new MBeanNotificationInfo(new String[]{NOTIF_PREFIX + "." + eType.name()}, Notification.class.getName(), "Broadcasts state changes to " + eType.name());
		}		
		return infos;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#in(java.lang.Object)
	 */
	@Override
	public Void in(final E event) {
		if(event!=null) {
			final E prior = state.getAndSet(event);
			if(prior!=event) {
				timestamp.set(System.currentTimeMillis());
				sendStateChangeNotification(prior, event);
			}
			context.eventSunk(pipelineId);
		}		
		return null;
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

}
