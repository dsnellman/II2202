package se.ii2202.dht.appmsg;

import se.ii2202.dht.object.NodeInfo;

public class ResultRequest extends AppMessage<Object>{

    public NodeInfo returnAddress;

    public ResultRequest(NodeInfo src, NodeInfo dst, NodeInfo returnAddress) {
        super(src, dst);
        this.returnAddress = returnAddress;
    }
}
