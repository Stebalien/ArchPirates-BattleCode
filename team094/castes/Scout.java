package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class Scout extends Caste {
    private static int TIMEOUT = 150;
    private static enum State {
        INIT,
        WANDER,
        BUILD,
        BUILD_ARMORY,
        YIELD
    }
    private State state;

    private final Builder builder;
    private Mine[] targets;
    private boolean armory;
    private MapLocation lastMine,
                        towerLoc;
    private int ti,
                timeout;

    public Scout(RobotProperties rp){
        super(rp);

        state = State.INIT;
        builder = new Builder(rp);
        targets = new Mine[10];
    }

    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case INIT:
                        init();
                        break;
                    case WANDER:
                        wander();
                        break;
                    case BUILD:
                        build();
                        break;
                    case BUILD_ARMORY:
                        build_armory();
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


    private void init() {
        ti = -1;
        state = State.WANDER;
    }

    private void wander() throws GameActionException {
        if(ti < 0) {
            Mine[] mines = myRP.sensor.senseNearbyGameObjects(Mine.class);
            for(Mine m: mines) {
                MapLocation mLoc = m.getLocation();
                if(ti < 9 && myRP.sensor.senseObjectAtLocation(mLoc, RobotLevel.ON_GROUND) == null) {
                    targets[++ti] = m;
                    nav.setDestination(mLoc, 1.8);
                }
            }

            // If more than two mines are in the area, attempt to build another armory
            armory = ti > 0 || (lastMine != null && ti == 0);
        }

        if(ti < 0) {
            lastMine = null;
            if(nav.canMoveForward())
                nav.move(true);
            else
                nav.setDirection(myRC.getDirection().opposite().rotateLeft());
        } else if(nav.bugNavigate()) {
            if(lastMine == null)
                lastMine = targets[ti].getLocation();
            builder.startBuild(true, targets[ti].getLocation(), Chassis.BUILDING, ComponentType.RECYCLER);
            timeout = 0;
            state = State.BUILD;
        }
    }

    @SuppressWarnings("fallthrough")
    private void build() throws GameActionException {
        switch(builder.doBuild()) {
            case WAITING:
                if(++timeout < TIMEOUT)
                    break;
            case FAIL:
                armory = false;
                lastMine = null;
            case DONE:
                ti--;
                if(ti > -1) {
                    nav.setDestination(targets[ti].getLocation(), 1.9);
                } else {
                    nav.setDestination(new MapLocation(0, 0));

                    if(armory) {
                        MapLocation l1 = targets[0].getLocation();

                        Direction term = lastMine.directionTo(l1);
                        Direction d = term.rotateLeft();

                        armory = false;
                        while(d != term && (towerLoc == null || armory)) {
                            MapLocation dest = lastMine.add(d);
                            if(!dest.equals(l1)
                               && myRC.senseTerrainTile(dest) == TerrainTile.LAND) {
                                if(!armory && dest.isAdjacentTo(l1)) {
                                    nav.setDestination(dest, 1.8);
                                    builder.startBuild(true, dest, Chassis.BUILDING, ComponentType.ARMORY);
                                    timeout = 0;
                                    armory = true;
                                } else if(!lastMine.directionTo(dest).isDiagonal()) {
                                    towerLoc = dest;
                                }
                            }

                            d = d.rotateLeft();
                        }

                        if(armory) {
                            armory = false;
                            state = State.BUILD_ARMORY;
                            return;
                        }
                    }
                }
                towerLoc = null;
                state = State.WANDER;
                break;
            default:
                break;
        }
    }

    @SuppressWarnings("fallthrough")
    private void build_armory() throws GameActionException {
        if(nav.bugNavigate()) {
            switch(builder.doBuild()) {
            case WAITING:
                if(++timeout < TIMEOUT)
                    break;
            case FAIL:
                towerLoc = null;
            case DONE:
                if(towerLoc != null) {
                    builder.startBuild(false, towerLoc, Chassis.BUILDING);
                    towerLoc = null;
                    timeout = 0;
                } else {
                    state = state.WANDER;
                    lastMine = null;
                }
                break;
            default:
                break;
            }
        }
    }
}
