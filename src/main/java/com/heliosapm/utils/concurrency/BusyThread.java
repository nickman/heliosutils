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
package com.heliosapm.utils.concurrency;

/**
 * <p>Title: BusyThread</p>
 * <p>Description: Represents collected data on a thread when computing top threads.<</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.concurrency.BusyThread</code></p>
 */

public class BusyThread implements Comparable<BusyThread> {
	/** The cpu time in ns. */
	protected final long cpuTime;	
	/** The thread name */
	protected String threadName;
	
	
	
	/**
	 * Creates a new BusyThread
	 * @param cpuTime The cpu time in ns.
	 * @param threadName The thread name
	 */
	public BusyThread(long cpuTime, String threadName) {
		this.cpuTime = cpuTime;
		this.threadName = threadName;
	}



	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(BusyThread bt) {
		if(bt.cpuTime==cpuTime) return threadName.compareTo(bt.threadName);
		return bt.cpuTime>cpuTime ? 1 : -1;
	}



	@Override
	public String toString() {
		return String.format("%s\t:%s", threadName, cpuTime);
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((threadName == null) ? 0 : threadName.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BusyThread other = (BusyThread) obj;
		if (threadName == null) {
			if (other.threadName != null)
				return false;
		} else if (!threadName.equals(other.threadName))
			return false;
		return true;
	}

}
