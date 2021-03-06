 /* ----------------------------------------------------------------------------
  * This file was automatically generated by SWIG (http://www.swig.org).
  * Version 1.3.40
  *
  * Do not make changes to this file unless you know what you are doing--modify
  * the SWIG interface file instead.
  * ----------------------------------------------------------------------------- */
 
 package libtisch;
 
 public class Matcher {
   private long swigCPtr;
   protected boolean swigCMemOwn;
 
   protected Matcher(long cPtr, boolean cMemoryOwn) {
     swigCMemOwn = cMemoryOwn;
     swigCPtr = cPtr;
   }
 
   protected static long getCPtr(Matcher obj) {
     return (obj == null) ? 0 : obj.swigCPtr;
   }
 
   protected void finalize() {
     delete();
   }
 
   public synchronized void delete() {
     if (swigCPtr != 0) {
       if (swigCMemOwn) {
         swigCMemOwn = false;
         libtischJNI.delete_Matcher(swigCPtr);
       }
       swigCPtr = 0;
     }
   }
 
  public void request_update(java.math.BigInteger id) {
     libtischJNI.Matcher_request_update(swigCPtr, this, id);
   }
 
  public void trigger_gesture(java.math.BigInteger id, Gesture g) {
     libtischJNI.Matcher_trigger_gesture(swigCPtr, this, id, Gesture.getCPtr(g), g);
   }
 
   public void process_blob(BasicBlob blob) {
     libtischJNI.Matcher_process_blob(swigCPtr, this, BasicBlob.getCPtr(blob), blob);
   }
 
   public void process_gestures() {
     libtischJNI.Matcher_process_gestures(swigCPtr, this);
   }
 
   public void load_defaults(long set) {
     libtischJNI.Matcher_load_defaults__SWIG_0(swigCPtr, this, set);
   }
 
   public void load_defaults() {
     libtischJNI.Matcher_load_defaults__SWIG_1(swigCPtr, this);
   }
 
   public SWIGTYPE_p_void run() {
     long cPtr = libtischJNI.Matcher_run(swigCPtr, this);
     return (cPtr == 0) ? null : new SWIGTYPE_p_void(cPtr, false);
   }
 
  public void update(java.math.BigInteger id, Region r) {
     libtischJNI.Matcher_update(swigCPtr, this, id, Region.getCPtr(r), r);
   }
 
  public void remove(java.math.BigInteger id) {
     libtischJNI.Matcher_remove(swigCPtr, this, id);
   }
 
  public void raise(java.math.BigInteger id) {
     libtischJNI.Matcher_raise(swigCPtr, this, id);
   }
 
  public void lower(java.math.BigInteger id) {
     libtischJNI.Matcher_lower(swigCPtr, this, id);
   }
 
   public void peakmode(boolean _use_peak) {
     libtischJNI.Matcher_peakmode(swigCPtr, this, _use_peak);
   }
 
   public void clear() {
     libtischJNI.Matcher_clear(swigCPtr, this);
   }
 
 }
