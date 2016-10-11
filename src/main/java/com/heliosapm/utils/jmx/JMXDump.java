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
package com.heliosapm.utils.jmx;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.heliosapm.utils.lang.StringHelper;

/**
 * <p>Title: JMXDump</p>
 * <p>Description: Command line JMXMP connect and dump server info</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.JMXDump</code></p>
 */

public class JMXDump {


	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String jmxUrl = args[0];
		final String[] credentials;
		if(args.length >= 3) {
			credentials = new String[]{args[1], args[2]};			
		} else {
			credentials = null;
		}
		log("Connecting to [%s]....", jmxUrl);
		JMXConnector connector = null;
		try {
			final JMXServiceURL serviceURL = JMXHelper.serviceUrl(jmxUrl);
			final Map<String , Object> env = new HashMap<String, Object>();
			if(credentials!=null) {
				env.put(JMXConnector.CREDENTIALS, credentials);
			}
			final long start = System.currentTimeMillis();
			connector = JMXConnectorFactory.connect(serviceURL, env);
			final long elapsed = System.currentTimeMillis() - start;
			log("Connected to [%s] in %s ms.", jmxUrl, elapsed);
			final MBeanServerConnection server = connector.getMBeanServerConnection();
			final String[] runtimeAttrNames = JMXHelper.getAttributeNames(JMXHelper.MXBEAN_RUNTIME_ON, server);
			final Map<String, Object> runtimeAttrs = JMXHelper.getAttributes(JMXHelper.MXBEAN_RUNTIME_ON, runtimeAttrNames);
			@SuppressWarnings("unchecked")
			final Map<String, String> sysProps = (Map<String, String>)runtimeAttrs.remove("SystemProperties");
			for(Map.Entry<String, Object> entry: runtimeAttrs.entrySet()) {
				if(entry.getValue() instanceof Long) {
					entry.setValue(new Date((Long)entry.getValue()));
				}
			}
			final String beanPrint = StringHelper.printBeanNames(runtimeAttrs);
			log("Runtime Attributes:\n%s", beanPrint);			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			System.exit(-1);
		} finally {
			if(connector!=null) {
				try { connector.close(); } catch (Exception x) {/* No Op */}
			}
		}
	}
	
	public static void log(final Object fmt, final Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}

}
