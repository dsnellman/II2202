package se.ii2202.dht.msg;

import se.ii2202.dht.object.NodeInfo;

public class Notify extends RingMessage<Object> {

    public NodeInfo node;


    public Notify(NodeInfo src, NodeInfo dst, NodeInfo node) {
        super(src, dst);
        this.node = node;
    }

}
