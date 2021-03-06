 package org.huamuzhen.oa.server.controller;
 
 import java.util.List;
 
 import javax.annotation.Resource;
 import javax.servlet.http.HttpServletRequest;
 
 import org.huamuzhen.oa.biz.OrgUnitManager;
 import org.huamuzhen.oa.domain.entity.OrgUnit;
 import org.springframework.stereotype.Controller;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestMethod;
 import org.springframework.web.servlet.ModelAndView;
 
 @Controller
 @RequestMapping("/orgUnit")
 public class OrgUnitController {
 	
 	@Resource
 	private OrgUnitManager orgUnitManager;
 	
 	@RequestMapping(value = { "", "/" })
 	public ModelAndView list(){
 		List<OrgUnit> orgUnitList = orgUnitManager.findAllOrgUnit();
 		ModelAndView mav = new ModelAndView("orgUnit");
 		mav.addObject("orgUnitList", orgUnitList);
 		return mav;
 	}
 	
 	@RequestMapping(value="/add",method=RequestMethod.POST)
 	public String add(HttpServletRequest request){	
 		String name = request.getParameter("name");
 		String description = request.getParameter("description");
 		String parentId = request.getParameter("parentId");
 		orgUnitManager.saveOrgUnit(null, name, description, parentId);
 		
 		return "redirect:/orgUnit";
 	}
 	
 	@RequestMapping(value="/edit",method=RequestMethod.POST)
 	public String edit(HttpServletRequest request){
 		String id = request.getParameter("id");
 		String name = request.getParameter("name");
 		String description = request.getParameter("description");
 		String parentId = request.getParameter("parentId");
 		orgUnitManager.saveOrgUnit(id, name, description, parentId);
 		
 		return "redirect:/orgUnit";
 	}
 	
 	@RequestMapping(value="/delete/{id}",method=RequestMethod.POST)
 	public String delete(@PathVariable String id){
 		
 		orgUnitManager.deleteOrgUnit(id);
 		return "redirect:/orgUnit";
 	}
 	
 	@RequestMapping(value="/addOrgUnit")
	public String addUser(){
		return "addOrgUnit";
 	}
 	
 	@RequestMapping(value="/editOrgUnit/{id}")
 	public ModelAndView editUser(@PathVariable String id){
 		ModelAndView mav = new ModelAndView("editOrgUnit");
		OrgUnit orgUnit = orgUnitManager.findOne(id);
		mav.addObject("orgUnit", orgUnit);
 		return mav;
 	}
 
 }
