 package woko.hibernate;
 
 import net.sourceforge.stripes.action.Resolution;
 import net.sourceforge.stripes.controller.ExecutionContext;
 import net.sourceforge.stripes.controller.Intercepts;
 import net.sourceforge.stripes.controller.LifecycleStage;
 import org.hibernate.Session;
 import org.hibernate.SessionFactory;
 import org.hibernate.Transaction;
 import woko.Woko;
 import woko.util.WLogger;
 
 @Intercepts({LifecycleStage.RequestInit, LifecycleStage.RequestComplete})
 public class HibernateTxInterceptor implements net.sourceforge.stripes.controller.Interceptor {
 
   private static final WLogger log = WLogger.getLogger(HibernateTxInterceptor.class);
 
   private SessionFactory getSessionFactory(ExecutionContext context) {
     Woko woko = Woko.getWoko(context.getActionBeanContext().getServletContext());
     HibernateStore hs = (HibernateStore)woko.getObjectStore();
     return hs.getSessionFactory();
   }
 
   public Resolution intercept(ExecutionContext context) throws Exception {
     LifecycleStage stage = context.getLifecycleStage();
     if (stage==LifecycleStage.RequestInit) {
       Transaction tx = getSessionFactory(context).getCurrentSession().beginTransaction();
       log.debug("Started transaction : " + tx);
 
     } else if (stage.equals(LifecycleStage.RequestComplete)) {
 
       Transaction tx = getSessionFactory(context).getCurrentSession().getTransaction();
       if (tx==null) {
         log.debug("No transaction found, nothing to do.");
       } else {
         try {
           log.debug("Commiting transaction " + tx);
           tx.commit();
         } catch(Exception e) {
          log.error("Commit error : $e", e);
           tx.rollback();
           throw e;
         }
       }
     }
 
     try {
       return context.proceed();
     } catch(Exception e) {
       log.error("Exception while proceeding with context, rollbacking transaction if any, exception will be rethrown", e);
       Session session = getSessionFactory(context).getCurrentSession();
       if (session!=null) {
         Transaction tx = session.getTransaction();
         if (tx!=null) {
           try {
             tx.rollback();
           } catch(Exception e2) {
             log.error("Exception while rollbacking", e2);
           }
         }
       }
       // re-throw exception
       throw e;
     }
   }
 
 }
