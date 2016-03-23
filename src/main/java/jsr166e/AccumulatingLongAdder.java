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
package jsr166e;

import java.util.concurrent.Callable;

/**
 * <p>Title: AccumulatingLongAdder</p>
 * <p>Description: A long adder that maintains its own count and aggregates up to the provided delegate</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>jsr166e.AccumulatingLongAdder</code></p>
 */

public class AccumulatingLongAdder extends LongAdder {
	/**  */
	private static final long serialVersionUID = 5098869511522966010L;
	/** The parent accumulated up to */
	protected final LongAdder parent;
	/**
	 * Creates a new AccumulatingLongAdder
	 */
	public AccumulatingLongAdder(final LongAdder parent) {
		if(parent==null) throw new IllegalArgumentException("The passed LongAdder parent was null");
		this.parent = parent;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see jsr166e.LongAdder#add(long)
	 */
	@Override
	public void add(final long x) {		
		parent.add(x);
		super.add(x);		
	}
	
	
	public static void main(String[] args) {
		final LongAdder parent =  new LongAdder();
		final AccumulatingLongAdder child = new AccumulatingLongAdder(parent);
		child.increment();
		log("parent: " + parent + ", child:" + child);
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
