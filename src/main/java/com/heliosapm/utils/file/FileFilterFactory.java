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

/**
 * <p>Title: FileFilterFactory</p>
 * <p>Description: Defines a factory for {@link FileFilter}s</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.file.FileFilterFactory</code></p>
 */

public interface FileFilterFactory {
	/**
	 * Creates a new file filter
	 * @param args The generic arguments
	 * @return a FileFilter
	 */
	public FileFilter create(final Object...args);
}
