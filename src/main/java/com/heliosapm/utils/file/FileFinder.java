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
import java.io.FileFilter;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;

import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: FileFinder</p>
 * <p>Description: Scans a given directory for files matching a file filter</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.file.FileFinder</code></p>
 */

public class FileFinder {
	public static final File[] EMPTY_FILE_ARR = {};
	public static final URL[] EMPTY_URL_ARR = {};
	private static final FileFilter DEFAULT_FILTER = new NOFILTER();
	protected FileFilter filter = DEFAULT_FILTER;
	protected int maxLevel = Integer.MAX_VALUE;
	protected int maxFiles = 1024;	
	protected int actualLevel = 0;
	protected int actualCount = 0;
	protected final Set<File> foundFiles = new LinkedHashSet<File>();
	protected final Set<File> dirsToSearch = new LinkedHashSet<File>();
	
	private FileFinder(final String...dirs) {
		addSearchDir(dirs);
	}
	
	public static FileFinder newFileFinder(final String...dirsToSearch) {
		return new FileFinder(dirsToSearch);
	}
	
	
	public FileFinder addSearchDir(final String...dirs) {
		for(String s : dirs) {
			if(s==null || s.trim().isEmpty()) continue;
			File f = new File(s.trim());
			if(f.exists() && f.isDirectory()) {
				dirsToSearch.add(f);
			}
		}
		return this;
	}
	
	public FileFilterBuilder filterBuilder() {
		return FileFilterBuilder.newBuilder(this);
	}
	
	public FileFinder maxDepth(final int depth) {
		if(depth < 0) throw new IllegalArgumentException("Invalid maximum directory depth [" + depth + "]. Must be >= 0");
		this.maxLevel = depth;
		return this;
	}
	
	public FileFinder maxFiles(final int maxFiles) {
		if(maxFiles < 0) throw new IllegalArgumentException("Invalid maximum file matches [" + maxFiles + "]. Must be >= 0");
		this.maxFiles = maxFiles;
		return this;
	}
	
	public File[] find() {
		dirsToSearch.clear();
		actualCount = 0;
		actualLevel = 0;
		if(dirsToSearch.isEmpty() || maxFiles==0) return EMPTY_FILE_ARR;
		for(File dir: dirsToSearch) {
			doIt(dir);
			actualLevel = 0;
			if(actualCount==maxFiles) break;
		}
		return foundFiles.toArray(new File[actualCount]);
	}
	
	public URL[] findAsURLs() {
		final File[] files = find();
		if(files.length==0) return EMPTY_URL_ARR;
		final URL[] urls = new URL[files.length];
		for(int i = 0; i <urls.length; i++) {
			urls[i] = URLHelper.toURL(files[i]);
		}
		return urls;
	}
	
	protected void doIt(final File dir) {
		if(actualCount==maxFiles) return;
		for(File f: dir.listFiles()) {
			if(actualCount==maxFiles) return;
			if(filter.accept(f)) {
				foundFiles.add(f);
				actualCount++;
				if(actualCount==maxFiles) return;
			}
			if(f.isDirectory() && actualLevel <= maxLevel) {
				actualLevel++;				
				doIt(f);
			}
		}
	}
	
	public FileChangeWatcher watch(final long scanPeriodSecs, final boolean initBeforeFire, final FileChangeEventListener...listeners) {
		return new FileChangeWatcher(this, scanPeriodSecs, initBeforeFire, listeners); 
	}
	
	private static class NOFILTER implements FileFilter {
		@Override
		public boolean accept(final File pathname) {
			return true;
		}
	}
}
