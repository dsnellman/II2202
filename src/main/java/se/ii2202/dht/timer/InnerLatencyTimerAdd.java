package se.ii2202.dht.timer;

import se.ii2202.dht.appmsg.Add;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class InnerLatencyTimerAdd extends Timeout {

    public Add add;
    public int msgId;

    public InnerLatencyTimerAdd(ScheduleTimeout request,int msgId, Add add) {
        super(request);
        this.add = add;
        this.msgId = msgId;
    }
}
