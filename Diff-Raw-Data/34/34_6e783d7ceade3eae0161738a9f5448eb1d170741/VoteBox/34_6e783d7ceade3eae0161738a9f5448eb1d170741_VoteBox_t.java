 /**
   * This file is part of VoteBox.
   * 
   * VoteBox is free software: you can redistribute it and/or modify
   * it under the terms of the GNU General Public License version 3 as published by
   * the Free Software Foundation.
   * 
   * You should have received a copy of the GNU General Public License
   * along with VoteBox, found in the root of any distribution or
   * repository containing all or part of VoteBox.
   * 
   * THIS SOFTWARE IS PROVIDED BY WILLIAM MARSH RICE UNIVERSITY, HOUSTON,
   * TX AND IS PROVIDED 'AS IS' AND WITHOUT ANY EXPRESS, IMPLIED OR
   * STATUTORY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, WARRANTIES OF
   * ACCURACY, COMPLETENESS, AND NONINFRINGEMENT.  THE SOFTWARE USER SHALL
   * INDEMNIFY, DEFEND AND HOLD HARMLESS RICE UNIVERSITY AND ITS FACULTY,
   * STAFF AND STUDENTS FROM ANY AND ALL CLAIMS, ACTIONS, DAMAGES, LOSSES,
   * LIABILITIES, COSTS AND EXPENSES, INCLUDING ATTORNEYS' FEES AND COURT
   * COSTS, DIRECTLY OR INDIRECTLY ARISING OUR OF OR IN CONNECTION WITH
   * ACCESS OR USE OF THE SOFTWARE.
  */
 
 package votebox;
 
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.KeyAdapter;
 import java.awt.event.KeyEvent;
 import java.awt.image.BufferedImage;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.util.*;
 
 import javax.imageio.ImageIO;
 import javax.swing.*;
 import javax.swing.Timer;
 
 import com.sun.deploy.util.ArrayUtil;
 import crypto.BallotEncrypter;
 import crypto.PiecemealBallotEncrypter;
 import crypto.interop.AdderKeyManipulator;
 import edu.uconn.cse.adder.AdderInteger;
 import edu.uconn.cse.adder.PublicKey;
 
 import preptool.model.language.Language;
 import preptool.model.layout.PrintButton;
 import preptool.model.layout.ReviewButton;
 import preptool.model.layout.manager.RenderingUtils;
 import sexpression.*;
 import sexpression.stream.InvalidVerbatimStreamException;
 import votebox.events.*;
 import votebox.middle.*;
 import votebox.middle.Properties;
 import votebox.middle.ballot.Ballot;
 import votebox.middle.driver.*;
 import votebox.middle.view.*;
 import auditorium.*;
 import auditorium.Event;
 import printer.*;
 
 /**
  * This is the top level votebox main class. This class organizes and connects
  * the components of VoteBox, namely:<br>
  * 1) The auditorium network<br>
  * 2) The vote storage backend<br>
  * 3) The votebox "middle" (that is, the link between the voter and the backend,
  * the gui)
  * 
  * @author derrley, cshaw
  */
 public class VoteBox{
 
     private final AuditoriumParams _constants;
     private final IViewFactory _factory;
     private VoteBoxAuditoriumConnector auditorium;
     private Driver currentDriver;
     private IVoteBoxInactiveUI inactiveUI;
     private final int mySerial;
     private boolean connected;
     private boolean voting;
     private boolean promptingForPin;
     private boolean override;
     private boolean committedBallot;
     private boolean finishedVoting;
     private boolean isProvisional;
     private int label;
     private String bid;
     private Event<Integer> labelChangedEvent;
     private int protectedCount;
     private int publicCount;
     private int numConnections;
     private byte[] nonce;
     private int pageBeforeOverride;
     private Timer killVBTimer;
     private Timer statusTimer;
     boolean superOnline;
     private int superSerial;
     private JOptionPane pinPane;
     private String precinct;
 
     /**
      * Will keep the short code - nonce pairings to send over when the polls close
      */
     private HashMap<ASExpression, VotePair> plaintextAuditCommits;
 
     private  Printer printer;
 
     private Random rand;
 
     private byte[] pinNonce;
 
     private File _currentBallotFile;
 
     private static File staticCurrentBallotFile;
 
     /**
      * Equivalent to new VoteBox(-1).
      */
     public VoteBox(){
     	this(-1);
     }
     
     /**
      * Constructs a new instance of a persistent VoteBox booth. This
      * implementation runs in the background, on an auditorium network, and
      * waits to receive an authorization before launching the VoteBox middle.
      * For a standalone implementation, see
      * {@link votebox.middle.datacollection.Launcher}.
      * 
      * @param serial
      *            the serial number of the votebox
      */
     public VoteBox(int serial) {
         rand = new Random(System.currentTimeMillis());
         _constants = new AuditoriumParams("vb.conf");
         
         if(serial != -1)
         	mySerial = serial;
         else
         	mySerial = _constants.getDefaultSerialNumber();
         
         
         if(mySerial == -1)
         	throw new RuntimeException("usage: VoteBox <machineID>");
         
         numConnections = 0;
         labelChangedEvent = new Event<Integer>();
         statusTimer = new Timer(300000, new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 if (connected) {
                     auditorium.announce(getStatus());
                 }
             }
         });
 

         if (_constants.getViewImplementation().equals("AWT")) {
             // run fullscreen on OSX only
             _factory = new AWTViewFactory(_constants.getUseWindowedView(), _constants.getAllowUIScaling());
         }else
             throw new RuntimeException(
                     "Unknown view implementation defined in configuration");
 
         promptingForPin = false;
 
         plaintextAuditCommits = new HashMap<ASExpression, VotePair>();
 
         staticCurrentBallotFile = new File(_constants.getBallotFile());
     }
 
     /**
      * Broadcasts this VoteBox booth's status, and resets the status timer
      */
     public void broadcastStatus() {
         auditorium.announce(getStatus());
         statusTimer.restart();
     }
 
     /**
      * Returns this booth's label, assigned by a supervisor over auditorium. If
      * unassigned, will return 0.
      * 
      * @return the label
      */
     public int getLabel() {
         return label;
     }
 
     /**
      * Returns this booth's status as a VoteBoxEvent, used for periodic
      * broadcasts
      * 
      * @return the status
      */
     public VoteBoxEvent getStatus() {
         VoteBoxEvent event;
 
         int battery = BatteryStatus.read(_constants.getOS());
 
         if (voting && isProvisional)
             event = new VoteBoxEvent(mySerial, label, "provisional-in-use", battery,
                     protectedCount, publicCount);
         else if(voting)
             event = new VoteBoxEvent(mySerial, label, "in-use", battery,
                     protectedCount, publicCount);
         else
             event = new VoteBoxEvent(mySerial, label, "ready", battery,
                     protectedCount, publicCount);
         return event;
     }
 
     /**
      * Allows the VoteBox inactive UI (what is shown when a user isn't voting)
      * to register for a label changed event, and update itself accordingly
      * 
      * @param obs
      *            the observer
      */
     public void registerForLabelChanged(Observer obs) {
         labelChangedEvent.addObserver(obs);
     }
 
     /**
      * Launch the VoteBox middle. Registers for events that we would want to
      * know about (such as cast ballot, so we can send the message over
      * auditorium)
      * 
      * @param location
      *            the location on disk of the ballot
      */
     public void run(String location) {
         inactiveUI.setVisible(false);
         currentDriver = new Driver(location, _factory, _constants.getCastBallotEncryptionEnabled());
         voting = true;
         currentDriver.run();
 


         	
 
         //Listen for commit ui events.  When received, send out an encrypted vote.
         currentDriver.getView().registerForCommit(new Observer() {
 
             @SuppressWarnings("unchecked")
             public void update(Observable o, Object argTemp) {
                 if (!connected)
                     throw new RuntimeException(
                             "Attempted to cast ballot when not connected to any machines");
                 if (!voting || currentDriver == null)
                     throw new RuntimeException(
                             "VoteBox attempted to cast ballot, but was not currently voting");
                 if (finishedVoting)
                     throw new RuntimeException(
                             "This machine has already finished voting, but attempted to vote again");
 
                 committedBallot = true;
 
                 Object[] arg = (Object[])argTemp;
 
                 // arg1 should be the cast ballot structure, check
                 if (Ballot.BALLOT_PATTERN.match((ASExpression) arg[0]) == NoMatch.SINGLETON)
                     throw new RuntimeException(
                             "Incorrectly expected a cast-ballot");
                 ListExpression ballot = (ListExpression) arg[0];
 
                 for(Object i : arg){
                     System.out.println(i);
                 }
 
                 try {
                     if(!isProvisional){
                         if(!_constants.getEnableNIZKs()){
                             auditorium.announce(new CommitBallotEvent(mySerial,
                                     StringExpression.makeString(nonce),
                                     BallotEncrypter.SINGLETON.encrypt(ballot, _constants.getKeyStore().loadKey(mySerial + "-public")), StringExpression.makeString(bid), StringExpression.makeString(precinct)));
                         } else{
                             ASExpression encBallot = BallotEncrypter.SINGLETON.encryptWithProof(ballot, (List<List<String>>) arg[1],
                                     (PublicKey) _constants.getKeyStore().loadAdderKey("public"));
 
                             //Compute and store shortcodes for this ballot
                             //shortCodes(ballot, (List<List<String>>) arg[1]);
 
                             auditorium.announce(new CommitBallotEvent(mySerial,
                                     StringExpression.makeString(nonce),
                                     encBallot,
                                     StringExpression.makeString(bid), StringExpression.makeString(precinct)));
                         }
                     } else {
                         auditorium.announce(new ProvisionalCommitEvent(mySerial,
                                 StringExpression.makeString(nonce),
                                 BallotEncrypter.SINGLETON.encrypt(ballot, _constants.getKeyStore().loadKey(mySerial + "-public")), StringExpression.makeString(bid)));
                     }
 
 
                     List<List<String>> races = currentDriver.getBallotAdapter().getRaceGroups();
                     auditorium.announce(new BallotPrintingEvent(mySerial, bid,
                             nonce));
                     printer = new Printer(_currentBallotFile, races);
 
                     boolean success = printer.printCommittedBallot(ballot, bid);
                     printer.printedReceipt(bid);
 
                     //By this time, the voter is done voting
                     //Wait before returning to inactive
                     long start = System.currentTimeMillis();
                     while(System.currentTimeMillis() - start < 5000);
                     finishedVoting = true;
 
                     if(success)
                         auditorium.announce(new BallotPrintSuccessEvent(mySerial, bid, nonce));
 
                 } catch (AuditoriumCryptoException e) {
                     Bugout.err("Crypto error trying to commit ballot: "+e.getMessage());
                     e.printStackTrace();
                 }
         	}
         });
         	
         //Listen for cast ui events.
         //Rather than actually send the ballot out, just send the nonce (which can identify the whole
         //transaction).
         //Clean up the encryptor afterwards so as to destroy the random number needed for challenging.
         //TODO This code doesn't do anything?
         currentDriver.getView().registerForCastBallot(new Observer(){
 
             public void update(Observable o, Object argTemp) {
                 if (!connected)
                     throw new RuntimeException(
                             "Attempted to cast ballot when not connected to any machines");
                 if (!voting || currentDriver == null)
                     throw new RuntimeException(
                             "VoteBox attempted to cast ballot, but was not currently voting");
                 if (finishedVoting)
                     throw new RuntimeException(
                             "This machine has already finished voting, but attempted to vote again");
 
                 finishedVoting = true;
                 ++publicCount;
                 ++protectedCount;
 
                 //Object[] arg = (Object[])argTemp;
 
                 auditorium.announce(new CastCommittedBallotEvent(mySerial,
                         StringExpression.makeString(nonce), StringExpression.makeString(bid)));
 
                 BallotEncrypter.SINGLETON.clear();
 
             }
 
         });
 
         /**
          * If we're using piecemeal encryption, we need to listen for each page change.
          */
         if(_constants.getUsePiecemealEncryption()){
             currentDriver.getView().registerForPageChanged(new Observer(){
                 public void update(Observable o, Object args){
                     List<String> affectedUIDs = (List<String>)args;
 
                     Map<String, List<ASExpression>> needUpdate = currentDriver.getBallotAdapter().getAffectedRaces(affectedUIDs);
 
                     for(String uid : needUpdate.keySet()){
                         if(!_constants.getEnableNIZKs()){
                             try{
                                 PiecemealBallotEncrypter.SINGELTON.update(uid, needUpdate.get(uid), _constants.getKeyStore().loadKey("public"));
                             }catch(AuditoriumCryptoException e){
                                 throw new RuntimeException(e);
                             }
                         }else{
                             List<String> raceGroup = currentDriver.getBallotAdapter().getRaceGroupContaining(needUpdate.get(uid));
                             PiecemealBallotEncrypter.SINGELTON.adderUpdate(uid, needUpdate.get(uid), raceGroup, (PublicKey)_constants.getKeyStore().loadAdderKey("public"));
                         }
                     }
                 }
             });
         }
 
         currentDriver.getView().registerForOverrideCancelConfirm(
                 new Observer() {
                     /**
                      * Kill the VB runtime, and announce the confirm message
                      */
                     public void update(Observable o, Object arg) {
                         if (voting && override && !finishedVoting
                                 && currentDriver != null) {
                             auditorium.announce(new OverrideCancelConfirmEvent(
                                     mySerial, nonce));
                             currentDriver.kill();
                             currentDriver = null;
                             nonce = null;
                             voting = false;
                             override = false;
                             broadcastStatus();
                             inactiveUI.setVisible(true);
                             promptForPin("Enter Voting Authentication PIN");
                             
                             //printBallotSpoiled();
                         } else
                             throw new RuntimeException(
                                     "Received an override-cancel-confirm event at the incorrect time");
                     }
                 });
         
         currentDriver.getView().registerForOverrideCancelDeny(new Observer() {
             /**
              * Announce the deny message, and return to the page the voter was
              * previously on
              */
             public void update(Observable o, Object arg) {
                 if (voting && override && !finishedVoting
                         && currentDriver != null) {
                     auditorium.announce(new OverrideCancelDenyEvent(mySerial,
                             nonce));
                     override = false;
                     currentDriver.getView().drawPage(pageBeforeOverride, false);
                 } else
                     throw new RuntimeException(
                             "Received an override-cancel-deny event at the incorrect time");
             }
         });
         
         currentDriver.getView().registerForOverrideCastConfirm(new Observer() {
             /**
              * Increment counters, and send the ballot in the confirm message.
              * Also kill votebox and show the inactive UI
              */
             public void update(Observable o, Object argTemp) {
                 if (voting && override && !finishedVoting
                         && currentDriver != null) {
                     ++publicCount;
                     ++protectedCount;
                     ListExpression arg = (ListExpression) argTemp;
                     //Object [] arg = (Object []) (((ListExpression) argTemp).toVerbatim());
 
                     // arg1 should be the cast ballot structure, check
                     if (Ballot.BALLOT_PATTERN.match((ASExpression) arg) == NoMatch.SINGLETON)
                         throw new RuntimeException(
                                 "Incorrectly expected a cast-ballot");
                     ListExpression ballot = (ListExpression) arg;
 
                     committedBallot = true;
 
                     auditorium.announce(new OverrideCastConfirmEvent(mySerial,
                             nonce, ballot.toVerbatim()));
 
 
 
 
                     try
                     {
                         if(!_constants.getEnableNIZKs()){
                             auditorium.announce(new CommitBallotEvent(mySerial,
                                     StringExpression.makeString(nonce),
                                     BallotEncrypter.SINGLETON.encrypt(ballot, _constants.getKeyStore().loadKey(mySerial + "-public")), StringExpression.makeString(bid), StringExpression.makeString(precinct)));
                         } else{
                             auditorium.announce(new CommitBallotEvent(mySerial,
                                     StringExpression.makeString(nonce),
                                     BallotEncrypter.SINGLETON.encryptWithProof(ballot, (List<List<String>>) arg.get(1), (PublicKey) _constants.getKeyStore().loadAdderKey("public")),
                                     StringExpression.makeString(bid), StringExpression.makeString(precinct))
                             );
                         }
                     }
                     catch (AuditoriumCryptoException e) {
                         Bugout.err("Crypto error trying to commit ballot: "+e.getMessage());
                         e.printStackTrace();
                     }
 
                     broadcastStatus();
 
 
                     List<List<String>> races = currentDriver.getBallotAdapter().getRaceGroups();
                     auditorium.announce(new BallotPrintingEvent(mySerial, bid,
                             nonce));
                     printer = new Printer(_currentBallotFile, races);
                     boolean success = printer.printCommittedBallot(ballot, bid);
                     printer.printedReceipt(bid);
 
                     //By this time, the voter is done voting
                     //Wait before returning to inactive
                     long start = System.currentTimeMillis();
                     while(System.currentTimeMillis() - start < 5000);
                     finishedVoting = true;
 
                     if(success)
                         auditorium.announce(new BallotPrintSuccessEvent(mySerial, bid, nonce));
                 } else
                     throw new RuntimeException(
                             "Received an override-cast-confirm event at the incorrect time");
             }
         });
         
         currentDriver.getView().registerForOverrideCastDeny(new Observer() {
             /**
              * Announce the deny message, and return to the page the voter was
              * previously on
              */
             public void update(Observable o, Object arg) {
                 if (voting && override && !finishedVoting
                         && currentDriver != null) {
                     auditorium.announce(new OverrideCastDenyEvent(mySerial,
                             nonce));
                     override = false;
                     currentDriver.getView().drawPage(pageBeforeOverride, false);
                 } else
                     throw new RuntimeException(
                             "Received an override-cast-deny event at the incorrect time");
             }
         });
     }
 
     /**
      * This method generates a short code for each vote on this machine, which will
      * eventually be sent over the wire to the controller, who will then publish the
      * short code - plaintext pairs and document the nonces generated here so that the
      * votes can be audited and verified
      *
      * @param ballot - the ballot containing all of the votes to be short coded
      *
      */
     private void shortCodes(ListExpression ballot, List<List<String>> raceGroups) {
         try{
             for(int i = 0; i < ballot.size(); i++){
                 ListExpression choice = (ListExpression)ballot.get(i);
                 System.err.println("The choice ListExpression is:\n" + choice);
                 //If this is who the vote selected in this race, make a short code
                 System.err.println("Attempting to convert " + choice.get(1) + " to AdderInteger.");
                 if(AdderInteger.fromASE(choice.get(1)) == AdderInteger.ONE){
 
                     ASExpression raceId = null;
 
                     for(List<String> race : raceGroups){
                         if(race.contains(choice.get(0)))
                             raceId = ASExpression.make(race.get(0));
                     }
 
                     if(raceId == null)
                         throw new RuntimeException("Found a vote with no race?");
 
 
                     ListExpression code =  new ListExpression(raceId, ASExpression.make(bid), ASExpression.makeVerbatim(nonce));
                     ASExpression shortCode = ASExpression.makeVerbatim(code.getSHA256());
 
                     VotePair pair = new VotePair(shortCode, choice);
 
                     //Pair this ballot's nonce with each of its short codes and votes
                     plaintextAuditCommits.put(ASExpression.makeVerbatim(nonce), pair);
 
 
                 }
             }//for
 
         } catch (InvalidVerbatimStreamException e) {
             throw new RuntimeException("Malformed nonce!", e);
         }
 
     }
 
 
     /**
      * Starts Auditorium, registers the listener, and connects to the network.
      */
     public void start() {
 
         inactiveUI = new VoteBoxInactiveUI(this);
         
         inactiveUI.setVisible(true);
 
         try {
             auditorium = new VoteBoxAuditoriumConnector(mySerial,
             		_constants,
                     ActivatedEvent.getMatcher(), AssignLabelEvent.getMatcher(),
                     AuthorizedToCastEvent.getMatcher(), BallotReceivedEvent.getMatcher(),
                     OverrideCancelEvent.getMatcher(), OverrideCastEvent.getMatcher(),
                     PollsOpenQEvent.getMatcher(), BallotCountedEvent.getMatcher(),
                     AuthorizedToCastWithNIZKsEvent.getMatcher(), PINEnteredEvent.getMatcher(),
                     InvalidPinEvent.getMatcher(), PollsOpenEvent.getMatcher(),
                     PollStatusEvent.getMatcher(), BallotPrintingEvent.getMatcher(),
                     BallotPrintSuccessEvent.getMatcher(), BallotPrintFailEvent.getMatcher(),
                     PollMachinesEvent.getMatcher(), ProvisionalAuthorizeEvent.getMatcher());
         } catch (NetworkException e1) {
         	//NetworkException represents a recoverable error
         	//  so just note it and continue
             System.out.println("Recoverable error occurred: "+e1.getMessage());
             e1.printStackTrace(System.err);
         }
 
         auditorium.addListener(new VoteBoxEventListener() {
 			public void ballotCounted(BallotCountedEvent e){
         		if (e.getNode() == mySerial
                         && Arrays.equals(e.getNonce(), nonce)) {
                     if (!finishedVoting)
                         throw new RuntimeException(
                                 "Someone said the ballot was counted, but this machine hasn't finished voting yet");
         		
 
                     currentDriver.getView().nextPage();
                     nonce = null;
                     voting = false;
                     finishedVoting = false;
                     committedBallot = false;
                     broadcastStatus();
                     killVBTimer = new Timer(_constants.getViewRestartTimeout(), new ActionListener() {
                     	public void actionPerformed(ActionEvent arg0) {
                     		currentDriver.kill();
                     		currentDriver = null;
                     		inactiveUI.setVisible(true);
                     		killVBTimer = null;
                             promptForPin("Enter Voting Authentication PIN");
                     	};
                     });
                     killVBTimer.setRepeats(false);
                     killVBTimer.start();
         		}//if
         	}
         	
             /**
              * Handler for the activated message. Look to see if this VoteBox's
              * status exists (and is correct), and if not, broadcast its status
              */
             public void activated(ActivatedEvent e) {
                 boolean found = false;
                 for (StatusEvent ae : e.getStatuses()) {
                     if (ae.getNode() == mySerial) {
                         VoteBoxEvent ve = (VoteBoxEvent) ae.getStatus();
                         VoteBoxEvent status = getStatus();
                         if (!ve.getStatus().equals(status.getStatus())
                                 || ve.getBattery() != status.getBattery()
                                 || ve.getLabel() != status.getLabel()
                                 || ve.getProtectedCount() != status
                                         .getProtectedCount()
                                 || ve.getPublicCount() != status
                                         .getPublicCount())
                             broadcastStatus();
                         found = true;
                     }
                 }
                 if (!found) broadcastStatus();
                 superSerial = e.getSerial();
                 superOnline = true;
             }
 
             /**
              * Handler for the assign-label message. If it is referring to this
              * booth, set the label.
              */
             public void assignLabel(AssignLabelEvent e) {
                 if (e.getNode() == mySerial){
                 	label = e.getLabel();
                 	System.out.println("\tNew Label: "+label);
                 }//if
                 
                 labelChangedEvent.notify(label);
             }
 
             /**
              * Handler for the authorized-to-cast message. If it is for this
              * booth, and it is not already voting, unzip the ballot and fire
              * the VoteBox runtime. Also announce the new status.
              */
             public void authorizedToCast(AuthorizedToCastEvent e) {
                 if (e.getNode() == mySerial) {
                     isProvisional = false;
 
                     if (voting || currentDriver != null && killVBTimer == null)
                         throw new RuntimeException(
                                 "VoteBox was authorized-to-cast, but was already voting");
 
                     // If last VB runtime is on thank you screen and counting
                     // down to when it disappears, kill it prematurely without
                     // showing inactive UI
                     if (killVBTimer != null && currentDriver != null) {
                         killVBTimer.stop();
                         killVBTimer = null;
                         currentDriver.kill();
                         currentDriver = null;
                     }
 
                     nonce = e.getNonce().toVerbatim();
 
                     //Current working directory
                     File path = new File(System.getProperty("user.dir"));
                     path = new File(path, "tmp");
                     path = new File(path, "ballots");
                     path = new File(path, "ballot" + protectedCount);
                     path.mkdirs();
 
                     bid = String.valueOf(rand.nextInt(Integer.MAX_VALUE));
                     precinct = e.getPrecinct();
                     
                     try {
                     	_currentBallotFile = new File(path, "ballot.zip");
                         staticCurrentBallotFile = _currentBallotFile;
 
                         FileOutputStream fout = new FileOutputStream(_currentBallotFile);
                         byte[] ballot = e.getBallot();
                         fout.write(ballot);
                         
                         Driver.unzip(new File(path, "ballot.zip").getAbsolutePath(), new File(path, "data").getAbsolutePath());
                         Driver.deleteRecursivelyOnExit(path.getAbsolutePath());
 
                         run(new File(path, "data").getAbsolutePath());
                         broadcastStatus();
                     } catch (IOException e1) {
                         throw new RuntimeException(e1);
                     }
                 }
             }
 
 
 
             /**
              * Handler for the ballot-received message. Show the next page on
              * the VB runtime (the thank you screen), and start a timer that
              * kills the runtime after a set amount of time (5 seconds), and
              * then shows the inactive screen. Also responds with its status.
              */
             public void ballotReceived(BallotReceivedEvent e) {
                 if (e.getNode() == mySerial
                         && Arrays.equals(e.getNonce(), nonce)) {
 
                     if (!committedBallot )
                         throw new RuntimeException(
                                 "Someone said the ballot was received, but this machine hasn't committed it yet. Maybe the supervisor is not configured properly?");
 
 
                     if(isProvisional)  {
                         try{
                             currentDriver.getView().drawPage(currentDriver.getView().getCurrentLayout().getProperties().getInteger(
                                 Properties.PROVISIONAL_SUCCESS_PAGE), false);
                         } catch (IncorrectTypeException e1) {
                             e1.printStackTrace();
                         }
                     } else{
                         currentDriver.getView().nextPage();
                     }
 
 
                     BallotEncrypter.SINGLETON.clear();
                     nonce = null;
                     voting = false;
                     finishedVoting = false;
                     committedBallot = false;
                     broadcastStatus();
                     killVBTimer = new Timer(_constants.getViewRestartTimeout(), new ActionListener() {
                         public void actionPerformed(ActionEvent arg0) {
                             currentDriver.kill();
                             currentDriver = null;
                             inactiveUI.setVisible(true);
                             killVBTimer = null;
                             promptForPin("Enter Voting Authentication PIN");
                         }
                     });
                     killVBTimer.setRepeats(false);
                     killVBTimer.start();
 
                 }
             }
 
             public void castCommittedBallot(CastCommittedBallotEvent e) {
                 // NO-OP
             }
                 
 
             /**
              * Increment the number of connections
              */
             public void joined(JoinEvent e) {
                 ++numConnections;
                 connected = true;
                 if(e.getSerial()==superSerial)
                     superOnline = true;
             }
 
             public void lastPollsOpen(LastPollsOpenEvent e) {
                 // NO-OP
             }
 
             /**
              * Decrement the number of connections
              */
             public void left(LeaveEvent e) {
                 --numConnections;
                 if (numConnections == 0) connected = false;
 
                 if(e.getSerial()==superSerial){
                     superOnline = false;
                 }
             }
 
             /**
              * Handler for the override-cancel message. If it is referring to
              * this booth, and it is in a state that it can be overridden, send
              * the runtime to the proper override page and record the page the
              * user was previously on.
              */
             public void overrideCancel(OverrideCancelEvent e) {
                 if (mySerial == e.getNode()
                         && Arrays.equals(e.getNonce(), nonce)) {
                     try {
                         if (voting && !finishedVoting && currentDriver != null) {
                             int page = currentDriver.getView().overrideCancel();
                             if (!override) {
                                 pageBeforeOverride = page;
                                 override = true;
                             }
                         } else
                             throw new RuntimeException(
                                     "Received an override-cancel message when the user wasn't voting");
                     } catch (IncorrectTypeException e1) {
                         Bugout.err("Incorrect type in overrideCancel handler");
                     }
                 }
             }
 
             public void overrideCancelConfirm(OverrideCancelConfirmEvent e) {
                 // NO-OP
             }
 
             public void overrideCancelDeny(OverrideCancelDenyEvent e) {
                 // NO-OP
             }
 
             /**
              * Handler for the override-cast message. If it is referring to this
              * booth, and it is in a state that it can be overridden, send the
              * runtime to the proper override page and record the page the user
              * was previously on.
              */
             public void overrideCast(OverrideCastEvent e) {
                 if(e.getNode() == mySerial){
                     try {
                         if (voting && !finishedVoting && currentDriver != null) {
                             int page = currentDriver.getView().overrideCast();
                             if (!override) {
                                 pageBeforeOverride = page;
                                 override = true;
                             }
                         } else
                             throw new RuntimeException(
                                     "Received an override-cast message when the user wasn't voting");
                     } catch (IncorrectTypeException e1) {
                         //We don't want to bail once VoteBox is up and running,
                         //  so report and continue in this case
                         System.out.println("Incorrect type received in overrideCast event: "+e1.getMessage());
                         e1.printStackTrace(System.err);
                     }
                 }
             }
 
             public void overrideCastConfirm(OverrideCastConfirmEvent e) {
                 // NO-OP
             }
 
             public void overrideCastDeny(OverrideCastDenyEvent e) {
                 // NO-OP
             }
 
             public void pollsClosed(PollsClosedEvent e) {
                 // NO-OP
             }
 
             /**
              * Handler for Polls Open. If not voting, booth prompts for pin
              */
             public void pollsOpen(PollsOpenEvent e) {
                 if(!voting){
                     promptForPin("Enter Authorization PIN");
                 }
             }
 
             /**
              * Handler for the polls-open? event. Searches the machine's log,
              * and replies with a last-polls-open message if an appropriate
              * polls-open message is found.
              */
             public void pollsOpenQ(PollsOpenQEvent e) {
                 if (e.getSerial() != mySerial) {
                     // TODO: Search the log and extract an appropriate
                     // polls-open message
 
                     ASExpression res = null;
                     if (res != null && res != NoMatch.SINGLETON) {
                         VoteBoxEventMatcher matcher = new VoteBoxEventMatcher(
                                 PollsOpenEvent.getMatcher());
                         PollsOpenEvent event = (PollsOpenEvent) matcher.match(
                                 0, res);
                         if (event != null
                                 && event.getKeyword().equals(e.getKeyword()))
                             auditorium.announce(new LastPollsOpenEvent(
                                     mySerial, event));
                     }
                 }
             }
 
             public void supervisor(SupervisorEvent e) {
                 // NO-OP
             }
 
             public void votebox(VoteBoxEvent e) {
                 // NO-OP
             }
 
             public void commitBallot(CommitBallotEvent e) {
                 // NO-OP
 
             }
 
 
             public void pinEntered(PINEnteredEvent e) {
                 // NO-OP
             }
 
             /**
              * Handles InvalidPinEvent and reprompts for PIN
              */
             public void invalidPin(InvalidPinEvent e) {
                 if(e.getNode() == mySerial)
                     promptForPin("Invalid PIN: Enter Valid PIN");
             }
 
             /**
              * received by VoteBox booth when it joins the network after a polls open event. PIN prompt is executed.
              */
             public void pollStatus(PollStatusEvent pollStatusEvent) {
                 System.out.println("Received Poll Status event and the polls are " + (pollStatusEvent.getPollStatus() == 1 ? "open":"closed"));
                 if(!voting && pollStatusEvent.getPollStatus() == 1){
                     promptForPin("Enter Authorization PIN");
                 }
             }
 
             public void ballotPrinting(BallotPrintingEvent ballotPrintingEvent) {
                 // NO-OP
             }
 
             /**
              * This indicates that the ballot was successfully printed and the voting session can safely end
              */
             public void ballotPrintSuccess(BallotPrintSuccessEvent e){
                 if (e.getBID() == bid
                         && Arrays.equals(e.getNonce(), nonce)) {
 
                     //This should never happen...
                     if (!finishedVoting)
                         throw new RuntimeException(
                                 "Someone said the ballot was printed, but this machine hasn't finished voting yet");
 
 
                     broadcastStatus();
 
                 }//if
 
             }
 
             public void ballotPrintFail(BallotPrintFailEvent e){
                 // Should implement something to indicate the print failed
             }
 
             public void uploadCastBallots(CastBallotUploadEvent castBallotUploadEvent) {}
 
 
             public void uploadChallengedBallots(ChallengedBallotUploadEvent challengedBallotUploadEvent) {}
 
             public void scannerStart(StartScannerEvent startScannerEvent) {
                 // NO-OP
             }
 
             /**
              * Used as an intermittent poll on the status of this booth through auditorium
              */
             public void pollMachines(PollMachinesEvent pollMachinesEvent) {
                 broadcastStatus();
                 try {
                     Thread.sleep(100);
                 } catch (InterruptedException e) {
                     e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                 }
             }
 
             public void spoilBallot(SpoilBallotEvent spoilBallotEvent) {
                 // NO-OP
             }
 
             public void announceProvisionalBallot(ProvisionalBallotEvent e) {
             }
 
             /**
              * Handler for ProvisionalAuthorizeEvent from supervisor. Generates ballot file path and stores ballot
              */
             public void provisionalAuthorizedToCast(ProvisionalAuthorizeEvent e) {
 
                 if (e.getNode() == mySerial) {
 
                     isProvisional = true;
 
                     if (voting || currentDriver != null && killVBTimer == null)
                         throw new RuntimeException(
                                 "VoteBox was authorized-to-cast, but was already voting");
 
                     // If last VB runtime is on thank you screen and counting
                     // down to when it disappears, kill it prematurely without
                     // showing inactive UI
                     if (killVBTimer != null && currentDriver != null) {
                         killVBTimer.stop();
                         killVBTimer = null;
                         currentDriver.kill();
                         currentDriver = null;
                     }
 
                     nonce = e.getNonce();
 
                     //Current working directory
                     File path = new File(System.getProperty("user.dir"));
                     path = new File(path, "tmp");
                     path = new File(path, "ballots");
                     path = new File(path, "ballot" + protectedCount);
                     path.mkdirs();
 
                     bid = String.valueOf(rand.nextInt(Integer.MAX_VALUE));
 
                     try {
                         _currentBallotFile = new File(path, "ballot.zip");
                         staticCurrentBallotFile = _currentBallotFile;
 
                         FileOutputStream fout = new FileOutputStream(_currentBallotFile);
                         byte[] ballot = e.getBallot();
                         fout.write(ballot);
 
                         Driver.unzip(new File(path, "ballot.zip").getAbsolutePath(), new File(path, "data").getAbsolutePath());
                         Driver.deleteRecursivelyOnExit(path.getAbsolutePath());
 
 
                         run(new File(path, "data").getAbsolutePath());
                         broadcastStatus();
                     } catch (IOException e1) {
                         throw new RuntimeException(e1);
                     }
                 }
 
             }
 
             public void provisionalCommitBallot(ProvisionalCommitEvent provisionalCommitEvent) {
                 // NO-OP
             }
 
             public void authorizedToCastWithNIZKS(AuthorizedToCastWithNIZKsEvent e) {
                 // NO-OP
             }
 
             public void tapMachine(TapMachineEvent tapMachineEvent) {
                 // NO-OP
             }
 
 
             public void ballotScanned(BallotScannedEvent e) {
                 // NO-OP
             }
 
             public void ballotScanner(BallotScannerEvent e) {
                 // NO-OP
             }
 
             public void ballotAccepted(BallotScanAcceptedEvent e){
                 // NO-OP
             }
 
             public void ballotRejected(BallotScanRejectedEvent e){
                 // NO-OP
             }
 
         });
 
         try {
             auditorium.connect();
             auditorium.announce(getStatus());
         }
         catch (NetworkException e1) {
             throw new RuntimeException(e1);
         }
 
         statusTimer.start();
     }
 
     /**
      * Initializes a GUI dialog through which the user enters their assigned pin. Returns with no action if
      * already prompting for PIN.
      *
      * @param message message displayed as the header of the PIN prompt e.g. "Please Enter Your PIN"
      */
     public void promptForPin(String message) {
         if(promptingForPin){
             System.out.println("Still prompting for PIN!");
             return;
         }
         /*//if(!superOnline) return;
         promptingForPin = true;
         JTextField limitedField = new JTextField(new PlainDocument() {
             private int limit=4;
             public void insertString(int offs, String str, AttributeSet attr) throws BadLocationException {
                 if(str == null)
                     return;
                 if((getLength() + str.length()) <= this.limit) {
                     super.insertString(offs, str, attr);
                 }
             }
         }, "", 5);
 
         Object[] msg = {
                 message, limitedField
         };
         int pinResult = JOptionPane.showConfirmDialog(
                 (JFrame)inactiveUI,
                 msg,
                 "Authorization Required",
                 JOptionPane.OK_CANCEL_OPTION,
                 JOptionPane.PLAIN_MESSAGE);
 
         while(pinResult != JOptionPane.OK_OPTION) {
             pinResult = JOptionPane.showConfirmDialog(
                     (JFrame)inactiveUI,
                     msg,
                     "Authorization Required",
                     JOptionPane.OK_CANCEL_OPTION,
                     JOptionPane.PLAIN_MESSAGE);
         }
 
 
         try{
             String pin = limitedField.getText();
             validatePin(pin);
         }catch(NumberFormatException nfe){
             promptingForPin = false;
             promptForPin("Invalid PIN: Enter 4-digit PIN");
         }*/
 
         // Create a new GUI to prompt for PIN.
         final PINAuthorizationGUI pinGUI = new PINAuthorizationGUI(_constants.getScreenCenterX(), _constants.getScreenCenterY());
         // Set the GUI's label to our message.
         pinGUI.setLabelText(message);
         // Add a listener for the OK button so that when it gets clicked, it validates the pin.
         pinGUI.okButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent arg0) {
                 validatePin(pinGUI.getPin());
                 pinGUI.stop();
             }
         });
         // Add a listener for the pin text field so that when the Enter key gets pressed, it validates the pin.
         pinGUI.pinTextField.addKeyListener(new KeyAdapter() {
             @Override
             public void keyPressed(KeyEvent arg0) {
                 if(arg0.getKeyCode() == KeyEvent.VK_ENTER)
                 {
                     validatePin(pinGUI.getPin());
                     pinGUI.stop();
                 }
             }
         });
         pinGUI.start();
 
         promptingForPin = false;
     }
 
     /**
      * Generates a new PINEnteredEvent and sends over the network for validation by supervisor.
      * @param pin 4-digit, decimal PIN to be validated
      */
     public void validatePin(String pin) {
         byte[] pinNonce = new byte[256];
         for (int i = 0; i < 256; i++)
             pinNonce[i] = (byte) (Math.random() * 256);
         this.pinNonce = pinNonce;
         auditorium.announce(new PINEnteredEvent(mySerial, pin, pinNonce));
     }
 
     /**
      * Renders images for write-in candidates. It gets candidate name(s) from the write-in
      * prompt, along with the corresponding uid (to get the filename) and type (so that this
      * method knows how many names to read), and uses the names to render ballot images for
      * the write-in candidate that contain his/her name.
      *
      * @param uid the uid of the candidate
      * @param type the type of the candidate ("Regular" or "Presidential")
      * @param names the name(s) of the candidate
      */
     public static void renderWriteInImages(String uid, String type, String... names)
     {
         System.out.println("Got name(s) for a " + type + " candidate with uid " + uid + ".\nName of Candidate: " + names[0]);
         if (type.equals("Presidential"))
         {
             System.out.println("Name of Vice-Candidate: " + names[1]);
         }
 
         // Render the images for the candidate names.
         BufferedImage writeInToggleButton = RenderingUtils.renderToggleButton(names[0], type.equals("Presidential") ? names[1] : "", "", 20, 600, true, false, false);
         BufferedImage focusedWriteInToggleButton = RenderingUtils.renderToggleButton(names[0], type.equals("Presidential") ? names[1] : "", "", 20, 600, true, false, true);
         BufferedImage selectedWriteInToggleButton = RenderingUtils.renderToggleButton(names[0], type.equals("Presidential") ? names[1] : "", "", 20, 600, true, true, false);
         BufferedImage focusedSelectedWriteInToggleButton = RenderingUtils.renderToggleButton(names[0], type.equals("Presidential") ? names[1] : "", "", 20, 600, true, true, true);
 
         // Save the images to files, overwriting the previous write-in candidate images for this UID.
         String fileSeparator = System.getProperty("file.separator");
         System.out.println("Static ballot file path: " + staticCurrentBallotFile.getAbsolutePath());
         String filePath = staticCurrentBallotFile.getAbsolutePath().substring(0, staticCurrentBallotFile.getAbsolutePath().lastIndexOf(fileSeparator) + 1) + "data" + fileSeparator + "media" + fileSeparator;
         System.out.println("Images being saved to " + filePath);
         for (Language language : Language.getAllLanguages())
         {
             try
             {
                 ImageIO.write(writeInToggleButton, "png", new File(filePath + uid + "_1_" + language.getShortName() + ".png"));
                 ImageIO.write(focusedWriteInToggleButton, "png", new File(filePath + uid + "_focused_1_" + language.getShortName() + ".png"));
                 ImageIO.write(selectedWriteInToggleButton, "png", new File(filePath + uid + "_selected_1_" + language.getShortName() + ".png"));
                 ImageIO.write(focusedSelectedWriteInToggleButton, "png", new File(filePath + uid + "_focusedSelected_1_" + language.getShortName() + ".png"));
             }
             catch (IOException e)
             {
                 System.err.println("Unable to render write-in candidate images!");
                 return;
             }
         }
 
         // Invalidate the view to force a repaint.
 
 
         // Create a ReviewButton and a PrintButton.
         //ReviewButton review = new ReviewButton(tb.getUID() + "_review", tb.getBothLines(), "GoToPage", getSizeVisitor());
         //PrintButton pb = new PrintButton(tb.getUID() + "_printable", tb.getText(), getSizeVisitor());
     }
 
     /**
      * A getter method to send the BallotFile to the printer
      *
      * @return _currentBallotFile  - the current ballot
      */
      public File getCurrentBallotFile(){
          return _currentBallotFile;
      }
     /**
      * Main entry point into the program. If an argument is given, it will be
      * the serial number, otherwise VoteBox will load a serial from its config file.
      * 
      * @param args
      */
     public static void main(String[] args) {
         if (args.length == 1)
             new VoteBox(Integer.parseInt(args[0])).start();
         else
         	//Tell VoteBox to refer to its config file for the serial number
             new VoteBox().start();
     }
 }
