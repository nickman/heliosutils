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

/**
 * <p>Title: AbstractResourceMerger</p>
 * <p>Description: Base class for {@link ResourceMerger} implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jar.AbstractResourceMerger</code></p>
 */

public abstract class AbstractResourceMerger implements ResourceMerger {
	protected final boolean urlScans;
	protected final boolean fileScans;
	protected final boolean files;
	protected final boolean dirs;
	

	public AbstractResourceMerger(final boolean urlScans, final boolean fileScans, final boolean files, final boolean dirs) {		
		this.urlScans = urlScans;
		this.fileScans = fileScans;
		this.files = files;
		this.dirs = dirs;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jar.ResourceMerger#worksOnUrlScans()
	 */
	@Override
	public boolean worksOnUrlScans() {
		return urlScans;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jar.ResourceMerger#worksOnFileScans()
	 */
	@Override
	public boolean worksOnFileScans() {
		return fileScans;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jar.ResourceMerger#worksOnFiles()
	 */
	@Override
	public boolean worksOnFiles() {
		return files;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jar.ResourceMerger#worksOnDirs()
	 */
	@Override
	public boolean worksOnDirs() {
		return dirs;
	}


}
