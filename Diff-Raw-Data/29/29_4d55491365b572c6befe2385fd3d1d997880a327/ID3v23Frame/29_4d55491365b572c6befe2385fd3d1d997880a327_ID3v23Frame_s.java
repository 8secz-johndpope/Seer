 /**
  *  Amended @author : Paul Taylor
  *  Initial @author : Eric Farng
  *
  *  Version @version:$Id$
  *
  *  MusicTag Copyright (C)2003,2004
  *
  *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
  *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
  *  or (at your option) any later version.
  *
  *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
  *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *  See the GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
  *  you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
  *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
  *
  * Description:
  * This class is the tag frame header used for ID3v2.30 tags
  *
  */
 package org.jaudiotagger.tag.id3;
 
 import org.jaudiotagger.audio.mp3.*;
 import org.jaudiotagger.tag.*;
 import org.jaudiotagger.tag.id3.framebody.FrameBodyUnsupported;
 import org.jaudiotagger.tag.id3.framebody.AbstractID3v2FrameBody;
 import org.jaudiotagger.FileConstants;
 
 import java.io.IOException;
 import java.io.RandomAccessFile;
 import java.util.regex.*;
 
 import java.nio.*;
 
 public class ID3v23Frame
     extends ID3v22Frame
 {
     Pattern validFrameIdentifier = Pattern.compile("[A-Z][0-9A-Z]{3}");
 
     protected static int FRAME_ID_SIZE = 4;
     protected static int FRAME_FLAGS_SIZE = 2;
     protected static int FRAME_SIZE_SIZE = 4;
     protected static int FRAME_HEADER_SIZE = FRAME_ID_SIZE + FRAME_SIZE_SIZE + FRAME_FLAGS_SIZE;
 
     /**
      * Creates a new ID3v2_3Frame datatype.
      */
     public ID3v23Frame()
     {
     }
 
     /**
      * Creates a new ID3v2_3Frame of type identifier. An empty
      * body of the correct type will be automatically created.
      * This constructor should be used when wish to create a new
      * frame from scratch using user data.
      *
      * @param body DOCUMENT ME!
      */
     public ID3v23Frame(String identifier)
     {
         super(identifier);
         statusFlags = new StatusFlags();
         encodingFlags = new EncodingFlags();
     }
 
     /**
      * Copy Constructor:
      * Creates a new v2.3 frame datatype based on another v2.3 frame
      */
     public ID3v23Frame(ID3v23Frame frame)
     {
         super(frame);
         statusFlags = new StatusFlags(frame.getStatusFlags().getOriginalFlags());
         encodingFlags = new EncodingFlags(frame.getEncodingFlags().getFlags());
     }
 
     /**
      * Creates a new ID3v2_3Frame datatype based on another frame.
      *
      * @param frame DOCUMENT ME!
      */
     public ID3v23Frame(AbstractID3v2Frame frame)
     {
         logger.info("Creating frame from a frame of a different version");
         if ((frame instanceof ID3v23Frame == true) && (frame instanceof ID3v24Frame == false))
         {
             throw new UnsupportedOperationException("Copy Constructor not called. Please type cast the argument");
         }
         if (frame instanceof ID3v24Frame)
         {
             statusFlags = new StatusFlags((ID3v24Frame.StatusFlags) ((ID3v24Frame) frame).getStatusFlags());
             encodingFlags = new EncodingFlags(((ID3v23Frame) frame).getEncodingFlags().getFlags());
         }
         try
         {
             if (frame instanceof ID3v24Frame)
             {
                 /** Version between v4 and v3 */
                 identifier = ID3Tags.convertFrameID24To23(frame.getIdentifier());
                 if (identifier != null)
                 {
                     logger.info("V3:Orig id is:" + frame.getIdentifier() + ":New id is:" + identifier);
                     this.frameBody = (AbstractID3v2FrameBody) ID3Tags.copyObject(frame.getBody());
                     return;
                 }
                 /** Is it a known v4 frame which needs forcing to v3 frame e.g. TDRC - TYER,TDAT */
                 else if (ID3Tags.isID3v24FrameIdentifier(frame.getIdentifier()) == true)
                 {
                     identifier = ID3Tags.forceFrameID24To23(frame.getIdentifier());
                     logger.info("V3:Orig id is:" + frame.getIdentifier() + ":New id is:" + identifier);
                     this.frameBody = this.readBody(identifier, (AbstractID3v2FrameBody) frame.getBody());
                     return;
                 }
                 /** Unknown Frame e.g NCON */
                 this.frameBody = new FrameBodyUnsupported((FrameBodyUnsupported) frame.getBody());
                 identifier = frame.getIdentifier();
                 logger.info("UNKNOWN:Orig id is:" + frame.getIdentifier() + ":New id is:" + identifier);
                 return;
             }
             else if (frame instanceof ID3v22Frame)
             {
                 identifier = ID3Tags.convertFrameID22To23(frame.getIdentifier());
                 if (identifier != null)
                 {
                     logger.info("V3:Orig id is:" + frame.getIdentifier() + ":New id is:" + identifier);
                     this.frameBody = (AbstractID3v2FrameBody) ID3Tags.copyObject(frame.getBody());
                     return;
                 }
                 /** Unknown Frame e.g NCON */
                 this.frameBody = new FrameBodyUnsupported((FrameBodyUnsupported) frame.getBody());
                 identifier = frame.getIdentifier();
                 logger.info("UNKNOWN:Orig id is:" + frame.getIdentifier() + ":New id is:" + identifier);
                 return;
             }
         }
         catch (Exception e)
         {
             logger.warning("Unable to convert to v23 Frame:Frame Identifier" + frame.getIdentifier());
         }
         logger.info("Created frame from a frame of a different version");
     }
 
     /**
      * Creates a new ID3v2_3Frame datatype by reading from file.
      *
      * @param file The mp3File to read from
      * @throws IOException         DOCUMENT ME!
      * @throws InvalidTagException DOCUMENT ME!
      */
     public ID3v23Frame(RandomAccessFile file)
         throws IOException, InvalidFrameException
     {
         this.read(file);
     }
 
     /**
      * Return size of frame
      *
      * @return int frame size
      */
     public int getSize()
     {
         return frameBody.getSize() + FRAME_HEADER_SIZE;
     }
 
     /**
      * Compare for equality
      * To be deemed equal obj must be a IDv23Frame with the same identifier
      * and the same flags.
      * containing the same body,datatype list ectera.
      * equals() method is made up from all the various components
      *
      * @param obj DOCUMENT ME!
      * @return DOCUMENT ME!
      */
     public boolean equals(Object obj)
     {
         if ((obj instanceof ID3v23Frame) == false)
         {
             return false;
         }
         ID3v23Frame object = (ID3v23Frame) obj;
         if (this.statusFlags.getOriginalFlags() != object.statusFlags.getOriginalFlags())
         {
             return false;
         }
         if (this.encodingFlags.getFlags() != object.encodingFlags.getFlags())
         {
             return false;
         }
         return super.equals(obj);
     }
 
     /**
      * Read the frame from file
      *
      * @param file DOCUMENT ME!
      * @throws IOException         DOCUMENT ME!
      * @throws InvalidTagException DOCUMENT ME!
      */
     public void read(RandomAccessFile file)
         throws IOException, InvalidFrameException
     {
         logger.info("Read Frame from file");
         byte[] buffer = new byte[FRAME_ID_SIZE];
 
         // Read the Frame ID Identifier
         file.read(buffer, 0, FRAME_ID_SIZE);
         identifier = new String(buffer);
         // Is this a valid identifier?
         if (isValidID3v2FrameIdentifier(identifier) == false)
         {
             logger.info("Invalid identifier:" + identifier);
             file.seek(file.getFilePointer() - (FRAME_ID_SIZE - 1));
             throw new InvalidFrameException(identifier + " is not a valid ID3v2.30 frame");
         }
         //Read the size field
         frameSize = file.readInt();
         if (frameSize < 0)
         {
             logger.warning("Invalid Frame Size:" + identifier);
             throw new InvalidFrameException(identifier + " is invalid frame");
         }
         if (frameSize == 0)
         {
             logger.warning("Empty Frame Size:" + identifier);
             throw new EmptyFrameException(identifier + " is empty frame");
         }
        if (frameSize > MAX_FRAME_SIZE)
        {
            logger.warning("Invalid Frame Size Reported :" + identifier + "Reported as" + frameSize +
                " Must be frame misread");
            throw new InvalidFrameException("Frame Size Reported :" + identifier + "Reported as" + frameSize +
                " Must be frame misread");
        }
         //Read the flag bytes
         statusFlags = new StatusFlags(file.readByte());
         encodingFlags = new EncodingFlags(file.readByte());
         String id;
         /** If this identifier a valid v24 identifier or easily converted to v24 */
         id = (String) ID3Tags.convertFrameID23To24(identifier);
         /** Cant easily be converted to v23 but is it a valid v24 identifier */
         if (id == null)
         {
             /** It is a valid v23 identifier so should be able to find a
              *  frame body for it.
              */
             if (ID3Tags.isID3v23FrameIdentifier(identifier) == true)
             {
                 id = identifier;
             }
             /** Unknown so will be created as FrameBodyUnsupported
              *
              */
             else
             {
                 id = UNSUPPORTED_ID;
             }
         }
         logger.fine("Identifier was:" + identifier + " reading using:" + id);
         //Read the body data
         frameBody = readBody(id, file, frameSize);
     }
 
     /**
      * Write the frame to file
      *
      * @param file DOCUMENT ME!
      * @throws IOException DOCUMENT ME!
      */
     public void write(ByteBuffer tagBuffer)
         throws IOException
     {
         logger.info("Writing frame to file:" + getIdentifier());
         //This is where we will write header, move position to where we can
         //write body
         ByteBuffer headerBuffer = tagBuffer.slice();
         tagBuffer.position(tagBuffer.position() + this.FRAME_HEADER_SIZE);
         //Write Frame Body Data
         ((AbstractID3v2FrameBody) frameBody).write(tagBuffer);
         //Write Frame Header
         //Write Frame ID
         if (getIdentifier().length() == 3)
         {
             identifier = identifier + ' ';
         }
         headerBuffer.put(getIdentifier().getBytes(), 0, FRAME_ID_SIZE);
         //Write Frame Size
         int size = frameBody.getSize();
         logger.fine("Frame Size Is:" + size);
         headerBuffer.putInt(frameBody.getSize());
         //Write the Flags
         //@todo What about adjustments to header based on encoding flag
         headerBuffer.put(statusFlags.getWriteFlags());
         headerBuffer.put(encodingFlags.getFlags());
     }
 
     protected AbstractID3v2Frame.StatusFlags getStatusFlags()
     {
         return statusFlags;
     }
 
     protected AbstractID3v2Frame.EncodingFlags getEncodingFlags()
     {
         return encodingFlags;
     }
 
     /**
      * This represents a frame headers Status Flags
      * Make adjustments if necessary based on frame type and specification.
      */
     class StatusFlags
         extends AbstractID3v2Frame.StatusFlags
     {
         public static final String TYPE_TAGALTERPRESERVATION = "typeTagAlterPreservation";
         public static final String TYPE_FILEALTERPRESERVATION = "typeFileAlterPreservation";
         public static final String TYPE_READONLY = "typeReadOnly";
 
         /**
          * Discard frame if tag altered
          */
         public static final int MASK_TAG_ALTER_PRESERVATION = FileConstants.BIT7;
 
         /**
          * Discard frame if audio file part  altered
          */
         public static final int MASK_FILE_ALTER_PRESERVATION = FileConstants.BIT6;
 
         /**
          * Frame tagged as read only
          */
         public static final int MASK_READ_ONLY = FileConstants.BIT5;
 
         public StatusFlags()
         {
             originalFlags = (byte) 0;
             writeFlags = (byte) 0;
         }
 
         StatusFlags(byte flags)
         {
             originalFlags = flags;
             writeFlags = flags;
             modifyFlags();
         }
 
         /**
          * Use this constructor when convert a v24 frame
          */
         StatusFlags(ID3v24Frame.StatusFlags statusFlags)
         {
             originalFlags = convertV4ToV3Flags(statusFlags.getOriginalFlags());
             writeFlags = originalFlags;
             modifyFlags();
         }
 
         private byte convertV4ToV3Flags(byte v4Flag)
         {
             byte v3Flag = 0;
             if ((v4Flag & ID3v24Frame.StatusFlags.MASK_FILE_ALTER_PRESERVATION) != 0)
             {
                 v3Flag |= MASK_FILE_ALTER_PRESERVATION;
             }
             if ((v4Flag & ID3v24Frame.StatusFlags.MASK_TAG_ALTER_PRESERVATION) != 0)
             {
                 v3Flag |= MASK_TAG_ALTER_PRESERVATION;
             }
             return v3Flag;
         }
 
         protected void modifyFlags()
         {
             String str = getIdentifier();
             if (ID3v23Frames.getInstanceOf().isDiscardIfFileAltered(str) == true)
             {
                 writeFlags |= MASK_FILE_ALTER_PRESERVATION;
                 writeFlags &= ~MASK_TAG_ALTER_PRESERVATION;
             }
             else
             {
                 writeFlags &= ~MASK_FILE_ALTER_PRESERVATION;
                 writeFlags &= ~MASK_TAG_ALTER_PRESERVATION;
             }
         }
 
         public void createStructure()
         {
             MP3File.getStructureFormatter().openHeadingElement(TYPE_FLAGS, "");
             MP3File.getStructureFormatter().addElement(TYPE_TAGALTERPRESERVATION, originalFlags & MASK_TAG_ALTER_PRESERVATION);
             MP3File.getStructureFormatter().addElement(TYPE_FILEALTERPRESERVATION, originalFlags & MASK_FILE_ALTER_PRESERVATION);
             MP3File.getStructureFormatter().addElement(TYPE_READONLY, originalFlags & MASK_READ_ONLY);
             MP3File.getStructureFormatter().closeHeadingElement(TYPE_FLAGS);
         }
     }
 
     /**
      * This represents a frame headers Encoding Flags
      */
     class EncodingFlags
         extends AbstractID3v2Frame.EncodingFlags
     {
         public static final String TYPE_COMPRESSION = "compression";
         public static final String TYPE_ENCRYPTION = "encryption";
         public static final String TYPE_GROUPIDENTITY = "groupidentity";
 
 
         /**
          * Frame is compressed
          */
         public static final int MASK_COMPRESSION = FileConstants.BIT7;
 
         /**
          * Frame is encrypted
          */
         public static final int MASK_ENCRYPTION = FileConstants.BIT6;
 
         /**
          * Frame is part of a group
          */
         public static final int MASK_GROUPING_IDENTITY = FileConstants.BIT5;
 
         public EncodingFlags()
         {
             this((byte) 0);
         }
 
         public EncodingFlags(byte flags)
         {
             this.flags = flags;
         }
 
         public void createStructure()
         {
             MP3File.getStructureFormatter().openHeadingElement(TYPE_FLAGS, "");
             MP3File.getStructureFormatter().addElement(TYPE_COMPRESSION, flags & MASK_COMPRESSION);
             MP3File.getStructureFormatter().addElement(TYPE_ENCRYPTION, flags & MASK_ENCRYPTION);
             MP3File.getStructureFormatter().addElement(TYPE_GROUPIDENTITY, flags & MASK_GROUPING_IDENTITY);
             MP3File.getStructureFormatter().closeHeadingElement(TYPE_FLAGS);
         }
     }
 
     /**
      * Does the frame identifier meet the syntax for a idv3v2 frame identifier.
      * must start with a capital letter and only contain capital letters and numbers
      *
      * @param identifier DOCUMENT ME!
      * @return DOCUMENT ME!
      */
     public boolean isValidID3v2FrameIdentifier(String identifier)
     {
         Matcher m = validFrameIdentifier.matcher(identifier);
         return m.matches();
     }
 
     /**
      * Return String Representation of body
      *
      * @return DOCUMENT ME!
      */
     public void createStructure()
     {
         MP3File.getStructureFormatter().openHeadingElement(TYPE_FRAME, getIdentifier());
         MP3File.getStructureFormatter().addElement(TYPE_FRAME_SIZE, frameSize);
         statusFlags.createStructure();
         encodingFlags.createStructure();
         frameBody.createStructure();
         MP3File.getStructureFormatter().closeHeadingElement(TYPE_FRAME);
     }
 
 }
