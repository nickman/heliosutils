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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Title: ProcessStreamHandlers</p>
 * <p>Description: Some commonly used {@link IProcessStreamHandler} implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.system.ProcessStreamHandlers</code></p>
 */

public class ProcessStreamHandlers {

	/** The default {@link BufferedOutputStream} buffer size */
	public static final int BOS_BUFFER_SIZE = 8192 * 3;
	/** The default transfer byte array size */
	public static final int BA_BUFFER_SIZE = 8192;
	
	
	public static class StreamToFileHandler implements IProcessStreamHandler {
		/** The file to write to */
		final File output;
		/** The file output stream to write to */
		final FileOutputStream fos;
		/** The buffered output stream to write to */
		final BufferedOutputStream bos;
		/** The transfer byte array size */
		final int baSize;
		/** Flag set to false when the process end is signalled */
		final AtomicBoolean running = new AtomicBoolean(true);
		/** The reader thread */
		protected Thread readerThread = null;
		
		/**
		 * Creates a new StreamToFileHandler
		 * @param output The output file to write to
		 * @param append true to append, false to overwrite
		 * @param bosSize The {@link BufferedOutputStream} buffer size in bytes
		 * @param baSize The transfer byte array size
		 */
		public StreamToFileHandler(final File output, final boolean append, final int bosSize, final int baSize) {
			this.output = output;
			this.baSize = baSize;
			try {
				fos = new FileOutputStream(output, append);
				bos = new BufferedOutputStream(fos, bosSize);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to start StreamToFileHandler for file [" + output + "]");
			}
		}
		
		/**
		 * Creates a new StreamToFileHandler using the default buffer sizes
		 * @param output The output file to write to
		 * @param append true to append, false to overwrite
		 */
		public StreamToFileHandler(final File output, final boolean append) {
			this(output, append, BOS_BUFFER_SIZE, BA_BUFFER_SIZE);
		}
		
		/**
		 * Creates a new StreamToFileHandler
		 * @param output The output file name to write to
		 * @param append true to append, false to overwrite
		 * @param bosSize The {@link BufferedOutputStream} buffer size in bytes
		 * @param baSize The transfer byte array size
		 */
		public StreamToFileHandler(final String output, final boolean append, final int bosSize, final int baSize) {
			this(new File(output), append, bosSize, baSize);
		}
		
		/**
		 * Creates a new StreamToFileHandler using the default buffer sizes
		 * @param output The output file name to write to
		 * @param append true to append, false to overwrite
		 */
		public StreamToFileHandler(final String output, final boolean append) {
			this(new File(output), append, BOS_BUFFER_SIZE, BA_BUFFER_SIZE);
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.utils.system.IProcessStreamHandler#handleStream(java.io.InputStream, boolean, java.lang.Process)
		 */
		@Override
		public void handleStream(final InputStream in, final boolean out, final Process process) {
			final byte[] transfer = new byte[baSize];
			readerThread = new Thread("StreamHandler-" + output + (out ? "-out" : "-err") + "Thread") {
				public void run() {
					int bytesRead = -1;
					while(running.get()) {
						try {
							bytesRead = in.read(transfer);
							if(bytesRead == -1) break;
							bos.write(transfer, 0, bytesRead);
						} catch (IOException iex) {
							
						}
					}
				}
			};
			readerThread.setDaemon(true);
			readerThread.start();
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.utils.system.IProcessStreamHandler#onProcessEnd(java.lang.Process)
		 */
		@Override
		public void onProcessEnd(final Process process) {			
			running.set(false);
			if(readerThread!=null) {
				try { readerThread.interrupt(); } catch (Exception x) {/* No Op */}
			}
		}
		
		
		
	}
	
	private ProcessStreamHandlers() {}

}
