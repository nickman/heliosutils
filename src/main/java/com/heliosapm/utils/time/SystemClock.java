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
package com.heliosapm.utils.time;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;



/**
 * <p>Title: SystemClock</p>
 * <p>Description: General purpose time and timings provider.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.time.SystemClock</code></p>
 */

public class SystemClock {
	
	/**
	 * Starts a new timer
	 * @return the elapsed time object on which elapsed times can be drawn
	 */
	public static ElapsedTime startClock() {
		return new ElapsedTime();
	}
	
	/**
	 * Returns the current time in ms.
	 * @return the current time in ms.
	 */
	public static long time() {
		return System.currentTimeMillis();
	}
	
	/**
	 * Returns the current time in milliseconds to second precision
	 * @return the second precision current timestamp in ms.
	 */
	public static long rtime() {
		return TimeUnit.MILLISECONDS.convert(TimeUnit.SECONDS.convert(time(), TimeUnit.MILLISECONDS), TimeUnit.SECONDS);
	}
	
	/**
	 * Returns a JDBC timestamp for the current time
	 * @return a JDBC timestamp for the current time
	 */
	public static java.sql.Timestamp getTimestamp() {
		return new java.sql.Timestamp(time());
	}
	
	/**
	 * Returns the current time in Unix Time (s.)
	 * @return the current time in Unix Time
	 */
	public static long unixTime() {
		return TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}
	
	
	/**
	 * Returns the relative time in ns.
	 * @return the relative time in ms.
	 */
	public static long timens() {
		return System.nanoTime();
	}
	
	
	
	
	/**
	 * <p>Title: ElapsedTime</p>
	 * <p>Description: An elapsed time reporter</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.time.SystemClock.ElapsedTime</code></p>
	 * TODO: Lots....
	 * Format multiple time units, avg units
	 * Lap times
	 */
	public static class ElapsedTime {
		/** The start time in ns. */
		public final long startNs;
		/** The last lap end time in ns. */
		public long endNs;
	
		/**
		 * Creates a new ElapsedTime
		 */
		private ElapsedTime(){
			startNs = System.nanoTime();
		}
		
		/**
		 * Returns the start time in ns.
		 * @return the start time in ns.
		 */
		public long startTime() {
			return startNs;
		}
		
		/**
		 * Returns the start time in ms.
		 * @return the start time in ms.
		 */
		public long startTimeMs() {
			return TimeUnit.MILLISECONDS.convert(startNs, TimeUnit.NANOSECONDS);
		}
		
		/**
		 * Returns the start time in s.
		 * @return the start time in s.
		 */
		public long startTimeS() {
			return TimeUnit.SECONDS.convert(startNs, TimeUnit.NANOSECONDS);
		}
		
		
		
		
		/** Some extended time unit entries */
		public static final Map<TimeUnit, String> UNITS;
		
		static {
			Map<TimeUnit, String> tmp =  new EnumMap<TimeUnit, String>(TimeUnit.class);
			tmp.put(TimeUnit.DAYS, "days");
			tmp.put(TimeUnit.HOURS, "hrs.");
			tmp.put(TimeUnit.MICROSECONDS, "\u00b5s.");
			tmp.put(TimeUnit.MILLISECONDS, "ms.");
			tmp.put(TimeUnit.MINUTES, "min.");
			tmp.put(TimeUnit.NANOSECONDS, "ns.");
			tmp.put(TimeUnit.SECONDS, "s.");
			UNITS = Collections.unmodifiableMap(tmp);			
		}
		
		
		
//		private ElapsedTime(boolean lap, long endTime) {
//			endNs = endTime;
//			startNs = timerStart.get()[0];
//			long[] lastLapRead = lapTime.get();
//			if(lastLapRead!=null) {
//				lastLapNs = lastLapRead[0];
//			}
//			if(lap) {
//				lapTime.set(new long[]{endTime});
//			} else {
//				timerStart.remove();
//				lapTime.remove();
//			}
//			elapsedNs = endNs-startNs;
//			elapsedMs = TimeUnit.MILLISECONDS.convert(elapsedNs, TimeUnit.NANOSECONDS);
//			if(lastLapNs!=-1L) {
//				elapsedSinceLastLapNs = endTime -lastLapNs;
//				elapsedSinceLastLapMs = TimeUnit.MILLISECONDS.convert(elapsedSinceLastLapNs, TimeUnit.NANOSECONDS);
//			}
//			 
//		}
		/**
		 * Returns the average elapsed time in ms. for the passed number of events
		 * @param cnt The number of events
		 * @return The average elapsed time in ms.
		 */
		public long avgMs(double cnt) {			
			return _avg(elapsed(TimeUnit.MILLISECONDS), cnt);
		}
		
