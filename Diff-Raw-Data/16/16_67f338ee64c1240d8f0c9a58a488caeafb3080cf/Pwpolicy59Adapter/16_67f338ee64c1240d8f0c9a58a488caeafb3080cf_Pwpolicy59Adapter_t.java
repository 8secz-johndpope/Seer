 // Copyright (C) 2012 jOVAL.org.  All rights reserved.
 // This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
 
 package org.joval.oval.adapter.macos;
 
 import java.util.Collection;
 import java.util.Hashtable;
 import java.util.Vector;
 import javax.xml.bind.JAXBElement;
 
 import oval.schemas.common.MessageLevelEnumeration;
 import oval.schemas.common.MessageType;
 import oval.schemas.common.OperationEnumeration;
 import oval.schemas.common.SimpleDatatypeEnumeration;
 import oval.schemas.definitions.core.ObjectType;
 import oval.schemas.definitions.core.EntityObjectStringType;
 import oval.schemas.definitions.macos.Pwpolicy59Object;
 import oval.schemas.systemcharacteristics.core.ItemType;
 import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
 import oval.schemas.systemcharacteristics.core.EntityItemIntType;
 import oval.schemas.systemcharacteristics.core.EntityItemStringType;
 import oval.schemas.systemcharacteristics.core.FlagEnumeration;
 import oval.schemas.systemcharacteristics.macos.Pwpolicy59Item;
 import oval.schemas.results.core.ResultEnumeration;
 
 import org.joval.intf.plugin.IAdapter;
 import org.joval.intf.plugin.IRequestContext;
 import org.joval.intf.system.IBaseSession;
 import org.joval.intf.unix.system.IUnixSession;
 import org.joval.oval.Factories;
 import org.joval.oval.CollectException;
 import org.joval.util.JOVALMsg;
 import org.joval.util.SafeCLI;
