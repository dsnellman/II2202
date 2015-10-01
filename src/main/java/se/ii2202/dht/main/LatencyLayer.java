package se.ii2202.dht.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ii2202.dht.appmsg.AppMessage;
import se.ii2202.dht.appmsg.ResultRequest;
import se.ii2202.dht.appmsg.ResultResponse;
import se.ii2202.dht.object.NodeInfo;
import se.ii2202.dht.timer.LatencyTimer;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

import java.util.ArrayList;

public class LatencyLayer extends ComponentDefinition {
    private static final Logger log = LoggerFactory.getLogger(LatencyLayer.class);

    private Positive<Timer> timer = requires(Timer.class);
    private Positive<Network> network = requires(Network.class);
    private Negative<Network> local = provides(Network.class);


    public NodeInfo selfAddress;
    public Integer[] latency;

    public LatencyLayer(LatencyInit init) {

        selfAddress = init.selfAddress;
        latency = init.latency;

        subscribe(handleGoingFromApp, local);
        subscribe(handleGoingToApp, network);
        subscribe(handleLatecnyTimer, timer);


    }

    private Handler<AppMessage<Object>> handleGoingFromApp = new Handler<AppMessage<Object>>() {

        @Override
        public void handle(AppMessage<Object> event) {


            //log.info("{}: sending outgoing msg to app... from ring {} to ring {}, type {}", new Object[]{selfAddress, event.fromRing, event.toRing, event.getClass()});

            if(event instanceof ResultRequest){
                trigger(event, network);


            } else {
                if (latency[event.toRing] > 0) {

                    ScheduleTimeout spt = new ScheduleTimeout(latency[event.toRing]);
                    LatencyTimer sc = new LatencyTimer(spt, event, 1);
                    spt.setTimeoutEvent(sc);
                    trigger(spt, timer);

                } else {
                    trigger(event, network);
                }
            }



        }
    };

    private Handler<AppMessage<Object>> handleGoingToApp = new Handler<AppMessage<Object>>() {

        @Override
        public void handle(AppMessage<Object> event) {
            //log.info("{}: recevied incomnig app msg... from ring {} to ring {} type: {}", new Object[]{selfAddress, event.fromRing, event.toRing, event.getClass()});

            if(event instanceof ResultResponse){
                trigger(event, local);
            } else {


                if (latency[event.fromRing] > 0) {

                    ScheduleTimeout spt = new ScheduleTimeout(latency[event.fromRing]);
                    LatencyTimer sc = new LatencyTimer(spt, event, 2);
                    spt.setTimeoutEvent(sc);
                    trigger(spt, timer);

                } else {
                    trigger(event, local);
                }
            }


        }

    };

    private Handler<LatencyTimer> handleLatecnyTimer = new Handler<LatencyTimer>() {

        public void handle(LatencyTimer event){

            if(event.type == 1)
                trigger(event.msg, network);
            else
                trigger(event.msg, local);

        }
    };



    public static class LatencyInit extends Init<LatencyLayer> {

        public NodeInfo selfAddress;
        public Integer[] latency;

        public LatencyInit(NodeInfo selfAddress, Integer[] latency){
            this.selfAddress = selfAddress;
            this.latency = latency;
        }


    }

}
