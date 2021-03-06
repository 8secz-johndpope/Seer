 package com.bloatit.data;
 
 import javax.persistence.Basic;
 import javax.persistence.Column;
 import javax.persistence.Entity;
 import javax.persistence.Enumerated;
 import javax.persistence.OneToOne;
 
 import org.hibernate.HibernateException;
 import org.hibernate.Session;
 
 import com.bloatit.common.Log;
 import com.bloatit.framework.exceptions.NonOptionalParameterException;
 
 @Entity
 public class DaoFileMetadata extends DaoUserContent {
 
     public enum FileType {
         TEXT, HTML, TEX, PDF, ODT, DOC, BMP, JPG, PNG, SVG,
     }
 
     @Basic(optional = false)
     private String filename;
 
     @Basic(optional = false)
     private String directory;
 
     @Basic(optional = false)
     private int size;
 
     @Column(columnDefinition = "TEXT")
     private String shortDescription;
 
     @Basic(optional = false)
     @Enumerated
     private FileType type;
 
    @OneToOne(optional = true, mappedBy = "file")
     private DaoImage image;
 
     public static DaoFileMetadata createAndPersist(final DaoMember member,
                                                    final DaoUserContent relatedContent,
                                                    final String filename,
                                                    final String directory,
                                                    final FileType type,
                                                    final int size) {
         final Session session = SessionManager.getSessionFactory().getCurrentSession();
         final DaoFileMetadata file = new DaoFileMetadata(member, relatedContent, filename, directory, type, size);
         try {
             session.save(file);
         } catch (final HibernateException e) {
             session.getTransaction().rollback();
             Log.data().error(e);
             session.beginTransaction();
             throw e;
         }
         return file;
     }
 
     /**
      * @param member is the author (the one who uploaded the file)
      * @param relatedContent can be null. It is the content with which this file has been
      *        uploaded.
      * @param filename is the name of the file (with its extension, but without its whole
      *        folder path)
      * @param directory is the path of the directory where the file is.
      * @param type is the type of the file (found using its extension or mimetype)
      * @param size is the size of the file.
      */
     private DaoFileMetadata(final DaoMember member,
                             final DaoUserContent relatedContent,
                             final String filename,
                             final String directory,
                             final FileType type,
                             final int size) {
         super(member);
         if (filename == null || directory == null || type == null || filename.isEmpty() || directory.isEmpty()) {
             throw new NonOptionalParameterException();
         }
         this.size = size;
         this.filename = filename;
         this.directory = directory;
         this.type = type;
         this.shortDescription = null;
         if (relatedContent != null) {
             relatedContent.addFile(this);
         }
         // At the end to make sure the assignment are done.
         // It works only if equal is final !!
         if (equals(relatedContent)) {
             throw new IllegalArgumentException();
         }
     }
 
     /**
      * @param shortDescription the shortDescription to set
      */
     public final void setShortDescription(final String shortDescription) {
         this.shortDescription = shortDescription;
     }
 
     /**
      * Tells that the current File is an image. Used in DaoImage constructor.
      *
      * @param image the image to set.
      */
     void setImage(DaoImage image) {
         this.image = image;
     }
 
     /**
      * If the file is an image, it should be associated with a DaoImage object.
      *
      * @return the image object associated with this file. It can be null.
      */
     public DaoImage getImage() {
         return image;
     }
 
     /**
      * @return the the directory + filename.
      */
     public final String getFilePath() {
         return directory + filename;
     }
 
     /**
      * @return the shortDescription
      */
     public final String getShortDescription() {
         return shortDescription;
     }
 
     /**
      * @return the filename
      */
     public final String getFilename() {
         return filename;
     }
 
     /**
      * @return the directory
      */
     public final String getFolder() {
         return directory;
     }
 
     /**
      * @return the size
      */
     public final int getSize() {
         return size;
     }
 
     public FileType getType() {
         return type;
     }
 
     // ======================================================================
     // For hibernate mapping
     // ======================================================================
 
     protected DaoFileMetadata() {
         // for hibernate.
     }
 
     // ======================================================================
     // equals hashcode.
     // ======================================================================
 
     /*
      * (non-Javadoc)
      * @see java.lang.Object#hashCode()
      */
     @Override
     public final int hashCode() {
         final int prime = 31;
         int result = super.hashCode();
         result = prime * result + ((directory == null) ? 0 : directory.hashCode());
         result = prime * result + ((filename == null) ? 0 : filename.hashCode());
         return result;
     }
 
     /*
      * (non-Javadoc)
      * @see java.lang.Object#equals(java.lang.Object)
      */
     @Override
     public final boolean equals(final Object obj) {
         if (this == obj) {
             return true;
         }
         if (!super.equals(obj)) {
             return false;
         }
         if (getClass() != obj.getClass()) {
             return false;
         }
         final DaoFileMetadata other = (DaoFileMetadata) obj;
         if (directory == null) {
             if (other.directory != null) {
                 return false;
             }
         } else if (!directory.equals(other.directory)) {
             return false;
         }
         if (filename == null) {
             if (other.filename != null) {
                 return false;
             }
         } else if (!filename.equals(other.filename)) {
             return false;
         }
         return true;
     }
 
 }
