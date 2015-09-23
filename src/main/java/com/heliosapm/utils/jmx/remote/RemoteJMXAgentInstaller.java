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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.logging.Logger;

import javax.management.remote.jmxmp.JMXMPConnector;

import com.heliosapm.shorthand.attach.vm.AttachProvider;
import com.heliosapm.shorthand.attach.vm.agent.AgentBootstrap;
import com.heliosapm.utils.jar.JarBuilder;

/**
 * <p>Title: RemoteJMXAgentInstaller</p>
 * <p>Description: </p> 
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
	private MappedByteBuffer installJarBytes = null;
	
	public static final String VERSION = "0.1";
	
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
	
	public void writeAgent(final OutputStream os) {
		try {
			Channels.newChannel(os).write(installJarBytes.slice());
		} catch (Exception ex) {
			throw new RuntimeException("Failed to write agent jar", ex);
		}
	}
	
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
			.res("META-INF/services").classLoader(JMXMPConnector.class).apply()
			.manifestBuilder()
				.agentClass(AgentBootstrap.class.getName())
				.preMainClass(AgentBootstrap.class.getName())
				.autoCreatedBy()
				.name("HeliosAPM Remote JMX Agent Installer")
				.implVersion(VERSION)
			.done()
			.build();
		RandomAccessFile raf = null;
		FileChannel fc = null;
		try {
			raf = new RandomAccessFile(installJar, "r");
			fc = raf.getChannel();
			installJarBytes = fc.map(MapMode.READ_ONLY, 0, installJar.length());
			installJarBytes.load();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load install jar bytes from [" + installJar + "]", ex);
		} finally {
			if(raf!=null) try { raf.close(); } catch (Exception x) {/* No Op */}
			if(fc!=null) try { fc.close(); } catch (Exception x) {/* No Op */}
		}
		
	}
	
	public static void main(String[] args) {
		File f = getInstance().installJar;
		System.out.println(f);
		ByteBuffer b = getInstance().installJarBytes.slice();		
		System.out.println(String.format("cap:%s, pos:%s, lim:%s, readable:%s", b.capacity(), b.position(), b.limit(), b.limit()-b.position()));
		byte[] bytes = new byte[10];		
		b.get(bytes);
		System.out.println(String.format("cap:%s, pos:%s, lim:%s, readable:%s", b.capacity(), b.position(), b.limit(), b.limit()-b.position()));
	}

}
