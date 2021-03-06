 // Copyright (C) 2012 The Android Open Source Project
 //
 // Licensed under the Apache License, Version 2.0 (the "License");
 // you may not use this file except in compliance with the License.
 // You may obtain a copy of the License at
 //
 // http://www.apache.org/licenses/LICENSE-2.0
 //
 // Unless required by applicable law or agreed to in writing, software
 // distributed under the License is distributed on an "AS IS" BASIS,
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 // See the License for the specific language governing permissions and
 // limitations under the License.
 
 
 package com.google.gerrit.server.changedetail;
 
 import com.google.gerrit.common.data.ReviewResult;
 import com.google.gerrit.reviewdb.client.Change;
 import com.google.gerrit.reviewdb.client.ChangeMessage;
 import com.google.gerrit.reviewdb.server.ReviewDb;
 import com.google.gerrit.server.ChangeUtil;
 import com.google.gerrit.server.IdentifiedUser;
 import com.google.gerrit.server.mail.EmailException;
 import com.google.gerrit.server.project.ChangeControl;
 import com.google.gerrit.server.project.InvalidChangeOperationException;
 import com.google.gerrit.server.project.NoSuchChangeException;
 import com.google.gwtorm.server.AtomicUpdate;
 import com.google.gwtorm.server.OrmException;
 import com.google.inject.Inject;
 
 import java.util.Collections;
 import java.util.concurrent.Callable;
 
 import org.kohsuke.args4j.Argument;
 import org.kohsuke.args4j.Option;
 
 public class AlterTopic implements Callable<ReviewResult> {
 
   private final ChangeControl.Factory changeControlFactory;
   private final ReviewDb db;
   private final IdentifiedUser currentUser;
 
   @Argument(index = 0, required = true, multiValued = false,
             usage = "change with topic to change")
   private Change.Id changeId;
 
   public void setChangeId(final Change.Id changeId) {
     this.changeId = changeId;
   }
 
   @Argument(index = 1, required = true, multiValued = false, usage = "new topic")
   private String newTopicName;
 
   public void setTopic(final String topic) {
     this.newTopicName = topic.trim();
   }
 
   @Option(name = "--message", aliases = {"-m"},
           usage = "optional message to append to change")
   private String message;
 
   public void setMessage(final String message) {
     this.message = message;
   }
 
   @Inject
   AlterTopic(final ChangeControl.Factory changeControlFactory, final ReviewDb db,
       final IdentifiedUser currentUser) {
     this.changeControlFactory = changeControlFactory;
     this.db = db;
     this.currentUser = currentUser;
 
     changeId = null;
     newTopicName = null;
     message = null;
   }
 
   @Override
   public ReviewResult call() throws EmailException,
       InvalidChangeOperationException, NoSuchChangeException, OrmException {
     final ChangeControl control = changeControlFactory.validateFor(changeId);
     final ReviewResult result = new ReviewResult();
     result.setChangeId(changeId);
 
     if (!control.canAddPatchSet()) {
       throw new NoSuchChangeException(changeId);
     }
     if (!control.canEditTopicName()) {
       result.addError(new ReviewResult.Error(
           ReviewResult.Error.Type.EDIT_TOPIC_NAME_NOT_PERMITTED));
       return result;
     }
 
     final Change change = db.changes().get(changeId);
    final String oldTopicName = change.getTopic() != null ? change.getTopic() : "";
     if (!oldTopicName.equals(newTopicName)) {
       String summary;
       if (oldTopicName.isEmpty()) {
         summary = "Topic set to \"" + newTopicName + "\"";
       } else if (newTopicName.isEmpty()) {
         summary = "Topic \"" + oldTopicName + "\" removed";
       } else {
         summary = "Topic changed from \"" + oldTopicName //
             + "\" to \"" + newTopicName + "\"";
       }
       final ChangeMessage cmsg = new ChangeMessage(
           new ChangeMessage.Key(changeId, ChangeUtil.messageUUID(db)),
           currentUser.getAccountId(), change.currentPatchSetId());
       final StringBuilder msgBuf = new StringBuilder(summary);
       if (message != null && message.length() > 0) {
         msgBuf.append("\n\n");
         msgBuf.append(message);
       }
       cmsg.setMessage(msgBuf.toString());
 
       final Change updatedChange = db.changes().atomicUpdate(changeId,
           new AtomicUpdate<Change>() {
         @Override
         public Change update(Change change) {
           change.setTopic(newTopicName);
           return change;
         }
       });
 
       if (updatedChange == null) {
         String err = "Change is closed, submitted, or patchset is not latest";
         throw new InvalidChangeOperationException(err);
       }
       db.changeMessages().insert(Collections.singleton(cmsg));
     }
 
     return result;
   }
 }
