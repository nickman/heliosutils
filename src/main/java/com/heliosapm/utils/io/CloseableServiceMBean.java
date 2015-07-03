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
package com.heliosapm.utils.io;

import java.io.Closeable;

/**
 * <p>Title: CloseableServiceMBean</p>
 * <p>Description: JMX MBean interface for {@link CloseableService}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.io.CloseableServiceMBean</code></p>
 */

public interface CloseableServiceMBean {
	
	/**
	 * Returns the number of registered closeables
	 * @return the number of registered closeables
	 */
	public int getCloseableCount();
	
	/**
	 * Registers a new closeable
	 * @param closeable the closeable to register
	 */
	public void register(final Closeable closeable);
	
	/**
	 * Closes all the registered closeables and clears the tracking set
	 */
	public void closeAll();
	
	/**
	 * Closes all the registered closeables and clears the tracking set
	 * @param self If true, unregisters the MBean when done with the closing
	 */
	public void closeAll(final boolean self);
	
	/**
	 * Returns the cumulative count of successful closes
	 * @return the cumulative count of successful closes
	 */
	public long getGoodCloses();
	/**
	 * Returns the cumulative count of failed closes
	 * @return the cumulative count of failed closes
	 */
	public long getFailedCloses();

}
