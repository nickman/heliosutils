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
package com.heliosapm.utils.ref;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.Callable;

import com.heliosapm.utils.time.SystemClock;

/**
 * <p>Title: ThreadLocalRefCache</p>
 * <p>Description: Had enough repeating this code. This is a weakly referenced thread local cache for arbitrary types.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ref.ThreadLocalRefCache</code></p>
 */

public class ThreadLocalRefCache {

	private ThreadLocalRefCache() {}
	
	private static final Runnable onEnqueueSingle(final String s) {
		return new Runnable() {
			public void run() {
//				System.out.println("Cleared ThreadLocal Ref [" + s + "]");
			}
		};
	}
	
	public static void main(String[] args) {
		final ThreadLocalCache<StringBuilder> K = new ThreadLocalCache<StringBuilder>(new Callable<StringBuilder>(){
			@Override
			public StringBuilder call() throws Exception {				
				return new StringBuilder("Hello Thread [" + Thread.currentThread() + "]");
			}
		});
		Thread t = new Thread() {
			public void run() {
				K.set(new StringBuilder().append("It is I, thread [" + Thread.currentThread() + "]"));
				System.out.println("SB:" + K.get());
			}
		};
		t.start();
		try {
			t.join();
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		System.out.println("Thread Done");
		SystemClock.sleep(1000);
		System.out.println("1st sleep");
		System.gc();
		System.out.println("GC");
		SystemClock.sleep(1000);
		System.out.println("2nd sleep");
		System.gc();
		System.out.println("Another GC");
		
		
	}
	
	public static class ThreadLocalCacheMap<K, V> {
		/** The thread local ref */
		private final ThreadLocal<Map<K, WeakReference<V>>> cache = new ThreadLocal<Map<K, WeakReference<V>>>();
		/** A factory to create the required instance of T */
		private final Callable<V> factory;
		
		/**
		 * Creates a new ThreadLocalCacheMap
		 * @param factory An optional factory to create the required instance of V 
		 */
		public ThreadLocalCacheMap(Callable<V> factory) {
			this.factory = factory;
		}
		
		private V make() {
			if(factory==null) return null;
			try {
				return factory.call();
			} catch (Exception ex) {
				throw new RuntimeException("Failed to create instance to cache in map", ex);
			}
		}
		
		public void set(final K k, final V v) {
			//cache.set(ReferenceService.getInstance().newWeakReference(t, onEnqueueSingle));											
		}
		

		
	}
	
	public static class ThreadLocalCache<T> {
		/** The thread local ref */
		private final ThreadLocal<WeakReference<T>> cache = new ThreadLocal<WeakReference<T>>();
		/** A factory to create the required instance of T */
		private final Callable<T> factory;
		
		/**
		 * Creates a new ThreadLocalCache
		 * @param factory An optional factory to create the required instance of T 
		 */
		public ThreadLocalCache(Callable<T> factory) {
			this.factory = factory;
		}
		
		private T make() {
			if(factory==null) return null;
			try {
				return factory.call();
			} catch (Exception ex) {
				throw new RuntimeException("Failed to create instance to cache", ex);
			}
		}
		
		public void set(final T t) {
			cache.set(ReferenceService.getInstance().newWeakReference(t, onEnqueueSingle(t.getClass().getSimpleName())));											
		}
		
		/**
		 * Removes and returns the referenced object for the calling thread
		 * @return the referenced object or null if one was not present
		 */
		public T remove() {
			T t = null;
			final WeakReference<T> tref = cache.get();
			if(tref!=null) t = tref.get();
			cache.remove();
			return t;
		}
		
		public T get() {
			T t = null;
			WeakReference<T> ref = cache.get();
			if(ref==null) {				
				t = make();
				if(t==null) return null;
				ref = ReferenceService.getInstance().newWeakReference(t, onEnqueueSingle(t.getClass().getSimpleName()));
				cache.set(ref);				
			} else {
				t = ref.get();
				if(t==null) {
					t = make();
					if(t==null) return null;
					ref = ReferenceService.getInstance().newWeakReference(t, onEnqueueSingle(t.getClass().getSimpleName()));
					cache.set(ref);									
				}				
			}
			return t;
		}
	}

}
