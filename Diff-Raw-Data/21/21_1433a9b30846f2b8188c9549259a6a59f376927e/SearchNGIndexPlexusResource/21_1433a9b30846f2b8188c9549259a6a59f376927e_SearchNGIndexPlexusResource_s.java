 /**
  * Sonatype Nexus (TM) Open Source Version.
  * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
  * Includes the third-party code listed at http://nexus.sonatype.org/dev/attributions.html
  * This program is licensed to you under Version 3 only of the GNU General Public License as published by the Free Software Foundation.
  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU General Public License Version 3 for more details.
  * You should have received a copy of the GNU General Public License Version 3 along with this program.
  * If not, see http://www.gnu.org/licenses/.
  * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc.
  * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
  */
 package org.sonatype.nexus.rest.index;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 
 import javax.ws.rs.GET;
 import javax.ws.rs.Path;
 import javax.ws.rs.QueryParam;
 
 import org.apache.lucene.store.AlreadyClosedException;
 import org.apache.maven.artifact.versioning.ArtifactVersion;
 import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
 import org.codehaus.plexus.component.annotations.Component;
 import org.codehaus.plexus.component.annotations.Requirement;
 import org.restlet.Context;
 import org.restlet.data.Form;
 import org.restlet.data.Parameter;
 import org.restlet.data.Request;
 import org.restlet.data.Response;
 import org.restlet.data.Status;
 import org.restlet.resource.ResourceException;
 import org.restlet.resource.Variant;
 import org.sonatype.nexus.artifact.Gav;
 import org.sonatype.nexus.artifact.GavCalculator;
 import org.sonatype.nexus.artifact.IllegalArtifactCoordinateException;
 import org.sonatype.nexus.artifact.VersionUtils;
 import org.sonatype.nexus.index.ArtifactInfo;
 import org.sonatype.nexus.index.ArtifactInfoFilter;
 import org.sonatype.nexus.index.IteratorSearchResponse;
 import org.sonatype.nexus.index.KeywordSearcher;
 import org.sonatype.nexus.index.MAVEN;
 import org.sonatype.nexus.index.MavenCoordinatesSearcher;
 import org.sonatype.nexus.index.SearchType;
 import org.sonatype.nexus.index.Searcher;
 import org.sonatype.nexus.index.UniqueArtifactFilterPostprocessor;
 import org.sonatype.nexus.index.context.IndexingContext;
 import org.sonatype.nexus.proxy.NoSuchRepositoryException;
 import org.sonatype.nexus.proxy.repository.GroupRepository;
 import org.sonatype.nexus.proxy.repository.HostedRepository;
 import org.sonatype.nexus.proxy.repository.ProxyRepository;
 import org.sonatype.nexus.proxy.repository.Repository;
 import org.sonatype.nexus.proxy.repository.RepositoryKind;
 import org.sonatype.nexus.proxy.repository.ShadowRepository;
 import org.sonatype.nexus.rest.AbstractIndexerNexusPlexusResource;
 import org.sonatype.nexus.rest.model.NexusNGArtifact;
 import org.sonatype.nexus.rest.model.NexusNGArtifactHit;
 import org.sonatype.nexus.rest.model.NexusNGArtifactLink;
 import org.sonatype.nexus.rest.model.SearchNGResponse;
 import org.sonatype.nexus.rest.model.SearchResponse;
 import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
 import org.sonatype.plexus.rest.resource.PlexusResource;
 
 @Component( role = PlexusResource.class, hint = "SearchNGIndexPlexusResource" )
 @Path( SearchNGIndexPlexusResource.RESOURCE_URI )
 public class SearchNGIndexPlexusResource
     extends AbstractIndexerNexusPlexusResource
 {
     /**
      * Hard upper limit of the count of search hits delivered over REST API.
      */
     private static final int HIT_LIMIT = 500;
 
     /**
      * The treshold, that is used to "uncollapse" the collapsed results (if less hits than threshold).
      */
     private static final int COLLAPSE_OVERRIDE_TRESHOLD = 35;
 
     public static final String RESOURCE_URI = "/data_index1";
 
     @Requirement( role = Searcher.class )
     private List<Searcher> searchers;
 
     @Requirement( hint = "maven2" )
     private GavCalculator m2GavCalculator;
 
     @Override
     public String getResourceUri()
     {
         return RESOURCE_URI;
     }
 
     @Override
     public PathProtectionDescriptor getResourceProtection()
     {
         return new PathProtectionDescriptor( getResourceUri(), "authcBasic,perms[nexus:index]" );
     }
 
     @Override
     public Object getPayloadInstance()
     {
         return null;
     }
 
     /**
      * Search against all repositories using provided parameters. Note there are a few different types of searches you
      * can perform. If you provide the 'q' query parameter, a keyword search will be performed. If you provide the 'g,
      * a, v, p or c' query parameters, a maven coordinate search will be performed. If you provide the 'cn' query
      * parameter, a classname search will be performed. If you provide the 'sha1' query parameter, a checksum search
      * will be performed.
      * 
      * @param q provide this param for a keyword search (g, a, v, p, c, cn, sha1 params will be ignored).
      * @param sha1 provide this param for a checksum search (g, a, v, p, c, cn params will be ignored).
      * @param cn provide this param for a classname search (g, a, v, p, c params will be ignored).
      * @param g group id to perform a maven search against (can be combined with a, v, p & c params as well).
      * @param a artifact id to perform a maven search against (can be combined with g, v, p & c params as well).
      * @param v version to perform a maven search against (can be combined with g, a, p & c params as well).
      * @param p packaging type to perform a maven search against (can be combined with g, a, v & c params as well).
      * @param c classifier to perform a maven search against (can be combined with g, a, v & p params as well).
      * @param from result index to start retrieving results from.
      * @param count number of results to have returned to you.
      */
     @Override
     @GET
     @ResourceMethodSignature( queryParams = { @QueryParam( "q" ), @QueryParam( "g" ), @QueryParam( "a" ),
         @QueryParam( "v" ), @QueryParam( "p" ), @QueryParam( "c" ), @QueryParam( "cn" ), @QueryParam( "sha1" ),
         @QueryParam( "from" ), @QueryParam( "count" ) }, output = SearchResponse.class )
     public Object get( Context context, Request request, Response response, Variant variant )
         throws ResourceException
     {
         Form form = request.getResourceRef().getQueryAsForm();
 
         final Map<String, String> terms = new HashMap<String, String>();
 
         for ( Parameter parameter : form )
         {
             terms.put( parameter.getName(), parameter.getValue() );
         }
 
         Integer from = null;
         Integer count = null;
         Boolean exact = null;
        Boolean expandVersion = null;
         Boolean collapseResults = Boolean.FALSE;
 
         if ( form.getFirstValue( "from" ) != null )
         {
             try
             {
                 from = Integer.valueOf( form.getFirstValue( "from" ) );
             }
             catch ( NumberFormatException e )
             {
                 from = null;
             }
         }
 
         if ( form.getFirstValue( "count" ) != null )
         {
             try
             {
                 count = Integer.valueOf( form.getFirstValue( "count" ) );
             }
             catch ( NumberFormatException e )
             {
                 count = null;
             }
         }
 
         if ( form.getFirstValue( "exact" ) != null )
         {
             exact = Boolean.valueOf( form.getFirstValue( "exact" ) );
         }
 
         if ( form.getFirstValue( "versionexpand" ) != null )
         {
             expandVersion = Boolean.valueOf( form.getFirstValue( "versionexpand" ) );
         }
         if ( form.getFirstValue( "collapseresults" ) != null )
         {
             collapseResults = Boolean.valueOf( form.getFirstValue( "collapseresults" ) );
         }
 
         IteratorSearchResponse searchResult = null;
 
         SearchNGResponse result = new SearchNGResponse();
 
         // doing "plain search" 3 times in case of AlreadyClosedExce
         final int RETRIES = 3;
 
         int runCount = 0;
 
         while ( runCount < RETRIES )
         {
             try
             {
                 List<ArtifactInfoFilter> filters = new ArrayList<ArtifactInfoFilter>();
 
                 // we need to save this reference to later
                 LatestVersionCollector collector = new LatestVersionCollector();
 
                 filters.add( collector );
 
                 searchResult = searchByTerms( terms, null, from, count, exact, expandVersion, collapseResults, filters );
 
                 if ( searchResult == null )
                 {
                     collapseResults = false;
 
                     continue;
                 }
                 else
                 {
                     repackIteratorSearchResponse( request, result, collapseResults, from, count, searchResult,
                         collector );
 
                     if ( !result.isTooManyResults() )
                     {
                         // if we had collapseResults ON, and the totalHits are larger than actual (filtered) results,
                         // and
                         // the actual result count is below COLLAPSE_OVERRIDE_TRESHOLD,
                         // and full result set is smaller than HIT_LIMIT
                         // then repeat without collapse
                         if ( collapseResults && result.getData().size() < searchResult.getTotalHits()
                             && result.getData().size() < COLLAPSE_OVERRIDE_TRESHOLD
                             && searchResult.getTotalHits() < HIT_LIMIT )
                         {
                             collapseResults = false;
 
                             continue;
                         }
                     }
                 }
 
                 // we came here, so we break the while-loop, we got what we need
                 break;
             }
             catch ( NoSuchRepositoryException e )
             {
                 throw new ResourceException( Status.CLIENT_ERROR_BAD_REQUEST,
                     "Repository to be searched does not exists!", e );
             }
             catch ( AlreadyClosedException e )
             {
                 runCount++;
 
                 getLogger().info(
                     "*** NexusIndexer bug, we got AlreadyClosedException that should never happen with ReadOnly IndexReaders! Please put Nexus into DEBUG log mode and report this issue together with the stack trace!" );
 
                 if ( getLogger().isDebugEnabled() )
                 {
                     // just keep it silent (DEBUG)
                     getLogger().debug( "Got AlreadyClosedException exception!", e );
                 }
 
                 result.setData( null );
             }
         }
 
         if ( result.getData() == null )
         {
             try
             {
                 repackIteratorSearchResponse( request, result, collapseResults, from, count,
                     IteratorSearchResponse.TOO_MANY_HITS_ITERATOR_SEARCH_RESPONSE, null );
             }
             catch ( NoSuchRepositoryException e )
             {
                 // will not happen
             }
 
             getLogger().info(
                 "Nexus BUG: Was unable to perform search " + RETRIES
                     + " times, giving up, and lying about TooManyResults." );
         }
 
         return result;
     }
 
     private IteratorSearchResponse searchByTerms( final Map<String, String> terms, final String repositoryId,
                                                   final Integer from, final Integer count, final Boolean exact,
                                                   final Boolean expandVersion, final Boolean collapseResults,
                                                   List<ArtifactInfoFilter> filters )
         throws NoSuchRepositoryException, ResourceException
     {
         for ( Searcher searcher : searchers )
         {
             if ( searcher.canHandle( terms ) )
             {
                 SearchType searchType = searcher.getDefaultSearchType();
 
                 if ( exact != null )
                 {
                     if ( exact )
                     {
                         searchType = SearchType.EXACT;
                     }
                     else
                     {
                         searchType = SearchType.SCORED;
                     }
                 }
 
                 // copy the list, to be able to modify it but do not interleave with potential recursive calls
                 List<ArtifactInfoFilter> actualFilters = new ArrayList<ArtifactInfoFilter>( filters );
 
                 if ( collapseResults )
                 {
                     // filters should affect only Keyword and GAVSearch!
                     // TODO: maybe we should left this to the given Searcher implementation to handle (like kw and gav
                     // searcer is)
                     // Downside would be that REST query params would be too far away from incoming call (too spread)
                     if ( searcher instanceof KeywordSearcher || searcher instanceof MavenCoordinatesSearcher )
                     {
                         UniqueArtifactFilterPostprocessor filter = new UniqueArtifactFilterPostprocessor();
 
                         filter.addField( MAVEN.GROUP_ID );
                         filter.addField( MAVEN.ARTIFACT_ID );
                         filter.addField( MAVEN.PACKAGING );
                         filter.addField( MAVEN.CLASSIFIER );
                         filter.addField( MAVEN.REPOSITORY_ID );
 
                         if ( Boolean.TRUE.equals( expandVersion ) )
                         {
                             filter.addField( MAVEN.VERSION );
                         }
 
                         actualFilters.add( filter );
                     }
                 }
 
                 final IteratorSearchResponse searchResponse =
                     searcher.flatIteratorSearch( terms, repositoryId, from, count, HIT_LIMIT, false, searchType,
                         actualFilters );
 
                 if ( searchResponse != null )
                 {
                     if ( searchResponse.isHitLimitExceeded() )
                     {
                         return IteratorSearchResponse.TOO_MANY_HITS_ITERATOR_SEARCH_RESPONSE;
                     }
                     else if ( collapseResults && searchResponse.getTotalHits() < COLLAPSE_OVERRIDE_TRESHOLD )
                     {
                         // FIXME: fix this, this is ugly
                         // We are returning null, to hint that we need UNCOLLAPSED search!
                         // Needed, to be able to "signal" the fact that we are overriding collapsed switch
                         // since we have to send it back in DTOs to REST client
                         return null;
 
                         // old code was a recursive call:
                         // this was a "collapsed" search (probably initiated by UI), and we have less then treshold hits
                         // override collapse
                         // return searchByTerms( terms, repositoryId, from, count, exact, expandVersion, false, filters
                         // );
                     }
                     else
                     {
                         return searchResponse;
                     }
                 }
             }
         }
 
         throw new ResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "Requested search query is not supported" );
     }
 
     protected void repackIteratorSearchResponse( Request request, SearchNGResponse response, boolean collapsed,
                                                  Integer from, Integer count, IteratorSearchResponse iterator,
                                                  LatestVersionCollector collector )
         throws NoSuchRepositoryException
     {
         response.setTooManyResults( iterator.isHitLimitExceeded() );
 
         response.setCollapsed( collapsed );
 
         response.setTotalCount( iterator.getTotalHits() );
 
         response.setFrom( from == null ? -1 : from.intValue() );
 
         response.setCount( count == null ? -1 : count );
 
         if ( !response.isTooManyResults() )
         {
             // 1st pass, collect results
             LinkedHashMap<String, NexusNGArtifact> hits = new LinkedHashMap<String, NexusNGArtifact>();
 
             NexusNGArtifact artifact;
 
             for ( ArtifactInfo ai : iterator )
             {
                 final String key = ai.groupId + ":" + ai.artifactId + ":" + ai.version;
 
                 artifact = hits.get( key );
 
                 if ( artifact == null )
                 {
                     artifact = new NexusNGArtifact();
 
                     artifact.setGroupId( ai.groupId );
 
                     artifact.setArtifactId( ai.artifactId );
 
                     artifact.setVersion( ai.version );
 
                     artifact.setHighlightedFragment( getMatchHighlightHtmlSnippet( ai ) );
 
                     hits.put( key, artifact );
                 }
 
                 Repository repository = getRepositoryRegistry().getRepository( ai.repository );
 
                 NexusNGArtifactHit hit = null;
 
                 for ( NexusNGArtifactHit artifactHit : artifact.getHits() )
                 {
                     if ( repository.getId().equals( artifactHit.getRepositoryId() ) )
                     {
                         hit = artifactHit;
 
                         break;
                     }
                 }
 
                 if ( hit == null )
                 {
                     hit = new NexusNGArtifactHit();
 
                     hit.setRepositoryId( repository.getId() );
 
                     hit.setRepositoryName( repository.getName() );
 
                     hit.setRepositoryURL( createRepositoryReference( request, repository.getId() ).getTargetRef().toString() );
 
                     hit.setRepositoryContentClass( repository.getRepositoryContentClass().getId() );
 
                     hit.setRepositoryKind( extractRepositoryKind( repository ) );
 
                     // we are adding the POM link "blindly", unless packaging is POM,
                     // since the it will be added below the "usual" way
                     if ( !"pom".equals( ai.packaging ) )
                     {
                         NexusNGArtifactLink link = new NexusNGArtifactLink();
 
                         link.setClassifier( null );
 
                         link.setExtension( "pom" );
 
                         try
                         {
                             Gav gav = ai.calculateGav();
 
                             String path = m2GavCalculator.gavToPath( gav );
 
                             path = path.replace( "." + gav.getExtension(), ".pom" );
 
                             link.setArtifactLink( createRepositoryReference( request, repository.getId(), path ).getTargetRef().toString() );
                         }
                         catch ( IllegalArtifactCoordinateException e )
                         {
                             // hum hum, nothig here then
                         }
 
                         // add the POM link
                         hit.addArtifactLink( link );
                     }
 
                     // we just created it, add it
                     artifact.addHit( hit );
                 }
 
                 NexusNGArtifactLink link = new NexusNGArtifactLink();
 
                 link.setClassifier( ai.classifier );
 
                 link.setExtension( ai.fextension );
 
                 try
                 {
                     String path = m2GavCalculator.gavToPath( ai.calculateGav() );
 
                     link.setArtifactLink( createRepositoryReference( request, repository.getId(), path ).getTargetRef().toString() );
                 }
                 catch ( IllegalArtifactCoordinateException e )
                 {
                     // hum hum, nothig here then
                 }
 
                 // only if unique
                 hit.addArtifactLink( link );
             }
 
             // 2nd pass, set versions
             for ( NexusNGArtifact artifactNg : hits.values() )
             {
                 LatestVersionHolder holder =
                     collector.getLvhs().get( collector.getKey( artifactNg.getGroupId(), artifactNg.getArtifactId() ) );
 
                 if ( holder != null )
                 {
                     if ( holder.getLatest() != null )
                     {
                         artifactNg.setLatestVersion( holder.getLatest().toString() );
                     }
 
                     if ( holder.getRelease() != null )
                     {
                         artifactNg.setReleasedVersion( holder.getRelease().toString() );
                     }
 
                     if ( collapsed )
                     {
                         String versionToSet = holder.getRelease().toString();
 
                         String token = artifactNg.getVersion();
 
                         artifactNg.setVersion( versionToSet );
 
                         for ( NexusNGArtifactHit hit : artifactNg.getHits() )
                         {
                             for ( NexusNGArtifactLink link : hit.getArtifactLinks() )
                             {
                                 link.setArtifactLink( link.getArtifactLink().replaceAll( token, versionToSet ) );
                             }
                         }
                     }
                 }
             }
 
             response.setData( new ArrayList<NexusNGArtifact>( hits.values() ) );
         }
     }
 
     protected String extractRepositoryKind( Repository repository )
     {
         RepositoryKind kind = repository.getRepositoryKind();
 
         if ( kind.isFacetAvailable( HostedRepository.class ) )
         {
             return "hosted";
         }
         else if ( kind.isFacetAvailable( ProxyRepository.class ) )
         {
             return "proxy";
         }
         else if ( kind.isFacetAvailable( GroupRepository.class ) )
         {
             return "group";
         }
         else if ( kind.isFacetAvailable( ShadowRepository.class ) )
         {
             return "virtual";
         }
         else
         {
             // huh?
             return repository.getRepositoryKind().getMainFacet().getName();
         }
     }
 
     // ==
 
     protected static class LatestVersionHolder
     {
         private final String groupId;
 
         private final String artifactId;
 
         private ArtifactVersion latest;
 
         private ArtifactVersion release;
 
         public LatestVersionHolder( final ArtifactInfo ai )
         {
             this.groupId = ai.groupId;
 
             this.artifactId = ai.artifactId;
 
             maintainLatestVersions( ai );
         }
 
         @SuppressWarnings( "unchecked" )
         public void maintainLatestVersions( final ArtifactInfo ai )
         {
             if ( this.latest == null )
             {
                 this.latest = ai.getArtifactVersion();
             }
             else if ( this.latest.compareTo( ai.getArtifactVersion() ) < 0 )
             {
                 this.latest = ai.getArtifactVersion();
             }
 
             if ( !VersionUtils.isSnapshot( ai.version ) )
             {
                 if ( this.release == null )
                 {
                     this.release = ai.getArtifactVersion();
                 }
                 else if ( this.release.compareTo( ai.getArtifactVersion() ) < 0 )
                 {
                     this.release = ai.getArtifactVersion();
                 }
             }
         }
 
         // ==
 
         public String getGroupId()
         {
             return groupId;
         }
 
         public String getArtifactId()
         {
             return artifactId;
         }
 
         public ArtifactVersion getLatest()
         {
             return latest;
         }
 
         public ArtifactVersion getRelease()
         {
             return release;
         }
 
         @Override
         public int hashCode()
         {
             final int prime = 31;
             int result = 1;
             result = prime * result + ( ( artifactId == null ) ? 0 : artifactId.hashCode() );
             result = prime * result + ( ( groupId == null ) ? 0 : groupId.hashCode() );
             return result;
         }
 
         @Override
         public boolean equals( Object obj )
         {
             if ( this == obj )
                 return true;
             if ( obj == null )
                 return false;
             if ( getClass() != obj.getClass() )
                 return false;
             LatestVersionHolder other = (LatestVersionHolder) obj;
             if ( artifactId == null )
             {
                 if ( other.artifactId != null )
                     return false;
             }
             else if ( !artifactId.equals( other.artifactId ) )
                 return false;
             if ( groupId == null )
             {
                 if ( other.groupId != null )
                     return false;
             }
             else if ( !groupId.equals( other.groupId ) )
                 return false;
             return true;
         }
     }
 
     /**
      * A special filter that actually does not filter, but collects the latest and release version for every GA. After
      * iteratorSearchResponse has been processed, this collector will hold all the needed versions of the processed
      * artifact infos.
      * 
      * @author cstamas
      */
     protected static class LatestVersionCollector
         implements ArtifactInfoFilter
     {
         private HashMap<String, LatestVersionHolder> lvhs = new HashMap<String, LatestVersionHolder>();
 
         public boolean accepts( IndexingContext ctx, ArtifactInfo ai )
         {
             final String key = getKey( ai.groupId, ai.artifactId );
 
             LatestVersionHolder lvh = lvhs.get( key );
 
             if ( lvh == null )
             {
                 lvh = new LatestVersionHolder( ai );
 
                 lvhs.put( key, lvh );
             }
 
             lvh.maintainLatestVersions( ai );
 
             return true;
         }
 
         public String getKey( String groupId, String artifactId )
         {
             return groupId + ":" + artifactId;
         }
 
         public HashMap<String, LatestVersionHolder> getLvhs()
         {
             return lvhs;
         }
     }
 }
