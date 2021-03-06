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
 
 import org.jtalks.jcommune.model.dao.TopicDao;
 import org.jtalks.jcommune.model.entity.Post;
 import org.jtalks.jcommune.model.entity.Topic;
 import org.jtalks.jcommune.model.entity.User;
 import org.jtalks.jcommune.service.BranchService;
 import org.jtalks.jcommune.service.SecurityService;
 import org.jtalks.jcommune.service.TopicService;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.util.List;
 
 /**
  * Topic service class. This class contains method needed to manipulate with Topic persistent entity.
  *
  * @author Osadchuck Eugeny
  * @author Vervenko Pavel
  * @author Kirill Afonin
  * @author Vitaliy Kravchenko
  * @author Max Malakhov
  */
 public class TransactionalTopicService extends AbstractTransactionalEntityService<Topic, TopicDao>
         implements TopicService {
     private final Logger logger = LoggerFactory.getLogger(getClass());
     private final SecurityService securityService;
     private BranchService branchService;
 
     /**
      * Create an instance of User entity based service
      *
      * @param dao                data access object, which should be able do all CRUD operations with topic entity
      * @param securityService    {@link SecurityService} for retrieving current user
      * @param branchService {@link org.jtalks.jcommune.service.BranchService} instance to be injected
      */
     public TransactionalTopicService(TopicDao dao, SecurityService securityService,
                                      BranchService branchService) {
         this.securityService = securityService;
         this.dao = dao;
         this.branchService = branchService;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public Topic getTopicWithPosts(long id) {
         return dao.get(id);
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void addAnswer(long topicId, String answerBody) {
         User currentUser = securityService.getCurrentUser();
         // Check if the user is authenticated
         if (currentUser == null) {
             throw new IllegalStateException("User should log in to post answers.");
         }
         Topic topic = dao.get(topicId);
         Post answer = Post.createNewPost();
         answer.setPostContent(answerBody);
         answer.setUserCreated(currentUser);
         topic.addPost(answer);
         dao.saveOrUpdate(topic);
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void createTopic(String topicName, String bodyText, long branchId) {
         User currentUser = securityService.getCurrentUser();
 
         Post post = Post.createNewPost();
         post.setUserCreated(currentUser);
         post.setPostContent(bodyText);
 
         Topic topic = Topic.createNewTopic();
         topic.setTitle(topicName);
         topic.setTopicStarter(currentUser);
         topic.addPost(post);
         topic.setBranch(branchService.get(branchId));
 
         dao.saveOrUpdate(topic);
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public List<Topic> getAllTopicsAccordingToBranch(Long id) {
         return dao.getAllTopicsAccordingToBranch(id);
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void deletePost(long topicId, long postId) {
         logger.debug("User confirm post removing postId = " + postId);
         Topic topic = dao.get(topicId);
         List<Post> posts = topic.getPosts();
 
         for (Post post : posts) {
             if (post.getId() == postId) {
                 topic.removePost(post);           
                 break;
             }
        }       
         dao.saveOrUpdate(topic);
     }
 
 }
