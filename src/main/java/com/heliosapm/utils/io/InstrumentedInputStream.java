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
import java.io.InputStream;

import jsr166e.LongAdder;

/**
 * <p>Title: InstrumentedInputStream</p>
 * <p>Description: An instrumented input stream wrapper that tracks the number of bytes transferred</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.io.InstrumentedInputStream</code></p>
 */
public class InstrumentedInputStream extends InputStream {
	protected final InputStream is;	
	protected final LongAdder bytesUp;
	protected Runnable runOnClose = null;

	/**
	 * Creates a new InstrumentedOutputStream
	 * @param is The input stream to instrument
	 * @param bytesUp A delta long adder to track the byte transfers
	 * @param runOnClose An optional runnable to run on a clean close
	 */
	public InstrumentedInputStream(final InputStream is, final LongAdder bytesUp, final Runnable runOnClose) {
		if(is==null) throw new IllegalArgumentException("The passed InputStream was null");
		this.is = is;
		this.runOnClose = runOnClose;
		this.bytesUp = bytesUp;
	}
	
	/**
	 * Returns the number of bytes transferred
	 * @return the number of bytes transferred
	 */
	public long getBytesTransferred() {
		return bytesUp.longValue();
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		final int i = is.read();
		bytesUp.increment();
		return i;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#read(byte[])
	 */
	@Override
	public int read(final byte[] b) throws IOException {
		final int i = is.read(b);
		bytesUp.add(i>0 ? i : 0);
		return i;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		final int i = is.read(b, off, len);
		bytesUp.add(i>0 ? i : 0);
		return i;
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return is.hashCode();
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		return is.equals(obj);
	}


	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#skip(long)
	 */
	@Override
	public long skip(long n) throws IOException {
		return is.skip(n);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return is.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#available()
	 */
	@Override
	public int available() throws IOException {
		return is.available();
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#close()
	 */
	@Override
	public void close() throws IOException {
		IOException iex = null;
		try {
			is.close();
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
	 * @see java.io.InputStream#mark(int)
	 */
	@Override
	public void mark(final int readlimit) {
		is.mark(readlimit);
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#reset()
	 */
	@Override
	public void reset() throws IOException {
		is.reset();
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#markSupported()
	 */
	@Override
	public boolean markSupported() {
		return is.markSupported();
	}


}
