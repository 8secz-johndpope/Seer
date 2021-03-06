 /*
  * Copyright 1999-2002 Carnegie Mellon University.  
  * Portions Copyright 2002 Sun Microsystems, Inc.  
  * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
  * All Rights Reserved.  Use is subject to license terms.
  * 
  * See the file "license.terms" for information on usage and
  * redistribution of this file, and for a DISCLAIMER OF ALL 
  * WARRANTIES.
  *
  */
 
 
package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.Audio;
import edu.cmu.sphinx.frontend.AudioSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.Utterance;
 import edu.cmu.sphinx.frontend.util.Util;
 
 import edu.cmu.sphinx.util.SphinxProperties;
 
 import java.io.IOException;
 import java.io.InputStream;
 
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Vector;
 
 import javax.sound.sampled.AudioFormat;
 import javax.sound.sampled.AudioSystem;
 import javax.sound.sampled.DataLine;
 import javax.sound.sampled.LineEvent;
 import javax.sound.sampled.LineListener;
 import javax.sound.sampled.LineUnavailableException;
 import javax.sound.sampled.TargetDataLine;
 
 
 /**
  * A Microphone captures audio data from the system's underlying
  * audio input systems. Converts these audio data into Audio
  * objects. The Microphone should be run in a separate thread.
  * When the method <code>startRecording()</code> is called, it will
  * start capturing audio, and stops when <code>stopRecording()</code>
  * is called. An Utterance is created for all the audio captured
  * in between calls to <code>startRecording()</code> and
  * <code>stopRecording()</code>.
  * Calling <code>getAudio()</code> returns the captured audio
  * data as Audio objects. If the SphinxProperty: <pre>
  * edu.cmu.sphinx.frontend.keepAudioReference </pre>
  * is set to true, then the Audio objects returned will contain
  * a reference to the (entire) Utterance object.
  */
 public class Microphone extends DataProcessor implements AudioSource {
 
     /**
      * Parameters for audioFormat
      */
     private AudioFormat audioFormat;
     private float sampleRate = 8000f;
     private int sampleSizeInBits = 16;
     private int channels = 1;
     private boolean signed = true;
     private boolean bigEndian = true;
 
     /**
      * The audio capturing device.
      */
     private TargetDataLine audioLine = null;
     private LineListener lineListener = new MicrophoneLineListener();
     private UtteranceList utteranceList;
 
     private int frameSizeInBytes;
     private volatile boolean started = false;
     private volatile boolean recording = false;
     private volatile boolean closed = false;
     private boolean keepAudioReference = true;
 
     private static Logger logger = Logger.getLogger
     ("edu.cmu.sphinx.frontend.Microphone");
 
 
     /**
      * Constructs a Microphone with the given InputStream.
      *
      * @param name the name of this Microphone
      * @param context the context of this Microphone
      * @param props the SphinxProperties to read properties from
      */
     public Microphone(String name, String context, SphinxProperties props) 
 	throws IOException {
         super(name, context);
 	setProperties(props);
         audioFormat = new AudioFormat(sampleRate, sampleSizeInBits,
                                       channels, signed, bigEndian);
         utteranceList = new UtteranceList();
     }
 
 
     /**
      * Reads the parameters needed from the static SphinxProperties object.
      */
     public void setProperties(SphinxProperties props) {
 
         sampleRate = props.getInt(FrontEnd.PROP_SAMPLE_RATE, 16000);
 
         SphinxProperties properties = getSphinxProperties();
         frameSizeInBytes = properties.getInt
             (FrontEnd.PROP_BYTES_PER_AUDIO_FRAME, 4096);
 
         if (frameSizeInBytes % 2 == 1) {
             frameSizeInBytes++;
         }
 
         keepAudioReference = properties.getBoolean
             (FrontEnd.PROP_KEEP_AUDIO_REFERENCE, true);
     }
 
 
     /**
      * Terminates this Microphone. In this version, it currently
      * does nothing.
      */
     public void terminate() {}
 
 
     /**
      * Returns the current AudioFormat used by this Microphone.
      *
      * @return the current AudioFormat
      */
     public AudioFormat getAudioFormat() {
         return audioFormat;
     }
 
 
     /**
      * Prints the given message to System.out.
      *
      * @param message the message to print
      */
     private void printMessage(String message) {
         logger.info("Microphone: " + message);
     }
 
 
     /**
      * This Thread records audio, and caches them in an audio buffer.
      */
     class RecordingThread extends Thread {
         
         /**
          * Implements the run() method of the Thread class.
          * Records audio, and cache them in the audio buffer.
          */
         public void run() {
             
             if (audioLine != null && audioLine.isOpen()) {
                 
                 Utterance currentUtterance = new Utterance
                     ("Microphone", getContext());
                 utteranceList.add(currentUtterance);
                 
                 audioLine.start();
                 printMessage("started recording");
                                 
                 while (getRecording() && !getClosed()) {
                                         
                     // Read the next chunk of data from the TargetDataLine.
                     byte[] data = new byte[frameSizeInBytes];
                     int numBytesRead = audioLine.read(data, 0, data.length);
                     
                     if (numBytesRead == frameSizeInBytes) {
                         currentUtterance.add(data);
                     } else {
                         numBytesRead = (numBytesRead % 2 == 0) ?
                             numBytesRead + 2 : numBytesRead + 3;
                         
                         byte[] shrinked = new byte[numBytesRead];
                         System.arraycopy(data, 0, shrinked, 0, numBytesRead);
                         currentUtterance.add(shrinked);
                     }
                     
                     printMessage("recorded 1 frame (" + numBytesRead + ") bytes");
                 }
                 
                 audioLine.stop();
                 audioLine.close();
                 
                 printMessage("stopped recording");
                 
             } else {
                 printMessage("Unable to open line");
             }
         }
     }
 
     /**
      * Opens the audio capturing device so that it will be ready
      * for capturing audio.
      *
      * @return true if the audio capturing device is opened successfully;
      *     false otherwise
      */
     private boolean open() {
         DataLine.Info info = new DataLine.Info
             (TargetDataLine.class, audioFormat);
         
         if (!AudioSystem.isLineSupported(info)) {
             logger.severe(audioFormat + " not supported");
             return false;
         }
 
         // Obtain and open the line.
         try {
             audioLine = (TargetDataLine) AudioSystem.getLine(info);
             audioLine.addLineListener(lineListener);
             audioLine.open(audioFormat);
             return true;
         } catch (LineUnavailableException ex) {
             audioLine = null;
             ex.printStackTrace();
             return false;
         }        
     }
 
 
     /**
      * Clears all cached audio data.
      */
     public void clear() {
         utteranceList.clear();
     }
 
 
     /**
      * Starts recording audio. This method will return only
      * when a START event is received, meaning that this Microphone
      * has started capturing audio.
      *
      * @return true if the recording started successfully; false otherwise
      */
     public synchronized boolean startRecording() {
         if (open()) {
             setRecording(true);
             RecordingThread recorder = new RecordingThread();
             recorder.start();
             while (!getStarted()) {
                 try {
                     wait();
                 } catch (InterruptedException ie) {
                     ie.printStackTrace();
                 }
             }
             return true;
         } else {
             return false;
         }
     }
 
 
     /**
      * Stops recording audio.
      */
     public synchronized void stopRecording() {
         if (audioLine != null) {
             setRecording(false);
             setStarted(false);
         }
     }
 
     
     /**
      * Reads and returns the next Audio object from this
      * Microphone, return null if there is no more audio data.
      * All audio data captured in-between <code>startRecording()</code>
      * and <code>stopRecording()</code> is cached in an Utterance
      * object. Calling this method basically returns the next
      * chunk of audio data cached in this Utterance. If the
      * SphinxProperty <pre>
      * edu.cmu.sphinx.frontend.keepAudioReference </pre> is true,
      * then the return Audio object will contain a reference to
      * the original Utterance object.
      *
      * @return the next Audio or <code>null</code> if none is
      *     available
      *
      * @throws java.io.IOException
      */
     public Audio getAudio() throws IOException {
 
         getTimer().start();
 
         Audio output = utteranceList.getNextAudio();
 
         getTimer().stop();
 
         return output;
     }
 
 
     private synchronized boolean getStarted() {
         return started;
     }
 
 
     private synchronized void setStarted(boolean started) {
         this.started = started;
     }
 
 
     /**
      * Returns true if this Microphone is currently
      * in a recording state; false otherwise
      *
      * @return true if recording, false if not recording
      */ 
     public synchronized boolean getRecording() {
         return recording;
     }
 
     
     /**
      * Sets whether this Microphone is in a recording state.
      *
      * @param recording true to set this Microphone
      * in a recording state false to a non-recording state
      */
     private synchronized void setRecording(boolean recording) {
         this.recording = recording;
     }
 
 
     /**
      * Returns true if this Microphone thread finished running.
      * Normally, this Microphone is run in its own thread. If this
      * method returns true, it means the <code>run()</code> method
      * of the thread is finished running.
      *
      * @return true if this Microphone thread has finished running
      */
     private synchronized boolean getClosed() {
         return closed;
     }
 
 
     /**
      * Sets whether to terminate the Microphone thread.
      *
      * @param closed true to terminate the Micrphone thread
      */
     private synchronized void setClosed(boolean closed) {
         this.closed = closed;
     }
 
 
     /**
      * Provides a LineListener for this Microphone
      */
     class MicrophoneLineListener implements LineListener {
 
         /**
          * Implements update() method of LineListener interface.
          * Responds to the START line event by waking up all the
          * threads that are waiting on the Microphone's monitor.
          *
          * @param event the LineEvent to handle
          */
         public void update(LineEvent event) {
             if (event.getType().equals(LineEvent.Type.START)) {
                 setStarted(true);
                 synchronized (Microphone.this) {
                     Microphone.this.notifyAll();
                 }
             }
         }
     }
     
 
     /**
      * A cache of Utterances for live-mode decoding. This inner class
      * allows you add convenient add Utterance objects to it by the
      * <code>add()</code> method, and get Audio objects generated by
      * these Utterances using the <code>getNextAudio()</code> method.
      */
     class UtteranceList {
         
         private List utterances = new LinkedList();
         private Utterance currentUtterance = null;
         
         
         /**
          * Adds an Utterance into this cache.
          *
          * @param utterance the Utterance to add
          */
         public void add(Utterance utterance) {
             synchronized (utterances) {
                 utterances.add(utterance);
             }
         }
 
 
         /**
          * Remove all the Utterances in this UtteranceList.
          */
         public void clear() {
             utterances.clear();
         }
         
         
         /**
          * Returns the next Audio object from this UtteranceList.
          * Audio objects with Signal.UTTERANCE_START and Signal.UTTERANCE_END
          * are returned before and after the Audio objects from each
          * Utterance.
          *
          * @return the next Audio object, or null if no more Audio available
          */
         public Audio getNextAudio() throws IOException {
             Audio output = null;
   
             // If the current Utterance is null, try to get the next
             // one from the list. If a new one is obtained, send
             // an UTTERANCE_START Signal. Otherwise, we really have no
             // more utterances, so return null.
 
             if (currentUtterance == null) {
                 Object first = null;
 
                 synchronized (utterances) {
                     if (utterances.size() > 0) {
                         first = utterances.remove(0);
                     }
                 }
 
                 if (first != null) {
                     currentUtterance = (Utterance) first;
                     output = new Audio(Signal.UTTERANCE_START);
                 } else {
                     // we really have no more utterances
                     output = null;
                 }
             } else {
                 output = readNextFrame();
                 if (output == null) {
                     currentUtterance = null;
                     output = new Audio(Signal.UTTERANCE_END);
                 }
             }
             return output;
         }
         
         
         /**
          * Returns the next Audio object from the current Utterance, 
          * or null if there is none available.
          *
          * @return an Audio object or null
          *
          * @throws java.io.IOException
          */
         private Audio readNextFrame() throws IOException {
             
             // read one frame's worth of bytes
             byte[] audioFrame = null;
             
             do {
                 audioFrame = currentUtterance.getNext();
             } while (audioFrame == null && getRecording());
             
             if (audioFrame == null) {
                 return null;
             }
             
             // turn it into an Audio object
             Audio audio = null;
         
             if (keepAudioReference) {
                 audio = new Audio
                     (Util.byteToDoubleArray(audioFrame, 0, audioFrame.length),
                      currentUtterance);
             } else {
                 audio = new Audio
                     (Util.byteToDoubleArray(audioFrame, 0, audioFrame.length));
             }
             
             if (getDump()) {
                 System.out.println("FRAME_SOURCE " + audio.toString());
             }
             
             return audio;
         }
     }
 }
