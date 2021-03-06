 /*
  * Copyright (C) 2010-2012 Klaus Reimer <k@ailis.de>
  * See LICENSE.TXT for licensing information.
  */
 
 package de.ailis.xadrian.components;
 
 import java.awt.BorderLayout;
 import java.awt.Color;
 import java.awt.Font;
 import java.awt.print.PrinterException;
 import java.io.File;
 import java.io.IOException;
 import java.net.URL;
 import java.util.HashMap;
 import java.util.Map;
 
 import javax.swing.JCheckBoxMenuItem;
 import javax.swing.JComponent;
 import javax.swing.JOptionPane;
 import javax.swing.JPopupMenu;
 import javax.swing.JScrollPane;
 import javax.swing.JTextPane;
 import javax.swing.TransferHandler;
 import javax.swing.UIManager;
 import javax.swing.event.CaretEvent;
 import javax.swing.event.CaretListener;
 import javax.swing.event.HyperlinkEvent;
 import javax.swing.event.HyperlinkEvent.EventType;
 import javax.swing.event.HyperlinkListener;
 import javax.swing.text.html.HTMLDocument;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.dom4j.Document;
 import org.dom4j.DocumentException;
 import org.dom4j.io.SAXReader;
 
 import de.ailis.xadrian.Main;
 import de.ailis.xadrian.actions.AddFactoryAction;
 import de.ailis.xadrian.actions.ChangePricesAction;
 import de.ailis.xadrian.actions.ChangeSectorAction;
 import de.ailis.xadrian.actions.ChangeSunsAction;
 import de.ailis.xadrian.actions.CopyAction;
 import de.ailis.xadrian.actions.SelectAllAction;
 import de.ailis.xadrian.actions.ToggleBaseComplexAction;
 import de.ailis.xadrian.data.Complex;
 import de.ailis.xadrian.data.Factory;
 import de.ailis.xadrian.data.Game;
 import de.ailis.xadrian.data.Sector;
 import de.ailis.xadrian.data.Ware;
 import de.ailis.xadrian.dialogs.AddFactoryDialog;
 import de.ailis.xadrian.dialogs.ChangePricesDialog;
 import de.ailis.xadrian.dialogs.ChangeQuantityDialog;
 import de.ailis.xadrian.dialogs.ChangeSunsDialog;
 import de.ailis.xadrian.dialogs.OpenComplexDialog;
 import de.ailis.xadrian.dialogs.SaveComplexDialog;
 import de.ailis.xadrian.dialogs.SelectSectorDialog;
 import de.ailis.xadrian.dialogs.SetYieldsDialog;
 import de.ailis.xadrian.freemarker.TemplateFactory;
 import de.ailis.xadrian.interfaces.ClipboardProvider;
 import de.ailis.xadrian.interfaces.ComplexProvider;
 import de.ailis.xadrian.interfaces.GameProvider;
 import de.ailis.xadrian.interfaces.SectorProvider;
 import de.ailis.xadrian.interfaces.StateProvider;
 import de.ailis.xadrian.listeners.ClipboardStateListener;
 import de.ailis.xadrian.listeners.EditorStateListener;
 import de.ailis.xadrian.listeners.StateListener;
 import de.ailis.xadrian.support.Config;
 import de.ailis.xadrian.support.I18N;
 import de.ailis.xadrian.support.ModalDialog.Result;
 import de.ailis.xadrian.utils.FileUtils;
 import de.ailis.xadrian.utils.SwingUtils;
 import de.ailis.xadrian.utils.XmlUtils;
 import freemarker.template.Template;
 
 /**
  * Complex Editor component.
  * 
  * @author Klaus Reimer (k@ailis.de)
  */
 public class ComplexEditor extends JComponent implements HyperlinkListener,
     CaretListener, ClipboardProvider, ComplexProvider, SectorProvider,
     GameProvider
 {
     /** Serial version UID */
     private static final long serialVersionUID = -582597303446091577L;
 
     /** The logger */
     private static final Log log = LogFactory.getLog(ComplexEditor.class);
 
     /** The freemarker template for the content */
     private static final Template template = TemplateFactory
         .getTemplate("complex.ftl");
 
     /** The text pane */
     private final JTextPane textPane;
 
     /** The edited complex */
     private final Complex complex;
 
     /** The file under which this complex was last saved */
     private File file;
 
     /** True if this editor has unsaved changes */
     private boolean changed = false;
 
     /**
      * Constructor
      * 
      * @param complex
      *            The complex to edit
      */
     public ComplexEditor(final Complex complex)
     {
         super();
         setLayout(new BorderLayout());
 
         this.complex = complex;
 
         // Create the text pane
         this.textPane = new JTextPane();
         this.textPane.setEditable(false);
         this.textPane.setBackground(Color.WHITE);
         this.textPane.setContentType("text/html");
         this.textPane.setDoubleBuffered(true);
         this.textPane.addHyperlinkListener(this);
         this.textPane.addCaretListener(this);
 
         // Create the popup menu for the text pane
         final JPopupMenu popupMenu = new JPopupMenu();
         popupMenu.add(new CopyAction(this));
         popupMenu.add(new SelectAllAction(this));
         popupMenu.addSeparator();
         popupMenu.add(new AddFactoryAction(this));
         popupMenu.add(new ChangeSectorAction(this.complex, this, "complex"));
         popupMenu.add(new ChangeSunsAction(this));
         popupMenu.add(new ChangePricesAction(this));
         popupMenu.add(new JCheckBoxMenuItem(new ToggleBaseComplexAction(this)));
         SwingUtils.setPopupMenu(this.textPane, popupMenu);
 
         final HTMLDocument document =
             (HTMLDocument) this.textPane.getDocument();
 
         // Set the base URL of the text pane
         document.setBase(Main.class.getResource("templates/"));
 
         // Modify the body style so it matches the system font
         final Font font = UIManager.getFont("Label.font");
         String bodyRule = "body { font-family: " + font.getFamily() +
             "; font-size: " + font.getSize() + "pt; }";
         document.getStyleSheet().addRule(bodyRule);
 
         // Create the scroll pane
         final JScrollPane scrollPane = new JScrollPane(this.textPane);
         add(scrollPane);
 
         // Redraw the content
         redraw();
 
         fireComplexState();
     }
 
     /**
      * Adds an editor state listener.
      * 
      * @param listener
      *            The editor state listener to add
      */
     public void addStateListener(final EditorStateListener listener)
     {
         this.listenerList.add(EditorStateListener.class, listener);
     }
 
     /**
      * Removes an editor state listener.
      * 
      * @param listener
      *            The editor state listener to remove
      */
     public void removeStateListener(final EditorStateListener listener)
     {
         this.listenerList.remove(EditorStateListener.class, listener);
     }
 
     /**
      * Fire the editor changed event.
      */
     private void fireState()
     {
         final Object[] listeners = this.listenerList.getListenerList();
         for (int i = listeners.length - 2; i >= 0; i -= 2)
             if (listeners[i] == EditorStateListener.class)
                 ((EditorStateListener) listeners[i + 1])
                     .editorStateChanged(this);
     }
 
     /**
      * Mark this editor as changed.
      */
     private void doChange()
     {
         this.changed = true;
         fireState();
         fireComplexState();
     }
 
     /**
      * Redraws the freemarker template.
      */
     private void redraw()
     {
         final int c = this.textPane.getCaretPosition();
         final Map<String, Object> model = new HashMap<String, Object>();
         final Config config = Config.getInstance();
         model.put("complex", this.complex);
         model.put("print", false);
         model.put("config", config);
         final String content = TemplateFactory.processTemplate(template, model);
         this.textPane.setText(content);
         this.textPane.setCaretPosition(Math.min(this.textPane.getDocument()
             .getLength() - 1, c));
         this.textPane.requestFocus();
     }
 
     /**
      * @see HyperlinkListener#hyperlinkUpdate(HyperlinkEvent)
      */
     @Override
     public void hyperlinkUpdate(final HyperlinkEvent e)
     {
         if (e.getEventType() != EventType.ACTIVATED) return;
 
         final URL url = e.getURL();
         final String protocol = url.getProtocol();
 
         if ("file".equals(protocol))
         {
             final String action = url.getHost();
             if ("addFactory".equals(action))
             {
                 addFactory();
             }
             else if ("removeFactory".equals(action))
             {
                 removeFactory(Integer.parseInt(url.getPath().substring(1)));
             }
             else if ("disableFactory".equals(action))
             {
                 disableFactory(Integer.parseInt(url.getPath().substring(1)));
             }
             else if ("enableFactory".equals(action))
             {
                 enableFactory(Integer.parseInt(url.getPath().substring(1)));
             }
             else if ("acceptFactory".equals(action))
             {
                 acceptFactory(Integer.parseInt(url.getPath().substring(1)));
             }
             else if ("changeQuantity".equals(action))
             {
                 changeQuantity(Integer.parseInt(url.getPath().substring(1)));
             }
             else if ("increaseQuantity".equals(action))
             {
                 increaseQuantity(Integer.parseInt(url.getPath().substring(1)));
             }
             else if ("decreaseQuantity".equals(action))
             {
                 decreaseQuantity(Integer.parseInt(url.getPath().substring(1)));
             }
             else if ("changeYield".equals(action))
             {
                 changeYield(Integer.parseInt(url.getPath().substring(1)));
             }
             else if ("changeSuns".equals(action))
             {
                 changeSuns();
             }
             else if ("changeSector".equals(action))
             {
                 changeSector();
             }
             else if ("changePrice".equals(action))
             {
                 changePrices(this.complex.getGame().getWareFactory().getWare(
                     url.getPath().substring(1)));
             }
             else if ("toggleShowingProductionStats".equals(action))
             {
                 toggleShowingProductionStats();
             }
             else if ("toggleShowingStorageCapacities".equals(action))
             {
                 toggleShowingStorageCapacities();
             }
             else if ("toggleShowingShoppingList".equals(action))
             {
                 toggleShowingShoppingList();
             }
             else if ("toggleShowingComplexSetup".equals(action))
             {
                 toggleShowingComplexSetup();
             }
             else if ("buildFactory".equals(action))
             {
                 buildFactory(url.getPath().substring(1));
             }
             else if ("destroyFactory".equals(action))
             {
                 destroyFactory(url.getPath().substring(1));
             }
             else if ("buildKit".equals(action))
             {
                 buildKit();
             }
             else if ("destroyKit".equals(action))
             {
                 destroyKit();
             }
         }
     }
 
     /**
      * Adds a new factory to the complex.
      */
     @Override
     public void addFactory()
     {
         final AddFactoryDialog dialog =
             this.complex.getGame().getAddFactoryDialog();
         if (dialog.open() == Result.OK)
         {
             for (final Factory factory : dialog.getFactories())
             {
                 this.complex.addFactory(factory);
             }
             doChange();
             redraw();
         }
     }
 
     /**
      * Sets the sector.
      */
     @Override
     public void changeSector()
     {
         final SelectSectorDialog dialog =
             this.complex.getGame().getSelectSectorDialog();
         dialog.setSelected(this.complex.getSector());
         if (dialog.open() == Result.OK)
         {
             this.complex.setSector(dialog.getSelected());
             doChange();
             redraw();
         }
     }
 
     /**
      * Toggles the display of the complex setup.
      */
     public void toggleShowingComplexSetup()
     {
         this.complex.toggleShowingComplexSetup();
         doChange();
         redraw();
     }
 
     /**
      * Builds the factory with the given id.
      * 
      * @param id
      *            The ID of the factory to build
      */
     public void buildFactory(final String id)
     {
         this.complex.buildFactory(id);
         doChange();
         redraw();
     }
 
     /**
      * Destroys the factory with the given id.
      * 
      * @param id
      *            The ID of the factory to destroy
      */
     public void destroyFactory(final String id)
     {
         this.complex.destroyFactory(id);
         doChange();
         redraw();
     }
 
     /**
      * Builds the factory with the given id.
      */
     public void buildKit()
     {
         this.complex.buildKit();
         doChange();
         redraw();
     }
 
     /**
      * Destroys a kit.
      */
     public void destroyKit()
     {
         this.complex.destroyKit();
         doChange();
         redraw();
     }
 
     /**
      * Toggles the display of production statistics.
      */
     public void toggleShowingProductionStats()
     {
         this.complex.toggleShowingProductionStats();
         doChange();
         redraw();
     }
 
     /**
      * Toggles the display of production statistics.
      */
     public void toggleShowingStorageCapacities()
     {
         this.complex.toggleShowingStorageCapacities();
         doChange();
         redraw();
     }
 
     /**
      * Toggles the display of the shopping list.
      */
     public void toggleShowingShoppingList()
     {
         this.complex.toggleShowingShoppingList();
         doChange();
         redraw();
     }
 
     /**
      * Removes the factory with the specified index.
      * 
      * @param index
      *            The index of the factory to remove
      */
     public void removeFactory(final int index)
     {
         this.complex.removeFactory(index);
         doChange();
         redraw();
     }
 
     /**
      * Disables the factory with the specified index.
      * 
      * @param index
      *            The index of the factory to disable
      */
     public void disableFactory(final int index)
     {
         this.complex.disableFactory(index);
         doChange();
         redraw();
     }
 
     /**
      * Enables the factory with the specified index.
      * 
      * @param index
      *            The index of the factory to enable
      */
     public void enableFactory(final int index)
     {
         this.complex.enableFactory(index);
         doChange();
         redraw();
     }
 
     /**
      * Accepts an automatically created factory.
      * 
      * @param index
      *            The index of the factory to accept
      */
     public void acceptFactory(final int index)
     {
         this.complex.acceptFactory(index);
         doChange();
         redraw();
     }
 
     /**
      * Changes the quantity of the factory with the specified index.
      * 
      * @param index
      *            The index of the factory to change
      */
     public void changeQuantity(final int index)
     {
         final ChangeQuantityDialog dialog = ChangeQuantityDialog.getInstance();
         dialog.setQuantity(this.complex.getQuantity(index));
         if (dialog.open() == Result.OK)
         {
             this.complex.setQuantity(index, dialog.getQuantity());
             doChange();
             redraw();
         }
     }
 
     /**
      * Increases the quantity of the factory with the specified index.
      * 
      * @param index
      *            The index of the factory to change
      */
     public void increaseQuantity(final int index)
     {
         if (this.complex.increaseQuantity(index))
         {
             doChange();
             redraw();
         }
     }
 
     /**
      * Decreases the quantity of the factory with the specified index.
      * 
      * @param index
      *            The index of the factory to change
      */
     public void decreaseQuantity(final int index)
     {
         if (this.complex.decreaseQuantity(index))
         {
             doChange();
             redraw();
         }
     }
 
     /**
      * Changes the yield of the factory with the specified index.
      * 
      * @param index
      *            The index of the factory to change
      */
     public void changeYield(final int index)
     {
         final Factory mineType = this.complex.getFactory(index);
         final SetYieldsDialog dialog = new SetYieldsDialog(mineType);
         dialog.setYields(this.complex.getYields(index));
         dialog.setSector(this.complex.getSector());
         if (dialog.open() == Result.OK)
         {
             this.complex.setYields(index, dialog.getYields());
             this.complex.setSector(dialog.getSector());
             doChange();
             redraw();
         }
     }
 
     /**
      * Changes the suns.
      */
     @Override
     public void changeSuns()
     {
         final ChangeSunsDialog dialog =
             this.complex.getGame().getChangeSunsDialog();
         dialog.setSuns(this.complex.getSuns());
         if (dialog.open() == Result.OK)
         {
             this.complex.setSuns(dialog.getSuns());
             doChange();
             redraw();
         }
     }
 
     /**
      * Saves the complex under the last saved file. If the file was not saved
      * before then saveAs() is called instead.
      */
     public void save()
     {
         if (this.file == null)
             saveAs();
         else
             save(this.file);
     }
 
     /**
      * Prompts for a file name and saves the complex there.
      */
     public void saveAs()
     {
         final SaveComplexDialog dialog = SaveComplexDialog.getInstance();
         dialog.setSelectedFile(getFile());
         File file = dialog.open();
         if (file != null)
         {
             // Add file extension if none present
             if (FileUtils.getExtension(file) == null)
                 file = new File(file.getPath() + ".x3c");
 
             // Save the file if it does not yet exists are user confirms
             // overwrite
             if (!file.exists()
                 || JOptionPane.showConfirmDialog(null, I18N
                     .getString("confirm.overwrite"), I18N
                     .getString("confirm.title"),
                     JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
             {
                 save(file);
             }
         }
     }
 
     /**
      * Prompts for a file name and opens a new complex from there. Returns the
      * complex editor or null if no file was loaded.
      * 
      * @return The new complex editor or null if file was not loaded
      */
     public static ComplexEditor open()
     {
         final OpenComplexDialog dialog = OpenComplexDialog.getInstance();
 
         dialog.setSelectedFile(null);
         final File file = dialog.open();
         if (file != null) open(file);
         return null;
     }
 
     /**
      * Reads a new complex from the specified file and returns the complex
     * editor or null if an error occured while reading the file.
      * 
      * @param file
      *            The file to open.
      * @return The new complex editor or null if file was not loaded.
      */
     public static ComplexEditor open(File file)
     {
         try
         {
             final SAXReader reader = new SAXReader();
             Document document = reader.read(file);
             final Complex complex = Complex.fromXML(document);
             complex.setName(FileUtils.getNameWithoutExt(file));
             final ComplexEditor editor = new ComplexEditor(complex);
             editor.file = file;
             return editor;
         }
         catch (final DocumentException e)
         {
             JOptionPane.showMessageDialog(null, I18N.getString(
                 "error.cantReadComplex", file, e.getMessage()), I18N
                 .getString("error.title"), JOptionPane.ERROR_MESSAGE);
             log.error("Unable to load complex from file '" + file + "': "
                 + e, e);
             return null;
         }
     }
 
     /**
      * Save the complex in the specified file.
      * 
      * @param file
      *            The file
      */
     private void save(final File file)
     {
         try
         {
             XmlUtils.write(this.complex.toXML(), file);
             this.file = file;
             this.changed = false;
             this.complex.setName(FileUtils.getNameWithoutExt(file));
             redraw();
             fireState();
             fireComplexState();
         }
         catch (final IOException e)
         {
             JOptionPane.showMessageDialog(null, I18N.getString(
                 "error.cantWriteComplex", file), I18N
                 .getString("error.title"), JOptionPane.ERROR_MESSAGE);
             log.error("Unable to save complex to file '" + file + "': " + e, e);
         }
     }
 
     /**
      * Returns the edited complex.
      * 
      * @return The edited complex
      */
     public Complex getComplex()
     {
         return this.complex;
     }
 
     /**
      * Returns true if this editor has unsaved changes. False if not.
      * 
      * @return True if this editor has unsaved changes. False if not.
      */
     public boolean isChanged()
     {
         return this.changed;
     }
 
     /**
      * Toggles the addition of automatically calculated base complex.
      */
     @Override
     public void toggleBaseComplex()
     {
         this.complex.toggleAddBaseComplex();
         doChange();
         redraw();
     }
 
     /**
      * Prints the complex data
      */
     public void print()
     {
         // Prepare model
         final Map<String, Object> model = new HashMap<String, Object>();
         model.put("complex", this.complex);
         model.put("print", true);
         model.put("config", Config.getInstance());
 
         // Generate content
         final String content = TemplateFactory.processTemplate(template, model);
 
         // Put content into a text pane component
         final JTextPane printPane = new JTextPane();
         printPane.setContentType("text/html");
         ((HTMLDocument) printPane.getDocument()).setBase(Main.class
             .getResource("templates/"));
         printPane.setText(content);
 
         // Print the text pane
         try
         {
             printPane.print();
         }
         catch (final PrinterException e)
         {
             JOptionPane.showMessageDialog(null, I18N
                 .getString("error.cantPrint"), I18N
                 .getString("error.title"), JOptionPane.ERROR_MESSAGE);
             log.error("Unable to print complex: " + e, e);
         }
     }
 
     /**
      * Returns true if this editor is new (and can be replaced with an other
      * editor).
      * 
      * @return True if editor is new
      */
     public boolean isNew()
     {
         return !this.changed && this.file == null
             && this.complex.getFactories().size() == 0;
     }
 
     /**
      * Updates the base complex
      */
     public void updateBaseComplex()
     {
         this.complex.updateBaseComplex();
         redraw();
     }
 
     /**
      * @see javax.swing.event.CaretListener#caretUpdate(javax.swing.event.CaretEvent)
      */
     @Override
     public void caretUpdate(final CaretEvent e)
     {
         fireClipboardState();
     }
 
     /**
      * Returns the selected text or null if none selected.
      * 
      * @return The selected text or null if none
      */
     public String getSelectedText()
     {
         return this.textPane.getSelectedText();
     }
 
     /**
      * Copies the selected text into the clipboard.
      */
     public void copySelection()
     {
         this.textPane.copy();
     }
 
     /**
      * Selects all the text in the text pane.
      */
     @Override
     public void selectAll()
     {
         this.textPane.requestFocus();
         this.textPane.selectAll();
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ClipboardProvider#canCopy()
      */
     @Override
     public boolean canCopy()
     {
         return this.textPane.getSelectedText() != null;
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ClipboardProvider#canCut()
      */
     @Override
     public boolean canCut()
     {
         return false;
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ClipboardProvider#canPaste()
      */
     @Override
     public boolean canPaste()
     {
         return false;
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ClipboardProvider#copy()
      */
     @Override
     public void copy()
     {
         this.textPane.requestFocus();
         this.textPane.copy();
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ClipboardProvider#cut()
      */
     @Override
     public void cut()
     {
         this.textPane.requestFocus();
         this.textPane.cut();
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ClipboardProvider#paste()
      */
     @Override
     public void paste()
     {
         this.textPane.requestFocus();
         this.textPane.paste();
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ClipboardProvider#addClipboardStateListener(de.ailis.xadrian.listeners.ClipboardStateListener)
      */
     @Override
     public void
         addClipboardStateListener(final ClipboardStateListener listener)
     {
         this.listenerList.add(ClipboardStateListener.class, listener);
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ClipboardProvider#removeClipboardStateListener(de.ailis.xadrian.listeners.ClipboardStateListener)
      */
     @Override
     public void removeClipboardStateListener(
         final ClipboardStateListener listener)
     {
         this.listenerList.remove(ClipboardStateListener.class, listener);
     }
 
     /**
      * Fire the clipboard state changed event.
      */
     private void fireClipboardState()
     {
         final Object[] listeners = this.listenerList.getListenerList();
         for (int i = listeners.length - 2; i >= 0; i -= 2)
             if (listeners[i] == ClipboardStateListener.class)
                 ((ClipboardStateListener) listeners[i + 1])
                     .clipboardStateChanged(this);
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ClipboardProvider#canSelectAll()
      */
     @Override
     public boolean canSelectAll()
     {
         return true;
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ComplexProvider#canAddFactory()
      */
     @Override
     public boolean canAddFactory()
     {
         return true;
     }
 
     /**
      * @see StateProvider#addStateListener(StateListener)
      */
     @Override
     public void addStateListener(final StateListener listener)
     {
         this.listenerList.add(StateListener.class, listener);
     }
 
     /**
      * @see StateProvider#removeStateListener(StateListener)
      */
     @Override
     public void removeStateListener(final StateListener listener)
     {
         this.listenerList.remove(StateListener.class, listener);
     }
 
     /**
      * Fire the complex state event.
      */
     private void fireComplexState()
     {
         final Object[] listeners = this.listenerList.getListenerList();
         for (int i = listeners.length - 2; i >= 0; i -= 2)
             if (listeners[i] == StateListener.class)
                 ((StateListener) listeners[i + 1]).stateChanged();
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ComplexProvider#canChangeSuns()
      */
     @Override
     public boolean canChangeSuns()
     {
         return this.complex.getSector() == null;
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ComplexProvider#canToggleBaseComplex()
      */
     @Override
     public boolean canToggleBaseComplex()
     {
         return true;
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ComplexProvider#isAddBaseComplex()
      */
     @Override
     public boolean isAddBaseComplex()
     {
         return this.complex.isAddBaseComplex();
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ComplexProvider#canChangeSector()
      */
     @Override
     public boolean canChangeSector()
     {
         return true;
     }
 
     /**
      * Returns the file under which the currently edited complex could be saved.
      * 
      * @return A suggested file name for saving.
      */
     private File getFile()
     {
         if (this.file != null) return this.file;
         return new File(this.complex.getName() + ".x3c");
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ComplexProvider#canChangePrices()
      */
     @Override
     public boolean canChangePrices()
     {
         return !this.complex.isEmpty();
     }
 
     /**
      * Opens the change prices dialog. Focuses the specified ware (if not null).
      * 
      * @param focusedWare
      *            The ware to focus (null for none)
      */
     public void changePrices(final Ware focusedWare)
     {
         final ChangePricesDialog dialog =
             this.complex.getGame().getChangePricesDialog();
         dialog.setCustomPrices(this.complex.getCustomPrices());
         dialog.setActiveWare(focusedWare);
         if (dialog.open(this.complex) == Result.OK)
         {
             this.complex.setCustomPrices(dialog.getCustomPrices());
             doChange();
             redraw();
         }
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.ComplexProvider#changePrices()
      */
     @Override
     public void changePrices()
     {
         changePrices(null);
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.SectorProvider#getSector()
      */
     @Override
     public Sector getSector()
     {
         return this.complex.getSector();
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.SectorProvider#setSector(de.ailis.xadrian.data.Sector)
      */
     @Override
     public void setSector(final Sector sector)
     {
         this.complex.setSector(sector);
         doChange();
         redraw();
     }
 
     /**
      * @see de.ailis.xadrian.interfaces.GameProvider#getGame()
      */
     @Override
     public Game getGame()
     {
         return this.complex.getGame();
     }
     
     /**
      * @see JComponent#setTransferHandler(TransferHandler)
      */
     @Override
     public void setTransferHandler(final TransferHandler transferHandler)
     {
         super.setTransferHandler(transferHandler);
         this.textPane.setTransferHandler(transferHandler);
     }
 }
