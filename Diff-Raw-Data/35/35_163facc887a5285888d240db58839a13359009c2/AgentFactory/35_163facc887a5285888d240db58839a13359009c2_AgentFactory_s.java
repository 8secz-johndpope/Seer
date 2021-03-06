 package com.almende.eve.agent;
 
 import java.lang.reflect.InvocationHandler;
 import java.lang.reflect.Method;
 import java.lang.reflect.Proxy;
 import java.net.ProtocolException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.CopyOnWriteArrayList;
 import java.util.logging.Logger;
 
 import com.almende.eve.agent.annotation.Sender;
 import com.almende.eve.agent.annotation.ThreadSafe;
 import com.almende.eve.agent.log.EventLogger;
 import com.almende.eve.config.Config;
 import com.almende.eve.rpc.RequestParams;
 import com.almende.eve.rpc.jsonrpc.JSONRPC;
 import com.almende.eve.rpc.jsonrpc.JSONRPCException;
 import com.almende.eve.rpc.jsonrpc.JSONRequest;
 import com.almende.eve.rpc.jsonrpc.JSONResponse;
 import com.almende.eve.scheduler.Scheduler;
 import com.almende.eve.scheduler.SchedulerFactory;
 import com.almende.eve.state.State;
 import com.almende.eve.state.StateFactory;
 import com.almende.eve.transport.AsyncCallback;
 import com.almende.eve.transport.TransportService;
 import com.almende.eve.transport.http.HttpService;
 import com.almende.util.ClassUtil;
 
 /**
  * The AgentFactory is a factory to instantiate and invoke Eve Agents within the 
  * configured state. The AgentFactory can invoke local as well as remote 
  * agents.
  * 
  * An AgentFactory must be instantiated with a valid Eve configuration file.
  * This configuration is needed to load the configured agent classes and 
  * instantiate a state for each agent.
  * 
  * Example usage:
  *     // generic constructor
  *     Config config = new Config("eve.yaml");
  *     AgentFactory factory = new AgentFactory(config);
  *     
  *     // construct in servlet
  *     InputStream is = getServletContext().getResourceAsStream("/WEB-INF/eve.yaml");
  *     Config config = new Config(is);
  *     AgentFactory factory = new AgentFactory(config);
  *     
  *     // create or get a shared instance of the AgentFactory
  *     AgentFactory factory = AgentFactory.createInstance(namespace, config);
  *     AgentFactory factory = AgentFactory.getInstance(namespace);
  *     
  *     // invoke a local agent by its id
  *     response = factory.invoke(agentId, request); 
  *
  *     // invoke a local or remote agent by its url
  *     response = factory.send(senderId, receiverUrl, request);
  *     
  *     // create a new agent
  *     Agent agent = factory.createAgent(agentType, agentId);
  *     String desc = agent.getDescription(); // use the agent
  *     agent.destroy(); // neatly shutdown the agents state
  *     
  *     // instantiate an existing agent
  *     Agent agent = factory.getAgent(agentId);
  *     String desc = agent.getDescription(); // use the agent
  *     agent.destroy(); // neatly shutdown the agents state
  * 
  * @author jos
  */
 public class AgentFactory {
 	
 	// Note: the CopyOnWriteArrayList is inefficient but thread safe. 
 	private List<TransportService> transportServices = new CopyOnWriteArrayList<TransportService>();
 	private StateFactory stateFactory = null;
 	private SchedulerFactory schedulerFactory = null;
 	private Config config = null;
 	private EventLogger eventLogger = new EventLogger(this);
 
 	private static String ENVIRONMENT_PATH[] = new String[] {
 		"com.google.appengine.runtime.environment",
 		"com.almende.eve.runtime.environment"
 	};
 	private static String environment = null;
 	
 	private static Map<String, AgentFactory> factories = 
 			new ConcurrentHashMap<String, AgentFactory>();  // namespace:factory
 
 	private final static Map<String, String> STATE_FACTORIES = new HashMap<String, String>();
 	static {
         STATE_FACTORIES.put("FileStateFactory", "com.almende.eve.state.FileStateFactory");
         STATE_FACTORIES.put("MemoryStateFactory", "com.almende.eve.state.MemoryStateFactory");
         STATE_FACTORIES.put("DatastoreStateFactory", "com.almende.eve.state.google.DatastoreStateFactory");
     }
 
 	private final static Map<String, String> SCHEDULERS = new HashMap<String, String>();
 	static {
 		SCHEDULERS.put("RunnableSchedulerFactory",  "com.almende.eve.scheduler.RunnableSchedulerFactory");
 		SCHEDULERS.put("ClockSchedulerFactory",  "com.almende.eve.scheduler.ClockSchedulerFactory");
 		SCHEDULERS.put("GaeSchedulerFactory", "com.almende.eve.scheduler.google.GaeSchedulerFactory");
 	}
 	
 	private final static Map<String, String> TRANSPORT_SERVICES = new HashMap<String, String>();
 	static {
 		TRANSPORT_SERVICES.put("XmppService", "com.almende.eve.transport.xmpp.XmppService");
 		TRANSPORT_SERVICES.put("HttpService", "com.almende.eve.transport.http.HttpService");
     }
 
 	private final static RequestParams eveRequestParams = new RequestParams();
 	static {
 		eveRequestParams.put(Sender.class, null);
 	}
 	
 	private static AgentCache agents;
 	
 	private static Logger logger = Logger.getLogger(AgentFactory.class.getSimpleName());
 	
 	public AgentFactory () {
 		agents = new AgentCache();
 
 		// ensure there is always an HttpService for outgoing calls
 		addTransportService(new HttpService());
 	}
 
 	/**
 	 * Construct an AgentFactory and initialize the configuration
 	 * @param config
      * @throws Exception
 	 */
 	public AgentFactory(Config config) throws Exception {
 		this.config = config;
 		if (config != null) {
 			agents = new AgentCache(config);
 
 			// initialize all factories for state, transport, and scheduler
 			// important to initialize in the correct order: cache first, 
 			// then the state and transport services, and lastly scheduler.
 			setStateFactory(config);
 			addTransportServices(config);
 			// ensure there is always an HttpService for outgoing calls
 			addTransportService(new HttpService()); 
 			setSchedulerFactory(config);
 			addAgents(config);
 		}
 		else {
 			agents = new AgentCache();
 
 			// ensure there is always an HttpService for outgoing calls
 			addTransportService(new HttpService()); 
 		}
 	}
 	
 	/**
 	 * Get a shared AgentFactory instance with the default namespace "default"
 	 * @return factory     Returns the factory instance, or null when not 
 	 *                     existing 
 	 */
 	public static AgentFactory getInstance() {
 		return getInstance(null);
 	}
 
 	/**
 	 * Get a shared AgentFactory instance with a specific namespace
 	 * @param namespace    If null, "default" namespace will be loaded.
 	 * @return factory     Returns the factory instance, or null when not 
 	 *                     existing 
 	 */
 	public static AgentFactory getInstance(String namespace) {
 		if (namespace == null) {
 			namespace = "default";
 		}
 		return factories.get(namespace);
 	}
 	
 	/**
 	 * Create a shared AgentFactory instance with the default namespace "default"
 	 * @return factory
 	 */
 	public static synchronized AgentFactory createInstance() 
 			throws Exception{
 		return createInstance(null, null);
 	}
 	
 	/**
 	 * Create a shared AgentFactory instance with the default namespace "default"
 	 * @param config
 	 * @return factory
 	 */
 	public static synchronized AgentFactory createInstance(Config config) 
 			throws Exception{
 		return createInstance(null, config);
 	}
 
 	/**
 	 * Create a shared AgentFactory instance with a specific namespace
 	 * @param namespace
 	 * @return factory
 	 */
 	public static synchronized AgentFactory createInstance(String namespace) 
 			throws Exception {
 		return createInstance(namespace, null);
 	}
 	
 	/**
 	 * Create a shared AgentFactory instance with a specific namespace
 	 * @param namespace    If null, "default" namespace will be loaded.
 	 * @param config       If null, a non-configured AgentFactory will be
 	 *                     created.
 	 * @return factory
 	 * @throws Exception
 	 */
 	public static synchronized AgentFactory createInstance(String namespace, 
 			Config config) throws Exception {
 		if (namespace == null) {
 			namespace = "default";
 		}
 		
 		if (factories.containsKey(namespace)) {
 			throw new Exception("Shared AgentFactory with namespace '" + 
 					namespace + "' already exists. " +
 					"A shared AgentFactory can only be created once. " +
 					"Use getInstance instead to get the existing shared instance.");
 		}
 		
 		AgentFactory factory = new AgentFactory(config);
 		factories.put(namespace, factory);
 		
 		return factory;
 	}
 
 	/**
 	 * Get an agent by its id. Returns null if the agent does not exist
 	 * 
 	 * Before deleting the agent, the method agent.destroy() must be executed
 	 * to neatly shutdown the instantiated state.
 	 * 
 	 * @param agentId
 	 * @return agent
 	 * @throws Exception
 	 */
 	public Agent getAgent(String agentId) throws Exception {
 		if (agentId == null) {
 			return null;
 		}
 		
 		//Check if agent is instantiated already, returning if it is:
 		Agent agent = agents.get(agentId);
 		if (agent != null){
 			//System.err.println("Agent "+agentId+" found in cache!");
 			return agent;
 		}
 		//No agent found, normal initialization:
 		
 		// load the State
 		State state = null; 
 		state = getStateFactory().get(agentId);
 		if (state == null) {
 			// agent does not exist
 			return null;
 		}
 		state.init();
 		
 		// read the agents class name from state
 		Class<?> agentType = state.getAgentType();
 		if (agentType == null) {
 			throw new Exception("Cannot instantiate agent. " +
 					"Class information missing in the agents state " +
 					"(agentId='" + agentId + "')");
 		}
 		
 		// instantiate the agent
 		agent = (Agent) agentType.getConstructor().newInstance();
 		agent.setAgentFactory(this);
 		agent.setState(state);
 		agent.init();
 		
 		if (agentType.isAnnotationPresent(ThreadSafe.class) && 
 				agentType.getAnnotation(ThreadSafe.class).value()){
 			//System.err.println("Agent "+agentId+" is threadSafe, keeping!");
 			agents.put(agentId, agent);
 		}
 		
 		return agent;
 	}
 
 	/**
 	 * Create an agent proxy from an java interface
 	 * @param senderId        Internal id of the sender agent.
 	 *                        Not required for all transport services 
 	 *                        (for example not for outgoing HTTP requests)
 	 * @param receiverUrl     Url of the receiving agent
 	 * @param agentInterface  A java Interface, extending AgentInterface
 	 * @return
 	 */
 	@SuppressWarnings("unchecked")
 	public <T> T createAgentProxy(final String senderId, final String receiverUrl,
 			Class<T> agentInterface) {
 		if (!ClassUtil.hasInterface(agentInterface, AgentInterface.class)) {
 			throw new IllegalArgumentException("agentInterface must extend " + 
 					AgentInterface.class.getName());
 		}
 		
 		// http://docs.oracle.com/javase/1.4.2/docs/guide/reflection/proxy.html
 		T proxy = (T) Proxy.newProxyInstance(agentInterface.getClassLoader(),
 				new Class[] { agentInterface },
 				new InvocationHandler() {
 					public Object invoke(Object proxy, Method method,
 							Object[] args) throws Throwable {
 						String id = getAgentId(receiverUrl);
 						if (id != null) {
 							// local agent
 							Agent agent = getAgent(id);
 							return method.invoke(agent, args);
 						}
 						else {
 							// remote agent
 							JSONRequest request = JSONRPC.createRequest(method, args);
 							JSONResponse response = send(senderId, receiverUrl, request);
 							
 							JSONRPCException err = response.getError();
 							if (err != null) {
 								throw err;
 							}
 							else if (response.getResult() != null &&
 									!method.getReturnType().equals(Void.TYPE)) {
 								return response.getResult(method.getReturnType());
 							}
 							else {
 								return null;
 							}
 						}
 					}
 				});
 		
 		// TODO: for optimization, one can cache the created proxy's
 
 		return proxy;
 	}
 
 	/**
 	 * Create an agent.
 	 * 
 	 * Before deleting the agent, the method agent.destroy() must be executed
 	 * to neatly shutdown the instantiated state.
 	 * 
 	 * @param agentType  full class path
 	 * @param agentId
 	 * @return
 	 * @throws Exception
 	 */
 	public Agent createAgent(String agentType, String agentId) throws Exception {
 		return (Agent) createAgent(Class.forName(agentType), agentId);
 	}
 	
 	/**
 	 * Create an agent.
 	 * 
 	 * Before deleting the agent, the method agent.destroy() must be executed
 	 * to neatly shutdown the instantiated state.
 	 * 
 	 * @param agentType
 	 * @param agentId
 	 * @return
 	 * @throws Exception
 	 */
 	public Agent createAgent(Class<?> agentType, String agentId) throws Exception {
 		if (!ClassUtil.hasSuperClass(agentType, Agent.class)) {
 			throw new Exception(
 					"Class " + agentType + " does not extend class " + Agent.class);
 		}
 
 		// validate the Eve agent and output as warnings
 		List<String> errors = JSONRPC.validate(agentType, eveRequestParams);
 		for (String error : errors) {
 			logger.warning("Validation error class: " + agentType.getName() + 
 					", message: " + error);
 		}
 		
 		// create the state
 		State state = getStateFactory().create(agentId);
 		state.setAgentType(agentType);
 		state.destroy();
 
 		// instantiate the agent
 		Agent agent = (Agent) agentType.getConstructor().newInstance();
 		agent.setAgentFactory(this);
 		agent.setState(state);
 		agent.create();
 		agent.init();
 
 		if (agentType.isAnnotationPresent(ThreadSafe.class) && 
 				agentType.getAnnotation(ThreadSafe.class).value()){
 			//System.err.println("Agent "+agentId+" is threadSafe, keeping!");
 			agents.put(agentId, agent);
 		}
 		
 		return agent;
 	}
 	
 	/**
 	 * Delete an agent
 	 * @param agentId
 	 * @throws Exception 
 	 */
 	public void deleteAgent(String agentId) throws Exception {
 		Exception e = null;
 		if (agentId == null) {
 			return;
 		}
		schedulerFactory.destroyScheduler(agentId);
 		try {
 			// get the agent and execute the delete method
 			Agent agent = getAgent(agentId);
 			agent.destroy();
 			agent.delete();
 			agents.delete(agentId);
 			agent = null;
 		}
 		catch (Exception err) {
 			e = err;
 		}
 
 		try {
 			// delete the state, even if the agent.destroy or agent.delete
 			// failed.
 			getStateFactory().delete(agentId);
 		}
 		catch (Exception err) {
 			if (e == null) {
 				e = err;
 			}
 		}
 		
 		// rethrow the first exception
 		if (e != null) {
 			throw e;
 		}
 	}
 	
 	/**
 	 * Test if an agent exists
 	 * @param agentId
 	 * @return true if the agent exists
 	 * @throws Exception 
 	 */
 	public boolean hasAgent(String agentId) throws Exception {
 		return getStateFactory().exists(agentId);
 	}
 
 	/**
 	 * Get the event logger. The event logger is used to temporary log 
 	 * triggered events, and display them on the agents web interface.
 	 * @return eventLogger
 	 */
 	public EventLogger getEventLogger() {
 		return eventLogger;
 	}
 	
 	/**
 	 * Invoke a local agent
 	 * @param receiverId  Id of the receiver agent 
 	 * @param request
 	 * @param requestParams
 	 * @return
 	 * @throws Exception
 	 */
 	// TOOD: cleanup this method?
 	public JSONResponse invoke(String receiverId, 
 			JSONRequest request, RequestParams requestParams) throws Exception {
 		Agent receiver = getAgent(receiverId);
 		if (receiver != null) {
 			JSONResponse response = JSONRPC.invoke(receiver, request, requestParams);
 			receiver.destroy();
 			return response;
 		}
 		else {
 			throw new Exception("Agent with id '" + receiverId + "' not found");
 		}
 	}
 
 	/**
 	 * Invoke a local or remote agent. 
 	 * In case of an local agent, the agent is invoked immediately.
 	 * In case of an remote agent, an HTTP Request is sent to the concerning
 	 * agent.
 	 * @param senderId    Internal id of the sender agent
 	 *                    Not required for all transport services 
 	 *                    (for example not for outgoing HTTP requests)
 	 * @param receiverUrl
 	 * @param request
 	 * @return
 	 * @throws Exception
 	 */
 	public JSONResponse send(String senderId, String receiverUrl, JSONRequest request) 
 			throws Exception {
 		String agentId = getAgentId(receiverUrl);
 		if (agentId != null) {
 			// local agent, invoke locally
 			// TODO: provide Sender in requestParams
 			RequestParams requestParams = new RequestParams();
 			requestParams.put(Sender.class, null);
 			JSONResponse response = invoke(agentId, request, requestParams);
 			return response;
 		}
 		else {
 			TransportService service = null;
 			String protocol = null;
 			int separator = receiverUrl.indexOf(":");
 			if (separator != -1) {
 				protocol = receiverUrl.substring(0, separator);
 				service = getTransportService(protocol);
 			}
 			if (service != null) {
 				JSONResponse response = service.send(senderId, receiverUrl, request);
 				return response;
 			}
 			else {
 				throw new ProtocolException(
 					"No transport service configured for protocol '" + protocol + "'.");
 			}			
 		}
 	}
 	
 	/**
 	 * Asynchronously invoke a request on an agent.
 	 * @param senderId    Internal id of the sender agent. 
 	 *                    Not required for all transport services 
 	 *                    (for example not for outgoing HTTP requests)
 	 * @param receiverUrl
 	 * @param request
 	 * @param callback
 	 * @throws Exception 
 	 */
 	public void sendAsync(final String senderId, final String receiverUrl, 
 			final JSONRequest request, 
 			final AsyncCallback<JSONResponse> callback) throws Exception {
 		final String receiverId = getAgentId(receiverUrl);
 		if (receiverId != null) {
 			new Thread(new Runnable () {
 				@Override
 				public void run() {
 					JSONResponse response;
 					try {
 						// TODO: provide Sender in requestParams
 						RequestParams requestParams = new RequestParams();
 						requestParams.put(Sender.class, null);
 						response = invoke(receiverId, request, requestParams);
 						callback.onSuccess(response);
 					} catch (Exception e) {
 						callback.onFailure(e);
 					}
 				}
 			}).start();
 		}
 		else {
 			TransportService service = null;
 			String protocol = null;
 			int separator = receiverUrl.indexOf(":");
 			if (separator != -1) {
 				protocol = receiverUrl.substring(0, separator);
 				service = getTransportService(protocol);
 			}
 			if (service != null) {
 				service.sendAsync(senderId, receiverUrl, request, callback);
 			}
 			else {
 				throw new ProtocolException(
 					"No transport service configured for protocol '" + protocol + "'.");
 			}
 		}
 	}
 
 	/**
 	 * Get the agentId from given agentUrl. The url can be any protocol.
 	 * If the url matches any of the registered transport services, 
 	 * an agentId is returned.
 	 * This means that the url represents a local agent. It is possible
 	 * that no agent with this id exists.
 	 * @param agentUrl
 	 * @return agentId
 	 */
 	private String getAgentId(String agentUrl) {
 		for (TransportService service : transportServices) {
 			String agentId = service.getAgentId(agentUrl);
 			if (agentId != null) {
 				return agentId;
 			}
 		}		
 		return null;
 	}
 	
 	/**
 	 * Retrieve the current environment, using the configured State.
 	 * Can return values like "Production", "Development". If no environment
 	 * variable is found, "Production" is returned.
 	 * @return environment
 	 */
 	public static String getEnvironment() {
 		if (environment == null) {
 			for (String path : ENVIRONMENT_PATH) {
 				environment = System.getProperty(path);
 				if (environment != null) {
 					logger.info("Current environment: '" + environment + 
 							"' (read from path '" + path + "')");
 					break;
 				}
 			}
 			
 			if (environment == null) {
 				// no environment variable found. Fall back to "Production"
 				environment = "Production";
 
 				String msg = "No environment variable found. " +
 						"Environment set to '" + environment + "'. Checked paths: ";
 				for (String path : ENVIRONMENT_PATH) {
 					msg += path + ", ";
 				}
 				logger.warning(msg);
 			}
 		}
 		
 		return environment;
 	}
 
 	/**
 	 * Programmatically set the environment
 	 * @param env   The environment, for example "Production" or "Development" 
 	 * @return
 	 */
 	public static void setEnvironment(String env) {
 		environment = env;
 	}
 	
 	/**
 	 * Get the loaded config file
 	 * @return config   A configuration file
 	 */
 	public void setConfig(Config config) {
 		this.config = config;
 	}
 	
 	/**
 	 * Get the loaded config file
 	 * @return config   A configuration file
 	 */
 	public Config getConfig() {
 		return config;
 	}
 
 	/**
 	 * Load a state factory from config 
 	 * @param config
 	 * @throws Exception
 	 */
 	public void setStateFactory(Config config) {
 		// get the class name from the config file
 		// first read from the environment specific configuration,
 		// if not found read from the global configuration
 		String className = config.get("state", "class");
 		String configName = "state";
 		if (className == null) {
 			className = config.get("context", "class");
 			if (className == null) {
 				throw new IllegalArgumentException(
 						"Config parameter 'state.class' missing in Eve configuration.");
 			} else {
 				logger.warning("Use of config parameter 'context' is deprecated, please use 'state' instead.");
 				configName="context";
 			}
 		}
 		
 		// TODO: deprecated since "2013-02-20"
 		if ("FileContextFactory".equals(className)){
 			logger.warning("Use of Classname FileContextFactory is deprecated, please use 'FileStateFactory' instead.");
 			className="FileStateFactory";
 		}
 		if ("MemoryContextFactory".equals(className)){
 			logger.warning("Use of Classname MemoryContextFactory is deprecated, please use 'MemoryStateFactory' instead.");
 			className="MemoryStateFactory";
 		}
 		if ("DatastoreContextFactory".equals(className)){
 			logger.warning("Use of Classname DatastoreContextFactory is deprecated, please use 'DatastoreStateFactory' instead.");
 			className="DatastoreStateFactory";
 		}
 		
 		// Recognize known classes by their short name,
 		// and replace the short name for the full class path
 		for (String name : STATE_FACTORIES.keySet()) {
 			if (className.toLowerCase().equals(name.toLowerCase())) {
 				className = STATE_FACTORIES.get(name);
 				break;
 			}
 		}
 		
 		try {
 			// get the class
 			Class<?> stateClass = Class.forName(className);
 			if (!ClassUtil.hasInterface(stateClass, StateFactory.class)) {
 				throw new IllegalArgumentException(
 						"State factory class " + stateClass.getName() + 
 						" must extend " + State.class.getName());
 			}
 	
 			// instantiate the state factory
 			Map<String, Object> params = config.get(configName);
 			StateFactory stateFactory = (StateFactory) stateClass
 					.getConstructor(AgentFactory.class, Map.class )
 					.newInstance(this, params);
 
 			setStateFactory(stateFactory);
 			logger.info("Initialized state factory: " + stateFactory.toString());
 		}
 		catch (Exception e) {
 			e.printStackTrace();
 		}		
 	}
 	
 	/**
 	 * Create agents from a config (only when they do not yet exist).
 	 * Agents will be read from the configuration path bootstrap.agents,
 	 * which must contain a map where the keys are agentId's and the values
 	 * are the agent types (full java class path).
 	 * @param config
 	 */
 	public void addAgents (Config config) {
 		Map<String, String> agents = config.get("bootstrap", "agents");
 		if (agents != null) {
 			for (Entry<String, String> entry : agents.entrySet()) {
 				String agentId = entry.getKey();
 				String agentType = entry.getValue();
 				try {
 					Agent agent = getAgent(agentId);
 					if (agent == null) {
 						// agent does not yet exist. create it
 						agent = createAgent(agentType, agentId);
 						agent.destroy();
 						logger.info("Bootstrap created agent id=" + agentId + 
 								", type=" + agentType);
 					}
 				} catch (Exception e) {
 					e.printStackTrace();
 				}
 			}
 		}
 	}
 
 	/**
 	 * Set a state factory. The state factory is used to get/create/delete
 	 * an agents state.
 	 * @param stateFactory
 	 */
 	public void setStateFactory(StateFactory stateFactory) {
 		this.stateFactory = stateFactory;
 	}
 
 	/**
 	 * Get the configured state factory.
 	 * @return stateFactory
 	 */
 	public StateFactory getStateFactory() throws Exception {
 		if (stateFactory == null) {
 			throw new Exception("No state factory initialized.");
 		}
 		return stateFactory;
 	}
 
 	/**
 	 * Load a scheduler factory from a config file
 	 * @param config
 	 * @throws Exception
 	 */
 	public void setSchedulerFactory(Config config) {
 		// get the class name from the config file
 		// first read from the environment specific configuration,
 		// if not found read from the global configuration
 		String className = config.get("environment", getEnvironment(), "scheduler", "class");
 		if (className == null) {
 			className = config.get("scheduler", "class");
 		}
 		if (className == null) {
 			throw new IllegalArgumentException(
 				"Config parameter 'scheduler.class' missing in Eve configuration.");
 		}
 
 		// TODO: remove warning some day (added 2013-01-22)
 		if (className.toLowerCase().equals("RunnableScheduler".toLowerCase())) {
 			logger.warning("Deprecated class RunnableScheduler configured. Use RunnableSchedulerFactory instead to configure a scheduler factory.");
 			className = "RunnableSchedulerFactory";
 		}
 		if (className.toLowerCase().equals("AppEngineScheduler".toLowerCase())) {
 			logger.warning("Deprecated class AppEngineScheduler configured. Use GaeSchedulerFactory instead to configure a scheduler factory.");
 			className = "GaeSchedulerFactory";
 		}
 		if (className.toLowerCase().equals("AppEngineSchedulerFactory".toLowerCase())) {
 			logger.warning("Deprecated class AppEngineSchedulerFactory configured. Use GaeSchedulerFactory instead to configure a scheduler factory.");
 			className = "GaeSchedulerFactory";
 		}
 		
 		// Recognize known classes by their short name,
 		// and replace the short name for the full class path
 		for (String name : SCHEDULERS.keySet()) {
 			if (className.toLowerCase().equals(name.toLowerCase())) {
 				className = SCHEDULERS.get(name);
 				break;
 			}
 		}
 
 		// read all scheduler params (will be fed to the scheduler factory
 		// on construction)
 		Map<String, Object> params = config.get("environment", getEnvironment(), "scheduler");
 		if (params == null) {
 			params = config.get("scheduler");
 		}
 		
 		try {
 			// get the class
 			Class<?> schedulerClass = Class.forName(className);
 			if (!ClassUtil.hasInterface(schedulerClass, SchedulerFactory.class)) {
 				throw new IllegalArgumentException(
 						"Scheduler class " + schedulerClass.getName() + 
 						" must implement " + SchedulerFactory.class.getName());
 			}
 			
 			// initialize the scheduler factory
 			SchedulerFactory schedulerFactory = (SchedulerFactory) schedulerClass
 						.getConstructor(AgentFactory.class, Map.class )
 						.newInstance(this, params);
 
 			setSchedulerFactory(schedulerFactory);
 			
 			logger.info("Initialized scheduler factory: " + 
 					schedulerFactory.getClass().getName());
 		}
 		catch (Exception e) {
 			e.printStackTrace();
 		}		
 	}
 
 	/**
 	 * Load transport services for incoming and outgoing messages from a config
 	 * (for example http and xmpp services).
 	 * @param config
 	 */
 	public void addTransportServices(Config config) {
 		if (config == null) {
 			Exception e = new Exception("Configuration uninitialized");
 			e.printStackTrace();
 			return;
 		}
 		
 		// create a list to hold both global and environment specific transport
 		List<Map<String, Object>> allTransportParams = 
 				new ArrayList<Map<String, Object>>();
 		
 		// read global service params
 		List<Map<String, Object>> globalTransportParams = 
 				config.get("transport_services");
 		if (globalTransportParams == null) {
 			// TODO: cleanup some day. deprecated since 2013-01-17
 			globalTransportParams = config.get("services");
 			if (globalTransportParams != null) {
 				logger.warning("Property 'services' is deprecated. Use 'transport_services' instead.");
 			}
 		}
 		if (globalTransportParams != null) {
 			allTransportParams.addAll(globalTransportParams);
 		}
 
 		// read service params for the current environment
 		List<Map<String, Object>> environmentTransportParams = 
 				config.get("environment", getEnvironment(), "transport_services");
 		if (environmentTransportParams == null) {
 			// TODO: cleanup some day. deprecated since 2013-01-17
 			environmentTransportParams = config.get("environment", getEnvironment(), "services");
 			if (environmentTransportParams != null) {
 				logger.warning("Property 'services' is deprecated. Use 'transport_services' instead.");
 			}
 		}
 		if (environmentTransportParams != null) {
 			allTransportParams.addAll(environmentTransportParams);
 		}
 		
 		int index = 0;
 		for (Map<String, Object> transportParams : allTransportParams) {
 			String className = (String) transportParams.get("class");
 			try {
 				if (className != null) {
 					// Recognize known classes by their short name,
 					// and replace the short name for the full class path
 					
 					// TODO: remove deprecation warning some day (added 2013-01-24)
 					if (className.toLowerCase().equals("XmppTransportService".toLowerCase())) {
 						logger.warning("Deprecated class XmppTransportService, use XmppService instead.");
 						className = "XmppService";
 					}
 					if (className.toLowerCase().equals("HttpTransportService".toLowerCase())) {
 						logger.warning("Deprecated class HttpTransportService, use HttpService instead.");
 						className = "HttpService";
 					}
 
 					for (String name : TRANSPORT_SERVICES.keySet()) {
 						if (className.toLowerCase().equals(name.toLowerCase())) {
 							className = TRANSPORT_SERVICES.get(name);
 							break;
 						}
 					}
 					
 					// get class
 					Class<?> transportClass = Class.forName(className);
 					if (!ClassUtil.hasInterface(transportClass, TransportService.class)) {
 						throw new IllegalArgumentException(
 								"TransportService class " + transportClass.getName() + 
 								" must implement " + TransportService.class.getName());
 					}
 					
 					// initialize the transport service
 					TransportService transport = (TransportService) transportClass
 							.getConstructor(AgentFactory.class, Map.class)
 							.newInstance(this, transportParams);
 
 					// register the service with the agent factory
 					addTransportService(transport);
 				}
 				else {
 					logger.warning("Cannot load transport service at index " + index + 
 							": no class defined.");
 				}
 			}
 			catch (Exception e) {
 				logger.warning("Cannot load service at index " + index + 
 						": " + e.getMessage());
 			}
 			index++;
 		}
 	}
 
 	/**
 	 * Add a new transport service
 	 * @param transportService
 	 */
 	public void addTransportService(TransportService transportService) {
 		transportServices.add(transportService);
 		logger.info("Registered transport service: " + transportService.toString());
 	}
 
 	/**
 	 * Remove a registered a transport service
 	 * @param transportService
 	 */
 	public void removeTransportService(TransportService transportService) {
 		transportServices.remove(transportService);
 		logger.info("Unregistered transport service " + transportService.toString());
 	}
 
 	/**
 	 * Get all registered transport services
 	 * @return transportService
 	 */
 	public List<TransportService> getTransportServices() {
 		return transportServices;
 	}
 	
 	/**
 	 * Get all registered transport services which can handle given protocol
 	 * @param protocol   A protocol, for example "http" or "xmpp"
 	 * @return transportService
 	 */
 	public List<TransportService> getTransportServices(String protocol) {
 		List<TransportService> filteredServices = new ArrayList<TransportService> ();
 		
 		for (TransportService service : transportServices) {
 			List<String> protocols = service.getProtocols();
 			if (protocols.contains(protocol)) {
 				filteredServices.add(service);
 			}
 		}
 		
 		return filteredServices;
 	}
 	
 	/**
 	 * Get the first registered transport service which supports given protocol. 
 	 * Returns null when none of the registered transport services can handle
 	 * the protocol.
 	 * @param protocol   A protocol, for example "http" or "xmpp"
 	 * @return service
 	 */
 	public TransportService getTransportService(String protocol) {
 		List<TransportService> services = getTransportServices(protocol);
 		if (services.size() > 0) {
 			return services.get(0);
 		}
 		return null;
 	}
 
 	public List<Object> getMethods(Agent agent, Boolean asJSON) {
 		return JSONRPC.describe(agent.getClass(), eveRequestParams, asJSON);	
 	}
 
 	/**
 	 * Set a scheduler factory. The scheduler factory is used to get/create/delete
 	 * an agents scheduler.
 	 * @param schedulerFactory
 	 */
 	public synchronized void setSchedulerFactory(SchedulerFactory schedulerFactory) {
 		this.schedulerFactory = schedulerFactory;
 	}
 
 	/**
 	 * create a scheduler for an agent
 	 * @param agentId
 	 * @return scheduler
 	 */
 	public synchronized Scheduler getScheduler(String agentId) {
 		return schedulerFactory.getScheduler(agentId);
 	}
 
 }
