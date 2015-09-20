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
import java.lang.instrument.Instrumentation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import jsr166e.LongAdder;

import com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller;
import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.ref.MBeanProxy;
import com.heliosapm.utils.ref.ReferenceService;
import com.heliosapm.utils.ref.ReferenceService.ReferenceType;
import com.heliosapm.utils.reflect.PrivateAccessor;

/**
 * <p>Title: HeliosURLClassLoader</p>
 * <p>Description: A {@link URLClassLoader} with a little extra pizaz.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.classload.HeliosURLClassLoader</code></p>
 */

public class HeliosURLClassLoader extends URLClassLoader implements HeliosURLClassLoaderMBean {
	/** The unique URL checker */
	protected final Set<URL> urls = new CopyOnWriteArraySet<URL>();
	/** The class loader name */
	protected final String name;
	/** The class loader serial number */
	protected final long id;
	/** The class loader's JMX ObjectName */
	protected ObjectName objectName;
	/** The instrumentation instance */
	protected static final Instrumentation instr;
	
	
	/** The number of cleared references */
	protected static final LongAdder cleared = new LongAdder();
	/** Classloader serial number */
	protected static final AtomicLong serial = new AtomicLong(0L);
	/** All helios classloaders keyed by the name */
	protected static final ConcurrentHashMap<String, WeakReference<HeliosURLClassLoader>> loaders = new ConcurrentHashMap<String, WeakReference<HeliosURLClassLoader>>(); 
	
	static {
		HeliosURLClassLoaderService.getInstance();
		Instrumentation i = null;
		try {
			i = LocalAgentInstaller.getInstrumentation();
		} catch (Throwable t) {
			i = null;
		}
		instr = i;
	}
	
	/**
	 * Creates a new HeliosURLClassLoader
	 * @param name The optional class loader name
	 * @param urls The URLs to initialize the class loader with
	 */
	@SuppressWarnings("unchecked")
	public HeliosURLClassLoader(final String name, final URL...urls) {
		super(unique(URL.class, urls));
		id = serial.incrementAndGet();		
		this.name = (name==null || name.trim().isEmpty()) ? ("HeliosURLClassLoader#" + id) : name.trim();		
		ref(this);
	}
	
	
	private static void ref(final HeliosURLClassLoader loader) {
		final String key = loader.getName();
		if(!loaders.containsKey(key)) {
			synchronized(loaders) {
				if(!loaders.containsKey(key)) {
					final String name = loader.getName();
					final ObjectName loaderObjectName = JMXHelper.objectName(OBJECT_NAME + loader.name);
					loader.objectName = loaderObjectName;
					MBeanProxy.register(ReferenceType.WEAK, loaderObjectName, HeliosURLClassLoaderMBean.class, loader);
					loaders.put(key, ReferenceService.getInstance().newWeakReference(loader, new Runnable(){
						@Override
						public void run() {
							try { JMXHelper.unregisterMBean(loaderObjectName); } catch (Exception ex) {/* No Op */}
							loaders.remove(name);
							cleared.increment();							
							System.err.println("HeliosURLClassLoader[" + name + "] was garbage collected");
						}
					})); 
							//new HeliosURLClassLoaderWeakReference(loader.getName(), loader));
					return;
				}
			}
		}
		throw new IllegalArgumentException("The name [" + key + "] is already registered");
	}
	
	
	/**
	 * Unloads this classloader, hopefully making it elligible for GC.
	 */
	@Override
	public void unload() {
		final WeakReference<HeliosURLClassLoader> ref = loaders.remove(name);
		urls.clear();
		if(ref!=null) {
			ref.enqueue();
		}
	}
	
	/**
	 * Unloads the named classloader
	 * @param name the name of the classloader to unload
	 * @see HeliosURLClassLoader#unload()
	 */
	public static void unload(final String name) {
		final WeakReference<HeliosURLClassLoader> ref = loaders.remove(name.trim());
		if(ref!=null) {
			final HeliosURLClassLoader cl = ref.get();
			if(cl!=null) {
				cl.unload();
			}
		}
	}
	
  @Override
	public URL[] getURLs() {
    URL[] urls = super.getURLs();
    if(urls.length==0) {
    	ClassLoader p = getParent();
    	if(p!=null && (p instanceof URLClassLoader)) {
    		urls = ((URLClassLoader)p).getURLs();
    	}
    }
    return urls;
  }
	
