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
package com.heliosapm.utils.config;

import java.util.Properties;

/**
 * <p>Title: PropertiesExt</p>
 * <p>Description: Some extended properties utilities</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.config.PropertiesExt</code></p>
 */

public abstract class PropertiesExt {
	/**
	 * Extracts and returns all the properties in {@code props} that are prefixed with the passed prefix
	 * @param prefix The target prefix minus any property name space delimiter
	 * @param delim The namespace delimiter
	 * @param props The properties to extract from
	 * @return the extracted properties
	 */
	public static Properties prefixed(final String prefix, final String delim, final Properties props) {
		if(props==null) throw new IllegalArgumentException("The passed properties was null");
		if(props.isEmpty()) return props;
		if(prefix==null || prefix.trim().isEmpty()) return props;
		final String pref = prefix.trim() + (delim==null ? "" : delim.trim());
		final int prefLen = pref.length();
		final Properties p = new Properties();
		for(final String key: props.stringPropertyNames()) {
			if(key.indexOf(pref)==0) {
				p.setProperty(key.substring(prefLen), props.getProperty(key));
			}
		}
		return p;
	}
	
	/**
	 * Extracts and returns all the properties in {@code props} that are prefixed with the passed prefix and a delimiter of <b><code>.</code></b>
	 * @param prefix The target prefix minus any property name space delimiter
	 * @param props The properties to extract from
	 * @return the extracted properties
	 */
	public static Properties prefixed(final String prefix, final Properties props) {
		return prefixed(prefix, ".", props);
	}
	
	public static Properties tokenAwareProperties(final Properties props) {
		if(props==null) throw new IllegalArgumentException("The passed properties was null");
		return new TokenAwareProperties(props);
	}
	
	public static void main(String args[]) {
		final Properties p = new Properties();
		final String pref = "foo.bar";
		final String[] testPrefs = new String[]{"foo.bar", "snafu", "token"};
		for(int i = 0; i < testPrefs.length; i++) {
			String s = testPrefs[i];
			p.setProperty(s + "." + "AAA", "AAA" + i);
			p.setProperty(s + "." + "XYZ", "XYZ" + i);
			p.setProperty(s + "." + "USERHOME", "${user.home}");
			p.setProperty(s + "." + "SHELL", "${shell}");
			
		}
		System.out.println("Original:" + p);
		for(String s: testPrefs) {
			if("token".equals(s)) {
				System.out.println("Prefix [" + s + "]:" + tokenAwareProperties(prefixed(s, p)));
			} else {
				System.out.println("Prefix [" + s + "]:" + prefixed(s, p));
			}
		}
	}

}
