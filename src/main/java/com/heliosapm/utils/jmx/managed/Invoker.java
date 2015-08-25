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
 * <p>Title: Invoker</p>
 * <p>Description: Defines an op or attribute invoker for MBeans</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.managed.Invoker</code></p>
 */

public interface Invoker {
	/**
	 * Binds the target of the invocation to the invoker
	 * @param target The target of the invocation
	 * @return the return value of the target invocation
	 */
	public Invoker bindTo(Object target);
	
	/**
	 * Invokes against the target 
	 * @param args The arguments to the invocation
	 * @return the invocation return value
	 */
	public Object invoke(Object...args);
	
	/**
	 * Returns the logical name of the invoker's target object.
	 * Typically a JMX attribute name or operation action
	 * @return the logical name of the invoker's target object
	 */
	public String getName();
	
	/**
	 * Indicates if the invoker is bound
	 * @return true if the invoker is bound or the target is static
	 */
	public boolean isBound();
	
	/**
	 * Returns the bound target
	 * @return the bound target
	 */
	public Object getTarget();
}
