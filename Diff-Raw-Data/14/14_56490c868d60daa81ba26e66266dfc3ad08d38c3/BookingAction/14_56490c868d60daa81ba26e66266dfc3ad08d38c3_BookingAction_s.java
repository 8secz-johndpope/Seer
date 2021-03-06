 package action;
 
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.List;
 import java.util.Map;
 
 import model.Adjustment;
 import model.ExtraItem;
 import model.Booking;
 import model.Extra;
 import model.Guest;
 import model.Payment;
 import model.Room;
 import model.RoomType;
 import model.Structure;
 import model.User;
 import model.UserAware;
 import model.internal.Message;
 import model.listini.Convention;
 import model.listini.Season;
 
 import org.apache.struts2.convention.annotation.Action;
 import org.apache.struts2.convention.annotation.Actions;
 import org.apache.struts2.convention.annotation.InterceptorRef;
 import org.apache.struts2.convention.annotation.InterceptorRefs;
 import org.apache.struts2.convention.annotation.ParentPackage;
 import org.apache.struts2.convention.annotation.Result;
 import org.apache.struts2.interceptor.SessionAware;
 import org.springframework.beans.factory.annotation.Autowired;
 
 import service.BookingService;
 import service.ConventionService;
 import service.ExtraService;
 import service.GuestService;
 import service.RoomService;
 import service.SeasonService;
 import service.StructureService;
 import com.opensymphony.xwork2.ActionSupport;
 
 @ParentPackage( value="default")
 @InterceptorRefs({
 	@InterceptorRef("userAwareStack")    
 })
 @Result(name="notLogged", location="/homeNotLogged.jsp")
 public class BookingAction extends ActionSupport implements SessionAware,UserAware{
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
 	private List<Convention> conventions = null;
 	private List bookingExtraIds = new ArrayList();
 	private Double adjustmentsSubtotal = 0.0;
 	private Double paymentsSubtotal = 0.0;
 	private Integer idStructure;
 	
 	@Autowired
 	private ExtraService extraService = null;
 	@Autowired
 	private GuestService guestService = null;
 	@Autowired
 	private StructureService structureService = null;
 	@Autowired
 	private BookingService bookingService = null;
 	@Autowired
 	private RoomService roomService = null;
 	@Autowired
 	private ConventionService conventionService = null;
 	@Autowired
 	private SeasonService seasonService = null;
 	
 	@Actions({
 		@Action(
 				
 				value="/updateBookingDates",results = {
 				@Result(type ="json",name="success", params={
 						"excludeProperties","session,extraService,guestService,structureService,bookingService,roomService,conventionService,seasonService"
 				}),
 				@Result(type ="json",name="error", params={
 						"excludeProperties","session,extraService,guestService,structureService,bookingService,roomService,conventionService,seasonService"
 				}),
 				@Result(name="input", location = "/validationError.jsp")
 		})
 	})	
 	public String updateBookingDates() {
 		
 		if(!this.checkBookingDates(this.getIdStructure())){
 			return ERROR;
 		}		
 		this.updateBookingDates(this.getIdStructure());
 		this.getMessage().setResult(Message.SUCCESS);
 		this.getMessage().setDescription(getText("calculatedPriceAction"));
 		return "success";						
 	}
 	
 	public void updateBookingDates(Integer id_structure) {
 		Integer numNights;
 		Booking booking = null;
 		
 		booking  = (Booking) this.getSession().get("booking");		
 		
 		booking.setDateIn(this.getBooking().getDateIn());
 		booking.setDateOut(this.getBooking().getDateOut());
 		
 		numNights = booking.calculateNumNights();
 		this.setNumNights(numNights);
 		
 		this.updateRoomSubtotal(id_structure, booking);		
 		this.updateUnitaryPriceInBookedExtraItems(id_structure, booking);
 		this.updateMaxQuantityInBookedExtraItems(booking);
 		booking.updateExtraSubtotal();
 		
 		this.setBooking(booking);
 		
 	}
 	
 	
 		
 	@Actions({
 		@Action(value="/updateRoom",results = {
 				@Result(type ="json",name="success", params={
 						"excludeProperties","session,extraService,guestService,structureService,bookingService,roomService,conventionService,seasonService"
 				}),
 				@Result(type ="json",name="error", params={
 						"excludeProperties","session,extraService,guestService,structureService,bookingService,roomService,conventionService,seasonService"
 				}),
 				@Result(name="input", location = "/validationError.jsp")
 		})
 	})	
 	public String updateRoom() {
 		
 		if(!this.checkBookingDates(this.getIdStructure())){
 			return ERROR;
 		}		
 		this.updateRoom(this.getIdStructure());
 		this.getMessage().setResult(Message.SUCCESS);
 		this.getMessage().setDescription(getText("calculatedPriceAction"));
 		return "success";						
 	}
 	
 	public void updateRoom(Integer id_structure) {
 		Integer numGuests;
 		Booking booking = null;			
 		RoomType newRoomType = null;
 		RoomType oldRoomType = null;
 				
 		booking  = (Booking) this.getSession().get("booking");
 		
 		if(booking.getRoom()!=null){
 			oldRoomType = booking.getRoom().getRoomType();
 		}
 		
 		this.updateRoom(booking);	
 		
 		if(booking.getRoom()!=null){
 			newRoomType = booking.getRoom().getRoomType();
 		}
 		
 		//Se cambia la categoria allora devo aggiornare i prezzi
 		if((oldRoomType!= null) && (newRoomType!= null) && !(oldRoomType.equals(newRoomType) )){
 			numGuests = booking.getNrGuests();
 			if (numGuests > newRoomType.getMaxGuests()) {	//nel caso cambiassi la room con preselezionato un nrGuests superiore al maxGuests della room stessa
 				numGuests = newRoomType.getMaxGuests();
 				booking.setNrGuests(numGuests);
 				//Se cambia il numero di guest devo aggiornare anche la quantità massima degli extra item
 				this.updateMaxQuantityInBookedExtraItems(booking);
 			}			
 			this.updateRoomSubtotal(id_structure, booking);			
 			this.updateUnitaryPriceInBookedExtraItems(id_structure, booking);
 			booking.updateExtraSubtotal();
 		}
 		
 		this.setBooking(booking);
 	}
 	
 	
 	@Actions({
 		@Action(value="/updateNrGuests",results = {
 				@Result(type ="json",name="success", params={
 						"excludeProperties","session,extraService,guestService,structureService,bookingService,roomService,conventionService,seasonService"
 				}),
 				@Result(type ="json",name="error", params={
 						"excludeProperties","session,extraService,guestService,structureService,bookingService,roomService,conventionService,seasonService"
 				}),
 				@Result(name="input", location = "/validationError.jsp")
 		})
 	})	
 	public String updateNrGuests() {
 		
 		if(!this.checkBookingDates(this.getIdStructure())){
 			return ERROR;
 		}		
 		this.updateNrGuests(this.getIdStructure());
 		this.getMessage().setResult(Message.SUCCESS);
 		this.getMessage().setDescription(getText("calculatedPriceAction"));
 		return "success";						
 	}
 	
 	public void updateNrGuests(Integer id_structure) {
 		Booking booking = null;		
 		
 		booking  = (Booking) this.getSession().get("booking");			
 		
 		booking.setNrGuests(this.getBooking().getNrGuests());
 		this.updateRoomSubtotal(id_structure, booking);
 		this.updateMaxQuantityInBookedExtraItems(booking);
 		
 		this.setBooking(booking);
 		
 	}
 	
 	
 	@Actions({
 		@Action(value="/updateExtras",results = {
 				@Result(type ="json",name="success", params={
 						"excludeProperties","session,extraService,guestService,structureService,bookingService,roomService,conventionService,seasonService"
 				}),
 				@Result(type ="json",name="error", params={
 						"excludeProperties","session,extraService,guestService,structureService,bookingService,roomService,conventionService,seasonService"
 				}),
 				@Result(name="input", location = "/validationError.jsp")
 		})
 	})	
 	public String updateExtras() {
 		
 		if(!this.checkBookingDatesNotNull(this.getIdStructure())){
 			return ERROR;
 		}		
 		this.updateExtras(this.getIdStructure());
 		
 		this.getMessage().setResult(Message.SUCCESS);
 		this.getMessage().setDescription(getText("calculatedPriceAction"));
 		return "success";						
 	}
 	
 	public void updateExtras(Integer id_structure) {	
 		Booking booking = null;	
 		List<Extra> checkedExtras = null;
 		List<Integer> filteredBookingExtraIds = null;
 		Integer anInt;
 					
 		booking  = (Booking) this.getSession().get("booking");			
 		
 		filteredBookingExtraIds = new ArrayList<Integer>();
 		for(Object each: this.getBookingExtraIds()){
 			try{
 				anInt = Integer.parseInt((String)each);
 				filteredBookingExtraIds.add(anInt);
 			}catch (Exception e) {
 				
 			}			
 		}		
 		checkedExtras = this.getExtraService().findExtrasByIds(filteredBookingExtraIds);
 		this.updateExtraItems(id_structure, booking,checkedExtras);					
 		booking.updateExtraSubtotal();	
 		
 		this.setBooking(booking);
 	}	
 	
 	@Actions({
 		@Action(value="/updateConvention",results = {
 				@Result(type ="json",name="success", params={
 						"excludeProperties","session,extraService,guestService,structureService,bookingService,roomService,conventionService,seasonService"
 				}),
 				@Result(type ="json",name="error", params={
 						"excludeProperties","session,extraService,guestService,structureService,bookingService,roomService,conventionService,seasonService"
 				}),
 				@Result(name="input", location = "/validationError.jsp")
 		})
 	})	
 	public String updateConvention() {
 		
 		if(!this.checkBookingDates(this.getIdStructure())){
 			return ERROR;
 		}		
 		this.updateConvention(this.getIdStructure());
 		this.getMessage().setResult(Message.SUCCESS);
 		this.getMessage().setDescription(getText("calculatedPriceAction"));
 		return "success";						
 	}
 	
 	public void updateConvention(Integer id_structure) {
 		Booking booking = null;			
 						
 		booking  = (Booking) this.getSession().get("booking");
 		booking.setConvention(this.getBooking().getConvention());
 				
 		//Se cambia la convenzione allora devo aggiornare i prezzi della room e degli extra
 		this.updateRoomSubtotal(id_structure, booking);			
 		this.updateUnitaryPriceInBookedExtraItems(id_structure, booking);
 		booking.updateExtraSubtotal();
 		
 		this.setBooking(booking);
 	}
 	
 	
 	private void updateRoom(Booking booking){
 		Room newRoom = null; 
 		
 		//newRoom = this.getRoomService().findRoomById(structure,this.getBooking().getRoom().getId());
 		newRoom = this.getRoomService().findRoomById(this.getBooking().getRoom().getId());
 		booking.setRoom(newRoom);	
 	}
 	
 	
 	
 	private void updateExtraItems(Integer id_structure, Booking booking,List<Extra> checkedExtras){
 		ExtraItem extraItem = null;		
 		List<ExtraItem> extraItems = null;
 		
 		extraItems = new ArrayList<ExtraItem>();	
 		for(Extra each: checkedExtras){
 			extraItem = null;
 			for(ExtraItem bookedExtraItem: this.getBooking().getExtraItems()){
 				//each esiste già e devo aggiornare solo la quantità leggendola dalla request
 				if(bookedExtraItem.getExtra().equals(each)){
 					extraItem = bookedExtraItem;	
 					extraItem.setExtra(each);//In certi casi extraItem.extra aveva solo l'id e non tutte le altre proprietà
 					extraItem.setMaxQuantity(booking.calculateExtraItemMaxQuantity(each));					
 				}				
 			}
 			if(extraItem == null){
 				//each è un nuovo extra quindi devo creare un nuovo extra item associato
 				extraItem = new ExtraItem();
 				extraItem.setExtra(each);
 				extraItem.setQuantity(booking.calculateExtraItemMaxQuantity(each));
 				extraItem.setMaxQuantity(booking.calculateExtraItemMaxQuantity(each));
 				extraItem.setUnitaryPrice(
 						this.getStructureService().calculateExtraItemUnitaryPrice(id_structure, booking.getDateIn(), booking.getDateOut(), booking.getRoom().getRoomType(), booking.getConvention(), each));
 			}
 			extraItems.add(extraItem);	
 		}	
 		booking.setExtraItems(extraItems);		
 	}
 	
 	
 	private void updateRoomSubtotal(Integer id_structure, Booking booking){
 		Double roomSubtotal = 0.0;
 		
 		roomSubtotal = this.getBookingService().calculateRoomSubtotalForBooking(id_structure,booking);
 		booking.setRoomSubtotal(roomSubtotal);		
 	}
 	
 	
 	
 	private void updateMaxQuantityInBookedExtraItems(Booking booking){
 		Integer maxQuantity;
 		
 		for(ExtraItem each: booking.getExtraItems()){
 			maxQuantity = booking.calculateExtraItemMaxQuantity(each.getExtra());
 			each.setMaxQuantity(maxQuantity);
 		}		
 	}	
 	
 	private void updateUnitaryPriceInBookedExtraItems(Integer id_structure, Booking booking){
 		Double unitaryPrice;
 		if((booking.getDateIn()!=null) && (booking.getDateOut()!=null)){
 			for(ExtraItem each: booking.getExtraItems()){
 				unitaryPrice = this.getStructureService().calculateExtraItemUnitaryPrice(id_structure, booking.getDateIn(), booking.getDateOut(), booking.getRoom().getRoomType(), booking.getConvention(), each.getExtra());
 				each.setUnitaryPrice(unitaryPrice);
 			}	
 			
 		}
 			
 	}
 	
 	
 	@Actions({
 		@Action(value="/displayQuantitySelect",results = {
 				@Result(name="success",location="/jsp/contents/extraQuantity_select.jsp")
 				})
 	})
 	public String displayQuantitySelect() {
 		
 		this.setExtras(this.getExtraService().findExtrasByIdStructure(this.getIdStructure()));
 		this.setBooking((Booking) this.getSession().get("booking"));
 		return SUCCESS;		
 	}
 	
 	
 	@Actions({
 		@Action(value="/saveUpdateBooking",results = {
 				@Result(type ="json",name="success", params={
 						"root","message"
 				}),
 				@Result(name="input", location="/validationError.jsp"),
 				@Result(type ="json",name="error", params={
 						"root","message"
 				})
 		})
 	})
 	public String saveUpdateBooking(){
 		Guest booker = null;
 		Booking booking = null;
 		Convention convention = null;
 		
 		
 		if(!this.checkBookingDates(this.getIdStructure())){
 			return ERROR;
 		}		
 		
 		booking = (Booking) this.getSession().get("booking");
 		
 		//Adjustments
 		this.filterAdjustments();
 		booking.setAdjustments(this.getBooking().getAdjustments());
 		//Payments
 		this.filterPayments();
 		booking.setPayments(this.getBooking().getPayments());
 		//ExtraItems: non c'è bisogno di filtrarli perchè vengono aggiornati ad ogni chiamata di updateExtras		
 		
 		//Guests: da inserire quando ci saranno i guests
 		/*
 		this.filterGuests();	
 		booking.setGuests(this.getBooking().getGuests());
 		*/
 		booker = this.getBooking().getBooker();		
 		booker.setId_structure(this.getIdStructure());
 		booking.setBooker(booker);
 		
 		booking.setId_room(this.getBooking().getRoom().getId());
 		
 		convention = booking.getConvention();
 		booking.setId_convention(convention.getId());
 		
 		booking.setStatus(this.getBooking().getStatus());		
 		booking.setId_structure(this.getIdStructure());
 		
 		this.getBookingService().saveUpdateBooking(booking);
 		
 		this.getSession().put("booking", booking);
 		this.getMessage().setResult(Message.SUCCESS);
 		this.getMessage().setDescription(getText("bookingAddUpdateSuccessAction"));
 		return SUCCESS;
 	}
 	
 		
 	@Actions({
 		@Action(value="/goAddBookingFromPlanner",results = {
 				@Result(name="success",location="/jsp/contents/booking_form.jsp"),
 				
 				@Result(name="input", location="/validationError.jsp")
 		})
 	})
 	public String goAddNewBookingFromPlanner() {
 		Room theBookedRoom = null;
 		Convention defaultConvention = null;
 		Integer numNights = 0;
 		Double roomSubtotal = 0.0;
 		Booking booking = null;
 		
 		booking = new Booking();
 		this.getSession().put("booking", booking);
 		
 		booking.setDateIn(this.getBooking().getDateIn());
 		booking.setDateOut(this.getBooking().getDateOut());		
 		
 		//theBookedRoom = this.getRoomService().findRoomById(structure,this.getBooking().getRoom().getId());
 		theBookedRoom = this.getRoomService().findRoomById(this.getBooking().getRoom().getId());
 		booking.setRoom(theBookedRoom);
 		
 		defaultConvention = this.getConventionService().findConventionsByIdStructure(this.getIdStructure()).get(0);
 		booking.setConvention(defaultConvention);
 		
 		roomSubtotal = this.getBookingService().calculateRoomSubtotalForBooking(this.getIdStructure(),booking);
 		booking.setRoomSubtotal(roomSubtotal);
 		
 		this.setBooking(booking);
 		
 		this.setRooms(this.getRoomService().findRoomsByIdStructure(this.getIdStructure()));
 		this.setExtras(this.getExtraService().findExtrasByIdStructure(this.getIdStructure()));
 		this.setConventions(this.getConventionService().findConventionsByIdStructure(this.getIdStructure()));
 				
 		numNights = booking.calculateNumNights();
 		this.setNumNights(numNights);
 		
 		
 		return SUCCESS;
 	}
 	
 	@Actions({
 		@Action(value="/goAddNewBooking",results = {
 				@Result(name="success",location="/book.jsp")
 		})
 	})
 	public String goAddNewBooking() {
 		Convention defaultConvention = null;
 		Booking booking = null;
 				
 		booking = new Booking();
 		this.getSession().put("booking", booking);
 						
 		defaultConvention = this.getConventionService().findConventionsByIdStructure(this.getIdStructure()).get(0);
 		booking.setConvention(defaultConvention);	
 		this.setBooking(booking);
 		
 		this.setRooms(this.getRoomService().findRoomsByIdStructure(this.getIdStructure()));
 		this.setExtras(this.getExtraService().findExtrasByIdStructure(this.getIdStructure()));
 		this.setConventions(this.getConventionService().findConventionsByIdStructure(this.getIdStructure()));		
 		
 			
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
 		Booking booking = null;
 		Integer numNights = 0;
 		Double adjustmentsSubtotal = 0.0;
 		Double paymentsSubtotal = 0.0;
 		
 		//findBookingById deve caricare dal DB anche gli extraItems, adjustments e payments, 
 		//booker e Convention perchè devono essre usati nella JSP
 		
 		booking = this.getBookingService().findBookingById(this.getId());
 		this.getSession().put("booking", booking);
 		
 		this.setBooking(booking);
 		
 		this.setRooms(this.getRoomService().findRoomsByIdStructure(this.getIdStructure()));
 		//Tutti gli Extra della Struttura
 		this.setExtras(this.getExtraService().findExtrasByIdStructure(this.getIdStructure()));
 		
 		
 		this.setBookingExtraIds(booking.calculateExtraIds());
 		this.setConventions(this.getConventionService().findConventionsByIdStructure(this.getIdStructure()));		
 		
 		numNights = this.getBooking().calculateNumNights();
 		this.setNumNights(numNights);
 		
 		adjustmentsSubtotal = this.getBooking().calculateAdjustmentsSubtotal();
 		this.setAdjustmentsSubtotal(adjustmentsSubtotal);
 		
 		paymentsSubtotal = this.getBooking().calculatePaymentsSubtotal();
 		this.setPaymentsSubtotal(paymentsSubtotal);		
 		
 		return SUCCESS;
 	}
 	
 	
 	
 	@Actions({
 		@Action(value="/findAllBookings",results = {
 				@Result(name="success",location="/bookings.jsp")
 		}),
 		@Action(value="/findAllBookingsJson",results = {
 				@Result(type ="json",name="success", params={
 						"root","bookings"
 				})
 		}) 
 	})
 	public String findAllBookings(){
 		
 		this.setBookings(this.getBookingService().findBookingsByIdStructure(this.getIdStructure()));
 		return SUCCESS;		
 	}	
 
 	
 	@Actions({
 		@Action(value="/checkBookingDates",results = {
 				@Result(type ="json",name="success", params={
 						"root","message"
 				}),
 				@Result(name="input", location="/validationError.jsp"),
 				@Result(type ="json",name="error", params={
 						"root","message"
 				})
 		})
 	})
 	public String checkBookingDates(){
 		
 		if(!this.checkBookingDates(this.getIdStructure())){
 			return ERROR;
 		}
 		return SUCCESS;
 				
 	}
 	
 	@Actions({
 		@Action(value="/checkBookingDatesNotNull",results = {
 				@Result(type ="json",name="success", params={
 						"root","message"
 				}),
 				@Result(name="input", location="/validationError.jsp"),				
 				@Result(type ="json",name="error", params={
 						"root","message"
 				})
 		})
 	})
 	public String checkBookingDatesNotNull(){
 		
 		if(!this.checkBookingDatesNotNull(this.getIdStructure())){
 			return ERROR;
 		}
 		return SUCCESS;
 				
 	}
 	
 	private Boolean checkBookingDates(Integer id_structure) {
 		List<Date> bookingDates = null;
 		Season season = null;
 		
 		if(this.getBooking().getDateIn()!=null && this.getBooking().getDateOut()!=null){
 			if(!this.getBooking().checkDates()){
 				this.getMessage().setResult(Message.ERROR);
 				this.getMessage().setDescription(getText("dateOutMoreDateInAction"));
 				return false;
 			}
 			if ((this.getBooking().getRoom().getId()!=null) && (!this.getStructureService().hasRoomFreeForBooking(id_structure,this.getBooking()))) {
 				this.getMessage().setResult(Message.ERROR);
 				this.getMessage().setDescription(getText("bookingOverlappedAction"));
 				return false;
 			}	
 			bookingDates = this.getBooking().calculateBookingDates();
 			for(Date aBookingDate: bookingDates){
 				season = this.getSeasonService().findSeasonByDate(id_structure,aBookingDate );
 				if(season == null){
 					this.getMessage().setResult(Message.ERROR);
 					this.getMessage().setDescription("Nel periodo selezionato non sono state configurate le stagioni");
 					return false;
 				}
 			}
 		}
 		
 
 		this.getMessage().setDescription(getText("bookingDatesOK"));
 		this.getMessage().setResult(Message.SUCCESS);
 		return true;
 	}
 	
 	
 	private Boolean checkBookingDatesNotNull(Integer id_structure) {
 		List<Date> bookingDates = null;
 		Season season = null;
 		
 		if(this.getBooking().getDateIn()!=null && this.getBooking().getDateOut()!=null){
 			if(!this.getBooking().checkDates()){
 				this.getMessage().setResult(Message.ERROR);
 				this.getMessage().setDescription(getText("dateOutMoreDateInAction"));
 				return false;
 			}
 			if ((this.getBooking().getRoom().getId()!=null) && (!this.getStructureService().hasRoomFreeForBooking(id_structure,this.getBooking()))) {
 				this.getMessage().setResult(Message.ERROR);
 				this.getMessage().setDescription(getText("bookingOverlappedAction"));
 				return false;
 			}
 			bookingDates = this.getBooking().calculateBookingDates();
 			for(Date aBookingDate: bookingDates){
 				season = this.getSeasonService().findSeasonByDate(id_structure,aBookingDate );
 				if(season == null){
 					this.getMessage().setResult(Message.ERROR);
 					this.getMessage().setDescription("Nel periodo selezionato non sono state configurate le stagioni");
 					return false;
 				}
 			}
 			
 			
 		}
 		else{
 			this.getMessage().setResult(Message.ERROR);
 			this.getMessage().setDescription(getText("bookingNotChosenDates"));
 			return false;
 		}
 		this.getMessage().setDescription(getText("bookingDatesOK"));
 		this.getMessage().setResult(Message.SUCCESS);
 		return true;
 	}
 	
 	private void filterAdjustments(){
 		List<Adjustment> adjustmentsWithoutNulls = null;
 		
 		adjustmentsWithoutNulls = new ArrayList<Adjustment>();
 		
 		for(Adjustment each: this.getBooking().getAdjustments()){
 			if(each!=null){
 				adjustmentsWithoutNulls.add(each);
 			}			
 		}
 		this.getBooking().setAdjustments(adjustmentsWithoutNulls);		
 	}
 	
 	private void filterPayments(){
 		List<Payment> paymentsWithoutNulls = null;
 		
 		paymentsWithoutNulls = new ArrayList<Payment>();
 		
 		for(Payment each: this.getBooking().getPayments()){
 			if(each!=null){
 				paymentsWithoutNulls.add(each);
 			}			
 		}
 		this.getBooking().setPayments(paymentsWithoutNulls);		
 	}
 	
 	
 	private void filterGuests(){
 		List<Guest> guestWithoutNulls = null;
 		
 		guestWithoutNulls = new ArrayList<Guest>();
 		
 		for(Guest each: this.getBooking().getGuests()){
 			if(each!=null){
 				guestWithoutNulls.add(each);
 			}			
 		}
 		this.getBooking().setGuests(guestWithoutNulls);
 		
 	}
 	
 	@Actions({
 		@Action(value="/deleteBooking",results = {
 				@Result(type ="json",name="success", params={
 						"root","message"
 				}),
 				@Result(type ="json",name="error", params={
 						"root","message"
 				})
 		})
 	})
 	public String deleteBooking() {
 		Integer count = 0;
 		
 		count = this.getBookingService().deleteBooking(this.getBooking().getId());
 		if(count > 0 ){
 			this.getMessage().setResult(Message.SUCCESS);
 			this.getMessage().setDescription(getText("bookingDeleteSuccessAction"));
 			return "success";
 		}else{
 			this.getMessage().setResult(Message.ERROR);
 			this.getMessage().setDescription(getText("bookingDeleteErrorAction"));
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
 	public List getBookingExtraIds() {
 		return bookingExtraIds;
 	}
 	public void setBookingExtraIds(List bookingExtraIds) {
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
 	public List<Convention> getConventions() {
 		return conventions;
 	}
 	public void setConventions(List<Convention> conventions) {
 		this.conventions = conventions;
 	}
 
 
 	public Integer getIdStructure() {
 		return idStructure;
 	}
 
 	public void setIdStructure(Integer idStructure) {
 		this.idStructure = idStructure;
 	}
 
 	public ExtraService getExtraService() {
 		return extraService;
 	}
 
 
 	public void setExtraService(ExtraService extraService) {
 		this.extraService = extraService;
 	}
 
 
 	public GuestService getGuestService() {
 		return guestService;
 	}
 
 
 	public void setGuestService(GuestService guestService) {
 		this.guestService = guestService;
 	}
 
 	public StructureService getStructureService() {
 		return structureService;
 	}
 
 	public void setStructureService(StructureService structureService) {
 		this.structureService = structureService;
 	}
 
 	public BookingService getBookingService() {
 		return bookingService;
 	}
 
 	public void setBookingService(BookingService bookingService) {
 		this.bookingService = bookingService;
 	}
 
 	public RoomService getRoomService() {
 		return roomService;
 	}
 
 	public void setRoomService(RoomService roomService) {
 		this.roomService = roomService;
 	}
 
 	public ConventionService getConventionService() {
 		return conventionService;
 	}
 
 	public void setConventionService(ConventionService conventionService) {
 		this.conventionService = conventionService;
 	}
 
 	public SeasonService getSeasonService() {
 		return seasonService;
 	}
 
 	public void setSeasonService(SeasonService seasonService) {
 		this.seasonService = seasonService;
 	}
 	
 	
 	
 }
