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
package com.heliosapm.utils.instrumentation;


import javax.management.ObjectName;

import com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller;
import com.heliosapm.utils.jmx.JMXHelper;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.UnmodifiableClassException;
import java.util.jar.JarFile;


/**
 * <p>Title: Instrumentation</p>
 * <p>Description: A wrapper for a {@link java.lang.instrument.Instrumentation} instance</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.instrumentation.Instrumentation</code></p>
 */

public class Instrumentation implements InstrumentationMBean {

	/** The delegate instrumentation */
	protected final java.lang.instrument.Instrumentation delegate;
	/** The singleton instance */
	private static volatile Instrumentation instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	
	/**
	 * Installs the MBean
	 * @param delegate The actual instrumentation instance
	 * @return the singleton instance
	 */
	public static Instrumentation install(final java.lang.instrument.Instrumentation delegate) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					if(delegate==null) {
						try {
							java.lang.instrument.Instrumentation _delegate = LocalAgentInstaller.getInstrumentation();
							instance = new Instrumentation(_delegate);
						} catch (Exception ex) {
							throw new RuntimeException("Passed instrumentation was null and local agent installer failed: " + ex);
						}
					} else {
						instance = new Instrumentation(delegate);
					}
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new Instrumentation
	 * @param delegate The delegate instrumentation
	 */
	private Instrumentation(final java.lang.instrument.Instrumentation delegate) {
		if(delegate==null) throw new IllegalArgumentException("The passed java.lang.instrument.Instrumentation instance was null");
		this.delegate = delegate;
		if(JMXHelper.isRegistered(OBJECT_NAME)) {
			JMXHelper.unregisterMBean(OBJECT_NAME);
		}
		JMXHelper.registerMBean(OBJECT_NAME, this);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#getInstance()
	 */
	@Override
	public java.lang.instrument.Instrumentation getInstance() {
		return delegate;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#addTransformer(java.lang.instrument.ClassFileTransformer, boolean)
	 */
	@Override
	public void addTransformer(final ClassFileTransformer transformer,
			boolean canRetransform) {
		delegate.addTransformer(transformer, canRetransform);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#addTransformer(java.lang.instrument.ClassFileTransformer)
	 */
	@Override
	public void addTransformer(final ClassFileTransformer transformer) {
		delegate.addTransformer(transformer);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#removeTransformer(java.lang.instrument.ClassFileTransformer)
	 */
	@Override
	public boolean removeTransformer(final ClassFileTransformer transformer) {
		return delegate.removeTransformer(transformer);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#isRetransformClassesSupported()
	 */
	@Override
	public boolean isRetransformClassesSupported() {
		return delegate.isRetransformClassesSupported();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#retransformClasses(java.lang.Class[])
	 */
	@Override
	public void retransformClasses(final Class<?>... classes)
			throws UnmodifiableClassException {
		delegate.retransformClasses(classes);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#isRedefineClassesSupported()
	 */
	@Override
	public boolean isRedefineClassesSupported() {
		return delegate.isRedefineClassesSupported();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#redefineClasses(java.lang.instrument.ClassDefinition[])
	 */
	@Override
	public void redefineClasses(final ClassDefinition... definitions)
			throws ClassNotFoundException, UnmodifiableClassException {
		delegate.redefineClasses(definitions);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#isModifiableClass(java.lang.Class)
	 */
	@Override
	public boolean isModifiableClass(final Class<?> theClass) {
		return delegate.isModifiableClass(theClass);
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#getInitiatedClasses(java.lang.ClassLoader)
	 */
	@Override
	public Class<?>[] getInitiatedClasses(final ClassLoader loader) {
		return delegate.getInitiatedClasses(loader);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#getObjectSize(java.lang.Object)
	 */
	@Override
	public long getObjectSize(final Object objectToSize) {
		return delegate.getObjectSize(objectToSize);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#appendToBootstrapClassLoaderSearch(java.util.jar.JarFile)
	 */
	@Override
	public void appendToBootstrapClassLoaderSearch(final JarFile jarfile) {
		delegate.appendToBootstrapClassLoaderSearch(jarfile);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#appendToSystemClassLoaderSearch(java.util.jar.JarFile)
	 */
	@Override
	public void appendToSystemClassLoaderSearch(final JarFile jarfile) {
		delegate.appendToSystemClassLoaderSearch(jarfile);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#isNativeMethodPrefixSupported()
	 */
	@Override
	public boolean isNativeMethodPrefixSupported() {
		return delegate.isNativeMethodPrefixSupported();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.instrumentation.InstrumentationMBean#setNativeMethodPrefix(java.lang.instrument.ClassFileTransformer, java.lang.String)
	 */
	@Override
	public void setNativeMethodPrefix(final ClassFileTransformer transformer, final String prefix) {
		delegate.setNativeMethodPrefix(transformer, prefix);
	}

	/**
	 * Returns an array of all the loaded classes in the JVM
	 * @return an array of all the loaded classes in the JVM
	 */
	public Class<?>[] getAllLoadedClasses() {
		return new Class[]{};
	}

}
