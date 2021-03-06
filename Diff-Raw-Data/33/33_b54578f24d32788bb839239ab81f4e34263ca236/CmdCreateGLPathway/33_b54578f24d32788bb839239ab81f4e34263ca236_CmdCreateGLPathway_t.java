 package org.caleydo.core.command.view.opengl;
 
 import java.util.ArrayList;
 import org.caleydo.core.command.ECommandType;
 import org.caleydo.core.data.view.camera.EProjectionMode;
 import org.caleydo.core.parser.parameter.IParameterHandler;
 import org.caleydo.core.util.system.StringConversionTool;
 import org.caleydo.core.view.opengl.canvas.pathway.GLPathway;
 
 /**
  * Create single OpenGL pathway view.
  * 
  * @author Marc Streit
  */
 public class CmdCreateGLPathway
 	extends CmdCreateGLEventListener
 {
 
 	private int iPathwayID = -1;
 
 	/**
 	 * Constructor.
 	 */
 	public CmdCreateGLPathway(final ECommandType cmdType)
 	{
 		super(cmdType);
 	}
 
 	@Override
 	public void setParameterHandler(final IParameterHandler parameterHandler)
 	{
 		super.setParameterHandler(parameterHandler);
 
 		iPathwayID = StringConversionTool.convertStringToInt(sAttribute4, -1);
 	}
 
 	public void setAttributes(final int iPathwayID, final ArrayList<Integer> iArSetIDs,
 			final EProjectionMode eProjectionMode, final float fLeft, final float fRight,
 			final float fTop, final float fBottom, final float fNear, final float fFar)
 	{
		super.setAttributes(eProjectionMode, fLeft, fRight, fBottom, fTop, fNear, fFar,
 				iArSetIDs, -1);
 
 		this.iAlSetIDs = iArSetIDs;
 		this.iPathwayID = iPathwayID;
 		this.iExternalID = iUniqueID;
 		iParentContainerId = -1;
 	}
 
 	@Override
 	public final void doCommand()
 	{
 		super.doCommand();
 
 		((GLPathway) createdObject).setPathwayID(iPathwayID);
 	}
 }
