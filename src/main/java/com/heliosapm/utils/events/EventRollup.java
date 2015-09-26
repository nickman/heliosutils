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

/**
 * <p>Title: EventRollup</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.EventRollup</code></p>
 */

public enum EventRollup {
	/** Only the submitted event is counted */
	NONE,
	/** The submitted event and all of the ordinals above it are incremented */
	UP, 
	/** The submitted event and all of the ordinals below it are incremented */
	DOWN;
	
	public static class RollNone<E extends Enum<E>> implements EnumRollup<E> {		
		@Override
		public E[] rollup(Class<E> etype, final E e) {
			final E[] arr = (E[])Array.newInstance(etype, 1);
			arr[0] = e; 
			return arr;
		}		
	}
	
	public static class Rollup<E extends Enum<E>> implements EnumRollup<E> {		
		@Override
		public E[] rollup(Class<E> etype, final E e) {
			
			final E[] arr = (E[])Array.newInstance(etype, e.getDeclaringClass().getEnumConstants().length - e.ordinal());
			arr[0] = e; 
			return arr;
		}		
	}
	
}
