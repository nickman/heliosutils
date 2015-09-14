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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.ObjectName;

import jsr166e.LongAdder;

import com.heliosapm.utils.jmx.JMXHelper;

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
	private final ObjectName objectName;
	private final Set<LocalPortForwarder> forwards = new CopyOnWriteArraySet<LocalPortForwarder>();
	private final LongAdder opens = new LongAdder();
	private final LongAdder closes = new LongAdder();
	private final LongAdder bytesUp = new LongAdder();
	private final LongAdder bytesDown = new LongAdder();
	private final LongAdder accepts = new LongAdder();
	
	
	public static LocalPortForwardWatcher getInstance(final String host, final int port) {
		final String key = host + ":" + port;
		LocalPortForwardWatcher watcher = watchers.get(key);
		if(watcher==null) {
			synchronized(watchers) {
				watcher = watchers.get(key);
				if(watcher==null) {
					watcher = new LocalPortForwardWatcher(host, port);
					watchers.put(key, watcher);
				}
			}
		}
		return watcher;
	}
	
	/**
	 * Creates a new LocalPortForwardWatcher
	 */
	private LocalPortForwardWatcher(final String host, final int port) {
		this.host = host;
		this.port = port;
		objectName = JMXHelper.objectName(new StringBuilder("com.heliosapm.ssh:service=LocalPortForwards,remoteHost=")
		.append(this.host)
		.append(",localPort=").append(this.port)
		);
		JMXHelper.registerMBean(this, objectName);
	}
	
	void addForwarder(final LocalPortForwarder forwarder) {
		if(forwarder!=null) {
			if(forwards.add(forwarder)) opens.increment();
		}
	}
	
	void removeForwarder(final LocalPortForwarder forwarder) {
		if(forwarder!=null) {
			if(forwards.remove(forwarder)) closes.increment();
		}

	}
	
	public int getOpen() {
		return forwards.size();
	}
	
	public String getRemoteHost() {
		return host;
	}
	
	public int getRemotePort() {
		return port;
	}

	
	public long getBytesUp() {
		if(forwards.isEmpty()) return bytesUp.longValue();
		long total = 0;
		for(LocalPortForwarder lpf: forwards) {
			total += lpf.getDeltaBytesUp();
		}
		bytesUp.add(total);
		return bytesUp.longValue();
	}
	
	public long getBytesDown() {
		if(forwards.isEmpty()) return bytesDown.longValue();
		long total = 0;
		for(LocalPortForwarder lpf: forwards) {
			total += lpf.getDeltaBytesDown();
		}
		bytesDown.add(total);
		return bytesDown.longValue();
		
		
	}
	
	public long getAccepts() {
		if(forwards.isEmpty()) return accepts.longValue();
		long total = 0;
		for(LocalPortForwarder lpf: forwards) {
			total += lpf.getDeltaAccepts();
		}
		accepts.add(total);
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


}
