package se.ii2202.dht.msg;

import se.ii2202.dht.object.NodeInfo;

public class RingLookUp extends RingMessage<Object> {

    public int key;
    public NodeInfo returnAddress;
    public NodeInfo foundedAddress;
    public int id;
    public int counter;
    public Long startInnerLatency;

    public RingLookUp(NodeInfo src, NodeInfo dst, NodeInfo returnAddress, int key, NodeInfo foundedAddress, int id, int counter, Long startInnerLatency){
        super(src, dst);
        this.key = key;
        this.returnAddress = returnAddress;
        this.foundedAddress = foundedAddress;
        this.id = id;
        this.counter = counter;
        this.startInnerLatency = startInnerLatency;
    }
}
