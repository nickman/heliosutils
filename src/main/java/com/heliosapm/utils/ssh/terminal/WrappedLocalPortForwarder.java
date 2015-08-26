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

import java.net.InetSocketAddress;

import ch.ethz.ssh2.LocalPortForwarder;

import com.heliosapm.utils.io.CloseListener;

/**
 * <p>Title: WrappedLocalPortForwarder</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ssh.terminal.WrappedLocalPortForwarder</code></p>
 */

public class WrappedLocalPortForwarder implements CloseListener<WrappedConnection> {
	/** The local port forward reference */
	private final LocalPortForwarder lpf;
	/** The local port forward's parent connection */
	private final WrappedConnection parentConnection;
	/**
	 * Creates a new WrappedLocalPortForwarder
	 * @param lpf The local port forwarder
	 * @param conn The wrapped connection
	 */
	public WrappedLocalPortForwarder(final LocalPortForwarder lpf, final WrappedConnection conn) {
		this.lpf = lpf;
		this.parentConnection = conn;
	}

	/**
	 * Returns the local socket address
	 * @return the local socket address
	 * @see ch.ethz.ssh2.LocalPortForwarder#getLocalSocketAddress()
	 */
	public InetSocketAddress getLocalSocketAddress() {
		return lpf.getLocalSocketAddress();
	}
	
	/**
	 * Closes the local port forward
	 * @see ch.ethz.ssh2.LocalPortForwarder#close()
	 */
	public void close() {
		try {
			lpf.close();
		} catch (Exception ex) {
			/* No Op */
		}
	}
	
	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return lpf.toString();
	}
	
}
