 package fi.internetix.edelphi.pages;
 
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.List;
 
 import fi.internetix.edelphi.dao.base.DelfoiBulletinDAO;
 import fi.internetix.edelphi.dao.panels.PanelDAO;
 import fi.internetix.edelphi.dao.panels.PanelInvitationDAO;
 import fi.internetix.edelphi.domainmodel.base.Bulletin;
 import fi.internetix.edelphi.domainmodel.base.Delfoi;
 import fi.internetix.edelphi.domainmodel.panels.Panel;
 import fi.internetix.edelphi.domainmodel.panels.PanelAccessLevel;
 import fi.internetix.edelphi.domainmodel.panels.PanelInvitation;
 import fi.internetix.edelphi.domainmodel.panels.PanelInvitationState;
 import fi.internetix.edelphi.domainmodel.panels.PanelState;
 import fi.internetix.edelphi.domainmodel.users.User;
 import fi.internetix.edelphi.utils.ActionUtils;
 import fi.internetix.edelphi.utils.RequestUtils;
 import fi.internetix.smvc.PageNotFoundException;
 import fi.internetix.smvc.controllers.PageRequestContext;
 
 public class ViewBulletinPageController extends DelfoiPageController {
 
   @Override
   public void processPageRequest(PageRequestContext pageRequestContext) { 
     DelfoiBulletinDAO bulletinDAO = new DelfoiBulletinDAO();
 
     Long bulletinId = pageRequestContext.getLong("bulletinId");
    Bulletin bulletin = bulletinDAO.findById(bulletinId);
     if (bulletin == null) {
       throw new PageNotFoundException(pageRequestContext.getRequest().getLocale());
     }
     
     pageRequestContext.getRequest().setAttribute("bulletin", bulletin);
     
     PanelDAO panelDAO = new PanelDAO();
     Delfoi delfoi = RequestUtils.getDelfoi(pageRequestContext);
     
     pageRequestContext.getRequest().setAttribute("openPanels", 
         panelDAO.listByDelfoiAndAccessLevelAndState(delfoi, PanelAccessLevel.OPEN, PanelState.IN_PROGRESS));
 
     User loggedUser = RequestUtils.getUser(pageRequestContext);
     if (loggedUser != null) {
       List<Panel> myPanels = ActionUtils.isSuperUser(pageRequestContext) ? panelDAO.listByDelfoi(delfoi) : panelDAO.listByDelfoiAndUser(delfoi, loggedUser);
       Collections.sort(myPanels, new Comparator<Panel>() {
         @Override
         public int compare(Panel o1, Panel o2) {
           return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
         }
       });
       pageRequestContext.getRequest().setAttribute("myPanels", myPanels);
       // TODO what about invitations to user's other e-mail addresses?
       if (loggedUser.getDefaultEmail() != null) {
         PanelInvitationDAO panelInvitationDAO = new PanelInvitationDAO();
         List<PanelInvitation> myInvitations = panelInvitationDAO.listByEmailAndState(loggedUser.getDefaultEmail().getAddress(), PanelInvitationState.PENDING);
         pageRequestContext.getRequest().setAttribute("myInvitations", myInvitations);
       }
     }
     
//    // TODO: Ugly hack...
//    TupasAuthenticationStrategy tupasAuthenticationProvider = (TupasAuthenticationStrategy) AuthenticationProviderFactory.getInstance().getAuthenticationProvider("Tupas");
//    if (tupasAuthenticationProvider != null) {
//      tupasAuthenticationProvider.appendBankSettings(pageRequestContext);
//    }

     // Action access information
     ActionUtils.includeRoleAccessList(pageRequestContext);
     
     pageRequestContext.setIncludeJSP("/jsp/pages/viewbulletin.jsp");
   }
 }
