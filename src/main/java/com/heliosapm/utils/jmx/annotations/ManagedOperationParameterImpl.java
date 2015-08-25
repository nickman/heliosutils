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


import java.util.HashMap;
import java.util.Map;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.DescriptorSupport;

import static com.heliosapm.utils.jmx.Reflector.*;

/**
 * <p>Title: ManagedOperationParameterImpl</p>
 * <p>Description: A concrete bean representing an extracted {@link ManagedOperationParameter}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.annotation.ManagedOperationParameterImpl</code></p>
 */

public class ManagedOperationParameterImpl {
	/** The managed parameter name. */
	protected final String name;

	/** The managed parameter description */
	protected final String description;
	
	/** empty const array */
	public static final ManagedOperationParameterImpl[] EMPTY_ARR = {};
	/** empty const array */
	public static final MBeanParameterInfo[] EMPTY_INFO_ARR = {};
	
	
	/**
	 * Extracts the ManagedOperationParameter from the passed ManagedOperation and returns an array of ManagedOperationParameterImpls.
	 * @param mo The ManagedOperation to extract from
	 * @return a [possibly zero length] array of ManagedOperationParameterImpls
	 */
	public static ManagedOperationParameterImpl[] from(ManagedOperation mo) {
		ManagedOperationParameter[] mops = nvl(mo, "Managed Operation").parameters();
		return from(mops);
	}
	
	/**
	 * Converts an array of ManagedOperationParameters to an array of ManagedOperationParameterImpls
	 * @param mops the array of ManagedOperationParameters to convert
	 * @return a [possibly zero length] array of ManagedOperationParameterImpls
	 */
	public static ManagedOperationParameterImpl[] from(ManagedOperationParameter... mops) {
		if(mops==null || mops.length==0) return EMPTY_ARR;
		ManagedOperationParameterImpl[] mopis = new ManagedOperationParameterImpl[mops.length];
		for(int i = 0; i < mops.length; i++) {
			mopis[i] = new ManagedOperationParameterImpl(mops[i]);
		}
		return mopis;		
	}
	
	/**
	 * Generates an array of MBeanParameterInfos for the passed array of ManagedOperationParameterImpls
	 * @param attrTypes An array of types, one type for each ManagedOperationParameterImpls
	 * @param params The ManagedOperationParameterImpls to convert
	 * @return a [possibly zero length] array of MBeanParameterInfos
	 */
	public static MBeanParameterInfo[] from(Class<?>[] attrTypes, ManagedOperationParameterImpl...params) {
		if(params==null || params.length==0) return EMPTY_INFO_ARR;
		MBeanParameterInfo[] infos = new MBeanParameterInfo[params.length];
		for(int i = 0; i < infos.length; i++) {
			infos[i] = params[i].toMBeanInfo(attrTypes[i]);
		}		
		return infos;		
	}
	

	/**
	 * Creates a new ManagedOperationParameterImpl
	 * @param mop The managed parameter to extract from
	 */
	public ManagedOperationParameterImpl(ManagedOperationParameter mop) {
		name = nws(nvl(mop, "Managed Attribute").name());
		description = nws(mop.description());
	}
	
	/**
	 * Creates a new ManagedOperationParameterImpl
	 * @param name The parameter name specification
	 * @param description The parameter description
	 */
	ManagedOperationParameterImpl(CharSequence name, CharSequence description) {
		this.name = nws(name);
		this.description = nws(description);
	}	

	/**
	 * Returns the managed parameter name. Defaults to the attributized method name 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the managed parameter description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Returns an MBeanParameterInfo rendered form this ManagedOperationParameterImpl.
	 * @param attrType The type of the attribute
	 * @return a MBeanParameterInfo rendered form this ManagedOperationParameterImpl
	 */
	public MBeanParameterInfo toMBeanInfo(Class<?> attrType) {		
		return new MBeanParameterInfo(
				name,
				attrType.getName(),
				description,				
				toDescriptor()
		);		
	}
	
	
	/**
	 * Generates a mutable MBean descriptor for this ManagedOperationParameterImpl
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor() {
		return toDescriptor(false);
	}
	
	/**
	 * Generates a MBean descriptor for this ManagedOperationParameterImpl
	 * @param immutable true for an immutable descriptor, false otherwise
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor(boolean immutable) {
		Map<String, Object> map = new HashMap<String, Object>();
		// FIXME:
		return !immutable ?  new ImmutableDescriptor(map) : new DescriptorSupport(map.keySet().toArray(new String[map.size()]), map.values().toArray(new Object[map.size()]));	
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ManagedOperationParameterImpl [name:%s, description:%s]", name==null ? "none" : name, description==null ? "none" : description);
	}	




}
