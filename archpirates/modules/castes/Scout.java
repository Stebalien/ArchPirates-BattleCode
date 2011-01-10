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
