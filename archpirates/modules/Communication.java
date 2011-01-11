package archpirates.modules;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class Communication {
    private final BroadcastController comm;

    /**
     * Facilitates communication to and from this robot.
     *
     * @param rp The robots RobotProperties.
     */
    public Communication(RobotProperties rp) {
        comm = rp.comm;
    }
    
    private boolean canBroadcast() {
        if (comm == null || comm.isActive())
            return false;
        else
            return true;
    }
    /**
     * Notifies all attackers of an enimy building.
     *
     * @param destination The location of the mine.
     * @param path The waypoints leading to the mine.
     */
    public void notifyBuilding(MapLocation destination, MapLocation... path) {

    }

    /**
     * Notifies all attackers to attack a location.
     *
     * @param destination The location of the mine.
     * @param path The waypoints leading to the mine.
     */
    public void notifyAttack(MapLocation destination, MapLocation... path) {
    }

    /**
     * Notifies all attackers to defend a location.
     *
     * @param destination The location of the mine.
     * @param path The waypoints leading to the mine.
     */
    public void notifyDefend(MapLocation destination, MapLocation... path) {
    }

    /**
     * Notifies all healers to defend a location.
     *
     * @param destination The location of the mine.
     * @param path The waypoints leading to the mine.
     */
    public void notifyHeal(MapLocation destination, MapLocation... path) {
    }

    private Message encode(int [] ints, String [] strings, MapLocation [] locations) {
        Message message = new Message();
        
        if (ints != null) {
            message.ints = new int [ints.length*2+1];
            message.ints[0] = MessageID.get();
            for (int i = 0, j = 1; i < ints.length; i++) {
                message.ints[j++] = ints[i];
                message.ints[j++] = ints[i];
            }
        } else {
            message.ints = new int [] {MessageID.get()};
        }

        if (strings != null) {
            message.strings = new String [strings.length*2];
            for (int i = 0, j = 0; i < strings.length; i++) {
                message.strings[j++] = strings[i];
                message.strings[j++] = strings[i];
            }
        }

        if (locations != null) {
            message.locations = new MapLocation [locations.length*2];
            for (int i = 0, j = 0; i < locations.length; i++) {
                message.locations[j++] = locations[i];
                message.locations[j++] = locations[i];
            }
        }
        return message;
    }
}
