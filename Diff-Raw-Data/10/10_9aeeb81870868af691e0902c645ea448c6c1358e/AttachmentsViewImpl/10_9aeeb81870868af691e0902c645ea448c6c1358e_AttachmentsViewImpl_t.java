 package org.sagebionetworks.web.client.widget.entity;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.sagebionetworks.repo.model.attachment.AttachmentData;
 import org.sagebionetworks.web.client.DisplayConstants;
 import org.sagebionetworks.web.client.DisplayUtils;
 import org.sagebionetworks.web.client.IconsImageBundle;
 
 import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
 import com.extjs.gxt.ui.client.data.BaseModelData;
 import com.extjs.gxt.ui.client.event.ButtonEvent;
 import com.extjs.gxt.ui.client.event.Listener;
 import com.extjs.gxt.ui.client.event.MenuEvent;
 import com.extjs.gxt.ui.client.event.MessageBoxEvent;
 import com.extjs.gxt.ui.client.event.SelectionListener;
 import com.extjs.gxt.ui.client.store.ListStore;
 import com.extjs.gxt.ui.client.widget.Dialog;
 import com.extjs.gxt.ui.client.widget.HorizontalPanel;
 import com.extjs.gxt.ui.client.widget.Html;
 import com.extjs.gxt.ui.client.widget.LayoutContainer;
 import com.extjs.gxt.ui.client.widget.MessageBox;
 import com.extjs.gxt.ui.client.widget.button.Button;
 import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
 import com.extjs.gxt.ui.client.widget.grid.ColumnData;
 import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
 import com.extjs.gxt.ui.client.widget.grid.Grid;
 import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
 import com.extjs.gxt.ui.client.widget.layout.TableData;
 import com.extjs.gxt.ui.client.widget.menu.Menu;
 import com.extjs.gxt.ui.client.widget.menu.MenuItem;
 import com.google.gwt.safehtml.shared.SafeHtml;
 import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
 import com.google.gwt.safehtml.shared.SafeHtmlUtils;
 import com.google.gwt.user.client.Element;
 import com.google.gwt.user.client.ui.AbstractImagePrototype;
 import com.google.gwt.user.client.ui.FlowPanel;
 import com.google.gwt.user.client.ui.Widget;
 import com.google.inject.Inject;
 
 public class AttachmentsViewImpl extends LayoutContainer implements AttachmentsView {
 	
	private static final String ATTACHMENT_COLUMN_WIDTH = "210px";
 	private static final String LINK_KEY = "link";
 	private static final String ATTACHMENT_DATA_TOKEN_KEY = "attachmentDataKey";
 	private static final String TOOLTIP_TEXT_KEY = "tooltip";
 	private static final String ATTACHMENT_DATA_NAME_KEY = "attachmentDataName";
 
 	Grid<BaseModelData> grid;
 	ListStore<BaseModelData> gridStore;
 	ColumnModel columnModel;
 	private Presenter presenter;
 	private IconsImageBundle iconsImageBundle;
 	private boolean isEmpty = true;
 	
 	@Inject
 	public AttachmentsViewImpl(IconsImageBundle iconsImageBundle) {
 		this.iconsImageBundle = iconsImageBundle;
 		
 		gridStore = new ListStore<BaseModelData>();
 	}
 	
 	@Override
 	protected void onRender(Element parent, int index) {
 		super.onRender(parent, index);
 		this.setBorders(true);
 
 	    List<ColumnConfig> configs = new ArrayList<ColumnConfig>();  
 	    GridCellRenderer<BaseModelData> valueRenderer = createValueRenderer();
 	    
 	    ColumnConfig column = new ColumnConfig();  
 	    column.setId(LINK_KEY);  
 	    column.setHeader("Attachment");  	    	   
 	    column.setRowHeader(false);
 	    column.setWidth(259);
 		column.setRenderer(valueRenderer);
 	    configs.add(column);  
 	  	  	     	 
 	    columnModel = new ColumnModel(configs);  
 	  
 	    grid = new Grid<BaseModelData>(gridStore, columnModel);  
 	    grid.setStyleAttribute("borderTop", "none");  
 	    grid.setAutoExpandColumn(LINK_KEY);  
 		grid.setAutoExpandMin(100);
 		grid.setAutoExpandMax(300);
 		// This is important, the grid must resize to fit its height.
 		grid.setAutoHeight(true);
 		grid.setAutoWidth(false);
 		grid.setBorders(true);
 		grid.setStripeRows(false);
 		grid.setColumnLines(false);
 		grid.setColumnReordering(false);
 		grid.setHideHeaders(true);
 		grid.setTrackMouseOver(false);
 		grid.setShadow(false);		
 		
 		//configureContextMenu();
 		this.add(grid);
 	}
 	
 	@Override
 	public void configure(String baseUrl, String entityId,
 			List<AttachmentData> attachments) {		
 		gridStore.removeAll();
		if(attachments == null || attachments.size() == 0){
 			addNoAttachmentRow();
 		} else {
 			populateStore(baseUrl, entityId, attachments);			
 		}
 		//configureContextMenu();
 		
 		if(isRendered())
 			grid.reconfigure(gridStore, columnModel);
 		this.layout(true);		
 	}
 
 	private void addNoAttachmentRow() {
 		isEmpty = true;
 		BaseModelData model = new BaseModelData();
 		model.set(LINK_KEY, DisplayConstants.TEXT_NO_ATTACHMENTS);
 		model.set(TOOLTIP_TEXT_KEY, null);
 		gridStore.add(model);		
 	}
 
 	
 	private void populateStore(String baseUrl, String entityId,
 			List<AttachmentData> attachments) {		
 		 
 		for(AttachmentData data: attachments){
 			SafeHtml dataName = SafeHtmlUtils.fromString(data.getName());
 			
 			SafeHtmlBuilder builder = new SafeHtmlBuilder();
 			builder.appendHtmlConstant("<a href=\"" 
 					+ DisplayUtils.createAttachmentUrl(baseUrl, entityId, data.getTokenId(), data.getTokenId()) 
 					+ "\" target=\"_blank\" name=\"" 
 					+ dataName.asString() 
 					+ "\">");
 			SafeHtml iconNumber = SafeHtmlUtils.fromSafeConstant(DisplayUtils.getAttachmentIcon(data.getName()));
 			builder.appendHtmlConstant("<div class=\"icon-white-small icon"+iconNumber.asString()+"-white\">");
 			builder.appendHtmlConstant("<div style=\"margin-left:20px\">");
 			builder.appendEscaped(DisplayUtils.replaceWhiteSpace(data.getName()));
 			builder.appendHtmlConstant("</div>");
 			builder.appendHtmlConstant("</div>");
 			builder.appendHtmlConstant("</a>");
 			Html listItem = new Html(builder.toSafeHtml().asString());
 		    // If we have a preview then show it as a tooltip.
 			SafeHtml previewToolip;
 		    if(data.getPreviewId() != null){
 		    	previewToolip = SafeHtmlUtils.fromSafeConstant("<div class=\"preview-image-loading\" >"
 			    		+ DisplayUtils.IMAGE_CENTERING_TABLE_START
 			    		+ "<img style=\"margin:auto; display:block;\" src=\"" 
 			    		+ DisplayUtils.createAttachmentUrl(baseUrl, entityId, data.getPreviewId(), null) + "\"/>"
 			    		+ DisplayUtils.IMAGE_CENTERING_TABLE_END
 			    		+ "</div>");  
 		    }else{
 			    previewToolip = dataName;			    
 		    }		    
 
 		    BaseModelData model = new BaseModelData();
 			model.set(LINK_KEY, listItem.getHtml());
 			model.set(ATTACHMENT_DATA_TOKEN_KEY, data.getTokenId());
 			model.set(ATTACHMENT_DATA_NAME_KEY, SafeHtmlUtils.fromString(data.getName()).asString());
 			model.set(TOOLTIP_TEXT_KEY, previewToolip.asString());
 			gridStore.add(model);
 		}
 		
 		if(gridStore.getCount() > 0) {
 			isEmpty = false;
 		}
 	}
 
 	public GridCellRenderer<BaseModelData> createValueRenderer() {
 		GridCellRenderer<BaseModelData> valueRenderer = new GridCellRenderer<BaseModelData>() {
 
 			@Override
 			public Object render(BaseModelData model, String property,
 					ColumnData config, final int rowIndex, int colIndex,
 					ListStore<BaseModelData> store, Grid<BaseModelData> grid) {
 				String value = model.get(property);
 				if (value == null) {
 					value = "";
 				}
 				StringBuilder builder = new StringBuilder();
 				builder.append("<div style='font-weight: normal;color:black; overflow:hidden; text-overflow:ellipsis; width:auto;'>");
 				builder.append(value);
 				builder.append("</div>");
 				// TODO : determine if value should be SafeHtml escaped
 				Html html = new Html(builder.toString());
 				String tooltip = (String)model.get(TOOLTIP_TEXT_KEY);
 				if(tooltip != null) {
 				    html.setToolTip(tooltip);
 				}
 
				if (model.get(TOOLTIP_TEXT_KEY) == null) {
					return html;
				}

 				Button button = new Button();
 				SelectionListener<ButtonEvent> listener;
 
 				AbstractImagePrototype img = AbstractImagePrototype.create(iconsImageBundle
 						.deleteButton16());
 
 				listener = new SelectionListener<ButtonEvent>() {
 
 					@Override
 					public void componentSelected(ButtonEvent ce) {
 						deleteAttachmentAt(rowIndex);
 					}
 				};
 				button.addSelectionListener(listener);
 				button.setIcon(img);
 				HorizontalPanel panel = new HorizontalPanel();
 				FlowPanel wrap = new FlowPanel();
 				wrap.add(html);
 				wrap.setWidth(ATTACHMENT_COLUMN_WIDTH);
 				panel.add(wrap);
 
 				TableData td = new TableData();
 				td.setHorizontalAlign(HorizontalAlignment.RIGHT);
 				panel.add(button, td);
 				panel.setAutoWidth(true);
 				return panel;
 			}
 
 		};
 		return valueRenderer;
 	}
 
 	@Override
 	public Widget asWidget() {
 		if(isRendered()) {
 			grid.reconfigure(gridStore, columnModel);
 			this.layout(true);
 		}
 		return this;
 	}
 
 	@Override
 	public void setPresenter(Presenter presenter) {
 		this.presenter = presenter;
 	}
 
 	@Override
 	public void showLoading() {
 		// TODO Auto-generated method stub
 		
 	}
 
 	@Override
 	public void clear() {
 		// TODO Auto-generated method stub
 		
 	}
 
 	@Override
 	public void showInfo(String title, String message) {
 		DisplayUtils.showInfo(title, message);
 	}
 
 	@Override
 	public void showErrorMessage(String message) {
 		DisplayUtils.showErrorMessage(message);
 	}
 
 	
 	/*
 	 * Private Methods
 	 */
 	private void configureContextMenu() {
 		if(grid == null) return;
 		
 		if(!isEmpty) {
 			Menu contextMenu = new Menu();
 	
 			MenuItem remove = new MenuItem();
 			remove.setText(DisplayConstants.LABEL_DELETE);
 			remove.setIcon(AbstractImagePrototype.create(iconsImageBundle.deleteButton16()));
 			remove.addSelectionListener(new SelectionListener<MenuEvent>() {
 				public void componentSelected(MenuEvent ce) {
 					final BaseModelData model = grid.getSelectionModel().getSelectedItem();
 					if (model != null) {
 						MessageBox.confirm(DisplayConstants.LABEL_DELETE +" " + model.get(ATTACHMENT_DATA_NAME_KEY), DisplayConstants.PROMPT_SURE_DELETE + " " + model.get(ATTACHMENT_DATA_NAME_KEY) +"?", new Listener<MessageBoxEvent>() {					
 							@Override
 							public void handleEvent(MessageBoxEvent be) { 												
 								Button btn = be.getButtonClicked();
 								if(Dialog.YES.equals(btn.getItemId())) {
 									presenter.deleteAttachment((String)model.get(ATTACHMENT_DATA_TOKEN_KEY));								
 								}
 							}
 						});					
 					}
 				}
 			});
 			contextMenu.add(remove);
 	
 			grid.setContextMenu(contextMenu);
 		} else {
 			grid.setContextMenu(null);
 		}
 	}
 
 	public void deleteAttachmentAt(int rowIndex) {
 		final BaseModelData model = grid.getStore().getAt(rowIndex);
 		if (model != null) {
 			MessageBox.confirm(
 					DisplayConstants.LABEL_DELETE + " "
 							+ model.get(ATTACHMENT_DATA_NAME_KEY),
 					DisplayConstants.PROMPT_SURE_DELETE + " "
 							+ model.get(ATTACHMENT_DATA_NAME_KEY) + "?",
 					new Listener<MessageBoxEvent>() {
 						@Override
 						public void handleEvent(MessageBoxEvent be) {
 							Button btn = be.getButtonClicked();
 							if (Dialog.YES.equals(btn.getItemId())) {
 								presenter.deleteAttachment((String) model
 										.get(ATTACHMENT_DATA_TOKEN_KEY));
 							}
 						}
 					});
 		}
 	}
 
 	@Override
 	public void attachmentDeleted(String tokenId, String deletedName) {
 		if(isRendered() && tokenId != null && grid != null && gridStore != null) {
 			//BaseModelData
 			Integer foundIdx = null;			
 			for(int i=0; i<gridStore.getCount(); i++) {
 				BaseModelData model = gridStore.getAt(i);
 				if(tokenId.equals(model.get(ATTACHMENT_DATA_TOKEN_KEY)))
 					foundIdx = i;
 			}
 			if(foundIdx != null) {
 				gridStore.remove(foundIdx);
 				if(gridStore.getCount() == 0) {
 					configureContextMenu();
 					addNoAttachmentRow();
 				}
 				grid.reconfigure(gridStore, columnModel);
 			}
 		}
 		showInfo(deletedName + " " + DisplayConstants.LABEL_DELETED + ".", "");
 	}
 	
 }
