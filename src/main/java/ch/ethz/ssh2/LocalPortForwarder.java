/*
 * Copyright (c) 2006-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */

package ch.ethz.ssh2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.ObjectName;

import ch.ethz.ssh2.channel.ChannelManager;
import ch.ethz.ssh2.channel.LocalAcceptThread;

import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.ssh.terminal.SSHService;

/**
 * A <code>LocalPortForwarder</code> forwards TCP/IP connections to a local
 * port via the secure tunnel to another host (which may or may not be identical
 * to the remote SSH-2 server). Checkout {@link Connection#createLocalPortForwarder(int, String, int)}
 * on how to create one.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class LocalPortForwarder implements LocalPortForwarderMBean, Runnable
{
	final ChannelManager cm;

	final String host_to_connect;

	final int port_to_connect;

	final LocalAcceptThread lat;
	
	final AtomicBoolean open = new AtomicBoolean(false);
	final AtomicBoolean clean = new AtomicBoolean(true);
	final ObjectName objectName;
	final AtomicReference<ScheduledFuture<?>> handle = new AtomicReference<ScheduledFuture<?>>(null); 

	LocalPortForwarder(ChannelManager cm, int local_port, String host_to_connect, int port_to_connect)
			throws IOException
	{
		this.cm = cm;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		lat = new LocalAcceptThread(cm, local_port, host_to_connect, port_to_connect, this);
		lat.setDaemon(true);
		lat.start();
		open.set(true);
		objectName = register();
	}
	
	protected ObjectName register() {
		ObjectName on = JMXHelper.objectName(new StringBuilder("com.heliosapm.ssh.localtunnel:remoteHost=")
		.append(this.host_to_connect)
		.append(",remotePort=").append(this.port_to_connect)
		.append(",iface=").append(getLocalIface())
		.append(",localPort=").append(getLocalPort())
	);
	if(JMXHelper.isRegistered(on)) {
		if(JMXHelper.getAttribute(on, "Open")) {
			System.err.println("LocalPortForward [" + on + "] still open and registered");
		} else {
			final ScheduledFuture<?> h = handle.getAndSet(null);
			if(h!=null) {
				h.cancel(true);								
				try { JMXHelper.unregisterMBean(objectName); } catch (Exception x) {/* No Op */}
			}
		}
	}
	JMXHelper.registerMBean(this, on);
	LocalPortForwardWatcher.getInstance(host_to_connect, port_to_connect).addForwarder(this);
	return on;		
	}

	LocalPortForwarder(ChannelManager cm, InetSocketAddress addr, String host_to_connect, int port_to_connect)
			throws IOException
	{
		this.cm = cm;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		lat = new LocalAcceptThread(cm, addr, host_to_connect, port_to_connect, this);
		lat.setDaemon(true);
		lat.start();
		open.set(true);
		objectName = register();
	}

	/**
	 * Return the local socket address of the {@link ServerSocket} used to accept connections.
	 * @return
	 */
	public InetSocketAddress getLocalSocketAddress()
	{
		return (InetSocketAddress) lat.getServerSocket().getLocalSocketAddress();
	}
	
	public void run() {
		try { 
			clean.set(false); 
			close(); 
		} catch (Exception ex) {
			System.err.println("Failed to close");
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * Stop TCP/IP forwarding of newly arriving connections.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		
		LocalPortForwardWatcher.getInstance(host_to_connect, port_to_connect).removeForwarder(this);
		try { lat.stopWorking(); } catch (Exception x) {/* No Op */}
		open.set(false);		
		handle.set(SSHService.getInstance().schedule(new Runnable(){
			public void run() {
				final ScheduledFuture<?> h = handle.getAndSet(null);
				if(h!=null && !h.isCancelled()) {
					try { JMXHelper.unregisterMBean(objectName); } catch (Exception x) {/* No Op */}
				}
			}
		}, 60, TimeUnit.SECONDS));
	}
	
	public String getAcceptThreadState() {
		if(lat==null) return "NULL";
		return lat.getState().name();
	}
	
	public String getAcceptThreadName() {
		if(lat==null) return "NULL";
		return lat.getName();		
	}
	
	public long getAcceptThreadId() {
		if(lat==null) return -1L;;
		return lat.getId();		
	}
	
	public boolean isServerSocketBound() {
		if(lat==null) return false;
		ServerSocket ss = lat.getServerSocket();
		if(ss==null) return false;
		return ss.isBound();		
	}
	
	public boolean isServerSocketClosed() {
		if(lat==null) return true;
		ServerSocket ss = lat.getServerSocket();
		if(ss==null) return true;		
		return ss.isClosed();		
	}
	
	
	

	/**
	 * Returns the remote host
	 * @return the remote host
	 */
	public String getHost() {
		return host_to_connect;
	}
	
	/**
	 * Returns the local bind interface
	 * @return the local bind interface
	 */
	public String getLocalIface() {
		return (lat!=null && lat.getServerSocket()!=null && lat.getServerSocket().getLocalSocketAddress()!=null) ?
					 ((InetSocketAddress)lat.getServerSocket().getLocalSocketAddress()).getHostString() : null;
	}

	/**
	 * Returns the local listening port
	 * @return the local listening port
	 */
	public int getLocalPort() {
		return (lat!=null && lat.getServerSocket()!=null && lat.getServerSocket().getLocalSocketAddress()!=null) ?
					 ((InetSocketAddress)lat.getServerSocket().getLocalSocketAddress()).getPort() : -1;
	}

	//lat.getServerSocket().getLocalSocketAddress()
	
	/**
	 * Returns 
	 * @return the port_to_connect
	 */
	public int getPort() {
		return port_to_connect;
	}
	
	public boolean isOpen() {
		return open.get();
	}
	
	public long getBytesUp() {
		return lat.getBytesUp();
	}
	
	public long getBytesDown() {
		return lat.getBytesDown();
	}
	
	public long getAccepts() {
		return lat.getAccepts();
	}
	
	public long getDeltaBytesUp() {
		return lat.getDeltaBytesUp();
	}
	
	public long getDeltaBytesDown() {
		return lat.getDeltaBytesDown();
	}
	
	public long getDeltaAccepts() {
		return lat.getDeltaAccepts();
	}
	
	
	public long getTimeTillUnregister() {
		final ScheduledFuture<?> h = handle.get();
		return h==null ? -1L : h.getDelay(TimeUnit.SECONDS);
	}
	
}
