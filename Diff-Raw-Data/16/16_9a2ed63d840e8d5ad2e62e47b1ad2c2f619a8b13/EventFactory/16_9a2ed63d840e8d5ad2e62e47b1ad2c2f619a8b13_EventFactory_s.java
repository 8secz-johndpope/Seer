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
 package org.fedoraproject.candlepin.audit;
 
 import java.util.List;
 
 import org.codehaus.jackson.JsonGenerator;
 import org.codehaus.jackson.map.AnnotationIntrospector;
 import org.codehaus.jackson.map.ObjectMapper;
 import org.codehaus.jackson.map.ObjectWriter;
 import org.codehaus.jackson.map.SerializationConfig;
 import org.codehaus.jackson.map.SerializerProvider;
 import org.codehaus.jackson.map.introspect.BasicBeanDescription;
 import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
 import org.codehaus.jackson.map.ser.BeanPropertyWriter;
 import org.codehaus.jackson.map.ser.BeanSerializer;
 import org.codehaus.jackson.map.ser.CustomSerializerFactory;
 import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
 import org.fedoraproject.candlepin.auth.Principal;
 import org.fedoraproject.candlepin.guice.PrincipalProvider;
 import org.fedoraproject.candlepin.model.Consumer;
 import org.fedoraproject.candlepin.model.Entitlement;
 import org.fedoraproject.candlepin.model.Owner;
 import org.fedoraproject.candlepin.model.Pool;
 import org.fedoraproject.candlepin.model.Subscription;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.google.inject.Inject;
 
 /**
  * EventFactory
  */
 public class EventFactory {
     private final PrincipalProvider principalProvider;
     private final ObjectMapper mapper;
     private final ObjectWriter entitlementWriter;
     private static Logger logger = LoggerFactory.getLogger(EventFactory.class);
     @Inject
     public EventFactory(PrincipalProvider principalProvider) {
         this.principalProvider = principalProvider;
         
         AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
         AnnotationIntrospector secondary = new JaxbAnnotationIntrospector();
         AnnotationIntrospector pair = new AnnotationIntrospector.Pair(primary, secondary);
         mapper = new ObjectMapper();
         mapper.setSerializerFactory(new CandlepinSerializerFactory());
         mapper.getSerializationConfig().setAnnotationIntrospector(pair);
         mapper.getDeserializationConfig().setAnnotationIntrospector(pair);
         this.entitlementWriter = mapper.viewWriter(Event.Target.ENTITLEMENT.getClass());
     }
 
     public Event consumerCreated(Consumer newConsumer) {
         String newEntityJson = entityToJson(newConsumer);
         Principal principal = principalProvider.get();
         
         Event e = new Event(Event.Type.CREATED, Event.Target.CONSUMER, 
             principal, principal.getOwner().getId(), newConsumer.getId(),
             newConsumer.getId(), null, newEntityJson);
         return e;
     }
     
     public Event consumerModified(Consumer oldConsumer, Consumer newConsumer) {
         String oldEntityJson = entityToJson(oldConsumer);
         String newEntityJson = entityToJson(newConsumer);
         Principal principal = principalProvider.get();
         
         return new Event(Event.Type.MODIFIED, Event.Target.CONSUMER, 
             principal, principal.getOwner().getId(), oldConsumer.getId(),
             oldConsumer.getId(), oldEntityJson, newEntityJson);
     }
     
     public Event subscriptionModified(Subscription oldSub, Subscription newSub) {
         String olds = entityToJson(oldSub);
         String news = entityToJson(newSub);
         Principal principal = principalProvider.get();
         return new Event(Event.Type.MODIFIED, Event.Target.SUBSCRIPTION,
             principal, principal.getOwner().getId(), null, newSub.getId(), olds, news);
     }
 
     public Event consumerDeleted(Consumer oldConsumer) {
         String oldEntityJson = entityToJson(oldConsumer);
 
         Event e = new Event(Event.Type.DELETED, Event.Target.CONSUMER, 
             principalProvider.get(), oldConsumer.getOwner().getId(), oldConsumer.getId(),
             oldConsumer.getId(), oldEntityJson, null);
         return e;
     }
 
     public Event entitlementCreated(Entitlement e) {
         return entitlementEvent(e, Event.Type.CREATED);
     }
     
     public Event entitlementDeleted(Entitlement e) {
         return entitlementEvent(e, Event.Type.DELETED);
     }
 
     public Event entitlementChanged(Entitlement e) {
         return entitlementEvent(e, Event.Type.MODIFIED);
     }
 
     private Event entitlementEvent(Entitlement e, Event.Type type) {
         String json = serializeEntitlement(e);
         String old = null, latest = null;
         Owner owner = e.getOwner();
         if (type == Event.Type.DELETED) {
             old = json;
         }
         else {
             latest = json;
         }
         return new Event(type, Event.Target.ENTITLEMENT,
             principalProvider.get(), owner.getId(), e.getConsumer().getId(), e
                 .getId(), old, latest);
     }
 
     /**
      * @param e
      * @return
      */
     private String serializeEntitlement(Entitlement e) {
         try {
             return this.entitlementWriter.writeValueAsString(e);
         }
         catch (Exception e1) {
             logger.warn("Unable to jsonify: {}", e);
             logger.error("jsonification failed!", e1);
             return "";
         }
     }
 
     public Event ownerCreated(Owner newOwner) {
         String newEntityJson = entityToJson(newOwner);
         Event e = new Event(Event.Type.CREATED, Event.Target.OWNER, 
             principalProvider.get(), newOwner.getId(), null,
             newOwner.getId(), null, newEntityJson);
         return e;
     }
     
     public Event poolCreated(Pool newPool) {
         String newEntityJson = entityToJson(newPool);
         Owner o = newPool.getOwner();
         Event e = new Event(Event.Type.CREATED, Event.Target.POOL, 
             principalProvider.get(), o.getId(), null, newPool.getId(), null, newEntityJson);
         return e;
     }
     
     public Event poolChangedFrom(Pool before) {
         Owner o = before.getOwner();
         Event e = new Event(Event.Type.MODIFIED, Event.Target.POOL, principalProvider.get(),
             o.getId(), null, before.getId(), entityToJson(before), null);
         return e;
     }
     
     public void poolChangedTo(Event e, Pool after) {
         e.setNewEntity(entityToJson(after));
     }
     
     public Event poolDeleted(Pool pool) {
         String oldJson = entityToJson(pool);
         Owner o = pool.getOwner();
         Event e = new Event(Event.Type.DELETED, Event.Target.POOL,
             principalProvider.get(), o.getId(), null, pool.getId(), oldJson, null);
         return e;
     }
 
     public Event ownerDeleted(Owner owner) {
         Event e = new Event(Event.Type.DELETED, Event.Target.OWNER, principalProvider.get(),
             owner.getId(), null, owner.getId(), entityToJson(owner), null);
         return e;
     }
     
     public Event exportCreated(Consumer consumer) {
         Principal principal = principalProvider.get();
         Event e = new Event(Event.Type.CREATED, Event.Target.EXPORT, principal, 
             principal.getOwner().getId(), consumer.getId(), consumer.getId(), null,
             entityToJson(consumer));
         return e;
     }
     
     public Event importCreated(Owner owner) {
         Principal principal = principalProvider.get();
         Event e = new Event(Event.Type.CREATED, Event.Target.IMPORT, principal, 
             owner.getId(), null, owner.getId(), null, entityToJson(owner));
         return e;
     }
     
     public Event subscriptionCreated(Subscription subscription) {
         Principal principal = principalProvider.get();
 
         Event e = new Event(Event.Type.CREATED, Event.Target.SUBSCRIPTION,
             principal, principal.getOwner().getId(), null, subscription.getId(),
             null, entityToJson(subscription));
         return e;
     }
 
     private String entityToJson(Object entity) {
         String newEntityJson = "";
         // TODO: Throw an auditing exception here
     
         // Drop data on consumer we do not want serialized, Jackson doesn't seem to
         // care about XmlTransient annotations when used here:
     
         try {
             newEntityJson = mapper.writeValueAsString(entity);
         }
         catch (Exception e) {
             logger.warn("Unable to jsonify: {}", entity);
             logger.error("jsonification failed!", e);
         }
         return newEntityJson;
     }
     /**
      * ConsumerWriter
      */
     private class ConsumerWriter extends BeanPropertyWriter {
         public ConsumerWriter() {
             //ignore other nulls for now - we are not using it internally.
             super("consumerWriter", null, null, null, null, null, false, null);
         }
         
         @Override
         public void serializeAsField(Object bean, JsonGenerator jgen,
             SerializerProvider prov) throws Exception {
             if (prov.getSerializationView().equals(Event.Target.ENTITLEMENT.getClass())) {
                 Consumer consumer = ((Entitlement) bean).getConsumer();
                 jgen.writeObjectFieldStart("consumer");
                 jgen.writeStringField("uuid", consumer.getUuid());
                 jgen.writeObjectField("facts", consumer.getFacts());
                 jgen.writeEndObject();
             }
         }
     }
     
     private class CandlepinSerializerFactory extends CustomSerializerFactory{
         
         @Override
         protected BeanSerializer processViews(SerializationConfig config,
             BasicBeanDescription beanDesc, BeanSerializer ser,
             List<BeanPropertyWriter> props) {
             ser = super.processViews(config, beanDesc, ser, props);
             //serialize consumer for entitlement objects.
             if (beanDesc.getBeanClass() == Entitlement.class) {
                 BeanPropertyWriter[] writers = props
                     .toArray(new BeanPropertyWriter[props.size() + 1]);
                 writers[writers.length - 1] = new ConsumerWriter();
                 ser = ser.withFiltered(writers);
             }
             return ser;
         }
     }
 }
