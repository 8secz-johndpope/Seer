 /*
  * User.java
  *
  * Created on April 23, 2007, 1:30 PM
  *
  * To change this template, choose Tools | Template Manager
  * and open the template in the editor.
  */
 package hu.sch.domain;
 
 import java.io.Serializable;
 import java.text.Collator;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.List;
 import java.util.Locale;
 import javax.persistence.Column;
 import javax.persistence.Entity;
 import javax.persistence.EnumType;
 import javax.persistence.Enumerated;
 import javax.persistence.FetchType;
 import javax.persistence.GeneratedValue;
 import javax.persistence.Id;
 import javax.persistence.JoinColumn;
 import javax.persistence.ManyToOne;
 import javax.persistence.NamedQueries;
 import javax.persistence.NamedQuery;
 import javax.persistence.OneToMany;
 import javax.persistence.SequenceGenerator;
 import javax.persistence.Table;
 import javax.persistence.Transient;
 
 /**
  * Felhasználót reprezentáló entitás
  * @author hege
  */
 @Entity
 @Table(name = "users")
 @NamedQueries({
     @NamedQuery(name = "findAllUser",
     query = "SELECT u FROM User u"),
     @NamedQuery(name = "findUserWithMemberships",
     query = "SELECT u FROM User u LEFT OUTER JOIN FETCH u.memberships WHERE u.id = :id"),
     @NamedQuery(name = "findUserByNeptunCode",
     query = "SELECT u FROM User u WHERE u.neptunCode = :neptun")})
 @SequenceGenerator(name = "users_seq", sequenceName = "users_usr_id_seq")
 public class User implements Serializable, Comparable<User> {
 
     private static final long serialVersionUID = 1L;
     public static final String findAll = "findAllUser";
     public static final String findByLoginName = "findUserByLoginName";
     public static final String findWithMemberships = "findUserWithMemberships";
     /*
     usr_id                 | integer                | not null default nextval('users_usr_id_seq'::regclass)
     usr_email              | character varying(64)  | 
     usr_neptun             | character(6)           | 
     usr_firstname          | text                   | not null
     usr_lastname           | text                   | not null
     usr_nickname           | text                   | 
     usr_svie_state         | character varying(255) | not null default 'NEMTAG'::character varying
     usr_svie_member_type   | character varying(255) | not null default 'NEMTAG'::character varying
    usr_svie_primary_group | integer                | 
      */
     /**
      * Felhasználó azonosítója
      */
     private Long id;
     /**
      * E-mail cím
      */
     private String emailAddress;
     /**
      * Neptun-kód
      */
     private String neptunCode;
     /**
      * Keresztnév
      */
     private String firstName;
     /**
      * Vezetéknév
      */
     private String lastName;
     /**
      * Bejelentkezési név
      */
     private String nickName;
     /**
      * SVIE tagság státusza
      */
     private SvieStatus svieStatus;
     /**
      * SVIE tagság típusa
      */
     private SvieMembershipType svieMembershipType;
     /**
      * SVIE elsődleges kör
      * Rendes tagsága kell legyen a körben
      */
     private Membership sviePrimaryMemberhip;
     /**
      * Csoporttagságok - tagsági idővel kiegészítve
      */
     private List<Membership> memberships;
     /**
      * Tranziens csoporttagsagok
      */
     private List<Group> groups;
 
     /**
      * A felhasználó egyedi azonosítóját visszaadó függvény
      * @return A user virId-je
      */
     @Id
     @GeneratedValue(generator = "users_seq")
     @Column(name = "usr_id")
     public Long getId() {
         return id;
     }
 
     public void setId(Long id) {
         this.id = id;
     }
 
     @Column(name = "usr_email", length = 64, nullable = false, columnDefinition =
     "varchar(64)")
     public String getEmailAddress() {
         return emailAddress;
     }
 
     public void setEmailAddress(String emailAddress) {
         this.emailAddress = emailAddress;
     }
 
     @Column(name = "usr_neptun", columnDefinition = "char(6)", length = 6, nullable =
     true, updatable = false)
     public String getNeptunCode() {
         return neptunCode;
     }
 
     public void setNeptunCode(String neptunCode) {
         this.neptunCode = neptunCode;
     }
 
     @Column(name = "usr_firstname", nullable = false, columnDefinition = "text")
     public String getFirstName() {
         return firstName;
     }
 
     public void setFirstName(String firstName) {
         this.firstName = firstName;
     }
 
     @Column(name = "usr_lastname", nullable = false, columnDefinition = "text")
     public String getLastName() {
         return lastName;
     }
 
     public void setLastName(String lastName) {
         this.lastName = lastName;
     }
 
     @Column(name = "usr_nickname", nullable = true, columnDefinition = "text")
     public String getNickName() {
         return nickName;
     }
 
     public void setNickName(String nickName) {
         this.nickName = nickName;
     }
 
     @Enumerated(EnumType.STRING)
     @Column(name = "usr_svie_state")
     public SvieStatus getSvieStatus() {
         return svieStatus;
     }
 
     public void setSvieStatus(SvieStatus svieStatus) {
         this.svieStatus = svieStatus;
     }
 
     @Enumerated(EnumType.STRING)
     @Column(name = "usr_svie_member_type")
     public SvieMembershipType getSvieMembershipType() {
         return svieMembershipType;
     }
 
     public void setSvieMembershipType(SvieMembershipType svieMembershipType) {
         this.svieMembershipType = svieMembershipType;
     }
 
     @ManyToOne
     @JoinColumn(name = "usr_svie_primary_group", insertable = false, updatable = false)
     public Membership getSviePrimaryMembership() {
         return sviePrimaryMemberhip;
     }
 
     public void setSviePrimaryMembership(Membership sviePrimaryMembership) {
         this.sviePrimaryMemberhip = sviePrimaryMembership;
     }
 
     @Transient
     public List<Group> getGroups() {
         if (groups == null) {
             loadGroups();
         }
         return groups;
     }
 
     private void loadGroups() {
         groups = new ArrayList<Group>();
         if (memberships != null) {
             for (Membership m : memberships) {
                 groups.add(m.getGroup());
             }
         }
     }
 
     @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
     public List<Membership> getMemberships() {
         return memberships;
     }
 
     public void setMemberships(List<Membership> memberships) {
         this.memberships = memberships;
     }
 
     @Transient
     public String getName() {
         return getLastName() + " " + getFirstName();
     }
 
     public void sortMemberships() {
         if (this.getMemberships() != null) {
             Collections.sort(this.getMemberships(),
                     new Comparator<Membership>() {
 
                         public int compare(Membership o1, Membership o2) {
                             if (o1.getEnd() == null ^ o2.getEnd() == null) {
                                 return o1.getEnd() == null ? -1 : 1;
                             }
                             return o1.getGroup().compareTo(o2.getGroup());
                         }
                     });
         }
     }
 
     @Override
     public String toString() {
         return "hu.uml13.domain.User " +
                 "id=" + (getId() == null ? "NULL" : getId()) +
                 ", nev=" + getName() +
                 ", becenev=" + getNickName() +
                 ", email=" + getEmailAddress();
     }
 
     @Override
     public int compareTo(User o) {
         Collator huCollator = Collator.getInstance(new Locale("hu"));
         return huCollator.compare(getName(), o.getName());
     }
 }
