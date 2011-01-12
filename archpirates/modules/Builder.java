package archpirates.modules;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class Builder {
    private static double MULT = 1.2; // We need at least MULT * RESOURCES to build.

    private final BuilderController builder;
    private final SensorController sensor;
    private final MovementController motor;
    private final RobotController myRC;
    private final int periph; // Number of squares that we can see in our peripheral vision.
    private final RobotLevel myLevel;

    // Cache
    private MapLocation location;
    private RobotLevel level;
    private Chassis chassis;
    private ComponentType [] components;
    private boolean turn_on;

    private int p = -2; //progress

    /**
     * Builds buildings and robots.
     *
     * Call startBuild to begin building and doBuild every round until it returns TaskState.DONE
     * or fails with TaskState.FAIL. You can also count TaskState.WAITING to impliment a timeout.
     *
     * @param rp Your robot properties.
     */
    public Builder(RobotProperties rp) {
        builder = rp.builder;
        sensor = rp.sensor;
        motor = rp.motor;
        myRC = rp.myRC;
        periph = (int)((sensor.type().angle / 90));
        myLevel = myRC.getRobot().getRobotLevel();
    }

    /**
     * Start a build in the first free location.
     *
     * Note, this method will NOT check if you can actually build said chassis/components.
     *
     * @param turn_on turns the robot on if true.
     * @param chassis The chassis that will be built.
     * @param components A list of components (if any) that will be built on this chassis.
     *
     * @return TaskState.WAITING
     */
    public TaskState startBuild(boolean turn_on, Chassis chassis, ComponentType... components) {
        return startBuild(turn_on, null, chassis, components);
    }

    /**
     * Start a build in the specified location.
     *
     * Note, this method will NOT check if you can actually build said chassis/components.
     *
     * @param turn_on turns the robot on if true.
     * @param location The location where the robot will be built.
     * @param chassis The chassis that will be built.
     * @param components A list of components (if any) that will be built on this chassis.
     *
     * @return TaskState.WAITING
     */
    public TaskState startBuild(boolean turn_on, MapLocation location, Chassis chassis, ComponentType... components) {
        this.chassis = chassis;
        this.level = chassis.level;
        this.components = components;
        this.p = -1;
        this.location = location;
        this.turn_on = turn_on;
        return TaskState.WAITING;
    }
    /**
     * Build components at a specified location and level.
     *
     * Note, this method will NOT check if you can actually build said components.
     *
     * @param turn_on turns the robot on if true.
     * @param location The location where the components will be built.
     * @param level The level at which the components will be built.
     * @param components A list of components (if any) that will be built on this chassis.
     *
     * @return TaskState.WAITING
     */
    public TaskState startBuild(boolean turn_on, MapLocation location, RobotLevel level, ComponentType... components) {
        this.p = 0;
        this.chassis = null;
        this.level = level;
        this.components = components;
        this.location = location;
        this.turn_on = turn_on;
        return TaskState.WAITING;
    }

    /**
     * Continue building until DONE or FAIL.
     *
     * @return The state of the build.
     *   * WAITING when waiting for space to clear or for flux.
     *   * ACTIVE when building or the builder is Active.
     *   * DONE when the build is done and the robot has turned on.
     *   * FAIL when the build fails.
     */
    public TaskState doBuild() throws GameActionException {
        // Build Chassis
        if (builder.isActive()) return TaskState.ACTIVE;

        switch(p) {
            case -2:
                return TaskState.FAIL;
            case -1:
                if (chassis == null) {
                    p = -2;
                    return TaskState.FAIL;
                }
                if (myRC.getTeamResources() < MULT*chassis.cost)
                    return TaskState.WAITING;
                if (location == null) {
                    MapLocation my_loc = myRC.getLocation();
                    Direction my_dir = myRC.getDirection(); // Also becomes r_dir

                    if (level == myLevel) {
                        for (int i = 8; i-- > 0;) {
                            if (motor.canMove(my_dir)) {
                                location = my_loc.add(my_dir);
                                break;
                            }
                            my_dir = my_dir.rotateRight();
                        }
                    } else {
                        Direction l_dir = my_dir;
                        MapLocation tmp_loc = my_loc.add(my_dir);

                        if (sensor.senseObjectAtLocation(tmp_loc, level) == null) {
                            location = tmp_loc;
                        }

                        for (int i = periph; i-- > 0;) {
                            my_dir = my_dir.rotateRight();
                            tmp_loc = my_loc.add(my_dir);
                            if (sensor.senseObjectAtLocation(tmp_loc, level) == null) {
                                location = tmp_loc;
                                break;
                            }
                            l_dir = l_dir.rotateLeft();
                            tmp_loc = my_loc.add(l_dir);
                            if (sensor.senseObjectAtLocation(tmp_loc, level) == null) {
                                location = tmp_loc;
                                break;
                            }
                        }
                    } 
                    if (location == null)
                        return TaskState.WAITING;
                }
                // If you specify a location, you better be able to build in it.
                // I don't check.
                try {
                    builder.build(chassis, location);
                } catch (Exception e) {
                    System.out.println("caught exception:");
                    e.printStackTrace();
                    p = -2;
                    return TaskState.FAIL;
                }
                break;
            default:
                // I don't check if I can still build because the exception doesn't really cost that much and checking this is a PAIN.
                if (myRC.getTeamResources() < MULT*components[p].cost)
                    return TaskState.WAITING;
                else {
                    try {
                        builder.build(components[p], location, level);
                    } catch (Exception e) {
                        System.out.println("caught exception:");
                        e.printStackTrace();
                        p = -2;
                        return TaskState.FAIL;
                    }
                }
        }

        // Complete build or return in progress.
        if (++p == components.length) {
            try {
                if (turn_on)
                    myRC.turnOn(location, level);
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
                return TaskState.FAIL;
            } finally {
                p = -2;
            }
            return TaskState.DONE;
        } else {
            return TaskState.ACTIVE;
        }
    }
}
