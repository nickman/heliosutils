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

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.remote.JMXServiceURL;

import com.heliosapm.utils.ssh.terminal.ConnectInfo;
import com.heliosapm.utils.ssh.terminal.SSHService;
import com.heliosapm.utils.ssh.terminal.WrappedLocalPortForwarder;

/**
 * <p>Title: URLRewriter</p>
 * <p>Description: Rewrites a URL to connect to an endpoint via a created tunnel</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.nash.fixtures.URLRewriter</code></p>
 */

public class URLRewriter {
	private static volatile URLRewriter instance = null;
	private static final Object lock = new Object();
	
	

	/*
		standard = Pattern.compile('jdbc:.*?:(?:\\W+)+?(.*?):(\\d+)[:|/].*$');
		rac = Pattern.compile('jdbc:.*?@.*?ADDRESS_LIST.*?TCP.*?HOST.*?PORT.*?');
		racHostPort = Pattern.compile("\\(HOST=(.*?)\\).*?\\(PORT=(\\d+)\\)");
	 */
	
	public static final Pattern STANDARD_JDBC = Pattern.compile("jdbc:.*?:(?:\\W+)+?(.*?):(\\d+)[:|/].*$");
	public static final Pattern RAC_JDBC = Pattern.compile("jdbc:.*?@.*?ADDRESS_LIST.*?TCP.*?HOST.*?PORT.*?", Pattern.CASE_INSENSITIVE);
	public static final Pattern RAC_HOST_PORT = Pattern.compile("\\(HOST=(.*?)\\).*?\\(PORT=(\\d+)\\)", Pattern.CASE_INSENSITIVE);
	
	private final Map<String, HostPort> hostPorts = new ConcurrentHashMap<String, HostPort>();
	private final Map<Object, Rewritten<?>> rewrites = new ConcurrentHashMap<Object, Rewritten<?>>();
	
	public static final String FMT = "%s:%s->%s:%s";
	
