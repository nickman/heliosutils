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
package com.heliosapm.utils.collections;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Properties;

import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: Props</p>
 * <p>Description: {@link Properties} convenience utility functions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.collections.Props</code></p>
 */

public class Props {

	/**
	 * Produces a properties instance from the passed stringy.
	 * Attempts properties creation in the following order:<ol>
	 * 	<li>Attempts to resolve as a URL or File and load accordingly</li>
	 *  <li>Attempts to read as XML properties</li>
	 *  <li>Loads as EOL delimited key value pairs</li>
	 * </ol>
	 * @param cs The properties stringy
	 * @return a properties instance
	 */
	public static Properties strToProps(final CharSequence cs) {
		final Properties p = new Properties();
		if(cs!=null) {
			final String s = cs.toString();
			if(!s.trim().isEmpty()) {
				if(URLHelper.isValidURL(s)) {
					return URLHelper.readProperties(URLHelper.toURL(s));
				}
				
				try {
					ByteArrayInputStream bais = new ByteArrayInputStream(s.trim().getBytes(Charset.forName("UTF8"))); 
					p.load(bais);
				} catch (Exception x) {/* No Op */}
			}
		}
		return p;
	}
	
	private Props() {}

}
