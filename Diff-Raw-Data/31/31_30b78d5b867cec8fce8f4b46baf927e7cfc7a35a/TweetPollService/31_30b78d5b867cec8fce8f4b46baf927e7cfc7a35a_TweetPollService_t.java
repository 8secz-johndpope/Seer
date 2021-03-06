 /*
  ************************************************************************************
  * Copyright (C) 2001-2009 encuestame: system online surveys Copyright (C) 2009
  * encuestame Development Team.
  * Licensed under the Apache Software License version 2.0
  * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to  in writing,  software  distributed
  * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
  * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
  * specific language governing permissions and limitations under the License.
  ************************************************************************************
  */
 package org.encuestame.business.service;
 
 import java.io.UnsupportedEncodingException;
 import java.security.NoSuchAlgorithmException;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.List;
 import java.util.Set;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.encuestame.core.exception.EnMeFailSendSocialTweetException;
 import org.encuestame.core.service.imp.ITweetPollService;
 import org.encuestame.core.util.ConvertDomainBean;
 import org.encuestame.core.util.EnMeUtils;
 import org.encuestame.core.util.SocialUtils;
 import org.encuestame.persistence.domain.HashTag;
 import org.encuestame.persistence.domain.notifications.NotificationEnum;
 import org.encuestame.persistence.domain.question.Question;
 import org.encuestame.persistence.domain.question.QuestionAnswer;
 import org.encuestame.persistence.domain.security.Account;
 import org.encuestame.persistence.domain.security.SocialAccount;
 import org.encuestame.persistence.domain.security.UserAccount;
 import org.encuestame.persistence.domain.tweetpoll.Status;
 import org.encuestame.persistence.domain.tweetpoll.TweetPoll;
 import org.encuestame.persistence.domain.tweetpoll.TweetPollFolder;
 import org.encuestame.persistence.domain.tweetpoll.TweetPollResult;
 import org.encuestame.persistence.domain.tweetpoll.TweetPollSavedPublishedStatus;
 import org.encuestame.persistence.domain.tweetpoll.TweetPollSwitch;
 import org.encuestame.persistence.exception.EnMeExpcetion;
 import org.encuestame.persistence.exception.EnMeNoResultsFoundException;
 import org.encuestame.persistence.exception.EnMeTweetPollNotFoundException;
 import org.encuestame.persistence.exception.EnmeFailOperation;
 import org.encuestame.utils.RestFullUtil;
 import org.encuestame.utils.TweetPublishedMetadata;
 import org.encuestame.utils.json.FolderBean;
 import org.encuestame.utils.json.LinksSocialBean;
 import org.encuestame.utils.json.QuestionBean;
 import org.encuestame.utils.json.SocialAccountBean;
 import org.encuestame.utils.json.TweetPollAnswerSwitchBean;
 import org.encuestame.utils.json.TweetPollBean;
 import org.encuestame.utils.web.QuestionAnswerBean;
 import org.encuestame.utils.web.TweetPollResultsBean;
 import org.encuestame.utils.web.UnitTweetPollResult;
 import org.joda.time.DateTime;
 import org.springframework.stereotype.Service;
 import org.springframework.util.Assert;
 
 import twitter4j.TwitterException;
 
 /**
  * {@link TweetPoll} service support.
  * @author Morales, Diana Paola paola AT encuestame.org
  * @since  April 02, 2010
  */
 @Service
 public class TweetPollService extends AbstractSurveyService implements ITweetPollService{
 
     /**
      * Log.
      */
     private Log log = LogFactory.getLog(this.getClass());
 
     /**
      * Default votes.
      */
     private final Long TOTALVOTE = 0L ;
 
     /**
      * Get Tweet Polls by User Id.
      * @param username username.
      * @return list of Tweet polls bean
      * @throws EnMeNoResultsFoundException
      */
     public List<TweetPollBean> getTweetsPollsByUserName(final String username,
             final Integer maxResults,
             final Integer start) throws EnMeNoResultsFoundException{
         log.debug("tweetPoll username "+username);
         final List<TweetPoll> tweetPolls = getTweetPollDao().retrieveTweetsByUserId(getPrimaryUser(username), maxResults, start);
          log.info("tweetPoll size "+tweetPolls.size());
         return this.setTweetPollListAnswers(tweetPolls, Boolean.TRUE);
     }
 
     /**
      * Set List Answer.
      * @param listTweetPolls List of {@link TweetPoll}
      * @return
      * @throws EnMeExpcetion
      */
     private List<TweetPollBean> setTweetPollListAnswers(final List<TweetPoll> listTweetPolls, final Boolean results){
         final List<TweetPollBean> tweetPollsBean = new ArrayList<TweetPollBean>();
         for (TweetPoll tweetPoll : listTweetPolls) {
              final List<TweetPollSwitch> answers = this.getTweetPollSwitch(tweetPoll);
              final TweetPollBean unitTweetPoll = ConvertDomainBean.convertTweetPollToBean(tweetPoll);
              final List<TweetPollAnswerSwitchBean> listSwitchs = new ArrayList<TweetPollAnswerSwitchBean>();
              //);
              if (results) {
                  final List<TweetPollResultsBean> list = new ArrayList<TweetPollResultsBean>();
                  for (TweetPollSwitch tweetPollSwitch : answers) {
                      final TweetPollAnswerSwitchBean answerResults = ConvertDomainBean.convertTweetPollSwitchToBean(tweetPollSwitch);
                      final TweetPollResultsBean rBean = this.getVotesByTweetPollAnswerId(tweetPoll.getTweetPollId(), tweetPollSwitch.getAnswers());
                      answerResults.setResultsBean(rBean);
                      list.add(rBean);
                      listSwitchs.add(answerResults);
                 }
                  this.calculatePercents(list);
              }
              unitTweetPoll.setAnswerSwitchBeans(listSwitchs);
              tweetPollsBean.add(unitTweetPoll);
         }
         return tweetPollsBean;
     }
 
     /**
      * Search {@link TweetPoll} by Keyword.
      * @param username username session
      * @param keyword keyword.
      * @return
      * @throws EnMeExpcetion
      */
     public List<TweetPollBean> searchTweetsPollsByKeyWord(
                                final String username,
                                final String keyword,
                                final Integer maxResults,
                                final Integer start) throws EnMeExpcetion{
         log.info("search keyword tweetPoll  "+keyword);
         List<TweetPoll> tweetPolls  = new ArrayList<TweetPoll>();
         if(keyword == null){
            throw new EnMeExpcetion("keyword is missing");
         } else {
             tweetPolls = getTweetPollDao().retrieveTweetsByQuestionName(keyword, getPrimaryUser(username), maxResults, start);
         }
         log.info("search keyword tweetPoll size "+tweetPolls.size());
         return this.setTweetPollListAnswers(tweetPolls, Boolean.TRUE);
     }
 
     /**
      * Search Tweet Polls Today.
      * @param username
      * @param keyword
      * @param maxResults
      * @param start
      * @return
      * @throws EnMeExpcetion
      */
     public List<TweetPollBean> searchTweetsPollsToday(final String username,
             final Integer maxResults, final Integer start) throws EnMeExpcetion{
         return this.setTweetPollListAnswers(getTweetPollDao().retrieveTweetPollToday(
                 getPrimaryUser(username), maxResults, start), Boolean.TRUE);
     }
 
     /**
      * Search Tweet Polls Last Week.
      * @param username
      * @param keyword
      * @param maxResults
      * @param start
      * @return
      * @throws EnMeExpcetion
      */
     public List<TweetPollBean> searchTweetsPollsLastWeek(final String username,
             final Integer maxResults, final Integer start) throws EnMeExpcetion{
         return this.setTweetPollListAnswers(getTweetPollDao().retrieveTweetPollLastWeek(
                 getPrimaryUser(username), maxResults, start), Boolean.TRUE);
     }
 
     /**
      * Search Favourites TweetPolls.
      * @param username
      * @param keyword
      * @param maxResults
      * @param start
      * @return
      * @throws EnMeExpcetion
      */
     public List<TweetPollBean> searchTweetsPollFavourites(final String username,
             final Integer maxResults, final Integer start) throws EnMeExpcetion{
         return this.setTweetPollListAnswers(getTweetPollDao().retrieveFavouritesTweetPoll(
                 getPrimaryUser(username), maxResults, start), Boolean.TRUE);
     }
 
     /**
      * Search Scheduled TweetsPoll.
      * @param username
      * @param keyword
      * @param maxResults
      * @param start
      * @return
      * @throws EnMeExpcetion
      */
     public List<TweetPollBean> searchTweetsPollScheduled(final String username,
             final Integer maxResults, final Integer start) throws EnMeExpcetion{
         return this.setTweetPollListAnswers(getTweetPollDao().retrieveScheduledTweetPoll(
                 getPrimaryUser(username), maxResults, start), Boolean.TRUE);
     }
 
     /**
      * Create tweetPoll.
      * @param tweetPollBean {@link TweetPollBean}
      * @param question {@link Question}.
      * @return
      */
     private TweetPoll newTweetPoll(final TweetPollBean tweetPollBean, Question question){
         final TweetPoll tweetPollDomain = new TweetPoll();
         log.debug(tweetPollBean.toString());
         tweetPollDomain.setQuestion(question);
         tweetPollDomain.setCloseNotification(tweetPollBean.getCloseNotification());
         tweetPollDomain.setCompleted(Boolean.FALSE);
         tweetPollDomain.setCaptcha(tweetPollBean.getCaptcha());
         tweetPollDomain.setAllowLiveResults(tweetPollBean.getAllowLiveResults());
         tweetPollDomain.setLimitVotes(tweetPollBean.getLimitVotes());
         tweetPollDomain.setTweetOwner(getAccountDao().getUserById(tweetPollBean.getUserId()));
         tweetPollDomain.setEditorOwner(getUserAccountonSecurityContext());
         tweetPollDomain.setResultNotification(tweetPollBean.getResultNotification());
         tweetPollDomain.setPublishTweetPoll(Boolean.FALSE);
         tweetPollDomain.setCreateDate(Calendar.getInstance().getTime());
         tweetPollDomain.setAllowLiveResults(tweetPollBean.getAllowLiveResults());
         tweetPollDomain.setScheduleTweetPoll(tweetPollBean.getSchedule());
         tweetPollDomain.setScheduleDate(tweetPollBean.getScheduleDate());
         tweetPollDomain.setUpdatedDate(Calendar.getInstance().getTime());
         tweetPollDomain.setDateLimit((tweetPollBean.getLimitVotesDate() == null ? true : tweetPollBean.getLimitVotesDate()));
         final Calendar limit = Calendar.getInstance();
         limit.add(Calendar.DAY_OF_YEAR, 7); //TODO: define when is the default limit for each tweetpoll.
         tweetPollDomain.setDateLimited(limit.getTime());
         this.getTweetPollDao().saveOrUpdate(tweetPollDomain);
         return tweetPollDomain;
     }
 
     /**
      * Create new question with answers.
      * @param questionName
      * @param answers
      * @param user
      * @return
      * @throws EnMeExpcetion
      * @throws UnsupportedEncodingException
      * @throws NoSuchAlgorithmException
      */
     public Question createTweetPollQuestion(
             final String questionName,
             final UserAccount user) throws EnMeExpcetion, NoSuchAlgorithmException, UnsupportedEncodingException{
         final QuestionBean questionBean = new QuestionBean();
         questionBean.setQuestionName(questionName);
         questionBean.setUserId(user.getUid());
         final Question questionDomain = createQuestion(questionBean, user);
         return questionDomain;
     }
 
     /*
      * (non-Javadoc)
      * @see org.encuestame.business.service.imp.ITweetPollService#createTweetPoll(org.encuestame.utils.web.TweetPollBean, java.lang.String, java.lang.String[], java.lang.String[], org.encuestame.persistence.domain.security.UserAccount)
      */
     public TweetPoll createTweetPoll(
             final TweetPollBean tweetPollBean,
             final String questionName,
             final UserAccount user) throws EnMeExpcetion {
         try{
             final Question question = createTweetPollQuestion(questionName, user);
             log.debug("question found:{"+question);
             if (question == null) {
                 throw new EnMeNoResultsFoundException("question not found");
             } else {
                 final TweetPoll tweetPollDomain = newTweetPoll(tweetPollBean, question);
                 //save Hash tags for this tweetPoll.
                 log.debug("HashTag size:{"+tweetPollBean.getHashTags().size());
                 //update TweetPoll.
                 if (tweetPollBean.getHashTags().size() > 0) {
                     tweetPollDomain.getHashTags().addAll(retrieveListOfHashTags(tweetPollBean.getHashTags()));
                     log.debug("Update Hash Tag");
                     getTweetPollDao().saveOrUpdate(tweetPollDomain);
                 }
                 //update tweetpoll switch support
                 this.updateTweetPollSwitchSupport(tweetPollDomain);
                 return tweetPollDomain;
             }
         } catch (Exception e) {
             log.error("Error creating TweetlPoll:{"+e);
             throw new EnMeExpcetion(e);
         }
     }
 
     /*
      * (non-Javadoc)
      * @see org.encuestame.business.service.imp.ITweetPollService#updateTweetPoll(org.encuestame.persistence.domain.tweetpoll.TweetPoll, java.lang.String[], java.util.List)
      */
     public TweetPoll updateTweetPoll(final TweetPollBean tweetPollBean) throws EnMeNoResultsFoundException {
         //updating hashtags
         log.debug("Updated tweetpoll with id :"+tweetPollBean.getId());
         final TweetPoll tweetPoll = getTweetPoll(tweetPollBean.getId(), getUserPrincipalUsername());
         Assert.notNull(tweetPoll);
         if (tweetPoll == null) {
             throw new EnMeTweetPollNotFoundException();
         }
         final List<HashTag> newList = retrieveListOfHashTags(tweetPollBean.getHashTags());
         log.debug("new list of hashtags size: "+newList.size());
         //update question name.
         final Question questionDomain = tweetPoll.getQuestion();
         Assert.notNull(questionDomain);
         questionDomain.setQuestion(tweetPollBean.getQuestionBean().getQuestionName());
         questionDomain.setSlugQuestion(RestFullUtil.slugify(tweetPollBean.getQuestionBean().getQuestionName()));
         questionDomain.setCreateDate(Calendar.getInstance().getTime());
         getQuestionDao().saveOrUpdate(questionDomain);
         //update hashtags.
         tweetPoll.getHashTags().addAll(retrieveListOfHashTags(tweetPollBean.getHashTags())); //TODO check if this action remove old hashtags.
         //update options.
         tweetPoll.setAllowLiveResults(tweetPollBean.getAllowLiveResults());
         tweetPoll.setAllowRepatedVotes(tweetPollBean.getAllowRepeatedVotes());
         tweetPoll.setCaptcha(tweetPollBean.getCaptcha());
         tweetPoll.setCloseNotification(tweetPollBean.getCloseNotification());
         tweetPoll.setLimitVotes(tweetPollBean.getLimitVotes());
         tweetPoll.setLimitVotesEnabled(tweetPollBean.getLimitVotesEnabled());
         tweetPoll.setMaxRepeatedVotes(tweetPollBean.getMaxRepeatedVotes());
         tweetPoll.setResultNotification(tweetPollBean.getResultNotification());
         tweetPoll.setResumeLiveResults(tweetPollBean.getResumeLiveResults());
         tweetPoll.setScheduleDate(tweetPollBean.getScheduleDate());
         tweetPoll.setResumeTweetPollDashBoard(tweetPollBean.getResumeTweetPollDashBoard());
         tweetPoll.setUpdatedDate(Calendar.getInstance().getTime());
         getTweetPollDao().saveOrUpdate(tweetPoll);
         log.debug("removing answers for tweetpoll id: "+tweetPoll.getTweetPollId());
 
         /*
          * answer auto-save handler.
          */
         //no make sense remove all questions. disabled.
         //this.removeAllQuestionsAnswers(tweetPoll);
 
         //create new answers.
 //        for (int i = 0; i < answers.length; i++) {
 //            log.debug("Creating new answer:{ "+answers[i].toString());
 //            log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
 //            //createQuestionAnswer(new QuestionAnswerBean(answers[i]), tweetPoll.getQuestion());
 //            log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
 //        }
         log.debug("update switchs question.");
         //update switchs question..
         //updateTweetPollSwitchSupport(tweetPoll);
         return tweetPoll;
     }
 
     /*
      * (non-Javadoc)
      * @see org.encuestame.business.service.imp.ITweetPollService#removeAllQuestionsAnswers(org.encuestame.persistence.domain.tweetpoll.TweetPoll)
      */
     public void removeAllQuestionsAnswers(final TweetPoll tweetPoll){
         final Question question = tweetPoll.getQuestion();
         final Set<QuestionAnswer> currentQuestionAnswers = question.getQuestionsAnswers();
         //removing old answers.
         for (QuestionAnswer questionAnswer : currentQuestionAnswers) {
             this.removeQuestionAnswer(questionAnswer);
         }
         getQuestionDao().saveOrUpdate(question);
     }
 
     /*
      * (non-Javadoc)
      * @see org.encuestame.business.service.imp.ITweetPollService#removeQuestionAnswer(org.encuestame.persistence.domain.question.QuestionAnswer)
      */
     public void removeQuestionAnswer(final QuestionAnswer questionAnswer){
         //removing old data.
         final List<TweetPollSwitch> list = getTweetPollDao().getAnswerTweetSwitch(questionAnswer);
         log.debug("removeQuestionAnswer switch size:"+list.size());
         for (TweetPollSwitch tweetPollSwitch : list) {
              getTweetPollDao().delete(tweetPollSwitch);
         }
         log.debug("removing answer:{"+questionAnswer.getQuestionAnswerId());
         getQuestionDao().delete(questionAnswer); //remove answer.
     }
 
     /*
      * (non-Javadoc)
      * @see org.encuestame.business.service.imp.ITweetPollService#createTweetPollQuestionAnswer(org.encuestame.utils.web.QuestionAnswerBean, org.encuestame.persistence.domain.tweetpoll.TweetPoll)
      */
     public TweetPollSwitch createTweetPollQuestionAnswer(
             final QuestionAnswerBean answerBean,
             final TweetPoll tp)
             throws EnMeNoResultsFoundException {
         final Question question = tp.getQuestion();
         //create answer
         final QuestionAnswer questionAnswer = createQuestionAnswer(answerBean,
                 question);
         if (questionAnswer == null) {
             throw new EnMeNoResultsFoundException("answer is missing");
         } else {
             //create tweet poll switch with tp and new answer.
             log.debug("createTweetPollQuestionAnswer: short url provider:{ "+questionAnswer.getProvider());
             final TweetPollSwitch tpSwitch = this.createTweetPollSwitch(tp, questionAnswer);
             return tpSwitch;
         }
     }
 
    /*
     * (non-Javadoc)
     * @see org.encuestame.business.service.imp.ITweetPollService#getTweetPollSwitch(org.encuestame.persistence.domain.tweetpoll.TweetPoll)
     */
     public List<TweetPollSwitch> getTweetPollSwitch(final TweetPoll tweetPoll){
         return getTweetPollDao().getListAnswesByTweetPoll(tweetPoll);
     }
 
     /**
      * Generate TweetPoll Text.
      * @param tweetPoll tweetPoll
      * @param url url
      * @return tweet text
      * @throws EnMeExpcetion exception
      */
     public String generateTweetPollContent(final TweetPoll tweetPollDomain) throws EnMeExpcetion{
         String tweetQuestionText = "";
         try{
             log.debug("generateTweetPollText");
             log.debug("TweetPoll ID: "+tweetPollDomain.getTweetPollId());
             tweetQuestionText = tweetPollDomain.getQuestion().getQuestion();
             log.debug("Question text: "+tweetQuestionText);
             final List<TweetPollSwitch> tweetPollSwitchs = getTweetPollSwitch(tweetPollDomain);
             log.debug("generateTweetPollText tweetPollSwitchs:{ "+tweetPollSwitchs.size());
             final StringBuilder builder = new StringBuilder(tweetQuestionText);
             //
             for (final TweetPollSwitch tpswitch : tweetPollSwitchs) {
                  final QuestionAnswer questionsAnswers = tpswitch.getAnswers();
                  log.debug("Answer ID: "+questionsAnswers.getQuestionAnswerId());
                  log.debug("Answer Question: "+questionsAnswers.getAnswer());
                  builder.append(" ");
                  builder.append(questionsAnswers.getAnswer());
                  builder.append(" ");
                  builder.append(tpswitch.getShortUrl());
             }
             //Build Hash Tag.
             for (final HashTag tag : tweetPollDomain.getHashTags()) {
                  log.debug("Hash Tag ID: "+tag.getHashTagId());
                  log.debug("Tag Name "+tag.getHashTag());
                  builder.append(" ");
                  builder.append("#");
                  builder.append(tag.getHashTag());
             }
             tweetQuestionText = builder.toString();
         } catch (Exception e) {
             throw new EnMeExpcetion(e);
         }
         log.debug("Tweet Text Generated: "+tweetQuestionText);
         log.debug("Tweet Text Generated: "+tweetQuestionText.length());
         return tweetQuestionText;
     }
 
     /*
      * (non-Javadoc)
      * @see org.encuestame.business.service.imp.ITweetPollService#publishMultiplesOnSocialAccounts(java.util.List, java.lang.Long, java.lang.String)
      */
     public List<TweetPollSavedPublishedStatus> publishMultiplesOnSocialAccounts(
             final List<SocialAccountBean> twitterAccounts,
             final TweetPoll tweetPoll,
             final String tweetText){
             log.debug("publicMultiplesTweetAccounts:{"+twitterAccounts.size());
             final List<TweetPollSavedPublishedStatus> results = new ArrayList<TweetPollSavedPublishedStatus>();
             for (SocialAccountBean unitTwitterAccountBean : twitterAccounts) {
                 log.debug("publicMultiplesTweetAccounts unitTwitterAccountBean:{ "+unitTwitterAccountBean.toString());
                 results.add(this.publishTweetBySocialAccountId(unitTwitterAccountBean.getAccountId(), tweetPoll, tweetText));
             }
             return results;
     }
 
     /**
      *
      * @param tweetPoll
      * @throws EnMeNoResultsFoundException
      */
     public void createTweetPollNotification(final TweetPoll tweetPoll) throws EnMeNoResultsFoundException {
         createNotification(NotificationEnum.TWEETPOLL_PUBLISHED,
                 getMessageProperties("notification.tweetpoll.created"),
                 this.createTweetPollUrlAccess(tweetPoll), false);
     }
 
 
     /**
      * Create url to acces to tweetPoll.
      * format tweetpoll/932/test
      * @param tweetPoll
      * @return
      */
     private String createTweetPollUrlAccess(final TweetPoll tweetPoll){
         final StringBuilder builder = new StringBuilder("/tweetpoll/");
         builder.append(tweetPoll.getTweetPollId());
         builder.append("/");
         builder.append(tweetPoll.getQuestion().getSlugQuestion());
         return builder.toString();
     }
 
     /*
      * (non-Javadoc)
      * @see org.encuestame.business.service.imp.ITweetPollService#publishTweetPoll(java.lang.Long, org.encuestame.persistence.domain.tweetpoll.TweetPoll, org.encuestame.persistence.domain.social.SocialProvider)
      */
     public TweetPollSavedPublishedStatus publishTweetBySocialAccountId(
             final Long socialAccountId,
             final TweetPoll tweetPoll,
             final String tweetText) {
          log.debug("publicMultiplesTweetAccounts tweetPoll" + tweetPoll);
         //get social account
          final SocialAccount socialAccount = getAccountDao().getSocialAccountById(socialAccountId);
          log.debug("publishTweetPoll socialTwitterAccounts: {"+socialAccount);
          //create tweet status
          final TweetPollSavedPublishedStatus publishedStatus = new TweetPollSavedPublishedStatus();
          //social provider.
          publishedStatus.setApiType(socialAccount.getAccounType());
          //adding tweetpoll
          publishedStatus.setTweetPoll(tweetPoll);
          //checking required values.
          if (socialAccount != null && tweetPoll != null) {
              log.debug("socialAccount Account NAME:{"+socialAccount.getSocialAccountName());
              //adding social account
              publishedStatus.setSocialAccount(socialAccount);
              try {
                  log.debug("publishTweetPoll Publishing... "+tweetText.length());
                  final TweetPublishedMetadata metadata = publicTweetPoll(tweetText, socialAccount);
                  if (metadata == null) {
                      throw new EnMeFailSendSocialTweetException("status not valid");
                  }//getMessageProperties(propertieId)
                  if (metadata.getTweetId() == null) {
                      log.warn("tweet id is empty");
                  }
                  //store original tweet id.
                  publishedStatus.setTweetId(metadata.getTweetId());
                  //store original publication date.
                  publishedStatus.setPublicationDateTweet(metadata.getDatePublished());
                  //success publish state..
                  publishedStatus.setStatus(Status.SUCCESS);
                  //store original tweet content.
                  publishedStatus.setTweetContent(metadata.getTextTweeted());
                  //create notification
                  //createNotification(NotificationEnum.TWEETPOLL_PUBLISHED, "tweet published", socialAccount.getAccount());
                  createNotification(NotificationEnum.SOCIAL_MESSAGE_PUBLISHED,tweetText, SocialUtils.getSocialTweetPublishedUrl(
                          metadata.getTweetId(), socialAccount.getSocialAccountName(), socialAccount.getAccounType()), Boolean.TRUE);
              } catch (Exception e) {
                  log.error("Error publish tweet:{"+e);
                  e.printStackTrace();
                  //change status to failed
                  publishedStatus.setStatus(Status.FAILED);
                  //store error descrition
                  if (e.getMessage() != null && e.getMessage().isEmpty()) {
                      publishedStatus.setDescriptionStatus(e.getMessage().substring(254)); //limited to 254 characters.
                  } else {
                      publishedStatus.setDescriptionStatus("");
                  }
                  //save original tweet content.
                  publishedStatus.setTweetContent(tweetText);
              }
          } else {
              log.warn("Twitter Account Not Found [Id:"+socialAccountId+"]");
              publishedStatus.setStatus(Status.FAILED);
              //throw new EnMeFailSendSocialTweetException("Twitter Account Not Found [Id:"+accountId+"]");
              tweetPoll.setPublishTweetPoll(Boolean.FALSE);
              getTweetPollDao().saveOrUpdate(tweetPoll);
          }
          log.info("Publish Status Social :{------------>"+publishedStatus.toString());
          getTweetPollDao().saveOrUpdate(publishedStatus);
          return publishedStatus;
     }
 
     /**
      * Vote on TweetPoll.
      * @param pollSwitch {@link TweetPollSwitch}
      * @param ip ip
      */
     public void tweetPollVote(final TweetPollSwitch pollSwitch, final String ip){
         final TweetPollResult pollResult = new TweetPollResult();
         pollResult.setIpVote(ip.trim());
         pollResult.setTweetPollSwitch(pollSwitch);
         pollResult.setTweetResponseDate(new Date());
         getTweetPollDao().saveOrUpdate(pollResult);
     }
 
     /*
      * (non-Javadoc)
      * @see org.encuestame.business.service.imp.ITweetPollService#getTweetPollById(java.lang.Long, org.encuestame.persistence.domain.security.UserAccount)
      */
     public TweetPoll getTweetPollById(final Long tweetPollId) throws EnMeNoResultsFoundException{
         return this.getTweetPollById(tweetPollId, getUserPrincipalUsername());
     }
 
 
     /**
      *
      * @param tweetPollId
      * @param username
      * @param slug
      * @return
      * @throws EnMeNoResultsFoundException
      */
     public TweetPoll getTweetPollByIdSlugName(final Long tweetPollId, final String slug) throws EnMeNoResultsFoundException {
         TweetPoll tweetPoll;
         try {
                 tweetPoll = getTweetPollDao().getTweetPollByIdandSlugName(tweetPollId, slug);
             if (tweetPoll == null) {
                 log.error("tweet poll invalid with this id "+tweetPollId);
                 throw new EnMeTweetPollNotFoundException("tweet poll invalid with this id "+tweetPollId);
             }
         } catch (UnsupportedEncodingException e) {
             log.error(e);
             tweetPoll = null;
         }
         return tweetPoll;
     }
 
     /*
      *
      */
     public TweetPoll getTweetPollById(final Long tweetPollId, final String username) throws EnMeNoResultsFoundException {
         TweetPoll tweetPoll = null;
         if (username != null) {
             tweetPoll = getTweetPollDao()
                     .getTweetPollByIdandUserId(tweetPollId,
                             getUserAccount(username).getAccount().getUid());
         } else {
             tweetPoll = getTweetPollDao().getTweetPollById(tweetPollId);
         }
         if (tweetPoll == null) {
             log.error("tweet poll invalid with this id "+tweetPollId);
             throw new EnMeTweetPollNotFoundException("tweet poll invalid with this id "+tweetPollId);
         }
         return tweetPoll;
     }
 
     /**
      * Get Tweet Poll Folder by User and FolderId.
      * @param id folder id.
      * @throws EnMeNoResultsFoundException if username not exist.
      */
     private TweetPollFolder getTweetPollFolderByFolderId(final Long folderId) throws EnMeNoResultsFoundException{
         final TweetPollFolder folder = this.getTweetPollDao()
                 .getTweetPollFolderByIdandUser(folderId,
                         getUserAccount(getUserPrincipalUsername()).getAccount());
         if (folder == null) {
             throw new EnMeNoResultsFoundException("tweetpoll folder not valid");
         }
         return folder;
     }
 
     /**
      * Get TweetPoll.
      * @param tweetPollId
      * @param username
      * @return
      * @throws EnMeNoResultsFoundException
      */
     private TweetPoll getTweetPoll(final Long tweetPollId, final String username) throws EnMeNoResultsFoundException{
         return this.getTweetPollById(tweetPollId, username);
     }
 
     /**
      * Get Results By {@link TweetPoll}.
      * @param tweetPollId tweetPoll Id
      * @return list of {@link UnitTweetPollResult}
      * @throws EnMeNoResultsFoundException
      */
     public List<TweetPollResultsBean> getResultsByTweetPollId(final Long tweetPollId) throws EnMeNoResultsFoundException{
         if (log.isDebugEnabled()) {
             log.debug("getResultsByTweetPollId "+tweetPollId);
         }
         final List<TweetPollResultsBean> pollResults = new ArrayList<TweetPollResultsBean>();
         final TweetPoll tweetPoll = getTweetPollById(tweetPollId, null);
         if (log.isDebugEnabled()) {
             log.debug("Answers Size "+tweetPoll.getQuestion().getQuestionsAnswers().size());
             log.debug("tweetPoll "+tweetPoll);
         }
         for (QuestionAnswer questionsAnswer : getQuestionDao().getAnswersByQuestionId(tweetPoll.getQuestion().getQid())) {
               if (log.isDebugEnabled()) {
                   log.debug("Question Name "+tweetPoll.getQuestion().getQuestion());
               }
               pollResults.add(this.getVotesByTweetPollAnswerId(tweetPollId, questionsAnswer));
         }
         this.calculatePercents(pollResults);
         return pollResults;
     }
 
     /*
      * (non-Javadoc)
      * @see org.encuestame.business.service.imp.ITweetPollService#getTweetPollTotalVotes(org.encuestame.persistence.domain.tweetpoll.TweetPoll)
      */
     public Long getTweetPollTotalVotes(final TweetPoll tweetPoll) {
         long totalVotes = 0;
         for (QuestionAnswer questionsAnswers : getQuestionDao().getAnswersByQuestionId(tweetPoll.getQuestion().getQid())) {
             totalVotes += this.getVotesByTweetPollAnswerId(tweetPoll.getTweetPollId(), questionsAnswers).getVotes();
       }
         return totalVotes;
     }
 
     /**
      * Calculate tweetpoll votes percent.
      * @param list
      */
     private void calculatePercents(final List<TweetPollResultsBean> list) {
         double totalVotes = 0;
         for (TweetPollResultsBean tweetPollResultsBean : list) {
             totalVotes += totalVotes + tweetPollResultsBean.getVotes();
         }
         for (TweetPollResultsBean tweetPollResultsBean : list) {
             tweetPollResultsBean.setPercent(EnMeUtils.calculatePercent(totalVotes, tweetPollResultsBean.getVotes()));
         }
     }
 
     /**
      *
      * @param tweetPollId
      * @param answerId
      * @return
      */
     public TweetPollResultsBean getVotesByTweetPollAnswerId(final Long tweetPollId, final QuestionAnswer answer) {
         final List<Object[]> result = getTweetPollDao().getResultsByTweetPoll(tweetPollId, answer.getQuestionAnswerId());
         log.debug("result getResultsByTweetPollId- "+result.size());
         final TweetPollResultsBean tweetPollResult = new TweetPollResultsBean();
         tweetPollResult.setAnswerName(answer.getAnswer());
         if (result.size() == 0) {
             tweetPollResult.setAnswerId(answer.getQuestionAnswerId());
             tweetPollResult.setColor(answer.getColor());
             tweetPollResult.setVotes(0L);
         } else {
             for (Object[] objects : result) {
                 tweetPollResult.setAnswerId(answer.getQuestionAnswerId());
                 tweetPollResult.setColor(answer.getColor());
                 tweetPollResult.setVotes(Long.valueOf(objects[2].toString()));
             }
         }
         return tweetPollResult;
     }
 
     /**
      * Validate TweetPoll IP.
      * @param ipVote  ipVote
      * @param tweetPoll tweetPoll
      * @return {@link TweetPollResult}
      */
     public TweetPollResult validateTweetPollIP(final String ipVote, final TweetPoll tweetPoll){
         return getTweetPollDao().validateVoteIP(ipVote, tweetPoll);
     }
 
     /**
      * Validate User Twitter Account.
      * @param username username logged.
      * @return if user have twitter account
      * @throws EnMeNoResultsFoundException
      * @throws TwitterException
      */
     @Deprecated
     public Boolean validateUserTwitterAccount(final String username) throws EnMeNoResultsFoundException{
         final Account users = getUserAccount(username).getAccount();
         Boolean validate = false;
      // TODO: Removed by ENCUESTAME-43
     /*    log.info(users.getTwitterAccount());
         if(!users.getTwitterAccount().isEmpty() && !users.getTwitterPassword().isEmpty()){
             log.info(users.getTwitterPassword());
             try{
                 final User user = getTwitterService().verifyCredentials(users.getTwitterAccount(), users.getTwitterPassword());
                 log.info(user);
                 validate = Integer.valueOf(user.getId()) != null ? true : false;
             }
             catch (Exception e) {
                 log.error("Error Validate Twitter Account "+e.getMessage());
             }
         }*/
         log.info(validate);
         return validate;
     }
 
     /**
      * Create TweetPoll Folder.
      * @param folderName
      * @param username
      * @return
      * @throws EnMeNoResultsFoundException
      */
     public FolderBean createTweetPollFolder(final String folderName, final String username) throws EnMeNoResultsFoundException{
         final TweetPollFolder tweetPollFolderDomain = new TweetPollFolder();
         tweetPollFolderDomain.setUsers(getUserAccount(username).getAccount());
         tweetPollFolderDomain.setCreatedAt(new Date());
         tweetPollFolderDomain.setCreatedBy(getUserAccount(getUserPrincipalUsername()));
         tweetPollFolderDomain.setStatus(org.encuestame.persistence.domain.Status.ACTIVE);
         tweetPollFolderDomain.setFolderName(folderName);
         this.getTweetPollDao().saveOrUpdate(tweetPollFolderDomain);
         return ConvertDomainBean.convertFolderToBeanFolder(tweetPollFolderDomain);
     }
 
     /**
      * Get List of TweetPoll Folders.
      * @return
      * @throws EnMeNoResultsFoundException
      */
     public List<FolderBean> getFolders() throws EnMeNoResultsFoundException {
         final List<TweetPollFolder> folders = getTweetPollDao()
                 .retrieveTweetPollFolderByAccount(
                         getUserAccount(getUserPrincipalUsername()).getAccount());
         log.debug("List of Folders :"+folders.size());
         return ConvertDomainBean.convertListTweetPollFoldertoBean(folders);
 
     }
 
     /**
      * Update Tweet Poll Folder.
      * @throws EnMeNoResultsFoundException
      */
     public FolderBean updateTweetPollFolder(final Long folderId, final String folderName, final String username)
            throws EnMeNoResultsFoundException{
         final TweetPollFolder tweetPollFolder = this.getTweetPollFolder(folderId);
         if(tweetPollFolder == null) {
             throw new EnMeNoResultsFoundException("Tweet Poll Folder not found");
         }
         else{
             tweetPollFolder.setFolderName(folderName);
             getTweetPollDao().saveOrUpdate(tweetPollFolder);
         }
          return ConvertDomainBean.convertFolderToBeanFolder(tweetPollFolder);
      }
 
      /**
      * Remove TweetPoll Folder.
      * @param TweetPoll folderId
      * @throws EnMeNoResultsFoundException
      */
     public void deleteTweetPollFolder(final Long folderId) throws EnMeNoResultsFoundException{
         final TweetPollFolder tweetPollfolder = this.getTweetPollFolder(folderId);
         if(tweetPollfolder != null) {
             getTweetPollDao().delete(tweetPollfolder);
         } else {
             throw new EnMeNoResultsFoundException("TweetPoll folder not found");
         }
     }
 
     /**
      * Get Tweet Poll Folder.
      * @param id
      * @return
      */
     private TweetPollFolder getTweetPollFolder(final Long folderId){
         return this.getTweetPollDao().getTweetPollFolderById(folderId);
     }
 
     /**
      * Add {@link TweetPoll} to Folder.
      * @param folderId
      * @throws EnMeNoResultsFoundException
      */
     public void addTweetPollToFolder(final Long folderId, final String username, final Long tweetPollId)
            throws EnMeNoResultsFoundException {
         final TweetPollFolder tpfolder = this.getTweetPollFolderByFolderId(folderId);
          if (tpfolder != null) {
              final TweetPoll tpoll = this.getTweetPollById(tweetPollId);
              tpoll.setTweetPollFolder(tpfolder);
              getTweetPollDao().saveOrUpdate(tpoll);
          } else {
              throw new EnMeNoResultsFoundException("tweetPoll folder not found");
          }
     }
 
     /**
      * Change Status {@link TweetPoll}.
      * @param tweetPollId
      * @param username
      * @throws EnMeNoResultsFoundException
      */
     public void changeStatusTweetPoll(final Long tweetPollId, final String username)
            throws EnMeNoResultsFoundException, EnmeFailOperation{
         final TweetPoll tweetPoll = getTweetPoll(tweetPollId, username);
         if (tweetPoll != null) {
             tweetPoll.setCloseNotification(Boolean.TRUE);
             getTweetPollDao().saveOrUpdate(tweetPoll);
         } else {
                throw new EnmeFailOperation("Fail Change Status Operation");
         }
     }
 
     /**
      * Set if {@link TweetPoll} as favorite.
      * @param tweetPollId
      * @param username
      * @throws EnMeNoResultsFoundException
      * @throws EnmeFailOperation
      */
     public void setFavouriteTweetPoll(final Long tweetPollId, final String username) throws
            EnMeNoResultsFoundException, EnmeFailOperation{
         final TweetPoll tweetPoll = getTweetPoll(tweetPollId, username);
         if (tweetPoll != null) {
             tweetPoll.setFavourites(tweetPoll.getFavourites() == null ? false : !tweetPoll.getFavourites());
             getTweetPollDao().saveOrUpdate(tweetPoll);
         } else {
                throw new EnmeFailOperation("Fail Change Status Operation");
         }
     }
 
 
     /**
      * Change Allow Live Results {@link TweetPoll}.
      * @param tweetPollId
      * @param username
      * @throws EnMeNoResultsFoundException
      */
     public void changeAllowLiveResultsTweetPoll(final Long tweetPollId, final String username)
                 throws EnMeNoResultsFoundException, EnmeFailOperation{
         final TweetPoll tweetPoll = getTweetPollDao().getTweetPollByIdandUserId(tweetPollId, getPrimaryUser(username));
         if (tweetPoll != null){
             tweetPoll.setAllowLiveResults(tweetPoll.getAllowLiveResults() == null
                       ? false : !tweetPoll.getAllowLiveResults());
             getTweetPollDao().saveOrUpdate(tweetPoll);
         }
         else {
             throw new EnmeFailOperation("Fail Change Allow Live Results Operation");
         }
     }
 
     /**
      * Change Allow Live Results {@link TweetPoll}.
      * @param tweetPollId
      * @param username
      * @throws EnMeNoResultsFoundException
      */
     public void changeAllowCaptchaTweetPoll(final Long tweetPollId, final String username)
            throws EnMeNoResultsFoundException, EnmeFailOperation{
         final TweetPoll tweetPoll = getTweetPoll(tweetPollId, username);
         if (tweetPoll != null) {
              tweetPoll.setCaptcha(tweetPoll.getCaptcha() == null ? false : !tweetPoll.getCaptcha());
              getTweetPollDao().saveOrUpdate(tweetPoll);
         } else {
             throw new EnmeFailOperation("Fail Change Allow Captcha Operation");
         }
     }
 
     /**
      * Change Resume Live Results {@link TweetPoll}.
      * @param tweetPollId
      * @param username
      * @throws EnMeNoResultsFoundException
      */
     public void changeResumeLiveResultsTweetPoll(final Long tweetPollId, final String username)
            throws EnMeNoResultsFoundException, EnmeFailOperation {
         final TweetPoll tweetPoll = getTweetPoll(tweetPollId, username);
         if (tweetPoll != null) {
             tweetPoll.setResumeLiveResults(tweetPoll.getResumeLiveResults() == null
                       ? false : !tweetPoll.getResumeLiveResults());
             getTweetPollDao().saveOrUpdate(tweetPoll);
         }
         else {
             throw new EnmeFailOperation("Fail Change Resume Live Results Operation");
         }
     }
 
     /**
      * Change Allow Repeated TweetPoll.
      * @param tweetPollId
      * @param username
      * @throws EnMeNoResultsFoundException
      * @throws EnmeFailOperation
      */
     public void changeAllowRepeatedTweetPoll(final Long tweetPollId, final String username)
                 throws EnMeNoResultsFoundException, EnmeFailOperation{
         final TweetPoll tweetPoll = this.getTweetPoll(tweetPollId, username);
         if (tweetPoll != null){
             tweetPoll.setAllowRepatedVotes(tweetPoll.getAllowRepatedVotes() == null ? false : !tweetPoll.getAllowRepatedVotes());
             getTweetPollDao().saveOrUpdate(tweetPoll);
         } else {
             throw new EnmeFailOperation("Fail Change Allow Repeated Operation");
         }
     }
 
     /**
      * Change Close Notification.
      * @param tweetPollId
      * @param username
      * @throws EnMeNoResultsFoundException
      * @throws EnmeFailOperation
      */
     public void changeCloseNotificationTweetPoll(final Long tweetPollId, final String username)
            throws EnMeNoResultsFoundException, EnmeFailOperation{
         final TweetPoll tweetPoll = this.getTweetPoll(tweetPollId, username);
         if (tweetPoll != null){
             tweetPoll.setCloseNotification(tweetPoll.getCloseNotification() == null
                       ? false : !tweetPoll.getCloseNotification());
             getTweetPollDao().saveOrUpdate(tweetPoll);
         } else {
             throw new EnmeFailOperation("Fail Change Allow Repeated Operation");
         }
     }
 
     /*
      * (non-Javadoc)
      * @see org.encuestame.business.service.imp.ITweetPollService#saveOrUpdateTweetPoll(org.encuestame.persistence.domain.tweetpoll.TweetPoll)
      */
     public void saveOrUpdateTweetPoll(final TweetPoll tweetPoll){
         if(tweetPoll != null) {
             getTweetPollDao().saveOrUpdate(tweetPoll);
         }
     }
 
     /*
      * (non-Javadoc)
      * @see org.encuestame.business.service.imp.ITweetPollService#checkTweetPollCompleteStatus(org.encuestame.persistence.domain.tweetpoll.TweetPoll)
      */
     public void checkTweetPollCompleteStatus(final TweetPoll tweetPoll) {
         boolean next = true;
         log.debug("checkTweetPollCompleteStatus tweetPoll.getLimitVotesEnabled() "+tweetPoll.getLimitVotesEnabled());
         if (tweetPoll.getLimitVotesEnabled()) {
             long limitVotes = tweetPoll.getLimitVotes() == null ? 0 : tweetPoll.getLimitVotes();
             if (limitVotes <= this.getTweetPollTotalVotes(tweetPoll)) {
                 log.debug("checkTweetPollCompleteStatus limis vote");
                 tweetPoll.setCompleted(Boolean.TRUE);
                 next = false;
             }
         }
         log.debug("checkTweetPollCompleteStatus tweetPoll.getDateLimit() "+tweetPoll.getDateLimit());
         if (next && tweetPoll.getDateLimited() != null) {
             DateTime date = new DateTime(tweetPoll.getDateLimited());
             log.debug(date);
             if(date.isBeforeNow()){
                 log.debug("checkTweetPollCompleteStatus is After Now");
                 tweetPoll.setCompleted(Boolean.TRUE);
                 next = false;
             }
         }
         //TODO: other possibles validates.
         this.saveOrUpdateTweetPoll(tweetPoll);
     }
 
     /*
      * (non-Javadoc)
      * @see org.encuestame.business.service.imp.ITweetPollService#getTweetPollLinks(org.encuestame.persistence.domain.tweetpoll.TweetPoll)
      */
     public List<LinksSocialBean> getTweetPollLinks(final TweetPoll tweetPoll) {
       final List<LinksSocialBean> linksBean = new ArrayList<LinksSocialBean>();
       final List<TweetPollSavedPublishedStatus> links = getTweetPollDao().getLinksByTweetPoll(tweetPoll);
       log.debug("getTweetPollLinks "+links.size());
       for (TweetPollSavedPublishedStatus tweetPollSavedPublishedStatus : links) {
           log.debug("getTweetPollLinks "+tweetPollSavedPublishedStatus.toString());
           final LinksSocialBean linksSocialBean = new LinksSocialBean();
             linksSocialBean.setProvider(tweetPollSavedPublishedStatus
                     .getSocialAccount().getAccounType().name());
             linksSocialBean.setLink(SocialUtils.getSocialTweetPublishedUrl(
                     tweetPollSavedPublishedStatus.getTweetId(),
                     tweetPollSavedPublishedStatus.getSocialAccount()
                             .getSocialAccountName(),
                     tweetPollSavedPublishedStatus.getSocialAccount()
                             .getAccounType()));
           linksBean.add(linksSocialBean);
           log.debug("getTweetPollLinks "+linksSocialBean.toString());
        }
 
       return linksBean;
     }
 }
 
