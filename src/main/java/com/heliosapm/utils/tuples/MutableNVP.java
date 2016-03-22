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
package com.heliosapm.utils.tuples;

/**
 * <p>Title: MutableNVP</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.tuples.MutableNVP</code></p>
 * @param <K> The key type
 * @param <V> The key value
 */

public class MutableNVP<K, V> extends NVP<K, V> {

	/**
	 * Creates a new MutableNVP
	 * @param key The NVP key
	 * @param value The NVP value
	 */
	public MutableNVP(final K key, final V value) {
		super(key, value);		
	}
	
	/**
	 * Creates a new MutableNVP
	 */
	public MutableNVP() {
		super(null, null);		
	}
	
	
	
	/**
	 * Sets a new key
	 * @param newKey The new key
	 */
	public void setKey(final K newKey) {
		this.key = newKey;
	}
	
	/**
	 * Sets a new value
	 * @param newValue The new value
	 */
	public void setValue(final V newValue) {
		this.value = newValue;
	}
	
	/**
	 * Sets the key/value pair
	 * @param key The NVP key
	 * @param value The NVP value
	 * @return this NVP
	 */
	public NVP<K,V> set(final K key, final V value) {
		this.key = key;
		this.value = value;		
		return this;
	}
	
	
}
