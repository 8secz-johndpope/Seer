 package de.deepamehta.core.impl;
 
 import de.deepamehta.core.ResultSet;
 import de.deepamehta.core.model.AssociationModel;
 import de.deepamehta.core.model.IndexMode;
 import de.deepamehta.core.model.RelatedAssociationModel;
 import de.deepamehta.core.model.RelatedTopicModel;
 import de.deepamehta.core.model.SimpleValue;
 import de.deepamehta.core.model.TopicModel;
 import de.deepamehta.core.service.accesscontrol.AccessControlList;
 import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
 import de.deepamehta.core.storage.spi.DeepaMehtaStorage;
 import de.deepamehta.core.util.DeepaMehtaUtils;
 
 import org.codehaus.jettison.json.JSONObject;
 
 import static java.util.Arrays.asList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 import java.util.logging.Logger;
 
 
 
 /**
  * A DeepaMehta storage implementation by the means of a MehtaGraph implementation. ### FIXDOC
  * <p>
  * The DeepaMehta service knows nothing about a MehtaGraph and a MehtaGraph knows nothing about DeepaMehta.
  * This class bridges between them. ### FIXDOC
  */
 public class StorageDecorator {
 
     // ---------------------------------------------------------------------------------------------- Instance Variables
 
     private DeepaMehtaStorage storage;
 
     private final Logger logger = Logger.getLogger(getClass().getName());
 
     // ---------------------------------------------------------------------------------------------------- Constructors
 
     public StorageDecorator(DeepaMehtaStorage storage) {
         this.storage = storage;
     }
 
     // -------------------------------------------------------------------------------------------------- Public Methods
 
 
 
     // === Topics ===
 
     /**
      * @return  The fetched topic.
      *          Note: its composite value is not initialized.
      */
     public TopicModel fetchTopic(long topicId) {
         return storage.fetchTopic(topicId);
     }
 
     /**
      * Looks up a single topic by exact property value.
      * If no such topic exists <code>null</code> is returned.
      * If more than one topic were found a runtime exception is thrown.
      * <p>
      * IMPORTANT: Looking up a topic this way requires the property to be indexed with indexing mode <code>KEY</code>.
      * This is achieved by declaring the respective data field with <code>indexing_mode: "KEY"</code>
      * (for statically declared data field, typically in <code>types.json</code>) or
      * by calling DataField's {@link DataField#setIndexingMode} method with <code>"KEY"</code> as argument
      * (for dynamically created data fields, typically in migration classes).
      *
      * @return  The fetched topic.
      *          Note: its composite value is not initialized.
      */
     public TopicModel fetchTopic(String key, SimpleValue value) {
         return storage.fetchTopic(key, value.value());
     }
 
     // ---
 
     /**
      * @return  The fetched topics.
      *          Note: their composite values are not initialized.
      */
     public Set<TopicModel> queryTopics(String searchTerm, String fieldUri) {
         return DeepaMehtaUtils.toTopicSet(storage.queryTopics(fieldUri, searchTerm));
     }
 
     // ---
 
     /**
      * Stores and indexes the topic's URI.
      */
     public void storeTopicUri(long topicId, String uri) {
         storage.storeTopicUri(topicId, uri);
     }
 
     // ---
 
     /**
      * Convenience method.
      */
     public void storeTopicValue(long assocId, SimpleValue value) {
         storeTopicValue(assocId, value, new HashSet(asList(IndexMode.OFF)), null);
     }
 
     /**
      * Stores the topic's value.
      * <p>
      * Note: the value is not indexed automatically. Use the {@link indexTopicValue} method. ### FIXDOC
      *
      * @return  The previous value, or <code>null</code> if no value was stored before. ### FIXDOC
      */
     public void storeTopicValue(long topicId, SimpleValue value, Set<IndexMode> indexModes, String indexKey) {
         storage.storeTopicValue(topicId, value, indexModes, indexKey);
     }
 
     // ---
 
     /**
      * Creates a topic.
      * <p>
      * The topic's URI is stored and indexed.
      *
      * @return  FIXME ### the created topic. Note:
      *          - the topic URI   is initialzed and     persisted.
      *          - the topic value is initialzed but not persisted.
      *          - the type URI    is initialzed but not persisted.
      */
     public void storeTopic(TopicModel topicModel) {
         storage.storeTopic(topicModel);
     }
 
     /**
      * Deletes the topic.
      * <p>
      * Prerequisite: the topic has no relations.
      */
     public void deleteTopic(long topicId) {
         storage.deleteTopic(topicId);
     }
 
 
 
     // --- Traversal ---
 
     /**
      * @return  The fetched associations.
      *          Note: their composite values are not initialized.
      */
     public Set<AssociationModel> fetchTopicAssociations(long topicId) {
         return storage.fetchTopicAssociations(topicId);
     }
 
     // ---
 
     /**
      * Convenience method (checks singularity).
      *
      * @return  The fetched topics.
      *          Note: their composite values are not initialized.
      */
     public RelatedTopicModel fetchTopicRelatedTopic(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                     String othersRoleTypeUri, String othersTopicTypeUri) {
         ResultSet<RelatedTopicModel> topics = fetchTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri,
             othersRoleTypeUri, othersTopicTypeUri, 0);
         switch (topics.getSize()) {
         case 0:
             return null;
         case 1:
             return topics.getIterator().next();
         default:
             throw new RuntimeException("Ambiguity: there are " + topics.getSize() + " related topics (topicId=" +
                 topicId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                 "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")");
         }
     }
 
     /**
      * @return  The fetched topics.
      *          Note: their composite values are not initialized.
      */
     public ResultSet<RelatedTopicModel> fetchTopicRelatedTopics(long topicId, String assocTypeUri,
                                                                 String myRoleTypeUri, String othersRoleTypeUri,
                                                                 String othersTopicTypeUri, int maxResultSize) {
         Set<RelatedTopicModel> relTopics = storage.fetchTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri,
             othersRoleTypeUri, othersTopicTypeUri);
         // ### TODO: respect maxResultSize
         return new ResultSet(relTopics.size(), relTopics);
     }
 
     /**
      * Convenience method (receives *list* of association types).
      *
     * @param   assocTypeUris       may be null
      * @param   myRoleTypeUri       may be null
      * @param   othersRoleTypeUri   may be null
      * @param   othersTopicTypeUri  may be null
      *
      * @return  The fetched topics.
      *          Note: their composite values are not initialized.
      */
     public ResultSet<RelatedTopicModel> fetchTopicRelatedTopics(long topicId, List<String> assocTypeUris,
                                                                 String myRoleTypeUri, String othersRoleTypeUri,
                                                                 String othersTopicTypeUri, int maxResultSize) {
         ResultSet<RelatedTopicModel> result = new ResultSet();
         for (String assocTypeUri : assocTypeUris) {
             ResultSet<RelatedTopicModel> res = fetchTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri,
                 othersRoleTypeUri, othersTopicTypeUri, maxResultSize);
             result.addAll(res);
         }
         return result;
     }
 
     // ---
 
     /**
      * Convenience method (checks singularity).
      *
      * @return  The fetched association.
      *          Note: its composite value is not initialized.
      */
     public RelatedAssociationModel fetchTopicRelatedAssociation(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                                 String othersRoleTypeUri, String othersAssocTypeUri) {
         Set<RelatedAssociationModel> assocs = fetchTopicRelatedAssociations(topicId, assocTypeUri, myRoleTypeUri,
             othersRoleTypeUri, othersAssocTypeUri);
         switch (assocs.size()) {
         case 0:
             return null;
         case 1:
             return assocs.iterator().next();
         default:
             throw new RuntimeException("Ambiguity: there are " + assocs.size() + " related associations (topicId=" +
                 topicId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                 "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersAssocTypeUri=\"" + othersAssocTypeUri + "\")");
         }
     }
 
     /**
      * @param   assocTypeUri        may be null
      * @param   myRoleTypeUri       may be null
      * @param   othersRoleTypeUri   may be null
      * @param   othersAssocTypeUri  may be null
      *
      * @return  The fetched associations.
      *          Note: their composite values are not initialized.
      */
     public Set<RelatedAssociationModel> fetchTopicRelatedAssociations(long topicId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
         return storage.fetchTopicRelatedAssociations(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
             othersAssocTypeUri);
     }
 
 
 
     // === Associations ===
 
     public AssociationModel fetchAssociation(long assocId) {
         return storage.fetchAssociation(assocId);
     }
 
     // ---
 
     /**
      * Convenience method (checks singularity).
      *
      * Returns the association between two topics, qualified by association type and both role types.
      * If no such association exists <code>null</code> is returned.
      * If more than one association exist, a runtime exception is thrown.
      *
      * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
      *                          ### FIXME: for methods with a singular return value all filters should be mandatory
      */
     public AssociationModel fetchAssociation(String assocTypeUri, long topicId1, long topicId2, String roleTypeUri1,
                                                                                                 String roleTypeUri2) {
         Set<AssociationModel> assocs = fetchAssociations(assocTypeUri, topicId1, topicId2, roleTypeUri1, roleTypeUri2);
         switch (assocs.size()) {
         case 0:
             return null;
         case 1:
             return assocs.iterator().next();
         default:
             throw new RuntimeException("Ambiguity: there are " + assocs.size() + " \"" + assocTypeUri +
                 "\" associations (topicId1=" + topicId1 + ", topicId2=" + topicId2 + ", " +
                 "roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\")");
         }
     }
 
     /**
      * Returns the associations between two topics. If no such association exists an empty set is returned.
      *
      * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
      */
     public Set<AssociationModel> fetchAssociations(String assocTypeUri, long topicId1, long topicId2,
                                                                         String roleTypeUri1, String roleTypeUri2) {
         return storage.fetchAssociations(assocTypeUri, topicId1, topicId2, roleTypeUri1, roleTypeUri2);
     }
 
     // ---
 
     /**
      * Convenience method (checks singularity).
      */
     public AssociationModel fetchAssociationBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                                      String topicRoleTypeUri, String assocRoleTypeUri) {
         Set<AssociationModel> assocs = fetchAssociationsBetweenTopicAndAssociation(assocTypeUri, topicId, assocId,
             topicRoleTypeUri, assocRoleTypeUri);
         switch (assocs.size()) {
         case 0:
             return null;
         case 1:
             return assocs.iterator().next();
         default:
             throw new RuntimeException("Ambiguity: there are " + assocs.size() + " \"" + assocTypeUri +
                 "\" associations (topicId=" + topicId + ", assocId=" + assocId + ", " +
                 "topicRoleTypeUri=\"" + topicRoleTypeUri + "\", assocRoleTypeUri=\"" + assocRoleTypeUri + "\")");
         }
     }
 
     public Set<AssociationModel> fetchAssociationsBetweenTopicAndAssociation(String assocTypeUri, long topicId,
                                                        long assocId, String topicRoleTypeUri, String assocRoleTypeUri) {
         return storage.fetchAssociationsBetweenTopicAndAssociation(assocTypeUri, topicId, assocId, topicRoleTypeUri,
             assocRoleTypeUri);
     }
 
     // ---
 
     public void storeRoleTypeUri(long assocId, long playerId, String roleTypeUri) {
         storage.storeRoleTypeUri(assocId, playerId, roleTypeUri);
     }
 
     // ---
 
     /**
      * Stores and indexes the association's URI.
      */
     public void storeAssociationUri(long assocId, String uri) {
         storage.storeAssociationUri(assocId, uri);
     }
 
     // ---
 
     /**
      * Convenience method.
      */
     public void storeAssociationValue(long assocId, SimpleValue value) {
         storeAssociationValue(assocId, value, new HashSet(asList(IndexMode.OFF)), null);
     }
 
     /**
      * Stores the association's value.
      * <p>
      * Note: the value is not indexed automatically. Use the {@link indexAssociationValue} method. ### FIXDOC
      *
      * @return  The previous value, or <code>null</code> if no value was stored before. ### FIXDOC
      */
     public void storeAssociationValue(long assocId, SimpleValue value, Set<IndexMode> indexModes, String indexKey) {
         storage.storeAssociationValue(assocId, value, indexModes, indexKey);
     }
 
     // ---
 
     public void storeAssociation(AssociationModel assocModel) {
         storage.storeAssociation(assocModel);
     }
 
     public void deleteAssociation(long assocId) {
         storage.deleteAssociation(assocId);
     }
 
 
 
     // --- Traversal ---
 
     public Set<AssociationModel> fetchAssociationAssociations(long assocId) {
         return storage.fetchAssociationAssociations(assocId);
     }
 
     // ---
 
     /**
      * Convenience method (checks singularity).
      *
      * @return  The fetched topics.
      *          Note: their composite values are not initialized.
      */
     public RelatedTopicModel fetchAssociationRelatedTopic(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                           String othersRoleTypeUri, String othersTopicTypeUri) {
         ResultSet<RelatedTopicModel> topics = fetchAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
             othersRoleTypeUri, othersTopicTypeUri, 0);
         switch (topics.getSize()) {
         case 0:
             return null;
         case 1:
             return topics.getIterator().next();
         default:
             throw new RuntimeException("Ambiguity: there are " + topics.getSize() + " related topics (assocId=" +
                 assocId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                 "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")");
         }
     }
 
     /**
      * @return  The fetched topics.
      *          Note: their composite values are not initialized.
      */
     public ResultSet<RelatedTopicModel> fetchAssociationRelatedTopics(long assocId, String assocTypeUri,
                                                                       String myRoleTypeUri, String othersRoleTypeUri,
                                                                       String othersTopicTypeUri, int maxResultSize) {
         Set<RelatedTopicModel> relTopics = storage.fetchAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
             othersRoleTypeUri, othersTopicTypeUri);
         // ### TODO: respect maxResultSize
         return new ResultSet(relTopics.size(), relTopics);
     }
 
     /**
      * Convenience method (receives *list* of association types).
      *
      * @param   assocTypeUris       may be null
      * @param   myRoleTypeUri       may be null
      * @param   othersRoleTypeUri   may be null
      * @param   othersTopicTypeUri  may be null
      *
      * @return  The fetched topics.
      *          Note: their composite values are not initialized.
      */
     public ResultSet<RelatedTopicModel> fetchAssociationRelatedTopics(long assocId, List<String> assocTypeUris,
                                                                       String myRoleTypeUri, String othersRoleTypeUri,
                                                                       String othersTopicTypeUri, int maxResultSize) {
         ResultSet<RelatedTopicModel> result = new ResultSet();
         for (String assocTypeUri : assocTypeUris) {
             ResultSet<RelatedTopicModel> res = fetchAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
                 othersRoleTypeUri, othersTopicTypeUri, maxResultSize);
             result.addAll(res);
         }
         return result;
     }
 
     // ---
 
     /**
      * Convenience method (checks singularity).
      *
      * @return  The fetched association.
      *          Note: its composite value is not initialized.
      */
     public RelatedAssociationModel fetchAssociationRelatedAssociation(long assocId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
         Set<RelatedAssociationModel> assocs = fetchAssociationRelatedAssociations(assocId, assocTypeUri, myRoleTypeUri,
             othersRoleTypeUri, othersAssocTypeUri);
         switch (assocs.size()) {
         case 0:
             return null;
         case 1:
             return assocs.iterator().next();
         default:
             throw new RuntimeException("Ambiguity: there are " + assocs.size() + " related associations (assocId=" +
                 assocId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                 "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersAssocTypeUri=\"" + othersAssocTypeUri +
                 "\"),\nresult=" + assocs);
         }
     }
 
     /**
      * @param   assocTypeUri        may be null
      * @param   myRoleTypeUri       may be null
      * @param   othersRoleTypeUri   may be null
      * @param   othersAssocTypeUri  may be null
      *
      * @return  The fetched associations.
      *          Note: their composite values are not initialized.
      */
     public Set<RelatedAssociationModel> fetchAssociationRelatedAssociations(long assocId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
         return storage.fetchAssociationRelatedAssociations(assocId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
             othersAssocTypeUri);
     }
 
 
 
     // === Access Control ===
 
     /**
      * Fetches the Access Control List for the specified topic or association.
      * If no one is stored an empty Access Control List is returned.
      */
     public AccessControlList fetchACL(long objectId) {
         try {
             boolean hasACL = storage.hasProperty(objectId, "acl");
             JSONObject acl = hasACL ? new JSONObject((String) storage.fetchProperty(objectId, "acl"))
                                     : new JSONObject();
             return new AccessControlList(acl);
         } catch (Exception e) {
             throw new RuntimeException("Fetching access control list for object " + objectId + " failed", e);
         }
     }
 
     /**
      * Creates the Access Control List for the specified topic or association.
      */
     public void storeACL(long objectId, AccessControlList acl) {
         storage.storeProperty(objectId, "acl", acl.toJSON().toString());
     }
 
     // ---
 
     public String fetchCreator(long objectId) {
         return storage.hasProperty(objectId, "creator") ? (String) storage.fetchProperty(objectId, "creator") : null;
     }
 
     public void storeCreator(long objectId, String username) {
         storage.storeProperty(objectId, "creator", username);
     }
 
     // ---
 
     public String fetchOwner(long objectId) {
         return storage.hasProperty(objectId, "owner") ? (String) storage.fetchProperty(objectId, "owner") : null;
     }
 
     public void storeOwner(long objectId, String username) {
         storage.storeProperty(objectId, "owner", username);
     }
 
 
 
     // === DB ===
 
     public DeepaMehtaTransaction beginTx() {
         return storage.beginTx();
     }
 
     /**
      * Initializes the database.
      * Prerequisite: there is an open transaction.
      *
      * @return  <code>true</code> if a clean install is detected, <code>false</code> otherwise.
      */
     public boolean init() {
         boolean isCleanInstall = storage.setupRootNode();
         if (isCleanInstall) {
             logger.info("Starting with a fresh DB -- Setting migration number to 0");
             storeMigrationNr(0);
         }
         return isCleanInstall;
     }
 
     public void shutdown() {
         storage.shutdown();
     }
 
     public int fetchMigrationNr() {
         return (Integer) storage.fetchProperty(0, "core_migration_nr");
     }
 
     public void storeMigrationNr(int migrationNr) {
         storage.storeProperty(0, "core_migration_nr", migrationNr);
     }
 }
