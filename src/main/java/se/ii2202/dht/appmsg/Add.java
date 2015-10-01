package se.ii2202.dht.appmsg;

import se.ii2202.dht.main.TYPE;
import se.ii2202.dht.object.Item;
import se.ii2202.dht.object.NodeInfo;

import java.util.ArrayList;

public class Add extends AppMessage<Object>{

    public Item item;
    public NodeInfo[] ringNodes;
    public TYPE type;
    public int id;
    public NodeInfo returnAddress;
    public Long startInnerLatency;
    public int msgCounter;

    public Add(NodeInfo src, NodeInfo dst, TYPE type, Item item, int id, NodeInfo returnAddress){
        super(src, dst);
        this.item = item;
        this.type = type;
        this.id = id;
        this.msgCounter = 0;
        this.returnAddress = returnAddress;
        this.startInnerLatency = 0L;
    }

    public Add(NodeInfo src, NodeInfo dst, TYPE type, Item item, int id, NodeInfo returnAddress, Long startInnerLatecy) {
        super(src, dst);
        this.item = item;
        this.type = type;
        this.id = id;
        this.msgCounter = 0;
        this.returnAddress = returnAddress;
        this.startInnerLatency = startInnerLatecy;
    }
}
