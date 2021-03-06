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
import java.util.Map;
import java.util.Set;

/**
 * <p>Title: BitMasked</p>
 * <p>Description: Defines the basic bit maked enums operations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.enums.BitMasked</code></p>
 */

public interface BitMasked {


	/**
	 * Returns the mask for this member
	 * @return the mask for this member
	 */
	public int getMask();
	
	/**
	 * Indicates if the passed mask is enabled for the current member
	 * @param mask The mask to test
	 * @return true if enabled, false otherwiseEnum<E>
	 */
	public boolean isEnabled(final int mask);
	
	public int enableFor(int mask);
	
	public int disableFor(int mask);
	
//	public <E extends Enum<E> & BitMasked> int maskFor(final E...members);
	
	
	
	/**
	 * <p>Title: StaticOps</p>
	 * <p>Description: Static methods to support {@link BitMasked}</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.enums.BitMasked.StaticOps</code></p>
	 */
	public static final class StaticOps {
		/**
		 * Returns the bit mask for the passed enum ordinal
		 * @param member The enum member
		 * @return the bit mask for the passed member
		 */
		public static <E extends Enum<E>> int ordinalBitMaskInt(final E member) {
			return EnumSupport.getMask(member);
		}
		
		public static boolean isEnabled(final BitMasked member, final int mask) {
			return mask == (mask | member.getMask());
		}
		
		public static int enableFor(final BitMasked member, final int mask) {
			return mask | member.getMask();
		}
		
		public static int disableFor(final BitMasked member, final int mask) {
			return mask & ~member.getMask();
		}
		
		public static <E extends Enum<E> & BitMasked> Set<E> membersFor(final Class<E> type, final int mask) {
			final EnumSet<E> set = EnumSet.noneOf(type);			
			for(E e: type.getEnumConstants()) {
				if(e.isEnabled(mask)) set.add(e);
			}
			return set;
		}
		
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
		
		public static <E extends Enum<E> & BitMasked> int maskFor(final E...members) {
			int mask = 0;
			for(E e : members) {
				if(e==null) continue;
				mask = mask | e.getMask();
			}			
			return mask;
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
		
		public static <E extends Enum<E>> E[] makeArr(final Class<E> type, final String...memberNames) {
			if(memberNames.length==0) return makeArr(type, 0);
			final E[] arr = makeArr(type, memberNames.length);
			for(int i = 0; i < memberNames.length; i++) {				
				arr[i] = Enum.valueOf(type, memberNames[i].trim().toUpperCase());				
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
		
		@SuppressWarnings("unchecked")
		public static <E extends Enum<E>> E decode(final Class<E> type, final Object value) {
			if(type==null) throw new IllegalArgumentException("The passed enum type was null");
			if(value==null) return null;
			if(type.isInstance(value)) return (E)value;
			final E[] members = type.getEnumConstants(); 
			if(members.length==0) return null;
			final int maxIndex = members.length-1;
			if(Number.class.isInstance(value)) {
				final int index = ((Number)value).intValue();
				if(index < 0 || index > maxIndex) {
					throw new IllegalArgumentException("The supplied numeric value [" + value + "] was not a valid ordinal for enum [" + type.getName() + "]");
				}
				return members[((Number)value).intValue()];
			}
			
			String name = value.toString().trim();
			if(name.isEmpty()) return null;
			if(members[0].name().equals(members[0].name().toUpperCase())) {
				name = name.toUpperCase();
			}
			try {
				return Enum.valueOf(type, name);
			} catch (Exception ex) {
				try {
					final int index = Integer.parseInt(name);
					if(index < 0 || index > maxIndex) {
						throw new IllegalArgumentException("The supplied numeric value [" + value + "] was not a valid ordinal for enum [" + type.getName() + "]");
					}
					return members[index];
				} catch (Exception ex2) {
					throw new IllegalArgumentException("Failed to decode value [" + value + "] to a member of enum [" + type.getName() + "]");
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		public static <E extends Enum<E>> E[] decodeArr(final Class<E> type, final boolean skipInvalid, final Object...values) {
			if(type==null) throw new IllegalArgumentException("The passed enum type was null");
			if(values==null || values.length==0) return (E[]) Array.newInstance(type, 0);
			return decode(type, skipInvalid, Arrays.asList(values));
		}
		
		@SuppressWarnings("unchecked")
		public static <E extends Enum<E>> E[] decode(final Class<E> type, final boolean skipInvalid, final Collection<Object> values) {
			if(type==null) throw new IllegalArgumentException("The passed enum type was null");
			if(values==null || values.isEmpty()) return (E[]) Array.newInstance(type, 0);
			final Set<E> set = EnumSet.noneOf(type);
			for(final Object value: values) {
				try {
					set.add(decode(type, value));
				} catch (Exception ex) {
					if(!skipInvalid) throw new IllegalArgumentException("Failed to decode value [" + value + "]", ex);
				}
			}
			return set.toArray((E[]) Array.newInstance(type, 0));
		}
		

	}
	
}
