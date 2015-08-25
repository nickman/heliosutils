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
package com.heliosapm.phoenix.udf.json;

import java.nio.charset.Charset;
import java.util.List;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>Title: JsonNodeResultWriter</p>
 * <p>Description: Defines the JsonNode result result writer for a given json node type</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.phoenix.udf.json.JsonNodeResultWriter</code></p>
 */

public interface JsonNodeResultWriter {
	
	/** UTF8 Character Set */
	public static final Charset UTF8 = Charset.forName("UTF8");

	/**
	 * Handles the writing of a JsonNode result to the UDF output
	 * @param node The node to write to the UDF response stream
	 * @param ptr The writable to wwrite the response to
	 * @param args The UDF arguments
	 * @return the UDF evaluate response
	 */
	public boolean writeResult(final JsonNode node, final ImmutableBytesWritable ptr, final List<Object> args);
}
