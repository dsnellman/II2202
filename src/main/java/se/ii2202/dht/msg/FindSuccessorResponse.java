package se.ii2202.dht.msg;

import se.ii2202.dht.main.TYPE;
import se.ii2202.dht.object.NodeInfo;

public class FindSuccessorResponse extends RingMessage<Object> {

    public NodeInfo succ;
    public int finger;
    public TYPE type;

    public FindSuccessorResponse(NodeInfo src, NodeInfo dst, TYPE type, NodeInfo succ, int finger) {
        super(src, dst);
        this.succ = succ;
        this.finger = finger;
        this.type = type;
    }
}
