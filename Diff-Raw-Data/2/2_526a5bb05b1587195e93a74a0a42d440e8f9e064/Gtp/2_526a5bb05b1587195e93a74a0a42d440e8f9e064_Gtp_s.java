 //----------------------------------------------------------------------------
 // $Id$
 // $Source$
 //----------------------------------------------------------------------------
 
 package gtp;
 
 import java.io.*;
 import java.util.*;
 import go.Color;
 import go.Point;
 import go.Move;
 import utils.StringUtils;
 import utils.MessageQueue;
 
 //----------------------------------------------------------------------------
 
 /** Interface to a Go program that uses GTP over the standard I/O streams.
     <p>
     This class is final because it starts a thread in its constructor which
     might conflict with subclassing because the subclass constructor will
     be called after the thread is started.
     </p>
     <p>
     GTP commands are sent with the sendCommand functions.
     Callbacks can be registered to monitor the input, output and error stream
     and to handle timeout and invalid responses.
     All callbacks are only called in the sendCommand functions and from
     the caller's thread.
     </p>
     <p>
     Internally the class reads the output and error streams from different
     threads and puts them on a message queue, so that the callbacks should
     be in the same order as the received data.
     It was not possible to use java.nio.Selector, because the streams in
     class Process are not SelectableChannels (Java 1.5.0).
     </p>
 */
 public final class Gtp
 {
     /** Callback if a timeout occured.
         If the function askContinue returns true, Gtp.sendCommand will
         wait for another timeout period, if it returns false, the program
         will be killed.
     */
     public interface TimeoutCallback
     {
         public boolean askContinue();
     }
 
     /** Callback if an invalid response occured.
         Can be used to display invalid responses (without a status character)
         immediately, because sendCommand will not abort on an invalid response
         but continue to wait for a valid response line.
         This is necessary for some Go programs with broken GTP implementation
         which write debug data to standard output (e.g. Wallyplus 0.1.2).
         This callback will only be called at the first invalid response,
         subsequent invalid responses will be ignored quietly.
     */
     public interface InvalidResponseCallback
     {
         public void show(String line);
     }
 
     /** Callback interface for logging or displaying the GTP stream. */
     public interface IOCallback
     {
         public void receivedInvalidResponse(String s);
 
         public void receivedResponse(boolean error, String s);
 
         public void receivedStdErr(String s);
 
         public void sentCommand(String s);
     }    
 
     /** Constructor.
         @param program Command line for program.
         Will be split into words with respect to " as in StringUtils.tokenize.
         If the command line contains the string "%SRAND", it will be replaced
         by a random seed. This is useful if the random seed can be set by
         a command line option to produce deterministic randomness (the
         command returned by getProgramCommand() will contain the actual
         random seed used).
         @param log Log input, output and error stream to standard error.
         @param callback Callback for external display of the streams.
     */
     public Gtp(String program, boolean log, IOCallback callback)
         throws GtpError
     {
         m_log = log;
         m_program = program;
         m_callback = callback;
         if (m_program.indexOf("%SRAND") >= 0)
         {
             // RAND_MAX in stdlib.h ist at least 32767
             final int RAND_MAX = 32767;
             int rand = (int)(Math.random() * (RAND_MAX + 1));
             m_program =
                 m_program.replaceAll("%SRAND", Integer.toString(rand));
         }
         Runtime runtime = Runtime.getRuntime();
         try
         {
             // Create command array with StringUtils::tokenize
             // because Runtime.exec(String) uses a default StringTokenizer
             // which does not respect ".
             m_process = runtime.exec(StringUtils.tokenize(program));
         }
         catch (IOException e)
         {
             throw new GtpError("Could not execute " + program + ":\n" +
                                 e.getMessage());
         }
         m_out = new PrintWriter(m_process.getOutputStream());
         m_isProgramDead = false;
         
         m_queue = new MessageQueue();
         m_inputThread = new InputThread(m_process.getInputStream(), m_queue);
         m_errorThread = new ErrorThread(m_process.getErrorStream(), m_queue);
         m_inputThread.start();
         m_errorThread.start();
     }
     
     /** Close the output stream to the program. */
     public void close()
     {
         m_out.close();
     }
 
     /** Kill the Go program. */
     public void destroyProcess()
     {
         m_process.destroy();
     }
 
     /** Get response to last command sent. */
     public String getResponse()
     {
         return m_response;
     }
 
     /** Get command for setting the board size.
         Note: call queryProtocolVersion first
         @return The boardsize command for GTP version 2 programs,
         otherwise null.
     */
     public String getCommandBoardsize(int size)
     {
         if (m_protocolVersion == 2)
             return ("boardsize " + size);
         else
             return null;
     }
 
     /** Get command for starting a new game.
         Note: call queryProtocolVersion first
         @return The boardsize command for GTP version 1 programs,
         otherwise the clear_board command.
     */
     public String getCommandClearBoard(int size)
     {
         if (m_protocolVersion == 1)
             return "boardsize " + size;
         else
             return "clear_board";
     }
 
     /** Get command for generating a move.
         Note: call queryProtocolVersion first
         @return The right command depending on the GTP version.
     */
     public String getCommandGenmove(Color color)
     {
         String c = color.toString();
         if (m_protocolVersion == 1)
             return "genmove_" + c;
         else
             return "genmove " + c;
     }
 
     /** Get command for playing a move without the point argument.
         Note: call queryProtocolVersion first
         @return The right command depending on the GTP version.
     */
     public String getCommandPlay(Color color)
     {
         
         String command = "";
         if (m_protocolVersion == 2)
             command = "play ";
         command = command + color.toString();
         return command;
     }
 
     /** Get command for playing a move.
         Note: call queryProtocolVersion first
         @return The right command depending on the GTP version.
     */
     public String getCommandPlay(Move move)
     {
         
         String command = getCommandPlay(move.getColor());
         go.Point p = move.getPoint();
         if (p == null)
             command = command + " pass";
         else
             command = command + " " + p.toString();
         return command;
     }
 
     /** Send command cputime and convert the result to double.
         @throws GtpError if command fails or does not return a floating point
         number.
     */
     public double getCpuTime() throws GtpError
     {
         try
         {
             return Double.parseDouble(sendCommand("cputime"));
         }
         catch (NumberFormatException e)
         {
             throw new GtpError("Invalid response to cputime command");
         }
     }
 
     /** Get full response including status and ID and last command. */
     public String getFullResponse()
     {
         return m_fullResponse;
     }
 
     /** Get the command line that was used for invoking the Go program.
         @return The command line that was given to the constructor.
     */
     public String getProgramCommand()
     {
         return m_program;
     }
 
     /** Get protocol version.
         You have to call queryProtocolVersion() first, otherwise this method
         will always return 1.
     */
     public int getProtocolVersion()
     {
         return m_protocolVersion;
     }
 
     /** Get the supported commands.
         Note: call querySupportedCommands() first.
         @return A vector of strings with the supported commands.
     */
     public Vector getSupportedCommands()
     {
         Vector result = new Vector(128, 128);
         if (m_supportedCommands != null)
             for (int i = 0; i < m_supportedCommands.length; ++i)
                 result.add(m_supportedCommands[i]);
         return result;
     }
 
     /** Check if a command is supported.
         Note: call querySupportedCommands() first.
     */
     public boolean isCommandSupported(String command)
     {
         if (m_supportedCommands == null)
             return false;
         for (int i = 0; i < m_supportedCommands.length; ++i)
             if (m_supportedCommands[i].equals(command))
                 return true;
         return false;
     }
 
     /** Check if cputime command is supported.
         Note: call querySupportedCommands() first.
     */
     public boolean isCpuTimeSupported()
     {
         return isCommandSupported("cputime");
     }
 
     /** Check if interrupting a command is supported.
         Interrupting can supported by ANSI C signals or the special
         comment line "# interrupt" as described in the GoGui documentation
         chapter "Interrupting commands".
         Note: call queryInterruptSupport() first.
     */
     public boolean isInterruptSupported()
     {
         return (m_isInterruptCommentSupported || m_pid != null);
     }
 
     /** Check if program is dead. */
     public boolean isProgramDead()
     {
         return m_isProgramDead;
     }
 
     /** Query if interrupting is supported.
         @see Gtp#isInterruptSupported
     */
     public void queryInterruptSupport()
     {
         try
         {
             if (isCommandSupported("gogui_interrupt"))
             {
                 sendCommand("gogui_interrupt");
                 m_isInterruptCommentSupported = true;
             }
             else if (isCommandSupported("gogui_sigint"))
                 m_pid = sendCommand("gogui_sigint").trim();
         }
         catch (GtpError e)
         {
         }
     }
 
     /** Queries the name.
         @return Name or "Unknown Program" if name command not supported
     */
     public String queryName()
     {
         try
         {
             return sendCommand("name");
         }
         catch (GtpError e)
         {
             return "Unknown Program";
         }
     }
 
     /** Query the protocol version.
         Sets the protocol version to the response or to 1 protocol_version
         command fails.
         @see Gtp#getProtocolVersion
         @throws GtpError if the response to protocol_version is not 1 or 2.
     */
     public void queryProtocolVersion() throws GtpError
     {
         try
         {            
             String response;
             try
             {
                 response = sendCommand("protocol_version");
             }
             catch (GtpError e)
             {
                 m_protocolVersion = 1;
                 return;
             }
             int v = Integer.parseInt(response);
             if (v < 1 || v > 2)
                 throw new GtpError("Unknown protocol version: " + v);
             m_protocolVersion = v;
         }
         catch (NumberFormatException e)
         {
             throw new GtpError("Invalid protocol version");
         }
     }
 
     /** Query the supported commands.
         @see Gtp#getSupportedCommands
         @see Gtp#isCommandSupported
     */
     public void querySupportedCommands() throws GtpError
     {
         String command = (m_protocolVersion == 1 ? "help" : "list_commands");
         String response = sendCommand(command);
         m_supportedCommands = StringUtils.tokenize(response);
         for (int i = 0; i < m_supportedCommands.length; ++i)
             m_supportedCommands[i] = m_supportedCommands[i].trim();
     }
 
     /** Queries the program version..
         @return The version or an empty string if the version command fails.
     */
     public String queryVersion()
     {
         try
         {
             return sendCommand("version");
         }
         catch (GtpError e)
         {
             return "";
         }
     }
 
     /** Send a command.
         @return The response text of the successful response not including
         the status character.
         @throws GtpError containing the response if the command fails.
     */
     public String sendCommand(String command) throws GtpError
     {
         return sendCommand(command, -1, null);
     }
 
     /** Send a command with timeout.
         @param command The command to send
         @param timeout Timeout in milliseconds or -1, if no timeout
         @param timeoutCallback Timeout callback or null if no timeout.
         @return The response text of the successful response not including
         the status character.
         @throws GtpError containing the response if the command fails.
         @see TimeoutCallback
     */
     public String sendCommand(String command, long timeout,
                               TimeoutCallback timeoutCallback)
         throws GtpError
     {
         assert(! command.trim().equals(""));
         assert(! command.trim().startsWith("#"));
         m_timeoutCallback = timeoutCallback;
         m_fullResponse = "";
         m_response = "";
         if (m_isProgramDead)
             throwProgramDied();
         if (m_autoNumber)
         {
             ++m_commandNumber;
             command = Integer.toString(m_commandNumber) + " " + command;
         }
         log(">> " + command);
         m_out.println(command);
         m_out.flush();
         if (m_out.checkError())
         {
             m_isProgramDead = true;
             throwProgramDied();
         }
         if (m_callback != null)
             m_callback.sentCommand(command);
         readResponse(timeout);
         return m_response;
     }
 
     /** Send command for setting the board size.
         Send the command if it exists in the GTP protocol version.
         Note: call queryProtocolVersion first
         @see Gtp#getCommandBoardsize
     */
     public void sendCommandBoardsize(int size) throws GtpError
     {
         String command = getCommandBoardsize(size);
         if (command != null)
             sendCommand(command);
     }
 
     /** Send command for staring a new game.
         Note: call queryProtocolVersion first
         @see Gtp#getCommandClearBoard
     */
     public void sendCommandClearBoard(int size) throws GtpError
     {
         sendCommand(getCommandClearBoard(size));
     }
 
     /** Send command for playing a move.
         Note: call queryProtocolVersion first
     */
     public void sendCommandPlay(Move move) throws GtpError
     {
         sendCommand(getCommandPlay(move));
     }
 
     /** Send comment.
         @param comment comment line (must start with '#').
     */
     public void sendComment(String comment)
     {
         assert(comment.trim().startsWith("#"));
         log(">> " + comment);
         if (m_callback != null)
             m_callback.sentCommand(comment);
         m_out.println(comment);
         m_out.flush();
     }
 
     /** Interrupt current command.
         Can be called from a different thread during a sendCommand.
         Note: call queryInterruptSupport first
         @see Gtp#isInterruptSupported
         @throws GtpError if interrupting commands is not supported.
     */
     public void sendInterrupt() throws GtpError
     {
         if (m_isInterruptCommentSupported)
             sendComment("# interrupt");
         else if (m_pid != null)
         {
             String command = "kill -INT " + m_pid;
             log(" " + command);
             Runtime runtime = Runtime.getRuntime();
             try
             {
                 Process process = runtime.exec(command);
                 int result = process.waitFor();
                 if (result != 0)
                     throw new GtpError("Command \"" + command
                                         + "\" returned " + result + ".");
             }
             catch (IOException e)
             {
                 throw new GtpError("Could not run command " + command +
                                     ":\n" + e);
             }
             catch (InterruptedException e)
             {
             }
         }
         else
             throw new GtpError("Interrupt not supported");
     }
 
     /** Enable auto-numbering commands.
         Every command will be prepended by an integer as defined in the GTP
         standard, the integer is incremented after each command.
     */
     public void enableAutoNumber()
     {
         m_autoNumber = true;
     }
 
     /** Set the callback for invalid responses.
         @see InvalidResponseCallback
     */
     public void setInvalidResponseCallback(InvalidResponseCallback callback)
     {
         m_invalidResponseCallback = callback;
     }
 
     /** Set a prefix for logging to standard error.
         Only used if logging was enabled in the constructor.
     */
     public void setLogPrefix(String prefix)
     {
         synchronized (this)
         {
             m_logPrefix = prefix;
         }
     }
 
     /** Wait until the process of the program exits. */
     public void waitForExit()
     {
         try
         {
             m_process.waitFor();
         }
         catch (InterruptedException e)
         {
             System.err.println("Interrupted");
         }
     }
 
 
     public void waitForExit(int timeout, TimeoutCallback timeoutCallback)
     {
         while (true)
         {
             Thread thread = new Thread()
                 {
                     public void run()
                     {
                         try
                         {
                             m_process.waitFor();
                         }
                         catch (InterruptedException e)
                         {
                         }
                     }
                 };
             thread.start();
             try
             {
                 Thread.sleep(timeout);
             }
             catch (InterruptedException e)
             {
                 assert(false);
             }
             if (thread.isAlive())
             {
                 if (! timeoutCallback.askContinue())
                 {
                     m_process.destroy();
                     return;
                 }
                 // Would like to interrupt the thread before creating
                 // a new one, but Process.waitFor is not interruptible
             }
         }
     }
 
     private static class ReadMessage
     {
         public ReadMessage(boolean isError, String text)
         {
             m_isError = isError;
             m_text = text;
         }
 
         public boolean m_isError;
 
         public String m_text;
     }
     
     private static class InputThread
         extends Thread
     {
         InputThread(InputStream in, MessageQueue queue)
         {
             m_in = new BufferedReader(new InputStreamReader(in));
             m_queue = queue;
         }
 
         public void run()
         {
             try
             {
                 mainLoop();
             }
             catch (Throwable t)
             {
                 StringUtils.printException(t);
             }
         }
 
         private BufferedReader m_in;
 
         private MessageQueue m_queue;
 
         private void mainLoop() throws InterruptedException
         {
             while (true)
             {
                 String line;
                 try
                 {
                     line = m_in.readLine();
                 }
                 catch (IOException e)
                 {
                     line = null;
                 }
                 Thread.yield(); // Give ErrorThread a chance to read first
                 m_queue.put(new ReadMessage(false, line));
                 if (line == null)
                     return;
             }
         }
     }
 
     private static class ErrorThread
         extends Thread
     {
         public ErrorThread(InputStream in, MessageQueue queue)
         {
             m_in = new InputStreamReader(in);
             m_queue = queue;
         }
 
         public void run()
         {
             try
             {
                 mainLoop();
             }
             catch (Throwable t)
             {
                 StringUtils.printException(t);
             }
         }
 
         private Reader m_in;
 
         private MessageQueue m_queue;
 
         private void mainLoop() throws InterruptedException
         {
             int size = 1024;
             char[] buffer = new char[size];
             while (true)
             {                
                 int n;
                 try
                 {
                     n = m_in.read(buffer, 0, size);
                 }
                 catch (IOException e)
                 {
                     n = -1;
                 }
                 String text = null;
                 if (n > 0)
                     text = new String(buffer, 0, n);
                 m_queue.put(new ReadMessage(true, text));
                 if (text == null)
                     return;
             }
         }
     }
 
     private boolean m_autoNumber;
 
     private InvalidResponseCallback m_invalidResponseCallback;
 
     private boolean m_isInterruptCommentSupported;
 
     private boolean m_isProgramDead;
 
     private boolean m_log;
 
     private int m_protocolVersion = 1;
 
     private int m_commandNumber;
 
     private IOCallback m_callback;
 
     private PrintWriter m_out;
 
     private Process m_process;
 
     private String m_fullResponse;
 
     private String m_response;
 
     private String m_logPrefix;
 
     private String m_pid;
 
     private String m_program;
 
     private String[] m_supportedCommands;
 
     private ErrorThread m_errorThread;
 
     private InputThread m_inputThread;
 
     private MessageQueue m_queue;
 
     private TimeoutCallback m_timeoutCallback;
 
     private void handleErrorStream(String text)
     {
         if (text == null)
             return;
         if (m_log)
             System.err.print(text);
         if (m_callback != null)
             m_callback.receivedStdErr(text);
     }
 
     private static boolean isResponseLine(String line)
     {
         if (line.length() < 1)
             return false;
         char c = line.charAt(0);
         return (c == '=' || c == '?');
     }
 
     private synchronized void log(String msg)
     {
         if (m_log)
         {
             if (m_logPrefix != null)
                 System.err.print(m_logPrefix);
             System.err.println(msg);
         }
     }
 
     private String readLine(long timeout) throws GtpError
     {
         while (true)
         {            
             ReadMessage message;
             if (timeout < 0)
                 message = (ReadMessage)m_queue.waitFor();
             else
             {
                 message = null;
                 while (message == null)
                 {
                     message = (ReadMessage)m_queue.waitFor(timeout);
                     if (message == null)
                     {
                         assert(m_timeoutCallback != null);
                         if (! m_timeoutCallback.askContinue())
                         {
                             destroyProcess();
                             throwProgramDied();
                         }
                     }
                 }
             }
             if (! message.m_isError)
             {
                 String line = message.m_text;
                 if (line == null)
                 {
                     m_isProgramDead = true;
                     while (! m_queue.isEmpty())
                     {
                         message = (ReadMessage)m_queue.waitFor();
                         assert(message.m_isError);
                         if (message.m_text != null)
                             handleErrorStream(message.m_text);
                     }
                     throwProgramDied();
                 }
                 log("<< " + line);
                 return line;
             }
             else
             {
                 StringBuffer buffer = new StringBuffer(2048);
                 while (message != null)
                 {
                     if (message.m_text != null)
                         buffer.append(message.m_text);
                     synchronized (m_queue)
                     {
                         message = (ReadMessage)m_queue.unsynchronizedPeek();
                         if (message != null && ! message.m_isError)
                             message = null;
                     }
                     if (message != null)
                     {
                         message = (ReadMessage)m_queue.getIfAvaliable();
                     }
                 }
                 handleErrorStream(buffer.toString());
             }
         }
     }
 
     private void readResponse(long timeout) throws GtpError
     {
         String line = "";
         while (line.trim().equals(""))
             line = readLine(timeout);
         StringBuffer response;
         while (true)
         {
             response = new StringBuffer(line);
             response.append("\n");
             if (isResponseLine(line))
                 break;
             m_fullResponse = response.toString();
             if (m_callback != null)
                 m_callback.receivedInvalidResponse(response.toString());
             if (m_invalidResponseCallback != null)
                 m_invalidResponseCallback.show(line);       
             line = readLine(timeout);
         }
         boolean error = (line.charAt(0) != '=');
         if (m_callback != null)
             m_callback.receivedResponse(error, response.toString());
         boolean done = false;
         while (! done)
         {
             line = readLine(timeout);
             done = line.equals("");
             response.append(line);
             response.append("\n");
             if (m_callback != null)
                 m_callback.receivedResponse(error, line + "\n");
         }
         m_fullResponse = response.toString();
         assert(response.length() >= 3);            
         int index = response.indexOf(" ");
         if (index < 0)
             m_response = response.substring(1, response.length() - 2);
         else
             m_response =
                 response.substring(index + 1, response.length() - 2);
         if (error)
         {
             String message = m_response.trim();
             if (message.equals(""))
                 message = "GTP command failed";
             throw new GtpError(message);
         }
     }
 
     private void throwProgramDied() throws GtpError
     {
         throw new GtpError("Go program died");
     }
 }
 
 //----------------------------------------------------------------------------
