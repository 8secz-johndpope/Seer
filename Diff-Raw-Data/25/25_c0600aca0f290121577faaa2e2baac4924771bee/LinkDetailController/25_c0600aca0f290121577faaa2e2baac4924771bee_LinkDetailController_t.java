 package cn.edu.sdufe.cms.common.web.link;
 
 import cn.edu.sdufe.cms.common.entity.link.Link;
 import cn.edu.sdufe.cms.common.service.link.LinkManager;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.annotation.Qualifier;
 import org.springframework.stereotype.Controller;
 import org.springframework.ui.Model;
 import org.springframework.web.bind.annotation.ModelAttribute;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestMethod;
 import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
 import javax.validation.Valid;
 
 /**
  * link 指定id控制器
  * User: pengfei.dongpf@gmail.com
  * Date: 12-4-8
  * Time: 下午7:03
  */
 @Controller
 @RequestMapping(value = "/link")
 public class LinkDetailController {
     private LinkManager linkManager;
 
     /**
      * 删除编号为id的link
      *
      * @param id
      */
     @RequestMapping(value = "delete/{id}")
     public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
         linkManager.delete(id);
         if (null == linkManager.getLink(id)) {
             redirectAttributes.addFlashAttribute("info", "删除链接成功");
         }
         return "redirect:/link/listAll";
     }
 
     /**
      * 跳转到修改link页面
      *
      * @param id
      * @param model
      * @return
      */
     @RequestMapping(value = "edit/{id}")
     public String edit(@PathVariable Long id, Model model) {
         Link link = linkManager.getLink(id);
         model.addAttribute("link", link);
         return "dashboard/link/edit";
     }
 
     /**
      * 保存修改的link
      *
      * @param id
      * @param link
      * @param redirectAttributes
      * @return
      */
     @RequestMapping(value = "save/{id}")
     public String save(@PathVariable Long id, @Valid @ModelAttribute Link link, RedirectAttributes redirectAttributes) {
         linkManager.update(link);
         return "redirect:/link/listAll";
     }
 
     /**
      * 审核编号为id的评论
      *
      * @return
      */
     @RequestMapping(value = "audit/{id}", method = RequestMethod.GET)
     public String auditComment(@PathVariable("id") Long id, @ModelAttribute("link") Link link, RedirectAttributes redirectAttributes) {
         link.setStatus(!link.isStatus());
         if (linkManager.update(link) > 0) {
             redirectAttributes.addFlashAttribute("error", "操作友情链接 " + id + " 失败.");
             return "redirect:/link/listAll";
         }
         if (link.isStatus()) {
             redirectAttributes.addFlashAttribute("info", "审核友情链接" + id + " 成功.");
         } else {
             redirectAttributes.addFlashAttribute("info", "反审核友情链接" + id + " 成功.");
         }
         return "redirect:/link/listAll";
     }
 
     @ModelAttribute("link")
     public Link getLink(@PathVariable Long id) {
         return linkManager.getLink(id);
     }
 
     @Autowired
     public void setLinkManager(@Qualifier("linkManager") LinkManager linkManager) {
         this.linkManager = linkManager;
     }
 }
