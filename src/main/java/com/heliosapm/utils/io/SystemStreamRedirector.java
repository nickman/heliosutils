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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Title: SystemStreamRedirector</p>
 * <p>Description: JVM service to manage the system stream definition on a per thread basis</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.io.SystemStreamRedirector</code></p>
 */

public class SystemStreamRedirector extends PrintStream {
	private static final PrintStream SYSTEM_OUT = System.out;
	private static final PrintStream SYSTEM_ERR = System.err;
	private static final SystemStreamRedirector outRedirector = new SystemStreamRedirector(SYSTEM_OUT, true);
	private static final SystemStreamRedirector errRedirector = new SystemStreamRedirector(SYSTEM_ERR, false);
	private static InheritableThreadLocal<PrintStream> setOutStream = new InheritableThreadLocal<PrintStream>() {
		@Override
		protected PrintStream initialValue() {
			return SYSTEM_OUT; 
		}
	};	
	private static InheritableThreadLocal<PrintStream> setErrStream = new InheritableThreadLocal<PrintStream>() {
		@Override
		protected PrintStream initialValue() {
			return SYSTEM_ERR; 
		}
	};
	
	/** Indicates if this stream replaces StdOut(true) or StdErr(false) */
	private final boolean isStdOut; 
	
	/** Indicates if the system redirector is globally installed */
	private static final AtomicBoolean installed = new AtomicBoolean(false);
	
	/**
	 * Installs the global system redirector if it is not installed already
	 */
	public static void install() {
		if(!installed.get()) {
			System.setOut(outRedirector);
			System.setErr(errRedirector);
			installed.set(true);
		} 
	}
	/**
	 * Uninstalls the global system redirector if it is installed 
	 */	
	public static void uninstall() {
		if(installed.get()) {
			System.setOut(SYSTEM_OUT);
			System.setErr(SYSTEM_ERR);
			installed.set(false);
		} 
	}
	
	/**
	 * Will the real System.out please stand up
	 * @return the real System.out 
	 */
	public static PrintStream out() {
		return SYSTEM_OUT;
	}
	
	/**
	 * Will the real System.err please stand up
	 * @return the real System.err 
	 */
	public static PrintStream err() {
		return SYSTEM_ERR;
	}
	
	
	/**
	 * Determines if either stdout or stderr are redirected
	 * @return true if stdout or stderr is redirected, false otherwise
	 */
	public static boolean isInstalledOnCurrentThread() {
		return (
				!setOutStream.get().equals(SYSTEM_OUT) ||
				!setErrStream.get().equals(SYSTEM_ERR)
		);
	}
	
	/**
	 * Redirects the out and error streams for the current thread
	 * @param outPs The output stream redirect for Standard Out
	 * @param errPs The output stream redirect for Standard Err
	 */
	public static void set(PrintStream outPs, PrintStream errPs) {
		if(outPs==null) {
			throw new RuntimeException("Out PrintStream was null");
		}
		if(errPs==null) {
			errPs = outPs;
		}
		if(!installed.get()) {
			throw new RuntimeException("The SystemRedirector is not installed");			
		} else {
			setOutStream.set(outPs);
			setErrStream.set(errPs);
		}
	}
	
	public static final OutputStream NOOP_OUT = new NoopOutputStream();
	private static class NoopOutputStream extends OutputStream {

		@Override
		public void write(final int b) throws IOException {
			/* No Op */
		}
		
	}
	
	/**
	 * Redirects the out and error streams for the current thread
	 * @param outPs The output stream redirect for Standard Out
	 * @param errPs The output stream redirect for Standard Err
	 */
	public static void set(OutputStream out, OutputStream err) {
		if(out==null) {
			throw new RuntimeException("Out OutputStream was null");
		}
		if(err==null) {
			err = out;
		}
		if(!installed.get()) {
			throw new RuntimeException("The SystemRedirector is not installed");			
		} else {
			setOutStream.set(new PrintStream(out, true));
			setErrStream.set(new PrintStream(err, true));
		}
	}	
	
	public static void setNoop() {
		set(NOOP_OUT, NOOP_OUT);
	}
	
	/**
	 * Redirects the out and error streams for the current thread to the same stream
	 * @param ps The output stream redirect for Standard Out and Standard Err
	 */
	public static void set(PrintStream ps) {
		if(ps==null) {
			throw new RuntimeException("Out/Err PrintStream was null");
		}
		if(!installed.get()) {
			throw new RuntimeException("The SystemRedirector is not installed");			
		} else {
			setOutStream.set(ps);
			setErrStream.set(ps);
		}
	}
	
