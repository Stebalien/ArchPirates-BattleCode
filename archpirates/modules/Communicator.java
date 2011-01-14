package archpirates.modules;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class Communicator {
    private final BroadcastController comm;
    private final RobotController myRC;

    public static final int ATTACK = 1;
    public static final int ATTACK_BUILDING = 2;
    public static final int DEFEND = 4;
    public static final int HEAL = 8;

    // Cache
    private int mask;
    private MapLocation [] path;


    /**
     * Facilitates communication to and from this robot.
     *
     * @param rp The robots RobotProperties.
     */
    public Communicator(RobotProperties rp) {
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
    public boolean send(int bitmask, MapLocation... path) throws GameActionException {
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
     * @return true if a message was recieved.
     */
    public boolean recieve(int bitmask) {
        int round = Clock.getRoundNum();
        int id_prev = MessageID.get(round - 1);
        int id_now = MessageID.get(round);

        // Cache values for faster lookup
        int loc_length; // Length of the locations array
        int [] ints;
        int myloc_length;
        MapLocation [] locations;

        read_message:
        for (Message m : myRC.getAllMessages()) {
            ints = m.ints;
            locations = m.locations;
            // 1. Check if ints is null.
            // 2. Check if locations is null.
            // 3. ints needs at 2 items.
            // 4. The first int must be either the current or the previous id.
            // 5. The the bitmask must be doubled.
            // 6. The mask must match the passed bitmask.
            // 7. locations needs at least 2 items. This step also records the length
            //    of the locations array.
            // 8. The locations array must be even.
            if (  ints == null
               || locations == null
               || ints.length < 2
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
            return true;
        }

        path = null;
        mask = 0;
        return false;
    }

    /**
     * Gets the command bitmask for the last message.
     * @return The last message's bitmask.
     */
    public int getCommand() {
        return mask;
    }

    /**
     * Gets the path for the last message.
     * @return The last message's path.
     */
    public MapLocation [] getPath() {
        return path;
    }

    /**
     * Gets the destination for the last message.
     * @return The last message's destination.
     */
    public MapLocation getDestination() {
        if (path != null && path.length >= 1)
            return path[path.length-1];
        else
            return null;
    }
}
