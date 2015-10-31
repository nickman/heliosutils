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
package javax.management.remote.jmxmp;

import javax.management.remote.JMXConnectorServerMBean;

/**
 * <p>Title: JMXMPConnectorServerMBean</p>
 * <p>Description: Instrumented extension for JMXMP connection server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>javax.management.remote.jmxmp.JMXMPConnectorServerMBean</code></p>
 */

public interface JMXMPConnectorServerMBean extends JMXConnectorServerMBean {
	/**
	 * Returns the total number of bytes read in 
	 * @return the total number of bytes read in
	 */
	public long getBytesIn();

	/**
	 * Returns the total number of bytes written out 
	 * @return the total number of bytes written out
	 */
	public long getBytesOut();
	
	/**
	 * Resets the IO counters
	 */
	public void resetStats();

}
