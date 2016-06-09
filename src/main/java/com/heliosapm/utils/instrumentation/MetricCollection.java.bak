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
package com.heliosapm.utils.instrumentation;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.heliosapm.utils.enums.BitMasked;
import com.heliosapm.utils.enums.IntBitMaskedEnum;
import com.heliosapm.utils.enums.Primitive;
import com.heliosapm.utils.instrumentation.measure.AbstractDeltaMeasurer;
import com.heliosapm.utils.instrumentation.measure.DefaultMeasurer;
import com.heliosapm.utils.instrumentation.measure.DelegatingMeasurer;
import com.heliosapm.utils.instrumentation.measure.InvocationMeasurer;
import com.heliosapm.utils.instrumentation.measure.Measurer;
import com.heliosapm.utils.instrumentation.measure.NoopThreadAllocatedBytesReader;
import com.heliosapm.utils.instrumentation.measure.ThreadAllocatedBytesReader;
import com.heliosapm.utils.time.SystemClock;

/**
 * <p>Title: MetricCollection</p>
 * <p>Description: A functional enum of metrics collectors</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.instrumentation.MetricCollection</code></p>
 */

public enum MetricCollection implements ICollector<MetricCollection>, BitMasked {
	/** The elapsed system cpu time in microseconds */
	SYS_CPU(false, true, "CPU Time (\u00b5s)", "syscpu", "CPU Thread Execution Time", new DefaultSysCpuMeasurer(0), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The elapsed user mode cpu time in microseconds */
	USER_CPU(false, true, "CPU Time (\u00b5s)", "usercpu", "CPU Thread Execution Time In User Mode", new DefaultUserCpuMeasurer(1), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The number of thread waits on locks or other concurrent barriers */
	WAIT_COUNT(false, true, "Thread Waits", "waits", "Thread Waiting On Notification Count", new DefaultWaitCountMeasurer(2), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The thread wait time on locks or other concurrent barriers */
	WAIT_TIME(false, true, "Thread Wait Time (ms)", "waittime", "Thread Waiting On Notification Time", new DefaultWaitTimeMeasurer(3), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The number of thread waits on synchronization monitors */
	BLOCK_COUNT(false, true, "Thread Blocks", "blocks", "Thread Waiting On Monitor Entry Count", new DefaultBlockCountMeasurer(4), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The thread wait time on synchronization monitors  */
	BLOCK_TIME(false, true, "Thread Block Time (ms)", "blocktime", "Thread Waiting On Monitor Entry Time", new DefaultBlockTimeMeasurer(5), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The elapsed wall clock time in ns. */
	ELAPSED(true, false, "Elapsed Time (ns)", "elapsed", "Elapsed Execution Time", new DefaultElapsedTimeMeasurer(6), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The elapsed compilation time in ms */
	COMPILATION(false, false, "Elapsed Compilation Time (ms)", "compilation", "Elapsed Compilation Time", new DefaultCompilationTimeMeasurer(7), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The number of threads concurrently passing through the instrumented joinpoint  */
	METHOD_CONCURRENCY(false, false, "Method Concurrent Thread Execution Count", "methconcurrent", "Number Of Threads Executing Concurrently In The Same Method", new DelegatingMeasurer(new DefaultMeasurer(8)), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The total number of invocations of the instrumented joinpoint  */
	INVOCATION_COUNT(true, false, "Method Invocations", "invcount", "Method Invocation Count", new InvocationMeasurer(9), DataStruct.getInstance(Primitive.LONG, 1, 0), "Count"),
	/** The total number of successful invocations of the instrumented method */
	RETURN_COUNT(false, false, "Method Returns", "retcount", "Method Return Count", new DelegatingMeasurer(new DefaultMeasurer(10)), DataStruct.getInstance(Primitive.LONG, 1, 0), "Count"),
	/** The total number of invocations of the instrumented method that terminated on an exception */
	EXCEPTION_COUNT(false, false, "Method Invocation Exceptions ", "exccount", "Method Invocation Exception Count", new DelegatingMeasurer(new DefaultMeasurer(11)), DataStruct.getInstance(Primitive.LONG, 1, 0), "Count"),
	/** The number of bytes allocated by the calling thread in bytes */
	BYTES_ALLOCATED(false, false, "Bytes Allocated (bytes)", "allocation", "Memory Allocated", new DefaultThreadAllocatedBytesTimeMeasurer(12), DataStruct.getInstance(Primitive.LONG, 1, 0L), "Bytes");
	
	
	
//	final boolean defaultOn, final boolean isRequiresTI, 
//	final String unit, final String shortName, final String description, 
//	final Measurer measurer, final DataStruct ds, final int[] dependencies, 
//	final String...subNames) {	
	
	

	public static void main(String[] args) {
		log("MC");
		for(MetricCollection mc: values) {
			log(mc.name() + ":" + mc.baseMask);
		}
		final long[] st = methodEnter(allMetricsMask);
		SystemClock.sleep(100);
		final long[] rez = methodExit(st);
		log("Results:" + printResults(rez));
		log("===================");
		
	}
	
	

	
	/**
	 * The private enum ctor
	 * @param defaultOn If true, this metric is turned on by default
	 * @param isRequiresTI indicates if the metric's measurer will require a ThreadInfo instance
	 * @param unit the unit of the metric
	 * @param shortName a short name for the metric
	 * @param description the description of the metric
	 * @param measurer the measurement capturing procedure for this metric
	 * @param ds The data struct describing the memory allocation required for collected metrics
	 * @param dependencies The ordinals of this collector's dependencies
	 * @param subNames The metric sub names
	 */
	private MetricCollection(final boolean defaultOn, final boolean isRequiresTI, 
			final String unit, final String shortName, final String description, final Measurer measurer, final DataStruct ds, final int[] dependencies, final String...subNames) {
		this.baseMask = IntBitMaskedEnum.POW2[ordinal()];
		this.defaultOn = defaultOn;
		this.isRequiresTI = isRequiresTI;
		this.unit = unit;
		this.shortName = shortName;
		this.description = description;
		this.measurer = measurer;
		this.ds = ds;
		this.subNames = subNames;
		this.dependencies = dependencies;
		if(ds.size != subNames.length) {
			throw new IllegalArgumentException("DataStruct Size [" + ds.size + "] was not the same as sub names length [" + subNames.length + "] for [" + name() + "]. Programmer Error.");
		}
	}
	
	/**
	 * The private enum ctor with no dependencies
	 * @param defaultOn If true, this metric is turned on by default
	 * @param isRequiresTI indicates if the metric's measurer will require a ThreadInfo instance
	 * @param unit the unit of the metric
	 * @param shortName a short name for the metric
	 * @param description the description of the metric
	 * @param measurer the measurement capturing procedure for this metric
	 * @param ds The data struct describing the memory allocation required for collected metrics
	 * @param subNames The metric sub names
	 */
	private MetricCollection(final boolean defaultOn, final boolean isRequiresTI, 
			final String unit, final String shortName, final String description, final Measurer measurer, final DataStruct ds, final String...subNames) {
		this(defaultOn, isRequiresTI, unit, shortName, description, measurer, ds, new int[0], subNames);
	}
	
	private static final MetricCollection[] values = values();
	/** The total number of collectors */
	public static final int itemCount;
	/** The location in the values array where the bit mask can be found */
	public static final int bitMaskIndex;
	/** The location in the open close indicator can be found */
	public static final int openCloseIndex;
	
	/** The bitmask for all metrics enabled */
	public static final int allMetricsMask;
	/** The bitmask for default metrics enabled */
	public static final int defaultMetricsMask;
	/** The pre-apply collectors, needed to be done before the others,  in the order specified */
	public static final Set<MetricCollection> preApplies = EnumSet.of(MetricCollection.INVOCATION_COUNT);
	/** The base bitMasks for metrics that require a ThreadInfo for measurement */
	private static final int[] threadInfoRequiredMasks;
	/** the current measurement ThreadInfo */
	private static final ThreadLocal<ThreadInfo> currentThreadInfo = new ThreadLocal<ThreadInfo>();
	/** the JVM ThreadMXBean */
	private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	/** The internal code */
	public final int baseMask;
	/** indicates if this metric is turned on by default */
	public final boolean defaultOn;
	/** indicates if the metric's measurer will require a ThreadInfo instance */
	private final boolean isRequiresTI;
	/** The metric unit */
	private final String unit;	
	/** The metric short name */
	private final String shortName;
	/** The metric description */
	private final String description;
	/** the measurer */
	private final Measurer measurer;
	/** The data struct */
	public final DataStruct ds;
	/** The metric sub names */
	private final String[] subNames;
	/** The ordinals of this collector's dependencies */
	private final int[] dependencies;
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	static {
		itemCount = values.length;
		bitMaskIndex = values.length;
		openCloseIndex = bitMaskIndex +1;
		final List<int[]> tis = new ArrayList<int[]>(); 
		int _allMaskIndex = 0, _defaultMaskIndex = 0;
		for(MetricCollection mi: values) {
			_allMaskIndex = mi.enableFor(_allMaskIndex);
			if(mi.defaultOn) _defaultMaskIndex = mi.enableFor(_defaultMaskIndex);
			if(mi.isRequiresTI) tis.add(new int[]{mi.baseMask});
		}
		
		threadInfoRequiredMasks = new int[tis.size()];
		for(int i = 0; i < tis.size(); i++) {
			threadInfoRequiredMasks[i] = tis.get(i)[0];
		}
		allMetricsMask = _allMaskIndex;
		defaultMetricsMask = _defaultMaskIndex;
		threadMXBean.setThreadContentionMonitoringEnabled(true);
		threadMXBean.setThreadCpuTimeEnabled(true);
	}
	
	/**
	 * Determines if the passed bitMask requires a ThreadInfo for measurement.
	 * @param bitMask the bitMask to test
	 * @return true if the passed bitMask requires a ThreadInfo for measurement.
	 */
	public static boolean isRequiresTI(final int bitMask) {
		for(int m: threadInfoRequiredMasks) {
			if((bitMask & m)==m) return true;
		}
		return false;
	}	
	
	

	// ==================================================================================================
	// ==================================================================================================
	//  FIXME:  methodEnter and methodExit should be compiled
	// ==================================================================================================
	// ==================================================================================================
	
	/**
	 * Executes the actual measurement and updates the values array accordingly.
	 * If the measurement returned is -1, the measurement failed and the bitMask at
	 * the end of the values array will be updated to turn off the failed metric.
	 * @param values The values array
	 * @param m the MetricCollection being measured
	 * @param isStart true if the collection is starting, false if it is stopping.
	 * @return the updated values array
	 */
	private static long[] executeMeasurement(final long[] values, final MetricCollection m, final boolean isStart) {
		m.measurer.measure(isStart, values);
		return values;
	}
	
	
	/**
	 * Captures method entry metric baselines.
	 * @param bitMask the bitMask indicating which metrics are enabled.
	 * @return and array of thread stat baseline values.
	 */
	public static long[] methodEnter(int bitMask) {
		long[] values = new long[itemCount+2];
		values[bitMaskIndex] = bitMask;
		values[openCloseIndex] = 1;
		if(isRequiresTI(bitMask)) {
			currentThreadInfo.set(threadMXBean.getThreadInfo(Thread.currentThread().getId()));
		}			
		for(MetricCollection m: MetricCollection.values()) {
			if(m.isEnabled(bitMask)) {
				values = executeMeasurement(values, m, true);
			}
		}
		currentThreadInfo.remove();
		return values;		
	}
	
	/**
	 * Captures method normal exit metrics. If {@link #RETURN_COUNT} is enabled, it will be incremented.
	 * @param values The method entry caputed baseline
	 * @return and array of thread stat baseline values.
	 */
	public static long[] methodExit(long[] values) {
		int bitMask = (int)values[bitMaskIndex];
		if(isRequiresTI(bitMask)) {
			currentThreadInfo.set(threadMXBean.getThreadInfo(Thread.currentThread().getId()));
		}			
		for(MetricCollection m: MetricCollection.values()) {
			if(m.isEnabled(bitMask)) {
				values = executeMeasurement(values, m, false);
			}
		}
		if(RETURN_COUNT.isEnabled(bitMask)) {
			values[RETURN_COUNT.ordinal()]++;
		}
		currentThreadInfo.remove();
		values[openCloseIndex] = 0;
		return values;				
	}
	
	/**
	 * Captures exit metrics for an exception throwing method exit. If {@link #EXCEPTION_COUNT} is enabled, it will be incremented.
	 * @param values The method entry caputed baseline
	 * @return and array of thread stat baseline values.
	 */
	public static long[] methodException(long[] values) {
		int bitMask = (int)values[bitMaskIndex];
		if(isRequiresTI(bitMask)) {
			currentThreadInfo.set(threadMXBean.getThreadInfo(Thread.currentThread().getId()));
		}			
		for(MetricCollection m: MetricCollection.values()) {
			if(m.isEnabled(bitMask)) {
				values = executeMeasurement(values, m, false);
			}
		}
		if(EXCEPTION_COUNT.isEnabled(bitMask)) {
			values[EXCEPTION_COUNT.ordinal()]++;
		}
		currentThreadInfo.remove();
		values[openCloseIndex] = 0;
		return values;				
	}
	
	public static String printResults(final long[] results) {
		final StringBuilder b = new StringBuilder();
		for(MetricCollection mc: values) {
			b.append("\n\t").append(mc.name()).append(" : ").append(results[mc.ordinal()]);
		}
		return b.toString();
	}
	
	
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.ICollector#getMask()
	 */
	@Override
	public int getMask() {		
		return baseMask;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.ICollector#isEnabled(int)
	 */
	@Override
	public boolean isEnabled(final int mask) {		
		return (mask & baseMask) == baseMask;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.enums.BitMasked#enableFor(int)
	 */
	@Override
	public int enableFor(final int mask) {
		return mask | baseMask;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.enums.BitMasked#disableFor(int)
	 */
	@Override
	public int disableFor(final int mask) {
		return baseMask & ~mask;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.ICollector#collectors()
	 */
	@Override
	public ICollector<?>[] collectors() {
		return values();
	}

	@Override
	public int getBitMaskOf(String... collectorNames) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void preFlush(long address, int bitMask) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getBitMaskIndex() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Measurer getMeasurer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDefaultOn() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int enable(int bitMask) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getUnit() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getShortName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataStruct getDataStruct() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getSubMetricNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getAllocationFor(int bitMask) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Set<MetricCollection> getEnabledCollectors(int bitmask) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resetMemSpace(long address, int bitmask) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long[][] getDefaultValues(int bitMask) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MetricCollection[] getPreApplies(int bitmask) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isPreApply() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void apply(long address, long[] collectedValues) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void preApply(long address, long[] collectedValues) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<MetricCollection, Long> getOffsets(int bitMask) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getEnabledNames(int bitMask) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	/**
	 * <p>Title: DefaultSysCpuMeasurer</p>
	 * <p>Description: Default system cpu time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 * <p><code>com.heliosapm.jmx.metrics.DefaultSysCpuMeasurer</code></p>
	 */
	public static class DefaultSysCpuMeasurer extends AbstractDeltaMeasurer {
		/**
		 * Creates a new DefaultSysCpuMeasurer
		 * @param metricOrdinal
		 */
		public DefaultSysCpuMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			try {
				return TimeUnit.MICROSECONDS.convert(threadMXBean.getCurrentThreadCpuTime(), TimeUnit.NANOSECONDS);
			} catch (Exception e) {
				return -1L;
			}			
		}
	}
	/**
	 * <p>Title: DefaultUserCpuMeasurer</p>
	 * <p>Description: Default user cpu time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 * <p><code>com.heliosapm.jmx.metrics.DefaultUserCpuMeasurer</code></p>
	 */
	public static class DefaultUserCpuMeasurer extends AbstractDeltaMeasurer {
		/**
		 * Creates a new DefaultUserCpuMeasurer
		 * @param metricOrdinal
		 */
		public DefaultUserCpuMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			try {
				return TimeUnit.MICROSECONDS.convert(threadMXBean.getCurrentThreadUserTime(), TimeUnit.NANOSECONDS);
			} catch (Exception e) {
				return -1L;
			}			
		}
	}
	
	/**
	 * <p>Title: DefaultWaitCountMeasurer</p>
	 * <p>Description: Default wait time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 * <p><code>com.heliosapm.jmx.metrics.DefaultWaitCountMeasurer</code></p>
	 */
	public static class DefaultWaitCountMeasurer extends AbstractDeltaMeasurer  {
		/**
		 * Creates a new DefaultWaitCountMeasurer
		 * @param metricOrdinal
		 */
		public DefaultWaitCountMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			return currentThreadInfo.get().getWaitedCount();
		}
	}
	
	/**
	 * <p>Title: DefaultWaitTimeMeasurer</p>
	 * <p>Description: Default wait time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 * <p><code>com.heliosapm.jmx.metrics.DefaultWaitTimeMeasurer</code></p>
	 */
	public static class DefaultWaitTimeMeasurer extends AbstractDeltaMeasurer {
		/**
		 * Creates a new DefaultWaitCountMeasurer
		 * @param metricOrdinal
		 */
		public DefaultWaitTimeMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			return currentThreadInfo.get().getWaitedTime();
		}
	}
	/**
	 * <p>Title: DefaultBlockCountMeasurer</p>
	 * <p>Description: Default block count measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 * <p><code>com.heliosapm.jmx.metrics.DefaultBlockCountMeasurer</code></p>
	 */
	public static class DefaultBlockCountMeasurer extends AbstractDeltaMeasurer {
		/**
		 * Creates a new DefaultBlockCountMeasurer
		 * @param metricOrdinal
		 */
		public DefaultBlockCountMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			return currentThreadInfo.get().getBlockedCount();
		}
	}

	/**
	 * <p>Title: DefaultBlockTimeMeasurer</p>
	 * <p>Description: Default block time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.instrumentation.MetricCollection.DefaultBlockTimeMeasurer</code></p>
	 */
	public static class DefaultBlockTimeMeasurer extends AbstractDeltaMeasurer {
		/**
		 * Creates a new DefaultBlockTimeMeasurer
		 * @param metricOrdinal the metric ordinal
		 */
		public DefaultBlockTimeMeasurer(final int metricOrdinal) {
			super(metricOrdinal);
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.utils.instrumentation.measure.AbstractDeltaMeasurer#sample()
		 */
		@Override
		protected long sample() {
			return currentThreadInfo.get().getBlockedTime();
		}
	}
	
	/**
	 * <p>Title: DefaultThreadAllocatedBytesTimeMeasurer</p>
	 * <p>Description: Default thread memory allocation measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.instrumentation.MetricCollection.DefaultThreadAllocatedBytesTimeMeasurer</code></p>
	 */
	public static class DefaultThreadAllocatedBytesTimeMeasurer extends AbstractDeltaMeasurer {
		/** The concrete memory measurer which may fail class loading */
		public static final String IMPL = "com.heliosapm.utils.instrumentation.measure.ThreadMemAlloc";
		private static final ThreadAllocatedBytesReader TXR;
		
		static {
			ThreadAllocatedBytesReader reader = null;
			try {
				Class<?> clazz = Class.forName(IMPL);
				reader = (ThreadAllocatedBytesReader)clazz.newInstance();
			} catch (Throwable t) {
				reader = new NoopThreadAllocatedBytesReader();
			}
			TXR = reader;
		}
		
		
		/**
		 * Creates a new DefaultThreadAllocatedBytesTimeMeasurer
		 * @param metricOrdinal the metric ordinal
		 */
		public DefaultThreadAllocatedBytesTimeMeasurer(final int metricOrdinal) {
			super(metricOrdinal);
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.utils.instrumentation.measure.AbstractDeltaMeasurer#sample()
		 */
		@Override
		protected long sample() {
			return TXR.getThreadAllocatedBytes(Thread.currentThread().getId());
		}
	}
	
	
	/**
	 * <p>Title: DefaultElapsedTimeMeasurer</p>
	 * <p>Description: Default elapsed time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 * <p><code>com.heliosapm.jmx.metrics.DefaultElapsedTimeMeasurer</code></p>
	 */
	public static class DefaultElapsedTimeMeasurer extends AbstractDeltaMeasurer {
		//private static final long startTime;
		
		static {
			long min = Long.MAX_VALUE;
			long max = Long.MIN_VALUE;
			long last = -1;
			for(int i = 0; i < 10000; i++) {
				long start = System.nanoTime();
				long elapsed = System.nanoTime()-start;
				if(elapsed<min) min = elapsed;
				if(elapsed>max) max = elapsed;
				last = elapsed;
			}
			log("HIGH REZ Clock Warmup. Min:" + min + "  Max:" + max + "  Last:" + last);
		}
		/**
		 * Creates a new DefaultBlockTimeMeasurer
		 * @param metricOrdinal
		 */
		public DefaultElapsedTimeMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			return System.nanoTime();
		}		
	}
	
	/**
	 * <p>Title: DefaultCompilationTimeMeasurer</p>
	 * <p>Description: Default elapsed compilation time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.instrumentation.MetricCollection.DefaultCompilationTimeMeasurer</code></p>
	 */
	public static class DefaultCompilationTimeMeasurer  extends AbstractDeltaMeasurer {
		private final CompilationMXBean cmx = ManagementFactory.getCompilationMXBean();
		
		/**
		 * Creates a new DefaultCompilationTimeMeasurer
		 * @param metricOrdinal
		 */
		public DefaultCompilationTimeMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			return cmx.getTotalCompilationTime();
		}
	}



	

}
