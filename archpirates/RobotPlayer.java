package archpirates;

import archpirates.modules.RobotProperties;
import archpirates.modules.Attack;
import archpirates.modules.Targeter;
import archpirates.modules.Builder;
import archpirates.modules.TaskState;
import archpirates.modules.Navigation;
import archpirates.modules.Caste;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class RobotPlayer implements Runnable {
	private final RobotController myRC;

    public RobotPlayer(RobotController rc) {
        myRC = rc;
    }

	public void run() {
        ComponentController [] components = myRC.components();
        if (components.length <= 1) {
            myRC.turnOff();
        }
        Caste caste = Caste.fate(myRC);
        caste.SM();
	}
}
