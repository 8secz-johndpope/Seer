 /*
  * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
  *
  * Distributable under LGPL license.
  * See terms of license at gnu.org.
  */
 package org.jitsi.impl.neomedia.codec.audio.opus;
 
 import java.awt.*;
 
 import javax.media.*;
 import javax.media.format.*;
 
 import net.sf.fmj.media.*;
 
 import org.jitsi.impl.neomedia.codec.*;
 import org.jitsi.service.neomedia.codec.*;
 import org.jitsi.service.neomedia.control.*;
 import org.jitsi.util.*;
 
 /**
  * Implements an Opus decoder.
  *
  * @author Boris Grozev
  * @author Lyubomir Marinov
  */
 public class JNIDecoder
     extends AbstractCodec2
     implements FECDecoderControl
 {
     /**
      * The <tt>Logger</tt> used by this <tt>JNIDecoder</tt> instance
      * for logging output.
      */
     private static final Logger logger = Logger.getLogger(JNIDecoder.class);
 
     /**
      * The list of <tt>Format</tt>s of audio data supported as input by
      * <tt>JNIDecoder</tt> instances.
      */
     private static final Format[] SUPPORTED_INPUT_FORMATS
         = new Format[] { new AudioFormat(Constants.OPUS_RTP) };
 
     /**
      * The list of <tt>Format</tt>s of audio data supported as output by
      * <tt>JNIDecoder</tt> instances.
      */
     private static final Format[] SUPPORTED_OUTPUT_FORMATS
         = new Format[]
                 {
                     new AudioFormat(
                             AudioFormat.LINEAR,
                             48000,
                             16,
                             1,
                             AudioFormat.LITTLE_ENDIAN,
                             AudioFormat.SIGNED,
                             /* frameSizeInBits */ Format.NOT_SPECIFIED,
                             /* frameRate */ Format.NOT_SPECIFIED,
                             Format.byteArray)
                 };
 
     static
     {
         /*
          * If the Opus class or its supporting JNI library are not functional,
          * it is too late to discover the fact in #doOpen() because a JNIDecoder
          * instance has already been initialized and it has already signaled
          * that the Opus codec is supported. 
          */
         Opus.assertOpusIsFunctional();
     }
 
     /**
      * Number of channels
      */
     private int channels = 1;
 
     /**
      * Pointer to the native OpusDecoder structure
      */
     private long decoder = 0;
 
     /**
      * The size in samples per channel of the last decoded frame in the terms of
      * the Opus library.
      */
     private int lastFrameSizeInSamplesPerChannel;
 
     /**
      * The sequence number of the last processed <tt>Buffer</tt>.
      */
     private long lastSeqNo = Buffer.SEQUENCE_UNKNOWN;
 
     /**
      * Number of packets decoded with FEC
      */
     private int nbDecodedFec = 0;
 
     /**
      * The size in bytes of an audio frame in the terms of the output
      * <tt>AudioFormat</tt> of this instance i.e. based on the values of the
      * <tt>sampleSizeInBits</tt> and <tt>channels</tt> properties of the
      * <tt>outputFormat</tt> of this instance.
      */
     private int outputFrameSize;
 
     /**
      * The sample rate of the audio data output by this instance.
      */
     private int outputSampleRate;
 
     /**
      * Initializes a new <tt>JNIDecoder</tt> instance.
      */
     public JNIDecoder()
     {
         super("Opus JNI Decoder", AudioFormat.class, SUPPORTED_OUTPUT_FORMATS);
 
         inputFormats = SUPPORTED_INPUT_FORMATS;
 
         addControl(this);
     }
 
     /**
      * @see AbstractCodecExt#doClose()
      */
     protected void doClose()
     {
         if (decoder != 0)
         {
             Opus.decoder_destroy(decoder);
             decoder = 0;
         }
     }
 
     /**
      * Opens this <tt>Codec</tt> and acquires the resources that it needs to
      * operate. A call to {@link PlugIn#open()} on this instance will result in
      * a call to <tt>doOpen</tt> only if {@link AbstractCodec#opened} is
      * <tt>false</tt>. All required input and/or output formats are assumed to
      * have been set on this <tt>Codec</tt> before <tt>doOpen</tt> is called.
      *
      * @throws ResourceUnavailableException if any of the resources that this
      * <tt>Codec</tt> needs to operate cannot be acquired
      * @see AbstractCodecExt#doOpen()
      */
     protected void doOpen()
         throws ResourceUnavailableException
     {
         if (decoder == 0)
         {
             decoder = Opus.decoder_create(outputSampleRate, channels);
             if (decoder == 0)
                 throw new ResourceUnavailableException("opus_decoder_create");
 
             lastFrameSizeInSamplesPerChannel = 0;
             lastSeqNo = Buffer.SEQUENCE_UNKNOWN;
         }
     }
 
     /**
      * Decodes an Opus packet
      *
      * @param inBuffer input <tt>Buffer</tt>
      * @param outBuffer output <tt>Buffer</tt>
      * @return <tt>BUFFER_PROCESSED_OK</tt> if <tt>inBuffer</tt> has been
      * successfully processed
      * @see AbstractCodecExt#doProcess(Buffer, Buffer)
      */
     protected int doProcess(Buffer inBuffer, Buffer outBuffer)
     {
         Format inFormat = inBuffer.getFormat();
 
         if ((inFormat != null)
                 && (inFormat != this.inputFormat)
                 && !inFormat.equals(this.inputFormat)
                 && (null == setInputFormat(inFormat)))
         {
             return BUFFER_PROCESSED_FAILED;
         }
 
         long seqNo = inBuffer.getSequenceNumber();
         boolean decodeFEC = false;
 
         /*
          * Detect lost packets. (Take into account sequence number wrapping.)
          */
         if ((lastSeqNo != Buffer.SEQUENCE_UNKNOWN)
                 && (seqNo != lastSeqNo + 1)
                 && (seqNo > lastSeqNo)
                 && (lastFrameSizeInSamplesPerChannel > 0))
         {
             /*
              * When no in-band forward error correction data is available, the
              * Opus decoder will operate as if PLC has been specified.
              */
             decodeFEC = true;
         }
         if ((inBuffer.getFlags() & Buffer.FLAG_SKIP_FEC) != 0)
         {
             decodeFEC = false;
             if (logger.isTraceEnabled())
             {
                 logger.trace(
                         "Not decoding FEC for " + seqNo
                             + " because of Buffer.FLAG_SKIP_FEC.");
             }
         }
 
         // After we have determined what is to be decoded, do decode it.
         byte[] in = (byte[]) inBuffer.getData();
         int inOffset = inBuffer.getOffset();
         int inLength = inBuffer.getLength();
         int outOffset = 0;
         int outLength = 0;
         int totalFrameSizeInSamplesPerChannel = 0;
 
         if (decodeFEC)
         {
             byte[] out
                 = validateByteArraySize(
                         outBuffer,
                         outOffset
                             + lastFrameSizeInSamplesPerChannel
                                 * outputFrameSize,
                         outOffset != 0);
             int frameSizeInSamplesPerChannel
                 = Opus.decode(
                         decoder,
                         in, inOffset, inLength,
                         out, /* outOffset, */ lastFrameSizeInSamplesPerChannel,
                        /* decodeFEC */ 1);
 
             if (frameSizeInSamplesPerChannel > 0)
             {
                 int frameSizeInBytes
                     = frameSizeInSamplesPerChannel * outputFrameSize;
 
                 outLength += frameSizeInBytes;
                 outOffset += frameSizeInBytes;
                 totalFrameSizeInSamplesPerChannel
                     += frameSizeInSamplesPerChannel;
 
                 outBuffer.setFlags(outBuffer.getFlags() | BUFFER_FLAG_FEC);
                 nbDecodedFec++;
             }
 
             lastSeqNo++;
             if (lastSeqNo > 65535)
                 lastSeqNo = 0;
         }
         else
         {
             int frameSizeInSamplesPerChannel
                 = Opus.decoder_get_nb_samples(decoder, in, inOffset, inLength);
             byte[] out
                 = validateByteArraySize(
                         outBuffer,
                         outOffset
                             + frameSizeInSamplesPerChannel * outputFrameSize,
                         outOffset != 0);
 
             frameSizeInSamplesPerChannel
                 = Opus.decode(
                         decoder,
                         in, inOffset, inLength,
                         out, /* outOffset, */ frameSizeInSamplesPerChannel,
                        /* decodeFEC */ 0);
             if (frameSizeInSamplesPerChannel > 0)
             {
                 int frameSizeInBytes
                     = frameSizeInSamplesPerChannel * outputFrameSize;
 
                 outLength += frameSizeInBytes;
                 outOffset += frameSizeInBytes;
                 totalFrameSizeInSamplesPerChannel
                     += frameSizeInSamplesPerChannel;
 
                 outBuffer.setFlags(outBuffer.getFlags() & ~BUFFER_FLAG_FEC);
 
                 /*
                  * When we encounter a lost frame, we will presume that it was
                  * of the same duration as the last received frame.
                  */
                 lastFrameSizeInSamplesPerChannel = frameSizeInSamplesPerChannel;
             }
 
             lastSeqNo = seqNo;
         }
 
         if (outLength > 0)
         {
             outBuffer.setDuration(
                     totalFrameSizeInSamplesPerChannel * channels * 1000L * 1000L
                         / outputSampleRate);
             outBuffer.setFormat(getOutputFormat());
             outBuffer.setLength(outLength);
             outBuffer.setOffset(0);
         }
         else
         {
             outBuffer.setLength(0);
             discardOutputBuffer(outBuffer);
         }
 
         if (lastSeqNo == seqNo)
             return BUFFER_PROCESSED_OK;
         else
             return INPUT_BUFFER_NOT_CONSUMED;
     }
 
     /**
      * Returns the number of packets decoded with FEC.
      *
      * @return the number of packets decoded with FEC
      */
     public int fecPacketsDecoded()
     {
         return nbDecodedFec;
     }
 
     /**
      * Implements {@link Control#getControlComponent()}. <tt>JNIDecoder</tt>
      * does not provide user interface of its own.
      *
      * @return <tt>null</tt> to signify that <tt>JNIDecoder</tt> does not
      * provide user interface of its own 
      */
     public Component getControlComponent()
     {
         return null;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     protected Format[] getMatchingOutputFormats(Format inputFormat)
     {
         AudioFormat inputAudioFormat = (AudioFormat) inputFormat;
 
         return
             new Format[]
                     {
                         new AudioFormat(
                                 AudioFormat.LINEAR,
                                 inputAudioFormat.getSampleRate(),
                                 16,
                                 1,
                                 AudioFormat.LITTLE_ENDIAN,
                                 AudioFormat.SIGNED,
                                 /* frameSizeInBits */ Format.NOT_SPECIFIED,
                                 /* frameRate */ Format.NOT_SPECIFIED,
                                 Format.byteArray)
                     };
     }
 
     /**
      * {@inheritDoc}
      *
      * Makes sure that the <tt>outputFormat</tt> of this instance is in accord
      * with the <tt>inputFormat</tt> of this instance.
      */
     @Override
     public Format setInputFormat(Format format)
     {
         Format inFormat = super.setInputFormat(format);
 
         if (inFormat != null)
         {
             double outSampleRate;
             int outChannels;
 
             if (outputFormat == null)
             {
                 outSampleRate = Format.NOT_SPECIFIED;
                 outChannels = Format.NOT_SPECIFIED;
             }
             else
             {
                 AudioFormat outAudioFormat = (AudioFormat) outputFormat;
 
                 outSampleRate = outAudioFormat.getSampleRate();
                 outChannels = outAudioFormat.getChannels();
             }
 
             AudioFormat inAudioFormat = (AudioFormat) inFormat;
             double inSampleRate = inAudioFormat.getSampleRate();
             int inChannels = inAudioFormat.getChannels();
 
             if ((outSampleRate != inSampleRate) || (outChannels != inChannels))
             {
                 setOutputFormat(
                         new AudioFormat(
                                 AudioFormat.LINEAR,
                                 inSampleRate,
                                 16,
                                 inChannels,
                                 AudioFormat.LITTLE_ENDIAN,
                                 AudioFormat.SIGNED,
                                 /* frameSizeInBits */ Format.NOT_SPECIFIED,
                                 /* frameRate */ Format.NOT_SPECIFIED,
                                 Format.byteArray));
             }
         }
         return inFormat;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public Format setOutputFormat(Format format)
     {
         Format setOutputFormat = super.setOutputFormat(format);
 
         if (setOutputFormat != null)
         {
             AudioFormat af = (AudioFormat) setOutputFormat;
 
             outputFrameSize = (af.getSampleSizeInBits() / 8) * af.getChannels();
             outputSampleRate = (int) af.getSampleRate();
         }
         return setOutputFormat;
     }
 }
