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

/**
 * <p>Title: BitMasked</p>
 * <p>Description: Defines the basic bit maked enums operations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.enums.BitMasked</code></p>
 */

public interface BitMasked {
	/** An all zeroes bit mask template */
	public static final String INTBITS = "0000000000000000000000000000000000000000000000000000000000000000";

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
		public static <E extends Enum<E>> int ordinalBitMaskInt(final Enum<E> member) {
			return Integer.parseInt("1" + INTBITS.substring(0, member.ordinal()), 2);
		}
		
		
	}
	
	
}
