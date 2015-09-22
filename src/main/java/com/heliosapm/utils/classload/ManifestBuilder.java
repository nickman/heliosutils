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

import java.util.jar.Manifest;

import com.heliosapm.shorthand.attach.vm.agent.AgentInstrumentation;

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
	
	
//	manifest.append("Manifest-Version: 1.0\nAgent-Class: " + AgentInstrumentation.class.getName() + "\n");
//	manifest.append("Can-Redefine-Classes: true\n");
//	manifest.append("Can-Retransform-Classes: true\n");
//	manifest.append("Premain-Class: " + AgentInstrumentation.class.getName() + "\n");
	
	
	
	/**
	 * Creates a new ManifestBuilder
	 */
	public ManifestBuilder() {

	}

}
