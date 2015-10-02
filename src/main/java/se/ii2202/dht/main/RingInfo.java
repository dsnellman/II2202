package se.ii2202.dht.main;

public class RingInfo{

    public int ring;
    public int avg;

    public RingInfo(int ring, int avg) {
        this.ring = ring;
        this.avg = avg;
    }

    public String toString(){
        return "{" + ring + ", avg: " + avg + "}";
    }
}
