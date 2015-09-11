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
package com.heliosapm.utils.file;

import java.io.FileFilter;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

/**
 * <p>Title: FileFilterBuilder</p>
 * <p>Description: A fluent style builder for file filters</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.file.FileFilterBuilder</code></p>
 */

public class FileFilterBuilder {
	/** A cache of created file filters */
	static final NonBlockingHashMap<String, FileFilter> filterCache = new NonBlockingHashMap<String, FileFilter>(); 
	/**
	 * Creates a new FileFilterBuilder
	 */
	public FileFilterBuilder() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Builds a key for a filter type and its args
	 * @param filter The filter type
	 * @param args The filter arguments
	 * @return the string key
	 */
	static String key(final Filters filter, final Object...args) {
		final StringBuilder b = new StringBuilder(filter.name());
		for(Object o: args) {
			b.append(o);
		}
		return b.toString();
	}

}
