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
package com.heliosapm.utils.collections;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: TypeFriendlyMap</p>
 * <p>Description: A String/Object hashmap equipped with some type narrowing getters
 * (because casting looks ugly)</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.collections.TypeFriendlyMap</code></p>
 * @param <K> The type of the keys for this map
 */

public class TypeFriendlyMap<K> extends HashMap<K, Object> {
	

	/**  */
	private static final long serialVersionUID = 2455989126339984662L;

	/**
	 * Returns the keyed value as a boolean
	 * @param key The key
	 * @param defaultValue the value to return if no value is bound
	 * @return the value
	 */
	public boolean getBoolean(final K key, final boolean defaultValue) {
		if(key==null) throw new IllegalArgumentException("The passed key was null");
		final Object o = get(key);
		if(o==null) return defaultValue;
		return ((Boolean)o).booleanValue();
	}
	
	/**
	 * Returns the keyed value as a char
	 * @param key The key
	 * @param defaultValue the value to return if no value is bound
	 * @return the value
	 */
	public char getChar(final K key, final char defaultValue) {
		if(key==null) throw new IllegalArgumentException("The passed key was null");
		final Object o = get(key);
		if(o==null) return defaultValue;
		return ((Character)o).charValue();
	}
	
	/**
	 * Returns the keyed value as a Number
	 * @param key The key
	 * @param defaultValue the value to return if no value is bound
	 * @return the value
	 */
	public Number getNumber(final K key, final Number defaultValue) {
		if(key==null) throw new IllegalArgumentException("The passed key was null");
		final Object o = get(key);
		if(o==null) {
			if(defaultValue==null) throw new IllegalArgumentException("The passed default value for key [" + key + "] was null, and we needed it");
			return defaultValue;
		}
		return (Number)o;
	}
	
	
	
	/**
	 * Returns the keyed value as a byte
	 * @param key The key
	 * @param defaultValue the value to return if no value is bound
	 * @return the value
	 */
	public byte get(final K key, final byte defaultValue) {
		if(key==null) throw new IllegalArgumentException("The passed key was null");
		return getNumber(key, defaultValue).byteValue();
	}

	/**
	 * Returns the keyed value as a short
	 * @param key The key
	 * @param defaultValue the value to return if no value is bound
	 * @return the value
	 */
	public short get(final K key, final short defaultValue) {
		if(key==null) throw new IllegalArgumentException("The passed key was null");
		return getNumber(key, defaultValue).shortValue();
	}
	
	/**
	 * Returns the keyed value as an int
	 * @param key The key
	 * @param defaultValue the value to return if no value is bound
	 * @return the value
	 */
	public int get(final K key, final int defaultValue) {
		if(key==null) throw new IllegalArgumentException("The passed key was null");
		return getNumber(key, defaultValue).intValue();
	}
	
	/**
	 * Returns the keyed value as a float
	 * @param key The key
	 * @param defaultValue the value to return if no value is bound
	 * @return the value
	 */
	public float get(final K key, final float defaultValue) {
		if(key==null) throw new IllegalArgumentException("The passed key was null");
		return getNumber(key, defaultValue).floatValue();
	}
	
	/**
	 * Returns the keyed value as a double
	 * @param key The key
	 * @param defaultValue the value to return if no value is bound
	 * @return the value
	 */
	public double get(final K key, final double defaultValue) {
		if(key==null) throw new IllegalArgumentException("The passed key was null");
		return getNumber(key, defaultValue).doubleValue();
	}
	
	/**
	 * Returns the keyed value as a long
	 * @param key The key
	 * @param defaultValue the value to return if no value is bound
	 * @return the value
	 */
	public long get(final K key, final long defaultValue) {
		if(key==null) throw new IllegalArgumentException("The passed key was null");
		return getNumber(key, defaultValue).longValue();
	}
	
	/**
	 * Returns the keyed value as the type specified
	 * @param key The key
	 * @param type the type of the expected return value
	 * @param defaultValue The value to return if no value is bound
	 * @return the value
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(final K key, final Class<T> type, final T defaultValue) {
		if(key==null) throw new IllegalArgumentException("The passed key was null");
		if(type==null) throw new IllegalArgumentException("The passed type was null");
		final Object o = get(key);
		if(o==null) return defaultValue;
		return (T)o;
	}
	
	/**
	 * Returns the keyed value as the type specified
	 * @param key The key
	 * @param type the type of the expected return value
	 * @return the value
	 */
	public <T> T get(final K key, final Class<T> type) {
		return get(key, type, null);
	}
	

	/**
	 * Creates a new TypeFriendlyMap
	 */
	public TypeFriendlyMap() {
	}

	/**
	 * Creates a new TypeFriendlyMap
	 * @param initialCapacity The initial capacity
	 */
	public TypeFriendlyMap(final int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new TypeFriendlyMap
	 * @param that Loads this map with that map
	 */
	public TypeFriendlyMap(final Map<K, ? extends Object> that) {
		super(that);
	}

	/**
	 * Creates a new TypeFriendlyMap
	 * @param initialCapacity The initial capacity
	 * @param loadFactor The load factor
	 */
	public TypeFriendlyMap(final int initialCapacity, final float loadFactor) {
		super(initialCapacity, loadFactor);
	}

}
