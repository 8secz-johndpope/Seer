 /*******************************************************************************
  * Copyright (c) 2007-2008 Sonatype Inc
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Eugene Kuleshov (Sonatype)
  *    Tam�s Cserven�k (Sonatype)
  *    Brian Fox (Sonatype)
  *    Jason Van Zyl (Sonatype)
  *******************************************************************************/
 package org.sonatype.nexus.index;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.apache.lucene.index.Term;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.RAMDirectory;
 
 /** 
  * @author Eugene Kuleshov
  */
 public class Index20081108RegressionTest
     extends AbstractRepoNexusIndexerTest
 {
     @Override
     protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
         throws Exception
     {
         context = nexusIndexer.addIndexingContext(
             "test",
             "test",
             null,
             indexDir,
             null,
             null,
             NexusIndexer.DEFAULT_INDEX );
         
        InputStream is = new FileInputStream("src" + File.separator + "test" + File.separator  + "nexus-maven-repository-index.20081108.zip" );
         Directory archiveDir = new RAMDirectory();
         
         IndexUtils.unpackIndexArchive( is, archiveDir );
         
         context.replace( archiveDir );
     }
     
     public void testExtension() throws Exception 
     {
         assertEquals( 31, context.getIndexReader().numDocs() );
         
         {
             Query q = nexusIndexer.constructQuery( ArtifactInfo.GROUP_ID, "qdox" );
             FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );
             assertEquals( response.getResults().toString(), 2, response.getTotalHits() );
   
             List<ArtifactInfo> list = new ArrayList<ArtifactInfo>( response.getResults() );
             assertEquals( 2, list.size() );
   
             {
                 ArtifactInfo ai = list.get( 0 );
                 assertEquals( "1.6.1", ai.version );
                 assertEquals( "jar", ai.fextension );
                 assertEquals( "jar", ai.packaging );
             }
             {
                 ArtifactInfo ai = list.get( 1 );
                 assertEquals( "1.5", ai.version );
                 assertEquals( "jar", ai.fextension );
                 assertEquals( "jar", ai.packaging );
             }
         }
         {
           Query query = new TermQuery( new Term( ArtifactInfo.PACKAGING, "tar.gz" ) );
           FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( query ) );
           assertEquals( response.getResults().toString(), 1, response.getTotalHits() );
           
           ArtifactInfo ai = response.getResults().iterator().next();
           assertEquals( "tar.gz", ai.packaging );
           assertEquals( "tar.gz", ai.fextension );
         }
         {
             Query query = new TermQuery( new Term( ArtifactInfo.PACKAGING, "zip" ) );
             FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( query ) );
             assertEquals( response.getResults().toString(), 1, response.getTotalHits() );
             
             ArtifactInfo ai = response.getResults().iterator().next();
             assertEquals( "zip", ai.packaging );
             assertEquals( "zip", ai.fextension );
         }
     }
 }
