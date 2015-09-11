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
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

import com.heliosapm.utils.enums.BitMasked;

/**
 * <p>Title: Filters</p>
 * <p>Description: A bunch of {@link FileFilterFactory} implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.file.Filters</code></p>
 */

public abstract class Filters {
	/** A cache of created file filters */
	private static final NonBlockingHashMap<String, FileFilter> filterCache = new NonBlockingHashMap<String, FileFilter>(); 
	
	/**
	 * Builds a key for a filter type and its args
	 * @param filter The filter type
	 * @param args The filter arguments
	 * @return the string key
	 */
	public static String key(final Class<? extends FileFilterFactory> filter, final Object...args) {
		final StringBuilder b = new StringBuilder(filter.getName());
		for(Object o: args) {
			b.append(o);
		}
		return b.toString();
	}
	
	public static abstract class AbstractFileFilterFactory implements FileFilterFactory {
		
	}
	
	/**
	 * <p>Title: AbstractCachingFileFilterFactory</p>
	 * <p>Description: Abstract base class that supports caching for created filters</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.file.Filters.AbstractCachingFileFilterFactory</code></p>
	 */
	public static abstract class AbstractCachingFileFilterFactory extends AbstractFileFilterFactory {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.utils.file.FileFilterFactory#create(java.lang.Object[])
		 */
		@Override
		public FileFilter create(final Object... args) {
			final String key = key(getClass(), args);
			FileFilter ff = filterCache.get(key);
			if(ff==null) {
				synchronized(filterCache) {
					ff = filterCache.get(key);
					if(ff==null) {
						ff = doCreate(args);
						filterCache.put(key, ff);
					}
				}
			}
			return ff;
		}
		
		/**
		 * Implements the FileFilter creation
		 * @param args The filter arguments
		 * @return the filter
		 */
		public abstract FileFilter doCreate(final Object... args);
	}
	
	
	/**
	 * <p>Title: LongPairFileFilter</p>
	 * <p>Description: Base abstract class for a filter builder that accepts one or two long values</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.file.Filters.LongPairFileFilter</code></p>
	 */
	public static abstract class LongPairFileFilter extends AbstractFileFilterFactory {
		public abstract boolean one(final File f, long one);
		public abstract boolean two(final File f, long one, long two);
		@Override
		public FileFilter create(final Object... args) {
			final long[] range = new long[args.length];
			for(int i = 0; i < args.length; i++) {
				range[i] = (Long)args[i];
			}
			return new FileFilter() {
				@Override
				public boolean accept(final File file) {
					if(range.length==1) return one(file, range[0]);
					return two(file, range[0], range[1]);
				}
			};
		}
	}
	
	/**
	 * <p>Title: SizeRangeFileFilter</p>
	 * <p>Description: Builds filters that filter in files within size ranges</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.file.Filters.SizeRangeFileFilter</code></p>
	 */
	public static class SizeRangeFileFilter extends LongPairFileFilter {
		/**
		 * <p>Validates that the file size is greater than or equal to the passed size.</p>
		 * {@inheritDoc}
		 * @see com.heliosapm.utils.file.Filters.LongPairFileFilter#one(java.io.File, long)
		 */
		@Override
		public boolean one(final File f, final long one) {
			return f.length() >= one;
		}

		/**
		 * <p>Validates that the file size is greater than or equal to the {@code one} size and less than or equal to the {@code two} size</p>
		 * {@inheritDoc}
		 * @see com.heliosapm.utils.file.Filters.LongPairFileFilter#two(java.io.File, long, long)
		 */
		@Override
		public boolean two(final File f, final long one, final long two) {
			final long l = f.length();
			return l >= one && l <= two;
		}		
	}
	
	/**
	 * <p>Title: TimeRangeFileFilter</p>
	 * <p>Description: Builds filters that filter in files within last modified time ranges</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.file.Filters.TimeRangeFileFilter</code></p>
	 */
	public static class TimeRangeFileFilter extends LongPairFileFilter {
		/**
		 * <p>Validates that the file last modified time is greater than or equal to the passed time.</p>
		 * {@inheritDoc}
		 * @see com.heliosapm.utils.file.Filters.LongPairFileFilter#one(java.io.File, long)
		 */
		@Override
		public boolean one(final File f, final long one) {
			return f.lastModified() >= one;
		}

