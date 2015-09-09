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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Pattern;

import javax.management.ObjectName;

import ch.ethz.ssh2.auth.AgentIdentity;
import ch.ethz.ssh2.auth.AgentProxy;

import com.heliosapm.utils.config.ConfigurationHelper;
import com.heliosapm.utils.io.CloseListener;
import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.jmx.JMXManagedScheduler;
import com.heliosapm.utils.jmx.JMXManagedThreadPool;
import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: SSHService</p>
 * <p>Description: Convenience and configuration wrapper for acquiring wrapped connections</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ssh.terminal.SSHService</code></p>
 */

public class SSHService implements AgentProxy, CloseListener<WrappedConnection> {
	/** The singleton instance */
	private static volatile SSHService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The configuration key for the SSHService connect timeout (in ms) */
	public static final String PROP_SSH_CONNECT_TIMEOUT = "ssh.connect.timeout";
	/** The default SSHService connect timeout */
	public static final int DEFAULT_SSH_CONNECT_TIMEOUT = 2000;
	/** The configuration key for the SSHService read timeout (in ms) */
	public static final String PROP_SSH_READ_TIMEOUT = "ssh.read.timeout";
	/** The default SSHService read timeout */
	public static final int DEFAULT_SSH_READ_TIMEOUT = 2000;
	/** The configuration key for the default SSH user name */
	public static final String PROP_SSH_USER = "ssh.user";
	/** The default SSHService SSH user name */
	public static final String DEFAULT_SSH_USER = System.getProperty("user.name");
	/** The configuration key for the default SSH private key file name */
	public static final String PROP_SSH_PK = "ssh.pk";
	/** The default SSHService SSH private key file name */
	public static final String DEFAULT_SSH_PK = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "id_dsa";
	/** The configuration key for the default SSH private key passphrase file name */
	public static final String PROP_SSH_PK_PASS = "ssh.pk";
	/** The default SSHService SSH private key passphrase file name */
	public static final String DEFAULT_SSH_PK_PASS = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "id_dsa_passphrase";
	/** The configuration key for the default SSH user password file name */
	public static final String PROP_SSH_USER_PASS = "user.pass";
	/** The default SSHService SSH user password file name */
	public static final String DEFAULT_SSH_USER_PASS = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "id_dsa_passphrase";
	/** The JMX ObjectName for the reconnect thread pool */
	public static final ObjectName reconnectThreadPoolObjectName = JMXHelper.objectName("com.heliosapm.ssh:service=SSHReconnectExecutor");
	/** The JMX ObjectName for the reconnect scheduler */
	public static final ObjectName reconnectSchedulerObjectName = JMXHelper.objectName("com.heliosapm.ssh:service=SSHReconnectScheduler");	
	/** White space replacer */
	public static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("\\s+");
	
	
	/** The configured SSH connect timeout in ms. */
	protected int connectTimeout = DEFAULT_SSH_CONNECT_TIMEOUT;
	/** The configured SSH read timeout in ms. */
	protected int readTimeout = DEFAULT_SSH_READ_TIMEOUT;
	/** The configured SSH default user */
	protected String user = DEFAULT_SSH_USER;
	/** The configured SSH default private key file name */
	protected String pkFile = DEFAULT_SSH_PK;
	/** The configured SSH default private key passphrase file name */
	protected String pkPassFile = DEFAULT_SSH_PK_PASS;
	/** The configured SSH default user password file name */
	protected String userPassFile = DEFAULT_SSH_USER_PASS;
	/** The configured SSH default private key passphrase */
	protected String pkPass = null;
	/** The configured SSH default user password */
	protected String userPass = null;
	/** The SSH connection reconnect thread pool */
	protected final JMXManagedThreadPool reconnectThreadPool = new JMXManagedThreadPool(reconnectThreadPoolObjectName, "SSHReconnectExecutor", 1, 10, 100, 60000, 100, 99, true);
	/** The SSH connection reconnect scheduler */
	protected final JMXManagedScheduler reconnectScheduler = new JMXManagedScheduler(reconnectSchedulerObjectName, "SSHReconnectScheduler", 2, true);
	/** Connections watched for disconnect and scheduled for reconnect */
	protected final Map<WrappedConnection, ScheduledFuture<?>> reconnectConnections = new ConcurrentHashMap<WrappedConnection, ScheduledFuture<?>>();
	
	
	
