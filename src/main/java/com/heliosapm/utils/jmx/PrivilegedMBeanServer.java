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

import javax.management.MBeanServer;
import javax.management.InstanceAlreadyExistsException;
import javax.management.loading.ClassLoaderRepository;
import javax.management.NotCompliantMBeanException;
import java.lang.Object;
import javax.management.IntrospectionException;
import java.lang.String;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.management.ObjectInstance;
import javax.management.InstanceNotFoundException;
import javax.management.OperationsException;
import javax.management.ObjectName;
import javax.management.MBeanInfo;
import javax.management.AttributeNotFoundException;
import javax.management.NotificationListener;
import java.util.Set;
import javax.management.QueryExp;
import java.lang.ClassLoader;
import javax.management.ReflectionException;
import java.lang.Integer;
import javax.management.Attribute;
import java.io.ObjectInputStream;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.NotificationFilter;
import javax.management.AttributeList;
import javax.management.MBeanRegistrationException;
import javax.management.InvalidAttributeValueException;


/**
 * <p>Title: PrivilegedMBeanServer</p>
 * <p>Description: Wraps an MBeanServer and executes all operations against it as a privileged action</p> 
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.PrivilegedMBeanServer</code></p>
 */

public class PrivilegedMBeanServer implements MBeanServer {
	/** The wrapped MBeanServer */
	private final MBeanServer server;
	
	
	/**
	 * Creates a new PrivilegedMBeanServer
	 * @param server The wrapped MBeanServer
	 */
	public PrivilegedMBeanServer(final MBeanServer server) {
		if(server==null) throw new IllegalArgumentException("The passed MBeanServer was null");
		this.server = server;
	}
	

