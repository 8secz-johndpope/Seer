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
 package gov.nih.nci.caintegrator2.application.kmplot;
 
 import gov.nih.nci.caintegrator.plots.kaplanmeier.dto.KMCriteriaDTO;
 import gov.nih.nci.caintegrator.plots.kaplanmeier.dto.KMSampleDTO;
 import gov.nih.nci.caintegrator.plots.kaplanmeier.dto.KMSampleGroupCriteriaDTO;
 
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 
 import org.jfree.chart.JFreeChart;
 
 /**
  * Implementation of KMPlotter that directly uses original caIntegrator KMPlot code.
  */
 public class KMPlotServiceCaIntegratorImpl implements KMPlotService {
     
     private gov.nih.nci.caintegrator.plots.services.KMPlotService caIntegratorPlotService;
 
     /**
      * {@inheritDoc}
      */
     public KMPlot generatePlot(KMPlotConfiguration configuration) {
         KMPlotImpl plot = new KMPlotImpl();
         Map<SubjectGroup, KMSampleGroupCriteriaDTO> groupToDTOMap = 
             new HashMap<SubjectGroup, KMSampleGroupCriteriaDTO>();
         KMCriteriaDTO kmCriteriaDTO = createCriteriaDTO(configuration, groupToDTOMap);
         plot.setPlotChart((JFreeChart) caIntegratorPlotService.getChart(kmCriteriaDTO));
         setPValues(plot, groupToDTOMap);
         return plot;
     }
 
     private void setPValues(KMPlotImpl plot, Map<SubjectGroup, KMSampleGroupCriteriaDTO> groupToDTOMap) {
         for (SubjectGroup group1 : groupToDTOMap.keySet()) {
             for (SubjectGroup group2 : groupToDTOMap.keySet()) {
                 if (!group1.equals(group2)) {
                     Double pValue = caIntegratorPlotService.computeLogRankPValueBetween(groupToDTOMap.get(group1), 
                             groupToDTOMap.get(group2));
                     plot.setPValue(group1, group2, pValue);
                 }
             }
         }
     }
 
     private KMCriteriaDTO createCriteriaDTO(KMPlotConfiguration configuration, 
             Map<SubjectGroup, KMSampleGroupCriteriaDTO> groupToDTOMap) {
         KMCriteriaDTO criteriaDTO = new KMCriteriaDTO();
         criteriaDTO.setDurationAxisLabel(configuration.getDurationLabel());
         criteriaDTO.setProbablityAxisLabel(configuration.getProbabilityLabel());
         criteriaDTO.setPlotTitle(configuration.getTitle());
         criteriaDTO.setSampleGroupCriteriaDTOCollection(new HashSet<KMSampleGroupCriteriaDTO>());
         for (SubjectGroup group : configuration.getGroups()) {
             KMSampleGroupCriteriaDTO sampleGroupCriteriaDTO = createSampleGroupCriteriaDTO(group);
             criteriaDTO.getSampleGroupCriteriaDTOCollection().add(sampleGroupCriteriaDTO);
             groupToDTOMap.put(group, sampleGroupCriteriaDTO);
         }
         return criteriaDTO;
     }
 
     private KMSampleGroupCriteriaDTO createSampleGroupCriteriaDTO(SubjectGroup group) {
         KMSampleGroupCriteriaDTO groupCriteriaDTO = new KMSampleGroupCriteriaDTO();
         groupCriteriaDTO.setColor(group.getColor());
         groupCriteriaDTO.setSampleGroupName(group.getName());
         groupCriteriaDTO.setKmSampleDTOCollection(new HashSet<KMSampleDTO>());
         for (SubjectSurvivalData survivalData : group.getSurvivalData()) {
             groupCriteriaDTO.getKmSampleDTOCollection().add(createSampleDTO(survivalData));
         }
         return groupCriteriaDTO;
     }
 
     private KMSampleDTO createSampleDTO(SubjectSurvivalData survivalData) {
        return new KMSampleDTO(survivalData.getSurvivalLength(), survivalData.isCensor());
     }
 
     /**
      * @return the caIntegratorPlotService
      */
     public gov.nih.nci.caintegrator.plots.services.KMPlotService getCaIntegratorPlotService() {
         return caIntegratorPlotService;
     }
 
     /**
      * @param caIntegratorPlotService the caIntegratorPlotService to set
      */
     public void setCaIntegratorPlotService(gov.nih.nci.caintegrator.plots.services.KMPlotService 
             caIntegratorPlotService) {
         this.caIntegratorPlotService = caIntegratorPlotService;
     }
 
 }
