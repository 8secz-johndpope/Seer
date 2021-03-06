 /**
  * Most of the code in the Qalingo project is copyrighted Hoteia and licensed
  * under the Apache License Version 2.0 (release version 0.7.0)
  *         http://www.apache.org/licenses/LICENSE-2.0
  *
  *                   Copyright (c) Hoteia, 2012-2013
  * http://www.hoteia.com - http://twitter.com/hoteia - contact@hoteia.com
  *
  */
 package org.hoteia.qalingo.web.mvc.controller.catalog;
 
 import java.util.List;
 import java.util.Locale;
 
 import javax.servlet.http.HttpServletRequest;
 
 
 
 
 
 //import org.hibernate.engine.internal.Collections;
 import org.hoteia.qalingo.core.ModelConstants;
 import org.hoteia.qalingo.core.RequestConstants;
 import org.hoteia.qalingo.core.domain.CatalogCategoryVirtual;
 import org.hoteia.qalingo.core.domain.MarketArea;
 import org.hoteia.qalingo.core.domain.enumtype.FoUrls;
 import org.hoteia.qalingo.core.pojo.RequestData;
 import org.hoteia.qalingo.core.service.CatalogCategoryService;
 import org.hoteia.qalingo.core.web.mvc.viewbean.CatalogCategoryViewBean;
 import org.hoteia.qalingo.core.web.mvc.viewbean.ProductMarketingViewBean;
 import org.hoteia.qalingo.core.web.servlet.ModelAndViewThemeDevice;
 import org.hoteia.qalingo.web.mvc.controller.AbstractMCommerceController;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.stereotype.Controller;
 import org.springframework.ui.Model;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.servlet.ModelAndView;
 
 
 /**
  * 
  */
 @Controller("productLineController")
 public class ProductLineController extends AbstractMCommerceController {
 
 	@Autowired
 	protected CatalogCategoryService productCategoryService;
 	
 	@RequestMapping(FoUrls.CATEGORY_AS_LINE_URL)
 	public ModelAndView productLine(final HttpServletRequest request, final Model model, @PathVariable(RequestConstants.URL_PATTERN_CATEGORY_CODE) final String categoryCode) throws Exception {
 		ModelAndViewThemeDevice modelAndView = new ModelAndViewThemeDevice(getCurrentVelocityPath(request), FoUrls.CATEGORY_AS_LINE.getVelocityPage());
         final RequestData requestData = requestUtil.getRequestData(request);
         final MarketArea currentMarketArea = requestData.getMarketArea();
         final Locale locale = requestData.getLocale();
         
        String sortBy = request.getParameter("sortBy");
        String orderBy = request.getParameter("orderBy");
         String paraItem = request.getParameter("item");
         String paraPaged = request.getParameter("paged"); 
        
         
 		final CatalogCategoryVirtual productCategory = productCategoryService.getVirtualCatalogCategoryByCode(currentMarketArea.getId(), categoryCode);
 		
 		String seoPageMetaKeywords = coreMessageSource.getMessage("page.meta.keywords", locale);
         model.addAttribute("seoPageMetaKeywords", seoPageMetaKeywords);
 
 		String seoPageMetaDescription = coreMessageSource.getMessage("page.meta.description", locale);
         model.addAttribute("seoPageMetaDescription", seoPageMetaDescription);
 
 		String pageTitleKey = "header.title." + "";
 		String seoPageTitle = coreMessageSource.getMessage("page.title.prefix", locale) + " - " + coreMessageSource.getMessage(pageTitleKey, locale);
         model.addAttribute("seoPageTitle", seoPageTitle);
         
 		final CatalogCategoryViewBean productCategoryViewBean = frontofficeViewBeanFactory.buildCatalogCategoryViewBean(requestUtil.getRequestData(request), productCategory);
 		
 		int page = 1;
 	    int item = 1;
 	    int total = 0;
 		try { 
 			page = Integer.parseInt(paraPaged);
 		    item = Integer.parseInt(paraItem);
 		   
 	    } catch(NumberFormatException e) { 
 	        //return false; 
 	    }
 		
 		List<ProductMarketingViewBean> productMarketings = productCategoryViewBean.getProductMarketings();
 		int size = productMarketings.size();
 		total = size/ item;
 		if(size%item != 0){
 			total = total + 1;
 		}
 		if(page > total){
 			page = total ;
 		}
 		int itemTo = page * item;
 		int itemFrom = itemTo - item;
 		if(itemTo>size){
 			itemTo = size;
 		}
 		if(size > 0){
 			productMarketings = productMarketings.subList(itemFrom, itemTo);
 			productCategoryViewBean.setProductMarketings(productMarketings);
 		}
 		
 		final List<CatalogCategoryViewBean> catalogCategoryViewBeans = frontofficeViewBeanFactory.buildListRootCatalogCategories(requestUtil.getRequestData(request), currentMarketArea);
 		model.addAttribute("catalogCategories", catalogCategoryViewBeans);
 		model.addAttribute(ModelConstants.CATALOG_CATEGORY_VIEW_BEAN, productCategoryViewBean);
 		model.addAttribute("sortBy",sortBy);
 		model.addAttribute("item",item);
		model.addAttribute("orderBy",orderBy);
 		model.addAttribute("pagesCurrent",page);
 		model.addAttribute("totalPage",total);
 
 		return modelAndView;
 	}
     
 }
