package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class SoldierTower extends Caste {
    private static enum State {
        SETUP,
        MINE,
        SPAWN,
        BUILD,
        YIELD
    }
    private int RESOURCES;

    private final Builder builder;
    private Attacker attacker;
    private final MapLocation myLoc; // Buildings can't move.

    private State state;
    private int soldiers,
                flux;

    public SoldierTower(RobotProperties rp) {
        super(rp);

        state = State.SETUP;
        builder = new Builder(rp);
        myLoc = myRC.getLocation();

        builder.startBuild(false, 1.1, myLoc, RobotLevel.ON_GROUND, ComponentType.RADAR, ComponentType.SMG, ComponentType.SMG);

        RESOURCES = 400+(10000-Clock.getRoundNum())/10;
    }

    @SuppressWarnings("fallthrough")
    public void SM() {
        MapLocation l;

        while(true) {
            flux++;
            try {
                if(state != State.SETUP) {
                    if((l = attacker.autoFire()) != null)
                        nav.setDirection(myLoc.directionTo(l));
                    else
                        nav.setDirection(myRC.getDirection().opposite());
                }

                switch(state) {
                    case SETUP:
                        setup();
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
                myRP.update();
                attacker = new Attacker(myRP, false);
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
        if(soldiers < 3 || (myRC.getTeamResources() > RESOURCES && myRP.sensor.senseNearbyGameObjects(Robot.class).length < 10)) {
            Direction d = Direction.NORTH;
            for(int i = 0; i < 8; i++) {
                MapLocation dest = myLoc.add(d);
                if(myRP.builder.canBuild(Chassis.LIGHT, dest)) {
                    builder.startBuild(true, 1.1, dest, Chassis.LIGHT, ComponentType.ANTENNA, ComponentType.SMG, ComponentType.SMG, ComponentType.RADAR);
                    state = state.BUILD;
                    break;
                }

                d = d.rotateRight();
            }
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
}
