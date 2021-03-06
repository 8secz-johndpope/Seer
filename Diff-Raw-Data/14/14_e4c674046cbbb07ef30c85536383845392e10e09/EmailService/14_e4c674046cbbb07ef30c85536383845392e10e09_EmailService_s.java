 package pl.softwaremill.asamal.example.service.email;
 
 import org.apache.velocity.VelocityContext;
 import org.apache.velocity.app.Velocity;
 import pl.softwaremill.asamal.example.logic.auth.LoginBean;
 import pl.softwaremill.asamal.example.logic.conf.ConfigurationBean;
 import pl.softwaremill.asamal.example.model.conf.Conf;
 import pl.softwaremill.asamal.example.model.ticket.Invoice;
import pl.softwaremill.asamal.helper.AsamalHelper;
 import pl.softwaremill.common.cdi.transaction.Transactional;
 import pl.softwaremill.common.sqs.email.SendEmailTask;
 import pl.softwaremill.common.sqs.util.EmailDescription;
 
 import javax.inject.Inject;
 import javax.persistence.EntityManager;
 import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
 import java.io.StringWriter;
 import java.util.List;
 
 public class EmailService {
 
     @Inject
     private ConfigurationBean configurationBean;
 
     @Inject
     private LoginBean loginBean;
 
    @Inject
    private AsamalHelper asamalHelper;

    @Inject
    private HttpServletRequest request;

     @PersistenceContext
     private EntityManager entityManager;
 
     public void sendThankYouEmail(Invoice invoice) {
         VelocityContext context = new VelocityContext();
 
         context.put("name", invoice.getName());
         context.put("tickets", invoice.getTickets());
         context.put("paymentMethod", invoice.getMethod());
 
         String emailTemplate = configurationBean.getProperty(Conf.TICKETS_THANKYOU_EMAIL);
 
         StringWriter sw = new StringWriter();
         Velocity.evaluate(context, sw, "emailThankYou", emailTemplate);
 
         EmailSendingBean.scheduleTask(new SendEmailTask(new EmailDescription(loginBean.getUser().getUsername(),
                 sw.toString(), configurationBean.getProperty(Conf.TICKETS_THANKYOU_EMAIL_SUBJECT), null, null,
                 configurationBean.getProperty(Conf.TICKETS_THANKYOU_BCC))));
     }
 
     public void sendInvoiceEmail(Invoice invoice) {
         VelocityContext context = new VelocityContext();
 
         context.put("name", invoice.getName());
 
         context.put("invoice_link", getInvoiceLink(invoice));
 
         String emailTemplate = configurationBean.getProperty(Conf.INVOICE_EMAIL);
 
         StringWriter sw = new StringWriter();
         Velocity.evaluate(context, sw, "emailInvoice", emailTemplate);
 
         EmailSendingBean.scheduleTask(new SendEmailTask(new EmailDescription(loginBean.getUser().getUsername(),
                 sw.toString(), configurationBean.getProperty(Conf.INVOICE_EMAIL_SUBJECT))));
     }
 
     public void sendTransferAcceptedEmail(Invoice invoice) {
         VelocityContext context = new VelocityContext();
 
         context.put("name", invoice.getName());
 
         context.put("invoice_link", getInvoiceLink(invoice));
 
         String emailTemplate = configurationBean.getProperty(Conf.TICKETS_TRANSFER_RECEIVED_EMAIL);
 
         StringWriter sw = new StringWriter();
         Velocity.evaluate(context, sw, "emailTransferReceived", emailTemplate);
 
         EmailSendingBean.scheduleTask(new SendEmailTask(new EmailDescription(loginBean.getUser().getUsername(),
                 sw.toString(), configurationBean.getProperty(Conf.TICKETS_TRANSFER_RECEIVED_SUBJECT))));
     }
 
     @Transactional
     public void sendEmailToAll(String subject, String message) {
         List<String> allEmails = entityManager.createQuery(
                 "select distinct(i.user.username) from Invoice i").getResultList();
 
         allEmails.add(configurationBean.getProperty(Conf.TICKETS_THANKYOU_BCC));
 
         // for each user create new task
         for (String email : allEmails) {
             EmailSendingBean.scheduleTask(new SendEmailTask(new EmailDescription(email, message, subject)));
         }
     }
 
     private String getInvoiceLink(Invoice invoice) {
        return configurationBean.getProperty(Conf.SYSTEM_URL) + asamalHelper.pdf("invoice", "pdf") +
                "/" + invoice.getId();
     }
 }
