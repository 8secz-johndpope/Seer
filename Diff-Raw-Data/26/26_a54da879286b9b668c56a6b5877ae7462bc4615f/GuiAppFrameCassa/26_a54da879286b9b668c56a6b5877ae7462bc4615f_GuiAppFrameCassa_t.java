 /*
  * GuiAppFrameCassa.java
  * 
  * Copyright (C) 2009 Nicola Roberto Viganò
  * 
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 /*
  * GuiAppFrameCassa.java
  *
  * Created on 21-mag-2009, 19.54.50
  */
 
 package gestionecassa.clients.cassa.gui;
 
 import gestionecassa.clients.gui.GuiAppFrame;
 import gestionecassa.clients.gui.GuiHelper;
 import gestionecassa.clients.gui.GuiOkCancelDialog;
 import gestionecassa.clients.gui.GuiPreferencesPanel;
 import gestionecassa.clients.cassa.CassaAPI;
 import gestionecassa.clients.cassa.CassaPrefs;
 
 /**
  *
  * @author ben
  */
 public class GuiAppFrameCassa extends GuiAppFrame<CassaAPI> {
 
     /**
      * 
      */
     GuiStatusCassaPanel statusPanel;
 
     /**
      * Creates new form GuiAppFrameCassa
      *
      * @param owner
      */
     public GuiAppFrameCassa(CassaAPI owner) {
         super(owner);
         initComponents();
 
         statusPanel = new GuiStatusCassaPanel(owner.getHostname());
 
         GuiHelper.MngBorderLayout.init(getContentPane());
         GuiHelper.MngBorderLayout.putTop(getContentPane(), toolbar);
         GuiHelper.MngBorderLayout.putCenter(getContentPane(), jScrollPanelMain);
         GuiHelper.MngBorderLayout.putRight(getContentPane(), statusPanel);
 
         GuiHelper.packAndCenter(this);
     }
 
     /** This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {
 
     setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
 
     pack();
   }// </editor-fold>//GEN-END:initComponents
 
   // Variables declaration - do not modify//GEN-BEGIN:variables
   // End of variables declaration//GEN-END:variables
 
     /**
      * 
      */
     @Override
     public void selectedDialogOptions() {
         new GuiOkCancelDialog(this, "Client Options",
                  new GuiPreferencesPanel<CassaPrefs>(owner)).setVisible(true);
     }
 
     /**
      *
      * @param cassaAPI
      * @param username
      */
     public void setupAfterLogin(CassaAPI cassaAPI, String username) {
         this.enableLogout(true);
         GuiNewOrderPanel orderPanel = new GuiNewOrderPanel(cassaAPI,this);
         this.setContentPanel(orderPanel);
         statusPanel.setOrderPanel(orderPanel);
         statusPanel.setLogin(username);
     }
 
     /**
      *
      */
     @Override
     public void setdownAfterLogout() {
         super.setdownAfterLogout();
         statusPanel.setOrderPanel(null);
         statusPanel.reset();
     }
 
     /**
      * 
      * @return
      */
     public GuiStatusCassaPanel getStatusPanel() {
         return statusPanel;
     }
 }
