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
	
	/** The prefix for a token */
	public static final String S_PREF = "${";
	/** The regex pattern to match a token */
	public static final Pattern P_PREF = Pattern.compile("\\$\\{(.*?)(?::(.*?))?\\}");
	
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
		return token(v, true);
	}
	
	/**
	 * Replaces <b><code>${}</code></b> tokens in the passed string, replacing them with the named
	 * system property, otherwise environmental variable with <b><code>.</code></b> (dots) replaced
	 * with <b><code>_</code></b> (underscores) and uppercased. Otherwise, the embedded default is used if present.
	 * @param v The string to detokenize
	 * @param blankOnUnResolved If true, unresolved tokens are replaced with a blank string
	 * @return the detokenized string
	 */
	public static String token(final String v, final boolean blankOnUnResolved) {
		if(v==null) return v;
		if(v.indexOf(S_PREF)!=-1) {
			final StringBuffer b = new StringBuffer();
			final Matcher m = P_PREF.matcher(v.trim());
			while(m.find()) {
				final String token = m.group(1).trim();
				String def = m.group(2);
				if(def!=null) def = def.trim();
				final String tokenValue = ConfigurationHelper.getSystemThenEnvProperty(token, def);
				if(tokenValue!=null) {
					m.appendReplacement(b, tokenValue);
				} else if(blankOnUnResolved) {
					m.appendReplacement(b, "");
				}
			}
			m.appendTail(b);
			return b.toString();
		} 
		return v;		
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
