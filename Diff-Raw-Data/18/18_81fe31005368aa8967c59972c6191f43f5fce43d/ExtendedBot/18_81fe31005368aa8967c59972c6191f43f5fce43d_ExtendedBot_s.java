 package com.jaxelson;
 
 import java.awt.Graphics2D;
 import java.awt.Shape;
 import java.awt.geom.Ellipse2D;
 
 import robocode.AdvancedRobot;
 import robocode.util.Utils;
 
 public class ExtendedBot extends AdvancedRobot {
 
     public void drawCenteredCircle(Graphics2D g, Integer radius) {
     	double x = getX() - radius;
         double y = getY() - radius;
 		Shape circle = new Ellipse2D.Double(x, y, radius*2, radius*2);
         g.draw(circle);
     }
     
     public Double getDistanceToRightWall() {
     	double botRightEdge = getX();
     	double rightWallLocation = getBattleFieldWidth();
     	return rightWallLocation - botRightEdge;
     }
     
     /**
      * Turns to the desired angle
      * @param desiredAngle the desired angle in radians
      * @return how far is needed to turn
      */
 	public Double turnTo(double desiredAngle) {
 		double currentAngle = this.getHeadingRadians();
 		double turnDistanceRad = currentAngle - desiredAngle;
 		turnDistanceRad = Utils.normalRelativeAngle(turnDistanceRad);
 		this.setTurnLeftRadians(turnDistanceRad);
 		
 		return Math.abs(turnDistanceRad);
 	}
 	
 	public Double getCenterX() {
 		return new Double(this.getX());
 	}
 	
 	public Double getCenterY() {
 		return new Double(this.getY());
 	}
 }
