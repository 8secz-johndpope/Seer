 /*
  * <copyright>
  * Copyright  2006 by Carnegie Mellon University, all rights reserved.
  *
  * Use of the Open Source AADL Tool Environment (OSATE) is subject to the terms of the license set forth
  * at http://www.eclipse.org/legal/cpl-v10.html.
  *
  * NO WARRANTY
  *
  * ANY INFORMATION, MATERIALS, SERVICES, INTELLECTUAL PROPERTY OR OTHER PROPERTY OR RIGHTS GRANTED OR PROVIDED BY
  * CARNEGIE MELLON UNIVERSITY PURSUANT TO THIS LICENSE (HEREINAFTER THE "DELIVERABLES") ARE ON AN "AS-IS" BASIS.
  * CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED AS TO ANY MATTER INCLUDING,
  * BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABILITY, INFORMATIONAL CONTENT,
  * NONINFRINGEMENT, OR ERROR-FREE OPERATION. CARNEGIE MELLON UNIVERSITY SHALL NOT BE LIABLE FOR INDIRECT, SPECIAL OR
  * CONSEQUENTIAL DAMAGES, SUCH AS LOSS OF PROFITS OR INABILITY TO USE SAID INTELLECTUAL PROPERTY, UNDER THIS LICENSE,
  * REGARDLESS OF WHETHER SUCH PARTY WAS AWARE OF THE POSSIBILITY OF SUCH DAMAGES. LICENSEE AGREES THAT IT WILL NOT
  * MAKE ANY WARRANTY ON BEHALF OF CARNEGIE MELLON UNIVERSITY, EXPRESS OR IMPLIED, TO ANY PERSON CONCERNING THE
  * APPLICATION OF OR THE RESULTS TO BE OBTAINED WITH THE DELIVERABLES UNDER THIS LICENSE.
  *
  * Licensee hereby agrees to defend, indemnify, and hold harmless Carnegie Mellon University, its trustees, officers,
  * employees, and agents from all claims or demands made against them (and any related losses, expenses, or
  * attorney's fees) arising out of, or relating to Licensee's and/or its sub licensees' negligent use or willful
  * misuse of or negligent conduct or willful misconduct regarding the Software, facilities, or other rights or
  * assistance granted by Carnegie Mellon University under this License, including, but not limited to, any claims of
  * product liability, personal injury, death, damage to property, or violation of any laws or regulations.
  *
  * Carnegie Mellon University Software Engineering Institute authored documents are sponsored by the U.S. Department
  * of Defense under Contract F19628-00-C-0003. Carnegie Mellon University retains copyrights in all material produced
  * under this contract. The U.S. Government retains a non-exclusive, royalty-free license to publish or reproduce these
  * documents, or allow others to do so, for U.S. Government purposes only pursuant to the copyright license
  * under the contract clause at 252.227.7013.
  * </copyright>
  */
 package org.osate.analysis.resource.budgets.logic;
 
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
 
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.emf.common.util.BasicEList;
 import org.eclipse.emf.common.util.EList;
 import org.osate.aadl2.ComponentCategory;
 import org.osate.aadl2.DirectionType;
 import org.osate.aadl2.Element;
 import org.osate.aadl2.NamedElement;
 import org.osate.aadl2.Property;
 import org.osate.aadl2.UnitLiteral;
 import org.osate.aadl2.instance.ComponentInstance;
 import org.osate.aadl2.instance.ConnectionInstance;
 import org.osate.aadl2.instance.ConnectionInstanceEnd;
 import org.osate.aadl2.instance.ConnectionKind;
 import org.osate.aadl2.instance.FeatureInstance;
 import org.osate.aadl2.instance.InstanceObject;
 import org.osate.aadl2.instance.InstanceReferenceValue;
 import org.osate.aadl2.instance.SystemInstance;
 import org.osate.aadl2.instance.SystemOperationMode;
 import org.osate.aadl2.modelsupport.errorreporting.AnalysisErrorReporterManager;
 import org.osate.aadl2.modelsupport.modeltraversal.ForAllElement;
 import org.osate.aadl2.modelsupport.modeltraversal.SOMIterator;
 import org.osate.aadl2.modelsupport.util.ConnectionGroupIterator;
 import org.osate.aadl2.properties.InvalidModelException;
 import org.osate.aadl2.properties.PropertyDoesNotApplyToHolderException;
 import org.osate.aadl2.util.Aadl2Util;
 import org.osate.analysis.architecture.InstanceValidation;
 import org.osate.ui.dialogs.Dialog;
 import org.osate.xtext.aadl2.properties.util.GetProperties;
