 package com.sun.electric.database.topology;
 
 import com.sun.electric.database.ImmutableNodeInst;
 import com.sun.electric.database.geometry.Orientation;
 import com.sun.electric.database.hierarchy.Cell;
 import com.sun.electric.database.prototype.NodeProto;
 import com.sun.electric.database.text.Name;
 import com.sun.electric.database.variable.ImmutableTextDescriptor;
 import com.sun.electric.database.variable.Variable;
 import com.sun.electric.database.variable.TextDescriptor;
 
 import java.awt.geom.Point2D;
 
 
 /**
  * Created by IntelliJ IDEA.
  * User: gg151869
  * Date: Jul 25, 2005
  * Time: 5:43:34 PM
  * To change this template use File | Settings | File Templates.
  */
 public class IconNodeInst extends NodeInst
 {
 	/**
 	 * The constructor of IconNodeInst. Use the factory "newInstance" instead.
      * @param d persistent data of this IconNodeInst.
 	 * @param parent the Cell in which this IconNodeInst will reside.
 	 */
     IconNodeInst(ImmutableNodeInst d, Cell parent) {
         super(d, parent);
     }
     
     public Variable newVar(Variable.Key key, Object value, TextDescriptor td)
     {
         Variable var = super.newVar(key, value, td);
         parent.newVar(key, value, td);
         return var;
     }
 
     /**
      * Rename a Variable. Note that this creates a new variable of
      * the new name and copies all values from the old variable, and
      * then deletes the old variable.
      * @param name the name of the var to rename
      * @param newName the new name of the variable
      * @return the new renamed variable
      */
     public Variable renameVar(String name, String newName) {
         parent.renameVar(name, newName);
         return (super.renameVar(name, newName));
     }
 
     /**
 	 * Method to update a Variable on this ElectricObject with the specified values.
 	 * If the Variable already exists, only the value is changed; the displayable attributes are preserved.
 	 * @param name the name of the Variable.
 	 * @param value the object to store in the Variable.
 	 * @return the Variable that has been updated.
 	 */
 	public Variable updateVar(String name, Object value) {
         parent.updateVar(name, value);
        return updateVar(Variable.newKey(name), value);
     }
 
     /**
 	 * Overwriting NodeInst.setTextDescriptor for handling icons.
 	 * @param varName name of variable or special name.
 	 * @param td new value TextDescriptor
 	 */
 //	public void setTextDescriptor(String varName, TextDescriptor td)
 //    {
 //        // td is cloned inside setTextDescriptor
 //        parent.setTextDescriptor(varName, td);
 //        super.setTextDescriptor(varName, td);
 //    }
 
 //    public void setVar(Variable.Key key, Object value, int index)
 //    {
 //        System.out.println("Overwrite setVar");
 //    }
 }
