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
 * <p>Title: ExecutorServiceFactory</p>
 * <p>Description: An object that creates new {@link ExecutorService} on demand. Using an {@link ExecutorServiceFactory} mainly
 * simplifies providing a custom {@link ExecutorService} implementation to Netty's event loops.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.ExecutorServiceFactory</code></p>
 */

public interface ExecutorServiceFactory {
	/**
	 * Creates a new ExecutorService
	 * @param parallelism The degreee of parallelism
	 * @return the new ExecutorService
	 */
	ExecutorService newExecutorService(int parallelism);
}
