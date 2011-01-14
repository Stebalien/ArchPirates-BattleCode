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
    private MapLocation destination;
    private MapLocation source;
    private Message last_message;


    /**
     * Facilitates communication to and from this robot.
     *
     * Message format:
     *   ints: (round-id) (bitmask + bitmask) (distance from source to last ^2) (distance from source to last ^2) (rebroadcast num) (rebroadcast num)
     *   Strings: null
     *   MapLocations: (source) (source) (dest) (dest)
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
    // locations: [(source) (source) (dest) (dest)] (null) [repeat...]
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
     * @param destination The destination.
     * @return true if the message was sent.
     */
    public boolean send(int bitmask, int rebroadcast, MapLocation destination) throws GameActionException {
        if (comm == null || comm.isActive() || destination == null) return false;

        Message message = new Message();

        message.ints = new int [6];
        message.ints[0] = MessageID.get(Clock.getRoundNum());
        message.ints[1] = bitmask + (bitmask << 16);
        message.ints[2] = message.ints[3] = 0;
        message.ints[4] = message.ints[5] = rebroadcast;

        message.locations = new MapLocation [4];
        message.locations[0] = message.locations[1] = myRC.getLocation();
        message.locations[2] = message.locations[3] = destination;

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

        int [] ints;
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
            // 8. locations needs at least 4 items (source/dest).
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
               || locations.length != 4
                ) continue read_message;


            // Get the source if valid
            if (!locations[0].equals(source = locations[1]))
                continue read_message;

            if (!locations[2].equals(destination = locations[3]))
                continue read_message;

            last_message = m;
            return true;
        }

        source = null;
        last_message = null;
        source = null;
        destination = null;
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
        return destination;
    }
}
