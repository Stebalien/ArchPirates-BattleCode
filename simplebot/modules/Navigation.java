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

    private int bnav_lastDist,
                bnav_targetDist;
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
            return (bnav_lastDest == null);

        // Likewise, if the robot is already at its destination,
        // signal finish
        MapLocation loc = robot.getLocation();
        int targetDist = loc.distanceSquaredTo(bnav_lastDest);
        System.out.println("Distance to target: " + targetDist);
        if(targetDist <= bnav_targetDist) {
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
                    bnav_lastDist = targetDist;
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

            if(right && targetDist > bnav_lastDist*2) {
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

            if(scan == test || (motor.canMove(d) && targetDist < bnav_lastDist)) {
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
        setDestination(loc, 0);
    }

    /**
     * Navigates towards the given map location, stopping when the robot is the
     * given distance away.  This restarts the current bug navigation.
     *
     * @param motor the motor object for the robot
     * @param loc the destination location, in absolute coordinates
     * @param dist the distance away from the location you'd like to stop
     *
     * @see simplebot.RobotPlayer#navigate(MovementController)
     */
    public void setDestination(MapLocation loc, double dist) {
        // restart if this is a new destination
        if(!loc.equals(bnav_lastDest)) {
            bnav_lastDist = 0;
            bnav_targetDist = (int)(dist*dist);
            bnav_lastDest = loc;
        }
    }

    /**
     * Turn to the given location
     *
     * @param d direction to turn
     */
    public void setDirection(Direction d) throws GameActionException {
        if(motor.isActive())
            return;

        motor.setDirection(d);
    }

    /**
     * Rotate the given direction
     *
     * @param right turn right if true, left if false
     * @param mag number of times to turn
     */
    public void rotate(boolean right, int mag) throws GameActionException {
        if(motor.isActive())
            return;

        Direction d = robot.getDirection();
        for(int i = 0; i < mag; i++)
            if(right)
                d = d.rotateRight();
            else
                d = d.rotateLeft();

        motor.setDirection(d);
    }

    /**
     * Move the robot forward or backward
     *
     * @param forward move forward if true, backward otherwise
     */
    public void move(boolean forward) throws GameActionException {
        if(motor.isActive())
            return;

        if(forward) {
            if(motor.canMove(robot.getDirection())) {
                motor.moveForward();
            }
        } else {
            if(motor.canMove(robot.getDirection().opposite())) {
                motor.moveBackward();
            }
        }
    }

    /**
     * Tests if the robot can move forward, wraps around MovementController
     *
     * @return true if the robot can move forward, false otherwise
     */
    public boolean canMoveForward() {
        return motor.canMove(robot.getDirection());
    }

    /**
     * Tests if the robot can move backward, wraps around MovementController
     *
     * @return true if the robot can move backward, false otherwise
     */
    public boolean canMoveBackward() {
        return motor.canMove(robot.getDirection().opposite());
    }
}
