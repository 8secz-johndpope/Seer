 /*
 This file is part of ewuCalligraphy.
 
     Foobar is free software: you can redistribute it and/or modify
     it under the terms of the GNU General Public License as published by
     the Free Software Foundation, either version 3 of the License, or
     (at your option) any later version.
 
     ewuCalligraphy is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.
 
     You should have received a copy of the GNU General Public License
     along with ewuCalligraph.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package ewucalligraphy.gui;
 
 import java.awt.Graphics;
 import java.awt.Image;
 import java.awt.image.BufferedImage;
 import java.io.File;
 import javax.imageio.ImageIO;
 import javax.swing.JFileChooser;
 import javax.swing.JOptionPane;
 import javax.swing.filechooser.FileNameExtensionFilter;
 
 /**
  *
  * @author dave
  */
 
 public class MainWindow extends javax.swing.JFrame {
 	private static final long serialVersionUID = 1L;
 
 	private AboutWindow windowAbout;
 	private JFileChooser windowFileChooser;
 	private BufferedImage fileImage;
 	private String fileName;
 	private int[] imageSize = new int[2];
 	/**
 	 * Creates new form MainWindow
 	 */
 	public MainWindow()
 	{
 		initComponents();
 	}
 
 	public void start()
 	{
 		this.setVisible(true);
 		windowAbout = new AboutWindow();
 
 		windowFileChooser = new JFileChooser();
 		FileNameExtensionFilter fileFilterJpeg;
 		fileFilterJpeg = new FileNameExtensionFilter("JPEG Images", "jpg", "jpeg");
 		windowFileChooser.setFileFilter(fileFilterJpeg);
 
 	}
 
 
 	/**
 	 * This method is called from within the constructor to initialize the form.
 	 * WARNING: Do NOT modify this code. The content of this method is always
 	 * regenerated by the Form Editor.
 	 */
 	@SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents()
     {
 
         jMenuBar1 = new javax.swing.JMenuBar();
         jMenuFile = new javax.swing.JMenu();
         jMenuFileOpen = new javax.swing.JMenuItem();
         jMenuFileExit = new javax.swing.JMenuItem();
         jMenuHelp = new javax.swing.JMenu();
         jMenuHelpAbout = new javax.swing.JMenuItem();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
         setTitle("EwuCalligraphy");
         setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
         setMinimumSize(new java.awt.Dimension(50, 100));
         setName("ewuCalligraphy"); // NOI18N
 
         jMenuFile.setText("File");
 
         jMenuFileOpen.setText("Open");
         jMenuFileOpen.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 jMenuFileOpenActionPerformed(evt);
             }
         });
         jMenuFile.add(jMenuFileOpen);
 
         jMenuFileExit.setText("Exit");
         jMenuFileExit.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 jMenuFileExitActionPerformed(evt);
             }
         });
         jMenuFile.add(jMenuFileExit);
 
         jMenuBar1.add(jMenuFile);
 
         jMenuHelp.setText("Help");
 
         jMenuHelpAbout.setText("About");
         jMenuHelpAbout.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 jMenuHelpAboutActionPerformed(evt);
             }
         });
         jMenuHelp.add(jMenuHelpAbout);
 
         jMenuBar1.add(jMenuHelp);
 
         setJMenuBar(jMenuBar1);
 
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGap(0, 400, Short.MAX_VALUE)
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGap(0, 279, Short.MAX_VALUE)
         );
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
 
     private void jMenuFileExitActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuFileExitActionPerformed
     {//GEN-HEADEREND:event_jMenuFileExitActionPerformed
 		windowAbout.dispose();
 		this.dispose();
     }//GEN-LAST:event_jMenuFileExitActionPerformed
 
     private void jMenuHelpAboutActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuHelpAboutActionPerformed
     {//GEN-HEADEREND:event_jMenuHelpAboutActionPerformed
 		windowAbout.setVisible(true);
     }//GEN-LAST:event_jMenuHelpAboutActionPerformed
 
     private void jMenuFileOpenActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuFileOpenActionPerformed
     {//GEN-HEADEREND:event_jMenuFileOpenActionPerformed
 		int returnVal = windowFileChooser.showOpenDialog(this);
 		if(returnVal == JFileChooser.APPROVE_OPTION)
 		{
 			File selectedFile = windowFileChooser.getSelectedFile();
 			if(selectedFile.canRead() && selectedFile.isFile())
 			{
 				fileName = selectedFile.getName();
 				try
 				{
 					fileImage = ImageIO.read(selectedFile);
 					imageSize[0] = fileImage.getHeight();
 					imageSize[1] = fileImage.getWidth();
 					this.repaint(); //Calls paint(Graphics g);
 
 				}
 				catch(Exception e)
 				{
 					System.out.println("Image Opening Failed");
					JOptionPane.showMessageDialog(null, "Image Opening Failed", "Error", JOptionPane.ERROR_MESSAGE);
 
 				}
 
 			}
 		}
     }//GEN-LAST:event_jMenuFileOpenActionPerformed
 
 	private int oldWindowSize[] = new int[2];
 	private int newWindowSize[] = new int[2];
 
 	private final int edgeOffset = 10;
 	private final int topOffset = 45;
 	private boolean drawed = false;
 
 	@Override
 	public void paint(Graphics g)
 	{
 		super.paint(g);
 
 		//TODO: Might be a good idea to change image resizing to ...
 		//		java.awt.image.ReplicateScaleFilter
 
 		if(fileImage != null)
 		{
 			newWindowSize[0] = this.getHeight();
 			newWindowSize[1] = this.getWidth();
 
 			boolean windowChanged = (newWindowSize[0] != oldWindowSize[0]) ||
 									(newWindowSize[1] != oldWindowSize[1]);
 
 			if(windowChanged || !drawed)
 			{
 				oldWindowSize[0] = newWindowSize[0];
 				oldWindowSize[1] = newWindowSize[1];
 
 				int windowRatio = newWindowSize[0] * imageSize[1];
 				int picRatio    = imageSize[0] * newWindowSize[1];
 
 				int newImageSizeWidth, newImageSizeLength;
 
 				newImageSizeWidth = 0; newImageSizeLength = 0;
 
 				if(windowRatio < picRatio)
 				{
 					//window not long enough
 					newImageSizeLength = newWindowSize[0] - edgeOffset - topOffset;
 					newImageSizeWidth = (newImageSizeLength * imageSize[1]) / imageSize[0];
 				}
 				else
 				{
 					//window not wide enough
 					newImageSizeWidth = newWindowSize[1] - edgeOffset * 2;
 					newImageSizeLength = (newImageSizeWidth * imageSize[0]) / imageSize[1];
 				}
 
 
 
 
 				if((newImageSizeWidth > 0 && newImageSizeLength > 0) || !drawed)
 				{
 					Image scaledImage = fileImage.getScaledInstance(newImageSizeWidth, newImageSizeLength, Image.SCALE_DEFAULT);
 					drawed = g.drawImage(scaledImage, edgeOffset, topOffset, newImageSizeWidth, newImageSizeLength, null);
 				}
 			}
 		}
 	}
 
 	/**
 	 * @param args the command line arguments
 	 */
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JMenuBar jMenuBar1;
     private javax.swing.JMenu jMenuFile;
     private javax.swing.JMenuItem jMenuFileExit;
     private javax.swing.JMenuItem jMenuFileOpen;
     private javax.swing.JMenu jMenuHelp;
     private javax.swing.JMenuItem jMenuHelpAbout;
     // End of variables declaration//GEN-END:variables
 }
