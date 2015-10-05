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
package com.heliosapm.utils.jmx;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

/**
 * <p>Title: ConfigurationConstantsService</p>
 * <p>Description: Exposes confguration constants defined by a specified class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.ConfigurationConstantsService</code></p>
 */

public class ConfigurationConstantsService implements DynamicMBean {
	/** The configured classes */
	private static final ConcurrentHashMap<Class<?>, ConfigurationConstantsService> constantClasses = new ConcurrentHashMap<Class<?>, ConfigurationConstantsService>(); 

	/** The constants configuration class */
	private final Class<?> clazz;
	/** The property prefix and optional default prefix */
	private final String[] prefixes;
	/** The property prefix and optional default prefix */
	private final ConcurrentHashMap<String, String[]> prefixFields = new ConcurrentHashMap<String, String[]>();
	
	/** Static class logger */
	private final Logger log = Logger.getLogger(getClass().getName()); 

	/**
	 * Acquires the ConfigurationConstantsService for the passed class
	 * @param clazz The constants configuration class
	 * @param prefixes The property prefix and optional default prefix
	 * @return the ConfigurationConstantsService for the passed class
	 */
	public static ConfigurationConstantsService getInstance(final Class<?> clazz, final String[] prefixes) {
		if(clazz==null) throw new IllegalArgumentException("The passed class was null");
		if(prefixes==null || prefixes.length==0) throw new IllegalArgumentException("The passed prefix array for class [" + clazz.getName() + "] was null or zero length");
		if(prefixes[0]==null || prefixes[0].trim().isEmpty()) throw new IllegalArgumentException("The passed prefix[0] for class [" + clazz.getName() + "] was null or zero length");
		if(prefixes.length > 1) if(prefixes[1]==null || prefixes[1].trim().isEmpty()) throw new IllegalArgumentException("The passed prefix[1] for class [" + clazz.getName() + "] was null or zero length");
		ConfigurationConstantsService svc = constantClasses.get(clazz);
		if(svc==null) {
			synchronized(constantClasses) {
				svc = constantClasses.get(clazz);
				if(svc==null) {
					svc = new ConfigurationConstantsService(clazz, prefixes);
				}
			}
		}
		return svc;
	}
	
	/**
	 * Creates a new ConfigurationConstantsService
	 * @param clazz The constants configuration class
	 * @param prefixes The property prefix and optional default prefix
	 */
	private ConfigurationConstantsService(final Class<?> clazz, final String[] prefixes) {
		this.clazz = clazz;
		this.prefixes = prefixes;
		try {
			// prefixFields = new ConcurrentHashMap<String, String[]>();
			
			for(Field f: clazz.getDeclaredFields()) {
				final int mod = f.getModifiers();
				if(Modifier.isStatic(mod) && Modifier.isPublic(mod) && Modifier.isFinal(mod)) {
					Field df = null;
					final String pname = f.getName();
					final String dname;
					if(pname.startsWith(this.prefixes[0])) {
						if(prefixes.length==1) {
							dname = null;
						} else {
							String def = pname.replace(this.prefixes[0], this.prefixes[1]);
							String tmp = null;
							try {
								df = clazz.getDeclaredField(def);
								final int mod2 = df.getModifiers();
								if(Modifier.isStatic(mod2) && Modifier.isPublic(mod2) && Modifier.isFinal(mod2)) {
									tmp = df.getName();
								}
								tmp = null;
							} catch (NoSuchFieldException nfe) {
								tmp = null;
							}
							dname = tmp;
							final String[] pair = new String[dname==null ? 1 : 2];
							pair[0] = pname;
							if(dname!=null) pair[1] = dname;
//							prefixFields.put(pname, f.get(null).toString(), )
							
						}
					}
				}
			}
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Failed to create ConfigurationConstantsService for [" + clazz.getName() + "]", ex);
			throw new RuntimeException("Failed to create ConfigurationConstantsService for [" + clazz.getName() + "]", ex);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.DynamicMBean#getAttribute(java.lang.String)
	 */
	@Override
	public Object getAttribute(String attribute)
			throws AttributeNotFoundException, MBeanException, ReflectionException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
	 */
	@Override
	public void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.DynamicMBean#getAttributes(java.lang.String[])
	 */
	@Override
	public AttributeList getAttributes(String[] attributes) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.DynamicMBean#setAttributes(javax.management.AttributeList)
	 */
	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.DynamicMBean#invoke(java.lang.String, java.lang.Object[], java.lang.String[])
	 */
	@Override
	public Object invoke(String actionName, Object[] params, String[] signature)
			throws MBeanException, ReflectionException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.DynamicMBean#getMBeanInfo()
	 */
	@Override
	public MBeanInfo getMBeanInfo() {
		// TODO Auto-generated method stub
		return null;
	}

}
