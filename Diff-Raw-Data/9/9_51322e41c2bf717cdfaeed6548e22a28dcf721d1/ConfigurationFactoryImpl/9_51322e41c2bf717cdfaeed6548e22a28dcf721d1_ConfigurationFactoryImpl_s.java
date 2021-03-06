 package org.jboss.forge.addon.configuration;
 
 import java.io.File;
 import java.io.IOException;
 
 import javax.enterprise.context.ApplicationScoped;
 import javax.enterprise.inject.Produces;
 
 import org.apache.commons.configuration.XMLConfiguration;
 import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.io.FileUtils;
 import org.jboss.forge.addon.resource.FileResource;
 import org.jboss.forge.furnace.util.OperatingSystemUtils;
 
 @ApplicationScoped
 public class ConfigurationFactoryImpl implements ConfigurationFactory
 {
    static final String USER_CONFIG_PATH = "org.jboss.forge.addon.configuration.USER_CONFIG_PATH";
 
    private Configuration userConfiguration;
 
    @Override
    @Produces
    @ApplicationScoped
    public Configuration getUserConfiguration() throws ConfigurationException
    {
       if (userConfiguration == null)
       {
          String property = System.getProperty(USER_CONFIG_PATH);
          File userConfigurationFile;
          if (property == null || property.isEmpty())
          {
             userConfigurationFile = new File(OperatingSystemUtils.getUserForgeDir(), "config.xml");
          }
          else
          {
             userConfigurationFile = new File(property);
          }
          if (!userConfigurationFile.exists() || userConfigurationFile.length() == 0L)
          {
            try
             {
               FileUtils.writeStringToFile(userConfigurationFile, "<configuration/>");
             }
             catch (IOException e)
             {
                throw new ConfigurationException("Error while create user configuration", e);
             }
          }
          userConfiguration = getConfiguration(userConfigurationFile);
       }
       return userConfiguration;
    }
 
    @Override
    public Configuration getConfiguration(FileResource<?> configFile)
    {
       return getConfiguration(configFile.getUnderlyingResourceObject());
    }
 
    private Configuration getConfiguration(File file)
    {
       try
       {
          XMLConfiguration commonsConfig = new XMLConfiguration(file);
          commonsConfig.setEncoding("UTF-8");
          commonsConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
          commonsConfig.setAutoSave(true);
          return new ConfigurationAdapter(commonsConfig);
       }
       catch (org.apache.commons.configuration.ConfigurationException e)
       {
          throw new ConfigurationException(e);
       }
    }
 }
