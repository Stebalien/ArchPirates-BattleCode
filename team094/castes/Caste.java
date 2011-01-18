package team094.castes;

import team094.modules.Navigator;
import team094.modules.Communicator;
import team094.modules.RobotProperties;
import battlecode.common.*;

public abstract class Caste {
    protected final RobotController myRC;
    protected final RobotProperties myRP;
    protected final Navigator nav;
    protected Communicator com;

    /**
     * Instantiates the abstract caste.
     *
     * @param rp This robots RobotProperties.
     */
    public Caste(RobotProperties rp) {
        myRP = rp;
        myRC = myRP.myRC;
        nav = new Navigator(myRP);
        com = new Communicator(myRP);
    }


    /**
     * Fate the robot based on its components.
     *
     * Generates a RobotProperties and then chooses a caste based on the chassis and available components.
     *
     * @param rc The RobotController for this robot.
     * @return The instantiated caste of the robot.
     */
    public static Caste fate(RobotController rc) {
        RobotProperties myRP = new RobotProperties(rc);

        switch (rc.getChassis()) {
            case LIGHT:
                if (myRP.builder != null)
                    return new StartingScout(myRP);
                else
                    return new Fighter(myRP);
            case MEDIUM:
                return new Fighter(myRP);
            case HEAVY:
                return new Fighter(myRP);
            case FLYING:
                if(myRP.builder == null)
                    return new Sentry(myRP);
                else
                    return new Scout(myRP);
            case BUILDING:
                if(myRP.builder != null) {
                    switch (myRP.builder.type()) {
                        case RECYCLER:
                            return new Miner(myRP);
                        case ARMORY:
                            return new Armory(myRP);
                        case FACTORY:
                            return new Factory(myRP);
                        default:
                            return new Tower(myRP);
                    }
                } else {
                    return new Tower(myRP);
                }
            default:
                return new Fighter(myRP);
        }
    }
    protected void yield() {
        // Stub method that simply returns
    }

    /**
     * The state manager for this robot.
     * If this method returns, the robot dies a lonely and miserable robot death.
     * No techno dirge will be heard, no system beeps will be cried, no silent kernel panics will be shed.
     */
    public abstract void SM();
}
