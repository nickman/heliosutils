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
package com.heliosapm.utils.http;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;
import javax.xml.bind.DatatypeConverter;

import jsr166e.LongAdder;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

import com.heliosapm.utils.config.ConfigurationHelper;
import com.heliosapm.utils.io.InstrumentedOutputStream;
import com.heliosapm.utils.io.NIOHelper;
import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.jmx.JMXManagedThreadPool;
import com.heliosapm.utils.jmx.bulk.BulkJMXServiceInstaller;
import com.heliosapm.utils.jmx.bulk.BulkJMXServiceMBean;
import com.heliosapm.utils.lang.StringHelper;
import com.heliosapm.utils.net.LocalHost;
import com.heliosapm.utils.time.SystemClock;
import com.heliosapm.utils.url.URLHelper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * <p>Title: HTTPJarServer</p>
 * <p>Description: A simple HTTP server for serving JARs to support remote http classloading</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.http.HTTPJarServer</code></p>
 */
@SuppressWarnings("restriction")
public class HTTPJarServer implements HttpHandler, HTTPJarServerMBean, Runnable {
	/** The singleton instance */
	private static volatile HTTPJarServer instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();

	/** Instance logger */
	private final Logger log = Logger.getLogger(getClass().getName());
	/** The thread pool management mbean ObjectName */
	public final ObjectName executorObjectName = JMXHelper.objectName("com.heliosapm.http:service=HttpJarServerThreadPool");
	/** The http server's management mbean ObjectName */
	public final ObjectName objectName = JMXHelper.objectName("com.heliosapm.http:service=HttpJarServer");
	/** The registered jar files keyed by the http path */
	public final NonBlockingHashMap<String, ByteBuffer> cachedContent = new NonBlockingHashMap<String, ByteBuffer>(); 
	/** The Http Server */
	private HttpServer server;
	/** The listening port */
	private final int port;
	/** The listening iface */
	private final String iface;
	/** The listener backlog */
	private final int backlog;
	
	/** The name or ip address the server should be accessed by from remote servers */
	private final String remoteHostName = LocalHost.hostName();
	
	/** A long adder to track the number of bytes written out */
	private final LongAdder bytesDown = new LongAdder();
	/** A long adder to track the number of expiries */
	private final LongAdder expiryCount = new LongAdder();
	/** A long adder to track the number of completions */
	private final LongAdder completionCount = new LongAdder();
	/** A map of served content buffers counts keyed by the content name */
	private final NonBlockingHashMap<String, LongAdder> servedContentCounts = new NonBlockingHashMap<String, LongAdder>(); 
	/** The request executor */
	private final JMXManagedThreadPool executor;
	/** The expiration queue poller thread */
	private final Thread expireThread;
	/** The server running flag */
	final AtomicBoolean running = new AtomicBoolean(true);
	
