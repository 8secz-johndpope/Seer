 package com.ramblingwood.minecraft.jsonapi;
 
 
 import java.io.File;
 import java.io.FilenameFilter;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.util.ArrayList;
 import java.util.Enumeration;
 import java.util.Hashtable;
 import java.util.List;
 import java.util.Properties;
 import java.util.logging.Logger;
 
 import org.json.simpleForBukkit.JSONArray;
 import org.json.simpleForBukkit.JSONAware;
 import org.json.simpleForBukkit.JSONObject;
 import org.json.simpleForBukkit.parser.JSONParser;
 
 import com.ramblingwood.minecraft.jsonapi.dynamic.Caller;
 import com.ramblingwood.minecraft.jsonapi.streams.ChatMessage;
 import com.ramblingwood.minecraft.jsonapi.streams.ConnectionMessage;
 import com.ramblingwood.minecraft.jsonapi.streams.ConsoleMessage;
 import com.ramblingwood.minecraft.jsonapi.streams.JSONAPIStream;
 import com.ramblingwood.minecraft.jsonapi.streams.StreamingResponse;
 
 
 public class JSONServer extends NanoHTTPD {
 	Hashtable<String, Object> methods = new Hashtable<String, Object>();
 	Hashtable<String, String> logins = new Hashtable<String, String>();
 	private JSONAPI inst;
 	private Logger outLog = Logger.getLogger("JSONAPI");
 	private Caller caller;
 
 	private ArrayList<ChatMessage> chat = new ArrayList<ChatMessage>(); 
 	private ArrayList<ConsoleMessage> console = new ArrayList<ConsoleMessage>(); 
 	private ArrayList<ConnectionMessage> connections = new ArrayList<ConnectionMessage>(); 
 
 	
 	public JSONServer(Hashtable<String, String> logins, JSONAPI plugin) throws IOException {
 		super(plugin.port);
 		inst = plugin;
 		caller = new Caller(inst);
 		caller.loadFile(new File(inst.getDataFolder()+File.separator+"methods.json"));
 		
 		File[] files = (new File(inst.getDataFolder()+File.separator+"methods"+File.separator)).listFiles(new FilenameFilter() {
 		    @Override
 		    public boolean accept(File dir, String name) {
 		        return name.endsWith(".json");
 		    }
 		});
 		
 		if(files != null && files.length > 0) {
 			for(File f : files) {
 				caller.loadFile(f);
 			}
 		}
 		
 		this.logins = logins;
 	}
 	
 	public Caller getCaller() {
 		return caller;
 	}
 	
 	public void logChat(String player, String message) {
 		chat.add(new ChatMessage(player, message));
 		if(chat.size() > 50) {
 			chat.remove(0);
 			chat.trimToSize();
 		}
 	}
 	
 	public void logConsole(String line) {
 		console.add(new ConsoleMessage(line));
 		if(console.size() > 50) {
 			console.remove(0);
 			console.trimToSize();
 		}
 	}
 	
 	public void logConnected(String player) {
 		connections.add(new ConnectionMessage(player, true));
 		if(connections.size() > 50) {
 			connections.remove(0);
 			connections.trimToSize();
 		}
 	}
 	
 	public void logDisconnected(String player) {
 		connections.add(new ConnectionMessage(player, false));
 		if(connections.size() > 50) {
 			connections.remove(0);
 			connections.trimToSize();
 		}
 	}	
 	
 	public boolean testLogin (String method, String hash) {
 		try {
 			boolean valid = false;
 			
 			Enumeration<String> e = logins.keys();
 			
 			while(e.hasMoreElements()) {
 				String user = e.nextElement();
 				String pass = logins.get(user);
 
 				String thishash = JSONAPI.SHA256(user+method+pass+inst.salt);
 				
 				if(thishash.equals(hash)) {
 					valid = true;
 					break;
 				}
 			}
 			
 			return valid;
 		}
 		catch (Exception e) {
 			return false;
 		}
 	}
 	
 	public static String callback (String callback, String json) {
 		if(callback == null || callback.equals("")) return json;
 		return callback.concat("(").concat(json).concat(")");
 	}
 	
 	public void info (final String log) {
 		if(inst.logging || !inst.logFile.equals("false")) {
 			outLog.info("[JSONAPI] " +log);
 		}
 	}	
 	
 	public void warning (final String log) {
 		if(inst.logging || !inst.logFile.equals("false")) {
 			outLog.warning("[JSONAPI] " +log);
 		}
 	}	
 	
 	@SuppressWarnings("unchecked")
 	@Override
 	public Response serve( String uri, String method, Properties header, Properties parms )	{
 		String callback = parms.getProperty("callback");
 		
 		if(inst.whitelist.size() > 0 && !inst.whitelist.contains(header.get("X-REMOTE-ADDR"))) {
 			outLog.warning("[JSONAPI] An API call from "+ header.get("X-REMOTE-ADDR") +" was blocked because "+header.get("X-REMOTE-ADDR")+" is not on the whitelist.");
 			return jsonRespone(returnAPIError("You are not allowed to make API calls."), callback, HTTP_FORBIDDEN);			
 		}
 
 		if(uri.equals("/api/subscribe")) {
 			String source = parms.getProperty("source");
 			String key = parms.getProperty("key");
 
 			if(!testLogin(source, key)) {
 				info("[Streaming API] "+header.get("X-REMOTE-ADDR")+": Invalid API Key.");
 				return jsonRespone(returnAPIError("Invalid API key."), callback, HTTP_FORBIDDEN);
 			}
 
 			info("[Streaming API] "+header.get("X-REMOTE-ADDR")+": source="+ source);
 
 			try {
 				if(source == null) {
 					throw new Exception();
 				}
 				
 				ArrayList<? extends JSONAPIStream> arr;
 				
 				if(source.equals("chat")) {
 					arr = chat;
 				}
 				else if(source.equals("connections")) {
 					arr = connections;
 				}
 				else if(source.equals("console")) {
 					arr = console;
 				}
 				else {
 					throw new Exception();
 				}
 
 				StreamingResponse out = new StreamingResponse(source, arr, callback);
 
 				return new NanoHTTPD.Response( HTTP_OK, MIME_PLAINTEXT, out);
 			} catch (Exception e) {
 				e.printStackTrace();
 				return jsonRespone(returnAPIError("'"+source+"' is not a valid stream source!"), callback, HTTP_NOTFOUND);
 			}
 		}
 		
		if(!uri.equals("/api/call") && !uri.equals("/api/call-multiple")) {
 			return new NanoHTTPD.Response(HTTP_NOTFOUND, MIME_PLAINTEXT, "File not found.");
 		}
 
 		Object args = parms.getProperty("args","[]");
 		String calledMethod = (String)parms.getProperty("method");
 
 		if(calledMethod == null) {
 			info("[API Call] "+header.get("X-REMOTE-ADDR")+": Parameter 'method' was not defined.");
 			return jsonRespone(returnAPIError("Parameter 'method' was not defined."), callback, HTTP_NOTFOUND);
 		}
 
 		String key = parms.getProperty("key");
 		if(!testLogin(calledMethod, key)) {
 			info("[API Call] "+header.get("X-REMOTE-ADDR")+": Invalid API Key.");
 			return jsonRespone(returnAPIError("Invalid API key."), callback, HTTP_FORBIDDEN);
 		}
 
 
 		info("[API Call] "+header.get("X-REMOTE-ADDR")+": method="+ parms.getProperty("method").concat("?args=").concat((String) args));
 
 		if(args == null || calledMethod == null) {
 			return jsonRespone(returnAPIError("You need to pass a method and an array of arguments."), callback, HTTP_NOTFOUND);
 		}
 		else {
 			try {
 				JSONParser parse = new JSONParser();
 				args = parse.parse((String) args);
 
 				if(uri.equals("/api/call-multiple")) {
 					List<String> methods = new ArrayList<String>();
 					List<Object> arguments = new ArrayList<Object>();
 					Object o = parse.parse(calledMethod);
 					if (o instanceof List<?> && args instanceof List<?>) {
 						methods = (List<String>)o;
 						arguments = (List<Object>)args;
 					}
 					else {
 						return jsonRespone(returnAPIException(new Exception("method and args both need to be arrays for /api/call-multiple")), callback);
 					}
 
 					int size = methods.size();
 					JSONArray arr = new JSONArray();
 					for(int i = 0; i < size; i++) {
 						arr.add(serveAPICall(methods.get(i), (arguments.size()-1 >= i ? arguments.get(i) : new ArrayList<Object>())));
 					}
 
 					return jsonRespone(returnAPISuccess(o, arr), callback);
 				}
 				else {
 					return jsonRespone(serveAPICall(calledMethod, args), callback);
 				}
 			}
 			catch (Exception e) {
 				return jsonRespone(returnAPIException(e), callback);
 			}
 		}
 	}
 
 	public JSONObject returnAPIException (Exception e) {
 		JSONObject r = new JSONObject();
 		r.put("result", "error");
 		StringWriter pw = new StringWriter();
 		e.printStackTrace(new PrintWriter( pw ));
 		e.printStackTrace();
 		r.put("error", "Caught exception: "+pw.toString().replaceAll("\\n", "\n").replaceAll("\\r", "\r"));
 		return r;
 	}
 
 	public JSONObject returnAPIError (String error) {
 		JSONObject r = new JSONObject();
 		r.put("result", "error");
 		r.put("error", error);
 		return r;
 	}
 
 	public JSONObject returnAPISuccess (Object calledMethod, Object result) {
 		JSONObject r = new JSONObject();
 		r.put("result", "success");
 		r.put("source", calledMethod);
 		r.put("success", result);
 		return r;
 	}
 
 	public NanoHTTPD.Response jsonRespone (JSONAware o, String callback, String code) {
 		return new NanoHTTPD.Response(code, MIME_JSON, callback(callback, o.toJSONString()));
 	}
 
 	public NanoHTTPD.Response jsonRespone (JSONAware o, String callback) {
 		return jsonRespone(o, callback, HTTP_OK);
 	}
 
 	@SuppressWarnings("unchecked")
 	public JSONObject serveAPICall(String calledMethod, Object args) {
 		try {
 			if(caller.methodExists(calledMethod)) {
 				if(args instanceof JSONArray) {
 					Object result = caller.call(calledMethod,
 							(Object[]) ((ArrayList) args).toArray(new Object[((ArrayList) args).size()]));
 					return returnAPISuccess(calledMethod, result);
 				}
 			}
 			else {
 				warning("The method '"+calledMethod+"' does not exist!");
 				return returnAPIError("The method '"+calledMethod+"' does not exist!");
 			}
 		}
 		catch (Exception e) {
 			return returnAPIException(e);
 		}
 
 		return returnAPIError("You need to pass a method and an array of arguments.");
 	}
 }
