package archpirates.castes;

import archpirates.modules.*;
import battlecode.common.*;

public class Sentry extends Caste {
    private static enum State {
        WANDER,
        ASSIST,
        ATTACK,
        YIELD
    }
    private State state;

    private final Attacker attacker;
    private MapLocation assistLoc;
    private int bitmask;

    public Sentry(RobotProperties rp){
        super(rp);

        state = State.WANDER;
        attacker = new Attacker(rp);

        bitmask = (Communicator.ATTACK|Communicator.DEFEND);
    }

    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case WANDER:
                        wander();
                        break;
                    case ASSIST:
                        assist();
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
            com.send(Communicator.ATTACK, new MapLocation[] {l});
            nav.setDestination(l, 3);
            nav.bugNavigate();
            state = State.ATTACK;
            return;
        } else if(com.recieve(bitmask)) {
            assistLoc = com.getPath()[com.getPath().length-1];
            nav.setDestination(assistLoc, 3);
            nav.bugNavigate();
            state = State.ASSIST;
            return;
        }

        if(nav.canMoveForward())
            nav.move(true);
        else
            nav.setDirection(myRC.getDirection().rotateRight().rotateRight());
    }

    private void assist() throws GameActionException {
        attacker.autoFire();
        if(nav.bugNavigate()) {
            state = State.ATTACK;
        }
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
