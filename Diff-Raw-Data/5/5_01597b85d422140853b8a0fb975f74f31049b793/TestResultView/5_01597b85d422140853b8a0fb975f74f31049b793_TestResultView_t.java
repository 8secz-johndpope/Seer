 package com.piece_framework.makegood.ui.views;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.core.filesystem.EFS;
 import org.eclipse.core.filesystem.IFileStore;
 import org.eclipse.jface.action.ActionContributionItem;
 import org.eclipse.jface.action.IAction;
 import org.eclipse.jface.action.IToolBarManager;
 import org.eclipse.jface.text.BadLocationException;
 import org.eclipse.jface.text.IRegion;
 import org.eclipse.jface.viewers.ISelectionChangedListener;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.jface.viewers.SelectionChangedEvent;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.jface.viewers.TreeViewer;
 import org.eclipse.jface.viewers.Viewer;
 import org.eclipse.jface.viewers.ViewerFilter;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.CLabel;
 import org.eclipse.swt.custom.SashForm;
 import org.eclipse.swt.events.ControlAdapter;
 import org.eclipse.swt.events.ControlEvent;
 import org.eclipse.swt.events.MouseListener;
 import org.eclipse.swt.events.MouseMoveListener;
 import org.eclipse.swt.events.MouseEvent;
 import org.eclipse.swt.graphics.Color;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.graphics.RGB;
 import org.eclipse.swt.graphics.Cursor;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.layout.RowLayout;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.Tree;
 import org.eclipse.swt.custom.StyleRange;
 import org.eclipse.swt.custom.StyledText;
 import org.eclipse.ui.IEditorPart;
 import org.eclipse.ui.IViewPart;
 import org.eclipse.ui.IViewSite;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.contexts.IContextService;
 import org.eclipse.ui.ide.IDE;
 import org.eclipse.ui.part.ViewPart;
 import org.eclipse.ui.texteditor.ITextEditor;
 
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import java.util.Vector;
 
 import com.piece_framework.makegood.launch.elements.ProblemType;
 import com.piece_framework.makegood.launch.elements.TestCase;
 import com.piece_framework.makegood.launch.elements.TestResult;
 import com.piece_framework.makegood.ui.Activator;
 import com.piece_framework.makegood.ui.Messages;
 
 public class TestResultView extends ViewPart {
     private static final String VIEW_ID = Activator.PLUGIN_ID + ".views.resultView"; //$NON-NLS-1$
     private static final String TERMINATE_ACTION_ID = Activator.PLUGIN_ID + ".viewActions.resultView.terminateTest"; //$NON-NLS-1$
     private static final String RERUN_ACTION_ID = Activator.PLUGIN_ID + ".viewActions.resultView.rerunTest"; //$NON-NLS-1$
     private static final String CONTEXT_ID = Activator.PLUGIN_ID + ".contexts.resultView"; //$NON-NLS-1$
 
     private MakeGoodProgressBar progressBar;
     private Label tests;
     private ResultLabel passes;
     private ResultLabel failures;
     private ResultLabel errors;
     private TreeViewer resultTreeViewer;
     private Label rate;
     private Label average;
     private ShowTimer showTimer;
     private IAction terminateAction;
     private IAction rerunAction;
     private FailureTrace failureTrace;
 
     private ViewerFilter failureFilter = new ViewerFilter() {
         @Override
         public boolean select(Viewer viewer,
                               Object parentElement,
                               Object element
                               ) {
             if (!(element instanceof TestResult)) {
                 return false;
             }
 
             TestResult result = (TestResult) element;
             return result.hasFailure() || result.hasError();
         }
     };
 
     public TestResultView() {
         // TODO Auto-generated constructor stub
     }
 
     @Override
     public IViewSite getViewSite() {
         IViewSite site = super.getViewSite();
 
         // There is no hook point for disabling the actions...
         if (terminateAction == null) {
             IToolBarManager manager = site.getActionBars().getToolBarManager();
             ActionContributionItem terminateItem = (ActionContributionItem) manager.find(TERMINATE_ACTION_ID);
             if (terminateItem != null) {
                 terminateAction = terminateItem.getAction();
                 terminateAction.setEnabled(false);
             }
 
             ActionContributionItem rerunItem = (ActionContributionItem) manager.find(RERUN_ACTION_ID);
             if (rerunItem != null) {
                 rerunAction = rerunItem.getAction();
                 rerunAction.setEnabled(false);
             }
         }
 
         return site;
     }
 
     @Override
     public void createPartControl(final Composite parent) {
         IContextService service = (IContextService) getSite().getService(IContextService.class);
         service.activateContext(CONTEXT_ID); //$NON-NLS-1$
 
         parent.setLayout(new GridLayout(1, false));
 
         Composite progress = new Composite(parent, SWT.NULL);
         progress.setLayoutData(createHorizontalFillGridData());
         progress.setLayout(new GridLayout(3, false));
 
         rate = new Label(progress, SWT.LEFT);
         progressBar = new MakeGoodProgressBar(progress);
         progressBar.setLayoutData(createHorizontalFillGridData());
         average = new Label(progress, SWT.LEFT);
 
         Composite summary = new Composite(parent, SWT.NULL);
         summary.setLayoutData(createHorizontalFillGridData());
         summary.setLayout(new GridLayout(2, true));
 
         tests = new Label(summary, SWT.LEFT);
         tests.setLayoutData(createHorizontalFillGridData());
 
         Composite labels = new Composite(summary, SWT.NULL);
         labels.setLayoutData(createHorizontalFillGridData());
         labels.setLayout(new FillLayout(SWT.HORIZONTAL));
         passes = new ResultLabel(labels,
                                  Messages.TestResultView_passesLabel,
                                  Activator.getImageDescriptor("icons/pass-gray.gif").createImage() //$NON-NLS-1$
                                  );
         failures = new ResultLabel(labels,
                                    Messages.TestResultView_failuresLabel,
                                    Activator.getImageDescriptor("icons/failure-gray.gif").createImage() //$NON-NLS-1$
                                    );
         errors = new ResultLabel(labels,
                                  Messages.TestResultView_errorsLabel,
                                  Activator.getImageDescriptor("icons/error-gray.gif").createImage() //$NON-NLS-1$
                                  );
 
         SashForm form = new SashForm(parent, SWT.HORIZONTAL);
         form.setLayoutData(createBothFillGridData());
         form.setLayout(new GridLayout(2, false));
 
         Composite treeParent = new Composite(form, SWT.NULL);
         treeParent.setLayoutData(createHorizontalFillGridData());
         treeParent.setLayout(new GridLayout(1, false));
 
         Composite result = new Composite(treeParent, SWT.NULL);
         result.setLayoutData(createHorizontalFillGridData());
         result.setLayout(new RowLayout());
 
         new ResultLabel(result,
                         Messages.TestResultView_resultsLabel,
                         null
                         );
 
         Tree resultTree = new Tree(treeParent, SWT.BORDER);
         resultTree.setLayoutData(createBothFillGridData());
         resultTreeViewer = new TreeViewer(resultTree);
         resultTreeViewer.setContentProvider(new TestResultContentProvider());
         resultTreeViewer.setLabelProvider(new TestResultLabelProvider());
         resultTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
             @Override
             public void selectionChanged(SelectionChangedEvent event) {
                 failureTrace.clearText();
 
                 if (!(event.getSelection() instanceof IStructuredSelection)) {
                     return;
                 }
                 IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                 if (!(selection.getFirstElement() instanceof TestCase)) {
                     return;
                 }
 
                 TestCase testCase = (TestCase) selection.getFirstElement();
                 if (testCase.getProblem().getType() == ProblemType.Pass) {
                     return;
                 }
 
                 failureTrace.setText(testCase.getProblem().getContent());
             }
         });
 
         failureTrace = new FailureTrace(form);
 
         reset();
     }
 
     @Override
     public void setFocus() {
         // TODO Auto-generated method stub
     }
 
     public void reset() {
         rate.setText("  0 " +   //$NON-NLS-1$ 
                      Messages.TestResultView_percent +
                      "  "       //$NON-NLS-1$
                      );
         average.setText(" 0.000 " +      //$NON-NLS-1$
                         Messages.TestResultView_second +
                         " / " +         //$NON-NLS-1$
                         Messages.TestResultView_averageTest +
                         "  "            //$NON-NLS-1$
                         );
         tests.setText(Messages.TestResultView_testsLabel + " " + //$NON-NLS-1$
                       " 0/0 " + //$NON-NLS-1$
                       "(" +         //$NON-NLS-1$
                           Messages.TestResultView_realTime +
                           " 0.000 " +     //$NON-NLS-1$
                           Messages.TestResultView_second +
                           "," +     //$NON-NLS-1$
                       " " +         //$NON-NLS-1$
                           Messages.TestResultView_testTime +
                           " 0.000 " +     //$NON-NLS-1$
                           Messages.TestResultView_second +
                       ")" //$NON-NLS-1$
                       );
         passes.reset();
         failures.reset();
         errors.reset();
 
         progressBar.reset();
 
         resultTreeViewer.setInput(null);
     }
 
     public static TestResultView getView() {
         IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
         IViewPart view = page.findView(VIEW_ID);
         if (!(view instanceof TestResultView)) {
             return null;
         }
         return (TestResultView) view;
     }
 
     public static TestResultView showView() {
         IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
         IViewPart view = null;
         try {
             view = page.showView(VIEW_ID);
         } catch (PartInitException e) {
         }
         if (!(view instanceof TestResultView)) {
             return null;
         }
         return (TestResultView) view;
     }
 
     private GridData createHorizontalFillGridData() {
         GridData horizontalFillGrid = new GridData();
         horizontalFillGrid.horizontalAlignment = GridData.FILL;
         horizontalFillGrid.grabExcessHorizontalSpace = true;
         return horizontalFillGrid;
     }
 
     private GridData createBothFillGridData() {
         GridData bothFillGrid = new GridData();
         bothFillGrid.horizontalAlignment = GridData.FILL;
         bothFillGrid.verticalAlignment = GridData.FILL;
         bothFillGrid.grabExcessHorizontalSpace = true;
         bothFillGrid.grabExcessVerticalSpace = true;
         return bothFillGrid;
     }
 
     public void nextResult() {
         IStructuredSelection selection = (IStructuredSelection) resultTreeViewer.getSelection();
         TestResult selected = (TestResult) selection.getFirstElement();
 
         java.util.List<TestResult> results = (java.util.List<TestResult>) resultTreeViewer.getInput();
         if (results == null || results.size() == 0) {
             return;
         }
 
         if (selected == null) {
             selected = results.get(0);
         }
 
         TestResultSearch search = new TestResultSearch(results, selected);
         TestResult next = search.getNextFailure();
         if (next != null) {
             resultTreeViewer.expandAll();
             resultTreeViewer.setSelection(new StructuredSelection(next), true);
         }
     }
 
     public void previousResult() {
         IStructuredSelection selection = (IStructuredSelection) resultTreeViewer.getSelection();
         TestResult selected = (TestResult) selection.getFirstElement();
 
         java.util.List<TestResult> results = (java.util.List<TestResult>) resultTreeViewer.getInput();
         if (results == null || results.size() == 0) {
             return;
         }
 
         if (selected == null) {
             selected = results.get(0);
         }
 
         TestResultSearch search = new TestResultSearch(results, selected);
         TestResult previous = search.getPreviousFailure();
         if (previous != null) {
             resultTreeViewer.expandAll();
             resultTreeViewer.setSelection(new StructuredSelection(previous), true);
         }
     }
 
     public void filterResult(boolean filterOn) {
         if (filterOn) {
             resultTreeViewer.addFilter(failureFilter);
         } else {
             resultTreeViewer.removeFilter(failureFilter);
         }
     }
 
     public void setTreeInput(java.util.List<TestResult> suites) {
         resultTreeViewer.setInput(suites);
     }
 
     public boolean isSetTreeInput() {
         return resultTreeViewer.getInput() != null;
     }
 
     public void refresh(TestProgress progress, TestResult result) {
         rate.setText(String.format("%3d", progress.getRate()) +     //$NON-NLS-1$
                      " " +      //$NON-NLS-1$
                      Messages.TestResultView_percent +
                      "  "       //$NON-NLS-1$
                      );
         average.setText(String.format("%.3f", progress.getAverage()) +      //$NON-NLS-1$
                         " " +       //$NON-NLS-1$
                         Messages.TestResultView_second +
                         " / " +     //$NON-NLS-1$
                         Messages.TestResultView_averageTest +
                         "  "        //$NON-NLS-1$
                         );
 
         showTimer.show();
         passes.setCount(progress.getPassCount());
         failures.setCount(progress.getFailureCount());
         errors.setCount(progress.getErrorCount());
 
         boolean raiseErrorOrFailure = progress.getErrorCount() > 0
                                       || progress.getFailureCount() > 0;
         if (raiseErrorOrFailure) {
             progressBar.raisedError();
         }
         progressBar.worked(progress.getRate());
 
         if (result != null) {
             resultTreeViewer.expandAll();
             resultTreeViewer.setSelection(new StructuredSelection(result));
         }
         resultTreeViewer.refresh();
     }
 
     public void start(TestProgress progress) {
         showTimer = new ShowTimer(tests, progress, 200);
         showTimer.start();
 
         terminateAction.setEnabled(true);
         rerunAction.setEnabled(false);
     }
 
     public void terminate() {
         showTimer.terminate();
 
         terminateAction.setEnabled(false);
         rerunAction.setEnabled(true);
     }
 
     private class ResultLabel {
         private CLabel label;
         private String text;
 
         private ResultLabel(Composite parent, String text, Image icon) {
             label = new CLabel(parent, SWT.LEFT);
             label.setText(text);
             if (icon != null) {
                 label.setImage(icon);
             }
 
             this.text = text;
         }
 
         private void setCount(int count) {
             label.setText(text + " " + count); //$NON-NLS-1$
         }
 
         private void reset() {
             label.setText(text);
         }
     }
 
     private class MakeGoodProgressBar extends Composite {
         private final RGB GREEN = new RGB(95, 191, 95);
         private final RGB RED = new RGB(159, 63, 63);
         private final RGB NONE = new RGB(255, 255, 255);
 
         private Label bar;
         private int rate;
 
         private MakeGoodProgressBar(Composite parent) {
             super(parent, SWT.BORDER);
             GridLayout layout = new GridLayout();
             layout.marginTop = 0;
             layout.marginBottom = 0;
             layout.marginLeft = 0;
             layout.marginRight = 0;
             layout.marginHeight = 0;
             layout.marginWidth = 0;
             layout.verticalSpacing = 0;
             layout.horizontalSpacing = 0;
             setLayout(layout);
 
             setBackground(new Color(parent.getDisplay(), NONE));
 
             bar = new Label(this, SWT.NONE);
             bar.setLayoutData(new GridData());
 
             ControlAdapter listener = new ControlAdapter() {
                 @Override
                 public void controlResized(ControlEvent e) {
                     worked(rate);
                 }
             };
             bar.addControlListener(listener);
             addControlListener(listener);
 
             reset();
         }
 
         private void worked(int rate) {
             int maxWidth = getSize().x;
 
             int width = bar.getSize().x;
             if (rate < 100) {
                 width = (int) (maxWidth * ((double) rate / 100d));
             } else if (rate >= 100) {
                 width = maxWidth;
             }
             final int barWidth = width;
 
             getDisplay().asyncExec(new Runnable() {
                 @Override
                 public void run() {
                     Point size = bar.getSize();
                     size.x = barWidth;
                     bar.setSize(size);
                 }
             });
 
             this.rate = rate;
         }
 
         private void raisedError() {
             bar.setBackground(new Color(getDisplay(), RED));
         }
 
         private void reset() {
             worked(0);
 
             bar.setBackground(new Color(getDisplay(), GREEN));
         }
     }
 
     private class ShowTimer implements Runnable {
         private Label tests;
         private TestProgress progress;
         private int delay;
         private long startTime;
         private boolean terminate;
         private double elapsedTime;
 
         private ShowTimer(Label tests,
                           TestProgress progress,
                           int delay
                           ) {
             this.tests = tests;
             this.progress = progress;
             this.delay = delay;
         }
 
         private void start() {
             startTime = System.currentTimeMillis();
             schedule();
         }
 
         private void terminate() {
             terminate = true;
         }
 
         private void schedule() {
             tests.getDisplay().timerExec(delay, this);
         }
 
         private void show() {
             tests.setText(Messages.TestResultView_testsLabel + " " + //$NON-NLS-1$
                           progress.getEndTestCount() + "/" + //$NON-NLS-1$
                           progress.getAllTestCount() + " " + //$NON-NLS-1$
                           "(" +         //$NON-NLS-1$
                               Messages.TestResultView_realTime +
                               " " +     //$NON-NLS-1$
                               String.format("%.3f", elapsedTime) +      //$NON-NLS-1$
                               " " +     //$NON-NLS-1$
                               Messages.TestResultView_second +
                               "," +     //$NON-NLS-1$
                           " " +         //$NON-NLS-1$
                               Messages.TestResultView_testTime +
                               " " +     //$NON-NLS-1$
                               String.format("%.3f", progress.getTotalTime()) +      //$NON-NLS-1$
                               " " +     //$NON-NLS-1$
                               Messages.TestResultView_second +
                           ")" //$NON-NLS-1$
                           );
         }
 
         @Override
         public void run() {
             elapsedTime = (System.currentTimeMillis() - startTime) / 1000d;
             show();
 
             if (!terminate) {
                 schedule();
             }
         }
     }
 
     private class FileWithLineRange extends StyleRange {
         Integer line;
     }
 
     private class InternalFileWithLineRange extends FileWithLineRange {
         IFile file;
     }
 
     private class ExternalFileWithLineRange extends FileWithLineRange {
         IFileStore fileStore;
     }
 
     private class FailureTrace implements MouseListener, MouseMoveListener {
         private StyledText text;
         private Cursor handCursor;
         private Cursor arrowCursor;
         private Vector<FileWithLineRange> ranges;
 
         public FailureTrace(Composite parent) {
             Composite traceParent = new Composite(parent, SWT.NULL);
             traceParent.setLayoutData(createHorizontalFillGridData());
             traceParent.setLayout(new GridLayout(1, false));
             Composite trace = new Composite(traceParent, SWT.NULL);
             trace.setLayoutData(createHorizontalFillGridData());
             trace.setLayout(new RowLayout());
             new ResultLabel(
                 trace,
                 Messages.TestResultView_failureTraceLabel,
                 Activator.getImageDescriptor("icons/failure-trace.gif").createImage() //$NON-NLS-1$
             );
             text = new StyledText(
                        traceParent,
                        SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL
                    );
             text.setLayoutData(createBothFillGridData());
             text.setEditable(false);
             text.addMouseListener(this);
             text.addMouseMoveListener(this);
 
             handCursor = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);
             arrowCursor = new Cursor(parent.getDisplay(), SWT.CURSOR_ARROW);
         }
 
         public void setText(String text) {
             this.text.setText(text);
             generatelinks(text);
         }
 
         private void generatelinks(String text) {
             ranges = new Vector<FileWithLineRange>();
             Matcher matcher = Pattern.compile(
                                   "^(.+):(\\d+)$", //$NON-NLS-1$
                                   Pattern.MULTILINE
                                       ).matcher(text);
             while (matcher.find()) {
                 IFile[] files;
                 try {
                     files = ResourcesPlugin.getWorkspace().getRoot()
                             .findFilesForLocationURI(
                                     new URI("file:///" + matcher.group(1))); //$NON-NLS-1$
                 } catch (URISyntaxException e) {
                     Activator.getDefault().getLog().log(
                         new Status(
                             IStatus.WARNING,
                             Activator.PLUGIN_ID,
                             0,
                             e.getMessage(),
                             e
                         )
                     );
                     continue;
                 }
 
                 FileWithLineRange range;
                 if (files.length > 0) {
                     InternalFileWithLineRange iRange =
                         new InternalFileWithLineRange();
                     iRange.file = files[0];
                     iRange.foreground =
                         this.text.getDisplay().getSystemColor(SWT.COLOR_BLUE);
                     range = (FileWithLineRange) iRange;
                 } else {
                     ExternalFileWithLineRange eRange =
                         new ExternalFileWithLineRange();
                     try {
                         eRange.fileStore =
                             EFS.getLocalFileSystem()
                                .getStore(new URI("file:///" + matcher.group(1))); //$NON-NLS-1$
                     } catch (URISyntaxException e) {
                         Activator.getDefault().getLog().log(
                             new Status(
                                 IStatus.WARNING,
                                 Activator.PLUGIN_ID,
                                 0,
                                 e.getMessage(),
                                 e
                             )
                         );
                         continue;
                     }
 
                     eRange.foreground = new Color(
                                             this.text.getDisplay(),
                                             114, 159, 207
                                         );
                     range = (FileWithLineRange) eRange;
                 }
 
                range.start = matcher.start();
                range.length = matcher.group().length();
                 range.line = Integer.valueOf(matcher.group(2));
                 ranges.add(range);
                 setRange(range);
             }
         }
 
         public void clearText() {
             text.setText(""); //$NON-NLS-1$
         }
 
         public void setRange(StyleRange range) {
             text.setStyleRange(range);
         }
 
         public void mouseDoubleClick(MouseEvent e) {}
 
         public void mouseDown(MouseEvent event) {
             FileWithLineRange range =
                 findFileWithLineRange(new Point(event.x, event.y));
             if (range == null) {
                 return;
             }
 
             try {
                 IEditorPart editorPart =
                     openEditor(
                         PlatformUI.getWorkbench()
                                   .getActiveWorkbenchWindow()
                                   .getActivePage(),
                         range
                     );
                 gotoLine((ITextEditor) editorPart, range.line);
             } catch (PartInitException e) {
                 Activator.getDefault().getLog().log(
                     new Status(
                         IStatus.WARNING,
                         Activator.PLUGIN_ID,
                         0,
                         e.getMessage(),
                         e
                     )
                 );
             } catch (BadLocationException e) {
                 Activator.getDefault().getLog().log(
                     new Status(
                         IStatus.WARNING,
                         Activator.PLUGIN_ID,
                         0,
                         e.getMessage(),
                         e
                     )
                 );
             }
         }
 
         public void mouseUp(MouseEvent e) {}
 
         public void mouseMove(MouseEvent event) {
             FileWithLineRange range =
                 findFileWithLineRange(new Point(event.x, event.y));
             if (range != null) {
                 text.setCursor(handCursor);
             } else {
                 text.setCursor(arrowCursor);
             }
         }
 
         private FileWithLineRange findFileWithLineRange(Point point) {
             int offset;
             try {
                 offset = text.getOffsetAtLocation(point);
             } catch (IllegalArgumentException e) {
                 return null;
             }
 
             for (int i = 0; i < ranges.size(); ++i) {
                 FileWithLineRange range = ranges.get(i);
                 int startOffset = range.start;
                 int endOffset = startOffset + range.length;
                 if (offset >= startOffset && offset <= endOffset) {
                     return range;
                 }
             }
 
             return null;
         }
 
         private void gotoLine(ITextEditor editor, Integer line)
             throws BadLocationException {
             IRegion region;
             region = editor.getDocumentProvider()
                            .getDocument(editor.getEditorInput())
                            .getLineInformation(line - 1);
             editor.selectAndReveal(region.getOffset(), region.getLength());
         }
 
         private IEditorPart openEditor(
                 IWorkbenchPage page, FileWithLineRange range
             ) throws PartInitException {
             if (range instanceof InternalFileWithLineRange) {
                 return IDE.openEditor(
                             page, ((InternalFileWithLineRange) range).file
                         );
             } else if (range instanceof ExternalFileWithLineRange) {
                 return IDE.openEditorOnFileStore(
                             page, ((ExternalFileWithLineRange) range).fileStore
                         );
             } else {
                 return null;
             }
         }
     }
 }
