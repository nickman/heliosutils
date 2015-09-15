/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.ObjectName;

import com.heliosapm.utils.io.InstrumentedInputStream;
import com.heliosapm.utils.io.InstrumentedOutputStream;
import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.ssh.terminal.SSHService;

import ch.ethz.ssh2.channel.Channel;
import ch.ethz.ssh2.channel.ChannelManager;
import jsr166e.LongAdder;

/**
 * A <code>LocalStreamForwarder</code> forwards an Input- and Outputstream
 * pair via the secure tunnel to another host (which may or may not be identical
 * to the remote SSH-2 server).
 *
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class LocalStreamForwarder implements LocalStreamForwarderMBean, Closeable
{
	private static final AtomicLong serial = new AtomicLong(0);
	private ChannelManager cm;

	private Channel cn;
	private final AtomicBoolean open = new AtomicBoolean(false);
	
	private final String host;
	private final int port;
	private final long mySerial = serial.incrementAndGet();
	private final ObjectName objectName;
	private final LongAdder bytesUp;
	private final LongAdder bytesDown;
	private final Runnable onClose = new Runnable() {
		public void run() {
			try {
				close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
	private final AtomicReference<ScheduledFuture<?>> handle = new AtomicReference<ScheduledFuture<?>>(null);
	
	public long getSerial() {
		return mySerial;
	}
	
	public long getBytesUp() {
		return bytesUp.longValue();
	}
	
	public long getBytesDown() {
		return bytesDown.longValue();
	}
	


	LocalStreamForwarder(ChannelManager cm, String host_to_connect, int port_to_connect) throws IOException
	{
		this.host = host_to_connect;
		this.port = port_to_connect;
		this.cm = cm;
		cn = cm.openDirectTCPIPChannel(host_to_connect, port_to_connect,
				InetAddress.getLocalHost().getHostAddress(), 0);
		open.set(true);
		objectName = register();
		final LocalStreamForwarderWatcher watcher = LocalStreamForwarderWatcher.getInstance(host, port);
		bytesUp = watcher.getBytesUpAccumulator();
		bytesDown = watcher.getBytesDownAccumulator();
		watcher.incrementOpens();

		
	}
	
	public long getTimeTillUnregister() {
		final ScheduledFuture<?> h = handle.get();
		return h==null ? -1L : h.getDelay(TimeUnit.SECONDS);
	}
	
	
	protected ObjectName register() {
		ObjectName on = JMXHelper.objectName(new StringBuilder("com.heliosapm.ssh.localstream:remoteHost=")
			.append(host)
			.append(",remotePort=").append(port)
			.append(",serial=").append(mySerial)
		);
		if(JMXHelper.isRegistered(on)) {
			if(JMXHelper.getAttribute(on, "Open")) {
				System.err.println("LocalStreamForward [" + on + "] still open and registered");
			} else {
				final ScheduledFuture<?> h = handle.getAndSet(null);
				if(h!=null) {
					h.cancel(true);								
					try { JMXHelper.unregisterMBean(objectName); } catch (Exception x) {/* No Op */}
				}
			}
		}
		JMXHelper.registerMBean(this, on);
//	LocalPortForwardWatcher.getInstance(host_to_connect, port_to_connect).addForwarder(this);
		return on;		
	}
	
	
	public boolean isOpen() {
		return open.get();
	}
	
	public int getRemotePort() {
		return port;
	}
	public String getRemoteHost() {
		return host;
	}

	/**
	 * @return An <code>InputStream</code> object.
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException
	{
		return new InstrumentedInputStream(cn.getStdoutStream(), bytesDown, onClose);
	}

	/**
	 * Get the OutputStream. Please be aware that the implementation MAY use an
	 * internal buffer. To make sure that the buffered data is sent over the
	 * tunnel, you have to call the <code>flush</code> method of the
	 * <code>OutputStream</code>. To signal EOF, please use the
	 * <code>close</code> method of the <code>OutputStream</code>.
	 *
	 * @return An <code>OutputStream</code> object.
	 * @throws IOException
	 */
	public OutputStream getOutputStream() throws IOException
	{
		return new InstrumentedOutputStream(cn.getStdinStream(), bytesUp, onClose);		
	}

	/**
	 * Close the underlying SSH forwarding channel and free up resources.
	 * You can also use this method to force the shutdown of the underlying
	 * forwarding channel. Pending output (OutputStream not flushed) will NOT
	 * be sent. Pending input (InputStream) can still be read. If the shutdown
	 * operation is already in progress (initiated from either side), then this
	 * call is a no-op.
	 *
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		if(open.compareAndSet(true, false)) {
			try {
				cm.closeChannel(cn, "Closed due to user request.", true);
			} finally {
				LocalStreamForwarderWatcher.getInstance(host, port).incrementCloses();				
				handle.set(SSHService.getInstance().schedule(new Runnable(){
					public void run() {
						final ScheduledFuture<?> h = handle.getAndSet(null);
						if(h!=null && !h.isCancelled()) {
							try { JMXHelper.unregisterMBean(objectName); } catch (Exception x) {/* No Op */}
						}
					}
				}, 60, TimeUnit.SECONDS));				
			}
		}
		
	}
}
