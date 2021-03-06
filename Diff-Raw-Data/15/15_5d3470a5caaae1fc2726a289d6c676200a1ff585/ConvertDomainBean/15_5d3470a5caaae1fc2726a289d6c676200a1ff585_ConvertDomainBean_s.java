 /*
  ************************************************************************************
 
  * Copyright (C) 2001-2011 encuestame: system online surveys Copyright (C) 2011
  * encuestame Development Team.
  * Licensed under the Apache Software License version 2.0
  * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to  in writing,  software  distributed
  * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
  * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
  * specific language governing permissions and limitations under the License.
  ************************************************************************************
  */
 package org.encuestame.core.util;
 
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Set;
 
 import junit.framework.Assert;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.encuestame.persistence.dao.IFolder;
 import org.encuestame.persistence.domain.Comment;
 import org.encuestame.persistence.domain.EmailList;
 import org.encuestame.persistence.domain.GeoPoint;
 import org.encuestame.persistence.domain.GeoPointFolder;
 import org.encuestame.persistence.domain.GeoPointType;
 import org.encuestame.persistence.domain.HashTag;
 import org.encuestame.persistence.domain.Project;
 import org.encuestame.persistence.domain.dashboard.Dashboard;
 import org.encuestame.persistence.domain.dashboard.Gadget;
 import org.encuestame.persistence.domain.dashboard.GadgetProperties;
 import org.encuestame.persistence.domain.question.Question;
 import org.encuestame.persistence.domain.question.QuestionAnswer;
 import org.encuestame.persistence.domain.question.QuestionPattern;
 import org.encuestame.persistence.domain.security.Account;
 import org.encuestame.persistence.domain.security.Group;
 import org.encuestame.persistence.domain.security.Permission;
 import org.encuestame.persistence.domain.security.SocialAccount;
 import org.encuestame.persistence.domain.security.UserAccount;
 import org.encuestame.persistence.domain.survey.Poll;
 import org.encuestame.persistence.domain.survey.PollFolder;
 import org.encuestame.persistence.domain.survey.Survey;
 import org.encuestame.persistence.domain.survey.SurveyFolder;
 import org.encuestame.persistence.domain.tweetpoll.TweetPoll;
 import org.encuestame.persistence.domain.tweetpoll.TweetPollFolder;
 import org.encuestame.persistence.domain.tweetpoll.TweetPollSwitch;
 import org.encuestame.utils.DateUtil;
 import org.encuestame.utils.enums.Status;
 import org.encuestame.utils.enums.TypeSearchResult;
 import org.encuestame.utils.json.FolderBean;
 import org.encuestame.utils.json.HomeBean;
 import org.encuestame.utils.json.ProfileUserAccount;
 import org.encuestame.utils.json.QuestionBean;
 import org.encuestame.utils.json.SocialAccountBean;
 import org.encuestame.utils.json.TweetPollAnswerSwitchBean;
 import org.encuestame.utils.json.TweetPollBean;
 import org.encuestame.utils.json.QuestionPatternBean;
 import org.encuestame.utils.security.SignUpBean;
 import org.encuestame.utils.social.TypeAuth;
 import org.encuestame.utils.web.CommentBean;
 import org.encuestame.utils.web.DashboardBean;
 import org.encuestame.utils.web.GadgetBean;
 import org.encuestame.utils.web.GadgetPropertiesBean;
 import org.encuestame.utils.web.HashTagBean;
 import org.encuestame.utils.web.QuestionAnswerBean;
 import org.encuestame.utils.web.TypeTreeNode;
 import org.encuestame.utils.web.UnitGroupBean;
 import org.encuestame.utils.web.UnitLists;
 import org.encuestame.utils.web.UnitLocationBean;
 import org.encuestame.utils.web.UnitLocationFolder;
 import org.encuestame.utils.web.UnitLocationTypeBean;
 import org.encuestame.utils.web.UnitPermission;
 import org.encuestame.utils.web.PollBean;
 import org.encuestame.utils.web.UnitProjectBean;
 import org.encuestame.utils.web.UnitSessionUserBean;
 import org.encuestame.utils.web.SurveyBean;
 import org.encuestame.utils.web.UserAccountBean;
 import org.encuestame.utils.web.UtilTreeNode;
 
 
 /**
  * Convert Domain to  Beans.
  * @author Picado, Juan juanATencuestame.org
  * @since 03/12/2009 06:38:32
  */
 public class ConvertDomainBean {
 
     /**
      * Log.
      */
     private static Log log = LogFactory.getLog(ConvertDomainBean.class);
 
     /**
      *
      * @param type
      * @return
      */
     public static final	 TypeAuth convertStringToEnum(final String type){
             return TypeAuth.OAUTH1;
     }
 
     /**
      *
      * @param pollSwitchs
      * @return
      */
     public static final  List<TweetPollAnswerSwitchBean> convertListTweetPollSwitchToBean(final List<TweetPollSwitch> pollSwitchs) {
         final List<TweetPollAnswerSwitchBean> listSwitchs = new ArrayList<TweetPollAnswerSwitchBean>();
         for (TweetPollSwitch account : pollSwitchs) {
             listSwitchs.add(ConvertDomainBean.convertTweetPollSwitchToBean(account));
         }
         return listSwitchs;
     }
 
     /**
      *
      * @param pollSwitch
      * @return
      */
     public static final TweetPollAnswerSwitchBean convertTweetPollSwitchToBean(final TweetPollSwitch pollSwitch){
         final TweetPollAnswerSwitchBean answerSwitchBean = new TweetPollAnswerSwitchBean();
         answerSwitchBean.setTweetPollId(pollSwitch.getTweetPoll().getTweetPollId());
         answerSwitchBean.setAnswerBean(ConvertDomainBean.convertAnswerToBean(pollSwitch.getAnswers()));
         answerSwitchBean.setShortUrl(pollSwitch.getShortUrl());
         answerSwitchBean.setId(pollSwitch.getSwitchId());
         return answerSwitchBean;
     }
 
     /**
      * Social Account.
      * @param socialAccount {@link SocialAccount}.
      * @return {@link SocialAccountBean}
      */
     public static final SocialAccountBean convertSocialAccountToBean(final SocialAccount socialAccount){
            final SocialAccountBean socialAccountBean = new SocialAccountBean();
                    socialAccountBean.setAccount(socialAccount.getSocialAccountName());
                    socialAccountBean.setAccountId(socialAccount.getId());
                    socialAccountBean.setTypeAccount(socialAccount.getAccounType().toString());
                    socialAccountBean.setDescriptionProfile(socialAccount.getDescriptionProfile());
                    socialAccount.setEmail(socialAccount.getEmail() == null ? "" : socialAccount.getEmail());
                    socialAccount.setDefaultSelected(socialAccount.getDefaultSelected() ==  null
                                     ? false : socialAccount.getDefaultSelected());
                    socialAccountBean.setAddedAccount(socialAccount.getAddedAccount());
                    socialAccountBean.setPrictureUrl(socialAccount.getPrictureUrl());
                    socialAccountBean.setProfilePictureUrl(socialAccount.getProfilePictureUrl());
                    socialAccountBean.setProfileThumbnailPictureUrl(socialAccount.getProfileThumbnailPictureUrl());
                    socialAccountBean.setRealName(socialAccount.getRealName());
                    socialAccountBean.setSocialAccountName(socialAccount
                             .getSocialAccountName());
                    socialAccountBean.setSocialProfileUrl(socialAccount
                             .getPublicProfileUrl() == null ? "" : socialAccount
                             .getPublicProfileUrl());
         return socialAccountBean;
     }
 
     /**
      * Convert {@link HashTag} to {@link HashTagBean}.
      * @param hashTag name
      * @return
      */
     public static final HashTagBean convertHashTagDomain(final HashTag hashTag){
         final HashTagBean unitHashTag = new HashTagBean();
         unitHashTag.setHashTagName(hashTag.getHashTag());
         unitHashTag.setId(hashTag.getHashTagId());
         unitHashTag.setHits(hashTag.getHits());
         //TODO: ENCUESTAME-191
         // int x = (10 + (int)(Math.random() * ((40) - 5) + 10)); //TEMP.
         if(hashTag.getSize() != null){
             unitHashTag.setSize(hashTag.getSize().intValue());
         }
 
         return unitHashTag;
     }
 
     /**
      * Convert List of {@link HashTag} to List of {@link HashTagBean}.
      * @param tags list of tags
      * @return
      */
     public static final  List<HashTagBean> convertListHashTagsToBean(final List<HashTag> tags) {
         final List<HashTagBean> listTags = new ArrayList<HashTagBean>();
         for (HashTag account : tags) {
             listTags.add(ConvertDomainBean.convertHashTagDomain(account));
         }
         return listTags;
     }
 
     /**
      * Convert List Twitter Accounts.
      * @param accounts
      * @return
      */
     public static final  List<SocialAccountBean> convertListSocialAccountsToBean(final List<SocialAccount> accounts) {
         log.debug("convertListSocialAccountsToBean "+accounts.size());
         final List<SocialAccountBean> loadListPermission = new ArrayList<SocialAccountBean>();
         for (SocialAccount account : accounts) {
             log.debug("convertListSocialAccountsToBean account "+account.getId());
             loadListPermission.add(ConvertDomainBean.convertSocialAccountToBean(account));
         }
         return loadListPermission;
     }
 
     /**
      * Email Lists.
      * @param twitterAccounts
      * @return
      */
     public static final UnitLists convertEmailListtoToBean(final EmailList emailLists){
            final UnitLists emailListsBean = new UnitLists();
                   emailListsBean.setCreatedAt(emailLists.getCreatedAt());
                   emailListsBean.setId(emailLists.getIdList());
                   emailListsBean.setListName(emailLists.getListName());
                   emailListsBean.setUserId(emailLists.getUsuarioEmail().getUid());
            return emailListsBean;
     }
 
     /**
      * Convert List Twitter Accounts.
      * @param accounts
      * @return
      */
     public static final  List<UnitLists> convertEmailListToBean(final List<EmailList> lists) {
         final List<UnitLists> loadEmailLists = new ArrayList<UnitLists>();
         for (EmailList emailList : lists) {
             loadEmailLists.add(ConvertDomainBean.convertEmailListtoToBean(emailList));
         }
         return loadEmailLists;
     }
 
 
     /**
      * Convert {@link Account} to {@link UnitSessionUserBean}.
      * @param user user
      * @return {@link UnitSessionUserBean}
      */
     public static final UnitSessionUserBean convertUserSessionToUserBean(final Account user){
         final UnitSessionUserBean sessionUserBean =  new UnitSessionUserBean();
         sessionUserBean.setUserSessionId(user.getUid());
      /*   sessionUserBean.setConsumerTwitterKey(user.getConsumerKey());
         sessionUserBean.setConsumerTwitterSecret(user.getConsumerSecret());
         sessionUserBean.setTwitterPassword(user.getTwitterPassword());
         sessionUserBean.setTwitterAccount(user.getTwitterAccount());
         sessionUserBean.setTwitterTwitterPing(user.getTwitterPing());
      */
         // TODO: Removed by ENCUESTAME-43
         return sessionUserBean;
     }
 
     /**
      * Convert {@link UserAccount} to {@link UserAccountBean}.
      * @param secUserSecondary {@link UserAccount}.
      * @return {@link UserAccountBean}
      */
     public static final UserAccountBean convertSecondaryUserToUserBean(final UserAccount secUserSecondary){
         final UserAccountBean unitUserBean = new UserAccountBean();
         if(secUserSecondary != null){
             unitUserBean.setId(secUserSecondary.getUid());
             unitUserBean.setName(secUserSecondary.getCompleteName());
             unitUserBean.setEmail(secUserSecondary.getUserEmail());
             unitUserBean.setUsername(secUserSecondary.getUsername());
             unitUserBean.setStatus(secUserSecondary.isUserStatus());
             unitUserBean.setGroupBean(convertGroupDomainToBean(secUserSecondary.getGroup()));
             unitUserBean.setGroupId(secUserSecondary.getGroup() == null ? null : secUserSecondary.getGroup().getGroupId());
             unitUserBean.setListPermission(convertSetToUnitPermission(secUserSecondary.getSecUserPermissions()));
             //System.out.println("Convert Enjoy Date "+secUserSecondary.getEnjoyDate());
             unitUserBean.setDateNew(secUserSecondary.getEnjoyDate());
             //System.out.println("Convert Enjoy Date 1"+unitUserBean.getDateNew());
             unitUserBean.setIpLastLogged(secUserSecondary.getLastIpLogged());
             unitUserBean.setLastTimeLogged(secUserSecondary.getLastTimeLogged());
         }
         return unitUserBean;
     }
 
     /**
      * Return user account limited profile info.
      * @param secUserSecondary
      * @return
      */
     public static final ProfileUserAccount convertUserAccountToUserProfileBean(final UserAccount secUserSecondary){
         final ProfileUserAccount unitUserBean = new ProfileUserAccount();
         if(secUserSecondary != null){
             unitUserBean.setName(secUserSecondary.getCompleteName());
             unitUserBean.setEmail(secUserSecondary.getUserEmail());
             unitUserBean.setUsername(secUserSecondary.getUsername());
             //TODO: Bug 112, add private, language y bio.
         }
         return unitUserBean;
     }
 
     /**
      * Convert Basic {@link UserAccount} to {@link UserAccountBean}.
      * @param userAccount {@link UserAccount}.
      * @return {@link UserAccountBean}
      */
     public static final UserAccountBean convertBasicSecondaryUserToUserBean(final UserAccount userAccount){
         final UserAccountBean unitUserBean = new UserAccountBean();
         if (userAccount != null) {
             unitUserBean.setId(userAccount.getUid());
             unitUserBean.setName(userAccount.getCompleteName());
             unitUserBean.setEmail(userAccount.getUserEmail());
             unitUserBean.setUsername(userAccount.getUsername());
             unitUserBean.setStatus(userAccount.isUserStatus());
             unitUserBean.setInviteCode(userAccount.getInviteCode());
             unitUserBean
                     .setPictureSource(userAccount.getPictureSource() == null ? null
                             : userAccount.getPictureSource().toString().toLowerCase());
         }
         return unitUserBean;
     }
 
     /**
      * Convert {@link UserAccount} to {@link SignUpBean}.
      * @param userAccount
      * @return
      */
     public static final SignUpBean convertUserAccountToSignUpBean(final UserAccount userAccount){
         final SignUpBean signUpBean = new SignUpBean();
         if(userAccount != null){
             signUpBean.setCaptcha("");
             signUpBean.setEmail(userAccount.getUserEmail());
             signUpBean.setFullName(userAccount.getCompleteName());
             signUpBean.setPassword(userAccount.getPassword());
             signUpBean.setUsername(userAccount.getUsername());
         }
         return signUpBean;
     }
 
 
     /**
      * Convert List of {@link UserAccount} to {@link UserAccountBean}.
      * @param listUsers
      * @return
      */
     public static final List<UserAccountBean> convertCollectionUsersToBean(final Collection<UserAccount> listUsers) {
         final List<UserAccountBean> loadListUsers = new LinkedList<UserAccountBean>();
         for (UserAccount secUserSecondary : listUsers) {
             loadListUsers.add(ConvertDomainBean.convertSecondaryUserToUserBean(secUserSecondary));
         }
         return loadListUsers;
     }
 
     /**
      * Convert Domain Permission to Bean Permission.
      * @param userId user id
      * @return collection of permission
      * @throws Exception all exceptions.
   */
     public static final Collection<UnitPermission> convertSetToUnitPermission(final Set<Permission> permissions) {
         final Collection<UnitPermission> loadListPermission = new LinkedList<UnitPermission>();
         for (Permission permission : permissions) {
             loadListPermission.add(ConvertDomainBean.convertPermissionToBean(permission));
         }
         return loadListPermission;
     }
 
     /**
      * Convert set to unit group bean
      * @param userId user id
      * @return collection of groups beans.
      * @throws Exception
      */
     public static final Collection<UnitGroupBean> convertSetToUnitGroupBean(final Set<Group> groups){
             final Collection<UnitGroupBean> loadListGroups = new LinkedList<UnitGroupBean>();
             for (Group groupsList : groups) {
                  loadListGroups.add(ConvertDomainBean.convertGroupDomainToBean(groupsList));
             }
         return loadListGroups;
     }
 
     /**
      * Convert {@link Group} to {@link UnitGroupBean}
      * @param groupDomain {@link Group}
      * @return {@link UnitGroupBean}
      */
     public static final UnitGroupBean convertGroupDomainToBean(final Group groupDomain) {
         final UnitGroupBean groupBean = new UnitGroupBean();
         if (groupDomain != null) {
             groupBean.setId(groupDomain.getGroupId());
             groupBean.setGroupName(groupDomain.getGroupName());
             groupBean.setGroupDescription(groupDomain.getGroupDescriptionInfo());
             groupBean.setStateId(groupDomain.getIdState());
         }
         return groupBean;
     }
 
     /**
      * Convert {@link GeoPoint} to {@link UnitLocationBean}.
      * @param location {@link GeoPoint}
      * @return {@link UnitLocationBean}
      */
     public static final UnitLocationBean convertLocationToBean(final GeoPoint location){
         final UnitLocationBean locationBean = new UnitLocationBean();
         locationBean.setId(location.getLocateId());
         locationBean.setStatus(location.getLocationStatus()  == null ? Status.INACTIVE.name() : location.getLocationStatus().name());
         locationBean.setName(location.getLocationDescription());
         locationBean.setLat(location.getLocationLatitude());
         locationBean.setLng(location.getLocationLongitude());
         locationBean.setAccuracy(location.getLocationAccuracy());
         locationBean.setAddress(location.getLocationAddress());
         locationBean.setCountryName(location.getLocationCountryName());
         locationBean.setCountryCode(location.getLocationCountryCode());
         if(location.getTidtype() != null){
             locationBean.setTidtype(location.getTidtype().getLocationTypeId());
         }
         return locationBean;
     }
 
     /**
      * Convert List of Locations.
      * @param geoPoint List {@link GeoPoint}
      * @return List of {@link UnitLocationBean}
      */
     public static final List<UnitLocationBean> convertListToUnitLocationBean(final List<GeoPoint> geoPoint){
         final List<UnitLocationBean> listLocations = new ArrayList<UnitLocationBean>();
         for (GeoPoint location : geoPoint) {
             listLocations.add(ConvertDomainBean.convertLocationToBean(location));
         }
     return listLocations;
     }
 
     /**
      * @param locationType {@link GeoPointType}.
      * @return {@link UnitLocationTypeBean}
      */
    public static final UnitLocationTypeBean convertLocationTypeToBean(final GeoPointType locationType){
         final UnitLocationTypeBean locationTypeBean = new UnitLocationTypeBean();
         locationTypeBean.setIdLocType(locationType.getLocationTypeId());
         locationTypeBean.setLocTypeDesc(locationType.getLocationTypeDescription());
         locationTypeBean.setLevel(locationType.getLocationTypeLevel());
         return locationTypeBean;
     }
 
     /**
      * Convert {@link Project} to {@link UnitProjectBean}
       * @param project {@link UnitProjectBean}
      * @return {@link UnitProjectBean}
      */
     public static final UnitProjectBean convertProjectDomainToBean(final Project project) {
         final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateUtil.DEFAULT_FORMAT_DATE);
         final UnitProjectBean projectBean = new UnitProjectBean();
         projectBean.setName(project.getProjectDescription());
         projectBean.setDateFinish(project.getProjectDateFinish());
         projectBean.setDateInit(project.getProjectDateStart());
         projectBean.setId(project.getProyectId());
         projectBean.setDescription(project.getProjectDescription());
         projectBean.setName(project.getProjectName());
         projectBean.setProjectInfo(project.getProjectInfo());
         if(project.getLead() != null){
             projectBean.setLeader(project.getLead().getUid());
         }
         projectBean.setPriority(project.getPriority().name());
         projectBean.setPublished(project.getPublished());
         projectBean.setState(project.getProjectStatus().name());
         if(project.getProjectDateStart() != null){
             projectBean.setFormatedDateInit(simpleDateFormat.format(project.getProjectDateStart()));
         }
         if(project.getProjectDateFinish() != null){
             projectBean.setFormatedDateFinish(simpleDateFormat.format(project.getProjectDateFinish()));
         }
         projectBean.setHide(project.getHideProject());
         projectBean.setNotify(project.getNotifyMembers());
         //TODO: add other properties.
         log.debug("project bean converted "+projectBean.toString());
         return projectBean;
     }
 
 
     /**
      * Convert {@link Permission} to {@link UnitPermission}
      * @param permission permission.
      * @return permBean
      */
     public static final UnitPermission convertPermissionToBean(final Permission permission){
       final UnitPermission permBean = new UnitPermission();
       permBean.setId(permission.getIdPermission());
       permBean.setDescription(permission.getPermissionDescription());
       permBean.setPermission(permission.getPermission().toString());
       return permBean;
     }
 
 
     /**
      * Convert {@link QuestionPattern} to {@link QuestionPatternBean}.
      * @param pattern  {@link QuestionPattern}
      * @return {@link QuestionPatternBean}
      */
     public static final QuestionPatternBean convertQuestionPatternToBean(final QuestionPattern pattern){
         final QuestionPatternBean patterBean = new QuestionPatternBean();
         patterBean.setId(pattern.getPatternId());
         patterBean.setPatronType(pattern.getPatternType());
         patterBean.setLabel(pattern.getLabelQid());
         return patterBean;
     }
 
     /**
      * Convert {@link Question} to {@link QuestionBean}.
      * @param questions {@link Question}
      * @return {@link QuestionBean}
      */
     public static final QuestionBean convertQuestionsToBean(final Question questions){
         final QuestionBean questionBean = new QuestionBean();
         questionBean.setId(questions.getQid());
         questionBean.setQuestionName(questions.getQuestion());
         questionBean.setSlugName(questions.getSlugQuestion());
         questionBean.setHits(questions.getHits());
         questionBean.setUserId(questions.getAccountQuestion() == null ? null : questions.getAccountQuestion().getUid());
         return questionBean;
     }
 
     /**
      * Convert {@link QuestionAnswer} to {@link QuestionAnswerBean}.
      * @param questionsAnswer {@link QuestionAnswer}
      * @return {@link QuestionAnswerBean}.
      */
     public static final QuestionAnswerBean convertAnswerToBean(final QuestionAnswer questionsAnswer){
             final QuestionAnswerBean answersBean = new QuestionAnswerBean();
             answersBean.setAnswerId(questionsAnswer.getQuestionAnswerId());
             answersBean.setAnswers(questionsAnswer.getAnswer());
             answersBean.setUrl(questionsAnswer.getUrlAnswer());
             answersBean.setShortUrl(questionsAnswer.getProvider() == null ? null : questionsAnswer.getProvider().toString());
             answersBean.setAnswerHash(questionsAnswer.getUniqueAnserHash());
             answersBean.setQuestionId(questionsAnswer.getQuestions().getQid());
             answersBean.setColor(questionsAnswer.getColor());
             return answersBean;
     }
 
     /**
      * Convert {@link TweetPoll} to {@link TweetPollBean}.
      * @param tweetPoll tweet poll.
      * @return {@link TweetPollBean}
      */
     public static final TweetPollBean convertTweetPollToBean(final TweetPoll tweetPoll){
         final TweetPollBean unitTweetPoll = new TweetPollBean();
         unitTweetPoll.setId(tweetPoll.getTweetPollId());
         unitTweetPoll.setScheduleDate(tweetPoll.getScheduleDate());
         unitTweetPoll.setCreateDate(DateUtil.DOJO_DATE_FORMAT.format(tweetPoll.getCreateDate()));
         unitTweetPoll.setAllowLiveResults(tweetPoll.getAllowLiveResults() == null ? false : tweetPoll.getAllowLiveResults());
         unitTweetPoll.setResumeLiveResults(tweetPoll.getResumeLiveResults() == null ? false : tweetPoll.getResumeLiveResults());
         unitTweetPoll.setSchedule(tweetPoll.getScheduleTweetPoll() == null ? false : tweetPoll.getScheduleTweetPoll());
         unitTweetPoll.setResultNotification(tweetPoll.getResultNotification() == null ? false : tweetPoll.getResultNotification());
         unitTweetPoll.setUserId(tweetPoll.getTweetOwner().getUid());
         unitTweetPoll.setOwnerUsername(tweetPoll.getEditorOwner() == null ? null : tweetPoll.getEditorOwner().getUsername());
         unitTweetPoll.setCaptcha(tweetPoll.getCaptcha() == null ? false : tweetPoll.getCaptcha());
         unitTweetPoll.setCloseNotification(tweetPoll.getCloseNotification() == null ? false : tweetPoll.getCloseNotification());
         unitTweetPoll.setFavourites(tweetPoll.getFavourites() == null ? false : tweetPoll.getFavourites());
         unitTweetPoll.setCompleted(tweetPoll.getCompleted() == null ? false : tweetPoll.getCompleted());
         unitTweetPoll.setQuestionBean(convertQuestionsToBean(tweetPoll.getQuestion()));
         unitTweetPoll.setHits(tweetPoll.getHits() == null ? EnMeUtils.VOTE_MIN : tweetPoll.getHits());
         unitTweetPoll.setAllowRepeatedVotes(tweetPoll.getAllowRepatedVotes() == null ? false : tweetPoll.getAllowRepatedVotes());
         unitTweetPoll.setHashTags(ConvertDomainBean.convertListHashTagsToBean(new ArrayList<HashTag>(tweetPoll.getHashTags())));
         unitTweetPoll.setTotalVotes(tweetPoll.getNumbervotes() == null ? EnMeUtils.VOTE_MIN : Long.valueOf(tweetPoll.getNumbervotes()));
         unitTweetPoll.setCreatedDateAt(tweetPoll.getCreateDate());
         unitTweetPoll.setLimitVotesDate(tweetPoll.getDateLimit() == null ? false : tweetPoll.getDateLimit());
         unitTweetPoll.setUpdateDate(tweetPoll.getUpdatedDate());
         if (tweetPoll.getDateLimit() != null && tweetPoll.getDateLimited() != null) {
             unitTweetPoll.setDateToLimit(tweetPoll.getDateLimited() == null
                     ? null
                     : DateUtil.DOJO_DATE_FORMAT.format(tweetPoll.getDateLimited()));
         }
         unitTweetPoll.setRelevance(tweetPoll.getRelevance() == null ? EnMeUtils.RATE_DEFAULT : tweetPoll.getRelevance());
         return unitTweetPoll;
     }
 
     /**
      * Convert TweetPoll List to TweetPoll Bean.
      * @param tweetPollBean
      * @return
      */
     public static final List<TweetPollBean> convertListToTweetPollBean(final List<TweetPoll> tweetPol){
         final List<TweetPollBean> listTweetPolls = new ArrayList<TweetPollBean>();
         for (TweetPoll tweets : tweetPol) {
             listTweetPolls.add(ConvertDomainBean.convertTweetPollToBean(tweets));
         }
     return listTweetPolls;
     }
 
     /**
      * Convert {@link Poll} to {@link PollBean}.
      * @param poll
      * @return unitPoll unitPoll
      */
     public static final PollBean convertPollDomainToBean(final Poll poll){
         final PollBean unitPoll = new PollBean();
         unitPoll.setId(poll.getPollId());
         unitPoll.setCompletedPoll(poll.getPollCompleted());
        unitPoll.setCreationDate(DateUtil.DOJO_DATE_FORMAT.format(poll.getCreatedAt()));
         unitPoll.setQuestionBean(ConvertDomainBean.convertQuestionsToBean(poll.getQuestion()));
         unitPoll.setPublishPoll(poll.getPublish());
         unitPoll.setUpdatedDate(poll.getUpdatedDate());
         unitPoll.setTotalVotes(poll.getNumbervotes() == null ? EnMeUtils.VOTE_MIN : Long.valueOf(poll.getNumbervotes()));
         unitPoll.setLikeVote(poll.getLikeVote() == null ? EnMeUtils.LIKE_DEFAULT : Long.valueOf(poll.getLikeVote()));
         unitPoll.setDislikeVote(poll.getDislikeVote() == null ? EnMeUtils.DISLIKE_DEFAULT : Long.valueOf(poll.getDislikeVote()));
         unitPoll.setOwnerUsername(poll.getEditorOwner().getCompleteName());
         unitPoll.setRelevance(poll.getRelevance() == null ? EnMeUtils.RATE_DEFAULT : poll.getRelevance());
         unitPoll.setHits(poll.getHits() == null ? EnMeUtils.VOTE_MIN : poll.getHits());
         unitPoll.setFavorite(poll.getFavourites());
         unitPoll.setHashTags(ConvertDomainBean.convertListHashTagsToBean(new ArrayList<HashTag>(poll.getHashTags())));
         return unitPoll;
     }
 
     /**
      * Convert list to poll bean.
      * @param poll
      * @return
      */
     public static final List<PollBean> convertListToPollBean(final List<Poll> poll){
         final List<PollBean> listPolls = new ArrayList<PollBean>();
         for (Poll polls : poll) {
             listPolls.add(ConvertDomainBean.convertPollDomainToBean(polls));
         }
     return listPolls;
     }
 
     /**
      * Convert Set to Unit Poll bean.
      * @param userId user id
      * @return collection of groups beans.
      * @throws Exception
      */
     public static final List<PollBean> convertSetToPollBean(final List<Poll> polls){
         final List<PollBean> loadListPolls = new LinkedList<PollBean>();
             for (Poll poll : polls) {
                 loadListPolls.add(ConvertDomainBean.convertPollDomainToBean(poll));
             }
         return loadListPolls;
     }
 
     /**
      * Convert List of {@link GeoPointFolder}. to List of {@link UnitLocationFolder}.
      * @param GeoPointFolders {@link GeoPointFolder}.
      * @return
      */
     public static final List<UnitLocationFolder> convertListToUnitLocationFolderBean(final List<GeoPointFolder> geoPointFolders){
         final List<UnitLocationFolder> listFolders = new ArrayList<UnitLocationFolder>();
         for (GeoPointFolder locationFolder : geoPointFolders) {
             listFolders.add(ConvertDomainBean.convertGeoPointFolderDomainToBean(locationFolder));
         }
     return listFolders;
     }
 
     /**
      * Convert {@link GeoPointFolder}. to {@link UnitLocationFolder}.
      * @param geoPointFolder {@link GeoPointFolder}.
      * @return {@link UnitLocationFolder}.
      */
     public static UnitLocationFolder convertGeoPointFolderDomainToBean(final GeoPointFolder geoPointFolder){
         final UnitLocationFolder locationFolder = new UnitLocationFolder();
         locationFolder.setId(geoPointFolder.getLocationFolderId());
         locationFolder.setName(geoPointFolder.getFolderName());
         locationFolder.setType(geoPointFolder.getFolderType().GROUPING.name());
         return locationFolder;
     }
 
     /**
      * Convert {@link UnitLocationFolder} to {@link UtilTreeNode}.
      * @param unitLocationSubFolder List {@link UnitLocationFolder}.
      * @return List {@link UtilTreeNode}.
      */
     public static List<UtilTreeNode> convertFolderToDragrable(
             final List<UnitLocationFolder> unitFolderLocationBeans,
             final TypeTreeNode typeTreeNode){
         final List<UtilTreeNode> itemDragables = new ArrayList<UtilTreeNode>();
         for (UnitLocationFolder unitLocation : unitFolderLocationBeans) {
             final UtilTreeNode dragable = new UtilTreeNode();
             dragable.setId(unitLocation.getId());
             dragable.setName(unitLocation.getName());
             dragable.setNode(typeTreeNode);
             itemDragables.add(dragable);
         }
         return itemDragables;
     }
 
      /**
       * Convert {@link UnitLocationBean} to {@link UtilTreeNode}.
       * @param unitLocationSubFolder List of {@link UnitLocationBean}.
       * @return
       */
     public static List<UtilTreeNode> convertItemToDragrable(
              final List<UnitLocationBean> unitLocationBeans,
              final TypeTreeNode typeTreeNode){
          final List<UtilTreeNode> itemDragables = new ArrayList<UtilTreeNode>();
          for (UnitLocationBean unitLocation : unitLocationBeans) {
              final UtilTreeNode dragable = new UtilTreeNode();
              log.info("convertItemToDragrable "+unitLocation.getId() +" "+unitLocation.getName());
              dragable.setId(unitLocation.getId());
              dragable.setName(unitLocation.getName());
              dragable.setNode(typeTreeNode);
              itemDragables.add(dragable);
          }
          log.info("itemDragables "+itemDragables.size());
          return itemDragables;
      }
 
     /**
      * Convert List to Unit Question bean.
      * @param question question id
      * @return collection of question beans.
      * @throws Exception
      */
     public static final List<QuestionBean> convertListToUnitQuestionBean(final List<Question> questions){
         final List<QuestionBean> loadListQuestions = new LinkedList<QuestionBean>();
             for (Question question : questions) {
                 loadListQuestions.add(ConvertDomainBean.convertQuestionsToBean(question));
             }
         return loadListQuestions;
     }
 
     /**
      * Convert Folder to {@link FolderBean}.
      * @param folder
      * @return
      */
     public static final FolderBean convertFolderToBeanFolder(final IFolder folder){
         final FolderBean unitFolder = new FolderBean();
         unitFolder.setCreateAt(folder.getCreatedAt());
         unitFolder.setFolderName(folder.getFolderName());
         unitFolder.setId(folder.getId());
         return unitFolder;
     }
 
     /**
      * Convert List of {@link IFolder} to {@link FolderBean}.
      * @param folders List of Folders.
      * @return
      */
     public static final List<FolderBean> convertListTweetPollFoldertoBean(final List<TweetPollFolder> folders){
         final List<FolderBean> folderList = new LinkedList<FolderBean>();
             for (TweetPollFolder folder : folders) {
                 folderList.add(ConvertDomainBean.convertFolderToBeanFolder(folder));
             }
         return folderList;
     }
 
     /**
      * Convert a List of {@link PollFolder} to {@link FolderBean}.
      * @param folders List of {@link PollFolder}.
      * @return
      */
     public static final List<FolderBean> convertListPollFolderToBean(final List<PollFolder> folders){
         final List<FolderBean> folderList = new LinkedList<FolderBean>();
             for (PollFolder folder : folders) {
                 folderList.add(ConvertDomainBean.convertFolderToBeanFolder(folder));
             }
         return folderList;
     }
 
     /**
      * Convert a List of {@link SurveyFolder} to {@link FolderBean}.
      * @param folders List of {@link PollFolder}.
      * @return
      */
     public static final List<FolderBean> convertListSurveyFolderToBean(final List<SurveyFolder> folders){
         final List<FolderBean> folderList = new LinkedList<FolderBean>();
             for (SurveyFolder folder : folders) {
                 folderList.add(ConvertDomainBean.convertFolderToBeanFolder(folder));
             }
         return folderList;
     }
 
     /**
      *
      * @param survey
      * @return
      */
     public static final SurveyBean convertSurveyDomaintoBean(final Survey survey){
         final SurveyBean unitSurvey = new SurveyBean();
         unitSurvey.setSid(survey.getSid());
         unitSurvey.setTicket(survey.getTicket());
         unitSurvey.setStartDate(survey.getStartDate());
         unitSurvey.setEndDate(survey.getEndDate());
         unitSurvey.setDateInterview(survey.getDateInterview());
         unitSurvey.setComplete(survey.getComplete());
         //unitSurvey.setUnitUserBean(ConvertDomainBean.convertUserSessionToUserBean(survey.getSecUsers()));
         unitSurvey.setCustomMessage(survey.getCustomMessage());
         unitSurvey.setCustomStartMessages(survey.getCustomStartMessages());
         //unitSurvey.setCustomFinalMessage(survey.getCustomFinalMessage());
         unitSurvey.setShowProgressBar(survey.getShowProgressBar());
         unitSurvey.setOptionalTitle(survey.getOptionalTitle());
         unitSurvey.setPasswordRestrictions(survey.getPasswordRestrictions());
         unitSurvey.setIpProtection(survey.getIpProtection());
         unitSurvey.setIpRestriction(survey.getIpRestriction());
         unitSurvey.setPassProtection(survey.getPassProtection());
         unitSurvey.setCloseAfterDate(survey.getCloseAfterDate());
         unitSurvey.setClosedDate(survey.getClosedDate());
         unitSurvey.setCloseAfterquota(survey.getCloseAfterquota());
         unitSurvey.setClosedQuota(survey.getClosedQuota());
         unitSurvey.setShowResults(survey.getShowResults());
         unitSurvey.setNumbervotes(survey.getNumbervotes());
         //unitSurvey.setHits(survey.getHits());
         unitSurvey.setAdditionalInfo(survey.getAdditionalInfo());
         unitSurvey.setShowAdditionalInfo(survey.getShowAdditionalInfo());
         unitSurvey.setNotifications(survey.getNotifications());
         unitSurvey.setName(survey.getName());
         unitSurvey.setRelevance(survey.getRelevance());
         return unitSurvey;}
 
     /**
      * Convert List survey domain to bean.
      * @param surveyList
      * @return
      */
     public static final List<SurveyBean> convertListSurveyToBean(final List<Survey> surveyList){
         final List<SurveyBean> surveyBeanList = new LinkedList<SurveyBean>();
         for (Survey survey : surveyList) {
             surveyBeanList.add(ConvertDomainBean.convertSurveyDomaintoBean(survey));
         }
         return surveyBeanList;
     }
 
     /**
      * Convert Dashboard bean to dashboard domain.
      * @param dashboards
      * @return
      */
     public static final List<DashboardBean> convertListDashboardToBean(final List<Dashboard> dashboards){
         final List<DashboardBean> dashboardList = new LinkedList<DashboardBean>();
             for (Dashboard dashboard : dashboards) {
                 dashboardList.add(ConvertDomainBean.convertDashboardDomaintoBean(dashboard));
             }
         return dashboardList;
     }
 
     /**
      * Convert Dashboard domain to dashboard bean.
      * @param dashboard
      * @return
      */
     public static final DashboardBean convertDashboardDomaintoBean(final Dashboard dashboard){
         final DashboardBean dashboardBean = new DashboardBean();
         if (dashboard != null) {
             dashboardBean.setDashboardId(dashboard.getBoardId());
             dashboardBean.setDashboardName(dashboard.getPageBoardName());
             dashboardBean.setDashboardDesc(dashboard.getDescription());
             dashboardBean.setFavorite(dashboard.getFavorite());
             dashboardBean.setFavoriteCounter(dashboard.getFavoriteCounter());
             dashboardBean.setLayout((dashboard.getPageLayout() == null ? null : dashboard.getPageLayout().toString()));
             dashboardBean.setSequence(dashboard.getBoardSequence());
             dashboardBean.setSelected(dashboard.getSelectedByDefault() == null ? false : dashboard.getSelectedByDefault());
         }
         return dashboardBean;
     }
 
     /**
      * Convert Gadget domain to gadget bean.
      * @param gadget
      * @return
      */
     public static final GadgetBean convertGadgetDomaintoBean(final Gadget gadget){
         final GadgetBean gadgetBean = new GadgetBean();
         gadgetBean.setGadgetId(gadget.getGadgetId());
         gadgetBean.setGadgetName(gadget.getGadgetName());
         gadgetBean.setGadgetColor(gadget.getGadgetColor());
         gadgetBean.setGadgetColumn(gadget.getGadgetColumn());
         gadgetBean.setGadgetPosition(gadget.getGadgetColumn());
         gadgetBean.setStatus(gadget.getStatus());
         return gadgetBean;
     }
 
     /**
      * Convert Gadget domain list to gadget bean.
      * @param gadgets
      * @return
      */
     public static final List<GadgetBean> convertListGadgetToBean(final List<Gadget> gadgets){
         final List<GadgetBean> gadgetList = new LinkedList<GadgetBean>();
             for (Gadget gadget : gadgets) {
                 gadgetList.add(ConvertDomainBean.convertGadgetDomaintoBean(gadget));
             }
         return gadgetList;
     }
 
     /**
      * Convert Gadget properties domain to bean.
      * @param gadgetProp
      * @return
      */
     public static final GadgetPropertiesBean convertGadgetPropertiesDomaintoBean(final GadgetProperties gadgetProp){
         final GadgetPropertiesBean propertiesBean = new GadgetPropertiesBean();
         propertiesBean.setPropertyId(gadgetProp.getPropertyId());
         propertiesBean.setGadgetPropName(gadgetProp.getGadgetPropName());
         propertiesBean.setGadgetPropValue(gadgetProp.getGadgetPropValue());
         //propertiesBean.setUserAccount(gadgetProp.getUserAccount());
         //propertiesBean.setGadget(gadgetProp.getGadget());
         return propertiesBean;
     }
 
     /**
      * Convert list gadget properties domain to bean.
      * @param properties
      * @return
      */
     public static final List<GadgetPropertiesBean> convertListGadgetPropertiesToBean(final List<GadgetProperties> properties){
         final List<GadgetPropertiesBean> gadgetBean = new LinkedList<GadgetPropertiesBean>();
             for (GadgetProperties gadget : properties) {
                 gadgetBean.add(ConvertDomainBean.convertGadgetPropertiesDomaintoBean(gadget));
             }
         return gadgetBean;
     }
 
     /**
      * Convert Domain to bean.
      * @param commentDomain
      * @return
      */
    public static final CommentBean convertCommentDomainToBean(final Comment commentDomain) {
        final CommentBean commentBean = new CommentBean();
        Assert.assertNotNull(commentDomain);
        commentBean.setCommentId(commentDomain.getCommentId());
        commentBean.setComment(commentDomain.getComment());
        commentBean.setCreatedAt(commentDomain.getCreatedAt());
         commentBean
                 .setDislikeVote(commentDomain.getDislikeVote() == null ? EnMeUtils.VOTE_MIN
                         : commentDomain.getDislikeVote());
         commentBean
                 .setLikeVote(commentDomain.getLikeVote() == null ? EnMeUtils.VOTE_MIN
                         : commentDomain.getLikeVote());
        long id = 0;
        boolean set = false;
        /*
         * is possible refactor this part? ..
         */
        if (commentDomain.getPoll() != null && !set) {
            id = commentDomain.getPoll().getPollId();
            commentBean.setType(TypeSearchResult.POLL.toString());
            set = true;
        }
        if (commentDomain.getTweetPoll() != null && !set) {
            id = commentDomain.getTweetPoll().getTweetPollId();
            commentBean.setType(TypeSearchResult.TWEETPOLL.toString());
            set = true;
        }
        if (commentDomain.getSurvey() != null && !set) {
            id = commentDomain.getSurvey().getSid();
            commentBean.setType(TypeSearchResult.SURVEY.toString());
            set = true;
        }
        commentBean.setId(id);
        commentBean.setParentId(commentDomain.getParentId() == null ? null : commentDomain.getParentId());
        commentBean.setUserAccountId(commentDomain.getUser() == null ? null : commentDomain.getUser().getUid());
        commentBean.setCommentedBy(commentDomain.getUser().getCompleteName());
        commentBean.setCommentedByUsername(commentDomain.getUser().getUsername());
        return commentBean;
    }
 
    /**
     * Convert Comment list domain to comment list bean
     * @param comments
     * @return
     */
 
    public static final List<CommentBean> convertListCommentDomainToBean(final List<Comment> commentList){
        final List<CommentBean> commentsBean = new LinkedList<CommentBean>();
        for (Comment comment : commentList) {
            commentsBean.add(ConvertDomainBean.convertCommentDomainToBean(comment));
        }
        return commentsBean;
      }
 
    /**
     * Convert TweetPollBean List to Home Bean.
     * @param tweetPollBean
     * @return
     */
     public static final List<HomeBean> convertTweetPollListToHomeBean(
             final List<TweetPollBean> items) {
         final List<HomeBean> listFrontEndTweetPollItems = new ArrayList<HomeBean>();
         for (TweetPollBean tweetPollBean : items) {
             listFrontEndTweetPollItems.add(ConvertDomainBean
                     .convertTweetPollToHomeBean(tweetPollBean));
         }
         return listFrontEndTweetPollItems;
     }
 
   /**
    * Convert {@link TweetPollBean} to {@link HomeBean}.
    * @param tweetBean
    * @return
    */
    public static final HomeBean convertTweetPollToHomeBean(final TweetPollBean tweetBean){
        final HomeBean homeBean = new HomeBean();
            homeBean.setId(tweetBean.getId());
           homeBean.setCreateDate(DateUtil.DOJO_DATE_FORMAT.format(tweetBean.getCreateDate()));
            homeBean.setQuestionBean(tweetBean.getQuestionBean());
            homeBean.setRelativeTime(tweetBean.getRelativeTime());
            homeBean.setTotalVotes(tweetBean.getTotalVotes());
            homeBean.setHits(tweetBean.getHits() == null ? 0L : tweetBean.getHits());
            homeBean.setUserId(tweetBean.getUserId());
            homeBean.setOwnerUsername(tweetBean.getOwnerUsername());
            homeBean.setItemType(tweetBean.getItemType() == null ? null : tweetBean.getItemType().toString());
            homeBean.setRelevance(tweetBean.getRelevance());
        return homeBean;
    }
 
    /**
     * Convert PollBean List to Home Bean.
     * @param PollBean
     * @return
     */
    public static final List<HomeBean> convertPollListToHomeBean(final List<PollBean> items){
        final List<HomeBean> listFrontEndItems = new ArrayList<HomeBean>();
        for (PollBean pollBean : items) {
            listFrontEndItems.add(ConvertDomainBean.convertPollToHomeBean(pollBean));
         }
    return listFrontEndItems;
    }
 
   /**
    * Convert {@link PollBean} to {@link HomeBean}.
    * @param pollBean
    * @return
    */
    public static final HomeBean convertPollToHomeBean(final PollBean pollBean){
        final HomeBean homeBean = new HomeBean();
        homeBean.setId(pollBean.getId());
        homeBean.setQuestionBean(pollBean.getQuestionBean());
        homeBean.setOwnerUsername(pollBean.getOwnerUsername());
       homeBean.setCreateDate(DateUtil.DOJO_DATE_FORMAT.format(pollBean.getCreationDate()));
        homeBean.setTotalVotes(pollBean.getTotalVotes());
        homeBean.setHits(pollBean.getHits() == null ? 0L : pollBean.getHits());
        homeBean.setRelativeTime(pollBean.getRelativeTime());
        homeBean.setItemType(pollBean.getItemType() == null ? null : pollBean.getItemType().toString());
        return homeBean;
    }
 
    /**
     * Convert SurveyBean List to Home Bean.
     * @param items
     * @return
     */
    public static final List<HomeBean> convertSurveyListToHomeBean(final List<SurveyBean> items){
        final List<HomeBean> listFrontEndItems = new ArrayList<HomeBean>();
        for (SurveyBean surveyBean : items) {
            listFrontEndItems.add(ConvertDomainBean.convertSurveyToHomeBean(surveyBean));
         }
    return listFrontEndItems;
    }
 
    /**
     * Convert {@link SurveyBean} to {@link HomeBean}.
     * @param pollBean
     * @return
     */
    public static final HomeBean convertSurveyToHomeBean(final SurveyBean surveyBean){
        // TODO: ENCUESTAME-312
        final HomeBean homeBean = new HomeBean();
        homeBean.setId(surveyBean.getSid());
        homeBean.setQuestionBean(null);
        homeBean.setOwnerUsername(null);
        homeBean.setHits(null);
        homeBean.setCreateDate(null);
        homeBean.setTotalVotes(null);
        homeBean.setRelativeTime(null);
        homeBean.setItemType(null);
        return homeBean;
    }
 }
