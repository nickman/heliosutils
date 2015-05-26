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
package com.heliosapm.utils.io;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Title: StdInCommandHandler</p>
 * <p>Description: Listens on StdIn for registered and subitted commands</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.io.StdInCommandHandler</code></p>
 */

public class StdInCommandHandler implements Runnable {
	/** The singleton instance */
	private static volatile StdInCommandHandler instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock =  new Object();

	/** A map of the commands keyed by the command name */
	private final Map<String, Runnable> commands = new ConcurrentHashMap<String, Runnable>();
	/** The std in polling thread */
	private final Thread stdInThread;
	
	/**
	 * Acquires and returns the singleton StdInCommandHandler instance
	 * @return the singleton StdInCommandHandler instance
	 */
	public static StdInCommandHandler getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new StdInCommandHandler();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new StdInCommandHandler
	 */
	private StdInCommandHandler() {
		commands.put("exit", new Runnable(){
			@Override
			public void run() {
				System.exit(0);
			}
		});
		commands.put("exit-1", new Runnable(){
			@Override
			public void run() {
				System.exit(-1);
			}
		});
		
		stdInThread = new Thread(this, "StdInCommandHandlerThread");
		stdInThread.setDaemon(true);
		stdInThread.start();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		String command = null;
		BufferedReader br = null;
		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(System.in);
			br = new BufferedReader(isr);
			while(true) {
				try {
					command = br.readLine();
					if(command==null || command.trim().isEmpty()) continue;
					Runnable r = commands.get(command.trim().toLowerCase());
					if(r==null) {
						System.err.println("[StdInCommandHandler]: Unrecognized command: [" + command + "]");						
					} else {
						r.run();
					}
				} catch (Exception ex) {
					if(InterruptedException.class.isInstance(ex)) break;
					if(command!=null) {
						System.err.println("[StdInCommandHandler]: Command [" + command + "] failed. Stack trace follows.");
						ex.printStackTrace(System.err);
					}
				} finally {
					command = null;
				}
			}
		} catch (Exception ex) {
			System.err.println("[StdInCommandHandler]: Failed to initialize StdInReader. Stack trace follows.");
			ex.printStackTrace(System.err);
		} finally {
			if(isr!=null) try { isr.close(); } catch (Exception x) {/* No Op */}
			if(br!=null) try { br.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	
	/**
	 * Installs a new command
	 * @param command The command name
	 * @param runnable The command to execute
	 */
	public void registerCommand(final String command, final Runnable runnable) {
		if(command==null || command.trim().isEmpty()) throw new IllegalArgumentException("The passed command name was null");
		if(runnable==null) throw new IllegalArgumentException("The passed runnable was null");
		final String key = command.trim().toLowerCase();
		boolean installed = false;
		if(!commands.containsKey(key)) {
			synchronized(commands) {
				if(!commands.containsKey(key)) {
					commands.put(command, runnable);
					installed = true;
				}
			}
		}
		if(!installed) {
			throw new IllegalStateException("Failed to install command [" + command + "]. Command already installed");
		}
	}

}
