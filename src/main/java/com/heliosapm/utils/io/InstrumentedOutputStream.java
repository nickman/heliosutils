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

import jsr166e.LongAdder;

/**
 * <p>Title: InstrumentedOutputStream</p>
 * <p>Description: An instrumented output stream wrapper that tracks the number of bytes transferred</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.io.InstrumentedOutputStream</code></p>
 */
public class InstrumentedOutputStream extends OutputStream {
	protected final OutputStream os;	
	protected final LongAdder bytesDown;
	protected Runnable runOnClose = null;

	/**
	 * Creates a new InstrumentedOutputStream
	 * @param os The output stream to instrument
	 * @param bytesDown A delta long adder to track the byte transfers
	 * @param runOnClose An optional runnable to run on a clean close
	 */
	public InstrumentedOutputStream(final OutputStream os, final LongAdder bytesDown, final Runnable runOnClose) {
		if(os==null) throw new IllegalArgumentException("The passed OutputStream was null");
		this.os = os;
		this.runOnClose = runOnClose;
		this.bytesDown = bytesDown;
	}

	/**
	 * Returns the number of bytes transferred
	 * @return the number of bytes transferred
	 */
	public long getBytesTransferred() {
		return bytesDown.longValue();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(final int b) throws IOException {
		os.write(b);
		bytesDown.increment();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#write(byte[])
	 */
	@Override
	public void write(final byte[] b) throws IOException {		
		os.write(b);
		bytesDown.add(b.length);
	}
	
	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		os.write(b, off, len);
		bytesDown.add(len);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return os.hashCode();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return os.equals(obj);
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#flush()
	 */
	@Override
	public void flush() throws IOException {
		os.flush();
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#close()
	 */
	@Override
	public void close() throws IOException {
		IOException iex = null;
		try {
			os.close();
		} catch (IOException i) {
			iex = i;
		}
		if(runOnClose!=null) {
			final Runnable r = runOnClose;
			runOnClose = null;
			r.run();
		}
		if(iex!=null) throw iex;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return os.toString();
	}
	
	

}
