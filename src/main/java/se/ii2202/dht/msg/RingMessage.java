package se.ii2202.dht.msg;

import se.ii2202.dht.object.NodeInfo;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Transport;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicHeader;

public abstract class RingMessage  <C extends Object> extends BasicContentMsg<Address, Header<Address>, C> {

        public RingMessage(NodeInfo src, NodeInfo dst) {
        this(new BasicHeader(src.address, dst.address, Transport.UDP));
    }

        public RingMessage(Header<Address> header) {
        super(header, null);
    }

}
