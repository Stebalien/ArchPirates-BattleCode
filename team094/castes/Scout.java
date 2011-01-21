package team094.castes;

import team094.modules.*;
import battlecode.common.*;

import java.util.Random;

public class Scout extends Caste {
    private static int TIMEOUT = 150;
    private static double FACTORY_PROB = .0012;
    private static enum State {
        INIT,
        WANDER,
        BUILD,
        BUILD_ARMORY,
        BUILD_FACTORY,
        YIELD
    }
    private State state;

    private final Builder builder;
    private Random r;
    private Mine[] targets;
    private boolean armory;
    private MapLocation lastMine,
                        towerLoc,
                        home;
    private int ti,
                timeout;
    private double health;
    private int runCooldown = -1;

    public Scout(RobotProperties rp){
        super(rp);
        health = myRC.getHitpoints();

        state = State.INIT;
        builder = new Builder(rp);
        targets = new Mine[10];
        home = myRC.getLocation();

        r = new Random(myRC.getRobot().getID());
    }

    @SuppressWarnings("fallthrough")
    public void SM() {
        while(true) {
            try {
                if (!run()) {
                    switch(state) {
                        case INIT:
                            init();
                        case WANDER:
                            wander();
                            break;
                        case BUILD:
                            build();
                            break;
                        case BUILD_ARMORY:
                            build_armory();
                            break;
                        case BUILD_FACTORY:
                            build_factory();
                            break;
                        case YIELD:
                        default:
                            yield();
                            break;
                    }
                }
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }
            myRC.yield();
        }
    }


    private void init() {
        ti = -1;
        state = State.WANDER;
    }
    private boolean run() throws GameActionException {
        double tmp_health = myRC.getHitpoints();
        runCooldown--;
        if (tmp_health < health) {
            runCooldown = 5;
            nav.move(false);
            health = tmp_health;
            return true;
        } else if (runCooldown == 0) {
            nav.setDirection(myRC.getDirection().opposite());
            health = tmp_health;
            return true;
        }
        health = tmp_health;
        return false;
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
            // Check to see if you should build a factory (beeeeg soldiers! >:D)
            if(r.nextDouble() < FACTORY_PROB+home.distanceSquaredTo(myRC.getLocation())*.0000002) {
                MapLocation loc = myRC.getLocation();
                Direction d = myRC.getDirection().rotateLeft();
                for(int i = 0; i < 3; i++, d = d.rotateRight()) {
                    MapLocation dest = loc.add(d);
                    if(myRC.senseTerrainTile(dest) == TerrainTile.LAND &&
                       myRP.sensor.senseObjectAtLocation(dest, RobotLevel.ON_GROUND) == null) {
                        builder.startBuild(true, 1.2, dest, Chassis.BUILDING, ComponentType.FACTORY);
                        state = State.BUILD_FACTORY;
                    }
                }
            } else {
                lastMine = null;
                if(nav.canMoveForward())
                    nav.move(true);
                else
                    nav.setDirection(myRC.getDirection().opposite().rotateLeft());
            }
        } else if(nav.bugNavigate(false)) {
            if(lastMine == null)
                lastMine = targets[ti].getLocation();
            builder.startBuild(true, 1.05, targets[ti].getLocation(), Chassis.BUILDING, ComponentType.RECYCLER);
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
                                    builder.startBuild(true, 1.2, dest, Chassis.BUILDING, ComponentType.ARMORY);
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
        if(nav.bugNavigate(false)) {
            switch(builder.doBuild()) {
            case WAITING:
                if(++timeout < TIMEOUT)
                    break;
            case FAIL:
                towerLoc = null;
            case DONE:
                if(towerLoc != null) {
                    builder.startBuild(true, 1.3, towerLoc, Chassis.BUILDING);
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

    @SuppressWarnings("fallthrough")
    private void build_factory() throws GameActionException {
        switch(builder.doBuild()) {
            case FAIL:
            case DONE:
                state = state.WANDER;
                break;
            default:
                break;
        }
    }
}
