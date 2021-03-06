 package fi.csc.microarray.client.operation;
 
 import java.awt.CardLayout;
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.Cursor;
 import java.awt.Dimension;
 import java.awt.FlowLayout;
 import java.awt.GridBagConstraints;
 import java.awt.GridBagLayout;
 import java.awt.GridLayout;
 import java.awt.Insets;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 import java.util.Collection;
 import java.util.List;
 import java.util.Vector;
 
 import javax.swing.BorderFactory;
 import javax.swing.JButton;
 import javax.swing.JLabel;
 import javax.swing.JPanel;
 import javax.swing.JScrollPane;
 import javax.swing.JTextArea;
 import javax.swing.JTextField;
 import javax.swing.JToolBar;
 import javax.swing.ScrollPaneConstants;
 import javax.swing.SwingConstants;
 import javax.swing.event.CaretEvent;
 import javax.swing.event.CaretListener;
 
 import org.apache.log4j.Logger;
 
 import com.jgoodies.looks.HeaderStyle;
 import com.jgoodies.looks.Options;
 import com.jgoodies.uif_lite.panel.SimpleInternalFrame;
 
 import fi.csc.microarray.client.ClientApplication;
 import fi.csc.microarray.client.Session;
 import fi.csc.microarray.client.dialog.ChipsterDialog.DetailsVisibility;
 import fi.csc.microarray.client.dialog.DialogInfo.Severity;
 import fi.csc.microarray.client.operation.OperationDefinition.Suitability;
 import fi.csc.microarray.client.operation.parameter.Parameter;
 import fi.csc.microarray.client.operation.parameter.ToolParameterPanel;
 import fi.csc.microarray.client.selection.DatasetChoiceEvent;
 import fi.csc.microarray.client.tasks.TaskException;
 import fi.csc.microarray.constants.VisualConstants;
 import fi.csc.microarray.description.SADLParser.ParseException;
 import fi.csc.microarray.exception.MicroarrayException;
 
 /**
  * The main panel for all operation, parameter and visualization choices in
  * the client mainframe.
  * 
  * @author Janne KÃ¤ki, Petri KlemelÃ¤
  *
  */
 @SuppressWarnings("serial")
 public class OperationPanel extends JPanel
 							implements ActionListener, PropertyChangeListener {
     // Logger for this class
     private static final Logger logger = Logger.getLogger(OperationPanel.class);
     
 	private static final String OPERATION_LIST_TITLE = "Analysis tools";
 	private static final String SHOW_PARAMETERS_TEXT = "Show parameters";
 	private static final String HIDE_PARAMETERS_TEXT = "Hide parameters";	
 	
 	private static final String OPERATIONS = "Operations";
 	private static final String OPERATIONS_CATEGORIZED = "Categorized operations";
 	private static final String OPERATIONS_FILTERED = "Filtered operations";
 	private static final String PARAMETERS = "Parameters";
 	
 	private static final int WHOLE_PANEL_HEIGHT = 240;
 	private static final int WHOLE_PANEL_WIDTH= 660;
 	
 	private JPanel operationPanel;
 	private JPanel operationCardPanel;
 	private OperationChoicePanel operationChoicePanel;
 	private OperationFilterPanel operationFilterPanel;
 	private JTextField searchField;
 	private JPanel cardPanel;
 	private JTextArea detailField = new JTextArea();
 	
 	private JLabel suitabilityLabel = new JLabel();
 	private JButton sourceButton = new JButton("Show tool sourcecode");
 	private JButton helpButton = new JButton("More help");
 	private JButton parametersButton = new JButton();
 	private JButton executeButton = new JButton();
 	private JScrollPane detailFieldScroller;
 	private boolean isParametersVisible = false;
 	
 	private ExecutionItem chosenOperation = null;
 	private ClientApplication application = Session.getSession().getApplication();
 	
 	/**
 	 * Creates a new OperationPanel.
 	 * 
 	 * @param client The client under whose command this panel is assigned.
 	 */
 	public OperationPanel(Collection<OperationCategory> parsedCategories) throws ParseException {
 		super(new GridBagLayout());
 		this.setPreferredSize(new Dimension(WHOLE_PANEL_WIDTH, WHOLE_PANEL_HEIGHT));
 		this.setMinimumSize(new Dimension(0,0));
 		
 		operationChoicePanel = new OperationChoicePanel(this, parsedCategories);
 		operationFilterPanel = new OperationFilterPanel(this, parsedCategories);
 		
 		cardPanel = new JPanel(new CardLayout());
 		
 		detailField.setEditable(false);
 		detailField.setLineWrap(true);
 		detailField.setWrapStyleWord(true);	
 		
 		detailFieldScroller = new JScrollPane(detailField);
 		detailFieldScroller.setHorizontalScrollBarPolicy(
 		        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);		
 
 		sourceButton.setEnabled(false);
         sourceButton.setToolTipText("View Source Code");
 		sourceButton.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {				
 				try {
 					application.showSourceFor(chosenOperation.getID());
 				} catch (TaskException je) {
 					application.reportException(je);
 				}
 			}			
 		});				
 				
 		helpButton.setEnabled(false);
 		helpButton.setToolTipText("More information about this tool");
 		helpButton.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				OperationDefinition od =
 				    chosenOperation instanceof OperationDefinition ?
 				            (OperationDefinition) chosenOperation :
 				            ((Operation)chosenOperation).getDefinition();
 				application.viewHelpFor(od);
 			}			
 		});
 				
 		parametersButton.addActionListener(this);
 		parametersButton.setEnabled(false);
 		parametersButton.setText(SHOW_PARAMETERS_TEXT);				
 
 		suitabilityLabel.setPreferredSize(new Dimension(
 				VisualConstants.INCOMPATIBLE_ICON.getIconHeight(),
 				VisualConstants.INCOMPATIBLE_ICON.getIconHeight()));		
 		
 		executeButton.setIcon(VisualConstants.DOUBLE_FORWARD_ICON);
 		executeButton.setDisabledIcon(VisualConstants.DOUBLE_FORWARD_BW_ICON);
 		executeButton.setText("<html><b>Run</b></html>");
 		executeButton.setHorizontalAlignment(SwingConstants.CENTER);
 		executeButton.setHorizontalTextPosition(SwingConstants.LEFT);
 		executeButton.setToolTipText("Run selected operation for selected datasets");
 		executeButton.addActionListener(this);
 		executeButton.setName("executeButton");
 		setExecuteButtonEnabled(false);
 		
 		detailFieldScroller.setBorder(
 				BorderFactory.createMatteBorder(1, 0, 0, 0, VisualConstants.OPERATION_LIST_BORDER_COLOR));
 		
 	    // Search bar
         JToolBar searchPanel = new JToolBar();
         searchPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 1));
         // Text field
         searchField = new JTextField(20);
         searchField.addCaretListener(new CaretListener() {
             public void caretUpdate(CaretEvent e) {
                 // Show filtered tools
                 JTextField field = (JTextField) e.getSource();
                 if (field.getText().length() > 0) {
                     operationFilterPanel.loadFilteredOperations(field.getText());
                     showOperationCard(OPERATIONS_FILTERED);
                 } else {
                     operationChoicePanel.deselectOperation();
                     showOperationCard(OPERATIONS_CATEGORIZED);
                 }
             }
         });
         // Clear search
         JButton showAllButton = new JButton(VisualConstants.CLOSE_FILE_ICON);
         showAllButton.setFocusPainted(false);
         showAllButton.setContentAreaFilled(false);
         showAllButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
         showAllButton.setBorder(null);
         showAllButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent arg0) {
                 searchField.setText("");
                 operationChoicePanel.deselectOperation();
                 showOperationCard(OPERATIONS_CATEGORIZED);
             }
         });
         searchPanel.add(new JLabel(VisualConstants.MAGNIFIER_ICON));
         searchPanel.add(searchField);
         searchField.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
         searchField.setPreferredSize(new Dimension(100, 22));
         searchField.add(showAllButton);
         //searchPanel.add(showAllButton);
         searchPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1,
                 VisualConstants.OPERATION_LIST_BORDER_COLOR));
 		
 		// Operation choice card contains two other cards:
 		// operations with categories and filtered operations
 		operationPanel = new JPanel(new GridBagLayout());
 		GridBagConstraints c = new GridBagConstraints();
 	    c.gridx = 0;
         c.gridy = 0;
         c.weightx = 1;
         c.weighty = 0;
         c.gridheight = 1;
         c.insets.set(0,0,0,0);
         c.fill = GridBagConstraints.BOTH;
         searchPanel.setPreferredSize(new Dimension(10, 23));
         searchPanel.setMinimumSize(new Dimension(10, 23));
         searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
         searchPanel.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.SINGLE);
         operationPanel.add(searchPanel, c);
         operationCardPanel = new JPanel(new CardLayout());
         c.gridy = 1;
         c.weightx = 1;
         c.weighty = 1;
         c.fill = GridBagConstraints.BOTH;
         c.insets.set(0,0,0,0);
         operationPanel.add(operationCardPanel, c);
         
         // Tool selection panels inside operation card
         operationCardPanel.add(operationChoicePanel, OPERATIONS_CATEGORIZED);
         operationCardPanel.add(operationFilterPanel, OPERATIONS_FILTERED);
         operationCardPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                 VisualConstants.OPERATION_LIST_BORDER_COLOR));
 
 	    // Add operation panel
 	    cardPanel.add(operationPanel, OPERATIONS);
         cardPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1,
                 VisualConstants.OPERATION_LIST_BORDER_COLOR));
 		
 	    // Help and execution panel
 		JPanel topLeftPanel = new JPanel(new GridBagLayout());
 		c = new GridBagConstraints();
 		c.gridx = 0;
 		c.gridy = 0;
 		c.gridheight = 1;
 		c.weightx = 0;
 		c.weighty = 0;				
 		c.fill = GridBagConstraints.NONE;
 		c.insets.set(0,10,0,10);
 		topLeftPanel.add(suitabilityLabel, c);
 		c.gridx++;
 		c.weightx = 1;
 		c.weighty = 1;				
 		c.fill = GridBagConstraints.BOTH;
 		c.insets.set(0,0,0,0);
 		topLeftPanel.add(parametersButton,c);
 		JPanel topPanel = new JPanel(new GridLayout(1,2));
 		topPanel.add(topLeftPanel);
 		topPanel.add(executeButton);
 		JPanel bottomPanel = new JPanel(new GridLayout(1,2));
 		bottomPanel.add(helpButton);
 		bottomPanel.add(sourceButton);
 		
 		// Add everything to the main panel
 		c.gridx = 0;
 		c.gridy = 0;
 		c.gridheight = 3;
 		c.weightx = 1;
 		c.weighty = 1;
 		c.fill = GridBagConstraints.BOTH;
 		c.insets.set(0,0,0,0);
 		this.add(cardPanel, c);
 		c.gridy = 0;
 		c.gridx = 1;
 		c.gridwidth = 1;
 		c.gridheight = 1;
 		c.weightx = 0;
 		c.weighty = 0;
 		this.add(topPanel,c);		
 		c.gridx = 1;
 		c.gridy++;		
 		c.gridheight = 1;
 		c.weightx = 0;
 		c.weighty = 1;
 		this.add(detailFieldScroller, c);
 		c.gridy++;		
 		c.weightx = 0;
 		c.weighty = 0;
 		this.add(bottomPanel, c);		
 
 		// start listening
 		Session.getSession().getApplication().addPropertyChangeListener(this);
 		
 	}
 	
 	public Vector<Component> getFocusComponents(){
 				
 		Vector<Component> order = new Vector<Component>();
 		order.addAll(operationChoicePanel.getFocusComponents());
 		//No easy way to add parametercomponents
 		//order.add(parametersButton);
 		order.add(executeButton);				
 		return order;
 	}
 	
 	/**
 	 * The method for listening the top right corner arrow button.
 	 * 
 	 * @param e The action event.
 	 */
 	public void actionPerformed(ActionEvent e) {
 		if (e.getSource() == parametersButton) {
 			parametersButtonClicked();
 		} else if (e.getSource() == executeButton ) {
 		    // Check if we can run the operation
 		    Suitability suitability = evaluateSuitability();
 		    
 		    if (!suitability.isOk()) {
 		        application.showDialog("Check parameters", suitability.toString(), "",
 		                               Severity.INFO, true,
 		                               DetailsVisibility.DETAILS_ALWAYS_HIDDEN);
 		        return;
 		    }
 		    
 		    // Run it	    
 			if (chosenOperation instanceof OperationDefinition) {
 				application.executeOperation((OperationDefinition)chosenOperation, null);
 			} else {				
 				try {
 					// we MUST clone the operation, or otherwise results share the same
 					// operation as long as it is executed and parameter panel is not closed
 					Operation clonedOperation = new Operation((Operation)chosenOperation);
 					application.executeOperation(clonedOperation);
 				} catch (MicroarrayException me) {
 					throw new RuntimeException(me);
 				}
 			}
 		}
 	}
 
 	private void parametersButtonClicked(){
 		if(!isParametersVisible){
 			showParameterPanel();			
 		} else {
 			showOperationsPanel();
 		}
 	}		
 	
 	private void setExecuteButtonEnabled(boolean enabled){
 		if (enabled){
 			executeButton.setEnabled(true);
 		} else {
 			executeButton.setEnabled(false);
 		}
 	}
 	
 	/**
 	 * Activate a certain card in the left panel.
 	 */
 	private void showCard(String card) {
 	    CardLayout cl = (CardLayout)(cardPanel.getLayout());
         cl.show(cardPanel, card);
 	}
 	
 	/**
      * Activate a certain card in the operation panel.
      * 
      * Currently you can choose between categorized operations and
      * a list of filtered operations.
      */
     private void showOperationCard(String card) {
         CardLayout cl = (CardLayout)(operationCardPanel.getLayout());
         cl.show(operationCardPanel, card);
     }
 
 	private void showParameterPanel() {			
 		showParametersTitle(true);
 		parametersButton.setText(HIDE_PARAMETERS_TEXT);
 
 		try {
 		    // Display parameters for selected operation
 			if (chosenOperation instanceof Operation) {
 				chosenOperation = (Operation)chosenOperation;
 
 			} else if (chosenOperation instanceof OperationDefinition) {
 				chosenOperation = new Operation((OperationDefinition)chosenOperation,
 				        application.getSelectionManager().getSelectedDatasAsArray());
 
 			} else {
 				throw new RuntimeException("wrong type: " + chosenOperation.getClass().getSimpleName());				
 			}
 			
 			cardPanel.add(new ToolParameterPanel((Operation)chosenOperation, this),PARAMETERS);
 			showCard(PARAMETERS);
 		    isParametersVisible = true;
 
 		} catch (MicroarrayException e) {
 			application.reportException(e);
 		}
 	}
 
 	private void showOperationsPanel() {       
         showParametersTitle(false);
         parametersButton.setText(SHOW_PARAMETERS_TEXT);
         isParametersVisible = false;
         showCard(OPERATIONS);
 	}
 	
 	/**
 	 * Sets the title of this panel (shown in the TitledBorder) according to
 	 * the received command word (suggesting either a generic "Operations"
 	 * title or a more specific one, if an operation is already selected).
 	 * The selected dataset will also be taken into accound when setting
 	 * the title.
 	 * 
 	 * @param commandWord Either OperationPanel.showOperationsCommand or
 	 * 					  OperationPanel.showParametersCommand.
 	 */
 	private void showParametersTitle(boolean showParametersTitle) {
 		String title;
 		if (showParametersTitle) {
 			// ExecutionItem oper =
 			//	operationChoiceView.getSelectedOperation();
 			title = OPERATION_LIST_TITLE + 
 			  " - " + this.chosenOperation.getCategoryName() +
			  " - " + this.chosenOperation.getDisplayName();
 		} else {
 			title = OPERATION_LIST_TITLE;
 		}
 		((SimpleInternalFrame)this.getParent()).setTitle(title);
 		this.repaint();
 	}
 		
 	/**
 	 * Shows the "details" (that is, description text and suitability
 	 * evaluation) for the given operation. Suitability is evaluated for
 	 * the currently chosen dataset.
 	 * 
 	 * @param operation The operation (or operation definition, or workflow)
 	 * 					whose details are to be shown.
 	 */
 	public void selectOperation(ExecutionItem operation) {
 		
 		// update source button and operation description text
 		
 		this.chosenOperation = operation;
 		this.showParametersTitle(false);
 		
 		this.showOperationInfoText();
 		
 		// update suitability label and action buttons
 		if (operation != null) {
 			parametersButton.setEnabled(true);
 			
 			Suitability suitability = evaluateSuitability();
 			if (suitability.isImpossible()) {
 				makeButtonsEnabled(false);
 			} else {
 				makeButtonsEnabled(true);
 			}
 			
 			if( suitability.isImpossible()){
 				suitabilityLabel.setIcon(VisualConstants.INCOMPATIBLE_ICON);
 			} else if( suitability.isOk()){
 				suitabilityLabel.setIcon(VisualConstants.SUITABLE_ICON);
 			} else {
 				suitabilityLabel.setIcon(VisualConstants.SUITABLE_ICON);
 			}
 					
 			suitabilityLabel.setToolTipText(" " + suitability.toString());
 		} else {
 			makeButtonsEnabled(false);
 			parametersButton.setEnabled(false);
 			suitabilityLabel.setIcon(null);
 			suitabilityLabel.setToolTipText("");
 		}
 	}
 	
 	/**
 	 * Sets new info text and updates text margins if the vertical scrollbar 
 	 * appers
 	 * 
 	 * @param text
 	 * @param color
 	 * @param enable
 	 */
 	public void setInfoText(String text, Color color, boolean enable) {
 		detailField.setForeground(color);
 		detailField.setText(text);
 		
 		// Increases text margins if the scrollbar appears
 		updateDetailFieldMargin();
 	}
 	
 	public void showOperationInfoText() {
 		if (chosenOperation != null) {
 			setInfoText(chosenOperation.getDescription(), Color.BLACK, true);
 			sourceButton.setEnabled(true);
 			helpButton.setEnabled(true);
 		} else {
 			setInfoText("", Color.BLACK, false);
 			sourceButton.setEnabled(false);
 			helpButton.setEnabled(false);
 		}
 	}
 
 	/**
 	 * Increases the text margin if the scrollbar appers. The problem is that 
 	 * word wrapping is done before scrollbar appers and some of the text is left 
 	 * behind scrollbar.
 	 * 
 	 * @author mkoski
 	 */
 	private void updateDetailFieldMargin(){
 		// TODO I think the word wrapping problem should be fixed in a some better way
 		// than increasing the margin
 		
 		// Gets the scroller and sets more margin if the scrollbar is visible
 		if(detailFieldScroller.getVerticalScrollBar().isVisible()){
 			logger.debug("vertical scrollbar is visible");
 			detailField.setMargin(new Insets(2,2,2,2+ detailFieldScroller.getVerticalScrollBar().getWidth()));
 		} else {
 			logger.debug("vertical scrollbar is not visible");
 			detailField.setMargin(new Insets(2,2,2,2));
 		}
 	}
 		
 	/**
 	 * Enables (or disables) action - the two buttons in the top right corner,
 	 * to be exact. This prevents from enabling an operation without selecting
 	 * a dataset first.
 	 * 
 	 * @param enabled Whether action is to be enabled or not.
 	 */
 	public void enableAction(boolean enabled) {
 		if (!evaluateSuitability().isImpossible()) {
 			makeButtonsEnabled(enabled);			
 		}
 		if(!enabled){
 			suitabilityLabel.setIcon(null);
 		}
 	}
 	
 	private void makeButtonsEnabled(boolean enabled) {
 		//if(!enabled){
 		//	this.showOperationsPanel();
 		//}
 		setExecuteButtonEnabled(enabled);
 	}
 	
 	/**
 	 * Datset selection changed.
 	 */
 	public void propertyChange(PropertyChangeEvent dataEvent) {
 		if(dataEvent instanceof DatasetChoiceEvent) {
 			logger.debug("chosen data " +
 			        application.getSelectionManager().getSelectedDataBean() +
 			        " (possible one among many)");
 			
             // Reselect operation definition, so suitability is recalculated
 			// and parameter values are set to default
             OperationDefinition chosenOperationDefinition = 
                     chosenOperation instanceof Operation ?
                     ((Operation)chosenOperation).getDefinition() :
                     (OperationDefinition)chosenOperation;
             selectOperation(chosenOperationDefinition);
             
             // In case parameter panel is open, open operations panel,
             // because we have just nulled the parameter values
             showOperationsPanel();
 		}
 	}
 	
 	private Suitability evaluateSuitability() {
 		if (chosenOperation == null) {
 			return Suitability.IMPOSSIBLE;
 		}
 		
 		// Check input dataset suitability
 		Suitability suitability = chosenOperation.evaluateSuitabilityFor(
 		        application.getSelectionManager().getSelectedDataBeans());
 		
 		// Get parameters so we could check if required
 		// parameters aren't empty
 		List<Parameter> params;
 		if (chosenOperation instanceof Operation) {
 		    params = ((Operation)chosenOperation).getParameters();
 		} else {
 		    params = ((OperationDefinition)chosenOperation).getParameters();
 		}
         
         // Check parameter suitability
         for (Parameter param : params) {
             // Required parameters can not be empty
             if (!param.isOptional() && (param.getValue() == null ||
                                         param.getValue().equals(""))) {
                 if (suitability.isOk()) {
                     return Suitability.EMPTY_REQUIRED_PARAMETERS;
                 }
             }
         }
 		
 		return suitability;
 	}
 }
