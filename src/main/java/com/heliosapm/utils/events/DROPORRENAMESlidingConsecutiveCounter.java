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
import java.util.concurrent.atomic.AtomicReference;

import com.heliosapm.utils.enums.BitMasked;
import com.heliosapm.utils.unsafe.UnsafeAdapter;

/**
 * <p>Title: SlidingConsecutiveCounter</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.SlidingConsecutiveCounter</code></p>
 */

public class DROPORRENAMESlidingConsecutiveCounter<E extends Enum<E> & BitMasked> {
	/** The event type */
	final Class<E> eventType;
	/** The event counter */
	final EnumMap<E, int[]> counters;
	/** The prior accepted event */
	final AtomicReference<E> prior = new AtomicReference<E>(null);
	/** All the event types included in this counter */
	final E[] eventTypes;
	/** Spin lock around the counter */
	protected final UnsafeAdapter.SpinLock lock = UnsafeAdapter.allocateSpinLock();

	
	/**
	 * Creates a new SlidingConsecutiveCounter
	 * @param eventType The event type enum
	 * @param eventTypes The event type members to be included in this counter. All will be included if this array is empty.
	 */
	public DROPORRENAMESlidingConsecutiveCounter(final Class<E> eventType, final E...eventTypes) {
		this.eventType = eventType;
		counters = new EnumMap<E, int[]>(eventType);
		this.eventTypes = eventTypes.length==0 ? eventType.getEnumConstants() : eventTypes;
		Arrays.sort(eventTypes);
		for(E e: eventTypes) {
			counters.put(e, new int[1]);
		}
	}
	
	/**
	 * Accepts a new event
	 * @param event The event
	 * @return The consecutive highwater for this event type
	 */
	public int accept(final E event) {
		try {
			lock.xlock(true);
			final int[] arr = counters.get(event);
			if(arr==null) {
				return -1;
			}
			arr[0]++;
			final E p = prior.getAndSet(event);
			if(event!=p) {				
				for(E e: eventTypes) {
					if(e!=event) {
						counters.get(e)[0] = 0;
					}
				}
			}
			return arr[0];						
		} finally {
			lock.xunlock();
		}
	}
	
	

}
