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
package test.com.heliosapm.utils.ssh.terminal;

import java.io.PrintStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.ethz.ssh2.ServerConnection;
import ch.ethz.ssh2.crypto.PEMDecoder;
import ch.ethz.ssh2.signature.DSAPrivateKey;
import ch.ethz.ssh2.signature.RSAPrivateKey;

import com.heliosapm.utils.io.StdInCommandHandler;
import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: SSHServer</p>
 * <p>Description: Test SSH server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.com.heliosapm.utils.ssh.terminal.SSHServer</code></p>
 */

public class SSHServer {
	/** A cache of existing servers */
	private static final Map<String, SSHServer> servers = new ConcurrentHashMap<String, SSHServer>();
	
	/** The server listener connection */
	private final ServerConnection serverConnection;
	
	/** The test private key pass phrase */
	private static final String PK_PASSPHRASE = "the moon is a balloon";
	
	/** Retain system out */
	protected static final PrintStream OUT = System.out;
	/** Retain system err */
	protected static final PrintStream ERR = System.err;
	
	
	/** The test RSA private key file, sans passphrase */
	public static final String RSA_PRIVATE_KEY_FILE = "./src/test/resources/keys/id_rsa";
	/** The test RSA private key file, avec passphrase */
	public static final String RSA_PP_PRIVATE_KEY_FILE = "./src/test/resources/keys/id_pp_rsa";
	/** The test DSA private key file, sans passphrase */
	public static final String DSA_PRIVATE_KEY_FILE = "./src/test/resources/keys/id_dsa";
	/** The test DSA private key file, avec passphrase */
	public static final String DSA_PP_PRIVATE_KEY_FILE = "./src/test/resources/keys/id_pp_dsa";
	
	/**
	 * Creates a new SSHServer
	 * @param iface The binding interface
	 * @param port The port to bind to
	 * @param dsaKey The optional dsa key
	 * @param rsaKey The optional rsa key
	 * @return a new SSHServer
	 */
	public static SSHServer getInstance(final String iface, final int port, final String dsaKey, final String rsaKey) {
		final String _iface = (iface==null || iface.trim().isEmpty()) ? "0.0.0.0" : iface.trim();
		final String key = _iface + ":" + port;
		SSHServer server = servers.get(key);
		if(server==null) {
			synchronized(servers) {
				server = servers.get(key);
				if(server==null) {
					server = new SSHServer(_iface, port, dsaKey, rsaKey);
				}
			}
		}
		return server;
	}
	
	/**
	 * Creates a new SSHServer
	 */
	private SSHServer(final String iface, final int port, final String dsaKeyFile, final String rsaKeyFile) {
		DSAPrivateKey dsaKey = (DSAPrivateKey)readKey(dsaKeyFile);
		RSAPrivateKey rsaKey = (RSAPrivateKey)readKey(rsaKeyFile);
		try {
			serverConnection = new ServerConnection(new Socket(iface, port), dsaKey, rsaKey);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to initialize server for [" + iface + ":" + port + "]", ex);
		}
	}
	
	public static void main(final String[] args) {
		log("Test SSHServer");
		SSHServer server = SSHServer.getInstance("0.0.0.0", 22, DSA_PRIVATE_KEY_FILE, RSA_PRIVATE_KEY_FILE);
		log("Server Started");
		StdInCommandHandler.getInstance();
	}
	
	
	private static Object readKey(final String fileName) {
		if(URLHelper.isFile(fileName)) {
			final char[] c = URLHelper.getCharsFromURL(fileName);
			try {
				if(PEMDecoder.isPEMEncrypted(c)) {
					return PEMDecoder.decode(c, PK_PASSPHRASE);					
				} else {
					return PEMDecoder.decode(c, null);
				}
			} catch (Exception x) {/* No Op */}
		}
		return null;
	}

	/**
	 * Out printer
	 * @param fmt the message format
	 * @param args the message values
	 */
	public static void log(String fmt, Object...args) {
		OUT.println(String.format(fmt, args));
	}
	
	
	/**
	 * Err printer
	 * @param fmt the message format
	 * @param args the message values
	 */
	public static void loge(String fmt, Object...args) {
		ERR.print(String.format(fmt, args));
		if(args!=null && args.length>0 && args[0] instanceof Throwable) {
			ERR.println("  Stack trace follows:");
			((Throwable)args[0]).printStackTrace(ERR);
		} else {
			ERR.println("");
		}
	}
	

}
