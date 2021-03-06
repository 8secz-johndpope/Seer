 package net.anei.cadpage.parsers.NC;
 
 import net.anei.cadpage.parsers.FieldProgramParser;
 import net.anei.cadpage.parsers.MsgInfo.Data;
 
 /* 
 Rutherford County, NC
 Contact: Jeffrey Lynn <lynnj163@yahoo.com>
 Sender: paging@rutherfordcountync.gov
 
 Paging: Location= 159 WALKER DR*\nAPT/ROOM* City=Forest City* Call \nType= FIRE STRUCT* Units=SMFD1*\nApr24,3:05am
 Paging: Location= 557 US 221A HWY*\nAPT/ROOM* City=Forest City* Call \nType= FIRE POWERLN* Units=DUKE1, SMFD1*\nApr23,4:27am
 Paging: Location= 140 WALKER DR*\nAPT/ROOM* City=Forest City* Call \nType= FIRE ILLEGAL* Units=SMFD1*\nApr23,3:53am
 Paging: Location= 139 GREENE RD*\nAPT/ROOM* City=Forest City* Call \nType= FIRE STRUCT* Units=SMFD1*\nApr19,5:36pm
 Paging: Location= HIGH SHOALS CHURCH RD*\nAPT/ROOM* City=Cliffside* Call \nType= FIRE STRUCT* Units=CFD1, EFD1,SMFD1*\nApr18,10:50pm
 Paging: Location= OLD HENRIETTA RD & BYERS RD*\nAPT/ROOM* City=Ellenboro* Call \nType= STRANGE ODOR* Units=SMFD1*\nApr 25,10:59am
 Paging: Location= US221 S HWY & HARRIS HENRIETTA RD*\nAPT/ROOM* City=Forest City* Call \nType= ACCIDENT PI* Units=SMFD1,RCR1,EMS1*\nApr 25, 3:31pm
 
 Contact: Jeff Lynn <lynnj163@gmail.com>
 Paging:Location=376 WOMACK LAKE RD* APT/ROOM* City=FOREST CITY* Call Type=ACCIDENT F* Units=428,MED10,RCR1,SMFD1*
 
 Contact: Ferrell Hamrick <fireball3412@gmail.com>
 prvs=11539c2346=paging@rutherfordcountync.gov (Message Forwarded by PageGate) Location=WITHROW RD & HUDLOW*\nAPT/ROOM*\n\nCity=FOREST CITY*\n\nCall Type=ACCIDENT PI*\n\n\n\nUnits=103,FCFD1,
 
Contact: Active911
[] Paging:Line1=2012-059502* Location=1600 US 221S HWY* APT/ROOM=* City=FOREST CITY* Call Type=EMS CHESTPAI* Line11=CHEST PAIN (NON-TRAUMATIC)* Units=ems1,MED11,vls1*\r\n\n
[] Paging:Location=108 PATCHES LN* APT/ROOM=* City=BOSTIC* Call Type=EMS DIFF BRE* Units=802,MED30,RCR1*\r\n\n
[] Paging:Line1=2012-059515* Location=394 SHENANDOAH DR* APT/ROOM=* City=Spindale* Call Type=EMS CHESTPAI* Line11=CHEST PAIN (NON-TRAUMATIC)* Units=MED10*\r\n\n
[] Paging:Line1=2012-059516* Location=113 GOLD TREE LN* APT/ROOM=* City=RUTHERFORDTON* Call Type=EMS CHESTPAI* Line11=CHEST PAIN (NON-TRAUMATIC)* Units=MED11*\r\n\n
[] Paging:Line1=2012-059517* Location=SANDY MUSH* APT/ROOM=* City=RUTHERFORDTON* Call Type=STAND BY* Line11=STAND BY* Units=MED40*\r\n\n
[] Paging:Location=1172 PINEY RIDGE RD* APT/ROOM=* City=FOREST CITY* Call Type=EMS CHESTPAI* Units=MED40,RCR1*\r\n\n
 
 */
 
 public class NCRutherfordCountyParser extends FieldProgramParser {
   
   public NCRutherfordCountyParser() {
     super("RUTHERFORD COUNTY", "NC",
           "Location:ADDR! APT/ROME:APT? City:CITY! Call_Type:CALL! Line11:INFO? Units:UNIT!");
   }
   
   @Override
   public String getFilter() {
     return "paging@rutherfordcountync.gov";
   }
   
   @Override
   public boolean parseMsg(String subject, String body, Data data) {
     do {
       if (body.startsWith("Paging:")) {
         body = body.substring(7).trim();
         break;
       }
       if (subject.endsWith("PageGate")) break;
       return false;
     } while (false);
     body = body.replace("\n", "").replace('=', ':');
     return super.parseFields(body.split("\\*"), data);
   }
 }
