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
package test.com.heliosapm.utils.ssh.terminal;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.com.heliosutils.BaseTest;

import com.heliosapm.utils.ssh.terminal.ConnectInfo;
import com.heliosapm.utils.ssh.terminal.WrappedConnection;

/**
 * <p>Title: WrappedConnectionTest</p>
 * <p>Description: SSH connection tests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.com.heliosapm.utils.ssh.terminal.WrappedConnectionTest</code></p>
 */

public class WrappedConnectionTest extends BaseTest {
	/** The test SSHServer */
	protected static SSHServer sshServer = null;
	/** The test SSHServer bound interface */
	protected static final String serverIface = "0.0.0.0";
	/** The test SSHServer listening port */
	protected static int serverPort = -1;
	
	/** The connection under test */
	protected WrappedConnection wc = null;
	
	/**
	 * Starts the test SSHServer
	 */
	@BeforeClass
	public static void startServer() {
		sshServer = SSHServer.getInstance(serverIface, 0, getDSAPrivateKey(), getRSAPrivateKey()).start();
		serverPort = sshServer.getListeningPort();		
	}
	
	/**
	 * Stops the test SSHServer 
	 */
	@AfterClass
	public static void stopServer() {
		sshServer.stop();
		sshServer = null;
		serverPort = -1;
	}
	
	/**
	 * Attempts to close the wrapped connection after each test
	 */
	@After
	public void closeWrappedConnection() {
		if(wc!=null) {
			try { wc.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Tests a connection using the special NSA user name 
	 */
	@Test
	public void testNSAConnect() {
		wc = WrappedConnection.connectAndAuthenticate("localhost", serverPort, new ConnectInfo("nsa-agent").setYesManVerifier());
		Assert.assertNotNull("Connection is null", wc);
		Assert.assertTrue("Not connected", wc.isOpen());
		Assert.assertEquals("Did not get PONG", "PONG", wc.execCommand("PING"));
	}

}
