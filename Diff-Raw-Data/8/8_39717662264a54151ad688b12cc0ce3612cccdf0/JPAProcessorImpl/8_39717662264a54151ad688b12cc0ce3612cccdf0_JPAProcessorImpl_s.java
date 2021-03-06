 package com.sap.core.odata.processor.core.jpa.access.data;
 
 import java.io.InputStream;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 
 import javax.persistence.EntityManager;
 import javax.persistence.Query;
 
 import com.sap.core.odata.api.edm.EdmEntitySet;
 import com.sap.core.odata.api.edm.EdmEntityType;
 import com.sap.core.odata.api.edm.EdmException;
 import com.sap.core.odata.api.edm.EdmMultiplicity;
 import com.sap.core.odata.api.ep.entry.ODataEntry;
 import com.sap.core.odata.api.uri.info.DeleteUriInfo;
 import com.sap.core.odata.api.uri.info.GetEntityCountUriInfo;
 import com.sap.core.odata.api.uri.info.GetEntityLinkUriInfo;
 import com.sap.core.odata.api.uri.info.GetEntitySetCountUriInfo;
 import com.sap.core.odata.api.uri.info.GetEntitySetLinksUriInfo;
 import com.sap.core.odata.api.uri.info.GetEntitySetUriInfo;
 import com.sap.core.odata.api.uri.info.GetEntityUriInfo;
 import com.sap.core.odata.api.uri.info.GetFunctionImportUriInfo;
 import com.sap.core.odata.api.uri.info.PostUriInfo;
 import com.sap.core.odata.api.uri.info.PutMergePatchUriInfo;
 import com.sap.core.odata.processor.api.jpa.ODataJPAContext;
 import com.sap.core.odata.processor.api.jpa.access.JPAFunction;
 import com.sap.core.odata.processor.api.jpa.access.JPAMethodContext;
 import com.sap.core.odata.processor.api.jpa.access.JPAProcessor;
 import com.sap.core.odata.processor.api.jpa.exception.ODataJPAModelException;
 import com.sap.core.odata.processor.api.jpa.exception.ODataJPARuntimeException;
 import com.sap.core.odata.processor.api.jpa.jpql.JPQLContext;
 import com.sap.core.odata.processor.api.jpa.jpql.JPQLContextType;
 import com.sap.core.odata.processor.api.jpa.jpql.JPQLStatement;
 import com.sap.core.odata.processor.core.jpa.ODataEntityParser;
 
 public class JPAProcessorImpl implements JPAProcessor {
 
   ODataJPAContext oDataJPAContext;
   EntityManager em;
 
   public JPAProcessorImpl(final ODataJPAContext oDataJPAContext) {
     this.oDataJPAContext = oDataJPAContext;
     em = oDataJPAContext.getEntityManager();
   }
 
   /* Process Function Import Request */
   @SuppressWarnings("unchecked")
   @Override
   public List<Object> process(final GetFunctionImportUriInfo uriParserResultView)
       throws ODataJPAModelException, ODataJPARuntimeException {
 
     JPAMethodContext jpaMethodContext = JPAMethodContext.createBuilder(
         JPQLContextType.FUNCTION, uriParserResultView).build();
 
     List<Object> resultObj = null;
 
     try {
 
       JPAFunction jpaFunction = jpaMethodContext.getJPAFunctionList()
           .get(0);
       Method method = jpaFunction.getFunction();
       Object[] args = jpaFunction.getArguments();
 
       if (uriParserResultView.getFunctionImport().getReturnType()
           .getMultiplicity().equals(EdmMultiplicity.MANY)) {
 
         resultObj = (List<Object>) method.invoke(
             jpaMethodContext.getEnclosingObject(), args);
       } else {
         resultObj = new ArrayList<Object>();
         Object result = method.invoke(
             jpaMethodContext.getEnclosingObject(), args);
         resultObj.add(result);
       }
 
     } catch (EdmException e) {
       throw ODataJPARuntimeException
           .throwException(ODataJPARuntimeException.GENERAL
               .addContent(e.getMessage()), e);
     } catch (IllegalAccessException e) {
       throw ODataJPARuntimeException
           .throwException(ODataJPARuntimeException.GENERAL
               .addContent(e.getMessage()), e);
     } catch (IllegalArgumentException e) {
       throw ODataJPARuntimeException
           .throwException(ODataJPARuntimeException.GENERAL
               .addContent(e.getMessage()), e);
     } catch (InvocationTargetException e) {
       throw ODataJPARuntimeException
           .throwException(ODataJPARuntimeException.GENERAL
               .addContent(e.getTargetException().getMessage()), e.getTargetException());
     }
 
     return resultObj;
   }
 
   /* Process Get Entity Set Request (Query) */
   @SuppressWarnings("unchecked")
   @Override
   public <T> List<T> process(final GetEntitySetUriInfo uriParserResultView)
       throws ODataJPAModelException, ODataJPARuntimeException {
 
     if (uriParserResultView.getFunctionImport() != null) {
       return (List<T>) process((GetFunctionImportUriInfo) uriParserResultView);
     }
     JPQLContextType contextType = null;
     try {
       if (!uriParserResultView.getStartEntitySet().getName()
           .equals(uriParserResultView.getTargetEntitySet().getName())) {
         contextType = JPQLContextType.JOIN;
       } else {
         contextType = JPQLContextType.SELECT;
       }
 
     } catch (EdmException e) {
       ODataJPARuntimeException.throwException(
           ODataJPARuntimeException.GENERAL, e);
     }
 
     JPQLContext jpqlContext = JPQLContext.createBuilder(contextType,
         uriParserResultView).build();
 
     JPQLStatement jpqlStatement = JPQLStatement.createBuilder(jpqlContext)
         .build();
     Query query = null;
     try {
       query = em.createQuery(jpqlStatement.toString());
       // $top/$skip with $inlinecount case handled in response builder to avoid multiple DB call
       if (uriParserResultView.getSkip() != null && uriParserResultView.getInlineCount() == null) {
         query.setFirstResult(uriParserResultView.getSkip());
       }
 
       if (uriParserResultView.getTop() != null && uriParserResultView.getInlineCount() == null) {
         if (uriParserResultView.getTop() == 0) {
           List<T> resultList = new ArrayList<T>();
           return resultList;
         } else {
           query.setMaxResults(uriParserResultView.getTop());
         }
       }
       return query.getResultList();
     } catch (Exception e) {
       throw ODataJPARuntimeException.throwException(
           ODataJPARuntimeException.ERROR_JPQL_QUERY_CREATE, e);
 
     }
   }
 
   /* Process Get Entity Request (Read) */
   @Override
   public <T> Object process(GetEntityUriInfo uriParserResultView)
       throws ODataJPAModelException, ODataJPARuntimeException {
 
     JPQLContextType contextType = null;
     try {
       if (uriParserResultView instanceof GetEntityUriInfo) {
         uriParserResultView = ((GetEntityUriInfo) uriParserResultView);
         if (!((GetEntityUriInfo) uriParserResultView).getStartEntitySet().getName()
             .equals(((GetEntityUriInfo) uriParserResultView).getTargetEntitySet().getName())) {
           contextType = JPQLContextType.JOIN_SINGLE;
         } else {
           contextType = JPQLContextType.SELECT_SINGLE;
         }
       }
     } catch (EdmException e) {
       ODataJPARuntimeException.throwException(
           ODataJPARuntimeException.GENERAL, e);
     }
 
     return readEntity(uriParserResultView, contextType);
   }
 
   /* Process $count for Get Entity Set Request */
   @Override
   public long process(final GetEntitySetCountUriInfo resultsView)
       throws ODataJPAModelException, ODataJPARuntimeException {
 
     JPQLContextType contextType = null;
     try {
       if (!resultsView.getStartEntitySet().getName()
           .equals(resultsView.getTargetEntitySet().getName())) {
         contextType = JPQLContextType.JOIN_COUNT;
       } else {
         contextType = JPQLContextType.SELECT_COUNT;
       }
     } catch (EdmException e) {
       ODataJPARuntimeException.throwException(
           ODataJPARuntimeException.GENERAL, e);
     }
 
     JPQLContext jpqlContext = JPQLContext.createBuilder(contextType,
         resultsView).build();
 
     JPQLStatement jpqlStatement = JPQLStatement.createBuilder(jpqlContext)
         .build();
     Query query = null;
     try {
 
       query = em.createQuery(jpqlStatement.toString());
       List<?> resultList = query.getResultList();
       if (resultList != null && resultList.size() == 1) {
         return Long.valueOf(resultList.get(0).toString());
       }
     } catch (IllegalArgumentException e) {
       throw ODataJPARuntimeException.throwException(
           ODataJPARuntimeException.ERROR_JPQL_QUERY_CREATE, e);
     }
     return 0;
   }
 
   /* Process $count for Get Entity Request */
   @Override
   public long process(final GetEntityCountUriInfo resultsView) throws ODataJPAModelException, ODataJPARuntimeException {
 
     JPQLContextType contextType = null;
     try {
       if (!resultsView.getStartEntitySet().getName()
           .equals(resultsView.getTargetEntitySet().getName())) {
         contextType = JPQLContextType.JOIN_COUNT;
       } else {
         contextType = JPQLContextType.SELECT_COUNT;
       }
     } catch (EdmException e) {
       ODataJPARuntimeException.throwException(
           ODataJPARuntimeException.GENERAL, e);
     }
 
     JPQLContext jpqlContext = JPQLContext.createBuilder(contextType,
         resultsView).build();
 
     JPQLStatement jpqlStatement = JPQLStatement.createBuilder(jpqlContext)
         .build();
     Query query = null;
     try {
 
       query = em.createQuery(jpqlStatement.toString());
       List<?> resultList = query.getResultList();
       if (resultList != null && resultList.size() == 1) {
         return Long.valueOf(resultList.get(0).toString());
       }
     } catch (IllegalArgumentException e) {
       throw ODataJPARuntimeException.throwException(
           ODataJPARuntimeException.ERROR_JPQL_QUERY_CREATE, e);
     }
 
     return 0;
   }
 
   /* Process Create Entity Request */
   public <T> List<T> process(final PostUriInfo createView, final InputStream content,
       final String requestedContentType) throws ODataJPAModelException,
       ODataJPARuntimeException {
     return processCreate(createView, content, null, requestedContentType);
   }
 
   @Override
   public <T> List<T> process(PostUriInfo createView, Map<String, Object> content) throws ODataJPAModelException, ODataJPARuntimeException {
     return processCreate(createView, null, content, null);
   }
 
   /* Process Update Entity Request */
   @Override
   public <T> Object process(PutMergePatchUriInfo updateView,
       final InputStream content, final String requestContentType)
       throws ODataJPAModelException, ODataJPARuntimeException {
     return processUpdate(updateView, content, null, requestContentType);
   }
 
   @Override
   public <T> Object process(PutMergePatchUriInfo updateView, Map<String, Object> content) throws ODataJPAModelException, ODataJPARuntimeException {
     return processUpdate(updateView, null, content, null);
   }
 
   @SuppressWarnings("unchecked")
   private <T> List<T> processCreate(final PostUriInfo createView, final InputStream content, final Map<String, Object> properties,
       final String requestedContentType) throws ODataJPAModelException,
       ODataJPARuntimeException {
     try {
 
       final EdmEntitySet oDataEntitySet = createView.getTargetEntitySet();
       final EdmEntityType oDataEntityType = oDataEntitySet.getEntityType();
       final JPAEntity virtualJPAEntity = new JPAEntity(oDataEntityType, oDataEntitySet);
       final List<Object> createList = new ArrayList<Object>();
       Object jpaEntity = null;
 
       if (content != null) {
         final ODataEntityParser oDataEntityParser = new ODataEntityParser(oDataJPAContext);
         final ODataEntry oDataEntry = oDataEntityParser.parseEntry(oDataEntitySet, content, requestedContentType, false);
         virtualJPAEntity.create(oDataEntry);
       }
       else if (properties != null)
         virtualJPAEntity.create(properties);
       else
         return null;
 
       em.getTransaction().begin();
       jpaEntity = virtualJPAEntity.getJPAEntity();
 
      JPALink link = new JPALink(oDataJPAContext);
      link.setSourceJPAEntity(jpaEntity);
      link.create(createView, content, requestedContentType, requestedContentType);

       em.persist(jpaEntity);
       if (em.contains(jpaEntity)) {
         em.getTransaction().commit();
 
         createList.add(virtualJPAEntity.getJPAEntity());
         createList.add(virtualJPAEntity.getInlineJPAEntities());
 
         return (List<T>) createList;
       }
     } catch (Exception e) {
       em.getTransaction().rollback();
       throw ODataJPARuntimeException.throwException(
           ODataJPARuntimeException.ERROR_JPQL_CREATE_REQUEST, e);
     }
     return null;
   }
 
   public <T> Object processUpdate(PutMergePatchUriInfo updateView,
       final InputStream content, Map<String, Object> properties, final String requestContentType)
       throws ODataJPAModelException, ODataJPARuntimeException {
     JPQLContextType contextType = null;
     Object jpaEntity = null;
     try {
       em.getTransaction().begin();
       if (updateView instanceof PutMergePatchUriInfo) {
         updateView = ((PutMergePatchUriInfo) updateView);
         if (!((PutMergePatchUriInfo) updateView).getStartEntitySet().getName()
             .equals(((PutMergePatchUriInfo) updateView).getTargetEntitySet().getName())) {
           contextType = JPQLContextType.JOIN_SINGLE;
         } else {
           contextType = JPQLContextType.SELECT_SINGLE;
         }
       }
 
       jpaEntity = readEntity(updateView, contextType);
 
       if (jpaEntity == null)
         throw ODataJPARuntimeException
             .throwException(ODataJPARuntimeException.RESOURCE_NOT_FOUND, null);
 
       final EdmEntitySet oDataEntitySet = updateView.getTargetEntitySet();
       final EdmEntityType oDataEntityType = oDataEntitySet.getEntityType();
       final JPAEntity virtualJPAEntity = new JPAEntity(oDataEntityType, oDataEntitySet);
       virtualJPAEntity.setJPAEntity(jpaEntity);
 
       if (content != null) {
         final ODataEntityParser oDataEntityParser = new ODataEntityParser(oDataJPAContext);
         final ODataEntry oDataEntry = oDataEntityParser.parseEntry(oDataEntitySet, content, requestContentType, false);
         virtualJPAEntity.update(oDataEntry);
       }
       else if (properties != null)
         virtualJPAEntity.update(properties);
       else
         return null;
       em.flush();
       em.getTransaction().commit();
     } catch (Exception e) {
       em.getTransaction().rollback();
       throw ODataJPARuntimeException.throwException(
           ODataJPARuntimeException.ERROR_JPQL_UPDATE_REQUEST, e);
     }
 
     return jpaEntity;
   }
 
   /* Process Delete Entity Request */
   @Override
   public Object process(DeleteUriInfo uriParserResultView, final String contentType)
       throws ODataJPAModelException, ODataJPARuntimeException {
     JPQLContextType contextType = null;
     try {
       if (uriParserResultView instanceof DeleteUriInfo) {
         uriParserResultView = ((DeleteUriInfo) uriParserResultView);
         if (!((DeleteUriInfo) uriParserResultView).getStartEntitySet().getName()
             .equals(((DeleteUriInfo) uriParserResultView).getTargetEntitySet().getName())) {
           contextType = JPQLContextType.JOIN_SINGLE;
         } else {
           contextType = JPQLContextType.SELECT_SINGLE;
         }
       }
     } catch (EdmException e) {
       ODataJPARuntimeException.throwException(
           ODataJPARuntimeException.GENERAL, e);
     }
 
     // First read the entity with read operation.
     Object selectedObject = readEntity(uriParserResultView, contextType);
     // Read operation done. This object would be passed on to entity manager for delete
     if (selectedObject != null) {
       try {
         em.getTransaction().begin();
         em.remove(selectedObject);
         em.flush();
         em.getTransaction().commit();
       } catch (Exception e) {
         em.getTransaction().rollback();
         throw ODataJPARuntimeException.throwException(
             ODataJPARuntimeException.ERROR_JPQL_DELETE_REQUEST, e);
       }
     }
     return selectedObject;
   }
 
   /* Process Get Entity Link Request */
   @Override
   public Object process(final GetEntityLinkUriInfo uriParserResultView)
       throws ODataJPAModelException, ODataJPARuntimeException {
 
     return this.process((GetEntityUriInfo) uriParserResultView);
   }
 
   /* Process Get Entity Set Link Request */
   @Override
   public <T> List<T> process(final GetEntitySetLinksUriInfo uriParserResultView)
       throws ODataJPAModelException, ODataJPARuntimeException {
     return this.process((GetEntitySetUriInfo) uriParserResultView);
   }
 
   @Override
   public void process(final PostUriInfo uriInfo,
       final InputStream content, final String requestContentType, final String contentType)
       throws ODataJPARuntimeException, ODataJPAModelException {
     JPALink link = new JPALink(oDataJPAContext);
     link.create(uriInfo, content, requestContentType, contentType);
     link.save();
   }
 
   /* Common method for Read and Delete */
   private Object readEntity(final Object uriParserResultView, final JPQLContextType contextType)
       throws ODataJPAModelException, ODataJPARuntimeException {
 
     Object selectedObject = null;
 
     if (uriParserResultView instanceof DeleteUriInfo || uriParserResultView instanceof GetEntityUriInfo || uriParserResultView instanceof PutMergePatchUriInfo) {
 
       JPQLContext selectJPQLContext = JPQLContext.createBuilder(
           contextType, uriParserResultView).build();
 
       JPQLStatement selectJPQLStatement = JPQLStatement.createBuilder(
           selectJPQLContext).build();
       Query query = null;
       try {
         query = em.createQuery(selectJPQLStatement.toString());
         if (!query.getResultList().isEmpty()) {
           selectedObject = query.getResultList().get(0);
         }
       } catch (IllegalArgumentException e) {
         throw ODataJPARuntimeException.throwException(
             ODataJPARuntimeException.ERROR_JPQL_QUERY_CREATE, e);
       }
     }
     return selectedObject;
   }
 
   @Override
   public void process(final PutMergePatchUriInfo putUriInfo,
       final InputStream content, final String requestContentType, final String contentType)
       throws ODataJPARuntimeException, ODataJPAModelException {
 
     JPALink link = new JPALink(oDataJPAContext);
     link.update(putUriInfo, content, requestContentType, contentType);
     link.save();
 
   }
 
 }
