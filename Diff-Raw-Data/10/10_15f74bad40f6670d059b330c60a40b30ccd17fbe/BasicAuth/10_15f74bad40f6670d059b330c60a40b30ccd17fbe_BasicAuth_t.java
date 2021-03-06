 /**
  * Copyright (c) 2009 Red Hat, Inc.
  *
  * This software is licensed to you under the GNU General Public License,
  * version 2 (GPLv2). There is NO WARRANTY for this software, express or
  * implied, including the implied warranties of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
  * along with this software; if not, see
  * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
  *
  * Red Hat trademarks are not licensed under GPLv2. No permission is
  * granted to use or replicate Red Hat trademarks that are incorporated
  * in this software or its documentation.
  */
 package org.fedoraproject.candlepin.resteasy.interceptor;
 
 import java.util.List;
 
 import org.apache.commons.codec.binary.Base64;
 import org.apache.log4j.Logger;
 import org.fedoraproject.candlepin.auth.Principal;
 import org.fedoraproject.candlepin.auth.Role;
 import org.fedoraproject.candlepin.auth.SystemPrincipal;
 import org.fedoraproject.candlepin.auth.UserPrincipal;
 import org.fedoraproject.candlepin.exceptions.NotFoundException;
 import org.fedoraproject.candlepin.model.Owner;
 import org.fedoraproject.candlepin.model.OwnerCurator;
 import org.fedoraproject.candlepin.service.UserServiceAdapter;
 import org.jboss.resteasy.spi.HttpRequest;
 import org.jboss.resteasy.spi.ResteasyProviderFactory;
 
 import com.google.inject.Inject;
 
 /**
  * BasicAuth
  */
 class BasicAuth {
 
     private Logger log = Logger.getLogger(BasicAuth.class);
     private UserServiceAdapter userServiceAdapter;
     private OwnerCurator ownerCurator;
     
     @Inject
     BasicAuth(UserServiceAdapter userServiceAdapter,
             OwnerCurator ownerCurator) {
         this.userServiceAdapter = userServiceAdapter;
         this.ownerCurator = ownerCurator;
     }
 
     Principal getPrincipal(HttpRequest request) throws Exception {
 
         List<String> header = request.getHttpHeaders().getRequestHeader("Authorization");
         String auth = null;
         if (null != header && header.size() > 0) {
             auth = header.get(0);
         }
 
         if (auth != null && auth.toUpperCase().startsWith("BASIC ")) {
             String userpassEncoded = auth.substring(6);
             String[] userpass = new String(Base64.decodeBase64(userpassEncoded))
                     .split(":");
 
             String username = userpass[0];
            String password = null;
            if (userpass.length > 1) {
                password = userpass[1];
            }
            
             log.debug("check for: " + username + " - password of length #" +
                (password == null ? 0 : password.length()) + " = <omitted>");
             if (userServiceAdapter.validateUser(username, password)) {
                 Principal principal = createPrincipal(username);
                 if (log.isDebugEnabled()) {
                     log.debug("principal created for owner '" +
                         principal.getOwner().getDisplayName() + "' with username '" +
                         username + "'");
                 }
 
                 return principal;
             }
         }
 
         return null;
     }
 
     private Principal createPrincipal(String username) {
         Owner owner = this.userServiceAdapter.getOwner(username);
         owner = lookupOwner(owner);
         List<Role> roles = this.userServiceAdapter.getRoles(username);
         return new UserPrincipal(username, owner, roles);
     }
     
     private Owner lookupOwner(Owner owner) {
         Owner o = this.ownerCurator.lookupByKey(owner.getKey());
         if (o == null) {
             if (owner.getKey() == null) {
                 throw new NotFoundException("An owner does not exist for a null org id");
             }
             
             Principal systemPrincipal = new SystemPrincipal();
             ResteasyProviderFactory.pushContext(Principal.class, systemPrincipal);
             o = this.ownerCurator.create(owner);
             ResteasyProviderFactory.popContextData(Principal.class);
         }
         
         return o;
     }
 }
