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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

import ch.ethz.ssh2.AuthenticationResult;
import ch.ethz.ssh2.ConnectionInfo;
import ch.ethz.ssh2.PtySettings;
import ch.ethz.ssh2.ServerAuthenticationCallback;
import ch.ethz.ssh2.ServerConnection;
import ch.ethz.ssh2.ServerConnectionCallback;
import ch.ethz.ssh2.ServerSession;
import ch.ethz.ssh2.ServerSessionCallback;
import ch.ethz.ssh2.SimpleServerSessionCallback;
import ch.ethz.ssh2.crypto.PEMDecoder;
import ch.ethz.ssh2.signature.DSAPrivateKey;
import ch.ethz.ssh2.signature.RSAPrivateKey;

import com.heliosapm.utils.io.CloseListener;
import com.heliosapm.utils.io.CloseableService;
import com.heliosapm.utils.io.StdInCommandHandler;
import com.heliosapm.utils.ssh.terminal.AuthMethod;
import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: SSHServer</p>
 * <p>Description: Test SSH server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.com.heliosapm.utils.ssh.terminal.SSHServer</code></p>
 */

public class SSHServer implements ServerAuthenticationCallback, ServerConnectionCallback, CloseListener<Socket>, Closeable {
	/** A cache of existing servers */
	private static final Map<String, SSHServer> servers = new ConcurrentHashMap<String, SSHServer>();
	
	
	/** The server's listening interface */
	private final String listeningIface;
	/** The server's listening port */
	private final int listeningPort;
	/** The server's DSA private key */
	private final DSAPrivateKey dsaKey;
	/** The server's RSA private key */
	private final RSAPrivateKey rsaKey;
	/** Indicates if the server is running */
	private final AtomicBoolean started = new AtomicBoolean(false);
	/** The server accept thread */
	private Thread acceptThread = null;
	/** The server's listening socket */
	private final ServerSocket serverSocket;
	/** The server side of the connected client sockets keyed by the port */
	private final NonBlockingHashMapLong<ServerConnection> clientSockets = new NonBlockingHashMapLong<ServerConnection>();
	/** The client handling thread group */
	private final ThreadGroup clientThreadGroup = new ThreadGroup("ClientHandlingThreadGroup");
	/** The client handling thread factory */
	private final ThreadFactory clientThreadFactory = new ThreadFactory() {
		final AtomicLong serial = new AtomicLong(0);
		@Override
		public Thread newThread(final Runnable r) {
			final Thread t = new Thread(clientThreadGroup, r, "ClientHandlingThread#" + serial.incrementAndGet()) {
				@Override
				public void run() {
					super.run();
				}
				@Override
				public synchronized void start() {
					super.start();
				}
			};
			t.setDaemon(true);
			return t;
		}
	};
	/** The remaining auth methods */
	private final Set<AuthMethod> remainingAuthMethods = AuthMethod.getAvailableMethodSet();
	
	
	/** The test private key pass phrase */
	private static final String PK_PASSPHRASE = "the moon is a balloon";
	/** The authentication user */
	private static final String AUTH_USER = "sshuser";
	/** The authentication password */
    private static final String AUTH_PASS = "sshpassword";	
	
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
	public static SSHServer getInstance(final String iface, final int port, final DSAPrivateKey dsaKey, final RSAPrivateKey rsaKey) {
		final String _iface = (iface==null || iface.trim().isEmpty()) ? "0.0.0.0" : iface.trim();
		final String key = _iface + ":" + port;
		SSHServer server = servers.get(key);
		if(server==null) {
			synchronized(servers) {
				server = servers.get(key);
				if(server==null) {
					server = new SSHServer(_iface, port, dsaKey, rsaKey);
					servers.put(key, server);
				}
			}
		}
		return server;		
	}
	
	
	/**
	 * Creates a new SSHServer
	 * @param iface The binding interface
	 * @param port The port to bind to
	 * @param dsaKey The optional dsa key file name
	 * @param rsaKey The optional rsa key file name
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
					servers.put(key, server);
				}
			}
		}
		return server;
	}
	
	/**
	 * Creates a new SSHServer
	 * @param iface The bind interface
	 * @param port The listening port
	 * @param dsaKeyFile An optional DSA private key file name
	 * @param rsaKeyFile An optional RSA private key file name
	 */
	private SSHServer(final String iface, final int port, final String dsaKeyFile, final String rsaKeyFile) {
		this(iface, port, (DSAPrivateKey)readKey(dsaKeyFile), (RSAPrivateKey)readKey(rsaKeyFile));
	}
	
