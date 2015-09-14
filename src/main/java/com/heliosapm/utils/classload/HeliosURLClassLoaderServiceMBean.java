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
package com.heliosapm.utils.classload;

import java.net.URL;

/**
 * <p>Title: HeliosURLClassLoaderServiceMBean</p>
 * <p>Description: JMX MBean interface for {@link HeliosURLClassLoaderService}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.classload.HeliosURLClassLoaderServiceMBean</code></p>
 */

public interface HeliosURLClassLoaderServiceMBean {
	/** The object name prefix */
	public static final String OBJECT_NAME = "com.heliosapm.classpath:service=HeliosURLClassLoaderService";
	
	public int getCount();
	
	public long getClearedCount();
	
	public String[] getNames();
	
	public URL[] getURLsFor(final String name);
	
	public String getClassLoaderNameFor(final String className);

}
