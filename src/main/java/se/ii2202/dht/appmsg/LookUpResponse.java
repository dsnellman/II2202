package se.ii2202.dht.appmsg;

import se.ii2202.dht.object.NodeInfo;

public class LookUpResponse extends AppMessage<Object> {

    public enum LookUp {
        InStore, InReplica, NotFound;
    }

    public LookUp answer;
    public int key;
    public NodeInfo address;
    public int id;
    public int counter;
    public Long startInnerLatency;
    public Long endInnerLatency;
    public se.ii2202.dht.appmsg.LookUp.LookUpTYPE type;

    public LookUpResponse(NodeInfo src, NodeInfo dst, int key,  LookUp answer, NodeInfo address, int id, int counter, se.ii2202.dht.appmsg.LookUp.LookUpTYPE type, Long startInnerLatency, Long endInnerLatency) {
        super(src, dst);
        this.answer = answer;
        this.key = key;
        this.address = address;
        this.id = id;
        this.counter = counter;
        this.startInnerLatency = startInnerLatency;
        this.endInnerLatency = endInnerLatency;
        this.type = type;
    }

}
