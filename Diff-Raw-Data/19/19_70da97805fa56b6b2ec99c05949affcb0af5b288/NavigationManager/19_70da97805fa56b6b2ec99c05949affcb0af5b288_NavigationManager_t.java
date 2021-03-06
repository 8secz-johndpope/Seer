 package com.vaadin.addon.touchkit.ui;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.Stack;
 
 import com.vaadin.addon.touchkit.gwt.client.VNavigationManager;
 import com.vaadin.terminal.PaintException;
 import com.vaadin.terminal.PaintTarget;
 import com.vaadin.ui.AbstractComponentContainer;
 import com.vaadin.ui.ClientWidget;
 import com.vaadin.ui.ClientWidget.LoadStyle;
 import com.vaadin.ui.Component;
 
 /**
  * A non-visible component container that allows for smooth navigation between
  * components, or views. It support all components, but back buttons are updated
  * automatically only for {@link NavigationView}s.
  * <p>
  * When a component is navigated to, it replaces the currently visible
  * component, which in turn is pushed on to the stack of previous views. One can
  * navigate backwards by calling {@link #navigateBack()}, in which case the
  * currently visible view is forgotten and the previous view is restored from
  * the stack and made visible.
  * <p>
 * When used with {@link NavigationView}s, {@link NavigationBar}s and
 * {@link NavigationButton}s, navigation is smooth and quite automatic.
  * <p>
 * Bootstrap the navigation by giving the {@link NavigationManager} an initial
 * view, either by using the constructor {@link #NavigationManager(Component)}
 * or by calling {@link #navigateTo(Component)}.
  */
 @ClientWidget(value = VNavigationManager.class, loadStyle = LoadStyle.EAGER)
 public class NavigationManager extends AbstractComponentContainer {
     /*-
     TODO deprecate + throw on component container mutation methods.
     
     TODO make automatic viewStack configurable, developers might want to release
     previous views from memory and handle back navigation by themselves. E.g.
     keep only one view backwards in memory and reload older ones if necessary.
     -*/
 
     /*
      * Implementation notes
      * 
      * Actually has three 'active' components: previous, current and next. The
      * previous component is actually pushed onto the viewStack only when
      * natigateTo() pushes everything down. I.e setPreviousComponent() actually
      * replaces the previous component before it's pushed onto the stack. In
      * javadoc, this is simplified to ignore implementation details, instead
      * pretending the previous component is topmost on the 'history'.
      */
 
     private Stack<Component> viewStack = new Stack<Component>();
 
     private Component currentComponent;
     private Component previousComponent;
     private Component nextComponent;
 
     /**
      * Constructs a {@link NavigationManager} that is 100% wide and high.
      */
     public NavigationManager() {
         setSizeFull();
     }
 
     /**
     * Constructs a {@link NavigationManager} that is 100% wide and high, and
     * initially navigates to (shows) the given component.
     */
    public NavigationManager(Component c) {
        this();
        navigateTo(c);
    }

    /**
      * Navigates to the given component, effectively making it the new visible
      * component. If the given component is actually the previous component in
      * the history, {@link #navigateBack()} is performed, otherwise the replaced
      * view (previously visible) is pushed onto the history.
      * 
      * @param c
      *            the view to navigate to
      */
     public void navigateTo(Component c) {
         if (c == null) {
             throw new UnsupportedOperationException(
                     "Some component must always be visible");
         } else if (c == currentComponent) {
             /*
              * Already navigated to this component.
              */
             return;
         } else if (previousComponent == c) {
             /*
              * Same as navigateBack
              */
             navigateBack();
             return;
         }
 
         if (nextComponent != c) {
             if (nextComponent != null) {
                 removeComponent(nextComponent);
             }
             addComponent(c);
             if (c instanceof NavigationView) {
                 NavigationView view = (NavigationView) c;
                 if (view.getPreviousComponent() == null) {
                     view.setPreviousComponent(currentComponent);
                 }
             }
         } else {
             nextComponent = null;
         }
         if (previousComponent != null) {
             removeComponent(previousComponent);
             viewStack.push(previousComponent);
         }
         previousComponent = currentComponent;
         currentComponent = c;
         notifyViewOfBecomingVisible();
         requestRepaint();
     }
 
     private void notifyViewOfBecomingVisible() {
         if (currentComponent instanceof NavigationView) {
             NavigationView v = (NavigationView) currentComponent;
             v.onBecomingVisible();
             /*
              * TODO consider forcing setting the previous component here.
              */
         }
 
     }
 
     /**
      * Makes the previous component in the history visible, replacing (and
      * essentially forgetting) the component that was previously visible.
      */
     public void navigateBack() {
         if (previousComponent == null) {
             return;
         }
         if (nextComponent != null) {
             removeComponent(nextComponent);
         }
         // nextComponent is kept for the animation and in case the user
         // navigates 'back to the future':
         nextComponent = currentComponent;
         currentComponent = previousComponent;
         previousComponent = viewStack.isEmpty() ? null : viewStack.pop();
         if (previousComponent != null) {
             addComponent(previousComponent);
         }
         notifyViewOfBecomingVisible();
         requestRepaint();
     }
 
     /**
      * Sets the currently displayed component in the NavigationManager.
      * <p>
      * If current component is already set it is overridden. If the new
      * component or the next component is of type NavigationView, their previous
      * components will be automatically re-assigned.
      * 
      * @param newcurrentComponent
      */
     public void setCurrentComponent(Component newcurrentComponent) {
         if (currentComponent != newcurrentComponent) {
             if (currentComponent != null) {
                 removeComponent(currentComponent);
             }
             currentComponent = newcurrentComponent;
             addComponent(newcurrentComponent);
             if (previousComponent != null
                     && currentComponent instanceof NavigationView) {
                 NavigationView view = (NavigationView) currentComponent;
                 view.setPreviousComponent(previousComponent);
             }
             if (nextComponent != null
                     && nextComponent instanceof NavigationView) {
                 NavigationView view = (NavigationView) nextComponent;
                 view.setPreviousComponent(currentComponent);
 
             }
             requestRepaint();
         }
     }
 
     /**
      * Returns the currently visible component.
      * 
      * @return the component that is currently visible
      */
     public Component getCurrentComponent() {
         return currentComponent;
     }
 
     /**
      * Replaces the topmost component in the history, forgetting the replaced
      * component - i.e modifies the history.
      * 
      * @param newPreviousComponent
      */
     public void setPreviousComponent(Component newPreviousComponent) {
         if (previousComponent != newPreviousComponent) {
             if (previousComponent != null) {
                 removeComponent(previousComponent);
             }
             previousComponent = newPreviousComponent;
             if (currentComponent instanceof NavigationView) {
                 NavigationView view = (NavigationView) currentComponent;
                 view.setPreviousComponent(newPreviousComponent);
             }
             addComponent(newPreviousComponent);
             requestRepaint();
         }
     }
 
     /**
      * Gets the previous component from the history.
      * 
      * @return the previous component, or null if n/a
      */
     public Component getPreviousComponent() {
         return previousComponent;
     }
 
     /**
      * If the developer knows the next component where user is going to
      * navigate, it can be set with this method. This might allow the component
      * to be pre-rendered before the actual navigation (and animation) occurs.
      * Having a null as nextComponent shows a placeholder content until the next
      * view is rendered.
      */
     public void setNextComponent(Component nextComponent) {
         this.nextComponent = nextComponent;
         requestRepaint();
     }
 
     /**
      * Gets the next component, if one is set.
      * 
      * @see #setNextComponent(Component)
      * @return the next component, or null id n/a
      */
     public Component getNextComponent() {
         return nextComponent;
     }
 
     /**
      * This operation is not supported
      */
     public void replaceComponent(Component oldComponent, Component newComponent) {
         throw new UnsupportedOperationException();
     }
 
     @Override
     public void paintContent(PaintTarget target) throws PaintException {
         super.paintContent(target);
         if (currentComponent != null) {
             target.addAttribute("c", currentComponent);
         }
         if (nextComponent != null) {
             target.addAttribute("n", nextComponent);
         }
         if (previousComponent != null) {
             target.addAttribute("p", previousComponent);
         }
         Iterator<Component> componentIterator = getComponentIterator();
         while (componentIterator.hasNext()) {
             Component next = componentIterator.next();
             next.paint(target);
         }
     }
 
     public Iterator<Component> getComponentIterator() {
         ArrayList<Component> components = new ArrayList<Component>(3);
         if (previousComponent != null) {
             components.add(previousComponent);
         }
         if (currentComponent != null) {
             components.add(currentComponent);
         }
         if (nextComponent != null) {
             components.add(nextComponent);
         }
         return components.iterator();
     }
 
 }
