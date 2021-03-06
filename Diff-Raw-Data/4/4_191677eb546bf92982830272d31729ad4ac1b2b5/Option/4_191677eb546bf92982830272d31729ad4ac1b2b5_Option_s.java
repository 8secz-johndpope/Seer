 //
 // Tapestry Web Application Framework
 // Copyright (c) 2000-2002 by Howard Lewis Ship
 //
 // Howard Lewis Ship
 // http://sf.net/projects/tapestry
 // mailto:hship@users.sf.net
 //
 // This library is free software.
 //
 // You may redistribute it and/or modify it under the terms of the GNU
 // Lesser General Public License as published by the Free Software Foundation.
 //
 // Version 2.1 of the license should be included with this distribution in
 // the file LICENSE, as well as License.html. If the license is not
 // included with this distribution, you may find a copy at the FSF web
 // site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
 // Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.
 //
 // This library is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied waranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 // Lesser General Public License for more details.
 //
 
 package net.sf.tapestry.form;
 
 import net.sf.tapestry.AbstractComponent;
 import net.sf.tapestry.IBinding;
 import net.sf.tapestry.IMarkupWriter;
 import net.sf.tapestry.IRequestCycle;
 import net.sf.tapestry.RequestCycleException;
 import net.sf.tapestry.RequiredParameterException;
 import net.sf.tapestry.Tapestry;
 
 /**
  *  Implements a component that manages an HTML &lt;option&gt; form element.
  *  Such a component must be wrapped (possibly indirectly)
  *  inside a {@link Select} component.
  *
  * <table border=1>
  * <tr> 
  *    <td>Parameter</td>
  *    <td>Type</td>
  *	  <td>Read / Write </td>
  *    <td>Required</td> 
  *    <td>Default</td>
  *    <td>Description</td>
  * </tr>
  *
  *
  * <tr>
  *		<td>selected</td>
  *		<td>java.lang.Boolean</td>
  *		<td>R / W </td>
  *		<td>yes</td>
  *		<td>&nbsp;</td>
  *		<td>Used to indicate whether the given option is selected.
  *		</td>
  *	</tr>
  *
  *  <tr>
  *		<td>label</td>
  *		<td>java.lang.String</td>
  *		<td>R</td>
  *		<td>no</td>
  *		<td>&nbsp;</td>
  *		<td>A string which represents the option that may be selected.  This is optional;
  *		any text that follows the &lt;option&gt; tag is considered the label, but this
  *      saves the designed from including one more 
  *      {@link net.sf.tapestry.components.Insert} component.
  *		</td>
  *	</tr>
  *	</table>
  *
  *  <p>Allows informal parameters, but may not contain a body.
  *
  *  @author Howard Lewis Ship
  *  @version $Id$
  * 
  **/
 
 public class Option extends AbstractComponent
 {
     private IBinding selectedBinding;
 	private String label;
 
 
     public IBinding getSelectedBinding()
     {
         return selectedBinding;
     }
 
     /**
      *  Renders the &lt;option&gt; element, or responds when the form containing the element 
      *  is submitted (by checking {@link Form#isRewinding()}.
      *
      *  <table border=1>
      *  <tr>  <th>attribute</th>  <th>value</th> </tr>
      *  <tr>  <td>value</td>  <td>from {@link Select#getNextOptionId()}</td>  </tr>
      *  <tr> <td>selected</td> <td>from selected property</td> </tr>
      *  <tr> <td><i>other</i></td> <td>from extra bindings</td> </tr>
      *  </tr>
      *  </table>
      *
      * <p>If the <code>label</code> property is set, it is inserted after the
      * &lt;option&gt; tag.
      *
      **/
 
     protected void renderComponent(IMarkupWriter writer, IRequestCycle cycle) throws RequestCycleException
     {
         String value;
        String label = null;
         Select select;
         boolean rewinding;
 
         select = Select.get(cycle);
         if (select == null)
             throw new RequestCycleException(Tapestry.getString("Option.must-be-contained-by-select"), this);
 
         // It isn't enough to know whether the cycle in general is rewinding, need to know
         // specifically if the form which contains this component is rewinding.
 
         rewinding = select.isRewinding();
 
         value = select.getNextOptionId();
 
         if (rewinding)
         {
             if (!select.isDisabled())
                 selectedBinding.setBoolean(select.isSelected(value));
         }
         else
         {
             writer.beginEmpty("option");
 
             writer.attribute("value", value);
 
             if (selectedBinding.getBoolean())
                 writer.attribute("selected");
 
             generateAttributes(writer, cycle);
 
            if (label != null)
                 writer.print(label);
 
             writer.println();
         }
 
     }
 
     public void setSelectedBinding(IBinding value)
     {
         selectedBinding = value;
     }
     public String getLabel()
     {
         return label;
     }
 
     public void setLabel(String label)
     {
         this.label = label;
     }
 
 }
