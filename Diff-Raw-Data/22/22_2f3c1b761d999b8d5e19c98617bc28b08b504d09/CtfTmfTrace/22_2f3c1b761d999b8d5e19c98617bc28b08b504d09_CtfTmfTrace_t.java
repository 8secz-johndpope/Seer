 /*******************************************************************************
  * Copyright (c) 2012 Ericsson
  *
  * All rights reserved. This program and the accompanying materials are made
  * available under the terms of the Eclipse Public License v1.0 which
  * accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors: Matthew Khouzam - Initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.linuxtools.tmf.core.ctfadaptor;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
 import org.eclipse.linuxtools.ctf.core.trace.CTFTrace;
 import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
 import org.eclipse.linuxtools.tmf.core.event.ITmfTimestamp;
 import org.eclipse.linuxtools.tmf.core.event.TmfTimestamp;
 import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
 import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
 import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
 import org.eclipse.linuxtools.tmf.core.trace.ITmfEventParser;
 import org.eclipse.linuxtools.tmf.core.trace.ITmfLocation;
 import org.eclipse.linuxtools.tmf.core.trace.TmfTrace;
 
 /**
  * The CTf trace handler
  *
  * @version 1.0
  * @author Matthew khouzam
  */
 public class CtfTmfTrace extends TmfTrace implements ITmfEventParser {
 
 
     //-------------------------------------------
     //        Constants
     //-------------------------------------------
     /**
      * Default cache size for CTF traces
      */
     protected static final int DEFAULT_CACHE_SIZE = 50000;
 
     //-------------------------------------------
     //        Fields
     //-------------------------------------------
 
     /** Reference to the state system assigned to this trace */
     protected ITmfStateSystem ss = null;
 
     /* Reference to the CTF Trace */
     private CTFTrace fTrace;
 
 
 
     //-------------------------------------------
     //        TmfTrace Overrides
     //-------------------------------------------
     /**
      * Method initTrace.
      *
      * @param resource
      *            The resource associated with this trace
      * @param path
      *            The path to the trace file
      * @param eventType
      *            The type of events that will be read from this trace
      * @throws TmfTraceException
      *             If something when wrong while reading the trace
      */
     @Override
     public void initTrace(final IResource resource, final String path, final Class<? extends ITmfEvent> eventType)
             throws TmfTraceException {
         /*
          * Set the cache size. This has to be done before the call to super()
          * because the super needs to know the cache size.
          */
         setCacheSize();
 
         @SuppressWarnings("unused")
         CtfTmfEventType type;
 
         try {
             this.fTrace = new CTFTrace(path);
             CtfIteratorManager.addTrace(this);
             CtfTmfLightweightContext ctx;
             /* Set the start and (current) end times for this trace */
             ctx = (CtfTmfLightweightContext) seekEvent(0L);
             CtfTmfEvent event = getNext(ctx);
             if((ctx.getLocation().equals(CtfIterator.NULL_LOCATION)) || (ctx.getCurrentEvent() == null)) {
                 /* Handle the case where the trace is empty */
                 this.setStartTime(TmfTimestamp.BIG_BANG);
             } else {
                 final ITmfTimestamp curTime = event.getTimestamp();
                 this.setStartTime(curTime);
                 this.setEndTime(curTime);
             }
 
         } catch (final CTFReaderException e) {
             /*
              * If it failed at the init(), we can assume it's because the file
              * was not found or was not recognized as a CTF trace. Throw into
              * the new type of exception expected by the rest of TMF.
              */
             throw new TmfTraceException(e.getMessage(), e);
         }
 
         super.initTrace(resource, path, eventType);
     }
 
     /* (non-Javadoc)
      * @see org.eclipse.linuxtools.tmf.core.trace.TmfTrace#dispose()
      */
     @Override
     public synchronized void dispose() {
         CtfIteratorManager.removeTrace(this);
         fTrace = null;
         super.dispose();
     }
 
     /**
      * Method validate.
      * @param project IProject
      * @param path String
      * @return boolean
      * @see org.eclipse.linuxtools.tmf.core.trace.ITmfTrace#validate(IProject, String)
      */
     @Override
     public boolean validate(final IProject project, final String path) {
         try {
             final CTFTrace temp = new CTFTrace(path);
             return temp.majortIsSet(); // random test
         } catch (final CTFReaderException e) {
             /* Nope, not a CTF trace we can read */
             return false;
         }
     }
 
     /**
      * Method getCurrentLocation. This is not applicable in CTF
      * @return null, since the trace has no knowledge of the current location
      * @see org.eclipse.linuxtools.tmf.core.trace.ITmfTrace#getCurrentLocation()
      */
     @Override
     public ITmfLocation getCurrentLocation() {
         return null;
     }
 
     @Override
     public double getLocationRatio(ITmfLocation location) {
         final CtfLocation curLocation = (CtfLocation) location;
         final CtfTmfLightweightContext context = new CtfTmfLightweightContext(this);
         context.setLocation(curLocation);
         context.seek(curLocation.getLocationInfo());
         final CtfLocationData currentTime = ((CtfLocationData)context.getLocation().getLocationInfo());
         final long startTime = getIterator(this, context).getStartTime();
         final long endTime = getIterator(this, context).getEndTime();
         return ((double) currentTime.getTimestamp() - startTime)
                 / (endTime - startTime);
     }
 
     /**
      * Method seekEvent.
      * @param location ITmfLocation<?>
      * @return ITmfContext
      */
     @Override
     public ITmfContext seekEvent(final ITmfLocation location) {
         CtfLocation currentLocation = (CtfLocation) location;
         CtfTmfLightweightContext context = new CtfTmfLightweightContext(this);
         /*
          * The rank is set to 0 if the iterator seeks the beginning. If not, it
          * will be set to UNKNOWN_RANK, since CTF traces don't support seeking
          * by rank for now.
          */
         if (currentLocation == null) {
             currentLocation = new CtfLocation(new CtfLocationData(0L, 0L));
             context.setRank(0);
         }
         if (currentLocation.getLocationInfo() == CtfLocation.INVALID_LOCATION) {
             currentLocation = new CtfLocation(getEndTime().getValue() + 1, 0L);
         }
         context.setLocation(currentLocation);
         if (location == null) {
             CtfTmfEvent event = getIterator(this, context).getCurrentEvent();
             if (event != null) {
                 currentLocation = new CtfLocation(event.getTimestamp().getValue(), 0);
             }
         }
         if(context.getRank() != 0) {
             context.setRank(ITmfContext.UNKNOWN_RANK);
         }
         return context;
     }
 
 
     @Override
     public ITmfContext seekEvent(double ratio) {
         CtfTmfLightweightContext context = new CtfTmfLightweightContext(this);
         final long end = this.getEndTime().getValue();
         final long start = this.getStartTime().getValue();
         final long diff = end - start;
         final long ratioTs = Math.round(diff * ratio) + start;
         context.seek(ratioTs);
         context.setRank(ITmfContext.UNKNOWN_RANK);
         return context;
     }
 
     /**
      * Method readNextEvent.
      * @param context ITmfContext
      * @return CtfTmfEvent
      * @see org.eclipse.linuxtools.tmf.core.trace.ITmfTrace#getNext(ITmfContext)
      */
     @Override
     public synchronized CtfTmfEvent getNext(final ITmfContext context) {
         if (fTrace == null) {
             return null;
         }
         CtfTmfEvent event = null;
         if (context instanceof CtfTmfLightweightContext) {
            if (context.getLocation() == null || CtfLocation.INVALID_LOCATION.equals(context.getLocation().getLocationInfo())) {
                 return null;
             }
             CtfTmfLightweightContext ctfContext = (CtfTmfLightweightContext) context;
             event = ctfContext.getCurrentEvent();
 
             if (event != null) {
                 updateAttributes(context, event.getTimestamp());
                 ctfContext.advance();
                 ctfContext.increaseRank();
             }
         }
 
         return event;
     }
 
     /**
      * @since 2.0
      */
     @Override
     public ITmfStateSystem getStateSystem() {
         return this.ss;
     }
 
     /**
      * gets the CTFtrace that this is wrapping
      * @return the CTF trace
      */
     public CTFTrace getCTFTrace() {
         return fTrace;
     }
 
 
     //-------------------------------------------
     //        Environment Parameters
     //-------------------------------------------
     /**
      * Method getNbEnvVars.
      *
      * @return int
      */
     public int getNbEnvVars() {
         return this.fTrace.getEnvironment().size();
     }
 
     /**
      * Method getEnvNames.
      *
      * @return String[]
      */
     public String[] getEnvNames() {
         final String[] s = new String[getNbEnvVars()];
         return this.fTrace.getEnvironment().keySet().toArray(s);
     }
 
     /**
      * Method getEnvValue.
      *
      * @param key
      *            String
      * @return String
      */
     public String getEnvValue(final String key) {
         return this.fTrace.getEnvironment().get(key);
     }
 
     //-------------------------------------------
     //        Clocks
     //-------------------------------------------
 
     /**
      * gets the clock offset
      * @return the clock offset in ns
      */
     public long getOffset(){
         if( fTrace != null ) {
             return fTrace.getOffset();
         }
         return 0;
     }
 
     //-------------------------------------------
     //        Parser
     //-------------------------------------------
 
     @Override
     public CtfTmfEvent parseEvent(ITmfContext context) {
         CtfTmfEvent event = null;
         if( context instanceof CtfTmfLightweightContext ){
             CtfTmfLightweightContext itt = (CtfTmfLightweightContext) context.clone();
             event = itt.getCurrentEvent();
         }
         return event;
     }
 
     /**
      * Sets the cache size for a CtfTmfTrace.
      */
     protected void setCacheSize() {
         setCacheSize(DEFAULT_CACHE_SIZE);
     }
 
     //-------------------------------------------
     //          Helpers
     //-------------------------------------------
 
     private static CtfIterator getIterator(CtfTmfTrace trace,  CtfTmfLightweightContext context) {
         return CtfIteratorManager.getIterator(trace, context);
     }
 }
