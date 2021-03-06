 /**
  * Project Tie-Break, EASV (2nd Semester, 2013)
  *
  * @author Kasper Pedersen, Jesper Agerbo Hansen,
  * @author Mads Funch Patrzalek Reese and Jakob Hansen.
  *
  * Code stored at: https://github.com/MadsReese/projecttiebreak
  */
 package GUI;
 
 import BE.Member;
 import BLL.MemberManager;
 import com.microsoft.sqlserver.jdbc.SQLServerException;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.sql.SQLException;
 import java.util.Calendar;
 import java.util.List;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.swing.DefaultListModel;
 import javax.swing.JOptionPane;
 
 /**
  * This is the main-class for the MemberGui-system.
  *
  * @author Jakob Hansen
  */
 public class MemberGui extends javax.swing.JFrame
 {
 //  instance fields.
     private DefaultListModel model = new DefaultListModel();
     private DefaultListModel model2 = new DefaultListModel();
     private MemberManager mM;
     private int switchLimitation = Integer.MAX_VALUE;
     private int switchType = 0;
 
     /**
      * Initializes the main member GUI.
      */
     public MemberGui() throws SQLServerException, SQLException, FileNotFoundException, IOException
     {
         this.setTitle("Tie-Break Tennis Club");
         initComponents();
         mM = MemberManager.getInstance();
         lstResults.setModel(model);
         lstDetails.setModel(model2);
     }
 
     /**
      * Main-method for starting the program from this class.
      *
      * @param args command-line arguments.
      * @throws SQLServerException
      * @throws SQLException
      * @throws FileNotFoundException
      * @throws IOException
      */
     public static void main(String[] args) throws SQLServerException, SQLException, FileNotFoundException, IOException
     {
         new MemberGui().setVisible(true);
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
 
         txtBoxQuery = new javax.swing.JTextField();
         lblQuery = new javax.swing.JLabel();
         pnlResultsAndDetails = new javax.swing.JPanel();
         scrPnlResults = new javax.swing.JScrollPane();
         lstResults = new javax.swing.JList();
         scrPnlDetails = new javax.swing.JScrollPane();
         lstDetails = new javax.swing.JList();
         btnClose = new javax.swing.JButton();
         lblCount = new javax.swing.JLabel();
         btnAddMember = new javax.swing.JButton();
         btnRemoveMember = new javax.swing.JButton();
         btnEdit = new javax.swing.JButton();
         pnlSearch = new javax.swing.JPanel();
         lblSearchFor = new javax.swing.JLabel();
         cmbBoxSearchType = new javax.swing.JComboBox();
         pnlLimit = new javax.swing.JPanel();
         lblLimit = new javax.swing.JLabel();
         cmbBoxLimit = new javax.swing.JComboBox();
         btnSearch = new javax.swing.JButton();
         jMenuBar = new javax.swing.JMenuBar();
         File = new javax.swing.JMenu();
         Settings = new javax.swing.JMenu();
         Options = new javax.swing.JMenuItem();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
         setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
         setMaximumSize(new java.awt.Dimension(460, 499));
         setMinimumSize(new java.awt.Dimension(460, 499));
         setResizable(false);
 
         txtBoxQuery.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 txtBoxQueryActionPerformed(evt);
             }
         });
 
         lblQuery.setText("Query");
 
         pnlResultsAndDetails.setBorder(javax.swing.BorderFactory.createTitledBorder("Results and Details"));
         pnlResultsAndDetails.setMaximumSize(new java.awt.Dimension(440, 353));
         pnlResultsAndDetails.setMinimumSize(new java.awt.Dimension(440, 353));
         pnlResultsAndDetails.setName("test"); // NOI18N
 
         lstResults.setModel(new javax.swing.AbstractListModel()
         {
             String[] strings = { "model" };
             public int getSize() { return strings.length; }
             public Object getElementAt(int i) { return strings[i]; }
         });
         lstResults.setMaximumSize(new java.awt.Dimension(200, 278));
         lstResults.setMinimumSize(new java.awt.Dimension(200, 278));
         lstResults.addListSelectionListener(new javax.swing.event.ListSelectionListener()
         {
             public void valueChanged(javax.swing.event.ListSelectionEvent evt)
             {
                 lstResultsValueChanged(evt);
             }
         });
         scrPnlResults.setViewportView(lstResults);
 
         lstDetails.setModel(new javax.swing.AbstractListModel()
         {
             String[] strings = { "model2" };
             public int getSize() { return strings.length; }
             public Object getElementAt(int i) { return strings[i]; }
         });
         lstDetails.setMaximumSize(new java.awt.Dimension(200, 278));
         lstDetails.setMinimumSize(new java.awt.Dimension(200, 278));
         scrPnlDetails.setViewportView(lstDetails);
 
         btnClose.setText("Close");
         btnClose.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 btnCloseActionPerformed(evt);
             }
         });
 
         lblCount.setText("Count: ");
 
         btnAddMember.setText("Add...");
         btnAddMember.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 btnAddMemberActionPerformed(evt);
             }
         });
 
         btnRemoveMember.setText("Remove");
         btnRemoveMember.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 btnRemoveMemberActionPerformed(evt);
             }
         });
 
         btnEdit.setText("Edit...");
         btnEdit.addMouseListener(new java.awt.event.MouseAdapter()
         {
             public void mouseClicked(java.awt.event.MouseEvent evt)
             {
                 btnEditMouseClicked(evt);
             }
         });
         btnEdit.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 btnEditActionPerformed(evt);
             }
         });
 
         javax.swing.GroupLayout pnlResultsAndDetailsLayout = new javax.swing.GroupLayout(pnlResultsAndDetails);
         pnlResultsAndDetails.setLayout(pnlResultsAndDetailsLayout);
         pnlResultsAndDetailsLayout.setHorizontalGroup(
             pnlResultsAndDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(pnlResultsAndDetailsLayout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(pnlResultsAndDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(pnlResultsAndDetailsLayout.createSequentialGroup()
                         .addComponent(btnAddMember)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                         .addComponent(btnEdit)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                         .addComponent(btnRemoveMember)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                         .addComponent(btnClose))
                     .addGroup(pnlResultsAndDetailsLayout.createSequentialGroup()
                         .addComponent(lblCount)
                         .addGap(0, 0, Short.MAX_VALUE))
                     .addGroup(pnlResultsAndDetailsLayout.createSequentialGroup()
                         .addComponent(scrPnlResults, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                         .addGap(18, 18, 18)
                         .addComponent(scrPnlDetails)))
                 .addContainerGap())
         );
         pnlResultsAndDetailsLayout.setVerticalGroup(
             pnlResultsAndDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(pnlResultsAndDetailsLayout.createSequentialGroup()
                 .addGap(6, 6, 6)
                 .addComponent(lblCount, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                 .addGroup(pnlResultsAndDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(scrPnlDetails)
                     .addComponent(scrPnlResults, javax.swing.GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE))
                 .addGap(18, 18, 18)
                 .addGroup(pnlResultsAndDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(btnAddMember)
                     .addComponent(btnEdit)
                     .addComponent(btnRemoveMember)
                     .addComponent(btnClose))
                 .addContainerGap())
         );
 
         pnlSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Options"));
         pnlSearch.setPreferredSize(new java.awt.Dimension(226, 158));
 
         lblSearchFor.setText("Search for...");
 
         cmbBoxSearchType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Full Name", "Member No." }));
         cmbBoxSearchType.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 cmbBoxSearchTypeActionPerformed(evt);
             }
         });
 
         javax.swing.GroupLayout pnlSearchLayout = new javax.swing.GroupLayout(pnlSearch);
         pnlSearch.setLayout(pnlSearchLayout);
         pnlSearchLayout.setHorizontalGroup(
             pnlSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(pnlSearchLayout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(pnlSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(pnlSearchLayout.createSequentialGroup()
                         .addComponent(lblSearchFor)
                         .addGap(0, 0, Short.MAX_VALUE))
                     .addComponent(cmbBoxSearchType, 0, 213, Short.MAX_VALUE))
                 .addContainerGap())
         );
         pnlSearchLayout.setVerticalGroup(
             pnlSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(pnlSearchLayout.createSequentialGroup()
                 .addComponent(lblSearchFor)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addComponent(cmbBoxSearchType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addContainerGap(25, Short.MAX_VALUE))
         );
 
         pnlLimit.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Limit"));
         pnlLimit.setAutoscrolls(true);
 
         lblLimit.setText("Limit to...");
 
         cmbBoxLimit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "10", "20", "50", "100" }));
         cmbBoxLimit.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 cmbBoxLimitActionPerformed(evt);
             }
         });
 
         javax.swing.GroupLayout pnlLimitLayout = new javax.swing.GroupLayout(pnlLimit);
         pnlLimit.setLayout(pnlLimitLayout);
         pnlLimitLayout.setHorizontalGroup(
             pnlLimitLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(pnlLimitLayout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(pnlLimitLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(cmbBoxLimit, 0, 215, Short.MAX_VALUE)
                     .addGroup(pnlLimitLayout.createSequentialGroup()
                         .addComponent(lblLimit)
                         .addGap(0, 0, Short.MAX_VALUE)))
                 .addContainerGap())
         );
         pnlLimitLayout.setVerticalGroup(
             pnlLimitLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(pnlLimitLayout.createSequentialGroup()
                 .addComponent(lblLimit)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addComponent(cmbBoxLimit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addContainerGap(25, Short.MAX_VALUE))
         );
 
         btnSearch.setText("Search");
         btnSearch.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 btnSearchActionPerformed(evt);
             }
         });
 
         File.setText("File");
         jMenuBar.add(File);
 
         Settings.setText("Settings");
 
         Options.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
         Options.setText("Options");
         Options.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 OptionsActionPerformed(evt);
             }
         });
         Settings.add(Options);
 
         jMenuBar.add(Settings);
 
         setJMenuBar(jMenuBar);
 
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(pnlResultsAndDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                         .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                             .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                 .addComponent(lblQuery)
                                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                 .addComponent(txtBoxQuery))
                             .addComponent(pnlSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE))
                         .addGap(18, 18, 18)
                         .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                             .addComponent(pnlLimit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                             .addComponent(btnSearch))))
                 .addContainerGap())
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                     .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                         .addComponent(txtBoxQuery)
                         .addComponent(btnSearch))
                     .addComponent(lblQuery, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                     .addComponent(pnlLimit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addComponent(pnlSearch, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE))
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                 .addComponent(pnlResultsAndDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addContainerGap())
         );
 
         pnlResultsAndDetails.getAccessibleContext().setAccessibleName("");
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
 
     /**
      * Action upon pressing "close".
      *
      * @param evt
      */
     private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
         System.exit(0);
     }//GEN-LAST:event_btnCloseActionPerformed
 
     /**
      * Action upon switching the limitation.
      *
      * @param evt
      */
     private void cmbBoxLimitActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cmbBoxLimitActionPerformed
     {//GEN-HEADEREND:event_cmbBoxLimitActionPerformed
         switch (cmbBoxLimit.getSelectedIndex())
         {
             case 0:
                 switchLimitation = Integer.MAX_VALUE;
                 break;
             case 1:
                 switchLimitation = 10;
                 break;
             case 2:
                 switchLimitation = 20;
                 break;
             case 3:
                 switchLimitation = 50;
                 break;
             case 4:
                 switchLimitation = 100;
                 break;
         }
         
         btnSearch.doClick();
     }//GEN-LAST:event_cmbBoxLimitActionPerformed
 
     /**
      * Action upon pressing "search".
      *
      * @param evt
      */
     private void btnSearchActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnSearchActionPerformed
     {//GEN-HEADEREND:event_btnSearchActionPerformed
         if (switchType == 0)
         {
             searchByName();
         }
         else
         {
             searchByMemberNo();
         }
     }//GEN-LAST:event_btnSearchActionPerformed
 
     /**
      * Action upon changing search-types.
      *
      * @param evt
      */
     private void cmbBoxSearchTypeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cmbBoxSearchTypeActionPerformed
     {//GEN-HEADEREND:event_cmbBoxSearchTypeActionPerformed
         switch (cmbBoxSearchType.getSelectedIndex())
         {
             case 0:
                 switchType = 0;
                 break;
             case 1:
                 switchType = 1;
                 break;
         }
     }//GEN-LAST:event_cmbBoxSearchTypeActionPerformed
 
     /**
      * Action upon pressing the "Add..." button. (launches a new "NewMemberGui"
      * frame!)
      *
      * @param evt
      */
     private void btnAddMemberActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnAddMemberActionPerformed
     {//GEN-HEADEREND:event_btnAddMemberActionPerformed
         new NewMemberGui().setVisible(true);
     }//GEN-LAST:event_btnAddMemberActionPerformed
 
     private void txtBoxQueryActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_txtBoxQueryActionPerformed
     {//GEN-HEADEREND:event_txtBoxQueryActionPerformed
         btnSearch.doClick();
     }//GEN-LAST:event_txtBoxQueryActionPerformed
 
     private void lstResultsValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_lstResultsValueChanged
     {//GEN-HEADEREND:event_lstResultsValueChanged
         model2.clear();
         Member m = (Member)lstResults.getSelectedValue();
        if(m == null){
            
        } else {
         model2.addElement("Member No: " + m.getMemberNo());
         model2.addElement("Name: " + m.getFirstName() + " " + m.getLastName());
         Calendar c = Calendar.getInstance();
         int age = c.get(Calendar.YEAR) - m.getBirthYear();
         model2.addElement("Age: " + age + " - " + m.getMemberType());
         model2.addElement("Address: " + m.getAddress());
         model2.addElement("Email: " + m.getEmail());
         model2.addElement("Telephone: " + m.getPhoneNo());
        }
     }//GEN-LAST:event_lstResultsValueChanged
 
     private void btnRemoveMemberActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnRemoveMemberActionPerformed
     {//GEN-HEADEREND:event_btnRemoveMemberActionPerformed
         Member m = (Member)lstResults.getSelectedValue();
         //lstResults.clearSelection();
         System.out.println("Derp");
         model.removeElement(m);
         try
         {
             mM.delete(m);
         } 
         catch (Exception ex)
         {
             ex.printStackTrace();
         }
         if (switchType == 0)
         {
             searchByName();
         }
         else
         {
             searchByMemberNo();
         }
     }//GEN-LAST:event_btnRemoveMemberActionPerformed
 
     private void OptionsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_OptionsActionPerformed
     {//GEN-HEADEREND:event_OptionsActionPerformed
         new OptionsGui().setVisible(true);
     }//GEN-LAST:event_OptionsActionPerformed
 
     private void btnEditMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_btnEditMouseClicked
     {//GEN-HEADEREND:event_btnEditMouseClicked
         Member m = (Member)lstResults.getSelectedValue();
         System.out.println("DEBUG: Sent member with #" + m.getMemberNo() + " to EditMemberGui.");;
         //lstResults.clearSelection();
         new EditMemberGui(m).setVisible(true);
     }//GEN-LAST:event_btnEditMouseClicked
 
     private void btnEditActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnEditActionPerformed
     {//GEN-HEADEREND:event_btnEditActionPerformed
       
     }//GEN-LAST:event_btnEditActionPerformed
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JMenu File;
     private javax.swing.JMenuItem Options;
     private javax.swing.JMenu Settings;
     private javax.swing.JButton btnAddMember;
     private javax.swing.JButton btnClose;
     private javax.swing.JButton btnEdit;
     private javax.swing.JButton btnRemoveMember;
     private javax.swing.JButton btnSearch;
     private javax.swing.JComboBox cmbBoxLimit;
     private javax.swing.JComboBox cmbBoxSearchType;
     private javax.swing.JMenuBar jMenuBar;
     private javax.swing.JLabel lblCount;
     private javax.swing.JLabel lblLimit;
     private javax.swing.JLabel lblQuery;
     private javax.swing.JLabel lblSearchFor;
     private javax.swing.JList lstDetails;
     private javax.swing.JList lstResults;
     private javax.swing.JPanel pnlLimit;
     private javax.swing.JPanel pnlResultsAndDetails;
     private javax.swing.JPanel pnlSearch;
     private javax.swing.JScrollPane scrPnlDetails;
     private javax.swing.JScrollPane scrPnlResults;
     private javax.swing.JTextField txtBoxQuery;
     // End of variables declaration//GEN-END:variables
 
     /**
      * Method for searching for members by their names.
      */
     private void searchByName()
     {
         model.clear();
         String query = txtBoxQuery.getText();
         List<Member> resultSet = mM.getByName(query.toLowerCase());
         resultSet = resultSet.subList(0, Math.min(resultSet.size(), switchLimitation));
         if (!resultSet.isEmpty())
         {
             for (Member m : resultSet)
             {
                 model.addElement(m);
             }
             lblCount.setText("Count: " + resultSet.size());
         } else
         {
             lblCount.setText("No results.");
         }
     }
 
     /**
      * Method for searching for members by their member-numbers. 
      */
     private void searchByMemberNo()
    {        
         model.clear();
         String query = txtBoxQuery.getText();
         List<Member> resultSet = mM.getByMemberNo(query);
         if (!resultSet.isEmpty())
         {
             for(Member m : resultSet)
             {
                 model.addElement(m);
             }
             lblCount.setText("Count: " + resultSet.size());
         } 
         else
         {
             lblCount.setText("No results.");
         }
     }
 }
