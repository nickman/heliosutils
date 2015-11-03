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
package com.heliosapm.utils.jar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.heliosapm.utils.file.FileFilterBuilder;
import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: JarBuilder</p>
 * <p>Description: Flient style on-the-fly jar builder</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jar.JarBuilder</code></p>
 */

public class JarBuilder {
	/** The file we will write the jar to */
	final File jarFile;
	/** The content specifiers */
	final Set<ResourceFilter> specifiers = new LinkedHashSet<ResourceFilter>();
	/** The resources that will be written to the jar */
	final Map<URL, String> foundResources = new HashMap<URL, String>();
	/** The resource names to track uniqueness */
	final Set<String> resourceNames = new HashSet<String>();
	/** Duplicate resources  */
	final Set<URL> pathConflicts = new HashSet<URL>();
	/** A set of resource mergers in the order in which they should be executed */
	final Set<ResourceMerger> mergers = new LinkedHashSet<ResourceMerger>();
	
	
	/** The manifest */
	Manifest manifest = null;

	/** Static class logger */
	private final static Logger log = Logger.getLogger(JarBuilder.class.getName()); 

	/**
	 * Creates a new JarBuilder to write a jar to the passed file, overwriting if it already exists
	 * @param jarFile the file tha archive will be written to
	 */
	public JarBuilder(final File jarFile) {
		this(jarFile, true);
	}

	/**
	 * Creates a new JarBuilder to write a jar to to a temp file
	 */
	public JarBuilder() {
		this(tmpFile(), true);
	}
	
	private static File tmpFile() {
		try {
			return File.createTempFile("heliosapm", ".jar");
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create temp file", ex);
		}
	}
	
	/**
	 * Returns a new manifest builder
	 * @return a new manifest builder
	 */
	public ManifestBuilder manifestBuilder() {
		return new ManifestBuilder(this);
	}
	
	/**
	 * Sets the manifest
	 * @param manifest a manifest for this jar
	 * @return this jar builder
	 */
	public JarBuilder setMainifest(final Manifest manifest) {
		if(manifest==null) throw new IllegalArgumentException("The passed Manifest was null");
		this.manifest = manifest;
		return this;
	}
	
	/**
	 * Adds a resource merger
	 * @param merger the merger to add
	 * @return this jar builder
	 */
	public JarBuilder addResourceMerger(final ResourceMerger merger) {
		if(merger==null) throw new IllegalArgumentException("The passed ResourceMerger was null");
		mergers.add(merger);
		return this;
	}
	
	/**
	 * Creates a new JarBuilder to write a jar to the passed file
	 * @param jarFile the file tha archive will be written to
	 * @param overwrite true to overwrite an existing file, false for an exception if the file exists
	 */
	public JarBuilder(final File jarFile, final boolean overwrite) {
		if(jarFile==null) throw new IllegalArgumentException("The passed file was null");
		if(jarFile.isDirectory()) throw new IllegalArgumentException("The passed file [" + jarFile + "] is a directory");
		if(jarFile.exists()) {
			if(overwrite) {
				final boolean deleted = jarFile.delete();
				if(!deleted) throw new RuntimeException("Cannot overwrite file [" + jarFile + "]. Failed to delete.");
			} else {
				throw new RuntimeException("File [" + jarFile + "] already exists and overwrite was false");
			}
		}
		this.jarFile = jarFile;
		
	}
	
	protected void queueResource(final URL url, final String name) {
		if(!resourceNames.add(name)) {
			pathConflicts.add(url);
			log.warning("Duplicate Resource Conflict: [" + url + "]");
		} else {
			foundResources.put(url, name);
		}
	}

	
	public static void main(String[] args) {
		
		File f = new JarBuilder(new File("/tmp/test.jar"), true)
//			.res("").classLoader(ClassLoader.getSystemClassLoader().getParent()).apply()
			.res("META-INF/services").classLoader(ClassLoader.getSystemClassLoader().getParent()).apply()
			.res("META-INF/services").classLoader(ClassLoader.getSystemClassLoader()).apply()
			.res("META-INF/services").apply()			
			.addResourceMerger(new ServiceDefinitionMerger())
//			.res("javax.management.remote").classLoader(JMXMPConnector.class).apply()
//			.res("org.json").classLoader(JSONObject.class).apply()
			.build();
		System.out.println(f);
	}
	
