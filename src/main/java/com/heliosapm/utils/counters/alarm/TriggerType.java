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
package com.heliosapm.utils.counters.alarm;

import java.lang.reflect.Array;

import com.heliosapm.utils.enums.BitMasked;

/**
 * <p>Title: TriggerType</p>
 * <p>Description: Functional enumeration of the supported trigger types</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.counters.alarm.TriggerType</code></p>
 */

public enum TriggerType implements TriggerFactory {
	CONSECUNTILRESET,
	RESETTINGCONCEC,
	DECAY;

	
	
	private static class ConsecUntilResetTriggerFactory<R, E extends Enum<E> & BitMasked> implements TriggerFactory<R, E> {

		@Override
		public Trigger<R, E> createTrigger(Class<R> returnType, Class<E> eventType, Object... params) {
			final Long c = (Long)params[0];
			final E[] states = (E[]) Array.newInstance(eventType, params.length-1);
			System.array
			return null;
		}
		
	}
	
	
	
}
