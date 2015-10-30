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
package com.heliosapm.utils.jmx.remote;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.loading.MLet;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServerMBean;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.jmxmp.JMXMPConnector;
import javax.management.remote.jmxmp.JMXMPConnectorServer;

import jsr166e.LongAdder;

import com.heliosapm.shorthand.attach.vm.AttachProvider;
import com.heliosapm.shorthand.attach.vm.agent.AgentBootstrap;
import com.heliosapm.utils.http.HTTPJarServer;
import com.heliosapm.utils.http.HTTPJarServer.CompletionKeyFuture;
import com.heliosapm.utils.io.InstrumentedInputStream;
import com.heliosapm.utils.io.NIOHelper;
import com.heliosapm.utils.jar.JarBuilder;
import com.heliosapm.utils.jmx.JMXHelper;

/**
 * <p>Title: RemoteJMXAgentInstaller</p>
 * <p>Description: Installer to installa JMXMP server to a remote JVM</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.remote.RemoteJMXAgentInstaller</code></p>
 */

public class RemoteJMXAgentInstaller {
	/** Static class logger */
	private final static Logger log = Logger.getLogger(RemoteJMXAgentInstaller.class.getName());
	/** The singleton instance */
	private static volatile RemoteJMXAgentInstaller instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	private File installJar = null;
	private ByteBuffer installJarBytes = null;
	private String httpContentKey;
	
	/** The version of the installer */
	public static final String VERSION = "0.1";
	/** The content key for remote class loaders */
	public static final String CONTENT_KEY = "JMXMPAgent.jar";
	
	/** The JMX ObjectName for the installed JMX Server */
	public static final ObjectName AGENT_SERVICE_OBJECT_NAME = JMXHelper.objectName(JMXMPConnectorServer.class);
	/** The JMX ObjectName for the installed JMX Server's classloader */
	public static final ObjectName AGENT_SERVICE_CL_OBJECT_NAME = JMXHelper.objectName(AGENT_SERVICE_OBJECT_NAME.toString() + ",ext=ClassLoader");
	
	
	/**
	 * Acquires and returns the RemoteJMXAgentInstaller singleton
	 * @return the RemoteJMXAgentInstaller singleton
	 */
	public static RemoteJMXAgentInstaller getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new RemoteJMXAgentInstaller();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Writes the agent jar and content to the passed output stream
	 * @param os The output stream to write to
	 */
	public void writeAgent(final OutputStream os) {
		try {
			Channels.newChannel(os).write(installJarBytes.slice());
		} catch (Exception ex) {
			throw new RuntimeException("Failed to write agent jar", ex);
		}
	}
	
	/**
	 * Returns the size of the agent jar content in bytes
	 * @return the size of the agent jar content
	 */
	public int getAgentSize() {
		return installJarBytes.capacity();
	}
	
	
	
	/**
	 * Creates a new RemoteJMXAgentInstaller
	 */
	private RemoteJMXAgentInstaller() {
		installJar = new JarBuilder()
			.res("com.heliosapm.shorthand.attach").classLoader(AttachProvider.class).apply()
			.res("javax.management.remote").classLoader(JMXMPConnector.class).apply()
			.res("com.sun.jmx.remote").classLoader(JMXMPConnector.class).apply()
			.res("jsr166e").classLoader(LongAdder.class).apply()
			.res("com.heliosapm.utils.io").classLoader(InstrumentedInputStream.class).apply()
			.res("META-INF/services").classLoader(JMXMPConnector.class).apply()
			.manifestBuilder()
				.agentClass(AgentBootstrap.class.getName())
				.preMainClass(AgentBootstrap.class.getName())
				.autoCreatedBy()
				.name("HeliosAPM Remote JMX Agent Installer")
				.implVersion(VERSION)
			.done()
			.build();		
		installJarBytes = NIOHelper.load(installJar, true);		
		httpContentKey = HTTPJarServer.getInstance().register(CONTENT_KEY, installJarBytes);
		log.info("Installed [" + CONTENT_KEY + "] to the HTTPJarServer");
	}
	
