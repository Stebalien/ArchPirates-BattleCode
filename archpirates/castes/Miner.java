package archpirates.castes;

import archpirates.modules.*;
import battlecode.common.*;

public class Miner extends Caste {
    private static enum State {
        START,
        IDLE,
        BUILD,
        YIELD
    }
    private State state;

    private final Builder builder;
    private MapLocation loc;

    boolean scout;

    public Miner(RobotProperties rp){
        super(rp);

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
        loc = myRC.getLocation();
        state = State.IDLE;
    }

    private void idle() throws GameActionException {
        GameObject obj = myRP.sensor.senseObjectAtLocation(loc, RobotLevel.IN_AIR);
        if(obj != null && obj.getTeam() == myRP.myTeam && !myRP.sensor.senseRobotInfo((Robot)obj).on) {
            builder.startBuild(true, loc, RobotLevel.IN_AIR, ComponentType.SIGHT, ComponentType.CONSTRUCTOR);
            builder.doBuild();
            state = State.BUILD;
        }
    }

    private void build() throws GameActionException {
        switch (builder.doBuild()) {
            case DONE:
            case FAIL:
                state = State.IDLE;
                break;
            default:
                break;
        }
    }
}
