 /*
  * Copyright to the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.rioproject.watch;
 
 import java.io.Serializable;
 import java.text.DateFormat;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 
 /**
  * A Calculable is the atomic attribute that is being 'watched'
  */
 public class Calculable implements Serializable {
     static final long serialVersionUID = 1L;
     /** The identifier for the Calculable */
     private String id;
     /** The value for the Calculable */
     private double value;
     /**
      * Holds value of property when, indicates when the Calculable was
      * recorded
      */
     private long when;
     /** Holds Optional detail about the metric*/
     private String detail;
 
     /**
      * Create a new Calculable
      */
     public Calculable() {
     }
 
     /**
      * Create a new Calculable that records the time
      * 
      * @param id The identifier for the Calculable
      * @param value The value for the Calculable
      */
     public Calculable(String id, double value) {
         this.id = id;
         this.value = value;
         this.when = System.currentTimeMillis();
     }
 
     /**
      * Create a new Calculable
      *                                                                                  
      * @param id The identifier for this Calculable record
      * @param value The value for the Calculable Record
      * @param when The time when the recorded value was captured
      */
     public Calculable(String id, double value, long when) {
         this(id, value);
        this.when = when;
     }
 
     /**
      * Getter for property id
      * 
      * @return Value of property id.
      */
     public String getId() {
         return (id);
     }
 
     /**
      * Setter for property id
      * 
      * @param id New value of property id.
      */
     public void setId(String id) {
         this.id = id;
     }
 
     /**
      * Getter for property value.
      * 
      * @return Value of property value.
      */
     public double getValue() {
         return (value);
     }
 
     /**
      * Setter for property value.
      * 
      * @param value New value of property value.
      */
     public void setValue(double value) {
         this.value = value;
     }
 
     /**
      * Getter for property when.
      * 
      * @return Value of property when.
      */
     public long getWhen() {
         return (when);
     }
 
     /**
      * Setter for property when.
      * 
      * @param when New value of property when.
      */
     public void setWhen(long when) {
         this.when = when;
     }
 
     /**
      * Get the Date that represents the time of the Calculable
      *
      * @return The Date  that represents the time of the Calculable
      */
     public Date getDate(){
         return(new Date(getWhen()));
     }
 
     public String getDetail() {
         return detail;
     }
 
     public void setDetail(String detail) {
         this.detail = detail;
     }
 
     /**
      * Indicates whether some other object is "equal to" this one.
      * 
      * @param obj the object to compare to this one
      * @return true if the objects are equal
      */
     public boolean equals(Object obj) {
         if(obj instanceof Calculable) {
             if(getId() == null && ((Calculable)obj).getId() == null)
                 return (true);
             else if(getId() == null || ((Calculable)obj).getId() == null)
                 return (false);
             else
                 return (getId().equals(((Calculable)obj).getId()));
         }
         return (false);
     }
 
     /**
      * Returns a hash code value for the object.
      * 
      * @return a hash code value for the object.
      */
     public int hashCode() {
         return (id != null) ? id.hashCode() : 0;
     }
 
     /**
      * Gets an archival representation for this Calculable
      * 
      * @return a string representation in archive format
      */
     public String getArchiveRecord() {
         return (getId() + '|' + getValue() + '|' + when);
     }
 
     /**
      * Returns a string representation of the object.
      * 
      * @return String representation of the object.
      */
     public String toString() {
         String format = "yyyy.MM.dd HH:mm:ss,SSS";
         DateFormat formatter = new SimpleDateFormat(format);
         Date date = new Date(when);
         StringBuilder builder = new StringBuilder();
         builder.append(id).append(" = ").append(getValue()).append(", ").append(formatter.format(date));
         return builder.toString();
     }
 }
