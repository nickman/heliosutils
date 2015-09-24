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

import java.lang.reflect.Array;

import com.heliosapm.utils.enums.BitMasked;

/**
 * <p>Title: TriggerType</p>
 * <p>Description: Functional enumeration of the supported trigger types</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.TriggerType</code></p>
 */

public enum TriggerType implements TriggerFactory {
	SLIDINGCONSEC(new SlidingConsecutiveEventTriggerFactory()),
	TUMBLINGCONSEC(new TumblingConsecutiveEventTriggerFactory()),
	DECAY(null);

	private TriggerType(final TriggerFactory tf) {
		this.tf = tf;
	}
	
	final TriggerFactory tf;
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.TriggerFactory#createTrigger(java.lang.Class, java.lang.Class, java.lang.Object[])
	 */
	@Override
	public Trigger createTrigger(final Class returnType, final Class eventType, final Object... params) {
		return tf.createTrigger(returnType, eventType, params);
	}
	
	/**
	 * Creates a new trigger
	 * @param type
	 * @param returnType
	 * @param eventType
	 * @param params
	 * @return
	 */
	public static <R, E extends Enum<E> & BitMasked> Trigger<R,E> trigger(final TriggerType type, final Class<R> returnType, final Class<E> eventType, final Object...params) {
		return type.createTrigger(returnType, eventType, params);
	}

	
	private static class SlidingConsecutiveEventTriggerFactory<R, E extends Enum<E> & BitMasked> implements TriggerFactory<R, E> {
		@Override
		public Trigger<R, E> createTrigger(Class<R> returnType, Class<E> eventType, Object... params) {
			final Long c = (Long)params[0];
			final E[] states = (E[]) Array.newInstance(eventType, params.length-1);
			System.arraycopy(params, 1, states, 0, params.length-1);
			return new SlidingConsecutiveEventTrigger(c, states);
		}
	}
	
	private static class TumblingConsecutiveEventTriggerFactory<R, E extends Enum<E> & BitMasked> implements TriggerFactory<R, E> {
		@Override
		public Trigger<R, E> createTrigger(Class<R> returnType, Class<E> eventType, Object... params) {
			final Long c = (Long)params[0];
			final E[] states = (E[]) Array.newInstance(eventType, params.length-1);
			System.arraycopy(params, 1, states, 0, params.length-1);
			return new TumblingConsecutiveEventTrigger(c, states);
		}
	}

	
	
	
	
}
