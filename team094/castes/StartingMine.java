package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class StartingMine extends Caste {
    private static enum State {
        OFF,
        SHIELD,
        IDLE,
        BUILD,
        YIELD
    }
    private static final int OFF_ROUNDS = 5;
    private final int RESOURCES;

    private final Builder builder;
    private final MapLocation myLoc; // Buildings can't move.

    private State state;
    private int offCounter;
    private boolean sentry,
                    shields;

    public StartingMine(RobotProperties rp){
        super(rp);

        state = State.OFF;
        builder = new Builder(rp);
        myLoc = myRC.getLocation();

        sentry = false;

        RESOURCES = 500+(10000-Clock.getRoundNum())/2;
    }

    @SuppressWarnings("fallthrough")
    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case SHIELD:
                        shield();
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
    private void shield() throws GameActionException {
        switch(builder.doBuild()) {
            case DONE:
                shields = true;
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
        Robot[] nearby = myRP.sensor.senseNearbyGameObjects(Robot.class);
        for(Robot r: nearby) {
            if(r != null && r.getTeam() == myRP.myTeam) {
                RobotInfo ri = myRP.sensor.senseRobotInfo(r);
                if(!ri.on) {
                    switch(ri.chassis) {
                    case FLYING:
                        if(ri.location.equals(myLoc)) {
                            if(sentry)
                                builder.startBuild(true, 1.2, myLoc, RobotLevel.IN_AIR, ComponentType.ANTENNA, ComponentType.RADAR, ComponentType.SMG);
                            else
                                builder.startBuild(true, 1.2, myLoc, RobotLevel.IN_AIR, ComponentType.SIGHT, ComponentType.CONSTRUCTOR);

                            builder.doBuild();
                            state = State.BUILD;
                            return;
                        }
                        break;
                    case BUILDING:
                        if(myLoc.isAdjacentTo(ri.location) && !myLoc.directionTo(ri.location).isDiagonal() && myRP.sensor.senseObjectAtLocation(ri.location, RobotLevel.MINE) == null) {
                            builder.startBuild(true, 1.2, ri.location, RobotLevel.ON_GROUND, ComponentType.RADAR, ComponentType.SMG, ComponentType.SMG);
                            builder.doBuild();
                            state = State.BUILD;
                            return;
                        }
                        break;
                    default:
                        continue;
                    }
                }
            }
        }

        if(!shields && myRC.getTeamResources() > 200) {
            builder.startBuild(false, 1.1, myLoc, RobotLevel.ON_GROUND, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD);
            state = state.SHIELD;
            return;
        }

        if(++offCounter >= OFF_ROUNDS) {
            state = State.OFF;
        }
    }

    @SuppressWarnings("fallthrough")
    private void build() throws GameActionException {
        switch (builder.doBuild()) {
            case DONE:
                sentry = !sentry;
            case FAIL:
                state = State.OFF;
                break;
            default:
                break;
        }
    }
}