	/** The jar loader http path */
	public static final String JAR_CONTEXT = "/classpath/jar";
	/** Arg splitter */
	public static final Pattern EQ_SPLIT = Pattern.compile("=");
	/** Args splitter */
	public static final Pattern AMP_SPLIT = Pattern.compile("&");
	/** Completion callback key. Used to determine when the issued client has consumed a given resource */
	public static final String CKEY = "ckey";
    /** The config prop key for the listening port */
    public static final String PROP_LISTEN_PORT = "httpjarserver.listener.port";
    /** The default listening port */
    public static final int DEFAULT_LISTEN_PORT = 0;
    /** The config prop key for the server binding interface */
    public static final String PROP_BIND_FACE = "httpjarserver.listener.iface";
    /** The default server binding interface */
    public static final String DEFAULT_BIND_IFACE = "127.0.0.1";
    /** The config prop key for the http server's listener backlog */
    public static final String PROP_BACKLOG = "httpjarserver.listener.backlog";
    /** The default http server's listener backlog  */
    public static final int DEFAULT_BACKLOG = 128;
	
	
	/**
	 * Acquires and returns the HTTPJarServer singleton
	 * @return the HTTPJarServer singleton
	 */
	public static HTTPJarServer getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new HTTPJarServer();
				}
			}
		}
		return instance;
	}
	
	public static void main(String[] args) {
		log("HTTPJarFileServer Test:  Java Home: [" + System.getProperty("java.home") + "]");
		JMXHelper.fireUpJMXMPServer("0.0.0.0", 2147, JMXHelper.getHeliosMBeanServer());
		try {
			//final JMXServiceURL jmxUrl = new JMXServiceURL("service:jmx:attach:///[.*?\\.Groovy.*]");
			//final JMXServiceURL jmxUrl = new JMXServiceURL("service:jmx:attach://17796");
			final JMXServiceURL jmxUrl = new JMXServiceURL("service:jmx:attach:///[.*?\\.jboss.*]");
			final BulkJMXServiceMBean proxy = BulkJMXServiceInstaller.getInstance().install(jmxUrl, null, 15, TimeUnit.SECONDS, null);
			if(proxy==null) {
				log("Failed install to [" + jmxUrl + "]");
			}
			log("Completed install to [" + jmxUrl + "]");
			int names = 0;
			int attrs = 0;
//			log("Warmup");
//			for(int i = 0; i < 1000; i++) {
//				proxy.getAttributes(JMXHelper.objectName("*:*"), null, new String[]{"*"});
//				proxy.getCompressedAttributes(JMXHelper.objectName("*:*"), null, new String[]{"*"});
//				
//			}
			long start = System.currentTimeMillis();
			Map<ObjectName, Map<String, Object>> map = proxy.getAttributes(JMXHelper.objectName("*:*"), null, new String[]{"*"});
			long elapsed = System.currentTimeMillis() - start;
			log("NonCompressed: " + elapsed + " ms.");
			start = System.currentTimeMillis();
			byte[] bmap = proxy.getCompressedAttributes(JMXHelper.objectName("*:*"), null, new String[]{"*"});
			elapsed = System.currentTimeMillis() - start;
			log("Compressed: " + elapsed + " ms.");
			
			for(Map.Entry<ObjectName, Map<String, Object>> entry: map.entrySet()) {
				log("ObjectName: %s", entry.getKey());
				names++;
				for(Map.Entry<String, Object> attr: entry.getValue().entrySet()) {
					log("\tAttr [%s]: [%s]", attr.getKey(), attr.getValue());
					attrs++;
				}
			}
			log("\n\t Complete. MBeans: %s, Attributes: %s", names, attrs);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);			
		} finally {
			SystemClock.sleep(1000000);
			instance.stop();
		}
