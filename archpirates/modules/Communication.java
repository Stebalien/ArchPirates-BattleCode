package archpirates.modules;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class Communication {
    private final BroadcastController comm;
    private final RobotController myRC;

    public static final int ATTACK = 1;
    public static final int ATTACK_BUILDING = 2;
    public static final int DEFEND = 4;
    public static final int HEAL = 8;


    /**
     * Facilitates communication to and from this robot.
     *
     * @param rp The robots RobotProperties.
     */
    public Communication(RobotProperties rp) {
        comm = rp.comm;
        myRC = rp.myRC;
    }
    
    /**
     * Sends out a command to robots within range.
     *
     * @param bitmask The bitmask that describes the robots that should recieve this message.
     * @param path The path to the destination.
     * @return true if the message was sent. 
     */
    public boolean sendCommand(int bitmask, MapLocation... path) throws GameActionException {
        if (comm == null || comm.isActive()) return false;

        Message message = new Message();
        
        message.ints = new int [2];
        message.ints[0] = MessageID.get(Clock.getRoundNum());
        message.ints[1] = bitmask + (bitmask << 16);

        if (path != null) {
            message.locations = new MapLocation [path.length*2];
            for (int i = 0, j = 0; i < path.length; i++) {
                message.locations[j++] = path[i];
                message.locations[j++] = path[i];
            }
        }
        comm.broadcast(message);
        return true;
    }

    /**
     * Gets the first matching command message.
     *
     * Bitmask: 1 - Attack
     *          2 - Attack Building
     *          4 - Defend
     *          8 - Heal
     *
     * @param bitmask The bitmask that must match the command.
     * @return A path to the destination.
     */
    public MapLocation [] getCommand(int bitmask) {
        int round = Clock.getRoundNum();
        int id_prev = MessageID.get(round - 1);
        int id_now = MessageID.get(round);

        // Cache values for faster lookup
        int loc_length; // Length of the locations array
        int mask;
        int [] ints;
        int myloc_length;
        MapLocation [] path;
        MapLocation [] locations;

        read_message:
        for (Message m : myRC.getAllMessages()) {
            ints = m.ints;
            locations = m.locations;
            // 1. Check if ints is null.
            // 2. Check if locations is null.
            // 3. ints needs at 3 items.
            // 4. The first int must be either the current or the previous id.
            // 5. The the bitmask must be doubled.
            // 6. The mask must match the passed bitmask.
            // 7. locations needs at least 2 items. This step also records the length
            //    of the locations array.
            // 8. The locations array must be even.
            if (  ints == null
               || locations == null
               || ints.length != 2
               ||!(  ints[0] == id_now
                  || ints[0] == id_prev
                  )
               || (mask = (char)ints[1]) != (ints[1]>>>16)
               || ((char)bitmask & mask) == 0
               || (loc_length = locations.length) < 2
               || (loc_length & 1) != 0
                ) continue read_message;

            myloc_length = loc_length >> 1; //divide by two
            path = new MapLocation[myloc_length];

            do {
                if (!locations[--loc_length].equals(locations[--loc_length]))
                    continue read_message;
                path[--myloc_length] = locations[loc_length];
            } while ( loc_length != 0 );
            return path;
        }
        return null;
    }
}
