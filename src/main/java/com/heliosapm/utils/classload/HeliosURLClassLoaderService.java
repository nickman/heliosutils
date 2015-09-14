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

import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;



/**
 * <p>Title: HeliosURLClassLoaderService</p>
 * <p>Description: JMX info on {@link HeliosURLClassLoader}s</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.classload.HeliosURLClassLoaderService</code></p>
 */

public class HeliosURLClassLoaderService implements HeliosURLClassLoaderServiceMBean {
	/** The singleton instance */
	private static volatile HeliosURLClassLoaderService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	private static final Set<String> PRIMITIVES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"byte", "boolean", "short", "char", "int", "float", "long", "double", "void"
	)));
	
	
	/**
	 * Acquires and returns the HeliosURLClassLoaderService singleton
	 * @return the HeliosURLClassLoaderService singleton
	 */
	public static HeliosURLClassLoaderService getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new HeliosURLClassLoaderService();
				}
			}
		}
		return instance;
	}

	
	public final ObjectName objectName;
	
	public static final URL[] EMPTY_URL_ARR = {};
	/**
	 * Creates a new HeliosURLClassLoaderService
	 */
	private HeliosURLClassLoaderService() {
		try {
			objectName = new ObjectName(OBJECT_NAME);
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, objectName);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException(ex);
		}
	}
	
	public int getCount() {
		return HeliosURLClassLoader.loaders.size();
	}
	
	public long getClearedCount() {
		return HeliosURLClassLoader.cleared.longValue();
	}
	
	
	public String[] getNames() {
		return HeliosURLClassLoader.loaders.keySet().toArray(new String[0]);
	}
	
	public URL[] getURLsFor(final String name) {
		final HeliosURLClassLoader loader = HeliosURLClassLoader.getLoader(name.trim());
		if(loader==null) return EMPTY_URL_ARR;
		return loader.getURLs();
	}
	
	public String getClassLoaderNameFor(final String className) {
		if(className==null || className.trim().isEmpty()) throw new IllegalArgumentException("Class name was null or empty");
		if(PRIMITIVES.contains(className)) return "System";
		for(String key: HeliosURLClassLoader.loaders.keySet()) {
			final HeliosURLClassLoader loader = HeliosURLClassLoader.getLoader(key);
			if(loader!=null) {
				try {
					Class<?> clazz = loader.loadClass(className);
					ClassLoader cl = clazz.getClassLoader();
//					if(cl==null) return "System";
					if(cl instanceof HeliosURLClassLoader) {
						return ((HeliosURLClassLoader)cl).name;
					} else {
						return cl.toString();
					}
				} catch (Exception x) {/* No Op */}
			}
		}
		ClassLoader cl = HeliosURLClassLoaderService.class.getClassLoader();
		while(cl!=null) {
			try {
				cl.loadClass(className);
				return cl.toString();
			} catch (Exception x) {/* No Op */}
			cl = cl.getParent();
		}
		return "Not Found";
	}

}
