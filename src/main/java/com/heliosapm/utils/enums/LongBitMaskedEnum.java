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
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Title: LongBitMaskedEnum</p>
 * <p>Description: An enum overlay that supports the filtering and aggregation of members through long bit masks</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.enums.LongBitMaskedEnum</code></p>
 */

public class LongBitMaskedEnum<E extends Enum<E>> {
	/** A map of created IntBitMaskedEnums keyed by the enum class */
	private static final Map<Class<? extends Enum<?>>, LongBitMaskedEnum<? extends Enum<?>>> instances = new ConcurrentHashMap<Class<? extends Enum<?>>, LongBitMaskedEnum<? extends Enum<?>>>(56, 0.75f, Runtime.getRuntime().availableProcessors());
	
	/** The enum type */
	protected final Class<E> enumType;
	/** The enum members */
	protected final E[] members;
	/** An enum keyed map to lookup the mask value for each member */
	protected final Map<E, Long> maskMap;
	/** An enum member name (in upper) keyed map to lookup the member by upper name */
	protected final Map<String, E> nameMap;
	
	/** Empty array of members, constant */	
	public final E[] emptyArr;
	
	/** The maximum number of members supported */
	public static final int MAX_MEMBERS = Long.SIZE - 1;
	/** The first {@link #MAX_MEMBERS} power of twos */
	private static final long[] POW2 = new long[]{1L, 2L, 4L, 8L, 16L, 32L, 64L, 128L, 256L, 512L, 1024L, 2048L, 4096L, 8192L, 16384L, 32768L, 65536L, 131072L, 262144L, 524288L, 1048576L, 2097152L, 4194304L, 8388608L, 16777216L, 33554432L, 67108864L, 134217728L, 268435456L, 536870912L, 1073741824L, 2147483648L, 4294967296L, 8589934592L, 17179869184L, 34359738368L, 68719476736L, 137438953472L, 274877906944L, 549755813888L, 1099511627776L, 2199023255552L, 4398046511104L, 8796093022208L, 17592186044416L, 35184372088832L, 70368744177664L, 140737488355328L, 281474976710656L, 562949953421312L, 1125899906842624L, 2251799813685248L, 4503599627370496L, 9007199254740992L, 18014398509481984L, 36028797018963968L, 72057594037927936L, 144115188075855872L, 288230376151711744L, 576460752303423488L, 1152921504606846976L, 2305843009213693952L, 4611686018427387904L};
	
	
	/**
	 * Acquires the LongBitMaskedEnum singleton for the passed enum type
	 * @param enumType The enum type
	 * @return The LongBitMaskedEnum for the passed enum type
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static LongBitMaskedEnum<? extends Enum<?>> bitMaskedEnum(final Class<? extends Enum<?>> enumType) {
		if(enumType==null) throw new IllegalArgumentException("The passed enum type was null");
		LongBitMaskedEnum<? extends Enum<?>> x = instances.get(enumType);
		if(x==null) {
			synchronized(instances) {
				x = instances.get(enumType);
				if(x==null) {
					x = new LongBitMaskedEnum(enumType);
					instances.put(enumType, x);
				}
			}
		}
		return x;
	}

	
	/**
	 * Creates a new LongBitMaskedEnum
	 * @param enumType The enum type
	 */
	@SuppressWarnings("unchecked")
	private LongBitMaskedEnum(final Class<E> enumType) {
		if(enumType==null) throw new IllegalArgumentException("The passed enum type was null");
		this.enumType = enumType;
		members = enumType.getEnumConstants();
		if(members.length > MAX_MEMBERS) {
			throw new IllegalArgumentException("The enum type [" + enumType.getName() + "] has [" + members.length + "] but IntBitMaskedEnums can only support [" + MAX_MEMBERS + "]");
		}
		EnumMap<E, Long> _maskMap = new EnumMap<E, Long>(enumType);
		HashMap<String, E> _nameMap = new HashMap<String, E>(members.length); 
		for(int i = 0; i < members.length; i++ ) {
			_nameMap.put(members[i].name().toUpperCase(), members[i]);
			_maskMap.put(members[i], POW2[i]);
		}
		maskMap = Collections.unmodifiableMap(_maskMap);		
		nameMap = Collections.unmodifiableMap(_nameMap);
		emptyArr = (E[]) Array.newInstance(enumType, 0);
	}

