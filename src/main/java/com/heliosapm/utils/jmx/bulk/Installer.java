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
package com.heliosapm.utils.jmx.bulk;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.loading.MLet;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.heliosapm.utils.http.HTTPJarServer;
import com.heliosapm.utils.http.HTTPJarServer.CompletionKeyFuture;
import com.heliosapm.utils.jar.JarBuilder;
import com.heliosapm.utils.jmx.JMXHelper;


/**
 * <p>Title: Installer</p>
 * <p>Description: Installs the BulkJMXService on a remote JVM</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.bulk.Installer</code></p>
 */

public class Installer {
	/** The singleton instance */
	private static volatile Installer instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** Instance logger */
	private final Logger log = Logger.getLogger(getClass().getName());
	
	/** The content key for the Http Jar Server */
	private final String CONTENT_KEY;
	
	/**
	 * Acquires and returns the Installer singleton
	 * @return the Installer singleton
	 */
	public static Installer getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new Installer();
				}
			}
		}
		return instance;
	}

	public static final ObjectName BULK_SERVICE_OBJECT_NAME = JMXHelper.objectName(BulkJMXService.class);
	public static final ObjectName BULK_SERVICE_CL_OBJECT_NAME = JMXHelper.objectName(BULK_SERVICE_OBJECT_NAME.toString() + ",ext=ClassLoader");

	public boolean install(final JMXServiceURL jmxUrl, final String[] credentials, final long timeout, final TimeUnit unit, final Runnable onComplete) {
		if(jmxUrl==null) throw new IllegalArgumentException("The passed JMXServiceURL was null");
		JMXConnector connector = null;
		try {
			final HashMap<String, Object> env = new HashMap<String, Object>();
			if(credentials!=null && credentials.length==2 && credentials[0]!=null) {
				env.put(JMXConnector.CREDENTIALS, credentials);
			}
			connector = JMXConnectorFactory.connect(jmxUrl, env);
			MBeanServerConnection server = connector.getMBeanServerConnection();
			final CompletionKeyFuture ckf = HTTPJarServer.getInstance().getCompletionKey(CONTENT_KEY, timeout, unit, onComplete);
			if(server.isRegistered(BULK_SERVICE_CL_OBJECT_NAME)) {
				try { server.unregisterMBean(BULK_SERVICE_CL_OBJECT_NAME); } catch (Exception x) {/* No Op */}
			}
			server.createMBean(MLet.class.getName(), BULK_SERVICE_CL_OBJECT_NAME, new Object[]{new URL[]{ckf.getRetrievalURL()},true}, new String[]{URL[].class.getName(), "boolean"});
			if(server.isRegistered(BULK_SERVICE_OBJECT_NAME)) {
				try { server.unregisterMBean(BULK_SERVICE_OBJECT_NAME); } catch (Exception x) {/* No Op */}
			}
			server.createMBean(BulkJMXService.class.getName(), BULK_SERVICE_OBJECT_NAME, new Object[0], new String[0]);
			return true;
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Failed to install BulkJMXService to [" + jmxUrl + "]", ex);
			ex.printStackTrace(System.err);
			return false;
		} finally {
			if(connector!=null) try { connector.close(); } catch (Exception x) {/* No Op */}
		}
	}
	/**
	 * Creates a new Installer
	 */
	private Installer() {
		File tmp = null;
		try {
			tmp = File.createTempFile("BulkJMXService", ".jar");
			File f = new JarBuilder(tmp, true)
					.res("com.heliosapm.utils.jmx.bulk").classLoader(com.heliosapm.utils.jmx.bulk.BulkJMXService.class)
					.filterPath(true, ".*BulkJMXService.*?\\.class")
					.apply()
				.manifestBuilder().done()
				.build();
			CONTENT_KEY = HTTPJarServer.getInstance().register(f);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			if(tmp!=null) {
				if(!tmp.delete()) tmp.deleteOnExit();
			}
		}
	}

}
