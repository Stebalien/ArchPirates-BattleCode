package archpirates.castes;

import archpirates.modules.*;
import battlecode.common.*;

public class Miner extends Caste {
    private static enum State {
        IDLE,
        BUILD,
        YIELD
    }
    private static final int MAX_SCOUTS = 5;

    private final Builder builder;
    private final MapLocation myLoc; // Buildings can't move.

    private boolean scout;
    private State state;
    private int scouts;

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
        GameObject obj = myRP.sensor.senseObjectAtLocation(myLoc, RobotLevel.IN_AIR);
        if(obj != null && obj.getTeam() == myRP.myTeam && !myRP.sensor.senseRobotInfo((Robot)obj).on) {
            if(scout)
                builder.startBuild(true, myLoc, RobotLevel.IN_AIR, ComponentType.SIGHT, ComponentType.CONSTRUCTOR);
            else
                builder.startBuild(true, myLoc, RobotLevel.IN_AIR, ComponentType.SHIELD, ComponentType.RADAR, ComponentType.SMG);
            builder.doBuild();
            state = State.BUILD;
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
