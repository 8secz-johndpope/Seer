 /* 
  * Copyright (c) 2008-2010, Hazel Ltd. All Rights Reserved.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at 
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */
 
 package com.hazelcast.nio;
 
 import java.io.*;
 import java.util.Date;
 
 public class SerializationHelper {
     public static void writeObject(DataOutput out, Object obj) throws IOException {
         if (obj == null) {
             out.writeByte(0);
         } else if (obj instanceof Long) {
             out.writeByte(1);
             out.writeLong((Long) obj);
         } else if (obj instanceof Integer) {
             out.writeByte(2);
             out.writeInt((Integer) obj);
         } else if (obj instanceof String) {
             out.writeByte(3);
             out.writeUTF((String) obj);
         } else if (obj instanceof Double) {
             out.writeByte(4);
             out.writeDouble((Double) obj);
         } else if (obj instanceof Float) {
             out.writeByte(5);
            out.writeDouble((Float) obj);
         } else if (obj instanceof Boolean) {
             out.writeByte(6);
             out.writeBoolean((Boolean) obj);
         } else if (obj instanceof DataSerializable) {
             out.writeByte(7);
             out.writeUTF(obj.getClass().getName());
             ((DataSerializable) obj).writeData(out);
         } else if (obj instanceof Date) {
             out.writeByte(8);
             out.writeLong(((Date) obj).getTime());
         } else {
             out.writeByte(9);
             ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos);
             oos.writeObject(obj);
             oos.close();
             byte[] buf = bos.toByteArray();
             out.writeInt(buf.length);
             out.write(buf);
         }
     }
 
     public static Object readObject(DataInput in) throws IOException {
         byte type = in.readByte();
         if (type == 0) {
             return null;
         } else if (type == 1) {
            return in.readInt();
         } else if (type == 2) {
             return in.readInt();
         } else if (type == 3) {
             return in.readUTF();
         } else if (type == 4) {
             return in.readDouble();
         } else if (type == 5) {
             return in.readFloat();
         } else if (type == 6) {
             return in.readBoolean();
         } else if (type == 7) {
             DataSerializable ds;
             try {
                 ds = (DataSerializable) Serializer.classForName(in.readUTF()).newInstance();
             } catch (Throwable e) {
                 throw new IOException(e.getMessage());
             }
             ds.readData(in);
             return ds;
         } else if (type == 8) {
             return new Date(in.readLong());
         } else if (type == 9) {
             int len = in.readInt();
             byte[] buf = new byte[len];
             in.readFully(buf);
             ObjectInputStream oin = Serializer.newObjectInputStream(new ByteArrayInputStream(buf));
             try {
                 return oin.readObject();
             } catch (ClassNotFoundException e) {
                 e.printStackTrace();
             }
             oin.close();
         } else {
             throw new IOException("Unknown object type=" + type);
         }
         return null;
     }
 }
