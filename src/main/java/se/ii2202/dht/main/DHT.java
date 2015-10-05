package se.ii2202.dht.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ii2202.dht.object.NodeInfo;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.p2ptoolbox.simulator.timed.api.Timed;
import se.sics.p2ptoolbox.simulator.timed.api.TimedControler;
import se.sics.p2ptoolbox.simulator.timed.api.TimedControlerBuilder;

import java.util.ArrayList;

public class DHT extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(DHT.class);

    private Positive<Timer> timer = requires(Timer.class);
    private Positive<Network> network = requires(Network.class);

    private TimedControler tc;

    private final NodeInfo self;
    private final NodeInfo firstNode;

    private final Component chord;

    public DHT(DhtInit init){
        this.self = init.selfAddress;
        this.firstNode = init.firstNode;

        tc = init.tcb.registerComponent(self.id, this);
        subscribe(handleStart, control);
        subscribe(handleStarted, control);

        chord = create(Chord.class, new Chord.ChordInit(self, firstNode, init.properties, init.tcb));
        connect(chord.getNegative(Timer.class), timer);
        connect(chord.getNegative(Network.class), network);

    }

    private Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            tc.advance(DHT.this, 0);
            //log.info("Starting dht for node {}....", new Object[]{selfAddress});
        }
    };
    private Handler<Started> handleStarted = new Handler<Started>() {
        @Override
        public void handle(Started event) {
            tc.advance(DHT.this, 0);
            //log.info("Starting dht for node {}....", new Object[]{selfAddress});
        }
    };

    public static class DhtInit extends Init<DHT> implements Timed{

        public TimedControlerBuilder tcb;
        public NodeInfo selfAddress;
        public NodeInfo firstNode;
        public RunProperties properties;

        public DhtInit(NodeInfo selfAddress, NodeInfo firstNode, RunProperties properties) {
            this.selfAddress = selfAddress;
            this.firstNode = firstNode;
            this.properties = properties;
        }

        @Override
        public void set(TimedControlerBuilder tcb) {
            this.tcb = tcb;
        }
    }

}
