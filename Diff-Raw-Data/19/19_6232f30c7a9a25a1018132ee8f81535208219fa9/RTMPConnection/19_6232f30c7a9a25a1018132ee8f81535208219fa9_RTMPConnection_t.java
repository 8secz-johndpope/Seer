 package org.red5.server.net.rtmp;
 
 import static org.red5.server.api.ScopeUtils.getScopeService;
 
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.UUID;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.mina.common.ByteBuffer;
 import org.red5.server.BaseConnection;
 import org.red5.server.api.IContext;
 import org.red5.server.api.IScope;
 import org.red5.server.api.service.IPendingServiceCall;
 import org.red5.server.api.service.IPendingServiceCallback;
 import org.red5.server.api.service.IServiceCall;
 import org.red5.server.api.service.IServiceCapableConnection;
 import org.red5.server.api.so.ISharedObject;
 import org.red5.server.api.so.ISharedObjectCapableConnection;
 import org.red5.server.api.so.ISharedObjectService;
 import org.red5.server.api.stream.IClientBroadcastStream;
 import org.red5.server.api.stream.IClientStream;
 import org.red5.server.api.stream.IPlaylistSubscriberStream;
 import org.red5.server.api.stream.ISingleItemSubscriberStream;
 import org.red5.server.api.stream.IStreamCapableConnection;
 import org.red5.server.api.stream.IStreamService;
 import org.red5.server.net.rtmp.message.Invoke;
 import org.red5.server.net.rtmp.message.OutPacket;
 import org.red5.server.net.rtmp.message.Ping;
 import org.red5.server.service.PendingCall;
 import org.red5.server.so.SharedObjectService;
 import org.red5.server.stream.ClientBroadcastStream;
 import org.red5.server.stream.OutputStream;
 import org.red5.server.stream.PlaylistSubscriberStream;
 import org.red5.server.stream.StreamService;
 import org.red5.server.stream.VideoCodecFactory;
 import org.springframework.context.ApplicationContext;
 
 public abstract class RTMPConnection extends BaseConnection 
 	implements IStreamCapableConnection, IServiceCapableConnection {
 
 	protected static Log log =
         LogFactory.getLog(RTMPConnection.class.getName());
 
 	private final static int MAX_STREAMS = 12;
 	private final static String VIDEO_CODEC_FACTORY = "videoCodecFactory";
 	
 	//private Context context;
 	private Channel[] channels = new Channel[64];
 	private IClientStream[] streams = new IClientStream[MAX_STREAMS];
 	private boolean[] reservedStreams = new boolean[MAX_STREAMS];
 	protected Integer invokeId = new Integer(1);
 	protected HashMap<Integer,IPendingServiceCall> pendingCalls = new HashMap<Integer,IPendingServiceCall>();
 	protected int lastPingTime = -1;
 
 	public RTMPConnection(String type) {
 		// We start with an anonymous connection without a scope.
 		// These parameters will be set during the call of "connect" later.
 		//super(null, "");	temp fix to get things to compile
 		super(type,null,null,null,null,null);
 	}
 	
 	public void setup(String host, String path, String sessionId, Map<String, String> params){
 		this.host = host;
 		this.path = path;
 		this.sessionId = sessionId;
 		this.params = params;
 	}
 		
 	public int getNextAvailableChannelId(){
 		int result = -1;
 		for(byte i=4; i<channels.length; i++){
 			if(!isChannelUsed(i)){
 				result = i;
 				break;
 			}
 		}
 		return result;
 	}
 	
 	public boolean isChannelUsed(byte channelId){
 		return (channels[channelId] != null);
 	}
 
 	public Channel getChannel(byte channelId){
 		if(!isChannelUsed(channelId)) 
 			channels[channelId] = new Channel(this, channelId);
 		return channels[channelId];
 	}
 	
 	public void closeChannel(byte channelId){
 		channels[channelId] = null;
 	}
 	
 	public int reserveStreamId() {
 		int result = -1;
 		synchronized (reservedStreams) {
 			for (int i=0; i<reservedStreams.length; i++) {
 				if (!reservedStreams[i]) {
 					reservedStreams[i] = true;
 					result = i;
 					break;
 				}
 			}
 		}
 		return result + 1;
 	}
 	
 	public OutputStream createOutputStream(int streamId) {
 		byte channelId = (byte) (4 + ((streamId - 1) * 5));
 		final Channel data = getChannel(channelId++);
 		final Channel video = getChannel(channelId++);
 		final Channel audio = getChannel(channelId++);
 		//final Channel unknown = getChannel(channelId++);
 		//final Channel ctrl = getChannel(channelId++);
 		return new OutputStream(video, audio, data);
 	}
 	
 	public VideoCodecFactory getVideoCodecFactory() {
 		final IContext context = scope.getContext();
 		ApplicationContext appCtx = context.getApplicationContext();
 		if (!appCtx.containsBean(VIDEO_CODEC_FACTORY))
 			return null;
 	
 		return (VideoCodecFactory) appCtx.getBean(VIDEO_CODEC_FACTORY);
 	}
 	
 	public IClientBroadcastStream newBroadcastStream(int streamId) {
 		if (!reservedStreams[streamId - 1])
 			// StreamId has not been reserved before
 			return null;
 		
 		if (streams[streamId - 1] != null)
 			// Another stream already exists with this id
 			return null;
 		
 		ClientBroadcastStream cbs = new ClientBroadcastStream();
 		cbs.setStreamId(streamId);
 		cbs.setConnection(this);
 		cbs.setName(createStreamName());
 		cbs.setScope(this.getScope());
 
 		streams[streamId - 1] = cbs;
 		return cbs;
 	}
 	
 	public ISingleItemSubscriberStream newSingleItemSubscriberStream(int streamId) {
 		// TODO implement it
 		return null;
 	}
 	
 	public IPlaylistSubscriberStream newPlaylistSubscriberStream(int streamId) {
 		if (!reservedStreams[streamId - 1])
 			// StreamId has not been reserved before
 			return null;
 		
 		if (streams[streamId - 1] != null)
 			// Another stream already exists with this id
 			return null;
 		
 		PlaylistSubscriberStream pss = new PlaylistSubscriberStream();
 		pss.setName(createStreamName());
 		pss.setConnection(this);
 		pss.setScope(this.getScope());
 		pss.setStreamId(streamId);
 		streams[streamId - 1] = pss;
 		return pss;
 	}
 	
 	public IClientStream getStreamById(int id){
 		if (id <= 0 || id > MAX_STREAMS-1)
 			return null;
 		
 		return streams[id-1];
 	}
 	
 	public IClientStream getStreamByChannelId(byte channelId){
 		if (channelId < 4)
 			return null;
 		
 		//log.debug("Channel id: "+channelId);
 		int id = (int) Math.floor((channelId-4)/5);
 		//log.debug("Stream: "+streamId);
 		return streams[id];
 	}
 	
 	public void close(){
 		IStreamService streamService = (IStreamService) getScopeService(scope, IStreamService.STREAM_SERVICE, StreamService.class);
 		if (streamService != null) {
 			synchronized (streams) {
 				for(int i=0; i<streams.length; i++){
 					IClientStream stream = streams[i];
 					if(stream != null) {
 						log.debug("Closing stream: "+ stream.getStreamId());
 						streamService.deleteStream(this, stream.getStreamId());
 						streams[i] = null;
 					}
 				}
 			}
 		}
 		super.close();
 	}
 	
 	public void unreserveStreamId(int streamId) {
 		if (streamId >= 0 && streamId < MAX_STREAMS-1) {
 			streams[streamId-1] = null;
 			reservedStreams[streamId-1] = false;
 		}
 	}
 	
 	public void ping(Ping ping){
 		getChannel((byte)2).write(ping);
 	}
 	
 	public abstract void rawWrite(ByteBuffer out);
 	public abstract void write(OutPacket out);
 
 	public void invoke(IServiceCall call) {
 		// We need to use Invoke for all calls to the client
 		Invoke invoke = new Invoke();
 		invoke.setCall(call);
 		synchronized (invokeId) {
 			invoke.setInvokeId(invokeId);
 			if (call instanceof IPendingServiceCall) {
 				synchronized (pendingCalls) {
 					pendingCalls.put(invokeId, (IPendingServiceCall) call);
 				}
 			}
 			invokeId += 1;
 		}
 		getChannel((byte) 3).write(invoke);
 	}
 
 	public void invoke(String method) {
 		invoke(method, null, null);
 	}
 	
 	public void invoke(String method, Object[] params) {
 		invoke(method, params, null);
 	}
 	
 	public void invoke(String method, IPendingServiceCallback callback) {
 		invoke(method, null, callback);
 	}
 	
 	public void invoke(String method, Object[] params, IPendingServiceCallback callback) {
 		IPendingServiceCall call = new PendingCall(method, params);
 		if (callback != null)
 			call.registerCallback(callback);
 		
 		invoke(call);
 	}
 	
 	protected IPendingServiceCall getPendingCall(int invokeId) {
 		IPendingServiceCall result;
 		synchronized (pendingCalls) {
 			result = pendingCalls.get(invokeId);
 			if (result != null)
 				pendingCalls.remove(invokeId);
 		}
 		return result;
 	}
 	
 	protected String createStreamName() {
 		return UUID.randomUUID().toString();
 	}
 	
 	protected void messageReceived() {
 		readMessages++;
 	}
 	
 	protected void messageSent() {
 		writtenMessages++;
 	}
 	
 	protected void messageDropped() {
 		droppedMessages++;
 	}
 	
 	public void ping() {
 		Ping pingRequest = new Ping();
 		pingRequest.setValue1((short) 6);
 		int now = (int) (System.currentTimeMillis() & 0xffffffff);
 		pingRequest.setValue2(now);
 		pingRequest.setValue3(Ping.UNDEFINED);
 		ping(pingRequest);
 	}
 	
 	protected void pingReceived(Ping pong) {
 		int now = (int) (System.currentTimeMillis() & 0xffffffff);
 		lastPingTime = now - pong.getValue2();
 	}
 	
 	public int getLastPingTime() {
 		return lastPingTime;
 	}
 	
 }
