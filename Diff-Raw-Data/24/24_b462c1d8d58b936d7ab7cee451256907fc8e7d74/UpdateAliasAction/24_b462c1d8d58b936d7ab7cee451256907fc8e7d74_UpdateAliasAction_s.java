 package jamm.webapp;
 
 import java.util.Set;
 import java.util.HashSet;
 import java.util.Arrays;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
 
import org.apache.struts.action.Action;
 import org.apache.struts.action.ActionMapping;
 import org.apache.struts.action.ActionForm;
 import org.apache.struts.action.ActionForward;
 import org.apache.struts.action.ActionErrors;
 import org.apache.struts.action.ActionError;
 
 import jamm.backend.MailManager;
 
 public class UpdateAliasAction extends JammAction
 {
     public ActionForward execute(ActionMapping mapping,
                                  ActionForm actionForm,
                                  HttpServletRequest request,
                                  HttpServletResponse response)
         throws Exception
     {
         UpdateAliasForm form = (UpdateAliasForm) actionForm;
         User user = getUser(request);
         MailManager manager =
             new MailManager(Globals.getLdapHost(),
                             Globals.getLdapPort(),
                             Globals.getLdapSearchBase(),
                             user.getDn(),
                             user.getPassword());
 
         String mail = form.getMail();
         String[] destinations = manager.getAliasDestinations(mail);
         
         Set newDestinations = new HashSet(Arrays.asList(destinations));
         newDestinations.addAll(form.getAddedAddresses());
         newDestinations.removeAll(Arrays.asList(form.getDeleted()));
 
         ActionErrors errors = new ActionErrors();
 
         if (newDestinations.size() == 0)
         {
             errors.add(ActionErrors.GLOBAL_ERROR,
                        new ActionError("alias_update.error.non_zero_aliases"));
         }
 
         if (!errors.empty())
         {
             saveErrors(request, errors);
             return new ActionForward(mapping.getInput());
         }
 
         manager.modifyAlias(mail, newDestinations);
 
         // Get the forward, and then create a new forward with the mail
         // tacked onto it.
         ActionForward adminForward = mapping.findForward("account_admin");
         StringBuffer pathWithMail = new StringBuffer(adminForward.getPath());
         pathWithMail.append("?mail=").append(mail);
         ActionForward forwardWithMail =
             new ActionForward(pathWithMail.toString(),
                               adminForward.getRedirect());
         return (forwardWithMail);
     }
 }
