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
package com.heliosapm.utils.jndi;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * <p>Title: JNDIHelper</p>
 * <p>Description: Static JNDI helper methods</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jndi.JNDIHelper</code></p>
 */

public abstract class JNDIHelper {
	
	/**
	 * Looks up a value in JNDI, returning that value or null
	 * @param ctx The context to lookup in
	 * @param name The name to lookup
	 * @return The looked up value or null if none was found or an error was thrown
	 * @param <T> The expected type of the return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T lookup(final Context ctx, final String name) {
		if(ctx==null) throw new IllegalArgumentException("The passed context was null");
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		try {
			return (T)ctx.lookup(name);
		} catch (Exception ex) {
			return null;
		}
	}
	
	/**
	 * Binds the passed object into JNDI
	 * @param ctx The root context
	 * @param name The name to bind to (compound supported)
	 * @param value The value to bind
	 * @param overwrite true to overwrite any bindings in the way, false to error out
	 */
	public static void bind(final Context ctx, final String name, final Object value, final boolean overwrite) {
		if(ctx==null) throw new IllegalArgumentException("The passed context was null");
		if(value==null) throw new IllegalArgumentException("The passed value was null");
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		prod(ctx);
		Context currentContext = ctx;
		final String[] allNames = splitName(ctx, name);
		if(allNames.length==0) throw new RuntimeException("Failed to split name [" + name + "]");
		try {
			if(allNames.length==1) {
				ctx.bind(allNames[0], value);
				return;
			}
			final String[] frags = new String[allNames.length-1];
			System.arraycopy(allNames, 0, frags, 0, allNames.length-1);
			for(String frag: frags) {
				final Object obj = lookup(currentContext, frag);
				if(obj==null) {
					currentContext = currentContext.createSubcontext(frag);
					continue;
				}
				if(obj instanceof Context) {
					currentContext = (Context)obj;
					continue;						
				}
				if(overwrite) {
					currentContext.rebind(name, obj);
				}
				throw new RuntimeException("Object already bound at [" + frag + "]");				
			}
			final String lastBind = allNames[allNames.length-1]; 
			final Object obj = lookup(currentContext, lastBind);
			
			if(obj!=null) {
				if(!overwrite) throw new RuntimeException("Object already bound at [" + lastBind + "]");
				currentContext.unbind(lastBind);
			}
			currentContext.bind(allNames[allNames.length-1], value);
			return;
			
		} catch (RuntimeException rex) {
			throw rex;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to bind value to [" + name + "]", ex);
		}		
	}
	
	/**
	 * Parses the passed name into an array of sub-names
	 * @param name The name to split
	 * @return an array of the split names 
	 */
	public static String[] splitName(final Name name) {
		if(name==null) throw new IllegalArgumentException("The passed name was null");
		final List<String> names = new ArrayList<String>(4);
		for(final Enumeration<String> nenum = name.getAll(); nenum.hasMoreElements();) {
			names.add(nenum.nextElement());
		}
		return names.toArray(new String[names.size()]);
	}

	/**
	 * Parses the passed compound name into an array of sub-names
	 * @param ctx The context
	 * @param name The name to split
	 * @return an array of the split names 
	 */
	public static String[] splitName(final Context ctx, final String name) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		if(ctx==null) throw new IllegalArgumentException("The passed context was null");
		prod(ctx);
		try {
			return splitName(ctx.getNameParser("").parse(name));
		} catch (NamingException ne) {
			throw new RuntimeException("Failed to split name [" + name + "]", ne);
		}
	}

	public static void prod(final Context ctx) {
		if(ctx!=null) {
			try { ctx.lookup(""); } catch (Exception ex) {}
		}
	}

}
