 package de.uniluebeck.itm.wsn.drivers.core;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.util.ArrayList;
 import java.util.List;
 
 import com.google.common.io.Closeables;
 
 import de.uniluebeck.itm.wsn.drivers.core.io.InputStreamBridge;
 import de.uniluebeck.itm.wsn.drivers.core.io.OutputStreamBridge;
 
 
 /**
  * Class that implement the common functionality of a connection class.
  * 
  * @author Malte Legenhausen
  */
 public abstract class AbstractConnection implements Connection {
 	
 	/**
 	 * List for listeners that want to be notified on connection state changes.
 	 */
 	private final List<ConnectionListener> listeners = new ArrayList<ConnectionListener>();
 	
 	/**
 	 * Current connection state.
 	 */
 	private boolean connected = false;
 	
 	/**
 	 * Input stream of the connection.
 	 */
 	private InputStreamBridge inputStream = new InputStreamBridge();
 	
 	/**
 	 * Output stream of the connection.
 	 */
 	private OutputStreamBridge outputStream = new OutputStreamBridge();
 	
 	/**
 	 * The uri of the connected resource.
 	 */
 	private String uri;
 	
 	/**
 	 * Setter for the connection that will fire a connection change event.
 	 * 
 	 * @param connected True when connected else false.
 	 */
 	protected void setConnected(final boolean connected) {
 		this.connected = connected;
 		fireConnectionChange(new ConnectionEvent(this, uri, connected));
 	}
 	
 	/**
 	 * Fire a connection change event to all registered listeners.
 	 * 
 	 * @param event The event you want to fire.
 	 */
 	protected void fireConnectionChange(ConnectionEvent event) {
 		for (final ConnectionListener listener : listeners.toArray(new ConnectionListener[0])) {
 			listener.onConnectionChange(event);
 		}
 	}
 	
 	/**
 	 * Setter for the input stream.
 	 * 
 	 * @param inputStream The input stream object.
 	 */
 	protected void setInputStream(final InputStream inputStream) {
 		this.inputStream.setInputStream(inputStream);
 	}
 	
 	/**
 	 * Setter for the output stream.
 	 * 
 	 * @param outputStream The output stream object.
 	 */
 	protected void setOutputStream(final OutputStream outputStream) {
 		this.outputStream.setOutputStream(outputStream);
 	}
 	
 	/**
 	 * Setter for the uri.
 	 * 
 	 * @param uri The string representation of the resource.
 	 */
 	protected void setUri(final String uri) {
 		this.uri = uri;
 	}
 	
 	@Override
 	public InputStream getInputStream() {
 		return inputStream;
 	}
 	
 	@Override
 	public OutputStream getOutputStream() {
 		return outputStream;
 	}
 	
 	@Override
 	public boolean isConnected() {
 		return connected;
 	}
 
 	@Override
 	public void addListener(final ConnectionListener listener) {
 		listeners.add(listener);
 	}
 
 	@Override
 	public void removeListener(final ConnectionListener listener) {
 		listeners.remove(listener);
 	}
 	
 	@Override
 	public void close() throws IOException {
 		Closeables.close(inputStream, true);
		inputStream = null;
 		
 		Closeables.close(outputStream, true);
		outputStream = null;
 	}
 }
