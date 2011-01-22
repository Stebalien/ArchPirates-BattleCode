package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class StartingMine extends Caste {
    private static enum State {
        OFF,
        SETUP,
        IDLE,
        BUILD,
        YIELD
    }
    private static final int OFF_ROUNDS = 5;

    private final Builder builder;
    private final MapLocation myLoc; // Buildings can't move.

    private State state;
    private int offCounter;

    public StartingMine(RobotProperties rp){
        super(rp);

        state = State.SETUP;
        builder = new Builder(rp);
        myLoc = myRC.getLocation();

        builder.startBuild(false, 1.1, myLoc, RobotLevel.ON_GROUND, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD);
    }

    @SuppressWarnings("fallthrough")
    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case SETUP:
                        setup();
                        break;
                    case OFF:
                        off();
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

    @SuppressWarnings("fallthrough")
    private void setup() throws GameActionException {
        switch(builder.doBuild()) {
            case DONE:
            case FAIL:
                state = State.IDLE;
            default:
            break;
        }
    }

    private void off() {
        myRC.turnOff();
        offCounter = 0;
        state = State.IDLE;
    }

    private void idle() throws GameActionException {
        Robot r = (Robot)myRP.sensor.senseObjectAtLocation(myLoc, RobotLevel.IN_AIR);
        if(r != null && r.getTeam() == myRP.myTeam && !myRP.sensor.senseRobotInfo(r).on) {
            builder.startBuild(true, 1.2, myLoc, RobotLevel.IN_AIR, ComponentType.SIGHT, ComponentType.CONSTRUCTOR);
            builder.doBuild();
            state = State.BUILD;
        } else if(++offCounter >= OFF_ROUNDS) {
            state = State.OFF;
        }
    }

    @SuppressWarnings("fallthrough")
    private void build() throws GameActionException {
        switch (builder.doBuild()) {
            case DONE:
            case FAIL:
                state = State.OFF;
                break;
            default:
                break;
        }
    }
}
