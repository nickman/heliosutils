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
package com.sun.jmx.remote.generic;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * <p>Title: SocketDefaults</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.sun.jmx.remote.generic.SocketDefaults</code></p>
 */

public class SocketDefaults {
    private static final Socket DSOCKET = new Socket();
    private static final ServerSocket SSOCKET; 
    
    static {
    	try {
    		SSOCKET = new ServerSocket();
    	} catch (Exception ex) {
    		throw new RuntimeException(ex);
    	}
    }

    public static boolean clientKeepAlive() {
    	try { return DSOCKET.getKeepAlive(); } catch (Exception ex) { return false; }
    }
    
    public static boolean clientReuseAddress() {
    	try { return DSOCKET.getReuseAddress(); } catch (Exception ex) { return false; }
    }
    
    public static boolean clientTcpNoDelay() {
    	try { return DSOCKET.getTcpNoDelay(); } catch (Exception ex) { return false; }
    }
    
    
    
    public static int clientReceiveBufferSize() {
    	try { return DSOCKET.getReceiveBufferSize(); } catch (Exception ex) { return 8192; }
    }
    
    public static int clientSendBufferSize() {
    	try { return DSOCKET.getSendBufferSize(); } catch (Exception ex) { return 8192; }
    }
    
    public static int clientSoTimeout() {
    	try { return DSOCKET.getSoTimeout(); } catch (Exception ex) { return 0; }
    }
    
    public static int serverReceiveBufferSize() {
    	try { return SSOCKET.getReceiveBufferSize(); } catch (Exception ex) { return 43690; }
    }

    public static boolean serverReuseAddress() {
    	try { return SSOCKET.getReuseAddress(); } catch (Exception ex) { return true; }
    }

    public static int serverSoTimeout() {
    	try { return SSOCKET.getSoTimeout(); } catch (Exception ex) { return 0; }
    }
    
	private SocketDefaults() {}

}
