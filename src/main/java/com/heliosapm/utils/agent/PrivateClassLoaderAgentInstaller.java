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
package com.heliosapm.utils.agent;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.heliosapm.shorthand.attach.vm.VirtualMachine;
import com.heliosapm.utils.jar.JarBuilder;

/**
 * <p>Title: PrivateClassLoaderAgentInstaller</p>
 * <p>Description: Installer for java agents that loads the target agent through a private class loader so as to not pollute the classpath</p> 
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.agent.PrivateClassLoaderAgentInstaller</code></p>
 */

public class PrivateClassLoaderAgentInstaller implements Closeable {
	/** The id of the target JVM */
	final String vmId;
	/** The target JVM */
	final VirtualMachine vm;
	/** The private agent jar file */
	final File privateAgentJarFile;
	
	/**
	 * Creates a new PrivateClassLoaderAgentInstaller
	 * @param vmId The id of the target JVM
	 */
	public PrivateClassLoaderAgentInstaller(final String vmId) {
		this.vmId = vmId;
		vm = VirtualMachine.attach(vmId);
		privateAgentJarFile = new JarBuilder().res(PrivateClassLoaderAgent.class.getName()).classLoader(PrivateClassLoaderAgent.class).apply()
			.manifestBuilder()
				.agentClass(PrivateClassLoaderAgent.class.getName())
				.preMainClass(PrivateClassLoaderAgent.class.getName())
				.done()
			.build();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.Closeable#close()
	 */
	public void close() throws IOException {
		vm.detach();
	}
	
	/**
	 * Returns the attached vm's system properties
	 * @return the attached vm's system properties
	 */
	public Properties getSystemProperties() {
		return vm.getSystemProperties();
	}
	
	/**
	 * Returns the attached vm's agent properties
	 * @return the attached vm's agent properties
	 */
	public Properties getAgentProperties() {
		return vm.getAgentProperties();
	}
	
	
	/**
	 * Installs the specified java agent in the specified target agent jar with the specified agent command
	 * @param targetAgent The jar of the agent to load
	 * @param agentCommand The agent command
	 */
	public void install(final String targetAgent, final String agentCommand) {
		vm.loadAgent(privateAgentJarFile.getAbsolutePath(), targetAgent + " " + agentCommand);
	}
	
	/*
	 * Create a new jar
	 * Put the private agent installer class in the jar
	 * create a manifest containing the agent name for PreMain and PreAgent
	 * add the manifest to the jar
	 * return the URL of the jar
	 */

}
