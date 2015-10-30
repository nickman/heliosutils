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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: ExternalizableMap</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.bulk.ExternalizableMap</code></p>
 */

public class ExternalizableMap extends HashMap<String, Object> implements Externalizable {

	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {		
		final HashMap<String, Object> tmp = new HashMap<String, Object>(this); 
		out.writeInt(tmp.size());
		if(!tmp.isEmpty()) {
			for(Map.Entry<String, Object> entry: tmp.entrySet()) {
				try {
					final byte[] bytes = serialize(entry.getValue());
					out.writeUTF(entry.getKey());
					out.writeInt(bytes.length);
					out.write(bytes);										
				} catch (Exception x) {/* No Op */}
			}
			tmp.clear();
		}		
	}
	
	@Override
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		final int size = in.readInt();
		for(int x = 0; x < size; x++) {
			try {
				String key = in.readUTF();
				int len = in.readInt();
				byte[] bytes = new byte[len];
				in.read(bytes);
				final Object o = deserialize(bytes);
				put(key, o);
			} catch (Exception ex) {/* No Op */}
		}
		
	}
	
	
	private static byte[] serialize(final Object obj) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(baos);
			if(obj instanceof Externalizable) {
				((Externalizable)obj).writeExternal(oos);
			} else {
				oos.writeObject(obj);
			}
			oos.flush();
			baos.flush();
			return baos.toByteArray();
		} finally {
			try { oos.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	private static Object deserialize(final byte[] bytes) throws IOException, ClassNotFoundException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(bais);
			return ois.readObject();
		} finally {
			try { ois.close(); } catch (Exception x) {/* No Op */}
		}
		
	}


	
}
