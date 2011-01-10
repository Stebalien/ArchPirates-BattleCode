package archpirates.modules.castes;
import archpirates.modules.*;
import battlecode.common.*;

public class Fighter extends Caste {
    private static enum State {
        DEFEND,
        PURSUE,
        WANDER,
        YIELD
    }
    private State state;

    private final Attack attacker;
    private final Targeter targeter;

    public Fighter(RobotProperties rp){
        super(rp);

        state = State.PURSUE;

        attacker = new Attack(rp);
        targeter = new Targeter(myRP, myRP.opTeam);
    }
    

    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case DEFEND:
                        attacker.autoFire(targeter);
                        break;
                    case WANDER:
                        // TODO
                        break;
                    case PURSUE:
                        attacker.autoChase(targeter, nav);
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
}
