 package final_project.control;
 
 import java.util.*;
 import final_project.model.*;
 import final_project.model.store.*;
 import final_project.model.DERound.NoSuchMatchException;
 import final_project.input.*;
 
 //Created in the main method.
 public class TournamentController implements Constants{
 
 	private Collection<EventController> _events;
 	private IDataStore _dataStore;
 	private int _currentEventID;
 	private StripController _stripController;
 	private SMSController _smsController;
 	private DataFormattingHelper _dataHelper;
 
 	public TournamentController(String username, String password, ITournamentInfo info) {
 		_currentEventID = 0;
 		_events = new LinkedList<EventController>();
 		_dataStore = info.getDataStore();
 		_stripController = new StripController();
 
         for (IEventInfo e : info.getEvents())
             addEvent(e.getWeaponType(),e.getPreregs());
 
 		_dataHelper = new DataFormattingHelper(_dataStore);
 		_smsController = new SMSController(_dataStore, this, username, password); 
 	}
 
 	public int addEvent(String weapon){
         int id = ++_currentEventID;
 		_events.add(new EventController(id, _dataStore, weapon));
         return id;
 	}
 
 	public int addEvent(String weapon, Collection<Integer> preregs){
         int id = ++_currentEventID;
 		_events.add(new EventController(id, _dataStore, weapon, preregs));
         return id;
 	}
 
 	public int[] getStripSizes(int eventID) {
 		if(_events.isEmpty())
 			throw new IllegalStateException("No event created.");
 
 		for (EventController e: _events)
 			return e.getStripArrangement(); //This works because we only care about the first one
 
 		return null;
 	}
 
 	public void setStripSizes(int eventID, int stripRows, int stripCols) {
 		if(_events.isEmpty())
 			throw new IllegalStateException("No event created.");
 
 		for (EventController e: _events)
 			e.setStripArrangement(stripRows, stripCols);
 	}
 
 	public void startPoolRound(int eventID, int poolSize) throws IllegalStateException{
 		Iterator<EventController> iter = _events.iterator();
 		if(iter.hasNext()){
 			if(!iter.next().startPoolRound(poolSize)){
 				throw new IllegalStateException("Not correct time to create pool round.");
 			}
 		}
 		throw new IllegalStateException("No event created.");
 	}
 
 	public Result[] getDEMatches(int eventID){
 		Iterator<EventController> iter = _events.iterator();
 		if(iter.hasNext()){
 			return iter.next().getDEMatches();
 		}
 		throw new IllegalStateException("No event created.");
 	}
 
 	//in progress
 	public void startDERound(int eventID, double cut) throws IllegalStateException{
 		Iterator<EventController> iter = _events.iterator();
 		if(iter.hasNext()){
 			iter.next().startDERound(cut);
 		}
 		throw new IllegalStateException("No event created.");
 	}
 
 	public Collection<PoolSizeInfo> getValidPoolSizes(int eventID){
 		Iterator<EventController> iter = _events.iterator();
 		if(iter.hasNext()){
 			return iter.next().getValidPoolSizes();
 		}
 		throw new IllegalStateException("No event created.");
 	}
 
 	/**
 	 *Adds the given player to the given event.
 	 * @param eventID
 	 * @param playerID
 	 * @return a boolean, true if player was added, false otherwise.
 	 */
 	public boolean addPlayer(int eventID, int playerID){
 		Iterator<EventController> iter = _events.iterator();
 		if(iter.hasNext()){
 			iter.next().addPlayer(playerID);
 			return true;
 		}
 		return false;
 	}
 
 	/**
 	 * Takes a completed bout and adds it to appropriate event.
 	 * @param bout A completed bout to be added to results
 	 * @param stripID The number of the strip the bout was fenced on
 	 */
 	public void addCompletedResult(CompleteResult result, int ref) {
 		for(EventController event : _events){
 			if(event.hasRef(ref)){
 				try {
 					event.addCompletedResult(result);
 					_smsController.sendMessage("Result successfully submitted!", _dataStore.getReferee(ref).getPhoneNumber());
 					return;
 				} catch (NoSuchMatchException e) {
 					_smsController.sendMessage("Error: result could not be processed", _dataStore.getReferee(ref).getPhoneNumber());
 					e.printStackTrace();
 				}
 			}
 		}
 	}
 
 	/**
 	 * Method called by the SMSController when two refs need to be
 	 * swapped.
 	 * @param oldRefID
 	 * @param newRefID
 	 */
 	public void swapRef(int eventID, int oldRefID, int newRefID) {
 		//TODO: empty stub
 	}
 
 	/* METHODS TO MAKE IT POSSIBLE FOR THE GUI TO GET INFORMATION FROM THE DATA STORE */
 	public Object[][] checkInFencer(int playerID, boolean checkAs) {
 		final IPlayer temp = _dataStore.getPlayer(playerID).setCheckedIn(checkAs);
 		_dataStore.runTransaction(new Runnable(){
 			public void run(){
 				_dataStore.putData(temp);
 			}
 		});
 		return _dataHelper.giveSignInPanelInfo();
 	}
 
 	//Checks in all players as the boolean parameter
 	public Object[][] checkInAll(boolean checkAs) {
 		System.out.println("check in all called");
 		for (IPlayer i: _dataStore.getPlayers()){
 			final IPlayer temp = i.setCheckedIn(checkAs);
 			System.out.println("Player " + temp.getID() + " checked in? " + temp.getCheckedIn());
 			_dataStore.runTransaction(new Runnable(){
 				public void run(){
 					_dataStore.putData(temp);
 				}
 			});
 		}
 		return _dataHelper.giveSignInPanelInfo();
 	}
 
 	public Object[][] giveSubscriberTableInfo() {
 		return _dataHelper.giveSubscriberTableInfo();
 	}
 
 	public Object[][] giveSignInPanelInfo() {
 		return _dataHelper.giveSignInPanelInfo();
 	}
 
 	public Object[][] registerSpectator(String number, String firstName, String lastName) {		
 		final IPerson temp =  _dataStore.createSpectator(number, firstName, lastName, "", "Spectator");
 		_dataStore.runTransaction(new Runnable(){
 			public void run(){
 				_dataStore.putData(temp);
 			}
 		});
 		//Should text new spectator that they were registered, so they'll have a phone number to respond to
 		//_smsController.sendMessage("Thank you for registering with our system! Please respond " +
 			 //"to this phone number with your follower requests.", number);
 		return _dataHelper.giveSubscriberTableInfo();
 	}
 
 	public Object[][] registerAndCheckInFencer(String number, String firstName, String lastName, int rank) {
 		final IPlayer p = _dataStore.createPlayer(number, firstName, lastName, "", "Fencer", rank).setCheckedIn(true);
 		_dataStore.runTransaction(new Runnable(){
 			public void run(){
 				_dataStore.putData(p);
 			}
 		});
 		return _dataHelper.giveSignInPanelInfo();
 	}
 
 	public void registerFencer(String number, String firstName, String lastName, int rank) {
 		//Concatenating a 1 to the beginning of the phone number
 		number = "1" + number;
 		final IPlayer p = _dataStore.createPlayer(number, firstName, lastName, "", "Fencer", rank);
 		_dataStore.runTransaction(new Runnable(){
 			public void run(){
 				_dataStore.putData(p);
 			}
 		});
 	}
 
     public Collection<PoolSizeInfo> getValidPoolSizes() {
         if (!_events.isEmpty())
             return _events.iterator().next().getValidPoolSizes();
         return new ArrayList<PoolSizeInfo>();
     }
 
 	public Object[][] getPoolSizeInfoTable() {
 		int[] stripSizes = this.getStripSizes(EVENT_ID);
 		Object[][] toReturn = _dataHelper.getPoolSizeInfoTable(stripSizes[0], stripSizes[1]);
 		return toReturn;
 	}
 	
 	public String getNameFromId(int playerId) {
 		return _dataStore.getPlayer(playerId).getFirstName() + " " + _dataStore.getPlayer(playerId).getLastName();
 	}
 
 	public Object[][] getPoolRefListTable(int id) {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	public Collection<Pool> getPools(int eventId) {
 		// TODO Auto-generated method stub
		return null;
 	}
 }
