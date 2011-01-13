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

    private final Attacker attacker;

    public Sentry(RobotProperties rp){
        super(rp);

        state = State.WANDER;
        attacker = new Attacker(rp);
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

//            System.out.println(Clock.getBytecodeNum());
            myRC.yield();
        }
    }

    private void wander() throws GameActionException {
        MapLocation l;
        if((l = attacker.autoFire()) != null) {
            nav.setDestination(l, 3);
            nav.bugNavigate();
            state = State.ATTACK;
            return;
        }

        if(nav.canMoveForward())
            nav.move(true);
        else
            nav.setDirection(myRC.getDirection().opposite().rotateRight());
    }

    private void attack() throws GameActionException {
        MapLocation l = attacker.autoFire();
        if(l == null)
            state = State.WANDER;
        else
            nav.setDestination(l, 3);

        nav.bugNavigate();
    }
}
