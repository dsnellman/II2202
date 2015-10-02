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
    private NodeInfo[] ringNodes;

    private int lookUpCounter = 1;
    private int storeCounter = 1;
    private int pingCounter = 1;

    private int M;
    private int nRings;
    private List<Integer> itemsKey = new ArrayList<>();
    private ArrayList<Integer> replications;

    private HashMap<Integer, KeyPlacedInfo> keyPlacedInRing = new HashMap<>();

    private HashMap<Integer, SendingInfo> addSendingTimes = new HashMap<>();
    private ArrayList<TestResult> addResult = new ArrayList<>();


    private HashMap<Integer, Long> lookUpSendingTimes = new HashMap<>();
    private ArrayList<TestResult> lookupResult = new ArrayList<>();

    private HashMap<Integer, Long> pingSendingTimes = new HashMap<>();

    private ArrayList<Command> commands = new ArrayList<>();

    private ArrayList<ArrayList<RingController>> ringController = new ArrayList<>();

    private Stats stats = new Stats();

    private Random rand = new Random();
    private int NUMBER_OF_ADDS = 100;
    private int NUMBER_OF_LOOKUPS = 10;
    private int PERIODIC_PING_TIMEOUT = 2000;

    private int nSavedResponseTime = 10;

    public Application(ApplicationInit init) {
        self = init.selfAddress;
        M = init.M;
        nRings = init.nRings;
        replications = init.replications;
        ringNodes = init.ringNodes;

        for(int i = 0; i < nRings; i++){
            ringController.add(new ArrayList<>());
        }


        subscribe(handleStart, control);
        subscribe(handleTimer, timer);
        subscribe(handleLookUpResponse, network);
        subscribe(handleAddResponse, network);
        subscribe(handleResultRequest, network);
        subscribe(handlePong, network);

        subscribe(handlePeriodicPingTimer, timer);
        subscribe(handlePeriodicClockTimer, timer);

    }


    private Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
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

            commands.add(new Command(Command.TYPE.SLEEP, 100));

            for(int i = 0; i < NUMBER_OF_ADDS; i++) {
                int index = (NUMBER_OF_ADDS * self.id) + i;
                if(index >= itemsKey.size()){
                    index = index - itemsKey.size();
                }
                commands.add(new Command(Command.TYPE.ADD, itemsKey.get(index), 500));
                commands.add(new Command(Command.TYPE.SLEEP, 100));
            }


            commands.add(new Command(Command.TYPE.SLEEP, 50000));

            for(int i = 0; i < NUMBER_OF_LOOKUPS; i++) {
                int index = (NUMBER_OF_LOOKUPS * self.id) + i;
                if(index >= itemsKey.size()){
                    index = index - itemsKey.size();
                }
                commands.add(new Command(Command.TYPE.LOOKUP, itemsKey.get(index), 500));
                commands.add(new Command(Command.TYPE.SLEEP, 50));
            }


            runNextCommand();

        }
    };


    private Handler<CommandTimer> handleTimer = new Handler<CommandTimer>() {

        public void handle(CommandTimer event) {

            if (event.type == 1) {



            } else if (event.type == 2){

                runNextCommand();

            }

        }
    };

    public int bestRing(){
        Long bestTime = null;
        int bestId = -1;

        for(int i = 0; i < ringController.size(); i++){


        }
        return bestId;
    }

    public void runNextCommand(){

        if(!commands.isEmpty()){

            Command command = commands.remove(0);

            //log.info("{} | App runing command: {}, key {}, value {] with nodering: {}", new Object[]{self, command.type, command.key, command.value, Arrays.toString(ringNodes)});

            if(command.type == Command.TYPE.SLEEP){
                ScheduleTimeout spt = new ScheduleTimeout(command.value);
                CommandTimer sc = new CommandTimer(spt, 2);
                spt.setTimeoutEvent(sc);
                trigger(spt, timer);

                return;
            }
            else if(command.type == Command.TYPE.ADD){

                //int ring = bestRing();
                int ring = rand.nextInt(nRings);
                ArrayList<NodeInfo> replicaAddress = new ArrayList<>();
                int index = 0;
                for(int i : replications){

                    if(i > 0){
                        int replica = ring + i;
                        if(replica >= nRings){
                            replica = nRings - replica;
                        }

                        if(replica != ring)
                            replicaAddress.add(ringNodes[replica]);

                    }

                }



                Item item = new Item(command.key, command.value);

                SendingInfo si = new SendingInfo();
                si.list.put(storeCounter, System.currentTimeMillis());

                addSendingTimes.put(command.key, si);
                trigger(new Add(self, ringNodes[ring], TYPE.ADD, item, storeCounter, self), network);
                storeCounter++;

                for(int i = 0; i < replicaAddress.size(); i++){
                    si.list.put(storeCounter, System.currentTimeMillis());
                    trigger(new Add(self, replicaAddress.get(i), TYPE.ADDREPLICA, item, storeCounter, self), network);
                    storeCounter++;
                }

            }
            else if(command.type == Command.TYPE.LOOKUP){

                KeyPlacedInfo keyInfo = keyPlacedInRing.get(command.key);
                int ring = 0;
                //log.info("Key size {}, lookup key: {} in ring {}" ,new Object[]{keyPlacedInRing.size(), command.key,  keyInfo});
                if(keyInfo.ring != -1){
                    ring = keyInfo.ring;
                } else {
                    if(!keyInfo.replicas.isEmpty()){
                        ring = keyInfo.replicas.get(0);
                    }
                }

                trigger(new LookUp(self, ringNodes[ring], command.key, self, lookUpCounter), network);
                lookUpSendingTimes.put(lookUpCounter, System.currentTimeMillis());
                lookUpCounter++;

            }

            runNextCommand();
        }
    }

    private Handler<LookUpResponse> handleLookUpResponse = new Handler<LookUpResponse>() {

        public void handle(LookUpResponse msg){
            Long receviedTime = System.currentTimeMillis();
            stats.LookUpResponses++;

            if(msg.answer == LookUpResponse.LookUp.InStore || msg.answer == LookUpResponse.LookUp.InReplica) {

                Long external1 = msg.startInnerLatency - lookUpSendingTimes.get(msg.id);
                Long internal = msg.endInnerLatency - msg.startInnerLatency;
                Long external2 = receviedTime - msg.endInnerLatency;

                Long total = external1 + internal + external2;

                //log.info("{} Revecied lookupresponse from {}, found: {}, external {} + {}, internal {}, total {} = {}", new Object[]{self, msg.address, msg.answer, external1, external2, internal, total, total2});
                lookupResult.add(new TestResult(TestResult.TestType.LOOKUP, self.id, msg.fromRing, msg.counter, msg.key, external1, external2, internal, total));
                ringController.get(msg.fromRing).add(0, new RingController(external1, external2, internal, total));


                while(ringController.get(msg.fromRing).size() > nSavedResponseTime){
                    ringController.get(msg.fromRing).remove(ringController.get(msg.fromRing).size() - 1);
                }
            }

        }

    };

    private Handler<AddResponse> handleAddResponse = new Handler<AddResponse>() {

        public void handle(AddResponse msg){
            stats.AddResponses++;

            if(addSendingTimes.containsKey(msg.key)) {

                SendingInfo si = addSendingTimes.get(msg.key);
                if (si.list.size() == 1 && si.list.containsKey(msg.id)) {
                    Long time = System.currentTimeMillis();
                    Long external1 = msg.startInnerLatency - si.list.get(msg.id);
                    Long internal = msg.endInnerLatency - msg.startInnerLatency;
                    Long external2 = time - msg.endInnerLatency;
                    Long total = external1 + internal + external2;

                    addResult.add(new TestResult(TestResult.TestType.STORE, self.id, msg.fromRing, msg.msgCounter, msg.key, external1, external2, internal, total));
                    ringController.get(msg.fromRing).add(0, new RingController(external1, external2, internal, total));

                    while(ringController.get(msg.fromRing).size() > nSavedResponseTime){
                        ringController.get(msg.fromRing).remove(ringController.get(msg.fromRing).size() - 1);
                    }
                    addSendingTimes.remove(msg.key);



                } else {
                    si.list.remove(msg.id);
                }
            }

            if(keyPlacedInRing.containsKey(msg.key)){

                if(msg.type == 1){
                    keyPlacedInRing.get(msg.key).ring = msg.fromRing;
                } else {
                    keyPlacedInRing.get(msg.key).replicas.add(msg.fromRing);
                }

            } else {
                KeyPlacedInfo keyInfo = new KeyPlacedInfo();
                if(msg.type == 1){
                    keyInfo.ring = msg.fromRing;
                } else {
                    keyInfo.replicas.add(msg.fromRing);
                }
                keyPlacedInRing.put(msg.key, keyInfo);

            }
            //log.info("Add response key {}, keyplayced {}", new Object[]{msg.key, keyPlacedInRing.size()});

        }

    };

    private Handler<Pong> handlePong = new Handler<Pong>() {

        public void handle(Pong msg) {
            long receivdedTime = System.currentTimeMillis();

            long external1 = msg.startInnerLatencyTime - pingSendingTimes.get(msg.id);
            long external2 = receivdedTime - msg.endInnerLatencyTime;
            long internal = msg.endInnerLatencyTime - msg.startInnerLatencyTime;
            long total = external1 + external2 + internal;
            long testTotal = receivdedTime - pingSendingTimes.get(msg.id);

            ringController.get(msg.fromRing).add(0, new RingController(external1, external2, internal, total));


            while(ringController.get(msg.fromRing).size() > nSavedResponseTime){
                ringController.get(msg.fromRing).remove(ringController.get(msg.fromRing).size() - 1);
            }


        }
    };

    private Handler<PeriodicPingTimer> handlePeriodicPingTimer = new Handler<PeriodicPingTimer>() {

        public void handle(PeriodicPingTimer event) {

            for(int i = 0; i < ringNodes.length; i++){
                if(ringNodes[i] != null){
                    pingSendingTimes.put(pingCounter, System.currentTimeMillis());
                    trigger(new Ping(self, ringNodes[i],pingCounter), network);
                    pingCounter++;

                }
            }

        }
    };
    private Handler<PeriodicClockTimer> handlePeriodicClockTimer = new Handler<PeriodicClockTimer>() {

        public void handle(PeriodicClockTimer event) {

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
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(PERIODIC_PING_TIMEOUT, PERIODIC_PING_TIMEOUT);
        PeriodicPingTimer sc = new PeriodicPingTimer(spt);
        spt.setTimeoutEvent(sc);
        trigger(spt, timer);
    }


    private Handler<ResultRequest> handleResultRequest = new Handler<ResultRequest>() {

        public void handle(ResultRequest event) {

            log.info("Node: {}", new Object[]{self});

            log.info("Stores: {}, LookUps: {}", new Object[]{addResult.size(), lookupResult.size()});
            log.info("LookUpResponses: {}, AddResponses: {}", new Object[]{stats.LookUpResponses, stats.AddResponses});
            //log.info("LookUp in store: {}, look up in replica {}", new Object[]{lookupInStore, lookupInReplica});
            //log.info("LookUp in store from ring: {}, LookUp in replica from ring {}", new Object[]{Arrays.toString(lookUpInRing), Arrays.toString(lookUpInRingReplicas)});
            //log.info("Stored to ring: {}, Added in store: {}, Added in replica {}\n", new Object[]{Arrays.toString(storedToRing), addCounter1, addCounter2});


            trigger(new ResultResponse(self, event.returnAddress, addResult, lookupResult), network);

        }
    };



    public static class ApplicationInit extends Init<Application> {

        public NodeInfo selfAddress;
        public int M;
        public int nRings;
        public ArrayList<Integer> replications;
        public NodeInfo[] ringNodes;

        public ApplicationInit(NodeInfo selfAddress, int M, int nRings, ArrayList<Integer> replications, NodeInfo[] ringNodes) {
            this.selfAddress = selfAddress;
            this.M = M;
            this.nRings = nRings;
            this.replications = replications;
            this.ringNodes = ringNodes;
        }

    }
}
