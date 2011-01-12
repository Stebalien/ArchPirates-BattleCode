package archpirates;

import archpirates.modules.RobotProperties;
import archpirates.modules.Attack;
import archpirates.modules.Targeter;
import archpirates.modules.Builder;
import archpirates.modules.TaskState;
import archpirates.modules.Navigation;
import archpirates.modules.MessageID;

import archpirates.castes.Caste;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class RobotPlayer implements Runnable {
	private final RobotController myRC;

    public RobotPlayer(RobotController rc) {
        myRC = rc;
    }

	public void run() {
        // Run this first so that we read the array into memory.
        ComponentController [] components = myRC.components();
        if (components.length <= 2) {
            myRC.turnOff();
        }
        MessageID.get(0); // 1000 bytecode down the drain.
        Caste caste = Caste.fate(myRC);
        caste.SM();
	}
}
