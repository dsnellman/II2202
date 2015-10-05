package se.ii2202.dht.main;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ii2202.dht.appmsg.*;
import se.ii2202.dht.msg.*;
import se.ii2202.dht.object.*;
import se.ii2202.dht.timer.*;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.simulator.timed.api.TimedControler;
import se.sics.p2ptoolbox.simulator.timed.api.TimedControlerBuilder;

import java.util.*;

public class Chord extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(Chord.class);

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);


    private NodeInfo self;
    private NodeInfo firstNode;
    private NodeInfo storedAddress = new NodeInfo();
    private ArrayList<Finger> fingers = new ArrayList<>();

    private ArrayList<Item> replicaStorage = new ArrayList<>();
    private ArrayList<Item> storage = new ArrayList<>();

    private TimedControler tc;

    private int processedMsgAddCounter = 0;
    private HashMap<Integer, Integer> processingAppMsgAdd = new HashMap<>();

    private int processedMsgRingAddCounter = 0;
    private HashMap<Integer, Integer> processingAppMsgRingAdd = new HashMap<>();

    private int processedMsgLookUpCounter = 0;
    private HashMap<Integer, Integer> processingAppMsgLookUp = new HashMap<>();

    private int processedMsgRingLookUpCounter = 0;
    private HashMap<Integer, Integer> processingAppMsgRingLookUp = new HashMap<>();

    private int processedMsgClosestCounter = 0;
    private HashMap<Integer, Integer> processingAppMsgClosest = new HashMap<>();

    private Random rand = new Random();

    private RunProperties PROPERTIES;

    private final int STABILIZE_TIMEOUT = 2000;
    //private final int M;
    //private final int nRings;

    //private final int maxProcessMsgTime = 10;

    public Chord(ChordInit init) {

        self = init.selfAddress;
        firstNode = init.firstNode;
        PROPERTIES = init.properties;

        tc = init.tcb.registerComponent(self.id, this);

        //Init fingers
        fingers.add(new Finger(self.id));
        for (int k = 1; k <= PROPERTIES.M; k++) {
            int s = (int) ((self.id + Math.pow(2, k - 1)) % Math.pow(2, PROPERTIES.M));
            fingers.add(new Finger(s));
        }

        self.succ = init.firstNode;

        subscribe(handleStart, control);

        subscribe(handleJoin, network);
        subscribe(handleJoinResponse, network);
        subscribe(handleClosestFinger, network);
        subscribe(handleClosestFingerResponse, network);
        subscribe(handleSuccessor, network);
        subscribe(handleSuccessorResponse, network);
        subscribe(handlePredecessor, network);
        subscribe(handlePredecessorResponse, network);
        subscribe(handleUpdateFingerTable, network);
        subscribe(handleNotify, network);
        subscribe(handleAdd, network);
        subscribe(handleRingAdd, network);
        subscribe(handleLookUp, network);
        subscribe(handleRingLookUp, network);

        subscribe(handleStabilizerTimer, timer);

    }

    private final Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            tc.advance(Chord.this, 0);
            //log.info("{} | - Starting chord! first: {}", new Object[]{self, firstNode});

            scheduleStabilizer();

            self.pred = new NodeInfo();

            if (self == firstNode) {

                for (int i = 0; i <= PROPERTIES.M; i++) {
                    fingers.get(i).node = self;
                }

                self.pred = self;

            } else {

                trigger(new Join(self, self.succ, self), network);
            }
        }
    };

    private final Handler<Join> handleJoin = new Handler<Join>() {

        @Override
        public void handle(Join msg) {
            tc.advance(Chord.this, 0);
            //log.info("{} | Received join msg from {}", new Object[]{self, msg.getSource()});

            NodeInfo s = find_successor(msg.address.id, msg.address, TYPE.JOIN, 0, null, 0, 0, 0L, null);


            if (s != null) {
                trigger(new JoinResponse(self, msg.address, s), network);
            }
        }
    };

    private final Handler<JoinResponse> handleJoinResponse = new Handler<JoinResponse>() {

        @Override
        public void handle(JoinResponse msg) {
            tc.advance(Chord.this, 0);
            //log.info("{} => | - Joinresponse reveiced with succ {}", new Object[]{self, msg.succ});

            self.succ = msg.succ;
            storedAddress.address = msg.getSource();

            trigger(new FindSuccessor(self, storedAddress, TYPE.INIT, fingers.get(1).start, self, 1), network);

        }
    };

    private final Handler<FindSuccessor> handleSuccessor = new Handler<FindSuccessor>() {

        @Override
        public void handle(FindSuccessor msg) {
            tc.advance(Chord.this, 0);
            //log.info("{} | Received FindSuccessor msg from {}", new Object[]{self, msg.getSource()});
            //log.info("{} (Succ: {}, Pred: {}) => Find successor for id: {} type {}", new Object[]{self, self.succ.id, self.pred.id, msg.id, msg.type});
            NodeInfo succ = find_successor(msg.id, msg.address, msg.type, msg.finger, null, 0, 0, 0L, null);
            //log.info("{} (Succ: {}, Pred: {}) => Found successor directly {}", new Object[]{self, self.succ.id, self.pred.id, succ});
            if(succ != null)
                trigger(new FindSuccessorResponse(self, msg.address, msg.type, succ, msg.finger), network);

        }
    };

    private final Handler<FindSuccessorResponse> handleSuccessorResponse = new Handler<FindSuccessorResponse>() {

        @Override
        public void handle(FindSuccessorResponse msg) {
            tc.advance(Chord.this, 0);
            //log.info("{} | Received FindSuccessorResponse msg from {}", new Object[]{self, msg.getSource()});

            fingers.get(msg.finger).node = msg.succ;

            if(msg.type == TYPE.INIT){

                trigger(new Predecessor(self, self.succ, TYPE.INIT, self), network);

            } else if (msg.type == TYPE.INIT_FINGER){

                if(msg.finger == PROPERTIES.M)
                    update_others();
                else {

                    for(int i = msg.finger; i < PROPERTIES.M; i++){
                        if (between(fingers.get(i + 1).start, self.id, fingers.get(i).node.id, true, false)) {
                            fingers.get(i + 1).node = fingers.get(i).node;
                            if(i == PROPERTIES.M-1)
                                update_others();
                        } else {
                            trigger(new FindSuccessor(self, storedAddress, TYPE.INIT_FINGER, fingers.get(i + 1).start, self, i +1), network);
                            break;
                        }
                    }

                }
            }
        }
    };

    private final Handler<ClosestFinger> handleClosestFinger = new Handler<ClosestFinger>() {

        @Override
        public void handle(ClosestFinger msg) {
            tc.advance(Chord.this, 0);
            //log.info("{} | Received ClosestFinger msg from {}", new Object[]{self, msg.getSource()});

            NodeInfo nPrime = closest_finger(msg.id);
            trigger(new ClosestFingerResponse(self, msg.sender, msg.returnAddress, msg.type, msg.id, nPrime, msg.finger, msg.item, msg.lookupID, msg.msgCounter, msg.lookupType, msg.startInnerLactency), network);
        }

    };

    private final Handler<ClosestFingerResponse> handleClosestFingerResponse = new Handler<ClosestFingerResponse>() {

        @Override
        public void handle(ClosestFingerResponse msg) {

            //TODO latency when lookup or add
            tc.advance(Chord.this, 0);
            //log.info("{} | Received ClosestFingerResponse msg from {}", new Object[]{self, msg.getSource()});

            NodeInfo nPrime = msg.foundedAddress;
            //log.info("{} reveive closestfinger response with returnaddress: {}", new Object[]{self, msg.returnAddress});

            while (!between(msg.id, nPrime.id, nPrime.succ.id, false, true)) {

                msg.msgCounter++;
                //nPrime = closest_finger(msg.id);
                //if(msg.lookupID != 0)
                //log.info("{} => sending closest, counter {} nprime {} key {}", new Object[]{self, msg.lookUpCounter, nPrime, msg.id});
                trigger(new ClosestFinger(self, nPrime, msg.returnAddress, msg.type, msg.id, msg.foundedAddress, msg.finger, msg.item, msg.lookupID, msg.msgCounter, msg.lookupType, msg.startInnerLatency), network);
                return;
            }

            if (msg.type == TYPE.JOIN) {
                trigger(new JoinResponse(self, msg.returnAddress, nPrime.succ), network);
            } else if (msg.type == TYPE.ADD) {
                msg.msgCounter++;
                trigger(new RingAdd(self, nPrime.succ, msg.type, msg.item, msg.lookupID, msg.msgCounter, msg.returnAddress, msg.startInnerLatency), network);
            } else if (msg.type == TYPE.LOOKUP) {
                msg.msgCounter++;
                trigger(new RingLookUp(self, nPrime.succ, msg.returnAddress, msg.id, nPrime, msg.lookupID, msg.msgCounter, msg.lookupType, msg.startInnerLatency), network);
            } else if (msg.type == TYPE.OTHERS) {
                trigger(new UpdateFingerTable(self, nPrime, self, msg.finger), network);
            } else {
                trigger(new FindSuccessorResponse(self, msg.returnAddress, msg.type, nPrime.succ, msg.finger), network);
            }

        }

    };

    private final Handler<Predecessor> handlePredecessor = new Handler<Predecessor>() {

        @Override
        public void handle(Predecessor msg) {
            tc.advance(Chord.this, 0);
            //log.info("{} | Received Predecessor msg from {}, time: {}", new Object[]{self, msg.getSource(), System.currentTimeMillis()});

            trigger(new PredecessorResponse(self, msg.node, msg.type, self.pred), network);
            if(msg.type == TYPE.INIT){
                self.pred = msg.node;
            }

        }
    };

    private final Handler<PredecessorResponse> handlePredecessorResponse = new Handler<PredecessorResponse>() {

        @Override
        public void handle(PredecessorResponse msg) {
            tc.advance(Chord.this, 0);
            //log.info("{} | Received PredecessorResponse msg from {}, time: {}", new Object[]{self, msg.getSource(), System.currentTimeMillis()});
            //log.info("{} (Succ: {}, Pred: {}) => Received Predecessorresponse: {}", new Object[]{self, self.succ.id, self.pred.id, msg.node});
            if(msg.type == TYPE.INIT){
                self.pred = msg.node;

                for(int i = 1; i < PROPERTIES.M; i++){
                    if (between(fingers.get(i + 1).start, self.id, fingers.get(i).node.id, true, false)) {
                        //log.info("{} (Succ: {}, Pred: {}) => Finger {} to {}", new Object[]{self, self.succ.id, self.pred.id, i+1, fingers.get(i).node.id});
                        fingers.get(i + 1).node = fingers.get(i).node;
                        if(i == PROPERTIES.M-1)
                            update_others();
                    } else {
                        trigger(new FindSuccessor(self, storedAddress, TYPE.INIT_FINGER, fingers.get(i + 1).start, self, i +1), network);
                        break;
                    }
                }
            }
            else if(msg.type == TYPE.STABILIZE){

                if(between(msg.node.id, self.id, self.succ.id, false, false)){
                    //log.info("{} new succ: {}", new Object[]{self, msg.node});
                    self.succ = msg.node;
                }

                trigger(new Notify(self, self.succ, self), network);


                //FIX_FINGERS
                int index = rand.nextInt(PROPERTIES.M) + 1;
                //log.info("{} Fixing finger ({}).start: {}", new Object[]{self, index, fingers.get(index).start});
                NodeInfo succ = find_successor(fingers.get(index).start, self, TYPE.FIX_FINGER, index, null, 0,0,0L, null);
                if(succ != null)
                    fingers.get(index).node = succ;

            }
        }
    };

    private final Handler<UpdateFingerTable> handleUpdateFingerTable = new Handler<UpdateFingerTable>() {

        @Override
        public void handle(UpdateFingerTable msg) {
            tc.advance(Chord.this, 0);
            //log.info("Update finger table");
            if (between(msg.node.id, self.id, fingers.get(msg.i).node.id, true, false)) {
                fingers.get(msg.i).node = msg.node;
                trigger(new UpdateFingerTable(self, self.pred, msg.node, msg.i), network);
            }

        }
    };

    private final Handler<Notify> handleNotify = new Handler<Notify>() {

        @Override
        public void handle(Notify msg) {
            tc.advance(Chord.this, 0);
            //log.info("{} | Notify", new Object[]{self});
            if(self.pred == null || between(msg.node.id, self.pred.id, self.id, false ,false)){
                self.pred = msg.node;
            }

        }
    };

    private final Handler<Add> handleAdd = new Handler<Add>() {

        @Override
        public void handle(Add msg) {
            tc.advance(Chord.this, PROPERTIES.constantProccessTime);

            if (msg.startInnerLatency == 0L)
                msg.startInnerLatency = System.currentTimeMillis();

            if (between(msg.item.key, self.pred.id, self.id, false, true)) {
                storage.add(msg.item);
                trigger(new AddResponse(self, msg.returnAddress, msg.item.key, msg.id, 1,msg.msgCounter, msg.startInnerLatency, System.currentTimeMillis()), network);
            } else {
                msg.msgCounter++;
                NodeInfo s = find_successor(msg.item.key, msg.returnAddress, msg.type, 0, msg.item, msg.id,msg.msgCounter, msg.startInnerLatency, null);
                if (s != null) {
                    trigger(new RingAdd(self, s, msg.type, msg.item, msg.id, msg.msgCounter, msg.returnAddress, msg.startInnerLatency), network);
                }
            }
        }
    };


    private final Handler<RingAdd> handleRingAdd = new Handler<RingAdd>() {

        @Override
        public void handle(RingAdd msg) {
            tc.advance(Chord.this, PROPERTIES.constantProccessTime);

            if (between(msg.item.key, self.pred.id, self.id, false, true)) {
                if(msg.type == TYPE.ADD){
                    storage.add(msg.item);
                    trigger(new AddResponse(self, msg.returnAddress, msg.item.key, msg.id, 1, msg.msgCounter, msg.startInnerLatency, System.currentTimeMillis()), network);

                }
                else if(msg.type == TYPE.ADDREPLICA){
                    replicaStorage.add(msg.item);
                    //log.info("{} => Add item {} in replica", new Object[]{self, msg.item});
                    trigger(new AddResponse(self, msg.returnAddress, msg.item.key, msg.id, 2, msg.msgCounter, msg.startInnerLatency, System.currentTimeMillis()), network);
                }
            } else {
                msg.msgCounter++;
                NodeInfo s = find_successor(msg.item.key, msg.returnAddress, msg.type, 0, msg.item, msg.id, msg.msgCounter, msg.startInnerLatency, null);
                if (s != null) {
                    trigger(new RingAdd(self, s, msg.type, msg.item, msg.id, msg.msgCounter, msg.returnAddress, msg.startInnerLatency), network);
                }
            }


        }
    };

    private final Handler<LookUp> handleLookUp = new Handler<LookUp>() {

        @Override
        public void handle(LookUp msg) {

            if(msg.type == LookUp.LookUpTYPE.LOOKUP)
                tc.advance(Chord.this, PROPERTIES.constantProccessTime);
            else
                tc.advance(Chord.this, 0);
            //log.info("{}: Received lookup msg for key {} with counter : {} - Time: {}, returnaddress: {}", new Object[]{self,msg.key, msg.counter, System.nanoTime(), msg.returnAddress});

            if (msg.startInnerLatency == 0L)
                msg.startInnerLatency = System.currentTimeMillis();

            if (between(msg.key, self.pred.id, self.id, false, true)) {
                //log.info("{}: found right node {} with counter : {}", new Object[]{self,msg.key, msg.counter});
                Long endInnerLatency = System.currentTimeMillis();

                if (storage.contains(new Item(msg.key))) {

                    trigger(new LookUpResponse(self, msg.returnAddress, msg.key, LookUpResponse.LookUp.InStore, self, msg.id, msg.counter, msg.type, msg.startInnerLatency, endInnerLatency), network);

                } else if (replicaStorage.contains(new Item(msg.key))){
                    trigger(new LookUpResponse(self, msg.returnAddress, msg.key, LookUpResponse.LookUp.InReplica, self, msg.id, msg.counter, msg.type, msg.startInnerLatency, endInnerLatency), network);

                } else {

                    trigger(new LookUpResponse(self, msg.returnAddress, msg.key, LookUpResponse.LookUp.NotFound, self, msg.id, msg.counter, msg.type, msg.startInnerLatency, endInnerLatency), network);
                }

            } else {
                msg.counter++;
                NodeInfo s = find_successor(msg.key, msg.returnAddress, TYPE.LOOKUP, 0, null, msg.id, msg.counter, msg.startInnerLatency, msg.type);
                if (s != null) {
                    //log.info("{}: sending lookup msg to ring for key {} with counter : {}, time: {}", new Object[]{self,msg.key, msg.counter, System.nanoTime()});
                    trigger(new RingLookUp(self, s, msg.returnAddress, msg.key, s, msg.id, msg.counter, msg.type, msg.startInnerLatency), network);
                }
            }

        }
    };

    private final Handler<RingLookUp> handleRingLookUp = new Handler<RingLookUp>() {

        @Override
        public void handle(RingLookUp msg) {
            if(msg.type == LookUp.LookUpTYPE.LOOKUP)
                tc.advance(Chord.this, PROPERTIES.constantProccessTime);
            else
                tc.advance(Chord.this, 0);

            //log.info("{}: Received ringlookup msg for key {} with counter : {}, time: {}, returnaddress; {}", new Object[]{self,msg.key, msg.counter, System.nanoTime(), msg.returnAddress});
            if (between(msg.key, self.pred.id, self.id, false, true)) {
                //log.info("{}: found right node for key {} with counter : {}", new Object[]{self,msg.key, msg.counter});
                Long endInnerLatency = System.currentTimeMillis();
                if (storage.contains(new Item(msg.key))) {

                    trigger(new LookUpResponse(self, msg.returnAddress, msg.key, LookUpResponse.LookUp.InStore, self, msg.id, msg.counter, msg.type, msg.startInnerLatency, endInnerLatency), network);

                } else if (replicaStorage.contains(new Item(msg.key))){
                    trigger(new LookUpResponse(self, msg.returnAddress, msg.key, LookUpResponse.LookUp.InReplica, self, msg.id, msg.counter, msg.type, msg.startInnerLatency, endInnerLatency), network);

                } else {

                    trigger(new LookUpResponse(self, msg.returnAddress, msg.key, LookUpResponse.LookUp.NotFound, self, msg.id, msg.counter, msg.type, msg.startInnerLatency, endInnerLatency), network);
                }
            } else {
                msg.counter++;
                NodeInfo s = find_successor(msg.key, msg.returnAddress, TYPE.LOOKUP, 0, null, msg.id, msg.counter, msg.startInnerLatency, msg.type);
                if (s != null) {
                    //log.info("{}: Sending ringlookup msg for key {} with counter : {}, Time: {}", new Object[]{self,msg.key, msg.counter, System.nanoTime()});
                    trigger(new RingLookUp(self, s, msg.returnAddress, msg.key, s, msg.id, msg.counter, msg.type, msg.startInnerLatency), network);
                }
            }

        }
    };



    public void update_others() {

        for (int i = 1; i <= PROPERTIES.M; i++) {
            int x = (int) Math.pow(2, i - 1);
            if (self.id - x >= 0) {
                x = self.id - x;
            } else {
                x = (PROPERTIES.M - 1) + x;
            }

            NodeInfo p = find_predecessor(x, self, TYPE.OTHERS, i, null,0, 0, null, 0L);
            if (p != null) {
                trigger(new UpdateFingerTable(self, p, self, i), network);
            }

        }

    }

    public NodeInfo find_successor(int id, NodeInfo returnAddress, TYPE type, int index, Item item, int lookupID, int msgCounter, Long startInnerLatency, LookUp.LookUpTYPE lookupType) {
        //log.info("{} find successor for id: {}", new Object[]{self, id});
        NodeInfo nPrime = find_predecessor(id, returnAddress, type, index, item, lookupID, msgCounter, lookupType, startInnerLatency);
        if (nPrime != null) {
            return nPrime.succ;
        }

        return null;
    }

    public NodeInfo find_predecessor(int id, NodeInfo returnAddress, TYPE type, int i, Item item, int lookupID, int msgCounter, LookUp.LookUpTYPE lookupType, Long startInnerLatency) {

        NodeInfo nPrime = self;

        while (!between(id, nPrime.id, nPrime.succ.id, false, true)) {
            nPrime = closest_finger(id);
            //if(lookupID > 0)
            //log.info("{} sending cloest finger to node {} with key {}", new Object[]{self, nPrime, id});
            trigger(new ClosestFinger(self, nPrime, returnAddress, type, id, null, i, item, lookupID, msgCounter, lookupType, startInnerLatency), network);
            return null;
        }
        return nPrime;
    }

    public NodeInfo closest_finger(int id) {
        //log.info("{} |cloest finger for id : {}", new Object[]{self, id});
        for (int i = PROPERTIES.M; i >= 1; i--) {
            if (between(fingers.get(i).node.id, self.id, id, false, false)) {
                if(fingers.get(i).node.id == -1)
                    log.info("Finger = null");
                return fingers.get(i).node;
            }
        }

        return self.succ;
    }

    private final Handler<StabilizerTimer> handleStabilizerTimer = new Handler<StabilizerTimer>() {
        @Override
        public void handle(StabilizerTimer event) {
            tc.advance(Chord.this, 0);
            //log.info("{} | Stabilizing!", new Object[]{self});
            //print_node();
            trigger(new Predecessor(self, self.succ, TYPE.STABILIZE, self), network);

        }
    };

    private void scheduleStabilizer() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(STABILIZE_TIMEOUT, STABILIZE_TIMEOUT);
        StabilizerTimer sc = new StabilizerTimer(spt);
        spt.setTimeoutEvent(sc);

        trigger(spt, timer);
    }

    public boolean between(int key, int from, int to, boolean incFrom, boolean incTo) {
        //log.info("betweeen: key {}, from {}, to {}", new Object[]{key, from, to});
        if(key < 0)
            return false;

        if (from == to) {
            return true;
        }

        if (key == from && incFrom) {
            return true;
        }

        if (key == to && incTo) {
            return true;
        }

        if (key > from && key < to) {
            return true;
        }

        return from > to && (key > from || key < to);

    }

    public void print_node() {
        log.info("\nPrinting node (id: {}, in ring{})  information: ", new Object[]{self.id, self.ring});
        for (int i = 1; i < fingers.size(); i++) {
            log.info("{} => Succ: {} , Pred: {} | - Finger[{}].start: {}, .node: {}, ", new Object[]{self.id, self.succ.id, self.pred.id, i, fingers.get(i).start, fingers.get(i).node});
        }

        log.info("{} => Succ: {} , Pred: {} | - Storage: {}", new Object[]{self.id, self.succ.id, self.pred.id, storage.toString()});
        log.info("{} => Succ: {} , Pred: {} | - Replicastorage: {}", new Object[]{self.id, self.succ.id, self.pred.id, replicaStorage.toString()});

    }



    public static class ChordInit extends Init<Chord> {

        public TimedControlerBuilder tcb;
        public NodeInfo selfAddress;
        public NodeInfo firstNode;
        public RunProperties properties;

        public ChordInit(NodeInfo self, NodeInfo firstNode, RunProperties properties, TimedControlerBuilder tcb) {
            this.selfAddress = self;
            this.firstNode = firstNode;
            this.properties = properties;
            this.tcb = tcb;
        }



    }

}

