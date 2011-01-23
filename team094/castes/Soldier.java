package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class Soldier extends Caste {
    private static enum State {
        SPREAD,
        SEARCH,
        GO,
        ATTACK,
        YIELD
    }
    private State state;

    private final Attacker attacker;
    private MapLocation target;
    private int msgMask,
                timer;

    public Soldier(RobotProperties rp){
        super(rp);
        state = State.SPREAD;
        attacker = new Attacker(rp, true);

        msgMask = Communicator.ATTACK;
    }

    public void SM() {
        while(true) {
            try {
                if(com.receive(msgMask)) {
                    nav.setDestination(com.getDestination(), 6);
                    target = com.getDestination();
                    state = State.GO;
                    com.send();
                }

                switch(state) {
                    case SPREAD:
                        spread();
                        break;
                    case SEARCH:
                        search();
                        break;
                    case GO:
                        go();
                        break;
                    case ATTACK:
                        attack();
                        break;
                }
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }

            //System.out.println(Clock.getBytecodeNum());
            myRC.yield();
        }
    }


    private void spread() throws GameActionException {
        // Attempt to stay 5 units from all objects
        MapLocation myLoc = myRC.getLocation();
        MapLocation dest = myLoc;

        // Gather info about your front
        for(Robot r: myRP.sensor.senseNearbyGameObjects(Robot.class)) {
            MapLocation rl = myRP.sensor.senseLocationOf(r);
            Direction rd = myLoc.directionTo(rl);
            int dist = (int)Math.sqrt(rl.distanceSquaredTo(myLoc));

            if(dist >= 4)
                dest = dest.add(rd.opposite(), dist-4);
            else
                dest = dest.add(rd, 4-dist);
        }
        nav.setDirection(myRC.getDirection().opposite());

        attacker.autoFire();
        myRC.yield();

        // And about your back
        for(Robot r: myRP.sensor.senseNearbyGameObjects(Robot.class)) {
            MapLocation rl = myRP.sensor.senseLocationOf(r);
            Direction rd = rl.directionTo(myLoc);

            dest = dest.add(rd, 5-(int)Math.sqrt(rl.distanceSquaredTo(myLoc)));
        }
        nav.setDirection(myRC.getDirection().opposite());

        com.receive(msgMask);
        com.send();
        attacker.autoFire();
        myRC.yield();

        nav.setDestination(dest, 1.9);
        nav.bugNavigate(false);
        com.receive(msgMask);
        if((myLoc = attacker.autoFire()) != null) {
            state = state.ATTACK;
            com.send(Communicator.ATTACK, attacker.rank, 5, myLoc);
        } else {
            com.send();
        }
    }



    private void search() throws GameActionException {
        MapLocation l;
        if((l = attacker.autoFire()) != null) {
            nav.setDestination(l, 6);
            nav.bugNavigate(true);
            state = State.ATTACK;
        } else if(timer > 7) {
            state = State.SPREAD;
        } else {
            nav.rotate(true, 1);
            timer++;
        }
    }

    private void go() throws GameActionException {
        MapLocation l;
        if((l = attacker.autoFire()) != null) {
            nav.setDestination(l, 6);
            nav.bugNavigate(true);
            state = State.ATTACK;
        } else if(nav.bugNavigate(true)) {
            timer = 0;
            state = state.SEARCH;
        }
    }

    private void attack() throws GameActionException {
        MapLocation l;
        if((l = attacker.autoFire()) == null) {
            if(target != null) {
                nav.setDestination(target, 6);
                state = state.GO;
            } else {
                timer = 0;
                state = State.SEARCH;
            }
        } else {
            nav.setDestination(l);
        }

        if(l != null && myRC.getLocation().distanceSquaredTo(l) <= 25)
            nav.move(false);
        else
            nav.bugNavigate(true);
    }
}
