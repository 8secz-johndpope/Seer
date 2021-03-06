 /*******************************************************************************
  * Copyright 2012 The Linux Box Corporation.
  * 
  * This file is part of Enkive CE (Community Edition).
  * 
  * Enkive CE is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of
  * the License, or (at your option) any later version.
  * 
  * Enkive CE is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU Affero General Public License for more details.
  * 
  * You should have received a copy of the GNU Affero General Public
  * License along with Enkive CE. If not, see
  * <http://www.gnu.org/licenses/>.
  ******************************************************************************/
 package com.linuxbox.enkive.message.search.mongodb;
 
 import static com.linuxbox.enkive.archiver.MesssageAttributeConstants.CC;
 import static com.linuxbox.enkive.archiver.MesssageAttributeConstants.DATE;
 import static com.linuxbox.enkive.archiver.MesssageAttributeConstants.FROM;
 import static com.linuxbox.enkive.archiver.MesssageAttributeConstants.MAIL_FROM;
 import static com.linuxbox.enkive.archiver.MesssageAttributeConstants.RCPT_TO;
 import static com.linuxbox.enkive.archiver.MesssageAttributeConstants.SUBJECT;
 import static com.linuxbox.enkive.archiver.MesssageAttributeConstants.MESSAGE_ID;
 import static com.linuxbox.enkive.archiver.MesssageAttributeConstants.TO;
 import static com.linuxbox.enkive.archiver.mongodb.MongoMessageStoreConstants.ATTACHMENT_ID_LIST;
 import static com.linuxbox.enkive.search.Constants.CONTENT_PARAMETER;
 import static com.linuxbox.enkive.search.Constants.DATE_EARLIEST_PARAMETER;
 import static com.linuxbox.enkive.search.Constants.DATE_LATEST_PARAMETER;
 import static com.linuxbox.enkive.search.Constants.NUMERIC_SEARCH_FORMAT;
 import static com.linuxbox.enkive.search.Constants.RECIPIENT_PARAMETER;
 import static com.linuxbox.enkive.search.Constants.SENDER_PARAMETER;
 import static com.linuxbox.enkive.search.Constants.SUBJECT_PARAMETER;
 import static com.linuxbox.enkive.search.Constants.MESSAGE_ID_PARAMETER;
 
 import java.text.ParseException;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 import java.util.regex.Pattern;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 import com.linuxbox.enkive.docsearch.exception.DocSearchException;
 import com.linuxbox.enkive.message.search.AbstractMessageSearchService;
 import com.linuxbox.enkive.message.search.exception.MessageSearchException;
 import com.mongodb.BasicDBList;
 import com.mongodb.BasicDBObject;
 import com.mongodb.DB;
 import com.mongodb.DBCollection;
 import com.mongodb.DBCursor;
 import com.mongodb.DBObject;
 import com.mongodb.Mongo;
 
 public class MongoMessageSearchService extends AbstractMessageSearchService {
 
 	protected final static Log LOGGER = LogFactory
 			.getLog("com.linuxbox.enkive.searchService.mongodb");
 
 	protected Mongo m = null;
 	protected DB messageDb;
 	protected DBCollection messageColl;
 
 	public MongoMessageSearchService(Mongo m, String dbName, String collName) {
 		this.m = m;
 		messageDb = m.getDB(dbName);
 		messageColl = messageDb.getCollection(collName);
 	}
 
 	public Set<String> searchImpl(HashMap<String, String> fields)
 			throws MessageSearchException {
 		Set<String> messageIds = new HashSet<String>();

 		BasicDBObject query = buildQueryObject(fields);
 		DBCursor results = messageColl.find(query);
 		while (results.hasNext()) {
 			DBObject message = results.next();
 			messageIds.add((String) message.get("_id"));
 		}
 
 		return messageIds;
 	}
 
 	protected BasicDBObject buildQueryObject(HashMap<String, String> fields)
 			throws MessageSearchException {
 
 		BasicDBObject query = new BasicDBObject();
 
 		for (String searchField : fields.keySet()) {
 			if (fields.get(searchField) == null
 					|| fields.get(searchField).isEmpty()
 					|| fields.get(searchField).trim().isEmpty()) {
 				// Do Nothing
 			} else if (searchField.equals(SENDER_PARAMETER)
 					|| searchField.equals(RECIPIENT_PARAMETER)) {
 				BasicDBList addressesQuery = new BasicDBList();
 				if (fields.containsKey(SENDER_PARAMETER)
 						&& fields.get(SENDER_PARAMETER) != null
 						&& !fields.get(SENDER_PARAMETER).isEmpty()) {
 					// Needs to match MAIL_FROM OR FROM
 					BasicDBList senderQuery = new BasicDBList();
 					senderQuery.add(new BasicDBObject(MAIL_FROM, fields
 							.get(SENDER_PARAMETER)));
 					senderQuery.add(new BasicDBObject(FROM, fields
 							.get(SENDER_PARAMETER)));
 
 					addressesQuery.add(new BasicDBObject("$or", senderQuery));
 				}
 				if (fields.containsKey(RECIPIENT_PARAMETER)
 						&& fields.get(RECIPIENT_PARAMETER) != null
 						&& !fields.get(RECIPIENT_PARAMETER).isEmpty()) {
 					// Needs to match TO OR CC OR RCPTO
 					BasicDBList receiverQuery = new BasicDBList();
 					receiverQuery.add(new BasicDBObject(RCPT_TO, fields
 							.get(RECIPIENT_PARAMETER)));
 					receiverQuery.add(new BasicDBObject(TO, fields
 							.get(RECIPIENT_PARAMETER)));
 					receiverQuery.add(new BasicDBObject(CC, fields
 							.get(RECIPIENT_PARAMETER)));
 
 					addressesQuery.add(new BasicDBObject("$or", receiverQuery));
 				}
 				query.put("$and", addressesQuery);
 			} else if (searchField.equals(DATE_EARLIEST_PARAMETER)
 					|| searchField.equals(DATE_LATEST_PARAMETER)) {
 				BasicDBObject dateQuery = new BasicDBObject();
 				if (fields.containsKey(DATE_EARLIEST_PARAMETER)
 						&& fields.get(DATE_EARLIEST_PARAMETER) != null
 						&& !fields.get(DATE_EARLIEST_PARAMETER).isEmpty()) {
 					try {
 						Date dateEarliest = NUMERIC_SEARCH_FORMAT.parse(fields
 								.get(DATE_EARLIEST_PARAMETER));
 						dateQuery.put("$gte", dateEarliest);
 					} catch (ParseException e) {
 						if (LOGGER.isWarnEnabled())
 							LOGGER.warn("Could not parse earliest date submitted to search - "
 									+ fields.get(DATE_EARLIEST_PARAMETER));
 					}
 				}
 				if (fields.containsKey(DATE_LATEST_PARAMETER)
 						&& fields.get(DATE_LATEST_PARAMETER) != null
 						&& !fields.get(DATE_LATEST_PARAMETER).isEmpty()) {
 					try {
 						Date dateLatest = NUMERIC_SEARCH_FORMAT.parse(fields
 								.get(DATE_LATEST_PARAMETER));
 						dateQuery.put("$lte", dateLatest);
 					} catch (ParseException e) {
 						if (LOGGER.isWarnEnabled())
 							LOGGER.warn("Could not parse latest date submitted to search - "
 									+ fields.get(DATE_LATEST_PARAMETER));
 					}
 				}
 				query.put(DATE, dateQuery);
 			} else if (searchField.equals(SUBJECT_PARAMETER)) {
 				Pattern subjectRegex = Pattern.compile(fields.get(searchField),
 						Pattern.CASE_INSENSITIVE);
 				query.put(SUBJECT, subjectRegex);
 			} else if (searchField.equals(MESSAGE_ID_PARAMETER)) {
				Pattern messageIdRegex = Pattern.compile(fields.get(searchField),
						Pattern.CASE_INSENSITIVE);
 				query.put(MESSAGE_ID, messageIdRegex);
 			} else if (searchField.equals(CONTENT_PARAMETER)) {
 				try {
 					List<String> attachments = docSearchService.search(fields
 							.get(searchField));
 					BasicDBList attachmentQuery = new BasicDBList();
					for (String attachment : attachments)
 						attachmentQuery.add(new BasicDBObject(
								ATTACHMENT_ID_LIST, attachment));
 					query.put("$or", attachmentQuery);
 					// Need to search attachment ids field here, separated by OR
 				} catch (DocSearchException e) {
 					throw new MessageSearchException(
 							"Exception occurred searching Content", e);
 				}
 			}
 		}
 		return query;
 	}
 
 	@Override
 	public boolean cancelAsyncSearch(String searchId)
 			throws MessageSearchException {
 		throw new MessageSearchException("Unimplemented");
 	}
 
 }
