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

    private final int STABILIZE_TIMEOUT = 2000;
    private final int M;
    private final int nRings;

    private final int maxProcessMsgTime = 7;
    private final int minProcessMsgTime = 3;

    public Chord(ChordInit init) {

        self = init.selfAddress;
        firstNode = init.firstNode;
        M = init.m;
        nRings = init.nRings;

        //Init fingers
        fingers.add(new Finger(self.id));
        for (int k = 1; k <= M; k++) {
            int s = (int) ((self.id + Math.pow(2, k - 1)) % Math.pow(2, M));
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
        subscribe(handlePing, network);
        subscribe(handleRingPing, network);

        subscribe(handleAddTimer, timer);
        subscribe(handleRingAddTimer, timer);
        subscribe(handleClosestTimer, timer);
        subscribe(handleLookUpTimer, timer);
        subscribe(handleRingLookUpTimer, timer);
        subscribe(handleStabilizerTimer, timer);

    }

    private final Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {

            log.info("{} | - Starting chord! first: {}", new Object[]{self, firstNode});

            scheduleStabilizer();

            self.pred = new NodeInfo();

            if (self == firstNode) {

                for (int i = 0; i <= M; i++) {
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
            //log.info("{} | Received join msg from {}", new Object[]{self, msg.getSource()});

            NodeInfo s = find_successor(msg.address.id, msg.address, TYPE.JOIN, 0, null, 0, 0, 0L);


            if (s != null) {
                trigger(new JoinResponse(self, msg.address, s), network);
            }
        }
    };

    private final Handler<JoinResponse> handleJoinResponse = new Handler<JoinResponse>() {

        @Override
        public void handle(JoinResponse msg) {

            //log.info("{} => | - Joinresponse reveiced with succ {}", new Object[]{self, msg.succ});

            self.succ = msg.succ;
            storedAddress.address = msg.getSource();

            trigger(new FindSuccessor(self, storedAddress, TYPE.INIT, fingers.get(1).start, self, 1), network);

        }
    };

    private final Handler<FindSuccessor> handleSuccessor = new Handler<FindSuccessor>() {

        @Override
        public void handle(FindSuccessor msg) {
            //log.info("{} | Received FindSuccessor msg from {}", new Object[]{self, msg.getSource()});
            //log.info("{} (Succ: {}, Pred: {}) => Find successor for id: {} type {}", new Object[]{self, self.succ.id, self.pred.id, msg.id, msg.type});
            NodeInfo succ = find_successor(msg.id, msg.address, msg.type, msg.finger, null, 0, 0, 0L);
            //log.info("{} (Succ: {}, Pred: {}) => Found successor directly {}", new Object[]{self, self.succ.id, self.pred.id, succ});
            if(succ != null)
                trigger(new FindSuccessorResponse(self, msg.address, msg.type, succ, msg.finger), network);

        }
    };

    private final Handler<FindSuccessorResponse> handleSuccessorResponse = new Handler<FindSuccessorResponse>() {

        @Override
        public void handle(FindSuccessorResponse msg) {
            //log.info("{} | Received FindSuccessorResponse msg from {}", new Object[]{self, msg.getSource()});

            fingers.get(msg.finger).node = msg.succ;

            if(msg.type == TYPE.INIT){

                trigger(new Predecessor(self, self.succ, TYPE.INIT, self), network);

            } else if (msg.type == TYPE.INIT_FINGER){

                if(msg.finger == M)
                    update_others();
                else {

                    for(int i = msg.finger; i < M; i++){
                        if (between(fingers.get(i + 1).start, self.id, fingers.get(i).node.id, true, false)) {
                            fingers.get(i + 1).node = fingers.get(i).node;
                            if(i == M-1)
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

            //log.info("{} | Received ClosestFinger msg from {}", new Object[]{self, msg.getSource()});

            NodeInfo nPrime = closest_finger(msg.id);
            trigger(new ClosestFingerResponse(self, msg.sender, msg.returnAddress, msg.type, msg.id, nPrime, msg.finger, msg.item, msg.lookupID, msg.msgCounter, msg.startInnerLactency), network);
        }

    };

    private final Handler<ClosestFingerResponse> handleClosestFingerResponse = new Handler<ClosestFingerResponse>() {

        @Override
        public void handle(ClosestFingerResponse msg) {
            //log.info("{} | Received ClosestFingerResponse msg from {}", new Object[]{self, msg.getSource()});

            if(msg.type == TYPE.ADD || msg.type == TYPE.ADDREPLICA || msg.type == TYPE.LOOKUP) {

                //SIMULATING INTERNAL LATENCY
                int value = rand.nextInt(maxProcessMsgTime + minProcessMsgTime) + minProcessMsgTime;
                processingAppMsgClosest.put(processedMsgClosestCounter, value);
                int sleep = 0;
                Iterator it = processingAppMsgClosest.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    sleep += (Integer) pair.getValue();
                }

                //log.info("{} simulating inner latency for ring add id: {} sleep: {}, msg: {} counter {}", new Object[]{self, msg.id, sleep, processingAppMsgClosest.size(), processedMsgClosestCounter});

                ScheduleTimeout spt = new ScheduleTimeout(sleep);
                InnerLatencyTimerClosest sc = new InnerLatencyTimerClosest(spt, processedMsgClosestCounter, msg);
                spt.setTimeoutEvent(sc);
                trigger(spt, timer);
                processedMsgClosestCounter++;


            } else {


                NodeInfo nPrime = msg.foundedAddress;
                //log.info("{} reveive closestfinger response with returnaddress: {}", new Object[]{self, msg.returnAddress});

                while (!between(msg.id, nPrime.id, nPrime.succ.id, false, true)) {

                    msg.msgCounter++;
                    //nPrime = closest_finger(msg.id);
                    //if(msg.lookupID != 0)
                    //log.info("{} => sending closest, counter {} nprime {} key {}", new Object[]{self, msg.lookUpCounter, nPrime, msg.id});
                    trigger(new ClosestFinger(self, nPrime, msg.returnAddress, msg.type, msg.id, msg.foundedAddress, msg.finger, msg.item, msg.lookupID, msg.msgCounter, msg.startInnerLatency), network);
                    return;
                }

                if (msg.type == TYPE.JOIN) {
                    trigger(new JoinResponse(self, msg.returnAddress, nPrime.succ), network);
                } else if (msg.type == TYPE.ADD || msg.type == TYPE.ADDREPLICA) {
                    trigger(new RingAdd(self, nPrime.succ, msg.type, msg.item, msg.lookupID, msg.msgCounter, msg.returnAddress, msg.startInnerLatency), network);
                } else if (msg.type == TYPE.LOOKUP) {
                    msg.msgCounter++;
                    trigger(new RingLookUp(self, nPrime.succ, msg.returnAddress, msg.id, nPrime, msg.lookupID, msg.msgCounter, msg.startInnerLatency), network);
                } else if (msg.type == TYPE.OTHERS) {
                    trigger(new UpdateFingerTable(self, nPrime, self, msg.finger), network);
                } else {
                    trigger(new FindSuccessorResponse(self, msg.returnAddress, msg.type, nPrime.succ, msg.finger), network);
                }
            }
        }

    };

    private final Handler<InnerLatencyTimerClosest> handleClosestTimer = new Handler<InnerLatencyTimerClosest>() {

        @Override
        public void handle(InnerLatencyTimerClosest timer) {

            processingAppMsgClosest.remove(timer.msgId);

            ClosestFingerResponse msg = timer.closestFingerResponse;

            NodeInfo nPrime = msg.foundedAddress;
            //log.info("{} reveive closestfinger response with returnaddress: {}", new Object[]{self, msg.returnAddress});

            while (!between(msg.id, nPrime.id, nPrime.succ.id, false, true)) {

                msg.msgCounter++;
                //nPrime = closest_finger(msg.id);
                //if(msg.lookupID != 0)
                //log.info("{} => sending closest, counter {} nprime {} key {}", new Object[]{self, msg.lookUpCounter, nPrime, msg.id});
                trigger(new ClosestFinger(self, nPrime, msg.returnAddress, msg.type, msg.id, msg.foundedAddress, msg.finger, msg.item, msg.lookupID, msg.msgCounter, msg.startInnerLatency), network);
                return;
            }

            if (msg.type == TYPE.ADD || msg.type == TYPE.ADDREPLICA) {
                trigger(new RingAdd(self, nPrime.succ, msg.type, msg.item, msg.lookupID, msg.msgCounter, msg.returnAddress, msg.startInnerLatency), network);
            } else if (msg.type == TYPE.LOOKUP) {
                msg.msgCounter++;
                trigger(new RingLookUp(self, nPrime.succ, msg.returnAddress, msg.id, nPrime, msg.lookupID, msg.msgCounter, msg.startInnerLatency), network);
            }

        }
    };

    private final Handler<Predecessor> handlePredecessor = new Handler<Predecessor>() {

        @Override
        public void handle(Predecessor msg) {
            //log.info("{} | Received Predecessor msg from {}", new Object[]{self, msg.getSource()});

            trigger(new PredecessorResponse(self, msg.node, msg.type, self.pred), network);
            if(msg.type == TYPE.INIT){
                self.pred = msg.node;
            }

        }
    };

    private final Handler<PredecessorResponse> handlePredecessorResponse = new Handler<PredecessorResponse>() {

        @Override
        public void handle(PredecessorResponse msg) {
            //log.info("{} | Received PredecessorResponse msg from {}", new Object[]{self, msg.getSource()});
            //log.info("{} (Succ: {}, Pred: {}) => Received Predecessorresponse: {}", new Object[]{self, self.succ.id, self.pred.id, msg.node});
            if(msg.type == TYPE.INIT){
                self.pred = msg.node;

                for(int i = 1; i < M; i++){
                    if (between(fingers.get(i + 1).start, self.id, fingers.get(i).node.id, true, false)) {
                        //log.info("{} (Succ: {}, Pred: {}) => Finger {} to {}", new Object[]{self, self.succ.id, self.pred.id, i+1, fingers.get(i).node.id});
                        fingers.get(i + 1).node = fingers.get(i).node;
                        if(i == M-1)
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
                int index = rand.nextInt(M) + 1;
                //log.info("{} Fixing finger ({}).start: {}", new Object[]{self, index, fingers.get(index).start});
                NodeInfo succ = find_successor(fingers.get(index).start, self, TYPE.FIX_FINGER, index, null, 0,0, 0L);
                if(succ != null)
                    fingers.get(index).node = succ;

            }
        }
    };

    private final Handler<UpdateFingerTable> handleUpdateFingerTable = new Handler<UpdateFingerTable>() {

        @Override
        public void handle(UpdateFingerTable msg) {
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
            //log.info("{} | Notify", new Object[]{self});
            if(self.pred == null || between(msg.node.id, self.pred.id, self.id, false ,false)){
                self.pred = msg.node;
            }

        }
    };

    private final Handler<Add> handleAdd = new Handler<Add>() {

        @Override
        public void handle(Add msg) {


            if (msg.startInnerLatency == 0L)
                msg.startInnerLatency = System.currentTimeMillis();


            //SIMULATING INTERNAL LATENCY
            int value = rand.nextInt(maxProcessMsgTime + minProcessMsgTime) + minProcessMsgTime;
            processingAppMsgAdd.put(processedMsgAddCounter, value);
            int sleep = 0;
            Iterator it = processingAppMsgAdd.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                sleep += (Integer) pair.getValue();
            }

            //log.info("{} simulating inner latency for add id: {} value }{} sleep: {}, msg: {} counter {}", new Object[]{self, msg.id, value, sleep, processingAppMsgAdd.size(), processedMsgAddCounter});

            ScheduleTimeout spt = new ScheduleTimeout(sleep);
            InnerLatencyTimerAdd sc = new InnerLatencyTimerAdd(spt, processedMsgAddCounter, msg);
            spt.setTimeoutEvent(sc);
            trigger(spt, timer);
            processedMsgAddCounter++;
        }
    };

    private final Handler<InnerLatencyTimerAdd> handleAddTimer = new Handler<InnerLatencyTimerAdd>() {

        @Override
        public void handle(InnerLatencyTimerAdd timer) {

            processingAppMsgAdd.remove(timer.msgId);

            Add msg = timer.add;

            if (between(msg.item.key, self.pred.id, self.id, false, true)) {
                if(msg.type == TYPE.ADD){
                    storage.add(msg.item);
                    trigger(new AddResponse(self, msg.returnAddress, msg.item.key, msg.id, 1,msg.msgCounter, msg.startInnerLatency, System.currentTimeMillis()), network);


                    //log.info("{} => Add item {}", new Object[]{self, msg.item});

                    if(!msg.replicaAddress.isEmpty()){
                        for(NodeInfo node : msg.replicaAddress){
                            trigger(new Add(self, node, TYPE.ADDREPLICA, msg.item, msg.id, msg.returnAddress, msg.startInnerLatency, null), network);
                        }

                    }


                }
                else if(msg.type == TYPE.ADDREPLICA){
                    replicaStorage.add(msg.item);
                    //log.info("{} => Add item {} in replica", new Object[]{self, msg.item});
                    trigger(new AddResponse(self, msg.returnAddress, msg.item.key, msg.id, 2,msg.msgCounter, msg.startInnerLatency, System.currentTimeMillis()), network);
                }
            } else {
                msg.msgCounter++;
                NodeInfo s = find_successor(msg.item.key, msg.returnAddress, msg.type, 0, msg.item, msg.id,msg.msgCounter, msg.startInnerLatency);
                if (s != null) {
                    trigger(new RingAdd(self, s, msg.type, msg.item, msg.id, msg.msgCounter, msg.returnAddress, msg.startInnerLatency), network);
                }
            }

        }
    };

    private final Handler<RingAdd> handleRingAdd = new Handler<RingAdd>() {

        @Override
        public void handle(RingAdd msg) {


            //SIMULATING INTERNAL LATENCY
            int value = rand.nextInt(maxProcessMsgTime + minProcessMsgTime) + minProcessMsgTime;
            processingAppMsgRingAdd.put(processedMsgRingAddCounter, value);
            int sleep = 0;
            Iterator it = processingAppMsgAdd.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                sleep += (Integer) pair.getValue();
            }

            //log.info("{} simulating inner latency for ring add id: {} value {} sleep: {}, msg: {} counter {}", new Object[]{self, msg.id, value, sleep, processingAppMsgRingAdd.size(), processedMsgRingAddCounter});

            ScheduleTimeout spt = new ScheduleTimeout(sleep);
            InnerLatencyTimerRingAdd sc = new InnerLatencyTimerRingAdd(spt, processedMsgRingAddCounter, msg);
            spt.setTimeoutEvent(sc);
            trigger(spt, timer);
            processedMsgRingAddCounter++;

        }
    };

    private final Handler<InnerLatencyTimerRingAdd> handleRingAddTimer = new Handler<InnerLatencyTimerRingAdd>() {

        @Override
        public void handle(InnerLatencyTimerRingAdd timer) {
            //log.info("{} => RingAdd item {}", new Object[]{self, msg.item});
            processingAppMsgRingAdd.remove(timer.msgId);

            RingAdd msg = timer.ringadd;

            if (between(msg.item.key, self.pred.id, self.id, false, true)) {
                if(msg.type == TYPE.ADD){
                    storage.add(msg.item);
                    trigger(new AddResponse(self, msg.returnAddress, msg.item.key, msg.id, 1, msg.msgCounter, msg.startInnerLatency, System.currentTimeMillis()), network);


                    //log.info("{} => Add item {}", new Object[]{self, msg.item});
                    /*if(!replications.isEmpty()){
                        for(Integer i : replications){
                            int ring  = 0;
                            if(i == 0){
                                ring = rand.nextInt(nRings);
                            } else {
                                ring = i + self.ring;
                                if(ring > nRings)
                                    ring = 0;
                            }


                            if(ringNodes[ring] != null)
                                trigger(new Add(self, ringNodes[ring], TYPE.ADDREPLICA, msg.item, ringNodes, msg.id, msg.returnAddress, msg.startInnerLatency), network);
                        }
                    }*/
                }
                else if(msg.type == TYPE.ADDREPLICA){
                    replicaStorage.add(msg.item);
                    //log.info("{} => Add item {} in replica", new Object[]{self, msg.item});
                    trigger(new AddResponse(self, msg.returnAddress, msg.item.key, msg.id, 2, msg.msgCounter, msg.startInnerLatency, System.currentTimeMillis()), network);
                }
            } else {
                msg.msgCounter++;
                NodeInfo s = find_successor(msg.item.key, msg.returnAddress, msg.type, 0, msg.item, msg.id, msg.msgCounter, msg.startInnerLatency);
                if (s != null) {
                    trigger(new RingAdd(self, s, msg.type, msg.item, msg.id, msg.msgCounter, msg.returnAddress, msg.startInnerLatency), network);
                }
            }


        }
    };

    private final Handler<LookUp> handleLookUp = new Handler<LookUp>() {

        @Override
        public void handle(LookUp msg) {


            if(msg.startInnerLatency == 0L)
                msg.startInnerLatency = System.currentTimeMillis();

            //SIMULATING INTERNAL LATENCY
            int value = rand.nextInt(maxProcessMsgTime + minProcessMsgTime) + minProcessMsgTime;
            processingAppMsgLookUp.put(processedMsgLookUpCounter, value);
            int sleep = 0;
            Iterator it = processingAppMsgLookUp.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                sleep += (Integer) pair.getValue();
            }

            //log.info("{} simulating inner latency for ring add id: {} value {} sleep: {}, msg: {} counter {}", new Object[]{self, msg.id, value, sleep, processingAppMsgLookUp.size(), processedMsgLookUpCounter});

            ScheduleTimeout spt = new ScheduleTimeout(sleep);
            InnerLatencyTimerLookUp sc = new InnerLatencyTimerLookUp(spt, processedMsgLookUpCounter, msg);
            spt.setTimeoutEvent(sc);
            trigger(spt, timer);
            processedMsgLookUpCounter++;

        }
    };

    private final Handler<InnerLatencyTimerLookUp> handleLookUpTimer = new Handler<InnerLatencyTimerLookUp>() {

        @Override
        public void handle(InnerLatencyTimerLookUp timer) {

            processingAppMsgLookUp.remove(timer.msgId);

            LookUp msg = timer.lookup;

            //log.info("{}: Received lookup msg for key {} with counter : {} - Time: {}, returnaddress: {}", new Object[]{self,msg.key, msg.counter, System.nanoTime(), msg.returnAddress});


            if (between(msg.key, self.pred.id, self.id, false, true)) {
                //log.info("{}: found right node {} with counter : {}", new Object[]{self,msg.key, msg.counter});
                Long endInnerLatency = System.currentTimeMillis();

                if (storage.contains(new Item(msg.key))) {

                    trigger(new LookUpResponse(self, msg.returnAddress, msg.key, LookUpResponse.LookUp.InStore, self, msg.id, msg.counter, msg.startInnerLatency, endInnerLatency), network);

                } else if (replicaStorage.contains(new Item(msg.key))){
                    trigger(new LookUpResponse(self, msg.returnAddress, msg.key, LookUpResponse.LookUp.InReplica, self, msg.id, msg.counter, msg.startInnerLatency, endInnerLatency), network);

                } else {

                    trigger(new LookUpResponse(self, msg.returnAddress, msg.key, LookUpResponse.LookUp.NotFound, self, msg.id, msg.counter, msg.startInnerLatency, endInnerLatency), network);
                }

            } else {
                msg.counter++;
                NodeInfo s = find_successor(msg.key, msg.returnAddress, TYPE.LOOKUP, 0, null, msg.id, msg.counter, msg.startInnerLatency);
                if (s != null) {
                    //log.info("{}: sending lookup msg to ring for key {} with counter : {}, time: {}", new Object[]{self,msg.key, msg.counter, System.nanoTime()});
                    trigger(new RingLookUp(self, s, msg.returnAddress, msg.key, s, msg.id, msg.counter, msg.startInnerLatency), network);
                }
            }

        }
    };

    private final Handler<RingLookUp> handleRingLookUp = new Handler<RingLookUp>() {

        @Override
        public void handle(RingLookUp msg) {


            //SIMULATING INTERNAL LATENCY
            int value = rand.nextInt(maxProcessMsgTime + minProcessMsgTime) + minProcessMsgTime;
            processingAppMsgRingLookUp.put(processedMsgRingLookUpCounter, value);
            int sleep = 0;
            Iterator it = processingAppMsgRingLookUp.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                sleep += (Integer) pair.getValue();
            }

            //log.info("{} simulating inner latency for ring add id: {} value {} sleep: {}, msg: {} counter {}", new Object[]{self, msg.id, value, sleep, processingAppMsgRingLookUp.size(), processedMsgRingLookUpCounter});

            ScheduleTimeout spt = new ScheduleTimeout(sleep);
            InnerLatencyTimerRingLookUp sc = new InnerLatencyTimerRingLookUp(spt, processedMsgRingLookUpCounter, msg);
            spt.setTimeoutEvent(sc);
            trigger(spt, timer);
            processedMsgRingLookUpCounter++;

        }
    };

    private final Handler<InnerLatencyTimerRingLookUp> handleRingLookUpTimer = new Handler<InnerLatencyTimerRingLookUp>() {

        @Override
        public void handle(InnerLatencyTimerRingLookUp timer) {

            processingAppMsgRingLookUp.remove(timer.msgId);

            RingLookUp msg = timer.lookup;

            //log.info("{}: Received ringlookup msg for key {} with counter : {}, time: {}, returnaddress; {}", new Object[]{self,msg.key, msg.counter, System.nanoTime(), msg.returnAddress});
            if (between(msg.key, self.pred.id, self.id, false, true)) {
                //log.info("{}: found right node for key {} with counter : {}", new Object[]{self,msg.key, msg.counter});
                Long endInnerLatency = System.currentTimeMillis();
                if (storage.contains(new Item(msg.key))) {

                    trigger(new LookUpResponse(self, msg.returnAddress, msg.key, LookUpResponse.LookUp.InStore, self, msg.id, msg.counter, msg.startInnerLatency, endInnerLatency), network);

                } else if (replicaStorage.contains(new Item(msg.key))){
                    trigger(new LookUpResponse(self, msg.returnAddress, msg.key, LookUpResponse.LookUp.InReplica, self, msg.id, msg.counter, msg.startInnerLatency, endInnerLatency), network);

                } else {

                    trigger(new LookUpResponse(self, msg.returnAddress, msg.key, LookUpResponse.LookUp.NotFound, self, msg.id, msg.counter, msg.startInnerLatency, endInnerLatency), network);
                }
            } else {
                msg.counter++;
                NodeInfo s = find_successor(msg.key, msg.returnAddress, TYPE.LOOKUP, 0, null, msg.id, msg.counter, msg.startInnerLatency);
                if (s != null) {
                    //log.info("{}: Sending ringlookup msg for key {} with counter : {}, Time: {}", new Object[]{self,msg.key, msg.counter, System.nanoTime()});
                    trigger(new RingLookUp(self, s, msg.returnAddress, msg.key, s, msg.id, msg.counter, msg.startInnerLatency), network);
                }
            }

        }
    };

    private final Handler<Ping> handlePing = new Handler<Ping>() {

        @Override
        public void handle(Ping msg) {
            if(self.succ != self)
                trigger(new RingPing(self, self.succ, msg.returnAddress, msg.startAddress, msg.id, System.currentTimeMillis()), network);
        }
    };

    private final Handler<RingPing> handleRingPing = new Handler<RingPing>() {

        @Override
        public void handle(RingPing msg) {

            if(msg.startAddress != self){
                trigger(new RingPing(self, self.succ, msg.returnAddress, msg.startAddress, msg.id, msg.startInnerLatencyTime), network);
            }
            else {
                trigger(new Pong(self, msg.returnAddress, self.ring, msg.id, msg.startInnerLatencyTime, System.currentTimeMillis()), network);
            }

        }
    };






    public void update_others() {

        for (int i = 1; i <= M; i++) {
            int x = (int) Math.pow(2, i - 1);
            if (self.id - x >= 0) {
                x = self.id - x;
            } else {
                x = (M - 1) + x;
            }

            NodeInfo p = find_predecessor(x, self, TYPE.OTHERS, i, null,0, 0, 0L);
            if (p != null) {
                trigger(new UpdateFingerTable(self, p, self, i), network);
            }

        }

    }

    public NodeInfo find_successor(int id, NodeInfo returnAddress, TYPE type, int index, Item item, int lookupID, int msgCounter, Long startInnerLatency) {
        //log.info("{} find successor for id: {}", new Object[]{self, id});
        NodeInfo nPrime = find_predecessor(id, returnAddress, type, index, item, lookupID, msgCounter, startInnerLatency);
        if (nPrime != null) {
            return nPrime.succ;
        }

        return null;
    }

    public NodeInfo find_predecessor(int id, NodeInfo returnAddress, TYPE type, int i, Item item, int lookupID, int msgCounter, Long startInnerLatency) {

        NodeInfo nPrime = self;

        while (!between(id, nPrime.id, nPrime.succ.id, false, true)) {
            nPrime = closest_finger(id);
            //if(lookupID > 0)
            //log.info("{} sending cloest finger to node {} with key {}", new Object[]{self, nPrime, id});
            trigger(new ClosestFinger(self, nPrime, returnAddress, type, id, null, i, item, lookupID, msgCounter, startInnerLatency), network);
            return null;
        }
        return nPrime;
    }

    public NodeInfo closest_finger(int id) {
        //log.info("{} |cloest finger for id : {}", new Object[]{self, id});
        for (int i = M; i >= 1; i--) {
            if (between(fingers.get(i).node.id, self.id, id, false, false)) {
                if(fingers.get(i).node.id == -1)
                    log.info("Finger = null");
                return fingers.get(i).node;
            }
        }

        return self.succ;
    }


    private Long lastTime;
    private final Handler<StabilizerTimer> handleStabilizerTimer = new Handler<StabilizerTimer>() {
        @Override
        public void handle(StabilizerTimer event) {
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

        public NodeInfo selfAddress;
        public NodeInfo firstNode;
        public int m;
        public ArrayList<Integer> replications;
        public int nRings;

        public ChordInit(NodeInfo self, NodeInfo firstNode, int m, ArrayList<Integer> replications, int nRings) {
            this.selfAddress = self;
            this.firstNode = firstNode;
            this.m = m;
            this.replications = replications;
            this.nRings = nRings;
        }



    }

}

