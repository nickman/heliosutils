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
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.heliosapm.utils.url.URLHelper;
import com.heliosapm.utils.xml.XMLHelper;

/**
 * <p>Title: Props</p>
 * <p>Description: {@link Properties} convenience utility functions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.collections.Props</code></p>
 */

public class Props {
	
	/** The UTF character set */
	public static final Charset UTF8 = Charset.forName("UTF8");

	/**
	 * Produces a properties instance from the passed stringy.
	 * Attempts properties creation in the following order:<ol>
	 * 	<li>Attempts to resolve as a URL or File and load accordingly</li>
	 *  <li>Attempts to read as XML properties</li>
	 *  <li>Loads as EOL delimited key value pairs</li>
	 * </ol>
	 * @param cs The properties stringy
	 * @param charset The expected character set of the passed stringy, defaults to UTF8 if null.
	 * @return a properties instance
	 */
	public static Properties strToProps(final CharSequence cs, final Charset charset) {
		final Properties p = new Properties();
		final Charset ch = charset==null ? UTF8 : charset;
		if(cs!=null) {
			final String s = cs.toString();
			if(!s.trim().isEmpty()) {
				if(URLHelper.isValidURL(s)) {
					return URLHelper.readProperties(URLHelper.toURL(s));
				}
				final ByteArrayInputStream bais = new ByteArrayInputStream(s.trim().getBytes(ch));
				bais.mark(0);
				try {
					XMLHelper.parseXML(bais);
					bais.reset();
					p.loadFromXML(bais);
					return p;
				} catch (Exception x) {/* No Op */} finally { bais.reset(); }
				
				try {					 
					p.load(bais);
					return p;
				} catch (Exception x) {/* No Op */}
			}
		}
		return p;
	}
	
	/**
	 * Produces a properties instance from the passed UTF8 encoded stringy.
	 * Attempts properties creation in the following order:<ol>
	 * 	<li>Attempts to resolve as a URL or File and load accordingly</li>
	 *  <li>Attempts to read as XML properties</li>
	 *  <li>Loads as EOL delimited key value pairs</li>
	 * </ol>
	 * @param cs The properties stringy
	 * @return a properties instance
	 */
	public static Properties strToProps(final CharSequence cs) {
		return strToProps(cs, UTF8);
	}
	
	/**
	 * Add the passed properties to the system properties
	 * @param p The properties to add to system
	 */
	public static void setSystem(final Properties p) {
		if(p!=null && !p.isEmpty()) {
			for(String key: p.stringPropertyNames()) {
				System.setProperty(key, p.getProperty(key));
			}
		}
	}
	
	/**
	 * Converts the passed properties array to a single map
	 * @param props An array of properties
	 * @return a map
	 */
	public static Map<String, String> asMap(final Properties...props) {
		final Map<String, String> map = new HashMap<String, String>();
		for(Properties p: props) {
			for(Map.Entry<Object, Object> entry: p.entrySet()) {
				map.put(entry.getKey().toString(), entry.getValue()!=null ? entry.getValue().toString() : null);
			}
		}
		return map;
	}
	
	/**
	 * Creates and returns a new PropsBuilder, optionally initializing it with the passed properties 
	 * @param initialProps The optional properties to initialize with
	 * @return a new PropsBuilder
	 */
	public static PropsBuilder newPropsBuilder(final Properties...initialProps) {
		return new PropsBuilder(initialProps);
	}
	
	/**
	 * <p>Title: PropsBuilder</p>
	 * <p>Description: </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.collections.PropsBuilder</code></p>
	 */
	public static class PropsBuilder {
		final Properties p = new Properties();
		
		/**
		 * Creates a new PropsBuilder
		 * @param initialProps An array of initial props merged into this builder's properties.
		 * The properties at the end of the array will overwrite those at the beginning of the array where the keys match
		 */
		public PropsBuilder(final Properties...initialProps) {
			merge(initialProps);
		}
		
