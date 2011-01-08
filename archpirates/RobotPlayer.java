package archpirates;

import archpirates.modules.RobotProperties;
import archpirates.modules.Attack;
import archpirates.modules.Targeter;
import archpirates.modules.Builder;
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
        MovementController motor = properties.motor;
        BuilderController builder = properties.builder;
        MapLocation myLoc = myRC.getLocation();
		while (true) {
            try {
                // Only build when I can
				while (!motor.canMove(myRC.getDirection())) {
                    motor.setDirection(myRC.getDirection().rotateRight());
                    myRC.yield();
                }
                MapLocation loc = myLoc.add(myRC.getDirection());
                //Build Chassis
                while (myRC.getTeamResources() < 2*Chassis.LIGHT.cost) myRC.yield();
                builder.build(Chassis.LIGHT, loc);
                myRC.yield();

                while (myRC.getTeamResources() < 2*ComponentType.SMG.cost) myRC.yield();
                builder.build(ComponentType.SMG, loc, RobotLevel.ON_GROUND);
                myRC.yield();

                while (myRC.getTeamResources() < 2*ComponentType.RADAR.cost) myRC.yield();
                builder.build(ComponentType.RADAR, loc, RobotLevel.ON_GROUND);
                //myRC.yield();

                // Turn it on
                myRC.turnOn(loc, RobotLevel.ON_GROUND);
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }
            myRC.yield();
        }
	}

    public void runKiller() {
        Attack attacker = new Attack(properties);
        Targeter targeter = new Targeter(properties, properties.myTeam, Chassis.LIGHT);
        while (true) {
            try {
                //DEBUG
                int start = Clock.getBytecodeNum();
                attacker.fireGunsOn(targeter);
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