	private static final <T> T[] unique(final Class<T> type, final T...items) {
		final Set<T> set = new HashSet<T>();
		Collections.addAll(set, items);
		if(set.isEmpty()) return (T[])Array.newInstance(type, 0);
		return set.toArray((T[])Array.newInstance(type, set.size()));
	}

	/**
	 * Creates a new HeliosURLClassLoader
	 * @param name 
	 * @param parent
	 * @param urls
	 */
	@SuppressWarnings("unchecked")
	public HeliosURLClassLoader(final String name, final ClassLoader parent, final URL...urls) {
		super(unique(URL.class, urls), parent);
		id = serial.incrementAndGet();
		this.name = (name==null || name.trim().isEmpty()) ? ("HeliosURLClassLoader#" + id) : name.trim();
		ref(this);
	}

	/**
	 * Creates a new HeliosURLClassLoader
	 * @param name 
	 * @param parent
	 * @param factory
	 * @param urls
	 */
	public HeliosURLClassLoader(final String name, final ClassLoader parent, final URLStreamHandlerFactory factory, final URL... urls) {
		super(unique(URL.class, urls), parent, factory);
		id = serial.incrementAndGet();
		this.name = (name==null || name.trim().isEmpty()) ? ("HeliosURLClassLoader#" + id) : name.trim();		
		ref(this);
	}
	
	/**
	 * Returns the named classloader
	 * @param name The name of the classloader
	 * @return the named classloader or null if not found.
	 */
	public static HeliosURLClassLoader getLoader(final String name) {
		final WeakReference<HeliosURLClassLoader> rf = loaders.get(name.trim());
		if(rf!=null) {
			final HeliosURLClassLoader loader = rf.get();
			if(loader!=null) return loader;
		}
		return null;
	}
	
	/**
	 * Returns the named classloader, creating it if one with the same name is not found
	 * @param name The name of the classloader
	 * @param parent The optional parent classloader. If null, will use {@link ClassLoader#getSystemClassLoader()}
	 * @param factory  The optional URLStreamHandlerFactory for handling custom URL types
	 * @param urls The urls to add to the class loader
	 * @return the named classloader or null if not found.
	 */
	public static HeliosURLClassLoader getOrCreateLoader(final String name, final ClassLoader parent, final URLStreamHandlerFactory factory, final URL... urls) {
		final String key = name.trim();
		HeliosURLClassLoader loader = getLoader(key);
		if(loader==null) {
			synchronized(loaders) {
				loader = getLoader(key);
				if(loader==null) {
					final ClassLoader p = parent==null ? ClassLoader.getSystemClassLoader() : parent;
					final URL[] xurls = urls==null ? new URL[0] : urls;
					if(factory==null) {
						loader = new HeliosURLClassLoader(key, p, xurls);
					} else {
						loader = new HeliosURLClassLoader(key, p, factory, xurls);
					}
				}
			}
		}
		loader.addURLs(urls);
		return loader;
	}
	
	/**
	 * Returns the named classloader, creating it if one with the same name is not found
	 * @param name The name of the classloader
	 * @param parent The optional parent classloader. If null, will use {@link ClassLoader#getSystemClassLoader()}
	 * @param urls The urls to add to the class loader
	 * @return the named classloader or null if not found.
	 */
	public static HeliosURLClassLoader getOrCreateLoader(final String name, final ClassLoader parent, final URL... urls) {
		return getOrCreateLoader(name, parent, null, urls);
	}
	
	/**
	 * Returns the named classloader, creating it if one with the same name is not found
	 * @param name The name of the classloader
	 * @param urls The urls to add to the class loader
	 * @return the named classloader or null if not found.
	 */
	public static HeliosURLClassLoader getOrCreateLoader(final String name, final URL... urls) {
		return getOrCreateLoader(name, null, null, urls);
	}
	
	public int getClassCount() {
		if(instr==null) return -1;		
		return instr.getInitiatedClasses(this).length;
	}
	public int getParentClassCount() {
		if(instr==null || getParent()==null) return -1;		
		return instr.getInitiatedClasses(getParent()).length;
	}
	public String[] printLoadedClasses() {
		if(instr==null) return null;
		final Class<?>[] clazzes = instr.getInitiatedClasses(this);
		final String[] names = new String[clazzes.length];
		for(int i = 0; i < clazzes.length; i++) {
			names[i] = clazzes[i].getName();
		}
		return names;
	}
	
