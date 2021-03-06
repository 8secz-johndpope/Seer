 package se.ltu.M7017E.lab2.client;
 
 import java.io.BufferedReader;
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileReader;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.NoSuchElementException;
 import java.util.Set;
 import java.util.TreeSet;
 
 import javax.swing.JOptionPane;
 
 import lombok.Getter;
 import lombok.Setter;
 
 import org.gstreamer.Gst;
 
 import se.ltu.M7017E.lab2.client.audio.ReceiverPipeline;
 import se.ltu.M7017E.lab2.client.audio.SenderPipeline;
 import se.ltu.M7017E.lab2.client.ui.Gui;
 import se.ltu.M7017E.lab2.common.Room;
 import se.ltu.M7017E.lab2.common.messages.AnswerCall;
 import se.ltu.M7017E.lab2.common.messages.Call;
 import se.ltu.M7017E.lab2.common.messages.ConnectedList;
 import se.ltu.M7017E.lab2.common.messages.Hello;
 import se.ltu.M7017E.lab2.common.messages.Joined;
 import se.ltu.M7017E.lab2.common.messages.Left;
 import se.ltu.M7017E.lab2.common.messages.ListMsg;
 import se.ltu.M7017E.lab2.common.messages.StopCall;
 
 @Getter
 public class App {
 	private ControlChannel control;
 	private Set<String> contacts = new TreeSet<String>();
 	private Set<String> connected = new HashSet<String>();
 	private String username;
 	private String friend;
 
 	@Setter
 	private Gui gui;
 
 	@Getter
 	private List<Room> allRooms = new LinkedList<Room>();
 	private List<Room> myRooms = new LinkedList<Room>();
 	@Setter
 	@Getter
 	private boolean serverIsWriting = false;
 	private List<String> msgFromServer = new LinkedList<String>();
 
 	private ReceiverPipeline receiver;
 	private SenderPipeline sender;
 
 	public App() {
 		// ######### BUSINESS LOGIC ############
 		fetchUsername();
 		fetchContacts();
 
 		// ######### COMMUNICATION WITH SERVER ###########
 		control = new ControlChannel(this);
 		new Thread(control).start();
 
 		// say hello :)
 		control.send(new Hello(this.username).toString());
 
 		// ############ GSTREAMER STUFF ###############
 		Gst.init("Audioconferencing", new String[] { "--gst-debug-level=3",
 				"--gst-debug-no-color" });
 		receiver = new ReceiverPipeline();
 		receiver.play();
 
 		sender = new SenderPipeline();
 	}
 
 	public void msg(Joined joined) {
 		// TODO: update tree
 	}
 
 	public void msg(Left left) {
 		// TODO: update tree
 	}
 
 	public void msg(StopCall stopCall) {
 		receiver.stopUnicastReceiving();
 		sender.stopStreamingToUnicast();
 	}
 
 	public void joinRoom(int roomId) {
 		long mySSRC = sender.streamTo(roomId);
 		Room newRoom = new Room();
 		receiver.receiveFromRoom(roomId, mySSRC);
 		// TODO: connect sender too
 		try {
 			getControl().send("JOIN," + roomId);
 			control.getRoomsListFinished().acquire();
 			newRoom = updateAfterJoin(control.getUpdatedAudience());
 			for (Room oldRoom : allRooms) {
 				if (oldRoom.getId() == newRoom.getId()) {
 					allRooms.set(allRooms.indexOf(oldRoom), newRoom);
 				}
 			}
 		} catch (InterruptedException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 	}
 
 	public void leaveRoom(int roomId) {
 		sender.stopStreamingToRoom(roomId);
 		receiver.stopRoomReceiving(roomId);
 	}
 
 	public void fetchUsername() {
 		/*
 		 * algo: check if file exists, then check if there is a name inside. If
 		 * any of these two tests fail: ask for a name
 		 */
 		File file = new File("name.txt");
 		if (file.exists()) {
 			try {
 				FileReader fr = new FileReader(file);
 				BufferedReader br = new BufferedReader(fr);
 				br = new BufferedReader(fr);
 				String s = br.readLine();
 				System.out.println("hello" + s);
 				if (s != null && !s.isEmpty()) {
 					this.username = s;
 					fr.close();
 					return;
 				}
 				fr.close();
 			} catch (IOException e) {
 				System.err
 						.println("Got a problem while fetching username from file");
 				e.printStackTrace();
 			}
 		}
 
 		// fetching failed, ask for a name
 		String name;
 		do {
 			name = JOptionPane.showInputDialog(null, "Choose a name",
 					"Name selection", JOptionPane.QUESTION_MESSAGE);
 		} while (name == null);
 		this.username = name;
 
 		// save it
 		try {
 			FileWriter myFile = new FileWriter("name.txt");
 			myFile.write(name);
 			myFile.close();
 		} catch (IOException e) {
 			System.err.println("Got a problem while saving username to file");
 		}
 	}
 
 	/**
 	 * send a message to the server to try to call someone
 	 */
 	public void askToCall(String receiver) {
 
 		if (receiver.endsWith("(Disconnected)")) {
 			receiver = receiver.substring(0, receiver.length() - 15);
 		}
 		int port = this.receiver.receiveFromUnicast();
 		control.send(new Call(username, receiver, "0", port).toString());
 		this.friend = receiver;
 	}
 
 	public void call(String ipReceiver, int port) {
 		System.out.println("IP: " + ipReceiver);
 		sender.streamTo(ipReceiver, port);
 	}
 
 	/**
 	 * hang up. Stop the pipelines and send a message to the server for telling
 	 * the other client the call is finished
 	 */
 	public void askToStopCall() {
 		stopCall();
 
 		StopCall stop = new StopCall();
 		stop.setReceiver(friend);
 		control.send(stop.toString());
 	}
 
 	/**
 	 * Stop streaming from/to friend.
 	 */
 	public void stopCall() {
 		// stop streaming from friend
 		receiver.stopUnicastReceiving();
 		// stop streaming to friend
 		sender.stopStreamingToUnicast();
 
 	}
 
 	/**
 	 * send an AnswerCall message to the server which transmit to the other
 	 * client. Used to tell if we accept or refuse the call
 	 * 
 	 * @param String
 	 *            answer : "yes" or "no"
 	 * @param Call
 	 *            call : the Call message received from the server
 	 */
 	public void answerCall(String answer, Call call) {
		int port = receiver.receiveFromUnicast();
 
 		if (answer.equals("yes")) {
 			this.friend = call.getSender();
 			System.out.println("My friend is the wonderful " + this.friend);
 		}
 
 		control.send(new AnswerCall(port, call.getSender(), call.getReceiver(),
 				answer, "0").toString());
 	}
 
 	/**
 	 * Fetch all the contacts from the file.
 	 */
 	private void fetchContacts() {
 		File file = new File("contacts.txt");
 		if (!file.exists()) // check if the file already exist, else the
 							// file is created
 			try {
 				file.createNewFile();
 			} catch (IOException e1) {
 				e1.printStackTrace();
 			}
 
 		String s;
 		try {
 			FileReader fr = new FileReader("contacts.txt");
 			BufferedReader br = new BufferedReader(fr);
 			while ((s = br.readLine()) != null) {
 				contacts.add(s);
 			}
 			fr.close();
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 	}
 
 	/**
 	 * Save the contact List
 	 */
 	private void saveContacts() {
 		FileWriter file;
 		try {
 			file = new FileWriter("contacts.txt");
 			BufferedWriter out = new BufferedWriter(file);
 			for (String contact : contacts) {
 				out.write(contact);
 				out.newLine();
 			}
 			out.close();
 			file.close();
 		} catch (IOException e) {
 			System.err.println("Got a problem while saving contacts list");
 			e.printStackTrace();
 		}
 	}
 
 	/**
 	 * Add a contact in the list of contacts
 	 */
 	public void addContact(String contact) {
 		this.contacts.add(contact);
 		saveContacts();
 	}
 
 	/**
 	 * Remove a contact from the list of contacts.
 	 */
 	public void removeContact(String contact) {
 		if (username.endsWith("(Disconnected)")) {
 			username = username.substring(0, username.length() - 15);
 		}
 
 		this.contacts.remove(username);
 		saveContacts();
 	}
 
 	/**
 	 * Set the people connected to the server list with the ConnectedMessage
 	 * received from the server
 	 */
 	public void setConnected(String connectedMessage) {
 		ConnectedList connectedList = ConnectedList
 				.fromString(connectedMessage);
 		connected = connectedList.getConnected();
 		String deleteme = "List of connected persons : ";
 		for (String c : connected) {
 			deleteme += "\n " + c;
 		}
 
 		System.out.println(deleteme);
 		if (gui != null) {
 			System.out.println("plooouf");
 			gui.refreshContactsList();
 		}
 	}
 
 	/**
 	 * Initiate the allRooms global variable, which contains all the rooms where
 	 * there is at least one person
 	 */
 	public void createAllRoomList() {
 		allRooms.clear();
 		boolean server = true;
 
 		if (server) {
 			try {
 				// ask the server for the list
 				getControl().send(new ListMsg().toString());
 				// wait for the semaphore
 				control.getRoomsListFinished().acquire();
 				System.out.println(control.getMsgList());
 				updateMsgFromServer(control.getMsgList());
 				for (String oneRoom : msgFromServer) {
 					Room newRoom = new Room();
 					Set<String> roomContactList = new TreeSet<String>();
 					String split[] = oneRoom.split(",", 0);
 
 					newRoom.setId(Integer.parseInt(split[1]));
 					for (int i = 2; i < split.length; i++) {
 						roomContactList.add(split[i]);
 					}
 					newRoom.setAudience(roomContactList);
 					allRooms.add(newRoom);
 				}
 			} catch (InterruptedException e) {
 				e.printStackTrace();
 				System.out.println("Error when the thread is waiting");
 			} catch (NoSuchElementException e1) {
 				e1.printStackTrace();
 				System.out.println("End of the Server message");
 			} catch (NumberFormatException n) {
 				n.printStackTrace();
 				System.out.println("NumberFormatException for parseint");
 			}
 			// Debug : create the list when the server is down
 		} else {
 			for (int i = 0; i < 3; i++) {
 				Room newRoom = new Room();
 				Set<String> roomContactList = new TreeSet<String>();
 				newRoom.setId(i);
 				if (i < 2) {
 					for (int j = 0; j < 2; j++) {
 						roomContactList.add("Contact " + i);
 					}
 				} else {
 					roomContactList.add("Contact 1");
 				}
 				newRoom.setAudience(roomContactList);
 				allRooms.add(newRoom);
 			}
 		}
 	}
 
 	/**
 	 * Update a room when someone joined it.
 	 * 
 	 * @param newAudience
 	 *            the new contact list in the room joined.
 	 * @return the new state of the room joined
 	 */
 	public Room updateAfterJoin(String newAudience) {
 		Room updatedRoom = new Room();
 		Set<String> updatedContactList = new TreeSet<String>();
 		String splitnewAudience[] = newAudience.split(",", 0);
 
 		updatedRoom.setId(Integer.parseInt(splitnewAudience[1]));
 		for (int i = 2; i < splitnewAudience.length; i++) {
 			String string = splitnewAudience[i];
 			updatedContactList.add(string);
 		}
 		updatedRoom.setAudience(updatedContactList);
 		return updatedRoom;
 	}
 
 	/**
 	 * Update the message received from the server when asking for a list of
 	 * rooms.
 	 * 
 	 * @param CloneList
 	 *            the message List from the server
 	 */
 	public void updateMsgFromServer(List<String> CloneList) {
 		msgFromServer.clear();
 		for (String string : CloneList) {
 			msgFromServer.add(string);
 		}
 	}
 
 	/**
 	 * Update the list which contains all the room the user is in.
 	 * 
 	 * @param allRoomList
 	 *            the list with all the Rooms currently in use
 	 */
 	public void createMyRooms(List<Room> allRoomList) {
 		Room mynewRoom;
 		myRooms.clear();
 		for (Room rooms : allRoomList) {
 			if (rooms.getAudienceAsStrings().contains(username)) {
 				mynewRoom = new Room();
 				mynewRoom.setAudience(rooms.getAudience());
 				mynewRoom.setId(rooms.getId());
 				myRooms.add(mynewRoom);
 			}
 		}
 	}
 }
