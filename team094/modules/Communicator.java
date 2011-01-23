package team094.modules;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class Communicator {
    private final BroadcastController comm;
    private final RobotController myRC;
    private final int DM;

    public static final int NONE = 0;
    public static final int ATTACK = 1;
    public static final int ATTACK_BUILDING = 2;
    public static final int DEFEND = 4;
    public static final int HEAL = 8;
    public static final int SCATTER = 16;

    // Cache
    private Message message;


    /**
     * Facilitates communication to and from this robot.
     *
     * Message format:
     *   ints: (round-id) (checksum) (mask) (rank) (distance from source to last ^2) (rebroadcast-rounds)
     *   strings: null
     *   locations: (source) (dest)
     *
     * Checksum:
     *     (rebroadcast-rounds)
     *     ^(distance from source to last ^2 &lt;&lt; 1)
     *     ^(rank &lt;&lt; 2)
     *     ^(mask &lt;&lt; 3)
     *     ^(destination_location &lt;&lt; 4)
     *     ^(source_location &lt;&lt; 5)
     *
     *
     * @param rp The robots RobotProperties.
     */
    public Communicator(RobotProperties rp) {
        comm = rp.comm;
        myRC = rp.myRC;
        DM = rp.DIST_MULTIPLIER;
    }

    /**
     * Clears all messages from the message queue.
     *
     * @return true if messages were cleared.
     */
    public boolean clear() {
        message = null;
        return myRC.getAllMessages().length > 0;
    }

    public boolean turnOn(int [] ids) throws GameActionException {
        if (comm == null || comm.isActive()) return false;
        comm.broadcastTurnOn(ids);
        return true;
    }

    /**
     * Relays the cached message if there is one and we should relay it.
     *
     * @return true if we relayed the message
     */
    public boolean send() throws GameActionException {
        if (comm == null || comm.isActive() || message == null || --message.ints[5] < 0) return false;
        int distance;
        if ((distance = myRC.getLocation().distanceSquaredTo(message.locations[0])) < message.ints[4]) {
            return false;
        }
        message.ints[4] = distance;
        message.ints[1] = (message.ints[5] ^ (message.ints[4]<<1) ^ (message.ints[3]<<2) ^ (message.ints[2]<<3) ^ (message.locations[1].hashCode()<<4) ^ (message.locations[0].hashCode()<<5));
        message.ints[0] = MessageID.get(Clock.getRoundNum());
        comm.broadcast(message);
        return true;
    }

    /**
     * Sends out a command to robots within range.
     *
     * @param bitmask The bitmask that describes the robots that should receive this message.
     * @param rank The rank of the situation
     * @param destination The destination.
     * @return true if the message was sent.
     */
    public boolean send(int bitmask, int rank, int rebroadcast, MapLocation destination) throws GameActionException {
        if (comm == null || destination == null || comm.isActive()) return false;

        Message message = new Message();

        message.ints = new int [6];
        message.ints[2] = bitmask;
        message.ints[3] = rank;
        message.ints[4] = 0; //distance from source
        message.ints[5] = rebroadcast;

        message.locations = new MapLocation [2];
        message.locations[0] = myRC.getLocation();
        message.locations[1] = destination;

        message.ints[1] = (message.ints[5] ^ (message.ints[4]<<1) ^ (message.ints[3]<<2) ^ (message.ints[2]<<3) ^ (message.locations[1].hashCode()<<4) ^ (message.locations[0].hashCode()<<5));

        // Put this at the end to guarentee that the round number is correct.
        message.ints[0] = MessageID.get(Clock.getRoundNum());
        comm.broadcast(message);
        return true;
    }

    /**
     * Parses incomming messages but does not pick one out for use.
     * This method also parses all other messages and prepares them for the send method it MUST be called before send.
     *
     * @return false
     */
    public boolean receive() {
        return receive(0);
    }

    /**
     * Gets the highest ranked command matching bitmask.
     * This method also parses all other messages and prepares them for the send method it MUST be called before send.
     *
     * Bitmask: 1 - Attack
     *          2 - Attack Building
     *          4 - Defend
     *          8 - Heal
     *         16 - Base
     *
     * @param bitmask The bitmask that must match the command.
     * @return true if a message was chosen.
     */
    public boolean receive(int bitmask) {
        // Do this so that both id_now and id_prev are stored in constants. (saves a bytecode per lookup and one here)
        int id_now = Clock.getRoundNum();
        int id_prev = MessageID.get(id_now - 1);
        id_now = MessageID.get(id_now);

        int [] ints;
        MapLocation [] locations;
        MapLocation myLoc = myRC.getLocation();

        int high_rank;
        //saves bytecode if we don't care about parsing a message
        if (bitmask == 0)
            high_rank = 10000;
        else
            high_rank = -1000;

        Message [] messages = myRC.getAllMessages();
        int mi = -1;

    read:
        for (int this_rank, i = messages.length; i-- > 0;) {
            if (  (ints = messages[i].ints) == null
               || (locations = messages[i].locations) == null
               || (ints.length) != 6
               ||!(  ints[0] == id_now
                  || ints[0] == id_prev
                  )
               || (locations.length) != 2
               || locations[0] == null
               || locations[1] == null
               || ints[5] < 0
               || ((ints[5]
                   ^(ints[4]<<1)
                   ^(ints[3]<<2)
                   ^(ints[2]<<3)
                   ^(locations[1].hashCode()<<4)
                   ^(locations[0].hashCode()<<5))
                  !=ints[1]
                  )
                )
            {
                messages[i] = null;
                continue read;
            }

            if ((this_rank = (ints[3] - myLoc.distanceSquaredTo(locations[1]))*DM) > high_rank && (bitmask & ints[2]) != 0) {
                mi = i;
                high_rank = this_rank;
            }
        }
        if (mi < 0) {
            this.message = null;
            return false;
        } else {
            this.message = messages[mi];
            return true;
        }
    }

    /**
     * Gets the command bitmask for the last message.
     * @return The last message's bitmask.
     */
    public int getCommand() {
        if (message == null) return 0;
        return message.ints[2];
    }

    /**
     * Gets the destination for the last message.
     * @return The last message's destination.
     */
    public MapLocation getDestination() {
        if (message == null) return null;
        return message.locations[1];
    }
}
