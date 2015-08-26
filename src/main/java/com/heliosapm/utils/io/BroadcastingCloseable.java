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
 * <p>Title: BroadcastingCloseable</p>
 * <p>Description: An extension of {@link Closeable} on which {@link CloseListener}s can be registered</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.io.BroadcastingCloseable</code></p>
 * @param <T> The closeable type
 */

public interface BroadcastingCloseable<T extends Closeable> extends Closeable {
	/**
	 * Adds a listener to the close event on this closeable
	 * @param listener The close listener to add
	 */
	public void addListener(final CloseListener<T> listener);
	
	/**
	 * Removes a listener from this closeable
	 * @param listener The close listener to remove
	 */
	public void removeListener(final CloseListener<T> listener);

}
