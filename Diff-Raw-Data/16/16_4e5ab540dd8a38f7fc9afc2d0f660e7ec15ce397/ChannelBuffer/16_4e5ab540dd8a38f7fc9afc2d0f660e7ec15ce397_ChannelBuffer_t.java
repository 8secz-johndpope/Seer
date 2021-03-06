 /*
  * JBoss, Home of Professional Open Source.
  * Copyright 2009, Red Hat, Inc. and/or its affiliates, and
  * individual contributors as indicated by the @author tags. See the
  * copyright.txt file in the distribution for a full listing of
  * individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 package org.infinispan.server.core.transport;
 
 /**
  * ChannelBuffer.
  * 
  * @author Galder Zamarreño
  * @since 4.1
  */
 public interface ChannelBuffer {
    byte readByte();
    void readBytes(byte[] dst, int dstIndex, int length);
    short readUnsignedByte();
    int readUnsignedInt();
    long readUnsignedLong();
    ChannelBuffer readBytes(int length);
    int readerIndex();
    void readBytes(byte[] dst);
    byte[] readRangedBytes();
 
    /**
     * Reads length of String and then returns an UTF-8 formatted String of such length.
     */
    String readString();
 
    void writeByte(byte value);
    void writeBytes(byte[] src);
 
    /**
     * Writes the length of the byte array and transfers the specified source array's data to this buffer
     */
    void writeRangedBytes(byte[] src);
    void writeUnsignedInt(int i);
    void writeUnsignedLong(long l);
    int writerIndex();
 
   /**
    * Writes the length of the String followed by the String itself. This methods expects String not to be null. 
    */
   void writeString(String msg);

    Object getUnderlyingChannelBuffer();
 
 }
