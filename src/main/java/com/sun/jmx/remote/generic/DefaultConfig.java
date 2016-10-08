/*
 * @(#)file      DefaultConfig.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.27
 * @(#)lastedit  07/03/08
 * @(#)build     @BUILD_TAG_PLACEHOLDER@
 *
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL")(collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://opendmk.dev.java.net/legal_notices/licenses.txt or in the 
 * LEGAL_NOTICES folder that accompanied this code. See the License for the 
 * specific language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file found at
 *     http://opendmk.dev.java.net/legal_notices/licenses.txt
 * or in the LEGAL_NOTICES folder that accompanied this code.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.
 * 
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * 
 *       "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding
 * 
 *       "[Contributor] elects to include this software in this distribution
 *        under the [CDDL or GPL Version 2] license."
 * 
 * If you don't indicate a single choice of license, a recipient has the option
 * to distribute your version of this file under either the CDDL or the GPL
 * Version 2, or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the
 * GPL Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 * 
 */ 

package com.sun.jmx.remote.generic;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import com.heliosapm.utils.config.ConfigurationHelper;
import com.sun.jmx.remote.opt.util.EnvHelp;

public class DefaultConfig {

    /**
     * <p>Name of the attribute that specifies the maximum number of
     * threads used at the server side for each client connection.</p>
     */
    public final static String SERVER_MAX_THREADS =
	"jmx.remote.x.server.max.threads";

    /** 
     * Returns the maximum number of threads used at server side for
     * each client connection.  Its default value is 10.
     */
    public static int getServerMaxThreads(Map env) {
	return (int) EnvHelp.getIntegerAttribute(env, SERVER_MAX_THREADS,
						 10, 1, Integer.MAX_VALUE);
    }

    /**
     * <p>Name of the attribute that specifies the minimum number of
     * threads used at the server side for each client connection.</p>
     */
    public final static String SERVER_MIN_THREADS =
	"jmx.remote.x.server.min.threads";

    /** 
     * Returns the minimum number of threads used at server side for
     * each client connection.  Its default value is 1.
     */
    public static int getServerMinThreads(Map env) {
	return (int) EnvHelp.getIntegerAttribute(env, SERVER_MIN_THREADS,
						 1, 1, Integer.MAX_VALUE);
    }

    /**
     * <p>Name of the attribute that specifies the timeout in
     * milliseconds for a client request to wait for its response.
     * The default value is <code>Long.MAX_VALUE</code>.
     */
    public static final String REQUEST_WAITING_TIME =
	"jmx.remote.x.request.timeout";

    /** 
     * Returns the timeout for a client request.
     * Its default value is <code>Long.MAX_VALUE</code>.
     */
    public static long getRequestTimeout(Map env) {
	return EnvHelp.getIntegerAttribute(env, REQUEST_WAITING_TIME,
					   Long.MAX_VALUE, 0, Long.MAX_VALUE);
    }


    /**
     * <p>Name of the attribute that specifies the timeout in
     * milliseconds for a server to finish connecting with a new client.
     * Zero means no timeout.
     * If a user-specified value is less than or equal to zero, or more than the max value,
     * Zero will be used.
     * The default value is zero.
     */
    public static final String SERVER_SIDE_CONNECTING_TIMEOUT =
	"jmx.remote.x.server.side.connecting.timeout";

    /** 
     * Returns the connecting timeout at server side for a new client, zero means no timeout.
     * If a user-specified value is less than zero, zero will be used.
     * The default value is 0.
     */

    public static long getConnectingTimeout(Map env) {
	long l;

	try {
	    l = EnvHelp.getIntegerAttribute(env, SERVER_SIDE_CONNECTING_TIMEOUT,
					   0, 0, Long.MAX_VALUE);
	} catch (IllegalArgumentException iae) {
	    l = 0;
	}

	return l;
    }

    /**
     * <p>Name of the attribute that specifies a ServerAdmin object.
     * The value associated with this attribute is ServerAdmin object</p>
     */
    public static final String SERVER_ADMIN =
	"com.sun.jmx.remote.server.admin";

