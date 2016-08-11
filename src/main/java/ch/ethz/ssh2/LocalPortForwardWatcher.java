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
package ch.ethz.ssh2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;

import com.heliosapm.utils.jmx.JMXHelper;

import jsr166e.AccumulatingLongAdder;
import jsr166e.LongAdder;

/**
 * <p>Title: LocalPortForwardWatcher</p>
 * <p>Description: Service to provide aggregated local port forwarding stats</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>ch.ethz.ssh2.LocalPortForwardWatcher</code></p>
 */

public class LocalPortForwardWatcher implements LocalPortForwardWatcherMBean {
	private static final Map<String, LocalPortForwardWatcher> watchers = new ConcurrentHashMap<String, LocalPortForwardWatcher>();

	private final String host;
	private final int port;
	private final int localPort;
	private final LocalPortForwarder lpf;
	
	private final ObjectName objectName;
	
	private final LongAdder opens = new LongAdder();
	private final LongAdder closes = new LongAdder();
	private final LongAdder bytesUp = new LongAdder();
	private final LongAdder bytesDown = new LongAdder();
	private final LongAdder accepts = new LongAdder();
	private final LongAdder openForwards = new LongAdder();
	

	
	
	public static LocalPortForwardWatcher getInstance(final LocalPortForwarder lpf) {
		final String key = lpf.getHost() + ":" + lpf.getPort();
		LocalPortForwardWatcher watcher = watchers.get(key);
		if(watcher==null) {
			synchronized(watchers) {
				watcher = watchers.get(key);
				if(watcher==null) {
					watcher = new LocalPortForwardWatcher(lpf);
					watchers.put(key, watcher);
				}
			}
		}
		return watcher;
	}
	
	/**
	 * Creates a new LocalPortForwardWatcher
	 */
	private LocalPortForwardWatcher(final LocalPortForwarder lpf) {		
		this.host = lpf.getHost();
		this.port = lpf.getPort();
		this.lpf = lpf;
		objectName = JMXHelper.objectName(new StringBuilder("com.heliosapm.ssh:service=LocalPortForwards,remoteHost=")
		.append(this.host)
		.append(",remotePort=").append(this.port)
		);
		JMXHelper.registerMBean(this, objectName);
		this.localPort = lpf.getLocalPort();
	}
	
	void incrementOpens() {
		opens.increment();
		openForwards.increment();
	}
	void incrementCloses() {
		closes.increment();
		openForwards.decrement();
	}
	
	
	public int getOpen() {
		return openForwards.intValue();
	}
	
	public String getRemoteHost() {
		return host;
	}
	
	public int getRemotePort() {
		return port;
	}

	public int getLocalPort() {
		return lpf.getLocalPort();
	}
	
	public long getBytesUp() {
		return bytesUp.longValue();
	}
	
	public long getBytesDown() {
		return bytesDown.longValue();
		
		
	}
	
	public long getAccepts() {
		return accepts.longValue();		
	}

	/**
	 * Returns the total number of opens
	 * @return the opens
	 */
	public long getOpens() {
		return opens.longValue();
	}

	/**
	 * Returns the total number of closes
	 * @return the closes
	 */
	public long getCloses() {
		return closes.longValue();
	}

	LongAdder getAcceptsAccumulator() {
		return new AccumulatingLongAdder(accepts);
	}
	LongAdder getBytesDownAccumulator() {
		return new AccumulatingLongAdder(bytesDown);
	}
	LongAdder getBytesUpAccumulator() {
		return new AccumulatingLongAdder(bytesUp);
	}
	LongAdder getClosesAccumulator() {
		return new AccumulatingLongAdder(closes);
	}
	LongAdder getOpensAccumulator() {
		return new AccumulatingLongAdder(opens);
	}


}
