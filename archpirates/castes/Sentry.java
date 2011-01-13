package archpirates.castes;

import archpirates.modules.*;
import battlecode.common.*;

public class Sentry extends Caste {
    private static enum State {
        WANDER,
        ATTACK,
        YIELD
    }
    private State state;

    private final Targeter targeter;

    public Sentry(RobotProperties rp){
        super(rp);

        state = State.WANDER;
        targeter = new Targeter(rp);
    }

    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case WANDER:
                        wander();
                        break;
                    case ATTACK:
                        attack();
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

    // FIXME: use targeter to seek enemies within range
    private void wander() throws GameActionException {
//        if(ti < 0) {
            if(nav.canMoveForward())
                nav.move(true);
            else
                nav.setDirection(myRC.getDirection().opposite().rotateLeft());
/*        } else if(nav.bugNavigate()) {
            MapLocation mineLoc = targets[ti].getLocation();
            MapLocation loc = myRC.getLocation();

            if(mineLoc.equals(loc)) {
                if(nav.canMoveBackward())
                    nav.move(false);
            } else {
                builder.startBuild(true, mineLoc, Chassis.BUILDING, ComponentType.RECYCLER);
                state = State.BUILD;
            }
        }*/
    }

    // FIXME: actually attack things
    private void attack() throws GameActionException {
/*        switch(builder.doBuild()) {
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
        }*/
    }
}
