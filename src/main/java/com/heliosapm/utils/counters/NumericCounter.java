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
package com.heliosapm.utils.counters;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.cliffc.high_scale_lib.Counter;

/**
 * <p>Title: NumericCounter</p>
 * <p>Description: Disguises a {@link org.cliffc.high_scale_lib.Counter} as a {@link Number}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.counters.NumericCounter</code></p>
 */

public class NumericCounter extends Number implements Serializable {
	/**  */
	private static final long serialVersionUID = -1840368799060992801L;
	/** The delegate counter */
	private final Counter counter = new Counter();
	
	
	/**
	 * Creates a new NumericCounter
	 */
	public NumericCounter() {

	}
	
	/**
	 * Replaces this object with it's current long value when serialized
	 * @return the current long value
	 * @throws ObjectStreamException
	 */
	Object writeReplace() throws ObjectStreamException {
		return counter.get();
	}


	/**
	 * @param x
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#add(long)
	 */
	public void add(long x) {
		counter.add(x);
	}


	/**
	 * 
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#decrement()
	 */
	public void decrement() {
		counter.decrement();
	}


	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return counter.hashCode();
	}


	/**
	 * 
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#increment()
	 */
	public void increment() {
		counter.increment();
	}


	/**
	 * @param x
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#set(long)
	 */
	public void set(long x) {
		counter.set(x);
	}


	/**
	 * @return
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#get()
	 */
	public long get() {
		return counter.get();
	}


	/**
	 * @return
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#intValue()
	 */
	public int intValue() {
		return counter.intValue();
	}


	/**
	 * @return
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#longValue()
	 */
	public long longValue() {
		return counter.longValue();
	}


	/**
	 * @return
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#estimate_get()
	 */
	public long estimate_get() {
		return counter.estimate_get();
	}


	/**
	 * @return
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#toString()
	 */
	public String toString() {
		return counter.toString();
	}


	/**
	 * 
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#print()
	 */
	public void print() {
		counter.print();
	}


	/**
	 * @return
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#internal_size()
	 */
	public int internal_size() {
		return counter.internal_size();
	}


	/**
	 * Returns the internal counter
	 * @return the counter
	 */
	public Counter getCounter() {
		return counter;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Number#floatValue()
	 */
	@Override
	public float floatValue() {
		return counter.get();
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Number#doubleValue()
	 */
	@Override
	public double doubleValue() {
		return counter.get();		
	}

}
