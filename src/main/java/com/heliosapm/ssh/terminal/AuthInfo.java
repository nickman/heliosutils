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
package com.heliosapm.ssh.terminal;

import java.io.File;
import java.util.Arrays;

import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: AuthInfo</p>
 * <p>Description: Bean construct to wrap authentication credentials</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdbex.monitor.ssh.AuthInfo</code></p>
 */

public class AuthInfo {
	/** The authentication user name */
	private final String userName;
	/** The authentication user password */
	private String userPassword = null;
	/** The authentication private key char array */
	private char[] privateKey = null;
	/** The authentication private key passphrase */
	private String privateKeyPassword = null;
	/** The authentication methods in the order they should be attempted */
	private AuthMethod[] authMethods = AuthMethod.values();
	
	


	/**
	 * Creates a new AuthInfo
	 */
	public AuthInfo() {
		this(System.getProperty("user.name"));
	}
	
	/**
	 * Creates a new AuthInfo
	 * @param userName The user name
	 */
	public AuthInfo(final String userName) {
		if(userName==null) throw new IllegalArgumentException("The passed user name was null");
		this.userName = userName.trim();
	}
	
	// ======================================================================
	// Setters
	// ======================================================================

	/**
	 * Sets the user password
	 * @param userPassword the userPassword to set
	 * @return this AuthInfo
	 */
	public AuthInfo setUserPassword(final String userPassword) {
		if(userPassword==null) throw new IllegalArgumentException("The passed user password was null");
		this.userPassword = userPassword.trim();
		return this;
	}

	/**
	 * Sets the private key
	 * @param privateKey the privateKey to set
	 * @return this AuthInfo
	 */
	public AuthInfo setPrivateKey(final char[] privateKey) {
		if(privateKey==null) throw new IllegalArgumentException("The passed privateKey was null");
		this.privateKey = privateKey;
		return this;
	}
	
	/**
	 * Sets the private key
	 * @param privateKey the privateKey to set
	 * @return this AuthInfo
	 */
	public AuthInfo setPrivateKey(final String privateKey) {
		if(privateKey==null) throw new IllegalArgumentException("The passed privateKey was null");
		this.privateKey = privateKey.toCharArray();
		return this;		
	}
	
	/**
	 * Sets the private key
	 * @param privateKey the privateKey to set
	 * @return this AuthInfo
	 */
	public AuthInfo setPrivateKey(final File privateKey) {
		if(privateKey==null) throw new IllegalArgumentException("The passed privateKey was null");
		if(privateKey.canRead()) throw new IllegalArgumentException("The passed privateKey file [" + privateKey.getAbsolutePath() + "] cannot be read");
		this.privateKey = URLHelper.getCharsFromURL(privateKey.getAbsolutePath());
		return this;		
	}

	/**
	 * Sets the private key pass phrase
	 * @param privateKeyPassword the privateKeyPassword to set
	 * @return this AuthInfo
	 */
	public AuthInfo setPrivateKeyPassword(final String privateKeyPassword) {
		if(privateKeyPassword==null) throw new IllegalArgumentException("The passed privateKeyPassword was null");
		this.privateKeyPassword = privateKeyPassword;
		return this;
	}
	
	/**
	 * Sets the authentication methods 
	 * @param authMethods the authMethods to set
	 * @return this AuthInfo
	 */
	public AuthInfo setAuthMethods(AuthMethod... authMethods) {
		this.authMethods = authMethods;
		return this;
	}
	
	// ======================================================================
	// Getters
	// ======================================================================


	/**
	 * Returns the user name
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder(userName).append("[");
		b.append("pass:").append((userPassword==null || userPassword.isEmpty()) ? false : true).append(", ");
		b.append("key:").append((privateKey==null) ? false : true).append(", ");
		b.append("keypass:").append((privateKeyPassword==null || privateKeyPassword.isEmpty()) ? false : true).append(", ");		
		b.append("authMethods:").append(Arrays.toString(authMethods));		
		return b.append("]").toString();
	}

	/**
	 * Returns the user password
	 * @return the userPassword
	 */
	public String getUserPassword() {
		return userPassword;
	}

	/**
	 * Returns the private key
	 * @return the privateKey
	 */
	public char[] getPrivateKey() {
		return privateKey;
	}

	/**
	 * Returns the private key password
	 * @return the privateKeyPassword
	 */
	public String getPrivateKeyPassword() {
		return privateKeyPassword;
	}
	
	/**
	 * Returns the enabled auth methods
	 * @return the authMethods
	 */
	public AuthMethod[] getAuthMethods() {
		return authMethods.clone();
	}
	

}
