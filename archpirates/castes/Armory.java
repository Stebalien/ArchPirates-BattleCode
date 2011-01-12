package archpirates.castes;

import archpirates.modules.*;
import battlecode.common.*;

public class Armory extends Caste {
    private static enum State {
        START,
        IDLE,
        BUILD,
        YIELD
    }
    private State state;

    private final Builder builder;
    private MapLocation[] locations;
    int locIndex;

    public Armory(RobotProperties rp) {
        super(rp);

        locations = new MapLocation[2]; // We can do this because there will only be one factory, near the start base
        locIndex = -1;

        state = State.START;
        builder = new Builder(rp);
    }

    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case START:
                        start();
                        break;
                    case IDLE:
                        idle();
                        break;
                    case BUILD:
                        build();
                        break;
                    case YIELD:
                    default:
                        yield();
                        break;
                }
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }
            System.out.println(Clock.getBytecodeNum());
            myRC.yield();
        }
    }

    private void start() {
        MapLocation loc = myRC.getLocation();

        Mine[] mines = myRP.sensor.senseNearbyGameObjects(Mine.class);
        for(Mine m: mines) {
            MapLocation mloc = m.getLocation();
            if(loc.isAdjacentTo(mloc))
                locations[++locIndex] = mloc;
        }

        locIndex = 0;
        state = State.IDLE;
    }

    private void idle() throws GameActionException {
        builder.startBuild(false, locations[locIndex], Chassis.FLYING);
        state = state.BUILD;
    }

    private void build() throws GameActionException {
        switch (builder.doBuild()) {
            case DONE:
            case FAIL:
                state = State.IDLE;
                locIndex = locIndex^1;
                break;
            default:
                break;
        }
    }
}