	/**
	 * Returns this classloader's name
	 * @return this classloader's name
	 */
	@Override
	public String getName() {
		return name;
	}
	
	/**
	 * Adds the passed URLs to this classloader
	 * @param urls the URLs to add
	 */
	@Override
	public void addURLs(final URL...urls) {
		for(URL url: urls) {
			if(this.urls.add(url)) {
				addURL(url);
			}
		}		
	}
	
	/**
	 * Adds the passed URLs to this classloader
	 * @param urls the URLs to add
	 */
	@Override
	public void addURLs(final Collection<URL> urls) {
		for(URL url: urls) {
			if(url==null) continue;
			if(this.urls.add(url)) {
				addURL(url);
			}
		}		
	}
	
	@Override
	public Class<?> loadClassOrNull(String name)  {
		try {
			return super.loadClass(name);
		} catch (Throwable t) {
			return null;
		}
	}
	
	/**
	 * Adds the passed files as URLs to this classloader
	 * @param files the files to add
	 */
	@Override
	public void addURLs(final File... files) {
		for(File file: files) {
			if(file.canRead()) {
				final URL url = toURL(file);
				if(url==null) continue;
				if(this.urls.add(url)) {
					addURL(url);
				}
			}
		}		
	}
	
	/**
	 * Adds the passed files as URLs to this classloader
	 * @param urls the files to add
	 */
	@Override
	public void addURLs(final String... urls) {
		for(String url: urls) {
			if(url==null || url.trim().isEmpty()) continue;
			if(url.indexOf(',')!=0) {
				addURLs(url.split(","));
				return;
			}
			URL u = toURL(url.trim());
			if(u==null) continue;
			if(this.urls.add(u)) {
				addURL(u);
			}
		}		
	}
	
	@Override
	public String toString() {		
		return "HeliosURLClassLoader[" + name + "]";
	}
	
	/**
	 * Returns the URL for the passed file
	 * @param file the file to get the URL for
	 * @return a URL for the passed file
	 */
	private static URL toURL(File file) {
		try {
			return file.toURI().toURL();
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Determines if the passed stringy represents an existing file name
	 * @param urlStr The stringy to test
	 * @return true if the passed stringy represents an existing file name, false otherwise
	 */
	private static boolean isFile(final CharSequence urlStr) {
		if(urlStr==null || urlStr.toString().trim().isEmpty()) throw new IllegalArgumentException("The passed URL stringy was null or empty");
		return new File(urlStr.toString().trim()).exists();
	}
	
	
	/**
	 * Creates a URL from the passed string 
	 * @param urlStr A char sequence containing a URL representation
	 * @return a URL
	 */
	private static URL toURL(final CharSequence urlStr) {
		if(urlStr==null || urlStr.toString().trim().isEmpty()) throw new IllegalArgumentException("The passed URL stringy was null or empty");
		try {
			if(isFile(urlStr)) {
//				System.out.println("URL from File (" + urlStr + "): [" + new File(urlStr.toString()).getAbsoluteFile() + "]");
				return toURL(new File(urlStr.toString()).getAbsoluteFile());
			}
//			System.err.println("NOT A File (" + urlStr + "): [" + new File(urlStr.toString()).getAbsoluteFile() + "]");
			return new URL(urlStr.toString());
		} catch (Exception e) {
			return null;
		}
	}
	

	
	
	/**
	 * Returns the classloader id
	 * @return the classloader id
	 */
	@Override
	public long getId() {
		return id;
	}
	
	/**
	 * Returns all known classloader URLs
	 * @return all known classloader URLs
	 */
	public static URL[] getAllURLs() {
		final Set<URL> urls = new HashSet<URL>();
		for(String key: loaders.keySet()) {
			HeliosURLClassLoader hcl = getLoader(key);
			if(hcl!=null) {
				Collections.addAll(urls, hcl.getURLs());
			}
		}
		return urls.toArray(new URL[urls.size()]);
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.classload.HeliosURLClassLoaderMBean#getParentName()
	 */
	@Override
	public String getParentName() {
		final ClassLoader c = getParent();
		if(c==null) return null;
		if(c instanceof HeliosURLClassLoader) return ((HeliosURLClassLoader)c).getName();
		return c.toString();
	}



	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.classload.HeliosURLClassLoaderMBean#getObjectName()
	 */
	@Override
	public ObjectName getObjectName() {
		return objectName;
	}

}
