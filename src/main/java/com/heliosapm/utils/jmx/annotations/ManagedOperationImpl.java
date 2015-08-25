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
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.DescriptorSupport;

import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

import com.heliosapm.utils.jmx.managed.Invoker;
import com.heliosapm.utils.lang.StringHelper;

/**
 * <p>Title: ManagedOperationImpl</p>
 * <p>Description: A concrete bean representing an extracted {@link ManagedOperation}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.annotation.ManagedOperationImpl</code></p>
 */

public class ManagedOperationImpl {
	/** The managed operation name. Defaults to the method name */
	protected final String name;
	/** The managed operation description */
	protected final String description;
	/** The managed operation impact */
	protected final int impact;
	/** The operation's managed parameters  */
	protected final ManagedOperationParameterImpl[] parameters;	
	/** An array of managed notifications that may be emitted by the annotated managed operation */
	protected final ManagedNotificationImpl[] notifications;
	
	/** empty const array */
	public static final ManagedOperationImpl[] EMPTY_ARR = {};
	
	/** empty const array */
	public static final MBeanOperationInfo[] EMPTY_INFO_ARR = {};
	
	
	/**
	 * Converts an array of ManagedOperations to an array of ManagedOperationImpls
	 * @param methods An array of methods, one for each ManagedOperationImpls
	 * @param ops the array of ManagedOperations to convert
	 * @return a [possibly zero length] array of ManagedOperationImpls
	 */
	public static ManagedOperationImpl[] from(Method[] methods, ManagedOperation...ops) {
		if(ops==null || ops.length==0 || methods==null || methods.length==0) return EMPTY_ARR;
		if(methods.length != ops.length) {
			throw new IllegalArgumentException("Method/Ops Array Size Mismatch. Methods:" + methods.length + ", ManagedOps:" + ops.length);
		}
		
		ManagedOperationImpl[] mopis = new ManagedOperationImpl[ops.length];
		for(int i = 0; i < ops.length; i++) {
			mopis[i] = new ManagedOperationImpl(methods[i].getName(), ops[i]);
		}
		return mopis;		
	}
	
	/**
	 * Generates an array of MBeanOperationInfos for the passed array of ManagedOperationImpls
	 * @param opInvokers A map of invokers to place this method's invoker into
	 * @param methods An array of methods, one for each ManagedOperationImpls
	 * @param ops The ManagedOperationImpls to convert
	 * @return a [possibly zero length] array of MBeanOperationInfos
	 */
	public static MBeanOperationInfo[] from(Method[] methods, ManagedOperationImpl...ops) {
		if(ops==null || ops.length==0) return EMPTY_INFO_ARR;
		if(methods.length != ops.length) {
			throw new IllegalArgumentException("Method/Ops Array Size Mismatch. Methods:" + methods.length + ", ManagedOps:" + ops.length);
		}
		MBeanOperationInfo[] infos = new MBeanOperationInfo[ops.length];
		for(int i = 0; i < infos.length; i++) {
			infos[i] = ops[i].toMBeanInfo(methods[i]);
		}		
		return infos;		
	}
	


	/**
	 * Creates a new ManagedOperationImpl
	 * @param methodName The method name if the annotation did not provide a name
	 * @param mo The managed operation to extract from
	 */
	public ManagedOperationImpl(String methodName, ManagedOperation mo) {
		name = nws(nvl(mo, "Managed Operation").name())==null ? methodName : mo.name().trim();
		description = nws(mo.description());
		impact = mo.impact();
		parameters = ManagedOperationParameterImpl.from(mo);
		notifications = ManagedNotificationImpl.from(mo.notifications());
	}
	
	/**
	 * Creates a new ManagedOperationImpl
	 * @param name The operation name specification
	 * @param description The operation description
	 * @param impact the operation impact
	 * @param notifications An array of managed notifications that may be emitted by the annotated managed operation
	 * @param parameters an array of the operation's managed parameters
	 */
	ManagedOperationImpl(CharSequence name, CharSequence description, int impact, ManagedNotification[] notifications, ManagedOperationParameter...parameters) {
		this.name = nws(name);
		this.description = nws(description);
		this.impact = impact;
		this.parameters = ManagedOperationParameterImpl.from(parameters);
		this.notifications = ManagedNotificationImpl.from(notifications); 
	}
	
	/**
	 * Creates a new ManagedOperationImpl
	 * @param name The operation name specification
	 * @param description The operation description
	 * @param impact the operation impact
	 * @param notifications An array of managed notifications that may be emitted by the annotated managed operation
	 * @param parameters an array of the operation's managed parameters
	 */
	ManagedOperationImpl(CharSequence name, CharSequence description, int impact, ManagedNotificationImpl[] notifications, ManagedOperationParameterImpl...parameters) {		
		this.name = nws(name);
		this.description = nws(description);
		this.impact = impact;
		this.notifications = notifications==null ? ManagedNotificationImpl.EMPTY_ARR : notifications; 
		this.parameters = parameters;
	}	
	

	/**
	 * Returns the managed operation name. Defaults to the method name 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the managed operation description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Returns the managed operation impact
	 * @return the impact
	 */
	public int getImpact() {
		return impact;
	}
	
	/**
	 * Returns the operation's managed parameters
	 * @return the parameters
	 */
	public ManagedOperationParameterImpl[] getParameters() {
		return parameters;
	}	
	
	/**
	 * Returns the array of managed notifications that may be emitted by the annotated managed operations
	 * @return the notifications
	 */
	public ManagedNotificationImpl[] getNotifications() {
		return notifications;
	}	
	
	/**
	 * Returns an MBeanOperationInfo rendered form this ManagedOperationImpl.
	 * @param method The method that this ManagedOperationImpl represents
	 * @param opInvokers A map of invokers to place this method's invoker into
	 * @return a MBeanOperationInfo rendered form this ManagedOperationImpl
	 */
	public MBeanOperationInfo toMBeanInfo(Method method) {		
		Class<?>[] sig = method.getParameterTypes();
		if(sig.length != parameters.length) {
			throw new IllegalArgumentException("Parameter Mismatch. Method:" + sig.length + ", ManagedParams:" + parameters.length);
		}
		return new MBeanOperationInfo(
				name,
				description,
				ManagedOperationParameterImpl.from(method.getParameterTypes(), parameters),
				method.getReturnType().getName(),
				impact,
				toDescriptor(method)
		);		
	}
	
	
	/**
	 * Generates a mutable MBean descriptor for this ManagedOperationImpl
	 * @param method The method we're generating a descriptor for
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor(Method method) {
		return toDescriptor(method, false);
	}
	
	/**
	 * Generates a MBean descriptor for this ManagedOperationImpl
	 * @param method The method we're generating a descriptor for
	 * @param immutable true for an immutable descriptor, false otherwise
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor(Method method, boolean immutable) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("signature", StringHelper.getMethodDescriptor(method));
		map.put("methodName", method.getName());
//		MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
//		map.put("methodDescriptor", methodType.toMethodDescriptorString());
//		Invoker mh = Invokers.exactInvoker(methodType);		
//		map.put("*methodHandle", mh);		
		return !immutable ?  new ImmutableDescriptor(map) : new DescriptorSupport(map.keySet().toArray(new String[map.size()]), map.values().toArray(new Object[map.size()]));	
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ManagedOperationImpl [name:%s, description:%s]", name==null ? "none" : name, description==null ? "none" : description);
	}

}
