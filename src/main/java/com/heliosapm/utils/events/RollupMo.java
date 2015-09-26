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
import java.util.EnumMap;

/**
 * <p>Title: RollupMo</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.RollupMo</code></p>
 */

public class RollupMo<E extends Enum<E>> {
	/** The singleton instance */
	private static volatile RollupMo instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();

	
	public enum Rollup {
		/** Only the submitted event is counted */
		NONE,
		/** The submitted event and all of the ordinals above it are incremented */
		UP, 
		/** The submitted event and all of the ordinals below it are incremented */
		DOWN;		
	}
	
	/**
	 * Acquires and returns the RollupMo singleton
	 * @return the RollupMo singleton
	 */
	public static RollupMo getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new RollupMo();
				}
			}
		}
		return instance;
	}

	
	
	public EnumMap<Rollup, EnumMap<E, E[]>> computeRolls(final Class<E> type) {
		final EnumMap<Rollup, EnumMap<E, E[]>> emap = new EnumMap<Rollup, EnumMap<E, E[]>>(Rollup.class);
		final E[] enumTypes = type.getEnumConstants();
		final int ecnt = enumTypes.length;
		// ================
		// ====== NONE  ======
		// ================
		EnumMap<E, E[]> none = new EnumMap<E, E[]>(type);
		for(E e: type.getEnumConstants()) {
			E[] arr = (E[])Array.newInstance(type, 1);
			arr[0] = e;
			none.put(e, arr);
			emap.put(Rollup.NONE, none);
		}
		// ================
		// ====== UP  ======
		// ================
		EnumMap<E, E[]> up = new EnumMap<E, E[]>(type);
		for(E e: type.getEnumConstants()) {
			final int size = ecnt - e.ordinal();
			E[] arr = (E[])Array.newInstance(type, size);
			for(int i = e.ordinal(); i < ecnt; i++) {
				
			}
			arr[0] = e;
			none.put(e, arr);
			emap.put(Rollup.UP, up);
		}
		
		
		// ================
		// ====== DOWN  ======
		// ================
		
		
		return emap;
	}

}
