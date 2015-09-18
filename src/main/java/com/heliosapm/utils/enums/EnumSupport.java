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
package com.heliosapm.utils.enums;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: EnumSupport</p>
 * <p>Description: Base class for bitmasked enum classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.enums.EnumSupport</code></p>
 * @param <E> The enum type
 */
public class EnumSupport<E extends Enum<E>> {

	/** The enum type */
	protected final Class<E> enumType;
	/** The enum members */
	protected final E[] members;
	/** An enum member name (in upper) keyed map to lookup the member by upper name */
	protected final Map<String, E> nameMap;
	
	/** Empty array of members, constant */	
	public final E[] emptyArr;
	

	/**
	 * Creates a new EnumSupport
	 * @param enumType The enum type this instance will support
	 */
	@SuppressWarnings("unchecked")
	EnumSupport(final Class<E> enumType) {
		if(enumType==null) throw new IllegalArgumentException("The passed enum type was null");
		this.enumType = enumType;
		members = enumType.getEnumConstants();
		HashMap<String, E> _nameMap = new HashMap<String, E>(members.length); 
		for(int i = 0; i < members.length; i++ ) {
			_nameMap.put(members[i].name().toUpperCase(), members[i]);
		}
		nameMap = Collections.unmodifiableMap(_nameMap);
		emptyArr = (E[]) Array.newInstance(enumType, 0);
	}


	/**
	 * Decodes the passed string to the corresponding enum
	 * @param name The enum member name
	 * @return the corresponding enum member
	 */
	public E decode(final String name) {
		final E e = decodeOrNull(name);
		if(e==null) throw new IllegalArgumentException("The passed name [" + name + "] was not a valid enum member of [" + enumType.getName() + "]");
		return e;		
	}
	
	/**
	 * Decodes the passed string to the corresponding enum or null if it cannot be decoded
	 * @param name The enum member name
	 * @return the corresponding enum member or null if it did not match
	 */
	public E decodeOrNull(final String name) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		return nameMap.get(name.trim().toUpperCase());
	}

	/**
	 * Decodes each passed name to an enum member and returns an array of the matches
	 * @param ignoreErrors true to ignore match failures, false otherwise (except null or empty strings)
	 * @param names The names to decode
	 * @return a possibly zero length array of enum members
	 */
	public E[] from(final boolean ignoreErrors, final String...names) {
		if(names==null || names.length==0) return emptyArr;
		final EnumSet<E> matched = EnumSet.noneOf(enumType);
		for(String s: names) {
			if(s==null || s.trim().isEmpty()) continue;
			E e = decodeOrNull(s.trim().toUpperCase());
			if(e==null) {
				if(ignoreErrors) continue;
				throw new IllegalArgumentException("The passed name [" + s + "] was not a valid enum member of [" + enumType.getName() + "]"); 
			}
			matched.add(e);
		}
		return matched.toArray(emptyArr);
	}

	
	/**
	 * Decodes each passed name to an enum member and returns an array of the matches, ignoring any match failures
	 * @param names The names to decode
	 * @return a possibly zero length array of enum members
	 */
	public E[] from(final String...names) {
		return from(true, names);
	}
	
	public static class EnumCardinality<E extends Enum<E>> {
		/** The cardinality counting map */
		private final EnumMap<E, int[]> map;
		
		/**
		 * Creates a new EnumCardinality
		 * @param type The type of the enum we're counting for
		 */
		public EnumCardinality(final Class<E> type) {
			map = new EnumMap<E, int[]>(type);
			for(E e: type.getEnumConstants()) {
				map.put(e, new int[1]);
			}
		}
		
		/**
		 * Accumulates the passed enum events into the cardinality map
		 * @param es An array of enum members to accumulate
		 */
		public void accumulate(final E...es) {
			if(es!=null) {
				for(E e: es) {
					map.get(e)[0]++;
				}
			}			 
		}
		
		/**
		 * Returns the accumulated map
		 * @return the accumulated map
		 */
		public Map<E, int[]> getResults() {
			return map;
		}
	}
	

}