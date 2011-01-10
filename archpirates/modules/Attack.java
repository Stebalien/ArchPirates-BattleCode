package archpirates.modules;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
public class Attack {
	private final RobotController myRC;
    private final WeaponController [] guns;
    private final WeaponController [] beams;

    /**
     * The Attack class controls weapons and attacking.
     *
     * @param properties The properties of the controlling robot.
     */
    public Attack(RobotProperties properties) {
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
     *
     * @return True if a gun was fired.
     */
    public boolean autoFire(Targeter targeter) {
        return autoFire(targeter, guns); 
    }

    /**
     * Fire given guns on targets.
     * Should be grouped by type for speed.
     *
     * @param targeter The targeter.
     * @param guns All of the guns to fire.
     *
     * @return True if a gun was fired.
     */
    public boolean autoFire(Targeter targeter, WeaponController [] guns) {
        RobotInfo info = null;
        ComponentType type = null;
        boolean active = false;
        for (WeaponController gun : guns) {
            // TODO: Group weapons better.
            if (!gun.isActive()) {
                if (type != (type = gun.type()))
                    info = targeter.targetRobot(gun);
                if (info == null)
                    continue;
                try {
                    gun.attackSquare(info.location, info.robot.getRobotLevel());
                    active = true;
                } catch(GameActionException e) {e.printStackTrace();}
            } else {
                active = true;
            }
        }
        return active;
    }

    /**
     * Fire gun with Targeter.
     *
     * @param targeter The targeter.
     * @param gun The gun.
     *
     * @return True if a gun was fired.
     */
    public boolean autoFire(Targeter targeter, WeaponController gun) {
        if (gun.isActive()) return true;

        RobotInfo info = targeter.targetRobot(gun);
        if (info != null) {
            try {
                gun.attackSquare(info.location, info.robot.getRobotLevel());
                return true;
            } catch(GameActionException e) {e.printStackTrace();}
        }
        return false;
    }

    public boolean pursue(Targeter targeter, Navigation nav) {
        RobotInfo info = null;
        ComponentType type = null;
        boolean active = false;
        for (WeaponController gun : guns) {
            // TODO: Group weapons better.
            if (!gun.isActive()) {
                if (type != (type = gun.type()))
                    info = targeter.chaseRobot(gun, nav);
                if (info == null)
                    continue;
                try {
                    gun.attackSquare(info.location, info.robot.getRobotLevel());
                    active = true;
                } catch(GameActionException e) {e.printStackTrace();}
            } else {
                active = true;
            }
        }
        return active;
    }
}
