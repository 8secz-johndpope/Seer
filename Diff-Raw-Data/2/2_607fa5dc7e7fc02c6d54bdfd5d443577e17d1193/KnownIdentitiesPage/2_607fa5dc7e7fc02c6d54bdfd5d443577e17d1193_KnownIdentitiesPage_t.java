 /**
  * This code is part of WoT, a plugin for Freenet. It is distributed 
  * under the GNU General Public License, version 2 (or at your option
  * any later version). See http://www.gnu.org/ for details of the GPL.
  */
 package plugins.WoT.ui.web;
 
 import com.db4o.ObjectContainer;
 import com.db4o.ObjectSet;
 
 import plugins.WoT.Identity;
 import plugins.WoT.OwnIdentity;
 import plugins.WoT.WoT;
 import plugins.WoT.exceptions.DuplicateScoreException;
 import plugins.WoT.exceptions.DuplicateTrustException;
 import plugins.WoT.exceptions.NotInTrustTreeException;
 import freenet.pluginmanager.PluginRespirator;
 import freenet.support.HTMLNode;
 import freenet.support.api.HTTPRequest;
 import freenet.support.Logger;
 
 
 	/**
  * The page where users can manage others identities.
  * 
  * @author Julien Cornuwel (batosai@freenetproject.org)
  */
 public class KnownIdentitiesPage extends WebPageImpl {
 
 	/**
 	 * Creates a new OwnIdentitiesPage.
 	 * 
 	 * @param wot a reference to the WoT, used to get resources the page needs. 
 	 * @param request the request sent by the user.
 	 */
 	public KnownIdentitiesPage(WoT wot, HTTPRequest request) {
 		super(wot, request);
 	}
 
 	/* (non-Javadoc)
 	 * @see plugins.WoT.ui.web.WebPage#make()
 	 */
 	public void make() {
 		OwnIdentity treeOwner = null;
 		ObjectContainer db = wot.getDB();
 		PluginRespirator pr = wot.getPR();
 		
 		makeAddIdentityForm(pr);
 
 		if(request.isParameterSet("knownidentities")) {
 			int nbOwnIdentities = OwnIdentity.getNbOwnIdentities(db);
 
 			if (nbOwnIdentities == 0)
 				makeNoOwnIdentityWarning();
 			else if (nbOwnIdentities == 1)
 				treeOwner = OwnIdentity.getAllOwnIdentities(db).next();
 			else
 				makeSelectTreeOwnerForm(db, pr);
 		}
 		else if(!request.getPartAsString("ownerId", 1024).equals("")) {
 			try {
 				treeOwner = OwnIdentity.getById(db, request.getPartAsString("ownerId", 1024));
 			} catch (Exception e) {
 				Logger.error(this, "Error while selecting the OwnIdentity", e);
 				addErrorBox("Error while selecting the OwnIdentity", e.getLocalizedMessage());
 			}
 		}
 		
 		if(treeOwner != null) {
 			try {
 				makeKnownIdentitiesList(treeOwner, db, pr);
 			} catch (Exception e) {
 				Logger.error(this, e.getMessage());
 				addErrorBox("Error : " + e.getClass(), e.getMessage());
 			}
 		}
 	}
 	
 	/**
 	 * Makes a form where the user can enter the requestURI of an Identity he knows.
 	 * 
 	 * @param pr a reference to the {@link PluginRespirator}
 	 */
 	private void makeAddIdentityForm(PluginRespirator pr) {
 		
 		// TODO Add trust value and comment fields and make them mandatory
 		// The user should only add an identity he trusts
 		HTMLNode addBoxContent = getContentBox("Add an identity");
 	
 		HTMLNode createForm = pr.addFormChild(addBoxContent, SELF_URI, "addIdentity");
 		createForm.addChild("input", new String[] { "type", "name", "value" }, new String[] { "hidden", "page", "addIdentity" });
 		createForm.addChild("span", new String[] {"title", "style"}, new String[] { "This must be a valid Freenet URI.", "border-bottom: 1px dotted; cursor: help;"} , "Identity URI : ");
 		createForm.addChild("input", new String[] {"type", "name", "size"}, new String[] {"text", "identityURI", "70"});
 		createForm.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", "add", "Add this identity !" });	
 	}
 
 	private void makeNoOwnIdentityWarning() {
 		addErrorBox("No own identity found", "You should create an identity first...");
 	}
 	
 	private void makeSelectTreeOwnerForm(ObjectContainer db, PluginRespirator pr) {
 
 		HTMLNode listBoxContent = getContentBox("OwnIdentity selection");
 		HTMLNode selectForm = pr.addFormChild(listBoxContent, SELF_URI, "viewTree");
 		selectForm.addChild("input", new String[] { "type", "name", "value" }, new String[] { "hidden", "page", "viewTree" });
 		HTMLNode selectBox = selectForm.addChild("select", "name", "ownerId");
 
 		ObjectSet<OwnIdentity> ownIdentities = OwnIdentity.getAllOwnIdentities(db);
 		while(ownIdentities.hasNext()) {
 			OwnIdentity ownIdentity = ownIdentities.next();
 			selectBox.addChild("option", "value", ownIdentity.getId(), ownIdentity.getNickName());				
 		}
 		selectForm.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", "select", "View this identity's Web of Trust" });
 	}
 /**
 	 * Makes the list of Identities known by the tree owner.
 	 * 
 	 * @param db a reference to the database 
 	 * @param pr a reference to the {@link PluginRespirator}
 	 * @param treeOwner owner of the trust tree we want to display 
 	 */
 	private void makeKnownIdentitiesList(OwnIdentity treeOwner, ObjectContainer db, PluginRespirator pr) throws DuplicateScoreException, DuplicateTrustException {
 
 		HTMLNode listBoxContent = getContentBox("Known Identities");
 
 		// Display the list of known identities
 		HTMLNode identitiesTable = listBoxContent.addChild("table", "border", "0");
 		HTMLNode row=identitiesTable.addChild("tr");
 		row.addChild("th", "NickName");
 		row.addChild("th", "Last update");
 		row.addChild("th", "Publish TrustList ?");
 		row.addChild("th", "Score (Rank)");
 		row.addChild("th", "Trust/Comment");
 		row.addChild("th", "Trusters");
 		row.addChild("th", "Trustees");
 		
 		ObjectSet<Identity> identities = Identity.getAllIdentities(db);
 		while(identities.hasNext()) {
 			Identity id = identities.next();
 			
 			if(id == treeOwner) continue;
 
 			row=identitiesTable.addChild("tr");
 			
 			// NickName
 			row.addChild("td", new String[] {"title", "style"}, new String[] {id.getRequestURI().toString(), "cursor: help;"}, id.getNickName());
 			
 			// Last Change
 			row.addChild("td", id.getReadableLastChange());
 			
 			// Publish TrustList
 			row.addChild("td", new String[] { "align" }, new String[] { "center" } , id.doesPublishTrustList() ? "Yes" : "No");
 			
 			//Score
 			try {
 				row.addChild("td", new String[] { "align" }, new String[] { "center" } , String.valueOf(id.getScore((OwnIdentity)treeOwner, db).getScore())+" ("+id.getScore((OwnIdentity)treeOwner, db).getRank()+")");
 			}
 			catch (NotInTrustTreeException e) {
 				// This only happen with identities added manually by the user
 				// TODO Maybe we should give the opportunity to trust it at creation time
 				row.addChild("td", "null");	
 			}
 			
 			// Own Trust
			row.addChild(id.getReceivedTrustForm(db, pr, SELF_URI, treeOwner));
 			
 			// Nb Trusters
 			HTMLNode trustersCell = row.addChild("td", new String[] { "align" }, new String[] { "center" });
 			trustersCell.addChild(new HTMLNode("a", "href", SELF_URI + "?getTrusters&id="+id.getId(), Long.toString(id.getNbReceivedTrusts(db))));
 			
 			// Nb Trustees
 			HTMLNode trusteesCell = row.addChild("td", new String[] { "align" }, new String[] { "center" });
 			trusteesCell.addChild(new HTMLNode("a", "href", SELF_URI + "?getTrustees&id="+id.getId(), Long.toString(id.getNbGivenTrusts(db))));
 		}
 
 		
 	}
 
 }
