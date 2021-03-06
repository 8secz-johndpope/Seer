 /*
  *      Copyright (c) 2004-2013 YAMJ Members
  *      https://github.com/organizations/YAMJ/teams
  *
  *      This file is part of the Yet Another Media Jukebox (YAMJ).
  *
  *      The YAMJ is free software: you can redistribute it and/or modify
  *      it under the terms of the GNU General Public License as published by
  *      the Free Software Foundation, either version 3 of the License, or
  *      any later version.
  *
  *      YAMJ is distributed in the hope that it will be useful,
  *      but WITHOUT ANY WARRANTY; without even the implied warranty of
  *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *      GNU General Public License for more details.
  *
  *      You should have received a copy of the GNU General Public License
  *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
  *
  *      Web: https://github.com/YAMJ/yamj-v3
  *
  */
 package org.yamj.core.database.dao;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.EnumMap;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import org.apache.commons.collections.CollectionUtils;
 import org.apache.commons.lang3.StringUtils;
 import org.hibernate.type.DateType;
 import org.hibernate.type.IntegerType;
 import org.hibernate.type.LongType;
 import org.hibernate.type.StringType;
 import org.hibernate.type.TimestampType;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.stereotype.Service;
 import org.yamj.core.api.model.CountTimestamp;
 import org.yamj.core.api.model.dto.ApiVideoDTO;
 import org.yamj.common.type.MetaDataType;
 import org.yamj.core.api.model.CountGeneric;
 import org.yamj.core.api.model.DataItem;
 import org.yamj.core.api.model.DataItemTools;
 import org.yamj.core.api.wrapper.ApiWrapperList;
 import org.yamj.core.api.model.SqlScalars;
 import org.yamj.core.api.model.dto.ApiArtworkDTO;
 import org.yamj.core.api.model.dto.ApiSeriesInfoDTO;
 import org.yamj.core.api.model.dto.ApiEpisodeDTO;
 import org.yamj.core.api.model.dto.ApiGenreDTO;
 import org.yamj.core.api.model.dto.ApiPersonDTO;
 import org.yamj.core.api.model.dto.ApiSeasonInfoDTO;
 import org.yamj.core.api.options.OptionsEpisode;
 import org.yamj.core.api.options.OptionsIdArtwork;
 import org.yamj.core.api.options.OptionsIndexArtwork;
 import org.yamj.core.api.options.OptionsIndexPerson;
 import org.yamj.core.api.options.OptionsIndexVideo;
 import org.yamj.core.api.wrapper.ApiWrapperSingle;
 import org.yamj.core.database.model.type.ArtworkType;
 import org.yamj.core.hibernate.HibernateDao;
 
 @Service("apiDao")
 public class ApiDao extends HibernateDao {
 
     private static final Logger LOG = LoggerFactory.getLogger(ApiDao.class);
 
     /**
      * Generate the query and load the results into the wrapper
      *
      * @param sqlString
      * @param wrapper
      */
     public void getVideoList(ApiWrapperList<ApiVideoDTO> wrapper) {
         SqlScalars sqlScalars = new SqlScalars(generateSqlForVideoList(wrapper));
         OptionsIndexVideo options = (OptionsIndexVideo) wrapper.getOptions();
 
         sqlScalars.addScalar("id", LongType.INSTANCE);
         sqlScalars.addScalar("videoTypeString", StringType.INSTANCE);
         sqlScalars.addScalar("title", StringType.INSTANCE);
         sqlScalars.addScalar("originalTitle", StringType.INSTANCE);
         sqlScalars.addScalar("videoYear", IntegerType.INSTANCE);
         sqlScalars.addScalar("firstAired", StringType.INSTANCE);
         sqlScalars.addScalar("seriesId", LongType.INSTANCE);
         sqlScalars.addScalar("seasonId", LongType.INSTANCE);
         sqlScalars.addScalar("season", LongType.INSTANCE);
         DataItemTools.addDataItemScalars(sqlScalars, options.splitDataitems());
 
         List<ApiVideoDTO> queryResults = executeQueryWithTransform(ApiVideoDTO.class, sqlScalars, wrapper);
         wrapper.setResults(queryResults);
 
         if (CollectionUtils.isNotEmpty(queryResults)) {
             if (CollectionUtils.isNotEmpty(options.getArtworkTypes())) {
                 // Create and populate the ID list
                 Map<MetaDataType, List<Long>> ids = new EnumMap<MetaDataType, List<Long>>(MetaDataType.class);
                 for (MetaDataType mdt : MetaDataType.values()) {
                     ids.put(mdt, new ArrayList());
                 }
 
                 Map<String, ApiVideoDTO> results = new HashMap<String, ApiVideoDTO>();
 
                 for (ApiVideoDTO single : queryResults) {
                     // Add the item to the map for further processing
                     results.put(ApiArtworkDTO.makeKey(single), single);
                     // Add the ID to the list
                     ids.get(single.getVideoType()).add(single.getId());
                 }
 
                 boolean foundArtworkIds = Boolean.FALSE;    // Check to see that we have artwork to find
                 // Remove any blank entries
                 for (MetaDataType mdt : MetaDataType.values()) {
                     if (CollectionUtils.isEmpty(ids.get(mdt))) {
                         ids.remove(mdt);
                     } else {
                         // We've found an artwork, so we can continue
                         foundArtworkIds = Boolean.TRUE;
                     }
                 }
 
                 if (foundArtworkIds) {
                     LOG.debug("Found artwork to process, IDs: {}", ids);
                     addArtworks(ids, results, options);
                 } else {
                     LOG.debug("No artwork found to process, skipping.");
                 }
             } else {
                 LOG.debug("Artwork not required, skipping.");
             }
         } else {
             LOG.debug("No results found to process.");
         }
     }
 
     /**
      * Generate the SQL for the video list
      *
      * Note: In this method MetaDataType.UNKNOWN will return all types
      *
      * @param wrapper
      * @return
      */
     private String generateSqlForVideoList(ApiWrapperList<ApiVideoDTO> wrapper) {
         OptionsIndexVideo options = (OptionsIndexVideo) wrapper.getOptions();
         Map<String, String> includes = options.splitIncludes();
         Map<String, String> excludes = options.splitExcludes();
         List<DataItem> dataItems = options.splitDataitems();
 
         List<MetaDataType> mdt = options.splitTypes();
         LOG.debug("Getting video list for types: {}", mdt.toString());
         if (CollectionUtils.isNotEmpty(dataItems)) {
             LOG.debug("Additional data items requested: {}", dataItems.toString());
         }
 
         boolean hasMovie = mdt.contains(MetaDataType.MOVIE);
         boolean hasSeries = mdt.contains(MetaDataType.SERIES);
         boolean hasSeason = mdt.contains(MetaDataType.SEASON);
 
         StringBuilder sbSQL = new StringBuilder();
 
         // Add the movie entries
         if (hasMovie) {
             sbSQL.append(generateSqlForVideo(options, includes, excludes, dataItems));
         }
 
         if (hasMovie && hasSeries) {
             sbSQL.append(" UNION ALL ");
         }
 
         // Add the TV series entires
         if (hasSeries) {
             sbSQL.append(generateSqlForSeries(options, includes, excludes, dataItems));
         }
 
         if ((hasMovie || hasSeries) && hasSeason) {
             sbSQL.append(" UNION ALL ");
         }
 
         // Add the TV season entires
         if (hasSeason) {
             sbSQL.append(generateSqlForSeason(options, includes, excludes, dataItems));
         }
 
         // Add the sort string, this will be empty if there is no sort required
         sbSQL.append(options.getSortString());
 
         return sbSQL.toString();
     }
 
     /**
      * Create the SQL fragment for the selection of movies
      *
      * @param options
      * @param includes
      * @param excludes
      * @return
      */
     private String generateSqlForVideo(OptionsIndexVideo options, Map<String, String> includes, Map<String, String> excludes, List<DataItem> dataItems) {
         StringBuilder sbSQL = new StringBuilder();
 
         sbSQL.append("SELECT vd.id");
         sbSQL.append(", '").append(MetaDataType.MOVIE).append("' AS videoTypeString");
         sbSQL.append(", vd.title");
         sbSQL.append(", vd.title_original AS originalTitle");
         sbSQL.append(", vd.publication_year AS videoYear");
         sbSQL.append(", '-1' AS firstAired");
         sbSQL.append(", '-1' AS seriesId");
         sbSQL.append(", '-1' AS seasonId");
         sbSQL.append(", '-1' AS season");
         sbSQL.append(DataItemTools.addSqlDataItems(dataItems, "vd"));
         sbSQL.append(" FROM videodata vd");
         // Add genre tables for include and exclude
         if (includes.containsKey("genre") || excludes.containsKey("genre")) {
             sbSQL.append(", videodata_genres vg, genre g");
         }
 
         sbSQL.append(" WHERE vd.episode < 0");
         if (options.getId() > 0L) {
             sbSQL.append(" AND vd.id=").append(options.getId());
         }
         // Add joins for genres
         if (includes.containsKey("genre") || excludes.containsKey("genre")) {
             sbSQL.append(" AND vd.id=vg.data_id");
             sbSQL.append(" AND vg.genre_id=g.id");
             sbSQL.append(" AND g.name='");
             if (includes.containsKey("genre")) {
                 sbSQL.append(includes.get("genre"));
             } else {
                 sbSQL.append(excludes.get("genre"));
             }
             sbSQL.append("'");
         }
 
         if (includes.containsKey("year")) {
             sbSQL.append(" AND vd.publication_year=").append(includes.get("year"));
         }
 
         if (excludes.containsKey("year")) {
             sbSQL.append(" AND vd.publication_year!=").append(includes.get("year"));
         }
 
         // Add the search string, this will be empty if there is no search required
         sbSQL.append(options.getSearchString(false));
 
         LOG.debug("SQL: {}", sbSQL);
         return sbSQL.toString();
     }
 
     /**
      * Create the SQL fragment for the selection of series
      *
      * @param options
      * @param includes
      * @param excludes
      * @return
      */
     private String generateSqlForSeries(OptionsIndexVideo options, Map<String, String> includes, Map<String, String> excludes, List<DataItem> dataItems) {
         StringBuilder sbSQL = new StringBuilder();
 
         sbSQL.append("SELECT ser.id");
         sbSQL.append(", '").append(MetaDataType.SERIES).append("' AS videoTypeString");
         sbSQL.append(", ser.title");
         sbSQL.append(", ser.title_original AS originalTitle");
         sbSQL.append(", ser.start_year AS videoYear");
         sbSQL.append(", '-1' AS firstAired");
         sbSQL.append(", ser.id AS seriesId");
         sbSQL.append(", '-1' AS seasonId");
         sbSQL.append(", '-1' AS season");
         sbSQL.append(DataItemTools.addSqlDataItems(dataItems, "ser"));
         sbSQL.append(" FROM series ser ");
         sbSQL.append(" WHERE 1=1"); // To make it easier to add the optional include and excludes
         if (options.getId() > 0L) {
             sbSQL.append(" AND ser.id=").append(options.getId());
         }
 
         if (includes.containsKey("year")) {
             sbSQL.append(" AND ser.start_year=").append(includes.get("year"));
         }
 
         if (excludes.containsKey("year")) {
             sbSQL.append(" AND ser.start_year!=").append(includes.get("year"));
         }
 
         // Add the search string, this will be empty if there is no search required
         sbSQL.append(options.getSearchString(false));
 
         return sbSQL.toString();
     }
 
     /**
      * Create the SQL fragment for the selection of seasons
      *
      * @param options
      * @param includes
      * @param excludes
      * @return
      */
     private String generateSqlForSeason(OptionsIndexVideo options, Map<String, String> includes, Map<String, String> excludes, List<DataItem> dataItems) {
         StringBuilder sbSQL = new StringBuilder();
 
         sbSQL.append("SELECT sea.id");
         sbSQL.append(", '").append(MetaDataType.SEASON).append("' AS videoTypeString");
         sbSQL.append(", sea.title");
         sbSQL.append(", sea.title_original AS originalTitle");
         sbSQL.append(", -1 as videoYear");
         sbSQL.append(", sea.first_aired AS firstAired");
         sbSQL.append(", sea.series_id AS seriesId");
         sbSQL.append(", sea.id AS seasonId");
         sbSQL.append(", sea.season AS season");
         sbSQL.append(DataItemTools.addSqlDataItems(dataItems, "sea"));
         sbSQL.append(" FROM season sea");
         sbSQL.append(" WHERE 1=1"); // To make it easier to add the optional include and excludes
         if (options.getId() > 0L) {
             sbSQL.append(" AND sea.id=").append(options.getId());
         }
 
         if (includes.containsKey("year")) {
             sbSQL.append(" AND sea.first_aired LIKE '").append(includes.get("year")).append("%'");
         }
 
         if (excludes.containsKey("year")) {
             sbSQL.append(" AND sea.first_aired NOT LIKE '").append(includes.get("year")).append("%'");
         }
 
         // Add the search string, this will be empty if there is no search required
         sbSQL.append(options.getSearchString(false));
 
         return sbSQL.toString();
     }
 
     /**
      * Search the list of IDs for artwork and add to the artworkList.
      *
      * @param ids
      * @param artworkList
      * @param options
      */
     private void addArtworks(Map<MetaDataType, List<Long>> ids, Map<String, ApiVideoDTO> artworkList, OptionsIndexVideo options) {
         List<String> artworkRequired = options.getArtworkTypes();
         LOG.debug("Artwork required: {}", artworkRequired.toString());
 
         if (CollectionUtils.isNotEmpty(artworkRequired)) {
             SqlScalars sqlScalars = new SqlScalars();
             boolean hasMovie = CollectionUtils.isNotEmpty(ids.get(MetaDataType.MOVIE));
             boolean hasSeries = CollectionUtils.isNotEmpty(ids.get(MetaDataType.SERIES));
             boolean hasSeason = CollectionUtils.isNotEmpty(ids.get(MetaDataType.SEASON));
 
             if (hasMovie) {
                 sqlScalars.addToSql("SELECT 'MOVIE' as sourceString, v.id as sourceId, a.id as artworkId, al.id as locatedId, ag.id as generatedId, a.artwork_type as artworkTypeString, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename");
                 sqlScalars.addToSql(" FROM videodata v, artwork a");
                 sqlScalars.addToSql(" LEFT JOIN artwork_located al ON a.id=al.artwork_id");
                 sqlScalars.addToSql(" LEFT JOIN artwork_generated ag ON al.id=ag.located_id");
                 sqlScalars.addToSql(" WHERE v.id=a.videodata_id");
                 sqlScalars.addToSql(" AND v.episode<0");
                 sqlScalars.addToSql(" AND v.id IN (:movielist)");
                 sqlScalars.addToSql(" AND a.artwork_type IN (:artworklist)");
             }
 
             if (hasMovie && hasSeries) {
                 sqlScalars.addToSql(" UNION");
             }
 
             if (hasSeries) {
                 sqlScalars.addToSql(" SELECT 'SERIES' as sourceString, s.id as sourceId, a.id as artworkId, al.id as locatedId, ag.id as generatedId, a.artwork_type as artworkTypeString, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename");
                 sqlScalars.addToSql(" FROM series s, artwork a");
                 sqlScalars.addToSql(" LEFT JOIN artwork_located al ON a.id=al.artwork_id");
                 sqlScalars.addToSql(" LEFT JOIN artwork_generated ag ON al.id=ag.located_id");
                 sqlScalars.addToSql(" WHERE s.id=a.series_id");
                 sqlScalars.addToSql(" AND s.id IN (:serieslist)");
                 sqlScalars.addToSql(" AND a.artwork_type IN (:artworklist)");
             }
 
             if ((hasMovie || hasSeries) && hasSeason) {
                 sqlScalars.addToSql(" UNION");
             }
 
             if (hasSeason) {
                 sqlScalars.addToSql(" SELECT 'SEASON' as sourceString, s.id as sourceId, a.id as artworkId, al.id as locatedId, ag.id as generatedId, a.artwork_type as artworkTypeString, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename");
                 sqlScalars.addToSql(" FROM season s, artwork a");
                 sqlScalars.addToSql(" LEFT JOIN artwork_located al ON a.id=al.artwork_id");
                 sqlScalars.addToSql(" LEFT JOIN artwork_generated ag ON al.id=ag.located_id");
                 sqlScalars.addToSql(" WHERE s.id=a.season_id");
                 sqlScalars.addToSql(" AND s.id IN (:seasonlist)");
                 sqlScalars.addToSql(" AND a.artwork_type IN (:artworklist)");
             }
 
             sqlScalars.addScalar("sourceString", StringType.INSTANCE);
             sqlScalars.addScalar("sourceId", LongType.INSTANCE);
             sqlScalars.addScalar("artworkId", LongType.INSTANCE);
             sqlScalars.addScalar("locatedId", LongType.INSTANCE);
             sqlScalars.addScalar("generatedId", LongType.INSTANCE);
             sqlScalars.addScalar("artworkTypeString", StringType.INSTANCE);
             sqlScalars.addScalar("cacheDir", StringType.INSTANCE);
             sqlScalars.addScalar("cacheFilename", StringType.INSTANCE);
 
             if (hasMovie) {
                 sqlScalars.addParameters("movielist", ids.get(MetaDataType.MOVIE));
             }
 
             if (hasSeries) {
                 sqlScalars.addParameters("serieslist", ids.get(MetaDataType.SERIES));
             }
 
             if (hasSeason) {
                 sqlScalars.addParameters("seasonlist", ids.get(MetaDataType.SEASON));
             }
 
             sqlScalars.addParameters("artworklist", artworkRequired);
 
             List<ApiArtworkDTO> results = executeQueryWithTransform(ApiArtworkDTO.class, sqlScalars, null);
 
             LOG.trace("Found {} artworks", results.size());
             for (ApiArtworkDTO ia : results) {
                 LOG.trace("  {} = {}", ia.Key(), ia.toString());
                 artworkList.get(ia.Key()).addArtwork(ia);
             }
         }
     }
 
     /**
      * Get a single Count and Timestamp
      *
      * @param type
      * @param tablename
      * @param clause
      * @return
      */
     public CountTimestamp getCountTimestamp(MetaDataType type, String tablename, String clause) {
         if (StringUtils.isBlank(tablename)) {
             return null;
         }
 
         StringBuilder sql = new StringBuilder("SELECT '").append(type).append("' as typeString, ");
         sql.append("count(*) as count, ");
         sql.append("MAX(create_timestamp) as createTimestamp, ");
         sql.append("MAX(update_timestamp) as updateTimestamp, ");
         sql.append("MAX(id) as lastId ");
         sql.append("FROM ").append(tablename);
         if (StringUtils.isNotBlank(clause)) {
             sql.append(" WHERE ").append(clause);
         }
 
         SqlScalars sqlScalars = new SqlScalars(sql);
 
         sqlScalars.addScalar("typeString", StringType.INSTANCE);
         sqlScalars.addScalar("count", LongType.INSTANCE);
         sqlScalars.addScalar("createTimestamp", TimestampType.INSTANCE);
         sqlScalars.addScalar("updateTimestamp", TimestampType.INSTANCE);
         sqlScalars.addScalar("lastId", LongType.INSTANCE);
 
         List<CountTimestamp> results = executeQueryWithTransform(CountTimestamp.class, sqlScalars, null);
         if (CollectionUtils.isEmpty(results)) {
             return new CountTimestamp(type);
         }
 
         return results.get(0);
     }
 
     /**
      * Get a list of the people
      *
      * @param wrapper
      */
     public void getPersonList(ApiWrapperList<ApiPersonDTO> wrapper) {
         SqlScalars sqlScalars = generateSqlForPerson((OptionsIndexPerson) wrapper.getOptions());
         List<ApiPersonDTO> results = executeQueryWithTransform(ApiPersonDTO.class, sqlScalars, wrapper);
         wrapper.setResults(results);
     }
 
     /**
      * Get a single person using the ID in the wrapper options.
      *
      * @param wrapper
      */
     public void getPerson(ApiWrapperSingle<ApiPersonDTO> wrapper) {
         OptionsIndexPerson options = (OptionsIndexPerson) wrapper.getOptions();
         SqlScalars sqlScalars = generateSqlForPerson(options);
         List<ApiPersonDTO> results = executeQueryWithTransform(ApiPersonDTO.class, sqlScalars, wrapper);
         if (CollectionUtils.isNotEmpty(results)) {
             ApiPersonDTO person = results.get(0);
             if (options.splitDataitems().contains(DataItem.ARTWORK)) {
                 LOG.info("Adding photo for {}", person.getName());
                 // Add the artwork
                 Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForId(MetaDataType.PERSON, person.getId(), Arrays.asList("PHOTO"));
                 if (artworkList.containsKey(options.getId())) {
                     LOG.info("Found {} artworks", artworkList.get(options.getId()).size());
                     person.setArtwork(artworkList.get(options.getId()));
                 } else {
                     LOG.info("No artwork found for Person ID '{}'", options.getId());
                 }
             }
             wrapper.setResult(person);
         } else {
             wrapper.setResult(null);
         }
     }
 
     public void getPersonListByVideoType(MetaDataType metaDataType, ApiWrapperList<ApiPersonDTO> wrapper) {
         OptionsIndexPerson options = (OptionsIndexPerson) wrapper.getOptions();
         LOG.info("Getting person list for {} with ID '{}'", metaDataType, options.getId());
 
         SqlScalars sqlScalars = generateSqlForVideoPerson(metaDataType, options);
         List<ApiPersonDTO> results = executeQueryWithTransform(ApiPersonDTO.class, sqlScalars, wrapper);
         LOG.info("Found {} results for {} with id '{}'", results.size(), metaDataType, options.getId());
 
         if (options.splitDataitems().contains(DataItem.ARTWORK)) {
             LOG.info("Looking for person artwork for {} with id '{}'", metaDataType, options.getId());
             List<Long> personIds = new ArrayList<Long>();
             for (ApiPersonDTO p : results) {
                 personIds.add(p.getId());
             }
 
             Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForId(MetaDataType.PERSON, personIds, Arrays.asList("PHOTO"));
             for (ApiPersonDTO person : results) {
                 if (artworkList.containsKey(person.getId())) {
                     person.setArtwork(artworkList.get(person.getId()));
                 }
             }
         } else {
             LOG.info("No artwork requested for {} with id '{}'", metaDataType, options.getId());
         }
 
         wrapper.setResults(results);
     }
 
     private SqlScalars generateSqlForVideoPerson(MetaDataType metaDataType, OptionsIndexPerson options) {
         SqlScalars sqlScalars = new SqlScalars();
 
         sqlScalars.addToSql("SELECT p.id,");
         sqlScalars.addToSql(" p.name,");
         sqlScalars.addToSql(" p.biography,");
         sqlScalars.addToSql(" p.birth_day AS birthDay,");
         sqlScalars.addToSql(" p.birth_place AS birthPlace,");
         sqlScalars.addToSql(" p.birth_name AS birthName,");
         sqlScalars.addToSql(" p.death_day AS deathDay,");
         sqlScalars.addToSql(" c.job,");
         sqlScalars.addToSql(" c.role");
         sqlScalars.addToSql(" FROM person p, cast_crew c");
         sqlScalars.addToSql(" WHERE p.id=c.person_id");
 
         // TODO: Split by series/season/episode
         if (metaDataType == MetaDataType.MOVIE) {
             sqlScalars.addToSql(" AND c.videodata_id=:id");
         } else if (metaDataType == MetaDataType.SERIES) {
             sqlScalars.addToSql("AND c.videodata_id IN");
             sqlScalars.addToSql(" (SELECT DISTINCT v.id FROM season s, videodata v");
             sqlScalars.addToSql(" WHERE s.series_id = :id AND s.id = v.season_id)");
         } else if (metaDataType == MetaDataType.SEASON) {
         } else if (metaDataType == MetaDataType.EPISODE) {
             sqlScalars.addToSql(" AND c.videodata_id=:id");
         } else {
             throw new UnsupportedOperationException("Person list by '" + metaDataType.toString() + "' not supported.");
         }
 
         if (CollectionUtils.isNotEmpty(options.getJob())) {
             sqlScalars.addToSql(" AND c.job IN (:joblist)");
             sqlScalars.addParameters("joblist", options.getJob());
         }
 
         // Add the search string
         sqlScalars.addToSql(options.getSearchString(Boolean.FALSE));
         // This will default to blank if there's no sort required
         sqlScalars.addToSql(options.getSortString());
 
         // Add the ID
         sqlScalars.addParameters("id", options.getId());
 
         sqlScalars.addScalar("id", LongType.INSTANCE);
         sqlScalars.addScalar("name", StringType.INSTANCE);
         sqlScalars.addScalar("biography", StringType.INSTANCE);
         sqlScalars.addScalar("birthDay", DateType.INSTANCE);
         sqlScalars.addScalar("birthPlace", StringType.INSTANCE);
         sqlScalars.addScalar("birthName", StringType.INSTANCE);
         sqlScalars.addScalar("deathDay", DateType.INSTANCE);
         sqlScalars.addScalar("job", StringType.INSTANCE);
         sqlScalars.addScalar("role", StringType.INSTANCE);
 
         LOG.debug("SQL ForVideoPerson: {}", sqlScalars.getSql());
         return sqlScalars;
     }
 
     private SqlScalars generateSqlForPerson(OptionsIndexPerson options) {
         SqlScalars sqlScalars = new SqlScalars();
         List<DataItem> dataitems = options.splitDataitems();
         // Make sure to set the alias for the files for the Transformation into the class
         sqlScalars.addToSql("SELECT DISTINCT p.id,");
         sqlScalars.addToSql(" p.name,");
         if (dataitems.contains(DataItem.BIOGRAPHY)) {
             sqlScalars.addToSql(" p.biography, ");
             sqlScalars.addScalar("biography", StringType.INSTANCE);
         }
         sqlScalars.addToSql(" p.birth_day AS birthDay, ");
         sqlScalars.addToSql(" p.birth_place AS birthPlace, ");
         sqlScalars.addToSql(" p.birth_name AS birthName, ");
         sqlScalars.addToSql(" p.death_day AS deathDay ");
         sqlScalars.addToSql(" FROM person p");
         if (CollectionUtils.isNotEmpty(options.getJob())) {
             sqlScalars.addToSql(", cast_crew c");
         }
         if (options.getId() > 0L) {
             sqlScalars.addToSql(" WHERE id=:id");
             sqlScalars.addParameters("id", options.getId());
         } else {
             sqlScalars.addToSql(" WHERE 1=1");
         }
         if (CollectionUtils.isNotEmpty(options.getJob())) {
             sqlScalars.addToSql(" AND p.id=c.person_id");
             sqlScalars.addToSql(" AND c.job IN (:joblist)");
             sqlScalars.addParameters("joblist", options.getJob());
         }
         // Add the search string
         sqlScalars.addToSql(options.getSearchString(Boolean.FALSE));
         // This will default to blank if there's no sort required
         sqlScalars.addToSql(options.getSortString());
 
         sqlScalars.addScalar("id", LongType.INSTANCE);
         sqlScalars.addScalar("name", StringType.INSTANCE);
         sqlScalars.addScalar("birthDay", DateType.INSTANCE);
         sqlScalars.addScalar("birthPlace", StringType.INSTANCE);
         sqlScalars.addScalar("birthName", StringType.INSTANCE);
         sqlScalars.addScalar("deathDay", DateType.INSTANCE);
 
         return sqlScalars;
     }
 
     //<editor-fold defaultstate="collapsed" desc="Artwork Methods">
     public ApiArtworkDTO getArtworkById(Long id) {
         SqlScalars sqlScalars = getSqlArtwork(new OptionsIndexArtwork(id));
 
         List<ApiArtworkDTO> results = executeQueryWithTransform(ApiArtworkDTO.class, sqlScalars, null);
         if (CollectionUtils.isEmpty(results)) {
             return new ApiArtworkDTO();
         }
 
         return results.get(0);
     }
 
     public List<ApiArtworkDTO> getArtworkList(ApiWrapperList<ApiArtworkDTO> wrapper) {
         SqlScalars sqlScalars = getSqlArtwork((OptionsIndexArtwork) wrapper.getOptions());
         return executeQueryWithTransform(ApiArtworkDTO.class, sqlScalars, wrapper);
     }
 
     private SqlScalars getSqlArtwork(OptionsIndexArtwork options) {
         SqlScalars sqlScalars = new SqlScalars();
 
         sqlScalars.addToSql("SELECT a.id AS artworkId,");
         sqlScalars.addToSql(" al.id AS locatedId,");
         sqlScalars.addToSql(" ag.id AS generatedId,");
         sqlScalars.addToSql(" a.season_id AS seasonId,");
         sqlScalars.addToSql(" a.series_id AS seriesId,");
         sqlScalars.addToSql(" a.videodata_id AS videodataId,");
         sqlScalars.addToSql(" a.artwork_type AS artworkTypeString,");
         sqlScalars.addToSql(" ag.cache_filename AS cacheFilename,");
         sqlScalars.addToSql(" ag.cache_dir AS cacheDir");
         sqlScalars.addToSql(" FROM artwork a");
         sqlScalars.addToSql(" LEFT JOIN artwork_located al ON a.id=al.artwork_id");
         sqlScalars.addToSql(" LEFT JOIN artwork_generated ag ON al.id=ag.located_id");
         sqlScalars.addToSql(" WHERE 1=1"); // Make appending restrictions easier
         if (options != null) {
             if (options.getId() > 0L) {
                 sqlScalars.addToSql(" AND a.id=:id");
                 sqlScalars.addParameters("id", options.getId());
             }
 
             if (CollectionUtils.isNotEmpty(options.getArtwork())) {
                 sqlScalars.addToSql(" AND a.artwork_type IN (:artworklist)");
                 sqlScalars.addParameters("artworklist", options.getArtwork());
             }
 
             if (CollectionUtils.isNotEmpty(options.getVideo())) {
                 StringBuilder sb = new StringBuilder("AND (");
                 boolean first = Boolean.TRUE;
                 for (String type : options.getVideo()) {
                     MetaDataType mdt = MetaDataType.fromString(type);
                     if (first) {
                         first = Boolean.FALSE;
                     } else {
                         sb.append(" OR");
                     }
                     if (mdt == MetaDataType.MOVIE) {
                         sb.append(" videodata_id IS NOT NULL");
                     } else if (mdt == MetaDataType.SERIES) {
                         sb.append(" series_id IS NOT NULL");
                     } else if (mdt == MetaDataType.SEASON) {
                         sb.append(" season_id IS NOT NULL");
                     }
                 }
                 sb.append(")");
                 sqlScalars.addToSql(sb.toString());
             }
         }
 
         // Add the scalars
         sqlScalars.addScalar("artworkId", LongType.INSTANCE);
         sqlScalars.addScalar("locatedId", LongType.INSTANCE);
         sqlScalars.addScalar("generatedId", LongType.INSTANCE);
         sqlScalars.addScalar("seasonId", LongType.INSTANCE);
         sqlScalars.addScalar("seriesId", LongType.INSTANCE);
         sqlScalars.addScalar("videodataId", LongType.INSTANCE);
         sqlScalars.addScalar("artworkTypeString", StringType.INSTANCE);
         sqlScalars.addScalar("cacheDir", StringType.INSTANCE);
         sqlScalars.addScalar("cacheFilename", StringType.INSTANCE);
 
         return sqlScalars;
     }
     //</editor-fold>
 
     public void getEpisodeList(ApiWrapperList<ApiEpisodeDTO> wrapper) {
         OptionsEpisode options = (OptionsEpisode) wrapper.getOptions();
         SqlScalars sqlScalars = new SqlScalars();
 
         sqlScalars.addToSql("SELECT ser.id AS seriesId, sea.id AS seasonId, sea.season, vid.episode, vid.title,");
         sqlScalars.addToSql("ag.cache_filename AS cacheFilename, ag.cache_dir AS cacheDir");
         sqlScalars.addToSql("FROM season sea, series ser, videodata vid, artwork a");
         sqlScalars.addToSql("LEFT JOIN artwork_located al ON a.id=al.artwork_id");
         sqlScalars.addToSql("LEFT JOIN artwork_generated ag ON al.id=ag.located_id");
         sqlScalars.addToSql("WHERE sea.series_id=ser.id");
         sqlScalars.addToSql("AND vid.season_id=sea.id");
         sqlScalars.addToSql("AND a.videodata_id=vid.id");
         if (options.getSeriesid() > 0L) {
             sqlScalars.addToSql("AND ser.id=:seriesid");
             sqlScalars.addParameters("seriesid", options.getSeriesid());
             if (options.getSeason() > 0L) {
                 sqlScalars.addToSql("AND sea.season=:season");
                 sqlScalars.addParameters("season", options.getSeason());
             }
         }
         if (options.getSeasonid() > 0L) {
             sqlScalars.addToSql("AND sea.id=:seasonid");
             sqlScalars.addParameters("seasonid", options.getSeasonid());
         }
         sqlScalars.addToSql("ORDER BY seriesId, season, episode");
 
         sqlScalars.addScalar("seriesId", LongType.INSTANCE);
         sqlScalars.addScalar("seasonId", LongType.INSTANCE);
         sqlScalars.addScalar("season", LongType.INSTANCE);
         sqlScalars.addScalar("episode", LongType.INSTANCE);
         sqlScalars.addScalar("title", StringType.INSTANCE);
         sqlScalars.addScalar("cacheFilename", StringType.INSTANCE);
         sqlScalars.addScalar("cacheDir", StringType.INSTANCE);
 
         List<ApiEpisodeDTO> results = executeQueryWithTransform(ApiEpisodeDTO.class, sqlScalars, wrapper);
         wrapper.setResults(results);
     }
 
     public void getSingleVideo(ApiWrapperSingle<ApiVideoDTO> wrapper) {
         OptionsIndexVideo options = (OptionsIndexVideo) wrapper.getOptions();
         Map<String, String> includes = options.splitIncludes();
         Map<String, String> excludes = options.splitExcludes();
         MetaDataType type = MetaDataType.fromString(options.getType());
 
         List<DataItem> dataItems = options.splitDataitems();
         LOG.debug("Getting additional data items: {} ", dataItems.toString());
 
         String sql;
         if (type == MetaDataType.MOVIE) {
             sql = generateSqlForVideo(options, includes, excludes, dataItems);
         } else if (type == MetaDataType.SERIES) {
             sql = generateSqlForSeries(options, includes, excludes, dataItems);
         } else if (type == MetaDataType.SEASON) {
             sql = generateSqlForSeason(options, includes, excludes, dataItems);
         } else {
             throw new UnsupportedOperationException("Unable to process type '" + type + "' (Original: '" + options.getType() + "')");
         }
         LOG.debug("SQL for {}-{}: {}", type, options.getId(), sql);
 
         SqlScalars sqlScalars = new SqlScalars(sql);
 
         sqlScalars.addScalar("id", LongType.INSTANCE);
         sqlScalars.addScalar("videoTypeString", StringType.INSTANCE);
         sqlScalars.addScalar("title", StringType.INSTANCE);
         sqlScalars.addScalar("originalTitle", StringType.INSTANCE);
         sqlScalars.addScalar("videoYear", IntegerType.INSTANCE);
         sqlScalars.addScalar("firstAired", StringType.INSTANCE);
         sqlScalars.addScalar("seriesId", LongType.INSTANCE);
         sqlScalars.addScalar("seasonId", LongType.INSTANCE);
         sqlScalars.addScalar("season", LongType.INSTANCE);
         // Add Scalars for additional data item columns
         DataItemTools.addDataItemScalars(sqlScalars, dataItems);
 
         List<ApiVideoDTO> queryResults = executeQueryWithTransform(ApiVideoDTO.class, sqlScalars, wrapper);
         LOG.debug("Found {} results for ID '{}'", queryResults.size(), options.getId());
         if (CollectionUtils.isNotEmpty(queryResults)) {
             ApiVideoDTO video = queryResults.get(0);
             if (dataItems.contains(DataItem.GENRE)) {
                 LOG.debug("Adding genres");
                 video.setGenres(getGenresForId(MetaDataType.MOVIE, options.getId()));
             }
 
             if (dataItems.contains(DataItem.ARTWORK)) {
                 LOG.debug("Adding artwork");
                 Map<Long, List<ApiArtworkDTO>> artworkList;
                 if (CollectionUtils.isNotEmpty(options.getArtworkTypes())) {
                     artworkList = getArtworkForId(type, options.getId(), options.getArtworkTypes());
                 } else {
                     artworkList = getArtworkForId(type, options.getId());
                 }
 
                 if (artworkList.containsKey(options.getId())) {
                     video.setArtwork(artworkList.get(options.getId()));
                 }
             }
             wrapper.setResult(video);
         } else {
             wrapper.setResult(null);
         }
     }
 
     /**
      * Get a list of the Genres for a give video ID
      *
      * @param type
      * @param id
      * @return
      */
     public List<ApiGenreDTO> getGenresForId(MetaDataType type, Long id) {
         SqlScalars sqlScalars = new SqlScalars();
         sqlScalars.addToSql("SELECT g.id, g.name");
         sqlScalars.addToSql("FROM videodata_genres vg, genre g");
         if (type == MetaDataType.SERIES) {
             sqlScalars.addToSql(", series v");
         } else if (type == MetaDataType.SEASON) {
             sqlScalars.addToSql(", season v");
         } else {
             // Default to Movie
             sqlScalars.addToSql(", videodata v");
         }
         sqlScalars.addToSql("WHERE vg.genre_id=g.id");
         sqlScalars.addToSql("AND vg.data_id=:id");
         sqlScalars.addToSql("AND v.id=vg.data_id");
 
         sqlScalars.addScalar("id", LongType.INSTANCE);
         sqlScalars.addScalar("name", StringType.INSTANCE);
 
         sqlScalars.addParameters("id", id);
 
         return executeQueryWithTransform(ApiGenreDTO.class, sqlScalars, null);
 
     }
 
     /**
      * Get a list of all artwork available for a video ID
      *
      * @param type
      * @param id
      * @return
      */
     public Map<Long, List<ApiArtworkDTO>> getArtworkForId(MetaDataType type, Long id) {
         List<String> artworkRequired = new ArrayList<String>();
         for (ArtworkType at : ArtworkType.values()) {
             artworkRequired.add(at.toString());
         }
         // Remove the unknown type
         artworkRequired.remove(ArtworkType.UNKNOWN.toString());
 
         return getArtworkForId(type, id, artworkRequired);
     }
 
     /**
      * Get a select list of artwork available for a video ID
      *
      * @param type
      * @param id
      * @param artworkRequired
      * @return
      */
     public Map<Long, List<ApiArtworkDTO>> getArtworkForId(MetaDataType type, Object id, List<String> artworkRequired) {
         LOG.debug("Artwork required for ID '{}' is {}", id, artworkRequired);
         StringBuilder sbSQL = new StringBuilder();
         sbSQL.append("SELECT '").append(type.toString()).append("' as sourceString, ");
         sbSQL.append(" v.id as sourceId, a.id as artworkId, al.id as locatedId, ag.id as generatedId, ");
         sbSQL.append(" a.artwork_type as artworkTypeString, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename ");
         if (type == MetaDataType.MOVIE) {
             sbSQL.append("FROM videodata v ");
         } else if (type == MetaDataType.SERIES) {
             sbSQL.append("FROM series v ");
         } else if (type == MetaDataType.SEASON) {
             sbSQL.append("FROM season v ");
         } else if (type == MetaDataType.PERSON) {
             sbSQL.append("FROM person v");
         }
         sbSQL.append(", artwork a");    // Artwork must be last for the LEFT JOIN
         sbSQL.append(" LEFT JOIN artwork_located al ON a.id=al.artwork_id");
         sbSQL.append(" LEFT JOIN artwork_generated ag ON al.id=ag.located_id");
         if (type == MetaDataType.MOVIE) {
             sbSQL.append(" WHERE v.id=a.videodata_id");
             sbSQL.append(" AND v.episode<0");
         } else if (type == MetaDataType.SERIES) {
             sbSQL.append(" WHERE v.id=a.series_id");
         } else if (type == MetaDataType.SEASON) {
             sbSQL.append(" WHERE v.id=a.season_id");
         } else if (type == MetaDataType.PERSON) {
             sbSQL.append(" WHERE v.id=a.person_id");
         }
         sbSQL.append(" AND v.id IN (:id)");
         sbSQL.append(" AND a.artwork_type IN (:artworklist)");
 
         SqlScalars sqlScalars = new SqlScalars(sbSQL);
 
         sqlScalars.addScalar("sourceString", StringType.INSTANCE);
         sqlScalars.addScalar("sourceId", LongType.INSTANCE);
         sqlScalars.addScalar("artworkId", LongType.INSTANCE);
         sqlScalars.addScalar("locatedId", LongType.INSTANCE);
         sqlScalars.addScalar("generatedId", LongType.INSTANCE);
         sqlScalars.addScalar("artworkTypeString", StringType.INSTANCE);
         sqlScalars.addScalar("cacheDir", StringType.INSTANCE);
         sqlScalars.addScalar("cacheFilename", StringType.INSTANCE);
 
         sqlScalars.addParameters("id", id);
         sqlScalars.addParameters("artworklist", artworkRequired);
 
         List<ApiArtworkDTO> results = executeQueryWithTransform(ApiArtworkDTO.class, sqlScalars, null);
         Map<Long, List<ApiArtworkDTO>> artworkList = new HashMap<Long, List<ApiArtworkDTO>>();
         for (ApiArtworkDTO artwork : results) {
             Long sourceId = artwork.getSourceId();
             if (artworkList.containsKey(sourceId)) {
                 artworkList.get(sourceId).add(artwork);
             } else {
                 // ID didn't exist so add a new list
                 artworkList.put(sourceId, new ArrayList<ApiArtworkDTO>(Arrays.asList(artwork)));
             }
         }
         return artworkList;
     }
 
     public List<CountGeneric> getJobCount(List<String> requiredJobs) {
         LOG.info("getJobCount: Required Jobs: {}", (requiredJobs == null ? "all" : requiredJobs));
         SqlScalars sqlScalars = new SqlScalars();
 
         sqlScalars.addToSql("SELECT job AS item, COUNT(*) AS count");
         sqlScalars.addToSql("FROM  cast_crew");
         if (CollectionUtils.isNotEmpty(requiredJobs)) {
             sqlScalars.addToSql("WHERE job IN (:joblist)");
             sqlScalars.addParameters("joblist", requiredJobs);
         }
         sqlScalars.addToSql("GROUP BY job");
 
         sqlScalars.addScalar("item", StringType.INSTANCE);
         sqlScalars.addScalar("count", LongType.INSTANCE);
 
         return executeQueryWithTransform(CountGeneric.class, sqlScalars, null);
     }
 
     public void getSeriesInfo(ApiWrapperList<ApiSeriesInfoDTO> wrapper) {
         OptionsIdArtwork options = (OptionsIdArtwork) wrapper.getOptions();
         Long id = options.getId();
         LOG.info("Getting series information for seriesId '{}'", id);
 
         SqlScalars sqlScalars = new SqlScalars();
         sqlScalars.addToSql("SELECT s.id AS seriesId, title, start_year AS seriesYear");
         sqlScalars.addToSql("FROM series s");
         sqlScalars.addToSql("WHERE id=:id");
         sqlScalars.addToSql("ORDER BY id");
         sqlScalars.addParameters("id", options.getId());
 
         sqlScalars.addScalar("seriesId", LongType.INSTANCE);
         sqlScalars.addScalar("title", StringType.INSTANCE);
         sqlScalars.addScalar("seriesYear", IntegerType.INSTANCE);
 
         List<ApiSeriesInfoDTO> seriesResults = executeQueryWithTransform(ApiSeriesInfoDTO.class, sqlScalars, wrapper);
         LOG.debug("Found {} series for SeriesId '{}'", seriesResults.size(), id);
 
         for (ApiSeriesInfoDTO series : seriesResults) {
             if (options.hasDataItem(DataItem.ARTWORK)) {
                 Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForId(MetaDataType.SERIES, id, options.getArtworkTypes());
                 for (ApiArtworkDTO artwork : artworkList.get(id)) {
                     series.addArtwork(artwork);
                 }
             }
             series.setSeasonList(getSeasonInfo(options));
         }
         wrapper.setResults(seriesResults);
     }
 
     private List<ApiSeasonInfoDTO> getSeasonInfo(OptionsIdArtwork options) {
         Long seriesId = options.getId();
 
         LOG.debug("Getting season information for seriesId '{}'", seriesId);
         SqlScalars sqlScalars = new SqlScalars();
         sqlScalars.addToSql("SELECT s.series_id AS seriesId, s.id AS seasonId, s.season, title");
         sqlScalars.addToSql("FROM season s");
         sqlScalars.addToSql("WHERE series_id=:id");
         sqlScalars.addParameters("id", seriesId);
 
         sqlScalars.addScalar("seriesId", LongType.INSTANCE);
         sqlScalars.addScalar("seasonId", LongType.INSTANCE);
         sqlScalars.addScalar("season", IntegerType.INSTANCE);
         sqlScalars.addScalar("title", StringType.INSTANCE);
 
         List<ApiSeasonInfoDTO> seasonResults = executeQueryWithTransform(ApiSeasonInfoDTO.class, sqlScalars, null);
         LOG.debug("Found {} seasons for SeriesId '{}'", seasonResults.size(), seriesId);
 
         if (options.hasDataItem(DataItem.ARTWORK)) {
             for (ApiSeasonInfoDTO season : seasonResults) {
                 Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForId(MetaDataType.SEASON, seriesId, options.getArtworkTypes());
                 for (ApiArtworkDTO artwork : artworkList.get(seriesId)) {
                     season.addArtwork(artwork);
                 }
             }
         }
 
         return seasonResults;
     }
 }