	/**
	 * Creates a new SSHServer
	 * @param iface The bind interface
	 * @param port The listening port
	 * @param dsaKeyFile An optional DSA private key file name
	 * @param rsaKeyFile An optional RSA private key file name
	 */
	private SSHServer(final String iface, final int port, final DSAPrivateKey dsaKey, final RSAPrivateKey rsaKey) {
		listeningIface = iface;
		this.dsaKey = dsaKey;
		this.rsaKey = rsaKey;
		
		try {
			serverSocket = new ServerSocket(port, 100, InetAddress.getByName(iface));
			listeningPort = serverSocket.getLocalPort();
			serverSocket.setSoTimeout(1000);
			log("SSHServer bound on [%s:%s]", listeningIface, listeningPort);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to initialize server for [" + iface + ":" + port + "]", ex);
		}
		CloseableService.getInstance().register(this);
	}
	
	
	public static void main(final String[] args) {
		log("Test SSHServer");
		final SSHServer server = SSHServer.getInstance("0.0.0.0", 0, DSA_PRIVATE_KEY_FILE, RSA_PRIVATE_KEY_FILE);
		server.start();
		StdInCommandHandler.getInstance().registerCommand("stop", new Runnable(){
			public void run() {
				server.stop();
			}
		});
		StdInCommandHandler.getInstance().registerCommand("count", new Runnable(){
			public void run() {
				log("SSHServer Count: %s", servers.size());
			}
		});
		StdInCommandHandler.getInstance().registerCommand("clicount", new Runnable(){
			public void run() {
				log("SSHServer [%s:%s] Client Count: %s", server.listeningIface, server.listeningPort, server.clientSockets.size());
			}
		});
		
	}
	
	/**
	 * Returns the listening port
	 * @return the listening port
	 */
	public int getListeningPort() {
		return listeningPort;
	}
	
	/**
	 * Returns the iface the server is bound to
	 * @return the iface the server is bound to
	 */
	public String getBoundInterface() {
		return listeningIface;
	}
	
	private static Object readKey(final String fileName) {
		if(URLHelper.isFile(fileName)) {
			final char[] c = URLHelper.getCharsFromURL(fileName);
			try {
				if(PEMDecoder.isPEMEncrypted(c)) {
					return PEMDecoder.decode(c, PK_PASSPHRASE);					
				}
				return PEMDecoder.decode(c, null);
			} catch (Exception x) {/* No Op */}
		}
		return null;
	}
	
	public SSHServer start() {
		if(started.compareAndSet(false, true)) {
			acceptThread = new Thread("ServerAcceptThread") {				
				public void run() {
					while(started.get()) {
						try {
							final Socket s = serverSocket.accept();
							clientThreadFactory.newThread(newClientRunnable(s)).start();
						} catch (SocketTimeoutException ste) {
							continue;
						} catch (Exception e) {
							if(e instanceof InterruptedException) {
								if(Thread.interrupted()) Thread.interrupted();
								continue;
							}
							e.printStackTrace();
						}
					}
					log("AcceptThread for [%s] Stopped", serverSocket.getLocalPort());
					clientSockets.remove(listeningPort);
				}
			};
			acceptThread.start();
			log("SSHServer started and accepting on [%s:%s]", listeningIface, listeningPort);
		}
		return this;
	}
	
	/**
	 * Stops this SSHServer instance
	 */
	public void stop() {
		if(started.compareAndSet(true, false)) {
			acceptThread.interrupt();
		}
	}
	
	/**
	 * Passthrough to {@link #stop()}
	 */
	public void close() {
		stop();
	}
	
	Runnable newClientRunnable(final Socket socket) {
		final CloseNotifyingSocket clientSocket = new CloseNotifyingSocket(socket, this);		
		final SSHServer server = this;
		return new Runnable() {
			public void run() {
				log("Starting new ClientSocket [%s] on thread [%s]", clientSocket, Thread.currentThread().getName());
				final ServerConnection sc = new ServerConnection(clientSocket);
				sc.setDsaHostKey(dsaKey);
				sc.setRsaHostKey(rsaKey);
				sc.setServerConnectionCallback(server);
				sc.setAuthenticationCallback(server);
				clientSockets.put(clientSocket.getLocalPort(), sc);
				ConnectionInfo info = null;
				try {
					sc.connect(5000);
					info = sc.getConnectionInfo();
	                System.out.println("Kex Algo:             " + info.keyExchangeAlgorithm);
	                System.out.println("Kex Count:            " + info.keyExchangeCounter);
	                System.out.println("Server Host Key Algo: " + info.serverHostKeyAlgorithm);
	                System.out.println("C2S Crypto Algo:      " + info.clientToServerCryptoAlgorithm);
	                System.out.println("C2S MAC Algo:         " + info.clientToServerMACAlgorithm);
	                System.out.println("S2C Crypto Algo:      " + info.serverToClientCryptoAlgorithm);
	                System.out.println("S2C MAC Algo:         " + info.serverToClientMACAlgorithm);
	                System.out.flush();									
				} catch (Exception ex) {
					ex.printStackTrace(System.err);
					try { clientSocket.close(); } catch (Exception x) {/* No Op */}
				}
			}
		};
	}
	
