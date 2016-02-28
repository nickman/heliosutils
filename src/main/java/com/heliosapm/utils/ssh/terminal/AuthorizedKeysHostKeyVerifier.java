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
package com.heliosapm.utils.ssh.terminal;

import java.io.File;

import com.heliosapm.utils.config.ConfigurationHelper;

import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.ServerHostKeyVerifier;

/**
 * <p>Title: AuthorizedKeysHostKeyVerifier</p>
 * <p>Description: A host key verifier that automatically loads <b><code></code></b> files and can load additional files in addition</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ssh.terminal.AuthorizedKeysHostKeyVerifier</code></p>
 */

public class AuthorizedKeysHostKeyVerifier implements ServerHostKeyVerifier {
	/** The known hosts DB */
	private final KnownHosts knownHosts = new KnownHosts();
	/** Indicates if changed host keys should be accepted */
	private boolean acceptChangedKeys = false;
	
	/** The system property name that optionally specifies if changed keys should be accepted */
	public static final String KNOWN_HOSTS_ADD_CHANGED = "com.heliosapm.utils.ssh.knownhosts.addchanged";
	/** The system property name that optionally references a configured comma separated string of know host files */
	public static final String KNOWN_HOSTS_PROP_FILES = "com.heliosapm.utils.ssh.knownhosts.files";
	/** A default known hosts file */
	public static final String DEFAULT_KNOWN_HOSTS = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "authorized_keys";
	/** Another default known hosts file */
	public static final String DEFAULT_KNOWN_HOSTS2 = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "authorized_keys2";	

	/**
	 * Creates a new AuthorizedKeysHostKeyVerifier
	 * @param acceptChangedKeys Indicates if changed host keys should be accepted
	 * @param fileNames Known hosts files to add
	 */
	public AuthorizedKeysHostKeyVerifier(final boolean acceptChangedKeys, String...fileNames) {
		this.acceptChangedKeys = acceptChangedKeys;
		addKnownHostsFile(DEFAULT_KNOWN_HOSTS);
		addKnownHostsFile(DEFAULT_KNOWN_HOSTS2);
		for(String s: fileNames) {
			addKnownHostsFile(s);
		}
		loadSysPropDefinedFiles();
	}
	
	/**
	 * Creates a new AuthorizedKeysHostKeyVerifier
	 * @param fileNames Known hosts files to add
	 */
	public AuthorizedKeysHostKeyVerifier(String...fileNames) {
		this(lookupAccept(), fileNames);
	}
	
	private static boolean lookupAccept() {
		return ConfigurationHelper.getBooleanSystemThenEnvProperty(KNOWN_HOSTS_ADD_CHANGED, false);
	}
	
	private void loadSysPropDefinedFiles() {
		final String[] fileNames = ConfigurationHelper.getArraySystemThenEnvProperty(KNOWN_HOSTS_PROP_FILES, (String[])null);
		if(fileNames != null) {
			for(String s: fileNames) {
				addKnownHostsFile(s);
			}			
		}
	}
	
	/**
	 * Adds a new known host file 
	 * @param fileName The file name
	 * @return true if successfully added, false otherwise
	 */
	public boolean addKnownHostsFile(final String fileName) {
		if(fileName==null || fileName.trim().isEmpty()) return false;
		final File f = new File(fileName.trim());
		if(!f.canRead()) return false;
		try {
			knownHosts.addHostkeys(f);
			return true;
		} catch (Exception x) {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.ServerHostKeyVerifier#verifyServerHostKey(java.lang.String, int, java.lang.String, byte[])
	 */
	@Override
	public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws Exception {
		final int result = knownHosts.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);
		final boolean accept;
		if(result == KnownHosts.HOSTKEY_IS_OK) accept = true;
		else if(result == KnownHosts.HOSTKEY_IS_NEW) accept = false;
		else {
			accept = acceptChangedKeys;
			if(accept) {
				knownHosts.addHostkey(new String[]{hostname}, serverHostKeyAlgorithm, serverHostKey);
			}
		}
		return accept;		
	}

	/**
	 * Indicates if changed host keys should be accepted 
	 * @return true if changed host keys should be accepted, false otherwise
	 */
	public boolean isAcceptChangedKeys() {
		return acceptChangedKeys;
	}

	/**
	 * Sets if changed host keys should be accepted 
	 * @param acceptChangedKeys true if changed host keys should be accepted, false otherwise
	 */
	public void setAcceptChangedKeys(final boolean acceptChangedKeys) {
		this.acceptChangedKeys = acceptChangedKeys;
	}

}
