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
 package org.fedoraproject.candlepin.resource;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import javax.ws.rs.Consumes;
 import javax.ws.rs.GET;
 import javax.ws.rs.POST;
 import javax.ws.rs.Path;
 import javax.ws.rs.PathParam;
 import javax.ws.rs.Produces;
 import javax.ws.rs.core.MediaType;
 
 import org.apache.log4j.Logger;
 import org.fedoraproject.candlepin.model.Consumer;
 import org.fedoraproject.candlepin.model.ConsumerCurator;
 import org.fedoraproject.candlepin.model.ConsumerInfo;
 import org.fedoraproject.candlepin.model.ConsumerType;
 import org.fedoraproject.candlepin.model.ObjectFactory;
 import org.fedoraproject.candlepin.model.Owner;
 import org.fedoraproject.candlepin.model.OwnerCurator;
 import org.fedoraproject.candlepin.model.Product;
 
import com.google.inject.Inject;

 /**
  * API Gateway for Consumers
  */
 @Path("/consumer")
 public class ConsumerResource extends BaseResource {
 
     private static Logger log = Logger.getLogger(ConsumerResource.class);
     private OwnerCurator ownerCurator;
     private ConsumerCurator consumerCurator;
 
    @Inject
     public ConsumerResource(OwnerCurator ownerCurator, ConsumerCurator consumerCurator) {
         super(Consumer.class);
 
         this.ownerCurator = ownerCurator;
         this.consumerCurator = consumerCurator;
     }
    
     /**
      * List available Consumers
      * @return list of available consumers.
      */
     @GET
     @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
     public List<Consumer> list() {
         List<Object> u = ObjectFactory.get().listObjectsByClass(getApiClass());
         List<Consumer> consumers = new ArrayList<Consumer>();
         for (Object o : u) {
             consumers.add((Consumer) o);
         }
         return consumers;
     }
    
     /**
      * Create a Consumer
      * @param ci Consumer metadata encapsulated in a ConsumerInfo.
      * @param type Consumer type
      * @return newly created Consumer
      */
     @POST
     @Consumes(MediaType.APPLICATION_JSON)
     @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
     public Consumer create(ConsumerInfo ci, ConsumerType type) {
 
         // TODO: Determine owner based on the authenticated user:
         Owner owner = ownerCurator.listAll().get(0);
 
 
         // TODO: Change to use proper constructor.
         Consumer c = new Consumer("consumer name?", owner, type);
         c.setInfo(ci);
 
 //        ObjectFactory.get().store(c);
         consumerCurator.create(c);
         return c;
     }
 
     /**
      * Returns the ConsumerInfo for the given Consumer.
      * @return the ConsumerInfo for the given Consumer.
      */
     @GET @Path("/info")
     @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
     // TODO: What consumer?
     public ConsumerInfo getInfo() {
         ConsumerInfo ci = new ConsumerInfo();
 //        ci.setType(new ConsumerType("system"));
         ci.setConsumer(null);
 //        Map<String,String> m = new HashMap<String,String>();
 //        m.put("cpu", "i386");
 //        m.put("hey", "biteme");
 //        ci.setMetadata(m);
         ci.setMetadataField("cpu", "i386");
         ci.setMetadataField("hey", "foobar");
         return ci;
     }
     
     /**
      * removes the product whose id matches pid, from the consumer, cid.
      * @param cid Consumer ID to affect
      * @param pid Product ID to remove from Consumer.
      */
 //    @DELETE @Path("{cid}/products/{pid}")
 //    public void delete(@PathParam("cid") String cid,
 //                       @PathParam("pid") String pid) {
 //        System.out.println("cid " + cid + " pid = " + pid);
 //        Consumer c = (Consumer) ObjectFactory.get().lookupByUUID(Consumer.class, cid);
 //        if (!c.getConsumedProducts().remove(pid)) {
 //            log.error("no product " + pid + " found.");
 //        }
 //    }
 
     /**
      * Returns the product whose id matches pid, from the consumer, cid.
      * @param cid Consumer ID to affect
      * @param pid Product ID to remove from Consumer.
      * @return the product whose id matches pid, from the consumer, cid.
      */
     @GET @Path("{cid}/products/{pid}")
     @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
     public Product getProduct(@PathParam("cid") String cid,
                        @PathParam("pid") String pid) {
         return null;
     }
 }
