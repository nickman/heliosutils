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
package com.heliosapm.utils.counters;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;

import com.heliosapm.utils.enums.BitMasked;
import com.heliosapm.utils.unsafe.UnsafeAdapter;

/**
 * <p>Title: EventSeries</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.counters.EventSeries</code></p>
 * @param <E> The event type
 * @param <T> The sample value type
 */

public class EventSeries<T, E extends Enum<E> & BitMasked> {
	/** Spin lock around the counter */
	protected final UnsafeAdapter.SpinLock lock = UnsafeAdapter.allocateSpinLock();
	/** The cardinality counters */
	protected final int[] cardinality;
	/** The current number of samples */
	protected int count = 0;
	/** The sample window size */
	protected int window;
	/** The current sample series */
	protected LinkedList<EventSample> series;
	
	/** The event type */
	protected final Class<E> eventType;
	
	/** Ascending comparator */
	protected final Comparator<EventSample> asc = new EventSampleOrderByTimestamp();
	/** Descending comparator */
	protected final Comparator<EventSample> desc = Collections.reverseOrder(asc);
	/** An empty cardinality map */
	protected final Map<E, Integer> emptyMap;
	/** An all zero populated cardinality map */
	protected final Map<E, Integer> zeroMap;

	
	/** Empty int array const */
	public static final int[] EMPTY_INT_ARR = {};
	/** Empty long array const */
	public static final long[] EMPTY_LONG_ARR = {};
	
	/** The array of event types */
	protected final E[] eventTypes;
	/**
	 * Creates a new EventSeries
	 * @param window The window size
	 * @param eventType  The event type
	 */
	public EventSeries(final int window, final Class<E> eventType) {
		this.eventType = eventType;
		this.window = window;
		series = new LinkedList<EventSample>();
		eventTypes = eventType.getEnumConstants();
		this.cardinality = new int[eventTypes.length];
		emptyMap = Collections.unmodifiableMap(new EnumMap<E, Integer>(eventType));
		EnumMap<E, Integer> tmp = new EnumMap<E, Integer>(eventType);
		for(E e: eventTypes) {
			tmp.put(e, 0);
		}
		zeroMap = Collections.unmodifiableMap(tmp);
	}
	
	/**
	 * Returns the cardinalities of the passed event types
	 * @param eventTypes The event types to get the cardinalities for
	 * @return an array of the event type cardinalities in the order the types were supplied
	 */
	public int[] getCardinalities(final E...eventTypes) {
		final int len = eventTypes.length;
		if(len==0) return EMPTY_INT_ARR;
		final int[] cards = new int[len];
		try {
			lock.xlock();
			for(int i = 0; i < len; i++) {
				cards[i] = cardinality[eventTypes[i].ordinal()];				
			}
		} finally {
			lock.xunlock();
		}
		return cards;
	}
	
	/**
	 * Returns a cardinalities map of all event types
	 * @return a map of event counts keyed by the event type
	 */
	public Map<E, Integer> getCardinalityMap() {
		if(count==0) return emptyMap;
		final int[] cards = getCardinalities();
		final EnumMap<E, Integer> map = new EnumMap<E, Integer>(zeroMap);
		for(E e: eventTypes) {
			map.put(e, cards[e.ordinal()]);
		}
		return map;
	}
	
	
	/**
	 * Returns the cardinalities for all types
	 * @return an array of the event type cardinalities in the order the types were supplied
	 */
	public int[] getCardinalities() {
		try {
			lock.xlock();
			return cardinality.clone();
		} finally {
			lock.xunlock();
		}		
	}
	
	
	/**
	 * Accepts a new sample into the series
	 * @param eventType The event type
	 * @param timestamp The timestamp of the sample
	 * @param value The value of the sample
	 */
	public void sample(final E eventType, final long timestamp, final T value) {
		final EventSample sample = new EventSample(eventType.ordinal(), timestamp, value);
		try {
			lock.xlock(true);
			series.addFirst(sample);
			if(count==window) {
				final EventSample removed = series.removeLast();
				cardinality[removed.eventType]--;
			} else {
				count++;
				cardinality[eventType.ordinal()]++;
			}
		} finally {
			lock.xunlock();
		}
	}
	
	/**
	 * Accepts a new sample into the series using the current timestamp
	 * @param eventType The event type
	 * @param value The value of the sample
	 */
	public void sample(final E eventType, final T value) {
		sample(eventType, System.currentTimeMillis(), value);
	}
	

	/**
	 * Accepts a new sample into the series
	 * @param eventType The event type ordinal
	 * @param timestamp The timestamp of the sample
	 * @param value The value of the sample
	 */
	public void sample(final int eventType, final long timestamp, final T value) {
		sample(eventTypes[eventType], timestamp, value);
	}
	
	/**
	 * Accepts a new sample into the series using the current timestamp
	 * @param eventType The event type ordinal
	 * @param value The value of the sample
	 */
	public void sample(final int eventType, final T value) {
		sample(eventTypes[eventType], System.currentTimeMillis(), value);
	}

	
	/**
	 * <p>Title: EventSample</p>
	 * <p>Description: Container for an event series sample instance</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.counters.EventSeries.EventSample</code></p>
	 */
	public class EventSample {
		/** The event type ordinal */
		final int eventType;
		/** The sample timestamp */
		final long timestamp;
		/** The sample value */
		final T value;
		
		/**
		 * Creates a new EventSample
		 * @param eventType The event type ordinal
		 * @param timestamp The timestamp for this sample
		 * @param value The value of this sample
		 */
		private EventSample(final int eventType, final long timestamp, final T value) {
			this.eventType = eventType;
			this.timestamp = timestamp;
			this.value = value;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return new StringBuilder("Event Sample (")
				.append(value.getClass().getName())
				.append(") [eventType:").append(eventTypes[eventType].name())
				.append(", ts:").append(new Date(timestamp))
				.append(", value:").append(value)
				.append("]")
				.toString();
		}
		
	}
	
	/**
	 * <p>Title: EventSampleOrderByTimestamp</p>
	 * <p>Description: EventSample comparator to sort by timestamp</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.counters.EventSeriesEventSampleOrderByTimestamp</code></p>
	 */
	public class EventSampleOrderByTimestamp implements Comparator<EventSample> {

		/**
		 * Compares its two arguments for order. Returns a negative integer, zero, 
		 * or a positive integer as the first argument is less than, equal to, or greater than the second.
		 * {@inheritDoc}
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(final EventSeries<T, E>.EventSample e1, final EventSeries<T, E>.EventSample e2) {			
			return (e1.timestamp - e2.timestamp)<1 ? 1 : -1; 
		}

		
	}
	
	

}
