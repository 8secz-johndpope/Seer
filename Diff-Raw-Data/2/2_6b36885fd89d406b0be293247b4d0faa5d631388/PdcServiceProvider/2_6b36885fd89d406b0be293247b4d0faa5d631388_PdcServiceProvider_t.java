 /*
  * Copyright (C) 2000 - 2011 Silverpeas
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License, or (at your option) any later version.
  *
  * As a special exception to the terms and conditions of version 3.0 of
  * the GPL, you may redistribute this Program in connection withWriter Free/Libre
  * Open Source Software ("FLOSS") applications as described in Silverpeas's
  * FLOSS exception.  You should have recieved a copy of the text describing
  * the FLOSS exception, and it is also available here:
  * "http://www.silverpeas.org/legal/licensing"
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package com.silverpeas.pdc.web;
 
 import com.silverpeas.thesaurus.control.ThesaurusManager;
 import com.stratelia.silverpeas.contentManager.ContentManager;
 import com.stratelia.silverpeas.contentManager.ContentManagerException;
 import com.stratelia.silverpeas.pdc.control.PdcBm;
 import com.stratelia.silverpeas.pdc.model.ClassifyPosition;
 import com.stratelia.silverpeas.pdc.model.PdcException;
 import com.stratelia.silverpeas.pdc.model.UsedAxis;
 import com.stratelia.silverpeas.silvertrace.SilverTrace;
 import com.stratelia.webactiv.beans.admin.UserDetail;
 import java.util.List;
 import javax.inject.Inject;
 import javax.inject.Named;
 import static com.silverpeas.pdc.web.UserThesaurusHolder.*;
 
 /**
  * A provider of services on the classification plan (named PdC). This class implements the adaptor
  * pattern by wrapping all the features about the PdC and provided by several business services so
  * that an unified and a unique access point is provided to the PdC web resources.
  * The instances of this class are managed by the IoC container and can be then injected as
  * dependency into the PdC web resources.
  */
 @Named
 public class PdcServiceProvider {
 
   @Inject
   private PdcBm pdcBm;
   @Inject
   private ThesaurusManager thesaurusManager;
   @Inject
   private ContentManager contentManager;
 
   /**
    * A convenient method to enhance the readability of method calls when a component identifier is
    * expected as argument.
    * @param componentId the identifier of a Silverpeas component instance.
    * @return the identifier.
    */
   public static String inComponentOfId(String componentId) {
     return componentId;
   }
 
   /**
    * A convenient method to enhance the readability of method calls when a resource content
    * identifier is expected as argument.
    * @param contentId the identifier of a Silverpeas resource content.
    * @return the identifier.
    */
   public static String forContentOfId(String contentId) {
     return contentId;
   }
 
   /**
    * Adds a new position of the specified resource content on the PdC configured for the specified
    * Silverpeas component instance.
    * Once added, an identifier is set for the specified position.
    * @param position the classification position to add.
    * @param contentId the identifier of the  content for which a new position is created on the PdC.
    * @param componentId the identifier of the component instance that owns the PdC instance.
    * @throws ContentManagerException if no such content or component instance exists with the
    * specified identifier.
    * @throws PdcException if the position adding fails.
    */
   public void addPosition(final ClassifyPosition position, String contentId, String componentId)
           throws ContentManagerException, PdcException {
     int silverObjectId = getSilverObjectId(contentId, componentId);
     int positionId = getPdcBm().addPosition(silverObjectId, position, componentId);
     position.setPositionId(positionId);
   }
 
   /**
    * Updates the specified position of the specified resource content on the PdC configured for the
    * specified Silverpeas component instance. The position of the content on the PdC whose the
    * identifier is the one of the specified position is replaced by the passed position.
    * @param position the classification position to update.
    * @param contentId the identifier of the  content for which the position is to update on the PdC.
    * @param componentId the identifier of the component instance that owns the PdC instance.
    * @throws ContentManagerException if no such content or component instance exists with the
    * specified identifier.
    * @throws PdcException if the position update fails.
    */
   public void updatePosition(final ClassifyPosition position, String contentId, String componentId)
           throws ContentManagerException, PdcException {
     int silverObjectId = getSilverObjectId(contentId, componentId);
    getPdcBm().updatePosition(position, componentId, silverObjectId);
   }
 
   /**
    * Deletes the specified position of the specified resource content on the PdC configured for the
    * specified component instance.
    * @param positionId the identifier of the position to delete.
    * @param componentId the identifier of the component that owns the PdC instance.
    * @throws PdcException if the position or the component identifier doesn't exist or if
    * the deletion fails.
    */
   public void deletePosition(int positionId, String contentId, String componentId) throws
           PdcException, ContentManagerException {
     List<UsedAxis> axis = getAxisUsedInPdcFor(componentId);
     List<ClassifyPosition> positions = getAllPositions(contentId, componentId);
     if (positions.size() == 1) {
       for (UsedAxis anAxis : axis) {
         if (anAxis.getMandatory() == 1) {
           throw new PdcPositionDeletionException(getClass().getSimpleName(), SilverTrace.TRACE_LEVEL_ERROR,
                   "Pdc.CANNOT_DELETE_VALUE");
         }
       }
     }
     getPdcBm().deletePosition(positionId, componentId);
   }
 
   /**
    * Gets the positions of the specified resource content on the PdC of the specified component
    * instance.
    * @param contentId the identifier of the content.
    * @param componentId the identifier of the Silverpeas component instance.
    * @return a list of classification positions of the specified content.
    * @throws ContentManagerException if no such content or component instance exists with the
    * specified identifier.
    * @throws PdcException if the position fetching fails.
    */
   public List<ClassifyPosition> getAllPositions(String contentId, String componentId) throws
           ContentManagerException, PdcException {
     int silverObjectId = getSilverObjectId(contentId, componentId);
     return getPdcBm().getPositions(silverObjectId, componentId);
   }
 
   /**
    * Gets the axis used in the PdC configured for the specified component instance in order to
    * classify the specified resource content. If the resource content is already classified, then
    * the positions of the resource content on the invariant axis are kept as the only possible value
    * on theses axis.
    * In the case no axis are specifically used for the component instance, then all the PdC axis
    * are sent back as axis that can be used to classify the specified content.
    * @param contentId the identifier of the content to classify (or to refine the classification). 
    * It is used to find its previous classification in order to fix the value of the invariant axis.
    * @param inComponentId the identifier of the component instance.
    * @return a list of used axis.
    * @throws ContentManagerException if no such content or component instance exists with the
    * specified identifier.
    * @throws PdcException if the axis cannot be fetched.
    */
   public List<UsedAxis> getAxisUsedInPdcToClassify(String contentId, String inComponentId)
           throws ContentManagerException, PdcException {
     int silverObjectId = getSilverObjectId(contentId, inComponentId);
     return getPdcBm().getUsedAxisToClassify(inComponentId, silverObjectId);
   }
 
   /**
    * Gets the axis used in the PdC configured for the specified Silverpeas component instance.
    * @param componentId the unique identifier of the component instance.
    * @return a list of axis used in the PdC configured for the component instance.
    * @throws PdcException if the axis cannot be fetched.
    */
   public List<UsedAxis> getAxisUsedInPdcFor(String componentId) throws PdcException {
     return getPdcBm().getUsedAxisToClassify(componentId, -1);
   }
 
   /**
    * Gets a holder of the thesaurus for the specified user.
    * @param user the user for which a holder will hold the thesaurus.
    * @return a UserThesaurusHolder instance.
    */
   public UserThesaurusHolder getThesaurusOfUser(final UserDetail user) {
     return UserThesaurusHolder.holdThesaurus(getThesaurusManager(), forUser(user));
   }
 
   private PdcBm getPdcBm() {
     return this.pdcBm;
   }
 
   private ContentManager getContentManager() {
     return this.contentManager;
   }
 
   private ThesaurusManager getThesaurusManager() {
     return this.thesaurusManager;
   }
 
   private int getSilverObjectId(String ofTheContent, String inTheComponent) throws
           ContentManagerException {
     return getContentManager().getSilverContentId(ofTheContent, inTheComponent);
   }
 }
