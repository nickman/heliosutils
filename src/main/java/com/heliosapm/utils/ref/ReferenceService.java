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

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;

import jsr166e.LongAdder;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.jmx.JMXManagedThreadPool;
import com.heliosapm.utils.unsafe.collections.ConcurrentLongSlidingWindow;

/**
 * <p>Title: ReferenceService</p>
 * <p>Description: Generic service for actively handling enqueued references</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ref.ReferenceService</code></p>
 */

public class ReferenceService implements Runnable, ReferenceServiceMXBean, UncaughtExceptionHandler {
	/** The singleton instance */
	private static volatile ReferenceService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The ref queue cleaner thread */
	private final Thread refQueueThread;
	/** A thread pool to run the ref cleaner runnables */
	private JMXManagedThreadPool threadPool;
	
	/** The queue where enqueued references go to die */
	private final ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>(); 
	
	/** A map of counts of cleared references keyed by the reference class name */
	private final NonBlockingHashMap<String, ReferenceTypeCountMBean> countsByType = new NonBlockingHashMap<String, ReferenceTypeCountMBean>(); 
	
	/** Elapsed time stats in ms. to process a ref clear */
	private final ConcurrentLongSlidingWindow clearStats = new ConcurrentLongSlidingWindow(1000);
	/** A count of the number of cleared references */
	private final LongAdder clearedRefCount = new LongAdder();	
	/** A count of the clearing thread errors */
	private final LongAdder clearingErrors = new LongAdder();
	/** A count of presumably uncleared references */
	private final LongAdder unClearedRefCount = new LongAdder();	
	
	/**
	 * Returns the count of registered but uncleared references
	 * @return the count of registered but uncleared references
	 */
	public long getUnClearedRefCount() {
		return unClearedRefCount.longValue();
	}


	/** The task start time for each task execution thread */
	private final ThreadLocal<long[]> taskStartTime = new ThreadLocal<long[]>() {
		@Override
		protected long[] initialValue() {			
			return new long[1];
		}
	};
	
	/** The ref service JMX ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName(ReferenceService.class);
	/** The ref service's thread pool JMX ObjectName */
	public static final ObjectName THREAD_POOL_OBJECT_NAME = JMXHelper.objectName(new StringBuilder(OBJECT_NAME.toString()).append("ThreadPool"));
	
	public static final CompositeType REF_TYPE_COUNT;
	public static final TabularType TABULAR_REF_TYPE_COUNT;
	
	
	
	/** The length field in the ref queue class */
	private static final Field refQueueLengthField;
	/** The referent field in the reference base class */
	private static final Field referentField;
	
