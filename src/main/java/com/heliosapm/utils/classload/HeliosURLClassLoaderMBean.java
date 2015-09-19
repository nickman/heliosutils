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
package com.heliosapm.utils.classload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;

import javax.management.ObjectName;

/**
 * <p>Title: HeliosURLClassLoaderMBean</p>
 * <p>Description: JMX MBean interface for {@link HeliosURLClassLoader}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.classload.HeliosURLClassLoaderMBean</code></p>
 */

public interface HeliosURLClassLoaderMBean {
	
	/** The object name prefix */
	public static final String OBJECT_NAME = "com.heliosapm.classpath:service=HeliosURLClassLoader,name=";

	
	/**
	 * Unloads this classloader, hopefully making it elligible for GC.
	 */
	public void unload();
	
	/**
	 * Returns this classloader's URLs
	 * @return this classloader's URLs
	 */
	public URL[] getURLs();
	
	/**
	 * Returns this classloader's name
	 * @return this classloader's name
	 */
	public String getName();
	
	
	/**
	 * Returns this classloader's parent's name
	 * @return this classloader's parent's name
	 */
	public String getParentName();
	
	/**
	 * Returns this classloader's JMX ObjectName
	 * @return this classloader's JMX ObjectName
	 */
	public ObjectName getObjectName();
	
	
	/**
	 * Adds the passed URLs to this classloader
	 * @param urls the URLs to add
	 */
	public void addURLs(final URL...urls);
	
	/**
	 * Adds the passed URLs to this classloader
	 * @param urls the URLs to add
	 */
	public void addURLs(final Collection<URL> urls);
	
	/**
	 * Loads the named class and returns it, returning null load fails
	 * @param name The name of the class to load
	 * @return the loaded class or null
	 */
	public Class<?> loadClassOrNull(String name);
	
	/**
	 * Adds the passed files as URLs to this classloader
	 * @param files the files to add
	 */
	public void addURLs(final File... files);
	
	/**
	 * Adds the passed files as URLs to this classloader
	 * @param urls the files to add
	 */
	public void addURLs(final String... urls);
	
	
	/**
	 * Returns the classloader id
	 * @return the classloader id
	 */
	public long getId();
	
  /**
   * Returns an input stream for reading the specified resource.
   * If this loader is closed, then any resources opened by this method
   * will be closed.
   *
   * <p> The search order is described in the documentation for {@link
   * #getResource(String)}.  </p>
   *
   * @param  name
   *         The resource name
   *
   * @return  An input stream for reading the resource, or <tt>null</tt>
   *          if the resource could not be found
   *
   * @since  1.7
   */
  public InputStream getResourceAsStream(String name);
  
  /**
   * Closes this URLClassLoader, so that it can no longer be used to load
   * new classes or resources that are defined by this loader.
   * Classes and resources defined by any of this loader's parents in the
   * delegation hierarchy are still accessible. Also, any classes or resources
   * that are already loaded, are still accessible.
   * <p>
   * In the case of jar: and file: URLs, it also closes any files
   * that were opened by it. If another thread is loading a
   * class when the {@code close} method is invoked, then the result of
   * that load is undefined.
   * <p>
   * The method makes a best effort attempt to close all opened files,
   * by catching {@link IOException}s internally. Unchecked exceptions
   * and errors are not caught. Calling close on an already closed
   * loader has no effect.
   * <p>
   * @throws IOException if closing any file opened by this class loader
   * resulted in an IOException. Any such exceptions are caught internally.
   * If only one is caught, then it is re-thrown. If more than one exception
   * is caught, then the second and following exceptions are added
   * as suppressed exceptions of the first one caught, which is then re-thrown.
   *
   * @throws SecurityException if a security manager is set, and it denies
   *   {@link RuntimePermission}<tt>("closeClassLoader")</tt>
   *
   * @since 1.7
   */
   public void close() throws IOException;
   
