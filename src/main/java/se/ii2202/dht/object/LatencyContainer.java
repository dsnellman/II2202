package se.ii2202.dht.object;


import java.util.ArrayList;
import java.util.Objects;

public class LatencyContainer {

    public String from;
    public String to;
    public ArrayList<Integer> latencies;

    public LatencyContainer(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public LatencyContainer(){
        latencies = new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LatencyContainer that = (LatencyContainer) o;
        return Objects.equals(from, that.from) &&
                Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    public String toString(){
        return from + "-" + to + "(" + latencies.size() + ")";
    }
}
