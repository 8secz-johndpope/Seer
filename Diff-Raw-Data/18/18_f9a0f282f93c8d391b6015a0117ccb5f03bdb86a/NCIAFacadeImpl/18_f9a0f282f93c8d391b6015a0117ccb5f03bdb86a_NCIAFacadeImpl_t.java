 /**
  * The software subject to this notice and license includes both human readable
  * source code form and machine readable, binary, object code form. The caIntegrator2
  * Software was developed in conjunction with the National Cancer Institute 
  * (NCI) by NCI employees, 5AM Solutions, Inc. (5AM), ScenPro, Inc. (ScenPro)
  * and Science Applications International Corporation (SAIC). To the extent 
  * government employees are authors, any rights in such works shall be subject 
  * to Title 17 of the United States Code, section 105. 
  *
  * This caIntegrator2 Software License (the License) is between NCI and You. You (or 
  * Your) shall mean a person or an entity, and all other entities that control, 
  * are controlled by, or are under common control with the entity. Control for 
  * purposes of this definition means (i) the direct or indirect power to cause 
  * the direction or management of such entity, whether by contract or otherwise,
  * or (ii) ownership of fifty percent (50%) or more of the outstanding shares, 
  * or (iii) beneficial ownership of such entity. 
  *
  * This License is granted provided that You agree to the conditions described 
  * below. NCI grants You a non-exclusive, worldwide, perpetual, fully-paid-up, 
  * no-charge, irrevocable, transferable and royalty-free right and license in 
  * its rights in the caIntegrator2 Software to (i) use, install, access, operate, 
  * execute, copy, modify, translate, market, publicly display, publicly perform,
  * and prepare derivative works of the caIntegrator2 Software; (ii) distribute and 
  * have distributed to and by third parties the caIntegrator2 Software and any 
  * modifications and derivative works thereof; and (iii) sublicense the 
  * foregoing rights set out in (i) and (ii) to third parties, including the 
  * right to license such rights to further third parties. For sake of clarity, 
  * and not by way of limitation, NCI shall have no right of accounting or right 
  * of payment from You or Your sub-licensees for the rights granted under this 
  * License. This License is granted at no charge to You.
  *
  * Your redistributions of the source code for the Software must retain the 
  * above copyright notice, this list of conditions and the disclaimer and 
  * limitation of liability of Article 6, below. Your redistributions in object 
  * code form must reproduce the above copyright notice, this list of conditions 
  * and the disclaimer of Article 6 in the documentation and/or other materials 
  * provided with the distribution, if any. 
  *
  * Your end-user documentation included with the redistribution, if any, must 
  * include the following acknowledgment: This product includes software 
  * developed by 5AM, ScenPro, SAIC and the National Cancer Institute. If You do 
  * not include such end-user documentation, You shall include this acknowledgment 
  * in the Software itself, wherever such third-party acknowledgments normally 
  * appear.
  *
  * You may not use the names "The National Cancer Institute", "NCI", "ScenPro",
  * "SAIC" or "5AM" to endorse or promote products derived from this Software. 
  * This License does not authorize You to use any trademarks, service marks, 
  * trade names, logos or product names of either NCI, ScenPro, SAID or 5AM, 
  * except as required to comply with the terms of this License. 
  *
  * For sake of clarity, and not by way of limitation, You may incorporate this 
  * Software into Your proprietary programs and into any third party proprietary 
  * programs. However, if You incorporate the Software into third party 
  * proprietary programs, You agree that You are solely responsible for obtaining
  * any permission from such third parties required to incorporate the Software 
  * into such third party proprietary programs and for informing Your a
  * sub-licensees, including without limitation Your end-users, of their 
  * obligation to secure any required permissions from such third parties before 
  * incorporating the Software into such third party proprietary software 
  * programs. In the event that You fail to obtain such permissions, You agree 
  * to indemnify NCI for any claims against NCI by such third parties, except to 
  * the extent prohibited by law, resulting from Your failure to obtain such 
  * permissions. 
  *
  * For sake of clarity, and not by way of limitation, You may add Your own 
  * copyright statement to Your modifications and to the derivative works, and 
  * You may provide additional or different license terms and conditions in Your 
  * sublicenses of modifications of the Software, or any derivative works of the 
  * Software as a whole, provided Your use, reproduction, and distribution of the
  * Work otherwise complies with the conditions stated in this License.
  *
  * THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED WARRANTIES, 
  * (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY, 
  * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE) ARE DISCLAIMED. IN NO 
  * EVENT SHALL THE NATIONAL CANCER INSTITUTE, 5AM SOLUTIONS, INC., SCENPRO, INC.,
  * SCIENCE APPLICATIONS INTERNATIONAL CORPORATION OR THEIR 
  * AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
  * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
  * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
  * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
  * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
  * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
  * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package gov.nih.nci.caintegrator2.external.ncia;
 
 import gov.nih.nci.caintegrator2.domain.imaging.ImageSeries;
 import gov.nih.nci.caintegrator2.domain.imaging.ImageSeriesAcquisition;
 import gov.nih.nci.caintegrator2.external.ConnectionException;
 import gov.nih.nci.caintegrator2.external.ServerConnectionProfile;
 import gov.nih.nci.caintegrator2.file.FileManager;
 import gov.nih.nci.ncia.domain.Image;
 import gov.nih.nci.ncia.domain.Patient;
 import gov.nih.nci.ncia.domain.Series;
 import gov.nih.nci.ncia.domain.Study;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 
 import org.apache.log4j.Logger;
 
 /**
  * Implementation of the NCIAFacade.
  */
 public class NCIAFacadeImpl implements NCIAFacade {
     
     private static final Logger LOGGER = Logger.getLogger(NCIAFacadeImpl.class);
     private NCIAServiceFactory nciaServiceFactory;
     private NCIADicomJobFactory nciaDicomJobFactory;
     private FileManager fileManager;
 
     /**
      * {@inheritDoc}
      */
     public List<String> getAllCollectionNameProjects(ServerConnectionProfile profile) 
     throws ConnectionException {
         NCIASearchService client = nciaServiceFactory.createNCIASearchService(profile);
         return client.retrieveAllCollectionNameProjects();
     }
     
     /**
      * {@inheritDoc}
      */
     public List<ImageSeriesAcquisition> getImageSeriesAcquisitions(String collectionNameProject, 
             ServerConnectionProfile profile) throws ConnectionException {
         LOGGER.info("Retrieving ImageSeriesAcquisitions for " + collectionNameProject);
         NCIASearchService client = nciaServiceFactory.createNCIASearchService(profile);
         List<ImageSeriesAcquisition> imageSeriesAcquisitions = new ArrayList<ImageSeriesAcquisition>();
         List<Patient> patientCollection = 
             client.retrievePatientCollectionFromCollectionNameProject(collectionNameProject);
         for (Patient patient : patientCollection) {
             imageSeriesAcquisitions.addAll(createImageSeriesAcquisitions(patient, client));
         }
         LOGGER.info("Completed retrieving ImageSeriesAcquisitions for " + collectionNameProject);
         return imageSeriesAcquisitions;
     }
 
     private List<ImageSeriesAcquisition> createImageSeriesAcquisitions(Patient patient, NCIASearchService client) 
     throws ConnectionException {
         List<Study> studies = client.retrieveStudyCollectionFromPatient(patient.getPatientId());
         List<ImageSeriesAcquisition> acquisitions = new ArrayList<ImageSeriesAcquisition>(studies.size());
         for (Study study : studies) {
             acquisitions.add(convertToImageSeriesAcquisition(study, client));
         }
         return acquisitions;
     }
 
     private ImageSeriesAcquisition convertToImageSeriesAcquisition(Study study, NCIASearchService client) 
     throws ConnectionException {
         ImageSeriesAcquisition acquisition = new ImageSeriesAcquisition();
         acquisition.setIdentifier(study.getStudyInstanceUID());
         acquisition.setSeriesCollection(new HashSet<ImageSeries>());
         List<Series> seriesList = client.retrieveImageSeriesCollectionFromStudy(study.getStudyInstanceUID());
         for (Series series : seriesList) {
             ImageSeries imageSeries = convertToImageSeries(series, client);
             acquisition.getSeriesCollection().add(imageSeries);
             imageSeries.setImageStudy(acquisition);
         }
         return acquisition;
     }
 
    @SuppressWarnings("unused") // See "TJ NOTE" below, this should be temporary.
     private ImageSeries convertToImageSeries(Series series, NCIASearchService client) throws ConnectionException {
         ImageSeries imageSeries = new ImageSeries();
         imageSeries.setIdentifier(series.getSeriesInstanceUID());
         imageSeries.setImageCollection(new HashSet<gov.nih.nci.caintegrator2.domain.imaging.Image>());
        // TJ NOTE: 5-5-2009
        // For now we want to hack this out for a demo, don't want to retrieve the images because it times server out.
//        List<Image> nciaImageList = client.retrieveImageCollectionFromSeries(series.getSeriesInstanceUID());
//        for (Image nciaImage : nciaImageList) {
//            gov.nih.nci.caintegrator2.domain.imaging.Image image = convertToImage(nciaImage);
//            imageSeries.getImageCollection().add(image);
//            image.setSeries(imageSeries);
//        }
         return imageSeries;
     }
 
    @SuppressWarnings("unused") // See "TJ NOTE" above in previous method, this should be temporary.
     private gov.nih.nci.caintegrator2.domain.imaging.Image convertToImage(Image nciaImage) {
         gov.nih.nci.caintegrator2.domain.imaging.Image image = new gov.nih.nci.caintegrator2.domain.imaging.Image();
         image.setIdentifier(nciaImage.getSopInstanceUID());
         return image;
     }
     
     /**
      * {@inheritDoc}
      */
     public File retrieveDicomFiles(NCIADicomJob job) 
         throws ConnectionException {
         if (!job.hasData()) {
             return null;
         }
         NCIADicomJobRunner jobRunner = nciaDicomJobFactory.createNCIADicomJobRunner(fileManager, job);
         return jobRunner.retrieveDicomFiles();
     }
 
     /**
      * @return the nciaServiceFactory
      */
     public NCIAServiceFactory getNciaServiceFactory() {
         return nciaServiceFactory;
     }
 
     /**
      * @param nciaServiceFactory the nciaServiceFactory to set
      */
     public void setNciaServiceFactory(NCIAServiceFactory nciaServiceFactory) {
         this.nciaServiceFactory = nciaServiceFactory;
     }
 
     /**
      * @return the fileManager
      */
     public FileManager getFileManager() {
         return fileManager;
     }
 
     /**
      * @param fileManager the fileManager to set
      */
     public void setFileManager(FileManager fileManager) {
         this.fileManager = fileManager;
     }
 
     /**
      * @return the nciaDicomJobFactory
      */
     public NCIADicomJobFactory getNciaDicomJobFactory() {
         return nciaDicomJobFactory;
     }
 
     /**
      * @param nciaDicomJobFactory the nciaDicomJobFactory to set
      */
     public void setNciaDicomJobFactory(NCIADicomJobFactory nciaDicomJobFactory) {
         this.nciaDicomJobFactory = nciaDicomJobFactory;
     }
   
 }
