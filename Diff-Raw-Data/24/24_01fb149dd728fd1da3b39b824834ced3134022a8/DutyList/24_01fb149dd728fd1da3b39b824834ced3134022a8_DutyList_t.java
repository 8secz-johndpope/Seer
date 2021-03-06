 package war.webapp.action;
 
 import static war.webapp.Constants.STAROSTA_ROLE;
 
 import java.io.IOException;
 import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.List;
 import java.util.Locale;
 import java.util.concurrent.Semaphore;
 
 import javax.faces.event.ActionEvent;
 import javax.faces.event.FacesEvent;
 import javax.faces.event.ValueChangeEvent;
 import javax.faces.model.SelectItem;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.springframework.security.context.HttpSessionContextIntegrationFilter;
 import org.springframework.security.context.SecurityContext;
 
 import war.webapp.Constants;
 import war.webapp.model.DayDuty;
 import war.webapp.model.DutyMonth;
 import war.webapp.model.EmptyUser;
 import war.webapp.model.Role;
 import war.webapp.model.User;
 import war.webapp.model.UserDuty;
 import war.webapp.service.DayDutyManager;
 import war.webapp.service.DutyListLoadService;
 import war.webapp.service.MonthManager;
 import war.webapp.service.UserManager;
 import war.webapp.util.MonthHelper;
 
 public class DutyList extends BasePage implements Serializable {
     private static final transient Log logger = LogFactory.getLog(DutyList.class);
 
 
     private static final long serialVersionUID = 911159310602744018L;
 
     public static final int MIN_FLOOR = 2;
     public static final int MAX_FLOOR = 12;
 
     public static final String FIRST_SHIFT = "firstShift";
     public static final String SECOND_SHIFT = "secondShift";
     public static final String FIRST_SHIFT_USER = "firstShiftUser";
     public static final String SECOND_SHIFT_USER = "secondShiftUser";
     public static final String SELECT_USER_STRING = "-";
 
     private DayDutyManager dayDutyManager;
     private MonthManager monthManager;
     private UserManager userManager;
 
     private User user;
     private Integer month;
     private String monthString;
     private Integer floor;
 
     private String selectedUser;
     private List<SelectItem> floorUsersList;
 
     private List<DayDuty> dutyList;
     private static User emptyUser = new EmptyUser();
     private List<DayDuty> emptyDayDutyList;
    private boolean monthAvailable = false;
 
     public DutyList() {
         user = (User) ((SecurityContext) getSession().getAttribute(
                 HttpSessionContextIntegrationFilter.SPRING_SECURITY_CONTEXT_KEY)).getAuthentication().getPrincipal();
         setSortColumn("dayOfWeek");
         setMonth(Calendar.getInstance().get(Calendar.MONTH));
         //28 - minimum count of days in any month in year(this month is February)
         initializeEmptyDayDutyList(28);
     }
     
     private void initializeEmptyDayDutyList(int size){
         emptyDayDutyList = new ArrayList<DayDuty>();
         while(size!=0){
             DayDuty dayDuty = new DayDuty();
             Calendar date = Calendar.getInstance();
             dayDuty.setFirstUser(emptyUser);
             dayDuty.setSecondUser(emptyUser);
             dayDuty.setDate(date);
             size--;
         }
     }
 
     public List<DayDuty> getDutyList() {
         if (getFloor() == null) {
             setFloor(user.getAddress().getHostelFloor());
         }
         List<DayDuty> d = dayDutyManager.loadAllDayDutyByDateAndFloor(month, floor);
         for (DayDuty duty : d) {
             if (duty.isFirstEmpty()) {
                 duty.setFirstUser(getEmptyUser());
             }
             if (duty.isSecondEmpty()) {
                 duty.setSecondUser(getEmptyUser());
             }
         }
         List<DayDuty> result = getEmptyDutyList();
         if (d != null) {
             for (DayDuty dayDuty : d) {
                 result.set(dayDuty.getDate().get(Calendar.DAY_OF_MONTH) - 1, dayDuty);
             }
         }
         dutyList = result;
         return dutyList;
     }
 
     private User getEmptyUser() {
         return emptyUser;
     }
 
     public List<SelectItem> getUsersByStarostaFloor() {
         if (isOnOwnFloor() && isUserStarosta() && floorUsersList == null) {
             floorUsersList = new ArrayList<SelectItem>();
             floorUsersList.add(new SelectItem(SELECT_USER_STRING));
 
             List<User> floorUsers = userManager.getUsersByFloor(floor);
             floorUsers.remove(user);
             for (User floorUser : floorUsers) {
                 floorUsersList.add(new SelectItem(floorUser.getUsername()));
             }
         }
         return floorUsersList;
     }
 
     public void deleteUser(ActionEvent e) {
         int index = getTableRowNumber(e);
         User emptyUser = getEmptyUser();
         DayDuty dayDuty = dutyList.get(index);
         if (e.getComponent().getId().equals(FIRST_SHIFT_USER)) {
             dayDutyManager.deleteFirstDutyUser(dayDuty);
             dayDuty.setFirstUser(emptyUser);
         } else if (e.getComponent().getId().equals(SECOND_SHIFT_USER)) {
             dayDutyManager.deleteSecondDutyUser(dayDuty);
             dayDuty.setSecondUser(emptyUser);
         }
         return;
     }
 
     public void floorUserChanged(ValueChangeEvent e) {
         String newValue = (String) e.getNewValue();
 
         if (newValue.equals(SELECT_USER_STRING))
             return;
 
         String userName = newValue.split(" ")[0];
         User userToWriteOnDuty = userManager.getUserByUsername(userName);
 
         Calendar date = getDate(e);
         DayDuty dayDuty = dayDutyManager.loadDayDutyByDateAndFloor(date, getFloor());
         if (dayDuty == null) {
             dayDuty = new DayDuty();
             dayDuty.setDate(date);
             dayDuty.setFloor(getFloor());
         }
 
         String shift = e.getComponent().getId();
         if (shift.equals(FIRST_SHIFT)) {
             dayDuty.setFirstUser(userToWriteOnDuty);
         } else if (shift.equals(SECOND_SHIFT)) {
             dayDuty.setSecondUser(userToWriteOnDuty);
         }
 
         dayDutyManager.saveDayDuty(dayDuty);
         dutyList = null;
 
     }
 
     public boolean isUserStarosta() {
         return user.getRoles().contains(new Role(STAROSTA_ROLE));
     }
 
     public int getTableRowNumber(FacesEvent e) {
         return Integer.valueOf(e.getComponent().getClientId(getFacesContext()).split(":")[2]);
     }
 
     public List<UserDuty> getUserDuties() throws Exception {
         List<UserDuty> userDuties = new ArrayList<UserDuty>();
         if (isMonthAvailable()) {
             for (DayDuty dayDuty : getDutyList()) {
                 if (dayDuty.getFirstUser() != null && dayDuty.getFirstUser().equals(user)) {
                     userDuties.add(new UserDuty(1, dayDuty));
                 }
                 if (dayDuty.getSecondUser() != null && dayDuty.getSecondUser().equals(user)) {
                     userDuties.add(new UserDuty(2, dayDuty));
                 }
             }
         }
         return userDuties;
     }
     
     public void writeFirstOnDuty(ActionEvent e) {
         if (!isOnOwnFloor() || !isMonthAvailable()) {
             return;
         }
         Calendar date = getDate(e);
         DayDuty dayDuty = getDayDutyManager().loadDayDutyByDateAndFloor(date, getFloor());
 
         if (dayDuty == null) {
             dayDuty = new DayDuty();
             dayDuty.setDate(date);
             dayDuty.setFloor(getFloor());
         }
         if (dayDuty.getFirstUser() != null) {
             return;
         }
         dayDuty.setFirstUser(user);
         getDayDutyManager().saveDayDuty(dayDuty);
         return;
     }
 
     public void writeSecondOnDuty(ActionEvent e) {
         if (!isOnOwnFloor() || !isMonthAvailable()) {
             return;
         }
         Calendar date = getDate(e);
         DayDuty dayDuty = getDayDutyManager().loadDayDutyByDateAndFloor(date, getFloor());
         if (dayDuty == null) {
             dayDuty = new DayDuty();
             dayDuty.setDate(date);
             dayDuty.setFloor(getFloor());
         }
         if (dayDuty.getSecondUser() != null) {
             return;
         }
         dayDuty.setSecondUser(user);
         getDayDutyManager().saveDayDuty(dayDuty);
         return;
     }
 
     public void deleteDuty(ActionEvent e) throws Exception {
         int index = getTableRowNumber(e);
         try {
             UserDuty userDuty = getUserDuties().get(index);
             if (userDuty.getShift() == 1) {
                 userDuty.getDayDuty().setFirstUser(null);
             }
             if (userDuty.getShift() == 2) {
                 userDuty.getDayDuty().setSecondUser(null);
             }
 
             dayDutyManager.deleteDayDuty(userDuty.getDayDuty());
         } catch (Exception ex) {
             throw new Exception(ex);
         }
     }
 
     private Calendar getDate(FacesEvent e) {
         String id = e.getComponent().getClientId(getFacesContext());
         int day = Integer.parseInt(id.split(":")[2]) + 1;
         Calendar date = Calendar.getInstance();
         date.set(Calendar.MONTH, month);
         date.set(Calendar.DAY_OF_MONTH, day);
         return date;
     }
 
     private void correctEmptyDayDutyList(int countOfDays){
         int currentEmptyListSize = emptyDayDutyList.size();
         if (currentEmptyListSize == countOfDays) {
             return;
         }
         int delta = currentEmptyListSize - countOfDays;
         if (delta > 0) {
             while (delta!=0) {
                 emptyDayDutyList.remove(0);
                 delta--;
             }
         } else {
             for ( int i = 0 ; i < Math.abs(delta); i++) {
                 DayDuty dd = new DayDuty();
                 Calendar date = Calendar.getInstance();
                 dd.setFirstUser(emptyUser);
                 dd.setSecondUser(emptyUser);
                 dd.setDate(date);
                 emptyDayDutyList.add(dd);
             }
         }
     }
     
     private List<DayDuty> getEmptyDutyList() {
         correctEmptyDayDutyList(MonthHelper.getDaysNumInMonth(month + 1));
         int counter = 1;
         for (DayDuty dd: emptyDayDutyList) {
             dd.getDate().set(Calendar.MONTH, month);
             dd.getDate().set(Calendar.DAY_OF_MONTH, counter);
             counter++;
         }
         return emptyDayDutyList;
     }
 
     public List<SelectItem> getMonthItems() {
         ArrayList<SelectItem> items = new ArrayList<SelectItem>();
         String[] months = MonthHelper.getMonths(getBundle());
         for (int i = 0; i < months.length; ++i) {
             items.add(new SelectItem(months[i]));
         }
         return items;
     }
 
     public void monthSelectionChanged(ValueChangeEvent e) {
         String newValue = (String) e.getNewValue();
         setMonth(MonthHelper.getMonth(newValue, getBundle()));
         dutyList = null;
     }
 
     public List<SelectItem> getFloors() {
         ArrayList<SelectItem> items = new ArrayList<SelectItem>();
         for (int i = MIN_FLOOR; i <= MAX_FLOOR; ++i) {
             items.add(new SelectItem(i));
         }
         return items;
     }
 
     public void floorChanged(ValueChangeEvent e) {
         setFloor((Integer) e.getNewValue());
         dutyList = null;
     }
 
     public void print(ActionEvent e) {
         Semaphore sem = new Semaphore(1, true);
         try {
             sem.acquire();
             //TODO forth param must be name of vospetka and starosta
             String monthForExcelReport = MonthHelper.getMonthString(month, getBundle(new Locale("ru")));
             Object[] params = new Object[]{getFloor(), monthForExcelReport, "Федоров В.В", "Иванова А.А.", getDutyList()};
             DutyListLoadService.getService(Constants.HTTP_DOWNLOADER).download(params);
         } catch (InterruptedException ex) {
             logger.error("Current thread was interrupted!");
         } catch (IOException ex) {
             logger.error("IO error:"+ex.getMessage());
         } catch (NullPointerException exc){
             //no critic, it's joke! :)
             logger.error("Something wrong.."+exc.getMessage());
         }
         finally {
             sem.release();
         }
         
     }
 
     public void changeMonthAvailability(ActionEvent e) {
         Integer year = Calendar.getInstance().get(Calendar.YEAR);
         DutyMonth dutyMonth = monthManager.loadMonth(year, month, getFloor());
         if (dutyMonth == null) {
             dutyMonth = createDutyMonth();
             dutyMonth.setAvailable(true);
         } else {
             dutyMonth.setAvailable(!dutyMonth.getAvailable());
         }
         monthManager.saveMonth(dutyMonth);
        monthAvailable = !monthAvailable;
     }
 
     private DutyMonth createDutyMonth() {
         DutyMonth dutyMonth = new DutyMonth();
         // TODO user should be able to choose the year
         dutyMonth.setYear(Calendar.getInstance().get(Calendar.YEAR));
         dutyMonth.setMonth(month);
         dutyMonth.setFloor(getFloor());
         return dutyMonth;
     }
 
     public boolean isMonthAvailable() {
        return monthAvailable;
    }
    
    private void refreshMonthAvailability() {
        if (monthManager == null) {
            return;
        }
         Integer year = Calendar.getInstance().get(Calendar.YEAR);
         DutyMonth dutyMonth = monthManager.loadMonth(year, month, getFloor());
         if (dutyMonth == null) {
            monthAvailable = false;
        } else {
            monthAvailable = dutyMonth.getAvailable();
         }
     }
 
     public DayDutyManager getDayDutyManager() {
         return dayDutyManager;
     }
 
     public Integer getMonth() {
         return month;
     }
 
     public void setMonth(Integer month) {
         this.month = month;
         monthString = MonthHelper.getMonthString(month, getBundle());
        refreshMonthAvailability();
     }
 
     public Integer getFloor() {
         if (floor == null) {
             setFloor(user.getAddress().getHostelFloor());
         }
         return floor;
     }
 
     public boolean isOnOwnFloor() {
         return user.getAddress().getHostelFloor().equals(getFloor());
     }
 
     public void setFloor(Integer floor) {
         this.floor = floor;
        refreshMonthAvailability();
     }
 
     public void setDayDutyManager(DayDutyManager dayDutyManager) {
         this.dayDutyManager = dayDutyManager;
     }
 
     public MonthManager getMonthManager() {
         return monthManager;
     }
 
     public void setMonthManager(MonthManager monthManager) {
         this.monthManager = monthManager;
        refreshMonthAvailability();
     }
 
     public String getMonthString() {
         return monthString;
     }
 
     public void setMonthString(String monthString) {
         this.monthString = monthString;
     }
 
     public UserManager getUserManager() {
         return userManager;
     }
 
     public void setUserManager(UserManager userManager) {
         this.userManager = userManager;
     }
 
     public String getSelectedUser() {
         return selectedUser;
     }
 
     public void setSelectedUser(String selectedUser) {
         this.selectedUser = selectedUser;
     }
 
 }
