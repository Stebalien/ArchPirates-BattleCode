package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class Tower extends Caste {
    private static enum State {
        DEFEND,
        YIELD
    }

    private State state;
    private Attacker attacker;
    private MapLocation myLoc;

    public Tower(RobotProperties rp){
        super(rp);

        attacker = new Attacker(rp);
        state = State.DEFEND;
        myLoc = myRC.getLocation();
    }

    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case DEFEND:
                        defend();
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

    private void defend() throws GameActionException {
        MapLocation target;
        if((target = attacker.autoFire()) != null) {
            Direction d = myLoc.directionTo(target);
            if(d != myRC.getDirection())
                nav.setDirection(d);
        } else {
            nav.setDirection(myRC.getDirection().opposite());
        }
    }
}
