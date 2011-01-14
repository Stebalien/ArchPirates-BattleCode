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
    private MapLocation source;
    private Message last_message;


    /**
     * Facilitates communication to and from this robot.
     *
     * Message format:
     *   ints: (round-id) (bitmask + bitmask) (distance from source to last ^2) (distance from source to last ^2) (rebroadcast num) (rebroadcast num)
     *   Strings: null
     *   MapLocations: [(path) (path)]... (dest) (dest) (source) (source)
     *
     * @param rp The robots RobotProperties.
     */
    public Communicator(RobotProperties rp) {
        comm = rp.comm;
        myRC = rp.myRC;
    }

    /**
     * Clears all messages from the message queue.
     *
     * @return true if messages were cleared.
     */
    public boolean clear() {
        return myRC.getAllMessages().length > 0;
    }


    //TODO: This is not finished, we should relay messages even if it was not destined for us.
    // maybe expand message/two messages in one.
    //
    // null separrated. TODO <<<<<<!!!!!!!!!!!!!!!!>>>>>>>>>>>>>>>>>>>>>>>> THIS IS IT FIXME TODO THIS GOOD!! Nulls are free
    //
    // ints: (round-id) [(mask+mask) (distance from source to last ^2) (distance from source to last ^2) (rebroadcast-rounds) (rebroadcast-rounds)] (null) [repeat...]
    // locations: [[(path) (path)]... (dest) (dest) (source) (source)] (null) [repeat...]
    /**
     * Relays the cached message if there is one and we should relay it.
     *
     * @return true if we relayed the message
     */
    public boolean relay() throws GameActionException {
        if (last_message != null && comm != null && !comm.isActive() && last_message.ints[4]-- > 0) {
            int distance = source.distanceSquaredTo(myRC.getLocation());
            if (distance > last_message.ints[2]) {
                last_message.ints[2] = last_message.ints[3] = distance; // Set both distances
                last_message.ints[5]--; //decriment the other round counter
                last_message.ints[0] = MessageID.get(Clock.getRoundNum()); // set the message id
                comm.broadcast(last_message);
                return true;
            }
        }
        return false;
    }

    /**
     * Sends out a command to robots within range.
     *
     * @param bitmask The bitmask that describes the robots that should recieve this message.
     * @param path The path to the destination.
     * @return true if the message was sent.
     */
    public boolean send(int bitmask, int rebroadcast, MapLocation... path) throws GameActionException {
        if (comm == null || comm.isActive() || path == null || path.length == 0) return false;

        Message message = new Message();

        message.ints = new int [6];
        message.ints[0] = MessageID.get(Clock.getRoundNum());
        message.ints[1] = bitmask + (bitmask << 16);
        message.ints[2] = message.ints[3] = 0;
        message.ints[4] = message.ints[5] = rebroadcast;

        int path_length = path.length;
        int loc_length = ((path_length+1) * 2); // The length of the locations array includes the source
        message.locations = new MapLocation [loc_length];
        message.locations[--loc_length] = message.locations[--loc_length] = myRC.getLocation(); // The last two items in the path are the source

        do {
            message.locations[--loc_length] = message.locations[--loc_length] = path[--path_length];
        } while (path_length > 0);

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
        // Do this so that both id_now and id_prev are stored in constants. (saves a bytecode per lookup and one here)
        int id_now = Clock.getRoundNum();
        int id_prev = MessageID.get(id_now - 1);
        id_now = MessageID.get(id_now);

        // Cache values for faster lookup
        int loc_length; // Length of the locations array
        int [] ints;
        int path_length;
        MapLocation [] locations;

        read_message:
        for (Message m : myRC.getAllMessages()) {
            // 1. Check if ints is null.
            // 2. Check if locations is null.
            // 3. ints needs 6 items.
            // 4. The first int must be either the current or the previous id.
            // 5. The the bitmask must be doubled.
            // 6. The mask must match the passed bitmask.
            // 7. The rebroadcast must be doubled and greater than or equal to 0.
            // 8. locations needs at least 4 items (source/dest). This step also records the length
            //    of the locations array.
            // 9. Return if the location list is too long.
            //10. The locations array must be even.
            if (  (ints = m.ints) == null
               || (locations = m.locations) == null
               || ints.length != 6
               ||!(  ints[0] == id_now
                  || ints[0] == id_prev
                  )
               || (mask = (char)ints[1]) != (ints[1]>>>16)
               || ((char)bitmask & mask) == 0
               ||!(  ints[4] == ints[5]
                  && ints[4] >= 0
                  )
               || (loc_length = locations.length) < 4
               || loc_length > 40
               || (loc_length & 1) != 0
                ) continue read_message;

            path_length = ((loc_length >> 1) - 1); //The length of the path is 1/2 of the length of the locations array  and minus 1 for the source
            path = new MapLocation[path_length];

            // Get the source if valid
            if (!locations[--loc_length].equals(locations[--loc_length]))
                continue read_message;
            source = locations[loc_length];

            // do...while to save bytecode (we know that the length > 0)
            do {
                if (!locations[--loc_length].equals(locations[--loc_length]))
                    continue read_message;
                path[--path_length] = locations[loc_length];
            } while ( loc_length > 0 );

            last_message = m; // cache the message
            return true;
        }

        source = null;
        last_message = null;
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
     * Gets the source of the last message.
     * @return The last message's path.
     */
    public MapLocation getSource() {
        return source;
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
