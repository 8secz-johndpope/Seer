 package net.ripe.db.whois.api.rest;
 
 import com.google.common.collect.Lists;
 import net.ripe.db.whois.api.AbstractIntegrationTest;
 import net.ripe.db.whois.api.RestTest;
 import net.ripe.db.whois.api.rest.domain.Attribute;
 import net.ripe.db.whois.api.rest.domain.Flag;
 import net.ripe.db.whois.api.rest.domain.Flags;
 import net.ripe.db.whois.api.rest.domain.InverseAttributes;
 import net.ripe.db.whois.api.rest.domain.Link;
 import net.ripe.db.whois.api.rest.domain.Parameters;
 import net.ripe.db.whois.api.rest.domain.QueryStrings;
 import net.ripe.db.whois.api.rest.domain.Source;
 import net.ripe.db.whois.api.rest.domain.Sources;
 import net.ripe.db.whois.api.rest.domain.TypeFilters;
 import net.ripe.db.whois.api.rest.domain.WhoisObject;
 import net.ripe.db.whois.api.rest.domain.WhoisResources;
 import net.ripe.db.whois.api.rest.domain.WhoisTag;
 import net.ripe.db.whois.api.rest.domain.WhoisVersion;
 import net.ripe.db.whois.api.rest.domain.WhoisVersions;
 import net.ripe.db.whois.api.rest.mapper.WhoisObjectServerMapper;
 import net.ripe.db.whois.common.IntegrationTest;
 import net.ripe.db.whois.common.MaintenanceMode;
 import net.ripe.db.whois.common.dao.RpslObjectUpdateInfo;
 import net.ripe.db.whois.common.domain.User;
 import net.ripe.db.whois.common.domain.io.Downloader;
 import net.ripe.db.whois.common.rpsl.AttributeType;
 import net.ripe.db.whois.common.rpsl.ObjectType;
 import net.ripe.db.whois.common.rpsl.RpslAttribute;
 import net.ripe.db.whois.common.rpsl.RpslObject;
 import net.ripe.db.whois.common.rpsl.RpslObjectBuilder;
 import net.ripe.db.whois.common.support.DummyWhoisClient;
 import org.junit.Before;
 import org.junit.Ignore;
 import org.junit.Test;
 import org.junit.experimental.categories.Category;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.dao.EmptyResultDataAccessException;
 
 import javax.ws.rs.BadRequestException;
 import javax.ws.rs.ClientErrorException;
 import javax.ws.rs.NotAllowedException;
 import javax.ws.rs.NotAuthorizedException;
 import javax.ws.rs.NotFoundException;
 import javax.ws.rs.ServiceUnavailableException;
 import javax.ws.rs.client.Entity;
 import javax.ws.rs.core.MediaType;
 import javax.ws.rs.core.Response;
 import java.io.IOException;
 import java.net.URL;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.util.Arrays;
 import java.util.List;
 import java.util.Map;
 
 import static net.ripe.db.whois.api.RestTest.assertErrorMessage;
 import static net.ripe.db.whois.api.RestTest.assertOnlyErrorMessage;
 import static net.ripe.db.whois.api.RestTest.mapClientException;
 import static net.ripe.db.whois.common.support.StringMatchesRegexp.stringMatchesRegexp;
 import static org.hamcrest.Matchers.contains;
 import static org.hamcrest.Matchers.containsInAnyOrder;
 import static org.hamcrest.Matchers.containsString;
 import static org.hamcrest.Matchers.empty;
 import static org.hamcrest.Matchers.endsWith;
 import static org.hamcrest.Matchers.hasSize;
 import static org.hamcrest.Matchers.is;
 import static org.hamcrest.Matchers.not;
 import static org.hamcrest.Matchers.nullValue;
 import static org.junit.Assert.assertThat;
 import static org.junit.Assert.fail;
 
 @Category(IntegrationTest.class)
 public class WhoisRestServiceTestIntegration extends AbstractIntegrationTest {
 
     private static final String VERSION_DATE_PATTERN = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}";
 
     private static final RpslObject PAULETH_PALTHEN = RpslObject.parse("" +
             "person:    Pauleth Palthen\n" +
             "address:   Singel 258\n" +
             "phone:     +31-1234567890\n" +
             "e-mail:    noreply@ripe.net\n" +
             "mnt-by:    OWNER-MNT\n" +
             "nic-hdl:   PP1-TEST\n" +
             "changed:   noreply@ripe.net 20120101\n" +
             "remarks:   remark\n" +
             "source:    TEST\n");
 
     private static final RpslObject OWNER_MNT = RpslObject.parse("" +
             "mntner:      OWNER-MNT\n" +
             "descr:       Owner Maintainer\n" +
             "admin-c:     TP1-TEST\n" +
             "upd-to:      noreply@ripe.net\n" +
             "auth:        MD5-PW $1$d9fKeTr2$Si7YudNf4rUGmR71n/cqk/ #test\n" +
             "mnt-by:      OWNER-MNT\n" +
             "referral-by: OWNER-MNT\n" +
             "changed:     dbtest@ripe.net 20120101\n" +
             "source:      TEST");
 
     private static final RpslObject TEST_PERSON = RpslObject.parse("" +
             "person:    Test Person\n" +
             "address:   Singel 258\n" +
             "phone:     +31 6 12345678\n" +
             "nic-hdl:   TP1-TEST\n" +
             "mnt-by:    OWNER-MNT\n" +
             "changed:   dbtest@ripe.net 20120101\n" +
             "source:    TEST\n");
 
     private static final RpslObject TEST_ROLE = RpslObject.parse("" +
             "role:      Test Role\n" +
             "address:   Singel 258\n" +
             "phone:     +31 6 12345678\n" +
             "nic-hdl:   TR1-TEST\n" +
             "admin-c:   TR1-TEST\n" +
             "abuse-mailbox: abuse@test.net\n" +
             "mnt-by:    OWNER-MNT\n" +
             "changed:   dbtest@ripe.net 20120101\n" +
             "source:    TEST\n");
 
     private static final RpslObject TEST_IRT = RpslObject.parse("" +
             "irt:          irt-test\n" +
             "address:      RIPE NCC\n" +
             "e-mail:       noreply@ripe.net\n" +
             "admin-c:      TP1-TEST\n" +
             "tech-c:       TP1-TEST\n" +
             "auth:         MD5-PW $1$d9fKeTr2$Si7YudNf4rUGmR71n/cqk/ #test\n" +
             "mnt-by:       OWNER-MNT\n" +
             "changed:      dbtest@ripe.net 20120101\n" +
             "source:       TEST\n");
 
     @Autowired
     private WhoisObjectServerMapper whoisObjectMapper;
     @Autowired
     private MaintenanceMode maintenanceMode;
 
     @Before
     public void setup() {
         databaseHelper.addObject("person: Test Person\nnic-hdl: TP1-TEST");
         databaseHelper.addObject("role: Test Role\nnic-hdl: TR1-TEST");
         databaseHelper.addObject(OWNER_MNT);
         databaseHelper.updateObject(TEST_PERSON);
         databaseHelper.updateObject(TEST_ROLE);
         maintenanceMode.set("FULL,FULL");
     }
 
     // lookup
 
     @Test
     public void lookup_downloader_test() throws Exception {
         Path path = Files.createTempFile("downloader_test", "");
         Downloader downloader = new Downloader();
         downloader.downloadTo(LoggerFactory.getLogger("downloader_test"), new URL(String.format("http://localhost:%d/whois/test/mntner/owner-mnt", getPort())), path);
         final String result = new String(Files.readAllBytes(path));
         assertThat(result, containsString("OWNER-MNT"));
         assertThat(result, endsWith("</whois-resources>"));
     }
 
     @Test
     public void lookup_without_accepts_header() throws Exception {
         final String query = DummyWhoisClient.query(getPort(), "GET /whois/test/mntner/owner-mnt HTTP/1.1\nHost: localhost\nConnection: close\n");
 
         assertThat(query, containsString("HTTP/1.1 200 OK"));
         assertThat(query, containsString("<whois-resources xmlns"));
     }
 
     @Test
     public void lookup_with_empty_accepts_header() throws Exception {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/mntner/owner-mnt")
                 .request()
                 .get(WhoisResources.class);
 
         final RpslObject object = whoisObjectMapper.map(whoisResources.getWhoisObjects().get(0));
 
         assertThat(object, is(RpslObject.parse("" +
                 "mntner:         OWNER-MNT\n" +
                 "descr:          Owner Maintainer\n" +
                 "admin-c:        TP1-TEST\n" +
                 "auth:           MD5-PW\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "referral-by:    OWNER-MNT\n" +
                 "source:         TEST")));
     }
 
     @Test
     public void lookup_inet6num_without_prefix_length() {
         databaseHelper.addObject(
                 "inet6num:       2001:2002:2003::/48\n" +
                         "netname:        RIPE-NCC\n" +
                         "descr:          Private Network\n" +
                         "country:        NL\n" +
                         "tech-c:         TP1-TEST\n" +
                         "status:         ASSIGNED PA\n" +
                         "mnt-by:         OWNER-MNT\n" +
                         "mnt-lower:      OWNER-MNT\n" +
                         "source:         TEST");
         ipTreeUpdater.rebuild();
 
         try {
             RestTest.target(getPort(), "whois/test/inet6num/2001:2002:2003::").request().get(WhoisResources.class);
             fail();
         } catch (NotFoundException ignored) {
             // expected
         }
     }
 
     @Test
     public void lookup_inet6num_with_prefix_length() {
         databaseHelper.addObject(
                 "inet6num:       2001:2002:2003::/48\n" +
                         "netname:        RIPE-NCC\n" +
                         "descr:          Private Network\n" +
                         "country:        NL\n" +
                         "tech-c:         TP1-TEST\n" +
                         "status:         ASSIGNED PA\n" +
                         "mnt-by:         OWNER-MNT\n" +
                         "mnt-lower:      OWNER-MNT\n" +
                         "source:         TEST");
         ipTreeUpdater.rebuild();
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/inet6num/2001:2002:2003::/48").request().get(WhoisResources.class);
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getPrimaryKey().get(0).getValue(), is("2001:2002:2003::/48"));
     }
 
     @Test
     public void lookup_person() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/person/TP1-TEST").request().get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("person", "Test Person"),
                 new Attribute("address", "Singel 258"),
                 new Attribute("phone", "+31 6 12345678"),
                 new Attribute("nic-hdl", "TP1-TEST"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST", "Filtered", null, null)));
     }
 
     @Test
     public void lookup_xml_text_not_contains_empty_xmlns() {
         final String whoisResources = RestTest.target(getPort(), "whois/test/person/TP1-TEST").request().get(String.class);
         assertThat(whoisResources, not(containsString("xmlns=\"\"")));
     }
 
     @Test
     public void lookup_xml_text_not_contains_root_level_locator() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/person/TP1-TEST").request().get(WhoisResources.class);
         assertThat(whoisResources.getLink(), nullValue());
     }
 
     @Test
     public void lookup_inet6num() throws Exception {
         final RpslObject inet6num = RpslObject.parse("" +
                 "inet6num: 2001::/48\n" +
                 "netname: RIPE-NCC\n" +
                 "descr: some description\n" +
                 "country: DK\n" +
                 "admin-c: TP1-TEST\n" +
                 "tech-c: TP1-TEST\n" +
                 "status: ASSIGNED\n" +
                 "mnt-by: OWNER-MNT\n" +
                 "changed: org@ripe.net 20120505\n" +
                 "source: TEST\n");
         databaseHelper.addObject(inet6num);
         ipTreeUpdater.rebuild();
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/inet6num/2001::/48").request().get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("inet6num", "2001::/48"),
                 new Attribute("netname", "RIPE-NCC"),
                 new Attribute("descr", "some description"),
                 new Attribute("country", "DK"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("tech-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("status", "ASSIGNED"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST", "Filtered", null, null)
         ));
     }
 
     @Test
     public void lookup_route() throws Exception {
         final RpslObject route = RpslObject.parse("" +
                 "route:           193.254.30.0/24\n" +
                 "descr:           Test route\n" +
                 "origin:          AS12726\n" +
                 "mnt-by:          OWNER-MNT\n" +
                 "changed:         ripe@test.net 20091015\n" +
                 "source:          TEST\n");
         databaseHelper.addObject(route);
         ipTreeUpdater.rebuild();
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/route/193.254.30.0/24AS12726").request().get(WhoisResources.class);
         assertThat(whoisResources.getErrorMessages(), is(empty()));
 
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getLink().getHref(), is("http://rest-test.db.ripe.net/test/route/193.254.30.0/24AS12726"));
         assertThat(whoisObject.getAttributes(), containsInAnyOrder(
                 new Attribute("route", "193.254.30.0/24"),
                 new Attribute("descr", "Test route"),
                 new Attribute("origin", "AS12726", null, "aut-num", new Link("locator", "http://rest-test.db.ripe.net/test/aut-num/AS12726")),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST", "Filtered", null, null)
         ));
     }
 
     @Test
     public void lookup_person_json() throws Exception {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/person/TP1-TEST")
                 .request(MediaType.APPLICATION_JSON_TYPE)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("person", "Test Person"),
                 new Attribute("address", "Singel 258"),
                 new Attribute("phone", "+31 6 12345678"),
                 new Attribute("nic-hdl", "TP1-TEST"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST", "Filtered", null, null)));
 
         assertThat(whoisResources.getTermsAndConditions().getHref(), is(WhoisResources.TERMS_AND_CONDITIONS));
     }
 
     @Test
     public void lookup_role_accept_json() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/role/TR1-TEST")
                 .request(MediaType.APPLICATION_JSON_TYPE)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
 
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("role", "Test Role"),
                 new Attribute("address", "Singel 258"),
                 new Attribute("phone", "+31 6 12345678"),
                 new Attribute("nic-hdl", "TR1-TEST"),
                 new Attribute("admin-c", "TR1-TEST", null, "role", new Link("locator", "http://rest-test.db.ripe.net/test/role/TR1-TEST")),
                 new Attribute("abuse-mailbox", "abuse@test.net"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST", "Filtered", null, null)));
     }
 
     @Test
     public void lookup_person_accept_json() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/TEST/person/TP1-TEST")
                 .request(MediaType.APPLICATION_JSON_TYPE)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
 
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getPrimaryKey().get(0).getValue(), is("TP1-TEST"));
     }
 
     @Test
     public void lookup_object_json_extension() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/TEST/person/TP1-TEST.json")
                 .request()
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
 
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getPrimaryKey().get(0).getValue(), is("TP1-TEST"));
     }
 
     @Test
     public void lookup_object_not_found() {
         try {
             RestTest.target(getPort(), "whois/test/person/PP1-TEST").request().get(WhoisResources.class);
             fail();
         } catch (NotFoundException ignored) {
             // expected
         }
     }
 
     @Test
     public void lookup_object_wrong_source() {
         try {
             RestTest.target(getPort(), "whois/test-grs/person/TP1-TEST").request().get(String.class);
             fail();
         } catch (NotFoundException ignored) {
             // expected
         }
     }
 
     @Test
     public void grs_lookup_object_wrong_source() {
         try {
             RestTest.target(getPort(), "whois/pez/person/PP1-TEST").request().get(String.class);
             fail();
         } catch (BadRequestException e) {
             assertOnlyErrorMessage(e, "Error", "Invalid source '%s'", "pez");
         }
     }
 
     @Test
     public void grs_lookup_found() {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST-GRS\n");
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test-grs/aut-num/AS102").request().get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         assertThat(whoisResources.getWhoisObjects().get(0).getPrimaryKey(), contains(new Attribute("aut-num", "AS102")));
         assertThat(whoisResources.getWhoisObjects().get(0).getSource(), is(new Source("test-grs")));
     }
 
     @Test
     public void lookup_autnum_includes_tags() {
         final RpslObject autnum = RpslObject.parse("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
         Map<RpslObject, RpslObjectUpdateInfo> updateInfos = databaseHelper.addObjects(Lists.newArrayList(autnum));
 
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "unref", "28");
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "foobar", "description");
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "other", "other stuff");
 
         final WhoisResources whoisResources = RestTest.target(getPort(),
                 "whois/TEST/aut-num/AS102")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
 
         assertThat(whoisObject.getTags(), contains(
                 new WhoisTag("foobar", "description"),
                 new WhoisTag("other", "other stuff"),
                 new WhoisTag("unref", "28")));
     }
 
     @Test
     public void lookup_mntner() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/mntner/OWNER-MNT").request().get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("mntner", "OWNER-MNT"),
                 new Attribute("descr", "Owner Maintainer"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("auth", "MD5-PW", "Filtered", null, null),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("referral-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST", "Filtered", null, null)));
     }
 
     @Test
     public void lookup_unfiltered() throws Exception {
         databaseHelper.addObject(PAULETH_PALTHEN);
 
         final String unfiltered = RestTest.target(getPort(), "whois/test/person/PP1-TEST?unfiltered").request().get(String.class);
         assertThat(unfiltered, containsString("attribute name=\"e-mail\" value=\"noreply@ripe.net\""));
 
         final String unfilteredEquals = RestTest.target(getPort(), "whois/test/person/PP1-TEST?unfiltered=").request().get(String.class);
         assertThat(unfilteredEquals, containsString("attribute name=\"e-mail\" value=\"noreply@ripe.net\""));
 
         final String unfilteredEqualsTrue = RestTest.target(getPort(), "whois/test/person/PP1-TEST?unfiltered=true").request().get(String.class);
         assertThat(unfilteredEqualsTrue, containsString("attribute name=\"e-mail\" value=\"noreply@ripe.net\""));
 
         final String unfilteredEqualsFalse = RestTest.target(getPort(), "whois/test/person/PP1-TEST?unfiltered=false").request().get(String.class);
         assertThat(unfilteredEqualsFalse, not(containsString("attribute name=\"e-mail\" value=\"noreply@ripe.net\"")));
 
         final String withOtherParameters = RestTest.target(getPort(), "whois/test/person/PP1-TEST?unfiltered=true&pretty=false").request().get(String.class);
         assertThat(withOtherParameters, containsString("attribute name=\"e-mail\" value=\"noreply@ripe.net\""));
 
         final String filteredByDefault = RestTest.target(getPort(), "whois/test/person/PP1-TEST").request().get(String.class);
         assertThat(filteredByDefault, not(containsString("attribute name=\"e-mail\" value=\"noreply@ripe.net\"")));
     }
 
     @Test
     public void lookup_mntner_without_password_unfiltered() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/mntner/OWNER-MNT?unfiltered").request().get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("mntner", "OWNER-MNT"),
                 new Attribute("descr", "Owner Maintainer"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("upd-to", "noreply@ripe.net", null, null, null),
                 new Attribute("auth", "MD5-PW", "Filtered", null, null),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("referral-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("changed", "dbtest@ripe.net 20120101", null, null, null),
                 new Attribute("source", "TEST", "Filtered", null, null)));
     }
 
     @Test
     public void lookup_mntner_correct_password() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/mntner/OWNER-MNT?password=test&unfiltered").request().get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("mntner", "OWNER-MNT"),
                 new Attribute("descr", "Owner Maintainer"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("upd-to", "noreply@ripe.net", null, null, null),
                 new Attribute("auth", "MD5-PW $1$d9fKeTr2$Si7YudNf4rUGmR71n/cqk/", "test", null, null),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("referral-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("changed", "dbtest@ripe.net 20120101", null, null, null),
                 new Attribute("source", "TEST", null, null, null)));
     }
 
     @Test
     public void lookup_mntner_correct_password_filtered() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/mntner/OWNER-MNT?password=test").request().get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("mntner", "OWNER-MNT"),
                 new Attribute("descr", "Owner Maintainer"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("auth", "MD5-PW $1$d9fKeTr2$Si7YudNf4rUGmR71n/cqk/", "test", null, null),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("referral-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST", "Filtered", null, null)));
     }
 
     @Test
     public void lookup_mntner_incorrect_password() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/mntner/OWNER-MNT?password=incorrect").request().get(WhoisResources.class);
 
         //TODO [TP] there should be an error message in the response for the incorrect password
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("mntner", "OWNER-MNT"),
                 new Attribute("descr", "Owner Maintainer"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("auth", "MD5-PW", "Filtered", null, null),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("referral-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST", "Filtered", null, null)));
     }
 
     @Test
     public void lookup_mntner_multiple_passwords() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/mntner/OWNER-MNT?password=incorrect&password=test&unfiltered").request().get(WhoisResources.class);
 
         //TODO [TP] there should be an error message in the response for the incorrect password
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("mntner", "OWNER-MNT"),
                 new Attribute("descr", "Owner Maintainer"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("upd-to", "noreply@ripe.net", null, null, null),
                 new Attribute("auth", "MD5-PW $1$d9fKeTr2$Si7YudNf4rUGmR71n/cqk/", "test", null, null),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("referral-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("changed", "dbtest@ripe.net 20120101", null, null, null),
                 new Attribute("source", "TEST", null, null, null)));
     }
 
     @Test
     public void lookup_irt_correct_password() {
         databaseHelper.addObject(TEST_IRT);
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/irt/irt-test?password=test&unfiltered").request().get(WhoisResources.class);
 
         //TODO [TP] there should be an error message in the response for the incorrect password
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("irt", "irt-test"),
                 new Attribute("address", "RIPE NCC"),
                 new Attribute("e-mail", "noreply@ripe.net"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("tech-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("auth", "MD5-PW $1$d9fKeTr2$Si7YudNf4rUGmR71n/cqk/", "test", null, null),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("changed", "dbtest@ripe.net 20120101", null, null, null),
                 new Attribute("source", "TEST")));
     }
 
     @Test
     public void lookup_irt_incorrect_password() {
         databaseHelper.addObject(TEST_IRT);
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/irt/irt-test?password=incorrect").request().get(WhoisResources.class);
 
         //TODO [TP] there should be an error message in the response for the incorrect password
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("irt", "irt-test"),
                 new Attribute("address", "RIPE NCC"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("tech-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("auth", "MD5-PW", "Filtered", null, null),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST", "Filtered", null, null)));
     }
 
     @Test
     public void lookup_mntner_xml_text() {
         final String result = RestTest.target(getPort(), "whois/test/mntner/owner-mnt.xml")
                 .request()
                 .get(String.class);
 
         assertThat(result, is(
                 "<?xml version='1.0' encoding='UTF-8'?>" +
                 "<whois-resources xmlns:xlink=\"http://www.w3.org/1999/xlink\">" +
                 "<objects>" +
                 "<object type=\"mntner\">" +
                 "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\" />" +
                 "<source id=\"test\" />" +
                 "<primary-key>" +
                 "<attribute name=\"mntner\" value=\"OWNER-MNT\" />" +
                 "</primary-key>" +
                 "<attributes>" +
                 "<attribute name=\"mntner\" value=\"OWNER-MNT\" />" +
                 "<attribute name=\"descr\" value=\"Owner Maintainer\" />" +
                 "<attribute name=\"admin-c\" value=\"TP1-TEST\" referenced-type=\"person\">" +
                 "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test/person/TP1-TEST\" />" +
                 "</attribute>" +
                 "<attribute name=\"auth\" value=\"MD5-PW\" comment=\"Filtered\" />" +
                 "<attribute name=\"mnt-by\" value=\"OWNER-MNT\" referenced-type=\"mntner\">" +
                 "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\" />" +
                 "</attribute>" +
                 "<attribute name=\"referral-by\" value=\"OWNER-MNT\" referenced-type=\"mntner\">" +
                 "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\" />" +
                 "</attribute>" +
                 "<attribute name=\"source\" value=\"TEST\" comment=\"Filtered\" />" +
                 "</attributes>" +
                 "<tags />" +
                 "</object>" +
                 "</objects>" +
                 "<terms-and-conditions xlink:type=\"locator\" xlink:href=\"http://www.ripe.net/db/support/db-terms-conditions.pdf\" />" +
                 "</whois-resources>"));
     }
 
     @Test
     public void lookup_mntner_json_text() {
         final String result = RestTest.target(getPort(), "whois/test/mntner/owner-mnt.json")
                 .request()
                 .get(String.class);
 
         assertThat(result, is(
                 "{\"objects\":" +
                 "{\"object\":[{" +
                 "\"type\":\"mntner\"," +
                 "\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\"}," +
                 "\"source\":{\"id\":\"test\"}," +
                 "\"primary-key\":{" +
                 "\"attribute\":[" +
                 "{\"name\":\"mntner\",\"value\":\"OWNER-MNT\"}" +
                 "]}," +
                 "\"attributes\":{" +
                 "\"attribute\":[" +
                 "{\"name\":\"mntner\",\"value\":\"OWNER-MNT\"}," +
                 "{\"name\":\"descr\",\"value\":\"Owner Maintainer\"}," +
                 "{\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test/person/TP1-TEST\"},\"name\":\"admin-c\",\"value\":\"TP1-TEST\",\"referenced-type\":\"person\"}," +
                 "{\"name\":\"auth\",\"value\":\"MD5-PW\",\"comment\":\"Filtered\"}," +
                 "{\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\"},\"name\":\"mnt-by\",\"value\":\"OWNER-MNT\",\"referenced-type\":\"mntner\"}," +
                 "{\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\"},\"name\":\"referral-by\",\"value\":\"OWNER-MNT\",\"referenced-type\":\"mntner\"}," +
                 "{\"name\":\"source\",\"value\":\"TEST\",\"comment\":\"Filtered\"}" +
                 "]}," +
                 "\"tags\":{\"tag\":[]}" +
                 "}]}," +
                 "\"terms-and-conditions\":{\"type\":\"locator\",\"href\":\"http://www.ripe.net/db/support/db-terms-conditions.pdf\"}" +
                 "}"
         ));
     }
 
     @Test
     public void lookup_person_json_text() {
         final String result = RestTest.target(getPort(), "whois/test/person/TP1-TEST")
                 .request(MediaType.APPLICATION_JSON_TYPE)
                 .get(String.class);
 
         assertThat(result, is(
                 "{\"objects\":{" +
                         "\"object\":[{" +
                         "\"type\":\"person\"," +
                         "\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test/person/TP1-TEST\"}," +
                         "\"source\":{\"id\":\"test\"}," +
                         "\"primary-key\":{" +
                         "\"attribute\":[" +
                         "{\"name\":\"nic-hdl\",\"value\":\"TP1-TEST\"}" +
                         "]}," +
                         "\"attributes\":{" +
                         "\"attribute\":[" +
                         "{\"name\":\"person\",\"value\":\"Test Person\"}," +
                         "{\"name\":\"address\",\"value\":\"Singel 258\"}," +
                         "{\"name\":\"phone\",\"value\":\"+31 6 12345678\"}," +
                         "{\"name\":\"nic-hdl\",\"value\":\"TP1-TEST\"}," +
                         "{\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\"},\"name\":\"mnt-by\",\"value\":\"OWNER-MNT\",\"referenced-type\":\"mntner\"}," +
                         "{\"name\":\"source\",\"value\":\"TEST\",\"comment\":\"Filtered\"}" +
                         "]}," +
                         "\"tags\":{\"tag\":[]}" +
                         "}]}," +
                         "\"terms-and-conditions\":{\"type\":\"locator\",\"href\":\"http://www.ripe.net/db/support/db-terms-conditions.pdf\"}" +
                         "}"));
     }
 
     @Test
     public void grs_lookup_autnum_json_text() {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST-GRS\n");
 
         final String result = RestTest.target(getPort(), "whois/test-grs/aut-num/AS102.json").request().get(String.class);
 
         assertThat(result, is(
                 "{\"objects\":" +
                 "{\"object\":[{" +
                 "\"type\":\"aut-num\"," +
                 "\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test-grs/aut-num/AS102\"}," +
                 "\"source\":{\"id\":\"test-grs\"}," +
                 "\"primary-key\":{" +
                 "\"attribute\":[" +
                 "{\"name\":\"aut-num\",\"value\":\"AS102\"}" +
                 "]}," +
                 "\"attributes\":{" +
                 "\"attribute\":[" +
                 "{\"name\":\"aut-num\",\"value\":\"AS102\"}," +
                 "{\"name\":\"as-name\",\"value\":\"End-User-2\"}," +
                 "{\"name\":\"descr\",\"value\":\"description\"}," +
                 "{\"name\":\"admin-c\",\"value\":\"DUMY-RIPE\"}," +
                 "{\"name\":\"tech-c\",\"value\":\"DUMY-RIPE\"}," +
                 "{\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test-grs/mntner/OWNER-MNT\"},\"name\":\"mnt-by\",\"value\":\"OWNER-MNT\",\"referenced-type\":\"mntner\"}," +
                 "{\"name\":\"source\",\"value\":\"TEST-GRS\"}," +
                 "{\"name\":\"remarks\",\"value\":\"****************************\"}," +
                 "{\"name\":\"remarks\",\"value\":\"* THIS OBJECT IS MODIFIED\"}," +
                 "{\"name\":\"remarks\",\"value\":\"* Please note that all data that is generally regarded as personal\"}," +
                 "{\"name\":\"remarks\",\"value\":\"* data has been removed from this object.\"}," +
                 "{\"name\":\"remarks\",\"value\":\"* To view the original object, please query the RIPE Database at:\"}," +
                 "{\"name\":\"remarks\",\"value\":\"* http://www.ripe.net/whois\"}," +
                 "{\"name\":\"remarks\",\"value\":\"****************************\"}" +
                 "]}," +
                 "\"tags\":{\"tag\":[]}" +
                 "}]}," +
                 "\"terms-and-conditions\":{\"type\":\"locator\",\"href\":\"http://www.ripe.net/db/support/db-terms-conditions.pdf\"}" +
                 "}"
         ));
     }
 
     @Test
     public void grs_lookup_autnum_xml_text() {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST-GRS\n");
 
         final String result = RestTest.target(getPort(), "whois/test-grs/aut-num/AS102.xml").request().get(String.class);
 
         assertThat(result, is(
                 "<?xml version='1.0' encoding='UTF-8'?>" +
                 "<whois-resources xmlns:xlink=\"http://www.w3.org/1999/xlink\">" +
                 "<objects>" +
                 "<object type=\"aut-num\">" +
                 "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test-grs/aut-num/AS102\" />" +
                 "<source id=\"test-grs\" />" +
                 "<primary-key>" +
                 "<attribute name=\"aut-num\" value=\"AS102\" />" +
                 "</primary-key>" +
                 "<attributes>" +
                 "<attribute name=\"aut-num\" value=\"AS102\" />" +
                 "<attribute name=\"as-name\" value=\"End-User-2\" />" +
                 "<attribute name=\"descr\" value=\"description\" />" +
                 "<attribute name=\"admin-c\" value=\"DUMY-RIPE\" />" +
                 "<attribute name=\"tech-c\" value=\"DUMY-RIPE\" />" +
                 "<attribute name=\"mnt-by\" value=\"OWNER-MNT\" referenced-type=\"mntner\">" +
                 "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test-grs/mntner/OWNER-MNT\" />" +
                 "</attribute>" +
                 "<attribute name=\"source\" value=\"TEST-GRS\" />" +
                 "<attribute name=\"remarks\" value=\"****************************\" />" +
                 "<attribute name=\"remarks\" value=\"* THIS OBJECT IS MODIFIED\" />" +
                 "<attribute name=\"remarks\" value=\"* Please note that all data that is generally regarded as personal\" />" +
                 "<attribute name=\"remarks\" value=\"* data has been removed from this object.\" />" +
                 "<attribute name=\"remarks\" value=\"* To view the original object, please query the RIPE Database at:\" />" +
                 "<attribute name=\"remarks\" value=\"* http://www.ripe.net/whois\" />" +
                 "<attribute name=\"remarks\" value=\"****************************\" />" +
                 "</attributes>" +
                 "<tags />" +
                 "</object>" +
                 "</objects>" +
                 "<terms-and-conditions xlink:type=\"locator\" xlink:href=\"http://www.ripe.net/db/support/db-terms-conditions.pdf\" />" +
                 "</whois-resources>"));
     }
 
     @Test
     public void lookup_accept_application_xml() {
         final String response = RestTest.target(getPort(), "whois/test/person/TP1-TEST")
                 .request(MediaType.APPLICATION_XML)
                 .get(String.class);
 
         assertThat(response, containsString("<?xml version='1.0' encoding='UTF-8'?>"));
         assertThat(response, containsString("<whois-resources"));
     }
 
     @Test
     public void lookup_accept_application_json() {
         final String response = RestTest.target(getPort(), "whois/test/person/TP1-TEST")
                 .request(MediaType.APPLICATION_JSON)
                 .get(String.class);
 
         assertThat(response, containsString("\"objects\""));
         assertThat(response, containsString("\"object\""));
         assertThat(response, containsString("\"type\""));
         assertThat(response, containsString("\"href\""));
     }
 
     @Test
     public void lookup_xml_response_doesnt_contain_invalid_values() {
         databaseHelper.addObject("" +
                 "mntner:      TEST-MNT\n" +
                 "descr:       escape invalid values like \uDC00Brat\u001b$B!l\u001b <b> <!-- &#x0;\n" +
                 "admin-c:     TP1-TEST\n" +
                 "upd-to:      noreply@ripe.net\n" +
                 "auth:        MD5-PW $1$d9fKeTr2$Si7YudNf4rUGmR71n/cqk/ #test\n" +
                 "mnt-by:      TEST-MNT\n" +
                 "referral-by: TEST-MNT\n" +
                 "changed:     dbtest@ripe.net 20120101\n" +
                 "source:      TEST");
 
         final String response = RestTest.target(getPort(), "whois/test/mntner/TEST-MNT")
                 .request(MediaType.APPLICATION_XML)
                 .get(String.class);
 
         assertThat(response, not(containsString("\u001b")));
         assertThat(response, not(containsString("<b>")));
         assertThat(response, not(containsString("&#x0;")));
         assertThat(response, not(containsString("<!--")));
     }
 
     // create
 
     @Test
     public void create_succeeds() throws Exception {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/person?password=test")
                 .request()
                 .post(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_XML))
                 .readEntity(WhoisResources.class);
 
         assertThat(whoisResources.getLink().getHref(), is(String.format("http://localhost:%s/test/person?password=test", getPort())));
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         final WhoisObject object = whoisResources.getWhoisObjects().get(0);
 
         assertThat(object.getAttributes(), contains(
                 new Attribute("person", "Pauleth Palthen"),
                 new Attribute("address", "Singel 258"),
                 new Attribute("phone", "+31-1234567890"),
                 new Attribute("e-mail", "noreply@ripe.net"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("nic-hdl", "PP1-TEST"),
                 new Attribute("changed", "noreply@ripe.net 20120101"),
                 new Attribute("remarks", "remark"),
                 new Attribute("source", "TEST")));
 
         assertThat(whoisResources.getTermsAndConditions().getHref(), is(WhoisResources.TERMS_AND_CONDITIONS));
     }
 
     @Test
     public void create_invalid_source_in_request_body() {
         final RpslObject rpslObject = RpslObject.parse("" +
                 "person:  Pauleth Palthen\n" +
                 "address: Singel 258\n" +
                 "phone:   +31-1234567890\n" +
                 "e-mail:  noreply@ripe.net\n" +
                 "mnt-by:  OWNER-MNT\n" +
                 "nic-hdl: PP1-TEST\n" +
                 "changed: noreply@ripe.net 20120101\n" +
                 "remarks: remark\n" +
                 "source:  NONE\n");
         try {
             RestTest.target(getPort(), "whois/test/person?password=test")
                     .request()
                     .post(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(rpslObject)), MediaType.APPLICATION_XML), String.class);
             fail("expected request to fail");
         } catch (BadRequestException e) {
             assertOnlyErrorMessage(e, "Error", "Unrecognized source: %s", "NONE");
         }
     }
 
     @Test
     public void create_invalid_reference() {
         try {
             RestTest.target(getPort(), "whois/test/person?password=test")
                     .request()
                     .post(Entity.entity("<whois-resources>\n" +
                             "    <objects>\n" +
                             "        <object type=\"person\">\n" +
                             "            <source id=\"RIPE\"/>\n" +
                             "            <attributes>\n" +
                             "                <attribute name=\"person\" value=\"Pauleth Palthen\"/>\n" +
                             "                <attribute name=\"address\" value=\"Singel 258\"/>\n" +
                             "                <attribute name=\"phone\" value=\"+31-1234567890\"/>\n" +
                             "                <attribute name=\"e-mail\" value=\"noreply@ripe.net\"/>\n" +
                             "                <attribute name=\"admin-c\" value=\"INVALID\"/>\n" +
                             "                <attribute name=\"mnt-by\" value=\"OWNER-MNT\"/>\n" +
                             "                <attribute name=\"nic-hdl\" value=\"PP1-TEST\"/>\n" +
                             "                <attribute name=\"changed\" value=\"ppalse@ripe.net 20101228\"/>\n" +
                             "                <attribute name=\"source\" value=\"RIPE\"/>\n" +
                             "            </attributes>\n" +
                             "        </object>\n" +
                             "    </objects>\n" +
                             "</whois-resources>", MediaType.APPLICATION_XML), String.class);
             fail();
         } catch (BadRequestException e) {
             final WhoisResources whoisResources = mapClientException(e);
             assertErrorMessage(whoisResources, 0, "Error", "Unrecognized source: %s", "RIPE");
             assertErrorMessage(whoisResources, 1, "Error", "\"%s\" is not valid for this object type", "admin-c");
         }
     }
 
     @Test(expected = BadRequestException.class)
     public void create_bad_imput_empty_objects_element() {
             RestTest.target(getPort(), "whois/test/person?password=test")
                     .request()
                     .post(Entity.entity("<whois-resources>\n<objects/>\n</whois-resources>", MediaType.APPLICATION_XML), String.class);
     }
 
     @Test(expected = BadRequestException.class)
     public void create_bad_imput_no_objects_element() {
             RestTest.target(getPort(), "whois/test/person?password=test")
                     .request()
                     .post(Entity.entity("<whois-resources/>", MediaType.APPLICATION_XML), String.class);
     }
 
     @Test(expected = BadRequestException.class)
     public void create_bad_input_empty_body() {
             RestTest.target(getPort(), "whois/test/person?password=test")
                     .request()
                     .post(Entity.entity("", MediaType.APPLICATION_XML), String.class);
     }
 
     @Test
     public void create_multiple_passwords() {
         WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/person?password=invalid&password=test")
                 .request()
                 .post(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_XML), WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
     }
 
     @Test
     public void create_invalid_password() {
         try {
             RestTest.target(getPort(), "whois/test/person?password=invalid")
                     .request()
                     .post(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_XML), String.class);
             fail();
         } catch (NotAuthorizedException e) {
             assertOnlyErrorMessage(e, "Error", "Authorisation for [%s] %s failed\nusing \"%s:\"\nnot authenticated by: %s", "person", "PP1-TEST", "mnt-by", "OWNER-MNT");
         }
     }
 
     @Test
     public void create_no_password() {
         try {
             RestTest.target(getPort(), "whois/test/person")
                     .request(MediaType.APPLICATION_XML)
                     .post(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_XML), String.class);
             fail();
         } catch (NotAuthorizedException e) {
             assertOnlyErrorMessage(e, "Error", "Authorisation for [%s] %s failed\nusing \"%s:\"\nnot authenticated by: %s", "person", "PP1-TEST", "mnt-by", "OWNER-MNT");
         }
     }
 
     @Test
     public void create_already_exists() {
         try {
             RestTest.target(getPort(), "whois/test/person?password=test")
                     .request()
                     .post(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(OWNER_MNT)), MediaType.APPLICATION_XML), String.class);
             fail();
         } catch (ClientErrorException e1) {
             assertThat(e1.getResponse().getStatus(), is(Response.Status.CONFLICT.getStatusCode()));
             assertOnlyErrorMessage(e1, "Error", "Enforced new keyword specified, but the object already exists in the database");
         }
     }
 
     @Test
     public void create_delete_method_not_allowed() {
         try {
             RestTest.target(getPort(), "whois/test/person")
                     .request()
                     .delete(String.class);
             fail();
         } catch (NotAllowedException e) {
             // expected
         }
     }
 
     @Test
     public void create_get_resource_not_found() {
         try {
             RestTest.target(getPort(), "whois/test")
                     .request(MediaType.APPLICATION_XML)
                     .get(WhoisResources.class);
             fail();
         } catch (NotFoundException e) {
             // expected
         }
     }
 
     @Test
     public void create_person_xml_text() {
         final String response = RestTest.target(getPort(), "whois/test/person?password=test")
                 .request(MediaType.APPLICATION_XML_TYPE)
                 .post(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_JSON), String.class);
 
        assertThat(response, is(String.format(
                 "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                         "<whois-resources xmlns:xlink=\"http://www.w3.org/1999/xlink\">" +
                        "<link xlink:type=\"locator\" xlink:href=\"http://localhost:%d/test/person?password=test\"/>" +
                         "<objects>" +
                         "<object type=\"person\">" +
                         "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test/person/PP1-TEST\"/>" +
                         "<source id=\"test\"/>" +
                         "<primary-key>" +
                         "<attribute name=\"nic-hdl\" value=\"PP1-TEST\"/>" +
                         "</primary-key>" +
                         "<attributes>" +
                         "<attribute name=\"person\" value=\"Pauleth Palthen\"/>" +
                         "<attribute name=\"address\" value=\"Singel 258\"/>" +
                         "<attribute name=\"phone\" value=\"+31-1234567890\"/>" +
                         "<attribute name=\"e-mail\" value=\"noreply@ripe.net\"/>" +
                         "<attribute name=\"mnt-by\" value=\"OWNER-MNT\" referenced-type=\"mntner\">" +
                         "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\"/>" +
                         "</attribute>" +
                         "<attribute name=\"nic-hdl\" value=\"PP1-TEST\"/>" +
                         "<attribute name=\"changed\" value=\"noreply@ripe.net 20120101\"/>" +
                         "<attribute name=\"remarks\" value=\"remark\"/>" +
                         "<attribute name=\"source\" value=\"TEST\"/>" +
                         "</attributes>" +
                         "</object>" +
                         "</objects>" +
                         "<terms-and-conditions xlink:type=\"locator\" xlink:href=\"http://www.ripe.net/db/support/db-terms-conditions.pdf\"/>" +
                        "</whois-resources>",getPort())));
     }
 
     @Test
     public void create_person_json_text() {
         final String response = RestTest.target(getPort(), "whois/test/person?password=test")
                 .request(MediaType.APPLICATION_JSON_TYPE)
                 .post(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_JSON), String.class);
 
        assertThat(response, is(String.format(
                 "{\n" +
                 "  \"link\" : {\n" +
                 "    \"type\" : \"locator\",\n" +
                "    \"href\" : \"http://localhost:%d/test/person?password=test\"\n" +
                 "  },\n" +
                 "  \"objects\" : {\n" +
                 "    \"object\" : [ {\n" +
                 "      \"type\" : \"person\",\n" +
                 "      \"link\" : {\n" +
                 "        \"type\" : \"locator\",\n" +
                 "        \"href\" : \"http://rest-test.db.ripe.net/test/person/PP1-TEST\"\n" +
                 "      },\n" +
                 "      \"source\" : {\n" +
                 "        \"id\" : \"test\"\n" +
                 "      },\n" +
                 "      \"primary-key\" : {\n" +
                 "        \"attribute\" : [ {\n" +
                 "          \"name\" : \"nic-hdl\",\n" +
                 "          \"value\" : \"PP1-TEST\"\n" +
                 "        } ]\n" +
                 "      },\n" +
                 "      \"attributes\" : {\n" +
                 "        \"attribute\" : [ {\n" +
                 "          \"name\" : \"person\",\n" +
                 "          \"value\" : \"Pauleth Palthen\"\n" +
                 "        }, {\n" +
                 "          \"name\" : \"address\",\n" +
                 "          \"value\" : \"Singel 258\"\n" +
                 "        }, {\n" +
                 "          \"name\" : \"phone\",\n" +
                 "          \"value\" : \"+31-1234567890\"\n" +
                 "        }, {\n" +
                 "          \"name\" : \"e-mail\",\n" +
                 "          \"value\" : \"noreply@ripe.net\"\n" +
                 "        }, {\n" +
                 "          \"link\" : {\n" +
                 "            \"type\" : \"locator\",\n" +
                 "            \"href\" : \"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\"\n" +
                 "          },\n" +
                 "          \"name\" : \"mnt-by\",\n" +
                 "          \"value\" : \"OWNER-MNT\",\n" +
                 "          \"referenced-type\" : \"mntner\"\n" +
                 "        }, {\n" +
                 "          \"name\" : \"nic-hdl\",\n" +
                 "          \"value\" : \"PP1-TEST\"\n" +
                 "        }, {\n" +
                 "          \"name\" : \"changed\",\n" +
                 "          \"value\" : \"noreply@ripe.net 20120101\"\n" +
                 "        }, {\n" +
                 "          \"name\" : \"remarks\",\n" +
                 "          \"value\" : \"remark\"\n" +
                 "        }, {\n" +
                 "          \"name\" : \"source\",\n" +
                 "          \"value\" : \"TEST\"\n" +
                 "        } ]\n" +
                 "      }\n" +
                 "    } ]\n" +
                 "  },\n" +
                 "  \"terms-and-conditions\" : {\n" +
                 "    \"type\" : \"locator\",\n" +
                 "    \"href\" : \"http://www.ripe.net/db/support/db-terms-conditions.pdf\"\n" +
                 "  }\n" +
                "}",getPort())));
     }
 
     @Test
     public void create_utf8_character_encoding() {
         final RpslObject person = RpslObject.parse("" +
             "person:    Pauleth Palthen\n" +
             "address:   test \u03A3 and \u00DF characters\n" +
             "phone:     +31-1234567890\n" +
             "e-mail:    noreply@ripe.net\n" +
             "mnt-by:    OWNER-MNT\n" +
             "nic-hdl:   PP1-TEST\n" +
             "changed:   noreply@ripe.net 20120101\n" +
             "remarks:   remark\n" +
             "source:    TEST\n");
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/person?password=test")
                 .request()
                 .post(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(person)), MediaType.APPLICATION_XML))
                 .readEntity(WhoisResources.class);
 
         // UTF-8 characters are mapped to latin1. Characters outside the latin1 charset are substituted by '?'
         final WhoisObject responseObject = whoisResources.getWhoisObjects().get(0);
         assertThat(responseObject.getAttributes().get(1).getValue(), is("test ? and \u00DF characters"));
     }
 
     // delete
 
     @Test
     public void delete_succeeds() {
         databaseHelper.addObject(PAULETH_PALTHEN);
         WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/person/PP1-TEST?password=test").request().delete(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         try {
             databaseHelper.lookupObject(ObjectType.PERSON, "PP1-TEST");
             fail();
         } catch (EmptyResultDataAccessException ignored) {
             // expected
         }
     }
 
     @Test
     public void delete_with_reason_succeeds() {
         databaseHelper.addObject(PAULETH_PALTHEN);
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/person/PP1-TEST?password=test&reason=not_needed_no_more").request().delete(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         try {
             databaseHelper.lookupObject(ObjectType.PERSON, "PP1-TEST");
             fail();
         } catch (EmptyResultDataAccessException expected) {}
     }
 
     @Test
     public void delete_nonexistant() {
         try {
             RestTest.target(getPort(), "whois/test/person/NON-EXISTANT").request().delete(String.class);
             fail();
         } catch (NotFoundException ignored) {
             // expected
         }
     }
 
     @Test
     public void delete_referenced_from_other_objects() {
         try {
             RestTest.target(getPort(), "whois/test/person/TP1-TEST?password=test").request().delete(WhoisResources.class);
             fail();
         } catch (BadRequestException e) {
             assertOnlyErrorMessage(e, "Error", "Object [%s] %s is referenced from other objects", "person", "TP1-TEST");
         }
     }
 
     @Test
     public void delete_invalid_password() {
         try {
             databaseHelper.addObject(PAULETH_PALTHEN);
             RestTest.target(getPort(), "whois/test/person/PP1-TEST?password=invalid").request().delete(String.class);
             fail();
         } catch (NotAuthorizedException e) {
             assertOnlyErrorMessage(e, "Error", "Authorisation for [%s] %s failed\nusing \"%s:\"\nnot authenticated by: %s", "person", "PP1-TEST", "mnt-by", "OWNER-MNT");
         }
     }
 
     @Test
     public void delete_no_password() {
         try {
             databaseHelper.addObject(PAULETH_PALTHEN);
             RestTest.target(getPort(), "whois/test/person/PP1-TEST").request().delete(String.class);
             fail();
         } catch (NotAuthorizedException e) {
             assertOnlyErrorMessage(e, "Error", "Authorisation for [%s] %s failed\nusing \"%s:\"\nnot authenticated by: %s", "person", "PP1-TEST", "mnt-by", "OWNER-MNT");
         }
     }
 
     // update
 
     @Test
     public void update_succeeds() {
         databaseHelper.addObject(PAULETH_PALTHEN);
 
         final RpslObject updatedObject = new RpslObjectBuilder(PAULETH_PALTHEN).addAttribute(new RpslAttribute(AttributeType.REMARKS, "updated")).sort().get();
 
         WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/person/PP1-TEST?password=test")
                 .request(MediaType.APPLICATION_XML)
                 .put(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(updatedObject)), MediaType.APPLICATION_XML), WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject object = whoisResources.getWhoisObjects().get(0);
         assertThat(object.getAttributes(), contains(
                 new Attribute("person", "Pauleth Palthen"),
                 new Attribute("address", "Singel 258"),
                 new Attribute("phone", "+31-1234567890"),
                 new Attribute("e-mail", "noreply@ripe.net"),
                 new Attribute("nic-hdl", "PP1-TEST"),
                 new Attribute("remarks", "remark"),
                 new Attribute("remarks", "updated"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("changed", "noreply@ripe.net 20120101"),
                 new Attribute("source", "TEST")));
 
         assertThat(whoisResources.getTermsAndConditions().getHref(), is(WhoisResources.TERMS_AND_CONDITIONS));
     }
 
     @Test
     public void update_noop() {
         databaseHelper.addObject(PAULETH_PALTHEN);
         WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/person/PP1-TEST?password=test")
                 .request(MediaType.APPLICATION_XML)
                 .put(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_XML), WhoisResources.class);
 
         assertErrorMessage(whoisResources, 0, "Warning", "Submitted object identical to database object");
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject object = whoisResources.getWhoisObjects().get(0);
         assertThat(object.getAttributes(), containsInAnyOrder(
                 new Attribute("person", "Pauleth Palthen"),
                 new Attribute("address", "Singel 258"),
                 new Attribute("phone", "+31-1234567890"),
                 new Attribute("e-mail", "noreply@ripe.net"),
                 new Attribute("nic-hdl", "PP1-TEST"),
                 new Attribute("remarks", "remark"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("changed", "noreply@ripe.net 20120101"),
                 new Attribute("source", "TEST")));
 
         assertThat(whoisResources.getTermsAndConditions().getHref(), is(WhoisResources.TERMS_AND_CONDITIONS));
     }
 
     @Test
     public void update_noop_with_overrides() {
         databaseHelper.addObject(PAULETH_PALTHEN);
         databaseHelper.insertUser(User.createWithPlainTextPassword("agoston", "zoh", ObjectType.PERSON));
 
         WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/person/PP1-TEST?override=agoston,zoh,reason")
                 .request(MediaType.APPLICATION_XML)
                 .put(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_XML), WhoisResources.class);
 
         assertErrorMessage(whoisResources, 0, "Warning", "Submitted object identical to database object");
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject object = whoisResources.getWhoisObjects().get(0);
         assertThat(object.getAttributes(), containsInAnyOrder(
                 new Attribute("person", "Pauleth Palthen"),
                 new Attribute("address", "Singel 258"),
                 new Attribute("phone", "+31-1234567890"),
                 new Attribute("e-mail", "noreply@ripe.net"),
                 new Attribute("nic-hdl", "PP1-TEST"),
                 new Attribute("remarks", "remark"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("changed", "noreply@ripe.net 20120101"),
                 new Attribute("source", "TEST")));
 
         assertThat(whoisResources.getTermsAndConditions().getHref(), is(WhoisResources.TERMS_AND_CONDITIONS));
     }
 
     @Test
     public void update_spaces_in_password_succeeds() {
         databaseHelper.addObject(RpslObject.parse(
                 "mntner:      OWNER2-MNT\n" +
                         "descr:       Owner Maintainer\n" +
                         "admin-c:     TP1-TEST\n" +
                         "upd-to:      noreply@ripe.net\n" +
                         "auth:        MD5-PW $1$d9fKeTr2$NitG3QQZnA4z6zp1o.qmm/ # ' spaces '\n" +
                         "mnt-by:      OWNER2-MNT\n" +
                         "referral-by: OWNER2-MNT\n" +
                         "changed:     dbtest@ripe.net 20120101\n" +
                         "source:      TEST"));
 
         final String response = RestTest.target(getPort(), "whois/test/mntner/OWNER2-MNT?password=%20spaces%20")
                 .request(MediaType.APPLICATION_XML)
                 .put(Entity.entity("<whois-resources>\n" +
                         "    <objects>\n" +
                         "        <object type=\"mntner\">\n" +
                         "            <source id=\"TEST\"/>\n" +
                         "            <attributes>\n" +
                         "                <attribute name=\"mntner\" value=\"OWNER2-MNT\"/>\n" +
                         "                <attribute name=\"descr\" value=\"Owner Maintainer\"/>\n" +
                         "                <attribute name=\"admin-c\" value=\"TP1-TEST\"/>\n" +
                         "                <attribute name=\"upd-to\" value=\"noreply@ripe.net\"/>\n" +
                         "                <attribute name=\"auth\" value=\"MD5-PW $1$d9fKeTr2$NitG3QQZnA4z6zp1o.qmm/\"/>\n" +
                         "                <attribute name=\"remarks\" value=\"updated\"/>\n" +
                         "                <attribute name=\"mnt-by\" value=\"OWNER2-MNT\"/>\n" +
                         "                <attribute name=\"referral-by\" value=\"OWNER2-MNT\"/>\n" +
                         "                <attribute name=\"changed\" value=\"dbtest@ripe.net 20120102\"/>\n" +
                         "                <attribute name=\"source\" value=\"TEST\"/>\n" +
                         "            </attributes>\n" +
                         "        </object>\n" +
                         "    </objects>\n" +
                         "</whois-resources>", MediaType.APPLICATION_XML), String.class);
 
         assertThat(response, containsString("<attribute name=\"remarks\" value=\"updated\"/>"));
         assertThat(response, not(containsString("errormessages")));
     }
 
     @Test
     public void update_json_request_and_response_content() {
         final String update =
                 "{\n" +
                         "  \"objects\" : {\n" +
                         "      \"object\" : [ {\n" +
                         "        \"source\" : {\n" +
                         "          \"id\" : \"test\"\n" +
                         "        },\n" +
                         "        \"attributes\" : {\n" +
                         "          \"attribute\" : [\n" +
                         "            {\"name\":\"mntner\", \"value\":\"OWNER-MNT\"},\n" +
                         "            {\"name\":\"descr\", \"value\":\"description\"},\n" +
                         "            {\"name\":\"admin-c\", \"value\":\"TP1-TEST\"},\n" +
                         "            {\"name\":\"upd-to\", \"value\":\"noreply@ripe.net\"},\n" +
                         "            {\"name\":\"auth\", \"value\":\"MD5-PW $1$d9fKeTr2$Si7YudNf4rUGmR71n/cqk/\"},\n" +
                         "            {\"name\":\"mnt-by\", \"value\":\"OWNER-MNT\"},\n" +
                         "            {\"name\":\"referral-by\", \"value\":\"OWNER-MNT\"},\n" +
                         "            {\"name\":\"changed\", \"value\":\"dbtest@ripe.net 20120101\"},\n" +
                         "            {\"name\":\"source\", \"value\":\"TEST\"}\n" +
                         "        ] }\n" +
                         "     }]\n" +
                         "   }\n" +
                         "}";
 
         final String response = RestTest.target(getPort(), "whois/test/mntner/OWNER-MNT?password=test")
                 .request(MediaType.APPLICATION_JSON)
                 .put(Entity.entity(update, MediaType.APPLICATION_JSON), String.class);
 
        assertThat(response, is(String.format(
                 "{\n" +
                 "  \"link\" : {\n" +
                 "    \"type\" : \"locator\",\n" +
                "    \"href\" : \"http://localhost:%d/test/mntner/OWNER-MNT?password=test\"\n" +
                 "  },\n" +
                 "  \"objects\" : {\n" +
                 "    \"object\" : [ {\n" +
                 "      \"type\" : \"mntner\",\n" +
                 "      \"link\" : {\n" +
                 "        \"type\" : \"locator\",\n" +
                 "        \"href\" : \"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\"\n" +
                 "      },\n" +
                 "      \"source\" : {\n" +
                 "        \"id\" : \"test\"\n" +
                 "      },\n" +
                 "      \"primary-key\" : {\n" +
                 "        \"attribute\" : [ {\n" +
                 "          \"name\" : \"mntner\",\n" +
                 "          \"value\" : \"OWNER-MNT\"\n" +
                 "        } ]\n" +
                 "      },\n" +
                 "      \"attributes\" : {\n" +
                 "        \"attribute\" : [ {\n" +
                 "          \"name\" : \"mntner\",\n" +
                 "          \"value\" : \"OWNER-MNT\"\n" +
                 "        }, {\n" +
                 "          \"name\" : \"descr\",\n" +
                 "          \"value\" : \"description\"\n" +
                 "        }, {\n" +
                 "          \"link\" : {\n" +
                 "            \"type\" : \"locator\",\n" +
                 "            \"href\" : \"http://rest-test.db.ripe.net/test/person/TP1-TEST\"\n" +
                 "          },\n" +
                 "          \"name\" : \"admin-c\",\n" +
                 "          \"value\" : \"TP1-TEST\",\n" +
                 "          \"referenced-type\" : \"person\"\n" +
                 "        }, {\n" +
                 "          \"name\" : \"upd-to\",\n" +
                 "          \"value\" : \"noreply@ripe.net\"\n" +
                 "        }, {\n" +
                 "          \"name\" : \"auth\",\n" +
                 "          \"value\" : \"MD5-PW $1$d9fKeTr2$Si7YudNf4rUGmR71n/cqk/\"\n" +
                 "        }, {\n" +
                 "          \"link\" : {\n" +
                 "            \"type\" : \"locator\",\n" +
                 "            \"href\" : \"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\"\n" +
                 "          },\n" +
                 "          \"name\" : \"mnt-by\",\n" +
                 "          \"value\" : \"OWNER-MNT\",\n" +
                 "          \"referenced-type\" : \"mntner\"\n" +
                 "        }, {\n" +
                 "          \"link\" : {\n" +
                 "            \"type\" : \"locator\",\n" +
                 "            \"href\" : \"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\"\n" +
                 "          },\n" +
                 "          \"name\" : \"referral-by\",\n" +
                 "          \"value\" : \"OWNER-MNT\",\n" +
                 "          \"referenced-type\" : \"mntner\"\n" +
                 "        }, {\n" +
                 "          \"name\" : \"changed\",\n" +
                 "          \"value\" : \"dbtest@ripe.net 20120101\"\n" +
                 "        }, {\n" +
                 "          \"name\" : \"source\",\n" +
                 "          \"value\" : \"TEST\"\n" +
                 "        } ]\n" +
                 "      }\n" +
                 "    } ]\n" +
                 "  },\n" +
                 "  \"terms-and-conditions\" : {\n" +
                 "    \"type\" : \"locator\",\n" +
                 "    \"href\" : \"http://www.ripe.net/db/support/db-terms-conditions.pdf\"\n" +
                 "  }\n" +
                "}",getPort())));
     }
 
     @Test
     public void update_path_vs_object_mismatch_objecttype() throws Exception {
         try {
             databaseHelper.addObject(PAULETH_PALTHEN);
             RestTest.target(getPort(), "whois/test/mntner/PP1-TEST?password=test")
                     .request(MediaType.APPLICATION_XML)
                     .put(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_XML), WhoisResources.class);
             fail();
         } catch (BadRequestException e) {
             assertOnlyErrorMessage(e, "Error", "Object type and key specified in URI (%s: %s) do not match the WhoisResources contents", "mntner", "PP1-TEST");
         }
     }
 
     @Test
     public void update_path_vs_object_mismatch_key() throws Exception {
         try {
             RestTest.target(getPort(), "whois/test/mntner/OWNER-MNT?password=test")
                     .request(MediaType.APPLICATION_XML)
                     .put(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_XML), WhoisResources.class);
             fail();
         } catch (BadRequestException e) {
             assertOnlyErrorMessage(e, "Error", "Object type and key specified in URI (%s: %s) do not match the WhoisResources contents", "mntner", "OWNER-MNT");
         }
     }
 
     @Test
     public void update_without_query_params() {
         try {
             databaseHelper.addObject(PAULETH_PALTHEN);
             RestTest.target(getPort(), "whois/test/person/PP1-TEST")
                     .request(MediaType.APPLICATION_XML)
                     .put(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_XML), WhoisResources.class);
             fail();
         } catch (NotAuthorizedException e) {
             assertOnlyErrorMessage(e, "Error", "Authorisation for [%s] %s failed\nusing \"%s:\"\nnot authenticated by: %s",
                     "person", "PP1-TEST", "mnt-by", "OWNER-MNT");
         }
     }
 
     @Test
     public void update_post_not_allowed() {
         try {
             RestTest.target(getPort(), "whois/test/person/PP1-TEST?password=test")
                     .request(MediaType.APPLICATION_XML)
                     .post(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_XML), String.class);
             fail();
         } catch (NotAllowedException ignored) {
             // expected
         }
     }
 
     // versions
 
     @Test
     public void versions_returns_xml() throws IOException {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "changed:        noreply@ripe.net 20120101\n" +
                 "source:         TEST\n");
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/aut-num/AS102/versions")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         final WhoisVersions whoisVersions = whoisResources.getVersions();
         assertThat(whoisVersions.getType(), is("aut-num"));
         assertThat(whoisVersions.getKey(), is("AS102"));
         assertThat(whoisVersions.getVersions(), hasSize(1));
         final WhoisVersion whoisVersion = whoisVersions.getVersions().get(0);
         assertThat(whoisVersion, is(new WhoisVersion("ADD/UPD", whoisVersion.getDate(), 1)));
     }
 
     @Test
     public void versions_deleted() throws IOException {
         final RpslObject autnum = RpslObject.parse("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "changed:        noreply@ripe.net 20120101\n" +
                 "source:         TEST\n");
         databaseHelper.addObject(autnum);
         databaseHelper.deleteObject(autnum);
         databaseHelper.addObject(autnum);
         databaseHelper.updateObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "changed:        noreply@ripe.net 20120101\n" +
                 "source:         TEST\n");
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/aut-num/AS102/versions")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
         assertThat(whoisResources.getErrorMessages(), is(empty()));
 
         final List<WhoisVersion> versions = whoisResources.getVersions().getVersions();
         assertThat(versions, hasSize(3));
         assertThat(versions.get(0).getDeletedDate(), is(not(nullValue())));
         assertThat(versions.get(0).getOperation(), is(nullValue()));
         assertThat(versions.get(0).getDate(), is(nullValue()));
         assertThat(versions.get(0).getRevision(), is(nullValue()));
 
         assertThat(versions.get(1).getDeletedDate(), is(nullValue()));
         assertThat(versions.get(1).getOperation(), is("ADD/UPD"));
         assertThat(versions.get(1).getRevision(), is(1));
         assertThat(versions.get(1).getDate(), stringMatchesRegexp(VERSION_DATE_PATTERN));
 
         assertThat(versions.get(2).getDeletedDate(), is(nullValue()));
         assertThat(versions.get(2).getOperation(), is("ADD/UPD"));
         assertThat(versions.get(2).getRevision(), is(2));
         assertThat(versions.get(2).getDate(), stringMatchesRegexp(VERSION_DATE_PATTERN));
     }
 
     @Test
     public void versions_deleted_versions_json() throws IOException {
         final RpslObject autnum = RpslObject.parse("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "changed:        noreply@ripe.net 20120101\n" +
                 "source:         TEST\n");
         databaseHelper.addObject(autnum);
         databaseHelper.deleteObject(autnum);
         databaseHelper.addObject(autnum);
         databaseHelper.updateObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "changed:        noreply@ripe.net 20120101\n" +
                 "source:         TEST\n");
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/aut-num/AS102/versions")
                 .request(MediaType.APPLICATION_JSON)
                 .get(WhoisResources.class);
         assertThat(whoisResources.getErrorMessages(), is(empty()));
 
         final List<WhoisVersion> versions = whoisResources.getVersions().getVersions();
         assertThat(versions, hasSize(3));
         assertThat(versions.get(0).getDeletedDate(), stringMatchesRegexp(VERSION_DATE_PATTERN));
         assertThat(versions.get(0).getOperation(), is(nullValue()));
         assertThat(versions.get(0).getDate(), is(nullValue()));
         assertThat(versions.get(0).getRevision(), is(nullValue()));
 
         assertThat(versions.get(1).getDeletedDate(), is(nullValue()));
         assertThat(versions.get(1).getOperation(), is("ADD/UPD"));
         assertThat(versions.get(1).getRevision(), is(1));
         assertThat(versions.get(1).getDate(), stringMatchesRegexp(VERSION_DATE_PATTERN));
 
         assertThat(versions.get(2).getDeletedDate(), is(nullValue()));
         assertThat(versions.get(2).getOperation(), is("ADD/UPD"));
         assertThat(versions.get(2).getRevision(), is(2));
         assertThat(versions.get(2).getDate(), stringMatchesRegexp(VERSION_DATE_PATTERN));
     }
 
     @Test
     public void versions_last_version_deleted() throws IOException {
         final RpslObject autnum = RpslObject.parse("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "changed:        noreply@ripe.net 20120101\n" +
                 "source:         TEST\n");
         databaseHelper.addObject(autnum);
         databaseHelper.deleteObject(autnum);
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/aut-num/AS102/versions")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
         assertThat(whoisResources.getErrorMessages(), is(empty()));
 
         final List<WhoisVersion> versions = whoisResources.getVersions().getVersions();
         assertThat(versions, hasSize(1));
         assertThat(versions.get(0).getDeletedDate(), stringMatchesRegexp(VERSION_DATE_PATTERN));
         assertThat(versions.get(0).getOperation(), is(nullValue()));
         assertThat(versions.get(0).getDate(), is(nullValue()));
         assertThat(versions.get(0).getRevision(), is(nullValue()));
     }
 
     @Test
     public void versions_no_versions_found() throws IOException {
         try {
             RestTest.target(getPort(), "whois/test/aut-num/AS102/versions")
                     .request(MediaType.APPLICATION_XML)
                     .get(String.class);
             fail();
         } catch (NotFoundException ignored) {
             // expected
         }
     }
 
     @Test
     public void version_nonexistant_version() throws IOException {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "changed:        noreply@ripe.net 20120101\n" +
                 "source:         TEST\n");
 
         try {
             RestTest.target(getPort(), "whois/test/aut-num/AS102/versions/2")
                     .request(MediaType.APPLICATION_XML)
                     .get(WhoisResources.class);
             fail();
         } catch (NotFoundException ignored) {
             // expected
         }
     }
 
     @Test
     public void version_wrong_object_type() throws IOException {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "changed:        noreply@ripe.net 20120101\n" +
                 "source:         TEST\n");
 
         try {
             RestTest.target(getPort(), "whois/test/inetnum/AS102/versions/1")
                     .request(MediaType.APPLICATION_XML)
                     .get(WhoisResources.class);
             fail();
         } catch (NotFoundException ignored) {
             // expected
         }
     }
 
     @Test
     public void version_returns_xml() throws IOException {
         final RpslObject autnum = RpslObject.parse("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
         databaseHelper.addObject(autnum);
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/aut-num/AS102/versions/1")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject object = whoisResources.getWhoisObjects().get(0);
         assertThat(object.getType(), is("aut-num"));
         assertThat(object.getVersion(), is(1));
         final List<Attribute> attributes = object.getAttributes();
         final List<RpslAttribute> originalAttributes = autnum.getAttributes();
         for (int i = 0; i < originalAttributes.size(); i++) {
             assertThat(originalAttributes.get(i).getCleanValue().toString(), is(attributes.get(i).getValue()));
         }
     }
 
     @Test
     public void version_returns_json() throws IOException {
         final RpslObject autnum = RpslObject.parse("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
         databaseHelper.addObject(autnum);
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/aut-num/AS102/versions/1")
                 .request(MediaType.APPLICATION_JSON)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects().size(), is(1));
         final WhoisObject object = whoisResources.getWhoisObjects().get(0);
         assertThat(object.getType(), is("aut-num"));
         assertThat(object.getVersion(), is(1));
 
         final List<Attribute> attributes = object.getAttributes();
         final List<RpslAttribute> originalAttributes = autnum.getAttributes();
         for (int i = 0; i < originalAttributes.size(); i++) {
             assertThat(originalAttributes.get(i).getCleanValue().toString(), is(attributes.get(i).getValue()));
         }
     }
 
     @Test
     public void version_not_showing_deleted_version() throws IOException {
         final RpslObject autnum = RpslObject.parse("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "changed:        noreply@ripe.net 20120101\n" +
                 "source:         TEST\n");
         databaseHelper.addObject(autnum);
         databaseHelper.deleteObject(autnum);
 
         try {
             RestTest.target(getPort(), "whois/test/aut-num/AS102/versions/1")
                     .request(MediaType.APPLICATION_XML)
                     .get(WhoisResources.class);
             fail();
         } catch (NotFoundException ignored) {
             // expected
         }
     }
 
     // search
 
     @Test
     public void search() {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/search?query-string=AS102&source=TEST")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(2));
         assertThat(whoisResources.getService().getName(), is(WhoisRestService.SERVICE_SEARCH));
 
         final WhoisObject autnum = whoisResources.getWhoisObjects().get(0);
         assertThat(autnum.getType(), is("aut-num"));
         assertThat(autnum.getLink(), is(new Link("locator", "http://rest-test.db.ripe.net/test/aut-num/AS102")));
         assertThat(autnum.getPrimaryKey().get(0).getValue(), is("AS102"));
 
         assertThat(autnum.getAttributes(), contains(
                 new Attribute("aut-num", "AS102"),
                 new Attribute("as-name", "End-User-2"),
                 new Attribute("descr", "description"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("tech-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST")
         ));
 
         final WhoisObject person = whoisResources.getWhoisObjects().get(1);
         assertThat(person.getType(), is("person"));
         assertThat(person.getPrimaryKey().get(0).getValue(), is("TP1-TEST"));
         assertThat(person.getLink(), is(new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")));
 
         assertThat(person.getAttributes(), contains(
                 new Attribute("person", "Test Person"),
                 new Attribute("address", "Singel 258"),
                 new Attribute("phone", "+31 6 12345678"),
                 new Attribute("nic-hdl", "TP1-TEST"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST", "Filtered", null, null)
         ));
         assertThat(whoisResources.getTermsAndConditions().getHref(), is(WhoisResources.TERMS_AND_CONDITIONS));
     }
 
     @Test
     public void search_accept_json() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/search?query-string=TP1-TEST&source=TEST")
                 .request(MediaType.APPLICATION_JSON_TYPE)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
 
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getPrimaryKey().get(0).getValue(), is("TP1-TEST"));
     }
 
     @Test
     public void search_json_extension() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/search.json?query-string=TP1-TEST&source=TEST")
                 .request()
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
 
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getPrimaryKey().get(0).getValue(), is("TP1-TEST"));
     }
 
     @Test
     public void search_with_long_options() {
         databaseHelper.addObject("" +
                 "person:    Lo Person\n" +
                 "admin-c:   TP1-TEST\n" +
                 "tech-c:    TP1-TEST\n" +
                 "nic-hdl:   LP1-TEST\n" +
                 "mnt-by:    OWNER-MNT\n" +
                 "source:    TEST\n");
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/search?query-string=LP1-TEST&source=TEST&flags=no-filtering&flags=rB")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
 
         final List<Flag> flags = whoisResources.getParameters().getFlags().getFlags();
         assertThat(flags, hasSize(2));
         assertThat(flags.get(0).getValue(), is("no-referenced"));
         assertThat(flags.get(1).getValue(), is("no-filtering"));
     }
 
     @Test
     public void search_with_short_and_long_options_together() {
         databaseHelper.addObject("" +
                 "person:    Lo Person\n" +
                 "admin-c:   TP1-TEST\n" +
                 "tech-c:    TP1-TEST\n" +
                 "nic-hdl:   LP1-TEST\n" +
                 "mnt-by:    OWNER-MNT\n" +
                 "source:    TEST\n");
 
         try {
             RestTest.target(getPort(), "whois/search?query-string=LP1-TEST&source=TEST&flags=show-tag-inforG")
                     .request(MediaType.APPLICATION_XML)
                     .get(WhoisResources.class);
             fail();
         } catch (BadRequestException e) {
             assertOnlyErrorMessage(e, "Error", "Invalid search flag '%s' (in parameter '%s')", "h", "show-tag-inforG");
         }
     }
 
     @Test
     public void search_space_with_dash_invalid_flag() {
         try {
             RestTest.target(getPort(), "whois/search?query-string=10.0.0.0%20%2D10.1.1.1&source=TEST")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
             fail();
         } catch (BadRequestException e) {
             assertOnlyErrorMessage(e, "Error", "Flags are not allowed in 'query-string'");
         }
     }
 
     @Test
     public void search_invalid_flag() {
         try {
             RestTest.target(getPort(), "whois/search?query-string=LP1-TEST&source=TEST&flags=q")
                     .request(MediaType.APPLICATION_XML)
                     .get(WhoisResources.class);
             fail();
         } catch (BadRequestException e) {
             assertOnlyErrorMessage(e, "Error", "Disallowed search flag '%s'", "q");
         }
     }
 
     @Test
     public void search_tags_in_response() {
         final RpslObject autnum = RpslObject.parse("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
         Map<RpslObject, RpslObjectUpdateInfo> updateInfos = databaseHelper.addObjects(Lists.newArrayList(autnum));
 
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "unref", "28");
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "foobar", "description");
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "other", "other stuff");
 
         final WhoisResources whoisResources = RestTest.target(getPort(),
                 "whois/TEST/aut-num/AS102?include-tag=foobar&include-tag=unref")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getTags(), contains(
                 new WhoisTag("foobar", "description"),
                 new WhoisTag("other", "other stuff"),
                 new WhoisTag("unref", "28")));
     }
 
     @Test
     public void search_include_tag_param() {
         final RpslObject autnum = RpslObject.parse("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
         Map<RpslObject, RpslObjectUpdateInfo> updateInfos = databaseHelper.addObjects(Lists.newArrayList(autnum));
 
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "unref", "28");
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "foobar", "description");
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "other", "other stuff");
 
         final WhoisResources whoisResources = RestTest.target(getPort(),
                 "whois/search?source=TEST&query-string=AS102&include-tag=foobar&include-tag=unref")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
 
         assertThat(whoisObject.getTags(), contains(
                 new WhoisTag("foobar", "description"),
                 new WhoisTag("other", "other stuff"),
                 new WhoisTag("unref", "28")));
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("aut-num", "AS102"),
                 new Attribute("as-name", "End-User-2"),
                 new Attribute("descr", "description"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("tech-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST")
         ));
     }
 
     @Test
     public void search_include_tag_param_no_results() {
         databaseHelper.addObject(RpslObject.parse("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n"));
 
         try {
             RestTest.target(getPort(),
                     "whois/search?source=TEST&query-string=AS102&include-tag=foobar")
                     .request(MediaType.APPLICATION_XML)
                     .get(WhoisResources.class);
             fail();
         } catch (NotFoundException ignored) {
             // expected
         }
     }
 
     @Test
     public void search_include_and_exclude_tags_params_no_results() {
         final RpslObject autnum = RpslObject.parse("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
         Map<RpslObject, RpslObjectUpdateInfo> updateInfos = databaseHelper.addObjects(Lists.newArrayList(autnum));
 
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "unref", "28");
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "foobar", "foobar");
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "other", "other stuff");
 
         try {
             RestTest.target(getPort(),
                     "whois/search?source=TEST&query-string=AS102&exclude-tag=foobar&include-tag=unref&include-tag=other")
                     .request(MediaType.APPLICATION_XML)
                     .get(WhoisResources.class);
             fail();
         } catch (NotFoundException ignored) {
             // expected
         }
     }
 
     @Test
     public void search_include_and_exclude_tags_params() {
         final RpslObject autnum = RpslObject.parse("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
         Map<RpslObject, RpslObjectUpdateInfo> updateInfos = databaseHelper.addObjects(Lists.newArrayList(autnum));
 
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "unref", "28");
         whoisTemplate.update("INSERT INTO tags VALUES (?, ?, ?)", updateInfos.get(autnum).getObjectId(), "foobar", "foobar");
 
         final WhoisResources whoisResources = RestTest.target(getPort(),
                 "whois/search?source=TEST&query-string=AS102&exclude-tag=other&include-tag=unref&include-tag=foobar")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
 
         final WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getLink(), is(new Link("locator", "http://rest-test.db.ripe.net/test/aut-num/AS102")));
         assertThat(whoisObject.getTags(), contains(
                 new WhoisTag("foobar", "foobar"),
                 new WhoisTag("unref", "28")));
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("aut-num", "AS102"),
                 new Attribute("as-name", "End-User-2"),
                 new Attribute("descr", "description"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("tech-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST")
         ));
     }
 
     @Test
     public void search_no_sources_given() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/search?query-string=TP1-TEST")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
     }
 
     @Test
     public void search_no_querystring_given() {
         try {
             RestTest.target(getPort(), "whois/search?source=TEST")
                     .request(MediaType.APPLICATION_XML)
                     .get(WhoisResources.class);
             fail();
         } catch (BadRequestException ignored) {
             assertOnlyErrorMessage(ignored, "Error", "Query param 'query-string' cannot be empty");
         }
     }
 
     @Test
     public void search_invalid_source() {
         try {
             RestTest.target(getPort(), "whois/search?query-string=AS102&source=INVALID")
                     .request(MediaType.APPLICATION_XML)
                     .get(WhoisResources.class);
             fail();
         } catch (BadRequestException e) {
             assertOnlyErrorMessage(e, "Error", "Invalid source '%s'", "INVALID");
             assertThat(e.getResponse().getHeaders().get("Content-Type"), contains((Object) "application/xml"));
         }
     }
 
     @Test
     public void search_multiple_sources() {
         try {
             RestTest.target(getPort(), "whois/search?query-string=TP1-TEST&source=TEST&source=RIPE")
                     .request(MediaType.APPLICATION_XML)
                     .get(WhoisResources.class);
             fail();
         } catch (BadRequestException e) {
             assertOnlyErrorMessage(e, "Error", "Invalid source '%s'", "RIPE");
         }
     }
 
     @Test
     public void search_with_type_filter() {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/search?query-string=AS102&source=TEST&type-filter=aut-num,as-block")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(2));
 
         WhoisObject aut_num = whoisResources.getWhoisObjects().get(0);
         WhoisObject person = whoisResources.getWhoisObjects().get(1);
 
         assertThat(person.getLink(), is(new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")));
         assertThat(aut_num.getLink(), is(new Link("locator", "http://rest-test.db.ripe.net/test/aut-num/AS102")));
         assertThat(aut_num.getAttributes(), contains(
                 new Attribute("aut-num", "AS102"),
                 new Attribute("as-name", "End-User-2"),
                 new Attribute("descr", "description"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("tech-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST")
         ));
     }
 
     @Test
     public void search_inverse() {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/search?query-string=TP1-TEST&source=TEST&inverse-attribute=admin-c,tech-c")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(4));
         WhoisObject aut_num = whoisResources.getWhoisObjects().get(0);
         assertThat(aut_num.getLink(), is(new Link("locator", "http://rest-test.db.ripe.net/test/aut-num/AS102")));
         assertThat(aut_num.getAttributes(), contains(
                 new Attribute("aut-num", "AS102"),
                 new Attribute("as-name", "End-User-2"),
                 new Attribute("descr", "description"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("tech-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST")
         ));
 
         WhoisObject person = whoisResources.getWhoisObjects().get(1);
         assertThat(person.getLink(), is(new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")));
         assertThat(person.getAttributes(), contains(
                 new Attribute("person", "Test Person"),
                 new Attribute("address", "Singel 258"),
                 new Attribute("phone", "+31 6 12345678"),
                 new Attribute("nic-hdl", "TP1-TEST"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST", "Filtered", null, null)
         ));
         WhoisObject mntner = whoisResources.getWhoisObjects().get(2);
         assertThat(mntner.getLink(), is(new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")));
         assertThat(mntner.getAttributes(), contains(
                 new Attribute("mntner", "OWNER-MNT"),
                 new Attribute("descr", "Owner Maintainer"),
                 new Attribute("admin-c", "TP1-TEST", null, "person", new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")),
                 new Attribute("auth", "MD5-PW", "Filtered", null, null),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("referral-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST", "Filtered", null, null)
         ));
 
         WhoisObject person2 = whoisResources.getWhoisObjects().get(3);
         assertThat(person2.getLink(), is(new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")));
         assertThat(person2.getAttributes(), contains(
                 new Attribute("person", "Test Person"),
                 new Attribute("address", "Singel 258"),
                 new Attribute("phone", "+31 6 12345678"),
                 new Attribute("nic-hdl", "TP1-TEST"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST", "Filtered", null, null)
         ));
     }
 
     @Ignore("TODO: error on parsing query is not handled -> response is plaintext error, not xml")
     @Test
     public void search_invalid_query_flags() {
         try {
             RestTest.target(getPort(), "whois/search?query-string=denis+walker&flags=resource")
                     .request(MediaType.APPLICATION_XML)
                     .get(String.class);
         } catch (BadRequestException e) {
             final WhoisResources response = e.getResponse().readEntity(WhoisResources.class);
             assertThat(response.getErrorMessages(), hasSize(1));
         }
     }
 
     @Test
     public void search_flags() {
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/search?query-string=TP1-TEST&source=TEST&flags=BrCx")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         WhoisObject whoisObject = whoisResources.getWhoisObjects().get(0);
         assertThat(whoisObject.getLink(), is(new Link("locator", "http://rest-test.db.ripe.net/test/person/TP1-TEST")));
         assertThat(whoisObject.getAttributes(), contains(
                 new Attribute("person", "Test Person"),
                 new Attribute("address", "Singel 258"),
                 new Attribute("phone", "+31 6 12345678"),
                 new Attribute("nic-hdl", "TP1-TEST"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("changed", "dbtest@ripe.net 20120101"),
                 new Attribute("source", "TEST")
         ));
     }
 
     @Test
     public void search_hierarchical_flags() {
         databaseHelper.addObject(
                 "inet6num:       2001:2002:2003::/48\n" +
                         "netname:        RIPE-NCC\n" +
                         "descr:          Private Network\n" +
                         "country:        NL\n" +
                         "tech-c:         TP1-TEST\n" +
                         "status:         ASSIGNED PA\n" +
                         "mnt-by:         OWNER-MNT\n" +
                         "mnt-lower:      OWNER-MNT\n" +
                         "source:         TEST");
         ipTreeUpdater.rebuild();
 
         WhoisResources whoisResources = RestTest.target(getPort(), "whois/search?query-string=2001:2002:2003:2004::5&flags=Lr")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
 
         whoisResources = RestTest.target(getPort(), "whois/search?query-string=2001:2002::/32&flags=M&flags=r")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
     }
 
     @Test
     public void search_invalid_flags() {
         try {
             RestTest.target(getPort(), "whois/search?query-string=TP1-TEST&source=TEST&flags=kq")
                     .request(MediaType.APPLICATION_XML)
                     .get(WhoisResources.class);
             fail();
         } catch (BadRequestException e) {
             assertOnlyErrorMessage(e, "Error", "Disallowed search flag '%s'", "persistent-connection");
         }
     }
 
     @Test
     public void search_grs() {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST-GRS\n");
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "whois/search?query-string=AS102&source=TEST-GRS")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         WhoisObject aut_num = whoisResources.getWhoisObjects().get(0);
         assertThat(aut_num.getLink(), is(new Link("locator", "http://rest-test.db.ripe.net/test-grs/aut-num/AS102")));
         assertThat(aut_num.getAttributes(), contains(
                 new Attribute("aut-num", "AS102"),
                 new Attribute("as-name", "End-User-2"),
                 new Attribute("descr", "description"),
                 new Attribute("admin-c", "DUMY-RIPE"),
                 new Attribute("tech-c", "DUMY-RIPE"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test-grs/mntner/OWNER-MNT")),
                 new Attribute("source", "TEST-GRS"),
                 new Attribute("remarks", "****************************"),
                 new Attribute("remarks", "* THIS OBJECT IS MODIFIED"),
                 new Attribute("remarks", "* Please note that all data that is generally regarded as personal"),
                 new Attribute("remarks", "* data has been removed from this object."),
                 new Attribute("remarks", "* To view the original object, please query the RIPE Database at:"),
                 new Attribute("remarks", "* http://www.ripe.net/whois"),
                 new Attribute("remarks", "****************************")
         ));
     }
 
     @Test
     public void search_parameters_are_returned() {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
 
         final WhoisResources whoisResources = RestTest.target(getPort(), "" +
                 "whois/search?inverse-attribute=person" +
                 "&type-filter=aut-num" +
                 "&source=test" +
                 "&flags=rB" +
                 "&query-string=TP1-TEST")
                 .request(MediaType.APPLICATION_XML)
                 .get(WhoisResources.class);
 
         assertThat(whoisResources.getErrorMessages(), is(empty()));
 
         final Parameters parameters = whoisResources.getParameters();
         final Flags flags = parameters.getFlags();
         assertThat(flags.getFlags().get(0).getValue(), is("no-referenced"));
         assertThat(flags.getFlags().get(1).getValue(), is("no-filtering"));
         final InverseAttributes inverseAttributes = parameters.getInverseLookup();
         assertThat(inverseAttributes.getInverseAttributes().get(0).getValue(), is("person"));
         final TypeFilters typeFilters = parameters.getTypeFilters();
         assertThat(typeFilters.getTypeFilters().get(0).getId(), is("aut-num"));
         final Sources sources = parameters.getSources();
         assertThat(sources.getSources().get(0).getId(), is("test"));
         final QueryStrings queryStrings = parameters.getQueryStrings();
         assertThat(queryStrings.getQueryStrings().get(0).getValue(), is("TP1-TEST"));
     }
 
     @Test
     public void search_not_found() {
         try {
             RestTest.target(getPort(), "whois/search?query-string=NONEXISTANT&source=TEST")
                     .request(MediaType.APPLICATION_XML)
                     .get(WhoisResources.class);
             fail();
         } catch (NotFoundException e) {
             assertThat(e.getResponse().readEntity(String.class), not(containsString("Caused by:")));
         }
     }
 
     @Test
     public void search_streaming_puts_xlink_into_root_element_and_nowhere_else() throws Exception {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
 
         final String whoisResources = RestTest.target(getPort(), "whois/search?query-string=AS102&source=TEST")
                 .request(MediaType.APPLICATION_XML)
                 .get(String.class);
 
         assertThat(whoisResources, containsString("<whois-resources xmlns:xlink=\"http://www.w3.org/1999/xlink\">"));
         assertThat(whoisResources, containsString("<object type=\"aut-num\">"));
         assertThat(whoisResources, containsString("<objects>"));
     }
 
     @Test
     public void search_non_streaming_puts_xlink_into_root_element_and_nowhere_else() throws Exception {
         final RpslObject autnum = RpslObject.parse("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
         databaseHelper.addObject(autnum);
 
         final String whoisResources = RestTest.target(getPort(), "whois/test/aut-num/AS102/versions/1")
                 .request(MediaType.APPLICATION_XML)
                 .get(String.class);
 
         assertThat(whoisResources, containsString("<whois-resources xmlns:xlink=\"http://www.w3.org/1999/xlink\">"));
         assertThat(whoisResources, containsString("<object type=\"aut-num\" version=\"1\">"));
         assertThat(whoisResources, containsString("<objects>"));
     }
 
     @Test
     public void search_multiple_objects_json_format() {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
 
         final String response = RestTest.target(getPort(), "whois/search?query-string=AS102&source=TEST")
                 .request(MediaType.APPLICATION_JSON_TYPE)
                 .get(String.class);
 
         assertThat(response, is(
                 "{\"service\":{\"name\":\"search\"}," +
                 "\"parameters\":{" +
                 "\"inverse-lookup\":{\"inverse-attribute\":[]}," +
                 "\"type-filters\":{\"type-filter\":[]}," +
                 "\"flags\":{\"flag\":[]}," +
                 "\"query-strings\":{\"query-string\":[{\"value\":\"AS102\"}]}," +
                 "\"sources\":{\"source\":[{\"id\":\"TEST\"}]}}," +
                 "\"objects\":{" +
                 "\"object\":[{" +
                 "\"type\":\"aut-num\"," +
                 "\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test/aut-num/AS102\"}," +
                 "\"source\":{\"id\":\"test\"}," +
                 "\"primary-key\":{\"attribute\":[{\"name\":\"aut-num\",\"value\":\"AS102\"}]}," +
                 "\"attributes\":{" +
                 "\"attribute\":[" +
                 "{\"name\":\"aut-num\",\"value\":\"AS102\"}," +
                 "{\"name\":\"as-name\",\"value\":\"End-User-2\"}," +
                 "{\"name\":\"descr\",\"value\":\"description\"}," +
                 "{\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test/person/TP1-TEST\"},\"name\":\"admin-c\",\"value\":\"TP1-TEST\",\"referenced-type\":\"person\"}," +
                 "{\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test/person/TP1-TEST\"},\"name\":\"tech-c\",\"value\":\"TP1-TEST\",\"referenced-type\":\"person\"}," +
                 "{\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\"},\"name\":\"mnt-by\",\"value\":\"OWNER-MNT\",\"referenced-type\":\"mntner\"}," +
                 "{\"name\":\"source\",\"value\":\"TEST\"}" +
                 "]}," +
                 "\"tags\":{\"tag\":[]}" +
                 "},{" +
                 "\"type\":\"person\"," +
                 "\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test/person/TP1-TEST\"}," +
                 "\"source\":{\"id\":\"test\"}," +
                 "\"primary-key\":{\"attribute\":[{\"name\":\"nic-hdl\",\"value\":\"TP1-TEST\"}]}," +
                 "\"attributes\":{" +
                 "\"attribute\":[" +
                 "{\"name\":\"person\",\"value\":\"Test Person\"}," +
                 "{\"name\":\"address\",\"value\":\"Singel 258\"}," +
                 "{\"name\":\"phone\",\"value\":\"+31 6 12345678\"}," +
                 "{\"name\":\"nic-hdl\",\"value\":\"TP1-TEST\"}," +
                 "{\"link\":{\"type\":\"locator\",\"href\":\"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\"},\"name\":\"mnt-by\",\"value\":\"OWNER-MNT\",\"referenced-type\":\"mntner\"}," +
                 "{\"name\":\"source\",\"value\":\"TEST\",\"comment\":\"Filtered\"}" +
                 "]}," +
                 "\"tags\":{\"tag\":[]}" +
                 "}]}," +
                 "\"terms-and-conditions\":{\"type\":\"locator\",\"href\":\"http://www.ripe.net/db/support/db-terms-conditions.pdf\"}" +
                 "}"));
     }
 
     @Test
     public void search_multiple_objects_xml_format() {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
 
         final String response = RestTest.target(getPort(), "whois/search?query-string=AS102&source=TEST")
                 .request(MediaType.APPLICATION_XML)
                 .get(String.class);
 
         assertThat(response, is(
                 "<?xml version='1.0' encoding='UTF-8'?>" +
                 "<whois-resources xmlns:xlink=\"http://www.w3.org/1999/xlink\">" +
                 "<service name=\"search\" />" +
                 "<parameters>" +
                 "<inverse-lookup />" +
                 "<type-filters />" +
                 "<flags />" +
                 "<query-strings>" +
                 "<query-string value=\"AS102\" />" +
                 "</query-strings>" +
                 "<sources>" +
                 "<source id=\"TEST\" />" +
                 "</sources>" +
                 "</parameters>" +
                 "<objects>" +
                 "<object type=\"aut-num\">" +
                 "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test/aut-num/AS102\" />" +
                 "<source id=\"test\" />" +
                 "<primary-key>" +
                 "<attribute name=\"aut-num\" value=\"AS102\" />" +
                 "</primary-key>" +
                 "<attributes>" +
                 "<attribute name=\"aut-num\" value=\"AS102\" />" +
                 "<attribute name=\"as-name\" value=\"End-User-2\" />" +
                 "<attribute name=\"descr\" value=\"description\" />" +
                 "<attribute name=\"admin-c\" value=\"TP1-TEST\" referenced-type=\"person\">" +
                 "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test/person/TP1-TEST\" />" +
                 "</attribute>" +
                 "<attribute name=\"tech-c\" value=\"TP1-TEST\" referenced-type=\"person\">" +
                 "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test/person/TP1-TEST\" />" +
                 "</attribute>" +
                 "<attribute name=\"mnt-by\" value=\"OWNER-MNT\" referenced-type=\"mntner\">" +
                 "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\" />" +
                 "</attribute>" +
                 "<attribute name=\"source\" value=\"TEST\" />" +
                 "</attributes>" +
                 "<tags />" +
                 "</object>" +
                 "<object type=\"person\">" +
                 "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test/person/TP1-TEST\" />" +
                 "<source id=\"test\" />" +
                 "<primary-key>" +
                 "<attribute name=\"nic-hdl\" value=\"TP1-TEST\" />" +
                 "</primary-key>" +
                 "<attributes>" +
                 "<attribute name=\"person\" value=\"Test Person\" />" +
                 "<attribute name=\"address\" value=\"Singel 258\" />" +
                 "<attribute name=\"phone\" value=\"+31 6 12345678\" />" +
                 "<attribute name=\"nic-hdl\" value=\"TP1-TEST\" />" +
                 "<attribute name=\"mnt-by\" value=\"OWNER-MNT\" referenced-type=\"mntner\">" +
                 "<link xlink:type=\"locator\" xlink:href=\"http://rest-test.db.ripe.net/test/mntner/OWNER-MNT\" />" +
                 "</attribute>" +
                 "<attribute name=\"source\" value=\"TEST\" comment=\"Filtered\" />" +
                 "</attributes>" +
                 "<tags />" +
                 "</object>" +
                 "</objects>" +
                 "<terms-and-conditions xlink:type=\"locator\" xlink:href=\"http://www.ripe.net/db/support/db-terms-conditions.pdf\" />" +
                 "</whois-resources>"));
     }
 
     @Test
     public void search_returns_object_locator() {
         databaseHelper.addObject("" +
                 "person:    Lo Person\n" +
                 "admin-c:   TP1-TEST\n" +
                 "tech-c:    TP1-TEST\n" +
                 "nic-hdl:   LP1-TEST\n" +
                 "mnt-by:    OWNER-MNT\n" +
                 "source:    TEST\n");
 
         final String response = RestTest.target(getPort(), "whois/search?query-string=LP1-TEST&source=TEST")
                 .request(MediaType.APPLICATION_XML)
                 .get(String.class);
 
         assertThat(response, stringMatchesRegexp(
                 ".*<object type=\\\"person\\\">.*" +
                 "<link xlink:type=\\\"locator\\\" xlink:href=\\\"http://rest-test.db.ripe.net/test/person/LP1-TEST\\\" />.*" +
                 "</object>.*"));
     }
 
     @Test
     public void search_not_contains_empty_xmlns() {
         databaseHelper.addObject(
                 "inet6num:       2001:2002:2003::/48\n" +
                         "netname:        RIPE-NCC\n" +
                         "descr:          Private Network\n" +
                         "country:        NL\n" +
                         "tech-c:         TP1-TEST\n" +
                         "status:         ASSIGNED PA\n" +
                         "mnt-by:         OWNER-MNT\n" +
                         "mnt-lower:      OWNER-MNT\n" +
                         "source:         TEST");
         ipTreeUpdater.rebuild();
 
         final String whoisResources = RestTest.target(getPort(), "whois/search?query-string=2001:2002:2003:2004::5")
                 .request(MediaType.APPLICATION_XML)
                 .get(String.class);
 
         assertThat(whoisResources, not(containsString("xmlns=\"\"")));
     }
 
     @Test
     public void xsi_attributes_not_in_root_level_link() {
         final String whoisResources = RestTest.target(getPort(), "whois/search?query-string=TP1-TEST&source=TEST")
                 .request(MediaType.APPLICATION_XML_TYPE).get(String.class);
         assertThat(whoisResources, not(containsString("xsi:type")));
         assertThat(whoisResources, not(containsString("xmlns:xsi")));
     }
 
     @Test
     public void non_ascii_characters_are_preserved() {
         assertThat(RestTest.target(getPort(), "whois/test/person?password=test")
                 .request(MediaType.APPLICATION_JSON)
                 .post(Entity.entity("{ \"objects\": { \"object\": [ {\n" +
                         "\"source\": { \"id\": \"RIPE\" },\n" +
                         "\"attributes\": {\n \"attribute\": [\n" +
                         "{ \"name\": \"person\", \"value\": \"Pauleth Palthen\" },\n" +
                         "{ \"name\": \"address\", \"value\": \"Flughafenstraße 109/a\" },\n" +
                         "{ \"name\": \"phone\", \"value\": \"+31-2-1234567\" },\n" +
                         "{ \"name\": \"e-mail\", \"value\": \"noreply@ripe.net\" },\n" +
                         "{ \"name\": \"mnt-by\", \"value\": \"OWNER-MNT\" },\n" +
                         "{ \"name\": \"nic-hdl\", \"value\": \"PP1-TEST\" },\n" +
                         "{ \"name\": \"changed\", \"value\": \"noreply@ripe.net\" },\n" +
                         "{ \"name\": \"remarks\", \"value\": \"created\" },\n" +
                         "{ \"name\": \"source\", \"value\": \"TEST\" }\n" +
                         "] } } ] } }", MediaType.APPLICATION_JSON), String.class), containsString("Flughafenstraße 109/a"));
 
         assertThat(RestTest.target(getPort(), "whois/test/person/PP1-TEST")
                 .request(MediaType.APPLICATION_JSON)
                 .get(String.class), containsString("Flughafenstraße 109/a"));
 
         assertThat(RestTest.target(getPort(), "whois/search?query-string=PP1-TEST&source=TEST")
                 .request(MediaType.APPLICATION_JSON)
                 .get(String.class), containsString("Flughafenstraße 109/a"));
 
         assertThat(RestTest.target(getPort(), "whois/test/person/PP1-TEST?password=test")
                 .request(MediaType.APPLICATION_JSON)
                 .put(Entity.entity(
                         "{ \"objects\": { \"object\": [ {\n" +
                                 "\"source\": { \"id\": \"RIPE\" },\n" +
                                 "\"attributes\": {\n \"attribute\": [\n" +
                                 "{ \"name\": \"person\", \"value\": \"Pauleth Palthen\" },\n" +
                                 "{ \"name\": \"address\", \"value\": \"Flughafenstraße 109/a\" },\n" +
                                 "{ \"name\": \"phone\", \"value\": \"+31-2-1234567\" },\n" +
                                 "{ \"name\": \"e-mail\", \"value\": \"noreply@ripe.net\" },\n" +
                                 "{ \"name\": \"mnt-by\", \"value\": \"OWNER-MNT\" },\n" +
                                 "{ \"name\": \"nic-hdl\", \"value\": \"PP1-TEST\" },\n" +
                                 "{ \"name\": \"changed\", \"value\": \"noreply@ripe.net\" },\n" +
                                 "{ \"name\": \"remarks\", \"value\": \"updated\" },\n" +
                                 "{ \"name\": \"source\", \"value\": \"TEST\" }\n" +
                                 "] } } ] } }", MediaType.APPLICATION_JSON), String.class), containsString("Flughafenstraße 109/a"));
     }
 
     @Test
     public void override_update_succeeds() {
         databaseHelper.addObject(PAULETH_PALTHEN);
         databaseHelper.insertUser(User.createWithPlainTextPassword("agoston", "zoh", ObjectType.PERSON));
 
         final RpslObject updatedObject = new RpslObjectBuilder(PAULETH_PALTHEN).addAttribute(new RpslAttribute(AttributeType.REMARKS, "updated")).sort().get();
 
         WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/person/PP1-TEST?override=agoston,zoh,reason")
                 .request(MediaType.APPLICATION_XML)
                 .put(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(updatedObject)), MediaType.APPLICATION_XML), WhoisResources.class);
 
         assertErrorMessage(whoisResources, 0, "Info", "Authorisation override used");
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
         final WhoisObject object = whoisResources.getWhoisObjects().get(0);
         assertThat(object.getAttributes(), contains(
                 new Attribute("person", "Pauleth Palthen"),
                 new Attribute("address", "Singel 258"),
                 new Attribute("phone", "+31-1234567890"),
                 new Attribute("e-mail", "noreply@ripe.net"),
                 new Attribute("nic-hdl", "PP1-TEST"),
                 new Attribute("remarks", "remark"),
                 new Attribute("remarks", "updated"),
                 new Attribute("mnt-by", "OWNER-MNT", null, "mntner", new Link("locator", "http://rest-test.db.ripe.net/test/mntner/OWNER-MNT")),
                 new Attribute("changed", "noreply@ripe.net 20120101"),
                 new Attribute("source", "TEST")));
 
         assertThat(whoisResources.getTermsAndConditions().getHref(), is(WhoisResources.TERMS_AND_CONDITIONS));
     }
 
     @Test
     public void error_message_not_included_in_success() throws Exception {
         final String response = RestTest.target(getPort(), "whois/test/person?password=test")
                 .request()
                 .post(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_XML))
                 .readEntity(String.class);
 
         assertThat(response, not(containsString("errormessage")));
     }
 
     @Test
     public void error_message_not_included_in_search() throws Exception {
         databaseHelper.addObject("" +
                 "aut-num:        AS102\n" +
                 "as-name:        End-User-2\n" +
                 "descr:          description\n" +
                 "admin-c:        TP1-TEST\n" +
                 "tech-c:         TP1-TEST\n" +
                 "mnt-by:         OWNER-MNT\n" +
                 "source:         TEST\n");
 
         final String response = RestTest.target(getPort(), "whois/search?query-string=AS102&source=TEST")
                 .request(MediaType.APPLICATION_XML)
                 .get(String.class);
 
         assertThat(response, not(containsString("errormessage")));
     }
 
     // maintenance mode
 
     // TODO: [AH] also test origin, i.e. maintenanceMode.set("NONE,READONLY")
 
     @Test(expected = ServiceUnavailableException.class)
     public void maintenance_mode_readonly_update() {
         maintenanceMode.set("READONLY,READONLY");
         RestTest.target(getPort(), "whois/test/person/PP1-TEST")
                 .request(MediaType.APPLICATION_XML)
                 .put(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_XML), String.class);
     }
 
     @Test
     public void maintenance_mode_readonly_query() {
         maintenanceMode.set("READONLY,READONLY");
         WhoisResources whoisResources = RestTest.target(getPort(), "whois/test/person/TP1-TEST").request().get(WhoisResources.class);
         assertThat(whoisResources.getWhoisObjects(), hasSize(1));
     }
 
     @Test(expected = ServiceUnavailableException.class)
     public void maintenance_mode_none_update() {
         maintenanceMode.set("NONE,NONE");
         RestTest.target(getPort(), "whois/test/person/PP1-TEST")
                 .request(MediaType.APPLICATION_XML)
                 .put(Entity.entity(whoisObjectMapper.mapRpslObjects(Arrays.asList(PAULETH_PALTHEN)), MediaType.APPLICATION_XML), String.class);
     }
 
     @Test(expected = ServiceUnavailableException.class)
     public void maintenance_mode_none_query() {
         maintenanceMode.set("NONE,NONE");
         RestTest.target(getPort(), "whois/test/person/TP1-TEST").request().get(WhoisResources.class);
     }
 }
