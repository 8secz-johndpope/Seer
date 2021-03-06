 package org.caleydo.core.view.opengl.util.overlay.contextmenu.item;
 
 import org.caleydo.core.manager.event.view.group.InterchangeGroupsEvent;
 import org.caleydo.core.view.opengl.util.overlay.contextmenu.AContextMenuItem;
 import org.caleydo.core.view.opengl.util.texture.EIconTextures;
 
 /**
  * Item for interchanging to groups/clusters
  * 
  * @author Bernhard Schlegl
  */
 public class InterchangeGroupsItem
 	extends AContextMenuItem {
 
 	/**
 	 * Constructor which sets the default values for icon and text
 	 */
 	public InterchangeGroupsItem() {
 		super();
 		setIconTexture(EIconTextures.CM_LOAD_DEPENDING_PATHWAYS);
 		setText("Interchange Groups");
 	}
 
 	/**
	 * Depending on which group info should be handeld a bollean has to be set. True for genes, false for
 	 * experiments
 	 * 
 	 * @param bGeneGroup
 	 *            if true gene groups will be handled, if false experiment groups
 	 */
 	public void setGeneExperimentFlag(boolean bGeneGroup) {
 
 		InterchangeGroupsEvent interchangeGroupsEvent = new InterchangeGroupsEvent();
 		interchangeGroupsEvent.setSender(this);
 		interchangeGroupsEvent.setGeneExperimentFlag(bGeneGroup);
 		registerEvent(interchangeGroupsEvent);
 	}
 }
