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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * <p>Title: FluentMap</p>
 * <p>Description: A fluent style map with put operations that return the map instance put into.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.collections.FluentMap</code></p>
 * @param <K> The assumed key type
 * @param <V> The assumed value type
 */
@SuppressWarnings({"rawtypes", "unchecked"}) 
public class FluentMap<K, V> implements Map<K, V> {
	/** The internal delegate map instance */
	final Map<K,V> instance;
	/** The internal map type */
	final MapType type;
	
	/** The default map type */
	public static final MapType DEFAULT_TYPE = MapType.LINK;
	
	/**
	 * <p>Title: MapCreator</p>
	 * <p>Description: Defines the {@link MapType} operations to support map creation</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.collections.FluentMap.MapCreator</code></p>
	 */
	interface MapCreator<K, V> {
		/**
		 * Creates a map of a specific type
		 * @return the map
		 */
		public Map<K, V> createMap();
		/**
		 * Determines if the passed map is of this implementation's {@link MapType}
		 * @param map The map to test
		 * @return true if the map is of the target implementation, false if not or the map was null
		 */
		public boolean isInstanceOf(Class<? extends Map> map);
	}
	
	/**
	 * <p>Title: MapType</p>
	 * <p>Description: A functional enum for creating map instances of different types</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.collections.FluentMap.MapType</code></p>
	 */
	static enum MapType implements MapCreator {
		/** Creates {@link LinkedHashMap}s */
		LINK(){@Override
			public Map createMap() {
				return new LinkedHashMap();
			}
		  @Override
			public boolean isInstanceOf(final Class map) {
		  	if(map==null) return false;
				return LinkedHashMap.class.isInstance(map);
			}		
		
		},
		/** Creates {@link HashMap}s */
		HASH(){@Override
			public Map createMap() {
				return new HashMap();
			}	
		  @Override
			public boolean isInstanceOf(final Class map) {
		  	if(map==null) return false;
				return HashMap.class.isInstance(map);
			}
		},
		/** Creates {@link TreeMap}s */
		TREE(){@Override
			public Map createMap() {
				return new TreeMap();
			}
		  @Override
			public boolean isInstanceOf(final Class map) {
		  	if(map==null) return false;
				return TreeMap.class.isInstance(map);
			}		
		},
		/** Creates {@link WeakHashMap}s */
		WEAK(){@Override
			public Map createMap() {
				return new WeakHashMap();
			}
		  @Override
			public boolean isInstanceOf(final Class map) {
		  	if(map==null) return false;
				return WeakHashMap.class.isInstance(map);
			}				
		},
		/** Creates {@link IdentityHashMap}s */
		ID(){@Override
			public Map createMap() {
				return new IdentityHashMap();
			}
		  @Override
			public boolean isInstanceOf(final Class map) {
		  	if(map==null) return false;
				return IdentityHashMap.class.isInstance(map);
			}				
		},
		/** Creates {@link ConcurrentHashMap}s */
		CHASH(){@Override
			public Map createMap() {
				return new ConcurrentHashMap();
			}
		  @Override
			public boolean isInstanceOf(final Class map) {
		  	if(map==null) return false;
				return ConcurrentHashMap.class.isInstance(map);
			}				
		},
		/** Creates {@link ConcurrentSkipListMap}s */
		CSKP(){@Override
			public Map createMap() {
				return new ConcurrentSkipListMap();
			}
		  @Override
			public boolean isInstanceOf(final Class map) {
		  	if(map==null) return false;
				return ConcurrentSkipListMap.class.isInstance(map);
			}				
		}	
	}
	
	
	/**
	 * creates a new fluent map of the passed type
	 * @param mapType The map type. A hashmap will be returned if null
	 * @return the map
	 */
	public static <K, V> FluentMap<K, V> newMap(final MapType mapType) {
		return new FluentMap(mapType);
	}

