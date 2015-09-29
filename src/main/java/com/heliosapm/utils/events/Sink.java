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

import com.heliosapm.utils.enums.BitMasked;

/**
 * <p>Title: Sink</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.Sink</code></p>
 */

public interface Sink<E extends Enum<E> & BitMasked> extends Trigger<Void, E> {
	/**
	 * Returns the current state
	 * @return the current state
	 */
	public E getState();
	/**
	 * Returns the prior state
	 * @return the prior state
	 */
	public E getPriorState();
	/**
	 * Returns the timestamp in ms. of the last status change
	 * @return the timestamp in ms. of the last status change
	 */
	public long getLastStatusChange();
	
}
