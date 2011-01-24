package team094.castes;

import team094.modules.*;
import battlecode.common.*;

public class Soldier extends Caste {
    private static enum State {
        FOLLOW,
        SPREAD,
        SEARCH,
        GO,
        ATTACK,
        YIELD
    }
    private State state;

    private final Attacker attacker;
    private MapLocation target;
    private boolean lost, following;
    private SensorController sensor;
    private int msgMask,
                timer;

    public Soldier(RobotProperties rp){
        super(rp);
        state = State.FOLLOW;
        attacker = new Attacker(rp, true);
        sensor = rp.sensor;

        msgMask = Communicator.ATTACK;
    }

    public void SM() {
        while(true) {
            try {
                switch(state) {
                    case FOLLOW:
                        follow();
                        break;
                    //case SPREAD:
                    //    spread();
                    //    break;
                    //case SEARCH:
                    //    search();
                    //    break;
                    case GO:
                        go();
                        break;
                    case ATTACK:
                        attack();
                        break;
                }
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }

            //System.out.println(Clock.getBytecodeNum());
            myRC.yield();
        }
    }

    /**
     * Follow the closest robot if it is facing away from you.
     *
     * This code stalks robots on your team to keep them clumped.
     * I could easily make it migrate more but then the robots get too far away from eachother and are easily picked off.
     */
    private void follow() throws GameActionException {
        RobotInfo tmpRobotInfo, robotInfo = null;
        int tmpDistSq = 1000;
        int minDistSq = 1000;
        MapLocation l, myLoc = myRC.getLocation();
        // Should I attack
        if((l = attacker.autoFire()) != null) {
            nav.setDestination(l, 6);
            nav.bugNavigate(true);
            state = State.ATTACK;
            com.clear();
            com.send(Communicator.ATTACK, attacker.rank, 10, myLoc);
            return;
        }

        // I CAN HAZ MSG
        if (com.receive(msgMask)) {
            nav.setDestination(com.getDestination());
            nav.bugNavigate(true);
            state = State.GO;
            com.send();
            return;
        }

        // Where is everybody
        if (!following) {
            for (Robot r : sensor.senseNearbyGameObjects(Robot.class)) {
                if (r.getTeam() == myRP.myTeam) {
                    tmpRobotInfo = sensor.senseRobotInfo(r);
                    if (tmpRobotInfo.chassis == Chassis.LIGHT
                       && (tmpDistSq = myLoc.distanceSquaredTo(tmpRobotInfo.location)) < minDistSq)
                    {
                        minDistSq = tmpDistSq;
                        robotInfo = tmpRobotInfo;
                    }
                }
            }
        }

        // Didn't find anyone
        if (robotInfo == null) {
            following = false;
            // Lost?
            if (lost) {
                if (!nav.bugNavigate(false)) {
                    nav.setDestination(myLoc.add(myRC.getDirection(), 100));
                    nav.bugNavigate(false);
                }
                return;
            }

            // Maybe they are behind me.
            lost = true;
            nav.setDirection(myRC.getDirection().opposite());
            return;
        } else {
            lost = false;

            // Don't follow if they are facing me.
            Direction dir = robotInfo.location.directionTo(myLoc);
            int dist;
            if (dir == robotInfo.direction || dir == robotInfo.direction.rotateRight() || dir == robotInfo.direction.rotateLeft()) {
                following = false;
                lost = true;
                nav.setDestination(myLoc.add(myRC.getDirection().opposite(), 100));
                nav.bugNavigate(true);
                return;
            } else if ((dist = myLoc.distanceSquaredTo(robotInfo.location)) >= 16) {
                following = true;
                nav.move(true);
            } else if (dist < 16) {
                following = true;
                nav.move(false);
            } //else {
              //  nav.setDirection(myRC.getDirection().opposite());
            //}
        }
    }


    private void spread() throws GameActionException {
        // Attempt to stay 5 units from all objects
        MapLocation myLoc = myRC.getLocation();
        MapLocation dest = myLoc;

        // Gather info about your front
        for(Robot r: myRP.sensor.senseNearbyGameObjects(Robot.class)) {
            RobotInfo ri = myRP.sensor.senseRobotInfo(r);
            if(ri.chassis != Chassis.LIGHT)
                continue;

            Direction rd = myLoc.directionTo(ri.location);
            int dist = (int)Math.sqrt(ri.location.distanceSquaredTo(myLoc));

            if(dist >= 4)
                dest = dest.add(rd.opposite(), dist-4);
            else
                dest = dest.add(rd, 4-dist);
        }
        nav.setDirection(myRC.getDirection().opposite());

        attacker.autoFire();
        com.receive();
        com.send();
        myRC.yield();

        // And about your back
        for(Robot r: myRP.sensor.senseNearbyGameObjects(Robot.class)) {
            RobotInfo ri = myRP.sensor.senseRobotInfo(r);
            if(ri.chassis != Chassis.LIGHT)
                continue;

            Direction rd = ri.location.directionTo(myLoc);
            int dist = (int)Math.sqrt(ri.location.distanceSquaredTo(myLoc));

            if(dist >= 4)
                dest = dest.add(rd.opposite(), dist-4);
            else
                dest = dest.add(rd, 4-dist);
        }
        nav.setDirection(myRC.getDirection().opposite());

        com.receive(msgMask);
        com.send();
        attacker.autoFire();
        myRC.yield();

        nav.setDestination(dest, 1.9);
        nav.bugNavigate(false);
        com.receive(msgMask);
        if((myLoc = attacker.autoFire()) != null) {
            state = state.ATTACK;
            com.send(Communicator.ATTACK, attacker.rank, 10, myLoc);
        } else {
            com.send();
        }
    }



    private void search() throws GameActionException {
        MapLocation l;
        if((l = attacker.autoFire()) != null) {
            nav.setDestination(l, 6);
            nav.bugNavigate(true);
            state = State.ATTACK;
        } else if(timer > 7) {
            state = State.SPREAD;
        } else {
            nav.rotate(true, 1);
            timer++;
        }
    }

    private void go() throws GameActionException {
        MapLocation l;
        if((l = attacker.autoFire()) != null) {
            nav.setDestination(l, 6);
            nav.bugNavigate(true);
            state = State.ATTACK;
        } else if(nav.bugNavigate(true)) {
            //timer = 0;
            //state = state.SEARCH;
            state = state.FOLLOW;
        }
        com.receive();
        com.send();
    }

    private void attack() throws GameActionException {
        MapLocation l;
        if((l = attacker.autoFire()) == null) {
            if(target != null) {
                nav.setDestination(target, 6);
                state = state.GO;
            } else {
                //timer = 0;
                //state = State.SEARCH;
                state = State.FOLLOW;
            }
        } else {
            nav.setDestination(l);
        }

        if(l != null && myRC.getLocation().distanceSquaredTo(l) <= 25)
            nav.move(false);
        else
            nav.bugNavigate(true);
    }
}
