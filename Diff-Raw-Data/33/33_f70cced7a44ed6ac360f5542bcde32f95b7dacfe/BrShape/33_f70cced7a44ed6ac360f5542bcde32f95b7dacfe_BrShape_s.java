 /*
  * brevis is free software: you can redistribute it and/or modify                                                                                                           
     it under the terms of the GNU General Public License as published by                                                                                                             
     the Free Software Foundation, either version 3 of the License, or                                                                                                                
     (at your option) any later version.                                                                                                                                              
                                                                                                                                                                                      
     brevis is distributed in the hope that it will be useful,                                                                                                                
     but WITHOUT ANY WARRANTY; without even the implied warranty of                                                                                                                   
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                                                                                                                    
     GNU General Public License for more details.                                                                                                                                     
                                                                                                                                                                                      
     You should have received a copy of the GNU General Public License                                                                                                                
     along with brevis.  If not, see <http://www.gnu.org/licenses/>.                                                                                                          
                                                                                                                                                                                      
 	Copyright 2012, 2013 Kyle Harrington
  */
 
 package brevis;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileReader;
 import java.io.InputStreamReader;
 import java.net.URL;
 import java.net.URLClassLoader;
 import java.nio.FloatBuffer;
 import java.nio.IntBuffer;
 
 import javax.vecmath.Vector3d;
 
 import org.lwjgl.BufferUtils;
 import org.lwjgl.opengl.ARBVertexBufferObject;
 import org.lwjgl.opengl.GL15;
 import org.lwjgl.opengl.GLContext;
 import org.lwjgl.util.vector.Vector3f;
 import org.ode4j.ode.DGeom;
 import org.ode4j.ode.DMass;
 import org.ode4j.ode.DSpace;
 import org.ode4j.ode.DTriMeshData;
 import org.ode4j.ode.OdeHelper;
 
 import brevis.graphics.BrMesh;
 
 public class BrShape {
 	public enum BrShapeType {
 		BOX, SPHERE, CONE, CYLINDER, MESH,
 		// Unit meshes for optimized rendering
 		UNIT_CONE, UNIT_SPHERE, //FLOOR
 		ICOSAHEDRON, PRISM
 	};
 	
 	static public String objDir = "obj" + File.separator;
 	
 	public BrShapeType type;
 	//public Vector3d dim;
 	public Vector3f dim;
 	public int vertBID = -1;
 	public int colBID = -1;
 	public int idxBID = -1;
 	public int numIdx = 0;
 	public BrMesh mesh = null;
 	
 	// Make final?
 	public static BrMesh unitCone = null;	
 	public static BrMesh unitSphere = null;	
 	
 	public void resize( Vector3f newDim ) {
 		dim = newDim;
 		// should reload shoul
 	}
 	
 	public BrMesh getMesh() {
 		return mesh;
 	}
 	
 	public String toString() {
 		String s = "#BrShape{ :type " + type + ", :dim" + dim +
 				", :mesh " + mesh + "}";		 				
 		return s;
 	}
 	
 	BrShape( BrShapeType t, Vector3f d ) {
 		//type = BrShapeType.SPHERE;
 		//dim = new Vector3d(1,1,1);
 		type = t;
 		dim = d;
 		
 		if( type == BrShapeType.UNIT_CONE ) {
 			if( unitCone == null ) {
 				initUnitCone();				
 			}
 			mesh = unitCone;
 			//dim = new Vector3d( mesh.getXWidth(), mesh.getYHeight(), mesh.getZDepth() );
 			dim = new Vector3f( 1, 1, 1 );
 			//System.out.println( dim );
 		} else if( type == BrShapeType.UNIT_SPHERE) {
 			if( unitSphere== null ) {
 				initUnitSphere();				
 			}
 			mesh = unitSphere;
 			//dim = new Vector3d( mesh.getXWidth(), mesh.getYHeight(), mesh.getZDepth() );
 			dim = new Vector3f( 1, 1, 1 );
 			//System.out.println( dim );
 		} else {
 			createMesh();
 			if( mesh != null ) {
 				mesh.rescaleMesh( (float)dim.x, (float)dim.y, (float)dim.z );
 			}
 		}
 				
 	}
 	
 	BrShape( String filename, boolean isResource ) {
 		type = BrShapeType.MESH;
 		loadMesh( filename, isResource );
 	}
 	
 	public void initUnitCone() {		
 		String filename = objDir + "cone.obj";
 	
 		try {		
 			BufferedReader br = new BufferedReader( new InputStreamReader( ClassLoader.getSystemResource( filename ).openStream() ) );
 			unitCone = new BrMesh( br, true );
 		} catch( Exception e ) {
 			e.printStackTrace();
 		}	
 	}
 	
 	public void initUnitSphere() {		
 		String filename = objDir + "sphere.obj";
 	
 		try {		
 			BufferedReader br = new BufferedReader( new InputStreamReader( ClassLoader.getSystemResource( filename ).openStream() ) );
 			unitSphere = new BrMesh( br, true );
 		} catch( Exception e ) {
 			e.printStackTrace();
 		}	
 	}
 	
 	public void draw() {
 		
 	}
 	
 	public String getType() {
 		if( type == BrShapeType.BOX ) {
 			return "box";
 		} else if( type == BrShapeType.SPHERE  ||  type == BrShapeType.UNIT_SPHERE ) {
 			return "sphere";
 		} else if( type == BrShapeType.CONE || type == BrShapeType.UNIT_CONE ) {
 			return "cone";			
 		} else if( type == BrShapeType.CYLINDER ) {
 			return "cylinder";
 		} else if( type == BrShapeType.ICOSAHEDRON ) {
 			return "icosahedron";
 		} else if( type == BrShapeType.PRISM ) {
 			return "prism";
 		} else if( type == BrShapeType.MESH ) {
 			return "mesh";
 		} else {
 			return "unknown";
 		}
 	}
 	
 	/*
 	 * Return a mass that is appropriate for this object and its dimensions
 	 */
 	public DMass createMass( double density ) {
 		DMass m = OdeHelper.createMass();
 		if( type == BrShapeType.BOX ) {
 			m.setBox(density, dim.x, dim.y, dim.z );
 		} else if( type == BrShapeType.SPHERE || type == BrShapeType.UNIT_SPHERE || type == BrShapeType.ICOSAHEDRON || type == BrShapeType.PRISM ) {
 			m.setSphere( density, dim.x );
 		} else if( type == BrShapeType.CONE || type == BrShapeType.UNIT_CONE ) {
 			m.setSphere(density, dim.x);
 		} else if( type == BrShapeType.CYLINDER ) {
 			m.setSphere(density, dim.x);
 		} else if( type == BrShapeType.MESH ) {
 			m.setSphere(density, 1 );
 		}
 		return m;
 	}
 	
 	/*
 	 * VBO code, currently nonfunctional
 	 * 
 	  public static int createVBOID() {
 		  if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
 		    IntBuffer buffer = BufferUtils.createIntBuffer(1);
 		    ARBVertexBufferObject.glGenBuffersARB(buffer);
 		    return buffer.get(0);
 		  }
 		  return 0;
 		}*/
 	
 	/*
 	public static int createVBOID() {
 	    //IntBuffer buffer = BufferUtils.createIntBuffer(1);
 	    //GL15.glGenBuffers(buffer);
 	    //return buffer.get(0);
 	    //Or alternatively you can simply use the convenience method:
 	    return GL15.glGenBuffers(); //Which can only supply you with a single id.
 	}
 
 
 	public static void vertexBufferData(int id, FloatBuffer buffer) { //Not restricted to FloatBuffer
 	    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, id); //Bind buffer (also specifies type of buffer)
 	    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW); //Send up the data and specify usage hint.
 	}
 	public static void indexBufferData(int id, IntBuffer buffer) { //Not restricted to IntBuffer
 	    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, id);
 	    GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
 	}
 
 	public void createVBOFromMesh( ) {
 		vertBID = createVBOID();
 		colBID = createVBOID();
 		idxBID = createVBOID();
 		numIdx = mesh.numIdx();
 		
 		vertexBufferData( vertBID, FloatBuffer.wrap( mesh.verts ) );
 		vertexBufferData( colBID, FloatBuffer.wrap( mesh.col ) );
 		indexBufferData( idxBID, IntBuffer.wrap( mesh .idx ) );
 		
 	}
 	
 	public void createMesh() {
 		mesh = new BrMesh();
 		
 		if( type == BrShapeType.BOX ) {
 			mesh.initBox( dim );			
 		} else if( type == BrShapeType.SPHERE ) {
 			mesh.initSphere( dim );
 		} else if( type == BrShapeType.CONE ) {
 			mesh.initCone( dim );
 		} else if( type == BrShapeType.CYLINDER ) {
 			mesh.initCylinder( dim );
 		}
 	}
 	
 	public static void bufferData(int id, FloatBuffer buffer) {
 		  if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
 		    ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, id);
 		    ARBVertexBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, buffer, ARBVertexBufferObject.GL_STATIC_DRAW_ARB);
 		  }
 		}
 */
 	
 	/*public void createMesh() {		
 		String filename  = "";
 		if( type == BrShapeType.BOX ) {
 			//mesh.initBox( dim );
 			filename = "box.obj";
 		} else if( type == BrShapeType.SPHERE ) {
 			//mesh.initSphere( dim );
 			filename = "sphere.obj";
 		} else if( type == BrShapeType.CONE ) {
 			//mesh.initCone( dim );
 			filename = "cone.obj";
 		} else if( type == BrShapeType.CYLINDER ) {
 			//mesh.initCylinder( dim );
 			filename = "cylinder.obj";
 		}
 		filename = objDir + filename;
 		
 		//System.out.println( "createMesh " + filename + " " + type );
 		loadMesh( filename, true );
 	}*/
 	
 	
 	public void createMesh() {		
 		String filename  = "";
 		if( type == BrShapeType.BOX ) {
 			//initBox( dim );
 			filename = "box.obj";
 		} else if( type == BrShapeType.SPHERE ) {
 			//mesh.initSphere( dim );
 			filename = "sphere.obj";
 		} else if( type == BrShapeType.CONE ) {
 			//mesh.initCone( dim );
 			filename = "cone.obj";
 		} else if( type == BrShapeType.CYLINDER ) {
 			//mesh.initCylinder( dim );
 			filename = "cylinder.obj";
 		} else if( type == BrShapeType.ICOSAHEDRON ) {
 			//mesh.initCylinder( dim );
 			filename = "icosahedron.obj";
 		} else if( type == BrShapeType.PRISM) {
 			//mesh.initCylinder( dim );
 			filename = "prism.obj";
 		}
 		filename = objDir + filename;
 		
 		//System.out.println( "createMesh " + filename + " " + type );
 		loadMesh( filename, true );
 	}
 	
 	public void loadMesh( String filename, boolean isResource ) {
 		try {
 			//System.out.println( "Loading object: " + filename );			
 			
 			if( isResource ) {			
 			//FileReader fr = new FileReader(filename);		
 				BufferedReader br = new BufferedReader( new InputStreamReader( ClassLoader.getSystemResource( filename ).openStream() ) );
 				//mesh = new BrMesh( br, false );
 				mesh = new BrMesh( br, true );
 			} else {				
 				BufferedReader br = new BufferedReader( new FileReader( filename ) );
 				//mesh = new BrMesh( br, false );
 				mesh = new BrMesh( br, true );
 			}
 						
 			// this is actually size
 			//dim = new Vector3d( mesh.getXWidth(), mesh.getYHeight(), mesh.getZDepth() );
 			
 			// this is being used for scale
 			dim = new Vector3f( 1, 1, 1 );
 			
 			//mesh.opengldrawtolist();
 		} catch( Exception e ) {
 			e.printStackTrace();
 		}
 	}		
 	
 	public DGeom createGeom( DSpace space ) {
 		DGeom g;
 		
 		if( mesh != null ) {
 			DTriMeshData new_tmdata = OdeHelper.createTriMeshData();
 			//System.out.println( "createGeom " + type );
 			new_tmdata.build( mesh.trimeshVertices( new float[]{ (float) dim.x, (float) dim.y, (float) dim.z } ), mesh.trimeshIndices() );
 			
 			g = OdeHelper.createTriMesh(space, new_tmdata, null, null, null);
 			
 			
 			//g.getBody().
 			
 			return g;
 		} 
 		
 		// Should be where primitive shapes are
 		switch( type ) {
 		case BOX:
 			return OdeHelper.createBox( space, dim.x, dim.y, dim.z );		
 		default:
 		case SPHERE:
 			return OdeHelper.createSphere( space, 1 );			
 		}		
 	}
 	
 	public void setDimension( Vector3f newd ) {
 		dim = newd;
 		if( mesh != null ) {
 			mesh.rescaleMesh( (float)newd.x, (float)newd.y, (float)newd.z );
 			//System.out.println( "rescaling " + newd );
 		}
 	}
 	
 	public Vector3f getDimension() {
 		return dim;
 	}
 	
 	public static BrShape createMeshFromFile( String filename, boolean isResource ) {
 		//System.out.println( filename );
 		return ( new BrShape( filename, isResource ) );
 	}
 	
 	public static BrShape createSphere( double r ) {
 		return ( new BrShape( BrShapeType.SPHERE, new Vector3f( (float)r, (float)r, (float)r ) ) );
 		//return ( new BrShape( BrShapeType.UNIT_SPHERE, new Vector3d( r, r, r )));	
 	}
 	
 	public static BrShape createIcosahedron( double r ) {
 		return ( new BrShape( BrShapeType.ICOSAHEDRON, new Vector3f( (float)r, (float)r, (float)r ) ) );
 		//return ( new BrShape( BrShapeType.UNIT_SPHERE, new Vector3d( r, r, r )));	
 	}
 	
 	public static BrShape createBox( double x, double y, double z ) {
 		return ( new BrShape( BrShapeType.BOX, new Vector3f( (float)x, (float)y, (float)z ) ) );
 	}
 	
 	public static BrShape createCone( double length, double base ) {
 		//return ( new BrShape( BrShapeType.CONE, new Vector3d( length, base, 25 )));	// last element of vector is # of sides or stacks (depending on renderer)
 		//return ( new BrShape( BrShapeType.UNIT_CONE, new Vector3f( (float)length, (float)base, (float)25 )));	// last element of vector is # of sides or stacks (depending on renderer)
 		return ( new BrShape( BrShapeType.CONE, new Vector3f( (float)length, (float)base, (float)25 )));	// last element of vector is # of sides or stacks (depending on renderer)
 	}
 	
 	public static BrShape createCylinder( double length, double radius ) {
 		return ( new BrShape( BrShapeType.CYLINDER, new Vector3f( (float)length, (float)radius, (float)25 )));	// last element of vector is # of sides or stacks (depending on renderer)
 	}
 }
