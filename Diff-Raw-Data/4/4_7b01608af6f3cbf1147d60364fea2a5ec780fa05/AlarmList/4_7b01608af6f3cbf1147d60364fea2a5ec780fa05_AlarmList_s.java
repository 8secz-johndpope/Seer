 package se.chalmers.dat255.sleepfighter.model;
 
 import java.util.ArrayList;
 import java.util.BitSet;
 import java.util.List;
 
 import se.chalmers.dat255.sleepfighter.utils.collect.ObservableList;
 import se.chalmers.dat255.sleepfighter.utils.message.Message;
 import se.chalmers.dat255.sleepfighter.utils.message.MessageBus;
 
 import com.badlogic.gdx.utils.IntArray;
 
 /**
  * Manages all the existing alarms.
  *
  * @author Centril<twingoow@gmail.com> / Mazdak Farrokhzad.
  * @version 1.0
  * @since Sep 18, 2013
  */
 public class AlarmList extends ObservableList<Alarm> {
 	/**
 	 * Constructs the manager with no initial alarms.
 	 */	
 	public AlarmList() {
 		this( new ArrayList<Alarm>() );
 	}
 
 	/**
 	 * Constructs the manager starting with given alarms.
 	 *
 	 * @param alarms list of given alarms. Don't modify this list directly.
 	 */
 	public AlarmList( List<Alarm> alarms ) {
 		this.setDelegate( alarms );
 	}
 
 	/**
 	 * Sets the list of alarms.
 	 *
 	 * @param delegate list of given alarms. Don't modify this list directly.
 	 */
 	@Override
 	public void setDelegate( List<Alarm> delegate ) {
 		super.setDelegate( delegate );
 	}
 
 	@Override
 	protected void fireEvent( Event e ) {
 		// Intercept add/update events and inject message bus.
 		if ( e.operation() == Operation.ADD ) {
 			for ( Object obj : e.elements() ) {
 				((Alarm) obj).setMessageBus( this.getMessageBus() );
 			}
 		} else if ( e.operation() == Operation.UPDATE ) {
 			this.get( e.index() ).setMessageBus( this.getMessageBus() );
 		}
 
 		super.fireEvent( e );
 	}
 
 	/**
 	 * <p>Finds the lowest unnamed placement number.</p>
 	 *
<<<<<<< HEAD
=======
 	 * <p>Time complexity: O(n),<br/>
 	 * Space complexity: O(n).</p>
 	 *
>>>>>>> Fixed findLowestUnnamedPlacement() and provided test.
 	 * @see Alarm#getUnnamedPlacement()
 	 * @return the lowest unnamed placement number.
 	 */
 	public int findLowestUnnamedPlacement() {
 		// Bail early if we can.
 		if ( this.size() == 0 ) {
 			return 1;
 		}
 
 		// First extract the unnamed placements defined.
 		IntArray arr = new IntArray(); 
 		arr.ordered = false;
 		for ( Alarm alarm : this ) {
 			if ( alarm.isUnnamed() ) {
 				int place = alarm.getUnnamedPlacement();
 				if ( place > 0 ) {
 					arr.add( place );
 				}
 			}
 		}
 
 		// Another opportunity to bail.
 		if ( arr.size == 0 ) {
 			return 1;
 		}
 
 		arr.shrink();
 
 		// Set all bits < N
 		BitSet bits = new BitSet(arr.size);
 		for ( int i = 0; i < arr.size; ++i ) {
 			int v = arr.get( i ) - 1;
 			if ( v < arr.size ) {
 				bits.set( v );
 			}
 		}
 
 		// Find first false bit.
 		return bits.nextClearBit( 0 ) + 1;
 	}
 
 	/**
 	 * Sets the message bus, if not set, no events will be received.
 	 *
 	 * @param messageBus the buss that receives events.
 	 */
 	public void setMessageBus( MessageBus<Message> messageBus ) {
 		super.setMessageBus( messageBus );
 
 		for ( Alarm alarm : this ) {
 			alarm.setMessageBus( messageBus );
 		}
 	}
 
 	/**
 	 * Returns info about the earliest alarm.<br/>
 	 * The info contains info about milliseconds and the alarm.
 	 *
 	 * @param now current time in unix epoch timestamp.
 	 * @return info about the earliest alarm. 
 	 */
 	public AlarmTimestamp getEarliestAlarm( long now ) {
 		Long millis = null;
 		int earliestIndex = -1;
 
 		for ( int i = 0; i < this.size(); i++ ) {
 			Long currMillis = this.get( i ).getNextMillis( now );
 			if ( currMillis != Alarm.NEXT_NON_REAL && (millis == Alarm.NEXT_NON_REAL || millis > currMillis) ) {
 				earliestIndex = i;
 				millis = currMillis;
 			}
 		}
 
 		return earliestIndex == -1 ? AlarmTimestamp.INVALID : new AlarmTimestamp( millis, this.get( earliestIndex) );
 	}
 	
 	/**
 	 * Returns an the alarm with the unique id provided.
 	 * 
 	 * @param id the unique id of the alarm.
 	 * @return the alarm, if not found it returns null.
 	 */
 	public Alarm getById(int id) {
 		for (int i = 0; i < size(); i++) {
 			if (get(i).getId() == id) {
 				return get(i);
 			}
 		}
 		return null;
 	}
 }
