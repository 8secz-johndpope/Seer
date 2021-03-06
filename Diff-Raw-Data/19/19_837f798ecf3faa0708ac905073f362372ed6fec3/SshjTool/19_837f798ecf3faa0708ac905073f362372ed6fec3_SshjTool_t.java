 /**
  * Licensed to jclouds, Inc. (jclouds) under one or more
  * contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  jclouds licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 package brooklyn.util.internal.ssh;
 
 import static brooklyn.util.NetworkUtils.checkPortValid;
 import static com.google.common.base.Preconditions.checkArgument;
 import static com.google.common.base.Preconditions.checkNotNull;
 import static com.google.common.base.Throwables.getCausalChain;
 import static com.google.common.collect.Iterables.any;
 
 import java.io.ByteArrayOutputStream;
 import java.io.Closeable;
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.util.ArrayList;
 import java.util.Collections;
import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 import java.util.concurrent.TimeUnit;
 
 import net.schmizz.sshj.connection.ConnectionException;
 import net.schmizz.sshj.connection.channel.direct.PTYMode;
 import net.schmizz.sshj.connection.channel.direct.Session;
 import net.schmizz.sshj.connection.channel.direct.Session.Command;
 import net.schmizz.sshj.connection.channel.direct.Session.Shell;
 import net.schmizz.sshj.connection.channel.direct.SessionChannel;
 import net.schmizz.sshj.sftp.FileAttributes;
 import net.schmizz.sshj.sftp.SFTPClient;
 import net.schmizz.sshj.transport.TransportException;
 import net.schmizz.sshj.xfer.InMemorySourceFile;
 
 import org.apache.commons.io.input.ProxyInputStream;
 import org.bouncycastle.util.Strings;
 import org.jclouds.io.InputSuppliers;
 import org.jclouds.io.Payload;
 import org.jclouds.io.Payloads;
 import org.jclouds.io.payloads.ByteArrayPayload;
 import org.jclouds.io.payloads.FilePayload;
 import org.jclouds.io.payloads.InputStreamPayload;
 import org.jclouds.io.payloads.StringPayload;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import brooklyn.config.ConfigKey;
 import brooklyn.util.Time;
 import brooklyn.util.flags.TypeCoercions;
 import brooklyn.util.internal.SshTool;
 import brooklyn.util.internal.StreamGobbler;
 import brooklyn.util.text.Identifiers;
 import brooklyn.util.text.StringEscapes.BashStringEscapes;
 
 import com.google.common.annotations.VisibleForTesting;
 import com.google.common.base.Joiner;
 import com.google.common.base.Objects;
 import com.google.common.base.Predicate;
 import com.google.common.base.Stopwatch;
 import com.google.common.base.Throwables;
 import com.google.common.collect.ImmutableList;
 import com.google.common.collect.ImmutableMap;
 import com.google.common.collect.Iterables;
 import com.google.common.collect.Sets;
 import com.google.common.io.ByteStreams;
 import com.google.common.io.Files;
 import com.google.common.io.LimitInputStream;
 import com.google.common.net.HostAndPort;
 
 /**
  * For ssh and scp-style commands, using the sshj library.
  * <p>
  * The implementation is based on a combination of the existing brooklyn SshJschTool,
  * and the jclouds SshjSshClient.
  * <p>
  * Not thread-safe. Use a different SshjTool for each concurrent thread. 
  * If passing from one thread to another, ensure code goes through a synchronized block.
  */
 public class SshjTool implements SshTool {
 
     private static final Logger LOG = LoggerFactory.getLogger(SshjTool.class);
 
     private class CloseFtpChannelOnCloseInputStream extends ProxyInputStream {
 
         private final SFTPClient sftp;
 
         private CloseFtpChannelOnCloseInputStream(InputStream proxy, SFTPClient sftp) {
             super(proxy);
             this.sftp = sftp;
         }
 
         @Override
         public void close() throws IOException {
             super.close();
             closeWhispering(sftp, this);
         }
     }
 
     public static interface SshAction<T> {
         void clear() throws Exception;
 
         T create() throws Exception;
     }
 
 
     private final String toString;
 
     private final int sshTriesTimeout;
 
     private final int sshTries;
 
     private final SshjClientConnection sshClientConnection;
 
     private final BackoffLimitedRetryHandler backoffLimitedRetryHandler;
 
     private final String host;
     private final String user;
     private final String password;
     private final int port;
     private String privateKeyPassphrase;
     private String privateKeyData;
     private File privateKeyFile;
     private boolean strictHostKeyChecking;
     private boolean allocatePTY;
 
 
     public static Builder builder() {
         return new Builder();
     }
     
     private static void warnOnDeprecated(Map<String, ?> props, String deprecatedKey, String correctKey) {
         if (props.containsKey(deprecatedKey)) {
             if (correctKey != null && props.containsKey(correctKey)) {
                 Object dv = props.get(deprecatedKey);
                 Object cv = props.get(correctKey);
                 if (!Objects.equal(cv, dv)) {
                     LOG.warn("SshjTool detected deprecated key '"+deprecatedKey+"' with different value ("+dv+") "+
                             "than new key '"+correctKey+"' ("+cv+"); ambiguous which will be used");
                 } else {
                     // ignore, the deprecated key populated for legacy reasons
                 }
             } else {
                 Object dv = props.get(deprecatedKey);
                 LOG.warn("SshjTool detected deprecated key '"+deprecatedKey+"' used, with value ("+dv+")");     
             }
         }
     }
 
     public static class Builder {
         private String host;
         private int port = PROP_PORT.getDefaultValue();
         private String user = System.getProperty("user.name");
         private String password;
         private String privateKeyData;
         public String privateKeyPassphrase;
         private Set<String> privateKeyFiles = Sets.newLinkedHashSet();
         private boolean strictHostKeyChecking = false;
         private boolean allocatePTY = false;
         private int connectTimeout;
         private int sessionTimeout;
         private int sshTries = 4;  //allow 4 tries by default, much safer
         private int sshTriesTimeout = 2*60*1000;  //allow 2 minutesby default (so if too slow trying sshTries times, abort anyway)
         private long sshRetryDelay = 50L;
         
         @SuppressWarnings("unchecked")
        public Builder from(Map<String,?> propsAll) {
            Map<String,?> props = new LinkedHashMap<String,Object>(propsAll);
            //TODO _remove_ props as they are used, then warn if non-empty at the end
            // (to prevent flags being passed here which are not used here,
            // e.g. out, scriptHeader, etc)
            
             host = getMandatoryVal(props, PROP_HOST);
             port = getOptionalVal(props, PROP_PORT, port);
             user = getOptionalVal(props, PROP_USER, user);
             
             password = getOptionalVal(props, PROP_PASSWORD, password);
             
             // TODO use config keys for these
             warnOnDeprecated(props, "privateKey", "privateKeyData");
             privateKeyData = getOptionalVal(props, "privateKey", String.class, privateKeyData);
             privateKeyData = getOptionalVal(props, "privateKeyData", String.class, privateKeyData);
             privateKeyPassphrase = getOptionalVal(props, "privateKeyPassphrase", String.class, privateKeyPassphrase);
             
             // for backwards compatibility accept keyFiles and privateKey
             // but sshj accepts only a single privateKeyFile; leave blank to use defaults (i.e. ~/.ssh/id_rsa and id_dsa)
             warnOnDeprecated(props, "keyFiles", null);
             privateKeyFiles.addAll(getOptionalVal(props, "keyFiles", List.class, Collections.emptyList()));
             String privateKeyFile = getOptionalVal(props, "privateKeyFile", String.class, null);
             if (privateKeyFile != null) privateKeyFiles.add(privateKeyFile);
             
             strictHostKeyChecking = getOptionalVal(props, "strictHostKeyChecking", Boolean.class, strictHostKeyChecking);
             allocatePTY = getOptionalVal(props, "allocatePTY", Boolean.class, allocatePTY);
             connectTimeout = getOptionalVal(props, "connectTimeout", Integer.class, connectTimeout);
             sessionTimeout = getOptionalVal(props, "sessionTimeout", Integer.class, sessionTimeout);
             sshTries = getOptionalVal(props, "sshTries", Integer.class, sshTries);
             sshTriesTimeout = getOptionalVal(props, "sshTriesTimeout", Integer.class, sshTriesTimeout);
             sshRetryDelay = getOptionalVal(props, "sshRetryDelay", Long.class, sshRetryDelay);
 
             return this;
         }
         public Builder host(String val) {
             this.host = val; return this;
         }
         public Builder user(String val) {
             this.user = val; return this;
         }
         public Builder password(String val) {
             this.password = val; return this;
         }
         public Builder port(int val) {
             this.port = val; return this;
         }
         public Builder privateKeyPassphrase(String val) {
             this.privateKeyPassphrase = val; return this;
         }
         /** @deprecated 1.4.0, use privateKeyData */
         public Builder privateKey(String val) {
             this.privateKeyData = val; return this;
         }
         public Builder privateKeyData(String val) {
             this.privateKeyData = val; return this;
         }
         public Builder privateKeyFile(String val) {
             this.privateKeyFiles.add(val); return this;
         }
         public Builder connectTimeout(int val) {
             this.connectTimeout = val; return this;
         }
         public Builder sessionTimeout(int val) {
             this.sessionTimeout = val; return this;
         }
         public Builder sshRetries(int val) {
             this.sshTries = val; return this;
         }
         public Builder sshRetriesTimeout(int val) {
             this.sshTriesTimeout = val; return this;
         }
         public Builder sshRetryDelay(long val) {
             this.sshRetryDelay = val; return this;
         }
         public SshjTool build() {
             return new SshjTool(this);
         }
     }
 
     public SshjTool(Map<String,?> map) {
         this(builder().from(map));
     }
     
     private SshjTool(Builder builder) {
         // TODO Does this need to be ported from SshJschTool?
 //        if (host && host==~ /[^@]+@[^@]+/) {
 //            (user,host) = (host=~/([^@]+)@([^@]+)/)[0][1,2]
 //        }
 
         host = checkNotNull(builder.host, "host");
         port = builder.port;
         user = builder.user;
         password = builder.password;
         strictHostKeyChecking = builder.strictHostKeyChecking;
         allocatePTY = builder.allocatePTY;
         sshTries = builder.sshTries;
         sshTriesTimeout = builder.sshTriesTimeout;
         backoffLimitedRetryHandler = new BackoffLimitedRetryHandler(sshTries, builder.sshRetryDelay);
         privateKeyPassphrase = builder.privateKeyPassphrase;
         privateKeyData = builder.privateKeyData;
         
         if (builder.privateKeyFiles.size() > 1) {
             throw new IllegalArgumentException("sshj supports only a single private key-file; " +
                     "for defaults of ~/.ssh/id_rsa and ~/.ssh/id_dsa leave blank");
         } else if (builder.privateKeyFiles.size() == 1) {
             String privateKeyFileStr = Iterables.get(builder.privateKeyFiles, 0);
             String amendedKeyFile = privateKeyFileStr.startsWith("~") ? (System.getProperty("user.home")+privateKeyFileStr.substring(1)) : privateKeyFileStr;
             privateKeyFile = new File(amendedKeyFile);
         } else {
             privateKeyFile = null;
         }
         
         checkArgument(host.length() > 0, "host value must not be an empty string");
         checkPortValid(port, "ssh port");
 
         toString = String.format("%s@%s:%d", user, host, port);
 
         sshClientConnection = SshjClientConnection.builder()
                 .hostAndPort(HostAndPort.fromParts(host, port))
                 .username(user)
                 .password(password)
                 .privateKeyPassphrase(privateKeyPassphrase)
                 .privateKeyData(privateKeyData)
                 .privateKeyFile(privateKeyFile)
                 .strictHostKeyChecking(strictHostKeyChecking)
                 .connectTimeout(builder.connectTimeout)
                 .sessionTimeout(builder.sessionTimeout)
                 .build();
         
         if (LOG.isTraceEnabled()) LOG.trace("Created SshjTool {} ({})", this, System.identityHashCode(this));
     }
     
     public String getHostAddress() {
         return this.host;
     }
 
     public String getUsername() {
         return this.user;
     }
 
     @Override
     public void connect() {
         try {
             if (LOG.isTraceEnabled()) LOG.trace("Connecting SshjTool {} ({})", this, System.identityHashCode(this));
             acquire(sshClientConnection);
         } catch (Exception e) {
             if (LOG.isDebugEnabled()) LOG.debug(toString()+" failed to connect (rethrowing)", e);
             throw propagate(e, "failed to connect");
         }
     }
 
     @Override
     public void connect(int maxAttempts) {
         connect(); // FIXME Should callers instead configure sshTries? But that would apply to all ssh attempts
     }
 
     @Override
     public void disconnect() {
         if (LOG.isTraceEnabled()) LOG.trace("Disconnecting SshjTool {} ({})", this, System.identityHashCode(this));
         try {
             sshClientConnection.clear();
         } catch (Exception e) {
             throw Throwables.propagate(e);
         }
     }
 
     @Override
     public boolean isConnected() {
         return sshClientConnection.isConnected() && sshClientConnection.isAuthenticated();
     }
     
     @Override
     public int transferFileTo(Map<String,?> props, InputStream input, String pathAndFileOnRemoteServer) {
         return createFile(props, pathAndFileOnRemoteServer, toPayload(input));
     }
     
     @Override
     public int createFile(Map<String,?> props, String pathAndFileOnRemoteServer, InputStream input, long size) {
         return createFile(props, pathAndFileOnRemoteServer, toPayload(input, size));
     }
 
     /**
      * Creates the given file with the given contents.
      *
      * Permissions specified using 'permissions:0755'.
      */
     @Override
     public int createFile(Map<String,?> props, String pathAndFileOnRemoteServer, String contents) {
         return createFile(props, pathAndFileOnRemoteServer, new StringPayload(contents));
     }
 
     /** Creates the given file with the given contents.
      *
      * Permissions specified using 'permissions:0755'.
      */
     @Override
     public int createFile(Map<String,?> props, String pathAndFileOnRemoteServer, byte[] contents) {
         return createFile(props, pathAndFileOnRemoteServer, new ByteArrayPayload(contents));
     }
 
     @Override
     public int copyToServer(Map<String,?> props, File f, String pathAndFileOnRemoteServer) {
         return createFile(props, pathAndFileOnRemoteServer, new FilePayload(f));
     }
 
     @Override
     public int transferFileFrom(Map<String,?> props, String pathAndFileOnRemoteServer, String pathAndFileOnLocalServer) {
         Payload payload = acquire(new GetFileAction(pathAndFileOnRemoteServer));
         try {
             Files.copy(InputSuppliers.of(payload.getInput()), new File(pathAndFileOnLocalServer));
             return 0; // TODO Can we assume put will have thrown exception if failed? Rather than exit code != 0?
         } catch (IOException e) {
             throw Throwables.propagate(e);
         }
     }
 
     private int createFile(Map<String,?> props, String pathAndFileOnRemoteServer, Payload payload) {
         acquire(new PutFileAction(props, pathAndFileOnRemoteServer, payload));
         return 0; // TODO Can we assume put will have thrown exception if failed? Rather than exit code != 0?
     }
 
     public int execShell(Map<String,?> props, List<String> commands) {
         return execScript(props, commands, Collections.<String,Object>emptyMap());
     }
     public int execShell(Map<String,?> props, List<String> commands, Map<String,?> env) {
         return execScript(props, commands, env);
     }
 
     @Override
     public int execScript(Map<String,?> props, List<String> commands) {
         return execScript(props, commands, Collections.<String,Object>emptyMap());
     }
     
     /**
      * This creates a script containing the user's commands, copies it to the remote server, and
      * executes the script. The script is then deleted.
      * <p>
      * Executing commands directly is fraught with dangers! Here are other options, and their problems:
      * <ul>
      *   <li>Use execCommands, rather than shell.
      *       The user's environment will not be setup normally (e.g. ~/.bash_profile will not have been sourced)
      *       so things like wget may not be on the PATH.
      *   <li>Send the stream of commands to the shell.
      *       But characters being sent can be lost.
      *       Try the following (e.g. in an OS X terminal):
      *        - sleep 5
      *        - <paste a command that is 1000s of characters long>
      *       Only the first 1024 characters appear. The rest are lost.
      *       If sending a stream of commands, you need to be careful not send the next (big) command while the
      *       previous one is still executing.
      *   <li>Send a stream to the shell, but spot when the previous command has completed.
      *       e.g. by looking for the prompt (but what if the commands being executed change the prompt?)
      *       e.g. by putting every second command as "echo <uid>", and waiting for the stdout.
      *       This gets fiddly...
      * </ul>
      * 
      * So on balance, the script-based approach seems most reliable, even if there is an overhead
      * of separate message(s) for copying the file!
      */
     @Override
     public int execScript(Map<String,?> props, List<String> commands, Map<String,?> env) {
         OutputStream out = getOptionalVal(props, PROP_OUT_STREAM);
         OutputStream err = getOptionalVal(props, PROP_ERR_STREAM);
         String scriptDir = getOptionalVal(props, PROP_SCRIPT_DIR);
         String scriptPath = scriptDir+"/brooklyn-"+System.currentTimeMillis()+"-"+Identifiers.makeRandomId(8)+".sh";
         
         String scriptContents = toScript(props, commands, env);
         
         if (LOG.isTraceEnabled()) LOG.trace("Running shell command at {} as script: {}", host, scriptContents);
         
         createFile(ImmutableMap.of("permissions", "0700"), scriptPath, scriptContents);
         
         // use "-f" because some systems have "rm" aliased to "rm -i"; use "< /dev/null" to guarantee doesn't hang
         List<String> cmds = ImmutableList.of(
                 scriptPath+" < /dev/null",
                 "RESULT=$?",
                 "echo \"Executed "+scriptPath+", result $RESULT\"", 
                 "rm -f "+scriptPath+" < /dev/null", 
                 "exit $RESULT");
         
         Integer result = acquire(new ShellAction(cmds, out, err));
         return result != null ? result : -1;
     }
 
     public int execShellDirect(Map<String,?> props, List<String> commands, Map<String,?> env) {
         OutputStream out = getOptionalVal(props, PROP_OUT_STREAM);
         OutputStream err = getOptionalVal(props, PROP_ERR_STREAM);
         
         List<String> cmdSequence = toCommandSequence(commands, env);
         List<String> allcmds = ImmutableList.<String>builder()
                 .add(getOptionalVal(props, PROP_DIRECT_HEADER))
                 .addAll(cmdSequence)
                 .add("exit $?")
                 .build();
         
         if (LOG.isTraceEnabled()) LOG.trace("Running shell command at {}: {}", host, allcmds);
         
         Integer result = acquire(new ShellAction(allcmds, out, err));
         if (LOG.isTraceEnabled()) LOG.trace("Running shell command at {} completed: return status {}", host, result);
         return result != null ? result : -1;
     }
 
     @Override
     public int execCommands(Map<String,?> props, List<String> commands) {
         return execCommands(props, commands, Collections.<String,Object>emptyMap());
     }
 
     @Override
     public int execCommands(Map<String,?> props, List<String> commands, Map<String,?> env) {
         if (props.containsKey("blocks") && props.get("blocks") == Boolean.FALSE) {
             throw new IllegalArgumentException("Cannot exec non-blocking: command="+commands);
         }
         OutputStream out = getOptionalVal(props, PROP_OUT_STREAM);
         OutputStream err = getOptionalVal(props, PROP_ERR_STREAM);
         String separator = getOptionalVal(props, PROP_SEPARATOR);
 
         List<String> allcmds = toCommandSequence(commands, env);
         String singlecmd = Joiner.on(separator).join(allcmds);
 
         if (LOG.isTraceEnabled()) LOG.trace("Running command at {}: {}", host, singlecmd);
         
         Command result = acquire(new ExecAction(singlecmd, out, err));
         if (LOG.isTraceEnabled()) LOG.trace("Running command at {} completed: exit code {}", host, result.getExitStatus());
         return result.getExitStatus();
     }
 
     private String toScript(Map<String,?> props, List<String> commands, Map<String,?> env) {
         List<String> allcmds = toCommandSequence(commands, env);
         
         StringBuilder result = new StringBuilder();
         // -e causes it to fail on any command in the script which has an error (non-zero return code)
         result.append(getOptionalVal(props, PROP_SCRIPT_HEADER)+"\n");
         
         for (String cmd : allcmds) {
             result.append(cmd+"\n");
         }
         
         return result.toString();
     }
 
     /**
      * Merges the commands and env, into a single set of commands. Also escapes the commands as required.
      * 
      * Not all ssh servers handle "env", so instead convert env into exported variables
      */
     private List<String> toCommandSequence(List<String> commands, Map<String,?> env) {
         List<String> result = new ArrayList<String>(env.size()+commands.size());
         
         for (Entry<String,?> entry : env.entrySet()) {
             if (entry.getKey() == null || entry.getValue() == null) {
                 LOG.warn("env key-values must not be null; ignoring: key="+entry.getKey()+"; value="+entry.getValue());
                 continue;
             }
             String escapedVal = BashStringEscapes.escapeLiteralForDoubleQuotedBash(entry.getValue().toString());
             result.add("export "+entry.getKey()+"=\""+escapedVal+"\"");
         }
         
         for (CharSequence cmd : commands) { // objects in commands can be groovy GString so can't treat as String here
             result.add(cmd.toString());
         }
 
         return result;
     }
 
     private void checkConnected() {
         if (!isConnected()) {
             throw new IllegalStateException(String.format("(%s) ssh not connected!", toString()));
         }
     }
 
     private void backoffForAttempt(int retryAttempt, String message) {
         backoffLimitedRetryHandler.imposeBackoffExponentialDelay(retryAttempt, message);
     }
 
     protected <T, C extends SshAction<T>> T acquire(C connection) {
         Stopwatch stopwatch = new Stopwatch().start();
         
         for (int i = 0; i < sshTries; i++) {
             try {
                 connection.clear();
                 if (LOG.isTraceEnabled()) LOG.trace(">> ({}) acquiring {}", toString(), connection);
                 T returnVal = connection.create();
                 if (LOG.isTraceEnabled()) LOG.trace("<< ({}) acquired {}", toString(), returnVal);
                 return returnVal;
             } catch (Exception e) {
                 String errorMessage = String.format("(%s) error acquiring %s", toString(), connection);
                 String fullMessage = String.format("%s (attempt %s/%s, in time %s/%s)", 
                         errorMessage, (i+1), sshTries, Time.makeTimeString(stopwatch.elapsedMillis()), 
                         (sshTriesTimeout > 0 ? Time.makeTimeString(sshTriesTimeout) : "unlimited"));
                 try {
                     disconnect();
                 } catch (Exception e2) {
                     LOG.warn("<< ("+toString()+") error closing connection: "+e+" / "+e2, e);
                 }
                 if (i + 1 == sshTries) {
                     LOG.warn("<< {}: {}", fullMessage, e.getMessage());
                     throw propagate(e, fullMessage + "; out of retries");
                 } else if (sshTriesTimeout > 0 && stopwatch.elapsedMillis() > sshTriesTimeout) {
                     LOG.warn("<< {}: {}", fullMessage, e.getMessage());
                     throw propagate(e, fullMessage + "; out of time");
                 } else {
                     if (LOG.isDebugEnabled()) LOG.debug("<< {}: {}", fullMessage, e.getMessage());
                     backoffForAttempt(i + 1, errorMessage + ": " + e.getMessage());
                     if (connection != sshClientConnection)
                         connect();
                     continue;
                 }
             }
         }
         assert false : "should not reach here";
         return null;
     }
 
     private final SshAction<SFTPClient> sftpConnection = new SshAction<SFTPClient>() {
 
         private SFTPClient sftp;
 
         @Override
         public void clear() {
             closeWhispering(sftp, this);
             sftp = null;
         }
 
         @Override
         public SFTPClient create() throws IOException {
             checkConnected();
             sftp = sshClientConnection.ssh.newSFTPClient();
             return sftp;
         }
 
         @Override
         public String toString() {
             return "SFTPClient()";
         }
     };
 
     private class GetFileAction implements SshAction<Payload> {
         private final String path;
         private SFTPClient sftp;
 
         GetFileAction(String path) {
             this.path = checkNotNull(path, "path");
         }
 
         @Override
         public void clear() throws IOException {
             closeWhispering(sftp, this);
             sftp = null;
         }
 
         @Override
         public Payload create() throws Exception {
             sftp = acquire(sftpConnection);
             return Payloads.newInputStreamPayload(new CloseFtpChannelOnCloseInputStream(
                     sftp.getSFTPEngine().open(path).getInputStream(), sftp));
         }
 
         @Override
         public String toString() {
             return "Payload(path=[" + path + "])";
         }
     };
 
     private class PutFileAction implements SshAction<Void> {
         // TODO See SshJschTool.createFile: it does whacky stuff when copying; do we need that here as well?
         // TODO support backup as a property?
         
         private final String path;
         private final Payload contents;
         private SFTPClient sftp;
         private int permissionsMask;
         private long lastModificationDate;
         private long lastAccessDate;
         
         PutFileAction(Map<String,?> props, String path, Payload contents) {
             String permissions = getOptionalVal(props, "permissions", String.class, "0644");
             permissionsMask = Integer.parseInt(permissions, 8);
             lastModificationDate = getOptionalVal(props, "lastModificationDate", Long.class, 0L);
             lastAccessDate = getOptionalVal(props, "lastAccessDate", Long.class, 0L);
             if (lastAccessDate <= 0 ^ lastModificationDate <= 0) {
                 lastAccessDate = Math.max(lastAccessDate, lastModificationDate);
                 lastModificationDate = Math.max(lastAccessDate, lastModificationDate);
             }
             this.path = checkNotNull(path, "path");
             this.contents = checkNotNull(contents, "contents");
         }
 
         @Override
         public void clear() {
             closeWhispering(sftp, this);
             sftp = null;
         }
 
         @Override
         public Void create() throws Exception {
             sftp = acquire(sftpConnection);
             try {
                 sftp.put(new InMemorySourceFile() {
                     @Override public String getName() {
                         return path;
                     }
                     @Override public long getLength() {
                         return contents.getContentMetadata().getContentLength();
                     }
                     @Override public InputStream getInputStream() throws IOException {
                         return checkNotNull(contents.getInput(), "inputstream for path %s", path);
                     }
                 }, path);
                 sftp.chmod(path, permissionsMask);
                 if (lastAccessDate > 0) {
                     sftp.setattr(path, new FileAttributes.Builder()
                             .withAtimeMtime(lastAccessDate, lastModificationDate)
                             .build());
                 }
             } finally {
                 contents.release();
             }
             return null;
         }
 
         @Override
         public String toString() {
             return "Put(path=[" + path + "])";
         }
     };
 
     @VisibleForTesting
     Predicate<String> causalChainHasMessageContaining(final Exception from) {
         return new Predicate<String>() {
 
             @Override
             public boolean apply(final String input) {
                 return any(getCausalChain(from), new Predicate<Throwable>() {
 
                     @Override
                     public boolean apply(Throwable arg0) {
                         return (arg0.toString().indexOf(input) != -1)
                                 || (arg0.getMessage() != null && arg0.getMessage().indexOf(input) != -1);
                     }
 
                 });
             }
 
         };
     }
 
     private SshException propagate(Exception e, String message) throws SshException {
         throw new SshException("(" + toString() + ") " + message + ":" + e.getMessage(), e);
     }
     
     protected void allocatePTY(Session s) throws ConnectionException, TransportException {
         // this was set as the default, but it makes output harder to read
         // and causes stderr to be sent to stdout;
         // but some systems requiretty for sudoing
         if (allocatePTY)
             s.allocatePTY("vt100", 80, 24, 0, 0, Collections.<PTYMode, Integer> emptyMap());
 //            s.allocatePTY("dumb", 80, 24, 0, 0, Collections.<PTYMode, Integer> emptyMap());
     }
 
     @Override
     public String toString() {
         return toString;
     }
 
     protected SshAction<Session> newSessionAction() {
 
         return new SshAction<Session>() {
 
             private Session session = null;
 
             @Override
             public void clear() throws TransportException, ConnectionException {
                 closeWhispering(session, this);
                 session = null;
             }
 
             @Override
             public Session create() throws Exception {
                 checkConnected();
                 session = sshClientConnection.ssh.startSession();
                 allocatePTY(session);
                 return session;
             }
 
             @Override
             public String toString() {
                 return "Session()";
             }
         };
 
     }
 
     class ExecAction implements SshAction<Command> {
         private final String command;
         
         private Session session;
         private Shell shell;
         private StreamGobbler outgobbler;
         private StreamGobbler errgobbler;
         private OutputStream out;
         private OutputStream err;
 
         ExecAction(String command, OutputStream out, OutputStream err) {
             this.command = checkNotNull(command, "command");
             this.out = out;
             this.err = err;
         }
 
         @Override
         public void clear() throws TransportException, ConnectionException {
             closeWhispering(session, this);
             closeWhispering(shell, this);
             closeWhispering(outgobbler, this);
             closeWhispering(errgobbler, this);
             session = null;
             shell = null;
         }
 
         @Override
         public Command create() throws Exception {
             try {
                 session = acquire(newSessionAction());
                 
                 Command output = session.exec(checkNotNull(command, "command"));
                 
                 if (out != null) {
                     outgobbler = new StreamGobbler(output.getInputStream(), out, (Logger)null);
                     outgobbler.start();
                 }
                 if (err != null) {
                     errgobbler = new StreamGobbler(output.getErrorStream(), err, (Logger)null);
                     errgobbler.start();
                 }
                 try {
                     output.join(sshClientConnection.getSessionTimeout(), TimeUnit.MILLISECONDS);
                     return output;
                     
                 } finally {
                     // wait for all stdout/stderr to have been re-directed
                     try {
                         if (outgobbler != null) outgobbler.join();
                         if (errgobbler != null) errgobbler.join();
                     } catch (InterruptedException e) {
                         LOG.warn("Interrupted gobbling streams from ssh: "+command, e);
                         Thread.currentThread().interrupt();
                     }
                 }
                 
             } finally {
                 clear();
             }
         }
 
         @Override
         public String toString() {
             return "Exec(command=[" + command + "])";
         }
     }
 
     class ShellAction implements SshAction<Integer> {
         private final List<String> commands;
         
         private Session session;
         private Shell shell;
         private StreamGobbler outgobbler;
         private StreamGobbler errgobbler;
         private OutputStream out;
         private OutputStream err;
 
         ShellAction(List<String> commands, OutputStream out, OutputStream err) {
             this.commands = checkNotNull(commands, "commands");
             this.out = out;
             this.err = err;
         }
 
         @Override
         public void clear() throws TransportException, ConnectionException {
             closeWhispering(session, this);
             closeWhispering(shell, this);
             closeWhispering(outgobbler, this);
             closeWhispering(errgobbler, this);
             session = null;
             shell = null;
         }
 
         @Override
         public Integer create() throws Exception {
             try {
                 session = acquire(newSessionAction());
                 
                 shell = session.startShell();
                 
                 if (out != null) {
                     InputStream outstream = shell.getInputStream();
                     outgobbler = new StreamGobbler(outstream, out, (Logger)null);
                     outgobbler.start();
                 }
                 if (err != null) {
                     InputStream errstream = shell.getErrorStream();
                     errgobbler = new StreamGobbler(errstream, err, (Logger)null);
                     errgobbler.start();
                 }
                 
                 OutputStream output = shell.getOutputStream();
 
                 for (CharSequence cmd : commands) {
                     try {
                         output.write(Strings.toUTF8ByteArray(cmd+"\n"));
                         output.flush();
                     } catch (ConnectionException e) {
                         if (!shell.isOpen()) {
                             // shell is closed; presumably the user command did `exit`
                             if (LOG.isDebugEnabled()) LOG.debug("Shell closed to {} when executing {}", SshjTool.this.toString(), commands);
                             break;
                         } else {
                             throw e;
                         }
                     }
                 }
                 shell.sendEOF();
                 closeWhispering(output, this);
                 
                 try {
                     int timeout = sshClientConnection.getSessionTimeout();
                     long timeoutEnd = System.currentTimeMillis() + timeout;
                     Exception last = null;
                     do {
                         if (!shell.isOpen() && ((SessionChannel)session).getExitStatus()!=null)
                             // shell closed, and exit status returned
                             break;
                         boolean endBecauseReturned =
                             // if either condition is satisfied, then wait 1s in hopes the other does, then return
                             (!shell.isOpen() || ((SessionChannel)session).getExitStatus()!=null);
                         try {
                             shell.join(1000, TimeUnit.MILLISECONDS);
                         } catch (ConnectionException e) { last = e; }
                         if (endBecauseReturned)
                             // shell is still open, ie some process is running
                             // but we have a result code, so main shell is finished
                             // we waited one second extra to allow any background process 
                             // which is nohupped to really be in the background (#162)
                             // now let's bail out
                             break;
                     } while (timeout<=0 || System.currentTimeMillis() < timeoutEnd);
                     if (shell.isOpen() && ((SessionChannel)session).getExitStatus()==null) {
                         LOG.debug("Timeout ({}) in SSH shell to {}", sshClientConnection.getSessionTimeout(), this);
                         // we timed out, or other problem -- reproduce the error
                         throw last;
                     }
                     return ((SessionChannel)session).getExitStatus();
                 } finally {
                     // wait for all stdout/stderr to have been re-directed
                     closeWhispering(shell, this);
                     shell = null;
                     try {
                         if (outgobbler != null) outgobbler.join();
                         if (errgobbler != null) errgobbler.join();
                     } catch (InterruptedException e) {
                         LOG.warn("Interrupted gobbling streams from ssh: "+commands, e);
                         Thread.currentThread().interrupt();
                     }
                 }
                 
             } finally {
                 clear();
             }
         }
 
         @Override
         public String toString() {
             return "Shell(command=[" + commands + "])";
         }
     }
 
     private Payload toPayload(InputStream input, long length) {
         InputStreamPayload payload = new InputStreamPayload(new LimitInputStream(input, length));
         payload.getContentMetadata().setContentLength(length);
         return payload;
     }
     
     private Payload toPayload(InputStream input) {
         /*
          * TODO sshj needs to know the length of the InputStream to copy the file:
          *   java.lang.NullPointerException
          *     at brooklyn.util.internal.ssh.SshjTool$PutFileAction$1.getLength(SshjTool.java:574)
          *     at net.schmizz.sshj.sftp.SFTPFileTransfer$Uploader.upload(SFTPFileTransfer.java:174)
          *     at net.schmizz.sshj.sftp.SFTPFileTransfer$Uploader.access$100(SFTPFileTransfer.java:162)
          *     at net.schmizz.sshj.sftp.SFTPFileTransfer.upload(SFTPFileTransfer.java:61)
          *     at net.schmizz.sshj.sftp.SFTPClient.put(SFTPClient.java:248)
          *     at brooklyn.util.internal.ssh.SshjTool$PutFileAction.create(SshjTool.java:569)
          * 
          * Unfortunately that requires consuming the input stream to find out! We can't just do:
          *   new InputStreamPayload(input)
          * 
          * This is nasty: we have to hold the entire file in-memory.
          * It's worth a look at changing sshj to not need the length, if possible.
          */
         try {
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ByteStreams.copy(input, byteArrayOutputStream);
             return new ByteArrayPayload(byteArrayOutputStream.toByteArray());
         } catch (IOException e) {
             LOG.warn("Error consuming stream", e);
             throw Throwables.propagate(e);
         }
     }
 
     static <T> T getMandatoryVal(Map<String,?> map, ConfigKey<T> keyC) {
         String key = keyC.getName();
         checkArgument(map.containsKey(key), "must contain key '"+keyC+"'");
         return TypeCoercions.coerce(map.get(key), keyC.getType());
     }
     
     static <T> T getOptionalVal(Map<String,?> map, ConfigKey<T> keyC) {
         String key = keyC.getName();
         if (map.containsKey(key)) {
             return TypeCoercions.coerce(map.get(key), keyC.getType());
         } else {
             return keyC.getDefaultValue();
         }
     }
 
     /** returns the value of the key if specified, otherwise defaultValue */
     static <T> T getOptionalVal(Map<String,?> map, ConfigKey<T> keyC, T defaultValue) {
         String key = keyC.getName();
         if (map.containsKey(key)) {
             return TypeCoercions.coerce(map.get(key), keyC.getType());
         } else {
             return defaultValue;
         }
     }
 
     /** @deprecated since 0.5.0 use ConfigKey variant */
     @Deprecated
     static <T> T getMandatoryVal(Map<String,?> map, String key, Class<T> clazz) {
         checkArgument(map.containsKey(key), "must contain key '"+key+"'");
         return TypeCoercions.coerce(map.get(key), clazz);
     }
     
     /** @deprecated since 0.5.0 use ConfigKey variant */
     @Deprecated
     static <T> T getOptionalVal(Map<String,?> map, String key, Class<T> clazz, T defaultVal) {
         if (map.containsKey(key)) {
             return TypeCoercions.coerce(map.get(key), clazz);
         } else {
             return defaultVal;
         }
     }
     
     /**
      * Similar to Guava's Closeables.closeQuitely, except logs exception at debug with context in message.
      */
     private void closeWhispering(Closeable closeable, Object context) {
         if (closeable != null) {
             try {
                 closeable.close();
             } catch (IOException e) {
                 if (LOG.isDebugEnabled()) {
                     String msg = String.format("<< exception during close, for %s -> %s (%s); continuing.", 
                             SshjTool.this.toString(), context, closeable);
                     LOG.debug(msg, e);
                 }
             }
         }
     }
 }
