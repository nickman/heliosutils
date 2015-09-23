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
 * <p>Title: ConsecutiveUntilResetEventsTrigger</p>
 * <p>Description: Consecutive event trigger that does not wind down and alarms until the consecutive streak ends</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.counters.alarm.ConsecutiveUntilResetEventsTrigger</code></p>
 */

public class ConsecutiveUntilResetEventsTrigger<E extends Enum<E> & BitMasked> extends AbstractConsecutiveEventsTrigger<E> {

	/**
	 * Creates a new ConsecutiveUntilResetEventsTrigger
	 * @param consec The threshold number of consecutive events that fires the trigger
	 * @param states The states that increment the count of consecutives
	 */
	public ConsecutiveUntilResetEventsTrigger(final long consec, final E... states) {
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
		ctr.decrementAndGet();
	}

}
