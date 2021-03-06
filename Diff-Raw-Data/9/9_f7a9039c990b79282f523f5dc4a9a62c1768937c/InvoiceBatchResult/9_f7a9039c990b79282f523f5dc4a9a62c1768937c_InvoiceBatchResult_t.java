 package se.idega.idegaweb.commune.accounting.invoice.presentation;
 
 import java.rmi.RemoteException;
 import java.util.Collection;
 import java.util.Iterator;
 
 import javax.ejb.FinderException;
 
 import se.idega.idegaweb.commune.accounting.invoice.business.InvoiceBusiness;
 import se.idega.idegaweb.commune.accounting.invoice.data.BatchRun;
 import se.idega.idegaweb.commune.accounting.invoice.data.BatchRunError;
 import se.idega.idegaweb.commune.accounting.invoice.data.BatchRunErrorHome;
 import se.idega.idegaweb.commune.accounting.posting.business.PostingBusiness;
 import se.idega.idegaweb.commune.accounting.presentation.AccountingBlock;
 import se.idega.idegaweb.commune.accounting.presentation.OperationalFieldsMenu;
 
 import com.idega.business.IBOLookup;
 import com.idega.data.IDOLookup;
 import com.idega.idegaweb.IWApplicationContext;
 import com.idega.presentation.IWContext;
 import com.idega.presentation.Table;
 import com.idega.presentation.text.Text;
 import com.idega.presentation.ui.Form;
 import com.idega.util.IWTimestamp;
 
 /**
  * Displays the results of the batchrun, and all possible errors that could have occured
  * see fonster 33 in C&P req spec.
  * 
  * @see se.idega.idegaweb.commune.accounting.invoice.business.InvoiceBusiness
  * @see se.idega.idegaweb.commune.accounting.invoice.business.BillingThread
  * 
  * @author Joakim
  */
 public class InvoiceBatchResult extends AccountingBlock{
 	
 	public void init(IWContext iwc){
 		Form form = new Form();
 		Table table = new Table(2,6);
 		OperationalFieldsMenu opFields = new OperationalFieldsMenu();
 		
 		try {
 			handleAction(iwc);
 		
 			//"Top section"
 			add(opFields);
 
 			add(form);
 			
 			table.add(getLocalizedLabel("invbr.period","Period"),1,1);
 			table.add(getLocalizedLabel("invbr.batchrun_starttime","Batchrun start-time"),1,2);
 			table.add(getLocalizedLabel("invbr.batchrun_endtime","Batchrun end-time"),1,3);
 			table.add(getLocalizedLabel("invbr.number_of_invoices","Number of invoices"),1,4);
 			table.add(getLocalizedLabel("invbr.number_of_billed_children","Number of handled children"),1,5);
 			table.add(getLocalizedLabel("invbr.totalAmount","Total amount"),1,6);
 
 			InvoiceBusiness invoiceBusiness = getInvoiceBusiness(iwc);
 			String schoolCategory = getSession().getOperationalField();
 
 			BatchRun batchRun = invoiceBusiness.getBatchRunByCategory(schoolCategory);
 
 			IWTimestamp period = new IWTimestamp(batchRun.getPeriod());
 			IWTimestamp start = new IWTimestamp(batchRun.getStart());
 			
 			IWTimestamp end = null;
 			if(batchRun.getEnd()!=null){
 				end = new IWTimestamp(batchRun.getEnd());
 			}
 
 			table.add(period.getDateString("MMM yyyy"),2,1);
 			table.add(start.getDateString("yyyy-MM-dd kk:mm:ss"),2,2);
			if(end!=null){
				table.add(end.getDateString("yyyy-MM-dd kk:mm:ss"),2,3);
			} else {
				table.add(getLocalizedSmallText("invbr.not_finished","Not finished"),2,3);
			}
 			table.add(""+invoiceBusiness.getNoProviders(batchRun),2,4);
 			table.add(""+invoiceBusiness.getNoPlacements(batchRun),2,5);
 			table.add(""+invoiceBusiness.getTotAmountWithoutVAT(batchRun),2,6);
 		
 			form.add(table);
 			
 			int row = 1;
 			BatchRunErrorHome batchRunErrorHome = (BatchRunErrorHome)IDOLookup.getHome(BatchRunError.class);
 			Collection errorColl = batchRunErrorHome.findByBatchRun(batchRun);
 
 			//Bottom section
 			add(getLocalizedLabel("invbr.Total_number_of_suspected_errors","Total number of suspected errors"));
 			add(new Text(new Integer(errorColl.size()).toString()));
 
 			//Middle section with the error list
 			Table errorTable = new Table();
 
 			System.out.println("Size of table BatchRunError: "+errorColl.size());
 			Iterator errorIter = errorColl.iterator();
 			if(errorIter.hasNext()){
 				System.out.println("Found error description");
 				errorTable.add(getLocalizedLabel("invbr.number","Nr"),1,1);
 				errorTable.add(getLocalizedLabel("invbr.related_object","Related object"),2,1);
 				errorTable.add(getLocalizedLabel("invbr.suspected_error","Suspected error"),3,1);
 				
 				while(errorIter.hasNext()){
 					BatchRunError batchRunError = (BatchRunError)errorIter.next();
 					errorTable.setRowColor (row, (row % 2 == 0) ? getZebraColor1 ()
 									   : getZebraColor2 ());
 					errorTable.add(new Text(new Integer(row).toString()),1,row+1);
 					if(batchRunError.getRelated().indexOf("invoice.")!=-1){
 						errorTable.add(getLocalizedLabel(batchRunError.getRelated(),batchRunError.getRelated()),2,row+1);
 						System.out.println("Error "+batchRunError.getRelated());
 					}else{
 						errorTable.add(new Text(batchRunError.getRelated()),2,row+1);
 						System.out.println("Error Plain text "+batchRunError.getRelated());
 					}
 					errorTable.add(new Text(batchRunError.getDescription()),3,row+1);
 					row++;
 				}
 				System.out.println("Found errors: "+row);
 				add(errorTable);
 			}
 			
 		} catch (FinderException e) {
 			add(getLocalizedSmallHeader("invbr.no_batchrun_available","No batchrun available"));
 		} catch (Exception e) {
 			add(getLocalizedSmallHeader("invbr.error_occured","Error occured"));
 			e.printStackTrace();
 		}
 	}
 	
 	/**
 	 * Probably remove to handle the navigation outside this presentation block
 	 * @param iwc
 	 */
 	private void handleAction(IWContext iwc) {
 		System.out.println("WARNING, program should not reach this point...");
 		iwc.toString();	//Dummy line to remove warning
 	}
 	
 	protected PostingBusiness getPostingBusiness(IWApplicationContext iwc) throws RemoteException {
 		return (PostingBusiness) IBOLookup.getServiceInstance(iwc, PostingBusiness.class);
 	}	
 	
 	protected InvoiceBusiness getInvoiceBusiness(IWApplicationContext iwc) throws RemoteException {
 		return (InvoiceBusiness) IBOLookup.getServiceInstance(iwc, InvoiceBusiness.class);
 	}
 
 }
