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

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Title: ProcessStreamHandler</p>
 * <p>Description: Simple utility to sping up two threads and handle the incoming
 * out and err streams (such as from a process) and redirect to this JVM's stdout and stderr.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.system.ProcessStreamHandler</code></p>
 */

public class ProcessStreamHandler {
	final Process process;
	final Thread outThread;
	final Thread errThread;
	final String prefix;
	
	/** The UTF character set */
	public static final Charset UTF8 = Charset.forName("UTF8");
	
	/**
	 * Creates a new ProcessStreamHandler that handles both system.out and system.err streams
	 * @param process The process to stream from
	 * @param prefix A prefix put in front of every printed line
	 * @param handleOut true to handle system.out, false otherwise
	 * @param handleErr true to handle system.err, false otherwise
	 */
	public ProcessStreamHandler(final Process process, final String prefix, final boolean handleOut, final boolean handleErr) {
		this.process = process;
		this.prefix = "[" + prefix + "]";
		outThread = new StreamThread(process.getInputStream(), System.out);
		errThread = new StreamThread(process.getErrorStream(), System.err);
		outThread.start();
		errThread.start();
	}
	
	/**
	 * Creates a new ProcessStreamHandler that handles both system.out and system.err streams
	 * @param process The process to stream from
	 * @param prefix A prefix put in front of every printed line
	 */
	public ProcessStreamHandler(final Process process, final String prefix) {
		this(process, prefix, true, true);
	}
	
	
	private class StreamThread extends Thread {
		/** The incoming stream */
		final InputStream in;
		/** The outbound print writer to redirect to */
		final PrintStream out;
		
		/**
		 * Creates a new StreamThread
		 * @param in The incoming stream
		 * @param out The outbound print writer to redirect to
		 */
		public StreamThread(final InputStream in, final PrintStream out) {
			super("Process Stream Handler for Process [" + process + "]");
			this.setDaemon(true);
			this.in = in;
			this.out = out;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			int bytesRead = 0;
			byte[] buff = new byte[1024];
			while(true) {
				try {
					while((bytesRead = in.read(buff))!=-1) {						
						out.print(prefix + new String(buff, 0, bytesRead));
					}
				} catch (Exception x) {/* No Op */}
			}
		}
		
		
	}

}
