package se.ii2202.dht.msg;

import se.ii2202.dht.object.NodeInfo;

public class Join  extends RingMessage<Object> {

    public NodeInfo address;

    public Join(NodeInfo src, NodeInfo dst, NodeInfo address) {
        super(src, dst);
        this.address = address;
    }
}
