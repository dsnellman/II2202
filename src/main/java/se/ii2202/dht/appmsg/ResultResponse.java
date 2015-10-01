package se.ii2202.dht.appmsg;

import se.ii2202.dht.object.NodeInfo;
import se.ii2202.dht.object.TestResult;

import java.util.ArrayList;

public class ResultResponse extends AppMessage<Object> {

    public ArrayList<TestResult> storeTimes;
    public ArrayList<TestResult> lookupTimes;

    public ResultResponse(NodeInfo src, NodeInfo dst, ArrayList<TestResult> storeTimes, ArrayList<TestResult> lookupTimes) {
        super(src, dst);
        this.storeTimes = storeTimes;
        this.lookupTimes = lookupTimes;
    }
}
