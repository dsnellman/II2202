package se.ii2202.dht.object;

import se.ii2202.dht.main.RingInfo;

import java.util.Comparator;

public class CompareRingInfo implements Comparator<RingInfo> {

    @Override
    public int compare(RingInfo o1, RingInfo o2) {
        if(o1.avg > o2.avg)
            return 1;
        else
            return -1;
    }
}
