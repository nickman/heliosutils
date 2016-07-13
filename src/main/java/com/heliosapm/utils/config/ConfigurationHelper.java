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

import java.io.File;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.heliosapm.utils.lang.StringHelper;
import com.heliosapm.utils.url.URLHelper;

import sun.reflect.Reflection;

/**
 * <p>Title: ConfigurationHelper</p>
 * <p>Description: Configuration util functions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.config.ConfigurationHelper</code></p>
 */

public class ConfigurationHelper {
	/** Empty String aray const */
	public static final String[] EMPTY_STR_ARR = {};
	/** Empty int aray const */
	public static final int[] EMPTY_INT_ARR = {};
	
	/** Comma splitter regex const */
	public static final Pattern COMMA_SPLITTER = Pattern.compile(",");
	
	/** If property names start with this, system properties and environment variables should be ignored. */
	public static final String NOSYSENV = "tsd.";
	
	/** App property keys that start with this are set as system properties (minus the prefix) */
	public static final String SYSPROP_PREFIX = "system.";
	private static final int SYSPROP_PREFIX_LEN = SYSPROP_PREFIX.length();
	
	
	/** App specified properties that take presedence over sys props, but are not IN sys props */
	private static final AtomicReference<Properties> appProperties = new AtomicReference<Properties>(null); 

	/**
	 * Sets the app properties
	 * @param p The properties to set
	 * @return true if the app properties were set, false if they were already set
	 */
	public static boolean setAppProperties(final Properties p) {
		if(p==null) throw new IllegalArgumentException("The passed properties were null");
		final boolean wasSet = appProperties.compareAndSet(null, p);
		for(final String key: p.stringPropertyNames()) {
			if(key.startsWith(SYSPROP_PREFIX)) {
				final String value = p.getProperty(key);
				final String sysKey = key.substring(SYSPROP_PREFIX_LEN);				
				System.setProperty(sysKey, value);
//				System.err.println("SET SYSPROP [" + sysKey + ":" + value + "]");
			}
		}
		return wasSet;
	}
	
	
	/**
	 * Merges the passed properties
	 * @param properties The properties to merge
	 * @return the merged properties
	 */
	public static Properties mergeProperties(Properties...properties) {
		Properties allProps = new Properties(System.getProperties());
		for(int i = properties.length-1; i>=0; i--) {
			if(properties[i] != null && properties[i].size() >0) {
				allProps.putAll(properties[i]);
			}
		}
		final Properties appProps = appProperties.get();
		if(appProps!=null) {
			allProps.putAll(appProps);
		}
		return allProps;
	}
	
	
	/**
	 * Looks up a property, first in the environment, then the system properties. 
	 * If not found in either, returns the supplied default.
	 * @param name The name of the key to look up.
	 * @param defaultValue The default to return if the name is not found.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located value or the default if it was not found.
	 */
	public static String getEnvThenSystemProperty(String name, String defaultValue, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		String value = System.getenv(name.replace('.', '_'));
		if(value==null) {			
			value = mergeProperties(properties).getProperty(name);
		}
		if(value==null) {
			value=defaultValue;
		}
		return appendAudit(caller, name, defaultValue, value);
	}
	
	/**
	 * Looks up a property, first in the system properties, then the environment. 
	 * If not found in either, returns the supplied default.
	 * @param name The name of the key to look up.
	 * @param defaultValue The default to return if the name is not found.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located value or the default if it was not found.
	 */
	public static String getSystemThenEnvProperty(String name, String defaultValue, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed property name was null or empty");
		if(name.trim().toLowerCase().startsWith(NOSYSENV)) {
			if(properties==null || properties.length==0 || properties[0]==null) return defaultValue;
			return properties[0].getProperty(name.trim(), defaultValue);
		}
		String value = mergeProperties(properties).getProperty(name);
		if(value==null) {
			value = System.getenv(name.replace('.', '_').toUpperCase());
		}
		if(value==null) {
			value=defaultValue;
		}
		appendAudit(caller, name, defaultValue, value);
		return value;
	}
	
	/** The default value passed for an empty array */
	public static final String EMPTY_ARRAY_TOKEN = "_org_helios_empty_array_";
	
