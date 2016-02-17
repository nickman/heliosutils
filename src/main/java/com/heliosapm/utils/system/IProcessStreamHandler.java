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
package com.heliosapm.utils.system;

import java.io.InputStream;

/**
 * <p>Title: IProcessStreamHandler</p>
 * <p>Description: Defines a class that will handle output streams (out and err) from a {@link Process}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.system.IProcessStreamHandler</code></p>
 */

public interface IProcessStreamHandler {
	/**
	 * Handles the passed stream from a process instance
	 * @param in The input stream to read
	 * @param out true if the stream is <b><code>System.out</code></b>, false if it is <b><code>System.err</code></b> 
	 * @param process The process the stream is being read from
	 */
	public void handleStream(final InputStream in, final boolean out, final Process process);
	
	/**
	 * Called when the process ends
	 * @param process The process that ended
	 */
	public void onProcessEnd(final Process process);
}