	public File build() {
		for(ResourceFilter rs: this.specifiers) {
			rs.find();
		}
		FileOutputStream fos = null;
		JarOutputStream jos = null;
		try {
			fos = new FileOutputStream(jarFile, false);
			if(manifest==null) {
				manifest = new ManifestBuilder(this).autoCreatedBy().build();
			}
			jos = new JarOutputStream(fos, manifest);
			jos.setLevel(9);
			
			final byte[] buf = new byte[8192];
			int bytesRead = 0;
			for(Map.Entry<URL, String> entry: foundResources.entrySet()) {
				JarEntry je = new JarEntry(entry.getValue());
				jos.putNextEntry(je);
				bytesRead = 0;
				InputStream is = null;
				try {
					is = entry.getKey().openStream();
					while((bytesRead=is.read(buf))!=-1) {
						jos.write(buf, 0, bytesRead);
					}
					jos.flush();
					jos.closeEntry();
					if(log.isLoggable(Level.FINE)) log.log(Level.FINE, "Wrote entry [" + entry.getValue() + "]"); 					
				} catch (Exception ex) {
					throw new RuntimeException("Failed to write entry [" + entry.getValue() + "]", ex);
				} finally {
					if(is!=null) try { is.close(); } catch (Exception x) {/* No Op */}
				}
				//jos.putNextEntry(new ZipEntry(clazz.getName().replace('.', '/') + ".class"));
			}
			jos.flush();
			for(ResourceMerger rm: mergers) {
				rm.writeMerged(jos);
			}
			jos.close();
			fos.flush();
			fos.close();
			if(log.isLoggable(Level.FINE)) log.fine(String.format("Jar Complete: [%s], Size: [%s] bytes", jarFile.getAbsolutePath(), jarFile.length()));
			return jarFile;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to write Jar [" + jarFile + "]", e);
			throw new RuntimeException("Failed to write Jar [" + jarFile + "]", e);
		} finally {
			if(fos!=null) try { fos.close(); } catch (Exception e) {}
		}
		
	}
	
	public static String binToRes(final String path) {
		if(path==null) throw new IllegalArgumentException("The passed path was null");
		return path.trim().replace('.', '/');
	}
	
	public static String binToPath(final String path) {
		if(path==null || path.trim().isEmpty()) throw new IllegalArgumentException("The passed path was null");
		return path.trim().replace('.', File.separatorChar);
	}
	
	
	public ResourceFilter res(final String base) {
		return new ResourceFilter(base);
	}
	
	/**
	 * <p>Title: ResourceFilter</p>
	 * <p>Description: Finds an extracts all matching resources in the configured paths.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.jar.JarBuilder.ResourceFilter</code></p>
	 */
	public class ResourceFilter {
		final String base;
		ClassLoader classLoader = null;
		boolean recurse = true;
		final Set<Pattern> patterns = new LinkedHashSet<Pattern>();

		public ResourceFilter(final String base) {
			if(base==null) throw new IllegalArgumentException("The passed base was null");
			this.base = base.trim();
		}
		
		public ResourceFilter classLoader(final Class<?> clazz) {
			if(clazz==null) throw new IllegalArgumentException("The passed class was null");
			final ClassLoader cl = clazz.getClassLoader();
			if(cl==null) throw new IllegalArgumentException("The class [" + clazz.getName() + "] has a primordial classloader");
			this.classLoader = cl;
			return this;
		}
		
		public ResourceFilter classLoader(final ClassLoader classLoader) {
			if(classLoader==null) throw new IllegalArgumentException("The passed class loader was null");
			this.classLoader = classLoader;
			return this;
		}
		
		
		public ResourceFilter recurse(final boolean recurse) {
			this.recurse = recurse;
			return this;
		}
		
 		public ResourceFilter filterPath(final boolean caseSensitive, final String...patterns) {
			for(String p: patterns) {
				this.patterns.add(Pattern.compile(p, caseSensitive ? Pattern.CASE_INSENSITIVE : 0));
			}
			return this;
		}
		
		public JarBuilder apply() {
			specifiers.add(this);
			return JarBuilder.this;
		}
		
		public void find() {
			final String path = binToRes(base);
			final ClassLoader cl = classLoader==null ? Thread.currentThread().getContextClassLoader() : classLoader;
			if(cl instanceof URLClassLoader) {
				URL[] urls = ((URLClassLoader)cl).getURLs();
				for(URL url: urls) {	
					final String protocol = url.getProtocol();
					final String fileName = url.getFile().toLowerCase(); 
					if(fileName.endsWith("/")) {						
						scanDirectory(url, path);
					} else  {
						scanJar(url, path);
					}
				}
			}
			
			
		}
		
