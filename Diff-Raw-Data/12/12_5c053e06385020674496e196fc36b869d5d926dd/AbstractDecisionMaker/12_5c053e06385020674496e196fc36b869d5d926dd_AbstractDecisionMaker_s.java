 /**
  * Copyright (c) 2011, SOCIETIES Consortium (WATERFORD INSTITUTE OF TECHNOLOGY (TSSG), HERIOT-WATT UNIVERSITY (HWU), SOLUTA.NET 
  * (SN), GERMAN AEROSPACE CENTRE (Deutsches Zentrum fuer Luft- und Raumfahrt e.V.) (DLR), Zavod za varnostne tehnologije
  * informacijske družbe in elektronsko poslovanje (SETCCE), INSTITUTE OF COMMUNICATION AND COMPUTER SYSTEMS (ICCS), LAKE
  * COMMUNICATIONS (LAKE), INTEL PERFORMANCE LEARNING SOLUTIONS LTD (INTEL), PORTUGAL TELECOM INOVAÇÃO, SA (PTIN), IBM Corp., 
  * INSTITUT TELECOM (ITSUD), AMITEC DIACHYTI EFYIA PLIROFORIKI KAI EPIKINONIES ETERIA PERIORISMENIS EFTHINIS (AMITEC), TELECOM 
  * ITALIA S.p.a.(TI),  TRIALOG (TRIALOG), Stiftelsen SINTEF (SINTEF), NEC EUROPE LTD (NEC))
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
  * conditions are met:
  *
  * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
  *
  * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
  *    disclaimer in the documentation and/or other materials provided with the distribution.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
  * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT 
  * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 
 package org.societies.useragent.decisionmaking;
 
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.concurrent.ExecutionException;
 
 import org.slf4j.LoggerFactory;
 import org.societies.api.internal.servicelifecycle.IServiceDiscovery;
 import org.societies.api.internal.servicelifecycle.ServiceDiscoveryException;
 import org.societies.api.internal.useragent.conflict.IConflictResolutionManager;
 import org.societies.api.internal.useragent.decisionmaking.IDecisionMaker;
 import org.societies.api.internal.useragent.feedback.IUserFeedback;
 import org.societies.useragent.conflict.*;
 import org.societies.api.internal.useragent.model.ExpProposalContent;
 import org.societies.api.internal.useragent.model.ExpProposalType;
 import org.societies.api.personalisation.model.IAction;
 import org.societies.api.personalisation.model.IActionConsumer;
 import org.societies.api.schema.servicelifecycle.model.Service;
 import org.societies.api.internal.personalisation.model.IOutcome;
 import org.slf4j.*;
 
 
 public abstract class AbstractDecisionMaker implements IDecisionMaker {
 	IConflictResolutionManager manager;
 	IUserFeedback feedbackHandler;
 	private Logger logging = LoggerFactory.getLogger(this.getClass());
 
 	public IConflictResolutionManager getManager() {
 		return manager;
 	}
 
 	public void setManager(IConflictResolutionManager manager) {
 		this.manager = manager;
 	}
 
 	public IUserFeedback getFeedbackHandler() {
 		return feedbackHandler;
 	}
 
 	public void setFeedbackHandler(IUserFeedback feedbackHandler) {
 		this.feedbackHandler = feedbackHandler;
 	}
 
 	@Override
 	public void makeDecision(List<IOutcome> intents, List<IOutcome> preferences) {
 		// TODO Auto-generated method stub
 		HashSet<IOutcome> conflicts=new HashSet<IOutcome>();
 		logging.debug("start resolving DM");
 		for (IOutcome intent : intents) {
 			IOutcome action=intent;
 			//unresolved preference ioutcomes
 			for (IOutcome preference : preferences) {
 				ConflictType conflict = detectConflict(intent, preference);
 				if (conflict == ConflictType.PREFERNCE_INTENT_NOT_MATCH) {
 					action = manager.resolveConflict(action,preference);
 					if(action ==null){
 						conflicts.add(preference);
 					}
 				}else if (conflict==ConflictType.UNKNOWN_CONFLICT){
 					/*handler the unknown work*/
 				}
 			}
 			if(conflicts.size()==0)//no unresolved conflicts
 				this.implementIAction(action);
 			else{
 				List<String> options=new ArrayList<String>();
 				options.add(intent.toString());
 				for(IOutcome conf:conflicts)
 				options.add(conf.toString());
 				ExpProposalContent epc=new ExpProposalContent("Conflict Detected!",
 						options.toArray(new String[options.size()]));
 				feedbackHandler.getExplicitFB(ExpProposalType.RADIOLIST, epc, 
 						new DecisionMakingCallback(this,intent,conflicts));
 			}
 			conflicts.clear();
 		}
 		logging.debug("after resolving DM");
 	}
 
 	protected abstract ConflictType detectConflict(IOutcome intent,
 			IOutcome prefernce);
 
 	protected abstract void implementIAction(IAction action);
 
 }
