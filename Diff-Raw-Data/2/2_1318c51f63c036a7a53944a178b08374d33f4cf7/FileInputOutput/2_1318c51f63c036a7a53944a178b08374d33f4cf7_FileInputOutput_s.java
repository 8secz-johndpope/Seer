 /*
  * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
  *
  * This file is part of Resin(R) Open Source
  *
  * Each copy or derived work must preserve the copyright notice and this
  * notice unmodified.
  *
  * Resin Open Source is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * Resin Open Source is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
  * of NON-INFRINGEMENT.  See the GNU General Public License for more
  * details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Resin Open Source; if not, write to the
  *
  *   Free Software Foundation, Inc.
  *   59 Temple Place, Suite 330
  *   Boston, MA 02111-1307  USA
  *
  * @author Emil Ong
  */
 
 package com.caucho.quercus.lib.file;
 
 import com.caucho.quercus.env.BytesBuilderValue;
 import com.caucho.quercus.env.BytesValue;
 import com.caucho.quercus.env.Env;
 import com.caucho.quercus.env.UnicodeBuilderValue;
 import com.caucho.quercus.env.StringValue;
 import com.caucho.quercus.env.Value;
 import com.caucho.vfs.Encoding;
 import com.caucho.vfs.FilePath;
 import com.caucho.vfs.Path;
 import com.caucho.vfs.RandomAccessStream;
 
 import java.io.*;
 import java.nio.channels.FileChannel;
 import java.nio.channels.FileLock;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 /**
  * Represents a PHP open file
  */
 public class FileInputOutput extends AbstractBinaryOutput
   implements BinaryInput, BinaryOutput, Closeable
 {
   private static final Logger log
     = Logger.getLogger(FileInputOutput.class.getName());
 
   private Env _env;
   private Path _path;
   private LineReader _lineReader;
 
   private RandomAccessStream _stream;
   private int _buffer;
   private boolean _doUnread = false;
 
   private Reader _readEncoding;
   private String _readEncodingName;
 
   private FileLock _fileLock;
   private RandomAccessFile _randomAccessFile;
 
   private boolean _temporary;
 
   public FileInputOutput(Env env, Path path)
     throws IOException
   {
     this(env, path, false, false, false);
   }
 
   public FileInputOutput(Env env, Path path, boolean append, boolean truncate)
     throws IOException
   {
     this(env, path, append, truncate, false);
   }
 
   public FileInputOutput(Env env, Path path, 
                          boolean append, boolean truncate, boolean temporary)
     throws IOException
   {
     _env = env;
     
     env.addClose(this);
 
     _path = path;
 
     _lineReader = new LineReader(env);
 
     if (truncate)
       path.truncate(0L);
 
     _stream = path.openRandomAccess();
 
     if (append && _stream.getLength() > 0)
       _stream.seek(_stream.getLength());
 
     _temporary = temporary;
   }
 
   /**
    * Returns the write stream.
    */
   public OutputStream getOutputStream()
   {
     try {
       return _stream.getOutputStream();
     } catch (IOException e) {
       log.log(Level.FINE, e.toString(), e);
 
       return null;
     }
   }
 
   /**
    * Returns the read stream.
    */
   public InputStream getInputStream()
   {
     try {
       return _stream.getInputStream();
     } catch (IOException e) {
       log.log(Level.FINE, e.toString(), e);
 
       return null;
     }
   }
 
   /**
    * Returns the path.
    */
   public Path getPath()
   {
     return _path;
   }
 
   /**
    * Sets the current read encoding.  The encoding can either be a
    * Java encoding name or a mime encoding.
    *
    * @param encoding name of the read encoding
    */
   public void setEncoding(String encoding)
     throws UnsupportedEncodingException
   {
     String mimeName = Encoding.getMimeName(encoding);
     
     if (mimeName != null && mimeName.equals(_readEncodingName))
       return;
     
     _readEncoding = Encoding.getReadEncoding(getInputStream(), encoding);
     _readEncodingName = mimeName;
   }
 
   private int readChar()
     throws IOException
   {
     if (_readEncoding != null) {
       int ch = _readEncoding.read();
       return ch;
     }
 
     return read() & 0xff;
   }
 
   /**
    * Unread a character.
    */
   public void unread()
     throws IOException
   {
     _doUnread = true;
   }
 
   /**
    * Reads a character from a file, returning -1 on EOF.
    */
   public int read()
     throws IOException
   {
     if (_doUnread) {
       _doUnread = false;
 
       return _buffer;
     } else {
       _buffer = _stream.read();
       
       return _buffer;
     }
   }
 
   /**
    * Reads a buffer from a file, returning -1 on EOF.
    */
   public int read(byte []buffer, int offset, int length)
     throws IOException
   {
     _doUnread = false;
 
     return _stream.read(buffer, offset, length);
   }
 
   /**
    * Reads a Binary string.
    */
   public BytesValue read(int length)
     throws IOException
   {
     BytesBuilderValue bb = new BytesBuilderValue();
 
     while (length > 0) {
       bb.prepareReadBuffer();
 
       int sublen = bb.getLength() - bb.getOffset();
 
       if (length < sublen)
         sublen = length;
 
       sublen = read(bb.getBuffer(), bb.getOffset(), sublen);
 
       if (sublen > 0) {
         bb.setOffset(bb.getOffset() + sublen);
         length -= sublen;
       }
       else
         return bb;
     }
 
     return bb;
   }
 
   /**
    * Reads the optional linefeed character from a \r\n
    */
   public boolean readOptionalLinefeed()
     throws IOException
   {
     int ch = read();
 
     if (ch == '\n') {
       return true;
     }
     else {
       unread();
       return false;
     }
   }
 
  
   /**
    * Reads a line from the buffer.
    */
   public StringValue readLine(long length)
     throws IOException
   {
     return _lineReader.readLine(this, length);
   }
 
   /**
    * Returns true on the EOF.
    */
   public boolean isEOF()
   {
     try {
      return _stream.getFilePointer() == _stream.getLength() - 1;
     } catch (IOException e) {
       return true;
     }
   }
     
   /**
    * Prints a string to a file.
    */
   public void print(char v)
     throws IOException
   {
     _stream.write((byte) v);
   }
 
   /**
    * Prints a string to a file.
    */
   public void print(String v)
     throws IOException
   {
     for (int i = 0; i < v.length(); i++)
       write(v.charAt(i));
   }
 
    /**
    * Writes a buffer to a file.
    */
   public void write(byte []buffer, int offset, int length)
     throws IOException
   {
     _stream.write(buffer, offset, length);
   }
 
   /**
    * Writes a buffer to a file.
    */
   public void write(int ch)
     throws IOException
   {
     _stream.write(ch);
   }
 
   /**
    * Flushes the output.
    */
   public void flush()
     throws IOException
   {
   }
 
   /**
    * Closes the file for writing.
    */
   public void closeWrite()
   {
     close();
   }
   
   /**
    * Closes the file for reading.
    */
   public void closeRead()
   {
     close();
   }
 
   /**
    * Closes the file.
    */
   public void close()
   {
     try {
       _env.removeClose(this);
       
       _stream.close();
 
       if (_temporary)
         _path.remove();
     } catch (IOException e) {
       log.log(Level.FINE, e.toString(), e);
     }
     
     try {
       RandomAccessFile file = _randomAccessFile;
       _randomAccessFile = null;
       
       if (file != null) {
         file.close();
       }
     } catch (IOException e) {
       log.log(Level.FINE, e.toString(), e);
     }
   }
 
   /**
    * Returns the current location in the file.
    */
   public long getPosition()
   {
     try {
       return _stream.getFilePointer();
     } catch (IOException e) {
       log.log(Level.FINE, e.toString(), e);
 
       return -1;
     }
   }
 
   /**
    * Sets the current location in the stream
    */
   public boolean setPosition(long offset)
   {
     _stream.seek(offset);
 
     return true;
   }
 
   public long seek(long offset, int whence)
   {
     switch (whence) {
       case BinaryInput.SEEK_CUR:
         offset = getPosition() + offset;
         break;
       case BinaryInput.SEEK_END:
         try {
           offset = _stream.getLength() + offset;
         } catch (IOException e) {
           log.log(Level.FINE, e.toString(), e);
 
           return getPosition();
         }
         break;
       case SEEK_SET:
       default:
         break;
     }
 
     _stream.seek(offset);
 
     return offset;
   }
 
   /**
    * Opens a copy.
    */
   public BinaryInput openCopy()
     throws IOException
   {
     return new FileInputOutput(_env, _path);
   }
 
   /**
    * Lock the shared advisory lock.
    */
   public boolean lock(boolean shared, boolean block)
   {
     if (! (getPath() instanceof FilePath))
       return false;
 
     try {
       File file = ((FilePath) getPath()).getFile();
 
       if (_randomAccessFile == null) {
         _randomAccessFile = new RandomAccessFile(file, "rw");
       }
       
       FileChannel fileChannel = _randomAccessFile.getChannel();
 
       if (block)
         _fileLock = fileChannel.lock(0, Long.MAX_VALUE, shared);
       else 
         _fileLock = fileChannel.tryLock(0, Long.MAX_VALUE, shared);
 
       return _fileLock != null;
     } catch (IOException e) {
       return false;
     }
   }
 
   /**
    * Unlock the advisory lock.
    */
   public boolean unlock()
   {
     try {
       if (_fileLock != null) {
         _fileLock.release();
 
         return true;
       }
 
       return false;
     } catch (IOException e) {
       return false;
     }
   }
 
   public Value stat()
   {
     return FileModule.statImpl(_env, getPath());
   }
 
   /**
    * Converts to a string.
    * @param env
    */
   public String toString()
   {
     return "FileInputOutput[" + getPath() + "]";
   }
 }
 
