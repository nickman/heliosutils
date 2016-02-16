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

/**
 * <p>Title: ThreadAllocatedBytesReader</p>
 * <p>Description: Defines a class that can determines the number of bytes allocated by a thread. 
 * A concrete implementation may or may not exist in any given JVM</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.instrumentation.measure.ThreadAllocatedBytesReader</code></p>
 */

public interface ThreadAllocatedBytesReader {
	
	/**
	 * Returns the number of bytes allocated by a thread
	 * @param id The id of the thread
	 * @return the total number of bytes allocated
	 */
	public long getThreadAllocatedBytes(final long id);
	
	/**
	 * Returns the number of bytes allocated by the specified threads
	 * @param ids The ids of the threads
	 * @return the total number of bytes allocated for each specified thread
	 */
	public long[] getThreadAllocatedBytes(final long[] ids);
}
