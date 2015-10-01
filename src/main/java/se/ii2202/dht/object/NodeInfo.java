package se.ii2202.dht.object;

import se.sics.kompics.network.Address;

public class NodeInfo {

    public Address address;
    public int id;
    public NodeInfo succ;
    public NodeInfo pred;
    public NodeInfo start;
    public int ring;


    public NodeInfo(){
        address = null;
        id = -1;
    }

    public NodeInfo(Address address, int id){
        this.address = address;
        this.id = id;
    }

    public NodeInfo(NodeInfo node){
        this.address = node.address;
        this.id = node.id;
    }

    public NodeInfo(Address address, int id, int ring) {
        this.address = address;
        this.id = id;
        this.ring = ring;
    }


    @Override
    public String toString(){
        return address.toString() + "(" + ring + ")";
    }
}
