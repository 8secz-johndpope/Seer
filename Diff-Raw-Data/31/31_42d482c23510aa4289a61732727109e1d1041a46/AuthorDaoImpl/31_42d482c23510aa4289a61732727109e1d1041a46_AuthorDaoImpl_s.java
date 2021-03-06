 package com.andrada.sitracker.db.dao;
 
 import com.andrada.sitracker.db.beans.Author;
 import com.j256.ormlite.dao.BaseDaoImpl;
 import com.j256.ormlite.support.ConnectionSource;
 
 import java.sql.SQLException;
 import java.util.List;
 
 /**
  * Created by ggodonoga on 07/06/13.
  */
 public class AuthorDaoImpl extends BaseDaoImpl<Author, Integer>
         implements AuthorDao {
 
     public AuthorDaoImpl(ConnectionSource connectionSource)
             throws SQLException {
         super(connectionSource, Author.class);
     }
 
     @Override
     public int getNewAuthorsCount() throws SQLException {
 
         return (int) this.queryRawValue(
                "SELECT COUNT(*) FROM authors, publications WHERE authors.id = publications.author_id AND publications.isNew = 1");
     }
 
     @Override
     public List<Author> getAllAuthorsSorted() throws SQLException {
         return this.queryBuilder().orderBy("name", true).query();
     }
 
     @Override
     public void markAsRead(Author author) throws SQLException {
         author.setUpdated(false);
         this.update(author);
     }
 }
