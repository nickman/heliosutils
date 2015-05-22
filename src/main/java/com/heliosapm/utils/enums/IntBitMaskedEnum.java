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

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Title: IntBitMaskedEnum</p>
 * <p>Description: An enum overlay that supports the filtering and aggregation of members through int bit masks</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.enums.IntBitMaskedEnum</code></p>
 * @param <E> The bit mask supporting enum type
 */

public class IntBitMaskedEnum<E extends Enum<E>>  extends EnumSupport<E> {
	
	/** A map of created IntBitMaskedEnums keyed by the enum class */
	private static final Map<Class<? extends Enum<?>>, IntBitMaskedEnum<? extends Enum<?>>> instances = new ConcurrentHashMap<Class<? extends Enum<?>>, IntBitMaskedEnum<? extends Enum<?>>>(56, 0.75f, Runtime.getRuntime().availableProcessors());
	
	/** An enum keyed map to lookup the mask value for each member */
	protected final Map<E, Integer> maskMap;
	
	
	/** The maximum number of members supported */
	public static final int MAX_MEMBERS = Integer.SIZE - 1;
	/** The first {@link #MAX_MEMBERS} power of twos */
	private static final int[] POW2 = new int[]{1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824};

	/**
	 * Acquires the IntBitMaskedEnum singleton for the passed enum type
	 * @param enumType The enum type
	 * @return The IntBitMaskedEnum for the passed enum type
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })	
	public static IntBitMaskedEnum<? extends Enum<?>> bitMaskedEnum(final Class<? extends Enum<?>> enumType) {
		if(enumType==null) throw new IllegalArgumentException("The passed enum type was null");
		IntBitMaskedEnum<? extends Enum<?>> x = instances.get(enumType);
		if(x==null) {
			synchronized(instances) {
				x = instances.get(enumType);
				if(x==null) {
					x = new IntBitMaskedEnum(enumType);
					instances.put(enumType, x);
				}
			}
		}
		return x;
	}

	
	/**
	 * Creates a new IntBitMaskedEnum
	 * @param enumType The enum type
	 */
	
	private IntBitMaskedEnum(final Class<E> enumType) {
		super(enumType);
		if(members.length > MAX_MEMBERS) {
			throw new IllegalArgumentException("The enum type [" + enumType.getName() + "] has [" + members.length + "] but IntBitMaskedEnums can only support [" + MAX_MEMBERS + "]");
		}
		EnumMap<E, Integer> _maskMap = new EnumMap<E, Integer>(enumType);
		for(int i = 0; i < members.length; i++ ) {
			_maskMap.put(members[i], POW2[i]);
		}
		maskMap = Collections.unmodifiableMap(_maskMap);		
	}

	
	/**
	 * Returns the mask for the passed member
	 * @param member The member to get the mask for
	 * @return the mask
	 */
	public int mask(final E member) {
		if(member==null) throw new IllegalArgumentException("The passed enum member was null");
		return maskMap.get(member);
	}

	
	/**
	 * Determines if the passed mask is enabled for the passed member's mask
	 * @param member The member 
	 * @param mask The mask to test
	 * @return true if enabled, false otherwise
	 */
	public boolean isEnabled(final E member, int mask) {
		if(member==null) throw new IllegalArgumentException("The passed enum member was null");
		return mask == (mask | mask(member));
	}
	
	/**
	 * Returns an array of all the enum members enabled for the passed bitmask
	 * @param bitMask The bitmask to filter the enum members by
	 * @return a possibly zero length array of enum members
	 */
	public E[] matching(final int bitMask) {
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
	public int bitMaskFor(final E...members) {
		if(members==null || members.length==0) return 0;
		int mask = 0;
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
	public boolean isEnabledForAll(final int bitMask, final E...members) {
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
	public boolean isEnabledForAny(final int bitMask, final E...members) {
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
	public int enableFor(final int bitMask, final E...members) {
		if(members==null || members.length==0) return bitMask;
		int x = bitMask;
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
	public int disableFor(final int bitMask, final E...members) {
		if(members==null || members.length==0) return bitMask;
		int x = bitMask;
		for(E e: members) {
			x = x & ~mask(e);
		}
		return x;
	}
	
	

}
