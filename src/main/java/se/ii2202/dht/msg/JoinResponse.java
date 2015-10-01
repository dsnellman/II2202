package se.ii2202.dht.msg;

import se.ii2202.dht.object.NodeInfo;

public class JoinResponse extends RingMessage<Object> {

    public NodeInfo succ;

    public JoinResponse(NodeInfo src, NodeInfo dst, NodeInfo succ) {
        super(src, dst);
        this.succ = succ;

    }
}
