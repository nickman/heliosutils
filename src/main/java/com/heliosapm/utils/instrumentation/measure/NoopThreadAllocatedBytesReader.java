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
package com.heliosapm.utils.instrumentation.measure;

import java.util.Arrays;

/**
 * <p>Title: NoopThreadAllocatedBytesReader</p>
 * <p>Description: A No Op ThreadAllocatedBytesReader implementation that returns -1.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.instrumentation.measure.NoopThreadAllocatedBytesReader</code></p>
 */

public class NoopThreadAllocatedBytesReader implements ThreadAllocatedBytesReader {


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.measure.ThreadAllocatedBytesReader#getThreadAllocatedBytes(long)
	 */
	@Override
	public long getThreadAllocatedBytes(final long id) {
		return -1L;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.measure.ThreadAllocatedBytesReader#getThreadAllocatedBytes(long[])
	 */
	@Override
	public long[] getThreadAllocatedBytes(final long[] ids) {
		final long[] dup = ids.clone();
		Arrays.fill(dup, -1L);
		return dup;
	}

}
