 package com.marakana.examples;
 
 import java.util.ArrayList;
 import java.util.Random;
 
 public class MsgGenerator implements Runnable {
 	private ArrayList<MsgInterface> list;
 
 	public MsgGenerator() {
 		list = new ArrayList<MsgInterface>();
 	}
 	
 	public void run() {
 		Random rand = new Random();
 		int r = 0;
 
 		while((r = rand.nextInt(20)) < 18) {
 			MsgInterface msg = null;
 
 			switch (rand.nextInt(3)) {
 				case 0: msg = new MsgTypeImplementation();
 					break;
 				case 1: msg = new MsgTypeAdditional();
 					break;
 				case 2: msg = new MsgTypeImplementationExtended();
 					break;
 			}
 
 			msg.setMsg("Num is: "+r);
 
			list.add(msg);
 		}
 
 		synchronized(this) {
 			this.notifyAll();
 		}
 	}
 
 	public void printList() {
 		System.out.println("List Contents:");
		for(MsgInterface msg : list) {
 			System.out.println("  "+msg.getMsgType()+" msg = "+msg.getMsg());
 			if(msg.getMsgType().equals("MsgTypeImplementationExtended")) {
 				System.out.println(" *** Overloaded getMsg : "+ 
 						((MsgTypeImplementationExtended) msg).getMsg("Special") +
 						((MsgTypeImplementationExtended) msg).getMsg(99));
 			}
 		}
 		System.out.println("List Size: "+list.size());
 	}
 }
