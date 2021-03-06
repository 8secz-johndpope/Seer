 /* Copyright c 2005-2012.
  * Licensed under GNU  LESSER General Public License, Version 3.
  * http://www.gnu.org/licenses
  */
 package org.beangle.struts2.view;
 
 import java.util.List;
 import java.util.Map;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import javax.servlet.http.HttpSession;
 
 import org.apache.struts2.views.TagLibrary;
 import org.beangle.commons.collection.CollectUtils;
 import org.beangle.struts2.view.component.Component;
 import org.beangle.struts2.view.template.Theme;
 
 import com.opensymphony.xwork2.util.ValueStack;
 
 /**
  * Abstract Freemarker Tag Library
  * 
  * @author chaostone
  */
 public abstract class AbstractTagLibrary implements TagLibrary {
   protected ValueStack stack;
   protected HttpServletRequest req;
   protected HttpServletResponse res;
   protected Map<Class<?>, TagModel> models = CollectUtils.newHashMap();
 
   protected UITheme theme;
 
   public AbstractTagLibrary() {
 
   }
 
   public AbstractTagLibrary(ValueStack stack, HttpServletRequest req, HttpServletResponse res) {
     super();
     this.stack = stack;
     this.req = req;
     this.res = res;
 
     theme = UITheme.getTheme(getUitheme(), req.getContextPath());
    stack.getContext().put(Theme.THEME, Theme.getTheme(Theme.DEFAULT_THEME));
    stack.getContext().put(UIIdGenerator.GENERATOR,
        new IndexableIdGenerator(Math.abs(req.getRequestURI().hashCode())));
   }
 
   protected String getUitheme() {
     String uitheme = req.getParameter("ui.theme");
     if (null == uitheme) {
       HttpSession session = req.getSession(false);
       if (null != session && null != session.getAttribute("ui.theme")) {
         uitheme = (String) session.getAttribute("ui.theme");
       }
     }
     if (null == uitheme) uitheme = "default";
     return uitheme;
   }
 
   protected TagModel get(final Class<? extends Component> clazz) {
     TagModel model = models.get(clazz);
     if (null == model) {
       model = new TagModel(stack, clazz);
       models.put(clazz, model);
     }
     return model;
   }
 
   @SuppressWarnings("rawtypes")
   public List<Class> getVelocityDirectiveClasses() {
     return null;
   }
 
   public UITheme getTheme() {
     return theme;
   }
 
 }
