 package ch.usi.da.smr;
 /* 
  * Copyright (c) 2013 Università della Svizzera italiana (USI)
  * 
  * This file is part of URingPaxos.
  *
  * URingPaxos is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * URingPaxos is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with URingPaxos.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.net.Inet4Address;
 import java.net.Inet6Address;
 import java.net.InetAddress;
 import java.net.NetworkInterface;
 import java.net.SocketException;
 import java.util.ArrayList;
import java.util.Collections;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.Iterator;
import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
import java.util.Set;
 import java.util.Map.Entry;
 import java.util.Random;
 import java.util.concurrent.BlockingQueue;
 import java.util.concurrent.LinkedBlockingQueue;
 import java.util.concurrent.atomic.AtomicInteger;
 
 import ch.usi.da.smr.message.Command;
 import ch.usi.da.smr.message.CommandType;
 import ch.usi.da.smr.message.Message;
 import ch.usi.da.smr.transport.Receiver;
 import ch.usi.da.smr.transport.Response;
 import ch.usi.da.smr.transport.UDPListener;
 
 /**
  * Name: Client<br>
  * Description: <br>
  * 
  * Creation date: Mar 12, 2013<br>
  * $Id$
  * 
  * @author Samuel Benz <benz@geoid.ch>
  */
 public class Client implements Receiver {
 
 	private final PartitionManager partitions;
 				
 	private final UDPListener udp;
 	
 	private Map<Integer,Response> commands = new HashMap<Integer,Response>();
 
 	private Map<Integer,List<Command>> responses = new HashMap<Integer,List<Command>>();
 
 	private Map<Integer,List<String>> await_response = new HashMap<Integer,List<String>>();
 	
 	private Map<Integer, BlockingQueue<Response>> send_queues = new HashMap<Integer, BlockingQueue<Response>>();
 	
	// we need only one response per replica group
	Set<Integer> delivered = Collections.newSetFromMap(new LinkedHashMap<Integer, Boolean>(){
		private static final long serialVersionUID = -5674181661800265432L;
		protected boolean removeEldestEntry(Map.Entry<Integer, Boolean> eldest) {
	        return size() > 50000;
	    }
	});

 	private final InetAddress ip;
 	
 	private final int port;
 	
 	private final Map<Integer,Integer> connectMap;
 	
 	public Client(PartitionManager partitions,Map<Integer,Integer> connectMap) throws IOException {
 		this.partitions = partitions;
 		this.connectMap = connectMap;
 		ip = getHostAddress(false);
 		port = 5000 + new Random(Thread.currentThread().getId()).nextInt(15000);
 		udp = new UDPListener(port);
 		Thread t = new Thread(udp);
 		t.setName("UDPListener");
 		t.start();
 	}
 
 	public void init() {
 		udp.registerReceiver(this);		
 	}
 
 	public void readStdin() throws Exception {
 		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
 	    String s;
 	    try {
 	    	int id = 0;
 	    	Command cmd = null;
 		    while((s = in.readLine()) != null && s.length() != 0){
 		    	// read input
 		    	String[] line = s.split("\\s+");
 		    	if(line.length > 3){
 		    		try{
 		    			String arg2 = line[2];
 		    			if(arg2.equals(".")){ arg2 = ""; } // simulate empty string
 		    			cmd = new Command(id,CommandType.valueOf(line[0].toUpperCase()),line[1],arg2.getBytes(),Integer.parseInt(line[3]));
 		    		}catch (IllegalArgumentException e){
 		    			System.err.println(e.getMessage());
 		    		}
 		    	}else if(line.length > 2){
 		    		try{
 		    			cmd = new Command(id,CommandType.valueOf(line[0].toUpperCase()),line[1],line[2].getBytes());
 		    		}catch (IllegalArgumentException e){
 		    			System.err.println(e.getMessage());
 		    		}
 		    	}else if(line.length > 1){
 		    		try{
 		    			cmd = new Command(id,CommandType.valueOf(line[0].toUpperCase()),line[1],new byte[0]);
 		    		}catch (IllegalArgumentException e){
 		    			System.err.println(e.getMessage());
 		    		}
 		    	}else if(s.startsWith("start")){
 		    		System.out.println("Start load test");
 		    		final int concurrent_cmd = 20; // # of threads
 		    		final int send_per_thread = 50000;
 		    		final int value_size = 15000; // in bytes
 		    		final int key_count = 50000; // 50k * 15k byte memory needed at replica
 		    		final AtomicInteger send_id = new AtomicInteger(0); 
 		    		for(int i=0;i<concurrent_cmd;i++){
 		    			Thread t = new Thread("Command Sender " + i){
 							@Override
 							public void run(){
 								System.out.println("Start sender");
 								int send_count = 0;
 								while(send_count < send_per_thread){
 									Command cmd = new Command(send_id.incrementAndGet(),CommandType.PUT,"user" + (send_id.get() % key_count), new byte[value_size]);
 									Response r = null;
 									try{
 										//long time = System.nanoTime();
 										if((r = send(cmd)) != null){
 											r.getResponse(20000); // wait response
 											//System.out.println("latency: "  + (System.nanoTime() - time) + "ns");
 										}
 									} catch (Exception e){
 										
 									}
 									send_count++;
 								}
 								System.out.println("Quit sender");
 							}
 						};
 						t.start();
 		    		}
 		    	}else{
 		    		System.out.println("Add command: <PUT|GET|GETRANGE|DELETE> key <value>");
 		    	}
 		    	// send a command
 		    	if(cmd != null){
 		    		Response r = null;
 		        	if((r = send(cmd)) != null){
 		        		List<Command> response = r.getResponse(20000); // wait response
 		        		if(!response.isEmpty()){
 		        			for(Command c : response){
 		    	    			if(c.getType() == CommandType.RESPONSE){
 		    	    				if(c.getValue() != null){
 		    	    					System.out.println("  -> " + new String(c.getValue()));
 		    	    				}else{
 		    	    					System.out.println("<no entry>");
 		    	    				}
 		    	    			}			    				
 		        			}
 		        			id++; // re-use same ID if you run into a timeout
 		        		}else{
 		        			System.err.println("Did not receive response from replicas: " + cmd);
 		        		}
 		        	}else{
 		        		System.err.println("Could not send command: " + cmd);
 		        	}
 		    	}
 		    }
 		    in.close();
 	    } catch(IOException e){
 	    	e.printStackTrace();
 	    } catch (InterruptedException e) {
 		}
 	    stop();
 	}
 
 	public void stop(){
 		udp.close();
 	}
 
 	/**
 	 * Send a command (use same ID if your Response ended in a timeout)
 	 * 
 	 * (the commands will be batched to larger Paxos instances)
 	 * 
 	 * @param cmd The command to send
 	 * @return A Response object on which you can wait
 	 * @throws Exception
 	 */
 	public synchronized Response send(Command cmd) throws Exception {
 		Response r = new Response(cmd);
 		commands.put(cmd.getID(),r);
 		int ring = -1;
     	if(cmd.getType() == CommandType.GETRANGE){
     		ring  = partitions.getGlobalRing();
     		List<String> await = new ArrayList<String>();
     		for(Partition p : partitions.getPartitions()){
     			await.add(p.getID());
     		}
     		await_response.put(cmd.getID(),await);
     	}else{
     		ring = partitions.getRing(cmd.getKey());
     	}
 		if(ring < 0){ System.err.println("No partition found for key " + cmd.getKey()); return null; };
     	if(!send_queues.containsKey(ring)){
     			send_queues.put(ring,new LinkedBlockingQueue<Response>());
     			Thread t = new Thread(new BatchSender(ring,this));
     			t.setName("BatchSender-" + ring);
     			t.start();
     	}
     	send_queues.get(ring).add(r);
     	return r;		
 	}
 
 	@Override
 	public synchronized void receive(Message m) {
		// filter away already received replica answers
		if(delivered.contains(m.getID())){
			return;
		}else{
			delivered.add(m.getID());
		}
		
 		// un-batch response (multiple responses per command_id)
 		for(Command c : m.getCommands()){
 			if(!responses.containsKey(c.getID())){
 				List<Command> l = new ArrayList<Command>();
 				responses.put(c.getID(),l);
 			}
 			List<Command> l = responses.get(c.getID());
 			if(!c.getKey().isEmpty() && !l.contains(c)){
 				l.add(c);
 			}
 		}
		
 		// set responses to open commands
 		Iterator<Entry<Integer, List<Command>>> it = responses.entrySet().iterator();
 		while(it.hasNext()){
 			Entry<Integer, List<Command>> e = it.next();
 			if(commands.containsKey(e.getKey())){
 				if(await_response.containsKey(e.getKey())){ // handle GETRANGE responses from different partitions
 					await_response.get(e.getKey()).remove(m.getFrom());
 					if(await_response.get(e.getKey()).isEmpty()){
 						commands.get(e.getKey()).setResponse(e.getValue());
 						commands.remove(e.getKey());
 						await_response.remove(e.getKey());
 						it.remove();
 					}
 				}else{
 					commands.get(e.getKey()).setResponse(e.getValue());
 					commands.remove(e.getKey());
 					it.remove();
 				}
 			}
 		}
 	}
 
 	public PartitionManager getPartitions() {
 		return partitions;
 	}
 
 	public Map<Integer, BlockingQueue<Response>> getSendQueues() {
 		return send_queues;
 	}
 
 	public InetAddress getIp() {
 		return ip;
 	}
 
 	public int getPort() {
 		return port;
 	}
 
 	public Map<Integer, Integer> getConnectMap() {
 		return connectMap;
 	}
 
 	/**
 	 * @param args
 	 */
 	public static void main(String[] args) {
 		String zoo_host = "127.0.0.1:2181";
 		if (args.length > 1) {
 			zoo_host = args[1];
 		}
 		if (args.length < 1) {
 			System.err.println("Plese use \"Client\" \"ring ID,node ID[;ring ID,node ID]\"");
 		} else {
 			final Map<Integer,Integer> connectMap = parseConnectMap(args[0]);
 			try {
 				final PartitionManager partitions = new PartitionManager(zoo_host);
 				partitions.init();
 				final Client client = new Client(partitions,connectMap);
 				Runtime.getRuntime().addShutdownHook(new Thread("ShutdownHook"){
 					@Override
 					public void run(){
 						client.stop();
 					}
 				});
 				client.init();
 				client.readStdin();
 			} catch (Exception e) {
 				e.printStackTrace();
 				System.exit(1);
 			}
 		}
 	}
 
 	public static Map<Integer, Integer> parseConnectMap(String arg) {
 		Map<Integer,Integer> connectMap = new HashMap<Integer,Integer>();
 		for(String s : arg.split(";")){
 			connectMap.put(Integer.valueOf(s.split(",")[0]),Integer.valueOf(s.split(",")[1]));
 		}
 		return connectMap;
 	}
 
 	/**
 	 * Get the host IP address
 	 * 
 	 * @param ipv6 include IPv6 addresses in search
 	 * @return return the host IP address or null
 	 */
 	public static InetAddress getHostAddress(boolean ipv6){
 		try {
 			Enumeration<NetworkInterface> ni = NetworkInterface.getNetworkInterfaces();
 			while (ni.hasMoreElements()){
 				NetworkInterface n = ni.nextElement();
 				if(n.getDisplayName().equals("eth0")){
 					Enumeration<InetAddress> ia = n.getInetAddresses();
 					while(ia.hasMoreElements()){
 						InetAddress addr = ia.nextElement();
 						if(!(addr.isLinkLocalAddress() || addr.isLoopbackAddress() || addr.toString().contains("192.168.122"))){
 							if(addr instanceof Inet6Address && ipv6){
 								return addr;
 							}else if (addr instanceof Inet4Address && !ipv6){
 								return addr;
 							}
 						}
 					}
 				}
 			}
 			return InetAddress.getLoopbackAddress();
 		} catch (SocketException e) {
 			return InetAddress.getLoopbackAddress();
 		}
 	}
 
 }