	/**
	 * Looks up a property and converts to a string array, first in the system properties, then the environment. 
	 * If not found in either, returns the supplied default.
	 * @param name The name of the key to look up.
	 * @param defaultValue The default to return if the name is not found. Expected as a comma separated list of strings
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located value or the default if it was not found.
	 */
	public static String[] getSystemThenEnvPropertyArray(String name, String defaultValue, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		String[] value = null;
		String[] defValue = StringHelper.splitString(defaultValue, ',', true);
		final String raw = getSystemThenEnvProperty(name, null, properties);
		if(raw==null || raw.trim().isEmpty()) {
			value = defValue;
		} else {
			value = StringHelper.splitString(raw, ',', true);
		}
		return appendAudit(caller, name, defValue, value); 
	}

	/**
	 * Looks up a property and converts to an int array, first in the system properties, then the environment. 
	 * If not found in either, returns the supplied default.
	 * @param name The name of the key to look up.
	 * @param defaultValue The default to return if the name is not found. Expected as a comma separated list of strings
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located value or the default if it was not found.
	 */
	public static int[] getIntSystemThenEnvPropertyArray(String name, String defaultValue, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		String raw = getSystemThenEnvProperty(name, defaultValue, properties);
		int[] rez = null;
		if(raw==null || raw.trim().isEmpty()) {
			rez = EMPTY_INT_ARR;
		} else {
			List<Integer> values = new ArrayList<Integer>();
			for(String s: COMMA_SPLITTER.split(raw.trim())) {
				if(s.trim().isEmpty()) continue;
				try { values.add(new Integer(s.trim())); } catch (Exception ex) {}
			}		
			if(values.isEmpty()) {
				rez = EMPTY_INT_ARR;
			} else {
				rez = new int[values.size()];
				for(int i = 0; i < values.size(); i++) {
					rez[i] = values.get(i);
				}
			}
		}
		return appendAudit(caller, name, parseToIntArr(defaultValue), rez);
	}
	
	private static int[] parseToIntArr(final String intArr) {
		if(intArr==null || intArr.trim().isEmpty()) return EMPTY_INT_ARR;
		final String[] sarr = StringHelper.splitString(intArr, ',', true);
		if(sarr.length==0) return EMPTY_INT_ARR;
		final int[] iarr = new int[sarr.length];
		for(int i = 0; i < sarr.length; i++) {
			iarr[i] = Integer.parseInt(sarr[i].trim());
		}
		return iarr;
	}
	
	
	/**
	 * Determines if a name has been defined in the environment or system properties.
	 * @param name the name of the property to check for.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return true if the name is defined in the environment or system properties.
	 */
	public static boolean isDefined(String name, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		boolean value = false;
		if(System.getenv(name) != null) {
			value = true;
		} else {
			if(mergeProperties(properties).getProperty(name) != null) {
				value = true;
			}
			value = false;					
		}
		return appendAudit(caller, name, value, value);
	}
	
	/**
	 * Determines if a name has been defined as a valid int in the environment or system properties.
	 * @param name the name of the property to check for.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return true if the name is defined as a valid int in the environment or system properties.
	 */
	public static boolean isIntDefined(String name, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		String tmp = getEnvThenSystemProperty(name, null, properties);
		if(tmp==null) return false;
		try {
			Integer.parseInt(tmp);
			return true;
		} catch (Exception e) {
			return false;
		}				
	}
	
	/**
	 * Determines if a name has been defined as a valid boolean in the environment or system properties.
	 * @param name the name of the property to check for.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return true if the name is defined as a valid boolean in the environment or system properties.
	 */
	public static boolean isBooleanDefined(String name, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		String tmp = getEnvThenSystemProperty(name, null, properties);
		boolean value = false;
		if(tmp==null) {
			value = false;
		} else {
			try {
				tmp = tmp.toUpperCase();
				if(
						tmp.equalsIgnoreCase("TRUE") || tmp.equalsIgnoreCase("Y") || tmp.equalsIgnoreCase("YES") ||
						tmp.equalsIgnoreCase("FALSE") || tmp.equalsIgnoreCase("N") || tmp.equalsIgnoreCase("NO")
				) value = true;
				value = false;
			} catch (Exception e) {
				value = false;
			}							
		}
		return appendAudit(caller, name, value, value);
	}	
	
