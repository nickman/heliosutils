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
		
		public static int getMaskFor()
	}
	
	
}
