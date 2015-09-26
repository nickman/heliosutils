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
package com.heliosapm.utils.counters.alarm;

import java.util.Set;

import com.heliosapm.utils.enums.BitMasked;

/**
 * <p>Title: AlarmState</p>
 * <p>Description: Functional enumeration for alarm states</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.counters.alarm.AlarmState</code></p>
 */

public enum AlarmState implements BitMasked  {
	/** No data has been submitted to the alarm yet */
	NODATA,
	/** State is normal */
	OK,
	/** State is iffy */
	WARN,
	/** State is critical */
	CRITICAL,
	/** State is stale as no data has been received */
	STALE;
	
	final int mask = StaticOps.ordinalBitMaskInt(this);
	public int getMask(){ return mask; }
	public boolean isEnabled(final int mask){ return StaticOps.isEnabled(this, mask); }			
	public int enableFor(final int mask) { return StaticOps.enableFor(this, mask); }
	public int disableFor(final int mask) { return StaticOps.disableFor(this, mask); }	
	

	public static int maskFor(final AlarmState... members) { return StaticOps.maskFor(members); }
	public static Set<AlarmState> membersFor(final int mask) { return StaticOps.membersFor(AlarmState.class, mask); };

}