	/**
	 * creates a new fluent hash map.
	 * @param keyType The key type
	 * @return the map The value type
	 */
	public static <K, V> FluentMap<K, V> newMap(Class<K> keyType, Class<V> valueType) {
		return newMap(null);
	}

	private FluentMap(final MapType type) {
		if(type==null) {
			this.type = MapType.HASH;			
		} else {
			this.type = type;
		}
		instance = this.type.createMap();
	}
	
	/**
	 * Creates a new FluentMap from the passed map 
	 * @param map The map to load
	 */
	private FluentMap(final Map map) {
		if(map==null) throw new IllegalArgumentException("The passed map was null");
		if(map instanceof FluentMap) {
			type = ((FluentMap)map).type;
		} else {
			type = MapType.HASH;
		}
		instance = type.createMap();
		instance.putAll(map);
	}
	
	private FluentMap(final Map map, final MapType type) {
		this.instance = map;
		this.type = type;
	}
	
	/**
	 * Returns a new synchronized version of this map
	 * @return a new synchronized map disconnected from this map but containing all it's values
	 * @see Collections#synchronizedMap(Map)
	 */
	public FluentMap<K, V> synchro() {		
		return new FluentMap<K, V>(Collections.synchronizedMap(instance), this.type);
	}
	
	/**
	 * Returns a new read only version of this map
	 * @return a new read only map disconnected from this map but containing all it's values
	 * @see Collections#unmodifiableMap(Map)
	 */
	public FluentMap<K, V> readOnly() {		
		return new FluentMap<K, V>(Collections.unmodifiableMap(instance), this.type);
	}
	
	/**
	 * Returns a new dynamically typesafe version of this map
	 * @param keyType the type of key that m is permitted to hold
	 * @param valueType the type of value that m is permitted to hold
	 * @return a new dynamically typesafe map disconnected from this map but containing all it's values
	 * @see Collections#checkedMap(Map, Class, Class)
	 */
	public FluentMap<K, V> checked(final Class<K> keyType, final Class<V> valueType) {
		return new FluentMap<K, V>(Collections.checkedMap(instance, keyType, valueType), this.type);
	}
	
	
	/**
	 * Fluent put. Puts the passed key/value and returns this instance
	 * @param key The key to put
	 * @param value The value to put
	 * @return this map
	 */
	public FluentMap<K, V> fput(final K key, final V value) {
		instance.put(key, value);
		return this;
	}
	
	/**
	 * Fluent remove. Removes the passed key and returns this instance
	 * @param key The key to remove
	 * @return this map
	 */
	public FluentMap<K, V> fremove(final Object key) {
		remove(key);
		return this;
	}
	
	/**
	 * Fluent putAll. Puts all the key/values from the passed map and returns this instance
	 * @param m The map to putAll from
	 * @return this map
	 */
	public FluentMap<K, V> fputAll(final Map<? extends K, ? extends V> m) {
		putAll(m);
		return this;
	}
	
	/**
	 * Passes this map to the passed {@link MapAcceptor}
	 * @param acceptor The acceptor call site
	 * @return this map
	 */
	public FluentMap<K, V> accept(final MapAcceptor<K, V> acceptor) {
		if(acceptor==null) throw new IllegalArgumentException("The passed acceptor was null");
		acceptor.accept(this);
		return this;
	}
	
	// ======================================================================================
	//		Standard Map Ops
	//======================================================================================
	
  /**
   * Returns the number of key-value mappings in this map.  If the
   * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
   * <tt>Integer.MAX_VALUE</tt>.
   *
   * @return the number of key-value mappings in this map
   */
  @Override
	public int size() {
  	return instance.size();
  }

  /**
   * Returns <tt>true</tt> if this map contains no key-value mappings.
   *
   * @return <tt>true</tt> if this map contains no key-value mappings
   */
  @Override
	public boolean isEmpty() {
  	return instance.isEmpty();
  }

