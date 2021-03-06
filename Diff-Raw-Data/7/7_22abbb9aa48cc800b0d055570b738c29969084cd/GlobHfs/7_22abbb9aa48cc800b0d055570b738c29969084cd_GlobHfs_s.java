 /*
  * Copyright (c) 2007-2010 Concurrent, Inc. All Rights Reserved.
  *
  * Project and contact information: http://www.cascading.org/
  *
  * This file is part of the Cascading project.
  *
  * Cascading is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * Cascading is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Cascading.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package cascading.tap;
 
 import java.beans.ConstructorProperties;
 import java.io.IOException;
 import java.util.ArrayList;
import java.util.LinkedHashSet;
 import java.util.List;
import java.util.Set;
 
 import cascading.scheme.Scheme;
 import org.apache.hadoop.fs.FileStatus;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.fs.PathFilter;
 import org.apache.hadoop.mapred.JobConf;
 
 /**
  * Class GlobHfs is a type of {@link MultiSourceTap} that accepts Hadoop style 'file globing' expressions so
  * multiple files that match the given pattern may be used as the input sources for a given {@link cascading.flow.Flow}.
  * <p/>
  * See {@link FileSystem#globStatus(org.apache.hadoop.fs.Path)} for details on the globing syntax. But in short
  * it is similar to standard regular expressions except alternation is done via {foo,bar} instead of (foo|bar).
  * <p/>
  * Note that a {@link cascading.flow.Flow} sourcing from GlobHfs is not currently compatible with the {@link cascading.cascade.Cascade}
  * scheduler. GlobHfs expects the files and paths to exist so the wildcards can be resolved into concrete values so
  * that the scheduler can order the Flows properly.
  * <p/>
  * Note that globing can match files or directories. It may consume less resources to match directories and let
  * Hadoop include all sub-files immediately contained in the directory instead of enumerating every individual file.
  * Ending the glob path with a {@code /} should match only directories.
  *
  * @see Hfs
  * @see MultiSourceTap
  * @see FileSystem
  */
 public class GlobHfs extends MultiSourceTap
   {
   /** Field pathPattern */
   private String pathPattern;
   /** Field pathFilter */
   private PathFilter pathFilter;
 
   /**
    * Constructor GlobHfs creates a new GlobHfs instance.
    *
    * @param scheme      of type Scheme
    * @param pathPattern of type String
    */
   @ConstructorProperties({"scheme", "pathPattern"})
   public GlobHfs( Scheme scheme, String pathPattern )
     {
     this( scheme, pathPattern, null );
     }
 
   /**
    * Constructor GlobHfs creates a new GlobHfs instance.
    *
    * @param scheme      of type Scheme
    * @param pathPattern of type String
    * @param pathFilter  of type PathFilter
    */
   @ConstructorProperties({"scheme", "pathPattern", "pathFilter"})
   public GlobHfs( Scheme scheme, String pathPattern, PathFilter pathFilter )
     {
     super( scheme );
     this.pathPattern = pathPattern;
     this.pathFilter = pathFilter;
     }
 
   @Override
   protected Tap[] getTaps()
     {
     if( taps != null )
       return taps;
 
     try
       {
       taps = makeTaps( new JobConf() );
       }
     catch( IOException exception )
       {
       throw new TapException( "unable to resolve taps for globbing path: " + pathPattern );
       }
 
     return taps;
     }
 
   private Tap[] makeTaps( JobConf conf ) throws IOException
     {
     FileStatus[] statusList = null;
 
     Path path = new Path( pathPattern );
 
     FileSystem fileSystem = path.getFileSystem( conf );
 
     if( pathFilter == null )
       statusList = fileSystem.globStatus( path );
     else
       statusList = fileSystem.globStatus( path, pathFilter );
 
     if( statusList == null || statusList.length == 0 )
       throw new TapException( "unable to find paths matching path pattern: " + pathPattern );
 
     List<Hfs> notEmpty = new ArrayList<Hfs>();
 
     for( int i = 0; i < statusList.length; i++ )
       {
       if( statusList[ i ].getLen() != 0 )
         notEmpty.add( new Hfs( getScheme(), statusList[ i ].getPath().toString() ) );
       }
 
     return notEmpty.toArray( new Tap[ notEmpty.size() ] );
     }
 
   @Override
   public void sourceInit( JobConf conf ) throws IOException
     {
     taps = makeTaps( conf );
     super.sourceInit( conf );
     }
 
   @Override
   public boolean equals( Object object )
     {
     if( this == object )
       return true;
     if( object == null || getClass() != object.getClass() )
       return false;
 
     GlobHfs globHfs = (GlobHfs) object;
 
     // do not compare tap arrays, these values should be sufficient to show identity
     if( getScheme() != null ? !getScheme().equals( globHfs.getScheme() ) : globHfs.getScheme() != null )
       return false;
     if( pathFilter != null ? !pathFilter.equals( globHfs.pathFilter ) : globHfs.pathFilter != null )
       return false;
     if( pathPattern != null ? !pathPattern.equals( globHfs.pathPattern ) : globHfs.pathPattern != null )
       return false;
 
     return true;
     }
 
   @Override
   public int hashCode()
     {
     int result = pathPattern != null ? pathPattern.hashCode() : 0;
     result = 31 * result + ( pathFilter != null ? pathFilter.hashCode() : 0 );
     return result;
     }
 
   @Override
   public String toString()
     {
     return "GlobHfs[" + pathPattern + ']';
     }
   }
