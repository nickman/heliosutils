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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import ch.ethz.ssh2.LocalStreamForwarder;

import com.heliosapm.utils.io.CloseListener;

/**
 * <p>Title: WrappedStreamForwarder</p>
 * <p>Description: Functional wrapper for stream forwards</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ssh.terminal.WrappedStreamForwarder</code></p>
 */

public class WrappedStreamForwarder implements CloseListener<WrappedConnection> {
	/** The local port forward reference */
	private LocalStreamForwarder lsf;
	/** The local port forward's parent connection */
	private WrappedConnection parentConnection;
	/** The local port forward key */
	private final String key;
	/** Tracks if this forwarder is actually open */
	private final AtomicBoolean open = new AtomicBoolean(true);
	
	
	
	public static final Pattern KEY_SPLITTER = Pattern.compile(":");

	/**
	 * Creates a new WrappedLocalPortForwarder
	 * @param lsf The local stream forwarder to wrap
	 * @param key The local port forwared key
	 * @param conn The wrapped connection
	 */
	public WrappedStreamForwarder(final LocalStreamForwarder lsf, final String key, final WrappedConnection conn) {
		this.lsf = lsf;
		this.parentConnection = conn;
		this.key = key;
		this.parentConnection.addListener(this);
	}
	
	
	
	WrappedConnection parentConnection() {
		return parentConnection;
	}
	
	public InputStream getInputStream() throws IOException
	{
		return lsf.getInputStream();
	}

	/**
	 * Get the OutputStream. Please be aware that the implementation MAY use an
	 * internal buffer. To make sure that the buffered data is sent over the
	 * tunnel, you have to call the <code>flush</code> method of the
	 * <code>OutputStream</code>. To signal EOF, please use the
	 * <code>close</code> method of the <code>OutputStream</code>.
	 *
	 * @return An <code>OutputStream</code> object.
	 * @throws IOException
	 */
	public OutputStream getOutputStream() throws IOException
	{
		return lsf.getOutputStream();
	}	
	
	public void close() {
		try { lsf.close(); } catch (Exception x) {/* No Op */}
	}
	
	public boolean isOpen() {
		return lsf.isOpen();
	}

	@Override
	public void onClosed(WrappedConnection closeable, Throwable cause) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReset(final WrappedConnection resetCloseable) {
		final String[] hostPort = KEY_SPLITTER.split(key);
		try {
			lsf = resetCloseable.rawStreamTunnel(hostPort[0], Integer.parseInt(hostPort[1]));
			open.set(true);
		} catch (Exception ex) {
			open.set(false);
		}		
	}

}
