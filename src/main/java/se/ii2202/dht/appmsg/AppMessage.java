package se.ii2202.dht.appmsg;


import se.ii2202.dht.object.NodeInfo;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Transport;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicHeader;

public abstract class AppMessage<C extends Object> extends BasicContentMsg<Address, Header<Address>, C> {

    public int fromRing;
    public int toRing;


    public AppMessage(NodeInfo src, NodeInfo dst) {
        this(new BasicHeader(src.address, dst.address, Transport.UDP), src.ring, dst.ring);
    }

    public AppMessage(Header<Address> header, int from, int to) {
        super(header, null);
        this.fromRing = from;
        this.toRing = to;
    }
}