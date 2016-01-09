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
package com.heliosapm.utils.enums;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;

/**
 * <p>Title: SpaceUnit</p>
 * <p>Description: A byte unti enum, similar to {@link java.util.concurrent.TimeUnit} but for space/memory usage</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.enums.SpaceUnit</code></p>
 */

public enum SpaceUnit {
	BYTES("b", 0),
	KILOBYTES("kB", 1),
	MEGABYTES("MB", 2),
	GIGABYTES("GB", 3),
	TERABYTES("TB", 4),
	PETABYTES("TB", 5),
	EXABYTES("EB", 6),
	ZETTABYTES("ZB", 7);
//	YOTTABYTES("YB", 8);  after this we have to go to doubles
	
	private SpaceUnit(final String symbol, final int magnitude) {
		this.symbol = symbol;
		this.byteSize = magnitude==0 ? 1 : ((long)Math.pow(1024, magnitude));
		this.magnitude = magnitude;
	}
	
	private final String symbol;
	private final long byteSize;
	private final int magnitude;
	
	private static final String FMT = "#,###,###,###,###,###,###";
	private static final ThreadLocal<WeakReference<DecimalFormat>> formatCache = new ThreadLocal<WeakReference<DecimalFormat>>() {
		@Override
		protected WeakReference<DecimalFormat> initialValue() {			
			return new WeakReference<DecimalFormat>(new DecimalFormat(FMT));
		}
	};
	
	private static DecimalFormat format() {
		DecimalFormat d = formatCache.get().get();
		if(d==null) {
			d = new DecimalFormat(FMT);
			formatCache.set(new WeakReference<DecimalFormat>(d));			
		}
		return d;
	}
	
	/**
	 * Formats the passed number with comas
	 * @param n The number to format
	 * @return the formatted number
	 */
	public static String format(final Number n) {
		if(n==null) throw new IllegalArgumentException("The number to format was null");
		return format().format(n);
	}
	
	/**
	 * Returns the standard symbol for this space unit
	 * @return the standard symbol
	 */
	public String symbol() {
		return symbol;
	}
	
	/**
	 * The number of bytes in this unit of space
	 * @return the number of bytes
	 */
	public long byteSize() {
		return byteSize;
	}
	
	/**
	 * Returns the power of 1024 to derive the number of bytes in the unit, except for {@link #BYTES} which is zero
	 * @return the magnitude of the unit
	 */
	public int magnitude() {
		return magnitude;
	}
	
	public String toString() {
		return name() + "(" + symbol + "):" + format(byteSize);
	}
	
	public static void main(String args[]) {
		for(SpaceUnit s: SpaceUnit.values()) {
			log(s);
		}
		log("14 MB to kB:" + format(SpaceUnit.MEGABYTES.convert(14, SpaceUnit.KILOBYTES)) + "kB");
		log(format(SpaceUnit.MEGABYTES.convert(14274364, SpaceUnit.BYTES)) + "MB");
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
		
	}
	
	public long convert(long sourceSpace, SpaceUnit sourceUnit) {
		if(sourceUnit==null) throw new IllegalArgumentException("The source unit was null");
		return sourceUnit.byteSize * sourceSpace / byteSize;
	}
	
	public String fovert(long sourceSpace, SpaceUnit sourceUnit) {
		return format(convert(sourceSpace, sourceUnit)) + symbol;
	}
	
	
	
	
}
