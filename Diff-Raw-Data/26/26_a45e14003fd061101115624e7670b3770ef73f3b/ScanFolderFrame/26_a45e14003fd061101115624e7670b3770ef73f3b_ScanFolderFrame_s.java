 package de.jClipCorn.gui.frames.scanFolderFrame;
 
 import java.awt.BorderLayout;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.MouseEvent;
 import java.awt.event.MouseListener;
 import java.io.File;
 import java.io.FileFilter;
 import java.util.ArrayList;
 
 import javax.swing.DefaultListModel;
 import javax.swing.JButton;
 import javax.swing.JCheckBox;
 import javax.swing.JFileChooser;
 import javax.swing.JFrame;
 import javax.swing.JList;
 import javax.swing.JPanel;
 import javax.swing.JProgressBar;
 import javax.swing.JScrollPane;
 import javax.swing.SwingUtilities;
 
 import com.jgoodies.forms.factories.FormFactory;
 import com.jgoodies.forms.layout.ColumnSpec;
 import com.jgoodies.forms.layout.FormLayout;
 import com.jgoodies.forms.layout.RowSpec;
 
 import de.jClipCorn.database.databaseElement.columnTypes.CCMovieFormat;
 import de.jClipCorn.gui.CachedResourceLoader;
 import de.jClipCorn.gui.Resources;
 import de.jClipCorn.gui.frames.addMovieFrame.AddMovieFrame;
 import de.jClipCorn.gui.frames.mainFrame.MainFrame;
 import de.jClipCorn.gui.localization.LocaleBundle;
 import de.jClipCorn.properties.CCProperties;
 import de.jClipCorn.util.PathFormatter;
 
 public class ScanFolderFrame extends JFrame implements Runnable, MouseListener {
 	private static final long serialVersionUID = 7341007586104986521L;
 	
 	@SuppressWarnings("nls")
 	private final static String REGEX_PART_N = ".*\\(Part [2-9]\\)\\.[A-Za-z]{2,4}"; // .*\(Part [2-9]\)\.[A-Za-z]{2,4}
 	
 	private final JFileChooser folderchooser;
 	private final MainFrame owner;
 	
 	private JPanel panel;
 	private JScrollPane scrollPane;
 	private JList<String> lsFiles;
 	private JButton btnOpenFolder;
 	private JProgressBar progressBar;
 	private JButton btnAddAll;
 	private JButton btnRemoveAdditionalParts;
 	
 	private DefaultListModel<String> lsModel;
 	private JCheckBox cbIncludeSeries;
 	private JCheckBox cbExcludeIfo;
 	
 	private boolean includeSeries;
 	private boolean excludeIfos;
 	
 	public ScanFolderFrame(MainFrame mf) {
 		super();
 		this.folderchooser = new JFileChooser(PathFormatter.getRealSelfDirectory());
 		this.owner = mf;
 		
 		initGUI();
 		
 		cbIncludeSeries.setSelected(CCProperties.getInstance().PROP_SCANFOLDER_INCLUDESERIES.getValue());
 		cbExcludeIfo.setSelected(CCProperties.getInstance().PROP_SCANFOLDER_EXCLUDEIFOS.getValue());
 		
 		setLocationRelativeTo(mf);
 		
 		initFileChooser();
 	}
 	
 	private void initGUI() {
 		setTitle(LocaleBundle.getString("ScanFolderFrame.this.title")); //$NON-NLS-1$
 		setIconImage(CachedResourceLoader.getImage(Resources.IMG_FRAME_ICON));
 		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
 		
 		scrollPane = new JScrollPane();
 		getContentPane().add(scrollPane, BorderLayout.CENTER);
 		
 		lsFiles = new JList<>();
 		lsFiles.setVisibleRowCount(24);
 		scrollPane.setViewportView(lsFiles);
 		lsFiles.setModel(lsModel = new DefaultListModel<>());
 		lsFiles.addMouseListener(this);
 		
 		panel = new JPanel();
 		getContentPane().add(panel, BorderLayout.SOUTH);
 		panel.setLayout(new FormLayout(new ColumnSpec[] {
 				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
 				FormFactory.DEFAULT_COLSPEC,
 				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
 				ColumnSpec.decode("50dlu:grow"), //$NON-NLS-1$
 				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
 				FormFactory.DEFAULT_COLSPEC,
 				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
 				FormFactory.DEFAULT_COLSPEC,
 				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,},
 			new RowSpec[] {
 				FormFactory.LINE_GAP_ROWSPEC,
 				RowSpec.decode("23px"), //$NON-NLS-1$
 				FormFactory.RELATED_GAP_ROWSPEC,
 				FormFactory.DEFAULT_ROWSPEC,
 				FormFactory.LINE_GAP_ROWSPEC,}));
 		
 		btnOpenFolder = new JButton(LocaleBundle.getString("ScanFolderFrame.btnChooseFolder.text")); //$NON-NLS-1$
 		btnOpenFolder.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				if (folderchooser.showOpenDialog(ScanFolderFrame.this) == JFileChooser.APPROVE_OPTION) {
 					includeSeries = cbIncludeSeries.isSelected();
 					excludeIfos = cbExcludeIfo.isSelected();
 					
 					Thread run = new Thread(ScanFolderFrame.this);
 					run.start();
 				}
 			}
 		});
 		panel.add(btnOpenFolder, "2, 2, left, top"); //$NON-NLS-1$
 		
 		progressBar = new JProgressBar();
 		panel.add(progressBar, "4, 2, fill, center"); //$NON-NLS-1$
 		
 		btnRemoveAdditionalParts = new JButton(LocaleBundle.getString("ScanFolderFrame.btnCleanUp.text")); //$NON-NLS-1$
 		btnRemoveAdditionalParts.setEnabled(false);
 		btnRemoveAdditionalParts.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				removeAdditional();
 			}
 		});
 		panel.add(btnRemoveAdditionalParts, "6, 2, left, top"); //$NON-NLS-1$
 		
 		btnAddAll = new JButton();
 		btnAddAll.setEnabled(false);
 		btnAddAll.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				addAll();
 			}
 		});
 		panel.add(btnAddAll, "8, 2, left, top"); //$NON-NLS-1$
 		
 		cbIncludeSeries = new JCheckBox(LocaleBundle.getString("ScanFolderFrame.cbIncludeSeries.text")); //$NON-NLS-1$
 		cbIncludeSeries.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent arg0) {
 				CCProperties.getInstance().PROP_SCANFOLDER_INCLUDESERIES.setValue(cbIncludeSeries.isSelected());
 			}
 		});
 		panel.add(cbIncludeSeries, "2, 4, 3, 1"); //$NON-NLS-1$
 		
 		cbExcludeIfo = new JCheckBox(LocaleBundle.getString("ScanFolderFrame.cbExcludeIfos.text")); //$NON-NLS-1$
 		cbExcludeIfo.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				CCProperties.getInstance().PROP_SCANFOLDER_EXCLUDEIFOS.setValue(cbExcludeIfo.isSelected());
 			}
 		});
 		panel.add(cbExcludeIfo, "6, 4, 3, 1"); //$NON-NLS-1$
 		
 		updateCount();
 		
 		pack();
 	}
 	
 	private void initFileChooser() {
 		folderchooser.setDialogTitle(LocaleBundle.getString("ScanFolderFrame.dlg.title")); //$NON-NLS-1$
 		folderchooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY);
 	}
 
 	@Override
 	public void run() {
 		SwingUtilities.invokeLater(new Runnable() {
 			@Override
 			public void run() {
 				btnOpenFolder.setEnabled(false);
 				lsModel.clear();
 				progressBar.setIndeterminate(true);
 			}
 		});
 		
 		File dir = folderchooser.getSelectedFile();
 		
 		if (dir.isDirectory()) {
 			// List of Files in Directory
 			ArrayList<File> filelist = new ArrayList<>();
 			searchFiles(dir, filelist);
 			
 			// List of Files in in Database
 			ArrayList<File> movielist = owner.getMovielist().getAbsolutePathList(includeSeries);
 			
 			filelist.removeAll(movielist);
 
 			for (File f : filelist) {
 				addToList(f);
 			}
 
 		}
 		
 		SwingUtilities.invokeLater(new Runnable() {
 			@Override
 			public void run() {
 				btnRemoveAdditionalParts.setEnabled(true);
 				btnAddAll.setEnabled(true);
 				cbIncludeSeries.setEnabled(false);
 				progressBar.setIndeterminate(false);
 				updateCount();
 			}
 		});
 	}
 
 	private void searchFiles(File dir, ArrayList<File> filelist) {
 		File[] files = dir.listFiles(new FileFilter() {
 			@Override
 			public boolean accept(File f) {
 				return (CCMovieFormat.isValidMovieFormat(PathFormatter.getExtension(f.getAbsolutePath())) && !(excludeIfos && PathFormatter.getExtension(f.getAbsolutePath()).equalsIgnoreCase("ifo"))) || f.isDirectory(); //$NON-NLS-1$
 			}
 		});
 		for (File f : files) {
 			if (f.isDirectory()) {
 				searchFiles(f, filelist);
 			} else {
 				filelist.add(f);
 			}
 		}
 	}
 	
 	private void addToList(final File f) {
 		SwingUtilities.invokeLater(new Runnable() {
 			@Override
 			public void run() {
 				lsModel.addElement(f.getAbsolutePath());
 			}
 		});
 	}
 	
 	private void removeAdditional() {
 		for (int i = lsModel.size() - 1; i >= 0; i--) {
 			if (lsModel.get(i).matches(REGEX_PART_N)) {
 				lsModel.remove(i);
 			}
 		}
 		
 		updateCount();
 	}
 	
 	private void updateCount() {
 		btnAddAll.setText(LocaleBundle.getString("ScanFolderFrame.btnAddAll.text") + String.format(" (%03d)", lsModel.size()));  //$NON-NLS-1$//$NON-NLS-2$
 	}
 	
 	private void addAll() {
 		for (int i = 0; i < lsModel.size(); i++) {
 			String path = lsModel.get(i);
 			AddMovieFrame amf = new AddMovieFrame(this, owner.getMovielist(), path);
 			amf.setVisible(true);
 		}
 		dispose();
 	}
 
 	@Override
 	public void mouseClicked(MouseEvent e) {
 		if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2 && lsFiles.getSelectedIndex() >= 0) {
 			String path = lsFiles.getSelectedValue();
 			lsModel.remove(lsFiles.getSelectedIndex());
 			AddMovieFrame amf = new AddMovieFrame(this, owner.getMovielist(), path);
 			amf.setVisible(true);
 		}
 	}
 
 	@Override
 	public void mouseEntered(MouseEvent e) {
 		// ---
 	}
 
 	@Override
 	public void mouseExited(MouseEvent e) {
 		// ---
 	}
 
 	@Override
 	public void mousePressed(MouseEvent e) {
 		// ---
 	}
 
 	@Override
 	public void mouseReleased(MouseEvent e) {
 		// ---
 	}
 }
