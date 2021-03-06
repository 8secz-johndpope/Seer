 package net.jmhertlein.core.reporting;
 
 import java.io.IOException;
 import java.io.ObjectInputStream;
 import java.net.Socket;
 import java.util.LinkedList;
 import java.util.Set;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.mail.MessagingException;
 import javax.mail.internet.AddressException;
 import net.jmhertlein.core.mail.GoogleMail;
 
 /**
  *
  * @author joshua
  */
 public class ReceiveBugReportsWorkerThread extends Thread {
 
     private final Set<BugReport> reports;
     private final LinkedList<Socket> clientSockets;
     private boolean stop;
 
     public ReceiveBugReportsWorkerThread(Set<BugReport> reports, LinkedList<Socket> clients) {
         this.reports = reports;
         this.clientSockets = clients;
         stop = false;
     }
 
     @Override
     public void run() {
         while (!stop) {
             if (!clientSockets.isEmpty()) {
                 Socket cur;
                 synchronized (clientSockets) {
                     cur = clientSockets.removeFirst();
                 }
                 System.out.println("Worker received job.");
                 receiveReport(cur);
                 System.out.println("Worker finished job.");
                 try {
                     cur.close();
                     System.out.println("Worker disconnected client.");
                 } catch (IOException ex) {
                     Logger.getLogger(ReceiveBugReportsWorkerThread.class.getName()).log(Level.SEVERE, null, ex);
                 }
             } else {
                 try {
                     synchronized(clientSockets) {
                         if(!Thread.interrupted()) {
                             System.out.println("Waiting for new client...");
                             clientSockets.wait();
                             System.out.println("Done waiting for new client. Re-evaluating.");
                         } else {
                             System.out.println("Noticed interrupt flag, re-evaluating instead of waiting.");
                         }
                     }
                 } catch (InterruptedException ex) {
                     System.out.println("Worker interrupted, re-evaluating.");
                 }
             }
         }
         System.out.println("Worker thread closing.");
     }
 
     private void receiveReport(Socket client) {
         try (ObjectInputStream ois = new ObjectInputStream(client.getInputStream())) {
             Object rawReceived = ois.readObject();
             if (!(rawReceived instanceof BugReport)) {
                 System.err.println("Received object was not a BugReport!");
                 return;
             }
 
             BugReport received = (BugReport) rawReceived;
 
             received.setIp(client.getInetAddress().getHostAddress());
 
             System.out.println("Received report from " + client.getInetAddress());
             if (reports.contains(received)) {
                 System.out.println("Report was a duplicate, dropped.");
             } else {
                 synchronized (reports) {
                     reports.add(received);
                 }
                 //send email
                if(BugReportDaemon.canSendEmail())
                     GoogleMail.send(BugReportDaemon.getEmailSenderName(), BugReportDaemon.getEmailSenderPassword(), BugReportDaemon.getEmailDestination(), "Bug Report from " + client.getInetAddress().toString(), received.toString());
             }
         } catch (IOException ex) {
             System.err.println("Error dowloading: " + ex.getLocalizedMessage());
         } catch (ClassNotFoundException ex) {
             Logger.getLogger(ReceiveBugReportsWorkerThread.class.getName()).log(Level.SEVERE, null, ex);
         } catch (MessagingException ex) {
             Logger.getLogger(ReceiveBugReportsWorkerThread.class.getName()).log(Level.SEVERE, null, ex);
         }
     }
 
     public void setStopFlag() {
         this.stop = true;
     }
 }
