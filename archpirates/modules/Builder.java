package archpirates.modules;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class Builder {
    private static int MULT = 2; // We need at least MULT * RESOURCES to build.

    private final BuilderController builder;
    private final MovementController motor;
    private final RobotController myRC;

    // Cache
    private MapLocation location;
    private RobotLevel level;
    private Chassis chassis;
    private ComponentType [] components;
    private Direction direction;

    private int p = -1; //progress

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
        motor = rp.motor;
        myRC = rp.myRC;
    }

    /**
     * Start a build in the first free direction.
     *
     * Note, this method will NOT check if you can actually build said chassis/components.
     *
     * @param chassis The chassis that will be built.
     * @param components A list of components (if any) that will be built on this chassis.
     *
     * @return TaskState.WAITING
     */
    public TaskState startBuild(Chassis chassis, ComponentType... components) {
        return startBuild(chassis, null, components);
    }

    /**
     * Start a build in the specified direction.
     *
     * Note, this method will NOT check if you can actually build said chassis/components.
     *
     * @param chassis The chassis that will be built.
     * @param direction The direction in which the robot will be built.
     * @param components A list of components (if any) that will be built on this chassis.
     *
     * @return TaskState.WAITING
     */
    public TaskState startBuild(Chassis chassis, Direction direction, ComponentType... components) {
        this.chassis = chassis;
        this.direction = direction;
        this.level = chassis.level;
        this.components = components;
        this.p = -1;
        if (direction != null)
            this.location = myRC.getLocation().add(direction);
        return TaskState.WAITING;
    }

    private Direction chooseBuildDirection() {
        Direction direction = Direction.NORTH;
        for (int i = 0; i < 8; i++) {
            if (motor.canMove(direction = direction.rotateRight())) {
                return direction;
            }
        }
        return null;
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
    public TaskState doBuild() {
        // Build Chassis
        if (builder.isActive()) return TaskState.ACTIVE;

        if (p == -1) {
            if (myRC.getTeamResources() < MULT*chassis.cost)
                return TaskState.WAITING;
            if (direction == null) {
                direction = chooseBuildDirection();
                if (direction == null)
                    return TaskState.WAITING;
                else
                    this.location = myRC.getLocation().add(direction);
            }
            if (motor.canMove(direction)) {
                try {
                    builder.build(chassis, location);
                    p++;
                    return TaskState.ACTIVE;
                } catch (Exception e) {
                    System.out.println("caught exception:");
                    e.printStackTrace();
                    p = -1;
                    direction = null;
                    return TaskState.FAIL;
                }
            } else {
                return TaskState.FAIL;
            }
        }

        // Only build if we can
        if (this.location == null || this.direction == null || motor.canMove(direction)) {
            p = -1;
            direction = null;
            location = null;
            return TaskState.FAIL;
        }

        // Build Components if we have the money.
        if (myRC.getTeamResources() < MULT*components[p].cost)
            return TaskState.WAITING;
        else {
            try {
                builder.build(components[p], location, level);
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
                p = -1;
                direction = null;
                return TaskState.FAIL;
            }
        }

        // Complete build or return in progress.
        if (++p == components.length) {
            try {
                myRC.turnOn(location, level);
                return TaskState.DONE;
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
                p = -1;
                direction = null;
                return TaskState.FAIL;
            }
        } else {
            return TaskState.ACTIVE;
        }
    }
}
