package se.ii2202.dht.appmsg;

import se.ii2202.dht.object.NodeInfo;

public class AddResponse extends AppMessage<Object> {


    public int type;
    public int key;
    public int id;
    public int msgCounter;
    public Long startInnerLatency;
    public Long endInnerLatency;

    public AddResponse(NodeInfo src, NodeInfo dst, int key, int id, int type, int msgCounter, Long startInnerLatency, Long endInnerLatency) {
        super(src, dst);
        this.key = key;
        this.id = id;
        this.startInnerLatency = startInnerLatency;
        this.endInnerLatency = endInnerLatency;
        this.type = type;
        this.msgCounter = msgCounter;
    }
}
