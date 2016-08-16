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
package com.heliosapm.utils.ref;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;

import javax.management.JMException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.ref.ReferenceService.ReferenceType;

/**
 * <p>Title: MBeanProxyBuilder</p>
 * <p>Description: Static builder functions for MBeanProxy instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ref.MBeanProxyBuilder</code></p>
 */

public class MBeanProxyBuilder {
	
	/** The MBeanProxy class name */
	public static final String MBEAN_PROXY_CLASS_NAME = "com.heliosapm.utils.ref.MBeanProxy";
	/** The MBeanProxy inner proxy field name */
	public static final String MBEAN_PROXY_FIELD_NAME = "proxy";
	
	/**
	 * Creates and returns an MBean proxy without registering it
	 * @param refType THe reference type
	 * @param mbeanInterface The MBean interface
	 * @param object The MBean impl
	 * @param proxyInterfaces Additional interfaces implemented by the proxy
	 * @return the MBean proxy which can be registered
	 */
	public static Object proxyMBean(ReferenceType refType, Class<?> mbeanInterface, Object object, Class<?>... proxyInterfaces)  {
		InvocationHandler mbeanProxy = (InvocationHandler)newProxy(refType, object);
		final Class<?> tClazz = object.getClass();
		final HashSet<Class<?>> ifaces = new HashSet<Class<?>>(Arrays.asList(tClazz.getInterfaces())); 
		ifaces.add(MBeanRegistration.class);
		Object proxy = mbeanInterface.cast(Proxy.newProxyInstance(
				mbeanInterface.getClassLoader(), ifaces.toArray(new Class[ifaces.size()]), mbeanProxy));
		setProxy(mbeanProxy, proxy, object);
		return mbeanProxy;
	}
	
	/**
	 * Creates a proxy MBean and registers it to the server, overriding the
	 * existing mbean if necessary.
	 * @param refType The reference type to store the actual impl
	 * @param server MBean will be registered to this server.
	 * @param name The name under which the MBean will be registered
	 * @param mbeanInterface MBean interface to be exposed
	 * @param object MBean instance to be exposed
	 */
	public static <T> void register(ReferenceType refType, MBeanServer server, ObjectName name, Class<T> mbeanInterface, T object) {
		try {
			final Class<?> tClazz = object.getClass();
			final HashSet<Class<?>> ifaces = new HashSet<Class<?>>(Arrays.asList(tClazz.getInterfaces())); 
			ifaces.add(MBeanRegistration.class);
			InvocationHandler mbeanProxy = (InvocationHandler)newProxy(refType, object);
//			Object mbeanProxy = Class.
//			T proxy = mbeanInterface.cast(Proxy.newProxyInstance(
//					mbeanInterface.getClassLoader(), new Class[] { mbeanInterface,
//							MBeanRegistration.class }, mbeanProxy));
			T proxy = mbeanInterface.cast(Proxy.newProxyInstance(tClazz.getClassLoader(), ifaces.toArray(new Class[ifaces.size()]), mbeanProxy));
			setProxy(mbeanProxy, proxy, object);
			if (server.isRegistered(name)) {
				try {
					server.unregisterMBean(name);
				} catch (JMException e) {
					// if we fail to unregister, try to register ours anyway.
					// maybe a GC kicked in in-between.
				}
			}
	
			// since the proxy class has random names like '$Proxy1',
			// we need to use StandardMBean to designate a management interface
			server.registerMBean(new StandardMBean(proxy, mbeanInterface), name);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	} 
	
	
	private static Object newProxy(final ReferenceType refType, final Object realObject) {
		try {
			final Class<?> clazz = Class.forName(MBEAN_PROXY_CLASS_NAME, true, realObject.getClass().getClassLoader());
			final Constructor<?> ctor = clazz.getDeclaredConstructor(ReferenceType.class, Object.class);
			return ctor.newInstance(refType, realObject);
			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create instance of MBeanProxy for [" + realObject + "]", ex);
		}
	}
	
	private static void setProxy(final Object mbeanProxy, final Object proxy, final Object realObject) {
		try {
			final Field f = mbeanProxy.getClass().getDeclaredField(MBEAN_PROXY_FIELD_NAME);
			f.set(mbeanProxy, proxy);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to set inner proxy on MBeanProxy for [" + realObject + "]", ex);
		}
	}

	/**
	 * Creates a proxy MBean and registers it to the default MBeanServer, overriding the
	 * existing mbean if necessary.
	 * @param refType The reference type to store the actual impl
	 * @param name The name under which the MBean will be registered
	 * @param mbeanInterface MBean interface to be exposed
	 * @param object MBean instance to be exposed
	 */
	public static <T> void register(ReferenceType refType, ObjectName name, Class<T> mbeanInterface, T object)  {
		try {
			register(refType, JMXHelper.getHeliosMBeanServer(), name, mbeanInterface, object);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	

	
	private MBeanProxyBuilder() {}

}
