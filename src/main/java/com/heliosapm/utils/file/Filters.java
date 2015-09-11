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

import static com.heliosapm.utils.file.FileFilterBuilder.filterCache;
import static com.heliosapm.utils.file.FileFilterBuilder.key;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

/**
 * <p>Title: Filters</p>
 * <p>Description: Some pre-defined file filters</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.file.Filters</code></p>
 */

public enum Filters implements FileFilterFactory {
//	/** Uses regex pattern match against the file name. Args are: pattern, case sens */
//	PATTERN(){
//		@Override
//		public FileFilter create(final Object... args) {
//			final String key = key(this, args);
//			FileFilter f = filterCache.get(key);
//			if(f==null) {
//				final Pattern p = Pattern.compile(args[0].toString(), ((Boolean)args[1]).booleanValue() ? 0 : Pattern.CASE_INSENSITIVE);
//				f = new FileFilter() {
//					@Override
//					public boolean accept(final File file) {
//						if(file==null) return false;
//						return p.matcher(file.getName()).matches();
//					};
//				};
//			}
//			return f;
//		}		
//	},
//	/** Matches where the file name ends with. Args are: ending str, case sens */
//	ENDSWITH(){
//		@Override
//		public FileFilter create(final Object... args) {
//			final String key = key(this, args);
//			FileFilter f = filterCache.get(key);
//			if(f==null) {
//				final boolean c = (Boolean)args[1]; 
//				f = new FileFilter() {
//					@Override
//					public boolean accept(final File file) {
//						if(file==null) return false;
//						final String s = c ? file.getName() : file.getName().toLowerCase();  
//						return s.endsWith((c ? args[0].toString() : args[0].toString().toLowerCase()));
//					};
//				};
//			}
//			return f;
//		}
//	},	
//	/** Matches where the file name starts with. Args are: ending str, case sens */
//	STARTSWITH(){
//		@Override
//		public FileFilter create(final Object... args) {
//			final String key = key(this, args);
//			FileFilter f = filterCache.get(key);
//			if(f==null) {
//				final boolean c = (Boolean)args[1]; 
//				f = new FileFilter() {
//					@Override
//					public boolean accept(final File file) {
//						if(file==null) return false;
//						final String s = c ? file.getName() : file.getName().toLowerCase();  
//						return s.startsWith((c ? args[0].toString() : args[0].toString().toLowerCase()));
//					};
//				};
//			}
//			return f;
//		}
//	},
//	/** Matches where the file name starts with. Args are: ending str, case sens */
//	CONTAINS(){
//		@Override
//		public FileFilter create(final Object... args) {
//			final String key = key(this, args);
//			FileFilter f = filterCache.get(key);
//			if(f==null) {
//				final boolean c = (Boolean)args[1]; 
//				f = new FileFilter() {
//					@Override
//					public boolean accept(final File file) {
//						if(file==null) return false;
//						final String s = c ? file.getName() : file.getName().toLowerCase();  
//						return s.contains((c ? args[0].toString() : args[0].toString().toLowerCase()));
//					};
//				};
//			}
//			return f;
//		}
//	},
	/** Matches against the file size. Args are: ending str, case sens */
	SIZE(new LongPairFileFilter(){
		@Override
		public boolean one(final File f, final long one) {			
			return f.length() >= one;
		}
		@Override
		public boolean two(final File f, final long one, final long two) {
			return f.length() >= one && f.length() <= two;
		}		
	});
	
	private Filters(final FileFilterFactory factory) {
		this.factory = factory;
	}
	
	private final FileFilterFactory factory;
	
	
	@Override
	public FileFilter create(Object... args) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	private abstract static class LongPairFileFilter implements FileFilterFactory {
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
	


}