  /**
   * Returns <tt>true</tt> if this map contains a mapping for the specified
   * key.  More formally, returns <tt>true</tt> if and only if
   * this map contains a mapping for a key <tt>k</tt> such that
   * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
   * at most one such mapping.)
   *
   * @param key key whose presence in this map is to be tested
   * @return <tt>true</tt> if this map contains a mapping for the specified
   *         key
   * @throws ClassCastException if the key is of an inappropriate type for
   *         this map
   * (<a href="Collection.html#optional-restrictions">optional</a>)
   * @throws NullPointerException if the specified key is null and this map
   *         does not permit null keys
   * (<a href="Collection.html#optional-restrictions">optional</a>)
   */
  @Override
	public boolean containsKey(Object key) {
  	return instance.containsKey(key);
  }

  /**
   * Returns <tt>true</tt> if this map maps one or more keys to the
   * specified value.  More formally, returns <tt>true</tt> if and only if
   * this map contains at least one mapping to a value <tt>v</tt> such that
   * <tt>(value==null ? v==null : value.equals(v))</tt>.  This operation
   * will probably require time linear in the map size for most
   * implementations of the <tt>Map</tt> interface.
   *
   * @param value value whose presence in this map is to be tested
   * @return <tt>true</tt> if this map maps one or more keys to the
   *         specified value
   * @throws ClassCastException if the value is of an inappropriate type for
   *         this map
   * (<a href="Collection.html#optional-restrictions">optional</a>)
   * @throws NullPointerException if the specified value is null and this
   *         map does not permit null values
   * (<a href="Collection.html#optional-restrictions">optional</a>)
   */
  @Override
	public boolean containsValue(Object value) {
  	return instance.containsValue(value);
  }

  /**
   * Returns the value to which the specified key is mapped,
   * or {@code null} if this map contains no mapping for the key.
   *
   * <p>More formally, if this map contains a mapping from a key
   * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
   * key.equals(k))}, then this method returns {@code v}; otherwise
   * it returns {@code null}.  (There can be at most one such mapping.)
   *
   * <p>If this map permits null values, then a return value of
   * {@code null} does not <i>necessarily</i> indicate that the map
   * contains no mapping for the key; it's also possible that the map
   * explicitly maps the key to {@code null}.  The {@link #containsKey
   * containsKey} operation may be used to distinguish these two cases.
   *
   * @param key the key whose associated value is to be returned
   * @return the value to which the specified key is mapped, or
   *         {@code null} if this map contains no mapping for the key
   * @throws ClassCastException if the key is of an inappropriate type for
   *         this map
   * (<a href="Collection.html#optional-restrictions">optional</a>)
   * @throws NullPointerException if the specified key is null and this map
   *         does not permit null keys
   * (<a href="Collection.html#optional-restrictions">optional</a>)
   */
  @Override
	public V get(Object key) {
  	return instance.get(key);
  }

  // Modification Operations

  /**
   * Associates the specified value with the specified key in this map
   * (optional operation).  If the map previously contained a mapping for
   * the key, the old value is replaced by the specified value.  (A map
   * <tt>m</tt> is said to contain a mapping for a key <tt>k</tt> if and only
   * if {@link #containsKey(Object) m.containsKey(k)} would return
   * <tt>true</tt>.)
   *
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return the previous value associated with <tt>key</tt>, or
   *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
   *         (A <tt>null</tt> return can also indicate that the map
   *         previously associated <tt>null</tt> with <tt>key</tt>,
   *         if the implementation supports <tt>null</tt> values.)
   * @throws UnsupportedOperationException if the <tt>put</tt> operation
   *         is not supported by this map
   * @throws ClassCastException if the class of the specified key or value
   *         prevents it from being stored in this map
   * @throws NullPointerException if the specified key or value is null
   *         and this map does not permit null keys or values
   * @throws IllegalArgumentException if some property of the specified key
   *         or value prevents it from being stored in this map
   */
  @Override
	public V put(K key, V value) {
  	return instance.put(key, value);
  }

