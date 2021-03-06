 
 package org.discobots.aerialassist;
 import org.discobots.aerialassist.utils.GamePad;
 import edu.wpi.first.wpilibj.buttons.Button;
 import edu.wpi.first.wpilibj.buttons.JoystickButton;
import org.discobots.aerialassist.commands.Catapult;
 import org.discobots.aerialassist.commands.Changedrive;
import org.discobots.aerialassist.commands.Hold;
 import org.discobots.aerialassist.controllers.FixAngle;
 import org.discobots.aerialassist.subsystems.Drivetrain;
 
 /**
  * This class is the glue that binds the controls on the physical operator
  * interface to the commands and command groups that allow control of the robot.
  */
 public class OI {
     //// CREATING BUTTONS
     // One type of button is a joystick button which is any button on a joystick.
     // You create one by telling it which joystick it's on and which button
     // number it is.
     // Joystick stick = new Joystick(port);
     // Button button = new JoystickButton(stick, buttonNumber);
     
     // Another type of button you can create is a DigitalIOButton, which is
     // a button or switch hooked up to the cypress module. These are useful if
     // you want to build a customized operator interface.
     // Button button = new DigitalIOButton(1);
     
     // There are a few additional built in buttons you can use. Additionally,
     // by subclassing Button you can create custom triggers and bind those to
     // commands the same as any other Button.
     
     //// TRIGGERING COMMANDS WITH BUTTONS
     // Once you have a button, it's trivial to bind it to a button in one of
     // three ways:
     
     // Start the command when the button is pressed and let it run the command
     // until it is finished as determined by it's isFinished method.
     // button.whenPressed(new ExampleCommand());
     
     // Run the command while the button is being held down and interrupt it once
     // the button is released.
     // button.whileHeld(new ExampleCommand());
     
     // Start the command when the button is released  and let it run the command
     // until it is finished as determined by it's isFinished method.
     // button.whenReleased(new ExampleCommand());
     
     GamePad controller=new GamePad(1,GamePad.MODE_D);
     Button A= new JoystickButton(controller,controller.BTN_A);
     Button B= new JoystickButton(controller,controller.BTN_B);
     Button X= new JoystickButton(controller,controller.BTN_X);
     Button Y= new JoystickButton(controller,controller.BTN_Y);
     Button LBump= new JoystickButton(controller,controller.BUMPER_L);
     Button RBump= new JoystickButton(controller,controller.BUMPER_R);
     Button LTrig= new JoystickButton(controller,controller.TRIGGER_L);
     Button RTrig= new JoystickButton(controller,controller.TRIGGER_R);
 
     public OI(){
         A.whenPressed(new Changedrive(Drivetrain.MECANUM));
         B.whenPressed(new Changedrive(Drivetrain.TRACTION));
         X.whenPressed(new FixAngle());
        LTrig.whenPressed(new Hold());
        RTrig.whenPressed(new Catapult());
     }
     
     public GamePad getGP(){
         return controller;
     }
 }
 