    /** 
     * Returns an instance of ServerAdmin.  Its default value is a
     * <code>com.sun.jmx.remote.opt.security.AdminServer</code>.
     */
    public static ServerAdmin getServerAdmin(Map env) {
	ServerAdmin admin;
	final Object o = env.get(SERVER_ADMIN);

	if (o == null) {
	    admin = new com.sun.jmx.remote.opt.security.AdminServer(env);
	} else if (o instanceof ServerAdmin) {
	    admin = (ServerAdmin)o;
	} else {
	    final String msg =
		"The specified attribute \"" + SERVER_ADMIN +
		"\" is not a ServerAdmin object.";
	    throw new IllegalArgumentException(msg);
	}

	return admin;
    }

    /**
     * <p>Name of the attribute that specifies a ClientAdmin object.
     * The value associated with this attribute is ClientAdmin object</p>
     */
    public static final String CLIENT_ADMIN =
	"com.sun.jmx.remote.client.admin";

    /** 
     * Returns an instance of ClientAdmin.  Its default value is a
     * <code>com.sun.jmx.remote.opt.security.AdminClient</code>.
     */
    public static ClientAdmin getClientAdmin(Map env) {
	ClientAdmin admin;
	final Object o = env.get(CLIENT_ADMIN);

	if (o == null) {
	    admin = new com.sun.jmx.remote.opt.security.AdminClient(env);
	} else if (o instanceof ClientAdmin) {
	    admin = (ClientAdmin)o;
	} else {
	    final String msg =
		"The specified attribute \"" + CLIENT_ADMIN +
		"\" is not a ClientAdmin object.";
	    throw new IllegalArgumentException(msg);
	}

	return admin;
    }

    /**
     * <p>Name of the attribute that specifies a
     * <code>SynchroMessageConnectionServer</code> object.  The value
     * associated with this attribute is a
     * <code>SynchroMessageConnectionServer</code> object</p>
     */
    public static final String SYNCHRO_MESSAGE_CONNECTION_SERVER =
	"com.sun.jmx.remote.generic.synchro.server";

    /** 
     * Returns a <code>SynchroMessageConnectionServer</code> object
     * specified in the <code>Map</code> object. Returns null if it is
     * not specified in the map.
     */
    public static SynchroMessageConnectionServer
	    getSynchroMessageConnectionServer(Map env) {
	SynchroMessageConnectionServer ret = null;
	if (env != null) {
	    ret = (SynchroMessageConnectionServer)
		env.get(SYNCHRO_MESSAGE_CONNECTION_SERVER);
	}

	return ret;
    }
    

    // ===============================================================================
    //		CLIENT SOCKET OPTIONS
    // ===============================================================================
    
    
    /**
     * <p>Name of the attribute that specifies whether or not we set
     * set keep alive on the client socket. Its default value is false</p>
     */
    public final static String CLIENT_KEEP_ALIVE = "jmx.client.socket.keepalive";
    
    
    /**
     * Returns the <b>socket keep alive</b> for a client socket connection.
     * The passed env map will be inspected first, then defaults to {@link ConfigurationHelper}.
     * @param env The jmx client's environment map
     * @return the socket keep alive
     */
    public static boolean isClientKeepAlive(final Map<String, ?> env) {
    	final Object v = env.get(CLIENT_KEEP_ALIVE);
    	if(v!=null) return "true".equals(v.toString().trim().toLowerCase());
    	return ConfigurationHelper.getBooleanSystemThenEnvProperty(CLIENT_KEEP_ALIVE, SocketDefaults.clientKeepAlive());
    }


    /**
     * <p>Name of the attribute that specifies the receive buffer size.
     * Its default value is platform dependent (Windows: 8192, Linux: 43690) </p>
     */
    public final static String CLIENT_RECEIVE_BUFF = "jmx.client.socket.recbuff";
    