  /**
   * Removes the mapping for a key from this map if it is present
   * (optional operation).   More formally, if this map contains a mapping
   * from key <tt>k</tt> to value <tt>v</tt> such that
   * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping
   * is removed.  (The map can contain at most one such mapping.)
   *
   * <p>Returns the value to which this map previously associated the key,
   * or <tt>null</tt> if the map contained no mapping for the key.
   *
   * <p>If this map permits null values, then a return value of
   * <tt>null</tt> does not <i>necessarily</i> indicate that the map
   * contained no mapping for the key; it's also possible that the map
   * explicitly mapped the key to <tt>null</tt>.
   *
   * <p>The map will not contain a mapping for the specified key once the
   * call returns.
   *
   * @param key key whose mapping is to be removed from the map
   * @return the previous value associated with <tt>key</tt>, or
   *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
   * @throws UnsupportedOperationException if the <tt>remove</tt> operation
   *         is not supported by this map
   * @throws ClassCastException if the key is of an inappropriate type for
   *         this map
   * (<a href="Collection.html#optional-restrictions">optional</a>)
   * @throws NullPointerException if the specified key is null and this
   *         map does not permit null keys
   * (<a href="Collection.html#optional-restrictions">optional</a>)
   */
  @Override
	public V remove(Object key) {
  	return instance.remove(key);
  }


  // Bulk Operations

  /**
   * Copies all of the mappings from the specified map to this map
   * (optional operation).  The effect of this call is equivalent to that
   * of calling {@link #put(Object,Object) put(k, v)} on this map once
   * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the
   * specified map.  The behavior of this operation is undefined if the
   * specified map is modified while the operation is in progress.
   *
   * @param m mappings to be stored in this map
   * @throws UnsupportedOperationException if the <tt>putAll</tt> operation
   *         is not supported by this map
   * @throws ClassCastException if the class of a key or value in the
   *         specified map prevents it from being stored in this map
   * @throws NullPointerException if the specified map is null, or if
   *         this map does not permit null keys or values, and the
   *         specified map contains null keys or values
   * @throws IllegalArgumentException if some property of a key or value in
   *         the specified map prevents it from being stored in this map
   */
  @Override
	public void putAll(Map<? extends K, ? extends V> m) {
  	instance.putAll(m);
  }

  /**
   * Removes all of the mappings from this map (optional operation).
   * The map will be empty after this call returns.
   *
   * @throws UnsupportedOperationException if the <tt>clear</tt> operation
   *         is not supported by this map
   */
  @Override
	public void clear() {
  	instance.clear();
  }


  // Views

  /**
   * Returns a {@link Set} view of the keys contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa.  If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own <tt>remove</tt> operation), the results of
   * the iteration are undefined.  The set supports element removal,
   * which removes the corresponding mapping from the map, via the
   * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
   * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
   * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
   * operations.
   *
   * @return a set view of the keys contained in this map
   */
  @Override
	public Set<K> keySet() {
  	return instance.keySet();
  }

  /**
   * Returns a {@link Collection} view of the values contained in this map.
   * The collection is backed by the map, so changes to the map are
   * reflected in the collection, and vice-versa.  If the map is
   * modified while an iteration over the collection is in progress
   * (except through the iterator's own <tt>remove</tt> operation),
   * the results of the iteration are undefined.  The collection
   * supports element removal, which removes the corresponding
   * mapping from the map, via the <tt>Iterator.remove</tt>,
   * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
   * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
   * support the <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @return a collection view of the values contained in this map
   */
  @Override
	public Collection<V> values() {
  	return instance.values();
  }

  /**
   * Returns a {@link Set} view of the mappings contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa.  If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own <tt>remove</tt> operation, or through the
   * <tt>setValue</tt> operation on a map entry returned by the
   * iterator) the results of the iteration are undefined.  The set
   * supports element removal, which removes the corresponding
   * mapping from the map, via the <tt>Iterator.remove</tt>,
   * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
   * <tt>clear</tt> operations.  It does not support the
   * <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @return a set view of the mappings contained in this map
   */
  @Override
	public Set<Map.Entry<K, V>> entrySet() {
  	return instance.entrySet();
  }

	


