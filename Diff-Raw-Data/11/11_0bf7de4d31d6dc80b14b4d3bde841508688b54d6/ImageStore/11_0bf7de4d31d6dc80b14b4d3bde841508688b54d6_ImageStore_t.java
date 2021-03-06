 package core;
 
 import java.util.HashMap;
 import java.util.Map;
 import core.ConstantStore;
 //import org.newdawn.slick.Animation;
 import org.newdawn.slick.Image;
 import org.newdawn.slick.SlickException;
 
 /**
  * Manages images for use in the game.
  */
 public class ImageStore {	
 	private static ImageStore imageStore;
 
 	public final Map<String, Image> IMAGES;
 	
 	/**
 	 * Constructs a new {@code ImageStore} and initializes it.
 	 */
 	private ImageStore() {
 		imageStore = this;
 		
 		IMAGES = new HashMap<String, Image>();
 		
 		try {
 			initialize();
 		} catch (SlickException e) {
 			e.printStackTrace();
 		}
 	}
 	
 	/**
 	 * Initializes the store with all the images.
 	 * @throws SlickException
 	 */
 	private void initialize() throws SlickException {
 		IMAGES.put("NULL",  new Image(ConstantStore.PATH_LOGO + "null.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("VOID", new Image(ConstantStore.PATH_LOGO + "void.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("LOGO", new Image(ConstantStore.PATH_GRAPHICS + "logo.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("MAP_BACKGROUND", new Image(ConstantStore.PATH_BKGRND + "map.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("TRAIL_MAP", new Image(ConstantStore.PATH_BKGRND + "playerMap.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("DIRT_BACKGROUND", new Image(ConstantStore.PATH_BKGRND + "dark_dirt.png"));
 		IMAGES.put("CAMP_ICON", new Image(ConstantStore.PATH_ICONS + "fire.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("INVENTORY_ICON", new Image(ConstantStore.PATH_ICONS + "pack.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("TRAIL_ICON", new Image(ConstantStore.PATH_ICONS + "trail.png", false, Image.FILTER_NEAREST));
 		
 		IMAGES.put("MAP_POINTER1", new Image(ConstantStore.PATH_ICONS + "mapPointer1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("MAP_POINTER2", new Image(ConstantStore.PATH_ICONS + "mapPointer2.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("MAP_POINTER3", new Image(ConstantStore.PATH_ICONS + "mapPointer3.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("MAP_POINTER4", new Image(ConstantStore.PATH_ICONS + "mapPointer4.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("MAP_POINTER5", new Image(ConstantStore.PATH_ICONS + "mapPointer5.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("MAP_POINTER6", new Image(ConstantStore.PATH_ICONS + "mapPointer6.png", false, Image.FILTER_NEAREST));
 		
 		IMAGES.put("MAP_PARTY1", new Image(ConstantStore.PATH_ICONS + "partyMapRep1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("MAP_PARTY2", new Image(ConstantStore.PATH_ICONS + "partyMapRep2.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("MAP_PARTY3", new Image(ConstantStore.PATH_ICONS + "partyMapRep3.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("MAP_PARTY4", new Image(ConstantStore.PATH_ICONS + "partyMapRep4.png", false, Image.FILTER_NEAREST));
 		
 		IMAGES.put("GRASS", new Image(ConstantStore.PATH_GROUND + "grass.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("TRAIL", new Image(ConstantStore.PATH_GROUND + "trail.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HILL_A", new Image(ConstantStore.PATH_BKGRND + "hill_a.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HILL_B", new Image(ConstantStore.PATH_BKGRND + "hill_b.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("TREE", new Image(ConstantStore.PATH_GROUND + "tree.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("DEER", new Image(ConstantStore.PATH_ANIMALS + "deer.png", false, Image.FILTER_NEAREST));
 		
 		IMAGES.put("CLOUD_A", new Image(ConstantStore.PATH_BKGRND + "cloud_a.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("CLOUD_B", new Image(ConstantStore.PATH_BKGRND + "cloud_b.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("CLOUD_C", new Image(ConstantStore.PATH_BKGRND + "cloud_c.png", false, Image.FILTER_NEAREST));
 		
 		IMAGES.put("TRAIL_WAGON", new Image(ConstantStore.PATH_TRAIL + "trailWagon.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("SMALL_WHEEL_1", new Image(ConstantStore.PATH_TRAIL + "smallwheel1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("SMALL_WHEEL_2", new Image(ConstantStore.PATH_TRAIL + "smallwheel2.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("SMALL_WHEEL_3", new Image(ConstantStore.PATH_TRAIL + "smallwheel3.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("BIG_WHEEL_1", new Image(ConstantStore.PATH_TRAIL + "wheel1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("BIG_WHEEL_2", new Image(ConstantStore.PATH_TRAIL + "wheel2.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("BIG_WHEEL_3", new Image(ConstantStore.PATH_TRAIL + "wheel3.png", false, Image.FILTER_NEAREST));
 				
 		//images for hunt scene
 		//hunter
 		IMAGES.put("HUNT_RETICLE", new Image(ConstantStore.PATH_HUNT + "huntReticle.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HUNTER_LEFT", new Image(ConstantStore.PATH_HUNT + "hunterLeftFaceSide1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HUNTER_RIGHT", new Image(ConstantStore.PATH_HUNT + "hunterRightFaceSide1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HUNTER_FRONT", new Image(ConstantStore.PATH_HUNT + "hunterFaceFront1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HUNTER_BACK", new Image(ConstantStore.PATH_HUNT + "hunterFaceBack1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HUNTER_UPPERLEFT", new Image(ConstantStore.PATH_HUNT + "hunterLeftFaceBack1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HUNTER_UPPERRIGHT", new Image(ConstantStore.PATH_HUNT + "hunterRightFaceBack1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HUNTER_LOWERLEFT", new Image(ConstantStore.PATH_HUNT + "hunterLeftFaceFront1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HUNTER_LOWERRIGHT", new Image(ConstantStore.PATH_HUNT + "hunterRightFaceFront1.png", false, Image.FILTER_NEAREST));
 		//hunted prey
 		for (int incr = 1; incr < 7; incr ++){
 			String pigName = "pigBack" + incr + ".png";
 			String pigImgName = "HUNT_PIGBACK" + incr;
 			String cowName = "cowBack" + incr + ".png";
 			String cowImgName = "HUNT_COWBACK" + incr;
 			
 			IMAGES.put(pigImgName, new Image(ConstantStore.PATH_HUNTPREY + pigName, false, Image.FILTER_NEAREST));
 			IMAGES.put(cowImgName, new Image(ConstantStore.PATH_HUNTPREY + cowName, false, Image.FILTER_NEAREST));
 
 			pigName = "pigFront" + incr + ".png";
 			pigImgName = "HUNT_PIGFRONT" + incr;
 			cowName = "cowFront" + incr + ".png";
 			cowImgName = "HUNT_COWFRONT" + incr;
 			
 			IMAGES.put(pigImgName, new Image(ConstantStore.PATH_HUNTPREY + pigName, false, Image.FILTER_NEAREST));
 			IMAGES.put(cowImgName, new Image(ConstantStore.PATH_HUNTPREY + cowName, false, Image.FILTER_NEAREST));
 
 			pigName = "pigRight" + incr + ".png";
 			pigImgName = "HUNT_PIGRIGHT" + incr;
 			cowName = "cowRight" + incr + ".png";
 			cowImgName = "HUNT_COWRIGHT" + incr;
 			
 			IMAGES.put(pigImgName, new Image(ConstantStore.PATH_HUNTPREY + pigName, false, Image.FILTER_NEAREST));
 			IMAGES.put(cowImgName, new Image(ConstantStore.PATH_HUNTPREY + cowName, false, Image.FILTER_NEAREST));
 
 			pigName = "pigLeft" + incr + ".png";
 			pigImgName = "HUNT_PIGLEFT" + incr;
 			cowName = "cowLeft" + incr + ".png";
 			cowImgName = "HUNT_COWLEFT" + incr;
 			
 			IMAGES.put(pigImgName, new Image(ConstantStore.PATH_HUNTPREY + pigName, false, Image.FILTER_NEAREST));
 			IMAGES.put(cowImgName, new Image(ConstantStore.PATH_HUNTPREY + cowName, false, Image.FILTER_NEAREST));	
 		}
 		for (int incr = 7; incr < 10; incr ++){
 			String cowName = "cowBack" + incr + ".png";
 			String cowImgName = "HUNT_COWBACK" + incr;
 			
 			IMAGES.put(cowImgName, new Image(ConstantStore.PATH_HUNTPREY + cowName, false, Image.FILTER_NEAREST));
 
 			cowName = "cowFront" + incr + ".png";
 			cowImgName = "HUNT_COWFRONT" + incr;
 			
 			IMAGES.put(cowImgName, new Image(ConstantStore.PATH_HUNTPREY + cowName, false, Image.FILTER_NEAREST));
 
 			cowName = "cowRight" + incr + ".png";
 			cowImgName = "HUNT_COWRIGHT" + incr;
 			
 			IMAGES.put(cowImgName, new Image(ConstantStore.PATH_HUNTPREY + cowName, false, Image.FILTER_NEAREST));
 
 			cowName = "cowLeft" + incr + ".png";
 			cowImgName = "HUNT_COWLEFT" + incr;
 			
 			IMAGES.put(cowImgName, new Image(ConstantStore.PATH_HUNTPREY + cowName, false, Image.FILTER_NEAREST));	
 		}
 		
 		//terrain background
 		IMAGES.put("HUNT_GRASS", new Image(ConstantStore.PATH_HUNTBKG + "grassBG.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HUNT_SNOW", new Image(ConstantStore.PATH_HUNTBKG + "snowBG.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HUNT_MOUNTAIN", new Image(ConstantStore.PATH_HUNTBKG + "mountainBG.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HUNT_DESERT", new Image(ConstantStore.PATH_HUNTBKG + "desertBG.png", false, Image.FILTER_NEAREST));
 		//terrain - images for trees and rocks, collision and display
 		for (int incr = 0; incr < 16 ; incr++){
 			String imageDir = (incr == 10) ? "a" : 
 					(incr == 11) ? "b" : 
 					(incr == 12) ? "c" : 
 					(incr == 13) ? "d" : 
 					(incr == 14) ? "e" : 
 					(incr == 15) ? "f" : Integer.toString(incr);					
 				
 			String identifier = "tree1";
 			String imageIndex = "HUNT_TREE1" + imageDir.toUpperCase() + "0";
 			String imageName = identifier + imageDir + "0.png";
 			//System.out.println("tree1 index : " + imageIndex + " fileName : " + imageName);
 			IMAGES.put(imageIndex, new Image(ConstantStore.PATH_HUNTTERRAIN + imageName, false, Image.FILTER_NEAREST));
 			imageIndex = "HUNT_TREE1" + imageDir.toUpperCase() + "1";
 			imageName = identifier + imageDir + "1.png";
 			//System.out.println("tree1 index : " + imageIndex + " fileName : " + imageName);
 			IMAGES.put(imageIndex, new Image(ConstantStore.PATH_HUNTTERRAIN + imageName, false, Image.FILTER_NEAREST));
 			
 			imageIndex = "HUNT_TREE1" + imageDir.toUpperCase() + "0SHAD";
 			imageName = identifier + imageDir + "0Shad.png";
 			//System.out.println("tree1 index : " + imageIndex + " fileName : " + imageName);
 			IMAGES.put(imageIndex, new Image(ConstantStore.PATH_HUNTTERRAIN + imageName, false, Image.FILTER_NEAREST));
 			imageIndex = "HUNT_TREE1" + imageDir.toUpperCase() + "1SHAD";
 			imageName = identifier + imageDir + "1Shad.png";
 			//System.out.println("tree1 index : " + imageIndex + " fileName : " + imageName);
 			IMAGES.put(imageIndex, new Image(ConstantStore.PATH_HUNTTERRAIN + imageName, false, Image.FILTER_NEAREST));
 			
 			identifier = "rock1";
 			imageIndex = "HUNT_ROCK1" + imageDir.toUpperCase() + "0";
 			imageName = identifier + imageDir + "0.png";
 			//System.out.println("rock1 index : " + imageIndex + " fileName : " + imageName);
 			IMAGES.put(imageIndex, new Image(ConstantStore.PATH_HUNTTERRAIN + imageName, false, Image.FILTER_NEAREST));
 			imageIndex = "HUNT_ROCK1" + imageDir.toUpperCase() + "1";
 			imageName = identifier + imageDir + "1.png";
 			//System.out.println("rock1 index : " + imageIndex + " fileName : " + imageName);
 			IMAGES.put(imageIndex, new Image(ConstantStore.PATH_HUNTTERRAIN + imageName, false, Image.FILTER_NEAREST));
 			
 			imageIndex = "HUNT_ROCK1" + imageDir.toUpperCase() + "0SHAD";
 			imageName = identifier + imageDir + "0Shad.png";
 			//System.out.println("rock1 index : " + imageIndex + " fileName : " + imageName);
 			IMAGES.put(imageIndex, new Image(ConstantStore.PATH_HUNTTERRAIN + imageName, false, Image.FILTER_NEAREST));
 			imageIndex = "HUNT_ROCK1" + imageDir.toUpperCase() + "1SHAD";
 			imageName = identifier + imageDir + "1Shad.png";
 			//System.out.println("rock1 index : " + imageIndex + " fileName : " + imageName);
 			IMAGES.put(imageIndex, new Image(ConstantStore.PATH_HUNTTERRAIN + imageName, false, Image.FILTER_NEAREST));	
 		}
 
 		
 		//trail people
 		IMAGES.put("TRAPPER_LEFT", new Image(ConstantStore.PATH_PEOPLE + "hunterLeftFaceSide1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("TRAPPER_RIGHT", new Image(ConstantStore.PATH_PEOPLE + "hunterRightFaceSide1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("MAIDEN_LEFT", new Image(ConstantStore.PATH_PEOPLE + "maidenLeftFaceSide1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("MAIDEN_RIGHT", new Image(ConstantStore.PATH_PEOPLE + "maidenRightFaceSide1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HILLBILLY_LEFT", new Image(ConstantStore.PATH_PEOPLE + "hillbillyLeftFaceSide1.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("HILLBILLY_RIGHT", new Image(ConstantStore.PATH_PEOPLE + "hillbillyRightFaceSide1.png", false, Image.FILTER_NEAREST));
 		
 		IMAGES.put("STORE_BUILDING", new Image(ConstantStore.PATH_BUILDINGS + "general-store.png", false, Image.FILTER_NEAREST));
 		IMAGES.put("SALOON_BUILDING", new Image(ConstantStore.PATH_BUILDINGS + "tavern.png", false, Image.FILTER_NEAREST));
 		
		IMAGES.put("SALOON_BACKGROUND", new Image(ConstantStore.PATH_BKGRND + "saloonBkg.png", false, Image.FILTER_NEAREST));
 	}
 	
 	/**
 	 * Get the {@code Image} for the key.
 	 * @param name Key for the image
 	 * @return Image at that key, if any
 	 */
 	public Image getImage(String name) {
 		return IMAGES.get(name);
 	}
 	
 	/**
 	 * Returns the {@code ImageStore} instance. Creates one if one does not exist.
 	 * @return Instance of ImageStore
 	 */
 	public static ImageStore get() {
 		if (imageStore == null) {
 			imageStore = new ImageStore();
 		}
 		
 		return imageStore;
 	}
 }
