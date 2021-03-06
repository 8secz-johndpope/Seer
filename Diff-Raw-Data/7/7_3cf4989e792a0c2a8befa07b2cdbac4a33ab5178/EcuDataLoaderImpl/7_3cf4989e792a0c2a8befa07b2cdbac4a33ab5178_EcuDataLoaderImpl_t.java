 /*
  * RomRaider Open-Source Tuning, Logging and Reflashing
  * Copyright (C) 2006-2009 RomRaider.com
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License along
  * with this program; if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 
 package com.romraider.logger.ecu.definition;
 
 import com.romraider.io.connection.ConnectionProperties;
 import com.romraider.logger.ecu.comms.query.EcuInit;
 import com.romraider.logger.ecu.definition.xml.EcuDefinitionHandler;
 import com.romraider.logger.ecu.definition.xml.LoggerDefinitionHandler;
 import com.romraider.logger.ecu.exception.ConfigurationException;
 import static com.romraider.util.ParamChecker.checkNotNull;
 import static com.romraider.util.ParamChecker.checkNotNullOrEmpty;
 import static com.romraider.util.SaxParserFactory.getSaxParser;
 import java.io.BufferedInputStream;
 import java.io.File;
 import java.io.FileInputStream;
import java.io.FileNotFoundException;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 public final class EcuDataLoaderImpl implements EcuDataLoader {
     private Map<String, EcuDefinition> ecuDefinitionMap = new HashMap<String, EcuDefinition>();
     private List<EcuParameter> ecuParameters = new ArrayList<EcuParameter>();
     private List<EcuSwitch> ecuSwitches = new ArrayList<EcuSwitch>();
     private EcuSwitch fileLoggingControllerSwitch;
     private ConnectionProperties connectionProperties;
 
     public void loadEcuDefsFromXml(File ecuDefsFile) {
         checkNotNull(ecuDefsFile, "ecuDefsFile");
         try {
             InputStream inputStream = new BufferedInputStream(new FileInputStream(ecuDefsFile));
             try {
                 EcuDefinitionHandler handler = new EcuDefinitionHandler();
                 getSaxParser().parse(inputStream, handler);
                 ecuDefinitionMap = handler.getEcuDefinitionMap();
             } finally {
                 inputStream.close();
             }
        } catch (FileNotFoundException fnfe) {
        	throw new ConfigurationException("The specified ECU definition file " + ecuDefsFile + " does not exist.");
         } catch (Exception e) {
             throw new ConfigurationException(e);
         }
     }
 
     public void loadConfigFromXml(String loggerConfigFilePath, String protocol, String fileLoggingControllerSwitchId, EcuInit ecuInit) {
         checkNotNullOrEmpty(loggerConfigFilePath, "loggerConfigFilePath");
         checkNotNullOrEmpty(protocol, "protocol");
         checkNotNullOrEmpty(fileLoggingControllerSwitchId, "fileLoggingControllerSwitchId");
         try {
             InputStream inputStream = new BufferedInputStream(new FileInputStream(new File(loggerConfigFilePath)));
             try {
                 LoggerDefinitionHandler handler = new LoggerDefinitionHandler(protocol, fileLoggingControllerSwitchId, ecuInit);
                 getSaxParser().parse(inputStream, handler);
                 ecuParameters = handler.getEcuParameters();
                 ecuSwitches = handler.getEcuSwitches();
                 fileLoggingControllerSwitch = handler.getFileLoggingControllerSwitch();
                 connectionProperties = handler.getConnectionProperties();
             } finally {
                 inputStream.close();
             }
        } catch (FileNotFoundException fnfe) {
        	throw new ConfigurationException("The specified Logger Config file " + loggerConfigFilePath + " does not exist.");
         } catch (Exception e) {
             throw new ConfigurationException(e);
         }
     }
 
     public Map<String, EcuDefinition> getEcuDefinitionMap() {
         return ecuDefinitionMap;
     }
 
     public List<EcuParameter> getEcuParameters() {
         return ecuParameters;
     }
 
     public List<EcuSwitch> getEcuSwitches() {
         return ecuSwitches;
     }
 
     public EcuSwitch getFileLoggingControllerSwitch() {
         return fileLoggingControllerSwitch;
     }
 
     public ConnectionProperties getConnectionProperties() {
         return connectionProperties;
     }
 }
