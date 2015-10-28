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
package com.heliosapm.utils.jmx.bulk;

import java.util.Map;

import javax.management.ObjectName;
import javax.management.QueryExp;

/**
 * <p>Title: BulkJMXServiceMBean</p>
 * <p>Description: Simple MBean interface for the BulkJMXService</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.bulk.BulkJMXServiceMBean</code></p>
 */

public interface BulkJMXServiceMBean {
	/**
	 * Ping op
	 * @return the installed system's current UTC time
	 */
	public long ping();
	
	/**
	 * Bulk attribute lookup
	 * @param lookups A map of attribute names to lookup keyed by the [optionally pattern based] ObjectName of the MBeans to look them up from
	 * @return A map of key/value attributes keyed by the absolute ObjectName of the MBean they were read from
	 */
	public Map<ObjectName, Map<String, Object>> getAttributes(final Map<ObjectName, String[]> lookups);
	
	/**
	 * Locates all registered MBeans matching the criteria defined by the passed ObjectName and query
	 * and returns a bulk value map for all through {@link #getAttributes(Map)}.
	 * @param on The object name to match
	 * @param query The query to match
	 * @param attrNames The attribute names of the attributes to retrieve
	 * @return A map of key/value attributes keyed by the absolute ObjectName of the MBean they were read from
	 */
	public Map<ObjectName, Map<String, Object>> getAttributes(final ObjectName on, final QueryExp query, final String[] attrNames);
	
	/**
	 * Locates all registered MBeans matching the criteria defined by the passed ObjectName and query
	 * and returns a compressed byte array containing the serialized bulk value map for all through {@link #getCompressedAttributes(Map)}.
	 * @param on The object name to match
	 * @param query The query to match
	 * @param attrNames The attribute names of the attributes to retrieve
	 * @return A map of key/value attributes keyed by the absolute ObjectName of the MBean they were read from
	 * serialized into a compressed byte array
	 */
	public byte[] getCompressedAttributes(final ObjectName on, final QueryExp query, final String[] attrNames);
	
	/**
	 * Invokes the specified operation on all MBeans with ObjectNames matching the passed object name and query.
	 * The results are returned as values in a map keyed by absolute object name if the result was not null.
	 * @param on The object name
	 * @param query The query
	 * @param opName The operation name to invoke
	 * @param signature The signature of the target op
	 * @param args The arguments to pass in the invocation
	 * @return a map of invocation responses keyed by the absolute ObjectName
	 */
	public Map<ObjectName, Object> invoke(final ObjectName on, final QueryExp query, final String opName, final String[] signature, final Object...args);

	
	/**
	 * Bulk attribute lookup with response compression. Same op as {@link #getAttributes(Map)} and
	 * returns the result serialized into a compressed byte array
	 * @param lookups A map of attribute names to lookup keyed by the [optionally pattern based] ObjectName of the MBeans to look them up from
	 * @return A map of key/value attributes keyed by the absolute ObjectName of the MBean they were read from
	 * serialized into a compressed byte array
	 */
	public byte[] getCompressedAttributes(final Map<ObjectName, String[]> lookups);
	
	/**
	 * Returns the number of cached MBean attribute name arrays
	 * @return the number of cached MBean attribute name arrays
	 */
	public int getCachedAttributeNames();

}
