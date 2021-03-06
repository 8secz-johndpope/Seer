 /*
  * JBoss, Home of Professional Open Source.
  * Copyright 2010, Red Hat Middleware LLC, and individual contributors
  * as indicated by the @author tags. See the copyright.txt file in the
  * distribution for a full listing of individual contributors.
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
 package org.jboss.jca.codegenerator;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Locale;
 import java.util.ResourceBundle;
 
 import javax.xml.bind.JAXBContext;
 import javax.xml.bind.JAXBException;
 import javax.xml.bind.Unmarshaller;
 
 
 /**
  * Code generator main class
  * 
  * @author Jeff Zhang
  * @version $Revision: $
  */
 public class Main
 {
    private static final int SUCCESS = 0;
    private static final int ERROR = 1;
    private static final int OTHER = 2;
 
    /** ResourceBundle */
    private static ResourceBundle rb = ResourceBundle.getBundle("codegenerator", Locale.getDefault());
    
    /**
     * Code generator stand alone tool
     * 
     * @param args command line arguments
     */
    public static void main(String[] args)
    {
       String outputDir = "out"; //default output directory
       String defxml = null;
       int arg = 0;
  
       if (args.length > 0)
       {
          while (args.length > arg + 1)
          {
             if (args[arg].startsWith("-"))
             {
                if (args[arg].equals("-o"))
                {
                   arg++;
                   if (arg >= args.length)
                   {
                      usage();
                      System.exit(OTHER);
                   }
                   outputDir = args[arg];
                }
                else if (args[arg].equals("-f"))
                {
                   arg++;
                   if (arg >= args.length)
                   {
                      usage();
                      System.exit(OTHER);
                   }
                   defxml = args[arg];
                }
             } 
             else
             {
                usage();
                System.exit(OTHER);
             }
             arg++;
          }
       }
       try 
       {
          File out = new File(outputDir);
          Utils.recursiveDelete(out);
 
          Definition def = null;
          
          if (defxml == null)
             def = inputFromCommandLine();
          else
             def = inputFromXml(defxml);
          
          if (def == null)
             System.exit(ERROR);
          
          def.setOutputDir(outputDir);
 
          Profile profile;
          if (def.getVersion().equals("1.6"))
          {
             profile = new JCA16Profile();
          }
          else if (def.getVersion().equals("1.5"))
          {
             profile = new JCA15Profile();
          }
          else
          {
             profile = new JCA10Profile();
          }
          profile.generate(def);
          
          if (def.getBuild().equals("ant"))
             copyAllJars(outputDir);
          
          System.out.println(rb.getString("code.wrote"));
          System.exit(SUCCESS);
       }
       catch (IOException e)
       {
          e.printStackTrace();
       }
       catch (JAXBException e)
       {
          e.printStackTrace();
       }
    }
    
    /**
     * input from xml file
     * 
     * @param defxml definition xml file
     * @throws IOException ioException
     * @throws JAXBException jaxb exception
     * @return Definition definition from input
     */
    private static Definition inputFromXml(String defxml) throws IOException, JAXBException
    {
       JAXBContext context = JAXBContext.newInstance("org.jboss.jca.codegenerator");
       Unmarshaller unmarshaller = context.createUnmarshaller();
       Definition def = (Definition)unmarshaller.unmarshal(new File(defxml));
       //System.out.println(def.getVersion());
 
       return def;
    }
 
    /**
     * input from command line 
     * 
     * @throws IOException ioException
     * @return Definition definition from input
     */
    private static Definition inputFromCommandLine() throws IOException
    {
       BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
       Definition def = new Definition();
       
       //profile version
       String version = null;
       do
       {
          System.out.print(rb.getString("profile.version") + "[1.6]: ");
          version = in.readLine();
          if (version == null || version.equals(""))
             version = "1.6";
       }
       while (!(version.equals("1.6") || version.equals("1.5") || version.equals("1.0")));
       def.setVersion(version);
       
       //by default, support outbound, but not inbound
       def.setSupportOutbound(true);
       def.setSupportInbound(false);
       
       //bound
       if (!version.equals("1.0"))
       {
          System.out.print(rb.getString("support.bound") + "[O]: ");
          String bound = in.readLine();
          if (bound == null || bound.equals("") || bound.equals("O") || bound.equals("o") || bound.equals("Outbound"))
          {
             //keep default bound 
          }
          else if (bound.equals("I") || bound.equals("i") || bound.equals("Inbound"))
          {
             def.setSupportOutbound(false);
             def.setSupportInbound(true);
          }
          else if (bound.equals("B") || bound.equals("b") || bound.equals("Bidirectional"))
          {
             def.setSupportOutbound(true);
             def.setSupportInbound(true);
          }
       }
       
       //transaction
       if (def.isSupportOutbound())
       {
          System.out.print(rb.getString("support.transaction") + "[N]: ");
          String trans = in.readLine();
          if (trans == null || trans.equals(""))
             def.setSupportTransaction("NoTransaction");
          else if (trans.equals("L") || trans.equals("l") || trans.equals("LocalTransaction"))
          {
             def.setSupportTransaction("LocalTransaction");
          }
          else if (trans.equals("X") || trans.equals("x") || trans.equals("XATransaction"))
          {
             def.setSupportTransaction("XATransaction");
          }
          else
          {
             def.setSupportTransaction("NoTransaction");
          }
       }
       
       //package name
       System.out.print(rb.getString("package.name"));
       String packageName = in.readLine();
       def.setRaPackage(packageName);
       
       //support annotation
       if (version.equals("1.6"))
       {
          System.out.print(rb.getString("use.annotation") + "[Y]: ");
          String useAnnotation = in.readLine();
          if (useAnnotation == null)
             def.setUseAnnotation(true);
          else
          {
             if (useAnnotation.equals("N") || useAnnotation.equals("n") || useAnnotation.equals("No"))
                def.setUseAnnotation(false);
             else
                def.setUseAnnotation(true);
          }
       }
       else
       {
          def.setUseAnnotation(false);
       }
 
       //use resource adapter
       if (def.isSupportOutbound() && !def.isSupportInbound() && (version.equals("1.6") || version.equals("1.5")))
       {
          System.out.print(rb.getString("use.ra") + "[Y]: ");
          String useRa = in.readLine();
          if (useRa == null)
             def.setUseRa(true);
          else
          {
             if (useRa.equals("N") || useRa.equals("n") || useRa.equals("No"))
                def.setUseRa(false);
             else
                def.setUseRa(true);
          }
       }
       else if (version.equals("1.0"))
       {
          def.setUseRa(false);
       }
       else
       {
          def.setUseRa(true);
       }
       
       //input ra class name
       if (def.isUseRa() || def.isSupportInbound())
       {
          System.out.print(rb.getString("ra.class.name"));
          System.out.print("[" + def.getRaClass() + "]: ");
          String raClassName = in.readLine();
          if (raClassName != null && !raClassName.equals(""))
          {
             def.setRaClass(raClassName);
             setDefaultValue(def, raClassName, "ResourceAdapter");
             setDefaultValue(def, raClassName, "Ra");
          }
          
          List<ConfigPropType> raProps = inputProperties("ra", in, false);
          def.setRaConfigProps(raProps);
       }
       
       //outbound
       if (def.isSupportOutbound())
       {
          System.out.print(rb.getString("mcf.class.name"));
          System.out.print("[" + def.getMcfClass() + "]: ");
          String mcfClassName = in.readLine();
          if (mcfClassName != null && !mcfClassName.equals(""))
          {
             def.setMcfClass(mcfClassName);
             setDefaultValue(def, mcfClassName, "ManagedConnectionfactory");
             setDefaultValue(def, mcfClassName, "Mcf");
          }
   
          List<ConfigPropType> mcfProps = inputProperties("mcf", in, false);
          def.setMcfConfigProps(mcfProps);
 
          if (def.isUseRa())
          {
             System.out.print(rb.getString("mcf.impl.raa") + "[Y]: ");
             String raAssociation = in.readLine();
             if (raAssociation == null || raAssociation.equals(""))
                def.setImplRaAssociation(true);
             else
             {
                if (raAssociation.equals("Y") || raAssociation.equals("y") || raAssociation.equals("Yes"))
                   def.setImplRaAssociation(true);
                else
                   def.setImplRaAssociation(false);
             }
          }
          
          System.out.print(rb.getString("mc.class.name"));
          System.out.print("[" + def.getMcClass() + "]: ");
          String mcClassName = in.readLine();
          if (mcClassName != null && !mcClassName.equals(""))
             def.setMcClass(mcClassName);
          
          System.out.print(rb.getString("mcf.use.cci") + "[N]: ");
          String useCciConnection = in.readLine();
          if (useCciConnection == null)
             def.setUseCciConnection(false);
          else
          {
             if (useCciConnection.equals("Y") || useCciConnection.equals("y") || useCciConnection.equals("Yes"))
                def.setUseCciConnection(true);
             else
                def.setUseCciConnection(false);
          }
          
          if (!def.isUseCciConnection())
          {
             System.out.print(rb.getString("cf.interface.name"));
             System.out.print("[" + def.getCfInterfaceClass() + "]: ");
             String cfInterfaceName = in.readLine();
             if (cfInterfaceName != null && !cfInterfaceName.equals(""))
                def.setCfInterfaceClass(cfInterfaceName);
             
             System.out.print(rb.getString("cf.class.name"));
             System.out.print("[" + def.getCfClass() + "]: ");
             String cfClassName = in.readLine();
             if (cfClassName != null && !cfClassName.equals(""))
                def.setCfClass(cfClassName);
   
             System.out.print(rb.getString("conn.interface.name"));
             System.out.print("[" + def.getConnInterfaceClass() + "]: ");
             String connInterfaceName = in.readLine();
             if (connInterfaceName != null && !connInterfaceName.equals(""))
                def.setConnInterfaceClass(connInterfaceName);
             
             System.out.print(rb.getString("conn.class.name"));
             System.out.print("[" + def.getConnImplClass() + "]: ");
             String connImplName = in.readLine();
             if (connImplName != null && !connImplName.equals(""))
                def.setConnImplClass(connImplName);
             
             System.out.print(rb.getString("connection.method.support") + "[N]: ");
             String supportMethod = in.readLine();
             if (supportMethod == null)
                def.setDefineMethodInConnection(false);
             else
             {
                if (supportMethod.equals("Y") || supportMethod.equals("y") || supportMethod.equals("Yes"))
                   def.setDefineMethodInConnection(true);
                else
                   def.setDefineMethodInConnection(false);
             }
             if (def.isDefineMethodInConnection())
             {
                def.setMethods(inputMethod(in));
             }
          }
       }
 
       //inbound
       if (def.isSupportInbound())
       {
          System.out.print(rb.getString("ml.interface.name"));
          System.out.print("[" + def.getMlClass() + "]: ");
          String mlClassName = in.readLine();
          if (mlClassName != null && !mlClassName.equals(""))
          {
             def.setMlClass(mlClassName);
             setDefaultValue(def, mlClassName, "MessageListener");
             setDefaultValue(def, mlClassName, "Ml");
          }
 
          System.out.print(rb.getString("as.class.name"));
          System.out.print("[" + def.getAsClass() + "]: ");
          String asClassName = in.readLine();
          if (asClassName != null && !asClassName.equals(""))
             def.setAsClass(asClassName);
          
          List<ConfigPropType> asProps = inputProperties("as", in, true);
          def.setAsConfigProps(asProps);
          
          System.out.print(rb.getString("acti.class.name"));
          System.out.print("[" + def.getActivationClass() + "]: ");
          String actiClassName = in.readLine();
          if (actiClassName != null && !actiClassName.equals(""))
             def.setActivationClass(actiClassName);
       }
       
       //admin object
       System.out.print(rb.getString("gen.adminobject") + "[N]: ");
       String genAo = in.readLine();
       if (genAo == null)
          def.setGenAdminObject(false);
       else
       {
          if (genAo.equals("Y") || genAo.equals("y") || genAo.equals("Yes"))
             def.setGenAdminObject(true);
          else
             def.setGenAdminObject(false);
       }
       
       if (def.isGenAdminObject())
       {
          System.out.print(rb.getString("adminobject.raa") + "[Y]: ");
          String aoRaAssociation = in.readLine();
          if (aoRaAssociation == null || aoRaAssociation.equals(""))
             def.setAdminObjectImplRaAssociation(true);
          else
          {
             if (aoRaAssociation.equals("Y") || aoRaAssociation.equals("y") || aoRaAssociation.equals("Yes"))
                def.setAdminObjectImplRaAssociation(true);
             else
                def.setAdminObjectImplRaAssociation(false);
          }
       }
       int numOfAo = 0;
       while (numOfAo >= 0 && def.isGenAdminObject())
       {
         String strOrder = numOfAo > 0 ? new Integer(numOfAo).toString() : "";
          AdminObjectType aoType = new AdminObjectType();
          System.out.print(rb.getString("adminobject.interface.name"));
          System.out.print("[" + def.getDefaultValue() + strOrder + "AdminObjectInterface]: ");
          String aoInterfaceName = in.readLine();
          if (aoInterfaceName != null && !aoInterfaceName.equals(""))
          {
             aoType.setAdminObjectInterface(aoInterfaceName);
          }
          else
          {
             aoType.setAdminObjectInterface(def.getDefaultValue() + strOrder + "AdminObjectInterface");
          }
          
          System.out.print(rb.getString("adminobject.class.name"));
          System.out.print("[" + def.getDefaultValue() + strOrder + "AdminObjectImpl]: ");
          String aoClassName = in.readLine();
          if (aoClassName != null && !aoClassName.equals(""))
          {
             aoType.setAdminObjectClass(aoClassName);
          }
          else
          {
             aoType.setAdminObjectClass(def.getDefaultValue() + strOrder + "AdminObjectImpl");
          }
    
          List<ConfigPropType> aoProps = inputProperties("adminobject", in, false);
          aoType.setAoConfigProps(aoProps);
          
          if (def.getAdminObjects() == null)
             def.setAdminObjects(new ArrayList<AdminObjectType>());
          def.getAdminObjects().add(aoType);
          
          System.out.print(rb.getString("gen.adminobject.other") + "[N]: ");
          String genAoAgain = in.readLine();
          if (genAoAgain == null)
             numOfAo = -1;
          else
          {
             if (genAoAgain.equals("Y") || genAoAgain.equals("y") || genAoAgain.equals("Yes"))
                numOfAo++;
             else
                numOfAo = -1;
          }
       }
       
       if (!def.isUseCciConnection())
       {
          //generate mbean classes
          System.out.print(rb.getString("gen.mbean") + "[Y]: ");
          String genMbean = in.readLine();
          if (genMbean == null)
             def.setGenMbean(true);
          else
          {
             if (genMbean.equals("N") || genMbean.equals("n") || genMbean.equals("No"))
                def.setGenMbean(false);
             else
                def.setGenMbean(true);
          }
       }
       
       //build environment
       System.out.print(rb.getString("build.env"));
       System.out.print("[" + def.getBuild() + "]: ");
       String buildEnv = in.readLine();
       if (buildEnv != null && !buildEnv.equals(""))
       {
          if (buildEnv.equalsIgnoreCase("i") || 
             buildEnv.equalsIgnoreCase("ant+ivy") ||
             buildEnv.equalsIgnoreCase("ivy"))
          {
             def.setBuild("ivy");
          }
          else if (buildEnv.equalsIgnoreCase("m") || 
             buildEnv.equalsIgnoreCase("maven"))
          {
             def.setBuild("maven");
          }
          else
             def.setBuild("ant");
       }
       else
          def.setBuild("ant");
 
       return def;
    }
 
    /**
     * check defalut value and set it
     * 
     * @param def definition 
     * @param className
     * @param stringvalue post-fix string 
     */
    private static void setDefaultValue(Definition def, String className, String stringvalue)
    {
       if (className.endsWith(stringvalue))
          def.setDefaultValue(className.substring(0, className.length() - stringvalue.length()));
    }
 
    /**
     * copy all jars
     * @param outputDir output directory
     * @throws IOException ioException
     */
    private static void copyAllJars(String outputDir) throws IOException
    {
       File out = new File(outputDir);
       String path = out.getAbsolutePath();
       String targetPath = path + File.separatorChar + "lib";
 
       String libPath = path + File.separatorChar + ".." + File.separatorChar + ".." + File.separatorChar + 
          ".." + File.separatorChar + "lib";
       Utils.copyFolder(libPath, targetPath, "jar");
 
       String binPath = path + File.separatorChar + ".." + File.separatorChar + ".." + File.separatorChar + 
          ".." + File.separatorChar + "bin";
       Utils.copyFolder(binPath, targetPath, "jar");
    }
 
    /**
     * Input Properties
     * @param classname belong to which java class
     * @param in BufferedReader
     * @param supportRequired need input required property
     * @return List<ConfigPropType> list of properties
     * @throws IOException ioException
     */
    private static List<ConfigPropType> inputProperties(String classname, 
       BufferedReader in, boolean supportRequired) 
       throws IOException
    {
       List<ConfigPropType> props = new ArrayList<ConfigPropType>();
       while (true)
       {
          System.out.println(rb.getString(classname + ".config.properties"));
          System.out.print("    " + rb.getString("config.properties.name"));
          String name = in.readLine();
          if (name == null || name.equals(""))
             break;
          System.out.print("    " + rb.getString("config.properties.type"));
          String type = in.readLine();
 
          if (!BasicType.isBasicType(type))
          {
             System.out.print(rb.getString("config.properties.type.tip") + " [");
             System.out.println(BasicType.allBasicType() + "]");
             continue;
          }
          System.out.print("    " + rb.getString("config.properties.value"));
          String value = in.readLine();
          boolean required = false;
          if (supportRequired)
          {
             System.out.print("    " + rb.getString("config.properties.required") + "[N]: ");
             String propRequired = in.readLine();
             if (propRequired == null)
                required = false;
             else
             {
                if (propRequired.equals("Y") || propRequired.equals("y") || propRequired.equals("Yes"))
                   required = true;
                else
                   required = false;
             }
          }
          System.out.println();
          
          ConfigPropType config = new ConfigPropType(name, type, value, required);
          props.add(config);
       }
       return props;
    }
    
    /**
     * Input Methods
     * @param in BufferedReader
     * @return List<MethodForConnection> list of properties
     * @throws IOException ioException
     */
    private static List<MethodForConnection> inputMethod(BufferedReader in) 
       throws IOException
    {
       List<MethodForConnection> methods = new ArrayList<MethodForConnection>();
       while (true)
       {
          System.out.print("    " + rb.getString("connection.method.name"));
          String methodName = in.readLine();
          if (methodName == null || methodName.equals(""))
             break;
          MethodForConnection method = new MethodForConnection();
          method.setMethodName(methodName);
          System.out.print("    " + rb.getString("connection.method.return"));
          String methodReturn = in.readLine();
          if (!(methodReturn == null || methodReturn.equals("")))
             method.setReturnType(methodReturn);
          while (true)
          {
             System.out.print("    " + rb.getString("connection.method.param.name"));
             String paramName = in.readLine();
             if (paramName == null || paramName.equals(""))
                break;
             String paramType = null;
             while (true)
             {
                System.out.print("    " + rb.getString("connection.method.param.type"));
                paramType = in.readLine();
                if (BasicType.isBasicType(paramType) || BasicType.isPrimitiveType(paramType))
                   break;
                System.out.print(rb.getString("config.properties.type.tip") + " [");
                System.out.println(BasicType.allType() + "]");
             }
             
             MethodParam param = method.newParam(paramName, paramType);
             method.getParams().add(param);
          }
          
          while (true)
          {
             System.out.print("    " + rb.getString("connection.method.exception"));
             String exceptions = in.readLine();
             if (exceptions == null || exceptions.equals(""))
                break;
             method.getExceptionType().add(exceptions);
          }
          methods.add(method);
       }
       
       return methods;
    
    }
 
    /**
     * Tool usage
     */
    private static void usage()
    {
       System.out.println("Usage: codegenerator [-o directory]");
    }
 
 }