   /**
    * Finds the resource with the specified name on the URL search path.
    *
    * @param name the name of the resource
    * @return a <code>URL</code> for the resource, or <code>null</code>
    * if the resource could not be found, or if the loader is closed.
    */
   public URL findResource(final String name);
   
   /**
    * Returns an Enumeration of URLs representing all of the resources
    * on the URL search path having the specified name.
    *
    * @param name the resource name
    * @exception IOException if an I/O exception occurs
    * @return an <code>Enumeration</code> of <code>URL</code>s
    *         If the loader is closed, the Enumeration will be empty.
    */
   public Enumeration<URL> findResources(final String name) throws IOException;
   
   /**
    * Loads the class with the specified <a href="#name">binary name</a>.
    * This method searches for classes in the same manner as the {@link
    * #loadClass(String, boolean)} method.  It is invoked by the Java virtual
    * machine to resolve class references.  Invoking this method is equivalent
    * to invoking {@link #loadClass(String, boolean) <tt>loadClass(name,
    * false)</tt>}.  </p>
    *
    * @param  name
    *         The <a href="#name">binary name</a> of the class
    *
    * @return  The resulting <tt>Class</tt> object
    *
    * @throws  ClassNotFoundException
    *          If the class was not found
    */
   public Class<?> loadClass(String name) throws ClassNotFoundException;
   
   /**
    * Finds the resource with the given name.  A resource is some data
    * (images, audio, text, etc) that can be accessed by class code in a way
    * that is independent of the location of the code.
    *
    * <p> The name of a resource is a '<tt>/</tt>'-separated path name that
    * identifies the resource.
    *
    * <p> This method will first search the parent class loader for the
    * resource; if the parent is <tt>null</tt> the path of the class loader
    * built-in to the virtual machine is searched.  That failing, this method
    * will invoke {@link #findResource(String)} to find the resource.  </p>
    *
    * @param  name
    *         The resource name
    *
    * @return  A <tt>URL</tt> object for reading the resource, or
    *          <tt>null</tt> if the resource could not be found or the invoker
    *          doesn't have adequate  privileges to get the resource.
    *
    * @since  1.1
    */
   public URL getResource(String name);
   
   /**
    * Finds all the resources with the given name. A resource is some data
    * (images, audio, text, etc) that can be accessed by class code in a way
    * that is independent of the location of the code.
    *
    * <p>The name of a resource is a <tt>/</tt>-separated path name that
    * identifies the resource.
    *
    * <p> The search order is described in the documentation for {@link
    * #getResource(String)}.  </p>
    *
    * @param  name
    *         The resource name
    *
    * @return  An enumeration of {@link java.net.URL <tt>URL</tt>} objects for
    *          the resource.  If no resources could  be found, the enumeration
    *          will be empty.  Resources that the class loader doesn't have
    *          access to will not be in the enumeration.
    *
    * @throws  IOException
    *          If I/O errors occur
    *
    * @see  #findResources(String)
    *
    * @since  1.2
    */
   public Enumeration<URL> getResources(String name) throws IOException;
   
   /**
    * Returns the parent class loader for delegation. Some implementations may
    * use <tt>null</tt> to represent the bootstrap class loader. This method
    * will return <tt>null</tt> in such implementations if this class loader's
    * parent is the bootstrap class loader.
    *
    * <p> If a security manager is present, and the invoker's class loader is
    * not <tt>null</tt> and is not an ancestor of this class loader, then this
    * method invokes the security manager's {@link
    * SecurityManager#checkPermission(java.security.Permission)
    * <tt>checkPermission</tt>} method with a {@link
    * RuntimePermission#RuntimePermission(String)
    * <tt>RuntimePermission("getClassLoader")</tt>} permission to verify
    * access to the parent class loader is permitted.  If not, a
    * <tt>SecurityException</tt> will be thrown.  </p>
    *
    * @return  The parent <tt>ClassLoader</tt>
    *
    * @throws  SecurityException
    *          If a security manager exists and its <tt>checkPermission</tt>
    *          method doesn't allow access to this class loader's parent class
    *          loader.
    *
    * @since  1.2
    */
   public ClassLoader getParent();
   
