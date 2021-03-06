 /*
 * $Id: UICommand.java,v 1.80 2007/08/28 17:27:47 edburns Exp $
  */
 
 /*
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  * 
  * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
  * 
  * The contents of this file are subject to the terms of either the GNU
  * General Public License Version 2 only ("GPL") or the Common Development
  * and Distribution License("CDDL") (collectively, the "License").  You
  * may not use this file except in compliance with the License. You can obtain
  * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
  * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
  * language governing permissions and limitations under the License.
  * 
  * When distributing the software, include this License Header Notice in each
  * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
  * Sun designates this particular file as subject to the "Classpath" exception
  * as provided by Sun in the GPL Version 2 section of the License file that
  * accompanied this code.  If applicable, add the following below the License
  * Header, with the fields enclosed by brackets [] replaced by your own
  * identifying information: "Portions Copyrighted [year]
  * [name of copyright owner]"
  * 
  * Contributor(s):
  * 
  * If you wish your version of this file to be governed by only the CDDL or
  * only the GPL Version 2, indicate your decision by adding "[Contributor]
  * elects to include this software in this distribution under the [CDDL or GPL
  * Version 2] license."  If you don't indicate a single choice of license, a
  * recipient has the option to distribute your version of this file under
  * either the CDDL, the GPL Version 2 or to extend the choice of license to
  * its licensees as provided above.  However, if you add GPL Version 2 code
  * and therefore, elected the GPL Version 2 license, then the option applies
  * only if the new code is made subject to such option by the copyright
  * holder.
  */
 
 package javax.faces.component;
 
 import javax.el.ELException;
 import javax.el.MethodExpression;
 import javax.el.ValueExpression;
 import javax.faces.FacesException;
 import javax.faces.application.Application;
 import javax.faces.context.FacesContext;
 import javax.faces.el.MethodBinding;
 import javax.faces.event.AbortProcessingException;
 import javax.faces.event.ActionEvent;
 import javax.faces.event.ActionListener;
 import javax.faces.event.FacesEvent;
 import javax.faces.event.PhaseId;
 import javax.faces.render.Renderer;
 
 
 /**
  * <p><strong>UICommand</strong> is a {@link UIComponent} that represents
  * a user interface component which, when activated by the user, triggers
  * an application specific "command" or "action".  Such a component is
  * typically rendered as a push button, a menu item, or a hyperlink.</p>
  *
  * <p>When the <code>decode()</code> method of this {@link UICommand}, or
  * its corresponding {@link Renderer}, detects that this control has been
  * activated, it will queue an {@link ActionEvent}.
  * Later on, the <code>broadcast()</code> method will ensure that this
  * event is broadcast to all interested listeners.</p>
  * 
  * <p>Listeners will be invoked in the following order:
  * <ol>
  *  <li>{@link ActionListener}s, in the order in which they were registered.
  *  <li>The "actionListener" {@link MethodExpression} (which will cover
  *  the "actionListener" that was set as a <code>MethodBinding</code>).
  *  <li>The default {@link ActionListener}, retrieved from the
  *      {@link Application} - and therefore, any attached "action"
  *      {@link MethodExpression}.
  * </ol>
  * </p>
  * <p>By default, the <code>rendererType</code> property must be set to
  * "<code>javax.faces.Button</code>".  This value can be changed by calling the
  * <code>setRendererType()</code> method.</p>
  */
 
 public class UICommand extends UIComponentBase
     implements ActionSource2 {
 
 
     // ------------------------------------------------------ Manifest Constants
 
 
     /**
      * <p>The standard component type for this component.</p>
      */
     public static final String COMPONENT_TYPE = "javax.faces.Command";
 
 
     /**
      * <p>The standard component family for this component.</p>
      */
     public static final String COMPONENT_FAMILY = "javax.faces.Command";
 
 
     // ------------------------------------------------------------ Constructors
 
 
     /**
      * <p>Create a new {@link UICommand} instance with default property
      * values.</p>
      */
     public UICommand() {
 
         super();
         setRendererType("javax.faces.Button");
 
     }
 
 
     // ------------------------------------------------------ Instance Variables
 
 
     private Object value = null;
 
 
     // -------------------------------------------------------------- Properties
 
 
     public String getFamily() {
 
         return (COMPONENT_FAMILY);
 
     }
 
 
     // ------------------------------------------------- ActionSource/ActionSource2 Properties
 
 
     /**
      * {@inheritDoc}
      * @deprecated This has been replaced by {@link #getActionExpression}.
      */
     public MethodBinding getAction() {
 	MethodBinding result = null;
 	MethodExpression me;
 
 	if (null != (me = getActionExpression())) {
 	    // if the MethodExpression is an instance of our private
 	    // wrapper class.
 	    if (me.getClass().equals(MethodExpressionMethodBindingAdapter.class)) {
 		result = ((MethodExpressionMethodBindingAdapter)me).getWrapped();
 	    }
 	    else {
 		// otherwise, this is a real MethodExpression.  Wrap it
 		// in a MethodBinding.
 		result = new MethodBindingMethodExpressionAdapter(me);
 	    }
 	}
 	return result;
 	    
     }
 
     /**
      * {@inheritDoc}
      * @deprecated This has been replaced by {@link #setActionExpression(javax.el.MethodExpression)}.
      */
     public void setAction(MethodBinding action) {
 	MethodExpressionMethodBindingAdapter adapter;
 	if (null != action) {
 	    adapter = new MethodExpressionMethodBindingAdapter(action);
 	    setActionExpression(adapter);
 	}
 	else {
 	    setActionExpression(null);
 	}
     }
     
     /**
      * {@inheritDoc}
      * @deprecated Use {@link #getActionListeners} instead.
      */
     public MethodBinding getActionListener() {
         return this.methodBindingActionListener;
     }
 
     /**
      * {@inheritDoc}
      * @deprecated This has been replaced by {@link #addActionListener(javax.faces.event.ActionListener)}.
      */
     public void setActionListener(MethodBinding actionListener) {
         this.methodBindingActionListener = actionListener;
     } 
 
     /**
      * <p>The immediate flag.</p>
      */
     private boolean immediate = false;
     private boolean immediateSet = false;
 
 
     public boolean isImmediate() {
 
 	if (this.immediateSet) {
 	    return (this.immediate);
 	}
 	ValueExpression ve = getValueExpression("immediate");
 	if (ve != null) {
 	    try {
 		return (Boolean.TRUE.equals(ve.getValue(getFacesContext().getELContext())));
 	    }
 	    catch (ELException e) {
 		throw new FacesException(e);
 	    }
 	} else {
 	    return (this.immediate);
 	}
 
     }
 
 
     public void setImmediate(boolean immediate) {
 
 	// if the immediate value is changing.
 	if (immediate != this.immediate) {
 	    this.immediate = immediate;
 	}
 	this.immediateSet = true;
 
     }
 
 
 
     /**
      * <p>Returns the <code>value</code> property of the
      * <code>UICommand</code>. This is most often rendered as a label.</p>
      */
     public Object getValue() {
 
 	if (this.value != null) {
 	    return (this.value);
 	}
 	ValueExpression ve = getValueExpression("value");
 	if (ve != null) {
 	    try {
 		return (ve.getValue(getFacesContext().getELContext()));
 	    }
 	    catch (ELException e) {
 		throw new FacesException(e);
 	    }
 
 	} else {
 	    return (null);
 	}
 
     }
 
 
     /**
      * <p>Sets the <code>value</code> property of the <code>UICommand</code>.
      * This is most often rendered as a label.</p>
      *
      * @param value the new value
      */
     public void setValue(Object value) {
 
         this.value = value;
 
     }
     
     private MethodBinding methodBindingActionListener = null;
 
 
     // ---------------------------------------------------- ActionSource / ActionSource2 Methods
 
     
     /**
      * <p>The {@link MethodExpression} that, when invoked, yields the
      * literal outcome value.</p>
      */
     private MethodExpression actionExpression = null;
     
     public MethodExpression getActionExpression() {
         return actionExpression;
     }
     
     public void setActionExpression(MethodExpression actionExpression) {
         this.actionExpression = actionExpression;    
     }
     
     /** 
      * @throws NullPointerException {@inheritDoc}
      */ 
     public void addActionListener(ActionListener listener) {
 
         addFacesListener(listener);
 
     }
     
     public ActionListener[] getActionListeners() {
 
         ActionListener al[] = (ActionListener [])
 	    getFacesListeners(ActionListener.class);
         return (al);
 
     }
 
 
 
     /**
      * @throws NullPointerException {@inheritDoc}
      */ 
     public void removeActionListener(ActionListener listener) {
 
         removeFacesListener(listener);
 
     }
 
 
     // ----------------------------------------------------- StateHolder Methods
 
 
     private Object[] values;
 
     public Object saveState(FacesContext context) {
 
         if (values == null) {
              values = new Object[6];
         }
       
         values[0] = super.saveState(context);
         values[1] = saveAttachedState(context, methodBindingActionListener);
         values[2] = saveAttachedState(context, actionExpression);
         values[3] = immediate ? Boolean.TRUE : Boolean.FALSE;
         values[4] = immediateSet ? Boolean.TRUE : Boolean.FALSE;
         values[5] = value;
         
         return (values);
 
     }
 
 
     public void restoreState(FacesContext context, Object state) {
         values = (Object[]) state;
         super.restoreState(context, values[0]);
         methodBindingActionListener = (MethodBinding)
             restoreAttachedState(context, values[1]);
         actionExpression = 
 	    (MethodExpression) restoreAttachedState(context, values[2]);
         immediate = ((Boolean) values[3]).booleanValue();
         immediateSet = ((Boolean) values[4]).booleanValue();
         value = values[5];
         
     }
 
 
     // ----------------------------------------------------- UIComponent Methods
 
 
     /**
      * <p>In addition to to the default {@link UIComponent#broadcast}
      * processing, pass the {@link ActionEvent} being broadcast to the
      * method referenced by <code>actionListener</code> (if any),
      * and to the default {@link ActionListener} registered on the
      * {@link javax.faces.application.Application}.</p>
      *
      * @param event {@link FacesEvent} to be broadcast
      *
      * @throws AbortProcessingException Signal the JavaServer Faces
      *  implementation that no further processing on the current event
      *  should be performed
      * @throws IllegalArgumentException if the implementation class
      *  of this {@link FacesEvent} is not supported by this component
      * @throws NullPointerException if <code>event</code> is
      * <code>null</code>
      */
     public void broadcast(FacesEvent event) throws AbortProcessingException {
 
         // Perform standard superclass processing (including calling our
         // ActionListeners)
         super.broadcast(event);
 
         if (event instanceof ActionEvent) {
             FacesContext context = getFacesContext();
             
             // Notify the specified action listener method (if any)
             MethodBinding mb = getActionListener();
             if (mb != null) {
                 mb.invoke(context, new Object[] { event });
             }
 
             // Invoke the default ActionListener
             ActionListener listener =
               context.getApplication().getActionListener();
             if (listener != null) {
                 listener.processAction((ActionEvent) event);
             }
         }
     }
 
     /**

     * <p>Intercept <code>queueEvent</code> and take the following
     * action.  If the event is an <code>{@link ActionEvent}</code>,
     * obtain the <code>UIComponent</code> instance from the event.  If
     * the component is an <code>{@link ActionSource}</code> obtain the
     * value of its "immediate" property.  If it is true, mark the
     * phaseId for the event to be
     * <code>PhaseId.APPLY_REQUEST_VALUES</code> otherwise, mark the
     * phaseId to be <code>PhaseId.INVOKE_APPLICATION</code>.  The event
     * must be passed on to <code>super.queueEvent()</code> before
     * returning from this method.</p>

      */
 
     public void queueEvent(FacesEvent e) {
         UIComponent c = e.getComponent();
        if (e instanceof ActionEvent && c instanceof ActionSource) {
            if (((ActionSource) c).isImmediate()) {
                 e.setPhaseId(PhaseId.APPLY_REQUEST_VALUES);
             } else {
                 e.setPhaseId(PhaseId.INVOKE_APPLICATION);
             }
         }
         super.queueEvent(e);
     }
 }
