 package com.ocpsoft.socialpm.gwt.client.local.template;
 
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.uibinder.client.UiBinder;
 import com.google.gwt.uibinder.client.UiField;
 import com.google.gwt.user.client.ui.Composite;
 import com.google.gwt.user.client.ui.HTMLPanel;
 import com.google.gwt.user.client.ui.Widget;
 
 /**
  * A splash screen
  */
 public class NavBar extends Composite
 {
    interface NavBarBinder extends UiBinder<Widget, NavBar>
    {
    }
 
    private static NavBarBinder binder = GWT.create(NavBarBinder.class);
 
    @UiField
    HTMLPanel navbar;
 
    @UiField
    HTMLPanel brand;
 
    @UiField
    HTMLPanel right;
 
    @UiField
    UnorderedList list;
 
    public NavBar()
    {
       initWidget(binder.createAndBindUi(this));
    }
 
    public NavBar setFixedTop(boolean enabled)
    {
       if (enabled)
          navbar.addStyleName("navbar-fixed-top");
       else
          navbar.removeStyleName("navbar-fixed-stop");
       return this;
    }
 
   public NavBar addBrand(Widget link)
    {
       link.addStyleName("brand");
       brand.add(link);
       return this;
    }
 
    public NavBar add(Widget w)
    {
       return add(w, false);
    }
 
    public NavBar add(Widget w, boolean active)
    {
       Widget li = w;
       if (!(w instanceof ListItem))
       {
          li = new ListItem(w);
       }
 
       if (active)
          li.setStyleName("active");
 
       list.add(li);
       return this;
    }
 
    public NavBar addRight(Widget w)
    {
       right.add(w);
       return this;
    }
 
 }
