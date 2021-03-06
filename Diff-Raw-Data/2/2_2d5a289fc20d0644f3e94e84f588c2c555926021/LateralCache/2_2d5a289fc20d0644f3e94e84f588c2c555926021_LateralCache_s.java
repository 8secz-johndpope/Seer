 package org.apache.jcs.auxiliary.lateral;
 
 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 
 import java.io.IOException;
 import java.io.Serializable;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Set;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.jcs.auxiliary.AbstractAuxiliaryCacheEventLogging;
 import org.apache.jcs.auxiliary.AuxiliaryCacheAttributes;
 import org.apache.jcs.auxiliary.lateral.behavior.ILateralCacheAttributes;
 import org.apache.jcs.auxiliary.lateral.behavior.ILateralCacheService;
 import org.apache.jcs.engine.CacheConstants;
 import org.apache.jcs.engine.behavior.ICacheElement;
 import org.apache.jcs.engine.behavior.ICacheType;
 import org.apache.jcs.engine.behavior.IZombie;
 import org.apache.jcs.engine.stats.Stats;
 import org.apache.jcs.engine.stats.behavior.IStats;
 
 /**
  * Lateral distributor. Returns null on get by default. Net search not implemented.
  */
 public class LateralCache
     extends AbstractAuxiliaryCacheEventLogging
 {
     /** Don't change. */
     private static final long serialVersionUID = 6274549256562382782L;
 
     /** The logger. */
     private final static Log log = LogFactory.getLog( LateralCache.class );
 
     /** generalize this, use another interface */
     private final ILateralCacheAttributes lateralCacheAttribures;
 
     /** The region name */
     final String cacheName;
 
     /** either http, socket.udp, or socket.tcp can set in config */
     private ILateralCacheService lateralCacheService;
 
     /** Monitors the connection. */
     private LateralCacheMonitor monitor;
 
     /**
      * Constructor for the LateralCache object
      * <p>
      * @param cattr
      * @param lateral
      * @param monitor
      */
     public LateralCache( ILateralCacheAttributes cattr, ILateralCacheService lateral, LateralCacheMonitor monitor )
     {
         this.cacheName = cattr.getCacheName();
         this.lateralCacheAttribures = cattr;
         this.lateralCacheService = lateral;
         this.monitor = monitor;
     }
 
     /**
      * Constructor for the LateralCache object
      * <p>
      * @param cattr
      */
     public LateralCache( ILateralCacheAttributes cattr )
     {
         this.cacheName = cattr.getCacheName();
         this.lateralCacheAttribures = cattr;
     }
 
     /**
      * Update lateral.
      * <p>
      * @param ce
      * @throws IOException
      */
     @Override
     protected void processUpdate( ICacheElement ce )
         throws IOException
     {
         try
         {
             if ( log.isDebugEnabled() )
             {
                 log.debug( "update: lateral = [" + lateralCacheService + "], " + "LateralCacheInfo.listenerId = "
                     + LateralCacheInfo.listenerId );
             }
             lateralCacheService.update( ce, LateralCacheInfo.listenerId );
         }
         catch ( NullPointerException npe )
         {
             log.error( "Failure updating lateral. lateral = " + lateralCacheService, npe );
             handleException( npe, "Failed to put [" + ce.getKey() + "] to " + ce.getCacheName() + "@" + lateralCacheAttribures );
             return;
         }
         catch ( Exception ex )
         {
             handleException( ex, "Failed to put [" + ce.getKey() + "] to " + ce.getCacheName() + "@" + lateralCacheAttribures );
         }
     }
 
     /**
      * The performance costs are too great. It is not recommended that you enable lateral gets.
      * <p>
      * @param key
      * @return ICacheElement or null
      * @throws IOException
      */
     @Override
     protected ICacheElement processGet( Serializable key )
         throws IOException
     {
         ICacheElement obj = null;
 
         if ( this.lateralCacheAttribures.getPutOnlyMode() )
         {
             return null;
         }
         try
         {
             obj = lateralCacheService.get( cacheName, key );
         }
         catch ( Exception e )
         {
             log.error( e );
             handleException( e, "Failed to get [" + key + "] from " + lateralCacheAttribures.getCacheName() + "@" + lateralCacheAttribures );
         }
         return obj;
     }
 
     /**
      * @param pattern
      * @return A map of Serializable key to ICacheElement element, or an empty map if there is no
      *         data in cache for any of these keys
      * @throws IOException
      */
     @Override
     protected Map<Serializable, ICacheElement> processGetMatching( String pattern )
         throws IOException
     {
         if ( this.lateralCacheAttribures.getPutOnlyMode() )
         {
             return Collections.<Serializable, ICacheElement>emptyMap();
         }
         try
         {
             return lateralCacheService.getMatching( cacheName, pattern );
         }
         catch ( IOException e )
         {
             log.error( e );
             handleException( e, "Failed to getMatching [" + pattern + "] from " + lateralCacheAttribures.getCacheName() + "@" + lateralCacheAttribures );
             return Collections.<Serializable, ICacheElement>emptyMap();
         }
     }
 
     /**
      * Gets multiple items from the cache based on the given set of keys.
      * <p>
      * @param keys
      * @return a map of Serializable key to ICacheElement element, or an empty map if there is no
      *         data in cache for any of these keys
      * @throws IOException
      */
     @Override
     protected Map<Serializable, ICacheElement> processGetMultiple( Set<Serializable> keys )
         throws IOException
     {
         Map<Serializable, ICacheElement> elements = new HashMap<Serializable, ICacheElement>();
 
         if ( keys != null && !keys.isEmpty() )
         {
             for (Serializable key : keys)
             {
                 ICacheElement element = get( key );
 
                 if ( element != null )
                 {
                     elements.put( key, element );
                 }
             }
         }
 
         return elements;
     }
 
     /**
      * @param groupName
      * @return A set of group keys.
      * @throws IOException
      */
     public Set<Serializable> getGroupKeys( String groupName )
         throws IOException
     {
         try
         {
             return lateralCacheService.getGroupKeys( cacheName, groupName );
         }
         catch ( Exception ex )
         {
             handleException( ex, "Failed to remove groupName [" + groupName + "] from " + lateralCacheAttribures.getCacheName() + "@"
                 + lateralCacheAttribures );
         }
         return Collections.emptySet();
     }
 
     /**
      * Synchronously remove from the remote cache; if failed, replace the remote handle with a
      * zombie.
      * <p>
      * @param key
      * @return false always
      * @throws IOException
      */
     @Override
     protected boolean processRemove( Serializable key )
         throws IOException
     {
         if ( log.isDebugEnabled() )
         {
             log.debug( "removing key:" + key );
         }
 
         try
         {
             lateralCacheService.remove( cacheName, key, LateralCacheInfo.listenerId );
         }
         catch ( Exception ex )
         {
             handleException( ex, "Failed to remove " + key + " from " + lateralCacheAttribures.getCacheName() + "@" + lateralCacheAttribures );
         }
         return false;
     }
 
     /**
      * Synchronously removeAll from the remote cache; if failed, replace the remote handle with a
      * zombie.
      * <p>
      * @throws IOException
      */
     @Override
     protected void processRemoveAll()
         throws IOException
     {
         try
         {
             lateralCacheService.removeAll( cacheName, LateralCacheInfo.listenerId );
         }
         catch ( Exception ex )
         {
             handleException( ex, "Failed to remove all from " + lateralCacheAttribures.getCacheName() + "@" + lateralCacheAttribures );
         }
     }
 
     /**
      * Synchronously dispose the cache. Not sure we want this.
      * <p>
      * @throws IOException
      */
     @Override
     protected void processDispose()
         throws IOException
     {
         log.debug( "Disposing of lateral cache" );
 
         ///* HELP: This section did nothing but generate compilation warnings.
        // TODO: may limit this funcionality. It is dangerous.
         // asmuts -- Added functionality to help with warnings. I'm not getting
         // any.
         try
         {
             lateralCacheService.dispose( this.lateralCacheAttribures.getCacheName() );
             // Should remove connection
         }
         catch ( Exception ex )
         {
             log.error( "Couldn't dispose", ex );
             handleException( ex, "Failed to dispose " + lateralCacheAttribures.getCacheName() );
         }
     }
 
     /**
      * Returns the cache status.
      * <p>
      * @return The status value
      */
     public int getStatus()
     {
         return this.lateralCacheService instanceof IZombie ? CacheConstants.STATUS_ERROR : CacheConstants.STATUS_ALIVE;
     }
 
     /**
      * Returns the current cache size.
      * <p>
      * @return The size value
      */
     public int getSize()
     {
         return 0;
     }
 
     /**
      * Gets the cacheType attribute of the LateralCache object
      * <p>
      * @return The cacheType value
      */
     public int getCacheType()
     {
         return ICacheType.LATERAL_CACHE;
     }
 
     /**
      * Gets the cacheName attribute of the LateralCache object
      * <p>
      * @return The cacheName value
      */
     public String getCacheName()
     {
         return cacheName;
     }
 
     /**
      * Not yet sure what to do here.
      * <p>
      * @param ex
      * @param msg
      * @throws IOException
      */
     private void handleException( Exception ex, String msg )
         throws IOException
     {
         log.error( "Disabling lateral cache due to error " + msg, ex );
 
         lateralCacheService = new ZombieLateralCacheService( lateralCacheAttribures.getZombieQueueMaxSize() );
         // may want to flush if region specifies
         // Notify the cache monitor about the error, and kick off the recovery
         // process.
         monitor.notifyError();
 
         // could stop the net serach if it is built and try to reconnect?
         if ( ex instanceof IOException )
         {
             throw (IOException) ex;
         }
         throw new IOException( ex.getMessage() );
     }
 
     /**
      * Replaces the current remote cache service handle with the given handle.
      * <p>
      * @param restoredLateral
      */
     public void fixCache( ILateralCacheService restoredLateral )
     {
         if ( this.lateralCacheService != null && this.lateralCacheService instanceof ZombieLateralCacheService )
         {
             ZombieLateralCacheService zombie = (ZombieLateralCacheService) this.lateralCacheService;
             this.lateralCacheService = restoredLateral;
             try
             {
                 zombie.propagateEvents( restoredLateral );
             }
             catch ( Exception e )
             {
                 try
                 {
                     handleException( e, "Problem propagating events from Zombie Queue to new Lateral Service." );
                 }
                 catch ( IOException e1 )
                 {
                     // swallow, since this is just expected kick back.  Handle always throws
                 }
             }
         }
         else
         {
             this.lateralCacheService = restoredLateral;
         }
     }
 
     /**
      * getStats
      * <p>
      * @return String
      */
     public String getStats()
     {
         return "";
     }
 
     /**
      * @return Returns the AuxiliaryCacheAttributes.
      */
     public AuxiliaryCacheAttributes getAuxiliaryCacheAttributes()
     {
         return lateralCacheAttribures;
     }
 
     /**
      * @return debugging data.
      */
     @Override
     public String toString()
     {
         StringBuffer buf = new StringBuffer();
         buf.append( "\n LateralCache " );
         buf.append( "\n Cache Name [" + lateralCacheAttribures.getCacheName() + "]" );
         buf.append( "\n cattr =  [" + lateralCacheAttribures + "]" );
         return buf.toString();
     }
 
     /**
      * @return extra data.
      */
     @Override
     public String getEventLoggingExtraInfo()
     {
         return null;
     }
 
     /**
      * The NoWait on top does not call out to here yet.
      * <p>
      * @return almost nothing
      */
     public IStats getStatistics()
     {
         IStats stats = new Stats();
         stats.setTypeName( "LateralCache" );
         return stats;
     }
 }
