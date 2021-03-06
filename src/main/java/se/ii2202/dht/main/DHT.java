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

import java.util.ArrayList;

public class DHT extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(DHT.class);

    private Positive<Timer> timer = requires(Timer.class);
    private Positive<Network> network = requires(Network.class);

    private final NodeInfo selfAddress;
    private final NodeInfo firstNode;

    private final Component chord;

    public DHT(DhtInit init){
        this.selfAddress = init.selfAddress;
        this.firstNode = init.firstNode;


        subscribe(handleStart, control);

        chord = create(Chord.class, new Chord.ChordInit(selfAddress, firstNode, init.properties));
        connect(chord.getNegative(Timer.class), timer);
        connect(chord.getNegative(Network.class), network);

    }

    private Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            //log.info("Starting dht for node {}....", new Object[]{selfAddress});
        }
    };

    public static class DhtInit extends Init<DHT> {
        public NodeInfo selfAddress;
        public NodeInfo firstNode;
        public RunProperties properties;

        public DhtInit(NodeInfo selfAddress, NodeInfo firstNode, RunProperties properties) {
            this.selfAddress = selfAddress;
            this.firstNode = firstNode;
            this.properties = properties;
        }

    }

}
