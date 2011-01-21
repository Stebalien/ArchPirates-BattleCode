package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class StartingScout extends Caste {
    private static enum State {
        FIND_INIT,
        BUILD_INIT,
        FIND_WALL,
        SEARCH,
        BUILD,
        YIELD
    }
    private State state;

    private final Builder builder;

    private Mine[] targets;
    private int ti;
    private MapLocation wallLoc;
    private Direction wallDir;

    public StartingScout(RobotProperties rp){
        super(rp);

        state = State.FIND_INIT;
        targets = new Mine[10];
        wallDir = Direction.NORTH;
        ti = 2;

        builder = new Builder(rp);
    }

    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case FIND_INIT:
                        find_init();
                        break;
                    case BUILD_INIT:
                        build_init();
                        break;
                    case FIND_WALL:
                        find_wall();
                        break;
                    case SEARCH:
                        search();
                        break;
                    case BUILD:
                        build();
                        break;
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


    private void find_init() throws GameActionException {
        // Uses ti as a countdown to know when to build the armory, (after building two initial
        // mines
        if(ti == 2) {
            // First, locate where the home 4 squares are
            MapLocation loc = myRC.getLocation();

            if(!nav.canMove(wallDir)) {
                // Found something block the robots path, is it a mine?
                if(myRC.getDirection() == wallDir) {
                    targets[0] = (Mine)myRP.sensor.senseObjectAtLocation(loc.add(wallDir), RobotLevel.MINE);
                    if(targets[0] == null) {
                        // This is not the mine you're looking for
                        wallDir = wallDir.rotateRight().rotateRight();
                        return;
                    }

                    // Only two, (and at least two), should be in view.  Assign them manually
                    targets[0] = (Mine)myRP.sensor.senseObjectAtLocation(loc.add(wallDir, 2), RobotLevel.MINE);
                    targets[1] = (Mine)myRP.sensor.senseObjectAtLocation(loc.add(wallDir, 2).add(wallDir.rotateRight().rotateRight()), RobotLevel.MINE);
                    if(targets[1] == null)
                        targets[1] = (Mine)myRP.sensor.senseObjectAtLocation(loc.add(wallDir, 2).add(wallDir.rotateLeft().rotateLeft()), RobotLevel.MINE);
                    ti--;

                    // Finally head to the first mine
                    nav.setDestination(targets[1].getLocation(), 1.9);
                    nav.bugNavigate(false);
                } else {
                    // Fixes a bug, where the mine cannot be sensed if the robot doesn't start facing it
                    nav.setDirection(wallDir);
                }
            } else {
                // No mines this way, turn 90 degrees
                wallDir = wallDir.rotateRight().rotateRight();
            }
        } else if(ti == 1) {
            // Found first mine, move towards it and build
            if(nav.bugNavigate(false)) {
                builder.startBuild(true, 1, targets[1].getLocation(), Chassis.BUILDING, ComponentType.RECYCLER);
                builder.doBuild();

                nav.setDestination(targets[0].getLocation(), 1.9);
                state = State.BUILD_INIT;
            }
        } else if(ti == 0) {
            // Should be within range of second mine, but just in case, navigate
            if(nav.bugNavigate(false)) {
                MapLocation loc = myRC.getLocation();
                if(loc.equals(targets[ti].getLocation())) {
                    // It is most common to be on this second mine, so usually the robot will have
                    // to move backwards.  Reversing the order of the mines removes this need,
                    // but adds time to navigation and builds the armory too far away from home base
                    nav.move(false);
                } else {
                    builder.startBuild(true, 1, targets[0].getLocation(), Chassis.BUILDING, ComponentType.RECYCLER);
                    builder.doBuild();

                    state = State.BUILD_INIT;
                }
            }
        } else {
            // Finally, build the armory in the first square available (Doing a right sweep starting
            // from forward)
            Direction d = myRC.getDirection().rotateLeft();
            MapLocation loc = myRC.getLocation();
            MapLocation mLoc = targets[0].getLocation();
            while(!nav.canMove(d) || !loc.add(d).isAdjacentTo(mLoc))
                d = d.rotateLeft();

            builder.startBuild(true, 1, myRC.getLocation().add(d), Chassis.BUILDING, ComponentType.ARMORY);
            builder.doBuild();
            state = State.BUILD_INIT;
        }
    }

    private void build_init() throws GameActionException {
        // Wait for building to complete
        switch(builder.doBuild()) {
        case DONE:
            if(ti > -1) {
                ti--;
                state = State.FIND_INIT;
            } else {
                nav.setDestination(new MapLocation(0, 0));
                state = State.FIND_WALL;
            }
            break;
        case FAIL:
            // If building fails, build armory if possible, then go to normal search
            if(ti > -1) {
                ti = -1;
                state = State.FIND_INIT;
            } else {
                nav.setDestination(new MapLocation(0, 0));
                state = State.FIND_WALL;
            }
            break;
        default:
            return;
        }
    }

    private void find_wall() throws GameActionException {
        // Navigate towards 0,0, looking for map boundaries
        nav.bugNavigate(false);

        // If the square in front is off Map, turn right and start following the wall
        if(myRC.senseTerrainTile(myRC.getLocation().add(myRC.getDirection())) == TerrainTile.OFF_MAP) {
            /* Generally bad form, but this is a special case where nothing more needs to be done */
            myRC.yield();

            state = State.SEARCH;
        }
    }

    private void search() throws GameActionException {
        if(ti < 0) {
            Mine[] mines = myRP.sensor.senseNearbyGameObjects(Mine.class);
            for(Mine m: mines) {
                MapLocation mLoc = m.getLocation();
                if(ti < 9 && myRP.sensor.senseObjectAtLocation(mLoc, RobotLevel.ON_GROUND) == null) {
                    targets[++ti] = m;
                    if(wallLoc == null) {
                        wallLoc = myRC.getLocation();
                        wallDir = myRC.getDirection();
                    }
                    nav.setDestination(mLoc, 1.5);
                }
            }
        }

        if(ti < 0) {
            if(wallLoc != null) {
                nav.setDestination(wallLoc, wallDir, 0);
                if(nav.bugNavigate(false)) {
                    wallLoc = null;
                }
            } else {
                nav.wallFollow(false);
            }
        } else if(nav.bugNavigate(false)) {
            MapLocation mineLoc = targets[ti].getLocation();
            MapLocation loc = myRC.getLocation();

            if(mineLoc.equals(loc)) {
                if(nav.canMoveBackward())
                    nav.move(false);
            } else {
                builder.startBuild(true, 1.05, mineLoc, Chassis.BUILDING, ComponentType.RECYCLER);
                builder.doBuild();
                state = State.BUILD;
            }
        }
    }

    private void build() throws GameActionException {
        switch(builder.doBuild()) {
            case ACTIVE:
            case WAITING:
                return;
            case FAIL:
            case DONE:
                state = State.SEARCH;
                ti--;
                if(ti > -1)
                    nav.setDestination(targets[ti].getLocation(), 1.9);
                else
                    nav.setDestination(new MapLocation(0, 0));

                break;
        }
    }
}
