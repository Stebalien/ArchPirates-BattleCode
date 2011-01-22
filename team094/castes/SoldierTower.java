package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class SoldierTower extends Caste {
    private static enum State {
        OFF,
        SETUP,
        MINE,
        SPAWN,
        BUILD,
        YIELD
    }
    private final Builder builder;
    private final MapLocation myLoc; // Buildings can't move.

    private State state;
    private int soldiers,
                flux;

    public SoldierTower(RobotProperties rp) {
        super(rp);

        state = State.SETUP;
        builder = new Builder(rp);
        myLoc = myRC.getLocation();

        builder.startBuild(false, 1.1, myLoc, RobotLevel.ON_GROUND, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD);
    }

    @SuppressWarnings("fallthrough")
    public void SM() {
        while(true) {
            flux++;

            try {
                switch(state) {
                    case SETUP:
                        setup();
                        break;
                    case OFF:
                        off();
                        break;
                    case MINE:
                        mine();
                        break;
                    case SPAWN:
                        spawn();
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
//            System.out.println(Clock.getBytecodeNum());
            myRC.yield();
        }
    }

    @SuppressWarnings("fallthrough")
    private void setup() throws GameActionException {
        switch(builder.doBuild()) {
            case DONE:
            case FAIL:
                state = State.MINE;
            default:
            break;
        }
    }

    private void mine() {
        if(flux > 200 || myRC.getTeamResources() > 800)
            state = state.BUILD;
    }

    private void spawn() {
        if(soldiers >= 3) {
            state = State.OFF;
            return;
        }

        Direction d = Direction.NORTH;
        for(int i = 0; i < 8; i++) {
            MapLocation dest = myLoc.add(d);
            if(myRP.builder.canBuild(Chassis.LIGHT, dest)) {
                builder.startBuild(true, 1, dest, Chassis.LIGHT, ComponentType.ANTENNA, ComponentType.BLASTER, ComponentType.RADAR);
                state = state.BUILD;
                break;
            }

            d = d.rotateRight();
        }
    }

    @SuppressWarnings("fallthrough")
    private void build() throws GameActionException {
        switch (builder.doBuild()) {
            case DONE:
                soldiers++;
            case FAIL:
                state = State.SPAWN;
                break;
            default:
                break;
        }
    }

    private void off() {
        soldiers = 0;
        state = State.SPAWN;
        myRC.turnOff();
    }
}
