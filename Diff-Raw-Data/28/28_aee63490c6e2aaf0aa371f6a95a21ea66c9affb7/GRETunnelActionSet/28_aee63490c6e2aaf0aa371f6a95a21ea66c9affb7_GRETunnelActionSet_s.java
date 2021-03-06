 package net.i2cat.mantychore.actionsets.junos;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import net.i2cat.mantychore.actionsets.junos.actions.CreateTunnelAction;
 import net.i2cat.mantychore.actionsets.junos.actions.DeleteTunnelAction;
 import net.i2cat.mantychore.actionsets.junos.actions.GetTunnelConfigurationAction;
 import net.i2cat.mantychore.actionsets.junos.actions.ShowTunnelsAction;
 
 import org.opennaas.core.resources.action.ActionSet;
 
 public class GRETunnelActionSet extends ActionSet {
 
 	public GRETunnelActionSet() {
 		super.setActionSetId("gretunnelActionSet");
 		// geconfig??
 		this.putAction(ActionConstants.CREATETUNNEL, CreateTunnelAction.class);
 		this.putAction(ActionConstants.DELETETUNNEL, DeleteTunnelAction.class);
 		this.putAction(ActionConstants.GETTUNNELCONFIG, GetTunnelConfigurationAction.class);
 		this.putAction(ActionConstants.SHOWTUNNELS, ShowTunnelsAction.class);
 
 		/* add refresh actions */
 		this.refreshActions.add(ActionConstants.GETCONFIG);
 
 	}
 
 	public List<String> getActionNames() {
 		List<String> actionNames = new ArrayList<String>();
 
 		actionNames.add(ActionConstants.CREATETUNNEL);
 		actionNames.add(ActionConstants.GETTUNNELCONFIG);
 		actionNames.add(ActionConstants.SHOWTUNNELS);
 		actionNames.add(ActionConstants.DELETETUNNEL);
 
 		return actionNames;
 	}
 }
