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
 
 
 package edu.cmu.sphinx.frontend.endpoint;
 
 import edu.cmu.sphinx.frontend.Audio;
 import edu.cmu.sphinx.frontend.AudioSource;
 import edu.cmu.sphinx.frontend.DataProcessor;
 import edu.cmu.sphinx.frontend.Signal;
 
 import edu.cmu.sphinx.util.SphinxProperties;
 import edu.cmu.sphinx.util.LogMath;
 
 import java.io.IOException;
 
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 
 
 /**
  * Implements a level tracking endpointer invented by Bent Schmidt Nielsen.
  *
  * <p>This endpointer is composed of three main steps.
  * <ol>
  * <li>classification of audio into speech and non-speech
  * <li>inserting SPEECH_START and SPEECH_END signals around speech
  * <li>removing non-speech regions
  * </ol>
  *
  * <p>The first step, classification of audio into speech and non-speech,
  * uses Bent Schmidt Nielsen's algorithm. Each time audio comes in, 
  * the average signal level and the background noise level are updated,
  * using the signal level of the current audio. If the average signal
  * level is greater than the background noise level by a certain
  * threshold value (configurable), then the current audio is marked
  * as speech. Otherwise, it is marked as non-speech.
  *
  * <p>The second and third step of this endpointer are documented in the
  * classes SpeechMarker and AudioFilter.
  *
  * @see SpeechMarker
  * @see AudioFilter
  */
 public class LevelTracker extends DataProcessor implements AudioEndpointer {
 
     /**
      * Prefix for the SphinxProperties of this class.
      */
     public static final String PROP_PREFIX = 
         "edu.cmu.sphinx.frontend.endpoint.LevelTracker.";
 
     /**
      * The SphinxProperty specifying the endpointing frame length.
      * This is the number of samples in each endpointed frame.
      */
     public static final String PROP_FRAME_LENGTH = PROP_PREFIX + "frameLength";
 
     /**
      * The default value of PROP_FRAME_LENGTH.
      */
     public static final int PROP_FRAME_LENGTH_DEFAULT = 160;
 
     /**
      * The SphinxProperty specifying the minimum signal level used
      * to update the background signal level.
      */
     public static final String PROP_MIN_SIGNAL = PROP_PREFIX + "minSignal";
 
     /**
      * The default value of PROP_MIN_SIGNAL.
      */
     public static final double PROP_MIN_SIGNAL_DEFAULT = 0;
 
     /**
      * The SphinxProperty specifying the threshold. If the current signal
      * level is greater than the background level by this threshold,
      * then the current signal is marked as speech.
      */
     public static final String PROP_THRESHOLD = PROP_PREFIX + "threshold";
 
     /**
      * The default value of PROP_THRESHOLD.
      */
     public static final double PROP_THRESHOLD_DEFAULT = 10;
 
     /**
      * The SphinxProperty specifying the adjustment.
      */
     public static final String PROP_ADJUSTMENT = PROP_PREFIX + "adjustment";
 
     /**
      * The default value of PROP_ADJUSTMENT_DEFAULT.
      */
     public static final double PROP_ADJUSTMENT_DEFAULT = 0.003;
 
     /**
      * The SphinxProperty specifying whether to print debug messages.
      */
     public static final String PROP_DEBUG = PROP_PREFIX + "debug";
 
     /**
      * The default value of PROP_DEBUG.
      */
     public static final boolean PROP_DEBUG_DEFAULT = false;
     
 
     private boolean debug;
     private double averageNumber = 1;
     private double adjustment;
     private double level;               // average signal level
     private double background;          // background signal level
     private double minSignal;           // minimum valid signal level
     private double threshold;
     private int frameLength;
     private AudioSource predecessor;
     private SpeechMarker speechMarker;
     private AudioFilter filter;
     
 
     /**
      * Constructs an un-initialized LevelTracker.
      */
     public LevelTracker() {}
 
     /**
      * Initializes this LevelTracker endpointer with the given name, context,
      * and AudioSource predecessor.
      *
      * @param name the name of this LevelTracker
      * @param context the context of the SphinxProperties this
      *    LevelTracker use
      * @param props the SphinxProperties to read properties from
      * @param predecessor the AudioSource where this LevelTracker
      *    gets Audio from
      *
      * @throws java.io.IOException
      */
     public void initialize(String name, String context, 
                            SphinxProperties props,
                            AudioSource predecessor) throws IOException {
         super.initialize(name, context, props);
         this.predecessor = predecessor;
         reset();
         setProperties();
         speechMarker = new SpeechMarker();
         speechMarker.initialize
             ("SpeechMarker", getContext(), getSphinxProperties(), 
              new SpeechClassifier());
         filter = new AudioFilter
             ("AudioFilter", getContext(), getSphinxProperties(),
              speechMarker);
     }
 
     /**
      * Sets the properties for this LevelTracker.
      */
     private void setProperties() {
         SphinxProperties props = getSphinxProperties();
         frameLength = props.getInt
             (PROP_FRAME_LENGTH, PROP_FRAME_LENGTH_DEFAULT);
         adjustment = props.getDouble(PROP_ADJUSTMENT, PROP_ADJUSTMENT_DEFAULT);
         threshold = props.getDouble(PROP_THRESHOLD, PROP_THRESHOLD_DEFAULT);
         minSignal = props.getDouble(PROP_MIN_SIGNAL, PROP_MIN_SIGNAL_DEFAULT);
         debug = props.getBoolean(PROP_DEBUG, PROP_DEBUG_DEFAULT);
     }
 
     /**
      * Resets this LevelTracker to a starting state.
      */
     private void reset() {
         level = 0;
         background = 100;
     }
 
     /**
     * Returns the next Audio object, which are already classified
      * as speech or non-speech.
      *
      * @return the next Audio object, or null if none available
      *
      * @throws java.io.IOException if an error occurred
      *
      * @see Audio
      */
     public Audio getAudio() throws IOException {
         return filter.getAudio();            
     }
 
     /**
      * Classifies Audio into either speech or non-speech.
      */
     class SpeechClassifier implements AudioSource {
 
         List outputQueue = new LinkedList();
         
         /**
          * Returns the logarithm base 10 of the root mean square of the
          * given samples.
          *
          * @param sample the samples
          *
          * @return the calculated log root mean square in log 10
          */
         private double logRootMeanSquare(double[] samples) {
             assert samples.length > 0;
             double sumOfSquares = 0.0f;
             for (int i = 0; i < samples.length; i++) {
                 double sample = samples[i];
                 sumOfSquares += sample * sample;
             }
             double rootMeanSquare = Math.sqrt
                 ((double)sumOfSquares/samples.length);
             rootMeanSquare = Math.max(rootMeanSquare, 1);
             return (LogMath.log10((float)rootMeanSquare) * 20);
         }
         
         /**
          * Classifies the given audio frame as speech or not, and updates
          * the endpointing parameters.
          *
          * @param audio the audio frame
          */
         private void classify(Audio audio) {
             double current = logRootMeanSquare(audio.getSamples());
             // System.out.println("current: " + current);
             boolean isSpeech = false;
             if (current >= minSignal) {
                 level = ((level*averageNumber) + current)/(averageNumber + 1);
                 if (current < background) {
                     background = current;
                 } else {
                     background += (current - background) * adjustment;
                 }
                 if (level < background) {
                     level = background;
                 }
                 isSpeech = (level - background > threshold);
             }
             audio.setSpeech(isSpeech);
             if (debug) {
                 String speech = "";
                 if (audio.isSpeech()) {
                     speech = "*";
                 }
                 System.out.println("Bkg: " + background + ", level: " + level +
                                    ", current: " + current + " " + speech);
             }
             outputQueue.add(audio);
         }
         
         /**
          * Returns the next Audio object.
          *
          * @return the next Audio object, or null if none available
          *
          * @throws java.io.IOException if an error occurred
          *
          * @see Audio
          */
         public Audio getAudio() throws IOException {
             if (outputQueue.size() == 0) {
                 Audio audio = predecessor.getAudio();
                 if (audio != null) {
                     /*
                     System.out.println("LevelTracker: incoming: " +
                                        audio.getSignal().toString());
                     */
                     audio.setSpeech(false);
                     if (audio.hasContent()) {
                        assert audio.getSamples().length <= frameLength;
                         classify(audio);
                     } else {
                         outputQueue.add(audio);
                     }
                 }
             }
             if (outputQueue.size() > 0) {
                 Audio audio = (Audio) outputQueue.remove(0);
                 return audio;
             } else {
                 return null;
             }
         }
     }
 }
