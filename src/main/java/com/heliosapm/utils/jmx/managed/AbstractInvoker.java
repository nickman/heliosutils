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
package com.heliosapm.utils.jmx.managed;


/**
 * <p>Title: AbstractInvoker</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.managed.AbstractInvoker</code></p>
 */

public abstract class AbstractInvoker implements Invoker {
	/** The invocation target */
	protected Object target = null;
	/** The target logical name */
	protected final String name;
	
	
	
	/**
	 * Creates a new AbstractInvoker
	 * @param name The logical name of the invoker's target object.
	 * Typically a JMX attribute name or operation action
	 */
	public AbstractInvoker(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.managed.Invoker#bindTo(java.lang.Object)
	 */
	@Override
	public Invoker bindTo(Object target) {
		this.target = target;
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.managed.Invoker#getTarget()
	 */
	@Override
	public Object getTarget() {
		return target;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.managed.Invoker#isBound()
	 */
	@Override
	public boolean isBound() {
		return target!=null;
	}

//	@Override
//	public R invoke(Object... args) {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
