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
package com.heliosapm.shorthand.attach.agent.installer;

import java.lang.instrument.Instrumentation;


/**
 * <p>Title: GenericAgentInstaller</p>
 * <p>Description: The executable component of a stub agent, installed to assist in boot-strapping another agent
 * without loading it into the boot class path.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.attach.agent.installer.GenericAgentInstaller</code></p>
 */
public class GenericAgentInstaller {
	/*
	 * read real-agent jar location from agent args
	 * create isolated class loader for real-agent
	 * create MLET for class loader
	 * read agent class name from real-agent jar manifest
	 * create  real-agent instance and initialize with agent commands (not including real agent jar location)
	 */
	
	
	/**
	 * Creates a new GenericAgentInstaller
	 */
	private GenericAgentInstaller() {
		// No Op 
	}
	
	/**
	 * The agent premain
	 * @param agentArgs The agent argument string
	 * @param inst The instrumentation
	 */
	public static void premain(final String agentArgs, final Instrumentation inst) {
		try {
			// TODO
		} catch (Throwable ex) {
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * The agent premain with no instrumentation
	 * @param agentArgs The agent argument string
	 */
	public static void premain(final String agentArgs) {
		premain(agentArgs, null);
	}
	
	/**
	 * The agent main
	 * @param agentArgs The agent argument string
	 * @param inst The instrumentation
	 */
	public static void agentmain(final String agentArgs, final Instrumentation inst) {
		premain(agentArgs, inst);
	}

	/**
	 * The agent main with no instrumentation
	 * @param agentArgs The agent argument string
	 */
	public static void agentmain(final String agentArgs) {
		premain(agentArgs, null);
	}
	
	

}
