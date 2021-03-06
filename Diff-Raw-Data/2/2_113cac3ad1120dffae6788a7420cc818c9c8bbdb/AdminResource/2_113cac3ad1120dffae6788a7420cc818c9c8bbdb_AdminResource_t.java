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
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 
 import javax.ws.rs.Consumes;
 import javax.ws.rs.GET;
 import javax.ws.rs.POST;
 import javax.ws.rs.Path;
 import javax.ws.rs.Produces;
 import javax.ws.rs.core.MediaType;
 
 import org.apache.log4j.Logger;
 import org.fedoraproject.candlepin.auth.Role;
 import org.fedoraproject.candlepin.auth.interceptor.AllowRoles;
 import org.fedoraproject.candlepin.model.ConsumerType;
 import org.fedoraproject.candlepin.model.ConsumerTypeCurator;
 import org.fedoraproject.candlepin.model.Owner;
 import org.fedoraproject.candlepin.model.OwnerCurator;
 import org.fedoraproject.candlepin.model.User;
 import org.fedoraproject.candlepin.model.ConsumerType.ConsumerTypeEnum;
 import org.fedoraproject.candlepin.service.UserServiceAdapter;
 import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
 import org.jboss.resteasy.util.GenericType;
 
 import com.google.inject.Inject;
 
 /**
  * Candlepin server administration REST calls.
  */
 @Path("/admin")
 public class AdminResource {
 
     private static Logger log = Logger.getLogger(AdminResource.class);
     
     private ConsumerTypeCurator consumerTypeCurator;
     private OwnerCurator ownerCurator;
     private UserServiceAdapter userService;
 
     @Inject
     public AdminResource(ConsumerTypeCurator consumerTypeCurator, 
         OwnerCurator ownerCurator, UserServiceAdapter userService) {
         this.consumerTypeCurator = consumerTypeCurator;
         this.ownerCurator = ownerCurator;
         this.userService = userService;
     }
 
     /**
      * Initialize the Candlepin database.
      * 
      * Currently this just creates static'ish database entries for things like
      * consumer types. This call needs to happen once after a database is created.
      * Repeated calls are not required, but will be harmless.
      * 
      * @return Description if db was or already is initialized.
      */
     @GET
     @Produces({MediaType.TEXT_PLAIN})
     @Path("init")
     @AllowRoles(roles = {Role.NO_AUTH})
     public String initialize() {
         log.debug("Called initialize()");
 
         // First, determine if we've already setup the DB and if so, do *nothing*!
         ConsumerType systemType = consumerTypeCurator.lookupByLabel(
             ConsumerTypeEnum.SYSTEM.getLabel());
         if (systemType != null) {
             log.info("Database already initialized.");
             return "Already initialized.";
         }
         log.info("Initializing Candlepin database.");
 
         for (ConsumerTypeEnum type : ConsumerTypeEnum.values()) {
             ConsumerType created = new ConsumerType(type); 
             consumerTypeCurator.create(created);
             log.debug("Created: " + created);
         }
 
         log.info("Creating Admin owner.");
         Owner adminOwner = ownerCurator.create(new Owner("admin"));
         
         log.info("Creating default super admin.");
         try {
             User defaultAdmin = new User(adminOwner, "admin", "admin", true);
             userService.createUser(defaultAdmin);
         } 
         catch (UnsupportedOperationException e) {
             log.info("Admin creation is not supported!");
         }
 
         return "Initialized!";
     }
     
     @POST
     @Path("import")
     @AllowRoles(roles = Role.SUPER_ADMIN)
     @Consumes(MediaType.MULTIPART_FORM_DATA)
     public void importData(MultipartInput input) {
 
         try {
             InputPart part = input.getParts().get(0);
             log.info("part: " + part.getBodyAsString());
             File archive = part.getBody(new GenericType<File>(){});
             log.info("Importing archive: " + archive.getAbsolutePath());
 
             File export = new File("/tmp/candlepin/export.tar.gz");
             FileOutputStream fos = new FileOutputStream(export);
             FileInputStream fis = new FileInputStream(archive);
 
             int c;
             while ((c = fis.read()) != -1) {
                 fos.write(c);
             }
             fis.close();
             fos.close();
         }
         catch (IOException e) {
             // TODO
             log.error(e);
         }
     }
 
 }
