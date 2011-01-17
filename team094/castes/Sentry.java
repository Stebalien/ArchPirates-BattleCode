package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class Sentry extends Caste {
    private static enum State {
        ORIENT,
        WANDER,
        ASSIST,
        ATTACK,
        YIELD
    }
    private State state;

    private final Attacker attacker;
    private MapLocation assistLoc;
    private int bitmask,
                broadcastDelay;

    public Sentry(RobotProperties rp){
        super(rp);

        state = State.ORIENT;
        attacker = new Attacker(rp);

        bitmask = (Communicator.ATTACK|Communicator.DEFEND);
    }

    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case ORIENT:
                        orient();
                        break;
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

    private void orient() throws GameActionException {
        while (myRP.motor.isActive()) myRC.yield();
        myRP.motor.setDirection(myRC.getDirection().opposite());
        myRC.yield();
        MapLocation loc, myLoc = myRC.getLocation();
        loc = myLoc;

        if (myRC.senseTerrainTile(myLoc.add(Direction.NORTH, 4)) == TerrainTile.OFF_MAP) {
            loc = loc.add(Direction.NORTH);
        }
        if (myRC.senseTerrainTile(myLoc.add(Direction.SOUTH, 4)) == TerrainTile.OFF_MAP) {
            loc = loc.add(Direction.SOUTH);
        }
        if (myRC.senseTerrainTile(myLoc.add(Direction.EAST, 4)) == TerrainTile.OFF_MAP) {
            loc = loc.add(Direction.EAST);
        }
        if (myRC.senseTerrainTile(myLoc.add(Direction.WEST, 4)) == TerrainTile.OFF_MAP) {
            loc = loc.add(Direction.WEST);
        }
        Direction dir = myLoc.directionTo(loc);
        if (dir != Direction.OMNI) {
            while (myRP.motor.isActive()) myRC.yield();
            myRP.motor.setDirection(dir.opposite());
        }
        state = State.WANDER;
    }


    private void wander() throws GameActionException {
        MapLocation l;
        if((l = attacker.autoFire()) != null) {
            nav.setDestination(l, 3);
            nav.bugNavigate();
            state = State.ATTACK;
            return;
        } else if(com.recieve(bitmask)) {
            assistLoc = com.getDestination();
            com.relay();
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
        if(l == null) {
            broadcastDelay = 0;
            state = State.WANDER;
        } else {
            if(++broadcastDelay >= 10) {
                broadcastDelay = 0;
                com.send(Communicator.ATTACK, 5, l);
            }
            nav.setDestination(l, 3);
        }

        nav.bugNavigate();
    }
}
