 package edu.nrao.dss.client;
 
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 
 import com.extjs.gxt.ui.client.Style.SelectionMode;
 import com.extjs.gxt.ui.client.data.BaseModelData;
 import com.extjs.gxt.ui.client.data.BasePagingLoadResult;
 import com.extjs.gxt.ui.client.data.BasePagingLoader;
 import com.extjs.gxt.ui.client.data.DataField;
 import com.extjs.gxt.ui.client.data.DataReader;
 import com.extjs.gxt.ui.client.data.ModelData;
 import com.extjs.gxt.ui.client.data.ModelType;
 import com.extjs.gxt.ui.client.data.PagingLoader;
 import com.extjs.gxt.ui.client.event.ButtonEvent;
 import com.extjs.gxt.ui.client.event.ComponentEvent;
 import com.extjs.gxt.ui.client.event.Events;
 import com.extjs.gxt.ui.client.event.GridEvent;
 import com.extjs.gxt.ui.client.event.KeyListener;
 import com.extjs.gxt.ui.client.event.Listener;
 import com.extjs.gxt.ui.client.event.MenuEvent;
 import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
 import com.extjs.gxt.ui.client.event.SelectionChangedListener;
 import com.extjs.gxt.ui.client.event.SelectionListener;
 import com.extjs.gxt.ui.client.store.ListStore;
 import com.extjs.gxt.ui.client.store.Store;
 import com.extjs.gxt.ui.client.store.StoreEvent;
 import com.extjs.gxt.ui.client.store.StoreListener;
 import com.extjs.gxt.ui.client.widget.Component;
 import com.extjs.gxt.ui.client.widget.ContentPanel;
 import com.extjs.gxt.ui.client.widget.Dialog;
 import com.extjs.gxt.ui.client.widget.button.Button;
 import com.extjs.gxt.ui.client.widget.button.SplitButton;
 import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
 import com.extjs.gxt.ui.client.widget.form.TextField;
 import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
 import com.extjs.gxt.ui.client.widget.grid.CellEditor;
 import com.extjs.gxt.ui.client.widget.grid.CheckColumnConfig;
 import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
 import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
 import com.extjs.gxt.ui.client.widget.grid.EditorGrid;
 import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
 import com.extjs.gxt.ui.client.widget.layout.FitLayout;
 import com.extjs.gxt.ui.client.widget.menu.CheckMenuItem;
 import com.extjs.gxt.ui.client.widget.menu.Menu;
 import com.extjs.gxt.ui.client.widget.menu.MenuItem;
 import com.extjs.gxt.ui.client.widget.menu.SeparatorMenuItem;
 import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
 import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar;
 import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
 import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.http.client.RequestBuilder;
 import com.google.gwt.json.client.JSONArray;
 import com.google.gwt.json.client.JSONObject;
 import com.google.gwt.json.client.JSONValue;
 import com.google.gwt.user.client.Window;
 
 public class Explorer extends ContentPanel{
 	public Explorer(String url, ModelType mType) {
 		rootURL     = url;
 		modelType   = mType;
 		defaultDate = "";
 		pagingToolBar = null;
 	}
 	
 	public Explorer(String url, ModelType mType, PagingToolBar ptb)
 	{
 		rootURL = url;
 		modelType = mType;
 		defaultDate = "";
 		pagingToolBar = ptb;
 	}
 	
 	@SuppressWarnings("unchecked")
 	protected void initLayout(ColumnModel cm, Boolean createToolBar) {
 		
 		setHeaderVisible(false);
 		setLayout(new FitLayout());
 		setCommitState(false);
 		setAutoHeight(true);
 				
 		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, rootURL);
 
 		DataReader reader = new PagingJsonReader<BasePagingLoadResult>(modelType);
 		proxy  = new DynamicHttpProxy<BasePagingLoadResult<BaseModelData>>(builder);
 		loader = new BasePagingLoader<BasePagingLoadResult<BaseModelData>>(proxy, reader);  
 		loader.setRemoteSort(true);
 
 		store = new ListStore<BaseModelData>(loader);
 		
 	    grid  = new EditorGrid<BaseModelData>(store, cm);
 	    grid.setAutoHeight(true);
 	    
 		GridSelectionModel<BaseModelData> selectionModel = new GridSelectionModel<BaseModelData>();
 		selectionModel.setSelectionMode(SelectionMode.MULTI);
 		grid.setSelectionModel(selectionModel);
 		addPlugins();
 		add(grid);
 		grid.setBorders(true);
 
 		initListeners();
 		if (createToolBar) {
 		    initToolBar();
 		}
 		loadData();
 		
 		columnConfForm  = new ColumnConfigForm(this);
 		filterComboForm = new FilterComboForm(this);
 		
 	}
 	
 	private void addPlugins() {
 		for (CheckColumnConfig cb : checkBoxes) {
 			grid.addPlugin(cb);
 		}
 	}
 	
 	public void loadData() {
 		loader.load(0, getPageSize());
 	}
 	
 	private void initListeners() {
		grid.addListener(Events.AfterEdit, new Listener<GridEvent<BaseModelData>>() {
 			public void handleEvent(GridEvent<BaseModelData> ge) {
 				Object value = ge.getRecord().get(ge.getProperty());
 				for (BaseModelData model : grid.getSelectionModel()
 						.getSelectedItems()) {
 					store.getRecord(model).set(ge.getProperty(), value);
 				}
 			}
 		});
 		store.addStoreListener(new StoreListener<BaseModelData>() {
 			@Override
 			public void storeUpdate(StoreEvent<BaseModelData> se) {
 				save(se.getModel());
 			}
 		});
 	}
 	
 	private void save(ModelData model) {
 		if (!isCommitState()) {
 			return;
 		}
 		ArrayList<String> keys   = new ArrayList<String>();
         ArrayList<String> values = new ArrayList<String>();
 
         keys.add("_method");
         values.add("put");
 
         for (String name : model.getPropertyNames()) {
         	Object value = model.get(name);
             if (value != null) {
                 keys.add(name);
                 values.add(value.toString());
             }
         }
         JSONRequest.post(rootURL + "/" + ((Number) model.get("id")).intValue(),
 		         keys.toArray(new String[]{}),
 		         values.toArray(new String[]{}),
 		         new JSONCallbackAdapter());
         updateObservers();
 	}
 	
 	// to be implemented by children
 	public void registerObservers(){
 		
 	}
 	
 	// to be implemented by children
 	public void updateObservers(){
 		
 	}
 	
 	// to be implemented by children
 	public void viewObject() {
 		//return grid.getSelectionModel().getSelectedItem();
 	}
 	
 	// to be implemented by children
 	public void actionOnObject() {
 		//return grid.getSelectionModel().getSelectedItem();
 	}	
 	private void initToolBar() {
 		if (pagingToolBar == null)
 		{
 			pagingToolBar = new PagingToolBar(50);
 		}
 		
 		final TextField<String> pages = new TextField<String>();
 		pages.setWidth(30);
 		pages.setValue("50");
 		pages.addKeyListener(new KeyListener() {
 			public void componentKeyPress(ComponentEvent e) {
 				if (e.getKeyCode() == 13) {
 					int page_size = Integer.valueOf(pages.getValue()).intValue();
 					pagingToolBar.setPageSize(page_size);
 					setPageSize(page_size);
 					loadData();
 				}
 			}
 		});
 		pages.setTitle("Page Size");
 		pagingToolBar.add(pages);
 		setBottomComponent(pagingToolBar);
 		pagingToolBar.bind(loader);
 		
 		toolBar = new ToolBar();
 		setTopComponent(toolBar);
 		
 		if (showColumnsMenu) {
 			columnsItem = new Button("Columns");
 			columnsItem.setMenu(initColumnsMenu());
 			toolBar.add(columnsItem);
 		}
 		
 		viewItem = new Button("View");
 		toolBar.add(viewItem);
 		viewItem.setToolTip("view row.");
 		viewItem.addSelectionListener(new SelectionListener<ButtonEvent>() {
 	        @Override
 	        public void componentSelected(ButtonEvent ce) {
 	            viewObject();	
 	        }
 	    });
 		// hide this button by default
 		viewItem.setVisible(false);
 		
 		addItem = new Button("Add");
 		toolBar.add(addItem);
 		addItem.setToolTip("Add a new row.");
 		addItem.addSelectionListener(new SelectionListener<ButtonEvent>() {
 	        @Override
 	        public void componentSelected(ButtonEvent ce) {
 	        	HashMap<String, Object> fields = new HashMap<String, Object>();
 	        	if (defaultDate != "") {
 	        		fields.put("date", defaultDate);
 	        	}
 	        	addRecord(fields);
 	        }
 	    });
 		
 		final Button duplicateItem = new Button("Duplicate");
 		toolBar.add(duplicateItem);
 		duplicateItem.setToolTip("Copy a row.");
 		duplicateItem.setEnabled(false);
 		duplicateItem.addSelectionListener(new SelectionListener<ButtonEvent>() {
             @Override
             public void componentSelected(ButtonEvent be) {
                 addRecord(new HashMap<String, Object>(grid.getSelectionModel()
             			.getSelectedItem().getProperties()));
                 grid.getView().refresh(true);
             }
         });
 		
 		removeDialog = new Dialog();
 		removeDialog.setHeading("Confirmation");
 		removeDialog.addText("Remove record?");
 		removeDialog.setButtons(Dialog.YESNO);
 		removeDialog.setHideOnButtonClick(true);
 		removeApproval = removeDialog.getButtonById(Dialog.YES);
 		removeApproval.addSelectionListener(new SelectionListener<ButtonEvent>() {
 			@Override
 			public void componentSelected(ButtonEvent ce) {
 				Double id = grid.getSelectionModel().getSelectedItem().get("id");
 				JSONRequest.delete(rootURL + "/" + id.intValue(),
 						new JSONCallbackAdapter() {
 							public void onSuccess(JSONObject json) {
 								store.remove(grid.getSelectionModel()
 										.getSelectedItem());
 							}
 						});
 				updateObservers();
 			}
 		});	
 		removeDialog.hide();
 
 	
 		removeItem = new Button("Delete");
 		toolBar.add(removeItem);
 		removeItem.setToolTip("Delete a row.");
 		// make it so that children can override this behavior
 		setRemoveItemListener();
 	
 		// add a generic button that can be changed for whatever purpose a child class may have for it
 		actionItem = new Button("Action");
 		toolBar.add(actionItem);
 		//actionItem.setToolTip("view row.");
 		actionItem.addSelectionListener(new SelectionListener<ButtonEvent>() {
 	        @Override
 	        public void componentSelected(ButtonEvent ce) {
 	            actionOnObject();	
 	        }
 	    });
 		// hide this button by default
 		actionItem.setVisible(false);
 		
 		toolBar.add(new SeparatorToolItem());
 
 		filter = new FilterItem(Explorer.this, false);
 		toolBar.add(filter.getTextField());
 
 		for (SimpleComboBox<String> f : advancedFilters) {
 			toolBar.add(new SeparatorToolItem());
 		    toolBar.add(f);
 		}
 		toolBar.add(new SeparatorToolItem());
 		Button reset = new Button("Reset");
 		reset.addSelectionListener(new SelectionListener<ButtonEvent>() {
 			@Override
 			public void componentSelected(ButtonEvent ce) {
 				for (SimpleComboBox<String> f : advancedFilters) {
 					f.reset();
 				}
 				filter.getTextField().setValue("");
 			}
 		});
 		toolBar.add(reset);
 		toolBar.add(new SeparatorToolItem());
 		if (filterAction != null) {
 			toolBar.add(filterAction);
 		}
 		
 		toolBar.add(new FillToolItem());
 		toolBar.add(new SeparatorToolItem());
 
 		saveItem = new Button("Save");
 		toolBar.add(saveItem);
 
 		// Commit outstanding changes to the server.
 		saveItem.addSelectionListener(new SelectionListener<ButtonEvent>() {
 			@Override
 			public void componentSelected(ButtonEvent be) {
 				setCommitState(true);
 				store.commitChanges();
 				setCommitState(false);
 				//loadData();
 				//grid.getView().refresh(true);
 			}
 		});
 		
 		// Enable the "Duplicate" button only if there is a selection.
 		grid.getSelectionModel().addSelectionChangedListener(
 				new SelectionChangedListener<BaseModelData>() {
 					@Override
 					public void selectionChanged(SelectionChangedEvent<BaseModelData> se) {
 						duplicateItem.setEnabled(!grid.getSelectionModel().getSelectedItems().isEmpty());
 					}
 				});
 	}
 	
 	private Menu initColumnsMenu() {
 		final Menu menu = new Menu();
 		MenuItem saveConfig = new MenuItem("Save Column Combination");
 		saveConfig.addSelectionListener(new SelectionListener<MenuEvent>() {
 
 			public void componentSelected(MenuEvent ce) {
 				com.extjs.gxt.ui.client.widget.Window w = columnConfForm.getWindow();
 				columnConfForm.show();
 				w.show();
 			}
 			
 		});
 		
 		menu.add(saveConfig);
 		MenuItem removeConfigs = new MenuItem("Remove Checked Items");
 		removeConfigs.addSelectionListener(new SelectionListener<MenuEvent>() {
 
 			@Override
 			public void componentSelected(MenuEvent ce) {
 				for(Component mi : menu.getItems()){
 					//  Doing this the Python way. ;)
 					try {
 						final ColumnConfigMenuItem cmi = (ColumnConfigMenuItem) mi;
 						if (cmi.isChecked()){
 							HashMap<String, Object> data = new HashMap<String, Object>();
 							data.put("method_", "DELETE");
 							JSONRequest.post("/configurations/explorer/columnConfigs/" + cmi.config_id
 									       , data
 									       , new JSONCallbackAdapter() {
 								public void onSuccess(JSONObject json){
 									menu.remove(cmi);
 								}
 							});
 						}
 					} catch (ClassCastException e) {
 						
 					}
 					
 				}
 			}
 			
 		});
 		menu.add(removeConfigs);
 		menu.add(new SeparatorMenuItem());
 		
 		MenuItem all = new MenuItem("Restore All");
 		all.addSelectionListener(new SelectionListener<MenuEvent>() {
 
 			@Override
 			public void componentSelected(MenuEvent ce) {
 				for (ColumnConfig cc : grid.getColumnModel().getColumns()){
 					cc.setHidden(false);
 				}
 				grid.getView().refresh(true);	
 			}
 			
 		});
 		menu.add(all);
 		menu.add(new SeparatorMenuItem());
 		
 		HashMap<String, Object> data = new HashMap<String, Object>();
 		data.put("explorer", rootURL);
 		// Get save configurations from the server and populate them as menu items
 		JSONRequest.get("/configurations/explorer/columnConfigs", data, new JSONCallbackAdapter() {
 			public void onSuccess(JSONObject json){
 				JSONArray configs = json.get("configs").isArray();
 				for (int i = 0; i < configs.size(); ++i) {
 					JSONArray config = configs.get(i).isArray();
 					String config_id = config.get(1).isNumber().toString();
 					ColumnConfigMenuItem mi = 
 						new ColumnConfigMenuItem(grid
 							                   , config.get(0).isString().stringValue()
 							                   , config_id);
 					menu.add(mi);
 					columnConfigIds.add(config_id);
 				}
 			}
 		});
 		return menu;
 	}
 
 	protected FilterMenu initFilterMenu() {
 		filterMenu = new FilterMenu();
 		MenuItem saveCombos = new MenuItem("Save Filter Combination");
 		saveCombos.addSelectionListener(new SelectionListener<MenuEvent>() {
 
 			public void componentSelected(MenuEvent ce) {
 				com.extjs.gxt.ui.client.widget.Window w = filterComboForm.getWindow();
 				filterComboForm.show();
 				w.show();
 			}
 			
 		});
 		
 		filterMenu.add(saveCombos);
 		MenuItem removeCombos = new MenuItem("Remove Checked Items");
 		removeCombos.addSelectionListener(new SelectionListener<MenuEvent>() {
 
 			@Override
 			public void componentSelected(MenuEvent ce) {
 				for(Component mi : filterMenu.getItems()){
 					//  Doing this the Python way. ;)
 					try {
 						final FilterComboMenuItem cmi = (FilterComboMenuItem) mi;
 						if (cmi.isChecked()){
 							HashMap<String, Object> data = new HashMap<String, Object>();
 							data.put("method_", "DELETE");
 							JSONRequest.post("/configurations/explorer/filterCombos/" + cmi.combo_id
 									       , data
 									       , new JSONCallbackAdapter() {
 								public void onSuccess(JSONObject json){
 									filterMenu.remove(cmi);
 								}
 							});
 						}
 					} catch (ClassCastException e) {
 						
 					}
 					
 				}
 			}
 			
 		});
 		filterMenu.add(removeCombos);
 		filterMenu.add(new SeparatorMenuItem());
 		
 		//  TBF:  This is only used below to init the MenuItem outside the namespace
 		final Explorer e = this;
 		HashMap<String, Object> data = new HashMap<String, Object>();
 		data.put("explorer", rootURL);
 		// Get save configurations from the server and populate them as menu items
 		JSONRequest.get("/configurations/explorer/filterCombos", data, new JSONCallbackAdapter() {
 			public void onSuccess(JSONObject json){
 				JSONArray configs = json.get("configs").isArray();
 				for (int i = 0; i < configs.size(); ++i) {
 					JSONArray config = configs.get(i).isArray();
 					String filter_id = config.get(1).isNumber().toString();
 					FilterComboMenuItem mi = 
 						new FilterComboMenuItem(e
 							                   , config.get(0).isString().stringValue()
 							                   , filter_id);
 					filterMenu.add(mi);
 					filterComboIds.add(filter_id);
 				}
 			}
 		});
 		return filterMenu;
 	}
 	
 	protected void setRemoveItemListener() {
 		removeItem.addSelectionListener(new SelectionListener<ButtonEvent>() {
 			@Override
 			public void componentSelected(ButtonEvent be) {
 				removeDialog.show();
 			}
 		});
 	}
 
 	protected void addRecord(HashMap<String, Object> fields) {
 		//GWT.log(fields.toString(), null);
 		JSONRequest.post(rootURL, fields, new JSONCallbackAdapter() {
 			@Override
 			public void onSuccess(JSONObject json) {
 				BaseModelData model = new BaseModelData();
 				for (int i = 0; i < modelType.getFieldCount(); ++i) {
 					DataField field = modelType.getField(i);
 					String fName = field.getName();
 					if (json.containsKey(fName)) {
 						// Set model value dependent on data type
 						JSONValue value = json.get(fName);
 						if (value.isNumber() != null) {
 							double numValue = value.isNumber().doubleValue();
 							//TODO conditional for case to int
 							//model.set(fName, (int) numValue);
 							model.set(fName, numValue);
 						} else if (value.isBoolean()!= null) {
 							model.set(fName, value.isBoolean().booleanValue());
 						} else if (value.isString() != null) {
 							model.set(fName, value.isString().stringValue());
 						} else if (value.isNull() != null) {
 							// TODO: should this really be a no-op
 							//Window.alert("null JSON value type");
 						} else {
 							Window.alert("unknown JSON value type");
 						}
 					}
 				}
 				grid.stopEditing();
 				store.insert(model, 0);
 				//grid.getView().refresh(true);
 				grid.getSelectionModel().select(model, false);
 			}
 		});
 	}
 
 	protected SimpleComboBox<String> initCombo(String title, String[] options, int width) {
 		SimpleComboBox<String> filter = new SimpleComboBox<String>();
 		filter.setTriggerAction(TriggerAction.ALL);
 		filter.setWidth(width);
 		filter.setEmptyText(title);
 		filter.setTitle(title);
 		for (String o : options) {
 			filter.add(o);
 		}
 		return filter;
 	}
 	
 	protected CellEditor initCombo(String[] options) {
 	    final SimpleComboBox<String> combo = new SimpleComboBox<String>();
 	    combo.setForceSelection(true);
 	    combo.setTriggerAction(TriggerAction.ALL);
 	    for (String o : options) {
 	    	combo.add(o);
 	    }
 
 	    CellEditor editor = new CellEditor(combo) {
 	      @Override
 	      public Object preProcessValue(Object value) {
 	        if (value == null) {
 	          return value;
 	        }
 	        return combo.findModel(value.toString());
 	      }
 
 	      @Override
 	      public Object postProcessValue(Object value) {
 	        if (value == null) {
 	          return value;
 	        }
 	        return ((ModelData) value).get("value");
 	      }
 	    };
 	    return editor;
 	}
 	
 	public DynamicHttpProxy<BasePagingLoadResult<BaseModelData>> getProxy() {
 		return proxy;
 	}
 	
 	public String getRootURL() {
 		return rootURL;
 	}
 	
 	public void setRootURL(String rurl) {
 		rootURL = rurl;
 		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, rootURL);
 		proxy.setBuilder(builder);
 	}
 	
 	public void setPageSize(int pageSize) {
 		this.pageSize = pageSize;
 	}
 
 	public int getPageSize() {
 		return pageSize;
 	}
 	
 	public Button getColumnsItem() {
 		return columnsItem;
 	}
 	
 	public SplitButton getFilterAction() {
 		return filterAction;
 	}
 	
 	public FilterMenu getFilterMenu() {
 		return filterMenu;
 	}
 
 	public List<SimpleComboBox<String>> getAdvancedFilters() {
 		return advancedFilters;
 	}
 	
 	public void setCommitState(boolean commitState) {
 		this.commitState = commitState;
 	}
 
 	public boolean isCommitState() {
 		return commitState;
 	}
 	
 	public void setShowColumnsMenu(boolean state) {
 		showColumnsMenu = state;
 	}
 
 	/** Provides basic spreadsheet-like functionality. */
 	protected EditorGrid<BaseModelData> grid;
 
 	/** Use store.add() to remember dynamically created records. */
 	protected ListStore<BaseModelData> store;
 	
 	/** Flag for enforcing saves only on Save button press. **/
 	private boolean commitState;
 	private int pageSize = 50;
 	private ModelType modelType;	
 	private ColumnConfigForm columnConfForm;
 	private FilterComboForm filterComboForm;
 	private Button columnsItem;
 	private boolean showColumnsMenu = true;
 	private FilterMenu filterMenu;
 	public List<String> filterComboIds = new ArrayList<String>();
 	public List<String> columnConfigIds = new ArrayList<String>();
 
 	protected List<CheckColumnConfig> checkBoxes = new ArrayList<CheckColumnConfig>();
 	
 	/** Use loader.load() to refresh with the list of records on the server. */
 	protected PagingLoader<BasePagingLoadResult<BaseModelData>> loader;
 	
 	protected DynamicHttpProxy<BasePagingLoadResult<BaseModelData>> proxy;
 	
 	protected String rootURL;
 	
 	protected List<SimpleComboBox<String>> advancedFilters = new ArrayList<SimpleComboBox<String>>();
 	
 	protected SplitButton filterAction;
 	protected Button saveItem;
 	protected Button viewItem;
 	protected Button addItem;
 	protected Button removeItem;
 	protected Button removeApproval;
 	protected Dialog removeDialog;
 	protected Button actionItem;
 	protected ToolBar toolBar;
 	protected PagingToolBar pagingToolBar;
 	
 	protected FilterItem filter;
 	
 	protected String[] trimesters = new String[] {
 			// TBF - need to generate this list relative to the current date
 		      "10C", "10B", "10A"
 		    , "09C", "09B", "09A"
             , "08C", "08B", "08A"
             , "07C", "07B", "07A"
             , "06C", "06B", "06A"
             , "05C", "05B", "05A"
             , "04A"
             };
 	
 	protected String defaultDate;
 }
