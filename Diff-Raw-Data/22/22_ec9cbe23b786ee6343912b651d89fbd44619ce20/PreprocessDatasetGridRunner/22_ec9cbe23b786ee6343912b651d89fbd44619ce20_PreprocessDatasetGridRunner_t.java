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
 package gov.nih.nci.caintegrator2.application.analysis.grid.preprocess;
 
 import gov.nih.nci.cagrid.common.ZipUtilities;
 import gov.nih.nci.caintegrator2.common.CaGridUtil;
 import gov.nih.nci.caintegrator2.common.Cai2Util;
 import gov.nih.nci.caintegrator2.domain.application.StudySubscription;
 import gov.nih.nci.caintegrator2.external.ConnectionException;
 
 import java.io.BufferedInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.rmi.RemoteException;
 
 import org.apache.axis.types.URI.MalformedURIException;
 import org.apache.commons.io.FileUtils;
 import org.apache.log4j.Logger;
 import org.cagrid.transfer.context.client.TransferServiceContextClient;
 import org.cagrid.transfer.context.client.helper.TransferClientHelper;
 import org.cagrid.transfer.context.stubs.types.TransferServiceContextReference;
 import org.cagrid.transfer.descriptor.Status;
 import org.genepattern.cagrid.service.preprocessdataset.mage.common.PreprocessDatasetMAGEServiceI;
 import org.genepattern.cagrid.service.preprocessdataset.mage.context.client.PreprocessDatasetMAGEServiceContextClient;
 import org.genepattern.cagrid.service.preprocessdataset.mage.context.stubs.types.AnalysisNotComplete;
 import org.genepattern.cagrid.service.preprocessdataset.mage.context.stubs.types.CannotLocateResource;
 
 
 /**
  * Runs the GenePattern grid service PreprocessDataset (MAGE).
  */
 public class PreprocessDatasetGridRunner {
     private static final Logger LOGGER = Logger.getLogger(PreprocessDatasetGridRunner.class);
     private static final int DOWNLOAD_REFRESH_INTERVAL = 1000;
     private static final int TIMEOUT_SECONDS = 300;
     private final PreprocessDatasetMAGEServiceI client;
     
     /**
      * Public Constructor.
      * @param client of grid service.
      */
     public PreprocessDatasetGridRunner(PreprocessDatasetMAGEServiceI client) {
         this.client = client;
     }
     
     /**
      * Executes the grid service PreprocessDataset.
      * @param studySubscription for current study.
      * @param parameters for preprocess dataset.
      * @param gctFile the unprocessed gct file to run preprocess on.
      * @throws ConnectionException if unable to connect to grid service.
      * @throws InterruptedException if thread is interrupted while waiting for file download.
      */
     public void execute(StudySubscription studySubscription, PreprocessDatasetParameters parameters, 
             File gctFile) 
         throws ConnectionException, InterruptedException {
         // TODO figure out the "if" condition here, probably number of lines needs to be greater than something.
 //        if (dataset.getValues().length > 0) {
             
 //        }
         runPreprocessDataset(parameters, gctFile);
     }
 
     
     private File runPreprocessDataset(PreprocessDatasetParameters parameters, File unprocessedGctFile) 
     throws ConnectionException, InterruptedException {
         try {
             PreprocessDatasetMAGEServiceContextClient analysis = client.createAnalysis();
             postUpload(analysis, parameters, unprocessedGctFile);
             return downloadResult(unprocessedGctFile, analysis);
         } catch (RemoteException e) {
             throw new ConnectionException("Remote Connection Failed.", e);
         } catch (MalformedURIException e) {
             throw new ConnectionException("Malformed URI.", e);
         } catch (IOException e) {
             throw new IllegalStateException("Unable to read GCT file.");
         }
     }
 
     private void postUpload(PreprocessDatasetMAGEServiceContextClient analysis, PreprocessDatasetParameters parameters,
             File unprocessedGctFile) throws IOException, ConnectionException {
         TransferServiceContextReference up = analysis.submitData(parameters.createParameterList());
         TransferServiceContextClient tClient = new TransferServiceContextClient(up.getEndpointReference());
         BufferedInputStream bis = null;
         try {
             long size = unprocessedGctFile.length();
             bis = new BufferedInputStream(new FileInputStream(unprocessedGctFile));
             TransferClientHelper.putData(bis, size, tClient.getDataTransferDescriptor());
         } catch (Exception e) {
          // For some reason TransferClientHelper throws "Exception", going to rethrow a connection exception.
             throw new ConnectionException("Unable to transfer gct data to the server.", e);
         } finally {
             if (bis != null) {
                 bis.close();
             }
         }
         tClient.setStatus(Status.Staged);
     }
     
     private File downloadResult(File gctFile, PreprocessDatasetMAGEServiceContextClient analysisClient) 
     throws ConnectionException, InterruptedException, IOException {
         TransferServiceContextReference tscr = null;
         String hostInfo = "N/A";
         int callCount = 0;
        hostInfo = analysisClient.getEndpointReference().getAddress().getHost()
              + ":"
              + analysisClient.getEndpointReference().getAddress().getPort()
              + analysisClient.getEndpointReference().getAddress().getPath();
         while (tscr == null) {
             try {
                 callCount++;
                 tscr = analysisClient.getResult();
             } catch (AnalysisNotComplete e) {
                 LOGGER.info("Preprocess - Attempt # " + callCount + " to host:"
                         + hostInfo + " - Analysis not complete");
                 checkTimeout(callCount);
             } catch (CannotLocateResource e) {
                 LOGGER.info("Preprocess - Attempt # " + callCount + " to host:"
                         + hostInfo + " - Cannot locate resource");
                 checkTimeout(callCount);
             } catch (RemoteException e) {
                 throw new ConnectionException("Unable to connect to server to download result.", e);
             }
             Thread.sleep(DOWNLOAD_REFRESH_INTERVAL);
         }
         File zipFile = CaGridUtil.retrieveFileFromTscr(gctFile.getAbsolutePath() + ".zip", tscr); 
         return replaceGctFileWithPreprocessed(gctFile, zipFile);
     }
 
     private File replaceGctFileWithPreprocessed(File gctFile, File zipFile) throws IOException {
         File zipFileDirectory = new File(zipFile.getParent().concat("/tempPreprocessedZipDir"));
         FileUtils.deleteDirectory(zipFileDirectory);
         FileUtils.forceMkdir(zipFileDirectory);
         FileUtils.waitFor(zipFileDirectory, TIMEOUT_SECONDS);        
         Cai2Util.isValidZipFile(zipFile);
         ZipUtilities.unzip(zipFile, zipFileDirectory);
         FileUtils.waitFor(zipFileDirectory, TIMEOUT_SECONDS);
         Cai2Util.printDirContents(zipFileDirectory);
         if (zipFileDirectory.list() != null) {
             if (zipFileDirectory.list().length != 1) {
                 int dirListlength = zipFileDirectory.list().length;
                 FileUtils.deleteDirectory(zipFileDirectory);
                 throw new IllegalStateException("The zip file returned from PreprocessDataset"
                       + " should have exactly 1 file instead of " + dirListlength);
             }
         } else {
             String zipFileDirectoryPath = zipFileDirectory.getAbsolutePath();
             FileUtils.deleteDirectory(zipFileDirectory);
             throw new IllegalStateException("The zip file directory list at path: "
                     + zipFileDirectoryPath + "is null.");               
         }
         String[] files = zipFileDirectory.list();
         File preprocessedFile = new File(zipFileDirectory, files[0]);
         FileUtils.deleteQuietly(gctFile); // Remove the non-preprocessed file
         FileUtils.moveFile(preprocessedFile, gctFile); // move to gctFile
         FileUtils.deleteQuietly(zipFile);
         FileUtils.deleteDirectory(zipFileDirectory);
         return gctFile;
     }
         
     private void checkTimeout(int callCount) throws ConnectionException {
         if (callCount >= TIMEOUT_SECONDS) {
             throw new ConnectionException("Timed out trying to download preprocess dataset results");
         }
     }
     
 }
