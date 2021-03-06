 package com.wattu.sidpi;
 
 import com.wattu.sidpi.GPIOController;
 import com.wattu.sidpi.impl.GPIOControllerImpl;
 import com.wattu.test.sidpi.impl.GPIOControllerTestImpl;
 
 public class SIDPiController {
 
 	private static final int CS 	= GPIOController.PIN_NUMBERS[14];
 	private static final int RW 	= GPIOController.PIN_NUMBERS[15];
 	private static final int RES 	= GPIOController.PIN_NUMBERS[16];
 	private static final int CLK 	= GPIOController.PIN_NUMBERS[2];
 	private static final int[] DATA	= {GPIOController.PIN_NUMBERS[0],GPIOController.PIN_NUMBERS[1],GPIOController.PIN_NUMBERS[3]
 										,GPIOController.PIN_NUMBERS[4],GPIOController.PIN_NUMBERS[5],GPIOController.PIN_NUMBERS[6]
 										,GPIOController.PIN_NUMBERS[7],GPIOController.PIN_NUMBERS[8]};
 	
 	private static final int[] ADDR	= {GPIOController.PIN_NUMBERS[13],GPIOController.PIN_NUMBERS[12],GPIOController.PIN_NUMBERS[11]
 										,GPIOController.PIN_NUMBERS[10],GPIOController.PIN_NUMBERS[9]};
 			
 	private static final int DEFAULT_SID_SPEED_HZ = 1000000;
 	
 	private int currentSidSpeed = 0;
 	private boolean clockRunning = false;
 	private int[] vals = new int[8];
 	private int[] addr = new int[5];
 	
 	private GPIOController gpioController;
 	
 	public SIDPiController() {
 		
 		gpioController = new GPIOControllerImpl(); 
 		setClockSpeed(DEFAULT_SID_SPEED_HZ);
 		startClock();
 		reset();
 		setCSHigh();
 		
 	}
 
 	public void setReadMode() {
 		gpioController.setPin(RW, GPIOController.VALUE_HIGH);
 	}
 	
 	public void setWriteMode() {
 		gpioController.setPin(RW, GPIOController.VALUE_LOW);
 	}
 	
 	private void setResetHigh() {
 		gpioController.setPin(RES, GPIOController.VALUE_HIGH);
 	}
 
 	public void setCSLow() {
 		gpioController.setPin(CS, GPIOController.VALUE_LOW);
 	}
 
 	public void setCSHigh() {
 		gpioController.setPin(CS, GPIOController.VALUE_HIGH);
 	}
 
 	public int readRegister(int address) {
 		
 		addr[0] = (address & 1);
 		addr[1] = (address & 2) >> 1;
 		addr[2] = (address & 4) >> 2;
 		addr[3] = (address & 8) >> 3;
 		addr[4] = (address & 16) >> 4;
 		
 		gpioController.setPins(ADDR, addr);
 		setReadMode();
 		setCSLow();
 		clockHigh();
 		int[] data = gpioController.getPins(DATA);
 		setCSHigh();
 		clockLow();
 		
 		int value = data[0] | 
 					(data[1] << 1) |
 					(data[2] << 2) |
 					(data[3] << 3) |
 					(data[4] << 4) |
 					(data[5] << 5) |
 					(data[6] << 6) |
 					(data[7] << 7); 
 					
 					
 		return value;
 	}
 
 	public void writeRegister(int address, int value) {
 		
 		addr[0] = (address & 1);
 		addr[1] = (address & 2) >> 1;
 		addr[2] = (address & 4) >> 2;
 		addr[3] = (address & 8) >> 3;
 		addr[4] = (address & 16) >> 4;
 		
 		gpioController.setPins(ADDR, addr);
 		setCSLow();
 		
 		vals[0] = value & 1;
 		vals[1] = (value & 2) >> 1;
 		vals[2] = (value & 4) >> 2;
 		vals[3] = (value & 8) >> 3;
 		vals[4] = (value & 16) >> 4;
 		vals[5] = (value & 32) >> 5;
 		vals[6] = (value & 64) >> 6;
 		vals[7] = (value & 128) >> 7;
 		
 		gpioController.setPins(DATA,vals);
 		setCSHigh();
 		clockLow();
 	}
 
 	public void setClockSpeed(int speed) {
 		currentSidSpeed = speed;
 	}
 	
 	public boolean isClockRunning() {
 		return clockRunning;
 	}
 	public int getClockSpeed() {
 		return currentSidSpeed;
 	}
 
 	public void reset() {
 		gpioController.setPin(RES, GPIOController.VALUE_LOW);
 		
 		waitForCycles(8);
 		
 		gpioController.setPin(RES, GPIOController.VALUE_HIGH);
 		
 	}
 	
 	public void waitForCycles(int cycles) {
 		long target = gpioController.getClock() + cycles;
 			
 		while(gpioController.getClock() < target); 
 	}
 	
 	public void advanceClock() {
 		advanceClock(1);
 	}
 	
 	private void advanceClock(int cycles)  {
 	
 	}
 	public long getCurrentCycle() {
 		return gpioController.getClock();
 	}
 
 	private void clockHigh() {
 		if(!clockRunning) {
 			gpioController.setPin(CLK, GPIOController.VALUE_HIGH);
 		}
 	}
 	
 	private void clockLow() {
 	
 	}
 
 	public void startClock() {
 		gpioController.clockSpeed(CLK, currentSidSpeed);
 		clockRunning = true;
 	}
 	
 	public void stopClock() {
 		gpioController.setPinMode(CLK,GPIOController.MODE_OUT);
 		gpioController.setPin(CLK, GPIOController.VALUE_LOW);
 		clockRunning = false;
 	}
 }
