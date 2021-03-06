 /*
  *  Weblounge: Web Content Management System
  *  Copyright (c) 2010 The Weblounge Team
  *  http://weblounge.o2it.ch
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2
  *  of the License, or (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software Foundation
  *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
 
 package ch.o2it.weblounge.common.impl.content.image;
 
 import ch.o2it.weblounge.common.content.ResourceURI;
 import ch.o2it.weblounge.common.content.image.ImageContent;
 import ch.o2it.weblounge.common.content.image.ImageResource;
 import ch.o2it.weblounge.common.impl.content.ResourceImpl;
 
 /**
  * Default implementation of an image resource.
  */
 public class ImageResourceImpl extends ResourceImpl<ImageContent> implements ImageResource {
 
   /**
    * Creates a new image with the given uri.
    * 
    * @param uri
    *          the image uri
    */
   public ImageResourceImpl(ResourceURI uri) {
     super(uri);
   }
 
   /**
    * {@inheritDoc}
    *
    * @see ch.o2it.weblounge.common.impl.content.ResourceImpl#addContent(ch.o2it.weblounge.common.content.ResourceContent)
    */
   @Override
   public void addContent(ImageContent content) {
     if (content == null)
       throw new IllegalArgumentException("Content must not be null");
     super.addContent(content);
   }
 
   /**
    * {@inheritDoc}
    *
    * @see ch.o2it.weblounge.common.impl.content.ResourceImpl#toXmlRootTag()
    */
   @Override
   protected String toXmlRootTag() {
     return TYPE;
   }
 
 }
