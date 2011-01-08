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

    private boolean isCached(ComponentController component) {
        if (round != (round = Clock.getRoundNum())) { 
            if (robotInfo != null 
                && chassis == robotInfo.chassis 
                && sensor.canSenseObject(robotInfo.robot))
            {
                try {
                    robotInfo = sensor.senseRobotInfo(robotInfo.robot);
                    if (component.withinRange(robotInfo.location))
                        return true;
                } catch (GameActionException e) {}
            }
            robots = sensor.senseNearbyGameObjects(Robot.class);
        } else if (robotInfo != null
            && chassis == robotInfo.chassis
            && component.withinRange(robotInfo.location))
        {
            return true;
        }
        robotInfo = null;
        return false;
    }

    /**
     * Find the first robot.
     *
     * @param component The component that will be firing.
     * @param chassis The chassis to target.
     *
     * @return The RobotInfo of the first robot.
     */
    public RobotInfo getFirst(ComponentController component, Chassis chassis) {
        this.chassis = chassis;
        return getFirst(component);
    }
    /**
     * Find the first robot.
     *
     * @param component The component that will be firing.
     *
     * @return The RobotInfo of the first robot.
     */
    public RobotInfo getFirst(ComponentController component) {

        if (isCached(component)) return robotInfo;

        // Find one that we want
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
            } catch (GameActionException e) {}
        }
        robotInfo = null;
        return null;
    }

    /**
     * Find the nearest robot (SLOW/NO CACHE).
     *
     * @param range The max range.
     * @param cycles The max number of robots to loop over.
     *
     * @return The location of the nearest target.
     */
    public RobotInfo getNearest(int range, int cycles) {
        // Setup
        
        int nearDistSq = range*range;

        MapLocation myLoc = myRC.getLocation();

        //TODO: This should be a private method.
        int curRound = Clock.getRoundNum();
        if (curRound != round) { 
            robotInfo = null;
            robots = sensor.senseNearbyGameObjects(Robot.class);
            round = curRound;
        } else {
            // Return the cached robotInfo if within range or update cache and continue.
            if (myLoc.distanceSquaredTo(robotInfo.location) <= nearDistSq) {
                return robotInfo;
            } else {
                robotInfo = null;
                robots = sensor.senseNearbyGameObjects(Robot.class);
            }
        }

        // Find the one that we want
        for (int i = 0; i < ((cycles < robots.length) ? cycles : robots.length); i++) {
            if (team != robots[i].getTeam())
                continue;
            try {
                RobotInfo tempRobotInfo = sensor.senseRobotInfo(robots[i]);
                int distSq;
                if (tempRobotInfo.chassis == chassis && (distSq = myLoc.distanceSquaredTo(tempRobotInfo.location)) <= nearDistSq) {
                    robotInfo = tempRobotInfo;
                    nearDistSq = distSq;
                }
            } catch (GameActionException e) {}
        }
        return robotInfo;
    }
}

