/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
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
 * <p>Title: IntBitMaskedEnum</p>
 * <p>Description: An enum overlay that supports the filtering and aggregation of members through int bit masks</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.enums.IntBitMaskedEnum</code></p>
 * @param <E> The bit mask supporting enum type
 */

public class IntBitMaskedEnum<E extends Enum<E>> {
	
	/** A map of created IntBitMaskedEnums keyed by the enum class */
	private static final Map<Class<? extends Enum<?>>, IntBitMaskedEnum<? extends Enum<?>>> instances = new ConcurrentHashMap<Class<? extends Enum<?>>, IntBitMaskedEnum<? extends Enum<?>>>(56, 0.75f, Runtime.getRuntime().availableProcessors());
	
	/** The enum type */
	protected final Class<E> enumType;
	/** The enum members */
	protected final E[] members;
	/** An enum keyed map to lookup the mask value for each member */
	protected final Map<E, Integer> maskMap;
	/** An enum member name (in upper) keyed map to lookup the member by upper name */
	protected final Map<String, E> nameMap;
	
	/** Empty array of members, constant */	
	public final E[] emptyArr;
	
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
	@SuppressWarnings("unchecked")
	private IntBitMaskedEnum(final Class<E> enumType) {
		if(enumType==null) throw new IllegalArgumentException("The passed enum type was null");
		this.enumType = enumType;
		members = enumType.getEnumConstants();
		if(members.length > MAX_MEMBERS) {
			throw new IllegalArgumentException("The enum type [" + enumType.getName() + "] has [" + members.length + "] but IntBitMaskedEnums can only support [" + MAX_MEMBERS + "]");
		}
		EnumMap<E, Integer> _maskMap = new EnumMap<E, Integer>(enumType);
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
