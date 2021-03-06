 /**
  * @file TestAgent.java
  * 
  * @brief 
  * TODO: brief
  *
  * @license
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy 
  * of the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  *
  * Copyright © 2010-2012 Almende B.V.
  *
  * @author 	Jos de Jong, <jos@almende.org>
  * @date	  2011-03-05
  */
 package com.almende.test.agents;
 
 import java.io.IOException;
 import java.net.URI;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import com.almende.eve.agent.Agent;
 import com.almende.eve.agent.AgentFactory;
 import com.almende.eve.rpc.annotation.Access;
 import com.almende.eve.rpc.annotation.AccessType;
 import com.almende.eve.rpc.annotation.Name;
 import com.almende.eve.rpc.annotation.Required;
 import com.almende.eve.rpc.annotation.Sender;
 import com.almende.eve.rpc.jsonrpc.JSONRPCException;
 import com.almende.eve.rpc.jsonrpc.JSONRPCException.CODE;
 import com.almende.eve.rpc.jsonrpc.JSONRequest;
 import com.almende.eve.rpc.jsonrpc.jackson.JOM;
 import com.almende.eve.transport.AsyncCallback;
 import com.almende.eve.transport.xmpp.XmppService;
 import com.almende.test.agents.entity.Person;
 import com.fasterxml.jackson.databind.JavaType;
 import com.fasterxml.jackson.databind.node.ArrayNode;
 import com.fasterxml.jackson.databind.node.ObjectNode;
 import com.fasterxml.jackson.databind.type.TypeFactory;
 
 @Access(AccessType.PUBLIC)
 public class Test2Agent extends Agent implements Test2AgentInterface {
 	public String ping(@Name("message") String message, 
 			@Sender String sender) throws Exception {
 		ObjectNode params = JOM.createObjectNode();
 		params.put("message", message);
 		params.put("sender", sender);
 		
 		eventsFactory.trigger("message", params);
 		return message;
 	}
 
 	public void create() {
 		System.out.println("creating TestAgent/" + getId());
 		super.create();
 	}
 	
 	public void delete() {
 		System.out.println("deleting TestAgent/" + getId());
 		super.delete();
 	}
 
 	public void init() {
 		System.out.println("initializing TestAgent/" + getId());
 		super.init();
 	}
 	
 	public void destroy() {
 		System.out.println("destroying TestAgent/" + getId());
 		super.destroy();
 	}
 
 	public String getName(@Name("person") Person person) {
 		return person.getName();
 	}
 
 	public Double getMarksAvg(@Name("person") Person person) {
 		List<Double> marks = person.getMarks();
 		Double sum = new Double(0);
 		if (marks != null) {
 			for (Double mark : marks) {
 				sum += mark;
 			}
 		}
 		return sum;
 	}
 
 	private URI getMyUrl() {
 		return getFirstUrl();
 	}
 	
 	public String callMyself(@Name("method") String method, 
 			@Name("params") ObjectNode params) 
 			throws IOException, JSONRPCException, Exception {
 		String resp = send(getMyUrl(), method, params, JOM.getSimpleType(String.class));
 		System.out.println("callMyself method=" + method  + ", params=" + params.toString() + ", resp=" +  resp);
 		return resp;
 	}
 
 	public STATUS testEnum(@Name("status") STATUS status) {
 		System.out.println("Status: " + status);
 		return status;
 	}
 
 	public STATUS testEnumProxy() {
 		URI url = URI.create("http://eveagents.appspot.com/agents/test/");
 		Test2AgentInterface other = createAgentProxy(url, Test2AgentInterface.class);
 		
 		STATUS value = other.testEnum(STATUS.GOOD);
 		return value;
 	}
 
 	public void testVoid() {
 		System.out.println("testVoid");
 	}
 
 	public void testVoidProxy() {
 		URI url = URI.create("http://eveagents.appspot.com/agents/test/");
 		Test2AgentInterface other = createAgentProxy(url, Test2AgentInterface.class);
 		other.testVoid();
 	}
 
 	public STATUS testEnumSend() throws Exception {
 		URI url = URI.create("http://eveagents.appspot.com/agents/test/");
 		Map<String, Object> params = new HashMap<String, Object>();
 		params.put("status", STATUS.GOOD);
 		STATUS value = send(url, "testEnum", params, JOM.getSimpleType(STATUS.class));
 		
 		return value;
 	}
 	
 	public String cascade() throws IOException, JSONRPCException, Exception {
 		String name1 = get("name");
 		Map<String, Object> params = new HashMap<String, Object>();
 		params.put("key", "name");
 		params.put("value", Math.round(Math.random() * 1000));
 		send(getMyUrl(), "put" , params);
 
 		String name2 = (String)get("name");
 
 		System.out.println("callMyself name1=" + name1 + ", name2=" + name2);
 		return name1 + " " + name2;
 	}
 	
 	public Person cascade2() throws IOException, JSONRPCException, Exception {
 		// test sending a POJO as params
 		Person person = new Person();
 		person.setName("testname");
 		return send(getMyUrl(), "getPerson" , person, JOM.getSimpleType(Person.class));
 	}
 	
 	public Person getPerson(@Name("name") String name) {
 		Person person = new Person();
 		person.setName(name);
 		List<Double> marks = new ArrayList<Double>();
 		marks.add(6.8);
 		marks.add(5.0);
 		person.setMarks(marks);
 		return person;
 	}
 
 	public Double add(@Name("a") Double a, @Name("b") Double b) {
 		return a + b;
 	}
 
 	public Double subtract(@Name("a") Double a, @Name("b") Double b) {
 		return a - b;
 	}
 
 	public Double multiply(Double a, Double b) {
 		return a * b;
 	}
 
 	public Double divide(@Name("a") Double a, @Name("b") Double b) {
 		return a / b;
 	}
 
 	public String printParams(ObjectNode params) {
 		return "fields: " + params.size();
 	}
 
 	public void throwException() throws Exception {
 		throw new Exception("Nothing went wrong...");
 	}
 	
 	public void throwJSONRPCException() throws JSONRPCException {
 		throw new JSONRPCException(CODE.NOT_FOUND);
 	}
 	
 	// TODO: get this working
 	public Double sum(@Name("values") List<Double> values) {
 		Double sum = new Double(0);
 		for (Double value : values) {
 			sum += value;
 		}
 		return sum;
 	}
 	
 	public Double sumArray(@Name("values") Double[] values) {
 		Double sum = new Double(0);
 		for (Double value : values) {
 			sum += value;
 		}
 		return sum;
 	}
 
 	public void complexParameter(
 			@Name("values") Map<String, List<Double>> values) {
 		for (String key : values.keySet()) {
 			List<Double> value = values.get(key);
 			for (Double v : value) {
 				System.out.println(key + " " + v);
 			}
 		}
 	}
 	
 	public Map<String, List<Double>> complexResult() {
 		System.err.println("ComplexResult called!");
 		Map<String,List<Double>> result = new HashMap<String,List<Double>>();
 		List<Double> list = new ArrayList<Double>();
 		list.add(1.1);
 		list.add(0.4);
 		result.put("result", list);
 		
 		return result;
 	}
 	
 	public Double testTFComplexResult(@Name("url") String url) throws Exception{
 		TypeFactory tf = JOM.getTypeFactory();
 		Map<String, List<Double>> res = send(URI.create(url), "complexResult",JOM.createObjectNode(),tf.constructMapType(HashMap.class, JOM.getSimpleType(String.class),(JavaType)tf.constructCollectionType(List.class, Double.class)));
 		return res.get("result").get(0);
 	}
 
 	public Double testComplexResult(@Name("url") String url) throws Exception{
 		Map<String, List<Double>> res = new HashMap<String,List<Double>>();
 		
 		send(res, URI.create(url), "complexResult");
 		return res.get("result").get(0);
 	}
 
 	public Double increment() {
		Double value = getState().get("count",Double.class);
 		if (value == null) {
 			value = new Double(0);
 		}
 		value++;
 		getState().put("count", value);
 
 		return value;
 	}
 	
 	public Double testDoubleRet() throws Exception{
 		return send(new Double(1.1),getFirstUrl(),"increment");
 	}
 	
 	public String get(@Name("key") String key) {
		return getState().get(key,String.class);
 	}
 
 	public void put(@Name("key") String key, 
 			@Name("value") String value) {
 		getState().put(key, value);
 	}
 	
 	public void registerPingEvent() throws Exception {
 		eventsFactory.subscribe(getMyUrl(), "ping", "pingCallback");
 	}
 	
 	public void unregisterPingEvent() throws Exception {
 		eventsFactory.subscribe(getMyUrl(), "ping", "pingCallback");
 	}
 	
 	public void pingCallback(@Name("params") ObjectNode params) {
 		System.out.println("pingCallback " + getId() + " " + params.toString());
 	}
 	
 	public void triggerPingEvent(
 			@Name("message") @Required(false) String message ) throws Exception {
 		String event = "ping";
 		ObjectNode params = null;
 		if (message != null) {
 			params = JOM.createObjectNode();
 			params.put("message", message);
 		}
 		eventsFactory.trigger(event, params);
 	}
 
 	public void cancelTask(@Name("id") String id) {
 		getScheduler().cancelTask(id);
 	}
 	
 	public String createTask(@Name("delay") long delay) throws Exception {
 		ObjectNode params = JOM.createObjectNode();
 		params.put("message", "hello world");
 		JSONRequest request = new JSONRequest("myTask", params);
 		String id = getScheduler().createTask(request, delay);
 		return id;
 	}
 	
 	public void myTask(@Name("message") String message) throws Exception {
 		ObjectNode params = JOM.createObjectNode();
 		params.put("message", message);
 		eventsFactory.trigger("task", params);
 		System.out.println("myTask is executed. Message: " + message);
 	}
 
 	public Object testSend(@Name("url") @Required(false) String url,
 			@Name("method") @Required(false) String method) throws Exception {
 		if (url == null) {
 			url = "http://localhost:8080/EveCore/agents/chatagent/1/";
 		}
 		if (method == null) {
 			method = "getDescription";
 		}
 		Object res = send(URI.create(url), method, JOM.getSimpleType(Object.class));
 		System.out.println(res);
 		return res;
 	}
 
 	public String testSendNonExistingMethod() throws Exception {
 		String res = send(URI.create("http://localhost:8080/EveCore/agents/chatagent/1/"), 
				"nonExistingMethod", String.class);
 		System.out.println(res);
 		return res;
 	}
 	
 	public String subscribeToAgent(@Required(false) @Name("url") String url) throws Exception {
 		if (url == null) {
 				url = "http://localhost:8080/agents/testagent2/";
 		}
 		String event = "dataChanged";
 		String callback = "onEvent";
 		return eventsFactory.subscribe(URI.create(url), event, callback);
 	}
 
 	public void unsubscribeFromAgent(@Required(false) @Name("url") String url,
 			@Name("subscriptionId") String subscriptionId) throws Exception {
 		if (url == null) {
 			url = "http://localhost:8080/agents/testagent2/";
 		}
 		//String event = "dataChanged";
 		//String callback = "onEvent";
 		eventsFactory.unsubscribe(URI.create(url), subscriptionId);
 	}
 	
 	public void triggerDataChanged() throws Exception {
 		eventsFactory.trigger("dataChanged", null);
 	}
 	
 	public Object getEverything() {
 		return getState();
 	}
 	
 	public void onEvent(
 			@Required(false) @Name("subscriptionId") String subscriptionId,
 			@Required(false) @Name("agent") String agent,
 	        @Required(false) @Name("event") String event, 
 	        @Required(false) @Name("params") ObjectNode params) throws Exception {
 	    System.out.println("onEvent " +
 	    		"subscriptionId=" + subscriptionId + ", " +
 	            "agent=" + agent + ", " +
 	            "event=" + event + ", " +
 	            "params=" + ((params != null) ? params.toString() : null));
 	    
 	    ObjectNode data = JOM.createObjectNode();
 	    data.put("subscriptionId", subscriptionId);
 	    data.put("agent", agent);
 	    data.put("event", event);
 	    data.put("params", params);
 	    eventsFactory.trigger ("onEvent", data);
 	}
 
 	private String privateMethod() {
 		return "You should not be able to execute this method via JSON-RPC! " +
 			"It is private.";
 	}
 	
 	// multiple methods with the same name
 	public void methodVersionOne() {
 		privateMethod();
 	}
 	public void methodVersionOne(@Name("param") String param) {
 		privateMethod();
 	}
 
 	public String invalidMethod(@Name("param1") String param1, int param2) {
 		return "This method is no valid JSON-RPC method: misses an @Name annotation.";
 	}
 	
 	public void testAsyncXMPP (@Name("url") String url) throws Exception {
 		System.out.println("testAsyncSend, url=" + url);
 		String method = "multiply";
 		ObjectNode params = JOM.createObjectNode();
 		params.put("a", new Double(3));
 		params.put("b", new Double(4.5));
 		System.out.println("testAsyncXMPP, request=" + new JSONRequest(method, params));
 		sendAsync(URI.create(url), method, params, new AsyncCallback<Double>() {
 			@Override
 			public void onSuccess(Double result) {
 				System.out.println("testAsyncXMPP result=" + result);
 				ObjectNode params = JOM.createObjectNode();
 				params.put("result", result);
 				try {
 					eventsFactory.trigger("message", params);
 				} catch (Exception e) {
 					// TODO Auto-generated catch block
 					e.printStackTrace();
 				}
 			}
 
 			@Override
 			public void onFailure(Exception exception) {
 				exception.printStackTrace();
 			}
 		}, JOM.getSimpleType(Double.class));
 	}
 
 	public void testSyncXMPP (@Name("url") String url) throws Exception {
 		System.out.println("testSyncSend, url=" + url);
 		String method = "multiply";
 		ObjectNode params = JOM.createObjectNode();
 		params.put("a", new Double(3));
 		params.put("b", new Double(4.5));
 		System.out.println("testSyncXMPP, request=" + new JSONRequest(method, params));
 		Double result = send(URI.create(url), method, params, JOM.getSimpleType(Double.class));
 		System.out.println("testSyncXMPP result=" + result);
 		try {
 			ObjectNode messageParams = JOM.createObjectNode();
 			messageParams.put("result", result);
 			eventsFactory.trigger("message", messageParams);
 		} catch (Exception e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 	}
 	
 	public void testGetContacts (@Name("url") String url) throws Exception {
 		System.out.println("testGetContacts, url=" + url);
 		String method = "getContacts";
 		ObjectNode params = JOM.createObjectNode();
 		params.put("filter", "");
 		System.out.println("testGetContacts, request=" + new JSONRequest(method, params));
 		sendAsync(URI.create(url), method, params, new AsyncCallback<ArrayNode>() {
 			@Override
 			public void onSuccess(ArrayNode result) {
 				System.out.println("testGetContacts result=" + result);
 				ObjectNode params = JOM.createObjectNode();
 				params.put("result", result);
 				try {
 					eventsFactory.trigger("message", params);
 				} catch (Exception e) {
 					// TODO Auto-generated catch block
 					e.printStackTrace();
 				}
 			}
 
 			@Override
 			public void onFailure(Exception exception) {
 				exception.printStackTrace();
 			}
 		}, JOM.getSimpleType(ArrayNode.class));
 	}
 	
 	public void testAsyncHTTP () throws Exception {
 		System.out.println("testAsyncHTTP start...");
 		String url = "http://eveagents.appspot.com/agents/googledirectionsagent/1/";
 		String method = "getDurationHuman";
 		ObjectNode params = JOM.createObjectNode();
 		params.put("origin", "rotterdam");
 		params.put("destination", "utrecht");
 		sendAsync(URI.create(url), method, params, new AsyncCallback<String>() {
 			@Override
 			public void onSuccess(String result) {
 				System.out.println("testAsyncHTTP result=" + result);
 			}
 
 			@Override
 			public void onFailure(Exception exception) {
 				exception.printStackTrace();
 			}
 		}, JOM.getSimpleType(String.class));
 		
 		System.out.println("testAsyncHTTP end...");
 	}
 	
 	public void xmppConnect(@Name("username") String username, 
 			@Name("password") String password) throws Exception {
 		AgentFactory factory = getAgentFactory();
 		
 		XmppService service = (XmppService) factory.getTransportService("xmpp");
 		if (service != null) {
 			service.connect(getId(), username, password);
 		}
 		else {
 			throw new Exception("No XMPP service registered");
 		}
 	}
 	
 	public void xmppDisconnect() throws Exception {
 		AgentFactory factory = getAgentFactory();
 		XmppService service = (XmppService) factory.getTransportService("xmpp");
 		if (service != null) {
 			service.disconnect(getId());
 		}
 		else {
 			throw new Exception("No XMPP service registered");
 		}
 	}
 	
 	public void deleteMySelf() throws Exception {
 		getAgentFactory().deleteAgent(getId());
 	}
 	
 	public Double testAgentProxy() {
 		String url = "http://eveagents.appspot.com/agents/testagent/1/";
 		Test2AgentInterface other = createAgentProxy(URI.create(url), Test2AgentInterface.class);
 		Double value = other.increment();
 		return value;
 	}
 	
 	public Double testAgentProxy2() {
 		String url = "http://eveagents.appspot.com/agents/testagent/1/";
 		Test2AgentInterface other = createAgentProxy(URI.create(url), Test2AgentInterface.class);
 		
 		Double value = other.multiply(2.3, 4.5);
 		return value;
 	}
 
 	public List<Object> testAgentProxy3() {
 		String url = "http://eveagents.appspot.com/agents/testagent/1/";
 		Test2AgentInterface other = createAgentProxy(URI.create(url), Test2AgentInterface.class);
 		
 		List<Object> value = other.getMethods();
 		return value;
 	}
 	
 	public void testAgentProxy5() {
 		String url = "http://eveagents.appspot.com/agents/testagent/1/";
 		// This should fail, Person is no Interface...
 		Person other = createAgentProxy(URI.create(url), Person.class);
 		other.setName("bla");
 	}
 	
 	public Double testAgentProxy4() {
 		String url = "http://eveagents.appspot.com/agents/testagent/1/";
 		
 		Test2AgentInterface other = createAgentProxy(URI.create(url), Test2AgentInterface.class);
 		
 		Double value = other.add(2.3, null);
 		return value;
 	}
 	
 	public ArrayNode getUrlsOfGloria() throws Exception {
 		String url = "xmpp:gloria@openid.almende.org";
 		/* async works fine
 		sendAsync(url, "getUrls", JOM.createObjectNode(), new AsyncCallback<ArrayNode>() {
 			@Override
 			public void onSuccess(ArrayNode result) {
 				System.out.println("gloria's urls=" + urls);
 			}
 
 			@Override
 			public void onFailure(Exception exception) {
 				exception.printStackTrace();
 			}
 		}, ArrayNode.class);
 		*/
 		ArrayNode urls = send(URI.create(url), "getUrls", JOM.createObjectNode(), JOM.getSimpleType(ArrayNode.class));
 		System.out.println("gloria's urls=" + urls);
 		return urls;
 	}
 	
 	public void getUrlsOfMerlinAsync() throws Exception {
 		String url = "xmpp:merlin@openid.almende.org";
 		sendAsync(URI.create(url), "getUrls", JOM.createObjectNode(), new AsyncCallback<ArrayNode>() {
 			@Override
 			public void onSuccess(ArrayNode urls) {
 				System.out.println("merlins urls=" + urls);
 			}
 
 			@Override
 			public void onFailure(Exception exception) {
 				exception.printStackTrace();
 			}
 		}, JOM.getSimpleType(ArrayNode.class));
 	}
 
 	public ArrayNode getUrlsOfMerlin() throws Exception {
 		String url = "xmpp:merlin@openid.almende.org";
 		ArrayNode urls = send(URI.create(url), "getUrls", JOM.createObjectNode(), JOM.getSimpleType(ArrayNode.class));
 		System.out.println("merlins urls=" + urls);
 		return urls;
 	}
 	
 	public ArrayNode getUrlsOfJos() throws Exception {
 		String url = "xmpp:jos@openid.almende.org";
 		ArrayNode urls = send(URI.create(url), "getUrls", JOM.createObjectNode(), JOM.getSimpleType(ArrayNode.class));
 		System.out.println("jos's urls=" + urls);
 		return urls;
 	}
 		
 	public ArrayNode getListOfMerlin() throws Exception {
 		String url = "xmpp:merlin@openid.almende.org";
 		ArrayNode list = send(URI.create(url), "list", JOM.createObjectNode(),JOM.getSimpleType(ArrayNode.class));
 		System.out.println("merlins list=" + list);
 		return list;
 	}
 
 	@Override
 	public String getVersion() {
 		return "1.0";
 	}
 	
 	@Override
 	public String getDescription() {
 		return 
 		"This agent can be used for test purposes. " +
 		"It contains a simple ping method.";
 	}	
 }
