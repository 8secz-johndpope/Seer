 import java.io.*;  
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 //TO LOOK INTO:
 //http://www.mathworks.com/support/solutions/en/data/1-9389FH/index.html?product=ML&solution=1-9389FH
 
 public class NEURON_reader {
 
 	BufferedInputStream pin;
 	FileInputStream perr;
 	Process p;
 
 	//CONSTANTS
 	//-----------------------------------------------------------------------
 	public static final String term_string_const = String.format("\n<oc>\n");
 	public static final Pattern pattern = Pattern.compile("\n(oc>)*");
 
 	//??? Use StringBuilder????
 	StringBuffer input_data = new StringBuffer(2000);
 	StringBuffer error_data = new StringBuffer(2000);
 	byte[] temp_data  = new byte[2000];
 
 	//Set by class 
 	public boolean success_flag           = false;   //aka 'result' or 'success flag'
 	public boolean detected_end_statement = false;   //Set true if we detect the terminal string
 	public boolean stackdump_present      = false; 	 //Set true if stackdump detected
 	public boolean process_running        = false;   //Set false if process is no longer running
 	public boolean read_timeout 	      = false; 	 //Set true if the reading failed due to a timeout
 	public String  result_str             = new String();
 
 	//For repeated calls
 	long start_time;
 	long wait_time_nanoseconds;
 	boolean debug;
 	boolean allow_timeout;
 
 	//We start everything in Matlab and pass in the relevant objects here ...
 	public NEURON_reader(BufferedInputStream pin, FileInputStream perr, Process p) {
 		this.p    = p;
 		this.perr = perr;
 		this.pin  = pin;
 	}
 
 	public String getCurrentInputString(){
 		return input_data.toString();
 	}
 	
 	public String getCurrentErrorString(){
 		return error_data.toString();
 	}
 	
 	public void init_read(long wait_time_seconds, boolean debug_input)
 	{
 		//Initialization ...
 		input_data.setLength(0);
 		error_data.setLength(0);
 		read_timeout = false;
 		stackdump_present = false;
 		detected_end_statement = false;
 		success_flag = false;
 
 		start_time = System.nanoTime();
 		debug = debug_input;
 		allow_timeout = wait_time_seconds != -1;
 		wait_time_nanoseconds = (long) (wait_time_seconds*1e9);
 	}
 
 	public boolean read_result() throws IOException
 	{
 
 		//RETURNED VALUE SHOULD BE WHETHER OR NOT TO STOP ...
 
 		//NOTE: I couldn't figure out how to interrupt
 		//So I call this function a bunch of times from Matlab ... :/
 
 		int n_bytes_available;
 
 		boolean is_terminal_string = false;
 
 		//OUTLINE
 		//------------------------------------------------------------------
 		//1 Check if process is running
 		//2 Check timing
 		//3 Read input
 		//4 Read error
 		//5 Brief pause ????
 
 		//PROCESS RUNNING CODE
 		//---------------------------------------------------
 		//NOTE: Asking a process for its exit value will throw an error if it is still running
 		//I don't know of any other way to ask if the process is still valid ...
 		try {
 			p.exitValue();
			process_running = false;
			System.err.println("NEURON process Exited");
			return true;
 		} catch (IllegalThreadStateException e) {
 			process_running = true;
 		}
 
		//TIME CHECKING
 		//---------------------------------------------------
 		if (allow_timeout && ((System.nanoTime() - start_time) > wait_time_nanoseconds)) {
 			read_timeout = true;
 			System.err.println("Reading from NEURON timed out");
 			return true;
 		}
 
 		//READING ERROR
 		//---------------------------------------------------
 		n_bytes_available = perr.available();
 		if (n_bytes_available > 0){
 			perr.read(temp_data,0,n_bytes_available);
			readStream(n_bytes_available, debug, false);
 			//NOTE: We'll never get the terminal string from the error stream
 			//Don't assign variable from function call..
 		}
 
 		//READING INPUT
 		//---------------------------------------------------
 		n_bytes_available = pin.available();
 		if (n_bytes_available > 0){
 			pin.read(temp_data,0,n_bytes_available);
 			is_terminal_string = readStream(n_bytes_available, debug, true);
 		}
 		
 		if (is_terminal_string){
 			detected_end_statement = true;
 
 			//SETTING THE FINAL STRING
 			//--------------------------------------------------------------------------
 			if (error_data.length() > 0){
 				success_flag = false;
 				//NOTE: We'll Ignore partially good strings for now ...
 				/*
 	        if obj.temp_stdout_index > 0
             obj.partial_good_str = obj.temp_stdout_str(1:obj.temp_stdout_index-1);
 	        end
 				 */
 				result_str = error_data.toString();
 			}else {
 				success_flag = true;
 				result_str = input_data.toString();
 			}
 		}
 		
 		return is_terminal_string;
 	}
 
 	private boolean isStackdumpPresent(String temp_string, boolean is_success){
 
 		//NOTE: This code is a bit messy, essentially we look for a particular string
 		//in the current error string. 
 
 		boolean potential_stackdump;
 		String  possible_error_string;
 		boolean stackdump_present = false;
 
 		potential_stackdump = is_success && error_data.length() > 0;
 		if (potential_stackdump){
 			possible_error_string = error_data.toString();
 			stackdump_present     = possible_error_string.lastIndexOf("Dumping stack trace to") != -1;
 			if (stackdump_present){
 				System.err.printf("STACKDUMP ERROR MESSAGE:\n%s\n",possible_error_string);
 			}
 		}
 
 		return stackdump_present;
 	}
 
 	private String cleanString(int n_bytes_available){
 
 		Matcher matcher;
 		String temp_string;
 
 		//Add newline to facilitate oc> matching ...
 		//Convert from bytes to string ...
 		temp_string = "\n" + new String(temp_data,0,n_bytes_available);
 
 		//Replace with a replacement of \noc>oc>* with just the newline.
 		matcher     = pattern.matcher(temp_string);
 		temp_string = matcher.replaceAll("\n");
 
 		//NOTE: I need to remove the first newline since I added it to facilitate matching
 		return temp_string.substring(1);
 	}
 
 	private boolean readStream(int n_bytes_available, boolean debug, boolean is_input_string){
 
 		String temp_string;
 		int index_term_string_match;
 		boolean is_terminal_string = false;
 
 		//Bytes to string
 		//------------------------------------------------------------
 		temp_string = cleanString(n_bytes_available);
 
 		//Check if the terminal string is present
 		//If it is, trim it out of the result ...
 		//------------------------------------------------------------
 		if (is_input_string){
 			index_term_string_match = temp_string.lastIndexOf(term_string_const);
 			if (index_term_string_match >= 0){
 				is_terminal_string = true;
 				//Remove the terminal string ...
 				if (index_term_string_match == 0){
 					temp_string = new String("");
 				}else{
 					temp_string = temp_string.substring(0,index_term_string_match-1);
 				}
 			}
 		}
 
 		//Print out things if debugging ...
 		if (debug && temp_string.length() > 0){
 			if (is_input_string){
 				System.out.println(temp_string);
 			} else {
 				System.err.println(temp_string);
 			}
 		}
 
 		if (is_terminal_string){
 			//NOTE: We will eventually remove this ...
 			//System.out.println("Terminal String Detected");
 		}else{
 			if (isStackdumpPresent(temp_string,is_input_string)){
 				stackdump_present = true;
 			}
 		}
 
 		//String copying to buffer ...
 		if (is_input_string){
 			input_data.append(temp_string);
 		} else {
 			error_data.append(temp_string);
 		}
 		return is_terminal_string;
 	}
 
 }
