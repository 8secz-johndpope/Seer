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
 
 import org.fedoraproject.candlepin.audit.Event;
 import org.fedoraproject.candlepin.audit.EventAdapter;
 import org.fedoraproject.candlepin.audit.EventFactory;
 import org.fedoraproject.candlepin.audit.EventSink;
 import org.fedoraproject.candlepin.controller.PoolManager;
 import org.fedoraproject.candlepin.exceptions.BadRequestException;
 import org.fedoraproject.candlepin.exceptions.CandlepinException;
 import org.fedoraproject.candlepin.exceptions.IseException;
 import org.fedoraproject.candlepin.exceptions.NotFoundException;
 import org.fedoraproject.candlepin.model.Consumer;
 import org.fedoraproject.candlepin.model.ConsumerCurator;
 import org.fedoraproject.candlepin.model.Entitlement;
 import org.fedoraproject.candlepin.model.EventCurator;
 import org.fedoraproject.candlepin.model.ExporterMetadata;
 import org.fedoraproject.candlepin.model.ExporterMetadataCurator;
 import org.fedoraproject.candlepin.model.ImportRecord;
 import org.fedoraproject.candlepin.model.ImportRecordCurator;
 import org.fedoraproject.candlepin.model.Owner;
 import org.fedoraproject.candlepin.model.OwnerCurator;
 import org.fedoraproject.candlepin.model.OwnerInfo;
 import org.fedoraproject.candlepin.model.OwnerInfoCurator;
 import org.fedoraproject.candlepin.model.OwnerPermission;
 import org.fedoraproject.candlepin.model.OwnerPermissionCurator;
 import org.fedoraproject.candlepin.model.Pool;
 import org.fedoraproject.candlepin.model.PoolCurator;
 import org.fedoraproject.candlepin.model.ProductCurator;
 import org.fedoraproject.candlepin.model.Subscription;
 import org.fedoraproject.candlepin.model.SubscriptionCurator;
 import org.fedoraproject.candlepin.model.SubscriptionToken;
 import org.fedoraproject.candlepin.model.SubscriptionTokenCurator;
 import org.fedoraproject.candlepin.model.User;
 import org.fedoraproject.candlepin.pinsetter.tasks.RefreshPoolsJob;
 import org.fedoraproject.candlepin.service.SubscriptionServiceAdapter;
 import org.fedoraproject.candlepin.service.UserServiceAdapter;
 import org.fedoraproject.candlepin.sync.Importer;
 import org.fedoraproject.candlepin.sync.ImporterException;
 import org.fedoraproject.candlepin.sync.SyncDataFormatException;
 
 import com.google.inject.Inject;
 import com.wideplay.warp.persist.Transactional;
 
 import org.apache.log4j.Logger;
 import org.jboss.resteasy.annotations.providers.jaxb.Wrapped;
 import org.jboss.resteasy.plugins.providers.atom.Feed;
 import org.jboss.resteasy.plugins.providers.multipart.InputPart;
 import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
 import org.jboss.resteasy.util.GenericType;
 import org.quartz.JobDetail;
 import org.xnap.commons.i18n.I18n;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.Date;
 import java.util.LinkedList;
 import java.util.List;
 
 import javax.ws.rs.Consumes;
 import javax.ws.rs.DELETE;
 import javax.ws.rs.DefaultValue;
 import javax.ws.rs.GET;
 import javax.ws.rs.POST;
 import javax.ws.rs.PUT;
 import javax.ws.rs.Path;
 import javax.ws.rs.PathParam;
 import javax.ws.rs.Produces;
 import javax.ws.rs.QueryParam;
 import javax.ws.rs.core.MediaType;
 import javax.xml.bind.DatatypeConverter;
 import org.fedoraproject.candlepin.auth.interceptor.Verify;
 
 /**
  * Owner Resource
  */
 @Path("/owners")
 public class OwnerResource {
     private OwnerCurator ownerCurator;
     private OwnerInfoCurator ownerInfoCurator;
     private PoolCurator poolCurator;
     private SubscriptionCurator subscriptionCurator;
     private SubscriptionTokenCurator subscriptionTokenCurator;
     private UserServiceAdapter userService;
     private SubscriptionServiceAdapter subService;
     private ConsumerCurator consumerCurator;
     private I18n i18n;
     private EventSink sink;
     private EventFactory eventFactory;
     private EventAdapter eventAdapter;
     private static Logger log = Logger.getLogger(OwnerResource.class);
     private EventCurator eventCurator;
     private ProductCurator productCurator;
     private Importer importer;
     private ExporterMetadataCurator exportCurator;
     private ImportRecordCurator importRecordCurator;
     private OwnerPermissionCurator permissionCurator;
     private PoolManager poolManager;
     private static final int FEED_LIMIT = 1000;
 
     @Inject
     public OwnerResource(OwnerCurator ownerCurator, PoolCurator poolCurator,
         ProductCurator productCurator, SubscriptionCurator subscriptionCurator,
         SubscriptionTokenCurator subscriptionTokenCurator,
         ConsumerCurator consumerCurator, I18n i18n,
         UserServiceAdapter userService, EventSink sink,
         EventFactory eventFactory, EventCurator eventCurator,
         EventAdapter eventAdapter, Importer importer, PoolManager poolManager,
         ExporterMetadataCurator exportCurator,
         OwnerInfoCurator ownerInfoCurator,
         ImportRecordCurator importRecordCurator,
         SubscriptionServiceAdapter subService,
         OwnerPermissionCurator permCurator) {
 
         this.ownerCurator = ownerCurator;
         this.ownerInfoCurator = ownerInfoCurator;
         this.productCurator = productCurator;
         this.poolCurator = poolCurator;
         this.subscriptionCurator = subscriptionCurator;
         this.subscriptionTokenCurator = subscriptionTokenCurator;
         this.consumerCurator = consumerCurator;
         this.userService = userService;
         this.i18n = i18n;
         this.sink = sink;
         this.eventFactory = eventFactory;
         this.eventCurator = eventCurator;
         this.importer = importer;
         this.exportCurator = exportCurator;
         this.importRecordCurator = importRecordCurator;
         this.poolManager = poolManager;
         this.eventAdapter = eventAdapter;
         this.subService = subService;
         this.permissionCurator = permCurator;
     }
 
     /**
      * Return list of Owners.
      * 
      * @return list of Owners
      */
     @GET
     @Produces(MediaType.APPLICATION_JSON)
     @Wrapped(element = "owners")
     public List<Owner> list(@QueryParam("key") String keyFilter) {
 
         // For now, assuming key filter is just one key:
         if (keyFilter != null) {
             List<Owner> results = new LinkedList<Owner>();
             Owner o = ownerCurator.lookupByKey(keyFilter);
             if (o != null) {
                 results.add(o);
             }
             return results;
         }
 
         return ownerCurator.listAll();
     }
 
     /**
      * Return the owner identified by the given ID.
      * 
      * @param ownerKey Owner ID.
      * @return the owner identified by the given id.
      */
     @GET
     @Path("/{owner_key}")
     @Produces(MediaType.APPLICATION_JSON)
     public Owner getOwner(@PathParam("owner_key") String ownerKey) {
         return findOwner(ownerKey);
     }
 
     /**
      * Return the owner's info identified by the given ID.
      * 
      * @param ownerKey Owner ID.
      * @return the info of the owner identified by the given id.
      */
     @GET
     @Path("/{owner_key}/info")
     @Produces(MediaType.APPLICATION_JSON)
     public OwnerInfo getOwnerInfo(@PathParam("owner_key") String ownerKey) {
         Owner owner = findOwner(ownerKey);
         return ownerInfoCurator.lookupByOwner(owner);
     }
 
     /**
      * Creates a new Owner
      * 
      * @return the new owner
      */
     @POST
     @Produces(MediaType.APPLICATION_JSON)
     public Owner createOwner(Owner owner) {
         Owner parent = owner.getParentOwner();
         if (parent != null && ownerCurator.find(parent.getId()) == null) {
             throw new BadRequestException(i18n.tr(
                 "Cound not create the Owner: {0}. Parent {1} does not exist.",
                 owner, parent));
         }
         Owner toReturn = ownerCurator.create(owner);
 
         sink.emitOwnerCreated(owner);
 
         if (toReturn != null) {
             return toReturn;
         }
 
         throw new BadRequestException(i18n.tr(
             "Cound not create the Owner: {0}", owner));
     }
 
     /**
      * Deletes an owner
      */
     @DELETE
     @Path("/{owner_key}")
     @Produces(MediaType.APPLICATION_JSON)
     public void deleteOwner(@PathParam("owner_key") String ownerKey,
         @QueryParam("revoke") @DefaultValue("true") boolean revoke) {
         Owner owner = findOwner(ownerKey);
         Event e = eventFactory.ownerDeleted(owner);
 
         cleanupAndDelete(owner, revoke);
 
         sink.sendEvent(e);
     }
 
     private void cleanupAndDelete(Owner owner, boolean revokeCerts) {
         log.info("Cleaning up owner: " + owner);
         if (!userService.isReadyOnly()) {
             for (User u : userService.listByOwner(owner)) {
                 userService.deleteUser(u);
             }
         }
         for (Consumer c : consumerCurator.listByOwner(owner)) {
             log.info("Deleting consumer: " + c);
 
             if (revokeCerts) {
                 poolManager.revokeAllEntitlements(c);
             }
             else {
                 // otherwise just remove them without touching the CRL
                 poolManager.removeAllEntitlements(c);
             }
 
             // need to check if this has been removed due to a
             // parent being deleted
             // TODO: There has to be a more efficient way to do this...
             c = consumerCurator.find(c.getId());
             if (c != null) {
                 consumerCurator.delete(c);
             }
         }
         for (SubscriptionToken token : subscriptionTokenCurator
             .listByOwner(owner)) {
             log.info("Deleting subscription token: " + token);
             subscriptionTokenCurator.delete(token);
         }
         for (Subscription s : subscriptionCurator.listByOwner(owner)) {
             log.info("Deleting subscription: " + s);
             subscriptionCurator.delete(s);
         }
         for (Pool p : poolCurator.listByOwner(owner)) {
             log.info("Deleting pool: " + p);
             poolCurator.delete(p);
         }
         ExporterMetadata m = exportCurator.lookupByTypeAndOwner(
             ExporterMetadata.TYPE_PER_USER, owner);
         if (m != null) {
             log.info("Deleting export metadata: " + m);
             exportCurator.delete(m);
         }
         for (ImportRecord record : importRecordCurator.findRecords(owner)) {
             log.info("Deleting import record:  " + record);
             importRecordCurator.delete(record);
         }
         
         for (OwnerPermission perm : permissionCurator.findByOwner(owner)) {
             log.info("Deleting permission: " + perm.getAccess());
            perm.getRole().getPermissions().remove(perm);
             permissionCurator.delete(perm);
         }
 
         log.info("Deleting owner: " + owner);
         ownerCurator.delete(owner);
     }
 
     /**
      * Return the entitlements for the owner of the given id.
      * 
      * @param ownerKey id of the owner whose entitlements are sought.
      * @return the entitlements for the owner of the given id.
      */
     @GET
     @Produces(MediaType.APPLICATION_JSON)
     @Path("{owner_key}/entitlements")
     public List<Entitlement> ownerEntitlements(
         @PathParam("owner_key") String ownerKey) {
         Owner owner = findOwner(ownerKey);
 
         List<Entitlement> toReturn = new LinkedList<Entitlement>();
         for (Pool pool : owner.getPools()) {
             toReturn.addAll(poolCurator.entitlementsIn(pool));
         }
 
         return toReturn;
     }
 
     /**
      * Return the consumers for the owner of the given id.
      * 
      * @param ownerKey id of the owner whose consumers are sought.
      * @return the consumers for the owner of the given id.
      */
     @GET
     @Produces(MediaType.APPLICATION_JSON)
     @Path("{owner_key}/consumers")
     public List<Consumer> ownerConsumers(@PathParam("owner_key") String ownerKey) {
 
         Owner owner = findOwner(ownerKey);
         return new LinkedList<Consumer>(owner.getConsumers());
     }
 
     /**
      * Return the entitlement pools for the owner of the given id.
      * 
      * @param ownerKey id of the owner whose entitlement pools are sought.
      * @return the entitlement pools for the owner of the given id.
      */
     @GET
     @Produces(MediaType.APPLICATION_JSON)
     @Path("{owner_key}/pools")
     public List<Pool> ownerEntitlementPools(
         @PathParam("owner_key") String ownerKey,
         @QueryParam("consumer") String consumerUuid,
         @QueryParam("product") String productId,
         @QueryParam("listall") @DefaultValue("false") boolean listAll,
         @QueryParam("activeon") String activeOn) {
 
         Owner owner = findOwner(ownerKey);
 
         Date activeOnDate = new Date();
         if (activeOn != null) {
             activeOnDate = parseActiveOnString(activeOn);
         }
 
         Consumer c = null;
         if (consumerUuid != null) {
             c = consumerCurator.findByUuid(consumerUuid);
             if (c == null) {
                 throw new NotFoundException(i18n.tr("consumer: {0} not found",
                     consumerUuid));
             }
             if (c.getOwner().getId().equals(owner.getId())) {
                 throw new BadRequestException(
                     "Consumer specified does not belong to owner on path");
             }
         }
         return poolCurator.listAvailableEntitlementPools(c, owner, productId,
             activeOnDate, true, listAll);
     }
 
     // ----- User -----
     /**
      * Associates a user with this owner.
      *
      * @param ownerKey
      * @param userLogin
      * @return the associated user
      *
      * @deprecated use the membership resource instead!
      */
     @POST
     @Consumes(MediaType.APPLICATION_JSON)
     @Produces(MediaType.APPLICATION_JSON)
     @Path("{owner_key}/users")
     @Deprecated
     public User associateUser(@PathParam("owner_key") String ownerKey,
                               @QueryParam("user_login") String userLogin) {
         User user = findUser(userLogin);
 
         // HEY LOOK!  THIS IS NOT ACTUALLY DOING ANYTHING!
 
         return user;
     }
 
     @POST
     @Consumes(MediaType.APPLICATION_JSON)
     @Produces(MediaType.APPLICATION_JSON)
     @Path("{owner_key}/subscriptions")
     public Subscription createSubscription(
         @PathParam("owner_key") @Verify(Owner.class) String ownerKey,
         Subscription subscription) {
         Owner o = findOwner(ownerKey);
         subscription.setOwner(o);
         return subService.createSubscription(subscription);
     }
 
     @GET
     @Produces("application/atom+xml")
     @Path("{owner_key}/atom")
     public Feed getOwnerAtomFeed(@PathParam("owner_key")
             @Verify(Owner.class) String ownerKey) {
         Owner o = findOwner(ownerKey);
         String path = String.format("/owners/%s/atom", ownerKey);
         Feed feed = this.eventAdapter.toFeed(
             this.eventCurator.listMostRecent(FEED_LIMIT, o), path);
         feed.setTitle("Event feed for owner " + o.getDisplayName());
         return feed;
     }
 
     @GET
     @Produces("application/atom+xml")
     @Path("{owner_key}/consumers/{consumer_uuid}/atom")
     public Feed getConsumerAtomFeed(
         @PathParam("owner_key") @Verify(Owner.class) String ownerKey,
         @PathParam("consumer_uuid") String consumerUuid) {
         String path = String.format("/consumers/%s/atom", consumerUuid);
         Consumer consumer = findConsumer(consumerUuid);
         Feed feed = this.eventAdapter.toFeed(
             this.eventCurator.listMostRecent(FEED_LIMIT, consumer), path);
         feed.setTitle("Event feed for consumer " + consumer.getUuid());
         return feed;
     }
 
     @GET
     @Produces(MediaType.APPLICATION_JSON)
     @Path("{owner_key}/events")
     public List<Event> getEvents(
         @PathParam("owner_key") @Verify(Owner.class) String ownerKey) {
         Owner o = findOwner(ownerKey);
         List<Event> events = this.eventCurator.listMostRecent(FEED_LIMIT, o);
         if (events != null) {
             eventAdapter.addMessageText(events);
         }
         return events;
     }
 
     @GET
     @Consumes(MediaType.APPLICATION_JSON)
     @Produces(MediaType.APPLICATION_JSON)
     @Path("{owner_key}/subscriptions")
     public List<Subscription> getSubscriptions(
         @PathParam("owner_key") @Verify(Owner.class) String ownerKey) {
         Owner o = findOwner(ownerKey);
         return subService.getSubscriptions(o);
     }
 
     /**
      * @param ownerKey
      * @return list of users under that owner name
      */
     @GET
     @Consumes(MediaType.APPLICATION_JSON)
     @Produces(MediaType.APPLICATION_JSON)
     @Path("{owner_key}/users")
     public List<User> getUsers(
         @PathParam("owner_key") @Verify(Owner.class) String ownerKey) {
         Owner o = findOwner(ownerKey);
         return userService.listByOwner(o);
     }
 
     private Owner findOwner(String key) {
         Owner owner = ownerCurator.lookupByKey(key);
 
         if (owner == null) {
             throw new NotFoundException(i18n.tr(
                 "owner with key: {0} was not found.", key));
         }
 
         return owner;
     }
 
     private User findUser(String login) {
         User user = userService.findByLogin(login);
 
         if (user == null) {
             throw new NotFoundException(i18n.tr(
                 "user with login: {0} was not found.", login));
         }
 
         return user;
     }
 
     private Consumer findConsumer(String consumerUuid) {
         Consumer consumer = consumerCurator.findByUuid(consumerUuid);
 
         if (consumer == null) {
             throw new NotFoundException(i18n.tr("No such consumer: {0}",
                 consumerUuid));
         }
         return consumer;
     }
 
     /**
      * expose updates for owners
      * 
      * @param key
      * @param owner
      * @return the update {@link Owner}
      */
     @PUT
     @Produces(MediaType.APPLICATION_JSON)
     @Path("{owner_key}")
     @Transactional
     public Owner updateOwner(@PathParam("owner_key") @Verify(Owner.class) String key,
         Owner owner) {
         Owner toUpdate = findOwner(key);
         log.debug("Updating");
         toUpdate.setDisplayName(owner.getDisplayName());
         toUpdate.setKey(owner.getKey());
         toUpdate.setParentOwner(owner.getParentOwner());
         ownerCurator.merge(toUpdate);
         return toUpdate;
     }
 
     /**
      * 'Tickle' an owner to have all of their entitlement pools synced with
      * their subscriptions. This method (and the one below may not be entirely
      * RESTful, as the updated data is not supplied as an argument.
      * 
      * @param ownerKey unique id key of the owner whose pools should be updated
      * @return the status of the pending job
      */
     @PUT
     @Produces(MediaType.APPLICATION_JSON)
     @Path("{owner_key}/subscriptions")
     public JobDetail refreshPools(
         // TODO: Can we verify with autocreate?
         @PathParam("owner_key") @Verify(Owner.class) String ownerKey,
         @QueryParam("auto_create_owner") @DefaultValue("false") Boolean autoCreateOwner) {
 
         Owner owner = ownerCurator.lookupByKey(ownerKey);
         if (owner == null) {
             if (autoCreateOwner) {
                 owner = this.createOwner(new Owner(ownerKey, ownerKey));
             }
             else {
                 throw new NotFoundException(i18n.tr(
                     "owner with key: {0} was not found.", ownerKey));
             }
         }
 
         return RefreshPoolsJob.forOwner(owner);
     }
 
     @PUT
     @Path("/subscriptions")
     public void updateSubscription(Subscription subscription) {
         // TODO: Do we even need the owner id here?
         Subscription existingSubscription = this.subscriptionCurator
             .find(subscription.getId());
         if (existingSubscription == null) {
             throw new NotFoundException(i18n.tr(
                 "subscription with id: {0} not found.", subscription.getId()));
         }
         this.subscriptionCurator.merge(subscription);
     }
 
     @POST
     @Path("{owner_key}/imports")
     @Consumes(MediaType.MULTIPART_FORM_DATA)
     public void importData(@PathParam("owner_key") @Verify(Owner.class) String ownerKey,
         MultipartInput input) {
         Owner owner = findOwner(ownerKey);
 
         try {
             InputPart part = input.getParts().get(0);
             File archive = part.getBody(new GenericType<File>() {
             });
             log.info("Importing archive: " + archive.getAbsolutePath());
             importer.loadExport(owner, archive);
 
             sink.emitImportCreated(owner);
             recordImportSuccess(owner);
         }
         catch (IOException e) {
             recordImportFailure(owner, e);
             throw new IseException(i18n.tr("Error reading export archive"), e);
         }
         catch (SyncDataFormatException e) {
             recordImportFailure(owner, e);
             throw new BadRequestException(
                 i18n.tr("Bad data in export archive"), e);
         }
         // These come back with internationalized messages, so we can transfer:
         catch (ImporterException e) {
             recordImportFailure(owner, e);
             throw new IseException(e.getMessage(), e);
         }
         // Grab candlepin exceptions to record the error and then rethrow
         // to pass on the http return code
         catch (CandlepinException e) {
             recordImportFailure(owner, e);
             throw e;
         }
     }
 
     @GET
     @Path("{owner_key}/imports")
     public List<ImportRecord> getImports(
         @PathParam("owner_key") @Verify(Owner.class) String ownerKey) {
         Owner owner = findOwner(ownerKey);
         
         return this.importRecordCurator.findRecords(owner);
     }
 
     private void recordImportSuccess(Owner owner) {
         ImportRecord record = new ImportRecord(owner);
         record.recordStatus(ImportRecord.Status.SUCCESS,
             i18n.tr("{0} file imported successfully.", owner.getKey()));
 
         this.importRecordCurator.create(record);
     }
 
     private void recordImportFailure(Owner owner, Throwable error) {
         ImportRecord record = new ImportRecord(owner);
         record.recordStatus(ImportRecord.Status.FAILURE, error.getMessage());
 
         this.importRecordCurator.create(record);
     }
 
 
     private Date parseActiveOnString(String activeOn) {
         Date d;
         try {
             d = DatatypeConverter.parseDateTime(activeOn).getTime();
         }
         catch (IllegalArgumentException e) {
             throw new BadRequestException(
                 "Invalid date, must use ISO 8601 format");
         }
         return d;
     }
 
 }