		/**
		 * Merges the passed properties into this builder's properties
		 * @param initialProps An array of props merged into this builder's properties.
		 * The properties at the end of the array will overwrite those at the beginning of the array where the keys match
		 * @return this builder
		 */
		public PropsBuilder merge(final Properties...initialProps) {
			for(Properties props: initialProps) {
				if(props==null || props.isEmpty()) continue;
				for(String key: props.stringPropertyNames()) {
					p.setProperty(key, props.getProperty(key));
				}
			}
			return this;
		}
		
		/**
		 * Converts the keys and values of the passed maps to Strings (using {@link #toString()}
		 * and merges them as properties into this builder's properties
		 * @param maps An array of maps merged into this builder's properties.
		 * The properties at the end of the array will overwrite those at the beginning of the array where the keys match
		 * @return this builder
		 */
		public PropsBuilder setPropertiesFromMaps(final Map<?, ?>...maps) {
			for(Map<?, ?> map: maps) {
				if(map==null || map.isEmpty()) continue;
				for(Map.Entry<?, ?> entry: map.entrySet()) {
					p.setProperty(entry.getKey().toString(), entry.getValue().toString());
				}
			}
			return this;
		}
		
		/**
		 * Sets a property
		 * @param key The property key
		 * @param value The property value
		 * @return this builder
		 */
		public PropsBuilder setProperty(final String key, final String value) {
			p.setProperty(key, value);
			return this;
		}
		
		/**
		 * Converts the passed stringy to properties and adds them to this builder's properties
		 * @param cs The stringy to convert
		 * @param charset The character set of the passed stringy, defaulting to UTF8 if null
		 * @return this builder
		 */
		public PropsBuilder addProperties(final CharSequence cs, final Charset charset) {
			merge(strToProps(cs, charset));
			return this;
		}
		
		/**
		 * Converts the passed UTF8 encoded stringy to properties and adds them to this builder's properties
		 * @param cs The stringy to convert
		 * @return this builder
		 */
		public PropsBuilder addProperties(final CharSequence cs) {
			merge(strToProps(cs));
			return this;
		}
		
		/**
		 * Parses the passed <b><code>"="</code></b> stringy into a key/value and adds it to this builder's properties
		 * @param cs The stringy to parse
		 * @return this builder
		 */
		public PropsBuilder parseProperty(final CharSequence cs) {
			final String s = cs.toString().trim();
			final int index = s.indexOf('=');
			if(index==-1) throw new IllegalArgumentException("No = delimiter found in [" + cs + "]");
			if(index==0) throw new IllegalArgumentException("No key found in [" + cs + "]");
			if(index==s.length()-1) throw new IllegalArgumentException("No value found in [" + cs + "]");
			final String key = s.substring(0, index).trim();
			final String value = s.substring(index+1).trim();
			p.setProperty(key, value);
			return this;
		}
		
		/**
		 * Removes properties with the passed keys
		 * @param keys The keys to remove properties for
		 * @return this builder
		 */
		public PropsBuilder remove(final Object...keys) {
			for(Object key: keys) {
				if(key==null) continue;
				p.remove(key);
			}
			return this;
		}
		
		/**
		 * Return the built properties
		 * @return the built properties
		 */
		public Properties getProperties() {
			return p;
		}
		
		/**
		 * Return a copy of the built properties
		 * @return a copy of the built properties
		 */
		public Properties getPropertiesCopy() {
			return new PropsBuilder(p).getProperties();
		}
		
		/**
		 * Returns the build properties as a UTF8 encoded string
		 * @param comments The optional comments
		 * @return the build properties as a string
		 */
		public String asString(final String comments) {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				p.store(baos, comments==null ? "" : comments);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to convert to a string");
			}
			return new String(baos.toByteArray(), UTF8);
		}
		
