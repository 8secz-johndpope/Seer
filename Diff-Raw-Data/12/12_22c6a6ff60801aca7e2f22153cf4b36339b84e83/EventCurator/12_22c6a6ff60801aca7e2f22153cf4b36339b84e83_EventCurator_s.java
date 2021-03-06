 /**
  * Copyright (c) 2009 Red Hat, Inc.
  *
  * This software is licensed to you under the GNU General Public License,
  * version 2 (GPLv2). There is NO WARRANTY for this software, express or
  * implied, including the implied warranties of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
  * along with this software; if not, see
  * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
  *
  * Red Hat trademarks are not licensed under GPLv2. No permission is
  * granted to use or replicate Red Hat trademarks that are incorporated
  * in this software or its documentation.
  */
 package org.fedoraproject.candlepin.model;
 
 
 import java.util.Date;
 import java.util.List;
 
 import org.fedoraproject.candlepin.audit.Event;
 import org.fedoraproject.candlepin.auth.interceptor.EnforceAccessControl;
 import org.hibernate.Criteria;
 import org.hibernate.criterion.Order;
 import org.hibernate.criterion.Restrictions;
 import org.jboss.resteasy.plugins.providers.atom.Content;
 import org.jboss.resteasy.plugins.providers.atom.Entry;
 import org.jboss.resteasy.plugins.providers.atom.Feed;
 
 /**
  * AttributeCurator
  */
 public class EventCurator extends AbstractHibernateCurator<Event> {
 
     protected EventCurator() {
         super(Event.class);
     }
 
     /**
      * Query events, most recent first.
      *
      * @return List of events.
      */
     @SuppressWarnings("unchecked")
     public List<Event> listMostRecent(int limit) {
         Criteria crit = createEventCriteria(limit);
         return crit.list();
     }
 
     /**
      * @param limit
      * @return
      */
     private Criteria createEventCriteria(int limit) {
         return currentSession().createCriteria(Event.class)
             .setMaxResults(limit).addOrder(Order.desc("timestamp"));
     }
     
     @SuppressWarnings("unchecked")
     @EnforceAccessControl
     public List<Event> listMostRecent(int limit, Owner owner) {
         return createEventCriteria(limit).add(
             Restrictions.eq("ownerId", owner.getId())).list();
     }
 
     @SuppressWarnings("unchecked")
     @EnforceAccessControl
     public List<Event> listMostRecent(int limit, Consumer consumer) {
         return createEventCriteria(limit).add(
             Restrictions.eq("consumerId", consumer.getId())).list();
     }
     
     /**
      * Convert the given list of Events into an Atom feed.
      * 
      * @param events List of events.
      * @return Atom feed for these events.
      */
     public Feed toFeed(List<Event> events) {
         Feed feed = new Feed();
         feed.setUpdated(new Date());
         for (Event e : events) {
             Entry entry = new Entry();
             entry.setTitle(e.getTarget().toString() + " " + e.getType().toString());
             entry.setPublished(e.getTimestamp());
 
             Content content = new Content();
             content.setJAXBObject(e);
             entry.setContent(content);
             feed.getEntries().add(entry);
         }
         // Use the most recent event as the feed's published time. Assumes events do not
         // get modified, if they do then the feed published date could be inaccurate.
         if (events.size() > 0) {
             feed.setUpdated(events.get(0).getTimestamp());
         }
 
         return feed;
     }
 
 
 }