	public Object invoke(final ObjectName on, final String str, final Object[] obj, final String[] str2) throws InstanceNotFoundException, MBeanException, ReflectionException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
				@Override
				public Object run() throws Exception {
					 return server.invoke(on, str, obj, str2);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public ClassLoader getClassLoader(final ObjectName on) throws InstanceNotFoundException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {
				@Override
				public ClassLoader run() throws Exception {
					 return server.getClassLoader(on);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public String getDefaultDomain()  {
		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			@Override
			public String run() {
				 return server.getDefaultDomain();
			}
		});
	}

	public boolean isRegistered(final ObjectName on)  {
		return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			@Override
			public Boolean run() {
				 return server.isRegistered(on);
			}
		});
	}

	public AttributeList getAttributes(final ObjectName on, final String[] str) throws InstanceNotFoundException, ReflectionException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<AttributeList>() {
				@Override
				public AttributeList run() throws Exception {
					 return server.getAttributes(on, str);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public Object getAttribute(final ObjectName on, final String str) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
				@Override
				public Object run() throws Exception {
					 return server.getAttribute(on, str);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public boolean isInstanceOf(final ObjectName on, final String str) throws InstanceNotFoundException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
				@Override
				public Boolean run() throws Exception {
					 return server.isInstanceOf(on, str);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public void setAttribute(final ObjectName on, final Attribute att) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		 try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
				@Override
				public Void run() throws Exception {
					 server.setAttribute(on, att);
					 return null;
				}
			});
			} catch (PrivilegedActionException e) {
				throw new RuntimeException("PrivilegedActionException", e);			
			}
	}

	public Object instantiate(final String str, final ObjectName on) throws ReflectionException, MBeanException, InstanceNotFoundException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
				@Override
				public Object run() throws Exception {
					 return server.instantiate(str, on);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public Object instantiate(final String str, final Object[] obj, final String[] str2) throws ReflectionException, MBeanException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
				@Override
				public Object run() throws Exception {
					 return server.instantiate(str, obj, str2);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public Object instantiate(final String str, final ObjectName on, final Object[] obj, final String[] str2) throws ReflectionException, MBeanException, InstanceNotFoundException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
				@Override
				public Object run() throws Exception {
					 return server.instantiate(str, on, obj, str2);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public Object instantiate(final String str) throws ReflectionException, MBeanException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
				@Override
				public Object run() throws Exception {
					 return server.instantiate(str);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public AttributeList setAttributes(final ObjectName on, final AttributeList al) throws InstanceNotFoundException, ReflectionException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<AttributeList>() {
				@Override
				public AttributeList run() throws Exception {
					 return server.setAttributes(on, al);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public void addNotificationListener(final ObjectName on, final NotificationListener nl, final NotificationFilter nf, final Object obj) throws InstanceNotFoundException {
		 try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
				@Override
				public Void run() throws Exception {
					 server.addNotificationListener(on, nl, nf, obj);
					 return null;
				}
			});
			} catch (PrivilegedActionException e) {
				throw new RuntimeException("PrivilegedActionException", e);			
			}
	}

	public void addNotificationListener(final ObjectName on, final ObjectName on2, final NotificationFilter nf, final Object obj) throws InstanceNotFoundException {
		 try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
				@Override
				public Void run() throws Exception {
					 server.addNotificationListener(on, on2, nf, obj);
					 return null;
				}
			});
			} catch (PrivilegedActionException e) {
				throw new RuntimeException("PrivilegedActionException", e);			
			}
	}

	public ObjectInstance createMBean(final String str, final ObjectName on, final ObjectName on2) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<ObjectInstance>() {
				@Override
				public ObjectInstance run() throws Exception {
					 return server.createMBean(str, on, on2);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public ObjectInstance createMBean(final String str, final ObjectName on, final Object[] obj, final String[] str2) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<ObjectInstance>() {
				@Override
				public ObjectInstance run() throws Exception {
					 return server.createMBean(str, on, obj, str2);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public ObjectInstance createMBean(final String str, final ObjectName on, final ObjectName on2, final Object[] obj, final String[] str2) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<ObjectInstance>() {
				@Override
				public ObjectInstance run() throws Exception {
					 return server.createMBean(str, on, on2, obj, str2);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public ObjectInstance createMBean(final String str, final ObjectName on) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<ObjectInstance>() {
				@Override
				public ObjectInstance run() throws Exception {
					 return server.createMBean(str, on);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public ObjectInputStream deserialize(final String str, final byte[] byt) throws OperationsException, ReflectionException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<ObjectInputStream>() {
				@SuppressWarnings("deprecation")
				@Override
				public ObjectInputStream run() throws Exception {
					 return server.deserialize(str, byt);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public ObjectInputStream deserialize(final String str, final ObjectName on, final byte[] byt) throws InstanceNotFoundException, OperationsException, ReflectionException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<ObjectInputStream>() {
				@SuppressWarnings("deprecation")
				@Override
				public ObjectInputStream run() throws Exception {
					 return server.deserialize(str, on, byt);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public ObjectInputStream deserialize(final ObjectName on, final byte[] byt) throws InstanceNotFoundException, OperationsException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<ObjectInputStream>() {
				@SuppressWarnings("deprecation")
				@Override
				public ObjectInputStream run() throws Exception {
					 return server.deserialize(on, byt);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public ClassLoader getClassLoaderFor(final ObjectName on) throws InstanceNotFoundException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {
				@Override
				public ClassLoader run() throws Exception {
					 return server.getClassLoaderFor(on);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public ClassLoaderRepository getClassLoaderRepository()  {
		return AccessController.doPrivileged(new PrivilegedAction<ClassLoaderRepository>() {
			@Override
			public ClassLoaderRepository run() {
				 return server.getClassLoaderRepository();
			}
		});
	}

	public String[] getDomains()  {
		return AccessController.doPrivileged(new PrivilegedAction<String[]>() {
			@Override
			public String[] run() {
				 return server.getDomains();
			}
		});
	}

	public Integer getMBeanCount()  {
		return AccessController.doPrivileged(new PrivilegedAction<Integer>() {
			@Override
			public Integer run() {
				 return server.getMBeanCount();
			}
		});
	}

	public MBeanInfo getMBeanInfo(final ObjectName on) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<MBeanInfo>() {
				@Override
				public MBeanInfo run() throws Exception {
					 return server.getMBeanInfo(on);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public ObjectInstance getObjectInstance(final ObjectName on) throws InstanceNotFoundException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<ObjectInstance>() {
				@Override
				public ObjectInstance run() throws Exception {
					 return server.getObjectInstance(on);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public Set<ObjectInstance> queryMBeans(final ObjectName on, final QueryExp qe)  {
		return AccessController.doPrivileged(new PrivilegedAction<Set<ObjectInstance>>() {
			@Override
			public Set<ObjectInstance> run() {
				 return server.queryMBeans(on, qe);
			}
		});
	}

	public Set<ObjectName> queryNames(final ObjectName on, final QueryExp qe)  {
		return AccessController.doPrivileged(new PrivilegedAction<Set<ObjectName>>() {
			@Override
			public Set<ObjectName> run() {
				 return server.queryNames(on, qe);
			}
		});
	}

	public ObjectInstance registerMBean(final Object obj, final ObjectName on) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<ObjectInstance>() {
				@Override
				public ObjectInstance run() throws Exception {
					 return server.registerMBean(obj, on);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public void removeNotificationListener(final ObjectName on, final ObjectName on2, final NotificationFilter nf, final Object obj) throws InstanceNotFoundException, ListenerNotFoundException {
		 try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
				@Override
				public Void run() throws Exception {
					 server.removeNotificationListener(on, on2, nf, obj);
					 return null;
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public void removeNotificationListener(final ObjectName on, final ObjectName on2) throws InstanceNotFoundException, ListenerNotFoundException {
		 try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
				@Override
				public Void run() throws Exception {
					 server.removeNotificationListener(on, on2);
					 return null;
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public void removeNotificationListener(final ObjectName on, final NotificationListener nl) throws InstanceNotFoundException, ListenerNotFoundException {
		 try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
				@Override
				public Void run() throws Exception {
					 server.removeNotificationListener(on, nl);
					 return null;
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public void removeNotificationListener(final ObjectName on, final NotificationListener nl, final NotificationFilter nf, final Object obj) throws InstanceNotFoundException, ListenerNotFoundException {
		 try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
				@Override
				public Void run() throws Exception {
					 server.removeNotificationListener(on, nl, nf, obj);
					 return null;
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	}

	public void unregisterMBean(final ObjectName on) throws InstanceNotFoundException, MBeanRegistrationException {
		 try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
				@Override
				public Void run() throws Exception {
					 server.unregisterMBean(on);
					 return null;
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("PrivilegedActionException", e);			
		}
	 }
	

}
