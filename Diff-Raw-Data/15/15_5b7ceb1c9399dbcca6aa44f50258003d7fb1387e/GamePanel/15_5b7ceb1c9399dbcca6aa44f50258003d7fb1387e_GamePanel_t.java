 package com.example.pineapple;
 
 import java.util.ArrayList;
 
 import android.content.Context;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.graphics.Canvas;
 import android.graphics.Color;
 import android.graphics.Matrix;
 import android.graphics.Paint;
 import android.graphics.Paint.Style;
 import android.graphics.Path;
 import android.graphics.RectF;
 import android.util.Log;
 import android.view.MotionEvent;
 import android.view.SurfaceHolder;
 import android.view.SurfaceView;
 
 public class GamePanel extends SurfaceView implements SurfaceHolder.Callback{
 	private final String TAG = GamePanel.class.getSimpleName();
 	private final int INVALID_POINTER = -1;
 
 	
 	private int leftStickId = INVALID_POINTER;
 	private int rightStickId = INVALID_POINTER;
 	private final int width = 155;
 	private final int height = 100;
 	private double screenX;
 	private double screenY;
 	private final int screenXPadding = 50;
 	private final int screenYPadding = 20;
 	private MainThread thread;
 	private Protagonist protagonist;
 	private Ground ground;
 	private double scaleY, scaleX;
 	private Stick leftStick, rightStick;
 	private LevelLoader levelLoader;
 	private ArrayList<Platform> platforms;
 	private ArrayList<Bullet> bullets;
 	private ArrayList<Enemy> enemies;
 	private ArrayList<Integer> trees;
 	private ArrayList<int[]> rocks;
 	private int level;
 	private HeatMeter heatMeter;
 	private boolean firing = false;
 	private Paint green = new Paint();
 	private Paint red = new Paint();
 	private Paint frame = new Paint();
 	private double time;
 	private double bulletDamage = 0.05;
 	
 	//Special tutorial variables
 	private Protagonist mentor;
 	private int[] checkpoints;
 	private int currentCheckpoint;
 	private ArrayList<ArrayList<String>> hints;
 	private Paint textPaint;
 	private Bird bird;
 	private int timesMentorJumped, pastCheckpointBorder;
 	
 	//Ground rendering variables 
 	private int numberOfPatches, foliageSize = 2, groundThickness = 6;
 	private double xGap, yGap, gap, groundAngle; 
 	private Paint groundPaint = new Paint();
 	private Path groundPath, dirtPath;;
 	private Matrix renderMatrix = new Matrix();
 	
 	//Bitmaps
 	private Bitmap bodyBitmap;
 	private Bitmap footBitmap;
 	private Bitmap eyeMouthBitmap;
 	private Bitmap weaponBitmap;
 	private Bitmap pupilBitmap;
 	private Bitmap stickBitmap;
 	private Bitmap bulletBitmap;
 	private Bitmap birdBitmap;
 	private Bitmap treeBitmap;
 	private Bitmap mentorBodyBitmap;
 	private Bitmap eyeBeardBitmap;
 	private Bitmap[] rockBitmap;
 	
 	private Bitmap[] enemyBodyBitmap = new Bitmap[3];
 	private Bitmap[] enemyEyeMouthBitmap = new Bitmap[3];
 	private Bitmap[] enemyLeftArmBitmap = new Bitmap[3];
 	private Bitmap[] enemyRightArmBitmap = new Bitmap[3];
 	private Bitmap[] enemyFootBitmap = new Bitmap[3];
 	private Bitmap[] enemyPupilBitmap = new Bitmap[3];
 	
 	private Bitmap bodyBitmapFlipped;
 	private Bitmap footBitmapFlipped;
 	private Bitmap eyeMouthBitmapFlipped;
 	private Bitmap weaponBitmapFlipped;
 	private Bitmap pupilBitmapFlipped;
 	private Bitmap mentorBodyBitmapFlipped;
 	private Bitmap eyeBeardBitmapFlipped;
 	
 	private Bitmap[] enemyBodyBitmapFlipped = new Bitmap[3];
 	private Bitmap[] enemyEyeMouthBitmapFlipped = new Bitmap[3];
 	private Bitmap[] enemyFootBitmapFlipped = new Bitmap[3];
 
 	public GamePanel(Context context, int level){
 		super(context);
 		getHolder().addCallback(this);
 		setFocusable(true);
 		this.level = level;
 
 		//Create game components
 		levelLoader = new LevelLoader(level);
 		heatMeter = new HeatMeter(0.01);
 		bullets = new ArrayList<Bullet>();
 		ground = new Ground(levelLoader.getLevelX(level), levelLoader.getLevelY(level));
 		protagonist = new Protagonist(levelLoader.getStartX(), levelLoader.getStartY(), this);
 		leftStick = new Stick(Stick.LEFT);
 		rightStick = new Stick(Stick.RIGHT);
 		thread = new MainThread(this.getHolder(), this);
 
 		loadPlatforms();
 		loadEnemies();
 		loadTrees();
 		loadRocks();
 		
 		green.setColor(Color.GREEN);
 		red.setColor(Color.RED);
 		groundPaint.setColor(Color.rgb(10, 250, 10));
 		
 		if(level == 0){
 			mentor = new Protagonist(10, 0, this);
 			checkpoints = levelLoader.getCheckpoints();
 			hints = new ArrayList<ArrayList<String>>();
 			bird = new Bird(790, 100);
 			pastCheckpointBorder = 10;
 			String[] rawHints = {
 					"Hi there, welcome to the tutorial! Let's get right into the action! You can move around by using your left stick! Why don't you give it a go?",
 					"Good job! Believe it or not, but moving is essential to make it in this world. Come along!",
 					"You can jump by pressing up on the left stick!",
 					"Wow! You seem to have some strong legs there young one! Oh my, what a steep slope! We can slide down this for sure!",
 					"Woohoo!",
 					"That was fun! But let's get back to business!",
 					"Even though we are a peaceful people with no enemies what so ever, it is always good to carry some protection, like the gun in your hand for example. Fire off a couple of shots with your right stick!",
 					"Good, but shooting a gun isn't that exciting if you're not aiming at something, am I right? Let's find something to shoot!",
 					"Do you see that bird up there? They always eat my crops and sing early in the morning! See if you can scare him with your gun!",
 					"..... Well, now he won't wake me up early at least! We better go before the animal rights people show up. See if you can get up on this platform!",
 					"Wow, you can see so much from up here! Actually... I see something strange over there! What is that?",
 					"Good heavens, what an ugly creature! I know we are a friendly people but you better put him out of his misery! He doesn't look like a nice monster anyways...", 
 					"May he rest in peace! Now where were we? Oh right, there's one final thing you need to know! That weapon of yours, it gets easily overheated. Watch out for that if you feel like firing for a long time! Try it!",
 					"Well, that should be everything you need to know! I hereby name you... What is that noise? Run and look, will you?"
 			};
 			//Split the hints up into rows and add them to the final hint list
 			int lettersPerRow = 50;
 			for(int i = 0; i < rawHints.length; i++){
 				hints.add(new ArrayList<String>());
 				while(rawHints[i].length() > lettersPerRow){
 					String row = rawHints[i].substring(0, lettersPerRow-1);
 					int spaceIndex = row.lastIndexOf(" ");
 					hints.get(i).add(rawHints[i].substring(0, spaceIndex));
 					rawHints[i] = rawHints[i].substring(spaceIndex+1, rawHints[i].length());
 				}
 				hints.get(i).add(rawHints[i]);
 			}
 			
 			//Paint
 			textPaint = new Paint();
 			textPaint.setColor(Color.BLACK);
 			textPaint.setTextSize((float)(20));
 		}
 
 	}
 
 	//Load the platforms from LevelLoader and add them to the platforms list 
 	public void loadPlatforms(){
 		platforms = new ArrayList<Platform>();
 		for(int i = 0; i < levelLoader.getNumberOfPlatforms(); i++){
 			platforms.add(new Platform(levelLoader.getPlatformUpperX(i), levelLoader.getPlatformUpperY(i), levelLoader.getPlatformLowerX(i), levelLoader.getPlatformLowerY(i)));
 		}
 	}
 
 	public void loadEnemies(){
 		enemies = new ArrayList<Enemy>();
 		for(int i = 0; i < levelLoader.getNumberOfEnemies(); i++){
 			int[] enemyData = levelLoader.getEnemyData(i);
 			enemies.add(new Enemy(enemyData[0], enemyData[1], enemyData[2], enemyData[3], this));
 		}
 	}
 	
 	public void loadTrees(){
 		trees = levelLoader.getTrees();
 	}
 	
 	public void loadRocks(){
 		rocks = levelLoader.getRocks();
 	}
 
 	//Method that gets called every frame to update the games state
 	public void update(){
 		protagonist.checkSlope(ground, platforms);
 		handleSticks();
 		moveProtagonist();
 		moveEnemies();
 		moveBullets();
 		moveScreen();
 		handleHeatMeter();
 		handleBulletEnemyCollisions();
 		handleProtagonistEnemyCollisions();
 		this.time++;
 		
 		//Tutorial
 		if(level == 0){
 			moveMentor();
 			handleCheckpoints();
 			bird.update();
 			if(currentCheckpoint == 8){
 				screenY += (50-screenY)/20;
 			}
 			if(currentCheckpoint == 11){
 				screenY += (183-screenY)/20;
 				screenX += (1207-screenX)/20;
 			}
 			if(protagonist.getXPos() > checkpoints[currentCheckpoint] + pastCheckpointBorder && currentCheckpoint <= 12){
 				protagonist.setXPos(checkpoints[currentCheckpoint] + pastCheckpointBorder);
 			}
 		}
 	}
 
 	public void handleSticks(){
 		if(leftStick.isPointed()) {
 			protagonist.handleLeftStick(leftStick.getAngle(), 0.4);
 		} else if (Math.abs(protagonist.getXVel()) > 0){
 			protagonist.slowDown();
 			protagonist.setStepCount(0);
 		}
 		if(level > 0 || currentCheckpoint > 6){
 			if(rightStick.isPointed()){
 				//Aim
 				double angle = rightStick.getAngle();
 				protagonist.aim(angle);
 				//Fire
 				if(!heatMeter.isCoolingDown()){
 					bullets.add(new Bullet(protagonist.getXPos()+protagonist.getWidth()/2*Math.cos(angle/180*Math.PI), protagonist.getYPos()-protagonist.getWidth()/2*Math.sin(angle/180*Math.PI), angle, 10));
 					firing = true;
 				}
 			}
 		}
 	}
 
 	public void moveProtagonist(){
 		protagonist.gravity();
 		protagonist.move();
 		protagonist.faceDirection(leftStick, rightStick);
 		protagonist.breathe();
 		protagonist.invincibility();
 		protagonist.checkGround(ground);
 		protagonist.checkPlatform(platforms);
 		protagonist.dashing(ground, platforms);
 	}
 	
 	//Move all the enemies and check for obstacles etc
 	public void moveEnemies(){
 		for(int i = 0; i < enemies.size(); i++){
 			Enemy enemy = enemies.get(i);
 			if(enemy.hasSpawned()){
 				enemy.gravity();
 				enemy.accelerate(protagonist);
 				enemy.checkSlope(ground, platforms);
 				enemy.move();
 				enemy.checkGround(ground);
 				enemy.checkPlatform(platforms);
 				enemy.checkAirborne(ground, platforms);
 				enemy.waveArms();
 				enemy.lookAt(protagonist);
 			} else {
 				if(protagonist.getXPos() > enemy.getSpawnX()){
 					enemy.spawn();
 				}
 			}
 		}
 	}
 
 	public void moveBullets(){
 		for(int i = 0; i < bullets.size(); i++){
 			Bullet b = bullets.get(i);
 			b.gravity(1);
 			b.move();
 			int leftBound = ground.getX(0);
 			int rightBound = ground.getX(ground.getLength()-1);
 			if(b.getXPos() < leftBound || b.getXPos() > rightBound){
 				bullets.remove(i);
 				i--;
 			} else if(b.checkObstacles(ground, platforms)){
 				bullets.remove(i);
 				i--;
 			}
 
 		}
 	}
 
 	//Moves the screen if the protagonist is close to the edge of the screen
 	public void moveScreen(){
 		if(protagonist.getXPos() - screenX > width - screenXPadding){
 			screenX = protagonist.getXPos() - width + screenXPadding;
 		} else if(protagonist.getXPos() - screenX < screenXPadding){
 			screenX = protagonist.getXPos() - screenXPadding;
 		}
 		if(protagonist.getYPos() - screenY > height - screenYPadding){
 			screenY = protagonist.getYPos() - height + screenYPadding;
 		} else if(protagonist.getYPos() - screenY < screenYPadding){
 			screenY = protagonist.getYPos() - screenYPadding;
 		}
 
 	}
 
 	//Handles the HeatMeter 
 	public void handleHeatMeter(){
 		if(heatMeter.isCoolingDown()){//If the weapon is overheated
 			heatMeter.coolDown();     //Cool down                      (Not able to fire)
 		} else if(firing){			  //Else if the protagonist has fired
 			heatMeter.addHeat(0.01);  //Heat up the weapon
 			firing = false;
 		} else {                      //Otherwise cool down the weapon (Still able to fire)
 			heatMeter.coolDown();
 		}
 	}
 
 	//Check collision between enemies and bullets
 	public void handleBulletEnemyCollisions(){
 		for(int i = 0; i < bullets.size(); i++){//All bullets
 			Bullet bullet = bullets.get(i);
 			for(int j = 0; j < enemies.size(); j++){//All enemies
 				Enemy enemy = enemies.get(j);
 				if(bullet.collideEnemy(enemy) && enemy.hasSpawned()){//If collision detected
 					bullets.remove(i);//Remove the bullet from the game
 					i--;
 
 					enemy.takeDamage(bulletDamage*enemies.get(j).getDamageGrade()); //Reduce the enemies' health SET A CONSTANT OR SOMETHING HERE INSTEAD OF 0.05
 
 					if(enemy.getHealth() <= 0){//If the enemy is dead
 						enemies.remove(j);
 						Log.d(TAG, "Enemy down.");
 					}
 					break;
 				}
 			}
 		}
 	}
 
 	//Check collision between enemies and protagonist
 	public void handleProtagonistEnemyCollisions(){
 		for(int i = 0; i < enemies.size(); i++){
 			//Dashmove
 			//if(protagonist.collide(enemies.get(i)) && protagonist.isDashBonus()){
 			if(Math.abs(protagonist.getXPos() - enemies.get(i).getXPos()) < protagonist.getWidth()*3 && Math.abs(protagonist.getYPos() - enemies.get(i).getYPos()) < protagonist.getHeight()*1.5 && protagonist.isDashBonus() && enemies.get(i).isTouchingGround()){
 				enemies.get(i).takeDashDamage(protagonist);
 				if(enemies.get(i).getHealth() <= 0){//If the enemy is dead
 					enemies.remove(i);
 					Log.d(TAG, "Enemy down. Splash.");
 				}
 			}
 			if(protagonist.collide(enemies.get(i)) && !protagonist.isInvincible() && enemies.get(i).hasSpawned()){
 				protagonist.setInvincible(true);
 				protagonist.setXVel(-protagonist.getXVel());
 				protagonist.setYVel(-5);
 				protagonist.reduceHealth(0.05); //Change this constant
 				protagonist.setTouchingGround(false);
 			}
 		}
 	}
 	
 	public void moveMentor(){
 		mentor.gravity();
 		if(mentor.getXPos() < checkpoints[currentCheckpoint]){
 			if(mentor.getXPos() > 840 && timesMentorJumped == 0 || mentor.getXPos() > 1250 && timesMentorJumped == 1){
 				mentor.jump();
 				timesMentorJumped++;
 			}
 			mentor.accelerate(0.1);
 			mentor.step(1);
 			mentor.move();
 			mentor.faceDirection(1);
 		} else {
 			mentor.setStepCount(0);
 			mentor.faceDirection((int)(Math.signum(protagonist.getXPos()-mentor.getXPos())));
 		}
 		mentor.breathe();
 		mentor.checkGround(ground);
 		mentor.checkPlatform(platforms);
 	}
 
 	//A special method for the tutorial
 	public void handleCheckpoints() {
 		switch(currentCheckpoint){
 		case 0:
 			if(leftStick.isPointed() && (leftStick.getAngle() < 45 || leftStick.getAngle() > 315 || leftStick.getAngle() > 135 && leftStick.getAngle() < 225))
 				currentCheckpoint++;
 			break;
 		case 1:
 			if(protagonist.getXPos() > checkpoints[1]-width/4)
 				currentCheckpoint++;
 			break;
 		case 2:
 			if(!protagonist.isTouchingGround())
 				currentCheckpoint++;
 			break;
 		case 3: 
 			if(protagonist.getXPos() > 258)
 				currentCheckpoint++;
 			break;
 		case 4: 
 			if(protagonist.getXPos() > checkpoints[4])
 				currentCheckpoint++;
 			break;
 		case 5:
 			if(protagonist.getXPos() > checkpoints[5])
 				currentCheckpoint++;
 			break;
 		case 6: 
 			if(rightStick.isPointed())
 				currentCheckpoint++;
 			break;
 		case 7:
 			if(protagonist.getXPos() > checkpoints[7] - width/4)
 				currentCheckpoint++;
 			break;
 		case 8:
 			if(bird.collide(bullets)){
 				currentCheckpoint++;
 			}
 			break;
 		case 9:
 			if(protagonist.getXPos() > checkpoints[9] && protagonist.getYPos() < platforms.get(0).getUpperY()[0]){
 				currentCheckpoint++;
 			}
 			break;
 		case 10:
 			if(protagonist.getXPos() > checkpoints[10]){
 				currentCheckpoint++;
 			}
 		case 11:
 			if(enemies.size() == 0){
 				currentCheckpoint++;
 			}
 			break;
 		case 12:
 			if(heatMeter.isCoolingDown()){
 				currentCheckpoint++;
 			}
 			break;
 		}
 	}
 
 	//Method that gets called to render the graphics
 	public void render(Canvas canvas){
 		canvas.drawColor(Color.rgb(135, 206, 250)); //Sky
 		renderSun(canvas);
 		renderTrees(canvas);
 		renderRocks(canvas);
 		renderPlatforms(canvas);
 		renderEnemies(canvas);
 		if(level == 0){ //Tutorial
 			renderMentor(canvas);
 			renderBird(canvas);
 		}
 		renderProtagonist(canvas);
 		renderGround(canvas);
 		renderBullets(canvas);
 		renderSticks(canvas);
 		renderHeatMeter(canvas);
 		renderHealthMeter(canvas);
 
 		
 		//Tutorial 
 		if(level == 0){
 			renderHint(canvas);
 		}
 	}
 
 	//Draw the enemies
 	public void renderEnemies(Canvas canvas){
 		
 		for(int i = 0; i < enemies.size(); i++){
 			if(enemies.get(i).hasSpawned()){
 				Enemy e = enemies.get(i);
 				int feetAngle = (int)(Const.enemyMaxFootAngle*Math.sin(time/3));
 				if(e.getXVel() < 0){
 					
 					//Back arm
 					renderMatrix.setRotate(e.getLeftArmAngle(), enemyRightArmBitmap[e.getType()-1].getWidth(), (float)(enemyRightArmBitmap[e.getType()-1].getHeight()*0.9));
 					renderMatrix.postTranslate((float)((e.getXPos() + e.getWidth()*(0.5-Const.enemyArmXAxis) - e.getWidth()*Const.enemyArmRadius*Math.cos(-e.getLeftArmAngle()*Math.PI/180) - screenX)*scaleX)-enemyRightArmBitmap[e.getType()-1].getWidth(), (float)((e.getYPos() + e.getHeight()*(Const.enemyArmYAxis-0.5) + e.getHeight()*Const.enemyArmRadius*Math.sin(-e.getLeftArmAngle()*Math.PI/180) - screenY)*scaleY));
 					canvas.drawBitmap(enemyRightArmBitmap[e.getType()-1], renderMatrix, null);
 					//Back foot
 					renderMatrix.setRotate(-feetAngle, enemyFootBitmap[e.getType()-1].getWidth()/2, enemyFootBitmap[e.getType()-1].getHeight()/2);
 					renderMatrix.postTranslate((float)((e.getXPos() - e.getWidth()*(0.5-Const.enemyFootXAxis-Const.backFootOffset) - e.getWidth()*Const.enemyFootRadius*Math.sin(-feetAngle*Math.PI/180) - screenX)*scaleX), (float)((e.getYPos() + e.getHeight()*(Const.enemyFootYAxis-0.5) + e.getHeight()*Const.enemyFootRadius*Math.cos(-feetAngle*Math.PI/180) - screenY)*scaleY));
 					canvas.drawBitmap(enemyFootBitmap[e.getType()-1], renderMatrix, null);
 					//Body
 					renderMatrix.setTranslate((float)((e.getXPos()-e.getWidth()*(0.5-Const.enemyBodyXOffset)-screenX)*scaleX), (float)((e.getYPos()-e.getHeight()*(0.5-Const.enemyBodyYOffset)-screenY)*scaleY));
 					canvas.drawBitmap(enemyBodyBitmap[e.getType()-1], renderMatrix, null);
 					//Eyes and mouth
 					renderMatrix.setTranslate((float)((e.getXPos()-e.getWidth()*(0.5-Const.enemyEyeMouthXOffset)-screenX)*scaleX), (float)((e.getYPos()-e.getHeight()*(0.5-Const.enemyEyeMouthYOffset)-screenY)*scaleY));
 					canvas.drawBitmap(enemyEyeMouthBitmap[e.getType()-1], renderMatrix, null);
 					//Front arm
 					renderMatrix.setRotate(e.getRightArmAngle(), 0, (float)(enemyLeftArmBitmap[e.getType()-1].getHeight()*0.9));
 					renderMatrix.postTranslate((float)((e.getXPos() - e.getWidth()*(0.5-Const.enemyArmXAxis) + e.getWidth()*Const.enemyArmRadius*Math.cos(e.getRightArmAngle()*Math.PI/180) - screenX)*scaleX), (float)((e.getYPos() + e.getHeight()*(Const.enemyArmYAxis-0.5) + e.getHeight()*Const.enemyArmRadius*Math.sin(e.getRightArmAngle()*Math.PI/180) - screenY)*scaleY));
 					canvas.drawBitmap(enemyLeftArmBitmap[e.getType()-1], renderMatrix, null);
 					//Front foot
 					renderMatrix.setRotate(feetAngle, enemyFootBitmap[e.getType()-1].getWidth()/2, enemyFootBitmap[e.getType()-1].getHeight()/2);
 					renderMatrix.postTranslate((float)((e.getXPos() - e.getWidth()*(0.5-Const.enemyFootXAxis-Const.backFootOffset) - e.getWidth()*Const.enemyFootRadius*Math.sin(feetAngle*Math.PI/180) - screenX)*scaleX), (float)((e.getYPos() + e.getHeight()*(Const.enemyFootYAxis-0.5) + e.getHeight()*Const.enemyFootRadius*Math.cos(feetAngle*Math.PI/180) - screenY)*scaleY));
 					canvas.drawBitmap(enemyFootBitmap[e.getType()-1], renderMatrix, null);
 					//Pupils
 					renderMatrix.setTranslate((float)((e.getXPos() + e.getWidth()*(Const.enemyPupilXOffset-0.5)+e.getWidth()*Const.enemyPupilRadius*Math.cos(e.getPupilAngle()*Math.PI/180)-screenX)*scaleX), (float)((e.getYPos() + e.getHeight()*(Const.enemyPupilYOffset-0.5)+e.getHeight()*Const.enemyPupilRadius*Math.sin(e.getPupilAngle()*Math.PI/180) - screenY)*scaleY));
 					canvas.drawBitmap(enemyPupilBitmap[e.getType()-1], renderMatrix, null);
 				} else {
 					//Back arm
 					renderMatrix.setRotate(e.getRightArmAngle(), 0, (float)(enemyLeftArmBitmap[e.getType()-1].getHeight()*0.9));
 					renderMatrix.postTranslate((float)((e.getXPos() - e.getWidth()*(0.5-Const.enemyArmXAxis) + e.getWidth()*Const.enemyArmRadius*Math.cos(e.getRightArmAngle()*Math.PI/180) - screenX)*scaleX), (float)((e.getYPos() + e.getHeight()*(Const.enemyArmYAxis-0.5) + e.getHeight()*Const.enemyArmRadius*Math.sin(e.getRightArmAngle()*Math.PI/180) - screenY)*scaleY));
 					canvas.drawBitmap(enemyLeftArmBitmap[e.getType()-1], renderMatrix, null);
 					//Back foot
 					renderMatrix.setRotate(-feetAngle, enemyFootBitmap[e.getType()-1].getWidth()/2, enemyFootBitmap[e.getType()-1].getHeight()/2);
 					renderMatrix.postTranslate((float)((e.getXPos() + e.getWidth()*(0.5-Const.enemyFootXAxis-Const.backFootOffset) - e.getWidth()*Const.enemyFootRadius*Math.sin(-feetAngle*Math.PI/180) - screenX)*scaleX) - enemyFootBitmap[e.getType()-1].getWidth(), (float)((e.getYPos() + e.getHeight()*(Const.enemyFootYAxis-0.5) + e.getHeight()*Const.enemyFootRadius*Math.cos(feetAngle*Math.PI/180) - screenY)*scaleY));
 					canvas.drawBitmap(enemyFootBitmap[e.getType()-1], renderMatrix, null);
 					//Body
 					renderMatrix.setTranslate((float)((e.getXPos()-e.getWidth()*(0.5-Const.enemyBodyXOffset)-screenX)*scaleX), (float)((e.getYPos()-e.getHeight()*(0.5-Const.enemyBodyYOffset)-screenY)*scaleY));
 					canvas.drawBitmap(enemyBodyBitmapFlipped[e.getType()-1], renderMatrix, null);
 					//Eyes and mouth
 					renderMatrix.setTranslate((float)((e.getXPos()+e.getWidth()*(0.5-Const.enemyEyeMouthXOffset)-screenX)*scaleX)-enemyEyeMouthBitmap[e.getType()-1].getWidth(), (float)((e.getYPos()-e.getHeight()*(0.5-Const.enemyEyeMouthYOffset)-screenY)*scaleY));
 					canvas.drawBitmap(enemyEyeMouthBitmapFlipped[e.getType()-1], renderMatrix, null);
 					//Front arm
 					renderMatrix.setRotate(e.getLeftArmAngle(), enemyRightArmBitmap[e.getType()-1].getWidth(), (float)(enemyRightArmBitmap[e.getType()-1].getHeight()*0.9));
 					renderMatrix.postTranslate((float)((e.getXPos() + e.getWidth()*(0.5-Const.enemyArmXAxis) - e.getWidth()*Const.enemyArmRadius*Math.cos(-e.getLeftArmAngle()*Math.PI/180) - screenX)*scaleX)-enemyRightArmBitmap[e.getType()-1].getWidth(), (float)((e.getYPos() + e.getHeight()*(Const.enemyArmYAxis-0.5) + e.getHeight()*Const.enemyArmRadius*Math.sin(-e.getLeftArmAngle()*Math.PI/180) - screenY)*scaleY));
 					canvas.drawBitmap(enemyRightArmBitmap[e.getType()-1], renderMatrix, null);
 					//Front foot
 					renderMatrix.setRotate(feetAngle, enemyFootBitmap[e.getType()-1].getWidth()/2, enemyFootBitmap[e.getType()-1].getHeight()/2);
 					renderMatrix.postTranslate((float)((e.getXPos() + e.getWidth()*(0.5-Const.enemyFootXAxis-Const.backFootOffset) - e.getWidth()*Const.enemyFootRadius*Math.sin(feetAngle*Math.PI/180) - screenX)*scaleX) - enemyFootBitmap[e.getType()-1].getWidth(), (float)((e.getYPos() + e.getHeight()*(Const.enemyFootYAxis-0.5) + e.getHeight()*Const.enemyFootRadius*Math.cos(-feetAngle*Math.PI/180) - screenY)*scaleY));
 					canvas.drawBitmap(enemyFootBitmapFlipped[e.getType()-1], renderMatrix, null);
 					//Pupils
 					renderMatrix.setTranslate((float)((e.getXPos() - e.getWidth()*(Const.enemyPupilXOffset-0.5)+e.getWidth()*Const.enemyPupilRadius*Math.cos(e.getPupilAngle()*Math.PI/180)-screenX)*scaleX)-enemyPupilBitmap[e.getType()-1].getWidth(), (float)((e.getYPos() + e.getHeight()*(Const.enemyPupilYOffset-0.5)+e.getHeight()*Const.enemyPupilRadius*Math.sin(e.getPupilAngle()*Math.PI/180) - screenY)*scaleY));
 					canvas.drawBitmap(enemyPupilBitmap[e.getType()-1], renderMatrix, null);
 				}
 
 				switch(e.getType()){
 				case 1:
 					
 					break;
 				case 2:
 					
 					break;
 				case 3:
 					
 					break;
 				}
 				
 			}
 		}
 	}
 
 	//Draw the protagonist
 	public void renderProtagonist(Canvas canvas){
 		/*Paint p = new Paint();
 		p.setColor(Color.BLUE);
 		canvas.drawRect((float)((protagonist.getXPos()-protagonist.getWidth()/2-screenX)*scaleX), (float)((protagonist.getYPos()-protagonist.getHeight()/2)*scaleY), (float)((protagonist.getXPos()+protagonist.getWidth()/2-screenX)*scaleX), (float)((protagonist.getYPos()+protagonist.getHeight()/2)*scaleY), p);*/
 		float aimAngle = (float)rightStick.getAngle();
 		float feetAngle;
 		if(protagonist.isTouchingGround()){
 			feetAngle = (float)(180/Math.PI*Math.sin((double)protagonist.getStepCount()/protagonist.getNumberOfSteps()*Math.PI));
 		} else {
 			feetAngle = Const.jumpFeetAngle;
 		}
 		//Draw all the protagonist parts
 		;
 		if(protagonist.isFacingRight()){
 			//Draw back foot
 			renderMatrix.setRotate(-feetAngle, footBitmap.getWidth()/2, footBitmap.getHeight()/2);
 			renderMatrix.postTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.footXAxis-Const.backFootOffset) - protagonist.getWidth()*Const.footRadius*Math.sin(-feetAngle*Math.PI/180) - screenX)*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.footYAxis-0.5) + protagonist.getHeight()*Const.footRadius*Math.cos(-feetAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(footBitmap, renderMatrix, null);
 			//Draw body
 			renderMatrix.setTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.bodyXOffset) - screenX)*scaleX), (float)((protagonist.getYPos() - protagonist.getHeight()*(0.5-Const.bodyYOffset + Const.breathOffset*Math.sin((double)protagonist.getBreathCount()/protagonist.getBreathMax()*2*Math.PI)) - screenY)*scaleY));
 			canvas.drawBitmap(bodyBitmap, renderMatrix, null);
 			//Draw eyes and mouth
 			renderMatrix.setTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.eyeMouthXOffset) - screenX)*scaleX), (float)((protagonist.getYPos() - protagonist.getHeight()*(0.5-Const.eyeMouthYOffset) - screenY)*scaleY));
 			canvas.drawBitmap(eyeMouthBitmap, renderMatrix, null);
 			//Draw front foot
 			renderMatrix.setRotate(feetAngle, footBitmap.getWidth()/2, footBitmap.getHeight()/2);
 			renderMatrix.postTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.footXAxis) - protagonist.getWidth()*Const.footRadius*Math.sin(feetAngle*Math.PI/180) - screenX)*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.footYAxis-0.5) + protagonist.getHeight()*Const.footRadius*Math.cos(feetAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(footBitmap, renderMatrix, null);
 			//Draw pupils
 			renderMatrix.setTranslate((float)((protagonist.getXPos() + protagonist.getWidth()*(Const.pupilXOffset-0.5)+protagonist.getWidth()*Const.pupilRadius*Math.cos(aimAngle*Math.PI/180)-screenX)*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.pupilYOffset-0.5)-protagonist.getHeight()*Const.pupilRadius*Math.sin(aimAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(pupilBitmap, renderMatrix, null);
 			//Draw weapon
 			renderMatrix.setRotate(-aimAngle, weaponBitmap.getWidth()/2, weaponBitmap.getHeight()/2);
 			renderMatrix.postTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.weaponXAxis) + protagonist.getWidth()*Const.weaponRadius*Math.cos(aimAngle*Math.PI/180) - screenX)*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.weaponYAxis-0.5) - protagonist.getHeight()*Const.weaponRadius*Math.sin(aimAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(weaponBitmap, renderMatrix, null);
 		} else {
 			//Draw back foot
 			renderMatrix.setRotate(feetAngle, footBitmapFlipped.getWidth()/2, footBitmapFlipped.getHeight()/2);
 			renderMatrix.postTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.footXAxis+Const.backFootOffset) - protagonist.getWidth()*Const.footRadius*Math.sin(Math.PI-feetAngle*Math.PI/180) - screenX)*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.footYAxis-0.5) + protagonist.getHeight()*Const.footRadius*Math.cos(-feetAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(footBitmapFlipped, renderMatrix, null);
 			//Draw body
 			renderMatrix.setTranslate((float)((protagonist.getXPos() + protagonist.getWidth()*(0.5-Const.bodyXOffset) - screenX)*scaleX) - bodyBitmapFlipped.getWidth(), (float)((protagonist.getYPos() - protagonist.getHeight()*(0.5-Const.bodyYOffset + Const.breathOffset*Math.sin((double)protagonist.getBreathCount()/protagonist.getBreathMax()*2*Math.PI)) - screenY)*scaleY));
 			canvas.drawBitmap(bodyBitmapFlipped, renderMatrix, null);
 			//Draw eyes and mouth
 			renderMatrix.setTranslate((float)((protagonist.getXPos() + protagonist.getWidth()*(0.5-Const.eyeMouthXOffset) - screenX)*scaleX) - eyeMouthBitmapFlipped.getWidth(), (float)((protagonist.getYPos() - protagonist.getHeight()*(0.5-Const.eyeMouthYOffset) - screenY)*scaleY));
 			canvas.drawBitmap(eyeMouthBitmapFlipped, renderMatrix, null);
 			//Draw front foot
 			renderMatrix.setRotate(-feetAngle, footBitmapFlipped.getWidth()/2, footBitmapFlipped.getHeight()/2);
 			renderMatrix.postTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.footXAxis) - protagonist.getWidth()*Const.footRadius*Math.sin(Math.PI+feetAngle*Math.PI/180) - screenX)*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.footYAxis-0.5) + protagonist.getHeight()*Const.footRadius*Math.cos(feetAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(footBitmapFlipped, renderMatrix, null);
 			//Draw pupils
 			renderMatrix.setTranslate((float)((protagonist.getXPos() + protagonist.getWidth()*(Const.pupilXOffset-0.5)+protagonist.getWidth()*Const.pupilRadius*Math.cos(aimAngle*Math.PI/180)-screenX)*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.pupilYOffset-0.5)-protagonist.getHeight()*Const.pupilRadius*Math.sin(aimAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(pupilBitmapFlipped, renderMatrix, null);
 			//Draw weapon
 			renderMatrix.setRotate(180-aimAngle, weaponBitmapFlipped.getWidth()/2, weaponBitmapFlipped.getHeight()/2);
 			renderMatrix.postTranslate((float)((protagonist.getXPos()  + protagonist.getWidth()*(0.5-Const.weaponXAxis) + protagonist.getWidth()*Const.weaponRadius*Math.cos(aimAngle*Math.PI/180) - screenX)*scaleX - weaponBitmapFlipped.getWidth()), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.weaponYAxis-0.5) + protagonist.getHeight()*Const.weaponRadius*Math.sin(Math.PI+aimAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(weaponBitmapFlipped, renderMatrix, null);
 		}
 
 
 	}
 	
 	
 
 	//Draws the ground using a Path
 	//Draw only the parts which are visible on the screen
 	public void renderGround(Canvas canvas){
 		int i = 0;
 		
 		//Find the interval of the ground that has to be rendered
 		while(ground.getX(i+1) < screenX){
 			i++;
 		}
 		int startIndex = i;
 		int lowestPoint = ground.getY(i);
 		while(ground.getX(i) < screenX + width && i < ground.getLength()-2){
 			i++;
 			lowestPoint = Math.max(lowestPoint, ground.getY(i));
 		}
 		
 		int stopIndex = i;
 		Paint dirtPaint = new Paint();
 		dirtPaint.setColor(Color.rgb(87, 59, 12));
 		for(i = startIndex; i <= stopIndex; i++){
 			
 			groundPath = new Path();
 			groundPath.moveTo((int)((ground.getX(i)-screenX)*scaleX), (int)((ground.getY(i)-screenY)*scaleY));
 			groundPath.lineTo((int)((ground.getX(i+1)-screenX)*scaleX), (int)((ground.getY(i+1)-screenY)*scaleY));
 			groundPath.lineTo((int)((ground.getX(i+1)-screenX)*scaleX), (int)((ground.getY(i+1)+groundThickness-screenY)*scaleY)); 
 			groundPath.lineTo((int)((ground.getX(i)-screenX)*scaleX), (int)((ground.getY(i)+groundThickness-screenY)*scaleY));
 			groundPath.lineTo((int)((ground.getX(i)-screenX)*scaleX), (int)((ground.getY(i)-screenY)*scaleY));
 			dirtPath = new Path();
 			dirtPath.moveTo((int)((ground.getX(i)-screenX)*scaleX), (int)((ground.getY(i)+groundThickness-screenY)*scaleY));
 			dirtPath.lineTo((int)((ground.getX(i+1)-screenX)*scaleX), (int)((ground.getY(i+1)+groundThickness-screenY)*scaleY));
 			dirtPath.lineTo((int)((ground.getX(i+1)-screenX)*scaleX), (int)((lowestPoint+height-screenY)*scaleY));
 			dirtPath.lineTo((int)((ground.getX(i)-screenX)*scaleX), (int)((lowestPoint+height-screenY)*scaleY));
 			dirtPath.lineTo((int)((ground.getX(i)-screenX)*scaleX), (int)((ground.getY(i)+groundThickness-screenY)*scaleY));
 			canvas.drawPath(groundPath, groundPaint);
 			canvas.drawPath(dirtPath, dirtPaint);			
 		}
 		//Experiment (Different ground details)
 		//Balls
 		
 		/*for(i = startIndex; i <= stopIndex; i++){
 			xGap = (ground.getX(i+1)-ground.getX(i));
 			yGap = (ground.getY(i+1)-ground.getY(i));
 			gap = Math.sqrt(Math.pow(xGap, 2) + Math.pow(yGap, 2));
 			numberOfPatches = (int)(gap/foliageSize/2+2);
 			for(int j = 0; j < numberOfPatches; j++){
 				canvas.drawOval(new RectF((float)((ground.getX(i)+xGap*j/numberOfPatches - foliageSize - screenX)*scaleX), (float)((ground.getY(i)+yGap*j/numberOfPatches-foliageSize - screenY)*scaleY), (float)((ground.getX(i)+xGap*j/numberOfPatches+foliageSize - screenX)*scaleX), (float)((ground.getY(i)+yGap*j/numberOfPatches+foliageSize - screenY)*scaleY)), groundPaint);
 			}
 		}*/
 		
 		//Spikes
 		groundPath = new Path();
 		for(i = startIndex; i <= stopIndex; i++){
 			xGap = (ground.getX(i+1)-ground.getX(i));
 			yGap = (ground.getY(i+1)-ground.getY(i));
 			gap = Math.sqrt(Math.pow(xGap, 2) + Math.pow(yGap, 2));
 			numberOfPatches = (int)(gap/foliageSize/2+2);
 			groundAngle = Math.atan(ground.getSlope((ground.getX(i)+ground.getX(i+1))/2));
 			groundPath.moveTo((float)((ground.getX(i)-screenX)*scaleX), (float)((ground.getY(i)-screenY)*scaleY));
 			for(int j = 0; j < numberOfPatches; j++){
 				groundPath.lineTo((float)((ground.getX(i)+xGap*(j+0.5)/numberOfPatches+foliageSize*Math.sin(groundAngle)-screenX)*scaleX), (float)((ground.getY(i)+yGap/numberOfPatches*(j+0.5)-foliageSize*Math.cos(groundAngle)-screenY)*scaleY));
 				groundPath.lineTo((float)((ground.getX(i)+xGap*(j+1)/numberOfPatches - screenX)*scaleX), (float)((ground.getY(i)+yGap/numberOfPatches*(j+1)-screenY)*scaleY));
 			}
 			groundPath.lineTo((float)((ground.getX(i)-screenX)*scaleX), (float)((ground.getY(i)-screenY)*scaleY));
 			canvas.drawPath(groundPath, groundPaint);
 		}
 		
 	}
 
 	//Draw the platforms
 	public void renderPlatforms(Canvas canvas){
 		for(int i = 0; i < platforms.size(); i++){
 			if(platforms.get(i).getUpperX()[0] < screenX+width && platforms.get(i).getUpperX()[platforms.get(i).getUpperX().length-1] > screenX){
 				Path path = platforms.get(i).getPath();
 				Path newPath = new Path(path); 
 				renderMatrix.setTranslate(-(float)screenX, -(float)screenY);
 				renderMatrix.postScale((float)scaleX, (float)scaleY);
 				newPath.transform(renderMatrix);
 				canvas.drawPath(newPath, groundPaint);
 				//Draw platform details
 				//Spikes on top
 				groundPath = new Path();
 				for(int k = 0; k < platforms.get(i).getUpperX().length-1; k++){
 					xGap = (platforms.get(i).getUpperX()[k+1]-platforms.get(i).getUpperX()[k]);
 					yGap = (platforms.get(i).getUpperY()[k+1]-platforms.get(i).getUpperY()[k]);
 					gap = Math.sqrt(Math.pow(xGap, 2) + Math.pow(yGap, 2));
 					numberOfPatches = (int)(gap/foliageSize/2+2);
 					groundAngle = Math.atan(platforms.get(i).getSlope((platforms.get(i).getUpperX()[k]+platforms.get(i).getUpperX()[k+1])/2));
 					groundPath.moveTo((float)((platforms.get(i).getUpperX()[k]-screenX)*scaleX), (float)((platforms.get(i).getUpperY()[k]-screenY)*scaleY));
 					for(int j = 0; j < numberOfPatches; j++){
 						groundPath.lineTo((float)((platforms.get(i).getUpperX()[k]+xGap*(j+0.5)/numberOfPatches+foliageSize*Math.sin(groundAngle)-screenX)*scaleX), (float)((platforms.get(i).getUpperY()[k]+yGap/numberOfPatches*(j+0.5)-foliageSize*Math.cos(groundAngle)-screenY)*scaleY));
 						groundPath.lineTo((float)((platforms.get(i).getUpperX()[k]+xGap*(j+1)/numberOfPatches - screenX)*scaleX), (float)((platforms.get(i).getUpperY()[k]+yGap/numberOfPatches*(j+1)-screenY)*scaleY));
 					}
 					groundPath.lineTo((float)((platforms.get(i).getUpperX()[k]-screenX)*scaleX), (float)((platforms.get(i).getUpperY()[k]-screenY)*scaleY));
 					canvas.drawPath(groundPath, groundPaint);
 				}
 			}
 		}
 	}
 
 	//Draw the sticks
 	public void renderSticks(Canvas canvas){
 		canvas.drawBitmap(stickBitmap, (float)((leftStick.getX()-leftStick.getRadius())*scaleX), (float)((leftStick.getY()-leftStick.getRadius())*scaleY), null);
 		canvas.drawBitmap(stickBitmap, (float)((rightStick.getX()-rightStick.getRadius())*scaleX), (float)((rightStick.getY()-rightStick.getRadius())*scaleY), null);
 	}
 
 	//Draw the bullets
 	public void renderBullets(Canvas canvas){
 		int radius = Bullet.getRadius();
 		for(int i = 0; i < bullets.size(); i++){
 			Bullet b = bullets.get(i);
 			renderMatrix.setRotate((float)(180/Math.PI*Math.atan2(b.getYVel(), b.getXVel())), (float)(bulletBitmap.getWidth()/2), (float)(bulletBitmap.getHeight()/2));
 			renderMatrix.postTranslate((float)((b.getXPos()-radius/2.-screenX)*scaleX), (float)((b.getYPos()-radius/2.-screenY)*scaleY));
 			canvas.drawBitmap(bulletBitmap, renderMatrix, null);
 		}
 	}
 
 	//Draw the HeatMeter
 	public void renderHeatMeter(Canvas canvas){
 		int xPadding = 10;
 		int yPadding = 10;
 		int width = 20;
 		int height = 10;
 		int frameSize = 1;
 		double blinkInterval = 0.05; //Lower value means faster blinking (Should be at least coolingRate of HeatMeter)
 
 		//This makes the frame blink red if overheated
 		if(heatMeter.isCoolingDown() && heatMeter.getHeat() % (2*blinkInterval) > blinkInterval){
 			frame.setColor(Color.RED);
 		} else {
 			frame.setColor(Color.BLACK);
 		}
 
 		//Draw in top right corner
 		//Draw frame
 		canvas.drawRect((float)((this.width - width - xPadding - frameSize)*scaleX), (float)((yPadding-frameSize)*scaleY), (float)((this.width - xPadding + frameSize)*scaleX), (float)((yPadding+height+frameSize)*scaleY), frame);
 		//Draw green background
 		canvas.drawRect((float)((this.width - width - xPadding)*scaleX), (float)(yPadding*scaleY), (float)((this.width - xPadding)*scaleX), (float)((yPadding+height)*scaleY), green);
 		//Draw red indicator that moves with current heat level
 		canvas.drawRect((float)((this.width - width - xPadding)*scaleX), (float)(yPadding*scaleY), (float)((this.width - xPadding - width*(1-heatMeter.getHeat()))*scaleX), (float)((yPadding+height)*scaleY), red);
 	}
 
 	public void renderHealthMeter(Canvas canvas){
 		int xPadding = 10;
 		int yPadding = 10;
 		int width = 20;
 		int height = 10;
 		int frameSize = 1;
 
 		//Draw in top left corner
 		//Draw frame
 		canvas.drawRect((float)((xPadding-frameSize)*scaleX), (float)((yPadding-frameSize)*scaleY), (float)((xPadding+width+frameSize)*scaleX), (float)((yPadding+height+frameSize)*scaleY), new Paint());
 		//Draw green background
 		canvas.drawRect((float)(xPadding*scaleX), (float)(yPadding*scaleY), (float)((xPadding+width)*scaleX), (float)((yPadding+height)*scaleY), red);
 		//Draw red indicator that moves with current heat level
 		canvas.drawRect((float)(xPadding*scaleX), (float)(yPadding*scaleY), (float)((xPadding+width*protagonist.getHealth())*scaleX), (float)((yPadding+height)*scaleY), green);
 	}
 
 	//Draw the sun, moving in time
 	public void renderSun(Canvas canvas){
 		float x = (float)(width*scaleX/3);//Make generalll
 		float y = (float)((50 + 50*Math.sin(Math.PI + time/500))*scaleY);//Make generalll
 		float radius = (float)(width/20*scaleX);
 		Paint p = new Paint();
 		p.setColor(Color.YELLOW);
 
 		canvas.drawCircle(x, y, radius, p);
 	}
 
 	//Draw trees
 	public void renderTrees(Canvas canvas){
 		//x is centre of tree?
 		/*Paint trunk = new Paint();
 		trunk.setColor(Color.DKGRAY);
 		Paint top = new Paint();
 		top.setColor(Color.GREEN);
 		Paint border = new Paint();
 		border.setStyle(Style.STROKE);
 		for(int i = 0; i < trees.size(); i++){
 			float y = (float)(100); //Make generalll
 			float trunkHeight = (float)(height/4);//Make generalll
 			float trunkWidth = (float)(5);//Make generalll
 			float radius = 20;
 			canvas.drawCircle((float)((trees.get(i) - screenX/4)*scaleX), (float)((y - trunkHeight - radius/2)*scaleY), (float)(radius*scaleX), top);
 			canvas.drawCircle((float)((trees.get(i) - screenX/4)*scaleX), (float)((y - trunkHeight - radius/2)*scaleY), (float)(radius*scaleX), border);
 			canvas.drawRect((float)((trees.get(i) - trunkWidth/2 - screenX/4)*scaleX), (float)((y - trunkHeight)*scaleY), (float)((trees.get(i) + trunkWidth/2 - (float)(screenX/4))*scaleX), (float)(y*scaleY), trunk);
 		}
 		*/
 		for(int i = 0; i < trees.size(); i++){
 			canvas.drawBitmap(treeBitmap, (float)((trees.get(i) - screenX)*scaleX)-treeBitmap.getWidth()/2, (float)((ground.getYFromX(trees.get(i))-screenY)*scaleY-treeBitmap.getHeight()*0.8), null);
 		}
 		
 	}
 	
 	//Draw rocks
 	public void renderRocks(Canvas canvas){
 		for(int i = 0; i < rocks.size(); i++){
 			groundAngle = (Math.atan(ground.getSlope(rocks.get(i)[0])));
 			renderMatrix.setScale((float)(rocks.get(i)[2]/Const.maxRockSize), (float)(rocks.get(i)[2]/Const.maxRockSize));
 			renderMatrix.postRotate((float)(groundAngle*180/Math.PI), (float)(rocks.get(i)[2]/2*scaleX), (float)(rocks.get(i)[2]/2*scaleY));
 			renderMatrix.postTranslate((float)((rocks.get(i)[0]-screenX-rocks.get(i)[2]/2)*scaleX), (float)((ground.getYFromX(rocks.get(i)[0])-screenY-rocks.get(i)[2]*Const.partOfRockVisible)*scaleY));
 			canvas.drawBitmap(rockBitmap[rocks.get(i)[1]-1], renderMatrix, null);
 		}
 	}
 	
 	public void renderMentor(Canvas canvas){
 		float feetAngle;
 		if(mentor.isTouchingGround()){
 			feetAngle = (float)(180/Math.PI*Math.sin((double)mentor.getStepCount()/mentor.getNumberOfSteps()*Math.PI));
 		} else {
 			feetAngle = Const.jumpFeetAngle;
 		}
 		//Draw all the mentor parts
 		if(mentor.isFacingRight()){
 			float aimAngle = 0;
 			//Draw back foot
 			renderMatrix.setRotate(-feetAngle, footBitmap.getWidth()/2, footBitmap.getHeight()/2);
 			renderMatrix.postTranslate((float)((mentor.getXPos() - mentor.getWidth()*(0.5-Const.footXAxis-Const.backFootOffset) - mentor.getWidth()*Const.footRadius*Math.sin(-feetAngle*Math.PI/180) - screenX)*scaleX), (float)((mentor.getYPos() + mentor.getHeight()*(Const.footYAxis-0.5) + mentor.getHeight()*Const.footRadius*Math.cos(-feetAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(footBitmap, renderMatrix, null);
 			//Draw body
 			renderMatrix.setTranslate((float)((mentor.getXPos() - mentor.getWidth()*(0.5-Const.bodyXOffset) - screenX)*scaleX), (float)((mentor.getYPos() - mentor.getHeight()*(0.5-Const.bodyYOffset + Const.breathOffset*Math.sin((double)mentor.getBreathCount()/mentor.getBreathMax()*2*Math.PI)) - screenY)*scaleY));
 			canvas.drawBitmap(mentorBodyBitmap, renderMatrix, null);
 			//Draw eyes and mouth
 			renderMatrix.setTranslate((float)((mentor.getXPos() - mentor.getWidth()*(0.5-Const.eyeBeardXOffset) - screenX)*scaleX), (float)((mentor.getYPos() - mentor.getHeight()*(0.5-Const.eyeBeardYOffset) - screenY)*scaleY));
 			canvas.drawBitmap(eyeBeardBitmap, renderMatrix, null);
 			//Draw front foot
 			renderMatrix.setRotate(feetAngle, footBitmap.getWidth()/2, footBitmap.getHeight()/2);
 			renderMatrix.postTranslate((float)((mentor.getXPos() - mentor.getWidth()*(0.5-Const.footXAxis) - mentor.getWidth()*Const.footRadius*Math.sin(feetAngle*Math.PI/180) - screenX)*scaleX), (float)((mentor.getYPos() + mentor.getHeight()*(Const.footYAxis-0.5) + mentor.getHeight()*Const.footRadius*Math.cos(feetAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(footBitmap, renderMatrix, null);
 			//Draw pupils
 			renderMatrix.setTranslate((float)((mentor.getXPos() + mentor.getWidth()*(Const.pupilXOffset-0.5)+mentor.getWidth()*Const.pupilRadius*Math.cos(aimAngle*Math.PI/180)-screenX)*scaleX), (float)((mentor.getYPos() + mentor.getHeight()*(Const.pupilYOffset-0.5)-mentor.getHeight()*Const.pupilRadius*Math.sin(aimAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(pupilBitmap, renderMatrix, null);
 			//Draw weapon
 			renderMatrix.setRotate(-aimAngle, weaponBitmap.getWidth()/2, weaponBitmap.getHeight()/2);
 			renderMatrix.postTranslate((float)((mentor.getXPos() - mentor.getWidth()*(0.5-Const.weaponXAxis) + mentor.getWidth()*Const.weaponRadius*Math.cos(aimAngle*Math.PI/180) - screenX)*scaleX), (float)((mentor.getYPos() + mentor.getHeight()*(Const.weaponYAxis-0.5) - mentor.getHeight()*Const.weaponRadius*Math.sin(aimAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(weaponBitmap, renderMatrix, null);
 		} else {
 			float aimAngle = 180;
 			//Draw back foot
 			renderMatrix.setRotate(feetAngle, footBitmapFlipped.getWidth()/2, footBitmapFlipped.getHeight()/2);
 			renderMatrix.postTranslate((float)((mentor.getXPos() - mentor.getWidth()*(0.5-Const.footXAxis+Const.backFootOffset) - mentor.getWidth()*Const.footRadius*Math.sin(Math.PI-feetAngle*Math.PI/180) - screenX)*scaleX), (float)((mentor.getYPos() + mentor.getHeight()*(Const.footYAxis-0.5) + mentor.getHeight()*Const.footRadius*Math.cos(-feetAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(footBitmapFlipped, renderMatrix, null);
 			//Draw body
 			renderMatrix.setTranslate((float)((mentor.getXPos() + mentor.getWidth()*(0.5-Const.bodyXOffset) - screenX)*scaleX) - bodyBitmapFlipped.getWidth(), (float)((mentor.getYPos() - mentor.getHeight()*(0.5-Const.bodyYOffset + Const.breathOffset*Math.sin((double)mentor.getBreathCount()/mentor.getBreathMax()*2*Math.PI)) - screenY)*scaleY));
 			canvas.drawBitmap(mentorBodyBitmapFlipped, renderMatrix, null);
 			//Draw eyes and mouth
 			renderMatrix.setTranslate((float)((mentor.getXPos() + mentor.getWidth()*(0.5-Const.eyeBeardXOffset) - screenX)*scaleX) - eyeBeardBitmapFlipped.getWidth(), (float)((mentor.getYPos() - mentor.getHeight()*(0.5-Const.eyeBeardYOffset) - screenY)*scaleY));
 			canvas.drawBitmap(eyeBeardBitmapFlipped, renderMatrix, null);
 			//Draw front foot
 			renderMatrix.setRotate(-feetAngle, footBitmapFlipped.getWidth()/2, footBitmapFlipped.getHeight()/2);
 			renderMatrix.postTranslate((float)((mentor.getXPos() - mentor.getWidth()*(0.5-Const.footXAxis) - mentor.getWidth()*Const.footRadius*Math.sin(Math.PI+feetAngle*Math.PI/180) - screenX)*scaleX), (float)((mentor.getYPos() + mentor.getHeight()*(Const.footYAxis-0.5) + mentor.getHeight()*Const.footRadius*Math.cos(feetAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(footBitmapFlipped, renderMatrix, null);
 			//Draw pupils
 			renderMatrix.setTranslate((float)((mentor.getXPos() + mentor.getWidth()*(Const.pupilXOffset-0.5)+mentor.getWidth()*Const.pupilRadius*Math.cos(aimAngle*Math.PI/180)-screenX)*scaleX), (float)((mentor.getYPos() + mentor.getHeight()*(Const.pupilYOffset-0.5)-mentor.getHeight()*Const.pupilRadius*Math.sin(aimAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(pupilBitmapFlipped, renderMatrix, null);
 			//Draw weapon
 			renderMatrix.setRotate(180-aimAngle, weaponBitmapFlipped.getWidth()/2, weaponBitmapFlipped.getHeight()/2);
 			renderMatrix.postTranslate((float)((mentor.getXPos()  + mentor.getWidth()*(0.5-Const.weaponXAxis) + mentor.getWidth()*Const.weaponRadius*Math.cos(aimAngle*Math.PI/180) - screenX)*scaleX - weaponBitmapFlipped.getWidth()), (float)((mentor.getYPos() + mentor.getHeight()*(Const.weaponYAxis-0.5) + mentor.getHeight()*Const.weaponRadius*Math.sin(Math.PI+aimAngle*Math.PI/180) - screenY)*scaleY));
 			canvas.drawBitmap(weaponBitmapFlipped, renderMatrix, null);
 		}
 	}
 	
 	public void renderHint(Canvas canvas){
 		if(protagonist.getXPos() < checkpoints[currentCheckpoint] + pastCheckpointBorder){
 			for(int i = 0; i < hints.get(currentCheckpoint).size(); i++){
 				canvas.drawText(hints.get(currentCheckpoint).get(i), (float)(10*scaleX), (float)((30+textPaint.getTextSize()*i/scaleY)*scaleY), textPaint);
 			}
 		} else if(currentCheckpoint <= 12){
 			canvas.drawText("That's far enough for now!" , (float)(10*scaleX), (float)(30*scaleY), textPaint);
 		}
 	}
 
 	public void renderBird(Canvas canvas){
 		renderMatrix = new Matrix();
 		if(!bird.isAlive()){
 			renderMatrix.setRotate((float)bird.getRotation(), (float)(birdBitmap.getWidth()/2), (float)(birdBitmap.getHeight()/2));
 		}
 		renderMatrix.postTranslate((float)((bird.getX() - bird.getWidth()/2 - screenX)*scaleX), (float)((bird.getY() - bird.getHeight() - screenY)*scaleY));
 		canvas.drawBitmap(birdBitmap, renderMatrix, null);
 	}
 
 	@Override
 	public boolean onTouchEvent(MotionEvent e){
 		double x;
 		double y;
 
 		int index = e.getActionIndex();
 		int id = e.getPointerId(index);
 
 		switch(e.getActionMasked()){
 
 		case MotionEvent.ACTION_DOWN:
 			x = e.getX()/scaleX;
 			y = e.getY()/scaleY;
 
 			if(x > width/2){
 				rightStick.handleTouch(x, y);
 				rightStickId = id;
 			} else {
 				leftStick.handleTouch(x, y);
 				leftStickId = id;
 			}
 
 			break;
 
 		case MotionEvent.ACTION_POINTER_DOWN:
 
 			x = e.getX(index)/scaleX;
 			y = e.getY(index)/scaleY;
 
 
 			if(x > width/2){
 				rightStick.handleTouch(x, y);
 				rightStickId = id;
 			} else {
 				leftStick.handleTouch(x, y);
 				leftStickId = id;
 			}
 
 			break;
 
 		case MotionEvent.ACTION_MOVE:
 
 			for(index=0; index<e.getPointerCount(); index++) {
 				id=e.getPointerId(index);
 				x = (int) e.getX(index)/scaleX;
 				y = (int) e.getY(index)/scaleY; 
 
 				if(id == rightStickId) {
 					if(x > width/2){
 						rightStick.handleTouch(x, y);
 						rightStickId = id;
 					} 
 				}
 				else if(id == leftStickId){
 					if(x < width/2){
 						leftStick.handleTouch(x, y);
 						leftStickId = id;
 					}
 				}
 			}
 			break;
 
 		case MotionEvent.ACTION_UP:
 			rightStick.release();
 			leftStick.release();
 			leftStickId = INVALID_POINTER;
 			rightStickId = INVALID_POINTER;
 
 			
 			break;
 
 
 		case MotionEvent.ACTION_POINTER_UP:
 			if(id == leftStickId){
 				leftStick.release();
 				leftStickId = INVALID_POINTER;
 			}
 			if(id == rightStickId){
 				rightStick.release();
 				rightStickId = INVALID_POINTER;
 			}
 			break;
 		}
 		return true;
 	}
 
 	@Override
 	public void surfaceChanged(SurfaceHolder holder, int format, int width,
 			int height) {
 
 	}
 
 	@Override
 	public void surfaceCreated(SurfaceHolder holder) {
 		//Calculate the scale that we will use to render the game
 		scaleY = (double)getHeight()/height;
 		scaleX = (double)getWidth()/width;
 
 		//Load Bitmaps
 		bodyBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.valentine_in_game_90_body), (int)(protagonist.getWidth()*scaleX*Const.bodyXScale), (int)(protagonist.getHeight()*scaleY*Const.bodyYScale), true);
 		mentorBodyBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mentor_body), (int)(protagonist.getWidth()*scaleX*Const.bodyXScale), (int)(protagonist.getHeight()*scaleY*Const.bodyYScale), true);
 		eyeMouthBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.valentine_in_game_90_eye_mouth), (int)(protagonist.getWidth()*scaleX*Const.eyeMouthXScale), (int)(protagonist.getHeight()*scaleY*Const.eyeMouthYScale), true);
 		eyeBeardBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.eye_beard), (int)(protagonist.getWidth()*scaleX*Const.eyeBeardXScale), (int)(protagonist.getHeight()*scaleY*Const.eyeBeardYScale), true);
 		footBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.valentine_in_game_90_foot), (int)(protagonist.getWidth()*scaleX*Const.footXScale), (int)(protagonist.getHeight()*scaleY*Const.footYScale), true);
 		weaponBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.valentine_in_game_90_hand_gun), (int)(protagonist.getWidth()*scaleX*Const.weaponXScale), (int)(protagonist.getHeight()*scaleY*Const.weaponYScale), true);
 		pupilBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.valentine_in_game_90_pupil), (int)(protagonist.getWidth()*scaleX*Const.pupilXScale), (int)(protagonist.getHeight()*scaleY*Const.pupilYScale), true);
 		stickBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.stick), (int)(2*leftStick.getRadius()*scaleX), (int)(2*leftStick.getRadius()*scaleX), true);
 		bulletBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.bullet), (int)(Bullet.getRadius()*2*scaleX), (int)(Bullet.getRadius()*2*scaleY), true);
 		birdBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.bird), (int)(Bird.getWidth()*scaleX), (int)(Bird.getHeight()*scaleY), true);
 		treeBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.tree_1), (int)(Const.maxTreeWidth*scaleX), (int)(Const.maxTreeHeight*scaleY), true);
 		rockBitmap = new Bitmap[]{
 				Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.stone_1), (int)(Const.maxRockSize*scaleX), (int)(Const.maxRockSize*scaleY), true),
 				Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.stone_2), (int)(Const.maxRockSize*scaleX), (int)(Const.maxRockSize*scaleY), true),
 				Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.stone_3), (int)(Const.maxRockSize*scaleX), (int)(Const.maxRockSize*scaleY), true),
 				Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.stone_4), (int)(Const.maxRockSize*scaleX), (int)(Const.maxRockSize*scaleY), true),
 		};
 		
 		//Drone
 		enemyBodyBitmap[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_body), (int)(Enemy.getBaseWidth()*Const.enemyBodyXScale*scaleX), (int)(Enemy.getBaseHeight()*Const.enemyBodyYScale*scaleY), true);	
 		enemyEyeMouthBitmap[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_eye_mouth), (int)(Enemy.getBaseWidth()*Const.enemyEyeMouthXScale*scaleX), (int)(Enemy.getBaseHeight()*Const.enemyEyeMouthYScale*scaleY), true);
 		enemyLeftArmBitmap[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_arm), (int)(Enemy.getBaseWidth()*Const.enemyArmXScale*scaleX), (int)(Enemy.getBaseHeight()*Const.enemyArmYScale*scaleY), true);
 		enemyFootBitmap[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_foot), (int)(Enemy.getBaseWidth()*Const.enemyFootXScale*scaleX), (int)(Enemy.getBaseHeight()*Const.enemyFootYScale*scaleY), true);
 		enemyPupilBitmap[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_pupil), (int)(Enemy.getBaseWidth()*Const.enemyPupilXScale*scaleX), (int)(Enemy.getBaseHeight()*Const.enemyPupilYScale*scaleY), true);
 		
 		//Ninja
 		enemyBodyBitmap[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_body), (int)(Enemy.getBaseWidth()*Enemy.getScaleNinja()*Const.enemyBodyXScale*scaleX), (int)(Enemy.getBaseHeight()*Enemy.getScaleNinja()*Const.enemyBodyYScale*scaleY), true);	
 		enemyEyeMouthBitmap[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_eye_mouth), (int)(Enemy.getBaseWidth()*Enemy.getScaleNinja()*Const.enemyEyeMouthXScale*scaleX), (int)(Enemy.getBaseHeight()*Enemy.getScaleNinja()*Const.enemyEyeMouthYScale*scaleY), true);
 		enemyLeftArmBitmap[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_arm), (int)(Enemy.getBaseWidth()*Enemy.getScaleNinja()*Const.enemyArmXScale*scaleX), (int)(Enemy.getBaseHeight()*Enemy.getScaleNinja()*Const.enemyArmYScale*scaleY), true);
 		enemyFootBitmap[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_foot), (int)(Enemy.getBaseWidth()*Enemy.getScaleNinja()*Const.enemyFootXScale*scaleX), (int)(Enemy.getBaseHeight()*Enemy.getScaleNinja()*Const.enemyFootYScale*scaleY), true);
 		enemyPupilBitmap[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_pupil), (int)(Enemy.getBaseWidth()*Enemy.getScaleNinja()*Const.enemyPupilXScale*scaleX), (int)(Enemy.getBaseHeight()*Enemy.getScaleNinja()*Const.enemyPupilYScale*scaleY), true);
 		
 		//Tank
 		enemyBodyBitmap[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_body), (int)(Enemy.getBaseWidth()*Enemy.getScaleTank()*Const.enemyBodyXScale*scaleX), (int)(Enemy.getBaseHeight()*Enemy.getScaleTank()*Const.enemyBodyYScale*scaleY), true);	
 		enemyEyeMouthBitmap[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_eye_mouth), (int)(Enemy.getBaseWidth()*Enemy.getScaleTank()*Const.enemyEyeMouthXScale*scaleX), (int)(Enemy.getBaseHeight()*Enemy.getScaleTank()*Const.enemyEyeMouthYScale*scaleY), true);
 		enemyLeftArmBitmap[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_arm), (int)(Enemy.getBaseWidth()*Enemy.getScaleTank()*Const.enemyArmXScale*scaleX), (int)(Enemy.getBaseHeight()*Enemy.getScaleTank()*Const.enemyArmYScale*scaleY), true);
 		enemyFootBitmap[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_foot), (int)(Enemy.getBaseWidth()*Enemy.getScaleTank()*Const.enemyFootXScale*scaleX), (int)(Enemy.getBaseHeight()*Enemy.getScaleTank()*Const.enemyFootYScale*scaleY), true);
 		enemyPupilBitmap[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_pupil), (int)(Enemy.getBaseWidth()*Enemy.getScaleTank()*Const.enemyPupilXScale*scaleX), (int)(Enemy.getBaseHeight()*Enemy.getScaleTank()*Const.enemyPupilYScale*scaleY), true);
 		
 		Matrix m = new Matrix();
 		//Flip images
 		m.setScale(-1, 1);
 
 		bodyBitmapFlipped = Bitmap.createBitmap(bodyBitmap, 0, 0, bodyBitmap.getWidth(), bodyBitmap.getHeight(), m, false);
 		mentorBodyBitmapFlipped = Bitmap.createBitmap(mentorBodyBitmap, 0, 0, mentorBodyBitmap.getWidth(), mentorBodyBitmap.getHeight(), m, false);
 		eyeMouthBitmapFlipped = Bitmap.createBitmap(eyeMouthBitmap, 0, 0, eyeMouthBitmap.getWidth(), eyeMouthBitmap.getHeight(), m, false);
 		eyeBeardBitmapFlipped = Bitmap.createBitmap(eyeBeardBitmap, 0, 0, eyeBeardBitmap.getWidth(), eyeBeardBitmap.getHeight(), m, false);
 		footBitmapFlipped = Bitmap.createBitmap(footBitmap, 0, 0, footBitmap.getWidth(), footBitmap.getHeight(), m, false);
 		weaponBitmapFlipped = Bitmap.createBitmap(weaponBitmap, 0, 0, weaponBitmap.getWidth(), weaponBitmap.getHeight(), m, false);
 		pupilBitmapFlipped = Bitmap.createBitmap(pupilBitmap, 0, 0, pupilBitmap.getWidth(), pupilBitmap.getHeight(), m, false);
 		
 		enemyBodyBitmapFlipped[0] = Bitmap.createBitmap(enemyBodyBitmap[0], 0, 0, enemyBodyBitmap[0].getWidth(), enemyBodyBitmap[0].getHeight(), m, false);
 		enemyBodyBitmapFlipped[1] = Bitmap.createBitmap(enemyBodyBitmap[1], 0, 0, enemyBodyBitmap[1].getWidth(), enemyBodyBitmap[1].getHeight(), m, false);
 		enemyBodyBitmapFlipped[2] = Bitmap.createBitmap(enemyBodyBitmap[2], 0, 0, enemyBodyBitmap[2].getWidth(), enemyBodyBitmap[2].getHeight(), m, false);
 		
 		enemyEyeMouthBitmapFlipped[0] = Bitmap.createBitmap(enemyEyeMouthBitmap[0], 0, 0, enemyEyeMouthBitmap[0].getWidth(), enemyEyeMouthBitmap[0].getHeight(), m, false);
 		enemyEyeMouthBitmapFlipped[1] = Bitmap.createBitmap(enemyEyeMouthBitmap[1], 0, 0, enemyEyeMouthBitmap[1].getWidth(), enemyEyeMouthBitmap[1].getHeight(), m, false);
 		enemyEyeMouthBitmapFlipped[2] = Bitmap.createBitmap(enemyEyeMouthBitmap[2], 0, 0, enemyEyeMouthBitmap[2].getWidth(), enemyEyeMouthBitmap[2].getHeight(), m, false);
 		
 		enemyFootBitmapFlipped[0] = Bitmap.createBitmap(enemyFootBitmap[0], 0, 0, enemyFootBitmap[0].getWidth(), enemyFootBitmap[0].getHeight(), m, false);
 		enemyFootBitmapFlipped[1] = Bitmap.createBitmap(enemyFootBitmap[1], 0, 0, enemyFootBitmap[1].getWidth(), enemyFootBitmap[1].getHeight(), m, false);
 		enemyFootBitmapFlipped[2] = Bitmap.createBitmap(enemyFootBitmap[2], 0, 0, enemyFootBitmap[2].getWidth(), enemyFootBitmap[2].getHeight(), m, false);
 		
 		enemyRightArmBitmap[0] = Bitmap.createBitmap(enemyLeftArmBitmap[0], 0, 0, enemyLeftArmBitmap[0].getWidth(), enemyLeftArmBitmap[0].getHeight(), m, false);
 		enemyRightArmBitmap[1] = Bitmap.createBitmap(enemyLeftArmBitmap[1], 0, 0, enemyLeftArmBitmap[1].getWidth(), enemyLeftArmBitmap[1].getHeight(), m, false);		
 		enemyRightArmBitmap[2] = Bitmap.createBitmap(enemyLeftArmBitmap[2], 0, 0, enemyLeftArmBitmap[2].getWidth(), enemyLeftArmBitmap[2].getHeight(), m, false);
 		
 		
 		
 		//Start the thread
 		thread.setRunning(true);
 		try{thread.start();} catch(IllegalThreadStateException e){}
 	}
 
 	@Override
 	public void surfaceDestroyed(SurfaceHolder holder) {
 		//End the thread
 		boolean retry = true;
 		while(retry){
 			try{
 				thread.join();
 				retry = false;
 			} catch(InterruptedException e){
 
 			}
 		}
 	}
 
 
 	public Ground getGround() {
 		return ground;
 	}
 
 	public ArrayList<Platform> getPlatforms() {
 		return platforms;
 	}
 
 	//Pause the game
 	public void pause(){
 		thread.setRunning(false);
 	}
 	
 	//Resume the game
 	public void resume(){
 		thread.setRunning(true);
 	}
 }
