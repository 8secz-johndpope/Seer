 /**
  * Flexmojos is a set of maven goals to allow maven users to compile, optimize and test Flex SWF, Flex SWC, Air SWF and Air SWC.
  * Copyright (C) 2008-2012  Marvin Froeder <marvin@flexmojos.net>
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.sonatype.flexmojos.util;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.List;
 
 /**
  * this class provides functions used to generate a relative path from two absolute paths
  * 
  * @author Marvin H. Froeder
  * @author David M. Howard
  */
 public class PathUtil
 {
 
     private PathUtil()
     {
         super();
     }
 
     static
     {
         new PathUtil();
     }
 
     public static boolean existAll( File... files )
     {
         if ( files == null )
         {
             return false;
         }
         if ( files.length == 0 )
         {
             return false;
         }
 
         for ( File file : files )
         {
             if ( !file.exists() )
             {
                 return false;
             }
         }
         return true;
     }
 
     public static boolean existAll( List<String> paths )
     {
         if ( paths == null )
         {
             return false;
         }
 
         return existAll( files( paths ) );
     }
 
     public static boolean existAny( File... files )
     {
         if ( files == null )
         {
             return false;
         }
         if ( files.length == 0 )
         {
             return false;
         }
 
         for ( File file : files )
         {
             if ( file.exists() )
             {
                 return true;
             }
         }
         return false;
     }
 
     public static File file( File file )
     {
         if ( file == null )
         {
             return null;
         }
 
         try
         {
             return file.getCanonicalFile();
         }
         catch ( IOException e )
         {
             return file.getAbsoluteFile();
         }
     }
 
     public static File file( String path, String basedir )
     {
         return file( path, file( basedir ) );
     }
 
     public static File file( String path, File basedir )
     {
         if ( path == null )
         {
             return null;
         }
 
         File file = new File( path );
         if ( !file.isAbsolute() )
         {
             file = new File( basedir, path );
         }
 
         return file( file );
     }
 
     public static File file( String path, File... basedirs )
     {
         if ( path == null )
         {
             return null;
         }
 
         return file( path, Arrays.asList( basedirs ) );
     }
 
    public static File[] files( String[] paths, List<File> basedirs )
    {
        if ( paths == null )
        {
            return null;
        }

        File[] files = new File[paths.length];
        for ( int i = 0; i < paths.length; i++ )
        {
            files[i] = file( paths[i], basedirs );
        }
        return files;
    }

     public static File file( String path, List<File> basedirs )
     {
         if ( path == null )
         {
             return null;
         }
 
         File file = new File( path );
 
         if ( file.isAbsolute() )
         {
             return file;
         }
 
         for ( File basedir : basedirs )
         {
             file = file( path, basedir );
             if ( file.exists() )
             {
                 return file;
             }
         }
 
         return null;
     }
 
     public static Collection<File> files( List<String> paths, File basedir )
     {
         if ( paths == null )
         {
             return null;
         }
 
         List<File> files = new ArrayList<File>();
         for ( String path : paths )
         {
             files.add( file( path, basedir ) );
         }
 
         return files;
     }
 
     public static Collection<File> files( String[] paths, File basedir )
     {
         if ( paths == null )
         {
             return null;
         }
 
         return files( Arrays.asList( paths ), basedir );
     }
 
     public static String path( File file )
     {
         if ( file == null )
         {
             return null;
         }
 
         try
         {
             return file.getCanonicalPath();
         }
         catch ( IOException e )
         {
             return file.getAbsolutePath();
         }
     }
 
     public static String[] paths( Collection<File> files )
     {
         if ( files == null )
         {
             return null;
         }
 
         return paths( files.toArray( new File[files.size()] ) );
     }
 
     public static String[] paths( File... files )
     {
         if ( files == null )
         {
             return null;
         }
 
         String[] paths = new String[files.length];
         for ( int i = 0; i < paths.length; i++ )
         {
             paths[i] = path( files[i] );
         }
         return paths;
     }
 
     public static List<String> pathsList( File[] files )
     {
         if ( files == null )
         {
             return null;
         }
         return Arrays.asList( paths( files ) );
     }
 
     public static String pathString( File[] files )
     {
         if ( files == null )
         {
             return null;
         }
 
         StringBuilder paths = new StringBuilder();
         for ( File file : files )
         {
             if ( paths.length() != 0 )
             {
                 paths.append( File.pathSeparatorChar );
             }
             paths.append( path( file ) );
         }
         return paths.toString();
     }
 
     public static File[] existingFiles( Collection<String> paths )
     {
         if ( paths == null )
         {
             return null;
         }
 
         return existingFilesList( paths ).toArray( new File[0] );
     }
 
     public static File[] existingFiles( File... files )
     {
         if ( files == null )
         {
             return null;
         }
 
         return existingFilesList( Arrays.asList( files ) ).toArray( new File[0] );
     }
 
     public static List<File> existingFilesList( Collection<String> paths )
     {
         if ( paths == null )
         {
             return null;
         }
 
         return existingFilesList( filesList( paths ) );
     }
 
     public static List<File> existingFilesList( List<File> files )
     {
         if ( files == null )
         {
             return null;
         }
 
         files = new ArrayList<File>( files );
         for ( Iterator<File> iterator = files.iterator(); iterator.hasNext(); )
         {
             File file = (File) iterator.next();
             if ( !file.exists() )
             {
                 iterator.remove();
             }
         }
 
         return files;
     }
 
     public static File file( String path )
     {
         if ( path == null )
         {
             return null;
         }
 
         return file( new File( path ) );
     }
 
     public static File[] files( Collection<String> paths )
     {
         if ( paths == null )
         {
             return null;
         }
 
         File[] files = new File[paths.size()];
         int i = 0;
         for ( String path : paths )
         {
             files[i++] = file( new File( path ) );
         }
 
         return files;
     }
 
     public static File[] files( String... paths )
     {
         if ( paths == null )
         {
             return null;
         }
 
         return files( Arrays.asList( paths ) );
     }
 
     public static List<File> filesList( Collection<String> paths )
     {
         if ( paths == null )
         {
             return null;
         }
 
         return Arrays.asList( files( paths ) );
     }
 
     /**
      * break a path down into individual elements and add to a list. example : if a path is /a/b/c/d.txt, the breakdown
      * will be [d.txt,c,b,a]
      * 
      * @param f input file
      * @return a List collection with the individual elements of the path in reverse order
      */
     private static List<String> pathList( File f )
     {
         List<String> l = new ArrayList<String>();
         File r = file( f );
         while ( r != null )
         {
             l.add( r.getName() );
             r = r.getParentFile();
         }
         return l;
     }
 
     /**
      * get relative path of File 'f' with respect to 'home' directory example : home = /a/b/c f = /a/d/e/x.txt s =
      * getRelativePath(home,f) = ../../d/e/x.txt
      * 
      * @param home base path, should be a directory, not a file, or it doesn't make sense
      * @param f file to generate path for
      * @return path from home to f as a string
      */
     public static String relativePath( File home, File f )
     {
         List<String> homelist = pathList( home );
         List<String> filelist = pathList( f );
         return matchPathLists( homelist, filelist ).replace( '\\', '/' );
     }
 
     /**
      * figure out a string representing the relative path of 'f' with respect to 'r'
      * 
      * @param r home path
      * @param f path of file
      */
     private static String matchPathLists( List<String> r, List<String> f )
     {
         int i;
         int j;
         String s;
         // start at the beginning of the lists
         // iterate while both lists are equal
         s = "";
         i = r.size() - 1;
         j = f.size() - 1;
 
         // first eliminate common root
         while ( ( i >= 0 ) && ( j >= 0 ) && ( r.get( i ).equals( f.get( j ) ) ) )
         {
             i--;
             j--;
         }
 
         // for each remaining level in the home path, add a ..
         for ( ; i >= 0; i-- )
         {
             s += ".." + File.separator;
         }
 
         // for each level in the file path, add the path
         for ( ; j >= 1; j-- )
         {
             s += f.get( j ) + File.separator;
         }
 
         // file name
         s += f.get( j );
         return s;
     }
 
     public static String fileExtention( File file )
     {
         if ( file == null )
         {
             return null;
         }
 
         String path = file.getName();
 
         String[] doted = path.split( "\\." );
         if ( doted.length == 1 )
         {
             return "";
         }
 
         if ( "gz".equals( doted[doted.length - 1] ) || "bz2".equals( doted[doted.length - 1] ) )
         {
             if ( doted.length > 2 && "tar".equals( doted[doted.length - 2].toLowerCase() ) )
             {
                 return "tar." + doted[doted.length - 1];
             }
         }
 
         return doted[doted.length - 1];
     }
 
 }
