 /*
  * Copyright 2005-2008 Noelios Consulting.
  * 
  * The contents of this file are subject to the terms of the Common Development
  * and Distribution License (the "License"). You may not use this file except in
  * compliance with the License.
  * 
  * You can obtain a copy of the license at
  * http://www.opensource.org/licenses/cddl1.txt See the License for the specific
  * language governing permissions and limitations under the License.
  * 
  * When distributing Covered Code, include this CDDL HEADER in each file and
  * include the License file at http://www.opensource.org/licenses/cddl1.txt If
  * applicable, add the following below this CDDL HEADER, with the fields
  * enclosed by brackets "[]" replaced with your own identifying information:
  * Portions Copyright [yyyy] [name of copyright owner]
  */
 
 package org.restlet.util;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.net.URL;
 import java.util.Collection;
 import java.util.List;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.restlet.Application;
 import org.restlet.Client;
 import org.restlet.Component;
 import org.restlet.Context;
 import org.restlet.Directory;
 import org.restlet.Guard;
 import org.restlet.Server;
 import org.restlet.data.CharacterSet;
 import org.restlet.data.ClientInfo;
 import org.restlet.data.Cookie;
 import org.restlet.data.CookieSetting;
 import org.restlet.data.Dimension;
 import org.restlet.data.Form;
 import org.restlet.data.Language;
 import org.restlet.data.MediaType;
 import org.restlet.data.Parameter;
 import org.restlet.data.Request;
 import org.restlet.data.Response;
 import org.restlet.resource.Representation;
 import org.restlet.resource.Resource;
 import org.restlet.resource.Variant;
 
 /**
  * Facade to the engine implementating the Restlet API. Note that this is an SPI
  * class that is not intended for public usage.
  * 
  * @author Jerome Louvel (contact@noelios.com)
  */
 public abstract class Engine {
 
     /** Classloader to use for dynamic class loading. */
     private static volatile ClassLoader classloader = Engine.class
             .getClassLoader();
 
     /** The registered engine. */
     private static volatile Engine instance = null;
 
     /** Obtain a suitable logger. */
     private static final Logger logger = Logger.getLogger(Engine.class
             .getCanonicalName());
 
     /** Major version number. */
     public static final String MAJOR_NUMBER = "@major-number@";
 
     /** Minor version number. */
     public static final String MINOR_NUMBER = "@minor-number@";
 
     /** Provider resource. */
     private static final String providerResource = "META-INF/services/org.restlet.util.Engine";
 
     /** Release number. */
     public static final String RELEASE_NUMBER = "@release-type@@release-number@";
 
     /** Complete version. */
     public static final String VERSION = MAJOR_NUMBER + '.' + MINOR_NUMBER
             + '.' + RELEASE_NUMBER;
 
     /**
      * Returns the class object for the given name using the context class
      * loader first, or the classloader of the current class.
      * 
      * @param classname
      *                The class name to lookup.
      * @return The class object.
      * @throws ClassNotFoundException
      */
     public static Class<?> classForName(String classname)
             throws ClassNotFoundException {
         Class<?> result = null;
         ClassLoader loader = Thread.currentThread().getContextClassLoader();
 
         if (loader != null) {
             result = Class.forName(classname, false, loader);
         } else {
             result = Class.forName(classname);
         }
 
         return result;
     }
 
     /**
      * Returns a class loader to use when creating instantiating implementation
      * classes. By default, it reused the classloader of this Engine's class.
      * 
      * @return the ClassLoader
      */
     public static ClassLoader getClassLoader() {
         return classloader;
     }
 
     /**
      * Returns the registered Restlet engine.
      * 
      * @return The registered Restlet engine.
      */
     public static Engine getInstance() {
         Engine result = instance;
 
         if (result == null) {
             // Find the engine class name
             String engineClassName = null;
 
             // Try the default classloader
             ClassLoader cl = getClassLoader();
             URL configURL = cl.getResource(providerResource);
 
             if (configURL == null) {
                 // Try the current thread's classloader
                 cl = Thread.currentThread().getContextClassLoader();
                 configURL = cl.getResource(providerResource);
             }
 
             if (configURL == null) {
                 // Try the system classloader
                 cl = ClassLoader.getSystemClassLoader();
                 configURL = cl.getResource(providerResource);
             }
 
             if (configURL != null) {
                 BufferedReader reader = null;
                 try {
                     reader = new BufferedReader(new InputStreamReader(configURL
                             .openStream(), "utf-8"));
                     String providerName = reader.readLine();
 
                     if (providerName != null)
                         engineClassName = providerName.substring(0,
                                 providerName.indexOf('#')).trim();
                 } catch (IOException e) {
                     logger
                             .log(
                                     Level.SEVERE,
                                     "Unable to register the Restlet API implementation. Please check that the JAR file is in your classpath.");
                 } finally {
                     if (reader != null) {
                         try {
                             reader.close();
                         } catch (IOException e) {
                             logger
                                     .warning("IOException encountered while closing an open BufferedReader"
                                             + e.getMessage());
                         }
                     }
 
                 }
 
                 // Instantiate the engine
                 try {
                     instance = (Engine) Class.forName(engineClassName)
                             .newInstance();
                     result = instance;
                 } catch (Exception e) {
                     logger
                             .log(
                                     Level.SEVERE,
                                     "Unable to register the Restlet API implementation",
                                     e);
                     throw new RuntimeException(
                             "Unable to register the Restlet API implementation");
                 }
             }
 
             if (configURL == null) {
                 logger
                         .log(
                                 Level.SEVERE,
                                 "Unable to find an implementation of the Restlet API. Please check your classpath.");
 
             }
         }
 
         return result;
     }
 
     /**
      * Computes the hash code of a set of objects. Follows the algorithm
      * specified in List.hasCode().
      * 
      * @param objects
      *                the objects to compute the hashCode
      * 
      * @return The hash code of a set of objects.
      */
     public static int hashCode(Object... objects) {
         int result = 1;
 
         if (objects != null) {
             for (Object obj : objects) {
                 result = 31 * result + (obj == null ? 0 : obj.hashCode());
             }
         }
 
         return result;
     }
 
     /**
      * Sets a new class loader to use when creating instantiating implementation
      * classes.
      * 
      * @param newClassloader
      *                The new class loader to use.
      */
     public static void setClassLoader(ClassLoader newClassloader) {
         classloader = newClassloader;
     }
 
     /**
      * Sets the registered Restlet engine.
      * 
      * @param engine
      *                The registered Restlet engine.
      */
     public static void setInstance(Engine engine) {
         instance = engine;
     }
 
     /**
      * Indicates if the call is properly authenticated. By default, this
      * delegates credential checking to checkSecret().
      * 
      * @param request
      *                The request to authenticate.
      * @param guard
      *                The associated guard to callback.
      * @return -1 if the given credentials were invalid, 0 if no credentials
      *         were found and 1 otherwise.
      * @see Guard#checkSecret(Request, String, char[])
      */
     public abstract int authenticate(Request request, Guard guard);
 
     /**
      * Challenges the client by adding a challenge request to the response and
      * by setting the status to CLIENT_ERROR_UNAUTHORIZED.
      * 
      * @param response
      *                The response to update.
      * @param stale
      *                Indicates if the new challenge is due to a stale response.
      * @param guard
      *                The associated guard to callback.
      */
     public abstract void challenge(Response response, boolean stale, Guard guard);
 
     /**
     * Copies the given header parameters into the given {@link Response}.
      * 
      * @param headers
      *                The headers to copy.
      * @param response
      *                The response to update. Must contain a
      *                {@link Representation} to copy the representation headers
      *                in it.
      * @param logger
      *                The logger to use.
      */
     public abstract void copyResponseHeaders(Iterable<Parameter> headers,
             Response response, Logger logger);
 
     /**
      * Copies the headers of the given {@link Response} into the given
      * {@link Series}.
      * 
      * @param response
      *                The response to update. Should contain a
      *                {@link Representation} to copy the representation headers
      *                from it.
      * @param headers
      *                The Series to copy the headers in.
      * @param logger
      *                The logger to use.
      */
     public abstract void copyResponseHeaders(Response response,
             Series<Parameter> headers, Logger logger);
 
     /**
      * Creates a directory resource.
      * 
      * @param handler
      *                The parent directory handler.
      * @param request
      *                The request to handle.
      * @param response
      *                The response to return.
      * @return A new directory resource.
      * @throws IOException
      */
     public abstract Resource createDirectoryResource(Directory handler,
             Request request, Response response) throws IOException;
 
     /**
      * Creates a new helper for a given component.
      * 
      * @param application
      *                The application to help.
      * @param parentContext
      *                The parent context, typically the component's context.
      * @return The new helper.
      */
     public abstract Helper<Application> createHelper(Application application,
             Context parentContext);
 
     /**
      * Creates a new helper for a given client connector.
      * 
      * @param client
      *                The client to help.
      * @param helperClass
      *                Optional helper class name.
      * @return The new helper.
      */
     public abstract Helper<Client> createHelper(Client client,
             String helperClass);
 
     /**
      * Creates a new helper for a given component.
      * 
      * @param component
      *                The component to help.
      * @return The new helper.
      */
     public abstract Helper<Component> createHelper(Component component);
 
     /**
      * Creates a new helper for a given server connector.
      * 
      * @param server
      *                The server to help.
      * @param helperClass
      *                Optional helper class name.
      * @return The new helper.
      */
     public abstract Helper<Server> createHelper(Server server,
             String helperClass);
 
     /**
      * Formats the given Cookie to a String
      * 
      * @param cookie
      * @return the Cookie as String
      * @throws IllegalArgumentException
      *                 Thrown if the Cookie contains illegal values
      */
     public abstract String formatCookie(Cookie cookie)
             throws IllegalArgumentException;
 
     /**
      * Formats the given CookieSetting to a String
      * 
      * @param cookieSetting
      * @return the CookieSetting as String
      * @throws IllegalArgumentException
      *                 Thrown if the CookieSetting contains illegal values
      */
     public abstract String formatCookieSetting(CookieSetting cookieSetting)
             throws IllegalArgumentException;
 
     /**
      * Formats the given Set of Dimensions to a String for the HTTP Vary header.
      * 
      * @param dimensions
      *                the dimensions to format.
      * @return the Vary header or null, if dimensions is null or empty.
      */
     public abstract String formatDimensions(Collection<Dimension> dimensions);
 
     /**
      * Returns the best variant representation for a given resource according
      * the the client preferences.<br>
      * A default language is provided in case the variants don't match the
      * client preferences.
      * 
      * @param client
      *                The client preferences.
      * @param variants
      *                The list of variants to compare.
      * @param defaultLanguage
      *                The default language.
      * @return The preferred variant.
      * @see <a
      *      href="http://httpd.apache.org/docs/2.2/en/content-negotiation.html#algorithm">Apache
      *      content negotiation algorithm</a>
      */
     public abstract Variant getPreferredVariant(ClientInfo client,
             List<Variant> variants, Language defaultLanguage);
 
     /**
      * Parses a representation into a form.
      * 
      * @param logger
      *                The logger to use.
      * @param form
      *                The target form.
      * @param representation
      *                The representation to parse.
      */
     public abstract void parse(Logger logger, Form form,
             Representation representation);
 
     /**
      * Parses a parameters string to parse into a given form.
      * 
      * @param logger
      *                The logger to use.
      * @param form
      *                The target form.
      * @param parametersString
      *                The parameters string to parse.
      * @param characterSet
      *                The supported character encoding.
      * @param decode
      *                Indicates if the parameters should be decoded using the
      *                given character set.
      * @param separator
      *                The separator character to append between parameters.
      */
     public abstract void parse(Logger logger, Form form,
             String parametersString, CharacterSet characterSet, boolean decode,
             char separator);
 
     /**
      * Parses the given Content Type.
      * 
      * @param contentType
      *                the Content Type as String
      * @return the ContentType as MediaType; charset etc. are parameters.
      * @throws IllegalArgumentException
      *                 if the String can not be parsed.
      */
     public abstract MediaType parseContentType(String contentType)
             throws IllegalArgumentException;
 
     /**
      * Parses the given String to a Cookie
      * 
      * @param cookie
      * @return the Cookie parsed from the String
      * @throws IllegalArgumentException
      *                 Thrown if the String can not be parsed as Cookie.
      */
     public abstract Cookie parseCookie(String cookie)
             throws IllegalArgumentException;
 
     /**
      * Parses the given String to a CookieSetting
      * 
      * @param cookieSetting
      * @return the CookieSetting parsed from the String
      * @throws IllegalArgumentException
      *                 Thrown if the String can not be parsed as CookieSetting.
      */
     public abstract CookieSetting parseCookieSetting(String cookieSetting)
             throws IllegalArgumentException;
 
     /**
      * Returns the MD5 digest of the target string. Target is decoded to bytes
      * using the US-ASCII charset. The returned hexidecimal String always
      * contains 32 lowercase alphanumeric characters. For example, if target is
      * "HelloWorld", this method returns "68e109f0f40ca72a15e05cc22786f8e6".
      * 
      * @param target
      *                The string to encode.
      * @return The MD5 digest of the target string.
      */
     public abstract String toMd5(String target);
 }
