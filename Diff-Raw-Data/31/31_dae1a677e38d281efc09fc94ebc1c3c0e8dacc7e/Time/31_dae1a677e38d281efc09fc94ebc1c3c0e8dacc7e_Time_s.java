package org.ardverk.concurrent;
 
 import java.io.Serializable;
 import java.util.concurrent.TimeUnit;
 
 /**
  * {@link Time} is an immutable class that holds a time (a long value)
  * and the {@link TimeUnit} of the {@link Time}.
  */
class Time implements Serializable, Comparable<Time> {
     
     private static final long serialVersionUID = 8113035746133232743L;
 
    static final Time NONE = new Time(-1L, TimeUnit.MILLISECONDS);
     
     private final long time;
     
     private final TimeUnit unit;
     
     public Time(long time, TimeUnit unit) {
         if (time < 0L && time != -1L) {
             throw new IllegalArgumentException("time=" + time);
         }
         
         if (unit == null) {
             throw new NullPointerException("unit");
         }
         
         this.time = time;
         this.unit = unit;
     }
     
     /**
      * Returns the time.
      */
     public long getTime() {
         return time;
     }
     
     /**
      * Returns the {@link TimeUnit}.
      */
     public TimeUnit getUnit() {
         return unit;
     }
     
     /**
      * Converts the time to the given {@link TimeUnit} and returns it.
      */
     public long getTime(TimeUnit unit) {
         return unit.convert(time, this.unit);
     }
     
     @Override
     public int hashCode() {
         return (int)unit.toMillis(time);
     }
     
     @Override
     public boolean equals(Object o) {
         if (o == this) {
             return true;
         } else if (!(o instanceof Time)) {
             return false;
         }
         
         return compareTo((Time)o) == 0;
     }
 
     @Override
     public int compareTo(Time other) {
         if (other == null) {
             throw new NullPointerException("other");
         }
         
         long diff = time - unit.convert(other.time, other.unit);
         
         if (diff < 0L) {
             return -1;
         } else if (diff > 0L) {
             return 1;
         }
         
         return 0;
     }
     
     @Override
     public String toString() {
         return time + " " + unit;
     }
 }
