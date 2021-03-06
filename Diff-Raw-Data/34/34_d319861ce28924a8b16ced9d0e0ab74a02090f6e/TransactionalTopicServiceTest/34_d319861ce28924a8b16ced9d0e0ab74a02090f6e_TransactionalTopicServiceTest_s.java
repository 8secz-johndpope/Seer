 /**
  * Copyright (C) 2011  jtalks.org Team
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
  * Also add information on how to contact you by electronic and paper mail.
  * Creation date: Apr 12, 2011 / 8:05:19 PM
  * The jtalks.org Project
  */
 package org.jtalks.jcommune.service.transactional;
 
 import org.joda.time.DateTime;
 import org.jtalks.jcommune.model.dao.TopicDao;
 import org.jtalks.jcommune.model.entity.Branch;
 import org.jtalks.jcommune.model.entity.Post;
 import org.jtalks.jcommune.model.entity.Topic;
 import org.jtalks.jcommune.model.entity.User;
 import org.jtalks.jcommune.service.BranchService;
 import org.jtalks.jcommune.service.SecurityService;
 import org.jtalks.jcommune.service.TopicService;
 import org.mockito.Matchers;
 import org.testng.Assert;
 import org.testng.annotations.BeforeMethod;
 import org.testng.annotations.Test;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import static org.mockito.Mockito.*;
 
 /**
  * This test cover {@code TransactionalTopicService} logic validation.
  * Logic validation cover update/get/error cases by this class.
  * @author Osadchuck Eugeny
  * @author Kravchenko Vitaliy
  */
 public class TransactionalTopicServiceTest {
 
     final long TOPIC_ID = 999;
     final String TOPIC_TITLE = "topic title";
     final String ANSWER_BODY = "Test Answer Body";
     final DateTime TOPIC_CREATION_DATE = new DateTime();
     private TopicService topicService;
     private SecurityService securityService;
     private BranchService branchService;
     private TopicDao topicDao;
     final long POST_ID = 333;
 
     @BeforeMethod
     public void setUp() throws Exception {
         topicDao = mock(TopicDao.class);
         branchService = mock(BranchService.class);
         securityService = mock(SecurityService.class);
         topicService = new TransactionalTopicService(topicDao, securityService, branchService);
     }
     
     @Test
     public void deleteByIdTest(){
         topicService.delete(POST_ID);
         
         verify(topicDao, times(1)).delete(Matchers.anyLong());
     }
 
     @Test(expectedExceptions = {IllegalArgumentException.class})
     public void deleteByNagativeIdTest(){
         topicService.delete(-1l);
         verify(topicDao, never()).delete(Matchers.anyLong());
     }
 
     @Test
     public void getByIdTest() {
         when(topicDao.get(TOPIC_ID)).thenReturn(getTopic(false));
 
         Topic topic = topicService.get(TOPIC_ID);
 
         Assert.assertEquals(topic, getTopic(false), "Topics aren't equals");
 
         verify(topicDao, times(1)).get(Matchers.anyLong());
     }
 
     @Test(expectedExceptions = {IllegalArgumentException.class})
     public void getByNagativeIdTest(){
         topicService.get(-1l);
         verify(topicDao, never()).get(Matchers.anyLong());
     }
     
     @Test
     public void getAllTest() {
         List<Topic> expectedUserList = new ArrayList<Topic>();
         expectedUserList.add(getTopic(false));
         when(topicDao.getAll()).thenReturn(expectedUserList);
 
         List<Topic> actualUserList = topicService.getAll();
 
         Assert.assertEquals(actualUserList, expectedUserList, "Topics lists aren't equals");
 
         verify(topicDao, times(1)).getAll();
     }
 
     /**
      * Check for the answering logic works correctly.
      */
     @Test
     public void addAnswerTest() {
         Topic topic = getTopic(false);
         int postsNumberBefore = topic.getPosts().size();
         User currentUser = getUser();
 
         when(topicService.get(TOPIC_ID)).thenReturn(topic);
         when(securityService.getCurrentUser()).thenReturn(currentUser);
 
         topicService.addAnswer(TOPIC_ID, ANSWER_BODY);
         int postsNumberAfter = topic.getPosts().size();
         Post newPost = topic.getPosts().get(postsNumberAfter - 1);
 
         Assert.assertEquals(postsNumberBefore + 1, postsNumberAfter, "Posts number didn't increased by 1");
         Assert.assertEquals(newPost.getPostContent(), ANSWER_BODY, "Answer body isn't the same");
         Assert.assertEquals(newPost.getUserCreated(), currentUser, "User isn't the same");
 
         verify(securityService, times(1)).getCurrentUser();
         verify(topicDao, times(1)).saveOrUpdate(topic);
         verify(topicDao, times(1)).get(TOPIC_ID);
     }
 
     /**
      * Check for throwing exception when the user isn't logged in.
      */
     @Test(expectedExceptions = IllegalStateException.class)
     public void addAnswerExceptionTest() {
         Topic topic = getTopic(false);
 
         when(securityService.getCurrentUser()).thenReturn(null);
         when(topicService.get(TOPIC_ID)).thenReturn(topic);
 
         topicService.addAnswer(TOPIC_ID, ANSWER_BODY);
     }
 
     @Test
     public void testCreateTopic() {
         when(securityService.getCurrentUser()).thenReturn(getUser());
         when(branchService.get(1l)).thenReturn(new Branch());
         topicService.createTopic(TOPIC_TITLE, ANSWER_BODY, 1l);
 
         verify(securityService, times(1)).getCurrentUser();
         verify(topicDao, times(1)).saveOrUpdate(Matchers.<Topic>anyObject());
         verify(branchService, times(1)).get(1l);
     }
     
     @Test
     public void deletePostTest(){
         Topic topic = getTopic(true);
         int expectedPostCount = topic.getPosts().size();
         when(topicDao.get(anyLong())).thenReturn(topic);
         topicService.deletePost(TOPIC_ID, POST_ID);
         int actualPostCount = topic.getPosts().size();
         Assert.assertTrue(actualPostCount == 0 || actualPostCount < expectedPostCount, "Post was not deleted");        
         verify(topicDao, times(1)).get(anyLong());
         verify(topicDao, times(1)).saveOrUpdate(Matchers.any(Topic.class));
     }
 
     @Test
     public void getAllTopicsAccordingToBranchTest(){
         List<Topic> topics = new ArrayList<Topic>();
         topics.add(getTopic(false));
         when(topicDao.getAllTopicsAccordingToBranch(1L)).thenReturn(topics);
 
         List<Topic> actualTopics = topicService.getAllTopicsAccordingToBranch(1L);
 
         verify(topicDao,times(1)).getAllTopicsAccordingToBranch(1L);
         Assert.assertTrue(topics.size()==actualTopics.size());
 
     }
 
     /**
      * Create new dummy User with username "Test" and user.id  equal to 100500.
      *
      * @return the user
      */
     private User getUser() {
         User currentUser = new User();
         currentUser.setId(100500);
         currentUser.setUsername("Test");
         return currentUser;
     }
     
     /**
      * Get dummy topic.
      * @param withPosts - set to true if you want add collection of 10 posts to topic
      * @return - dummy topic with topicId=TOPIC_ID,topictitle= TOPIC_TITLE,
      * topic_creationDate=TOPIC_CREATION_DATE and posts. Posts ids started from POST_ID to POST_ID plus 10.
      */
     private Topic getTopic(boolean withPosts) {
         User topicStarter = getUser();
         Topic topic = new Topic();
         topic.setCreationDate(TOPIC_CREATION_DATE);
         topic.setId(TOPIC_ID);
         topic.setTitle(TOPIC_TITLE);
         topic.setTopicStarter(topicStarter);
         if(withPosts){
             for (long i = POST_ID; i <= POST_ID + 10; i++) {
                 Post post = Post.createNewPost();
                 post.setId(i);
                 topic.addPost(post);
             }
         }
         return topic;
     }
 }
