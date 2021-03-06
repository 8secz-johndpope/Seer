 /*******************************************************************************
  * Copyright (c) 2006, 2007 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.help.internal.toc;
 
 import java.util.HashMap;
 import java.util.Map;
 
 import org.eclipse.core.expressions.IEvaluationContext;
 import org.eclipse.help.IToc;
 import org.eclipse.help.ITocContribution;
 import org.eclipse.help.ITopic;
 import org.eclipse.help.IUAElement;
 import org.eclipse.help.internal.UAElement;
 import org.w3c.dom.Element;
 
 public class Toc extends UAElement implements IToc {
 
 	public static final String NAME = "toc"; //$NON-NLS-1$
 	public static final String ATTRIBUTE_LABEL = "label"; //$NON-NLS-1$
 	public static final String ATTRIBUTE_HREF = "href"; //$NON-NLS-1$
 	public static final String ATTRIBUTE_TOPIC = "topic"; //$NON-NLS-1$
 	public static final String ATTRIBUTE_LINK_TO = "link_to"; //$NON-NLS-1$
 	public static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
 
 	private ITocContribution contribution;
 	private ITopic topic;
 	private Map href2TopicMap;
 
 	public Toc(IToc src) {
 		super(NAME, src);
 		setHref(src.getHref());
 		setLabel(src.getLabel());
 		ITopic topic = src.getTopic(null);
 		if (topic != null) {
 			setTopic(topic.getHref());
 		}
 		appendChildren(src.getChildren());
 	}
 	
 	public Toc(Element src) {
 		super(src);
 	}
 	
 	/*
 	 * Creates a mapping of all topic hrefs to ITopics.
 	 */
 	private Map createHref2TopicMap() {
 		Map map = new HashMap();
 		if (topic != null) {
 			map.put(topic.getHref(), topic);
 		}
 		ITopic[] topics = getTopics();
 		for (int i=0;i<topics.length;++i) {
 			createHref2TopicMapAux(map, topics[i]);
 		}
 		return map;
 	}
 
 	/*
 	 * Creates a mapping of all topic hrefs to ITopics under the given
 	 * ITopic and stores in the given Map.
 	 */
 	private void createHref2TopicMapAux(Map map, ITopic topic) {
 		map.put(topic.getHref(), topic);
 		ITopic[] subtopics = topic.getSubtopics();
 		if (subtopics != null) {
 			for (int i=0;i<subtopics.length;++i) {
 				if (subtopics[i] != null) {
 					createHref2TopicMapAux(map, subtopics[i]);
 				}
 			}
 		}
 	}
 
 	public String getHref() {
 		return getAttribute(ATTRIBUTE_HREF);
 	}
 
 	/*
 	 * Returns a mapping of all topic hrefs to ITopics.
 	 */
	private Map getHref2TopicMap() {
 		if (href2TopicMap == null) {
 			href2TopicMap = createHref2TopicMap();
 		}
 		return href2TopicMap;
 	}
 	
 	public String getLabel() {
 		return getAttribute(ATTRIBUTE_LABEL);
 	}
 	
 	public String getLinkTo() {
 		return getAttribute(ATTRIBUTE_LINK_TO);
 	}
 	
 	public String getTopic() {
 		return getAttribute(ATTRIBUTE_TOPIC);
 	}
 	
 	public ITopic getTopic(String href) {
 		if (href == null) {
 			if (topic == null) {
 				topic = new ITopic() {
 					public String getHref() {
 						return getTopic();
 					}
 					public String getLabel() {
 						return Toc.this.getLabel();
 					}
 					public ITopic[] getSubtopics() {
 						return getTopics();
 					}
 					public boolean isEnabled(IEvaluationContext context) {
 						return isEnabled(context);
 					}
 					public IUAElement[] getChildren() {
 						return getChildren();
 					}
 				};
 			}
 			return topic;
 		}
 		else {
 			return (ITopic)getHref2TopicMap().get(href);
 		}
 	}
 
 	public ITopic[] getTopics() {
 		return (ITopic[])getChildren(ITopic.class);
 	}
 	
 	public void setLabel(String label) {
 		setAttribute(ATTRIBUTE_LABEL, label);
 	}
 
 	public void setLinkTo(String linkTo) {
 		setAttribute(ATTRIBUTE_LINK_TO, linkTo);
 	}
 
 	public void setTopic(String href) {
 		setAttribute(ATTRIBUTE_TOPIC, href);
 	}
 	
 	public void setHref(String href) {
 		setAttribute(ATTRIBUTE_HREF, href);
 	}
 	
 	public ITocContribution getTocContribution() {
 		return contribution;
 	}
 	
 	public void setTocContribution(ITocContribution contribution) {
 		this.contribution = contribution;
 	}
 }