	/**
	 * Acquires and returns the SSHService singleton
	 * @return the SSHService singleton
	 */
	public static SSHService getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new SSHService();
				}
			}
		}
		return instance;
	}
	
	
	/**
	 * Creates a new SSHService
	 */
	private SSHService() {
		connectTimeout = ConfigurationHelper.getIntSystemThenEnvProperty(PROP_SSH_CONNECT_TIMEOUT, DEFAULT_SSH_CONNECT_TIMEOUT); 				
		readTimeout = ConfigurationHelper.getIntSystemThenEnvProperty(PROP_SSH_READ_TIMEOUT, DEFAULT_SSH_READ_TIMEOUT);
		user = ConfigurationHelper.getSystemThenEnvProperty(PROP_SSH_USER, DEFAULT_SSH_USER);
		pkFile = ConfigurationHelper.getSystemThenEnvProperty(PROP_SSH_PK, DEFAULT_SSH_PK);
		pkPassFile = ConfigurationHelper.getSystemThenEnvProperty(PROP_SSH_PK_PASS, DEFAULT_SSH_PK_PASS);
		userPassFile = ConfigurationHelper.getSystemThenEnvProperty(PROP_SSH_USER_PASS, DEFAULT_SSH_USER_PASS);
		pkPass = getFileContent(pkPassFile);
		if(pkPass==null) pkPassFile = null;
		userPass = getFileContent(userPassFile);
		if(userPass==null) userPassFile = null;
	}
	
	/**
	 * Reads the content of a file as text
	 * @param fileName The file name
	 * @return the trimmed text or null if no text was read or file could not be read
	 */
	private String getFileContent(final String fileName) {
		if(fileName==null || fileName.trim().isEmpty()) return null;
		final File f = new File(fileName.trim());
		if(f.canRead()) {
			final String content = URLHelper.getTextFromURL(URLHelper.toURL(f));
			if(content!=null && !content.trim().isEmpty()) {
				return content.trim();
			}
		}
		return null;
	}
	
	private AuthInfo defaultAuthInfo() {
		final AuthInfo authInfo = new AuthInfo(user)
			.setConnectTimeout(connectTimeout)
			.setKexTimeout(readTimeout)  // FIXME:  rename read timeout to kex timeout
			.setUserPassword(userPass)
			.setYesManVerifier()				// FIXME:  add hosts file to use
			.setPrivateKeyPassword(pkPass);
		if(pkFile!=null) authInfo.setPrivateKey(new File(pkFile));
		return authInfo;
			
	}
	
	/**
	 * Connects to the SSH server at the passed host and port using all the configured defaults
	 * @param host The SSH server host name
	 * @param port The SSH listening port
	 * @return the wrapped SSH connection
	 */
	public WrappedConnection connect(final String host, final int port) {
		if(host==null || host.trim().isEmpty()) throw new IllegalArgumentException("The passed host was null or empty");
		return WrappedConnection.connectAndAuthenticate(host, port, defaultAuthInfo());
	}
	
	/**
	 * Connects to the SSH server at the passed host and port 22 using all the configured defaults
	 * @param host The SSH server host name
	 * @return the wrapped SSH connection
	 */
	public WrappedConnection connect(final String host) {
		return WrappedConnection.connectAndAuthenticate(host, 22, defaultAuthInfo());
	}


	/**
	 * Returns the default SSH connect timeout in ms. 
	 * @return the default SSH connect timeout in ms. 
	 */
	public int getConnectTimeout() {
		return connectTimeout;
	}


	/**
	 * Sets the default SSH connect timeout in ms. 
	 * @param connectTimeout the default connect timeout to set
	 */
	public void setConnectTimeout(final int connectTimeout) {
		if(connectTimeout < 0) throw new IllegalArgumentException("Invalid negative connect timeout value [" + connectTimeout + "]");
		this.connectTimeout = connectTimeout;
	}


	/**
	 * Returns the default SSH read timeout in ms. 
	 * @return the default SSH read timeout in ms.
	 */
	public int getReadTimeout() {
		return readTimeout;
	}


	/**
	 * Sets the default SSH read timeout in ms.
	 * @param readTimeout the default SSH read timeout in ms.
	 */
	public void setReadTimeout(final int readTimeout) {
		if(readTimeout < 0) throw new IllegalArgumentException("Invalid negative read timeout value [" + readTimeout + "]");
		this.readTimeout = readTimeout;
	}


	@Override
	public Collection<AgentIdentity> getIdentities() {
		// TODO Auto-generated method stub
		return null;
	}


	/**
	 * Returns the default SSH user
	 * @return the default SSH user
	 */
	public String getUser() {
		return user;
	}


	/**
	 * Sets the default SSH user
	 * @param user the default SSH user
	 */
	public void setUser(final String user) {
		if(user==null || user.trim().isEmpty()) throw new IllegalArgumentException("The passed user was null or empty");
		this.user = user;
	}


	/**
	 * Returns the default pk file name
	 * @return the default pk file name
	 */
	public String getPkFile() {
		return pkFile;
	}


	/**
	 * Sets the default pk file name
	 * @param pkFile the default pk file name
	 */
	public void setPkFile(final String pkFile) {
		this.pkFile = pkFile;
		// TODO: update key ref
	}


	/**
	 * Returns the default pk's passphrase file name
	 * @return the default pk's passphrase file name
	 */
	public String getPkPassFile() {
		return pkPassFile;
	}


	/**
	 * Sets the default pk's passphrase file name
	 * @param pkPassFile the default pk's passphrase file name
	 */
	public void setPkPassFile(final String pkPassFile) {		
		this.pkPassFile = pkPassFile;
		this.pkPass = getFileContent(this.pkPassFile);
	}


	/**
	 * Returns the default user password file name
	 * @return the default user password file name
	 */
	public String getUserPassFile() {
		return userPassFile;
	}


	/**
	 * Sets default user password file name
	 * @param userPassFile the default user password file name
	 */
	public void setUserPassFile(final String userPassFile) {
		this.userPassFile = userPassFile;
		this.userPass = getFileContent(this.userPassFile);
	}




	/**
	 * Sets the default pk's passphrase 
	 * @param pkPass the pkPass to set
	 */
	public void setPkPass(final String pkPass) {
		this.pkPass = pkPass;
	}


	/**
	 * Sets the default SSH user name
	 * @param userPass the default SSH user name
	 */
	public void setUserPass(final String userPass) {
		this.userPass = userPass;
	}


	@Override
	public void onClosed(WrappedConnection closeable, Throwable cause) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onReset(WrappedConnection resetCloseable) {
		// TODO Auto-generated method stub
		
	}

}
