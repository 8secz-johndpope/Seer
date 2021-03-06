 package com.dmr;
 
 public class CSBK {
 	boolean lb,pf;
 	private String display[]=new String[3];
 	
 	// The main decode method
 	public String[] decode (DMRDecode theApp,boolean bits[]) 	{
 		int csbko,fid;
 		// LB
 		lb=bits[0];
 		// PF
 		pf=bits[1];
 		// CSBKO
 		if (bits[2]==true) csbko=32;
 		else csbko=0;
 		if (bits[3]==true) csbko=csbko+16;
 		if (bits[4]==true) csbko=csbko+8;
 		if (bits[5]==true) csbko=csbko+4;
 		if (bits[6]==true) csbko=csbko+2;
 		if (bits[7]==true) csbko++;
 		// FID
 		if (bits[8]==true) fid=128;
 		else fid=0;
 		if (bits[9]==true) fid=fid+64;
 		if (bits[10]==true) fid=fid+32;
 		if (bits[11]==true) fid=fid+16;
 		if (bits[12]==true) fid=fid+8;
 		if (bits[13]==true) fid=fid+4;
 		if (bits[14]==true) fid=fid+2;
 		if (bits[15]==true) fid++;
 		// CSBK Types
 		// 56 - BS_Dwn_Act
 		if (csbko==56)	{
 			bs_dwn_act(bits);
 		}
 		// 01 (FID 6) - Connect Plus
 		else if ((csbko==1)&&(fid==6))	{
 			big_m_csbko01(theApp,bits);
 		}
 		// 03 (FID 6) - Connect Plus
 		else if ((csbko==3)&&(fid==6))	{
 			big_m_csbko03(theApp,bits);
 		}
 		// 04 - UU_V_Reg
 		else if (csbko==4)	{
 			uu_v_reg(bits);
 		}
 		// 05 - UU_Ans_Rsp
 		else if (csbko==5)	{
 			uu_ans_rep(bits);
 		}
 		// 31 (FID 16) - Call Alert 
 		// Note in Tier III that CSBKO=31 is C_RAND but only inbound also FID=0 
 		else if ((csbko==31)&&(fid==16))	{
 			csbko31fid16(theApp,bits);
 		}
 		// 32 (FID 16) - Call Alert Ack
 		// Note in Tier III that CSBKO=32 is C/P_ACKD but also FID=0
 		else if ((csbko==32)&&(fid==16))	{
 			csbko32fid16(theApp,bits);
 		}
 		// 38 - NACK_Rsp
 		else if (csbko==38)	{
 			nack_rsp(bits);
 		}
 		// 61 - Pre_CSBK
 		else if (csbko==61)	{
 			preCSBK(theApp,bits);
 		}
 		// 62 - Capacity Plus
 		else if (csbko==62)	{
 			big_m_csbko62(theApp,bits);
 		}	
 		else	{
 			unknownCSBK(csbko,fid,bits);
 		}
 		return display;
 	}
 	
 	// Handle unknown CSBK types
 	private void unknownCSBK (int csbko,int fid,boolean bits[])	{
 		int a;
 		StringBuilder sb=new StringBuilder(250);
 		sb.append("Unknown CSBK : CSBKO="+Integer.toString(csbko)+" + FID="+Integer.toString(fid)+" ");
 		// Display the binary
 		for (a=16;a<80;a++)	{
 			if (bits[a]==true) sb.append("1");
 			else sb.append("0");
 		}
 		display[0]=sb.toString();
 	}
 	
 	// Handle a Preamble CSBK
 	private void preCSBK (DMRDecode theApp,boolean bits[])	{
 		int index;
 		StringBuilder sb=new StringBuilder(250);
 		StringBuilder sc=new StringBuilder(250);
 		Utilities utils=new Utilities();
 		// 0 if CSBK , 1 if Data
 		boolean dc=bits[16];
 		// 0 if target is individual and 1 if group
 		boolean gi=bits[17];
 		// Bits 18,19,20,21,22,23 are reserved
 		// Next 8 bits are the bits to follow
 		int bfol=0;
 		if (bits[24]==true) bfol=128;
 		if (bits[25]==true) bfol=bfol+64;
 		if (bits[26]==true) bfol=bfol+32;
 		if (bits[27]==true) bfol=bfol+16;
 		if (bits[28]==true) bfol=bfol+8;
 		if (bits[29]==true) bfol=bfol+4;
 		if (bits[30]==true) bfol=bfol+2;
 		if (bits[31]==true) bfol++;
 		// Target address
 		int target=utils.retAddress(bits,32);
 		// Source address
 		int source=utils.retAddress(bits,56);
 		// Display this
 		sb.append("Preamble CSBK : ");
 		if (dc==false) sb.append(" CSBK content ");
 		else sb.append(" Data content ");
 		sb.append(Integer.toString(bfol)+" Blocks to follow");
 		display[0]=sb.toString();		
 		sc.append("Target Address : "+Integer.toString(target));
 		if (gi==true) sc.append(" (Group)");
 		sc.append(" Source Address : "+Integer.toString(source));
 		display[1]=sc.toString();
 		// Target
 		theApp.usersLogged.addUser(target);
 		index=theApp.usersLogged.findUserIndex(target);
 		if (index!=-1)	{
 			theApp.usersLogged.setAsDataUser(index);
 			theApp.usersLogged.setChannel(index,theApp.currentChannel);
 		}
 		// Source
 		theApp.usersLogged.addUser(source);
 		index=theApp.usersLogged.findUserIndex(source);
 		if (index!=-1)	{
 			theApp.usersLogged.setAsDataUser(index);
 			theApp.usersLogged.setChannel(index,theApp.currentChannel);
 		}
 	}
 		
 	// BS Outbound Activation CSBK
 	private void bs_dwn_act (boolean bits[])	{
 		// TODO : Full decoding of bs_dwn_act
 		display[0]="BS Outbound Activation";
 	}
 	
 	// Unit to Unit Voice Service Request CSBK
 	private void uu_v_reg (boolean bits[])	{
 		// TODO : Full decoding of UU_V_Req
 		display[0]="Unit to Unit Voice Service Request";
 	}
 	
 	// Unit to Unit Service Answer Response CSBK
 	private void uu_ans_rep (boolean bits[])	{
 		// TODO : Full decoding of UU_Ans_Rsp
 		display[0]="Unit to Unit Service Answer Response";
 	}
 	
 	// Negative Acknowledge Response CSBK
 	private void nack_rsp (boolean bits[])	{
 		// TODO : Full decoding of NACK_Rsp
 		display[0]="Negative Acknowledge Response";
 	}
 	
 	// Capacity Plus
     // A great deal of information on this type of packet was kindly provided by Eric Cottrell on the Radioreference forums	
 	// see http://forums.radioreference.com/digital-voice-decoding-software/209318-understanding-capacity-plus-trunking-6.html#post2078924	
 	private void big_m_csbko62 (DMRDecode theApp,boolean bits[])	{
 		int group1,group2,group3,group4,group5,group6,a,lcn;
 		StringBuilder sb1=new StringBuilder(300);
 		StringBuilder sb2=new StringBuilder(300);
 		display[0]="Capacity Plus CSBK : CSBKO=62";
 		// LCN
 		if (bits[20]==true) lcn=8;
 		else lcn=0;
 		if (bits[21]==true) lcn=lcn+4;
 		if (bits[22]==true) lcn=lcn+2;
 		if (bits[23]==true) lcn++;
 		// Group idents
 		// Low group
 		if (bits[32]==true) group1=128;
 		else group1=0;
 		if (bits[33]==true) group1=group1+64;
 		if (bits[34]==true) group1=group1+32;
 		if (bits[35]==true) group1=group1+16;
 		if (bits[36]==true) group1=group1+8;
 		if (bits[37]==true) group1=group1+4;
 		if (bits[38]==true) group1=group1+2;
 		if (bits[39]==true) group1++;
 		// Group 2
 		if (bits[40]==true) group2=128;
 		else group2=0;
 		if (bits[41]==true) group2=group2+64;
 		if (bits[42]==true) group2=group2+32;
 		if (bits[43]==true) group2=group2+16;
 		if (bits[44]==true) group2=group2+8;
 		if (bits[45]==true) group2=group2+4;
 		if (bits[46]==true) group2=group2+2;
 		if (bits[47]==true) group2++;
 		// Group 3
 		if (bits[48]==true) group3=128;
 		else group3=0;
 		if (bits[49]==true) group3=group3+64;
 		if (bits[50]==true) group3=group3+32;
 		if (bits[51]==true) group3=group3+16;
 		if (bits[52]==true) group3=group3+8;
 		if (bits[53]==true) group3=group3+4;
 		if (bits[54]==true) group3=group3+2;
 		if (bits[55]==true) group3++;
 		// Group 4
 		if (bits[56]==true) group4=128;
 		else group4=0;
 		if (bits[57]==true) group4=group4+64;
 		if (bits[58]==true) group4=group4+32;
 		if (bits[59]==true) group4=group4+16;
 		if (bits[60]==true) group4=group4+8;
 		if (bits[61]==true) group4=group4+4;
 		if (bits[62]==true) group4=group4+2;
 		if (bits[63]==true) group4++;
 		// Group 5
 		if (bits[64]==true) group5=128;
 		else group5=0;
 		if (bits[65]==true) group5=group5+64;
 		if (bits[66]==true) group5=group5+32;
 		if (bits[67]==true) group5=group5+16;
 		if (bits[68]==true) group5=group5+8;
 		if (bits[69]==true) group5=group5+4;
 		if (bits[70]==true) group5=group5+2;
 		if (bits[71]==true) group5++;		
 		// Group 6
 		if (bits[72]==true) group6=128;
 		else group6=0;
 		if (bits[73]==true) group6=group6+64;
 		if (bits[74]==true) group6=group6+32;
 		if (bits[75]==true) group6=group6+16;
 		if (bits[76]==true) group6=group6+8;
 		if (bits[77]==true) group6=group6+4;
 		if (bits[78]==true) group6=group6+2;
 		if (bits[79]==true) group6++;
 		// Display all this 
 		// Only show more if we have any activity
 		if ((bits[24]==false)&&(bits[25]==false)&&(bits[26]==false)&&(bits[27]==false)&&(bits[28]==false)&&(bits[29]==false))	{
 			sb1.append("Activity Update : LCN "+Integer.toString(lcn)+" is the Rest Channel");
 		} else {
 			boolean nf=false;
 			sb1.append("Activity Update : LCN "+Integer.toString(lcn)+" is the rest channel (");
 			if (bits[24]==true)	{
 				if (group1>0) sb1.append("Group "+Integer.toString(group1)+" on LCN 1");
 				else sb1.append("Activity on LCN 1");
 				nf=true;
 			}
 			if (bits[25]==true)	{
 				if (nf==true) sb1.append(",");
 				if (group2>0) sb1.append("Group "+Integer.toString(group2)+" on LCN 2");
 				else sb1.append("Activity on LCN 2");
 				nf=true;
 			}
 			if (bits[26]==true)	{
 				if (nf==true) sb1.append(",");
 				if (group3>0) sb1.append("Group "+Integer.toString(group3)+" on LCN 3");
 				else sb1.append("Activity on LCN 3");
 				nf=true;
 			}
 			if (bits[27]==true)	{
 				if (nf==true) sb1.append(",");
 				if (group4>0) sb1.append("Group "+Integer.toString(group4)+" on LCN 4");
 				else sb1.append("Activity on LCN 4");
 				nf=true;
 			}
 			if (bits[28]==true)	{
 				if (nf==true) sb1.append(",");
 				if (group5>0) sb1.append("Group "+Integer.toString(group5)+" on LCN 5");
 				else sb1.append("Activity on LCN 5");
 				nf=true;
 			}
 			if (bits[29]==true)	{
 				if (nf==true) sb1.append(",");
 				if (group6>0) sb1.append("Group "+Integer.toString(group6)+" on LCN 6");
 				else sb1.append("Activity on LCN 6");
 				nf=true;
 			}
 			if (nf==true) sb1.append(")");
 		}
 		display[1]=sb1.toString();
 		// Display the full binary if in debug mode
 		if (theApp.isDebug()==true)	{
 			for (a=16;a<80;a++)	{
 				if (bits[a]==true) sb2.append("1");
 				else sb2.append("0");
 			}
 			display[2]=sb2.toString();
 		}
 	}
 	
 	// Connect Plus - CSBKO 03 FID=6
 	private void big_m_csbko03 (DMRDecode theApp,boolean bits[])	{
 		int a,lcn;
 		Utilities utils=new Utilities();
 		StringBuilder sb1=new StringBuilder(300);
 		StringBuilder sb2=new StringBuilder(300);
 		display[0]="Connect Plus CSBK : CSBKO=3";
 		// Source ID
 		int source=utils.retAddress(bits,16);
 		// Group address
 		int group=utils.retAddress(bits,40);
 		// LCN
 		if (bits[64]==true) lcn=8;
 		else lcn=0;
 		if (bits[65]==true) lcn=lcn+4;
 		if (bits[66]==true) lcn=lcn+2;
 		if (bits[67]==true) lcn++;
 		// Time Slot
 	    // The information on the time slot bit was kindly provided by W8EMX on the Radioreference forums
 		// see http://forums.radioreference.com/digital-voice-decoding-software/213131-understanding-connect-plus-trunking-7.html#post1909226
 		boolean timeSlot=bits[68];
 		// Display this
 		sb1.append("Channel Grant : LCN "+Integer.toString(lcn));
 		if (timeSlot==false) sb1.append(" TS1");
 		else sb1.append(" TS2");
 		sb1.append(" Source "+Integer.toString(source));
 		sb1.append(" Group "+Integer.toString(group));
 		display[1]=sb1.toString();
 		// Display the full binary if in debug mode
 		if (theApp.isDebug()==true)	{
 			for (a=16;a<80;a++)	{
 				if (bits[a]==true) sb2.append("1");
 				else sb2.append("0");
 			}
 			display[2]=sb2.toString();
 		}
 	}
 	
 	// Connect Plus - CSBKO 01 FID=6
 	private void big_m_csbko01 (DMRDecode theApp,boolean bits[])	{
 		int a,nb1,nb2,nb3,nb4,nb5;
 		Utilities utils=new Utilities();
 		StringBuilder sb1=new StringBuilder(300);
 		StringBuilder sb2=new StringBuilder(300);
 		display[0]="Connect Plus CSBK : CSBKO=1";
 		sb1.append("Control Channel Neighbour List : ");
 		// The information to decode these packets was kindly provided by inigo88 on the Radioreference forums
 		// see http://forums.radioreference.com/digital-voice-decoding-software/213131-understanding-connect-plus-trunking-6.html#post1866950
 		//                 67 890123 45 678901 23 456789 01 234567 89 012345 6789 0123 4567 8901 2345 6789
 		// CSBKO=1 + FID=6 00 000001 00 000011 00 000100 00 000101 00 000110 0000 0000 0000 0000 0000 1110
 		//                         1         3         4	     5         6	                      
 		// bits 16,17 have an unknown purpose
 		// bits 18,19,20,21,22,23 make up the first neighbour site ID
 		nb1=utils.retSix(bits,18);
 		// bits 24,25 have an unknown purpose
 		// bits 26,27,28,29,30,31 make up the second neighbour site ID
 		nb2=utils.retSix(bits,26);
 		// bits 32,33 have an unknown purpose
 		// bits 34,35,36,37,38,39 make up the third neighbour site ID
 		nb3=utils.retSix(bits,34);
 		// bits 40,41 have an unknown purpose
 		// bits 42,43,44,45,46,47 make up the fourth neighbour site ID
 		nb4=utils.retSix(bits,42);
 		// bits 48,49 have an unknown purpose
 		// bits 50,51,52,53,54,55 make up the fifth neighbour site ID
 		nb5=utils.retSix(bits,50);
 		// bits 56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79 have an unknown purpose
 		// Display this info
 		sb1.append(Integer.toString(nb1)+","+Integer.toString(nb2)+","+Integer.toString(nb3)+","+Integer.toString(nb4)+","+Integer.toString(nb5)+" (");
 		// Also display as raw binary for now
 		for (a=16;a<80;a++)	{
 			if (bits[a]==true) sb1.append("1");
 			else sb1.append("0");
 		}
 		sb1.append(")");
 		display[1]=sb1.toString();
 		// Display the full binary if in debug mode
 		if (theApp.isDebug()==true)	{
 			for (a=16;a<80;a++)	{
 				if (bits[a]==true) sb2.append("1");
 				else sb2.append("0");
 			}
 			display[2]=sb2.toString();
 		}
 	}
 
 	// CSBKO 31 FID 16 Call Alert
 	// The information to decode this was kindly provided by bben95 on the Radioreference forums
 	// http://forums.radioreference.com/digital-voice-decoding-software/191957-java-program-decode-dmr-31.html#post2098983
 	// 0000000000000000 000000000001011101110010 000000000001011101110001
 	// 1111222222222233 333333334444444444555555 555566666666667777777777
 	// 6789012345678901 234567890123456789012345 678901234567890123456789
 	// is Call alert from 6001 to 6002
 	private void csbko31fid16 (DMRDecode theApp,boolean bits[])	{
 		int a;
 		Utilities utils=new Utilities();
		int from=utils.retAddress(bits,32);
		int to=utils.retAddress(bits,56);
 		StringBuilder sb1=new StringBuilder(300);
 		display[0]="CSBK : CSBKO=31 + FID=16";
 		sb1.append("Call Alert from "+Integer.toString(from)+" to "+Integer.toString(to)+" (");
 		// Also display the unknown part as raw binary for now
 		for (a=16;a<32;a++)	{
 			if (bits[a]==true) sb1.append("1");
 			else sb1.append("0");
 			}
 		sb1.append(")");
 		display[1]=sb1.toString();	
 	}
 	
 	// CSBKO 32 FID 16 Call Alert Ack
 	// The information to decode this was kindly provided by bben95 on the Radioreference forums
 	// http://forums.radioreference.com/digital-voice-decoding-software/191957-java-program-decode-dmr-31.html#post2098983
     // 1001111100000000 000000000001011101110001 000000000001011101110010
 	// 1111222222222233 333333334444444444555555 555566666666667777777777
 	// 6789012345678901 234567890123456789012345 678901234567890123456789
 	// is Call alert from 6001 to 6002: acknowledged
 	private void csbko32fid16 (DMRDecode theApp,boolean bits[])	{
 		int a;
 		Utilities utils=new Utilities();
		int from=utils.retAddress(bits,32);
		int to=utils.retAddress(bits,56);
 		StringBuilder sb1=new StringBuilder(300);
 		display[0]="CSBK : CSBKO=32 + FID=16";
 		sb1.append("Call Alert ACK from "+Integer.toString(from)+" to "+Integer.toString(to)+" (");
 		// Also display the unknown part as raw binary for now
 		for (a=16;a<32;a++)	{
 			if (bits[a]==true) sb1.append("1");
 			else sb1.append("0");
 			}
 		sb1.append(")");
 		display[1]=sb1.toString();		
 	}
 	
 }
