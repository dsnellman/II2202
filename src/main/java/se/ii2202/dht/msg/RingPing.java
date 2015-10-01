package se.ii2202.dht.msg;

import se.ii2202.dht.object.NodeInfo;

public class RingPing extends RingMessage<Object> {

    public NodeInfo returnAddress;
    public NodeInfo startAddress;
    public Long startInnerLatencyTime;
    public int id;

    public RingPing (NodeInfo src, NodeInfo dst, NodeInfo returnAddress, NodeInfo startAddress, int id, Long startInnerLatencyTime){
        super(src, dst);
        this.returnAddress = returnAddress;
        this.startAddress = startAddress;
        this.startInnerLatencyTime = startInnerLatencyTime;
        this.id = id;

    }

}
