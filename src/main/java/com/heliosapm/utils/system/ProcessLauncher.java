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
package com.heliosapm.utils.system;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Title: ProcessLauncher</p>
 * <p>Description: Simple external java process launcher</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.system.ProcessLauncher</code></p>
 */

public class ProcessLauncher {
	/** The underlying process builder */
	protected ProcessBuilder pb = null;
	/** The commands and arguments to be executed */
	protected final List<String> cmds = new ArrayList<String>();
	
	/** Indicates if the supporting OS is windows */
	public static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("windows");
	/** The java (jre) home directory */
	public static final File JAVA_HOME = new File(System.getProperty("java.home"));
	/** The java (jre) executable */
	public static final File JRE_EXE = new File(JAVA_HOME.getAbsolutePath() + File.separator + "bin" + File.separator + "java" + (IS_WIN ? ".exe" : ""));
	
	/**
	 * Creates a new process lancher
	 * @param cmds The leading commands / arguments
	 * @return the new launcher
	 */
	public static ProcessLauncher newInstance(final String...cmds) {
		final List<String> xcmds = new ArrayList<String>();
		for(String cmd: cmds) {
			if(cmd!=null && !cmd.trim().isEmpty()) {
				xcmds.add(cmd.trim());
			}
		}
		return new ProcessLauncher(xcmds);		
	}
	
	/**
	 * Creates a new process lancher
	 * @param cmds The leading commands / arguments
	 * @return the new launcher
	 */
	public static ProcessLauncher newInstance(final List<String> cmds) {
		return newInstance(cmds.toArray(new String[0]));
	}
	
	/**
	 * Creates a new ProcessLauncher
	 * @param cmds The base commands
	 */
	private ProcessLauncher(final List<String> cmds) {
		if(cmds!=null && !cmds.isEmpty()) {
			this.cmds.addAll(cmds);
		}
	}
	
	/**
	 * Appends the java executable running this JVM to prepare an external java launch.
	 * @return this launcher
	 */
	public ProcessLauncher thisJre() {
		if(!JRE_EXE.canExecute()) {
			throw new RuntimeException("The default JRE at [" + JRE_EXE + "] cannot be executed");
		}
		cmds.add(JRE_EXE.getAbsolutePath());
		return this;
	}
	
	/**
	 * Appends a command to the process launcher
	 * @param cmds The commands to append
	 * @return this launcher
	 */
	public ProcessLauncher cmd(final String...cmds) {
		for(String cmd: cmds) {
			if(cmd!=null && !cmd.trim().isEmpty()) {
				this.cmds.add(cmd.trim());
			}
		}
		return this;
	}
	
	/**
	 * Executes the configured process
	 * @param streamHandler true to install an auto-stream handler
	 * @param prefix An optional prefix to put on all stream lines
	 * @return the process
	 */
	public Process execute(final boolean streamHandler, final String prefix) {
		pb = new ProcessBuilder(cmds);
		try {
			final Process p = pb.start();
			if(streamHandler) {
				final ProcessStreamHandler psh = new ProcessStreamHandler(p, prefix);				
			}
			return p;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to start process [" + cmds + "]", ex);
		}
	}

}