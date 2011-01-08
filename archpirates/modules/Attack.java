package archpirates.modules;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
public class Attack {
	private final RobotController myRC;
    private final Team myTeam;
    private final Team opTeam;
    private final WeaponController [] guns;
    private final WeaponController [] beams;

    /**
     * The Attack class controls weapons and attacking.
     *
     * @param properties The properties of the controlling robot.
     */
    public Attack(RobotProperties properties) {
        this.myTeam = properties.myTeam;
        this.opTeam = properties.opTeam;
        this.myRC = properties.myRC;
        this.guns = properties.guns;
        this.beams = properties.beams;
    }

    /**
     * Fire everything location.
     */
    public void fireAt(MapLocation location, RobotLevel level) {
        fireAt(location, level, guns);
        fireAt(location, level, beams);
    }
    public void fireGunsAt(MapLocation location, RobotLevel level) {
        fireAt(location, level, guns);
    }
    public void fireBeamsAt(MapLocation location, RobotLevel level) {
        fireAt(location, level, beams);
    }
    public void fireAt(MapLocation location, RobotLevel level, WeaponController [] weapons) {
        for (WeaponController weapon : weapons) {
            if ( !weapon.isActive() && weapon.withinRange(location)) {
                try {
                    weapon.attackSquare(location, level);
                } catch(GameActionException e) {}
            }
        }
    }
    /**
     * Fire all guns on targets.
     *
     * @param targeter The targeter.
     */
    public void fireGunsOn(Targeter targeter) {
        fireGunsOn(targeter, guns); 
    }

    /**
     * Fire given guns on targets.
     * Should be grouped by type for speed.
     *
     * @param targeter The targeter.
     * @param guns All of the guns to fire.
     */
    public void fireGunsOn(Targeter targeter, WeaponController [] guns) {
        RobotInfo info = null;
        ComponentType type = null;
        for (WeaponController gun : guns) {
            // TODO: Group weapons better.
            if (!gun.isActive()) {
                if (type != (type = gun.type()))
                    info = targeter.getFirst(gun);
                if (info == null)
                    continue;
                try {
                    gun.attackSquare(info.location, info.robot.getRobotLevel());
                } catch(GameActionException e) {e.printStackTrace();}
            }
        }
    }

    /**
     * Fire gun with Targeter.
     *
     * @param targeter The targeter.
     * @param gun The gun.
     */
    public void fireGunOn(Targeter targeter, WeaponController gun) {
        if (gun.isActive()) return;

        RobotInfo info = targeter.getFirst(gun);
        if (info != null) {
            try {
                gun.attackSquare(info.location, info.robot.getRobotLevel());
            } catch(GameActionException e) {}
        }
    }
    public void autoFire() {
        autoFire(beams, guns);
    }
    public void autoFire(WeaponController [] beams, WeaponController [] guns) {
        // Impliment automatic targeting based on weapon here.
        // Should use subfunctions for indevidual weapons types.
        // SMG -> light
        // Beam -> Buildings (+MINES+)
    }
}