		/**
		 * Returns the build properties as a UTF8 encoded string with blank comments
		 * @return the build properties as a string
		 */
		public String asString() {
			return asString("");
		}
		
		
		/**
		 * Returns the build properties as a UTF8 encoded XML string
		 * @param comments The optional comments
		 * @return the build properties as an XML string
		 */
		public String asXML(final String comments) {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				p.storeToXML(baos, comments==null ? "" : comments);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to convert to XML");
			}
			return new String(baos.toByteArray(), UTF8);
		}
		
		/**
		 * Returns the build properties as a UTF8 encoded XML string with blank comments
		 * @return the build properties as an XML string
		 */
		public String asXML() {
			return asXML("");
		}
		
		/**
		 * Returns an array of strings containing the <b><code>-Dkey=value</code></b> command line
		 * directives to set system props.
		 * @return an array of strings
		 */
		public String[] asCommandLine() {
			final Set<String> cmds = new LinkedHashSet<String>();
			for(String key: p.stringPropertyNames()) {
				cmds.add("-D" + key + "=" + p.getProperty(key));
			}
			return cmds.toArray(new String[cmds.size()]);
		}		
	}
	
	/**
	 * Conditionally sets the key/values in the <code>from</code> properties into the <code>to</code> properties.
	 * For each property in <code>from</code>, if the key maps to a non-null and non-empty in any of the <code>unless</code> properties, 
	 * they pair will not be copied. If the key is bound to a non-null and non-empty value in the <code>to</code> properties, 
	 * the pair will not be copied unless <code>overrideTo</code> is set to true. 
	 * @param from The properties to copy from
	 * @param to The properties to copy to
	 * @param overrideTo true to override values in the <code>to</code> properties
	 * @param unless An array of properties to test to see if a <code>to</code> property exists in which case the pair will not be copied.
	 * @return true if any copies were made, false otherwise
	 */
	public static boolean setFromUnless(final Properties from, final Properties to, final boolean overrideTo, final Properties...unless) {
		if(from==null) throw new IllegalArgumentException("The passed from properties was null");
		if(to==null) throw new IllegalArgumentException("The passed to properties was null");
		if(from.isEmpty()) return false;
		boolean anySet = false;
		for(final String key: from.stringPropertyNames()) {
			if(anyContains(key, unless)) continue;
			final String toVal = to.getProperty(key);
			if(toVal==null || toVal.trim().isEmpty() || overrideTo) {
				to.setProperty(key, from.getProperty(key));
				anySet = true;
			}
		}
		return anySet;
	}
	
	/**
	 * Determines if any of the passed properties has a non-null and non-empty value for the passed key
	 * @param key The key to test for
	 * @param props The properties to tes
	 * @return true if the key is found, false otherwise
	 */
	public static boolean anyContains(final String key, final Properties...props) {
		if(props==null || props.length==0) return false;
		for(Properties p: props) {
			final String val = p.getProperty(key);
			if(val!=null && !val.trim().isEmpty()) return true;
		}
		return false;
	}

	
	/**
	 * Scans the passed source properties for any property keys starting with the passed prefix and copies any matching to the target which is returned.
	 * @param prefix The prefix to match
	 * @param source The source propereties to copy from
	 * @param removePrefix If true, the prefix is removed from the key before adding to the target
	 * @param removeSource if true, matching properties are removed from the source
	 * @return the target properties
	 */
	public static Properties extract(final String prefix, final Properties source, final boolean removePrefix, final boolean removeSource) {
		if(prefix==null || prefix.trim().isEmpty()) throw new IllegalArgumentException("The passed prefix was null or empty");
		if(source==null || source.isEmpty()) return new Properties();
		final String _prefix = prefix.trim().endsWith(".") ? prefix.trim() : (prefix.trim() + ".");
		final int len = _prefix.length();
		final Properties p = new Properties();
		if(!source.isEmpty()) {
			for(final String key: source.stringPropertyNames()) {
				if(key.startsWith(prefix)) {
					final String newKey = removePrefix ? key.substring(len) : key;
					p.setProperty(newKey, source.getProperty(key));
					if(removeSource) source.remove(key);
				}
			}
		}
		return p;
	}
	
	private Props() {}

}
