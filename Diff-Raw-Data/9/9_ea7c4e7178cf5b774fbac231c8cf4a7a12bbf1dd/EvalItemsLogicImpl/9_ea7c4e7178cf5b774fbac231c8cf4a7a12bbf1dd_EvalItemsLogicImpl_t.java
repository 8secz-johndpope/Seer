 /******************************************************************************
  * EvalItemsLogicImpl.java - created by aaronz@vt.edu on Jan 2, 2007
  * 
  * Copyright (c) 2007 Virginia Polytechnic Institute and State University
  * Licensed under the Educational Community License version 1.0
  * 
  * A copy of the Educational Community License has been included in this 
  * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
  * 
  * Contributors:
  * Aaron Zeckoski (aaronz@vt.edu) - primary
  * 
  *****************************************************************************/
 
 package org.sakaiproject.evaluation.logic.impl;
 
 import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.Date;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.sakaiproject.evaluation.dao.EvaluationDao;
 import org.sakaiproject.evaluation.logic.EvalExternalLogic;
 import org.sakaiproject.evaluation.logic.EvalItemsLogic;
 import org.sakaiproject.evaluation.logic.EvalSettings;
 import org.sakaiproject.evaluation.logic.utils.ArrayUtils;
 import org.sakaiproject.evaluation.logic.utils.EvalUtils;
 import org.sakaiproject.evaluation.model.EvalItem;
 import org.sakaiproject.evaluation.model.EvalScale;
 import org.sakaiproject.evaluation.model.EvalTemplate;
 import org.sakaiproject.evaluation.model.EvalTemplateItem;
 import org.sakaiproject.evaluation.model.constant.EvalConstants;
 import org.sakaiproject.genericdao.api.finders.ByPropsFinder;
 
 
 /**
  * Implementation for EvalItemsLogic
  *
  * @author Aaron Zeckoski (aaronz@vt.edu)
  */
 public class EvalItemsLogicImpl implements EvalItemsLogic {
 
 	private static Log log = LogFactory.getLog(EvalItemsLogicImpl.class);
 
 	private EvaluationDao dao;
 	public void setDao(EvaluationDao dao) {
 		this.dao = dao;
 	}
 
 	private EvalExternalLogic external;
 	public void setExternalLogic(EvalExternalLogic external) {
 		this.external = external;
 	}
 
 	private EvalSettings settings;
 	public void setSettings(EvalSettings settings) {
 		this.settings = settings;
 	}
 
 
 	// INIT method
 	public void init() {
 		log.debug("Init");
 	}
 
 
 
 	/* (non-Javadoc)
 	 * @see org.sakaiproject.evaluation.logic.EvalItemsLogic#getItemById(java.lang.Long)
 	 */
 	public EvalItem getItemById(Long itemId) {
 		log.debug("itemId:" + itemId);
 		return (EvalItem) dao.findById(EvalItem.class, itemId);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.sakaiproject.evaluation.logic.EvalItemsLogic#saveItem(org.sakaiproject.evaluation.model.EvalItem, java.lang.String)
 	 */
 	public void saveItem(EvalItem item, String userId) {
 		log.debug("item:" + item.getId() + ", userId:" + userId);
 
 		// set the date modified
 		item.setLastModified( new Date() );
 
 		// check on ITEM_TYPE and invalid combinations of item values depending on the type
 		if ( EvalConstants.ITEM_TYPE_SCALED.equals(item.getClassification()) ) {
 			if (item.getScale() == null) {
 				throw new IllegalArgumentException("Item scale must be specified for scaled type items");
 			} else if (item.getScaleDisplaySetting() == null) {
 				throw new IllegalArgumentException("Item scale display setting must be specified for scaled type items");
 			} else if (item.getDisplayRows() != null) {
 				throw new IllegalArgumentException("Item displayRows cannot be included for scaled type items");
 			}
 		} else if ( EvalConstants.ITEM_TYPE_TEXT.equals(item.getClassification()) ) {
 			if (item.getDisplayRows() == null) {
 				throw new IllegalArgumentException("Item display rows must be specified for text type items");
 			} else if (item.getScale() != null) {
 				throw new IllegalArgumentException("Item scale cannot be included for text type items");
 			} else if (item.getScaleDisplaySetting() != null) {
 				throw new IllegalArgumentException("Item scale display setting cannot be included for text type items");
 			}
 		} else if ( EvalConstants.ITEM_TYPE_HEADER.equals(item.getClassification()) ) {
 			if (item.getScale() != null) {
 				throw new IllegalArgumentException("Item scale cannot be included for header type items");
 			} else if (item.getScaleDisplaySetting() != null) {
 				throw new IllegalArgumentException("Item scale display setting cannot be included for header type items");
 			} else if (item.getDisplayRows() != null) {
 				throw new IllegalArgumentException("Item displayRows cannot be included for header type items");
 			}
 		} else if ( EvalConstants.ITEM_TYPE_BLOCK_PARENT.equals(item.getClassification()) ) {
 			if (item.getScale() == null) {
 				throw new IllegalArgumentException("Item scale must be specified for block parent type items");
 			} else if (item.getScaleDisplaySetting() == null) {
 				throw new IllegalArgumentException("Item scale display setting must be specified for block parent type items");
 			} else if (item.getDisplayRows() != null) {
 				throw new IllegalArgumentException("Item displayRows cannot be included for block parent type items");
 			}
 		} else {
 			throw new IllegalArgumentException("Invalid item classification specified ("+item.getClassification()+"), you must use the ITEM_TYPE constants to indicate classification (and cannot use BLOCK_CHILD)");
 		}
 
 		// check the sharing constants
 		if (! EvalUtils.checkSharingConstant(item.getSharing()) ||
 				EvalConstants.SHARING_OWNER.equals(item.getSharing()) ) {
 			throw new IllegalArgumentException("Invalid sharing constant ("+item.getSharing()+") set for item ("+item.getItemText()+")");
 		} else if ( EvalConstants.SHARING_PUBLIC.equals(item.getSharing()) ) {
 			// test if non-admin trying to set public sharing
 			if (! external.isUserAdmin(userId) ) {
 				throw new IllegalArgumentException("Only admins can set item ("+item.getItemText()+") sharing to public");
 			}
 		}
 
 		if (item.getId() != null) {
 			// existing item, don't allow change to locked setting
 			EvalItem existingItem = (EvalItem) dao.findById(EvalItem.class, item.getId());
 			if (existingItem == null) {
 				throw new IllegalArgumentException("Cannot find item with id: " + item.getId());
 			}
 
 			if (! existingItem.getLocked().equals(item.getLocked())) {
 				throw new IllegalArgumentException("Cannot change locked setting on existing item (" + item.getId() + ")");
 			}
 		}
 
 		// fill in the default settings for optional unspecified values
 		if (item.getLocked() == null) {
 			item.setLocked( Boolean.FALSE );
 		}
 		// check the NOT_AVAILABLE_ALLOWED system setting
 		Boolean naAllowed = (Boolean)settings.get(EvalSettings.NOT_AVAILABLE_ALLOWED);
 		if (naAllowed.booleanValue()) {
 			// can set NA
 			if (item.getUsesNA() == null) {
 				item.setUsesNA( Boolean.FALSE );
 			}
 		} else {
 			item.setUsesNA( Boolean.FALSE );
 		}
 		if (item.getCategory() == null) {
 			item.setCategory( EvalConstants.ITEM_CATEGORY_COURSE );
 		}
 
 		if (checkUserControlItem(userId, item)) {
 			dao.save(item);
 			log.info("User ("+userId+") saved item ("+item.getId()+"), title: " + item.getItemText());
 
 			if (item.getLocked().booleanValue() == true && item.getScale() != null) {
 				// lock associated scale
 				log.info("Locking scale ("+item.getScale().getTitle()+") associated with new item ("+item.getId()+")");
 				dao.lockScale( item.getScale(), Boolean.FALSE );
 			}
 
 			return;
 		}
 
 		// should not get here so die if we do
 		throw new RuntimeException("User ("+userId+") could NOT save item ("+item.getId()+"), title: " + item.getItemText());
 	}
 
 	/* (non-Javadoc)
 	 * @see org.sakaiproject.evaluation.logic.EvalItemsLogic#deleteItem(java.lang.Long, java.lang.String)
 	 */
 	public void deleteItem(Long itemId, String userId) {
 		log.debug("itemId:" + itemId + ", userId:" + userId);
 
 		// get the item by id
 		EvalItem item = (EvalItem) dao.findById(EvalItem.class, itemId);
 		if (item == null) {
 			throw new IllegalArgumentException("Cannot find item with id: " + itemId);
 		}
 
 		// ADMIN USER CAN REMOVE EXPERT ITEMS -AZ
 //		// cannot remove expert items
 //		if (item.getExpert().booleanValue() == true) {
 //			throw new IllegalStateException("Cannot remove expert item ("+itemId+")");
 //		}
 
 		if (checkUserControlItem(userId, item)) {
 			EvalScale scale = item.getScale();
 			dao.delete(item);
 			log.info("User ("+userId+") removed item ("+item.getId()+"), title: " + item.getItemText());
 
 			if (item.getLocked().booleanValue() && scale != null) {
 				// unlock associated scales
 				log.info("Unlocking associated scale ("+scale.getTitle()+") for removed item ("+itemId+")");
 				dao.lockScale( scale, Boolean.FALSE );
 			}
 
 			return;
 		}
 		
 		// should not get here so die if we do
 		throw new RuntimeException("User ("+userId+") could NOT delete item ("+item.getId()+")");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.sakaiproject.evaluation.logic.EvalItemsLogic#getItemsForUser(java.lang.String, java.lang.String, java.lang.String, boolean)
 	 */
 	public List getItemsForUser(String userId, String sharingConstant, String filter, boolean includeExpert) {
 		log.debug("sharingConstant:" + sharingConstant + ", userId:" + userId + ", filter:" + filter  + ", includeExpert:" + includeExpert);
 
 		List l = new ArrayList();
 
 		// get admin state
 		boolean isAdmin = external.isUserAdmin(userId);
 
 		boolean getPublic = false;
 		boolean getPrivate = false;
 		// check the sharingConstant param
 		if (sharingConstant != null && 
 				! EvalUtils.checkSharingConstant(sharingConstant)) {
 			throw new IllegalArgumentException("Invalid sharing constant: " + sharingConstant);
 		}
 		if ( sharingConstant == null || 
 				EvalConstants.SHARING_OWNER.equals(sharingConstant) ) {
 			// return all items visible to this user
 			getPublic = true;
 			getPrivate = true;
 		} else if ( EvalConstants.SHARING_PRIVATE.equals(sharingConstant) ) {
 			// return only private items visible to this user
 			getPrivate = true;
 		} else if ( EvalConstants.SHARING_PUBLIC.equals(sharingConstant) ) {
 			// return all public items
 			getPublic = true;
 		}
 
 		// leave out the block parent items
 		String[] props = new String[] { "classification" };
 		Object[] values = new Object[] { EvalConstants.ITEM_TYPE_BLOCK_PARENT };
 		int[] comparisons = new int[] { ByPropsFinder.NOT_EQUALS };
 
 		if (!includeExpert) {
 			props = ArrayUtils.appendArray(props, "expert");
 			values = ArrayUtils.appendArray(values, Boolean.TRUE);
 			comparisons = ArrayUtils.appendArray(comparisons, ByPropsFinder.NOT_EQUALS);
 		}
 
 		if (filter != null && filter.length() > 0) {
 			props = ArrayUtils.appendArray(props, "itemText");
 			values = ArrayUtils.appendArray(values, "%" + filter + "%");
 			comparisons = ArrayUtils.appendArray(comparisons, ByPropsFinder.LIKE);
 		}
 
 		// handle private sharing items
 		if (getPrivate) {
 			String[] privateProps = ArrayUtils.appendArray(props, "sharing");
 			Object[] privateValues = ArrayUtils.appendArray(values, EvalConstants.SHARING_PRIVATE);
 			int[] privateComparisons = ArrayUtils.appendArray(comparisons, ByPropsFinder.EQUALS);
 
 			if (!isAdmin) {
 				privateProps = ArrayUtils.appendArray(privateProps, "owner");
 				privateValues = ArrayUtils.appendArray(privateValues, userId);
 				privateComparisons = ArrayUtils.appendArray(privateComparisons, ByPropsFinder.EQUALS);
 			}
 
 			l.addAll( dao.findByProperties(EvalItem.class, privateProps, privateValues, privateComparisons, new String[] {"id"}) );
 		}
 
 		// handle public sharing items
 		if (getPublic) {
 			String[] publicProps = ArrayUtils.appendArray(props, "sharing");
 			Object[] publicValues = ArrayUtils.appendArray(values, EvalConstants.SHARING_PUBLIC);
 			int[] publicComparisons = ArrayUtils.appendArray(comparisons, ByPropsFinder.EQUALS);
 
 			l.addAll( dao.findByProperties(EvalItem.class, publicProps, publicValues, publicComparisons, new String[] {"id"}) );
 		}
 
 		return EvalUtils.removeDuplicates(l); // remove duplicates from the list
 	}
 
 	/* (non-Javadoc)
 	 * @see org.sakaiproject.evaluation.logic.EvalItemsLogic#getItemsForTemplate(java.lang.Long, java.lang.String)
 	 */
 	public List getItemsForTemplate(Long templateId, String userId) {
 		log.debug("templateId:" + templateId + ", userId:" + userId);
 
 		List l = new ArrayList();
 		for (Iterator iter = getTemplateItemsForTemplate(templateId, userId, null).iterator(); iter.hasNext();) {
 			EvalTemplateItem eti = (EvalTemplateItem) iter.next();
 			l.add(eti.getItem());
 		}
 		return l;
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.sakaiproject.evaluation.logic.EvalItemsLogic#getTemplateItemById(java.lang.Long)
 	 */
 	public EvalTemplateItem getTemplateItemById(Long templateItemId) {
 		log.debug("templateItemId:" + templateItemId);
 		return (EvalTemplateItem) dao.findById(EvalTemplateItem.class, templateItemId);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.sakaiproject.evaluation.logic.EvalItemsLogic#saveTemplateItem(org.sakaiproject.evaluation.model.EvalTemplateItem, java.lang.String)
 	 */
 	public void saveTemplateItem(EvalTemplateItem templateItem, String userId) {
 		log.debug("templateItem:" + templateItem.getId() + ", userId:" + userId);
 
 		// set the date modified
 		templateItem.setLastModified( new Date() );
 
 		// get item and check it
 		EvalItem item = templateItem.getItem();
 		if (item == null) {
 			throw new IllegalArgumentException("Item cannot be null");
 		} else if (item.getId() == null) {
 			throw new IllegalArgumentException("Item ("+item.getItemText()+") must already be saved");
 		}
 
 		// check on ITEM_TYPE and invalid combinations of item values depending on the type
 		// inherit settings from item if not set correctly here
 		if ( EvalConstants.ITEM_TYPE_SCALED.equals(item.getClassification()) ) {
 			// check if this scaled item is a block item
 			if (templateItem.getBlockParent() == null) {
 				// not block item (block parent must be specified)
 				// general scaled items checks
 				if (templateItem.getScaleDisplaySetting() == null) {
 					if (item.getScaleDisplaySetting() == null) {
 						throw new IllegalArgumentException("Item scale display setting must be specified for scaled type items");
 					} else {
 						templateItem.setScaleDisplaySetting(item.getScaleDisplaySetting());
 					}
 				} else if (templateItem.getBlockId() != null) {
 					throw new IllegalArgumentException("Item blockid must be null for scaled type items");
 				} else if (templateItem.getDisplayRows() != null) {
 					throw new IllegalArgumentException("Item displayRows must be null for scaled type items");
 				}
 			} else {
 				// this is related to a block
 				if ( templateItem.getBlockParent() != null ) {
 					// this is a child block item
 					if (templateItem.getBlockId() == null) {
 						throw new IllegalArgumentException("Item blockid must be specified for child block items");
 					} else if (templateItem.getDisplayRows() != null) {
 						throw new IllegalArgumentException("Item displayRows must be null for block type items");
 					}
 				}
 
 			}
 		} else if ( EvalConstants.ITEM_TYPE_BLOCK_PARENT.equals(item.getClassification()) ) {
 			// this is a block parent item (created just to hold the block parent text)
 			if (templateItem.getBlockParent() == null || !templateItem.getBlockParent().booleanValue() ) {
 				throw new IllegalArgumentException("Template Item block parent must be TRUE for parent block item");
 			} else if (templateItem.getScaleDisplaySetting() == null) {
 				throw new IllegalArgumentException("Template Item scale display setting must be included for parent block item");
 			} else if (templateItem.getBlockId() != null) {
 				throw new IllegalArgumentException("Item blockid must be null for parent block item");
 			} else if (templateItem.getDisplayRows() != null) {
 				throw new IllegalArgumentException("Item displayRows must be null for block type items");
 			}
 		} else if ( EvalConstants.ITEM_TYPE_TEXT.equals(item.getClassification()) ) {
 			if (templateItem.getDisplayRows() == null) {
 				if (item.getDisplayRows() == null) {
 					throw new IllegalArgumentException("Item display rows must be specified for text type items");
 				} else {
 					templateItem.setDisplayRows(item.getDisplayRows());
 				}
 			} else if (templateItem.getBlockId() != null) {
 				throw new IllegalArgumentException("Item blockid cannot be included for text type items");
 			} else if (templateItem.getScaleDisplaySetting() != null) {
 				throw new IllegalArgumentException("Item scale display setting cannot be included for text type items");
 			} else if (templateItem.getBlockParent() != null) {
 				throw new IllegalArgumentException("Item blockParent must be null for text type items");
 			}
 		} else if ( EvalConstants.ITEM_TYPE_HEADER.equals(item.getClassification()) ) {
 			if (templateItem.getBlockId() != null) {
 				throw new IllegalArgumentException("Item blockid cannot be included for header type items");
 			} else if (templateItem.getScaleDisplaySetting() != null) {
 				throw new IllegalArgumentException("Item scale display setting cannot be included for header type items");
 			} else if (templateItem.getDisplayRows() != null) {
 				throw new IllegalArgumentException("Item displayRows cannot be included for header type items");
 			} else if (templateItem.getBlockParent() != null) {
 				throw new IllegalArgumentException("Item blockParent must be null for header type items");
 			}
 		} else {
 			throw new IllegalArgumentException("Invalid item classification specified ("+item.getClassification()+"), you must use the ITEM_TYPE constants to indicate classification (and cannot use BLOCK)");
 		}
 
 		// get template and check it
 		EvalTemplate template = templateItem.getTemplate();
 		if (template == null) {
 			throw new IllegalArgumentException("Template cannot be null");
 		} else if (template.getId() == null) {
 			throw new IllegalArgumentException("Template ("+template.getTitle()+") must already be saved");
 		}
 
 		// check the template lock state and do not allow saves when template is locked
 		if (template.getLocked().booleanValue()) {
 			throw new IllegalStateException("This template ("+template.getId()+") is locked, templateItems and items cannot be changed");
 		}
 
 		// get the template items count to set display order for new templateItems
 		if (templateItem.getId() == null) {
 			if (templateItem.getBlockParent() != null &&
 					templateItem.getBlockParent().booleanValue() &&
 					templateItem.getDisplayOrder() != null) {
 				// if this a block parent then we allow the display order to be set
 			} else {
 				// new item
 				int itemsCount = 0;
 				if (template.getTemplateItems() != null) {
 					// TODO - write a DAO method to do this faster
 					for (Iterator iter = template.getTemplateItems().iterator(); iter.hasNext();) {
 						EvalTemplateItem eti = (EvalTemplateItem) iter.next();
 						if (eti.getBlockId() == null) {
 							// only count items which are not children of a block
 							itemsCount++;
 						}
 					}
 					//itemsCount = template.getTemplateItems().size();
 				}
 				templateItem.setDisplayOrder( new Integer(itemsCount + 1) );
 			}
 		} else {
 			// existing item
 			// TODO - check if the display orders are set to a value that is used already?
 		}
 
 		// set the default values for unspecified optional values
 		if (templateItem.getItemCategory() == null) {
 			if (item.getCategory() == null) {
 				templateItem.setItemCategory(EvalConstants.ITEM_CATEGORY_COURSE);
 			} else {
 				templateItem.setItemCategory(item.getCategory());
 			}
 		}
 		Boolean naAllowed = (Boolean)settings.get(EvalSettings.NOT_AVAILABLE_ALLOWED);
 		if (naAllowed.booleanValue()) {
 			// can set NA
 			if (templateItem.getUsesNA() == null) {
 				templateItem.setUsesNA( Boolean.FALSE );
 			}
 		} else {
 			templateItem.setUsesNA( Boolean.FALSE );
 		}
 		// defaults for hierarchy level of template items
 		if (templateItem.getHierarchyLevel() == null) {
 			templateItem.setHierarchyLevel(EvalConstants.HIERARCHY_LEVEL_TOP);
 		}
 		if (templateItem.getHierarchyNodeId() == null) {
 			templateItem.setHierarchyNodeId(EvalConstants.HIERARCHY_NODE_ID_TOP);
 		}
 
 		if (checkUserControlTemplateItem(userId, templateItem)) {
 
 			if (templateItem.getId() == null) {
 				// if this is a new templateItem then associate it with 
 				// the existing item and template and save all together
 				Set[] entitySets = new HashSet[3];
 
 				Set tiSet = new HashSet();
 				tiSet.add(templateItem);
 				entitySets[0] = tiSet;
 
 				if (item.getTemplateItems() == null) {
 					item.setTemplateItems( new HashSet() );
 				}
 				item.getTemplateItems().add(templateItem);
 				Set itemSet = new HashSet();
 				itemSet.add(item);
 				entitySets[1] = itemSet;
 
 				if (template.getTemplateItems() == null) {
 					template.setTemplateItems( new HashSet() );
 				}
 				template.getTemplateItems().add(templateItem);
 				Set templateSet = new HashSet();
 				templateSet.add(template);
 				entitySets[2] = templateSet;
 
 				dao.saveMixedSet(entitySets);
 			} else {
 				// existing item so just save it
 
 				// TODO - make sure the item and template do not change for existing templateItems
 
 				dao.save(templateItem);
 			}
 
			// Should not be locking this here -AZ
//			// lock related item and associated scales
//			log.info("Locking item ("+item.getId()+") and associated scale");
//			dao.lockItem(item, Boolean.TRUE);

 			log.info("User ("+userId+") saved templateItem ("+templateItem.getId()+"), " +
 					"linked item (" + item.getId() +") and template ("+ template.getId()+")");
 			return;
 		}
 
 		// should not get here so die if we do
 		throw new RuntimeException("User ("+userId+") could NOT save templateItem ("+templateItem.getId()+")");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.sakaiproject.evaluation.logic.EvalItemsLogic#deleteTemplateItem(java.lang.Long, java.lang.String)
 	 */
 	public void deleteTemplateItem(Long templateItemId, String userId) {
 		log.debug("templateItemId:" + templateItemId + ", userId:" + userId);
 
 		// get the templateItem by id
 		EvalTemplateItem templateItem = (EvalTemplateItem) dao.findById(EvalTemplateItem.class, templateItemId);
 		if (templateItem == null) {
 			throw new IllegalArgumentException("Cannot find templateItem with id: " + templateItemId);
 		}
 
 		// check if this templateItem can be removed (checks if associated template is locked)
 		if (checkUserControlTemplateItem(userId, templateItem)) {
 			EvalItem item = templateItem.getItem();
 			// remove the templateItem and update all linkages
 			dao.removeTemplateItems( new EvalTemplateItem[] {templateItem} );
 			// attempt to unlock the related item
 			dao.lockItem(item, Boolean.FALSE);
 			return;
 		}
 		
 		// should not get here so die if we do
 		throw new RuntimeException("User ("+userId+") could NOT delete template-item linkage ("+templateItem.getId()+")");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.sakaiproject.evaluation.logic.EvalItemsLogic#getTemplateItemsForTemplate(java.lang.Long, java.lang.String, java.lang.String)
 	 */
 	public List getTemplateItemsForTemplate(Long templateId, String userId, String hierarchyLevel) {
 		log.debug("templateId:" + templateId + ", userId:" + userId + ", hierarchyLevel:" + hierarchyLevel);
 
 		// check if the template is a valid one
 		EvalTemplate template = (EvalTemplate) dao.findById(EvalTemplate.class, templateId);
 		if (template == null) {
 			throw new IllegalArgumentException("Cannot find template with id: " + templateId);
 		}
 
 		// TODO - make this use the new dao method -AZ
 		//String[] nodeIds = null;
 		//dao.getTemplateItemsByTemplate(templateId, nodeIds, instructorIds, groupIds);
 
 		String[] props = new String[] { "template.id" };
 		Object[] values = new Object[] { templateId };
 		int[] comparisons = new int[] { ByPropsFinder.EQUALS };
 
 		// TODO - check if this user can see this item (must be either taking a related eval or must somehow control the template)
 
 		List l = dao.findByProperties(EvalTemplateItem.class, 
 				props, values, comparisons,
 				new String[] { "displayOrder" } );
 		return l;
 	}
 
 	public List getTemplateItemsForEvaluation(Long evalId, String userId, String hierarchyLevel) {
 		// TODO Auto-generated method stub
 		throw new IllegalStateException("Not implemented yet");
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.sakaiproject.evaluation.logic.EvalItemsLogic#getBlockChildTemplateItemsForBlockParent(java.lang.Long, boolean)
 	 */
 	public List getBlockChildTemplateItemsForBlockParent(Long parentId, boolean includeParent) {
 		
 		// get the templateItem by id to verify parent exists
 		EvalTemplateItem templateItem = (EvalTemplateItem) dao.findById(EvalTemplateItem.class, parentId);
 		if (templateItem == null) {
 			throw new IllegalArgumentException("Cannot find block parent templateItem with id: " + parentId);
 		}
 
 		if (templateItem.getBlockParent() == null ||
 				templateItem.getBlockParent().booleanValue() == false) {
 			throw new IllegalArgumentException("Cannot request child block items for a templateItem which is not a block parent: " + templateItem.getId());
 		}
 
 		List l = new ArrayList();
 		if (includeParent) {
 			l.add(templateItem);
 		}
 
 		l.addAll( dao.findByProperties(EvalTemplateItem.class, 
 				new String[] { "blockId" }, 
 				new Object[] { parentId },
 				new int[] { ByPropsFinder.EQUALS },
 				new String[] { "displayOrder" }) );
 
 		return l;
 	}
 
 
 	/**
 	 * @param userId
 	 * @param itemId
 	 * @return
 	 * @deprecated
 	 */
 	public boolean canControlItem(String userId, Long itemId) {
 		log.debug("itemId:" + itemId + ", userId:" + userId);
 		// get the item by id
 		EvalItem item = (EvalItem) dao.findById(EvalItem.class, itemId);
 		if (item == null) {
 			throw new IllegalArgumentException("Cannot find item with id: " + itemId);
 		}
 
 		// check perms and locked
 		try {
 			return checkUserControlItem(userId, item);
 		} catch (RuntimeException e) {
 			log.info(e.getMessage());
 		}
 		return false;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.sakaiproject.evaluation.logic.EvalItemsLogic#canModifyItem(java.lang.String, java.lang.Long)
 	 */
 	public boolean canModifyItem(String userId, Long itemId) {
 		log.debug("itemId:" + itemId + ", userId:" + userId);
 		// get the item by id
 		EvalItem item = (EvalItem) dao.findById(EvalItem.class, itemId);
 		if (item == null) {
 			throw new IllegalArgumentException("Cannot find item with id: " + itemId);
 		}
 
 		// check perms and locked
 		try {
 			return checkUserControlItem(userId, item);
 		} catch (RuntimeException e) {
 			log.info(e.getMessage());
 		}
 		return false;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.sakaiproject.evaluation.logic.EvalItemsLogic#canRemoveItem(java.lang.String, java.lang.Long)
 	 */
 	public boolean canRemoveItem(String userId, Long itemId) {
 		log.debug("itemId:" + itemId + ", userId:" + userId);
 		// get the item by id
 		EvalItem item = (EvalItem) dao.findById(EvalItem.class, itemId);
 		if (item == null) {
 			throw new IllegalArgumentException("Cannot find item with id: " + itemId);
 		}
 
 		// cannot remove items that are in use
 		if (dao.isUsedItem(itemId)) {
 			log.debug("Cannot remove item ("+itemId+") which is used in at least one template");
 			return false;
 		}
 
 		// check perms and locked
 		try {
 			return checkUserControlItem(userId, item);
 		} catch (RuntimeException e) {
 			log.info(e.getMessage());
 		}
 		return false;
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.sakaiproject.evaluation.logic.EvalItemsLogic#canControlTemplateItem(java.lang.String, java.lang.Long)
 	 */
 	public boolean canControlTemplateItem(String userId, Long templateItemId) {
 		log.debug("templateItemId:" + templateItemId + ", userId:" + userId);
 		// get the template item by id
 		EvalTemplateItem templateItem = (EvalTemplateItem) dao.findById(EvalTemplateItem.class, templateItemId);
 		if (templateItem == null) {
 			throw new IllegalArgumentException("Cannot find template item with id: " + templateItemId);
 		}
 
 		// check perms and locked
 		try {
 			return checkUserControlTemplateItem(userId, templateItem);
 		} catch (RuntimeException e) {
 			log.info(e.getMessage());
 		}
 		return false;
 	}
 
 
 	// PRIVATE METHODS
 
 	/**
 	 * @param userId
 	 * @param item
 	 * @return
 	 */
 	protected boolean checkUserControlItem(String userId, EvalItem item) {
 		log.debug("item: " + item.getId() + ", userId: " + userId);
 		// check locked first
 		if (item.getId() != null &&
 				item.getLocked().booleanValue() == true) {
 			throw new IllegalStateException("Cannot control (modify) locked item ("+item.getId()+")");
 		}
 
 		// check ownership or super user
 		if ( external.isUserAdmin(userId) ) {
 			return true;
 		} else if ( item.getOwner().equals(userId) ) {
 			return true;
 		} else {
 			throw new SecurityException("User ("+userId+") cannot control item ("+item.getId()+") without permissions");
 		}
 	}
 
 	/**
 	 * @param userId
 	 * @param templateItem
 	 * @return
 	 */
 	protected boolean checkUserControlTemplateItem(String userId, EvalTemplateItem templateItem) {
 		log.debug("templateItem: " + templateItem.getId() + ", userId: " + userId);
 		// check locked first (expensive check)
 		if (templateItem.getId() != null &&
 				templateItem.getTemplate().getLocked().booleanValue() == true) {
 			throw new IllegalStateException("Cannot control (modify,remove) template item ("+
 					templateItem.getId()+") in locked template ("+templateItem.getTemplate().getTitle()+")");
 		}
 
 		// check ownership or super user
 		if ( external.isUserAdmin(userId) ) {
 			return true;
 		} else if ( templateItem.getOwner().equals(userId) ) {
 			return true;
 		} else {
 			throw new SecurityException("User ("+userId+") cannot control template item ("+templateItem.getId()+") without permissions");
 		}
 	}
 
 	/**
 	 * @author Aaron Zeckoski (aaronz@vt.edu)
 	 */
 	protected static class TemplateItemDisplayOrderComparator implements Comparator {
 		public int compare(Object ti0, Object ti1) {
 			// expects to get Evaluation objects
 			return ((EvalTemplateItem)ti0).getDisplayOrder().
 				compareTo(((EvalTemplateItem)ti1).getDisplayOrder());
 		}
 	}
 
 }
