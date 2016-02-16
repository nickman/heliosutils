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
 * <p>Title: FileChangeEventListener</p>
 * <p>Description: Defines a listener notified of file change events</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.file.FileChangeEventListener</code></p>
 */

public interface FileChangeEventListener {
	/**
	 * Callback fired when a watched file changes
	 * @param file the changed file
	 */
	public void onChange(final File file);
	/**
	 * Callback fired when a watched file is deleted
	 * @param file the deleted file
	 */
	public void onDelete(final File file);
	/**
	 * Callback fired when a new file is created matching the watcher's criteria
	 * @param file the new file
	 */
	public void onNew(final File file);
	/**
	 * Returns the file change events that this listener is interested in
	 * @return an array of file change events
	 */
	public FileChangeEvent[] getInterest();
	
	/**
	 * Provides this listener a reference to the FileChangeWatcher that is managing the file watch
	 * @param fileChangeWatcher The parent FileChangeWatcher
	 */
	public void setFileChangeWatcher(final FileChangeWatcher fileChangeWatcher);
}
