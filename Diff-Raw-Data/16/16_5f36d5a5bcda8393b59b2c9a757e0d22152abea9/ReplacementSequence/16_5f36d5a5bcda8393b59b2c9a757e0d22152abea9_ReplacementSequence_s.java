 /*
  * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
  *
  * This code is free software; you can redistribute it and/or modify it
  * under the terms of the GNU General Public License version 2 only, as
  * published by the Free Software Foundation.  
  *
  * This code is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  * version 2 for more details (a copy is included in the LICENSE file that
  * accompanied this code).
  *
  * You should have received a copy of the GNU General Public License version
  * 2 along with this work; if not, write to the Free Software Foundation,
  * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  *
  * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
  * CA 95054 USA or visit www.sun.com if you need additional information or
  * have any questions.
  */
 
 package com.sun.javafx.runtime.sequence;
 
 /**
  * ReplacementSequence
  *
  * @author Brian Goetz
  */
 class ReplacementSequence<T> extends DerivedSequence<T> implements Sequence<T> {
     private final int newIndex;
     private final T newValue;
 
     public ReplacementSequence(Sequence<T> sequence, int newIndex, T newValue) {
         super(sequence.getElementType(), sequence);
         this.newIndex = newIndex;
         this.newValue = newValue;
     }
 
     public T get(int position) {
         return (position == newIndex) ? newValue : sequence.get(position);
     }
     
     @Override
     public void toArray(int sourceOffset, int length, Object[] dest, int destOffset) {
         if (sourceOffset < 0 || (length > 0 && sourceOffset + length > size))
             throw new ArrayIndexOutOfBoundsException();
 
         sequence.toArray(sourceOffset, length, dest, destOffset);
         final int position = destOffset + newIndex - sourceOffset;
        if (position >= 0 && position < size) {
             dest[position] = newValue;
         }
     }
 }
