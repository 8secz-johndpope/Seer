 /*
  * Copyright 2009-2010 by The Regents of the University of California
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * you may obtain a copy of the License from
  * 
  *     http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package edu.uci.ics.algebricks.compiler.algebra.metadata;
 
 import java.util.List;
 
 import edu.uci.ics.algebricks.api.constraints.AlgebricksPartitionConstraint;
 import edu.uci.ics.algebricks.api.data.IPrinterFactory;
 import edu.uci.ics.algebricks.api.exceptions.AlgebricksException;
 import edu.uci.ics.algebricks.compiler.algebra.base.LogicalVariable;
 import edu.uci.ics.algebricks.compiler.algebra.operators.logical.IOperatorSchema;
 import edu.uci.ics.algebricks.runtime.hyracks.base.IPushRuntimeFactory;
 import edu.uci.ics.algebricks.runtime.hyracks.jobgen.impl.JobGenContext;
 import edu.uci.ics.algebricks.utils.Pair;
 import edu.uci.ics.hyracks.api.dataflow.IOperatorDescriptor;
 import edu.uci.ics.hyracks.api.dataflow.value.RecordDescriptor;
 import edu.uci.ics.hyracks.api.job.JobSpecification;
 
 public interface IMetadataProvider<S, I> {
     public IDataSource<S> findDataSource(S id) throws AlgebricksException;
 
     /**
      * Obs: A scanner may choose to contribute a null
      * AlgebricksPartitionConstraint and implement
      * contributeSchedulingConstraints instead.
      */
     public Pair<IOperatorDescriptor, AlgebricksPartitionConstraint> getScannerRuntime(IDataSource<S> dataSource,
             List<LogicalVariable> scanVariables, List<LogicalVariable> projectVariables, boolean projectPushed,
             JobGenContext context, JobSpecification jobSpec) throws AlgebricksException;
 
     public boolean scannerOperatorIsLeaf(IDataSource<S> dataSource);
 
     public Pair<IPushRuntimeFactory, AlgebricksPartitionConstraint> getWriteFileRuntime(IDataSink sink,
             int[] printColumns, IPrinterFactory[] printerFactories, RecordDescriptor inputDesc)
             throws AlgebricksException;
 
     public Pair<IOperatorDescriptor, AlgebricksPartitionConstraint> getWriteResultRuntime(String datasetName,
             IOperatorSchema propagatedSchema, List<LogicalVariable> keys, LogicalVariable payLoadVar,
             JobGenContext context, JobSpecification jobSpec) throws AlgebricksException;
 
     public Pair<IOperatorDescriptor, AlgebricksPartitionConstraint> getInsertRuntime(String datasetName,
             IOperatorSchema propagatedSchema, List<LogicalVariable> keys, LogicalVariable payLoadVar,
             RecordDescriptor recordDesc, JobGenContext context, JobSpecification jobSpec) throws AlgebricksException;
 
     public Pair<IOperatorDescriptor, AlgebricksPartitionConstraint> getDeleteRuntime(String datasetName,
            IOperatorSchema propagatedSchema, List<LogicalVariable> keys, RecordDescriptor recordDesc,
             JobGenContext context, JobSpecification jobSpec) throws AlgebricksException;
 
     public Pair<IOperatorDescriptor, AlgebricksPartitionConstraint> getIndexInsertRuntime(String datasetName,
             String indexName, IOperatorSchema propagatedSchema, List<LogicalVariable> primaryKeys,
             List<LogicalVariable> secondaryKeys, RecordDescriptor recordDesc, JobGenContext context,
             JobSpecification spec) throws AlgebricksException;
 
     public Pair<IOperatorDescriptor, AlgebricksPartitionConstraint> getIndexDeleteRuntime(String datasetName,
             String indexName, IOperatorSchema propagatedSchema, List<LogicalVariable> primaryKeys,
             List<LogicalVariable> secondaryKeys, RecordDescriptor recordDesc, JobGenContext context,
             JobSpecification spec) throws AlgebricksException;
 
     public IDataSourceIndex<I, S> findDataSourceIndex(I indexId, S dataSourceId) throws AlgebricksException;
 }
