package team094.modules;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
public class Attacker {
    // {{{ Finals
    private final SensorController sensor;
    private final WeaponController [] guns;
    private final WeaponController [] beams;

	private final RobotController myRC;
    private final RobotProperties myRP;
    // }}}

    // {{{ Settings
    private int chassisMask;
	private Team team;
    // }}}

    // {{{ Cache
    public int rank;
    public int mask;
    private Robot [] robots;
    private double hp;
    private MapLocation runDest;
    private MapLocation lastLoc;
    // }}}

    // {{{ Constructors
    /**
     * Controll weapons and medics and target robots on the opposing team.
     *
     * @param properties The properties of the controlling robot.
     */
    public Attacker(RobotProperties rp, Chassis... chassis) {
        this(rp, rp.opTeam, chassis);
    }

    /**
     * Target a robot.
     *
     * @param rp The properties object describing the current robot.
     * @param team The team that the robot is on.
     * @param chassis A list of chassis to target.
     */
    public Attacker(RobotProperties rp, Team team, Chassis... chassis) {
        this.myRP = rp;
        this.sensor = rp.sensor;
        this.guns = rp.guns;
        this.beams = rp.beams;
        this.team = team;
        this.myRC = rp.myRC;
        this.hp = myRC.getHitpoints();
        setChassis(chassis);
    }
    // }}}

    // {{{ Set Settings
    /**
     * Set the chassis that will be targeted.
     *
     * @param chassis A list of chassis to target.
     */
    public void setChassis(Chassis... chassis) {
        this.chassisMask = 0;
        if (chassis != null) {
            for (Chassis c: chassis) {
                this.chassisMask |= (1 << c.ordinal());
            }
        }
    }

    /**
     * Set the team that will be targeted.
     *
     * @param team the team that will be targeted.
     */
    public void setTeam(Team team) {
        if (this.team != team) {
            this.team = team;
        }
    }
    // }}}


    // {{{ Fire At
    /**
     * Fire everything location.
     */
    //TODO: Clean this up.
    public void fireAt(MapLocation location, RobotLevel level) throws GameActionException {
        fireAt(location, level, guns);
        fireAt(location, level, beams);
    }
    public void fireGunsAt(MapLocation location, RobotLevel level) throws GameActionException {
        fireAt(location, level, guns);
    }
    public void fireBeamsAt(MapLocation location, RobotLevel level) throws GameActionException {
        fireAt(location, level, beams);
    }
    public void fireAt(MapLocation location, RobotLevel level, WeaponController [] weapons) throws GameActionException {
        for (WeaponController weapon : weapons) {
            if ( !weapon.isActive() && weapon.withinRange(location)) {
                try {
                    weapon.attackSquare(location, level);
                } catch(GameActionException e) {}
            }
        }
    }
    // }}}

    // {{{ Auto Fire
    /**
     * Fire all guns on targets.
     *
     * @param targeter The targeter.
     *
     * @return The location of the currently targeted robot.
     */
    public MapLocation autoFire() throws GameActionException {
        RobotInfo robotInfo = null, tmpRobotInfo = null;
        MapLocation myLoc = myRC.getLocation();
        int bit = 0;
        int rank = 0;
        int mask = 0;

        int tmpDistSq, minDistSq = 1000;

        robots = sensor.senseNearbyGameObjects(Robot.class);

        // Find a new target
        for (Robot r : robots) {
            if (team != r.getTeam())
                continue;
            tmpRobotInfo = sensor.senseRobotInfo(r);

            rank += tmpRobotInfo.chassis.cost;
            mask |= (bit = (1 << tmpRobotInfo.chassis.ordinal()));
            if (chassisMask == 0 || (chassisMask & bit) != 0)
            {
                if ((tmpDistSq = myLoc.distanceSquaredTo(tmpRobotInfo.location)) < minDistSq) {
                    minDistSq = tmpDistSq;
                    robotInfo = tmpRobotInfo;
                }
            }
        }
        // Don't reference global varaibles too much.
        this.rank = rank;
        this.mask = mask;



        // Return if nothing is within sensor range but return behind if I am being attacked and can't see my attacker.
        double new_hp = myRC.getHitpoints();
        if (robotInfo == null) {
            if (new_hp < hp) {
                if (runDest == null)
                    runDest = myLoc.add(myRC.getDirection().opposite(), 20);
                hp = new_hp;
                return runDest;
            } else if (lastLoc != null && sensor.canSenseSquare(lastLoc))
                lastLoc = null;
            hp = new_hp;
            return lastLoc;
        }
        runDest = null;
        hp = new_hp;

        for (WeaponController gun : guns) {
            if (gun.isActive())
                continue;
            try {
                if (gun.withinRange(robotInfo.location))
                    gun.attackSquare(robotInfo.location, robotInfo.robot.getRobotLevel());
            } catch(GameActionException e) {e.printStackTrace();}
        }
        return (lastLoc = robotInfo.location);
    }

    /**
     * Fire gun on targets.
     * Should be grouped by type for speed.
     *
     * @param weapon The weapon to fire.
     *
     * @return The location of the currently targeted robot.
     */
    public MapLocation autoFire(WeaponController weapon) throws GameActionException {
        RobotInfo robotInfo = null, tmpRobotInfo = null;
        robots = sensor.senseNearbyGameObjects(Robot.class);
        int bit = 0;
        int rank = 0;
        int mask = 0;

        // Find a new target
        for (Robot r : robots) {
            if (team != r.getTeam())
                continue;
            tmpRobotInfo = sensor.senseRobotInfo(r);

            rank += tmpRobotInfo.chassis.cost;
            mask |= (bit = (1 << tmpRobotInfo.chassis.ordinal()));

            if ((chassisMask == 0 || (chassisMask & bit) != 0))
            {
                robotInfo = tmpRobotInfo;
                if (weapon.withinRange(robotInfo.location)) {
                    break;
                }
            }
        }

        // Don't reference global varaibles too much.
        this.rank = rank;
        this.mask = mask;


        // Return if nothing is within sensor range but return behind if I am being attacked and can't see my attacker.
        double new_hp = myRC.getHitpoints();
        if (robotInfo == null) {
            if (new_hp < hp) {
                if (runDest == null)
                    runDest = myRC.getLocation().add(myRC.getDirection().opposite(), 20);
                hp = new_hp;
                return runDest;
            } else if (lastLoc != null && sensor.canSenseSquare(lastLoc))
                lastLoc = null;
            hp = new_hp;
            return lastLoc;
        }
        runDest = null;
        hp = new_hp;

        if (!weapon.isActive()) {
            try {
                if (weapon.withinRange(robotInfo.location))
                    weapon.attackSquare(robotInfo.location, robotInfo.robot.getRobotLevel());
            } catch(GameActionException e) {e.printStackTrace();}
        }
        return (lastLoc = robotInfo.location);
    }
    // }}}
}

