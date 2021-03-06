 /*******************************************************************************
  * Copyright (c) 2010-2013 by Min Cai (min.cai.china@gmail.com).
  *
  * This file is part of the Archimulator multicore architectural simulator.
  *
  * Archimulator is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * Archimulator is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Archimulator. If not, see <http://www.gnu.org/licenses/>.
  ******************************************************************************/
 package archimulator.service;
 
 import archimulator.model.*;
 import archimulator.sim.uncore.cache.replacement.CacheReplacementPolicyType;
 import archimulator.sim.uncore.helperThread.HelperThreadingHelper;
 import net.pickapack.service.Service;
 import net.pickapack.util.StorageUnitHelper;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.function.Predicate;
 
 /**
  * Service for managing experiments.
  *
  * @author Min Cai
  */
 public interface ExperimentService extends Service {
     /**
      * Get all experiments.
      *
      * @return all experiments
      */
     List<Experiment> getAllExperiments();
 
     /**
      * Get all experiments by offset and count.
      *
      * @param first offset
      * @param count count
      * @return all experiments by offset and count
      */
     List<Experiment> getAllExperiments(long first, long count);
 
     /**
      * Get the number of all experiments.
      *
      * @return the number of all experiments
      */
     long getNumAllExperiments();
 
     /**
      * Get the number of all experiments in the specified state.
      *
      * @param experimentState the experiment's state
      * @return the number of all experiments in the specified state
      */
     long getNumAllExperimentsByState(ExperimentState experimentState);
 
     /**
      * Get the experiment by id.
      *
      * @param id the experiment's id
      * @return the experiment matching the id if any; otherwise null
      */
     Experiment getExperimentById(long id);
 
     /**
      * Get the list of experiments by title.
      *
      * @param title the experiment's title
      * @return a list of experiments matching the title
      */
     List<Experiment> getExperimentsByTitle(String title);
 
     /**
      * Get the first experiment by title.
      *
      * @param title the experiment's title
      * @return first experiment matching the specified title if any; otherwise null
      */
     Experiment getFirstExperimentByTitle(String title);
 
     /**
      * Get the parent for the specified experiment.
      *
      * @param experiment the experiment
      * @return the parent for the specified experiment
      */
     ExperimentPack getParent(Experiment experiment);
 
     /**
      * Get the first experiment under the specified parent experiment pack.
      *
      * @param parent parent experiment pack
      * @return the first experiment under the specified parent experiment pack
      */
     Experiment getFirstExperimentByParent(ExperimentPack parent);
 
     /**
      * Get the latest experiment by title.
      *
      * @param title the experiment's title
      * @return the latest experiment matching the specified title if any; otherwise null
      */
     Experiment getLatestExperimentByTitle(String title);
 
     /**
      * Get the list of experiments matching the specified benchmark.
      *
      * @param benchmark the benchmark
      * @return the list of experiments matching the specified benchmark
      */
     List<Experiment> getExperimentsByBenchmark(Benchmark benchmark);
 
     /**
      * Get the list of the experiments in the stopped state matching the specified benchmark.
      *
      * @param benchmark the benchmark
      * @return the list of experiments in the stopped state matching the specified benchmark
      */
     List<Experiment> getStoppedExperimentsByBenchmark(Benchmark benchmark);
 
     /**
      * Get the list of experiments by architecture.
      *
      * @param architecture the architecture
      * @return the list of experiments targeting the architecture
      */
     List<Experiment> getExperimentsByArchitecture(Architecture architecture);
 
     /**
      * Get the list of experiments by parent experiment pack.
      *
      * @param parent the parent experiment pack
      * @return the list of experiments under the parent experiment pack
      */
     List<Experiment> getExperimentsByParent(ExperimentPack parent);
 
     /**
      * Get the list of experiments by parent experiment pack, offset and count.
      *
      * @param parent the parent experiment pack
      * @param first  offset
      * @param count  count
      * @return the list of experiments under the parent experiment pack with the specified offset and count
      */
     List<Experiment> getExperimentsByParent(ExperimentPack parent, long first, long count);
 
     /**
      * Add an experiment.
      *
      * @param experiment the experiment
      */
     void addExperiment(Experiment experiment);
 
     /**
      * Remove the experiment by id.
      *
      * @param id the experiment's id
      */
     void removeExperimentById(long id);
 
     /**
      * Update the specified experiment.
      *
      * @param experiment the experiment
      */
     void updateExperiment(Experiment experiment);
 
     /**
      * Get the first experiment to run.
      *
      * @return the first experiment to run if any; otherwise null.
      */
     Experiment getFirstExperimentToRun();
 
     /**
      * Get the list of all the experiments in the stopped state.
      *
      * @return the list of all the experiments in the stopped state
      */
     List<Experiment> getAllStoppedExperiments();
 
     /**
      * Get the list of experiments in the stopped state under the specified parent experiment pack.
      *
      * @param parent parent experiment pack
      * @return the list of experiments in the stopped state under the specified parent experiment pack
      */
     List<Experiment> getStoppedExperimentsByParent(ExperimentPack parent);
 
     /**
      * Get the first experiment in the stopped state under the specified parent experiment pack.
      *
      * @param parent parent experiment pack
      * @return the first experiment in the stopped state under the specified parent experiment pack
      */
     Experiment getFirstStoppedExperimentByParent(ExperimentPack parent);
 
     /**
      * Get all experiment packs.
      *
      * @return all experiment packs
      */
     List<ExperimentPack> getAllExperimentPacks();
 
     /**
      * Get all experiment packs by offset and count.
      *
      * @param first offset
      * @param count count
      * @return all experiment packs by offset and count
      */
     List<ExperimentPack> getAllExperimentPacks(long first, long count);
 
     /**
      * Get the experiment pack by id.
      *
      * @param id the experiment pack's id
      * @return the experiment pack matching the specified id
      */
     ExperimentPack getExperimentPackById(long id);
 
     /**
      * Get the experiment pack by title.
      *
      * @param title the experiment pack's title
      * @return the experiment pack matching the specified title
      */
     ExperimentPack getExperimentPackByTitle(String title);
 
     /**
      * Get the list of experiment packs matching the specified tag.
      *
      * @param tag the tag
      * @return the list of experiment packs matching the specified tag
      */
     List<ExperimentPack> getExperimentPacksByTag(String tag);
 
     /**
      * Get first experiment pack.
      *
      * @return experiment pack
      */
     ExperimentPack getFirstExperimentPack();
 
     /**
      * Add an experiment pack.
      *
      * @param experimentPack the experiment pack to be added
      */
     void addExperimentPack(ExperimentPack experimentPack);
 
     /**
      * Remove the experiment pack by id.
      *
      * @param id the id of the experiment pack which is to be removed
      */
     void removeExperimentPackById(long id);
 
     /**
      * Update the experiment pack.
      *
      * @param experimentPack the experiment pack
      */
     void updateExperimentPack(ExperimentPack experimentPack);
 
     /**
      * Get the number of experiments by the parent experiment pack.
      *
      * @param parent the parent experiment pack
      * @return the number of experiments owned by the parent experiment pack
      */
     long getNumExperimentsByParent(ExperimentPack parent);
 
     /**
      * Get the number of experiments by the parent experiment pack and state.
      *
      * @param parent          the parent experiment pack
      * @param experimentState the experiment's state
      * @return the number of experiments in the specified state and owned by the specified parent experiment pack
      */
     long getNumExperimentsByParentAndState(ExperimentPack parent, ExperimentState experimentState);
 
     /**
      * Start the specified experiment pack.
      *
      * @param experimentPack the experiment pack to be started
      */
     void startExperimentPack(ExperimentPack experimentPack);
 
     /**
      * Stop the specified experiment pack.
      *
      * @param experimentPack the experiment pack to be stopped
      */
     void stopExperimentPack(ExperimentPack experimentPack);
 
     /**
      * Stop the completed experiments owned by the parent experiment pack to the pending state.
      *
      * @param parent the parent experiment pack
      */
     void resetCompletedExperimentsByParent(ExperimentPack parent);
 
     /**
      * Reset the aborted experiments owned by the parent experiment pack to the pending state.
      *
      * @param parent the parent experiment pack
      */
     void resetAbortedExperimentsByParent(ExperimentPack parent);
 
     /**
      * Start the experiment.
      *
      * @param experiment the experiment to be started.
      */
     void startExperiment(Experiment experiment);
 
     /**
      * Start the experiment runner.
      */
     void start();
 
     /**
      * Initialize the service.
      */
     void initialize();
 
     /**
      * Get the corresponding helper thread prefetch coverage baseline experiment for the specified experiment.
      *
      * @param experiment the experiment
      * @return the corresponding helper thread prefetch coverage baseline experiment for the specified experiment
      */
     static Experiment getHelperThreadPrefetchCoverageBaselineExperiment(Experiment experiment) {
         List<Experiment> experimentsByBenchmark = ServiceManager.getExperimentService().getExperimentsByBenchmark(
                 HelperThreadingHelper.getBaselineBenchmark(experiment.getContextMappings().get(0).getBenchmark())
         );
 
         return experimentsByBenchmark.stream().filter(exp -> exp.getArchitecture().getL2Size() == experiment.getArchitecture().getL2Size()
                 && exp.getArchitecture().getL2ReplacementPolicyType() == CacheReplacementPolicyType.LRU
         ).findFirst().get();
     }
 
     /**
      * Get the corresponding speedup baseline experiment for the specified experiment.
      *
      * @return the corresponding speedup baseline experiment for the specified experiment
      */
     static Experiment getSpeedupBaselineExperiment(ExperimentPack experimentPack, Experiment experiment) {
         if(experimentPack == null) {
             return null;
         }
 
         String experimentPackTitle = experimentPack.getTitle();
 
         List<ExperimentPack> baselineExperimentPacks = new ArrayList<>();
 
         Predicate<Experiment> speedupBaselineExperimentPredicate = null;
 
         if (experimentPackTitle.endsWith("_baseline_lru")) {
             baselineExperimentPacks.add(experimentPack);
             speedupBaselineExperimentPredicate = exp -> true;
         } else if (experimentPackTitle.endsWith("_baseline_lru_l2Sizes")) {
             baselineExperimentPacks.add(ServiceManager.getExperimentService().getExperimentPackByTitle(experimentPackTitle.replaceAll("_baseline_lru_l2Sizes", "_baseline_lru")));
             speedupBaselineExperimentPredicate = exp -> exp.getArchitecture().getL2Size() == StorageUnitHelper.displaySizeToByteCount("96 KB");
         } else if (experimentPackTitle.endsWith("_ht_lru")) {
             baselineExperimentPacks.add(ServiceManager.getExperimentService().getExperimentPackByTitle(experimentPackTitle.replaceAll("_ht_lru", "_baseline_lru")));
             speedupBaselineExperimentPredicate = exp -> true;
         } else if (experimentPackTitle.endsWith("_ht_lru_l2Sizes")) {
             baselineExperimentPacks.add(ServiceManager.getExperimentService().getExperimentPackByTitle(experimentPackTitle.replaceAll("_ht_lru_l2Sizes", "_baseline_lru")));
             baselineExperimentPacks.add(ServiceManager.getExperimentService().getExperimentPackByTitle(experimentPackTitle.replaceAll("_ht_lru_l2Sizes", "_baseline_lru_l2Sizes")));
             speedupBaselineExperimentPredicate = exp -> exp.getArchitecture().getL2Size() == experiment.getArchitecture().getL2Size();
         } else if (experimentPackTitle.endsWith("_ht_lru_lookaheads")) {
            baselineExperimentPacks.add(ServiceManager.getExperimentService().getExperimentPackByTitle(experimentPackTitle.replaceAll("_ht_lru_lookaheads", "_baseline_lru")));
            speedupBaselineExperimentPredicate = exp -> true;
         } else if (experimentPackTitle.endsWith("_ht_lru_static_partitioned")) {
             baselineExperimentPacks.add(ServiceManager.getExperimentService().getExperimentPackByTitle(experimentPackTitle.replaceAll("_ht_lru_static_partitioned", "_ht_lru")));
             speedupBaselineExperimentPredicate = exp -> exp.getArchitecture().getL2ReplacementPolicyType() == CacheReplacementPolicyType.LRU;
         } else if (experimentPackTitle.endsWith("_ht_lru_dynamic_partitioned")) {
             baselineExperimentPacks.add(ServiceManager.getExperimentService().getExperimentPackByTitle(experimentPackTitle.replaceAll("_ht_lru_dynamic_partitioned", "_ht_lru")));
             speedupBaselineExperimentPredicate = exp -> exp.getArchitecture().getL2ReplacementPolicyType() == CacheReplacementPolicyType.LRU;
         } else if (experimentPackTitle.endsWith("_ht_l2ReplacementPolicies")) {
             baselineExperimentPacks.add(ServiceManager.getExperimentService().getExperimentPackByTitle(experimentPackTitle.replaceAll("_ht_l2ReplacementPolicies", "_ht_lru")));
             speedupBaselineExperimentPredicate = exp -> exp.getArchitecture().getL2ReplacementPolicyType() == CacheReplacementPolicyType.LRU;
         }
 
         return baselineExperimentPacks.isEmpty() || speedupBaselineExperimentPredicate == null ? null : baselineExperimentPacks.stream().flatMap(expPack -> expPack.getExperiments().stream()).filter(speedupBaselineExperimentPredicate).findFirst().get();
     }
 }
