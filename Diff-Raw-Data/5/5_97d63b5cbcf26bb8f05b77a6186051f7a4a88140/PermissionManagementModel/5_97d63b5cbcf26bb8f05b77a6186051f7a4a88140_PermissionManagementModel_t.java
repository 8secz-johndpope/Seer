 /**
  * @author Jessica Lundberg
  * @author Erik Roos
  * @author Robert Wagner
  * @date 04-02-2011
  * 
  * This is the model for the PermissionManagementPlugin, which allows
  * users to see their rights and also the rights of others on entities
  * they own. 
  */
 package org.molgenis.auth.service.permissionmanagement;
 
 import org.molgenis.auth.MolgenisRole;
 
 public class PermissionManagementModel {
 
	//Danny: This was crashing freemarker, if we can't reach the database we 
	//still need to have a default role so we don't blow up the template
    private MolgenisRole role = new MolgenisRole(999,"unk");
     private String action = "init";
     private int permId = 0;
     
     public PermissionManagementModel() {
 	
     }
 
     public void setRole(MolgenisRole molgenisRole) {
 	this.role = molgenisRole;
     }
 
     public MolgenisRole getRole() {
 	return role;
     }
 
 	public void setAction(String action) {
 		this.action = action;
 	}
 
 	public String getAction() {
 		return action;
 	}
 
 	public void setPermId(int permId) {
 		this.permId = permId;
 	}
 
 	public int getPermId() {
 		return permId;
 	}
 }
