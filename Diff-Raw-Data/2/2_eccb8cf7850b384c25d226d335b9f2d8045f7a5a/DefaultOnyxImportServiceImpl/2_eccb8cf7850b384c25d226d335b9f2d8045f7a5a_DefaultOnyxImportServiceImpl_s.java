 /*******************************************************************************
  * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
  * 
  * This program and the accompanying materials
  * are made available under the terms of the GNU Public License v3.0.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  ******************************************************************************/
 package org.obiba.opal.core.service.impl;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Date;
 import java.util.List;
 
 import javax.security.auth.callback.Callback;
 import javax.security.auth.callback.PasswordCallback;
 
 import org.obiba.onyx.engine.variable.Variable;
 import org.obiba.onyx.engine.variable.VariableData;
 import org.obiba.onyx.engine.variable.VariableDataSet;
 import org.obiba.onyx.engine.variable.impl.DefaultVariablePathNamingStrategy;
 import org.obiba.onyx.engine.variable.util.VariableFinder;
 import org.obiba.onyx.engine.variable.util.VariableStreamer;
 import org.obiba.onyx.util.data.Data;
 import org.obiba.opal.core.datasource.onyx.DecryptingOnyxDataInputStrategy;
 import org.obiba.opal.core.datasource.onyx.IOnyxDataInputStrategy;
 import org.obiba.opal.core.datasource.onyx.OnyxDataInputContext;
 import org.obiba.opal.core.datasource.onyx.OpalKeyStore;
 import org.obiba.opal.core.service.IParticipantKeyReadRegistry;
 import org.obiba.opal.core.service.IParticipantKeyWriteRegistry;
 import org.obiba.opal.core.service.OnyxImportService;
 import org.springframework.transaction.annotation.Transactional;
 
 import com.sun.security.auth.callback.TextCallbackHandler;
 
 /**
  * Default <code>OnyxImportService</code> implementation.
  */
 @Transactional
 public class DefaultOnyxImportServiceImpl implements OnyxImportService {
 
   private static final String VARIABLES_FILE = "variables.xml";
 
   private IParticipantKeyWriteRegistry participantKeyWriteRegistry;
 
   private IOnyxDataInputStrategy dataInputStrategy;
 
   /** The unique opal identifying key for the current participant being imported. */
   private String opalKey;
 
   public void setParticipantKeyWriteRegistry(IParticipantKeyWriteRegistry participantKeyWriteRegistry) {
     this.participantKeyWriteRegistry = participantKeyWriteRegistry;
   }
 
   public void setDataInputStrategy(IOnyxDataInputStrategy dataInputStrategy) {
     this.dataInputStrategy = dataInputStrategy;
   }
 
   public void importData(String username, String password) {
     // TODO Auto-generated method stub
     System.out.println("<importData(user: " + username + ", password: " + password + ")>");
   }
 
   public void importData(String username, String password, Date date, String site, List<String> tags) {
     // TODO Auto-generated method stub
     System.out.println("<importData(user: " + username + ", password: " + password + ", date: " + date.toString() + ", site: " + site + ", tags: " + tags + ")>");
   }
 
   public void importData(String username, String password, List<String> tags, File source) {
     String keystorePassword = promptForPassword("Enter keystore password: ");
 
     String keyPassword = promptForPassword("Enter key password (RETURN if same as keystore password): ");
     if(keyPassword == null) {
       keyPassword = keystorePassword;
     }
 
     // Create the dataInputContext, based on the specified command-line options.
     OnyxDataInputContext dataInputContext = new OnyxDataInputContext();
     dataInputContext.setKeyProviderArg(OpalKeyStore.KEYSTORE_PASSWORD_ARGKEY, keystorePassword);
     dataInputContext.setKeyProviderArg(OpalKeyStore.KEY_PASSWORD_ARGKEY, keyPassword);
     dataInputContext.setSource(source.getPath());
 
     dataInputStrategy.prepare(dataInputContext);
 
     Variable variableRoot = null;
     for(String entryName : dataInputStrategy.listEntries()) {
       if(entryName.equalsIgnoreCase(VARIABLES_FILE)) {
         variableRoot = getInputStreamFromFile(entryName);
       }
     }
 
     int participantsProcessed = 0;
     int participantKeysRegistered = 0;
     for(String entryName : dataInputStrategy.listEntries()) {
       if(((DecryptingOnyxDataInputStrategy) dataInputStrategy).isParticipantEntry(entryName)) {
 
         opalKey = participantKeyWriteRegistry.generateUniqueKey(IParticipantKeyReadRegistry.PARTICIPANT_KEY_DB_OPAL_NAME);
 
         VariableDataSet variableDataSetRoot = getInputStreamFromFile(entryName);
         // System.out.println(VariableStreamer.toXML(variableDataSetRoot));
         VariableFinder variableFinder = VariableFinder.getInstance(variableRoot, new DefaultVariablePathNamingStrategy());
         for(VariableData variableData : variableDataSetRoot.getVariableDatas()) {
           Variable variable = variableFinder.findVariable(variableData.getVariablePath());
           if(variable != null && variable.getKey() != null && !variable.getKey().equals("")) {
             if(variable.getParent().isRepeatable()) {
               for(VariableData repeatVariableData : variableData.getVariableDatas()) {
                 registerOwnerAndKeyInParticipantKeyDatabase(variable, repeatVariableData);
                 participantKeysRegistered++;
               }
             } else {
               registerOwnerAndKeyInParticipantKeyDatabase(variable, variableData);
               participantKeysRegistered++;
             }
           }
         }
         participantsProcessed++;
       }
     }
     System.out.println("Participants processed [" + participantsProcessed + "]    Participant Keys Registered [" + participantKeysRegistered + "]");
   }
 
   private <T> T getInputStreamFromFile(String filename) {
     InputStream inputStream = null;
     T object = null;
     try {
       inputStream = dataInputStrategy.getEntry(filename);
      object = VariableStreamer.fromXML(inputStream);
       if(object == null) throw new IllegalStateException("Unable to load variables from the file [" + filename + "].");
     } finally {
       try {
         inputStream.close();
       } catch(IOException e) {
         throw new IllegalStateException("Could not close InputStream for file [" + filename + "].");
       }
     }
     return object;
   }
 
   private void registerOwnerAndKeyInParticipantKeyDatabase(Variable variable, VariableData variableData) {
     String owner = variable.getKey();
     for(Data data : variableData.getDatas()) {
       String key = data.getValueAsString();
       // System.out.println("processing: " + " key[" + owner + "] participantId[" + key + "] opalKey[" + opalKey +
       // "] variablePath[" + variableData.getVariablePath() + "]");
       participantKeyWriteRegistry.registerEntry(IParticipantKeyReadRegistry.PARTICIPANT_KEY_DB_OPAL_NAME, opalKey, owner, key);
     }
   }
 
   private String promptForPassword(String prompt) {
     String password = null;
 
     PasswordCallback passwordCallback = new PasswordCallback(prompt, false);
     TextCallbackHandler handler = new TextCallbackHandler();
 
     try {
       handler.handle(new Callback[] { passwordCallback });
       if(passwordCallback.getPassword() != null) {
         password = new String(passwordCallback.getPassword());
 
         if(password.length() == 0) {
           password = null;
         }
       }
     } catch(Exception ex) {
       // nothing to do
     }
 
     return password;
   }
 }
