 package org.openregistry.core.service;
 
 import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
 import org.springframework.test.context.ContextConfiguration;
 import org.springframework.jdbc.core.JdbcTemplate;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.openregistry.core.repository.PersonRepository;
 import org.openregistry.core.repository.ReferenceRepository;
 import org.openregistry.core.domain.jpa.JpaPersonImpl;
 import org.openregistry.core.domain.jpa.JpaIdentifierTypeImpl;
 import org.openregistry.core.domain.jpa.JpaIdentifierImpl;
 import org.openregistry.core.domain.*;
 import org.junit.Test;
 import org.junit.Before;
 import org.junit.After;
 import static org.junit.Assert.*;
 
 import javax.persistence.EntityManager;
 import javax.persistence.PersistenceContext;
 import java.util.Date;
 
 /**
  * Integration test for {@link org.openregistry.core.service.DefaultActivationService} that links up with the JPA
  * repositories.
  *
  * @version $Revision$ $Date$
  * @since 0.1
  */
 @ContextConfiguration(locations = {"classpath:test-activationServices-context.xml"})
 public final class DefaultActivationServiceIntegrationTests extends AbstractTransactionalJUnit4SpringContextTests {
 
     private static final String IDENTIFIER_TYPE ="NetId";
 
     private static final String IDENTIFIER_VALUE ="test";
 
     private static final String LOCK_VALUE = "LOCK";
 
     @Autowired
     private ActivationService activationService;
 
     @Autowired
     private PersonRepository personRepository;
 
     @Autowired
     private ReferenceRepository referenceRepository;
 
     @PersistenceContext
     private EntityManager entityManager;
 
     private Person person;
 
     @Before
     public void setUp() throws Exception {
         final Person person = new JpaPersonImpl();
         person.setDateOfBirth(new Date());
         person.setGender("M");
         this.simpleJdbcTemplate.update("insert into prd_identifier_types(identifier_t, name) values(null, '" + IDENTIFIER_TYPE + "')");
         final IdentifierType identifierType = this.referenceRepository.findIdentifierType(IDENTIFIER_TYPE);
         final Identifier identifier = person.addIdentifier(identifierType, IDENTIFIER_VALUE);
         identifier.setDeleted(false);
         identifier.setPrimary(true);
         final Name name = person.addName();
         name.setGiven("Scott");
         this.person = this.personRepository.savePerson(person);
     }
 
     @After
     public void tearDown() throws Exception {
         this.person = person;
     }
 
     @Test
     public void testGenerateNewActivationKeyWithPerson() {
         final ActivationKey currentActivationKey = person.getCurrentActivationKey();
         assertEquals(1,countRowsInTable("prc_persons"));
         final String oldActivationKeyString = this.simpleJdbcTemplate.queryForObject("select activation_key from prc_persons where id = ?", String.class, person.getId());
         assertEquals(currentActivationKey.asString(), oldActivationKeyString);
         final ActivationKey newActivationKey = this.activationService.generateActivationKey(person);
         // TODO: figure out why we need to flush here.  Is it necessary?
         this.entityManager.flush();
         final String newActivationKeyString = this.simpleJdbcTemplate.queryForObject("select activation_key from prc_persons where id = ?", String.class, person.getId());
         assertEquals(newActivationKey.asString(), newActivationKeyString);
         final Date endDate = this.simpleJdbcTemplate.queryForObject("select act_key_end_date from prc_persons where id = ?", Date.class, person.getId());
         final Date startDate = this.simpleJdbcTemplate.queryForObject("select act_key_start_date from prc_persons where id = ?", Date.class, person.getId());
         assertEquals(newActivationKey.getEnd(), endDate);
         assertEquals(newActivationKey.getStart(), startDate);
         assertEquals(1,countRowsInTable("prc_persons"));
     }
 
 
     @Test
     public void testGenerateNewActivationKeyWithIdentifiers() {
         final ActivationKey currentActivationKey = person.getCurrentActivationKey();
         assertEquals(1,countRowsInTable("prc_persons"));
         final String oldActivationKeyString = this.simpleJdbcTemplate.queryForObject("select activation_key from prc_persons where id = ?", String.class, person.getId());
         assertEquals(currentActivationKey.asString(), oldActivationKeyString);
         final ActivationKey newActivationKey = this.activationService.generateActivationKey(IDENTIFIER_TYPE, IDENTIFIER_VALUE);
         this.entityManager.flush();
         final String newActivationKeyString = this.simpleJdbcTemplate.queryForObject("select activation_key from prc_persons where id = ?", String.class, person.getId());
         assertEquals(newActivationKey.asString(), newActivationKeyString);
         final Date endDate = this.simpleJdbcTemplate.queryForObject("select act_key_end_date from prc_persons where id = ?", Date.class, person.getId());
         final Date startDate = this.simpleJdbcTemplate.queryForObject("select act_key_start_date from prc_persons where id = ?", Date.class, person.getId());
         assertEquals(newActivationKey.getEnd(), endDate);
         assertEquals(newActivationKey.getStart(), startDate);
         assertEquals(1,countRowsInTable("prc_persons"));
     }
 
 
 
     @Test
     public void testVerifyActivationKeyWithPerson() {
         final ActivationKey activationKey = this.activationService.getActivationKey(person, person.getCurrentActivationKey().asString(), LOCK_VALUE);
         assertEquals(person.getCurrentActivationKey().asString(), activationKey.asString());
         this.entityManager.flush();
         final String lockValue = this.simpleJdbcTemplate.queryForObject("select act_key_lock from prc_persons where id = ?", String.class, this.person.getId());
         final Date lockDate = this.simpleJdbcTemplate.queryForObject("select act_key_lock_expiration from prc_persons where id = ?", Date.class, this.person.getId());
         assertEquals(LOCK_VALUE, lockValue);
        // TODO how to test if lock was set correctly
         assertEquals(1,countRowsInTable("prc_persons"));
     }
 
 
     @Test
     public void testVerifyActivationKeyWithIdentifiers() {
         final ActivationKey activationKey = this.activationService.getActivationKey(IDENTIFIER_TYPE, IDENTIFIER_VALUE, person.getCurrentActivationKey().asString(), LOCK_VALUE);
         assertEquals(person.getCurrentActivationKey().asString(), activationKey.asString());
         this.entityManager.flush();
         final String lockValue = this.simpleJdbcTemplate.queryForObject("select act_key_lock from prc_persons where id = ?", String.class, this.person.getId());
         final Date lockDate = this.simpleJdbcTemplate.queryForObject("select act_key_lock_expiration from prc_persons where id = ?", Date.class, this.person.getId());
         assertEquals(LOCK_VALUE, lockValue);
         // TODO how to test if lock was set correctly
         assertEquals(1,countRowsInTable("prc_persons"));
     }
     /*
     @Test
     public void testInvalidateActivationKeyWithPerson() {
 
     }
 
     public void testInvalidateActivationKeyWithIdentifiers() {
 
     }  */
 
     public void setActivationService(final ActivationService activationService) {
         this.activationService = activationService;
     }
 
     public void setPersonRepository(final PersonRepository personRepository) {
         this.personRepository = personRepository;
     }
 }
