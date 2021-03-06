 package de.deepamehta.core.impl.service;
 
 import de.deepamehta.core.Association;
 import de.deepamehta.core.AssociationDefinition;
 import de.deepamehta.core.RelatedTopic;
 import de.deepamehta.core.Topic;
 import de.deepamehta.core.TopicType;
 import de.deepamehta.core.model.Composite;
 import de.deepamehta.core.model.IndexMode;
 import de.deepamehta.core.model.RelatedTopicModel;
 import de.deepamehta.core.model.TopicModel;
 import de.deepamehta.core.model.TopicRoleModel;
 import de.deepamehta.core.model.TopicValue;
 import de.deepamehta.core.util.JavaUtils;
 
 import org.codehaus.jettison.json.JSONObject;
 
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Set;
 import java.util.logging.Logger;
 
 
 
 /**
  * A topic that is attached to the {@link DeepaMehtaService}.
  */
 class AttachedTopic extends AttachedDeepaMehtaObject implements Topic {
 
     // ---------------------------------------------------------------------------------------------- Instance Variables
 
     private Logger logger = Logger.getLogger(getClass().getName());
 
     // ---------------------------------------------------------------------------------------------------- Constructors
 
     AttachedTopic(TopicModel model, EmbeddedService dms) {
         super(model, dms);
     }
 
     // -------------------------------------------------------------------------------------------------- Public Methods
 
 
 
     // ******************************************
     // *** AttachedDeepaMehtaObject Overrides ***
     // ******************************************
 
 
 
     @Override
     public void setUri(String uri) {
         // update memory
         super.setUri(uri);
         // update DB
         storeTopicUri(uri);
     }
 
     @Override
     public void setValue(TopicValue value) {
         // update memory
         super.setValue(value);
         // update DB
         storeTopicValue(value);
     }
 
     @Override
     public void setComposite(Composite comp) {
         // update memory
         super.setComposite(comp);
         // update DB
         storeComposite(comp);
     }
 
 
 
     // ****************************
     // *** Topic Implementation ***
     // ****************************
 
 
 
     // === Traversal ===
 
     @Override
     public TopicValue getChildTopicValue(String assocDefUri) {
         return fetchChildTopicValue(getAssocDef(assocDefUri));
     }
 
     @Override
     public void setChildTopicValue(String assocDefUri, TopicValue value) {
         Composite comp = getComposite();
         // update memory
         comp.put(assocDefUri, value.value());
         // update DB
         storeChildTopicValue(getAssocDef(assocDefUri), value);
         //
         updateTopicValue(comp);
     }
 
     @Override
     public Set<RelatedTopic> getRelatedTopics(String assocTypeUri) {
         Set<RelatedTopic> topics = dms.attach(dms.storage.getTopicRelatedTopics(
             getId(), assocTypeUri, null, null, null), false);   // fetchComposite=false
         //
         /* ### for (RelatedTopic topic : topics) {
             triggerHook(Hook.PROVIDE_TOPIC_PROPERTIES, relTopic.getTopic());
             triggerHook(Hook.PROVIDE_RELATION_PROPERTIES, relTopic.getRelation());
         } */
         //
         return topics;
     }
 
     @Override
     public AttachedRelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                            String othersTopicTypeUri,
                                                                                            boolean fetchComposite) {
         RelatedTopicModel topic = dms.storage.getTopicRelatedTopic(getId(), assocTypeUri, myRoleTypeUri,
             othersRoleTypeUri, othersTopicTypeUri);
         return topic != null ? dms.attach(topic, fetchComposite) : null;
     }
 
     @Override
     public Set<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                          String othersTopicTypeUri,
                                                                                          boolean fetchComposite) {
         return dms.attach(dms.storage.getTopicRelatedTopics(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
             othersTopicTypeUri), fetchComposite);
     }
 
     @Override
     public Set<Association> getAssociations(String myRoleTypeUri) {
         return dms.attach(dms.storage.getAssociations(getId(), myRoleTypeUri));
     }
 
 
 
     // ----------------------------------------------------------------------------------------------- Protected Methods
 
     // ### This is supposed to be protected, but doesn't compile!
     // ### It is called from the subclasses constructors, but on a differnt TopicBase instance.
     // ### See de.deepamehta.core.impl.storage.HGTopic and de.deepamehta.core.impl.service.AttachedTopic.
     public TopicModel getModel() {
         return (TopicModel) super.getModel();
     }
 
     protected final void setModel(TopicModel model) {
         super.setModel(model);
     }
 
 
 
     // ----------------------------------------------------------------------------------------- Package Private Methods
 
     /**
      * Called from {@link EmbeddedService#attach}
      */
     void loadComposite() {
         // fetch from DB
         Composite comp = fetchComposite();
         // update memory
         super.setComposite(comp);
     }
 
     void update(TopicModel topicModel) {
         if (getTopicType().getDataTypeUri().equals("dm3.core.composite")) {
             setComposite(topicModel.getComposite());    // setComposite() includes setValue()
         } else {
             setValue(topicModel.getValue());
         }
         setUri(topicModel.getUri());
     }
 
     TopicType getTopicType() {
         return dms.getTopicType(getTypeUri(), null);    // FIXME: clientContext=null
     }
 
     // ------------------------------------------------------------------------------------------------- Private Methods
 
 
 
     // === Fetch ===
 
     private Composite fetchComposite() {
         try {
             Composite comp = new Composite();
             for (AssociationDefinition assocDef : getTopicType().getAssocDefs().values()) {
                 String assocDefUri = assocDef.getUri();
                 TopicType topicType2 = dms.getTopicType(assocDef.getPartTopicTypeUri(), null);  // clientContext=null
                 if (topicType2.getDataTypeUri().equals("dm3.core.composite")) {
                     AttachedTopic childTopic = fetchChildTopic(assocDef);
                     if (childTopic != null) {
                         comp.put(assocDefUri, childTopic.fetchComposite());
                     }
                 } else {
                     TopicValue value = fetchChildTopicValue(assocDef);
                     if (value != null) {
                         comp.put(assocDefUri, value.value());
                     }
                 }
             }
             return comp;
         } catch (Exception e) {
             throw new RuntimeException("Fetching the topic's composite failed (" + this + ")", e);
         }
     }
 
     private TopicValue fetchChildTopicValue(AssociationDefinition assocDef) {
         Topic childTopic = fetchChildTopic(assocDef);
         if (childTopic != null) {
             return childTopic.getValue();
         }
         return null;
     }
 
 
 
     // === Store ===
 
     private void storeTopicUri(String uri) {
         dms.storage.setTopicUri(getId(), uri);
     }
 
     private void storeTopicValue(TopicValue value) {
         TopicValue oldValue = dms.storage.setTopicValue(getId(), value);
         indexTopicValue(value, oldValue);
     }
 
     // TODO: factorize this method
     private void storeComposite(Composite comp) {
         try {
             Iterator<String> i = comp.keys();
             while (i.hasNext()) {
                 String key = i.next();
                 String[] t = key.split("\\$");
                 //
                 if (t.length < 1 || t.length > 2 || t.length == 2 && !t[1].equals("id")) {
                     throw new RuntimeException("Invalid composite key (\"" + key + "\")");
                 }
                 //
                 String assocDefUri = t[0];
                 AssociationDefinition assocDef = getAssocDef(assocDefUri);
                 TopicType childTopicType = dms.getTopicType(assocDef.getPartTopicTypeUri(), null);
                 String assocTypeUri = assocDef.getAssocTypeUri();
                 Object value = comp.get(key);
                 if (assocTypeUri.equals("dm3.core.composition_def")) {
                     if (childTopicType.getDataTypeUri().equals("dm3.core.composite")) {
                         AttachedTopic childTopic = storeChildTopicValue(assocDef, null);
                         childTopic.storeComposite((Composite) value);
                     } else {
                         storeChildTopicValue(assocDef, new TopicValue(value));
                     }
                 } else if (assocTypeUri.equals("dm3.core.aggregation_def")) {
                     if (childTopicType.getDataTypeUri().equals("dm3.core.composite")) {
                         throw new RuntimeException("Aggregation of composite topic types not yet supported");
                     } else {
                         // remove current assignment
                         RelatedTopic childTopic = fetchChildTopic(assocDef);
                         if (childTopic != null) {
                             long assocId = childTopic.getAssociation().getId();
                             dms.deleteAssociation(assocId, null);  // clientContext=null
                         }
                         //
                         boolean assignExistingTopic = t.length == 2;
                         if (assignExistingTopic) {
                            // update DB
                             long childTopicId = (Integer) value;   // Note: the JSON parser creates Integers (not Longs)
                             associateChildTopic(assocDef, childTopicId);
                            // adjust memory
                            // ### FIXME: ConcurrentModificationException
                            // ### current workaround: refetch after update, see EmbeddedService.updateTopic()
                            // Topic assignedTopic = dms.getTopic(childTopicId, false, null);  // fetchComposite=false
                            // comp.remove(key);
                            // comp.put(assocDefUri, assignedTopic.getValue().value());
                         } else {
                             // create new child topic
                             storeChildTopicValue(assocDef, new TopicValue(value));
                         }
                     }
                 } else {
                     throw new RuntimeException("Association type \"" + assocTypeUri + "\" not supported");
                 }
             }
             //
             updateTopicValue(comp);
         } catch (Exception e) {
             throw new RuntimeException("Storing the topic's composite failed (" + this +
                 ",\ncomposite=" + comp + ")", e);
         }
     }
 
     /**
      * Stores a child's topic value in the database. If the child topic does not exist it is created.
      *
      * @param   assocDefUri     The "axis" that leads to the child: the URI of an {@link AssociationDefinition}.
      * @param   value           The value to set. If <code>null</code> nothing is set. The child topic is potentially
      *                          created and returned anyway.
      *
      * @return  The child topic.
      */
     private AttachedTopic storeChildTopicValue(AssociationDefinition assocDef, final TopicValue value) {
         try {
             AttachedTopic childTopic = fetchChildTopic(assocDef);
             if (childTopic != null) {
                 if (value != null) {
                     childTopic.setValue(value);
                 }
             } else {
                 // create child topic
                 String topicTypeUri = assocDef.getPartTopicTypeUri();
                 childTopic = dms.createTopic(new TopicModel(null, value, topicTypeUri, null), null);
                 // associate child topic
                 associateChildTopic(assocDef, childTopic.getId());
             }
             return childTopic;
         } catch (Exception e) {
             throw new RuntimeException("Storing child topic value failed (parentTopic=" + this +
                 ", assocDef=" + assocDef + ", value=" + value + ")", e);
         }
     }
 
     private void indexTopicValue(TopicValue value, TopicValue oldValue) {
         TopicType topicType = getTopicType();
         String indexKey = topicType.getUri();
         // strip HTML tags before indexing
         if (topicType.getDataTypeUri().equals("dm3.core.html")) {
             value = new TopicValue(JavaUtils.stripHTML(value.toString()));
             if (oldValue != null) {
                 oldValue = new TopicValue(JavaUtils.stripHTML(oldValue.toString()));
             }
         }
         //
         for (IndexMode indexMode : topicType.getIndexModes()) {
             dms.storage.indexTopicValue(getId(), indexMode, indexKey, value, oldValue);
         }
     }
 
 
 
     // === Helper ===
 
     private AttachedRelatedTopic fetchChildTopic(AssociationDefinition assocDef) {
         String assocTypeUri       = assocDef.getInstanceLevelAssocTypeUri();
         String myRoleTypeUri      = assocDef.getWholeRoleTypeUri();
         String othersRoleTypeUri  = assocDef.getPartRoleTypeUri();
         String othersTopicTypeUri = assocDef.getPartTopicTypeUri();
         //
         return getRelatedTopic(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri, true);
         // fetchComposite=true ### false sufficient?
     }
 
     private void associateChildTopic(AssociationDefinition assocDef, long childTopicId) {
         dms.createAssociation(assocDef.getInstanceLevelAssocTypeUri(),
             new TopicRoleModel(getId(), assocDef.getWholeRoleTypeUri()),
             new TopicRoleModel(childTopicId, assocDef.getPartRoleTypeUri()));
     }
 
     // ---
 
     private AssociationDefinition getAssocDef(String assocDefUri) {
         return getTopicType().getAssocDef(assocDefUri);
     }
 
     // ---
 
     private void updateTopicValue(Composite comp) {
         setValue(comp.getLabel());
     }
 }
