package team094;

import team094.castes.Caste;
import team094.modules.MessageID;

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
