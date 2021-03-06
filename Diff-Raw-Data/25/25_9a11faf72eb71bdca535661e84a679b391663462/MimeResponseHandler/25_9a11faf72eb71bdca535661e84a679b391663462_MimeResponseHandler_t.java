 /*
  * JBoss, a division of Red Hat
  * Copyright 2010, Red Hat Middleware, LLC, and individual
  * contributors as indicated by the @authors tag. See the
  * copyright.txt in the distribution for a full listing of
  * individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 
 package org.gatein.wsrp.consumer.handlers;
 
 import org.gatein.common.text.TextTools;
 import org.gatein.pc.api.PortletInvokerException;
 import org.gatein.pc.api.URLFormat;
 import org.gatein.pc.api.cache.CacheScope;
 import org.gatein.pc.api.invocation.PortletInvocation;
 import org.gatein.pc.api.invocation.response.ContentResponse;
 import org.gatein.pc.api.invocation.response.ErrorResponse;
 import org.gatein.pc.api.invocation.response.PortletInvocationResponse;
 import org.gatein.pc.api.invocation.response.ResponseProperties;
 import org.gatein.pc.api.spi.PortletInvocationContext;
 import org.gatein.wsrp.WSRPConstants;
 import org.gatein.wsrp.WSRPConsumer;
 import org.gatein.wsrp.WSRPPortletURL;
 import org.gatein.wsrp.WSRPResourceURL;
 import org.gatein.wsrp.WSRPRewritingConstants;
 import org.gatein.wsrp.consumer.ProducerInfo;
 import org.gatein.wsrp.consumer.WSRPConsumerImpl;
 import org.oasis.wsrp.v2.CacheControl;
 import org.oasis.wsrp.v2.MimeResponse;
 import org.oasis.wsrp.v2.SessionContext;
 
 import java.util.Map;
 import java.util.Set;
 
 /**
  * @author <a href="mailto:chris.laprun@jboss.com">Chris Laprun</a>
  * @version $Revision$
  */
 public abstract class MimeResponseHandler<Invocation extends PortletInvocation, Request, Response, LocalMimeResponse extends MimeResponse> extends InvocationHandler<Invocation, Request, Response>
 {
    private static final org.gatein.pc.api.cache.CacheControl DEFAULT_CACHE_CONTROL = new org.gatein.pc.api.cache.CacheControl(0, CacheScope.PRIVATE, null);
 
    protected MimeResponseHandler(WSRPConsumerImpl consumer)
    {
       super(consumer);
    }
 
    protected abstract SessionContext getSessionContextFrom(Response response);
 
    protected abstract LocalMimeResponse getMimeResponseFrom(Response response);
 
    @Override
    protected PortletInvocationResponse processResponse(Response response, Invocation invocation, RequestPrecursor<Invocation> requestPrecursor) throws PortletInvokerException
    {
       consumer.getSessionHandler().updateSessionIfNeeded(getSessionContextFrom(response), invocation,
          requestPrecursor.getPortletHandle());
 
       LocalMimeResponse mimeResponse = getMimeResponseFrom(response);
       String markup = mimeResponse.getItemString();
       byte[] binary = mimeResponse.getItemBinary();
       if (markup != null && binary != null)
       {
          return new ErrorResponse(new IllegalArgumentException("Markup response cannot contain both string and binary " +
             "markup. Per Section 6.1.10 of the WSRP specification, this is a Producer error."));
       }
 
       if (markup == null && binary == null)
       {
          if (mimeResponse.isUseCachedItem())
          {
             //todo: deal with cache GTNWSRP-40
          }
          else
          {
             return new ErrorResponse(new IllegalArgumentException("Markup response must contain at least string or binary" +
                " markup. Per Section 6.1.10 of the WSRP specification, this is a Producer error."));
          }
       }
 
       if (markup != null && markup.length() > 0)
       {
          if (Boolean.TRUE.equals(mimeResponse.isRequiresRewriting()))
          {
             markup = processMarkup(
                markup,
                getNamespaceFrom(invocation.getWindowContext()),
                invocation.getContext(),
                invocation.getTarget(),
                new URLFormat(invocation.getSecurityContext().isSecure(), invocation.getSecurityContext().isAuthenticated(), true, true),
                consumer
             );
          }
       }
 
       String mimeType = mimeResponse.getMimeType();
       if (mimeType == null || mimeType.length() == 0)
       {
          return new ErrorResponse(new IllegalArgumentException("No MIME type was provided for portlet content."));
       }
 
       return createContentResponse(mimeResponse, invocation, null, null, mimeType, binary, markup, createCacheControl(mimeResponse));
    }
 
    protected PortletInvocationResponse createContentResponse(LocalMimeResponse mimeResponse, Invocation invocation,
                                                              ResponseProperties properties, Map<String, Object> attributes,
                                                              String mimeType, byte[] bytes, String markup,
                                                              org.gatein.pc.api.cache.CacheControl cacheControl)
    {
       return new ContentResponse(properties, attributes, mimeType, bytes, markup, cacheControl);
    }
 
    static String processMarkup(String markup, String namespace, PortletInvocationContext context, org.gatein.pc.api.PortletContext target, URLFormat format, WSRPConsumer consumer)
    {
       // fix-me: how to deal with fragment header? => interceptor?
 
       // todo: remove, this is a work-around for GTNWSRP-12
      if (!WSRPConstants.RUNS_IN_EPP)
      {
         markup = markup.replaceFirst("%3ftimeout%3d.*%2f", "%2f");
      }
 
       markup = TextTools.replaceBoundedString(
          markup,
          WSRPRewritingConstants.WSRP_REWRITE,
          WSRPRewritingConstants.END_WSRP_REWRITE,
          new MarkupProcessor(namespace, context, target, format, consumer.getProducerInfo()),
          true,
          false,
          true
       );
 
       return markup;
    }
 
    protected org.gatein.pc.api.cache.CacheControl createCacheControl(LocalMimeResponse mimeResponse)
    {
       CacheControl cacheControl = mimeResponse.getCacheControl();
       org.gatein.pc.api.cache.CacheControl result = DEFAULT_CACHE_CONTROL;
 
       int expires;
       if (cacheControl != null)
       {
          expires = cacheControl.getExpires();
          String userScope = cacheControl.getUserScope();
 
          // check that we support the user scope...
          if (consumer.supportsUserScope(userScope))
          {
             if (debug)
             {
                log.debug("Trying to cache markup " + userScope + " for " + expires + " seconds.");
             }
             CacheScope scope;
             if (WSRPConstants.CACHE_FOR_ALL.equals(userScope))
             {
                scope = CacheScope.PUBLIC;
             }
             else if (WSRPConstants.CACHE_PER_USER.equals(userScope))
             {
                scope = CacheScope.PRIVATE;
             }
             else
             {
                throw new IllegalArgumentException("Unknown CacheControl user scope: " + userScope); // should not happen
             }
 
             result = new org.gatein.pc.api.cache.CacheControl(expires, scope, cacheControl.getValidateTag());
          }
       }
 
       return result;
    }
 
    /**
     * @author <a href="mailto:chris.laprun@jboss.com">Chris Laprun</a>
     * @version $Revision$
     */
    private static class MarkupProcessor implements TextTools.StringReplacementGenerator
    {
       private final PortletInvocationContext context;
       private final URLFormat format;
       private final Set<String> supportedCustomModes;
       private final Set<String> supportedCustomWindowStates;
       private final String namespace;
 
       protected MarkupProcessor(String namespace, PortletInvocationContext context, org.gatein.pc.api.PortletContext target, URLFormat format, ProducerInfo info)
       {
          this.namespace = namespace;
          this.context = context;
          this.format = format;
          supportedCustomModes = info.getSupportedCustomModes();
          supportedCustomWindowStates = info.getSupportedCustomWindowStates();
       }
 
       public String getReplacementFor(String match, String prefix, String suffix)
       {
          if (prefix.equals(match))
          {
             return namespace;
          }
          else if (match.startsWith(WSRPRewritingConstants.BEGIN_WSRP_REWRITE_END))
          {
             // remove end of rewrite token
             match = match.substring(WSRPRewritingConstants.BEGIN_WSRP_REWRITE_END.length());
 
             WSRPPortletURL portletURL = WSRPPortletURL.create(match, supportedCustomModes, supportedCustomWindowStates, true);
             return context.renderURL(portletURL, format);
          }
          else
          {
             // match is not something we know how to process
             return match;
          }
       }
 
 
       static String getResourceURL(String urlAsString, WSRPResourceURL resource)
       {
          String resourceURL = resource.getResourceURL().toExternalForm();
          if (log.isDebugEnabled())
          {
             log.debug("URL '" + urlAsString + "' refers to a resource which are not currently well supported. " +
                "Attempting to craft a URL that we might be able to work with: '" + resourceURL + "'");
          }
 
          // right now the resourceURL should be output as is, because it will be used directly but it really should be encoded
          return resourceURL;
       }
    }
 }
