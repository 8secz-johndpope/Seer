 import java.awt.Color;
 import java.awt.Cursor;
 import java.awt.Font;
 import java.awt.Image;
 import java.awt.Toolkit; 
 import java.awt.Point;
 import java.awt.Event;
 import java.awt.Graphics;
 import java.awt.image.MemoryImageSource;
 import java.io.IOException;
 import java.applet.*;
 import render.*;
 public class game_new extends RenderApplet{
 	Material boxColor, pumpkinColor1, springColor, stalkColor,lineColor,wallColor,groundColor;
 	Material gunColor,barrelColor,gunheadColor,gunRing1Color,gunRing2Color,gunRing3Color,laserColor,gunWingColor;
 	// ghost
 	Material shirtColor, skinColor, bodyColor, eyeColor;
 	
 	Font bigFont = new Font("Helvetica", Font.BOLD, 23);
 	int enemyNumber = 3;
 	int springNumber = 10;
 	int pumpkinNumber = 4;
 	int pumpkinSections = 4;
 	int score = 0;
 	int levelScore = 10;
 	int miss = 0;
 	int bullet = 0;
 	int totalBullet = 100;
 	int level = 1;
 	int endGame = 0;
 	int reStart = 0;
 	int addLightCount = 0;
 	int turn = 0, leftKey = 0, rightKey = 0;
 	double turningAngle = 0;
 	int gunEnergy = 100;
 	int shootEnergy = 8;
 	double reloadCount = 0;
 	double previousTime = 0;
 	static final double chargeRate = .15;
 	Geometry box[][] = new Geometry[enemyNumber][2];
 	Geometry stalk[] = new Geometry[enemyNumber];
 	Geometry spring[] = new Geometry[springNumber*enemyNumber];
 	Geometry pumpkin[] = new Geometry[pumpkinNumber*enemyNumber];
 	Material pumpkinFadeColor[] = new Material[enemyNumber];
 	
 	double pumpkin_vecs[][] = new double[pumpkinSections*pumpkinSections][5]; // x, y, z, u, v
 	
 	Geometry line,wall,ground;
 	Geometry gun,gunBody,gunHead,barrel,gunRing1,gunRing2,gunRing3,laser,gunWing;
 	//ghost
 	Geometry body[] = new Geometry[enemyNumber];
 	Geometry shoulder_r[] = new Geometry[enemyNumber];
 	Geometry shoulder_l[] = new Geometry[enemyNumber];
 	Geometry eye_r[] = new Geometry[enemyNumber];
 	Geometry eye_l[] = new Geometry[enemyNumber];
 	Geometry torso[] = new Geometry[enemyNumber];
 	Geometry hand_r[] = new Geometry[enemyNumber];
 	Geometry hand_l[] = new Geometry[enemyNumber];
 	
 	
 	boolean isCapturedClick = true;
 	double point[] = new double[3];
 	Matrix m;
 	Geometry g = new Geometry();
 	double time = 0;
 	double shootTime = 0;
 	double clickTime[] = new double[enemyNumber];
 	boolean isShoot[] = new boolean[enemyNumber];
 	double swingTime;
 	double runTime[] = new double[enemyNumber];
 	
 	static final double explodeTime = .25;	// how long a pumpkin takes to explode
 	static final double ghostTime = 10.;// how long a ghost takes to approach
 	static final double pumpkinTime = 10.;	// how long a pumpkin takes to approach
 	static final double startDist = 50.;		// distance from which enemies appear
 	Texture texture;
 	int H, W;
 	int mouseX, mouseY;
 	AudioClip gunShot,blowup;
 	
 	
 	public boolean keyDown(Event evt, int key){
 		if (key == 'A' || key == 'a'){
 			leftKey = 1;
 			turn = -1;
 		}
 		else if (key == 'D' || key == 'd'){
 			rightKey = 1;
 			turn = 1;
 		}
 			
 		return true;
 	}
 	
 	public boolean keyUp(Event evt, int key){
 		if (key == 'A' || key == 'a'){
 			leftKey = 0;
 			if (rightKey == 0)
 				turn = 0;
 			else 
 				turn = 1;
 		}
 		else if (key == 'D' || key == 'd'){
 			rightKey = 0;
 			if (leftKey == 0)
 				turn = 0;
 			else 
 				turn = -1;
 		}
 			
 		return true;
 	}
 	
 	public boolean mouseDown(Event e, int x, int y) {
 		isCapturedClick = true;
 		if (endGame == 0){
 			if (gunEnergy >= shootEnergy){
 				gunEnergy -= shootEnergy;
 				bullet++;
 		//		totalBullet--;
 				g = queryCursor(point);
 				
 				shootTime = time;
 		//		gunTheta = -1*Math.atan2(point[0],-point[2]);
 		//	    gunPhi = 1*Math.atan2(point[1], Math.sqrt(Math.pow((point[0]),2)+Math.pow(-point[2], 2)));
 		//		
 				gunShot.play();
 				if (g == null)
 					return true;
 				for (int i=0;i<stalk.length;i++){
 					if((g==stalk[i] || g.isDescendant(box[i][1])) && isShoot[i] == false){
 						isShoot[i] = true;
 						clickTime[i] = time;
 						score=score+2;
 		//				totalBullet = totalBullet + 10;
 		//				System.out.print("stalk shot");
 					}
 				}
 				for (int i=0;i<pumpkin.length;i++){
 					if (g.isDescendant(pumpkin[i]) && isShoot[(int)i/pumpkinNumber] == false){
 						isShoot[(int)i/pumpkinNumber] = true;
 						clickTime[(int)i/pumpkinNumber] = time;
 						score++;
 					}
 				}
 			}
 			else 
 				System.out.print("run out of energy");
 		}
 		return true;
 	}
 	
 		public boolean mouseMove(Event e, int x, int y){
 			
 			mouseX = x;
 			mouseY = y;
 			return false;
 		}
 		
 	   public boolean mouseDrag(Event e, int x, int y) {
 		   
 		   mouseX = x;
 			mouseY = y;
 //	      if (isCapturedClick)
 //	         return true;
 	      
 	      return false;
 	   }
 
 	   public boolean mouseUp(Event e, int x, int y) {
 	      if (isCapturedClick) {
 	         isCapturedClick = false;
 //	         return true;
 	      }
 	      if (endGame == 1 && x<100 && y < 50){
 
 	    	
 	    	reStart = 1;
 //	    	initialize();
 	    	
 	      }
 	      return false;
 	   }
 
 	   public void initialize() {
 		   
 		   	enemyNumber++;
 		    box = new Geometry[enemyNumber][2];
 			stalk = new Geometry[enemyNumber];
 			spring = new Geometry[springNumber*enemyNumber];
 			pumpkin = new Geometry[pumpkinNumber*enemyNumber];
 			pumpkin_vecs = new double[pumpkinSections*pumpkinSections][5];
 			pumpkinFadeColor = new Material[enemyNumber];
 			//ghost
 			body = new Geometry[enemyNumber];
 			shoulder_r = new Geometry[enemyNumber];
 			shoulder_l = new Geometry[enemyNumber];
 			eye_r = new Geometry[enemyNumber];
 			eye_l = new Geometry[enemyNumber];
 			torso = new Geometry[enemyNumber];
 			hand_r = new Geometry[enemyNumber];
 			hand_l = new Geometry[enemyNumber];
 			
 			isCapturedClick = true;
 			point = new double[3];
 			//time = 0;
 			shootTime = 0;
 			clickTime = new double[enemyNumber];
 			isShoot = new boolean[enemyNumber];
 			runTime = new double[enemyNumber];
 		   
 			Y= new double[enemyNumber];
 			swingX = 0;
 		    swingY = new double[enemyNumber];
 		    pop = 0;
 		    dx = new double[enemyNumber]; //moving formula of x-coordinate 
 		    dz = new double[enemyNumber]; //moving formula of z-coordinate
 		    enemyAngle = new double[enemyNumber];
 		    enemySpeed = new double[enemyNumber];
 
 		    isMiss = new boolean[enemyNumber];
 		    c = 0;
 		    
 		   //hide cursor
 		   int[] pixels = new int[16 * 16];  
 		   Image image = java.awt.Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, pixels, 0, 16));   
 		   Cursor transparentCursor = java.awt.Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "invisiblecursor"); //invisiblecursor 
 		   setCursor(transparentCursor);   
 		   
 		   
 		   gunShot = getAudioClip(getCodeBase(), "LASER.wav");
 		   blowup = getAudioClip(getCodeBase(), "pop8.wav");
 		   for (int i=0;i<enemyNumber;i++)
 			   Y[i] = Math.random();
 		   H = getHeight();
 		   W = getWidth();
 		   //when does the enemy start to come out
 		   for (int i=0;i<runTime.length;i++)
 			   runTime[i] = time;
 		   //enemy is shoot or not
 		   for (int i=0;i<isShoot.length;i++)
 				isShoot[i] = false;
 		   //is miss counted or not
 		   for (int i=0;i<isMiss.length;i++)
 			   isMiss[i] = false;
 		  //set colors
 	      setBgColor(0, .5, 0);
 	      if (addLightCount == 0){
 	    	  addLightCount++;
 		      addLight( 1, 1, 1, .8, .85, 1);
 		      addLight(-1,-1,-1, 1, 1, 1);
 		      addLight(-1, 1, 1, .8, .85, 1);
 		      addLight(-1, 1,-1, .5, .5, .5);
 	      }
 	      gunColor = new Material();
 	      gunColor.setAmbient(1, 1, 1);
 	      gunColor.setDiffuse(.2, .2, .2);
 	      gunColor.setSpecular(.5, .5, .5, 20);
 	      barrelColor = new Material();
 	      barrelColor.setAmbient(0, 1, 0);
 	      barrelColor.setDiffuse(0, 1, 0);
 	      barrelColor.setSpecular(.8, .5, 0, 20);
 	      gunheadColor = new Material();
 	      gunheadColor.setAmbient(.1, .1, .1);
 	      gunheadColor.setDiffuse(.2, .2, .2);
 	      gunheadColor.setSpecular(.2, .2, .2, 20);
 	      gunRing1Color = new Material();
 	      gunRing1Color.setAmbient(.5, .1, .5);
 	      gunRing1Color.setDiffuse(.2, .2, .2);
 	      gunRing1Color.setSpecular(.2, .2, .2, 20);
 	      gunRing2Color = new Material();
 	      gunRing2Color.setAmbient(.5, .1, .5);
 	      gunRing2Color.setDiffuse(.2, .2, .2);
 	      gunRing2Color.setSpecular(.2, .2, .2, 20);
 	      gunRing3Color = new Material();
 	      gunRing3Color.setAmbient(.5, .1, .5);
 	      gunRing3Color.setDiffuse(.2, .2, .2);
 	      gunRing3Color.setSpecular(.2, .2, .2, 20);
 	      laserColor = new Material();
 	      laserColor.setAmbient(0, .4, 0);
 	      laserColor.setDiffuse(0, .2, 0);
 	      laserColor.setSpecular(0, 1, 0, 1);
 	      gunWingColor = new Material();
 	      gunWingColor.setAmbient(.7, .7, .7);
 	      gunWingColor.setDiffuse(.1, .1, .1);
 	      gunWingColor.setSpecular(.1, .1, .1, 20);
 	      
 	      //ghost
 	      // Body Color
 	      bodyColor = new Material();
 	      bodyColor.setAmbient(0.4, 0.4,0.4);
 	      bodyColor.setDiffuse(0.8, 0.8, 0.75);
 	      bodyColor.setSpecular(0,0,0,1);
 	       
 	      // Eye Color
 	      eyeColor = new Material();
 	      eyeColor.setAmbient(0, 0, 0);
 	      eyeColor.setDiffuse(0, 0, 0);
 	      eyeColor.setSpecular(1,1,1,10);
 	      
 	      
 	      wallColor = new Material();
 	      wallColor.setAmbient(0, 0, 0);
 	      wallColor.setDiffuse(0, 0, 0);
 	      wallColor.setSpecular(.2, .2, .2, 20);
 	      
 	      groundColor = new Material();
 	      groundColor.setAmbient(.5, .1, .5);
 	      groundColor.setDiffuse(.2, .2, .2);
 	      groundColor.setSpecular(.2, .2, .2, 20);
 	      
 	      lineColor = new Material();
 	      lineColor.setAmbient(0.0, 0.0, 0.5);
 	      lineColor.setDiffuse(0.0, 0.0, 0.8);
 	      lineColor.setSpecular(.2, .5, 0, 20);
 	      
 	      boxColor = new Material();
 	      boxColor.setAmbient(0.5, 0.0, 0.2);
 	      boxColor.setDiffuse(0.0, 0.0, 0.8);
 	      boxColor.setSpecular(.2, .5, 0, 20);
 
 	      pumpkinColor1 = new Material();
 	      pumpkinColor1.setAmbient(0.6, 0.2, 0);
 	      pumpkinColor1.setDiffuse(.6, .3, 0);
 	      
 	      for (int i = 0; i < enemyNumber; i++){
 	    	  pumpkinFadeColor[i] = new Material();
 	    	  pumpkinFadeColor[i].copy(pumpkinColor1);
 	    	  pumpkinFadeColor[i].setTransparency(0.);
 	      }
 		
 	      springColor = new Material();
 	      springColor.setAmbient(0.2, 0.2, 0.2);
 	      springColor.setDiffuse(0.1, 0.1, 0.1);
 	      springColor.setSpecular(1, 1, 1, 50);
 
 	      stalkColor = new Material();
 	      stalkColor.setAmbient(.0, 0.2, .0);
 	      stalkColor.setDiffuse(0.0, 0, 0);
 	      stalkColor.setSpecular(0, 0, 0, 1);
 	      
 	      //add geometries
 	      	gun = getWorld().add();
 	      	gunBody = gun.add().sphere(16);
 	      	barrel = gun.add().cylinder(16);
 	      	gunHead = gun.add().sphere(8);
 	      	gunRing1 = gun.add().torus(16, 16, .1);
 	      	gunRing2 = gun.add().torus(16, 16, .1);
 	      	gunRing3 = gun.add().torus(16, 16, .1);
 	      	gunWing = gun.add().sphere(16);
 	      	laser = gun.add().cylinder(16);
 	      	laser.setVisible(false);
 	      	
 	      	gunBody.setMaterial(gunColor);
 	      	barrel.setMaterial(barrelColor);
 	      	gunHead.setMaterial(gunheadColor);
 	      	gunRing1.setMaterial(gunRing1Color);
 	      	gunRing2.setMaterial(gunRing2Color);
 	      	gunRing3.setMaterial(gunRing3Color);
 	      	gunWing.setMaterial(gunWingColor);
 	      	laser.setMaterial(laserColor);
 	      	
 	      	wall = getWorld().add().torus(16, 16, .2);
 	      	wall.setMaterial(wallColor);
 	      	ground = getWorld().add().cube();
 	      	ground.setMaterial(groundColor);
 	      	line = getWorld().add().torus(8, 8, .2);
 	      	line.setMaterial(lineColor);
 	      //the Hierarchy of the enemy
 	      //cube for box->first torus for spring->rest toruses for spring->all spheres for pumpkin->cylinder for stalk
 	      //add all boxes to world
 
 	      	for (int i=0;i<box.length;i++){
 	      	box[i][0] = getWorld().add().cube();
 	      	
 	      	 //ghost
 	          body[i] = getWorld().add();
 	          box[i][1] = body[i];
 	          shoulder_r[i] = body[i].add();
 	          shoulder_l[i] = body[i].add();
 	          
 	          torso[i] = body[i].add().partialSphere(32);
 	          torso[i].setMaterial(bodyColor);
 	          
 	          eye_r[i] = body[i].add().sphere(16);
 	          eye_r[i].setMaterial(eyeColor);
 	          
 	          eye_l[i] = body[i].add().sphere(16);
 	          eye_l[i].setMaterial(eyeColor);
 	          
 	          hand_r[i] = shoulder_r[i].add().sphere(16);
 	          hand_r[i].setMaterial(bodyColor);
 	          
 	          hand_l[i] = shoulder_l[i].add().sphere(16);
 	          hand_l[i].setMaterial(bodyColor);	    
 	      	
 	      }
 	      	//add first torus to box
 	      for (int i=0;i<enemyNumber;i++){
 	    	  spring[i*springNumber] = box[i][0].add().torus(16, 16, .2);
 	      }
 	      //add rest torus to the previous one
 	      for (int i = 0;i <enemyNumber;i++)
 	    	  for (int j=1;j<springNumber;j++)
 	    	  spring[i*springNumber+j] = spring[i*springNumber+j-1].add().torus(16, 16, .2);
 	      
 	      	// compute coordinates for the parts of the pumpkin (pre-explosion)
 		    // based on Geometry::globe(int m, int n, double uLo, double uHi, double vLo, double vHi)
 		    for (int u = 0, i = 0;u < pumpkinSections;u++)
 			  for (int v = 0;v < pumpkinSections;v++)
 				  {
 					  double su = (u*1.+.5)/pumpkinSections;	// u scaled to [0..1) 
 					  double sv = (v*1.+.5)/pumpkinSections;
 					  double theta = 2 * su * Math.PI;
 			          double phi = (sv-.5) * Math.PI;
 			          double x = Math.cos(phi) * Math.cos(theta);
 			          double y = Math.cos(phi) * Math.sin(theta);
 			          double z = Math.sin(phi);
 			          
 			          pumpkin_vecs[i][0] = x;
 			          pumpkin_vecs[i][1] = y;
 			          pumpkin_vecs[i][2] = z;
 			          pumpkin_vecs[i][3] = su;
 			          pumpkin_vecs[i][4] = sv;
 			          
 			          i++;
 				  }
 	   
 	      //add all sphere of pumpkin to the last torus of spring
 	      for (int i = 0;i <enemyNumber;i++)
 	    	  for (int j = 0;j < pumpkinNumber;j++)
 	    	  {
 	    		  Geometry g = pumpkin[i*pumpkinNumber+j] = spring[(i+1)*springNumber-1].add();
 	    		  for (double[] v : pumpkin_vecs)
 	    		  {
 	    				 Geometry piece = g.add().globe(4, 4, v[3], v[3]+(1.0/pumpkinSections), v[4], v[4]+(1.0/pumpkinSections));
 	    		  }
 	    	  }
 	      
 	      //add cylinder to the last torus of spring
 	      for (int i=0;i<enemyNumber;i++){
 	    	  stalk[i] = spring[(i+1)*springNumber-1].add().cylinder(16);	          
 	      }    
 	      
 	      for (int i=0;i<stalk.length;i++){
 	    	  stalk[i].setMaterial(stalkColor);
 	      }
 	      //set material of all geometries
 	      for (int i=0;i<box.length;i++)
 	    	  box[i][0].setMaterial(boxColor);
 	      for (int i = 0;i < spring.length;i++)
 	    	  spring[i].setMaterial(springColor);
 	      
 	      for (int i = 0; i < enemyNumber; i++)
 	    	  for (int j = 0; j < pumpkinNumber; j++)
 	    	  {
 		    	  pumpkin[i*pumpkinNumber+j].setMaterial(pumpkinFadeColor[i]);
 	    	  }
 	      
 	      
 	      for (int i = 0; i < enemyNumber; i++)
 	      {
 	    	  respawn(i, 1);
 	      }
 
 	   }
 
 //	   double waveDuration = 2.0; // DURATION OF ONE WAVE ANIMATION
 		double Y[]= new double[enemyNumber];
 		double swingX = 0;
 	      double swingY[] = new double[enemyNumber];
 	      double pop = 0;
 	      double dx[] = new double[enemyNumber]; //moving formula of x-coordinate 
 	      double dz[] = new double[enemyNumber]; //moving formula of z-coordinate 
 	      double enemyAngle[] = new double[enemyNumber];  // enemy's angle of attack
 	      double enemySpeed[] = new double[enemyNumber];
 //	      double runDu = 5;
 	      boolean isMiss[] = new boolean[enemyNumber];
 	      double c = 0;
 	      double gunTheta, gunPhi;
 //	      int gunInit = 0;
 	      
 	   public void animate(double time) {
 //		   System.out.println(turn);
 //		   gunColor.setAmbient(gunEnergy/100, 1, 1);
 		   if (reloadCount >= chargeRate){
 			   gunEnergy = Math.min(gunEnergy+1, 100);
 			   reloadCount = 0;
 		   }
 		   reloadCount = reloadCount + time - previousTime;
 		   previousTime = time;
 		   
 		   turningAngle += turn * Math.PI/10;
 		   if(score >= levelScore){
 			   	  
 				  getWorld().child = null;
 				  initialize();
 				  levelScore = levelScore + 20;
 				  bullet = 0;
 //				  totalBullet = totalBullet + 20;
 				  totalBullet = 100;
 				  gunEnergy = 100;
 				  level++;
 		   }
 		   //rotate camera
 		   m = getRenderer().getCamera();
 		   m.identity();
 		   m.rotateY(turningAngle);
 		   
 		   if(totalBullet <=0 && endGame == 0){
 			   getWorld().child = null;
 			   
 //		    	this.time = 0;
 			   endGame = 1;
 //			   initialize();
 		   }
 		   if (endGame == 1 && reStart == 1){
 			   enemyNumber = 3;
 		    	score = 0;
 		    	levelScore = 10;
 		    	miss = 0;
 		    	bullet = 0;
 		    	totalBullet = 100;
 		    	level = 1;
 		    	turningAngle = 0;
 			   reStart = 0;
 			   endGame = 0;
 			   
 //		    	time = 0;
 			   initialize();
 			   for (int i=0;i<enemyNumber;i++){
				   box[i][0].setVisible(false);
 				   box[i][1].setVisible(false);
 				   }
 		   }
 	      this.time = time;
 	      
 	      m = getWorld().getMatrix();
 	      m.identity();
 	      //set gun
 	      setGun(time);
 	      
 	      if(bullet==0){
 	    	 int i;
 	    	 
 	    	 //for(i=0; i<enemyNumber;i++)
 	    	//	 box[i][1].setVisible(false);
 	
 	      }
 	            
 	      for (int i=0;i<enemyNumber;i++)
 	    	  swingY[i] = Y[i]*Math.cos(3*(time - swingTime)+Y[i]); //this is how they swing their head;
 	    pop = 0.5;
 	    
 	    //setEnviornment
 	    m = line.getMatrix();
 	    m.identity();
 	    m.translate(0,-3, 0);
 	    m.rotateX(Math.PI/2);
 	    m.scale(10,10,5);
 	    m = wall.getMatrix();
 	    m.identity();
 	    m.translate(0, -3, 0);
 	    m.rotateX(Math.PI/2);
 	    m.scale(60,60,200);
 	    m = ground.getMatrix();
 	    m.identity();
 	    m.translate(0, -3.5, 0);
 	    m.scale(100,.2,100);
 	      for (int i=0;i<box.length;i++){ // here is how the enemies move
 //	    	  m.scale(2);
 	    	  
 	    	  
 	    	  
 	    	  if (box[i][0].isVisible) {
 	    		  // pumpkin movements
 	    		  
 	    		  double t = 2*(time-runTime[i])-i;
 	    		  
 	    		  //enemyAngle[i] = (i/enemyNumber)*2.*Math.PI; //Math.atan2(dz[i], dx[i]);
 	    		  
 		    	  //dx[i] = (i-enemyNumber/2)*8/(1+(time-runTime[i])/4); // set x; they are separated by there index and will closer to each other when they moving towards you 
 		    	  //dz[i] = -startDist+2.5*(2*(time-runTime[i])-i); //moving outward
 	    		  double dist = (pumpkinTime-t)/pumpkinTime*startDist;
 	    		  if (enemySpeed[i] != 0)
 	    		  {
 	    		  dx[i] = Math.cos(enemyAngle[i])*dist;
 	    		  dz[i] = Math.sin(enemyAngle[i])*dist;
 	    		  }
 	    		  
 		    	  m = box[i][0].getMatrix();
 		    	  m.identity();
 	
 		    	  if (t < Math.PI){
 			    	  // first big bounce (from below playfield)
 		    		  m.translate(dx[i], 3*Math.abs(Math.sin(t))-7+4*t/Math.PI, dz[i]);
 		    	  } else {
 		    		  m.translate(dx[i], -3+3*Math.abs(Math.sin(t)), dz[i]);
 		    	  }
 		    	  m.scale(.5);
 		    	  m.rotateY(Math.PI/2.+enemyAngle[i]);
 	    	  }
 	    	  
 	    	  else if (box[i][1].isVisible) {
 			      //ghost
 	    		  
 	    		  double t = time-runTime[i];
 	    		  //System.out.println("ghost!");
 	    		  double dist = (ghostTime-t)/ghostTime*startDist;
 	    		  double[] ghostFocus = new double [3];	// center of the ghost's motion
 	    		  Vec.set(ghostFocus, Math.cos(enemyAngle[i])*dist,
 	    				  0, Math.sin(enemyAngle[i])*dist);
 	    		  double swayScale = dist/6.0*Math.sin(t*2.);
 	    		  double[] ghostTrans = new double[3];	// side-to-side motion is along this vector
 	    		  Vec.set(ghostTrans, Math.cos(enemyAngle[i]+Math.PI/2.)*swayScale, 0,
 	    				  Math.sin(enemyAngle[i]+Math.PI/2.)*swayScale);
 //
 	    		  if (enemySpeed[i] != 0)
 	    		  {
 	    			  dx[i] = ghostFocus[0]+ghostTrans[0];
 	    		  	dz[i] = ghostFocus[2]+ghostTrans[2];
 	    		  }
 
 	    		  m = box[i][1].getMatrix();
 	    		  m.identity();
 	    		  m.translate(dx[i], 0, dz[i]);
 	    		  m.rotateX(Math.PI/2);
 	    		  m.rotateZ(Math.PI/2.+enemyAngle[i]);
 	    		  m.scale(1.2,1.1,1.5);
 	    		  m.scale(0.5);
 				  		  	
 				  	m = eye_r[i].getMatrix();
 				  	m.identity();
 				  	m.translate(0.35,0.8,-0.35);
 				  	m.scale(0.25,0.25,0.25);
 				  	
 				  	m = eye_l[i].getMatrix();
 				  	m.identity();
 				  	m.translate(-0.35,0.8,-0.35);
 				  	m.scale(0.25,0.25,0.25);
 				  	
 				  	m = shoulder_r[i].getMatrix();
 				  	m.identity();
 				  	m.translate(0.8, 0, -0.25);
 				  	
 				  	m = shoulder_l[i].getMatrix();
 				  	m.identity();
 				  	m.translate(-0.8, 0, -0.25);
 				  		  	
 				  	m = hand_r[i].getMatrix();
 				  	m.identity();
 				  	m.translate(0.4, 0.1, 0);
 			      	m.rotateY(Math.PI*8/12);
 			      	m.scale(0.3,0.25,0.60);
 			      	
 			    	m = hand_l[i].getMatrix();
 				  	m.identity();
 				  	m.translate(-0.4, 0.1, 0);
 			      	m.rotateY(-Math.PI*8/12);
 			      	m.scale(0.3,0.25,0.60);
 	    	  }
 	      }
 	      
 	      for (int i=0;i<enemyNumber;i++){
 	    	  if (dx[i]*dx[i]+dz[i]*dz[i]<(5*5)){ // if one cross the bar
	    		  if (isShoot[i] == false && isMiss[i] == false && box[i][0].isVisible == true && endGame == 0){
 	    			  miss++;
	    			  totalBullet = totalBullet-5;
 	    			  isMiss[i] = true;
 	    		  }
 	    		  enemySpeed[i] = 0;
 	    		  for (int j=0;j<pumpkinNumber;j++){
 		    		  pumpkin[i*pumpkinNumber+j].setVisible(false);
 		    		  spring[i*springNumber+j].setVisible(false);
 		    	  }
 		    	  stalk[i].setVisible(false);
 		    	  box[i][0].setVisible(false);
 		    	  box[i][1].setVisible(false);
 		    	  isShoot[i] = false;
 		    	  if (Math.random()<.08){ //they have 8% chance every frame to go back to the origin and appear again
 		    		  respawn(i, .7);
 		    	  }
 	    	  }
 	      }
 	      
 	      for (int i=0;i<enemyNumber;i++){
 		      m = spring[i*springNumber].getMatrix(); //the first torus of the spring
 		      m.identity();
 		      m.rotateX(-Math.PI/2);
 		      m.translate(0, 0, 2*pop);
 		      m.rotateY(.1*swingY[i]);
 		      m.scale(.3);
 	      }
 	      for (int i = 0;i <enemyNumber;i++){ // spring movement
 	    	  for (int j=1;j<springNumber;j++){
 	    	  
 	    	  m = spring[i*springNumber+j].getMatrix();
 		      m.identity();	     
 		      m.rotateX(.1*swingX);
 		      m.rotateY(.2*swingY[i]);
 		      m.translate(0, 0, pop+Math.random()*.1);
 
 	    	  }
 	      }
 	      for (int i = 0;i <enemyNumber;i++){ //make spheres to the pumpkin
 	    	  for (int j = 0;j < pumpkinNumber;j++){
 	    		  Geometry pg = pumpkin[i*pumpkinNumber+j];
 	    		  for (int c=0; c < pg.nChildren(); c++) {
 	    			  Matrix cm = pg.child(c).getMatrix();
 	    			  cm.identity();
 	    		  }
 			      m = pumpkin[i*pumpkinNumber+j].getMatrix();
 			      m.identity();
 			      m.translate(0, 0, 8*pop);
 			      m.rotateZ(i*pumpkinNumber+j*Math.PI/5);
 			      m.scale(8*pop,4*pop,6*pop);
 			      m.scale(2);
 			      
 	    	  }
 	      
 	    	  // normally opaque
 	    	  pumpkinFadeColor[i].setTransparency(0.);
 		  }
 	      
 	      for (int i=0;i<enemyNumber;i++){ // the stalk
 		      m = stalk[i].getMatrix();
 		      m.identity();
 		      m.translate(0, 0, 12*pop);
 		      m.scale(.45,.45,1);
 		      m.rotateX(Math.PI/10);
 		      m.scale(2.5);
 		      
 	      }
 	      //what if the pumpkin get shot
 	      for (int i=0;i<enemyNumber;i++){
 		      if (time-clickTime[i]<explodeTime && isShoot[i] == true){ //explode!
 		    	  double s = 1+(time - clickTime[i])/explodeTime*3;
 		    	  
 		    	  blowup.play();
 		    	  for (int j=0;j<pumpkinNumber;j++){
 		    		  Geometry pg = pumpkin[i*pumpkinNumber+j];
 		    		  
 		    		  for (int c=0; c < pg.nChildren(); c++) {
 		    			  Matrix cm = pg.child(c).getMatrix();
 		    			  double[] pv = pumpkin_vecs[c];
 		    			  
 		    			  cm.identity();
 		    			  // expand outward
 		    			  cm.translate((s-1)*pv[0], (s-1)*pv[1] , (s-1)*pv[2]);
 		    		  }
 		    	  }
 		    	  
 		    	  // fade out this pumpkin's bits
 		    	  pumpkinFadeColor[i].setTransparency((time - clickTime[i])/explodeTime);
 		    	  
 		    	  stalk[i].setVisible(false);		    	  
 		      }
 		      if (time-clickTime[i]>=explodeTime && isShoot[i] == true){ //then disappear
 		    	  for (int j=0;j<pumpkinNumber;j++){
 		    		  pumpkin[i*pumpkinNumber+j].setVisible(false);
 		    		  spring[i*springNumber+j].setVisible(false);
 		    	  }
 		    	  stalk[i].setVisible(false);
 		    	  box[i][0].setVisible(false);
 		    	  box[i][1].setVisible(false);
 		    	  isShoot[i] = false;
 		    	  if (Math.random()<.5){ //they have 50% chance to go back to the origin and appear again; or they have to wait until they cross the bar
 		    		  System.out.println("instant respawn "+i);
 		    		  respawn(i, .7);
 		    	  }
 		    	  else
 		    	  {
 		    		  System.out.println("defer respawn "+i);
 		    		  dx[i]=0;
 		    		  dz[i]=0;
 		    	  }
 		      }
 	      }
 
 	   }
 	   Color C = new Color(255,255,255,100); 
 
 	   public void drawOverlay(Graphics g) {
 	              g.setColor(Color.white);
 	              //draw the front sight
 	              g.drawOval(mouseX-50, mouseY-50, 100, 100);
 	              g.drawLine(mouseX-50, mouseY, mouseX+50, mouseY);
 	              g.drawLine(mouseX, mouseY-50, mouseX, mouseY+50);
 	              if(endGame != 1){
 	              g.setColor(C);
 	              g.fillOval((int)(150*1.0)-50+400+180, (int)(150*1.0)-110-20,100,100);
 	              g.setColor(Color.green);
 	              g.fillArc((int)(150*1.0)-50+400+180, (int)(150*1.0)-110-20, 100, 100, 135-(int)(turningAngle/Math.PI*360/2), -90);
 	              //g.setColor(Color.RED);
 	              for(int i=0; i<enemyNumber; i++){
 	                  if(box[i][0].isVisible == true){
 	                      g.setColor(Color.RED);
 	                      double tempDouble = (box[i][0].getMatrix().get(3, 0))*box[i][0].vertices[0][0] + 
 	                      (box[i][0].getMatrix().get(3, 1))*box[i][0].vertices[0][1] +
 	                      (box[i][0].getMatrix().get(3, 2))*box[i][0].vertices[0][2] +
 	                      (box[i][0].getMatrix().get(3, 3))*box[i][0].vertices[0][3];
 	                      
 	                      g.fillOval((int)(((((box[i][0].getMatrix().get(0, 0))*box[i][0].vertices[0][0] + 
 	                              (box[i][0].getMatrix().get(0, 1))*box[i][0].vertices[0][1] +
 	                              (box[i][0].getMatrix().get(0, 2))*box[i][0].vertices[0][2] +
 	                              (box[i][0].getMatrix().get(0, 3))*box[i][0].vertices[0][3])/tempDouble)*0.8 + 150)*1.0)+400+180, (int)(((((box[i][0].getMatrix().get(2, 0))*box[i][0].vertices[0][0] + 
 	                              (box[i][0].getMatrix().get(2, 1))*box[i][0].vertices[0][1] +
 	                              (box[i][0].getMatrix().get(2, 2))*box[i][0].vertices[0][2] +
 	                              (box[i][0].getMatrix().get(2, 3))*box[i][0].vertices[0][3])/tempDouble)*0.8 + 150)*1.0)-60-20, 5, 5);
 	                  } if(box[i][1].isVisible == true){
 	                      g.setColor(Color.blue);
 	                      
 	                      g.fillOval((int)(dx[i]*0.8+150+400+180),(int)(dz[i]*0.8+150-60-20),5,5);
 	                  }
 	                  
 	                  
 	              }
 	              g.setColor(Color.green);
 	              g.fillOval((int)(145*1.0)+400+180+2, (int)(150*1.0)-60-20-1, 5, 5);
 
 	              }
 	              
 	              if( endGame == 1 ){
 	                  g.setFont(bigFont);
 	                  g.setColor(Color.cyan);
 	                  g.fillRect(0, 0, 100, 50);
 	                  g.setColor(Color.black);
 	                  g.drawString("restart", 10, 30);
 	                  g.drawString("Game Over !", 210+100, 160+50);
 	                  g.drawString("Total Score: "+score, 200+100, 190+50);
 	                  g.drawString("Final  Level: "+level, 200+100, 220+50);
 	              }else{ 
 	              
 	            	  for (int i=0;i<totalBullet;i++){
 	   	           	   g.setColor(new Color(Math.max(0,(255-3*i)),Math.min(255,0+3*i),0));
 	   	           	   g.fillRect(50+3*i, 70, 3, 14);
 	   	              }
 	   	              for (int i=0;i<gunEnergy;i++){
 	   		           	   g.setColor(new Color(200,Math.min(255,50+3*i),0));
 	   		           	   g.fillRect(50+3*i, 100, 3, 14);
 	   		          }
 		              g.setColor(Color.white);
 		              g.drawRect(50, 70, 300, 14);
 		              g.drawRect(50, 100, 300, 14);
 		              g.drawString("score to next level: "+levelScore, 50, 30);
 		              g.drawString("Score: "+score, 50, 50);
 		              g.drawString("Energy: "+gunEnergy, 60, 112);
 		              g.drawString("Life: "+ totalBullet, 60, 82);
 		              g.drawString("level: "+ level, 50, 130);
 	              
 	              
 	              
 	              if(totalBullet >0 && totalBullet <= 15){
 	                  g.drawString("Warning!! ", 280+120, 30);
 	                  g.drawString("life is low!! ", 250+120, 50);
 	              }
 	              }
 	          }
 
 	   
 		
 	   public void setGun(double time){
 //		   	  gunTheta = -.3*Math.atan2(mouseX-800/2,50);
 //		      gunPhi = -.3*Math.atan2(mouseY-600/2, Math.sqrt(Math.pow((mouseX-800/2),2)+Math.pow(50, 2)));
 		   
 		      m = gun.getMatrix();
 		      m.identity();
 		      m.rotateY(-turningAngle);
 		      m.translate(0,-2, 3);
 //		      System.out.println(mouseX-W/2+" "+gunTheta+" "+W+" "+H);
 		      m.rotateY(gunTheta);
 		      m.rotateX(gunPhi);
 //		      m.scale(.1,.1,100);	      
 		      m = gunBody.getMatrix();
 		      m.identity();
 		      m.scale(.5, .6,2); 
 		      m = barrel.getMatrix();
 		      m.identity();
 		      m.translate(0, 0, -3);
 		      m.scale(.06,.06,1);
 		      m = gunHead.getMatrix();
 		      m.identity();
 		      m.translate(0, 0, -4);
 		      m.scale(.15);
 		      m = gunRing1.getMatrix();
 		      m.identity();
 		      m.translate(0, 0, -2);
 		      m.scale(.3,.3,1);
 		      m = gunRing2.getMatrix();
 		      m.identity();
 		      m.translate(0, 0, -2.5);
 		      m.scale(.3,.3,1);
 		      m = gunRing3.getMatrix();
 		      m.identity();
 		      m.translate(0, 0, -3);
 		      m.scale(.3,.3,1);
 		      m = gunWing.getMatrix();
 		      m.identity();
 		      m.translate(0, .2, 1);
 		      m.scale(.1,.6,.6);
 		      if (time-c>.5)
 		    	  c = time;
 		      gunRing1Color.setAmbient((time-c)*2, (time-c)*2, 0);
 		      gunRing2Color.setAmbient(0, (time-c)*2, 0);
 		      gunRing3Color.setAmbient(0, (time-c)*2, (time-c)*2);
 
 		    //set laser
 		      m = laser.getMatrix();
 		      m.identity();
 		      m.translate(0, 0, -50);
 		      m.scale(.07, .07, 50);
 		      if (time - shootTime<.2)
 		    	  laser.setVisible(true);
 		      else {
 		    	  laser.setVisible(false);
 			   	  gunTheta = -.2*Math.atan2(mouseX-800/2,80);
 			      gunPhi = -.2*Math.atan2(mouseY-600/2, Math.sqrt(Math.pow((mouseX-800/2),2)+Math.pow(80, 2)));
 		      }
 	   }
 
 	  void respawn(int i, double pumpkinProbability) {
 		  runTime[i] = time;
 		  //dx[i] = (i-enemyNumber/2)*8/(1+(time-runTime[i])/4);
 	 	  //dz[i] = -startDist+5*(time-runTime[i]);
 	 	  enemyAngle[i] = i*2.*Math.PI/enemyNumber; //Math.atan2(dz[i], dx[i]);
 	 	  dx[i] = Math.cos(enemyAngle[i])*startDist;
 	 	  dz[i] = Math.sin(enemyAngle[i])*startDist;
 		  for (int j=0;j<pumpkinNumber;j++){
 	 		  pumpkin[i*pumpkinNumber+j].setVisible(true);
 	 		  spring[i*springNumber+j].setVisible(true);
 	 	  }
 	 	  stalk[i].setVisible(true);
 	 	  if(Math.random()<pumpkinProbability){
 	 		  box[i][0].setVisible(true);
 	 		  box[i][1].setVisible(false);
 	 		  //System.out.println("pumpkin");
 	 		  enemySpeed[i] = 50/4;
 	 	  }else{
 	 		  box[i][0].setVisible(false);
 		    	  box[i][1].setVisible(true);
 		 		  //System.out.println("ghost");
 		      enemySpeed[i] = 50/4;
 	 	  }
 	 	 isShoot[i] = false;
 	 	 isMiss[i] = false;
 	 	 
 	 	 Y[i] = Math.random(); //get a new swing coefficient
 	 	 
 	 	 
 	 	 {
 	 		 // move offscreen so it doesn't show up yet this frame
 	 		 Matrix m = box[i][0].getMatrix();
 	 		 m.identity();
 	 		 m.translate(0,-1000,0);
 	 		 
 	 		 m = box[i][1].getMatrix();
 	 		 m.identity();
 	 		 m.translate(0,-1000,0);
 	 	 }
  	  }
 
 }
