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
 package org.fedoraproject.candlepin.resource.test;
 
 import java.util.Collection;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Locale;
 import static org.mockito.Mockito.*;
 
 import org.apache.commons.lang.StringUtils;
 import org.fedoraproject.candlepin.audit.EventSink;
 import org.fedoraproject.candlepin.auth.NoAuthPrincipal;
 import org.fedoraproject.candlepin.auth.Principal;
 import org.fedoraproject.candlepin.auth.Access;
 import org.fedoraproject.candlepin.auth.TrustedUserPrincipal;
 import org.fedoraproject.candlepin.auth.UserPrincipal;
 import org.fedoraproject.candlepin.auth.permissions.Permission;
 import org.fedoraproject.candlepin.exceptions.BadRequestException;
 import org.fedoraproject.candlepin.exceptions.ForbiddenException;
 import org.fedoraproject.candlepin.exceptions.NotFoundException;
 import org.fedoraproject.candlepin.model.ActivationKey;
 import org.fedoraproject.candlepin.model.ActivationKeyCurator;
 import org.fedoraproject.candlepin.model.Consumer;
 import org.fedoraproject.candlepin.model.ConsumerCurator;
 import org.fedoraproject.candlepin.model.ConsumerType;
 import org.fedoraproject.candlepin.model.ConsumerTypeCurator;
 import org.fedoraproject.candlepin.model.IdentityCertificate;
 import org.fedoraproject.candlepin.model.Role;
 import org.fedoraproject.candlepin.model.Owner;
 import org.fedoraproject.candlepin.model.OwnerCurator;
 import org.fedoraproject.candlepin.model.OwnerPermission;
 import org.fedoraproject.candlepin.model.User;
 import org.fedoraproject.candlepin.resource.ConsumerResource;
 import org.fedoraproject.candlepin.service.IdentityCertServiceAdapter;
 import org.fedoraproject.candlepin.service.SubscriptionServiceAdapter;
 import org.fedoraproject.candlepin.service.UserServiceAdapter;
 import org.junit.Assert;
 import org.junit.Before;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.mockito.Mock;
 import org.mockito.invocation.InvocationOnMock;
 import org.mockito.runners.MockitoJUnitRunner;
 import org.mockito.stubbing.Answer;
 import org.xnap.commons.i18n.I18n;
 import org.xnap.commons.i18n.I18nFactory;
 
 import edu.emory.mathcs.backport.java.util.Collections;
 
 /**
  *
  */
 @RunWith(MockitoJUnitRunner.class)
 public class ConsumerResourceCreationTest {
 
     private static final String USER = "testuser";
 
     @Mock private UserServiceAdapter userService;
     @Mock private IdentityCertServiceAdapter idCertService;
     @Mock private SubscriptionServiceAdapter subscriptionService;
     @Mock private ConsumerCurator consumerCurator;
     @Mock private ConsumerTypeCurator consumerTypeCurator;
     @Mock private OwnerCurator ownerCurator;
     @Mock private EventSink sink;
     @Mock private ActivationKeyCurator activationKeyCurator;
     private I18n i18n;
 
     private ConsumerResource resource;
     private ConsumerType system;
     private Owner owner;
     private Role role;
 
     @Before
     public void init() throws Exception {
         this.i18n = I18nFactory.getI18n(getClass(), Locale.US, I18nFactory.FALLBACK);
 
         this.resource = new ConsumerResource(this.consumerCurator,
             this.consumerTypeCurator, null, this.subscriptionService, null,
             this.idCertService, null, this.i18n, this.sink, null, null, null,
             this.userService, null, null, null, null, this.ownerCurator,
             this.activationKeyCurator, null);
 
         this.system = new ConsumerType(ConsumerType.ConsumerTypeEnum.SYSTEM);
 
         owner = new Owner("test_owner");
         User user = new User(USER, "");
         OwnerPermission p = new OwnerPermission(owner, Access.ALL);
         role = new Role();
         role.addPermission(p);
         role.addUser(user);
 
         when(consumerCurator.create(any(Consumer.class))).thenAnswer(new Answer() {
             @Override
             public Object answer(InvocationOnMock invocation) throws Throwable {
                 return invocation.getArguments()[0];
             }
         });
         when(consumerTypeCurator.lookupByLabel(system.getLabel())).thenReturn(system);
         when(userService.findByLogin(USER)).thenReturn(user);
         when(idCertService.generateIdentityCert(any(Consumer.class)))
                 .thenReturn(new IdentityCertificate());
         when(ownerCurator.lookupByKey(owner.getKey())).thenReturn(owner);
     }
 
     private Consumer createConsumer(String consumerName) {
         Collection<Permission> perms = new HashSet<Permission>();
         perms.addAll(role.getPermissions());
         Principal principal = new UserPrincipal(USER, perms, false);
 
         return createConsumer(consumerName, principal, Collections.emptyList());
     }
 
     private Consumer createConsumer(String consumerName, Principal principal, 
         List<String> activationKeys) {
         Consumer consumer = new Consumer(consumerName, null, null, system);
         return this.resource.create(consumer, principal, USER, owner.getKey(), 
             createKeysString(activationKeys));
     }
 
     @Test
     public void acceptedConsumerName() {
         Assert.assertNotNull(createConsumer("test_user"));
     }
 
     @Test
     public void camelCaseName() {
         Assert.assertNotNull(createConsumer("ConsumerTest32953"));
     }
 
     @Test
     public void startsWithUnderscore() {
         Assert.assertNotNull(createConsumer("__init__"));
     }
 
     @Test
     public void startsWithDash() {
         Assert.assertNotNull(createConsumer("-dash"));
     }
 
     @Test
     public void containsNumbers() {
         Assert.assertNotNull(createConsumer("testmachine99"));
     }
 
     @Test
     public void startsWithNumbers() {
         Assert.assertNotNull(createConsumer("001test7"));
     }
 
     @Test
     public void containsPeriods() {
         Assert.assertNotNull(createConsumer("test-system.resource.net"));
     }
 
     @Test
     public void containsUserServiceChars() {
         Assert.assertNotNull(createConsumer("{bob}'s_b!g_#boi.`?uestlove!x"));
     }
 
     @Test(expected = BadRequestException.class)
     public void startsWithPound() {
         createConsumer("#pound");
     }
 
     @Test(expected = BadRequestException.class)
     public void emptyConsumerName() {
         createConsumer("");
     }
 
     @Test(expected = BadRequestException.class)
     public void nullConsumerName() {
         createConsumer(null);
     }
 
     @Test(expected = BadRequestException.class)
     public void startsWithBadCharacter() {
         createConsumer("#foo");
     }
 
     @Test(expected = BadRequestException.class)
     public void containsBadCharacter() {
         createConsumer("bar$%camp");
     }
 
     @Test(expected = ForbiddenException.class)
     public void authRequired() {
         Principal p = new NoAuthPrincipal();
         createConsumer("sys.example.com", p, Collections.emptyList());
     }
 
     private List<String> mockActivationKeys() {
         ActivationKey key1 = new ActivationKey("key1", owner);
         when(activationKeyCurator.lookupForOwner("key1", owner)).thenReturn(key1);
         ActivationKey key2 = new ActivationKey("key2", owner);
         when(activationKeyCurator.lookupForOwner("key2", owner)).thenReturn(key2);
         ActivationKey key3 = new ActivationKey("key3", owner);
         when(activationKeyCurator.lookupForOwner("key3", owner)).thenReturn(key3);
         List<String> keys = new LinkedList<String>();
         keys.add(key1.getName());
         keys.add(key2.getName());
         keys.add(key3.getName());
         return keys;
     }
 
     private String createKeysString(List<String> activationKeys) {
         return StringUtils.join(activationKeys, ',');
     }
 
     @Test
     public void oauthRegistrationSupported() {
         // Should be able to register successfully with as a trusted user principal:
         Principal p = new TrustedUserPrincipal("anyuser");
         Consumer consumer = new Consumer("sys.example.com", null, null, system);
         resource.create(consumer, p, null, owner.getKey(), "");
     }
 
     @Test
     public void registerWithKeys() {
         // No auth should be required for registering with keys:
         Principal p = new NoAuthPrincipal();
         List<String> keys = mockActivationKeys();
         Consumer consumer = new Consumer("sys.example.com", null, null, system);
         resource.create(consumer, p, null, owner.getKey(), createKeysString(keys));
         for (String keyName : keys) {
             verify(activationKeyCurator).lookupForOwner(keyName, owner);
         }
     }
 
     @Test(expected = BadRequestException.class)
     public void orgRequiredWithActivationKeys() {
         Principal p = new NoAuthPrincipal();
         List<String> keys = mockActivationKeys();
         Consumer consumer = new Consumer("sys.example.com", null, null, system);
         resource.create(consumer, p, null, null, createKeysString(keys));
     }
 
     @Test(expected = BadRequestException.class)
     public void cannotMixUsernameWithActivationKeys() {
         Principal p = new NoAuthPrincipal();
         List<String> keys = mockActivationKeys();
         Consumer consumer = new Consumer("sys.example.com", null, null, system);
         resource.create(consumer, p, USER, owner.getKey(), createKeysString(keys));
     }
 
     @Test(expected = NotFoundException.class)
     public void failIfAnyActivationKeyDoesNotExistForOrg() {
         Principal p = new NoAuthPrincipal();
         List<String> keys = mockActivationKeys();
         keys.add("NoSuchKey");
         Consumer consumer = new Consumer("sys.example.com", null, null, system);
         resource.create(consumer, p, null, owner.getKey(), createKeysString(keys));
     }
    
    @Test
    public void registerWithNoInstalledProducts() {
        Principal p = new TrustedUserPrincipal("anyuser");
        Consumer consumer = new Consumer();
        consumer.setType(system);
        consumer.setName("consumername");
        resource.create(consumer, p, USER, owner.getKey(), "");

    }
 }
