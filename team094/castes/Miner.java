package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class Miner extends Caste {
    private static enum State {
        OFF,
        IDLE,
        BUILD,
        YIELD
    }
    private static final int MAX_SCOUTS = 10,
                             OFF_ROUNDS = 5;

    private final Builder builder;
    private final MapLocation myLoc; // Buildings can't move.

    private boolean scout;
    private State state;
    private int scouts,
                offCounter;

    public Miner(RobotProperties rp){
        super(rp);

        state = State.OFF;
        builder = new Builder(rp);
        myLoc = myRC.getLocation();

        // Uncomment to produce scouts first
        //scout = true;
    }

    @SuppressWarnings("fallthrough")
    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case OFF:
                        off();
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
                            if(scout)
                                builder.startBuild(true, 1.2, myLoc, RobotLevel.IN_AIR, ComponentType.SIGHT, ComponentType.CONSTRUCTOR);
                            else
                                builder.startBuild(true, 1.2, myLoc, RobotLevel.IN_AIR, ComponentType.ANTENNA, ComponentType.RADAR, ComponentType.SMG);
                            builder.doBuild();
                            state = State.BUILD;
                            return;
                        }
                        break;
                    case BUILDING:
                        if(scouts > 0 && myLoc.isAdjacentTo(ri.location) && !myLoc.directionTo(ri.location).isDiagonal() && myRP.sensor.senseObjectAtLocation(ri.location, RobotLevel.MINE) == null) {
                            builder.startBuild(true, 1.2, ri.location, RobotLevel.ON_GROUND, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.RADAR, ComponentType.SMG, ComponentType.SMG, ComponentType.BLASTER, ComponentType.BLASTER, ComponentType.BLASTER, ComponentType.BLASTER);
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

        if(++offCounter >= OFF_ROUNDS)
            state = State.OFF;
    }

    /* Not used for now */
    private void build_fighter() {
        Direction dir = Direction.NORTH;
        MapLocation loc = null;
        for (int i = 8; --i > 0;) {
            if (myRP.motor.canMove(dir = dir.rotateLeft())) {
                loc = myLoc.add(dir);
                break;
            }
        }
        if (loc == null)
            return;
        switch(builder.startBuild(true, 1.2, loc, Chassis.LIGHT, ComponentType.SMG, ComponentType.ANTENNA, ComponentType.RADAR)) {
            case ACTIVE:
            case WAITING:
                state = State.BUILD;
                break;
            case FAIL:
            default:
                state = State.IDLE;
                break;
        }
    }

    @SuppressWarnings("fallthrough")
    private void build() throws GameActionException {
        switch (builder.doBuild()) {
            case DONE:
                if(scout) {
                    scouts++;
                    scout = false;
                } else if(scouts < MAX_SCOUTS) {
                    scout = true;
                }
            case FAIL:
                state = State.IDLE;
                offCounter = 0;
                break;
            default:
                break;
        }
    }
}
