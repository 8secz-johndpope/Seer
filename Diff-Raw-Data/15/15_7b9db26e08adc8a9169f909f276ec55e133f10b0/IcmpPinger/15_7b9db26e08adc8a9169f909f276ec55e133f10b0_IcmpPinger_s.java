 import java.util.*;
 import java.util.regex.*;
 import java.net.*;
 
 /**
    This class launches an external ping command and returns the collected
    times.
 
    @author   Steven Pigeon <pigeon@iro.umontreal.ca>
 */
 public class IcmpPinger implements Prober {
     /** Holds the last collected times */
     private String m_icmp_times;
 
     /** A reference to ClientInfo */
     private ClientInfo m_info;
 
     /** Describes OS-specific commands for external ping command */
     private static final String[][] OS_SPECIFIC_COMMAND =  {
         // The number of pings parameter must be LAST because it is
         // appended at run-time using ClientInfo.getNumberOfPings()
         //
         // Linux
         {"ping", "-w", "30", "-W", "2", "-c"},
         // BSD, OSX ?
         {"ping", "-c"},
         // Windows XP        
         {"ping", "-w", "2000", "-n"}
     };
 
     /** OS-specific regexes to get the summary line */
     private static final String[][] OS_SPECIFIC_SUMMARY_REGEX = {
         // On *nixes: "7 packets transmitted, 6 received, 14% packet loss, time 6014ms"
         {"(([0-9]+)\\s[\\w|\\s]+),\\s(([0-9]+)\\s[\\w|\\s]+),.*(time)\\s(.*)", "$2 $4 $6"},
 
         // On English Windows XP: " Packets: Sent = 10, Received = 9, Lost = 1 (10% loss)"
 	// On French Windows 7: "     Paquets : envoyés = 3, reçus = 0, perdus = 3 (perte 100%),"
 	// To be resistent to language, we make a very generic pattern. Hopefully, they don't swap numbers!
         {"[^=]+=\\s+(\\d+), [^=]+=\\s+(\\d+),.*", "$1 $2 ?ms"}
     };
 
     public String getLastProbe() { return m_icmp_times; }
 
     public void clearProbe() { m_icmp_times = ""; }
 
     /**
        Returns the ping summary (how many sent, how many received, and if
        possible the time in milliseconds elapsed)
 
        @return The ping summary
     */
     public static String getSummary(String[] summary_regex,
                                     List<String> stdout_lines) {
 	Pattern summary = Pattern.compile(summary_regex[0]);
         for (String s : stdout_lines)
             if (summary.matcher(s).matches())
                 return s.replaceAll(summary_regex[0], summary_regex[1]);
 
 	if(stdout_lines.size() == 0){
 	    return "ping have no output";
 	}
 
 	String str = stdout_lines.get(0);
         for (String s : stdout_lines){
 	    if (s.length() > 0 && s.charAt(0) == ' ' && s.contains("=")){
 		str = s;
 		break;
 	    }
 	}
         return "ping summary not found. The closest output line is : '" + str + "'";
     }
     /**
        @return The ping times
     */
     public static String getTimes(List<String> stdout_lines) {
 	String ret = "";
 	Pattern times = Pattern.compile(".*\\s(time|temps)(=|<)[0-9]+.*"); // osx/bsd/nunux/windows?
	System.out.println("getTimes");
 	for (String s : stdout_lines)
 	    if (times.matcher(s).matches())
 		// Regex to match times, probably good for all
 		// variants.
 		//
 		// .* matches stuff before
 		// time= or time< (if time<, it goes in the number) (group 1 and 2)
 		// (<?[0-9]+(\\.[0-9]+)?) matches an int or float  (group 3 and 4)
 		// (\\ ?) matches an optional whitespace (group 5)
 		// (\\S+) matches the unit (ms, s, etc) (group 6)
 		// .* matches the trailing crap, if any (group 7)
 		//
 		if (s.contains("time")){
                     ret += " " + s.replaceAll("(.*time(=?))(<?[0-9]+(\\.[0-9]+)?)(\\ ?)(\\S+).*", "$3$6");
 		}else if (s.contains("temps")){
 		    ret += " " + s.replaceAll("(.*temps(=?))(<?[0-9]+(\\.[0-9]+)?)(\\ ?)(\\S+).*", "$3$6");
 		}
 	if(ret.length() == 0){
 	    for (String s : stdout_lines){
 		System.out.println(s);
 	    }
 	}
 	return ret;
 
     }
     public int probe(InetAddress addr) throws InterruptedException {
         String[] specific_command;
         String[] summary_regex;
 
         // does the OS-specific setup
         switch (m_info.getOS()) {
         case BSD:
         case OSX:
             specific_command = OS_SPECIFIC_COMMAND[1];
             summary_regex = OS_SPECIFIC_SUMMARY_REGEX[0]; // FIXME: cross-check
             break;
 
         case WinXP:
         case Win7:
         case WinOther:
             specific_command = OS_SPECIFIC_COMMAND[2];
             summary_regex = OS_SPECIFIC_SUMMARY_REGEX[1];
             break;
 
         case Linux:
         default:
             specific_command = OS_SPECIFIC_COMMAND[0];
             summary_regex = OS_SPECIFIC_SUMMARY_REGEX[0];
             break;
         }
 
         ArrayList<String> command = new ArrayList<String>();
         for (String s : specific_command)
             command.add(s); // copy the params
         command.add(Integer.toString(m_info.getNumberOfPings()));
         command.add(addr.toString().split("/")[1]); // append addr (as string) to ping
 
         LinkedList<String> stdout_lines = new LinkedList<String>();
         LinkedList<String> stderr_lines = new LinkedList<String>();
   
         // FIXME: Check if 20 is always ok for ping.
         int retval = Launcher.launch(command, stdout_lines, stderr_lines, 20);
 
         m_icmp_times = "ICMP " + addr.toString().split("/")[1] + " "; // the protocol+target
         if (retval==0) {
             // Success! Scan for summary.
             //
             m_icmp_times += getSummary(summary_regex,stdout_lines);
 
             // Scan output for times
             //
 	    m_icmp_times += getTimes(stdout_lines);
         }
         else
             // Deal with errors
             if (retval == 1)
                 // find summary anyway
                 m_icmp_times += getSummary(summary_regex, stdout_lines);
             else
                 m_icmp_times += "failed " + String.valueOf(retval);
 
         return retval; // m_icmp_times may or may not be assigned!
     }
 
     /**
        Creates an IcmpPinger (linked to a ClientInfo configuration)
        @param this_info A reference to a ClientInfo
        @see ClientInfo
     */
     public IcmpPinger(ClientInfo this_info) {
         m_icmp_times = "";
         m_info = this_info;
     }
 
     public static void main(String args[]) throws InterruptedException {
	
 	// Tests getSummary on Windows regex as there is difference in output depending of the OS language.
 	String[] to_test= {" Packets: Sent = 10, Received = 9, Lost = 1 (10% loss)",
 			   "     Paquets : envoyes = 10, recus = 9, perdus = 1 (perte 10%),",
 			   "     Paquets : envoyés = 10, recus = 9, perdus = 1 (perte 10%),",
 			   "     Paquets : envoyes = 10, reçus = 9, perdus = 1 (perte 10%),",
 			   "     Paquets : envoyés = 10, reçus = 9, perdus = 1 (perte 10%),",
 	};
         ArrayList<String> out = new ArrayList<String>();
 	for (int i = 0; i < to_test.length; i++){
 	    out.add(to_test[i]);
 	    System.out.println(IcmpPinger.getSummary(OS_SPECIFIC_SUMMARY_REGEX[1], out));
 	    out.remove(0);
 	}
 
 	// Test getTimes
 	String[] to_test2 = {"64 bytes from 127.0.0.1: icmp_seq=1 ttl=64 time=0.024 ms", //linux
 			     "Réponse de 173.194.75.94 : octets=32 temps=48 ms TTL=48", //Windows7 French
 			     "Reply from 74.125.224.82: bytes=1500 time=70ms TTL=52"// Windows7 English
 	};
 	for (int i = 0; i < to_test2.length; i++){
 	    out.add(to_test2[i]);
 	    System.out.println(IcmpPinger.getTimes(out));
 	    out.remove(0);
 	}
 
 	return;
     }
 }
