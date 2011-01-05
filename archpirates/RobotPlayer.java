package archpirates;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class RobotPlayer implements Runnable {

	private final RobotController myRC;
    private final Archbot myBot;

    public RobotPlayer(RobotController rc) {
        myRC = rc;
    }

	public void run() {
		ComponentController [] = myRC.newComponents();
        //DEBUG
		//System.out.println(java.util.Arrays.toString(components));
		//System.out.flush();
        switch(myRC.getChassis()) {
            case Chassis.BUILDING:
                myBot = new Building(myRC);
                break;
            default:
                myBot = new Archbot(myRC);
                break;
        }
        myBot.init()
		if(myRC.getChassis()==Chassis.BUILDING)
			runBuilder((MovementController)components[0],(BuilderController)components[2]);
		else
			runMotor((MovementController)components[0]);
	}

	public void testit(MovementController m) {
		m.withinRange(myRC.getLocation());
	}

	public void runBuilder(MovementController motor, BuilderController builder) {
		while (true) {
            try {
				myRC.yield();

				if(!motor.canMove(myRC.getDirection())) {
					motor.setDirection(myRC.getDirection().rotateRight());
                } else if(myRC.getTeamResources() >= 2*Chassis.LIGHT.cost) {
					builder.build(Chassis.LIGHT,myRC.getLocation().add(myRC.getDirection()));
                }
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }
        }
	}

    public void runMotor(MovementController motor) {
        while (true) {
            try {
                /*** beginning of main loop ***/
                while (motor.isActive()) {
                    myRC.yield();
                }

                if (motor.canMove(myRC.getDirection())) {
                    //DEBUG
                    //System.out.println("about to move");
                    motor.moveForward();
                } else {
                    motor.setDirection(myRC.getDirection().rotateRight());
                }

                /*** end of main loop ***/
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }
        }
    }
}
