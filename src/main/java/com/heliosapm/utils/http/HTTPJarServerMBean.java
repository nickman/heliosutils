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
package com.heliosapm.utils.http;

import java.util.HashMap;

/**
 * <p>Title: HTTPJarServerMBean</p>
 * <p>Description: JMX MBean interface for the {@link HTTPJarServer}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.http.HTTPJarServerMBean</code></p>
 */

public interface HTTPJarServerMBean {
	/**
	 * Returns the available JAR paths
	 * @return the available JAR paths
	 */
	public String[] getAvailablePaths();
	
	/**
	 * Returns the listening port
	 * @return the listening port
	 */
	public int getPort();
	
	/**
	 * Stops this server and resets the singleton reference 
	 */
	public void stop();
	
	/**
	 * Returns the number of bytes transmitted
	 * @return the number of bytes transmitted
	 */
	public long getBytesTransmitted();
	
	/**
	 * Returns the cummulative number of expiries since the last reset
	 * @return the cummulative number of expiries since the last reset
	 */
	public long getExpiryCount();
	
	/**
	 * Returns the cummulative number of completions since the last reset
	 * @return the cummulative number of completions since the last reset
	 */
	public long getCompletionCount();	
	
	/**
	 * Returns the number of served content buffers by content name
	 * @return the number of served content buffers by content name
	 */
	public HashMap<String, Long> getServedCounts();	
	/**
	 * Resets the stats
	 */
	public void resetStats();
}
