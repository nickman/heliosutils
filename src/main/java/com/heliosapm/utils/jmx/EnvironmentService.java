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

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * <p>Title: EnvironmentService</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.EnvironmentService</code></p>
 */

public class EnvironmentService implements DynamicMBean {
	/** The singleton instance */
	private static volatile EnvironmentService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();

	/** Static class logger */
	private final Logger log = Logger.getLogger(getClass().getName()); 
	
	private final Map<String, MBeanAttributeInfo> attrInfos = new ConcurrentSkipListMap<String, MBeanAttributeInfo>();
	private final MBeanOperationInfo[] opInfos = new MBeanOperationInfo[]{
		new MBeanOperationInfo("refresh", "Rereads the environment and regenrates the meta-data", new MBeanParameterInfo[0], Void.class.getName(), MBeanOperationInfo.INFO)
	};
	
	private volatile MBeanInfo info = null;
	
	
	/** The service JMX ObjectName */
	public static final ObjectName objectName = JMXHelper.objectName("com.heliosapm.env:service=Environment");

	/**
	 * Acquires and returns the EnvironmentService singleton
	 * @return the EnvironmentService singleton
	 */
	public static EnvironmentService getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new EnvironmentService();
				}
			}
		}
		return instance;
	}

	
	/**
	 * Creates a new EnvironmentService
	 */
	private EnvironmentService() {
		refresh();
		JMXHelper.registerMBean(this, objectName);
		log.fine("Environment Service installed at [" + objectName + "]");
	}
	
	private void refresh() {
		attrInfos.clear();
		populateAttrs();
		info = new MBeanInfo(
				getClass().getName(), "MBean to expose the process environment",
				attrInfos.values().toArray(new MBeanAttributeInfo[attrInfos.size()]),
				new MBeanConstructorInfo[0],			
				opInfos, 
				new MBeanNotificationInfo[0] 			
			);
	}
	
	private void populateAttrs() {
		final Map<String, String> env = System.getenv();
		for(Map.Entry<String, String> entry: env.entrySet()) {
			final MBeanAttributeInfo info = new MBeanAttributeInfo(
					entry.getKey(), 
					String.class.getName(), 
					"Environment Variable", true, false, false 
				); 
			attrInfos.put(entry.getKey(), info);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.DynamicMBean#getAttribute(java.lang.String)
	 */
	@Override
	public Object getAttribute(final String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
		final Map<String, String> env = System.getenv();
		if(!env.containsKey(attribute)) throw new AttributeNotFoundException("No env entry for [" + attribute + "]");
		return env.get(attribute);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
	 */
	@Override
	public void setAttribute(final Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.DynamicMBean#getAttributes(java.lang.String[])
	 */
	@Override
	public AttributeList getAttributes(final String[] attributes) {
		final Map<String, String> env = System.getenv();
		final AttributeList attrList = new AttributeList();
		for(String s: attributes) {
			if(env.containsKey(s)) {
				attrList.add(new Attribute(s, env.get(s)));
			}
		}
		return attrList;
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see javax.management.DynamicMBean#setAttributes(javax.management.AttributeList)
	 */
	@Override
	public AttributeList setAttributes(final AttributeList attributes) {
		return new AttributeList();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.DynamicMBean#invoke(java.lang.String, java.lang.Object[], java.lang.String[])
	 */
	@Override
	public Object invoke(final String actionName, final Object[] params, final String[] signature) throws MBeanException, ReflectionException {
		if("refresh".equals(actionName)) {
			refresh();
			return null;
		}
		throw new MBeanException(new Exception(), "No operation named [" + actionName + "]");
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.DynamicMBean#getMBeanInfo()
	 */
	@Override
	public MBeanInfo getMBeanInfo() {
		return info;
	}
	
	public static void main(String[] args) {
		getInstance();
		try { Thread.currentThread().join(); } catch (Exception x) {/* No Op */}
	}

}
