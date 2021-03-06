 /*
  * Created by IntelliJ IDEA.
  * User: fhuet
  * Date: Apr 18, 2002
  * Time: 1:38:12 PM
  * To change template for new class use
  * Code Style | Class Templates options (Tools | IDE Options).
  */
 package org.objectweb.proactive.ext.mixedlocation;
 
 import java.io.IOException;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
 
 import org.objectweb.proactive.Body;
 import org.objectweb.proactive.core.body.UniversalBody;
 import org.objectweb.proactive.core.body.migration.MigrationManagerImpl;
 import org.objectweb.proactive.core.body.reply.ReplyReceiver;
 import org.objectweb.proactive.core.body.reply.ReplyReceiverForwarder;
 import org.objectweb.proactive.core.body.request.RequestReceiver;
 import org.objectweb.proactive.core.body.request.RequestReceiverForwarder;
 import org.objectweb.proactive.ext.locationserver.LocationServer;
 
 
 public class MigrationManagerWithMixedLocation extends MigrationManagerImpl
     implements java.io.Serializable {
     	
     protected UniversalBodyWrapper wrapper;
     transient protected LocationServer locationServer;
     protected int migrationCounter;
 
     public MigrationManagerWithMixedLocation() {
         System.out.println("<init> LocationServer is " + locationServer);
     }
 
     public MigrationManagerWithMixedLocation(LocationServer locationServer) {
         this.migrationCounter = 0;
         System.out.println("LocationServer is " + locationServer);
         this.locationServer = locationServer;
     }
 
     protected synchronized void createWrapper(UniversalBody remoteBody) {
         if (this.wrapper == null) {
             this.wrapper = new UniversalBodyWrapper(remoteBody, 6000);
         }
     }
 
     public RequestReceiver createRequestReceiver(UniversalBody remoteBody, 
                                                  RequestReceiver currentRequestReceiver) {
         this.createWrapper(remoteBody);
         return new RequestReceiverForwarder(wrapper);
     }
 
     public ReplyReceiver createReplyReceiver(UniversalBody remoteBody, 
                                              ReplyReceiver currentReplyReceiver) {
         this.createWrapper(wrapper);
         return new ReplyReceiverForwarder(wrapper);
     }
 
     public void updateLocation(Body body) {
        //        System.out.println("MigrationManagerWithMixedLocation.updateLocation " +
        //        locationServer);
         if (locationServer != null) {
            //            System.out.println("MigrationManagerWithMixedLocation.updateLocation");
             locationServer.updateLocation(body.getID(), body.getRemoteAdapter());
         }
     }
 
     public void startingAfterMigration(Body body) {
         super.startingAfterMigration(body);
         //we update our location
         this.migrationCounter++;
        System.out.println("XXX counter == " + this.migrationCounter);
        //          if (this.migrationCounter > 3) {
         updateLocation(body);
        //              this.migrationCounter = 0;
        //          }
     }
 
     private void readObject(ObjectInputStream in)
                      throws IOException, ClassNotFoundException {
         System.out.println(
                 "MigrationManagerWithMixedLocation readObject XXXXXXX");
         in.defaultReadObject();
     }
 
     private void writeObject(ObjectOutputStream out)
                       throws IOException {
         System.out.println(
                 "MigrationManagerWithMixedLocation writeObject YYYYYY");
                 this.locationServer = null;
         out.defaultWriteObject();
     }
 }
