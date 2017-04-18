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
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;





/**
 * <p>Title: EnumSupport</p>
 * <p>Description: Base class for bitmasked enum classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.enums.EnumSupport</code></p>
 * @param <E> The enum type
 */
public class EnumSupport {
	/** The maximum number of members supported */
	public static final int MAX_MEMBERS = Integer.SIZE - 1;
	/** The first {@link #MAX_MEMBERS} power of twos */
	private static final int[] POW2 = new int[]{1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824};
	
	private static class EnumCache<E extends Enum<E>> {
		final ConcurrentHashMap<Class<E>, E[]> cache = new ConcurrentHashMap<Class<E>, E[]>();
		final ConcurrentHashMap<Class<E>, E[]> emptyArrCache = new ConcurrentHashMap<Class<E>, E[]>();
		final ConcurrentHashMap<Class<E>, Map<String, E>> nameCache = new ConcurrentHashMap<Class<E>, Map<String, E>>();

		public E forName(final String name, final Class<E> enumType) {
			Map<String, E> map = nameCache.get(enumType);
			if(map==null) {
				synchronized(nameCache) {
					map = nameCache.get(enumType);
					if(map==null) {
						final E[] es = enumType.getEnumConstants();
						map = new HashMap<String, E>(es.length);
						for(E e: es) {
							map.put(e.name().toUpperCase(), e);
						}
						nameCache.put(enumType, map);
					}
				}
			}			
			return map.get(name.trim().toUpperCase());
		}
		
		public E[] put(Class<E> enumType) {
			E[] es = cache.get(enumType);
			if(es==null) {
				synchronized(cache) {
					es = cache.get(enumType);
					if(es==null) {
						es = enumType.getEnumConstants();
						cache.put(enumType, es);
						if(es.length!=0) {
							forName(es[0].name(), enumType);
						}
					}
				}
			}
			return es;
		}
		