	/**
	 * Installs a {@link JMXMPConnectorServer} to the target MBeanServer resolved from the passed {@link JMXServiceURL}.
	 * @param jmxUrl The JMXServiceURL to connect to the target MBeanServer
	 * @param credentials The optional credentials
	 * @param timeout The installation timeout
	 * @param unit The timeout unit
	 * @param onComplete An optional runnable to invoke when the installation is complete
	 * @return a proxy invoker for the installed {@link JMXMPConnectorServer}
	 * FIXME: We need to do something with the connector so it doesn't stay open.
	 */
	public JMXConnectorServerMBean install(final JMXServiceURL jmxUrl, final String[] credentials, final long timeout, final TimeUnit unit, final Runnable onComplete) {
		if(jmxUrl==null) throw new IllegalArgumentException("The passed JMXServiceURL was null");
		JMXConnector connector = null;
		JMXConnectorServerMBean proxy = null;
		try {
			final HashMap<String, Object> env = new HashMap<String, Object>();
			if(credentials!=null && credentials.length==2 && credentials[0]!=null) {
				env.put(JMXConnector.CREDENTIALS, credentials);
			}
			connector = JMXConnectorFactory.connect(jmxUrl, env);
			MBeanServerConnection server = connector.getMBeanServerConnection();
			final CompletionKeyFuture ckf = HTTPJarServer.getInstance().getCompletionKey(httpContentKey, timeout, unit, onComplete);
			if(server.isRegistered(AGENT_SERVICE_CL_OBJECT_NAME)) {
				try { server.unregisterMBean(AGENT_SERVICE_CL_OBJECT_NAME); } catch (Exception x) {/* No Op */}
			}
			server.createMBean(MLet.class.getName(), AGENT_SERVICE_CL_OBJECT_NAME, new Object[]{new URL[]{ckf.getRetrievalURL()},true}, new String[]{URL[].class.getName(), "boolean"});
			if(server.isRegistered(AGENT_SERVICE_OBJECT_NAME)) {
				try { server.unregisterMBean(AGENT_SERVICE_OBJECT_NAME); } catch (Exception x) {/* No Op */}
			}
			server.createMBean(JMXMPConnectorServer.class.getName(), AGENT_SERVICE_OBJECT_NAME, new Object[0], new String[0]);
			proxy = MBeanServerInvocationHandler.newProxyInstance(server, AGENT_SERVICE_OBJECT_NAME, JMXConnectorServerMBean.class, true);
			proxy.start();
			return proxy;
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Failed to install JMXMPConnectorServer to [" + jmxUrl + "]", ex);
			throw new RuntimeException("Failed to install JMXMPConnectorServer to [" + jmxUrl + "]", ex);
		} finally {
			if(proxy==null) {
				if(connector!=null) try { connector.close(); } catch (Exception x) {/* No Op */}
			}
		}
	}

	public static void log(final Object fmt, final Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}
	
	public static void main(String[] args) {
		try {
			final JMXServiceURL jmxUrl = new JMXServiceURL("service:jmx:attach:///[.*?\\.HMaster.*]");
			final JMXConnectorServerMBean proxy = getInstance().install(jmxUrl, null, 15, TimeUnit.SECONDS, null);
			log("JMXMPServer installed. URL: [" + proxy.getAddress() + "]");			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

	}
	
	
//	public static void main(String[] args) {
//		File f = getInstance().installJar;
//		System.out.println(f);
//		ByteBuffer b = getInstance().installJarBytes.slice();		
//		System.out.println(String.format("cap:%s, pos:%s, lim:%s, readable:%s", b.capacity(), b.position(), b.limit(), b.limit()-b.position()));
//		byte[] bytes = new byte[10];		
//		b.get(bytes);
//		System.out.println(String.format("cap:%s, pos:%s, lim:%s, readable:%s", b.capacity(), b.position(), b.limit(), b.limit()-b.position()));
		
//	}

}
