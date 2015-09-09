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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

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
	private LocalPortForwarder lpf;
	/** The local port forward's parent connection */
	private WrappedConnection parentConnection;
	/** The number of claimed callers */
	private final AtomicInteger claims = new AtomicInteger(0);
	/** The local port forward key */
	private final String key;
	/** Tracks if this forwarder is actually open */
	private final AtomicBoolean open = new AtomicBoolean(true);
	
	public static final Pattern KEY_SPLITTER = Pattern.compile(":");
	
	/**
	 * Creates a new WrappedLocalPortForwarder
	 * @param lpf The local port forwarder
	 * @param key The local port forwared key
	 * @param conn The wrapped connection
	 */
	public WrappedLocalPortForwarder(final LocalPortForwarder lpf, final String key, final WrappedConnection conn) {
		this.lpf = lpf;
		this.parentConnection = conn;
		this.key = key;
		this.parentConnection.addListener(this);
	}
	
	WrappedConnection parentConnection() {
		return parentConnection;
	}
	
	/**
	 * Returns the tunnel key
	 * @return the tunnel key
	 */
	public String getKey() {
		return key;
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
	 * Returns the local port
	 * @return the local port
	 */
	public int getLocalPort() {
		return lpf.getLocalSocketAddress().getPort();
	}
	
	/**
	 * Returns the current claim count
	 * @return the current claim count
	 */
	public int getClaimCount() {
		return claims.get();
	}
	
	/**
	 * Increments and returns the new claim count
	 * @return the new claim count
	 */
	int incrementClaimCount() {
		return claims.incrementAndGet();
	}
	/**
	 * Closes the local port forward
	 * @see ch.ethz.ssh2.LocalPortForwarder#close()
	 */
	public void close() {
		final int cc = claims.decrementAndGet();
		if(cc==0) {
			hardClose();
		}
		this.parentConnection.onLocalPortForwardClosed(key);
	}
	
	/**
	 * Hard closes the underlying port forward
	 */
	void hardClose() {
		try {
			lpf.close();
			open.set(false);
		} catch (Exception ex) {
			/* No Op */
		}		
	}
	
	/**
	 * Indicates if this tunnel is open
	 * @return true if this tunnel is open, false if it has been hard closed
	 */
	public boolean isOpen() {
		return open.get();
	}
	
	/**
	 * @return the to string
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return lpf.toString();
	}

	@Override
	public void onClosed(final WrappedConnection closeable, final Throwable cause) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReset(final WrappedConnection resetCloseable) {
		final String[] hostPort = KEY_SPLITTER.split(key);
		try {
			lpf = resetCloseable.rawTunnel(hostPort[0], Integer.parseInt(hostPort[1]));
			open.set(true);
		} catch (Exception ex) {
			open.set(false);
		}
		
	}
	
}
