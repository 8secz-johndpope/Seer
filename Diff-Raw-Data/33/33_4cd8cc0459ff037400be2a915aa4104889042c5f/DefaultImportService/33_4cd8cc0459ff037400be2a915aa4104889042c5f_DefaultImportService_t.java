 /*******************************************************************************
  * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
  * 
  * This program and the accompanying materials
  * are made available under the terms of the GNU Public License v3.0.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  ******************************************************************************/
 package org.obiba.opal.core.service.impl;
 
 import java.io.IOException;
 import java.lang.reflect.InvocationTargetException;
 import java.util.Set;
 import java.util.TreeSet;
 import java.util.concurrent.ThreadFactory;
 
 import org.apache.commons.vfs.FileObject;
 import org.apache.commons.vfs.FileType;
 import org.obiba.magma.Datasource;
 import org.obiba.magma.MagmaEngine;
 import org.obiba.magma.MagmaRuntimeException;
 import org.obiba.magma.NoSuchDatasourceException;
 import org.obiba.magma.ValueSet;
 import org.obiba.magma.ValueTable;
 import org.obiba.magma.ValueTableWriter;
 import org.obiba.magma.ValueTableWriter.ValueSetWriter;
 import org.obiba.magma.ValueTableWriter.VariableWriter;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableEntity;
 import org.obiba.magma.datasource.crypt.DatasourceEncryptionStrategy;
 import org.obiba.magma.datasource.fs.FsDatasource;
 import org.obiba.magma.lang.Closeables;
 import org.obiba.magma.support.DatasourceCopier;
 import org.obiba.magma.support.DatasourceCopier.DatasourceCopyValueSetEventListener;
 import org.obiba.magma.support.DatasourceCopier.MultiplexingStrategy;
 import org.obiba.magma.support.DatasourceCopier.VariableTransformer;
import org.obiba.magma.support.MagmaEngineReferenceResolver;
import org.obiba.magma.support.MagmaEngineTableResolver;
import org.obiba.magma.support.MultithreadedDatasourceCopier;
 import org.obiba.magma.type.BooleanType;
 import org.obiba.magma.type.TextType;
 import org.obiba.magma.views.SelectClause;
 import org.obiba.magma.views.View;
 import org.obiba.opal.core.domain.participant.identifier.IParticipantIdentifier;
 import org.obiba.opal.core.magma.FunctionalUnitView;
 import org.obiba.opal.core.magma.FunctionalUnitView.Policy;
