 /*
  * Copyright (c) 2009, 2010, 2011, 2012, 2013, B3log Team
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.b3log.symphony.util;
 
 import java.util.Calendar;
 import java.util.Map;
 import javax.inject.Inject;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import org.b3log.latke.Latkes;
 import org.b3log.latke.model.User;
 import org.b3log.latke.service.LangPropsService;
 import org.b3log.latke.service.annotation.Service;
 import org.b3log.latke.user.GeneralUser;
 import org.b3log.latke.user.UserService;
 import org.b3log.latke.user.UserServiceFactory;
 import org.b3log.latke.util.Sessions;
 import org.b3log.symphony.SymphonyServletListener;
 import org.b3log.symphony.model.Common;
 import org.b3log.symphony.model.Notification;
 import org.b3log.symphony.model.Option;
 import org.b3log.symphony.service.ArticleQueryService;
 import org.b3log.symphony.service.CommentQueryService;
 import org.b3log.symphony.service.NotificationQueryService;
 import org.b3log.symphony.service.OptionQueryService;
 import org.b3log.symphony.service.TagQueryService;
 import org.b3log.symphony.service.UserMgmtService;
 import org.json.JSONObject;
 
 /**
  * Filler utilities.
  *
  * @author <a href="http://88250.b3log.org">Liang Ding</a>
  * @version 1.0.0.9, Sep 2, 2013
  * @since 0.2.0
  */
 @Service
 public class Filler {
 
     /**
      * Language service.
      */
     @Inject
     private LangPropsService langPropsService;
 
     /**
      * User service.
      */
     private UserService userService = UserServiceFactory.getUserService();
 
     /**
      * Article query service.
      */
     @Inject
     private ArticleQueryService articleQueryService;
 
     /**
      * Comment query service.
      */
     @Inject
     private CommentQueryService commentQueryService;
 
     /**
      * Tag query service.
      */
     @Inject
     private TagQueryService tagQueryService;
 
     /**
      * Option query service.
      */
     @Inject
     private OptionQueryService optionQueryService;
 
     /**
      * User management service.
      */
     @Inject
     private UserMgmtService userMgmtService;
     
     /**
      * Notification query service.
      */
     @Inject
     private NotificationQueryService notificationQueryService;
 
     /**
      * Fills relevant articles.
      * 
      * @param dataModel the specified data model
      * @param article the specified article
      * @throws Exception exception
      */
     public void fillRelevantArticles(final Map<String, Object> dataModel, final JSONObject article) throws Exception {
         dataModel.put(Common.SIDE_RELEVANT_ARTICLES,
                 articleQueryService.getRelevantArticles(article, Symphonys.getInt("sideRelevantArticlesCnt")));
     }
 
     /**
      * Fills the latest comments.
      * 
      * @param dataModel the specified data model
      * @throws Exception exception
      */
     public void fillLatestCmts(final Map<String, Object> dataModel) throws Exception {
         dataModel.put(Common.SIDE_LATEST_CMTS, commentQueryService.getLatestComments(Symphonys.getInt("sizeLatestCmtsCnt")));
     }
 
     /**
      * Fills random articles.
      * 
      * @param dataModel the specified data model
      * @throws Exception exception 
      */
     public void fillRandomArticles(final Map<String, Object> dataModel) throws Exception {
         dataModel.put(Common.SIDE_RANDOM_ARTICLES, articleQueryService.getRandomArticles(Symphonys.getInt("sideRandomArticlesCnt")));
     }
 
     /**
      * Fills tags.
      * 
      * @param dataModel the specified data model
      * @throws Exception exception
      */
     public void fillSideTags(final Map<String, Object> dataModel) throws Exception {
         dataModel.put(Common.SIDE_TAGS, tagQueryService.getTags(Symphonys.getInt("sideTagsCnt")));
     }
 
     /**
      * Fills header.
      * 
      * @param request the specified request
      * @param response the specified response
      * @param dataModel the specified data model
      * @throws Exception exception 
      */
     public void fillHeader(final HttpServletRequest request, final HttpServletResponse response,
             final Map<String, Object> dataModel) throws Exception {
         fillMinified(dataModel);
         dataModel.put(Common.STATIC_RESOURCE_VERSION, Latkes.getStaticResourceVersion());
 
         fillTrendTags(dataModel);
         fillPersonalNav(request, response, dataModel);
 
         fillLangs(dataModel);
     }
 
     /**
      * Fills footer.
      * 
      * @param dataModel the specified data model
      * @throws Exception exception
      */
     public void fillFooter(final Map<String, Object> dataModel) throws Exception {
         fillSysInfo(dataModel);
 
         dataModel.put(Common.YEAR, String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
     }
 
     /**
      * Fills personal navigation.
      * 
      * @param request the specified request
      * @param response the specified response
      * @param dataModel the specified data model
      */
     private void fillPersonalNav(final HttpServletRequest request, final HttpServletResponse response,
             final Map<String, Object> dataModel) {
         dataModel.put(Common.IS_LOGGED_IN, false);
 
         if (null == Sessions.currentUser(request) && !userMgmtService.tryLogInWithCookie(request, response)) {
             dataModel.put("loginLabel", langPropsService.get("loginLabel"));
 
             return;
         }
         
         final GeneralUser curUser = userService.getCurrentUser(request);
         if (null == curUser) {
             dataModel.put("loginLabel", langPropsService.get("loginLabel"));
 
             return;
         }
 
         dataModel.put(Common.IS_LOGGED_IN, true);
         dataModel.put(Common.LOGOUT_URL, userService.createLogoutURL("/"));
 
         dataModel.put("logoutLabel", langPropsService.get("logoutLabel"));
 
         final String userName = curUser.getNickname();
         dataModel.put(User.USER_NAME, userName);
         
         final int unreadNotificationCount = notificationQueryService.getUnreadNotificationCount(curUser.getId());
         dataModel.put(Notification.NOTIFICATION_T_UNREAD_COUNT, unreadNotificationCount);
     }
 
     /**
      * Fills minified directory and file postfix for static JavaScript, CSS.
      * 
      * @param dataModel the specified data model
      */
     public void fillMinified(final Map<String, Object> dataModel) {
         switch (Latkes.getRuntimeMode()) {
             case DEVELOPMENT:
                 dataModel.put(Common.MINI_POSTFIX, "");
                 break;
             case PRODUCTION:
                 dataModel.put(Common.MINI_POSTFIX, Common.MINI_POSTFIX_VALUE);
                 break;
             default:
                 throw new AssertionError();
         }
     }
 
     /**
      * Fills the all language labels.
      * 
      * @param dataModel the specified data model
      */
     private void fillLangs(final Map<String, Object> dataModel) {
         dataModel.putAll(langPropsService.getAll(Latkes.getLocale()));
     }
 
     /**
      * Fills trend tags.
      * 
      * @param dataModel the specified data model
      * @throws Exception exception
      */
     private void fillTrendTags(final Map<String, Object> dataModel) throws Exception {
         dataModel.put(Common.TREND_TAGS, tagQueryService.getTrendTags(Symphonys.getInt("trendTagsCnt")));
     }
 
     /**
      * Fills system info.
      * 
      * @param dataModel the specified data model
      * @throws Exception exception 
      */
     private void fillSysInfo(final Map<String, Object> dataModel) throws Exception {
         dataModel.put(Common.VERSION, SymphonyServletListener.VERSION);
         dataModel.put(Common.ONLINE_VISITOR_CNT, optionQueryService.getOnlineVisitorCount());
 
         final JSONObject statistic = optionQueryService.getStatistic();
         dataModel.put(Option.CATEGORY_C_STATISTIC, statistic);
     }
 }
