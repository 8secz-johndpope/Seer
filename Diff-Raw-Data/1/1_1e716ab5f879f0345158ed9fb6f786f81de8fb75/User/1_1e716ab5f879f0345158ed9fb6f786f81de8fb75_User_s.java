 /**
  * Copyright (c) 2009 Red Hat, Inc.
  *
  * This software is licensed to you under the GNU General Public License,
  * version 2 (GPLv2). There is NO WARRANTY for this software, express or
  * implied, including the implied warranties of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
  * along with this software; if not, see
  * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
  *
  * Red Hat trademarks are not licensed under GPLv2. No permission is
  * granted to use or replicate Red Hat trademarks that are incorporated
  * in this software or its documentation.
  */
 package org.fedoraproject.candlepin.model;
 
 import java.util.Formatter;
 
 import javax.persistence.Column;
 import javax.persistence.Entity;
 import javax.persistence.GeneratedValue;
 import javax.persistence.GenerationType;
 import javax.persistence.Id;
 import javax.persistence.JoinColumn;
 import javax.persistence.ManyToOne;
 import javax.persistence.SequenceGenerator;
 import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
 import javax.xml.bind.annotation.XmlAccessType;
 import javax.xml.bind.annotation.XmlAccessorType;
 import javax.xml.bind.annotation.XmlRootElement;
 import javax.xml.bind.annotation.XmlTransient;
 
 import org.hibernate.annotations.ForeignKey;
 
 /**
  * Represents the user.
  *
  * A user is more akin to an account within an owner. (i.e. organization)
  */
 @XmlRootElement
 @XmlAccessorType(XmlAccessType.PROPERTY)
 @Entity
 @Table(name = "cp_user")
 @SequenceGenerator(name = "seq_user", sequenceName = "seq_user", allocationSize = 1)
 public class User implements Persisted {
     
     @Id
     @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_user")
     private Long id;
 
     @ManyToOne
     @ForeignKey(name = "fk_user_owner_id")
     @JoinColumn(nullable = false)
     private Owner owner;
     
     @Column(nullable = false, unique = true)
     private String login;
 
     // TODO: Hash!
     private String password;
     
     private boolean superAdmin;
 
     public User() {
     }
 
     public User(Owner owner, String login, String password) {
         this(owner, login, password, false);
     }
     
     public User(Owner owner, String login, String password, boolean superAdmin) {
         this.owner = owner;
         this.login = login;
         this.password = password;
         this.superAdmin = superAdmin;
     }
 
     /**
      * @return the id
      */
     public Long getId() {
         return id;
     }
 
     /**
      * @param id the id to set
      */
     public void setId(Long id) {
         this.id = id;
     }
 
     /**
      * @return the login
      */
     public String getLogin() {
         return login;
     }
     /**
      * @param login the login to set
      */
     public void setLogin(String login) {
         this.login = login;
     }
     /**
      * @return the password
      */
     public String getPassword() {
         return password;
     }
     /**
      * @param password the password to set
      */
     public void setPassword(String password) {
         this.password = password;
     }
     /**
      * @return the owner
      */
     @XmlTransient
     public Owner getOwner() {
         return owner;
     }
     /**
      * @param owner the owner to set
      */
     public void setOwner(Owner owner) {
         this.owner = owner;
     }
    
     /**
      * @return if the user has the SUPER_ADMIN role
      */
     public boolean isSuperAdmin() {
         return superAdmin;
     }
 
     /**
      * @param superAdmin if the user should have the SUPER_ADMIN role
      */
     public void setSuperAdmin(boolean superAdmin) {
         this.superAdmin = superAdmin;
     }
 
     /**
      * Return string representation of the user object
      * @return string representation of the user object
      */
     public String toString() {
         return new Formatter().format("User :{login: %s, password: %s}",
                 login, password).toString();
     }
 
 }
