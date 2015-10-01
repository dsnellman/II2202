package se.ii2202.dht.timer;

import se.ii2202.dht.msg.ClosestFingerResponse;
import se.ii2202.dht.msg.RingAdd;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class InnerLatencyTimerClosest extends Timeout {

    public ClosestFingerResponse closestFingerResponse;
    public int msgId;

    public InnerLatencyTimerClosest(ScheduleTimeout request,int msgId, ClosestFingerResponse closestFingerResponse) {
        super(request);
        this.closestFingerResponse = closestFingerResponse;
        this.msgId = msgId;
    }
}
