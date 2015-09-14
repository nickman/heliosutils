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

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Title: TokenAwareProperties</p>
 * <p>Description: {@link Properties} extension that replaces tokens with resolved values</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.config.TokenAwareProperties</code></p>
 */

public class TokenAwareProperties extends Properties {

	/**  */
	private static final long serialVersionUID = 3007418806375074091L;
	
	public static final String S_PREF = "${";
	public static final Pattern P_PREF = Pattern.compile("\\$\\{(.*?)\\}");
	
	/**
	 * Creates a new TokenAwareProperties
	 */
	public TokenAwareProperties() {
		super();
	}
	
	/**
	 * Creates a new TokenAwareProperties
	 * @param p The properties to initialize with
	 */
	public TokenAwareProperties(final Properties p) {
		super();
		this.putAll(p);
	}
	
	
	@Override
	public String getProperty(final String key, final String defaultValue) {
		final String v =  super.getProperty(key, defaultValue);
		return token(v);
	}
	
	public static String token(final String v) {
		if(v==null) return v;
		if(v.indexOf(S_PREF)!=-1) {
			final StringBuffer b = new StringBuffer();
			final Matcher m = P_PREF.matcher(v.trim());
			while(m.find()) {
				final String token = m.group(1).trim();
				final String tokenValue = ConfigurationHelper.getSystemThenEnvProperty(token, "");
				m.appendReplacement(b, tokenValue);
			}
			m.appendTail(b);
			return b.toString();
		} 
		return v;		
	}
	
	@Override
	public String getProperty(final String key) {
		return getProperty(key, null);
	}
	
  public synchronized String toString() {
    int max = size() - 1;
    if (max == -1)
        return "{}";

    StringBuilder sb = new StringBuilder();
    Iterator<Map.Entry<Object,Object>> it = entrySet().iterator();

    sb.append('{');
    for (int i = 0; ; i++) {
        Map.Entry<Object,Object> e = it.next();
        Object key = e.getKey();
        Object value = e.getValue();
        sb.append(key   == this ? "(this Map)" : key.toString());
        sb.append('=');
        sb.append(value == this ? "(this Map)" : token(value.toString()));

        if (i == max)
            return sb.append('}').toString();
        sb.append(", ");
    }
}
	
	

}
