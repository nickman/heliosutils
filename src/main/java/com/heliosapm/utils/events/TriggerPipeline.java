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

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.jmx.SharedNotificationExecutor;
import com.heliosapm.utils.jmx.notifcations.DelegateNotificationBroadcaster;

/**
 * <p>Title: TriggerPipeline</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.TriggerPipeline</code></p>
 * @param <E> The pipeline entry type
 * @param <R> The pipeline sink type
 */

public class TriggerPipeline<R, E> implements PipelineContext, NotificationBroadcaster, TriggerPipelineMBean {
	
	/** The list of triggers in the pipeline */
	protected final LinkedList<Trigger<R,E>> pipeline;
	/** The parent notification broadcaster */
	protected final NotificationBroadcasterSupport notificationBroadcaster;
	
	/** The starter trigger */
	protected final Trigger<?, E> starter;
	/** The sink trigger */
	protected final Trigger<Void, R> sink;
	/** The JMX object name for this pipeline */
	protected final ObjectName objectName;
	/** The JMX notification serial number factory */
	protected final AtomicLong notifSerial = new AtomicLong(0L);
	/** The executor service for async triggers */
	protected final ExecutorService pipelineExecutor;
	/** Flag to indicate if this pipeline is started */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	/** The sink state source */
	protected Sink<?> stateSink;
	/** Mini graphic displaying the flow of the pipeline */
	protected final String flow;
	/** The last advisory message */
	protected final AtomicReference<String> advisory = new AtomicReference<String>("None");
	
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass().getName());
	/** The JMX notification type prefix */
	public static final String NOTIF_PREFIX = "event.sink";
	/** The JMX notification type for pipeline started */
	public static final String NOTIF_STARTED = NOTIF_PREFIX + ".started";
	/** The JMX notification type for pipeline stopped */
	public static final String NOTIF_STOPPED = NOTIF_PREFIX + ".stopped";
	/** The JMX notification type for a pipeline advisory message */
	public static final String NOTIF_ADVISORY = NOTIF_PREFIX + ".advisory";
	
	
	private static final MBeanNotificationInfo[] NOTIF_INFOS = new MBeanNotificationInfo[]{
		new MBeanNotificationInfo(new String[]{NOTIF_STARTED}, Notification.class.getName(), "Notification issued when the pipeline starts"),
		new MBeanNotificationInfo(new String[]{NOTIF_STOPPED}, Notification.class.getName(), "Notification issued when the pipeline stops"),
		new MBeanNotificationInfo(new String[]{NOTIF_ADVISORY}, Notification.class.getName(), "Notification issued for a pipeline advisory")
	};
	
	/**
	 * Creates a new TriggerPipeline
	 * @param pipeline The list of triggers in the pipeline
	 */
	TriggerPipeline(final ObjectName objectName, final LinkedList<Trigger<R,E>> pipeline, final ExecutorService pipelineExecutor) {
		this.pipeline = pipeline;
		this.objectName = objectName;
		this.pipelineExecutor = pipelineExecutor;
		starter = pipeline.getFirst();
		sink = (Trigger<Void, R>) pipeline.getLast();
		stateSink = (Sink<?>)sink;
		final Iterator<Trigger<R,E>> ascIter = this.pipeline.iterator();
		int index = 0;
		Trigger<R,E> priorTrigger = null;
		final LinkedHashSet<MBeanNotificationInfo> notifInfos = new LinkedHashSet<MBeanNotificationInfo>();
		Collections.addAll(notifInfos, NOTIF_INFOS);
		final Set<DelegateNotificationBroadcaster> notifDelegates = new HashSet<DelegateNotificationBroadcaster>();
		final StringBuilder b = new StringBuilder();
		while(ascIter.hasNext()) {
			Trigger<R,E> trigger = ascIter.next();
			b.append(trigger.getClass().getSimpleName()).append("-->");
			trigger.setPipelineContext(this, index);
			if(trigger instanceof DelegateNotificationBroadcaster) {
				final DelegateNotificationBroadcaster delegate = (DelegateNotificationBroadcaster)trigger;				
				notifDelegates.add(delegate);
				Collections.addAll(notifInfos, delegate.getNotificationInfo());
			}
			if(priorTrigger!=null) priorTrigger.setNextTrigger((Trigger<?, R>) trigger);
			priorTrigger = trigger;
			index++;
		}
		notificationBroadcaster = new NotificationBroadcasterSupport(SharedNotificationExecutor.getInstance(), notifInfos.toArray(new MBeanNotificationInfo[notifInfos.size()]));
		for(DelegateNotificationBroadcaster delegate: notifDelegates) {
				delegate.setNotificationBroadcaster(notificationBroadcaster, this.objectName, notifSerial);
		}
		
		b.deleteCharAt(b.length()-1); b.deleteCharAt(b.length()-1); b.deleteCharAt(b.length()-1);		
		flow = b.toString();
		JMXHelper.registerMBean(this, this.objectName);
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.TriggerPipelineMBean#getAdvisory()
	 */
	@Override
	public String getAdvisory() {
		return this.advisory.get();
	}
	
	public Trigger<Void, R> getSink() {
		return sink;
	}
	
	/**
	 * Sets an advisory message
	 * @param message an advisory message
	 */
	public void setAdvisory(final String message) {
		if(message==null || message.trim().isEmpty()) throw new IllegalArgumentException("The passed message was empty or null");
		final String prior = advisory.getAndSet(message.trim());
		if(!message.equals(prior)) {
			notificationBroadcaster.sendNotification(new Notification(NOTIF_ADVISORY, objectName, notifSerial.incrementAndGet(), System.currentTimeMillis(), message));
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.TriggerPipelineMBean#getFlow()
	 */
	@Override
	public String getFlow() {
		return flow;
	}
	
	/**
	 * Starts the pipeline
	 */
	public void start() {
		if(started.compareAndSet(false, true)) {
			final Iterator<Trigger<R,E>> descIter = this.pipeline.descendingIterator();
			while(descIter.hasNext()) {
				Trigger<R,E> trigger = descIter.next();
				trigger.start();
			}	
			notificationBroadcaster.sendNotification(new Notification(NOTIF_STARTED, objectName, notifSerial.incrementAndGet(), System.currentTimeMillis(), "[" + objectName + "] Pipeline Started"));
		}
	}
	
	/**
	 * Stops the pipeline
	 */
	public void stop() {
		if(started.compareAndSet(true, false)) {
			final Iterator<Trigger<R,E>> ascIter = this.pipeline.iterator();
			while(ascIter.hasNext()) {
				Trigger<R,E> trigger = ascIter.next();
				trigger.stop();
			}
			notificationBroadcaster.sendNotification(new Notification(NOTIF_STOPPED, objectName, notifSerial.incrementAndGet(), System.currentTimeMillis(), "[" + objectName + "] Pipeline Stopped"));
		}
	}
	
	/**
	 * Stops and unregisters the pipeline's JMX MBean
	 */
	public void unregister() {
		stop();
		JMXHelper.unregisterMBean(objectName);
	}
	@Override
	public ExecutorService getPipelineExecutor() {		
		return pipelineExecutor;
	}
	
	@Override
	public void eventSunk(final int triggerId, final Object event) {
		log.info("Event Sunk by [" + triggerId + "] --> [" + event + "]");		
	}
	
	public void in(final E e) {
		starter.in(e);
	}
	
	@Override
	public void onStateChange(final Trigger sender, final Object e) {
		if(sender==sink) {
			for(Iterator<Trigger<R,E>> iter = pipeline.descendingIterator(); iter.hasNext();) {
				final Trigger<R,E> t = iter.next();
				if(t!=sink) {
					try {
						t.onUpstreamStateChange((E) e);
					} catch (Exception x) {
						/* No Op */
					}
				}
			}
		}		
	}
	
	@Override
	public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws IllegalArgumentException {
		notificationBroadcaster.addNotificationListener(listener, filter, handback);
		
	}

	@Override
	public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
		notificationBroadcaster.removeNotificationListener(listener);
		
	}

	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		return notificationBroadcaster.getNotificationInfo();
	}

	@Override
	public boolean isStarted() {		
		return started.get();
	}

	public E getState() {
		final E e = (E) stateSink.getState();
		return e;
	}
	
	public String getStateName() {
		final E e = (E) stateSink.getState();
		return e==null ? null : e.toString();
	}
	
	public ObjectName getObjectName() {
		return objectName;
	}

	public E getPriorState(){
		return (E) stateSink.getPriorState();
	}

	public String getPriorStateName(){
		final E e = (E) stateSink.getPriorState();
		return e==null ? null : e.toString();
	}

	/**
	 * @return
	 * @see com.heliosapm.utils.events.Sink#getLastStatusChange()
	 */
	public long getLastStatusChange() {
		return stateSink.getLastStatusChange();
	}
	
	public Date getLastStatusChangeDate() {
		final long t = stateSink.getLastStatusChange();
		if(t==0L) return null;
		return new Date(stateSink.getLastStatusChange());
	}
	
}
