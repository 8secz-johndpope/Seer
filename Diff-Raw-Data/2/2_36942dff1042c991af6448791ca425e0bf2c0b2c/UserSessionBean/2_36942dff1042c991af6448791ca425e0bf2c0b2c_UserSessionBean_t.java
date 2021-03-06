 
 package gov.nih.nci.evs.browser.bean;
 
 import gov.nih.nci.evs.browser.utils.MailUtils;
 import gov.nih.nci.evs.browser.utils.SearchUtils;
 import gov.nih.nci.evs.browser.utils.Utils;
 
 import gov.nih.nci.evs.browser.properties.NCImBrowserProperties;
 
 import java.io.UnsupportedEncodingException;
 import java.net.URLDecoder;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Vector;
 
 import javax.faces.context.FacesContext;
 import javax.faces.event.ValueChangeEvent;
 import javax.faces.model.SelectItem;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.apache.log4j.Logger;
 
 import org.LexGrid.concepts.Concept;
 
 import gov.nih.nci.evs.browser.utils.*;
 import gov.nih.nci.evs.browser.common.Constants;
 import gov.nih.nci.evs.searchlog.SearchLog;
 
 import org.LexGrid.LexBIG.DataModel.Core.ResolvedConceptReference;
 import org.LexGrid.LexBIG.Utility.Iterators.ResolvedConceptReferencesIterator;
 import gov.nih.nci.evs.browser.common.Constants;
 
 /**
   * <!-- LICENSE_TEXT_START -->
 * Copyright 2008,2009 NGIT. This software was developed in conjunction with the National Cancer Institute,
 * and so to the extent government employees are co-authors, any rights in such works shall be subject to Title 17 of the United States Code, section 105.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the disclaimer of Article 3, below. Redistributions
 * in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 2. The end-user documentation included with the redistribution, if any, must include the following acknowledgment:
 * "This product includes software developed by NGIT and the National Cancer Institute."
 * If no such end-user documentation is to be included, this acknowledgment shall appear in the software itself,
 * wherever such third-party acknowledgments normally appear.
 * 3. The names "The National Cancer Institute", "NCI" and "NGIT" must not be used to endorse or promote products derived from this software.
 * 4. This license does not authorize the incorporation of this software into any third party proprietary programs. This license does not authorize
 * the recipient to use any trademarks owned by either NCI or NGIT
 * 5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 * NGIT, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
   * <!-- LICENSE_TEXT_END -->
   */
 
 /**
   * @author EVS Team
   * @version 1.0
   *
   * Modification history
   *     Initial implementation kim.ong@ngc.com
   *
  */
 
 public class UserSessionBean extends Object
 {
 	private static Logger logger = Logger.getLogger(UserSessionBean.class);
 
     private String selectedQuickLink = null;
     private List quickLinkList = null;
 
     public void setSelectedQuickLink(String selectedQuickLink) {
         this.selectedQuickLink = selectedQuickLink;
         HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
         request.getSession().setAttribute("selectedQuickLink", selectedQuickLink);
     }
 
     public String getSelectedQuickLink() {
         return this.selectedQuickLink;
     }
 
 
     public void quickLinkChanged(ValueChangeEvent event) {
         if (event.getNewValue() == null) return;
         String newValue = (String) event.getNewValue();
 
         System.out.println("quickLinkChanged; " + newValue);
         setSelectedQuickLink(newValue);
 
         HttpServletResponse response = (HttpServletResponse)FacesContext.getCurrentInstance().getExternalContext().getResponse();
 
         String targetURL = null;//"http://nciterms.nci.nih.gov/";
         if (selectedQuickLink.compareTo("NCI Terminology Browser") == 0) {
            targetURL = "http://nciterms.nci.nih.gov/";
         }
         try {
             response.sendRedirect(response.encodeRedirectURL(targetURL));
         } catch (Exception ex) {
             ex.printStackTrace();
             // send error message
         }
 
     }
 
 
 
     public List getQuickLinkList() {
         quickLinkList = new ArrayList();
         quickLinkList.add(new SelectItem("Quick Links"));
         quickLinkList.add(new SelectItem("NCI Terminology Browser"));
         quickLinkList.add(new SelectItem("EVS Home"));
         quickLinkList.add(new SelectItem("NCI Terminology Resources"));
         return quickLinkList;
     }
 
 
 
     public String advancedSearchAction() {
 		ResolvedConceptReferencesIteratorWrapper wrapper = null;
         HttpServletRequest request = (HttpServletRequest)FacesContext.
             getCurrentInstance().getExternalContext().getRequest();
 
 		SearchStatusBean bean = (SearchStatusBean) FacesContext.
 		    getCurrentInstance().getExternalContext().getRequestMap().
 		    get("searchStatusBean");
 		
 		if (bean == null) {
 			bean = new SearchStatusBean();
 			request.setAttribute("searchStatusBean", bean);
 		}
 
 		String matchText = (String) request.getParameter("matchText");
         if (matchText == null || matchText.length() == 0) {
             String message = "Please enter a search string.";
             request.getSession().setAttribute("message", message);
             return "message";
         }
         matchText = matchText.trim();
         bean.setMatchText(matchText);
 
         String matchAlgorithm = (String) request.getParameter("adv_search_algorithm");
         bean.setAlgorithm(matchAlgorithm);
         
         String source = (String) request.getParameter("adv_search_source");
         bean.setSelectedSource(source);
 
         String selectSearchOption = (String) request.getParameter("selectSearchOption");
         bean.setSelectedSearchOption(selectSearchOption);
 
 		String selectProperty = (String) request.getParameter("selectProperty");
 		bean.setSelectedProperty(selectProperty);
 
 		String rel_search_association = (String) request.getParameter("rel_search_association");
 		bean.setSelectedAssociation(rel_search_association);
         
 		String rel_search_rela = (String) request.getParameter("rel_search_rela");
 		bean.setSelectedRELA(rel_search_rela);
 		
 		FacesContext.getCurrentInstance().getExternalContext().getRequestMap().
 		    put("searchStatusBean", bean);
 		request.setAttribute("searchStatusBean", bean);
 
         String searchTarget = (String) request.getParameter("searchTarget");
 
         if (NCImBrowserProperties.debugOn) {
             System.out.println(Utils.SEPARATOR);
             System.out.println("* criteria: " + matchText);
             System.out.println("* source: " + source);
         }
 
         String scheme = Constants.CODING_SCHEME_NAME;
         Vector schemes = new Vector();
         schemes.add(scheme);
         
         String version = null;
         String max_str = null;
         int maxToReturn = -1;//1000;
         try {
             max_str = NCImBrowserProperties.getInstance().
                 getProperty(NCImBrowserProperties.MAXIMUM_RETURN);
             maxToReturn = Integer.parseInt(max_str);
         } catch (Exception ex) {
         }
         Utils.StopWatch stopWatch = new Utils.StopWatch();
         Vector<org.LexGrid.concepts.Concept> v = null;
 
         boolean excludeDesignation = true;
         boolean designationOnly = false;
 
         // check if this search has been performance previously through IteratorBeanManager
 		IteratorBeanManager iteratorBeanManager = (IteratorBeanManager) 
 		    FacesContext.getCurrentInstance().getExternalContext().
 		    getSessionMap().get("iteratorBeanManager");
 
 		if (iteratorBeanManager == null) {
 			iteratorBeanManager = new IteratorBeanManager();
 			FacesContext.getCurrentInstance().getExternalContext().getSessionMap().
 			    put("iteratorBeanManager", iteratorBeanManager);
 		}
 
         IteratorBean iteratorBean = null;
         ResolvedConceptReferencesIterator iterator = null;
         boolean ranking = true;
 
         SearchFields searchFields = null;
         String key = null;
 
 		String searchType = (String) request.getParameter("selectSearchOption");
 		System.out.println("SearchUtils.java searchType: " + searchType);
 
         if (searchType != null && searchType.compareTo("Property") == 0) {
 /*            
 			System.out.println("Advanced Search: ");
 			System.out.println("searchType: " + searchType);
 			System.out.println("matchText: " + matchText);
 			System.out.println("adv_search_algorithm: " + adv_search_algorithm);
 			System.out.println("adv_search_source: " + adv_search_source);
 */
 
 			String property_type = (String) request.getParameter("selectPropertyType");
 			if (property_type != null && property_type.compareTo("ALL") == 0) {
 				property_type = null;
 			}
 
 			String property_name = selectProperty;
             if (property_name != null) {
 				property_name = property_name.trim();
 				//if (property_name.length() == 0) property_name = null;
 				if (property_name.compareTo("ALL") == 0) property_name = null;
 			}
 
 			searchFields = SearchFields.setProperty(
 			    schemes, matchText, searchTarget, property_type, property_name,
 			    source, matchAlgorithm, maxToReturn);
 			key = searchFields.getKey();
 			if (iteratorBeanManager.containsIteratorBean(key)) {
 				iteratorBean = iteratorBeanManager.getIteratorBean(key);
 				iterator = iteratorBean.getIterator();
 			} else {
 				String[] property_types = null;
 				if (property_type != null)
 					property_types = new String[] {property_type};
 				String[] property_names = null;
 				if (property_name != null)
 					property_names = new String[] {property_name};
 				excludeDesignation = false;
                 wrapper = new SearchUtils().searchByProperties(scheme, version,
                     matchText, property_types, property_names, source,
                     matchAlgorithm, excludeDesignation, ranking, maxToReturn);
       	    	if (wrapper != null) {
 					iterator = wrapper.getIterator();
 				}
        	    	if (iterator != null) {
 					iteratorBean = new IteratorBean(iterator);
 					iteratorBean.setKey(key);
 					iteratorBean.setMatchText(matchText);
 					iteratorBeanManager.addIteratorBean(iteratorBean);
 				}
 			}
 
         } else if (searchType != null && searchType.compareTo("Relationship") == 0) {
             if (rel_search_association != null && rel_search_association.compareTo("ALL") == 0)
                 rel_search_association = null;
             
             if (rel_search_rela != null) {
                 rel_search_rela = rel_search_rela.trim();
                 if (rel_search_rela.length() == 0)
                     rel_search_rela = null;
             }
 
 			String rel_search_direction = (String) request.getParameter("rel_search_direction");
 /*
             System.out.println("Advanced Search: ");
             System.out.println("searchType: " + searchType);
             System.out.println("matchText: " + matchText);
             System.out.println("adv_search_algorithm: " + adv_search_algorithm);
             System.out.println("rel_search_association: " + rel_search_association);
             System.out.println("rel_search_rela: " + rel_search_rela);
             System.out.println("adv_search_source: " + adv_search_source);
             System.out.println("rel_search_direction: " + rel_search_direction);
 */
 			
 			//boolean direction = false;
 			int search_direction = Constants.SEARCH_BOTH_DIRECTION;
 			if (rel_search_direction != null && rel_search_direction.compareTo("source") == 0) {
 				search_direction = Constants.SEARCH_SOURCE;
 				//direction = true;
 			} else if (rel_search_direction != null && rel_search_direction.compareTo("target") == 0) {
 				search_direction = Constants.SEARCH_TARGET;
 				//direction = true;
 			}
 
             searchFields = SearchFields.setRelationship(schemes, matchText,
 			    searchTarget, rel_search_association, rel_search_rela,
 			    source, matchAlgorithm, maxToReturn);
             key = searchFields.getKey();
 
 			if (iteratorBeanManager.containsIteratorBean(key)) {
 				iteratorBean = iteratorBeanManager.getIteratorBean(key);
 				iterator = iteratorBean.getIterator();
 			} else {
 
 				String[] associationsToNavigate = null;
 				String[] association_qualifier_names = null;
 				String[] association_qualifier_values = null;
 
 				if (rel_search_association != null) {
 					associationsToNavigate = new String[] {rel_search_association};
 				} else {
 					System.out.println("(*) associationsToNavigate == null");
 				}
 
 				if (rel_search_rela != null) {
 					association_qualifier_names = new String[] {"rela"};
 					association_qualifier_values = new String[] {rel_search_rela};
 
 					if (associationsToNavigate == null) {
 						Vector w = OntologyBean.getAssociationNames();
 						if (w == null || w.size() == 0) {
 							System.out.println("OntologyBean.getAssociationNames() returns null, or nothing???");
 						} else {
 							associationsToNavigate = new String[w.size()];
 							for (int i=0; i<w.size(); i++) {
 								String nm = (String) w.elementAt(i);
 								associationsToNavigate[i] = nm;
 							}
 					    }
                     }
 
 				} else {
 					System.out.println("(*) qualifiers == null");
 				}
 
                 wrapper = new SearchUtils().searchByAssociations(
                     scheme, version, matchText, associationsToNavigate,
                     association_qualifier_names, association_qualifier_values,
                     search_direction, source, matchAlgorithm,
                         excludeDesignation, ranking, maxToReturn);
       	    	if (wrapper != null) {
 					iterator = wrapper.getIterator();
 				}
        	    	if (iterator != null) {
 					iteratorBean = new IteratorBean(iterator);
 					iteratorBean.setKey(key);
 					iteratorBean.setMatchText(matchText);
 					iteratorBeanManager.addIteratorBean(iteratorBean);
 				}
 			}
 
 		} else if (searchType != null && searchType.compareTo("Name") == 0) {
 		    searchFields = SearchFields.setName(schemes, matchText, 
 		        searchTarget, source, matchAlgorithm, maxToReturn);
             key = searchFields.getKey();
 			if (iteratorBeanManager.containsIteratorBean(key)) {
 				iteratorBean = iteratorBeanManager.getIteratorBean(key);
 				iterator = iteratorBean.getIterator();
 			} else {
 				wrapper =
 					new SearchUtils().searchByName(scheme, version, matchText, 
 					    source, matchAlgorithm, ranking, maxToReturn,
 					    SearchUtils.NameSearchType.Name);
 				iterator = wrapper.getIterator();
 				if (iterator != null) {
 					iteratorBean = new IteratorBean(iterator);
 					iteratorBean.setKey(key);
 					iteratorBean.setMatchText(matchText);
 					iteratorBeanManager.addIteratorBean(iteratorBean);
 				}
 			}
 
         } else if (searchType != null && searchType.compareTo("Code") == 0) {
             searchFields = SearchFields.setCode(schemes, matchText, searchTarget, 
                 source, matchAlgorithm, maxToReturn);
             key = searchFields.getKey();
             if (iteratorBeanManager.containsIteratorBean(key)) {
                 iteratorBean = iteratorBeanManager.getIteratorBean(key);
                 iterator = iteratorBean.getIterator();
             } else {
                 wrapper =
                     new SearchUtils().searchByName(scheme, version, matchText, 
                         source, matchAlgorithm, ranking, maxToReturn,
                         SearchUtils.NameSearchType.Code);
                 iterator = wrapper.getIterator();
                 if (iterator != null) {
                     iteratorBean = new IteratorBean(iterator);
                     iteratorBean.setKey(key);
                     iteratorBean.setMatchText(matchText);
                     iteratorBeanManager.addIteratorBean(iteratorBean);
                 }
             }
         }
 
         request.setAttribute("key", key);
         request.setAttribute("matchText", matchText);
 
         request.getSession().removeAttribute("neighborhood_synonyms");
         request.getSession().removeAttribute("neighborhood_atoms");
         request.getSession().removeAttribute("concept");
         request.getSession().removeAttribute("code");
         request.getSession().removeAttribute("codeInNCI");
         request.getSession().removeAttribute("AssociationTargetHashMap");
         request.getSession().removeAttribute("type");
 
         if (iterator != null) {
 			int size = iteratorBean.getSize();
 
 			// Write a search log entry
 			SearchLog.writeEntry(searchFields,
 			    size, HTTPUtils.getRefererParmDecode(request));
 
 			if (size > 1) {
 				request.getSession().setAttribute("search_results", v);
 
 				String match_size = Integer.toString(size);;//Integer.toString(v.size());
 				request.getSession().setAttribute("match_size", match_size);
 				request.getSession().setAttribute("page_string", "1");
 
 				request.getSession().setAttribute("new_search", Boolean.TRUE);
 				return "search_results";
 		    } else if (size == 1) {
 				request.getSession().setAttribute("singleton", "true");
 				request.getSession().setAttribute("dictionary", Constants.CODING_SCHEME_NAME);
 				//Concept c = (Concept) v.elementAt(0);
 				int pageNumber = 1;
 				List list = iteratorBean.getData(1);
 				ResolvedConceptReference ref = (ResolvedConceptReference) list.get(0);
 
 				Concept c = null;
 				if (ref == null) {
 					String msg = "Error: Null ResolvedConceptReference encountered.";
 					request.getSession().setAttribute("message", msg);
 					return "message";
 
 				} else {
 					c = ref.getReferencedEntry();
 					if (c == null) {
 						c = DataUtils.getConceptByCode(Constants.CODING_SCHEME_NAME, 
 						    null, null, ref.getConceptCode());
 					}
 				}
 
 				request.getSession().setAttribute("code", ref.getConceptCode());
 				request.getSession().setAttribute("concept", c);
 				request.getSession().setAttribute("type", "properties");
 
 				request.getSession().setAttribute("new_search", Boolean.TRUE);
 				return "concept_details";
 		    }
 		}
 
         //[#23463] Linking retired concept to corresponding new concept
         // Test case: C0536142|200601|SY|||C1433544|Y|
 
         if (matchAlgorithm.compareTo("exactMatch") == 0) {
 			String newCUI = HistoryUtils.getReferencedCUI(matchText);
 
 			if (newCUI != null) {
 				System.out.println("Searching for " + newCUI);
 				Concept c = DataUtils.getConceptByCode(
 				    Constants.CODING_SCHEME_NAME, null, null, newCUI);
 				request.getSession().setAttribute("code", newCUI);
 				request.getSession().setAttribute("concept", c);
 				request.getSession().setAttribute("type", "properties");
 
 				request.getSession().setAttribute("new_search", Boolean.TRUE);
 				request.getSession().setAttribute("retired_cui", matchText);
 				return "concept_details";
 			}
 	    }
 
         String message = "No match found.";
         if (matchAlgorithm.compareTo("exactMatch") == 0) {
             message = Constants.ERROR_NO_MATCH_FOUND_TRY_OTHER_ALGORITHMS;
         }
         request.setAttribute("message", message);
         return "no_match";
 
 	}
 
 
 
     public String searchAction() {
 		ResolvedConceptReferencesIteratorWrapper wrapper = null;
         HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
 
 		String matchText = (String) request.getParameter("matchText");
 		System.out.println("matchText: " + matchText);
 
 
         if (matchText != null) {
 			matchText = matchText.trim();
 			request.setAttribute("matchText", matchText);
 		}
 
 
         //[#19965] Error message is not displayed when Search Criteria is not proivded
         if (matchText == null || matchText.length() == 0)
         {
             String message = "Please enter a search string.";
             request.getSession().setAttribute("message", message);
             return "message";
         }
         //request.getSession().setAttribute("matchText", matchText);
         //request.setAttribute("matchText", matchText);
 
         String matchAlgorithm = (String) request.getParameter("algorithm");
         setSelectedAlgorithm(matchAlgorithm);
         String matchtype = (String) request.getParameter("matchtype");
         if (matchtype == null) matchtype = "string";
 
         String searchTarget = (String) request.getParameter("searchTarget");
         request.getSession().setAttribute("searchTarget", searchTarget);
 
         //Remove ranking check box (KLO, 092409)
         //String rankingStr = (String) request.getParameter("ranking");
         //boolean ranking = rankingStr != null && rankingStr.equals("on");
         //request.getSession().setAttribute("ranking", Boolean.toString(ranking));
 
         String source = (String) request.getParameter("source");
         if (source == null) {
             source = "ALL";
         }
         //request.getSession().setAttribute("source", source);
         setSelectedSource(source);
 
         if (NCImBrowserProperties.debugOn) {
             try {
                 System.out.println(Utils.SEPARATOR);
                 System.out.println("* criteria: " + matchText);
                 System.out.println("* matchType: " + matchtype);
                 System.out.println("* source: " + source);
                 //System.out.println("* ranking: " + ranking);
                // System.out.println("* sortOption: " + sortOption);
             } catch (Exception e) {
             }
         }
 
         String scheme = Constants.CODING_SCHEME_NAME;
         String version = null;
         String max_str = null;
         int maxToReturn = -1;//1000;
         try {
             max_str = NCImBrowserProperties.getInstance().getProperty(NCImBrowserProperties.MAXIMUM_RETURN);
             maxToReturn = Integer.parseInt(max_str);
         } catch (Exception ex) {
 
         }
         Utils.StopWatch stopWatch = new Utils.StopWatch();
         Vector<org.LexGrid.concepts.Concept> v = null;
 
 
         boolean excludeDesignation = true;
         boolean designationOnly = false;
 
         // check if this search has been performance previously through IteratorBeanManager
 		IteratorBeanManager iteratorBeanManager = (IteratorBeanManager) FacesContext.getCurrentInstance().getExternalContext()
 			.getSessionMap().get("iteratorBeanManager");
 
 		if (iteratorBeanManager == null) {
 			iteratorBeanManager = new IteratorBeanManager();
 			FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("iteratorBeanManager", iteratorBeanManager);
 		}
 
         IteratorBean iteratorBean = null;
         ResolvedConceptReferencesIterator iterator = null;
         Vector schemes = new Vector();
         schemes.add(scheme);
         boolean ranking = true;
 
         SearchFields searchFields = null;
         String key = null;
 
 
 		String searchType = (String) request.getParameter("selectSearchOption");
 		//System.out.println("SearchUtils.java searchType: " + searchType);
 
 
         if (searchType != null && searchType.compareTo("Property") == 0) {
 			String adv_search_algorithm = (String) request.getParameter("adv_search_algorithm");
 			matchAlgorithm = adv_search_algorithm;
 			String adv_search_source = (String) request.getParameter("adv_search_source");
 			/*
 			System.out.println("Advanced Search: ");
 			System.out.println("searchType: " + searchType);
 			System.out.println("matchText: " + matchText);
 			System.out.println("adv_search_algorithm: " + adv_search_algorithm);
 			System.out.println("adv_search_source: " + adv_search_source);
 			*/
 
 			if (adv_search_source != null && adv_search_source.compareTo("ALL") == 0) {
 				adv_search_source = null;
 			}
 
 			String property_type = (String) request.getParameter("selectPropertyType");
 			if (property_type != null && property_type.compareTo("ALL") == 0) {
 				property_type = null;
 			}
 
 			String property_name = (String) request.getParameter("selectProperty");
 
             if (property_name != null) {
 				property_name = property_name.trim();
 				//if (property_name.length() == 0) property_name = null;
 				if (property_name.compareTo("ALL") == 0) property_name = null;
 			}
 
 			//System.out.println("adv_search_selected_property: " + property_name);
 			if (adv_search_source == null) {
 				request.getSession().setAttribute("adv_search_source", "ALL");
 			} else {
 				request.getSession().setAttribute("adv_search_source", adv_search_source);
 			}
 			if (property_type == null) {
 				request.getSession().setAttribute("property_type", "ALL");
 			} else {
 				request.getSession().setAttribute("property_type", property_type);
 			}
 			if (property_name == null) {
 				request.getSession().setAttribute("property_name", " ");
 			} else {
 				request.getSession().setAttribute("property_name", property_name);
 			}
 
 			searchFields = SearchFields.setProperty(
 			    schemes, matchText, searchTarget, property_type, property_name,
 			    adv_search_source, adv_search_algorithm, maxToReturn);
 			key = searchFields.getKey();
 			if (iteratorBeanManager.containsIteratorBean(key)) {
 				iteratorBean = iteratorBeanManager.getIteratorBean(key);
 				iterator = iteratorBean.getIterator();
 			} else {
 				String[] property_types = null;
 				if (property_type != null) {
 					property_types = new String[] {property_type};
 				}
 				String[] property_names = null;
 				if (property_name != null) {
 					property_names = new String[] {property_name};
 				}
 				excludeDesignation = false;
                 wrapper = new SearchUtils().searchByProperties(scheme, version,
                                                                matchText,
 															   property_types,
 															   property_names,
 															   adv_search_source,
 															   adv_search_algorithm,
 															   excludeDesignation, ranking, maxToReturn);
       	    	if (wrapper != null) {
 					iterator = wrapper.getIterator();
 				}
        	    	if (iterator != null) {
 					iteratorBean = new IteratorBean(iterator);
 					iteratorBean.setKey(key);
 					iteratorBean.setMatchText(matchText);
 					iteratorBeanManager.addIteratorBean(iteratorBean);
 				}
 			}
 
         } else if (searchType != null && searchType.compareTo("Relationship") == 0) {
 
 			String adv_search_algorithm = (String) request.getParameter("adv_search_algorithm");
 			matchAlgorithm = adv_search_algorithm;
 			String adv_search_source = (String) request.getParameter("adv_search_source");
 			if (adv_search_source != null && adv_search_source.compareTo("ALL") == 0) {
 				adv_search_source = null;
 			}
 
             String rel_search_association = (String) request.getParameter("rel_search_association");
             if (rel_search_association != null && rel_search_association.compareTo("ALL") == 0) {
 				rel_search_association = null;
 			}
 
             String rel_search_rela = (String) request.getParameter("rel_search_rela");
             if (rel_search_rela != null) {
 				rel_search_rela = rel_search_rela.trim();
 				if (rel_search_rela.length() == 0) rel_search_rela = null;
 		    }
 
 			String rel_search_direction = (String) request.getParameter("rel_search_direction");
 /*
 			System.out.println("Advanced Search: ");
 			System.out.println("searchType: " + searchType);
 			System.out.println("matchText: " + matchText);
 			System.out.println("adv_search_algorithm: " + adv_search_algorithm);
 			System.out.println("rel_search_association: " + rel_search_association);
 			System.out.println("rel_search_rela: " + rel_search_rela);
 			System.out.println("adv_search_source: " + adv_search_source);
 			System.out.println("rel_search_direction: " + rel_search_direction);
 */
 			//boolean direction = false;
 			int search_direction = Constants.SEARCH_BOTH_DIRECTION;
 			if (rel_search_direction != null && rel_search_direction.compareTo("source") == 0) {
 				search_direction = Constants.SEARCH_SOURCE;
 				//direction = true;
 			} else if (rel_search_direction != null && rel_search_direction.compareTo("taret") == 0) {
 				search_direction = Constants.SEARCH_TARGET;
 				//direction = true;
 			}
 
 			request.getSession().setAttribute("adv_search_algorithm", adv_search_algorithm);
 			request.getSession().setAttribute("adv_search_source", adv_search_source);
 
 
 			if (rel_search_association == null) {
 				request.getSession().setAttribute("rel_search_association", "ALL");
 			} else {
 				request.getSession().setAttribute("rel_search_association", rel_search_association);
 			}
 			if (rel_search_rela == null) {
 				request.getSession().setAttribute("rel_search_rela", " ");
 			} else {
 				request.getSession().setAttribute("rel_search_rela", rel_search_rela);
 			}
 			request.getSession().setAttribute("search_direction", rel_search_direction);
 			if (adv_search_source == null) {
 				request.getSession().setAttribute("adv_search_source", "ALL");
 			} else {
 				request.getSession().setAttribute("adv_search_source", adv_search_source);
 			}
 
 
 			searchFields = SearchFields.setRelationship(schemes, matchText,
 			    searchTarget, rel_search_association, rel_search_rela,
 			    adv_search_source, adv_search_algorithm, maxToReturn);
 			key = searchFields.getKey();
 
 			if (iteratorBeanManager.containsIteratorBean(key)) {
 				iteratorBean = iteratorBeanManager.getIteratorBean(key);
 				iterator = iteratorBean.getIterator();
 			} else {
 
 
 				String[] associationsToNavigate = null;
 				String[] association_qualifier_names = null;
 				String[] association_qualifier_values = null;
 
 				if (rel_search_association != null) {
 					associationsToNavigate = new String[] {rel_search_association};
 				} else {
 					System.out.println("(*) associationsToNavigate == null");
 				}
 
 				if (rel_search_rela != null) {
 					association_qualifier_names = new String[] {"rela"};
 					association_qualifier_values = new String[] {rel_search_rela};
 
 					if (associationsToNavigate == null) {
 						Vector w = OntologyBean.getAssociationNames();
 						if (w == null || w.size() == 0) {
 							System.out.println("OntologyBean.getAssociationNames() returns null, or nothing???");
 						} else {
 							associationsToNavigate = new String[w.size()];
 							for (int i=0; i<w.size(); i++) {
 								String nm = (String) w.elementAt(i);
 								associationsToNavigate[i] = nm;
 							}
 					    }
                     }
 
 				} else {
 					System.out.println("(*) qualifiers == null");
 				}
 
                 wrapper = new SearchUtils().searchByAssociations(scheme, version, matchText,
 															   associationsToNavigate,
 															   association_qualifier_names,
 															   association_qualifier_values,
 															   search_direction,
 															   adv_search_source,
 															   adv_search_algorithm,
 															   excludeDesignation,
 															   ranking, maxToReturn);
       	    	if (wrapper != null) {
 					iterator = wrapper.getIterator();
 				}
        	    	if (iterator != null) {
 					iteratorBean = new IteratorBean(iterator);
 					iteratorBean.setKey(key);
 					iteratorBean.setMatchText(matchText);
 					iteratorBeanManager.addIteratorBean(iteratorBean);
 				}
 			}
 
 		} else {
 
 		    searchFields = SearchFields.setSimple(schemes, matchText, searchTarget, source, matchAlgorithm, maxToReturn);
 		    key = searchFields.getKey();
 			if (searchTarget.compareTo("names") == 0) {
 				if (iteratorBeanManager.containsIteratorBean(key)) {
 					iteratorBean = iteratorBeanManager.getIteratorBean(key);
 					iterator = iteratorBean.getIterator();
 				} else {
 					wrapper =
 						new SearchUtils().searchByName(scheme, version, matchText, source, matchAlgorithm, ranking, maxToReturn);
 					iterator = wrapper.getIterator();
 					if (iterator != null) {
 						iteratorBean = new IteratorBean(iterator);
 						iteratorBean.setKey(key);
 						iteratorBean.setMatchText(matchText);
 						iteratorBeanManager.addIteratorBean(iteratorBean);
 					}
 				}
 
 			} else if (searchTarget.compareTo("properties") == 0) {
 				if (iteratorBeanManager.containsIteratorBean(key)) {
 					iteratorBean = iteratorBeanManager.getIteratorBean(key);
 					iterator = iteratorBean.getIterator();
 				} else {
 					wrapper = new SearchUtils().searchByProperties(scheme, version, matchText, source, matchAlgorithm, excludeDesignation, ranking, maxToReturn);
 					iterator = wrapper.getIterator();
 					if (iterator != null) {
 						iteratorBean = new IteratorBean(iterator);
 						iteratorBean.setKey(key);
 						iteratorBean.setMatchText(matchText);
 						iteratorBeanManager.addIteratorBean(iteratorBean);
 					}
 				}
 
 			} else if (searchTarget.compareTo("relationships") == 0) {
 				designationOnly = true;
 				if (iteratorBeanManager.containsIteratorBean(key)) {
 					iteratorBean = iteratorBeanManager.getIteratorBean(key);
 					iterator = iteratorBean.getIterator();
 				} else {
 					wrapper = new SearchUtils().searchByAssociations(scheme, version, matchText, source, matchAlgorithm, designationOnly, ranking, maxToReturn);
 					iterator = wrapper.getIterator();
 					if (iterator != null) {
 						iteratorBean = new IteratorBean(iterator);
 						iteratorBean.setKey(key);
 						iteratorBean.setMatchText(matchText);
 						iteratorBeanManager.addIteratorBean(iteratorBean);
 					}
 				}
 			}
 	    }
         request.setAttribute("key", key);
         request.getSession().setAttribute("vocabulary", scheme);
         request.getSession().setAttribute("matchAlgorithm", matchAlgorithm);
         request.setAttribute("matchText", matchText);
 
         request.getSession().removeAttribute("neighborhood_synonyms");
         request.getSession().removeAttribute("neighborhood_atoms");
         request.getSession().removeAttribute("concept");
         request.getSession().removeAttribute("code");
         request.getSession().removeAttribute("codeInNCI");
         request.getSession().removeAttribute("AssociationTargetHashMap");
         request.getSession().removeAttribute("type");
 
         if (iterator != null) {
 			//request.setAttribute("matchText", matchText);
 
 			int size = iteratorBean.getSize();
 
 			// Write a search log entry
 			SearchLog.writeEntry(searchFields,
 			    size, HTTPUtils.getRefererParmDecode(request));
 
 			if (size > 1) {
 				request.getSession().setAttribute("search_results", v);
 
 				String match_size = Integer.toString(size);;//Integer.toString(v.size());
 				request.getSession().setAttribute("match_size", match_size);
 				request.getSession().setAttribute("page_string", "1");
 
 				request.getSession().setAttribute("new_search", Boolean.TRUE);
 				return "search_results";
 		    } else if (size == 1) {
 				request.getSession().setAttribute("singleton", "true");
 				request.getSession().setAttribute("dictionary", Constants.CODING_SCHEME_NAME);
 				//Concept c = (Concept) v.elementAt(0);
 				int pageNumber = 1;
 				List list = iteratorBean.getData(1);
 				ResolvedConceptReference ref = (ResolvedConceptReference) list.get(0);
 
 				Concept c = null;
 				if (ref == null) {
 					String msg = "Error: Null ResolvedConceptReference encountered.";
 					request.getSession().setAttribute("message", msg);
 					return "message";
 
 				} else {
 					c = ref.getReferencedEntry();
 					if (c == null) {
 						c = DataUtils.getConceptByCode(Constants.CODING_SCHEME_NAME, null, null, ref.getConceptCode());
 					}
 				}
 
 				request.getSession().setAttribute("code", ref.getConceptCode());
 				request.getSession().setAttribute("concept", c);
 				request.getSession().setAttribute("type", "properties");
 
 				request.getSession().setAttribute("new_search", Boolean.TRUE);
 				return "concept_details";
 		    }
 		}
 
         //[#23463] Linking retired concept to corresponding new concept
         // Test case: C0536142|200601|SY|||C1433544|Y|
 
         if (matchAlgorithm.compareTo("exactMatch") == 0) {
 			String newCUI = HistoryUtils.getReferencedCUI(matchText);
 
 			if (newCUI != null) {
 				System.out.println("Searching for " + newCUI);
 				Concept c = DataUtils.getConceptByCode(Constants.CODING_SCHEME_NAME, null, null, newCUI);
 				request.getSession().setAttribute("code", newCUI);
 				request.getSession().setAttribute("concept", c);
 				request.getSession().setAttribute("type", "properties");
 
 				request.getSession().setAttribute("new_search", Boolean.TRUE);
 				request.getSession().setAttribute("retired_cui", matchText);
 				return "concept_details";
 			}
 	    }
 
         String message = "No match found.";
         if (matchAlgorithm.compareTo("exactMatch") == 0) {
             message = Constants.ERROR_NO_MATCH_FOUND_TRY_OTHER_ALGORITHMS;
         }
         request.getSession().setAttribute("message", message);
         return "message";
     }
 
 
     private String selectedResultsPerPage = null;
     private List resultsPerPageList = null;
 
     public List getResultsPerPageList() {
         resultsPerPageList = new ArrayList();
         resultsPerPageList.add(new SelectItem("10"));
         resultsPerPageList.add(new SelectItem("25"));
         resultsPerPageList.add(new SelectItem("50"));
         resultsPerPageList.add(new SelectItem("75"));
         resultsPerPageList.add(new SelectItem("100"));
         resultsPerPageList.add(new SelectItem("250"));
         resultsPerPageList.add(new SelectItem("500"));
 
         selectedResultsPerPage = ((SelectItem) resultsPerPageList.get(2))
                 .getLabel(); // default to 50
 
         for (int i=0; i<selectedResultsPerPage.length(); i++) {
 			SelectItem item = (SelectItem) resultsPerPageList.get(i);
 			String label = item.getLabel();
 			int k = Integer.parseInt(label);
 			if (k == Constants.DEFAULT_PAGE_SIZE) {
 				selectedResultsPerPage = label;
 				break;
 			}
 		}
         return resultsPerPageList;
     }
 
     public void setSelectedResultsPerPage(String selectedResultsPerPage) {
         if (selectedResultsPerPage == null)
             return;
         this.selectedResultsPerPage = selectedResultsPerPage;
         HttpServletRequest request = (HttpServletRequest) FacesContext
                 .getCurrentInstance().getExternalContext().getRequest();
         request.getSession().setAttribute("selectedResultsPerPage",
                 selectedResultsPerPage);
     }
 
     public String getSelectedResultsPerPage() {
         HttpServletRequest request = (HttpServletRequest) FacesContext
                 .getCurrentInstance().getExternalContext().getRequest();
         String s = (String) request.getSession().getAttribute(
                 "selectedResultsPerPage");
         if (s != null) {
             this.selectedResultsPerPage = s;
         } else {
             this.selectedResultsPerPage = "50";
             request.getSession().setAttribute("selectedResultsPerPage", "50");
         }
         return this.selectedResultsPerPage;
     }
 
     public void resultsPerPageChanged(ValueChangeEvent event) {
         if (event.getNewValue() == null) {
             return;
         }
         String newValue = (String) event.getNewValue();
         setSelectedResultsPerPage(newValue);
     }
 
     public String linkAction() {
         HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
         return "";
     }
 
     private String selectedAlgorithm = null;
     private List algorithmList = null;
 
     public List getAlgorithmList() {
         algorithmList = new ArrayList();
         algorithmList.add(new SelectItem("exactMatch", "exactMatch"));
         algorithmList.add(new SelectItem("startsWith", "Begins With"));
         algorithmList.add(new SelectItem("contains", "Contains"));
         selectedAlgorithm = ((SelectItem) algorithmList.get(0)).getLabel();
         return algorithmList;
     }
 
     public void algorithmChanged(ValueChangeEvent event) {
         if (event.getNewValue() == null) return;
         String newValue = (String) event.getNewValue();
 
         //System.out.println("algorithmChanged; " + newValue);
         setSelectedAlgorithm(newValue);
     }
 
     public void setSelectedAlgorithm(String selectedAlgorithm) {
         this.selectedAlgorithm = selectedAlgorithm;
         HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
         request.getSession().setAttribute("selectedAlgorithm", selectedAlgorithm);
     }
 
     public String getSelectedAlgorithm() {
         return this.selectedAlgorithm;
     }
 
     public String contactUs() throws Exception {
         String msg = "Your message was successfully sent.";
         HttpServletRequest request = (HttpServletRequest) FacesContext
             .getCurrentInstance().getExternalContext().getRequest();
 
         try {
             String subject = request.getParameter("subject");
             String message = request.getParameter("message");
             String from = request.getParameter("emailaddress");
             String recipients[] = MailUtils.getRecipients();
             MailUtils.postMail(from, recipients, subject, message);
         } catch (Exception e) {
             msg = e.getMessage();
             request.setAttribute("errorMsg", Utils.toHtml(msg));
             return "error";
         }
 
         request.getSession().setAttribute("message", Utils.toHtml(msg));
         return "message";
     }
 
     ////////////////////////////////////////////////////////////////
     // source
     ////////////////////////////////////////////////////////////////
 
     private String selectedSource = "ALL";
     private List sourceList = null;
     private Vector<String> sourceListData = null;
 
     public List getSourceList() {
         if (sourceList != null) return sourceList;
         String codingSchemeName = Constants.CODING_SCHEME_NAME;
         String version = null;
         sourceListData = DataUtils.getSourceListData(codingSchemeName, version);
         sourceList = new ArrayList();
         if (sourceListData != null) {
             for (int i=0; i<sourceListData.size(); i++) {
                 String t = (String) sourceListData.elementAt(i);
                 sourceList.add(new SelectItem(t));
             }
         }
         return sourceList;
     }
 
     public void setSelectedSource(String selectedSource) {
         if (selectedSource == null) return;
         HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
         request.getSession().removeAttribute("selectedSource");
         request.getSession().setAttribute("selectedSource", selectedSource);
         this.selectedSource = selectedSource;
     }
 
     public String getSelectedSource() {
         if (selectedSource == null) {
             sourceList = getSourceList();
             if (sourceList != null && sourceList.size() > 0) {
                 this.selectedSource = ((SelectItem) sourceList.get(0)).getLabel();
             }
         }
         return this.selectedSource;
     }
 
     public void sourceSelectionChanged(ValueChangeEvent event) {
         if (event.getNewValue() != null) {
             String source = (String) event.getNewValue();
             //System.out.println("==================== sourceSelectionChanged to: " + source);
             setSelectedSource(source);
         }
     }
 
     ////////////////////////////////////////////////////////////////
     // concept sources
     ////////////////////////////////////////////////////////////////
 
     private String selectedConceptSource = null;
     private List conceptSourceList = null;
     private Vector<String> conceptSourceListData = null;
 
     public List getConceptSourceList() {
         String codingSchemeName = Constants.CODING_SCHEME_NAME;
         String version = null;
         HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
         String code = (String) request.getSession().getAttribute("code");
         conceptSourceListData = DataUtils.getSources(codingSchemeName, version, null, code);
         conceptSourceList = new ArrayList();
         if (conceptSourceListData == null) return conceptSourceList;
 
         for (int i=0; i<conceptSourceListData.size(); i++) {
             String t = (String) conceptSourceListData.elementAt(i);
             conceptSourceList.add(new SelectItem(t));
         }
         return conceptSourceList;
     }
 
     public void setSelectedConceptSource(String selectedConceptSource) {
         this.selectedConceptSource = selectedConceptSource;
         HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
         request.getSession().setAttribute("selectedConceptSource", selectedConceptSource);
     }
 
     public String getSelectedConceptSource() {
         if (selectedConceptSource == null) {
             conceptSourceList = getConceptSourceList();
             if (conceptSourceList != null && conceptSourceList.size() > 0) {
                 this.selectedConceptSource = ((SelectItem) conceptSourceList.get(0)).getLabel();
             }
         }
         return this.selectedConceptSource;
     }
 
     public void conceptSourceSelectionChanged(ValueChangeEvent event) {
         if (event.getNewValue() == null) return;
         HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
         request.getSession().removeAttribute("neighborhood_synonyms");
         request.getSession().removeAttribute("neighborhood_atoms");
         String source = (String) event.getNewValue();
         setSelectedConceptSource(source);
     }
 
     public String viewNeighborhoodAction() {
         HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
 
         String sab = getSelectedConceptSource();
         String code = (String) request.getParameter("code");
         return "neighborhood";
 
     }
 
    public String acceptLicenseAction() {
         HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
 		String dictionary = (String) request.getParameter("dictionary");
 		String code = (String) request.getParameter("code");
 		String sab = (String) request.getParameter("sab");
 
 		if (dictionary != null && code != null) {
 			LicenseBean licenseBean = (LicenseBean) request.getSession().getAttribute("licenseBean");
 			if (licenseBean == null) {
 				licenseBean = new LicenseBean();
 			}
 			licenseBean.addLicenseAgreement(dictionary);
 			request.getSession().setAttribute("licenseBean", licenseBean);
             request.getSession().setAttribute("term_browser_dictionary", dictionary);
             request.getSession().setAttribute("term_source_code", code);
 
             if (sab != null) {
 			    request.getSession().setAttribute("term_source", sab);
 			}
 			return "redirect";
 		} else {
 			String message = "Unidentifiable vocabulary name, or code";
 			request.getSession().setAttribute("warning", message);
 			return "message";
 		}
    }
 
    /*
    public String refreashAdvancedSearchAction() {
         HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
         return "advanced_search";
    }
    */
 }
