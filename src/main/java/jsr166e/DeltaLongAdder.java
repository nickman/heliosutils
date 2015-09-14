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
package jsr166e;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: DeltaLongAdder</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>jsr166e.DeltaLongAdder</code></p>
 */

public class DeltaLongAdder extends LongAdder {
	protected final AtomicLong last = new AtomicLong(0);
	/**
	 * Creates a new DeltaLongAdder
	 */
	public DeltaLongAdder() {
		super();
	}
	
	public long getDelta() {
		final long d = longValue();
		final long state = last.getAndSet(d);
		return d - state;		
	}

}
