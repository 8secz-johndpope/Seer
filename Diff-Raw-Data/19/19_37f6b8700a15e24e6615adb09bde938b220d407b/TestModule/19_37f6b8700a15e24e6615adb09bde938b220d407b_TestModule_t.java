 /**
 * Copyright (c) 2012 Cedric Cheneau
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
 package net.holmes.core;
 
 import net.holmes.core.configuration.IConfiguration;
 import net.holmes.core.configuration.TestConfiguration;
 import net.holmes.core.media.IMediaService;
 import net.holmes.core.media.MediaService;
import net.holmes.core.media.index.IMediaIndex;
import net.holmes.core.media.index.MediaIndex;
 import net.holmes.core.util.mimetype.IMimeTypeFactory;
 import net.holmes.core.util.mimetype.MimeTypeFactory;
import net.holmes.core.util.resource.IResource;
import net.holmes.core.util.resource.Resource;
 
 import com.google.inject.AbstractModule;
 import com.google.inject.Singleton;
 
 /**
  * The Class TestModule.
  */
 public class TestModule extends AbstractModule {
 
     /* (non-Javadoc)
      * @see com.google.inject.AbstractModule#configure()
      */
     @Override
     protected void configure() {
         bind(IConfiguration.class).to(TestConfiguration.class).in(Singleton.class);
        bind(IResource.class).to(Resource.class).in(Singleton.class);
 
         bind(IMediaService.class).to(MediaService.class).in(Singleton.class);
        bind(IMediaIndex.class).to(MediaIndex.class).in(Singleton.class);
 
         bind(IMimeTypeFactory.class).to(MimeTypeFactory.class).in(Singleton.class);
 
     }
 
 }
