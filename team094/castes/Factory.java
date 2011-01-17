package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class Factory extends Caste {
    private static enum State {
        INIT,
        BUILD_DISH,
        BUILD_SOLDIERS,
        IDLE,
        YIELD
    }
    private static final int MAX_SOLDIERS = 4,
                             WAKE_TIME = 150;
    private State state;

    private final Builder builder;
    private MapLocation myLoc,
                        soldierLoc;
    private int[] soldierIDS;
    private int soldiers,
                timer;

    public Factory(RobotProperties rp) {
        super(rp);

        state = State.INIT;
        builder = new Builder(rp);
        myLoc = myRC.getLocation();

        soldierIDS = new int[MAX_SOLDIERS];
    }

    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case INIT:
                        init();
                        break;
                    case BUILD_DISH:
                        build_dish();
                        break;
                    case BUILD_SOLDIERS:
                        build_soldiers();
                        break;
                    case IDLE:
                        idle();
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

    private void init() throws GameActionException {
        builder.startBuild(false, myRC.getLocation(), RobotLevel.ON_GROUND, ComponentType.DISH);
        builder.doBuild();
        state = State.BUILD_DISH;
    }

    private void build_dish() throws GameActionException {
        switch(builder.doBuild()) {
            case FAIL:
                builder.startBuild(false, myRC.getLocation(), RobotLevel.ON_GROUND, ComponentType.DISH);
                break;
            case DONE:
                state = State.IDLE;
                myRP.update();
                break;
            default:
                break;
        }
        com.receive();
        com.send();
    }

    @SuppressWarnings("fallthrough")
    private void build_soldiers() throws GameActionException {
        switch(builder.doBuild()) {
            case DONE:
                soldierIDS[soldiers] = ((Robot)myRP.sensor.senseObjectAtLocation(soldierLoc, RobotLevel.ON_GROUND)).getID();
                soldiers++;
            case FAIL:
                state = State.IDLE;
                break;
            default:
                break;
        }
        com.receive();
        com.send();
    }

    private void idle() throws GameActionException {
        if(soldiers < MAX_SOLDIERS) {
            state = State.BUILD_SOLDIERS;

            Direction d = Direction.NORTH;
            for(int i = 0; i < 8; i++, d = d.rotateRight()) {
                MapLocation dest = myLoc.add(d);
                if(myRC.senseTerrainTile(dest) == TerrainTile.LAND &&
                   myRP.sensor.senseObjectAtLocation(dest, RobotLevel.ON_GROUND) == null) {
                    builder.startBuild(true, dest, Chassis.MEDIUM, ComponentType.HARDENED, ComponentType.RAILGUN, ComponentType.TELESCOPE);
                    soldierLoc = dest;
                    state = State.BUILD_SOLDIERS;
                }
            }
            com.receive();
            com.send();
        } else {
            if(++timer >= WAKE_TIME) {
                com.turnOn(soldierIDS);
                timer = 0;
            } else {
                com.receive();
                com.send();
            }
        }
    }
}
