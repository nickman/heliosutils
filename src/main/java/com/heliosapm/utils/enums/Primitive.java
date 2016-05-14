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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.heliosapm.utils.unsafe.UnsafeAdapter;

/**
 * <p>Title: Primitive</p>
 * <p>Description: A simple enum of all primitive numerics</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.enums.Primitive</code></p>
 */

public enum Primitive {
	/** A boolean primitive */
	BOOLEAN(8, Boolean.TYPE, Boolean.class, boolean[].class, false, false),
	/** A byte primitive */
	BYTE(Byte.SIZE, Byte.TYPE, Byte.class, byte[].class, true, false),
	/** A char primitive */
	CHAR(2, Character.TYPE, Character.class, char[].class, false, false),	
	/** A short primitive */
	SHORT(Short.SIZE, Short.TYPE, Short.class, short[].class, true, false),
	/** An integer primitive */
	INTEGER(Integer.SIZE, Integer.TYPE, Integer.class, int[].class, true, false),
	/** A float primitive */
	FLOAT(Float.SIZE, Float.TYPE, Float.class, float[].class, true, true),
	/** A long primitive */
	LONG(Long.SIZE, Long.TYPE, Long.class, long[].class, true, false),
	/** A double primitive */
	DOUBLE(Double.SIZE, Double.TYPE, Double.class, double[].class, true, true);
	
	public static final Set<Class<?>> ALL_CLASSES;
	public static final Set<String> ALL_CLASS_NAMES;
	public static final Set<Class<?>> ALL_NUMERIC_CLASSES;
	public static final Set<String> ALL_NUMERIC_CLASS_NAMES;
//	public static final Map<String, Primitive> NAME2ENUM;
	
	static {
		final Primitive[] values = values();
		final Set<Class<?>> allClasses = new HashSet<Class<?>>(values.length*2);
		final Set<Class<?>> allNumericClasses = new HashSet<Class<?>>(values.length*2 + 4);
		final Set<Class<?>> allNumericPrimitives = new HashSet<Class<?>>(values.length-2);
		final Set<Class<?>> allNumericObjects = new HashSet<Class<?>>(values.length-2+4);
		final Set<String> allClassNames = new HashSet<String>(values.length*2);
		final Set<String> allNumericClassNames = new HashSet<String>(values.length*2 + 4);
		
		for(Primitive p: values) {
			allClasses.add(p.type);
			allClassNames.add(p.type.getName());
			if(p.numeric) {
				allNumericClasses.add(p.upcast);
				allNumericClassNames.add(p.upcast.getName());	
				allNumericPrimitives.add(p.type);
				allNumericObjects.add(p.upcast);
			}
		}
		allNumericClasses.add(AtomicInteger.class);
		allNumericClasses.add(AtomicLong.class);
		allNumericClasses.add(BigInteger.class);
		allNumericClasses.add(BigDecimal.class);
		allNumericClassNames.add(AtomicInteger.class.getName());
		allNumericClassNames.add(AtomicLong.class.getName());
		allNumericClassNames.add(BigInteger.class.getName());
		allNumericClassNames.add(BigDecimal.class.getName());		
		allNumericObjects.add(AtomicInteger.class);
		allNumericObjects.add(AtomicLong.class);
		allNumericObjects.add(BigInteger.class);
		allNumericObjects.add(BigDecimal.class);

		
		
		ALL_CLASSES = Collections.unmodifiableSet(allClasses);
		ALL_CLASS_NAMES = Collections.unmodifiableSet(allClassNames);
		ALL_NUMERIC_CLASSES = Collections.unmodifiableSet(allNumericClasses);
		ALL_NUMERIC_CLASS_NAMES = Collections.unmodifiableSet(allNumericClassNames);
		
	}
	
	
	private Primitive(final int size, final Class<?> type, final Class<?> upcast, final Class<?> arrayType, final boolean numeric, final boolean floating) {
		this.size = size/8;
		this.type = type;
		this.upcast = upcast;
		this.arrayType = arrayType;
		this.numeric = numeric;
		this.floating = floating;
		addressOffset = UnsafeAdapter.arrayBaseOffset(arrayType); 
	}
	
	/**
	 * Converts the passed numbers to a primitive array for the passed primitive enum member
	 * @param primitive The Primitive enum member
	 * @param defaultValues The numbers to convert
	 * @return a primitive array
	 */
	public static Object toPrimitiveArray(Primitive primitive, Number...defaultValues) {
		Object arr = Array.newInstance(primitive.type, defaultValues.length);
		for(int i = 0; i < defaultValues.length; i++) {
			Array.set(arr, i, defaultValues[i]);
		}
		return arr;
	}
	
	/**
	 * Allocates a primitive array for this primitive type
	 * @param size The size of the array
	 * @return the allocated array
	 */
	public Object allocateArray(int size) {
		return Array.newInstance(type, size);
	}
	
	public Number[] convert(Object array) {
		int size = Array.getLength(array);
		Number[] nums = new Number[size];
		for(int i = 0; i < size; i++) {
			nums[i] = (Number)Array.get(array, i);
		}
		return nums;
	}
	
	
	
	public static void main(String[] args) {
		log("Primitive Test");
		for(Primitive p: Primitive.values()) {
			log(String.format("\t[%s] size:%s type:%s array:%s  array-offset:%s", p.name(), p.size, p.type.getName(), p.arrayType.getName(), p.addressOffset));
		}	
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	
	/** The byte size of this primitive */
	public final int size;
	/** The type of the primitive */
	public final Class<?> type;
	/** The type of the primitive 1 diensional array */
	public final Class<?> arrayType;
	/** The offset size to the data in a primitive 1 diensional array */
	public final long addressOffset;
	/** The upcast for this primitive */
	public final Class<?> upcast;
	/** Indicates if the type is numeric */
	public final boolean numeric;
	/** Indicates if the type is numeric and floating */
	public final boolean floating;

}
