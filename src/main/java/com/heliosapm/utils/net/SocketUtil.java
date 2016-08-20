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
package com.heliosapm.utils.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Title: SocketUtil</p>
 * <p>Description: Utility class for acquiring ranges of available ports</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.net.SocketUtil</code></p>
 */

public class SocketUtil {
	/** The maximum number of attempts to make when acquiring a new random port */
	public static final int MAX_ATTEMPTS = 64;
	
	/**
	 * Creates a new acquired port range with a number of reserved ports
	 * @param bindAddress The address the ports will be reserved on
	 * @param initialSize The initial number of ports to reserve
	 * @param refill The number of additional ports to reserve when the range is exhausted
	 * @return the acquired port range
	 */
	public static AcquiredPorts acquirePorts(final InetAddress bindAddress, final int initialSize, final int refill) {
		if(bindAddress==null) throw new IllegalArgumentException("The passed bind address was null");
		if(initialSize < 0) throw new IllegalArgumentException("Invalid initial size:" + initialSize);
		if(refill < 0) throw new IllegalArgumentException("Invalid refill size:" + refill);
		return new AcquiredPorts(bindAddress, initialSize, refill);
	}
	
	/**
	 * Creates a new acquired port range with a number of reserved ports bound against the local wildcard
	 * @param initialSize The initial number of ports to reserve
	 * @param refill The number of additional ports to reserve when the range is exhausted
	 * @return the acquired port range
	 */
	public static AcquiredPorts acquirePorts(final int initialSize, final int refill) {
		return acquirePorts(LocalHost.LOCAL_WILDCARD, initialSize, refill);
	}
	
	/**
	 * Creates a new acquired port range with a number of reserved ports bound against the local wildcard and a refill of 1
	 * @param initialSize The initial number of ports to reserve
	 * @return the acquired port range
	 */
	public static AcquiredPorts acquirePorts(final int initialSize) {
		return acquirePorts(LocalHost.LOCAL_WILDCARD, initialSize, 1);
	}
	
	/**
	 * Creates a new acquired port range with a number of reserved ports bound against 
	 * the local wildcard and a refill and intial size of 1
	 * @return the acquired port range
	 */
	public static AcquiredPorts acquirePorts() {
		return acquirePorts(LocalHost.LOCAL_WILDCARD, 1, 1);
	}
	
	/**
	 * Creates a pre-assigned bound port
	 * @param port the pre-assigned port
	 * @return the pre-assigned bound port
	 */
	public static BoundPort preAssigned(final int port) {
		if(port < 1 || port > 65535) throw new IllegalArgumentException("Invalid port:" + port);
		return new BoundPort(port);
	}
	
	
	public static void main(String[] args) {
		AcquiredPorts ap = acquirePorts(2);
		for(int i = 0; i < 4; i++) {
			System.out.println("Port:" + ap.nextPort());
		}
	}
	
	
	/**
	 * <p>Title: BoundPort</p>
	 * <p>Description: Represents a bound port which is made available when {@link #getPort()} is called to return the port</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.net.SocketUtil.BoundPort</code></p>
	 */
	public static class BoundPort implements Closeable {
		private final ServerSocket ss;
		private final int port;
		private final AtomicBoolean open = new AtomicBoolean(true);
		
		private BoundPort(final ServerSocket ss) {
			this.ss = ss;
			port = ss.getLocalPort();
		}
		
		private BoundPort(final int port) {
			this.port = port;
			ss = null;
		}
		
		/**
		 * Returns the port without releasing the bound socket
		 * @return the unavailable port
		 */
		public int peekPort() {
			return port;
		}
		
		/**
		 * Closes the bound socket and returns the now available port
		 * @return the available port
		 */
		public int getPort() {
			if(ss==null) return port;
			if(open.compareAndSet(true, false)) {
				try {
					ss.close();
				} catch (Exception ex) {
					throw new RuntimeException("Socket [" + port + "] not closeable");
				}
				return port;
			}
			return port;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.io.Closeable#close()
		 */
		@Override
		public void close() throws IOException {
			if(!open.get()) {
				try { ss.close(); } catch (Exception x) {/* No Op */}
			}
		}
	}
	