	/**
	 * Returns the mask for the passed member
	 * @param member The member to get the mask for
	 * @return the mask
	 */
	public long mask(final E member) {
		if(member==null) throw new IllegalArgumentException("The passed enum member was null");
		return maskMap.get(member);
	}

	
	/**
	 * Determines if the passed mask is enabled for the passed member's mask
	 * @param member The member 
	 * @param mask The mask to test
	 * @return true if enabled, false otherwise
	 */
	public boolean isEnabled(final E member, long mask) {
		if(member==null) throw new IllegalArgumentException("The passed enum member was null");
		return mask == (mask | mask(member));
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
	
	/**
	 * Returns an array of all the enum members enabled for the passed bitmask
	 * @param bitMask The bitmask to filter the enum members by
	 * @return a possibly zero length array of enum members
	 */
	public E[] matching(final long bitMask) {
		final EnumSet<E> matched = EnumSet.noneOf(enumType);
		for(E e: members) {
			if(isEnabled(e, bitMask)) {
				matched.add(e);
			}
		}
		return matched.toArray(emptyArr);
	}
	
	/**
	 * Returns a bitmask enabled for the passed enum members
	 * @param members The memebers to get a bit mask for
	 * @return the bitmask
	 */
	@SuppressWarnings("unchecked")
	public long bitMaskFor(final E...members) {
		if(members==null || members.length==0) return 0;
		long mask = 0;
		for(E e: members) {			
			mask = mask | mask(e);
		}
		return mask;
	}
	
	/**
	 * Determines if the passed bitmask is enabled for all passed enum members
	 * @param bitMask The bitmask to test
	 * @param members The members to test for
	 * @return true if the bitmask is enabled for all members, false otherwise
	 */
	@SuppressWarnings("unchecked")
	public boolean isEnabledForAll(final long bitMask, final E...members) {
		if(members==null || members.length==0) return true;
		for(E e: members) {
			if(!isEnabled(e, bitMask)) return false;
		}
		return true;
	}
	
	/**
	 * Determines if the passed bitmask is enabled for any of the passed enum members
	 * @param bitMask The bitmask to test
	 * @param members The members to test for
	 * @return true if the bitmask is enabled for any of the members, false otherwise
	 */
	@SuppressWarnings("unchecked")
	public boolean isEnabledForAny(final long bitMask, final E...members) {
		if(members==null || members.length==0) return false;
		for(E e: members) {
			if(isEnabled(e, bitMask)) return true;
		}
		return false;
	}
	
	/**
	 * Enables the passed bitmask for the passed enum members
	 * @param bitMask The bitmask to start with
	 * @param members The members to enable the bitmask for
	 * @return the [possibly unmodified] bitmask
	 */
	@SuppressWarnings("unchecked")
	public long enableFor(final long bitMask, final E...members) {
		if(members==null || members.length==0) return bitMask;
		long x = bitMask;
		for(E e: members) {
			x = x | mask(e);
		}
		return x;
	}
	
	/**
	 * Disables the passed bitmask for the passed enum members
	 * @param bitMask The bitmask to start with
	 * @param members The members to disable the bitmask for
	 * @return the [possibly unmodified] bitmask
	 */
	@SuppressWarnings("unchecked")
	public long disableFor(final long bitMask, final E...members) {
		if(members==null || members.length==0) return bitMask;
		long x = bitMask;
		for(E e: members) {
			x = x & ~mask(e);
		}
		return x;
	}
	
	

}
