 package eu.uberdust.lights;
 
 import eu.uberdust.MainApp;
 import eu.uberdust.communication.UberdustClient;
 import eu.uberdust.communication.rest.RestClient;
 import eu.uberdust.communication.websocket.readings.WSReadingsClient;
 import eu.uberdust.lights.tasks.LightTask;
 import eu.uberdust.lights.tasks.TurnOffTask_2;
 import org.apache.log4j.Logger;
 import org.apache.log4j.PropertyConfigurator;
 
 import java.io.IOException;
 import java.util.Timer;
 
 /**
  * Basic functionality.
  */
 public final class FoiController {
 
     /**
      * Static Logger.
      */
     private static final Logger LOGGER = Logger.getLogger(FoiController.class);
 
     private boolean Zone;
 
     private boolean zone1;
 
     private boolean zone2;
 
     private long lastPirReading;
 
     private long zone1TurnedOnTimestamp;
 
     private long zone2TurnedOnTimestamp;
 
     private boolean isScreenLocked;
 
     public static final int WINDOW = 10;
 
     public static double[] Lum = new double[WINDOW];
 
     private static int i = WINDOW-1;
 
     public static final String URN_FOI = "urn:wisebed:ctitestbed:virtual:"+MainApp.FOI;
 
     public static final String SENSOR_SCREENLOCK_REST = "http://uberdust.cti.gr/rest/testbed/1/node/urn:wisebed:ctitestbed:virtual:"+MainApp.FOI+"/capability/urn:wisebed:ctitestbed:node:capability:lockScreen/tabdelimited/limit/1";
 
     public static final String USER_PREFERENCES ="http://150.140.16.31/api/v1/foi?identifier="+MainApp.FOI;
 
     public static final String FOI_CAPABILITIES = "http://uberdust.cti.gr/rest/testbed/1/node/urn:wisebed:ctitestbed:virtual:"+MainApp.FOI+"/capabilities/json";
 
     public static final String ACTUATOR_URL = "http://uberdust.cti.gr/rest/testbed/1/node/urn:wisebed:ctitestbed:virtual:"+MainApp.FOI+"/capability/urn:wisebed:node:capability:lz"+MainApp.ZONES[0]+"/json/limit/1";
 
     public static final String FOI_ACTUATOR = GetJson.getInstance().callGetJsonWebService(ACTUATOR_URL,"nodeId").split("0x")[1];
 
     /**
      * Pir timer.
      */
     private final Timer timer;
 
     /**
      * static instance(ourInstance) initialized as null.
      */
     private static FoiController ourInstance = null;
 
     public static double LUM_THRESHOLD_1 = Double.parseDouble(GetJson.getInstance().callGetJsonWebService(USER_PREFERENCES,"illumination"));  //350
 
     public static boolean BYPASS = false;
 
     private double lastLumReading;
 
     private double Median;
 
     /**
      * FoiController is loaded on the first execution of FoiController.getInstance()
      * or the first access to FoiController.ourInstance, not before.
      *
      * @return ourInstance
      */
     public static FoiController getInstance() {
         synchronized (FoiController.class) {
             if (ourInstance == null) {
                 ourInstance = new FoiController();
             }
         }
         return ourInstance;
     }
 
     /**
      * Private constructor suppresses generation of a (public) default constructor.
      */
     private FoiController() {
         PropertyConfigurator.configure(this.getClass().getClassLoader().getResource("log4j.properties"));
         LOGGER.info("FOI Controller initialized");
         timer = new Timer();
 
         LOGGER.info("lastLumReading -- " + lastLumReading);
         LOGGER.info("isScreenLocked -- " + isScreenLocked);
 
         Zone = false;
         zone1 = false;
         zone2 = false;
 
         WSReadingsClient.getInstance().setServerUrl("ws://uberdust.cti.gr:80/readings.ws");
 
         LOGGER.info(MainApp.FOI.split(":")[0]);
 
         //Subscription for notifications.
         if(MainApp.FOI.split(":")[0].equals("workstation")){
 
             setLum(RestClient.getInstance().callRestfulWebService(MainApp.SENSOR_LIGHT_READINGS_REST));
             setScreenLocked((Double.valueOf(RestClient.getInstance().callRestfulWebService(this.SENSOR_SCREENLOCK_REST).split("\t")[1]) == 1) || (Double.valueOf(RestClient.getInstance().callRestfulWebService(this.SENSOR_SCREENLOCK_REST).split("\t")[1]) == 3));
 
             WSReadingsClient.getInstance().subscribe(this.URN_FOI, MainApp.CAPABILITY_SCREENLOCK);
             WSReadingsClient.getInstance().subscribe("urn:wisebed:ctitestbed:virtual:room:0.I.2", MainApp.CAPABILITY_LIGHT);
 
         }
         else if(MainApp.FOI.split(":")[0].equals("room")){
 
             //setScreenLocked(false);
             WSReadingsClient.getInstance().subscribe(this.URN_FOI, MainApp.CAPABILITY_PIR);               //this.URN_FOI
         }
         //Adding Observer for the last readings
         WSReadingsClient.getInstance().addObserver(new ReadingsObserver());
 
     }
 
     public void setLum(final String readings){
 
         for(int k=0,j=1; k<=WINDOW-1; k++,j=j+3){
 
             Lum[k] = Double.valueOf(readings.split("\t")[j]);
             LOGGER.info("Lum["+k+"]: "+Lum[k]);
 
         }
 
     }
 
 
     public void setLastLumReading(final double thatReading) {
 
         LUM_THRESHOLD_1 = Double.parseDouble(GetJson.getInstance().callGetJsonWebService(USER_PREFERENCES,"illumination"));  //350
 
         double sum = 0;
 
         LOGGER.info(" i : "+i);
 
         if(i == 0) {
             Lum[i] = thatReading;
             i = WINDOW-1;
         } else {
             Lum[i] = thatReading;
             i--;
         }
 
         LOGGER.info("thatReading : "+thatReading);
         LOGGER.info(" i : "+i);
 
         for(int k=0; k<=WINDOW-1; k++){
             LOGGER.info("Lum["+k+"]: "+Lum[k]);
             sum+=Lum[k];
         }
 
         LOGGER.info("Median : "+(sum/WINDOW));
         this.Median = sum/WINDOW    ;
 
         this.lastLumReading = thatReading;
 
         if (Median < LUM_THRESHOLD_1) {
             if (!isScreenLocked) {
                 controlLight(true, Integer.parseInt(MainApp.ZONES[0]));
             }
 
         } else {
             controlLight(false, Integer.parseInt(MainApp.ZONES[0]));
         }
     }
 
 
     public void setScreenLocked(final boolean screenLocked) {
         this.isScreenLocked = screenLocked;
         updateLight3();
     }
 
     public synchronized void updateLight3() {
 
         LUM_THRESHOLD_1 = Double.parseDouble(GetJson.getInstance().callGetJsonWebService(USER_PREFERENCES,"illumination"));  //350
 
         if (!isScreenLocked) {
             if (Median < LUM_THRESHOLD_1 ) {
                 controlLight(true, Integer.parseInt(MainApp.ZONES[0]));
             }
         } else if (isScreenLocked) {
             timer.schedule(new TurnOffTask_2(timer), TurnOffTask_2.DELAY);
             //controlLight(false, Integer.parseInt(MainApp.ZONE));
         }
 
     }
 
     public long getLastPirReading() {
         return lastPirReading;
     }
 
     public void setLastPirReading(final long thatReading) {
         this.lastPirReading = thatReading;
         if (!zone1) {
             controlLight(true, Integer.parseInt(MainApp.ZONES[0]));        //3
             zone1TurnedOnTimestamp = thatReading;
             timer.schedule(new LightTask(timer), LightTask.DELAY);
         } else if (!zone2 && (MainApp.ZONES.length > 1) ) {
             controlLight(true, Integer.parseInt(MainApp.ZONES[0]));          //3
             if (thatReading - zone1TurnedOnTimestamp > 15000) {
                 controlLight(true, Integer.parseInt(MainApp.ZONES[1]));
                controlLight(true, Integer.parseInt(MainApp.ZONES[2]));
                 zone2TurnedOnTimestamp = thatReading;
             }
        } else {
             controlLight(true, Integer.parseInt(MainApp.ZONES[1]));
            controlLight(true, Integer.parseInt(MainApp.ZONES[2]));
         }
     }
 
     public double getLastLumReading() {
         return this.lastLumReading;
     }
 
     public double getMedian() {
         return this.Median;
     }
 
 
     public boolean isScreenLocked() {
         return this.isScreenLocked;
     }
 
     public boolean isZone() {
         return Zone;
     }
 
     public boolean isZone1() {
         return zone1;
     }
 
     public boolean isZone2() {
         return zone2;
     }
 
     public synchronized void controlLight(final boolean value, final int zone) {
 
         BYPASS = Boolean.parseBoolean(GetJson.getInstance().callGetJsonWebService(USER_PREFERENCES,"bypass"));
 
         if (!BYPASS){
             if (zone == Integer.parseInt(MainApp.ZONES[0])) {
                         zone1 = value;
               } else if(MainApp.ZONES.length > 1){
                 if (zone == Integer.parseInt(MainApp.ZONES[1]) || zone == Integer.parseInt(MainApp.ZONES[2])) {
                         zone2 = value;
                     }
             }
 
             //Zone = value;
 
             UberdustClient.getInstance().sendCoapPost(FOI_ACTUATOR, "lz" + zone, value ? "1" : "0"); //FOI_ACTUATOR
       }
     }
 
     public static void main(final String[] args) {
         FoiController.getInstance();
     }
 
 }
