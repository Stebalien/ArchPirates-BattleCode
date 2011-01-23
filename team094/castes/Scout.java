package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class Scout extends Caste {
    private static int TIMEOUT = 150;
    private static enum State {
        WANDER,
        BUILD,
        YIELD
    }
    private State state;

    private final Builder builder;
    private Mine[] targets;
    private MapLocation home;
    private int ti,
                timeout;
    private double health;
    private int runCooldown = -1;

    public Scout(RobotProperties rp){
        super(rp);
        health = myRC.getHitpoints();

        state = State.WANDER;
        builder = new Builder(rp);
        targets = new Mine[10];
        home = myRC.getLocation();

        ti = -1;
    }

    @SuppressWarnings("fallthrough")
    public void SM() {
        while(true) {
            try {
                if (run()) {
                    myRC.yield();
                    continue;
                }
                switch(state) {
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

            myRC.yield();
        }
    }

    private boolean run() throws GameActionException {
        double tmp_health = myRC.getHitpoints();

        if(runCooldown < 0) {
            if (tmp_health < health) {
                runCooldown = 5;
                nav.move(false);
                return true;
            } else {
                return false;
            }
        } else if(runCooldown == 0) {
            nav.setDirection(myRC.getDirection().opposite());
            runCooldown--;
            health = tmp_health;
            return false;
        } else {
            runCooldown--;
            nav.move(false);
            return true;
        }
    }

    private void wander() throws GameActionException {
        if(ti < 0) {
            Mine[] mines = myRP.sensor.senseNearbyGameObjects(Mine.class);
            for(Mine m: mines) {
                MapLocation mLoc = m.getLocation();
                if(ti < 9 && myRP.sensor.senseObjectAtLocation(mLoc, RobotLevel.ON_GROUND) == null) {
                    targets[++ti] = m;
                    nav.setDestination(mLoc, 1.8);
                }
            }
        }

        if(ti < 0) {
            if(nav.canMoveForward())
                nav.move(true);
            else
                nav.setDirection(myRC.getDirection().rotateLeft().rotateLeft());
        } else if(nav.bugNavigate(false)) {
            builder.startBuild(true, 1.05, targets[ti].getLocation(), Chassis.BUILDING, ComponentType.RECYCLER);
            timeout = 0;
            state = State.BUILD;
        }
    }

    @SuppressWarnings("fallthrough")
    private void build() throws GameActionException {
        switch(builder.doBuild()) {
            case WAITING:
                if(++timeout < TIMEOUT)
                    break;
            case FAIL:
            case DONE:
                ti--;
                if(ti > -1) {
                    nav.setDestination(targets[ti].getLocation(), 1.9);
                }
                state = State.WANDER;
                break;
            default:
                break;
        }
    }
}
