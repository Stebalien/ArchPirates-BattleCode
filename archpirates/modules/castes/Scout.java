package archpirates.modules.castes;
import archpirates.modules.*;
import battlecode.common.*;

public class Scout extends Caste {
    private static enum State {
        INIT,
        WANDER,
        BUILD,
        YIELD
    }
    private State state;

    private final Builder builder;

    public Scout(RobotProperties rp){
        super(rp);

        state = State.YIELD; // DOES NOTHINIG FOR NOW (TODO)

        builder = new Builder(rp);
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
                    case YIELD:
                    default:
                        yield();
                        break;
                }
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }

            System.out.println(Clock.getBytecodeNum());
            myRC.yield();
        }
    }


    private void init() {
        // TODO 
    }
    private void wander() {
        // TODO
    }
    private void build() {
        // TODO
    }
}
/*
    pubblic void scout_start() {
        nav.setDestination(new MapLocation(0, 0));
        SensorController sensor = null;
        BuilderController builder = null;
        for(ComponentController contr: myRC.components()) {
            ComponentClass c = contr.componentClass();
            if(c == ComponentClass.SENSOR) {
                sensor = (SensorController)contr;
            } else if(c == ComponentClass.BUILDER) {
                builder = (BuilderController)contr;
            }
        }

        Mine[] targets = new Mine[10];
        int ti = -1;
        boolean building = false;

        state = wander;
    }

    public void scout_wander() {
        if(ti < 0) {
            Mine[] mines = sensor.senseNearbyGameObjects(Mine.class);
            for(Mine m: mines) {
                if(ti < 9 && sensor.senseMineInfo(m).roundsLeft == GameConstants.MINE_ROUNDS) {
                    targets[++ti] = m;
                    nav.setDestination(m.getLocation(), 1.5);
                }
            }
        }

        if(ti < 0) {
            //wander
            nav.bugNavigate();
        } else if(nav.bugNavigate()) {
                    MapLocation mineLoc = targets[ti].getLocation();

                    if(mineLoc.equals(myRC.getLocation())) {
                        if(nav.canMoveBackward())
                            nav.move(false);
                    } else if(!building) {
                        System.out.println("Building a mine");
                        if(!builder.isActive() && myRC.getTeamResources() >= 1.2*Chassis.BUILDING.cost) {
                            builder.build(Chassis.BUILDING, targets[ti].getLocation());
                            building = true;
                        }
                    } else {
                        System.out.println("Building recycler...");
                        if(!builder.isActive() && myRC.getTeamResources() >= 1.2*ComponentType.RECYCLER.cost) {
                            builder.build(ComponentType.RECYCLER, targets[ti].getLocation(), RobotLevel.ON_GROUND);
                            building = false;
                            ti--;

                            if(ti > -1)
                                nav.setDestination(targets[ti].getLocation(), 1.9);
                            else
                                nav.setDestination(new MapLocation(0, 0));
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }

            System.out.println(Clock.getBytecodeNum());
            myRC.yield();
        }
    }
*/ 
