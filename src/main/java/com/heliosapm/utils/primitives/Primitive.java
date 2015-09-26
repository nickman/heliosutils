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
package com.heliosapm.utils.primitives;

/**
 * <p>Title: Primitive</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.primitives.Primitive</code></p>
 */

public enum Primitive {
	VOID(void.class, Void.class, (byte)-1),
	BYTE(byte.class, Byte.class, (byte)1),
	BOOLEAN(boolean.class, Boolean.class, (byte)1),
	CHAR(char.class, Character.class, (byte)2),
	SHORT(short.class, Short.class, (byte)2),
	INT(int.class, Integer.class, (byte)4),
	FLOAT(float.class, Float.class, (byte)4),
	LONG(long.class, Long.class, (byte)8),
	DOUBLE(double.class, Double.class, (byte)8);
	
	
	
	/**
	 * Creates a new Primitive
	 * @param primitiveType
	 * @param objectType
	 * @param number
	 * @param bordinal
	 * @param nsize
	 */
	private Primitive(Class<?> primitiveType, Class<?> objectType, byte nsize) {
		this.primitiveType = primitiveType;
		this.objectType = objectType;
		this.number = (Number.class.isAssignableFrom(objectType));
		this.bordinal = (byte)ordinal();
		this.nsize = nsize;
	}
	final Class<?> primitiveType;
	final Class<?> objectType;
	final boolean number;
	final byte bordinal;
	final byte nsize;
}
