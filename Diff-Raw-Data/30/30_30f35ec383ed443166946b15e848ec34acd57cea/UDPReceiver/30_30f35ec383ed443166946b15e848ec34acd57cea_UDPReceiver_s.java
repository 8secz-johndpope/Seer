 
 /***************************************************************************
  *   Copyright 2006-2007 by Christian Ihle                                 *
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
 import java.net.DatagramSocket;
 
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import net.usikkert.kouchat.Constants;
 import net.usikkert.kouchat.event.ReceiverListener;
 import net.usikkert.kouchat.misc.ErrorHandler;
 import net.usikkert.kouchat.misc.Settings;
 
 /**
  * Receives UDP packets sent directly to the IP address
  * of this machine.
  * 
  * @author Christian Ihle
  */
 public class UDPReceiver implements Runnable
 {
 	private static Logger log = Logger.getLogger( UDPReceiver.class.getName() );
 
 	private DatagramSocket udpSocket;
 	private ReceiverListener listener;
 	private boolean run;
 	private Thread worker;
 	private ErrorHandler errorHandler;
 
 	/**
 	 * Default constructor.
 	 */
 	public UDPReceiver()
 	{
 		errorHandler = ErrorHandler.getErrorHandler();
 	}
 
 	/**
 	 * The run() method of this thread. Checks for new packets,
 	 * extracts the message and IP address, and notifies the listener.
 	 */
 	public void run()
 	{
 		while ( run )
 		{
 			try
 			{
 				DatagramPacket packet = new DatagramPacket(
 						new byte[Constants.NETWORK_PACKET_SIZE], Constants.NETWORK_PACKET_SIZE );
 
 				udpSocket.receive( packet );
 				String ip = packet.getAddress().getHostAddress();
 				String message = new String( packet.getData(), Constants.NETWORK_CHARSET ).trim();
 
 				if ( listener != null )
 					listener.messageArrived( message, ip );
 			}
 
 			catch ( IOException e )
 			{
 				log.log( Level.WARNING, e.toString() );
 			}
 		}
 	}
 
 	/**
 	 * Creates a new UDP socket, and starts a thread listening
 	 * on the UDP port. If the UDP port is in use, a new port will be
 	 * tried instead.
	 * 
	 * @see Constants.NETWORK_UDP_PORT
 	 */
 	public void startReceiver()
 	{
 		int port = Constants.NETWORK_PRIVCHAT_PORT;
 		int counter = 0;
 		boolean done = false;
 
 		while ( counter < 10 && !done )
 		{
 			try
 			{
 				udpSocket = new DatagramSocket( port );
 				run = true;
 				worker = new Thread( this );
 				worker.start();
 				done = true;
 				Settings.getSettings().getMe().setPrivateChatPort( port );
 			}
 
 			catch ( IOException e )
 			{
 				log.log( Level.SEVERE, e.toString() + " " + port );
 				
 				counter++;
 				port++;
 				Settings.getSettings().getMe().setPrivateChatPort( 0 );
 			}
 		}
 
 		if ( !done )
 		{
 			String error = "Failed to initialize udp network:" +
 					"\nNo available listening port between " + Constants.NETWORK_PRIVCHAT_PORT +
 					" and " + ( port -1 ) + "." +
 					"\n\nYou will not be able to receive private messages!";
 			
 			log.log( Level.SEVERE, error );
 			errorHandler.showError( error );
 		}
 	}
 
 	/**
 	 * Closes the UDP socket, and stops the thread.
 	 */
 	public void stopReceiver()
 	{
 		run = false;
 
 		if ( udpSocket != null && !udpSocket.isClosed() )
 		{
 			udpSocket.close();
 		}
 	}
 
 	/**
 	 * Sets the listener who will receive all the messages
 	 * from the UDP packets.
 	 * 
 	 * @param listener The object to register as a listener.
 	 */
 	public void registerReceiverListener( ReceiverListener listener )
 	{
 		this.listener = listener;
 	}
 }
