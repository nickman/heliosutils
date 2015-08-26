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
package com.heliosapm.utils.ssh.terminal;

import java.io.IOException;

import ch.ethz.ssh2.Connection;

/**
 * <p>Title: Authenticator</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ssh.terminal.Authenticator</code></p>
 */

public interface Authenticator {
	/**
	 * Attempts an authentication agains the passed connection using the resources in the passed authed info.
	 * If the connection is already fully authenticated, immediately returns true
	 * @param conn The connection to authenticate against
	 * @param authInfo The authentication resources
	 * @return true the connection is now authenticated, false otherwise
	 * @throws IOException Thrown on any IO error
	 */
	public boolean authenticate(final Connection conn, final AuthInfo authInfo) throws IOException;
}
