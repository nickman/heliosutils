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

import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.crypto.PEMDecoder;

import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: ConnectInfo</p>
 * <p>Description: Bean construct to wrap connection properties and authentication credentials</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdbex.monitor.ssh.AuthInfo</code></p>
 */

public class ConnectInfo implements ServerHostKeyVerifier {
	/** The authentication user name */
	private String userName;
	/** The authentication user password */
	private String userPassword = null;
	/** The authentication private key char array */
	private char[] privateKey = (DEFAULT_PRIVATE_PEM_KEY_FILE!=null && DEFAULT_PRIVATE_PEM_KEY_FILE.canRead()) ? URLHelper.getTextFromURL(URLHelper.toURL(DEFAULT_PRIVATE_PEM_KEY_FILE)).toCharArray() : null;
	/** The authentication private key passphrase */
	private String privateKeyPassword = null;
	/** The authentication private key passphrase file */
	private String privateKeyPasswordFile = null;

	/** The authentication methods in the order they should be attempted */
	private AuthMethod[] authMethods = AuthMethod.values();
	/** The server host key verifier */
	private ServerHostKeyVerifier verifier = null;
	/** The known hosts manager */
	private KnownHosts knownHosts = null;
	/** The relay host to connect through */
	private String relayHost = null;
	/** The relay port to connect through */
	private int relayPort = 22;

	/** The connection timeout in ms. */
	private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
	/** The key exchange timeout in ms. */
	private int kexTimeout = DEFAULT_KEX_TIMEOUT;
	
	
	/** The default private key file name */
	public static final String DEFAULT_PRIVATE_PEM_KEY = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "id_dsa";
	/** The default private key file name */
	public static final File DEFAULT_PRIVATE_PEM_KEY_FILE = new File(DEFAULT_PRIVATE_PEM_KEY); 
	/** The default known hosts file name */
	public static final String DEFAULT_KNOWN_HOSTS_NAME = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "known_hosts";
	/** The default known hosts */
	public static final KnownHosts DEFAULT_KNOWN_HOSTS = KnownHostsRepo.getInstance().getKnownHostsOrNull(DEFAULT_KNOWN_HOSTS_NAME);
	
	/** The default connect timeout */
	public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
	/** The default kex timeout */
	public static final int DEFAULT_KEX_TIMEOUT = 5000;
	
