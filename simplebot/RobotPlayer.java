package simplebot;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import simplebot.modules.Navigation;

public class RobotPlayer implements Runnable {
    // Global variables
	private final RobotController myRC;
    private Navigation nav;

    // State information
    private State state;

    /* State-specific variables: keep grouped and organized */

    public RobotPlayer(RobotController rc) {
        myRC = rc;
        state = State.INIT;
    }

    /* Master method, calls each state as appropriate */
    @Override
	public void run() {
        while(true) {
                try {
            switch(state) {
                case INIT:
                    init();
                    break;
                case SCOUT_START:
                    scout_start();
                    break;
                case SCOUT_WANDER:
                    scout_wander();
                    break;
                case TEST_YIELD:
                case default:
                    test_yield();
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

    /* Inititialization code, this is run at the first round that any robot
     * is created */
    public void init() {
		ComponentController [] components = myRC.newComponents();
		System.out.println(java.util.Arrays.toString(components));
		System.out.flush();
		if(myRC.getChassis()==Chassis.BUILDING) {
            state = TEST_YIELD;
		} else {
            nav = new Navigation(myRC, (MovementController)components[0]);

            state = SCOUT_START;
        }
	}


    ////////////////
    // Scout Code //
    ////////////////

    public void scout_start() {
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
        /*** beginning of main loop ***/
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

                /*** end of main loop ***/
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }

            System.out.println(Clock.getBytecodeNum());
            myRC.yield();
        }
    }

    ///////////////////
    //  TESTING CODE //
    ///////////////////

	public void test_yield() {
        myRC.yield();
	}
}


enum State {
    INIT,

    // Scout states
    SCOUT_START, SCOUT_WANDER, SCOUT_BUILD, SCOUT_RUN,

    // Test states
    TEST_YIELD
}

