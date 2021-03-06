 package fi.opendemocracy.web.ui;
 
 import java.math.BigDecimal;
 import java.util.Arrays;
 import java.util.Date;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 import javax.persistence.TypedQuery;
 
 import com.vaadin.Application;
 import com.vaadin.addon.jpacontainer.JPAContainer;
 import com.vaadin.annotations.AutoGenerated;
 import com.vaadin.data.util.BeanItem;
 import com.vaadin.data.util.BeanItemContainer;
 import com.vaadin.event.ItemClickEvent;
 import com.vaadin.event.ItemClickEvent.ItemClickListener;
 import com.vaadin.spring.roo.addon.annotations.RooVaadinEntityView;
 import com.vaadin.terminal.ThemeResource;
 import com.vaadin.ui.AbsoluteLayout;
 import com.vaadin.ui.Alignment;
 import com.vaadin.ui.Button;
 import com.vaadin.ui.Component;
 import com.vaadin.ui.CustomComponent;
 import com.vaadin.ui.HorizontalLayout;
 import com.vaadin.ui.Label;
 import com.vaadin.ui.Panel;
 import com.vaadin.ui.RichTextArea;
 import com.vaadin.ui.Table;
 import com.vaadin.ui.TextField;
 import com.vaadin.ui.VerticalLayout;
 import com.vaadin.ui.Window;
 import com.vaadin.ui.Button.ClickEvent;
 import com.vaadin.ui.themes.Reindeer;
 
 import fi.opendemocracy.domain.Category;
 import fi.opendemocracy.domain.Expert;
 import fi.opendemocracy.domain.ODUser;
 import fi.opendemocracy.domain.Proposition;
 import fi.opendemocracy.domain.PropositionOption;
 import fi.opendemocracy.domain.Representation;
 import fi.opendemocracy.domain.Vote;
 import fi.opendemocracy.web.TabNavigator;
 import fi.opendemocracy.web.ThemeConstants;
 
 public class CategoryEntityView extends CustomComponent implements
 TabNavigator.View {
 	private AbsoluteLayout mainLayout;
 	private Panel scrollPanel;
 	private VerticalLayout scrollContent;
 	private final Category sourceCategory;
 	private final BeanItemContainer<Expert> expertContainer;
 	private final BeanItemContainer<Proposition> propositionContainer;
 	private VerticalLayout trustExpertForm;
 	private Window wDialog; 
 	
 	public CategoryEntityView(Category c){
 		sourceCategory = c;
 		final Set<Category> finderSet = new HashSet<Category>(Arrays.asList(sourceCategory));
 		final List<Expert> expertList = Expert.findExpertsByCategory(sourceCategory).getResultList();
 		final List<Proposition> propositionList = Proposition.findPropositionsByCategories(finderSet).getResultList();

		//Deprecated - alternatives?
 		expertContainer = (expertList.size() > 0) ? new BeanItemContainer<Expert>(expertList) : null;
 		propositionContainer = (propositionList.size() > 0) ? new BeanItemContainer<Proposition>(propositionList) : null;
 		
		
 		mainLayout = buildMainLayout();
 		setCompositionRoot(mainLayout);
 		setCaption(c.getName());
 		setIcon(ThemeConstants.TAB_ICON_CATEGORIES);
 	}
 	
 	private AbsoluteLayout buildMainLayout() {
 		// common part: create layout
 		mainLayout = new AbsoluteLayout();
         mainLayout.addStyleName("blue-bottom");
 
 		// top-level component properties
 		setWidth("100.0%");
 		setHeight("100.0%");
 
 		// scrollPanel
 		scrollPanel = buildScrollPanel();
 		mainLayout.addComponent(scrollPanel);
 
 		return mainLayout;
 	}
 
 	private Panel buildScrollPanel() {
 		// common part: create layout
 		scrollPanel = new Panel();
 		scrollPanel.setWidth("100.0%");
 		scrollPanel.setHeight("100.0%");
 		scrollPanel.setImmediate(false);
 
 		// scrollContent
 		scrollContent = buildScrollContent();
 		scrollPanel.setContent(scrollContent);
 
 		return scrollPanel;
 	}
 
 	private VerticalLayout buildScrollContent() {
 		// common part: create layout
 		scrollContent = new VerticalLayout();
 		scrollContent.setWidth("100.0%");
 		scrollContent.setHeight("-1px");
 		scrollContent.setImmediate(false);
 		scrollContent.setMargin(true);
 		scrollContent.setSpacing(true);
 
 		Label title = new Label("<h1>" + sourceCategory.getName() + "</h1>");
 		Label description = new Label("<p>" + sourceCategory.getDescription() + "</p>");
 		
 		Button toggleExpertise = new Button("Claim expertise", new Button.ClickListener() {
 			@Override
 			public void buttonClick(ClickEvent event) {
 				getWindow().showNotification("CLCICKCKK!");
 			}
 		});
 		
 		title.setContentMode(Label.CONTENT_XHTML);
 		description.setContentMode(Label.CONTENT_XHTML);
 		
 		scrollContent.addComponent(title);
 		scrollContent.addComponent(description);
 		scrollContent.addComponent(toggleExpertise);
 
 		if(expertContainer != null){
 			final Table expertTable = setupNewTable(ThemeConstants.TAB_CAPTION_EXPERTS, ThemeConstants.TAB_ICON_EXPERTS);
 			expertTable.addGeneratedColumn("Representation", new ExpertColumnGenerator("representation"));
 			expertTable.addGeneratedColumn("Username", new ExpertColumnGenerator("username"));
 			expertTable.setContainerDataSource(expertContainer);
 			expertTable.setVisibleColumns(new Object[] {"Username", "Representation"});
 			expertTable.addListener(new ItemClickListener() {
 				@Override
 				public void itemClick(ItemClickEvent event) {
 					Object value = event.getItemId();
 					if(expertTable.getValue() == value && event.getButton() == ItemClickEvent.BUTTON_LEFT){
 						//TODO: Cleanup following line ;)
 						Expert e = (Expert) (((BeanItem<Expert>)expertTable.getItem(value)).getBean());
 						trustExpert(e);
 					}
 				}
 			});
 			scrollContent.addComponent(expertTable);
 		}
 		if(propositionContainer != null){
 			final Table propositionTable = setupNewTable(ThemeConstants.TAB_CAPTION_PROPOSITION, ThemeConstants.TAB_ICON_PROPOSITION);
 			propositionTable.setContainerDataSource(propositionContainer);
 			propositionTable.addGeneratedColumn("Author", new PropositionColumnGenerator("username"));
 			propositionTable.setVisibleColumns(new Object[] {"name", "Author"});
 			scrollContent.addComponent(propositionTable);
 		}
 		
 		
 		return scrollContent;
 	}
 
 	private Table setupNewTable(String title, ThemeResource icon){
 		Table table = new Table();
 		table.setSelectable(true);
 		table.setNullSelectionAllowed(false);
 		table.setCaption(title);
 		table.setIcon(icon);
         table.addStyleName(Reindeer.TABLE_STRONG);
 		return table;
 	}
 	
 	//Trust expert modal
 	private void trustExpert(final Expert e) {		
 		final Window main = getWindow();
 		final ODUser currentUser = (ODUser)getApplication().getUser();
 		if(currentUser == null){
 			getWindow().showNotification("TODO: View (no trusting) if logged out. LOGIN!");
 			return;
 		}
 		//TODO: Singleton
 		//if(trustExpertForm == null){
 			trustExpertForm = new VerticalLayout();
 			trustExpertForm.setMargin(true);
 			trustExpertForm.setSpacing(true);
 			trustExpertForm.setWidth("400px");
 			
 			final VoteOptionSlider trust = new VoteOptionSlider("Trust");
 			trustExpertForm.addComponent(new Label("Expert in " + e.getCategory().getName() + ": " + e.getOdUser().getUsername()));
 			
 			Label expertise = new Label(e.getExpertise());
 			expertise.setContentMode(Label.CONTENT_XHTML);
 			
 			trustExpertForm.addComponent(expertise);
 			trustExpertForm.addComponent(trust);
 			
 			wDialog = new Window("Add Option", trustExpertForm);
 			wDialog.setModal(true);
 			
 			Button cancel = new Button("Close", new Button.ClickListener() {
 				@Override
 				public void buttonClick(ClickEvent event) {
 					main.removeWindow(wDialog);
 				}
 			});
 			Button add = new Button("Confirm trust", new Button.ClickListener() {
 				@Override
 				public void buttonClick(ClickEvent event) {
 					Representation r = new Representation();
 					r.setExpert(e);
 					r.setOdUser(currentUser);
 					r.setTrust(BigDecimal.valueOf((Double)trust.getValue()));
 					r.setTs(new Date());
 					r.persist();
 					main.showNotification("Trust saved");
 					main.removeWindow(wDialog);
 				}
 
 			});
 			HorizontalLayout buttons = new HorizontalLayout();
 			buttons.addComponent(cancel);
 			buttons.addComponent(add);
 			buttons.setSpacing(true);
 			trustExpertForm.addComponent(buttons);
 			trustExpertForm.setComponentAlignment(buttons, Alignment.TOP_RIGHT);
 		//}
 		main.addWindow(wDialog);
 	}
 	
 	//Column generators for custom tables
 	//TODO: Make getters in domain classes instead of generators?
     private class ExpertColumnGenerator implements Table.ColumnGenerator {
     	private String column;
     	public ExpertColumnGenerator(String column){
     		this.column = column;
     	}
         public Component generateCell(Table source, Object itemId, Object columnId) {
         	BeanItem<Expert> item = (BeanItem<Expert>) source.getItem(itemId);
         	final Expert expert = item.getBean();
         	
         	if(column == "representation"){
 	        	//TODO: Custom TypedQuery with calculated representation value
 	        	List<Representation> representationList = Representation.findRepresentationsByExpertAndTrustGreaterThan(expert, BigDecimal.valueOf(0L)).getResultList();
 	        	if(representationList.size() == 0){
 	        		return new Label("0");	
 	        	}
	        	BigDecimal rep = new BigDecimal(0);
 	        	for(Representation r : representationList){
	        		rep.add(r.getTrust());
 	        	}
	        	return new Label(Integer.toString(rep.intValue()));
         	}else if (column == "username"){
         		String username = expert.getOdUser().getUsername();
         		if(username == null){
         			return new Label("N/A");
         		}
         		return new Label(username);
         	}
 			return new Label("Error");
         }
     }
 
 
     private class PropositionColumnGenerator implements Table.ColumnGenerator {
     	private String column;
     	public PropositionColumnGenerator(String column){
     		this.column = column;
     	}
         public Component generateCell(Table source, Object itemId, Object columnId) {
         	BeanItem<Proposition> item = (BeanItem<Proposition>) source.getItem(itemId);
         	final Proposition proposition = item.getBean();
         	if (column == "username"){
         		String username = proposition.getAuthor().getUsername();
         		if(username == null){
         			return new Label("N/A");
         		}
         		return new Label(username);
         	}
 			return new Label("Error");
         }
     }
 	
 	@Override
 	public void init(TabNavigator navigator, Application application) {
 		// TODO Auto-generated method stub
 		
 	}
 
 	@Override
 	public void navigateTo(String requestedDataId) {
 		// TODO Auto-generated method stub
 		
 	}
 
 	@Override
 	public String getWarningForNavigatingFrom() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 	
 	
 }
