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
package com.heliosapm.utils.collections;

import java.util.Map;

/**
 * <p>Title: MapAcceptor</p>
 * <p>Description: Defines a class that accepts a map. Intended to add functionality to do 
 * something with a built {@link FluentMap} at the end of a build chain.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.collections.MapAcceptor</code></p>
 * @param <K> The assumed key type
 * @param <V> The assumed value type
 */

public interface MapAcceptor<K, V> {
	/**
	 * Accepts a map 
	 * @param map The map to accept
	 */
	public void accept(Map<K, V> map);
}
