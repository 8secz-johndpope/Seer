 package net.i2cat.mantychore.ipcapability.test;
 
 import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
 import static org.ops4j.pax.exam.CoreOptions.options;
 import static org.ops4j.pax.exam.CoreOptions.systemProperty;
 import static org.ops4j.pax.exam.OptionUtils.combine;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import net.i2cat.mantychore.actionsets.junos.ActionConstants;
 import net.i2cat.mantychore.capability.ip.IPCapability;
 import net.i2cat.mantychore.chassiscapability.test.MockResource;
 import net.i2cat.mantychore.model.ComputerSystem;
 import net.i2cat.mantychore.model.EthernetPort;
 import net.i2cat.mantychore.model.IPProtocolEndpoint;
 import net.i2cat.mantychore.model.NetworkPort;
 import net.i2cat.mantychore.queuemanager.QueueManager;
import net.i2cat.nexus.resources.queue.QueueConstants;
 import net.i2cat.nexus.resources.action.ActionResponse;
 import net.i2cat.nexus.resources.action.IAction;
 import net.i2cat.nexus.resources.capability.CapabilityException;
 import net.i2cat.nexus.resources.capability.ICapability;
 import net.i2cat.nexus.resources.capability.ICapabilityFactory;
 import net.i2cat.nexus.resources.command.Response;
 import net.i2cat.nexus.resources.command.Response.Status;
 import net.i2cat.nexus.resources.descriptor.CapabilityDescriptor;
 import net.i2cat.nexus.resources.descriptor.ResourceDescriptor;
 import net.i2cat.nexus.resources.descriptor.ResourceDescriptorConstants;
 import net.i2cat.nexus.resources.protocol.IProtocolManager;
 import net.i2cat.nexus.resources.protocol.ProtocolSessionContext;
 import net.i2cat.nexus.tests.IntegrationTestsHelper;
 
 import org.apache.commons.lang.exception.ExceptionUtils;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.karaf.testing.AbstractIntegrationTest;
 import org.junit.Assert;
 import org.junit.Before;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.ops4j.pax.exam.Inject;
 import org.ops4j.pax.exam.Option;
 import org.ops4j.pax.exam.junit.Configuration;
 import org.ops4j.pax.exam.junit.JUnit4TestRunner;
 import org.osgi.framework.BundleContext;
 
 @RunWith(JUnit4TestRunner.class)
 public class IPCapabilityIntegrationTest extends AbstractIntegrationTest {
 	static Log			log				= LogFactory
 												.getLog(IPCapabilityIntegrationTest.class);
 	static MockResource	mockResource;
 	String				deviceID		= "junos";
 	String				queueID			= "queue";
 
 	static ICapability	ipCapability;
 	@Inject
 	BundleContext		bundleContext	= null;
 	private ICapability	queueCapability;
 
 	@Configuration
 	public static Option[] configure() {
 
 		Option[] options = combine(
 				IntegrationTestsHelper.getMantychoreTestOptions(),
 				mavenBundle().groupId("net.i2cat.nexus").artifactId(
 						"net.i2cat.nexus.tests.helper")
 					// , vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")
 					);
 		// TODO IS IT EXIT A BETTER METHOD TO PASS THE URI
 		String uri = System.getProperty("protocol.uri");
 		if (uri != null && !uri.equals("${protocol.uri}")) {
 			Option[] optionsWithURI = options(systemProperty("protocol.uri").value(uri));
 			options = combine(options, optionsWithURI);
 		}
 		return options;
 	}
 
 	public void initResource() {
 		log.info("This is running inside Equinox. With all configuration set up like you specified. ");
 
 		/* initialize model */
 		mockResource = new MockResource();
 		mockResource.setModel(new ComputerSystem());
 
 		ResourceDescriptor resourceDescriptor = new ResourceDescriptor();
 
 		Map<String, String> properties = new HashMap<String, String>();
 		properties.put(ResourceDescriptorConstants.PROTOCOL_URI,
 				"user:pass@host.net:2212");
 		List<CapabilityDescriptor> capabilityDescriptors = new ArrayList<CapabilityDescriptor>();
 
 		/* chassis descriptor */
 		capabilityDescriptors.add(MockResource.createCapabilityDescriptor(
 				IPCapability.IP, "ip"));
 
 		/* queue descriptor */
 		capabilityDescriptors.add(MockResource.createCapabilityDescriptor(
 				QueueManager.QUEUE, "queue"));
 
 		resourceDescriptor.setProperties(properties);
 		resourceDescriptor.setCapabilityDescriptors(capabilityDescriptors);
 		resourceDescriptor.setId(deviceID);
 
 		mockResource.setResourceDescriptor(resourceDescriptor);
 	}
 
 	/**
 	 * Configure the protocol to connect
 	 */
 	private ProtocolSessionContext newSessionContextNetconf() {
 		String uri = System.getProperty("protocol.uri");
 		if (uri == null || uri.equals("${protocol.uri}")) {
 			uri = "mock://user:pass@host.net:2212/mocksubsystem";
 		}
 
 		ProtocolSessionContext protocolSessionContext = new ProtocolSessionContext();
 
 		protocolSessionContext.addParameter(
 				ProtocolSessionContext.PROTOCOL_URI, uri);
 		protocolSessionContext.addParameter(ProtocolSessionContext.PROTOCOL,
 				"netconf");
 		// ADDED
 		return protocolSessionContext;
 
 	}
 
 	public void initCapability() {
 
 		try {
 			log.info("INFO: Before test, getting queue...");
 			ICapabilityFactory queueManagerFactory = getOsgiService(ICapabilityFactory.class, "capability=queue", 5000);
 			Assert.assertNotNull(queueManagerFactory);
 
 			queueCapability = queueManagerFactory.create(mockResource);
 
 			// IQueueManagerService queueManagerService = (IQueueManagerService) getOsgiService(IQueueManagerService.class,
 			// "(capability=queue)(capability.name=" + deviceID + ")", 5000);
 
 			IProtocolManager protocolManager = getOsgiService(IProtocolManager.class, 5000);
 			protocolManager.getProtocolSessionManagerWithContext(mockResource.getResourceId(), newSessionContextNetconf());
 
 			ICapabilityFactory ipFactory = getOsgiService(ICapabilityFactory.class, "capability=ip", 10000);
 			// Test elements not null
 			log.info("Checking ip factory");
 			Assert.assertNotNull(ipFactory);
 			log.info("Checking capability descriptor");
 			Assert.assertNotNull(mockResource.getResourceDescriptor().getCapabilityDescriptor("ip"));
 			log.info("Creating ip capability");
 			ipCapability = ipFactory.create(mockResource);
 			Assert.assertNotNull(ipCapability);
 			ipCapability.initialize();
 
 		} catch (Exception e) {
 			e.printStackTrace();
 			log.error(e.getMessage());
 			log.error(ExceptionUtils.getRootCause(e).getMessage());
 			Assert.fail();
 		}
 	}
 
 	@Before
 	public void initBundles() {
 		log.info("Waiting to load all bundles");
 		/* Wait for the activation of all the bundles */
 		IntegrationTestsHelper.waitForAllBundlesActive(bundleContext);
 		log.info("Loaded all bundles");
 		initResource();
 		initCapability();
 	}
 
 	@Test
 	public void TestIPAction() {
 		log.info("TEST ip ACTION");
 		List<String> availabledActions = new ArrayList<String>();
 		availabledActions.add(ActionConstants.GETCONFIG);
 		availabledActions.add(ActionConstants.SETIPv4);
 		try {
 			Response resp = (Response) ipCapability.sendMessage(ActionConstants.GETCONFIG, null);
 			Assert.assertTrue(resp.getStatus() == Status.OK);
 			Assert.assertTrue(resp.getErrors().size() == 0);
			List<IAction> queue = (List<IAction>) queueCapability.sendMessage(QueueConstants.GETQUEUE, null);
 			Assert.assertTrue(queue.size() == 1);
			List<ActionResponse> responses = (List<ActionResponse>) queueCapability.sendMessage(QueueConstants.EXECUTE, null);
 
 			Assert.assertTrue(responses.size() == 2);
 			ActionResponse actionResponse = responses.get(0);
 			Assert.assertEquals(ActionConstants.GETCONFIG, actionResponse.getActionID());
 			for (Response response : actionResponse.getResponses()) {
 				Assert.assertTrue(response.getStatus() == Response.Status.OK);
 			}
 
			queue = (List<IAction>) queueCapability.sendMessage(QueueConstants.GETQUEUE, null);
 			Assert.assertTrue(queue.size() == 0);
 
 		} catch (CapabilityException e) {
 			e.printStackTrace();
 			Assert.fail();
 		}
 
 	}
 
 	public Object newParamsInterfaceEthernet(String name, String ipName, String mask) {
 		EthernetPort eth = new EthernetPort();
 		eth.setLinkTechnology(NetworkPort.LinkTechnology.ETHERNET);
 		eth.setElementName(name);
 		IPProtocolEndpoint ip = new IPProtocolEndpoint();
 		ip.setIPv4Address(ipName);
 		ip.setSubnetMask(mask);
 		eth.addProtocolEndpoint(ip);
 		return eth;
 	}
 }
