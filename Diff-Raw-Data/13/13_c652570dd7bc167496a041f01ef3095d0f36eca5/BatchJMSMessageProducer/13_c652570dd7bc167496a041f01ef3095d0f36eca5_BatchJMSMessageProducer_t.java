 /*
  * Licensed to the Sakai Foundation (SF) under one
  * or more contributor license agreements. See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership. The SF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License. You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations under the License.
  */
 package org.sakaiproject.nakamura.grouper.event;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 
 import javax.jms.Connection;
 import javax.jms.DeliveryMode;
 import javax.jms.JMSException;
 import javax.jms.Message;
 import javax.jms.MessageProducer;
 import javax.jms.Queue;
 import javax.jms.Session;
 
 import org.apache.felix.scr.annotations.Component;
 import org.apache.felix.scr.annotations.Modified;
 import org.apache.felix.scr.annotations.Property;
 import org.apache.felix.scr.annotations.Reference;
 import org.apache.sling.commons.osgi.OsgiUtil;
 import org.apache.solr.client.solrj.SolrQuery;
 import org.apache.solr.client.solrj.SolrServer;
 import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.common.SolrDocument;
 import org.sakaiproject.nakamura.api.activemq.ConnectionFactoryService;
 import org.sakaiproject.nakamura.api.solr.SolrServerService;
 import org.sakaiproject.nakamura.grouper.api.GrouperConfiguration;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * Post {@link Message}s to a {@link Queue} that cause the current groups,
  * courses, and contacts are sent to Grouper.
  */
 @Component(immediate = true, metatype=true)
 public class BatchJMSMessageProducer {
 
 	private static Logger log = LoggerFactory.getLogger(BatchJMSMessageProducer.class);
 
 	protected static final String QUEUE_NAME = "org/sakaiproject/nakamura/grouper/batch";
 
 	@Reference
 	protected ConnectionFactoryService connFactoryService;
 
 	@Reference
 	protected GrouperConfiguration grouperConfiguration;
 
 	@Reference
 	protected SolrServerService solrServerService;
 
 	@Property(boolValue=false)
 	protected static final String PROP_TRANSACTED = "grouper.batch.transacted";
 	protected boolean transacted;
 
 	@Property(boolValue=false)
 	protected static final String PROP_DO_BATCH = "grouper.batch.dobatch";
 	protected boolean doBatch;
 
 	protected static final String DEFAULT_ALL_GROUPS_QUERY = "(resourceType:authorizable AND type:g)";
 	@Property(value=DEFAULT_ALL_GROUPS_QUERY)
 	protected static final String PROP_ALL_GROUPS_QUERY = "grouper.allgroups.query";
 	protected String allGroupsQuery;
 
 	@Modified
 	public void updated(Map<String,Object> props) throws SolrServerException, JMSException{
 		transacted = OsgiUtil.toBoolean(props.get(PROP_TRANSACTED), false);
 		doBatch = OsgiUtil.toBoolean(props.get(PROP_DO_BATCH), false);
 		allGroupsQuery = OsgiUtil.toString(props.get(PROP_ALL_GROUPS_QUERY), DEFAULT_ALL_GROUPS_QUERY);
 
 		if (doBatch){
 			doGroups();
 			doContacts();
 		}
 	}
 
	/**
	 * Query for all groups in the system and post events to the batch queue.
	 * 
	 * @throws SolrServerException
	 * @throws JMSException
	 */
 	private void doGroups() throws SolrServerException, JMSException{
 		int start = 0;
 		int items = 25;
 
 		SolrServer server = solrServerService.getServer();
 		SolrQuery query = new SolrQuery();
 		query.setQuery(allGroupsQuery);
 		query.setStart(start);
 		query.setRows(items);
 
 		QueryResponse response = server.query(query);
 	    long totalResults = response.getResults().getNumFound();
 
 	    List<String> groupIds = new ArrayList<String>();
 	    while (start < totalResults){
 	        query.setStart(start);
 
 	        groupIds.clear();
 	        List<SolrDocument> resultDocs = response.getResults();
 	        for (SolrDocument doc : resultDocs){
 	        	groupIds.add((String)doc.get("id"));
 	        }
  	       	sendGroupMessages(groupIds);
 
	        start += resultDocs.size();
	        log.debug("Found {} groups.", resultDocs.size());
 	    }
 	}
 
 	private void doContacts(){
 		// TODO
 	}
 
 	/**
 	 * Send messages that trigger a batch update
 	 * @param groups
 	 * @throws JMSException
 	 */
 	private void sendGroupMessages(List<String> groupIds) throws JMSException {
 		Connection senderConnection = connFactoryService.getDefaultPooledConnectionFactory().createConnection();
 		Session senderSession = senderConnection.createSession(transacted, Session.CLIENT_ACKNOWLEDGE);
 		Queue squeue = senderSession.createQueue(QUEUE_NAME);
 		MessageProducer producer = senderSession.createProducer(squeue);
 
 		for (String groupId : groupIds){
 			Message msg = senderSession.createMessage();
 			msg.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
 			msg.setJMSType(QUEUE_NAME);
 			msg.setStringProperty("groupId", groupId);
 			msg.setBooleanProperty("grouper:batch", true);
 			producer.send(msg);
 			log.info("Sent: {} {} : messageId {}", new Object[] { QUEUE_NAME, groupId, msg.getJMSMessageID()});
 			log.debug("{} : {}", msg.getJMSMessageID(), msg);
 		}
 
 		try {
 			senderSession.close();
 		}
 		finally {
 			senderSession = null;
 		}
 		try {
 			senderConnection.close();
 		}
 		finally {
 			senderConnection = null;
 		}
 	}
 }
