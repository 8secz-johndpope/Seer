 package org.springframework.data.jdbc.repositories;
 
 import org.springframework.data.domain.Persistable;
 
 import java.util.Date;
 import java.util.LinkedHashMap;
 import java.util.Map;
 
 /**
  * @author Tomasz Nurkiewicz
  * @since 12/17/12, 11:12 PM
  */
 public class Comment implements Persistable<Integer> {
 
 	private Integer id;
 
 	private String userName;
 
 	private String contents;
 
 	private Date createdTime;
 
 	private int favouriteCount;
 
 	public Comment(String userName, String contents, Date createdTime, int favouriteCount) {
 		this.userName = userName;
 		this.contents = contents;
 		this.createdTime = createdTime;
 		this.favouriteCount = favouriteCount;
 	}
 
 	public Comment(Integer id, String userName, String contents, Date createdTime, int favouriteCount) {
 		this(userName, contents, createdTime, favouriteCount);
 		this.id = id;
 	}
 
 	@Override
 	public Integer getId() {
 		return id;
 	}
 
 	public void setId(Integer id) {
 		this.id = id;
 	}
 
 	@Override
 	public boolean isNew() {
 		return id == null;
 	}
 
 	public String getUserName() {
 		return userName;
 	}
 
 	public void setUserName(String userName) {
 		this.userName = userName;
 	}
 
 	public String getContents() {
 		return contents;
 	}
 
 	public void setContents(String contents) {
 		this.contents = contents;
 	}
 
 	public Date getCreatedTime() {
 		return createdTime;
 	}
 
 	public void setCreatedTime(Date createdTime) {
 		this.createdTime = createdTime;
 	}
 
 	public int getFavouriteCount() {
 		return favouriteCount;
 	}
 
 	public void setFavouriteCount(int favouriteCount) {
 		this.favouriteCount = favouriteCount;
 	}
 
 	public Map<String, Object> toMap() {
 		Map<String, Object> mapping = new LinkedHashMap<String, Object>();
 		mapping.put("id", getId());
 		mapping.put("user_name", getUserName());
 		mapping.put("contents", getContents());
		mapping.put("created_time", new java.sql.Timestamp(getCreatedTime().getTime()));
 		mapping.put("favourite_count", getFavouriteCount());
 		return mapping;
 	}
 
 	@Override
 	public String toString() {
 		return "Comment(id=" + id + ", userName='" + userName + '\'' + ", contents='" + contents + '\'' + ", createdTime=" + createdTime + ", favouriteCount=" + favouriteCount + ')';
 	}
 
 	@Override
 	public boolean equals(Object o) {
 		if (this == o) return true;
 		if (o == null || getClass() != o.getClass()) return false;
 
 		Comment comment = (Comment) o;
 
 		if (favouriteCount != comment.favouriteCount) return false;
 		if (contents != null ? !contents.equals(comment.contents) : comment.contents != null) return false;
 		if (createdTime != null ? !createdTime.equals(comment.createdTime) : comment.createdTime != null) return false;
 		if (id != null ? !id.equals(comment.id) : comment.id != null) return false;
 		if (userName != null ? !userName.equals(comment.userName) : comment.userName != null) return false;
 
 		return true;
 	}
 
 	@Override
 	public int hashCode() {
 		int result = id != null ? id.hashCode() : 0;
 		result = 31 * result + (userName != null ? userName.hashCode() : 0);
 		result = 31 * result + (contents != null ? contents.hashCode() : 0);
 		result = 31 * result + (createdTime != null ? createdTime.hashCode() : 0);
 		result = 31 * result + favouriteCount;
 		return result;
 	}
 }
