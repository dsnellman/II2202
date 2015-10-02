package se.ii2202.dht.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.ii2202.dht.object.LatencyContainer;
import se.ii2202.dht.object.NodeInfo;
import se.sics.kompics.network.Address;
import se.sics.p2ptoolbox.simulator.cmd.impl.StartAggregatorCmd;
import se.sics.p2ptoolbox.simulator.cmd.impl.StartNodeCmd;
import se.sics.p2ptoolbox.simulator.dsl.SimulationScenario;
import se.sics.p2ptoolbox.simulator.dsl.adaptor.Operation1;
import se.sics.p2ptoolbox.simulator.dsl.distribution.ConstantDistribution;
import se.sics.p2ptoolbox.simulator.dsl.distribution.extra.GenIntSequentialDistribution;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Scenario {

    private static final Logger log = LoggerFactory.getLogger(Scenario.class);

    private static InetAddress localHost;

    private static Random rand = new Random();



    // SIMULATION VARIABLES

    private final static int M = 14; //Number of bits in identifier
    private final static int nNode = 1000; //Number of nodes in each ring
    private final static int nRings = 5; //Number of rings
    private final static int nApps = 500; //Number of applications
    private final static int runTime = 1500; //In seconds

    private static ArrayList<Integer> replications = new ArrayList<>();
    private static String[] allRingCities = new String[]{"FRAN", "CALI", "SDNY", "SPLO", "TKYO"};
    private static String[] allcities = new String[]{"VGNI", "SNGP", "ORGN", "IRLD"};
    //private static String[] allcities = new String[]{"FRAN", "CALI", "SDNY", "SPLO", "TKYO", "VGNI", "SNGP", "ORGN", "IRLD"};

    private static String filename;

    // ********************


    public static NodeInfo[] firstNodes = new NodeInfo[nRings];

    public static Integer[] nodeIDs;
    public static Integer[] appIDs = new Integer[nApps];
    public static NodeInfo[] allApps = new NodeInfo[nApps];
    public static ArrayList<String> ringcities = new ArrayList<>();
    public static ArrayList<String> appcities = new ArrayList<>();

    public static ArrayList<LatencyContainer> latencies;

    static {

        try {
            localHost = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        for(int i = 0; i < nApps; i++)
            appIDs[i] = i;

        readIdsFromFile();

        //Replica on next ring
        replications.add(1);

        //Random ring for replica
        //replications.add(0);

        //Replica on next best ring
        //replications.add(-1);




    }

    private static void startUp(){

        while(ringcities.size() < nRings){
            int index = rand.nextInt(allRingCities.length);
            if(!ringcities.contains(allRingCities[index])){
                ringcities.add(allRingCities[index]);
            }
        }

        //log.info("Cities:{}", new Object[]{ringcities.toString()});

        LatencyLists l = new LatencyLists((int) (runTime * 1.5));
        latencies = l.getLatencies();

        File files = new File("./src/main/resources/tests/");
        int highest = 0;
        for(File f : files.listFiles()){
            String number = f.getName().substring(4, f.getName().length() - 4);
            int x = Integer.parseInt(number);
            if(x > highest)
                highest = x;
        }
        highest++;
        filename = "test" + highest;
    }

    private static void readIdsFromFile(){
        List<Integer> ids = new ArrayList<>();
        InputStream stream = Scenario.class.getResourceAsStream("/NodeIds.txt");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream)))
        {
            String line = br.readLine();
            String[] input = line.split(", ");
            for (int i = 0; i < nNode; i++) {
                for(int x = 0; x < nRings; x++) {
                    ids.add(Integer.parseInt(input[i] + "" + x));
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        nodeIDs = ids.toArray(new Integer[0]);
    }



    /*
    *   Starts an aggregator node for collect data.
    */
    static Operation1<StartNodeCmd, Integer> startApp = new Operation1<StartNodeCmd, Integer>() {

        @Override
        public StartNodeCmd generate(final Integer nodeId) {
            return new StartNodeCmd<App, Address>() {
                public NodeInfo applicationAddress;
                Address node;

                public Class getNodeComponentDefinition() {
                    return App.class;
                }


                public App.AppInit getNodeComponentInit(Address aggregatorServer, Set<Address> bootstrapNodes) {

                    node = new BasicAddress(localHost, 23456, nodeId);
                    applicationAddress = new NodeInfo(node, nodeId);


                    allApps[nodeId] = applicationAddress;

                    int cityIndex = rand.nextInt(allcities.length);
                    applicationAddress.ring = cityIndex;

                    return new App.AppInit(applicationAddress, M, nRings, allcities[cityIndex], ringcities, latencies, replications, firstNodes);
                }

                public Integer getNodeId() {
                    return nodeId;
                }

                public Address getAddress() {
                    return node;
                }

                public int bootstrapSize() {
                    return 1;
                }

            };
        }
    };



    static Operation1<StartNodeCmd, Integer> startNode = new Operation1<StartNodeCmd, Integer>() {

        @Override
        public StartNodeCmd generate(final Integer nodeId) {
            return new StartNodeCmd<DHT, Address>() {
                private Address node;
                private NodeInfo nodeAddress;


                public Class getNodeComponentDefinition() {
                    return DHT.class;
                }

                public DHT.DhtInit getNodeComponentInit(Address aggregatorServer, Set<Address> bootstrapNodes) {

                    if(latencies == null){

                       startUp();

                    }

                    node = new BasicAddress(localHost, 12345, nodeId);
                    int id = nodeId / 10;
                    int ring = nodeId % 10;

                    nodeAddress = new NodeInfo(node, id, ring);

                    if (firstNodes[ring] == null) {
                        firstNodes[ring] = nodeAddress;
                    }



                    return new DHT.DhtInit(nodeAddress, firstNodes[ring], M, replications, nRings);
                }


                public Integer getNodeId() {
                    return nodeId;
                }


                public Address getAddress() {
                    return node;
                }

                public int getId() {
                    return nodeAddress.id;
                }


                public int bootstrapSize() {
                    return 1;
                }

            };
        }
    };


    static Operation1<StartAggregatorCmd, Integer> startResult = new Operation1<StartAggregatorCmd, Integer>() {

        @Override
        public StartAggregatorCmd generate(final Integer nodeId) {
            return new StartAggregatorCmd<ResultComp, Address>() {
                private NodeInfo aggregatorAddress = new NodeInfo();

                @Override
                public Class getNodeComponentDefinition() {
                    return ResultComp.class;
                }

                @Override
                public ResultComp.ResultInit getNodeComponentInit() {
                    aggregatorAddress.address = new BasicAddress(localHost, 12340, nodeId);;
                    return new ResultComp.ResultInit(aggregatorAddress, allApps, filename);
                }

                @Override
                public Address getAddress() {
                    return aggregatorAddress.address;
                }

            };
        }
    };


    public static SimulationScenario start() {


        SimulationScenario scen = new SimulationScenario() {
            {
                StochasticProcess processApp = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(10));
                        raise(appIDs.length, startApp, new GenIntSequentialDistribution(appIDs));
                    }
                };


                StochasticProcess process1 = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(200));
                        raise(nodeIDs.length, startNode, new GenIntSequentialDistribution(nodeIDs));
                    }
                };

               StochasticProcess startResultComp = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(0));
                        raise(1, startResult, new ConstantDistribution(Integer.class, 0));
                    }
                };


                process1.start();
                processApp.startAfterStartOf((long) ((runTime * 0.8) * 1000), process1);
                startResultComp.startAfterTerminationOf(runTime * 1000, process1);
                terminateAfterTerminationOf(10000, startResultComp);

            }
        };


        scen.setSeed(1234L);

        return scen;
    }

}
