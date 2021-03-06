 package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;
 
 import com.google.common.collect.Lists;
 import de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementServiceImpl;
 import de.uniluebeck.itm.tr.runtime.portalapp.WSNServiceHandle;
 import eu.wisebed.testbed.api.wsn.v211.SecretReservationKey;
 import org.jboss.netty.channel.*;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.util.List;
 
 import static com.google.common.base.Preconditions.checkArgument;
 
 public class ProtobufControllerServerHandler extends SimpleChannelUpstreamHandler {
 
 	private static final Logger log = LoggerFactory.getLogger(ProtobufControllerServerHandler.class.getName());
 
 	private boolean firstMessage = true;
 
 	private ProtobufControllerServer protobufControllerServer;
 
 	private final SessionManagementServiceImpl sessionManagement;
 
 	private Channel channel;
 
 	private WSNServiceHandle wsnServiceHandle;
 
 	private String secretReservationKey;
 
 	public ProtobufControllerServerHandler(final ProtobufControllerServer protobufControllerServer,
 										   final SessionManagementServiceImpl sessionManagement) {
 		this.protobufControllerServer = protobufControllerServer;
 		this.sessionManagement = sessionManagement;
 	}
 
 	@Override
 	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
 		log.info("Client connected: {}", e);
 		channel = e.getChannel();
 		super.channelConnected(ctx, e);
 	}
 
 	@Override
 	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
 		log.info("Client disconnected: {}", e);
 		wsnServiceHandle.getProtobufControllerHelper().removeChannel(e.getChannel());
 		protobufControllerServer.removeHandler(this);
 		channel = null;
 		super.channelDisconnected(ctx, e);
 	}
 
 	@Override
 	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
 		WisebedProtocol.Envelope envelope = (WisebedProtocol.Envelope) e.getMessage();
 		switch (envelope.getBodyType()) {
 			case SECRET_RESERVATION_KEYS:
 
 				if (firstMessage) {
 					firstMessage = false;
 				}
 
 				checkArgument(!firstMessage, "Secret reservation keys are only allowed to be sent as the very first message.");
 				checkArgument(envelope.hasSecretReservationKeys(), "Envelope is missing secret reservation keys");
 
 				log.debug("ProtobufControllerServerHandler.receivedSecretReservationKeys()");
 
 				secretReservationKey = envelope.getSecretReservationKeys().getKeys(0).getKey();
 				wsnServiceHandle = sessionManagement.getWsnServiceHandle(secretReservationKey);
 
 				if (wsnServiceHandle == null) {
 
 					log.debug("Received reservation key for either unknown or not yet started WSN instance. Trying to start instance...");
 					List<SecretReservationKey> secretReservationKeys = convert(envelope.getSecretReservationKeys());
 
 					try {
 						sessionManagement.getInstance(secretReservationKeys, "NONE");
 						wsnServiceHandle = sessionManagement.getWsnServiceHandle(secretReservationKey);
 					} catch (Exception e1) {
 						log.warn("" + e1, e1);
 						channel.close();
 						return;
 					}
 
 					if (wsnServiceHandle == null) {
 						log.debug("Invalid secret reservation key. Closing channel.");
 						channel.close();
 						return;
 					}
 
 				}
 
 				log.debug("Valid secret reservation key. Adding listener to WSN App instance.");
 				wsnServiceHandle.getProtobufControllerHelper().addChannel(e.getChannel());
 				protobufControllerServer.addHandler(this);
 				break;
 
 			default:
 				log.warn("Received message other than secret reservation keys which is not allowed.");
 				e.getChannel().close();
 				break;
 		}
 
 	}
 
 	private List<SecretReservationKey> convert(WisebedProtocol.SecretReservationKeys secretReservationKeys) {
 		List<SecretReservationKey> retKeys = Lists.newArrayList();
 		for (WisebedProtocol.SecretReservationKeys.SecretReservationKey key : secretReservationKeys.getKeysList()) {
 			retKeys.add(convert(key));
 		}
 		return retKeys;
 	}
 
 	private SecretReservationKey convert(WisebedProtocol.SecretReservationKeys.SecretReservationKey key) {
 		SecretReservationKey retKey = new SecretReservationKey();
 		retKey.setUrnPrefix(key.getUrnPrefix());
 		retKey.setSecretReservationKey(key.getKey());
 		return retKey;
 	}
 
 	@Override
 	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
 		log.warn("Unexpected exception from downstream: {}", e);
 		e.getChannel().close();
 	}
 
 	public void stop() {
		log.debug("Stopping ProtobufControllerHandler for channel {}...", channel);
 		channel.close();
 	}
 
 	public String getSecretReservationKey() {
 		return secretReservationKey;
 	}
 }
