 package de.jutzig.jabylon.rest.ui.navbar;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import org.apache.wicket.markup.html.link.BookmarkablePageLink;
 import org.apache.wicket.markup.html.list.ListItem;
 import org.apache.wicket.markup.html.list.ListView;
 import org.apache.wicket.markup.html.panel.Panel;
 import org.apache.wicket.request.mapper.parameter.PageParameters;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IConfigurationElement;
 import org.eclipse.core.runtime.RegistryFactory;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import de.jutzig.jabylon.properties.Resolvable;
 import de.jutzig.jabylon.rest.ui.wicket.BasicResolvablePanel;
 import de.jutzig.jabylon.rest.ui.wicket.PanelFactory;
 import de.jutzig.jabylon.rest.ui.wicket.WelcomePage;
 
 public class NavbarPanel<T extends Resolvable<?, ?>> extends BasicResolvablePanel<T> {
 
 	private static final long serialVersionUID = 1L;
 
 	private static final Logger logger = LoggerFactory.getLogger(NavbarPanel.class);
 	
 	public NavbarPanel(String id, T object, PageParameters parameters) {
 		super(id, object, parameters);
 		add(new BookmarkablePageLink<String>("jabylon",WelcomePage.class));
 		Map<PanelFactory, Boolean> data = loadNavBarExtensions();
 
 		List<PanelFactory> items = new ArrayList<PanelFactory>();
 		List<PanelFactory> rightAligned = new ArrayList<PanelFactory>();
 
 		
 		
 		for (Entry<PanelFactory, Boolean> entry : data.entrySet()) {
 			if(entry.getValue())
 				rightAligned.add(entry.getKey());
 			else
 				items.add(entry.getKey());
 		}
 		
 		ListView<PanelFactory> listView = new ListView<PanelFactory>("items", items) {
 
 			private static final long serialVersionUID = 1L;
 
 			@Override
 			protected void populateItem(ListItem<PanelFactory> item) {
 				Panel newPanel = item.getModelObject().createPanel(getPageParameters(), NavbarPanel.this.getModelObject(), "content");
 				item.add(newPanel);
 			}
 		};
 		listView.setRenderBodyOnly(true);
 		add(listView);
 		
 		ListView<PanelFactory> rightListView = new ListView<PanelFactory>("right-items", rightAligned) {
 
 			private static final long serialVersionUID = 1L;
 
 			@Override
 			protected void populateItem(ListItem<PanelFactory> item) {
 				Panel newPanel = item.getModelObject().createPanel(getPageParameters(), NavbarPanel.this.getModelObject(), "content");
 				item.add(newPanel);
 			}
 		};
 		rightListView.setRenderBodyOnly(true);
 		add(rightListView);
 	}
 
 
 	private Map<PanelFactory,Boolean> loadNavBarExtensions() {
		Map<PanelFactory, Boolean> extensions = new LinkedHashMap<PanelFactory, Boolean>();
 		IConfigurationElement[] configurationElements = RegistryFactory.getRegistry().getConfigurationElementsFor(
 				"de.jutzig.jabylon.rest.ui.navbarItem");
 
 		for (IConfigurationElement element : configurationElements) {
 			try {
 				PanelFactory extension = (PanelFactory) element.createExecutableExtension("panel");
 				String pullRight = element.getAttribute("pullRight");
 				if(pullRight!=null && Boolean.valueOf(pullRight))
 					extensions.put(extension, true);
 				else
 					extensions.put(extension, false);
 			} catch (CoreException e) {
 				logger.error("Failed to load extension "+element,e);
 			}
 		}
 
 		return extensions;
 	}
 
 }
