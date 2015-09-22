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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import javax.management.remote.jmxmp.JMXMPConnector;

import org.json.JSONObject;

import com.heliosapm.shorthand.attach.vm.agent.AgentInstrumentation;
import com.heliosapm.utils.file.FileFilterBuilder;
import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: JarBuilder</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.classload.JarBuilder</code></p>
 */

public class JarBuilder {
	final File jarFile;
	final Set<ResourceSpecifier> specifiers = new LinkedHashSet<ResourceSpecifier>();
	final Map<URL, String> foundResources = new HashMap<URL, String>();

	
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
	
	
	public static void main(String[] args) {
		log("JarBuilder Test");
		new JarBuilder()
			.res("javax.management.remote").classLoader(JMXMPConnector.class).apply()
			.res("org.json").classLoader(JSONObject.class).apply()
			.build();
	}
	
	public void build() {
		for(ResourceSpecifier rs: this.specifiers) {
			rs.find();
		}
		FileOutputStream fos = null;
		JarOutputStream jos = null;
		try {
			StringBuilder manifest = new StringBuilder();
			manifest.append("Manifest-Version: 1.0\nAgent-Class: " + AgentInstrumentation.class.getName() + "\n");
			manifest.append("Can-Redefine-Classes: true\n");
			manifest.append("Can-Retransform-Classes: true\n");
			manifest.append("Premain-Class: " + AgentInstrumentation.class.getName() + "\n");
			ByteArrayInputStream bais = new ByteArrayInputStream(manifest.toString().getBytes());
			Manifest mf = new Manifest(bais);
			fos = new FileOutputStream(jarFile, false);
			jos = new JarOutputStream(fos, mf);
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
					log("Wrote entry [" + entry.getValue() + "]");
				} catch (Exception ex) {
					throw new RuntimeException("Failed to write entry [" + entry.getValue() + "]", ex);
				} finally {
					if(is!=null) try { is.close(); } catch (Exception x) {/* No Op */}
				}
				//jos.putNextEntry(new ZipEntry(clazz.getName().replace('.', '/') + ".class"));
			}
			jos.flush();
			jos.close();
			fos.flush();
			fos.close();
			log("Jar Complete: [%s], Size: [%s] bytes", jarFile.getAbsolutePath(), jarFile.length());
		} catch (Exception e) {
			throw new RuntimeException("Failed to write Jar [" + jarFile + "]", e);
		} finally {
			if(fos!=null) try { fos.close(); } catch (Exception e) {}
		}
		
	}
	
	public static String binToRes(final String path) {
		if(path==null || path.trim().isEmpty()) throw new IllegalArgumentException("The passed path was null");
		return path.trim().replace('.', '/');
	}
	
	public static String binToPath(final String path) {
		if(path==null || path.trim().isEmpty()) throw new IllegalArgumentException("The passed path was null");
		return path.trim().replace('.', File.separatorChar);
	}
	
	
	public ResourceSpecifier res(final String base) {
		return new ResourceSpecifier(base);
	}
	
	public class ResourceSpecifier {
		final String base;
		ClassLoader classLoader = null;
		boolean recurse = false;
		final Set<Pattern> patterns = new LinkedHashSet<Pattern>();

		public ResourceSpecifier(final String base) {
			if(base==null || base.trim().isEmpty()) throw new IllegalArgumentException("The passed base was null");
			this.base = base;
		}
		
		public ResourceSpecifier classLoader(final Class<?> clazz) {
			if(clazz==null) throw new IllegalArgumentException("The passed class was null");
			final ClassLoader cl = clazz.getClassLoader();
			if(cl==null) throw new IllegalArgumentException("The class [" + clazz.getName() + "] has a primordial classloader");
			this.classLoader = cl;
			return this;
		}
		
		public ResourceSpecifier recurse(final boolean recurse) {
			this.recurse = recurse;
			return this;
		}
		
 		public ResourceSpecifier filterPath(final boolean caseSensitive, final String...patterns) {
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
			final File f = new File(url.getFile());
			if(f.isDirectory()) {
				final String prefix = f.getAbsolutePath() + File.separator + path + File.separator;
				final String strip = url.toString();
				File dir = new File(prefix);
				if(dir.isDirectory()) {
					for(File fo : FileFilterBuilder.newBuilder()
						.caseInsensitive(true)
						.shouldBeFile()						
						.fileFinder()
						.maxDepth(recurse ? Integer.MAX_VALUE : 1)
						.addSearchDir(dir)
						.find()) {						
						final String entryName = URLHelper.toURL(fo).toString().replace(strip, "");
						//log("DirEntry: [%s]:[%s]", entryName, URLHelper.toURL(fo));
						foundResources.put(URLHelper.toURL(fo), entryName);
					}
				}
			}
		}
		
		void scanJar(final URL url, final String path) {			
			InputStream is = null;
			JarInputStream jis = null;
			try {
				final String prefix = url.toString();
				is = url.openStream();
				jis = new JarInputStream(is);
				JarEntry jarEntry = null;
				while((jarEntry = jis.getNextJarEntry())!=null) {
					if(jarEntry.isDirectory()) continue;
					final String name = jarEntry.getName();
					if(name.indexOf(path)==0) {
						final URL resourceUrl = URLHelper.toURL("jar:" + url + "!/" + name);
						//log("JAR Entry: [%s]", URLHelper.toURL("jar:" + url + "!/" + name));
						foundResources.put(URLHelper.toURL("jar:" + url + "!/" + name), name);
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
			ResourceSpecifier other = (ResourceSpecifier) obj;
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
	
	
	public static void log(final Object fmt, final Object...args) {
		System.out.format(fmt + "\n", args);
	}

}
