 /*
  * Copyright 2008 The MxUpdate Team
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  * Revision:        $Rev$
  * Last Changed:    $Date$
  * Last Changed By: $Author$
  */
 
 package net.sourceforge.mxupdate.update.program;
 
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.Writer;
 import java.util.Collection;
 import java.util.Set;
 import java.util.TreeSet;
 
 import matrix.db.Context;
 import matrix.util.MatrixException;
 
 import net.sourceforge.mxupdate.update.AbstractObject_mxJPO;
 import net.sourceforge.mxupdate.update.util.InfoAnno_mxJPO;
 import net.sourceforge.mxupdate.util.Mapping_mxJPO.AdminTypeDef;
 
 import org.xml.sax.SAXException;
 
 import static net.sourceforge.mxupdate.update.util.StringUtil_mxJPO.match;
 import static net.sourceforge.mxupdate.util.MqlUtil_mxJPO.execMql;
 
 /**
  * @author tmoxter
  * @version $Id$
  */
 @InfoAnno_mxJPO(adminType = AdminTypeDef.Program)
 public class Program_mxJPO
         extends AbstractObject_mxJPO
 {
     /**
      * Defines the serialize version unique identifier.
      */
     private static final long serialVersionUID = -3329894042318127257L;
 
     /**
      * Evaluates for given collection of string which programs are matching
      * returns them as set. Only programs which are not JPOs are returned.
      *
      * @param _context          context for this request
      * @param _matches          collection of strings which must match
      */
     @Override
     public Set<String> getMatchingNames(final Context _context,
                                         final Collection<String> _matches)
             throws MatrixException
     {
         final StringBuilder cmd = new StringBuilder()
                 .append("list program * select name isjavaprogram dump \"\t\"");
         final Set<String> ret = new TreeSet<String>();
         for (final String name : execMql(_context, cmd).split("\n"))  {
             final String[] nameArr = name.split("\t");
             if (!"TRUE".equals(nameArr[1]))  {
                 for (final String match : _matches)  {
                     if (match(nameArr[0], match))  {
                         ret.add(nameArr[0]);
                     }
                 }
             }
         }
         return ret;
     }
 
     /**
      * Exports given program to given path for given name.
      *
      * @param _context          context for this request
      * @param _path             export path
      * @param _name             name of JPO to export
      */
     @Override
     public void export(final Context _context,
                        final File _path,
                        final String _name)
             throws MatrixException, SAXException, IOException
     {
         final StringBuilder cmd = new StringBuilder()
                 .append("print program \"").append(_name).append("\" select code dump");
 
         final File file = new File(_path, _name);
         if (!file.getParentFile().exists())  {
             file.getParentFile().mkdirs();
         }
         final Writer out = new FileWriter(file);
         out.append(execMql(_context, cmd));
         out.flush();
         out.close();
     }
 
     /**
      * The program is updated if the modified date of the file is not the same
      * as the the version property.
      *
      * @param _context          context for this request
      * @param _name             name of the administration (business) object
      * @param _file             reference to the file to update
      * @param _newVersion       new version which must be set within the update
      *                          (or <code>null</code> if the version must not
      *                          be set).
      * @throws Exception if update of the program failed
      */
     @Override
     public void update(final Context _context,
                        final String _name,
                        final File _file,
                        final String _newVersion)
             throws Exception
     {
         // not equal => update JPO code and version
         final StringBuilder cmd = new StringBuilder()
                 .append("mod prog \"").append(_name)
                         .append("\" file \"").append(_file.getPath()).append("\";\n");
         if (_newVersion != null)  {
             cmd.append("mod prog \"").append(_name)
                .append("\" add property version value \"").append(_newVersion).append("\";");
         }
         execMql(_context, cmd);
     }
 }
