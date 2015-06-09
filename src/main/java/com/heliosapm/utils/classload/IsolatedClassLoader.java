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

import java.net.URL;
import java.net.URLClassLoader;

import javax.management.ObjectName;


/**
 * <p>Title: IsolatedClassLoader</p>
 * <p>Description: A parent last isolated classloader</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author <a href="http://stackoverflow.com/users/209856/karoberts">karoberts</a> on StackOverflow
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.classload.IsolatedClassLoader</code></p>
 */

public class IsolatedClassLoader extends ClassLoader {
	/** The child class loader */
	protected final ChildURLClassLoader childClassLoader;
	
	/**
	 * Creates a new IsolatedClassLoader
	 * @param objectName The JMX ObjectName to register the management interface with.
	 * Ignored if null.
	 * @param urls The classpath the loader will load from
	 */
	public IsolatedClassLoader(final ObjectName objectName, final URL[] urls) {
    super(Thread.currentThread().getContextClassLoader());
    childClassLoader = new ChildURLClassLoader( urls, new FindClassClassLoader(this.getParent()) );
	}
	
  /**
   * {@inheritDoc}
   * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
   */
  @Override
  protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
      try {
          // first we try to find a class inside the child classloader
          return childClassLoader.findClass(name);
      } catch( ClassNotFoundException e ) {
          // didn't find it, try the parent
          return super.loadClass(name, resolve);
      }
  }
	

  /**
   * This class delegates (child then parent) for the findClass method for a URLClassLoader.
   * We need this because findClass is protected in URLClassLoader
   */
  private static class ChildURLClassLoader extends URLClassLoader {
      private FindClassClassLoader realParent;

      public ChildURLClassLoader( URL[] urls, FindClassClassLoader realParent ) {
          super(urls, null);

          this.realParent = realParent;
      }

      @Override
      public Class<?> findClass(String name) throws ClassNotFoundException {
      	Class<?> loaded = super.findLoadedClass(name);
        if( loaded != null ) return loaded;	        	
          try {
              // first try to use the URLClassLoader findClass
              return super.findClass(name);
          }  catch( ClassNotFoundException e ) {
              // if that fails, we ask our real parent classloader to load the class (we give up)
              return realParent.loadClass(name);
          }
      }
  }
  
  /**
   * This class allows me to call findClass on a classloader
   */
  private static class FindClassClassLoader extends ClassLoader {
      public FindClassClassLoader(ClassLoader parent) {
          super(parent);
      }

      @Override
      public Class<?> findClass(String name) throws ClassNotFoundException {
          return super.findClass(name);
      }
  }
  

}
