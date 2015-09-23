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
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Title: ManifestBuilder</p>
 * <p>Description: Fluent style manifest builder</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.classload.ManifestBuilder</code></p>
 */

public class ManifestBuilder {
	/** The initally empty manifest */
	private final Manifest manifest = new Manifest();
	/** The base text of the manifest */
	private final StringBuilder b = new StringBuilder("Manifest-Version: 1.0\n");
	/** Map to accumulate the unique entries */
	private final Map<String, String> entries = new LinkedHashMap<String, String>();
	
	/** Static class logger */
	private final static Logger log = Logger.getLogger(ManifestBuilder.class.getName()); 
	
	public static final String MAIN_CLASS = "Main-Class: %s\n";
	public static final String PRE_MAIN_CLASS = "Premain-Class: %s\n";
	public static final String AGENT_CLASS = "Agent-Class: %s\n";
	public static final String BOOT_CLASS_PATH = "Boot-Class-Path: %s\n";
	public static final String REDEFINE = "Can-Redefine-Classes: %s\n";
	public static final String RETRANSFORM = "Can-Retransform-Classes: %s\n";
	public static final String NATIVE_METHOD_PREFIX = "Can-Set-Native-Method-Prefix: %s\n";
	public static final String CREATED_BY = "Created-By: %s\n";
	public static final String NAME = "Name: %s\n";
	public static final String SPEC_TITLE = "Specification-Title: %s\n";
	public static final String SPEC_VERSION = "Specification-Version: %s\n";
	public static final String SPEC_VENDOR = "Specification-Vendor: %s\n";
	public static final String IMPL_TITLE = "Implementation-Title: %s\n";
	public static final String IMPL_VERSION = "Implementation-Version: %s\n";
	public static final String IMPL_VENDOR = "Implementation-Vendor: %s\n";	
	public static final String CUSTOM = "%s: %s\n";
	public static final Charset UTF8 = Charset.forName("UTF8");
	
	public Manifest build() {
		for(String v: entries.values()) {
			b.append(v);			
		}
		try {
			if(log.isLoggable(Level.FINE)) log.fine("\n====================\n\tManifest\n=====================\n" + b + "\n====================="); 			
			manifest.read(new ByteArrayInputStream(b.toString().getBytes(UTF8)));
			return manifest;
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Failed to load manifest", ex);
			throw new RuntimeException("Failed to load manifest", ex);
		}
	}
	
	/**
	 * Creates and add an automatic and informative created-by manifest entry
	 * @return this builder
	 */
	public ManifestBuilder autoCreatedBy() {
		final String cb = String.format("%s on %s at %s, os:[%s/%s], jvm[%s/%s/%s]",
				System.getProperty("user.name"),
				ManagementFactory.getRuntimeMXBean().getName().split("@")[1],
				new Date(),
				System.getProperty("os.name"),
				System.getProperty("os.arch"),
				System.getProperty("java.runtime.version"),
				System.getProperty("java.runtime.version"),
				System.getProperty("sun.arch.data.model", "unknown"),
				System.getProperty("java.vendor")				 
		);
		
		return createdBy(cb);
	}

	/**
	 * Sets a custom entry
	 * @param k The key
	 * @param v The value
	 * @return this builder
	 */
	public ManifestBuilder custom(final String k, final String v) {
		if(v==null || v.trim().isEmpty()) throw new IllegalArgumentException("The passed value was null or empty");
		if(k==null || k.trim().isEmpty()) throw new IllegalArgumentException("The passed key was null or empty");
		entries.put(k.trim(), String.format(CUSTOM, k, v));
		return this;
	}

	
	/**
	 * Sets the entry for the implementation version
	 * @param v The value
	 * @return this builder
	 */
	public ManifestBuilder implVersion(final String v) {
		return val(v, IMPL_VERSION);
	}
	
	
	/**
	 * Sets the entry for the implementation vendor
	 * @param v The value
	 * @return this builder
	 */
	public ManifestBuilder implVendor(final String v) {
		return val(v, IMPL_VENDOR);
	}

	
	/**
	 * Sets the entry for the implementation title
	 * @param v The value
	 * @return this builder
	 */
	public ManifestBuilder implTitle(final String v) {
		return val(v, IMPL_TITLE);
	}


	
	/**
	 * Sets the entry for the specification version
	 * @param v The value
	 * @return this builder
	 */
	public ManifestBuilder specVersion(final String v) {
		return val(v, SPEC_VERSION);
	}
	
	
	/**
	 * Sets the entry for the specification vendor
	 * @param v The value
	 * @return this builder
	 */
	public ManifestBuilder specVendor(final String v) {
		return val(v, SPEC_VENDOR);
	}

	
	/**
	 * Sets the entry for the specification title
	 * @param v The value
	 * @return this builder
	 */
	public ManifestBuilder specTitle(final String v) {
		return val(v, SPEC_TITLE);
	}

	
	
