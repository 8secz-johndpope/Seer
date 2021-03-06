 package net.anei.cadpage.parsers.SC;
 
 import net.anei.cadpage.parsers.BaseParserTest;
 
 import org.junit.Test;
 
 
 public class SCOconeeCountyParserTest extends BaseParserTest {
   
   public SCOconeeCountyParserTest() {
     setParser(new SCOconeeCountyParser(), "OCONEE COUNTY", "SC");
   }
   
   @Test
   public void testParser() {
 
     doTest("T1",
         "[911 Message]  S80 - CORONARY PROBLEM  1280 N  STHY 11 XStreet: SPRINGDALE DR / SCENIC HEIGHTS RD, FOWLER RD WEST UNION    2011-00000815  09/26/11 22:27  Narr:   S",
         "CALL:S80 - CORONARY PROBLEM",
         "ADDR:1280 N STHY 11",
         "MADDR:1280 N ST 11",
         "X:SPRINGDALE DR / SCENIC HEIGHTS RD, FOWLER RD",
         "CITY:WEST UNION",
         "ID:2011-00000815",
         "DATE:09/26/11",
         "TIME:22:27");
 
     doTest("T2",
         "[911 Message]  S80 - CORONARY PROBLEM  206 S  TUGALOO ST XStreet: W MAULDIN ST / BOOKER DR WALHALLA    2011-00000809  09/25/11 06:49  Narr:   PATIENT IS HER FATHER",
         "CALL:S80 - CORONARY PROBLEM",
         "ADDR:206 S TUGALOO ST",
         "X:W MAULDIN ST / BOOKER DR",
         "CITY:WALHALLA",
         "ID:2011-00000809",
         "DATE:09/25/11",
         "TIME:06:49",
         "INFO:PATIENT IS HER FATHER");
 
     doTest("T3",
         "[911 Message]  S46 - ALTERED MENTAL STATUS  308 N  LAUREL ST XStreet: ARDASHIR LN / WALHALLA GARDENS CIR WALHALLA    2011-00000777  09/16/11 12:24  Narr:   76 YOA",
         "CALL:S46 - ALTERED MENTAL STATUS",
         "ADDR:308 N LAUREL ST",
         "X:ARDASHIR LN / WALHALLA GARDENS CIR",
         "CITY:WALHALLA",
         "ID:2011-00000777",
         "DATE:09/16/11",
         "TIME:12:24",
         "INFO:76 YOA");
 
     doTest("T4",
         "(911 Message) S4 - DIABETIC REACTION  100 PINE MANOR CIR APT 3 XStreet: STOUDEMIRE ST / STOUDEMIRE ST WALHALLA  COUNTRY RIDGE APTS  2011-00000872  10/26/11 19:21",
         "CALL:S4 - DIABETIC REACTION",
         "ADDR:100 PINE MANOR CIR",
         "APT:3",
         "X:STOUDEMIRE ST / STOUDEMIRE ST",
         "CITY:WALHALLA",
         "PLACE:COUNTRY RIDGE APTS",
         "ID:2011-00000872",
         "DATE:10/26/11",
         "TIME:19:21");
 
     doTest("T5",
         "(911 Message) LIFT ASSISTANCE  313 MANOR LN XStreet: INDUSTRIAL PARK PL / DEAD END SENECA     12/02/11 01:06  Narr:   COME TO THE BACK DOOR  NEEDS HELP GETTING UP",
         "CALL:LIFT ASSISTANCE",
         "ADDR:313 MANOR LN",
         "X:INDUSTRIAL PARK PL / DEAD END",
         "CITY:SENECA",
         "DATE:12/02/11",
         "TIME:01:06",
         "INFO:COME TO THE BACK DOOR  NEEDS HELP GETTING UP");
 
     doTest("T6",
         "(911 Message) S86 - CHEST PAIN  3440 BLUE RIDGE BLVD XStreet: MISTY DR, THE OLE HOME PLACE LN / TRAVELLERS BLVD WEST UNION  EDWARDS AUTO SALES  2011-00000946  11/",
         "CALL:S86 - CHEST PAIN",
         "ADDR:3440 BLUE RIDGE BLVD",
         "X:MISTY DR, THE OLE HOME PLACE LN / TRAVELLERS BLVD",
         "CITY:WEST UNION",
         "PLACE:EDWARDS AUTO SALES",
         "ID:2011-00000946");
 
     doTest("T7",
         "(911 Message) LIFT ASSISTANCE  313 MANOR LN XStreet: INDUSTRIAL PARK PL / DEAD END SENECA     12/02/11 01:06  Narr:   COME TO THE BACK DOOR  NEEDS HELP GETTING UP",
         "CALL:LIFT ASSISTANCE",
         "ADDR:313 MANOR LN",
         "X:INDUSTRIAL PARK PL / DEAD END",
         "CITY:SENECA",
         "DATE:12/02/11",
         "TIME:01:06",
         "INFO:COME TO THE BACK DOOR  NEEDS HELP GETTING UP");
 
   }
   
   @Test
   public void testActive911() {
 
     doTest("T1",
         "(911 Message) S74 - RESPIRATORY DISTRESS  502 VERA DR XStreet: HOBSON ST / DEAD END WESTMINSTER    2012-00000025  01/14/12 01:28  Narr:   TONED RQ5  DOES HAVE HEART PROBLEMS  HEART PT HAVING TROUBLE BREATHING  E911 Info - Class of Service: RESD Special Response Info: WESTMINSTER CITY PD WESTMINSTER CITY FIRE  EMS ER-5 ER-3",
         "CALL:S74 - RESPIRATORY DISTRESS",
         "ADDR:502 VERA DR",
         "X:HOBSON ST / DEAD END",
         "CITY:WESTMINSTER",
         "ID:2012-00000025",
         "DATE:01/14/12",
         "TIME:01:28",
         "INFO:TONED RQ5  DOES HAVE HEART PROBLEMS  HEART PT HAVING TROUBLE BREATHING");
 
     doTest("T2",
         "(911 Message) 70S - STRUCTURE FIRE  10941 CLEMSON BLVD XStreet: PRESSLEY PL / SONNYS DR SENECA  BQS #5 CLEMSON BLVD   01/16/12 17:44  Narr:   IN NEIGHBORHOOD BEHIND THE ROAD RUNNER  SOMEWHERE ACROSS STREET  E911 Info - Class of Service: BUSN Special Response Info: SHERIFF DEPT SENECA  CORINTH-SHILOH FIRE #3  EMS ER-1 ER-2",
         "CALL:70S - STRUCTURE FIRE",
         "ADDR:10941 CLEMSON BLVD",
         "X:PRESSLEY PL / SONNYS DR",
         "CITY:SENECA",
         "PLACE:BQS #5 CLEMSON BLVD",
         "DATE:01/16/12",
         "TIME:17:44",
         "INFO:IN NEIGHBORHOOD BEHIND THE ROAD RUNNER  SOMEWHERE ACROSS STREET");
 
     doTest("T3",
         "(911 Message) LIFT ASSISTANCE  1407 W LITTLE RIVER DR XStreet: AZURE COVE CT / KEOWEE LAKESHORE DR SENECA     01/15/12 20:43  Narr:   NO 10-52 RESPONDING UNLESS NEEDED  HE HAS FALLEN  NEEDS HELP LIFTING SOMEONE OUT OF THE FLOOR  E911 Info - Class of Service: RESD Special Response Info: SHERIFF DEPT SENECA CORINTH-SHILOH FIRE #3  EMS ER-1 ER-2",
         "CALL:LIFT ASSISTANCE",
         "ADDR:1407 W LITTLE RIVER DR",
         "X:AZURE COVE CT / KEOWEE LAKESHORE DR",
         "CITY:SENECA",
         "DATE:01/15/12",
         "TIME:20:43",
         "INFO:NO 10-52 RESPONDING UNLESS NEEDED  HE HAS FALLEN  NEEDS HELP LIFTING SOMEONE OUT OF THE FLOOR");
 
     doTest("T4",
         "(911 Message) S32 - SPINAL INJURY  105 GLORIA LN XStreet: SHILOH RD / DEAD END SENECA 2012-00000009  01/13/12 17:35  Narr:   CANT MOVE HIS LEG  HUSBAND FELL BATHROOM FLOOR",
         "CALL:S32 - SPINAL INJURY",
         "ADDR:105 GLORIA LN",
         "X:SHILOH RD / DEAD END",
         "CITY:SENECA",
        "PLACE:2012-00000009",
         "DATE:01/13/12",
         "TIME:17:35",
         "INFO:CANT MOVE HIS LEG  HUSBAND FELL BATHROOM FLOOR");
 
     doTest("T5",
         "[911 Message] 50 - TRAFFIC ACCIDENT  TEARDROP TRL WAR WOMAN TRL XStreet: SENECA     02/07/12 04:21  Narr:   PT NAME IS KEITH ALLEN STUTRIDGE  HE IS HURT  NO OVERTURNED  HAD A 10-50 ABOUT 50 YARDS UP THE ROADWAY  STD MALE LAYING IN THE ROADWAY \n",
         "CALL:50 - TRAFFIC ACCIDENT",
         "ADDR:TEARDROP TRL & WAR WOMAN TRL",
         "X:SENECA",
         "DATE:02/07/12",
         "TIME:04:21",
         "INFO:PT NAME IS KEITH ALLEN STUTRIDGE  HE IS HURT  NO OVERTURNED  HAD A 10-50 ABOUT 50 YARDS UP THE ROADWAY  STD MALE LAYING IN THE ROADWAY");
  }
   
   public static void main(String[] args) {
     new SCOconeeCountyParserTest().generateTests("T1", "UNIT CALL ADDR APT X CITY PLACE ID DATE TIME INFO");
   }
 }
