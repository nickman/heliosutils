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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.management.ObjectName;
import javax.management.loading.PrivateMLet;

/**
 * <p>Title: PrivateClassLoaderAgent</p>
 * <p>Description: A java agent that loads another agent's jar in a private classloader to avoid polluting the target app's classpath.</p>
 * <p>Install agent argument is one string containing:<ol>
 * 	<li>An absolute or relative, file or URL to the target agent jar to load. Will make a best effort to resolve the reference.</li>
 *  <li>The agent command to the target agent to be privately loaded.</li>
 *  </ol></p>
 *  <p>As such, the two parameters will be split simply by the first white space in the command.</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.agent.PrivateClassLoaderAgent</code></p>
 */

public class PrivateClassLoaderAgent {
	
	/** The instrumentation instance passed by the agent bootstrap */
	private static Instrumentation INSTRUMENTATION = null;
	/** The target agent jar private classloader */
	private static PrivateMLet privateClassLoader = null;
	
	/** The format for the agent classloader's MBean */
	public static final String OBJECT_NAME_FMT = "com.heliosapm.utils.agent:service=AgentClassLoader,jar=%s";
	
	/** The manifest entry key for the pre-main agent class name */
	public static final String PRE_MAIN_CLASS = "Premain-Class";
	/** The manifest entry key for the agent class name */
	public static final String AGENT_CLASS = "Agent-Class";


	/**
	 * Creates a new PrivateClassLoaderAgent
	 */
	public PrivateClassLoaderAgent() {

	}
	
	/**
	 * The agent premain
	 * @param agentArgs The agent arguments
	 * @param inst The agent instrumentation
	 */
	public static void premain(final String agentArgs, final Instrumentation inst) {
		if(INSTRUMENTATION==null) INSTRUMENTATION = inst;
		try {
			final String[] splitCommand = splitCommand(agentArgs);
			log("Command: jar:[%s], command:[%s]", splitCommand[0], splitCommand[1]);
			final URL jarUrl = jarToUrl(splitCommand[0]);
			log("Agent JAR URL:[%s]", jarUrl);
			privateClassLoader = createClassLoader(jarUrl);
			final String fileName = jarUrl.getFile();
			final ObjectName on = new ObjectName(String.format(OBJECT_NAME_FMT, fileName));
			ManagementFactory.getPlatformMBeanServer().registerMBean(privateClassLoader, on);
			log("PrivateAgentLoader registered at [%s]", on);
			final Class<?> agentClass = loadTargetAgent(jarUrl);
			log("Agent class loaded [%s]", agentClass.getName());
			final Method agentInvoker = getAgentInvocationMethod(agentClass);
			if(agentInvoker==null) {
				System.err.println("Failed to find any agent invocation methods in [" + agentClass.getName() + "]");
				return;
			}
			log("Invoking agent method [%s]", agentInvoker.toGenericString());
			if(agentInvoker.getParameterTypes().length==2) {
				agentInvoker.invoke(null, splitCommand[1], INSTRUMENTATION);
			} else {
				agentInvoker.invoke(null, splitCommand[1]);
			}
			log("Agent [%s] successfully loaded");
		} catch (Exception ex) {
			System.err.println("Failed to load agent");
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * The agent premain with no instrumentation
	 * @param agentArgs The agent arguments
	 */
	public static void premain(final String agentArgs) {
		premain(agentArgs, null);
	}
	
	/**
	 * The agent main 
	 * @param agentArgs The agent arguments
	 * @param inst The agent instrumentation
	 */
	public static void agentmain(final String agentArgs, final Instrumentation inst) {
		premain(agentArgs, inst);
	}

	/**
	 * The agent main with no instrumentation
	 * @param agentArgs The agent arguments
	 */
	public static void agentmain(final String agentArgs) {
		premain(agentArgs, null);
	}
	
	/**
	 * Splits the agent command into the target jar and the target agent command
	 * @param agentArgs The agent argument to this agent
	 * @return the target jar and the target agent command
	 */
	public static String[] splitCommand(final String agentArgs) {
		if(agentArgs==null || agentArgs.trim().isEmpty()) throw new IllegalArgumentException("The passed agent argument was empty");
		final String[] splitArgs = new String[2];
		final int index = agentArgs.indexOf(' ');
		splitArgs[0] = agentArgs.substring(0, index);
		splitArgs[1] = agentArgs.substring(index+1).trim();
		return splitArgs;
	}
	
	/**
	 * Attempts to resolve the passed name as a URL referencing the target agent jar
	 * @param name The name to resolve
	 * @return the URL of the taget agent jar
	 * @throws MalformedURLException thrown if the name cannot be resolved to a URL
	 */
	public static URL jarToUrl(final String name) throws MalformedURLException {
		final File f = new File(name);
		if(f.canRead()) {
			return f.toURI().toURL();
		}
		return new URL(name);
	}
	
	/**
	 * Creates the private class loader
	 * @param url The URL of the target agent jar
	 * @return the private class loader
	 */
	public static PrivateMLet createClassLoader(final URL url) {
		return new PrivateMLet(new URL[]{url}, true);
	}
	
	/**
	 * Formatted out logger
	 * @param fmt The message format
	 * @param args The message arguments
	 */
	public static void log(final Object fmt, final Object...args) {
		System.out.println("PrivateClassLoaderAgent [" + new Date() + "]:" + String.format(fmt.toString(), args));
	}
	
//	public static final String PRE_MAIN_CLASS = "Premain-Class";
//	public static final String AGENT_CLASS = "Agent-Class";
	
	
	/**
	 * Reads the manifest from the target jar, reads the agent classname and attempts to load the agent class
	 * @param url The URL of the target agent jar
	 * @return The target agent instance
	 * @throws IOException thrown on any IO error
	 */
	public static Class<?> loadTargetAgent(final URL url) throws IOException {
		InputStream is = null;
		JarInputStream jis = null;
		try {
			is = url.openStream();
			jis = new JarInputStream(is, true);
			final Manifest manifest = jis.getManifest();
			final Attributes attributes = manifest.getMainAttributes();			
			String className = attributes.getValue(PRE_MAIN_CLASS);
			if(className==null) {
				className = attributes.getValue(AGENT_CLASS);
			}
			if(className==null) throw new RuntimeException("Failed to load agent class from manifest in [" + url + "]");
			try {
				return privateClassLoader.loadClass(className);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to load agent class [" + className + "] from jar [" + url + "]");
			}
		} finally {
			if(jis!=null) try { jis.close(); } catch (Exception x) {/* No Op */}
			if(is!=null) try { is.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Attempts to locate the agent invocation method
	 * @param clazz The agent class
	 * @return The first found method or null if no method ws found
	 */
	public static Method getAgentInvocationMethod(final Class<?> clazz) {
		Method m = m(clazz, "premain", String.class, Instrumentation.class);
		if(m!=null) return m;
		m = m(clazz, "agentmain", String.class, Instrumentation.class);
		if(m!=null) return m;		
		m = m(clazz, "premain", String.class);
		if(m!=null) return m;		
		return m(clazz, "agentmain", String.class);
		
	}
	
	private static Method m(final Class<?> clazz, final String methodName, final Class<?>...paramTypes) {
		try {
			return clazz.getDeclaredMethod(methodName, paramTypes);
		} catch (Exception ex) {
			try { 
				return clazz.getMethod(methodName, paramTypes);
			} catch (Exception ex2) {
				return null;
			}
		}
	}
	

}
