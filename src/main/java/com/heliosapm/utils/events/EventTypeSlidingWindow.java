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

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Map;

import com.heliosapm.utils.counters.alarm.AlarmState;
import com.heliosapm.utils.enums.BitMasked;
import com.heliosapm.utils.unsafe.UnsafeAdapter;

/**
 * <p>Title: EventTypeSlidingWindow</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.EventTypeSlidingWindow</code></p>
 */

public class EventTypeSlidingWindow<E extends Enum<E> & BitMasked> {
	/** Spin lock around the window */
	protected final UnsafeAdapter.SpinLock lock = UnsafeAdapter.allocateSpinLock();
	/** The max position in the series */
	protected final int maxPos;
	/** The current position in the series */
	protected int currentPos = -1;
	/** Full indicator */
	protected boolean full = false;
	/** The series as an int array */	
	protected final int[] series;
	/** A buffer wrapping the series so we can efficiently compact */
	protected final IntBuffer buffer;;
	/** The decode for the event types */
	protected final E[] eventTypes;
	/** The event type */
	protected final Class<E> eventType;
	/** Indicates if we're keeping the event times, or discarding them if supplied */
	protected final boolean keepTimes;
	/** The optional time series as an long array */	
	protected final long[] times;
	/** A buffer wrapping the time series so we can efficiently compact */
	protected final LongBuffer tbuffer;
	
	/**
	 * Creates a new EventTypeSlidingWindow
	 * @param eventType The event type
	 * @param windowSize The length of the window 
	 */
	public EventTypeSlidingWindow(final Class<E> eventType, final int windowSize, final boolean keepTimes) {
		maxPos = windowSize -1;		
		this.keepTimes = keepTimes;
		series = new int[windowSize];
		Arrays.fill(series, -1);
		buffer = IntBuffer.wrap(series);
		if(keepTimes) {
			times = new long[windowSize];
			tbuffer = LongBuffer.wrap(times);
		} else {
			times = null;
			tbuffer = null;
		}
		
		
		this.eventType = eventType;
		eventTypes = eventType.getEnumConstants();
	}
	
	/**
	 * Inserts a new event into the series, discarding the oldest if the series is full
	 * @param event The event to insert
	 * @param timestamp The timestamp for this event
	 */
	public void insert(final E event, final long timestamp) {
		if(event==null) throw new IllegalArgumentException("The passed event type was null");
		final int index = event.ordinal();
		try {
			lock.xlock(true);
			if(full) {
				slide();
			} else {
				currentPos++;
			}
			series[currentPos] = index;
			full = currentPos==maxPos;
			if(times!=null) {
				times[currentPos] = timestamp;
			}
		} finally {
			lock.xunlock();
		}
	}
	
	/**
	 * Inserts a new event into the series, discarding the oldest if the series is full.
	 * @param event The event to insert
	 */
	public void insert(final E event) {
		insert(event, times==null ? -1L : System.currentTimeMillis());
	}
	
	
	protected void slide() {
		buffer.position(1);
		buffer.compact();	
		if(tbuffer!=null) {
			tbuffer.position(1);
			tbuffer.compact();				
		}
	}
	
	/**
	 * Returns the number of events currently in the series
	 * @return the number of events currently in the series
	 */
	public int size() {
		return currentPos+1;
	}
	
	public int[] cardinality() {
		return cardinality(-1L, -1L);
	}
	
	public int[] cardinalitySince(final long time) {
		return cardinality(time, -1L);
	}
	
	public int[] cardinalityWithin(final long startTime, final long endTime) {
		return cardinality(startTime, endTime);
	}
	
	
	/**
	 * Returns a cardinality array for this series
	 * @param the start time
	 * @param the end time
	 * @return a cardinality array 
	 */
	protected int[] cardinality(final long startTime, final long endTime) {
		final boolean timingEnabled = startTime!=-1L;		
		if(timingEnabled && times==null) {
			throw new IllegalArgumentException("This sliding window is not enabled for event timing");
		}
		final int[] cards = new int[eventTypes.length];
		if(currentPos==-1) return cards;
		final int[] seriesClone;
		final long[] timesClone;
		final int size;
		try {
			lock.xlock();
			seriesClone = series.clone();
			timesClone =timingEnabled ? this.times.clone() : null;
			size = size();
		} finally {
			lock.xunlock();
		}
		if(timingEnabled) {
			final long end = endTime==-1L ? System.currentTimeMillis() : endTime;
			for(int i = 0; i < size; i++) {
				long t = timesClone[i];
				if(t >= startTime && t <= end) {
					cards[seriesClone[i]]++;
				}
			}
			
		} else {
			for(int i = 0; i < size; i++) {
				cards[seriesClone[i]]++;
			}			
		}		
		return cards;
	}
	
	/**
	 * Returns a cardinality map of the event series
	 * @return the numer of each event type keyed by the event type
	 */
	public Map<E, Integer> cardinalityMap() {
		final int[] cards = cardinality();
		return BitMasked.StaticOps.arrToMap(eventType, cards);
	}
	
	public static void main(String[] args) {
		log("EventSlidingWindow Test");
		final AlarmState[] states = AlarmState.values();
		final int mod = states.length;
		final EventTypeSlidingWindow<AlarmState> window = new EventTypeSlidingWindow<AlarmState>(AlarmState.class, 5, false);
		for(int i = 0; i < 10; i++) {
			final int index = i%mod;
			final AlarmState state = states[index];
			window.insert(states[i%mod]);
			log("#" + i + ": (" + state + "/" + index + ") " + Arrays.toString(window.series) + "     Map:" + window.cardinalityMap());
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	

}
