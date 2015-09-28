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
import com.heliosapm.utils.enums.Rollup;
import com.heliosapm.utils.enums.RollupType;

/**
 * <p>Title: AlarmState</p>
 * <p>Description: Functional enumeration for alarm states</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.counters.alarm.AlarmState</code></p>
 */
@RollupType(Rollup.DOWN)
public enum AlarmState implements BitMasked  {
	/** The alarm has been disabled */
	OFF(false),		
	/** No data has been submitted to the alarm yet */
	NODATA(false),	
	/** State is stale as no data has been received */
	STALE(false),	
	/** State is normal */
	OK(true),
	/** State is iffy */
	WARN(true),
	/** State is critical */
	CRITICAL(true);
	
	private AlarmState(final boolean evaluatable) {
		this.evaluatable = evaluatable;
	}
	
	private static final AlarmState[] EVAL = new AlarmState[]{OK, WARN, CRITICAL};
	
	public final boolean evaluatable;
	
	/**
	 * Returns an array of the evaluatable alarm states
	 * @return an array of the evaluatable alarm states
	 */
	public static AlarmState[] evaluatable() {
		return EVAL.clone();
	}
	
	
	final int mask = StaticOps.ordinalBitMaskInt(this);
	public int getMask(){ return mask; }
	public boolean isEnabled(final int mask){ return StaticOps.isEnabled(this, mask); }			
	public int enableFor(final int mask) { return StaticOps.enableFor(this, mask); }
	public int disableFor(final int mask) { return StaticOps.disableFor(this, mask); }	
	

	public static int maskFor(final AlarmState... members) { return StaticOps.maskFor(members); }
	public static Set<AlarmState> membersFor(final int mask) { return StaticOps.membersFor(AlarmState.class, mask); };

}