	/**
	 * Determines if a name has been defined as a valid long in the environment or system properties.
	 * @param name the name of the property to check for.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return true if the name is defined as a valid long in the environment or system properties.
	 */
	public static boolean isLongDefined(String name, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		String tmp = getEnvThenSystemProperty(name, null, properties);
		if(tmp==null) return false;
		try {
			Long.parseLong(tmp);
			return true;
		} catch (Exception e) {
			return false;
		}				
	}
	
	/**
	 * Returns the value defined as an Integer looked up from the Environment, then System properties.
	 * @param name The name of the key to lookup.
	 * @param defaultValue The default value to return if the name is not defined or the value is not a valid int.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located integer or the passed default value.
	 */
	public static Integer getIntSystemThenEnvProperty(String name, Integer defaultValue, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		String tmp = getSystemThenEnvProperty(name, null, properties);
		Integer value = null;
		try {
			value = Integer.parseInt(tmp);
		} catch (Exception e) {
			value = defaultValue;
		}
		return appendAudit(caller, name, defaultValue, value);
	}
	
	/**
	 * Returns the value defined as an character looked up from the Environment, then System properties.
	 * Note that if the resolved property is a string of more than one character, the first one is accepted silently.
	 * @param name The name of the key to lookup.
	 * @param defaultValue The default value to return if the name is not defined or the value is not a valid int.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located character or the passed default value.
	 */
	public static Character getCharSystemThenEnvProperty(String name, Character defaultValue, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		String tmp = getSystemThenEnvProperty(name, null, properties);
		Character rez;
		try {
			rez = tmp.charAt(0);
		} catch (Exception e) {
			rez = defaultValue;
		}
		return appendAudit(caller, name, defaultValue, rez);
	}
	
	
	/**
	 * Returns the value defined as an Float looked up from the Environment, then System properties.
	 * @param name The name of the key to lookup.
	 * @param defaultValue The default value to return if the name is not defined or the value is not a valid int.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located float or the passed default value.
	 */
	public static Float getFloatSystemThenEnvProperty(String name, Float defaultValue, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		String tmp = getSystemThenEnvProperty(name, null, properties);
		Float value = null;
		try {
			value = Float.parseFloat(tmp);
		} catch (Exception e) {
			value = defaultValue;
		}
		return appendAudit(caller, name, defaultValue, value);
	}
	
	
	/**
	 * Returns the value defined as a Long looked up from the Environment, then System properties.
	 * @param name The name of the key to lookup.
	 * @param defaultValue The default value to return if the name is not defined or the value is not a valid long.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located long or the passed default value.
	 */
	public static Long getLongSystemThenEnvProperty(String name, Long defaultValue, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		String tmp = getSystemThenEnvProperty(name, null, properties);
		Long value = null;
		try {
			value = Long.parseLong(tmp);
		} catch (Exception e) {
			value = defaultValue;
		}
		return appendAudit(caller, name, defaultValue, value);
	}	
	
	/**
	 * Returns the value defined as a Boolean looked up from the Environment, then System properties.
	 * @param name The name of the key to lookup.
	 * @param defaultValue The default value to return if the name is not defined or the value is not a valid boolean.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located boolean or the passed default value.
	 */
	public static Boolean getBooleanSystemThenEnvProperty(String name, Boolean defaultValue, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		String tmp = getSystemThenEnvProperty(name, null, properties);
		final boolean rez;
		if(tmp==null) {
			rez = defaultValue;
		} else {
			tmp = tmp.toUpperCase();
			if(tmp.equalsIgnoreCase("TRUE") || tmp.equalsIgnoreCase("Y") || tmp.equalsIgnoreCase("YES")) rez = true;
			else if(tmp.equalsIgnoreCase("FALSE") || tmp.equalsIgnoreCase("N") || tmp.equalsIgnoreCase("NO")) rez = false;
			else rez = defaultValue;

		}
		return appendAudit(caller, name, defaultValue, rez);
	}
	
	/**
	 * Returns the value defined as a URL looked up from the Environment, then System properties.
	 * @param name The name of the key to lookup.
	 * @param defaultValue The default value to return as a URL if the name is not defined or the value is not a valid URL.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located URL or the passed default value.
	 */
	public static URL getURLSystemThenEnvProperty(String name, String defaultValue, Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		final String tmp = getSystemThenEnvProperty(name, null, properties);
		URL value = null;
		URL defValue = (defaultValue==null || defaultValue.trim().isEmpty()) ? null : URLHelper.toURL(defaultValue);
		try {
			value = URLHelper.toURL(tmp);
		} catch (Exception e) {
			value = defValue;
		}
		return appendAudit(caller, name, defValue, value);
	}
	
