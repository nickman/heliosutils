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
package com.heliosapm.utils.jmx.annotations;

import static com.heliosapm.utils.jmx.Reflector.nvl;
import static com.heliosapm.utils.jmx.Reflector.nws;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.modelmbean.DescriptorSupport;
import javax.xml.ws.spi.Invoker;

import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

import com.heliosapm.utils.jmx.OpenTypeFactory;
import com.heliosapm.utils.jmx.Reflector;
import com.heliosapm.utils.lang.StringHelper;

/**
 * <p>Title: ManagedAttributeImpl</p>
 * <p>Description: A concrete bean representing an extracted {@link ManagedAttribute}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.annotation.ManagedAttributeImpl</code></p>
 */

public class ManagedAttributeImpl {
	/** The managed attribute name. Defaults to the attributized method name */
	protected final String name;
	/** The managed attribute description */
	protected final String description;
	/** An array of managed notifications that may be emitted by the annotated managed attribute */
	protected final ManagedNotificationImpl[] notifications;

	/** empty const array */
	public static final ManagedAttributeImpl[] EMPTY_ARR = {};
	/** empty const array */
	public static final MBeanAttributeInfo[] EMPTY_INFO_ARR = {};
	
	

	
	/**
	 * Converts an array of ManagedAttributes to an array of ManagedAttributeImpls
	 * @param attrs the array of ManagedAttributes to convert
	 * @return a [possibly zero length] array of ManagedAttributeImpls
	 */
	public static ManagedAttributeImpl[] from(ManagedAttribute...attrs) {
		if(attrs==null || attrs.length==0) return EMPTY_ARR;
		ManagedAttributeImpl[] mopis = new ManagedAttributeImpl[attrs.length];
		for(int i = 0; i < attrs.length; i++) {
			mopis[i] = new ManagedAttributeImpl(attrs[i]);
		}
		return mopis;		
	}
	
	/**
	 * Generates an array of MBeanAttributeInfos for the passed array of ManagedAttributeImpls
	 * @param attrInvokers A map of attribute accessor invokers to populate
	 * @param methods An array of the methods being reflected, one for each managed ManagedAttributeImpls
	 * @param attributes The ManagedAttributeImpls to convert
	 * @return a [possibly zero length] array of MBeanAttributeInfos
	 */
	public static MBeanAttributeInfo[] from(final NonBlockingHashMapLong<Invoker[]> attrInvokers, Method[] methods, ManagedAttributeImpl...attributes) {
		if(attributes==null || attributes.length==0 || methods==null || methods.length==0) return EMPTY_INFO_ARR;
		if(methods.length != attributes.length) {
			throw new IllegalArgumentException("Type/Attribute Array Size Mismatch. Types:" + methods.length + ", Metrics:" + attributes.length);
		}		
		MBeanAttributeInfo[] infos = new MBeanAttributeInfo[attributes.length];
		for(int i = 0; i < infos.length; i++) {
			infos[i] = attributes[i].toMBeanInfo(methods[i]);
		}		
		return infos;		
	}
	
	
	/**
	 * Creates a new ManagedAttributeImpl
	 * @param ma The managed attribute to extract from
	 */
	public ManagedAttributeImpl(ManagedAttribute ma) {
		name = nws(nvl(ma, "Managed Attribute").name());
		description = nws(ma.description());
		this.notifications = ManagedNotificationImpl.from(ma.notifications());
	}
	
	/**
	 * Creates a new ManagedAttributeImpl
	 * @param method The method for the name will be used as the attribute name if the annotation does not supply one
	 * @param ma The managed attribute to extract from
	 */
	public ManagedAttributeImpl(Method method, ManagedAttribute ma) {
		name = nws(nvl(ma, "Managed Attribute").name())==null ? Reflector.attr(nvl(method, "method")) : ma.name().trim();
		description = nws(ma.description());
		this.notifications = ManagedNotificationImpl.from(ma.notifications());
	}
	
	
	/**
	 * Creates a new ManagedAttributeImpl
	 * @param name The attribute name specification
	 * @param description The attribute description
	 * @param notifications An array of managed notifications that may be emitted by the annotated managed attribute
	 */
	ManagedAttributeImpl(CharSequence name, CharSequence description, ManagedNotification...notifications) {
		this.name = nws(name);
		this.description = nws(description);
		this.notifications = ManagedNotificationImpl.from(notifications);
	}
	
	/**
	 * Creates a new ManagedAttributeImpl
	 * @param name The attribute name specification
	 * @param description The attribute description
	 * @param notifications An array of managed notifications that may be emitted by the annotated managed attribute
	 */
	ManagedAttributeImpl(CharSequence name, CharSequence description, ManagedNotificationImpl...notifications) {
		this.name = nws(name);
		this.description = nws(description);
		this.notifications = notifications;
	}	
	

	/**
	 * Returns the managed attribute name. Defaults to the attributized method name 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the managed attribute description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Returns the array of managed notifications that may be emitted by the annotated managed attribute
	 * @return the notifications
	 */
	public ManagedNotificationImpl[] getNotifications() {
		return notifications;
	}
	
	
	/**
	 * Returns an MBeanAttributeInfo rendered form this ManagedAttributeImpl.
	 * @param method The method we're generating an info for
	 * @return MBeanAttributeInfo rendered form this ManagedAttributeImpl
	 */
	public MBeanAttributeInfo toMBeanInfo(Method method) {
		boolean getter =  method.getParameterTypes().length==0;
		final long attrCode = StringHelper.longHashCode(name);
		return new MBeanAttributeInfo(
				name, 
				getter ? method.getReturnType().getName() : method.getParameterTypes()[0].getName(),  
				description, getter, !getter, false, toDescriptor(method)
		);
	}
	
	/**
	 * Generates a mutable MBean descriptor for this ManagedAttributeImpl
	 * @param method The method we're generating a descriptor for
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor(Method method) {
		return toDescriptor(method, false);
	}
	
	/**
	 * Generates a MBean descriptor for this ManagedAttributeImpl
	 * @param method The method we're generating a descriptor for
	 * @param immutable true for an immutable descriptor, false otherwise
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor(Method method, boolean immutable) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("signature", StringHelper.getMethodDescriptor(method));
		map.put(method.getParameterTypes().length==0 ? "getMethod" : "setMethod", method.getName());
		if(method.getName().startsWith("get") && method.getParameterTypes().length==0 && OpenTypeFactory.SIMPLE_TYPE_MAPPING.containsKey(method.getReturnType())) {
			map.put("openType", OpenTypeFactory.SIMPLE_TYPE_MAPPING.get(method.getReturnType()));
			map.put("originalType", method.getReturnType().getName());
		}

		return immutable ?  new ImmutableDescriptor(map) : new DescriptorSupport(map.keySet().toArray(new String[map.size()]), map.values().toArray(new Object[map.size()]));
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ManagedResourceImpl [name:%s, description:%s]", name==null ? "none" : name, description==null ? "none" : description);
	}



	
}
