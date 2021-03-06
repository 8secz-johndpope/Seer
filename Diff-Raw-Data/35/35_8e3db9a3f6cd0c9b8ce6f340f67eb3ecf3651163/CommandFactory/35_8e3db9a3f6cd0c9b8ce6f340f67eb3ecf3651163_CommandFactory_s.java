 package org.dcache.services.ssh2;
 
 import org.apache.sshd.common.Factory;
 import org.apache.sshd.server.Command;
 
 import java.io.File;
 
 import dmg.cells.nucleus.CellEndpoint;
 
 public class CommandFactory implements Factory<Command> {
 
     private final CellEndpoint _cellEndpoint;
     private final String _userName;
     private final File _historyFile;
 
     public CommandFactory(String username, CellEndpoint cellEndpoint,
            File historyFile) {
         _cellEndpoint = cellEndpoint;
         _userName = username;
         _historyFile = historyFile;
     }
 
     @Override
     public Command create() {
         return new ConsoleReaderCommand(_userName, _cellEndpoint,
                _historyFile);
     }
 }
