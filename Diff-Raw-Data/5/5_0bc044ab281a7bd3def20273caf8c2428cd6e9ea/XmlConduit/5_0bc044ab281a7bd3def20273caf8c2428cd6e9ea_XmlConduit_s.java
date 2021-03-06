 package org.dcache.services.info.conduits;
 
 import java.net.*;
 import java.io.*;
 
 import org.apache.log4j.Logger;
 import org.dcache.services.info.serialisation.XmlSerialiser;
 
 /**
  * Information Exporter class.<br>
  * This class is instantiated by the <code>InfoCollector</code> to send
 * over a plain tcp socket a <code>Schema</code> object that carries out
  * dynamic information from dCache.<br><br>
  * Also this class is independent from the particular implementation of 
 * Schema used. As matter of fact, this class serializes a generic Schema   
  * object over a socket. It's a job of the client to know what particular
  * implementation of Schema was sent.<br><br>
  * Note that client needs only to know the specializing class of the Schema.
  */
 public class XmlConduit extends AbstractThreadedConduit {
 
 	private static Logger _log = Logger.getLogger( XmlConduit.class);
 
 	private static final int DEFAULT_PORT = 22112;
 	
 	/** TCP port that the server listen on by default */
 	public  int _port = DEFAULT_PORT;
 	
 	/** Server Socket reference */
 	private ServerSocket _svr_skt=null;
 	
 	/** Our serialiser for the current dCache state */
 	private XmlSerialiser _xmlSerialiser = new XmlSerialiser();
 	
 	public void enable() {
 		try {
 			_svr_skt = new ServerSocket(_port);
 		} catch( IOException e) {
 			Thread.currentThread().interrupt();
 			return;
 		} catch(SecurityException e) {
 			_log.error( "security issue creating port "+_port, e);
 			return;
 		}
 		super.enable(); // start the thread.
 	}
 	
 
 	void triggerBlockingActivityToReturn() {
 		if( _svr_skt == null) 
 			return;
 		
 		try {
 			_svr_skt.close();
 		} catch( IOException e) {
 			_log.error("Problem closing server socket", e);
 		} finally {
 			_svr_skt = null;
 		}
 	}
 
 	
 	/**
 	 * Wait for an incoming connection to the listening socket.  When
 	 * one is received, send it the XML serialisation of our current state.
 	 */
 	void blockingActivity() {
 		Socket skt=null;
 		
 		try {
 			skt = _svr_skt.accept();			
 		} catch( SocketException e) {
 			if( this._should_run || !_svr_skt.isClosed()) {
 				_log.error( "accept() failed", e);
 			}
 		} catch( IOException e) {
 			Thread.currentThread().interrupt();
 			return;
 		} catch( SecurityException e) {
 			_log.error( "accept() failed for security reasons", e);
 			return;
 		} catch( Exception e) {
 			_log.error( "accept() failed for an unknown reason", e);
 			return;
 		}
 
 		if( skt != null) {
 			
 			if( _log.isInfoEnabled())
 				_log.info("Incoming connection from " + skt.toString());
 			
 			try {
 				_callCount++;
 				String xmlData = _xmlSerialiser.serialise();
 				skt.getOutputStream().write( xmlData.getBytes());
 			} catch( IOException e) {
 				_log.error( "failed to write XML data", e);
 			} catch( Exception e) {
 				_log.error( "unknown failure writing XML data", e);
 			} finally {
 				try {
 					skt.close();			
 				} catch( IOException e) {
 					Thread.currentThread().interrupt();
 				}
 			}
 		}
 	}
 }
