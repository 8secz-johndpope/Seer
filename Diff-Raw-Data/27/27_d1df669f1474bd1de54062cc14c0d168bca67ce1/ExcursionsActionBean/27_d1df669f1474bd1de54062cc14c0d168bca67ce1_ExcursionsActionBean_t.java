 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package cz.muni.fi.pa165.jtravelagency.web;
 
 import cz.muni.fi.pa165.jtravelagency.dto.ExcursionDTO;
 import cz.muni.fi.pa165.jtravelagency.dto.TripDTO;
 import cz.muni.fi.pa165.jtravelagency.facade.ServiceFacade;
import static cz.muni.fi.pa165.jtravelagency.web.BaseActionBean.escapeHTML;
 import java.util.List;
 import net.sourceforge.stripes.action.Before;
 import net.sourceforge.stripes.action.DefaultHandler;
 import net.sourceforge.stripes.action.ForwardResolution;
 import net.sourceforge.stripes.action.LocalizableMessage;
 import net.sourceforge.stripes.action.RedirectResolution;
 import net.sourceforge.stripes.action.Resolution;
 import net.sourceforge.stripes.action.UrlBinding;
 import net.sourceforge.stripes.controller.LifecycleStage;
 import net.sourceforge.stripes.integration.spring.SpringBean;
 import net.sourceforge.stripes.validation.Validate;
 import net.sourceforge.stripes.validation.ValidateNestedProperties;
 import net.sourceforge.stripes.validation.ValidationErrorHandler;
 import net.sourceforge.stripes.validation.ValidationErrors;
 import org.joda.time.DateTime;
 import org.joda.time.format.DateTimeFormat;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  *
  * @author Peter
  */
 @UrlBinding("/excursions/{$event}/{excursion.id}")
 public class ExcursionsActionBean extends BaseActionBean implements ValidationErrorHandler {
     
     final static Logger log = LoggerFactory.getLogger(ExcursionsActionBean.class);
     final static String pattern = "dd.MM.yyyy HH:mm";
     
     @SpringBean
     protected ServiceFacade facade;
     
     private List<ExcursionDTO> excursions;
     private List<TripDTO> trips;
     
     
     public List<TripDTO> getTrips() {
         return trips;
     }
     
     @DefaultHandler
     public Resolution list() {
         log.debug("list()");
         excursions = facade.getAllExcursions();
         trips = facade.getAllTrips();
         return new ForwardResolution("/excursion/list.jsp");
     }
     
     public List<ExcursionDTO> getExcursions() {
         return excursions;
     }
     
 
     @ValidateNestedProperties(value = {
             @Validate(on = {"add", "save"}, field = "description", required = true),
             @Validate(on = {"add", "save"}, field = "price", required = true, minvalue = 0, mask = "(\\d+|\\d+\\.\\d{1,2})"),
     })
     private ExcursionDTO excursion;
     @Validate(on = {"add", "save"}, required = true)
     private String date;
     @Validate(on = {"add", "save"}, required = true)
     private Long tripId; 
     
     public Resolution add() {
         log.debug("add() excursion={}", excursion);
         excursion.setExcursionDate(DateTime.parse(date, DateTimeFormat.forPattern(pattern)));
         excursion.setTrip(facade.getTrip(tripId));
         facade.createExcursion(excursion);
         getContext().getMessages().add(new LocalizableMessage("excursion.add.message",escapeHTML(excursion.getDescription())));
         return new RedirectResolution(this.getClass(), "list");
     }
 
     @Override
     public Resolution handleValidationErrors(ValidationErrors errors) throws Exception {
         trips = facade.getAllTrips();
         excursions = facade.getAllExcursions();
         return null;
     }
 
     public ExcursionDTO getExcursion() {
         return excursion;
     }
 
     public void setExcursion(ExcursionDTO excursion) {
         this.excursion = excursion;
     }
     
     public Long getTripId() {
         return tripId;
     }
 
     public void setTripId(Long tripId) {
         this.tripId = tripId;
     }
     
     public String getDate() {
         return date;
     }
 
     public void setDate(String date) {
         this.date = date;
     }
     
 
     public Resolution delete() {
         log.debug("delete() excursion={}", excursion);
         //only id is filled by the form
         excursion = facade.getExcursion(excursion.getId());
         facade.deleteExcursion(excursion);
         getContext().getMessages().add(new LocalizableMessage("excursion.delete.message",escapeHTML(excursion.getDescription())));
         return new RedirectResolution(this.getClass(), "list");
     }
     
 
     @Before(stages = LifecycleStage.BindingAndValidation, on = {"edit", "save"})
     public void loadExcursionFromDatabase() {
         String ids = getContext().getRequest().getParameter("excursion.id");
         if (ids == null) return;
         excursion = facade.getExcursion(Long.parseLong(ids));
         date = excursion.getExcursionDate().toString(DateTimeFormat.forPattern(pattern));
         trips = facade.getAllTrips();
         tripId = excursion.getTrip().getId();
     }
 
     public Resolution edit() {
         log.debug("edit() excursion={}", excursion);
         return new ForwardResolution("/excursion/edit.jsp");
     }
 
     public Resolution save() {
         log.debug("save() excursion={}", excursion);
         excursion.setExcursionDate(DateTime.parse(date, DateTimeFormat.forPattern(pattern)));
         excursion.setTrip(facade.getTrip(tripId));
         facade.updateExcursion(excursion);
        getContext().getMessages().add(new LocalizableMessage("excursion.update.message",escapeHTML(excursion.getDescription())));
         return new RedirectResolution(this.getClass(), "list");
     }
     
     public Resolution cancel() {
         return new RedirectResolution(this.getClass(), "list");
     }
 }
