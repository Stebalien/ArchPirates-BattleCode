package archpirates.castes;

import archpirates.modules.*;
import battlecode.common.*;

public class Fighter extends Caste {
    private static enum State {
        ATTACK,
        DEFEND,
        WANDER,
        INIT,
        YIELD,
        GOTO
    }
    private State state;

    private final Attack attacker;
    private final Targeter targeter;
    private final int bitmask;

    public Fighter(RobotProperties rp){
        super(rp);

        state = State.INIT;

        attacker = new Attack(rp);
        targeter = new Targeter(myRP, myRP.opTeam);

        bitmask = ( Communication.ATTACK | Communication.DEFEND );
    }
    

    @SuppressWarnings("fallthrough")
    public void SM() {
        MapLocation [] path = null;
        while(true) {
            try {
                switch(state) {
                    case INIT:
                        // Wait until we stop firing and the gun is ready
                        if (!attacker.autoFire(targeter)) {
                            nav.setDestination(new MapLocation(0,0));
                            state = State.WANDER;
                        }
                        break;
                    case WANDER:
                        if ((path = com.getCommand(bitmask)) != null) {
                            nav.setDestination(path[path.length-1], 4);
                            state = State.GOTO;
                        }
                        else if (attacker.autoFire(targeter)) {
                            state = State.ATTACK;
                        }
                        nav.bugNavigate();
                        break;
                    case DEFEND:
                        attacker.autoFire(targeter);
                        break;
                    case GOTO:
                        if (!nav.bugNavigate())
                            state = State.ATTACK;
                        break;
                    case ATTACK:
                        com.sendCommand(Communication.ATTACK, myRC.getLocation());
                        attacker.autoFire(targeter);
                        //if (!attacker.autoChase(targeter, nav))
                        //    state = State.INIT;
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
