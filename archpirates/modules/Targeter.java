package archpirates.modules;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
public class Targeter {
    private final SensorController sensor;
	private final RobotController myRC;
	private final Team team;
    private final RobotProperties myRP;

    // cache
    private Robot [] robots;
    private int round = -1;
    private RobotInfo robotInfo;

    private int chassisMask;
    
    /**
     * Target a robot on the opposing team.
     *
     * @param rp The properties object describing the current robot.
     * @param chassis A list of chassis to target.
     */
    public Targeter(RobotProperties rp, Chassis... chassis) {
        this(rp, rp.opTeam, chassis);
    }

    /**
     * Target a robot.
     *
     * @param rp The properties object describing the current robot.
     * @param team The team that the robot is on.
     * @param chassis A list of chassis to target.
     */
    public Targeter(RobotProperties rp, Team team, Chassis... chassis) {
        this.myRP = rp;
        this.sensor = rp.sensor;
        this.team = team;
        this.myRC = rp.myRC;
        setChassis(chassis);
    }
    

    /**
     * Set the chassis that this targeter targets.
     * 
     * @param chassis A list of chassis to target.
     */
    public void setChassis(Chassis... chassis) {
        this.chassisMask = 0;
        if (chassis != null) {
            for (Chassis c: chassis) {
                this.chassisMask |= (1 << c.ordinal());
            }
            if (robotInfo != null && (robotInfo.chassis.ordinal() & this.chassisMask) == 0)
                robotInfo = null;
        }
    }


    /**
     * Updates the cache and checks the range of the robot against the range of component.
     *
     * @param component The component against which the range will be checked.
     *
     * @return True if the cache is valid.
     */
    private boolean updateCache(ComponentController component) throws GameActionException {
        if (round != (round = Clock.getRoundNum())) { 
            if (robotInfo != null && sensor.canSenseObject(robotInfo.robot))
            {
                robotInfo = sensor.senseRobotInfo(robotInfo.robot);
                if (component.withinRange(robotInfo.location))
                    return true;
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
     *
     * @return The RobotInfo of the first robot.
     */
    public RobotInfo targetRobot(ComponentController component) throws GameActionException {

        // Check the cache
        if (updateCache(component)) return robotInfo;

        // Find a new target
        for (Robot r : robots) {
            if (team != r.getTeam())
                continue;
            robotInfo = sensor.senseRobotInfo(r);
            if ((chassisMask == 0 || (chassisMask & robotInfo.chassis.ordinal()) != 0)
                && component.withinRange(robotInfo.location))
            {
                return robotInfo;
            }
        }
        robotInfo = null;
        return null;
    }

    /**
     * Updates the cache.
     *
     * @return True if the cache is valid.
     */
    private boolean updateCache() throws GameActionException {
        if (round != (round = Clock.getRoundNum())) { 
            if (robotInfo != null 
                && sensor.canSenseObject(robotInfo.robot))
            {
                robotInfo = sensor.senseRobotInfo(robotInfo.robot);
                return true;
            }
            robots = sensor.senseNearbyGameObjects(Robot.class);
        } else if (robotInfo != null)
            return true;
        robotInfo = null;
        return false;
    }

    /**
     * Targets and follows the first robot it sees.
     *
     * @param component The component that will be firing.
     */
    public RobotInfo chaseRobot(ComponentController component, Navigation nav) throws GameActionException {
        boolean navigate = nav.bugNavigate();
        // Check ctrueache
        if (updateCache()) {
            nav.setDestination(robotInfo.location, 2);
            if (component.withinRange(robotInfo.location))
                return robotInfo;
            else {
                if (navigate)
                    myRP.motor.setDirection(
                            myRC.getLocation().directionTo(robotInfo.location)
                            );
                return null;
            }
        }

        // Find a new robot.
        for (Robot r : robots) {
            if (team != r.getTeam())
                continue;
            robotInfo = sensor.senseRobotInfo(r);
            if ((chassisMask == 0 || (chassisMask & robotInfo.chassis.ordinal()) != 0)) {
                nav.setDestination(robotInfo.location, 2);
                if (component.withinRange(robotInfo.location))
                    return robotInfo;
                else {
                    if (navigate)
                        myRP.motor.setDirection(
                                myRC.getLocation().directionTo(robotInfo.location)
                                );
                    return null;
                }
            }
        }

        nav.rotate(true, (int)(component.type().angle/45));

        robotInfo = null;
        return null;
    }
}

