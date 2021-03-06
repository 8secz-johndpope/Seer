 /* 
 * ################################################################
 * 
 * ProActive: The Java(TM) library for Parallel, Distributed, 
 *            Concurrent computing with Security and Mobility
 * 
 * Copyright (C) 1997-2002 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive-support@inria.fr
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *  
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s): 
 * 
 * ################################################################
 */ 
 package org.objectweb.proactive.core.body.reply;
 
 import org.objectweb.proactive.core.UniqueID;
import org.objectweb.proactive.core.body.LocalBodyStore;
 import org.objectweb.proactive.core.body.UniversalBody;
 import org.objectweb.proactive.core.body.message.MessageImpl;
 import org.objectweb.proactive.core.body.request.Request;
import org.objectweb.proactive.core.mop.Utils;
 
 
 public class ReplyImpl extends MessageImpl implements Reply, java.io.Serializable {
 
   /**
    * The hypothetic result
    */
   protected Object result;
   
   
   public ReplyImpl(UniqueID senderID, long sequenceNumber, String methodName, Object result) {
     super(senderID, sequenceNumber, true, methodName);
     this.result = result;
   }
 
   public Object getResult() {
     return result;
   }
     
   public void send(UniversalBody destinationBody) throws java.io.IOException {
  	// if destination body is on the same VM that the sender, we must perform 
  	// a deep copy of result in order to preserve ProActive model.
  	boolean isLocal = LocalBodyStore.getInstance().getLocalBody(destinationBody.getID()) != null;
  	if (isLocal) {
  		result = Utils.makeDeepCopy(result);
  	}
     destinationBody.receiveReply(this);
   }
 }