import org.joval.util.StringTools;
 
 /**
  * Retrieves Pwpolicy59Items.
  *
  * @author David A. Solin
  * @version %I% %G%
  */
 public class Pwpolicy59Adapter implements IAdapter {
     private IUnixSession session;
 
     // Implement IAdapter
 
     public Collection<Class> init(IBaseSession session) {
 	Collection<Class> classes = new Vector<Class>();
 	if (session instanceof IUnixSession) {
 	    if (((IUnixSession)session).getFlavor() == IUnixSession.Flavor.MACOSX) {
 		this.session = (IUnixSession)session;
 		classes.add(Pwpolicy59Object.class);
 	    }
 	}
 	return classes;
     }
 
     public Collection<Pwpolicy59Item> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
 	Collection<Pwpolicy59Item> items = new Vector<Pwpolicy59Item>();
 	Pwpolicy59Object pObj = (Pwpolicy59Object)obj;
 
 	Pwpolicy59Item item = Factories.sc.macos.createPwpolicy59Item();
 	StringBuffer sb = new StringBuffer("pwpolicy -getpolicy -u ");
 	OperationEnumeration op = pObj.getTargetUser().getOperation();
 	if (op == OperationEnumeration.EQUALS) {
 	    String value = (String)pObj.getTargetUser().getValue();
 	    sb.append(value);
 	    EntityItemStringType targetUser = Factories.sc.core.createEntityItemStringType();
 	    targetUser.setDatatype(SimpleDatatypeEnumeration.STRING.value());
 	    targetUser.setValue(value);
 	    item.setTargetUser(targetUser);
 	} else {
 	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
 	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
 	}
 	if (!pObj.getUsername().isNil()) {
 	    op = pObj.getUsername().getValue().getOperation();
 	    if (op == OperationEnumeration.EQUALS) {
 		String value = (String)pObj.getUsername().getValue().getValue();
 		sb.append(" -a ").append(value);
 		EntityItemStringType username = Factories.sc.core.createEntityItemStringType();
 		username.setDatatype(SimpleDatatypeEnumeration.STRING.value());
 		username.setValue(value);
 		item.setUsername(Factories.sc.macos.createPwpolicy59ItemUsername(username));
 	    } else {
 		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
 		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
 	    }
 	}
 	if (!pObj.getUserpass().isNil()) {
 	    op = pObj.getUserpass().getValue().getOperation();
 	    if (op == OperationEnumeration.EQUALS) {
 		String value = (String)pObj.getUserpass().getValue().getValue();
 		sb.append(" -p ").append(value);
 		EntityItemStringType userpass = Factories.sc.core.createEntityItemStringType();
 		userpass.setDatatype(SimpleDatatypeEnumeration.STRING.value());
 		userpass.setValue(value);
 		item.setUserpass(Factories.sc.macos.createPwpolicy59ItemUserpass(userpass));
 	    } else {
 		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
 		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
 	    }
 	}
 	if (!pObj.getDirectoryNode().isNil()) {
 	    op = pObj.getDirectoryNode().getValue().getOperation();
 	    if (op == OperationEnumeration.EQUALS) {
 		String value = (String)pObj.getDirectoryNode().getValue().getValue();
 		sb.append(" -n ").append(value);
 		EntityItemStringType directoryNode = Factories.sc.core.createEntityItemStringType();
 		directoryNode.setDatatype(SimpleDatatypeEnumeration.STRING.value());
 		directoryNode.setValue(value);
 		item.setDirectoryNode(Factories.sc.macos.createPwpolicy59ItemDirectoryNode(directoryNode));
 	    } else {
 		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
 		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
 	    }
 	}
 
 	try {
 	    Hashtable<String, String> policies = new Hashtable<String, String>();
 	    for (String line : SafeCLI.multiLine(sb.toString(), session, IUnixSession.Timeout.S)) {
		if (line.indexOf("=") > 0) {
		    for (String pair : StringTools.toList(StringTools.tokenize(line, " "))) {
			int ptr = line.indexOf("=");
			if (ptr != -1) {
			    String key = line.substring(0,ptr).trim();
			    String val = line.substring(ptr+1).trim();
			    policies.put(key, val);
			}
		    }
 		}
 	    }
 
 	    if (policies.containsKey("canModifyPasswordforSelf")) {
 		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
 		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
 		type.setValue(policies.get("canModifyPasswordforSelf"));
 		item.setCanModifyPasswordforSelf(type);
 	    }
 
 	    if (policies.containsKey("expirationDateGMT")) {
 		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
 		type.setDatatype(SimpleDatatypeEnumeration.STRING.value());
 		type.setValue(policies.get("expirationDateGMT"));
 		item.setExpirationDateGMT(type);
 	    }
 
 	    if (policies.containsKey("hardExpireDateGMT")) {
 		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
 		type.setDatatype(SimpleDatatypeEnumeration.STRING.value());
 		type.setValue(policies.get("hardExpireDateGMT"));
 		item.setHardExpireDateGMT(type);
 	    }
 
 	    if (policies.containsKey("maxChars")) {
 		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
 		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
 		type.setValue(policies.get("maxChars"));
 		item.setMaxChars(type);
 	    }
 
 	    if (policies.containsKey("maxFailedLoginAttempts")) {
 		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
 		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
 		type.setValue(policies.get("maxFailedLoginAttempts"));
 		item.setMaxFailedLoginAttempts(type);
 	    }
 
 	    if (policies.containsKey("maxMinutesOfNonUse")) {
 		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
 		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
 		type.setValue(policies.get("maxMinutesOfNonUse"));
 		item.setMaxMinutesOfNonUse(type);
 	    }
 
 	    if (policies.containsKey("maxMinutesUntilChangePassword")) {
 		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
 		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
 		type.setValue(policies.get("maxMinutesUntilChangePassword"));
 		item.setMaxMinutesUntilChangePassword(type);
 	    }
 
 	    if (policies.containsKey("maxMinutesUntilDisabled")) {
 		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
 		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
 		type.setValue(policies.get("maxMinutesUntilDisabled"));
 		item.setMaxMinutesUntilDisabled(type);
 	    }
 
 	    if (policies.containsKey("minChars")) {
 		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
 		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
 		type.setValue(policies.get("minChars"));
 		item.setMinChars(type);
 	    }
 
 	    if (policies.containsKey("minMinutesUntilChangePassword")) {
 		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
 		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
 		type.setValue(policies.get("minMinutesUntilChangePassword"));
 		item.setMinMinutesUntilChangePassword(type);
 	    }
 
 	    if (policies.containsKey("minutesUntilFailedLoginReset")) {
 		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
 		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
 		type.setValue(policies.get("minutesUntilFailedLoginReset"));
 		item.setMinutesUntilFailedLoginReset(type);
 	    }
 
 	    if (policies.containsKey("newPasswordRequired")) {
 		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
 		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
 		type.setValue(policies.get("newPasswordRequired"));
 		item.setNewPasswordRequired(type);
 	    }
 
 	    if (policies.containsKey("notGuessablePattern")) {
 		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
 		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
 		type.setValue(policies.get("notGuessablePattern"));
 		item.setNotGuessablePattern(type);
 	    }
 
 	    if (policies.containsKey("passwordCannotBeName")) {
 		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
 		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
 		type.setValue(policies.get("passwordCannotBeName"));
 		item.setPasswordCannotBeName(type);
 	    }
 
 	    if (policies.containsKey("requiresAlpha")) {
 		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
 		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
 		type.setValue(policies.get("requiresAlpha"));
 		item.setRequiresAlpha(type);
 	    }
 
 	    if (policies.containsKey("requiresMixedCase")) {
 		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
 		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
 		type.setValue(policies.get("requiresMixedCase"));
 		item.setRequiresMixedCase(type);
 	    }
 
 	    if (policies.containsKey("requiresNumeric")) {
 		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
 		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
 		type.setValue(policies.get("requiresNumeric"));
 		item.setRequiresNumeric(type);
 	    }
 
 	    if (policies.containsKey("requiresSymbol")) {
 		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
 		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
 		type.setValue(policies.get("requiresSymbol"));
 		item.setRequiresSymbol(type);
 	    }
 
 	    if (policies.containsKey("usingExpirationDate")) {
 		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
 		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
 		type.setValue(policies.get("usingExpirationDate"));
 		item.setUsingExpirationDate(type);
 	    }
 
 	    if (policies.containsKey("usingHardExpirationDate")) {
 		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
 		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
 		type.setValue(policies.get("usingHardExpirationDate"));
 		item.setUsingHardExpirationDate(type);
 	    }
 
 	    if (policies.containsKey("usingHistory")) {
 		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
 		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
 		type.setValue(policies.get("usingHistory"));
 		item.setUsingHistory(type);
 	    }
 
 	    items.add(item);
 	} catch (Exception e) {
 	    MessageType msg = Factories.common.createMessageType();
 	    msg.setLevel(MessageLevelEnumeration.ERROR);
 	    msg.setValue(e.getMessage());
 	    rc.addMessage(msg);
 	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
 	}
 	return items;
     }
 }
