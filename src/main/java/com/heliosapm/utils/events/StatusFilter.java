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

import java.util.Collection;

import com.heliosapm.utils.enums.BitMasked;
import com.heliosapm.utils.events.EventSeries.EventSampleFilter;

/**
 * <p>Title: StatusFilter</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.StatusFilter</code></p>
 * @param <T> The sample value type
 * @param <E> The event type
 */

public class StatusFilter<T, E extends Enum<E> & BitMasked> implements EventSampleFilter<T, E> {
	protected final E eventType;
	/**
	 * Creates a new StatusFilter
	 */
	public StatusFilter(final E eventType) {
		this.eventType = eventType;
	}
	@Override
	public boolean filter(EventSeries<T, E>.EventSample sample) {
		// TODO Auto-generated method stub
		return false;
	}


}
