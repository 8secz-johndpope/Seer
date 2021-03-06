 package AP2DX.sensor;
 
 import java.util.ArrayList;
 
 import AP2DX.AP2DXBase;
 import AP2DX.AP2DXMessage;
 import AP2DX.Message;
 import AP2DX.Module;
 import AP2DX.specializedMessages.*;
 
 
 public class Program extends AP2DXBase {
     /**
 	 * Entrypoint of mapper
 	 */
 	public static void main (String[] args){
 		new Program();
 	}
 
 	
 	/**
 	 * constructor
 	 */
 	public Program() 
     {
         super(Module.SENSOR); // explicitly calls base constructor
 		System.out.println(" Running Sensor... ");
 
 	}
 	
 	@Override
 	protected void doOverride() {
 
 	}
 
 
 	@Override
	public ArrayList<AP2DXMessage> componentLogic(Message msg) 
     {
        ArrayList<AP2DXMessage> messageList = new ArrayList<AP2DXMessage>();
 
         if (!msg.getMsgType().isAp2dxMessage)
         {
             System.out.println("Unexpected message in ap2dx.sensor.Program, was not an AP2DX message type.");
             return null;
         }
         
         switch (msg.getMsgType())
         {
             case AP2DX_SENSOR_SONAR:
                 SonarSensorMessage sonarSensorMessage = new SonarSensorMessage((AP2DXMessage)msg, IAM, Module.REFLEX);
                 messageList.add(sonarSensorMessage);
                 break;
             default:
                 System.out.println("Unexpected message type in ap2dx.sensor.Program: " + msg.getMsgType());
         }
 		return messageList;
 
 	}
 }
