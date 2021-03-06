 package models.dbentities;
 
 import java.text.DateFormat;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 
 import javax.persistence.Column;
 import javax.persistence.Entity;
 import javax.persistence.EnumType;
 import javax.persistence.Enumerated;
 import javax.persistence.Id;
 import javax.persistence.Table;
 
 import controllers.util.DateFormatter;
 
 import models.management.Listable;
 import models.management.ManageableModel;
 import models.user.Gender;
 import models.user.UserType;
 import play.data.format.Formats;
 import play.db.ebean.Model;
 import models.EMessages;
 
 /**
  * @author Jens N. Rammant
  * TODO check the date formats
  */
 @Entity
 @Table(name="users")
 public class UserModel extends ManageableModel implements Listable{
 
     private static final long serialVersionUID = 2L;
 
     @Id
     public String id;
     public String name;
 
     @Formats.DateTime(pattern = "yyyy/dd/mm")
     public Date birthdate;
 
     @Formats.DateTime(pattern = "yyyy/dd/mm")
     public Date registrationdate;
     public String preflanguage;
     public String password;
     public String hash;
     public String telephone;
     public String address;
     public String email;
 
     @Enumerated(EnumType.STRING)
     public Gender gender;
 
     @Enumerated(EnumType.STRING)
     public UserType type;
 
    public Date blockeduntil;
 
     @Column(name="class")
     public Integer classgroup;
 
     public UserModel(String id, UserType loginType, String name,
             Date birthdate, Date registrationdate,
             String password, String hash, String email,
             Gender gender, String preflanguage){
 
         this.id = id;
         this.type = loginType;
         this.name = name;
         this.birthdate = birthdate;
         this.registrationdate = registrationdate;
         this.password = password;
         this.hash = hash;
         this.email = email;
         this.gender = gender;
         this.preflanguage = preflanguage;
        this.blockeduntil = null;
         EMessages.setLang(preflanguage);
     }
 
     public UserModel() {
 		//empty constructor
 	}
 
 	/**
      * A finder for User.
      * We will use this finder to execute specific sql query's.
      */
     public static Finder<String,UserModel> find = new Model.Finder<String, UserModel>(String.class,UserModel.class);
 
     @Override
     public Map<String, String> options() {
         List<UserModel> users = find.all(); //TODO try-catch
         LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
         for(UserModel user: users) {
             options.put(user.id, user.id);
         }
         return options;
     }
 
 	@Override
 	public String[] getFieldValues() {
 		String[] res = {
 				id,
 				name,
 				gender.toString(),
 				convertDate(birthdate),
 				preflanguage,
				DateFormatter.formatDate(this.blockeduntil)				
 		};		
 		return res;
 	}
 
 	@Override
 	public String getID() {
 		return id;
 	}
 	
 	public String getBirthDate(){
 		return convertDate(this.birthdate);
 	}
 	
 	private String convertDate(Date d){
 		return DateFormatter.formatDate(d);
 	}
 	
 	/**
 	 * 
 	 * @return whether the user is currently blocked
 	 */
 	public boolean isCurrentlyBlocked(){
		if(this.blockeduntil==null) return false;
 		Date today = Calendar.getInstance().getTime();
		return !today.before(this.blockeduntil);
 	}
 }
