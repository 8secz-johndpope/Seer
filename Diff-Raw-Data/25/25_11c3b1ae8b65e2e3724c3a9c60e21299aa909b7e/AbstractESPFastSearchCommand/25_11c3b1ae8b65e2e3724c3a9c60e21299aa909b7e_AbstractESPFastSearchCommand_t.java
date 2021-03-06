 /* Copyright (2005-2008) Schibsted Søk AS
  * This file is part of SESAT.
  *
  *   SESAT is free software: you can redistribute it and/or modify
  *   it under the terms of the GNU Affero General Public License as published by
  *   the Free Software Foundation, either version 3 of the License, or
  *   (at your option) any later version.
  *
  *   SESAT is distributed in the hope that it will be useful,
  *   but WITHOUT ANY WARRANTY; without even the implied warranty of
  *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *   GNU Affero General Public License for more details.
  *
  *   You should have received a copy of the GNU Affero General Public License
  *   along with SESAT.  If not, see <http://www.gnu.org/licenses/>.
  *
  * AbstractESPFastSearchCommand.java
  *
  * Created on 14 March 2006, 19:51
  *
  */
 
 package no.sesat.search.mode.command;
 
 import com.fastsearch.esp.search.ConfigurationException;
 import com.fastsearch.esp.search.SearchEngineException;
 import com.fastsearch.esp.search.SearchFactory;
 import com.fastsearch.esp.search.query.BaseParameter;
 import com.fastsearch.esp.search.query.IQuery;
 import com.fastsearch.esp.search.query.Query;
 import com.fastsearch.esp.search.query.SearchParameter;
 import com.fastsearch.esp.search.result.IDocumentSummary;
 import com.fastsearch.esp.search.result.IDocumentSummaryField;
 import com.fastsearch.esp.search.result.IQueryResult;
 import com.fastsearch.esp.search.view.ISearchView;
 import no.sesat.search.mode.config.EspFastCommandConfig;
 import no.sesat.search.query.AndClause;
 import no.sesat.search.query.AndNotClause;
 import no.sesat.search.query.DefaultOperatorClause;
 import no.sesat.search.query.LeafClause;
 import no.sesat.search.query.NotClause;
 import no.sesat.search.query.OrClause;
 import no.sesat.search.query.UrlClause;
 import no.sesat.search.query.Visitor;
 import no.sesat.search.query.XorClause;
 import no.sesat.search.result.BasicResultList;
 import no.sesat.search.result.BasicResultItem;
 import no.sesat.search.result.FastSearchResult;
 import no.sesat.search.result.ResultItem;
 import no.sesat.search.result.ResultList;
 import no.sesat.search.site.config.SiteConfiguration;
 import org.apache.log4j.Logger;
 
 import java.io.IOException;
 import java.net.URL;
 import java.util.Map;
 import java.util.Properties;
 import java.util.concurrent.ConcurrentHashMap;
 
 /**
  * Base class for commands querying a FAST EPS Server.
  * See https://dev.sesat.no/confluence/display/TECHDEV/FAST+ESP+5.0+Documentation
 *
  * @version $Id$
  */
 public abstract class AbstractESPFastSearchCommand extends AbstractSearchCommand {
 
 
     // Attributes ----------------------------------------------------
     private final EspFastCommandConfig cfg;
     private final String queryServer;
     private final ISearchView searchView;
     private IQueryResult result;
 
     // Static --------------------------------------------------------
 
     private static final Map<String, ISearchView> SEARCH_VIEWS = new ConcurrentHashMap<String, ISearchView>();
 
     private final static String FACTORY_PROPERTY = "com.fastsearch.esp.search.SearchFactory";
     private final static String HTTP_FACTORY = "com.fastsearch.esp.search.http.HttpSearchFactory";
     private final static String QR_SERVER_PROPERTY = "com.fastsearch.esp.search.http.qrservers";
     private final static String ENCODER_PROPERTY = "com.fastsearch.esp.search.http.encoderclass";
     private final static String ENCODER_CLASS = "com.fastsearch.esp.search.http.DSURLUTF8Encoder";
 
     private final static String COLLAPSE_PARAMETER = "collapse";
 
     private static final Logger LOG = Logger.getLogger(AbstractESPFastSearchCommand.class);
     private static final String ERR_CALL_SET_VIEW = "setView() must be called prior to calling this method";
 
     private enum ReservedWord {
         AND("and"),
         OR("or"),
         ANDNOT("andnot"),
         MAX("max"),
         MIN("min"),
         ANY("any"),
         PHRASE("phrase"),
         RANK("rank"),
         NEAR("near"),
         ONEAR("onear"),
         INT("int"),
         FLOAT("float"),
         DOUBLE("double"),
         DATETIME("datetime"),
         RANGE("range"),
         FILTER("filter"),
         STARTS_WITH("starts-with"),
         ENDS_WITH("ends-with"),
         EQUALS("equals"),
         COUNT("count"),
         STRING("string");
 
         private String word;
 
         ReservedWord(final String word) {
             this.word = word;
         }
 
         public final String getWord() {
             return word;
         }
     }
 
     // Constructors --------------------------------------------------
 
     /**
      * Creates new instance of search command.
      *
      * @param cxt The context to work in.
      */
     public AbstractESPFastSearchCommand(final Context cxt) {
 
         super(cxt);
 
         cfg = (EspFastCommandConfig) getSearchConfiguration();
         final SiteConfiguration siteConf = cxt.getDataModel().getSite().getSiteConfiguration();
         queryServer = siteConf.getProperty(cfg.getQueryServer());
         searchView = initialiseSearchView();
     }
 
 
     // Public --------------------------------------------------------
 
     /**
      * {@inheritDoc}
      */
     public ResultList<? extends ResultItem> execute() {
 
         try {
 
             final StringBuilder filterBuilder = new StringBuilder();
 
             if (getFilter() != null) {
                 filterBuilder.append(getFilter());
                 filterBuilder.append(' ');
             }
 
             if (getAdditionalFilter() != null) {
                 filterBuilder.append(getAdditionalFilter());
                 filterBuilder.append(' ');
             }
 
             final String transformedQuery = appendFilter(cfg.getFilter(), getTransformedQuery());
 
             LOG.debug("Transformed query is " + transformedQuery);
 
             final String collapseId = getParameter(COLLAPSE_PARAMETER);
 
             final IQuery query = new Query(transformedQuery);
 
             if (isCollapsingEnabled()) {
                 if (collapseId == null || "".equals(collapseId) || !cfg.isExpansionEnabled()) {
                     if (cfg.isCollapsingRemoves()) {
                         query.setParameter(new SearchParameter("collapseon", "batvcollapseid"));
                     }
                 } else {
                     filterBuilder.append("+collapseid:").append(collapseId);
                 }
             }
 
             final String sortBy = getSortBy();
 
             query.setParameter(new SearchParameter("sesat:uniqueId",
                     context.getDataModel().getParameters().getUniqueId()));
 
             query.setParameter(new SearchParameter(BaseParameter.OFFSET, getOffset()));
             query.setParameter(new SearchParameter(BaseParameter.HITS, cfg.getResultsToReturn()));
             query.setParameter(new SearchParameter(BaseParameter.SORT_BY, sortBy));
            query.setParameter(new SearchParameter(BaseParameter.LEMMATIZE, cfg.isLemmatize()));
             query.setParameter(new SearchParameter(BaseParameter.FILTER, filterBuilder.toString()));
 
             if (!isNavigatable()) {
                 query.setParameter(new SearchParameter(BaseParameter.NAVIGATION, 0));
            }
 
             if (!"".equals(cfg.getQtPipeline())) {
                 query.setParameter(new SearchParameter(BaseParameter.QT_PIPELINE, cfg.getQtPipeline()));
             }
 
             modifyQuery(query);

             DUMP.info(query);
 
             result = searchView.search(query);
 
             return createSearchResult(result);
 
         } catch (SearchEngineException ex) {
             LOG.error(ex.getMessage() + ' ' + ex.getCause());
             return new BasicResultList<ResultItem>();
 
         } catch (IOException ex) {
             LOG.error(ex.getMessage(), ex);
             throw new SearchCommandException(ex);
         }
     }
 
     private String appendFilter(final String filter, final String q) {
         if (q.length() == 0 && filter.length() > 0) {
             return "filter(" + filter + ")";
         } else {
             return filter.length() > 0 ? "and(" + q + "," + "filter(" + filter + "))" : q;
         }
     }
 
     // Z implementation ----------------------------------------------
 
     // Y overrides ---------------------------------------------------
 
     // Package protected ---------------------------------------------
 
     // Protected -----------------------------------------------------
 
     /**
      * Default collapsing from the configuration, can be overridden in subcommands..
      * @return true if collapsing is enabled
      */
     protected boolean isCollapsingEnabled() {
         return cfg.isCollapsingEnabled();
     }
 
     /**
      * Default sortby, can be overridden by subcommands.
      * @return sortby field
      */
     protected String getSortBy() {
         String sortBy = cfg.getSortBy();
 
         if (null != getParameter("userSortBy")) {
 
             final String userSortBy = getParameter("userSortBy");
             LOG.debug("execute: SortBy " + userSortBy);
 
             if("default".equals(userSortBy)) {
                 sortBy = cfg.getSortBy();
             } else if ("alternative".equals(userSortBy) && cfg.getAlternativeSortBy() != null) {
                 sortBy = cfg.getAlternativeSortBy();
             } else if  ("standard".equals(userSortBy)) {
                 sortBy = "-frontpagename -contentprofile -docdatetime";
             } else if ("datetime".equals(userSortBy)) {
                 sortBy = "-frontpagename -docdatetime";
             } else{
                 sortBy = cfg.getSortBy();
             }
         }
 
         return sortBy;
     }

     /**
      * {@inheritDoc}
      */
     @Override
     protected String escapeFieldedLeaf(final LeafClause clause) {
 
         return '"' + (null != clause.getField() ? clause.getField() + ':' : "") + getTransformedTerm(clause) + '"';
     }
 
     /**
      * Concrete SearchCommand should override if it wants to set custom SearchParameters or do other
      * modifications to the query before it is run.
      *
      * @param query the FAST query to modify
      */
     protected void modifyQuery(IQuery query) {
         // Doing nothing
     }
 
 
     /**
      * Concrete SearchCommand should override if it wants to make custom SearchResult
      * from the FAST QueryResult.
      * <p/>
      * <b>Note:</b> To be sure that we do not break code for subclasses that depends on that this is in fact a
      * FastSearchResult and not a SearchResult, I made the signature of this return a FastSearchResult.
      * This, at least, applies to NavigatableESPFastCommand. Geir H. Pettersen - T-Rank.
      *
      * @param result the FAST IQueryResult to make a SearchResult from.
      * @return a searchResult constructed from the supplied IQueryResult.
      * @throws IOException if something bad happens... Like, an invalid url. (Actually just to not break old code.)
      */
     protected FastSearchResult<ResultItem> createSearchResult(final IQueryResult result) throws IOException {
 
         final FastSearchResult<ResultItem> searchResult = new FastSearchResult<ResultItem>();
         final int cnt = getOffset();
         final int maxIndex = getMaxDocIndex(result, cnt, cfg);
 
         searchResult.setHitCount(result.getDocCount());
 
         for (int i = cnt; i < maxIndex; i++) {
             try {
                 final IDocumentSummary document = result.getDocument(i + 1);
                 searchResult.addResult(createResultItem(document));
 
             } catch (NullPointerException e) { // THe doc count is not 100% accurate.
                 LOG.debug("Error finding document " + e);
                 return searchResult;
             }
         }
 
         if (isCollapsingEnabled() && cfg.isExpansionEnabled()) {
             final String collapseId = getParameter(COLLAPSE_PARAMETER);
             if (collapseId != null && !collapseId.equals("")) {
                 if (searchResult.getResults().size() > 0) {
                     final ResultItem itm = searchResult.getResults().get(0);
                     final URL url = new URL(itm.getField("url"));
                     searchResult.addField("collapsedDomain", url.getHost());
                 }
             }
         }
         return searchResult;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     protected final String escapeTerm(final String term) {

        final String termLC = term.toLowerCase();

         for (ReservedWord word : ReservedWord.values()) {

             // Term might already be prefixed by the TermPrefixTransformer.
            if (termLC.contains(":") && termLC.endsWith(':' + word.getWord()) || termLC.equals(word.getWord())) {
                return termLC.replace(word.getWord(), '"' + word.getWord() + '"');
             }
         }
 
         return term;
     }
 
     // Generate query in FQL.
     /**
      * {@inheritDoc}
      */
     @Override
     protected void visitImpl(final AndClause clause) {
         // The leaf clauses might not produce any output. For example terms
         // having a site: field. In these cases we should not output the
         // operator keyword.
         boolean hasEmptyLeaf = false;
 
         hasEmptyLeaf |= isEmptyLeaf(clause.getFirstClause());
         hasEmptyLeaf |= isEmptyLeaf(clause.getSecondClause());
 
         clause.getFirstClause().accept(this);
 
         if (!hasEmptyLeaf)
             appendToQueryRepresentation(" and ");
 
         clause.getSecondClause().accept(this);
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     protected void visitImpl(final OrClause clause) {
         appendToQueryRepresentation(" (");
         clause.getFirstClause().accept(this);
         appendToQueryRepresentation(" or ");
         clause.getSecondClause().accept(this);
         appendToQueryRepresentation(") ");
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     protected void visitImpl(final DefaultOperatorClause clause) {
         boolean hasEmptyLeaf = false;
 
         hasEmptyLeaf |= isEmptyLeaf(clause.getFirstClause());
         hasEmptyLeaf |= isEmptyLeaf(clause.getSecondClause());
 
         clause.getFirstClause().accept(this);
 
         if (!hasEmptyLeaf)
             appendToQueryRepresentation(" and ");
 
         clause.getSecondClause().accept(this);
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     protected void visitImpl(final NotClause clause) {
         appendToQueryRepresentation(" not ");
         appendToQueryRepresentation("(");
         clause.getFirstClause().accept(this);
         appendToQueryRepresentation(")");
 
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     protected void visitImpl(final AndNotClause clause) {
         appendToQueryRepresentation("andnot ");
         appendToQueryRepresentation("(");
         clause.getFirstClause().accept(this);
         appendToQueryRepresentation(")");
     }
 
     /**
      * Adds quotes around the URL and replacing the '?'. Failing so will produce syntax error in filter.
      *
      * @param clause The url clause.
      */
     protected void visitImpl(final UrlClause clause) {
 
         appendToQueryRepresentation('\"' + getTransformedTerm(clause).replace("?", " ") + '\"');
     }
 
     /**
      * @param clause The clause to examine.
      */
     @Override
     protected void visitXorClause(final Visitor visitor, final XorClause clause) {
         switch (clause.getHint()) {
             case FULLNAME_ON_LEFT:
             case PHRASE_ON_LEFT:
                 // Web searches should use phrases over separate words.
                 clause.getFirstClause().accept(visitor);
                 break;
             default:
                 // All other high level clauses are ignored.
                 clause.getSecondClause().accept(visitor);
                 break;
         }
     }
 
 
     /**
      * Returns the fast search result
      *
      * @return The fast search result.
      */
     protected IQueryResult getIQueryResult() {
         return result;
     }
 
     protected boolean isNavigatable() {
         return false;
     }
 
 
     // Private -------------------------------------------------------
 
     private ISearchView initialiseSearchView() {
 
 
         final String view = cfg.getView();
 
         if (view == null) {
             throw new IllegalStateException(ERR_CALL_SET_VIEW);
         }
 
         final String searchViewKey = queryServer + "/" + view;
 
         ISearchView searchView = SEARCH_VIEWS.get(searchViewKey);
 
         if (null == searchView) {
             final Properties props = new Properties();
 
             props.setProperty(FACTORY_PROPERTY, HTTP_FACTORY);
             props.setProperty(QR_SERVER_PROPERTY, queryServer);
             props.setProperty(ENCODER_PROPERTY, ENCODER_CLASS);
 
             try {
                 searchView = SearchFactory.newInstance(props).getSearchView(view);
 
                 // Force server address since we want to use the hardware load balancer.
                 // This also enables us to do tunneling.
                 final String serverName = queryServer.substring(0, queryServer.indexOf(':'));
                 final String serverPort = queryServer.substring(queryServer.indexOf(':') + 1);
                 searchView.setServerAddress(serverName, Integer.parseInt(serverPort), false);
             } catch (ConfigurationException e) {
                 throw new SearchCommandException(e);
             } catch (SearchEngineException e) {
                 throw new SearchCommandException(e);
             }
             SEARCH_VIEWS.put(searchViewKey, searchView);
         }
 
         LOG.debug("Using searchView: " + searchViewKey);
 
         return searchView;
     }
 
     private int getMaxDocIndex(
             final IQueryResult iQueryResult,
             final int cnt,
             final EspFastCommandConfig fastSearchConfiguration) {
         return Math.min(cnt + fastSearchConfiguration.getResultsToReturn(), iQueryResult.getDocCount());
     }
 
     private ResultItem createResultItem(final IDocumentSummary document) {
 
         ResultItem item = new BasicResultItem();
 
         for (final Map.Entry<String, String> entry : cfg.getResultFields().entrySet()) {
             final IDocumentSummaryField summary = document.getSummaryField(entry.getKey());
 
             if (summary != null && !summary.isEmpty()) {
                 item = item.addField(entry.getValue(), summary.getStringValue().trim());
             }
         }
 
         if (isCollapsingEnabled()) {
             final String currCollapseId = getParameter(COLLAPSE_PARAMETER);
 
             if (currCollapseId == null || currCollapseId.equals("")) {
 
                 if (!document.getSummaryField("fcocount").isEmpty() && Integer.parseInt(document.getSummaryField("fcocount").getStringValue()) > 1) {
                     item = item.addField("moreHits", "true")
                             .addField("collapseParameter", COLLAPSE_PARAMETER)
                             .addField("collapseId", document.getSummaryField("collapseid").getStringValue());
                 }
             }
         }
         return item;
     }
 
     // Inner classes -------------------------------------------------
 }
 
