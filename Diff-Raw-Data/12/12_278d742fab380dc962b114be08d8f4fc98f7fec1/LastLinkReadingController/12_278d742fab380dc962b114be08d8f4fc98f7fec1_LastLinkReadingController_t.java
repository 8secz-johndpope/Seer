 package eu.wisebed.wisedb.controller;
 
 import eu.wisebed.wisedb.model.LastLinkReading;
 import eu.wisebed.wiseml.model.setup.Capability;
 import eu.wisebed.wiseml.model.setup.Link;
 import org.hibernate.Criteria;
 import org.hibernate.Session;
 import org.hibernate.criterion.Projections;
 import org.hibernate.criterion.Restrictions;
 
 import java.sql.Timestamp;
 import java.util.List;
 
 public class LastLinkReadingController extends AbstractController<LastLinkReading> {
 
     /**
      * static instance(ourInstance) initialized as null.
      */
     private static LastLinkReadingController ourInstance = null;
 
     /**
      * Public constructor .
      */
     public LastLinkReadingController() {
         // Does nothing
         super();
     }
 
     /**
      * LastNodeReadingController is loaded on the first execution of
      * LastNodeReadingController.getInstance() or the first access to
      * LastNodeReadingController.ourInstance, not before.
      *
      * @return ourInstance
      */
     public static LastLinkReadingController getInstance() {
         synchronized (LastLinkReadingController.class) {
             if (ourInstance == null) {
                 ourInstance = new LastLinkReadingController();
             }
         }
 
         return ourInstance;
     }
 
     /**
      * Returns the last reading inserted in the persistence for a specific link & capability
      *
      * @param link       , a link.
      * @param capability , a capability.
      * @return the last node reading.
      */
     public LastLinkReading getByID(final Link link, final Capability capability) {
         final Session session = this.getSessionFactory().getCurrentSession();
         LastLinkReading lastLinkReading = new LastLinkReading();
         lastLinkReading.setLink(link);
         lastLinkReading.setCapability(capability);
         return (LastLinkReading) session.get(LastLinkReading.class, lastLinkReading);
     }
 
     /**
      * Returns a list of last reading rows inserted in the persistence for a specific link
      * @param link , a link.
      * @return a list of last reading rows for each capability.
      */
     public List<LastLinkReading> getByLink(final Link link){
         final Session session = this.getSessionFactory().getCurrentSession();
         Criteria criteria = session.createCriteria(LastLinkReading.class);
         criteria.add(Restrictions.eq("link", link));
         return (List<LastLinkReading>) criteria.list();
     }
 
     /**
      * Returns a list of last reading rows inserted in the persistence for a specific capability
      * @param capability , a capability.
      * @return a list of last reading rows for each capability.
      */
     public List<LastLinkReading> getByCapability(final Capability capability){
         final Session session = this.getSessionFactory().getCurrentSession();
         Criteria criteria = session.createCriteria(LastLinkReading.class);
         criteria.add(Restrictions.eq("capability",capability));
         return (List<LastLinkReading>) criteria.list();
     }
 
     /**
      * Returns the latest node reading fo the LastNodeReadings of all capabilities
      * @param link , a link.
      * @return a LastNodeReading entry
      */
     public LastLinkReading getLatestLinkReading(final Link link){
         final Session session = this.getSessionFactory().getCurrentSession();
 
         // get max timestamp
         Criteria criteria = session.createCriteria(LastLinkReading.class);
         criteria.add(Restrictions.eq("link",link));
         criteria.setProjection(Projections.max("timestamp"));
         criteria.setMaxResults(1);
         Timestamp maxTimestamp = (Timestamp) criteria.uniqueResult();
 
         // get latest node reading by comparing it with max timestamp
         criteria = session.createCriteria(LastLinkReading.class);
         criteria.add(Restrictions.eq("link",link));
         criteria.add(Restrictions.eq("timestamp",maxTimestamp));
         criteria.setMaxResults(1);
         return (LastLinkReading) criteria.uniqueResult();
     }
 
     /**
      * Returns the latest link reading fo the LastNodeReadings of all capabilities
      * @param capability , a capability.
      * @return a LastNodeReading entry
      */
     public LastLinkReading getLatestLinkReading(final Capability capability){
         final Session session = this.getSessionFactory().getCurrentSession();
 
         // get max timestamp
         Criteria criteria = session.createCriteria(LastLinkReading.class);
         criteria.add(Restrictions.eq("capability",capability));
         criteria.setProjection(Projections.max("timestamp"));
         criteria.setMaxResults(1);
        Timestamp maxTimestamp = (Timestamp) criteria.uniqueResult();
 
         // get latest link reading by comparing it with max timestamp
         criteria = session.createCriteria(LastLinkReading.class);
         criteria.add(Restrictions.eq("capability",capability));
         criteria.add(Restrictions.eq("timestamp",maxTimestamp));
         criteria.setMaxResults(1);
         return (LastLinkReading) criteria.uniqueResult();
     }
 }
