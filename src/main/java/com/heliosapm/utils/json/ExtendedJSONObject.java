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
package com.heliosapm.utils.json;

import static com.heliosapm.utils.config.TokenAwareProperties.token;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * <p>Title: ExtendedJSONObject</p>
 * <p>Description: Various extensions to {@link JSONObject}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.json.ExtendedJSONObject</code></p>
 */

public class ExtendedJSONObject extends JSONObject {

	/**
	 * Creates a new ExtendedJSONObject
	 */
	public ExtendedJSONObject() {
	}

	/**
	 * Creates a new ExtendedJSONObject
	 * @param x A JSONTokener object containing the source string
	 * @throws JSONException If any JSONExceptions are detected
	 */
	public ExtendedJSONObject(final JSONTokener x) throws JSONException {
		super(x);
	}

	/**
	 * Creates a new ExtendedJSONObject
	 * @param map  A map object that can be used to initialize the contents of the JSONObject
	 */
	@SuppressWarnings("rawtypes")
	public ExtendedJSONObject(final Map map) {
		super(map);
	}

	/**
	 * Creates a new ExtendedJSONObject
	 * @param bean An object that has getter methods that should be used to make a JSONObject
	 */
	public ExtendedJSONObject(final Object bean) {
		super(bean);
	}

	/**
	 * Creates a new ExtendedJSONObject
	 * @param source  A string beginning with { (left brace) and ending with }  (right brace).
	 * @throws JSONException If any JSONExceptions are detected
	 */
	public ExtendedJSONObject(final String source) throws JSONException {
		super(source);
	}

	/**
	 * Creates a new ExtendedJSONObject
	 * @param jo A JSONObject
	 * @param names An array of strings
	 */
	public ExtendedJSONObject(final JSONObject jo, final String[] names) {
		super(jo, names);
	}
	
	/**
	 * Returns the keys for the passed JSONObject as an array
	 * @param jo The JSONObject
	 * @return the keys
	 */
	public static String[] keys(final JSONObject jo) {
		if(jo==null) throw new IllegalArgumentException("The passed JSONObject was null");
		final String[] k = new String[jo.keySet().size()];
		int cnt = 0;
		for(Iterator<Object> iter = jo.keys(); iter.hasNext(); cnt++) {
			k[cnt] = iter.next().toString();
		}
		return k;
	}
	
	/**
	 * Creates a new ExtendedJSONObject
	 * @param jo A JSONObject
	 */
	public ExtendedJSONObject(final JSONObject jo) {
		this(jo, keys(jo));
	}
	

	/**
	 * Creates a new ExtendedJSONObject
	 * @param object An object that has fields that should be used to make a JSONObject
	 * @param names An array of strings, the names of the fields to be obtained from the object
	 */
	public ExtendedJSONObject(final Object object, final String[] names) {
		super(object, names);
	}

	/**
	 * Creates a new ExtendedJSONObject
	 * @param baseName The ResourceBundle base name
	 * @param locale The Locale to load the ResourceBundle for
	 * @throws JSONException If any JSONExceptions are detected
	 */
	public ExtendedJSONObject(final String baseName, final Locale locale) throws JSONException {
		super(baseName, locale);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.json.JSONObject#optString(java.lang.String)
	 */
	@Override
	public String optString(final String key) {
		return token(super.optString(key));
	}

	/**
	 * {@inheritDoc}
	 * @see org.json.JSONObject#optString(java.lang.String, java.lang.String)
	 */
	@Override
	public String optString(final String key, final String defaultValue) {		
		return token(super.optString(key, defaultValue));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.json.JSONObject#getString(java.lang.String)
	 */
	@Override
	public String getString(String key) throws JSONException {		
		return token(super.getString(key));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.json.JSONObject#getJSONObject(java.lang.String)
	 */
	@Override
	public ExtendedJSONObject getJSONObject(final String key) throws JSONException {
		return new ExtendedJSONObject(super.getJSONObject(key));
	}
	
	
}
