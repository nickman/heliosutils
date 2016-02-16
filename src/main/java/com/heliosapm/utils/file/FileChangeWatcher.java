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
package com.heliosapm.utils.file;

import java.io.File;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

import com.heliosapm.utils.jmx.SharedNotificationExecutor;
import com.heliosapm.utils.time.SystemClock;

/**
 * <p>Title: FileChangeWatcher</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.file.FileChangeWatcher</code></p>
 */

public class FileChangeWatcher extends Thread {
	protected final Map<File, Long> trackedFiles = new NonBlockingHashMap<File, Long>();
	protected final long scanPeriodSecs;
	protected final FileFinder finder;
	protected final Map<FileChangeEvent, Set<FileChangeEventListener>> fileChangeListeners = new EnumMap<FileChangeEvent, Set<FileChangeEventListener>>(FileChangeEvent.class);
	protected final AtomicBoolean running = new AtomicBoolean(false);
	protected final AtomicBoolean dead = new AtomicBoolean(false);
	protected final boolean initBeforeFire;
	protected int scanPeriod = DEFAULT_SCAN_PERIOD;
	
	private static final AtomicInteger serial = new AtomicInteger(0);
	
	/** The default number of seconds between change scans */
	public static final int DEFAULT_SCAN_PERIOD = 5;
	
	
	public FileChangeWatcher(final FileFinder finder, final long scanPeriodSecs, final boolean initBeforeFire, final FileChangeEventListener...listeners) {
		super("FileChangeWatcher#" + serial.incrementAndGet());
		setDaemon(true);
		this.finder = finder;
		this.initBeforeFire = initBeforeFire;
		this.scanPeriodSecs = scanPeriodSecs;
		for(FileChangeEvent fce: FileChangeEvent.values()) {
			fileChangeListeners.put(fce, new CopyOnWriteArraySet<FileChangeEventListener>());
		}
		addListeners(listeners);
	}
	
	public FileChangeWatcher addListeners(FileChangeEventListener...listeners) {
		if(dead.get()) throw new IllegalStateException("The FileChangeWatcher is dead");
		for(FileChangeEventListener listener: listeners) {
			if(listener!=null) {
				for(FileChangeEvent fce: listener.getInterest()) {
					if(fileChangeListeners.get(fce).add(listener)) {
						listener.setFileChangeWatcher(this);
					}
				}
			}
		}
		return this;
	}
	
	public FileChangeWatcher removeListeners(FileChangeEventListener...listeners) {
		for(FileChangeEventListener listener: listeners) {
			if(listener!=null) {
				for(FileChangeEvent fce: listener.getInterest()) {
					fileChangeListeners.get(fce).remove(listener);
				}
			}
		}
		return this;
	}
	
	
	public FileChangeWatcher startWatcher() {
		return startWatcher(DEFAULT_SCAN_PERIOD);
	}
	
	public FileChangeWatcher startWatcher(final int period) {
		if(dead.get()) throw new IllegalStateException("The FileChangeWatcher is dead");
		if(period < 1) throw new IllegalArgumentException("Invalid scan period [" + period + "]");
		if(running.compareAndSet(false, true)) {
			this.scanPeriod = period;
			start();
		}
		return this;
	}
	
	public void stopWatcher() {
		if(running.compareAndSet(true, false)) {
			dead.set(true);
			interrupt();			
		}
	}
	
	public boolean isStarted() {
		return running.get();
	}
	
	public boolean isDead() {
		return dead.get();
	}
	
	public void run() {
		doIt(!initBeforeFire);
		while(running.get()) {
			try {
				doIt(true);
			} catch (Exception ex) {
				if(Thread.interrupted()) Thread.interrupted();
				/* No Op */
			} finally {
				SystemClock.sleep(scanPeriod, TimeUnit.SECONDS);
			}
		}
	}
	
	protected void fireEvent(final FileChangeEvent event, final File file) {
		for(final FileChangeEventListener listener: fileChangeListeners.get(event)) {
			SharedNotificationExecutor.getInstance().execute(new Runnable(){
				public void run() {
					switch(event) {
					case DELETED:
						listener.onDelete(file);
						break;
					case MODIFIED:
						listener.onChange(file);
						break;
					case NEW:
						listener.onNew(file);
						break;
					default:
						break;					
					}
				}
			});
		}
	}
	
	protected void doIt(final boolean fire) {
		final Set<File> copy = new HashSet<File>(trackedFiles.keySet());
		final File[] foundFiles = finder.find();
		for(File f: foundFiles) {
			copy.remove(f);
			final long lastMod = f.lastModified();
			Long trackedTime = trackedFiles.put(f, lastMod);
			if(trackedTime==null) {				
				if(fire) fireEvent(FileChangeEvent.NEW, f);
			} else {				
				if(lastMod > trackedTime.longValue()) {
					if(fire) fireEvent(FileChangeEvent.MODIFIED, f);
				}
			}
		}
		if(fire) {
			for(File f: copy) {
				if(!f.exists()) {
					fireEvent(FileChangeEvent.DELETED, f);
				}
			}
		}
	}

	/**
	 * Returns 
	 * @return the finder
	 */
	public FileFinder getFinder() {
		return finder;
	}
	
	
}
