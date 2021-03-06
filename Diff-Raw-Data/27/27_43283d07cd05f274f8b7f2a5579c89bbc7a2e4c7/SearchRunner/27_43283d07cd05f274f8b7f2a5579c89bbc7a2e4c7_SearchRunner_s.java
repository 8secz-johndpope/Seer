 package net.intelie.lognit.cli.runners;
 
 import net.intelie.lognit.cli.Runner;
 import net.intelie.lognit.cli.UserConsole;
 import net.intelie.lognit.cli.UserOptions;
 import net.intelie.lognit.cli.http.RestListenerHandle;
 import net.intelie.lognit.cli.model.Lognit;
 import net.intelie.lognit.cli.state.Clock;
 
 import java.io.IOException;
 
 public class SearchRunner implements Runner {
     public static final String HANDSHAKE = "INFO: handshake (%dms)";
 
     private final UserConsole console;
     private final Lognit lognit;
     private final BufferListenerFactory factory;
     private final Clock clock;
 
     public SearchRunner(UserConsole console, Lognit lognit, BufferListenerFactory factory, Clock clock) {
         this.console = console;
         this.lognit = lognit;
         this.factory = factory;
         this.clock = clock;
     }
 
     @Override
    public int run(UserOptions options) throws IOException {
         BufferListener listener = factory.create(options.getFormat(), options.isVerbose());
 
         RestListenerHandle handle = handshake(options, listener);
 
        listener.waitHistoric(options.getTimeoutInMilliseconds(), options.getLines());
        if (options.isFollow()) {
            listener.releaseAll();
            console.waitChar('q');
         }
        handle.close();
 
         return 0;
     }
 
     private RestListenerHandle handshake(UserOptions options, BufferListener listener) throws IOException {
         long start = clock.currentMillis();
         RestListenerHandle handle = lognit.search(options.getQuery(), options.getLines(), listener);
         if (options.isVerbose())
             console.println(HANDSHAKE, clock.currentMillis() - start);
         return handle;
     }
 }
