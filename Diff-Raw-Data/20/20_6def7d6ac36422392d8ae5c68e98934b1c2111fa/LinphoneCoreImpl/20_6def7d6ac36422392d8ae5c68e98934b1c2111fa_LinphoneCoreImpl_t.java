 /*
 LinphoneCoreImpl.java
 Copyright (C) 2010  Belledonne Communications, Grenoble, France
 
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
 package org.linphone.core;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.Vector;
 
 
 class LinphoneCoreImpl implements LinphoneCore {
 
 	@SuppressWarnings("unused")
 	private final  LinphoneCoreListener mListener; //to make sure to keep a reference on this object
 	private long nativePtr = 0;
 	private native long newLinphoneCore(LinphoneCoreListener listener,String userConfig,String factoryConfig,Object  userdata);
 	private native void iterate(long nativePtr);
 	private native long getDefaultProxyConfig(long nativePtr);
 
 	private native void setDefaultProxyConfig(long nativePtr,long proxyCfgNativePtr);
 	private native int addProxyConfig(LinphoneProxyConfig jprtoxyCfg,long nativePtr,long proxyCfgNativePtr);
 	private native void clearAuthInfos(long nativePtr);
 	
 	private native void clearProxyConfigs(long nativePtr);
 	private native void addAuthInfo(long nativePtr,long authInfoNativePtr);
 	private native long invite(long nativePtr,String uri);
 	private native void terminateCall(long nativePtr, long call);
 	private native long getRemoteAddress(long nativePtr);
 	private native boolean  isInCall(long nativePtr);
 	private native boolean isInComingInvitePending(long nativePtr);
 	private native void acceptCall(long nativePtr, long call);
 	private native long getCallLog(long nativePtr,int position);
 	private native int getNumberOfCallLogs(long nativePtr);
 	private native void delete(long nativePtr);
 	private native void setNetworkStateReachable(long nativePtr,boolean isReachable);
 	private native void setPlaybackGain(long nativeptr, float gain);
 	private native float getPlaybackGain(long nativeptr);
 	private native void muteMic(long nativePtr,boolean isMuted);
 	private native long interpretUrl(long nativePtr,String destination);
 	private native long inviteAddress(long nativePtr,long to);
 	private native void sendDtmf(long nativePtr,char dtmf);
 	private native void clearCallLogs(long nativePtr);
 	private native boolean isMicMuted(long nativePtr);
 	private native long findPayloadType(long nativePtr, String mime, int clockRate);
 	private native int enablePayloadType(long nativePtr, long payloadType,	boolean enable);
 	private native void enableEchoCancellation(long nativePtr,boolean enable);
 	private native boolean isEchoCancellationEnabled(long nativePtr);
 	private native long getCurrentCall(long nativePtr) ;
 	private native void playDtmf(long nativePtr,char dtmf,int duration);
 	private native void stopDtmf(long nativePtr);
 	
 	private native void addFriend(long nativePtr,long friend);
 	private native void setPresenceInfo(long nativePtr,int minute_away, String alternative_contact,int status);
 	private native long createChatRoom(long nativePtr,String to);
 	private native void setRing(long nativePtr, String path);
 	private native String getRing(long nativePtr);
 	
 	LinphoneCoreImpl(LinphoneCoreListener listener, File userConfig,File factoryConfig,Object  userdata) throws IOException {
 		mListener=listener;
 		nativePtr = newLinphoneCore(listener,userConfig.getCanonicalPath(),factoryConfig.getCanonicalPath(),userdata);
 	}
 	LinphoneCoreImpl(LinphoneCoreListener listener) throws IOException {
 		mListener=listener;
 		nativePtr = newLinphoneCore(listener,null,null,null);
 	}
 	
 	protected void finalize() throws Throwable {
 		
 	}
 	
 	public synchronized void addAuthInfo(LinphoneAuthInfo info) {
 		isValid();
 		addAuthInfo(nativePtr,((LinphoneAuthInfoImpl)info).nativePtr);
 	}
 
 
 
 	public synchronized LinphoneProxyConfig getDefaultProxyConfig() {
 		isValid();
 		long lNativePtr = getDefaultProxyConfig(nativePtr);
 		if (lNativePtr!=0) {
 			return new LinphoneProxyConfigImpl(lNativePtr); 
 		} else {
 			return null;
 		}
 	}
 
 	public synchronized LinphoneCall invite(String uri) {
 		isValid();
 		long lNativePtr = invite(nativePtr,uri);
 		if (lNativePtr!=0) {
 			return new LinphoneCallImpl(lNativePtr); 
 		} else {
 			return null;
 		}
 	}
 
 	public synchronized void iterate() {
 		isValid();
 		iterate(nativePtr);
 	}
 
 	public synchronized void setDefaultProxyConfig(LinphoneProxyConfig proxyCfg) {
 		isValid();
 		setDefaultProxyConfig(nativePtr,((LinphoneProxyConfigImpl)proxyCfg).nativePtr);
 	}
 	public synchronized void addProxyConfig(LinphoneProxyConfig proxyCfg) throws LinphoneCoreException{
 		isValid();
 		if (addProxyConfig(proxyCfg,nativePtr,((LinphoneProxyConfigImpl)proxyCfg).nativePtr) !=0) {
 			throw new LinphoneCoreException("bad proxy config");
 		}
 	}
 	public synchronized void clearAuthInfos() {
 		isValid();
 		clearAuthInfos(nativePtr);
 		
 	}
 	public synchronized void clearProxyConfigs() {
 		isValid();
 		clearProxyConfigs(nativePtr);
 	}
 	public synchronized void terminateCall(LinphoneCall aCall) {
 		isValid();
 		if (aCall!=null)terminateCall(nativePtr,((LinphoneCallImpl)aCall).nativePtr);
 	}
 	public synchronized LinphoneAddress getRemoteAddress() {
 		isValid();
 		long ptr = getRemoteAddress(nativePtr);
 		if (ptr==0) {
 			return null;
 		} else {
 			return new LinphoneAddressImpl(ptr);
 		}
 	}
 	public synchronized  boolean isIncall() {
 		isValid();
 		return isInCall(nativePtr);
 	}
 	public synchronized boolean isInComingInvitePending() {
 		isValid();
 		return isInComingInvitePending(nativePtr);
 	}
 	public synchronized void acceptCall(LinphoneCall aCall) {
 		isValid();
 		acceptCall(nativePtr,((LinphoneCallImpl)aCall).nativePtr);
 		
 	}
 	public synchronized Vector<LinphoneCallLog> getCallLogs() {
 		isValid();
 		Vector<LinphoneCallLog> logs = new Vector<LinphoneCallLog>(); 
 		for (int i=0;i < getNumberOfCallLogs(nativePtr);i++) {
 			logs.add(new LinphoneCallLogImpl(getCallLog(nativePtr, i)));
 		}
 		return logs;
 	}
 	public synchronized void destroy() {
 		isValid();
 		delete(nativePtr);
 		nativePtr = 0;
 	}
 	
 	private void isValid() {
 		if (nativePtr == 0) {
 			throw new RuntimeException("object already destroyed");
 		}
 	}
 	public void setNetworkReachable(boolean isReachable) {
 		setNetworkStateReachable(nativePtr,isReachable);
 	}
 	public void setPlaybackGain(float gain) {
 		setPlaybackGain(nativePtr,gain);
 		
 	}
 	public float getPlaybackGain() {
 		return getPlaybackGain(nativePtr);
 	}
 	public void muteMic(boolean isMuted) {
 		muteMic(nativePtr,isMuted);
 	}
 	public LinphoneAddress interpretUrl(String destination) throws LinphoneCoreException {
 		long lAddress = interpretUrl(nativePtr,destination);
 		if (lAddress != 0) {
 			return new LinphoneAddressImpl(lAddress,true);
 		} else {
 			throw new LinphoneCoreException("Cannot interpret ["+destination+"]");
 		}
 	}
 	public LinphoneCall invite(LinphoneAddress to) { 
 		long lNativePtr = inviteAddress(nativePtr,((LinphoneAddressImpl)to).nativePtr);
 		if (lNativePtr!=0) {
 			return new LinphoneCallImpl(lNativePtr); 
 		} else {
 			return null;
 		}
 	}
 	public void sendDtmf(char number) {
 		sendDtmf(nativePtr,number);
 	}
 	public void clearCallLogs() {
 		clearCallLogs(nativePtr);
 	}
 	public boolean isMicMuted() {
 		return isMicMuted(nativePtr);
 	}
 	public PayloadType findPayloadType(String mime, int clockRate) {
 		isValid();
 		long playLoadType = findPayloadType(nativePtr, mime, clockRate);
 		if (playLoadType == 0) {
 			return null;
 		} else {
 			return new PayloadTypeImpl(playLoadType);
 		}
 	}
 	public void enablePayloadType(PayloadType pt, boolean enable)
 			throws LinphoneCoreException {
 		isValid();
 		if (enablePayloadType(nativePtr,((PayloadTypeImpl)pt).nativePtr,enable) != 0) {
 			throw new LinphoneCoreException("cannot enable payload type ["+pt+"]");
 		}
 		
 	}
 	public void enableEchoCancellation(boolean enable) {
 		isValid();
 		enableEchoCancellation(nativePtr, enable);
 	}
 	public boolean isEchoCancellationEnabled() {
 		isValid();
 		return isEchoCancellationEnabled(nativePtr);
 		
 	}
 
 	public synchronized LinphoneCall getCurrentCall() {
 		isValid();
 		long lNativePtr = getCurrentCall(nativePtr);
 		if (lNativePtr!=0) {
 			return new LinphoneCallImpl(lNativePtr); 
 		} else {
 			return null;
 		}
 	}
 	
 	public int getPlayLevel() {
 		// TODO Auto-generated method stub
 		return 0;
 	}
 	public void setPlayLevel(int level) {
 		// TODO Auto-generated method stub
 		
 	}
 	public void setSignalingTransport(Transport aTransport) {
 		// TODO Auto-generated method stub
 		
 	}
 	public void enableSpeaker(boolean value) {
 		// TODO Auto-generated method stub
 		
 	}
 	public boolean isSpeakerEnabled() {
 		// TODO Auto-generated method stub
 		return false;
 	}
 	public void playDtmf(char number, int duration) {
 		playDtmf(nativePtr,number, duration);
 		
 	}
 	public void stopDtmf() {
 		stopDtmf(nativePtr);
 	}
 	
 	public void addFriend(LinphoneFriend lf) throws LinphoneCoreException {
 		addFriend(nativePtr,((LinphoneFriendImpl)lf).nativePtr);
 		
 	}
 	public void setPresenceInfo(int minute_away, String alternative_contact,
 			OnlineStatus status) {
 		setPresenceInfo(nativePtr,minute_away,alternative_contact,status.mValue);
 		
 	}
 	public LinphoneChatRoom createChatRoom(String to) {
 		return new LinphoneChatRoomImpl(createChatRoom(nativePtr,to));
 	}
 	public void setPreviewWindow(Object w) {
 		throw new RuntimeException("not implemented yet");
 		// TODO Auto-generated method stub
 		
 	}
 	public void setVideoWindow(Object w) {
 		throw new RuntimeException("not implemented yet");
 		// TODO Auto-generated method stub
 	}
 	public void enableVideo(boolean vcap_enabled, boolean display_enabled) {
 		// TODO Auto-generated method stub
 		
 	}
 	public boolean isVideoEnabled() {
 		// TODO Auto-generated method stub
 		return false;
 	}
 	public void setStunServer(String stun_server) {
 		// TODO Auto-generated method stub
 		
 	}
 	public String getStunServer() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 	public void setFirewallPolicy(FirewallPolicy pol) {
 		// TODO Auto-generated method stub
 		
 	}
 	public FirewallPolicy getFirewallPolicy() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 	public void setRing(String path) {
 		setRing(nativePtr,path);
 		
 	}
 	public String getRing() {
 		return getRing(nativePtr);
 	}
 	public LinphoneCall inviteAddressWithParams(LinphoneAddress destination,
 			LinphoneCallParams params) throws LinphoneCoreException {
 		throw new RuntimeException("Not Implemenetd yet");
 	}
 	public int updateCall(LinphoneCall call, LinphoneCallParams params) {
 		throw new RuntimeException("Not Implemenetd yet");
 	}
 	public LinphoneCallParams createDefaultCallParameters() {
 		throw new RuntimeException("Not Implemenetd yet");
 	}
 	public boolean isNetworkReachable() {
 		throw new RuntimeException("Not Implemenetd yet");
 	}
 	public void setUploadBandwidth(int bw) {
 		throw new RuntimeException("Not Implemenetd yet");
 	}
 	public void setDownloadBandwidth(int bw) {
 		throw new RuntimeException("Not Implemenetd yet");
 	}
 	public void setPreferredVideoSize(VideoSize vSize) {
 		throw new RuntimeException("Not Implemenetd yet");
 	}
 	public VideoSize getPreferredVideoSize() {
 		throw new RuntimeException("Not Implemenetd yet");
 	}
	public PayloadType[] listVideoCodecs() {
		throw new RuntimeException("Not Implemenetd yet");
	}
 	
 
 }
