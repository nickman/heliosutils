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

import com.heliosapm.utils.enums.BitMasked;

/**
 * <p>Title: ResettingConsecutiveEventsTrigger</p>
 * <p>Description: Consecutive event trigger that resets the counter once the configured number of consecutive events have been received.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.counters.alarm.ResettingConsecutiveEventsTrigger</code></p>
 */

public class ResettingConsecutiveEventsTrigger<E extends Enum<E> & BitMasked> extends AbstractConsecutiveEventsTrigger<E> {

	/**
	 * Creates a new ResettingConsecutiveEventsTrigger
	 * @param consec The threshold number of consecutive events that fires the trigger
	 * @param states The states that increment the count of consecutives
	 */
	public ResettingConsecutiveEventsTrigger(final long consec, final E... states) {
		super(consec, states);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.counters.alarm.Trigger#reset()
	 */
	@Override
	public void reset() {
		ctr.set(0);

	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.counters.alarm.AbstractConsecutiveEventsTrigger#windDown()
	 */
	@Override
	protected void windDown() {
		ctr.set(0);
	}

}
