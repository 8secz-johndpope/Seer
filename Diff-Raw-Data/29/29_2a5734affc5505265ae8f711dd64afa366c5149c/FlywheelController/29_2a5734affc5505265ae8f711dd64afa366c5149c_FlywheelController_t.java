 /*
  * FIRST Team 254 - The Cheesy Poofs
  * Team 254 Lib
  * Control
  * Mechanisms
  * Flywheel Controller
  *
  * Controls a flywheel using full state feedback.
  */
 package com.team254.lib.control.impl;
 
 import com.team254.lib.control.ControlOutput;
 import com.team254.lib.control.ControlSource;
 import com.team254.lib.control.StateSpaceController;
 import com.team254.lib.control.StateSpaceGains;
 import com.team254.lib.util.Matrix;
 
 /**
  *
  * @author Tom Bottiglieri
  */
 public class FlywheelController extends StateSpaceController {
 
   ControlOutput output;
   ControlSource sensor;
   double velGoal; // Radians per second
   Matrix y;
   Matrix r;
   double prevPos;
   double goal;
   double curVel;
   double period = 1 / 50.0;
   double outputVoltage = 0.0;
   double targetError = 30;
 
   public FlywheelController(String name, ControlOutput output, ControlSource sensor, StateSpaceGains gains) {
     this(name, output, sensor, gains, 1 / 50.0);
   }
 
   public FlywheelController(String name, ControlOutput output, ControlSource sensor, StateSpaceGains gains, double period) {
     super(name, 1, 1, 2, gains, period);
     this.output = output;
     this.sensor = sensor;
     this.velGoal = 0.0;
     this.y = new Matrix(1, 1);
     this.r = new Matrix(2, 1);
     this.period = period;
   }
 
   public void capU() {
       double deltaU = U.get(0);
       double u_max = Umax.get(0);
       double u_min = Umin.get(0);
       double u = Xhat.get(0);
 
       double upWindow = u_max - outputVoltage;
       double downWindow = u_min - outputVoltage;
 
       if (deltaU > upWindow) {
         deltaU = upWindow ;
       } else if (deltaU < downWindow) {
         deltaU = downWindow;
       }
       outputVoltage += deltaU;
       U.set(0, deltaU);
   }
 
   public void update() {
     if (!enabled) {
       output.set(0);
       return;
     }
 
     double curSensorVel = sensor.get();
     curVel = curSensorVel;
 
     this.y.flash(new double[]{curSensorVel});
 
     r.flash(new double[]{(velGoal * (1 - A.get(1,1)))/ B.get(1,0), velGoal});
 
     // Update SSC
     update(r, y);
 
     if (velGoal < 1.0) {
       this.output.set(0.0);
       goal = curSensorVel;
     } else {
       this.output.set(outputVoltage / 12.0);
     }
   }
 
   public double getVelocity() {
     return curVel;
   }
 
   public void setVelocityGoal(double v) {
     velGoal = v;
   }
 
   public void enable() {
     enabled = true;
   }
 
   public void disable() {
     enabled = false;
     output.set(0);
     curVel = 0;
   }

   public boolean onTarget() {
     return enabled && Math.abs(curVel - velGoal) < targetError;
   }
 
   public double getVelocityGoal() {
     return velGoal;
   }
 
   public void setGoal(double goal) {
   }
 
   public double getGoal() {
     return 0;
   }
 }
