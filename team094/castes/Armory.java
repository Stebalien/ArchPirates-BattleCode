package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class Armory extends Caste {
    private static enum State {
        START,
        IDLE,
        BUILD,
        YIELD
    }
    private static final int MAX_UNITS = 10;
    private State state;

    private final Builder builder;
    private MapLocation[] locations;
    private int locIndex,
                units;

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

    private void start() throws GameActionException {
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
        Robot r = (Robot)myRP.sensor.senseObjectAtLocation(locations[locIndex], RobotLevel.ON_GROUND);

        if(myRP.sensor.senseObjectAtLocation(locations[locIndex], RobotLevel.IN_AIR) == null &&
           r != null) {
            builder.startBuild(false, locations[locIndex], Chassis.FLYING);
            if(r.getTeam() == myRP.myTeam && !myRP.sensor.senseRobotInfo(r).on)
                myRC.turnOn(locations[locIndex], RobotLevel.ON_GROUND);
            state = state.BUILD;
            build(); // Call build function here to save time.
        }

        locIndex = (locIndex+1+locations.length)%locations.length;
    }

    @SuppressWarnings("fallthrough")
    private void build() throws GameActionException {
        switch (builder.doBuild()) {
            case DONE:
                units++;
            case FAIL:
                if(units >= MAX_UNITS)
                    state = State.YIELD;
                else
                    state = State.IDLE;
                break;
            default:
                break;
        }
    }
}
