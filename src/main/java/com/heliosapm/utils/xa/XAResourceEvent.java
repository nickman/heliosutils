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
package com.heliosapm.utils.xa;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: XAResourceEvent</p>
 * <p>Description: Decodes for {@link javax.transaction.xa.XAResource} events</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.xa.XAResourceEvent</code></p>
 */

public enum XAResourceEvent {
	/** XAResource Code for TMENDRSCAN */
	TMENDRSCAN(8388608, "Ends a recovery scan."),
	/** XAResource Code for TMFAIL */
	TMFAIL(536870912, "Disassociates the caller and marks the transaction branch rollback-only."),
	/** XAResource Code for TMJOIN */
	TMJOIN(2097152, "Caller is joining existing transaction branch."),
	/** XAResource Code for TMNOFLAGS */
	TMNOFLAGS(0, "Use TMNOFLAGS to indicate no flags value is selected."),
	/** XAResource Code for TMONEPHASE */
	TMONEPHASE(1073741824, "Caller is using one-phase optimization."),
	/** XAResource Code for TMRESUME */
	TMRESUME(134217728, "Caller is resuming association with a suspended transaction branch."),
	/** XAResource Code for TMSTARTRSCAN */
	TMSTARTRSCAN(16777216, "Starts a recovery scan."),
	/** XAResource Code for TMSUCCESS */
	TMSUCCESS(67108864, "Disassociates caller from a transaction branch."),
	/** XAResource Code for TMSUSPEND */
	TMSUSPEND(33554432, "Caller is suspending (not ending) its association with a transaction branch."),
	/** XAResource Code for XA_RDONLY */
	XA_RDONLY(3, "The transaction branch has been read-only and has been committed."),
	/** XAResource Code for XA_OK */
	XA_OK(0, "The transaction work has been prepared normally."),
	/** XAResource Event for an invalid code */
	XA_INVALID(-1, "Invalid XAResource code.");
	
	
	private XAResourceEvent(final int code, final String description) {
		this.code = code;
		this.description = description;
	}
	
	public static final Map<Integer, XAResourceEvent> CODE2ENUM;
	
	static {
		final XAResourceEvent[] values = values();
		Map<Integer, XAResourceEvent> tmp = new HashMap<Integer, XAResourceEvent>(values.length);
		for(XAResourceEvent xar: values) {
			tmp.put(xar.code, xar);
		}
		CODE2ENUM = Collections.unmodifiableMap(tmp);
	}
	
	/** The XAResource code */
	public final int code;
	/** The code description */
	public final String description;
	
	/**
	 * Returns the XAResourceEvent decode for the passed code.
	 * @param c The code to decode
	 * @return The decoded XAResourceEvent or {@link #XA_INVALID} 
	 * if the passed code could not be decoded. 
	 */
	public static XAResourceEvent decode(final int c) {
		final XAResourceEvent xar = CODE2ENUM.get(c);
		return xar==null ? XA_INVALID : xar;
	}
	
	
}