import org.osate.xtext.aadl2.properties.util.InstanceModelUtil;
 import org.osate.xtext.aadl2.properties.util.PropertyUtils;
 
 //TODO-LW: assumes connection ends are features
 public class DoBoundResourceAnalysisLogic {
 	protected final String actionName;
 	/**
 	 * The string buffer that is used to record error messages.
 	 */
 	private final StringBuffer reportMessage;
 	private final AnalysisErrorReporterManager errManager;
 
 	/**
 	 * Secondary error reporter used to report to a string buffer.
 	 */
 	private final AnalysisErrorReporterManager loggingErrManager;
 
 	public DoBoundResourceAnalysisLogic(final String actionName, final StringBuffer reportMessage,
 			final AnalysisErrorReporterManager errManager, final AnalysisErrorReporterManager loggingErrManager) {
 		this.actionName = actionName;
 		this.reportMessage = reportMessage;
 		this.errManager = errManager;
 		this.loggingErrManager = loggingErrManager;
 	}
 
 	public void analysisBody(final IProgressMonitor monitor, final Element obj) {
 		if (obj instanceof InstanceObject) {
 			SystemInstance root = ((InstanceObject) obj).getSystemInstance();
 			InstanceValidation iv = new InstanceValidation(errManager);
 			if (!iv.checkReferenceProcessor(root)){
 				Dialog.showError("Bound Resource Budget Analysis","Model contains thread execution times without reference processor.");
 				return;
 			}
 			monitor.beginTask(actionName, IProgressMonitor.UNKNOWN);
 			final SOMIterator soms = new SOMIterator(root);
 			while (soms.hasNext()) {
 				final SystemOperationMode som = soms.nextSOM();
 				final String somName = Aadl2Util.getPrintableSOMName(som);
 				checkProcessorLoads(root, somName);
 //				checkVirtualProcessorLoads(root, somName);
 				checkMemoryLoads(root, somName);
 			}
 			monitor.done();
 
 			if (root.getSystemOperationModes().size() == 1) {
 				//Also report the results using a message dialog
 				Dialog.showInfo("Resource Budget Statistics", getResultsMessages());
 			}
 		} else
 			Dialog.showError("Bound Resource Analysis Error", "Can only check system instances");
 	}
 
 	protected void checkProcessorLoads(SystemInstance si, final String somName) {
 		ForAllElement mal = new ForAllElement() {
 			@Override
 			protected void process(Element obj) {
 				checkProcessorLoad((ComponentInstance) obj, somName);
 			}
 		};
 		mal.processPreOrderComponentInstance(si, ComponentCategory.PROCESSOR);
 	}
 
 	protected void checkVirtualProcessorLoads(SystemInstance si, final String somName) {
 		ForAllElement mal = new ForAllElement() {
 			@Override
 			protected void process(Element obj) {
 				checkProcessorLoad((ComponentInstance) obj, somName);
 			}
 		};
 		mal.processPreOrderComponentInstance(si, ComponentCategory.VIRTUAL_PROCESSOR);
 	}
 	
 	/**
 	 * get all components bound to the given component
 	 * @param procorVP
 	 * @return
 	 */
 	protected EList getBoundComponents(final ComponentInstance procorVP){
 		SystemInstance root = procorVP.getSystemInstance();
 		EList boundComponents = new ForAllElement() {
 			@Override
 			protected boolean suchThat(Element obj) {
				return InstanceModelUtil.isBoundToProcessor((ComponentInstance) obj, procorVP);
 			}
		}.processPreOrderComponentInstance(root,ComponentCategory.THREAD);
 		return boundComponents;
 	}
 
 
 	/**
 	 * check the load from components bound to the given processor The
 	 * components can be threads or higher level components.
 	 * 
 	 * @param curProcessor Component Instance of processor
 	 */
 	protected void checkProcessorLoad(ComponentInstance curProcessor, String somName) {
 		UnitLiteral mipsliteral = GetProperties.getMIPSUnitLiteral(curProcessor);
 		double MIPScapacity = GetProperties.getMIPSCapacityInMIPS(curProcessor,0.0);
 		if (MIPScapacity == 0 && curProcessor.getCategory().equals(ComponentCategory.VIRTUAL_PROCESSOR)){
 			MIPScapacity = GetProperties.getMIPSBudgetInMIPS(curProcessor);
 		}
 		EList boundComponents = getBoundComponents(curProcessor);
 		if (boundComponents.size() == 0) {
 			errorSummary(curProcessor, somName, "No application components bound to "
 					+ curProcessor.getComponentInstancePath());
 			return;
 		}
 		double totalMIPS = 0.0;
 		Set covered = new HashSet();
 		for (Iterator it = boundComponents.iterator(); it.hasNext();) {
 			ComponentInstance bci = (ComponentInstance) it.next();
 			if (covered.contains(bci)) {
 				// one of the children has a budget, which was accounted for
 				break;
 			}
 			double actualmips = GetProperties.getThreadExecutioninMIPS(bci);
 			double budget = GetProperties.getMIPSBudgetInMIPS(bci, 0.0);
 			if (actualmips > 0) {
 				if (budget > 0 && actualmips > budget) {
 					warningSummary(bci, somName, "Execution time (in MIPS) " + GetProperties.toStringScaled(actualmips, mipsliteral) + " exceeds MIPS budget "
 							+ GetProperties.toStringScaled(budget, mipsliteral) + " for "+bci.getComponentInstancePath());
 				} else if (budget > 0 && actualmips < budget) {
 					infoSummary(bci, somName, "Execution time (in MIPS) " + GetProperties.toStringScaled(actualmips, mipsliteral) + " is less than MIPS budget "
 							+ GetProperties.toStringScaled(budget, mipsliteral) + " for "+bci.getComponentInstancePath());
 				}
 				// add ancestors to coverage list
 				while ((bci = bci.getContainingComponentInstance()) != null) {
 					covered.add(bci);
 				}
 				totalMIPS += actualmips;
 			} else {
 				if (budget == 0) {
 					warningSummary(bci, somName, "Bound component " + bci.getComponentInstancePath()
 							+ " has no MIPS budget or execution time");
 				} else {
 					totalMIPS += budget;
 					// add ancestors to coverage list
 					while ((bci = bci.getContainingComponentInstance()) != null) {
 						covered.add(bci);
 					}
 				}
 			}
 		}
 		if (totalMIPS > MIPScapacity) {
 			errorSummary(curProcessor, somName, "Total MIPS " + GetProperties.toStringScaled(totalMIPS, mipsliteral) + " of bound tasks exceeds MIPS capacity "
 					+ GetProperties.toStringScaled(MIPScapacity, mipsliteral) + " of " + curProcessor.getComponentInstancePath());
 		} else if (totalMIPS == 0.0) {
 			warningSummary(curProcessor, somName, "Bound app's have no MIPS budget.");
 		} else {
 			infoSummary(curProcessor, somName, "Total MIPS " + GetProperties.toStringScaled(totalMIPS, mipsliteral) + " of bound tasks within "
 					+  "MIPS capacity " + GetProperties.toStringScaled(MIPScapacity, mipsliteral) + " of "
 					+ curProcessor.getComponentInstancePath());
 		}
 	}
 
 	protected void checkMemoryLoads(SystemInstance si, final String somName) {
 		ForAllElement mal = new ForAllElement() {
 			@Override
 			protected void process(Element obj) {
 				checkMemoryLoad((ComponentInstance) obj, somName);
 			}
 		};
 		mal.processPreOrderComponentInstance(si, ComponentCategory.MEMORY);
 	}
 
 	/**
 	 * check the load from components bound to the given memory The components
 	 * can be threads or higher level components.
 	 * 
 	 * @param curMemory Component Instance of memory
 	 */
 	protected void checkMemoryLoad(ComponentInstance curMemory, String somName) {
 		SystemInstance root = curMemory.getSystemInstance();
 		final ComponentInstance currentMemory = curMemory;
 		EList boundComponents = new ForAllElement() {
 			@Override
 			protected boolean suchThat(Element obj) {
 				List<ComponentInstance> boundMemoryList = GetProperties.getActualMemoryBinding((ComponentInstance)obj);
 				if (boundMemoryList.isEmpty())
 					return false;
 				return boundMemoryList.get(0) == currentMemory;
 			}
 			// process bottom up so we can check whether children had budgets
 		}.processPostOrderComponentInstance(root);
 		if (GetProperties.getROMCapacityInKB(curMemory,  0.0) > 0.0) {
 			doMemoryLoad(curMemory, somName, boundComponents, true); //ROM
 		}
 		if (GetProperties.getRAMCapacityInKB(curMemory,  0.0) > 0.0) {
 			doMemoryLoad(curMemory, somName, boundComponents, false); //RAM
 		}
 	}
 
 	/**
 	 * check the load from components bound to the given memory The components
 	 * can be threads or higher level components.
 	 * 
 	 * @param curMemory Component Instance of memory
 	 */
 	protected void doMemoryLoad(ComponentInstance curMemory, String somName, EList boundComponents, boolean isROM) {
 		double totalMemory = 0.0;
 		String resourceName = isROM ? "ROM" : "RAM";
 		Set budgeted = new HashSet();
 		for (Iterator it = boundComponents.iterator(); it.hasNext();) {
 			ComponentInstance bci = (ComponentInstance) it.next();
 			try {
 				double totalactual = sumActuals(bci,isROM);
 				double budget = isROM ? GetProperties.getROMBudgetInKB(bci, 0.0):
 					GetProperties.getRAMBudgetInKB(bci, 0.0);
 				if (totalactual > 0) {
 					// only compare if there were actuals
 					if (totalactual > budget) {
 						errorSummary(bci, somName, "Component " + bci.getComponentInstancePath() + " " + resourceName
 								+ " total exceeds budget by " + (totalactual - budget) + " KB");
 					} else if (totalactual < budget) {
 						warningSummary(bci, somName, "Component " + bci.getComponentInstancePath()
 								+ " has unallocated " + resourceName + " budget " + (budget - totalactual) + " KB");
 					}
 				}
 				if (totalactual == 0.0) {
 					// we use a budget number as there are no actuals
 					if (budget > 0 && !budgeted.contains(bci)) {
 						// only add it if no children budget numbers have been added
 						totalMemory += budget;
 						// add ancestors to budgeted list so their budget does not get added later
 						while ((bci = bci.getContainingComponentInstance()) != null) {
 							budgeted.add(bci);
 						}
 					}
 				} else {
 					// add only the current actual; the children actual have been added before
 					totalMemory += isROM ? GetProperties.getROMActualInKB(bci, 0.0):
 						GetProperties.getRAMActualInKB(bci, 0.0);;
 				}
 			} catch (Throwable e) {
 			}
 		}
 		try {
 			double Memorycapacity = isROM ? GetProperties.getROMCapacityInKB(curMemory, 0.0):
 				GetProperties.getRAMCapacityInKB(curMemory, 0.0);
 			if (totalMemory > Memorycapacity) {
 				errorSummary(curMemory, somName, "Total Memory " + totalMemory + " KB of bounds tasks exceeds Memory capacity "
 						+ Memorycapacity + " KB of "
 						+ curMemory.getComponentInstancePath());
 			} else if (totalMemory == 0.0 && Memorycapacity == 0.0) {
 				warningSummary(curMemory, somName, "" + (isROM ? "ROM" : "RAM") + " memory "
 						+ curMemory.getComponentInstancePath() + " has no capacity. Bound app's have no memory budget.");
 			} else {
 				infoSummary(curMemory, somName, "Total " + (isROM ? "ROM" : "RAM") + " memory " + totalMemory + " KB of bound tasks within Memory capacity "
 						+ Memorycapacity + " KB of "
 						+ curMemory.getComponentInstancePath());
 			}
 		} catch (InvalidModelException e) {
 			errorSummary(curMemory, somName, "" + (isROM ? "ROM" : "RAM") + " memory "
 					+ curMemory.getComponentInstancePath() + " has no memory capacity specified");
 		}
 	}
 
 	protected void checkBusLoads(SystemInstance si, final boolean doBindings, final boolean loopback,
 			final String somName) {
 		ForAllElement mal = new ForAllElement() {
 			@Override
 			protected void process(Element obj) {
 				checkBandWidthLoad((ComponentInstance) obj, doBindings, loopback, somName);
 			}
 		};
 		mal.processPreOrderComponentInstance(si, ComponentCategory.BUS);
 	}
 
 	/**
 	 * check the load from connections bound to the given bus
 	 * 
 	 * @param curBus Component Instance of bus
 	 * @param doBindings if true do bindings to all buses, if false do them only
 	 *            for EtherSwitch
 	 * @param loopback if true include communication to own processor in network
 	 *            workload
 	 * @param somName String somName to be used in messages
 	 */
 	protected void checkBandWidthLoad(final ComponentInstance curBus, boolean doBindings, boolean loopback,
 			String somName) {
 		double Buscapacity = GetProperties.getBandWidthCapacityInKbps(curBus, 0.0);
 		if (Buscapacity == 0)
 			return;
 		SystemInstance root = curBus.getSystemInstance();
 		double totalBandWidth = 0.0;
 		String binding = doBindings ? "bound" : "all";
 		EList<ConnectionInstance> connections = root.getAllConnectionInstances();
 		EList budgetedConnections = new BasicEList();
 		// filters out to use only Port connections or feature group connections
 		ConnectionGroupIterator cgi = new ConnectionGroupIterator(connections);
 		while (cgi.hasNext()) {
 			Object obj = cgi.next();
 			if (obj != null)
 				budgetedConnections.add(obj);
 		}
 		for (Iterator it = budgetedConnections.iterator(); it.hasNext();) {
 			Object obj = it.next();
 			if (obj instanceof ConnectionInstance) {
 				ConnectionInstance pci = (ConnectionInstance) obj;
 
 //				if (pci.getKind() == ConnectionKind.PORT_CONNECTION||pci.getKind() == ConnectionKind.FEATURE_GROUP_CONNECTION) {
 					double budget = 0.0;
 					double actual = 0.0;
 					if (doBindings) {
 						List connectionBindings = GetProperties.getActualConnectionBinding(pci);
 						if (connectionBindings != null && !connectionBindings.isEmpty()) {
 							for (Iterator i = connectionBindings.iterator(); i.hasNext();) {
 								final InstanceReferenceValue bindingVal = (InstanceReferenceValue) i.next();
 								final ComponentInstance boundObject = (ComponentInstance) bindingVal
 										.getReferencedInstanceObject();
 								if (boundObject == curBus) {
 									budget = GetProperties.getBandWidthBudgetInKbps(pci, 0.0);
 									actual = calcBandwidth(pci.getSource());
 									if (budget == 0 && actual == 0) {
 										errManager.warning(pci, "Connection " + pci.getName()
 												+ " has no bandwidth budget or port output rates");
 									}
 									if (budget > 0) {
 										if (actual > 0) {
 											totalBandWidth += actual;
 											if (actual > budget) {
 												errManager.warning(pci, "Connection " + pci.getName()
 														+ " has port-based bandwidth exceeds bandwidth budget");
 											}
 										} else {
 											totalBandWidth += budget;
 										}
 									}
 								}
 							}
 						} else {
 							// if null
 							if (connectedByBus(pci, curBus) || (hasSwitchLoopback(pci, curBus) && loopback)) {
 								budget = GetProperties.getBandWidthBudgetInKbps(pci, 0.0);
 								if (budget == 0) {
 									errManager.warning(pci, "Connection " + pci.getName() + " has no bandwidth budget");
 								} else {
 									totalBandWidth += budget;
 								}
 							}
 						}
 					} else {
 						// no binding; just do totals
 						budget = GetProperties.getBandWidthBudgetInKbps(pci, 0.0);
 						if (budget == 0 && !pci.getKind().equals(ConnectionKind.ACCESS_CONNECTION)) {
 							errManager.warning(pci, "Connection " + pci.getName() + " has no bandwidth budget");
 						}
 						if (budget > 0) {
 							totalBandWidth += budget;
 						}
 					}
 //				}
 			}
 		}
 		if (totalBandWidth > Buscapacity) {
 			errorSummary(curBus, somName, "Total Bus bandwidth budget " + totalBandWidth + " Kbps of " + binding + " tasks"
 					+ (loopback ? " with loopback" : "") + " exceeds bandwidth capacity " + Buscapacity + " KBytesps of " + curBus.getComponentInstancePath());
 		} else if (totalBandWidth > 0.0 && Buscapacity > 0.0) {
 			infoSummary(curBus, somName, "Total Bus bandwidth budget " + totalBandWidth + " Kbps of " + binding + " tasks"
 					+ (loopback ? " with loopback" : "") + " within bandwidth capacity " + Buscapacity + " KBytesps of " + curBus.getComponentInstancePath());
 		}
 	}
 	
 	public static double getDataRate(final NamedElement ne, final double defaultValue) {
 		// TODO add in new Input_Rate or OutPut_Rate property from V2
 		try {
 			Property dataRate = GetProperties.lookupPropertyDefinition(ne,"SEI", "Data_Rate");
 			return PropertyUtils.getRealValue(ne, dataRate, defaultValue);
 		} catch (Throwable e) {
 			return defaultValue;
 		}
 	}
 
 
 	/**
 	 * Calculate bandwidth demand from rate & data size
 	 * 
 	 * @param pci Port connection instance
 	 * @return
 	 */
 
 	protected double calcBandwidth(ConnectionInstanceEnd cie) {
 		double res = 0;
 
 		// TODO-LW add other cases
 		if (cie instanceof FeatureInstance) {
 			FeatureInstance fi = (FeatureInstance) cie;
 			double datasize = GetProperties.getSourceDataSizeInBytes(fi);
 			double srcRate = getDataRate(fi,  0.0);
 			if (srcRate == 0) {
 				srcRate = 1 / GetProperties.getPeriodInSeconds(fi.getContainingComponentInstance(), 1);
 			}
 			res = datasize * srcRate;
 			EList fil = fi.getFeatureInstances();
 			if (fil.size() > 0) {
 				double subres = 0;
 				for (Iterator it = fil.iterator(); it.hasNext();) {
 					FeatureInstance sfi = (FeatureInstance) it.next();
 					subres = subres + calcBandwidth(sfi);
 				}
 				if (subres > res) {
 					if (res > 0) {
 						warningSummary(fi, null, "Bandwidth of feature group ports " + subres
 								+ " exceeds feature group bandwidth " + res);
 					}
 					res = subres;
 				}
 			}
 		}
 		return res;
 	}
 
 	/*
 	 * are processors of connection endpoints the same and are connected to the
 	 * current bus
 	 */
 	protected boolean hasSwitchLoopback(ConnectionInstance pci, ComponentInstance curBus) {
 		ComponentInstance srcHW = getHardwareComponent(pci.getSource());
 		ComponentInstance dstHW = getHardwareComponent(pci.getDestination());
 		if (srcHW == null || dstHW == null)
 			return false;
 		if (srcHW == dstHW && connectedToBus(srcHW, curBus)) {
 			return true;
 		}
 		return false;
 	}
 
 	protected ComponentInstance getHardwareComponent(ConnectionInstanceEnd cie) {
 		ComponentInstance ci = null;
 		if (cie instanceof FeatureInstance) {
 			FeatureInstance fi = (FeatureInstance) cie;
 			ci = fi.getContainingComponentInstance();
 			if (ci.getCategory() == ComponentCategory.DEVICE) {
 				return ci;
 			}
 			List<ComponentInstance> ciList = GetProperties.getActualProcessorBinding(ci);
 			ci = ciList.isEmpty() ? null : ciList.get(0);
 		}
 		return ci;
 	}
 
 	/**
 	 * true if the processor of the port connection source is connected to the
 	 * specified bus
 	 * 
 	 * @param pci
 	 * @param curBus
 	 * @return
 	 */
 	protected boolean connectedByBus(ConnectionInstance pci, ComponentInstance curBus) {
 		ComponentInstance srcHW = getHardwareComponent(pci.getSource());
 		ComponentInstance dstHW = getHardwareComponent(pci.getDestination());
 		if (srcHW == null || dstHW == null || srcHW == dstHW)
 			return false;
 
 		return connectedToBus(srcHW, curBus) && connectedToBus(dstHW, curBus);
 
 	}
 
 	/**
 	 * is hardware component connected (directly) to the given bus
 	 * 
 	 * @param HWcomp ComponentInstance hardware component
 	 * @param bus ComponentInstance bus component
 	 * @return true if they are connected by bus access connection
 	 */
 	protected boolean connectedToBus(ComponentInstance HWcomp, ComponentInstance bus) {
 		EList acl = bus.getSrcConnectionInstances();
 		for (Iterator it = acl.iterator(); it.hasNext();) {
 			ConnectionInstance srcaci = (ConnectionInstance) it.next();
 			if (srcaci.getDestination().getContainingComponentInstance() == HWcomp) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	protected double sumActuals(ComponentInstance ci, boolean isROM) {
 		try {
 			double total = isROM ? GetProperties.getROMActualInKB(ci, 0.0):
 				GetProperties.getRAMActualInKB(ci, 0.0);
 			EList subcis = ci.getComponentInstances();
 			for (Iterator it = subcis.iterator(); it.hasNext();) {
 				ComponentInstance subci = (ComponentInstance) it.next();
 				total += sumActuals(subci, isROM);
 			}
 			return total;
 		} catch (PropertyDoesNotApplyToHolderException e) {
 			/*
 			 * Callers are allowed to be sloppy and not care if the property
 			 * actually applies to the category of the component instance 'ci'
 			 */
 			return 0.0;
 		}
 	}
 
 	protected void errorSummary(final Element obj, String somName, String msg) {
 		errManager.error(obj, somName+msg);
 		loggingErrManager.error(obj, somName+msg);
 	}
 
 	protected void warningSummary(final Element obj, String somName, String msg) {
 		errManager.warning(obj, somName+msg);
 		loggingErrManager.warning(obj, somName+msg);
 	}
 
 	protected void infoSummary(final Element obj, String somName, String msg) {
 		errManager.info(obj, somName+msg);
 		loggingErrManager.info(obj, somName+msg);
 	}
 
 	protected String getResultsMessages() {
 		synchronized (reportMessage) {
 			return reportMessage.toString();
 		}
 	}
 }
