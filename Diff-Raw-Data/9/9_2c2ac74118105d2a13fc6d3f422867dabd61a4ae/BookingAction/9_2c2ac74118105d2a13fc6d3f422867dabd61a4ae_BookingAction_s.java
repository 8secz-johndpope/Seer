 package action;
 
 import java.text.DateFormat;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 
 import model.Adjustment;
 import model.Booking;
 import model.Extra;
 import model.Guest;
 import model.Payment;
 import model.Room;
 import model.RoomFacility;
 import model.Structure;
 import model.User;
 import model.internal.Message;
 
 
 import org.apache.struts2.convention.annotation.Action;
 import org.apache.struts2.convention.annotation.Actions;
 import org.apache.struts2.convention.annotation.ParentPackage;
 import org.apache.struts2.convention.annotation.Result;
 import org.apache.struts2.interceptor.SessionAware;
 
 import com.opensymphony.xwork2.ActionSupport;
 
 @ParentPackage(value="default")
 public class BookingAction extends ActionSupport implements SessionAware{
 	private Map<String, Object> session = null;
 	private List<Booking> bookings = null;
 	private Booking booking = null;
 	private String dateIn = null;
 	private Integer id;
 	private Integer numNights;
 	private List<Room> rooms = null;
 	private Message message = new Message();
 	private String dateOut = null;
 	private List<Extra> extras = null;
 	private List<Integer> bookingExtraIds = new ArrayList<Integer>();
 	private Double adjustmentsSubtotal = 0.0;
 	private Double paymentsSubtotal = 0.0;
 	
 	
 	
 	@Actions({
 		@Action(value="/goAddBookingFromPlanner",results = {
 				@Result(name="success",location="/jsp/contents/booking_form.jsp"),
 				
 				@Result(name="input", location="/validationError.jsp")
 		})
 	})
 	
 	public String goAddNewBookingFromPlanner() {
 		User user = null;
 		Structure structure = null;
 		Room theBookedRoom = null;
 		Integer numNights = 0;
 		Double roomSubtotal = 0.0;
 		
 		user = (User)this.getSession().get("user");
 		structure = user.getStructure();
 		
 		theBookedRoom = structure.findRoomById(this.getBooking().getRoom().getId());
 		this.getBooking().setRoom(theBookedRoom);
 		
 		this.setRooms(structure.getRooms());
 		this.setExtras(structure.getExtras());
 		
 		roomSubtotal = structure.calculateRoomSubtotalForBooking(this.getBooking());
 		this.getBooking().setRoomSubtotal(roomSubtotal);
 		
 		numNights = this.getBooking().calculateNumNights();
 		this.setNumNights(numNights);
 		
 		return SUCCESS;
 	}
 	
 	@Actions({
 		@Action(value="/goAddNewBooking",results = {
 				@Result(name="success",location="/book.jsp")
 		})
 	})
 	
 	public String goAddNewBooking() {
 		User user = null;
 		Structure structure = null;
 		
 		user = (User)this.getSession().get("user");
 		structure = user.getStructure();
 		
 		this.setRooms(structure.getRooms());
 		this.setExtras(structure.getExtras());		
 		this.setBooking(new Booking());		
 		return SUCCESS;
 	}
 	
 	@Actions({
 		@Action(value="/goUpdateBooking",results = {
 				@Result(name="success",location="/book.jsp")
 		}),
 		@Action(value="/goUpdateBookingFromPlanner",results = {
 				@Result(name="success",location="/jsp/contents/booking_form.jsp")
 		})
 	})
 	
 	public String goUpdateBooking() {
 		User user = null;
 		Structure structure = null;
 		Booking oldBooking = null;
 		Integer numNights = 0;
 		Double extraSubtotal = 0.0;
 		Double roomSubtotal = 0.0;
 		Double adjustmentsSubtotal = 0.0;
 		Double paymentsSubtotal = 0.0;
 		
 		user = (User)this.getSession().get("user");
 		structure = user.getStructure();
 		
 		oldBooking = structure.findBookingById(this.getId());
 		this.setBooking(oldBooking);
 		
 		this.setRooms(structure.getRooms());
 		this.setExtras(structure.getExtras());		
 		this.setBookingExtraIds(this.calculateBookingExtraIds());
 		
 		/*
 		extraSubtotal = structure.calculateExtraSubtotalForBooking(this.getBooking());
 		this.getBooking().setExtraSubtotal(extraSubtotal);
 		*/
 		
 		/*
 		roomSubtotal = structure.calculateRoomSubtotalForBooking(this.getBooking());
 		this.getBooking().setRoomSubtotal(roomSubtotal);
 		*/
 		
 		numNights = this.getBooking().calculateNumNights();
 		this.setNumNights(numNights);
 		
 		adjustmentsSubtotal = this.getBooking().calculateAdjustmentsSubtotal();
 		this.setAdjustmentsSubtotal(adjustmentsSubtotal);
 		
 		paymentsSubtotal = this.getBooking().calculatePaymentsSubtotal();
 		this.setPaymentsSubtotal(paymentsSubtotal);
 		
 		return SUCCESS;
 	}
 	
 	
 	private List<Integer> calculateBookingExtraIds(){
 		List<Integer> ret = null;
 		
 		ret = new ArrayList<Integer>();
 		for(Extra each: this.getBooking().getExtras()){
 			ret.add(each.getId());
 		}
 		return ret;
 	}
 	
 	@Actions({
 		@Action(value="/findAllBookings",results = {
 				@Result(name="success",location="/bookings.jsp")
 		}),
 		@Action(value="/findAllBookingsJson",results = {
 				@Result(type ="json",name="success", params={
 						"root","bookings"
 				})}) 
 		
 	})
 	public String findAllBookings(){
 		User user = null;
 		Structure structure = null;
 		
 		user = (User)session.get("user");
 		structure = user.getStructure();
 		this.setBookings(structure.getBookings());
 		return SUCCESS;		
 	}
 	
 	
 	
 	@Actions({
 		@Action(value="/findBookingById",results = {
 				@Result(type ="json",name="success", params={
 						"excludeProperties","session,bookings,id"
 				}) ,
 				@Result(type ="json",name="error", params={
 						"root","message"
 				})}) 
 		
 	})
 	
 	
 	public String findBookingById(){
 		User user = null;
 		Structure structure = null;
 		Booking aBooking = null;
 		
 		user = (User)session.get("user");
 		structure = user.getStructure();
 		aBooking = structure.findBookingById(this.getId());
 		if(aBooking!=null){
 			this.setBooking(aBooking);
 			this.getMessage().setResult(Message.SUCCESS);
 			return SUCCESS;
 		}
 		this.getMessage().setResult(Message.ERROR);
 		this.getMessage().setDescription("Booking not found!");
 		return ERROR;
 	}
 	
 		
 	@Actions({
 		@Action(value="/saveUpdateBooking",results = {
 				@Result(type ="json",name="success", params={
 						"root","message"
 				} ),
 				@Result(name="input", location="/validationError.jsp"),
 				@Result(type ="json",name="error", params={
 						"root","message"
 				} )
 		})
 		
 	})
 	public String saveUpdateBooking(){
 		User user = null;
 		Structure structure = null;
 		Booking oldBooking = null;
 		Room theBookedRoom = null;
 		Guest guest = null;
 		Guest oldGuest = null;
 		List<Extra>  checkedExtras = null;
 		
 		user = (User)session.get("user");
 		structure = user.getStructure();
 		
 		if(!structure.hasRoomFreeForBooking(this.getBooking())){
 			this.getMessage().setResult(Message.ERROR);
 			this.getMessage().setDescription("Booking sovrapposti!");
 			return ERROR;
 		}
 		
 		theBookedRoom = structure.findRoomById(this.getBooking().getRoom().getId());
 		this.getBooking().setRoom(theBookedRoom);				
 		
 		checkedExtras = structure.findExtrasByIds(this.getBookingExtraIds());				
 		this.getBooking().setExtras(checkedExtras);	
 		
 		this.saveUpdateAdjustments(structure);
 		this.saveUpdatePayments(structure);
 		
 		guest = this.getBooking().getGuest();
 		oldGuest = structure.findGuestById(guest.getId());		
 		if(oldGuest == null){
 			//Si tratta di un nuovo guest e devo aggiungerlo
 			guest.setId(structure.nextKey());
 			structure.addGuest(guest);			
 		}else{
 			//Si tratta di un guest esistente e devo fare l'update
 			structure.updateGuest(guest);			
 		}	
 		
 		oldBooking = 
 			structure.findBookingById(this.getBooking().getId());
 		if(oldBooking==null){
 			//Si tratta di un nuovo booking
 			this.getBooking().setId(structure.nextKey());
 			structure.addBooking(this.getBooking());
 		}else{
 			//Si tratta di un update di un booking esistente			
 			structure.updateBooking(this.getBooking());		
 		}		
 		this.getMessage().setResult(Message.SUCCESS);
 		this.getMessage().setDescription("Booking added/modified successfully");
 		return SUCCESS;
 	}
 	
 	@Actions({
 		@Action(value="/checkBookingDates",results = {
 				@Result(type ="json",name="success", params={
 						"root","message"
 				} ),
 				@Result(name="input", location="/validationError.jsp"),
 				@Result(type ="json",name="error", params={
 						"root","message"
 				} )
 		})
 		
 	})
 	public String checkBookingDates(){
 		User user = null;
 		Structure structure = null;
 		
 		user = (User)session.get("user");
 		structure = user.getStructure();		
 		if(!structure.hasRoomFreeForBooking(this.getBooking())){
 				this.getMessage().setResult(Message.ERROR);
 				this.getMessage().setDescription("Booking sovrapposti!");
 				return ERROR;
 		}
 		this.getMessage().setResult(Message.SUCCESS);
 		this.getMessage().setDescription("Booking Dates OK!");
 		return SUCCESS;
 	}
 	
 	
 	
 	
 	private Boolean saveUpdateAdjustments(Structure structure){
 		List<Adjustment> adjustmentsWithoutNulls = null;
 		
 		adjustmentsWithoutNulls = new ArrayList<Adjustment>();
 		for(Adjustment each: this.getBooking().getAdjustments()){
 			if(each!=null){
 				if(each.getId() == null){
 					each.setId(structure.nextKey());
 				}
 				adjustmentsWithoutNulls.add(each);
 			}			
 		}
 		this.getBooking().setAdjustments(adjustmentsWithoutNulls);
 		
 		return true;
 		
 	}
 	
 	
 	
 	private Boolean saveUpdatePayments(Structure structure){
 		List<Payment> paymentsWithoutNulls = null;
 		
 		paymentsWithoutNulls = new ArrayList<Payment>();
 		for(Payment each: this.getBooking().getPayments()){
 			if(each!=null){
 				if(each.getId() == null){
 					each.setId(structure.nextKey());
 				}
 				paymentsWithoutNulls.add(each);
 			}			
 		}
 		this.getBooking().setPayments(paymentsWithoutNulls);		
 		return true;		
 	}
 	
 	@Actions({
 		@Action(value="/deleteBooking",results = {
 				@Result(type ="json",name="success", params={
 						"root","message"
 				} ),
 				@Result(type ="json",name="error", params={
 						"root","message"
 				} )
 		})
 		
 	})
 	
 	public String deleteBooking() {
 		User user = null;
 		Structure structure = null;
 		
 		user = (User)this.getSession().get("user");
 		//Controllare che sia diverso da null in un interceptor
 		structure = user.getStructure();
 		
 		if(structure.deleteBooking(this.getBooking())){
 			this.getMessage().setResult(Message.SUCCESS);
 			this.getMessage().setDescription("Booking deleted successfully");
 			return "success";
 		}else{
 			this.getMessage().setResult(Message.ERROR);
 			this.getMessage().setDescription("Error: booking not deleted");
 			return "error";
 		}		
 	}	
 	
 	@Actions({
 		@Action(value="/goOnlineBookings",results = {
 				@Result(name="success",location="/onlineBookings.jsp")
 		}) 
 		
 	})
 	public String goOnlineBookings(){
 		return SUCCESS;		
 	}
 
 	public Message getMessage() {
 		return message;
 	}
 
 	public void setMessage(Message message) {
 		this.message = message;
 	}
 
 	public Integer getId() {
 		return id;
 	}
 
 	public void setId(Integer id) {
 		this.id = id;
 	}
 
 	public Map<String, Object> getSession() {
 		return session;
 	}
 
 	@Override
 	public void setSession(Map<String, Object> session) {
 		this.session = session;
 	}
 
 	public Booking getBooking() {
 		return booking;
 	}
 
 	public void setBooking(Booking booking) {
 		this.booking = booking;
 	}
 
 	public List<Booking> getBookings() {
 		return bookings;
 	}
 
 	public void setBookings(List<Booking> bookings) {
 		this.bookings = bookings;
 	}
 
 	public String getDateIn() {
 		return dateIn;
 	}
 
 	public void setDateIn(String dateIn) {
 		this.dateIn = dateIn;
 	}
 
 	public Integer getNumNights() {
 		return numNights;
 	}
 
 	public void setNumNights(Integer numNights) {
 		this.numNights = numNights;
 	}
 	
 	public List<Room> getRooms() {
 		return rooms;
 	}
 	
 	public void setRooms(List<Room> rooms) {
 		this.rooms = rooms;
 	}
 
 	public String getDateOut() {
 		return dateOut;
 	}
 
 	public void setDateOut(String dateOut) {
 		this.dateOut = dateOut;
 	}
 
 	public List<Extra> getExtras() {
 		return extras;
 	}
 
 	public void setExtras(List<Extra> extras) {
 		this.extras = extras;
 	}
 
 
 	public List<Integer> getBookingExtraIds() {
 		return bookingExtraIds;
 	}
 
 
 	public void setBookingExtraIds(List<Integer> bookingExtraIds) {
 		this.bookingExtraIds = bookingExtraIds;
 	}
 
 	public Double getAdjustmentsSubtotal() {
 		return adjustmentsSubtotal;
 	}
 
 	public void setAdjustmentsSubtotal(Double adjustmentsSubtotal) {
 		this.adjustmentsSubtotal = adjustmentsSubtotal;
 	}
 
 	public Double getPaymentsSubtotal() {
 		return paymentsSubtotal;
 	}
 
 	public void setPaymentsSubtotal(Double paymentsSubtotal) {
 		this.paymentsSubtotal = paymentsSubtotal;
 	}
 
 	
 	
 
 }
