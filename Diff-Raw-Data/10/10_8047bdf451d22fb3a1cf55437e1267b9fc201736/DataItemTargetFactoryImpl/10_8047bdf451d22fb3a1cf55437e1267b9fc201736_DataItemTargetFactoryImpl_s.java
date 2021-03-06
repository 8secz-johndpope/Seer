 /*
  * This file is part of the OpenSCADA project
  * Copyright (C) 2006-2010 inavare GmbH (http://inavare.com)
  * 
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
 
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
  */
 
 package org.openscada.da.datasource.item;
 
 import java.util.Dictionary;
 import java.util.Hashtable;
 import java.util.Map;
 
 import org.openscada.da.datasource.DataSource;
 import org.openscada.da.server.common.DataItem;
 import org.openscada.da.server.common.DataItemInformationBase;
 import org.openscada.sec.osgi.AuthorizationHelper;
 import org.openscada.utils.osgi.ca.factory.AbstractServiceConfigurationFactory;
 import org.openscada.utils.osgi.pool.ObjectPoolHelper;
 import org.openscada.utils.osgi.pool.ObjectPoolImpl;
 import org.openscada.utils.osgi.pool.ObjectPoolTracker;
 import org.osgi.framework.BundleContext;
 import org.osgi.framework.Constants;
 import org.osgi.framework.InvalidSyntaxException;
 import org.osgi.framework.ServiceRegistration;
 
 public class DataItemTargetFactoryImpl extends AbstractServiceConfigurationFactory<DataItemTargetImpl>
 {
     public static final String FACTORY_ID = "da.dataitem.datasource";
 
     private final BundleContext context;
 
     private final ObjectPoolTracker poolTracker;
 
     private final ObjectPoolImpl itemPool;
 
     private final ServiceRegistration itemPoolHandle;
 
     private final AuthorizationHelper authorization;
 
     public DataItemTargetFactoryImpl ( final BundleContext context, final AuthorizationHelper authorization ) throws InvalidSyntaxException
     {
         super ( context );
         this.authorization = authorization;
         this.itemPool = new ObjectPoolImpl ();
 
         this.itemPoolHandle = ObjectPoolHelper.registerObjectPool ( context, this.itemPool, DataItem.class.getName () );
 
         this.context = context;
         this.poolTracker = new ObjectPoolTracker ( context, DataSource.class.getName () );
         this.poolTracker.open ();
     }
 
     @Override
     public synchronized void dispose ()
     {
         this.itemPoolHandle.unregister ();
         this.itemPool.dispose ();
 
         this.poolTracker.close ();
 
         super.dispose ();
     }
 
     @Override
     protected Entry<DataItemTargetImpl> createService ( final String configurationId, final BundleContext context, final Map<String, String> parameters ) throws Exception
     {
         return createDataItem ( configurationId, context, parameters );
     }
 
     @Override
     protected void disposeService ( final String id, final DataItemTargetImpl service )
     {
         this.itemPool.removeService ( id, service );
         service.dispose ();
     }
 
     @Override
     protected Entry<DataItemTargetImpl> updateService ( final String configurationId, final Entry<DataItemTargetImpl> entry, final Map<String, String> parameters ) throws Exception
     {
         this.itemPool.removeService ( configurationId, entry.getService () );
         entry.getService ().dispose ();
 
         return createDataItem ( configurationId, this.context, parameters );
     }
 
     protected Entry<DataItemTargetImpl> createDataItem ( final String configurationId, final BundleContext context, final Map<String, String> parameters ) throws InvalidSyntaxException
     {
         final String itemId = parameters.get ( "item.id" );
         if ( itemId == null )
         {
             throw new IllegalArgumentException ( "'item.id' must be set" );
         }
 
         final String datasourceId = parameters.get ( "datasource.id" );
         final DataItemTargetImpl item = new DataItemTargetImpl ( this.poolTracker, new DataItemInformationBase ( itemId ), datasourceId, this.authorization );
 
        final Dictionary<String, String> properties = new Hashtable<String, String> ();
        properties.put ( Constants.SERVICE_DESCRIPTION, "inavare GmbH" );
 
         // register
         this.itemPool.addService ( configurationId, item, properties );
 
         return new Entry<DataItemTargetImpl> ( configurationId, item );
     }
 }
