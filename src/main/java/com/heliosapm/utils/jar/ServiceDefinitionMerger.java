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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: ServiceDefinitionMerger</p>
 * <p>Description: Merges {@link java.util.ServiceLoader} definition files from multiple locations into a common merged set of definition files.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jar.ServiceDefinitionMerger</code></p>
 */

public class ServiceDefinitionMerger extends AbstractResourceMerger {
	
	/** The standard pattern for a service definition file or folder */
	public static final String SERVICE_PREFIX = "META-INF/services/";
	private static final int SPL = SERVICE_PREFIX.length();
	/** Static class logger */
	private final static Logger log = Logger.getLogger(ServiceDefinitionMerger.class.getName()); 

	public static final String EOL = System.getProperty("line.separator");
	
	protected final Map<String, StringBuilder> services = new HashMap<String, StringBuilder>();

	public static final Charset UTF8 = Charset.forName("UTF8");
	/**
	 * Creates a new ServiceDefinitionMerger
	 */
	public ServiceDefinitionMerger() {
		super(true, true, true, true);
	}
	
	public static String getServiceClassName(final URL url) {
		final String surl = url.toString();
		return surl.substring(surl.indexOf(SERVICE_PREFIX) + SPL);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jar.ResourceMerger#inspect(java.net.URL, java.lang.String, boolean)
	 */
	@Override
	public boolean inspect(final URL resourceURL, final String resourceName, final boolean isFile) {
		if(resourceName.startsWith(SERVICE_PREFIX)) {
			if(isFile) {
				final String className = getServiceClassName(resourceURL);
				StringBuilder b = services.get(className);
				if(b==null) {
					b = new StringBuilder();
					services.put(className, b);
				}
				b.append(URLHelper.getTextFromURL(resourceURL));				
				if(log.isLoggable(Level.FINE)) log.fine(String.format("URL:%s, Name:%s, isFile:%s", resourceURL, resourceName, isFile));
			}
			return false;
		}
		return true;
	}
	
//	@Override
//	public boolean worksOnDirs() {
//		return !inServices;
//	}
//	
//	@Override
//	public boolean worksOnFiles() {		
//		return inServices;
//	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jar.ResourceMerger#writeMerged(java.util.jar.JarOutputStream)
	 */
	@Override
	public void writeMerged(final JarOutputStream jos) throws IOException {
		for(Map.Entry<String, StringBuilder> entry: services.entrySet()) {
			JarEntry je = new JarEntry(SERVICE_PREFIX + entry.getKey());
			jos.putNextEntry(je);
			jos.write(entry.getValue().toString().getBytes(UTF8));
			jos.flush();
			jos.closeEntry();
			if(log.isLoggable(Level.FINE)) log.log(Level.FINE, "Wrote entry [" + entry.getKey() + "]"); 					
		}
	}

}