	/**
	 * Redirects the out and error streams for the current thread to the same stream
	 * @param ps The output stream redirect for Standard Out and Standard Err
	 */
	public static void set(OutputStream out) {
		if(out==null) {
			throw new RuntimeException("Out/Err OutputStream was null");
		}
		if(!installed.get()) {
			throw new RuntimeException("The SystemRedirector is not installed");			
		} else {
			setOutStream.set(new PrintStream(out, true));
			setErrStream.set(new PrintStream(out, true));
		}
	}
	
	
	
	/**
	 * Resets the out and error streams for the current thread to the default
	 */
	public static void reset() {
		setOutStream.set(SYSTEM_OUT);
		setErrStream.set(SYSTEM_ERR);		
	}
	
	
	
	/**
	 * Returns the correct out or err stream for this redirector.
	 * @return a printstream
	 */
	private PrintStream getPrintStream() {
		return isStdOut ? setOutStream.get() : setErrStream.get();
	}

	/**
	 * Creates new SystemStreamRedirector
	 * @param ps The default output this redirector replaces
	 * @param isStdOut true if this stream replaces System.out, false if it replaces System.err
	 */
	private SystemStreamRedirector(PrintStream ps, boolean isStdOut) {
		super(ps);
		this.isStdOut = isStdOut;
	}
	
	@Override
	public int hashCode() {
		return getPrintStream().hashCode();
	}
	@Override
	public void write(byte[] b) throws IOException {
		getPrintStream().write(b);
	}
	@Override
	public boolean equals(Object obj) {
		return getPrintStream().equals(obj);
	}
	@Override
	public String toString() {
		return getPrintStream().toString();
	}
	@Override
	public void flush() {
		getPrintStream().flush();
	}
	@Override
	public void close() {
		getPrintStream().close();
	}
	@Override
	public boolean checkError() {
		return getPrintStream().checkError();
	}
	@Override
	public void write(int b) {
		getPrintStream().write(b);
	}
	@Override
	public void write(byte[] buf, int off, int len) {
		getPrintStream().write(buf, off, len);
	}
	@Override
	public void print(boolean b) {
		getPrintStream().print(b);
	}
	@Override
	public void print(char c) {
		getPrintStream().print(c);
	}
	@Override
	public void print(int i) {
		getPrintStream().print(i);
	}
	@Override
	public void print(long l) {
		getPrintStream().print(l);
	}
	@Override
	public void print(float f) {
		getPrintStream().print(f);
	}
	@Override
	public void print(double d) {
		getPrintStream().print(d);
	}
	@Override
	public void print(char[] s) {
		getPrintStream().print(s);
	}
	@Override
	public void print(String s) {
		getPrintStream().print(s);
	}
	@Override
	public void print(Object obj) {
		getPrintStream().print(obj);
	}
	@Override
	public void println() {
		getPrintStream().println();
	}
	@Override
	public void println(boolean x) {
		getPrintStream().println(x);
	}
	@Override
	public void println(char x) {
		getPrintStream().println(x);
	}
	@Override
	public void println(int x) {
		getPrintStream().println(x);
	}
	@Override
	public void println(long x) {
		getPrintStream().println(x);
	}
	@Override
	public void println(float x) {
		getPrintStream().println(x);
	}
	@Override
	public void println(double x) {
		getPrintStream().println(x);
	}
	@Override
	public void println(char[] x) {
		getPrintStream().println(x);
	}
	@Override
	public void println(String x) {
		getPrintStream().println(x);
	}
	@Override
	public void println(Object x) {
		getPrintStream().println(x);
	}
	@Override
	public PrintStream printf(String format, Object... args) {
		return getPrintStream().printf(format, args);
	}
	@Override
	public PrintStream printf(Locale l, String format, Object... args) {
		return getPrintStream().printf(l, format, args);
	}
	@Override
	public PrintStream format(String format, Object... args) {
		return getPrintStream().format(format, args);
	}
	@Override
	public PrintStream format(Locale l, String format, Object... args) {
		return getPrintStream().format(l, format, args);
	}
	@Override
	public PrintStream append(CharSequence csq) {
		return getPrintStream().append(csq);
	}
	@Override
	public PrintStream append(CharSequence csq, int start, int end) {
		return getPrintStream().append(csq, start, end);
	}
	@Override
	public PrintStream append(char c) {
		return getPrintStream().append(c);
	}
	

}

