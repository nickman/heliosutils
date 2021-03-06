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
import java.util.concurrent.atomic.AtomicReference;

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
	
	/** The self joined thread */
	private Thread joinedThread = null;
	
	/** The name of the shutdown hook handler */
	private final AtomicReference<String> shutdownHookHandler = new AtomicReference<String>(null); 
	
	private final AtomicReference<Runnable> unhandled = new AtomicReference<Runnable>(null);
	private static final ThreadLocal<String> unhandledCommandName = new ThreadLocal<String>(); 
	
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
		commands.put("gc", new Runnable(){
			@Override
			public void run() {
				System.out.println("GC triggered by StdInCommandHandler.");
				System.gc();
			}
		});
		
		commands.put("exit", new Runnable(){
			@Override
			public void run() {
				System.out.println("Shutdown triggered by StdInCommandHandler. Exit code: 0");
				System.exit(0);
			}
		});
		commands.put("halt", new Runnable(){
			@Override
			public void run() {
				System.out.println("Halt triggered by StdInCommandHandler. Exit code: -1");
				Runtime.getRuntime().halt(-1);				
			}
		});
		
		commands.put("exit-1", new Runnable(){
			@Override
			public void run() {
				System.err.println("Shutdown triggered by StdInCommandHandler. Exit code: -1");
				System.exit(-1);
			}
		});
		commands.put("/", new Runnable(){
			@Override
			public void run() {
				final String shutdownCommand = shutdownHookHandler.get();
				final StringBuilder b = new StringBuilder("\nStdIn Commands:\n==============\n");
				for(String name: commands.keySet()) {
					b.append(name);
					if(name.equals(shutdownCommand)) {
						b.append("\t** Shutdown Hooked");
					}
					b.append("\n");
				}
				b.append("==============");
				System.out.println(b.toString());
			}
		});
		final Thread sdHook = new Thread("StdInCommandHandlerShutdownHook") {
			public void run() {
				final String shutdownCommand = shutdownHookHandler.get();
				if(shutdownCommand!=null) {
					
//					if(joinedThread != null) joinedThread.interrupt();
//					final Runnable r = commands.get(shutdownCommand.trim().toLowerCase());
//					if(r!=null) r.run();
				}
			}
		};
		Runtime.getRuntime().addShutdownHook(sdHook);
		stdInThread = new Thread(this, "StdInCommandHandlerThread");
		stdInThread.setDaemon(true);
		stdInThread.start();
	}
	
	/**
	 * Runs the command handler in a background thread
	 * @param daemon true to make the thread a daemon thread, false otherwise
	 * @return this handler
	 */
	public StdInCommandHandler runAsync(final boolean daemon) {
		final StdInCommandHandler handler = this;
		final Thread t = new Thread("AsyncStdInCmdHandler") {
			@Override
			public void run() {
				handler.run();
			}
		};
		t.setDaemon(daemon);
		t.start();
		return this;
	}
	
	/**
	 * Causes the calling thread to [@link {@link Thread#join()} itself until interrupted.
	 */
	public void join() {
		try {
			if(Thread.interrupted()) Thread.interrupted();
			joinedThread = Thread.currentThread();
			Thread.currentThread().join();
		} catch (InterruptedException iex) {
			if(Thread.interrupted()) Thread.interrupted();
		}
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
						final Runnable unhandled = this.unhandled.get();
						if(unhandled==null) {
							System.err.println("[StdInCommandHandler]: Unrecognized command: [" + command + "]");
						} else {
							try {
								unhandledCommandName.set(command.trim().toLowerCase());
								unhandled.run();
							} finally {
								unhandledCommandName.remove();
							}
						}
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
	 * Sets a command to be run by a shutdown hook
	 * @param cmd The name of the command to be run
	 * @return this command handler
	 */
	public StdInCommandHandler shutdownHook(final String cmd) {
		if(cmd==null || cmd.trim().isEmpty()) throw new IllegalArgumentException("The command was null or empty");
		if(!commands.containsKey(cmd.trim().toLowerCase())) {
			throw new IllegalArgumentException("The command [" + cmd + "] is not registered");
		}
		shutdownHookHandler.set(cmd.trim().toLowerCase());
		return this;
	}
	
	/**
	 * Sets <b><code>exit</code></b> as the command to be run by a shutdown hook
	 * @return this command handler
	 */
	public StdInCommandHandler shutdownHook() {
		return shutdownHook("exit");
	}

	
	/**
	 * Returns the unhandled command name.
	 * Only valid while the unhandled command callback is being run
	 * @return the unhandled command name.
	 */
	public String getUnhandledCommandName() {
		return unhandledCommandName.get();
	}

	/**
	 * Installs a new command
	 * @param command The command name
	 * @param runnable The command to execute
	 * @return this command handler
	 */
	public StdInCommandHandler registerCommand(final String command, final Runnable runnable) {
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
		return this;
	}
	
	/**
	 * Installs a new command if not already installed
	 * @param command The command name
	 * @param runnable The command to execute
	 * @return this command handler
	 */
	public StdInCommandHandler registerCommandIfNotInstalled(final String command, final Runnable runnable) {
		final String key = command.trim().toLowerCase();
		if(!commands.containsKey(key)) {
			registerCommand(key, runnable);
		}
		return this;
	}
	
	/**
	 * Installs a no key command executed when the key is not found in the commands map
	 * @param runnable The command to execute
	 */
	public void registerNoKeyCommand(final Runnable runnable) {
		unhandled.set(runnable);
	}
	

}
