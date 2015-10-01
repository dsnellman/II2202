package se.ii2202.dht.msg;

import se.ii2202.dht.main.TYPE;
import se.ii2202.dht.object.NodeInfo;

public class FindSuccessor extends RingMessage<Object> {

    public NodeInfo address;
    public int finger;
    public TYPE type;
    public int id;

    public FindSuccessor(NodeInfo src, NodeInfo dst, TYPE type, int id, NodeInfo address, int finger) {
        super(src, dst);
        this.id = id;
        this.address = address;
        this.finger = finger;
        this.type = type;
    }

}
