 package br.ufpe.cin.reviewer.ui.rcp.literaturereview;
 
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.List;
 
 import org.eclipse.core.runtime.IConfigurationElement;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.StyledText;
 import org.eclipse.swt.events.FocusEvent;
 import org.eclipse.swt.events.FocusListener;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.SelectionListener;
 import org.eclipse.swt.graphics.Font;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.ui.IPerspectiveRegistry;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.forms.events.IHyperlinkListener;
 import org.eclipse.ui.forms.widgets.Hyperlink;
 
 import br.ufpe.cin.reviewer.core.common.StudyController;
 import br.ufpe.cin.reviewer.model.common.Study;
 import br.ufpe.cin.reviewer.model.literaturereview.LiteratureReview;
 import br.ufpe.cin.reviewer.searchprovider.extensions.SearchProviderExtensionsRegistry;
 import br.ufpe.cin.reviewer.ui.rcp.common.BaseView;
 import br.ufpe.cin.reviewer.ui.rcp.common.ReviewerViewRegister;
 import br.ufpe.cin.reviewer.ui.rcp.common.UIConstants;
 import br.ufpe.cin.reviewer.ui.rcp.util.WidgetsUtil;
 
 public class StudyAnalysisView extends BaseView {
 
 	public static final String ID = "br.ufpe.cin.reviewer.ui.rcp.literaturereview.StudyAnalysisView";
 	
 	private Study study;
 	private LiteratureReview literatureReview;
 	
 	private StyledText codeStyledText;
 	private StyledText titleStyledText;
 	private StyledText statusStyledText;
 	private StyledText sourceStyledText;
 	private StyledText authorsStyledText;
 	private StyledText institutionsStyledText;
 	private StyledText countriesStyledText;
	private StyledText yearStyledText;
 	private StyledText urlStyledText;
 	private StyledText abstractStyledText;
 	
 	public StudyAnalysisView() {
 		ReviewerViewRegister.putView(ID, this);
 	}
 
 	public void setStudy(Study study) {
 		this.study = study;
 		
 		// Setting study code
 		if (study.getCode() == null) {
 			this.codeStyledText.setText("");
 		} else {
 			this.codeStyledText.setText(study.getCode());
 		}
 		this.codeStyledText.setLineJustify(0, this.codeStyledText.getLineCount(), true);
 		
 		// Setting study title
 		if (study.getTitle() == null) {
 			this.titleStyledText.setText("");
 		} else {
 			this.titleStyledText.setText(study.getTitle());
 		}
 		this.titleStyledText.setLineJustify(0, this.titleStyledText.getLineCount(), true);
 		
 		// Setting study status
 		if (study.getStatus() == null) {
 			this.statusStyledText.setText("");
 		} else {
 			this.statusStyledText.setText(study.getStatus().toString());
 		}
 		this.statusStyledText.setLineJustify(0, this.statusStyledText.getLineCount(), true);
 
 		// Setting study source
 		if (study.getSource() == null) {
 			this.sourceStyledText.setText("");
 		} else {
 			List<IConfigurationElement> configs = SearchProviderExtensionsRegistry.getConfigElements();
 			Collections.sort(configs, new SearchProviderConfiguratorElementComparator());
 			
 			for (IConfigurationElement config : configs) {
 				if (config.getAttribute("key").equals(study.getSource())) {
 					this.sourceStyledText.setText(config.getAttribute("friendly.name"));
 				}
 			}
 		}
 		this.sourceStyledText.setLineJustify(0, this.sourceStyledText.getLineCount(), true);
 
 		// Setting study authors
 		String authors = "";
 		if(study.getAuthors() != null) {
 			for (String author : study.getAuthors()) {
 				if (study.getAuthors().indexOf(author) != (study.getAuthors().size() - 1)) {
 					authors += author + ", ";
 				}
 				else {
 					authors += author;
 				}
 			}
 		}
 		this.authorsStyledText.setText(authors);
 		this.authorsStyledText.setLineJustify(0, this.authorsStyledText.getLineCount(), true);
 
 		// Setting study institutions
 		String institutions = "";
 		if(study.getInstitutions() != null) {
 			for (String institution : study.getInstitutions()) {
 				if (study.getInstitutions().indexOf(institution) != (study.getInstitutions().size() - 1)) {
 					institutions += institution + ", ";
 				}
 				else {
 					institutions += institution;
 				}
 			}
 		}
 		this.institutionsStyledText.setText(institutions);
 		this.institutionsStyledText.setLineJustify(0, this.institutionsStyledText.getLineCount(), true);
 		
 		// Setting study countries
 		String countries = "";
 		if(study.getCountries() != null) {
 			for (String country : study.getCountries()) {
 				if (study.getCountries().indexOf(country) != (study.getCountries().size() - 1)) {
 					countries += country + ", ";
 				}
 				else {
 					countries += country;
 				}
 			}
 		}
 		this.countriesStyledText.setText(countries);
 		this.countriesStyledText.setLineJustify(0, this.countriesStyledText.getLineCount(), true);

		// Setting study year
		if (study.getYear() == null) {
			this.yearStyledText.setText("");
		} else {
			this.yearStyledText.setText(study.getYear());
		}
		this.yearStyledText.setLineJustify(0, this.yearStyledText.getLineCount(), true);
 		
 		// Setting study url
 		if (study.getUrl() == null) {
 			this.urlStyledText.setText("");
 		} else {
 			this.urlStyledText.setText(study.getUrl());
 		}
 		this.urlStyledText.setLineJustify(0, this.urlStyledText.getLineCount(), true);
 		
 		// Setting study abstract
 		if (study.getAbstract() == null) {
 			this.abstractStyledText.setText("");
 		} else {
 			this.abstractStyledText.setText(study.getAbstract());
 		}
 		this.abstractStyledText.setLineJustify(0, this.abstractStyledText.getLineCount(), true);
 		
 		WidgetsUtil.refreshComposite(form.getBody());
 	}
 	
 	public void setLiteratureReview(LiteratureReview literatureReview){
 		this.literatureReview = literatureReview;
 	}
 	
 	public void createPartControlImpl(Composite parent) {
 		configureView(parent);
 		createStudyWidgets(parent);
 	}
 	
 	private void configureView(Composite parent) {
 		super.form.setText(super.form.getText() + " - Study analysis");
 		
 		GridLayout layoutData = new GridLayout(5, false);
 		layoutData.marginTop = 10;
 		layoutData.marginLeft = 10;
 		layoutData.marginRight = 20;
 		layoutData.marginBottom = 10;
 		
 		form.getBody().setLayout(layoutData);
 	}
 
 	private void createStudyWidgets(Composite parent) {
 		this.form.getBody().setLayoutData(new GridData(GridData.GRAB_VERTICAL, GridData.GRAB_HORIZONTAL));
 
 		GridData layoutData;
 		
 		// Code widgets
 		Label codeLabel = toolkit.createLabel(form.getBody(), "CODE: ");
 		codeLabel.setFont(new Font(UIConstants.APP_DISPLAY, UIConstants.SYSTEM_FONT_NAME, UIConstants.SYSTEM_FONT_HEIGHT, SWT.BOLD));
 		layoutData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
 		layoutData.horizontalSpan = 1;
 		codeLabel.setLayoutData(layoutData);
 		this.codeStyledText = new StyledText(form.getBody(), SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP);
 		this.codeStyledText.setFont(new Font(UIConstants.APP_DISPLAY, UIConstants.SYSTEM_FONT_NAME, UIConstants.SYSTEM_FONT_HEIGHT, SWT.BOLD));
 		layoutData = new GridData(GridData.FILL_HORIZONTAL);
 		layoutData.horizontalSpan = 4;
 		this.codeStyledText.setLayoutData(layoutData);
 		this.codeStyledText.addFocusListener(new StyleTextFocusHandler());
 		this.toolkit.adapt(this.codeStyledText, true, true);
 		
 		// Title widgets
 		Label titleLabel = toolkit.createLabel(form.getBody(), "TITLE: ");
 		titleLabel.setFont(new Font(UIConstants.APP_DISPLAY, UIConstants.SYSTEM_FONT_NAME, UIConstants.SYSTEM_FONT_HEIGHT, SWT.BOLD));
 		layoutData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
 		layoutData.horizontalSpan = 1;
 		titleLabel.setLayoutData(layoutData);
 		this.titleStyledText = new StyledText(form.getBody(), SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP);
 		this.titleStyledText.setFont(new Font(UIConstants.APP_DISPLAY, UIConstants.SYSTEM_FONT_NAME, UIConstants.SYSTEM_FONT_HEIGHT, SWT.BOLD));
 		layoutData = new GridData(GridData.FILL_HORIZONTAL);
 		layoutData.horizontalSpan = 4;
 		this.titleStyledText.setLayoutData(layoutData);
 		this.titleStyledText.addFocusListener(new StyleTextFocusHandler());
 		this.toolkit.adapt(this.titleStyledText, true, true);
 		
 		// Status widgets
 		Label statusLabel = toolkit.createLabel(form.getBody(), "STATUS: ");
 		layoutData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
 		layoutData.horizontalSpan = 1;
 		statusLabel.setLayoutData(layoutData);
 		this.statusStyledText = new StyledText(form.getBody(), SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP);
 		layoutData = new GridData(GridData.FILL_HORIZONTAL);
 		layoutData.horizontalSpan = 4;
 		this.statusStyledText.setLayoutData(layoutData);
 		this.statusStyledText.addFocusListener(new StyleTextFocusHandler());
 		this.toolkit.adapt(this.statusStyledText, true, true);
 		
 		// Source widgets
 		Label sourceLabel = toolkit.createLabel(form.getBody(), "SOURCE: ");
 		layoutData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
 		layoutData.horizontalSpan = 1;
 		sourceLabel.setLayoutData(layoutData);
 		this.sourceStyledText = new StyledText(form.getBody(), SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP);
 		layoutData = new GridData(GridData.FILL_HORIZONTAL);
 		layoutData.horizontalSpan = 4;
 		this.sourceStyledText.setLayoutData(layoutData);
 		this.sourceStyledText.addFocusListener(new StyleTextFocusHandler());
 		this.toolkit.adapt(this.sourceStyledText, true, true);
 		
 		// Authors widgets
 		Label authorsLabel = toolkit.createLabel(form.getBody(), "AUTHORS: ");
 		layoutData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
 		layoutData.horizontalSpan = 1;
 		authorsLabel.setLayoutData(layoutData);
 		this.authorsStyledText = new StyledText(form.getBody(), SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP);
 		layoutData = new GridData(GridData.FILL_HORIZONTAL);
 		layoutData.horizontalSpan = 4;
 		this.authorsStyledText.setLayoutData(layoutData);
 		this.authorsStyledText.addFocusListener(new StyleTextFocusHandler());
 		this.toolkit.adapt(this.authorsStyledText, true, true);
 		
 		// Institutions widgets
 		Label institutionsLabel = toolkit.createLabel(form.getBody(), "INSTITUTIONS: ");
 		layoutData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
 		layoutData.horizontalSpan = 1;
 		institutionsLabel.setLayoutData(layoutData);
 		this.institutionsStyledText = new StyledText(form.getBody(), SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP);
 		layoutData = new GridData(GridData.FILL_HORIZONTAL);
 		layoutData.horizontalSpan = 4;
 		this.institutionsStyledText.setLayoutData(layoutData);
 		this.institutionsStyledText.addFocusListener(new StyleTextFocusHandler());
 		this.toolkit.adapt(this.institutionsStyledText, true, true);
 		
 		// Countries widgets
 		Label countriesLabel = toolkit.createLabel(form.getBody(), "COUNTRIES: ");
 		layoutData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
 		layoutData.horizontalSpan = 1;
 		countriesLabel.setLayoutData(layoutData);
 		this.countriesStyledText = new StyledText(form.getBody(), SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP);
 		layoutData = new GridData(GridData.FILL_HORIZONTAL);
 		layoutData.horizontalSpan = 4;
 		this.countriesStyledText.setLayoutData(layoutData);
 		this.countriesStyledText.addFocusListener(new StyleTextFocusHandler());
 		this.toolkit.adapt(this.countriesStyledText, true, true);
 		
		// Year widgets
		Label yearLabel = toolkit.createLabel(form.getBody(), "YEAR: ");
		layoutData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
		layoutData.horizontalSpan = 1;
		yearLabel.setLayoutData(layoutData);
		this.yearStyledText = new StyledText(form.getBody(), SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP);
		layoutData = new GridData(GridData.FILL_HORIZONTAL);
		layoutData.horizontalSpan = 4;
		this.yearStyledText.setLayoutData(layoutData);
		this.yearStyledText.addFocusListener(new StyleTextFocusHandler());
		this.toolkit.adapt(this.yearStyledText, true, true);
		
 		// URL widgets
 		Label urlLabel = toolkit.createLabel(form.getBody(), "URL: ");
 		layoutData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
 		layoutData.horizontalSpan = 1;
 		urlLabel.setLayoutData(layoutData);
 		this.urlStyledText = new StyledText(form.getBody(), SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP);
 		layoutData = new GridData(GridData.FILL_HORIZONTAL);
 		layoutData.horizontalSpan = 4;
 		this.urlStyledText.setLayoutData(layoutData);
 		this.urlStyledText.addFocusListener(new StyleTextFocusHandler());
 		this.toolkit.adapt(this.urlStyledText, true, true);
 		
 		// Abstract widgets
 		Label abstractLabel = toolkit.createLabel(form.getBody(), "ABSTRACT:");
 		layoutData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
 		layoutData.horizontalSpan = 1;
 		abstractLabel.setLayoutData(layoutData);
 		this.abstractStyledText = new StyledText(form.getBody(), SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP);
 		layoutData = new GridData(GridData.FILL_HORIZONTAL);
 		layoutData.horizontalSpan = 4;
 		abstractStyledText.setLayoutData(layoutData);
 		this.abstractStyledText.addFocusListener(new StyleTextFocusHandler());
 		this.toolkit.adapt(this.abstractStyledText, true, true);
 		
 		// Back button
 		layoutData = new GridData(GridData.HORIZONTAL_ALIGN_END);
 		Button back = toolkit.createButton(form.getBody(), "Back", SWT.PUSH);
 		layoutData.horizontalSpan = 1;
 		layoutData.grabExcessVerticalSpace = true;
 		layoutData.verticalAlignment = SWT.END;
 		back.setLayoutData(layoutData);
 		back.addSelectionListener(new BackButtonHandler());
 		
 		// Include button
 		Button include = toolkit.createButton(form.getBody(), "Include", SWT.PUSH);
 		layoutData.horizontalSpan = 1;
 		layoutData.grabExcessVerticalSpace = true;
 		layoutData.verticalAlignment = SWT.END;
 		include.setLayoutData(layoutData);
 		layoutData = new GridData();
 		include.addSelectionListener(new IncludeButtonHandler());
 		
 		// Exclude button
 		Button exclude = toolkit.createButton(form.getBody(), "Exclude", SWT.PUSH);
 		layoutData.horizontalSpan = 1;
 		layoutData.grabExcessVerticalSpace = true;
 		layoutData.verticalAlignment = SWT.END;
 		exclude.setLayoutData(layoutData);
 		layoutData = new GridData();
 		exclude.addSelectionListener(new ExcludeButtonHandler());
 		
 		// Skip button
 		Button skip = toolkit.createButton(form.getBody(), "Skip", SWT.PUSH);
 		layoutData.horizontalSpan = 1;
 		layoutData.grabExcessVerticalSpace = true;
 		layoutData.verticalAlignment = SWT.END;
 		skip.setLayoutData(layoutData);
 		skip.addSelectionListener(new SkipButtonHandler());
 		
 		// View all studies link
 		Hyperlink studyLink = toolkit.createHyperlink(form.getBody(), "View all studies", SWT.WRAP);
 		GridData studyLinkLayout = new GridData(GridData.VERTICAL_ALIGN_END);
 		layoutData.horizontalSpan = 1;
 		studyLinkLayout.grabExcessVerticalSpace = true;
 		studyLinkLayout.horizontalAlignment = SWT.END;
 		studyLink.setLayoutData(studyLinkLayout);
 		studyLink.addHyperlinkListener(new LiteratureReviewStudiesLinkHandler());
 	}
 	
 	public void back(){
 
 		int index = 0;
 		Study previewStudy;
 		
 		for (Study study : literatureReview.getStudies()) {
 			if(study.getId() == StudyAnalysisView.this.study.getId()){
 				index = literatureReview.getStudies().indexOf(study);
 				break;
 			}
 		}
 		
 		if((index - 1) >= 0){
 			previewStudy = literatureReview.getStudies().get(index - 1);
 			this.setStudy(previewStudy);
 		}
 		
 		else{
 			IPerspectiveRegistry perspectiveRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();
 			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
 			activePage.setPerspective(perspectiveRegistry.findPerspectiveWithId(LiteratureReviewStudiesPerspective.ID));
 			
 			LiteratureReviewStudiesView literatureReviewStudiesView = (LiteratureReviewStudiesView) ReviewerViewRegister.getView(LiteratureReviewStudiesView.ID);
 			literatureReviewStudiesView.setLiteratureReview(literatureReview);
 		}
 	}
 	
 	public void skip(){
 		
 		int index = 0;
 		Study nextStudy;
 		
 		for (Study study : literatureReview.getStudies()) {
 			if(study.getId() == StudyAnalysisView.this.study.getId()){
 				index = literatureReview.getStudies().indexOf(study);
 				break;
 			}
 		}
 		
 		if(literatureReview.getStudies().size() > (index + 1)){
 			nextStudy = literatureReview.getStudies().get(index + 1);
 			this.setStudy(nextStudy);
 		}
 		else{
 			IPerspectiveRegistry perspectiveRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();
 			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
 			activePage.setPerspective(perspectiveRegistry.findPerspectiveWithId(LiteratureReviewStudiesPerspective.ID));
 			
 			LiteratureReviewStudiesView literatureReviewStudiesView = (LiteratureReviewStudiesView) ReviewerViewRegister.getView(LiteratureReviewStudiesView.ID);
 			literatureReviewStudiesView.setLiteratureReview(literatureReview);
 		}
 	}
 
 	public void setFocus() {
 
 	}
 	
 	public class BackButtonHandler implements SelectionListener {
 
 		public void widgetSelected(SelectionEvent e) {
 			
 			StudyAnalysisView.this.back();
 		}
 
 		public void widgetDefaultSelected(SelectionEvent e) {
 			
 		}
 		
 	}
 	
 	private class IncludeButtonHandler implements SelectionListener {
 
 		public void widgetSelected(SelectionEvent e) {
 			study.setStatus(Study.StudyStatus.INCLUDED);
 			StudyController studyController = new StudyController();
 			studyController.updateStudy(study);
 			StudyAnalysisView.this.skip();
 		}
 
 		public void widgetDefaultSelected(SelectionEvent e) {
 			
 		}
 		
 	}
 	
 	public class ExcludeButtonHandler implements SelectionListener {
 
 		public void widgetSelected(SelectionEvent e) {
 			study.setStatus(Study.StudyStatus.EXCLUDED);
 			StudyController studyController = new StudyController();
 			studyController.updateStudy(study);
 			StudyAnalysisView.this.skip();
 		}
 
 		public void widgetDefaultSelected(SelectionEvent e) {
 			
 		}
 		
 	}
 	
 	public class SkipButtonHandler implements SelectionListener {
 
 		public void widgetSelected(SelectionEvent e) {
 			
 			StudyAnalysisView.this.skip();
 		}
 
 		public void widgetDefaultSelected(SelectionEvent e) {
 			
 		}
 		
 	}
 	
 	private class LiteratureReviewStudiesLinkHandler implements IHyperlinkListener {
 
 		public void linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent e) {
 			
 		}
 
 		public void linkExited(org.eclipse.ui.forms.events.HyperlinkEvent e) {
 			
 		}
 
 		public void linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent e) {
 			IPerspectiveRegistry perspectiveRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();
 			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
 			activePage.setPerspective(perspectiveRegistry.findPerspectiveWithId(LiteratureReviewStudiesPerspective.ID));
 			
 			LiteratureReviewStudiesView literatureReviewStudiesView = (LiteratureReviewStudiesView) ReviewerViewRegister.getView(LiteratureReviewStudiesView.ID);
 			literatureReviewStudiesView.setLiteratureReview(literatureReview);
 		}
 		
 	}
 
 	private class StyleTextFocusHandler implements FocusListener {
 
 		public void focusGained(FocusEvent e) {
 			
 		}
 
 		public void focusLost(FocusEvent e) {
 			StyledText sourceWidget = (StyledText) e.getSource();
 			sourceWidget.setSelection(0);
 		}
 		
 	}
 	
 	private class SearchProviderConfiguratorElementComparator implements Comparator<IConfigurationElement> {
 
 		public int compare(IConfigurationElement config1, IConfigurationElement config2) {
 			return config1.getAttribute("friendly.name").compareTo(config2.getAttribute("friendly.name"));
 		}
 		
 	}
 	
 }
