 /*
  *  Copyright (c) 2012 Sam Harwell, Tunnel Vision Laboratories LLC
  *  All rights reserved.
  *
  *  The source code of this document is proprietary work, and is not licensed for
  *  distribution. For information about licensing, contact Sam Harwell at:
  *      sam@tunnelvisionlabs.com
  */
 package org.antlr.netbeans.parsing.spi.impl;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.EnumSet;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.Callable;
 import java.util.concurrent.Delayed;
 import java.util.concurrent.ExecutionException;
 import java.util.concurrent.Future;
 import java.util.concurrent.RejectedExecutionHandler;
 import java.util.concurrent.RunnableScheduledFuture;
 import java.util.concurrent.ScheduledFuture;
 import java.util.concurrent.ScheduledThreadPoolExecutor;
 import java.util.concurrent.ThreadFactory;
 import java.util.concurrent.ThreadPoolExecutor;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.TimeoutException;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.swing.text.Document;
 import javax.swing.text.JTextComponent;
 import org.antlr.netbeans.editor.text.DocumentSnapshot;
 import org.antlr.netbeans.editor.text.VersionedDocument;
 import org.antlr.netbeans.parsing.spi.ParseContext;
 import org.antlr.netbeans.parsing.spi.ParserData;
 import org.antlr.netbeans.parsing.spi.ParserDataDefinition;
 import org.antlr.netbeans.parsing.spi.ParserDataEvent;
 import org.antlr.netbeans.parsing.spi.ParserDataListener;
 import org.antlr.netbeans.parsing.spi.ParserDataOptions;
 import org.antlr.netbeans.parsing.spi.ParserResultHandler;
 import org.antlr.netbeans.parsing.spi.ParserTask;
 import org.antlr.netbeans.parsing.spi.ParserTaskDefinition;
 import org.antlr.netbeans.parsing.spi.ParserTaskManager;
 import org.antlr.netbeans.parsing.spi.ParserTaskProvider;
 import org.antlr.netbeans.parsing.spi.ParserTaskScheduler;
 import org.netbeans.api.annotations.common.NonNull;
 import org.netbeans.api.editor.mimelookup.MimeLookup;
 import org.netbeans.lib.editor.util.ListenerList;
 import org.openide.util.Exceptions;
 import org.openide.util.Lookup;
 import org.openide.util.Parameters;
 import org.openide.util.lookup.ServiceProvider;
 
 /**
  *
  * @author Sam Harwell
  */
 @ServiceProvider(service=ParserTaskManager.class)
 public class ParserTaskManagerImpl implements ParserTaskManager {
     // -J-Dorg.antlr.netbeans.parsing.spi.impl.ParserTaskManagerImpl.level=FINE
     private static final Logger LOGGER = Logger.getLogger(ParserTaskManagerImpl.class.getName());
 
     private static final Long DEFAULT_DELAY = 500L;
     private static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.MILLISECONDS;
 
     private static final int HIGH_THREAD_PRIORITY_VALUE = Thread.NORM_PRIORITY;
     private static final int LOW_THREAD_PRIORITY_VALUE = Thread.NORM_PRIORITY - 2;
 
     private final ListenerList<ParserDataListener<Object>> globalListeners = new ListenerList<ParserDataListener<Object>>();
 
     private final Map<ParserDataDefinition<?>, ListenerList<ParserDataListener<?>>> dataListeners =
         new HashMap<ParserDataDefinition<?>, ListenerList<ParserDataListener<?>>>();
 
     private final Map<String, Collection<? extends ParserTaskProvider>> taskProviders =
         new HashMap<String, Collection<? extends ParserTaskProvider>>();
 
     private static final String DOCUMENT_PROPERTIES_KEY = ParserTaskManagerImpl.class.getName() + "-properties";
 
     private final RejectionHandler rejectionHandler;
     private final ScheduledThreadPoolExecutor highPriorityExecutor;
     private final ScheduledThreadPoolExecutor lowPriorityExecutor;
 
     public ParserTaskManagerImpl() {
         rejectionHandler = new RejectionHandler();
 
         int highPriorityPoolSize = 2;
         highPriorityExecutor = new PriorityInsertionScheduledThreadPoolExecutor(highPriorityPoolSize, new ParserThreadFactory(HIGH_THREAD_PRIORITY_VALUE), rejectionHandler);
 
         int lowPriorityPoolSize = 2;//Math.max(2, Runtime.getRuntime().availableProcessors());
         lowPriorityExecutor = new PriorityInsertionScheduledThreadPoolExecutor(lowPriorityPoolSize, new ParserThreadFactory(LOW_THREAD_PRIORITY_VALUE), rejectionHandler);
     }
 
     @Override
     public <T> Future<ParserData<T>> getData(DocumentSnapshot snapshot, ParserDataDefinition<T> definition) {
         return getData(snapshot, null, definition);
     }
 
     @Override
     public Future<ParserData<?>>[] getData(DocumentSnapshot snapshot, Collection<ParserDataDefinition<?>> definitions) {
         return getData(snapshot, null, definitions);
     }
 
     @Override
     public <T> Future<ParserData<T>> getData(final DocumentSnapshot snapshot, final ParserDataDefinition<T> definition, EnumSet<ParserDataOptions> options) {
         return getData(snapshot, null, definition, options);
     }
 
     @Override
     public Future<ParserData<?>>[] getData(DocumentSnapshot snapshot, Collection<ParserDataDefinition<?>> definitions, EnumSet<ParserDataOptions> options) {
         return getData(snapshot, null, definitions, options);
     }
 
     @Override
     public <T> Future<ParserData<T>> getData(DocumentSnapshot snapshot, JTextComponent component, ParserDataDefinition<T> definition) {
         return getData(snapshot, definition, EnumSet.noneOf(ParserDataOptions.class));
     }
 
     @Override
     public Future<ParserData<?>>[] getData(DocumentSnapshot snapshot, JTextComponent component, Collection<ParserDataDefinition<?>> definitions) {
         return getData(snapshot, definitions, EnumSet.noneOf(ParserDataOptions.class));
     }
 
     @Override
     public <T> Future<ParserData<T>> getData(final DocumentSnapshot snapshot, JTextComponent component, final ParserDataDefinition<T> definition, EnumSet<ParserDataOptions> options) {
         Parameters.notNull("snapshot", snapshot);
         Parameters.notNull("definition", definition);
         Parameters.notNull("options", options);
 
         @SuppressWarnings("unchecked")
         ParserData<T> cachedData = getCachedData(snapshot.getVersionedDocument(), definition);
         boolean useCached = options.contains(ParserDataOptions.NO_UPDATE);
         boolean allowStale = options.contains(ParserDataOptions.ALLOW_STALE);
         if (!useCached && cachedData != null) {
             if (allowStale) {
                 useCached = true;
             } else if (cachedData.getSnapshot().equals(snapshot)) {
                 useCached = true;
             }
         }
 
         if (useCached) {
             if (!allowStale && cachedData != null && !cachedData.getSnapshot().equals(snapshot)) {
                 cachedData = null;
             }
 
             return new CompletedFuture<ParserData<T>>(cachedData, null);
         }
 
         ParseContext context = new ParseContext(snapshot, component);
         Callable<ParserData<T>> callable = createCallable(context, definition);
         if (options.contains(ParserDataOptions.SYNCHRONOUS) || isParserThread()) {
             try {
                 return new CompletedFuture<ParserData<T>>(callable.call(), null);
             } catch (Exception ex) {
                 Exceptions.printStackTrace(ex);
                 return new CompletedFuture<ParserData<T>>(null, ex);
             }
         }
 
         return lowPriorityExecutor.schedule(callable, 0, TimeUnit.NANOSECONDS);
     }
 
     @Override
     public Future<ParserData<?>>[] getData(DocumentSnapshot snapshot, JTextComponent component, Collection<ParserDataDefinition<?>> definitions, EnumSet<ParserDataOptions> options) {
         Parameters.notNull("snapshot", snapshot);
         Parameters.notNull("definitions", definitions);
         Parameters.notNull("options", options);
 
         throw new UnsupportedOperationException("Not supported yet.");
 //        List<ParserData<?>> data = new ArrayList<ParserData<?>>();
 //        for (ParserDataDefinition<?> definition : definitions) {
 //            data.add(getData(snapshot, definition, options));
 //        }
 //
 //        return data.toArray(new ParserData<?>[0]);
     }
 
     @Override
     public void reschedule(VersionedDocument document, Class<? extends ParserTaskScheduler> schedulerClass) {
         reschedule(document, null, schedulerClass);
     }
 
     @Override
     public void reschedule(VersionedDocument document, JTextComponent component, Class<? extends ParserTaskScheduler> schedulerClass) {
         Collection<? extends ParserTaskScheduler> schedulers = Lookup.getDefault().lookupAll(ParserTaskScheduler.class);
         ParserTaskScheduler scheduler = null;
         for (ParserTaskScheduler i : schedulers) {
             if (i.getClass() == schedulerClass) {
                 scheduler = i;
                 break;
             }
         }
 
         if (scheduler != null) {
             @SuppressWarnings("rawtypes")
             Collection<? extends ParserDataDefinition> data = MimeLookup.getLookup(document.getMimeType()).lookupAll(ParserDataDefinition.class);
             for (ParserDataDefinition<?> definition : data) {
                 if (definition.getScheduler() == schedulerClass && definition.isCacheable()) {
                    clearCachedData(document, definition);
                 }
             }
 
             ParseContext context = new ParseContext(scheduler, document, component);
             scheduler.schedule(context);
         }
     }
 
     @Override
     public void reschedule(VersionedDocument document, JTextComponent component, long delay, TimeUnit timeUnit, Class<? extends ParserTaskScheduler> schedulerClass) {
         Collection<? extends ParserTaskScheduler> schedulers = Lookup.getDefault().lookupAll(ParserTaskScheduler.class);
         ParserTaskScheduler scheduler = null;
         for (ParserTaskScheduler i : schedulers) {
             if (i.getClass() == schedulerClass) {
                 scheduler = i;
                 break;
             }
         }
 
         if (scheduler != null) {
             @SuppressWarnings("rawtypes")
             Collection<? extends ParserDataDefinition> data = MimeLookup.getLookup(document.getMimeType()).lookupAll(ParserDataDefinition.class);
             for (ParserDataDefinition<?> definition : data) {
                 if (definition.getScheduler() == schedulerClass && definition.isCacheable()) {
                    clearCachedData(document, definition);
                 }
             }
 
             ParseContext context = new ParseContext(scheduler, document, component);
             scheduler.schedule(context, delay, timeUnit);
         }
     }
 
 //    @Override
 //    public <T> ScheduledFuture<ParserData<T>> scheduleData(ParseRequest request, ParserDataDefinition<T> data) {
 //        return scheduleData(request, null, data);
 //    }
 //
 //    @Override
 //    public Map<ParserDataDefinition<?>, ScheduledFuture<ParserData<?>>> scheduleData(ParseRequest request, Collection<ParserDataDefinition<?>> data) {
 //        return scheduleData(request, null, data);
 //    }
 //
 //    @Override
 //    public <T> ScheduledFuture<ParserData<T>> scheduleData(ParseRequest request, ParserDataDefinition<T> data, long delay, TimeUnit timeUnit) {
 //        return scheduleData(request, null, data, delay, timeUnit);
 //    }
 //
 //    @Override
 //    public Map<ParserDataDefinition<?>, ScheduledFuture<ParserData<?>>> scheduleData(ParseRequest request, @NonNull Collection<ParserDataDefinition<?>> data, long delay, TimeUnit timeUnit) {
 //        return scheduleData(request, null, data, delay, timeUnit);
 //    }
 
     @Override
     public <T> ScheduledFuture<ParserData<T>> scheduleData(ParseContext context, ParserDataDefinition<T> data) {
         return scheduleData(context, data, DEFAULT_DELAY, DEFAULT_TIMEUNIT);
     }
 
     @Override
     public Map<ParserDataDefinition<?>, ScheduledFuture<ParserData<?>>> scheduleData(ParseContext context, Collection<ParserDataDefinition<?>> data) {
         return scheduleData(context, data, DEFAULT_DELAY, DEFAULT_TIMEUNIT);
     }
 
     @Override
     public <T> ScheduledFuture<ParserData<T>> scheduleData(ParseContext context, ParserDataDefinition<T> data, long delay, TimeUnit timeUnit) {
         Callable<ParserData<T>> callable = createCallable(context, data);
         return lowPriorityExecutor.schedule(callable, delay, timeUnit);
     }
 
     @Override
     @SuppressWarnings({"unchecked", "rawtypes"})
     public Map<ParserDataDefinition<?>, ScheduledFuture<ParserData<?>>> scheduleData(ParseContext context, @NonNull Collection<ParserDataDefinition<?>> data, long delay, TimeUnit timeUnit) {
         if (data.isEmpty()) {
             return Collections.emptyMap();
         }
 
         Map<ParserDataDefinition<?>, ScheduledFuture<ParserData<?>>> futures = new HashMap<ParserDataDefinition<?>, ScheduledFuture<ParserData<?>>>();
         for (ParserDataDefinition dataDefinition : data) {
             futures.put(dataDefinition, (ScheduledFuture<ParserData<?>>)scheduleData(context, dataDefinition, delay, timeUnit));
         }
 
         return futures;
     }
 
 //    @Override
 //    public ScheduledFuture<Collection<ParserData<?>>> scheduleTask(@NonNull ParseRequest request, @NonNull ParserTaskProvider provider) {
 //        return scheduleTask(request, null, provider);
 //    }
 //
 //    @Override
 //    public Map<ParserTaskProvider, ScheduledFuture<Collection<ParserData<?>>>> scheduleTask(@NonNull ParseRequest request, @NonNull Collection<ParserTaskProvider> providers) {
 //        return scheduleTask(request, null, providers);
 //    }
 //
 //    @Override
 //    public ScheduledFuture<Collection<ParserData<?>>> scheduleTask(@NonNull ParseRequest request, @NonNull ParserTaskProvider provider, long delay, @NonNull TimeUnit timeUnit) {
 //        return scheduleTask(request, null, provider, delay, timeUnit);
 //    }
 //
 //    @Override
 //    public Map<ParserTaskProvider, ScheduledFuture<Collection<ParserData<?>>>> scheduleTask(@NonNull ParseRequest request, @NonNull Collection<ParserTaskProvider> providers, long delay, @NonNull TimeUnit timeUnit) {
 //        return scheduleTask(request, null, providers, delay, timeUnit);
 //    }
 
     @Override
     public ScheduledFuture<Collection<ParserData<?>>> scheduleTask(@NonNull ParseContext context, @NonNull ParserTaskProvider provider) {
         return scheduleTask(context, provider, DEFAULT_DELAY, DEFAULT_TIMEUNIT);
     }
 
     @Override
     public Map<ParserTaskProvider, ScheduledFuture<Collection<ParserData<?>>>> scheduleTask(@NonNull ParseContext context, @NonNull Collection<ParserTaskProvider> providers) {
         return scheduleTask(context, providers, DEFAULT_DELAY, DEFAULT_TIMEUNIT);
     }
 
     @Override
     public ScheduledFuture<Collection<ParserData<?>>> scheduleTask(@NonNull ParseContext context, @NonNull ParserTaskProvider provider, long delay, @NonNull TimeUnit timeUnit) {
         Callable<Collection<ParserData<?>>> callable = createCallable(context, provider);
         return lowPriorityExecutor.schedule(callable, delay, timeUnit);
     }
 
     @Override
     public Map<ParserTaskProvider, ScheduledFuture<Collection<ParserData<?>>>> scheduleTask(@NonNull ParseContext context, @NonNull Collection<ParserTaskProvider> providers, long delay, @NonNull TimeUnit timeUnit) {
         if (providers.isEmpty()) {
             return Collections.emptyMap();
         }
 
         Map<ParserTaskProvider, ScheduledFuture<Collection<ParserData<?>>>> result = new HashMap<ParserTaskProvider, ScheduledFuture<Collection<ParserData<?>>>>();
         for (ParserTaskProvider provider : providers) {
             result.put(provider, scheduleTask(context, provider, delay, timeUnit));
         }
 
         return result;
     }
 
     @Override
     public <T> ScheduledFuture<T> scheduleLowPriority(Callable<T> callable) {
         Parameters.notNull("callable", callable);
 
         return lowPriorityExecutor.schedule(callable, 0, TimeUnit.MILLISECONDS);
     }
 
     @Override
     public <T> ScheduledFuture<T> scheduleHighPriority(Callable<T> callable) {
         Parameters.notNull("callable", callable);
 
         return highPriorityExecutor.schedule(callable, 0, TimeUnit.MILLISECONDS);
     }
 
     @Override
     public void addDataListener(ParserDataListener<Object> listener) {
         Parameters.notNull("listener", listener);
 
         synchronized (globalListeners) {
             globalListeners.add(listener);
         }
     }
 
     @Override
     public void removeDataListener(ParserDataListener<Object> listener) {
         Parameters.notNull("listener", listener);
 
         synchronized (globalListeners) {
             globalListeners.remove(listener);
         }
     }
 
     @Override
     public <T> void addDataListener(ParserDataDefinition<T> definition, ParserDataListener<T> listener) {
         Parameters.notNull("definition", definition);
         Parameters.notNull("listener", listener);
 
         synchronized (dataListeners) {
             ListenerList<ParserDataListener<?>> listeners = dataListeners.get(definition);
             if (listeners == null) {
                 listeners = new ListenerList<ParserDataListener<?>>();
                 dataListeners.put(definition, listeners);
             }
 
             listeners.add(listener);
         }
     }
 
     @Override
     public <T> void removeDataListener(ParserDataDefinition<T> definition, ParserDataListener<T> listener) {
         Parameters.notNull("definition", definition);
         Parameters.notNull("listener", listener);
 
         synchronized (dataListeners) {
             ListenerList<ParserDataListener<?>> listeners = dataListeners.get(definition);
             if (listeners == null) {
                 return;
             }
 
             listeners.remove(listener);
             if (listeners.getListenerCount() == 0) {
                 dataListeners.remove(definition);
             }
         }
     }
 
     private static boolean isParserThread() {
         return Thread.currentThread() instanceof ParserThread;
     }
 
     private <T> Callable<ParserData<T>> createCallable(ParseContext context, ParserDataDefinition<T> data) {
         Callable<ParserData<T>> callable = new UpdateDataCallable<T>(this, context, data);
         return callable;
     }
 
     private Callable<Collection<ParserData<?>>> createCallable(ParseContext context, ParserTaskProvider provider) {
         Callable<Collection<ParserData<?>>> callable = new UpdateTaskCallable(this, context, provider);
         return callable;
     }
 
     private <T> void fireDataChanged(ParserDataDefinition<T> definition, ParserData<T> data) {
         ListenerList<ParserDataListener<?>> listeners;
         synchronized (dataListeners) {
             listeners = dataListeners.get(definition);
         }
 
         if (listeners == null && globalListeners.getListenerCount() == 0) {
             return;
         }
 
         ParserDataEvent<T> event = new ParserDataEvent<T>(this, definition, data);
 
         if (listeners != null) {
             for (ParserDataListener<?> listener : listeners.getListeners()) {
                 @SuppressWarnings("unchecked")
                 ParserDataListener<T> typedListener = (ParserDataListener<T>)listener;
                 typedListener.dataChanged(event);
             }
         }
 
         for (ParserDataListener<Object> listener : globalListeners.getListeners()) {
             listener.dataChanged(event);
         }
     }
 
     private ParserTaskProvider getTaskProvider(VersionedDocument versionedDocument, ParserDataDefinition<?> definition) {
         Collection<? extends ParserTaskProvider> providers = getTaskProviders(versionedDocument);
         for (ParserTaskProvider provider : providers) {
             ParserTaskDefinition taskDefinition = provider.getDefinition();
             boolean hasOutput = taskDefinition.getOutputs().contains(definition);
             if (hasOutput) {
                 return provider;
             }
         }
 
         return null;
     }
 
     private Collection<? extends ParserTaskProvider> getTaskProviders(VersionedDocument versionedDocument) {
         String mimeType = versionedDocument.getMimeType();
         synchronized (taskProviders) {
             Collection<? extends ParserTaskProvider> providers = taskProviders.get(mimeType);
             if (providers == null) {
                 providers = MimeLookup.getLookup(mimeType).lookupAll(ParserTaskProvider.class);
                 taskProviders.put(mimeType, providers);
             }
 
             return providers;
         }
     }
 
     @SuppressWarnings("unchecked")
     private synchronized <T> ParserData<T> getCachedData(VersionedDocument versionedDocument, ParserDataDefinition<T> definition) {
         Document document = versionedDocument.getDocument();
         if (document != null) {
             Object cachedData = document.getProperty(definition);
             if (cachedData instanceof ParserData<?>) {
                 return (ParserData<T>)cachedData;
             }
 
             return null;
         }
 
         Map<ParserDataDefinition<?>, ParserData<?>> documentProperties = (Map<ParserDataDefinition<?>, ParserData<?>>)versionedDocument.getProperty(DOCUMENT_PROPERTIES_KEY);
         if (documentProperties != null) {
             return (ParserData<T>)documentProperties.get(definition);
         }
 
         return null;
     }
 
    private synchronized boolean clearCachedData(VersionedDocument versionedDocument, ParserDataDefinition<?> definition) {
        Document document = versionedDocument.getDocument();
        if (document != null) {
            ParserData<?> previousData = (ParserData<?>)document.getProperty(definition);
            if (previousData == null) {
                return false;
            }

            document.putProperty(definition, null);
            return true;
        }

        Map<?, ?> documentProperties = (Map<?, ?>)versionedDocument.getProperty(DOCUMENT_PROPERTIES_KEY);
        if (documentProperties == null) {
            return false;
        }

        return documentProperties.remove(definition) != null;
    }

     private synchronized boolean updateCachedData(VersionedDocument versionedDocument, ParserDataDefinition<?> definition, ParserData<?> data) {
         if (data == null) {
             return false;
         }
 
         Document document = versionedDocument.getDocument();
         if (document != null) {
             ParserData<?> previousData = (ParserData<?>)document.getProperty(definition);
             if (previousData == data || (previousData != null && previousData.equals(data))) {
                 return false;
             }
             else if (previousData != null && previousData.getSnapshot().getVersion().getVersionNumber() > data.getSnapshot().getVersion().getVersionNumber()) {
                 // don't replace new data with old
                 return false;
             }
 
             document.putProperty(definition, data);
             return true;
         }
 
         @SuppressWarnings("unchecked")
         Map<ParserDataDefinition<?>, ParserData<?>> documentProperties = (Map<ParserDataDefinition<?>, ParserData<?>>)versionedDocument.getProperty(DOCUMENT_PROPERTIES_KEY);
         if (documentProperties == null) {
             documentProperties = new HashMap<ParserDataDefinition<?>, ParserData<?>>();
             versionedDocument.putProperty(DOCUMENT_PROPERTIES_KEY, documentProperties);
         }
 
         ParserData<?> previousData = (ParserData<?>)documentProperties.get(definition);
         if (previousData == data || (previousData != null && previousData.equals(data))) {
             return false;
         }
         else if (previousData != null && previousData.getSnapshot().getVersion().getVersionNumber() > data.getSnapshot().getVersion().getVersionNumber()) {
             // don't replace new data with old
             return false;
         }
 
         documentProperties.put(definition, data);
         return previousData != data && (previousData == null || !previousData.equals(data));
     }
 
     private static class RejectionHandler implements RejectedExecutionHandler {
 
         @Override
         public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
             throw new UnsupportedOperationException("Not supported yet.");
         }
 
     }
 
     private static class ParserThread extends Thread {
 
         public ParserThread(Runnable target) {
             super(target);
         }
 
     }
 
     private static class ParserThreadFactory implements ThreadFactory {
         private final int priority;
         private int threadCount;
 
         public ParserThreadFactory(int priority) {
             if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
                 throw new IllegalArgumentException("Invalid thread priority.");
             }
 
             this.priority = priority;
         }
 
         @Override
         public Thread newThread(Runnable r) {
             Thread thread = new ParserThread(r);
             thread.setPriority(priority);
 
             String priorityName = priority >= HIGH_THREAD_PRIORITY_VALUE ? "Foreground" : "Background";
             String name = String.format("Parse (%s) #%d", priorityName, ++threadCount);
             thread.setName(name);
 
             return thread;
         }
     }
 
     private static abstract class UpdateCallable<Result> implements Callable<Result> {
         protected final ParserTaskManagerImpl outer;
         protected final ParseContext context;
 
         protected UpdateCallable(ParserTaskManagerImpl outer, ParseContext context) {
             this.outer = outer;
             this.context = context;
         }
     }
 
     private static class UpdateDataCallable<T> extends UpdateCallable<ParserData<T>> {
         private final ParserDataDefinition<T> data;
 
         public UpdateDataCallable(ParserTaskManagerImpl outer, ParseContext context, ParserDataDefinition<T> data) {
             super(outer, context);
             this.data = data;
         }
 
         @Override
         @SuppressWarnings("unchecked")
         public ParserData<T> call() throws Exception {
             VersionedDocument document = context.getDocument();
             DocumentSnapshot snapshot = context.getSnapshot();
             if (snapshot == null) {
                 snapshot = document.getCurrentSnapshot();
             }
 
             if (data.isCacheable()) {
                 ParserData<T> cachedData = outer.getCachedData(context.getDocument(), data);
                 if (cachedData != null && cachedData.getSnapshot().equals(snapshot)) {
                     return cachedData;
                 }
             }
 
             ParserTaskProvider provider = outer.getTaskProvider(document, data);
             if (provider == null) {
                 LOGGER.log(Level.WARNING, "No provider found for parser data \"{0}\".", data.getName());
                 return null;
             } else if (LOGGER.isLoggable(Level.FINE)) {
                 Object[] args = { provider.getDefinition().getName(), data.getName() };
                 LOGGER.log(Level.FINE, "Using provider \"{0}\" for data \"{1}\".", args);
             }
 
             final ParserTask task = provider.createTask(document);
 
             if (LOGGER.isLoggable(Level.FINE)) {
                 String threadName = Thread.currentThread().getName();
                 String messageFormat = "{0}: Updating data \"{1}\" with task \"{2}\" for {3}#{4}";
                 LOGGER.log(Level.FINE, messageFormat, new Object[] { threadName, data.getName(), task.getDefinition().getName(), document.getFileObject().getPath(), snapshot.getVersion().getVersionNumber() });
             }
 
             ResultAggregator handler = new ResultAggregator(outer, document);
             task.parse(outer, context, snapshot, Collections.<ParserDataDefinition<?>>singleton(data), handler);
 
             for (ParserData<?> result : handler.getUpdatedResults()) {
                 outer.fireDataChanged((ParserDataDefinition)result.getDefinition(), result);
             }
 
             for (ParserData<?> result : handler.getResults()) {
                 if (result.getDefinition().equals(data)) {
                     return (ParserData<T>)result;
                 }
             }
 
             return null;
         }
     }
 
     private static class UpdateTaskCallable extends UpdateCallable<Collection<ParserData<?>>> {
         private final ParserTaskProvider provider;
 
         public UpdateTaskCallable(ParserTaskManagerImpl outer, ParseContext context, ParserTaskProvider provider) {
             super(outer, context);
             this.provider = provider;
         }
 
         @Override
         @SuppressWarnings("unchecked")
         public Collection<ParserData<?>> call() throws Exception {
             VersionedDocument document = context.getDocument();
             final ParserTask task = provider.createTask(document);
             DocumentSnapshot snapshot = context.getSnapshot();
             if (snapshot == null) {
                 snapshot = document.getCurrentSnapshot();
             }
 
             if (LOGGER.isLoggable(Level.FINE)) {
                 String messageFormat = "{0}: Updating task \"{1}\" for {2}#{3}";
                 Object[] args =
                     {
                         Thread.currentThread().getName(),
                         task.getDefinition().getName(),
                         document.getFileObject().getPath(),
                         snapshot.getVersion().getVersionNumber()
                     };
                 LOGGER.log(Level.FINE, messageFormat, args);
             }
 
             ResultAggregator handler = new ResultAggregator(outer, document);
             task.parse(outer, context, snapshot, provider.getDefinition().getOutputs(), handler);
 
             for (ParserData<?> result : handler.getUpdatedResults()) {
                 outer.fireDataChanged((ParserDataDefinition)result.getDefinition(), result);
             }
 
             return handler.getResults();
         }
     }
 
     private static class ResultAggregator implements ParserResultHandler {
         private final List<ParserData<?>> results = new ArrayList<ParserData<?>>();
         private final List<ParserData<?>> updatedResults = new ArrayList<ParserData<?>>();
         private final ParserTaskManagerImpl outer;
         private final VersionedDocument document;
 
         public ResultAggregator(@NonNull ParserTaskManagerImpl outer, @NonNull VersionedDocument document) {
             Parameters.notNull("outer", outer);
             Parameters.notNull("document", document);
             this.document = document;
             this.outer = outer;
         }
 
         @Override
         public <T> void addResult(@NonNull ParserData<T> result) {
             Parameters.notNull("result", result);
             results.add(result);
             boolean cacheable = result.getDefinition().isCacheable();
             boolean updated = !cacheable;
             if (cacheable) {
                 updated |= outer.updateCachedData(document, result.getDefinition(), result);
             }
 
             if (updated) {
                 updatedResults.add(result);
             }
         }
 
         public List<ParserData<?>> getResults() {
             return results;
         }
 
         public List<ParserData<?>> getUpdatedResults() {
             return updatedResults;
         }
     }
 
     private static class PriorityInsertionScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {
 
         public PriorityInsertionScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
             super(corePoolSize, threadFactory, handler);
         }
 
         @Override
         protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
             int priority = PRIORITY_INITIAL;
             if (task.getDelay(DEFAULT_TIMEUNIT) <= 0) {
                 priority += PRIORITY_IMMEDIATE_OFFSET;
             }
 
             if (callable instanceof UpdateCallable<?>) {
                 UpdateCallable<?> updateCallable = (UpdateCallable<?>)callable;
                 if (updateCallable.context.getDocument().getDocument() != null) {
                     priority += PRIORITY_FOREGROUND_OFFSET;
                 }
             }
 
             return new PriorityInsertionRunnableScheduledFuture<V>(task, priority);
         }
 
         @Override
         protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
             int priority = PRIORITY_INITIAL;
             if (task.getDelay(DEFAULT_TIMEUNIT) <= 0) {
                 priority += PRIORITY_IMMEDIATE_OFFSET;
             }
 
             if (runnable instanceof UpdateCallable<?>) {
                 UpdateCallable<?> updateCallable = (UpdateCallable<?>)runnable;
                 if (updateCallable.context.getDocument().getDocument() != null) {
                     priority += PRIORITY_FOREGROUND_OFFSET;
                 }
             }
 
             return new PriorityInsertionRunnableScheduledFuture<V>(task, priority);
         }
 
     }
 
     private static final int PRIORITY_INITIAL = 2;
     private static final int PRIORITY_IMMEDIATE_OFFSET = -1;
     private static final int PRIORITY_FOREGROUND_OFFSET = -2;
 
     private static class PriorityInsertionRunnableScheduledFuture<V> implements RunnableScheduledFuture<V> {
         private final RunnableScheduledFuture<V> wrappedTask;
         private final int priority;
 
         public PriorityInsertionRunnableScheduledFuture(@NonNull RunnableScheduledFuture<V> wrappedTask, int priority) {
             Parameters.notNull("wrappedTask", wrappedTask);
             this.wrappedTask = wrappedTask;
             this.priority = priority;
         }
 
         @Override
         public boolean isPeriodic() {
             return wrappedTask.isPeriodic();
         }
 
         @Override
         public void run() {
             wrappedTask.run();
         }
 
         @Override
         public boolean cancel(boolean mayInterruptIfRunning) {
             return wrappedTask.cancel(mayInterruptIfRunning);
         }
 
         @Override
         public boolean isCancelled() {
             return wrappedTask.isCancelled();
         }
 
         @Override
         public boolean isDone() {
             return wrappedTask.isDone();
         }
 
         @Override
         public V get() throws InterruptedException, ExecutionException {
             return wrappedTask.get();
         }
 
         @Override
         public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
             return wrappedTask.get(timeout, unit);
         }
 
         @Override
         public long getDelay(TimeUnit unit) {
             return wrappedTask.getDelay(unit);
         }
 
         @Override
         public int compareTo(Delayed o) {
             if (o instanceof PriorityInsertionRunnableScheduledFuture<?>) {
                 PriorityInsertionRunnableScheduledFuture<?> other = (PriorityInsertionRunnableScheduledFuture<?>)o;
                 if (this.priority != other.priority) {
                     return this.priority - other.priority;
                 }
 
                 return wrappedTask.compareTo(((PriorityInsertionRunnableScheduledFuture<?>)o).wrappedTask);
             }
 
             return -1;
         }
 
     }
 
 }
