package se.ii2202.dht.appmsg;

import se.ii2202.dht.object.NodeInfo;

public class LookUp extends AppMessage<Object> {

    public enum LookUpTYPE {
        LOOKUP, PING;
    }


    public int key;
    public NodeInfo returnAddress;
    public int id;
    public int counter;
    public Long startInnerLatency;
    public LookUpTYPE type;


    public LookUp(NodeInfo src, NodeInfo dst, int key, NodeInfo returnAddress, int id, LookUpTYPE type) {
        super(src, dst);
        this.key = key;
        this.returnAddress = returnAddress;
        this.id = id;
        this.counter = 0;
        this.startInnerLatency = 0L;
        this.type = type;
    }

    public LookUp(NodeInfo src, NodeInfo dst, int key, NodeInfo returnAddress, int id, LookUpTYPE type, Long startInnterLatency) {
        super(src, dst);
        this.key = key;
        this.returnAddress = returnAddress;
        this.id = id;
        this.counter = 0;
        this.startInnerLatency = startInnterLatency;
        this.type = type;
    }
}
