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

import java.util.EnumMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.heliosapm.utils.enums.BitMasked;
import com.heliosapm.utils.enums.Rollup;

/**
 * <p>Title: SlidingConsecutiveEventTrigger</p>
 * <p>Description: Consecutive event trigger that does not wind down and alarms until the consecutive streak ends</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.SlidingConsecutiveEventTrigger</code></p>
 * @param <E> The event type
 */

public class SlidingConsecutiveEventTrigger<E extends Enum<E> & BitMasked> extends AbstractConsecutiveEventTrigger<E> {

	/**
	 * Creates a new SlidingConsecutiveEventTrigger
	 * @param eventType The event type class
	 * @param thresholds A map of the triggering consecutive thresholds for each triggering event type
	 * @param acceptedEvents The events accepted by this trigger. If length is zero, will assume all event types
	 */
	public SlidingConsecutiveEventTrigger(final Class<E> eventType, final EnumMap<E, Integer> thresholds, final E... acceptedEvents) {
		super(eventType, thresholds, acceptedEvents);
	}
	
	/**
	 * Creates a new SlidingConsecutiveEventTrigger from the passed JSON definition
	 * @param jsonDef The json trigger definition
	 * @return the SlidingConsecutiveEventTrigger
	 */
	@SuppressWarnings("unchecked")
	public static <E extends Enum<E> & BitMasked> SlidingConsecutiveEventTrigger<E> fromJSON(final JSONObject jsonDef) {
		if(jsonDef==null) throw new IllegalArgumentException("The passed JSON was null");
		final Class<E> eventType;
		final EnumMap<E, Integer> thresholds;
		final E[] acceptedEvents;
		final String eventTypeName = jsonDef.optString("eventType");
		if(eventTypeName==null) throw new IllegalArgumentException("The passed JSON did not contain an eventType");
		final JSONObject thresholdJsonMap = jsonDef.optJSONObject("thresholds");
		if(thresholdJsonMap==null) throw new IllegalArgumentException("The passed JSON did not contain a thresholds map");
		JSONArray acceptedEventArr = jsonDef.optJSONArray("acceptedEvents");
		if(acceptedEventArr==null) acceptedEventArr = new JSONArray();
		try {
			eventType = (Class<E>) Class.forName(eventTypeName, true, Thread.currentThread().getContextClassLoader());
			thresholds = new EnumMap<E, Integer>(eventType);
			for(Object key: thresholdJsonMap.keySet()) {
				final String sKey = key.toString();
				final E event = Enum.valueOf(eventType, sKey.trim().toUpperCase());
				final int t = thresholdJsonMap.getInt(sKey);
				thresholds.put(event, t);
			}
			final int ecnt = acceptedEventArr.length(); 
			if(ecnt==0) {
				acceptedEvents = eventType.getEnumConstants();
			} else {
				acceptedEvents = BitMasked.StaticOps.makeArr(eventType, ecnt);
				for(int i = 0; i < ecnt; i++) {
					acceptedEvents[i] = Enum.valueOf(eventType, acceptedEventArr.getString(i).trim().toUpperCase());
				}
			}
			return new SlidingConsecutiveEventTrigger<E>(eventType, thresholds, acceptedEvents);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to build SlidingConsecutiveEventTrigger from JSON", ex);
		}
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#windDown(java.lang.Enum)
	 */
	@Override
	public void windDown(final E event) {
		final boolean lockedByMe = lock.isLockedByMe();
		try {
			if(!lockedByMe) lock.xlock();
			counters.get(event)[0]--;
		} finally {
			if(!lockedByMe) lock.xunlock();
		}		
	}


}
