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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

import com.heliosapm.utils.ssh.terminal.URLRewriter.Rewritten;

/**
 * <p>Title: SSHJMXConnector</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.protocol.sshjmxmp.SSHJMXConnector</code></p>
 */

public class SSHJMXConnector implements JMXConnector {
	protected final Rewritten<JMXServiceURL> rewriter;
	protected final Map<String, ?> originalEnv;
	protected JMXConnector delegate = null;
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/**
	 * Creates a new SSHJMXConnector
	 * @param rewriter The SSH bits-and-pieces
	 * @param env The environment
	 */
	public SSHJMXConnector(final Rewritten<JMXServiceURL> rewriter, final Map<String, ?> env) {
		this.rewriter = rewriter;
		this.originalEnv = env;
		try {
			delegate = JMXConnectorFactory.newJMXConnector(rewriter.getRewritten(), null);
		} catch (Exception ex) {
			throw new RuntimeException("Unexpected error creating SSHJMXMP delegate to [" + rewriter.getRewritten() + "]", ex);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#connect()
	 */
	@Override
	public void connect() throws IOException {
		if(connected.compareAndSet(false, true)) {
			try {
				delegate.connect();
			} catch (Exception ex) {
				connected.set(false);
				throw new IOException("Failed to connec to [" + rewriter.getRewritten() + "]", ex);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#close()
	 */
	@Override 
	public void close() throws IOException {
		if(connected.compareAndSet(true, false)) {
			Exception e = null;
			try {
				delegate.close();
			} catch (Exception ex) {
				e = ex;
			}
			try {rewriter.close(); } catch (Exception x) {/* No Op */}
			if(e!=null) {
				throw new IOException("Failed to close", e);
			}			
		}
	}
	

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#connect(java.util.Map)
	 */
	@Override
	public void connect(final Map<String, ?> env) throws IOException {
		if(env!=null) {
			final Map<String, Object> map = (Map<String, Object>)env; 
			final Map<String, Object> tomap = (Map<String, Object>)originalEnv;
			for(Map.Entry<String, ?> entry: map.entrySet()) {
				if(entry.getValue()!=null && !tomap.containsKey(entry.getKey())) {
					tomap.put(entry.getKey(), entry.getValue());
				}
			}
		}
		delegate.connect(originalEnv);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getMBeanServerConnection()
	 */
	@Override
	public MBeanServerConnection getMBeanServerConnection() throws IOException {
		return delegate.getMBeanServerConnection();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getMBeanServerConnection(javax.security.auth.Subject)
	 */
	@Override
	public MBeanServerConnection getMBeanServerConnection(final Subject delegationSubject) throws IOException {
		return delegate.getMBeanServerConnection(delegationSubject);
	}


	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#addConnectionNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addConnectionNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) {
		delegate.addConnectionNotificationListener(listener, filter, handback);

	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#removeConnectionNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeConnectionNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
		delegate.removeConnectionNotificationListener(listener);

	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#removeConnectionNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void removeConnectionNotificationListener(final NotificationListener l, final NotificationFilter f, final Object handback) throws ListenerNotFoundException {
		delegate.removeConnectionNotificationListener(l, f, handback);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getConnectionId()
	 */
	@Override
	public String getConnectionId() throws IOException {
		return delegate.getConnectionId();
	}

}
