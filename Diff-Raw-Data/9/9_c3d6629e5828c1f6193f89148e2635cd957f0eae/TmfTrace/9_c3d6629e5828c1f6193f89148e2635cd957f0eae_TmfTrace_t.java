 /*******************************************************************************
  * Copyright (c) 2009, 2013 Ericsson
  *
  * All rights reserved. This program and the accompanying materials are
  * made available under the terms of the Eclipse Public License v1.0 which
  * accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *   Francois Chouinard - Initial API and implementation
  *   Francois Chouinard - Updated as per TMF Trace Model 1.0
  *   Patrick Tasse - Updated for removal of context clone
  *******************************************************************************/
 
 package org.eclipse.linuxtools.tmf.core.trace;
 
 import java.io.File;
 import java.util.Collections;
 import java.util.LinkedHashMap;
 import java.util.Map;
 
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.linuxtools.tmf.core.component.TmfEventProvider;
 import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
 import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
 import org.eclipse.linuxtools.tmf.core.request.ITmfDataRequest;
 import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
 import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
 import org.eclipse.linuxtools.tmf.core.signal.TmfTraceOpenedSignal;
 import org.eclipse.linuxtools.tmf.core.signal.TmfTraceRangeUpdatedSignal;
 import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
 import org.eclipse.linuxtools.tmf.core.statistics.ITmfStatistics;
 import org.eclipse.linuxtools.tmf.core.statistics.TmfStateStatistics;
 import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
 import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
 import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
 
 /**
  * Abstract implementation of ITmfTrace.
  * <p>
  * Since the concept of 'location' is trace specific, the concrete classes have
  * to provide the related methods, namely:
  * <ul>
  * <li> public ITmfLocation<?> getCurrentLocation()
  * <li> public double getLocationRatio(ITmfLocation<?> location)
  * <li> public ITmfContext seekEvent(ITmfLocation<?> location)
  * <li> public ITmfContext seekEvent(double ratio)
 * <li> public IStatus validate(IProject project, String path)
  * </ul>
  * A concrete trace must provide its corresponding parser. A common way to
  * accomplish this is by making the concrete class extend TmfTrace and
  * implement ITmfEventParser.
  * <p>
  * The concrete class can either specify its own indexer or use the provided
  * TmfCheckpointIndexer (default). In this case, the trace cache size will be
  * used as checkpoint interval.
  *
  * @version 1.0
  * @author Francois Chouinard
  *
  * @see ITmfEvent
  * @see ITmfTraceIndexer
  * @see ITmfEventParser
  */
 public abstract class TmfTrace extends TmfEventProvider implements ITmfTrace {
 
     // ------------------------------------------------------------------------
     // Attributes
     // ------------------------------------------------------------------------
 
     // The resource used for persistent properties for this trace
     private IResource fResource;
 
     // The trace path
     private String fPath;
 
     // The trace cache page size
     private int fCacheSize = ITmfTrace.DEFAULT_TRACE_CACHE_SIZE;
 
     // The number of events collected (so far)
     private long fNbEvents = 0;
 
     // The time span of the event stream
     private ITmfTimestamp fStartTime = TmfTimestamp.BIG_BANG;
     private ITmfTimestamp fEndTime = TmfTimestamp.BIG_BANG;
 
     // The trace streaming interval (0 = no streaming)
     private long fStreamingInterval = 0;
 
     // The trace indexer
     private ITmfTraceIndexer fIndexer;
 
     // The trace parser
     private ITmfEventParser fParser;
 
     // The trace's statistics
     private ITmfStatistics fStatistics;
 
     /**
      * The collection of state systems that are registered with this trace. Each
      * sub-class can decide to add its (one or many) state system to this map
      * during their {@link #buildStateSystem()}.
      *
      * @since 2.0
      */
     protected final Map<String, ITmfStateSystem> fStateSystems =
             new LinkedHashMap<String, ITmfStateSystem>();
 
     // ------------------------------------------------------------------------
     // Construction
     // ------------------------------------------------------------------------
 
     /**
      * The default, parameterless, constructor
      */
     public TmfTrace() {
         super();
     }
 
     /**
      * Full constructor.
      *
      * @param resource
      *            The resource associated to the trace
      * @param type
      *            The type of events that will be read from this trace
      * @param path
      *            The path to the trace on the filesystem
      * @param cacheSize
      *            The trace cache size. Pass '-1' to use the default specified
      *            in {@link ITmfTrace#DEFAULT_TRACE_CACHE_SIZE}
      * @param interval
      *            The trace streaming interval. You can use '0' for post-mortem
      *            traces.
      * @param indexer
      *            The trace indexer. You can pass 'null' to use a default
      *            checkpoint indexer.
      * @param parser
      *            The trace event parser. Use 'null' if (and only if) the trace
      *            object itself is also the ITmfEventParser to be used.
      * @throws TmfTraceException
      *             If something failed during the opening
      */
     protected TmfTrace(final IResource resource,
             final Class<? extends ITmfEvent> type,
             final String path,
             final int cacheSize,
             final long interval,
             final ITmfTraceIndexer indexer,
             final ITmfEventParser parser)
                     throws TmfTraceException {
         super();
         fCacheSize = (cacheSize > 0) ? cacheSize : ITmfTrace.DEFAULT_TRACE_CACHE_SIZE;
         fStreamingInterval = interval;
         fIndexer = (indexer != null) ? indexer : new TmfCheckpointIndexer(this, fCacheSize);
         fParser = parser;
         initialize(resource, path, type);
     }
 
     /**
      * Copy constructor
      *
      * @param trace the original trace
      * @throws TmfTraceException Should not happen usually
      */
     public TmfTrace(final TmfTrace trace) throws TmfTraceException {
         super();
         if (trace == null) {
             throw new IllegalArgumentException();
         }
         fCacheSize = trace.getCacheSize();
         fStreamingInterval = trace.getStreamingInterval();
         fIndexer = new TmfCheckpointIndexer(this);
         fParser = trace.fParser;
         initialize(trace.getResource(), trace.getPath(), trace.getEventType());
     }
 
     // ------------------------------------------------------------------------
     // ITmfTrace - Initializers
     // ------------------------------------------------------------------------
 
     @Override
     public void initTrace(final IResource resource, final String path, final Class<? extends ITmfEvent> type) throws TmfTraceException {
         fIndexer = new TmfCheckpointIndexer(this, fCacheSize);
         initialize(resource, path, type);
     }
 
     /**
      * Initialize the trace common attributes and the base component.
      *
      * @param resource the Eclipse resource (trace)
      * @param path the trace path
      * @param type the trace event type
      *
      * @throws TmfTraceException If something failed during the initialization
      */
     protected void initialize(final IResource resource,
             final String path,
             final Class<? extends ITmfEvent> type)
                     throws TmfTraceException {
         if (path == null) {
             throw new TmfTraceException("Invalid trace path"); //$NON-NLS-1$
         }
         fPath = path;
         fResource = resource;
         String traceName = (resource != null) ? resource.getName() : null;
         // If no resource was provided, extract the display name the trace path
         if (traceName == null) {
             final int sep = path.lastIndexOf(IPath.SEPARATOR);
             traceName = (sep >= 0) ? path.substring(sep + 1) : path;
         }
         if (fParser == null) {
             if (this instanceof ITmfEventParser) {
                 fParser = (ITmfEventParser) this;
             } else {
                 throw new TmfTraceException("Invalid trace parser"); //$NON-NLS-1$
             }
         }
         super.init(traceName, type);
     }
 
     /**
      * Indicates if the path points to an existing file/directory
      *
      * @param path the path to test
      * @return true if the file/directory exists
      */
     protected boolean fileExists(final String path) {
         final File file = new File(path);
         return file.exists();
     }
 
     /**
      * @since 2.0
      */
     @Override
     public void indexTrace(boolean waitForCompletion) {
         getIndexer().buildIndex(0, TmfTimeRange.ETERNITY, waitForCompletion);
     }
 
     /**
      * The default implementation of TmfTrace uses a TmfStatistics back-end.
      * Override this if you want to specify another type (or none at all).
      *
      * @throws TmfTraceException
      *             If there was a problem setting up the statistics
      * @since 2.0
      */
     protected void buildStatistics() throws TmfTraceException {
         /*
          * Initialize the statistics provider, but only if a Resource has been
          * set (so we don't build it for experiments, for unit tests, etc.)
          */
         fStatistics = (fResource == null ? null : new TmfStateStatistics(this) );
     }
 
     /**
      * Build the state system(s) associated with this trace type.
      *
      * Suppressing the warning, because the 'throws' will usually happen in
      * sub-classes.
      *
      * @throws TmfTraceException
      *             If there is a problem during the build
      * @since 2.0
      */
     @SuppressWarnings("unused")
     protected void buildStateSystem() throws TmfTraceException {
         /*
          * Nothing is done in the base implementation, please specify
          * how/if to register a new state system in derived classes.
          */
         return;
     }
 
     /**
      * Clears the trace
      */
     @Override
     public synchronized void dispose() {
         /* Clean up the index if applicable */
         if (getIndexer() != null) {
             getIndexer().dispose();
         }
 
         /* Clean up the statistics */
         if (fStatistics != null) {
             fStatistics.dispose();
         }
 
         /* Clean up the state systems */
         for (ITmfStateSystem ss : fStateSystems.values()) {
             ss.dispose();
         }
 
         super.dispose();
     }
 
     // ------------------------------------------------------------------------
     // ITmfTrace - Basic getters
     // ------------------------------------------------------------------------
 
     @Override
     public Class<ITmfEvent> getEventType() {
         return (Class<ITmfEvent>) super.getType();
     }
 
     @Override
     public IResource getResource() {
         return fResource;
     }
 
     @Override
     public String getPath() {
         return fPath;
     }
 
     @Override
     public int getCacheSize() {
         return fCacheSize;
     }
 
     @Override
     public long getStreamingInterval() {
         return fStreamingInterval;
     }
 
     /**
      * @return the trace indexer
      */
     protected ITmfTraceIndexer getIndexer() {
         return fIndexer;
     }
 
     /**
      * @return the trace parser
      */
     protected ITmfEventParser getParser() {
         return fParser;
     }
 
     /**
      * @since 2.0
      */
     @Override
     public ITmfStatistics getStatistics() {
         return fStatistics;
     }
 
     /**
      * @since 2.0
      */
     @Override
     public final Map<String, ITmfStateSystem> getStateSystems() {
         return Collections.unmodifiableMap(fStateSystems);
     }
 
     /**
      * @since 2.0
      */
     @Override
     public final void registerStateSystem(String id, ITmfStateSystem ss) {
         fStateSystems.put(id, ss);
     }
 
     // ------------------------------------------------------------------------
     // ITmfTrace - Trace characteristics getters
     // ------------------------------------------------------------------------
 
     @Override
     public synchronized long getNbEvents() {
         return fNbEvents;
     }
 
     /**
      * @since 2.0
      */
     @Override
     public TmfTimeRange getTimeRange() {
         return new TmfTimeRange(fStartTime, fEndTime);
     }
 
     /**
      * @since 2.0
      */
     @Override
     public ITmfTimestamp getStartTime() {
         return fStartTime;
     }
 
     /**
      * @since 2.0
      */
     @Override
     public ITmfTimestamp getEndTime() {
         return fEndTime;
     }
 
     /**
      * @since 2.0
      */
     @Override
     public ITmfTimestamp getInitialRangeOffset() {
         final long DEFAULT_INITIAL_OFFSET_VALUE = (1L * 100 * 1000 * 1000); // .1sec
         return new TmfTimestamp(DEFAULT_INITIAL_OFFSET_VALUE, ITmfTimestamp.NANOSECOND_SCALE);
     }
 
     // ------------------------------------------------------------------------
     // Convenience setters
     // ------------------------------------------------------------------------
 
     /**
      * Set the trace cache size. Must be done at initialization time.
      *
      * @param cacheSize The trace cache size
      */
     protected void setCacheSize(final int cacheSize) {
         fCacheSize = cacheSize;
     }
 
     /**
      * Set the trace known number of events. This can be quite dynamic
      * during indexing or for live traces.
      *
      * @param nbEvents The number of events
      */
     protected synchronized void setNbEvents(final long nbEvents) {
         fNbEvents = (nbEvents > 0) ? nbEvents : 0;
     }
 
     /**
      * Update the trace events time range
      *
      * @param range the new time range
      * @since 2.0
      */
     protected void setTimeRange(final TmfTimeRange range) {
         fStartTime = range.getStartTime();
         fEndTime = range.getEndTime();
     }
 
     /**
      * Update the trace chronologically first event timestamp
      *
      * @param startTime the new first event timestamp
      * @since 2.0
      */
     protected void setStartTime(final ITmfTimestamp startTime) {
         fStartTime = startTime;
     }
 
     /**
      * Update the trace chronologically last event timestamp
      *
      * @param endTime the new last event timestamp
      * @since 2.0
      */
     protected void setEndTime(final ITmfTimestamp endTime) {
         fEndTime = endTime;
     }
 
     /**
      * Set the polling interval for live traces (default = 0 = no streaming).
      *
      * @param interval the new trace streaming interval
      */
     protected void setStreamingInterval(final long interval) {
         fStreamingInterval = (interval > 0) ? interval : 0;
     }
 
     /**
      * Set the trace indexer. Must be done at initialization time.
      *
      * @param indexer the trace indexer
      */
     protected void setIndexer(final ITmfTraceIndexer indexer) {
         fIndexer = indexer;
     }
 
     /**
      * Set the trace parser. Must be done at initialization time.
      *
      * @param parser the new trace parser
      */
     protected void setParser(final ITmfEventParser parser) {
         fParser = parser;
     }
 
     // ------------------------------------------------------------------------
     // ITmfTrace - SeekEvent operations (returning a trace context)
     // ------------------------------------------------------------------------
 
     @Override
     public synchronized ITmfContext seekEvent(final long rank) {
 
         // A rank <= 0 indicates to seek the first event
         if (rank <= 0) {
             ITmfContext context = seekEvent((ITmfLocation) null);
             context.setRank(0);
             return context;
         }
 
         // Position the trace at the checkpoint
         final ITmfContext context = fIndexer.seekIndex(rank);
 
         // And locate the requested event context
         long pos = context.getRank();
         if (pos < rank) {
             ITmfEvent event = getNext(context);
             while ((event != null) && (++pos < rank)) {
                 event = getNext(context);
             }
         }
         return context;
     }
 
     /**
      * @since 2.0
      */
     @Override
     public synchronized ITmfContext seekEvent(final ITmfTimestamp timestamp) {
 
         // A null timestamp indicates to seek the first event
         if (timestamp == null) {
             ITmfContext context = seekEvent((ITmfLocation) null);
             context.setRank(0);
             return context;
         }
 
         // Position the trace at the checkpoint
         ITmfContext context = fIndexer.seekIndex(timestamp);
 
         // And locate the requested event context
         ITmfLocation previousLocation = context.getLocation();
         long previousRank = context.getRank();
         ITmfEvent event = getNext(context);
         while (event != null && event.getTimestamp().compareTo(timestamp, false) < 0) {
             previousLocation = context.getLocation();
             previousRank = context.getRank();
             event = getNext(context);
         }
         if (event == null) {
             context.setLocation(null);
             context.setRank(ITmfContext.UNKNOWN_RANK);
         } else {
             context.dispose();
             context = seekEvent(previousLocation);
             context.setRank(previousRank);
         }
         return context;
     }
 
     // ------------------------------------------------------------------------
     // ITmfTrace - Read operations (returning an actual event)
     // ------------------------------------------------------------------------
 
     @Override
     public synchronized ITmfEvent getNext(final ITmfContext context) {
         // parseEvent() does not update the context
         final ITmfEvent event = fParser.parseEvent(context);
         if (event != null) {
             updateAttributes(context, event.getTimestamp());
             context.setLocation(getCurrentLocation());
             context.increaseRank();
             processEvent(event);
         }
         return event;
     }
 
     /**
      * Hook for special event processing by the concrete class
      * (called by TmfTrace.getEvent())
      *
      * @param event the event
      */
     protected void processEvent(final ITmfEvent event) {
         // Do nothing
     }
 
     /**
      * Update the trace attributes
      *
      * @param context the current trace context
      * @param timestamp the corresponding timestamp
      * @since 2.0
      */
     protected synchronized void updateAttributes(final ITmfContext context, final ITmfTimestamp timestamp) {
         if (fStartTime.equals(TmfTimestamp.BIG_BANG) || (fStartTime.compareTo(timestamp, false) > 0)) {
             fStartTime = timestamp;
         }
         if (fEndTime.equals(TmfTimestamp.BIG_CRUNCH) || (fEndTime.compareTo(timestamp, false) < 0)) {
             fEndTime = timestamp;
         }
         if (context.hasValidRank()) {
             long rank = context.getRank();
             if (fNbEvents <= rank) {
                 fNbEvents = rank + 1;
             }
             if (fIndexer != null) {
                 fIndexer.updateIndex(context, timestamp);
             }
         }
     }
 
     // ------------------------------------------------------------------------
     // TmfDataProvider
     // ------------------------------------------------------------------------
 
     /**
      * @since 2.0
      */
     @Override
     public synchronized ITmfContext armRequest(final ITmfDataRequest request) {
         if (executorIsShutdown()) {
             return null;
         }
         if ((request instanceof ITmfEventRequest)
             && !TmfTimestamp.BIG_BANG.equals(((ITmfEventRequest) request).getRange().getStartTime())
             && (request.getIndex() == 0))
         {
             final ITmfContext context = seekEvent(((ITmfEventRequest) request).getRange().getStartTime());
             ((ITmfEventRequest) request).setStartIndex((int) context.getRank());
             return context;
 
         }
         return seekEvent(request.getIndex());
     }
 
     // ------------------------------------------------------------------------
     // Signal handlers
     // ------------------------------------------------------------------------
 
     /**
      * Handler for the Trace Opened signal
      *
      * @param signal
      *            The incoming signal
      * @since 2.0
      */
     @TmfSignalHandler
     public void traceOpened(TmfTraceOpenedSignal signal) {
         boolean signalIsForUs = false;
         for (ITmfTrace trace : TmfTraceManager.getTraceSet(signal.getTrace())) {
             if (trace == this) {
                 signalIsForUs = true;
                 break;
             }
         }
 
         if (!signalIsForUs) {
             return;
         }
 
         /*
          * The signal is either for this trace, or for an experiment containing
          * this trace.
          */
         try {
             buildStatistics();
             buildStateSystem();
         } catch (TmfTraceException e) {
             e.printStackTrace();
         }
 
         /* Refresh the project, so it can pick up new files that got created. */
         try {
             if (fResource != null) {
                 fResource.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
             }
         } catch (CoreException e) {
             e.printStackTrace();
         }
 
         if (signal.getTrace() == this) {
             /* Additionally, the signal is directly for this trace or experiment. */
             if (getNbEvents() == 0) {
                 return;
             }
 
             final TmfTimeRange timeRange = new TmfTimeRange(getStartTime(), TmfTimestamp.BIG_CRUNCH);
             final TmfTraceRangeUpdatedSignal rangeUpdatedsignal = new TmfTraceRangeUpdatedSignal(this, this, timeRange);
 
             // Broadcast in separate thread to prevent deadlock
             new Thread() {
                 @Override
                 public void run() {
                     broadcast(rangeUpdatedsignal);
                 }
             }.start();
             return;
         }
     }
 
     /**
      * Signal handler for the TmfTraceRangeUpdatedSignal signal
      *
      * @param signal The incoming signal
      * @since 2.0
      */
     @TmfSignalHandler
     public void traceRangeUpdated(final TmfTraceRangeUpdatedSignal signal) {
         if (signal.getTrace() == this) {
             getIndexer().buildIndex(getNbEvents(), signal.getRange(), false);
         }
     }
 
     // ------------------------------------------------------------------------
     // toString
     // ------------------------------------------------------------------------
 
     @Override
     @SuppressWarnings("nls")
     public synchronized String toString() {
         return "TmfTrace [fPath=" + fPath + ", fCacheSize=" + fCacheSize
                 + ", fNbEvents=" + fNbEvents + ", fStartTime=" + fStartTime
                 + ", fEndTime=" + fEndTime + ", fStreamingInterval=" + fStreamingInterval + "]";
     }
 
 }