		/**
		 * <p>Validates that the file last modified time is greater than or equal to the {@code one} time and less than or equal to the {@code two} time</p>
		 * {@inheritDoc}
		 * @see com.heliosapm.utils.file.Filters.LongPairFileFilter#two(java.io.File, long, long)
		 */
		@Override
		public boolean two(final File f, final long one, final long two) {
			final long l = f.lastModified();
			return l >= one && l <= two;
		}		
	}
	
	public static final Object[] EMPTY_ARR = {};
	
	public static Object[] sliceOffLeading(final int x, final Object...args) {
		final int len = args.length;
		if(len==0 || len <= x) return EMPTY_ARR;
		if(x<=0) return args;
		final int nlen = len-x;
		final Object[] arr = new Object[nlen];
		System.arraycopy(args, x, arr, 0, nlen);
		return arr;
	}
	
	/**
	 * <p>Title: AbstractAttributeFileFilter</p>
	 * <p>Description: Abstract filter builder that filters in by file attributes</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.file.Filters.AbstractAttributeFileFilter</code></p>
	 */
	public static abstract class AbstractAttributeFileFilter<T> extends AbstractCachingFileFilterFactory {
		@SuppressWarnings("unchecked")
		@Override
		public FileFilter doCreate(final Object... args) {
			return doCreate((T)args[0], sliceOffLeading(1, args));
		}
		
		protected abstract FileFilter doCreate(final T comp, final Object...args);
	}

	
	/**
	 * <p>Title: AbstractNameFileFilter</p>
	 * <p>Description: Abstract filter builder that filters in by name with an option to specify case sensitivity</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.file.Filters.AbstractNameFileFilter</code></p>
	 */
	public static abstract class AbstractNameFileFilter<T> extends AbstractCachingFileFilterFactory {
		@SuppressWarnings("unchecked")
		@Override
		public FileFilter doCreate(final Object... args) {
			return doCreate((T)args[0], args.length==1 ? true : (Boolean)args[1]);
		}
		
		protected abstract FileFilter doCreate(final T comp, final boolean caseSensitive);
	}
	
	/**
	 * <p>Title: RegexNameFileFilter</p>
	 * <p>Description: Builds filters that filter in files based on a regex match to the file name</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.file.Filters.RegexNameFileFilter</code></p>
	 */
	public static class RegexNameFileFilter extends AbstractNameFileFilter<String> {
		@Override
		protected FileFilter doCreate(final String comp, final boolean caseSensitive) {			
			return new FileFilter() {
				final Pattern pattern = Pattern.compile(comp, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
				@Override
				public boolean accept(final File file) {
					return pattern.matcher(file.getName()).matches();
				}
			};
		}
	}
	
	/**
	 * <p>Title: StartsWithNameFileFilter</p>
	 * <p>Description: Builds filters that filter in files based on a {@link String#startsWith(String)} to the file name</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.file.Filters.RegexNameFileFilter</code></p>
	 */
	public static class StartsWithNameFileFilter extends AbstractNameFileFilter<String> {
		@Override
		protected FileFilter doCreate(final String comp, final boolean caseSensitive) {			
			return new FileFilter() {
				final String cc = caseSensitive ? comp : comp.toLowerCase();
				@Override
				public boolean accept(final File file) {
					final String name = caseSensitive ? file.getName() : file.getName().toLowerCase();
					return name.startsWith(cc);
				}
			};
		}
	}
	
	/**
	 * <p>Title: EndsWithNameFileFilter</p>
	 * <p>Description: Builds filters that filter in files based on a {@link String#endsWith(String)} to the file name</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.file.Filters.EndsWithNameFileFilter</code></p>
	 */
	public static class EndsWithNameFileFilter extends AbstractNameFileFilter<String> {
		@Override
		protected FileFilter doCreate(final String comp, final boolean caseSensitive) {			
			return new FileFilter() {
				final String cc = caseSensitive ? comp : comp.toLowerCase();
				@Override
				public boolean accept(final File file) {
					final String name = caseSensitive ? file.getName() : file.getName().toLowerCase();
					return name.endsWith(cc);
				}
			};
		}
	}
	
