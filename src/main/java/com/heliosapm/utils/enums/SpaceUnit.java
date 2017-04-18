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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Random;

/**
 * <p>Title: SpaceUnit</p>
 * <p>Description: A byte unit enum, similar to {@link java.util.concurrent.TimeUnit} but for space/memory usage</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.enums.SpaceUnit</code></p>
 */

public enum SpaceUnit {
	/** Bytes */
	BYTES("b", 0),
	/** Kilobytes */
	KILOBYTES("kB", 1),
	/** Megabytes */
	MEGABYTES("MB", 2),
	/** Gigabytes */
	GIGABYTES("GB", 3),
	/** Terabytes */
	TERABYTES("TB", 4),
	/** PetaBytes */
	PETABYTES("TB", 5),
	/** Exabytes */
	EXABYTES("EB", 6),
	/** Zettabytes */
	ZETTABYTES("ZB", 7);
//	YOTTABYTES("YB", 8);  after this we have to go to doubles
	
	private SpaceUnit(final String symbol, final int magnitude) {
		this.symbol = symbol;
		this.byteSize = magnitude==0 ? 1 : ((long)Math.pow(1024, magnitude));
		this.magnitude = magnitude;
		pickRange = (magnitude+1) *3;
	}
	
	private final String symbol;
	private final long byteSize;
	private final int magnitude;
	private final long pickRange;
	
	public static final DecimalFormat format = (DecimalFormat)DecimalFormat.getInstance();
	public static final DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
	public static final char sep = symbols.getDecimalSeparator();	
	public static final char com = symbols.getGroupingSeparator();
	
	private static final String FMT = String.format("#%s###%s###%s###%s###%s###%s###%s##", com, com, com, com, com, com, sep);
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
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
		return name() + "(" + symbol + "):" + format(byteSize);
	}
	
	public static SpaceUnit pickUnit(final double value) {
		final int len = new DecimalFormat("#0").format(Math.abs(value)).length();
		for(SpaceUnit s: values()) {
			if(len <= s.pickRange) return s;
		}
		return ZETTABYTES;
		
	}
	
	
	/**
	 * Converts the passed space value in this member's unit to the specified unit
	 * @param sourceSpace The space value to convert
	 * @param sourceUnit The unit to convert to
	 * @return the converted value
	 */
	public long convert(final long sourceSpace, final SpaceUnit sourceUnit) {
		if(sourceUnit==null) throw new IllegalArgumentException("The source unit was null");
		return sourceUnit.byteSize * sourceSpace / byteSize;
	}
	
	/**
	 * Converts the passed space value in this member's unit to the specified unit
	 * and returns the converted value as a double so that the decimal portion is preserved.
	 * @param sourceSpace The space value to convert
	 * @param sourceUnit The unit to convert to
	 * @param decimalPlaces The number of decimal places to round the result to
	 * @return the converted value
	 */
	public double dconvert(final double sourceSpace, final SpaceUnit sourceUnit, final int decimalPlaces) {
		if(sourceUnit==null) throw new IllegalArgumentException("The source unit was null");
		return round((sourceUnit.byteSize * sourceSpace / byteSize), decimalPlaces);
	}
	
	/**
	 * Converts the passed space value in this member's unit to the specified unit
	 * and returns the converted value as a 2 decimal spaces rounded double so that the decimal portion is preserved.
	 * @param sourceSpace The space value to convert
	 * @param sourceUnit The unit to convert to
	 * @return the converted value
	 */
	public double dconvert(final double sourceSpace, final SpaceUnit sourceUnit) {
		return dconvert(sourceSpace, sourceUnit, 2);
	}
	
	/**
	 * Rounds the passed double to the specified number of decimal places
	 * @param value The value to round
	 * @param places The number of decimal places to round to
	 * @return the rounded value
	 */
	public static double round(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}	
	
	
	/**
	 * Converts the passed space value in this member's unit to the specified unit
	 * and formats the output number with the according symbol
	 * @param sourceSpace The space value to convert
	 * @param sourceUnit The unit to convert to
	 * @return the formatted result
	 */
	public String fovert(long sourceSpace, SpaceUnit sourceUnit) {
		return format(convert(sourceSpace, sourceUnit)) + symbol;
	}
	
	/**
	 * Converts the passed space value in this member's unit to the specified unit
	 * and formats the output number with the according symbol
	 * @param sourceSpace The space value to convert
	 * @param sourceUnit The unit to convert to
	 * @return the formatted result
	 */
	public String fovert(double sourceSpace, SpaceUnit sourceUnit) {
		return format(dconvert(sourceSpace, sourceUnit)) + symbol;
	}
	
	
	public static void main(String args[]) {
		log("FMT:[" + FMT + "]");
		for(SpaceUnit s: SpaceUnit.values()) {
			log(s);
		}
		log("14 MB to kB:" + format(SpaceUnit.MEGABYTES.convert(14, SpaceUnit.KILOBYTES)) + "kB");
		log(format(SpaceUnit.MEGABYTES.convert(14274364, SpaceUnit.BYTES)) + "MB");
		final Random r = new Random(System.currentTimeMillis());
		final DecimalFormat df = new DecimalFormat("#0");
		for(int x = 0; x < 10; x++) {			
			double d = r.nextInt(Math.abs(r.nextInt()));
			SpaceUnit unit = pickUnit(d);
			log("[" + df.format(d) + "]:" + unit.fovert(d, BYTES));
			d = Math.abs(r.nextLong()%r.nextLong());
			unit = pickUnit(d);
			log("[" + df.format(d) + "]:" + unit.fovert(d, BYTES));
			d = Math.abs(r.nextInt(99999));
			unit = pickUnit(d);
			log("[" + df.format(d) + "]:" + unit.fovert(d, BYTES));			
			
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
		
	}
	
	
	
}
