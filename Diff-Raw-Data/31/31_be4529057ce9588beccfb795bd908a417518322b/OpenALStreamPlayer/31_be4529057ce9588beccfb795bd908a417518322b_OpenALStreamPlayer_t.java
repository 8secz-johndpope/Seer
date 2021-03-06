 package org.newdawn.slick.openal;
 
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.nio.IntBuffer;
 
 import org.lwjgl.BufferUtils;
 import org.lwjgl.openal.AL10;
 import org.newdawn.slick.util.Log;
 import org.newdawn.slick.util.ResourceLoader;
 
 /**
  * A generic tool to work on a supplied stream, pulling out PCM data and buffered it to OpenAL
  * as required.
  * 
  * @author Kevin Glass
  */
 public class OpenALStreamPlayer {
 	/** The size of the sections to stream from the stream */
 	private static final int sectionSize = 4096 * 10;
 	
 	/** The buffer read from the data stream */
 	private byte[] buffer = new byte[sectionSize];
 	/** Holds the OpenAL buffer names */
 	private IntBuffer bufferNames;
 	/** The byte buffer passed to OpenAL containing the section */
 	private ByteBuffer bufferData = BufferUtils.createByteBuffer(sectionSize);
 	/** The buffer holding the names of the OpenAL buffer thats been fully played back */
 	private IntBuffer unqueued = BufferUtils.createIntBuffer(1);
 	/** The source we're playing back on */
     private int source;
 	/** The number of buffers remaining */
     private int remainingBufferCount;
 	/** True if we should loop the track */
 	private boolean loop;
 	/** True if we've completed play back */
 	private boolean done = true;
 	/** The stream we're currently reading from */
 	private AudioInputStream audio;
 	/** The source of the data */
 	private String ref;
 	
 	/**
 	 * Create a new player to work on an audio stream
 	 * 
 	 * @param source The source on which we'll play the audio
 	 * @param ref A reference to the audio file to stream
 	 */
 	public OpenALStreamPlayer(int source, String ref) {
 		this.source = source;
 		this.ref = ref;
 	}
 	
 	/**
 	 * Initialise our connection to the underlying resource
 	 * 
 	 * @throws IOException Indicates a failure to open the underling resource
 	 */
 	private void initStreams() throws IOException {
 		if (audio != null) {
 			audio.close();
 		}
 		
 		OggInputStream ogg = new OggInputStream(ResourceLoader.getResourceAsStream(ref));
 		audio = ogg;
 	}
 	
 	/**
 	 * Clean up the buffers applied to the sound source
 	 */
 	private void removeBuffers() {
 		IntBuffer buffer = BufferUtils.createIntBuffer(1);
 		int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
 		
 		while (queued > 0)
 		{
 			AL10.alSourceUnqueueBuffers(source, buffer);
 			queued--;
 		}
 	}
 	
 	/**
 	 * Start this stream playing
 	 * 
 	 * @param loop True if the stream should loop 
 	 * @throws IOException Indicates a failure to read from the stream
 	 */
 	public void play(boolean loop) throws IOException {
 		this.loop = loop;
 		
 		initStreams();
 		
 		done = false;
 
 		if (bufferNames != null) {
 			AL10.alSourceStop(source);
 			removeBuffers();
 			bufferNames.flip();
 			AL10.alDeleteBuffers(bufferNames);
 		}
 		
 		bufferNames = BufferUtils.createIntBuffer(2);
 		AL10.alGenBuffers(bufferNames);
 		remainingBufferCount = 2;
 	
 		for (int i=0;i<2;i++) {
 	        stream(bufferNames.get(i));
 		}
 		
         AL10.alSourceQueueBuffers(source, bufferNames);
 		AL10.alSourcePlay(source);
 	}
 	
 	/**
 	 * Setup the playback properties
 	 * 
 	 * @param pitch The pitch to play back at
 	 * @param gain The volume to play back at
 	 */
 	public void setup(float pitch, float gain) {
 		AL10.alSourcef(source, AL10.AL_PITCH, pitch);
 		AL10.alSourcef(source, AL10.AL_GAIN, gain); 
 	}
 	
 	/**
 	 * Check if the playback is complete. Note this will never
 	 * return true if we're looping
 	 * 
 	 * @return True if we're looping
 	 */
 	public boolean done() {
 		return done;
 	}
 	
 	/**
 	 * Poll the bufferNames - check if we need to fill the bufferNames with another
 	 * section. 
 	 * 
 	 * Most of the time this should be reasonably quick
 	 */
 	public void update() {
 		if (done) {
 			return;
 		}
 		
 		int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
 		
 		while (processed > 0) {
 			unqueued.clear();
 			
 			AL10.alSourceUnqueueBuffers(source, unqueued);
 	        if (stream(unqueued.get(0))) {
 	        	AL10.alSourceQueueBuffers(source, unqueued);
 	        } else {
 	        	remainingBufferCount--;
 	        	if (remainingBufferCount == 0) {
 	        		done = true;
 	        	}
 	        }
 	        processed--;
 		}
 		
 		int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
 	    
 	    if (state != AL10.AL_PLAYING) {
 	    	AL10.alSourcePlay(source);
 	    }
 	}
 	
 	/**
 	 * Stream some data from the audio stream to the buffer indicates by the ID
 	 * 
 	 * @param bufferId The ID of the buffer to fill
 	 * @return True if another section was available
 	 */
 	public boolean stream(int bufferId) {
 		try {
 			int frames = sectionSize;
 			
 			int count = audio.read(buffer);
 			
 			if (count != -1) {
 				bufferData.clear();
 				bufferData.put(buffer,0,count);
 				bufferData.flip();
 				bufferData.limit(count);
 		
 				AL10.alBufferData(bufferId, audio.getChannels() > 1 ? AL10.AL_FORMAT_STEREO16 : AL10.AL_FORMAT_MONO16, 
 											bufferData, audio.getRate());
 			} else {
				if (loop) {
					initStreams();
					stream(bufferId);
				} else {
					done = true;
					return false;
				}
 			}
 			
 			return true;
 		} catch (IOException e) {
 			Log.error(e);
 			return false;
 		}
 	}
 }
 
