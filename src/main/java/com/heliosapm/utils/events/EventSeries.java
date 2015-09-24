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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.heliosapm.utils.enums.BitMasked;
import com.heliosapm.utils.unsafe.UnsafeAdapter;

/**
 * <p>Title: EventSeries</p>
 * <p>Description: Maintains cardinality counts for a series of events</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.EventSeries</code></p>
 * @param <T> The sample value type
 * @param <E> The event type
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
	/** Indicates if we're keeping samples */
	protected final boolean keepSamples;
	
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
	/** The ordinal of the last event type sampled */
	protected int lastEventType = -1;

	
	/** Empty int array const */
	public static final int[] EMPTY_INT_ARR = {};
	/** Empty long array const */
	public static final long[] EMPTY_LONG_ARR = {};
	
	/** The array of event types */
	protected final E[] eventTypes;
	/**
	 * Creates a new EventSeries
	 * @param window The window size
	 * @param keepSamples True to keep samples in a sliding window, false otherwise
	 * @param eventType  The event type
	 */
	public EventSeries(final int window, final boolean keepSamples, final Class<E> eventType) {
		this.keepSamples = keepSamples;
		this.eventType = eventType;
		this.window = window;
		series = keepSamples ? new LinkedList<EventSample>() : null;
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
		if(keepSamples) {
			final EventSample sample = new EventSample(eventType, timestamp, value);
			try {
				lock.xlock(true);
				series.addFirst(sample);
				if(count==window) {
					cardinality[eventType.ordinal()]++;
					final EventSample removed = series.removeLast();
					cardinality[removed.eventType.ordinal()]--;
				} else {
					count++;					
				}
			} finally {
				lock.xunlock();
			}			
		} else {
			final int ord = eventType.ordinal();
			try {				
				lock.xlock(true);						
				if(lastEventType!=ord) {
					if(count==window) {
						cardinality[ord]++;
						cardinality[lastEventType]--;
					} else {
						count++;
						cardinality[ord]++;
					}
				}
			} finally {
				lastEventType = ord;
				lock.xunlock();
			}			
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
	 * Returns a collection of all the samples in this series that match the passed filter
	 * @param filter The filter to include samples with
	 * @return a [possibly empty] collection of matching samples
	 */
	public Collection<EventSample> filterIn(final EventSampleFilter<T, E> filter) {
		return filter(true, filter);
	}
	
	/**
	 * Returns a collection of all the samples in this series that <b>do not</b> match the passed filter
	 * @param filter The filter to include samples with
	 * @return a [possibly empty] collection of non-matching samples
	 */
	public Collection<EventSample> filterOut(final EventSampleFilter<T, E> filter) {
		return filter(false, filter);
	}
	
	/**
	 * Returns a cardinality array of all the samples in this series that match the passed filter
	 * @param filter The filter to include samples with
	 * @return a cardinality array
	 */
	public int[] cardinalityIn(final EventSampleFilter<T, E> filter) {
		return cardinality(true, filter);
	}
	
	/**
	 * Returns a cardinality array of all the samples in this series that <b>do not</b> match the passed filter
	 * @param filter The filter to include samples with
	 * @return a cardinality array
	 */
	public int[] cardinalityEx(final EventSampleFilter<T, E> filter) {
		return cardinality(false, filter);
	}
	
	/**
	 * Returns the number of samples in the series with a timestamp between the {@code startTime} (inclusive)
	 * and the {@code endTime} (inclusive) and of one of the passed event types
	 * @param startTime The start time of the time range in ms timestamp
	 * @param endTime The end time of the time range in ms timestamp
	 * @param eventTypes The event types we're looking for
	 * @return the count of located samples
	 */
	public int countWithin(final long startTime, final long endTime, E...eventTypes) {
		if(!keepSamples) throw new RuntimeException("Sample filtering not supported as this EventSeries does not keep samples");
		if(count==0) return 0;
		return filterIn(new FilterByEventTypeAndTime(startTime, endTime, eventTypes)).size();
	}
	
	/**
	 * Returns the number of samples in the series with a timestamp equal to or later than the {@code startTime}
	 * @param startTime The start time of the time range in ms timestamp
	 * @param eventTypes The event types we're looking for
	 * @return the count of located samples
	 */
	public int countSince(final long startTime, E...eventTypes) {
		if(!keepSamples) throw new RuntimeException("Sample filtering not supported as this EventSeries does not keep samples");
		if(count==0) return 0;
		return filterIn(new FilterByEventTypeAndTime(startTime, eventTypes)).size();
	}
	
	protected int[] cardinality(final boolean inclusive, final EventSampleFilter<T, E> filter) {
		if(!keepSamples) throw new RuntimeException("Sample filtering not supported as this EventSeries does not keep samples");
		final int[] cards = new int[eventTypes.length];
		for(EventSample sample: filter(inclusive, filter)) {
			cards[sample.eventType.ordinal()]++;
		}
		return cards;
	}
	
	protected Collection<EventSample> filter(final boolean inclusive, final EventSampleFilter<T, E> filter) {
		if(!keepSamples) throw new RuntimeException("Sample filtering not supported as this EventSeries does not keep samples");
		final List<EventSample> samples = new LinkedList<EventSample>();
		if(count==0) return samples;
		try {
			lock.xlock();
			samples.addAll(series);
		} finally {
			lock.xunlock();
		}
		Collections.sort(samples, asc);
		for(final Iterator<EventSample> iter = samples.iterator(); iter.hasNext();) {			
			if(filter.filter(iter.next())) {
				if(inclusive) continue;
				iter.remove();
			}
		}
		return samples;
	}
	
	
	/**
	 * <p>Title: EventSampleFilter</p>
	 * <p>Description: Defines a filter that filters in matching event samples from an {@link EventSeries}</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.events.EventSeries.EventSampleFilter</code></p>
	 */
	public interface EventSampleFilter<T, E extends Enum<E> & BitMasked> {
		/**
		 * Examines the passed sample to determine if the sample matches the filter's criteria
		 * @param sample The sample to examine
		 * @return true if the sample matches the filter's criteria, false otherwise
		 */
		public boolean filter(EventSeries<T, E>.EventSample sample);
	}
	
	public class FilterByEventTypeAndTime implements EventSampleFilter<T, E> {
		final int eventTypeMask;
		final long startTime;
		final long endTime;
		
		

		public FilterByEventTypeAndTime(final long startTime, final long endTime, final E...eventTypes) {
			eventTypeMask = BitMasked.StaticOps.maskFor(eventTypes);
			this.startTime = startTime;
			this.endTime = endTime;
		}

		public FilterByEventTypeAndTime(final long startTime, final E...eventTypes) {
			this(startTime, System.currentTimeMillis(), eventTypes);
		}


		@Override
		public boolean filter(final EventSample sample) {			
			return (
				(eventTypeMask | sample.eventType.getMask()) == eventTypeMask &&
				sample.timestamp >= startTime &&
				sample.timestamp <= endTime
			);
		}
		
	}


	
	

	
	/**
	 * <p>Title: EventSample</p>
	 * <p>Description: Container for an event series sample instance</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.events.EventSeries.EventSample</code></p>
	 */
	public class EventSample {
		/** The event type ordinal */
		final E eventType;
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
		private EventSample(final E eventType, final long timestamp, final T value) {
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
				.append(") [eventType:").append(eventType.name())
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
