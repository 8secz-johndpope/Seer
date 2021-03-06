 package de.lessvoid.nifty;
 
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Hashtable;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Properties;
 import java.util.ResourceBundle;
 import java.util.logging.Logger;
 
 import de.lessvoid.nifty.controls.StandardControl;
 import de.lessvoid.nifty.effects.EffectEventId;
 import de.lessvoid.nifty.elements.Element;
 import de.lessvoid.nifty.input.keyboard.KeyboardInputEvent;
 import de.lessvoid.nifty.input.mouse.MouseInputEvent;
 import de.lessvoid.nifty.input.mouse.MouseInputEventQueue;
 import de.lessvoid.nifty.layout.LayoutPart;
 import de.lessvoid.nifty.loaderv2.NiftyFactory;
 import de.lessvoid.nifty.loaderv2.NiftyLoader;
 import de.lessvoid.nifty.loaderv2.types.ControlDefinitionType;
 import de.lessvoid.nifty.loaderv2.types.NiftyType;
 import de.lessvoid.nifty.loaderv2.types.PopupType;
 import de.lessvoid.nifty.loaderv2.types.RegisterEffectType;
 import de.lessvoid.nifty.loaderv2.types.StyleType;
 import de.lessvoid.nifty.loaderv2.types.resolver.style.StyleResolver;
 import de.lessvoid.nifty.loaderv2.types.resolver.style.StyleResolverDefault;
 import de.lessvoid.nifty.render.NiftyRenderEngine;
 import de.lessvoid.nifty.render.NiftyRenderEngineImpl;
 import de.lessvoid.nifty.screen.NullScreen;
 import de.lessvoid.nifty.screen.Screen;
 import de.lessvoid.nifty.screen.ScreenController;
 import de.lessvoid.nifty.sound.SoundSystem;
 import de.lessvoid.nifty.spi.input.InputSystem;
 import de.lessvoid.nifty.spi.render.RenderDevice;
 import de.lessvoid.nifty.spi.sound.SoundDevice;
 import de.lessvoid.nifty.tools.TimeProvider;
 import de.lessvoid.nifty.tools.resourceloader.ResourceLoader;
 
 /**
  * The main Nifty class.
  * @author void
  */
 public class Nifty implements NiftyInputConsumer {
   private Logger log = Logger.getLogger(Nifty.class.getName());
   private NiftyRenderEngine renderEngine;
   private SoundSystem soundSystem;
   private Map < String, Screen > screens = new Hashtable < String, Screen >();
   private Map < String, PopupType > popups = new Hashtable < String, PopupType >();
   private Map < String, Element > activePopups = new Hashtable < String, Element >();
   private Map < String, StyleType > styles = new Hashtable < String, StyleType >();
   private Map < String, ControlDefinitionType > controlDefintions = new Hashtable < String, ControlDefinitionType >();
   private Map < String, RegisterEffectType > registeredEffects = new Hashtable < String, RegisterEffectType >();
   private Screen currentScreen = new NullScreen();
   private String currentLoaded;
   private boolean exit;
   private NiftyDebugConsole console;
   private TimeProvider timeProvider;
   private List < RemovePopUp > removePopupList = new ArrayList < RemovePopUp >();
   private NiftyLoader loader;
   private List < ControlToAdd > controlsToAdd = new ArrayList < ControlToAdd >();
   private List < EndOfFrameElementAction > endOfFrameElementActions = new ArrayList < EndOfFrameElementAction >();
   private boolean useDebugConsole;
   private MouseInputEventQueue mouseInputEventQueue;
   private Collection < ScreenController > registeredScreenControllers = new ArrayList < ScreenController >();
   private String alternateKeyForNextLoadXml;
   private long lastTime;
   private InputSystem inputSystem;
   private boolean gotoScreenInProgess;
   private String alternateKey;
   private Collection < DelayedMethodInvoke > delayedMethodInvokes = new ArrayList < DelayedMethodInvoke > ();
   private Map<String, String> resourceBundleSource = new Hashtable<String, String>();
   private Map<String, ResourceBundle> resourceBundles = new Hashtable<String, ResourceBundle>();
   private Locale locale = Locale.getDefault();
   private Properties globalProperties;
 
   /**
    * Create nifty. This is now deprecated because it's way easier to use the other
    * constructor that simply takes a SoundDevice instead of a SoundSystem.
    * @param newRenderDevice the RenderDevice
    * @param newSoundSystem SoundSystem
    * @param inputSystem InputSystem
    * @param newTimeProvider the TimeProvider
    */
   @Deprecated
   public Nifty(
       final RenderDevice newRenderDevice,
       final SoundSystem newSoundSystem,
       final InputSystem newInputSystem,
       final TimeProvider newTimeProvider) {
     initialize(new NiftyRenderEngineImpl(newRenderDevice), newSoundSystem, newInputSystem, newTimeProvider);
     console = new NiftyDebugConsole(newRenderDevice);
   }
 
   /**
    * Create nifty with optional console parameter.
    * @param newRenderDevice the RenderDevice
    * @param newSoundSystem SoundSystem
    * @param inputSystem TODO
    * @param newTimeProvider the TimeProvider
    */
   public Nifty(
       final RenderDevice newRenderDevice,
       final SoundDevice newSoundDevice,
       final InputSystem newInputSystem,
       final TimeProvider newTimeProvider) {
     initialize(new NiftyRenderEngineImpl(newRenderDevice), new SoundSystem(newSoundDevice), newInputSystem, newTimeProvider);
     console = new NiftyDebugConsole(newRenderDevice);
   }
 
   /**
    * Initialize this instance.
    * @param newRenderDevice RenderDevice
    * @param newSoundSystem SoundSystem
    * @param newInputSystem TODO
    * @param newTimeProvider TimeProvider
    */
   private void initialize(
       final NiftyRenderEngine newRenderDevice,
       final SoundSystem newSoundSystem,
       final InputSystem newInputSystem,
       final TimeProvider newTimeProvider) {
     this.renderEngine = newRenderDevice;
     this.soundSystem = newSoundSystem;
     this.inputSystem = newInputSystem;
     this.timeProvider = newTimeProvider;
     this.exit = false;
     this.currentLoaded = null;
     this.mouseInputEventQueue = new MouseInputEventQueue();
     this.lastTime = timeProvider.getMsTime();
 
     try {
       loader = new NiftyLoader(timeProvider);
       loader.registerSchema("nifty.nxs", ResourceLoader.getResourceAsStream("nifty.nxs"));
       loader.registerSchema("nifty-styles.nxs", ResourceLoader.getResourceAsStream("nifty-styles.nxs"));
       loader.registerSchema("nifty-controls.nxs", ResourceLoader.getResourceAsStream("nifty-controls.nxs"));
     } catch (Exception e) {
       log.warning(e.getMessage());
     }
   }
 
   public void setAlternateKeyForNextLoadXml(final String alternateKeyForNextLoadXmlParam) {
     alternateKeyForNextLoadXml = alternateKeyForNextLoadXmlParam;
   }
 
   /**
    * Render all stuff in the current Screen.
    * @param clearScreen true if nifty should clean the screen and false when you've done that already.
    * @return true when nifty has finished processing the screen and false when rendering should continue.
    */
   public boolean render(final boolean clearScreen) {
     renderEngine.beginFrame();
     if (clearScreen) {
       renderEngine.clear();
     }
 
     if (!currentScreen.isNull()) {
       mouseInputEventQueue.begin();
       inputSystem.forwardEvents(this);
       if (mouseInputEventQueue.hasLastMouseDownEvent()) {
         currentScreen.mouseEvent(mouseInputEventQueue.getLastMouseDownEvent());
       }
       currentScreen.renderLayers(renderEngine);
       if (useDebugConsole) {
         console.render(this, currentScreen, renderEngine);
       }
     }
 
     if (exit) {
       renderEngine.clear();
     }
 
     handleDynamicElements();
     renderEngine.endFrame();
 
     long current = timeProvider.getMsTime();
     int delta = (int) (current - lastTime);
     soundSystem.update(delta);
     lastTime = current;
 
 //    System.out.println("--> screen output");
 //    System.out.println(currentScreen.debugOutput());
     return exit;
   }
 
   public boolean processMouseEvent(final MouseInputEvent mouseEvent) {
     boolean handled = true;
     if (mouseInputEventQueue.canProcess(mouseEvent)) {
       mouseInputEventQueue.process(mouseEvent);
       handled = currentScreen.mouseEvent(mouseEvent);
       handleDynamicElements();
     }
     return handled;
   }
 
   public boolean processKeyboardEvent(final KeyboardInputEvent keyEvent) {
     if (!currentScreen.isNull()) {
       currentScreen.keyEvent(keyEvent);
     }
     return true;
   }
 
   public void resetEvents() {
     mouseInputEventQueue.reset();
   }
 
   private void handleDynamicElements() {
     while (hasDynamics()) {
       invokeMethods();
       removePopUps();
       removeLayerElements();
       addControls();
       executeEndOfFrameElementActions();
     }
   }
 
   private boolean hasDynamics() {
     return hasInvokeMethods() || hasRemovePopups() || hasRemoveLayerElements() || hasControlsToAdd() || hasEndOfFrameElementActions();
   }
 
   private boolean hasRemoveLayerElements() {
     if (!currentScreen.isNull()) {
       return currentScreen.hasDynamicElements();
     }
     return false;
   }
 
   private void removeLayerElements() {
     if (!currentScreen.isNull()) {
       currentScreen.processAddAndRemoveLayerElements();
     }
   }
 
   private void removePopUps() {
     if (hasRemovePopups()) {
       if (!currentScreen.isNull()) {
         for (RemovePopUp removePopup : removePopupList) {
           removePopup.close();
         }
       }
       removePopupList.clear();
     }
   }
 
   private boolean hasRemovePopups() {
     return !removePopupList.isEmpty();
   }
 
   public void addControls() {
     if (hasControlsToAdd()) {
       for (ControlToAdd controlToAdd : controlsToAdd) {
         try {
           controlToAdd.startControl(controlToAdd.createControl());
         } catch (Exception e) {
           e.printStackTrace();
         }
       }
       controlsToAdd.clear();
     }
   }
 
   private boolean hasControlsToAdd() {
     return !controlsToAdd.isEmpty();
   }
 
   public void addControlsWithoutStartScreen() {
     if (hasControlsToAdd()) {
       for (ControlToAdd controlToAdd : controlsToAdd) {
         try {
           controlToAdd.startControlWithCheck(controlToAdd.createControl());
         } catch (Exception e) {
           e.printStackTrace();
         }
       }
       controlsToAdd.clear();
     }
   }
 
   public void executeEndOfFrameElementActions() {
     if (hasEndOfFrameElementActions()) {
      for (EndOfFrameElementAction elementAction : endOfFrameElementActions) {
         elementAction.perform();
       }
       endOfFrameElementActions.clear();
     }
   }
 
   private boolean hasEndOfFrameElementActions() {
     return !endOfFrameElementActions.isEmpty();
   }
 
   /**
    * Initialize this Nifty instance from the given xml file.
    * @param filename filename to nifty xml
    * @param startScreen screen to start exec
    */
   public void fromXml(final String filename, final String startScreen) {
     prepareScreens(filename);
     loadFromFile(filename);
     gotoScreen(startScreen);
   }
 
   /**
    * Initialize this Nifty instance from the given xml file.
    * @param filename filename to nifty xml
    */
   public void fromXmlWithoutStartScreen(final String filename) {
     prepareScreens(filename);
     loadFromFile(filename);
   }
 
   /**
    * Initialize this Nifty instance from the given xml file.
    * @param filename filename to nifty xml
    * @param startScreen screen to start exec
    * @param controllers controllers to use
    */
   public void fromXml(
       final String filename,
       final String startScreen,
       final ScreenController ... controllers) {
     registerScreenController(controllers);
     prepareScreens(filename);
     loadFromFile(filename);
     gotoScreen(startScreen);
   }
 
   /**
    * fromXml.
    * @param fileId fileId
    * @param input inputStream
    * @param startScreen screen to start
    */
   public void fromXml(final String fileId, final InputStream input, final String startScreen) {
     prepareScreens(fileId);
     loadFromStream(input);
     gotoScreen(startScreen);
   }
 
   /**
    * fromXmlWithoutStartScreen.
    * @param fileId fileId
    * @param input inputStream
    */
   public void fromXmlWithoutStartScreen(final String fileId, final InputStream input) {
     prepareScreens(fileId);
     loadFromStream(input);
   }
 
   /**
    * fromXml with ScreenControllers.
    * @param fileId fileId
    * @param input inputStream
    * @param startScreen screen to start
    * @param controllers controllers to use
    */
   public void fromXml(
       final String fileId,
       final InputStream input,
       final String startScreen,
       final ScreenController ... controllers) {
     registerScreenController(controllers);
     prepareScreens(fileId);
     loadFromStream(input);
     gotoScreen(startScreen);
   }
 
   /**
    * Load and validate the given filename. If the file is valid, nothing happens. If it
    * is invalid you'll get an exception explaining the error.
    * @param filename filename to check
    * @throws Exception exception describing the error
    */
   public void validateXml(final String filename) throws Exception {
     loader.validateNiftyXml(ResourceLoader.getResourceAsStream(filename));
   }
 
   /**
    * Load and validate the given stream. If the stream is valid, nothing happens. If it
    * is invalid you'll get an exception explaining the error.
    * @param filename filename to check
    * @throws Exception exception describing the error
    */
   public void validateXml(final InputStream stream) throws Exception {
     loader.validateNiftyXml(stream);
   }
 
   /**
    * load from the given file.
    * @param filename filename to load
    */
   void loadFromFile(final String filename) {
     log.info("loadFromFile [" + filename + "]");
 
     try {
       long start = timeProvider.getMsTime();
       NiftyType niftyType = loader.loadNiftyXml("nifty.nxs", ResourceLoader.getResourceAsStream(filename), this);
       niftyType.create(this, timeProvider);
 //      log.info(niftyType.output());
       long end = timeProvider.getMsTime();
       log.info("loadFromFile took [" + (end - start) + "]");
     } catch (Exception e) {
       e.printStackTrace();
     }
   }
 
   /**
    * load from the given file.
    * @param stream stream to load
    */
   void loadFromStream(final InputStream stream) {
     log.info("loadFromStream []");
 
     try {
       long start = timeProvider.getMsTime();
       NiftyType niftyType = loader.loadNiftyXml("nifty.nxs", stream, this);
       niftyType.create(this, timeProvider);
 //      log.info(niftyType.output());
       long end = timeProvider.getMsTime();
       log.info("loadFromStream took [" + (end - start) + "]");
     } catch (Exception e) {
       e.printStackTrace();
     }
   }
 
   /**
    * prepare/reset screens.
    * @param xmlId xml id
    */
   void prepareScreens(final String xmlId) {
     screens.clear();
 
     // this.currentScreen = null;
     this.currentLoaded = xmlId;
     this.exit = false;
   }
 
   /**
    * goto screen command. this will send first an endScreen event to the current screen.
    * @param id the new screen id we should go to.
    */
   public void gotoScreen(final String id) {
     if (gotoScreenInProgess) {
       log.info("gotoScreen [" + id + "] aborted because still in gotoScreenInProgress phase");
       return;
     }
 
     log.info("gotoScreen [" + id + "]");
     gotoScreenInProgess = true;
 
     if (currentScreen.isNull()) {
       gotoScreenInternal(id);
     } else {
       // end current screen
       currentScreen.endScreen(
           new EndNotify() {
             public void perform() {
               gotoScreenInternal(id);
             }
           });
     }
   }
 
   /**
    * goto new screen.
    * @param id the new screen id we should go to.
    */
   private void gotoScreenInternal(final String id) {
     log.info("gotoScreenInternal [" + id + "]");
 
     currentScreen = screens.get(id);
     if (currentScreen == null) {
       currentScreen = new NullScreen();
       log.warning("screen [" + id + "] not found");
       gotoScreenInProgess = false;
       return;
     }
 
     // start the new screen
     if (alternateKeyForNextLoadXml != null) {
       currentScreen.setAlternateKey(alternateKeyForNextLoadXml);
       alternateKeyForNextLoadXml = null;
     }
     currentScreen.startScreen(new EndNotify() {
       public void perform() {
         gotoScreenInProgess = false;
       }
     });
   }
 
   /**
    * Set alternate key for all screen. This could be used to change behavior on all screens.
    * @param alternateKey the new alternate key to use
    */
   public void setAlternateKey(final String alternateKey) {
     this.alternateKey = alternateKey;
     for (Screen screen : screens.values()) {
       screen.setAlternateKey(alternateKey);
     }
   }
 
   /**
    * Returns a collection of the name of all screens
    * @return sn The collection containing the name of all screens
    */
   public Collection < String > getAllScreensName() {
     Collection < String > sn = new LinkedList < String >();
     for (Screen screen : screens.values()) {
       sn.add(screen.getScreenId());
     }
     return sn;
   }
 
   /**
    * exit.
    */
   public void exit() {
     currentScreen.endScreen(
         new EndNotify() {
           public final void perform() {
             exit = true;
             currentScreen = new NullScreen();
           }
         });
   }
 
   /**
    * get a specific screen.
    * @param id the id of the screen to retrieve.
    * @return the screen
    */
   public Screen getScreen(final String id) {
     Screen screen = screens.get(id);
     if (screen == null) {
       log.warning("screen [" + id + "] not found");
       return null;
     }
 
     return screen;
   }
 
   /**
    * Get the SoundSystem.
    * @return SoundSystem
    */
   public SoundSystem getSoundSystem() {
     return soundSystem;
   }
 
   /**
    * Return the RenderDevice.
    * @return RenderDevice
    */
   public NiftyRenderEngine getRenderEngine() {
     return renderEngine;
   }
 
   /**
    * Get current screen.
    * @return current screen
    */
   public Screen getCurrentScreen() {
     return currentScreen;
   }
 
   /**
    * Check if nifty displays the file with the given filename and is at a screen with the given screenId.
    * @param filename filename
    * @param screenId screenId
    * @return true if the given screen is active and false when not
    */
   public boolean isActive(final String filename, final String screenId) {
     if (currentLoaded != null && currentLoaded.equals(filename)) {
       if (!currentScreen.isNull() && currentScreen.getScreenId().equals(screenId)) {
         return true;
       }
     }
     return false;
   }
 
   /**
    * popup.
    * @param popup popup
    */
   public void registerPopup(final PopupType popup) {
     popups.put(popup.getAttributes().get("id"), popup);
   }
 
   /**
    * show popup in the given screen.
    * @param screen screen
    * @param id id
    */
   public void showPopup(final Screen screen, final String id, final Element defaultFocusElement) {
     Element popup = activePopups.get(id);
     if (popup == null) {
       log.warning("missing popup [" + id + "] o_O");
     } else {
       screen.addPopup(popup, defaultFocusElement);
     }
   }
 
   private Element createPopupFromType(final PopupType popupTypeParam) {
     Screen screen = getCurrentScreen();
     LayoutPart layerLayout = NiftyFactory.createRootLayerLayoutPart(this);
     PopupType popupType = new PopupType(popupTypeParam);
     popupType.prepare(this, screen, screen.getRootElement().getElementType());
     return popupType.create(
         screen.getRootElement(),
         this,
         screen,
         layerLayout);
   }
 
   public Element createPopup(final String id) {
     return createAndAddPopup(id, popups.get(id));
   }
 
   public Element createPopupWithId(final String popupId) {
     return createAndAddPopup(NiftyIdCreator.generate(), popups.get(popupId));
   }
 
   public Element createPopupWithStyle(final String id, final String style) {
     PopupType popupType = popups.get(id);
     popupType.getAttributes().set("style", style);
     return createAndAddPopup(id, popupType);
   }
 
   private Element createAndAddPopup(final String id, PopupType popupType) {
     Element popupElement = createPopupFromType(popupType);
     popupElement.setId(id);
     activePopups.put(id, popupElement);
     return popupElement;
   }
 
   public Element findActivePopupByName(final String id) {
     return activePopups.get(id);
   }
 
   public Element getTopMostPopup() {
     if (currentScreen != null) {
       return currentScreen.getTopMostPopup();
     }
     return null;
   }
 
   /**
    * Close the Popup with the given id.
    * @param id id of popup to close
    */
   public void closePopup(final String id) {
     closePopupInternal(id, null);
   }
 
   /**
    * Close the Popup with the given id. This calls the given EndNotify when the onEndScreen of the popup ends.
    * @param id id of popup to close
    * @param closeNotify EndNotify callback
    */
   public void closePopup(final String id, final EndNotify closeNotify) {
     closePopupInternal(id, closeNotify);
   }
 
   private void closePopupInternal(final String id, final EndNotify closeNotify) {
     Element popup = activePopups.get(id);
     if (popup == null) {
       log.warning("missing popup [" + id + "] o_O");
       return;
     }
     popup.resetAllEffects();
     popup.startEffect(EffectEventId.onEndScreen, new EndNotify() {
       public void perform() {
         removePopupList.add(new RemovePopUp(id, closeNotify));
       }
     });
   }
 
   /**
    * Add a control to this screen and the given parent element.
    * @param screen screen
    * @param parent parent element
    * @param controlName control name to add
    * @param id id of control
    * @param style style
    * @param focusable focusable
   public void addControl(
       final Screen screen,
       final Element parent,
       final String controlName,
       final String id,
       final String style,
       final Boolean focusable,
       final Attributes attributes) {
     controlsToAdd.add(
         new ControlToAdd(
             screen, parent, controlName, id, style, focusable, attributes));
   }
    */
 
   public void addControl(
       final Screen screen,
       final Element element,
       final StandardControl standardControl) {
     controlsToAdd.add(new ControlToAdd(screen, element, standardControl));
   }
 
   /**
    * ControlToAdd helper class.
    * @author void
    */
   private class ControlToAdd {
     private Screen screen;
     private Element parent;
     private StandardControl control;
 
     public ControlToAdd(
         final Screen screenParam,
         final Element parentParam,
         final StandardControl standardControl) {
       screen = screenParam;
       parent = parentParam;
       control = standardControl;
     }
 
     public void startControlWithCheck(final Element element) {
       element.bindToScreen(screen);
     }
 
     public Element createControl() throws Exception {
       return control.createControl(Nifty.this, screen, parent);
     }
 
     public void startControl(final Element newControl) {
       newControl.startEffect(EffectEventId.onStartScreen);
       newControl.startEffect(EffectEventId.onActive);
       newControl.onStartScreen(screen);
     }
   }
 
   private interface Action {
     void perform(Screen screen, Element element);
   }
 
   public class ElementRemoveAction implements Action {
     public void perform(final Screen screen, final Element element) {
       removeSingleElement(element);
       Element parent = element.getParent();
       if (parent != null) {
         parent.getElements().remove(element);
       }
       screen.layoutLayers();
     }
 
     private void removeSingleElement(final Element element) {
       Iterator < Element > elementIt = element.getElements().iterator();
       while (elementIt.hasNext()) {
         Element el = elementIt.next();
         removeSingleElement(el);
         elementIt.remove();
       }
     }
   }
 
   public class ElementMoveAction implements Action {
     private Element destinationElement;
 
     public ElementMoveAction(final Element destinationElement) {
       this.destinationElement = destinationElement;
     }
 
     public void perform(final Screen screen, final Element element) {
       Element parent = element.getParent();
       if (parent != null) {
         parent.getElements().remove(element);
       }
       element.setParent(destinationElement);
       destinationElement.add(element);
       screen.layoutLayers();
     }
   }
 
   private class EndOfFrameElementAction {
     private Screen screen;
     private Element element;
     private Action action;
     private EndNotify endNotify;
 
     public EndOfFrameElementAction(final Screen newScreen, final Element newElement, final Action action, final EndNotify endNotify) {
       this.screen = newScreen;
       this.element = newElement;
       this.action = action;
       this.endNotify = endNotify;
     }
 
     public void perform() {
       action.perform(screen, element);
       if (endNotify != null) {
         endNotify.perform();
       }
     }
   }
 
   public void removeElement(final Screen screen, final Element element) {
     removeElement(screen, element, null);
   }
 
   public void removeElement(final Screen screen, final Element element, final EndNotify endNotify) {
     element.removeFromFocusHandler();
     element.startEffect(EffectEventId.onEndScreen, new EndNotify() {
       public void perform() {
         endOfFrameElementActions.add(new EndOfFrameElementAction(screen, element, new ElementRemoveAction(), endNotify));
       }
     });
   }
 
   public void moveElement(final Screen screen, final Element elementToMove, final Element destination, final EndNotify endNotify) {
     elementToMove.removeFromFocusHandler();
     elementToMove.startEffect(EffectEventId.onEndScreen, new EndNotify() {
       public void perform() {
         endOfFrameElementActions.add(new EndOfFrameElementAction(screen, elementToMove, new ElementMoveAction(destination), endNotify));
       }
     });
   }
 
   public void toggleElementsDebugConsole() {
     useDebugConsole = !useDebugConsole;
     console.setOutputElements(true);
   }
 
   public void toggleEffectsDebugConsole() {
     useDebugConsole = !useDebugConsole;
     console.setOutputElements(false);
   }
 
   /**
    * @return the mouseInputEventQueue
    */
   public MouseInputEventQueue getMouseInputEventQueue() {
     return mouseInputEventQueue;
   }
 
   /**
    * Register a ScreenController instance.
    * @param controllers ScreenController
    */
   public void registerScreenController(final ScreenController ... controllers) {
     for (ScreenController c : controllers) {
       registeredScreenControllers.add(c);
     }
   }
 
   /**
    * find a ScreenController instance that matches the given controllerClass name.
    * @param controllerClass controller class name
    * @return ScreenController instance
    */
   public ScreenController findScreenController(final String controllerClass) {
     for (ScreenController controller : registeredScreenControllers) {
       if (controller.getClass().getName().equals(controllerClass)) {
         return controller;
       }
     }
     return null;
   }
 
   public NiftyLoader getLoader() {
     return loader;
   }
 
   public TimeProvider getTimeProvider() {
     return timeProvider;
   }
 
   public class RemovePopUp {
     private String removePopupId;
     private EndNotify closeNotify;
 
     public RemovePopUp(final String popupId, final EndNotify closeNotifyParam) {
       removePopupId = popupId;
       closeNotify = closeNotifyParam;
     }
 
     public void close() {
       currentScreen.closePopup(activePopups.get(removePopupId), closeNotify);
     }
   }
 
   public void addScreen(final String id, final Screen screen) {
     screens.put(id, screen);
   }
 
   public void registerStyle(final StyleType style) {
     log.fine("registerStyle " + style.getStyleId());
     styles.put(style.getStyleId(), style);
   }
 
   public void registerControlDefintion(final ControlDefinitionType controlDefintion) {
     controlDefintions.put(controlDefintion.getName(), controlDefintion);
   }
 
   public void registerEffect(final RegisterEffectType registerEffectType) {
     registeredEffects.put(registerEffectType.getName(), registerEffectType);
   }
 
   public ControlDefinitionType resolveControlDefinition(final String name) {
     if (name == null) {
       return null;
     }
     return controlDefintions.get(name);
   }
 
   public RegisterEffectType resolveRegisteredEffect(final String name) {
     if (name == null) {
       return null;
     }
     return registeredEffects.get(name);
   }
 
   public StyleResolver getDefaultStyleResolver() {
     return new StyleResolverDefault(styles);
   }
 
   public String getAlternateKey() {
     return alternateKey;
   }
 
   public void delayedMethodInvoke(final NiftyDelayedMethodInvoke method, final Object[] params) {
     delayedMethodInvokes.add(new DelayedMethodInvoke(method, params));
   }
 
   public void invokeMethods() {
     if (hasInvokeMethods()) {
       // make working copy in case a method invoke will create addtional method calls
       Collection < DelayedMethodInvoke > workingCopy = new ArrayList < DelayedMethodInvoke > (delayedMethodInvokes);
 
       // clean current List
       delayedMethodInvokes.clear();
 
       // process the working copy
       for (DelayedMethodInvoke method : workingCopy) {
         method.perform();
       }
 
       // the delayedMethodInvokes list is empty now or it has new entries that resulted from method.perform calls
       // in that case these methods will be processed next frame
     }
   }
 
   private boolean hasInvokeMethods() {
     return !delayedMethodInvokes.isEmpty();
   }
 
   private class DelayedMethodInvoke {
     private NiftyDelayedMethodInvoke method;
     private Object[] params;
 
     public DelayedMethodInvoke(final NiftyDelayedMethodInvoke method, final Object[] params) {
       this.method = method;
       this.params = params;
     }
 
     public void perform() {
       method.performInvoke(params);
     }
   }
 
   public void setLocale(final Locale locale) {
     this.locale = locale;
   }
 
   public Map<String, ResourceBundle> getResourceBundles() {
     return resourceBundles;
   }
 
   public void addResourceBundle(final String id, final String filename) {
     resourceBundleSource.put(id, filename);
     resourceBundles.put(id, ResourceBundle.getBundle(filename, locale));
   }
 
   public Properties getGlobalProperties() {
     return globalProperties;
   }
 
   public void setGlobalProperties(Properties globalProperties) {
     this.globalProperties = globalProperties;
   }
 }
