package se.ii2202.dht.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ii2202.dht.appmsg.*;
import se.ii2202.dht.object.*;
import se.ii2202.dht.timer.CommandTimer;
import se.ii2202.dht.object.Stats;
import se.ii2202.dht.timer.PeriodicClockTimer;
import se.ii2202.dht.timer.PeriodicPingTimer;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.simulator.timed.api.TimedControler;
import se.sics.p2ptoolbox.simulator.timed.api.TimedControlerBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Application extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private Positive<Timer> timer = requires(Timer.class);
    private Positive<Network> network = requires(Network.class);

    private NodeInfo self;
    private ArrayList<ArrayList<NodeInfo>> ringNodes;

    private int lookUpCounter = 1;
    private int storeCounter = 1;
    private int pingCounter = 1;

    //private int M;
    //private int nRings;
    private List<Integer> itemsKey = new ArrayList<>();


    private HashMap<Integer, ArrayList<Integer>> keyPlacedInRing = new HashMap<>();

    private HashMap<Integer, SendingInfo> addSendingTimes = new HashMap<>();
    private ArrayList<TestResult> addResult = new ArrayList<>();


    private HashMap<Integer, Long> lookUpSendingTimes = new HashMap<>();
    private ArrayList<TestResult> lookupResult = new ArrayList<>();

    private HashMap<Integer, Long> pingSendingTimes = new HashMap<>();

    private ArrayList<Command> commands = new ArrayList<>();

    private ArrayList<ArrayList<RingController>> ringController = new ArrayList<>();
    //private int replications;
    private Stats stats = new Stats();

    private RunProperties PROPERTIES;

    private TimedControler tc;

    private Random rand = new Random();

    public Application(ApplicationInit init) {
        self = init.selfAddress;
        PROPERTIES = init.properties;
        ringNodes = init.ringNodes;

        tc = init.tcb.registerComponent(self.id, this);

        for(int i = 0; i < PROPERTIES.nRings; i++){
            ringController.add(new ArrayList<>());
        }


        subscribe(handleStart, control);
        subscribe(handleTimer, timer);
        subscribe(handleLookUpResponse, network);
        subscribe(handleAddResponse, network);
        subscribe(handleResultRequest, network);

        subscribe(handlePeriodicPingTimer, timer);
        subscribe(handlePeriodicClockTimer, timer);

    }


    private Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            tc.advance(Application.this, 0);
            //log.info("Starts app with id: {} ", new Object[]{self.id});

            schedulePing();

            if(self.id == 0){
                schedulerClockTimer();
            }


            InputStream stream = Application.class.getResourceAsStream("/ItemKeys.txt");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream)))
            {
                String line = br.readLine();
                String[] input = line.split(", ");
                for(String s : input){
                    itemsKey.add(Integer.parseInt(s));
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

            commands.add(new Command(Command.TYPE.SLEEP, 10000));

            for(int i = 0; i < PROPERTIES.NUMBER_OF_ADDS; i++) {
                int index = ((PROPERTIES.NUMBER_OF_ADDS * self.id) + i) % 10000;
                commands.add(new Command(Command.TYPE.ADD, itemsKey.get(index), 500));
                commands.add(new Command(Command.TYPE.SLEEP, PROPERTIES.DELAY_BETWEEN_OP));
            }


            commands.add(new Command(Command.TYPE.SLEEP, PROPERTIES.DELAY_BETWEEN_ADD_LOOKUP));

            for(int i = 0; i < PROPERTIES.NUMBER_OF_LOOKUPS; i++) {
                int index = ((PROPERTIES.NUMBER_OF_LOOKUPS * self.id) + i) % 10000;
                commands.add(new Command(Command.TYPE.LOOKUP, itemsKey.get(index), 500));
                commands.add(new Command(Command.TYPE.SLEEP, PROPERTIES.DELAY_BETWEEN_OP));
            }


            runNextCommand();

        }
    };


    private Handler<CommandTimer> handleTimer = new Handler<CommandTimer>() {

        public void handle(CommandTimer event) {
            tc.advance(Application.this, 0);
            if (event.type == 1) {



            } else if (event.type == 2){

                runNextCommand();

            }

        }
    };

    public ArrayList<RingInfo> chooseRing(){
        ArrayList<RingInfo> ringAvg = new ArrayList<>();

        if(PROPERTIES.testStrategy_nFirst) {

            for (int i = 0; i < PROPERTIES.nRings; i++) {
                int totalRing = 0;
                ArrayList<RingController> ring = ringController.get(i);
                for(int j = 0; j < PROPERTIES.n; j++){
                    totalRing += ring.get(j).totalTime;
                }

                ringAvg.add(new RingInfo(i, totalRing / PROPERTIES.n));
                //log.info("{} Ring total {} for all last {} runs to ring {}, avg: {}", new Object[]{self, totalRing, n, i, totalRing / n});
            }

        }

        Collections.sort(ringAvg, new CompareRingInfo());
        //log.info("return ring: {}",ringAvg.toString());
        return ringAvg;
    }

    public void runNextCommand(){

        if(!commands.isEmpty()){

            Command command = commands.remove(0);


            if(command.type == Command.TYPE.SLEEP){
                ScheduleTimeout spt = new ScheduleTimeout(command.value);
                CommandTimer sc = new CommandTimer(spt, 2);
                spt.setTimeoutEvent(sc);
                trigger(spt, timer);

                return;
            }
            else if(command.type == Command.TYPE.ADD){

                if(storeCounter == 0 && self.id == 0)
                    log.info("Start sending add");

                //int ring = bestRing();
                //int ring = rand.nextInt(nRings);
                ArrayList<RingInfo> rings = chooseRing();

                Item item = new Item(command.key, command.value);
                SendingInfo si = new SendingInfo();
                addSendingTimes.put(command.key, si);

                if(PROPERTIES.bestChoose) {
                    for (int i = 0; i < PROPERTIES.replications; i++) {
                        //og.info("{} sening add for key {} to {}", new Object[]{self, command.key, rings.get(i).ring});
                        si.list.put(storeCounter, System.currentTimeMillis());
                        int index = storeCounter % ringNodes.get(rings.get(i).ring).size();
                        log.info("{} sening add for key {} to {} rings {}", new Object[]{self, command.key, rings.get(i).ring, rings.toString()});
                        trigger(new Add(self, ringNodes.get(rings.get(i).ring).get(index), TYPE.ADD, item, storeCounter, self), network);
                        storeCounter++;
                    }
                } else if(PROPERTIES.worstChoose){
                    for (int i = 0; i < PROPERTIES.replications; i++) {
                        //log.info("{} sending add for key {} to {}, rings: {}", new Object[]{self, command.key, rings.get((PROPERTIES.nRings - 1) - i).ring, rings.toString()});
                        si.list.put(storeCounter, System.currentTimeMillis());
                        int index = storeCounter % ringNodes.get(rings.get((PROPERTIES.nRings - 1) - i).ring).size();
                        //log.info("{} sening add for key {} to ring {} and node {}, index: {}, size {}", new Object[]{self, command.key, rings.get(i).ring, ringNodes.get(rings.get(i).ring).get(index), index, ringNodes.get(rings.get(i).ring).size()});
                        trigger(new Add(self, ringNodes.get(rings.get((PROPERTIES.nRings - 1) - i).ring).get(index), TYPE.ADD, item, storeCounter, self), network);
                        storeCounter++;
                    }
                } else if(PROPERTIES.randomChoose){
                    ArrayList<Integer> temp = new ArrayList<>();
                    for (int i = 0; i < PROPERTIES.replications; i++) {
                        si.list.put(storeCounter, System.currentTimeMillis());
                        int ring = rand.nextInt(PROPERTIES.nRings);
                        while(temp.contains(ring)){
                            ring = rand.nextInt(PROPERTIES.nRings);
                        }
                        temp.add(ring);
                        int index = storeCounter % ringNodes.get(ring).size();
                        log.info("{} sening add for key {} to ring {} and node {}, index: {}", new Object[]{self, command.key, ring, ringNodes.get(ring).get(index), index});
                        trigger(new Add(self, ringNodes.get(ring).get(index), TYPE.ADD, item, storeCounter, self), network);
                        storeCounter++;
                    }

                }





            }
            else if(command.type == Command.TYPE.LOOKUP){

                if(lookUpCounter == 0 && self.id == 0)
                    log.info("Start sending lookups, addresponses: {}", stats.AddResponses);

                ArrayList<Integer> rings = keyPlacedInRing.get(command.key);

                if(PROPERTIES.lookUpToAll){

                    for(int i = 0; i < rings.size(); i++){
                        //log.info("{} sening lookup for key {} to {}", new Object[]{self, command.key, rings.get(i)});
                        int index = lookUpCounter % ringNodes.get(rings.get(i)).size();
                        //log.info("{} sening lookup for key {} to ring {} and node {}, index: {}, size {}", new Object[]{self, command.key, i, ringNodes.get(rings.get(i)).get(index), index, ringNodes.get(rings.get(i)).size()});
                        trigger(new LookUp(self, ringNodes.get(rings.get(i)).get(index), command.key, self, lookUpCounter, LookUp.LookUpTYPE.LOOKUP), network);
                        lookUpSendingTimes.put(lookUpCounter, System.currentTimeMillis());
                        lookUpCounter++;
                    }
                }
            }

            runNextCommand();
        }
    }

    private Handler<LookUpResponse> handleLookUpResponse = new Handler<LookUpResponse>() {

        public void handle(LookUpResponse msg){
            tc.advance(Application.this, 0);
            Long receivedTime = System.currentTimeMillis();

            if(msg.type == LookUp.LookUpTYPE.PING){

                Long external1 = msg.startInnerLatency - pingSendingTimes.get(msg.id);
                Long internal = msg.endInnerLatency - msg.startInnerLatency;
                Long external2 = receivedTime - msg.endInnerLatency;

                Long total = external1 + internal + external2;
                ringController.get(msg.fromRing).add(0, new RingController(external1, external2, internal, total));
                //log.info("Pong from: {} ex: {} {} inttime: {} tot {}", new Object[]{msg.fromRing, external1, external2, internal, total});

            } else {
                Long external1 = msg.startInnerLatency - lookUpSendingTimes.get(msg.id);
                Long internal = msg.endInnerLatency - msg.startInnerLatency;
                Long external2 = receivedTime - msg.endInnerLatency;

                Long total = external1 + internal + external2;

                stats.LookUpResponses++;
                if(msg.answer == LookUpResponse.LookUp.InStore || msg.answer == LookUpResponse.LookUp.InReplica) {

                    lookupResult.add(new TestResult(TestResult.TestType.LOOKUP, self.id, msg.fromRing, msg.counter, msg.key, external1, external2, internal, total, receivedTime));
                    ringController.get(msg.fromRing).add(0, new RingController(external1, external2, internal, total));
                }

            }
        }

    };

    private Handler<AddResponse> handleAddResponse = new Handler<AddResponse>() {

        public void handle(AddResponse msg){
            tc.advance(Application.this, 0);
            stats.AddResponses++;
            if(addSendingTimes.containsKey(msg.key)) {
                SendingInfo si = addSendingTimes.get(msg.key);
                Long time = System.currentTimeMillis();
                Long external1 = msg.startInnerLatency - si.list.get(msg.id);
                Long internal = msg.endInnerLatency - msg.startInnerLatency;
                Long external2 = time - msg.endInnerLatency;
                Long total = external1 + internal + external2;

                log.info("{} got add response: {} ex: {} {}, internal {} , total: {}", new Object[]{self, msg.key, external1, external2, internal, total});

                ringController.get(msg.fromRing).add(0, new RingController(external1, external2, internal, total));



                if (si.list.size() == 1 && si.list.containsKey(msg.id)) {
                    addResult.add(new TestResult(TestResult.TestType.STORE, self.id, msg.fromRing, msg.msgCounter, msg.key, external1, external2, internal, total, time));
                    addSendingTimes.remove(msg.key);

                } else {
                    si.list.remove(msg.id);
                }
            }

            if(keyPlacedInRing.containsKey(msg.key)){

                    keyPlacedInRing.get(msg.key).add(msg.fromRing);

            } else {
                ArrayList<Integer> list = new ArrayList<>();
                list.add(msg.fromRing);
                keyPlacedInRing.put(msg.key, list);
            }

            if(stats.AddResponses == PROPERTIES.NUMBER_OF_ADDS * PROPERTIES.replications && self.id == 0)
                log.info("Got responses from all sent addmessages");
        }

    };


    private Handler<PeriodicPingTimer> handlePeriodicPingTimer = new Handler<PeriodicPingTimer>() {

        public void handle(PeriodicPingTimer event) {
            tc.advance(Application.this, 0);
            for(int i = 0; i < ringNodes.size(); i++){
                pingSendingTimes.put(pingCounter, System.currentTimeMillis());

                int keyindex = ((PROPERTIES.NUMBER_OF_LOOKUPS * self.id) + 0) % 10000;
                int nodeindex = storeCounter % ringNodes.get(i).size();
                trigger(new LookUp(self, ringNodes.get(i).get(nodeindex), itemsKey.get(keyindex), self, pingCounter, LookUp.LookUpTYPE.PING), network);
                //trigger(new Ping(self, ringNodes[i],pingCounter), network);
                pingCounter++;
            }

        }
    };
    private Handler<PeriodicClockTimer> handlePeriodicClockTimer = new Handler<PeriodicClockTimer>() {

        public void handle(PeriodicClockTimer event) {
            tc.advance(Application.this, 0);
            log.info("Clock update: {}", System.currentTimeMillis());

        }
    };

    private void schedulerClockTimer(){
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(10000, 10000);
        PeriodicClockTimer sc = new PeriodicClockTimer(spt);
        spt.setTimeoutEvent(sc);
        trigger(spt, timer);
    }

    private void schedulePing() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(PROPERTIES.PERIODIC_PING_TIMEOUT, PROPERTIES.PERIODIC_PING_TIMEOUT);
        PeriodicPingTimer sc = new PeriodicPingTimer(spt);
        spt.setTimeoutEvent(sc);
        trigger(spt, timer);
    }


    private Handler<ResultRequest> handleResultRequest = new Handler<ResultRequest>() {

        public void handle(ResultRequest event) {
            tc.advance(Application.this, 0);
            log.info("Node: {}", new Object[]{self});

            log.info("Stores: {}, LookUps: {}", new Object[]{addResult.size(), lookupResult.size()});
            log.info("Sent add: {}, Sent LookUps: {}", new Object[]{storeCounter, lookUpCounter});
            log.info("LookUpResponses: {}, AddResponses: {}", new Object[]{stats.LookUpResponses, stats.AddResponses});
            //log.info("LookUp in store: {}, look up in replica {}", new Object[]{lookupInStore, lookupInReplica});
            //log.info("LookUp in store from ring: {}, LookUp in replica from ring {}", new Object[]{Arrays.toString(lookUpInRing), Arrays.toString(lookUpInRingReplicas)});
            //log.info("Stored to ring: {}, Added in store: {}, Added in replica {}\n", new Object[]{Arrays.toString(storedToRing), addCounter1, addCounter2});


            trigger(new ResultResponse(self, event.returnAddress, addResult, lookupResult), network);

        }
    };



    public static class ApplicationInit extends Init<Application> {

        public TimedControlerBuilder tcb;
        public NodeInfo selfAddress;
        public RunProperties properties;
        public ArrayList<ArrayList<NodeInfo>> ringNodes;
        public ArrayList<LatencyContainer> latency;

        public ApplicationInit(NodeInfo selfAddress,RunProperties properties, ArrayList<ArrayList<NodeInfo>> ringNodes, ArrayList<LatencyContainer> latency, TimedControlerBuilder tcb) {
            this.selfAddress = selfAddress;
            this.properties = properties;
            this.ringNodes = ringNodes;
            this.latency = latency;
            this.tcb = tcb;
        }

    }
}
