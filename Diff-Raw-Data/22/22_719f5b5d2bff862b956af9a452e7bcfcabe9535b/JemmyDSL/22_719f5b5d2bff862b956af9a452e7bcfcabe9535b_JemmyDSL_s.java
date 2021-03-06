 package com.github.srec.jemmy;
 
 import com.github.srec.UnsupportedFeatureException;
 import com.github.srec.util.AWTTreeScanner;
 import com.github.srec.util.ScannerMatcher;
 import com.github.srec.util.Utils;
 import org.apache.log4j.Logger;
 import org.netbeans.jemmy.*;
 import org.netbeans.jemmy.operators.*;
 import org.netbeans.jemmy.util.NameComponentChooser;
 
 import javax.swing.*;
 import javax.swing.table.JTableHeader;
 import javax.swing.text.JTextComponent;
 import java.awt.*;
 import java.awt.event.InputEvent;
 import java.awt.event.KeyEvent;
 import java.io.PrintStream;
 import java.lang.reflect.Constructor;
 import java.util.*;
 import java.util.List;
 
 import static org.apache.commons.lang.StringUtils.isBlank;
 
 /**
  * A DSL wrapper for Jemmy operators.
  *
  * @author Victor Tatai
  */
 public class JemmyDSL {
     private static final Logger logger = Logger.getLogger(JemmyDSL.class);
 
     public enum ComponentType {
         text_field(JTextFieldOperator.class, JTextField.class),
         combo_box(JComboBoxOperator.class, JComboBox.class),
         button(JButtonOperator.class, JButton.class),
         radio_button(JRadioButtonOperator.class, JRadioButton.class),
         check_box(JCheckBoxOperator.class, JCheckBox.class),
         table(JTableOperator.class, JTable.class),
         menu_bar(JMenuBarOperator.class, JMenuBar.class),
         dialog(JDialogOperator.class, JDialog.class);
 
         private Class<? extends ComponentOperator> operatorClass;
         private Class<? extends java.awt.Component> awtClass;
 
         ComponentType(Class<? extends ComponentOperator> operatorClass, Class<? extends java.awt.Component> awtClass) {
             this.operatorClass = operatorClass;
             this.awtClass = awtClass;
         }
 
         public Class<? extends ComponentOperator> getOperatorClass() {
             return operatorClass;
         }
 
         public Class<? extends java.awt.Component> getAwtClass() {
             return awtClass;
         }
     }
 
     private static Window currentWindow;
     private static Properties props = new Properties();
 
     static {
         props.put("ComponentOperator.WaitComponentEnabledTimeout", "5000");
         props.put("ComponentOperator.WaitComponentTimeout", "5000");
         props.put("ComponentOperator.WaitStateTimeout", "10000");
         props.put("DialogWaiter.WaitDialogTimeout", "10000");
         props.put("FrameWaiter.WaitFrameTimeout", "10000");
         props.put("JComboBoxOperator.WaitListTimeout", "30000");
         props.put("JScrollBarOperator.WholeScrollTimeout", "10000");
         props.put("JSliderOperator.WholeScrollTimeout", "10000");
         props.put("JSplitPaneOperator.WholeScrollTimeout", "10000");
         props.put("ScrollbarOperator.WholeScrollTimeout", "10000");
         props.put("Waiter.WaitingTime", "10000");
         props.put("WindowWaiter.WaitWindowTimeout", "10000");
     }
 
     private static List<java.awt.Container> ignored = new ArrayList<java.awt.Container>();
     private static ComponentMap componentMap = new DefaultComponentMap();
 
     public static void init(java.awt.Container... ignored) {
         Timeouts timeouts = JemmyProperties.getCurrentTimeouts();
         for (Map.Entry<Object, Object> entry : props.entrySet()) {
             timeouts.setTimeout((String) entry.getKey(), Long.parseLong((String) entry.getValue()));
         }
         currentWindow = null;
         JemmyDSL.ignored = Arrays.asList(ignored);
         JemmyProperties.setCurrentOutput(new TestOut(System.in, (PrintStream) null, null));
         robotMode();
     }
 
     public static void robotMode() {
         JemmyProperties.setCurrentDispatchingModel(JemmyProperties.ROBOT_MODEL_MASK);
     }
 
     public static void dispatchingMode() {
         JemmyProperties.setCurrentDispatchingModel(JemmyProperties.QUEUE_MODEL_MASK);
     }
 
     public static boolean isRobotMode() {
         return JemmyProperties.getCurrentDispatchingModel() == JemmyProperties.ROBOT_MODEL_MASK;
     }
 
     @SuppressWarnings({"UnusedDeclaration"})
     public static ComponentMap getComponentMap() {
         return componentMap;
     }
 
     public static void setComponentMap(ComponentMap componentMap) {
         JemmyDSL.componentMap = componentMap;
     }
 
     public static Frame frame(String title) {
         Frame frame = new Frame(title);
         currentWindow = frame;
         return frame;
     }
 
     public static Frame frame() {
         return (Frame) currentWindow;
     }
 
     public static Window currentWindow() {
         if (currentWindow == null) {
             logger.info("No current container found, trying to find one.");
             currentWindow = findActiveWindow();
         } else if (!currentWindow.isActive()) {
             currentWindow = findActiveWindow();
         }
         if (currentWindow == null) throw new JemmyDSLException("Cannot find a currently active window");
         logger.info("Using as current container: " + currentWindow.getComponent());
         return currentWindow;
     }
 
     private static Window findActiveWindow() {
         java.awt.Window[] windows = JFrame.getWindows();
         for (java.awt.Window w : windows) {
             if (ignored.contains(w)) continue;
             if (!w.isActive()) continue;
             if (w instanceof JFrame) {
                 return new Frame((JFrame) w);
             } else if (w instanceof JDialog) {
                 return new Dialog((JDialog) w);
             } else {
                 logger.info("Found a window which is neither a JFrame nor JDialog");
             }
         }
         return null;
     }
 
     public static Container container(JFrame frame) {
         currentWindow = new Frame(frame);
         return currentWindow;
     }
 
     public static Dialog dialog(String title) {
         final Dialog dialog = new Dialog(title);
         currentWindow = dialog;
         return dialog;
     }
 
     public static TextField textField(String locator) {
         return new TextField(locator);
     }
 
     public static TextArea textArea(String locator) {
         return new TextArea(locator);
     }
 
     public static Button button(String locator) {
         return new Button(locator);
     }
 
     public static ComboBox comboBox(String locator) {
         return new ComboBox(locator);
     }
 
     public static CheckBox checkBox(String locator) {
         return new CheckBox(locator);
     }
 
     public static Table table(String locator) {
         return new Table(locator);
     }
 
     /**
      * Finds a component and stores it under the given id. The component can later be used on other commands using the
      * locator "id=ID_ASSIGNED". This method searches both VISIBLE and INVISIBLE components.
      *
      * @param locator The locator (accepted are name (default), title, text, label)
      * @param id The id
      * @param componentType The component type
      * @return The component found
      */
     @SuppressWarnings({"unchecked"})
     public static Component find(String locator, String id, String componentType) {
         java.awt.Component component = findComponent(locator, currentWindow().getComponent().getSource());
         if (component == null) {
             componentMap.putComponent(id, null);
             return null;
         }
         JComponentOperator operator = convertFind(component);
         componentMap.putComponent(id, operator);
         final Component finalComponent = convertFind(operator);
         if (finalComponent instanceof Window) {
             currentWindow = (Window) finalComponent;
         }
         return finalComponent;
     }
 
     @SuppressWarnings({"unchecked"})
     private static Class<? extends java.awt.Component> translateFindType(String findType) {
         for (ComponentType componentType : ComponentType.values()) {
             if (findType.equals(componentType.name())) return componentType.getAwtClass();
         }
         try {
             return (Class<? extends java.awt.Component>) Class.forName(findType);
         } catch (ClassNotFoundException e) {
             throw new JemmyDSLException("Unsupported find type " + findType);
         }
     }
 
     private static java.awt.Component findComponent(String locator, java.awt.Component component) {
         assert locator != null;
         String[] strs = parseLocator(locator);
         if (strs.length != 2) throw new JemmyDSLException("Invalid locator " + locator);
         if (strs[0].equals("id")) {
             return componentMap.getComponent(strs[1]).getSource();
         } else {
             return AWTTreeScanner.scan(component, compileMatcher(strs));
         }
     }
 
     private static String[] parseLocator(String locator) {
         int i = locator.indexOf("=");
         if (i == -1) {
             return new String[] { "name", locator.substring(i + 1).trim()};
         }
         return new String[] { locator.substring(0, i).trim(), locator.substring(i + 1).trim()};
     }
 
     private static ScannerMatcher compileMatcher(String[] strs) {
         if (strs[0].equals("name")) return new AWTTreeScanner.NameScannerMatcher(strs[1]);
         if (strs[0].equals("title")) return new AWTTreeScanner.TitleScannerMatcher(strs[1]);
         if (strs[0].equals("text")) return new AWTTreeScanner.TextScannerMatcher(strs[1]);
         throw new JemmyDSLException("Invalid locator " + strs[0] + "=" + strs[1]);
     }
 
     private static java.awt.Component findComponent(java.awt.Container container, Class<? extends java.awt.Component> componentClass) {
         for (java.awt.Component component : container.getComponents()) {
             if (componentClass.isAssignableFrom(component.getClass())) {
                 return component;
             }
             if (component instanceof java.awt.Container) {
                 java.awt.Component comp = findComponent((java.awt.Container) component, componentClass);
                 if (comp != null) return comp;
             }
         }
         return null;
     }
 
     private static JComponentOperator convertFind(java.awt.Component comp) {
         if (comp instanceof JComboBox) return new JComboBoxOperator((JComboBox) comp);
         if (comp instanceof JTextComponent) return new JTextFieldOperator((JTextField) comp);
         if (comp instanceof JCheckBox) return new JCheckBoxOperator((JCheckBox) comp);
         if (comp instanceof JRadioButton) return new JRadioButtonOperator((JRadioButton) comp);
         if (comp instanceof JButton) return new JButtonOperator((JButton) comp);
         if (comp instanceof AbstractButton) return new AbstractButtonOperator((AbstractButton) comp);
         if (comp instanceof JTable) return new JTableOperator((JTable) comp);
         if (comp instanceof JMenuBar) return new JMenuBarOperator((JMenuBar) comp);
         if (comp instanceof JScrollBar) return new JScrollBarOperator((JScrollBar) comp);
         throw new JemmyDSLException("Unsupported find type " + comp);
     }
 
     private static Component convertFind(JComponentOperator comp) {
         if (comp instanceof JComboBoxOperator) return new ComboBox((JComboBoxOperator) comp);
         if (comp instanceof JTextComponentOperator) return new TextField((JTextFieldOperator) comp);
         if (comp instanceof JCheckBoxOperator) return new CheckBox((JCheckBoxOperator) comp);
         if (comp instanceof JRadioButtonOperator) return new RadioButton((JRadioButtonOperator) comp);
         if (comp instanceof JButtonOperator) return new Button((JButtonOperator) comp);
         if (comp instanceof AbstractButtonOperator) return new GenericButton((AbstractButtonOperator) comp);
         if (comp instanceof JTableOperator) return new Table((JTableOperator) comp);
         if (comp instanceof JMenuBarOperator) return new MenuBar((JMenuBarOperator) comp);
         if (comp instanceof JScrollBarOperator) return new ScrollBar((JScrollBarOperator) comp);
         throw new JemmyDSLException("Unsupported find type " + comp);
     }
 
     /**
      * Finds the first component with the given component type and stores it under the given id. The component can later
      * be used on other commands using the locator "id=ID_ASSIGNED". This method searches both VISIBLE and INVISIBLE
      * components.
      *
      * @param id The id
      * @param componentType The component type
      * @return The component found
      */
     @SuppressWarnings({"unchecked"})
     public static Component findByComponentType(String id, String containerId, String componentType) {
         java.awt.Container container;
         if (isBlank(containerId)) {
             container = (java.awt.Container) currentWindow().getComponent().getSource();
         } else {
             ComponentOperator op = componentMap.getComponent(containerId);
             if (op != null && op.getSource() instanceof java.awt.Container) {
                 container = (java.awt.Container) op.getSource();
             } else {
                 container = (java.awt.Container) currentWindow().getComponent().getSource();
             }
         }
         java.awt.Component component = findComponent(container, translateFindType(componentType));
         if (component == null) {
             componentMap.putComponent(id, null);
             return null;
         }
         JComponentOperator operator = convertFind(component);
         componentMap.putComponent(id, operator);
         return convertFind(operator);
     }
 
     public static void click(String locator, int count, String modifiers) {
         final JComponentOperator operator = find(locator, JComponentOperator.class);
         if (operator == null) throw new JemmyDSLException("Could not find component for clicking " + locator);
         operator.clickMouse(operator.getCenterXForClick(), operator.getCenterYForClick(), count, InputEvent.BUTTON1_MASK,
                 convertModifiers(modifiers));
     }
 
     private static int convertModifiers(String modifiers) {
         if (isBlank(modifiers)) return 0;
         String[] mods = modifiers.split("[ |\\+|,]+");
         int flags = 0;
         for (String mod : mods) {
             if ("Shift".equalsIgnoreCase(mod)) {
                 flags |= InputEvent.SHIFT_MASK;
             } else if ("Control".equalsIgnoreCase(mod) || "Ctrl".equalsIgnoreCase(mod)) {
                 flags |= InputEvent.CTRL_MASK;
             } else if ("Alt".equalsIgnoreCase(mod)) {
                 flags |= InputEvent.ALT_MASK;
             } else {
                 throw new JemmyDSLException("Unknown modifier " + mod);
             }
         }
         return flags;
     }
 
     @SuppressWarnings({"unchecked"})
     public static <X extends JComponentOperator> X find(String locator, Class<X> clazz) {
         Map<String, String> locatorMap = Utils.parseLocator(locator);
         X component;
         if (locatorMap.containsKey("name")) {
             component = newInstance(clazz, currentWindow().getComponent(), new NameComponentChooser(locator));
         } else if (locatorMap.containsKey("label")) {
             JLabelOperator jlabel = new JLabelOperator(currentWindow().getComponent(), locatorMap.get("label"));
             if (!(jlabel.getLabelFor() instanceof JTextField)) {
                 throw new JemmyDSLException("Associated component for " + locator + " is not a JTextComponent");
             }
             component = newInstance(clazz, JTextField.class, (JTextField) jlabel.getLabelFor());
         } else if (locatorMap.containsKey("text")) {
             if (JTextComponentOperator.class.isAssignableFrom(clazz)) {
                 component = newInstance(clazz, currentWindow().getComponent(), new JTextComponentOperator.JTextComponentByTextFinder(locatorMap.get("text")));
             } else if (AbstractButtonOperator.class.isAssignableFrom(clazz)) {
                 component = newInstance(clazz, currentWindow().getComponent(), new AbstractButtonOperator.AbstractButtonByLabelFinder(locatorMap.get("text")));
             } else if (JComponentOperator.class.isAssignableFrom(clazz)) {
                 // Hack, we assume that what was really meant was AbstractButtonOperator
                 component = newInstance(clazz, currentWindow().getComponent(), new AbstractButtonOperator.AbstractButtonByLabelFinder(locatorMap.get("text")));
             } else {
                 throw new JemmyDSLException("Unsupported component type for location by text locator: " + locator);
             }
         } else if (locatorMap.containsKey("id")) {
             ComponentOperator operator = componentMap.getComponent(locatorMap.get("id"));
             if (operator == null) return null;
             if (!clazz.isAssignableFrom(operator.getClass())) {
                 throw new JemmyDSLException("Cannot convert component with " + locator + " from "
                         + operator.getClass().getName() + " to " + clazz.getName());
             }
             component = (X) operator;
         } else if (locatorMap.containsKey("title")) {
             if (JInternalFrameOperator.class.isAssignableFrom(clazz)) {
                 component = newInstance(clazz, currentWindow().getComponent(), new JInternalFrameOperator.JInternalFrameByTitleFinder(locatorMap.get("title")));
             } else {
                 throw new JemmyDSLException("Unsupported component type for location by text locator: " + locator);
             }
         } else {
             throw new JemmyDSLException("Unsupported locator: " + locator);
         }
         return component;
     }
 
     private static <X extends JComponentOperator> X newInstance(Class<X> clazz, ContainerOperator parent,
                                                                 ComponentChooser chooser) {
         try {
             Constructor<X> c = clazz.getConstructor(ContainerOperator.class, ComponentChooser.class);
             return c.newInstance(parent, chooser);
         } catch (Exception e) {
             // Check to see if the nested exception was caused by a regular Jemmy exception
             if (e.getCause() != null && e.getCause() instanceof JemmyException) throw (JemmyException) e.getCause();
             throw new JemmyDSLException(e);
         }
     }
 
     private static <X extends JComponentOperator, Y> X newInstance(Class<X> clazz, Class<Y> componentClass, JComponent component) {
         try {
             Constructor<X> c = findConstructor(clazz, componentClass);
             return c.newInstance(component);
         } catch (Exception e) {
             // Check to see if the nested exception was caused by a regular Jemmy exception
             if (e.getCause() != null && e.getCause() instanceof JemmyException) throw (JemmyException) e.getCause();
             throw new JemmyDSLException(e);
         }
     }
 
     @SuppressWarnings({"unchecked"})
     private static <X, Y> Constructor<X> findConstructor(Class<X> clazz, Class<Y> componentClass) {
         Constructor<X>[] cs = (Constructor<X>[]) clazz.getConstructors();
         for (Constructor<X> c : cs) {
             final Class<?>[] types = c.getParameterTypes();
             if (types.length == 1 && types[0].isAssignableFrom(componentClass)) return c;
         }
         throw new JemmyDSLException("Could not find suitable constructor in class " + clazz.getCanonicalName());
     }
 
     public static JComponent getSwingComponentById(String id) {
         ComponentOperator op = componentMap.getComponent(id);
         return (JComponent) op.getSource();
     }
 
     public static Label label(String locator) {
         return new Label(find(locator, JLabelOperator.class));
     }
 
     public static TabbedPane tabbedPane(String locator) {
         return new TabbedPane(locator);
     }
 
     public static Slider slider(String locator) {
         return new Slider(locator);
     }
 
     public static InternalFrame internalFrame(String locator) {
         return new InternalFrame(locator);
     }
 
     /**
      * Gets the menu bar for the last activated frame.
      *
      * @return The menu bar
      */
     public static MenuBar menuBar() {
         return new MenuBar();
     }
 
     public static void waitEnabled(String locator, boolean enabled) {
         JComponentOperator op = find(locator, JComponentOperator.class);
         try {
             if (enabled) {
                 op.waitComponentEnabled();
             } else {
                 waitComponentDisabled(op);
             }
         } catch (InterruptedException e) {
             throw new JemmyDSLException(e);
         }
     }
 
     private static void waitComponentDisabled(final ComponentOperator op) throws InterruptedException {
         Waiter waiter = new Waiter(new Waitable() {
             public Object actionProduced(Object obj) {
                 if (((java.awt.Component) obj).isEnabled()) {
                     return null;
                 } else {
                     return obj;
                 }
             }
 
             public String getDescription() {
                 return ("Component description: " + op.getSource().getClass().toString());
             }
         });
         waiter.setOutput(op.getOutput());
         waiter.setTimeoutsToCloneOf(op.getTimeouts(), "ComponentOperator.WaitComponentEnabledTimeout");
         waiter.waitAction(op.getSource());
     }
 
     public static abstract class Component {
         public abstract ComponentOperator getComponent();
 
         public void assertEnabled() {
             try {
                 getComponent().waitComponentEnabled();
             } catch (InterruptedException e) {
                 throw new JemmyDSLException(e);
             }
         }
 
         public void assertDisabled() {
             try {
                 waitComponentDisabled(getComponent());
             } catch (InterruptedException e) {
                 throw new JemmyDSLException(e);
             }
         }
 
         public void store(String id) {
             componentMap.putComponent(id, getComponent());
         }
     }
 
     public static abstract class Container extends Component {
         public abstract ContainerOperator getComponent();
     }
 
     public static abstract class Window extends Container {
         public abstract boolean isActive();
         public abstract boolean isShowing();
     }
 
     public static class Frame extends Window {
         private JFrameOperator component;
 
         public Frame(String title) {
             component = new JFrameOperator(title);
         }
 
         public Frame(JFrame frame) {
             component = new JFrameOperator(frame);
         }
 
         public Frame close() {
             component.requestClose();
             return this;
         }
 
         public Frame activate() {
             component.activate();
             currentWindow = this;
             return this;
         }
 
         @Override
         public boolean isActive() {
             return component.isActive();
         }
 
         @Override
         public boolean isShowing() {
             return component.isShowing();
         }
 
         public JFrameOperator getComponent() {
             return component;
         }
     }
 
     public static class Dialog extends Window {
         private JDialogOperator component;
 
         public Dialog(String title) {
             component = new JDialogOperator(title);
         }
 
         public Dialog(JDialog dialog) {
             component = new JDialogOperator(dialog);
         }
 
         public Dialog close() {
             component.requestClose();
             return this;
         }
 
         public JDialogOperator getComponent() {
             return component;
         }
 
         public Dialog activate() {
             component.activate();
             currentWindow = this;
             return this;
         }
 
         @Override
         public boolean isShowing() {
             return component.isShowing();
         }
 
         @Override
         public boolean isActive() {
             return component.isActive();
         }
     }
 
     public static class TextField extends Component {
         private JTextComponentOperator component;
 
         public TextField(String locator) {
             component = find(locator, JTextComponentOperator.class);
             component.setComparator(new Operator.DefaultStringComparator(true, true));
         }
 
         public TextField(JTextFieldOperator component) {
             this.component = component;
             component.setComparator(new Operator.DefaultStringComparator(true, true));
         }
 
         public TextField type(String text) {
            //TODO Remove this wait, just trying to avoid timeout
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
             if (text.contains("\t") || text.contains("\r") || text.contains("\n")) {
                 throw new IllegalParametersException("Text cannot contain \\t \\r \\n");
             }
             // TODO: find a better way to guarantee focus on the target typing component
             // The solution proposed here tries to guarantee that the textField has the focus
             // to make the test as closes as the human interactions as possible.
             component.requestFocus();            
             component.setVerification(false);
             component.typeText(text);
             return this;
         }
 
         public TextField type(char key) {
             component.typeKey(key);
             if (!isRobotMode()) {
                 // This is a hack because type key in queue mode does not wait for events to be fired
                 try {
                     Thread.sleep(2000);
                 } catch (InterruptedException e) {
                     throw new JemmyDSLException(e);
                 }
             }
             return this;
         }
 
         public TextField type(int key) {
             component.pushKey(key);
             return this;
         }
 
         public TextField typeSpecial(String keyString) {
            //TODO Remove this wait, just trying to avoid timeout
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

             int key;
             if ("Tab".equalsIgnoreCase(keyString)) key = KeyEvent.VK_TAB;
             else if ("Enter".equalsIgnoreCase(keyString)) key = KeyEvent.VK_ENTER;
             else if ("End".equalsIgnoreCase(keyString)) key = KeyEvent.VK_END;
             else if ("Backspace".equalsIgnoreCase(keyString)) key = KeyEvent.VK_BACK_SPACE;
             else if ("Delete".equalsIgnoreCase(keyString)) key = KeyEvent.VK_DELETE;
             else throw new UnsupportedFeatureException("Type special for " + keyString + " not supported");
             // TODO: find a better way to guarantee focus on the target typing component
             // The solution proposed here tries to guarantee that the textField has the focus
             // to make the test as closes as the human interactions as possible.
             component.requestFocus();
             type(key);
             return this;
         }
 
         public String text() {
             return component.getText();
         }
 
         public void assertText(String text) {
             component.waitText(text);
         }
 
         public JTextComponentOperator getComponent() {
             return component;
         }
 
         public TextField assertEmpty() {
             component.waitText("");
             return this;
         }
 
         public TextField clickCharPosition(int pos, String modifiers) {
             FontMetrics fm = component.getFontMetrics(component.getFont());
             component.clickMouse(fm.stringWidth(component.getText().substring(0, pos)) + component.getInsets().left,
                     component.getCenterYForClick(), 1, KeyEvent.BUTTON1_MASK, convertModifiers(modifiers));
             return this;
         }
     }
 
     public static class TextArea extends Component {
         private JTextAreaOperator component;
 
         public TextArea(String locator) {
             component = find(locator, JTextAreaOperator.class);
         }
 
         public TextArea(JTextAreaOperator component) {
             this.component = component;
         }
 
         public TextArea type(String text) {
             if (text.contains("\t") || text.contains("\r") || text.contains("\n")) {
                 throw new IllegalParametersException("Text cannot contain \\t \\r \\n");
             }
             component.setVerification(false);
             component.typeText(text);
             return this;
         }
 
         public String text() {
             return component.getText();
         }
 
         public JTextAreaOperator getComponent() {
             return component;
         }
 
         public TextArea assertEmpty() {
             component.waitText("");
             return this;
         }
     }
 
     public static class ComboBox extends Component {
         private JComboBoxOperator component;
 
         public ComboBox(String locator) {
             component = find(locator, JComboBoxOperator.class);
         }
 
         public ComboBox(JComboBoxOperator comp) {
             this.component = comp;
         }
 
         public void select(String text) {
             Map<String, String> selectedItem = Utils.parseLocator(text);
             if (selectedItem.containsKey("name")) {
                 clickDropDown();
                 component.selectItem(selectedItem.get("name"));
             } else if (selectedItem.containsKey("index")) {
                 select(Integer.parseInt(selectedItem.get("index")));
             } else {
                 throw new IllegalParametersException("Illegal parameters " + text + " for select command");
             }
         }
 
         public void select(int index) {
 
             // hack:begin
             // TODO: find a better way to avoid timeouts when this method is invoked twice in a row
             // The solution proposed here may not work in all cases because changing the focus to the next
             // component may trigger other undesired event handlers
             if (component.hasFocus()) {component.transferFocus();}
             // hack:end
 
             clickDropDown();
             component.setSelectedIndex(index);
             component.waitItemSelected(index);
         }
 
         private void clickDropDown() {
             component.pushComboButton();
             component.waitList();
         }
 
         public JComboBoxOperator getComponent() {
             return component;
         }
 
         public void assertSelected(String text) {
             component.waitItemSelected(text);
         }
     }
 
     public static class GenericButton extends Component {
         protected AbstractButtonOperator component;
 
         protected GenericButton() {}
 
         public GenericButton(String locator) {
             component = find(locator, AbstractButtonOperator.class);
         }
 
         public GenericButton(AbstractButtonOperator component) {
             this.component = component;
         }
 
         public void click() {
             component.push();
         }
         
         @Override
         public AbstractButtonOperator getComponent() {
             return component;
         }
     }
 
     public static class Button extends GenericButton {
         public Button(String locator) {
             component = find(locator, JButtonOperator.class);
         }
 
         public Button(JButtonOperator component) {
             super(component);
         }
 
         @Override
         public JButtonOperator getComponent() {
             return (JButtonOperator) component;
         }
     }
 
     public static class CheckBox extends GenericButton {
         public CheckBox(String locator) {
             component = find(locator, JCheckBoxOperator.class);
         }
 
         public CheckBox(JCheckBoxOperator component) {
             super(component);
         }
 
         @Override
         public JCheckBoxOperator getComponent() {
             return (JCheckBoxOperator) component;
         }
     }
 
     public static class RadioButton extends GenericButton {
         public RadioButton(String locator) {
             component = find(locator, JRadioButtonOperator.class);
         }
 
         public RadioButton(JRadioButtonOperator component) {
             super(component);
         }
 
         @Override
         public JRadioButtonOperator getComponent() {
             return (JRadioButtonOperator) component;
         }
     }
 
     public static class Table extends Component {
         private JTableOperator component;
 
         public Table(String locator) {
             component = find(locator, JTableOperator.class);
         }
 
         public Table(JTableOperator component) {
             this.component = component;
         }
 
         public Row row(int index) {
             return new Row(component, index);
         }
 
         public TableHeader header() {
             return new TableHeader(component.getTableHeader());
         }
 
         public Table selectRows(int first, int last) {
             component.setRowSelectionInterval(first, last);
             return this;
         }
 
         public JTableOperator getComponent() {
             return component;
         }
     }
 
     public static class Row {
         private JTableOperator component;
         private int index;
 
         public Row(JTableOperator component, int index) {
             this.component = component;
             this.index = index;
         }
 
         public Row assertColumn(int col, String value) {
             component.waitCell(value, index, col);
             return this;
         }
 
         public Row select() {
             component.setRowSelectionInterval(index, index);
             return this;
         }
 
         public Row assertSelected(final boolean selected) {
             component.waitState(new ComponentChooser() {
                 @Override
                 public boolean checkComponent(java.awt.Component comp) {
                     return ((JTable) comp).isRowSelected(index) == selected;
                 }
 
                 @Override
                 public String getDescription() {
                     return null;
                 }
             });
             return this;
         }
 
         public Row selectCell(int col) {
             component.selectCell(index, col);
             return this;
         }
 
         public Row clickCell(int col, int clicks) {
             component.clickOnCell(index, col, clicks);
             return this;
         }
     }
 
     public static class TableHeader {
         private JTableHeaderOperator component;
 
         public TableHeader(JTableHeader swingComponent) {
             component = new JTableHeaderOperator(swingComponent);
         }
 
         public TableHeader assertTitle(final int col, final String title) {
             component.waitState(new ComponentChooser() {
                 @Override
                 public boolean checkComponent(java.awt.Component comp) {
                     return ((JTableHeader) comp).getColumnModel().getColumn(col).getHeaderValue().equals(title);
                 }
 
                 @Override
                 public String getDescription() {
                     return null;
                 }
             });
             return this;
         }
     }
 
     public static class Label {
         private JLabelOperator component;
 
         public Label(JLabelOperator component) {
             this.component = component;
         }
 
         public Label text(String text) {
             component.waitText(text);
             return this;
         }
     }
 
     public static class TabbedPane extends Component {
         private JTabbedPaneOperator component;
 
         public TabbedPane(String locator) {
             component = find(locator, JTabbedPaneOperator.class);
         }
 
         public TabbedPane select(String title) {
             component.selectPage(title);
             return this;
         }
 
         public JTabbedPaneOperator getComponent() {
             return component;
         }
     }
 
     public static class Slider extends Component {
         private JSliderOperator component;
 
         public Slider(String locator) {
             component = find(locator, JSliderOperator.class);
         }
 
         public Slider value(int i) {
             component.setValue(i);
             return this;
         }
 
         public Slider assertValue(final int i) {
             component.waitState(new ComponentChooser() {
                 @Override
                 public boolean checkComponent(java.awt.Component comp) {
                     return ((JSlider) comp).getValue() == i;
                 }
 
                 @Override
                 public String getDescription() {
                     return null;
                 }
             });
             return this;
         }
 
         @Override
         public ComponentOperator getComponent() {
             return component;
         }
     }
 
     public static class MenuBar extends Container {
         private JMenuBarOperator component;
 
         public MenuBar() {
             component = new JMenuBarOperator(currentWindow().getComponent());
         }
 
         public MenuBar(JMenuBarOperator component) {
             this.component = component;
         }
 
         public MenuBar clickMenu(int... indexes) {
             if (indexes.length == 0) return this;
             String[] texts = new String[indexes.length];
             JMenu menu = component.getMenu(indexes[0]);
             texts[0] = menu.getText();
             for (int i = 1; i < indexes.length; i++) {
                 int index = indexes[i];
                 assert menu != null;
                 if (i == indexes.length - 1) {
                     JMenuItem item = (JMenuItem) menu.getMenuComponent(index);
                     texts[i] = item.getText();
                     menu = null;
                 } else {
                     menu = (JMenu) menu.getMenuComponent(index);
                     texts[i] = menu.getText();
                 }
             }
             clickMenu(texts);
             return this;
         }
 
         public MenuBar clickMenu(String... texts) {
             if (texts.length == 0) return this;
             component.showMenuItem(texts[0]);
             for (int i = 1; i < texts.length; i++) {
                 String text = texts[i];
                 new JMenuOperator(currentWindow().getComponent(), texts[i - 1]).showMenuItem(new String[] {text});
             }
             new JMenuItemOperator(currentWindow().getComponent(), texts[texts.length - 1]).clickMouse();
             return this;
         }
 
         @Override
         public JMenuBarOperator getComponent() {
             return component;
         }
 
     }
 
     public static class Menu extends Container {
         private JMenuOperator component;
 
         public Menu() {
         }
 
         @Override
         public JMenuOperator getComponent() {
             return component;
         }
     }
 
     public static class InternalFrame extends Container {
         private JInternalFrameOperator component;
 
         public InternalFrame(String locator) {
             component = find(locator, JInternalFrameOperator.class);
         }
 
         public InternalFrame(JInternalFrame frame) {
             component = new JInternalFrameOperator(frame);
         }
 
         public InternalFrame close() {
             ((JInternalFrame) component.getSource()).doDefaultCloseAction();
             return this;
         }
 
         public InternalFrame hide() {
             component.setVisible(false);
             return this;
         }
 
         public InternalFrame show() {
             component.setVisible(true);
             return this;
         }
 
         public InternalFrame activate() {
             component.activate();
             return this;
         }
 
         public InternalFrame assertVisible(Boolean visible) {
             component.waitComponentVisible(visible);
             return this;
         }
 
         public JInternalFrameOperator getComponent() {
             return component;
         }
     }
 
     public static class ScrollBar extends Component {
         private JScrollBarOperator component;
 
         public ScrollBar(String locator) {
             component = find(locator, JScrollBarOperator.class);
         }
 
         public ScrollBar(JScrollBarOperator component) {
             this.component = component;
         }
 
         @Override
         public JScrollBarOperator getComponent() {
             return (JScrollBarOperator) component;
         }
     }
 }
