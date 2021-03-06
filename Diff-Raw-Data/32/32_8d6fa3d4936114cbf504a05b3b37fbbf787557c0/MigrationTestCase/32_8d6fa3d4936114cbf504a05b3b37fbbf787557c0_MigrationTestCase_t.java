 /**
  * Copyright (C) 2009-2010 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *         http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.googlecode.flyway.core.migration;
 
 import com.googlecode.flyway.core.Flyway;
 import com.googlecode.flyway.core.dbsupport.DbSupport;
 import com.googlecode.flyway.core.metadatatable.MetaDataTableRow;
 import com.googlecode.flyway.core.migration.sql.PlaceholderReplacer;
 import com.googlecode.flyway.core.migration.sql.SqlMigration;
 import com.googlecode.flyway.core.validation.ValidationErrorMode;
 import com.googlecode.flyway.core.validation.ValidationMode;
 import org.junit.Before;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.springframework.core.io.ClassPathResource;
 import org.springframework.jdbc.core.JdbcTemplate;
 import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
 
 import javax.annotation.Resource;
 import javax.sql.DataSource;
 import java.util.HashMap;
 
 import static org.junit.Assert.*;
 
 /**
  * Test to demonstrate the migration functionality.
  */
 @RunWith(SpringJUnit4ClassRunner.class)
 public abstract class MigrationTestCase {
     /**
      * The datasource to use for single-threaded migration tests.
      */
     @Resource
     protected DataSource migrationDataSource;
 
     protected JdbcTemplate jdbcTemplate;
 
     protected Flyway flyway;
 
     @Before
     public void setUp() {
         jdbcTemplate = new JdbcTemplate(migrationDataSource);
 
         flyway = new Flyway();
         flyway.setDataSource(migrationDataSource);
         flyway.setValidationMode(ValidationMode.ALL);
         flyway.clean();
     }
 
     /**
      * @return The directory containing the migrations for the tests.
      */
     protected abstract String getBaseDir();
 
     /**
      * @param jdbcTemplate The jdbcTemplate to intialize the instance with.
      *
      * @return The DbSupport class to test.
      */
     protected abstract DbSupport getDbSupport(JdbcTemplate jdbcTemplate);
 
     @Test
     public void migrate() throws Exception {
         flyway.setBaseDir(getBaseDir());
         flyway.migrate();
         SchemaVersion schemaVersion = flyway.status().getVersion();
         assertEquals("2.0", schemaVersion.toString());
         assertEquals("Add foreign key", flyway.status().getDescription());
         assertEquals(0, flyway.migrate());
         assertEquals(3, flyway.history().size());
 
         for (MetaDataTableRow metaDataTableRow : flyway.history()) {
             assertChecksum(metaDataTableRow);
         }
     }
 
    @Test
    public void customTableName() throws Exception {
        flyway.setBaseDir(getBaseDir());
        flyway.setTable("my_custom_table");
        flyway.migrate();
        int count = jdbcTemplate.queryForInt("select count(*) from my_custom_table");
        assertEquals(3, count);
    }

     /**
      * Compares the DB checksum to the classpath checksum of this migration.
      *
      * @param appliedMigration The migration to check.
      */
     private void assertChecksum(MetaDataTableRow appliedMigration) {
         ClassPathResource resource = new ClassPathResource(getBaseDir() + "/" + appliedMigration.getScript());
         PlaceholderReplacer placeholderReplacer = new PlaceholderReplacer(new HashMap<String, String>(), "", "");
         Migration sqlMigration = new SqlMigration(resource, placeholderReplacer, "UTF-8", "1");
         assertEquals("Wrong checksum for " + appliedMigration.getScript(), sqlMigration.getChecksum(), appliedMigration.getChecksum());
     }
 
     @Test(expected = IllegalStateException.class)
     public void validateFails() throws Exception {
         flyway.setBaseDir(getBaseDir());
         flyway.setSqlMigrationSuffix("First.sql");
         flyway.migrate();
 
         SchemaVersion schemaVersion = flyway.status().getVersion();
         assertEquals("1", schemaVersion.toString());
 
         flyway.setSqlMigrationPrefix("CheckValidate");
         flyway.validate();
     }
 
     @Test
     public void validateClean() throws Exception {
         flyway.setBaseDir(getBaseDir());
         flyway.setSqlMigrationSuffix("First.sql");
         flyway.migrate();
 
         SchemaVersion schemaVersion = flyway.status().getVersion();
         assertEquals("1", schemaVersion.toString());
 
         flyway.setValidationMode(ValidationMode.ALL);
         flyway.setValidationErrorMode(ValidationErrorMode.CLEAN);
         flyway.setSqlMigrationPrefix("CheckValidate");
         assertEquals(1, flyway.migrate());
     }
 
     @Test
     public void failedMigration() throws Exception {
         flyway.setBaseDir("migration/failed");
 
         try {
             flyway.migrate();
             fail();
         } catch (IllegalStateException e) {
             //Expected
         }
 
         MetaDataTableRow migration = flyway.status();
         if (getDbSupport(new JdbcTemplate(migrationDataSource)).supportsDdlTransactions()) {
             assertNull(migration);
         } else {
             SchemaVersion version = migration.getVersion();
             assertEquals("1", version.toString());
             assertEquals("Should Fail", migration.getDescription());
             assertEquals(MigrationState.FAILED, migration.getState());
             assertEquals(1, flyway.history().size());
         }
     }
 
     @Test
     public void tableExists() throws Exception {
         flyway.init(null, null);
         assertTrue(getDbSupport(new JdbcTemplate(migrationDataSource)).tableExists("SCHEMA_VERSION"));
     }
 
     @Test
     public void columnExists() throws Exception {
         JdbcTemplate jdbcTemplate = new JdbcTemplate(migrationDataSource);
 
         flyway.init(null, null);
         assertTrue(getDbSupport(jdbcTemplate).columnExists("SCHEMA_VERSION", "DESCRIPTION"));
         assertFalse(getDbSupport(jdbcTemplate).columnExists("SCHEMA_VERSION", "INVALID"));
     }
 
     /**
      * check if meta table has no current migration (manually edited)
      */
     @Test
     public void checkForInvalidMetatable() {
         flyway.setBaseDir(getBaseDir());
         try {
             flyway.migrate();
         } catch (Exception e) {
             throw new RuntimeException("unhandled checked exception", e);
         }
         JdbcTemplate jdbcTemplate = new JdbcTemplate(migrationDataSource);
         DbSupport dbSupport = getDbSupport(jdbcTemplate);
         jdbcTemplate.update("UPDATE schema_version SET current_version = " + dbSupport.getBooleanFalse()
                 + " where current_version = " + dbSupport.getBooleanTrue());
         try {
             flyway.migrate();
             fail();
         } catch (IllegalStateException e) {
             // OK.
         } catch (Exception e) {
             throw new RuntimeException("unhandled checked exception", e);
         }
     }
 }
