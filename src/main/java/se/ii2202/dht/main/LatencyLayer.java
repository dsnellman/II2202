package se.ii2202.dht.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ii2202.dht.appmsg.AppMessage;
import se.ii2202.dht.appmsg.ResultRequest;
import se.ii2202.dht.appmsg.ResultResponse;
import se.ii2202.dht.object.LatencyContainer;
import se.ii2202.dht.object.NodeInfo;
import se.ii2202.dht.timer.LatencyTimer;
import se.ii2202.dht.timer.PeriodicLatencyUpdateTimer;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

import java.util.ArrayList;
import java.util.Arrays;

public class LatencyLayer extends ComponentDefinition {
    private static final Logger log = LoggerFactory.getLogger(LatencyLayer.class);

    private Positive<Timer> timer = requires(Timer.class);
    private Positive<Network> network = requires(Network.class);
    private Negative<Network> local = provides(Network.class);


    public NodeInfo selfAddress;
    public ArrayList<LatencyContainer> latency;

    public String city;
    public int nRings;


    public ArrayList<String> allCities;

    public Integer[] currentOutgoingLatencies;
    public Integer[] currentIncomingLatencies;

    public LatencyLayer(LatencyInit init) {

        selfAddress = init.selfAddress;
        latency = init.latency;
        city = init.city;
        nRings = init.nRings;
        allCities = init.allCities;

        currentOutgoingLatencies = new Integer[init.nRings];
        currentIncomingLatencies = new Integer[init.nRings];

        subscribe(handleStart, control);
        subscribe(handleGoingFromApp, local);
        subscribe(handleGoingToApp, network);
        subscribe(handleLatencyTimer, timer);
        subscribe(handleUpdateLatencyTimer, timer);
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("Starting app with id: {} and latency layer with city {}, {}", new Object[]{selfAddress.id, city, allCities.toString()});
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(0, 1000);
            PeriodicLatencyUpdateTimer sc = new PeriodicLatencyUpdateTimer(spt);
            spt.setTimeoutEvent(sc);
            trigger(spt, timer);




        }
    };

    private Handler<AppMessage<Object>> handleGoingFromApp = new Handler<AppMessage<Object>>() {

        @Override
        public void handle(AppMessage<Object> event) {

            //log.info("{}: sending outgoing msg to app... from ring {} to ring {}, type {}", new Object[]{selfAddress, event.fromRing, event.toRing, event.getClass()});

            if(event instanceof ResultRequest){
                trigger(event, network);


            } else {
                if (currentOutgoingLatencies[event.toRing] > 0) {
                    //log.info("latency out: {}", new Object[]{currentOutgoingLatencies[event.toRing]});
                    ScheduleTimeout spt = new ScheduleTimeout(currentOutgoingLatencies[event.toRing]);
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


                if (currentIncomingLatencies[event.fromRing] > 0) {
                    //log.info("latency: {}", new Object[]{currentIncomingLatencies[event.fromRing]});
                    ScheduleTimeout spt = new ScheduleTimeout(currentIncomingLatencies[event.fromRing]);
                    LatencyTimer sc = new LatencyTimer(spt, event, 2);
                    spt.setTimeoutEvent(sc);
                    trigger(spt, timer);

                } else {
                    trigger(event, local);
                }
            }


        }

    };

    private Handler<LatencyTimer> handleLatencyTimer = new Handler<LatencyTimer>() {

        public void handle(LatencyTimer event){

            if(event.type == 1)
                trigger(event.msg, network);
            else
                trigger(event.msg, local);

        }
    };
    int updateCounter =0;
    private Handler<PeriodicLatencyUpdateTimer> handleUpdateLatencyTimer = new Handler<PeriodicLatencyUpdateTimer>() {

        public void handle(PeriodicLatencyUpdateTimer event) {
            updateCounter++;
            for(int i = 0; i < nRings; i++){
                int index;
                if(allCities.get(i) != city){

                    index = latency.indexOf(new LatencyContainer(city, allCities.get(i)));
                    currentOutgoingLatencies[i] = latency.get(index).latencies.get(updateCounter);

                    index = latency.indexOf(new LatencyContainer(allCities.get(i), city));
                    currentIncomingLatencies[i] = latency.get(index).latencies.get(updateCounter);
                } else{
                    currentOutgoingLatencies[i] = 5;
                    currentIncomingLatencies[i] = 5;
                }


            }

            //log.info("{} has latencies: out {}, in {}", new Object[]{selfAddress, Arrays.toString(currentOutgoingLatencies), Arrays.toString(currentIncomingLatencies)});

        }
    };



    public static class LatencyInit extends Init<LatencyLayer> {

        public NodeInfo selfAddress;
        public ArrayList<LatencyContainer> latency;
        public int nRings;
        public String city;
        public ArrayList<String> allCities;

        public LatencyInit(NodeInfo selfAddress, int nRings, String city, ArrayList<String> allCities, ArrayList<LatencyContainer> latency){
            this.selfAddress = selfAddress;
            this.latency = latency;
            this.nRings = nRings;
            this.city = city;
            this.allCities = allCities;
        }


    }

}
