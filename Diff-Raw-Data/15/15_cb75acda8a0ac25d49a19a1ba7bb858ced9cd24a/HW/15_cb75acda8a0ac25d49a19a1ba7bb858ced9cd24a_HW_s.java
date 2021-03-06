 package org.discobots.aerialassist;
 
 import org.discobots.aerialassist.utils.AngleController;
 import org.discobots.aerialassist.commands.CommandBase;
 
 
 public class HW {
 
     /**---------------------------------
      * Motors
      ---------------------------------*/
     public static final int leftRearMotor = 1, rightRearMotor = 2,
                             leftFrontMotor = 3, rightFrontMotor= 4,
                             rollerMotor = 5, catapultMotor=7,
                             motorModule=1;
 
     /**---------------------------------
      * Pneumatics
      ---------------------------------*/
     public static final int /*compressorDigital = 2,*/ compressorRelay = 1,
                              pressureSwitchAnalog = 6, spikeReplacementVictor=1;
     
                            
     //Port 3 is bad so I changed driveShiftASolenoid to 5 and pressureSwitchAnalog to 6.
    public static final int extenderSolenoid = 1, pneumatapultSolenoid = 2,
                             driveShiftSolenoidForward = 7, driveShiftSolenoidReverse = 6, 
                             solonoidModule = 1;
     
     /**---------------------------------
      * Sensors
      ---------------------------------*/
     public static final int gyroChannel = 2, chooChooTouchSensor = 11;
 }
