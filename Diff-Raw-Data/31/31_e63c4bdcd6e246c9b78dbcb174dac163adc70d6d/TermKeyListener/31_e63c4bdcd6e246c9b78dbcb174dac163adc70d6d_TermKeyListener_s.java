 package org.crsh.visualvm.listener;
 
 import org.crsh.cmdline.CommandCompletion;
 import org.crsh.cmdline.Delimiter;
 import org.crsh.cmdline.spi.ValueCompletion;
 import org.crsh.visualvm.CrashSwingController;
 
 import javax.swing.*;
 import java.awt.event.KeyEvent;
 import java.awt.event.KeyListener;
 import java.io.IOException;
 import java.util.Map;
 
 /**
  * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
  */
 public class TermKeyListener implements KeyListener {
 
   private final CrashSwingController controller;
 
   public TermKeyListener(CrashSwingController controller) {
 
     if (controller == null) {
       throw new NullPointerException();
     }
 
     this.controller = controller;
 
   }
 
   public void keyPressed(KeyEvent e) {
 
     final String value = controller.inputRead();
 
     switch (e.getKeyCode()) {
       case KeyEvent.VK_ENTER:
         e.consume();
         if (value.length() == 0) {
           return;
         }
         controller.appendTypedCommand(value);
         controller.historyAdd(value);
         controller.inputClear();
 
         //
         new SwingWorker<Void, Void>() {
           @Override
           protected Void doInBackground() throws Exception {
             controller.execute(value);
             return null;
           }
         }.execute();
 
         break;
 
       case KeyEvent.VK_UP:
         e.consume();
         controller.historyPrevious();
         break;
 
       case KeyEvent.VK_DOWN:
         e.consume();
         controller.historyNext();
         break;
 
       case KeyEvent.VK_TAB:
         e.consume();
 
         SwingUtilities.invokeLater(new Runnable() {
           public void run() {
             String prefix = controller.inputReadToCaret();
             CommandCompletion completion = controller.complete(prefix);
             ValueCompletion vc = completion.getValue();
             if (vc.isEmpty()) {
               return;
             }
 
             Delimiter delimiter = completion.getDelimiter();
             try {
               if (vc.getSize() == 1) {
                 StringBuilder sb = new StringBuilder();
                 delimiter.escape(vc.iterator().next().getKey(), sb);
                 sb.append(completion.getDelimiter().getValue());
                 controller.insertCompletion(sb.toString());
               } else {
                controller.candidatesClear();
                 for (Map.Entry<String, Boolean> entry : vc) {
                   StringBuilder sb = new StringBuilder();
                   sb.append(vc.getPrefix());
                   delimiter.escape(entry.getKey(), sb);
                   if (entry.getValue()) {
                     sb.append(completion.getDelimiter().getValue());
                   }
                   int start = value.length() - (controller.inputCaretPosition() - vc.getPrefix().length());
                  JMenuItem item = new JMenuItem(sb.toString().substring(start));
                  item.setSelected(true);
                  item.addActionListener(new CompletionActionListener(controller));
                  controller.candidatesAdd(item);
 
                 }
 
                 controller.candidatesShow();
                 
               }
             }
             catch (IOException ignore) {}
           }
         });
         break;
     }
   }
 
   public void keyTyped(KeyEvent e) {}
   public void keyReleased(KeyEvent e) {}
   
 }
