package se.ii2202.dht.timer;

import se.ii2202.dht.appmsg.Add;
import se.ii2202.dht.msg.RingAdd;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class InnerLatencyTimerRingAdd extends Timeout {

    public RingAdd ringadd;
    public int msgId;

    public InnerLatencyTimerRingAdd(ScheduleTimeout request,int msgId, RingAdd ringadd) {
        super(request);
        this.ringadd = ringadd;
        this.msgId = msgId;
    }
}
