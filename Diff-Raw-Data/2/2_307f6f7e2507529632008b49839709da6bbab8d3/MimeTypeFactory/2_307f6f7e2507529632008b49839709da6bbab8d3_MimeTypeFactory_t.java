 /**
 * Copyright (C) 2012  Cedric Cheneau
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
 package net.holmes.core.util.mimetype;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Properties;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public final class MimeTypeFactory implements IMimeTypeFactory {
     private static Logger logger = LoggerFactory.getLogger(MimeTypeFactory.class);
 
     private Properties properties = null;
 
     public MimeTypeFactory() {
         // Load mime types from property file
         properties = new Properties();
         InputStream in = null;
         try {
             in = this.getClass().getResourceAsStream("/mimetypes.properties");
             properties.load(in);
         }
         catch (IOException e) {
             logger.error(e.getMessage(), e);
         }
         finally {
             try {
                 if (in != null) in.close();
             }
             catch (IOException e) {
                 logger.error(e.getMessage(), e);
             }
         }
     }
 
     /* (non-Javadoc)
      * @see net.holmes.core.util.mimetype.IMimeTypeFactory#getMimeType(java.lang.String)
      */
     @Override
     public MimeType getMimeType(String fileName) {
         // Get file extension
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length()).toLowerCase();
 
         // Get mime type
         return properties.getProperty(ext) == null ? null : new MimeType(properties.getProperty(ext));
     }
 }
