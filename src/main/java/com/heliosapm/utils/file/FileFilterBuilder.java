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
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.heliosapm.utils.file.Filters.FileMod;
import com.heliosapm.utils.file.Filters.FileType;

/**
 * <p>Title: FileFilterBuilder</p>
 * <p>Description: A fluent style builder for file filters</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.file.FileFilterBuilder</code></p>
 */

public class FileFilterBuilder {
	/** The accumulated filter arguments */
	private final Map<Filter, Object[]> enabledFilters = new EnumMap<Filter, Object[]>(Filter.class);
	/** Indicates if any text matches should be case insensitive (defaults to false) */
	private boolean caseInsensitive = false;
	
	private final FileFinder parent;
	
	private FileFilterBuilder() {
		this(FileFinder.newFileFinder());
	}
	private FileFilterBuilder(final FileFinder parent) {
		this.parent = parent;
	}
	
	
	/**
	 * Creates a new FileFilterBuilder
	 */
	public static FileFilterBuilder newBuilder() {
		return new FileFilterBuilder();
	}
	
	/**
	 * Creates a new FileFilterBuilder
	 */
	public static FileFilterBuilder newBuilder(final FileFinder parent) {
		return new FileFilterBuilder(parent);
	}
	
	public FileFinder fileFinder() {		
		return parent.setFilter(build());
	}
	public FileFilter build() {
		//if(enabledFilters.isEmpty()) throw new IllegalStateException("No filters or directories defined");
		final Set<FileFilter> filters = new LinkedHashSet<FileFilter>(enabledFilters.size());
		for(Map.Entry<Filter, Object[]> entry: enabledFilters.entrySet()) {
			if(Filter.CASE_FILTERS.contains(entry.getKey())) {
				entry.getValue()[1] = caseInsensitive;
			}
			filters.add(entry.getKey().create(entry.getValue()));
		}
		return new FileFilter() {
				@Override
				public boolean accept(final File file) {
					for(FileFilter filter: filters) {
						if(!filter.accept(file)) return false;
					}
					return true;
				}
		};
	}
	
	
	
	public FileFilterBuilder caseInsensitive(final boolean ins) {
		caseInsensitive = ins;
		return this;
	}
	
	public FileFilterBuilder patternMatch(final String pattern) {
		if(pattern==null || pattern.trim().isEmpty()) throw new IllegalArgumentException("The passed pattern was null or empty");
		enabledFilters.put(Filter.PATTERN, new Object[]{pattern, null});
		return this;
	}
	
	public FileFilterBuilder startsWithMatch(final String prefix) {
		if(prefix==null || prefix.trim().isEmpty()) throw new IllegalArgumentException("The passed prefix was null or empty");
		enabledFilters.put(Filter.STARTSWITH, new Object[]{prefix, null});
		return this;
	}
	
	public FileFilterBuilder exactMatch(final String name) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		enabledFilters.put(Filter.EXACT, new Object[]{name});
		return this;
	}
	
	
	public FileFilterBuilder endsWithMatch(final String suffix) {
		if(suffix==null || suffix.trim().isEmpty()) throw new IllegalArgumentException("The passed suffix was null or empty");
		enabledFilters.put(Filter.ENDSWITH, new Object[]{suffix, null});
		return this;
	}
	
	public FileFilterBuilder containsMatch(final String expr) {
		if(expr==null || expr.trim().isEmpty()) throw new IllegalArgumentException("The passed expression was null or empty");
		enabledFilters.put(Filter.CONTAINS, new Object[]{expr, null});
		return this;
	}
	
	public FileFilterBuilder size(final long minSize, final long maxSize) {
		enabledFilters.put(Filter.SIZE, new Object[]{minSize, maxSize});
		return this;		
	}
	
	public FileFilterBuilder size(final long minSize) {
		return size(minSize, Long.MAX_VALUE);
	}
	
	public FileFilterBuilder lastModTime(final long minTime, final long maxTime) {
		enabledFilters.put(Filter.TIME, new Object[]{minTime, maxTime});
		return this;		
	}
	
	public FileFilterBuilder lastModTime(final long minTime) {		
		return size(minTime, Long.MAX_VALUE);
	}
	
	public FileFilterBuilder linkedFile(final FileFilter...filters) {
		enabledFilters.put(Filter.LINK, filters);
		return this;
	}
	
	public FileFilterBuilder shouldBeFile() {
		enabledFilters.put(Filter.TYPE, new Object[]{FileType.FILE});
		return this;				
	}
	
	public FileFilterBuilder shouldBeDir() {
		enabledFilters.put(Filter.TYPE, new Object[]{FileType.DIRECTORY});
		return this;				
	}
	
	public FileFilterBuilder fileAttributes(final FileMod...mods) {
		if(mods==null || mods.length==0) throw new IllegalArgumentException("No FileMods defined");
		final Object[] args = new Object[mods.length];
		System.arraycopy(mods, 0, args, 0, mods.length);
		enabledFilters.put(Filter.MOD, args);
		return this;						
	}
	
	
	
//	SIZE(new SizeRangeFileFilter()),
//	TIME(new TimeRangeFileFilter()),
//	TYPE(new FileTypeFileFilter()),
//	MOD(new FileModFileFilter());
	
	

}	