import org.obiba.opal.core.magma.PrivateVariableEntityMap;
 import org.obiba.opal.core.magma.concurrent.LockingActionTemplate;
 import org.obiba.opal.core.runtime.OpalRuntime;
 import org.obiba.opal.core.service.ImportService;
 import org.obiba.opal.core.service.NoSuchFunctionalUnitException;
 import org.obiba.opal.core.unit.FunctionalUnit;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.transaction.TransactionStatus;
 import org.springframework.transaction.support.TransactionCallbackWithoutResult;
 import org.springframework.transaction.support.TransactionTemplate;
 import org.springframework.util.Assert;
 
 import com.google.common.collect.Sets;
 
 /**
  * Default implementation of {@link ImportService}.
  */
 public class DefaultImportService implements ImportService {
   //
   // Constants
   //
 
   public static final String STAGE_ATTRIBUTE_NAME = "stage";
 
   @SuppressWarnings("unused")
   private static final Logger log = LoggerFactory.getLogger(DefaultImportService.class);
 
   //
   // Instance Variables
   //
 
   private final TransactionTemplate txTemplate;
 
   private final OpalRuntime opalRuntime;
 
   private final IParticipantIdentifier participantIdentifier;
 
   /** Configured through org.obiba.opal.keys.tableReference */
   private final String keysTableReference;
 
   /** Configured through org.obiba.opal.keys.entityType */
   private final String keysTableEntityType;
 
   @Autowired
   public DefaultImportService(TransactionTemplate txTemplate, OpalRuntime opalRuntime, IParticipantIdentifier participantIdentifier, @Value("${org.obiba.opal.keys.tableReference}") String keysTableReference, @Value("${org.obiba.opal.keys.entityType}") String keysTableEntityType) {
     if(txTemplate == null) throw new IllegalArgumentException("txManager cannot be null");
     if(opalRuntime == null) throw new IllegalArgumentException("opalRuntime cannot be null");
     if(participantIdentifier == null) throw new IllegalArgumentException("participantIdentifier cannot be null");
     if(keysTableReference == null) throw new IllegalArgumentException("keysTableReference cannot be null");
     if(keysTableEntityType == null) throw new IllegalArgumentException("keysTableEntityType cannot be null");
 
     this.opalRuntime = opalRuntime;
     this.participantIdentifier = participantIdentifier;
     this.keysTableReference = keysTableReference;
     this.keysTableEntityType = keysTableEntityType;
 
     this.txTemplate = txTemplate;
     this.txTemplate.setIsolationLevel(TransactionTemplate.ISOLATION_READ_COMMITTED);
   }
 
   //
   // ImportService Methods
   //
 
   @Override
   public void importData(String unitName, String datasourceName, FileObject file) throws NoSuchFunctionalUnitException, NoSuchDatasourceException, IllegalArgumentException, IOException, InterruptedException {
     // OPAL-170 Dispatch the variables in tables corresponding to Onyx stage attribute value.
     importData(unitName, datasourceName, file, STAGE_ATTRIBUTE_NAME);
   }
 
   private void importData(String unitName, String datasourceName, FileObject file, String dispatchAttribute) throws NoSuchFunctionalUnitException, NoSuchDatasourceException, IllegalArgumentException, IOException, InterruptedException {
     // If unitName is the empty string, coerce it to null.
     String nonEmptyUnitName = (unitName != null && unitName.equals("")) ? null : unitName;
 
     if(nonEmptyUnitName != null) Assert.isTrue(!nonEmptyUnitName.equals(FunctionalUnit.OPAL_INSTANCE), "unitName cannot be " + FunctionalUnit.OPAL_INSTANCE);
     Assert.hasText(datasourceName, "datasourceName is null or empty");
     Assert.notNull(file, "file is null");
     Assert.isTrue(file.getType() == FileType.FILE, "No such file (" + file.getName().getPath() + ")");
 
     // Validate the datasource name.
     Datasource destinationDatasource = MagmaEngine.get().getDatasource(datasourceName);
 
     FunctionalUnit unit = null;
     if(nonEmptyUnitName != null) {
       unit = opalRuntime.getFunctionalUnit(nonEmptyUnitName);
       if(unit == null) {
         throw new NoSuchFunctionalUnitException(nonEmptyUnitName);
       }
     }
 
     copyToDestinationDatasource(file, dispatchAttribute, destinationDatasource, unit);
   }
 
   @Override
   public void importData(String unitName, String sourceDatasourceName, String destinationDatasourceName) throws NoSuchFunctionalUnitException, IOException, InterruptedException {
     // If unitName is the empty string, coerce it to null.
     String nonEmptyUnitName = (unitName != null && unitName.equals("")) ? null : unitName;
 
     if(nonEmptyUnitName != null) Assert.isTrue(!nonEmptyUnitName.equals(FunctionalUnit.OPAL_INSTANCE), "unitName cannot be " + FunctionalUnit.OPAL_INSTANCE);
     Assert.hasText(sourceDatasourceName, "sourceDatasourceName is null or empty");
     Assert.hasText(destinationDatasourceName, "destinationDatasourceName is null or empty");
 
     Datasource sourceDatasource = getDatasourceOrTransientDatasource(sourceDatasourceName);
     Datasource destinationDatasource = MagmaEngine.get().getDatasource(destinationDatasourceName);
 
     FunctionalUnit unit = null;
     if(nonEmptyUnitName != null) {
       unit = opalRuntime.getFunctionalUnit(nonEmptyUnitName);
       if(unit == null) {
         throw new NoSuchFunctionalUnitException(nonEmptyUnitName);
       }
     }
 
     try {
       sourceDatasource.initialise();
       copyValueTables(sourceDatasource, destinationDatasource, unit, STAGE_ATTRIBUTE_NAME);
     } finally {
       sourceDatasource.dispose();
     }
   }
 
   @Override
   public void importIdentifiers(String unitName, String sourceDatasourceName) throws IOException {
     Assert.hasText(unitName, "unitName is null or empty");
     Assert.hasText(sourceDatasourceName, "sourceDatasourceName is null or empty");
 
     FunctionalUnit unit = opalRuntime.getFunctionalUnit(unitName);
     if(unit == null) {
       throw new NoSuchFunctionalUnitException(unitName);
     }
 
     Datasource sourceDatasource = getDatasourceOrTransientDatasource(sourceDatasourceName);
 
     for(ValueTable vt : sourceDatasource.getValueTables()) {
       if(vt.getEntityType().equals(keysTableEntityType)) {
        ValueTable sourceKeysTable = createPrivateView(vt, unit);
        Variable unitKeyVariable = prepareKeysTable(sourceKeysTable, unit.getKeyVariableName());
        PrivateVariableEntityMap entityMap = new OpalPrivateVariableEntityMap(lookupKeysTable(), unitKeyVariable, participantIdentifier);

        for(VariableEntity privateEntity : sourceKeysTable.getVariableEntities()) {
          VariableEntity publicEntity = entityMap.publicEntity(privateEntity);
          if(publicEntity == null) {
            publicEntity = entityMap.createPublicEntity(privateEntity);
          }
          copyParticipantIdentifiers(entityMap.publicEntity(privateEntity), sourceKeysTable, unitKeyVariable, writeToKeysTable(), entityMap);
        }
       }
     }
   }
 
   @Override
   public void importIdentifiers(String sourceDatasourceName) throws IOException {
     Assert.hasText(sourceDatasourceName, "sourceDatasourceName is null or empty");
 
     Datasource sourceDatasource = getDatasourceOrTransientDatasource(sourceDatasourceName);
     ValueTable sourceKeysTable = sourceDatasource.getValueTable(getKeysTableName());
 
     if(sourceKeysTable.getEntityType().equals(keysTableEntityType) == false) {
       throw new IllegalArgumentException("source identifiers table has unexpected entity type '" + sourceKeysTable.getEntityType() + "' (expected '" + keysTableEntityType + "')");
     }
 
     DatasourceCopier.Builder.newCopier().dontCopyNullValues().withLoggingListener().build().copy(sourceKeysTable, MagmaEngine.get().getDatasource(getKeysDatasourceName()));
   }
 
   private String getKeysDatasourceName() {
     MagmaEngineReferenceResolver tableResolver = MagmaEngineTableResolver.valueOf(keysTableReference);
     return tableResolver.getDatasourceName();
   }
 
   private String getKeysTableName() {
     MagmaEngineReferenceResolver tableResolver = MagmaEngineTableResolver.valueOf(keysTableReference);
     return tableResolver.getTableName();
   }
 
   private Datasource getDatasourceOrTransientDatasource(String datasourceName) {
     if(MagmaEngine.get().hasDatasource(datasourceName)) {
       return MagmaEngine.get().getDatasource(datasourceName);
     } else {
       return MagmaEngine.get().getTransientDatasourceInstance(datasourceName);
     }
   }
 
   private void copyToDestinationDatasource(FileObject file, String dispatchAttribute, Datasource destinationDatasource, FunctionalUnit unit) throws IOException, InterruptedException {
     DatasourceEncryptionStrategy datasourceEncryptionStrategy = null;
     if(unit != null) datasourceEncryptionStrategy = unit.getDatasourceEncryptionStrategy();
     FsDatasource sourceDatasource = new FsDatasource(file.getName().getBaseName(), opalRuntime.getFileSystem().getLocalFile(file), datasourceEncryptionStrategy);
 
     try {
       sourceDatasource.initialise();
       copyValueTables(sourceDatasource, destinationDatasource, unit, dispatchAttribute);
     } finally {
       sourceDatasource.dispose();
     }
   }
 
   private void copyValueTables(final Datasource source, final Datasource destination, final FunctionalUnit unit, final String dispatchAttribute) throws IOException, InterruptedException {
     try {
       new LockingActionTemplate() {
 
         @Override
         protected Set<String> getLockNames() {
           return getTablesToLock(source);
         }
 
         @Override
         protected TransactionTemplate getTransactionTemplate() {
           return txTemplate;
         }
 
         @Override
         protected Action getAction() {
           return new Action() {
             public void execute() throws Exception {
               for(ValueTable valueTable : source.getValueTables()) {
                 if(Thread.interrupted()) {
                   throw new InterruptedException("Thread interrupted");
                 }
 
                 if(valueTable.isForEntityType(keysTableEntityType)) {
                   if(unit != null) {
                     copyParticipants(valueTable, source, destination, unit, dispatchAttribute);
                   } else {
                     addMissingEntitiesToKeysTable(valueTable);
                     MultithreadedDatasourceCopier.Builder.newCopier().withThreads(new ThreadFactory() {
                       @Override
                       public Thread newThread(Runnable r) {
                         return new TransactionalThread(r);
                       }
                     }).withCopier(newCopierForParticipants(dispatchAttribute, valueTable)).from(valueTable).to(destination).build().copy();
                   }
                 } else {
                   DatasourceCopier.Builder.newCopier().dontCopyNullValues().withLoggingListener().build().copy(valueTable, destination);
                 }
               }
             }
           };
         }
       }.execute();
     } catch(InvocationTargetException ex) {
       if(ex.getCause() instanceof IOException) {
         throw (IOException) (ex.getCause());
       } else if(ex.getCause() instanceof InterruptedException) {
         throw (InterruptedException) (ex.getCause());
       } else {
         throw new RuntimeException(ex.getCause());
       }
     }
   }
 
   private Set<VariableEntity> addMissingEntitiesToKeysTable(ValueTable valueTable) {
     Set<VariableEntity> nonExistentVariableEntities = Sets.newHashSet(valueTable.getVariableEntities());
 
     MagmaEngineReferenceResolver tableResolver = MagmaEngineTableResolver.valueOf(keysTableReference);
     if(MagmaEngine.get().getDatasource(tableResolver.getDatasourceName()).hasValueTable(tableResolver.getTableName())) {
       // Remove all entities that exist in the keys table. Whatever is left are the ones that don't exist...
       Set<VariableEntity> entitiesInKeysTable = lookupKeysTable().getVariableEntities();
       nonExistentVariableEntities.removeAll(entitiesInKeysTable);
     }
 
     if(nonExistentVariableEntities.size() > 0) {
       ValueTableWriter keysTableWriter = writeToKeysTable();
       try {
         for(VariableEntity ve : nonExistentVariableEntities) {
           keysTableWriter.writeValueSet(ve).close();
         }
       } catch(IOException e) {
         throw new RuntimeException(e);
       } finally {
         Closeables.closeQuietly(keysTableWriter);
       }
     }
 
     return nonExistentVariableEntities;
   }
 
   private Set<String> getTablesToLock(Datasource source) {
     Set<String> tablesToLock = new TreeSet<String>();
 
     boolean needToLockKeysTable = false;
 
     for(ValueTable valueTable : source.getValueTables()) {
       tablesToLock.add(valueTable.getDatasource() + "." + valueTable.getName());
       if(valueTable.getEntityType().equals(keysTableEntityType)) {
         needToLockKeysTable = true;
       }
     }
 
     if(needToLockKeysTable) {
       tablesToLock.add(keysTableReference);
     }
 
     return tablesToLock;
   }
 
   private void copyParticipants(ValueTable participantTable, Datasource source, Datasource destination, FunctionalUnit unit, final String dispatchAttribute) throws IOException {
     final String keyVariableName = unit.getKeyVariableName();
     final View privateView = createPrivateView(participantTable, unit);
     final Variable keyVariable = prepareKeysTable(privateView, keyVariableName);
 
     final FunctionalUnitView publicView = createPublicView(participantTable, unit);
     final PrivateVariableEntityMap entityMap = publicView.getPrivateVariableEntityMap();
 
     // prepare for copying participant data
     final ValueTableWriter keysTableWriter = writeToKeysTable();
 
     try {
       copyPublicViewToDestinationDatasource(destination, dispatchAttribute, publicView, createKeysListener(privateView, keyVariable, entityMap, keysTableWriter));
     } finally {
       keysTableWriter.close();
     }
   }
 
   /**
    * This listener will insert all participant identifiers in the keys datasource prior to copying the valueset to the
    * data datasource. It will also generate the public variable entity if it does not exist yet. As such, it must be
    * executed before the ValueSet is copied to the data datasource otherwise, it will not have an associated entity.
    */
   private DatasourceCopyValueSetEventListener createKeysListener(final View privateView, final Variable keyVariable, final PrivateVariableEntityMap entityMap, final ValueTableWriter keysTableWriter) {
     DatasourceCopyValueSetEventListener createKeysListener = new DatasourceCopyValueSetEventListener() {
 
       public void onValueSetCopied(ValueTable source, ValueSet valueSet, String... destination) {
       }
 
       public void onValueSetCopy(ValueTable source, ValueSet valueSet) {
         copyParticipantIdentifiers(valueSet.getVariableEntity(), privateView, keyVariable, keysTableWriter, entityMap);
       }
 
     };
     return createKeysListener;
   }
 
   private DatasourceCopier.Builder newCopierForParticipants(final String dispatchAttribute, final ValueTable sourceTable) {
     return DatasourceCopier.Builder.newCopier() //
     .withLoggingListener().withThroughtputListener() //
     .withMultiplexingStrategy(new VariableAttributeMutiplexingStrategy(dispatchAttribute, sourceTable.getName()))//
     .withVariableTransformer(new VariableTransformer() {
       /** Remove the dispatch attribute from the variable name. This is onyx-specific. See OPAL-170 */
       public Variable transform(Variable variable) {
         return Variable.Builder.sameAs(variable).name(variable.hasAttribute(dispatchAttribute) ? variable.getName().replaceFirst("^.*\\.?" + variable.getAttributeStringValue(dispatchAttribute) + "\\.", "") : variable.getName()).build();
       }
     });
   }
 
   private void copyPublicViewToDestinationDatasource(Datasource destination, final String dispatchAttribute, FunctionalUnitView publicView, DatasourceCopyValueSetEventListener createKeysListener) throws IOException {
     newCopierForParticipants(dispatchAttribute, publicView) //
     .withListener(createKeysListener).build()
     // Copy participant's non-identifiable variables and data
     .copy(publicView, destination);
   }
 
   /**
    * Creates a {@link View} of the participant table's "private" variables (i.e., identifiers).
    * 
    * @param viewName
    * @param participantTable
    * @return
    */
   private View createPrivateView(String viewName, ValueTable participantTable, FunctionalUnit unit) {
     if(unit.getSelect() != null) {
       final View privateView = View.Builder.newView(viewName, participantTable).select(unit.getSelect()).build();
       privateView.initialise();
       return privateView;
     } else {
       final View privateView = View.Builder.newView(viewName, participantTable).select(new SelectClause() {
         public boolean select(Variable variable) {
           return isIdentifierVariable(variable);
         }
       }).build();
       return privateView;
     }
   }
 
   private View createPrivateView(ValueTable participantTable, FunctionalUnit unit) {
     return createPrivateView(participantTable.getName(), participantTable, unit);
   }
 
   /**
    * Wraps the participant table in a {@link View} that exposes public entities and non-identifier variables.
    * 
    * @param participantTable
    * @param entityMap
    * @return
    */
   private FunctionalUnitView createPublicView(ValueTable participantTable, final FunctionalUnit unit) {
     FunctionalUnitView publicTable = new FunctionalUnitView(unit, Policy.UNIT_IDENTIFIERS_ARE_PRIVATE, participantTable, lookupKeysTable(), participantIdentifier);
     publicTable.setSelectClause(new SelectClause() {
 
       public boolean select(Variable variable) {
         return isIdentifierVariable(variable) == false && isIdentifierVariableForUnit(variable, unit) == false;
       }
 
     });
     publicTable.initialise();
     return publicTable;
   }
 
   /**
    * Write the key variable.
    * @param privateView
    * @param keyVariableName
    * @return
    * @throws IOException
    */
   private Variable prepareKeysTable(ValueTable privateView, String keyVariableName) throws IOException {
 
     Variable keyVariable = Variable.Builder.newVariable(keyVariableName, TextType.get(), privateView.getEntityType()).build();
 
     ValueTableWriter writer = writeToKeysTable();
     try {
       VariableWriter vw = writer.writeVariables();
       try {
         // Create private variables
         vw.writeVariable(keyVariable);
         DatasourceCopier.Builder.newCopier().dontCopyValues().build().copy(privateView, lookupKeysTable().getName(), vw);
       } finally {
         vw.close();
       }
     } finally {
       writer.close();
     }
     return keyVariable;
   }
 
   /**
    * Write the key variable and the identifier variables values; update the participant key private/public map.
    */
   private VariableEntity copyParticipantIdentifiers(VariableEntity publicEntity, ValueTable privateView, Variable keyVariable, ValueTableWriter writer, PrivateVariableEntityMap entityMap) {
     VariableEntity privateEntity = entityMap.privateEntity(publicEntity);
 
     ValueSetWriter vsw = writer.writeValueSet(publicEntity);
     try {
       // Copy all other private variable values
       DatasourceCopier.Builder.newCopier().dontCopyMetadata().build().copy(privateView, privateView.getValueSet(privateEntity), lookupKeysTable().getName(), vsw);
     } finally {
       try {
         vsw.close();
       } catch(IOException e) {
         throw new MagmaRuntimeException(e);
       }
     }
     return publicEntity;
   }
 
   private ValueTable lookupKeysTable() {
     return MagmaEngineTableResolver.valueOf(keysTableReference).resolveTable();
   }
 
   private ValueTableWriter writeToKeysTable() {
     MagmaEngineTableResolver resolver = MagmaEngineTableResolver.valueOf(keysTableReference);
     return MagmaEngine.get().getDatasource(resolver.getDatasourceName()).createWriter(resolver.getTableName(), keysTableEntityType);
   }
 
   private boolean isIdentifierVariable(Variable variable) {
     return variable.hasAttribute("identifier") && (variable.getAttribute("identifier").getValue().equals(BooleanType.get().trueValue()) || variable.getAttribute("identifier").getValue().equals(TextType.get().valueOf("true")));
   }
 
   private boolean isIdentifierVariableForUnit(Variable variable, FunctionalUnit unit) {
     return (unit.getSelect() != null && unit.getSelect().select(variable));
   }
 
   /**
    * A MultiplexingStrategy that uses a variable attribute as the destination table name
    */
   static private class VariableAttributeMutiplexingStrategy implements MultiplexingStrategy {
 
     private final String attributeName;
 
     private final String defaultName;
 
     public VariableAttributeMutiplexingStrategy(String attributeName, String defaultName) {
       this.attributeName = attributeName;
       this.defaultName = defaultName;
     }
 
     public String multiplexVariable(Variable variable) {
       return variable.hasAttribute(attributeName) ? variable.getAttributeStringValue(attributeName) : defaultName;
     }
 
     public String multiplexValueSet(VariableEntity entity, Variable variable) {
       return multiplexVariable(variable);
     }
   }
 
   class TransactionalThread extends Thread {
 
     private Runnable runnable;
 
     public TransactionalThread(Runnable runnable) {
       this.runnable = runnable;
     }
 
     public void run() {
       txTemplate.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
           runnable.run();
         }
       });
     }
   }
 }
