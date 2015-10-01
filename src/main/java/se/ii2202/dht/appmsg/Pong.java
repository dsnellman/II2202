package se.ii2202.dht.appmsg;

import se.ii2202.dht.object.NodeInfo;

public class Pong  extends AppMessage<Object> {

    public int ring;
    public int id;
    public Long startInnerLatencyTime;
    public Long endInnerLatencyTime;

    public Pong(NodeInfo src, NodeInfo dst, int ring, int id, Long startInnerLatencyTime, Long endInnerLatencyTime){
        super(src, dst);
        this.id = id;
        this.ring = ring;
        this.startInnerLatencyTime = startInnerLatencyTime;
        this.endInnerLatencyTime = endInnerLatencyTime;
    }
}