    /**
     * Returns the <b>socket receive buffer size in bytes</b> for a client socket connection.
     * The passed env map will be inspected first, then defaults to {@link ConfigurationHelper}.
     * @param env The jmx client's environment map
     * @return the socket receive buffer size in bytes
     */
    public static int getClientReceiveBufferSize(final Map<String, ?> env) {
    	try {
    		final Object v = env.get(CLIENT_RECEIVE_BUFF);
    		if(v!=null) return Number.class.isInstance(v) ? ((Number)v).intValue() : new Double(v.toString().trim()).intValue();
    	} catch (Exception x) {/* No Op */}
    	return ConfigurationHelper.getIntSystemThenEnvProperty(CLIENT_RECEIVE_BUFF, SocketDefaults.clientReceiveBufferSize());
    }
    

    /**
     * <p>Name of the attribute that specifies the send buffer size.
     * Its default value is platform dependent (Windows: 8192, Linux: 8192) </p>
     */
    public final static String CLIENT_SEND_BUFF = "jmx.client.socket.sendbuff";
    
    /**
     * Returns the <b>socket send buffer size in bytes</b> for a client socket connection.
     * The passed env map will be inspected first, then defaults to {@link ConfigurationHelper}.
     * @param env The jmx client's environment map
     * @return the socket send buffer size in bytes
     */
    public static int getClientSendBufferSize(final Map<String, ?> env) {
    	try {
    		final Object v = env.get(CLIENT_SEND_BUFF);
    		if(v!=null) return Number.class.isInstance(v) ? ((Number)v).intValue() : new Double(v.toString().trim()).intValue();
    	} catch (Exception x) {/* No Op */}
    	return ConfigurationHelper.getIntSystemThenEnvProperty(CLIENT_SEND_BUFF, SocketDefaults.clientSendBufferSize());
    }
    
    
    /**
     * <p>Name of the attribute that specifies the SO_REUSEADDR socket option.
     * Its default value is platform dependent (Windows: false) </p>
     */
    public final static String CLIENT_REUSE_ADDR = "jmx.client.socket.reuseaddr";
    
    /**
     * Returns the <b>socket reuse address</b> for a client socket connection.
     * The passed env map will be inspected first, then defaults to {@link ConfigurationHelper}.
     * @param env The jmx client's environment map
     * @return the socket reuse address
     */
    public static boolean isClientReuseAddress(final Map<String, ?> env) {
    	final Object v = env.get(CLIENT_REUSE_ADDR);
    	if(v!=null) return "true".equals(v.toString().trim().toLowerCase());
    	return ConfigurationHelper.getBooleanSystemThenEnvProperty(CLIENT_REUSE_ADDR, SocketDefaults.clientReuseAddress());
    }
    
    
    /**
     * <p>Name of the attribute that specifies if tcpnodelay (nagle's algorithm).
     * is disabled. Its default value is false, meaning nagle is in effect. </p>
     */
    public final static String CLIENT_TCP_NODELAY = "jmx.client.socket.tcpnodelay";
    
    /**
     * Returns the <b>socket tcp no delay</b> for a client socket connection.
     * The passed env map will be inspected first, then defaults to {@link ConfigurationHelper}.
     * @param env The jmx client's environment map
     * @return the socket tcp no delay
     */
    public static boolean isClientTcpNoDelay(final Map<String, ?> env) {
    	final Object v = env.get(CLIENT_TCP_NODELAY);
    	if(v!=null) return "true".equals(v.toString().trim().toLowerCase());
    	return ConfigurationHelper.getBooleanSystemThenEnvProperty(CLIENT_TCP_NODELAY, SocketDefaults.clientTcpNoDelay());
    }
    
    
    /**
     * <p>Name of the attribute that specifies an SO_TIMEOUT meaning a timeout
     * on socket blocking time in millis. Its default value is 0 (infinite)</p>
     */
    public final static String CLIENT_SO_TIMEOUT = "jmx.client.socket.sotimeout";
    
