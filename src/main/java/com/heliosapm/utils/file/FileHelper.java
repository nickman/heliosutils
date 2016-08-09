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

import java.io.File;

/**
 * <p>Title: FileHelper</p>
 * <p>Description: Static file helper utilities</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.file.FileHelper</code></p>
 */

public class FileHelper {
	/**
	 * Cleans out the passed directory
	 * @param dir the directory to clean
	 */
	public static void cleanDir(final File dir) {
		if(dir==null) throw new IllegalArgumentException("The passed file was null");
		if(dir.isFile()) throw new IllegalArgumentException("The passed file [" + dir + "] was not a directory");
		for(final File f: dir.listFiles()) {
			recursiveDel(f);
		}
	}
	
	
	/**
	 * Recursively deletes
	 * @param f the starting file
	 */
	public static void recursiveDel(final File f) {
		if(f==null) throw new IllegalArgumentException("The passed file was null");
		if(f.isFile()) {
			f.delete();
		} else {
			for(File x: f.listFiles()) {
				recursiveDel(x);
			}
		}		
	}
	
	
}
