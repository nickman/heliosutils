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
package com.heliosapm.utils.arrays;

import jsr166y.ThreadLocalRandom;

/**
 * <p>Title: ArrayHelper</p>
 * <p>Description: Some static array helper functions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.arrays.ArrayHelper</code></p>
 */

public abstract class ArrayHelper {
	
	private static final ThreadLocalRandom random = ThreadLocalRandom.current();

	/**
	 * Shuffles an array
	 * @param array The array to shuffle
	 * @return the shuffled array
	 */
	public static int[] shuffle(final int[] array){
		int randomPosition = -1;
		int temp = -1;
		for (int i=0; i<array.length; i++) {
				randomPosition = random.nextInt(array.length);
		    temp = array[i];
		    array[i] = array[randomPosition];
		    array[randomPosition] = temp;
		}
		return array;
	}
	
	/**
	 * Shuffles an array
	 * @param array The array to shuffle
	 * @return the shuffled array
	 */
	public static long[] shuffle(final long[] array){
		int randomPosition = -1;
		long temp = -1L;
		for (int i=0; i<array.length; i++) {
				randomPosition = random.nextInt(array.length);
		    temp = array[i];
		    array[i] = array[randomPosition];
		    array[randomPosition] = temp;
		}
		return array;
	}
	
	
	private ArrayHelper() {}

}
