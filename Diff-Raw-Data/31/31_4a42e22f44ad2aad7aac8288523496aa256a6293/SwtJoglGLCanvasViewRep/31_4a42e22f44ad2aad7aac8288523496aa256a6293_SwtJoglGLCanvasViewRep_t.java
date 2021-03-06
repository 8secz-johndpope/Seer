 package cerberus.view.gui.swt.jogl;
 
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.Vector;
 
 import java.util.concurrent.atomic.AtomicBoolean;
 
 import javax.media.opengl.GLAutoDrawable;
 import javax.media.opengl.GLCanvas;
 import javax.media.opengl.GLEventListener;
 
 import cerberus.manager.IGeneralManager;
 import cerberus.manager.ILoggerManager.LoggerType;
 import cerberus.manager.IViewGLCanvasManager;
 import cerberus.view.gui.swt.base.AJoglViewRep;
 import cerberus.view.gui.IView;
 import cerberus.view.gui.awt.jogl.CanvasForwarder;
 import cerberus.view.gui.opengl.IGLCanvasDirector;
 import cerberus.view.gui.opengl.IGLCanvasUser;
 import cerberus.util.exception.CerberusRuntimeException;
 
 public class SwtJoglGLCanvasViewRep 
 extends AJoglViewRep 
 implements IView, IGLCanvasDirector {
 	
 	private AtomicBoolean abEnableRendering;
 	
 	protected int iGLEventListernerId = 99000;
 	
 	protected int iGLCanvasId;
 	
 	protected GLEventListener refGLEventListener;
 	
 	protected Vector <IGLCanvasUser> vecGLCanvasUser;
 	
 	public SwtJoglGLCanvasViewRep(IGeneralManager refGeneralManager, 
 			int iViewId, 
 			int iParentContainerId, 
 			String sLabel ) {
 		
 		super(refGeneralManager, iViewId, iParentContainerId, sLabel);
 		
 		vecGLCanvasUser = new Vector <IGLCanvasUser> ();
 		
 		abEnableRendering = new AtomicBoolean( true );
 		
 		refGeneralManager.getSingelton().getViewGLCanvasManager(
 				).registerGLCanvasDirector( this, iViewId );
 	}
 	
 	/**
 	 * Attention, side effect! Call this before calling initView() !
 	 * 
 	 * @param iOpenGLCanvasId
 	 */
 	public void setOpenGLCanvasId( int iOpenGLCanvasId ) {
 		this.iGLCanvasId = iOpenGLCanvasId;		
 	}
 	
 	/**
 	 * Attension: call setOpenGLCanvasId(int) before calling this method!
 	 * 
 	 * @see cerberus.view.gui.swt.jogl.IGLCanvasDirector#initView()
 	 */
 	public void initView() {
 				
 		refGLEventListener = new CanvasForwarder( this );
 		
 		IViewGLCanvasManager canvasManager = 
 			refGeneralManager.getSingelton().getViewGLCanvasManager();
 
 		canvasManager.registerGLCanvas( refGLCanvas, iGLCanvasId );
 		canvasManager.registerGLCanvasDirector( this, iGLCanvasId);
 		
 		canvasManager.registerGLEventListener( refGLEventListener, iGLEventListernerId );
 		canvasManager.addGLEventListener2GLCanvasById( iGLEventListernerId, iGLCanvasId );
 		
 		super.setGLEventListener( refGLEventListener );
 		
 		refGeneralManager.getSingelton().getLoggerManager().logMsg(
 				"SwtJoglGLCanvasViewRep [" +
 				getId() + "] was created & initalized!",
 				LoggerType.TRANSITION );
 	}
 	
 	/* (non-Javadoc)
 	 * @see cerberus.view.gui.swt.jogl.IGLCanvasDirector#addGLCanvasUser(cerberus.view.gui.opengl.IGLCanvasUser)
 	 */
 	public void addGLCanvasUser( IGLCanvasUser user ) {
 		if ( vecGLCanvasUser.contains( user ) ) {
 			throw new CerberusRuntimeException("addGLCanvasUser() try to same user twice!");
 		}
 		vecGLCanvasUser.addElement( user );
 		
 		refGeneralManager.getSingelton().getLoggerManager().logMsg(
 				"SwtJoglGLCanvasViewRep [" +
 				getId() + "] added GLCanvas user=[" +
 				user.getId() + "] " + 
 				user.toString() ,
 				LoggerType.TRANSITION );
 	}
 	
 	/* (non-Javadoc)
 	 * @see cerberus.view.gui.swt.jogl.IGLCanvasDirector#removeGLCanvasUser(cerberus.view.gui.opengl.IGLCanvasUser)
 	 */
 	public void removeGLCanvasUser( IGLCanvasUser user ) {
 		if ( ! vecGLCanvasUser.remove( user ) ) {
 			throw new CerberusRuntimeException("removeGLCanvasUser() failed, because the user is not registered!");
 		}
 		user.destroy();
 	}
 	
 	/* (non-Javadoc)
 	 * @see cerberus.view.gui.swt.jogl.IGLCanvasDirector#removeAllGLCanvasUsers()
 	 */
 	public void removeAllGLCanvasUsers() {
 		
 		abEnableRendering.set( false );
 		
 		Iterator <IGLCanvasUser> iter = vecGLCanvasUser.iterator();
 		
 		while ( iter.hasNext() ) {		
 			IGLCanvasUser refIGLCanvasUser = iter.next();
 			refIGLCanvasUser.destroy();
 			refIGLCanvasUser = null;
 		}
 		vecGLCanvasUser.clear();
 	}
 	
 	/* (non-Javadoc)
 	 * @see cerberus.view.gui.swt.jogl.IGLCanvasDirector#getAllGLCanvasUsers()
 	 */
 	public Collection<IGLCanvasUser> getAllGLCanvasUsers() {
 		
 		return new LinkedList <IGLCanvasUser> ();
 	}	
 	
 	/* (non-Javadoc)
 	 * @see cerberus.view.gui.swt.jogl.IGLCanvasDirector#getGLCanvas()
 	 */
 	public GLCanvas getGLCanvas() {
 		return this.refGLCanvas;
 	}
 
 	/*
 	 *  (non-Javadoc)
 	 * @see cerberus.view.gui.opengl.IGLCanvasDirector#initGLCanvasUser(javax.media.opengl.GLAutoDrawable)
 	 */
 	public void initGLCanvasUser(GLAutoDrawable drawable) {
 		
 		if ( abEnableRendering.get() ) 
 		{
			System.out.println("SwtJoglCanvasViewRep.initGLCanvasUser() " + this.getClass().toString() );
			
 			Iterator <IGLCanvasUser> iter = vecGLCanvasUser.iterator();
 			
 			while ( iter.hasNext() ) {
 
 				IGLCanvasUser glCanvas = iter.next();
 				if  ( ! glCanvas.isInitGLDone() )
 				{
 					glCanvas.init( drawable );
 				}
 			}
 		}
 	}
 	
 	/*
 	 *  (non-Javadoc)
 	 * @see cerberus.view.gui.opengl.IGLCanvasDirector#renderGLCanvasUser(javax.media.opengl.GLAutoDrawable)
 	 */
 	public void renderGLCanvasUser(GLAutoDrawable drawable) {
 		
 		if ( abEnableRendering.get() ) 
 		{
 			Iterator <IGLCanvasUser> iter = vecGLCanvasUser.iterator();
 			
 			while ( iter.hasNext() ) {
 				IGLCanvasUser glCanvas = iter.next();
 				if ( glCanvas.isInitGLDone() )
 				{
 					glCanvas.render( drawable );
 				}
 			}
 		}
 	}
 	
 	public void reshapeGLCanvasUser(GLAutoDrawable drawable, 
 			final int x, final int y, 
 			final int width, final int height) {
 
 		if ( abEnableRendering.get() ) 
 		{
 			Iterator <IGLCanvasUser> iter = vecGLCanvasUser.iterator();
 			
 			while ( iter.hasNext() ) {
 				IGLCanvasUser glCanvas = iter.next();
 				if ( glCanvas.isInitGLDone() )
 				{
					glCanvas.reshape( drawable, x, y, width, height );
 				}
 			}
 		}
 	}
 	
 	/*
 	 *  (non-Javadoc)
 	 * @see cerberus.view.gui.opengl.IGLCanvasDirector#updateGLCanvasUser(javax.media.opengl.GLAutoDrawable)
 	 */
 	public void updateGLCanvasUser(GLAutoDrawable drawable) {
 		
 		if ( abEnableRendering.get() ) 
 		{
 			Iterator <IGLCanvasUser> iter = vecGLCanvasUser.iterator();
 			
 			while ( iter.hasNext() ) {
 				iter.next().update( drawable );
 			}
 		}
 	}
 	
 	public void displayGLChanged(GLAutoDrawable drawable, final boolean modeChanged, final boolean deviceChanged) {
 
 		Iterator <IGLCanvasUser> iter = vecGLCanvasUser.iterator();
 		
 		while ( iter.hasNext() ) {
 			iter.next().displayChanged(drawable, modeChanged, deviceChanged);
 		}
 		
 	}
 	
 	/*
 	 *  (non-Javadoc)
 	 * @see cerberus.view.gui.opengl.IGLCanvasDirector#destroyDirector()
 	 */
 	public void destroyDirector() {
 		
 		refGeneralManager.getSingelton().getLoggerManager().logMsg("SwtJoglCanvasViewRep.destroyDirector()  id=" +
 				iUniqueId);
 		
 		super.removeGLEventListener( refGLEventListener );
 		
 		IViewGLCanvasManager canvasManager = 
 			refGeneralManager.getSingelton().getViewGLCanvasManager();
 		
 		canvasManager.unregisterGLCanvasDirector( this );
 		canvasManager.unregisterGLCanvas( refGLCanvas );		
 		canvasManager.unregisterGLEventListener( refGLEventListener );
 		canvasManager.removeGLEventListener2GLCanvasById( iGLEventListernerId, iGLCanvasId );
 		
 		destroyOnExitViewRep();
 		
 		removeAllGLCanvasUsers();
 		
 		refGLEventListener = null;
 		
 		refGeneralManager.getSingelton().getLoggerManager().logMsg("SwtJoglCanvasViewRep.destroyDirector()  id=" +
 				iUniqueId + " ...[DONE]");
 	}
 
 	
 }