		void scanDirectory(final URL url, final String path) {	
			final boolean hasMergers;
			if(!mergers.isEmpty()) {
				boolean acceptDirs = false;
				for(ResourceMerger rm: mergers) {
					if(rm.worksOnFileScans()) {
						acceptDirs = true;
					}
				}
				hasMergers = acceptDirs;
			} else {
				hasMergers = false;
			}
			
			final File f = new File(url.getFile());
			if(f.isDirectory()) {
				final String prefix;
				if(path.isEmpty()) {
					prefix = f.getAbsolutePath() + File.separator;
				} else {
					prefix = f.getAbsolutePath() + File.separator + path + File.separator;
				}
				final String strip = url.toString();
				File dir = new File(prefix);
				if(dir.isDirectory()) {
					final File[] fp = FileFilterBuilder.newBuilder()
						.caseInsensitive(true)						
//						.containsMatch(path)
//						.shouldBeFile()						
						.fileFinder()
						.maxDepth(recurse ? Integer.MAX_VALUE : 1)
						.addSearchDir(dir)
						.find();
					for(File fo: fp) {
						final String entryName = URLHelper.toURL(fo).toString().replace(strip, "");
						if(hasMergers) {
							boolean queue = true;
							for(ResourceMerger rm: mergers) {
								final boolean isDir = fo.isDirectory();
								if((isDir && !rm.worksOnDirs()) || (!isDir && !rm.worksOnFiles())) continue; 
								if(!rm.inspect(URLHelper.toURL(fo), entryName, fo.isFile())) {
									queue = false;
									break;
								}
							}
							if(queue) queueResource(URLHelper.toURL(fo), entryName);
						} else {
							queueResource(URLHelper.toURL(fo), entryName);
						}
					}					
				}
			}
		}
		
		void scanJar(final URL url, final String path) {	
			final boolean hasMergers;
			if(!mergers.isEmpty()) {
				boolean acceptJars = false;
				for(ResourceMerger rm: mergers) {
					if(rm.worksOnUrlScans()) {
						acceptJars = true;
					}
				}
				hasMergers = acceptJars;
			} else {
				hasMergers = false;
			}
			
			InputStream is = null;
			JarInputStream jis = null;
			try {
				is = url.openStream();
				jis = new JarInputStream(is);
				JarEntry jarEntry = null;
				while((jarEntry = jis.getNextJarEntry())!=null) {					
					final String name = jarEntry.getName();
					if(name.indexOf(path)==0) {
						final URL resourceUrl = URLHelper.toURL("jar:" + url + "!/" + name);
						if(hasMergers) {
							boolean queue = true;
							for(ResourceMerger rm: mergers) {
								if(!rm.inspect(resourceUrl, name, !jarEntry.isDirectory())) {
									queue = false;
									break;
								}
							}
							if(queue) queueResource(resourceUrl, name);
						} else {
							queueResource(resourceUrl, name);
						}
					}					
				}
			} catch (Exception ex) {
				throw new RuntimeException("Failed to scan Jar [" + url + "]", ex);
			} finally {				
				if(jis!=null) try { jis.close(); } catch (Exception x) {/* No Op */}
				if(is!=null) try { is.close(); } catch (Exception x) {/* No Op */}
			}
		}
		

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((base == null) ? 0 : base.hashCode());
			result = prime * result + ((classLoader == null) ? 0 : classLoader.hashCode());
			result = prime * result + ((patterns == null) ? 0 : patterns.hashCode());
			result = prime * result + (recurse ? 1231 : 1237);
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ResourceFilter other = (ResourceFilter) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (base == null) {
				if (other.base != null)
					return false;
			} else if (!base.equals(other.base))
				return false;
			if (classLoader == null) {
				if (other.classLoader != null)
					return false;
			} else if (!classLoader.equals(other.classLoader))
				return false;
			if (patterns == null) {
				if (other.patterns != null)
					return false;
			} else if (!patterns.equals(other.patterns))
				return false;
			if (recurse != other.recurse)
				return false;
			return true;
		}

		private JarBuilder getOuterType() {
			return JarBuilder.this;
		}
	}
	
}
