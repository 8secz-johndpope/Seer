 package org.objectweb.proactive.examples.eratosthenes;
 
 /* 
 * ################################################################
 * 
 * ProActive: The Java(TM) library for Parallel, Distributed, 
 *            Concurrent computing with Security and Mobility
 * 
 * Copyright (C) 1997-2002 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive-support@inria.fr
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *  
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s): 
 * 
 * ################################################################
 *
 */
 
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 
 import javax.swing.JButton;
 import javax.swing.JFrame;
 import javax.swing.JToggleButton;
 
 import org.apache.log4j.Logger;
 import org.objectweb.proactive.Body;
 import org.objectweb.proactive.InitActive;
 import org.objectweb.proactive.ProActive;
 import org.objectweb.proactive.core.ProActiveException;
 import org.objectweb.proactive.core.config.ProActiveConfiguration;
 import org.objectweb.proactive.core.descriptor.data.ProActiveDescriptor;
 import org.objectweb.proactive.core.descriptor.data.VirtualNode;
 import org.objectweb.proactive.core.node.Node;
 import org.objectweb.proactive.core.node.NodeFactory;
 
 /**
  * @author Jonathan Streit
  * Main program for the Eratosthenes example. This class starts
  * an output listener, the first ActivePrimeContainer and a number source.
  * It also serves as creator of new ActivePrimeContainers.<br>
  * An XML descriptor file can be passed as first parameter, in this case,
  * the active objects are created in the nodes described by the virtual nodes
  * Containers, NumberSource and OutputListener. <br>
  * Main is not migratable due to the VirtualNode object.<br>
  * A control window allows to terminate the application and to pause
  * temporarily the NumberSource.
  * */
 public class Main implements ActivePrimeContainerCreator, InitActive {
 	
 	static Logger logger = Logger.getLogger(Main.class.getName());
   private PrimeOutputListener outputListener;
   private NumberSource source;
   private VirtualNode containersVirtualNode;
   private Node listenerNode;
   private Node sourceNode;
   private ProActiveDescriptor pad;
   private boolean gui;
   private Node lastNode;
   private int nodeCount;
   /** Place four ActivePrimeContainers in a Node before using the next for the distributed version. */
   private static final int ACTIVEPRIMECONTAINERS_PER_NODE = 4;
 
 
   /**
    * Constructor for Main.
    */
   public Main() {
   }
   
  public Main(String xmlDescriptor, Boolean gui) throws ProActiveException {
   	// read XML Descriptor
   	if (xmlDescriptor.length() > 0)
       pad = ProActive.getProactiveDescriptor(xmlDescriptor);   
    this.gui = gui.booleanValue();
   }
 
   /** Creates a new ActivePrimeContainer starting with number n */
   public ActivePrimeContainer newActivePrimeContainer(long n, Slowable previous) {
   	try {
   		int containerSize;
   		/* Create the new container with size = SQRT(n) * 20,
   		 * but at least 100 and at most 1000 */
   		containerSize = (int)Math.sqrt(n)*20;
   		if (containerSize < 100) containerSize = 100;
   		else if (containerSize > 1000) containerSize = 1000;  
 
 		// find correct node or use default node  		
   		Node node;
   		if (containersVirtualNode != null) { // alternate between nodes for creating containers
   			if (lastNode == null) {
   			  lastNode = containersVirtualNode.getNode();
   			  node = sourceNode;
   			  nodeCount = 0;
   			} else if (nodeCount < ACTIVEPRIMECONTAINERS_PER_NODE) {
   			  node = lastNode;
   			  nodeCount ++;	
   			} else {
   			  lastNode = node = containersVirtualNode.getNode();
   			  nodeCount = 1;
   			}
   		}
   		else node = NodeFactory.getDefaultNode();
   		
   		logger.info("    Creating container with size "+containerSize+" starting with number "+n);
     	ActivePrimeContainer result =
     	   (ActivePrimeContainer) ProActive.newActive(ActivePrimeContainer.class.getName(), 
   	        new Object [] { ProActive.getStubOnThis(), outputListener, 
   	        	new Integer(containerSize), new Long(n), previous }, node);
   	        	
         // Workaround for a little bug in ProActive (Exception in receiveRequest)
   	    // may be removed as the bug is fixed
   	    // This call makes us wait while the newly created object is not yet in his runActivity() method
   	    long v = result.getValue();
   	    
 	    return result;
   	} catch (ProActiveException e) {
   		e.printStackTrace();
   		return null;
   	}
   }
 
   public void initActivity(Body b) {
   	try {
       if (pad != null) {
       	// create nodes
         pad.activateMappings();
   	    containersVirtualNode = pad.getVirtualNode("Containers");   
 	    listenerNode = pad.getVirtualNode("OutputListener").getNode();	   
 	    sourceNode = pad.getVirtualNode("NumberSource").getNode();	   
       } else {
    		listenerNode = sourceNode = NodeFactory.getDefaultNode();
       }
       
       // create output listener
   	  outputListener = (PrimeOutputListener) ProActive.newActive(ConsolePrimeOutputListener.class.getName(),
   			new Object[] {}, listenerNode);
   	
   	  outputListener.newPrimeNumberFound(2);
   	
   		// create number source  
   	  source = (NumberSource) ProActive.newActive(NumberSource.class.getName(),
   			new Object[] { }, sourceNode);
 
 		// create first container  			
   	  ActivePrimeContainer first = newActivePrimeContainer(3, source);
   	  
   	  source.setFirst(first);
   	  
   	  if (gui) new ControlFrame(this);
   	  else source.pause(false); // start immediately if no gui
   			
   	} catch (Exception ex) {
   		ex.printStackTrace();
   	}
   }
 
   public void exit() {
     	if (containersVirtualNode != null) {
       		logger.info("Killing nodes...");
       		logger.info("This may print out some exception messages, but that's OK.");
       		java.util.Vector killedRTs = new java.util.Vector();
     		for (int i = -2; i < containersVirtualNode.getNodeCount(); i ++) {
     			try {
 	      		  Node node;
 	      		  if (i == -2) node = sourceNode;
 	      		  else if (i == -1) node = listenerNode;
 	      		  else node = containersVirtualNode.getNode(i);
       			  if (!NodeFactory.isNodeLocal(node) && 
       			       !killedRTs.contains(node.getProActiveRuntime().getURL())) {
       			  	killedRTs.add(node.getProActiveRuntime().getURL());
       			  	
       			  	// this method will catch and print out an exception,
       			  	// there is nothing we can do about it
       			  	node.getProActiveRuntime().killRT(false);
       			  }
     	 		} catch (Throwable ex) {
     			}
     		}
     		logger.info("Killed nodes.");
     	}
     	System.exit(0);
   }
 
   public NumberSource getSource() {
   	return source;
   }
 
   public static void main(String[] args) throws ProActiveException {  	
   	String xmlDescriptor = "";
   	boolean gui = true;
   	if (args.length > 0) {
   	  if (args[0].equalsIgnoreCase("-nogui")) {
   	  	gui = false;
   	  	if (args.length > 1) xmlDescriptor = args[1];
   	  } else xmlDescriptor = args[0];
   	}
 	ProActiveConfiguration.load();
   	Main main = (Main)ProActive.newActive(Main.class.getName(), 
   	  new Object[] {xmlDescriptor, new Boolean(gui)});
   }
 
   /** class for control window. */
   class ControlFrame extends JFrame implements ActionListener {
   	private JButton exitButton;
   	private JToggleButton pauseButton;
   	private Main main;
   	ControlFrame(Main m) {
   	  super("Eratosthenes control window");
   	  main = m;
   	  setSize(300, 80);
   	  getContentPane().setLayout(new java.awt.FlowLayout());
   	  pauseButton = new JToggleButton("Pause", true);
   	  exitButton = new JButton("Exit");
   	  pauseButton.addActionListener(this);
   	  exitButton.addActionListener(this);
   	  getContentPane().add(pauseButton);
   	  getContentPane().add(exitButton);
   	  show();
   	}
   	public void actionPerformed(ActionEvent e) {
   		if (e.getSource() == exitButton) {
   			main.exit();
   		} else if (e.getSource() == pauseButton) {
   			main.getSource().pause(pauseButton.isSelected());
   		}
   	}
   }
   
 }
