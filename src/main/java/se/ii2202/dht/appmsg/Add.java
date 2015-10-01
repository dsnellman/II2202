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
    public ArrayList<NodeInfo> replicaAddress;

    public Add(NodeInfo src, NodeInfo dst, TYPE type, Item item, int id, NodeInfo returnAddress, ArrayList<NodeInfo> replicaAddress) {
        super(src, dst);
        this.item = item;
        this.type = type;
        this.id = id;
        this.msgCounter = 0;
        this.returnAddress = returnAddress;
        this.startInnerLatency = 0L;
        this.replicaAddress = replicaAddress;
    }

    public Add(NodeInfo src, NodeInfo dst, TYPE type, Item item, int id, NodeInfo returnAddress, Long startInnerLatecy, ArrayList<NodeInfo> replicaAddress) {
        super(src, dst);
        this.item = item;
        this.type = type;
        this.id = id;
        this.msgCounter = 0;
        this.returnAddress = returnAddress;
        this.startInnerLatency = startInnerLatecy;
        this.replicaAddress =replicaAddress;
    }
}
