package se.ii2202.dht.msg;

import se.ii2202.dht.appmsg.LookUp;
import se.ii2202.dht.main.TYPE;
import se.ii2202.dht.object.Item;
import se.ii2202.dht.object.NodeInfo;

public class ClosestFinger extends RingMessage<Object> {

    public NodeInfo foundedAddress;
    public NodeInfo sender;
    public NodeInfo returnAddress;
    public int id;
    public int finger;
    public TYPE type;
    public Item item;
    public int lookupID;
    public int msgCounter;
    public Long startInnerLactency;
    public LookUp.LookUpTYPE lookupType;

    public ClosestFinger(NodeInfo src, NodeInfo dst, NodeInfo returnAddress, TYPE type, int id, NodeInfo foundedAddress, int finger, Item item, int lookupID, int msgCounter, LookUp.LookUpTYPE lookupType, Long startInnerLateny) {
        super(src, dst);
        this.foundedAddress = foundedAddress;
        this.sender = src;
        this.returnAddress = returnAddress;
        this.id = id;
        this.finger = finger;
        this.type = type;
        this.item = item;
        this.lookupID = lookupID;
        this.msgCounter = msgCounter;
        this.startInnerLactency = startInnerLateny;
        this.lookupType = lookupType;
    }

}
