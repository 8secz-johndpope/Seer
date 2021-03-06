 import java.io.*;
 
 import org.jdom2.Document;
 import org.jdom2.output.Format;
 import org.jdom2.output.XMLOutputter;
 
 import com.amazonaws.auth.AWSCredentials;
 import com.amazonaws.auth.PropertiesCredentials;
 
 
 
 
 class Server {
 
 	
 	
 	public static void main(String[] Arg) {
 		
 		String message = null;
 		String requestId, requestType,timeStart,timeStop,cellID,location;
 		String xmlString = null;
 		Document doc = null;
 		SQSAccess queue = null;
 		S3Access s3 = null;
 		DDBReader ddbRead = null;
 		AWSCredentials credentials = null;
 		File file = new File("src/AwsCredentials.properties");
 		
 		try {
 			credentials = new PropertiesCredentials(file);
 		} catch (IOException e1) {
 			System.out.println("Credentials were not properly entered into AwsCredentials.properties.");
 			System.out.println(e1.getMessage());
 			System.exit(-1);
 		}
 		System.out.println("Credentials are fine!");
 		
 		try {
 		queue = new SQSAccess(credentials);
 		s3 = new S3Access(credentials);
 		ddbRead = new DDBReader(credentials);
 		
 		}catch(Exception e2) {
 			e2.printStackTrace();
 		}
 		
 		
 		while (true) {
 		message = null;
 	 	
 		while(message == null)
 	 	{
 			message = queue.getXML();
 			try {
 				Thread.sleep(1000);
 			} catch (InterruptedException e) {
 				e.printStackTrace();
 			}
 	 	}
 			RequestID req = new RequestID();
 			try{
 			req.createParse(message);
 			}catch(Exception e3){
 				System.out.println("Error parsing XML file");
 			}
 			
 			requestId = req.getRacine();
 			requestType = req.getType();
 			timeStart = req.getTimeStart();
 			timeStop = req.getTimeStop();
 			cellID = req.getCellID();
 			
 			System.out.println("receive request n° " + requestId + " with type " + requestType );
 			System.out.println("TimeStart " + timeStart + " timeStop " + timeStop + " cellID " + cellID + "\n");
 			
 			
 	
 				if(requestType.equals("CellStatNet")) {
 					doc = ddbRead.reqCellStatNet(requestId, timeStart, timeStop, cellID);
 				}
 				else if (requestType.equals("CellStatSpeed")) {
 					
 					doc = ddbRead.reqCellStatSpeed(requestId, timeStart, timeStop, cellID);
 				}											
 				else if( requestType.equals("ListCells")) {
 					doc = ddbRead.reqListCells(requestId);
 				}			
 				XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
 		        xmlString = outputter.outputString(doc);
 				location = s3.uploadBucket(requestId + ".xml", new ByteArrayInputStream(xmlString.getBytes()));
 				queue.sendAnswer(location);
 			
 			}
 		}
 		
 	
 	}
 	
 
 
