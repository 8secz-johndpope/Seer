 /*
  * Metadata Editor
  * @author Jiri Kremser
  *
  *
  *
  * Metadata Editor - Rich internet application for editing metadata.
  * Copyright (C) 2011  Jiri Kremser (kremser@mzk.cz)
  * Moravian Library in Brno
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  *
  *
  */
 
 package cz.fi.muni.xkremser.editor.server.handler;
 
 import java.io.File;
 import java.io.FileFilter;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 
 import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
 import java.util.List;
 
 import javax.servlet.http.HttpSession;
 
 import javax.xml.parsers.ParserConfigurationException;
 import javax.xml.xpath.XPathExpressionException;
 
 import javax.inject.Inject;
 
 import com.google.inject.Provider;
 import com.google.inject.name.Named;
 import com.gwtplatform.dispatch.server.ExecutionContext;
 import com.gwtplatform.dispatch.server.actionhandler.ActionHandler;
 import com.gwtplatform.dispatch.shared.ActionException;
 
 import org.apache.log4j.Logger;
 
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 
 import org.xml.sax.SAXException;
 
 import cz.fi.muni.xkremser.editor.client.util.Constants;
 
 import cz.fi.muni.xkremser.editor.server.ServerUtils;
 import cz.fi.muni.xkremser.editor.server.DAO.InputQueueItemDAO;
 import cz.fi.muni.xkremser.editor.server.config.EditorConfiguration;
 import cz.fi.muni.xkremser.editor.server.exception.DatabaseException;
 import cz.fi.muni.xkremser.editor.server.fedora.FedoraAccess;
 import cz.fi.muni.xkremser.editor.server.fedora.utils.XMLUtils;
 
 import cz.fi.muni.xkremser.editor.shared.domain.DigitalObjectModel;
 import cz.fi.muni.xkremser.editor.shared.rpc.InputQueueItem;
 import cz.fi.muni.xkremser.editor.shared.rpc.action.ScanInputQueueAction;
 import cz.fi.muni.xkremser.editor.shared.rpc.action.ScanInputQueueResult;
 
 // TODO: Auto-generated Javadoc
 /**
  * The Class ScanInputQueueHandler.
  */
 public class ScanInputQueueHandler
         implements ActionHandler<ScanInputQueueAction, ScanInputQueueResult> {
 
     /** The logger. */
     private static final Logger LOGGER = Logger
             .getLogger(ScanInputQueueHandler.class.getPackage().toString());
 
     private static final Object LOCK = ScanInputQueueHandler.class;
 
     /** The configuration. */
     private final EditorConfiguration configuration;
 
     /** The fedora access. */
     @Inject
     @Named("securedFedoraAccess")
     private FedoraAccess fedoraAccess;
 
     /** The input queue dao. */
     @Inject
     private InputQueueItemDAO inputQueueDAO;
 
     /** The http session provider. */
     @Inject
     private Provider<HttpSession> httpSessionProvider;
 
     /**
      * Instantiates a new scan input queue handler.
      * 
      * @param configuration
      *        the configuration
      */
     @Inject
     public ScanInputQueueHandler(final EditorConfiguration configuration) {
         this.configuration = configuration;
     }
 
     /*
      * (non-Javadoc)
      * @see
      * com.gwtplatform.dispatch.server.actionhandler.ActionHandler#execute(com
      * .gwtplatform.dispatch.shared.Action,
      * com.gwtplatform.dispatch.server.ExecutionContext)
      */
     @Override
     public ScanInputQueueResult execute(final ScanInputQueueAction action, final ExecutionContext context)
             throws ActionException {
         // parse input
         final String id = action.getId() == null ? "" : action.getId();
         final boolean refresh = action.isRefresh();
         final String base = configuration.getScanInputQueuePath();
         LOGGER.debug("Processing input queue: " + base + id);
         ServerUtils.checkExpiredSession(httpSessionProvider);
 
         ScanInputQueueResult result = null;
 
         if (!refresh) {
             if (base == null || "".equals(base)) {
                 LOGGER.error("Scanning input queue: Action failed because attribut "
                         + EditorConfiguration.ServerConstants.INPUT_QUEUE + " is not set.");
                 throw new ActionException("Scanning input queue: Action failed because attribut "
                         + EditorConfiguration.ServerConstants.INPUT_QUEUE + " is not set.");
             }
             ArrayList<InputQueueItem> list; // due to gwt performance issues, more
                                             // concrete interface is used
             try {
                 if (id == null || "".equals(id)) { // top level
 
                     if ((list = inputQueueDAO.getItems(id)).size() == 0) { // empty db
                         result = new ScanInputQueueResult(updateDb(base));
                     } else {
                         result = new ScanInputQueueResult(list);
                     }
                 } else {
                     result = new ScanInputQueueResult(inputQueueDAO.getItems(id));
                 }
             } catch (DatabaseException e) {
                 throw new ActionException(e);
             }
         }
 
         if (refresh) {
             try {
                 result = new ScanInputQueueResult(updateDb(base));
             } catch (DatabaseException e) {
                 throw new ActionException(e);
             }
         }
         return result;
     }
 
     /**
      * Check document types.
      * 
      * @param types
      *        the types
      * @throws ActionException
      *         the action exception
      */
     private void checkDocumentTypes(String[] types) throws ActionException {
         for (String uuid : types) {
             if (!fedoraAccess.isDigitalObjectPresent(Constants.FEDORA_MODEL_PREFIX
                     + DigitalObjectModel.toString(DigitalObjectModel.parseString(uuid)))) {
                 LOGGER.error("Model " + uuid + " is not present in repository.");
                 throw new ActionException(Constants.FEDORA_MODEL_PREFIX + uuid);
             }
         }
     }
 
     /**
      * Update db.
      * 
      * @param base
      *        the base
      * @return the array list
      * @throws DatabaseException
      */
     private ArrayList<InputQueueItem> updateDb(String base) throws DatabaseException {
 
         String[] types = configuration.getDocumentTypes();
         try {
             checkDocumentTypes(types);
         } catch (ActionException e) {
             LOGGER.warn("Unsupported fedora model, check your configuration.properties for documentTypes. They have to be the same as models in Fedora Commons repository.");
         }
         ArrayList<InputQueueItem> list = new ArrayList<InputQueueItem>();
         ArrayList<InputQueueItem> listTopLvl = new ArrayList<InputQueueItem>(types.length);
         synchronized (LOCK) {
             for (int i = 0; i < types.length; i++) {
                 File test = new File(base + File.separator + types[i]);
                 if (!test.exists()) {
                     test.mkdir(); // create if not exists
                 }
                 InputQueueItem topLvl = new InputQueueItem(File.separator + types[i], types[i], false);
                 listTopLvl.add(topLvl);
                 list.add(topLvl);
                 list.addAll(scanDirectoryStructure(base, File.separator + types[i]));
             }
         }
        Collections.sort(list, new Comparator<InputQueueItem>() {

            @Override
            public int compare(InputQueueItem o1, InputQueueItem o2) {
                return o1.getBarcode().compareTo(o2.getBarcode());
            }
        });
         inputQueueDAO.updateItems(list);
         return listTopLvl;
 
     }
 
     // private
 
     /**
      * Scan directory structure.
      * 
      * @param pathPrefix
      *        the path prefix
      * @param relativePath
      *        the relative path
      * @return the list
      */
     private List<InputQueueItem> scanDirectoryStructure(String pathPrefix, String relativePath) {
         ArrayList<InputQueueItem> inputQueueList = new ArrayList<InputQueueItem>();
         scanDirectoryStructure(pathPrefix, relativePath, inputQueueList, Constants.DIR_MAX_DEPTH);
         return inputQueueList;
     }
 
     /**
      * Scan directory structure.
      * 
      * @param pathPrefix
      *        the path prefix
      * @param relativePath
      *        the relative path
      * @param list
      *        the list
      * @param level
      *        the level
      * @return the list
      */
     private boolean scanDirectoryStructure(String pathPrefix,
                                            String relativePath,
                                            final List<InputQueueItem> list,
                                            int level) {
         if (level == 0) return false;
         File path = new File(pathPrefix + relativePath);
         FileFilter filter = new FileFilter() {
 
             @Override
             public boolean accept(File pathname) {
                 return !pathname.isFile();
             }
 
         };
         File[] dirs = path.listFiles(filter);
         boolean hasBeenIngested = dirs.length > 0;
         for (int i = 0; i < dirs.length; i++) {
             String rltvpth = relativePath + File.separator + dirs[i].getName();
 
             boolean lowerLevelIngested = scanDirectoryStructure(pathPrefix, rltvpth, list, level - 1);
             list.add(new InputQueueItem(rltvpth, dirs[i].getName(), hasBeenIngested(rltvpth)
                     || lowerLevelIngested));
 
             hasBeenIngested =
                     list.get(i > 0 ? list.size() - 2 : list.size() - 1).getIngestInfo()
                             && list.get(list.size() - 1).getIngestInfo() && hasBeenIngested;
         }
         return hasBeenIngested;
     }
 
     private boolean hasBeenIngested(String path) {
         File ingestInfoFile =
                 new File(configuration.getScanInputQueuePath() + path + "/" + Constants.INGEST_INFO_FILE_NAME);
         boolean fileExists = ingestInfoFile.exists();
         Element element = null;
         if (fileExists) {
             try {
                 FileInputStream fileStream = new FileInputStream(ingestInfoFile);
                 Document doc = XMLUtils.parseDocument(fileStream);
                 element =
                         XMLUtils.getElement(doc, "//" + Constants.NAME_ROOT_INGEST_ELEMENT + "[1]//"
                                 + Constants.NAME_INGEST_ELEMENT);
                 fileStream.close();
             } catch (FileNotFoundException e) {
                 fileExists = false;
             } catch (SAXException e) {
                 fileExists = false;
             } catch (IOException e) {
                 fileExists = false;
             } catch (ParserConfigurationException e) {
                 fileExists = false;
             } catch (XPathExpressionException e) {
                 fileExists = false;
             }
         }
         return fileExists && element != null;
     }
 
     /*
      * (non-Javadoc)
      * @see
      * com.gwtplatform.dispatch.server.actionhandler.ActionHandler#getActionType
      * ()
      */
     @Override
     public Class<ScanInputQueueAction> getActionType() {
         return ScanInputQueueAction.class;
     }
 
     /*
      * (non-Javadoc)
      * @see
      * com.gwtplatform.dispatch.server.actionhandler.ActionHandler#undo(com.
      * gwtplatform.dispatch.shared.Action,
      * com.gwtplatform.dispatch.shared.Result,
      * com.gwtplatform.dispatch.server.ExecutionContext)
      */
     @Override
     public void undo(ScanInputQueueAction action, ScanInputQueueResult result, ExecutionContext context)
             throws ActionException {
         // TODO Auto-generated method stub
 
     }
 }
