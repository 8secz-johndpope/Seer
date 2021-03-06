 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package com.mti.webshare.daoimpl;
 
 import com.mti.webshare.dao.UserDAO;
 import com.mti.webshare.model.User;
 import com.mti.webshare.utilitaire.Encryptor;
 import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.Query;
 import org.hibernate.SessionFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.stereotype.Repository;
 import org.springframework.transaction.annotation.Transactional;
 
 /**
  *
  * @author vince
  */
 @Repository
 @Transactional
 public class UserDAOImpl implements UserDAO {
     
     @Autowired 
     private SessionFactory sessionFactory;
 
     @Override
     public Boolean create(String lastName, String fisrtName, String password, String email) {
         try
         {
             User user = new User();
             user.setLastname(lastName);
             user.setFirstname(fisrtName);
             user.setEmail(email);
             user.setPassword(Encryptor.getEncodedPassword(password));
             user.setDeleted(Boolean.FALSE);
         
            
             sessionFactory.getCurrentSession().save(user);
 
             return (true);
         }
         catch (Exception e)
         {
             System.out.println(e.getMessage());
             return false;
         }
     }
 
     @Override
     public Boolean update(User user) 
     {
         try 
         {
             sessionFactory.getCurrentSession().update(user);
             return true;
         }
         catch (Exception e)
         {
             return false;
         }
     }
 
     @Override
     public Boolean delete(User user) 
     {
         try
         {    
             user.setDeleted(Boolean.TRUE);
             update(user);
             return true;
         }
         catch (Exception e)
         {
             return null;
         }
     }
 
     @Override
     public User get(int id) {
         try {
            Integer userId = id;
           Query q = sessionFactory.getCurrentSession().createSQLQuery("from User wher id = ?");
           q.setParameter(0, userId.toString() , Hibernate.STRING);
           
           User user = (User) q.uniqueResult();
             return null;
         }
         catch (Exception e) 
         {
             return null;
         }
     }
 
     @Override
     public User get(String email) {
         try {
            Query q = sessionFactory.getCurrentSession().createQuery("from User  where email = ?");
            q.setParameter(0, email, Hibernate.STRING);
            
            return (User) q.uniqueResult();
         } catch (Exception e) {
             return null;
         }
     }
 
     @Override
     public List<User> getList() {
         
         try {
             List<User> users = sessionFactory.getCurrentSession().createQuery("from User order by id").list();
         
             return users;
         } catch (Exception e) {
             System.out.println(e.getMessage());
             return null;
         }
     }    
 }
