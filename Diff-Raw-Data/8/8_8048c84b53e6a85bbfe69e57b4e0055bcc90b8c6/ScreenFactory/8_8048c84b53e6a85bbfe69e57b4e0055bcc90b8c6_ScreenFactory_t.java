 /*
  * Copyright 2009 Sysmap Solutions Software e Consultoria Ltda.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 package br.com.sysmap.crux.core.client.component;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import br.com.sysmap.crux.core.client.JSEngine;
 import br.com.sysmap.crux.core.client.formatter.Formatter;
 import br.com.sysmap.crux.core.client.formatter.RegisteredClientFormatters;
 import br.com.sysmap.crux.core.client.utils.DOMUtils;
 
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.dom.client.Element;
 import com.google.gwt.dom.client.Node;
 import com.google.gwt.dom.client.NodeList;
 import com.google.gwt.user.client.DOM;
 import com.google.gwt.user.client.ui.HasWidgets;
 import com.google.gwt.user.client.ui.RootPanel;
 import com.google.gwt.user.client.ui.Widget;
 
 /**
  * Factory for CRUX screen. Based in the type (extracted from _type attribute in widget declaration span tag), 
  * determine witch class to create for all screen widgets.
  * @author Thiago
  *
  */
 public class ScreenFactory {
 	
 	private static ScreenFactory instance = null;
 	 
 	private Screen screen = null;
 	private RegisteredClientFormatters registeredClientFormatters = null;
 	private RegisteredWidgetFactories registeredWidgetFactories = null;
 	private String screenId = null;
 	
 	private ScreenFactory()
 	{
 		this.registeredWidgetFactories = (RegisteredWidgetFactories) GWT.create(RegisteredWidgetFactories.class);
 		this.registeredClientFormatters = (RegisteredClientFormatters) GWT.create(RegisteredClientFormatters.class);
 	}
 	
 	/**
 	 * Retrieve the ScreenFactory instance.
 	 * Is not synchronized, but it is not a problem. The screen is always build on a single thread
 	 * @return
 	 */
 	public static ScreenFactory getInstance()
 	{
 		if (instance == null)
 		{
 			instance = new ScreenFactory();
 		}
 		return instance;
 	}
 	
 	/**
 	 * Get the screen associated with current page. If not created yet, create it.
 	 * @return
 	 */
 	public Screen getScreen()
 	{
 		if (screen == null)
 		{
 			create();
 		}
 		return screen;
 	}
 	
 	private void create()
 	{
 		screen = new Screen(getScreenId());
 		Element body = RootPanel.getBodyElement();
 		NodeList<Element> widgetCandidates = body.getElementsByTagName("span");
 		List<Element> widgets = new ArrayList<Element>();
 		Element screenElement = null;
 		
 		for (int i=0; i<widgetCandidates.getLength(); i++)
 		{
 			if (isValidWidget(widgetCandidates.getItem(i)))
 			{
 				try 
 				{
 					createWidget(widgetCandidates.getItem(i), screen, widgets);
 				}
 				catch (Throwable e) 
 				{
 					GWT.log(e.getLocalizedMessage(), e);
 					widgetCandidates.getItem(i).setInnerText(JSEngine.messages.screenFactoryGenericErrorCreateWidget(e.getLocalizedMessage()));
 				}
 			}
 			else if (isScreenDefinitions(widgetCandidates.getItem(i)))
 			{
 				screenElement = widgetCandidates.getItem(i);
 				screen.parse(screenElement);
 			}
 		}
 		if (screenElement != null)
 		{
 			clearScreenMetaTag(screenElement);
 		}
 		clearWidgetsMetaTags(widgets);
 		screen.load();
 	}
 	
 	private void clearScreenMetaTag(Element screenElement)
 	{
 		while (screenElement.hasChildNodes())
 		{
 			Node child = screenElement.getFirstChild();
 			screenElement.removeChild(child);
 			screenElement.getParentNode().appendChild(child);
 		}
 		Node parent = screenElement.getParentNode();
 		if (parent != null)
 		{
 			parent.removeChild(screenElement);
 		}
 	}
 	
 	private void clearWidgetsMetaTags(List<Element> widgets)
 	{
 		for (Element element : widgets) 
 		{
 			String widgetId = element.getId();
 			if (widgetId != null && widgetId.length() >= 0)
 			{
 				element = DOM.getElementById(widgetId); // Evita que elementos reanexados ao DOM sejam esquecidos
 				Element parent;
 				if (element != null && (parent = element.getParentElement())!= null)
 				{
 					parent.removeChild(element);
 				}
 			}
 		}
 	}
 	
 	public String getScreenId()
 	{
 		if (screenId == null)
 		{
 			String fileName = DOMUtils.getDocumentName();
 			int indexBeg = fileName.indexOf(GWT.getModuleName());
 			int indexEnd = fileName.indexOf("?");
 			int begin = (indexBeg == -1) ? 0 : indexBeg;
 			int end = (indexEnd == -1) ? fileName.length() : indexEnd;
 			screenId = fileName.substring(begin, end);
 		}
 		return screenId;
 	}
 
 	Widget newWidget(Element element, String widgetId) throws InterfaceConfigException
 	{
 		String type = element.getAttribute("_type");
 		WidgetFactory<? extends Widget> widgetFactory = registeredWidgetFactories.getWidgetFactory(type);
 		if (widgetFactory == null)
 		{
 			throw new InterfaceConfigException(JSEngine.messages.screenFactoryWidgetFactoryNotFound(type));
 		}
 		
 		Widget widget = widgetFactory.createWidget(element, widgetId); 
 		if (widget == null)
 		{
 			throw new InterfaceConfigException(JSEngine.messages.screenFactoryErrorCreateWidget(widgetId));
 		}
 		
 		return widget;
 	}
 	
 	private boolean isScreenDefinitions(Element element)
 	{
 		if ("span".equalsIgnoreCase(element.getTagName()))
 		{
 			String type = element.getAttribute("_type");
 			if (type != null && "screen".equals(type))
 			{
 				return true;
 			}
 		}
 		return false;
 	}
 
 	boolean isValidWidget(Element element)
 	{
 		if ("span".equalsIgnoreCase(element.getTagName()))
 		{
 			String type = element.getAttribute("_type");
 			if (type != null && type.length() > 0 && !"screen".equals(type))
 			{
 				return true;
 			}
 		}
 		return false;
 	}
 	
 	private Element getParentElement(Element element) 
 	{
 		Element elementParent = element.getParentElement();
 		while (elementParent != null && !"body".equalsIgnoreCase(elementParent.getTagName()))
 		{
 			if (isValidWidget(elementParent))
 			{
 				return elementParent;
 			}
 			elementParent = elementParent.getParentElement();
 		}
 			
 		return null;	
 	}
 	
 	private Widget getParentWidget(Element element, Screen screen) throws InterfaceConfigException
 	{
 		String id = element.getId();
 		Widget parent = screen.getWidget(id); 
 
 		if (parent != null && !(parent instanceof HasWidgets))
 		{
 			throw new InterfaceConfigException(JSEngine.messages.screenFactoryInvalidWidgetParent(element.getId()));
 		}
 		return parent;
 	}
 	
 	@SuppressWarnings("unchecked")
 	private Widget createWidget(Element element, Screen screen, List<Element> widgetsElementsAdded) throws InterfaceConfigException
 	{
 		String widgetId = element.getId();
 		if (widgetId == null || widgetId.length() == 0)
 		{
 			throw new InterfaceConfigException(JSEngine.messages.screenFactoryWidgetIdRequired());
 		}
 		Widget widget = screen.getWidget(widgetId);
 		if (widget != null)
 		{
 			return widget;
 		}
 		
 		Widget parent = null;
 		Element parentElement = getParentElement(element);
 		if (parentElement != null)
 		{
 			parent = getParentWidget(parentElement, screen);
 			if (parent == null)
 			{
 				parent = createWidget(parentElement, screen, widgetsElementsAdded);
 			}
 		}
 		
 		widget = newWidget(element, widgetId);
 
 		if (parent != null)
 		{
 			HasWidgetsFactory<Widget> parentWidgetFactory = (HasWidgetsFactory<Widget>) registeredWidgetFactories.
 															getWidgetFactory(parentElement.getAttribute("_type"));
 			parentWidgetFactory.add(parent, widget, parentElement, element);
 		}
 		else
 		{
			GWT.log(JSEngine.messages.screenFactoryNonDeterministicWidgetPositionInParent(widgetId), null);
 			CruxWidgetPanel panel = new CruxWidgetPanel(element.getParentElement());
 			panel.add(widget);
 		}
 		widgetsElementsAdded.add(element);
 		return widget;
 	}
 	
 	public Formatter getClientFormatter(String formatter)
 	{
 		return this.registeredClientFormatters.getClientFormatter(formatter);
 	}
 }
