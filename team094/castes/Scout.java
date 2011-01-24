package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class Scout extends Caste {
    private static int TIMEOUT = 150;
    private static enum State {
        WANDER,
        BUILD,
        BUILD_TOWER,
        YIELD
    }
    private State state;

    private final Builder builder;
    private Mine[] targets;
    private int ti,
                timeout,
                mines;
    private double health;
    private int runCooldown = -1;
    private boolean tower;

    public Scout(RobotProperties rp){
        super(rp);
        health = myRC.getHitpoints();

        state = State.WANDER;
        builder = new Builder(rp);
        targets = new Mine[10];

        ti = -1;
    }

    @SuppressWarnings("fallthrough")
    public void SM() {
        while(true) {
            try {
                if (run()) {
                    myRC.yield();
                    continue;
                }
                switch(state) {
                    case WANDER:
                        wander();
                        break;
                    case BUILD:
                        build();
                        break;
                    case BUILD_TOWER:
                        build_tower();
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

            myRC.yield();
        }
    }

    private boolean run() throws GameActionException {
        double tmp_health = myRC.getHitpoints();

        if(runCooldown < 0) {
            if (tmp_health < health) {
                runCooldown = 5;
                nav.move(false);
                return true;
            } else {
                return false;
            }
        } else if(runCooldown == 0) {
            nav.setDirection(myRC.getDirection().opposite());
            runCooldown--;
            health = tmp_health;
            return false;
        } else {
            runCooldown--;
            nav.move(false);
            return true;
        }
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

            double resources = myRC.getTeamResources();
            tower = (ti > -1 && resources > 500 && (this.mines > 0 || resources > 1000));
            if(tower)
                System.out.println("Building a tower!");
        }

        if(ti < 0) {
            Direction cur = myRC.getDirection();
            if(myRC.senseTerrainTile(myRC.getLocation().add(cur, 2)) != TerrainTile.OFF_MAP  && nav.canMoveForward())
                nav.move(true);
            else
                nav.setDirection(myRC.getDirection().rotateLeft().rotateLeft());
        } else if(nav.bugNavigate(false)) {
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
            case DONE:
                mines++;
            case FAIL:
                ti--;
                if(ti > -1) {
                    nav.setDestination(targets[ti].getLocation(), 1.9);
                } else if(tower) {
                    tower = false;

                    Direction d = Direction.NORTH;
                    MapLocation myLoc = myRC.getLocation();
                    for(int i = 0; i < 8; i++, d = d.rotateRight()) {
                        MapLocation dest = myLoc.add(d);
                        if(myRP.builder.canBuild(Chassis.BUILDING, dest) && dest.isAdjacentTo(targets[0].getLocation()) && !dest.directionTo(targets[0].getLocation()).isDiagonal()) {
                            builder.startBuild(false, 1.2, dest, Chassis.BUILDING);
                            builder.doBuild();
                            state = state.BUILD_TOWER;
                            return;
                        }
                    }
                }

                state = State.WANDER;
                break;
            default:
                break;
        }
    }

    @SuppressWarnings("fallthrough")
    private void build_tower() throws GameActionException {
        switch(builder.doBuild()) {
            case WAITING:
                if(++timeout < TIMEOUT)
                    break;
            case DONE:
                mines = 0;
            case FAIL:
                state = State.WANDER;
                break;
            default:
                break;
        }
    }
}
