 /*
  * Copyright Myrrix Ltd
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
 
 package net.myrrix.online;
 
 import java.io.BufferedReader;
 import java.io.Closeable;
 import java.io.File;
 import java.io.IOException;
 import java.io.Reader;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.List;
 import java.util.NoSuchElementException;
 import java.util.Queue;
 import java.util.concurrent.Callable;
 import java.util.concurrent.ExecutionException;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.util.concurrent.Future;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.locks.Lock;
 import java.util.concurrent.locks.ReadWriteLock;
 
 import com.google.common.base.CharMatcher;
 import com.google.common.base.Preconditions;
 import com.google.common.base.Splitter;
 import com.google.common.collect.Lists;
 import com.google.common.io.Closeables;
 import com.google.common.net.HostAndPort;
 import com.google.common.util.concurrent.ThreadFactoryBuilder;
 import org.apache.commons.math3.util.FastMath;
 import org.apache.mahout.cf.taste.common.NoSuchItemException;
 import org.apache.mahout.cf.taste.common.NoSuchUserException;
 import org.apache.mahout.cf.taste.common.Refreshable;
 import org.apache.mahout.cf.taste.common.TasteException;
 import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
 import org.apache.mahout.cf.taste.model.DataModel;
 import org.apache.mahout.cf.taste.model.IDMigrator;
 import org.apache.mahout.cf.taste.recommender.IDRescorer;
 import org.apache.mahout.cf.taste.recommender.RecommendedItem;
 import org.apache.mahout.cf.taste.recommender.Rescorer;
 import org.apache.mahout.common.LongPair;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import net.myrrix.common.ClassUtils;
 import net.myrrix.common.OneWayMigrator;
 import net.myrrix.common.math.Solver;
 import net.myrrix.common.parallel.ExecutorUtils;
 import net.myrrix.common.ReloadingReference;
 import net.myrrix.common.MutableRecommendedItem;
 import net.myrrix.common.collection.FastByIDFloatMap;
 import net.myrrix.common.collection.FastIDSet;
 import net.myrrix.common.io.IOUtils;
 import net.myrrix.common.LangUtils;
 import net.myrrix.common.MyrrixRecommender;
 import net.myrrix.common.NotReadyException;
 import net.myrrix.common.TopN;
 import net.myrrix.common.collection.FastByIDMap;
 import net.myrrix.online.candidate.CandidateFilter;
 import net.myrrix.common.math.SimpleVectorMath;
 import net.myrrix.online.generation.Generation;
 import net.myrrix.online.generation.GenerationManager;
 import net.myrrix.online.generation.IDCluster;
 
 /**
  * <p>The core implementation of {@link org.apache.mahout.cf.taste.recommender.Recommender} and furthermore
  * {@link MyrrixRecommender} that lies inside the Serving Layer.</p>
  *
  * <p>It is useful to note here, again, that the API methods {@link #setPreference(long, long)}
   * and {@link #removePreference(long, long)}, retained from Apache Mahout, have a somewhat different meaning
   * than in Mahout. They add to an association strength, rather than replace it. See the javadoc.</p>
  */
 public final class ServerRecommender implements MyrrixRecommender, Closeable {
   
   private static final Logger log = LoggerFactory.getLogger(ServerRecommender.class);
 
   private static final Splitter DELIMITER = Splitter.on(CharMatcher.anyOf(",\t")).trimResults();
 
   // Maybe expose this publicly later
   private static final double FOLDIN_LEARN_RATE =
       Double.parseDouble(System.getProperty("model.foldin.learningRate", "1.0"));
   // Only temporary
   private static final double BIG_FOLDIN_THRESHOLD =
       Double.parseDouble(System.getProperty("model.foldin.bigThreshold", "10000.0"));
 
   private final GenerationManager generationManager;
   private final int numCores;
   private final ReloadingReference<ExecutorService> executor;
   private final IDMigrator tagHasher;
 
   /**
    * Calls {@link #ServerRecommender(String, String, File, int, ReloadingReference, File)} for simple local mode,
    * with no bucket, instance ID 0, and no partitions (partition 0 of 1 total).
    */
   public ServerRecommender(File localInputDir) {
     this(null, null, localInputDir, 0, null, null);
   }
 
   /**
    * @param bucket bucket that Serving Layer is using for instances
    * @param instanceID instance ID that the Serving Layer is serving. May be 0 for local mode.
    * @param localInputDir local input and model file directory
    * @param partition partition number in a partitioned distributed mode. 0 if not partitioned.
   * @param allPartitions reference to an object that can describe all partitions; only used to get their count
    * @param licenseFile see {@code RunnerConfiguration} for name and contents. Only used in distributed mode.
    */
   public ServerRecommender(String bucket,
                            String instanceID,
                            File localInputDir,
                            int partition,
                            ReloadingReference<List<List<HostAndPort>>> allPartitions,
                            File licenseFile) {
     Preconditions.checkNotNull(localInputDir, "No local dir");
 
     if (bucket == null || instanceID == null) {
       log.info("Creating ServerRecommender with local input dir {}", localInputDir);
     } else {
       log.info("Creating ServerRecommender for bucket {}, instance {} and with local input dir {}, partition {}",
                bucket, instanceID, localInputDir, partition);
     }
 
     generationManager = ClassUtils.loadInstanceOf(
         "net.myrrix.online.generation.DelegateGenerationManager",
         GenerationManager.class,
         new Class<?>[] { String.class, String.class, File.class, int.class, ReloadingReference.class, File.class },
         new Object[] { bucket, instanceID, localInputDir, partition, allPartitions, licenseFile });
 
     numCores = Runtime.getRuntime().availableProcessors();
     executor = new ReloadingReference<ExecutorService>(new Callable<ExecutorService>() {
       @Override
       public ExecutorService call() {
         return Executors.newFixedThreadPool(
             2 * numCores,
             new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ServerRecommender-%d").build());
       }
     });
     tagHasher = new OneWayMigrator();
   }
 
   public String getBucket() {
     return generationManager.getBucket();
   }
 
   public String getInstanceID() {
     return generationManager.getInstanceID();
   }
 
   public GenerationManager getGenerationManager() {
     return generationManager;
   }
 
   @Deprecated
   @Override
   public void refresh(Collection<Refreshable> alreadyRefreshed) {
     if (alreadyRefreshed != null) {
       log.warn("Ignoring argument {}", alreadyRefreshed);
     }
     refresh();
   }
   
   @Override
   public void refresh() {
     generationManager.refresh();
   }
 
   @Override
   public void ingest(File file) throws TasteException {
     Reader reader = null;
     try {
       reader = IOUtils.openReaderMaybeDecompressing(file);
       ingest(reader);
     } catch (IOException ioe) {
       throw new TasteException(ioe);
     } finally {
       try {
         Closeables.close(reader, true);
       } catch (IOException e) {
         // Can't happen, continue
       }
     }
   }
 
   @Override
   public void ingest(Reader reader) throws TasteException {
     // See also InputFilesReader
     BufferedReader buffered = IOUtils.buffer(reader);
     try {
 
       int lines = 0;
       int badLines = 0;
       String line;
       while ((line = buffered.readLine()) != null) {
         
         if (badLines > 100) { // Crude check
           throw new IOException("Too many bad lines; aborting");
         }
         
         lines++;
         
         if (line.isEmpty() || line.charAt(0) == '#') {
           continue;
         }
         
         Iterator<String> it = DELIMITER.split(line).iterator();
 
         long userID = Long.MIN_VALUE;
         String itemTag = null;        
         long itemID = Long.MIN_VALUE;
         String userTag = null;        
         float value;
         try {
           
           String userIDString = it.next();
           if (userIDString.startsWith("\"")) {
             itemTag = userIDString.substring(1, userIDString.length() - 1);
           } else {
             userID = Long.parseLong(userIDString);
           }
           
           String itemIDString = it.next();
           if (itemIDString.startsWith("\"")) {
             userTag = itemIDString.substring(1, itemIDString.length() - 1);
           } else {
             itemID = Long.parseLong(itemIDString);            
           }
           
           if (it.hasNext()) {
             String valueToken = it.next();
             value = valueToken.isEmpty() ? Float.NaN : LangUtils.parseFloat(valueToken);
           } else {
             value = 1.0f;
           }
 
         } catch (NoSuchElementException ignored) {
           log.warn("Ignoring line with too few columns: '{}'", line);
           badLines++;
           continue;
         } catch (IllegalArgumentException iae) { // includes NumberFormatException
           if (lines == 1) {
             log.info("Ignoring header line: '{}'", line);
           } else {
             log.warn("Ignoring unparseable line: '{}'", line);
             badLines++;
           }
           continue;
         }
         
         boolean remove = Float.isNaN(value);
         
         if (itemTag != null) {
                   
           if (userTag != null) {
             log.warn("Two tags not allowed: '{}'", line);
             badLines++;
             continue;
           }
 
           if (!remove) {
             setItemTag(itemTag, itemID, value, true);
           }
           // else ignore? no support for remove tag yet
 
         } else if (userTag != null) {
 
           if (!remove) {
             setUserTag(userID, userTag, value, true);
           }
           // else ignore? no support for remove tag yet
 
         } else {
           
           if (remove) {
             removePreference(userID, itemID, true);
           } else {
             setPreference(userID, itemID, value, true);
           }
           
         }
 
         if (lines % 1000000 == 0) {
           log.info("Finished {} lines", lines);
         }
       }
       generationManager.bulkDone();
 
     } catch (IOException ioe) {
       throw new TasteException(ioe);
     }
   }
 
   @Override
   public void close() throws IOException {
     generationManager.close();
     ExecutorService executorService = executor.maybeGet();
     if (executorService != null) {
       ExecutorUtils.shutdownNowAndAwait(executorService);
     }
   }
 
   /**
    * @throws NotReadyException if {@link GenerationManager#getCurrentGeneration()} returns null
    */
   private Generation getCurrentGeneration() throws NotReadyException {
     Generation generation = generationManager.getCurrentGeneration();
     if (generation == null) {
       throw new NotReadyException();
     }
     return generation;
   }
 
   /**
    * Like {@link #recommend(long, int, IDRescorer)} but supplies no rescorer.
    */
   @Override
   public List<RecommendedItem> recommend(long userID, int howMany) throws NoSuchUserException, NotReadyException {
     return recommend(userID, howMany, null);
   }
 
   /**
    * Like {@link #recommend(long, int, boolean, IDRescorer)} and specifies to not consider known items.
    */
   @Override
   public List<RecommendedItem> recommend(long userID, int howMany, IDRescorer rescorer)
       throws NoSuchUserException, NotReadyException {
     return recommend(userID, howMany, false, rescorer);
   }
 
   /**
    * @param userID user for which recommendations are to be computed
    * @param howMany desired number of recommendations
    * @param considerKnownItems if true, items that the user is already associated to are candidates
    *  for recommendation. Normally this is {@code false}.
    * @param rescorer rescoring function used to modify association strengths before ranking results
    * @return {@link List} of recommended {@link RecommendedItem}s, ordered from most strongly recommend to least
    * @throws NoSuchUserException if the user is not known in the model
    * @throws NotReadyException if the recommender has no model available yet
    */
   @Override
   public List<RecommendedItem> recommend(long userID,
                                          int howMany,
                                          boolean considerKnownItems,
                                          IDRescorer rescorer) throws NoSuchUserException, NotReadyException {
     return recommendToMany(new long[] { userID }, howMany,  considerKnownItems, rescorer);
   }
 
   @Override
   public List<RecommendedItem> recommendToMany(long[] userIDs,
                                                int howMany,
                                                boolean considerKnownItems,
                                                IDRescorer rescorer) throws NoSuchUserException, NotReadyException {
 
     Preconditions.checkArgument(howMany > 0, "howMany must be positive");
 
     Generation generation = getCurrentGeneration();
     FastByIDMap<float[]> X = generation.getX();
 
     Lock xLock = generation.getXLock().readLock();
     List<float[]> userFeatures = Lists.newArrayListWithCapacity(userIDs.length);
     xLock.lock();
     try {
       for (long userID : userIDs) {
         float[] theUserFeatures = X.get(userID);
         if (theUserFeatures != null) {
           userFeatures.add(theUserFeatures);
         }
       }
     } finally {
       xLock.unlock();
     }
     if (userFeatures.isEmpty()) {
       throw new NoSuchUserException(Arrays.toString(userIDs));
     }
 
     FastByIDMap<FastIDSet> knownItemIDs = generation.getKnownItemIDs();
     if (knownItemIDs == null && !considerKnownItems) {
       throw new UnsupportedOperationException("Can't ignore known items because no known items available");
     }
     FastIDSet usersKnownItemIDs = null;
     if (!considerKnownItems) {
       Lock knownItemLock = generation.getKnownItemLock().readLock();
       knownItemLock.lock();
       try {
         for (long userID : userIDs) {
           FastIDSet theKnownItemIDs = knownItemIDs.get(userID);
           if (theKnownItemIDs == null) {
             continue;
           }
           if (usersKnownItemIDs == null) {
             usersKnownItemIDs = theKnownItemIDs;
           } else {
             LongPrimitiveIterator it = usersKnownItemIDs.iterator();
             while (it.hasNext()) {
               if (!theKnownItemIDs.contains(it.nextLong())) {
                 it.remove();
               }
             }
           }
           if (usersKnownItemIDs.isEmpty()) {
             break;
           }
         }
       } finally {
         knownItemLock.unlock();
       }
     }
 
     float[][] userFeaturesArray = userFeatures.toArray(new float[userFeatures.size()][]);
     Lock yLock = generation.getYLock().readLock();
     yLock.lock();
     try {
       return multithreadedTopN(userFeaturesArray,
                                usersKnownItemIDs,
                                generation.getUserTagIDs(),
                                rescorer,
                                howMany,
                                generation.getCandidateFilter());
     } finally {
       yLock.unlock();
     }
 
   }
 
   private List<RecommendedItem> multithreadedTopN(final float[][] userFeatures,
                                                   final FastIDSet userKnownItemIDs,
                                                   final FastIDSet userTagIDs,
                                                   final IDRescorer rescorer,
                                                   final int howMany,
                                                   CandidateFilter candidateFilter) {
 
     Collection<Iterator<FastByIDMap.MapEntry<float[]>>> candidateIterators =
         candidateFilter.getCandidateIterator(userFeatures);
 
     int numIterators = candidateIterators.size();
     int parallelism = FastMath.min(numCores, numIterators);
 
     final Queue<MutableRecommendedItem> topN = TopN.initialQueue(howMany);
 
     if (parallelism > 1) {
 
       ExecutorService executorService = executor.get();
 
       final Iterator<Iterator<FastByIDMap.MapEntry<float[]>>> candidateIteratorsIterator =
           candidateIterators.iterator();
 
       List<Future<?>> futures = Lists.newArrayList();
       for (int i = 0; i < numCores; i++) {
         futures.add(executorService.submit(new Callable<Void>() {
           @Override
           public Void call() {
             float[] queueLeastValue = { Float.NEGATIVE_INFINITY };
             while (true) {
               Iterator<FastByIDMap.MapEntry<float[]>> candidateIterator;
               synchronized (candidateIteratorsIterator) {
                 if (!candidateIteratorsIterator.hasNext()) {
                   break;
                 }
                 candidateIterator = candidateIteratorsIterator.next();
               }
               Iterator<RecommendedItem> partialIterator =
                   new RecommendIterator(userFeatures, candidateIterator, userKnownItemIDs, userTagIDs, rescorer);
               TopN.selectTopNIntoQueueMultithreaded(topN, queueLeastValue, partialIterator, howMany);
             }
             return null;
           }
         }));
       }
       for (Future<?> future : futures) {
         try {
           future.get();
         } catch (InterruptedException e) {
           throw new IllegalStateException(e);
         } catch (ExecutionException e) {
           throw new IllegalStateException(e.getCause());
         }
       }
 
     } else {
 
       for (Iterator<FastByIDMap.MapEntry<float[]>> candidateIterator : candidateIterators) {
         Iterator<RecommendedItem> partialIterator =
             new RecommendIterator(userFeatures, candidateIterator, userKnownItemIDs, userTagIDs, rescorer);
         TopN.selectTopNIntoQueue(topN, partialIterator, howMany);
       }
 
     }
 
     return TopN.selectTopNFromQueue(topN, howMany);
   }
 
   @Override
   public List<RecommendedItem> recommendToAnonymous(long[] itemIDs, int howMany)
       throws NotReadyException, NoSuchItemException {
     return recommendToAnonymous(itemIDs, howMany, null);
   }
 
   @Override
   public List<RecommendedItem> recommendToAnonymous(long[] itemIDs, float[] values, int howMany)
       throws NotReadyException, NoSuchItemException {
     return recommendToAnonymous(itemIDs, values, howMany, null);
   }
 
   @Override
   public List<RecommendedItem> recommendToAnonymous(long[] itemIDs, int howMany, IDRescorer rescorer)
       throws NotReadyException, NoSuchItemException {
     return recommendToAnonymous(itemIDs, null, howMany, rescorer);
   }
 
   @Override
   public List<RecommendedItem> recommendToAnonymous(long[] itemIDs,
                                                     float[] values,
                                                     int howMany,
                                                     IDRescorer rescorer)
       throws NotReadyException, NoSuchItemException {
 
     Preconditions.checkArgument(howMany > 0, "howMany must be positive");
 
     float[] anonymousUserFeatures = buildAnonymousUserFeatures(itemIDs, values);
 
     FastIDSet userKnownItemIDs = new FastIDSet(itemIDs.length, 1.25f);
     for (long itemID : itemIDs) {
       userKnownItemIDs.add(itemID);
     }
 
     float[][] anonymousFeaturesAsArray = { anonymousUserFeatures };
 
     Generation generation = getCurrentGeneration();    
     Lock yLock = generation.getYLock().readLock();    
     yLock.lock();
     try {
       return multithreadedTopN(anonymousFeaturesAsArray,
                                userKnownItemIDs,
                                generation.getUserTagIDs(),
                                rescorer,
                                howMany,
                                generation.getCandidateFilter());
     } finally {
       yLock.unlock();
     }
   }
   
   private float[] buildAnonymousUserFeatures(long[] itemIDs, float[] values) 
       throws NotReadyException, NoSuchItemException {
 
     Preconditions.checkArgument(values == null || values.length == itemIDs.length,
                                 "Number of values doesn't match number of items");
     
     Generation generation = getCurrentGeneration();
 
     FastByIDMap<float[]> Y = generation.getY();
     Solver ytySolver = generation.getYTYSolver();
     if (ytySolver == null) {
       throw new NotReadyException();
     }
 
     float[] anonymousUserFeatures = null;
     Lock yLock = generation.getYLock().readLock();
 
     boolean anyItemIDFound = false;
     for (int j = 0; j < itemIDs.length; j++) {
       long itemID = itemIDs[j];
       float[] itemFeatures;
       yLock.lock();
       try {
         itemFeatures = Y.get(itemID);
       } finally {
         yLock.unlock();
       }
       if (itemFeatures == null) {
         continue;
       }
       anyItemIDFound = true;
       double[] userFoldIn = ytySolver.solveFToD(itemFeatures);
       if (anonymousUserFeatures == null) {
         anonymousUserFeatures = new float[userFoldIn.length];
       }
       double signedFoldInWeight = foldInWeight(0.0, values == null ? 1.0f : values[j]);
       if (signedFoldInWeight != 0.0) {
         for (int i = 0; i < anonymousUserFeatures.length; i++) {
           anonymousUserFeatures[i] += (float) (signedFoldInWeight * userFoldIn[i]);
         }
       }
     }
     if (!anyItemIDFound) {
       throw new NoSuchItemException(Arrays.toString(itemIDs));
     }
 
     return anonymousUserFeatures;
   }
 
   @Override
   public List<RecommendedItem> mostPopularItems(int howMany) throws NotReadyException {
     return mostPopularItems(howMany, null);
   }
 
   @Override
   public List<RecommendedItem> mostPopularItems(int howMany, IDRescorer rescorer) throws NotReadyException {
 
     Preconditions.checkArgument(howMany > 0, "howMany must be positive");
 
     Generation generation = getCurrentGeneration();
     FastByIDMap<FastIDSet> knownItemIDs = generation.getKnownItemIDs();
     if (knownItemIDs == null) {
       throw new UnsupportedOperationException();
     }
 
     FastIDSet itemTagIDs = generation.getItemTagIDs(); 
     FastByIDFloatMap itemCounts = new FastByIDFloatMap();    
     Lock knownItemReadLock = generation.getKnownItemLock().readLock();
     knownItemReadLock.lock();
     try {
         // Don't count data from users that are really item tags
         Lock xReadLock = generation.getXLock().readLock();
         xReadLock.lock();
         try {
           
           for (FastByIDMap.MapEntry<FastIDSet> entry : generation.getKnownItemIDs().entrySet()) {
             long userID = entry.getKey();
             if (!itemTagIDs.contains(userID)) {
               FastIDSet itemIDs = entry.getValue();
               synchronized (itemIDs) {
                 LongPrimitiveIterator it = itemIDs.iterator();
                 while (it.hasNext()) {
                   long itemID = it.nextLong();
                   itemCounts.increment(itemID, 1.0f);
                 }
               }
             }
           }
           
         } finally {
           xReadLock.unlock();
         }
     } finally {
       knownItemReadLock.unlock();
     }
     
     // Filter out 'items' that were really user tags
     FastIDSet userTagIDs = generation.getUserTagIDs();
     Lock yReadLock = generation.getYLock().readLock();
     yReadLock.lock();
     try {
       LongPrimitiveIterator it = itemCounts.keySetIterator();
       while (it.hasNext()) {
         if (userTagIDs.contains(it.nextLong())) {
           it.remove();
         }
       }
     } finally {
       yReadLock.unlock();
     }
 
     return TopN.selectTopN(new MostPopularItemsIterator(itemCounts.entrySet().iterator(), rescorer), howMany);
   }
 
   /**
    * @param userID user ID whose preference is to be estimated
    * @param itemID item ID to estimate preference for
    * @return an estimate of the strength of the association between the user and item. These values are the
    *  same as will be returned from {@link #recommend(long, int)}. They are opaque values and have no interpretation
    *  other than that larger means stronger. The values are typically in the range [0,1] but are not guaranteed
    *  to be so. Note that 0 will be returned if the user or item is not known in the data.
    * @throws NotReadyException if the recommender has no model available yet
    */
   @Override
   public float estimatePreference(long userID, long itemID) throws NotReadyException {
     return estimatePreferences(userID, itemID)[0];
   }
 
   @Override
   public float[] estimatePreferences(long userID, long... itemIDs) throws NotReadyException {
     
     Generation generation = getCurrentGeneration();
     FastByIDMap<float[]> X = generation.getX();
     
     float[] userFeatures;
     Lock xLock = generation.getXLock().readLock();
     xLock.lock();
     try {
       userFeatures = X.get(userID);
     } finally {
       xLock.unlock();
     }
     if (userFeatures == null) {
       return new float[itemIDs.length]; // All 0.0f
     }
     
     FastByIDMap<float[]> Y = generation.getY();
 
     Lock yLock = generation.getYLock().readLock();
     yLock.lock();
     try {
       float[] result = new float[itemIDs.length];
       for (int i = 0; i < itemIDs.length; i++) {
         long itemID = itemIDs[i];
         float[] itemFeatures = Y.get(itemID);
         if (itemFeatures != null) {
           float value = (float) SimpleVectorMath.dot(itemFeatures, userFeatures);
           Preconditions.checkState(LangUtils.isFinite(value), "Bad estimate");
           result[i] = value;
         } // else leave value at 0.0f
       }
       return result;
     } finally {
       yLock.unlock();
     }
   }
   
   @Override
   public float estimateForAnonymous(long toItemID, long[] itemIDs) throws NotReadyException, NoSuchItemException {
     return estimateForAnonymous(toItemID, itemIDs, null);
   }
   
   @Override
   public float estimateForAnonymous(long toItemID, long[] itemIDs, float[] values)
       throws NotReadyException, NoSuchItemException {
 
     Generation generation = getCurrentGeneration();    
     FastByIDMap<float[]> Y = generation.getY();
     Lock yLock = generation.getYLock().readLock();
     float[] toItemFeatures;    
     yLock.lock();
     try {
       toItemFeatures = Y.get(toItemID);
     } finally {
       yLock.unlock();
     }
     
     if (toItemFeatures == null) {
       throw new NoSuchItemException(toItemID);
     }
     
     float[] anonymousUserFeatures = buildAnonymousUserFeatures(itemIDs, values);    
     
     return (float) SimpleVectorMath.dot(anonymousUserFeatures, toItemFeatures);
   }
 
   /**
    * Calls {@link #setPreference(long, long, float)} with value 1.0.
    */
   @Override
   public void setPreference(long userID, long itemID) {
     setPreference(userID, itemID, 1.0f);
   }
 
   @Override
   public void setPreference(long userID, long itemID, float value) {
     setPreference(userID, itemID, value, false);
   }
 
   public void setPreference(long userID, long itemID, float value, boolean bulk) {
 
     // Record datum
     try {
       generationManager.append(userID, itemID, value, bulk);
     } catch (IOException ioe) {
       log.warn("Could not append datum; continuing", ioe);
     }
 
     Generation generation;
     try {
       generation = getCurrentGeneration();
     } catch (NotReadyException nre) {
       // Corner case -- no model ready so all we can do is record (above). Don't fail the request.
       return;
     }
 
     float[] userFeatures = getFeatures(userID, generation.getX(), generation.getXLock());
 
     boolean newItem;
     Lock yReadLock = generation.getYLock().readLock();
     yReadLock.lock();
     try {
       newItem = generation.getY().get(itemID) == null;
     } finally {
       yReadLock.unlock();
     }
     if (newItem) {
       generation.getCandidateFilter().addItem(itemID);
     }
     
     float[] itemFeatures = getFeatures(itemID, generation.getY(), generation.getYLock());
 
     updateFeatures(userFeatures, itemFeatures, value, generation);
 
     FastByIDMap<FastIDSet> knownItemIDs = generation.getKnownItemIDs();
     if (knownItemIDs != null) {
       FastIDSet userKnownItemIDs;
       ReadWriteLock knownItemLock = generation.getKnownItemLock();
       Lock knownItemReadLock = knownItemLock.readLock();
       knownItemReadLock.lock();
       try {
         userKnownItemIDs = knownItemIDs.get(userID);
         if (userKnownItemIDs == null) {
           userKnownItemIDs = new FastIDSet();
           Lock knownItemWriteLock = knownItemLock.writeLock();
           knownItemReadLock.unlock();
           knownItemWriteLock.lock();
           try {
             knownItemIDs.put(userID, userKnownItemIDs);
           } finally {
             knownItemReadLock.lock();
             knownItemWriteLock.unlock();
           }
         }
       } finally {
         knownItemReadLock.unlock();
       }
 
       synchronized (userKnownItemIDs) {
         userKnownItemIDs.add(itemID);
       }
     }
     
     updateClusters(userID, userFeatures, generation.getUserClusters(), generation.getUserClustersLock().readLock());
     updateClusters(itemID, itemFeatures, generation.getItemClusters(), generation.getItemClustersLock().readLock());
   }
   
   private static float[] getFeatures(long id, FastByIDMap<float[]> matrix, ReadWriteLock lock) {
     float[] features;
     Lock readLock = lock.readLock();
     readLock.lock();
     try {
       features = matrix.get(id);
       if (features == null) {
         int numFeatures = countFeatures(matrix);
         if (numFeatures > 0) {
           features = new float[numFeatures];
           Lock writeLock = lock.writeLock();
           readLock.unlock();
           writeLock.lock();
           try {
             matrix.put(id, features);
           } finally {
             readLock.lock();
             writeLock.unlock();
           }
         }
       }
     } finally {
       readLock.unlock();
     }
     return features;
   }
   
   private static void updateFeatures(float[] userFeatures, float[] itemFeatures, float value, Generation generation) {
     if (userFeatures == null || itemFeatures == null) {
       return;
     }
     double signedFoldInWeight = foldInWeight(SimpleVectorMath.dot(userFeatures, itemFeatures), value);
     if (signedFoldInWeight == 0.0) {
       return;
     }
     // Here, we are using userFeatures, which is a row of X, as if it were a column of X'.
     // This is multiplied on the left by (X'*X)^-1. That's our left-inverse of X or at least the one
     // column we need. Which is what the new data point is multiplied on the left by. The result is a column;
     // we scale to complete the multiplication of the fold-in and add it in.
     Solver xtxSolver = generation.getXTXSolver();
     double[] itemFoldIn = xtxSolver == null ? null : xtxSolver.solveFToD(userFeatures);
 
     // Same, but reversed. Multiply itemFeatures, which is a row of Y, on the right by (Y'*Y)^-1.
     // This is the right-inverse for Y', or at least the row we need. Because of the symmetries we can use
     // the same method above to carry out the multiply; the result is conceptually a row vector.
     // The result is scaled and added in.
     Solver ytySolver = generation.getYTYSolver();
     double[] userFoldIn = ytySolver == null ? null : ytySolver.solveFToD(itemFeatures);
 
     if (itemFoldIn != null) {
       if (SimpleVectorMath.norm(userFoldIn) > BIG_FOLDIN_THRESHOLD) {
         log.warn("Item fold in vector is large; reduce -Dmodel.features?");
       }
       for (int i = 0; i < itemFeatures.length; i++) {
         double delta = signedFoldInWeight * itemFoldIn[i];
         Preconditions.checkState(LangUtils.isFinite(delta));
         itemFeatures[i] += (float) delta;
       }
     }
     if (userFoldIn != null) {
       if (SimpleVectorMath.norm(userFoldIn) > BIG_FOLDIN_THRESHOLD) {
         log.warn("User fold in vector is large; reduce -Dmodel.features?");
       }
       for (int i = 0; i < userFeatures.length; i++) {
         double delta = signedFoldInWeight * userFoldIn[i];
         Preconditions.checkState(LangUtils.isFinite(delta));
         userFeatures[i] += (float) delta;
       }
     }
   }
   
   private static void updateClusters(long id, 
                                      float[] featureVector, 
                                      Collection<IDCluster> clusters, 
                                      Lock clustersReadLock) {
     if (featureVector == null || clusters == null || clusters.isEmpty()) {
       return;
     } 
     
     IDCluster closestCentroid;
     clustersReadLock.lock();
     try {
       closestCentroid = findClosestCentroid(featureVector, clusters);
     } finally {
       clustersReadLock.unlock();
     }
     
     if (closestCentroid == null) {
       return;
     }
 
     FastIDSet newMembers = closestCentroid.getMembers();
     
     boolean removeFromCurrentCluster;        
     synchronized (newMembers) {
       // Wasn't already present, so was present elsewhere; find and remove it        
       removeFromCurrentCluster = newMembers.add(id);
     }
     
     if (removeFromCurrentCluster) {
       clustersReadLock.lock();
       try {
         for (IDCluster cluster : clusters) {
           FastIDSet oldMembers = cluster.getMembers();
           synchronized (oldMembers) {
             if (oldMembers.remove(id)) {
               break;
             }
           }
         }
       } finally {
         clustersReadLock.unlock();
       }
     }
   }
   
   private static IDCluster findClosestCentroid(float[] vector, Iterable<IDCluster> clusters) {
     double vectorNorm = SimpleVectorMath.norm(vector);
     IDCluster closestCentroid = null;
     double highestDot = Double.NEGATIVE_INFINITY;
     for (IDCluster cluster : clusters) {
       double dot = SimpleVectorMath.dot(cluster.getCentroid(), vector) / cluster.getCentroidNorm() / vectorNorm;
       if (LangUtils.isFinite(dot) && dot > highestDot) {
         highestDot = dot;
         closestCentroid = cluster;
       }
     }
     return closestCentroid;
   }
 
   private static int countFeatures(FastByIDMap<float[]> M) {
     // assumes the read lock is held
     return M.isEmpty() ? 0 : M.entrySet().iterator().next().getValue().length;
   }
 
   /**
    * This function decides how much of a folded-in user or item vector should be added to a target item or user
    * vector, respectively, on a new action. The idea is that a positive value should push the current value towards
    * 1, but not further, and a negative value should push towards 0, but not further. How much to move should be
    * mostly proportional to the size of the value. 0 should move the result not at all; 2 ought to move twice as
    * much as 1, etc. This isn't quite possible but can be approximated by moving a fraction 1-1/(1+value) of the
    * distance towards 1, or 0.
    */
   private static double foldInWeight(double estimate, float value) {
     Preconditions.checkState(LangUtils.isFinite(estimate));
     double signedFoldInWeight;
     if (value > 0.0f && estimate < 1.0) {
       double multiplier = 1.0 - FastMath.max(0.0, estimate);
       signedFoldInWeight = (1.0 - 1.0 / (1.0 + value)) * multiplier;
     } else if (value < 0.0f && estimate > 0.0) {
       double multiplier = -FastMath.min(1.0, estimate);
       signedFoldInWeight = (1.0 - 1.0 / (1.0 - value)) * multiplier;
     } else {
       signedFoldInWeight = 0.0;
     }
     return FOLDIN_LEARN_RATE * signedFoldInWeight;
   }
 
   @Override
   public void removePreference(long userID, long itemID) {
     removePreference(userID, itemID, false);
   }
 
   private void removePreference(long userID, long itemID, boolean bulk) {
 
     // Record datum
     try {
       generationManager.remove(userID, itemID, bulk);
     } catch (IOException ioe) {
       log.warn("Could not append datum; continuing", ioe);
     }
 
     Generation generation;
     try {
       generation = getCurrentGeneration();
     } catch (NotReadyException nre) {
       // Corner case -- no model ready so all we can do is record (above). Don't fail the request.
       return;
     }
 
     ReadWriteLock knownItemLock = generation.getKnownItemLock();
 
     boolean removeUser = false;
     FastByIDMap<FastIDSet> knownItemIDs = generation.getKnownItemIDs();
     if (knownItemIDs != null) {
 
       Lock knownItemReadLock = knownItemLock.readLock();
       FastIDSet userKnownItemIDs;
       knownItemReadLock.lock();
       try {
         userKnownItemIDs = knownItemIDs.get(userID);
       } finally {
         knownItemReadLock.unlock();
       }
 
       if (userKnownItemIDs == null) {
         // Doesn't exist? So ignore this request
         return;
       }
 
       synchronized (userKnownItemIDs) {
         if (!userKnownItemIDs.remove(itemID)) {
           // Item unknown, so ignore this request
           return;
         }
         removeUser = userKnownItemIDs.isEmpty();
       }
     }
 
     // We can proceed with the request
 
     FastByIDMap<float[]> X = generation.getX();
 
     ReadWriteLock xLock = generation.getXLock();
 
     if (removeUser) {
 
       Lock knownItemWriteLock = knownItemLock.writeLock();
       knownItemWriteLock.lock();
       try {
         knownItemIDs.remove(userID);
       } finally {
         knownItemWriteLock.unlock();
       }
 
       Lock xWriteLock = xLock.writeLock();
       xWriteLock.lock();
       try {
         X.remove(userID);
       } finally {
         xWriteLock.unlock();
       }
 
     }
 
   }
   
   @Override
   public void setUserTag(long userID, String tag) {
     setUserTag(userID, tag, 1.0f);    
   }
   
   @Override
   public void setUserTag(long userID, String tag, float value) {
     setUserTag(userID, tag, value, false);
   }
 
   public void setUserTag(long userID, String tag, float value, boolean bulk) {
     Preconditions.checkNotNull(tag);    
     Preconditions.checkArgument(!tag.isEmpty());
     // Record datum
     try {
       generationManager.appendUserTag(userID, tag, value, bulk);
     } catch (IOException ioe) {
       log.warn("Could not append datum; continuing", ioe);
     }
 
     Generation generation;
     try {
       generation = getCurrentGeneration();
     } catch (NotReadyException nre) {
       // Corner case -- no model ready so all we can do is record (above). Don't fail the request.
       return;
     }
     
     long tagID = tagHasher.toLongID(tag);    
     
     FastIDSet userTagIDs = generation.getUserTagIDs();
     Lock userTagWriteLock = generation.getYLock().writeLock();
     userTagWriteLock.lock();
     try {
       userTagIDs.add(tagID);
     } finally {
       userTagWriteLock.unlock();
     }
 
     float[] userFeatures = getFeatures(userID, generation.getX(), generation.getXLock());
     float[] tagFeatures = getFeatures(tagID, generation.getY(), generation.getYLock());
     updateFeatures(userFeatures, tagFeatures, value, generation);
 
     updateClusters(userID, userFeatures, generation.getUserClusters(), generation.getUserClustersLock().readLock());
   }
 
   @Override
   public void setItemTag(String tag, long itemID) {
     setItemTag(tag, itemID, 1.0f);
   }
 
   @Override
   public void setItemTag(String tag, long itemID, float value) {
     setItemTag(tag, itemID, value, false);
   }
   
   public void setItemTag(String tag, long itemID, float value, boolean bulk) {
     Preconditions.checkNotNull(tag);    
     Preconditions.checkArgument(!tag.isEmpty());
     // Record datum
     try {
       generationManager.appendItemTag(tag, itemID, value, bulk);
     } catch (IOException ioe) {
       log.warn("Could not append datum; continuing", ioe);
     }
 
     Generation generation;
     try {
       generation = getCurrentGeneration();
     } catch (NotReadyException nre) {
       // Corner case -- no model ready so all we can do is record (above). Don't fail the request.
       return;
     }
 
     long tagID = tagHasher.toLongID(tag);
     
     FastIDSet itemTagIDs = generation.getItemTagIDs();
     Lock itemTagWriteLock = generation.getXLock().writeLock();
     itemTagWriteLock.lock();
     try {
       itemTagIDs.add(tagID);
     } finally {
       itemTagWriteLock.unlock();
     }
     
     float[] tagFeatures = getFeatures(tagID, generation.getX(), generation.getXLock());
     float[] itemFeatures = getFeatures(itemID, generation.getY(), generation.getYLock());
     updateFeatures(tagFeatures, itemFeatures, value, generation);
 
     updateClusters(itemID, itemFeatures, generation.getItemClusters(), generation.getItemClustersLock().readLock());  
   }
 
   /**
    * One-argument version of {@link #mostSimilarItems(long[], int)}.
    */
   @Override
   public List<RecommendedItem> mostSimilarItems(long itemID, int howMany)
       throws NoSuchItemException, NotReadyException {
     return mostSimilarItems(itemID, howMany, null);
   }
 
   /**
    * One-argument version of {@link #mostSimilarItems(long[], int, Rescorer)}.
    */
   @Override
   public List<RecommendedItem> mostSimilarItems(long itemID, int howMany, Rescorer<LongPair> rescorer)
       throws NoSuchItemException, NotReadyException {
 
     Preconditions.checkArgument(howMany > 0, "howMany must be positive");
 
     Generation generation = getCurrentGeneration();
     FastByIDMap<float[]> Y = generation.getY();
 
     Lock yLock = generation.getYLock().readLock();
     yLock.lock();
     try {
 
       float[] itemFeatures = Y.get(itemID);
       if (itemFeatures == null) {
         throw new NoSuchItemException(itemID);
       }
 
       return TopN.selectTopN(new MostSimilarItemIterator(Y.entrySet().iterator(),
                                                          generation.getUserTagIDs(),
                                                          new long[] { itemID },
                                                          new float[][] { itemFeatures },
                                                          rescorer),
                              howMany);
     } finally {
       yLock.unlock();
     }
 
   }
 
   /**
    * Like {@link #mostSimilarItems(long[], int, Rescorer)} but uses no rescorer.
    */
   @Override
   public List<RecommendedItem> mostSimilarItems(long[] itemIDs, int howMany)
       throws NoSuchItemException, NotReadyException {
     return mostSimilarItems(itemIDs, howMany, null);
   }
 
   /**
    * Computes items most similar to an item or items. The returned items have the highest average similarity
    * to the given items.
    *
    * @param itemIDs items for which most similar items are required
    * @param howMany maximum number of similar items to return; fewer may be returned
    * @param rescorer rescoring function used to modify item-item similarities before ranking results
    * @return {@link RecommendedItem}s representing the top recommendations for the user, ordered by quality,
    *  descending. The score associated to it is an opaque value. Larger means more similar, but no further
    *  interpretation may necessarily be applied.
    * @throws NoSuchItemException if any of the items is not known in the model
    * @throws NotReadyException if the recommender has no model available yet
    */
   @Override
   public List<RecommendedItem> mostSimilarItems(long[] itemIDs, int howMany, Rescorer<LongPair> rescorer)
       throws NoSuchItemException, NotReadyException {
 
     Preconditions.checkArgument(howMany > 0, "howMany must be positive");
 
     Generation generation = getCurrentGeneration();
     FastByIDMap<float[]> Y = generation.getY();
 
     Lock yLock = generation.getYLock().readLock();
     yLock.lock();
     try {
 
       List<float[]> itemFeatures = Lists.newArrayListWithCapacity(itemIDs.length);
       for (long itemID : itemIDs) {
         float[] features = Y.get(itemID);
         if (features != null) {
           itemFeatures.add(features);
         }
       }
       if (itemFeatures.isEmpty()) {
         throw new NoSuchItemException(Arrays.toString(itemIDs));
       }
       float[][] itemFeaturesArray = itemFeatures.toArray(new float[itemFeatures.size()][]);
 
       return TopN.selectTopN(new MostSimilarItemIterator(Y.entrySet().iterator(), 
                                                          generation.getUserTagIDs(),
                                                          itemIDs, 
                                                          itemFeaturesArray, 
                                                          rescorer),
                              howMany);
     } finally {
       yLock.unlock();
     }
   }
 
   @Override
   public float[] similarityToItem(long toItemID, long... itemIDs) throws TasteException {
 
     Generation generation = getCurrentGeneration();
     FastByIDMap<float[]> Y = generation.getY();
 
     float[] similarities = new float[itemIDs.length];
     Lock yLock = generation.getYLock().readLock();
     yLock.lock();
     try {
 
       float[] toFeatures = Y.get(toItemID);
       if (toFeatures == null) {
         throw new NoSuchItemException(toItemID);
       }
       double toFeaturesNorm = SimpleVectorMath.norm(toFeatures);
 
       boolean anyFound = false;
       for (int i = 0; i < similarities.length; i++) {
         float[] features = Y.get(itemIDs[i]);
         if (features == null) {
           similarities[i] = Float.NaN;
         } else {
           anyFound = true;
           double featuresNorm = SimpleVectorMath.norm(features);
           similarities[i] = (float) (SimpleVectorMath.dot(features, toFeatures) / (featuresNorm * toFeaturesNorm));
         }
       }
       if (!anyFound) {
         throw new NoSuchItemException(Arrays.toString(itemIDs));
       }
 
     } finally {
       yLock.unlock();
     }
 
     return similarities;
   }
 
   /**
    * <p>Lists the items that were most influential in recommending a given item to a given user. Exactly how this
    * is determined is left to the implementation, but, generally this will return items that the user prefers
    * and that are similar to the given item.</p>
    *
    * <p>These values by which the results are ordered are opaque values and have no interpretation
    * other than that larger means stronger.</p>
    *
    * @param userID ID of user who was recommended the item
    * @param itemID ID of item that was recommended
    * @param howMany maximum number of items to return
    * @return {@link List} of {@link RecommendedItem}, ordered from most influential in recommended the given
    *  item to least
    * @throws NoSuchUserException if the user is not known in the model
    * @throws NoSuchItemException if the item is not known in the model
    * @throws NotReadyException if the recommender has no model available yet
    */
   @Override
   public List<RecommendedItem> recommendedBecause(long userID, long itemID, int howMany)
       throws NoSuchUserException, NoSuchItemException, NotReadyException {
 
     Preconditions.checkArgument(howMany > 0, "howMany must be positive");
 
     Generation generation = getCurrentGeneration();
     FastByIDMap<FastIDSet> knownItemIDs = generation.getKnownItemIDs();
     if (knownItemIDs == null) {
       throw new UnsupportedOperationException("No known item IDs available");
     }
 
     Lock knownItemLock = generation.getKnownItemLock().readLock();
     FastIDSet userKnownItemIDs;
     knownItemLock.lock();
     try {
       userKnownItemIDs = knownItemIDs.get(userID);
     } finally {
       knownItemLock.unlock();
     }
     if (userKnownItemIDs == null) {
       throw new NoSuchUserException(userID);
     }
 
     FastByIDMap<float[]> Y = generation.getY();
 
     Lock yLock = generation.getYLock().readLock();
     yLock.lock();
     try {
 
       float[] features = Y.get(itemID);
       if (features == null) {
         throw new NoSuchItemException(itemID);
       }
       FastByIDMap<float[]> toFeatures;
       synchronized (userKnownItemIDs) {
         toFeatures = new FastByIDMap<float[]>(userKnownItemIDs.size(), 1.25f);
         LongPrimitiveIterator it = userKnownItemIDs.iterator();
         while (it.hasNext()) {
           long fromItemID = it.nextLong();
           float[] fromFeatures = Y.get(fromItemID);
           toFeatures.put(fromItemID, fromFeatures);
         }
       }
 
       return TopN.selectTopN(new RecommendedBecauseIterator(toFeatures.entrySet().iterator(), 
                                                             generation.getUserTagIDs(), 
                                                             features), 
                              howMany);
     } finally {
       yLock.unlock();
     }
   }
 
   @Override
   public boolean isReady() {
     try {
       getCurrentGeneration();
       return true;
     } catch (NotReadyException ignored) {
       return false;
     }
   }
 
   @Override
   public void await() throws InterruptedException {
     while (!isReady()) {
       Thread.sleep(1000L);
     }
   }
   
   @Override
   public boolean await(long time, TimeUnit unit) throws InterruptedException {
     Preconditions.checkArgument(time >= 0L, "time must be positive: {}", time);
     Preconditions.checkNotNull(unit);
     long waitForMS = TimeUnit.MILLISECONDS.convert(time, unit);
     long waitIntervalMS = FastMath.min(1000L, waitForMS);
     long waitUntil = System.currentTimeMillis() + waitForMS;
     while (!isReady()) {
       if (System.currentTimeMillis() > waitUntil) {
         return false;
       }
       Thread.sleep(waitIntervalMS);
     }
     return true;
   }
 
   @Override
   public FastIDSet getAllUserIDs() throws NotReadyException {
     Generation generation = getCurrentGeneration();
     return getIDsFromKeys(generation.getX(), generation.getXLock().readLock(), generation.getItemTagIDs());
   }
 
   @Override
   public FastIDSet getAllItemIDs() throws NotReadyException {
     Generation generation = getCurrentGeneration();
     return getIDsFromKeys(generation.getY(), generation.getYLock().readLock(), generation.getUserTagIDs());
   }
 
   private static FastIDSet getIDsFromKeys(FastByIDMap<float[]> map, Lock readLock, FastIDSet tagIDs) {
     readLock.lock();
     try {
       FastIDSet ids = new FastIDSet(map.size(), 1.25f);
       LongPrimitiveIterator it = map.keySetIterator();
       while (it.hasNext()) {
         long id = it.nextLong();
         if (!tagIDs.contains(id)) {
           ids.add(id);
         }
       }
       return ids;
     } finally {
       readLock.unlock();
     }
   }
 
   @Override
   public int getNumUserClusters() throws NotReadyException {
     Generation generation = getCurrentGeneration();
     List<IDCluster> clusters = generation.getUserClusters();
     if (clusters == null || clusters.isEmpty()) {
       throw new UnsupportedOperationException();
     }
     Lock lock = generation.getUserClustersLock().readLock();
     lock.lock();
     try {
       return clusters.size();
     } finally {
       lock.unlock();
     }
   }
 
   @Override
   public int getNumItemClusters() throws NotReadyException {
     Generation generation = getCurrentGeneration();
     List<IDCluster> clusters = generation.getItemClusters();
     if (clusters == null || clusters.isEmpty()) {
       throw new UnsupportedOperationException();
     }
     Lock lock = generation.getItemClustersLock().readLock();
     lock.lock();
     try {
       return clusters.size();
     } finally {
       lock.unlock();
     }
   }
 
   @Override
   public FastIDSet getUserCluster(int n) throws NotReadyException {
     Generation generation = getCurrentGeneration();
     List<IDCluster> clusters = generation.getUserClusters();
     if (clusters == null || clusters.isEmpty()) {
       throw new UnsupportedOperationException();
     }
     Lock lock = generation.getUserClustersLock().readLock();
     FastIDSet members;
     lock.lock();
     try {
       members = clusters.get(n).getMembers();
     } finally {
       lock.unlock();
     }
     synchronized (members) {
       return members.clone();
     }
   }
 
   @Override
   public FastIDSet getItemCluster(int n) throws NotReadyException {
     Generation generation = getCurrentGeneration();
     List<IDCluster> clusters = generation.getItemClusters();
     if (clusters == null || clusters.isEmpty()) {
       throw new UnsupportedOperationException();
     }
     Lock lock = generation.getItemClustersLock().readLock();
     FastIDSet members;
     lock.lock();
     try {
       members = clusters.get(n).getMembers();
     } finally {
       lock.unlock();
     }
     synchronized (members) {
       return members.clone();
     }
   }
 
   /**
    * @throws UnsupportedOperationException
    * @deprecated do not call
    */
   @Deprecated
   @Override
   public DataModel getDataModel() {
     throw new UnsupportedOperationException();
   }
 
   /**
    * {@code excludeItemIfNotSimilarToAll} is not applicable in this implementation.
    *
    * @return {@link #mostSimilarItems(long[], int)} if excludeItemIfNotSimilarToAll is false
    * @throws UnsupportedOperationException otherwise
    * @deprecated use {@link #mostSimilarItems(long[], int)} instead
    */
   @Deprecated
   @Override
   public List<RecommendedItem> mostSimilarItems(long[] itemIDs,
                                                 int howMany,
                                                 boolean excludeItemIfNotSimilarToAll)
       throws NoSuchItemException, NotReadyException {
     if (excludeItemIfNotSimilarToAll) {
       throw new UnsupportedOperationException();
     }
     return mostSimilarItems(itemIDs, howMany);
   }
 
   /**
    * {@code excludeItemIfNotSimilarToAll} is not applicable in this implementation.
    *
    * @return {@link #mostSimilarItems(long[], int, Rescorer)} if excludeItemIfNotSimilarToAll is false
    * @throws UnsupportedOperationException otherwise
    * @deprecated use {@link #mostSimilarItems(long[], int, Rescorer)} instead
    */
   @Deprecated
   @Override
   public List<RecommendedItem> mostSimilarItems(long[] itemIDs,
                                                 int howMany,
                                                 Rescorer<LongPair> rescorer,
                                                 boolean excludeItemIfNotSimilarToAll)
       throws NoSuchItemException, NotReadyException {
     if (excludeItemIfNotSimilarToAll) {
       throw new UnsupportedOperationException();
     }
     return mostSimilarItems(itemIDs, howMany);
   }
 
 }
