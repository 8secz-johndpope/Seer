 /**
  * Redistribution and use of this software and associated documentation
  * ("Software"), with or without modification, are permitted provided
  * that the following conditions are met:
  *
  * 1. Redistributions of source code must retain copyright
  *    statements and notices.  Redistributions must also contain a
  *    copy of this document.
  *
  * 2. Redistributions in binary form must reproduce the
  *    above copyright notice, this list of conditions and the
  *    following disclaimer in the documentation and/or other
  *    materials provided with the distribution.
  *
  * 3. The name "Exolab" must not be used to endorse or promote
  *    products derived from this Software without prior written
  *    permission of Exoffice Technologies.  For written permission,
  *    please contact info@exolab.org.
  *
  * 4. Products derived from this Software may not be called "Exolab"
  *    nor may "Exolab" appear in their names without prior written
  *    permission of Exoffice Technologies. Exolab is a registered
  *    trademark of Exoffice Technologies.
  *
  * 5. Due credit should be given to the Exolab Project
  *    (http://www.exolab.org/).
  *
  * THIS SOFTWARE IS PROVIDED BY EXOFFICE TECHNOLOGIES AND CONTRIBUTORS
  * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
  * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
  * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
  * EXOFFICE TECHNOLOGIES OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
  * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
  * OF THE POSSIBILITY OF SUCH DAMAGE.
  *
  * Copyright 1999 (C) Exoffice Technologies Inc. All Rights Reserved.
  *
  * $Id$
  */
 package org.openejb.util.resources;
 
 import java.util.ListResourceBundle;
 
 public class Messages extends ListResourceBundle{
 
  	/*
  	 * Error code prefixes:
  	 *
  	 * as   = Assembler
  	 * cc   = Container
  	 * cm   = ContainerManager
  	 * cs   = ContainerSystem
  	 * di   = DeploymentInfo
  	 * ge   = General Exception
  	 * im   = InstanceManager
  	 * ps   = PassivationStrategy
 	 * sa   = Server adapter
 	 * se   = Serializer
  	 * ss   = SecurityService
  	 * ts   = TransactionService
 	 */
 
  	static final Object[][] contents = {
  	// LOCALIZE THIS
{"ge0001", "FATAL ERROR: Unknown error in {0}.  Please send the following stack trace and this message to openejb-bugs@exolab.org :\n {1}"},
 {"ge0002", "The required properties object needed by {0} is null ."},// param 0 is the part of the system that needs the Properties object.
 {"ge0003", "Properties file {0} for {1} not found."}, //param 0 is the properties file name, param 1 is the part of the system that needs the properties file.
 {"ge0004", "Environment entry {0} not found in {1}."}, //param 0 is the property name, param 1 is the properties file name.
 {"ge0005", "Environment entry {0} contains illegal value {1}."}, //param 0 is the property name, param 1 is the illegal value.
 {"ge0006", "Environment entry {0} contains illegal value {1}. {2}"}, //param 0 is the property name, param 1 is the illegal value, param 2 is an additional message.
 {"ge0007", "The {0} cannot find and load the class {1}."}, //param 0 is the part of the system that needs the class, param 1 is the class that cannot be found.
 {"ge0008", "The {0} cannot instaniate the class {1}, the class or initializer is not accessible."},//param 0 is the part of the system that needs the class, param 1 is the class that cannot be accessed.
 {"ge0009", "The {0} cannot instaniate the class {1}, the class may be abstract or an interface."},//param 0 is the part of the system that needs the class, param 1 is the class that cannot be accessed.
 {"ge0010", "The {0} cannot locate the class {1}, the codebase {2} cannot be accessed. Received message: {3}"},
 {"ge0011", "The {0} cannot instaniate the class {1}:  Recieved exception {3}: {4}"},
 {"ge0012", "The {0} cannot instaniate the class {1} loaded from codebase {2}:  Recieved exception {3}: {4}"},
 {"cl0001", "Invalid codebase URI [{0}]. Received message: {1}"},
 {"cl0002", "Cannot access codebase [{0}]. Received message: {1}"},
 {"cl0003", "Error while loading remote interface {0} for bean {1}. Received message: {2}"},
 {"cl0004", "Error while loading home interface {0} for bean {1}. Received message: {2}"},
 {"cl0005", "Error while loading bean class {0} for bean {1}. Received message: {2}"},
 {"cl0006", "Error while loading primary key class {0} for bean {1}. Received message: {2}"},
 {"cl0007", "Cannot locate the class {0} from the codebase [{1}]"},
 {"as0001", "FATAL ERROR: Error in XML configuration file.  Received {0} from the parser stating {1} at line {2} column {3}."},// param 0 type of error, param 1 error message from parser, param 2 line number, param 3 column number.
 {"as0002", "Cannot load the container {0}.  Received message: {1}"},// param 0 type of error, param 1 error message from parser, param 2 line number, param 3 column number.
 {"as0003", "Cannot instantiate the container {0}.  Received message: {1}"},// param 0 type of error, param 1 error message from parser, param 2 line number, param 3 column number.
 {"as0004", "Cannot initialize the container {0}.  Received message: {1}"},// param 0 type of error, param 1 error message from parser, param 2 line number, param 3 column number.
 {"sa0001", "{0}: Connection to reset by peer."},//param 0 is the name of the server adapter.
 {"file.0010" ,"Cannot close file {0}. Received message: {1}"},
 {"file.0020" ,"Cannot open file {0}. Received message: {1}"},
 
 /*------------------------------------------
     Conf code key
    -------------------
    
    Forth Digit in Code
    0... = Misc
    1... = OpenEJB Configuration
    2... = openejb-jar
    3... = ejb-jar
    4... = service-jar
    
    Thrid Digit in Code
    .1.. = During input
    .0.. = During output
    
    Second Digit in Code
    ..0. = Finding
    ..1. = Reading
    ..2. = Unmarshalling file
    ..3. = Validating file
    ..4. = Validating data
    ..5. = Marshalling data
    ..6. = Writing
 --------------------------------- */
 
 {"conf.0001" ,"Jar file not found: {0}. Received message: {1}" },
 {"conf.0002" ,"Cannot read jar file {0}. Received message: {1}" },
 {"conf.0003" ,"Cannot write file {0} to jar {1}. Received message: {2}"},
 {"conf.0004" ,"Unable to load bean deployments from jar {0}.  {1}"},
 {"conf.0005" ,"Unable to load properties file {0}.  {1}"},
 {"conf.0006" ,"Properties file {0} not found.  Received message: {1}"},
 {"conf.0007" ,"Cannot read properties file {0}.  {1}"},
 {"conf.0008" ,"Jar {0} cannot be assembled.  The number of beans deployed ({2}) does not match the number of beans actually in the jar ({1}).  Please redeploy this jar."},
 {"conf.0009" ,"Security role reference {0} is not linked.  The reference will be linked to the OpenEJB default security role.  Bean name is {1}, jar file is {2}."},
 {"conf.0010" ,"Properties file {0} for JndiProvider {1} not found.  Received message: {2}"},
 {"conf.0011" ,"Cannot read properties file {0} for JndiProvider {1}.  Received message: {2}"},
 {"conf.0012" ,"Error while parsing properties.  Received message: {0}"},
 {"conf.0013" ,"Cannot create properties for service provider {0} in jar {1}.  {2}"},
 {"conf.0014" ,"Cannot create properties for {0} with ID {1} in config file {2}.  {3}"},
 {"conf.0100" ,"The Deployment ID {0} is already in use.  Please redeploy jar {1}."},
 {"conf.0102" ,"Role {1} already present.  Jar {0} declares a security role already present in another jar.  The role will be mapped to the pre-existing role by the same name."},
 {"conf.0101" ,"Invalid configuration {0}.  ID {1} is not unique!  Container IDs cannot be duplicated."},
 {"conf.0103" ,"Invalid configuration {0}.  ID {1} is not unique!  JndiProvider IDs cannot be duplicated."},
 {"conf.0104" ,"Invalid configuration {0}.  ID {1} is not unique!  Connector IDs cannot be duplicated."},
 {"conf.1040" ,"Cannot validate the OpenEJB configuration data. Received message: {1}"},
 {"conf.1050" ,"Cannot marshal the OpenEJB configuration data to file {0}. Received message: {1}"},
 {"conf.1060" ,"Cannot write the OpenEJB configuration file {0}. Received message: {1}"},
 {"conf.1110" ,"Cannot read the OpenEJB configuration file {0}. Received message: {1}"},
 {"conf.1120" ,"Cannot unmarshal the OpenEJB configuration in file {0}. Received message: {1}"},
 {"conf.1121" ,"Cannot unmarshal the OpenEJB configuration file {0}.  Recived error, host [{1}] not found. If your xml references a DTD on a remote system, you must be connected to a network to download the DTD.  Alternatively, you can remove the DOCTYPE element."},
 {"conf.1130" ,"Cannot validate the OpenEJB configuration file {0}. Received message: {1}"},
 {"conf.1900" ,"Cannot find the OpenEJB configuration file {0}. Received message: {1}"},
 {"conf.2040" ,"Cannot validate the openejb-jar data. Received message: {1}"},
 {"conf.2050" ,"Cannot marshal the openejb-jar data to file {0}. Received message: {1}"},
 {"conf.2060" ,"Cannot write the openejb-jar.xml file {0}. Received message: {1}"},
 {"conf.2110" ,"Cannot read the openejb-jar.xml in jar {0}. Received message: {1}"},
 {"conf.2120" ,"Cannot unmarshal the openejb-jar.xml file in jar {0}. Received message: {1}"},
 {"conf.2121" ,"Cannot unmarshal the openejb-jar.xml in jar {0}.  Error, host [{1}] not found. If your xml references a DTD on a remote system, you must be connected to a network to download the DTD.  Alternatively, you can remove the DOCTYPE element."},
 {"conf.2130" ,"Cannot validate openejb-jar.xml file in jar {0}. Received message: {1}"},
 {"conf.2900" ,"Cannot find the openejb-jar.xml in jar {0}. Received message: {1}"},
 {"conf.3040" ,"Cannot validate the ejb-jar data. Received message: {1}"},
 {"conf.3050" ,"Cannot marshal the ejb-jar data to file {0}. Received message: {1}"},
 {"conf.3060" ,"Cannot write the ejb-jar.xml file {0}. Received message: {1}"},
 {"conf.3110" ,"Cannot read the ejb-jar.xml in jar {0}. Received message: {1}"},
 {"conf.3120" ,"Cannot unmarshal the ejb-jar.xml file in jar {0}. Received message: {1}"},
 {"conf.3121" ,"Cannot unmarshal the ejb-jar.xml in jar {0}.  Error, host [{1}] not found. If your ejb-jar.xml references a DTD on a remote system, you must be connected to a network to download the DTD.  Alternatively, you can remove the DOCTYPE element."},
 {"conf.3130" ,"Cannot validate ejb-jar.xml file in jar {0}. Received message: {1}"},
 {"conf.3900" ,"Cannot find the ejb-jar.xml in jar {0}. Received message: {1}"},
 {"conf.4040" ,"Cannot validate the service-jar data. Received message: {1}"},
 {"conf.4050" ,"Cannot marshal the service-jar data to file {0}. Received message: {1}"},
 {"conf.4060" ,"Cannot write the service-jar.xml file {0}. Received message: {1}"},
 {"conf.4110" ,"Cannot read the service-jar.xml in jar {0}. Received message: {1}"},
 {"conf.4120" ,"Cannot unmarshal the service-jar.xml file in jar {0}. Received message: {1}"},
 {"conf.4121" ,"Cannot unmarshal the service-jar.xml in jar {0}.  Error, host [{1}] not found. If your xml references a DTD on a remote system, you must be connected to a network to download the DTD.  Alternatively, you can remove the DOCTYPE element."},
 {"conf.4130" ,"Cannot validate service-jar.xml file in jar {0}. Received message: {1}"},
 {"conf.4900" ,"Cannot find the service-jar.xml in jar {0}. Received message: {1}"},
 {"conf.4901" ,"Cannot find the ServiceProvider id {0} in the service-jar.xml of jar {0}. Check that your OpenEJB configuration file is point to the right jar file and the right ServiceProvider id."},
 {"conf.4902" ,"ServiceProvider of id \"{0}\" in the service-jar.xml of jar \"{1}\" is not of type \"{2}\""},
  	
 
 {"init.0100" , "Invalid {0} provider {1}.  The factory class specified, {2}, does not implement the {3} interface.  Please check the configuration of {1}."},
 
 
 // END OF MATERIAL TO LOCALIZE
  	};
 
  	public Object[][] getContents() {
  		return contents;
  	} 
 }
