 package com.runnershigh;
 
 
 import java.util.Random;
 import android.content.Context;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.graphics.Color;
 import android.graphics.Paint;
 import android.graphics.Rect;
 import android.util.Log;
 
 
 public class Level {
 	private int width;
 	private int height;
 	private float levelPosition;
 	private float deltaLevelPosition;
 	//public static float scoreCounter;
 	public float baseSpeed;
 	public float baseSpeedMax;
 	public float baseSpeedStart;
 	public float baseSpeedAcceleration;
 	public float extraSpeed;
 	public float extraSpeedStart;
 	public float extraSpeedMax;
 	public float extraSpeedAcceleration;
 	
 	public static Block[] blockData;
 	public static final int maxBlocks = 5;
 	private int leftBlockIndex;
 	private int rightBlockIndex;
 
 	public static Obstacle[] obstacleDataSlower;
 	public static final int maxObstaclesSlower = maxBlocks;
 	private int leftSlowerIndex;
 	private int rightSlowerIndex;
 	
 	public static Obstacle[] obstacleDataJumper;
 	public static final int maxObstaclesJumper = maxBlocks;
 	private int leftJumperIndex;
 	private int rightJumperIndex;
 	
 	public static Obstacle[] obstacleDataBonus;
 	public static final int maxObstaclesBonus = maxBlocks;
 	private int leftBonusIndex;
 	private int rightBonusIndex;
 
 	private float obstacleJumperWidth;
 	private float obstacleJumperHeight;
 	
 	private float obstacleSlowerWidth;
 	private float obstacleSlowerHeight;
 	
 	private float obstacleBonusWidth;
 	private float obstacleBonusHeight;
 	private float obstacleBonusDistanceToBlock;
 	
 	private final int OBSTACLEMASK_0_NO_OBSTACLE = 80;
 	private final int OBSTACLEMASK_1_JUMP = 30;
 	private final int OBSTACLEMASK_2_SLOW = 30;
 	private final int OBSTACLEMASK_3_JUMP_SLOW = 20;
 	private final int OBSTACLEMASK_4_BONUS = 40;
 	private final int OBSTACLEMASK_5_JUMP_BONUS = 20;
 	private final int OBSTACLEMASK_6_SLOW_BONUS = 20;
 	private final int OBSTACLEMASK_7_JUMP_SLOW_BONUS = 10;
 	
 	private final int OBSTACLEMASK_MAX =
 		OBSTACLEMASK_0_NO_OBSTACLE + 
 		OBSTACLEMASK_1_JUMP + 
 		OBSTACLEMASK_2_SLOW + 
 		OBSTACLEMASK_3_JUMP_SLOW + 
 		OBSTACLEMASK_4_BONUS + 
 		OBSTACLEMASK_5_JUMP_BONUS + 
 		OBSTACLEMASK_6_SLOW_BONUS +
 		OBSTACLEMASK_7_JUMP_SLOW_BONUS;
 	
 	private Bitmap obstacleSlowImg;
 	private Bitmap obstacleJumpImg;
 	private Bitmap obstacleBonusImg;
 	
 	private boolean slowDown;
 	Paint paint;
 	Rect blockRect;
 	private int BlockCounter;
 	private OpenGLRenderer renderer;
 	
 	private Random randomGenerator;
 	private boolean lastBlockWasSmall = false;
 	private int minBlockWidth = 0;
 	
 	public Level(Context context, OpenGLRenderer glrenderer, int _width, int _heigth) {
 		if(Settings.RHDEBUG)
 			Log.d("debug", "in Level constructor");
 		
 		width = _width;
 		height = _heigth;
 		levelPosition = 0;
 		//lastLevelPosition = 0;
 		deltaLevelPosition = 0;
 		//scoreCounter = 0;
 		
 		baseSpeedStart = Util.getPercentOfScreenWidth(0.075f);
 		baseSpeed = baseSpeedStart;
 		baseSpeedMax = Util.getPercentOfScreenWidth(0.375f);
 		baseSpeedAcceleration = baseSpeed*0.025f;
 		
 		extraSpeedStart = Util.getPercentOfScreenWidth(0.025f);
 		extraSpeed = extraSpeedStart;
 		extraSpeedMax = Util.getPercentOfScreenWidth(0.5f);
 		extraSpeedAcceleration = extraSpeed * 0.010f;
 		
		obstacleJumperWidth = Util.getPercentOfScreenWidth(5);
		obstacleJumperHeight = Util.getPercentOfScreenHeight(3);
 		
		obstacleSlowerWidth = Util.getPercentOfScreenWidth(6);
		obstacleSlowerHeight= Util.getPercentOfScreenHeight(3);
 		
 		obstacleBonusWidth = Util.getPercentOfScreenWidth(5);
 		obstacleBonusHeight = obstacleBonusWidth*Util.mWidthHeightRatio;
		obstacleBonusDistanceToBlock = Util.getPercentOfScreenHeight(12);
 		
 		
 		if(Settings.RHDEBUG){
 			Log.d("debug", "obstacleJumperWidth" + obstacleJumperWidth);
 			Log.d("debug", "obstacleJumperHeight" + obstacleJumperHeight);
 			Log.d("debug", "obstacleSlowerWidth" + obstacleSlowerWidth);
 			Log.d("debug", "obstacleSlowerHeight" + obstacleSlowerHeight);
 			Log.d("debug", "obstacleBonusWidth" + obstacleBonusWidth);
 			Log.d("debug", "obstacleBonusHeight" + obstacleBonusHeight);
 			Log.d("debug", "obstacleBonusDistanceToBlock" + obstacleBonusDistanceToBlock);
 		}
 		renderer = glrenderer;
 		
 		randomGenerator = new Random();
 		
 		paint = new Paint();
 		paint.setColor(Color.RED);
 		paint.setStyle(Paint.Style.FILL);
 		
 		blockData = new Block[maxBlocks];
 		leftBlockIndex = 0;
 		rightBlockIndex = maxBlocks;
 
 		obstacleDataSlower = new Obstacle[maxObstaclesSlower];
 		leftSlowerIndex = 0;
 		rightSlowerIndex = maxObstaclesSlower;
 		
 		obstacleDataJumper = new Obstacle[maxObstaclesJumper];
 		leftJumperIndex = 0;
 		rightJumperIndex = maxObstaclesJumper;
 		
 		obstacleDataBonus = new Obstacle[maxObstaclesBonus];
 		leftBonusIndex = 0;
 		rightBonusIndex = maxObstaclesBonus;
 
 		
 		obstacleSlowImg = BitmapFactory.decodeResource(context.getResources(), R.drawable.obstacleslow );
 		obstacleJumpImg = BitmapFactory.decodeResource(context.getResources(), R.drawable.obstaclejump );
 		obstacleBonusImg = BitmapFactory.decodeResource(context.getResources(), R.drawable.bonusimage);
 		
 		Block.setTextureLeft(
 				BitmapFactory.decodeResource(
 						context.getResources(), R.drawable.blockleft ));
 		Block.setTextureMiddle(
 				BitmapFactory.decodeResource(
 						context.getResources(), R.drawable.blockmiddle ));
 		Block.setTextureRight(
 				BitmapFactory.decodeResource(
 						context.getResources(), R.drawable.blockright ));
 		
 		
 		slowDown = false;
 		
 		initializeBlocks(true);
 		initializeObstacles(true);
 		
 	}
 	
 	public void update() {
 		
 		synchronized (blockData) {
 			//Log.d("debug", "in update");
 			
 
 			
 			if (0 > blockData[leftBlockIndex].BlockRect.right) {
 				appendBlockToEnd();
 				
 				if (BlockCounter > 10)
 					decideIfAndWhatObstaclesSpawn();
 			}
 			
 			baseSpeedAcceleration = baseSpeed * 0.005f;
 			extraSpeedAcceleration = extraSpeed * 0.005f;
 			
 			if(baseSpeed < baseSpeedMax)
 				baseSpeed+=baseSpeedAcceleration; //baseSpeed+=0.025;
 			
 			if(extraSpeed < extraSpeedMax)
 				extraSpeed+=extraSpeedAcceleration; //0.001; //extraSpeed+=0.001;
 			
 			if(slowDown){
 				//extraSpeed=0;
 				baseSpeed=baseSpeedStart;
 				slowDown=false;
 			}
 			
 			
 			deltaLevelPosition = baseSpeed + extraSpeed;
 			levelPosition += deltaLevelPosition;
 
 
 			//Log.d("debug", "deltaLevelPosition/10: " + deltaLevelPosition/10);
 			//scoreCounter += deltaLevelPosition/10;
 			
 			for (int i = 0; i < maxBlocks; i++)
 			{
 				blockData[i].x -= deltaLevelPosition;
 				blockData[i].updateRect();
 			}
 			
 			for (int i = 0; i < maxObstaclesJumper; i++)
 			{
 				obstacleDataJumper[i].x -= deltaLevelPosition;
 			}
 			
 			for (int i = 0; i < maxObstaclesSlower; i++)
 			{
 				obstacleDataSlower[i].x -= deltaLevelPosition;
 			}
 			
 			for (int i = 0; i < maxObstaclesBonus; i++)
 			{
 				obstacleDataBonus[i].centerX -= deltaLevelPosition;
				obstacleDataBonus[i].updateObstacleCircleMovement();
 			}
 			
 			
 			//lastLevelPosition=levelPosition;
 			//Log.d("debug", "in update after value mod");
 		}	
 	}
 	
 	private void initializeBlocks(Boolean firstTime) {
 		if(Settings.RHDEBUG)
 			Log.d("debug", "in initializeBlocks");
 		//Log.d("debug", "blockData.size() -> " + Integer.toString(blockData.size()) );
 		
 		if (firstTime)
 			blockData[0] = new Block();
 		blockData[0].x = 0;
 		blockData[0].setWidth(width);
 		blockData[0].setHeight(Settings.FirstBlockHeight);
 		blockData[0].updateRect();
 		
 		if(Settings.RHDEBUG)
 			Log.d("debug", "after blockdata 0");
 
 		if(firstTime)
 			renderer.addMesh(blockData[0]);
 		
 		leftBlockIndex = 1;
 		rightBlockIndex = 0;
 		
 		if(Settings.RHDEBUG)
 			Log.d("debug", "before for");
 		
 		for(int i = 1; i < maxBlocks; i++)
 		{
 			if (firstTime)
 				blockData[i] = new Block();
 			
 			if (firstTime)
 				renderer.addMesh(blockData[i]);
 			appendBlockToEnd();
 			blockData[i].updateRect();
 		}
 		if(Settings.RHDEBUG)
 			Log.d("debug", "left initializeBlocks");
 	}
 	
 	private void appendBlockToEnd()
 	{
 		if (minBlockWidth == 0) {
 			minBlockWidth =
 					Block.getTextureLeftWidth() + 
 					Block.getTextureRightWidth() + 
 					Block.getTextureMiddleWidth() * 2;
 		}
 		//Log.d("debug", "in appendBlockToEnd");
 		float newHeight;
 		float oldHeight;
 		float newWidth;
 		float distance;
 		float newLeft;
 		boolean thisBlockIsSmall = false;
 		
 		oldHeight = blockData[rightBlockIndex].BlockRect.top;
 		
 		if (oldHeight > height/2)
 			newHeight = (int)(Math.random()*height/3*2 + height/8);
 		else
 			newHeight = (int)(Math.random()*height/4 + height/8);
 		
 		if (lastBlockWasSmall) lastBlockWasSmall = false;
 		else if (50 - BlockCounter <= 0) thisBlockIsSmall = true;
 		else thisBlockIsSmall = (randomGenerator.nextInt(50 - BlockCounter) <= 5);
 		
 		if (thisBlockIsSmall) {
 			newWidth = minBlockWidth;
 			lastBlockWasSmall = true;
 		}
 		else {
 			newWidth = (int)(Math.random()*width/3+width/3);
 		}
 		
 		newWidth -= (newWidth - Block.getTextureLeftWidth() - Block.getTextureRightWidth()) % (Block.getTextureMiddleWidth());
 		
 		distance = (int)(Math.random()*width/16+width/12); 
 		
 //		newWidth = width/2; // FIXME REMOVE DEBUG VALUE
 //		distance = width/11; // FIXME REMOVE DEBUG VALUE
 		
 		if(distance <= Player.width)
 			distance = Player.width+2;
 		
 		Block lastBlock = blockData[rightBlockIndex];
 		newLeft = lastBlock.BlockRect.right + distance;
 		
 		blockData[leftBlockIndex].setHeight(newHeight);
 		blockData[leftBlockIndex].setWidth(newWidth);
 		blockData[leftBlockIndex].x = newLeft;
 
 		leftBlockIndex++;
 	    if (leftBlockIndex == maxBlocks)
 	    	leftBlockIndex = 0;
 	    
 	    rightBlockIndex++;
 	    if (rightBlockIndex== maxBlocks)
 	    	rightBlockIndex = 0;
 	    
 		BlockCounter++;
 
 		//Log.d("debug", "left appendBlockToEnd");
 	}
 	
 	private void initializeObstacles(Boolean firstTime)
 	{
 		for(int i = 0; i < maxObstaclesJumper; i++)
 		{
 			if (firstTime)
 			{
 				obstacleDataJumper[i] = new Obstacle(-1000, 0, 0.9f, obstacleJumperWidth, obstacleJumperHeight, 'j');
 				renderer.addMesh(obstacleDataJumper[i]);
 				obstacleDataJumper[i].loadBitmap(obstacleJumpImg);
 			}
 			obstacleDataJumper[i].x = -1000;
 			obstacleDataJumper[i].didTrigger = false;
 		}
 		for(int i = 0; i < maxObstaclesSlower; i++)
 		{
 			if (firstTime)
 			{
 				obstacleDataSlower[i] = new Obstacle(-1000, 0, 0.9f, obstacleSlowerWidth, obstacleSlowerHeight, 's');
 				renderer.addMesh(obstacleDataSlower[i]);				
 				obstacleDataSlower[i].loadBitmap(obstacleSlowImg);
 			}
 			
 			obstacleDataSlower[i].x = -1000;
 			obstacleDataSlower[i].didTrigger = false;
 		}
 		for(int i = 0; i < maxObstaclesBonus; i++)
 		{
 			if (firstTime)
 			{
 				obstacleDataBonus[i] = new Obstacle(-1000, 0, 0.9f, obstacleBonusWidth, obstacleBonusHeight, 'b');
 				renderer.addMesh(obstacleDataBonus[i]);
 				obstacleDataBonus[i].loadBitmap(obstacleBonusImg);
 			}
 			obstacleDataBonus[i].centerX = -1000;
 			obstacleDataBonus[i].didTrigger = false;
 		}
 	}
 	
 	private void decideIfAndWhatObstaclesSpawn()
 	{
 		int obstacleValue =randomGenerator.nextInt(OBSTACLEMASK_MAX);
 		
 		if (obstacleValue < OBSTACLEMASK_0_NO_OBSTACLE)
 		{
 			return;
 		}
 		obstacleValue -= OBSTACLEMASK_0_NO_OBSTACLE;
 		
 		if (obstacleValue < OBSTACLEMASK_1_JUMP)
 		{
 			appendObstaclesToEnd(true, false, false);
 			return;
 		}
 		obstacleValue -= OBSTACLEMASK_1_JUMP;
 		
 		if (obstacleValue < OBSTACLEMASK_2_SLOW)
 		{
 			appendObstaclesToEnd(false, true, false);
 			return;
 		}
 		obstacleValue -= OBSTACLEMASK_2_SLOW;
 		
 		if (obstacleValue < OBSTACLEMASK_3_JUMP_SLOW)
 		{
 			appendObstaclesToEnd(true, true, false);
 			return;
 		}
 		obstacleValue -= OBSTACLEMASK_3_JUMP_SLOW;
 		
 		if (obstacleValue < OBSTACLEMASK_4_BONUS)
 		{
 			appendObstaclesToEnd(false, false, true);
 			return;
 		}
 		obstacleValue -= OBSTACLEMASK_4_BONUS;
 		
 		if (obstacleValue < OBSTACLEMASK_5_JUMP_BONUS)
 		{
 			appendObstaclesToEnd(true, false, true);
 			return;
 		}
 		obstacleValue -= OBSTACLEMASK_5_JUMP_BONUS;
 		
 		if (obstacleValue < OBSTACLEMASK_6_SLOW_BONUS)
 		{
 			appendObstaclesToEnd(false, true, true);
 			return;
 		}
 		obstacleValue -= OBSTACLEMASK_6_SLOW_BONUS;
 		
 		if (obstacleValue < OBSTACLEMASK_7_JUMP_SLOW_BONUS)
 		{
 			appendObstaclesToEnd(true, true, true);
 			return;
 		}
 		obstacleValue -= OBSTACLEMASK_7_JUMP_SLOW_BONUS;
 		
 	}
 	
 	private void appendObstaclesToEnd(Boolean spawnJumper, Boolean spawnSlower, Boolean spawnBonus)
 	{
 		if (spawnSlower)
 		{
 			float obstacleLeft;
 			
 		    // compute a fraction of the range, 0 <= frac < range
 		    long fraction = (long)(blockData[rightBlockIndex].mWidth * 0.33 * randomGenerator.nextDouble());
 
 		    Obstacle newSlowObstacle = obstacleDataSlower[leftSlowerIndex];
 		    newSlowObstacle.didTrigger = false;
 			
 		    obstacleLeft =  
 		    	blockData[rightBlockIndex].x + blockData[rightBlockIndex].mWidth
     			- newSlowObstacle.width - fraction; 
 		    
 		    newSlowObstacle.x = obstacleLeft;
 		    newSlowObstacle.y = blockData[rightBlockIndex].mHeight;
 		    newSlowObstacle.setObstacleRect(
 		    		obstacleLeft,
 		    		obstacleLeft+newSlowObstacle.width,
 		    		blockData[rightBlockIndex].mHeight,
 		    		blockData[rightBlockIndex].mHeight-newSlowObstacle.height);
 			
 		    leftSlowerIndex++;
 		    if (leftSlowerIndex == maxObstaclesSlower)
 		    	leftSlowerIndex = 0;
 		    
 		    rightSlowerIndex++;
 		    if (rightSlowerIndex == maxObstaclesSlower)
 		    	rightSlowerIndex = 0;
 		    
 		}
 		
 		if (spawnJumper)
 		{
 			if(Settings.RHDEBUG)
 				Log.d("debug", "in spawnJumper");
 			float obstacleLeft;
 			Obstacle newJumpObstacle = obstacleDataJumper[leftJumperIndex];
 			newJumpObstacle.didTrigger = false;
 			
 			long fraction = (long)(blockData[rightBlockIndex].mWidth * 0.33 * randomGenerator.nextDouble());
 			
 			obstacleLeft =  (blockData[rightBlockIndex].x + newJumpObstacle.width + fraction);
 		
 			newJumpObstacle.x = obstacleLeft;
 		    newJumpObstacle.y = blockData[rightBlockIndex].mHeight;
 		    newJumpObstacle.setObstacleRect(
 		    		obstacleLeft,
 		    		obstacleLeft+newJumpObstacle.width,
 		    		blockData[rightBlockIndex].mHeight,
 		    		blockData[rightBlockIndex].mHeight-newJumpObstacle.height);
 		    
 		    if(Settings.RHDEBUG)
 		    	Log.d("debug", "x: " + newJumpObstacle.x +
 		    		" / y: " + newJumpObstacle.y +
 		    		" / z: " + newJumpObstacle.z);
 		    
 		    
 		    leftJumperIndex++;
 		    if (leftJumperIndex == maxObstaclesJumper)
 		    	leftJumperIndex = 0;
 		    
 		    rightJumperIndex++;
 		    if (rightJumperIndex == maxObstaclesJumper)
 		    	rightJumperIndex = 0;
 		    
 		}
 		
 		if (spawnBonus)
 		{
 			if(Settings.RHDEBUG)
 				Log.d("debug", "in spawnBonus");
 			
 			float range = blockData[rightBlockIndex].mWidth;
 			
 			int bonusLeft;
 			
 		    // compute a fraction of the range, 0 <= frac < range
 		    double fraction = range * randomGenerator.nextDouble();
 		    
 			Obstacle newBonus = obstacleDataBonus[leftBonusIndex];
 			newBonus.didTrigger = false;
 			
 		    newBonus.z=0;
 			
 		    bonusLeft = (int)(blockData[rightBlockIndex].x + fraction );
 		    
 		    //set new coordinates
 		    newBonus.x = newBonus.centerX = bonusLeft;
 		    newBonus.y = newBonus.centerY = blockData[rightBlockIndex].mHeight+obstacleBonusDistanceToBlock+randomGenerator.nextInt(75);
 		    
 		    newBonus.setObstacleRect(bonusLeft,
 		    		bonusLeft+newBonus.width,
 		    		blockData[rightBlockIndex].mHeight,
 		    		blockData[rightBlockIndex].mHeight-newBonus.height);
 		    
 		    if(Settings.RHDEBUG)
 		    	Log.d("debug", "x: " + newBonus.x +
 		    		" / y: " + newBonus.y +
 		    		" / z: " + newBonus.z);
 		    
 		    leftBonusIndex++;
 		    if (leftBonusIndex == maxObstaclesBonus)
 		    	leftBonusIndex = 0;
 		    
 		    rightBonusIndex++;
 		    if (rightBonusIndex== maxObstaclesBonus)
 		    	rightBonusIndex = 0;
 		    
 		}
 	}
 	
 	public int getDistanceScore()
 	{
 		return (int)(levelPosition * 800 / width / 10);
 	}
 	
 	public void lowerSpeed() {
 		slowDown = true;
 	}
 	public float getLevelPosition(){
 		return levelPosition;
 	}
 	public void reset() {
 		synchronized (blockData) {
 			levelPosition = 0;
 			
 			initializeBlocks(false);
 			initializeObstacles(false);
 			
 			baseSpeed = baseSpeedStart;
 			extraSpeed = extraSpeedStart;
 			BlockCounter=0;
 		}
 	}
 }
 
