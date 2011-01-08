package archpirates.modules;

import battlecode.common.*;

public class RobotProperties {
	public final RobotController myRC;
    public final Team myTeam;
    public final Team opTeam;
    public Cast cast;

    public ComponentController [] components;
    public WeaponController [] guns;
    public WeaponController [] beams;
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

        switch (myRC.getChassis()) {
            case LIGHT:
                if (builder != null)
                    cast = Cast.SCOUT;
                else
                    cast = Cast.FIGHTER;
                break;
            case MEDIUM:
                cast = Cast.FIGHTER;
                break;
            case HEAVY:
                cast = Cast.FIGHTER;
                break;
            case FLYING:
                if (dropship != null)
                    cast = Cast.TRANSPORT;
                else
                    cast = Cast.MEDIC;
                break;
            case BUILDING:
                if (builder.type() == ComponentType.RECYCLER)
                    cast = Cast.MINER;
                else
                    cast = Cast.MINER;
                break;
            default:
                cast = Cast.FIGHTER;
        }
    }
}