   /**
    * Sets the default assertion status for this class loader.  This setting
    * determines whether classes loaded by this class loader and initialized
    * in the future will have assertions enabled or disabled by default.
    * This setting may be overridden on a per-package or per-class basis by
    * invoking {@link #setPackageAssertionStatus(String, boolean)} or {@link
    * #setClassAssertionStatus(String, boolean)}.  </p>
    *
    * @param  enabled
    *         <tt>true</tt> if classes loaded by this class loader will
    *         henceforth have assertions enabled by default, <tt>false</tt>
    *         if they will have assertions disabled by default.
    *
    * @since  1.4
    */
   public void setDefaultAssertionStatus(boolean enabled);
   
   /**
    * Sets the package default assertion status for the named package.  The
    * package default assertion status determines the assertion status for
    * classes initialized in the future that belong to the named package or
    * any of its "subpackages".
    *
    * <p> A subpackage of a package named p is any package whose name begins
    * with "<tt>p.</tt>".  For example, <tt>javax.swing.text</tt> is a
    * subpackage of <tt>javax.swing</tt>, and both <tt>java.util</tt> and
    * <tt>java.lang.reflect</tt> are subpackages of <tt>java</tt>.
    *
    * <p> In the event that multiple package defaults apply to a given class,
    * the package default pertaining to the most specific package takes
    * precedence over the others.  For example, if <tt>javax.lang</tt> and
    * <tt>javax.lang.reflect</tt> both have package defaults associated with
    * them, the latter package default applies to classes in
    * <tt>javax.lang.reflect</tt>.
    *
    * <p> Package defaults take precedence over the class loader's default
    * assertion status, and may be overridden on a per-class basis by invoking
    * {@link #setClassAssertionStatus(String, boolean)}.  </p>
    *
    * @param  packageName
    *         The name of the package whose package default assertion status
    *         is to be set. A <tt>null</tt> value indicates the unnamed
    *         package that is "current"
    *         (see section 7.4.2 of
    *         <cite>The Java&trade; Language Specification</cite>.)
    *
    * @param  enabled
    *         <tt>true</tt> if classes loaded by this classloader and
    *         belonging to the named package or any of its subpackages will
    *         have assertions enabled by default, <tt>false</tt> if they will
    *         have assertions disabled by default.
    *
    * @since  1.4
    */
   public void setPackageAssertionStatus(String packageName, boolean enabled);
   
   /**
    * Sets the desired assertion status for the named top-level class in this
    * class loader and any nested classes contained therein.  This setting
    * takes precedence over the class loader's default assertion status, and
    * over any applicable per-package default.  This method has no effect if
    * the named class has already been initialized.  (Once a class is
    * initialized, its assertion status cannot change.)
    *
    * <p> If the named class is not a top-level class, this invocation will
    * have no effect on the actual assertion status of any class. </p>
    *
    * @param  className
    *         The fully qualified class name of the top-level class whose
    *         assertion status is to be set.
    *
    * @param  enabled
    *         <tt>true</tt> if the named class is to have assertions
    *         enabled when (and if) it is initialized, <tt>false</tt> if the
    *         class is to have assertions disabled.
    *
    * @since  1.4
    */
   public void setClassAssertionStatus(String className, boolean enabled);
   
   /**
    * Sets the default assertion status for this class loader to
    * <tt>false</tt> and discards any package defaults or class assertion
    * status settings associated with the class loader.  This method is
    * provided so that class loaders can be made to ignore any command line or
    * persistent assertion status settings and "start with a clean slate."
    * </p>
    *
    * @since  1.4
    */
   public void clearAssertionStatus();
   
   
   
   
   

}
