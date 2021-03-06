 /*
  * ################################################################
  *
  * ProActive: The Java(TM) library for Parallel, Distributed,
  *            Concurrent computing with Security and Mobility
  *
  * Copyright (C) 1997-2007 INRIA/University of Nice-Sophia Antipolis
  * Contact: proactive@objectweb.org
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version
  * 2 of the License, or any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this library; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
  * USA
  *
  *  Initial developer(s):               The ProActive Team
  *                        http://proactive.inria.fr/team_members.htm
  *  Contributor(s):
  *
  * ################################################################
  */
 package org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers;
 
 import javax.xml.xpath.XPath;
 
 import org.objectweb.proactive.extra.gcmdeployment.GCMParserHelper;
 import org.objectweb.proactive.extra.gcmdeployment.process.group.AbstractGroup;
 import org.objectweb.proactive.extra.gcmdeployment.process.group.GroupRSH;
 import org.objectweb.proactive.extra.gcmdeployment.process.group.GroupSSH;
 import org.w3c.dom.Node;
 
 
 public class GroupRSHParser extends AbstractGroupParser {
     private static final String ATTR_HOST_LIST = "hostList";
     static final String NODE_NAME = "rshGroup";
 
     @Override
     public AbstractGroup parseGroupNode(Node groupNode, XPath xpath) {
        GroupSSH groupSSH = (GroupSSH) super.parseGroupNode(groupNode, xpath);
 
         // Mandatory attributes
         String hostList = GCMParserHelper.getAttributeValue(groupNode, ATTR_HOST_LIST);
        groupSSH.setHostList(hostList);
 
        return groupSSH;
     }
 
     @Override
     public AbstractGroup createGroup() {
         return new GroupRSH();
     }
 
     @Override
     public String getNodeName() {
         return NODE_NAME;
     }
 }
