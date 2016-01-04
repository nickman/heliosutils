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
package com.heliosapm.utils.jmx;

import java.util.concurrent.ExecutorService;

/**
 * <p>Title: ManagedDefaultExecutorServiceFactory</p>
 * <p>Description: A factory for JMX managed ExecutorServiceFactory instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.ManagedDefaultExecutorServiceFactory</code></p>
 */
public class ManagedDefaultExecutorServiceFactory implements ExecutorServiceFactory {
	/** The name prefix of the ExecutorServiceFactory to be created by this factory */
	final String name;
	
	/**
	 * Creates a new ManagedDefaultExecutorServiceFactory
	 * @param name The name prefix of the ExecutorServiceFactory to be created by this factory
	 */
	public ManagedDefaultExecutorServiceFactory(final String name) {
		this.name = name;
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.ExecutorServiceFactory#newExecutorService(int)
	 */
	@Override
	public ExecutorService newExecutorService(final int parallelism) {
		return new ManagedForkJoinPool(name, parallelism, true);
	}

}
