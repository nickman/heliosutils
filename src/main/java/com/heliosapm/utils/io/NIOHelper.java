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
package com.heliosapm.utils.io;

import java.lang.reflect.Method;
import java.nio.Buffer;

/**
 * <p>Title: NIOHelper</p>
 * <p>Description: Some NIO related utility methods</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.io.NIOHelper</code></p>
 */

public class NIOHelper {
	/** The direct byte buff class */
	private static final Class<?> directByteBuffClass;
	/** The direct byte buff class cleaner accessor method*/
	private static final Method getCleanerMethod;
	/** The clean method in cleaner */
	private static final Method cleanMethod;
	
	private static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("windows");
	
	static {
		Class<?> clazz = null;
		Class<?> cleanerClazz = null;
		Method m = null;
		Method cm = null;
		try {
			clazz = Class.forName("java.nio.DirectByteBuffer", true, ClassLoader.getSystemClassLoader());
			m = clazz.getDeclaredMethod("cleaner");
			m.setAccessible(true);
			cleanerClazz = Class.forName("sun.misc.Cleaner", true, ClassLoader.getSystemClassLoader());
			cm = cleanerClazz.getDeclaredMethod("clean");
			cm.setAccessible(true);
		} catch (Throwable x) {
			clazz = null;
			m = null;
			cm = null;
			System.err.println("Failed to initialize DirectByteBuffer Cleaner:" + x + "\n\tNon-Fatal, will continue");
		}
		directByteBuffClass = clazz;
		getCleanerMethod = m;
		cleanMethod = cm;
	}
	

	
	
	/**
	 * Manual deallocation of the memory allocated for direct byte buffers.
	 * Does nothing if the cleaner class and methods were not reflected successfully,
	 * the passed buffer is null or not a DirectByteBuffer, 
	 * or if the clean invocation fails.
	 * @param buffs The buffers to clean
	 */
	public static void clean(final Buffer... buffs) {
		if(buffs!=null) {
			for(Buffer buff: buffs) {
				if(buff==null) continue;
				if(directByteBuffClass!=null && directByteBuffClass.isInstance(buff)) {
					try {
						Object cleaner = getCleanerMethod.invoke(buff);
						if(cleaner!=null) {
							cleanMethod.invoke(cleaner);					
						}
						return;
					} catch (Throwable t) {
						t.printStackTrace(System.err);
						/* No Op */
					}
				}
				if(IS_WIN) System.err.println("Uncleaned MappedByteBuffer on Windows !!!!");				
			}
		}
	}
	
	private NIOHelper() {}


}
