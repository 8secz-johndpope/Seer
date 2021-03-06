 package org.opennms.netmgt.ncs.persistence;
 
 import static org.junit.Assert.*;
 
 import java.util.List;
 
 import org.apache.log4j.BasicConfigurator;
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.opennms.netmgt.dao.DistPollerDao;
 import org.opennms.netmgt.dao.NodeDao;
 import org.opennms.netmgt.model.NetworkBuilder;
 import org.opennms.netmgt.model.OnmsDistPoller;
import org.opennms.netmgt.model.OnmsNode;
 import org.opennms.netmgt.model.ncs.NCSBuilder;
 import org.opennms.netmgt.model.ncs.NCSComponent;
 import org.opennms.netmgt.model.ncs.NCSComponent.DependencyRequirements;
 import org.opennms.netmgt.model.ncs.NCSComponentRepository;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.test.context.ContextConfiguration;
 import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
 import org.springframework.transaction.annotation.Transactional;
 
 @RunWith(SpringJUnit4ClassRunner.class)
 @ContextConfiguration(locations={
 		"classpath:META-INF/opennms/applicationContext-datasource.xml",
 		"classpath:META-INF/opennms/applicationContext-testDao.xml",
		"classpath*:META-INF/opennms/component-dao.xml",
 })
 @Transactional
 public class NCSComponentDaoTest {
 	
 	@Autowired
 	NCSComponentRepository m_repository;
 	
 	@Autowired
 	DistPollerDao m_distPollerDao;
 	
 	@Autowired
 	NodeDao m_nodeDao;
 	
 	int m_pe1NodeId;
 	
 	int m_pe2NodeId;
 	
 	@BeforeClass
 	public static void setupLogging()
 	{
 		BasicConfigurator.configure();		
 	}
 	
 	
 	@Before
 	public void setUp() {
 		
 		OnmsDistPoller distPoller = new OnmsDistPoller("localhost", "127.0.0.1");
 		
 		m_distPollerDao.save(distPoller);
 		
 		
 		NetworkBuilder bldr = new NetworkBuilder(distPoller);
 		bldr.addNode("PE1").setForeignSource("space").setForeignId("1111-PE1");
 		
 		m_nodeDao.save(bldr.getCurrentNode());
 		
 		m_pe1NodeId = bldr.getCurrentNode().getId();
 		
 		bldr.addNode("PE2").setForeignSource("space").setForeignId("2222-PE2");
 		
 		m_nodeDao.save(bldr.getCurrentNode());
 		
 		m_pe2NodeId = bldr.getCurrentNode().getId();
 		
 		NCSComponent svc = new NCSBuilder("Service", "NA-Service", "123")
 		.setName("CokeP2P")
 		.pushComponent("ServiceElement", "NA-ServiceElement", "8765")
 			.setName("PE1:SE1")
 			.setNodeIdentity("space", "1111-PE1")
 			.pushComponent("ServiceElementComponent", "NA-SvcElemComp", "8765:jnxVpnIf")
 				.setName("jnxVpnIf")
 				.setUpEventUei("uei.opennms.org/vendor/Juniper/traps/jnxVpnIfUp")
 				.setDownEventUei("uei.opennms.org/vendor/Juniper/traps/jnxVpnIfDown")
 				.setAttribute("jnxVpnIfType", "5")
 				.setAttribute("jnxVpnIfName", "ge-1/0/2.50")
 				.pushComponent("ServiceElementComponent", "NA-SvcElemComp", "8765:link")
 					.setName("link")
 					.setUpEventUei("uei.opennms.org/vendor/Juniper/traps/linkUp")
 					.setDownEventUei("uei.opennms.org/vendor/Juniper/traps/linkDown")
 					.setAttribute("linkName", "ge-1/0/2")
 				.popComponent()
 			.popComponent()
 			.pushComponent("ServiceElementComponent", "NA-SvcElemComp", "8765:jnxVpnPw-vcid(50)")
 				.setName("jnxVpnPw-vcid(50)")
 				.setUpEventUei("uei.opennms.org/vendor/Juniper/traps/jnxVpnPwUp")
 				.setDownEventUei("uei.opennms.org/vendor/Juniper/traps/jnxVpnPwDown")
 				.setAttribute("jnxVpnPwType", "5")
 				.setAttribute("jnxVpnPwName", "ge-1/0/2.50")
 				.setDependenciesRequired(DependencyRequirements.ANY)
 				.pushComponent("ServiceElementComponent", "NA-SvcElemComp", "8765:lspA-PE1-PE2")
 					.setName("lspA-PE1-PE2")
 					.setUpEventUei("uei.opennms.org/vendor/Juniper/traps/mplsLspPathUp")
 					.setDownEventUei("uei.opennms.org/vendor/Juniper/traps/mplsLspPathDown")
 					.setAttribute("mplsLspName", "lspA-PE1-PE2")
 				.popComponent()
 				.pushComponent("ServiceElementComponent", "NA-SvcElemComp", "8765:lspB-PE1-PE2")
 					.setName("lspB-PE1-PE2")
 					.setUpEventUei("uei.opennms.org/vendor/Juniper/traps/mplsLspPathUp")
 					.setDownEventUei("uei.opennms.org/vendor/Juniper/traps/mplsLspPathDown")
 					.setAttribute("mplsLspName", "lspB-PE1-PE2")
 				.popComponent()
 			.popComponent()
 		.popComponent()
 		.pushComponent("ServiceElement", "NA-ServiceElement", "9876")
 			.setName("PE2:SE1")
 			.setNodeIdentity("space", "2222-PE2")
 			.pushComponent("ServiceElementComponent", "NA-SvcElemComp", "9876:jnxVpnIf")
 				.setName("jnxVpnIf")
 				.setUpEventUei("uei.opennms.org/vendor/Juniper/traps/jnxVpnIfUp")
 				.setDownEventUei("uei.opennms.org/vendor/Juniper/traps/jnxVpnIfDown")
 				.setAttribute("jnxVpnIfType", "5")
 				.setAttribute("jnxVpnIfName", "ge-3/1/4.50")
 				.pushComponent("ServiceElementComponent", "NA-SvcElemComp", "9876:link")
 					.setName("link")
 					.setUpEventUei("uei.opennms.org/vendor/Juniper/traps/linkUp")
 					.setDownEventUei("uei.opennms.org/vendor/Juniper/traps/linkDown")
 					.setAttribute("linkName", "ge-3/1/4")
 				.popComponent()
 			.popComponent()
 			.pushComponent("ServiceElementComponent", "NA-SvcElemComp", "9876:jnxVpnPw-vcid(50)")
 				.setName("jnxVpnPw-vcid(50)")
 				.setUpEventUei("uei.opennms.org/vendor/Juniper/traps/jnxVpnPwUp")
 				.setDownEventUei("uei.opennms.org/vendor/Juniper/traps/jnxVpnPwDown")
 				.setAttribute("jnxVpnPwType", "5")
 				.setAttribute("jnxVpnPwName", "ge-3/1/4.50")
 				.setDependenciesRequired(DependencyRequirements.ANY)
 				.pushComponent("ServiceElementComponent", "NA-SvcElemComp", "9876:lspA-PE2-PE1")
 					.setName("lspA-PE2-PE1")
 					.setUpEventUei("uei.opennms.org/vendor/Juniper/traps/mplsLspPathUp")
 					.setDownEventUei("uei.opennms.org/vendor/Juniper/traps/mplsLspPathDown")
 					.setAttribute("mplsLspName", "lspA-PE2-PE1")
 				.popComponent()
 				.pushComponent("ServiceElementComponent", "NA-SvcElemComp", "9876:lspB-PE2-PE1")
 					.setName("lspB-PE2-PE1")
 					.setUpEventUei("uei.opennms.org/vendor/Juniper/traps/mplsLspPathUp")
 					.setDownEventUei("uei.opennms.org/vendor/Juniper/traps/mplsLspPathDown")
 					.setAttribute("mplsLspName", "lspB-PE2-PE1")
 				.popComponent()
 			.popComponent()
 		.popComponent()
 		.get();
 		
 		m_repository.save(svc);
 		
 		
 	}
 
 	@Test
 	public void testFindComponentsByNodeId() {
 		
 		assertNotNull(m_repository);
 		
 		assertFalse(0 == m_pe1NodeId);
 		assertFalse(0 == m_pe2NodeId);
 		
 		List<NCSComponent> pe1Components = m_repository.findComponentsByNodeId(m_pe1NodeId);
 		
 		assertFalse(pe1Components.isEmpty());
 		
 		NCSComponent pe1SvcElem = m_repository.findByTypeAndForeignIdentity("ServiceElement", "NA-ServiceElement", "8765");
 		
 		assertNotNull(pe1SvcElem);
 		
 		assertTrue(pe1Components.contains(pe1SvcElem));
 
 		List<NCSComponent> pe2Components = m_repository.findComponentsByNodeId(m_pe2NodeId);
 		
 		assertFalse(pe2Components.isEmpty());
 		
 	}
 
 }
