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

import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.ssh.terminal.SSHService;

import ch.ethz.ssh2.channel.ChannelManager;
import ch.ethz.ssh2.channel.LocalAcceptThread;
import jsr166e.AccumulatingLongAdder;
import jsr166e.LongAdder;

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
	/** An empty and inert LocalPortForwarder for use as a placeholder */
	public static final LocalPortForwarder PLACEHOLDER = new LocalPortForwarder();
	
	final ChannelManager cm;

	final String host_to_connect;

	final int port_to_connect;

	final LocalAcceptThread lat;
	final LongAdder bytesUp;
	final LongAdder bytesDown;
	final LongAdder accepts;
	
	
	final AtomicBoolean open = new AtomicBoolean(false);
	final AtomicBoolean clean = new AtomicBoolean(true);
	final ObjectName objectName;
	final AtomicReference<ScheduledFuture<?>> handle = new AtomicReference<ScheduledFuture<?>>(null);
	
	private LocalPortForwarder() {
		cm = null;
		host_to_connect = null;
		port_to_connect = -1;
		lat = null;
		bytesUp = null;
		bytesDown = null;
		accepts = null;
		objectName = null;
	}

	LocalPortForwarder(ChannelManager cm, int local_port, String host_to_connect, int port_to_connect) throws IOException
	{
		this.cm = cm;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;
		
		final LocalPortForwardWatcher watcher = LocalPortForwardWatcher.getInstance(this);
		bytesUp = watcher.getBytesUpAccumulator();
		bytesDown = watcher.getBytesDownAccumulator();
		accepts = watcher.getAcceptsAccumulator();		
		lat = new LocalAcceptThread(cm, local_port, host_to_connect, port_to_connect, bytesUp, bytesDown, accepts, this);
		lat.setDaemon(true);
		lat.start();
		open.set(true);
		watcher.incrementOpens();
		objectName = register();
	}
	
	LocalPortForwarder(ChannelManager cm, InetSocketAddress addr, String host_to_connect, int port_to_connect) throws IOException
	{
		this.cm = cm;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;
		
		
		final LocalPortForwardWatcher watcher = LocalPortForwardWatcher.getInstance(this);
		bytesUp = watcher.getBytesUpAccumulator();
		bytesDown = watcher.getBytesDownAccumulator();
		accepts = watcher.getAcceptsAccumulator();		
		lat = new LocalAcceptThread(cm, addr, host_to_connect, port_to_connect, bytesUp, bytesDown, accepts, this);
		lat.setDaemon(true);
		lat.start();
		open.set(true);
		watcher.incrementOpens();
		objectName = register();
		
		
		//lat = new LocalAcceptThread(cm, addr, host_to_connect, port_to_connect, bytesUp, bytesDown, accepts, this);
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
		return on;		
	}


	/**
	 * Return the local socket address of the {@link ServerSocket} used to accept connections.
	 * @return
	 */
	public InetSocketAddress getLocalSocketAddress()
	{
		return (InetSocketAddress) lat.getServerSocket().getLocalSocketAddress();
	}
	
	@Override
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
	public void close() throws IOException 	{
		if(open.compareAndSet(true, false)) {
			LocalPortForwardWatcher.getInstance(this).incrementCloses();
			try { lat.stopWorking(); } catch (Exception x) {/* No Op */}
			open.set(false);		
			handle.set(SSHService.getInstance().schedule(new Runnable(){
				@Override
				public void run() {
					final ScheduledFuture<?> h = handle.getAndSet(null);
					if(h!=null && !h.isCancelled()) {
						try { JMXHelper.unregisterMBean(objectName); } catch (Exception x) {/* No Op */}
					}
				}
			}, 60, TimeUnit.SECONDS));
		}
	}
	
	@Override
	public String getAcceptThreadState() {
		if(lat==null) return "NULL";
		return lat.getState().name();
	}
	
	@Override
	public String getAcceptThreadName() {
		if(lat==null) return "NULL";
		return lat.getName();		
	}
	
	@Override
	public long getAcceptThreadId() {
		if(lat==null) return -1L;;
		return lat.getId();		
	}
	
	@Override
	public boolean isServerSocketBound() {
		if(lat==null) return false;
		ServerSocket ss = lat.getServerSocket();
		if(ss==null) return false;
		return ss.isBound();		
	}
	
	@Override
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
	@Override
	public String getHost() {
		return host_to_connect;
	}
	
	/**
	 * Returns the local bind interface
	 * @return the local bind interface
	 */
	@Override
	public String getLocalIface() {
		return (lat!=null && lat.getServerSocket()!=null && lat.getServerSocket().getLocalSocketAddress()!=null) ?
					 ((InetSocketAddress)lat.getServerSocket().getLocalSocketAddress()).getHostString() : null;
	}

	/**
	 * Returns the local listening port
	 * @return the local listening port
	 */
	@Override
	public int getLocalPort() {
		return (lat!=null && lat.getServerSocket()!=null && lat.getServerSocket().getLocalSocketAddress()!=null) ?
					 ((InetSocketAddress)lat.getServerSocket().getLocalSocketAddress()).getPort() : -1;
	}

	//lat.getServerSocket().getLocalSocketAddress()
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LocalPortForward[" + getLocalIface() + ":" + getLocalPort() + "--->" + host_to_connect + ":" + port_to_connect;
	}
	
	/**
	 * Returns 
	 * @return the port_to_connect
	 */
	@Override
	public int getPort() {
		return port_to_connect;
	}
	
	@Override
	public boolean isOpen() {
		return open.get();
	}
	
	@Override
	public long getBytesUp() {
		return lat.getBytesUp();
	}
	
	@Override
	public long getBytesDown() {
		return lat.getBytesDown();
	}
	
	@Override
	public long getAccepts() {
		return lat.getAccepts();
	}
	
	
	@Override
	public long getTimeTillUnregister() {
		final ScheduledFuture<?> h = handle.get();
		return h==null ? -1L : h.getDelay(TimeUnit.SECONDS);
	}
	
}
