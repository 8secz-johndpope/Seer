 /*
  * See the NOTICE file distributed with this work for additional
  * information regarding copyright ownership.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 package org.phenotips.ontology.internal.solr;
 
 import org.phenotips.ontology.OntologyService;
 import org.phenotips.ontology.SolrOntologyServiceInitializer;
 import org.phenotips.ontology.OntologyTerm;
 
 import org.xwiki.cache.Cache;
 import org.xwiki.cache.CacheException;
 import org.xwiki.component.manager.ComponentLookupException;
 import org.xwiki.test.mockito.MockitoComponentMockingRule;
 
 import java.io.IOException;
 
 import org.apache.solr.client.solrj.SolrQuery;
 import org.apache.solr.client.solrj.SolrServer;
 import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.common.SolrDocument;
 import org.apache.solr.common.SolrDocumentList;
 import org.junit.Assert;
 import org.junit.Before;
 import org.junit.Rule;
 import org.junit.Test;
 import org.mockito.Mockito;
 
 import static org.mockito.Matchers.any;
 import static org.mockito.Mockito.mock;
 import static org.mockito.Mockito.when;
 /**
  * Tests for the HPO implementation of the {@link org.phenotips.ontology.OntologyService}, {@link
  * org.phenotips.ontology.internal.solr.HumanPhenotypeOntology}.
  */
 public class HumanPhenotypeOntologyTest
 {
     public int ontologyServiceResult;
 
     public Cache<OntologyTerm> cache;
 
     public SolrServer server;
 
     public OntologyService ontologyService;
 
     @Rule
     public final MockitoComponentMockingRule<OntologyService> mocker =
         new MockitoComponentMockingRule<OntologyService>(HumanPhenotypeOntology.class);
 
     @Before
     public void setUpOntology()
         throws ComponentLookupException, IOException, SolrServerException, CacheException
     {
         cache = mock(Cache.class);
         SolrOntologyServiceInitializer externalServicesAccess =
             mocker.getInstance(SolrOntologyServiceInitializer.class);
         when(externalServicesAccess.getCache()).thenReturn(cache);
         server = mock(SolrServer.class);
         when(externalServicesAccess.getServer()).thenReturn(server);
         ontologyService = mocker.getComponentUnderTest();
         ontologyServiceResult = ontologyService.reindex(null);
     }
 
     @Test
     public void testHumanPhenotypeOntologyReindex()
         throws ComponentLookupException, IOException, SolrServerException, CacheException
     {
         Mockito.verify(server).deleteByQuery("*:*");
         Mockito.verify(server).commit();
         Mockito.verify(server).add(Mockito.anyCollection());
         Mockito.verify(cache).removeAll();
         Mockito.verifyNoMoreInteractions(cache, server);
         Assert.assertTrue(ontologyServiceResult == 0);
     }
 
     @Test
     public void testHumanPhenotypeOntologyVersion() throws SolrServerException
     {
         QueryResponse response = mock(QueryResponse.class);
         when(server.query(any(SolrQuery.class))).thenReturn(response);
         SolrDocumentList results = mock(SolrDocumentList.class);
         when(response.getResults()).thenReturn(results);
         when(results.isEmpty()).thenReturn(false);
         SolrDocument versionDoc = mock(SolrDocument.class);
         when(results.get(0)).thenReturn(versionDoc);
         when(versionDoc.getFieldValue("version")).thenReturn("2014:01:01");
         Assert.assertEquals("2014:01:01", ontologyService.getVersion());
     }
 
     @Test
     public void testHumanPhenotypeOntologyDefaultLocation()
     {
         String location = ontologyService.getDefaultOntologyLocation();
        Assert.assertNotNull(location);
        Assert.assertTrue(location.endsWith("hp.obo"));
        Assert.assertTrue(location.startsWith("http"));
     }
 }
