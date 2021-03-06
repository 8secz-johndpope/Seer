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
 package br.com.sysmap.crux.module;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.locks.Lock;
 import java.util.concurrent.locks.ReentrantLock;
 
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
 import javax.xml.parsers.ParserConfigurationException;
 
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 import org.xml.sax.SAXException;
 
 import br.com.sysmap.crux.classpath.URLResourceHandlersRegistry;
 import br.com.sysmap.crux.core.i18n.MessagesFactory;
 import br.com.sysmap.crux.core.rebind.scanner.module.Module;
 import br.com.sysmap.crux.core.rebind.scanner.module.Modules;
 import br.com.sysmap.crux.core.server.Environment;
 import br.com.sysmap.crux.core.utils.RegexpPatterns;
 import br.com.sysmap.crux.module.config.CruxModuleConfigurationFactory;
 import br.com.sysmap.crux.module.validation.CruxModuleValidator;
 
 /**
  * @author Thiago da Rosa de Bustamante
  *
  */
 public class CruxModuleHandler
 {
 	private static Map<Module, CruxModule> cruxModules = null;
 	private static boolean pagesInitialized = false;
 	private static final Lock lock = new ReentrantLock();
 	private static final Lock lockPages = new ReentrantLock();
 	private static final CruxModuleMessages messages = MessagesFactory.getMessages(CruxModuleMessages.class);
 	private static DocumentBuilder documentBuilder;
 	
 	/**
 	 * 
 	 */
 	public static void initialize()
 	{
 		if (cruxModules != null)
 		{
 			return;
 		}
 		try
 		{
 			lock.lock();
 			if (cruxModules != null)
 			{
 				return;
 			}
 			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
 			documentBuilder = documentBuilderFactory.newDocumentBuilder();
 			
 			initializeModules();
 		}
 		catch (ParserConfigurationException e)
 		{
 			throw new CruxModuleException(messages.errorInitializingCruxModuleHandler(), e);
 		}
 		finally
 		{
 			lock.unlock();
 		}
 	}
 
 	/**
 	 * 
 	 * @return
 	 */
 	public static Iterator<CruxModule> iterateCruxModules()
 	{
 		if (cruxModules == null)
 		{
 			initialize();
 		}
 		return cruxModules.values().iterator();
 	}
 	
 	/**
 	 * 
 	 * @return
 	 */
 	public static CruxModule getCurrentModule()
 	{
 		String currentModule = CruxModuleBridge.getInstance().getCurrentModule(); 
 		
 		if (currentModule == null || currentModule.length() == 0)
 		{
 			Iterator<CruxModule> modules = iterateCruxModules();
 			if (modules.hasNext())
 			{
 				return modules.next();
 			}
 			else
 			{
 				throw new CruxModuleException(messages.developmentCruxModuleNotDefined());
 			}
 		}		
 		return getCruxModule(currentModule);
 	}
 
 	/**
 	 * 
 	 * @return
 	 */
 	public static String[] getDevelopmentModules()
 	{
 		if (Environment.isProduction())
 		{
 			return null;
 		}
 		String developmentModules = CruxModuleConfigurationFactory.getConfigurations().developmentModules();
 		if (developmentModules == null)
 		{
 			return new String[0];
 		}
 		String[] modules = RegexpPatterns.REGEXP_COMMA.split(developmentModules);
 		for (int i=0; i< modules.length; i++)
 		{
 			modules[i] = modules[i].trim();
 		}
 		return modules;
 	}
 	
 	
 	/**
 	 * 
 	 * @param module
 	 * @return
 	 */
 	public static CruxModule getCruxModule(String module)
 	{
 		if (cruxModules == null)
 		{
 			initialize();
 		}
 		Module mod = Modules.getInstance().getModule(module);
 		return cruxModules.get(mod);
 	}
 	
 	/**
 	 * 
 	 * @param module
 	 * @return
 	 */
 	public static boolean isDependenciesOk(String module)
 	{
 		return CruxModuleValidator.isDependenciesOk(module);
 	}
 
 	/**
 	 * 
 	 */
 	private static void initializeModules()
 	{
 		cruxModules = new HashMap<Module, CruxModule>();
 		Iterator<Module> modules = Modules.getInstance().iterateModules();
 		while (modules.hasNext())
 		{
 			Module module = modules.next();
 			try
 			{
				String moduleDescriptor = "/"+RegexpPatterns.REGEXP_DOT.matcher(module.getFullName()).replaceAll("/")+".module.xml";
				InputStream stream = CruxModuleHandler.class.getResourceAsStream(moduleDescriptor);
				ModuleInfo info = parseModuleDescriptor(stream);
 				if (info != null)
 				{
 					cruxModules.put(module, buildCruxModule(module, info));
 				}
 			}
 			catch (Exception e)
 			{
 				throw new CruxModuleException(messages.errorInitializingCruxModuleHandler(), e);
 			}
 		}
 	}
 
 	/**
 	 * 
 	 * @param stream
 	 * @return
 	 * @throws SAXException
 	 * @throws IOException
 	 */
 	public static ModuleInfo parseModuleDescriptor(InputStream stream) throws SAXException, IOException
 	{
 		if (stream == null)
 		{
 			return null;
 		}
 		
 		ModuleInfo info = new ModuleInfo();
 		Document descriptor = documentBuilder.parse(stream);
 		NodeList childNodes = descriptor.getDocumentElement().getChildNodes();
 		for (int i=0; i<childNodes.getLength(); i++)
 		{
 			Node item = childNodes.item(i);
 			
 			if (item instanceof Element)
 			{
 				Element element = (Element) item;
 				
 				String localName = element.getNodeName();
 				if ("description".equalsIgnoreCase(localName))
 				{
 					info.setDescription(element.getTextContent());
 				}
 			}
 		}
 		return info;
 	}
 
 	/**
 	 * 
 	 */
 	static void initializeModulesDependencies(CruxModule cruxModule)
 	{
 		Set<String> inherits = cruxModule.getGwtModule().getInherits();
 		List<ModuleRef> requiredModules = new ArrayList<ModuleRef>();
 		
 		for (String inherit : inherits)
         {
 			CruxModule dependency = getCruxModule(inherit);
 			if (dependency != null)
 			{
 				requiredModules.add(new ModuleRef(dependency.getName()));
 			}
         }
 		
 		cruxModule.setRequiredModules(requiredModules.toArray(new ModuleRef[requiredModules.size()]));
 	}
 	
 	
 	/**
 	 * 
 	 */
 	static void initializeModulesPages()
 	{
 		if (!pagesInitialized)
 		{
 			lockPages.lock();
 			if (!pagesInitialized)
 			{
 				pagesInitialized = true;
 				try
 				{
 					String currentModule = CruxModuleBridge.getInstance().getCurrentModule(); 
 					try
 					{
 						for (CruxModule cruxModule : cruxModules.values())
 						{
 							CruxModuleBridge.getInstance().registerCurrentModule(cruxModule.getName());
 							searchModulePages(cruxModule);
 						}
 					}
 					finally
 					{
 						CruxModuleBridge.getInstance().registerCurrentModule(currentModule);
 					}
 				}
 				finally
 				{
 					lockPages.unlock();
 				}
 			}
 		}
 	}
 
 	/**
 	 * 
 	 * @param module
 	 * @param info
 	 * @return
 	 */
 	private static CruxModule buildCruxModule(Module module, ModuleInfo info)
 	{
 		CruxModule cruxModule = new CruxModule(module, info);
 		URL location = module.getDescriptorURL();
 		location = URLResourceHandlersRegistry.getURLResourceHandler(location.getProtocol()).getParentDir(location);
 		cruxModule.setLocation(location);
 		return cruxModule;
 	}	
 
 
 	/**
 	 * 
 	 * @param cruxModule
 	 */
 	private static void searchModulePages(CruxModule cruxModule)
 	{
 		try
 		{
 			String[] modulePages = Modules.getInstance().searchModulePages(cruxModule.getGwtModule());
 			cruxModule.setPages(modulePages);
 		}
 		catch (Exception e)
 		{
 			throw new CruxModuleException(messages.errorSearchingForModulePages(cruxModule.getName()), e);
 		}
 	}
 }
