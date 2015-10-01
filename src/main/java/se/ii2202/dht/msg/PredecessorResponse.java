package se.ii2202.dht.msg;

import se.ii2202.dht.main.TYPE;
import se.ii2202.dht.object.NodeInfo;

public class PredecessorResponse extends RingMessage<Object> {

    public NodeInfo node;
    public TYPE type;


    public PredecessorResponse(NodeInfo src, NodeInfo dst, TYPE type, NodeInfo node) {
        super(src, dst);
        this.node = node;
        this.type = type;
    }
}
