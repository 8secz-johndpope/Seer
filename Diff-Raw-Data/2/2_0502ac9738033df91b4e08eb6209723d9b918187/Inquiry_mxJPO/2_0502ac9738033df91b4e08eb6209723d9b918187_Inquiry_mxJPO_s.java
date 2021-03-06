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
 
 package net.sourceforge.mxupdate.update.userinterface;
 
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.Writer;
 import java.util.HashMap;
 import java.util.Map;
 
 import matrix.db.Context;
 
 import static net.sourceforge.mxupdate.update.util.StringUtil_mxJPO.convert;
 
 /**
  *
  * @author tmoxter
  * @version $Id$
  */
 @net.sourceforge.mxupdate.update.util.InfoAnno_mxJPO(adminType = "inquiry",
                                                      filePrefix = "INQUIRY_",
                                                      fileSuffix = ".tcl",
                                                      filePath = "userinterface/inquiry",
                                                      description = "inquiry")
 public class Inquiry_mxJPO
        extends net.sourceforge.mxupdate.update.AbstractAdminObject_mxJPO
 {
     /**
      * Separator used between the inquiry update statements and the inquiry
      * code itself.
      */
     private final static String INQUIRY_SEPARATOR
         = "################################################################################\n"
         + "# INQUIRY CODE                                                                 #\n"
         + "################################################################################";
 
 
     /**
      * Code for the inquiry.
      */
     private String code = null;
 
     /**
      * Format for the inquiry.
      */
     private String format = null;
 
     /**
      * Pattern for the inquiry.
      */
     private String pattern = null;
 
     @Override
     protected void parse(final String _url,
                          final String _content)
     {
         if ("/code".equals(_url))  {
             this.code = _content;
         } else if ("/fmt".equals(_url))  {
             this.format = _content;
         } else if ("/pattern".equals(_url))  {
             this.pattern = _content;
         } else  {
             super.parse(_url, _content);
         }
     }
 
     @Override
     protected void writeObject(Writer _out) throws IOException
     {
         _out.append(" \\\n    pattern \"").append(convert(this.pattern)).append("\"")
             .append(" \\\n    format \"").append(convert(this.format)).append("\"")
             .append(" \\\n    file \"${FILE}\"");
         for (final net.sourceforge.mxupdate.update.AbstractAdminObject_mxJPO.Property prop : this.getPropertiesMap().values())  {
             if (prop.getName().startsWith("%"))  {
                 _out.append(" \\\n    add argument \"").append(convert(prop.getName().substring(1))).append("\"")
                     .append(" \"").append(convert(prop.getValue())).append("\"");
             }
         }
    }
 
     @Override
     protected void writeEnd(final Writer _out)
             throws IOException
     {
         _out.append("\n\n# do not change the next three lines, they are needed as separator information:\n")
             .append(INQUIRY_SEPARATOR)
            .append('\n').append(this.code);
     }
 
     /**
      * Updates this inquiry. Because the TCL source code of an inquiry includes
      * also the inquiry code itself, this inquiry code must be separated and
      * written in a temporary file. This temporary file is used while the
      * update is running (defined via TCL variable <code>FILE</code>). After
      * the update, the temporary file is removed (because not needed anymore).
      *
      * @param _context          context for this request
      * @param _newVersion       new version string
      * @param _code             code for the update
      * @throws Exception if update failed
      */
     @Override
     protected void update(final Context _context,
                           final CharSequence _preCode,
                           final CharSequence _code,
                           final Map<String,String> _tclVariables)
             throws Exception
     {
         // separate the inquiry code and the TCL code
         final int idx = _code.toString().lastIndexOf(INQUIRY_SEPARATOR);
         final CharSequence code = (idx >= 0)
                                   ? _code.subSequence(0, idx)
                                   : _code;
         final CharSequence inqu = (idx >= 0)
                                   ? _code.subSequence(idx + INQUIRY_SEPARATOR.length() + 1, _code.length())
                                   : "";
 
         final File tmpFile = File.createTempFile("TMP_", ".inquiry");
 
         try  {
             // write inquiry code
             final Writer out = new FileWriter(tmpFile);
             out.append(inqu.toString().trim());
             out.flush();
             out.close();
 
             // and update
             final Map<String,String> tclVariables = new HashMap<String,String>();
             tclVariables.putAll(_tclVariables);
             tclVariables.put("FILE", tmpFile.getPath());
             super.update(_context, _preCode, code, tclVariables);
         } finally  {
             tmpFile.delete();
         }
     }
 
     /**
      * Appends the MQL statement to reset this inquiry:
      * <ul>
      * <li>reset the description pattern and code</li>
      * <li>remove all arguments and arguments</li>
      * </ul>
      *
      * @param _cmd      string builder used to append the MQL statements
      */
     @Override
     protected void appendResetMQL(final StringBuilder _cmd)
     {
         _cmd.append("mod ").append(getInfoAnno().adminType())
             .append(" \"").append(getName()).append('\"')
             .append(" description \"\" pattern \"\" code \"\"");
         // reset arguments
         for (final net.sourceforge.mxupdate.update.AbstractAdminObject_mxJPO.Property prop : this.getPropertiesMap().values())  {
             if (prop.getName().startsWith("%"))  {
                 _cmd.append(" remove argument \"").append(prop.getName().substring(1)).append('\"');
             }
         }
         // reset properties
         this.appendResetProperties(_cmd);
     }
 }
