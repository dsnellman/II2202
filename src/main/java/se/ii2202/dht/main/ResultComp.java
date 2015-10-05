package se.ii2202.dht.main;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ii2202.dht.appmsg.ResultRequest;
import se.ii2202.dht.appmsg.ResultResponse;
import se.ii2202.dht.object.NodeInfo;
import se.ii2202.dht.object.TestResult;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ResultComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(ResultComp.class);
    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private NodeInfo selfAddress;
    private NodeInfo[] apps;
    private String filename = "";

    public ResultComp(ResultInit init) {
        this.selfAddress = init.selfAddress;
        this.apps = init.apps;
        this.filename = init.filename;

        subscribe(handleStart, control);
        subscribe(handleResponse, network);

    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("time: {} | {} starting... with {} apps", new Object[]{System.currentTimeMillis(),selfAddress, apps.length});

            for(int i = 0; i < apps.length; i++){
                trigger(new ResultRequest(selfAddress, apps[i], selfAddress), network);
            }
        }

    };

    private int counter = 0;
    private static ArrayList<TestResult> storeTimes = new ArrayList<TestResult>();
    private static ArrayList<TestResult> lookupTimes = new ArrayList<TestResult>();

    private Handler<ResultResponse> handleResponse = new Handler<ResultResponse>() {

        @Override
        public void handle(ResultResponse msg) {


            counter++;

            storeTimes.addAll(msg.storeTimes);
            lookupTimes.addAll(msg.lookupTimes);

            //log.info("{} Received response: add {} lookup {}", new Object[]{selfAddress, storeTimes.size(), lookupTimes.size()});
            if(counter == apps.length){
                try {
                    printresult();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }

    };



    public void printresult() throws IOException {


        File storeFile = new File("./src/main/resources/tests/" + filename + "-adds.txt");
        File lookupFile = new File("./src/main/resources/tests/" + filename + "-lookup.txt");
        FileWriter writer = new FileWriter(storeFile, true);

        writer.write("STORES:\r\n");
        writer.write("Appid: \tRing: \tMessages \tKey \tTime \tExternal1 \tExternal2 \tInternal \tTotal\r\n");
        for(TestResult time : storeTimes){
            writer.write(time.appid + "\t" + time.ring + "\t" + time.messages + "\t" + time.key + "\t" + time.time + "\t" + time.external1 + "\t" + time.external2 + "\t" + time.internal + "\t" + time.total);
            writer.write("\r\n");   // write new line
        }
        writer.close();
        writer = new FileWriter(lookupFile, true);
        writer.write("LOOKUPS:\r\n");
        writer.write("Appid: \tRing: \tMessages \tKey \tTime \tExternal1 \tExternal2 \tInternal \tTotal\r\n");
        for(TestResult time : lookupTimes){
            writer.write(time.appid + "\t" + time.ring + "\t" + time.messages + "\t" + time.key + "\t" + time.time + "\t" + time.external1 + "\t" + time.external2 + "\t" + time.internal + "\t" + time.total);
            writer.write("\r\n");   // write new line
        }

    }




    public static class ResultInit extends Init<ResultComp> {

        public final NodeInfo selfAddress;
        public NodeInfo[] apps;
        public String filename;

        public ResultInit(NodeInfo selfAddress, NodeInfo[] apps, String filename) {
            this.selfAddress = selfAddress;
            this.apps = apps;
            this.filename = filename;
        }
    }
}
