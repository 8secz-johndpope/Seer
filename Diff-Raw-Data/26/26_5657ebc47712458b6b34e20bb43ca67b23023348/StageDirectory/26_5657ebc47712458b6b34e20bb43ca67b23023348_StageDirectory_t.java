 package com.yamj.core.database.model;
 
 import javax.persistence.JoinColumn;
 import javax.persistence.ManyToOne;
 import org.hibernate.annotations.ForeignKey;
 
 import com.yamj.common.type.StatusType;
 import org.hibernate.annotations.Type;
 
 import java.util.HashSet;
 import java.util.Set;
 import javax.persistence.CascadeType;
 import javax.persistence.FetchType;
 import javax.persistence.OneToMany;
 
 import com.yamj.core.hibernate.usertypes.EnumStringUserType;
 import org.hibernate.annotations.Parameter;
 import org.hibernate.annotations.TypeDef;
 
 import java.util.Date;
 import javax.persistence.Temporal;
 import javax.persistence.TemporalType;
 
 import java.io.Serializable;
 import javax.persistence.Column;
 import javax.persistence.Entity;
 import javax.persistence.Table;
 import org.apache.commons.lang3.StringUtils;
 import org.hibernate.annotations.NaturalId;
 
 @TypeDef(name = "statusType",
         typeClass = EnumStringUserType.class,
         parameters = {
     @Parameter(name = "enumClassName", value = "com.yamj.common.type.StatusType")})
 @Entity
 @Table(name = "stage_directory")
 public class StageDirectory extends AbstractAuditable implements Serializable {
 
     private static final long serialVersionUID = 1706389732909764283L;
     @NaturalId
    @Column(name = "directory_path", nullable = false, length = 255)
     private String directoryPath;
     @Temporal(value = TemporalType.TIMESTAMP)
     @Column(name = "directory_date", nullable = false)
     private Date directoryDate;
     @Type(type = "statusType")
     @Column(name = "status", nullable = false, length = 30)
     private StatusType status;
     @NaturalId
     @ManyToOne(fetch = FetchType.LAZY)
     @ForeignKey(name = "FK_DIRECTORY_LIBRARY")
     @JoinColumn(name = "library_id", nullable = false)
     private Library library;
     @ManyToOne(fetch = FetchType.LAZY)
     @ForeignKey(name = "FK_DIRECTORY_PARENT")
     @JoinColumn(name = "parent_id")
     private StageDirectory parentDirectory;
     @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "stageDirectory")
     private Set<StageFile> stageFiles = new HashSet<StageFile>(0);
 
     // GETTER and SETTER
     public String getDirectoryPath() {
         return directoryPath;
     }
 
     public void setDirectoryPath(String directoryPath) {
         this.directoryPath = directoryPath;
     }
 
     public Date getDirectoryDate() {
         return directoryDate;
     }
 
     public void setDirectoryDate(Date directoryDate) {
         this.directoryDate = directoryDate;
     }
 
     public StatusType getStatus() {
         return status;
     }
 
     public void setStatus(StatusType status) {
         this.status = status;
     }
 
     public Library getLibrary() {
         return library;
     }
 
     public void setLibrary(Library library) {
         this.library = library;
     }
 
     public StageDirectory getParentDirectory() {
         return parentDirectory;
     }
 
     public void setParentDirectory(StageDirectory parentDirectory) {
         this.parentDirectory = parentDirectory;
     }
 
     public Set<StageFile> getStageFiles() {
         return stageFiles;
     }
 
     public void setStageFiles(Set<StageFile> stageFiles) {
         this.stageFiles = stageFiles;
     }
 
     // EQUALITY CHECKS
     @Override
     public int hashCode() {
         final int prime = 17;
         int result = 1;
         result = prime * result + (this.directoryPath == null ? 0 : this.directoryPath.hashCode());
         result = prime * result + (this.library == null ? 0 : this.library.hashCode());
         return result;
     }
 
     @Override
     public boolean equals(Object other) {
         if (this == other) {
             return true;
         }
         if (other == null) {
             return false;
         }
         if (!(other instanceof StageDirectory)) {
             return false;
         }
         StageDirectory castOther = (StageDirectory) other;
 
         if (!StringUtils.equals(this.directoryPath, castOther.directoryPath)) {
             return false;
         }
 
         if (this.library == null && castOther.library == null) {
             return true;
         }
         if (this.library == null) {
             return false;
         }
         if (castOther.library == null) {
             return false;
         }
         return this.library.equals(castOther.library);
     }
 
     @Override
     public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append("StageDirectory [ID=");
         sb.append(getId());
         sb.append(", directorPath=");
         sb.append(getDirectoryPath());
         sb.append(", directoryDate=");
         sb.append(getDirectoryDate());
         if (getLibrary() != null) {
             sb.append(", library=");
             sb.append(getLibrary().getId());
         }
         sb.append("]");
         return sb.toString();
     }
 }
