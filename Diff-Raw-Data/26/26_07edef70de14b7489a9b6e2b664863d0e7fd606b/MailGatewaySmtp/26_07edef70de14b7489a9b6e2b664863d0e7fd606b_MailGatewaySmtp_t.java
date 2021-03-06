 package net.ripe.db.whois.update.mail;
 
import com.sun.mail.smtp.SMTPAddressFailedException;
 import net.ripe.db.whois.common.Message;
 import net.ripe.db.whois.common.Messages;
 import net.ripe.db.whois.common.aspects.RetryFor;
import net.ripe.db.whois.common.collect.CollectionHelper;
 import net.ripe.db.whois.update.domain.ResponseMessage;
 import net.ripe.db.whois.update.log.LoggerContext;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.mail.MailException;
 import org.springframework.mail.MailParseException;
 import org.springframework.mail.MailSendException;
 import org.springframework.mail.javamail.JavaMailSender;
 import org.springframework.mail.javamail.MimeMessageHelper;
 import org.springframework.mail.javamail.MimeMessagePreparator;
 import org.springframework.stereotype.Component;
 
 import javax.mail.MessagingException;
 import javax.mail.internet.MimeMessage;
 
 @Component
 public class MailGatewaySmtp implements MailGateway {
     private static final Logger LOGGER = LoggerFactory.getLogger(MailGatewaySmtp.class);
 
     private final LoggerContext loggerContext;
     private final MailConfiguration mailConfiguration;
     private final JavaMailSender mailSender;
 
     private boolean outgoingMailEnabled;
 
     @Value("${mail.smtp.enabled}")
     public void setOutgoingMailEnabled(final boolean outgoingMailEnabled) {
         LOGGER.info("Outgoing mail enabled: {}", outgoingMailEnabled);
         this.outgoingMailEnabled = outgoingMailEnabled;
     }
 
     @Autowired
     public MailGatewaySmtp(final LoggerContext loggerContext, final MailConfiguration mailConfiguration, final JavaMailSender mailSender) {
         this.loggerContext = loggerContext;
         this.mailConfiguration = mailConfiguration;
         this.mailSender = mailSender;
     }
 
     @Override
     @RetryFor(value = MailSendException.class, attempts = 20, intervalMs = 10000)
     public void sendEmail(final String to, final ResponseMessage responseMessage) {
         sendEmail(to, responseMessage.getSubject(), responseMessage.getMessage());
     }
 
     @Override
     @RetryFor(value = MailSendException.class, attempts = 20, intervalMs = 10000)
     public void sendEmail(final String to, final String subject, final String text) {
         if (!outgoingMailEnabled) {
             LOGGER.warn("" +
                     "Outgoing mail disabled\n" +
                     "\n" +
                     "to      : {}\n" +
                     "subject : {}\n" +
                     "\n" +
                     "{}\n" +
                     "\n" +
                     "\n", to, subject, text);
 
             return;
         }
 
         try {
             sendEmailAttempt(to, subject, text);
         } catch (MailSendException e) {
             loggerContext.log(new Message(Messages.Type.ERROR, "Unable to send mail message to {} with subject {}", to, subject), e);
            if (!CollectionHelper.containsType(e.getMessageExceptions(), SMTPAddressFailedException.class)) {
                // don't retry on irrecoverable errors (e.g. malformed email address)
                throw e;
            }
         } catch (MailParseException e) {
             loggerContext.log(new Message(Messages.Type.ERROR, "Unable to parse mail to {} with subject {}", to, subject), e);
         } catch (MailException e) {
             LOGGER.error("Unable to send mail message to: {}", to, e);
         }
     }
 
     private void sendEmailAttempt(final String to, final String subject, final String text) {
         mailSender.send(new MimeMessagePreparator() {
             @Override
             public void prepare(final MimeMessage mimeMessage) throws MessagingException {
                 final MimeMessageHelper message = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_NO, "UTF-8");
                 message.setFrom(mailConfiguration.getFrom());
                 message.setTo(to);
                 message.setSubject(subject);
                 message.setText(text);
 
                 mimeMessage.addHeader("Precedence", "bulk");
                 mimeMessage.addHeader("Auto-Submitted", "auto-generated");
 
                 loggerContext.log("msg-out.txt", new MailMessageLogCallback(mimeMessage));
             }
         });
     }
 }