    /**
     * Returns the <b>socket so timeout in millis</b> for a client socket connection.
     * The passed env map will be inspected first, then defaults to {@link ConfigurationHelper}.
     * @param env The jmx client's environment map
     * @return the socket so timeout in millis
     */
    public static int getClientSoTimeout(final Map<String, ?> env) {
    	try {
    		final Object v = env.get(CLIENT_SO_TIMEOUT);
    		if(v!=null) return Number.class.isInstance(v) ? ((Number)v).intValue() : new Double(v.toString().trim()).intValue();
    	} catch (Exception x) {/* No Op */}
    	return ConfigurationHelper.getIntSystemThenEnvProperty(CLIENT_SO_TIMEOUT, SocketDefaults.clientSoTimeout());
    }
    

    // ===============================================================================
    //		SERVER SOCKET OPTIONS
    // ===============================================================================
    
    /**
     * <p>Name of the attribute that specifies the server side accepted socket receive buffer size.
     * Its default value is platform dependent (Windows: 8192, Linux: 43690) </p>
     */
    public final static String SERVER_RECEIVE_BUFF = "jmx.server.socket.recbuff";

    /**
     * Returns the <b>socket receive buffer size in bytes</b> for a server accepted socket.
     * The passed env map will be inspected first, then defaults to {@link ConfigurationHelper}.
     * @param env The jmx client's environment map
     * @return the server accepted socket receive buffer size in bytes
     */
    public static int getServerReceiveBufferSize(final Map<String, ?> env) {
    	try {
    		final Object v = env.get(SERVER_RECEIVE_BUFF);
    		if(v!=null) return Number.class.isInstance(v) ? ((Number)v).intValue() : new Double(v.toString().trim()).intValue();
    	} catch (Exception x) {/* No Op */}
    	return ConfigurationHelper.getIntSystemThenEnvProperty(SERVER_RECEIVE_BUFF, SocketDefaults.serverReceiveBufferSize());
    }
    
    
    /**
     * <p>Name of the attribute that specifies the server side accepted socket SO_REUSEADDR option.
     * Its default value is platform dependent (Windows: false, Linux: true) </p>
     */
    public final static String SERVER_REUSE_ADDR = "jmx.server.socket.reuseaddr";
    
    /**
     * Returns the <b>socket reuse address</b> for a server accepted socket.
     * The passed env map will be inspected first, then defaults to {@link ConfigurationHelper}.
     * @param env The jmx client's environment map
     * @return the server accepted socket reuse address
     */
    public static boolean isServerReuseAddress(final Map<String, ?> env) {
    	final Object v = env.get(SERVER_REUSE_ADDR);
    	if(v!=null) return "true".equals(v.toString().trim().toLowerCase());
    	return ConfigurationHelper.getBooleanSystemThenEnvProperty(SERVER_REUSE_ADDR, SocketDefaults.serverReuseAddress());
    }
    
    /**
     * <p>Name of the attribute that specifies the server side accepted socket SO_TIMEOUT meaning a timeout
     * on socket blocking time in millis. Its default value is 0 (infinite)</p>
     */
    public final static String SERVER_SO_TIMEOUT = "jmx.server.socket.sotimeout";
    
    /**
     * Returns the <b>socket so timeout in millis</b> for a server accepted socket.
     * The passed env map will be inspected first, then defaults to {@link ConfigurationHelper}.
     * @param env The jmx client's environment map
     * @return the server accepted socket so timeout in millis
     */
    public static int getServerSoTimeout(final Map<String, ?> env) {
    	try {
    		final Object v = env.get(SERVER_SO_TIMEOUT);
    		if(v!=null) return Number.class.isInstance(v) ? ((Number)v).intValue() : new Double(v.toString().trim()).intValue();
    	} catch (Exception x) {/* No Op */}
    	return ConfigurationHelper.getIntSystemThenEnvProperty(SERVER_SO_TIMEOUT, SocketDefaults.serverReceiveBufferSize());
    }
    
    /**
     * <p>Name of the attribute that specifies the server side socket backlog.
     * Ddefault value is 50 for sockets, but 100 for JMXMP</p>
     */
    public final static String SERVER_BACKLOG = "jmx.server.socket.backlog";
    
