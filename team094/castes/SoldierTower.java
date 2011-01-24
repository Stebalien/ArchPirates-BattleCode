package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class SoldierTower extends Caste {
    private static enum State {
        SETUP,
        EQUIP_TOWER,
        MINE,
        SPAWN,
        BUILD,
        YIELD
    }
    private int RESOURCES;

    private final Builder builder;
    private final SensorController mySensor;
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
        mySensor = myRP.sensor;

        builder.startBuild(false, 1.1, myLoc, RobotLevel.ON_GROUND, ComponentType.RADAR, ComponentType.SMG, ComponentType.SMG);

        RESOURCES = 300+(10000-Clock.getRoundNum())/10;
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
                    case EQUIP_TOWER:
                        equip_tower();
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
                Robot[] robots = mySensor.senseNearbyGameObjects(Robot.class);
                for(Robot robot: robots) {
                    RobotInfo r = mySensor.senseRobotInfo(robot);
                    if(r.chassis == Chassis.BUILDING && !r.on && !myLoc.directionTo(r.location).isDiagonal()) {
                        builder.startBuild(true, 1.2, r.location, RobotLevel.ON_GROUND,
                                           ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.SHIELD,
                                           ComponentType.SHIELD, ComponentType.SHIELD, ComponentType.RADAR,
                                           ComponentType.SMG, ComponentType.SMG, ComponentType.SMG,
                                           ComponentType.SMG, ComponentType.SMG, ComponentType.SMG,
                                           ComponentType.SMG, ComponentType.SMG, ComponentType.SMG,
                                           ComponentType.SMG, ComponentType.SMG, ComponentType.SMG);
                        state = State.EQUIP_TOWER;
                        return;
                    }
                }
                state = State.MINE;
            default:
                break;
        }
    }

    @SuppressWarnings("fallthrough")
    private void equip_tower() throws GameActionException {
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
        double resources = myRC.getTeamResources();
        if(soldiers < 3 || (resources > RESOURCES && mySensor.senseNearbyGameObjects(Robot.class).length < 10) || myRC.getHitpoints() < myRC.getMaxHp()/2) {
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
