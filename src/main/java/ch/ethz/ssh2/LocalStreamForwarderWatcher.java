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
 * <p>Title: LocalStreamForwarderWatcher</p>
 * <p>Description: Service to provide aggregated local stream forwarding stats</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>ch.ethz.ssh2.LocalStreamForwarderWatcher</code></p>
 */

public class LocalStreamForwarderWatcher implements LocalStreamForwarderWatcherMBean {
	private static final Map<String, LocalStreamForwarderWatcher> watchers = new ConcurrentHashMap<String, LocalStreamForwarderWatcher>();

	private final String host;
	private final int port;
	private final ObjectName objectName;
	private final LongAdder opens = new LongAdder();
	private final LongAdder closes = new LongAdder();
	private final LongAdder bytesUp = new LongAdder();
	private final LongAdder bytesDown = new LongAdder();
	private final LongAdder openStreams = new LongAdder();

	public static LocalStreamForwarderWatcher getInstance(final String host, final int port) {
		final String key = host + ":" + port;
		LocalStreamForwarderWatcher watcher = watchers.get(key);
		if(watcher==null) {
			synchronized(watchers) {
				watcher = watchers.get(key);
				if(watcher==null) {
					watcher = new LocalStreamForwarderWatcher(host, port);
					watchers.put(key, watcher);
				}
			}
		}
		return watcher;
	}
	

	/**
	 * Creates a new LocalStreamForwarderWatcher
	 */
	private LocalStreamForwarderWatcher(final String host, final int port) {
		this.host = host;
		this.port = port;
		objectName = JMXHelper.objectName(new StringBuilder("com.heliosapm.ssh:service=LocalStreamForwards,remoteHost=")
		.append(this.host)
		.append(",remotePort=").append(this.port)
		);
		JMXHelper.registerMBean(this, objectName);
	}
	
	void incrementOpens() {
		opens.increment();
		openStreams.increment();
	}
	void incrementCloses() {
		closes.increment();
		openStreams.decrement();
	}
	
	
	public int getOpen() {
		return openStreams.intValue();
	}
	
	public String getRemoteHost() {
		return host;
	}
	
	public int getRemotePort() {
		return port;
	}

	
	public long getBytesUp() {
		return bytesUp.longValue();
	}
	
	public long getBytesDown() {
		return bytesDown.longValue();
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
