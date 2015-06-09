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
package com.heliosapm.utils.reflect;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

/**
 * <p>Title: SModifier</p>
 * <p>Description: Enum version of java's reflection Modifier class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.reflect.SModifier</code></p>
 */

public enum SModifier {
	/** The public modifier */
	PUBLIC(Modifier.PUBLIC),
	/** The private modifier */
	PRIVATE(Modifier.PRIVATE),
	/** The protected modifier */
	PROTECTED(Modifier.PROTECTED),
	/** The static modifier */
	STATIC(Modifier.STATIC),
	/** The final modifier */
	FINAL(Modifier.FINAL),
	/** The synchronized modifier */
	SYNCHRONIZED(Modifier.SYNCHRONIZED),
	/** The volatile modifier */
	VOLATILE(Modifier.VOLATILE),
	/** The transient modifier */
	TRANSIENT(Modifier.TRANSIENT),
	/** The native modifier */
	NATIVE(Modifier.NATIVE),
	/** The interface modifier */
	INTERFACE(Modifier.INTERFACE),
	/** The abstract modifier */
	ABSTRACT(Modifier.ABSTRACT),
	/** The strict modifier */
	STRICT(Modifier.STRICT);
	
	private SModifier(final int mask) {
		_mask = mask;
	}
	
	/** The modifier mask */
	public final int _mask;
	
	/**
	 * Returns a mask enabled for all the passed modifiers
	 * @param modifiers The modifiers to get a mask for
	 * @return the mask
	 */
	public static int getMaskFor(final SModifier...modifiers) {
		int mask = 0;
		for(SModifier m: modifiers) {
			if(m==null) continue;
			mask = mask | m._mask;
		}
		return mask;
	}
	
	/**
	 * Determines if the passed member is enabled for all the passed modifiers
	 * @param member The member to test
	 * @param modifiers The modifiers to test for
	 * @return true if the passed member is enabled for all the passed modifiers, false otherwise
	 */
	public static boolean isEnabledForAll(final Member member, final SModifier...modifiers) {
		if(member==null) throw new IllegalArgumentException("The passed member was null");
		final int mmask = member.getModifiers();
		for(SModifier m: modifiers) {
			if(!m.isEnabled(mmask)) return false;
		}
		return true;
	}
	
	/**
	 * Determines if the passed member is enabled for any of the passed modifiers
	 * @param member The member to test
	 * @param modifiers The modifiers to test for
	 * @return true if the passed member is enabled for any of the passed modifiers, false otherwise
	 */
	public static boolean isEnabledForAny(final Member member, final SModifier...modifiers) {
		if(member==null) throw new IllegalArgumentException("The passed member was null");
		final int mmask = member.getModifiers();
		for(SModifier m: modifiers) {
			if(!m.isEnabled(mmask)) return true;
		}
		return false;
	}
	
	
	/**
	 * Determines if the passed mask is enabled for this SModifier
	 * @param mask The mask to test
	 * @return true if enabled, false otherwise
	 */
	public boolean isEnabled(final int mask) {
		return mask == (mask | _mask);
	}
	
	/**
	 * Determines if the passed member is enabled for this SModifier
	 * @param member The member to test
	 * @return true if enabled, false otherwise
	 */
	public boolean isEnabled(final Member member) {
		final int m = member.getModifiers();
		return m == (m | _mask);
	}
	
	
	
}
