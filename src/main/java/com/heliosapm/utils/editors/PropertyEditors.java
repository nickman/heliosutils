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
package com.heliosapm.utils.editors;

import java.beans.PropertyEditorManager;
import java.beans.PropertyEditorSupport;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: PropertyEditors</p>
 * <p>Description: A bunch of useful property editors</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.editors.PropertyEditors</code></p>
 */

public class PropertyEditors {
	
	/** Comma splitter pattern */
	public static final Pattern COMMA_SPLITTER = Pattern.compile(",");
	
	/**
	 * <p>Title: URLEditor</p>
	 * <p>Description: A property editor for {@link URL}s</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.editors.PropertyEditors.URLEditor</code></p>
	 */
	public static class URLEditor extends PropertyEditorSupport {
		/**
		 * {@inheritDoc}
		 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
		 */
		@Override
		public void setAsText(final String text) throws IllegalArgumentException {
			try {
				setValue(new URL(text));
			} catch (Exception ex) {
				throw new RuntimeException("Failed to convert text [" + text + "] to a URL");
			}
		}		
	}
	
	/**
	 * <p>Title: URLEditor</p>
	 * <p>Description: A property editor for {@link URL} arrays</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.editors.PropertyEditors.URLArrayEditor</code></p>
	 */
	public static class URLArrayEditor extends PropertyEditorSupport {
		/**
		 * {@inheritDoc}
		 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
		 */
		@Override
		public void setAsText(final String text) throws IllegalArgumentException {
			try {				
				final String[] arr = COMMA_SPLITTER.split(text.trim());
				final List<URL> urlList = new ArrayList<URL>(arr.length);
				for(String s: arr) {
					if(s==null || s.trim().isEmpty()) continue;
					urlList.add(URLHelper.toURL(s.trim()));
				}
				setValue(urlList.toArray(new URL[0]));
			} catch (Exception ex) {
				throw new RuntimeException("Failed to convert text [" + text + "] to a URL array");
			}
		}		
		
		/**
		 * {@inheritDoc}
		 * @see java.beans.PropertyEditorSupport#getAsText()
		 */
		@Override
		public String getAsText() {			
			return Arrays.toString((URL[])getValue());
		}
	}
	
	
	static {
		PropertyEditorManager.registerEditor(URL.class, URLEditor.class);
		PropertyEditorManager.registerEditor(URL[].class, URLArrayEditor.class);
	}


}
