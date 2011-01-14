package archpirates.castes;

import archpirates.modules.*;
import battlecode.common.*;

public class Miner extends Caste {
    private static enum State {
        IDLE,
        BUILD,
        YIELD
    }
    private static final int MAX_SCOUTS = 3,
                             TIMEOUT = 500;

    private final Builder builder;
    private final MapLocation myLoc; // Buildings can't move.

    private boolean scout;
    private State state;
    private int scouts,
                timer;

    public Miner(RobotProperties rp){
        super(rp);

        state = State.IDLE;
        builder = new Builder(rp);
        myLoc = myRC.getLocation();

        scout = true;
    }

    public void SM() {
        while(true) {
            try {
                switch(state) {
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

    private void idle() throws GameActionException {
        if(scouts == 0 && (++timer) >= TIMEOUT) {
            timer = 0;
            myRC.turnOff();
        }

        Robot[] nearby = myRP.sensor.senseNearbyGameObjects(Robot.class);
        for(Robot r: nearby) {
            if(r != null && r.getTeam() == myRP.myTeam) {
                RobotInfo ri = myRP.sensor.senseRobotInfo(r);
                if(!ri.on) {
                    switch(ri.chassis) {
                    case FLYING:
                        if(ri.location.equals(myLoc)) {
                            if(scout)
                                builder.startBuild(true, myLoc, RobotLevel.IN_AIR, ComponentType.SIGHT, ComponentType.CONSTRUCTOR);
                            else
                                builder.startBuild(true, myLoc, RobotLevel.IN_AIR, ComponentType.ANTENNA, ComponentType.RADAR, ComponentType.SMG);
                            builder.doBuild();
                            state = State.BUILD;
                            return;
                        }
                        break;
                    case BUILDING:
                        if(scouts > 0 && myLoc.isAdjacentTo(ri.location) && !myLoc.directionTo(ri.location).isDiagonal() && myRP.sensor.senseObjectAtLocation(ri.location, RobotLevel.MINE) == null) {
                            builder.startBuild(true, ri.location, RobotLevel.ON_GROUND, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.RADAR, ComponentType.SMG, ComponentType.SMG, ComponentType.BLASTER, ComponentType.BLASTER, ComponentType.BLASTER, ComponentType.BLASTER);
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
    }

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
        switch(builder.startBuild(true, loc, Chassis.LIGHT, ComponentType.SMG, ComponentType.ANTENNA, ComponentType.RADAR)) {
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
                break;
            default:
                break;
        }
    }
}
