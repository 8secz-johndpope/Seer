 package uy.com.elsubonline.domain;
 
 import java.io.Serializable;
 import java.util.Date;
 import javax.persistence.Entity;
 import javax.persistence.Id;
 import javax.persistence.NamedQuery;
 import javax.persistence.Table;
 import javax.persistence.Temporal;
 import javax.persistence.TemporalType;
 
 @NamedQuery(name="validate_credentials",
         query="select u.first_name from User u where u.email = :username and u.password = :password")
 
 @Entity
 @Table(name="users")
 public class User implements Serializable {
 
     @Id
     private String email;
 
     private String first_name;
     private String last_name;
     private String nick_name;
     private String password;
     private String phone;
     private boolean subscribed;
 
     @Temporal(TemporalType.TIMESTAMP)
     private Date creation_time;
     private UserStatus status;
    private boolean isAdmin;
 
     public String getEmail() {
       return email;
     }
 
     public void setEmail(String email) {
       this.email = email;
     }
 
     public String getFirst_name() {
       return first_name;
     }
 
     public void setFirst_name(String first_name) {
       this.first_name = first_name;
     }
 
     public String getLast_name() {
       return last_name;
     }
 
     public void setLast_name(String last_name) {
       this.last_name = last_name;
     }
 
     public String getNick_name() {
       return nick_name;
     }
 
     public void setNick_name(String nick_name) {
       this.nick_name = nick_name;
     }
 
     public String getPassword() {
       return password;
     }
 
     public void setPassword(String password) {
       this.password = password;
     }
 
     public String getPhone() {
       return phone;
     }
 
     public void setPhone(String phone) {
       this.phone = phone;
     }
 
     public boolean isSubscribed() {
       return subscribed;
     }
 
     public void setSubscribed(boolean subscribed) {
       this.subscribed = subscribed;
     }
 
     public Date getCreation_time() {
         return creation_time;
     }
 
     public void setCreation_time(Date creation_time) {
         this.creation_time = creation_time;
     }
 
     public UserStatus getStatus() {
         return status;
     }
 
     public void setStatus(UserStatus status) {
         this.status = status;
     }
 
    public boolean isIsAdmin() {
        return isAdmin;
     }
 
    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
     }
 
 }
