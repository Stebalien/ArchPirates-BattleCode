package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class Tower extends Caste {
    private static enum State {
        SETUP,
        DEFEND,
        YIELD
    }

    private State state;
    private Attacker attacker;
    private MapLocation myLoc;

    public Tower(RobotProperties rp){
        super(rp);

        state = State.SETUP;
        myLoc = myRC.getLocation();
    }

    @SuppressWarnings("fallthrough")
    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case SETUP:
                        setup();
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

    private void setup() throws GameActionException {
        Mine[] nearbyMines = myRP.sensor.senseNearbyGameObjects(Mine.class);
        for(Mine m: nearbyMines) {
            MapLocation l = m.getLocation();
            Robot r;
            if(myLoc.isAdjacentTo(l) &&
               !myLoc.directionTo(l).isDiagonal() &&
               (r = (Robot)myRP.sensor.senseObjectAtLocation(l, RobotLevel.ON_GROUND)) != null) {
                if(!myRP.sensor.senseRobotInfo(r).on)
                    myRC.turnOn(l, RobotLevel.ON_GROUND);
                break;
            }
        }

        state = State.DEFEND;
        myRC.turnOff();
        myRP.update();
        attacker = new Attacker(myRP, true);
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
