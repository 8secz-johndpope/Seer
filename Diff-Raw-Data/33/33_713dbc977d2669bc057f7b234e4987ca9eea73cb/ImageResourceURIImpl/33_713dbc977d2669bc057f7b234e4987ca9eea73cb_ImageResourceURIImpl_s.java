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
 
 import ch.o2it.weblounge.common.content.MalformedResourceURIException;
 import ch.o2it.weblounge.common.content.ResourceURI;
 import ch.o2it.weblounge.common.content.image.ImageResource;
 import ch.o2it.weblounge.common.impl.content.ResourceURIImpl;
 import ch.o2it.weblounge.common.request.WebloungeRequest;
 import ch.o2it.weblounge.common.site.Site;
 
 /**
  * This a <code>ResourceURI</code> intended to represent images of type
  * <code>ch.o2it.weblounge.common.content.image.ImageResource</code>.
  */
 public class ImageResourceURIImpl extends ResourceURIImpl {
 
   /** The serial version uid */
   private static final long serialVersionUID = -4786684576702578116L;
 
   /**
    * Creates a new {@link PageURI} from the given request, which is used to
    * determine <code>site</code>, <code>path</code> and <code>version</code>.
    * 
    * @param request
    *          the request
    */
   public ImageResourceURIImpl(WebloungeRequest request) {
     super(ImageResource.TYPE, request.getSite(), request.getUrl().getPath(), null, request.getVersion());
   }
 
   /**
    * Creates a new {@link ResourceURI} that is equal to <code>uri</code> except
    * for the version which is switched to <code>version</code>.
    * 
    * @param uri
    *          the uri
    * @param version
    *          the version
    */
   public ImageResourceURIImpl(ResourceURI uri, long version) {
    super(uri, version);
    setType(ImageResource.TYPE);
   }
 
   /**
    * Creates a new {@link ResourceURI} pointing to the live version of the image
    * identified by <code>site</code>.
    * <p>
    * <b>Note:</b> Make sure to set <code>id</code> or <code>path</code> prior to
    * the first use of this uri.
    * 
    * @param site
    *          the site
    * @throws MalformedResourceURIException
    *           if the uri cannot be created. Usually, this is due to a malformed
    *           <code>path</code> parameter
    */
   public ImageResourceURIImpl(Site site) throws MalformedResourceURIException {
     super(ImageResource.TYPE, site, null);
   }
 
   /**
    * Creates a new {@link ResourceURI} pointing to the live version of the image
    * identified by <code>site</code> and <code>path</code>.
    * 
    * @param site
    *          the site
    * @param path
    *          the path
    * @throws MalformedResourceURIException
    *           if the uri cannot be created. Usually, this is due to a malformed
    *           <code>path</code> parameter
    */
   public ImageResourceURIImpl(Site site, String path)
       throws MalformedResourceURIException {
     super(ImageResource.TYPE, site, path);
   }
 
   /**
    * Creates a new {@link ResourceURI} pointing to a specific version of the
    * image identified by <code>site</code>, <code>path</code> and
    * <code>version</code> .
    * 
    * @param site
    *          the site
    * @param path
    *          the path
    * @param version
    *          the version
    * @throws MalformedResourceURIException
    *           if the uri cannot be created. Usually, this is due to a malformed
    *           <code>path</code> parameter
    */
   public ImageResourceURIImpl(Site site, String path, long version)
       throws MalformedResourceURIException {
     super(ImageResource.TYPE, site, path, version);
   }
 
   /**
    * Creates a new {@link ResourceURI} pointing to a specific version of the
    * image identified by <code>id<code>, <code>site</code>, <code>path</code>
    * and <code>version</code>.
    * 
    * @param site
    *          the site
    * @param path
    *          the path
    * @param id
    *          the image identifier
    * @throws MalformedResourceURIException
    *           if the uri cannot be created. Usually, this is due to a malformed
    *           <code>path</code> parameter
    */
   public ImageResourceURIImpl(Site site, String path, String id)
       throws MalformedResourceURIException {
     super(ImageResource.TYPE, site, path, id);
   }
 
   /**
    * Creates a new {@link ResourceURI} pointing to a specific version of the
    * image identified by <code>id<code>, <code>site</code>, <code>path</code>
    * and <code>version</code>.
    * 
    * @param site
    *          the site
    * @param path
    *          the path
    * @param id
    *          the image identifier
    * @param version
    *          the version
    * @throws MalformedResourceURIException
    *           if the uri cannot be created. Usually, this is due to a malformed
    *           <code>path</code> parameter
    */
   public ImageResourceURIImpl(Site site, String path, String id, long version)
       throws MalformedResourceURIException {
     super(ImageResource.TYPE, site, path, id, version);
   }
 
 }
