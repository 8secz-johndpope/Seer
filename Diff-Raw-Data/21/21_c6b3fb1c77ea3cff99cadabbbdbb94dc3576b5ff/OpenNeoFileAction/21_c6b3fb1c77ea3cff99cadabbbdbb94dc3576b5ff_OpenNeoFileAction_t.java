 package ca.neo.ui.actions;
 
 import java.io.File;
import java.io.FileInputStream;
 import java.io.IOException;
 
 import javax.swing.JFileChooser;
 import javax.swing.SwingUtilities;
 
import org.python.util.PythonInterpreter;
import org.python.util.PythonObjectInputStream;
 import ca.neo.model.Node;
 import ca.neo.ui.NengoGraphics;
 import ca.neo.ui.models.INodeContainer;
 import ca.shu.ui.lib.actions.ActionException;
 import ca.shu.ui.lib.actions.StandardAction;
 import ca.shu.ui.lib.objects.activities.TrackedAction;
 import ca.shu.ui.lib.util.UserMessages;
 
 /**
  * Action used to open a Neo model from file
  * 
  * @author Shu Wu
  */
 public class OpenNeoFileAction extends StandardAction {
 
 	private static final long serialVersionUID = 1L;
 	private File file;
 	private INodeContainer nodeContainer;
 	private Object objLoaded;
 
 	/**
 	 * @param actionName
 	 *            Name of this action
 	 * @param nodeContainer
 	 *            Container to which the loaded model shall be added to
 	 */
 	public OpenNeoFileAction(INodeContainer nodeContainer) {
 		super("Open from file");
 		init(nodeContainer);
 	}
 
 	@Override
 	protected void action() throws ActionException {
 		int response = NengoGraphics.FileChooser.showOpenDialog();
 		if (response == JFileChooser.APPROVE_OPTION) {
 			file = NengoGraphics.FileChooser.getSelectedFile();
 
 			TrackedAction loadActivity = new TrackedAction("Loading network") {
 				private static final long serialVersionUID = 1L;
 
 				@Override
 				protected void action() throws ActionException {
					try {										
						// loading Python-based objects requires using a
						//  PythonObjectInputStream from within a PythonInterpreter.						
						// loading sometimes fails if a new interpreter is created, so
						//  we use the one from the NengoGraphics.
						PythonInterpreter pi=NengoGraphics.getInstance().getPythonInterpreter();
						pi.set("___inStream", new PythonObjectInputStream(new FileInputStream(file)));
						org.python.core.PyObject obj=pi.eval("___inStream.readObject()");
						objLoaded=obj.__tojava__(Class.forName("ca.neo.model.Node"));					
						pi.exec("del ___inStream");
 
 						SwingUtilities.invokeLater(new Runnable() {
 							public void run() {
 								if (objLoaded != null)
 									processLoadedObject(objLoaded);
 								objLoaded = null;
 
 							}
 						});
 
 					} catch (IOException e) {
 						UserMessages.showError("IO Exception loading file");
 					} catch (ClassNotFoundException e) {
 						e.printStackTrace();
 						UserMessages.showError("Class not found");
 					} catch (ClassCastException e) {
 						UserMessages.showError("Incorrect file version");
 					} catch (OutOfMemoryError e) {
 						UserMessages.showError("Out of memory loading file");
 					} catch (Exception e) {
						e.printStackTrace();
 						UserMessages.showError("Unexpected exception loading file");
 					}
 
 		
 				}
 
 			};
 			loadActivity.doAction();
 		}
 
 	}
 
 	/**
 	 * Initializes field variables
 	 */
 	private void init(INodeContainer nodeContainer) {
 		this.nodeContainer = nodeContainer;
 	}
 
 	/**
 	 * Wraps the loaded object and adds it to the Node Container
 	 * 
 	 * @param objLoaded
 	 *            Loaded object
 	 */
 	private void processLoadedObject(Object objLoaded) {
 
 		if (objLoaded instanceof Node) {
 			nodeContainer.addNodeModel((Node) objLoaded);
 		} else {
 			UserMessages.showError("File does not contain a Node");
 		}
 
 	}
 }
