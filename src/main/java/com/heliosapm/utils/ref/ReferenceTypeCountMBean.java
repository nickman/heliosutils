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
package com.heliosapm.utils.ref;

/**
 * <p>Title: ReferenceTypeCountMBean</p>
 * <p>Description: MXBean interface for the ReferenceService ref type count</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ref.ReferenceTypeCountMBean</code></p>
 */

public interface ReferenceTypeCountMBean {
	/**
	 * Returns the name of the cleared reference type
	 * @return the name of the cleared reference type
	 */
	public String getName();
	/**
	 * The count of cleared references of the associated type, since the last reset
	 * @return the count of cleared references
	 */
	public long getLong();
	
	/**
	 * Increments the cleared reference count
	 */
	public void increment();
	
	/**
	 * Sets the count to zero
	 */
	public void reset();

}
