/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2015, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.utils.ssh.terminal;

import java.beans.PropertyEditorManager;
import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.InteractiveCallback;

/**
 * <p>Title: AuthMethod</p>
 * <p>Description: Functional enumeration of authentication methods</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdbex.monitor.ssh.AuthMethod</code></p>
 */

public enum AuthMethod implements Authenticator {
	/** Attempt to authenticate with a username and nothing else */
	NONE("none"){
		@Override
		public boolean authenticate(final Connection conn, final ConnectInfo authInfo) throws IOException {
			if(validate(conn, authInfo)) return true;
			return conn.authenticateWithNone(authInfo.getUserName());
		}
	},
	/** Attempt to authenticate with a user name and password */
	PASSWORD("password"){
		@Override
		public boolean authenticate(final Connection conn, final ConnectInfo authInfo) throws IOException {
			if(validate(conn, authInfo)) return true;
			if(authInfo.getUserPassword()==null) return false;
			return conn.authenticateWithPassword(authInfo.getUserName(), authInfo.getUserPassword());
		}
	},
	/** Attempt to authenticate with an interactive callback */
	INTERACTIVE("keyboard-interactive"){
		@Override
		public boolean authenticate(final Connection conn, final ConnectInfo authInfo) throws IOException {
			if(validate(conn, authInfo)) return true;
			return conn.authenticateWithKeyboardInteractive(authInfo.getUserName(), iback(authInfo));
		}
	},
	/** Attempt to authenticate with a public key */
	PUBLICKEY("publickey"){
		@Override
		public boolean authenticate(final Connection conn, final ConnectInfo authInfo) throws IOException {
			if(validate(conn, authInfo)) return true;
			return conn.authenticateWithPublicKey(authInfo.getUserName(), authInfo.getPrivateKey(), authInfo.getPrivateKeyPassword());
		}
	};
	
	private AuthMethod(final String sshName) {
		this.sshName = sshName;
	}
	
	/** The SSH name of the authentication method */
	public final String sshName;
	
	/** Recognized SSH names for the auth methods */
	public static final Set<String> SSH_NAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("password", "publickey", "keyboard-interactive")));
	/** The AuthMethods keyed by the ssh name */
	public static final Map<String, AuthMethod> SSHNAME2ENUM;
	
	private static final String[] SSH_NAMES_ARR = SSH_NAMES.toArray(new String[SSH_NAMES.size()]);
	
	/**
	 * Returns the SSH names of the authentication methods
	 * @return the SSH names of the authentication methods
	 */
	public static final String[] getSSHAuthMethodNames() {
		return SSH_NAMES_ARR;
	}
	
	static {
		PropertyEditorManager.registerEditor(AuthMethod.class, AuthMethodPropertyEditor.class);
		PropertyEditorManager.registerEditor(AuthMethod[].class, AuthMethodArrPropertyEditor.class);
		final AuthMethod[] values = AuthMethod.values();
		final Map<String, AuthMethod> tmp = new HashMap<String, AuthMethod>(values.length);
		for(AuthMethod am: values) {
			tmp.put(am.sshName, am);
		}
		SSHNAME2ENUM = Collections.unmodifiableMap(tmp);
	}
	
	/**
	 * Returns an initialized set of the default available AuthMethods
	 * @return an initialized set of the default available AuthMethods
	 */
	public static Set<AuthMethod> getAvailableMethodSet() {
		return EnumSet.of(PASSWORD, PUBLICKEY);
	}
	
	/**
	 * Removes the passed AuthMethods from the passed set and returns the remaining as an array of the remaining members' ssh names
	 * @param remaining The set of remaining names
	 * @param remove The methods to remove
	 * @return A string array of the remaining auth method SSH names
	 */
	public static String[] toSSHNames(final Set<AuthMethod> remaining, final AuthMethod...remove) {
		if(remaining==null || remaining.isEmpty()) return new String[]{};
		final Set<String> set = new HashSet<String>(remaining.size());
		remaining.removeAll(new HashSet<AuthMethod>(Arrays.asList(remove)));
		if(!remaining.isEmpty()) {
			for(AuthMethod am: remaining) {
				set.add(am.sshName);
			}
		}
		return set.toArray(new String[set.size()]);
	}
	
	private static boolean validate(final Connection conn, final ConnectInfo authInfo) {
		if(conn==null) throw new IllegalArgumentException("The passed connection was null");
		if(conn.isAuthenticationComplete()) return true;
		if(authInfo==null) throw new IllegalArgumentException("The passed ConnectInfo was null");
		return false;
	}
	
	private static InteractiveCallback iback(final ConnectInfo authInfo) {
		return new InteractiveCallback() {
			@Override
			public String[] replyToChallenge(String name, String instruction,
					int numPrompts, String[] prompt, boolean[] echo)
					throws Exception {
					return new String[]{authInfo.getUserPassword()};
			}
		};
	}
	
	/**
	 * <p>Title: AuthMethodPropertyEditor</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.ssh.terminal.AuthMethod.AuthMethodPropertyEditor</code></p>
	 */
	public static class AuthMethodPropertyEditor extends PropertyEditorSupport {
		/**
		 * {@inheritDoc}
		 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
		 */
		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			setValue(AuthMethod.fromName(text));
			
		}
	}
	
	/**
	 * <p>Title: AuthMethodArrPropertyEditor</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.ssh.terminal.AuthMethod.AuthMethodArrPropertyEditor</code></p>
	 */
	public static class AuthMethodArrPropertyEditor extends PropertyEditorSupport {
		/**
		 * {@inheritDoc}
		 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
		 */
		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			setValue(AuthMethod.decode(text));
			
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.beans.PropertyEditorSupport#getAsText()
		 */
		@Override
		public String getAsText() {			
			return Arrays.toString((AuthMethod[])getValue());
		}
	}
	
	
	/**
	 * Decodes the passed comma delimited phrase into an array of AuthMethods
	 * @param phrase A comma delimited string of auth method names
	 * @return an array of AuthMethods
	 */
	public static AuthMethod[] decode(final String phrase) {
		final EnumSet<AuthMethod> set = EnumSet.noneOf(AuthMethod.class);
		for(String s: phrase.split(",")) {
			if(s==null || s.trim().isEmpty()) continue;
			set.add(fromName(s));
		}
		return set.toArray(new AuthMethod[set.size()]);
	}
	
	/**
	 * Returns the auth method from the passed trimmed and upper cased phrase
	 * @param phrase
	 * @return the AuthMethod
	 */
	public static AuthMethod fromName(final String phrase) {
		if(phrase==null || phrase.trim().isEmpty()) throw new IllegalArgumentException("The passed phrase was null or empty");
		try {
			return AuthMethod.valueOf(phrase.trim().toUpperCase());
		} catch (Exception ex) {
			throw new IllegalArgumentException("The passed phrase [" + phrase + "] could not be decoded to a valid AuthMethod");
		}
	}
	
}