    /**
     * Returns the <b>backlog</b> for a server socket.
     * The passed env map will be inspected first, then defaults to {@link ConfigurationHelper}.
     * @param env The jmx client's environment map
     * @return the server socket backlog
     */
    public static int getServerBacklog(final Map<String, ?> env) {
    	try {
    		final Object v = env.get(SERVER_BACKLOG);
    		if(v!=null) return Number.class.isInstance(v) ? ((Number)v).intValue() : new Double(v.toString().trim()).intValue();
    	} catch (Exception x) {/* No Op */}
    	return ConfigurationHelper.getIntSystemThenEnvProperty(SERVER_BACKLOG, 100);
    }

    // ===============================================================================
    
    /**
     * <p>Name of the attribute that specifies a
     * <code>ClientSynchroMessageConnection</code> object.  The value
     * associated with this attribute is a
     * <code>ClientSynchroMessageConnection</code> object</p>
     */
    public static final String CLIENT_SYNCHRO_MESSAGE_CONNECTION =
	"com.sun.jmx.remote.generic.synchro.client";


    /** 
     * Returns a <code>ClientSynchroMessageConnection</code> object
     * specified in the <code>Map</code> object. Returns null if it is
     * not specified in the map.
     */
    public static ClientSynchroMessageConnection
	    getClientSynchroMessageConnection(Map env) {
	ClientSynchroMessageConnection ret = null;
	if (env != null) {
	    ret = (ClientSynchroMessageConnection)
		env.get(CLIENT_SYNCHRO_MESSAGE_CONNECTION);
	}

	return ret;
    }

    /**
     * <p>Name of the attribute that specifies the timeout in
     * milliseconds for a client to wait for its state to become
     * connected.  The default value is 0.</p>
     */
    public static final String TIMEOUT_FOR_CONNECTED_STATE =
	"jmx.remote.x.client.connected.state.timeout";

    /** 
     * Returns the timeout in milliseconds for a client to wait for
     * its state to become connected.  The default timeout is 1
     * second.</p>
     */
    public static long getTimeoutForWaitConnectedState(Map env) {
	return EnvHelp.getIntegerAttribute(env, TIMEOUT_FOR_CONNECTED_STATE,
					   1000, 1, Long.MAX_VALUE);
    }

    /**
     * <p>Name of the attribute that specifies whether or not we set
     * ReuseAddress flag to a Server Socket. Its default value is false</p>
     */
    public final static String SERVER_REUSE_ADDRESS =
	"jmx.remote.x.server.reuse.address";

    /** 
     * Returns a value telling whether or not we set
     * ReuseAddress flag to a Server Socket.  Its default value is false.
     */
    public static boolean getServerReuseAddress(Map env) {
	final Object o;

	if (env == null || (o = env.get(SERVER_REUSE_ADDRESS)) == null)
	    return false;

	if (o instanceof Boolean) {
	    return ((Boolean)o).booleanValue();
	} else if (o instanceof String) {
	    return Boolean.valueOf((String)o).booleanValue();
	}

	throw new IllegalArgumentException("Attribute "+SERVER_REUSE_ADDRESS+
					   " value must be Boolean or String.");
    }

    /**
     * <p>Name of the attribute that specifies whether or not do reconnection if 
     * the client heartbeat
     * gets an InterruptedIOException because of a request timeout.
     * Its default value is false.</p>
     */
    public static final String TIMEOUT_RECONNECTION =
	"jmx.remote.x.client.timeout.reconnection";

    /** 
     * Returns a value telling whether or not we do reconnection if the client
     * heartbeat gets an InterruptedIOException because of a request timeout.
     * Its default value is false.
     */
    public static boolean getTimeoutReconnection(Map env) {
	final Object o;

	if (env == null || (o = env.get(TIMEOUT_RECONNECTION)) == null)
	    return false;

	if (o instanceof Boolean) {
	    return ((Boolean)o).booleanValue();
	} else if (o instanceof String) {
	    return Boolean.valueOf((String)o).booleanValue();
	}

	throw new IllegalArgumentException("Attribute "+TIMEOUT_RECONNECTION+
					   " value must be Boolean or String.");
    }
}
