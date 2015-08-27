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
package test.com.heliosapm.utils.ssh.terminal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import ch.ethz.ssh2.ServerSession;

/**
 * <p>Title: JavaScriptShell</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.com.heliosapm.utils.ssh.terminal.JavaScriptShell</code></p>
 */

public class JavaScriptShell implements Runnable, Closeable {
	/** The script engine manager */
	protected final ScriptEngineManager sem = new ScriptEngineManager();
	/** The script engine */
	protected final ScriptEngine se = sem.getEngineByExtension("js");
	/** The script engine context */
	protected final ScriptContext context = se.getContext();
	/** The current code buffer */
	protected final StringBuilder codeBuffer = new StringBuilder();
	/** The current line buffer */
	protected final StringBuilder codeLine = new StringBuilder();
	
	/** The server session */
	protected final ServerSession ss;
	
	/** The response writer */
	protected final PrintWriter writer;
	/** The response error writer */
	protected final PrintWriter ewriter;
	/** The input stream reader */
	protected final Reader reader;
	/** The input stream */
	protected final InputStream is;
	/** The output stream */
	protected final OutputStream os;
	
	/** The thread wot's doing all this */
	protected Thread execThread = null;
	
	/** FLag to indicate of the shell is still running */
	protected final AtomicBoolean running = new AtomicBoolean(true);
	
	/** The delimiter for end-of-script and execute */
	public static final int EXEC_DELIM = '/';
	/** The delimiter to clear the buffer */
	public final static int CLEAR = 'c';
	/** Line feed char */
	public final static int LF = 13;
	/** Exit delimiter */
	public final static int EXIT = 'x';
	/** backspace delimiter */
	public final static int BACK = 8;

	
	/**
	 * Creates a new JavaScriptShell
	 * @param ss The server session
	 */
	public JavaScriptShell(final ServerSession ss) {
		is = ss.getStdout();
		os = ss.getStdin();
		this.ss = ss;
		writer = new PrintWriter(new OutputStreamWriter(os, Charset.forName("UTF8")));
		ewriter = new PrintWriter(new OutputStreamWriter(os, Charset.forName("UTF8")));
		reader = new InputStreamReader(is, Charset.forName("UTF8"));
		context.setWriter(writer);
		context.setErrorWriter(writer);
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.Closeable#close()
	 */
	public void close() {
		writer.println("Exiting....");
		of();
		try { os.flush(); } catch (Exception x) {/* No Op */}
		try { ss.close(); } catch (Exception x) {/* No Op */}		
		try { is.close(); } catch (Exception x) {/* No Op */}
		try { os.close(); } catch (Exception x) {/* No Op */}		
		running.set(false);
	}

	public void run() {
		execThread = Thread.currentThread();
		out("Starting JavaScriptShell on thread [" + execThread + "]");
		main:
		while(running.get()) {
			try {
				final int c = is.read();			
				System.out.println("c:\t[" + c + "/" + ((char)c) + "]");
				switch(c) {
					case -1:
					case EXIT:	
						out("Exiting...");
						break main;
					case CLEAR:
						clear();
						break;
					case 13:
						nextln();
						break;
					case EXEC_DELIM:
						exec();
						break;
					default:
						ac(c);
						
				}
			} catch (IOException iex) {
				eout("Read failure", iex);
			}
		}
		close();
	}
	
	private void ac(final int c) {
		codeLine.append((char)c);
		cout(c);
	}
	
	private void clear() {
		if(codeLine.length()>0) {
			ac(CLEAR);
		} else {
			rb();	
			cout(LF);
		}
	}

	
	private void exec() {
		if(codeLine.length()>0) {
			ac(EXEC_DELIM);
		} else {
			if(codeBuffer.length()==0) {
				eout("Empty Code Buffer");
				return;
			}
			try {
				final Object result = se.eval(codeBuffer.toString());
				out(">>> Result:" + result);
			} catch (Exception ex) {
				eout("Failed to execute script", ex);
				ex.printStackTrace(System.err);
			} 
		}
	}
	
	private void nextln() {
		ac(LF);
		codeBuffer.append(codeLine.toString());
		rl();
		cout(LF);
	}
	
	private void out(final String s) {
		writer.println(s);
		of();
	}
	
	private void cout(final int s) {
		writer.print((char)s);
		if(s==LF) writer.print((char)10);
		of();
	}
	
	
	private void eout(final Object s) {
		eout(s, null);
	}
	
	private void eout(final Object s, final Throwable t) {
		writer.println(s);
		if(t!=null) {
			writer.println("Stack trace follows:");
			t.printStackTrace(ewriter);
			writer.println();
		}		
		of();
	}
	
	private void of() {
		ewriter.flush();
		writer.flush();
		try { os.flush(); } catch (Exception x) {/* No Op */}
	}
	
	
	private void rl() {
		codeLine.setLength(0);
		System.out.println("Cleared Code Line");
	}
	
	private void rb() {
		codeBuffer.setLength(0);
		System.out.println("Cleared Code Buffer");
	}
}
