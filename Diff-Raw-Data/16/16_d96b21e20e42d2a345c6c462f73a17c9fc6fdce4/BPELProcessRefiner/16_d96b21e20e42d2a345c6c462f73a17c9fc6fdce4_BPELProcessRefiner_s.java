 package org.oryxeditor.server;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.logging.Logger;
 
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 
 /**
  * Copyright (c) 2008-2009 
  * 
  * Zhen Peng
  * 
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  * 
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  * 
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 public class BPELProcessRefiner {
 	
 	
 	private Logger logger = Logger.getLogger(this.getClass().getName());
 	
 	private HashMap<String, Object[]> activityNodesMap = new HashMap<String, Object[]>();
 	
 	private HashMap<String, Object[]> linksMap = new HashMap<String, Object[]>();
 	
 	private ArrayList<String> linkList = new ArrayList<String>();
 	
 	public Document rearrangeDocument (Document document){
 	   
 		// use a hash map to record all activity node informations in this 
 		// document, these informations can simplify the rearrange process 
 		//
 		// in this hash map:
 		// key : node ID - type String
 		// value : {node address, depth of node, closest "flow" parent node}  
 		//          - type Object
 		// 
 		// the third item : [closest "flow" parent node] will only be used to
 		// find out the closest shared flow parent of two nodes - source node
 		// and target node of a link in order to locate the correct position 
 		// of this <link> element.
 		// attention: for a "flow" element, this item should be also parent
 		// node, but NOT itself
 		//
 		activityNodesMap = new HashMap<String, Object[]>();
 		
 		
 		// use a hash map to record all link node informations, so that we can
 		// easily get all necessary informations about a link
 		//
 		// in this hash map:
 		// key : node ID - type String
 		// value : {sourceNode ID,targetNode ID, linkName, 
 		// 			transitionCondition element}  - type Object
 		linksMap = new HashMap<String, Object[]>();
 		
 		// record all link IDs in a list, in the end we can immediately find out
 		// how many links do we have and what their IDs are
 		linkList = new ArrayList<String>();
 		
 		// visit this document from top to bottom, record activity nodes and link
 		// element informations
 		//
 		// especially for root element  
 		// depth of node : 0
 		// closest "flow" parent node : null
 		recordNodeInformation(document, 0, null);
 		
 		// visit this document again, rearrange it and at the same time find all 
 		// source nodes of link, then add them as source nodes into the linksMap. 
 		handleNode (document);
 	   
 		// visit all links in linkList, with help of linksMap append standardElement
 		// <sources> & <targets> under corresponding source node and target node,
 		// then build for each item a new <link> element and append it to the 
 		// correct flow parent.
 		buildLinkElements();
 		
 		// now the <link> elements are already in correct flow parent, so we
 		// can rearrange the child nodes of flow right now
 		// TODO : check this method
 		// rearrangeFlow(document);
 		
 		// visit all nodes for the last time, delete all useless attributes - 
 		// "bounds" "id" and "visited", and useless elements - "outgoingLink" 
 		// and "linkInfoSet" from nodes
 		cleanUp(document);
 		
 		return document;
 	}
 
 
 	private void cleanUp(Node currentNode) {
 		
 		if (!(currentNode instanceof Element || currentNode instanceof Document)) {
 			return;
 		};
 		
 		if (currentNode instanceof Element){
 			((Element)currentNode).removeAttribute("bounds");
 			((Element)currentNode).removeAttribute("id");
 			((Element)currentNode).removeAttribute("visited");	
 		}
 		
 		NodeList childNodes = currentNode.getChildNodes();
 		Node child;
 		for (int i=0; i<childNodes.getLength(); i++){
 			child = childNodes.item(i);
 			if (child instanceof Element){
 				if (child.getNodeName().equals("outgoingLink")
 						|| child.getNodeName().equals("linkInfoSet")){
					currentNode.removeChild(child);
 				} else {
 					cleanUp(child);
 				}
 			}
 		}	
 	}
 
 	/*********************** record node informations *****************/
 	
 	private void recordNodeInformation(Node currentNode, int depth, 
 			Element closestFlow) {
 		
 		if (!(currentNode instanceof Element 
 				|| currentNode instanceof Document)) {
 			return;
 		};
 		
 		// record activity node informations
 		if (isActivity(currentNode)){
 			String id = ((Element)currentNode).getAttribute("id");			
 			activityNodesMap.put(id, 
 					new Object[]{currentNode, depth, closestFlow});
 		};
 		
 		// record link element informations
 		if (currentNode.getNodeName().equals("linkInfoSet")){
 			Element link = (Element) currentNode;
 			String id = link.getAttribute("id");
 			String linkName = link.getAttribute("linkName");
 			String targetID = link.getAttribute("targetID");
 			String transCond_expLang = link.getAttribute("transCond_expLang");
 			String transCond_boolExp = link.getAttribute("transCond_boolExp");
 			String transCond_opaque = link.getAttribute("transCond_opaque");
 			
 			Element transitionCondition = currentNode.getOwnerDocument()
 							.createElement("transitionCondition");
 			transitionCondition.setAttribute("expressionLanguage", transCond_expLang);
 			transitionCondition.setNodeValue(transCond_boolExp);
 			if (transCond_opaque.equals("yes")){
 				transitionCondition.setAttribute("opaque", "yes");
 			}
 			
 			// right now we don't know the source node id yet, so we set the first item "null"
 			linksMap.put(id, new Object[]{null, targetID, linkName, transitionCondition});
 			
 			// record this link in linkList
 			linkList.add(id);
 		}
 		
 		Element newClosestFlow;
 		if (currentNode.getNodeName().equals("flow")){
 			newClosestFlow = (Element) currentNode;
 		} else {
 			newClosestFlow = closestFlow;
 		}
 		
 		// recursive research, visit all nodes
 		NodeList childNodes = currentNode.getChildNodes();
 		Node child;
 		for (int i=0; i<childNodes.getLength(); i++){
 			child = childNodes.item(i);
 			if (child instanceof Element){
 				recordNodeInformation(child, depth + 1, newClosestFlow);
 			}	
 		}		
 	}
 	
 	
 	private boolean isActivity(Node currentNode) {
 		if (currentNode.getNodeName().equals("receive")
 				|| currentNode.getNodeName().equals("reply")
 				|| currentNode.getNodeName().equals("invoke")
 				|| currentNode.getNodeName().equals("assign")
 				|| currentNode.getNodeName().equals("throw")
 				|| currentNode.getNodeName().equals("exit")
 				|| currentNode.getNodeName().equals("wait")
 				|| currentNode.getNodeName().equals("empty")
 				|| currentNode.getNodeName().equals("sequence")
 				|| currentNode.getNodeName().equals("if")
 				|| currentNode.getNodeName().equals("while")
 				|| currentNode.getNodeName().equals("repeatUntil")
 				|| currentNode.getNodeName().equals("forEach")
 				|| currentNode.getNodeName().equals("pick")
 				|| currentNode.getNodeName().equals("flow")
 				|| currentNode.getNodeName().equals("scope")
 				|| currentNode.getNodeName().equals("compensate")
 				|| currentNode.getNodeName().equals("compensateScope")
 				|| currentNode.getNodeName().equals("rethrow")
 				|| currentNode.getNodeName().equals("validate")
 				|| currentNode.getNodeName().equals("extensionActivity")){
 			
 			return true;
 		}
 		return false;
 	}
 
 	/******************************** rearrange Node *************************************/
     private void handleNode (Node currentNode){
 	      
 		// handle only the Nodes with type "Element" and "Document" (root element), 
 		// the other types e.g. "Attr","Comment" will be ignored 
 		if (!(currentNode instanceof Element || currentNode instanceof Document)){
 			return;
 		};
 	   
 		// recursive research, work on the child nodes at first.
 		NodeList childNodes = currentNode.getChildNodes();
 		Node child;
 		for (int i=0; i<childNodes.getLength(); i++){
 			child = childNodes.item(i);
 			if (child instanceof Element){
 				handleNode (child);
 			}
 		};
 	   
 		// after all of the child nodes are already handled, handle the current
 		// node
 		if (currentNode instanceof Element){
 			//the following elements could contain more than one child nodes,
 			//rearrange their child nodes.
 			if (currentNode.getNodeName().equals("process")
 					||currentNode.getNodeName().equals("invoke")
 					||currentNode.getNodeName().equals("scope")
 					||currentNode.getNodeName().equals("assign")
 					||currentNode.getNodeName().equals("eventHandlers")
 					||currentNode.getNodeName().equals("faultHandlers")
 					||currentNode.getNodeName().equals("compensationHandler")
 					||currentNode.getNodeName().equals("terminationHandler")
 					||currentNode.getNodeName().equals("if")
 					||currentNode.getNodeName().equals("sequence")
 					||currentNode.getNodeName().equals("pick") ){
 				  
 				rearrangeChildNodesOfCurrentNode((Element)currentNode);
 			};
 			   
 			// the order of child nodes in "flow" is not based on the position
 			// of child nodes, but the order of the edges "link", so we handle
 			// it in a different way.
 			if (currentNode.getNodeName().equals("flow")){
 				handleFlowElement((Element)currentNode);
 			}
 		   
 			// remove the head "elseIf" of the first child in if-block
 			if (currentNode.getNodeName().equals("if")){
 				handleIfElement ((Element)currentNode);
 			}
 		}
 	   
 		// after the arrangement of current node finished, visit all child nodes
 		// once again, find out all source nodes of links, such nodes are already
 		// marked with Element "outgoingLink". if "outgoingLink" element exists,
 		// record this node in linksMap.
 		childNodes = currentNode.getChildNodes();
 		for (int i=0; i<childNodes.getLength(); i++){
 			child = childNodes.item(i);
 			if (child.getNodeName().equals("outgoingLink")){
 				String linkID = ((Element) child).getAttribute("linkID");
 				String sourceID = ((Element)currentNode).getAttribute("id");
 				recordSourceNodeInlinksMap (linkID, sourceID);
 			}
 			
 		};
     }
   
     private void recordSourceNodeInlinksMap(String linkID, String sourceID) {
 		String targetID = getTargetID(linkID);
 		String linkName = getLinkName(linkID);
 		Element transitionCondition = getTransitionCondition(linkID);
 		
 		linksMap.remove(linkID);
 		
 		linksMap.put(linkID, new Object[]{sourceID, targetID, linkName, transitionCondition});
 	}  
 
     private void handleIfElement(Element currentNode) {
     	// remove the head "elseIf" of the first child in if-block
     	// find out the first "elseIf" element
     	Element firstElseifChild = getChildElementWithNodeName (currentNode, "elseIf", false);
   
 		// replace the first "elseIf" with two inner elements "condition" and "activity" 
 		if (firstElseifChild != null){
 			Node child = null;
 			Node condition = null;
 			Node activity = null;
 			NodeList childrenOfFirstChild = firstElseifChild.getChildNodes();
 			for (int i=0; i < childrenOfFirstChild.getLength(); i++){
 				child = childrenOfFirstChild.item(i);
 				if (child instanceof Element){
 					if (child.getNodeName().equals("condition")){
 						condition = child;
 					} else {
 						activity = child;
 					}
 				};
 			};
 	   
 			Node nextSibling = firstElseifChild.getNextSibling();
 			currentNode.removeChild(firstElseifChild);
 		
 			if (condition != null){
 				currentNode.insertBefore(condition, nextSibling);
 			};
 		
 			if (activity != null){
 				currentNode.insertBefore(activity, nextSibling);
 			}
 		}
 		
 	}
 
     /***************************  rearrange child nodes  *****************************/
 	
     private void rearrangeChildNodesOfCurrentNode(Element currentNode){
    
 	   // get all the children with be attribute "bounds"
 	   // record them in a array list "toArrangeChildren"
 	   ArrayList<Element> toArrangeChildren = new ArrayList<Element>();
 	   NodeList allChildren = currentNode.getChildNodes();
 	   Node child;
 	   for (int i=0; i<allChildren.getLength(); i++){
 		   child = allChildren.item(i);
 		   if (child instanceof Element){
 			  if (((Element) child).getAttribute("bounds") != ""){
 				   toArrangeChildren.add((Element) child);
 			   }
 		   }
 	   };
        
 	   // with a quick-sort method rearrange the children of currentNode;
 	   // copy the list first
 	   ArrayList<Element> arrangedChildren = new ArrayList<Element>();
 	   Iterator<Element> toArrangeChildrenIter = toArrangeChildren.iterator();
 	   Element thisElement;
 	   while (toArrangeChildrenIter.hasNext()){
 		   thisElement = toArrangeChildrenIter.next();
 		   arrangedChildren.add(thisElement);
 	   };
 	   quickSortForArrangingChildren(arrangedChildren, 0, arrangedChildren.size()-1);
 	   
 	   // delete old children
 	   Element oldChild;
 	   for (int i=0; i < toArrangeChildren.size(); i++){
 		   oldChild = toArrangeChildren.get(i);
 		   currentNode.removeChild(oldChild);
 	   }
 	   
 	   // add children again with the new order
 	   Element newChild;
 	   for (int i=0; i < arrangedChildren.size(); i++){
 		   newChild = arrangedChildren.get(i);
 		   currentNode.appendChild(newChild);
 	   }
     }
    
     private void quickSortForArrangingChildren
    		(ArrayList<Element> childrenList, int left, int right){
 	   
 	   if (left < right){
 		   int i,j;
 		   Element middle,elementTemp;
 		   
 		   i = left;
 		   j = right; 
 		   middle = childrenList.get((left + right)/2);
 		   
 		   while (i<=j) {
 			   while (isBefore(childrenList.get(i), middle) && (i < right)) i++;
 			   while (isBefore(middle, childrenList.get(j)) && (j > left)) j--;
 			  
 			   if (i<=j){
 				   elementTemp = childrenList.get(i);
 				   childrenList.set(i, childrenList.get(j));
 				   childrenList.set(j, elementTemp); 
 			   
 				   i++;
 				   j--;
 			   };
 		   };
 			   
 		   if (left < j){
 			   quickSortForArrangingChildren(childrenList, left, j);
 		   };
 		   
 		   if (i < right) {
 			   quickSortForArrangingChildren(childrenList, i, right);
 		   }
 	   }
    }
    
    private boolean isBefore(Element e1, Element e2){
 	   int valueOfE1 = getBoundsValueOf (e1);
 	   int valueOfE2 = getBoundsValueOf (e2);
 	   
 	   return valueOfE1 < valueOfE2;
    }
    
    private int getBoundsValueOf (Element e){
 	   String bounds = e.getAttribute("bounds");
 	   
 	   if (bounds == ""){
 		   return 0;
 	   }
 	   
 	   int indexOfFirstComma = bounds.indexOf(',');
 	   int indexOfSecondComma = bounds.indexOf(',', indexOfFirstComma + 1);	   
 	
 	   int leftUpperX = Integer.parseInt(bounds.substring(0, indexOfFirstComma));
 	   int leftUpperY = Integer.parseInt(bounds.substring(indexOfFirstComma + 1, indexOfSecondComma));
 	  
 	   return leftUpperX + leftUpperY;
    }
    	
    /************************* build link elements ****************************/
 
 	private void buildLinkElements() {
 		
 		Iterator<String> linkIter = linkList.iterator();
 		while (linkIter.hasNext()){
 			
 			String linkID = (String) linkIter.next();
 			
 			Element sourceNode = getSourceNode (linkID);
 			Element targetNode = getTargetNode (linkID);
 			
 			addStandardElement (linkID, sourceNode, targetNode);
 			
 			// build for each item a new <link> element and append it to the
 			// correct flow parent.
 			Element parentFlow = findClosestSharedFlowParent(sourceNode, 
 						targetNode);
 			
 			if (parentFlow != null){
 				Element links = getChildElementWithNodeName(parentFlow, "links",
 									true);
 				Element link = parentFlow.getOwnerDocument().createElement("link");
 			    String linkName = getLinkName (linkID);
 			    link.setAttribute("name", linkName);
 			    links.appendChild(link);
 			} else {
 				logger.warning("BUG : cann't get the correct shared flow parent..."
 						+ " null is returned... ");
 			}
 		}
 	}
 
 	private void addStandardElement(String linkID, Element sourceNode,
 			Element targetNode) {
 		
 		String linkName = getLinkName(linkID);
 	    Element transitionCondition = getTransitionCondition(linkID);
 		
 		// add <target> element under <targets> element in target node
 		Element targets = getChildElementWithNodeName(targetNode, "targets", true);
 		
 		Element target = targetNode.getOwnerDocument().createElement("target");
 		target.setAttribute("linkName", linkName);
 		targets.appendChild(target);
 		
 		// add <source> element under <sources> element in source node
 		// because in RDF2BPEL.xslt the JoinCondition has already recorded,
 		// here it will be ignored
 		Element sources = getChildElementWithNodeName(sourceNode, "sources", true);
 		
 		Element source = sourceNode.getOwnerDocument().createElement("source");
 		source.setAttribute("linkName", linkName);
 		source.appendChild(transitionCondition);
 		sources.appendChild(source);
 
 	}
 	
 	private Element findClosestSharedFlowParent(Element node_A,
 				Element node_B) {
 		
 		if (node_A == null || node_B == null){
 			return null;
 		}
 		
 		String ID_A = node_A.getAttribute("id");
 		String ID_B = node_B.getAttribute("id");
 		
 		// if the both nodes are identical flow elements, return one of them
 		if (node_A.getNodeName().equals("flow") && ID_A.equals(ID_B)){
 			return node_A;
 		}
 		
 		int level_A = getLevelOfNode(ID_A);
 		int level_B = getLevelOfNode(ID_B);
 		
 		if (level_A >= level_B){
 			return findClosestSharedFlowParent(getClosestFlowParentOfNode(ID_A),
 					node_B);
 		} else {
 			return findClosestSharedFlowParent(node_A,
 					getClosestFlowParentOfNode(ID_B));
 		}
 	}   
    
    /*************************** rearrange flow **************************/
  
 	private void rearrangeFlow(Node currentNode) {
 		
 		// handle only the Nodes with type "Element" and "Document" (root element), 
 		// the other types e.g. "Attr","Comment" will be ignored 
 		if (!(currentNode instanceof Element || currentNode instanceof Document)){
 			return;
 		};
 	   
 		// recursive research, work on the child nodes at first.
 		NodeList childNodes = currentNode.getChildNodes();
 		Node child;
 		for (int i=0; i<childNodes.getLength(); i++){
 			child = childNodes.item(i);
 			if (child instanceof Element){
 				rearrangeFlow (child);
 			}
 		};
 	   
 		// the order of child nodes in "flow" is not based on the position
 		// of child nodes, but the order of the edges "link", so we handle
 		// it in a different way.	   	
 		if (currentNode.getNodeName().equals("flow")){
 			handleFlowElement((Element)currentNode);
 		}
 		   
 	}
 	
 	private void handleFlowElement(Element currentNode) {
 		   
 		// find all child nodes which are the source nodes of a link but 
 		// not the target nodes of any other links, put them into a record
 		// list "childrenList", Then use a Breadth-First Search to find all
 		// other child nodes and arrange them with a correct order, in the
 		// end the result will be also in "childrenList" 
 		ArrayList<Element> childrenList = new ArrayList<Element>();
 
 		// record all targetNode IDs
 		ArrayList<String> targetIDList = new ArrayList<String>();
 		NodeList childNodesOfFlow = currentNode.getChildNodes();
 		Node childOfFlow;
 		for (int i=0; i<childNodesOfFlow.getLength(); i++){
 			childOfFlow = childNodesOfFlow.item(i);
 			if (childOfFlow instanceof Element && childOfFlow.getNodeName().equals("links")){
 				Element links = (Element) childOfFlow;
 				NodeList childNodesOfLinks = links.getChildNodes();
 				Node link;
 				for (int k=0; k<childNodesOfLinks.getLength(); k++){
 					link = childNodesOfLinks.item(k);
 					if (link instanceof Element && link.getNodeName().equals("link")){
 						String targetID = ((Element)link).getAttribute("targetID");
 						targetIDList.add(targetID); 
 					};
 				}
 				break;
 			}
 		};
 		   
 		// filter child nodes of flow with targetIDList in order get all
 		// nodes which are just the source nodes of a link but not the 
 		// target nodes of any other links, also are the start nodes of 
 		// BFS
 		for (int i=0; i<childNodesOfFlow.getLength(); i++){
 			childOfFlow = childNodesOfFlow.item(i);
 			if (childOfFlow instanceof Element  
 					&& !childOfFlow.getNodeName().equals("links")
 					&& !isTargetOfOtherNode((Element)childOfFlow, targetIDList)){
 		   
 				childrenList.add((Element)childOfFlow);
 				logger.finer("add Node : " +  childOfFlow.getNodeName() +
 					" in list...");
 			}
 		};
 	   
 		// if no such node can be found, there are 2 possibilities
 		// 1: flow doesn't contain any child at all.
 		// 2: there is a node circle.
 		// for the second situation, we put the first element node of flow in the list
 		if (childrenList.size()==0){
 			for (int i=0; i<childNodesOfFlow.getLength(); i++){
 				childOfFlow = childNodesOfFlow.item(i);
 				if (childOfFlow instanceof Element
 						&& !childOfFlow.getNodeName().equals("links")){
 					childrenList.add((Element)childOfFlow);
 					break;
 				}
 			};
 		}
 	   
 		logger.finest("BFS starts...");
 		BFSForArrangingChildrenOfFlow (0, childrenList);
 	   
 		// delete old children
 		childNodesOfFlow = currentNode.getChildNodes();
 		Node oldChild;
 		for (int i=0; i < childNodesOfFlow.getLength(); i++){
 			oldChild = childNodesOfFlow.item(i);
 			if (oldChild instanceof Element
 					&& !oldChild.getNodeName().equals("links")){
 				currentNode.removeChild(oldChild);
 			}
 		}
 	   
 		// add children again with the new order
 		Element newChild;
 		for (int i=0; i < childrenList.size(); i++){
 			newChild = childrenList.get(i);
 			currentNode.appendChild(newChild);
 		}
 		
 	}
 	
 	private boolean isTargetOfOtherNode(Element e,
 			   ArrayList<String> targetIDList) {
 		   
 		   String currentId = e.getAttribute("id");
 		
 		   if (currentId == null){
 			   return false;
 		   }
 		
 		   Iterator<String> listIter = targetIDList.iterator();
 		   String targetId;
 		   while (listIter.hasNext()){
 			   targetId = listIter.next();
 			   if (targetId.equals(currentId)){
 				   return true;
 			   }
 		   }
 		   
 		   return false;
 		}
 	   
 	   private void BFSForArrangingChildrenOfFlow (int index, 
 			   ArrayList<Element> childrenList){
 		   
 		   if (index >= childrenList.size()){
 			   return;
 		   }
 		   
 		   Element sourceNode = childrenList.get(index);
 		   
 		   logger.finer("index :" + index);
 		   logger.finer("node : " + sourceNode.getNodeName());
 		   
 		   markNodeAsVisited (sourceNode);
 		   
 		   ArrayList<String> outgoingLinkIDs = findAllOutgoingLinksOf(sourceNode);
 
 		   Element targetNode;
 		   Iterator<String> outgoingLinksIter = outgoingLinkIDs.iterator();
 		   String linkID;
 		   while (outgoingLinksIter.hasNext()){
 			   linkID = outgoingLinksIter.next();
 			   targetNode = getTargetNode(linkID);
 			   logger.finer("targetNode :" + targetNode);
 			   if (targetNode != null){
 				   if (!isVisited(targetNode)) {
 					   childrenList.add(targetNode);
 				   }
 			   }
 		   };
 		   
 		   BFSForArrangingChildrenOfFlow (index + 1, childrenList);
 	   }
 
 
 
 
 	  private void markNodeAsVisited (Element e){
 		  e.setAttribute("visited", "true");
 	  }
 	  
 	  
 	  private boolean isVisited(Element e) {
 		  String result = e.getAttribute("visited");
 		  if (result.equals("true")){
 			  return true;
 		  } else {
 			  return false;
 		  }
 	  }
 
 	  
 	  private ArrayList<String> findAllOutgoingLinksOf (Element e){
 		  ArrayList<String> linksList = new ArrayList<String>() ;
 		  
 		  NodeList childNodesOfE = e.getChildNodes();
 		  Node childOfE;
 		  String id;
 		  
 		  for (int i=0; i<childNodesOfE.getLength(); i++){
 			  childOfE = childNodesOfE.item(i);
 			  if (childOfE instanceof Element && childOfE.getNodeName().equals("outgoing")){
 				  id = ((Element) childOfE).getNodeValue();
 				  linksList.add(id);
 			  }
 		  }; 
 		   
 		  return linksList;
 	  }
 
 
 	/****************************** other GET-methods **************************/
   
 
 	private Element getChildElementWithNodeName(Element currentNode, 
 			String childName, boolean ifNullBuildNewElement) {
 		
 		NodeList childrenList = currentNode.getChildNodes();
 		for (int i = 0; i < childrenList.getLength(); i++){
 			Node child = childrenList.item(i);
 			if (child instanceof Element && child.getNodeName().equals(childName)){
 				return (Element)child;
 			}
 		}
 		
 		// if no such child element can be found
 		if (ifNullBuildNewElement){
 			Element newNode = currentNode.getOwnerDocument()
 								.createElement(childName);
			currentNode.appendChild(newNode);
 			return newNode;
 		} else {
 			return null;
 		}
 	}
 	
 	private Element getSourceNode(String linkID) {
 		String sourceID = getSourceID(linkID);
 		return getNodeWithID(sourceID);
 	}
 	
 	private Element getTargetNode(String linkID) {
 		String targetID = getTargetID(linkID);
 		return getNodeWithID(targetID);
 	}
 
 	private String getSourceID(String linkID) {
 		Object[] infoSet = linksMap.get(linkID);
 		return (String)infoSet[0];
 	}
 	
 	private String getTargetID(String linkID) {
 		Object[] infoSet = linksMap.get(linkID);
 		return (String)infoSet[1];
 	}
 
 	private String getLinkName(String linkID) {
 		Object[] infoSet = linksMap.get(linkID);
 		return (String)infoSet[2];
 	}
 
 	private Element getTransitionCondition(String linkID) {
 		Object[] infoSet = linksMap.get(linkID);
 		return (Element)infoSet[3];
 	}
 	
 	private Element getNodeWithID (String nodeID){
 		Object[] infoSet = activityNodesMap.get(nodeID);
 		return (Element)infoSet[0];
 	}
 	
 	private int getLevelOfNode (String nodeID){
 		Object[] infoSet = activityNodesMap.get(nodeID);
 		return (Integer)infoSet[1];
 	}
 	
 	private Element getClosestFlowParentOfNode (String nodeID){
 		Object[] infoSet = activityNodesMap.get(nodeID);
 		return (Element)infoSet[2];
 	}
 }
