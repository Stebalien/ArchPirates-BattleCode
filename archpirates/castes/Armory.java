package archpirates.castes;

import archpirates.modules.*;
import battlecode.common.*;

public class Armory extends Caste {
    private static enum State {
        START,
        IDLE,
        BUILD,
        DELAY,
        YIELD
    }
    private static final int COOLDOWN = 150;
    private State state;

    private final Builder builder;
    private MapLocation[] locations;
    private int locIndex,
                cooldown;

    public Armory(RobotProperties rp) {
        super(rp);

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
                    case DELAY:
                        delay();
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
//            System.out.println(Clock.getBytecodeNum());
            myRC.yield();
        }
    }

    private void start() {
        MapLocation loc = myRC.getLocation();

        Mine[] mines = myRP.sensor.senseNearbyGameObjects(Mine.class);
        int numLoc = 0;
        for(int i = 0; i < mines.length; i++) {
            MapLocation mloc = mines[i].getLocation();
            if(loc.isAdjacentTo(mloc)) {
                numLoc++;
            } else {
               mines[i] = null;
            }
        }

        locations = new MapLocation[numLoc];
        for(Mine m: mines) {
            if(m != null)
                locations[++locIndex] = m.getLocation();
        }

        state = State.IDLE;
    }

    private void idle() throws GameActionException {
        if(myRP.sensor.senseObjectAtLocation(locations[locIndex], RobotLevel.IN_AIR) == null) {
            builder.startBuild(false, locations[locIndex], Chassis.FLYING);
            state = state.BUILD;
        }

        locIndex = (locIndex+1+locations.length)%locations.length;
    }

    private void build() throws GameActionException {
        switch (builder.doBuild()) {
            case DONE:
            case FAIL:
                cooldown = COOLDOWN;
                state = State.DELAY;
                break;
            default:
                break;
        }
    }

    private void delay() {
        if(--cooldown < 1)
            state = State.IDLE;
    }
}
