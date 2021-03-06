 package org.jboss.as.quickstarts.kitchensinkrf.service;
 
 import org.jboss.as.quickstarts.kitchensinkrf.model.Member;
 
 import javax.ejb.Stateless;
 import javax.enterprise.event.Event;
 import javax.inject.Inject;
 import javax.persistence.EntityManager;
 import java.util.logging.Logger;
 
 // The @Stateless annotation eliminates the need for manual transaction demarcation
 @Stateless
 public class MemberRegistration {
 
    @Inject
    private Logger log;
 
    @Inject
    private EntityManager em;
 
    @Inject
    private Event<Member> memberEventSrc;
 
    public void register(Member member) throws Exception {
       log.info("Registering " + member.getName());
       em.persist(member);
       memberEventSrc.fire(member);
    }

   public Member findById(Long id) {
      log.info("Finding member by id " + id);
      return em.find(Member.class, id);
   }
 }
