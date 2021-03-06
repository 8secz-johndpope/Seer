 /* Copyright c 2005-2012.
  * Licensed under GNU  LESSER General Public License, Version 3.
  * http://www.gnu.org/licenses
  */
 package org.beangle.struts2.view;
 
 import static org.beangle.struts2.util.TextResourceHelper.getText;
 
 import java.io.StringWriter;
 import java.util.Enumeration;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.apache.commons.lang3.StringEscapeUtils;
 import org.beangle.commons.collection.page.Page;
 import org.beangle.struts2.view.component.*;
 
 import com.opensymphony.xwork2.inject.Inject;
 import com.opensymphony.xwork2.util.ValueStack;
 
 public class BeangleTagLibrary extends AbstractTagLibrary {
 
   private ActionUrlRender render;
 
   public BeangleTagLibrary() {
     super();
   }
 
   public BeangleTagLibrary(ValueStack stack, HttpServletRequest req, HttpServletResponse res) {
     super(stack, req, res);
   }
 
   public Object getFreemarkerModels(ValueStack stack, HttpServletRequest req, HttpServletResponse res) {
     BeangleTagLibrary models = new BeangleTagLibrary(stack, req, res);
     models.render = this.render;
     return models;
   }
 
   @Inject
   public void setRender(ActionUrlRender render) {
     this.render = render;
   }
 
   public String url(String url) {
     return render.render(req.getRequestURI(), url);
   }
 
   public java.util.Date getNow() {
     return new java.util.Date();
   }
 
   /**
    * query string and form control
    */
   public String getParamstring() {
     StringWriter sw = new StringWriter();
     Enumeration<?> em = req.getParameterNames();
     while (em.hasMoreElements()) {
       String attr = (String) em.nextElement();
       String value = req.getParameter(attr);
       if (attr.equals("x-requested-with")) {
         continue;
       }
       sw.write(attr);
       sw.write('=');
       sw.write(StringEscapeUtils.escapeEcmaScript(value));
       if (em.hasMoreElements()) {
         sw.write('&');
       }
     }
     return sw.toString();
   }
 
   public boolean isPage(Object data) {
     return data instanceof Page<?>;
   }
 
   public String text(String name) {
     // long start = System.currentTimeMillis();
     String msg = getText(name, name, stack);
     // System.out.println("I18n:" + name + "->" + msg + " use :" + (System.currentTimeMillis() -
     // start));
     return msg;
   }
 
   public String text(String name, Object arg0) {
     return getText(name, name, stack, arg0);
   }
 
   public String text(String name, Object arg0, Object arg1) {
     return getText(name, name, stack, arg0, arg1);
   }
 
  /**
   * Return useragent component.
   */
  public TagModel getAgent() {
    return get(Agent.class);
  }

   public TagModel getHead() {
     return get(Head.class);
   }
 
   public TagModel getDialog() {
     return get(Dialog.class);
   }
 
   public TagModel getCss() {
     return get(Css.class);
   }
 
   public TagModel getIframe() {
     return get(Iframe.class);
   }
 
   public TagModel getFoot() {
     return get(Foot.class);
   }
 
   public TagModel getForm() {
     return get(Form.class);
   }
 
   public TagModel getFormfoot() {
     return get(Formfoot.class);
   }
 
   public TagModel getSubmit() {
     return get(Submit.class);
   }
 
   public TagModel getReset() {
     return get(Reset.class);
   }
 
   public TagModel getToolbar() {
     return get(Toolbar.class);
   }
 
   public TagModel getTabs() {
     return get(Tabs.class);
   }
 
   public TagModel getTab() {
     return get(Tab.class);
   }
 
   public TagModel getGrid() {
     return get(Grid.class);
   }
 
   public TagModel getGridbar() {
     return get(Grid.Bar.class);
   }
 
   public TagModel getGridfilter() {
     return get(Grid.Filter.class);
   }
 
   public TagModel getRow() {
     return get(Grid.Row.class);
   }
 
   public TagModel getCol() {
     TagModel model = models.get(Grid.Col.class);
     if (null == model) {
       // just for performance
       model = new TagModel(stack) {
         @Override
         protected Component getBean() {
           return new Grid.Col(stack);
         }
       };
       models.put(Grid.Col.class, model);
     }
     return model;
   }
 
   public TagModel getTreecol() {
     return get(Grid.Treecol.class);
   }
 
   public TagModel getBoxcol() {
     return get(Grid.Boxcol.class);
   }
 
   public TagModel getPagebar() {
     return get(Pagebar.class);
   }
 
   public TagModel getPassword() {
     return get(Password.class);
   }
 
   public TagModel getA() {
     TagModel model = models.get(Anchor.class);
     if (null == model) {
       model = new TagModel(stack) {
         protected Component getBean() {
           return new Anchor(stack);
         }
       };
       models.put(Anchor.class, model);
     }
     return model;
   }
 
   public TagModel getMessages() {
     return get(Messages.class);
   }
 
   public TagModel getTextfield() {
     return get(Textfield.class);
   }
 
   public TagModel getEmail() {
     return get(Email.class);
   }
 
   public TagModel getTextarea() {
     return get(Textarea.class);
   }
 
   public TagModel getField() {
     return get(Field.class);
   }
 
   public TagModel getTextfields() {
     return get(Textfields.class);
   }
 
   public TagModel getDatepicker() {
     return get(Date.class);
   }
 
   public TagModel getDiv() {
     return get(Div.class);
   }
 
   public TagModel getSelect() {
     return get(Select.class);
   }
 
   public TagModel getSelect2() {
     return get(Select2.class);
   }
 
   public TagModel getModule() {
     return get(Module.class);
   }
 
   public TagModel getNavmenu() {
     return get(Navmenu.class);
   }
 
   public TagModel getNavitem() {
     return get(Navitem.class);
   }
 
   public TagModel getRadio() {
     return get(Radio.class);
   }
 
   public TagModel getRadios() {
     return get(Radios.class);
   }
 
   public TagModel getRecaptcha() {
     return get(Recaptcha.class);
   }
 
   public TagModel getStartend() {
     return get(Startend.class);
   }
 
   public TagModel getCheckbox() {
     return get(Checkbox.class);
   }
 
   public TagModel getCheckboxes() {
     return get(Checkboxes.class);
   }
 
   public TagModel getValidity() {
     return get(Validity.class);
   }
 }
