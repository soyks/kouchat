
/***************************************************************************
 *   Copyright 2006-2008 by Christian Ihle                                 *
 *   kontakt@usikkert.net                                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/

package net.usikkert.kouchat.net;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.usikkert.kouchat.Constants;
import net.usikkert.kouchat.event.ReceiverListener;
import net.usikkert.kouchat.misc.ErrorHandler;
import net.usikkert.kouchat.util.Loggers;

/**
 * This is the thread that listens for multicast messages from
 * the network, and notifies any listeners when messages arrive.
 *
 * @author Christian Ihle
 */
public class MessageReceiver implements Runnable
{
	/** The logger. */
	private static final Logger LOG = Loggers.NETWORK_LOG;

	private MulticastSocket mcSocket;
	private InetAddress address;
	private ReceiverListener listener;
	private boolean connected;
	private Thread worker;
	private final ErrorHandler errorHandler;

	public MessageReceiver()
	{
		errorHandler = ErrorHandler.getErrorHandler();

		try
		{
			address = InetAddress.getByName( Constants.NETWORK_IP );
		}

		catch ( final IOException e )
		{
			LOG.log( Level.SEVERE, e.toString(), e );
			errorHandler.showCriticalError( "Failed to initialize the network:\n" + e + "\n"
					+ Constants.APP_NAME + " will now shutdown." );
			System.exit( 1 );
		}
	}

	public void run()
	{
		while ( connected )
		{
			try
			{
				DatagramPacket packet = new DatagramPacket(
						new byte[Constants.NETWORK_PACKET_SIZE], Constants.NETWORK_PACKET_SIZE );

				mcSocket.receive( packet );
				String ip = packet.getAddress().getHostAddress();
				String message = new String( packet.getData(), Constants.MESSAGE_CHARSET ).trim();

				if ( listener != null )
					listener.messageArrived( message, ip );
			}

			catch ( final IOException e )
			{
				LOG.log( Level.WARNING, e.toString() );
			}
		}
	}

	private void startThread()
	{
		LOG.log( Level.FINE, "Starting." );
		worker = new Thread( this, "MessageReceiverWorker" );
		worker.start();
	}

	public boolean startReceiver( final NetworkInterface networkInterface )
	{
		LOG.log( Level.FINE, "Connecting..." );

		try
		{
			if ( connected )
			{
				LOG.log( Level.FINE, "Already connected." );
			}

			else
			{
				if ( mcSocket == null )
					mcSocket = new MulticastSocket( Constants.NETWORK_CHAT_PORT );

				if ( networkInterface != null )
					mcSocket.setNetworkInterface( networkInterface );

				mcSocket.joinGroup( address );
				connected = true;
				LOG.log( Level.FINE, "Connected to " + mcSocket.getNetworkInterface().getDisplayName() + "." );
			}
		}

		catch ( final IOException e )
		{
			LOG.log( Level.SEVERE, "Could not start receiver: " + e.toString() );
		}

		if ( connected && ( worker == null || !worker.isAlive() ) )
		{
			startThread();
		}

		return connected;
	}

	public void stopReceiver()
	{
		LOG.log( Level.FINE, "Disconnecting..." );

		if ( !connected )
		{
			LOG.log( Level.FINE, "Not connected." );
		}

		else
		{
			connected = false;

			try
			{
				if ( !mcSocket.isClosed() )
				{
					mcSocket.leaveGroup( address );
				}
			}

			catch ( final IOException e )
			{
				LOG.log( Level.WARNING, e.toString() );
			}

			if ( !mcSocket.isClosed() )
			{
				mcSocket.close();
				mcSocket = null;
			}

			LOG.log( Level.FINE, "Disconnected." );
		}
	}

	public void registerReceiverListener( final ReceiverListener listener )
	{
		this.listener = listener;
	}
}
