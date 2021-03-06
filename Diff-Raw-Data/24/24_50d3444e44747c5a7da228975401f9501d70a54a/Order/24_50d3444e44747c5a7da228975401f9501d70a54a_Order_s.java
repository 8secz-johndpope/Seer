 package com.vendsy.bartsy.venue.model;
 
 import java.text.DateFormat;
 import java.text.DecimalFormat;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.Locale;
 import java.util.TimeZone;
 
 import org.json.JSONException;
 import org.json.JSONObject;
 
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.Button;
 import android.widget.ImageView;
 import android.widget.LinearLayout;
 import android.widget.TextView;
 
 import com.vendsy.bartsy.venue.R;
 import com.vendsy.bartsy.venue.utils.Utilities;
 import com.vendsy.bartsy.venue.utils.WebServices;
 
 public class Order  {
 
 	static final String TAG = "Order";
 	
 	// Each order has an ID that is unique within a session number
 	public String serverID; 
 	
 	// Title and description are arbitrary strings
 	public String title, description;
 	public String itemId;
 	
 	// The total price is in the local denomination and is the sum of price * quantity, fee and tax
 	public int quantity = 1;
 	public int image_resource;
 	
 	// Fees: total = base + tax + fee + tip
 	public Float baseAmount;
 	public Float feeAmount;
 	public Float taxAmount;
 	public Float tipAmount;
 	public Float totalAmount;
 	
 	public String updatedDate;
 	public String createdDate;
 	
 	public String profileId;
 	
 	// Each order contains the sender and the recipient (another single in the bar or a friend to pick the order up)
 	public Profile orderSender;
 	public Profile orderReceiver;
 	public String senderId;
 	public String receiverId;
 	
 	// The view displaying this order or null. The view is the display of the order in a list. 
 	// The list could be either on the client or the server and it looks different in both cases
 	// but the code manages the differences. 
 	public View view = null;
 	
 	// The states are implemented in a status variable and each state transition has an associated time
 	public int status;	
 	public int last_status;	// the previous status of this order (needed for timeouts in particular)
 	public int timeOut;			// time in minutes this order has before it times out (from the last updated state)
 
 	// Order states
 	// (received) -> NEW -> (accepted) -> IN_PROGRESS -> (completed) -> READY   -> (picked_up) -> COMPLETE    -> (timed out, error, etc) -> CANCELLED
 	//                      (rejected) -> REJECTED       (failed)    -> FAILED     (forgotten) -> INCOMPLETE  
 	
 	public static final int ORDER_STATUS_NEW			= 0;
     public static final int ORDER_STATUS_REJECTED    	= 1;
     public static final int ORDER_STATUS_IN_PROGRESS  	= 2;
     public static final int ORDER_STATUS_READY 			= 3;
 	public static final int ORDER_STATUS_FAILED	 		= 4;
 	public static final int ORDER_STATUS_COMPLETE	 	= 5;
 	public static final int ORDER_STATUS_INCOMPLETE	 	= 6;
 	public static final int ORDER_STATUS_CANCELLED	 	= 7;
 	public static final int ORDER_STATUS_TIMEOUT 		= 8;
 	public static final int ORDER_STATUS_COUNT 			= 9;
 	
 	public String errorReason = ""; // used to send an error reason for negative order states
     public Date[] state_transitions = new Date[ORDER_STATUS_COUNT];
 
     DecimalFormat df = new DecimalFormat();
     
     
 	/**
 	 *  Default constructor
 	 */
     
 	public Order() {
 		df.setMaximumFractionDigits(2);
 		df.setMinimumFractionDigits(2);
 	}
 
 	
 
 	
 	/**
 	 * Constructor to parse all the information from the JSON
 	 * 
 	 * @param json
 	 */
 	
 	public Order(JSONObject json) {
 		
 		try {
 			title = json.getString("itemName");
 			serverID = json.getString("orderId");
 
 			
 			baseAmount = Float.valueOf(json.getString("basePrice"));
 			tipAmount = Float.valueOf(json.getString("tipPercentage"));
 			totalAmount = Float.valueOf(json.getString("totalPrice"));
 			taxAmount = totalAmount - tipAmount - baseAmount;
 			
 			if (json.has("bartsyId"))
 				profileId = json.getString("bartsyId");
 			if (json.has("description"))
 				description = json.getString("description");
 			
 			if (json.has("orderTimeout"))
 				timeOut = json.getInt("orderTimeout");
 
 			// Setup the order status if it exists or set it to NEW_ORDER if it doesn't
 			if (json.has("orderStatus"))
 				status = Integer.parseInt(json.getString("orderStatus"));
 			else 
 				status = ORDER_STATUS_NEW;
 			
 			// Used only by the getPastOrders syscall
 			if (json.has("dateCreated")) 
 				createdDate = json.getString("dateCreated");
 				
 			// Setup created date (time the order was placed)
 			if (json.has("orderTime")) {
 				// User server provided creation date in the following format: 27 Jun 2013 12:03:04 GMT
 				createdDate = json.getString("orderTime");
 				state_transitions[ORDER_STATUS_NEW] = parseWeirdDate(createdDate);
 			} else {
 				// If no created date use current date for the creation state
 				state_transitions[ORDER_STATUS_NEW] = new Date();
 			}
 
 			// Setup last updated date (time the order was updated last)  *** MAKE SURE to have updated status before getting here ***
 			if (json.has("updateTime")) {
 				// User server provided creation date in the following format: 27 Jun 2013 12:03:04 GMT
 				state_transitions[status] = parseWeirdDate(json.getString("updateTime"));
 			} else {
 				// If no created date use current date for the update time
 				state_transitions[status] = new Date();
 			}
 			
 			// For now, since we are not receiving the last status time, set it to the update time to avoid expiring the order locally before the server had a chance. 
 			// The bartender may not be warned with enough notice and the order may expire prematurely
 			if (status == ORDER_STATUS_READY) {
 				// HACK for now - need to add value is syscalls
 				state_transitions[ORDER_STATUS_IN_PROGRESS] = state_transitions[status];
 			}
 			
 			// Set up last status based on current status
 			switch (status) {
 			case ORDER_STATUS_NEW:
 				last_status = status;
 				break;
 			case ORDER_STATUS_IN_PROGRESS:
 				last_status = ORDER_STATUS_READY;
 				break;
 			case ORDER_STATUS_READY:
 				last_status = ORDER_STATUS_IN_PROGRESS;
 				break;
 			}
 			
 		} catch (NumberFormatException e) {
 			e.printStackTrace();
 		} catch (JSONException e) {
 			e.printStackTrace();
 		}
 
 		// Set our format 
 		df.setMaximumFractionDigits(2);
 		df.setMinimumFractionDigits(2);
 		
 	}
 		
 	Date parseWeirdDate(String date) {
 		Date d = Utilities.getLocalDateFromGTMString(createdDate, "dd MMM yyyy HH:mm:ss 'GMT'");
 		if (d == null)
 			d = Utilities.getLocalDateFromGTMString(createdDate, "d MMM yyyy HH:mm:ss 'GMT'");
 		return d;
 	}
 	
 	
 	/**
 	 * To process next positive state for the order
 	 */
 	
 	public void nextPositiveState() {
 		
 		Log.v(TAG, "setNextPositiveState()");
 
 		switch (status) {
 		case ORDER_STATUS_NEW:
 			last_status = status;
 			status = ORDER_STATUS_IN_PROGRESS;
 			break;
 		case ORDER_STATUS_IN_PROGRESS:
 			last_status = status;
 			status = ORDER_STATUS_READY;
 			break;
 		case ORDER_STATUS_READY:
 			last_status = status;
 			status = ORDER_STATUS_COMPLETE;
 			break;
 		default:
 			Log.v(TAG, "Order " + serverID + " status not changed to next positive because the status was " + status);
 			return;
 		}
 		
 		// Mark the time of the state transition in the timetable
 		state_transitions[status] = new Date();
 		
 		Log.v(TAG, "Order " + this.serverID + " moved from state" + last_status + " to state " + status);
 	}
 
 	public void setCancelledState(String cancelReason) {
 		
 		Log.v(TAG, "setCancelledState()");
 		
 		// Don't change orders that have already this status because their last_status would get lost
 		if (status == ORDER_STATUS_CANCELLED || status == ORDER_STATUS_TIMEOUT) {
 			Log.v(TAG, "Order " + this.serverID + " was already cancelled or timed out (last status: " + last_status + ")");
 			return;
 		}
 		
 		last_status = status;
 		status = ORDER_STATUS_CANCELLED;
 		state_transitions[status] = new Date();
 		errorReason = cancelReason;
 
 		Log.v(TAG, "Order " + this.serverID + " moved from status" + last_status + " to cancelled status " + status + " with reason " + cancelReason);
 	}
 
 	public void setTimeoutState() {
 		
 		Log.v(TAG, "setTimeoutState()");
 
 		// Don't change orders that have already this status because their last_status would get lost
 		if (status == ORDER_STATUS_TIMEOUT ||
 				status == ORDER_STATUS_REJECTED ||
 				status == ORDER_STATUS_FAILED ||
 				status == ORDER_STATUS_INCOMPLETE) {
 			Log.v(TAG, "Order " + this.serverID + "with last status " + last_status + " not changed to timeout status because the status was " + status + " with reason " + errorReason);
 			return;
 		}
 		
 		last_status = status;
 		status = ORDER_STATUS_TIMEOUT;
 		state_transitions[status] = new Date();
 		errorReason = "Server unreachable. Check your internet connection and notficy Bartsy customer support.";
 
 		Log.v(TAG, "Order " + this.serverID + " moved from state " + last_status + " to timeout state " + status);
 	}
 	
 	/**
 	 * To process next negative state for the order
 	 */
 	
 	public void nextNegativeState(String errorReason) {
 		
 		Log.v(TAG, "setNegativeState()");
 
 		int oldStatus = status;
 		
 		switch (status) {
 		case ORDER_STATUS_NEW:
 			last_status = status;
 			status = ORDER_STATUS_REJECTED;
 			break;
 		case ORDER_STATUS_IN_PROGRESS:
 			last_status = status;
 			status = ORDER_STATUS_FAILED;
 			break;
 		case ORDER_STATUS_READY:
 			last_status = status;
 			status = ORDER_STATUS_INCOMPLETE;
 			break;
 		default:
 			Log.v(TAG, "Order " + serverID + " status not changed to negative with reason " + errorReason + " because the status was " + status);
 			return;
 		}
 		
 		// Log the state change and update the order with an error reason
 		Log.v(TAG, "Order " + serverID + " changed status from " + oldStatus + " to " + status + " for reason: "  + errorReason);
 		this.errorReason = errorReason;
 		
 		// Mark the time of the state transition in the timetable
 		state_transitions[status] = new Date();
 	}
 	 
 	
 	/**
 	 * It will returns JSON format to update order status
 	 */
 	public JSONObject statusChangedJSON(){
 		final JSONObject orderData = new JSONObject();
 		try {
 			orderData.put("orderId", serverID);
 			orderData.put("orderStatus", status);
 			orderData.put("orderRejectionReason", errorReason);
 			
 		} catch (JSONException e) {
 			e.printStackTrace();
 		}
 		return orderData;
 	}
 	
 	
 	/**
 	 * Updates the order view. Notice the view holds a pointer to the object being displayed through the "tag" field
 	 */
 	
 	public void updateView () {
 
 		if (view == null) return;
 
 		// Set main order parameters
 		((TextView) view.findViewById(R.id.view_order_number)).setText(serverID);
 		((TextView) view.findViewById(R.id.view_order_title)).setText(title);
 		if (description != null && !description.equalsIgnoreCase(""))
 			((TextView) view.findViewById(R.id.view_order_description)).setText(description);
 		else
 			view.findViewById(R.id.view_order_description).setVisibility(View.GONE);
 
 		// Set base price
 		((TextView) view.findViewById(R.id.view_order_mini_base_amount)).setText(df.format(baseAmount));
 		
 		// Set the totals (we'll update again if we have more mini orders...)
 		updateTipTaxTotalView(tipAmount, taxAmount, totalAmount);
 		
 		if (orderSender != null ) {
 
 			// Update sender profile section
 			
 			ImageView profileImageView = ((ImageView)view.findViewById(R.id.view_order_profile_picture));
 			if (orderSender.image==null) {
 				// Download image from the profile URL
 				WebServices.downloadImage(orderSender.getProfileImageUrl(), orderSender, profileImageView);
 			} else {
 				// Set the saved image to the imageView
 				profileImageView.setImageBitmap(orderSender.image);
 			}
 			
 			
 			((TextView) view.findViewById(R.id.view_order_profile_name)).setText(orderSender.getName());
 		}
 
 		// Update buttons and background
 		
 		String positive="", negative="";
 		switch (status) {
 		case ORDER_STATUS_NEW:
 			positive = "ACCEPT";
 			negative = "REJECT";
 			break;
 		case ORDER_STATUS_IN_PROGRESS:
 			positive = "COMPLETED";
 			negative = "FAILED";
 			break;
 		case ORDER_STATUS_READY:
 			positive = "PICKED UP";
 			negative = "NO SHOW";
 			break;
 		}
 		
		// Compute timers
 		double timeout_ms	= timeOut * 60000;
		double elapsed_ms	= System.currentTimeMillis() - (state_transitions[ORDER_STATUS_NEW]).getTime();
		double left_ms     = timeout_ms - elapsed_ms;
		double elapsed_min = elapsed_ms / (double) 60000;
		double left_min    = Math.ceil(left_ms / (double) 60000);
 		
 		// Set the background color of the order depending on how much time has elapsed as a percent of the timeout green->orange->red
		if (elapsed_ms <= timeout_ms / 3.0)
			view.findViewById(R.id.view_order_header).setBackgroundResource(android.R.color.holo_green_dark);
		else if (elapsed_ms <=  timeout_ms * 2.0 / 3.0)
 			view.findViewById(R.id.view_order_header).setBackgroundResource(android.R.color.holo_orange_dark);
 		else
			view.findViewById(R.id.view_order_header).setBackgroundResource(android.R.color.holo_red_dark);
 
 
 		// Update timer since the order was placed
 		((TextView) view.findViewById(R.id.view_order_timer)).setText(String.valueOf((int)elapsed_min)+" min");
 
 		
 		// Handle timeout views
 		if (status == ORDER_STATUS_CANCELLED || status == ORDER_STATUS_TIMEOUT) {
 			// Change the order display to hide the normal action buttons and to show the timeout acknowledgment
 			view.findViewById(R.id.view_order_actions).setVisibility(View.GONE);
 			view.findViewById(R.id.view_order_expired).setVisibility(View.VISIBLE);
 			
 			// Set text based on reason
 			((TextView) view.findViewById(R.id.view_order_state_description)).setText(errorReason);
 
 			// Update timeout counter to always be expired even if there is some left (due to clock inconsistencies between local and server)
 			((TextView) view.findViewById(R.id.view_order_timeout)).setText("Expired");
 			
 			// Set tag to self for clicks
 			view.findViewById(R.id.view_order_button_expired).setTag(this);
 
 			// Hide order item rejection button
 			((Button) view.findViewById(R.id.view_order_button_remove)).setVisibility(View.GONE);
 
 		
 		} else {
 
 			// Update timout counter		
 			if (left_min > 0) 
 				((TextView) view.findViewById(R.id.view_order_timeout)).setText("Expires in < " + String.valueOf((int)left_min)+" min");
 			else
 				((TextView) view.findViewById(R.id.view_order_timeout)).setText("About to expire");
 
 			// Set up buttons
 			((Button) view.findViewById(R.id.view_order_button_positive)).setTag(this);
 			((Button) view.findViewById(R.id.view_order_button_positive)).setText(positive);
 			((Button) view.findViewById(R.id.view_order_button_negative)).setText(negative);
 			((Button) view.findViewById(R.id.view_order_button_negative)).setTag(this);
 			((Button) view.findViewById(R.id.view_order_button_remove)).setTag(this);
 		}
 		
 		// Set a pointer to the object being displayed 
 		view.setTag(this);
 		
 	}
 	
 	/**
 	 * Update the view of the order only with the given tip, tax and total amounts 
 	 */
 	public void updateTipTaxTotalView(float tipAmount,float taxAmount,float totalAmount){
 		if(view!=null){
 			((TextView) view.findViewById(R.id.view_order_tip_amount)).setText(df.format(tipAmount));
 			((TextView) view.findViewById(R.id.view_order_tax_amount)).setText(df.format(taxAmount));
 			((TextView) view.findViewById(R.id.view_order_total_amount)).setText(df.format(totalAmount));
 
 			// Set the tags of these views with the actual values (used for computing totals later)
 			((TextView) view.findViewById(R.id.view_order_tip_amount)).setTag(tipAmount);
 			((TextView) view.findViewById(R.id.view_order_tax_amount)).setTag(taxAmount);
 			((TextView) view.findViewById(R.id.view_order_total_amount)).setTag(totalAmount);		
 		}
 	}
 	
 	public View getMiniView(LayoutInflater inflater, ViewGroup container ) {
 		
 		LinearLayout view = (LinearLayout) inflater.inflate(R.layout.bartender_order_mini, container, false);
 		
 		((TextView) view.findViewById(R.id.view_order_title)).setText(title);
 		((TextView) view.findViewById(R.id.view_order_description)).setText(description);
 		((TextView) view.findViewById(R.id.view_order_mini_base_amount)).setText(df.format(baseAmount));
 
 		// Set a pointer to hte object being displayed
 		view.findViewById(R.id.view_order_button_remove).setTag(this);
 
 		return view;
 	}
 
 }
