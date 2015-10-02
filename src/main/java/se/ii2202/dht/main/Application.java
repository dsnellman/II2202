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


    private HashMap<Integer, ArrayList<Integer>> keyPlacedInRing = new HashMap<>();

    private HashMap<Integer, SendingInfo> addSendingTimes = new HashMap<>();
    private ArrayList<TestResult> addResult = new ArrayList<>();


    private HashMap<Integer, Long> lookUpSendingTimes = new HashMap<>();
    private ArrayList<TestResult> lookupResult = new ArrayList<>();

    private HashMap<Integer, Long> pingSendingTimes = new HashMap<>();

    private ArrayList<Command> commands = new ArrayList<>();

    private ArrayList<ArrayList<RingController>> ringController = new ArrayList<>();

    private Stats stats = new Stats();

    private Random rand = new Random();
    private int NUMBER_OF_ADDS = 1;
    private int NUMBER_OF_LOOKUPS = 1;
    private int PERIODIC_PING_TIMEOUT = 2000;
    private int replications;

    //Which strategy to choose rings..
    private String testStrategy = "nFirst";
    private boolean randomChoose = false;
    private boolean bestChoose = true;
    private boolean worstChoose = false;


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

            commands.add(new Command(Command.TYPE.SLEEP, 100000));

            for(int i = 0; i < NUMBER_OF_ADDS; i++) {
                int index = ((NUMBER_OF_ADDS * self.id) + i) % 10000;
                commands.add(new Command(Command.TYPE.ADD, itemsKey.get(index), 500));
                commands.add(new Command(Command.TYPE.SLEEP, 100));
            }


            commands.add(new Command(Command.TYPE.SLEEP, 50000));

            for(int i = 0; i < NUMBER_OF_LOOKUPS; i++) {
                int index = ((NUMBER_OF_LOOKUPS * self.id) + i) % 10000;
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

    public ArrayList<RingInfo> chooseRing(int n){
        ArrayList<RingInfo> ringAvg = new ArrayList<>();

        if(testStrategy == "nFirst") {

            for (int i = 0; i < nRings; i++) {
                int totalRing = 0;
                int counter = 0;
                for (RingController controller : ringController.get(i)) {
                    totalRing += controller.totalTime;
                    counter++;
                    log.info("{} Ring: {}, total time: {}", new Object[]{self, i, controller.totalTime});
                    if (counter == n)
                        break;
                }
                ringAvg.add(new RingInfo(i,totalRing / counter));
                log.info("{} Ring total {} for all last {} runs to ring {}, avg: {}", new Object[]{self, totalRing, counter, i, totalRing / counter});
            }

        }




        Collections.sort(ringAvg, new CompareRingInfo());
        log.info("return ring: {}",ringAvg.toString());
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

                //int ring = bestRing();
                //int ring = rand.nextInt(nRings);
                ArrayList<RingInfo> rings = chooseRing(1);

                Item item = new Item(command.key, command.value);
                SendingInfo si = new SendingInfo();
                addSendingTimes.put(command.key, si);

                if(bestChoose) {
                    for (int i = 0; i < replications; i++) {
                        log.info("{} sening add for key {} to {}", new Object[]{self, command.key, rings.get(i).ring});
                        si.list.put(storeCounter, System.currentTimeMillis());
                        trigger(new Add(self, ringNodes[rings.get(i).ring], TYPE.ADD, item, storeCounter, self), network);
                        storeCounter++;
                    }
                } else if(worstChoose){
                    for (int i = 0; i < replications; i++) {
                        log.info("{} sening add for key {} to {}", new Object[]{self, command.key, rings.get(rings.size() - i).ring});
                        si.list.put(storeCounter, System.currentTimeMillis());
                        trigger(new Add(self, ringNodes[rings.get(rings.size() - i).ring], TYPE.ADD, item, storeCounter, self), network);
                        storeCounter++;
                    }
                } else if(randomChoose){
                    ArrayList<Integer> temp = new ArrayList<>();
                    for (int i = 0; i < replications; i++) {

                        si.list.put(storeCounter, System.currentTimeMillis());
                        int index = rand.nextInt(nRings);
                        while(temp.contains(index)){
                            index = rand.nextInt(nRings);
                        }
                        temp.add(index);
                        log.info("{} sening add for key {} to {}", new Object[]{self, command.key, index});
                        trigger(new Add(self, ringNodes[index], TYPE.ADD, item, storeCounter, self), network);
                        storeCounter++;
                    }

                }





            }
            else if(command.type == Command.TYPE.LOOKUP){

                ArrayList<Integer> rings = keyPlacedInRing.get(command.key);

                for(int i = 0; i < rings.size(); i++){
                    log.info("{} sening lookup for key {} to {}", new Object[]{self, command.key, rings.get(i)});
                    trigger(new LookUp(self, ringNodes[rings.get(i)], command.key, self, lookUpCounter, LookUp.LookUpTYPE.LOOKUP), network);
                    lookUpSendingTimes.put(lookUpCounter, System.currentTimeMillis());
                    lookUpCounter++;
                }


            }

            runNextCommand();
        }
    }

    private Handler<LookUpResponse> handleLookUpResponse = new Handler<LookUpResponse>() {

        public void handle(LookUpResponse msg){
            Long receivedTime = System.currentTimeMillis();

            if(msg.type == LookUp.LookUpTYPE.PING){

                Long external1 = msg.startInnerLatency - pingSendingTimes.get(msg.id);
                Long internal = msg.endInnerLatency - msg.startInnerLatency;
                Long external2 = receivedTime - msg.endInnerLatency;

                Long total = external1 + internal + external2;
                ringController.get(msg.fromRing).add(0, new RingController(external1, external2, internal, total));


            } else {
                Long external1 = msg.startInnerLatency - lookUpSendingTimes.get(msg.id);
                Long internal = msg.endInnerLatency - msg.startInnerLatency;
                Long external2 = receivedTime - msg.endInnerLatency;

                Long total = external1 + internal + external2;

                stats.LookUpResponses++;
                if(msg.answer == LookUpResponse.LookUp.InStore || msg.answer == LookUpResponse.LookUp.InReplica) {

                    lookupResult.add(new TestResult(TestResult.TestType.LOOKUP, self.id, msg.fromRing, msg.counter, msg.key, external1, external2, internal, total));
                    ringController.get(msg.fromRing).add(0, new RingController(external1, external2, internal, total));
                }

            }
        }

    };

    private Handler<AddResponse> handleAddResponse = new Handler<AddResponse>() {

        public void handle(AddResponse msg){
            stats.AddResponses++;

            if(addSendingTimes.containsKey(msg.key)) {
                SendingInfo si = addSendingTimes.get(msg.key);
                Long time = System.currentTimeMillis();
                Long external1 = msg.startInnerLatency - si.list.get(msg.id);
                Long internal = msg.endInnerLatency - msg.startInnerLatency;
                Long external2 = time - msg.endInnerLatency;
                Long total = external1 + internal + external2;

                ringController.get(msg.fromRing).add(0, new RingController(external1, external2, internal, total));



                if (si.list.size() == 1 && si.list.containsKey(msg.id)) {
                    addResult.add(new TestResult(TestResult.TestType.STORE, self.id, msg.fromRing, msg.msgCounter, msg.key, external1, external2, internal, total));
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
            //log.info("Add response key {}, keyplayced {}", new Object[]{msg.key, keyPlacedInRing.size()});

        }

    };


    private Handler<PeriodicPingTimer> handlePeriodicPingTimer = new Handler<PeriodicPingTimer>() {

        public void handle(PeriodicPingTimer event) {

            for(int i = 0; i < ringNodes.length; i++){
                if(ringNodes[i] != null){
                    pingSendingTimes.put(pingCounter, System.currentTimeMillis());

                    int index = ((NUMBER_OF_LOOKUPS * self.id) + 0) % 10000;
                    trigger(new LookUp(self, ringNodes[i], itemsKey.get(index), self, pingCounter, LookUp.LookUpTYPE.PING), network);
                    //trigger(new Ping(self, ringNodes[i],pingCounter), network);
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
        public int replications;
        public NodeInfo[] ringNodes;
        public ArrayList<LatencyContainer> latency;

        public ApplicationInit(NodeInfo selfAddress, int M, int nRings, int replications, NodeInfo[] ringNodes, ArrayList<LatencyContainer> latency) {
            this.selfAddress = selfAddress;
            this.M = M;
            this.nRings = nRings;
            this.replications = replications;
            this.ringNodes = ringNodes;
            this.latency = latency;
        }

    }
}
