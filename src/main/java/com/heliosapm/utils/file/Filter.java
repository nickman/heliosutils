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
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.heliosapm.utils.file.Filters.ContainsNameFileFilter;
import com.heliosapm.utils.file.Filters.EndsWithNameFileFilter;
import com.heliosapm.utils.file.Filters.ExactNameFileFilter;
import com.heliosapm.utils.file.Filters.FileMod;
import com.heliosapm.utils.file.Filters.FileModFileFilter;
import com.heliosapm.utils.file.Filters.FileType;
import com.heliosapm.utils.file.Filters.FileTypeFileFilter;
import com.heliosapm.utils.file.Filters.LinkFileFileFilterFactory;
import com.heliosapm.utils.file.Filters.RegexNameFileFilter;
import com.heliosapm.utils.file.Filters.SizeRangeFileFilter;
import com.heliosapm.utils.file.Filters.StartsWithNameFileFilter;
import com.heliosapm.utils.file.Filters.TimeRangeFileFilter;

/**
 * <p>Title: Filters</p>
 * <p>Description: Some pre-defined file filters</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.file.Filter</code></p>
 */

public enum Filter implements FileFilterFactory {
	/** Exact match against the file name. Args are: name, case sens. (see {@link ExactNameFileFilter}) */
	EXACT(new ExactNameFileFilter()),	
	/** Uses regex pattern match against the file name. Args are: pattern, case sens. (see {@link RegexNameFileFilter}) */
	PATTERN(new RegexNameFileFilter()),
	/** Matches where the file name ends with. Args are: ending str, case sens. (see {@link EndsWithNameFileFilter}) */
	ENDSWITH(new EndsWithNameFileFilter()),	
	/** Matches where the file name starts with. Args are: starting str, case sens. (see {@link StartsWithNameFileFilter}) */
	STARTSWITH(new StartsWithNameFileFilter()),
	/** Matches where the file name contains. Args are: contained str, case sens. (see {@link ContainsNameFileFilter}) */
	CONTAINS(new ContainsNameFileFilter()),
	/** Matches against the file size. Args are: min size [max size]. (see {@link SizeRangeFileFilter}) */
	SIZE(new SizeRangeFileFilter()),
	/** Matches against the file time. Args are: min time [max time]. (see {@link TimeRangeFileFilter}) */
	TIME(new TimeRangeFileFilter()),
	/** Matches against the file size. Args are: {@link FileType}. (see {@link FileTypeFileFilter}) */
	TYPE(new FileTypeFileFilter()),
	/** Matches against the file size. Args are: {@link FileMod} array. (see {@link FileModFileFilter}) */
	MOD(new FileModFileFilter()),
	/** Matches against link files. Args are an optional array of {@link FileFilter}s to apply to the linked file) */
	LINK(new LinkFileFileFilterFactory());
	
	/** A set of the filters employing the case-insen boolean as arg[1] */
	public static final Set<Filter> CASE_FILTERS = Collections.unmodifiableSet(EnumSet.of(PATTERN, ENDSWITH, STARTSWITH, CONTAINS));
	
	
	
	
	private Filter(final FileFilterFactory factory) {
		this.factory = factory;
	}
	
	private final FileFilterFactory factory;

	@Override
	public FileFilter create(final Object... args) {
		return factory.create(args);
	}
	
	
	


}