	/** A set of all the recognized property names */
	public static final Set<String> PROPERTY_NAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"sshusername", "connecttimeout", "kextimeout", "sshpassword", "privatekey", "privatekeyf",
			"pkpassword", "pkpasswordf", "knownhosts", "noverifier", "relayhost", "relayport"
	)));
	

	
	/** A map of known successful authentication methods keyed by host:port */
	private static final Map<String, AuthMethod> successfulAuthMethods = new ConcurrentHashMap<String, AuthMethod>();

	
	/**
	 * Creates a new ConnectInfo from the passed JSON object
	 * @param authConfig The json configuration
	 * @return the built ConnectInfo
	 */
	public static ConnectInfo fromJSON(final JSONObject authConfig) {
		if(authConfig==null) throw new IllegalArgumentException("The passed authConfig was null");
		final ConnectInfo a = new ConnectInfo();
		JSONObject config = authConfig;
		if(config.has("tunnel")) {
			try {
				JSONObject x = config.getJSONObject("tunnel");
				config = x;
			} catch (Exception ex) {/* No Op */}
		}
		if(config.has("relayhost")) a.relayHost = config.optString("relayhost");
		if(config.has("relayport")) a.relayPort = config.optInt("relayport", 22);
		if(config.has("sshusername")) a.userName = config.optString("sshusername");
		if(config.has("connecttimeout")) a.connectTimeout = config.optInt("connecttimeout", DEFAULT_CONNECT_TIMEOUT);
		if(config.has("kextimeout")) a.kexTimeout = config.optInt("kextimeout", DEFAULT_KEX_TIMEOUT);		
		if(config.has("sshpassword")) a.userPassword = config.optString("sshpassword");
		if(config.has("privatekey")) { 
			a.privateKey = config.optString("privatekey").toCharArray();
		} else if(config.has("privatekeyf")) {
			a.setPrivateKey(new File(config.optString("privatekeyf")));
		}
		if(config.has("pkpassword")) {
			a.privateKeyPassword = config.optString("pkpassword");
		} else {
			if(config.has("pkpasswordf")) {
				a.privateKeyPasswordFile = config.optString("pkpasswordf");
				a.privateKeyPassword = URLHelper.getTextFromURL(URLHelper.toURL(config.optString("pkpasswordf")));
				if(a.privateKeyPassword!=null) a.privateKeyPassword = a.privateKeyPassword.trim();
			}
		}
		if(config.has("knownhosts")) {
			a.knownHosts = KnownHostsRepo.getInstance().getKnownHosts(config.optString("knownhosts"));
			a.verifier = a;
		}
		if(config.has("noverifier")) {
			if(a.verifier!=null) throw new RuntimeException("Option [noverifier] specified but KnownHosts verifier already set");
			a.verifier = YesManHostKeyVerifier.INSTANCE;
		}
		if(a.verifier==null && DEFAULT_KNOWN_HOSTS!=null) {
			a.verifier = a;
		}
		return a;
	}
	
	/**
	 * Creates a new ConnectInfo from the passed JSON object
	 * @param authConfig The json configuration
	 * @return the built ConnectInfo
	 */
	public static ConnectInfo fromJSON(final String authConfig) {
		if(authConfig==null || authConfig.trim().isEmpty()) throw new IllegalArgumentException("The passed authConfig was null or empty");
		try {
			return fromJSON(new JSONObject(authConfig));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	

	/**
	 * Creates a new ConnectInfo from the passed properties
	 * @param authConfig The properties configuration
	 * @return the built ConnectInfo
	 */
	public static ConnectInfo fromProperties(final Properties authConfig) {
		if(authConfig==null) throw new IllegalArgumentException("The passed authConfig was null");
		
		final ConnectInfo a = new ConnectInfo();
		if(authConfig.containsKey("relayhost")) a.relayHost = authConfig.getProperty("relayhost");
		if(authConfig.containsKey("relayport")) a.relayPort = Integer.parseInt(authConfig.getProperty("relayport", "22"));
		if(authConfig.containsKey("sshusername")) a.userName = authConfig.getProperty("sshusername");
		if(authConfig.containsKey("connecttimeout")) a.connectTimeout = Integer.parseInt(authConfig.getProperty("connecttimeout"));
		if(authConfig.containsKey("kextimeout")) a.kexTimeout = Integer.parseInt(authConfig.getProperty("kextimeout"));		
		if(authConfig.containsKey("sshpassword")) a.userPassword = authConfig.getProperty("sshpassword");
		if(authConfig.containsKey("privatekey")) { 
			a.privateKey = authConfig.getProperty("privatekey").toCharArray();
		} else if(authConfig.containsKey("privatekeyf")) {
			a.setPrivateKey(new File(authConfig.getProperty("privatekeyf")));
		}
		if(authConfig.containsKey("pkpassword")) {
			a.privateKeyPassword = authConfig.getProperty("pkpassword");
		} else {
			if(authConfig.containsKey("pkpasswordf")) {
				a.privateKeyPasswordFile = authConfig.getProperty("pkpasswordf");
				a.privateKeyPassword = URLHelper.getTextFromURL(URLHelper.toURL(authConfig.getProperty("pkpasswordf")));
				if(a.privateKeyPassword!=null) a.privateKeyPassword = a.privateKeyPassword.trim();
			}			
		}
		if(authConfig.containsKey("knownhosts")) {
			a.knownHosts = KnownHostsRepo.getInstance().getKnownHosts(authConfig.getProperty("knownhosts"));
			a.verifier = a;
		}
		if(authConfig.containsKey("noverifier")) {
			if(a.verifier!=null) throw new RuntimeException("Option [noverifier] specified but KnownHosts verifier already set");
			a.verifier = YesManHostKeyVerifier.INSTANCE;
		}
		if(a.verifier==null && DEFAULT_KNOWN_HOSTS!=null) {
			a.verifier = a;
		}		
		return a;
	}
	
	/**
	 * Returns a properties object built from the passed json object
	 * @param authConfig A JSON auth info representation
	 * @return A properties auth info representation
	 */
	public static Properties propsFromJSON(final JSONObject authConfig) {
		final Properties p = new Properties();
		JSONObject config = authConfig;
		if(config.has("tunnel")) {
			try {
				JSONObject x = config.getJSONObject("tunnel");
				config = x;
			} catch (Exception ex) {/* No Op */}
		} 
		for(String key: PROPERTY_NAMES) {
			if(config.has(key)) {
				if("pkpassword".equals(key)) {
					p.setProperty(key, "" + config.get(key));
				} else {
					p.setProperty(key, "" + config.get(key));
				}
			}
		}
		return p;
	}
	
	/**
	 * Creates a new ConnectInfo from the passed properties format string
	 * @param authConfig The properties string configuration
	 * @return the built ConnectInfo
	 */
	public static ConnectInfo fromProperties(final String authConfig) {
		if(authConfig==null || authConfig.trim().isEmpty()) throw new IllegalArgumentException("The passed authConfig was null or empty");
		try {
			Properties p = new Properties();
			p.load(new StringReader(authConfig));
			return fromProperties(p);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Creates a new ConnectInfo from the passed env map
	 * @param env The map configuration
	 * @return the built ConnectInfo
	 */
	public static ConnectInfo fromMap(final Map<String, ?> env) {
		if(env==null || env.isEmpty()) throw new IllegalArgumentException("The passed authConfig map was null or empty");
		final Properties p = new Properties();
		for(Map.Entry<String, ?> entry : env.entrySet()) {
			p.put(entry.getKey(), entry.getValue().toString());
		}
		return fromProperties(p);
	}
	
	
	
	
	
	/**
	 * Creates a new ConnectInfo from the string returned from the passed URL which can be in properties or json format
	 * @param authConfig The string configuration
	 * @return the built ConnectInfo
	 */
	public static ConnectInfo fromURL(final URL authConfig) {
		if(authConfig==null) throw new IllegalArgumentException("The passed authConfig was null");
		final String text = URLHelper.getTextFromURL(authConfig);
		try {
			return fromJSON(new JSONObject(text));
		} catch (JSONException ex) {
			try {				
				return fromProperties(text);
			} catch (Exception ex2) {
				throw new RuntimeException(ex2);
			}					
		}		
	}
	
	public Map<String, ?> toEnvMap() {
		final Map<String, Object> env = new HashMap<String, Object>();
		env.put("relayport", relayPort);
		env.put("connecttimeout", connectTimeout);
		env.put("kextimeout", kexTimeout);		
		if(relayHost!=null) env.put("relayhost", relayHost);
		
		if(userName!=null) env.put("sshusername", userName);
		if(userPassword!=null) env.put("sshpassword", userPassword);
		if(privateKey!=null) env.put("privatekey", new String(privateKey));
		if(privateKeyPassword!=null) env.put("pkpassword", privateKeyPassword);
		if(privateKeyPasswordFile!=null) env.put("pkpasswordf", privateKeyPasswordFile);

		
		if(verifier==YesManHostKeyVerifier.INSTANCE) {
			env.put("noverifier", true);
		} else {
			if(knownHosts!=null && knownHosts.getFileName()!=null) env.put("knownhosts", knownHosts.getFileName());
		}
		return env;		
	}

	
	/**
	 * Creates a new ConnectInfo from the string returned from the passed URL which can be in properties or json format
	 * @param authConfig The string configuration
	 * @return the built ConnectInfo
	 */
	public static ConnectInfo fromURL(final String authConfig) {
		return fromURL(URLHelper.toURL(authConfig));
	}

	
	
	
	/**
	 * Creates a new ConnectInfo
	 * @param userName
	 * @param userPassword
	 * @param privateKey
	 * @param privateKeyPassword
	 * @param authMethods
	 * @param verifier
	 * @param knownHosts
	 * @param connectTimeout
	 * @param kexTimeout
	 */
	private ConnectInfo(final String userName, final String userPassword, final char[] privateKey, final String privateKeyPassword,
			final AuthMethod[] authMethods, final ServerHostKeyVerifier verifier, final KnownHosts knownHosts, final int connectTimeout,
			final int kexTimeout, final String relayHost, final int relayPort) {
		this.userName = userName;
		this.userPassword = userPassword;
		this.privateKey = privateKey;
		this.privateKeyPassword = privateKeyPassword;
		this.authMethods = authMethods;
		this.verifier = verifier;
		this.knownHosts = knownHosts;
		this.connectTimeout = connectTimeout;
		this.kexTimeout = kexTimeout;
		this.relayHost = relayHost;
		this.relayPort = relayPort;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#clone()
	 */
	@Override
	public ConnectInfo clone() {
		return new ConnectInfo(userName, userPassword, privateKey, privateKeyPassword,
				authMethods, verifier, knownHosts, connectTimeout, kexTimeout, relayHost, relayPort);
	}

	/**
	 * Creates a new ConnectInfo 
	 */
	public ConnectInfo() {
		this(System.getProperty("user.name"));
	}
	
	/**
	 * Creates a new ConnectInfo
	 * @param userName The user name
	 */
	public ConnectInfo(final String userName) {
		if(userName==null) throw new IllegalArgumentException("The passed user name was null");
		this.userName = userName.trim();
	}
	
	/**
	 * Attempts to authenticate the passed connection.
	 * Returns true if the connection is already fully authenticated
	 * @param connection The connection to authenticate
	 * @return true if connection is authenticated, false otherwise
	 */
	boolean authenticate(final Connection connection) {
		if(connection==null) throw new IllegalArgumentException("Passed connection was null");
		if(connection.isAuthenticationComplete()) return true;
		if(authMethods==null || authMethods.length==0) return false;
		if(verifier==null) verifier = new AuthorizedKeysHostKeyVerifier();
		checkPrivateKey();
		final String amKey = connection.getHostname() + ":" + connection.getPort();
		AuthMethod method = successfulAuthMethods.get(amKey);
		if(method != null) {
			try {
				if(!connection.isConnected()) {
					try {
						connection.connect(verifier, connectTimeout, kexTimeout);
					} catch (Exception ex) {				
						throw new RuntimeException("Failed to connect [" + amKey + "]", ex);
					}
				}				
				method.authenticate(connection, this);
				if(connection.isAuthenticationComplete()) {
//					System.err.println("Completed authentication to [" + amKey + "] with method [" + method.name() + "]");
					successfulAuthMethods.put(amKey, method);
					return true;
				}
			} catch (Exception ex) {
				/* No Op */
			}
			
		}
		for(AuthMethod am: authMethods) {
			try {
				if(!connection.isConnected()) {
					try {
						connection.connect(verifier, connectTimeout, kexTimeout);
					} catch (Exception ex) {				
						throw new RuntimeException("Failed to connect [" + amKey + "]", ex);
					}
				}				
				am.authenticate(connection, this);
				if(connection.isAuthenticationComplete()) {
					successfulAuthMethods.put(amKey, am);
//					System.err.println("Completed authentication to [" + amKey + "] with method [" + am.name() + "]");
					return true;
				}
			} catch (Exception ex) {
//				System.err.println("Failed to authenticate [" + amKey + "] with method [" + am.name() + "]:" + ex);
//				ex.printStackTrace(System.err);
				/* No Op */
			}
		}
		return false;
	}
	
	private void checkPrivateKey() {
		if(				
			(userPassword==null || userPassword.isEmpty())
			&&
			(privateKey==null || privateKey.length==0)
			) {
			if(DEFAULT_PRIVATE_PEM_KEY_FILE.canRead()) {
				final char[] pkey = URLHelper.getCharsFromURL(URLHelper.toURL(DEFAULT_PRIVATE_PEM_KEY_FILE));
				try {
					PEMDecoder.decode(pkey, privateKeyPassword);
					privateKey = pkey;
				} catch (Exception ex) { /* No Op */ }				
			}
		} else {
			
		}
	}
	
	// ======================================================================
	// Setters
	// ======================================================================

	/**
	 * Sets the user password
	 * @param userPassword the userPassword to set
	 * @return this ConnectInfo
	 */
	public ConnectInfo setUserPassword(final String userPassword) {
		if(userPassword==null) throw new IllegalArgumentException("The passed user password was null");
		this.userPassword = userPassword.trim();
		return this;
	}
	
	/**
	 * Sets the relay host
	 * @param relayHost the relay host to set
	 * @return this ConnectInfo
	 */
	public ConnectInfo setRelayHost(final String relayHost) {
		if(relayHost==null) throw new IllegalArgumentException("The passed relay host was null");
		this.relayHost = relayHost.trim();
		return this;
	}
	
	/**
	 * Sets the relay port
	 * @param relayPort the relay port to set
	 * @return this ConnectInfo
	 */
	public ConnectInfo setRelayPort(final int relayPort) {
		if(relayPort<1) throw new IllegalArgumentException("Invalid relay port [" + relayPort + "]");
		this.relayPort = relayPort;
		return this;
	}
	
	

	/**
	 * Sets the private key
	 * @param privateKey the privateKey to set
	 * @return this ConnectInfo
	 */
	public ConnectInfo setPrivateKey(final char[] privateKey) {
		if(privateKey==null) throw new IllegalArgumentException("The passed privateKey was null");
		this.privateKey = privateKey;
		return this;
	}
	
	/**
	 * Sets the private key
	 * @param privateKey the privateKey to set
	 * @return this ConnectInfo
	 */
	public ConnectInfo setPrivateKey(final String privateKey) {
		if(privateKey==null) throw new IllegalArgumentException("The passed privateKey was null");
		this.privateKey = privateKey.toCharArray();
		return this;		
	}
	
	/**
	 * Sets the private key
	 * @param privateKey the privateKey to set
	 * @return this ConnectInfo
	 */
	public ConnectInfo setPrivateKey(final File privateKey) {
		if(privateKey==null) throw new IllegalArgumentException("The passed privateKey was null");
		if(!privateKey.canRead()) throw new IllegalArgumentException("The passed privateKey file [" + privateKey.getAbsolutePath() + "] cannot be read");
		this.privateKey = URLHelper.getCharsFromURL(privateKey.getAbsolutePath());
		return this;		
	}

	/**
	 * Sets the private key pass phrase
	 * @param privateKeyPassword the privateKeyPassword to set
	 * @return this ConnectInfo
	 */
	public ConnectInfo setPrivateKeyPassword(final String privateKeyPassword) {
		if(privateKeyPassword==null) throw new IllegalArgumentException("The passed privateKeyPassword was null");
		this.privateKeyPassword = privateKeyPassword;
		return this;
	}
	
	/**
	 * Sets the authentication methods 
	 * @param authMethods the authMethods to set
	 * @return this ConnectInfo
	 */
	public ConnectInfo setAuthMethods(AuthMethod... authMethods) {
		this.authMethods = authMethods;
		return this;
	}
	
	/**
	 * Sets the host key verifier
	 * @param verifier the verifier to set
	 * @return this ConnectInfo
	 */
	public ConnectInfo setVerifier(final ServerHostKeyVerifier verifier) {
		if(verifier==null) throw new IllegalArgumentException("The passed ServerHostKeyVerifier was null");
		this.verifier = verifier;
		return this;
	}
	
	/**
	 * Sets the YesMan verifier for this auth
	 * @return this ConnectInfo
	 */
	public ConnectInfo setYesManVerifier() {
		this.verifier = YesManHostKeyVerifier.INSTANCE;
		return this;
	}

	/**
	 * Sets the connection timeout in ms.
	 * @param connectTimeout the connectTimeout to set
	 * @return this ConnectInfo
	 */
	public ConnectInfo setConnectTimeout(final int connectTimeout) {
		if(connectTimeout<0) throw new IllegalArgumentException("The passed connectTimeout [" + connectTimeout + "] was invalid");
		this.connectTimeout = connectTimeout;
		return this;
	}

	/**
	 * Sets the key exchange timeout in ms.
	 * @param kexTimeout the kexTimeout to set
	 * @return this ConnectInfo
	 */
	public ConnectInfo setKexTimeout(final int kexTimeout) {
		if(kexTimeout<0) throw new IllegalArgumentException("The passed kexTimeout [" + kexTimeout + "] was invalid");
		this.kexTimeout = kexTimeout;
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
	 * Returns the relay host
	 * @return the relay host
	 */
	public String getRelayHost() {
		return relayHost;
	}
	
	/**
	 * Returns the relay host or the supplied default
	 * @param defaultHost the host to return if the info's relay host is null
	 * @return the relay host or the supplied default
	 */
	public String getRelayHost(final String defaultHost) {
		return relayHost==null ? defaultHost : relayHost;
	}
	
	
	/**
	 * Returns the relay port
	 * @return the relay port
	 */
	public int getRelayPort() {
		return relayPort;
	}
	
	/**
	 * Returns the host key verifier
	 * @return the verifier
	 */
	public ServerHostKeyVerifier getVerifier() {
		return verifier;
	}

	/**
	 * Returns the connection timeout in ms.
	 * @return the connectTimeout
	 */
	public int getConnectTimeout() {
		return connectTimeout;
	}

	/**
	 * Returns the key exchange timeout in ms.
	 * @return the kexTimeout
	 */
	public int getKexTimeout() {
		return kexTimeout;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder(userName).append("[");
		b.append("relayHost:").append((relayHost==null || relayHost.isEmpty()) ? "" : relayHost).append(", ");
		b.append("relayPort:").append(relayPort).append(", ");
		b.append("pass:").append((userPassword==null || userPassword.isEmpty()) ? false : true).append(", ");
		b.append("key:").append((privateKey==null) ? false : true).append(", ");
		b.append("ctimeout:").append(connectTimeout).append("ms. , ");
		b.append("ktimeout:").append(kexTimeout).append("ms. , ");
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

//	public static final int HOSTKEY_IS_OK = 0;
//	public static final int HOSTKEY_IS_NEW = 1;
//	public static final int HOSTKEY_HAS_CHANGED = 2;

	
	@Override
	public boolean verifyServerHostKey(final String hostname, final int port, final String serverHostKeyAlgorithm, final byte[] serverHostKey) throws Exception {
		if(knownHosts!=null) {
			final int result = knownHosts.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);
			switch(result) {
			case KnownHosts.HOSTKEY_IS_OK :
				return true;
			case KnownHosts.HOSTKEY_IS_NEW :
				break;
			case KnownHosts.HOSTKEY_HAS_CHANGED :
				break;
			}
			return false;
		}
		return false;
	}


}
