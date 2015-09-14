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
package ch.ethz.ssh2;

import java.net.InetSocketAddress;

/**
 * <p>Title: LocalPortForwarderMBean</p>
 * <p>Description: JMX MBean interface for {@link LocalPortForwarder} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>ch.ethz.ssh2.LocalPortForwarderMBean</code></p>
 */

public interface LocalPortForwarderMBean {
	/**
	 * Returns the remote host
	 * @return the remote host
	 */
	public String getHost();
	
	/**
	 * Returns the local bind interface
	 * @return the local bind interface
	 */
	public String getLocalIface();

	/**
	 * Returns the local listening port
	 * @return the local listening port
	 */
	public int getLocalPort();

	//lat.getServerSocket().getLocalSocketAddress()
	
	/**
	 * Returns 
	 * @return the port_to_connect
	 */
	public int getPort();
	
	public boolean isOpen();
	
	public long getBytesUp();
	
	public long getBytesDown();
	
	public long getAccepts();
	
	public long getTimeTillUnregister();

}
