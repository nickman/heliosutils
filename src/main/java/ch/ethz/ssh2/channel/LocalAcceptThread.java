/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import jsr166e.DeltaLongAdder;

/**
 * LocalAcceptThread.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class LocalAcceptThread extends Thread implements IChannelWorkerThread
{
	ChannelManager cm;
	String host_to_connect;
	int port_to_connect;
	Runnable onStop;
	long startTime = System.currentTimeMillis();
	
	final DeltaLongAdder bytesUp = new DeltaLongAdder();
	final DeltaLongAdder bytesDown = new DeltaLongAdder();
	final DeltaLongAdder accepts = new DeltaLongAdder();
	
	public long getBytesUp() {
		return bytesUp.longValue();
	}
	
	public long getBytesDown() {
		return bytesDown.longValue();
	}
	public long getAccepts() {
		return accepts.longValue();
	}
	
	public long getDeltaBytesUp() {
		return bytesUp.getDelta();
	}
	
	public long getDeltaBytesDown() {
		return bytesDown.getDelta();
	}
	public long getDeltaAccepts() {
		return accepts.getDelta();
	}

	final ServerSocket ss;

	public LocalAcceptThread(ChannelManager cm, int local_port, String host_to_connect, int port_to_connect, final Runnable onStop)
			throws IOException
	{
		this.cm = cm;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		ss = new ServerSocket(local_port);
		this.onStop = onStop;
	}

	public LocalAcceptThread(ChannelManager cm, InetSocketAddress localAddress, String host_to_connect,
			int port_to_connect, final Runnable onStop) throws IOException
	{
		this.cm = cm;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;
		
		ss = new ServerSocket();
		ss.bind(localAddress);
		this.onStop = onStop;
	}

	public ServerSocket getServerSocket()
	{
		return ss;
	}
	
	@Override
	public void run()
	{
		try
		{
			cm.registerThread(this);
		}
		catch (IOException e)
		{
			stopWorking();
			return;
		}

		while (true)
		{
			Socket s = null;

			try
			{
				s = ss.accept();
				accepts.increment();
			}
			catch (IOException e)
			{
				stopWorking();
				return;
			}

			Channel cn = null;
			StreamForwarder r2l = null;
			StreamForwarder l2r = null;

			try
			{
				/* This may fail, e.g., if the remote port is closed (in optimistic terms: not open yet) */

				cn = cm.openDirectTCPIPChannel(host_to_connect, port_to_connect, s.getInetAddress().getHostAddress(), s
						.getPort());

			}
			catch (IOException e)
			{
				/* Simply close the local socket and wait for the next incoming connection */

				try
				{
					s.close();
				}
				catch (IOException ignore)
				{
				}

				continue;
			}

			try
			{
				r2l = new StreamForwarder(cn, null, null, cn.stdoutStream, s.getOutputStream(), "RemoteToLocal", bytesDown);
				l2r = new StreamForwarder(cn, r2l, s, s.getInputStream(), cn.stdinStream, "LocalToRemote", bytesUp);
			}
			catch (IOException e)
			{
				try
				{
					/* This message is only visible during debugging, since we discard the channel immediatelly */
					cn.cm.closeChannel(cn, "Weird error during creation of StreamForwarder (" + e.getMessage() + ")",
							true);
				}
				catch (IOException ignore)
				{
				}

				continue;
			}

			r2l.setDaemon(true);
			l2r.setDaemon(true);
			r2l.start();
			l2r.start();
		}
	}

	public void stopWorking()
	{
		long endTime = System.currentTimeMillis() - startTime;
		System.err.println("Stopping Work after [" + TimeUnit.SECONDS.convert(endTime, TimeUnit.MILLISECONDS) + "] secs.");
		new Exception().printStackTrace(System.err);
		try
		{
			/* This will lead to an IOException in the ss.accept() call */
			ss.close();			
		}
		catch (IOException ignored)
		{
		} finally {
			if(onStop!=null) {				
				Runnable p = onStop;
				onStop = null;
				p.run();
				
			}
		}
	}
}