	/**
	 * Attempts to acquire a bound server socket on an ephemeral port
	 * @param inet The inet address that will be used to bind to reserve a port
	 * @return the bound server socket
	 */
	private static ServerSocket nextFreePort(final InetSocketAddress inet) {
		for(int i = 0; i < MAX_ATTEMPTS; i++) {
			try {
				ServerSocket ss = new ServerSocket();
				ss.setReuseAddress(true);
				ss.bind(inet);
				ss.getLocalPort();
				return ss;
			} catch (Exception x) {/* No Op */}
		}
		throw new RuntimeException("Failed to get a free port after [" + MAX_ATTEMPTS + "] attempts");
	}
	

	
	
	/**
	 * <p>Title: AcquiredPorts</p>
	 * <p>Description: Holds a collection of acquired ports which are held until used or closed.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.net.SocketUtil.AcquiredPorts</code></p>
	 */
	public static class AcquiredPorts implements Closeable {
		private final ConcurrentSkipListMap<Integer, BoundPort> sockets;
		private Iterator<Integer> portIter;
		private final InetSocketAddress bindSocketAddress;
		private final int refill;
		
		private AcquiredPorts(final InetAddress bindAddress, final int size, final int refill) {			
			sockets = new ConcurrentSkipListMap<Integer, BoundPort>();			
			this.refill = refill;
			bindSocketAddress = new InetSocketAddress(bindAddress, 0);
			fill(size);			
		}
		
		private void fill(final int howMany) {
			for(int i = 0; i < howMany; i++) {
				final ServerSocket ss = nextFreePort(bindSocketAddress);
				final BoundPort bp = new BoundPort(ss);
				sockets.put(bp.peekPort(), bp);
			}			
			portIter = sockets.keySet().iterator();
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.io.Closeable#close()
		 */
		@Override
		public void close() throws IOException {
			while(!sockets.isEmpty()) {
				for(Map.Entry<Integer, BoundPort> ss: sockets.entrySet()) {
					try { ss.getValue().close(); } catch (Exception x) {/* No Op */}
				}
				sockets.clear();
			}
		}
		
		/**
		 * Returns the number of bound ports that are acquired when refilling
		 * @return the number of bound ports that are acquired when refilling
		 */
		public int getRefill() {
			return refill;
		}
		
		/**
		 * Acquires the next available bound port and returns it's port, releasing the socket,
		 * making it available to be bound to. If no bound ports are available, will 
		 * attempt to refill by acquiring {@link #getRefill()} additional bound ports.
		 * @return an available port (but use it quick... it could get used elsewhere)
		 */
		public int nextPort() {			
			if(!portIter.hasNext()) {
				try { fill(refill); } catch (Exception x) {/* No Op */}
			}
			try {
				final int socketKey = portIter.next();
				final BoundPort bp = sockets.remove(socketKey);
				return bp.getPort();				
			} catch (Exception ex) {
				throw new RuntimeException("Failed to refill bound socket pool. No sockets available.", ex);
			}
		}
		
		/**
		 * Acquires the next available bound port and returns it.
		 * The port will continue to be reserved (and not usable) until {@link BoundPort#getPort()}
		 * is called which will release the underlying socket and return the now available port.
		 * If no bound ports are available, will 
		 * attempt to refill by acquiring {@link #getRefill()} additional bound ports.
		 * @return a bound port which can supply one available port when called
		 */
		public BoundPort nextBoundPort() {
			if(!portIter.hasNext()) {
				try { fill(refill); } catch (Exception x) {/* No Op */}
			}
			try {
				final int socketKey = portIter.next();
				return sockets.remove(socketKey);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to refill bound socket pool. No sockets available.", ex);
			}			
		}

	}
	
	
	private SocketUtil() {}	

}
