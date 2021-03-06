 /*
 * $Id: MicroArrayDataForm.java,v 1.6 2008-08-14 20:09:18 schroedn Exp $
  *
  * $Log: not supported by cvs2svn $
  * Revision 1.5  2008/08/14 06:29:33  schroedn
  * microarray url feature added
  *
  * Revision 1.4  2006/04/17 19:09:19  pandyas
  * caMod 2.1 OM changes
  *
  */
 package gov.nih.nci.camod.webapp.form;
 
 import java.io.Serializable;
 
/**
 * 
 * $Id: MicroArrayDataForm.java,v 1.6 2008-08-14 20:09:18 schroedn Exp $
 *
 * $Log: not supported by cvs2svn $
 * Revision 1.5  2008/08/14 06:29:33  schroedn
 * microarray url feature added
 *
 * Revision 1.4  2006/04/17 19:09:19  pandyas
 * caMod 2.1 OM changes
 *
 * 
 */
 public class MicroArrayDataForm extends BaseForm implements Serializable, MicroArrayDataData {
     
     private static final long serialVersionUID = 3257015453799404851L;
     
	/**
	 * Default empty constructor
	 * @author rajputs
	 */
	public MicroArrayDataForm() {}
	
 	protected String experimentName;
 	protected String experimentId;
 	protected String url;
 	
 	/**
 	 * @return Returns the experimentName.
 	 */
 	public String getExperimentName() {
 		return experimentName;
 	}
 	/**
 	 * @param experimentName The experimentName to set.
 	 */
 	public void setExperimentName(String experimentName) {
 		this.experimentName = experimentName;
 	}	
 	/**
 	 * @return Returns the experimentId.
 	 */
 	public String getExperimentId() {
 		return experimentId;
 	}
 	/**
 	 * @param experimentId The experimentId to set.
 	 */
 	public void setExperimentId(String experimentId) {
 		this.experimentId = experimentId;
 	}
 	/**
 	 * @return Returns the url.
 	 */
 	public String getUrl() {
 		return url;
 	}
 	/**
 	 * @param otherLocationURL The otherLocationURL to set.
 	 */
 	public void setUrl(String url) {
 		this.url = url;
 	}
 
 }
