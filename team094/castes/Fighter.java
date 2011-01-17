package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class Fighter extends Caste {
    private static enum State {
        OFF,
        SEARCH,
        GO,
        ATTACK,
        GO_HOME,
        YIELD
    }
    private State state;

    private final Attacker attacker;
    private MapLocation home,
                        target;
    private int msgMask,
                timer;

    public Fighter(RobotProperties rp){
        super(rp);
        state = State.SEARCH;
        attacker = new Attacker(rp);

        home = myRC.getLocation();

        msgMask = Communicator.ATTACK;
    }

    public void SM() {
        while(true) {
            try {
                if(com.receive(msgMask) && state != State.ATTACK) {
                    int msg = com.getCommand();
                    nav.setDestination(com.getDestination(), 3);
                    target = com.getDestination();
                    state = State.GO;
                }

                switch(state) {
                    case OFF:
                        off();
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
                    case GO_HOME:
                        go_home();
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

    private void off() {
        state = state.SEARCH;
        myRC.turnOff();
        for(timer = 0; timer < GameConstants.POWER_WAKE_DELAY; timer++)
            myRC.yield();
        timer = 0;

    }

    private void search() throws GameActionException {
        MapLocation l;
        if((l = attacker.autoFire()) != null) {
            nav.setDestination(l, 3);
            nav.bugNavigate(true);
            state = State.ATTACK;
        } else if(timer > 7) {
            nav.setDestination(home, 2);
            nav.bugNavigate(true);
            state = State.GO_HOME;
        } else {
            nav.rotate(true, 1);
            timer++;
        }
    }

    private void go() throws GameActionException {
        MapLocation l;
        if((l = attacker.autoFire()) != null) {
            nav.setDestination(l, 3);
            nav.bugNavigate(true);
            state = State.ATTACK;
        } else if(nav.bugNavigate(true)) {
            target = null;
            timer = 0;
            state = state.SEARCH;
        }
    }

    private void attack() throws GameActionException {
        MapLocation l;
        if((l = attacker.autoFire()) == null) {
            if(target != null) {
                nav.setDestination(target, 3);
                state = state.GO;
            } else {
                timer = 0;
                state = State.SEARCH;
            }
        }

        nav.bugNavigate(true);
    }

    private void go_home() throws GameActionException {
        MapLocation l;
        if((l = attacker.autoFire()) != null) {
            nav.setDestination(l, 3);
            nav.bugNavigate(true);
            state = State.ATTACK;
        } else if(nav.bugNavigate(true)) {
            state = State.OFF;
        }
    }
}
