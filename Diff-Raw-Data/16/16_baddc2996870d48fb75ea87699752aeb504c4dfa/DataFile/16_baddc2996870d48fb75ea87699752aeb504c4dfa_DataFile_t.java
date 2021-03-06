 /**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.fusesource.hawtjournal.api;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.RandomAccessFile;
 import org.fusesource.hawtjournal.util.IOHelper;
 
 /**
  * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
  */
 class DataFile implements Comparable<DataFile> {
 
     private final File file;
     private final Integer dataFileId;
     private volatile DataFile next;
     private volatile int length;
 
    DataFile(File file, int number) {
         this.file = file;
         this.dataFileId = Integer.valueOf(number);
         length = (int) (file.exists() ? file.length() : 0);
     }
 
     File getFile() {
         return file;
     }
 
     Integer getDataFileId() {
         return dataFileId;
     }
 
     synchronized DataFile getNext() {
         return next;
     }
 
     synchronized void setNext(DataFile next) {
         this.next = next;
     }
 
     synchronized int getLength() {
         return length;
     }
 
     synchronized void setLength(int length) {
         this.length = length;
     }
 
     synchronized void incrementLength(int size) {
         length += size;
     }
 
     public String toString() {
         return file.getName() + " number = " + dataFileId + " , length = " + length;
     }
 
     synchronized RandomAccessFile openRandomAccessFile() throws IOException {
         return new RandomAccessFile(file, "rw");
     }
 
     synchronized void closeRandomAccessFile(RandomAccessFile file) throws IOException {
         file.close();
     }
 
     synchronized boolean delete() throws IOException {
         return file.delete();
     }
 
     synchronized void move(File targetDirectory) throws IOException {
         IOHelper.moveFile(file, targetDirectory);
     }
 
     public int compareTo(DataFile df) {
         return dataFileId - df.dataFileId;
     }
 
     @Override
     public boolean equals(Object o) {
         boolean result = false;
         if (o instanceof DataFile) {
             result = compareTo((DataFile) o) == 0;
         }
         return result;
     }
 
     @Override
     public int hashCode() {
         return dataFileId;
     }
 
 }
