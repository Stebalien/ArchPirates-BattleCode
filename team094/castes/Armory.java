package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class Armory extends Caste {
    private static enum State {
        START,
        IDLE,
        BUILD,
        DELAY,
        YIELD
    }
    private static final int MAX_UNITS = 40,
                             DELAY = 110;
    private State state;

    private final Builder builder;
    private final Attacker attacker;
    private SensorController mySensor;
    private MapLocation[] locations;
    private int locIndex,
                units,
                cooldown;

    public Armory(RobotProperties rp) {
        super(rp);

        locIndex = -1;

        state = State.START;
        builder = new Builder(rp);
        attacker = new Attacker(rp, false);

        for(ComponentController c: myRC.components()) {
            if(c.type() == ComponentType.BUILDING_SENSOR) {
                mySensor = (SensorController)c;
                break;
            }
        }
    }

    public void SM() {
        MapLocation l;

        while(true) {
            try {
                if((l = attacker.autoFire()) != null)
                    nav.setDirection(myRC.getLocation().directionTo(l));
                else
                    nav.setDirection(myRC.getDirection().opposite());

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

    private void start() throws GameActionException {
        MapLocation loc = myRC.getLocation();

        Mine[] mines = mySensor.senseNearbyGameObjects(Mine.class);
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
        if(mySensor.senseObjectAtLocation(locations[locIndex], RobotLevel.IN_AIR) == null) {
            builder.startBuild(false, 2, locations[locIndex], Chassis.FLYING);
            state = state.BUILD;
            build(); // Call build function here to save time.
        } else {
            locIndex = (locIndex+1+locations.length)%locations.length;
        }
    }

    private void build() throws GameActionException {
        switch (builder.doBuild()) {
            case DONE:
                units++;
                state = state.DELAY;
                cooldown = DELAY;
                locIndex = (locIndex+1+locations.length)%locations.length;
                break;
            case FAIL:
                locIndex = (locIndex+1+locations.length)%locations.length;
                state = State.IDLE;
                break;
            case ACTIVE:
                Robot r = (Robot)mySensor.senseObjectAtLocation(locations[locIndex], RobotLevel.ON_GROUND);
                if(r != null && r.getTeam() == myRP.myTeam && !mySensor.senseRobotInfo(r).on)
                    myRC.turnOn(locations[locIndex], RobotLevel.ON_GROUND);
                break;
            default:
                break;
        }
    }

    private void delay() {
        if(--cooldown <= 0) {
            if(units >= MAX_UNITS)
                state = State.YIELD;
            else
                state = State.IDLE;
        }
    }
}
