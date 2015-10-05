package se.ii2202.dht.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ii2202.dht.object.LatencyContainer;
import se.ii2202.dht.object.NodeInfo;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.simulator.timed.api.Timed;
import se.sics.p2ptoolbox.simulator.timed.api.TimedControler;
import se.sics.p2ptoolbox.simulator.timed.api.TimedControlerBuilder;

import java.util.ArrayList;

public class App extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private Positive<Timer> timer = requires(Timer.class);
    private Positive<Network> network = requires(Network.class);

    private TimedControler tc;

    private final Component Application;
    private final Component latencyLayer;


    public App(AppInit init){

        tc = init.tcb.registerComponent(init.app.id, this);

        subscribe(handleStart, control);
        subscribe(handleStarted, control);

        latencyLayer = create(LatencyLayer.class, new LatencyLayer.LatencyInit(init.app, init.properties, init.city, init.latency, init.tcb));
        connect(latencyLayer.getNegative(Timer.class), timer);
        connect(latencyLayer.getNegative(Network.class), network);

        Application = create(Application.class, new Application.ApplicationInit(init.app, init.properties, init.ringNodes, init.latency, init.tcb));
        connect(Application.getNegative(Timer.class), timer);
        connect(Application.getNegative(Network.class), latencyLayer.getPositive(Network.class));


    }

    private Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            tc.advance(App.this, 0);
            //log.info("Starting app....", new Object[]{});
        }
    };

    private Handler<Started> handleStarted = new Handler<Started>() {
        @Override
        public void handle(Started event) {
            tc.advance(App.this, 0);
            //log.info("Starting app....", new Object[]{});
        }
    };

    public static class AppInit extends Init<App> implements Timed {

        public TimedControlerBuilder tcb;
        public NodeInfo app;
        public String city;
        public ArrayList<LatencyContainer> latency;
        public ArrayList<ArrayList<NodeInfo>> ringNodes;
        public RunProperties properties;

        public AppInit(NodeInfo app, RunProperties properties, String city, ArrayList<LatencyContainer> latency, ArrayList<ArrayList<NodeInfo>> ringNodes){
            this.properties = properties;
            this.app = app;
            this.latency = latency;
            this.city = city;
            this.ringNodes = ringNodes;

        }

        @Override
        public void set(TimedControlerBuilder tcb) {
            this.tcb = tcb;
        }

    }

}
