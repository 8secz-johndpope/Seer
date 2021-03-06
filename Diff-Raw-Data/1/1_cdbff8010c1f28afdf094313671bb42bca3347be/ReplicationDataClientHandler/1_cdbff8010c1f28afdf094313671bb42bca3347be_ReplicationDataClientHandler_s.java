 // This software is released into the Public Domain.  See copying.txt for details.
 package org.openstreetmap.osmosis.replicationhttp.v0_6.impl;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.nio.channels.FileChannel;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Properties;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.jboss.netty.buffer.ChannelBuffer;
 import org.jboss.netty.buffer.ChannelBufferInputStream;
 import org.jboss.netty.channel.ChannelHandlerContext;
 import org.jboss.netty.channel.ChannelStateEvent;
 import org.jboss.netty.util.CharsetUtil;
 import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
 import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;
 import org.openstreetmap.osmosis.core.task.v0_6.RunnableChangeSource;
 import org.openstreetmap.osmosis.core.util.PropertiesPersister;
 import org.openstreetmap.osmosis.replication.common.ReplicationState;
 import org.openstreetmap.osmosis.xml.common.CompressionMethod;
 import org.openstreetmap.osmosis.xml.v0_6.XmlChangeReader;
 
 
 /**
  * Netty handler for receiving replication data and notifying listeners.
  * 
  * @author Brett Henderson
  */
 public class ReplicationDataClientHandler extends SequenceClientHandler {
 
 	private static final Logger LOG = Logger.getLogger(ReplicationDataClientHandler.class.getName());
 
 	private ChangeSink changeSink;
 	private PropertiesPersister statePersistor;
 	private boolean sinkInitInvoked;
 	private boolean replicationStateReceived;
 	private ReplicationState replicationState;
 	private long chunksRemaining;
 	private File tmpDataFile;
 	private FileChannel tmpDataChannel;
 
 
 	/**
 	 * Creates a new instance.
 	 * 
 	 * @param control
 	 *            Provides the Netty handlers with access to the controller.
 	 * @param changeSink
 	 *            The destination for the replication data.
 	 */
 	public ReplicationDataClientHandler(SequenceClientControl control, ChangeSink changeSink) {
 		super(control);
 
 		this.changeSink = changeSink;
 
 		sinkInitInvoked = false;
 		replicationStateReceived = false;
 		replicationState = null;
 		chunksRemaining = -1;
 	}
 
 
 	@SuppressWarnings({ "unchecked", "rawtypes" })
 	private Map<String, String> propertiesToStringMap(Properties properties) {
 		return (Map) properties;
 	}
 
 
 	private ReplicationState loadState(ChannelBuffer buffer) {
 		Properties properties = new Properties();
 
 		try {
 			properties.load(new ChannelBufferInputStream(buffer));
 		} catch (IOException e) {
 			throw new OsmosisRuntimeException("Unable to load replication state from received properties", e);
 		}
 
 		ReplicationState newState = new ReplicationState();
 		newState.load(propertiesToStringMap(properties));
 
 		return newState;
 	}
 
 
 	private void createReplicationDataChannel() {
 
 		try {
 			tmpDataFile = File.createTempFile("change", ".tmp");
 		} catch (IOException e) {
 			throw new OsmosisRuntimeException("Unable to create replication data temp file", e);
 		}
 		try {
 			tmpDataChannel = new FileOutputStream(tmpDataFile).getChannel();
 		} catch (FileNotFoundException e) {
 			throw new OsmosisRuntimeException("Unable to open replication data temp file", e);
 		}
 	}
 
 
 	private void writeReplicationData(ChannelBuffer buffer) {
 		try {
 			tmpDataChannel.write(buffer.toByteBuffer());
 		} catch (IOException e) {
 			throw new OsmosisRuntimeException("Unable to write replication data to temp file", e);
 		}
 	}
 
 
 	private File prepareReplicationDataFile() {
 		try {
 			tmpDataChannel.close();
 		} catch (IOException e) {
 			throw new OsmosisRuntimeException("Unable to close replication data temp file", e);
 		}
 
 		File dataFile = tmpDataFile;
 		tmpDataFile = null;
 		return dataFile;
 	}
 
 
 	private void sendReplicationData() {
 		// Release all class level resources and prepare for passing the
 		// replication data downstream.
 		File replicationFile = prepareReplicationDataFile();
 		ReplicationState readyState = replicationState;
 		replicationState = null;
 
 		// Send the replication data downstream.
 		RunnableChangeSource changeReader = new XmlChangeReader(replicationFile, true, CompressionMethod.GZip);
 		changeReader.setChangeSink(changeSink);
 		changeReader.run();
 
 		// Persist the state.
 		statePersistor.store(readyState.store());
 
 		// The replication data file is no longer required.
 		replicationFile.delete();
 	}
 	
 	
 	private void invokeSinkInit() {
 		replicationState = new ReplicationState();
 		Map<String, Object> metaData = new HashMap<String, Object>(1);
 		changeSink.initialize(metaData);
 		sinkInitInvoked = true;
 	}
 
 
 	@Override
 	protected String getRequestUri() {
 		// We need to know the last replication number that we have received on
 		// a previous run. To do this we need to retrieve the replication state
 		// from our downstream replication task by initializing.
 		invokeSinkInit();
 		
 		// The downstream task returns the currently active sequence number, not the last received one.
 		long requestSequenceNumber = replicationState.getSequenceNumber() - 1;
 		
 		return "/replicationData/" + requestSequenceNumber + "/tail";
 	}
 
 
 	@Override
 	protected void processMessageData(ChannelBuffer buffer) {
 		if (!(sinkInitInvoked && replicationStateReceived)) {
 			// We usually have to invoke the sink init and retrieve existing
 			// replication state, but if this is during startup we may have
 			// already performed this step while preparing our initial request.
 			if (!sinkInitInvoked) {
 				invokeSinkInit();
 			}
 			
 			// The first chunk contains the replication state stored in
 			// properties format.
 			ReplicationState serverReplicationState = loadState(buffer);
 			
 			// Validate that the server has sent us the expected state.
 			if (serverReplicationState.getSequenceNumber() != replicationState.getSequenceNumber()) {
 				throw new OsmosisRuntimeException("Received sequence number "
 						+ serverReplicationState.getSequenceNumber() + " from server, expected "
 						+ replicationState.getSequenceNumber());
 			}
 			
 			// Update the local state with server values.
 			replicationState.setTimestamp(serverReplicationState.getTimestamp());
 
 		} else if (chunksRemaining < 0) {
 			// The next chunk contains the number of chunks that the replication
 			// data will be sent in.
 
 			String chunksRemainingString = buffer.toString(CharsetUtil.UTF_8);
 			chunksRemaining = Long.parseLong(chunksRemainingString);
 
 			if (chunksRemaining < 0) {
 				throw new OsmosisRuntimeException("The replication data chunk count is negative: " + chunksRemaining);
 			}
 
 			// Create a temp file and open an NIO channel on the file.
 			createReplicationDataChannel();
 
 		} else {
 
 			// Write the buffer to the currently open file channel.
 			writeReplicationData(buffer);
 			chunksRemaining--;
 		}
 
 		// If no more chunks are remaining we need to send the replication data
 		// downstream.
 		if (chunksRemaining <= 0) {
 			sendReplicationData();
 		}
 	}
 
 
 	@Override
 	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
 		if (tmpDataChannel != null) {
 			try {
 				tmpDataChannel.close();
 			} catch (IOException ex) {
 				LOG.log(Level.WARNING, "Unable to close the current replication data file", ex);
 			}
 		}
 
 		super.channelClosed(ctx, e);
 	}
 }
