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
 package org.apache.kahadb.util;
 
 import java.io.DataInput;
 import java.io.DataOutput;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.NoSuchElementException;
 
 /**
  * Keeps track of a added long values. Collapses ranges of numbers using a
  * Sequence representation. Use to keep track of received message ids to find
  * out if a message is duplicate or if there are any missing messages.
  * 
  * @author chirino
  */
 public class SequenceSet extends LinkedNodeList<Sequence> {
 
     public static class Marshaller implements org.apache.kahadb.util.Marshaller<SequenceSet> {
 
         public static final Marshaller INSTANCE = new Marshaller();
         
         public SequenceSet readPayload(DataInput in) throws IOException {
             SequenceSet value = new SequenceSet();
             int count = in.readInt();
             for (int i = 0; i < count; i++) {
                 if( in.readBoolean() ) {
                     Sequence sequence = new Sequence(in.readLong(), in.readLong());
                     value.addLast(sequence);
                 } else {
                     Sequence sequence = new Sequence(in.readLong());
                     value.addLast(sequence);
                 }
             }
             return value;
         }
 
         public void writePayload(SequenceSet value, DataOutput out) throws IOException {
             out.writeInt(value.size());
             Sequence sequence = value.getHead();
             while (sequence != null ) {
                 if( sequence.range() > 1 ) {
                     out.writeBoolean(true);
                     out.writeLong(sequence.first);
                     out.writeLong(sequence.last);
                 } else {
                     out.writeBoolean(false);
                     out.writeLong(sequence.first);
                 }
                 sequence = sequence.getNext();
             }
         }
 
         public int getFixedSize() {
             return -1;
         }
 
         public SequenceSet deepCopy(SequenceSet value) {
             SequenceSet rc = new SequenceSet();
             Sequence sequence = value.getHead();
             while (sequence != null ) {
                 rc.add(new Sequence(sequence.first, sequence.last));
                 sequence = sequence.getNext();
             }
             return rc;
         }
 
         public boolean isDeepCopySupported() {
             return true;
         }
     }
     
     public void add(Sequence value) {
         // TODO we can probably optimize this a bit
         for(long i=value.first; i<value.last+1; i++) {
             add(i);
         }
     }
     
     
     /**
      * 
      * @param value
      *            the value to add to the list
      * @return false if the value was a duplicate.
      */
     public boolean add(long value) {
 
         if (isEmpty()) {
             addFirst(new Sequence(value));
             return true;
         }
 
         Sequence sequence = getHead();
         while (sequence != null) {
 
             if (sequence.isAdjacentToLast(value)) {
                 // grow the sequence...
                 sequence.last = value;
                 // it might connect us to the next sequence..
                 if (sequence.getNext() != null) {
                     Sequence next = sequence.getNext();
                     if (next.isAdjacentToFirst(value)) {
                         // Yep the sequence connected.. so join them.
                         sequence.last = next.last;
                         next.unlink();
                     }
                 }
                 return true;
             }
 
             if (sequence.isAdjacentToFirst(value)) {
                 // grow the sequence...
                 sequence.first = value;
 
                 // it might connect us to the previous
                 if (sequence.getPrevious() != null) {
                     Sequence prev = sequence.getPrevious();
                     if (prev.isAdjacentToLast(value)) {
                         // Yep the sequence connected.. so join them.
                         sequence.first = prev.first;
                         prev.unlink();
                     }
                 }
                 return true;
             }
 
             // Did that value land before this sequence?
             if (value < sequence.first) {
                 // Then insert a new entry before this sequence item.
                 sequence.linkBefore(new Sequence(value));
                 return true;
             }
 
             // Did that value land within the sequence? The it's a duplicate.
             if (sequence.contains(value)) {
                 return false;
             }
 
             sequence = sequence.getNext();
         }
 
         // Then the value is getting appended to the tail of the sequence.
         addLast(new Sequence(value));
         return true;
     }
     
     /**
      * Removes and returns the first element from this list.
      *
      * @return the first element from this list.
      * @throws NoSuchElementException if this list is empty.
      */
     public long removeFirst() {
         if (isEmpty()) {
             throw new NoSuchElementException();
         }
         
         Sequence rc = removeFirstSequence(1);
         return rc.first;
     }
 
 
     public Sequence removeLastSequence() {
         if (isEmpty()) {
             return null;
         }
 
         Sequence rc = getTail();
         rc.unlink();
         return rc;
     }
 
     /**
      * Removes and returns the first sequence that is count range large.
      *
      * @return a sequence that is count range large, or null if no sequence is that large in the list.
      */
     public Sequence removeFirstSequence(long count) {
         if (isEmpty()) {
             return null;
         }
         
         Sequence sequence = getHead();
         while (sequence != null ) {
             if (sequence.range() == count ) {
                 sequence.unlink();
                 return sequence;
             }
             if (sequence.range() > count ) {
                 Sequence rc = new Sequence(sequence.first, sequence.first+count);
                 sequence.first+=count;
                 return rc;
             }
             sequence = sequence.getNext();
         }
         return null;
     }
 
 
     /**
      * @return all the id Sequences that are missing from this set that are not
      *         in between the range provided.
      */
     public List<Sequence> getMissing(long first, long last) {
         ArrayList<Sequence> rc = new ArrayList<Sequence>();
         if (first > last) {
             throw new IllegalArgumentException("First cannot be more than last");
         }
         if (isEmpty()) {
             // We are missing all the messages.
             rc.add(new Sequence(first, last));
             return rc;
         }
 
         Sequence sequence = getHead();
         while (sequence != null && first <= last) {
             if (sequence.contains(first)) {
                 first = sequence.last + 1;
             } else {
                 if (first < sequence.first) {
                     if (last < sequence.first) {
                         rc.add(new Sequence(first, last));
                         return rc;
                     } else {
                         rc.add(new Sequence(first, sequence.first - 1));
                         first = sequence.last + 1;
                     }
                 }
             }
             sequence = sequence.getNext();
         }
 
         if (first <= last) {
             rc.add(new Sequence(first, last));
         }
         return rc;
     }
 
     /**
      * @return all the Sequence that are in this list
      */
     public List<Sequence> getReceived() {
         ArrayList<Sequence> rc = new ArrayList<Sequence>(size());
         Sequence sequence = getHead();
         while (sequence != null) {
             rc.add(new Sequence(sequence.first, sequence.last));
             sequence = sequence.getNext();
         }
         return rc;
     }
 
     public boolean contains(int first, int last) {
         if (isEmpty()) {
             return false;
         }
         Sequence sequence = getHead();
         while (sequence != null) {
            if (sequence.first <= first && first <= sequence.last ) {
                 return last <= sequence.last ;
             }
             sequence = sequence.getNext();
         }
         return false;
     }
 
 }
