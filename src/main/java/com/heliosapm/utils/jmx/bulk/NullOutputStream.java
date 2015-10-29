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
package com.heliosapm.utils.jmx.bulk;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>Title: NullOutputStream</p>
 * <p>Description: OutputStream to nowhere</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.bulk.NullOutputStream</code></p>
 */

public class NullOutputStream extends OutputStream {
	/** Shareable instance */
	public static final NullOutputStream INSTANCE = new NullOutputStream();
	
	/**
	 * Creates a new NullOutputStream
	 */
	public NullOutputStream() {
	
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(final int b) throws IOException {
		/* No Op */
	}
	
	@Override
	public void write(final byte[] b) throws IOException {
		/* No Op */
	}
	
	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		/* No Op */
	}

}
