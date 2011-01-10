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

    public Builder(RobotProperties properties) {
        builder = properties.builder;
        motor = properties.motor;
        myRC = properties.myRC;
    }

    public TaskState startBuild(Chassis chassis, ComponentType... components) {
        return startBuild(chassis, null, components);
    }
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
                this.location = myRC.getLocation().add(direction);
                return direction;
            }
        }
        return null;
    }

    public TaskState doBuild() {
        // Build Chassis
        if (builder.isActive()) return TaskState.WAITING;

        if (p == -1) {
            if (myRC.getTeamResources() < MULT*chassis.cost)
                return TaskState.WAITING;
            if (direction == null) {
                direction = chooseBuildDirection();
                if (direction == null)
                    return TaskState.WAITING;
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

        // Can I build
        if (motor.canMove(direction)) {
            p = -1;
            direction = null;
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
