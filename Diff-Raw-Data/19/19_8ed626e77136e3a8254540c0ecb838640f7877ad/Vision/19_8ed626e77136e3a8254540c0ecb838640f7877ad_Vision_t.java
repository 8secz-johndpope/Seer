 package vision;
 
 import javax.xml.bind.JAXBException;
 
 import vision.controller.Controller;
 
 import vision.model.Model;
 
 import vision.view.View;
 
 /**
  * Main class
  * starts up the whole software 
  *
  */
 public class Vision {
 	
 		
 
 	public static void main(String[] args) {
 		
 		View mainView = new View();
 		Model mainModel = null;
 		try {
 			mainModel = new Model(mainView);
 		} catch (JAXBException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
			return;
 		}
 		Controller mainController = new Controller(mainView, mainModel);
 		mainView.setController(mainController);
 		mainView.setDaten(mainModel);
 		mainView.start();
 	}
 	
 	
 	
 }
