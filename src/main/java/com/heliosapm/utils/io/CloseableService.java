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
package com.heliosapm.utils.io;

import java.io.Closeable;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import com.heliosapm.utils.jmx.JMXHelper;

/**
 * <p>Title: CloseableService</p>
 * <p>Description: Service to register {@link Closeable} instances with so they can all be closed at later point.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.io.CloseableService</code></p>
 */

public class CloseableService implements CloseableServiceMBean {
	/** The singleton instance */
	private static volatile CloseableService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The registered closeables */
	private final Map<Long, Closeable> closeables = new ConcurrentSkipListMap<Long, Closeable>();
	/** The serial number assignore for each registered closeable */
	private final AtomicLong serial = new AtomicLong(0L);	
	/** The count of successfully closed closeables */
	private final AtomicLong goodCloses = new AtomicLong(0L);
	/** The count of failed closes */
	private final AtomicLong failedCloses = new AtomicLong(0L);
	
	/** The CloseableService JMX ObjectName  */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName(CloseableService.class);
	
	/**
	 * Acquires the CloseableService singleton instance
	 * @return the CloseableService singleton instance
	 */
	public static CloseableService getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new CloseableService();
					JMXHelper.registerMBean(instance, OBJECT_NAME);
				}
			}
		}
		return instance;
	}
	/**
	 * Creates a new CloseableService
	 */
	private CloseableService() {
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				closeAll();
			}
		});
	}
	
	/**
	 * Returns the number of registered closeables
	 * @return the number of registered closeables
	 */
	@Override
	public int getCloseableCount() {
		return closeables.size();
	}
	
	/**
	 * Registers a new closeable
	 * @param closeable the closeable to register
	 */
	@Override
	public void register(final Closeable closeable) {
		if(closeable==null) throw new IllegalArgumentException("The passed closeable was null");
		synchronized(closeables) {
			closeables.put(serial.incrementAndGet(), closeable);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.io.CloseableServiceMBean#closeAll()
	 */
	@Override
	public void closeAll() {
		closeAll(false);
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.io.CloseableServiceMBean#closeAll(boolean)
	 */
	@Override
	public void closeAll(final boolean self) {
		final Map<Long, Closeable> copy;
		synchronized(closeables) {
			copy = new TreeMap<Long, Closeable>(closeables);
			closeables.clear();
			serial.set(0);
		}
		if(!copy.isEmpty()) {
			for(Closeable c: copy.values()) {
				try { 
					c.close();
					goodCloses.incrementAndGet();
				} catch (Exception x) {
					failedCloses.incrementAndGet();
				}
			}
		}
		copy.clear();
		if(self) {
			try { JMXHelper.unregisterMBean(OBJECT_NAME); } catch (Exception x) {/* No Op */}
		}
	}
	/**
	 * Returns the cumulative count of successful closes
	 * @return the cumulative count of successful closes
	 */
	@Override
	public long getGoodCloses() {
		return goodCloses.get();
	}
	/**
	 * Returns the cumulative count of failed closes
	 * @return the cumulative count of failed closes
	 */
	@Override
	public long getFailedCloses() {
		return failedCloses.get();
	}

}
