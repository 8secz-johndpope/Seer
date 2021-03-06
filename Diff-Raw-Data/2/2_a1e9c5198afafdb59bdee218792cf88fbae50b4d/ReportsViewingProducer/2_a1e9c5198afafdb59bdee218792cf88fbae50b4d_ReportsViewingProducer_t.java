 /**
  * $Id$
  * $URL$
  * ReportsViewingProducer.java - evaluation - Oct 05, 2006 9:23:47 PM - whumphri
  **************************************************************************
  * Copyright (c) 2008 Centre for Applied Research in Educational Technologies, University of Cambridge
  * Licensed under the Educational Community License version 1.0
  * 
  * A copy of the Educational Community License has been included in this 
  * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
  *
  * Aaron Zeckoski (azeckoski@gmail.com) (aaronz@vt.edu) (aaron@caret.cam.ac.uk)
  */
 
 package org.sakaiproject.evaluation.tool.producers;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.sakaiproject.evaluation.constant.EvalConstants;
 import org.sakaiproject.evaluation.logic.EvalAuthoringService;
 import org.sakaiproject.evaluation.logic.EvalDeliveryService;
 import org.sakaiproject.evaluation.logic.EvalEvaluationService;
 import org.sakaiproject.evaluation.logic.EvalSettings;
 import org.sakaiproject.evaluation.logic.ReportingPermissions;
 import org.sakaiproject.evaluation.logic.externals.EvalExternalLogic;
 import org.sakaiproject.evaluation.logic.externals.ExternalHierarchyLogic;
 import org.sakaiproject.evaluation.logic.model.EvalHierarchyNode;
 import org.sakaiproject.evaluation.logic.model.EvalUser;
 import org.sakaiproject.evaluation.model.EvalAnswer;
 import org.sakaiproject.evaluation.model.EvalEvaluation;
 import org.sakaiproject.evaluation.model.EvalScale;
 import org.sakaiproject.evaluation.model.EvalTemplateItem;
 import org.sakaiproject.evaluation.tool.EvalToolConstants;
 import org.sakaiproject.evaluation.tool.utils.EvalResponseAggregatorUtil;
 import org.sakaiproject.evaluation.tool.utils.RenderingUtils;
 import org.sakaiproject.evaluation.tool.viewparams.CSVReportViewParams;
 import org.sakaiproject.evaluation.tool.viewparams.ExcelReportViewParams;
 import org.sakaiproject.evaluation.tool.viewparams.PDFReportViewParams;
 import org.sakaiproject.evaluation.tool.viewparams.ReportParameters;
 import org.sakaiproject.evaluation.utils.ArrayUtils;
 import org.sakaiproject.evaluation.utils.EvalUtils;
 import org.sakaiproject.evaluation.utils.TemplateItemDataList;
 import org.sakaiproject.evaluation.utils.TemplateItemUtils;
 import org.sakaiproject.evaluation.utils.TemplateItemDataList.DataTemplateItem;
 import org.sakaiproject.evaluation.utils.TemplateItemDataList.HierarchyNodeGroup;
 import org.sakaiproject.evaluation.utils.TemplateItemDataList.TemplateItemGroup;
 import org.sakaiproject.util.Validator;
 
 import uk.org.ponder.rsf.components.UIBranchContainer;
 import uk.org.ponder.rsf.components.UIContainer;
 import uk.org.ponder.rsf.components.UIInternalLink;
 import uk.org.ponder.rsf.components.UIMessage;
 import uk.org.ponder.rsf.components.UIOutput;
 import uk.org.ponder.rsf.components.UIVerbatim;
 import uk.org.ponder.rsf.components.decorators.UIStyleDecorator;
 import uk.org.ponder.rsf.view.ComponentChecker;
 import uk.org.ponder.rsf.view.ViewComponentProducer;
 import uk.org.ponder.rsf.viewstate.SimpleViewParameters;
 import uk.org.ponder.rsf.viewstate.ViewParameters;
 import uk.org.ponder.rsf.viewstate.ViewParamsReporter;
 
 /**
  * rendering the report results from an evaluation
  * 
  * @author Steven Githens
  * @author Aaron Zeckoski (aaronz@vt.edu)
  * @author Will Humphries (whumphri@vt.edu)
  */
 public class ReportsViewingProducer implements ViewComponentProducer, ViewParamsReporter {
    private static Log log = LogFactory.getLog(ReportsViewingProducer.class);
 
    private static final String VIEWMODE_REGULAR = "viewmode_regular";
    private static final String VIEWMODE_ALLESSAYS = "viewmode_allessays";
    private static final String VIEWMODE_SELECTITEMS = "viewmode_selectitems";
 
    public static final String VIEW_ID = "report_view";
    public String getViewID() {
       return VIEW_ID;
    }
 
    private EvalExternalLogic externalLogic;
    public void setExternalLogic(EvalExternalLogic externalLogic) {
       this.externalLogic = externalLogic;
    }
 
    private EvalEvaluationService evaluationService;
    public void setEvaluationService(EvalEvaluationService evaluationService) {
       this.evaluationService = evaluationService;
    }
 
    private EvalAuthoringService authoringService;
    public void setAuthoringService(EvalAuthoringService authoringService) {
       this.authoringService = authoringService;
    }
 
    private EvalDeliveryService deliveryService;
    public void setDeliveryService(EvalDeliveryService deliveryService) {
       this.deliveryService = deliveryService;
    }
 
    private ExternalHierarchyLogic hierarchyLogic;
    public void setExternalHierarchyLogic(ExternalHierarchyLogic logic) {
       this.hierarchyLogic = logic;
    }
 
    private EvalSettings evalSettings;
    public void setEvalSettings(EvalSettings evalSettings) {
       this.evalSettings = evalSettings;
    }
 
    private ReportingPermissions reportingPermissions;
    public void setReportingPermissions(ReportingPermissions perms) {
       this.reportingPermissions = perms;
    }
 
    int displayNumber = 0;
 
    String currentViewMode = VIEWMODE_REGULAR;
    boolean collapseEssays = true;
    Long[] itemsToView;
 
    /* (non-Javadoc)
     * @see uk.org.ponder.rsf.view.ComponentProducer#fillComponents(uk.org.ponder.rsf.components.UIContainer, uk.org.ponder.rsf.viewstate.ViewParameters, uk.org.ponder.rsf.view.ComponentChecker)
     */
    public void fillComponents(UIContainer tofill, ViewParameters viewparams, ComponentChecker checker) {
       ReportParameters reportViewParams = (ReportParameters) viewparams;
       String currentUserId = externalLogic.getCurrentUserId();
 
       if (VIEWMODE_ALLESSAYS.equals(reportViewParams.viewmode)) {
          currentViewMode = VIEWMODE_ALLESSAYS;
          collapseEssays = false;
       }
       else if (VIEWMODE_SELECTITEMS.equals(reportViewParams.viewmode)) {
          currentViewMode = VIEWMODE_SELECTITEMS;
          collapseEssays = false;
          if (reportViewParams.items == null) {
             itemsToView = new Long[] {};
          }
          else {
             itemsToView = reportViewParams.items;
          }
       }
       else {
          currentViewMode = VIEWMODE_REGULAR;
       }
 
       renderTopLinks(tofill);
 
       UIMessage.make(tofill, "view-report-title", "viewreport.page.title");
 
       Long evaluationId = reportViewParams.evaluationId;
       if (evaluationId == null) {
          // invalid view params
          throw new IllegalArgumentException("Evaluation id is required to view report");
       } else {
          /*
           * We only need to show the choose groups breadcrumb if it's actually 
           * possible for us to view more than one group.
           */
          String[] viewableGroups = reportingPermissions.chooseGroupsPartialCheck(evaluationId);
          if (viewableGroups.length > 1) {
             UIInternalLink.make(tofill, "report-groups-title", UIMessage.make("reportgroups.page.title"), 
                   new ReportParameters(ReportChooseGroupsProducer.VIEW_ID, reportViewParams.evaluationId));
          }
          String[] groupIds = (reportViewParams.groupIds == null ? new String[] {} : reportViewParams.groupIds);
          reportViewParams.groupIds = groupIds;
 
          EvalEvaluation evaluation = evaluationService.getEvaluationById(evaluationId);
 
          // do a permission check
          if (! reportingPermissions.canViewEvaluationResponses(evaluation, groupIds)) {
             throw new SecurityException("Invalid user attempting to access reports page: " + currentUserId);
          }
 
          Long templateId = evaluation.getTemplate().getId();
 
          // get all items for this template (maybe limit these by groups later)
          List<EvalTemplateItem> allTemplateItems = 
             authoringService.getTemplateItemsForTemplate(templateId, new String[] {}, new String[] {}, new String[] {});
 
          if (! allTemplateItems.isEmpty()) {
 
             if (VIEWMODE_ALLESSAYS.equals(reportViewParams.viewmode)) {
                currentViewMode = VIEWMODE_ALLESSAYS;
             }
 
             // Render Reporting links such as xls, pdf output, and options for viewing essays and stuff
             renderReportingOptionsTopLinks(tofill, evaluation, templateId, reportViewParams);
 
             // Evaluation Info
             UIOutput.make(tofill, "evaluationTitle", evaluation.getTitle());
             StringBuilder groupsString = new StringBuilder();
             for (int groupCounter = 0; groupCounter < groupIds.length; groupCounter++) {
                if (groupCounter > 0) {
                   groupsString.append(", ");
                }
                groupsString.append( externalLogic.getDisplayTitle(groupIds[groupCounter]) );
             }
             UIMessage.make(tofill, "selectedGroups", "viewreport.viewinggroups", new String[] {groupsString.toString()});
 
             // get all the answers
             List<EvalAnswer> answers = deliveryService.getAnswersForEval(evaluationId, groupIds, null);
 
             // get the list of all instructors for this report and put the user objects for them into a map
             Set<String> instructorIds = TemplateItemDataList.getInstructorsForAnswers(answers);
             List<EvalUser> instructors = externalLogic.getEvalUsersByIds(instructorIds.toArray(new String[] {}));
             Map<String,EvalUser> instructorIdtoEvalUser = new HashMap<String, EvalUser>();
             for (EvalUser evalUser : instructors) {
                instructorIdtoEvalUser.put(evalUser.userId, evalUser);
             }
 
             // Get the sorted list of all nodes for this set of template items
             List<EvalHierarchyNode> hierarchyNodes = RenderingUtils.makeEvalNodesList(hierarchyLogic, allTemplateItems);
 
             // make the TI data structure
             Map<String, List<String>> associates = new HashMap<String, List<String>>();
             associates.put(EvalConstants.ITEM_CATEGORY_INSTRUCTOR, new ArrayList<String>(instructorIds));
             TemplateItemDataList tidl = new TemplateItemDataList(allTemplateItems, hierarchyNodes, associates, answers);
 
             int renderedItemCount = 0;
             // loop through the TIGs and handle each associated category
             for (TemplateItemGroup tig : tidl.getTemplateItemGroups()) {
                // check if we should render any of these items at all
                if (! renderAnyBasedOnOptions(tig.getTemplateItems())) {
                   continue;
                }
                UIBranchContainer categorySectionBranch = UIBranchContainer.make(tofill, "categorySection:");
                // handle printing the category header
                if (EvalConstants.ITEM_CATEGORY_COURSE.equals(tig.associateType)) {
                   UIMessage.make(categorySectionBranch, "categoryHeader", "viewreport.itemlist.course");
                } else if (EvalConstants.ITEM_CATEGORY_INSTRUCTOR.equals(tig.associateType)) {
                   EvalUser instructor = instructorIdtoEvalUser.get( tig.associateId );
                   UIMessage.make(categorySectionBranch, "categoryHeader", 
                         "viewreport.itemlist.instructor", new Object[] { instructor.displayName });
                }
 
                // loop through the hierarchy node groups
                for (HierarchyNodeGroup hng : tig.hierarchyNodeGroups) {
                   // check if we should render any of these items at all
                   if (! renderAnyBasedOnOptions(hng.templateItems)) {
                      continue;
                   }
                   // render a node title
                   if (hng.node != null) {
                      // Showing the section title is system configurable via the administrate view
                      Boolean showHierSectionTitle = (Boolean) evalSettings.get(EvalSettings.DISPLAY_HIERARCHY_HEADERS);
                      if (showHierSectionTitle) {
                         UIBranchContainer nodeTitleBranch = UIBranchContainer.make(categorySectionBranch, "itemrow:nodeSection");
                         UIOutput.make(nodeTitleBranch, "nodeTitle", hng.node.title);
                      }
                   }
 
                   List<DataTemplateItem> dtis = hng.getDataTemplateItems(true); // include block children
                   for (int i = 0; i < dtis.size(); i++) {
                      DataTemplateItem dti = dtis.get(i);
                      if (renderBasedOnOptions(dti.templateItem)) {
                         UIBranchContainer nodeItemsBranch = UIBranchContainer.make(categorySectionBranch, "itemrow:templateItem");
                         if (renderedItemCount % 2 == 1) {
                            nodeItemsBranch.decorate( new UIStyleDecorator("itemsListOddLine") ); // must match the existing CSS class
                         }
                         renderTemplateItemResults(nodeItemsBranch, dti, reportViewParams);
                         renderedItemCount++;
                      }
                   }
                }
 
             }
 
          }
       }
 
    }
 
 
    /**
     * Handles the rendering of the response answer results for a specific templateItem
     * 
     * @param tofill the parent container to render in
     * @param dti the wrapper for this templateItem
     */
    private void renderTemplateItemResults(UIBranchContainer tofill, DataTemplateItem dti, ReportParameters reportViewParams) {
       EvalTemplateItem templateItem = dti.templateItem;
       String templateItemType = TemplateItemUtils.getTemplateItemType(templateItem);
 
       // get the answers associated with this data template item
       List<EvalAnswer> itemAnswers = dti.getAnswers();
 
       if ( EvalConstants.ITEM_TYPE_SCALED.equals(templateItemType) 
             || EvalConstants.ITEM_TYPE_MULTIPLEANSWER.equals(templateItemType) 
             || EvalConstants.ITEM_TYPE_MULTIPLECHOICE.equals(templateItemType)
             || EvalConstants.ITEM_TYPE_BLOCK_CHILD.equals(templateItemType) ) {
          // scales/choices type item
          displayNumber++;
 
          UIBranchContainer scaled = UIBranchContainer.make(tofill, "scaledSurvey:");
 
          UIOutput.make(scaled, "itemNum", displayNumber+"");
          UIVerbatim.make(scaled, "itemText", templateItem.getItem().getItemText());
 
          int responsesCount = 0;
          int naCount = 0;
          if (! VIEWMODE_ALLESSAYS.equals(currentViewMode)) {
             // if we are in essay view mode then do not show the scale or the answers counts
             EvalScale scale = templateItem.getItem().getScale();
             String[] scaleOptions = scale.getOptions();
             String scaleLabels[] = new String[scaleOptions.length];
 
             int[] responseNumbers = EvalResponseAggregatorUtil.countResponseChoices(templateItemType, scaleOptions.length, itemAnswers);
 
             for (int x = 0; x < scaleLabels.length; x++) {
                int responseCount = responseNumbers[x];
                responsesCount += responseCount;
                UIBranchContainer answerbranch = UIBranchContainer.make(scaled, "answers:", x + "");
                UIOutput.make(answerbranch, "responseText", scaleOptions[x]);
               UIMessage.make(answerbranch, "responsesCount", "viewreport.responses.count", new String[] {responseNumbers[x]+""});
             }
 
             if (templateItem.getUsesNA()) {
                naCount = responseNumbers[responseNumbers.length-1];
                UIBranchContainer answerbranch = UIBranchContainer.make(scaled, "answers:");
                UIMessage.make(answerbranch, "responseText", "reporting.notapplicable.longlabel");
                UIOutput.make(answerbranch, "responseTotal", naCount+"");
             }
          } else {
             responsesCount = itemAnswers.size();
          }
 
          UIMessage.make(scaled, "responsesCount", "viewreport.responses.count", new Object[] {responsesCount + naCount});
 
          if (templateItem.getUsesComment()) {
             // render the comments
             UIBranchContainer showCommentsBranch = UIBranchContainer.make(tofill, "showComments:");
             int commentCount = 0;
             for (EvalAnswer answer : itemAnswers) {
                if (! EvalUtils.isBlank(answer.getComment())) {
                   UIBranchContainer commentsBranch = UIBranchContainer.make(showCommentsBranch, "comments:");
                   if (commentCount % 2 == 1) {
                      commentsBranch.decorate(new UIStyleDecorator("itemsListOddLine")); // must match the existing CSS class
                   }
                   UIOutput.make(commentsBranch, "commentNum", (commentCount + 1)+"");
                   UIOutput.make(commentsBranch, "itemComment", answer.getComment());
                   commentCount++;
                }
             }
             if (commentCount == 0) {
                UIMessage.make(showCommentsBranch, "noComment", "viewreport.no.comments");
             }
          }            
 
       } else if (EvalConstants.ITEM_TYPE_TEXT.equals(templateItemType)) {
          // text/essay type items
          displayNumber++;
          UIBranchContainer essay = UIBranchContainer.make(tofill, "essayType:");
          UIOutput.make(essay, "itemNum", displayNumber + "");
          UIVerbatim.make(essay, "itemText", templateItem.getItem().getItemText());
 
          ReportParameters params = new ReportParameters(VIEW_ID, reportViewParams.evaluationId);
          params.viewmode = VIEWMODE_SELECTITEMS;
          params.items = new Long[] {templateItem.getId()};
          params.groupIds = reportViewParams.groupIds;
          UIInternalLink.make(essay, "essayResponse", UIMessage.make("viewreport.view.viewresponses"), params);
 
          // render the responses
          UIBranchContainer showResponsesBranch = UIBranchContainer.make(tofill, "showResponses:");
          int responsesCount = 0;
          int naCount = 0;
          for (EvalAnswer answer : itemAnswers) {
             if (answer.NA) {
                naCount++;
             } else if (! EvalUtils.isBlank(answer.getText())) {
                UIBranchContainer responsesBranch = UIBranchContainer.make(showResponsesBranch, "responses:");
                if (responsesCount % 2 == 1) {
                   responsesBranch.decorate(new UIStyleDecorator("itemsListOddLine")); // must match the existing CSS class
                }
                UIOutput.make(responsesBranch, "responseNum", (responsesCount + 1)+"");
                UIOutput.make(responsesBranch, "itemResponse", answer.getText());
                responsesCount++;
             }
          }
          if (responsesCount == 0) {
             UIMessage.make(showResponsesBranch, "noResponse", "viewreport.no.responses");
          }
 
          UIMessage.make(essay, "responsesCount", "viewreport.responses.count", new Object[] {responsesCount + naCount});
 
          if (templateItem.getUsesNA()) {
             // show the number of NA responses
             UIBranchContainer naBranch = UIBranchContainer.make(essay, "showNA:");
             UIOutput.make(naBranch, "naCount", naCount+"");
          }
 
       } else if (EvalConstants.ITEM_TYPE_HEADER.equals(templateItemType)
             || EvalConstants.ITEM_TYPE_BLOCK_PARENT.equals(templateItemType)) {
          // header/textual bits
          UIBranchContainer textual = UIBranchContainer.make(tofill, "textualItem:");
          UIVerbatim.make(textual, "itemText", templateItem.getItem().getItemText());
 
       } else {
          log.warn("Skipped invalid item type ("+templateItemType+"): TI: " + templateItem.getId() );
       }
    }
 
 
    /**
     * Checks if any of the items in a set should be rendered based
     * on the passed in options for this view
     * 
     * @param templateItems a list of {@link EvalTemplateItem}
     * @return true if any of the items in this list should be rendered, false otherwise
     */
    private boolean renderAnyBasedOnOptions(List<EvalTemplateItem> templateItems) {
       boolean renderAny = false;
       for (EvalTemplateItem templateItem : templateItems) {
          if (renderBasedOnOptions(templateItem)) {
             renderAny = true;
             break;
          }
       }
       return renderAny;
    }
 
    /**
     * Should we render this item based off the passed in parameters?
     * (ie. Should we only render certain template items)
     */
    private boolean renderBasedOnOptions(EvalTemplateItem templateItem) {
       boolean togo = false;
       if (VIEWMODE_SELECTITEMS.equals(currentViewMode)) {
          // If this item isn't in the list of items to view, don't render it.
          if (ArrayUtils.contains(itemsToView, templateItem.getId())) {
             togo = true;
          }
       }
       else if (VIEWMODE_ALLESSAYS.equals(currentViewMode)) {
          // show all items with comments or textual responses
          String templateItemType = TemplateItemUtils.getTemplateItemType(templateItem);
          if (EvalConstants.ITEM_TYPE_TEXT.equals(templateItemType)) {
             togo = true;
          } else if (templateItem.getUsesComment()) {
             togo = true;
          }
       }
       else if (VIEWMODE_REGULAR.equals(currentViewMode)) {
          togo = true;
       }
       else {
          // default to true
          togo = true;
       }
       return togo;
    }
 
    /**
     * Moved this top link rendering out of the main producer code. 
     * TODO FIXME: Is there not some standard Evalution Util class to render 
     * all these top links?  Find out. sgithens 2008-03-01 7:13PM Central Time
     *
     * @param tofill The parent UIContainer.
     */
    private void renderTopLinks(UIContainer tofill) {
 
       // local variables used in the render logic
       String currentUserId = externalLogic.getCurrentUserId();
       boolean userAdmin = externalLogic.isUserAdmin(currentUserId);
       boolean createTemplate = authoringService.canCreateTemplate(currentUserId);
       boolean beginEvaluation = evaluationService.canBeginEvaluation(currentUserId);
 
       /*
        * top links here
        */
       UIInternalLink.make(tofill, "summary-link", 
             UIMessage.make("summary.page.title"), 
             new SimpleViewParameters(SummaryProducer.VIEW_ID));
 
       if (userAdmin) {
          UIInternalLink.make(tofill, "administrate-link", 
                UIMessage.make("administrate.page.title"),
                new SimpleViewParameters(AdministrateProducer.VIEW_ID));
          UIInternalLink.make(tofill, "control-scales-link",
                UIMessage.make("controlscales.page.title"),
                new SimpleViewParameters(ControlScalesProducer.VIEW_ID));
       }
 
       if (createTemplate) {
          UIInternalLink.make(tofill, "control-templates-link",
                UIMessage.make("controltemplates.page.title"), 
                new SimpleViewParameters(ControlTemplatesProducer.VIEW_ID));
          UIInternalLink.make(tofill, "control-items-link",
                UIMessage.make("controlitems.page.title"), 
                new SimpleViewParameters(ControlItemsProducer.VIEW_ID));
       } else {
          throw new SecurityException("User attempted to access " + 
                VIEW_ID + " when they are not allowed");
       }
 
       if (beginEvaluation) {
          UIInternalLink.make(tofill, "control-evaluations-link",
                UIMessage.make("controlevaluations.page.title"),
                new SimpleViewParameters(ControlEvaluationsProducer.VIEW_ID));
       }
    }
 
    /**
     * Renders the reporting specific links like XLS and PDF download, etc.
     * 
     * @param tofill
     * @param evaluation
     * @param templateId
     * @param reportViewParams
     */
    private void renderReportingOptionsTopLinks(UIContainer tofill, EvalEvaluation evaluation, Long templateId, ReportParameters reportViewParams) {
       // Render the link to show "All Essays" or "All Questions"
       ReportParameters viewEssaysParams = (ReportParameters) reportViewParams.copyBase();
       if (VIEWMODE_ALLESSAYS.equals(currentViewMode)) {
          viewEssaysParams.viewmode = VIEWMODE_REGULAR; 
          UIInternalLink.make(tofill, "viewItemsFilterLink", UIMessage.make("viewreport.view.all"), 
                viewEssaysParams);
       }
       else {
          viewEssaysParams.viewmode = VIEWMODE_ALLESSAYS;
          UIInternalLink.make(tofill, "viewItemsFilterLink",  UIMessage.make("viewreport.view.written"), 
                viewEssaysParams);
       }
 
       /*
        * The Downloaded names should have a max length indicated by the
        * constant.  We run them through the validator to make sure they
        * are ok for display on OS filesystems. At the moment we are using
        * just the Evaluation title for the name of the downloaded file.
        */
       String evaltitle = evaluation.getTitle();
       if (evaltitle.length() > EvalToolConstants.EVAL_REPORTING_MAX_NAME_LENGTH) {
          evaltitle = evaltitle.substring(0, EvalToolConstants.EVAL_REPORTING_MAX_NAME_LENGTH);
       }
 
       // FIXME don't use sakai classes directly (plus what the crap does this do anyway? -AZ)
       evaltitle = Validator.escapeZipEntry(evaltitle);
 
       Boolean allowCSVExport = (Boolean) evalSettings.get(EvalSettings.ENABLE_CSV_REPORT_EXPORT);
       if (allowCSVExport != null && allowCSVExport == true) {
          UIInternalLink.make(tofill, "csvResultsReport", UIMessage.make("viewreport.view.csv"), new CSVReportViewParams(
                "csvResultsReport", templateId, reportViewParams.evaluationId, reportViewParams.groupIds, evaltitle+".csv"));
       }
 
       Boolean allowXLSExport = (Boolean) evalSettings.get(EvalSettings.ENABLE_XLS_REPORT_EXPORT);
       if (allowXLSExport != null && allowXLSExport == true) {
          UIInternalLink.make(tofill, "xlsResultsReport", UIMessage.make("viewreport.view.xls"), new ExcelReportViewParams(
                "xlsResultsReport", templateId, reportViewParams.evaluationId, reportViewParams.groupIds, evaltitle+".xls"));
       }
 
       Boolean allowPDFExport = (Boolean) evalSettings.get(EvalSettings.ENABLE_PDF_REPORT_EXPORT);
       if (allowPDFExport != null && allowPDFExport == true) {
          UIInternalLink.make(tofill, "pdfResultsReport", UIMessage.make("viewreport.view.pdf"), new PDFReportViewParams(
                "pdfResultsReport", templateId, reportViewParams.evaluationId, reportViewParams.groupIds, evaltitle+".pdf"));
       }
    }
 
 
    /* (non-Javadoc)
     * @see uk.org.ponder.rsf.viewstate.ViewParamsReporter#getViewParameters()
     */
    public ViewParameters getViewParameters() {
       return new ReportParameters();
    }
 
 }
