 /**********************************************************************************
  * $URL$
  * $Id$
  ***********************************************************************************
  *
  * Copyright (c) 2005, 2006 The Regents of the University of California and The Regents of the University of Michigan
  *
  * Licensed under the Educational Community License, Version 1.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.opensource.org/licenses/ecl1.php
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  **********************************************************************************/
 package org.sakaiproject.tool.section.jsf.backingbean;
 
 import java.io.Serializable;
 import java.util.*;
 import java.util.Map.Entry;
 import java.text.DateFormat;
 
 import javax.faces.application.Application;
 import javax.faces.component.UIColumn;
 import javax.faces.component.html.HtmlDataTable;
 import javax.faces.component.html.HtmlOutputText;
 import javax.faces.context.FacesContext;
 import javax.faces.event.ActionEvent;
 import javax.faces.model.SelectItem;
 
 import org.apache.commons.lang.StringUtils;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.myfaces.custom.sortheader.HtmlCommandSortHeader;
 import org.sakaiproject.section.api.coursemanagement.CourseSection;
 import org.sakaiproject.section.api.coursemanagement.EnrollmentRecord;
 import org.sakaiproject.section.api.coursemanagement.ParticipationRecord;
 import org.sakaiproject.section.api.coursemanagement.SectionEnrollments;
 import org.sakaiproject.section.api.SectionManager;
 import org.sakaiproject.tool.section.decorator.EnrollmentDecorator;
 import org.sakaiproject.tool.section.jsf.JsfUtil;
 
 import org.sakaiproject.jsf.spreadsheet.SpreadsheetDataFileWriterCsv;
 import org.sakaiproject.jsf.spreadsheet.SpreadsheetUtil;
 
 
 /**
  * Controls the roster page.
  *
  * @author <a href="mailto:jholtzman@berkeley.edu">Josh Holtzman</a>
  *
  */
 public class RosterBean extends CourseDependentBean implements Serializable {
 
 	private static final long serialVersionUID = 1L;
 	private static final Log log = LogFactory.getLog(RosterBean.class);
 	private static final String CAT_COLUMN_PREFIX = "cat";
 
 	private String searchText;
 	private int firstRow;
 	private int enrollmentsSize;
 	private boolean externallyManaged;
 	private List<SelectItem> filterItems;
 	private List<EnrollmentDecorator> enrollments;
 	private List<String> categories;
     private List<EnrollmentRecord> siteStudents;
 
     public void init() {
 		// Determine whether this course is externally managed
 		externallyManaged = getSectionManager().isExternallyManaged(getCourse().getUuid());
 
 		// Get the section categories
 		categories = getSectionManager().getSectionCategories(getSiteContext());
 
 		// Get the default search text
 		if(StringUtils.trimToNull(searchText) == null) {
 			searchText = JsfUtil.getLocalizedMessage("roster_search_text");
 		}
 
 		// Get the site enrollments
 		//List<EnrollmentRecord> siteStudents;
 		if(searchText.equals(JsfUtil.getLocalizedMessage("roster_search_text"))) {
 			siteStudents = getSectionManager().getSiteEnrollments(getSiteContext());
 		} else {
 			siteStudents = getSectionManager().findSiteEnrollments(getSiteContext(), searchText);
 		}
 
 		// Get the section enrollments
 		Set<String> studentUids = new HashSet<String>();
 		for(Iterator iter = siteStudents.iterator(); iter.hasNext();) {
 			ParticipationRecord record = (ParticipationRecord)iter.next();
 			studentUids.add(record.getUser().getUserUid());
 		}
 		SectionEnrollments sectionEnrollments = getSectionManager().getSectionEnrollmentsForStudents(getSiteContext(), studentUids);
 
 		// Construct the list of filter items
 		filterItems = new ArrayList<SelectItem>();
 		filterItems.add(new SelectItem("", JsfUtil.getLocalizedMessage("filter_all_sections")));
 		filterItems.add(new SelectItem("MY", JsfUtil.getLocalizedMessage("filter_my_category_sections", new String[] {""})));
 		for(Iterator<String> iter = categories.iterator(); iter.hasNext();) {
 			String cat = iter.next();
 			filterItems.add(new SelectItem(cat, JsfUtil.getLocalizedMessage("filter_my_category_sections",
 					new String[] {getCategoryName(cat)})));
 		}
 
 		// If this is a TA, and we're filtering, get the TA's participation records
 		List<CourseSection> assignedSections = null;
 		if(StringUtils.trimToNull(getFilter()) != null) {
 			assignedSections = findAssignedSections();
 		}
 
 		// Construct the decorated enrollments for the UI
 		decorateEnrollments(siteStudents, sectionEnrollments, assignedSections);
 	}
 
 	private void decorateEnrollments(List<EnrollmentRecord> siteStudents, SectionEnrollments sectionEnrollments, List<CourseSection> assignedSections) {
 		List<EnrollmentDecorator> unpagedEnrollments = new ArrayList<EnrollmentDecorator>();
 		for(Iterator<EnrollmentRecord> studentIter = siteStudents.iterator(); studentIter.hasNext();) {
 			EnrollmentRecord enrollment = studentIter.next();
 
 			// Build a map of categories to sections in which the student is enrolled
 			Map<String, CourseSection> map = new HashMap<String, CourseSection>();
 			for(Iterator catIter = categories.iterator(); catIter.hasNext();) {
 				String cat = (String)catIter.next();
 				CourseSection section = sectionEnrollments.getSection(enrollment.getUser().getUserUid(), cat);
                 map.put(cat, section);
 			}
 
 			// Check to see whether this enrollment should be filtered out
 			boolean includeStudent = false;
 			if(StringUtils.trimToNull(getFilter()) == null) {
 				includeStudent = true;
 			} else {
 				for(Iterator<Entry<String, CourseSection>> entryIter = map.entrySet().iterator(); entryIter.hasNext();) {
 					Entry<String, CourseSection> entry = entryIter.next();
 					CourseSection section = entry.getValue();
 					// Some map entries won't have a section at all, since the student isn't in any section of that category
 					if(section == null) {
 						continue;
 					}
 					if("MY".equals(getFilter()) && assignedSections.contains(section)) {
 						includeStudent = true;
 						break;
 					} else if(section.getCategory().equals(getFilter()) && assignedSections.contains(section)) {
 						includeStudent = true;
 						break;
 					}
 				}
 			}
 
 			if(includeStudent) {
 				EnrollmentDecorator decorator = new EnrollmentDecorator(enrollment, map);
 				unpagedEnrollments.add(decorator);
 			}
 		}
 
 		// Sort the list
 		Collections.sort(unpagedEnrollments, getComparator());
 
 		// Filter the list of enrollments
 		enrollments = new ArrayList<EnrollmentDecorator>();
 		int lastRow;
 		int maxDisplayedRows = getPrefs().getRosterMaxDisplayedRows();
 		if(maxDisplayedRows < 1 || firstRow + maxDisplayedRows > unpagedEnrollments.size()) {
 			lastRow = unpagedEnrollments.size();
 		} else {
 			lastRow = firstRow + maxDisplayedRows;
 		}
 		enrollments.addAll(unpagedEnrollments.subList(firstRow, lastRow));
 		enrollmentsSize = unpagedEnrollments.size();
 	}
 
 	private List<CourseSection> findAssignedSections() {
 		List<CourseSection> assignedSections = new ArrayList<CourseSection>();
 		for(Iterator<CourseSection> secIter = getSectionManager().getSections(getSiteContext()).iterator(); secIter.hasNext();) {
 			CourseSection section = secIter.next();
 			List<ParticipationRecord> tas = getSectionManager().getSectionTeachingAssistants(section.getUuid());
 			for(Iterator<ParticipationRecord> taIter = tas.iterator(); taIter.hasNext();) {
 				ParticipationRecord ta = taIter.next();
 				if(ta.getUser().getUserUid().equals(getUserUid())) {
 					assignedSections.add(section);
 					break;
 				}
 			}
 		}
 		return assignedSections;
 	}
 
 	private Comparator<EnrollmentDecorator> getComparator() {
 		String sortColumn = getPrefs().getRosterSortColumn();
 		boolean sortAscending = getPrefs().isRosterSortAscending();
 
 		if(sortColumn.equals("studentName")) {
 			return EnrollmentDecorator.getNameComparator(sortAscending);
 		} else if(sortColumn.equals("displayId")) {
 			return EnrollmentDecorator.getDisplayIdComparator(sortAscending);
 		} else {
 			return EnrollmentDecorator.getCategoryComparator(sortColumn, sortAscending);
 		}
 	}
 
 	public HtmlDataTable getRosterDataTable() {
 		return null;
 	}
 
 	public void setRosterDataTable(HtmlDataTable rosterDataTable) {
 		Set usedCategories = getUsedCategories();
 
 		if (rosterDataTable.findComponent(CAT_COLUMN_PREFIX + "0") == null) {
 			Application app = FacesContext.getCurrentInstance().getApplication();
 
 			// Add columns for each category. Be sure to create unique IDs
 			// for all child components.
 			int colpos = 0;
 			for (Iterator iter = usedCategories.iterator(); iter.hasNext(); colpos++) {
 				String category = (String)iter.next();
 				String categoryName = getCategoryName(category);
 
 				UIColumn col = new UIColumn();
 				col.setId(CAT_COLUMN_PREFIX + colpos);
 
                 HtmlCommandSortHeader sortHeader = new HtmlCommandSortHeader();
                 sortHeader.setId(CAT_COLUMN_PREFIX + "sorthdr_" + colpos);
                 sortHeader.setRendererType("org.apache.myfaces.SortHeader");
                 sortHeader.setArrow(true);
                 sortHeader.setColumnName(category);
                 //sortHeader.setActionListener(app.createMethodBinding("#{rosterBean.sort}", new Class[] {ActionEvent.class}));
 
 				HtmlOutputText headerText = new HtmlOutputText();
 				headerText.setId(CAT_COLUMN_PREFIX + "hdr_" + colpos);
 				headerText.setValue(categoryName);
 
                 sortHeader.getChildren().add(headerText);
                 col.setHeader(sortHeader);
 
 				HtmlOutputText contents = new HtmlOutputText();
 				contents.setId(CAT_COLUMN_PREFIX + "cell_" + colpos);
 				contents.setValueBinding("value",
 					app.createValueBinding("#{enrollment.categoryToSectionMap['" + category + "'].title}"));
 				col.getChildren().add(contents);
 				rosterDataTable.getChildren().add(col);
 			}
 		}
 	}
 
 	public void search(ActionEvent event) {
 //		firstRow = 0;
 	}
 
 	public void clearSearch(ActionEvent event) {
 		firstRow = 0;
 		searchText = null;
 	}
 
 	public List getEnrollments() {
 		return enrollments;
 	}
 	public int getEnrollmentsSize() {
 		return enrollmentsSize;
 	}
 	public boolean isExternallyManaged() {
 		return externallyManaged;
 	}
 	public String getSearchText() {
 		return searchText;
 	}
 	public void setSearchText(String searchText) {
         if (StringUtils.trimToNull(searchText) == null) {
         	searchText = JsfUtil.getLocalizedMessage("roster_search_text");
         }
     	if (!StringUtils.equals(searchText, this.searchText)) {
 	    	if (log.isDebugEnabled()) log.debug("setSearchString " + searchText);
 	        this.searchText = searchText;
 	        setFirstRow(0); // clear the paging when we update the search
 	    }
 	}
 	public int getFirstRow() {
 		return firstRow;
 	}
 	public void setFirstRow(int firstRow) {
 		this.firstRow = firstRow;
 	}
 
 	public String getFilter() {
 		return getPrefs().getRosterFilter();
 	}
 
 	public void setFilter(String filter) {
 		getPrefs().setRosterFilter(filter);
 	}
 
     public List getFilterItems() {
         return filterItems;
     }
 
     public void export(ActionEvent event){
 
         List<List<Object>> spreadsheetData = new ArrayList<List<Object>>();
         // Get the section enrollments
         Set<String> studentUids = new HashSet<String>();
         for(Iterator iter = siteStudents.iterator(); iter.hasNext();) {
             ParticipationRecord record = (ParticipationRecord)iter.next();
             studentUids.add(record.getUser().getUserUid());
         }
         SectionEnrollments sectionEnrollments = getSectionManager().getSectionEnrollmentsForStudents(getSiteContext(), studentUids);
         // Add the header row
         List<Object> header = new ArrayList<Object>();
         header.add(JsfUtil.getLocalizedMessage("roster_table_header_name"));
         header.add(JsfUtil.getLocalizedMessage("roster_table_header_id"));
 
         for (Iterator iter = getUsedCategories().iterator(); iter.hasNext();){
             String category = (String)iter.next();          
             String categoryName = getCategoryName(category);
             header.add(categoryName);
         }
         spreadsheetData.add(header);
         for(Iterator<EnrollmentDecorator> enrollmentIter = enrollments.iterator(); enrollmentIter.hasNext();) {
             EnrollmentDecorator enrollment = enrollmentIter.next();
             List<Object> row = new ArrayList<Object>();
             row.add(enrollment.getUser().getSortName());
             row.add(enrollment.getUser().getDisplayId());
 
             for (Iterator iter = getUsedCategories().iterator(); iter.hasNext();){
                 String category = (String)iter.next();
                 CourseSection section = sectionEnrollments.getSection(enrollment.getUser().getUserUid(), category);

                try{
                  row.add(section.getTitle());
                }catch(NullPointerException npe){
                    if(log.isDebugEnabled())log.debug("section type has no enrollments");
                    row.add("");
                }
             }
             spreadsheetData.add(row);
         }
         String spreadsheetName = getDownloadFileName(getCourse().getTitle());
         SpreadsheetUtil.downloadSpreadsheetData(spreadsheetData, spreadsheetName, new SpreadsheetDataFileWriterCsv());
 
     }
 
     protected String getDownloadFileName(String rawString) {
         String dateString = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date());
         return rawString.replaceAll("\\W","_")+ "_"+dateString;
     }
 }
