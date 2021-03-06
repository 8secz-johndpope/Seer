 /*
  * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.hazelcast.aws.utility;
 
 import com.hazelcast.config.AbstractXmlConfigHelper;
 import com.hazelcast.config.AwsConfig;
 import com.hazelcast.logging.ILogger;
 import com.hazelcast.logging.Logger;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.logging.Level;
 
 import static com.hazelcast.config.AbstractXmlConfigHelper.cleanNodeName;
 import static java.lang.String.format;
 
 public class CloudyUtility {
     final static ILogger logger = Logger.getLogger(CloudyUtility.class);
 
     public static String getQueryString(Map<String, String> attributes) {
         StringBuilder query = new StringBuilder();
         for (Iterator<String> iterator = attributes.keySet().iterator(); iterator.hasNext(); ) {
             String key = iterator.next();
             String value = attributes.get(key);
             query.append(AwsURLEncoder.urlEncode(key)).append("=").append(AwsURLEncoder.urlEncode(value)).append("&");
         }
         String result = query.toString();
         if (result != null && !result.equals(""))
             result = "?" + result.substring(0, result.length() - 1);
         return result;
     }
 
     public static Object unmarshalTheResponse(InputStream stream, AwsConfig awsConfig) throws IOException {
         Object o = parse(stream, awsConfig);
         return o;
     }
 
     private static Object parse(InputStream in, AwsConfig awsConfig) {
         final DocumentBuilder builder;
         try {
             builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
             Document doc = builder.parse(in);
             Element element = doc.getDocumentElement();
             NodeHolder elementNodeHolder = new NodeHolder(element);
             List<String> names = new ArrayList<String>();
             List<NodeHolder> reservationset = elementNodeHolder.getSubNodes("reservationset");
             for (NodeHolder reservation : reservationset) {
                 List<NodeHolder> items = reservation.getSubNodes("item");
                 for (NodeHolder item : items) {
                     NodeHolder instancesset = item.getSub("instancesset");
                     names.addAll(instancesset.getList("privateipaddress", awsConfig));
                 }
             }
             return names;
         } catch (Exception e) {
             logger.warning(e);
         }
         return new ArrayList<String>();
     }
 
     static class NodeHolder {
         Node node;
 
         public NodeHolder(Node node) {
             this.node = node;
         }
 
         public NodeHolder getSub(String name) {
             if (node != null) {
                 for (org.w3c.dom.Node node : new AbstractXmlConfigHelper.IterableNodeList(this.node.getChildNodes())) {
                     String nodeName = cleanNodeName(node.getNodeName());
                     if (name.equals(nodeName)) {
                         return new NodeHolder(node);
                     }
                 }
             }
             return new NodeHolder(null);
         }
 
         public List<NodeHolder> getSubNodes(String name) {
             List<NodeHolder> list = new ArrayList<NodeHolder>();
             if (node != null) {
                 for (org.w3c.dom.Node node : new AbstractXmlConfigHelper.IterableNodeList(this.node.getChildNodes())) {
                     String nodeName = cleanNodeName(node.getNodeName());
                     if (name.equals(nodeName)) {
                         list.add(new NodeHolder(node));
                     }
                 }
             }
             return list;
         }
 
         public List<String> getList(String name, AwsConfig awsConfig) {
             List<String> list = new ArrayList<String>();
             if (node == null) return list;
 
             for (org.w3c.dom.Node node : new AbstractXmlConfigHelper.IterableNodeList(this.node.getChildNodes())) {
                 String nodeName = cleanNodeName(node.getNodeName());
                 if (!"item".equals(nodeName)) continue;
 
                 final NodeHolder nodeHolder = new NodeHolder(node);
                 final String state = getState(nodeHolder);
                 final String ip = getIp(name, nodeHolder);
                 final String instanceName = getInstanceName(nodeHolder);
 
                  if (ip != null) {
                    if (!acceptState(state)) {
                        logger.finest(format("Ignoring EC2 instance [%s][%s] reason: the instance is not running but %s", instanceName, ip, state));
                     } else if (!acceptTag(awsConfig, node)) {
                        logger.finest(format("Ignoring EC2 instance [%s][%s] reason: tag-key/tag-value don't match", instanceName, ip));
                     } else if (!acceptGroupName(awsConfig, node)) {
                        logger.finest(format("Ignoring EC2 instance [%s][%s] reason: security-group-name doesn't match", instanceName, ip));
                     } else {
                         list.add(ip);
                         logger.finest(format("Accepting EC2 instance [%s][%s]",instanceName, ip));
                     }
                 }
 
             }
             return list;
         }
 
        private boolean acceptState(String state) {
            return "running".equals(state);
        }

         private static String getState(NodeHolder nodeHolder) {
             final NodeHolder instancestate = nodeHolder.getSub("instancestate");
             return instancestate.getSub("name").getNode().getFirstChild().getNodeValue();
         }
 
         private static String getInstanceName(NodeHolder nodeHolder) {
             final NodeHolder tagSetNode = nodeHolder.getSub("tagset");
             if (tagSetNode.getNode() == null) {
                 return null;
             }
 
             final NodeList childNodes = tagSetNode.getNode().getChildNodes();
             for (int k = 0; k < childNodes.getLength(); k++) {
                 Node item = childNodes.item(k);
                 if (!item.getNodeName().equals("item")) continue;
 
                 NodeHolder itemHolder = new NodeHolder(item);
 
                 final Node keyNode = itemHolder.getSub("key").getNode();
                 if (keyNode == null || keyNode.getFirstChild() == null) continue;
                 final String nodeValue = keyNode.getFirstChild().getNodeValue();
                 if (!"Name".equals(nodeValue)) continue;
 
                 final Node valueNode = itemHolder.getSub("value").getNode();
                 if (valueNode == null || valueNode.getFirstChild() == null) continue;
                 return valueNode.getFirstChild().getNodeValue();
             }
             return null;
         }
 
         private static String getIp(String name, NodeHolder nodeHolder) {
             final Node node1 = nodeHolder.getSub(name).getNode();
             return node1 == null ? null : node1.getFirstChild().getNodeValue();
         }
 
         private boolean acceptTag(AwsConfig awsConfig, Node node) {
             return applyTagFilter(node, awsConfig.getTagKey(), awsConfig.getTagValue());
         }
 
         private boolean acceptGroupName(AwsConfig awsConfig, Node node) {
             return applyFilter(node, awsConfig.getSecurityGroupName(), "groupset", "groupname");
         }
 
         private boolean applyFilter(Node node, String filter, String set, String filterField) {
             if (nullOrEmpty(filter)) {
                 return true;
             } else {
                 for (NodeHolder group : new NodeHolder(node).getSub(set).getSubNodes("item")) {
                     NodeHolder nh = group.getSub(filterField);
                     if (nh != null && nh.getNode().getFirstChild() != null && filter.equals(nh.getNode().getFirstChild().getNodeValue())) {
                         return true;
                     }
                 }
                 return false;
             }
         }
 
         private boolean applyTagFilter(Node node, String keyExpected, String valueExpected) {
             if (nullOrEmpty(keyExpected)) {
                 return true;
             } else {
                 for (NodeHolder group : new NodeHolder(node).getSub("tagset").getSubNodes("item")) {
                     if (keyEquals(keyExpected, group) &&
                             (nullOrEmpty(valueExpected) || valueEquals(valueExpected, group))) {
                         return true;
                     }
                 }
                 return false;
             }
         }
 
         private boolean valueEquals(String valueExpected, NodeHolder group) {
             NodeHolder nhValue = group.getSub("value");
             return nhValue != null && nhValue.getNode().getFirstChild() != null && valueExpected.equals(nhValue.getNode().getFirstChild().getNodeValue());
         }
 
         private boolean nullOrEmpty(String keyExpected) {
             return keyExpected == null || keyExpected.equals("");
         }
 
         private boolean keyEquals(String keyExpected, NodeHolder group) {
             NodeHolder nhKey = group.getSub("key");
             return nhKey != null && nhKey.getNode().getFirstChild() != null && keyExpected.equals(nhKey.getNode().getFirstChild().getNodeValue());
         }
 
         public Node getNode() {
             return node;
         }
     }
 }
