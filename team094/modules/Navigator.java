package team094.modules;

import team094.modules.RobotProperties;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

/**
 * Navigtion encapsulates navigating to a destination on the map using simple
 * pathfinding.
 */
public class Navigator {
    private final RobotController myRC;
    private final RobotProperties myRP;
    private final MovementController motor;

    private int bnav_lastDist,
                bnav_targetDist;
    private MapLocation bnav_lastDest;
    private Direction bnav_lastDir;
    private int wfTurned;
    private boolean right;

    /**
     * Instantiates a Navigation object
     *
     * @param robot the robot this navigator controls, used to get current location
     * @parm motor the controlle for the motor of this robot
     */
    public Navigator(RobotProperties rp) {
        this.myRC = rp.myRC;
        this.myRP = rp;
        this.motor = rp.motor;
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
     * @param faceDest if true, robot will turn to face the destination after reaching it, setting a
     *                 final facing overrides
     * @return false if there is a destination that hasn't been reached, true otherwise
     */
    public boolean bugNavigate(boolean faceDest) throws GameActionException {
        // If the motor is cooling down, don't bother navigating
        // Also return if no destination is set
        // Some precomputation might be useful eventually
        if(motor.isActive() || bnav_lastDest == null)
            return (bnav_lastDest == null);

        // Likewise, if the robot is already at its destination,
        // signal finish
        MapLocation loc = myRC.getLocation();
        Direction cur = myRC.getDirection();
        int targetDist = loc.distanceSquaredTo(bnav_lastDest);
        if(targetDist <= bnav_targetDist) {
            if(bnav_lastDir != null) {
                if(bnav_lastDir != cur) {
                    motor.setDirection(bnav_lastDir);
                    return false;
                }
            } else if(faceDest) {
                Direction destDir = loc.directionTo(bnav_lastDest);
                if(destDir != Direction.NONE && destDir != Direction.OMNI && cur != destDir) {
                    motor.setDirection(destDir);
                    return false;
                }
            }
            bnav_lastDest = null;
            bnav_lastDir = null;
            bnav_lastDist = 0;
            return true;
        }

        Direction d = loc.directionTo(bnav_lastDest);

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
            // Do a wall follow around the obstacle, escaping when robot faces target
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

            // If the way to the destination is open, go that way
            if(motor.canMove(d) && targetDist <= bnav_lastDist) {
                bnav_lastDist = 0;
                scan = d;
            }

            // Movement code, based on goal direction
            // If the open square is forward, move forward, otherwise turn
            if(scan == cur) {
                motor.moveForward();
                wfTurned = 0;
            } else {
                if(wfTurned > 1) {
                    // May have lost a wall, reset destination and stop wall following
                    // (prevents getting stuck in a turning loop)
                    motor.setDirection(d);
                    bnav_lastDist = 0;
                    wfTurned = 0;
                } else {
                    motor.setDirection(scan);
                    wfTurned++;
                }
            }
        }

        return false;
    }

    /**
     * Navigates towards the given map location, restarting the bug navigation.
     *
     * @param motor the motor object for the robot
     * @param loc the destination location, in absolute coordinates
     *
     * @see simplebot.RobotPlayer#bugNavigate(MovementController)
     */
    public void setDestination(MapLocation loc) {
        setDestination(loc, null, 0);
    }

    /**
     * Navigates towards the given map location, stopping when the robot is the
     * given distance away.  This restarts the current bug navigation.
     *
     * @param motor the motor object for the robot
     * @param loc the destination location, in absolute coordinates
     * @param dist the distance away from the location you'd like to stop
     *
     * @see simplebot.RobotPlayer#bugNavigate(MovementController)
     */
    public void setDestination(MapLocation loc, double dist) {
        setDestination(loc, null, dist);
    }

    /**
     * Navigates towards the given map location, stopping when the robot is the
     * given distance away.  Ends by facing the given direction.  This restarts
     * the current bug navigation.
     *
     * @param motor the motor object for the robot
     * @param loc the destination location, in absolute coordinates
     * @parma dir the destination direction, if desired
     * @param dist the distance away from the location you'd like to stop
     *
     * @see simplebot.RobotPlayer#bugNavigate(MovementController)
     */
    public void setDestination(MapLocation loc, Direction dir, double dist) {
        // restart if this is a new destination
        if(bnav_lastDest == null || !loc.equals(bnav_lastDest)) {
            if(bnav_lastDist != 0)
                bnav_lastDist = myRC.getLocation().distanceSquaredTo(loc);
            bnav_targetDist = (int)(dist*dist);
            bnav_lastDest = loc;
        }

        bnav_lastDir = dir;
    }

    /**
     * Performs the steps necessary to navigate using a wall-following algorithm.
     * left or right is specified by a boolean value.  Not compatible with bugNavigate
     *
     * @param right if true, follow a right-hand wall, else follow the left
     */
    public void wallFollow(boolean right) throws GameActionException {
        if(motor.isActive())
            return;

        // scan left to right for open directions:
        Direction cur = myRC.getDirection();
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

        // If the open square is forward, move forward, otherwise turn
        if(scan == cur) {
            motor.moveForward();
            wfTurned = 0;
        } else {
            if(wfTurned > 4) {
                // May have lost a wall, move forward until a wall is found, then resume
                if(motor.canMove(cur)) {
                    motor.moveForward();
                } else {
                    wfTurned = 0;
                    motor.setDirection(scan);
                }
            } else {
                motor.setDirection(scan);
                wfTurned++;
            }
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

        Direction d = myRC.getDirection();
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
            if(motor.canMove(myRC.getDirection())) {
                motor.moveForward();
            }
        } else {
            if(motor.canMove(myRC.getDirection().opposite())) {
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
        return motor.canMove(myRC.getDirection());
    }

    /**
     * Tests if the robot can move backward, wraps around MovementController
     *
     * @return true if the robot can move backward, false otherwise
     */
    public boolean canMoveBackward() {
        return motor.canMove(myRC.getDirection().opposite());
    }

    /**
     * Tests if the robot can move in the given direction, wraps around
     * MovementController
     *
     * @param dir Direction to test movement
     * @return true if the robot can move in the given direction, false otherwise
     */
    public boolean canMove(Direction d) {
        return motor.canMove(d);
    }
}
