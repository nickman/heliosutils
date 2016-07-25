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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.heliosapm.utils.tuples.NVP;

/**
 * <p>Title: TimeUnitSymbol</p>
 * <p>Description: Add on to {@link TimeUnit} to provide common short names, symbols and decodes therein.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.enums.TimeUnitSymbol</code></p>
 */

public enum TimeUnitSymbol {
	/** The timeunit naming definition for {@link TimeUnit#NANOSECONDS} */
	NANOSECONDS(TimeUnit.NANOSECONDS, "ns"),
	/** The timeunit naming definition for {@link TimeUnit#MICROSECONDS} */
	MICROSECONDS(TimeUnit.MICROSECONDS, "\u00B5s"),
	/** The timeunit naming definition for {@link TimeUnit#MILLISECONDS} */
	MILLISECONDS(TimeUnit.MILLISECONDS, "ms"),
	/** The timeunit naming definition for {@link TimeUnit#SECONDS} */
	SECONDS(TimeUnit.SECONDS, "s"),
	/** The timeunit naming definition for {@link TimeUnit#MINUTES} */
	MINUTES(TimeUnit.MINUTES, "m"),
	/** The timeunit naming definition for {@link TimeUnit#HOURS} */
	HOURS(TimeUnit.HOURS, "h"),
	/** The timeunit naming definition for {@link TimeUnit#DAYS} */
	DAYS(TimeUnit.DAYS, "d");

	public static final Pattern PERIOD_PATTERN;
	
	public static void main(String[] args) {
		for(TimeUnitSymbol tus: values()) {
			System.out.println(tus.name() + ": [" + tus.shortName + "]");
		}
		for(TimeUnitSymbol tus: values()) {
			System.out.print(tus.shortName + "|");
		}

	}
	
	private TimeUnitSymbol(final TimeUnit unit, final String shortName) {
		this.unit = unit;
		this.shortName = shortName;
	}
	private static final TimeUnitSymbol[] values = values();
	private static final int MAX_ORD = values.length -1;
	
	/** Map to decode a TimeUnit to TimeUnitSymbol */
	public static final Map<TimeUnit, TimeUnitSymbol> SYMBOL4UNIT;
	/** Map short name to TimeUnitSymbol */
	public static final Map<String, TimeUnitSymbol> SHORT2UNIT;
	
	
	static {
		final StringBuilder b = new StringBuilder();
		final Map<TimeUnit, TimeUnitSymbol> umap = new EnumMap<TimeUnit, TimeUnitSymbol>(TimeUnit.class);
		final Map<String, TimeUnitSymbol> smap = new HashMap<String, TimeUnitSymbol>(values.length);
		for(TimeUnitSymbol tus: values()) {
			umap.put(tus.unit, tus);
			smap.put(tus.shortName, tus);
			if(b.length()>0) {
				b.append(",|");
			}
			b.append(tus.shortName);
		}
		PERIOD_PATTERN = Pattern.compile("(\\d++)([" + b.toString() + "]){1}?", Pattern.CASE_INSENSITIVE);
		SYMBOL4UNIT = Collections.unmodifiableMap(umap);
		SHORT2UNIT = Collections.unmodifiableMap(smap);
	}
	
	
	/** A link to the corresponding time unit */
	public final TimeUnit unit;
	/** The short name for a time unit (e.g. ms for MILLISECONDS) */
	public final String shortName;
	
	/**
	 * Parses a period expression in the form of <b><code>&lt;time value&gt;&lt;unit symbol&gt;</code></b>,
	 * e.g. <b><code>45ms</code></b> for 45 milliseconds.
	 * @param value The expression to parse
	 * @return an NVP containing the parsed time period and unit
	 */
	public static NVP<Long, TimeUnitSymbol> period(final String value) {
		if(value==null || value.trim().isEmpty()) throw new IllegalArgumentException("The passed value was null");
		final Matcher m = PERIOD_PATTERN.matcher(value.trim());
		if(!m.matches()) throw new IllegalArgumentException("The passed value [" + value + "] was not a valid time period");
		final long t = Long.parseLong(m.group(1).trim());
		final TimeUnitSymbol tus = valueOf(m.group(2).trim().toLowerCase());
		return new NVP<Long, TimeUnitSymbol>(t, tus);
	}
	
	/**
	 * Returns the specified period as a byte array
	 * @param period The size of the period
	 * @param timeUnit The unit of the period
	 * @return the period as a byte array
	 */
	public static byte[] periodToByteArray(final long period, final TimeUnitSymbol timeUnit) {
		if(timeUnit==null) throw new IllegalArgumentException("The passed time unit was null");
		if(period < 1) throw new IllegalArgumentException("Invalid period: [" + period + "]");
		final byte[] bytes = new byte[9];
		final ByteBuffer buf = ByteBuffer.wrap(bytes);
		buf.putLong(period);
		buf.put((byte)timeUnit.ordinal());
		buf.flip();
		return bytes;
	}

	/**
	 * Decodes the passed byte to a period 
	 * @param bytes The bytes to decode
	 * @return an NVP containing the parsed time period and unit
	 */
	public static NVP<Long, TimeUnitSymbol> fromByteArray(final byte[] bytes) {
		if(bytes==null) throw new IllegalArgumentException("The passed byte array was null");
		if(bytes.length != 9) throw new IllegalArgumentException("The passed byte array was not 9 bytes, was [" + bytes.length + "] bytes");
		final ByteBuffer buf = ByteBuffer.wrap(bytes);
		final long t = buf.getLong(0);
		final byte ord = buf.get(8);
		if(ord < 0 || ord > MAX_ORD) throw new IllegalArgumentException("The decoded TimeUnitSymbol ordinal was invalid: [" + ord + "]");
		final TimeUnitSymbol tus = values[ord];
		return new NVP<Long, TimeUnitSymbol>(t, tus);		
	}
	
	
	/**
	 * Returns the symbol for the passed time unit
	 * @param unit The time unit to get the symbol for
	 * @return the symbol
	 */
	public static String symbol(final TimeUnit unit) {
		if(unit==null) throw new IllegalArgumentException("The passed TimeUnit was null");
		return SYMBOL4UNIT.get(unit).shortName;
	}
	
}
