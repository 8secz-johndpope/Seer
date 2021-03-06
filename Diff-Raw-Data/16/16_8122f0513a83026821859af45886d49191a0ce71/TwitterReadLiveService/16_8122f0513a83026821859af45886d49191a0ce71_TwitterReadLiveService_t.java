 package org.tweet.twitter.service.live;
 
 import java.util.List;
 import java.util.Set;
 
 import org.common.metrics.MetricsUtil;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.context.annotation.Profile;
 import org.springframework.social.InternalServerErrorException;
 import org.springframework.social.ResourceNotFoundException;
 import org.springframework.social.SocialException;
 import org.springframework.social.twitter.api.CursoredList;
 import org.springframework.social.twitter.api.FriendOperations;
 import org.springframework.social.twitter.api.SearchParameters;
 import org.springframework.social.twitter.api.SearchParameters.ResultType;
 import org.springframework.social.twitter.api.SearchResults;
 import org.springframework.social.twitter.api.TimelineOperations;
 import org.springframework.social.twitter.api.Tweet;
 import org.springframework.social.twitter.api.Twitter;
 import org.springframework.social.twitter.api.TwitterProfile;
 import org.springframework.stereotype.Service;
 import org.stackexchange.util.GenericUtil;
 import org.stackexchange.util.TwitterAccountEnum;
 import org.tweet.spring.util.SpringProfileUtil;
 import org.tweet.twitter.service.TwitterTemplateCreator;
 
 import com.codahale.metrics.MetricRegistry;
 import com.google.api.client.util.Preconditions;
 import com.google.common.base.Function;
 import com.google.common.collect.Lists;
 import com.google.common.collect.Sets;
 
 @Service
 @Profile(SpringProfileUtil.LIVE)
 public class TwitterReadLiveService {
     private final Logger logger = LoggerFactory.getLogger(getClass());
 
     @Autowired
     private TwitterTemplateCreator twitterCreator;
 
     @Autowired
     private MetricRegistry metrics;
 
     public TwitterReadLiveService() {
         super();
     }
 
     // API
 
     // user profiles
 
     /**
      * - note: will NOT return null
      */
     public TwitterProfile getProfileOfUser(final String userHandle) {
         try {
             return getProfileOfUserInternal(userHandle);
         } catch (final SocialException socialEx) {
             metrics.counter(MetricsUtil.Meta.TWITTER_READ_ERR).inc();
 
             final Throwable cause = socialEx.getCause();
             if (cause != null && cause instanceof InternalServerErrorException) {
                 // keep at warn or below - no need to know when this happens all the time
                 logger.warn("Known reason - Unable to retrieve profile of user: " + userHandle, socialEx);
                 return null;
             }
             if (socialEx instanceof ResourceNotFoundException) {
                 // keep at warn or below - no need to know when this happens all the time
                 logger.warn("Known reason - User no longer exists: " + userHandle, socialEx);
                 return null;
             }
 
             logger.error("Unable to retrieve profile of user: " + userHandle, socialEx);
             return null;
         } catch (final RuntimeException ex) {
             metrics.counter(MetricsUtil.Meta.TWITTER_READ_ERR).inc();
             logger.error("Unable to retrieve profile of user: " + userHandle, ex);
             return null;
         }
     }
 
     private final TwitterProfile getProfileOfUserInternal(final String userHandle) {
         final String randomAccount = GenericUtil.pickOneGeneric(TwitterAccountEnum.values()).name();
         final Twitter readOnlyTwitterTemplate = twitterCreator.createTwitterTemplate(randomAccount);
 
         final TwitterProfile userProfile = readOnlyTwitterTemplate.userOperations().getUserProfile(userHandle);
         metrics.counter(MetricsUtil.Meta.TWITTER_READ_OK).inc();
 
         return Preconditions.checkNotNull(userProfile);
     }
 
     // by internal accounts
 
     /**
      * - note: will NOT return null
      */
     public List<String> listTweetsOfInternalAccount(final String twitterAccount) {
         try {
             return listTweetsOfInternalAccountInternal(twitterAccount);
         } catch (final RuntimeException ex) {
             metrics.counter(MetricsUtil.Meta.TWITTER_READ_ERR).inc();
             logger.error("Unable to list tweets on twitterAccount= " + twitterAccount, ex);
             return Lists.newArrayList();
         }
     }
 
     /**
      * - note: will NOT return null
      */
     private final List<String> listTweetsOfInternalAccountInternal(final String twitterAccount) {
         return listTweetsOfInternalAccount(twitterAccount, 20);
     }
 
     /**
      * - note: will NOT return null
      */
     public List<String> listTweetsOfInternalAccount(final String twitterAccount, final int howmany) {
         try {
             return listTweetsOfInternalAccountInternal(twitterAccount, howmany);
         } catch (final RuntimeException ex) {
             metrics.counter(MetricsUtil.Meta.TWITTER_READ_ERR).inc();
             logger.error("Unable to list tweets on twitterAccount= " + twitterAccount, ex);
             return Lists.newArrayList();
         }
     }
 
     /**
      * - note: will NOT return null
      */
     private final List<String> listTweetsOfInternalAccountInternal(final String twitterAccount, final int howmany) {
         final List<Tweet> userTimeline = listTweetsOfInternalAccountRaw(twitterAccount, howmany);
         return Lists.transform(userTimeline, new TweetToStringFunction());
     }
 
     public List<Tweet> listTweetsOfInternalAccountRaw(final String twitterAccount, final int howmany) {
         try {
             return listTweetsOfInternalAccountRawInternal(twitterAccount, howmany);
         } catch (final RuntimeException ex) {
             metrics.counter(MetricsUtil.Meta.TWITTER_READ_ERR).inc();
             logger.error("Unable to list tweets on twitterAccount= " + twitterAccount, ex);
             return Lists.newArrayList();
         }
     }
 
     private final List<Tweet> listTweetsOfInternalAccountRawInternal(final String twitterAccount, final int howmany) {
         final Twitter twitterTemplate = twitterCreator.createTwitterTemplate(twitterAccount);
 
         final List<Tweet> userTimeline = twitterTemplate.timelineOperations().getUserTimeline(howmany);
         metrics.counter(MetricsUtil.Meta.TWITTER_READ_OK).inc();
 
         return userTimeline;
     }
 
     // by external accounts
 
     public List<String> listTweetsOfAccount(final String twitterAccount, final int howmany) {
         try {
             return listTweetsOfAccountInternal(twitterAccount, howmany);
         } catch (final RuntimeException ex) {
             metrics.counter(MetricsUtil.Meta.TWITTER_READ_ERR).inc();
             logger.error("Unable to list tweets on twitterAccount= " + twitterAccount, ex);
             return Lists.newArrayList();
         }
     }
 
     public List<Tweet> listTweetsOfAccountRaw(final String twitterAccount, final int howmany) {
         try {
             return listTweetsOfAccountRawInternal(twitterAccount, howmany);
         } catch (final RuntimeException ex) {
             metrics.counter(MetricsUtil.Meta.TWITTER_READ_ERR).inc();
             logger.error("Unable to list tweets on twitterAccount= " + twitterAccount, ex);
             return Lists.newArrayList();
         }
     }
 
     private final List<String> listTweetsOfAccountInternal(final String twitterAccount, final int howmany) {
         final List<Tweet> rawTweets = listTweetsOfAccountRawInternal(twitterAccount, howmany);
         final Function<Tweet, String> tweetToStringFunction = new TweetToStringFunction();
         return Lists.transform(rawTweets, tweetToStringFunction);
     }
 
     private final List<Tweet> listTweetsOfAccountRawInternal(final String twitterAccount, final int howmany) {
         final String randomAccount = GenericUtil.pickOneGeneric(TwitterAccountEnum.values()).name();
         final Twitter readOnlyTwitterTemplate = twitterCreator.createTwitterTemplate(randomAccount);
 
         final List<Tweet> userTimeline = readOnlyTwitterTemplate.timelineOperations().getUserTimeline(twitterAccount, howmany);
         metrics.counter(MetricsUtil.Meta.TWITTER_READ_OK).inc();
 
         return userTimeline;
     }
 
     // multi-request
 
     public List<Tweet> listTweetsOfAccountMultiRequestRaw(final String twitterAccount, final int howManyPages) {
         try {
             return listTweetsOfAccountMultiRequestRawInternal(twitterAccount, howManyPages);
         } catch (final RuntimeException ex) {
             metrics.counter(MetricsUtil.Meta.TWITTER_READ_ERR).inc();
             logger.error("Unable to list tweets on twitterAccount= " + twitterAccount, ex);
             return Lists.newArrayList();
         }
     }
 
     private final List<Tweet> listTweetsOfAccountMultiRequestRawInternal(final String twitterAccount, final int howManyPages) {
         if (howManyPages <= 1) {
             return listTweetsOfAccountRawInternal(twitterAccount, 200);
         }
         if (howManyPages > 20) {
             throw new IllegalStateException();
         }
 
         final int reqCount = howManyPages;
         int pageIndex = reqCount;
 
         final String randomAccount = GenericUtil.pickOneGeneric(TwitterAccountEnum.values()).name();
         final Twitter readOnlyTwitterTemplate = twitterCreator.createTwitterTemplate(randomAccount);
         final TimelineOperations timelineOperations = readOnlyTwitterTemplate.timelineOperations();
 
         final List<Tweet> collector = Lists.newArrayList();
         List<Tweet> currentPage = timelineOperations.getUserTimeline(twitterAccount, 200);
         metrics.counter(MetricsUtil.Meta.TWITTER_READ_OK).inc();
 
         collector.addAll(currentPage);
         if (collector.size() < 200) {
             // done - under 200 tweets (1 page)
             return collector;
         }
         long lastId = currentPage.get(currentPage.size() - 1).getId();
         while (pageIndex > 1) {
             currentPage = timelineOperations.getUserTimeline(twitterAccount, 200, 01, lastId);
             metrics.counter(MetricsUtil.Meta.TWITTER_READ_OK).inc();
 
             collector.addAll(currentPage);
            if (currentPage.isEmpty()) {
                logger.error("This should not happen but weirdly does when retrieving {} pages for twitterAccount= {}", howManyPages, twitterAccount);
                return collector;
            }
             lastId = currentPage.get(currentPage.size() - 1).getId();
             pageIndex--;
         }
 
         return collector;
     }
 
     // by hashtag
 
     public List<Tweet> listTweetsOfHashtag(final String hashtag) {
         final String randomAccount = GenericUtil.pickOneGeneric(TwitterAccountEnum.values()).name();
         return listTweetsOfHashtag(randomAccount, hashtag);
     }
 
     public List<Tweet> listTweetsOfHashtag(final String readOnlyAccountName, final String hashtag) {
         final Twitter twitterTemplate = twitterCreator.createTwitterTemplate(readOnlyAccountName);
 
         // ruby_rails
         final StringBuilder param = new StringBuilder();
         if (hashtag.contains("_")) {
             final String[] hashtags = hashtag.split("_");
             for (final String hashtagIndividual : hashtags) {
                 param.append("#").append(hashtagIndividual).append(" ");
             }
         } else {
             param.append("#").append(hashtag);
         }
 
         final SearchParameters searchParameters = new SearchParameters(param.toString().trim()).lang("en").count(100).includeEntities(true).resultType(ResultType.MIXED);
         final SearchResults search = twitterTemplate.searchOperations().search(searchParameters);
         metrics.counter(MetricsUtil.Meta.TWITTER_READ_OK).inc();
 
         return search.getTweets();
     }
 
     // single one
 
     public Tweet findOne(final long id) {
         return readOnlyTwitterApi().timelineOperations().getStatus(id);
     }
 
     // friends
 
     public Set<Long> getFriendIds(final TwitterProfile account, final int maxPages) {
         final Set<Long> fullListOfFriends = Sets.newHashSet();
         final FriendOperations friendOperations = readOnlyTwitterApi().friendOperations();
         final String screenName = account.getScreenName();
 
         CursoredList<Long> currentPage = friendOperations.getFriendIds(screenName);
         fullListOfFriends.addAll(currentPage);
 
         final int maxNecessaryPages = (account.getFriendsCount() / 5000) + 1;
         final int maxActualPages = Math.min(maxNecessaryPages, maxPages) - 1;
         for (int i = 0; i < maxActualPages; i++) {
             final long nextCursor = currentPage.getNextCursor();
             currentPage = friendOperations.getFriendIdsInCursor(screenName, nextCursor);
             fullListOfFriends.addAll(currentPage);
         }
 
         return fullListOfFriends;
     }
 
     // internal API
 
     public final Twitter readOnlyTwitterApi() {
         final String randomAccount = GenericUtil.pickOneGeneric(TwitterAccountEnum.values()).name();
         final Twitter readOnlyTwitterTemplate = twitterCreator.createTwitterTemplate(randomAccount);
         return readOnlyTwitterTemplate;
     }
 
     public final Twitter readOnlyTwitterApi(final String twitterAccount) {
         final Twitter readOnlyTwitterTemplate = twitterCreator.createTwitterTemplate(twitterAccount);
         return readOnlyTwitterTemplate;
     }
 
 }