		/**
		 * Returns the average elapsed time in ns. for the passed number of events
		 * @param cnt The number of events
		 * @return The average elapsed time in ns.
		 */
		public long avgNs(double cnt) {
			long elapsedNs = System.nanoTime()-startNs;
			return _avg(elapsedNs, cnt);
		}
		
		
		private long _avg(double time, double cnt) {
			if(time==0 || cnt==0 ) return 0L;
			double d = time/cnt;
			return Math.round(d);
		}
		
		/**
		 * Returns the elapsed time since start in ns.
		 * @return elapsed ns.
		 */
		public long elapsed() {
			return elapsed(TimeUnit.NANOSECONDS);
		}
		
		/**
		 * Returns the elapsed time since start in ms.
		 * @return elapsed ms.
		 */
		public long elapsedMs() {
			return elapsed(TimeUnit.MILLISECONDS);
		}
		
		/**
		 * Returns the elapsed time since start in s.
		 * @return elapsed s.
		 */
		public long elapsedS() {
			return elapsed(TimeUnit.SECONDS);
		}
		
		/**
		 * Returns the elapsed time since start in the passed unit
		 * @param unit The unit to report elapsed time in
		 * @return the elapsed time
		 */
		public long elapsed(TimeUnit unit) {
			long elapsedNs = System.nanoTime()-startNs;
			if(unit==null) unit = TimeUnit.NANOSECONDS;
			return unit.convert(elapsedNs, TimeUnit.NANOSECONDS);
		}
		
		/**
		 * Returns the decorated elapsed time since start in the passed unit
		 * @param unit The unit to report elapsed time in
		 * @return the decorated elapsed time 
		 */
		public String elapsedStr(TimeUnit unit) {
			long elapsedNs = System.nanoTime()-startNs;
			if(unit==null) unit = TimeUnit.NANOSECONDS;
			return new StringBuilder("[").append(unit.convert(elapsedNs, TimeUnit.NANOSECONDS)).append("] ").append(UNITS.get(unit)).toString();
		}

		/**
		 * Returns the decorated elapsed time since start in ns.
		 * @return the decorated elapsed time since start in ns.
		 */
		public String elapsedStr() {			
			return elapsedStr(TimeUnit.NANOSECONDS);
		}
		
		/**
		 * Returns the decorated elapsed time since start in ms.
		 * @return the decorated elapsed time since start in ms.
		 */
		public String elapsedStrMs() {			
			return elapsedStr(TimeUnit.MILLISECONDS);
		}

		public String printAvg(String unitName, double cnt) {
			endNs = System.nanoTime();
			long elapsedNs = endNs - startNs;
			long avgNs = _avg(elapsedNs, cnt);
			return String.format("Completed %s %s in %s ms.  AvgPer: %s ms/%s \u00b5s/%s ns.",
					cnt,
					unitName, 
					TimeUnit.MILLISECONDS.convert(elapsedNs, TimeUnit.NANOSECONDS),
					TimeUnit.MILLISECONDS.convert(avgNs, TimeUnit.NANOSECONDS),
					TimeUnit.MICROSECONDS.convert(avgNs, TimeUnit.NANOSECONDS),
					avgNs					
			);
		}
		
	}
	

}
