 /* This code is part of Freenet. It is distributed under the GNU General
  * Public License, version 2 (or at your option any later version). See
  * http://www.gnu.org/ for further details of the GPL. */
 package plugins.Freetalk.ui.FCP;
 
 import plugins.Freetalk.Freetalk;
 import freenet.pluginmanager.FredPluginFCP;
import freenet.pluginmanager.PluginNotFoundException;
 import freenet.pluginmanager.PluginReplySender;
 import freenet.support.SimpleFieldSet;
 import freenet.support.api.Bucket;
 
 /**
  * FCP message format:
  *   Command=name
  *   ... 
  */
 public final class FCPInterface implements FredPluginFCP {
     
     private final static int ERROR_EMPTY_MESSAGE        = 0;
     private final static int ERROR_COMMAND_NAME_MISSING = 1;
     private final static int ERROR_COMMAND_NAME_UNKNOWN = 2;
     
     private final static String[] ERROR_DESCRIPTIONS = {
         "Got empty message",
         "Invalid Command name",
         "Command not implemented"
     };
 
 	private final Freetalk mFreetalk;
 	
 	public FCPInterface(Freetalk myFreetalk) {
 		mFreetalk = myFreetalk;
 	}
 
     /**
      * @param replysender interface to send a reply
      * @param params parameters passed in, can be null
      * @param data a bucket of data passed in, can be null
      * @param access 0: direct call (plugin to plugin), 1: FCP restricted access,  2: FCP full access  
     * @throws PluginNotFoundException If the plugin has been unregistered.
      */
	public void handle(PluginReplySender replysender, SimpleFieldSet params, Bucket data, int accesstype) throws PluginNotFoundException {
 	    if (params == null) {
             sendError(replysender, ERROR_EMPTY_MESSAGE);
             return;
         }
 
         final String command = params.get("Command");
 
         if (command == null || command.trim().length() == 0) {
             sendError(replysender, ERROR_COMMAND_NAME_MISSING);
             return;
         }
 
         sendError(replysender, ERROR_COMMAND_NAME_UNKNOWN);
 	}
 
    /**
      * Send error to client.
      * 
      * @param replysender  the reply sender
      * @param code         error code
 * @throws PluginNotFoundException 
      */
    private void sendError(PluginReplySender replysender, int code) throws PluginNotFoundException {
         sendError(replysender, code, ERROR_DESCRIPTIONS[code]);
     }
 
 	/**
 	 * Send error to client.
 	 * 
 	 * Format:
 	 *   Status=Error
 	 *   Code=123
 	 *   Description=text...
 	 * 
 	 * @param replysender  the reply sender
 	 * @param code         error code
 	 * @param description  error description
	 * @throws PluginNotFoundException 
 	 */
	private void sendError(PluginReplySender replysender, int code, String description) throws PluginNotFoundException {
 	    SimpleFieldSet sfs = new SimpleFieldSet(true);
         sfs.putOverwrite("Status", "Error");
         sfs.put("Code", code);
         sfs.putOverwrite("Description", description);
         replysender.send(sfs);
     }
 }
