package se.ii2202.dht.timer;


import se.ii2202.dht.msg.RingLookUp;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class InnerLatencyTimerRingLookUp extends Timeout

{

    public RingLookUp lookup;
    public int msgId;

    public InnerLatencyTimerRingLookUp(ScheduleTimeout request,int msgId, RingLookUp lookup) {
        super(request);
        this.lookup = lookup;
        this.msgId = msgId;
    }
}