//		final HTTPJarServer server = HTTPJarServer.getInstance();
//		try {
//			File tmp = new File("/tmp/bulk.jar");
//			File f = new JarBuilder(tmp, true)
//				//.res("com.heliosapm.utils.jmx.bulk").classLoader(com.heliosapm.utils.jmx.bulk.BulkJMXService.class)
//					.res("com.heliosapm.utils.").classLoader(com.heliosapm.utils.jmx.bulk.BulkJMXService.class)
//					.recurse(true)
//					//.filterPath(true, ".*BulkJMXService.*?\\.class")
//					.filterPath(true, ".*?\\.class")
//					.apply()
//				.manifestBuilder().done()
//				.build();
//			log("JAR File: [%s]", f);
//			server.register(f);
//			log("JAR available at [http://127.0.0.1:%s%s/%s]", server.getPort(), JAR_CONTEXT, "bulk.jar");
//			JMXHelper.fireUpJMXMPServer("0.0.0.0", 2147, JMXHelper.getHeliosMBeanServer());
//			
//			Thread.currentThread().join();
//		} catch (Exception ex) {
//			ex.printStackTrace(System.err);
//		}
	}
	
	
	public static void log(final Object fmt, final Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}
	
	/**
	 * Registers the passed file to be served through the Http server.
	 * @param jarFile The file to register
	 * @return the fully qualified content key
	 */
	public String register(final File jarFile) {
		if(jarFile==null) throw new IllegalArgumentException("The passed file was null");
		if(!jarFile.canRead()) throw new IllegalArgumentException("The passed file [" + jarFile + "] cannot be read");
		final String key = JAR_CONTEXT + "/" + jarFile.getName();
		boolean added = false;
		if(!cachedContent.containsKey(key)) {
			synchronized(cachedContent) {
				if(!cachedContent.containsKey(key)) {
					final ByteBuffer bb = NIOHelper.load(jarFile, false);		
					cachedContent.put(key, bb);
					this.servedContentCounts.put(key, new LongAdder());
					added = true;
				}
			}
		}
		if(!added) {
			throw new RuntimeException("The context [" + key + "] is already registered");
		}
		server.createContext(key, this);
		return key;
	}
	
	/**
	 * Registers the passed URL to have it's content served through the Http server.
	 * @param url The URL to be served
	 */
	public void register(final URL url) {
		if(url==null) throw new IllegalArgumentException("The passed url was null");
		final String key = JAR_CONTEXT + "/" + url.getFile();
		boolean added = false;
		if(!cachedContent.containsKey(key)) {
			synchronized(cachedContent) {
				if(!cachedContent.containsKey(key)) {
					final ByteBuffer bb = NIOHelper.load(url);		
					cachedContent.put(key, bb);
					this.servedContentCounts.put(key, new LongAdder());
					added = true;
				}
			}
		}
		if(!added) {
			throw new RuntimeException("The context [" + key + "] is already registered");
		}
		server.createContext(key, this);
	}
	
	/**
	 * Unregisters the cached content keyed with the passed key
	 * @param key The content key
	 * @return true if the content was removed, false if no content 
	 */
	public boolean unregister(final String key) {
		if(key==null || key.trim().isEmpty()) return false;
		final ByteBuffer bb = cachedContent.remove(key);
		if(bb!=null) {
			NIOHelper.clean(bb);
		}
		server.removeContext(key);
		servedContentCounts.remove(key);
		return bb!=null;
	}
	
	
	/**
	 * Registers the passed file to be served through the Http server.
	 * The URL to get the content will be <b><code>/classpath/jar/&lt;unqualified file name&gt;</code></b>.
	 * @param jarFile The file to register
	 * @return The URL to access this jar
	 */
	public URL registerHandler(final File jarFile) {
		if(jarFile==null) throw new IllegalArgumentException("The passed file is null");
		if(!jarFile.canRead()) throw new IllegalArgumentException("The passed file [" + jarFile + "] cannot be read");
		final ByteBuffer bb = NIOHelper.load(jarFile, false);
		final String key = JAR_CONTEXT + "/" + jarFile.getName();
		cachedContent.put(key, bb);
		server.createContext(key, new HttpHandler(){
			@Override
			public void handle(final HttpExchange exch) throws IOException {
				final boolean acceptsGZip = supportsGZip(exch);
				final Headers hdrs = exch.getResponseHeaders();				
//				hdrs.add("Content-Type", "application/octet-stream");//"application/java-archive");
				
				hdrs.add("Access-Control-Allow-Origin", "*");
				hdrs.add("Access-Control-Allow-Methods", "GET");
				hdrs.add("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, Origin, User-Agent, DNT, Cache-Control, X-Mx-ReqToken, Keep-Alive, X-Requested-With, If-Modified-Since");
				if(acceptsGZip) {
					hdrs.add("Content-Encoding", "gzip");
				}
				exch.sendResponseHeaders(200, bb.capacity());
				final OutputStream os = exch.getResponseBody();
				final ByteBuffer dup = bb.duplicate();
				log.info("Bytes Remaining:" + dup.remaining());
				final InstrumentedOutputStream instros = new InstrumentedOutputStream(os, bytesDown, null);
				if(acceptsGZip) {
					final GZIPOutputStream gzip = new GZIPOutputStream(instros);
					Channels.newChannel(gzip).write(dup);
					gzip.flush();					
					gzip.close();
				} else {
					Channels.newChannel(instros).write(dup);
				}
				instros.flush();
				instros.close();				
				os.flush();
				os.close();
			}
		});
		return URLHelper.toURL(new StringBuilder("http://127.0.0.1:").append(port).append(key));
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.http.HTTPJarServerMBean#getBytesTransmitted()
	 */
	@Override
	public long getBytesTransmitted() {		
		return bytesDown.longValue();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.http.HTTPJarServerMBean#resetStats()
	 */
	@Override
	public void resetStats() {
		bytesDown.reset();		
		expiryCount.reset();
		completionCount.reset();
		for(LongAdder ad: this.servedContentCounts.values()) {
			ad.reset();
		}
	}
	
	/**
	 * Determines if the client that requested the passed HttpExchange supports gzip 
	 * @param exch The HttpExchange to test
	 * @return true if the client supports gzip, false otherwise
	 */
	protected static boolean supportsGZip(final HttpExchange exch) {
		final Headers rhdrs = exch.getRequestHeaders();
		final List<String> accepted = rhdrs.get("Accept-Encoding");
		if(accepted==null || accepted.isEmpty()) return false;
		
		for(String s: accepted) {
			final String[] encs = s.split(",");
			final Set<String> acceptedEncodings = new HashSet<String>(accepted);
			for(String e: encs) {
				acceptedEncodings.add(e.trim());
			}
			if(acceptedEncodings.contains("gzip")) return true;
		}
		return false;
	}

	
	/**
	 * Creates a new HTTPJarServer
	 */
	private HTTPJarServer() {
		try { 
			if(JMXHelper.isRegistered(executorObjectName)) {
				try { JMXHelper.unregisterMBean(executorObjectName); } catch (Exception x) {/* No Op */}
			}
			executor = new JMXManagedThreadPool(executorObjectName, getClass().getSimpleName(), 1, 8, 128, 60000, 128, 99, true);
			final int _port = ConfigurationHelper.getIntSystemThenEnvProperty(PROP_LISTEN_PORT, DEFAULT_LISTEN_PORT);
			backlog = ConfigurationHelper.getIntSystemThenEnvProperty(PROP_BACKLOG, DEFAULT_BACKLOG);
			iface = ConfigurationHelper.getSystemThenEnvProperty(PROP_BIND_FACE, DEFAULT_BIND_IFACE);
			server = HttpServer.create(new InetSocketAddress(iface, _port), backlog);
			server.setExecutor(executor);
			server.createContext(JAR_CONTEXT, this);
			server.start();
			port = server.getAddress().getPort();
			expireThread = new Thread(this, getClass().getSimpleName() + "ExpirationThread");
			expireThread.setDaemon(true);
			expireThread.start();
			JMXHelper.registerMBean(this, objectName);
			log.info(StringHelper.banner("Started HttpJarServer on [%s]", server.getAddress()));
		} catch (Exception ex) {
			throw new RuntimeException("Failed to start HttpServer", ex);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		log.info("ExpirationThread Started");
		while(true) {
			try {
				final CompletionKey ck = completionKeyExpirations.poll(3000, TimeUnit.MILLISECONDS);
				if(ck!=null) {
					ck.expire();
				}
			} catch (Exception ex) {
				if(!running.get()) {
					log.info("ExpirationThread Exiting");
					break;
				}
				if(Thread.interrupted()) Thread.interrupted();
			} 
		}
	}
	
	private void increment(final String key) {
		final LongAdder la = this.servedContentCounts.get(key);
		if(la!=null) la.increment();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.http.HTTPJarServerMBean#getPort()
	 */
	@Override
	public int getPort() {
		return port;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.http.HTTPJarServerMBean#getBacklog()
	 */
	@Override
	public int getBacklog() {
		return backlog;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.http.HTTPJarServerMBean#getRemoteHostName()
	 */
	@Override
	public String getRemoteHostName() {
		return remoteHostName;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.http.HTTPJarServerMBean#getIface()
	 */
	@Override
	public String getIface() {
		return iface;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.http.HTTPJarServerMBean#getAvailablePaths()
	 */
	@Override
	public String[] getAvailablePaths() {		
		return cachedContent.keySet().toArray(new String[0]);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.http.HTTPJarServerMBean#stop()
	 */
	@Override
	public void stop() {
		synchronized(lock) {
			try { server.stop(3); } catch (Exception x) {/* No Op */}
			running.set(false);
			expireThread.interrupt();
			executor.shutdownNow();
			try { JMXHelper.unregisterMBean(executorObjectName); } catch (Exception x) {/* No Op */}
			try { JMXHelper.unregisterMBean(objectName); } catch (Exception x) {/* No Op */}
			instance = null;
		}
	}
	
	
	/**
	 * Registers an expiring completion key for the passed content key and returns a future to track it
	 * @param contentKey The content key of the content to track the delivery of
	 * @param timeout The delivery timeout
	 * @param unit The timeout unit
	 * @param onComplete Optional runnable to execute on successful completion
	 * @return The future
	 */
	public CompletionKeyFuture getCompletionKey(final String contentKey, final long timeout, final TimeUnit unit, final Runnable onComplete) {
		if(contentKey==null || contentKey.trim().isEmpty()) throw new IllegalArgumentException("The passed content key was empty");
		if(unit==null) throw new IllegalArgumentException("The passed timeout unit was null");
		if(timeout < 1L) throw new IllegalArgumentException("Invalid timeout period: [" + timeout + "]");
		final String key = contentKey.trim();
		if(!cachedContent.containsKey(key)) throw new IllegalArgumentException("The passed content key [" + contentKey + "] is not mapped");
		final CompletionKey ck = new CompletionKey(timeout, unit, String.format("http://127.0.0.1:%s%s?%s=", getPort(), contentKey, CKEY), onComplete);
		completionKeyExpirations.add(ck);
		return ck;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
	 */
	@Override
	public void handle(final HttpExchange exch) throws IOException {
		final String path = exch.getHttpContext().getPath();
		final Map<String, String> args = parseQuery(exch);
		final String completionKey = args.get(CKEY);
		
		final Headers hdrs = exch.getResponseHeaders();
		hdrs.add("Access-Control-Allow-Origin", "*");
		hdrs.add("Access-Control-Allow-Methods", "GET");
		hdrs.add("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, Origin, User-Agent, DNT, Cache-Control, X-Mx-ReqToken, Keep-Alive, X-Requested-With, If-Modified-Since");
		final boolean acceptsGZip = supportsGZip(exch);
		if(acceptsGZip) {
			hdrs.add("Content-Encoding", "gzip");
		}
		
		final ByteBuffer bb = cachedContent.get(path);
		if(bb==null) {
			log.severe("CachedContent for path [" + path + "] not found");
			exch.sendResponseHeaders(404, 0);
			exch.close();
		}
		exch.sendResponseHeaders(200, bb.capacity());
		final OutputStream os = exch.getResponseBody();
		final ByteBuffer dup = bb.duplicate();
		final InstrumentedOutputStream instros = new InstrumentedOutputStream(os, bytesDown, null);
		if(acceptsGZip) {
			final GZIPOutputStream gzip = new GZIPOutputStream(instros);
			Channels.newChannel(gzip).write(dup);
			gzip.flush();					
			gzip.close();
		} else {
			Channels.newChannel(instros).write(dup);
		}
		instros.flush();
		instros.close();				
		os.flush();
		os.close();		
		try { exch.close(); } catch (Exception x) {/* No Op */}
		if(completionKey!=null) {
			for(Iterator<CompletionKey> iter = completionKeyExpirations.iterator(); iter.hasNext();) {
				final CompletionKey ck = iter.next();
				if(ck.key.equals(completionKey.toUpperCase())) {
					iter.remove();
					ck.complete();
					break;
				}
			}			
		}
		increment(path);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.http.HTTPJarServerMBean#getExpiryCount()
	 */
	@Override
	public long getExpiryCount() {
		return expiryCount.longValue();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.http.HTTPJarServerMBean#getCompletionCount()
	 */
	@Override
	public long getCompletionCount() {
		return completionCount.longValue();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.http.HTTPJarServerMBean#getServedCounts()
	 */
	@Override
	public HashMap<String, Long> getServedCounts() {
		final HashMap<String, Long> map = new HashMap<String, Long>(servedContentCounts.size());
		for(Map.Entry<String, LongAdder> entry: servedContentCounts.entrySet()) {
			map.put(entry.getKey(), entry.getValue().longValue());
		}
		return map;
	}
	
	static Map<String, String> parseQuery(final HttpExchange exch) {
		final Map<String, String> args = new HashMap<String, String>();
		if(exch!=null) {
			final URI queryURI = exch.getRequestURI();
			if(queryURI!=null) {
				final String queryString = queryURI.getQuery();
				final String[] argPairs = AMP_SPLIT.split(queryString);
				for(String argPair : argPairs) {
					if(argPair==null || argPair.trim().isEmpty()) continue;
					final String[] keyValue = EQ_SPLIT.split(argPair.trim());
					args.put(keyValue[0].trim().toLowerCase(), keyValue[1].trim().toLowerCase());
				}
			}
		}
		return args;
	}
	
	/** Serial number factory for Completion keys */
	private final AtomicLong ckserial = new AtomicLong(0);
	/** The expirtion queue for registered completion keys */
	private final DelayQueue<CompletionKey> completionKeyExpirations = new DelayQueue<CompletionKey>();
	
	public interface CompletionKeyFuture extends Future<Boolean> {
		/**
		 * Returns the retrieval URL used to retrieved the designated content and trip the completion event.
		 * @return the retrieval URL
		 */
		public URL getRetrievalURL();
		
	}
	
	private class CompletionKey implements Delayed, CompletionKeyFuture {
		final long delay; 		
		final long serial = ckserial.incrementAndGet();
		final AtomicBoolean complete = new AtomicBoolean(false);
		final AtomicBoolean cancelled = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		final String key = DatatypeConverter.printHexBinary(UUID.randomUUID().toString().getBytes());
		final Runnable onComplete;
		final URL retrievalURL;
		
		/**
		 * Creates a new CompletionKey
		 * @param delay The delay the key will wait for until it expires
		 * @param unit The unit of the delay
		 * @param retrievalUrlPrefix The retrieval URL prefix to which they key will be appended to create the full URL.
		 * @param onComplete An optional runnable to run on completion
		 */
		CompletionKey(final long delay, final TimeUnit unit, final String retrievalUrlPrefix, final Runnable onComplete) {
			this.delay = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(delay, unit);
			this.retrievalURL = URLHelper.toURL(retrievalUrlPrefix + key);
			this.onComplete = onComplete;
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.utils.http.HTTPJarServer.CompletionKeyFuture#getRetrievalURL()
		 */
		@Override
		public URL getRetrievalURL() {
			return retrievalURL;
		}
		
		/**
		 * Completes this key, confirming that the target client consumed it
		 * @return true if confirmed, false if already cancelled.
		 */
		boolean complete() {
			if(cancelled.get()) return false;
			complete.set(true);
			latch.countDown();			
			if(onComplete!=null) {
				executor.execute(onComplete);
			}
			completionCount.increment();
			return true;
		}
		
		/**
		 * Expires this CompletionKey
		 */
		void expire() {
			if(complete.get()) return;
			cancelled.set(true);
			complete.set(false);
			latch.countDown();
			expiryCount.increment();
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "CompletionKey:" + serial + ":" + key;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(final Delayed o) {
			final long d = o.getDelay(TimeUnit.MILLISECONDS);
			if(delay==d) {
				if(o instanceof CompletionKey) {
					return serial < ((CompletionKey)o).serial ? -1 : 1;
				}
			}
			return delay > d ? -1 : 1;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
		 */
		@Override
		public long getDelay(final TimeUnit unit) {			
			return unit.convert(delay - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		}
		
		
		
		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#cancel(boolean)
		 */
		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			final boolean removed = completionKeyExpirations.remove(this);			
			complete.set(false);
			final boolean firstToCancel = cancelled.compareAndSet(false, true);
			latch.countDown();
			return removed && firstToCancel;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#get()
		 */
		@Override
		public Boolean get() throws InterruptedException, ExecutionException {
			latch.await();
			return complete.get();
		}
		
		@Override
		public Boolean get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			if(cancelled.get()) return false;
			if(latch.getCount()>0) {
				if(latch.await(timeout, unit)) {
					return complete.get();
				} else {
					throw new TimeoutException();
				}
			}
			return complete.get();
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#isCancelled()
		 */
		@Override
		public boolean isCancelled() {			
			return cancelled.get();
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#isDone()
		 */
		@Override
		public boolean isDone() {
			return complete.get();
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CompletionKey other = (CompletionKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}

		private HTTPJarServer getOuterType() {
			return HTTPJarServer.this;
		}
		
		
		
		
		
	}

}
