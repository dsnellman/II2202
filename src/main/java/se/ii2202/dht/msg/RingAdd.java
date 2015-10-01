package se.ii2202.dht.msg;

import se.ii2202.dht.main.TYPE;
import se.ii2202.dht.object.Item;
import se.ii2202.dht.object.NodeInfo;

public class RingAdd extends RingMessage<Object> {

    public Item item;
    public int msgCounter;
    public TYPE type;
    public int id;
    public NodeInfo returnAddress;
    public Long startInnerLatency;

    public RingAdd(NodeInfo src, NodeInfo dst, TYPE type, Item item, int id, int msgCounter, NodeInfo returnAddress, Long startInnerLatency) {
        super(src, dst);
        this.item = item;
        this.msgCounter = msgCounter;
        this.type = type;
        this.id = id;
        this.returnAddress = returnAddress;
        this.startInnerLatency = startInnerLatency;
    }
}
