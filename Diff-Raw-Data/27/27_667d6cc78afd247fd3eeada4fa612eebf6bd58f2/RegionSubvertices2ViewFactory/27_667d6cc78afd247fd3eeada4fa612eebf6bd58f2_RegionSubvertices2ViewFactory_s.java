 package org.eclipse.uml2.diagram.statemachine.view.factories;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.eclipse.core.runtime.IAdaptable;
 
 import org.eclipse.emf.ecore.EAnnotation;
 import org.eclipse.emf.ecore.EcoreFactory;
 
 import org.eclipse.gmf.runtime.diagram.ui.view.factories.ListCompartmentViewFactory;
 
 import org.eclipse.gmf.runtime.notation.DrawerStyle;
 import org.eclipse.gmf.runtime.notation.NotationFactory;
 import org.eclipse.gmf.runtime.notation.NotationPackage;
 import org.eclipse.gmf.runtime.notation.TitleStyle;
 import org.eclipse.gmf.runtime.notation.View;
 
 import org.eclipse.uml2.diagram.statemachine.edit.parts.RegionSubvertices2EditPart;
 import org.eclipse.uml2.diagram.statemachine.edit.parts.StateMachineEditPart;
 
 import org.eclipse.uml2.diagram.statemachine.part.UMLVisualIDRegistry;
 
 /**
  * @generated
  */
 public class RegionSubvertices2ViewFactory extends ListCompartmentViewFactory {
 
 	/**
 	 * @generated 
 	 */
 	protected List createStyles(View view) {
 		List styles = new ArrayList();
		styles.add(NotationFactory.eINSTANCE.createDrawerStyle());
 		styles.add(NotationFactory.eINSTANCE.createSortingStyle());
 		styles.add(NotationFactory.eINSTANCE.createFilteringStyle());
 		return styles;
 	}
 
 	/**
 	 * @generated
 	 */
 	protected void decorateView(View containerView, View view, IAdaptable semanticAdapter, String semanticHint, int index, boolean persisted) {
 		if (semanticHint == null) {
 			semanticHint = UMLVisualIDRegistry.getType(RegionSubvertices2EditPart.VISUAL_ID);
 			view.setType(semanticHint);
 		}
 		super.decorateView(containerView, view, semanticAdapter, semanticHint, index, persisted);
 		setupCompartmentTitle(view);
 		setupCompartmentCollapsed(view);
 		if (!StateMachineEditPart.MODEL_ID.equals(UMLVisualIDRegistry.getModelID(containerView))) {
 			EAnnotation shortcutAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
 			shortcutAnnotation.setSource("Shortcut"); //$NON-NLS-1$
 			shortcutAnnotation.getDetails().put("modelID", StateMachineEditPart.MODEL_ID); //$NON-NLS-1$
 			view.getEAnnotations().add(shortcutAnnotation);
 		}
 	}
 
 	/**
 	 * @generated
 	 */
 	protected void setupCompartmentTitle(View view) {
 		TitleStyle titleStyle = (TitleStyle) view.getStyle(NotationPackage.eINSTANCE.getTitleStyle());
 		if (titleStyle != null) {
 			titleStyle.setShowTitle(true);
 		}
 	}
 
 	/**
 	 * @generated
 	 */
 	protected void setupCompartmentCollapsed(View view) {
 		DrawerStyle drawerStyle = (DrawerStyle) view.getStyle(NotationPackage.eINSTANCE.getDrawerStyle());
 		if (drawerStyle != null) {
 			drawerStyle.setCollapsed(false);
 		}
 	}
 
 }
