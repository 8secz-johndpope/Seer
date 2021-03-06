 /*
  * Copyright (C) 2009 The Sipdroid Open Source Project
  * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
  * 
  * This file is part of Sipdroid (http://www.sipdroid.org)
  * 
  * Sipdroid is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 3 of the License, or
  * (at your option) any later version.
  * 
  * This source code is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this source code; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */
 
 package org.sipdroid.media;
 
 import java.io.IOException;
 import java.net.DatagramSocket;
 import java.net.InetAddress;
 import java.util.Random;
 
 import org.sipdroid.net.RtpPacket;
 import org.sipdroid.net.RtpSocket;
 import org.sipdroid.sipua.ui.RegisterService;
 import org.sipdroid.sipua.ui.Sipdroid;
 
 import android.media.AudioFormat;
 import android.media.AudioManager;
 import android.media.AudioRecord;
 import android.media.MediaRecorder;
 
 /**
  * RtpStreamSender is a generic stream sender. It takes an InputStream and sends
  * it through RTP.
  */
 public class RtpStreamSender extends Thread {
 	/** Whether working in debug mode. */
 	public static boolean DEBUG = true;
 
 	/** The RtpSocket */
 	RtpSocket rtp_socket = null;
 
 	/** Payload type */
 	int p_type;
 
 	/** Number of frame per second */
 	long frame_rate;
 
 	/** Number of bytes per frame */
 	int frame_size;
 
 	/**
 	 * Whether it works synchronously with a local clock, or it it acts as slave
 	 * of the InputStream
 	 */
 	boolean do_sync = true;
 
 	/**
 	 * Synchronization correction value, in milliseconds. It accellarates the
 	 * sending rate respect to the nominal value, in order to compensate program
 	 * latencies.
 	 */
 	int sync_adj = 0;
 
 	/** Whether it is running */
 	boolean running = false;
 	boolean muted = false;
 
 	/**
 	 * Constructs a RtpStreamSender.
 	 * 
 	 * @param input_stream
 	 *            the stream to be sent
 	 * @param do_sync
 	 *            whether time synchronization must be performed by the
 	 *            RtpStreamSender, or it is performed by the InputStream (e.g.
 	 *            the system audio input)
 	 * @param payload_type
 	 *            the payload type
 	 * @param frame_rate
 	 *            the frame rate, i.e. the number of frames that should be sent
 	 *            per second; it is used to calculate the nominal packet time
 	 *            and,in case of do_sync==true, the next departure time
 	 * @param frame_size
 	 *            the size of the payload
 	 * @param src_socket
 	 *            the socket used to send the RTP packet
 	 * @param dest_addr
 	 *            the destination address
 	 * @param dest_port
 	 *            the destination port
 	 */
 	public RtpStreamSender(boolean do_sync,
 			int payload_type, long frame_rate, int frame_size,
 			DatagramSocket src_socket, String dest_addr, int dest_port) {
 		init(do_sync, payload_type, frame_rate, frame_size,
 				src_socket, dest_addr, dest_port);
 	}
 
 	/** Inits the RtpStreamSender */
 	private void init(boolean do_sync,
 			int payload_type, long frame_rate, int frame_size,
 			DatagramSocket src_socket, String dest_addr,
 			int dest_port) {
 		this.p_type = payload_type;
 		this.frame_rate = frame_rate;
 		this.frame_size = 1024; //15
 		this.do_sync = do_sync;
 		try {
 			rtp_socket = new RtpSocket(src_socket, InetAddress
 					.getByName(dest_addr), dest_port);
 		} catch (Exception e) {
 			if (!Sipdroid.release) e.printStackTrace();
 		}
 	}
 
 	/** Sets the synchronization adjustment time (in milliseconds). */
 	public void setSyncAdj(int millisecs) {
 		sync_adj = millisecs;
 	}
 
 	/** Whether is running */
 	public boolean isRunning() {
 		return running;
 	}
 	
 	public void mute() {
 		muted = !muted;
 	}
 
 	public static int delay = 0;
 	
 	/** Stops running */
 	public void halt() {
 		running = false;
 	}
 
 	int poweri,powern,powersum,powersil,power;
 	Random random;
 	
 	void silence(short[] lin,int off,int len) {
 		int i;
 		
 		for (i = 0; i < len; i++)
 			if (lin[i+off] < 300 && lin[i+off] > -300)
 				powersil++;
 			else
 				powersil = 0;
 	}
 	
 	void powersilence(short[] lin,int off,int len) {
 		int i;
 		
 		poweri = 0;
 		for (i = 0; i < len; i++) {
 			if (lin[i+off] < 300 && lin[i+off] > -300)
 				powersil++;
 			else
 				powersil = 0;
 			poweri += Math.abs(lin[i+off]);
 		}
 	}
 	
 	void noise(short[] lin,int off,int len,int power) {
 		int i;
 
 		if (power == 0) power = 10;
 		for (i = 0; i < len; i++)
 			lin[i+off] = (short)(random.nextInt(power*4)-power);
 	}
 	
 	/** Runs it in a new Thread. */
 	public void run() {
 		if (rtp_socket == null)
 			return;
 		byte[] buffer = new byte[frame_size + 12];
 		RtpPacket rtp_packet = new RtpPacket(buffer, 0);
 		rtp_packet.setPayloadType(p_type);
 		int seqn = 0;
 		long time = 0;
 		long byte_rate = frame_rate * frame_size;
 		long frame_time = (frame_size * 1000) / byte_rate;
 
 		running = true;
 
 		if (DEBUG)
 			println("Reading blocks of " + buffer.length + " bytes");
 
 		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
 		AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 
 				AudioRecord.getMinBufferSize(8000, 
 						AudioFormat.CHANNEL_CONFIGURATION_MONO, 
 						AudioFormat.ENCODING_PCM_16BIT)*3/2);
 		record.startRecording();
 		short[] lin = new short[frame_size*11];
 		int num,ring = 0;
 		random = new Random();
 		while (running) {
 			 RegisterService.hold = true;
 			 if (muted) {
 				record.stop();
 				while (running && muted) {
 					RegisterService.hold = true;
 					try {
 						sleep(1000);
 					} catch (InterruptedException e1) {
 					}
 				}
 				record.startRecording();
 			 }
 			 num = record.read(lin,(ring+delay)%(frame_size*11),frame_size);
 
 			 if (RtpStreamReceiver.speakermode == AudioManager.MODE_NORMAL)
	 			 if (RtpStreamReceiver.powersil < 2000)
 	 				 noise(lin,(ring+delay)%(frame_size*11),num,power);
 	 			 else
 		 			 if (powersil >= 4000) {
 		 				 powersilence(lin,(ring+delay)%(frame_size*11),num);
 		 				 if (powern >= 8000) {
 		 					 power = powersum/powern;
 		 					 powersum *= 1-(float)num/powern;
 		 				 } else
 		 					 powern += num;
 		 				 powersum += poweri;
 		 			 } else
 		 				 silence(lin,(ring+delay)%(frame_size*11),num);
 			 
 			 G711.linear2alaw(lin, ring%(frame_size*11), buffer, num);
  			 ring += frame_size;
  			 rtp_packet.setSequenceNumber(seqn++);
  			 rtp_packet.setTimestamp(time);
  			 rtp_packet.setPayloadLength(num);
  			 try {
  				 rtp_socket.send(rtp_packet);
  			 } catch (IOException e) {
  			 }
  			 time += num;
 		}
 		record.stop();
 		
 		rtp_socket.close();
 		rtp_socket = null;
 
 		if (DEBUG)
 			println("rtp sender terminated");
 	}
 
 	/** Debug output */
 	private static void println(String str) {
 		if (!Sipdroid.release) System.out.println("RtpStreamSender: " + str);
 	}
 
 }
