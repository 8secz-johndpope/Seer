 package org.ic.tennistrader.score;
 
 import java.util.ArrayList;
 import org.apache.log4j.Logger;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.swt.widgets.TableColumn;
 import org.ic.tennistrader.Main;
 import org.ic.tennistrader.domain.EventBetfair;
 import org.ic.tennistrader.domain.EventMarketBetfair;
 import org.ic.tennistrader.domain.match.HistoricalMatch;
 import org.ic.tennistrader.domain.match.Match;
 import org.ic.tennistrader.domain.match.Player;
 import org.ic.tennistrader.domain.match.PlayerEnum;
 import org.ic.tennistrader.domain.match.RealMatch;
 import org.ic.tennistrader.domain.match.Score;
 import org.ic.tennistrader.domain.match.Statistics;
 
 public class PredictionGui {
 
     private static Logger log = Logger.getLogger(Main.class);
 
     private Composite composite;
 
     private TableColumn[] columns;
 
     private ScoreUpdateThread scoreUpdateThread;
 
     private StatisticsUpdateThread statisticsUpdateThread;
         
     /**
      * For running the prediction gui separately
      */
     public static void main(String args[]) {
         final Display display = new Display();
         Shell shell = new Shell(display, SWT.SHELL_TRIM);
         shell.setLayout(new FillLayout());
 
         Player playerOne = new Player("Pedro", "Sousa");
         Player playerTwo = new Player("Joao", "Souza");
 
         Match match = new RealMatch("","", new EventBetfair("Sousa v Souza", new ArrayList<EventMarketBetfair>(), 1));
 
         new PredictionGui(shell, match);
 
         shell.open();
 
         while (!shell.isDisposed()) {
             if (!display.readAndDispatch())
                 display.sleep();
         }
         
         display.dispose();
     }
 
     public PredictionGui(final Composite parent, Match match) {
     	
     	this.composite = new Composite(parent, SWT.BORDER);
         composite.setLayout(new GridLayout());
          
     	ScorePanel sc = new ScorePanel(this.composite, match);
         StatisticsPanel st = new StatisticsPanel(parent, match);
     	
         this.statisticsUpdateThread = new StatisticsUpdateThread(match, st);
 
         this.scoreUpdateThread = new ScoreUpdateThread(match, sc);
 
 
         parent.getDisplay().timerExec(5000, new Runnable() {
             @Override
             public void run() {
             	statisticsUpdateThread.checkStatisticsUpdate();
                 if (!statisticsUpdateThread.isStatisticsPopulated())
                     parent.getDisplay().timerExec(5000, this);
             }
         });
 
         if (match.isInPlay()) {
             // only start score fetching for live matches
             parent.getDisplay().timerExec(5000, new Runnable() {
 
                 @Override
                 public void run() {
                     scoreUpdateThread.handleUpdate();
                     parent.getDisplay().timerExec(5000, this);
                 }
             });
 
             scoreUpdateThread.start();
         }
         statisticsUpdateThread.start();
         
         Score score = scoreUpdateThread.getScore();
         Statistics playerOneStats = statisticsUpdateThread.getPlayerOneStats();
         Statistics playerTwoStats = statisticsUpdateThread.getPlayerOneStats();
     	PlayerEnum server = sc.getServer();	
         
        PredictionCalculator predict = new PredictionCalculator(score, playerOneStats, playerTwoStats, server);
        predict.calculate();
         ProbabilityPanel probabilityPanel = new ProbabilityPanel(this.composite);

        statisticsUpdateThread.start();
     }   
 }
