package se.ii2202.dht.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ii2202.dht.object.LatencyContainer;
import se.ii2202.dht.object.NodeInfo;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

import java.util.ArrayList;

public class App extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private Positive<Timer> timer = requires(Timer.class);
    private Positive<Network> network = requires(Network.class);



    private final Component Application;
    private final Component latencyLayer;


    public App(AppInit init){
        subscribe(handleStart, control);

        latencyLayer = create(LatencyLayer.class, new LatencyLayer.LatencyInit(init.app, init.nRings, init.city, init.allCities, init.latency));
        connect(latencyLayer.getNegative(Timer.class), timer);
        connect(latencyLayer.getNegative(Network.class), network);

        Application = create(Application.class, new Application.ApplicationInit(init.app, init.m, init.nRings, init.replications, init.ringNodes, init.latency));
        connect(Application.getNegative(Timer.class), timer);
        connect(Application.getNegative(Network.class), latencyLayer.getPositive(Network.class));


    }

    private Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            //log.info("Starting app....", new Object[]{});
        }
    };

    public static class AppInit extends Init<App> {

        public int m;
        public NodeInfo app;
        public int nRings;
        public String city;
        public ArrayList<LatencyContainer> latency;
        public int replications;
        public  NodeInfo[] ringNodes;
        public ArrayList<String> allCities;

        public AppInit(NodeInfo app, int m, int nRings,String city, ArrayList<String> allCities, ArrayList<LatencyContainer> latency, int replications,  NodeInfo[] ringNodes){
            this.m = m;
            this.app = app;
            this.latency = latency;
            this.nRings = nRings;
            this.city = city;
            this.replications = replications;
            this.ringNodes = ringNodes;
            this.allCities = allCities;
        }

    }

}
