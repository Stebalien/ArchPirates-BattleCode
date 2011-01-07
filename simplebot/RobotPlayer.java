package simplebot;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import simplebot.modules.Navigation;

public class RobotPlayer implements Runnable {
	private final RobotController myRC;
    private Navigation nav;

    public RobotPlayer(RobotController rc) {
        myRC = rc;
    }

	public void run() {
		ComponentController [] components = myRC.newComponents();
		System.out.println(java.util.Arrays.toString(components));
		System.out.flush();
		if(myRC.getChassis()==Chassis.BUILDING) {
			runBuilder((MovementController)components[0],(BuilderController)components[2]);
		} else {
            nav = new Navigation(myRC, (MovementController)components[0]);
			runMotor();
        }
	}

	public void testit(MovementController m) {
		m.withinRange(myRC.getLocation());
	}

	public void runBuilder(MovementController motor, BuilderController builder) {
		while (true) {
            /*
            try {
				myRC.yield();

				if(!motor.canMove(myRC.getDirection())) {
					motor.setDirection(myRC.getDirection().rotateRight());
                } else if(myRC.getTeamResources() >= 3*Chassis.LIGHT.cost) {
					builder.build(Chassis.LIGHT,myRC.getLocation().add(myRC.getDirection()));
                    myRC.yield();
                    builder.build(ComponentType.SIGHT, myRC.getLocation().add(myRC.getDirection()), RobotLevel.ON_GROUND);
                }
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }
            */
        }
	}

    public void runMotor() {
        MapLocation goal = myRC.getLocation().add(Direction.SOUTH, 24);

        while (true) {
            try {
                /*** beginning of main loop ***/

                nav.bugNavigate(goal);

                /*** end of main loop ***/
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }

            myRC.yield();
        }
    }
}
