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
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import com.heliosapm.utils.jmx.SharedNotificationExecutor;

/**
 * <p>Title: BroadcastingCloseableImpl</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.io.BroadcastingCloseableImpl</code></p>
 * @param <T> The assumed closeable type being managed
 */

public abstract class BroadcastingCloseableImpl<T extends Closeable> implements BroadcastingCloseable<T> {
	/** A set of registered listeners */
	private final Set<CloseListener<Closeable>> listeners = new CopyOnWriteArraySet<CloseListener<Closeable>>();
	/** Flag indicating if the closeable has been closed */
	private final AtomicBoolean closed = new AtomicBoolean(false);
	/** The closeable to manage */
	private final T managedCloseable;
	
	/**
	 * Creates a new BroadcastingCloseableImpl
	 * @param managed The closeable to manage
	 */
	public BroadcastingCloseableImpl(final T managed) {
		if(managed==null) throw new IllegalArgumentException("The passed manaed closeable was null");
		this.managedCloseable = managed;
	}
	
	/**
	 * Returns the managed closeable
	 * @return the managed closeable
	 */
	public T getManagedCloseable() {
		return managedCloseable;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.io.BroadcastingCloseable#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return !closed.get();
	}
	
	/**
	 * To be called by the closeable to trigger the close event broadcast 
	 * @param cause The cause of the close or null if close was deliberate
	 */
	public void doClose(final Throwable cause)  {
		if(closed.compareAndSet(false, true)) {
			if(!listeners.isEmpty()) {
				final Closeable closeable = this;
				synchronized(closed) {
					for(final CloseListener<Closeable> listener : listeners) {
						SharedNotificationExecutor.getInstance().execute(new Runnable(){
							@Override
							public void run() {
								listener.onClosed(closeable, cause);
							}
						});
					}
				}
			}
		}
	}
	
	
	
	/**
	 * To be called by the closeable to trigger the reset event broadcast
	 * Resets the closeable (sets it to open) and notifies all registered listeners
	 */
	public void doReset() {
		synchronized(closed) {
			if(closed.compareAndSet(true, false)) {
				if(!listeners.isEmpty()) {
					final Closeable closeable = this;
					for(final CloseListener<Closeable> listener : listeners) {
						SharedNotificationExecutor.getInstance().execute(new Runnable(){
							@Override
							public void run() {
								listener.onReset(closeable);
							}
						});
					}
				}
			}			
		}
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.io.BroadcastingCloseable#addListener(com.heliosapm.utils.io.CloseListener)
	 */
	@Override
	public void addListener(final CloseListener<T> listener) {
		if(listener==null) throw new IllegalArgumentException("The passed listener was null");
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.io.BroadcastingCloseable#removeListener(com.heliosapm.utils.io.CloseListener)
	 */
	@Override
	public void removeListener(final CloseListener<T> listener) {
		if(listener==null) throw new IllegalArgumentException("The passed listener was null");
		
	}
	
	/**
	 * Removes all registered listeners
	 */
	public void clearListeners() {
		synchronized(closed) {
			listeners.clear();
		}
	}

}
