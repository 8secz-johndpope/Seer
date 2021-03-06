 /**
  * Copyright (c) 2002-2011 "Neo Technology,"
  * Network Engine for Objects in Lund AB [http://neotechnology.com]
  *
  * This file is part of Neo4j.
  *
  * Neo4j is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program. If not, see <http://www.gnu.org/licenses/>.
  */
 package org.neo4j.backup.log;
 
 import java.nio.channels.ReadableByteChannel;
 
 import org.neo4j.helpers.Service;
 import org.neo4j.kernel.impl.transaction.xaframework.LogApplier;
 import org.neo4j.kernel.impl.transaction.xaframework.LogBuffer;
 import org.neo4j.kernel.impl.transaction.xaframework.LogDeserializer;
 import org.neo4j.kernel.impl.transaction.xaframework.LogDeserializerProvider;
 import org.neo4j.kernel.impl.transaction.xaframework.XaCommandFactory;
 
 @Service.Implementation( LogDeserializerProvider.class )
 public class VerifyingLogDeserializerProvider extends LogDeserializerProvider
 {
     public VerifyingLogDeserializerProvider()
     {
         super( "verifying" );
     }
 
     @Override
     public LogDeserializer getLogApplier( ReadableByteChannel byteChannel,
            LogBuffer writeBuffer, LogApplier applier, XaCommandFactory cf )
     {
         return new VerifyingLogDeserializer( byteChannel, writeBuffer, applier,
                cf );
     }
 }
