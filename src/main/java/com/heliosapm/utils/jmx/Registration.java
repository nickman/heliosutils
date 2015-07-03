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

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.heliosapm.utils.io.CloseableService;

/**
 * <p>Title: Registration</p>
 * <p>Description: Tracks an MBean registration by {@link MBeanServer} and {@link ObjectName} 
 * so they can be efficiently unloaded.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.Registration</code></p>
 */

public class Registration implements Closeable {
	/** The registration ObjectName */
	protected final ObjectName objectName;
	/** The target registration MBeanServer */
	protected final MBeanServerConnection mbeanServer;
	/** Op names to call before unregistering */
	protected final String[] closeOps;
	/** The MBeanServer identifier */
	protected final String serverId;
	
	/** Null arguments const */
	public static final Object[] NULL_ARGS = {};
	/** Null signature const */
	public static final String[] NULL_SIG = {};
	
	/**
	 * Creates a new Registration and registers it with the close service
	 * @param objectName The registration ObjectName
	 * @param mbeanServer The target registration MBeanServer
	 * @param closeOps Op names to call before unregistering
	 */
	public static void registerCloseable(final ObjectName objectName, final MBeanServerConnection mbeanServer, final String...closeOps) {
		final Registration reg = new Registration(objectName, mbeanServer, closeOps);
		CloseableService.getInstance().register(reg);
	}
	
	/**
	 * Creates a new Registration
	 * @param objectName The registration ObjectName
	 * @param mbeanServer The target registration MBeanServer
	 * @param closeOps Op names to call before unregistering
	 */
	public Registration(final ObjectName objectName, final MBeanServerConnection mbeanServer, final String...closeOps) {
		if(objectName==null) throw new IllegalArgumentException("The passed ObjectName was null");
		if(objectName.isPattern()) throw new IllegalArgumentException("The passed ObjectName [" + objectName + "] is a pattern");
		if(mbeanServer==null) throw new IllegalArgumentException("The passed MBeanServer was null");
		this.objectName = objectName;
		this.mbeanServer = mbeanServer;
		this.closeOps = closeOps;
		serverId = getServerId(this.mbeanServer);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return new StringBuilder(objectName.toString()).append("[").append(serverId).append("], CloseOps:")
				.append(closeOps==null ? "[]" : Arrays.toString(closeOps))
				.toString();
	}
	
	/**
	 * Returns an MBeanServer identifier for the passed server
	 * @param server The server
	 * @return the id
	 */
	protected static String getServerId(final MBeanServerConnection server) {
		String id = null;
		try { id = server.getAttribute(JMXHelper.objectName(ManagementFactory.RUNTIME_MXBEAN_NAME), "Name").toString(); } catch (Exception x) {/* No Op */}
		if(id==null) {			
			try { id = server.getDefaultDomain(); } catch (Exception x) {/* No Op */}
		}
		if(id==null) {
			id = server.toString();
		}
		return id;
	}

	/**
	 * <p>Invokes any shutdown directives and unregisters the MBean</p>
	 * {@inheritDoc}
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		if(closeOps!=null) {
			for(String op: closeOps) {
				try { mbeanServer.invoke(objectName, op, NULL_ARGS, NULL_SIG); } catch (Exception x) {/* No Op */}
			}
		}
		try { JMXHelper.unregisterMBean(mbeanServer, objectName); } catch (Exception x) {/* No Op */}
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((objectName == null) ? 0 : objectName.hashCode());
		result = prime * result
				+ ((serverId == null) ? 0 : serverId.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Registration other = (Registration) obj;
		if (objectName == null) {
			if (other.objectName != null)
				return false;
		} else if (!objectName.equals(other.objectName))
			return false;
		if (serverId == null) {
			if (other.serverId != null)
				return false;
		} else if (!serverId.equals(other.serverId))
			return false;
		return true;
	}
	
	
	
}