	/**
	 * <p>Title: ContainsNameFileFilter</p>
	 * <p>Description: Builds filters that filter in files based on a {@link String#contains(CharSequence)} to the file name</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.file.Filters.ContainsNameFileFilter</code></p>
	 */
	public static class ContainsNameFileFilter extends AbstractNameFileFilter<String> {
		@Override
		protected FileFilter doCreate(final String comp, final boolean caseSensitive) {			
			return new FileFilter() {
				final String cc = caseSensitive ? comp : comp.toLowerCase();
				@Override
				public boolean accept(final File file) {
					final String name = caseSensitive ? file.getName() : file.getName().toLowerCase();
					//int x = FileMod.maskFor(FileMod.EXECUTABLE, FileMod.HIDDEN);
					return name.contains(cc);
				}
			};
		}
	}
	
	/**
	 * <p>Title: FileTypeFileFilter</p>
	 * <p>Description: Builds filters that filter in files based on their file type ({@link FileType}</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.file.Filters.FileTypeFileFilter</code></p>
	 */
	public static class FileTypeFileFilter extends AbstractAttributeFileFilter<FileType> {
		@Override
		protected FileFilter doCreate(final FileType comp, final Object...args) {			
			return new FileFilter() {
				@Override
				public boolean accept(final File file) {
					return comp.accept(file);
				}
			};
		}
	}
	
	/**
	 * <p>Title: FileModFileFilter</p>
	 * <p>Description: Builds filters that filter in files based on their file mods ({@link FileMod}</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.file.Filters.FileModFileFilter</code></p>
	 */
	public static class FileModFileFilter extends AbstractAttributeFileFilter<FileMod> {
		@Override
		protected FileFilter doCreate(final FileMod comp, final Object...args) {	
			final int mask = FileMod.maskFor(comp, args);
			return new FileFilter() {
				@Override
				public boolean accept(final File file) {
					return FileMod.accept(file, mask);
				}
			};
		}
	}
	
	
	
	public static enum FileType implements FileFilter {
		FILE{public boolean accept(final File f) { return f.isFile(); }},
		DIRECTORY{public boolean accept(final File f) { return f.isDirectory(); }},
		EITHER{public boolean accept(final File f) { return true; }};
	}
	
	public static enum FileMod implements FileFilter, BitMasked {
		READABLE{			
			public boolean accept(final File f) { return f.canRead(); }
		},
		WRITABLE{
			public boolean accept(final File f) { return f.canWrite(); }
		},
		EXECUTABLE{
			public boolean accept(final File f) { return f.canExecute(); }
		},
		HIDDEN{
			public boolean accept(final File f) { return f.isHidden(); }
		},
		NOTREADABLE{			
			public boolean accept(final File f) { return !f.canRead(); }
		},
		NOTWRITABLE{
			public boolean accept(final File f) { return !f.canWrite(); }
		},
		NOTEXECUTABLE{
			public boolean accept(final File f) { return !f.canExecute(); }
		},
		NOTHIDDEN{
			public boolean accept(final File f) { return !f.isHidden(); }
		};		
		
		final int mask = StaticOps.ordinalBitMaskInt(this);
		public int getMask(){ return mask; }
		public boolean isEnabled(final int mask){ return StaticOps.isEnabled(this, mask); }			
		public int enableFor(final int mask) { return StaticOps.enableFor(this, mask); }
		public int disableFor(final int mask) { return StaticOps.disableFor(this, mask); }	
		

		public static int maskFor(final FileMod... members) { return StaticOps.maskFor(members); }
		public static Set<FileMod> membersFor(final int mask) { return StaticOps.membersFor(FileMod.class, mask); };
		
		public static FileMod[] fillIn(final FileMod comp, final Object...args) {
			final Set<FileMod> set = EnumSet.noneOf(FileMod.class);
			if(comp!=null) set.add(comp);			
			for(Object o: args) {
				if(o!=null && o instanceof FileMod) set.add((FileMod)o);
			}
			return set.toArray(new FileMod[set.size()]);
		}
		
		public static int maskFor(final FileMod comp, final Object...args) {
			return maskFor(fillIn(comp, args));
		}
		
		public static boolean accept(final File file, final int mask) {
			for(FileMod mod: membersFor(mask)) {
				if(!mod.accept(file)) return false;
			}
			return true;
		}

	}

}
