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
package com.heliosapm.utils.jmx.protocol.sshjmxmp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.generic.GenericConnector;
import javax.management.remote.generic.MessageConnection;

import com.heliosapm.utils.jmx.protocol.WrappedJMXConnector;

/**
 * <p>Title: ClientProvider</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.protocol.sshjmxmp.ClientProvider</code></p>
 */

public class ClientProvider implements JMXConnectorProvider {
	/** The protocol name */
	public static final String PROTOCOL_NAME = "sshjmxmp";

	/**
	 * Creates a new ClientProvider
	 */
	public ClientProvider() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnectorProvider#newJMXConnector(javax.management.remote.JMXServiceURL, java.util.Map)
	 */
	@Override
	public JMXConnector newJMXConnector(final JMXServiceURL serviceURL, final Map<String, ?> environment) throws IOException {
		if (!serviceURL.getProtocol().equals(PROTOCOL_NAME)) {
			throw new MalformedURLException("Protocol not [" + PROTOCOL_NAME + "]: " +
						    serviceURL.getProtocol());
		}
		final Map<String, Object> env = (Map<String, Object>)environment; 
		//final ConnectInfo ci = ConnectInfo.fromMap(environment);
//		final Rewritten<JMXServiceURL> rewriter = URLRewriter.getInstance().rewrite(serviceURL, (Map<String, Object>) environment);
//		return WrappedJMXConnector.addressable(new SSHJMXConnector(rewriter, environment), serviceURL);
//		SSHTunnelMessageConnection
		final MessageConnection messageConnection = new SSHTunnelMessageConnection(serviceURL, env);
		env.put(GenericConnector.MESSAGE_CONNECTION, messageConnection);
		final GenericConnector gc = new GenericConnector(env);
		return WrappedJMXConnector.addressable(gc, serviceURL);
	}

}
