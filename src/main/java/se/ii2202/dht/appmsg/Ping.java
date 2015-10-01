package se.ii2202.dht.appmsg;

import se.ii2202.dht.object.NodeInfo;

public class Ping extends AppMessage<Object>{

    public NodeInfo returnAddress;
    public NodeInfo startAddress;
    public int id;

    public Ping (NodeInfo src, NodeInfo dst, int id){
        super(src, dst);
        this.id = id;
        this.returnAddress = src;
        this.startAddress = dst;
    }
}
