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

import ch.ethz.ssh2.ServerHostKeyVerifier;

/**
 * <p>Title: YesManHostKeyVerifier</p>
 * <p>Description: A {@link ServerHostKeyVerifier} implementation that always returns true</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ssh.terminal.YesManHostKeyVerifier</code></p>
 */

public class YesManHostKeyVerifier implements ServerHostKeyVerifier {
	/** A shareable instance */
	public static final YesManHostKeyVerifier INSTANCE = new YesManHostKeyVerifier();
	
	/**
	 * Creates a new YesManHostKeyVerifier
	 */
	private YesManHostKeyVerifier() {
	}

	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.ServerHostKeyVerifier#verifyServerHostKey(java.lang.String, int, java.lang.String, byte[])
	 */
	@Override
	public boolean verifyServerHostKey(final String hostname, final int port,  final String serverHostKeyAlgorithm, final byte[] serverHostKey) throws Exception {
		return true;
	}

}
