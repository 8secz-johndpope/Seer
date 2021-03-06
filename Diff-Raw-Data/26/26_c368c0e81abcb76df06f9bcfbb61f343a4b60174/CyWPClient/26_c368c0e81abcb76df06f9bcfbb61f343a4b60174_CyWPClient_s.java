 package org.wikipathways.cytoscapeapp.internal.webclient;
 
 import org.cytoscape.work.TaskIterator;
 
 import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
 import org.cytoscape.io.webservice.SearchWebServiceClient;
 import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;
 
 import java.util.List;
 import java.io.InputStream;
 
 import javax.swing.ListSelectionModel;
 import javax.swing.JPanel;
 import javax.swing.JTextField;
 import javax.swing.JCheckBox;
 import javax.swing.JComboBox;
 import javax.swing.JTable;
 import javax.swing.table.AbstractTableModel;
 import javax.swing.JButton;
 import javax.swing.JScrollPane;
 import java.awt.GridBagLayout;
 import java.awt.event.ItemEvent;
 import java.awt.event.ItemListener;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.MouseEvent;
 import java.awt.event.MouseAdapter;
 
 import org.cytoscape.work.Task;
 import org.cytoscape.work.TaskMonitor;
 import org.cytoscape.work.TaskIterator;
 
 import org.cytoscape.io.read.CyNetworkReader;
 
 import org.wikipathways.cytoscapeapp.internal.CyActivator;
 import org.wikipathways.cytoscapeapp.internal.webclient.WPClient.PathwayRef;
 
 public class CyWPClient extends AbstractWebServiceGUIClient implements NetworkImportWebServiceClient, SearchWebServiceClient {
   static final String[] RESULTS_TABLE_COLUMN_NAMES = {"Pathway Name", "Species"};
   final JTextField searchField = new JTextField();
   final JCheckBox speciesCheckBox = new JCheckBox("Only: ");
   final JComboBox speciesComboBox = new JComboBox();
   final JButton searchButton = new JButton("Search");
   final PathwayRefsTableModel tableModel = new PathwayRefsTableModel();
   final JTable resultsTable = new JTable(tableModel);
   WPClient client;
 
  public CyWPClient() {
     super("http://www.wikipathways.org", "WikiPathways", "WikiPathways");
 
     try {
       client = new WPClient();
     } catch (Exception e) {
       // TODO: log this exception
       client = null;
       return;
     }
 
     speciesCheckBox.addItemListener(new ItemListener() {
       public void itemStateChanged(ItemEvent e) {
         speciesComboBox.setEnabled(speciesCheckBox.isSelected());
       }
     });
 
     speciesCheckBox.setSelected(false);
     speciesComboBox.setEnabled(false);
 
     final ActionListener performSearch = new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         CyActivator.taskMgr.execute(new TaskIterator(new PerformSearch()));
       }
     };
     searchButton.addActionListener(performSearch);
     searchField.addActionListener(performSearch);
 
     resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
     resultsTable.addMouseListener(new MouseAdapter() {
       public void mouseClicked(MouseEvent e) {
         if (e.getClickCount() != 2)
           return;
         final TaskIterator taskIterator = new TaskIterator();
         taskIterator.append(new LoadPathway(taskIterator));
         CyActivator.taskMgr.execute(taskIterator);
       }
     });
 
     super.gui = new JPanel(new GridBagLayout());
     EasyGBC c = new EasyGBC();
     super.gui.add(searchField, c.expandHoriz().insets(0, 10, 5, 0));
     super.gui.add(speciesCheckBox, c.noExpand().right().insets(0, 5, 5, 0));
     super.gui.add(speciesComboBox, c.right().insets(0, 0, 5, 5));
     super.gui.add(searchButton, c.right().insets(0, 0, 5, 10));
     super.gui.add(new JScrollPane(resultsTable), c.down().expandBoth().spanHoriz(4).insets(0, 10, 10, 10));
 
     CyActivator.taskMgr.execute(new TaskIterator(new PopulateSpecies()));
   }
 
   public TaskIterator createTaskIterator(Object query) {
     return new TaskIterator();
   }
 
   abstract class InterruptableTask implements Task {
     public abstract void interruptableRun(TaskMonitor monitor) throws Exception;
 
     protected Thread thread = null;
     public void run(TaskMonitor monitor) throws Exception {
       thread = Thread.currentThread();
       interruptableRun(monitor);
       thread = null;
     }
 
     public void cancel() {
       if (thread != null)
         thread.interrupt();
     }
   }
 
   class PopulateSpecies extends InterruptableTask {
     public void interruptableRun(TaskMonitor monitor) throws Exception {
       monitor.setTitle("Obtain list of species from WikiPathways");
       for (final String species : client.getSpecies())
         speciesComboBox.addItem(species);
     }
   }
 
   class PerformSearch extends InterruptableTask {
     public void interruptableRun(TaskMonitor monitor) throws Exception {
       final String query = searchField.getText();
       if (query == null || query.length() == 0)
         return;
       monitor.setTitle(String.format("Search WikiPathways for '%s'", query));
 
       searchButton.setEnabled(false);
       searchButton.setText("Searching...");
 
       List<PathwayRef> pathwayRefs = null;
       if (speciesCheckBox.isSelected()) {
         final String species = (String) speciesComboBox.getSelectedItem();
         pathwayRefs = client.freeTextSearch(query, species);
       } else {
         pathwayRefs = client.freeTextSearch(query);
       }
 
       tableModel.setPathwayRefs(pathwayRefs);
 
       searchButton.setEnabled(true);
       searchButton.setText("Search");
     }
   }
 
   class LoadPathway extends InterruptableTask {
     final TaskIterator taskIterator;
     public LoadPathway(TaskIterator taskIterator) {
       this.taskIterator = taskIterator;
     }
 
     public void interruptableRun(TaskMonitor monitor) throws Exception {
       final PathwayRef pathwayRef = tableModel.getSelectedPathwayRef();
       if (pathwayRef == null)
         return;
       monitor.setTitle("Opening from WikiPathways: " + pathwayRef.getName());
       final InputStream gpmlStream = client.loadPathway(pathwayRef);
      final CyNetworkReader netReader = CyActivator.netReaderMgr.getReader(gpmlStream, pathwayRef.getName() + ".gpml");
      if (netReader == null)
        throw new Exception("Could not load GPML file");
      taskIterator.append(netReader);
     }
   }
 
   class PathwayRefsTableModel extends AbstractTableModel {
     List<PathwayRef> pathwayRefs = null;
 
     public void setPathwayRefs(List<PathwayRef> pathwayRefs) {
       this.pathwayRefs = pathwayRefs;
       super.fireTableDataChanged();
     }
 
     public int getRowCount() {
       return (pathwayRefs == null) ? 0 : pathwayRefs.size();
     }
 
     public int getColumnCount() {
       return 2;
     }
 
     public Object getValueAt(int row, int col) {
       final PathwayRef pathwayRef = pathwayRefs.get(row);
       switch(col) {
         case 0: return pathwayRef.getName();
         case 1: return pathwayRef.getSpecies();
         default: return null;
       }
     }
 
     public String getColumnName(int col) {
       switch(col) {
         case 0: return "Pathway Name";
         case 1: return "Species";
         default: return null;
       }
     }
 
     public boolean isCellEditable(int row, int col) {
       return false;
     }
 
     public PathwayRef getSelectedPathwayRef() {
       final int row = resultsTable.getSelectedRow();
       if (row < 0)
         return null;
       return pathwayRefs.get(row);
     }
   }
 }
