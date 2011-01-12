package archpirates.castes;

import archpirates.modules.*;
import battlecode.common.*;

public class Scout extends Caste {
    private static enum State {
        INIT,
        WANDER,
        BUILD,
        YIELD
    }
    private State state;

    private final Builder builder;
    private Mine[] targets;
    private int ti;

    public Scout(RobotProperties rp){
        super(rp);

        state = State.INIT;

        builder = new Builder(rp);

        targets = new Mine[10];
    }

    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case INIT:
                        init();
                        break;
                    case WANDER:
                        wander();
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


    private void init() {
        nav.setDestination(new MapLocation(0, 0));
        ti = -1;
        state = State.WANDER;
    }

    private void wander() throws GameActionException {
        Mine[] mines = myRP.sensor.senseNearbyGameObjects(Mine.class);
        for(Mine m: mines) {
            if(ti < 9 && myRP.sensor.senseMineInfo(m).roundsLeft == GameConstants.MINE_ROUNDS) {
                targets[++ti] = m;
                nav.setDestination(m.getLocation(), 1.5);
            }
        }

        if(ti < 0) {
            nav.bugNavigate();
        } else if(nav.bugNavigate()) {
            MapLocation mineLoc = targets[ti].getLocation();
            MapLocation loc = myRC.getLocation();

            if(mineLoc.equals(loc)) {
                if(nav.canMoveBackward())
                    nav.move(false);
            } else {
                builder.startBuild(true, mineLoc, Chassis.BUILDING, ComponentType.RECYCLER);
                state = State.BUILD;
            }
        }
    }

    private void build() throws GameActionException {
        switch(builder.doBuild()) {
            case ACTIVE:
            case WAITING:
                return;
            case FAIL:
            case DONE:
                state = State.WANDER;
                ti--;
                if(ti > -1)
                    nav.setDestination(targets[ti].getLocation(), 1.9);
                else
                    nav.setDestination(new MapLocation(0, 0));

                break;
        }
    }
}
