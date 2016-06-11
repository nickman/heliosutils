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
 * <p>Title: XAExceptionCode</p>
 * <p>Description: Decodes for {@link javax.transaction.xa.XAException} codes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.xa.XAExceptionCode</code></p>
 */

public enum XAExceptionCode {
	/** XAException Code for XA_RBBASE */
	XA_RBBASE(100, "The inclusive lower bound of the rollback codes."),
	/** XAException Code for XA_RBROLLBACK */
	XA_RBROLLBACK(100, "Indicates that the rollback was caused by an unspecified reason."),
	/** XAException Code for XA_RBCOMMFAIL */
	XA_RBCOMMFAIL(101, "Indicates that the rollback was caused by a communication failure."),
	/** XAException Code for XA_RBDEADLOCK */
	XA_RBDEADLOCK(102, "A deadlock was detected."),
	/** XAException Code for XA_RBINTEGRITY */
	XA_RBINTEGRITY(103, "A condition that violates the integrity of the resource was detected."),
	/** XAException Code for XA_RBOTHER */
	XA_RBOTHER(104, "The resource manager rolled back the transaction branch for a reason not on this list."),
	/** XAException Code for XA_RBPROTO */
	XA_RBPROTO(105, "A protocol error occurred in the resource manager."),
	/** XAException Code for XA_RBTIMEOUT */
	XA_RBTIMEOUT(106, "A transaction branch took too long."),
	/** XAException Code for XA_RBTRANSIENT */
	XA_RBTRANSIENT(107, "May retry the transaction branch."),
	/** XAException Code for XA_RBEND */
	XA_RBEND(107, "The inclusive upper bound of the rollback error code."),
	/** XAException Code for XA_NOMIGRATE */
	XA_NOMIGRATE(9, "Resumption must occur where the suspension occurred."),
	/** XAException Code for XA_HEURHAZ */
	XA_HEURHAZ(8, "The transaction branch may have been heuristically completed."),
	/** XAException Code for XA_HEURCOM */
	XA_HEURCOM(7, "The transaction branch has been heuristically committed."),
	/** XAException Code for XA_HEURRB */
	XA_HEURRB(6, "The transaction branch has been heuristically rolled back."),
	/** XAException Code for XA_HEURMIX */
	XA_HEURMIX(5, "The transaction branch has been heuristically committed and rolled back."),
	/** XAException Code for XA_RETRY */
	XA_RETRY(4, "Routine returned with no effect and may be reissued."),
	/** XAException Code for XA_RDONLY */
	XA_RDONLY(3, "The transaction branch was read-only and has been committed."),
	/** XAException Code for XAER_ASYNC */
	XAER_ASYNC(-2, "There is an asynchronous operation already outstanding."),
	/** XAException Code for XAER_RMERR */
	XAER_RMERR(-3, "A resource manager error has occurred in the transaction branch."),
	/** XAException Code for XAER_NOTA */
	XAER_NOTA(-4, "The XID is not valid."),
	/** XAException Code for XAER_INVAL */
	XAER_INVAL(-5, "Invalid arguments were given."),
	/** XAException Code for XAER_PROTO */
	XAER_PROTO(-6, "Routine was invoked in an improper context."),
	/** XAException Code for XAER_RMFAIL */
	XAER_RMFAIL(-7, "Resource manager is unavailable."),
	/** XAException Code for XAER_DUPID */
	XAER_DUPID(-8, "The XID already exists."),
	/** XAException Code for XAER_OUTSIDE */
	XAER_OUTSIDE(-9, "The resource manager is doing work outside a global transaction."),
	/** XAException Code for an invalid code */
	XA_INVALID(-999, "Invalid XAException code.");
	
	private XAExceptionCode(final int code, final String description) {
		this.code = code;
		this.description = description;
	}
	
	public static final Map<Integer, XAExceptionCode> CODE2ENUM;
	
	static {
		final XAExceptionCode[] values = values();
		Map<Integer, XAExceptionCode> tmp = new HashMap<Integer, XAExceptionCode>(values.length);
		for(XAExceptionCode xar: values) {
			tmp.put(xar.code, xar);
		}
		CODE2ENUM = Collections.unmodifiableMap(tmp);
	}
	
	/** The XAResource code */
	public final int code;
	/** The code description */
	public final String description;
	
	
	/**
	 * Returns the XAExceptionCode decode for the passed code.
	 * @param c The code to decode
	 * @return The decoded XAExceptionCode or {@link #XA_INVALID} 
	 * if the passed code could not be decoded. 
	 */
	public static XAExceptionCode decode(final int c) {
		final XAExceptionCode xar = CODE2ENUM.get(c);
		return xar==null ? XA_INVALID : xar;
	}
	
/*
ORA 3113

XAException.XAER_RMFAIL

ORA 3114

XAException.XAER_RMFAIL

ORA 24756

XAException.XAER_NOTA

ORA 24764

XAException.XA_HEURCOM

ORA 24765

XAException.XA_HEURRB

ORA 24766

XAException.XA_HEURMIX

ORA 24767

XAException.XA_RDONLY

ORA 25351

XAException.XA_RETRY

all other ORA errors

XAException.XAER_RMERR


 */
}
