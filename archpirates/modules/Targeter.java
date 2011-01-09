package archpirates.modules;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
public class Targeter {
    private final SensorController sensor;
	private final RobotController myRC;
	private final Team team;

    // cache
    private Robot [] robots;
    private int round = -1;
    private RobotInfo robotInfo;

    private Chassis chassis;
    
    /**
     * Target a robot.
     *
     * @param properties The properties object describing the current robot.
     */
    public Targeter(RobotProperties properties) {
        this(properties, properties.opTeam, null);
    }

    /**
     * Target a robot.
     *
     * @param properties The properties object describing the current robot.
     * @param chassis The team that the robot is on.
     */
    public Targeter(RobotProperties properties, Chassis chassis) {
        this(properties, properties.opTeam, chassis);
    }

    /**
     * Target a robot.
     *
     * @param properties The properties object describing the current robot.
     * @param team The team that the robot is on.
     */
    public Targeter(RobotProperties properties, Team team) {
        this(properties, team, null);
    }

    /**
     * Target a robot.
     *
     * @param properties The properties object describing the current robot.
     * @param team The team that the robot is on.
     * @param chassis The chassis to target.
     */
    public Targeter(RobotProperties properties, Team team, Chassis chassis) {
        this.sensor = properties.sensor;
        this.team = team;
        this.myRC = properties.myRC;
        this.chassis = chassis;
    }
    

    /**
     * Set the chassis that this targeter targets.
     * 
     * @param chassis The chassis.
     */
    public void setChassis(Chassis chassis) {
        if (robotInfo != null && robotInfo.chassis != chassis)
            robotInfo = null;
        this.chassis = chassis;
    }

    /**
     * Updates the cache.
     *
     * @return True if the cache is valid.
     */
    private boolean updateCache() {
        if (round != (round = Clock.getRoundNum())) { 
            if (robotInfo != null 
                && sensor.canSenseObject(robotInfo.robot))
            {
                try {
                    robotInfo = sensor.senseRobotInfo(robotInfo.robot);
                    return true;
                } catch (GameActionException e) {}
            }
            robots = sensor.senseNearbyGameObjects(Robot.class);
        } else if (robotInfo != null)
            return true;
        robotInfo = null;
        return false;
    }

    /**
     * Updates the cache and checks the range of the robot against the range of component.
     *
     * @param component The component against which the range will be checked.
     *
     * @return True if the cache is valid.
     */
    private boolean updateCache(ComponentController component) {
        if (round != (round = Clock.getRoundNum())) { 
            if (robotInfo != null && sensor.canSenseObject(robotInfo.robot))
            {
                try {
                    robotInfo = sensor.senseRobotInfo(robotInfo.robot);
                    if (component.withinRange(robotInfo.location))
                        return true;
                } catch (GameActionException e) {}
            }
            robots = sensor.senseNearbyGameObjects(Robot.class);
        } else if (robotInfo != null && component.withinRange(robotInfo.location))
            return true;
        robotInfo = null;
        return false;
    }

    /**
     * Targets the first robot that it finds.
     *
     * @param component The component that will be firing.
     * @param chassis The chassis to target.
     *
     * @return The RobotInfo of the first robot.
     */
    public RobotInfo targetRobot(ComponentController component, Chassis chassis) {
        setChassis(chassis);
        return targetRobot(component);
    }
    /**
     * Targets the first robot that it finds.
     *
     * @param component The component that will be firing.
     *
     * @return The RobotInfo of the first robot.
     */
    public RobotInfo targetRobot(ComponentController component) {

        // Check the cache
        if (updateCache(component)) return robotInfo;

        // Find a new target
        for (Robot r : robots) {
            if (team != r.getTeam())
                continue;
            try {
                robotInfo = sensor.senseRobotInfo(r);
                if ((chassis == null || chassis == robotInfo.chassis)
                    && component.withinRange(robotInfo.location))
                {
                    return robotInfo;
                }
            } catch (GameActionException e) {e.printStackTrace();}
        }
        robotInfo = null;
        return null;
    }

    /**
     * Targets the nearest robot or the previously targeted robot if still in range.
     *
     * @param component The component that will be firing.
     * @param cycles The max number of robots to loop over.
     * @param chassis The chassis to target.
     *
     * @return The RobotInfo of the first robot.
     */
    public RobotInfo targetNearestRobot(ComponentController component, int cycles, Chassis chassis) {
        setChassis(chassis);
        return targetNearestRobot(component, cycles);
    }

    /**
     * Targets the nearest robot or the previously targeted robot if still in range.
     *
     * @param component The component that will be firing.
     * @param cycles The max number of robots to loop over.
     *
     * @return The location of the nearest target.
     */
    public RobotInfo targetNearestRobot(ComponentController component, int cycles) {

        // Check the cache
        if (updateCache(component)) return robotInfo;

        // Setup
        int nearDistSq = 10000; // Very high number
        MapLocation myLoc = myRC.getLocation();

        // Find a new target
        for (int i = 0; i < ((cycles < robots.length) ? cycles : robots.length); i++) {
            if (team != robots[i].getTeam())
                continue;
            try {
                RobotInfo tempRobotInfo = sensor.senseRobotInfo(robots[i]);
                // Only check robot if:
                // 1. The chassis matches
                // 2. The robot is within range - Must check every time because of the angle restriction.
                if ((chassis == null || chassis == tempRobotInfo.chassis) && (component.withinRange(tempRobotInfo.location))) {
                    int distSq = myLoc.distanceSquaredTo(tempRobotInfo.location);
                    if (distSq < nearDistSq) {
                        robotInfo = tempRobotInfo;
                        nearDistSq = distSq;
                    }
                }
            } catch (GameActionException e) {e.printStackTrace();}
        }
        return robotInfo;
    }

    /**
     * Targets and follows the first robot it sees.
     *
     * @param component The component that will be firing.
     * @param chassis The chassis we are targeting
     */
    /*
    public RobotInfo chaseRobot(ComponentController component, Chassis chassis) {
        setChassis(chassis);
        return chaseFirst(component, navigator);
    }
    */

    /**
     * Targets and follows the first robot it sees.
     *
     * @param component The component that will be firing.
     */
    /*
    public RobotInfo chaseRobot(ComponentController component) {
        // Check cache
        if (updateCache()) {
            if (component.withinRange(robotInfo.location))
                return robotInfo;
            else {
                navigator.navigate(robotInfo.location);
                return null;
            }
        }

        // Find a new robot.
        for (Robot r : robots) {
            if (team == r.getTeam())
                continue;
            try {
                robotInfo = sensor.senseRobotInfo(r);
                if (chassis == null || chassis == robotInfo.chassis) {
                    if (component.withinRange(robotInfo.location))
                        return robotInfo;
                    else {
                        navigator.navigate(robotInfo.location);
                        return null;
                    }
                }
            } catch (GameActionException e) {e.printStackTrace();}
            robotInfo = null;
            return null;
        }
    }
    */
}

