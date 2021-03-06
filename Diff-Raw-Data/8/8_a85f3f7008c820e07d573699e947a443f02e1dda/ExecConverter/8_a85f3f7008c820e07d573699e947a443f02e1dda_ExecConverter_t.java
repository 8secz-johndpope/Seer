 package de.hpi.bpmn2execpn.converter;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import sun.reflect.generics.reflectiveObjects.NotImplementedException;
 import de.hpi.bpmn.BPMNDiagram;
 import de.hpi.bpmn.DataObject;
 import de.hpi.bpmn.Edge;
 import de.hpi.bpmn.IntermediateEvent;
 import de.hpi.bpmn.SubProcess;
 import de.hpi.bpmn.Task;
 import de.hpi.bpmn2execpn.model.ExecTask;
 import de.hpi.bpmn2pn.converter.Converter;
 import de.hpi.bpmn2pn.model.ConversionContext;
 import de.hpi.bpmn2pn.model.SubProcessPlaces;
 import de.hpi.execpn.AutomaticTransition;
 import de.hpi.execpn.ExecFlowRelationship;
 import de.hpi.execpn.ExecPetriNet;
 import de.hpi.execpn.FormTransition;
 import de.hpi.execpn.impl.ExecPNFactoryImpl;
 import de.hpi.execpn.pnml.Locator;
 import de.hpi.petrinet.PetriNet;
 import de.hpi.petrinet.Place;
 import de.hpi.petrinet.Transition;
 
 public class ExecConverter extends Converter {
 
 	private static final boolean abortWhenFinalize = true;
 	private static final String baseXsltURL = "http://localhost:3000/examples/contextPlace/";
 	private static final String copyXsltURL = baseXsltURL + "copy_xslt.xsl";
 	private static final String extractDataURL = baseXsltURL + "extract_processdata.xsl";
 	protected String standardModel;
 	private List<ExecTask> taskList;
 
 	public ExecConverter(BPMNDiagram diagram, String modelURL) {
 		super(diagram, new ExecPNFactoryImpl(modelURL));
 		this.standardModel = modelURL;
 		this.taskList = new ArrayList<ExecTask>();
 	}
 		
 	@Override
 	protected void handleDiagram(PetriNet net, ConversionContext c) {
 		((ExecPetriNet) net).setName(diagram.getTitle());
 	}
 
 	@Override
 	protected void createStartPlaces(PetriNet net, ConversionContext c) {
 		// do nothing...: we want start transitions instead of start places
 	}
 
 	// TODO this is a dirty hack...
 	@Override
 	protected void handleTask(PetriNet net, Task task, ConversionContext c) {
 		ExecTask exTask = new ExecTask();
 		exTask.setId(task.getId());
 		exTask.setLabel(task.getLabel());
 		
 		// create proper model, form and bindings
 		String model = null;
 		String form = null;
 		String bindings = null;
 		
 		// TODO interrogate all incoming data objects for task and create model
 		List<Edge> edges = task.getIncomingEdges();
 		for (Edge edge : edges) {
 			if (edge.getSource() instanceof DataObject) {
 				DataObject partmodelObject = (DataObject)edge.getSource();
 				// TODO getContentXML() has to be implemented
 				// XMLDocument doc = partmodelObject.getContentXML();
 				// TODO iterate contentModel and add nonconflicting nodes
 				// for (...next()) for (...hasChild()) add+hasChild()
 			}
 		}
 		
 		// TODO with model create formular and bindings
 		
 		
 		exTask.pl_ready = addPlace(net, "pl_ready_" + task.getId());
 		exTask.pl_running = addPlace(net, "pl_running_" + task.getId());
 		exTask.pl_deciding = addPlace(net, "pl_deciding_" + task.getId());
 		exTask.pl_suspended = addPlace(net, "pl_suspended_" + task.getId());
 		exTask.pl_complete = addPlace(net, "pl_complete_" + task.getId());
 		
 		exTask.pl_context = addPlace(net, "pl_context_" + task.getId());
 		exTask.pl_context.addLocator(new Locator("isDelegated", "xsd:string", "/data/metadata/isdelegated"));
 		exTask.pl_context.addLocator(new Locator("isReviewed", "xsd:string", "/data/metadata/isreviewed"));
 		
 		//enable transition
 		//TODO: read/write to context place
 		exTask.tr_enable = exTask.tr_done = addAutomaticTransition(net, "tr_enable_" + task.getId(), task.getLabel(), "", copyXsltURL, false);
 		addFlowRelationship(net, c.map.get(getIncomingSequenceFlow(task)), exTask.tr_enable);
 		addFlowRelationship(net, exTask.tr_enable, exTask.pl_ready);
 		
 		// allocate Transition
 		//TODO: change context_allocate when context place gets initialized at start of process
 		exTask.tr_allocate = addAutomaticTransition(net, "tr_allocate_" + task.getId(), task.getLabel(), "allocate", copyXsltURL, true);
 		addFlowRelationship(net, exTask.pl_ready, exTask.tr_allocate);
 		addExecFlowRelationship(net, exTask.tr_allocate, exTask.pl_running, extractDataURL);
 		//addFlowRelationship(net, exTask.pl_context, exTask.tr_allocate);
 		addExecFlowRelationship(net, exTask.tr_allocate, exTask.pl_context, baseXsltURL + "context_allocate.xsl");
 		
 		if (task.isSkippable()) {
 			// skip Transition
 			exTask.setSkippable(true);
 			exTask.tr_skip = addAutomaticTransition(net, "tr_skip_" + task.getId(), task.getLabel(), "skip", copyXsltURL, true);
 			addFlowRelationship(net, exTask.pl_ready, exTask.tr_skip);
 			addExecFlowRelationship(net, exTask.tr_skip, exTask.pl_complete, extractDataURL);
 			addFlowRelationship(net, exTask.pl_context, exTask.tr_skip);
 			addExecFlowRelationship(net, exTask.tr_skip, exTask.pl_context, baseXsltURL + "context_skip.xsl");
 		}
 		
 		// submit Transition
 		FormTransition submit = addFormTransition(net, "tr_submit_" + task.getId(), task.getLabel(),model,form,bindings);
 		// TODO: FormTransition -> Form erstellen/angeben
 		submit.setAction("submit");
 		exTask.tr_submit = submit;
 		addFlowRelationship(net, exTask.pl_running, exTask.tr_submit);
 		addExecFlowRelationship(net, exTask.tr_submit, exTask.pl_deciding, extractDataURL);
 		addFlowRelationship(net, exTask.pl_context, exTask.tr_submit);
 		addExecFlowRelationship(net, exTask.tr_submit, exTask.pl_context, baseXsltURL + "context_submit.xsl");
 		
 		// delegate Transition
 		FormTransition delegate = addFormTransition(net, "tr_delegate_" + task.getId(), task.getLabel(),model,form,bindings);
 		// TODO: FormTransition -> Form erstellen/angeben
 		delegate.setAction("delegate");
 		delegate.setGuard("plFinish.isDelegated == true");
 		exTask.tr_delegate = delegate;
 		addFlowRelationship(net, exTask.pl_deciding, exTask.tr_delegate);
 		addExecFlowRelationship(net, exTask.tr_delegate, exTask.pl_running, extractDataURL);
 		addFlowRelationship(net, exTask.pl_context, exTask.tr_delegate);
 		addExecFlowRelationship(net, exTask.tr_delegate, exTask.pl_context, baseXsltURL + "context_delegate.xsl");
 		
 		// review Transition
 		FormTransition review = addFormTransition(net, "tr_review_" + task.getId(), task.getLabel(),model,form,bindings);
 		// TODO: FormTransition -> Form erstellen/angeben
 		review.setAction("review");
 		review.setGuard("(plFinish.owner != '') &amp;&amp; (plFinish.isReviewed == true) &amp;&amp; (plFinish.isDelegated == false) &amp;&amp; (plFinish.wantToReview == true)");
 		exTask.tr_review = review;
 		addFlowRelationship(net, exTask.pl_deciding, exTask.tr_review);
 		addExecFlowRelationship(net, exTask.tr_review, exTask.pl_complete, extractDataURL);
 		addFlowRelationship(net, exTask.pl_context, exTask.tr_review);
 		addExecFlowRelationship(net, exTask.tr_review, exTask.pl_context, baseXsltURL + "context_review.xsl");
 
 		// done Transition
 		exTask.tr_done = addAutomaticTransition(net, "tr_done_" + task.getId(), task.getLabel(), "", copyXsltURL, false);
 		addFlowRelationship(net, exTask.pl_deciding, exTask.tr_done);
 		addFlowRelationship(net, exTask.tr_done, exTask.pl_complete);
 		addFlowRelationship(net, exTask.pl_context, exTask.tr_done);
 		addExecFlowRelationship(net, exTask.tr_done, exTask.pl_context, baseXsltURL + "context_done.xsl");
		exTask.tr_done.setGuard("((plFinish.owner == '') || (plFinish.isReviewed == false) || (plFinish.wantToReview == false)) &amp;&amp; (plFinish.isDelegated == false)");
 		
 		// suspend/resume
 		exTask.tr_suspend = addAutomaticTransition(net, "tr_suspend_" + task.getId(), task.getLabel(), "suspend", copyXsltURL, true);
 		addFlowRelationship(net, exTask.pl_running, exTask.tr_suspend);
 		addExecFlowRelationship(net, exTask.tr_suspend, exTask.pl_suspended, extractDataURL);
 		addFlowRelationship(net, exTask.pl_context, exTask.tr_suspend);
 		addExecFlowRelationship(net, exTask.tr_suspend, exTask.pl_context, baseXsltURL + "context_suspend.xsl");
 		
 		exTask.tr_resume = addAutomaticTransition(net, "tr_resume_" + task.getId(), task.getLabel(), "resume", copyXsltURL, true);
 		addFlowRelationship(net, exTask.pl_suspended, exTask.tr_resume);
 		addExecFlowRelationship(net, exTask.tr_resume, exTask.pl_running, extractDataURL);
 		addFlowRelationship(net, exTask.pl_context, exTask.tr_resume);
 		addExecFlowRelationship(net, exTask.tr_resume, exTask.pl_context, baseXsltURL + "context_resume.xsl");
 		
 		// finish transition
 		//TODO: create context_finish.xsl
 		exTask.tr_finish =exTask.tr_done = addAutomaticTransition(net, "tr_finish_" + task.getId(), task.getLabel(), "", copyXsltURL, false);
 		addFlowRelationship(net, exTask.pl_complete, exTask.tr_finish);
 		addExecFlowRelationship(net, exTask.tr_finish, c.map.get(getOutgoingSequenceFlow(task)), extractDataURL);
 		/*addFlowRelationship(net, exTask.pl_context, exTask.tr_finish);
 		addExecFlowRelationship(net, exTask.tr_finish, exTask.pl_context, baseXsltURL + "context_finish.xsl");
 		*/
 		
 		exTask.tr_delegate.setGuard(exTask.pl_context.getId() + ".isDelegated == 'true'");
 		exTask.tr_review.setGuard(exTask.pl_context.getId() + ".isDelegated != 'true' && " + exTask.pl_context.getId() + ".isReviewed == 'true'");
 		exTask.tr_done.setGuard(exTask.pl_context.getId() + ".isDelegated != 'true' && " + exTask.pl_context.getId() + ".isReviewed != 'true'");
 		
 		taskList.add(exTask);
 		
 		handleMessageFlow(net, task, exTask.tr_allocate, exTask.tr_submit, c);
 		if (c.ancestorHasExcpH)
 			handleExceptions(net, task, exTask.tr_submit, c);
 
 		for (IntermediateEvent event : task.getAttachedEvents())
 			handleAttachedIntermediateEventForTask(net, event, c);
 	}
 	
 	@Override
 	protected void handleSubProcess(PetriNet net, SubProcess process,
 			ConversionContext c) {
 		super.handleSubProcess(net, process, c);
 		if (process.isAdhoc()) {
 			handleSubProcessAdHoc(net, process, c);
 		}
 	}
 
 	// TODO: Data dependencies
 	// TODO missing completion condition concept
 	protected void handleSubProcessAdHoc(PetriNet net, SubProcess process,
 			ConversionContext c) {
 		SubProcessPlaces pl = c.getSubprocessPlaces(process);
 
 		// start and end transitions
 		Transition startT = addTauTransition(net, "ad-hoc_start_" + process.getId());
 		Transition endT = addTauTransition(net, "ad-hoc_end_" + process.getId());
 		Transition defaultEndT = addTauTransition(net, "ad-hoc_defaultEnd_" + process.getId());
 		Place execState = addPlace(net, "ad-hoc_execState_" + process.getId());
 		
 		addFlowRelationship(net, pl.startP, startT);
 		addFlowRelationship(net, startT, execState);
 		addFlowRelationship(net, execState, defaultEndT);
 		addFlowRelationship(net, execState, endT);
 		addFlowRelationship(net, defaultEndT, pl.endP);
 		addFlowRelationship(net, endT, pl.endP);
 
 		
 		// standard completion condition check
 		Place updatedState = addPlace(net, "ad-hoc_updatedState_" + process.getId());
 		Place ccStatus = addPlace(net, "ad-hoc_ccStatus_" + process.getId());
 		// TODO: make AutomaticTransition with functionality to evaluate completion condition
 		//Transition ccCheck = addLabeledTransition(net, "ad-hoc_ccCheck_" + process.getId(), "ad-hoc_cc_" + process.getCompletionCondition());
 		Transition ccCheck = addTauTransition(net, "ad-hoc_ccCheck_" + process.getId());
 		// TODO: make Tau when guards work
 		Transition finalize = addLabeledTransition(net, "ad-hoc_finalize_" + process.getId(), "ad-hoc_finalize");
 		// TODO: make Tau when guards work
 		//Transition resume = addLabeledTransition(net, "ad-hoc_resume_" + process.getId(), "ad-hoc_resume");
 		Transition resume = addTauTransition(net, "ad-hoc_resume_" + process.getId());
 		addFlowRelationship(net, updatedState, ccCheck);
 		addFlowRelationship(net, execState, ccCheck);
 		addFlowRelationship(net, ccCheck, execState);
 		addFlowRelationship(net, ccCheck, ccStatus);
 		
 		if (process.isParallelOrdering() && abortWhenFinalize) {
 			// parallel ad-hoc construct with abortion of tasks when completion condition is true -------------------------------
 			
 			//	synchronization and completionCondition checks(enableStarting, enableFinishing)
 			Place enableStarting = addPlace(net, "ad-hoc_enableStarting_" + process.getId());
 			Place enableFinishing = addPlace(net, "ad-hoc_enableFinishing_" + process.getId());
 			addFlowRelationship(net, startT, enableStarting);
 			addFlowRelationship(net, startT, enableFinishing);
 			
 			addFlowRelationship(net, enableStarting, defaultEndT);
 			addFlowRelationship(net, enableFinishing, defaultEndT);
 			
 			addFlowRelationship(net, enableStarting, ccCheck);
 			
 			addFlowRelationship(net, resume, enableStarting);
 			addFlowRelationship(net, resume, enableFinishing);
 			// TODO: add guard expressions
 			addFlowRelationship(net, ccStatus, resume); //guard expression: ccStatus == false
 			addFlowRelationship(net, ccStatus, finalize); // guard expression: ccStatus == true
 			
 			// task specific constructs
 			for (ExecTask exTask : taskList) {
 				// execution(enabledP, executedP, connections in between)
 				Place enabled = addPlace(net, "ad-hoc_task_enabled_"
 						+ exTask.getId());
 				Place executed = addPlace(net, "ad-hoc_task_executed_"
 						+ exTask.getId());
 				addFlowRelationship(net, startT, enabled);
 				addFlowRelationship(net, enabled, exTask.tr_allocate);
 				addFlowRelationship(net, enableStarting, exTask.tr_allocate);
 				addFlowRelationship(net, exTask.tr_allocate, enableStarting);
 				addFlowRelationship(net, enableFinishing, exTask.tr_done);
 				addFlowRelationship(net, exTask.tr_done, executed);		
 				addFlowRelationship(net, exTask.tr_done, updatedState);
 				addFlowRelationship(net, enableFinishing, exTask.tr_review);
 				addFlowRelationship(net, exTask.tr_review, executed);		
 				addFlowRelationship(net, exTask.tr_review, updatedState);
 				addFlowRelationship(net, executed, defaultEndT);
 
 				if (exTask.isSkippable()) {
 					addFlowRelationship(net, enabled, exTask.tr_skip);
 					addFlowRelationship(net, enableStarting, exTask.tr_skip);
 					addFlowRelationship(net, exTask.tr_skip, enableStarting);
 					addFlowRelationship(net, exTask.tr_skip, executed);
 					addFlowRelationship(net, exTask.tr_skip, updatedState);
 				}
 				
 				// finishing construct(finalize with skip, finish, abort and leave_suspend)
 				Place enableFinalize = addPlace(net, "ad-hoc_enable_finalize_task_" + exTask.getId());
 				Place taskFinalized = addPlace(net, "ad-hoc_task_finalized_" + exTask.getId());
 				Transition skip = addTauTransition(net, "ad-hoc_skip_task_"	+ exTask.getId());
 				Transition finish = addTauTransition(net, "ad-hoc_finish_task_"	+ exTask.getId());
 				Transition abort = addTauTransition(net, "ad-hoc_abort_task_" + exTask.getId());
 				Transition leaveSuspended = addTauTransition(net, "ad-hoc_leave_suspended_task_" + exTask.getId());
 				
 				addFlowRelationship(net, finalize, enableFinalize);
 				
 				addFlowRelationship(net, enableFinalize, skip);
 				addFlowRelationship(net, enabled, skip);
 				addFlowRelationship(net, skip, taskFinalized);
 				
 				addFlowRelationship(net, enableFinalize, finish);
 				addFlowRelationship(net, executed, finish);
 				addFlowRelationship(net, finish, taskFinalized);
 				
 				addFlowRelationship(net, enableFinalize, abort);
 				addFlowRelationship(net, exTask.pl_running, abort);
 				addFlowRelationship(net, abort, taskFinalized);
 					
 				addFlowRelationship(net, enableFinalize,  leaveSuspended);
 				addFlowRelationship(net, exTask.pl_suspended,  leaveSuspended);
 				addFlowRelationship(net,  leaveSuspended, taskFinalized);
 				
 				addFlowRelationship(net, taskFinalized, endT);	
 			}
 		}else if (process.isParallelOrdering() && !abortWhenFinalize) {
 			// parallel ad-hoc construct, running tasks can finish on their own after completion condition is true -------------
 			throw new NotImplementedException();
 		}else {
 			// sequential ad-hoc construct -----------------------------------------------------------------------------------------------
 
 			// synchronization and completionCondition checks(synch, corresponds to enableStarting)
 			Place synch = addPlace(net, "ad-hoc_synch_" + process.getId());
 			addFlowRelationship(net, startT, synch);
 			addFlowRelationship(net, synch, defaultEndT);
 			addFlowRelationship(net, resume, synch);
 			// TODO: add guard expressions
 			addFlowRelationship(net, ccStatus, resume); //guard expression: ccStatus == false
 			addFlowRelationship(net, ccStatus, finalize); // guard expression: ccStatus == true
 
 //			 task specific constructs
 			for (ExecTask exTask : taskList) {
 				// execution(enabledP, executedP, connections in between)
 				Place enabled = addPlace(net, "ad-hoc_task_enabled_" + exTask.getId());
 				Place executed = addPlace(net, "ad-hoc_task_executed_" + exTask.getId());
 				addFlowRelationship(net, startT, enabled);
 				addFlowRelationship(net, enabled, exTask.tr_allocate);
 				addFlowRelationship(net, synch, exTask.tr_allocate);
 				addFlowRelationship(net, exTask.tr_review, executed);
 				addFlowRelationship(net, exTask.tr_review, updatedState);
 				addFlowRelationship(net, exTask.tr_done, executed);
 				addFlowRelationship(net, exTask.tr_done, updatedState);
 				addFlowRelationship(net, executed, defaultEndT);
 
 				if (exTask.isSkippable()) {
 					addFlowRelationship(net, exTask.tr_skip, executed);
 					addFlowRelationship(net, exTask.tr_skip, updatedState);
 					addFlowRelationship(net, enabled, exTask.tr_skip);
 					addFlowRelationship(net, synch, exTask.tr_skip);
 				}
 
 				// finishing construct(finalize with skip, finish and abort)
 				Place enableFinalize = addPlace(net, "ad-hoc_enable_finalize_task_" + exTask.getId());
 				Place taskFinalized = addPlace(net, "ad-hoc_task_finalized_" + exTask.getId());
 				Transition skip = addTauTransition(net, "ad-hoc_skip_task_"	+ exTask.getId());
 				Transition finish = addTauTransition(net, "ad-hoc_finish_task_" + exTask.getId());
 				
 				addFlowRelationship(net, finalize, enableFinalize);
 				
 				addFlowRelationship(net, enableFinalize, skip);
 				addFlowRelationship(net, enabled, skip);
 				addFlowRelationship(net, skip, taskFinalized);
 				
 				addFlowRelationship(net, enableFinalize, finish);
 				addFlowRelationship(net, executed, finish);
 				addFlowRelationship(net, finish, taskFinalized);
 								
 				addFlowRelationship(net, taskFinalized, endT);	
 			}
 		}
 	}
 	
 	public AutomaticTransition addAutomaticTransition(PetriNet net, String id, String label, String action, String xsltURL, boolean triggerManually) {
 		AutomaticTransition t =((ExecPNFactoryImpl) pnfactory).createAutomaticTransition();
 		t.setId(id);
 		t.setLabel(label);
 		t.setAction(action);
 		t.setXsltURL(xsltURL);
 		t.setManuallyTriggered(triggerManually);
 		net.getTransitions().add(t);
 		return t;
 	}
 	
 	public ExecFlowRelationship addExecFlowRelationship(PetriNet net,
 			de.hpi.petrinet.Node source, de.hpi.petrinet.Node target, String xsltURL) {
 		if (source == null || target == null)
 			return null;
 		ExecFlowRelationship rel = ((ExecPNFactoryImpl) pnfactory).createExecFlowRelationship();
 		rel.setSource(source);
 		rel.setTarget(target);
 		rel.setTransformationURL(xsltURL);
 		net.getFlowRelationships().add(rel);
 		return rel;
 	}
 }
