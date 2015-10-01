package se.ii2202.dht.msg;

import se.ii2202.dht.object.NodeInfo;

public class UpdateFingerTable extends RingMessage<Object> {

    public NodeInfo node;
    public int i;


    public UpdateFingerTable(NodeInfo src, NodeInfo dst, NodeInfo node, int i) {
        super(src, dst);
        this.node = node;
        this.i = i;
    }
}
