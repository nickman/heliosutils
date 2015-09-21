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

import java.lang.ref.Reference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.ref.ReferenceService.ReferenceType;

/**
 * <p>Title: MBeanProxy</p>
 * <p>Description: Proxy MBean that avoid strong reference to the real MBean object</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Kohsuke Kawaguchi
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.util.reference.MBeanProxy</code></p>
 */
public class MBeanProxy implements InvocationHandler, MBeanRegistration, ReferenceRunnable {
	
	/** The reference holding the real impl */
	private final Reference<Object> real;
	/** The MBeanServer where the MBean was registered */
	private MBeanServer server;
	/** The ObjectName of the registered MBean */
	private ObjectName name;
	/** The proxy for the MBean */
	private Object proxy = null;
	/** The class name of the referent */
	private String rname;
	
	
	
	private MBeanProxy(ReferenceType refType, Object realObject) {
		this.real = ReferenceService.getInstance().newReference(refType, realObject, this);
		this.rname = realObject.getClass().getName();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
//		System.out.println(" ================> Unregister Task: Server [" + server + "], ObjectName: [" + name + "]");
		if(server!=null && name != null) {
			try {
				server.unregisterMBean(name);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceRunnable#getClearedRunnable()
	 */
	@Override
	public Runnable getClearedRunnable() {
		return this;
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
			MBeanProxy mbeanProxy = new MBeanProxy(refType, object);
			T proxy = mbeanInterface.cast(Proxy.newProxyInstance(
					mbeanInterface.getClassLoader(), new Class[] { mbeanInterface,
							MBeanRegistration.class }, mbeanProxy));
			mbeanProxy.proxy = proxy;
	
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
	
	/**
	 * Creates and returns an MBean proxy without registering it
	 * @param refType THe reference type
	 * @param mbeanInterface The MBean interface
	 * @param object The MBean impl
	 * @param proxyInterfaces Additional interfaces implemented by the proxy
	 * @return the MBean proxy which can be registered
	 */
	public static MBeanProxy proxyMBean(ReferenceType refType, Class<?> mbeanInterface, Object object, Class<?>... proxyInterfaces)  {
		MBeanProxy mbeanProxy = new MBeanProxy(refType, object);
		Class<?>[] proxyifaces = new Class[proxyInterfaces.length + 2];
		proxyifaces[0] = mbeanInterface;
		proxyifaces[1] = MBeanRegistration.class;
		for(int i = 0; i < proxyInterfaces.length; i++) {
			proxyifaces[i+2] = proxyInterfaces[i];
		}
//		StringBuilder b = new StringBuilder("\n\tMBean Ifaces:");
//		for(Class c: proxyifaces) {
//			b.append("\n\t").append(c.getName());
//		}
//		System.out.println(b.append("\n"));
		Object proxy = mbeanInterface.cast(Proxy.newProxyInstance(
				mbeanInterface.getClassLoader(), proxyifaces, mbeanProxy));
		mbeanProxy.proxy = proxy;
		return mbeanProxy;
	}

	/**
	 * Returns the JMX registerable proxy instance
	 * @return the JMX registerable proxy instance
	 */
	public Object getMBeanProxy() {
		return proxy;
	}
	
	/**
	 * Returns the reference to the real mbean impl which is being proxied
	 * @return the reference to the real mbean impl which is being proxied
	 */
	public Reference<?> getReference() {
		return real;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {		
		Object o = real.get();
		Object response = null;
		if (method.getDeclaringClass() == MBeanRegistration.class) {
			try {				
				response = method.invoke(this, args);
			} catch (InvocationTargetException e) {
				if (e.getCause() != null) throw e.getCause();
				throw e;
			}
			if(o instanceof MBeanRegistration) {
				try {
					method.invoke(o, args);
				} catch (InvocationTargetException e) {
					if (e.getCause() != null) throw e.getCause();
					throw e;
				}								
			}
			return response;			
		}

		if (o == null) {
			throw new InstanceNotFoundException(name + " no longer exists");
			//unregister();
//			throw new IllegalStateException(name + " no longer exists");
		}

		try {
			return method.invoke(o, args);
		} catch (InvocationTargetException e) {
			if (e.getCause() != null) throw e.getCause();
			throw e;
		}
	}

	private void unregister() {
		try {
			server.unregisterMBean(name);
			name = null;
		} catch (JMException e) {
			throw new Error(e); // is this even possible?
		}
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
		this.server = server;
		this.name = name;
		return name;
	}

	public void postRegister(Boolean registrationDone) {
		// noop
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	public void preDeregister() throws Exception {
		// noop
	}

	public void postDeregister() {
		server = null;
		name = null;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.ref.ReferenceRunnable#getName()
	 */
	@Override
	public String getName() {
		return rname;
	}

}