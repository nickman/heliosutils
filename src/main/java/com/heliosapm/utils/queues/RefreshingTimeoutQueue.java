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
package com.heliosapm.utils.queues;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: RefreshingTimeoutQueue</p>
 * <p>Description: Timeout queue that assists in refreshing the timeout period of queued items.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.queues.RefreshingTimeoutQueue</code></p>
 */

public class RefreshingTimeoutQueue<T> {

	/**
	 * Creates a new RefreshingTimeoutQueue
	 */
	public RefreshingTimeoutQueue() {
		// TODO Auto-generated constructor stub
	}
	
	class QueueKey<T> implements Delayed {

		@Override
		public int compareTo(final Delayed otherDelayed) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public long getDelay(final TimeUnit unit) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	
}
