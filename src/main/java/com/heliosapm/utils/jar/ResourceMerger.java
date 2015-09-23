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
package com.heliosapm.utils.jar;

import java.io.IOException;
import java.net.URL;
import java.util.jar.JarOutputStream;

/**
 * <p>Title: ResourceMerger</p>
 * <p>Description: Defines a class that inspects added resources to determine if and how they should be merged into the final jar</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jar.ResourceMerger</code></p>
 */

public interface ResourceMerger {
	/**
	 * Indicates if this merger knows how to handle URL scans
	 * @return true if this merger knows how to handle URL scans, false otherwise
	 */
	public boolean worksOnUrlScans();
	/**
	 * Indicates if this merger knows how to handle directory and file scans
	 * @return true if this merger knows how to handle directory and file scans, false otherwise
	 */
	public boolean worksOnFileScans();
	/**
	 * Indicates if this merger should inspect file based resources (as opposed to folder based ones) 
	 * @return true if this merger should inspect file based resources, false otherwise
	 */
	public boolean worksOnFiles();
	/**
	 * Indicates if this merger should inspect directory/folder based resources (as opposed to file based ones) 
	 * @return true if this merger should inspect directory/folder based resources, false otherwise
	 */
	public boolean worksOnDirs();
	/**
	 * Called once all the above filters have evaluated as true so the merger can do what it does
	 * @param resourceURL The URL of the resource
	 * @param resourceName The relative name of the resource
	 * @param isFile True if the resource is a file, false if it is a folder/directory
	 * @return true if the jarBuilder main should process this resource normally, 
	 * false if it will be handled exclusively by the merger 
	 */
	public boolean inspect(final URL resourceURL, final String resourceName, final boolean isFile); 
	
	/**
	 * Called once the jar builder main has completed the scans and written all resources to the jar.
	 * The impleentation of this method should write its merged content to the passed jar output stream.
	 * @param jos The jar output stream the merger should write the merged resources to
	 */
	public void writeMerged(final JarOutputStream jos) throws IOException;

}
