package team094.modules;

import battlecode.common.*;

public class RobotProperties {
	public final RobotController myRC;
    public final Team myTeam, opTeam;
    public final int DIST_MULTIPLIER;

    public ComponentController [] components;
    public WeaponController [] guns, beams;
    public MovementController motor;
    public SensorController sensor;
    public BuilderController builder;
    public BroadcastController comm;
    public DropshipController dropship;

    /**
     * A simple class to store information about the robot.
     *
     * @param rc The robot controller.
     */
    public RobotProperties(RobotController rc) {
        myRC = rc;
        myTeam = myRC.getTeam();
        opTeam = myTeam.opponent();
        switch(myRC.getChassis()) {
            case HEAVY:
                DIST_MULTIPLIER = 10;
                break;
            case MEDIUM:
                DIST_MULTIPLIER = 6;
                break;
            case LIGHT:
                DIST_MULTIPLIER = 3;
                break;
            case FLYING:
                DIST_MULTIPLIER = 1;
                break;
            default:
                DIST_MULTIPLIER = 0;
        }
        update();
    }
    public void update() {
        components = myRC.components();

        // Sort Components
        // REALLY BAD!!!!!!!!!
        WeaponController [] tmp_guns = new WeaponController[components.length];
        int c_guns = 0;
        WeaponController [] tmp_beams = new WeaponController[components.length];
        int c_beams = 0;

        for (ComponentController comp : components) {
            ComponentType type = comp.type();
            switch (type.componentClass) {
                case WEAPON:
                    if (type == ComponentType.BEAM)
                        tmp_beams[c_beams++] = (WeaponController)comp;
                    else
                        tmp_guns[c_guns++] = (WeaponController)comp;
                    break;
                case SENSOR:
                    sensor = (SensorController)comp;
                    break;
                case MOTOR:
                    motor = (MovementController)comp;
                    break;
                case COMM:
                    comm = (BroadcastController)comp;
                    break;
                case BUILDER:
                    builder = (BuilderController)comp;
                    break;
            }
        }

        guns = new WeaponController[c_guns];
        beams = new WeaponController[c_beams];

        for (int i = 0; i<c_guns; i++) {
            guns[i] = tmp_guns[i];
        }
        for (int i = 0; i<c_beams; i++) {
            beams[i] = tmp_beams[i];
        }

    }
}