  /**
   * Indicates whether some other object is "equal to" this one.
   * <p>
   * The {@code equals} method implements an equivalence relation
   * on non-null object references:
   * <ul>
   * <li>It is <i>reflexive</i>: for any non-null reference value
   *     {@code x}, {@code x.equals(x)} should return
   *     {@code true}.
   * <li>It is <i>symmetric</i>: for any non-null reference values
   *     {@code x} and {@code y}, {@code x.equals(y)}
   *     should return {@code true} if and only if
   *     {@code y.equals(x)} returns {@code true}.
   * <li>It is <i>transitive</i>: for any non-null reference values
   *     {@code x}, {@code y}, and {@code z}, if
   *     {@code x.equals(y)} returns {@code true} and
   *     {@code y.equals(z)} returns {@code true}, then
   *     {@code x.equals(z)} should return {@code true}.
   * <li>It is <i>consistent</i>: for any non-null reference values
   *     {@code x} and {@code y}, multiple invocations of
   *     {@code x.equals(y)} consistently return {@code true}
   *     or consistently return {@code false}, provided no
   *     information used in {@code equals} comparisons on the
   *     objects is modified.
   * <li>For any non-null reference value {@code x},
   *     {@code x.equals(null)} should return {@code false}.
   * </ul>
   * <p>
   * The {@code equals} method for class {@code Object} implements
   * the most discriminating possible equivalence relation on objects;
   * that is, for any non-null reference values {@code x} and
   * {@code y}, this method returns {@code true} if and only
   * if {@code x} and {@code y} refer to the same object
   * ({@code x == y} has the value {@code true}).
   * <p>
   * Note that it is generally necessary to override the {@code hashCode}
   * method whenever this method is overridden, so as to maintain the
   * general contract for the {@code hashCode} method, which states
   * that equal objects must have equal hash codes.
   *
   * @param   obj   the reference object with which to compare.
   * @return  {@code true} if this object is the same as the obj
   *          argument; {@code false} otherwise.
   * @see     #hashCode()
   * @see     java.util.HashMap
   */
	@Override
	public boolean equals(Object obj) {
		return instance.equals(obj);
	}

  /**
   * Returns a hash code value for the object. This method is
   * supported for the benefit of hash tables such as those provided by
   * {@link java.util.HashMap}.
   * <p>
   * The general contract of {@code hashCode} is:
   * <ul>
   * <li>Whenever it is invoked on the same object more than once during
   *     an execution of a Java application, the {@code hashCode} method
   *     must consistently return the same integer, provided no information
   *     used in {@code equals} comparisons on the object is modified.
   *     This integer need not remain consistent from one execution of an
   *     application to another execution of the same application.
   * <li>If two objects are equal according to the {@code equals(Object)}
   *     method, then calling the {@code hashCode} method on each of
   *     the two objects must produce the same integer result.
   * <li>It is <em>not</em> required that if two objects are unequal
   *     according to the {@link java.lang.Object#equals(java.lang.Object)}
   *     method, then calling the {@code hashCode} method on each of the
   *     two objects must produce distinct integer results.  However, the
   *     programmer should be aware that producing distinct integer results
   *     for unequal objects may improve the performance of hash tables.
   * </ul>
   * <p>
   * As much as is reasonably practical, the hashCode method defined by
   * class {@code Object} does return distinct integers for distinct
   * objects. (This is typically implemented by converting the internal
   * address of the object into an integer, but this implementation
   * technique is not required by the
   * Java<font size="-2"><sup>TM</sup></font> programming language.)
   *
   * @return  a hash code value for this object.
   * @see     java.lang.Object#equals(java.lang.Object)
   * @see     java.lang.System#identityHashCode
   */
	@Override
	public int hashCode() {
		return instance.hashCode();
	}


}
