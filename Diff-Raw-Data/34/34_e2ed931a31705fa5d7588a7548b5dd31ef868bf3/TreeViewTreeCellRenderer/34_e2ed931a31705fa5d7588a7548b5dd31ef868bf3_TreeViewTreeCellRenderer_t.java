 /*
  * Copyright (c) 2006-2007 Chris Smith, Shane Mc Cormack, Gregory Holmes
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 
 package uk.org.ownage.dmdirc.ui.framemanager.tree;
 
 import java.awt.Color;
 import java.awt.Component;
import java.awt.Font;
 import java.net.URL;
 import javax.swing.ImageIcon;
 import javax.swing.JTree;
 import javax.swing.border.EmptyBorder;
 import javax.swing.border.LineBorder;
 import javax.swing.tree.DefaultMutableTreeNode;
 import javax.swing.tree.DefaultTreeCellRenderer;
 import uk.org.ownage.dmdirc.FrameContainer;
 import uk.org.ownage.dmdirc.ui.MainFrame;
 
 /**
  * Displays a node in a tree according to its type
  */
 public class TreeViewTreeCellRenderer extends DefaultTreeCellRenderer {
     
     /**
      * A version number for this class. It should be changed whenever the class
      * structure is changed (or anything else that would prevent serialized
      * objects being unserialized with the new class).
      */
     private static final long serialVersionUID = 1;
     
     /**
      * default icon
      */
     ImageIcon defaultIcon;
     
     /**
      * Creates a new instance of TreeViewTreeCellRenderer
      */
     public TreeViewTreeCellRenderer() {
         ClassLoader cldr = this.getClass().getClassLoader();
         URL imageURL = cldr.getResource("uk/org/ownage/dmdirc/res/icon.png");
         defaultIcon = new ImageIcon(imageURL);
     }
     
     /**
      * Configures the renderer based on the passed parameters
      * @param tree JTree for this renderer
      * @param value node to be renderered
      * @param sel whether the node is selected
      * @param expanded whether the node is expanded
      * @param leaf whether the node is a leaf
      * @param row the node's row
      * @param hasFocus whether the node has focus
      * @return RendererComponent for this node
      */
     public Component getTreeCellRendererComponent(JTree tree, Object value,
             boolean sel, boolean expanded, boolean leaf, int row,
             boolean hasFocus) {
         
         Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
                 row, hasFocus);
         
         DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
         
         TreeFrameManager manager = null;
         
         if (MainFrame.hasMainFrame()) {
             manager = (TreeFrameManager)MainFrame.getMainFrame().getFrameManager();
         }
         
         c.setBackground(tree.getBackground());
         c.setForeground(tree.getForeground());
         
         setOpaque(true);
         if (node.getUserObject() instanceof FrameContainer && manager != null) {
             Color colour = manager.getNodeColour((FrameContainer)node.getUserObject());
             if (colour != null) {
                 c.setForeground(colour);
             }
             setIcon(((FrameContainer)node.getUserObject()).getIcon());
         } else {
             
             setIcon(defaultIcon);
         }
         
         if (manager != null && node.getUserObject().equals(manager.getSelected())) {
            setFont(getFont().deriveFont(Font.BOLD));
             setBorder(new LineBorder(Color.BLACK));
         } else {
            setFont(getFont().deriveFont(Font.PLAIN));
         }
        setBorder(new EmptyBorder(1, 0, 2, 0));
         
         setToolTipText(null);
         
         return this;
     }
 }
