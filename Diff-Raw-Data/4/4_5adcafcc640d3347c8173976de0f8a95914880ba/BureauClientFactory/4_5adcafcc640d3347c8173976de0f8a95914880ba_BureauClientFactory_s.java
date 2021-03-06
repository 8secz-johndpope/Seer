 //
 // $Id$
 //
 // Narya library - tools for developing networked games
 // Copyright (C) 2002-2007 Three Rings Design, Inc., All Rights Reserved
 // http://www.threerings.net/code/narya/
 //
 // This library is free software; you can redistribute it and/or modify it
 // under the terms of the GNU Lesser General Public License as published
 // by the Free Software Foundation; either version 2.1 of the License, or
 // (at your option) any later version.
 //
 // This library is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 // Lesser General Public License for more details.
 //
 // You should have received a copy of the GNU Lesser General Public
 // License along with this library; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 
 package com.threerings.bureau.server;
 
 import com.threerings.util.Name;
 
import com.threerings.presents.data.ClientObject;
 import com.threerings.presents.net.AuthRequest;
 import com.threerings.presents.server.ClientFactory;
 import com.threerings.presents.server.ClientResolver;
 import com.threerings.presents.server.PresentsClient;
 
 import com.threerings.bureau.data.BureauCredentials;
 
 /**
  * Handles resolution of bureaus and passes non-bureau resolution requests through to a normal
  * factory. For bureaus, creates base class instances {@link PresentsClient} and
 * {@link ClientObject}.
  * @see BureauRegistry#setDefaultClientFactory()
  */
 public class BureauClientFactory implements ClientFactory
 {
     public BureauClientFactory (ClientFactory delegate)
     {
         _delegate = delegate;
     }
 
     // from interface ClientFactory
     public Class<? extends PresentsClient> getClientClass (AuthRequest areq)
     {
         // Just give bureaus a vanilla PresentsClient client for now.
         // TODO: will bureaus need a more tailored client?
         if (areq.getCredentials() instanceof BureauCredentials) {
             return PresentsClient.class;
         } else {
             return _delegate.getClientClass(areq);
         }
     }
 
     // from interface ClientFactory
     public Class<? extends ClientResolver> getClientResolverClass (Name username)
     {
         // Just give bureaus a vanilla ClientResolver for now.
         // TODO: will bureaus need specific resolution?
         if (BureauCredentials.isBureau(username)) {
             return ClientResolver.class;
         } else {
             return _delegate.getClientResolverClass(username);
         }
     }
 
     protected ClientFactory _delegate;
 }
