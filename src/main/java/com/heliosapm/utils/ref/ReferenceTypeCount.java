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

import java.io.Serializable;
import java.util.Map;

import com.heliosapm.utils.counters.NumericCounter;



/**
 * <p>Title: ReferenceTypeCount</p>
 * <p>Description: A count tracker for type of references cleared in the ReferenceService</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ref.ReferenceTypeCount</code></p>
 */

public class ReferenceTypeCount implements ReferenceTypeCountMBean, Serializable {
	/**  */
	private static final long serialVersionUID = 5683688389706572174L;
	/** The name of a cleared reference type */
	private final String typeName;
	/** The count of cleared references */
	private final NumericCounter counter = new NumericCounter();
	
	/**
	 * Creates a new ReferenceTypeCount
	 * @param typeName The name of a cleared reference type
	 */
	public ReferenceTypeCount(String typeName) {
		this.typeName = typeName;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceTypeCountMBean#reset()
	 */
	public void reset() {
		counter.set(0);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceTypeCountMBean#increment()
	 */
	public void increment() {
		counter.increment();
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceTypeCountMBean#getName()
	 */
	@Override
	public String getName() {
		return typeName;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceTypeCountMBean#getLong()
	 */
	@Override
	public long getLong() {
		return counter.get();
	}



	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((typeName == null) ? 0 : typeName.hashCode());
		return result;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ReferenceTypeCount))
			return false;
		ReferenceTypeCount other = (ReferenceTypeCount) obj;
		if (typeName == null) {
			if (other.typeName != null)
				return false;
		} else if (!typeName.equals(other.typeName))
			return false;
		return true;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("ReferenceTypeCount [name:%s, count:%s]", typeName, counter.get());
	}

}