	/**
	 * Returns the value defined as a string array looked up from the System properties, then Environment.
	 * The values should be comma separated. Strips out any null or blank entries.
	 * @param name The name of the key to lookup.
	 * @param defaultValue The default value to return as a String array if the name is not defined.
	 * @param properties An array of properties to search in. If empty or null, will search system properties, then Environment. The first located match will be returned.
	 * @return The string array or the passed default value.
	 */
	public static String[] getArraySystemThenEnvProperty(final String name, final String[] defaultValue, final Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		final String tmp = getSystemThenEnvProperty(name, "", properties).trim();
		if(tmp==null || tmp.isEmpty()) return defaultValue;
		final String[] arr = tmp.split(",");
		final List<String> list = new ArrayList<String>(arr.length);
		for(int i = 0; i < arr.length; i++) {
			if(arr[i]==null || arr[i].trim().isEmpty()) continue;
			list.add(arr[i].trim());
		}
		return appendAudit(caller, name, defaultValue, list.toArray(new String[list.size()]));
	}
	
	/**
	 * Returns the value defined as a string array looked up from the System properties, then Environment.
	 * The values should be comma separated. Strips out any null or blank entries.
	 * @param name The name of the key to lookup.
	 * @param defaultValue The default value to return as a String array if the name is not defined.
	 * @param properties An array of properties to search in. If empty or null, will search system properties, then Environment. The first located match will be returned.
	 * @return The string array or the passed default value.
	 */
	public static String[] getArraySystemThenEnvProperty(final String name, final Collection<String> defaultValue, final Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;		
		return appendAudit(caller, name, defaultValue.toArray(new String[0]), getArraySystemThenEnvProperty(name, defaultValue.toArray(new String[0]), properties));
	}
	
	
	/**
	 * Returns the enum member indicated by the configured value looked up in System Properties then Environment with the passed name automatically upper cased.
	 * @param enumType The enum type
	 * @param name The config name
	 * @param defaultEnum The default enum member
	 * @param properties An array of properties to search in. If empty or null, will search system properties, then Environment. The first located match will be returned.
	 * @return the configured enum member or the default if one cannot be decoded
	 */
	public static <E extends Enum<E>> E getEnumUpperSystemThenEnvProperty(final Class<E> enumType, final String name, final E defaultEnum, final Properties...properties) {
		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		return appendAudit(caller, name, defaultEnum, getEnumSystemThenEnvProperty(enumType, name==null ? null : name.toUpperCase(), defaultEnum, properties)); 
	}
	
	/**
	 * Returns the enum member indicated by the configured value looked up in System Properties then Environment.
	 * @param enumType The enum type
	 * @param name The config name
	 * @param defaultEnum The default enum member
	 * @param properties An array of properties to search in. If empty or null, will search system properties, then Environment. The first located match will be returned.
	 * @return the configured enum member or the default if one cannot be decoded
	 */
	public static <E extends Enum<E>> E getEnumSystemThenEnvProperty(final Class<E> enumType, final String name, final E defaultEnum, final Properties...properties) {
 		final Class<?> caller = AUDIT_ENABLED ? Reflection.getCallerClass() : null;
		if(enumType==null) throw new IllegalArgumentException("The passed enum type was null");
		E rez = null;
		try {
			final String n = getSystemThenEnvProperty(name, null, properties);			
			if(n!=null && !n.trim().isEmpty()) {
				final String _n = n.trim();
				rez = Enum.valueOf(enumType, _n);
				return appendAudit(caller, name, defaultEnum, rez);
			}			
		} catch (Exception ex) {
			/* No Op */
		}
		rez = defaultEnum;
		return appendAudit(caller, name, defaultEnum, rez);
	}
	
	
	/**
	 * Attempts to create an instance of the passed class using one of:<ol>
	 * 	<li>Attempts to find a Constructor with the passed signature</li>
	 * 	<li>Attempts to find a static factory method called <b><code>getInstance</code></b> with the passed signature</li>
	 * 	<li>Attempts to find a static factory method called <b><code>newInstance</code></b> with the passed signature</li>
	 * </ol>
	 * @param clazz The class to create an instance of
	 * @param sig The signature of the constructor or static factory method
	 * @param args The arguments to the constructor or static factory method
	 * @return The created instance
	 * @throws Exception thrown on any error
	 */
	public static <T> T inst(Class<T> clazz, Class<?>[] sig, Object...args) throws Exception {
		Constructor<T> ctor = null;
		try {
			ctor = clazz.getDeclaredConstructor(sig);
			return ctor.newInstance(args);
		} catch (Exception e) {
			Method method = null;
			try { method = clazz.getDeclaredMethod("getInstance", sig); 
				if(!Modifier.isStatic(method.getModifiers())) throw new Exception();
			} catch (Exception ex) {}
			if(method==null) {
				try { method = clazz.getDeclaredMethod("newInstance", sig); } catch (Exception ex) {}
			}
			if(method==null) throw new Exception("Failed to find Constructor or Static Factory Method for [" + clazz.getName() + "]");
			if(!Modifier.isStatic(method.getModifiers())) throw new Exception("Factory Method [" + method.toGenericString() + "] is not static");
			return (T)method.invoke(null, args);
		}
	}
	
