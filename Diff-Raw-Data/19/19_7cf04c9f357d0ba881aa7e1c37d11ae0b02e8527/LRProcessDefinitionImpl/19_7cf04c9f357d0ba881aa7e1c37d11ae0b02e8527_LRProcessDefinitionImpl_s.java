 package cz.incad.kramerius.processes.impl;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.StringTokenizer;
 import java.util.UUID;
 
 import javax.net.ssl.SSLEngineResult.Status;
 
 import org.apache.commons.lang.SystemUtils;
 import org.w3c.dom.Element;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 
 import com.google.inject.Inject;
 
 import cz.incad.kramerius.processes.LRDefinitionAction;
 import cz.incad.kramerius.processes.LRProcess;
 import cz.incad.kramerius.processes.LRProcessDefinition;
 import cz.incad.kramerius.processes.LRProcessManager;
 import cz.incad.kramerius.processes.States;
 import cz.incad.kramerius.processes.os.impl.windows.WindowsLRProcessImpl;
 import cz.incad.kramerius.utils.conf.KConfiguration;
 
 public class LRProcessDefinitionImpl implements LRProcessDefinition {
 
 	//public static final String DEFAULT_LIB_DIR="lib";
 	public static final String DEFAULT_USER_DIR ="";
 	
 	private String libsDir;
 	private String id;
 	private String mainClass;
 	private String description;
 	private String standardStreamFolder;
 	private String errStreamFolder;
 	
 	private List<String> parameters = new ArrayList<String>();
 	private List<String> javaProcessParameters = new ArrayList<String>();
 	
 	private LRProcessManager pm;
 	private KConfiguration configuration;
 
 	private List<LRDefinitionAction> actions = new ArrayList<LRDefinitionAction>();
 
 	public LRProcessDefinitionImpl(LRProcessManager pm, KConfiguration configuration) {
 		super();
 		this.pm = pm;
 		this.configuration = configuration;
 	}
 
 	@Override
 	public String getLibsDir() {
 		return this.libsDir;
 	}
 
 	@Override
 	public List<String> getParameters() {
 		return new ArrayList<String>(this.parameters);
 	}
 
 	
 	public List<LRDefinitionAction> getActions() {
		if (!this.actions.contains(LRDefinitionAction.LOGS_ACTION)) {
			this.actions.add(0,LRDefinitionAction.LOGS_ACTION);
		}
 		return new ArrayList<LRDefinitionAction>(this.actions);
 	}
 
 
 	public void loadFromXml(Element elm) {
 		NodeList nodes = elm.getChildNodes();
 		for (int i = 0,ll=nodes.getLength(); i < ll; i++) {
 			Node item = nodes.item(i);
 			if (item.getNodeType() == Node.ELEMENT_NODE) {
 				String nodeName = item.getNodeName();
 				if (nodeName.equals("id")) {
 					this.id = item.getTextContent();
 				}
 				if (nodeName.equals("mainClass")) {
 					this.mainClass = item.getTextContent();
 				}
 				if (nodeName.equals("description")) {
 					this.description = item.getTextContent();
 				}
 				if (nodeName.equals("lib")) {
 					this.libsDir = item.getTextContent();
 				}
 				if (nodeName.equals("standardOs")) {
 					this.standardStreamFolder = item.getTextContent();
 				}
 				if (nodeName.equals("errOs")) {
 					this.errStreamFolder = item.getTextContent();
 				}
 				if (nodeName.equals("parameters")) {
 					oldStyleParameters(item);
 					if (this.parameters.isEmpty()) {
 						newStyleParameters(item);
 					}
 				}
 				if (nodeName.equals("javaProcessParameters")) {
 					javaProcessParameters(item);
 				}
 				if (nodeName.equals("actions")) {
 					actions(item);
 				}
 			}
 		}
 	}
 	
 	
 
 	private void javaProcessParameters(Node item) {
 		Element jpElem = (Element) item;
 		String textContent = jpElem.getTextContent();
 		StringTokenizer tokenizer = new StringTokenizer(textContent, " ");
 		while(tokenizer.hasMoreTokens()) {
 			String param = tokenizer.nextToken();
 			if (!param.trim().equals("")) {
 				this.javaProcessParameters.add(param);
 			}
 		}
 	}
 
 	private void actions(Node item) {
 		Element elm = (Element) item;
 		NodeList nodes = elm.getChildNodes();
 		for (int i = 0,ll=nodes.getLength(); i < ll; i++) {
 			Node chItem = nodes.item(i);
 			if (chItem.getNodeType() == Node.ELEMENT_NODE) {
 				//String chItemName = chItem.getNodeName();
 				LRDefinitionAction action = new LRDefinitionAction();
 				action.loadFromXml((Element) chItem);
 				this.actions.add(action);
 			}
 		}		
 	}
 
 	private void oldStyleParameters(Node item) {
 		Element elm = (Element) item;
 		NodeList nodes = elm.getChildNodes();
 		for (int i = 0,ll=nodes.getLength(); i < ll; i++) {
 			Node chItem = nodes.item(i);
 			if (chItem.getNodeType() == Node.ELEMENT_NODE) {
 				String chItemName = chItem.getNodeName();
 				if (chItemName.equals("parameter")) {
 					String chItemVal = chItem.getTextContent();
 					this.parameters.add(chItemVal);
 				}
 			}
 		}		
 	}
 	
 	private void newStyleParameters(Node item) {
 		Element elm = (Element) item;
 		String textContent = elm.getTextContent();
 		StringTokenizer tokenizer = new StringTokenizer(textContent, " ");
 		while(tokenizer.hasMoreTokens()) {
 			String param = tokenizer.nextToken();
 			if (!param.trim().equals("")) {
 				this.parameters.add(param);
 			}
 		}
 		
 	}
 
 	@Override
 	public LRProcess createNewProcess(String token) {
 		LRProcess process = createProcessInternal();
 		process.setToken(token != null ? token : UUID.randomUUID().toString());
 		return process;
 	}
 
 	private AbstractLRProcessImpl createProcessInternal() {
 		if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_UNIX) {
 			return new cz.incad.kramerius.processes.os.impl.unix.UnixLRProcessImpl(this, this.pm, this.configuration);
 		} else if (SystemUtils.IS_OS_WINDOWS) {
 			return new WindowsLRProcessImpl(this, this.pm, configuration); 
 		} else throw new UnsupportedOperationException("unsupported OS");
 	}
 	
 	
 	@Override
 	public LRProcess loadProcess(String uuid, String pid, long planned, States state, String name) {
 	    AbstractLRProcessImpl abs = createProcessInternal();
 		abs.setUuid(uuid);
 		abs.setDefinition(this);
 		abs.setPlannedTime(planned);
 		abs.setPid(pid);
 		abs.setProcessState(state);
 		abs.setProcessName(name);
 		return abs;
 	}
 
 	
 	public String getId() {
 		return id;
 	}
 
 	@Override
 	public String getMainClass() {
 		return this.mainClass;
 	}
 
 	@Override
 	public String getDescription() {
 		return this.description;
 	}
 
 	public LRProcessManager getPm() {
 		return pm;
 	}
 
 	public void setPm(LRProcessManager pm) {
 		this.pm = pm;
 	}
 
 	public String getStandardStreamFolder() {
 		return standardStreamFolder;
 	}
 
 	public void setStandardStreamFolder(String standardStreamFolder) {
 		this.standardStreamFolder = standardStreamFolder;
 	}
 
 	public String getErrStreamFolder() {
 		return errStreamFolder;
 	}
 
 	public void setErrStreamFolder(String errStreamFolder) {
 		this.errStreamFolder = errStreamFolder;
 	}
 
 	@Override
 	public List<String> getJavaProcessParameters() {
 		return this.javaProcessParameters;
 	}
 
 //	@Override
 //	public String getProcessOutputURL() {
 //		return this.processOutputURL;
 //	}
 
 	
 	
 }