	public static URLRewriter getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new URLRewriter();
				}
			}
		}
		return instance;
	}
	
	private URLRewriter() {
		
	}
	
	
	
	public HostPort get(final String hostName, final int port) {
		return get(hostName, 22, hostName, port);
	}
	
	public HostPort get(final String relayHost, final int relayPort, final String hostName, final int port) {
		return get(relayHost, relayPort, hostName, port, null);
	}
	
	public HostPort get(final String relayHost, final int relayPort, final String hostName, final int port, final ConnectInfo connectInfo) {
		if(hostName==null || hostName.trim().isEmpty()) throw new IllegalArgumentException("Passed host was null or empty");						
		if(port < 1) throw new IllegalArgumentException("Invalid port: " + port);
		if(relayPort < 1) throw new IllegalArgumentException("Invalid relay port: " + port);
		final String host = hostName.trim();
		final String relay = relayHost==null ? host : relayHost.trim();			
		final String key = String.format(FMT, relay, relayPort, host, port);
		HostPort hostPort = hostPorts.get(key);
		if(hostPort==null) {
			synchronized(hostPorts) {
				hostPort = hostPorts.get(key);
				if(hostPort==null) {
					hostPort = new HostPort(relayHost, relayPort, host, port, connectInfo);
					hostPorts.put(key, hostPort);
				}
			}
		}
		hostPort.claims.incrementAndGet();
		return hostPort;
	}

	@SuppressWarnings("unchecked")
	public Rewritten<String> rewriteJdbcUrl(final String jdbcUrl, final String relayHost, final int relayPort, final Properties info) {
		if(jdbcUrl==null || jdbcUrl.trim().isEmpty()) throw new IllegalArgumentException("Passed jdbcUrl was null or empty");
		final ConnectInfo authInfo = info==null ? null : ConnectInfo.fromProperties(info);
		Rewritten<String> rw = (Rewritten<String>)rewrites.get(jdbcUrl);
		if(rw==null) {
			synchronized(rewrites) {
				rw = (Rewritten<String>)rewrites.get(jdbcUrl);
				if(rw==null) {
					Matcher m = STANDARD_JDBC.matcher(jdbcUrl);
					final Set<HostPort> hostPorts = new HashSet<HostPort>(3);
					String rewritenUrl = null;
					if(m.matches()) {
//						hostPorts.add(get(relayHost, relayPort, m.group(1), Integer.parseInt(m.group(2))));
						hostPorts.add(new HostPort(relayHost, relayPort, m.group(1), Integer.parseInt(m.group(2)), authInfo));
					} else {
						m = RAC_JDBC.matcher(jdbcUrl);
						if(m.matches()) {
							m = RAC_HOST_PORT.matcher(jdbcUrl);
							while(m.find()) {
//								hostPorts.add(get(relayHost, relayPort, m.group(1), Integer.parseInt(m.group(2))));
								hostPorts.add(new HostPort(relayHost, relayPort, m.group(1), Integer.parseInt(m.group(2)), authInfo));
							}
						}
					}
					if(hostPorts.isEmpty()) {
						return new Rewritten<String>(jdbcUrl, jdbcUrl);
					}
					rewritenUrl = jdbcUrl;
					for(HostPort hp: hostPorts) {
						rewritenUrl = rewritenUrl.replaceFirst(Pattern.quote(hp.host), hp.getLocalBind()).replaceFirst(Pattern.quote("" + hp.port), "" + hp.getLocalPort());
//						rewritenUrl = rewritenUrl.replace(hp.host, hp.getLocalBind()).replace("" + hp.port, "" + hp.getLocalPort());
					}
					rw = new Rewritten<String>(jdbcUrl, rewritenUrl, hostPorts.toArray(new HostPort[hostPorts.size()]));					
				}
			}
		}
		return rw;
	}
	
	@SuppressWarnings("unchecked")
	public Rewritten<JMXServiceURL> rewrite(final JMXServiceURL jmxUrl, final Map<String, Object> env, final String relayHost, final int relayPort) {
		if(jmxUrl==null) throw new IllegalArgumentException("Passed jmxUrl was null");
		Rewritten<JMXServiceURL> rw = (Rewritten<JMXServiceURL>)rewrites.get(jmxUrl);
		if(rw==null) {
			synchronized(rewrites) {
				rw = (Rewritten<JMXServiceURL>)rewrites.get(jmxUrl);
				if(rw==null) {
					final int port = jmxUrl.getPort();
					final String host = jmxUrl.getHost();
					ConnectInfo ci = null;
					HostPort hp = null;
					if(env!=null && !env.isEmpty()) {
						ci = ConnectInfo.fromMap(env);
						hp = get(relayHost, relayPort, host, port, ci);
					} else {
						hp = get(relayHost, relayPort, host, port);
					}
					
					final String url = jmxUrl.toString().replace(host, hp.getLocalBind()).replace("" + port, "" + hp.getLocalPort()).replace(":sshjmxmp", ":jmxmp");
					try {
						JMXServiceURL rewrittenJmxUrl = new JMXServiceURL(url);
						rw = new Rewritten<JMXServiceURL>(jmxUrl, rewrittenJmxUrl, hp);
						rewrites.put(jmxUrl, rw);
					} catch (Exception ex) {						
						throw new RuntimeException("Failed to create rewritten JMXServiceURL [" + jmxUrl  + "] from [" + url + "]", ex);
					}
				}
			}
		}
		return rw;				
	}
	
	public Rewritten<JMXServiceURL> rewrite(final JMXServiceURL jmxUrl, final Map<String, Object> env, final String relayHost) {
		return rewrite(jmxUrl, env, relayHost, 22);
	}
	
	public Rewritten<JMXServiceURL> rewrite(final JMXServiceURL jmxUrl, final Map<String, Object> env) {
		if(jmxUrl==null) throw new IllegalArgumentException("Passed jmxUrl was null");
		return rewrite(jmxUrl, env, jmxUrl.getHost(), 22);
	}
	
	
	@SuppressWarnings("unchecked")
	public Rewritten<URI> rewrite(final URI uri, final String relayHost, final int relayPort) {
		if(uri==null) throw new IllegalArgumentException("Passed URI was null");
		Rewritten<URI> rw = (Rewritten<URI>)rewrites.get(uri);
		if(rw==null) {
			synchronized(rewrites) {
				rw = (Rewritten<URI>)rewrites.get(uri);
				if(rw==null) {
					final int port = uri.getPort();
					final String host = uri.getHost();
					HostPort hp = get(relayHost, relayPort, host, port);
					final String tmp = uri.toString().replace(host, hp.getLocalBind()).replace("" + port, "" + hp.getLocalPort());
					try {
						URI rewrittenUri = new URI(tmp);
						rw = new Rewritten<URI>(uri, rewrittenUri, hp);
						rewrites.put(uri, rw);
					} catch (Exception ex) {						
						throw new RuntimeException("Failed to create rewritten URI [" + uri + "] from [" + tmp + "]", ex);
					}
				}
			}
		}
		return rw;				
	}
	
	public Rewritten<URI> rewrite(final URI uri, final String relayHost) {
		return rewrite(uri, relayHost, 22);
	}
	
	public Rewritten<URI> rewrite(final URI uri) {
		if(uri==null) throw new IllegalArgumentException("Passed URI was null");
		return rewrite(uri, uri.getHost(), 22);
	}
	
	
	
	public class HostPort implements Closeable {
		public final String host;
		public final int port;
		public final String relayHost;
		public final int relayPort;
		private final ConnectInfo authInfo;
		private volatile WrappedLocalPortForwarder lpf;
		private volatile WrappedStreamForwarder lsf;
		private final AtomicInteger claims = new AtomicInteger(0);
		
		
		
		
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((host == null) ? 0 : host.hashCode());
			result = prime * result + port;
			result = prime * result + ((relayHost == null) ? 0 : relayHost.hashCode());
			result = prime * result + relayPort;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			HostPort other = (HostPort) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (host == null) {
				if (other.host != null)
					return false;
			} else if (!host.equals(other.host))
				return false;
			if (port != other.port)
				return false;
			if (relayHost == null) {
				if (other.relayHost != null)
					return false;
			} else if (!relayHost.equals(other.relayHost))
				return false;
			if (relayPort != other.relayPort)
				return false;
			return true;
		}
		
		private HostPort(final String relayHost, final int relayPort, final String host, final int port) {
			this(relayHost, relayPort, host, port, null);
		}

		private HostPort(final String relayHost, final int relayPort, final String host, final int port, final ConnectInfo authInfo) {
			this.host = host;
			this.port = port;
			this.relayHost = relayHost;
			this.relayPort = relayPort;
			this.authInfo = authInfo;
//			lpf = SSHService.getInstance().connect(relayHost, relayPort, authInfo).dedicatedTunnel(host, port);
//			claims.set(1);
//			SSHService.getInstance().registerForReconnect(lpf);
		}
		
		public String toString() {
			return String.format(FMT, relayPort, relayPort, host, port);
		}
		
		public WrappedLocalPortForwarder getTunnel() {
			if(lpf==null) {
				synchronized(this) {
					if(lpf==null) {
						lpf = SSHService.getInstance().connect(relayHost, relayPort, authInfo).dedicatedTunnel(host, port);
					}
				}
			}
			return lpf;
		}
		
		public WrappedStreamForwarder getStreamTunnel() {
			if(lsf==null) {
				synchronized(this) {
					if(lsf==null) {
						lsf = SSHService.getInstance().connect(relayHost, relayPort, authInfo).dedicatedStreamTunnel(host, port);
					}
				}
			}
			return lsf;
		}
		
		
		public int getLocalPort() {			
			return lpf==null ? -1 : lpf.getLocalPort();
		}
		
		public String getLocalBind() {
			//return lpf.getLocalSocketAddress().getAddress().getCanonicalHostName();
			return "127.0.0.1";
		}
		
		public void close() {
			final int cnt = claims.decrementAndGet();
			if(cnt < 1) {
				hardClose();
			}
		}
		
		void hardClose() {			
			if(lpf!=null) try { lpf.close(); lpf = null; } catch (Exception ex) {}
			if(lsf!=null) try { lsf.close(); lsf = null; } catch (Exception ex) {}
			hostPorts.remove(toString());
		}

		private URLRewriter getOuterType() {
			return URLRewriter.this;
		}
		
	}
	
	public class Rewritten<T> implements Closeable {
		private final HostPort[] hostPorts;
		private final T original;
		private final T rewritten;
		private final Map<String, Object> context = new HashMap<String, Object>(); 
		
		/**
		 * Creates a new Rewritten
		 * @param original
		 * @param rewritten
		 * @param hostPorts
		 */
		public Rewritten(final T original, final T rewritten, final HostPort...hostPorts) {
			this.original = original;
			this.rewritten = rewritten;
			this.hostPorts = hostPorts;
		}
		
		@Override
		public void close() throws IOException {
			for(HostPort hp: hostPorts) {
				try { hp.close(); } catch (Exception x) {/* No Op */}
			}			
		}

		public HostPort[] getHostPorts() {
			return hostPorts.clone();
		}
		
		public Rewritten<T> addContext(final String key, final Object value) {
			context.put(key, value);
			return this;
		}
		
		public Map<String, Object> getContext() {
			return context;
		}

		public T getOriginal() {
			return original;
		}

		public T getRewritten() {
			return rewritten;
		}
		
		
		
		
		
	}
	
	
}
