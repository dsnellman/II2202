package se.ii2202.dht.object;

public class Finger {

    public NodeInfo address;
    public NodeInfo node;
    public NodeInfo succ;
    public NodeInfo pred;
    public int start;

    public Finger(int start) {
        this.start = start;
        this.node = new NodeInfo();
    }

    public Finger(){
        address = null;
    }
}