	static {
		Field f = null;
		Field r = null;
		try {
			f = ReferenceQueue.class.getDeclaredField("queueLength");
			f.setAccessible(true);
			r = Reference.class.getDeclaredField("referent");
			r.setAccessible(true);
		} catch (Exception ex) {
			f = null;
		}
		refQueueLengthField = f;
		referentField = r;
		try {
			REF_TYPE_COUNT = new CompositeType("ReferenceServiceByTypeCount", "Defines a count of cleared references for a specific type", new String[]{"Type", "Count"}, new String[]{"The type name of tyhe reference", "The number of cleared references"}, new OpenType[]{SimpleType.STRING, SimpleType.LONG});
			TABULAR_REF_TYPE_COUNT = new TabularType("ReferenceServiceTypeCountSummary", "Defines a count of cleared references for a all cleared types", REF_TYPE_COUNT, new String[]{"Type"});
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * <p>Title: ReferenceProvider</p>
	 * <p>Description: Defines a class that can supply a new reference instance</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jmx.util.reference.ReferenceService.ReferenceProvider</code></p>
	 * @param <T> The reference type
	 */
	public static interface ReferenceProvider<T extends Reference<Object>> {
		/**
		 * Creates a new reference and sets the referent in it
		 * @param referent The referent
		 * @param onEnqueueTask An optional task to run when the reference is enqueued
		 * @return the reference
		 */
		public T newReference(Object referent, Runnable onEnqueueTask);
	}
	
	/**
	 * <p>Title: ReferenceType</p>
	 * <p>Description: </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jmx.util.reference.ReferenceService.ReferenceType</code></p>
	 */
	public static enum ReferenceType implements ReferenceProvider<Reference<Object>> {
		/** A soft reference */
		SOFT(new ReferenceProvider<SoftReference<Object>>(){ 
			public SoftReference<Object> newReference(Object referent, Runnable onEnqueueTask) { 
				return ReferenceService.getInstance().newSoftReference(referent, onEnqueueTask); 
			}
		}),		
		/** A weak reference */
		WEAK(new ReferenceProvider<WeakReference<Object>>(){ 
			public WeakReference<Object> newReference(Object referent, Runnable onEnqueueTask) { 
				return ReferenceService.getInstance().newWeakReference(referent, onEnqueueTask); 
			}
		}),
		/** A phantom reference */
		PHANTOM(new ReferenceProvider<PhantomReference<Object>>(){ 
			public PhantomReference<Object> newReference(Object referent, Runnable onEnqueueTask) { 
				return ReferenceService.getInstance().newPhantomReference(referent, onEnqueueTask); 
			}
		});
		
		private ReferenceType(ReferenceProvider<?> provider) {
			this.provider = provider;
		}
		
		private final ReferenceProvider<?> provider;

		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.reference.ReferenceService.ReferenceProvider#newReference(java.lang.Object, java.lang.Runnable)
		 */
		@Override
		public Reference<Object> newReference(Object referent, Runnable onEnqueueTask) {
			return provider.newReference(referent, onEnqueueTask);
		}
	}
	
	
	/**
	 * Acquires the singleton ReferenceService instance
	 * @return the singleton ReferenceService instance
	 */
	public static ReferenceService getInstance() {		
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ReferenceService();
					
					instance.threadPool = new JMXManagedThreadPool(THREAD_POOL_OBJECT_NAME, "ReferenceService", 2, 10, 5000, 60000, 100, 99, false);
					instance.threadPool.setRejectedExecutionHandler(new JMXManagedThreadPool.CallerRunsPolicy());					
					JMXHelper.registerMBean(instance, OBJECT_NAME);
					JMXHelper.registerMBean(instance.threadPool, THREAD_POOL_OBJECT_NAME);
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new ReferenceService
	 */
	private ReferenceService() {		
		refQueueThread = new Thread(this, getClass().getSimpleName() + "RefQueueThread");
		refQueueThread.setDaemon(true);
		refQueueThread.start();		
	}
	

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceServiceMXBean#getQueueDepth()
	 */
	public long getQueueDepth() {
		if(refQueueLengthField!=null) {
			try {
				return refQueueLengthField.getLong(refQueue);
			} catch (Exception ex) {
				return -1L;
			}
		}
		return -1L;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceServiceMXBean#getClearedRefCount()
	 */
	public long getClearedRefCount() {
		return clearedRefCount.longValue();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceServiceMXBean#resetStats()
	 */
	public void resetStats() {		
		clearingErrors.reset();
		clearedRefCount.reset();
		for(ReferenceTypeCountMBean rtc: countsByType.values()) {
			rtc.reset();
		}
	}
	

	/**
	 * Updates the count for the passed class name
	 * @param className The name of class of a cleared reference
	 */
	private void updateTypeCount(String className) {
		if(className==null || className.trim().isEmpty()) return;
		ReferenceTypeCountMBean rtc = countsByType.get(className);
		if(rtc==null) {
			synchronized(countsByType) {
				rtc = countsByType.get(className);
				if(rtc==null) {
					rtc = new ReferenceTypeCount(className);
					countsByType.put(className, rtc);
				}
			}
		}
		rtc.increment();
	}

	
	/** The thread pool timing pre-task */
	private final Runnable preTask = new Runnable() {
		public void run() {
			taskStartTime.get()[0] = System.nanoTime();
		}
	};
	
	/** The thread pool timing post-task */
	private final Runnable postTask = new Runnable() {
		public void run() {			
			clearStats.insert(System.nanoTime() - taskStartTime.get()[0]);			
		}
	};
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while(true) {
			try {
				final Object removed = refQueue.remove();
				if(removed==null) continue;
				clearedRefCount.increment();	
								
				if(removed instanceof ReferenceRunnable) {
					final ReferenceRunnable rr = (ReferenceRunnable)removed;
					updateTypeCount(rr.getName());
					unClearedRefCount.decrement();
//					System.out.println(">>>>>>> Dequeued [" + removed.getClass().getName() + "]");
					if(rr.getClearedRunnable()!=null) {
						threadPool.submit(new Runnable(){
							public void run() {
								rr.run();
//								System.out.println(">>>>>>> Executed Clear Task [" + removed.getClass().getName() + "]");
							}
						}, preTask, postTask, this);
					}					
				}
			} catch (Throwable t) {
				t.printStackTrace(System.err);
				clearingErrors.increment();
				if(Thread.interrupted()) Thread.interrupted();
			}
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceServiceMXBean#getCountsByTypes()
	 */
	@Override
	public ReferenceTypeCountMBean[] getCountsByTypes() {
		return countsByType.values().toArray(new ReferenceTypeCountMBean[0]);
	}
	
	public Map<String, ReferenceTypeCountMBean> getCountTT() {
		return new HashMap<String, ReferenceTypeCountMBean>(countsByType);
	}
	
	
	/**
	 * Returns the reference queue
	 * @param forType The type to get the ref queue for. (Unused, just for generics cleanliness)
	 * @return the reference queue
	 */
	@SuppressWarnings("unchecked")
	public <T> ReferenceQueue<T> getReferenceQueue(Class<? extends T> forType) {
		return (ReferenceQueue<T>) refQueue;
	}
	
	/**
	 * Creates a new reference of the type defined in the passed refType
	 * @param refType The type of reference to create
	 * @param referent The referent which when enqueued will trigger the passed task
	 * @param onEnqueueTask the task to run when the referent becomes reachable
	 * @return the reference
	 */
	@SuppressWarnings("unchecked")
	public <T> Reference<T> newReference(ReferenceType refType, T referent, Runnable onEnqueueTask) {
		return (Reference<T>) refType.newReference(referent, onEnqueueTask);
	}
	
	/**
	 * Creates a new phantom reference
	 * @param referent The referent which when enqueued will trigger the passed task
	 * @param onEnqueueTask the task to run when the referent becomes phantom reachable
	 * @return the reference
	 */
	@SuppressWarnings("unchecked")
	public <T> PhantomReference<T> newPhantomReference(T referent, Runnable onEnqueueTask) {
		return (PhantomReference<T>) new PhantomReferenceWrapper(referent, onEnqueueTask);
	}
	
	/**
	 * Creates a new weak reference
	 * @param referent The referent which when enqueued will trigger the passed task
	 * @param onEnqueueTask the task to run when the referent becomes weakly reachable
	 * @return the reference
	 */
	@SuppressWarnings("unchecked")
	public <T> WeakReference<T> newWeakReference(T referent, Runnable onEnqueueTask) {
		return (WeakReference<T>) new WeakReferenceWrapper(referent, onEnqueueTask);
	}
	
	/**
	 * Creates a new soft reference
	 * @param referent The referent which when enqueued will trigger the passed task
	 * @param onEnqueueTask the task to run when the referent becomes softly reachable
	 * @return the reference
	 */
	@SuppressWarnings("unchecked")
	public <T> SoftReference<T> newSoftReference(T referent, Runnable onEnqueueTask) {
		return (SoftReference<T>) new SoftReferenceWrapper(referent, onEnqueueTask);
	}
	
//	public static final ThreadLocal<SwapableReferentWeakReference<?>> swap = new ThreadLocal<SwapableReferentWeakReference<?>>(); 
//	private static Object tempReferent = new Object();
//	public static final <T> SwapableReferentWeakReference<T> getLastAllocated() {
//		SwapableReferentWeakReference<T> swapRef = null;
//		try {
//			swapRef = (SwapableReferentWeakReference<T>) swap.get();
//			swap.remove();			
//		} catch (Exception ex) {
//			ex.printStackTrace(System.err);
//		}
//		return swapRef;
//	}
//	
//	public <T> SwapableReferentWeakReference<T> newSwapableReferentWeakReference() {
//		return new SwapableReferentWeakReference<T>();
//	}
//	
//	public class SwapableReferentWeakReference<T> extends WeakReference<T> implements ReferenceRunnable {
//		private Runnable onClearTask = null;
//		
//		public SwapableReferentWeakReference() {
//			super((T) tempReferent, refQueue);
//			swap.set(this);
//			this.onClearTask = onClearTask;
//		}
//		
//		public void swap(T referent) {
//			try {
//				referentField.set(this, referent);
//			} catch (Exception ex) {
//				throw new RuntimeException(ex);
//			}
//		}
//		
//		public void setRunnable(Runnable onClearTask) {
//			this.onClearTask = onClearTask;
//		}
//
//		@Override
//		public void run() {
//			if(onClearTask!=null) {
//				onClearTask.run();
//			}
//			
//		}
//
//		@Override
//		public Runnable getClearedRunnable() {
//			return onClearTask;
//		}
//		
//	}

	
	
    private class PhantomReferenceWrapper extends PhantomReference<Object> implements ReferenceRunnable {
    	private final Runnable runOnClear;
    	private final String name;
    	public Runnable getClearedRunnable() {
    		return runOnClear;
    	}
    	public void run() {
    		if(runOnClear!=null) {
    			runOnClear.run();
    		}
    	}
    	/**
		 * Creates a new PhantomReferenceWrapper
		 * @param referent The phantom referent
		 * @param onEnqueueTask a task to fire when the reference is cleared
		 */
		public PhantomReferenceWrapper(final Object referent, final Runnable onEnqueueTask) {
			super(referent, refQueue);
			runOnClear = onEnqueueTask;
			name = referent.getClass().getName();
			unClearedRefCount.increment();
		}   
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.utils.ref.ReferenceRunnable#getName()
		 */
		@Override
		public String getName() {
			return name;
		}
    }
    
    private class WeakReferenceWrapper extends WeakReference<Object> implements ReferenceRunnable {
    	private final Runnable runOnClear;
    	private final String name;
    	public Runnable getClearedRunnable() {
    		return runOnClear;
    	}
    	public void run() {
    		if(runOnClear!=null) {
    			runOnClear.run();
    		}
    	}
    	/**
		 * Creates a new WeakReferenceWrapper
		 * @param referent The weak referent
		 * @param onEnqueueTask a task to fire when the reference is cleared
		 */
		public WeakReferenceWrapper(final Object referent, final Runnable onEnqueueTask) {
			super(referent, refQueue);
			runOnClear = onEnqueueTask;
			name = referent.getClass().getName();
			unClearedRefCount.increment();
		}    	
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.utils.ref.ReferenceRunnable#getName()
		 */
		@Override
		public String getName() {
			return name;
		}
    }
    
    private class SoftReferenceWrapper extends SoftReference<Object> implements ReferenceRunnable {
    	private final Runnable runOnClear;
    	private final String name;
    	public Runnable getClearedRunnable() {
    		return runOnClear;
    	}
    	public void run() {
    		if(runOnClear!=null) {
    			runOnClear.run();
    		}
    	}
    	/**
		 * Creates a new SoftReferenceWrapper
		 * @param referent The soft referent
		 * @param onEnqueueTask a task to fire when the reference is cleared
		 */
		public SoftReferenceWrapper(final Object referent, final Runnable onEnqueueTask) {
			super(referent, refQueue);
			runOnClear = onEnqueueTask;
			name = referent.getClass().getName();
			unClearedRefCount.increment();
		}    	
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.utils.ref.ReferenceRunnable#getName()
		 */
		@Override
		public String getName() {
			return name;
		}
    }

    /**
     * {@inheritDoc}
     * @see org.helios.jmx.util.reference.ReferenceServiceMXBean#getErrors()
     */
    @Override
    public long getErrors() {
		return clearingErrors.longValue();
	}
    
    /**
     * {@inheritDoc}
     * @see org.helios.jmx.util.reference.ReferenceServiceMXBean#getCount()
     */
    @Override
    public long getCount() {
    	return clearedRefCount.longValue();
    }

    /**
     * {@inheritDoc}
     * @see org.helios.jmx.util.reference.ReferenceServiceMXBean#getAverage()
     */
    @Override
    public double getAverage() {
		return clearStats.avg();
	}

    /**
     * {@inheritDoc}
     * @see org.helios.jmx.util.reference.ReferenceServiceMXBean#getMaximum()
     */
    @Override
    public double getMaximum() {
		return clearStats.max();
	}


    /**
     * {@inheritDoc}
     * @see org.helios.jmx.util.reference.ReferenceServiceMXBean#getMinimum()
     */
    @Override
	public double getMinimum() {
		return clearStats.min();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		clearingErrors.increment();
	}
    
    

}
