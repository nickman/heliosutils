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
package com.heliosapm.shorthand.attach.vm.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.heliosapm.utils.classload.IsolatedClassLoader;

/**
 * <p>Title: AgentBootstrap</p>
 * <p>Description: The main agent boot shim</p>
 * <p>Agent args format:<ol>
 * 	<li>The argument delimeter. Any non whitespace deterministic arbitrary value can be used provided it is not the delimeter termination.</li>
 *  <li>The argument delimeter termination: <b><code>##</code></b></li>
 *  <li>The class name to boot which must have a constructor with the signature <b><code>(Instrumentation, String...)</code></b>.</li>
 *  <li>The remaining arguments, parsed using the defined delimeter and passed to the boot class's second ctor argument.</li>
 * </ol>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.attach.vm.agent.AgentBootstrap</code></p>
 */

public class AgentBootstrap {
	/** Static class logger */
	private final static Logger log = Logger.getLogger(AgentBootstrap.class.getName());
	
	/** The args delimeter terminator */
	public static final String DELIM_TERM = "##";
	/** The booted agent instance */
	public static Object bootedAgent = null;
	
	

	/**
	 * The agent bootstrap entry point
	 * @param agentArgs The agent initialization arguments
	 * @param inst The instrumentation instance
	 */	
	public static void main(final String agentArgs, final Instrumentation inst) {
		if(agentArgs==null || agentArgs.trim().isEmpty()) throw new IllegalArgumentException("The agent arguments were null or empty");
		final String _args = agentArgs.trim();
		log.fine("Agent Args: [" + _args + "]");
		final String delim = delim(_args);
		log.fine("Args Delim: [" + delim + "]");
		final String[] varArgs = args(delim, _args.substring(delim.length() + DELIM_TERM.length()));
		if(varArgs.length==0) throw new IllegalArgumentException("The agent arguments [" + agentArgs + "] had no boot class.");
		final String bootClass = varArgs[0];
		log.fine("Boot Class: [" + bootClass + "]");
		final int rargsCount = varArgs.length-1;
		final String[] remainingArgs = new String[rargsCount];
		if(rargsCount>0) {
			System.arraycopy(varArgs, 1, remainingArgs, 0, rargsCount);
		}
		log.fine("Remaining Args: " + Arrays.toString(remainingArgs));
		try {
			final IsolatedClassLoader icl = new IsolatedClassLoader(AgentBootstrap.class, AgentBootstrap.class.getPackage().getName() + ":service=" + AgentBootstrap.class.getSimpleName());
			final ClassLoader current = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(icl);
				final Class<?> clazz = Class.forName(bootClass, true, icl);
				log.fine("Loaded Class: [" + clazz.getName() + "]");
				final Constructor<?> ctor = clazz.getDeclaredConstructor(Instrumentation.class, String[].class);
				log.fine("Located Constructor: [" + ctor.toGenericString() + "]");
				bootedAgent = ctor.newInstance(inst, remainingArgs);
				log.fine("Agent Booted Successfully");
			} finally {
				Thread.currentThread().setContextClassLoader(current);
			}
		} catch (Exception ex) {
			bootedAgent = null;
			log.log(Level.SEVERE, "Failed to boot agent", ex);
		}
	}
	
	private static String[] args(final String delim, final String agentArgs) {
		final List<String> l = new ArrayList<String>();
		final StringTokenizer tokenizer = new StringTokenizer(agentArgs, delim);
		while (tokenizer.hasMoreTokens()) {
			final String token = tokenizer.nextToken();
			if(token!=null && !token.trim().isEmpty()) {
				l.add(token.trim());
			}
		}
		return l.toArray(new String[0]);
	}
	
	private static String delim(final String agentArgs) {
		final int index = agentArgs.indexOf(DELIM_TERM);
		if(index<1) throw new IllegalArgumentException("The agent arguments [" + agentArgs + "] had no delimeter termination.");
		return agentArgs.substring(0, index);
	}

	/**
	 * The agent bootstrap entry point
	 * @param agentArgs The agent initialization arguments
	 * @param inst The instrumentation instance
	 */	
	public static void agentmain(final String agentArgs, final Instrumentation inst) {
		main(agentArgs, inst);
	}
	
	/**
	 * The agent bootstrap entry point
	 * @param agentArgs The agent initialization arguments
	 * @param inst The instrumentation instance
	 */
	public static void premain(final String agentArgs, final Instrumentation inst) {	
		main(agentArgs, inst);
	}
	
	/**
	 * The agent bootstrap entry point which fails the install since there is no instrumentation
	 * @param agentArgs The agent initialization arguments
	 */	
	public static void agentmain(final String agentArgs) {
		main(agentArgs, null);
	}
	
	/**
	 * The agent bootstrap entry point which fails the install since there is no instrumentation
	 * @param agentArgs The agent initialization arguments
	 */	
	public static void premain(final String agentArgs) {
		main(agentArgs, null);
	}
		


}
