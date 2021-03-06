 /**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2004-2005 The Regents of the University of Michigan, Trustees of Indiana University,
 *                  Board of Trustees of the Leland Stanford, Jr., University, and The MIT Corporation
 *
 * Licensed under the Educational Community License Version 1.0 (the "License");
 * By obtaining, using and/or copying this Original Work, you agree that you have read,
 * understand, and will comply with the terms and conditions of the Educational Community License.
 * You may obtain a copy of the License at:
 *
 *      http://cvs.sakaiproject.org/licenses/license_1_0.html
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 **********************************************************************************/
 package org.sakaiproject.tool.assessment.facade;
 
 import java.io.InputStream;
 import java.sql.Connection;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
 
 import org.hibernate.Criteria;
 import org.hibernate.Hibernate;
 import org.hibernate.HibernateException;
 import org.hibernate.Query;
 import org.hibernate.Session;
 import org.hibernate.criterion.Criterion;
 import org.hibernate.criterion.Disjunction;
 import org.hibernate.criterion.Expression;
 import org.hibernate.criterion.Order;
 import org.hibernate.type.Type;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.sakaiproject.service.gradebook.shared.GradebookService;
 import org.sakaiproject.spring.SpringBeanLocator;
 import org.sakaiproject.tool.assessment.services.PersistenceService;
 import org.sakaiproject.tool.assessment.data.dao.assessment.PublishedAnswer;
 import org.sakaiproject.tool.assessment.data.dao.assessment.PublishedAssessmentData;
 import org.sakaiproject.tool.assessment.data.dao.assessment.PublishedItemData;
 import org.sakaiproject.tool.assessment.data.dao.assessment.PublishedItemText;
 import org.sakaiproject.tool.assessment.data.dao.assessment.PublishedSectionData;
 import org.sakaiproject.tool.assessment.data.dao.grading.AssessmentGradingData;
 import org.sakaiproject.tool.assessment.data.dao.grading.ItemGradingData;
 import org.sakaiproject.tool.assessment.data.dao.grading.MediaData;
 import org.sakaiproject.tool.assessment.data.ifc.assessment.AnswerIfc;
 import org.sakaiproject.tool.assessment.data.ifc.assessment.AssessmentBaseIfc;
 import org.sakaiproject.tool.assessment.data.ifc.assessment.EvaluationModelIfc;
 import org.sakaiproject.tool.assessment.data.ifc.assessment.ItemDataIfc;
 import org.sakaiproject.tool.assessment.data.ifc.assessment.ItemMetaDataIfc;
 import org.sakaiproject.tool.assessment.data.ifc.assessment.ItemTextIfc;
 import org.sakaiproject.tool.assessment.data.ifc.assessment.PublishedAssessmentIfc;
 import org.sakaiproject.tool.assessment.data.ifc.grading.AssessmentGradingIfc;
 import org.sakaiproject.tool.assessment.data.ifc.grading.ItemGradingIfc;
 import org.sakaiproject.tool.assessment.integration.context.IntegrationContextFactory;
 import org.sakaiproject.tool.assessment.integration.helper.ifc.GradebookServiceHelper;
 import org.sakaiproject.tool.assessment.services.PersistenceService;
 import org.springframework.orm.hibernate3.HibernateCallback;
 import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
 
 public class AssessmentGradingFacadeQueries extends HibernateDaoSupport implements AssessmentGradingFacadeQueriesAPI{
   private static Log log = LogFactory.getLog(AssessmentGradingFacadeQueries.class);
 
   public AssessmentGradingFacadeQueries () {
   }
 
   public List getTotalScores(final String publishedId, String which) {
     try {
       // sectionSet of publishedAssessment is defined as lazy loading in
       // Hibernate OR map, so we need to initialize them. Unfortunately our
       // spring-1.0.2.jar does not support HibernateTemplate.intialize(Object)
       // so we need to do it ourselves
       PublishedAssessmentData assessment =PersistenceService.getInstance().getPublishedAssessmentFacadeQueries().
         loadPublishedAssessment(new Long(publishedId));
       HashSet sectionSet = PersistenceService.getInstance().
           getPublishedAssessmentFacadeQueries().getSectionSetForAssessment(assessment);
       assessment.setSectionSet(sectionSet);
       // proceed to get totalScores
 //      Object[] objects = new Object[2];
 //      objects[0] = new Long(publishedId);
 //      objects[1] = new Boolean(true);
 //      Type[] types = new Type[2];
 //      types[0] = Hibernate.LONG;
 //      types[1] = Hibernate.BOOLEAN;
 
       final HibernateCallback hcb = new HibernateCallback(){
       	public Object doInHibernate(Session session) throws HibernateException, SQLException {
       		Query q = session.createQuery(
       				"from AssessmentGradingData a where a.publishedAssessmentId=? and a.forGrade=? order by agentId ASC, finalScore DESC, submittedDate DESC");
       		q.setLong(0, Long.parseLong(publishedId));
       		q.setBoolean(1, true);
       		return q.list();
       	};
       };
       List list = getHibernateTemplate().executeFind(hcb);
 
 //      List list = getHibernateTemplate().find(
 //    		  "from AssessmentGradingData a where a.publishedAssessmentId=? and a.forGrade=? order by agentId ASC, finalScore DESC, submittedDate DESC", 
 //    		  objects, types);
 
       // last submission
       if (which.equals(EvaluationModelIfc.LAST_SCORE.toString())) {
     	    final HibernateCallback hcb2 = new HibernateCallback(){
     	    	public Object doInHibernate(Session session) throws HibernateException, SQLException {
     	    		Query q = session.createQuery(
     	    				"from AssessmentGradingData a where a.publishedAssessmentId=? and a.forGrade=? order by agentId ASC, submittedDate DESC");
     	      		q.setLong(0, Long.parseLong(publishedId));
     	      		q.setBoolean(1, true);
     	    		return q.list();
     	    	};
     	    };
     	    list = getHibernateTemplate().executeFind(hcb2);
 
 //    	  list = getHibernateTemplate().find(
 //    		  "from AssessmentGradingData a where a.publishedAssessmentId=? and a.forGrade=? order by agentId ASC, submittedDate DESC", 
 //    		  objects, types);
       }
 
       if (which.equals(EvaluationModelIfc.ALL_SCORE.toString())) {
         return list;
       }
       else {
         // only take highest or latest
         Iterator items = list.iterator();
         ArrayList newlist = new ArrayList();
         String agentid = null;
         AssessmentGradingData data = (AssessmentGradingData) items.next();
         // daisyf add the following line on 12/15/04
         data.setPublishedAssessmentId(assessment.getPublishedAssessmentId());
         agentid = data.getAgentId();
         newlist.add(data);
         while (items.hasNext()) {
           while (items.hasNext()) {
             data = (AssessmentGradingData) items.next();
             if (!data.getAgentId().equals(agentid)) {
               agentid = data.getAgentId();
               newlist.add(data);
               break;
             }
           }
         }
         return newlist;
       }
 
     } catch (Exception e) {
       e.printStackTrace();
       return new ArrayList();
     }
   }
 
   public List getAllSubmissions(final String publishedId)
   {
 //      Object[] objects = new Object[1];
 //      objects[0] = new Long(publishedId);
 //      Type[] types = new Type[1];
 //      types[0] = Hibernate.LONG;
 
       final HibernateCallback hcb = new HibernateCallback(){
       	public Object doInHibernate(Session session) throws HibernateException, SQLException {
       		Query q = session.createQuery(
       				"from AssessmentGradingData a where a.publishedAssessmentId=? and a.forGrade=1");
       		q.setLong(0, Long.parseLong(publishedId));
       		return q.list();
       	};
       };
       return getHibernateTemplate().executeFind(hcb);
 
 //      List list = getHibernateTemplate().find("from AssessmentGradingData a where a.publishedAssessmentId=? and a.forGrade=1", objects, types);
 //      return list;
   }
 
 
 
   public HashMap getItemScores(Long publishedId, final Long itemId, String which)
   {
     try {
       ArrayList scores = (ArrayList)
         getTotalScores(publishedId.toString(), which);
       HashMap map = new HashMap();
       List list = new ArrayList();
 
       // make final for callback to access
       final Iterator iter = scores.iterator();
 
       HibernateCallback hcb = new HibernateCallback()
       {
         public Object doInHibernate(Session session) throws HibernateException,
           SQLException
         {
           Criteria criteria = session.createCriteria(ItemGradingData.class);
           Disjunction disjunction = Expression.disjunction();
 
           /** make list from AssessmentGradingData ids */
           List gradingIdList = new ArrayList();
           while (iter.hasNext()){
             AssessmentGradingData data = (AssessmentGradingData) iter.next();
             gradingIdList.add(data.getAssessmentGradingId());
           }
 
           /** create or disjunctive expression for (in clauses) */
           List tempList = new ArrayList();
         for (int i = 0; i < gradingIdList.size(); i += 50){
           if (i + 50 > gradingIdList.size()){
               tempList = gradingIdList.subList(i, gradingIdList.size());
               disjunction.add(Expression.in("assessmentGradingId", tempList));
           }
           else{
             tempList = gradingIdList.subList(i, i + 50);
             disjunction.add(Expression.in("assessmentGradingId", tempList));
           }
         }
 
         if (itemId.equals(new Long(0))) {
           criteria.add(disjunction);
         }
         else {
 
           /** create logical and between the pubCriterion and the disjunction criterion */
           //Criterion pubCriterion = Expression.eq("publishedItem.itemId", itemId);
           Criterion pubCriterion = Expression.eq("publishedItemId", itemId);
           criteria.add(Expression.and(pubCriterion, disjunction));
         }
           criteria.addOrder(Order.asc("agentId"));
           criteria.addOrder(Order.desc("submittedDate"));
           //return criteria.list();
           //large list cause out of memory error (java heap space)
           return criteria.setMaxResults(10000).list();
         }
       };
       List temp = (List) getHibernateTemplate().execute(hcb);
 
       Iterator iter2 = temp.iterator();
       while (iter2.hasNext())
       {
         ItemGradingData data = (ItemGradingData) iter2.next();
         ArrayList thisone = (ArrayList)
           map.get(data.getPublishedItemId());
         if (thisone == null)
           thisone = new ArrayList();
         thisone.add(data);
         map.put(data.getPublishedItemId(), thisone);
       }
       return map;
     } catch (Exception e) {
       e.printStackTrace();
       return new HashMap();
     }
   }
 
   /**
    * This returns a hashmap of all the latest item entries, keyed by
    * item id for easy retrieval.
    * return (Long publishedItemId, ArrayList itemGradingData)
    */
   public HashMap getLastItemGradingData(final Long publishedId, final String agentId)
   {
     try {
 //      Object[] objects = new Object[2];
 //      objects[0] = publishedId;
 //      objects[1] = agentId;
 //      Type[] types = new Type[2];
 //      types[0] = Hibernate.LONG;
 //      types[1] = Hibernate.STRING;
 
       final HibernateCallback hcb = new HibernateCallback(){
       	public Object doInHibernate(Session session) throws HibernateException, SQLException {
       		Query q = session.createQuery("from AssessmentGradingData a where a.publishedAssessmentId=? and a.agentId=? order by submittedDate DESC");
      		q.setLong(1, publishedId.longValue());
       		q.setString(1, agentId);
       		return q.list();
       	};
       };
       ArrayList scores = (ArrayList) getHibernateTemplate().executeFind(hcb);
 
 //      ArrayList scores = (ArrayList) getHibernateTemplate().find("from AssessmentGradingData a where a.publishedAssessmentId=? and a.agentId=? order by submittedDate DESC", objects, types);
       HashMap map = new HashMap();
       if (scores.isEmpty())
         return new HashMap();
       AssessmentGradingData gdata = (AssessmentGradingData) scores.toArray()[0];
       // initialize itemGradingSet
       gdata.setItemGradingSet(getItemGradingSet(gdata.getAssessmentGradingId()));
       if (gdata.getForGrade().booleanValue())
         return new HashMap();
       Iterator iter = gdata.getItemGradingSet().iterator();
       while (iter.hasNext())
       {
         ItemGradingData data = (ItemGradingData) iter.next();
         ArrayList thisone = (ArrayList) map.get(data.getPublishedItemId());
         if (thisone == null)
           thisone = new ArrayList();
         thisone.add(data);
         map.put(data.getPublishedItemId(), thisone);
       }
       return map;
     } catch (Exception e) {
       e.printStackTrace();
       return new HashMap();
     }
   }
 
 
 
   /**
    * This returns a hashmap of all the submitted items, keyed by
    * item id for easy retrieval.
    */
   public HashMap getStudentGradingData(String assessmentGradingId)
   {
     try {
       HashMap map = new HashMap();
       AssessmentGradingData gdata = load(new Long(assessmentGradingId));
       gdata.setItemGradingSet(getItemGradingSet(gdata.getAssessmentGradingId()));
       log.debug("****#6, gdata="+gdata);
       log.debug("****#7, item size="+gdata.getItemGradingSet().size());
       Iterator iter = gdata.getItemGradingSet().iterator();
       while (iter.hasNext())
       {
         ItemGradingData data = (ItemGradingData) iter.next();
         ArrayList thisone = (ArrayList)
           map.get(data.getPublishedItemId());
         if (thisone == null)
           thisone = new ArrayList();
         thisone.add(data);
         map.put(data.getPublishedItemId(), thisone);
       }
       return map;
     } catch (Exception e) {
       e.printStackTrace();
       return new HashMap();
     }
   }
 
   public HashMap getSubmitData(final Long publishedId, final String agentId)
   {
     try {
 //      Object[] objects = new Object[3];
 //      objects[0] = publishedId;
 //      objects[1] = agentId;
 //      objects[2] = new Boolean(true);
 //      Type[] types = new Type[3];
 //      types[0] = Hibernate.LONG;
 //      types[1] = Hibernate.STRING;
 //      types[2] = Hibernate.BOOLEAN;
 
       final HibernateCallback hcb = new HibernateCallback(){
       	public Object doInHibernate(Session session) throws HibernateException, SQLException {
       		Query q = session.createQuery("from AssessmentGradingData a where a.publishedAssessmentId=? and a.agentId=? and a.forGrade=? order by submittedDate DESC");
       		q.setLong(0, publishedId.longValue());
       		q.setString(1, agentId);
       		q.setBoolean(2, true);
       		return q.list();
       	};
       };
       ArrayList scores = (ArrayList) getHibernateTemplate().executeFind(hcb);
 
 //      ArrayList scores = (ArrayList) getHibernateTemplate().find("from AssessmentGradingData a where a.publishedAssessmentId=? and a.agentId=? and a.forGrade=? order by submittedDate DESC", objects, types);
       HashMap map = new HashMap();
       if (scores.isEmpty())
         return new HashMap();
       AssessmentGradingData gdata = (AssessmentGradingData) scores.toArray()[0];
       gdata.setItemGradingSet(getItemGradingSet(gdata.getAssessmentGradingId()));
       Iterator iter = gdata.getItemGradingSet().iterator();
       while (iter.hasNext())
       {
         ItemGradingData data = (ItemGradingData) iter.next();
         ArrayList thisone = (ArrayList)
           map.get(data.getPublishedItemId());
         if (thisone == null)
           thisone = new ArrayList();
         thisone.add(data);
         map.put(data.getPublishedItemId(), thisone);
       }
       return map;
     } catch (Exception e) {
       e.printStackTrace();
       return new HashMap();
     }
   }
 
   public Long add(AssessmentGradingData a) {
     int retryCount = PersistenceService.getInstance().getRetryCount().intValue();
     while (retryCount > 0){ 
       try {
         getHibernateTemplate().save(a);
         retryCount = 0;
       }
       catch (Exception e) {
         log.warn("problem adding assessmentGrading: "+e.getMessage());
         retryCount = PersistenceService.getInstance().retryDeadlock(e, retryCount);
       }
     }
     return a.getAssessmentGradingId();
   }
 
   public int getSubmissionSizeOfPublishedAssessment(Long publishedAssessmentId){
     List size = getHibernateTemplate().find(
         "select count(i) from AssessmentGradingData a where a.forGrade=1 and a.publishedAssessmentId=?"+ publishedAssessmentId);
     Iterator iter = size.iterator();
     if (iter.hasNext()){
       int i = ((Integer)iter.next()).intValue();
       return i;
     }
     else{
       return 0;
     }
   }
 
   public HashMap getSubmissionSizeOfAllPublishedAssessments(){
     HashMap h = new HashMap();
     List list = getHibernateTemplate().find(
         "select new PublishedAssessmentData(a.publishedAssessmentId, count(a)) from AssessmentGradingData a where a.forGrade=1 group by a.publishedAssessmentId");
     Iterator iter = list.iterator();
     while (iter.hasNext()){
       PublishedAssessmentData o = (PublishedAssessmentData)iter.next();
       h.put(o.getPublishedAssessmentId(), new Integer(o.getSubmissionSize()));
     }
     return h;
   }
 
   public Long saveMedia(byte[] media, String mimeType){
     MediaData mediaData = new MediaData(media, mimeType);
     int retryCount = PersistenceService.getInstance().getRetryCount().intValue();
     while (retryCount > 0){ 
       try {
         getHibernateTemplate().save(mediaData);
         retryCount = 0;
       }
       catch (Exception e) {
         log.warn("problem saving media with mimeType: "+e.getMessage());
         retryCount = PersistenceService.getInstance().retryDeadlock(e, retryCount);
       }
     }
     return mediaData.getMediaId();
   }
 
   public Long saveMedia(MediaData mediaData){
     int retryCount = PersistenceService.getInstance().getRetryCount().intValue();
     while (retryCount > 0){ 
       try {
         getHibernateTemplate().save(mediaData);
         retryCount = 0;
       }
       catch (Exception e) {
         log.warn("problem saving media: "+e.getMessage());
         retryCount = PersistenceService.getInstance().retryDeadlock(e, retryCount);
       }
     }
     return mediaData.getMediaId();
   }
 
   public void removeMediaById(Long mediaId){
     MediaData media = (MediaData) getHibernateTemplate().load(MediaData.class, mediaId);
     int retryCount = PersistenceService.getInstance().getRetryCount().intValue();
     while (retryCount > 0){ 
       try {
         getHibernateTemplate().delete(media);
         retryCount = 0;
       }
       catch (Exception e) {
         log.warn("problem removing mediaId="+mediaId+":"+e.getMessage());
         retryCount = PersistenceService.getInstance().retryDeadlock(e, retryCount);
       }
     }
   }
 
   public MediaData getMedia(Long mediaId){
     MediaData mediaData = (MediaData) getHibernateTemplate().load(MediaData.class,mediaId);
     mediaData.setMedia(getMediaStream(mediaId));
     return mediaData;
   }
 
   public ArrayList getMediaArray(final Long itemGradingId){
     log.debug("*** itemGradingId ="+itemGradingId);
     ArrayList a = new ArrayList();
 
     final HibernateCallback hcb = new HibernateCallback(){
     	public Object doInHibernate(Session session) throws HibernateException, SQLException {
     		Query q = session.createQuery("from MediaData m where m.itemGradingData.itemGradingId=?");
     		q.setLong(0, itemGradingId.longValue());
     		return q.list();
     	};
     };
     List list = getHibernateTemplate().executeFind(hcb);
 
 //    List list = getHibernateTemplate().find(
 //        "from MediaData m where m.itemGradingData.itemGradingId=?",
 //        new Object[] { itemGradingId },
 //        new org.hibernate.type.Type[] { Hibernate.LONG });
     for (int i=0;i<list.size();i++){
       a.add((MediaData)list.get(i));
     }
     log.debug("*** no. of media ="+a.size());
     return a;
   }
 
   public ArrayList getMediaArray(ItemGradingData item){
     ArrayList a = new ArrayList();
     List list = getHibernateTemplate().find(
         "from MediaData m where m.itemGradingData=?", item );
     for (int i=0;i<list.size();i++){
       a.add((MediaData)list.get(i));
     }
     log.debug("*** no. of media ="+a.size());
     return a;
   }
 
   public ItemGradingData getLastItemGradingDataByAgent(
       final Long publishedItemId, final String agentId)
   {
 	    final HibernateCallback hcb = new HibernateCallback(){
 	    	public Object doInHibernate(Session session) throws HibernateException, SQLException {
 	    		Query q = session.createQuery("from ItemGradingData i where i.publishedItemId=? and i.agentId=?");
 	    		q.setLong(0, publishedItemId.longValue());
 	    		q.setString(1, agentId);
 	    		return q.list();
 	    	};
 	    };
 	    List itemGradings = getHibernateTemplate().executeFind(hcb);
 
 //	  List itemGradings = getHibernateTemplate().find(
 //        "from ItemGradingData i where i.publishedItemId=? and i.agentId=?",
 //        new Object[] { publishedItemId, agentId },
 //        new org.hibernate.type.Type[] { Hibernate.LONG, Hibernate.STRING });
     if (itemGradings.size() == 0)
       return null;
     return (ItemGradingData) itemGradings.get(0);
   }
 
   public ItemGradingData getItemGradingData(
       final Long assessmentGradingId, final Long publishedItemId)
   {
     log.debug("****assessmentGradingId="+assessmentGradingId);
     log.debug("****publishedItemId="+publishedItemId);
 
     final HibernateCallback hcb = new HibernateCallback(){
     	public Object doInHibernate(Session session) throws HibernateException, SQLException {
     		Query q = session.createQuery(
     				"from ItemGradingData i where i.assessmentGradingId = ? and i.publishedItemId=?");
     		q.setLong(0, assessmentGradingId.longValue());
     		q.setLong(1, publishedItemId.longValue());
     		return q.list();
     	};
     };
     List itemGradings = getHibernateTemplate().executeFind(hcb);
 
 //    List itemGradings = getHibernateTemplate().find(
 //        "from ItemGradingData i where i.assessmentGradingId = ? and i.publishedItemId=?",
 //        new Object[] { assessmentGradingId, publishedItemId },
 //        new org.hibernate.type.Type[] { Hibernate.LONG, Hibernate.LONG });
     if (itemGradings.size() == 0)
       return null;
     return (ItemGradingData) itemGradings.get(0);
   }
 
   public AssessmentGradingData load(Long id) {
     return (AssessmentGradingData) getHibernateTemplate().load(AssessmentGradingData.class, id);
   }
 
   public AssessmentGradingData getLastSavedAssessmentGradingByAgentId(final Long publishedAssessmentId, final String agentIdString) {
     AssessmentGradingData ag = null;
 
     final HibernateCallback hcb = new HibernateCallback(){
     	public Object doInHibernate(Session session) throws HibernateException, SQLException {
     		Query q = session.createQuery(
     				"from AssessmentGradingData a where a.publishedAssessmentId=? and a.agentId=? and a.forGrade=? order by a.submittedDate desc");
     		q.setLong(0, publishedAssessmentId.longValue());
     		q.setString(1, agentIdString);
     		q.setBoolean(2, false);
     		return q.list();
     	};
     };
     List assessmentGradings = getHibernateTemplate().executeFind(hcb);
 
 //    List assessmentGradings = getHibernateTemplate().find(
 //        "from AssessmentGradingData a where a.publishedAssessmentId=? and a.agentId=? and a.forGrade=? order by a.submittedDate desc",
 //         new Object[] { publishedAssessmentId, agentIdString, Boolean.FALSE },
 //         new org.hibernate.type.Type[] { Hibernate.LONG, Hibernate.STRING, Hibernate.BOOLEAN });
     if (assessmentGradings.size() != 0){
       ag = (AssessmentGradingData) assessmentGradings.get(0);
       ag.setItemGradingSet(getItemGradingSet(ag.getAssessmentGradingId()));
     }  
     return ag;
   }
 
   public AssessmentGradingData getLastAssessmentGradingByAgentId(final Long publishedAssessmentId, final String agentIdString) {
     AssessmentGradingData ag = null;
 
     final HibernateCallback hcb = new HibernateCallback(){
     	public Object doInHibernate(Session session) throws HibernateException, SQLException {
     		Query q = session.createQuery(
     				"from AssessmentGradingData a where a.publishedAssessmentId=? and a.agentId=? order by a.submittedDate desc");
     		q.setLong(0, publishedAssessmentId.longValue());
     		q.setString(1, agentIdString);
     		return q.list();
     	};
     };
     List assessmentGradings = getHibernateTemplate().executeFind(hcb);
 
 //    List assessmentGradings = getHibernateTemplate().find(
 //        "from AssessmentGradingData a where a.publishedAssessmentId=? and a.agentId=? order by a.submittedDate desc",
 //         new Object[] { publishedAssessmentId, agentIdString },
 //         new org.hibernate.type.Type[] { Hibernate.LONG, Hibernate.STRING });
     if (assessmentGradings.size() != 0){
       ag = (AssessmentGradingData) assessmentGradings.get(0);
       ag.setItemGradingSet(getItemGradingSet(ag.getAssessmentGradingId()));
     }  
     return ag;
   }
 
 
   public void saveItemGrading(ItemGradingIfc item) {
     int retryCount = PersistenceService.getInstance().getRetryCount().intValue();
     while (retryCount > 0){ 
       try {
         getHibernateTemplate().saveOrUpdate((ItemGradingData)item);
         retryCount = 0;
       }
       catch (Exception e) {
         log.warn("problem saving itemGrading: "+e.getMessage());
         retryCount = PersistenceService.getInstance().retryDeadlock(e, retryCount);
       }
     }
   }
 
   public void saveOrUpdateAssessmentGrading(AssessmentGradingIfc assessment) {
     int retryCount = PersistenceService.getInstance().getRetryCount().intValue();
     while (retryCount > 0){ 
       try {
         /* for testing the catch block - daisyf 
         if (retryCount >2)
           throw new Exception("uncategorized SQLException for SQL []; SQL state [61000]; error code [60]; ORA-00060: deadlock detected while waiting for resource");
 	*/ 
         getHibernateTemplate().saveOrUpdate((AssessmentGradingData)assessment);
         retryCount = 0;
       }
       catch (Exception e) {
         log.warn("problem inserting assessmentGrading: "+e.getMessage());
         retryCount = PersistenceService.getInstance().retryDeadlock(e, retryCount);
       }
     }
   }
 
   private byte[] getMediaStream(Long mediaId){
     byte[] b = new byte[4000];
     Session session = null;
     try{
       session = getSessionFactory().openSession();
       Connection conn = session.connection();
       log.debug("****Connection="+conn);
       String query="select MEDIA from SAM_MEDIA_T where MEDIAID=?";
       PreparedStatement statement = conn.prepareStatement(query);
       statement.setLong(1, mediaId.longValue());
       ResultSet rs = statement.executeQuery();
       if (rs.next()){
         java.lang.Object o = rs.getObject("MEDIA");
         if (o!=null){
           InputStream in = rs.getBinaryStream("MEDIA");
           in.mark(0);
           int ch;
           int len=0;
           while ((ch=in.read())!=-1)
         len++;
 
           b = new byte[len];
           in.reset();
           in.read(b,0,len);
           in.close();
   }
       }
     }
     catch(Exception e){
       log.warn(e.getMessage());
     }
     finally{
       try{
         if (session !=null) session.close();
       }
       catch(Exception ex){
         log.warn(ex.getMessage());
       }
     }
     return b;
   }
 
   public List getAssessmentGradingIds(final Long publishedItemId){
 	    final HibernateCallback hcb = new HibernateCallback(){
 	    	public Object doInHibernate(Session session) throws HibernateException, SQLException {
 	    		Query q = session.createQuery("select g.assessmentGradingId from "+
 	    		         " ItemGradingData g where g.publishedItemId=?");
 	    		q.setLong(0, publishedItemId.longValue());
 	    		return q.list();
 	    	};
 	    };
 	    return getHibernateTemplate().executeFind(hcb);
 
 //	  return getHibernateTemplate().find(
 //         "select g.assessmentGradingId from "+
 //         " ItemGradingData g where g.publishedItemId=?",
 //         new Object[] { publishedItemId },
 //         new org.hibernate.type.Type[] { Hibernate.LONG });
   }
 
   public AssessmentGradingIfc getHighestAssessmentGrading(
          final Long publishedAssessmentId, final String agentId)
   {
     AssessmentGradingData ag = null;
     final String query ="from AssessmentGradingData a "+
                   " where a.publishedAssessmentId=? and "+
                   " a.agentId=? order by a.finalScore desc";
 
     final HibernateCallback hcb = new HibernateCallback(){
     	public Object doInHibernate(Session session) throws HibernateException, SQLException {
     		Query q = session.createQuery(query);
     		q.setLong(0, publishedAssessmentId.longValue());
     		q.setString(1, agentId);
     		return q.list();
     	};
     };
     List assessmentGradings = getHibernateTemplate().executeFind(hcb);
 
 //    List assessmentGradings = getHibernateTemplate().find(query,
 //        new Object[] { publishedAssessmentId, agentId },
 //        new org.hibernate.type.Type[] { Hibernate.LONG, Hibernate.STRING });
     if (assessmentGradings.size() != 0){
       ag = (AssessmentGradingData) assessmentGradings.get(0);
       ag.setItemGradingSet(getItemGradingSet(ag.getAssessmentGradingId()));
     }  
     return ag;
   }
 
   public List getLastAssessmentGradingList(final Long publishedAssessmentId){
     final String query = "from AssessmentGradingData a where a.publishedAssessmentId=? order by agentId asc, a.submittedDate desc";
 
     final HibernateCallback hcb = new HibernateCallback(){
     	public Object doInHibernate(Session session) throws HibernateException, SQLException {
     		Query q = session.createQuery(query);
     		q.setLong(0, publishedAssessmentId.longValue());
     		return q.list();
     	};
     };
     List assessmentGradings = getHibernateTemplate().executeFind(hcb);
 
 //    List assessmentGradings = getHibernateTemplate().find(query,
 //         new Object[] { publishedAssessmentId },
 //         new org.hibernate.type.Type[] { Hibernate.LONG });
 
     ArrayList l = new ArrayList();
     String currentAgent="";
     for (int i=0; i<assessmentGradings.size(); i++){
       AssessmentGradingData g = (AssessmentGradingData)assessmentGradings.get(i);
       if (!currentAgent.equals(g.getAgentId())){
         l.add(g);
         currentAgent = g.getAgentId();
       }
     }
     return l;
   }
 
   public List getHighestAssessmentGradingList(final Long publishedAssessmentId){
     final String query = "from AssessmentGradingData a where a.publishedAssessmentId=? order by agentId asc, a.finalScore desc";
 
     final HibernateCallback hcb = new HibernateCallback(){
     	public Object doInHibernate(Session session) throws HibernateException, SQLException {
     		Query q = session.createQuery(query);
     		q.setLong(0, publishedAssessmentId.longValue());
     		return q.list();
     	};
     };
     List assessmentGradings = getHibernateTemplate().executeFind(hcb);
 
 //    List assessmentGradings = getHibernateTemplate().find(query,
 //         new Object[] { publishedAssessmentId },
 //         new org.hibernate.type.Type[] { Hibernate.LONG });
 
     ArrayList l = new ArrayList();
     String currentAgent="";
     for (int i=0; i<assessmentGradings.size(); i++){
       AssessmentGradingData g = (AssessmentGradingData)assessmentGradings.get(i);
       if (!currentAgent.equals(g.getAgentId())){
         l.add(g);
         currentAgent = g.getAgentId();
       }
     }
     return l;
   }
 
   // build a Hashmap (Long publishedItemId, ArrayList assessmentGradingIds)
   // containing the item submission of the last AssessmentGrading
   // (regardless of users who submitted it) of a given published assessment
   public HashMap getLastAssessmentGradingByPublishedItem(final Long publishedAssessmentId){
     HashMap h = new HashMap();
     final String query = "select new AssessmentGradingData("+
                    " a.assessmentGradingId, p.itemId, "+
                    " a.agentId, a.finalScore, a.submittedDate) "+
                    " from ItemGradingData i, AssessmentGradingData a,"+
                    " PublishedItemData p where "+
                    " i.assessmentGradingId = a.assessmentGradingId and i.publishedItemId = p.itemId and "+
                    " a.publishedAssessmentId=? " +
                    " order by a.agentId asc, a.submittedDate desc";
 
     final HibernateCallback hcb = new HibernateCallback(){
     	public Object doInHibernate(Session session) throws HibernateException, SQLException {
     		Query q = session.createQuery(query);
     		q.setLong(0, publishedAssessmentId.longValue());
     		return q.list();
     	};
     };
     List assessmentGradings = getHibernateTemplate().executeFind(hcb);
 
 //    List assessmentGradings = getHibernateTemplate().find(query,
 //         new Object[] { publishedAssessmentId },
 //         new org.hibernate.type.Type[] { Hibernate.LONG });
 
 //    ArrayList l = new ArrayList();
     String currentAgent="";
     Date submittedDate = null;
     for (int i=0; i<assessmentGradings.size(); i++){
       AssessmentGradingData g = (AssessmentGradingData)assessmentGradings.get(i);
       Long itemId = g.getPublishedItemId();
       Long gradingId = g.getAssessmentGradingId();
       log.debug("**** itemId="+itemId+", gradingId="+gradingId+", agentId="+g.getAgentId()+", score="+g.getFinalScore());
       if ( i==0 ){
         currentAgent = g.getAgentId();
         submittedDate = g.getSubmittedDate();
       }
       if (currentAgent.equals(g.getAgentId()) && submittedDate.equals(g.getSubmittedDate())){
         Object o = h.get(itemId);
         if (o != null)
           ((ArrayList) o).add(gradingId);
         else{
           ArrayList gradingIds = new ArrayList();
           gradingIds.add(gradingId);
           h.put(itemId, gradingIds);
   }
       }
       if (!currentAgent.equals(g.getAgentId())){
         currentAgent = g.getAgentId();
         submittedDate = g.getSubmittedDate();
       }
     }
     return h;
   }
 
   // build a Hashmap (Long publishedItemId, ArrayList assessmentGradingIds)
   // containing the item submission of the highest AssessmentGrading
   // (regardless of users who submitted it) of a given published assessment
   public HashMap getHighestAssessmentGradingByPublishedItem(final Long publishedAssessmentId){
     HashMap h = new HashMap();
     final String query = "select new AssessmentGradingData("+
                    " a.assessmentGradingId, p.itemId, "+
                    " a.agentId, a.finalScore, a.submittedDate) "+
                    " from ItemGradingData i, AssessmentGradingData a, "+
                    " PublishedItemData p where "+
                    " i.assessmentGradingId = a.assessmentGradingId and i.publishedItemId = p.itemId and "+
                    " a.publishedAssessmentId=? " +
                    " order by a.agentId asc, a.finalScore desc";
 
     final HibernateCallback hcb = new HibernateCallback(){
     	public Object doInHibernate(Session session) throws HibernateException, SQLException {
     		Query q = session.createQuery(query);
     		q.setLong(0, publishedAssessmentId.longValue());
     		return q.list();
     	};
     };
     List assessmentGradings = getHibernateTemplate().executeFind(hcb);
 
 //    List assessmentGradings = getHibernateTemplate().find(query,
 //         new Object[] { publishedAssessmentId },
 //         new org.hibernate.type.Type[] { Hibernate.LONG });
 
 //    ArrayList l = new ArrayList();
     String currentAgent="";
     Float finalScore = null;
     for (int i=0; i<assessmentGradings.size(); i++){
       AssessmentGradingData g = (AssessmentGradingData)assessmentGradings.get(i);
       Long itemId = g.getPublishedItemId();
       Long gradingId = g.getAssessmentGradingId();
       log.debug("**** itemId="+itemId+", gradingId="+gradingId+", agentId="+g.getAgentId()+", score="+g.getFinalScore());
       if ( i==0 ){
         currentAgent = g.getAgentId();
         finalScore = g.getFinalScore();
       }
       if (currentAgent.equals(g.getAgentId()) && finalScore.equals(g.getFinalScore())){
         Object o = h.get(itemId);
         if (o != null)
           ((ArrayList) o).add(gradingId);
         else{
           ArrayList gradingIds = new ArrayList();
           gradingIds.add(gradingId);
           h.put(itemId, gradingIds);
   }
       }
       if (!currentAgent.equals(g.getAgentId())){
         currentAgent = g.getAgentId();
         finalScore = g.getFinalScore();
       }
     }
     return h;
   }
 
   public Set getItemGradingSet(final Long assessmentGradingId){
     final String query = "from ItemGradingData i where i.assessmentGradingId=?";
 
     final HibernateCallback hcb = new HibernateCallback(){
     	public Object doInHibernate(Session session) throws HibernateException, SQLException {
     		Query q = session.createQuery(query);
     		q.setLong(0, assessmentGradingId.longValue());
     		return q.list();
     	};
     };
     List itemGradings = getHibernateTemplate().executeFind(hcb);
 
 //    List itemGradings = getHibernateTemplate().find(query,
 //                                                    new Object[] { assessmentGradingId },
 //                                                    new org.hibernate.type.Type[] { Hibernate.LONG });
     HashSet s = new HashSet();
     for (int i=0; i<itemGradings.size();i++){
       s.add(itemGradings.get(i));
     }
     return s;
   }
 
   public HashMap getAssessmentGradingByItemGradingId(final Long publishedAssessmentId){
     List aList = getAllSubmissions(publishedAssessmentId.toString());
     HashMap aHash = new HashMap();
     for (int j=0; j<aList.size();j++){
       AssessmentGradingData a = (AssessmentGradingData)aList.get(j);
       aHash.put(a.getAssessmentGradingId(), a);
     }
 
     final String query = "select new ItemGradingData(i.itemGradingId, a.assessmentGradingId) "+
                    " from ItemGradingData i, AssessmentGradingData a "+
                    " where i.assessmentGradingId=a.assessmentGradingId "+
                    " and a.publishedAssessmentId=?";
 
     final HibernateCallback hcb = new HibernateCallback(){
     	public Object doInHibernate(Session session) throws HibernateException, SQLException {
     		Query q = session.createQuery(query);
     		q.setLong(0, publishedAssessmentId.longValue());
     		return q.list();
     	};
     };
     List l = getHibernateTemplate().executeFind(hcb);
 
 //    List l = getHibernateTemplate().find(query,
 //             new Object[] { publishedAssessmentId },
 //             new org.hibernate.type.Type[] { Hibernate.LONG });
     //System.out.println("****** assessmentGradinghash="+l.size());
     HashMap h = new HashMap();
     for (int i=0; i<l.size();i++){
       ItemGradingData o = (ItemGradingData)l.get(i);
       h.put(o.getItemGradingId(), (AssessmentGradingData)aHash.get(o.getAssessmentGradingId()));
     }
     return h;
   }
 
   public void deleteAll(Collection c){
     int retryCount = PersistenceService.getInstance().getRetryCount().intValue();
     while (retryCount > 0){ 
       try {
         getHibernateTemplate().deleteAll(c);
         retryCount = 0;
       }
       catch (Exception e) {
         log.warn("problem inserting assessmentGrading: "+e.getMessage());
         retryCount = PersistenceService.getInstance().retryDeadlock(e, retryCount);
       }
     }
   }
 
 
   public void saveOrUpdateAll(Collection c) {
     int retryCount = PersistenceService.getInstance().getRetryCount().intValue();
     while (retryCount > 0){ 
       try {
         getHibernateTemplate().saveOrUpdateAll(c);
         retryCount = 0;
       }
       catch (Exception e) {
         log.warn("problem inserting assessmentGrading: "+e.getMessage());
         retryCount = PersistenceService.getInstance().retryDeadlock(e, retryCount);
       }
     }
   }
 
   public PublishedAssessmentIfc getPublishedAssessmentByAssessmentGradingId(final Long assessmentGradingId){
     PublishedAssessmentIfc pub = null;
     final String query = "select p from PublishedAssessmentData p, AssessmentGradingData a "+
                    " where a.publishedAssessmentId=p.publishedAssessmentId and a.assessmentGradingId=?";
 
     final HibernateCallback hcb = new HibernateCallback(){
     	public Object doInHibernate(Session session) throws HibernateException, SQLException {
     		Query q = session.createQuery(query);
     		q.setLong(0, assessmentGradingId.longValue());
     		return q.list();
     	};
     };
     List pubList = getHibernateTemplate().executeFind(hcb);
 
 //    List pubList = getHibernateTemplate().find(query,
 //                                                    new Object[] { assessmentGradingId },
 //                                                    new org.hibernate.type.Type[] { Hibernate.LONG });
     if (pubList!=null && pubList.size()>0)
       pub = (PublishedAssessmentIfc) pubList.get(0);
 
     return pub; 
   }
 
 }
