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
package com.heliosapm.utils.events;

import java.util.concurrent.atomic.AtomicLong;

import com.heliosapm.utils.enums.BitMasked;
import com.heliosapm.utils.unsafe.UnsafeAdapter;

/**
 * <p>Title: AbstractConsecutiveEventTrigger</p>
 * <p>Description: A trigger that fires on the receipt of a specified number of consecutive events</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.AbstractConsecutiveEventTrigger</code></p>
 * @param <E> The bit masked event type
 */

public abstract class AbstractConsecutiveEventTrigger<E extends Enum<E> & BitMasked> implements Trigger<Boolean, E> {
	/** The alarmable event counter */
	protected final AtomicLong ctr = new AtomicLong();	
	/** The threshold number of consecutive events that fires the trigger */
	protected final long consec;
	/** The bit mask of the event types that increment the counter */
	protected final int alertMask;	
	/** Spin lock around the counter */
	protected final UnsafeAdapter.SpinLock lock = UnsafeAdapter.allocateSpinLock();
	
	/**
	 * Creates a new ConsecutiveEventsTrigger
	 * @param consec The threshold number of consecutive events that fires the trigger
	 * @param states The states that increment the count of consecutives
	 */
	public AbstractConsecutiveEventTrigger(final long consec, final E...states) {
		this.consec = consec;
		this.alertMask = BitMasked.StaticOps.maskFor(states);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.events.Trigger#event(com.heliosapm.utils.counters.alarm.AlarmState)
	 */
	@Override
	public Boolean event(final E state) {
		if(state.isEnabled(alertMask)) {
			try {
				lock.xlock();
				final long id = ctr.incrementAndGet();
				if(id>=consec) {
					windDown();
					return true;
				}
			} finally {
				lock.xunlock();
			}
		} else {
			if(lock.isLocked()){
				reset();
			}
		}
		return false;
	}

	protected abstract void windDown();

}
