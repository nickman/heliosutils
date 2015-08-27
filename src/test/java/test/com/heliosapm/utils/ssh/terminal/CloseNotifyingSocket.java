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
package test.com.heliosapm.utils.ssh.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import com.heliosapm.utils.io.BroadcastingCloseable;
import com.heliosapm.utils.io.BroadcastingCloseableImpl;
import com.heliosapm.utils.io.CloseListener;

/**
 * <p>Title: CloseNotifyingSocket</p>
 * <p>Description: Close bevent broadcasting socket so we know when a client socket closes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.com.heliosapm.utils.ssh.terminal.CloseNotifyingSocket</code></p>
 */

public class CloseNotifyingSocket extends Socket implements BroadcastingCloseable<Socket> {
	/** The broadcast manager */
	final BroadcastingCloseableImpl<Socket> impl;
	/** The delegate socket */
	final Socket socket;
	/**
	 * Creates a new CloseNotifyingSocket
	 * @param socket The delegate socket
	 * @param listeners An optional array of close listeners
	 */
	public CloseNotifyingSocket(final Socket socket, final CloseListener<Socket>...listeners) {
		this.socket = socket;
		impl = new BroadcastingCloseableImpl<Socket>(socket) {
			@Override
			public void reset() {
				throw new UnsupportedOperationException("Reset not supported on " + getClass().getName());				
			}
			@Override
			public void close() throws IOException {				
				doClose(null);
			}			
		};
		if(listeners!=null) {
			for(CloseListener<Socket> listener: listeners) {
				if(listener==null) continue;
				addListener(listener);
			}
		}
	}
	@Override
	public void close() throws IOException {
		try { socket.close(); } catch (Exception x) { /* No Op */ }
		impl.close();
		
	}
	@Override
	public void addListener(final CloseListener<Socket> listener) {
		impl.addListener(listener);
	}
	@Override
	public void removeListener(CloseListener<Socket> listener) {
		impl.removeListener(listener);
		
	}
	@Override
	public void reset() {
		throw new UnsupportedOperationException("Reset not supported on " + getClass().getName());				
	}
	@Override
	public boolean isOpen() {
		return impl.isOpen();
	}
	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return socket.hashCode();
	}
	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return socket.equals(obj);
	}
	/**
	 * @param endpoint
	 * @throws IOException
	 * @see java.net.Socket#connect(java.net.SocketAddress)
	 */
	public void connect(SocketAddress endpoint) throws IOException {
		socket.connect(endpoint);
	}
	/**
	 * @param endpoint
	 * @param timeout
	 * @throws IOException
	 * @see java.net.Socket#connect(java.net.SocketAddress, int)
	 */
	public void connect(SocketAddress endpoint, int timeout) throws IOException {
		socket.connect(endpoint, timeout);
	}
	/**
	 * @param bindpoint
	 * @throws IOException
	 * @see java.net.Socket#bind(java.net.SocketAddress)
	 */
	public void bind(SocketAddress bindpoint) throws IOException {
		socket.bind(bindpoint);
	}
	/**
	 * @return
	 * @see java.net.Socket#getInetAddress()
	 */
	public InetAddress getInetAddress() {
		return socket.getInetAddress();
	}
	/**
	 * @return
	 * @see java.net.Socket#getLocalAddress()
	 */
	public InetAddress getLocalAddress() {
		return socket.getLocalAddress();
	}
	/**
	 * @return
	 * @see java.net.Socket#getPort()
	 */
	public int getPort() {
		return socket.getPort();
	}
	/**
	 * @return
	 * @see java.net.Socket#getLocalPort()
	 */
	public int getLocalPort() {
		return socket.getLocalPort();
	}
	/**
	 * @return
	 * @see java.net.Socket#getRemoteSocketAddress()
	 */
	public SocketAddress getRemoteSocketAddress() {
		return socket.getRemoteSocketAddress();
	}
	/**
	 * @return
	 * @see java.net.Socket#getLocalSocketAddress()
	 */
	public SocketAddress getLocalSocketAddress() {
		return socket.getLocalSocketAddress();
	}
	/**
	 * @return
	 * @see java.net.Socket#getChannel()
	 */
	public SocketChannel getChannel() {
		return socket.getChannel();
	}
	/**
	 * @return
	 * @throws IOException
	 * @see java.net.Socket#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}
	/**
	 * @return
	 * @throws IOException
	 * @see java.net.Socket#getOutputStream()
	 */
	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}
	/**
	 * @param on
	 * @throws SocketException
	 * @see java.net.Socket#setTcpNoDelay(boolean)
	 */
	public void setTcpNoDelay(boolean on) throws SocketException {
		socket.setTcpNoDelay(on);
	}
	/**
	 * @return
	 * @throws SocketException
	 * @see java.net.Socket#getTcpNoDelay()
	 */
	public boolean getTcpNoDelay() throws SocketException {
		return socket.getTcpNoDelay();
	}
	/**
	 * @param on
	 * @param linger
	 * @throws SocketException
	 * @see java.net.Socket#setSoLinger(boolean, int)
	 */
	public void setSoLinger(boolean on, int linger) throws SocketException {
		socket.setSoLinger(on, linger);
	}
	/**
	 * @return
	 * @throws SocketException
	 * @see java.net.Socket#getSoLinger()
	 */
	public int getSoLinger() throws SocketException {
		return socket.getSoLinger();
	}
	/**
	 * @param data
	 * @throws IOException
	 * @see java.net.Socket#sendUrgentData(int)
	 */
	public void sendUrgentData(int data) throws IOException {
		socket.sendUrgentData(data);
	}
	/**
	 * @param on
	 * @throws SocketException
	 * @see java.net.Socket#setOOBInline(boolean)
	 */
	public void setOOBInline(boolean on) throws SocketException {
		socket.setOOBInline(on);
	}
	/**
	 * @return
	 * @throws SocketException
	 * @see java.net.Socket#getOOBInline()
	 */
	public boolean getOOBInline() throws SocketException {
		return socket.getOOBInline();
	}
	/**
	 * @param timeout
	 * @throws SocketException
	 * @see java.net.Socket#setSoTimeout(int)
	 */
	public void setSoTimeout(int timeout) throws SocketException {
		socket.setSoTimeout(timeout);
	}
	/**
	 * @return
	 * @throws SocketException
	 * @see java.net.Socket#getSoTimeout()
	 */
	public int getSoTimeout() throws SocketException {
		return socket.getSoTimeout();
	}
	/**
	 * @param size
	 * @throws SocketException
	 * @see java.net.Socket#setSendBufferSize(int)
	 */
	public void setSendBufferSize(int size) throws SocketException {
		socket.setSendBufferSize(size);
	}
	/**
	 * @return
	 * @throws SocketException
	 * @see java.net.Socket#getSendBufferSize()
	 */
	public int getSendBufferSize() throws SocketException {
		return socket.getSendBufferSize();
	}
	/**
	 * @param size
	 * @throws SocketException
	 * @see java.net.Socket#setReceiveBufferSize(int)
	 */
	public void setReceiveBufferSize(int size) throws SocketException {
		socket.setReceiveBufferSize(size);
	}
	/**
	 * @return
	 * @throws SocketException
	 * @see java.net.Socket#getReceiveBufferSize()
	 */
	public int getReceiveBufferSize() throws SocketException {
		return socket.getReceiveBufferSize();
	}
	/**
	 * @param on
	 * @throws SocketException
	 * @see java.net.Socket#setKeepAlive(boolean)
	 */
	public void setKeepAlive(boolean on) throws SocketException {
		socket.setKeepAlive(on);
	}
	/**
	 * @return
	 * @throws SocketException
	 * @see java.net.Socket#getKeepAlive()
	 */
	public boolean getKeepAlive() throws SocketException {
		return socket.getKeepAlive();
	}
	/**
	 * @param tc
	 * @throws SocketException
	 * @see java.net.Socket#setTrafficClass(int)
	 */
	public void setTrafficClass(int tc) throws SocketException {
		socket.setTrafficClass(tc);
	}
	/**
	 * @return
	 * @throws SocketException
	 * @see java.net.Socket#getTrafficClass()
	 */
	public int getTrafficClass() throws SocketException {
		return socket.getTrafficClass();
	}
	/**
	 * @param on
	 * @throws SocketException
	 * @see java.net.Socket#setReuseAddress(boolean)
	 */
	public void setReuseAddress(boolean on) throws SocketException {
		socket.setReuseAddress(on);
	}
	/**
	 * @return
	 * @throws SocketException
	 * @see java.net.Socket#getReuseAddress()
	 */
	public boolean getReuseAddress() throws SocketException {
		return socket.getReuseAddress();
	}
	/**
	 * @throws IOException
	 * @see java.net.Socket#shutdownInput()
	 */
	public void shutdownInput() throws IOException {
		socket.shutdownInput();
	}
	/**
	 * @throws IOException
	 * @see java.net.Socket#shutdownOutput()
	 */
	public void shutdownOutput() throws IOException {
		socket.shutdownOutput();
	}
	/**
	 * @return
	 * @see java.net.Socket#toString()
	 */
	public String toString() {
		return socket.toString();
	}
	/**
	 * @return
	 * @see java.net.Socket#isConnected()
	 */
	public boolean isConnected() {
		return socket.isConnected();
	}
	/**
	 * @return
	 * @see java.net.Socket#isBound()
	 */
	public boolean isBound() {
		return socket.isBound();
	}
	/**
	 * @return
	 * @see java.net.Socket#isClosed()
	 */
	public boolean isClosed() {
		return socket.isClosed();
	}
	/**
	 * @return
	 * @see java.net.Socket#isInputShutdown()
	 */
	public boolean isInputShutdown() {
		return socket.isInputShutdown();
	}
	/**
	 * @return
	 * @see java.net.Socket#isOutputShutdown()
	 */
	public boolean isOutputShutdown() {
		return socket.isOutputShutdown();
	}
	/**
	 * @param connectionTime
	 * @param latency
	 * @param bandwidth
	 * @see java.net.Socket#setPerformancePreferences(int, int, int)
	 */
	public void setPerformancePreferences(int connectionTime, int latency,
			int bandwidth) {
		socket.setPerformancePreferences(connectionTime, latency, bandwidth);
	}

}
