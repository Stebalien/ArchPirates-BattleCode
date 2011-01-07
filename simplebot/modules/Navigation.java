package simplebot.modules;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

/**
 * Navigtion encapsulates navigating to a destination on the map using simple
 * pathfinding.
 */
public class Navigation {
    private RobotController robot;
    private MovementController motor;

    private int bnav_lastDist;
    private MapLocation bnav_lastDest;
    private int wfTurned;
    private boolean right;

    /**
     * Instantiates a Navigation object
     *
     * @param robot the robot this navigator controls, used to get current location
     * @parm motor the controlle for the motor of this robot
     */
    public Navigation(RobotController robot, MovementController motor) {
        this.robot = robot;
        this.motor = motor;
    }

    /**
     * Navigates the given motor towards the last given map location using a processor-friendly
     * bug navigation.  Start by moving towards the destination.  When a wall is sensed,
     * begin a wall following algorithm around the obstacle.  Wall following stops when two
     * things happen:
     *  - The robot is facing towards the goal
     *  - The robot is located closer to the goal than when it started
     *
     * If both conditions are met, the robot breaks out of the wall following code and simply
     * continues on towards the destination.  bugNavigate keeps track of 'bnav_lastDist', the distance
     * from the goal since the wall following started (0 if no wall following is occuring), and
     * 'bnav_lastDest', the last destination that navigate was called with.
     *
     * @param motor the motor object for the robot
     * @param sensor the sensor used to view the map
     * @return false if there is a destination that hasn't been reached, true otherwise
     */
    public boolean bugNavigate() {
        // If the motor is cooling down, don't bother navigating
        // Also return if no destination is set
        // Some precomputation might be useful eventually
        if(motor.isActive() || bnav_lastDest == null)
            return true;

        // Likewise, if the robot is already at its destination,
        // signal finish
        MapLocation loc = robot.getLocation();
        if(loc.equals(bnav_lastDest)) {
            bnav_lastDest = null;
            bnav_lastDist = 0;
            return true;
        }

        Direction d = loc.directionTo(bnav_lastDest);
        Direction cur = robot.getDirection();

            try {
        if(bnav_lastDist == 0) {
            // Try navigating towards the goal
            if(d == cur) {
                if(motor.canMove(d)) {
                    motor.moveForward();
                } else {
                    // Hit a wall, begin wall following
                    bnav_lastDist = loc.distanceSquaredTo(bnav_lastDest);
                    motor.setDirection(d.rotateRight().rotateRight());
                    right = true;
                }
            } else {
                motor.setDirection(d);
            }
        } else {
            // Sample, do a right-hand follow, escaping when robot faces target
            // Switches directions if the robot travels too far away from the
            // destination

            int dist = loc.distanceSquaredTo(bnav_lastDest);
            if(right && dist > bnav_lastDist*2) {
                right = false;
                motor.setDirection(cur.opposite());
                return false;
            }


            // scan left to right for open directions:
            Direction scan, test;
            if(right) {
                scan = cur.rotateLeft();
                test = scan.rotateLeft();
            } else {
                scan = cur.rotateRight();
                test = scan.rotateRight();
            }

            while(scan != test) {
                if(motor.canMove(scan))
                    break;
                if(right)
                    scan = scan.rotateRight();
                else
                    scan = scan.rotateLeft();
            }

            if(scan == test || (motor.canMove(d) && dist < bnav_lastDist)) {
                bnav_lastDist = 0;
                scan = d;
            }

            // Movement code, based on goal direction
            // If the open square is forward, move forward, otherwise turn
            if(scan == cur) {
                // Check square leading to destination, if that is free, take it and
                // stop wall following
                if(motor.canMove(cur)) {
                    motor.moveForward();
                    wfTurned = 0;
                }
            } else {
                if(wfTurned > 1) {
                    motor.setDirection(d);
                    bnav_lastDist = 0;
                    wfTurned = 0;
                } else {
                    motor.setDirection(scan);
                    wfTurned++;
                }
            }
        }
            } catch(Exception e) {
                // Do nothing at the moment
            }

        return false;
    }

    /**
     * Navigates towards the given map location, restarting the bug navigation.
     *
     * @param motor the motor object for the robot
     * @param loc the destination location, in absolute coordinates
     *
     * @see simplebot.RobotPlayer#navigate(MovementController)
     */
    public void setDestination(MapLocation loc) {
        // restart if this is a new destination
        if(!loc.equals(bnav_lastDest)) {
            bnav_lastDist = 0;
            bnav_lastDest = loc;
        }
    }
}
