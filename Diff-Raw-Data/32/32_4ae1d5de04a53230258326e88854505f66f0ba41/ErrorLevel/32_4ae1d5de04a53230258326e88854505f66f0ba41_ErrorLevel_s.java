 /*
  * Copyright (c) 2006-2007 Chris Smith, Shane Mc Cormack, Gregory Holmes
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 
 package com.dmdirc.logger;
 
 /**
  * Specific error levels allowed by Logger.
  */
 public enum ErrorLevel {
     /** Fatal error. */
     FATAL("Fatal"),
     /** High priority error. */
     HIGH("High"),
     /** Medium priority error. */
     MEDIUM("Medium"),
     /** Low priority error. */
     LOW("Low"),
     /** Unknown priority error. */
     UNKNOWN("Unknown");
     
     /** toString value of the item. */
     private String value;
     
     /** 
      * Instantiates the enum. 
      *
      * @param value toString value
      */
     ErrorLevel(final String value) {
         this.value = value;
     }
     
     /** {@inheritDoc} */
     @Override
     public String toString() {
         return value;
     }
     
     /**
      * Returns if the specified error is more important than this one
      *
      * @param level Error level to compare
      *
      * @return true iif the error is more important
      */
     public boolean moreImportant(final ErrorLevel level) {
         switch (level) {
             case FATAL:
                 return true;
             case HIGH:
                 if (this == FATAL) {
                     return true;
                 }
                 return false;
             case MEDIUM:
                 if (this == HIGH || this == FATAL) {
                     return true;
                 }
                 return false;
             case LOW:
                 if (this == MEDIUM || this == HIGH || this == FATAL) {
                     return true;
                 }
                 return false;
             case UNKNOWN:
                 if (this == LOW || this == MEDIUM || this == HIGH || this == FATAL) {
                     return true;
                 }
                 return false;
             default:
                 return false;
         }
     }
 }
