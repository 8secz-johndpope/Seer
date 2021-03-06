 package syndie.gui;
 
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import net.i2p.data.Base64;
 import net.i2p.data.Hash;
 
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.FocusEvent;
 import org.eclipse.swt.events.FocusListener;
 import org.eclipse.swt.events.ModifyEvent;
 import org.eclipse.swt.events.ModifyListener;
 import org.eclipse.swt.events.TraverseEvent;
 import org.eclipse.swt.events.TraverseListener;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.Menu;
 import org.eclipse.swt.widgets.MenuItem;
 import org.eclipse.swt.widgets.Text;
 import org.eclipse.swt.widgets.Tree;
 import org.eclipse.swt.widgets.TreeColumn;
 import org.eclipse.swt.widgets.TreeItem;
 
 import syndie.Constants;
 import syndie.data.ReferenceNode;
 import syndie.data.SyndieURI;
 import syndie.util.Timer;
 import syndie.data.WatchedChannel;
 import syndie.db.DBClient;
 import syndie.db.JobRunner;
 import syndie.db.UI;
 import syndie.util.DateTime;
 
 /**
  *
  * Parent is usually a ForumReferenceChooserPopup.
  *
  */
 public class NymChannelTree implements Themeable, Translatable {
     private DBClient _client;
     private UI _ui;
     private Composite _parent;
     private Composite _root;
     private Composite _top;
     private Tree _tree;
     private TreeColumn _colAvatar;
     private TreeColumn _colName;
     private TreeColumn _colDesc;
     private TreeColumn _colMsgs;
     private TreeColumn _colLastPost;
     private TreeColumn _colAttributes;
 
     private Label _filterLabel;
     private Button _unreadOnlySel;
     private Button _privateOnlySel;
     private Text _search;
     private Button _searchButton;
     private Button _searchAll;
     private boolean _wasSearchHelpCleared;
 
     private Composite _bottom;
     private Button _nymChannelsButton;
     private Button _bookmarksButton;
     
     private boolean _showTypeButtons;
     private boolean _noMenu;
     
     private ChannelSource _nymChannelsSource;
     private ChannelSource _bookmarksSource;
 
     private boolean _sourceChanged;
     private ChannelSource _currentSource;
     
     private boolean _unreadOnly;
     private boolean _privateOnly;
     
     private ThemeRegistry _themeRegistry;
     private TranslationRegistry _translationRegistry;
     private NavigationControl _navControl;
     private BanControl _banControl;
     private BookmarkControl _bookmarkControl;
     
     private NymChannelTreeListener _listener;
     
     private NymChannelTreeDnD _dnd;
 
     private static final String SPACER = "                                                                 ";
 
 
     public interface ChannelSource {
         public List getReferenceNodes();
         public boolean isManageable(long chanId);
         public boolean isPostable(long chanId);
         public boolean isWatched(long chanId);
         public boolean isDeletable(long chanId);
         public void loadSource();
     }
     
     public interface NymChannelTreeListener {
         public void channelSelected(SyndieURI uri);
         public void channelProfileSelected(SyndieURI uri);
         public void channelManageSelected(SyndieURI manageURI);
         public void channelPostSelected(SyndieURI postURI);
         public void channelPreviewed(Hash scope, long chanId, String name, String desc, Image avatar);
     }
     
     public NymChannelTree(DBClient client, UI ui, ThemeRegistry themes, TranslationRegistry trans, NavigationControl navControl, BanControl banControl, BookmarkControl bookmarkControl, Composite parent, NymChannelTreeListener lsnr) {
         this(client, ui, themes, trans, navControl, banControl, bookmarkControl, parent, lsnr, true, false);
     }
     public NymChannelTree(DBClient client, UI ui, ThemeRegistry themes, TranslationRegistry trans, NavigationControl navControl, BanControl banControl, BookmarkControl bookmarkControl, Composite parent, NymChannelTreeListener lsnr, boolean showTypeButtons) {
         this(client, ui, themes, trans, navControl, banControl, bookmarkControl, parent, lsnr, showTypeButtons, false);
     }
     public NymChannelTree(DBClient client, UI ui, ThemeRegistry themes, TranslationRegistry trans, NavigationControl navControl, BanControl banControl, BookmarkControl bookmarkControl, Composite parent, NymChannelTreeListener lsnr, boolean showTypeButtons, boolean noMenu) {
         _client = client;
         _ui = ui;
         _parent = parent;
         _themeRegistry = themes;
         _translationRegistry = trans;
         _listener = lsnr;
         _navControl = navControl;
         _banControl = banControl;
         _bookmarkControl = bookmarkControl;
         _unreadOnly = false;
         _privateOnly = false;
         _showTypeButtons = showTypeButtons;
         _noMenu = noMenu;
 
         initComponents();
         
         _dnd = new NymChannelTreeDnD(_client, _ui, _bookmarkControl, this);
         
         _translationRegistry.register(this);
         _themeRegistry.register(this);
     }
 
     public boolean isShowingRefs() { return _bookmarksSource == _currentSource; }
     
     public void forceFocus() { _tree.forceFocus(); }
     
     public void dispose() {
         _dnd.disable();
         _themeRegistry.unregister(this);
         _translationRegistry.unregister(this);
         disposeItems();
     }
     private void disposeItems() {
         ArrayList items = new ArrayList();
         if (_tree.isDisposed())
             return;
         
         TreeItem roots[] = _tree.getItems();
         for (int i = 0; i < roots.length; i++)
             getItems(items, roots[i]);
         for (int i = 0; i < items.size(); i++) {
             TreeItem item = (TreeItem)items.get(i);
             Image img = item.getImage(1);
             if (img != null)
                 ImageUtil.dispose(img);
         }
         for (int i = 0; i < roots.length; i++)
             roots[i].dispose();
     }
     private void getItems(List rv, TreeItem cur) {
         if (cur == null)
             return;
         rv.add(cur);
         TreeItem children[] = cur.getItems();
         if (children != null)
             for (int i = 0; i < children.length; i++)
                 getItems(rv, children[i]);
     }
     
     public void setLayoutData(Object data) { _root.setLayoutData(data); }
 
     private void initComponents() {
         _root = new Composite(_parent, SWT.NONE);
         GridLayout gl = new GridLayout(1, true);
         gl.marginHeight = 0;
         gl.marginWidth = 0;
         gl.verticalSpacing = 0;
         gl.horizontalSpacing = 0;
         _root.setLayout(gl);
         
         _top = new Composite(_root, SWT.NONE);
         _top.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
         initTop();
         
         _tree = new Tree(_root, SWT.MULTI | SWT.FULL_SELECTION);
         _tree.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
         _colName = new TreeColumn(_tree, SWT.LEFT);
         _colAvatar = new TreeColumn(_tree, SWT.CENTER);
         _colDesc = new TreeColumn(_tree, SWT.LEFT);
         _colMsgs = new TreeColumn(_tree, SWT.LEFT);
         _colLastPost = new TreeColumn(_tree, SWT.LEFT);
         _colAttributes = new TreeColumn(_tree, SWT.LEFT);
         _tree.setHeaderVisible(true);
         _tree.setLinesVisible(true);
         
         SyndieTreeListener lsnr = new SyndieTreeListener(_tree) {
             public void selected() {
                 TreeItem sel[] = _tree.getSelection();
                 if ( (sel != null) && (sel.length > 0) ) {
                     if (_noMenu) {
                         _tree.setMenu(null);
                     } else {
                         if (sel.length == 1) {
                             Long chanId = (Long)sel[0].getData("channelId");
                             if (chanId != null) {
                                 boolean manageable = (null != sel[0].getData("manageable"));
                                 boolean postable = (null != sel[0].getData("postable"));
                                 boolean deletable = (null != sel[0].getData("deletable"));
                                 boolean watched = (null != sel[0].getData("watched"));
                                 buildMenu(chanId.longValue(), manageable, postable, deletable, watched, null);
                             } else {
                                 if (sel[0].getItemCount() > 0) {
                                     buildGroupMenu(sel);
                                 } else {
                                     SyndieURI uri = (SyndieURI)sel[0].getData("syndieURI");
                                     if (uri != null)
                                         buildMenu(-1, false, false, true, false, uri);
                                     else
                                         _tree.setMenu(null);
                                 }
                             }
                         } else {
                             buildGroupMenu(sel);
                         }
                     }
                     Long chanId = (Long)sel[0].getData("channelId");
                     if (chanId != null) {
                         Hash scope = _client.getChannelHash(chanId.longValue());
                         _listener.channelPreviewed(scope, chanId.longValue(), sel[0].getText(0), sel[0].getText(2), sel[0].getImage(1));
                     }
                 } else {
                     _tree.setMenu(null);
                 }
             }
             public void returnHit() { viewSelected(); }
             public void doubleclick() { viewSelected(); }
             private void viewSelected() {
                 TreeItem sel[] = _tree.getSelection();
                 if ( (sel != null) && (sel.length == 1) ) {
                     Long chanId = (Long)sel[0].getData("channelId");
                     if (chanId != null) {
                         Hash chan = _client.getChannelHash(chanId.longValue());
                         _listener.channelSelected(SyndieURI.createScope(chan));
                         //_navControl.view(SyndieURI.createScope(chan));
                     } else {
                         ReferenceNode node = (ReferenceNode)sel[0].getData("node");
                         if ( (node != null) && (node.getURI() != null) )
                             _listener.channelSelected(node.getURI());
                     }
                 }
             }
         };
         _tree.addSelectionListener(lsnr);
         _tree.addMouseListener(lsnr);
         _tree.addTraverseListener(lsnr);
         _tree.addKeyListener(lsnr);
         
         _bottom = new Composite(_root, SWT.NONE);
         _bottom.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
         initBottom();
         
         resizeCols(true);
     }
     
     private void resizeCols(boolean showChanCols) {
         //_colAvatar.setWidth(Constants.MAX_AVATAR_WIDTH + (_tree.getGridLineWidth()*2));
         int lineWidth = _tree.getGridLineWidth();
         int sz = _parent.getClientArea().width - 25;
         _ui.debugMessage("client width: " + sz + " line width: " + lineWidth);
         int avatarWidth = Constants.MAX_AVATAR_WIDTH + lineWidth*2;
         int msgsWidth = (showChanCols ? 75 + lineWidth*2 : 0);
         int lastPostWidth = (showChanCols ? 100 + lineWidth*2 : 0);
         int attribWidth = (showChanCols ? 100 + lineWidth*2 : 0);
         int nameWidth = (int)(.6 * (sz - (avatarWidth + msgsWidth + lastPostWidth + attribWidth + lineWidth*2)));
         int descWidth = sz - (avatarWidth + msgsWidth + lastPostWidth + attribWidth + nameWidth + lineWidth*2);
         _colAvatar.setWidth(avatarWidth);
         _colName.setWidth(nameWidth);
         _colDesc.setWidth(descWidth);
         _colMsgs.setWidth(msgsWidth);
         _colLastPost.setWidth(lastPostWidth);
         _colAttributes.setWidth(attribWidth);
     }
     
     private void initTop() {
         GridLayout gl = new GridLayout(6, false);
         gl.horizontalSpacing = 8;
         gl.verticalSpacing = 0;
         gl.marginHeight = 3;
         gl.marginWidth = 5;
         _top.setLayout(gl);
         
         _filterLabel = new Label(_top, SWT.NONE);
         _filterLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
         
         _unreadOnlySel = new Button(_top, SWT.CHECK);
         _unreadOnlySel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true));
         _unreadOnlySel.addSelectionListener(new FireSelectionListener() { public void fire() { recalcTree(_unreadOnlySel.getSelection(), _privateOnlySel.getSelection()); } });
         
         _privateOnlySel = new Button(_top, SWT.CHECK);
         _privateOnlySel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
         _privateOnlySel.addSelectionListener(new FireSelectionListener() { public void fire() { recalcTree(_unreadOnlySel.getSelection(), _privateOnlySel.getSelection()); } });
         
         _search = new Text(_top, SWT.SINGLE | SWT.BORDER);
         _search.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true));
         _search.addTraverseListener(new TraverseListener() {
             public void keyTraversed(TraverseEvent evt) {
                 if (evt.detail == SWT.TRAVERSE_RETURN) {
                     String s = _search.getText();
                     String t = s.trim();
                     if (!s.equals(t))
                         _search.setText(t);
                     recalcTree(t, _unreadOnlySel.getSelection(), _privateOnlySel.getSelection());
                 }
             }
         });
 
         _searchButton = new Button(_top, SWT.PUSH);
         _searchButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
         _searchButton.addSelectionListener(new FireSelectionListener() { 
             public void fire() { 
                 String s = _search.getText();
                 String t = s.trim();
                 if (!s.equals(t))
                     _search.setText(t);
                 recalcTree(t, _unreadOnlySel.getSelection(), _privateOnlySel.getSelection());
             } 
         });
 
         _search.addModifyListener(new ModifyListener() {
             public void modifyText(ModifyEvent modifyEvent) {
                 boolean hasText = _search.getText().length() > 0;
                 _searchButton.setEnabled(hasText);
             }
         });
         _search.addFocusListener(new FocusListener() {
             // clear help when clicked on
             private boolean _wasCleared;
             public void focusGained(FocusEvent focusEvent) {
                 if (!_wasSearchHelpCleared) {
                     _search.setForeground(_unreadOnlySel.getForeground());  // just use button - how to get default color? or just save it...
                     _search.setText("");
                     _wasSearchHelpCleared = true;
                 }
             }
             public void focusLost(FocusEvent focusEvent) {
                 if (_wasSearchHelpCleared && _search.getText().length() <= 0) {
                     _search.setForeground(ColorUtil.getColor("gray"));
                     _search.setText(_translationRegistry.getText("Search term") + SPACER);  // make bigger
                     _searchButton.setEnabled(false);
                     _wasSearchHelpCleared = false;
                 }
             }
         });
         
         _searchAll = new Button(_top, SWT.PUSH);
         _searchAll.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
         _searchAll.addSelectionListener(new FireSelectionListener() { 
             public void fire() {
                 _search.setText("");
                 recalcTree("", _unreadOnlySel.getSelection(), _privateOnlySel.getSelection());
             }
         });
     }
     
     public void showNymChannels() {
         if (_nymChannelsSource == null)
             _nymChannelsSource = new NymChannelSource(_client, _translationRegistry);
         if (_currentSource != _nymChannelsSource) {
             setChannelSource(_nymChannelsSource);
             recalcTree(_unreadOnlySel.getSelection(), _privateOnlySel.getSelection());
         }
     }
     
     public void showBookmarks() {
         if (_bookmarksSource == null)
             _bookmarksSource = new BookmarksChannelSource(_client, _translationRegistry);
         if (_currentSource != _bookmarksSource) {
             setChannelSource(_bookmarksSource);
             recalcTree(_unreadOnlySel.getSelection(), _privateOnlySel.getSelection());
         }
     }
     
     public void bookmarksUpdated() { 
         _sourceChanged = true;
         if (_bookmarksSource != null)
             ((BookmarksChannelSource)_bookmarksSource).clearSource();
     }
     public void nymChannelsUpdated() { 
         _sourceChanged = true;
         if (_nymChannelsSource != null)
             ((NymChannelSource)_nymChannelsSource).clearSource();
     }
     
     private void initBottom() {
         _bottom.setLayout(new FillLayout(SWT.HORIZONTAL));
         
         _nymChannelsButton = new Button(_bottom, SWT.PUSH);
         _nymChannelsButton.addSelectionListener(new FireSelectionListener() { 
             public void fire() {
                 if (_nymChannelsSource == null)
                     _nymChannelsSource = new NymChannelSource(_client, _translationRegistry);
                 setChannelSource(_nymChannelsSource);
                 recalcTree(_unreadOnlySel.getSelection(), _privateOnlySel.getSelection());
             }
         });
         
         _bookmarksButton = new Button(_bottom, SWT.PUSH);
         _bookmarksButton.addSelectionListener(new FireSelectionListener() { 
             public void fire() {
                 if (_bookmarksSource == null)
                     _bookmarksSource = new BookmarksChannelSource(_client, _translationRegistry);
                 setChannelSource(_bookmarksSource);
                 recalcTree(_unreadOnlySel.getSelection(), _privateOnlySel.getSelection());
             }
         });
         
         _bottom.setVisible(_showTypeButtons);
         _nymChannelsButton.setVisible(_showTypeButtons);
         _bookmarksButton.setVisible(_showTypeButtons);
         if (!_showTypeButtons) {
             ((GridData)_bottom.getLayoutData()).exclude = true;
             _root.layout(true, true);
         }
     }
     
     private void buildGroupMenu(final TreeItem items[]) {
         Menu menu = _tree.getMenu();
         if (menu != null)
             menu.dispose();
         
         menu = new Menu(_tree);
         
         MenuItem view = new MenuItem(menu, SWT.PUSH);
         view.addSelectionListener(new FireSelectionListener() { 
             public void fire() { 
                 ArrayList scopes = new ArrayList();
                 for (int i = 0; i < items.length; i++)
                     getMatchingScopes(scopes, items[i]);
                 viewMatching(scopes); 
             }
         });
         view.setText(_translationRegistry.getText("Combined view of the selected forums"));
         
         _tree.setMenu(menu);
     }
 
     public void viewMatching() {
         TreeItem items[] = _tree.getSelection();
         ArrayList scopes = new ArrayList();
         if ( (items != null) && (items.length > 0) ) {
             for (int i = 0; i < items.length; i++)
                 getMatchingScopes(scopes, items[i]);
         } else {
             items = _tree.getItems();
             for (int i = 0; i < items.length; i++)
                 getMatchingScopes(scopes, items[i]);
         }
         viewMatching(scopes);
     }
     
     private void viewMatching(List scopes) {
         boolean unread = _unreadOnly;
         boolean threaded = true;
         boolean useImportDate = MessageTree.shouldUseImportDate(_client);
         _ui.debugMessage("all matching channels selected: " + scopes);
         _listener.channelSelected(SyndieURI.createSearch(scopes, unread, threaded, useImportDate));
     }
     
     private void getMatchingScopes(ArrayList scopes, TreeItem base) {
         if (base == null) return;
         Long chanId = (Long)base.getData("channelId");
         if (chanId != null) {
             Hash chan = _client.getChannelHash(chanId.longValue());
             if ( (chan != null) && (!scopes.contains(chan)) )
                 scopes.add(chan);
         }
         int kids = base.getItemCount();
         for (int i = 0; i < kids; i++)
             getMatchingScopes(scopes, base.getItem(i));
     }
     
     
     private void buildMenu(final long chanId, boolean manageable, boolean postable, boolean deletable, boolean watched, final SyndieURI otherURI) {
         Menu menu = _tree.getMenu();
         if (menu != null)
             menu.dispose();
         
         menu = new Menu(_tree);
         
         if (chanId >= 0) {
             MenuItem view = new MenuItem(menu, SWT.PUSH);
             view.addSelectionListener(new FireSelectionListener() {
                 public void fire() {
                     Hash chan = _client.getChannelHash(chanId);
                     _listener.channelSelected(SyndieURI.createScope(chan));
                     //_navControl.view(SyndieURI.createScope(chan));
                 }
             });
             view.setText(_translationRegistry.getText("View the forum"));
 
             MenuItem profile = new MenuItem(menu, SWT.PUSH);
             profile.addSelectionListener(new FireSelectionListener() {
                 public void fire() {
                     Hash chan = _client.getChannelHash(chanId);
                     _listener.channelProfileSelected(SyndieURI.createScope(chan));
                     //_navControl.view(SyndieURI.createScope(chan));
                 }
             });
             profile.setText(_translationRegistry.getText("View the forum's profile"));
 
             if (manageable) {
                 MenuItem manage = new MenuItem(menu, SWT.PUSH);
                 manage.addSelectionListener(new FireSelectionListener() {
                     public void fire() {
                         Hash chan = _client.getChannelHash(chanId);
                         _listener.channelManageSelected(URIHelper.instance().createMetaURI(chan));
                         //_navControl.view(URIHelper.instance().createMetaURI(chan));
                     }
                 });
                 manage.setText(_translationRegistry.getText("Manage the forum"));
             }
 
             if (postable) {
                 MenuItem post = new MenuItem(menu, SWT.PUSH);
                 post.addSelectionListener(new FireSelectionListener() {
                     public void fire() {
                         Hash chan = _client.getChannelHash(chanId);
                         _listener.channelPostSelected(URIHelper.instance().createPostURI(chan, null));
                         //_navControl.view(URIHelper.instance().createPostURI(chan, null));
                     }
                 });
                 post.setText(_translationRegistry.getText("Post in the forum"));
             }
 
             if (watched) {
                 MenuItem unwatch = new MenuItem(menu, SWT.PUSH);
                 unwatch.addSelectionListener(new FireSelectionListener() {
                     public void fire() {
                         Hash chan = _client.getChannelHash(chanId);
                         _client.unwatchChannel(chan);
                         loadData();
                         //_listener.channelPostSelected(URIHelper.instance().createPostURI(chan, null));
                         //_navControl.view(URIHelper.instance().createPostURI(chan, null));
                     }
                 });
                 unwatch.setText(_translationRegistry.getText("Unwatch the forum"));
             }
         } else {
             MenuItem view = new MenuItem(menu, SWT.PUSH);
             view.addSelectionListener(new FireSelectionListener() {
                 public void fire() {
                     _listener.channelSelected(otherURI);
                 }
             });
             view.setText(_translationRegistry.getText("View the resource"));
         }
 
         if (deletable) {
             MenuItem delete = new MenuItem(menu, SWT.PUSH);
             delete.addSelectionListener(new FireSelectionListener() {
                 public void fire() {
                     _bookmarkControl.deleteBookmark(chanId);
                     loadData();
                     //_listener.channelPostSelected(URIHelper.instance().createPostURI(chan, null));
                     //_navControl.view(URIHelper.instance().createPostURI(chan, null));
                 }
             });
             delete.setText(_translationRegistry.getText("Drop the reference"));
         }
         _tree.setMenu(menu);
     }
     
     
     public void setChannelSource(ChannelSource src) {
         if (src instanceof NymChannelSource)
             _nymChannelsSource = src;
         _sourceChanged = (src != _currentSource);
         if (_sourceChanged && (src instanceof BookmarksChannelSource))
             enableDnD();
         else
             disableDnD();
         _currentSource = src;
     }
     private ChannelSource getChannelSource() { return _currentSource; }
     
     private void enableDnD() { _dnd.enable(); }
     private void disableDnD() { _dnd.disable(); }
     Tree getTree() { return _tree; }
     ReferenceNode getNode(TreeItem item) { 
         if (item != null) 
             return (ReferenceNode)item.getData("node");
         else
             return null;
     }
     
     void recalcTree() {
         recalcTree(_unreadOnlySel.getSelection(), _privateOnlySel.getSelection()); 
     }
     public void recalcTree(boolean unreadOnly, boolean privateOnly) {
         if (_sourceChanged || (unreadOnly != _unreadOnly) || (privateOnly != _privateOnly) ) {
             _unreadOnly = unreadOnly;
             _privateOnly = privateOnly;
             loadData();
         }
     }
     
     public void recalcTree(final String term, boolean unreadOnly, boolean privateOnly) {
         _unreadOnly = unreadOnly;
         _privateOnly = privateOnly;
         setChannelSource(new SearchChannelSource(term, _client, _translationRegistry));
         loadData();
     }
     
     private void loading(boolean loading) {
         _tree.setEnabled(!loading);
         _tree.setVisible(!loading);
         _filterLabel.setEnabled(!loading);
         _unreadOnlySel.setEnabled(!loading);
         _privateOnlySel.setEnabled(!loading);
         _search.setEnabled(!loading);
         _searchButton.setEnabled(_wasSearchHelpCleared && _search.getText().length() > 0 && !loading);
         _searchAll.setEnabled(!loading);
         _nymChannelsButton.setEnabled(!loading);
         _bookmarksButton.setEnabled(!loading);
     }
     
     public void loadData() {
         loading(true);
         final Timer t = new Timer("load data", _ui);
         disposeItems();
         t.addEvent("items disposed");
         
         final ChannelSource src = getChannelSource();
         JobRunner.instance().enqueue(new Runnable() {
             public void run() {
                 t.addEvent("non swt thread running");
                 src.loadSource();
                 t.addEvent("source loaded");
                 final List nodes = src.getReferenceNodes();
                 t.addEvent("nodes fetched");
                 final Map chanIdToRecord = fetchRecords(src, nodes);
                 t.addEvent("records fetched");
                 _ui.debugMessage("loading data: source found " + nodes.size() + "/" + chanIdToRecord.size() + " nodes: " + src);
                 
                 _root.getDisplay().asyncExec(new Runnable() {
                     public void run() {
                         t.addEvent("swt thread running");
                         _ui.debugMessage("loading individual nodes in the swt thread");
                         for (int i = 0; i < nodes.size(); i++)
                             loadData(t, src, (ReferenceNode)nodes.get(i), chanIdToRecord, null);
                         t.addEvent("nodes loaded (" + nodes.size() + ")");
                         boolean showChannelCols = (!(src instanceof BookmarksChannelSource));
                         resizeCols(showChannelCols);
                         loading(false);
                         t.complete();
                     }
                 });
             }
         });
     }
     
     private static class Record {
         public Record() { 
             channelId = -1;
             deletable = true;
         }
         long channelId;
         String name;
         String desc;
         byte avatarData[];
         int unreadMessages;
         int unreadPrivate;
         int totalMessages;
         boolean manageable;
         boolean postable;
         boolean watched;
         boolean deletable;
         long lastPostDate;
         boolean referencesIncluded;
     }
     
     private Map fetchRecords(ChannelSource src, List nodes) {
         Map rv = new HashMap();
         Set groupNodes = new HashSet();
         for (int i = 0; i < nodes.size(); i++)
             findNodes(groupNodes, (ReferenceNode)nodes.get(i), true);
         Set forumNodes = new HashSet();
         for (int i = 0; i < nodes.size(); i++)
             findNodes(forumNodes, (ReferenceNode)nodes.get(i), false);
         
         boolean ok = populateForumRecords(forumNodes, rv, src);
         if (!ok)
             return new HashMap();
         return rv;
     }
 
     private void findNodes(Set rv, ReferenceNode node, boolean groupNodes) {
         SyndieURI uri = node.getURI();
         Record r = new Record();
         if (uri == null) {
             if (groupNodes)
                 rv.add(node);
         } else if (uri.isChannel() && (uri.getScope() != null) && (uri.getMessageId() == null) ) {
             if (!groupNodes)
                 rv.add(node);
         } else {
             if (groupNodes)
                 rv.add(node);
         }
         
         int kids = node.getChildCount();
         for (int i = 0; i < kids; i++)
             findNodes(rv, node.getChild(i), groupNodes);
     }
     
     private static final String SQL_GET_RECORD_BASIC_BEGIN = "SELECT c.name, c.channelHash, c.description, c.channelId, ca.avatarData FROM channel c LEFT OUTER JOIN channelAvatar ca ON c.channelId = ca.channelId WHERE c.channelId IN (";
     private static final String SQL_GET_RECORD_LASTPOST_BEGIN = "SELECT MAX(importDate), targetChannelId FROM channelMessage WHERE deletionCause IS NULL AND targetChannelId IN (";
     private static final String SQL_GET_RECORD_REFCOUNT_BEGIN = "SELECT COUNT(groupId), channelId FROM channelReferenceGroup WHERE channelId IN (";
     private static final String SQL_GET_RECORD_UNREADMSGS_BEGIN = "SELECT COUNT(msgId), targetChannelId FROM nymUnreadMessage num JOIN channelMessage cm ON num.msgId = cm.msgId WHERE cm.readKeyMissing = FALSE AND cm.replyKeyMissing = FALSE AND cm.pbePrompt IS NULL AND deletionCause IS NULL AND targetChannelId IN (";
     private static final String SQL_GET_RECORD_UNREADPRIV_BEGIN = "SELECT COUNT(msgId), targetChannelId FROM nymUnreadMessage num JOIN channelMessage cm ON num.msgId = cm.msgId WHERE cm.wasPrivate = true AND cm.readKeyMissing = FALSE AND cm.replyKeyMissing = FALSE AND cm.pbePrompt IS NULL AND deletionCause IS NULL AND targetChannelId IN (";
    private static final String SQL_GET_RECORD_TOTALMSGS_BEGIN = "SELECT COUNT(msgId) FROM channelMessage WHERE isCancelled = FALSE AND readKeyMissing = FALSE AND replyKeyMissing = FALSE AND pbePrompt IS NULL AND deletionCause IS NULL AND targetChannelId IN (";
 
     private boolean populateForumRecords(Set nodes, Map chanIdToRecord, ChannelSource src) {
         if (nodes.size() == 0)
             return true;
         
         if (src instanceof BookmarksChannelSource) {
             for (Iterator iter = nodes.iterator(); iter.hasNext(); ) {
                 ReferenceNode node = (ReferenceNode)iter.next();
                 Record r = new Record();
                 // these two are overridden if there is a match
                 r.name = node.getName();
                 r.desc = node.getDescription();
                 r.deletable = node.getChildCount() <= 0;
             }
             return true;
         }   
         
         StringBuilder idsBuf = new StringBuilder();
         for (Iterator iter = nodes.iterator(); iter.hasNext(); ) {
             ReferenceNode node = (ReferenceNode)iter.next();
             Long id = Long.valueOf(node.getUniqueId());
             if (chanIdToRecord.containsKey(id)) {
                 _ui.debugMessage("chanIdToRecord already exists: " + id + ": " + node);
                 continue;
             } else {
                 _ui.debugMessage("chanIdToRecord: building " + id);
             }
             
             Record r = new Record();
             // these two are overridden if there is a match
             r.name = node.getName();
             r.desc = node.getDescription();
             
             chanIdToRecord.put(id, r);
             
             if (idsBuf.length() > 0)
                 idsBuf.append(", ");
             idsBuf.append(node.getUniqueId());
         }
         String ids = idsBuf.toString();
 
         Statement stmt = null;
         ResultSet rs = null;
         
         //XXX String name = _client.getChannelName(chanId);
         //XXX Hash hash = _client.getChannelHash(chanId);
         //XXX byte avatar[] = _client.getChannelAvatar(chanId);
         //XXX String desc = _client.getChannelDescription(chanId);
         try {
             String query = SQL_GET_RECORD_BASIC_BEGIN + ids + ")";
             stmt = _client.con().createStatement();
             rs = stmt.executeQuery(query);
             while (rs.next()) {
                 // c.name, c.channelHash, c.description, c.channelId, ca.avatarData
                 String name = rs.getString(1);
                 byte hash[] = rs.getBytes(2);
                 String desc = rs.getString(3);
                 long chanId = rs.getLong(4);
                 byte avatar[] = rs.getBytes(5);
                 
                 Record r = (Record)chanIdToRecord.get(Long.valueOf(chanId));
                 r.channelId = chanId;
                 
                 if (name == null)
                     name = "";
                 if (hash != null)
                     name = name + " [" + Base64.encode(hash).substring(0,6) + "]";
                 r.name = name;
                 
                 if (desc == null)
                     desc = "";
                 r.desc = desc;
                 r.avatarData = avatar;
             }
         } catch (SQLException se) {
             _ui.errorMessage("Error fetching basic records", se);
             return false;
         } finally {
             if (rs != null) try { rs.close(); } catch (SQLException se) {}
             if (stmt != null) try { stmt.close(); } catch (SQLException se) {}
         }
         rs = null;
         stmt = null;
         
         //XXX r.lastPostDate = _client.getChannelLastPost(chanId);
         try {
             String query = SQL_GET_RECORD_LASTPOST_BEGIN + ids + ") GROUP BY targetChannelId";
             stmt = _client.con().createStatement();
             rs = stmt.executeQuery(query);
             while (rs.next()) {
                 //MAX(importDate), targetChannelId 
                 java.sql.Date when = rs.getDate(1);
                 long chanId = rs.getLong(2);
                 
                 Record r = (Record)chanIdToRecord.get(Long.valueOf(chanId));
                 // FIXME importDate is stored as a DATE, not a TIMESTAMP, we don't have the hhmmss stored.
                 // So we can't display the long version, the time is always 12:00:00
                 r.lastPostDate = (when != null ? when.getTime() : -1);
             }
         } catch (SQLException se) {
             _ui.errorMessage("Error fetching last import records", se);
             return false;
         } finally {
             if (rs != null) try { rs.close(); } catch (SQLException se) {}
             if (stmt != null) try { stmt.close(); } catch (SQLException se) {}
         }
         rs = null;
         stmt = null;
 
         //XXX List refs = _client.getChannelReferences(chanId);
         try {
             String query = SQL_GET_RECORD_REFCOUNT_BEGIN + ids + ") GROUP BY channelId";
             stmt = _client.con().createStatement();
             rs = stmt.executeQuery(query);
             while (rs.next()) {
                 // COUNT(groupId), channelId 
                 long count = rs.getLong(1);
                 long chanId = rs.getLong(2);
                 
                 Record r = (Record)chanIdToRecord.get(Long.valueOf(chanId));
                 r.referencesIncluded = count > 0;
             }
         } catch (SQLException se) {
             _ui.errorMessage("Error fetching ref count records", se);
             return false;
         } finally {
             if (rs != null) try { rs.close(); } catch (SQLException se) {}
             if (stmt != null) try { stmt.close(); } catch (SQLException se) {}
         }
         rs = null;
         stmt = null;
 
         //XXX int unread = _client.countUnreadMessages(chanId);
         try {
             String query = SQL_GET_RECORD_UNREADMSGS_BEGIN + ids + ") GROUP BY targetChannelId";
             stmt = _client.con().createStatement();
             rs = stmt.executeQuery(query);
             while (rs.next()) {
                 // COUNT(groupId), channelId 
                 long count = rs.getLong(1);
                 long chanId = rs.getLong(2);
                 
                 Record r = (Record)chanIdToRecord.get(Long.valueOf(chanId));
                 r.unreadMessages = (int)count;
             }
         } catch (SQLException se) {
             _ui.errorMessage("Error fetching unread message count records", se);
             return false;
         } finally {
             if (rs != null) try { rs.close(); } catch (SQLException se) {}
             if (stmt != null) try { stmt.close(); } catch (SQLException se) {}
         }
         rs = null;
         stmt = null;
 
         //XXX int privMsgs = (unread > 0 ? _client.countPrivateMessages(chanId, true) : 0);
         try {
             String query = SQL_GET_RECORD_UNREADPRIV_BEGIN + ids + ") GROUP BY targetChannelId";
             stmt = _client.con().createStatement();
             rs = stmt.executeQuery(query);
             while (rs.next()) {
                 // COUNT(groupId), channelId 
                 long count = rs.getLong(1);
                 long chanId = rs.getLong(2);
                 
                 Record r = (Record)chanIdToRecord.get(Long.valueOf(chanId));
                 r.unreadPrivate = (int)count;
             }
         } catch (SQLException se) {
             _ui.errorMessage("Error fetching unread message count records", se);
             return false;
         } finally {
             if (rs != null) try { rs.close(); } catch (SQLException se) {}
             if (stmt != null) try { stmt.close(); } catch (SQLException se) {}
         }
         rs = null;
         stmt = null;
 
         //XXX int total = _client.countMessages(chanId);
         try {
             String query = SQL_GET_RECORD_TOTALMSGS_BEGIN + ids + ") GROUP BY targetChannelId";
             stmt = _client.con().createStatement();
             rs = stmt.executeQuery(query);
             while (rs.next()) {
                 // COUNT(groupId), channelId 
                 long count = rs.getLong(1);
                 long chanId = rs.getLong(2);
                 
                 Record r = (Record)chanIdToRecord.get(Long.valueOf(chanId));
                 r.totalMessages = (int)count;
             }
         } catch (SQLException se) {
             _ui.errorMessage("Error fetching unread message count records", se);
             return false;
         } finally {
             if (rs != null) try { rs.close(); } catch (SQLException se) {}
             if (stmt != null) try { stmt.close(); } catch (SQLException se) {}
         }
         rs = null;
         stmt = null;
 
         for (Iterator iter = chanIdToRecord.values().iterator(); iter.hasNext(); ) {
             Record r = (Record)iter.next();
             if (r.channelId >= 0) {
                 r.manageable = src.isManageable(r.channelId);
                 r.postable = src.isPostable(r.channelId);
                 r.watched = src.isWatched(r.channelId);
                 r.deletable = src.isDeletable(r.channelId);
             }
         }
         
         return true;
     }
     
     private void loadData(Timer t, ChannelSource src, ReferenceNode node, Map chanIdToRecord, TreeItem parent) {
         if (node == null)
             return;
         
         TreeItem item = populateItem(t, src, node, chanIdToRecord, parent);
         if (item == null)
             return;
         
         int kids = node.getChildCount();
         for (int i = 0; i < kids; i++)
             loadData(t, src, node.getChild(i), chanIdToRecord, item);
     }
     
     private TreeItem populateItem(Timer t, ChannelSource src, ReferenceNode node, Map chanIdToRecord, TreeItem parent) {
         Record r = (Record)chanIdToRecord.get(Long.valueOf(node.getUniqueId()));
         if ( (r != null) && (r.channelId >= 0) )
             return populateForumItem(t, src, node, chanIdToRecord, parent);
         else
             return populateGroupItem(t, src, node, chanIdToRecord, parent);
     }
     
     private TreeItem populateGroupItem(Timer t, ChannelSource src, ReferenceNode node, Map chanIdToRecord, TreeItem parent) {
         _ui.debugMessage("populate group item: " + node.getUniqueId() + "/" + node.getURI() + ": " + node);
         TreeItem item = null;
         if (parent == null) {
             item = new TreeItem(_tree, SWT.NONE);
         } else {
             item = new TreeItem(parent, SWT.NONE);
             parent.setExpanded(true);
         }
         item.setExpanded(true);
         
         String name = node.getName();
         if (name == null)
             name = "";
 
         String desc = node.getDescription();
         // don't pollute description column with ugly URI
         //if ( (desc == null) || (desc.trim().length() <= 0) ) {
         //    SyndieURI uri = node.getURI();
         //    if ( (uri != null) && (uri.isURL()) )
         //        desc = uri.getURL();
         //}
         if (desc == null)
             desc = "";
 
         SyndieURI uri = node.getURI();
         if (uri != null)
             item.setData("syndieURI", uri);
         item.setData("node", node);
         
         item.setText(0, name);
         
         item.setImage(1, getImage(uri));
         
         //if ( (desc != null) && (desc.trim().length() > 0) )
             item.setText(2, desc);
         //else if (uri != null)
         //    item.setText(2, uri.toString());
         
         //t.addEvent("group loaded: " + name);
         return item;
     }
     
     private Image getImage(SyndieURI uri) {
         if (uri == null)
             return null;
         if ( (uri.isChannel() || uri.isSearch()) && (uri.getMessageId() == null) ) {
             Hash scope = uri.getScope();
             if (scope == null) {
                 Hash scopes[] = uri.getSearchScopes();
                 if ( (scopes != null) && (scopes.length == 1) )
                     scope = scopes[0];
             }
             long scopeId = -1;
             if (scope != null)
                 scopeId = _client.getChannelId(scope);
             if (scopeId >= 0) {
                 byte img[] = _client.getChannelAvatar(scopeId);
                 if (img != null)
                     return ImageUtil.createImage(img);
             }
             // fall through for unknown channels/etc
         }
         
         return ImageUtil.getTypeIcon(uri);
     }
     
     private TreeItem populateForumItem(Timer t, ChannelSource src, ReferenceNode node, Map chanIdToRecord, TreeItem parent) {
         Record r = (Record)chanIdToRecord.get(Long.valueOf(node.getUniqueId()));
         _ui.debugMessage("populate forum item: " + node.getUniqueId() + "/" + node.getURI() + ": " + node);
         long chanId = r.channelId;
         boolean manageable = r.manageable;
         boolean postable = r.postable;
         
         int unread = r.unreadMessages;
         int privMsgs = r.unreadPrivate;
         if (_unreadOnly) {
             if (unread <= 0)
                 return null;
         }
         if (_privateOnly) {
             if (privMsgs <= 0)
                 return null;
         }
         
         TreeItem item = null;
         if (parent == null) {
             item = new TreeItem(_tree, SWT.NONE);
         } else {
             item = new TreeItem(parent, SWT.NONE);
             parent.setExpanded(true);
         }
         item.setExpanded(true);
         
         item.setData("channelId", Long.valueOf(chanId));
         if (manageable)
             item.setData("manageable", "");
         if (postable)
             item.setData("postable", "");
         if (r.deletable)
             item.setData("deletable", "");
         if (r.watched)
             item.setData("watched", "");
         SyndieURI uri = node.getURI();
         if (uri != null)
             item.setData("syndieURI", uri);
         
         item.setData("node", node);
         
         item.setText(0, r.name);
         
         byte avatar[] = r.avatarData;
         if (avatar != null) {
             Image img = ImageUtil.createImage(avatar);
             if (img != null)
                 item.setImage(1, img);
             else
                 item.setImage(1, ImageUtil.getTypeIcon(uri));
         } else {
             item.setImage(1, ImageUtil.getTypeIcon(uri));
         }
         
         if ( (r.desc != null) && (r.desc.trim().length() > 0) )
             item.setText(2, r.desc);
         // don't pollute description column with ugly URI
         //else if (uri != null)
         //   item.setText(2, uri.toString());
         else
            item.setText(2, "");
         
         int total = r.totalMessages;
         
         if (total > 0)
             item.setText(3, unread + "/" + privMsgs + "/" + total);
         else
             item.setText(3, "");
         
         long when = r.lastPostDate;
         if (when > 0)
             // FIXME see above, no time available from DB
             //item.setText(4, DateTime.getDateTime(when));
             item.setText(4, DateTime.getDate(when));
         else
             item.setText(4, "");
         
         // TODO what is this?
         if (r.referencesIncluded)
             item.setText(5, _translationRegistry.getText("with references"));
         else
             item.setText(5, "");
         
         //t.addEvent("forum loaded: " + r.name);
         return item;
     }
     
     public void applyTheme(Theme theme) {
         _tree.setFont(theme.TREE_FONT);
         _filterLabel.setFont(theme.DEFAULT_FONT);
         _unreadOnlySel.setFont(theme.DEFAULT_FONT);
         _privateOnlySel.setFont(theme.DEFAULT_FONT);
         _search.setFont(theme.DEFAULT_FONT);
         _searchButton.setFont(theme.BUTTON_FONT);
         _searchAll.setFont(theme.BUTTON_FONT);
         _nymChannelsButton.setFont(theme.BUTTON_FONT);
         _bookmarksButton.setFont(theme.BUTTON_FONT);
         _root.layout(true, true);
     }
     
     public void translate(TranslationRegistry registry) {
         _colAvatar.setText(registry.getText("Avatar"));
         _colName.setText(registry.getText("Name"));
         _colDesc.setText(registry.getText("Description"));
         _colMsgs.setText(registry.getText("Messages"));
         _colMsgs.setToolTipText(registry.getText("Unread / Private / Total messages"));
         _colLastPost.setText(registry.getText("Last post"));
         _colAttributes.setToolTipText(registry.getText("Profile published references?"));
         
         _filterLabel.setText(registry.getText("Only include forums with") + ": ");
         _unreadOnlySel.setText(registry.getText("Unread messages"));
         _privateOnlySel.setText(registry.getText("Private messages"));
         _search.setText(registry.getText("Search term") + SPACER);   // make bigger
         _search.setForeground(ColorUtil.getColor("gray"));
         _searchButton.setText(registry.getText("Search"));
         _searchAll.setText(registry.getText("View all"));
         
         _nymChannelsButton.setText(registry.getText("Special forums"));
         _bookmarksButton.setText(registry.getText("Bookmarks"));
     }
     
     
     
 }