	/**
	 * Sets the entry for the manifest name
	 * @param v The value
	 * @return this builder
	 */
	public ManifestBuilder name(final String v) {
		return val(v, NAME);
	}
	
	
	
	
	/**
	 * Sets the entry for the jar's created by
	 * @param v The value
	 * @return this builder
	 */
	public ManifestBuilder createdBy(final String v) {
		return val(v, CREATED_BY);
	}


	/**
	 * Sets the entry for the agent's redefine capability
	 * @param enabled True if enabled, false otherwise
	 * @return this builder
	 */
	public ManifestBuilder redefine(final boolean enabled) {
		entries.put(REDEFINE, String.format(REDEFINE, enabled));
		return this;
	}
	
	/**
	 * Sets the entry for the agent's can set native prefix capability
	 * @param enabled True if enabled, false otherwise
	 * @return this builder
	 */
	public ManifestBuilder nativePrefix(final boolean enabled) {
		entries.put(NATIVE_METHOD_PREFIX, String.format(NATIVE_METHOD_PREFIX, enabled));
		return this;
	}
	
	
	/**
	 * Sets the entry for the agent's retransform capability
	 * @param enabled True if enabled, false otherwise
	 * @return this builder
	 */
	public ManifestBuilder retransform(final boolean enabled) {
		entries.put(RETRANSFORM, String.format(RETRANSFORM, enabled));
		return this;
	}
	
	/**
	 * Sets the entry for the boot class path
	 * @param v The value
	 * @return this builder
	 */
	public ManifestBuilder bootClassPath(final String v) {
		return val(v, BOOT_CLASS_PATH);
	}
	
	
	/**
	 * Sets the entry for the agent class
	 * @param v The value
	 * @return this builder
	 */
	public ManifestBuilder agentClass(final String v) {
		return val(v, AGENT_CLASS);
	}

	
	/**
	 * Sets the entry for the pre-main class
	 * @param v The value
	 * @return this builder
	 */
	public ManifestBuilder preMainClass(final String v) {
		return val(v, PRE_MAIN_CLASS);
	}

	
	/**
	 * Sets the entry for the main class
	 * @param v The value
	 * @return this builder
	 */
	public ManifestBuilder mainClass(final String v) {
		return val(v, MAIN_CLASS);
	}

	private ManifestBuilder val(final String v, final String name) {
		if(v==null || v.trim().isEmpty()) throw new IllegalArgumentException("The passed value for [" + name + "] was null or empty");
		entries.put(name, String.format(name, v.trim()));
		return this;
	}

	
	/**
	 * Builds the manifest and return back the starting jar builder
	 * @return the parent jar builder
	 */
	public JarBuilder done() {
		if(jb==null) throw new IllegalStateException("This manifest builder was not started from a JarBuilder");
		jb.setMainifest(build());
		return jb;
	}
	
	/**
	 * Creates a new ManifestBuilder
	 */
	public ManifestBuilder() {}
	
	/**
	 * Creates a new ManifestBuilder
	 * @param jb The parent JarBuilder
	 */
	public ManifestBuilder(final JarBuilder jb) {
		this.jb = jb;
	}

	
	private JarBuilder jb = null;
	
	

}
 