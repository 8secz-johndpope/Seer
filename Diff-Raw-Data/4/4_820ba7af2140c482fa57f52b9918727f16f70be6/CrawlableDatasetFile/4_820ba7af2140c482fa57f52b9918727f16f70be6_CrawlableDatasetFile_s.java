 package thredds.crawlabledataset;
 
 import java.io.File;
 import java.io.IOException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.*;
 
 /**
  * An implementation of CrawlableDataset where the dataset being represented
  * is a local file (java.io.File).
  *
  * <p>The constructor extends the allowed form of a CrawlableDataset path to
  * allow file paths to be given in their native formats including Unix
  * (/my/file), Windows (c:\my\file), and UNC file paths (\\myhost\my\file).
  * However, the resulting CrawlableDataset path is normalized to conform to the
  * allowed form of the CrawlableDataset path.
  *
  * <p>This is the default implementation of CrawlableDataset used by
  * CrawlableDatasetFactory if the class name handed to the
  * createCrawlableDataset() method is null.</p>
  *
  * @author edavis
  * @since Jun 8, 2005 15:34:04 -0600
  */
 public class CrawlableDatasetFile implements CrawlableDataset
 {
   static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CrawlableDatasetFile.class);
   //private static Log log = LogFactory.getLog( CrawlableDatasetFile.class );
 
   private final File file;
   private final String path;
   private final String name;
 
   private final Object configObj;
 
   //protected CrawlableDatasetFile() {}
 
   /**
    * Constructor required by CrawlableDatasetFactory.
    *
    * @param path the path of the CrawlableDataset being constructed.
    * @param configObj the configuration object required by CrawlableDatasetFactory; it is ignored.
    */
   CrawlableDatasetFile( String path, Object configObj )
   {
     if ( path.startsWith( "file:" ) )
     {
       try
       {
         this.file = new File( new URI( path ) );
       }
       catch ( URISyntaxException e )
       {
         String tmpMsg = "Bad URI syntax for path <" + path + ">: " + e.getMessage();
         log.debug( "CrawlableDatasetFile(): " + tmpMsg );
         throw new IllegalArgumentException( tmpMsg );
       }
     }
     else
     {
       file = new File( path );
     }
 
     this.path = this.normalizePath( path );
     this.name = this.file.getName();
 
     if ( configObj != null )
     {
       log.warn( "CrawlableDatasetFile(): config object not null, it will be ignored <" + configObj.toString() + ">.");
       this.configObj = configObj;
     }
     else
       this.configObj = null;
   }
 
   private CrawlableDatasetFile( CrawlableDatasetFile parent, String childPath )
   {
     String normalChildPath = this.normalizePath( childPath );
     if ( normalChildPath.startsWith( "/"))
       normalChildPath = normalChildPath.substring( 1);
     this.path = parent.getPath() + "/" + normalChildPath;
     this.file = new File( parent.getFile(), normalChildPath );
     this.name = this.file.getName();
 
     this.configObj = null;
   }
 
   private CrawlableDatasetFile( File file )
   {
     this.file = file;
     this.path = this.normalizePath( file.getPath() );
     this.name = this.file.getName();
     this.configObj = null;
   }
 
   /**
    * Normalize the given path so that it can be used in the creation of a CrawlableDataset.
    * This method can be used on absolute or relative paths.
    * <p/>
    * Normal uses slashes ("/") as path seperator, not backslashes ("\"), and does
    * not use trailing slashes. This function allows users to specify Windows
    * pathnames and UNC pathnames in there normal manner.
    *
    * @param path the path to be normalized.
    * @return the normalized path.
    * @throws NullPointerException if path is null.
   *
   * @see {@link CrawlableDatasetFactory.normalizePath(String) CrawlableDatasetFactory.normalizePath()}
    */
   private String normalizePath( String path )
   {
     // Replace any occurance of a backslash ("\") with a slash ("/").
     // NOTE: Both String and Pattern escape backslash, so need four backslashes to find one.
     // NOTE: No longer replace multiple backslashes with one slash, which allows for UNC pathnames (Windows LAN addresses).
     //       Was path.replaceAll( "\\\\+", "/");
     String newPath = path.replaceAll( "\\\\", "/" );
 
     // Remove trailing slashes.
     while ( newPath.endsWith( "/" ) && ! newPath.equals( "/" ) )
       newPath = newPath.substring( 0, newPath.length() - 1 );
 
     return newPath;
   }
 
   /**
    * Provide access to the java.io.File that this CrawlableDataset represents.
    *
    * @return the java.io.File that this CrawlableDataset represents.
    */
   public File getFile()
   {
     return file;
   }
 
   public Object getConfigObject()
   {
     return configObj;
   }
 
   public String getPath()
   {
     return( this.path);
   }
 
   public String getName()
   {
     return( this.name);
   }
 
   public boolean exists()
   {
     return file.exists();
   }
 
   public boolean isCollection()
   {
     return( file.isDirectory());
   }
 
   public CrawlableDataset getDescendant( String relativePath)
   {
     return new CrawlableDatasetFile( this, relativePath );
   }
 
   public List listDatasets() throws IOException
   {
     if ( ! this.isCollection() )
     {
       String tmpMsg = "This dataset <" + this.getPath() + "> is not a collection dataset.";
       log.error( "listDatasets(): " + tmpMsg);
       throw new IllegalStateException( tmpMsg );
     }
 
     File[] allFiles = this.file.listFiles();
     List list = new ArrayList();
     for ( int i = 0; i < allFiles.length; i++ )
     {
       list.add( new CrawlableDatasetFile( this, allFiles[i].getName() ) );
     }
 
     return ( list );
   }
 
   public List listDatasets( CrawlableDatasetFilter filter ) throws IOException
   {
     List list = this.listDatasets();
     if ( filter == null ) return list;
     List retList = new ArrayList();
     for ( Iterator it = list.iterator(); it.hasNext(); )
     {
       CrawlableDataset curDs = (CrawlableDataset) it.next();
       if ( filter.accept( curDs ) )
       {
         retList.add( curDs );
       }
     }
     return ( retList );
   }
 
   public CrawlableDataset getParentDataset()
   {
     File parentFile = this.file.getParentFile();
     if ( parentFile == null ) return null;
     return new CrawlableDatasetFile( parentFile );
   }
 
   public long length()
   {
     if ( this.isCollection()) return( 0);
     return( this.file.length());
   }
 
   public Date lastModified()
   {
     long lastModDate = this.file.lastModified();
     if ( lastModDate == 0 ) return null;
     
     Calendar cal = Calendar.getInstance();
     cal.clear();
     cal.setTimeInMillis( lastModDate );
     return( cal.getTime() );
   }
 
   public String toString()
   {
     return this.path;
   }
 }
