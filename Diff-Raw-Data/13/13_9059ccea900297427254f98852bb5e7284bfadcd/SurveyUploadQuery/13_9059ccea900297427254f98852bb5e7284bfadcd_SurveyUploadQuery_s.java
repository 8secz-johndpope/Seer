 /*******************************************************************************
  * Copyright 2012 The Regents of the University of California
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *   http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  ******************************************************************************/
 package org.ohmage.query.impl;
 
 import java.awt.Graphics2D;
 import java.awt.RenderingHints;
 import java.awt.image.BufferedImage;
 import java.io.File;
 import java.io.FilenameFilter;
 import java.io.IOException;
 import java.sql.Connection;
 import java.sql.PreparedStatement;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.sql.Timestamp;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Date;
 import java.util.List;
 import java.util.Map;
 import java.util.regex.Pattern;
 
 import javax.imageio.ImageIO;
 import javax.sql.DataSource;
 
 import org.apache.log4j.Logger;
 import org.json.JSONException;
 import org.ohmage.cache.PreferenceCache;
 import org.ohmage.domain.Location;
 import org.ohmage.domain.campaign.PromptResponse;
 import org.ohmage.domain.campaign.RepeatableSet;
 import org.ohmage.domain.campaign.RepeatableSetResponse;
 import org.ohmage.domain.campaign.Response;
 import org.ohmage.domain.campaign.SurveyResponse;
 import org.ohmage.domain.campaign.response.PhotoPromptResponse;
 import org.ohmage.exception.CacheMissException;
 import org.ohmage.exception.DataAccessException;
 import org.ohmage.query.ISurveyUploadQuery;
 import org.ohmage.request.JsonInputKeys;
 import org.ohmage.util.TimeUtils;
 import org.springframework.dao.DataIntegrityViolationException;
 import org.springframework.jdbc.core.PreparedStatementCreator;
 import org.springframework.jdbc.datasource.DataSourceTransactionManager;
 import org.springframework.jdbc.support.GeneratedKeyHolder;
 import org.springframework.jdbc.support.KeyHolder;
 import org.springframework.transaction.PlatformTransactionManager;
 import org.springframework.transaction.TransactionException;
 import org.springframework.transaction.TransactionStatus;
 import org.springframework.transaction.support.DefaultTransactionDefinition;
 
 /**
  * Persists a survey upload (potentially containing many surveys) into the db.
  * 
  * @author Joshua Selsky
  */
 public class SurveyUploadQuery extends AbstractUploadQuery implements ISurveyUploadQuery {
 	// The current directory to which the next image should be saved.
 	private static File currLeafDirectory;
 	
 	private static final Pattern IMAGE_DIRECTORY_PATTERN = Pattern.compile("[0-9]+");
 	
 	public static final String IMAGE_STORE_FORMAT = "png";
 	public static final String IMAGE_SCALED_EXTENSION = "-s";
 	private static final double IMAGE_SCALED_MAX_DIMENSION = 150.0;
 	
 	/**
 	 * Filters the sub-directories in a directory to only return those that
 	 * match the regular expression matcher for directories.
 	 * 
 	 * @author Joshua Selsky
 	 */
 	private static final class DirectoryFilter implements FilenameFilter {
 		/**
 		 * Returns true iff the filename is appropriate for the regular
 		 * expression. 
 		 */
 		public boolean accept(File f, String name) {
 			return IMAGE_DIRECTORY_PATTERN.matcher(name).matches();
 		}
 	}
 
 	private static final Logger LOGGER = Logger.getLogger(SurveyUploadQuery.class);
 	
 	private static final String SQL_INSERT_SURVEY_RESPONSE =
 		"INSERT into survey_response " +
 		"SET uuid = ?, " +
 		"user_id = (SELECT id from user where username = ?), " +
 		"campaign_id = (SELECT id from campaign where urn = ?), " +
 		"epoch_millis = ?, " +
 		"phone_timezone = ?, " +
 		"location_status = ?, " +
 		"location = ?, " +
 		"survey_id = ?, " +
 		"survey = ?, " +
 		"client = ?, " +
 		"upload_timestamp = ?, " +
 		"launch_context = ?, " +
 		"privacy_state_id = (SELECT id FROM survey_response_privacy_state WHERE privacy_state = ?)";
 		
 	private static final String SQL_INSERT_PROMPT_RESPONSE =
 		"INSERT into prompt_response " +
         "(survey_response_id, repeatable_set_id, repeatable_set_iteration," +
         "prompt_type, prompt_id, response) " +
         "VALUES (?,?,?,?,?,?)";
 	
 	// Inserts an images information into the url_based_resource table.
 	private static final String SQL_INSERT_IMAGE = 
 		"INSERT INTO url_based_resource(user_id, client, uuid, url) " +
 		"VALUES (" +
 			"(" +	// user_id
 				"SELECT id " +
 				"FROM user " +
 				"WHERE username = ?" +
 			")," +
 			"?," +	// client
 			"?," +	// uuid
 			"?" +	// url
 		")";
 	
 	/**
 	 * Creates this object.
 	 * 
 	 * @param dataSource The DataSource to use when querying the database.
 	 */
 	private SurveyUploadQuery(DataSource dataSource) {
 		super(dataSource);
 	}
 	
 	/**
 	 * Inserts surveys into survey_response, prompt_response,
 	 * and url_based_resource (if the payload contains images).
 	 * Any images are also persisted to the file system. The entire persistence
 	 * process is wrapped in one giant transaction.
 	 * 
 	 * @param user  The owner of the survey upload.
 	 * @param client  The software client that performed the upload.
 	 * @param campaignUrn  The campaign for the survey upload.
 	 * @param surveyUploadList  The surveys to persist.
 	 * @param bufferedImageMap  The images to persist.
 	 * @return Returns a List of Integers representing the ids of duplicate
 	 * surveys.
 	 * @throws DataAccessException  If any IO error occurs.
 	 */
 	public List<Integer> insertSurveys(final String username,
 			                                  final String client,
 			                                  final String campaignUrn,
 			                                  final List<SurveyResponse> surveyUploadList,
 			                                  final Map<String, BufferedImage> bufferedImageMap)  
 		throws DataAccessException {
 		
 		List<Integer> duplicateIndexList = new ArrayList<Integer>();
 		int numberOfSurveys = surveyUploadList.size();
 		
 		// The following variables are used in logging messages when errors occur
 		SurveyResponse currentSurveyResponse = null;
 		PromptResponse currentPromptResponse = null;
 		String currentSql = null;
 		
 		List<File> regularImageList = new ArrayList<File>();
 		List<File> scaledImageList = new ArrayList<File>();
 		
 		// Wrap all of the inserts in a transaction 
 		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
 		def.setName("survey upload");
 		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(getDataSource());
 		TransactionStatus status = transactionManager.getTransaction(def); // begin transaction
 		
 		// Use a savepoint to handle nested rollbacks if duplicates are found
 		Object savepoint = status.createSavepoint();
 		
 		try { // handle TransactionExceptions
 			
 			for(int surveyIndex = 0; surveyIndex < numberOfSurveys; surveyIndex++) { 
 				
 				 try { // handle DataAccessExceptions
 					
 					final SurveyResponse surveyUpload = surveyUploadList.get(surveyIndex);
 					currentSurveyResponse = surveyUpload; 
 					currentSql = SQL_INSERT_SURVEY_RESPONSE;
 			
 					KeyHolder idKeyHolder = new GeneratedKeyHolder();
 					
 					// First, insert the survey
 					
 					getJdbcTemplate().update(
 						new PreparedStatementCreator() {
 							public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
 								PreparedStatement ps 
 									= connection.prepareStatement(SQL_INSERT_SURVEY_RESPONSE, Statement.RETURN_GENERATED_KEYS);
 								
 								String locationString = null;
 								Location location = surveyUpload.getLocation();
 								if(location != null) {
 									try {
 										locationString = 
 												location.toJson(false).toString();
 									}
 									catch(JSONException e) {
 										throw new SQLException(e);
 									}
 								}
 								
 								ps.setString(1, surveyUpload.getSurveyResponseId().toString());
 								ps.setString(2, username);
 								ps.setString(3, campaignUrn);
 								ps.setLong(4, surveyUpload.getTime());
 								ps.setString(5, surveyUpload.getTimezone().getID());
 								ps.setString(6, surveyUpload.getLocationStatus().toString());
 								ps.setString(7, locationString);
 								ps.setString(8, surveyUpload.getSurvey().getId());
 								try {
 									ps.setString(9, surveyUpload.toJson(false, false, false, false, true, true, true, true, true, false, false, true, true, true, true, false, false).toString());
 								}
 								catch(JSONException e) {
 									throw new SQLException(
 											"Couldn't create the JSON.",
 											e);
 								}
 								ps.setString(10, client);
 								ps.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
 								try {
 									ps.setString(12, surveyUpload.getLaunchContext().toJson(true).toString());
 								}
 								catch(JSONException e) {
 									throw new SQLException(
 											"Couldn't create the JSON.",
 											e);
 								}
 								try {
 									ps.setString(13, PreferenceCache.instance().lookup(PreferenceCache.KEY_DEFAULT_SURVEY_RESPONSE_SHARING_STATE));
 								} catch (CacheMissException e) {
 									throw new SQLException(
 											"Error reading from the cache.", 
 											e);
 								}
 								return ps;
 							}
 						},
 						idKeyHolder
 					);
 					
 					savepoint = status.createSavepoint();
 					
 					final Number surveyResponseId = idKeyHolder.getKey(); // the primary key on the survey_response table for the 
 					                                                      // just-inserted survey
 					currentSql = SQL_INSERT_PROMPT_RESPONSE;
 					
 					// Now insert each prompt response from the survey
 					Collection<Response> promptUploadList = surveyUpload.getResponses().values();
 					
 					createPromptResponse(username, client, surveyResponseId, 
 							regularImageList, scaledImageList, 
 							promptUploadList, null, bufferedImageMap, 
 							transactionManager, status);
 					
 				} catch (DataIntegrityViolationException dive) { // a unique index exists only on the survey_response table
 					
 					if(isDuplicate(dive)) {
 						 
 						LOGGER.debug("Found a duplicate survey upload message for user " + username);
 						
 						duplicateIndexList.add(surveyIndex);
 						status.rollbackToSavepoint(savepoint);
 						
 					} 
 					else {
 					
 						// Some other integrity violation occurred - bad!! All 
 						// of the data to be inserted must be validated before 
 						// this query runs so there is either missing validation 
 						// or somehow an auto_incremented key has been duplicated.
 						
 						LOGGER.error("Caught DataAccessException", dive);
 						logErrorDetails(currentSurveyResponse, currentPromptResponse, currentSql, username, campaignUrn);
 						if(! regularImageList.isEmpty()) {
 							for(File f : regularImageList) {
 								f.delete();
 							}
 						}
 						if(! scaledImageList.isEmpty()) {
 							for(File f : scaledImageList) {
 								f.delete();
 							}
 						}
 						rollback(transactionManager, status);
 						throw new DataAccessException(dive);
 					}
 						
 				} catch (org.springframework.dao.DataAccessException dae) { 
 					
 					// Some other database problem happened that prevented
                     // the SQL from completing normally.
 					
 					LOGGER.error("caught DataAccessException", dae);
 					logErrorDetails(currentSurveyResponse, currentPromptResponse, currentSql, username, campaignUrn);
 					if(! regularImageList.isEmpty()) {
 						for(File f : regularImageList) {
 							f.delete();
 						}
 					}
 					if(! scaledImageList.isEmpty()) {
 						for(File f : scaledImageList) {
 							f.delete();
 						}
 					}
 					rollback(transactionManager, status);
 					throw new DataAccessException(dae);
 				} 
 				
 			}
 			
 			// Finally, commit the transaction
 			transactionManager.commit(status);
 			LOGGER.info("Completed survey message persistence");
 		} 
 		
 		catch (TransactionException te) { 
 			
 			LOGGER.error("failed to commit survey upload transaction, attempting to rollback", te);
 			rollback(transactionManager, status);
 			if(! regularImageList.isEmpty()) {
 				for(File f : regularImageList) {
 					f.delete();
 				}
 			}
 			if(! scaledImageList.isEmpty()) {
 				for(File f : scaledImageList) {
 					f.delete();
 				}
 			}
 			logErrorDetails(currentSurveyResponse, currentPromptResponse, currentSql, username, campaignUrn);
 			throw new DataAccessException(te);
 		}
 		
 		LOGGER.info("Finished inserting survey responses and any associated images into the database and the filesystem.");
 		return duplicateIndexList;
 	}
 	
 	/**
 	 * Attempts to rollback a transaction. 
 	 */
 	private void rollback(PlatformTransactionManager transactionManager, TransactionStatus transactionStatus) 
 		throws DataAccessException {
 		
 		try {
 			
 			LOGGER.error("rolling back a failed survey upload transaction");
 			transactionManager.rollback(transactionStatus);
 			
 		} catch (TransactionException te) {
 			
 			LOGGER.error("failed to rollback survey upload transaction", te);
 			throw new DataAccessException(te);
 		}
 	}
 	
 	private void logErrorDetails(SurveyResponse surveyResponse, PromptResponse promptResponse, String sql, String username,
 			String campaignUrn) {
 	
 		StringBuilder error = new StringBuilder();
 		error.append("\nAn error occurred when attempting to insert survey responses for user ");
 		error.append(username);
 		error.append(" in campaign ");
 		error.append(campaignUrn);
 		error.append(".\n");
 		error.append("The SQL statement at hand was ");
 		error.append(sql);
 		error.append("\n The survey response at hand was ");
 		error.append(surveyResponse);
 		error.append("\n The prompt response at hand was ");
 		error.append(promptResponse);
 		
 		LOGGER.error(error.toString());
 	}
 	
 	/**
 	 * Copied directly from ImageQueries.
 	 * 
 	 * Gets the directory to which a image should be saved. This should be used
 	 * instead of accessing the class-level variable directly as it handles the
 	 * creation of new folders and the checking that the current
 	 * folder is not full.
 	 * 
 	 * @return A File object for where a document should be written.
 	 */
 	private File getDirectory() throws DataAccessException {
 		// Get the maximum number of items in a directory.
 		int numFilesPerDirectory;
 		try {
 			numFilesPerDirectory = Integer.decode(PreferenceCache.instance().lookup(PreferenceCache.KEY_MAXIMUM_NUMBER_OF_FILES_PER_DIRECTORY));
 		}
 		catch(CacheMissException e) {
 			throw new DataAccessException("Preference cache doesn't know about 'known' key: " + PreferenceCache.KEY_MAXIMUM_NUMBER_OF_FILES_PER_DIRECTORY, e);
 		}
 		catch(NumberFormatException e) {
 			throw new DataAccessException("Stored value for key '" + PreferenceCache.KEY_MAXIMUM_NUMBER_OF_FILES_PER_DIRECTORY + "' is not decodable as a number.", e);
 		}
 		
 		// If the leaf directory was never initialized, then we should do
 		// that. Note that the initialization is dumb in that it will get to
 		// the end of the structure and not check to see if the leaf node is
 		// full.
 		if(currLeafDirectory == null) {
 			init(numFilesPerDirectory);
 		}
 		
 		File[] documents = currLeafDirectory.listFiles();
 		// If the 'currLeafDirectory' directory is full, traverse the tree and
 		// find a new directory.
 		if(documents.length >= numFilesPerDirectory) {
 			getNewDirectory(numFilesPerDirectory);
 		}
 		
 		return currLeafDirectory;
 	}
 	
 	/**
 	 * Initializes the directory structure by drilling down to the leaf
 	 * directory with each step choosing the directory with the largest
 	 * integer value.
 	 */
 	private synchronized void init(int numFilesPerDirectory) throws DataAccessException {
 		try {
 			// If the current leaf directory has been set, we weren't the
 			// first to call init(), so we can just back out.
 			if(currLeafDirectory != null) {
 				return;
 			}
 			
 			// Get the root directory from the preference cache based on the
 			// key.
 			String rootFile;
 			try {
 				rootFile = PreferenceCache.instance().lookup(PreferenceCache.KEY_IMAGE_DIRECTORY);
 			}
 			catch(CacheMissException e) {
 				throw new DataAccessException("Preference cache doesn't know about 'known' key: " + PreferenceCache.KEY_IMAGE_DIRECTORY, e);
 			}
 			File rootDirectory = new File(rootFile);
 			if(! rootDirectory.exists()) {
 				throw new DataAccessException("The root file doesn't exist suggesting an incomplete installation: " + rootFile);
 			}
 			else if(! rootDirectory.isDirectory()) {
 				throw new DataAccessException("The root file isn't a directory.");
 			}
 			
 			// Get the number of folders deep that documents are stored.
 			int fileDepth;
 			try {
 				fileDepth = Integer.decode(PreferenceCache.instance().lookup(PreferenceCache.KEY_FILE_HIERARCHY_DEPTH));
 			}
 			catch(CacheMissException e) {
 				throw new DataAccessException("Preference cache doesn't know about 'known' key: " + PreferenceCache.KEY_FILE_HIERARCHY_DEPTH, e);
 			}
 			catch(NumberFormatException e) {
 				throw new DataAccessException("Stored value for key '" + PreferenceCache.KEY_FILE_HIERARCHY_DEPTH + "' is not decodable as a number.", e);
 			}
 			
 			DirectoryFilter directoryFilter = new DirectoryFilter();
 			File currDirectory = rootDirectory;
 			for(int currDepth = 0; currDepth < fileDepth; currDepth++) {
 				// Get the list of directories in the current directory.
 				File[] currDirectories = currDirectory.listFiles(directoryFilter);
 				
 				// If there aren't any, create the first subdirectory in this
 				// directory.
 				if(currDirectories.length == 0) {
 					String newFolderName = directoryNameBuilder(0, numFilesPerDirectory);
 					currDirectory = new File(currDirectory.getAbsolutePath() + "/" + newFolderName);
 					currDirectory.mkdir();
 				}
 				// If the directory is overly full, step back up in the
 				// structure. This should never happen, as it indicates that
 				// there is an overflow in the structure.
 				else if(currDirectories.length > numFilesPerDirectory) {
 					LOGGER.warn("Too many subdirectories in: " + currDirectory.getAbsolutePath());
 					
 					// Take a step back in our depth.
 					currDepth--;
 					
 					// If, while backing up the tree, we back out of the root
 					// directory, we have filled up the space.
 					if(currDepth < 0) {
 						LOGGER.error("Image directory structure full!");
 						throw new DataAccessException("Image directory structure full!");
 					}
 
 					// Get the next parent and the current directory to it.
 					int nextDirectoryNumber = Integer.decode(currDirectory.getName()) + 1;
 					currDirectory = new File(currDirectory.getParent() + "/" + nextDirectoryNumber);
 					
 					// If the directory already exists, then there is either a
 					// concurrency issue or someone else is adding files.
 					// Either way, this shouldn't happen.
 					if(currDirectory.exists()) {
 						LOGGER.error("Somehow the 'new' directory already exists. This should be looked into: " + currDirectory.getAbsolutePath());
 					}
 					// Otherwise, create the directory.
 					else {
 						currDirectory.mkdir();
 					}
 				}
 				// Drill down to the directory with the largest, numeric value.
 				else {
 					currDirectory = getLargestSubfolder(currDirectories);
 				}
 			}
 			
 			// After we have found a suitable directory, set it.
 			currLeafDirectory = currDirectory;
 		}
 		catch(SecurityException e) {
 			throw new DataAccessException("The current process doesn't have sufficient permiossions to create new directories.", e);
 		}
 	}
 	
 	/**
 	 * Checks again that the current leaf directory is full. If it is not, then
 	 * it will just back out under the impression someone else made the change.
 	 * If it is, it will go up and down the directory tree structure to find a
 	 * new leaf node in which to store new files.
 	 * 
 	 * @param numFilesPerDirectory The maximum allowed number of files in a
 	 * 							   leaf directory and the maximum allowed
 	 * 							   number of directories in the branches.
 	 */
 	private synchronized void getNewDirectory(int numFilesPerDirectory) throws DataAccessException {
 		try {
 			// Make sure that this hasn't changed because another thread may
 			// have preempted us and already changed the current leaf
 			// directory.
 			File[] files = currLeafDirectory.listFiles();
 			if(files.length < numFilesPerDirectory) {
 				return;
 			}
 			
 			// Get the root directory from the preference cache based on the
 			// key.
 			String rootFile;
 			try {
 				rootFile = PreferenceCache.instance().lookup(PreferenceCache.KEY_IMAGE_DIRECTORY);
 			}
 			catch(CacheMissException e) {
 				throw new DataAccessException("Preference cache doesn't know about 'known' key: " + PreferenceCache.KEY_IMAGE_DIRECTORY, e);
 			}
 			File rootDirectory = new File(rootFile);
 			if(! rootDirectory.exists()) {
 				throw new DataAccessException("The root file doesn't exist suggesting an incomplete installation: " + rootFile);
 			}
 			else if(! rootDirectory.isDirectory()) {
 				throw new DataAccessException("The root file isn't a directory.");
 			}
 			String absoluteRootDirectory = rootDirectory.getAbsolutePath();
 			
 			// A filter when listing a set of directories for a file.
 			DirectoryFilter directoryFilter = new DirectoryFilter();
 			
 			// A local File to use while we are searching to not confuse other
 			// threads.
 			File newDirectory = currLeafDirectory;
 			
 			// A flag to indicate when we are done looking for a directory.
 			boolean lookingForDirectory = true;
 			
 			// The number of times we stepped up in the hierarchy.
 			int depth = 0;
 			
 			// While we are still looking for a suitable directory,
 			while(lookingForDirectory) {
 				// Get the current directory's name which should be a Long
 				// value.
 				long currDirectoryName;
 				try {
 					String dirName = newDirectory.getName();
 					while(dirName.startsWith("0")) {
 						dirName = dirName.substring(1);
 					}
 					if("".equals(dirName)) {
 						currDirectoryName = 0;
 					}
 					else {
 						currDirectoryName = Long.decode(dirName);
 					}
 				}
 				catch(NumberFormatException e) {
 					if(newDirectory.getAbsolutePath().equals(absoluteRootDirectory)) {
 						throw new DataAccessException("Document structure full!", e);
 					}
 					else {
 						throw new DataAccessException("Potential breach of document structure.", e);
 					}
 				}
 				
 				// Move the pointer up a directory.
 				newDirectory = new File(newDirectory.getParent());
 				// Get the list of files in the parent.
 				File[] parentDirectoryFiles = newDirectory.listFiles(directoryFilter);
 				
 				// If this directory has room for a new subdirectory,
 				if(parentDirectoryFiles.length < numFilesPerDirectory) {
 					// Increment the name for the next subfolder.
 					currDirectoryName++;
 					
 					// Create the new subfolder.
 					newDirectory = new File(newDirectory.getAbsolutePath() + "/" + directoryNameBuilder(currDirectoryName, numFilesPerDirectory));
 					newDirectory.mkdir();
 					
 					// Continue drilling down to reach an appropriate leaf
 					// node.
 					while(depth > 0) {
 						newDirectory = new File(newDirectory.getAbsolutePath() + "/" + directoryNameBuilder(0, numFilesPerDirectory));
 						newDirectory.mkdir();
 						
 						depth--;
 					}
 					
 					lookingForDirectory = false;
 				}
 				// If the parent is full as well, increment the depth unless
 				// we are already at the parent. If we are at the parent, then
 				// we cannot go up any further and have exhausted the
 				// directory structure.
 				else
 				{
 					if(newDirectory.getAbsoluteFile().equals(absoluteRootDirectory)) {
 						throw new DataAccessException("Document structure full!");
 					}
 					else {
 						depth++;
 					}
 				}
 			}
 			
 			currLeafDirectory = newDirectory;
 		}
 		catch(NumberFormatException e) {
 			throw new DataAccessException("Could not decode a directory name as an integer.", e);
 		}
 	}
 	
 	/**
 	 * Builds the name of a folder by prepending zeroes where necessary and
 	 * converting the name into a String.
 	 * 
 	 * @param name The name of the file as an integer.
 	 * 
 	 * @param numFilesPerDirectory The maximum number of files allowed in the
 	 * 							   directory used to determine how many zeroes
 	 * 							   to prepend.
 	 * 
 	 * @return A String representing the directory name based on the
 	 * 		   parameters.
 	 */
 	private String directoryNameBuilder(long name, int numFilesPerDirectory) {
 		int nameLength = String.valueOf(name).length();
 		int maxLength = new Double(Math.log10(numFilesPerDirectory)).intValue();
 		int numberOfZeros = maxLength - nameLength;
 		
 		StringBuilder builder = new StringBuilder();
 		for(int i = 0; i < numberOfZeros; i++) {
 			builder.append("0");
 		}
 		builder.append(String.valueOf(name));
 		
 		return builder.toString();
 	}
 	
 	/**
 	 * Sorts the directories and returns the one whose alphanumeric value is
 	 * the greatest.
 	 * 
 	 * This will work with any naming for directories, so it is the caller's
 	 * responsibility to ensure that the list of directories are what they
 	 * want them to be.
 	 *  
 	 * @param directories The list of directories whose largest alphanumeric
 	 * 					  value is desired.
 	 * 
 	 * @return Returns the File whose path and name has the largest
 	 * 		   alphanumeric value.
 	 */
 	private File getLargestSubfolder(File[] directories) {
 		Arrays.sort(directories);
 		
 		return directories[directories.length - 1];
 	}
 	
 	private void createPromptResponse(
 			final String username, final String client,
 			final Number surveyResponseId,
 			final List<File> regularImageList, 
 			final List<File> scaledImageList,
 			final Collection<Response> promptUploadList,
 			final Integer repeatableSetIteration,
             final Map<String, BufferedImage> bufferedImageMap,
             final DataSourceTransactionManager transactionManager,
             final TransactionStatus status) 
 			throws DataAccessException{
 		
 		for(Response response : promptUploadList) {
 			if(response instanceof RepeatableSetResponse) {
 				Map<Integer, Map<Integer, Response>> iterationToResponse =
 					((RepeatableSetResponse) response).getResponseGroups();
 				
 				for(Integer iteration : iterationToResponse.keySet()) {
 					createPromptResponse(username, client, surveyResponseId, 
 							regularImageList, scaledImageList,
 							iterationToResponse.get(iteration).values(), 
 							iteration, bufferedImageMap, 
 							transactionManager, status
 						);
 				}
 				continue;
 			}
 			final PromptResponse promptResponse = (PromptResponse) response;
 			
 			getJdbcTemplate().update(
 				new PreparedStatementCreator() {
 					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
 						PreparedStatement ps 
 							= connection.prepareStatement(SQL_INSERT_PROMPT_RESPONSE);
 						ps.setLong(1, surveyResponseId.longValue());
 						
 						RepeatableSet parent = promptResponse.getPrompt().getParent();
 						if(parent == null) {
 							ps.setNull(2, java.sql.Types.NULL);
 							ps.setNull(3, java.sql.Types.NULL);
 						}
 						else {
 							ps.setString(2, parent.getId());
 							ps.setInt(3, repeatableSetIteration);
 						}
 						ps.setString(4, promptResponse.getPrompt().getType().toString());
 						ps.setString(5, promptResponse.getPrompt().getId());
 						
 						Object response = promptResponse.getResponse();
 						if(response instanceof Date) {
 							ps.setString(6, TimeUtils.getIso8601DateTimeString((Date) response));
 						}
 						else {
 							ps.setString(6, response.toString());
 						}
 						
 						return ps;
 					}
 				}
 			);
 			
 			if(promptResponse instanceof PhotoPromptResponse) {
 				// Grab the associated image and save it
 				String imageId = promptResponse.getResponse().toString();
 				BufferedImage imageContents = bufferedImageMap.get(imageId);
 				
 				if(! JsonInputKeys.PROMPT_SKIPPED.equals(imageId) && ! JsonInputKeys.PROMPT_NOT_DISPLAYED.equals(imageId)) {
 					
 					// getDirectory() is used as opposed to accessing the current leaf
 					// directory class variable as it will do sanitation in case it hasn't
 					// been initialized or is full.
 					File imageDirectory = getDirectory();
 					File regularImage = new File(imageDirectory.getAbsolutePath() + "/" + imageId);
 					regularImageList.add(regularImage);
 					File scaledImage = new File(imageDirectory.getAbsolutePath() + "/" + imageId + IMAGE_SCALED_EXTENSION);
 					scaledImageList.add(scaledImage);
 					
 					// Write the original to the file system.
 					try {
 						ImageIO.write(imageContents, IMAGE_STORE_FORMAT, regularImage);
 					}
 					catch(IOException e) {
 						
 						rollback(transactionManager, status);
 						throw new DataAccessException("Error writing the regular image to the system.", e);
 					}
 					catch(IllegalArgumentException e) {
 						rollback(transactionManager, status);
 						throw new DataAccessException("The image contents are null.", e);
 					}
 					
 					// Write the scaled image to the file system.
 					try {
 						// Get the percentage to scale the image.
 						Double scalePercentage;
 						if(imageContents.getWidth() > imageContents.getHeight()) {
 							scalePercentage = IMAGE_SCALED_MAX_DIMENSION / imageContents.getWidth();
 						}
 						else {
 							scalePercentage = IMAGE_SCALED_MAX_DIMENSION / imageContents.getHeight();
 						}
 						
 						// Calculate the scaled image's width and height.
 						int width = (new Double(imageContents.getWidth() * scalePercentage)).intValue();
 						int height = (new Double(imageContents.getHeight() * scalePercentage)).intValue();
 						
 						// Create the new image of the same type as the original and of the
 						// scaled dimensions.
 						BufferedImage scaledContents = new BufferedImage(width, height, imageContents.getType());
 						
 						// Paint the original image onto the scaled canvas.
 						Graphics2D graphics2d = scaledContents.createGraphics();
 						graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
 						graphics2d.drawImage(imageContents, 0, 0, width, height, null);
 						
 						// Cleanup.
 						graphics2d.dispose();
 						
 						// Write the scaled image to the filesystem.
 						ImageIO.write(scaledContents, IMAGE_STORE_FORMAT, scaledImage);
 					}
 					catch(IOException e) {
 						regularImage.delete();
 						rollback(transactionManager, status);
 						throw new DataAccessException("Error writing the scaled image to the system.", e);
 					}
 					
 					// Get the image's URL.
 					String url = "file://" + regularImage.getAbsolutePath();
 					// Insert the image URL into the database.
 					try {
 						getJdbcTemplate().update(
 								SQL_INSERT_IMAGE, 
 								new Object[] { username, client, imageId, url }
 							);
 					}
 					catch(org.springframework.dao.DataAccessException e) {
 						regularImage.delete();
 						scaledImage.delete();
 						transactionManager.rollback(status);
 						throw new DataAccessException("Error executing SQL '" + SQL_INSERT_IMAGE + "' with parameters: " +
 								username + ", " + client + ", " + imageId + ", " + url, e);
 					}
 				}
 			}
 		}
 	}
 }
