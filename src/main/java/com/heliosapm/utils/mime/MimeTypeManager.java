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
package com.heliosapm.utils.mime;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: MimeTypeManager</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.mime.MimeTypeManager</code></p>
 */

public class MimeTypeManager {
	/** The singleton instance */
	private static volatile MimeTypeManager instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** A map of mime types keyed by file extension */
	protected final Map<String, String> mimeTypes = new ConcurrentHashMap<String, String>(1024, 0.75f, Runtime.getRuntime().availableProcessors());
	
	/** The built in mime types map */
	public static final String ALL_MIME_TYPES = "META-INF/all-mime.types";
	/** Whitespace splitter */
	public static final Pattern WHITESPACE_SPLIT = Pattern.compile("\\s+");
	
	/**
	 * Acquires and returns the MimeTypeManager singleton instance
	 * @return the MimeTypeManager singleton instance
	 */
	public static MimeTypeManager getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new MimeTypeManager();					
				}
			}
		}
		return instance;
	}
	
	private MimeTypeManager() {
		loadMimeMap(ALL_MIME_TYPES);
	}
	
	public static void main(String[] args) {
		getInstance();
	}
	
	/**
	 * Loads a mime map into the manager
	 * @param resource The resource name where the mime types can be read from
	 */
	public void loadMimeMap(final String resource) {
		if(resource==null || resource.trim().isEmpty()) throw new IllegalArgumentException("The passed resource was null or empty");
		final long start = System.currentTimeMillis();
		final URL url = URLHelper.toURL(resource);
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		String line = null;
		int loaded = 0, skipped = 0;
		final Set<String> dups = new HashSet<String>();
		try {
			is = url.openStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			while((line=br.readLine())!=null) {
				final String entry = line.trim();
				if(entry.isEmpty()) continue;
				if(entry.charAt(0)=='#') continue;
				final String[] segs = WHITESPACE_SPLIT.split(entry);
				if(segs.length < 2) continue;
				final String type = segs[0];				
				for(int i = 1; i < segs.length; i++) {
					if(segs[i]==null) continue;
					final String key = segs[i].toLowerCase();
					if(!mimeTypes.containsKey(key)) {
//						System.out.println("Adding MIME Type: [" + type + "], Ext: [" + key + "]");
						mimeTypes.put(key, type);
						loaded++;
					} else {
//						System.err.println("Skipping duplicate MIME Type: [" + type + "], Ext: [" + key + "]");
						dups.add(key);
						skipped++;
					}
				}
			}
			final long elapsed = System.currentTimeMillis() - start;
			System.out.println("Loaded [" + loaded + "], Skipped: [" + skipped + "], Dups: " + dups + ", Elapsed: " + elapsed + " ms.");
		} catch (Exception ex) {
			throw new RuntimeException("Failed to read mime type resource [" + resource + "]", ex);
		} finally {
			if(br!=null) try { br.close(); } catch (Exception x) {/* No Op */}
			if(isr!=null) try { isr.close(); } catch (Exception x) {/* No Op */}
			if(is!=null) try { is.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Returns the mime type for the passed extension
	 * @param extension The file extension to get the mime type for 
	 * @param defaultType The default mime type to return if the extension does not key to a type
	 * @return the decoded mime type or the default type if not found
	 */
	public String getMimeTypeForExt(final String extension, final String defaultType) {
		if(extension==null || extension.trim().isEmpty()) return defaultType;
		final String key = extension.trim().toLowerCase().replace(".", "");
		final String mtype = mimeTypes.get(key);
		return mtype==null ? defaultType : mtype;
	}


}