	private void acceptSingleConnection() throws IOException {
		log("Waiting for one (1) connection on [%s:%s]", listeningIface, listeningPort);
		
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

	@Override
	public ServerSessionCallback acceptSession(final ServerSession session) {
		final SimpleServerSessionCallback cb = new SimpleServerSessionCallback() {
			@Override
			public Runnable requestShell(final ServerSession ss) throws IOException {
				//clientThreadFactory.newThread(new JavaScriptShell(session)).start();
				return new Runnable() {
					public void run() {
						clientThreadFactory.newThread(new JavaScriptShell(session)).start();
					}
				};
			}
			@Override
			public Runnable requestPtyReq(final ServerSession ss, final PtySettings pty) throws IOException {
				return new Runnable() {       
                    public void run() {
                            System.out.println("Client requested " + pty.term + " pty");
                    }
                };
            }			
			
			@Override
			public void requestWindowChange(ServerSession ss,
					int term_width_columns, int term_height_rows,
					int term_width_pixels, int term_height_pixels)
					throws IOException {
				log("Requested Window Change");
				super.requestWindowChange(ss, term_width_columns, term_height_rows,
						term_width_pixels, term_height_pixels);
			}
			
			@Override
			public Runnable requestSubsystem(final ServerSession ss, final String subsystem) throws IOException {
				log("Requested Subsystem: [%s]", subsystem);
				return super.requestSubsystem(ss, subsystem);
			}
			
			@Override
			public Runnable requestEnv(final ServerSession ss, final String name, final String value) throws IOException {
				log("Requested Env: [%s:%s]", name, value);
				return super.requestEnv(ss, name, value);
			}
			
			@Override
			public Runnable requestExec(final ServerSession ss, final String command) throws IOException {
				log("Requested Exec: [%s]", command);
				if("PING".equalsIgnoreCase(command)) {
					return new Runnable() {
						public void run() {
							OutputStream os = null;
							try {
								os = ss.getStdin(); 
								os.write("PONG".getBytes(Charset.forName("UTF8")));
								os.flush();
							} catch (Exception ex) {
								throw new RuntimeException("Failed to write PONG", ex);
							} finally {
								try { ss.close(); } catch (Exception x) { /* No Op */ }
							}
						}
					};
				}
				return super.requestExec(ss, command);
			}
		};
		return cb;
	}

	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.ServerAuthenticationCallback#initAuthentication(ch.ethz.ssh2.ServerConnection)
	 */
	@Override
	public String initAuthentication(final ServerConnection sc) {		
		return "=====================================\r\nWelcome to the Ganymed SSH Server\r\nBeware of the barking dog!\r\n=====================================\r\n";
	}

	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.ServerAuthenticationCallback#getRemainingAuthMethods(ch.ethz.ssh2.ServerConnection)
	 */
	@Override
	public String[] getRemainingAuthMethods(final ServerConnection sc) {		
		return AuthMethod.toSSHNames(remainingAuthMethods);
	}

	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.ServerAuthenticationCallback#authenticateWithNone(ch.ethz.ssh2.ServerConnection, java.lang.String)
	 */
	@Override
	public AuthenticationResult authenticateWithNone(final ServerConnection sc, final String username) {
		return (username!=null && "nsa-agent".equalsIgnoreCase(username.trim())) ? AuthenticationResult.SUCCESS : AuthenticationResult.FAILURE;
	}

	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.ServerAuthenticationCallback#authenticateWithPassword(ch.ethz.ssh2.ServerConnection, java.lang.String, java.lang.String)
	 */
	@Override
	public AuthenticationResult authenticateWithPassword(final ServerConnection sc, final String username, final String password) {
		remainingAuthMethods.remove(AuthMethod.PASSWORD);
		return (AUTH_PASS.equals(password) && AUTH_USER.equals(username))  ? AuthenticationResult.SUCCESS : AuthenticationResult.FAILURE;
	}

	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.ServerAuthenticationCallback#authenticateWithPublicKey(ch.ethz.ssh2.ServerConnection, java.lang.String, java.lang.String, byte[], byte[])
	 */
	@Override
	public AuthenticationResult authenticateWithPublicKey(final ServerConnection sc, final String username, final String algorithm, final byte[] publickey, final byte[] signature) {
		remainingAuthMethods.remove(AuthMethod.PUBLICKEY);
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.io.CloseListener#onClosed(java.io.Closeable, java.lang.Throwable)
	 */
	@Override
	public void onClosed(Socket closeable, Throwable cause) {
		final int localPort = closeable.getLocalPort();
		if(clientSockets.remove(localPort)!=null) {
			log("Closed Client Connection from [%s]", closeable);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.io.CloseListener#onReset(java.io.Closeable)
	 */
	@Override
	public void onReset(final Socket resetCloseable) {
		/* No Op */
	}
	

}
