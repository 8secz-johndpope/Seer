 package org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.parts;
 
 import org.eclipse.draw2d.GridData;
 import org.eclipse.draw2d.IFigure;
 import org.eclipse.draw2d.PositionConstants;
 import org.eclipse.draw2d.RoundedRectangle;
 import org.eclipse.draw2d.Shape;
 import org.eclipse.draw2d.StackLayout;
 import org.eclipse.draw2d.ToolbarLayout;
 import org.eclipse.draw2d.geometry.Dimension;
 import org.eclipse.emf.ecore.EObject;
 import org.eclipse.emf.edit.command.AddCommand;
 import org.eclipse.emf.transaction.TransactionalEditingDomain;
 import org.eclipse.gef.EditPart;
 import org.eclipse.gef.EditPolicy;
 import org.eclipse.gef.Request;
 import org.eclipse.gef.commands.Command;
 import org.eclipse.gef.editpolicies.LayoutEditPolicy;
 import org.eclipse.gef.editpolicies.NonResizableEditPolicy;
 import org.eclipse.gef.requests.CreateRequest;
 import org.eclipse.gmf.runtime.diagram.ui.editparts.AbstractBorderedShapeEditPart;
 import org.eclipse.gmf.runtime.diagram.ui.editparts.IBorderItemEditPart;
 import org.eclipse.gmf.runtime.diagram.ui.editparts.IGraphicalEditPart;
 import org.eclipse.gmf.runtime.diagram.ui.editpolicies.BorderItemSelectionEditPolicy;
 import org.eclipse.gmf.runtime.diagram.ui.editpolicies.CreationEditPolicy;
 import org.eclipse.gmf.runtime.diagram.ui.editpolicies.DragDropEditPolicy;
 import org.eclipse.gmf.runtime.diagram.ui.editpolicies.EditPolicyRoles;
 import org.eclipse.gmf.runtime.diagram.ui.figures.BorderItemLocator;
 import org.eclipse.gmf.runtime.draw2d.ui.figures.ConstrainedToolbarLayout;
 import org.eclipse.gmf.runtime.draw2d.ui.figures.WrappingLabel;
 import org.eclipse.gmf.runtime.gef.ui.figures.DefaultSizeNodeFigure;
 import org.eclipse.gmf.runtime.gef.ui.figures.NodeFigure;
 import org.eclipse.gmf.runtime.notation.View;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.graphics.Color;
 import org.wso2.developerstudio.eclipse.gmf.esb.EsbFactory;
 import org.wso2.developerstudio.eclipse.gmf.esb.EsbPackage;
 import org.wso2.developerstudio.eclipse.gmf.esb.FailoverEndPoint;
 import org.wso2.developerstudio.eclipse.gmf.esb.FailoverEndPointOutputConnector;
 import org.wso2.developerstudio.eclipse.gmf.esb.SwitchCaseBranchOutputConnector;
 import org.wso2.developerstudio.eclipse.gmf.esb.SwitchCaseContainer;
 import org.wso2.developerstudio.eclipse.gmf.esb.SwitchMediatorContainer;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.custom.AbstractMediator;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.custom.FixedBorderItemLocator;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.custom.SwitchMediatorGraphicalShape;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.policies.SwitchMediatorCanonicalEditPolicy;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.policies.SwitchMediatorItemSemanticEditPolicy;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.part.EsbVisualIDRegistry;
 
 /**
  * @generated NOT
  */
 public class SwitchMediatorEditPart extends AbstractMediator {
 
 	public IFigure defaultOutputConnector;
 	public IFigure caseOutputConnector;
 
 	/**
 	 * @generated
 	 */
 	public static final int VISUAL_ID = 3498;
 
 	/**
 	 * @generated
 	 */
 	protected IFigure contentPane;
 
 	/**
 	 * @generated
 	 */
 	protected IFigure primaryShape;
 
 	/**
 	 * @generated
 	 */
 	public SwitchMediatorEditPart(View view) {
 		super(view);
 	}
 
 	/**
 	 * @generated
 	 */
 	protected void createDefaultEditPolicies() {
 		installEditPolicy(EditPolicyRoles.CREATION_ROLE, new CreationEditPolicy());
 		super.createDefaultEditPolicies();
 		installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE, new SwitchMediatorItemSemanticEditPolicy());
 		installEditPolicy(EditPolicyRoles.DRAG_DROP_ROLE, new DragDropEditPolicy());
 		installEditPolicy(EditPolicyRoles.CANONICAL_ROLE, new SwitchMediatorCanonicalEditPolicy());
 		installEditPolicy(EditPolicy.LAYOUT_ROLE, createLayoutEditPolicy());
 		// XXX need an SCR to runtime to have another abstract superclass that would let children add reasonable editpolicies
 		// removeEditPolicy(org.eclipse.gmf.runtime.diagram.ui.editpolicies.EditPolicyRoles.CONNECTION_HANDLES_ROLE);
 	}
 
 	/**
 	 * @generated
 	 */
 	protected LayoutEditPolicy createLayoutEditPolicy() {
 		org.eclipse.gmf.runtime.diagram.ui.editpolicies.LayoutEditPolicy lep =
 		                                                                       new org.eclipse.gmf.runtime.diagram.ui.editpolicies.LayoutEditPolicy() {
 
 			                                                                       protected EditPolicy createChildEditPolicy(EditPart child) {
 				                                                                       View childView =
 				                                                                                        (View) child.getModel();
 				                                                                       switch (EsbVisualIDRegistry.getVisualID(childView)) {
 					                                                                       case SwitchMediatorInputConnectorEditPart.VISUAL_ID:
 					                                                                       case SwitchCaseBranchOutputConnectorEditPart.VISUAL_ID:
 					                                                                       case SwitchDefaultBranchOutputConnectorEditPart.VISUAL_ID:
 						                                                                       return new BorderItemSelectionEditPolicy();
 				                                                                       }
 				                                                                       EditPolicy result =
 				                                                                                           child.getEditPolicy(EditPolicy.PRIMARY_DRAG_ROLE);
 				                                                                       if (result == null) {
 					                                                                       result =
 					                                                                                new NonResizableEditPolicy();
 				                                                                       }
 				                                                                       return result;
 			                                                                       }
 
 			                                                                       protected Command getMoveChildrenCommand(Request request) {
 				                                                                       return null;
 			                                                                       }
 
 			                                                                       protected Command getCreateCommand(CreateRequest request) {
 				                                                                       return null;
 			                                                                       }
 		                                                                       };
 		return lep;
 	}
 
 	/**
 	 * @generated
 	 */
 	protected IFigure createNodeShape() {
 		return primaryShape = new SwitchMediatorFigure();
 	}
 
 	/**
 	 * @generated
 	 */
 	public SwitchMediatorFigure getPrimaryShape() {
 		return (SwitchMediatorFigure) primaryShape;
 	}
 
 	protected boolean addFixedChild(EditPart childEditPart) {
 		/*if (childEditPart instanceof LogMediatorLogCategoryEditPart) {
 			((LogMediatorLogCategoryEditPart) childEditPart)
 					.setLabel(getPrimaryShape()
 							.getFigureLogCatogeryLogPropertyLabel());
 			return true;
 		}*/
 		if (childEditPart instanceof SwitchMediatorInputConnectorEditPart) {
 			IFigure borderItemFigure =
 			                           ((SwitchMediatorInputConnectorEditPart) childEditPart).getFigure();
 			BorderItemLocator locator =
 			                            new FixedBorderItemLocator(getMainFigure(),
 			                                                       borderItemFigure,
 			                                                       PositionConstants.WEST, 0.5);
 			getBorderedFigure().getBorderItemContainer().add(borderItemFigure, locator);
 			return true;
 		}
 		if (childEditPart instanceof SwitchMediatorOutputConnectorEditPart) {
 			IFigure borderItemFigure =
 			                           ((SwitchMediatorOutputConnectorEditPart) childEditPart).getFigure();
 			BorderItemLocator locator =
 			                            new FixedBorderItemLocator(getMainFigure(),
 			                                                       borderItemFigure,
 			                                                       PositionConstants.EAST, 0.5);
 			getBorderedFigure().getBorderItemContainer().add(borderItemFigure, locator);
 			return true;
 		}
 
 		if (childEditPart instanceof SwitchDefaultBranchOutputConnectorEditPart) {
 			defaultOutputConnector =
 			                         ((SwitchDefaultBranchOutputConnectorEditPart) childEditPart).getFigure();
 		}
 		if (childEditPart instanceof SwitchCaseBranchOutputConnectorEditPart) {
 			caseOutputConnector =
			                      ((SwitchCaseBranchOutputConnectorEditPart) childEditPart).getFigure();		
			
			//refreshOutputConnector();
 		}
		System.out.println("fffffffff"+((SwitchMediatorContainerEditPart)this.getChildren().get(3)).getChildren().get(0));
 
 		return false;
 	}
	

	public void refreshOutputConnector() {
			BorderItemLocator locator =
			                            new FixedBorderItemLocator(
			                                                       (IFigure) this,
			                                                       this.caseOutputConnector,
			                                                       PositionConstants.WEST, 0.5);
			if(this.caseOutputConnector!=null){
			this.getBorderedFigure().getBorderItemContainer()
			                      .add(this.caseOutputConnector, locator);
			}
			
	
	}
 
 	protected void addChildVisual(EditPart childEditPart, int index) {
 		if (addFixedChild(childEditPart)) {
 			return;
 		}
 		super.addChildVisual(childEditPart, -1);
 	}
 
 	/**
 	 * @generated
 	 */
 	protected NodeFigure createNodePlate() {
 		DefaultSizeNodeFigure result = new DefaultSizeNodeFigure(40, 40);
 		return result;
 	}
 
 	/**
 	 * Creates figure for this edit part.
 	 * 
 	 * Body of this method does not depend on settings in generation model
 	 * so you may safely remove <i>generated</i> tag and modify it.
 	 * 
 	 * @generated
 	 */
 	protected NodeFigure createMainFigure() {
 		NodeFigure figure = createNodePlate();
 		figure.setLayoutManager(new StackLayout());
 		IFigure shape = createNodeShape();
 		figure.add(shape);
 		contentPane = setupContentPane(shape);
 		return figure;
 	}
 
 	/**
 	 * Default implementation treats passed figure as content pane.
 	 * Respects layout one may have set for generated figure.
 	 * @param nodeShape instance of generated figure class
 	 * @generated
 	 */
 	protected IFigure setupContentPane(IFigure nodeShape) {
 		if (nodeShape.getLayoutManager() == null) {
 			ConstrainedToolbarLayout layout = new ConstrainedToolbarLayout();
 			layout.setSpacing(5);
 			nodeShape.setLayoutManager(layout);
 		}
 		return nodeShape; // use nodeShape itself as contentPane
 	}
 
 	/**
 	 * @generated
 	 */
 	public IFigure getContentPane() {
 		if (contentPane != null) {
 			return contentPane;
 		}
 		return super.getContentPane();
 	}
 
 	protected IFigure getContentPaneFor(IGraphicalEditPart editPart) {
 		if (editPart instanceof IBorderItemEditPart) {
 			return getBorderedFigure().getBorderItemContainer();
 		}
 		return getContentPane();
 	}
 
 	/**
 	 * @generated
 	 */
 	protected void setForegroundColor(Color color) {
 		if (primaryShape != null) {
 			primaryShape.setForegroundColor(color);
 		}
 	}
 
 	/**
 	 * @generated
 	 */
 	protected void setBackgroundColor(Color color) {
 		if (primaryShape != null) {
 			primaryShape.setBackgroundColor(color);
 		}
 	}
 
 	/**
 	 * @generated
 	 */
 	protected void setLineWidth(int width) {
 		if (primaryShape instanceof Shape) {
 			((Shape) primaryShape).setLineWidth(width);
 		}
 	}
 
 	/**
 	 * @generated
 	 */
 	protected void setLineType(int style) {
 		if (primaryShape instanceof Shape) {
 			((Shape) primaryShape).setLineStyle(style);
 		}
 	}
 
 	/**
 	 * @generated
 	 */
 	public class SwitchMediatorFigure extends SwitchMediatorGraphicalShape {
 
 		/**
 		 * @generated
 		 */
 		private WrappingLabel fFigureSwitchMediatorPropertyValue;
 
 		/**
 		 * @generated NOT
 		 */
 		public SwitchMediatorFigure() {
 
 			ToolbarLayout layoutThis = new ToolbarLayout();
 			layoutThis.setStretchMinorAxis(true);
 			layoutThis.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
 			layoutThis.setSpacing(0);
 			layoutThis.setVertical(false);
 			this.setLayoutManager(layoutThis);
 			this.setPreferredSize(new Dimension(getMapMode().DPtoLP(250), getMapMode().DPtoLP(100)));
 			this.setOutline(true);
 			this.setBackgroundColor(THIS_BACK);
 			createContents();
 		}
 
 		public void add(IFigure figure, Object constraint, int index) {
 			if (figure instanceof DefaultSizeNodeFigure) {
 				GridData layoutData = new GridData();
 				layoutData.grabExcessHorizontalSpace = true;
 				layoutData.grabExcessVerticalSpace = true;
 				layoutData.horizontalAlignment = GridData.FILL;
 				layoutData.verticalAlignment = GridData.FILL;
 				super.add(figure, layoutData, index);
 			} else if (figure instanceof RoundedRectangle) {
 				GridData layoutData = new GridData();
 				layoutData.grabExcessHorizontalSpace = true;
 				layoutData.grabExcessVerticalSpace = true;
 				layoutData.horizontalAlignment = GridData.FILL;
 				layoutData.verticalAlignment = GridData.FILL;
 				super.add(figure, layoutData, index);
 			}
 
 			else {
 				super.add(figure, constraint, index);
 			}
 		}
 
 		/**
 		 * @generated NOT
 		 */
 		private void createContents() {
 
 			fFigureSwitchMediatorPropertyValue = new WrappingLabel();
 			fFigureSwitchMediatorPropertyValue.setText("<...>");
 			fFigureSwitchMediatorPropertyValue.setAlignment(SWT.CENTER);
 
 		}
 
 		/**
 		 * @generated
 		 */
 		public WrappingLabel getFigureSwitchMediatorPropertyValue() {
 			return fFigureSwitchMediatorPropertyValue;
 		}
 
 		public String getIconPath() {
 			return "icons/ico20/switch-mediator.gif";
 		}
 
 		public String getNodeName() {
 			return "Switch";
 		}
 
 	}
 
 	/**
 	 * @generated
 	 */
 	static final Color THIS_BACK = new Color(null, 230, 230, 230);
 
 }
