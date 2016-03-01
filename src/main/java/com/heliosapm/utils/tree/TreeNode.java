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
package com.heliosapm.utils.tree;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: TreeNode</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.tree.TreeNode</code></p>
 */

public class TreeNode<K, V> {
	protected K key;
	protected final Map<K, TreeNode<K, V>> children = new HashMap<K, TreeNode<K, V>>();
	
	/**
	 * Creates a new TreeNode
	 */
	public TreeNode() {
		// TODO Auto-generated constructor stub
	}

}
