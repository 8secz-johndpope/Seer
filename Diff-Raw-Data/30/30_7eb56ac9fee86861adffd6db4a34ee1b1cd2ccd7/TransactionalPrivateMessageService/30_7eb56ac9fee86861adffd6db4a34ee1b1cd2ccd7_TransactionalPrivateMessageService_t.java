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
 
 import java.util.List;
 import org.jtalks.jcommune.model.dao.PrivateMessageDao;
 import org.jtalks.jcommune.model.entity.PrivateMessage;
 import org.jtalks.jcommune.model.entity.User;
 import org.jtalks.jcommune.service.PrivateMessageService;
 import org.jtalks.jcommune.service.SecurityService;
 import org.jtalks.jcommune.service.UserService;
 import org.jtalks.jcommune.service.exceptions.NotFoundException;
 
 /**
  * The implementation of PrivateMessageServices.
  * 
  * @author Pavel Vervenko
  */
 public class TransactionalPrivateMessageService
         extends AbstractTransactionalEntityService<PrivateMessage, PrivateMessageDao> implements PrivateMessageService {
 
     private final SecurityService securityService;
     private final UserService userService;
 
     /**
      * Creates the instance of service.
      * @param pmDao PrivateMessageDao
      * @param securityService for retrieving current user
     * @param userService for getting user by name
      */
     public TransactionalPrivateMessageService(PrivateMessageDao pmDao,
             SecurityService securityService, UserService userService) {
         this.dao = pmDao;
         this.securityService = securityService;
         this.userService = userService;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public List<PrivateMessage> getInboxForCurrentUser() {
         User currentUser = securityService.getCurrentUser();
         return dao.getAllForUser(currentUser);
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public List<PrivateMessage> getOutboxForCurrentUser() {
         User currentUser = securityService.getCurrentUser();
         return dao.getAllFromUser(currentUser);
     }
 
 
     /**
      * {@inheritDoc}
      */
     @Override
     public PrivateMessage sendMessage(String title, String body, String recipient) throws NotFoundException {
         PrivateMessage pm = PrivateMessage.createNewPrivateMessage();
         pm.setTitle(title);
         pm.setBody(body);
         pm.setUserFrom(securityService.getCurrentUser());
         pm.setUserTo(userService.getByUsername(recipient));
         dao.saveOrUpdate(pm);
         return pm;
     }
 }
