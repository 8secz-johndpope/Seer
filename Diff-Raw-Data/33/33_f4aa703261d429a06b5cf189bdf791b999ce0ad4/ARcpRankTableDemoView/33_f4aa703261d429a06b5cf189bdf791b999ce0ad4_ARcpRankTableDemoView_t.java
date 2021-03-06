 /*******************************************************************************
  * Caleydo - Visualization for Molecular Biology - http://caleydo.org
  * Copyright (c) The Caleydo Team. All rights reserved.
  * Licensed under the new BSD license, available at http://caleydo.org/license
  ******************************************************************************/
 package demo;
 
 import javax.xml.bind.annotation.XmlRootElement;
 
 import org.caleydo.core.serialize.ASerializedView;
import org.caleydo.core.view.ARcpGLElementViewPart;
 import org.caleydo.core.view.opengl.canvas.IGLCanvas;
 import org.caleydo.core.view.opengl.layout2.AGLElementView;
 import org.caleydo.core.view.opengl.layout2.GLElement;
 import org.caleydo.vis.rank.config.RankTableConfigBase;
 import org.caleydo.vis.rank.config.RankTableUIConfigs;
 import org.caleydo.vis.rank.layout.RowHeightLayouts;
 import org.caleydo.vis.rank.model.ARankColumnModel;
 import org.caleydo.vis.rank.model.RankTableModel;
 import org.caleydo.vis.rank.ui.RankTableKeyListener;
 import org.caleydo.vis.rank.ui.RankTableUI;
 import org.caleydo.vis.rank.ui.RankTableUIMouseKeyListener;
 
 import demo.RankTableDemo.IModelBuilder;
 
 /**
  * @author Samuel Gratzl
  *
  */
public abstract class ARcpRankTableDemoView extends ARcpGLElementViewPart {
 	public ARcpRankTableDemoView() {
 		super(SView.class);
 	}
 
 	@Override
	protected AGLElementView createView(IGLCanvas canvas) {
		return new GLView(canvas, getViewGUIID(), getViewGUIID());
 	}
 
 	/**
	 * Returns the rcp-ID of the view
	 *
	 * @return rcp-ID of the view
	 */
	public abstract String getViewGUIID();

	/**
 	 * @return
 	 */
 	public abstract RankTableDemo.IModelBuilder createModel();
 
 	class GLView extends AGLElementView {
 		protected final RankTableModel table;
 
 		public GLView(IGLCanvas glCanvas, String viewType, String viewName) {
 			super(glCanvas, viewType, viewName);
 			final IModelBuilder builder = createModel();
 			this.table = new RankTableModel(new RankTableConfigBase() {
 				@Override
 				public Iterable<? extends ARankColumnModel> createAutoSnapshotColumns(RankTableModel table,
 						ARankColumnModel model) {
 					return builder.createAutoSnapshotColumns(table, model);
 				}
 			});
 
 			try {
 				canvas.addKeyListener(new RankTableKeyListener(table));
 				builder.apply(table);
 			} catch (Exception e1) {
 				// TODO Auto-generated catch block
 				e1.printStackTrace();
 			}
 		}
 
 		@Override
 		public ASerializedView getSerializableRepresentation() {
 			return new SView();
 		}
 
 		@Override
 		protected GLElement createRoot() {
 			RankTableUI root = new RankTableUI();
 			root.init(table, RankTableUIConfigs.DEFAULT, RowHeightLayouts.UNIFORM, RowHeightLayouts.FISH_EYE);
 
 			RankTableUIMouseKeyListener l = new RankTableUIMouseKeyListener(root.findBody());
 			this.canvas.addMouseListener(l);
 			canvas.addKeyListener(l);
 			return root;
 		}
 	}
 
 
 	@XmlRootElement
 	public static class SView extends ASerializedView {
 		@Override
 		public String getViewType() {
 			return "";
 		}
 	}
 
 }
