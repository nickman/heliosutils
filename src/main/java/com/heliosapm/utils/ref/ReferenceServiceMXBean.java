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

import java.util.Map;

/**
 * <p>Title: ReferenceServiceMXBean</p>
 * <p>Description: JMX MBean interface for {@link ReferenceService}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ref.ReferenceServiceMXBean</code></p>
 */

public interface ReferenceServiceMXBean {
	/**
	 * Returns the length of the reference queue
	 * @return the length of the reference queue
	 */
	public long getQueueDepth();
	
  /**
   * Returns the number of pending soft references
   * @return the number of pending soft references
   */
  public int getMappedSoftRefCount();
  /**
   * Returns the number of pending wsofteak references
   * @return the number of pending weak references
   */
  public int getMappedWeakRefCount();
  /**
   * Returns the number of pending phantom references
   * @return the number of pending phantom references
   */
  public int getMappedPhantomRefCount();
	
	/**
	 * Returns the total number of cleared reference executions since the last reset
	 * @return the total number of cleared reference executions
	 */
	public long getClearedRefCount();
	
	/**
	 * Returns the count of registered but uncleared references
	 * @return the count of registered but uncleared references
	 */
	public long getUnClearedRefCount();
	
	/**
	 * Resets the service stats
	 */
	public void resetStats();
	
	/**
	 * Returns the last computed average.
	 * @return the last computed average 
	 */
	public double getAverage();

	/**
	 * Returns the minimum recorded value since the last reset
	 * @return the minimum recorded value 
	 */
	public double getMinimum();

	/**
	 * Returns the maximum recorded value since the last reset
	 * @return the maximum recorded value 
	 */
	public double getMaximum();

	
	/**
	 * Returns the count of recorded values since the last reset
	 * @return the count of recorded values 
	 */
	public long getCount();
	
	/**
	 * Returns the count of errors since the last reset
	 * @return the count of errors 
	 */
	public long getErrors();
	
	/**
	 * Returns a map of the counts of cleared references by reference type name
	 * @return a map of the counts of cleared references by reference type name
	 */
	public ReferenceTypeCountMBean[] getCountsByTypes();
	
	public Map<String, ReferenceTypeCountMBean> getCountTT();
}

