 package edu.sc.seis.sod.subsetter.channel;
 
 import java.io.FileNotFoundException;
 import org.w3c.dom.Element;
 import edu.iris.Fissures.IfNetwork.Channel;
 import edu.iris.Fissures.IfNetwork.ChannelId;
 import edu.iris.Fissures.IfNetwork.ChannelNotFound;
 import edu.iris.Fissures.IfNetwork.Instrumentation;
 import edu.iris.Fissures.network.ChannelIdUtil;
 import edu.sc.seis.fissuresUtil.cache.ProxyNetworkAccess;
 import edu.sc.seis.fissuresUtil.display.configuration.DOMHelper;
 import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
 import edu.sc.seis.fissuresUtil.sac.FissuresToSac;
 import edu.sc.seis.sod.velocity.PrintlineVelocitizer;
 
 
 /**
  * @author crotwell
  * Created on Jul 19, 2005
  */
 public class SacPoleZeroWriter  implements ChannelSubsetter {
 
     public SacPoleZeroWriter(Element config) {
         template = DOMHelper.extractText(config,
                                          "poleZeroFileTemplate",
                                          DEFAULT_TEMPLATE);
     }
 
     public boolean accept(Channel chan, ProxyNetworkAccess network) throws Exception {
         try {
             ChannelId channel_id = chan.get_id();
             Instrumentation inst = network.retrieve_instrumentation(channel_id,
                                                                     channel_id.begin_time);
             String response = FissuresToSac.getPoleZero(inst.the_response).toString();
             velocitizer.evaluate(template, response, chan);
         } catch(ChannelNotFound ex) {
             GlobalExceptionHandler.handle("Channel not found: "
                     + ChannelIdUtil.toString(chan.get_id()), ex);
             return false;
         } catch(FileNotFoundException fe) {
             GlobalExceptionHandler.handle("Error while response file for "
                     + ChannelIdUtil.toString(chan.get_id()), fe);
             return false;
         }
         return true;
     }
 
    public static final String DEFAULT_TEMPLATE = "polezero/$network.code-$station.code-${channel.code}.sacpz";
 
     private String template;
 
     private PrintlineVelocitizer velocitizer = new PrintlineVelocitizer();
 }
