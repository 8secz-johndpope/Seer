 /*
  * $Id$
  *
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 
 package org.apache.tiles.test.factory;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Locale;
 
 import javax.sql.DataSource;
 
 import org.apache.tiles.TilesApplicationContext;
 import org.apache.tiles.context.ChainedTilesRequestContextFactory;
 import org.apache.tiles.context.TilesRequestContextFactory;
 import org.apache.tiles.definition.LocaleDefinitionsFactory;
 import org.apache.tiles.definition.dao.DefinitionDAO;
 import org.apache.tiles.factory.BasicTilesContainerFactory;
 import org.apache.tiles.freemarker.context.FreeMarkerTilesRequestContextFactory;
 import org.apache.tiles.locale.LocaleResolver;
 import org.apache.tiles.test.definition.dao.LocaleDbDefinitionDAO;
import org.apache.tiles.velocity.context.VelocityTilesRequestContextFactory;
 
 
 /**
  * Test alternate Tiles container factory that uses a DB to store definitions.
  *
  * @version $Rev$ $Date$
  */
 public class TestDbTilesContainerFactory extends BasicTilesContainerFactory {
 
     /** {@inheritDoc} */
     @Override
     protected DefinitionDAO<Locale> createLocaleDefinitionDao(TilesApplicationContext applicationContext,
             TilesRequestContextFactory contextFactory,
             LocaleResolver resolver) {
         LocaleDbDefinitionDAO definitionDao = new LocaleDbDefinitionDAO();
         definitionDao.setDataSource((DataSource) applicationContext
                 .getApplicationScope().get("dataSource"));
         return definitionDao;
     }
 
     /** {@inheritDoc} */
     @Override
     protected LocaleDefinitionsFactory instantiateDefinitionsFactory(
             TilesApplicationContext applicationContext, TilesRequestContextFactory contextFactory,
             LocaleResolver resolver) {
         return new LocaleDefinitionsFactory();
     }
 
     /** {@inheritDoc} */
     @Override
     protected void registerChainedRequestContextFactories(
             ChainedTilesRequestContextFactory contextFactory) {
         List<TilesRequestContextFactory> factories = new ArrayList<TilesRequestContextFactory>(
                 3);
         registerRequestContextFactory(
                 "org.apache.tiles.servlet.context.ServletTilesRequestContextFactory",
                 factories, contextFactory);
         registerRequestContextFactory(
                 "org.apache.tiles.jsp.context.JspTilesRequestContextFactory",
                 factories, contextFactory);
         registerRequestContextFactory(
                 FreeMarkerTilesRequestContextFactory.class.getName(),
                 factories, contextFactory);
         registerRequestContextFactory(
                VelocityTilesRequestContextFactory.class.getName(),
                 factories, contextFactory);
         contextFactory.setFactories(factories);
     }
 }
