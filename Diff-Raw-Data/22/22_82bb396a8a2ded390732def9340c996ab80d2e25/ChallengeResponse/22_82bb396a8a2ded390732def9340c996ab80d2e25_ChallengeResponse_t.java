 /**
  * Copyright 2005-2009 Noelios Technologies.
  * 
  * The contents of this file are subject to the terms of one of the following
  * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
  * "Licenses"). You can select the license that you prefer but you may not use
  * this file except in compliance with one of these Licenses.
  * 
  * You can obtain a copy of the LGPL 3.0 license at
  * http://www.opensource.org/licenses/lgpl-3.0.html
  * 
  * You can obtain a copy of the LGPL 2.1 license at
  * http://www.opensource.org/licenses/lgpl-2.1.php
  * 
  * You can obtain a copy of the CDDL 1.0 license at
  * http://www.opensource.org/licenses/cddl1.php
  * 
  * You can obtain a copy of the EPL 1.0 license at
  * http://www.opensource.org/licenses/eclipse-1.0.php
  * 
  * See the Licenses for the specific language governing permissions and
  * limitations under the Licenses.
  * 
  * Alternatively, you can obtain a royalty free commercial license with less
  * limitations, transferable or non-transferable, directly at
  * http://www.noelios.com/products/restlet-engine
  * 
  * Restlet is a registered trademark of Noelios Technologies.
  */
 
 package org.restlet.data;
 
 import org.restlet.engine.util.SystemUtils;
 import org.restlet.util.Series;
 
 /**
  * Authentication response sent by client to an origin server. This is typically
  * following a {@link ChallengeRequest} sent by the origin server to the client.<br>
  * <br>
  * Sometimes, it might be faster to preemptively issue a challenge response if
  * the client knows for sure that the target resource will require
  * authentication.<br>
  * <br>
  * Note that when used with HTTP connectors, this class maps to the
  * "Authorization" header.
  * 
  * @author Jerome Louvel
  */
 public final class ChallengeResponse {
 
     /**
      * Indicates if the identifier or principal has been authenticated. The
      * application is responsible for updating this property, relying on a
      * {@link org.restlet.security.Guard} or manually.
      */
     private volatile boolean authenticated;
 
     /** The raw credentials for custom challenge schemes. */
     private volatile String credentials;
 
     /** The user identifier, such as a login name or an access key. */
     private volatile String identifier;
 
     /** The additional scheme parameters. */
     private volatile Series<Parameter> parameters;
 
     /** The challenge scheme. */
     private volatile ChallengeScheme scheme;
 
     /** The user secret, such as a password or a secret key. */
     private volatile char[] secret;
 
     /**
      * Constructor with no credentials.
      * 
      * @param scheme
      *            The challenge scheme.
      */
     public ChallengeResponse(ChallengeScheme scheme) {
         this(scheme, null);
     }
 
     /**
      * Constructor.
      * 
      * @param scheme
      *            The challenge scheme.
      * @param credentials
      *            The raw credentials for custom challenge schemes.
      */
     public ChallengeResponse(ChallengeScheme scheme, String credentials) {
         this.scheme = scheme;
         this.credentials = credentials;
         this.identifier = null;
         this.secret = null;
         this.parameters = null;
     }
 
     /**
      * Constructor.
      * 
      * @param scheme
      *            The challenge scheme.
      * @param identifier
      *            The user identifier, such as a login name or an access key.
      * @param secret
      *            The user secret, such as a password or a secret key.
      */
     public ChallengeResponse(final ChallengeScheme scheme,
             final String identifier, char[] secret) {
         this.scheme = scheme;
         this.credentials = null;
         this.identifier = identifier;
         this.secret = secret;
         this.parameters = null;
     }
 
     /**
      * Constructor.
      * 
      * @param scheme
      *            The challenge scheme.
      * @param identifier
      *            The user identifier, such as a login name or an access key.
      * @param parameters
      *            The additional scheme parameters.
      */
     public ChallengeResponse(final ChallengeScheme scheme,
             final String identifier, Series<Parameter> parameters) {
         this.scheme = scheme;
         this.credentials = null;
         this.identifier = identifier;
         this.secret = null;
         this.parameters = parameters;
     }
 
     /**
      * Constructor.
      * 
      * @param scheme
      *            The challenge scheme.
      * @param identifier
      *            The user identifier, such as a login name or an access key.
      * @param secret
      *            The user secret, such as a password or a secret key.
      */
     public ChallengeResponse(final ChallengeScheme scheme,
             final String identifier, String secret) {
         this.scheme = scheme;
         this.credentials = null;
         this.identifier = identifier;
         this.secret = (secret != null) ? secret.toCharArray() : null;
         this.parameters = null;
     }
 
     /** {@inheritDoc} */
     @Override
     public boolean equals(Object obj) {
         boolean result = (obj == this);
 
         // if obj == this no need to go further
         if (!result) {
             // if obj isn't a challenge request or is null don't evaluate
             // further
             if (obj instanceof ChallengeResponse) {
                 final ChallengeResponse that = (ChallengeResponse) obj;
 
                 if (getCredentials() != null) {
                     result = getCredentials().equals(that.getCredentials());
                 } else {
                     result = (that.getCredentials() == null);
                 }
 
                 if (result) {
                     if (getIdentifier() != null) {
                         result = getIdentifier().equals(that.getIdentifier());
                     } else {
                         result = (that.getIdentifier() == null);
                     }
 
                     if (result) {
                         if (getScheme() != null) {
                             result = getScheme().equals(that.getScheme());
                         } else {
                             result = (that.getScheme() == null);
                         }
 
                         if (result) {
                             if ((getSecret() == null)
                                     || (that.getSecret() == null)) {
                                 // check if both are null
                                 result = (getSecret() == that.getSecret());
                             } else {
                                 if (getSecret().length == that.getSecret().length) {
                                     boolean equals = true;
                                     for (int i = 0; (i < getSecret().length)
                                             && equals; i++) {
                                         equals = (getSecret()[i] == that
                                                 .getSecret()[i]);
                                     }
                                     result = equals;
                                 }
                             }
                         }
                     }
                 }
             }
         }
 
         return result;
     }
 
     /**
      * Returns the credentials.
      * 
      * @return The credentials.
      */
     public String getCredentials() {
         return this.credentials;
     }
 
     /**
      * Returns the user identifier, such as a login name or an access key.
      * 
      * @return The user identifier, such as a login name or an access key.
      */
     public String getIdentifier() {
         return this.identifier;
     }
 
     /**
      * Returns the modifiable series of scheme parameters. Creates a new
      * instance if no one has been set.
      * 
      * @return The modifiable series of scheme parameters.
      */
     public Series<Parameter> getParameters() {
         if (this.parameters == null) {
             this.parameters = new Form();
         }
 
         return this.parameters;
     }
 
     // [ifndef gwt] method
     /**
      * Gets the principal associated to the identifier property.
      * 
      * @return The {@link org.restlet.security.UserPrincipal}.
      */
    public java.security.Principal getPrincipal() {
        return new java.security.Principal() {
             public String getName() {
                 return getIdentifier();
             };
         };
     }
 
     /**
      * Returns the scheme used.
      * 
      * @return The scheme used.
      */
     public ChallengeScheme getScheme() {
         return this.scheme;
     }
 
     /**
      * Returns the user secret, such as a password or a secret key.
      * 
      * It is not recommended to use {@link String#String(char[])} for security
      * reasons.
      * 
      * @return The user secret, such as a password or a secret key.
      */
     public char[] getSecret() {
         return this.secret;
     }
 
     /** {@inheritDoc} */
     @Override
     public int hashCode() {
         return SystemUtils.hashCode(getScheme(), getIdentifier(),
         // Secret is simply discarded from hash code calculation because
                 // we don't want it to be materialized as a string
                 // (getSecret() == null) ? null : new String(getSecret()),
                 getCredentials());
     }
 
     /**
      * Indicates if the identifier or principal has been authenticated. The
      * application is responsible for updating this property, relying on a
      * {@link org.restlet.security.Guard} or manually.
      * 
      * @return True if the identifier or principal has been authenticated.
      * @deprecated Use {@link ClientInfo#isAuthenticated()} instead.
      */
     @Deprecated
     public boolean isAuthenticated() {
         return this.authenticated;
     }
 
     /**
      * Indicates if the identifier or principal has been authenticated. The
      * application is responsible for updating this property, relying on a
      * {@link org.restlet.security.Guard} or manually.
      * 
      * @param authenticated
      *            True if the identifier or principal has been authenticated.
      * @deprecated Use {@link ClientInfo#setAuthenticated(boolean)} instead.
      */
     @Deprecated
     public void setAuthenticated(boolean authenticated) {
         this.authenticated = authenticated;
     }
 
     /**
      * Sets the credentials components.
      * 
      * @param credentialComponents
      *            The credentials components.
      */
     public void setCredentialComponents(Series<Parameter> credentialComponents) {
         this.parameters = credentialComponents;
     }
 
     /**
      * Sets the credentials.
      * 
      * @param credentials
      *            The credentials.
      */
     public void setCredentials(String credentials) {
         this.credentials = credentials;
     }
 
     /**
      * Sets the user identifier, such as a login name or an access key.
      * 
      * @param identifier
      *            The user identifier, such as a login name or an access key.
      */
     public void setIdentifier(String identifier) {
         this.identifier = identifier;
     }
 
     /**
      * Sets the scheme used.
      * 
      * @param scheme
      *            The scheme used.
      */
     public void setScheme(ChallengeScheme scheme) {
         this.scheme = scheme;
     }
 
     /**
      * Sets the user secret, such as a password or a secret key.
      * 
      * @param secret
      *            The user secret, such as a password or a secret key.
      */
     public void setSecret(char[] secret) {
         this.secret = secret;
     }
 
     /**
      * Sets the user secret, such as a password or a secret key.
      * 
      * @param secret
      *            The user secret, such as a password or a secret key.
      */
     public void setSecret(String secret) {
         this.secret = (secret == null) ? null : secret.toCharArray();
     }
 
 }
