 package org.eclipse.jdi.internal;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
 
 import java.io.ByteArrayOutputStream;
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.IOException;
 import java.lang.reflect.Field;
 import java.lang.reflect.Modifier;
 import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.eclipse.jdi.internal.connect.ConnectorImpl;
 import org.eclipse.jdi.internal.connect.PacketReceiveManager;
 import org.eclipse.jdi.internal.connect.PacketSendManager;
 import org.eclipse.jdi.internal.event.EventQueueImpl;
 import org.eclipse.jdi.internal.jdwp.JdwpCommandPacket;
 import org.eclipse.jdi.internal.jdwp.JdwpObjectID;
 import org.eclipse.jdi.internal.jdwp.JdwpReferenceTypeID;
 import org.eclipse.jdi.internal.jdwp.JdwpReplyPacket;
 import org.eclipse.jdi.internal.request.EventRequestManagerImpl;
 
 import com.sun.jdi.BooleanValue;
 import com.sun.jdi.ByteValue;
 import com.sun.jdi.CharValue;
 import com.sun.jdi.DoubleValue;
 import com.sun.jdi.FloatValue;
 import com.sun.jdi.IntegerValue;
 import com.sun.jdi.LongValue;
 import com.sun.jdi.ObjectCollectedException;
 import com.sun.jdi.ShortValue;
 import com.sun.jdi.StringReference;
 import com.sun.jdi.VMDisconnectedException;
 import com.sun.jdi.VirtualMachine;
 import com.sun.jdi.event.EventQueue;
 import com.sun.jdi.request.EventRequestManager;
 
 /**
  * This class implements the corresponding interfaces
  * declared by the JDI specification. See the com.sun.jdi package
  * for more information.
  *
  */
 public class VirtualMachineImpl extends MirrorImpl implements VirtualMachine, org.eclipse.jdi.hcr.VirtualMachine, org.eclipse.jdi.VirtualMachine {
 	/** Result flags for Classes Have Changed command. */
 	public static final byte HCR_RELOAD_SUCCESS = 0;
 	public static final byte HCR_RELOAD_FAILURE = 1;
 	public static final byte HCR_RELOAD_IGNORED = 2;
 
 	/* Indexes in HCR capabilities list.*/
 	private static final int HCR_CAN_RELOAD_CLASSES = 0;
 	private static final int HCR_CAN_GET_CLASS_VERSION = 1;
 	private static final int HCR_CAN_DO_RETURN = 2;
 	private static final int HCR_CAN_REENTER_ON_EXIT = 3;
 	
 	/** Timeout value for requests to VM if not overriden for a particular VM. */
 	private int fRequestTimeout;
 	/** Mapping of command codes to strings. */
 	
 	private static Map fgHCRResultMap = null;
 
 	/** EventRequestManager that creates event objects on request. */
 	private EventRequestManagerImpl fEventReqMgr;
 	/** EventQueue that returns EventSets from the Virtual Manager. */
 	private EventQueueImpl fEventQueue;
 	
 	/** Connector to VM. */
 	private ConnectorImpl fConnector;
 	/** If a launchingconnector is used, we store the process. */
 	private Process fLaunchedProcess;
 	
 	/**
 	 * The following field contains cached Mirrors.
 	 * Note that these are optional: their only purpose is to speed up the debugger by
 	 * being able to use the stored results of JDWP calls.
 	 */
 	private ValueCache fCachedReftypes = new ValueCache();
 	private ValueCache fCachedObjects =  new ValueCache();
 
 	/** The following are the stored results of JDWP calls. */
 	private String fVersionDescription = null;	// Text information on the VM version.
 	private int fJdwpMajorVersion;
 	private int fJdwpMinorVersion;
 	private String fVMVersion;	// Target VM JRE version, as in the java.version property.
 	private String fVMName;		// Target VM name, as in the java.vm.name property.
 	private boolean fGotIDSizes = false;
 	private int fFieldIDSize;
 	private int fMethodIDSize;
 	private int fObjectIDSize;
 	private int fReferenceTypeIDSize;
 	private int fFrameIDSize;
        
 	private boolean fGotCapabilities = false;
 	private boolean fCanWatchFieldModification;
 	private boolean fCanWatchFieldAccess;
 	private boolean fCanGetBytecodes;
 	private boolean fCanGetSyntheticAttribute;
 	private boolean fCanGetOwnedMonitorInfo;
 	private boolean fCanGetCurrentContendedMonitor;
 	private boolean fCanGetMonitorInfo;
 	private boolean fCanRedefineClasses;
 	private boolean fCanAddMethod;
 	private boolean fCanUnrestrictedlyRedefineClasses;
 	private boolean fCanPopFrames;
 	private boolean fCanUseInstanceFilters;
 	private boolean fCanGetSourceDebugExtension;
 	private boolean fCanRequestVMDeathEvent;
 	private boolean fCanSetDefaultStratum;
 	private boolean[] fHcrCapabilities = null;
 	
 	/** 
 	 * Creates a new Virtual Machine.
 	 */
 	public VirtualMachineImpl(ConnectorImpl connector) {
 		super("VirtualMachine"); //$NON-NLS-1$
 		fEventReqMgr = new EventRequestManagerImpl(this);
 		fEventQueue = new EventQueueImpl(this);
 		fConnector = connector;
 		fRequestTimeout = connector.virtualMachineManager().getGlobalRequestTimeout();
 	}
 
 	/** 
 	 * @return Returns size of JDWP ID.
 	 */
 	public final int fieldIDSize() {
 		return fFieldIDSize;
 	}
 	
 	/** 
 	 * @return Returns size of JDWP ID.
 	 */
 	public final int methodIDSize() {
 		return fMethodIDSize;
 	}
 	
 	/** 
 	 * @return Returns size of JDWP ID.
 	 */
 	public final int objectIDSize() {
 		return fObjectIDSize;
 	}
 	
 	/** 
 	 * @return Returns size of JDWP ID.
 	 */
 	public final int referenceTypeIDSize() {
 		return fReferenceTypeIDSize;
 	}
 	
 	/** 
 	 * @return Returns size of JDWP ID.
 	 */
 	public final int frameIDSize() {
 		return fFrameIDSize;
 	}
 	
 	/** 
 	 * @return Returns cached mirror object, or null if method is not in cache.
 	 */
 	public ReferenceTypeImpl getCachedMirror(JdwpReferenceTypeID ID) {
 		return (ReferenceTypeImpl)fCachedReftypes.get(ID);
 	}
 	
 	/** 
 	 * @return Returns cached mirror object, or null if method is not in cache.
 	 */
 	public ObjectReferenceImpl getCachedMirror(JdwpObjectID ID) {
 		return (ObjectReferenceImpl)fCachedObjects.get(ID);
 	}
 	
 	/** 
 	 * Adds mirror object to cache.
 	 */
 	public void addCachedMirror(ReferenceTypeImpl mirror) {
 		fCachedReftypes.put(mirror.getRefTypeID(), mirror);
 		// tbd: It is now yet possible to only ask for unload events for
 		// classes that we know of due to a limitation in the J9 VM.
 		// eventRequestManagerImpl().enableInternalClasUnloadEvent(mirror);
 	}
 	
 	/** 
 	 * Adds mirror object to cache.
 	 */
 	public void addCachedMirror(ObjectReferenceImpl mirror) {
 		fCachedObjects.put(mirror.getObjectID(), mirror);
 	}
 	
 	/**
 	 * Flushes all stored Jdwp results.
 	 */
 	public void flushStoredJdwpResults() {
 		// All known classes also become invalid.
 		Iterator iter = fCachedReftypes.values().iterator();
 		while (iter.hasNext()) {
 			ReferenceTypeImpl refType = (ReferenceTypeImpl)iter.next();
 			refType.flushStoredJdwpResults();
 		}
 			
 		fVersionDescription = null;
 		fGotIDSizes = false;
 		fHcrCapabilities = null;
 	}
 
 	/*
 	 * Removes a known class.
 	 * A class/interface is known if we have ever received its ReferenceTypeID and we have
 	 * not received an unload event for it.
 	 */
 	public final void removeKnownRefType(String signature) {
 		List refTypeList = classesBySignature(signature);
 		if (refTypeList.isEmpty())
 			return;
 
 		// If we have only one known class for this signature, we known that this is the class
 		// to be removed.
 		if (refTypeList.size() == 1) {
 			ReferenceTypeImpl refType = (ReferenceTypeImpl)refTypeList.get(0);
 			refType.flushStoredJdwpResults();
 			fCachedReftypes.remove(refType.getRefTypeID());
 			return;
 		}
 		
 		// We have more than one known class for the signature, let's find the unloaded one(s).
 		Iterator iter = refTypeList.iterator();
 		while (iter.hasNext()) {
 			ReferenceTypeImpl refType = (ReferenceTypeImpl)iter.next();
 			boolean prepared= false;
 			try {
 				prepared= refType.isPrepared();
 			} catch (ObjectCollectedException exception) {
 				// The type is unloaded. Fall through
 			}
 			if (!prepared) {
 				refType.flushStoredJdwpResults();
 				iter.remove();
 				fCachedReftypes.remove(refType.getRefTypeID());
 			}
 		}
 	}
 
 	/*
 	 * @exception Throws UnsupportedOperationException if VM does not support HCR.
 	 */
 	public void checkHCRSupported() throws UnsupportedOperationException {
 		if (!name().equals("j9")) //$NON-NLS-1$
 			throw new UnsupportedOperationException(MessageFormat.format(JDIMessages.getString("VirtualMachineImpl.Target_VM_{0}_does_not_support_Hot_Code_Replacement_1"), new String[]{name()})); //$NON-NLS-1$
 	}
 
 	/*
 	 * @return Returns Manager for receiving packets from the Virtual Machine.
 	 */
 	public final PacketReceiveManager packetReceiveManager() {
 		return fConnector.packetReceiveManager();
 	}
 
 	/*
 	 * @return Returns Manager for sending packets to the Virtual Machine.
 	 */
 	public final PacketSendManager packetSendManager() {
 		/*
 		 * Before we send out first bytes to the VM by JDI calls, we need some initial requests:
 		 * - Get the sizes of the IDs (fieldID, method ID etc.) that the VM uses;
 		 * - Request class prepare and unload events. We used these to cache classes/interfaces and map their signatures.
 		 */
 		if (!fGotIDSizes) {
 			getIDSizes();
 			if (!fGotIDSizes) {	// We can't do much without them.
 				disconnectVM();
 				throw new VMDisconnectedException(JDIMessages.getString("VirtualMachineImpl.Failed_to_get_ID_sizes_2")); //$NON-NLS-1$
 			}
 
 			// tbd: This call should be moved to addKnownRefType() when it can be made specific
 			// for a referencetype.
 			eventRequestManagerImpl().enableInternalClasUnloadEvent();
 		}
 
 		return fConnector.packetSendManager();
 	}
 
 	/**
 	 * Returns all loaded types (classes, interfaces, and array types).
 	 * For each loaded type in the target VM a ReferenceType will be placed in the returned list.
 	 */
 	public List allClasses() {
 		// Note that this information should not be cached.
 		initJdwpRequest();
 		try {
 		 	JdwpReplyPacket replyPacket = requestVM(JdwpCommandPacket.VM_ALL_CLASSES);
 			defaultReplyErrorHandler(replyPacket.errorCode());
 			DataInputStream replyData = replyPacket.dataInStream();
 			int nrOfElements = readInt("elements", replyData); //$NON-NLS-1$
 			List elements = new ArrayList(nrOfElements);
 			for (int i = 0; i < nrOfElements; i++) {
 				ReferenceTypeImpl elt = ReferenceTypeImpl.readWithTypeTagAndSignature(this, replyData);
 				if (elt == null) {
 					continue;
 				}
 				readInt("status", ReferenceTypeImpl.classStatusStrings(), replyData); //$NON-NLS-1$
 				elements.add(elt);
 			}
 			return elements;
 		} catch (IOException e) {
 			defaultIOExceptionHandler(e);
 			return null;
 		} finally {
 			handledJdwpRequest();
 		}
 
 	}
 
 	/**
 	 * @return Returns an iterator over all loaded classes.
 	 */
 	protected final Iterator allRefTypes() {
 		return allClasses().iterator();
 	}
 
 	/**
 	 * @return Returns an iterator over all cached classes.
 	 */
 	protected final Iterator allCachedRefTypes() {
 		return fCachedReftypes.values().iterator();
 	}
 
 	/**
 	 * Returns a list of the currently running threads.
 	 * For each running thread in the target VM, a ThreadReference that mirrors it is placed in the list.
 	 */
 	public List allThreads() {
 		// Note that this information should not be cached.
 		initJdwpRequest();
 		try {
 			JdwpReplyPacket replyPacket = requestVM(JdwpCommandPacket.VM_ALL_THREADS);
 			defaultReplyErrorHandler(replyPacket.errorCode());
 			DataInputStream replyData = replyPacket.dataInStream();
 			int nrOfElements = readInt("elements", replyData); //$NON-NLS-1$
 			List elements = new ArrayList(nrOfElements);
 			for (int i = 0; i < nrOfElements; i++) {
 				ThreadReferenceImpl elt = ThreadReferenceImpl.read(this, replyData);
 				if (elt == null) {
 					continue;
 				}
 				elements.add(elt);
 			}
 			return elements;
 		} catch (IOException e) {
 			defaultIOExceptionHandler(e);
 			return null;
 		} finally {
 			handledJdwpRequest();
 		}
 	}
 			
 	/**
 	 * Retrieve this VM's capabilities. 
 	 */
 	public void getCapabilities() {
 		if (fGotCapabilities)
 			return;
 		
 		int command =  JdwpCommandPacket.VM_CAPABILITIES;
 		if (isJdwpVersionGreaterOrEqual(1, 4)) {
 			command = JdwpCommandPacket.VM_CAPABILITIES_NEW;
 		}
 		
 		initJdwpRequest();
 		try {
 			JdwpReplyPacket replyPacket = requestVM(command);
 			defaultReplyErrorHandler(replyPacket.errorCode());
 			DataInputStream replyData = replyPacket.dataInStream();
 		
 			fCanWatchFieldModification = readBoolean("watch field modification", replyData); //$NON-NLS-1$
 			fCanWatchFieldAccess = readBoolean("watch field access", replyData); //$NON-NLS-1$
 			fCanGetBytecodes = readBoolean("get bytecodes", replyData); //$NON-NLS-1$
 			fCanGetSyntheticAttribute = readBoolean("synth. attr", replyData); //$NON-NLS-1$
 			fCanGetOwnedMonitorInfo = readBoolean("owned monitor info", replyData); //$NON-NLS-1$
 			fCanGetCurrentContendedMonitor = readBoolean("curr. contended monitor", replyData); //$NON-NLS-1$
 			fCanGetMonitorInfo = readBoolean("monitor info", replyData); //$NON-NLS-1$
 			if (command == JdwpCommandPacket.VM_CAPABILITIES_NEW) {
 				// extended capabilities
 				fCanRedefineClasses = readBoolean("redefine classes", replyData); //$NON-NLS-1$
 				fCanAddMethod = readBoolean("add method", replyData); //$NON-NLS-1$
 				fCanUnrestrictedlyRedefineClasses = readBoolean("unrestrictedly redefine classes", replyData); //$NON-NLS-1$
 				fCanPopFrames = readBoolean("pop frames", replyData); //$NON-NLS-1$
 				fCanUseInstanceFilters = readBoolean("use instance filters", replyData); //$NON-NLS-1$
 				fCanGetSourceDebugExtension = readBoolean("get source debug extension", replyData); //$NON-NLS-1$
 				fCanRequestVMDeathEvent = readBoolean("request vm death", replyData); //$NON-NLS-1$
 			} else {
 				fCanRedefineClasses = false;
 				fCanAddMethod = false;
 				fCanUnrestrictedlyRedefineClasses = false;
 				fCanPopFrames = false;			
 				fCanUseInstanceFilters = false;
 				fCanGetSourceDebugExtension = false;
 				fCanRequestVMDeathEvent = false;
 			}
 			fGotCapabilities = true;
 		} catch (IOException e) {
 			fGotIDSizes = false;
 			defaultIOExceptionHandler(e);
 		} finally {
 			handledJdwpRequest();
 		}
 	}
 	
 	/**
 	 * @return Returns true if this implementation supports the retrieval of a method's bytecodes.
 	 */
 	public boolean canGetBytecodes() {
 		getCapabilities();
 		return fCanGetBytecodes;
 	}
 		
 	/**
 	 * @return Returns true if this implementation supports the retrieval of the monitor for which a thread is currently waiting.
 	 */
 	public boolean canGetCurrentContendedMonitor() {
 		getCapabilities();
 		return fCanGetCurrentContendedMonitor;
 	}
 	
 	/**
 	 * @return Returns true if this implementation supports the retrieval of the monitor information for an object.
 	 */
 	public boolean canGetMonitorInfo() {
 		getCapabilities();
 		return fCanGetMonitorInfo;
 	}
 	
 	/**
 	 * @return Returns true if this implementation supports the retrieval of the monitors owned by a thread.
 	 */
 	public boolean canGetOwnedMonitorInfo() {
 		getCapabilities();
 		return fCanGetOwnedMonitorInfo;
 	}
 	
 	/**
 	 * @return Returns true if this implementation supports the query of the synthetic attribute of a method or field.
 
 	 */
 	public boolean canGetSyntheticAttribute() {
 		getCapabilities();
 		return fCanGetSyntheticAttribute;
 	}
 	
 	/**
 	 * @return Returns true if this implementation supports watchpoints for field access.
 	 */
 	public boolean canWatchFieldAccess() {
 		getCapabilities();
 		return fCanWatchFieldAccess;
 	}
 	
 	/**
 	 * @return Returns true if this implementation supports watchpoints for field modification.
 	 */
 	public boolean canWatchFieldModification() {
 		getCapabilities();
 		return fCanWatchFieldModification;
 	}
 	
 	/**
 	 * @return Returns the loaded reference types that match a given signature.
 	 */
 	public List classesBySignature(String signature) {
 		// Note that this information should not be cached.
 		initJdwpRequest();
 		try {
 			ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
 			DataOutputStream outData = new DataOutputStream(outBytes);
 			writeString(signature, "signature", outData); //$NON-NLS-1$
 
 			JdwpReplyPacket replyPacket = requestVM(JdwpCommandPacket.VM_CLASSES_BY_SIGNATURE, outBytes);
 			defaultReplyErrorHandler(replyPacket.errorCode());
 			DataInputStream replyData = replyPacket.dataInStream();
 			int nrOfElements = readInt("elements", replyData); //$NON-NLS-1$
 			List elements = new ArrayList(nrOfElements);
 			for (int i = 0; i < nrOfElements; i++) {
 				ReferenceTypeImpl elt = ReferenceTypeImpl.readWithTypeTag(this, replyData);
 				readInt("status", ReferenceTypeImpl.classStatusStrings(), replyData); //$NON-NLS-1$
 				if (elt == null) {
 					continue;
 				}
 				elements.add(elt);
 			}
 			return elements;
 		} catch (IOException e) {
 			defaultIOExceptionHandler(e);
 			return null;
 		} finally {
 			handledJdwpRequest();
 		}
 	}
 	
 	/**
 	 * @return Returns the loaded reference types that match a given name.
 	 */
 	public List classesByName(String name) {
 		String signature = TypeImpl.classNameToSignature(name);
 		return classesBySignature(signature);
 	}
 	
 	/**
 	 * Invalidates this virtual machine mirror.
 	 */
 	public void dispose() {
 		initJdwpRequest();
 		try {
 			requestVM(JdwpCommandPacket.VM_DISPOSE);
 			disconnectVM();
 		} catch (VMDisconnectedException e) {
 			// The VM can exit before we receive the reply.
 		} finally {
 			handledJdwpRequest();
 		}
 	}
 	
 	/**
 	 * @return Returns EventQueue that returns EventSets from the Virtual Manager.
 	 */
 	public EventQueue eventQueue() {
 		return fEventQueue;
 	}
 
 	/**
 	 * @return Returns EventRequestManager that creates all event objects on request.
 	 */
 	public EventRequestManager eventRequestManager() {
 		return fEventReqMgr;
 	}
 	
 	/**
 	 * @return Returns EventRequestManagerImpl that creates all event objects on request.
 	 */
 	public EventRequestManagerImpl eventRequestManagerImpl() {
 		return fEventReqMgr;
 	}
 
 	/**
 	 * Causes the mirrored VM to terminate with the given error code. 
 	 */
 	public void exit(int exitCode) {
 		initJdwpRequest();
 		try {
 			ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
 			DataOutputStream outData = new DataOutputStream(outBytes);
 			writeInt(exitCode, "exit code", outData); //$NON-NLS-1$
 			requestVM(JdwpCommandPacket.VM_EXIT, outBytes);
 			disconnectVM();
 		} catch (VMDisconnectedException e) {
 			// The VM can exit before we receive the reply.
 		} catch (IOException e) {
 			defaultIOExceptionHandler(e);
 		} finally {
 			handledJdwpRequest();
 		}
 	}
 
 	/**
 	 * @return Returns newly created ByteValue for the given value. 
 	 */
 	public ByteValue mirrorOf(byte value) {
 		return new ByteValueImpl(virtualMachineImpl(), new Byte(value));
 	}
 
 	/**
 	 * @return Returns newly created CharValue for the given value. 
 	 */
 	public CharValue mirrorOf(char value) {
 		return new CharValueImpl(virtualMachineImpl(), new Character(value));
 	}
 
 	/**
 	 * @return Returns newly created DoubleValue for the given value. 
 	 */
 	public DoubleValue mirrorOf(double value) {
 		return new DoubleValueImpl(virtualMachineImpl(), new Double(value));
 	}
 
 	/**
 	 * @return Returns newly created FloatValue for the given value. 
 	 */
 	public FloatValue mirrorOf(float value) {
 		return new FloatValueImpl(virtualMachineImpl(), new Float(value));
 	}
 
 	/**
 	 * @return Returns newly created IntegerValue for the given value. 
 	 */
 	public IntegerValue mirrorOf(int value) {
 		return new IntegerValueImpl(virtualMachineImpl(), new Integer(value));
 	}
 
 	/**
 	 * @return Returns newly created LongValue for the given value. 
 	 */
 	public LongValue mirrorOf(long value) {
 		return new LongValueImpl(virtualMachineImpl(), new Long(value));
 	}
 
 	/**
 	 * @return Returns newly created ShortValue for the given value. 
 	 */
 	public ShortValue mirrorOf(short value) {
 		return new ShortValueImpl(virtualMachineImpl(), new Short(value));
 	}
 
 	/**
 	 * @return Returns newly created BooleanValue for the given value. 
 	 */
 	public BooleanValue mirrorOf(boolean value) {
 		return new BooleanValueImpl(virtualMachineImpl(), new Boolean(value));
 	}
 		
 	/**
 	 * @return Returns newly created StringReference for the given value. 
 	 */
 	public StringReference mirrorOf(String value) {
 		initJdwpRequest();
 		try {
 			ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
 			DataOutputStream outData = new DataOutputStream(outBytes);
 			writeString(value, "string value", outData); //$NON-NLS-1$
 	
 			JdwpReplyPacket replyPacket = requestVM(JdwpCommandPacket.VM_CREATE_STRING, outBytes);
 			defaultReplyErrorHandler(replyPacket.errorCode());
 			
 			DataInputStream replyData = replyPacket.dataInStream();
 			StringReference result = StringReferenceImpl.read(this, replyData);
 			return result;
 		} catch (IOException e) {
 			defaultIOExceptionHandler(e);
 			return null;
 		} finally {
 			handledJdwpRequest();
 		}
 	}
 
 	/**
 	 * @return Returns the Process object for this virtual machine if launched by a LaunchingConnector.
 	 */
 	public Process process() {
 	 	return fLaunchedProcess;
 	}
 	 	
 	/**
 	 * Sets Process object for this virtual machine if launched by a LaunchingConnector.
 	 */
 	public void setLauncedProcess(Process proc) {
 		fLaunchedProcess = proc;
 	}
 	
 	/** 
 	 * Continues the execution of the application running in this virtual machine. 
 	 */
 	public void resume() {
 		initJdwpRequest();
 		try {
 			primeThreadsForResume();
 			JdwpReplyPacket replyPacket = requestVM(JdwpCommandPacket.VM_RESUME);
 			defaultReplyErrorHandler(replyPacket.errorCode());
 		} finally {
 			handledJdwpRequest();
 		}
 	}
 		
 	public void setDebugTraceMode(int traceFlags) {
 		// We don't have trace info.
 	}
 
 	/** 
 	 * Suspends all threads.
 	 */
 	public void suspend() {
 		initJdwpRequest();
 		try {
 			JdwpReplyPacket replyPacket = requestVM(JdwpCommandPacket.VM_SUSPEND);
 			defaultReplyErrorHandler(replyPacket.errorCode());
 		} finally {
 			handledJdwpRequest();
 		}
 	}
 		
 	public List topLevelThreadGroups() {
 		// Note that this information should not be cached.
 		initJdwpRequest();
 		try {
 			JdwpReplyPacket replyPacket = requestVM(JdwpCommandPacket.VM_TOP_LEVEL_THREAD_GROUPS);
 			defaultReplyErrorHandler(replyPacket.errorCode());
 			
 			DataInputStream replyData = replyPacket.dataInStream();
 			int nrGroups = readInt("nr of groups", replyData); //$NON-NLS-1$
 			ArrayList result = new ArrayList(nrGroups);
 			for (int i = 0; i < nrGroups; i++) {
 				ThreadGroupReferenceImpl threadGroup = ThreadGroupReferenceImpl.read(this, replyData);
 				result.add(threadGroup);
 			}
 			return result;
 		} catch (IOException e) {
 			defaultIOExceptionHandler(e);
 			return null;
 		} finally {
 			handledJdwpRequest();
 		}
 	}
 
 	/** 
 	 * @return Returns the name of the target VM as reported by the property java.vm.name.
 	 */
 	public String name() {
 		getVersionInfo();
 		return fVMName;
 	}
 		
 	/** 
 	 * @return Returns the version of the Java Runtime Environment in the target VM as reported by the property java.version.
 	 */
 	public String version() {
 		getVersionInfo();
 		return fVMVersion;
 	}
 			
 	/** 
 	 * @return Returns text information on the target VM and the debugger support that mirrors it. 
 	 */
 	public String description() {
 		getVersionInfo();
 		return fVersionDescription;
 	}
 			
 	/**
 	 * Prepares the VM's threads for VM resume.
 	 * 
	 * Reset event flags of all ThreadReferenceImpl for which there exist references.
	 * Note that only when no references exist they will not be in cache.
	 * We can therefore be sure that we will reset all ThreadReferenceImpl objects that
	 * the application holds.
 	 * 
 	 * Also, make sure the suspend count on each thread is not greater than 1. If it is,
 	 * resuming the VM will not actually resume the thread.
 	 */
	public void primeThreadsForResume() {
		Iterator iter = fCachedObjects.valuesWithType(ThreadReferenceImpl.class).iterator();
 		ThreadReferenceImpl thread;
 		while (iter.hasNext()) {
 			thread = (ThreadReferenceImpl)iter.next();
 			thread.resetEventFlags();
 			while (thread.suspendCount() > 1) {
 				thread.resume();
 			}
 		}
 	}
 
 	/**
 	 * Request and fetch ID sizes of Virtual Machine.
 	 */
 	private void getIDSizes() {
 		if (fGotIDSizes)
 			return;
 
 		/*
 		 * fGotIDSizes must first be assigned true to prevent an invinite loop
 		 * because getIDSizes() calls requestVM which calls packetSendManager.
 		 */
 		fGotIDSizes = true;
 		 
 		// We use a different mirror to avoid having verbose output mixed with the initiating command.
 		MirrorImpl mirror = new VoidValueImpl(this);
 		
 		mirror.initJdwpRequest();
 		try {
 			JdwpReplyPacket replyPacket = mirror.requestVM(JdwpCommandPacket.VM_ID_SIZES);
 			mirror.defaultReplyErrorHandler(replyPacket.errorCode());
 			DataInputStream replyData = replyPacket.dataInStream();
 		
 			fFieldIDSize = mirror.readInt("field ID size", replyData); //$NON-NLS-1$
 			fMethodIDSize = mirror.readInt("method ID size", replyData); //$NON-NLS-1$
 			fObjectIDSize = mirror.readInt("object ID size", replyData); //$NON-NLS-1$
 			fReferenceTypeIDSize = mirror.readInt("refType ID size", replyData); //$NON-NLS-1$
 			fFrameIDSize = mirror.readInt("frame ID size", replyData); //$NON-NLS-1$
 		} catch (IOException e) {
 			fGotIDSizes = false;
 			mirror.defaultIOExceptionHandler(e);
 		} finally {
 			mirror.handledJdwpRequest();
 		}
 	}
 	
 	/**
 	 * Retrieves version info of the VM.
 	 */
 	public void getVersionInfo() {
 		if (fVersionDescription != null)
 			return;
 			
 		initJdwpRequest();
 		try {
 			JdwpReplyPacket replyPacket = requestVM(JdwpCommandPacket.VM_VERSION);
 			defaultReplyErrorHandler(replyPacket.errorCode());
 			DataInputStream replyData = replyPacket.dataInStream();
 		
 			fVersionDescription = readString("version descr.", replyData); //$NON-NLS-1$
 			fJdwpMajorVersion = readInt("major version", replyData); //$NON-NLS-1$
 			fJdwpMinorVersion = readInt("minor version", replyData); //$NON-NLS-1$
 			fVMVersion = readString("version", replyData); //$NON-NLS-1$
 			fVMName = readString("name", replyData); //$NON-NLS-1$
 		} catch (IOException e) {
 			fVersionDescription = null;
 			defaultIOExceptionHandler(e);
 		} finally {
 			handledJdwpRequest();
 		}
 	}
 		
 	/**
 	 * Retrieves the HCR capabilities of the VM.
 	 */
 	public void getHCRCapabilities() {
 		checkHCRSupported();
 		if (fHcrCapabilities != null)
 			return;
 		fHcrCapabilities = new boolean[HCR_CAN_REENTER_ON_EXIT + 1];
 		
 		initJdwpRequest();
 		try {
 			JdwpReplyPacket replyPacket = requestVM(JdwpCommandPacket.HCR_CAPABILITIES);
 			defaultReplyErrorHandler(replyPacket.errorCode());
 			DataInputStream replyData = replyPacket.dataInStream();
 		
 			fHcrCapabilities[HCR_CAN_RELOAD_CLASSES] = readBoolean("reload classes", replyData); //$NON-NLS-1$
 			fHcrCapabilities[HCR_CAN_GET_CLASS_VERSION] = readBoolean("get class version", replyData); //$NON-NLS-1$
 			fHcrCapabilities[HCR_CAN_DO_RETURN] = readBoolean("do return", replyData); //$NON-NLS-1$
 			fHcrCapabilities[HCR_CAN_REENTER_ON_EXIT] = readBoolean("reenter on exit", replyData); //$NON-NLS-1$
 		} catch (IOException e) {
 			fHcrCapabilities = null;
 			defaultIOExceptionHandler(e);
 		} finally {
 			handledJdwpRequest();
 		}
 	}
 	
 	/**
 	 * @return Returns Whether VM can deal with the 'Classes have Changed' command.
 	 */
 	public boolean canReloadClasses() {
 		getHCRCapabilities();
 		return fHcrCapabilities[HCR_CAN_RELOAD_CLASSES];
 	}
 	
 	/**
 	 * @return Returns Whether VM can get the version of a given class file.
 	 */
 	public boolean canGetClassFileVersion() {
 		getHCRCapabilities();
 		return fHcrCapabilities[HCR_CAN_GET_CLASS_VERSION];
 	}
 	
 	/**
 	 * @return Returns Whether VM can do a return in the middle of executing a method.
 	 */
 	public boolean canDoReturn() {
 		getHCRCapabilities();
 		return fHcrCapabilities[HCR_CAN_DO_RETURN];
 	}
 	
 	/**
 	 * @return Returns Whether VM can reenter a method on exit.
 	 */
 	public boolean canReenterOnExit() {
 		getHCRCapabilities();
 		return fHcrCapabilities[HCR_CAN_REENTER_ON_EXIT];
 	}
 	
 	/**
 	 * Notify the VM that classes have changed due to Hot Code Replacement.
 	 * @return Returns RELOAD_SUCCESS, RELOAD_FAILURE or RELOAD_IGNORED.
 	 */
 	public int classesHaveChanged(String[] names) {
 		checkHCRSupported();
 		// We convert the class/interface names to signatures.
 		String[] signatures = new String[names.length];
 		
 		initJdwpRequest();
 		try {
 			ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
 			DataOutputStream outData = new DataOutputStream(outBytes);
 			writeInt(names.length, "length", outData); //$NON-NLS-1$
 			for (int i = 0; i < names.length; i++) {
 				signatures[i] = TypeImpl.classNameToSignature(names[i]);
 				writeString(signatures[i], "signature", outData); //$NON-NLS-1$
 			}
 		
 			JdwpReplyPacket replyPacket = requestVM(JdwpCommandPacket.HCR_CLASSES_HAVE_CHANGED, outBytes);
 			defaultReplyErrorHandler(replyPacket.errorCode());
 			DataInputStream replyData = replyPacket.dataInStream();
 		
 			byte resultFlag = readByte("result", resultHCRMap(), replyData); //$NON-NLS-1$
 			switch (resultFlag) {
 				case HCR_RELOAD_SUCCESS:
 					return RELOAD_SUCCESS;
 				case HCR_RELOAD_FAILURE:
 					return RELOAD_FAILURE;
 				case HCR_RELOAD_IGNORED:
 					return RELOAD_IGNORED;
 			}
 			throw new InternalError(JDIMessages.getString("VirtualMachineImpl.Invalid_result_flag_in_Classes_Have_Changed_response___3") + resultFlag + JDIMessages.getString("VirtualMachineImpl._4")); //$NON-NLS-1$ //$NON-NLS-2$
 		} catch (IOException e) {
 			defaultIOExceptionHandler(e);
 			return 0;
 		} finally {
 			handledJdwpRequest();
 		}
 	}
 
 	/**
 	 * @return Returns description of Mirror object.
 	 */
 	public String toString() {
 		try {
 			return name();
 		} catch (Exception e) {
 			return fDescription;
 		}
 	}
 
 	/**
 	 * Retrieves constant mappings.
 	 */
 	public static void getConstantMaps() {
 		if (fgHCRResultMap != null) {
 			return;
 		}
 		
 		Field[] fields = VirtualMachineImpl.class.getDeclaredFields();
 		fgHCRResultMap = new HashMap();
 		for (int i = 0; i < fields.length; i++) {
 			Field field = fields[i];
 			if ((field.getModifiers() & Modifier.PUBLIC) == 0 || (field.getModifiers() & Modifier.STATIC) == 0 || (field.getModifiers() & Modifier.FINAL) == 0) {
 				continue;
 			}
 				
 			try {
 				String name = field.getName();
 				if (name.startsWith("HCR_RELOAD_")) { //$NON-NLS-1$
 					Integer intValue = new Integer(field.getInt(null));
 					name = name.substring(4);
 					fgHCRResultMap.put(intValue, name);
 				}
 			} catch (IllegalAccessException e) {
 				// Will not occur for own class.
 			} catch (IllegalArgumentException e) {
 				// Should not occur.
 				// We should take care that all public static final constants
 				// in this class are numbers that are convertible to int.
 			}
 		}
 	}
 	
 	/**
 	 * @return Returns a map with string representations of tags.
 	 */
 	 public static Map resultHCRMap() {
 	 	getConstantMaps();
 	 	return fgHCRResultMap;
 	 }
 	 
 	/**
 	 * Sets request timeout in ms.
 	 */
 	public void setRequestTimeout(int timeout) {
 		fRequestTimeout = timeout;
 	}
 	
 	/**
 	 * @return Returns request timeout in ms.
 	 */
 	public int getRequestTimeout() {
 		return fRequestTimeout;
 	}
 	
 	/**
 	 * Returns whether the JDWP version is greater
 	 * than or equal to the specified major/minor
 	 * version numbers.
 	 * 
 	 * @return whether the JDWP version is greater
 	 * than or equal to the specified major/minor
 	 * version numbers
 	 */
 	public boolean isJdwpVersionGreaterOrEqual(int major, int minor) {
 		getVersionInfo();
 		return (fJdwpMajorVersion > major) ||
 			(fJdwpMajorVersion == major && fJdwpMinorVersion >= minor);
 	}
 	
 	/*
 	 * @see VirtualMachine#redefineClasses(Map)
 	 */
 	public void redefineClasses(Map typesToBytes) {
 		if (!canRedefineClasses()) {
 			throw new UnsupportedOperationException();
 		}
 		
 		initJdwpRequest();
 		try {
 			ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
 			DataOutputStream outData = new DataOutputStream(outBytes);
 			writeInt(typesToBytes.size(), "classes", outData); //$NON-NLS-1$
 			
 			Set types = typesToBytes.keySet();
 			Iterator iter = types.iterator();
 			while (iter.hasNext()) {
 				ReferenceTypeImpl type = (ReferenceTypeImpl) iter.next();
 				type.write(this, outData);
 				byte[] bytes = (byte[]) typesToBytes.get(type);
 				writeInt(bytes.length, "classfile", outData); //$NON-NLS-1$
 				for (int i=0; i < bytes.length; i++) {
 					writeByte(bytes[i], "classByte", outData); //$NON-NLS-1$
 				}
 				fCachedReftypes.remove(type.getRefTypeID()); // flush local cache of redefined types
 			}
 			
 			JdwpReplyPacket reply = requestVM(JdwpCommandPacket.VM_REDEFINE_CLASSES, outBytes);
 			switch (reply.errorCode()) {
 				case JdwpReplyPacket.UNSUPPORTED_VERSION:
 					throw new UnsupportedClassVersionError();
 				case JdwpReplyPacket.INVALID_CLASS_FORMAT:
 					throw new ClassFormatError();
 				case JdwpReplyPacket.CIRCULAR_CLASS_DEFINITION:
 					throw new ClassCircularityError();
 				case JdwpReplyPacket.FAILS_VERIFICATION:
 					throw new VerifyError();
 				case JdwpReplyPacket.NAMES_DONT_MATCH:
 					throw new NoClassDefFoundError();
 				case JdwpReplyPacket.ADD_METHOD_NOT_IMPLEMENTED:
 					throw new UnsupportedOperationException(JDIMessages.getString("VirtualMachineImpl.Add_method_not_implemented_1")); //$NON-NLS-1$
 				case JdwpReplyPacket.SCHEMA_CHANGE_NOT_IMPLEMENTED:
 					throw new UnsupportedOperationException(JDIMessages.getString("VirtualMachineImpl.Scheme_change_not_implemented_2")); //$NON-NLS-1$
 				case JdwpReplyPacket.HIERARCHY_CHANGE_NOT_IMPLEMENTED:
 					throw new UnsupportedOperationException(JDIMessages.getString("VirtualMachineImpl.Hierarchy_change_not_implemented_3")); //$NON-NLS-1$
 				case JdwpReplyPacket.DELETE_METHOD_NOT_IMPLEMENTED:
 					throw new UnsupportedOperationException(JDIMessages.getString("VirtualMachineImpl.Delete_method_not_implemented_4")); //$NON-NLS-1$
 				case JdwpReplyPacket.CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED:
 					throw new UnsupportedOperationException(JDIMessages.getString("VirtualMachineImpl.Class_modifiers_change_not_implemented_5")); //$NON-NLS-1$
 				case JdwpReplyPacket.METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED:
 					throw new UnsupportedOperationException(JDIMessages.getString("VirtualMachineImpl.Method_modifiers_change_not_implemented_6")); //$NON-NLS-1$
 				default:
 					defaultReplyErrorHandler(reply.errorCode());
 			}
 		} catch (IOException ioe) {
 			defaultIOExceptionHandler(ioe);
 			return;
 		} finally {
 			handledJdwpRequest();
 		}
 	}
 
 	/*
 	 * @see VirtualMachine#canRedefineClasses()
 	 */
 	public boolean canRedefineClasses() {
 		getCapabilities();
 		return fCanRedefineClasses;
 	}
 
 	/*
 	 * @see VirtualMachine#canUseInstanceFilters()
 	 */
 	public boolean canUseInstanceFilters() {
 		getCapabilities();
 		return fCanUseInstanceFilters;
 	}
 
 	/*
 	 * @see VirtualMachine#canAddMethod()
 	 */
 	public boolean canAddMethod() {
 		getCapabilities();
 		return fCanAddMethod;
 	}
 
 	/*
 	 * @see VirtualMachine#canUnrestrictedlyRedefineClasses()
 	 */
 	public boolean canUnrestrictedlyRedefineClasses() {
 		getCapabilities();
 		return fCanUnrestrictedlyRedefineClasses;
 	}
 
 	/*
 	 * @see VirtualMachine#canPopFrames()
 	 */
 	public boolean canPopFrames() {
 		getCapabilities();
 		return fCanPopFrames;
 	}
 
 	/*
 	 * @see VirtualMachine#canGetSourceDebugExtension()
 	 */
 	public boolean canGetSourceDebugExtension() {
 		getCapabilities();
 		return fCanGetSourceDebugExtension;
 	}
 
 	/*
 	 * @see VirtualMachine#canRequestVMDeathEvent()
 	 */
 	public boolean canRequestVMDeathEvent() {
 		getCapabilities();
 		return fCanRequestVMDeathEvent;
 	}
 
 	/*
 	 * @see VirtualMachine#setDefaultStratum(String)
 	 */
 	public void setDefaultStratum(String stratum) {
 		if (!canSetDefaultStratum()) {
 			throw new UnsupportedOperationException();
 		}
 	}
 
 	/*
 	 * @see VirtualMachine#getDefaultStratum()
 	 */
 	public String getDefaultStratum() {
 		return null;
 	}
 	
 	/*
 	 * Returns whether the target VM supports setting
 	 * the default stratum.
 	 */
 	private boolean canSetDefaultStratum() {
 		getCapabilities();
 		return fCanSetDefaultStratum;
 	}
 
 }