	/** Empty class signature const */
	public static final Class<?>[] EMPTY_SIG = {};
	/** Empty arg const */
	public static final Object[] EMPTY_ARGS = {};
	
	/**
	 * Attempts to create an instance of the passed class using one of:<ol>
	 * 	<li>Attempts to find a Constructor</li>
	 * 	<li>Attempts to find a static factory method called <b><code>getInstance</code></b></li>
	 * 	<li>Attempts to find a static factory method called <b><code>newInstance</code></b></li>
	 * </ol>
	 * @param clazz The class to create an instance of
	 * @return The created instance
	 * @throws Exception thrown on any error
	 */
	public static <T> T inst(Class<T> clazz) throws Exception {
		return inst(clazz, EMPTY_SIG, EMPTY_ARGS);
	}

	private static final String PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	private static final boolean AUDIT_ENABLED;
	private static final SimpleDateFormat AUDIT_SDF = new SimpleDateFormat("yyyyMMdd:HHmmss.SS");
	private static final File AUDIT_FILE = new File(System.getProperty("java.io.tmpdir") + File.separator + "configuration-audit-" + PID + ".log");
	private static final String EOL = System.getProperty("line.separator", "\n");
	private static final ThreadLocal<String> AUDIT_CURRENT_KEY = new ThreadLocal<String>();
	static {
		final boolean enabled = "true".equalsIgnoreCase(System.getProperty("config.helper.audit.enabled", "false"));
		boolean keepEnabled = true;
		if(!enabled) {
			keepEnabled = false;
		} else {
			if(AUDIT_FILE.exists()) {
				if(!AUDIT_FILE.delete()) {
					keepEnabled = false;
					System.err.println("Failed to delete Audit File [" + AUDIT_FILE + "]");
				}
			}
			if(keepEnabled) {
				try {
					AUDIT_FILE.createNewFile();
				} catch (Exception ex) {
					keepEnabled = false;
					System.err.println("Failed to create Audit File [" + AUDIT_FILE + "]: " + ex);
				}			
			}			
		}		
		AUDIT_ENABLED = keepEnabled;
		if(AUDIT_ENABLED) {
			System.out.println("ConfigurationHelper Audit Enabled. File: [" + AUDIT_FILE + "]");
		}
	}
	
	private static synchronized <T> T appendAudit(final Class<?> caller, final String key, final T defaultValue, final T configValue) {
		if(caller==null || caller==ConfigurationHelper.class) return configValue;
		FileWriter fw = null;
		
		final String def = defaultValue==null ? "<null>" :
			defaultValue.getClass().isArray() ? Arrays.toString((String[])defaultValue) : defaultValue.toString();
		final String val = configValue==null ? "<null>" :
			configValue.getClass().isArray() ? Arrays.toString((String[])configValue) : configValue.toString();
			
		try {
			fw = new FileWriter(AUDIT_FILE, true);
			fw.append(new StringBuilder(AUDIT_SDF.format(new Date())).append(",")
					.append(caller.getName()).append(",")
					.append(key).append(",")
					.append(val).append(EOL)
			);
			fw.flush();
		} catch (Exception ex) {
			System.err.println("ConfigurationHelper Audit Write Error:" + ex);
		} finally {
			if(fw!=null) try { fw.close(); } catch (Exception x) {/* No Op */}
		}
		return configValue;
	}
	
	
}
