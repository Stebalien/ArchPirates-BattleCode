package archpirates.modules;
import archpirates.modules.castes.*;
import archpirates.modules.Navigation;
import archpirates.modules.RobotProperties;
import battlecode.common.*;

public abstract class Caste {
    protected final RobotController myRC;
    protected final RobotProperties myRP;
    protected final Navigation nav;

    public Caste(RobotProperties rp) {
        myRP = rp;
        myRC = myRP.myRC;
        nav = new Navigation(myRP);
    }


    public static Caste fate(RobotController myRC) {
        RobotProperties myRP = new RobotProperties(myRC);

        switch (myRC.getChassis()) {
            case LIGHT:
                if (myRP.builder != null)
                    return new Scout(myRP);
                else
                    return new Fighter(myRP);
            case MEDIUM:
                return new Fighter(myRP);
            case HEAVY:
                return new Fighter(myRP);
                /*
            case FLYING:
                if (myRP.dropship != null)
                    return new Transport(myRP);
                else
                    return new Medic(myRP);
                    */
            case BUILDING:
                /*
                if (myRP.builder == null)
                    return new Tower(myRP);
                else {
                    switch (myRP.builder.type()) {
                        case RECYCLER:
                */
                            return new Miner(myRP);
                /*
                        case FACTORY:
                            return new Factory(myRP);
                        case ARMORY:
                            return new Armory(myRP);
                    }
                }
                */
            default:
                return new Fighter(myRP);
        }
    }
    protected void yield() {
        myRC.yield();
    }

    public abstract void SM();
}
