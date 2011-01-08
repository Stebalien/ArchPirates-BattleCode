package archpirates;

import archpirates.modules.RobotProperties;
import archpirates.modules.Attack;
import archpirates.modules.Targeter;
import archpirates.modules.Builder;
import archpirates.modules.TaskState;
import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class RobotPlayer implements Runnable {

	private final RobotController myRC;
    private RobotProperties properties;

    public RobotPlayer(RobotController rc) {
        myRC = rc;
    }

	public void run() {
        ComponentController [] components = myRC.components();
		System.out.println(java.util.Arrays.toString(components));
		System.out.flush();

        if (components.length > 1) {
            // Am I a default robot.
            properties = new RobotProperties(myRC);
            if(myRC.getChassis()==Chassis.BUILDING)
                runBuilder();
            else
                runConstructor();
        } else {
            // I am not a default robot
            // Shut down until built
            myRC.turnOff();
            properties = new RobotProperties(myRC);
            runKiller();
            // Choose type
        }
	}


	public void runBuilder() {
        Builder builder = new Builder(properties);
        Direction buildDirection = null;
        TaskState state = TaskState.NONE;
        int TOTAL = 3;
        int num_built = 0;

		while (num_built < TOTAL) {
            try {
                switch (state) {
                    case NONE:
                        System.out.println("START");
                        state = builder.startBuild(Chassis.LIGHT, new ComponentType [] {ComponentType.SMG, ComponentType.RADAR});
                        break;
                    case WAITING:
                        System.out.println("WAIT");
                        state = builder.doBuild();
                        break;
                    case ACTIVE:
                        System.out.println("BUILD");
                        state = builder.doBuild();
                        break;
                    case FAIL:
                        System.out.println("FAIL");
                        state = TaskState.NONE;
                        break;
                    case DONE:
                        state = TaskState.NONE;
                        num_built++;
                        System.out.println("DONE");
                        break;
                }
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }
            myRC.yield();
        }
	}

    public void runKiller() {
        Attack attacker = new Attack(properties);
        Targeter targeter = new Targeter(properties, properties.opTeam, Chassis.LIGHT);
        while (true) {
            try {
                //DEBUG
                int start = Clock.getBytecodeNum();
                attacker.autoFire(targeter);
                //DEBUG
                System.out.println("Bytes:" + (Clock.getBytecodeNum()-start));
                myRC.yield();
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }
        }
    }
    public void runConstructor() {
        MovementController motor = properties.motor;
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