		@SuppressWarnings("unchecked")
		public E[] empty(Class<E> enumType) {
			E[] es = emptyArrCache.get(enumType);
			if(es==null) {
				synchronized(cache) {
					es = cache.get(enumType);
					if(es==null) {
						es = (E[]) java.lang.reflect.Array.newInstance(enumType.getEnumConstants().getClass().getComponentType(), 0);						
						cache.put(enumType, es);
						if(es.length!=0) {
							forName(es[0].name(), enumType);
						}
					}
				}
			}
			return es;
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	private static final EnumCache enumCache = new EnumCache();
	
	public static <E extends Enum<E>> int getMask(E e) {
		if(e==null) throw new IllegalArgumentException("The passed enum member was null");
		return POW2[e.ordinal()];
	}


	/**
	 * Decodes the passed object to the corresponding enum
	 * @param name The enum member name
	 * @return the corresponding enum member
	 */
	public static <E extends Enum<E>> E decode(final Object code, final Class<E> enumType) {
		if(code==null) throw new IllegalArgumentException("The passed code was null");
		if(enumType==null) throw new IllegalArgumentException("The passed enum type was null");
		final E e = decodeOrNull(code, enumType);
		if(e==null) throw new IllegalArgumentException("The passed code [" + code + "] was not a valid enum member of [" + enumType.getName() + "]");
		return e;		
	}
	



	/**
	 * Decodes the passed object to the corresponding enum or null if it cannot be decoded
	 * @param name The enum member name
	 * @param enumType The bitmasked enum class
	 * @return the corresponding enum member or null if it did not match
	 */
	@SuppressWarnings("unchecked")
	public static <E extends Enum<E>> E decodeOrNull(final Object code, final Class<E> enumType) {
		if(code==null) return null;
		if(enumType==null) throw new IllegalArgumentException("The passed enum type was null");		
		if(isNumber(code)) {
			
			final E[] es = (E[]) enumCache.put(enumType);
			final int ord = ((Number)code).intValue();
			if(ord < 0 || ord > es.length-1) return null;
			return es[ord];
		}
		final String sval = code.toString().trim();
		final Integer ord = fromCode(sval);
		if(ord!=null) {
			
			final E[] es = (E[]) enumCache.put(enumType);
			if(ord < 0 || ord > es.length-1) return null;
			return es[ord];
		}
		try {
			return (E) enumCache.forName(sval, enumType);
		} catch (Exception x) {/* No Op */}
		return null;
	}
	
	private static boolean isNumber(final Object obj) {
		return obj!=null && (obj instanceof Number);
	}
	
	private static Integer fromCode(final String code) {
		if(code.isEmpty()) return null;
		try {
			return new Double(code).intValue();
		} catch (Exception x) {
			return null;
		}
	}

	/**
	 * Decodes each passed name to an enum member and returns an array of the matches
	 * @param ignoreErrors true to ignore match failures, false otherwise (except null or empty strings)
	 * @param enumType The enum class
	 * @param codes The values to decode
	 * @return A possibly empty array of enum members
	 */
	public static <E extends Enum<E>> E[] from(final boolean ignoreErrors, final Class<E> enumType, final Object...codes) {
		if(enumType==null) throw new IllegalArgumentException("The passed enum type was null");
		@SuppressWarnings("unchecked")
		final E[] empty = (E[]) enumCache.empty(enumType);
		if(codes==null || codes.length==0) return empty;
		final EnumSet<E> matched = EnumSet.noneOf(enumType);
		for(Object code: codes) {
			if(code==null) continue;
			E e = decodeOrNull(code, enumType);
			if(e==null) {
				if(ignoreErrors) continue;
				throw new IllegalArgumentException("The passed code [" + code + "] was not a valid enum member of [" + enumType.getName() + "]"); 
			}
			matched.add(e);
		}
		return matched.toArray(empty);
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

    /**
     * Acquires the combined bitmask of the passed enum class members 
     * where the enabled members are identified by the passed names, each name optionally being a member name
     * or a member ordinal. Each passed name is trimmed and uppercased, but matched ignoring case. 
     * @param strict If true, a runtime exception will be thrown if any of the names cannot be matched
     * @param clazz The BitMasked enum class
     * @param codes The names or ordinals of the members
     * @return the effective bit mask
     */
	public static <E extends Enum<E> & BitMasked> int getEnabledBitMask(final boolean strict, Class<E> enumType, Object[] codes) {
		if(enumType==null) throw new IllegalArgumentException("The passed enum type was null");
		int mask = 0;
		if(codes!=null && codes.length > 0) {
			for(Object code: codes) {
				final E e = decodeOrNull(code, enumType);
				if(e==null) {
					if(strict) continue;
					throw new IllegalArgumentException("The passed code [" + code + "] was not a valid enum member of [" + enumType.getName() + "]");
				} 
				mask = mask | e.getMask();
			}
		}
		return mask;
	}
	
	
	/**
	 * Returns the bit mask for the passed enum ordinal
	 * @param member The enum member
	 * @return the bit mask for the passed member
	 */
	public static <E extends Enum<E>> int ordinalBitMaskInt(final E member) {
		return EnumSupport.getMask(member);
	}
	
	/**
	 * Determines if the passed member is enabled in the passed mask
	 * @param member The member to test
	 * @param mask The mask to test for
	 * @return true if enabled, false otherwise
	 */
	public static boolean isEnabled(final BitMasked member, final int mask) {
		return mask == (mask | member.getMask());
	}
	
	/**
	 * Accepts the passed mask and enables the mask of the passed member and returns the resulting mask
	 * @param member The member for which to enable the mask
	 * @param mask The starting mask
	 * @return The resulting mask
	 */
	public static int enableFor(final BitMasked member, final int mask) {
		return mask | member.getMask();
	}
	
	/**
	 * Accepts the passed mask and disables the mask of the passed member and returns the resulting mask
	 * @param member The member for which to disable the mask
	 * @param mask The starting mask
	 * @return The resulting mask
	 */
	public static int disableFor(final BitMasked member, final int mask) {
		return mask & ~member.getMask();
	}
	
	/**
	 * Returns a set of bitmasked enums which are enabled in the passed mask
	 * @param type The bitmasked enum type
	 * @param mask The mask to match against
	 * @return A possibly empty set of bitmasked enums
	 */
	public static <E extends Enum<E> & BitMasked> Set<E> membersFor(final Class<E> type, final int mask) {
		final EnumSet<E> set = EnumSet.noneOf(type);			
		for(E e: type.getEnumConstants()) {
			if(e.isEnabled(mask)) set.add(e);
		}
		return set;
	}
	
	/**
	 * Converts a collection of bitmasked enums to an array
	 * @param type The bitmasked enum type
	 * @param collection A collection of enums
	 * @return An array of bitmasked enums (unique)
	 */
	public static <E extends Enum<E> & BitMasked> E[] toArray(final Class<E> type, final Collection<E> collection) {
		final EnumSet<E> set = EnumSet.noneOf(type);
		set.addAll(collection);
		if(set.isEmpty()) return makeArr(type, 0);
		final int size = set.size();
		final E[] arr = makeArr(type, size);
		int ctr = 0;
		for(E e: set) {
			arr[ctr] = e;
			ctr++;
		}
		return arr;
	}
	
	/**
	 * Returns the enabled members for the passed mask
	 * @param enumType The
	 * @param type The bitmasked enum type
	 * @return
	 */
	public static <E extends Enum<E> & BitMasked> EnumSet<E> getEnabled(final Class<E> type, final int mask) {
		final EnumSet<E> set = EnumSet.noneOf(type);
		@SuppressWarnings("unchecked")
		final E[] es = (E[])enumCache.put(type);
		for(E e: es) {
			if(e.isEnabled(mask)) {
				set.add(e);
			}
		}
		return set;
	}
	
	public static <E extends Enum<E> & BitMasked> Map<E, Integer> arrToMap(final Class<E> type, final int[] cards) {
		final E[] types = type.getEnumConstants();
		if(cards==null || cards.length!=types.length) throw new IllegalArgumentException("The length of the array was not equal to the number of enum elements for [" + type.getName() + "]");
		final EnumMap<E, Integer> map = new EnumMap<E, Integer>(type);
		for(int i = 0; i < types.length; i++) {
			map.put(types[i], cards[i]);
		}
		return map;
	}
	
	
	
	public static <E extends Enum<E> & BitMasked> int[] mapToArr(final Class<E> type, final Map<E, Integer> map) {
		final E[] types = type.getEnumConstants();
		final int[] arr = new int[types.length];
		if(!map.isEmpty()) {
			for(Map.Entry<E, Integer> entry: map.entrySet()) {
				arr[entry.getKey().ordinal()] = entry.getValue();
			}
		}			
		return arr;			
	}
	
	public static <E extends Enum<E> & BitMasked> Class<E> classFor(final E e) {
		return e.getDeclaringClass();
	}
	
	
	public static <E extends Enum<E> & BitMasked> int maskFor(final Collection<E> members) {
		int mask = 0;
		for(E e : members) {
			if(e==null) continue;
			mask = mask | e.getMask();
		}			
		return mask;
	}
	
	
	public static <E extends Enum<E>> E[] makeArr(final Class<E> type, final E...members) {
		if(members.length==0) return makeArr(type, 0);
		final E[] arr = makeArr(type, members.length);
		Arrays.sort(members);
		System.arraycopy(members, 0, arr, 0, members.length);
		return arr;
	}
	
	public static <E extends Enum<E>> E[] makeArr(final Class<E> type, final Object...members) {
		if(members.length==0) return (E[]) enumCache.empty(type);
		final E[] arr = makeArr(type, members.length);
		for(int i = 0; i < members.length; i++) {
			if(members[i]==null) continue;
			arr[i] = decode(members[i], type);		
		}
		Arrays.sort(arr);
		return arr;
	}
	
	public static <E extends Enum<E>> E[] makeArr(final Class<E> type, final Collection<String> memberNames) {
		return makeArr(type, memberNames.toArray(new String[memberNames.size()]));
	}
	
	
	@SuppressWarnings("unchecked")
	public static <E extends Enum<E>> E[] makeArr(final Class<E> type, final int length) {
		return (E[]) Array.newInstance(type, length);
	}
	
	public static <E extends Enum<E>> E[] rollup(final Class<E> type, final E member, final E...members) {
		final int len = members.length;
		if(len==0) return makeArr(type, 0);
		Arrays.sort(members);
		final int index = Arrays.binarySearch(members, member);
		if(index<0) return makeArr(type, 0);
		final E[] arr = makeArr(type, len - index);
		System.arraycopy(members, index, arr, 0, len - index);
		return arr;			
	}
	
	public static <E extends Enum<E>> E[] rolldown(final Class<E> type, final E member, final E...members) {
		final int len = members.length;
		if(len==0) return makeArr(type, 0);
		Arrays.sort(members);
		final int index = Arrays.binarySearch(members, member);
		if(index<0) return makeArr(type, 0);
		final E[] arr = makeArr(type, index+1);
		System.arraycopy(members, 0, arr, 0, index+1);
		return arr;			
	}
	
	public static <E extends Enum<E>> boolean isIn(final E member, final E...members) {
		if(members.length==0) return false;
		for(int i = 0; i < members.length; i++) {
			if(members[i]==member) return true;
		}
		return false;
	}
	
	public static <E extends Enum<E>> E[] roll(final Class<E> type, final Rollup rollType, final E member, final E...candidates) {
		final E[] mems = candidates.length==0 ? type.getEnumConstants() : candidates;
		if(!isIn(member, mems)) return makeArr(type, 0);
		switch(rollType) {
			case DOWN:
				return rolldown(type, member, mems);
			case NONE:
				return makeArr(type, member);					 					
			case UP:
				return rollup(type, member, mems);
			default:
				return makeArr(type, 0);				
		}
	}
	

}