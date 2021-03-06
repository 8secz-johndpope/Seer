 /**
  * $Revision: $
  * $Date: $
  *
  * Copyright (C) 2006 Jive Software. All rights reserved.
  *
  * This software is published under the terms of the GNU Lesser Public License (LGPL),
  * a copy of which is included in this distribution.
  */
 
 package org.jivesoftware.sparkimpl.profile;
 
 import org.jivesoftware.resource.Res;
 import org.jivesoftware.spark.SparkManager;
 import org.jivesoftware.spark.util.GraphicUtils;
 import org.jivesoftware.spark.util.ResourceUtils;
 import org.jivesoftware.spark.util.SwingWorker;
 import org.jivesoftware.spark.util.URLFileSystem;
 import org.jivesoftware.spark.util.log.Log;
 
 import javax.swing.BorderFactory;
 import javax.swing.Icon;
 import javax.swing.ImageIcon;
 import javax.swing.JButton;
 import javax.swing.JLabel;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.JTextField;
import javax.swing.JDialog;
 
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.FileDialog;
 import java.awt.GridBagConstraints;
 import java.awt.GridBagLayout;
 import java.awt.Image;
 import java.awt.Insets;
import java.awt.Dialog;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.io.File;
 import java.io.FilenameFilter;
 import java.io.IOException;
 
 /**
  * UI to view/edit avatar.
  */
 public class AvatarPanel extends JPanel implements ActionListener {
     private JLabel avatar;
     private byte[] bytes;
     private File avatarFile;
     final JButton browseButton = new JButton();
     final JButton clearButton = new JButton();
     private FileDialog fileChooser;
 
    private Dialog dlg;

     /**
      * Default Constructor
      */
     public AvatarPanel() {
         setLayout(new GridBagLayout());
 
 
         final JLabel photo = new JLabel("Avatar:");
 
         avatar = new JLabel();
 
         add(photo, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
         add(avatar, new GridBagConstraints(1, 0, 1, 2, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
         add(browseButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
         add(clearButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
 
         browseButton.addActionListener(this);
 
         // Add ResourceUtils
         ResourceUtils.resButton(browseButton, Res.getString("button.browse"));
         ResourceUtils.resButton(clearButton, Res.getString("button.clear"));
 
         clearButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent actionEvent) {
                 avatar.setIcon(null);
                 bytes = null;
                 avatarFile = null;
                 avatar.setBorder(null);
             }
         });
 
         avatar.setText(Res.getString("message.no.avatar.found"));
 
         GraphicUtils.makeSameSize(browseButton, clearButton);
     }
 
     /**
      * Sets if the Avatar can be edited.
      *
      * @param editable true if edtiable.
      */
     public void setEditable(boolean editable) {
         browseButton.setVisible(editable);
         clearButton.setVisible(editable);
     }
 
     /**
      * Sets the displayable icon with the users avatar.
      *
      * @param icon the icon.
      */
     public void setAvatar(ImageIcon icon) {
         avatar.setBorder(BorderFactory.createBevelBorder(0, Color.white, Color.lightGray));
         avatar.setIcon(new ImageIcon(icon.getImage().getScaledInstance(-1, 48, Image.SCALE_SMOOTH)));
         avatar.setText("");
     }
 
     /**
      * Sets the avatar bytes.
      *
      * @param bytes the bytes.
      */
     public void setAvatarBytes(byte[] bytes) {
         this.bytes = bytes;
     }
 
     /**
      * Returns the avatars bytes.
      *
      * @return the bytes.
      */
     public byte[] getAvatarBytes() {
         return bytes;
     }
 
     /**
      * Returns the Icon representation of the Avatar.
      *
      * @return
      */
     public Icon getAvatar() {
         return avatar.getIcon();
     }
 
     /**
      * Returns the image file to use as the avatar.
      *
      * @return
      */
     public File getAvatarFile() {
         return avatarFile;
     }
 
     public void actionPerformed(ActionEvent e) {
         // init file chooser (if not already done)
         initFileChooser();
 
         fileChooser.show();
 
         if (fileChooser.getDirectory() != null && fileChooser.getFile() != null) {
             File file = new File(fileChooser.getDirectory(), fileChooser.getFile());
             String suffix = URLFileSystem.getSuffix(file);
             if (suffix.toLowerCase().equals(".jpeg") ||
                     suffix.toLowerCase().equals(".gif") ||
                     suffix.toLowerCase().equals(".png")) {
                 changeAvatar(file, this);
             }
             else {
                 JOptionPane.showMessageDialog(this, "Please choose a valid image file.", Res.getString("title.error"), JOptionPane.ERROR_MESSAGE);
             }
 
 
         }
     }
 
     private void changeAvatar(final File selectedFile, final Component parent) {
         SwingWorker worker = new SwingWorker() {
             public Object construct() {
                 try {
                     ImageIcon imageOnDisk = new ImageIcon(selectedFile.getCanonicalPath());
                     Image avatarImage = imageOnDisk.getImage();
                     if (avatarImage.getHeight(null) > 96 || avatarImage.getWidth(null) > 96) {
                         avatarImage = avatarImage.getScaledInstance(-1, 64, Image.SCALE_SMOOTH);
                     }
                     return avatarImage;
                 }
                 catch (IOException ex) {
                     Log.error(ex);
                 }
                 return null;
             }
 
             public void finished() {
                 Image avatarImage = (Image)get();
                 // Check size.
                 long length = GraphicUtils.getBytesFromImage(avatarImage).length * 8;
 
                 long k = 8192;
 
                 long actualSize = (length / k) + 1;
 
                 if (actualSize > 16) {
                     // Do not allow
                     JOptionPane.showMessageDialog(parent, Res.getString("message.image.too.large"));
                     return;
                 }
 
                 setAvatar(new ImageIcon(avatarImage));
                 avatarFile = selectedFile;
             }
         };
 
         worker.start();
     }
 
     public class ImageFilter implements FilenameFilter {
         public final String jpeg = "jpeg";
         public final String jpg = "jpg";
         public final String gif = "gif";
         public final String png = "png";
 
         //Accept all directories and all gif, jpg, tiff, or png files.
         public boolean accept(File f, String string) {
             if (f.isDirectory()) {
                 return true;
             }
 
             String extension = getExtension(f);
             if (extension != null) {
                 if (
                         extension.equals(gif) ||
                                 extension.equals(jpeg) ||
                                 extension.equals(jpg) ||
                                 extension.equals(png)
                         ) {
                     return true;
                 }
                 else {
                     return false;
                 }
             }
 
             return false;
         }
 
         /*
         * Get the extension of a file.
         */
         public String getExtension(File f) {
             String ext = null;
             String s = f.getName();
             int i = s.lastIndexOf('.');
 
             if (i > 0 && i < s.length() - 1) {
                 ext = s.substring(i + 1).toLowerCase();
             }
             return ext;
         }
 
         //The description of this filter
         public String getDescription() {
             return "*.JPEG, *.GIF, *.PNG";
         }
     }
 
     public void allowEditing(boolean allowEditing) {
         Component[] comps = getComponents();
         final int no = comps != null ? comps.length : 0;
         for (int i = 0; i < no; i++) {
             Component comp = comps[i];
             if (comp instanceof JTextField) {
                 ((JTextField)comp).setEditable(allowEditing);
             }
         }
     }
 
     public void initFileChooser() {
         if (fileChooser == null) {
            fileChooser = new FileDialog(dlg, "Choose Avatar", FileDialog.LOAD);
             fileChooser.setFilenameFilter(new ImageFilter());
         }
     }
 
    public void setParentDialog(Dialog dialog){
        this.dlg = dialog;
    }

 
 }
 
 
